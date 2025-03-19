/*
 * Copyright 2025 Uppsala University Library
 *
 * This file is part of Cora.
 *
 *     Cora is free software: you can redistribute it and/or modify
 *     it under the terms of the GNU General Public License as published by
 *     the Free Software Foundation, either version 3 of the License, or
 *     (at your option) any later version.
 *
 *     Cora is distributed in the hope that it will be useful,
 *     but WITHOUT ANY WARRANTY; without even the implied warranty of
 *     MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *     GNU General Public License for more details.
 *
 *     You should have received a copy of the GNU General Public License
 *     along with Cora.  If not, see <http://www.gnu.org/licenses/>.
 */
package se.uu.ub.cora.spider.cache;

import static org.testng.Assert.assertEquals;

import java.util.HashMap;
import java.util.Map;

import org.testng.annotations.BeforeMethod;
import org.testng.annotations.BeforeTest;
import org.testng.annotations.Test;

import se.uu.ub.cora.initialize.SettingsProvider;
import se.uu.ub.cora.logger.LoggerProvider;
import se.uu.ub.cora.logger.spies.LoggerFactorySpy;
import se.uu.ub.cora.messaging.AmqpMessageSenderRoutingInfo;
import se.uu.ub.cora.messaging.MessagingProvider;
import se.uu.ub.cora.spider.resourceconvert.spy.MessageSenderSpy;
import se.uu.ub.cora.spider.resourceconvert.spy.MessagingFactorySpy;

public class DataChangedSenderTest {
	private static final String SOME_TYPE = "someType";
	private static final String SOME_ID = "someId";
	private static final String SOME_ACTION = "someAction";
	private static final String FACTOR_METHOD_NAME = "factorTopicMessageSender";
	private static final String SOME_HOST = "someHostname";
	private static final String SOME_PORT = "8080";
	private static final String SOME_VHOST = "someVhost";
	private static final String DATA_CHANGED_EXCHANGE = "dataChangedExchange";
	private LoggerFactorySpy loggerFactorySpy = new LoggerFactorySpy();
	private MessagingFactorySpy messagingFactory;
	private DataChangedSender sender;

	@BeforeTest
	public void beforeTest() {
		LoggerProvider.setLoggerFactory(loggerFactorySpy);
	}

	@BeforeMethod
	public void beforeMethod() {
		messagingFactory = new MessagingFactorySpy();
		MessagingProvider.setMessagingFactory(messagingFactory);

		sender = DataChangedSenderImp.create();
		setServerInfoInSettingsProvider();
	}

	private void setServerInfoInSettingsProvider() {
		Map<String, String> settingsMap = new HashMap<>();
		settingsMap.put("rabbitMqHostname", SOME_HOST);
		settingsMap.put("rabbitMqPort", SOME_PORT);
		settingsMap.put("rabbitMqVirtualHost", SOME_VHOST);
		settingsMap.put("rabbitMqDataExchange", DATA_CHANGED_EXCHANGE);
		SettingsProvider.setSettings(settingsMap);
	}

	@Test
	public void testCorrectHostPortAndRoutingKeyUsedForMessageSender() {
		sender.sendDataChanged(SOME_TYPE, SOME_ID, SOME_ACTION);

		assertMessagingProviderCalledWithBindingInformation(SOME_TYPE);
	}

	private void assertMessagingProviderCalledWithBindingInformation(String type) {
		AmqpMessageSenderRoutingInfo messagingRoutingInfo = getMessageRoutingInfoSpyFromProvider();
		assertEquals(messagingRoutingInfo.hostname, SOME_HOST);
		assertEquals(messagingRoutingInfo.port, Integer.parseInt(SOME_PORT));
		assertEquals(messagingRoutingInfo.virtualHost, SOME_VHOST);
		assertEquals(messagingRoutingInfo.exchange, DATA_CHANGED_EXCHANGE);
		assertEquals(messagingRoutingInfo.routingKey, type);
	}

	private AmqpMessageSenderRoutingInfo getMessageRoutingInfoSpyFromProvider() {
		return (AmqpMessageSenderRoutingInfo) messagingFactory.MCR
				.getParameterForMethodAndCallNumberAndParameter(FACTOR_METHOD_NAME, 0,
						"messagingRoutingInfo");
	}

	@Test
	public void testSendHeaderInfo() {
		sender.sendDataChanged(SOME_TYPE, SOME_ID, SOME_ACTION);

		assertMessageIsSentUsingMessageSenderFromProvider(SOME_TYPE, SOME_ID, SOME_ACTION);
	}

	private void assertMessageIsSentUsingMessageSenderFromProvider(String type, String id,
			String action) {
		MessageSenderSpy messageSender = (MessageSenderSpy) messagingFactory.MCR
				.getReturnValue(FACTOR_METHOD_NAME, 0);

		Map<String, Object> headers = new HashMap<>();
		headers.put("type", type);
		headers.put("id", id);
		headers.put("action", action);
		headers.put("messagingId", MessagingProvider.getMessagingId());
		messageSender.MCR.assertMethodWasCalled("sendMessage");
		messageSender.MCR.assertParameterAsEqual("sendMessage", 0, "headers", headers);
		messageSender.MCR.assertParameterAsEqual("sendMessage", 0, "message", "");
	}

}
