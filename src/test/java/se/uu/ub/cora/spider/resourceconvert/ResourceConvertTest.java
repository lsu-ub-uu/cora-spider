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

import org.testng.annotations.BeforeMethod;
import org.testng.annotations.BeforeTest;
import org.testng.annotations.Test;

import se.uu.ub.cora.logger.LoggerProvider;
import se.uu.ub.cora.logger.spies.LoggerFactorySpy;
import se.uu.ub.cora.messaging.MessageRoutingInfo;
import se.uu.ub.cora.messaging.MessagingProvider;
import se.uu.ub.cora.spider.resourceconvert.spy.MessageSenderSpy;
import se.uu.ub.cora.spider.resourceconvert.spy.MessagingFactorySpy;

public class ResourceConvertTest {
	private static final String FACTOR_METHOD_NAME = "factorTopicMessageSender";
	private static final String SOME_ID = "someId";
	private static final String SOME_TYPE = "someType";
	private static final String SOME_DATA_DIVIDER = "someDataDivider";
	private static final String SOME_HOST = "someHostname";
	private static final String SOME_PORT = "somePort";
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
		resourceConvert = ResourceConvertImp.usingHostnameAndPort(SOME_HOST, SOME_PORT);

		// MessageListener topicMessageListener =
		// MessagingProvider.getTopicMessageListener(getMessageRoutingInfoSpyFromProvider());
		//
		// MessageReceiver messageReciever ;
		// topicMessageListener.listen(messageReciever );
	}

	@Test
	public void testCorrectHostPortAndRoutingKeyUsedForMessageSender() throws Exception {
		resourceConvert.analyzeAndConvertToThumbnails(SOME_DATA_DIVIDER, SOME_TYPE, SOME_ID);

		MessageRoutingInfo messagingRoutingInfo = getMessageRoutingInfoSpyFromProvider();
		assertEquals(messagingRoutingInfo.hostname, SOME_HOST);
		assertEquals(messagingRoutingInfo.port, SOME_PORT);
		assertEquals(messagingRoutingInfo.routingKey, "analyzeAndConvertToThumbnails");
	}

	private MessageRoutingInfo getMessageRoutingInfoSpyFromProvider() {
		return (MessageRoutingInfo) messagingFactory.MCR
				.getValueForMethodNameAndCallNumberAndParameterName(FACTOR_METHOD_NAME, 0,
						"messagingRoutingInfo");
	}

	@Test
	public void testSendAnalyzeAndConvertToThumbnailsMessage() throws Exception {
		resourceConvert.analyzeAndConvertToThumbnails(SOME_DATA_DIVIDER, SOME_TYPE, SOME_ID);
		MessageSenderSpy messageSender = (MessageSenderSpy) messagingFactory.MCR
				.getReturnValue(FACTOR_METHOD_NAME, 0);

		messageSender.MCR.assertMethodWasCalled("sendMessage");
		messageSender.MCR.assertParameters("sendMessage", 0);
	}

}
