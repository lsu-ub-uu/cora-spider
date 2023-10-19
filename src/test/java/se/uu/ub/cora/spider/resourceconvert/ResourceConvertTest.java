/*
 * Copyright 2023 Uppsala University Library
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
package se.uu.ub.cora.spider.resourceconvert;

import static org.testng.Assert.assertEquals;

import java.util.HashMap;
import java.util.Map;

import org.testng.annotations.BeforeMethod;
import org.testng.annotations.BeforeTest;
import org.testng.annotations.Test;

import se.uu.ub.cora.logger.LoggerProvider;
import se.uu.ub.cora.logger.spies.LoggerFactorySpy;
import se.uu.ub.cora.messaging.AmqpMessageSenderRoutingInfo;
import se.uu.ub.cora.messaging.MessagingProvider;
import se.uu.ub.cora.spider.resourceconvert.spy.MessageSenderSpy;
import se.uu.ub.cora.spider.resourceconvert.spy.MessagingFactorySpy;

public class ResourceConvertTest {
	private static final String FACTOR_METHOD_NAME = "factorTopicMessageSender";
	private static final String SOME_ID = "someId";
	private static final String SOME_TYPE = "someType";
	private static final String SOME_DATA_DIVIDER = "someDataDivider";
	private static final String SOME_HOST = "someHostname";
	private static final int SOME_PORT = 8080;
	private static final String SOME_VHOST = "someVhost";
	private static final String SOME_EXCHANGE = "someExchange";
	private static final String SOME_ROUTINGKEY = "someRoutingKey";
	private LoggerFactorySpy loggerFactorySpy = new LoggerFactorySpy();
	private MessagingFactorySpy messagingFactory;
	private ResourceConvert resourceConvert;

	@BeforeTest
	public void beforeTest() {
		LoggerProvider.setLoggerFactory(loggerFactorySpy);
	}

	@BeforeMethod
	public void beforeMethod() {
		messagingFactory = new MessagingFactorySpy();
		MessagingProvider.setMessagingFactory(messagingFactory);

		resourceConvert = ResourceConvertImp.usingHostnamePortVHostExchangeRoutingKey(SOME_HOST,
				SOME_PORT, SOME_VHOST, SOME_EXCHANGE, SOME_ROUTINGKEY);
	}

	@Test
	public void testCorrectHostPortAndRoutingKeyUsedForMessageSender() throws Exception {
		resourceConvert.sendMessageForAnalyzeAndConvertToThumbnails(SOME_DATA_DIVIDER, SOME_TYPE, SOME_ID);

		AmqpMessageSenderRoutingInfo messagingRoutingInfo = getMessageRoutingInfoSpyFromProvider();
		assertEquals(messagingRoutingInfo.hostname, SOME_HOST);
		assertEquals(messagingRoutingInfo.port, SOME_PORT);
		assertEquals(messagingRoutingInfo.virtualHost, SOME_VHOST);
		assertEquals(messagingRoutingInfo.exchange, SOME_EXCHANGE);
		assertEquals(messagingRoutingInfo.routingKey, SOME_ROUTINGKEY);
	}

	private AmqpMessageSenderRoutingInfo getMessageRoutingInfoSpyFromProvider() {
		return (AmqpMessageSenderRoutingInfo) messagingFactory.MCR
				.getValueForMethodNameAndCallNumberAndParameterName(FACTOR_METHOD_NAME, 0,
						"messagingRoutingInfo");
	}

	@Test
	public void testSendAnalyzeAndConvertToThumbnailsMessage() throws Exception {
		resourceConvert.sendMessageForAnalyzeAndConvertToThumbnails(SOME_DATA_DIVIDER, SOME_TYPE, SOME_ID);
		MessageSenderSpy messageSender = (MessageSenderSpy) messagingFactory.MCR
				.getReturnValue(FACTOR_METHOD_NAME, 0);

		Map<String, Object> headers = new HashMap<>();
		headers.put("dataDivider", SOME_DATA_DIVIDER);
		headers.put("type", SOME_TYPE);
		headers.put("id", SOME_ID);
		messageSender.MCR.assertMethodWasCalled("sendMessage");
		messageSender.MCR.assertParameterAsEqual("sendMessage", 0, "headers", headers);
		messageSender.MCR.assertParameterAsEqual("sendMessage", 0, "message",
				"Read metadata and convert to small formats");

	}

}
