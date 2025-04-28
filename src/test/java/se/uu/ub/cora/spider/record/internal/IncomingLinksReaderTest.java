/*
 * Copyright 2017, 2018, 2019, 2022, 2024 Uppsala University Library
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
package se.uu.ub.cora.spider.record.internal;

import static org.testng.Assert.fail;
import static se.uu.ub.cora.data.Action.READ;
import static se.uu.ub.cora.spider.extendedfunctionality.ExtendedFunctionalityPosition.INCOMING_LINKS_AFTER_AUTHORIZATION;

import java.util.LinkedHashSet;
import java.util.Set;

import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import se.uu.ub.cora.beefeater.authentication.User;
import se.uu.ub.cora.data.DataList;
import se.uu.ub.cora.data.DataProvider;
import se.uu.ub.cora.data.DataRecordGroup;
import se.uu.ub.cora.data.collected.CollectTerms;
import se.uu.ub.cora.data.collected.Link;
import se.uu.ub.cora.data.spies.DataFactorySpy;
import se.uu.ub.cora.data.spies.DataGroupSpy;
import se.uu.ub.cora.data.spies.DataListSpy;
import se.uu.ub.cora.data.spies.DataRecordGroupSpy;
import se.uu.ub.cora.data.spies.DataRecordLinkSpy;
import se.uu.ub.cora.logger.LoggerProvider;
import se.uu.ub.cora.spider.dependency.SpiderInstanceFactory;
import se.uu.ub.cora.spider.dependency.SpiderInstanceFactoryImp;
import se.uu.ub.cora.spider.dependency.SpiderInstanceProvider;
import se.uu.ub.cora.spider.dependency.spy.RecordTypeHandlerOldSpy;
import se.uu.ub.cora.spider.extendedfunctionality.ExtendedFunctionalityData;
import se.uu.ub.cora.spider.extendedfunctionality.internal.ExtendedFunctionalityProviderSpy;
import se.uu.ub.cora.spider.log.LoggerFactorySpy;
import se.uu.ub.cora.spider.record.IncomingLinksReader;
import se.uu.ub.cora.spider.spy.DataGroupTermCollectorSpy;
import se.uu.ub.cora.spider.spy.SpiderDependencyProviderSpy;
import se.uu.ub.cora.storage.spies.RecordStorageSpy;

public class IncomingLinksReaderTest {

	private IncomingLinksReader incomingLinksReader;

	private static final String SOME_AUTH_TOKEN = "someToken78678567";
	private static final String SOME_TYPE = "search";
	private static final String SOME_ID = "aSearchId";
	private LoggerFactorySpy loggerFactorySpy;
	private DataFactorySpy dataFactorySpy;
	private RecordStorageSpy recordStorage;
	private AuthenticatorSpy authenticator;
	private SpiderAuthorizatorSpy authorizator;
	private SpiderDependencyProviderSpy dependencyProvider;
	private DataGroupTermCollectorSpy termCollector;
	private ExtendedFunctionalityProviderSpy extendedFunctionalityProvider;
	private RecordTypeHandlerOldSpy recordTypeHandler;

	@BeforeMethod
	public void beforeMethod() {
		setUpFactoriesAndProviders();
		authenticator = new AuthenticatorSpy();
		authorizator = new SpiderAuthorizatorSpy();
		recordStorage = new RecordStorageSpy();
		recordStorage.MRV.setDefaultReturnValuesSupplier("read", DataRecordGroupSpy::new);
		termCollector = new DataGroupTermCollectorSpy();
		extendedFunctionalityProvider = new ExtendedFunctionalityProviderSpy();
		recordTypeHandler = new RecordTypeHandlerOldSpy();
		incomingLinksReader = setUpDependencyProvider();
	}

	private void setUpFactoriesAndProviders() {
		dataFactorySpy = new DataFactorySpy();
		DataProvider.onlyForTestSetDataFactory(dataFactorySpy);
		loggerFactorySpy = new LoggerFactorySpy();
		LoggerProvider.setLoggerFactory(loggerFactorySpy);
	}

	private IncomingLinksReader setUpDependencyProvider() {
		dependencyProvider = new SpiderDependencyProviderSpy();
		dependencyProvider.MRV.setDefaultReturnValuesSupplier("getAuthenticator",
				() -> authenticator);
		dependencyProvider.MRV.setDefaultReturnValuesSupplier("getSpiderAuthorizator",
				() -> authorizator);
		dependencyProvider.MRV.setDefaultReturnValuesSupplier("getRecordStorage",
				() -> recordStorage);
		dependencyProvider.MRV.setDefaultReturnValuesSupplier("getExtendedFunctionalityProvider",
				() -> extendedFunctionalityProvider);
		dependencyProvider.MRV.setDefaultReturnValuesSupplier("getDataGroupTermCollector",
				() -> termCollector);
		dependencyProvider.MRV.setDefaultReturnValuesSupplier(
				"getRecordTypeHandlerUsingDataRecordGroup", () -> recordTypeHandler);

		SpiderInstanceFactory factory = SpiderInstanceFactoryImp
				.usingDependencyProvider(dependencyProvider);
		SpiderInstanceProvider.setSpiderInstanceFactory(factory);

		return IncomingLinksReaderImp.usingDependencyProvider(dependencyProvider);
	}

	@Test
	public void testInitialize() throws Exception {
		dependencyProvider.MCR.assertMethodWasCalled("getAuthenticator");
		dependencyProvider.MCR.assertMethodWasCalled("getSpiderAuthorizator");
		dependencyProvider.MCR.assertMethodWasCalled("getRecordStorage");
		dependencyProvider.MCR.assertMethodWasCalled("getDataGroupTermCollector");
		dependencyProvider.MCR.assertMethodWasCalled("getExtendedFunctionalityProvider");
	}

	@Test
	public void testDeleteAuthorizedNoIncomingLinksCheckExternalDependenciesAreCalled() {
		incomingLinksReader.readIncomingLinks(SOME_AUTH_TOKEN, SOME_TYPE, SOME_ID);

		User user = (User) authenticator.MCR.getReturnValue("getUserForToken", 0);
		authorizator.MCR.assertParameters("checkUserIsAuthorizedForActionOnRecordType", 0, user,
				"read", SOME_TYPE);
	}

	@Test
	public void testReadIncomingLinksNoParentRecordTypeNoLinks() {
		DataListSpy linksPointingToRecord = (DataListSpy) incomingLinksReader
				.readIncomingLinks(SOME_AUTH_TOKEN, SOME_TYPE, SOME_ID);

		linksPointingToRecord.MCR.assertParameters("setFromNo", 0, "1");
		linksPointingToRecord.MCR.assertParameters("setToNo", 0, "0");
		linksPointingToRecord.MCR.assertParameters("setTotalNo", 0, "0");
		linksPointingToRecord.MCR.assertMethodNotCalled("addData");
	}

	@Test
	public void testStorageIsCalledToGetLinksToReturn() throws Exception {
		Set<Link> links = new LinkedHashSet<>();
		links.add(new Link("type0", "id0"));
		links.add(new Link("type1", "id1"));
		recordStorage.MRV.setDefaultReturnValuesSupplier("getLinksToRecord", () -> links);

		DataListSpy linksPointingToRecord = (DataListSpy) incomingLinksReader
				.readIncomingLinks(SOME_AUTH_TOKEN, SOME_TYPE, SOME_ID);

		recordTypeHandler.MCR.assertMethodNotCalled("getParentId");
		recordStorage.MCR.assertParameters("getLinksToRecord", 0, SOME_TYPE, SOME_ID);
		linksPointingToRecord.MCR.assertParameters("setTotalNo", 0, "2");
		linksPointingToRecord.MCR.assertParameters("setToNo", 0, "2");
		linksPointingToRecord.MCR.assertParameters("setFromNo", 0, "1");

		assertIncommingLinksRecord(linksPointingToRecord, 0);
		assertIncommingLinksRecord(linksPointingToRecord, 1);
	}

	private void assertIncommingLinksRecord(DataListSpy linksPointingToRecord, int linkNo) {
		dataFactorySpy.MCR.assertParameters("factorGroupUsingNameInData", linkNo,
				"recordToRecordLink");
		DataGroupSpy recordToRecordLink = (DataGroupSpy) dataFactorySpy.MCR
				.getReturnValue("factorGroupUsingNameInData", linkNo);
		linksPointingToRecord.MCR.assertParameters("addData", linkNo, recordToRecordLink);

		dataFactorySpy.MCR.assertParameters("factorRecordLinkUsingNameInDataAndTypeAndId",
				linkNo * 2, "from", "type" + linkNo, "id" + linkNo);
		DataRecordLinkSpy fFrom = (DataRecordLinkSpy) dataFactorySpy.MCR
				.getReturnValue("factorRecordLinkUsingNameInDataAndTypeAndId", linkNo * 2);
		recordToRecordLink.MCR.assertParameters("addChild", 0, fFrom);
		fFrom.MCR.assertParameters("addAction", 0, READ);

		dataFactorySpy.MCR.assertParameters("factorRecordLinkUsingNameInDataAndTypeAndId",
				linkNo * 2 + 1, "to", SOME_TYPE, SOME_ID);
		DataRecordLinkSpy fTo = (DataRecordLinkSpy) dataFactorySpy.MCR
				.getReturnValue("factorRecordLinkUsingNameInDataAndTypeAndId", linkNo * 2 + 1);
		recordToRecordLink.MCR.assertParameters("addChild", 1, fTo);
	}

	@Test
	public void testDependenciesAreConnectedCorrectly() throws Exception {
		DataList linksPointingToRecord = incomingLinksReader.readIncomingLinks(SOME_AUTH_TOKEN,
				SOME_TYPE, SOME_ID);

		var recordRead = assertReadOnStorage();

		dependencyProvider.MCR.assertParameters("getRecordTypeHandlerUsingDataRecordGroup", 0,
				recordRead);

		CollectTerms collectTerms = assertCollectTerms();

		var user = assertAuthenticator();
		assertAuthorization(collectTerms, user);

		dataFactorySpy.MCR.assertParameters("factorListUsingNameOfDataType", 0,
				"recordToRecordLink");
		dataFactorySpy.MCR.assertReturn("factorListUsingNameOfDataType", 0, linksPointingToRecord);
	}

	private void assertAuthorization(CollectTerms collectTerms, Object user) {
		authorizator.MCR.assertParameters(
				"checkUserIsAuthorizedForActionOnRecordTypeAndCollectedData", 0, user, "read",
				SOME_TYPE, collectTerms.permissionTerms);
	}

	private Object assertAuthenticator() {
		authenticator.MCR.assertParameters("getUserForToken", 0, SOME_AUTH_TOKEN);
		var user = authenticator.MCR.getReturnValue("getUserForToken", 0);
		return user;
	}

	private CollectTerms assertCollectTerms() {
		var metadataIdForType = recordTypeHandler.MCR.getReturnValue("getDefinitionId", 0);

		DataRecordGroup dataRecordGroup = getDataRecordGroupReadFromStorage(0);

		termCollector.MCR.assertParameters("collectTerms", 0, metadataIdForType, dataRecordGroup);
		CollectTerms collectTerms = (CollectTerms) termCollector.MCR.getReturnValue("collectTerms",
				0);
		return collectTerms;
	}

	private Object assertReadOnStorage() {
		recordStorage.MCR.assertParameters("read", 0, SOME_TYPE, SOME_ID);
		var recordRead = recordStorage.MCR.getReturnValue("read", 0);
		return recordRead;
	}

	private DataRecordGroup getDataRecordGroupReadFromStorage(int callNumber) {
		return (DataRecordGroup) recordStorage.MCR.getReturnValue("read", callNumber);
	}

	@Test
	public void testExtendedFunctionalitySetUp() {
		incomingLinksReader.readIncomingLinks(SOME_AUTH_TOKEN, SOME_TYPE, SOME_ID);

		extendedFunctionalityProvider.MCR.assertParameters(
				"getFunctionalityForPositionAndRecordType", 0, INCOMING_LINKS_AFTER_AUTHORIZATION,
				SOME_TYPE);
	}

	@Test
	public void testExtendedFunctionalityIsCalled() {
		incomingLinksReader.readIncomingLinks(SOME_AUTH_TOKEN, SOME_TYPE, SOME_ID);

		extendedFunctionalityProvider.assertCallToPositionAndFunctionalityCalledWithData(
				INCOMING_LINKS_AFTER_AUTHORIZATION, getExpectedDataForAfterAuthorization(), 0);
	}

	private ExtendedFunctionalityData getExpectedDataForAfterAuthorization() {
		ExtendedFunctionalityData expectedData = new ExtendedFunctionalityData();
		expectedData.recordType = SOME_TYPE;
		expectedData.recordId = SOME_ID;
		expectedData.authToken = SOME_AUTH_TOKEN;
		expectedData.user = (User) authenticator.MCR.getReturnValue("getUserForToken", 0);
		return expectedData;
	}

	@Test
	public void testEnsureExtendedFunctionalityPositionFor_AfterAuthorization() throws Exception {
		extendedFunctionalityProvider.setUpExtendedFunctionalityToThrowExceptionOnPosition(
				dependencyProvider, INCOMING_LINKS_AFTER_AUTHORIZATION, SOME_TYPE);

		callReadIncomingLinksAndCatchStopExecution();

		authorizator.MCR.assertMethodWasCalled("checkUserIsAuthorizedForActionOnRecordType");
		extendedFunctionalityProvider.MCR
				.assertNumberOfCallsToMethod("getFunctionalityForPositionAndRecordType", 1);
		recordStorage.MCR.assertMethodNotCalled("read");
	}

	private void callReadIncomingLinksAndCatchStopExecution() {
		try {
			incomingLinksReader.readIncomingLinks(SOME_AUTH_TOKEN, SOME_TYPE, SOME_ID);
			fail("Should fail as we want to stop execution when the extended functionality is used,"
					+ " to determin that the extended functionality is called in the correct place"
					+ " in the code");
		} catch (Exception e) {
		}
	}
}
