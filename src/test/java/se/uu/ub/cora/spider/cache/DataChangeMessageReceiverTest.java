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

import static org.testng.Assert.assertTrue;

import java.util.HashMap;
import java.util.Map;

import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import se.uu.ub.cora.bookkeeper.metadata.MetadataHolderProvider;
import se.uu.ub.cora.bookkeeper.storage.MetadataStorageProvider;
import se.uu.ub.cora.logger.LoggerProvider;
import se.uu.ub.cora.logger.spies.LoggerFactorySpy;
import se.uu.ub.cora.logger.spies.LoggerSpy;
import se.uu.ub.cora.messaging.MessageReceiver;
import se.uu.ub.cora.storage.RecordStorageProvider;
import se.uu.ub.cora.storage.spies.RecordStorageInstanceProviderSpy;

public class DataChangeMessageReceiverTest {

	private static final String EMPTY_MESSAGE = "";
	private DataChangeMessageReceiver receiver;
	// private GatekeeperLocatorSpy locator;

	private RecordStorageInstanceProviderSpy recordStorageProvider;
	private LoggerFactorySpy loggerFactory;
	private MetadataHolderSpy metadataHolder;
	private MetadataStorageViewInstanceProviderSpy metadataInstanceProvider;

	@BeforeMethod
	private void beforeMethod() {
		setUpRecordStorageProvider();
		// locator = new GatekeeperLocatorSpy();
		// GatekeeperInstanceProvider.setGatekeeperLocator(locator);

		receiver = new DataChangeMessageReceiver();
	}

	@AfterMethod
	private void afterMethod() {
		LoggerProvider.setLoggerFactory(null);
		RecordStorageProvider.onlyForTestSetRecordStorageInstanceProvider(null);
		MetadataHolderProvider.onlyForTestSetHolder(null);
	}

	private void setUpRecordStorageProvider() {
		loggerFactory = new LoggerFactorySpy();
		LoggerProvider.setLoggerFactory(loggerFactory);

		recordStorageProvider = new RecordStorageInstanceProviderSpy();
		RecordStorageProvider.onlyForTestSetRecordStorageInstanceProvider(recordStorageProvider);

		metadataInstanceProvider = new MetadataStorageViewInstanceProviderSpy();
		MetadataStorageProvider
				.onlyForTestSetMetadataStorageViewInstanceProvider(metadataInstanceProvider);

		metadataHolder = new MetadataHolderSpy();
		MetadataHolderProvider.onlyForTestSetHolder(metadataHolder);
	}

	@Test
	public void testImplementsMessageReceiver() {
		assertTrue(receiver instanceof MessageReceiver);
	}

	@Test
	public void testReceiveMessage_AnyType_updateRecordStorage() {
		Map<String, String> headers = createHeadersForType("someType", "someAction");

		receiver.receiveMessage(headers, EMPTY_MESSAGE);

		recordStorageProvider.MCR.assertParameters("dataChanged", 0, "someType", "someId",
				"someAction");
		metadataHolder.MCR.assertMethodNotCalled("addMetadataElement");
	}

	@Test
	public void testReceiveMessageAndCallDataChanged_forMetadata_metadataHolderUpdated() {
		Map<String, String> headers = createHeadersForType("metadata", "someAction");

		receiver.receiveMessage(headers, EMPTY_MESSAGE);

		recordStorageProvider.MCR.assertParameters("dataChanged", 0, "metadata", "someId",
				"someAction");
		var storageView = (MetadataStorageViewSpy) metadataInstanceProvider.MCR
				.getReturnValue("getStorageView", 0);
		var metadataElement = storageView.MCR.assertCalledParametersReturn("getMetadataElement",
				"someId");
		metadataHolder.MCR.assertParameters("addMetadataElement", 0, metadataElement);
	}

	@Test
	public void testReceiveMessageAndCallDataChanged_forMetadata_metadataHolderDelete() {
		Map<String, String> headers = createHeadersForType("metadata", "delete");

		receiver.receiveMessage(headers, EMPTY_MESSAGE);

		recordStorageProvider.MCR.assertParameters("dataChanged", 0, "metadata", "someId",
				"delete");
		metadataHolder.MCR.assertParameters("deleteMetadataElement", 0, "someId");
	}

	@Test(enabled = false)
	public void testTopicClosed() {
		// can not be tested as security manager is removed from java
		receiver.topicClosed();

		LoggerSpy logger = getLogger();
		logger.MCR.assertParameters("logFatalUsingMessage", 0,
				"Shuting down Spider due to lost connection with message broker,"
						+ "continued operation would lead to system inconsistencies.");
	}

	private LoggerSpy getLogger() {
		return (LoggerSpy) loggerFactory.MCR.getReturnValue("factorForClass", 0);
	}

	private Map<String, String> createHeadersForType(String type, String action) {
		Map<String, String> headers = new HashMap<>();
		headers.put("type", type);
		headers.put("id", "someId");
		headers.put("action", action);
		return headers;
	}

}
