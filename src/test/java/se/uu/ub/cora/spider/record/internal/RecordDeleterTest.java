/*
 * Copyright 2015, 2019, 2024, 2025 Uppsala University Library
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

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertTrue;
import static org.testng.Assert.fail;
import static se.uu.ub.cora.spider.extendedfunctionality.ExtendedFunctionalityPosition.DELETE_AFTER;
import static se.uu.ub.cora.spider.extendedfunctionality.ExtendedFunctionalityPosition.DELETE_AFTER_AUTHORIZATION;
import static se.uu.ub.cora.spider.extendedfunctionality.ExtendedFunctionalityPosition.DELETE_BEFORE;

import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import se.uu.ub.cora.beefeater.authentication.User;
import se.uu.ub.cora.data.DataProvider;
import se.uu.ub.cora.data.DataRecordGroup;
import se.uu.ub.cora.data.collected.CollectTerms;
import se.uu.ub.cora.data.spies.DataFactorySpy;
import se.uu.ub.cora.data.spies.DataRecordGroupSpy;
import se.uu.ub.cora.logger.LoggerProvider;
import se.uu.ub.cora.spider.dependency.SpiderInstanceFactory;
import se.uu.ub.cora.spider.dependency.SpiderInstanceFactoryImp;
import se.uu.ub.cora.spider.dependency.SpiderInstanceProvider;
import se.uu.ub.cora.spider.dependency.spy.RecordTypeHandlerSpy;
import se.uu.ub.cora.spider.extendedfunctionality.ExtendedFunctionalityData;
import se.uu.ub.cora.spider.extendedfunctionality.internal.ExtendedFunctionalityProviderSpy;
import se.uu.ub.cora.spider.log.LoggerFactorySpy;
import se.uu.ub.cora.spider.record.MisuseException;
import se.uu.ub.cora.spider.record.RecordDeleter;
import se.uu.ub.cora.spider.record.RecordNotFoundException;
import se.uu.ub.cora.spider.spy.DataChangedSenderSpy;
import se.uu.ub.cora.spider.spy.DataGroupTermCollectorSpy;
import se.uu.ub.cora.spider.spy.RecordIndexerSpy;
import se.uu.ub.cora.spider.spy.SpiderDependencyProviderSpy;
import se.uu.ub.cora.storage.spies.RecordStorageSpy;
import se.uu.ub.cora.storage.spies.archive.RecordArchiveSpy;

public class RecordDeleterTest {
	private RecordDeleter recordDeleter;

	private static final String SOME_AUTH_TOKEN = "someAuthToken";
	private static final String SOME_TYPE = "someType";
	private static final String SOME_ID = "someId";
	private LoggerFactorySpy loggerFactorySpy;
	private DataFactorySpy dataFactorySpy;
	private RecordStorageSpy recordStorage;
	private RecordArchiveSpy recordArchive;
	private AuthenticatorSpy authenticator;
	private SpiderAuthorizatorSpy authorizator;
	private SpiderDependencyProviderSpy dependencyProvider;
	private RecordIndexerSpy recordIndexer;
	private DataGroupTermCollectorSpy termCollector;
	private ExtendedFunctionalityProviderSpy extendedFunctionalityProvider;
	private RecordTypeHandlerSpy recordTypeHandler;

	@BeforeMethod
	public void beforeMethod() {
		setUpFactoriesAndProviders();
		authenticator = new AuthenticatorSpy();
		authorizator = new SpiderAuthorizatorSpy();
		recordStorage = new RecordStorageSpy();
		recordArchive = new RecordArchiveSpy();
		recordStorage.MRV.setDefaultReturnValuesSupplier("read", DataRecordGroupSpy::new);
		recordIndexer = new RecordIndexerSpy();
		termCollector = new DataGroupTermCollectorSpy();
		extendedFunctionalityProvider = new ExtendedFunctionalityProviderSpy();
		recordTypeHandler = new RecordTypeHandlerSpy();
		setUpDependencyProvider();
	}

	private void setUpFactoriesAndProviders() {
		loggerFactorySpy = new LoggerFactorySpy();
		LoggerProvider.setLoggerFactory(loggerFactorySpy);
		dataFactorySpy = new DataFactorySpy();
		DataProvider.onlyForTestSetDataFactory(dataFactorySpy);
	}

	private void setUpDependencyProvider() {
		dependencyProvider = new SpiderDependencyProviderSpy();
		dependencyProvider.MRV.setDefaultReturnValuesSupplier("getAuthenticator",
				() -> authenticator);
		dependencyProvider.MRV.setDefaultReturnValuesSupplier("getSpiderAuthorizator",
				() -> authorizator);
		dependencyProvider.MRV.setDefaultReturnValuesSupplier("getRecordStorage",
				() -> recordStorage);
		dependencyProvider.MRV.setDefaultReturnValuesSupplier("getRecordArchive",
				() -> recordArchive);
		dependencyProvider.MRV.setDefaultReturnValuesSupplier("getExtendedFunctionalityProvider",
				() -> extendedFunctionalityProvider);
		dependencyProvider.MRV.setDefaultReturnValuesSupplier("getRecordIndexer",
				() -> recordIndexer);
		dependencyProvider.MRV.setDefaultReturnValuesSupplier("getDataGroupTermCollector",
				() -> termCollector);
		dependencyProvider.MRV.setDefaultReturnValuesSupplier(
				"getRecordTypeHandlerUsingDataRecordGroup", () -> recordTypeHandler);

		SpiderInstanceFactory factory = SpiderInstanceFactoryImp
				.usingDependencyProvider(dependencyProvider);
		SpiderInstanceProvider.setSpiderInstanceFactory(factory);

		recordDeleter = RecordDeleterImp.usingDependencyProvider(dependencyProvider);
	}

	@Test
	public void testDeleteAuthorizedNoIncomingLinks() {

		recordDeleter.deleteRecord(SOME_AUTH_TOKEN, SOME_TYPE, SOME_ID);

		recordStorage.MCR.assertParameters("read", 0, SOME_TYPE, SOME_ID);
		recordStorage.MCR.assertParameters("deleteByTypeAndId", 0, SOME_TYPE, SOME_ID);
		recordIndexer.MCR.assertParameters("deleteFromIndex", 0, SOME_TYPE, SOME_ID);
	}

	@Test
	public void testAuthenticatorCalled() {
		recordDeleter.deleteRecord(SOME_AUTH_TOKEN, SOME_TYPE, SOME_ID);

		authenticator.MCR.assertParameters("getUserForToken", 0, SOME_AUTH_TOKEN);
	}

	@Test
	public void testDeleteAuthorizedNoIncomingLinksCheckExternalDependenciesAreCalled() {
		recordDeleter.deleteRecord(SOME_AUTH_TOKEN, SOME_TYPE, SOME_ID);

		User user = (User) authenticator.MCR.getReturnValue("getUserForToken", 0);
		String methodName = "checkUserIsAuthorizedForActionOnRecordTypeAndCollectedData";
		CollectTerms collectTerms = (CollectTerms) termCollector.MCR.getReturnValue("collectTerms",
				0);
		authorizator.MCR.assertParameters(methodName, 0, user, "delete", SOME_TYPE,
				collectTerms.permissionTerms);
	}

	@Test
	public void testGetCollectTerms() {

		recordDeleter.deleteRecord(SOME_AUTH_TOKEN, SOME_TYPE, SOME_ID);

		dependencyProvider.MCR.assertParameters("getRecordTypeHandlerUsingDataRecordGroup", 0,
				getReadDataRecordGroup());

		String definitionId = (String) recordTypeHandler.MCR.getReturnValue("getDefinitionId", 0);
		var readDataGroup = recordStorage.MCR.assertCalledParametersReturn("read", SOME_TYPE,
				SOME_ID);

		termCollector.MCR.assertParameters("collectTerms", 0, definitionId, readDataGroup);
	}

	private DataRecordGroup getReadDataRecordGroup() {
		return (DataRecordGroup) recordStorage.MCR.getReturnValue("read", 0);
	}

	@Test
	public void testExtendedFunctionalitySetUp() {
		recordDeleter.deleteRecord(SOME_AUTH_TOKEN, SOME_TYPE, SOME_ID);

		extendedFunctionalityProvider.MCR.assertParameters(
				"getFunctionalityForPositionAndRecordType", 0, DELETE_AFTER_AUTHORIZATION,
				SOME_TYPE);
		extendedFunctionalityProvider.MCR.assertParameters(
				"getFunctionalityForPositionAndRecordType", 1, DELETE_BEFORE, SOME_TYPE);

		extendedFunctionalityProvider.MCR.assertParameters(
				"getFunctionalityForPositionAndRecordType", 2, DELETE_AFTER, SOME_TYPE);
	}

	@Test
	public void testExtendedFunctionalityIsCalled() {
		recordDeleter.deleteRecord(SOME_AUTH_TOKEN, SOME_TYPE, SOME_ID);

		extendedFunctionalityProvider.assertCallToPositionAndFunctionalityCalledWithData(
				DELETE_AFTER_AUTHORIZATION, getExpectedDataForDeleteAfterAuthorization(), 0);
		extendedFunctionalityProvider.assertCallToPositionAndFunctionalityCalledWithData(
				DELETE_BEFORE, getExpectedDataUsingDataProviderCallNumber(0), 1);
		extendedFunctionalityProvider.assertCallToPositionAndFunctionalityCalledWithData(
				DELETE_AFTER, getExpectedDataUsingDataProviderCallNumber(1), 2);
	}

	private ExtendedFunctionalityData getExpectedDataForDeleteAfterAuthorization() {
		ExtendedFunctionalityData expectedData = new ExtendedFunctionalityData();
		expectedData.recordType = SOME_TYPE;
		expectedData.recordId = SOME_ID;
		expectedData.authToken = SOME_AUTH_TOKEN;
		expectedData.user = (User) authenticator.MCR.getReturnValue("getUserForToken", 0);
		return expectedData;
	}

	private ExtendedFunctionalityData getExpectedDataUsingDataProviderCallNumber(
			int dataProviderCallNumber) {
		ExtendedFunctionalityData expectedData = new ExtendedFunctionalityData();
		expectedData.recordType = SOME_TYPE;
		expectedData.recordId = SOME_ID;
		expectedData.authToken = SOME_AUTH_TOKEN;
		expectedData.user = (User) authenticator.MCR.getReturnValue("getUserForToken", 0);
		expectedData.dataRecordGroup = getReadDataRecordGroup();
		return expectedData;
	}

	@Test
	public void testEnsureExtendedFunctionalityPositionFor_AfterAuthorization() {
		extendedFunctionalityProvider.setUpExtendedFunctionalityToThrowExceptionOnPosition(
				dependencyProvider, DELETE_AFTER_AUTHORIZATION, SOME_TYPE);

		callDeleteRecordAndCatchStopExecution();

		authorizator.MCR.assertMethodWasCalled("checkUserIsAuthorizedForActionOnRecordType");
		extendedFunctionalityProvider.MCR
				.assertNumberOfCallsToMethod("getFunctionalityForPositionAndRecordType", 1);
		recordStorage.MCR.assertMethodNotCalled("read");
	}

	private void callDeleteRecordAndCatchStopExecution() {
		try {
			recordDeleter.deleteRecord(SOME_AUTH_TOKEN, SOME_TYPE, SOME_ID);
			fail("Should fail as we want to stop execution when the extended functionality is used,"
					+ " to determin that the extended functionality is called in the correct place"
					+ " in the code");
		} catch (Exception e) {
		}
	}

	@Test
	public void testEnsureExtendedFunctionalityPositionFor_DeleteBefore() {
		extendedFunctionalityProvider.setUpExtendedFunctionalityToThrowExceptionOnPosition(
				dependencyProvider, DELETE_BEFORE, SOME_TYPE);

		callDeleteRecordAndCatchStopExecution();

		recordStorage.MCR.assertMethodWasCalled("linksExistForRecord");
		extendedFunctionalityProvider.MCR
				.assertNumberOfCallsToMethod("getFunctionalityForPositionAndRecordType", 2);
		recordStorage.MCR.assertMethodNotCalled("deleteByTypeAndId");
	}

	@Test
	public void testEnsureExtendedFunctionalityPositionFor_DeleteAfter() {
		extendedFunctionalityProvider.setUpExtendedFunctionalityToThrowExceptionOnPosition(
				dependencyProvider, DELETE_AFTER, SOME_TYPE);

		callDeleteRecordAndCatchStopExecution();

		recordIndexer.MCR.assertMethodWasCalled("deleteFromIndex");
		extendedFunctionalityProvider.MCR
				.assertNumberOfCallsToMethod("getFunctionalityForPositionAndRecordType", 3);
	}

	@Test(expectedExceptions = MisuseException.class, expectedExceptionsMessageRegExp = "Record "
			+ "with type: " + SOME_TYPE + " and id: " + SOME_ID + " could not be deleted since "
			+ "other records are linking to it")
	public void testCouldNotDeleteSinceItHasIncomingLinks() {
		recordStorage.MRV.setDefaultReturnValuesSupplier("linksExistForRecord", () -> true);

		recordDeleter.deleteRecord(SOME_AUTH_TOKEN, SOME_TYPE, SOME_ID);
	}

	@Test
	public void testDelete_DeleteRecordOnStorage_NotFound() {
		recordStorage.MRV.setAlwaysThrowException("deleteByTypeAndId",
				se.uu.ub.cora.storage.RecordNotFoundException.withMessage("someError"));

		try {
			recordDeleter.deleteRecord(SOME_AUTH_TOKEN, SOME_TYPE, SOME_ID);
			fail("It should throw exception");
		} catch (Exception e) {
			assertTrue(e instanceof RecordNotFoundException);
			assertEquals(e.getMessage(),
					"Record with type: someType and id: someId could not be deleted.");
			assertEquals(e.getCause().getMessage(), "someError");
		}
	}

	@Test
	public void testDelete_ReadOnStorage_NotFound() {
		recordStorage.MRV.setAlwaysThrowException("read",
				se.uu.ub.cora.storage.RecordNotFoundException.withMessage("someError"));

		try {
			recordDeleter.deleteRecord(SOME_AUTH_TOKEN, SOME_TYPE, SOME_ID);
			fail("It should throw exception");
		} catch (Exception e) {
			assertTrue(e instanceof RecordNotFoundException);
			assertEquals(e.getMessage(),
					"Record with type: someType and id: someId could not be deleted.");
			assertEquals(e.getCause().getMessage(), "someError");
		}
	}

	@Test
	public void testOnlyForTestGetDependencyProvider() {
		RecordDeleterImp recordDeleterImp = (RecordDeleterImp) recordDeleter;
		assertEquals(recordDeleterImp.onlyForTestGetDependencyProvider(), dependencyProvider);

	}

	@Test
	public void testCallSendDataChanged() {
		recordDeleter.deleteRecord(SOME_AUTH_TOKEN, SOME_TYPE, SOME_ID);

		var sender = getDataChangedSender();
		sender.MCR.assertParameters("sendDataChanged", 0, SOME_TYPE, SOME_ID, "delete");
	}

	private DataChangedSenderSpy getDataChangedSender() {
		return (DataChangedSenderSpy) dependencyProvider.MCR
				.assertCalledParametersReturn("getDataChangeSender");
	}

	@Test
	public void testSendDataChangedAfterStoreInStorage() {
		recordStorage.MRV.setAlwaysThrowException("deleteByTypeAndId",
				new RuntimeException("someError"));

		try {
			recordDeleter.deleteRecord(SOME_AUTH_TOKEN, SOME_TYPE, SOME_ID);
			fail();
		} catch (Exception e) {
			dependencyProvider.MCR.assertMethodNotCalled("getDataChangeSender");
		}
	}

	@Test
	public void testSendDataChangedBeforeStoreInArchive() {
		recordTypeHandler.MRV.setDefaultReturnValuesSupplier("storeInArchive", () -> true);
		recordArchive.MRV.setAlwaysThrowException("delete", new RuntimeException("someError"));

		try {
			recordDeleter.deleteRecord(SOME_AUTH_TOKEN, SOME_TYPE, SOME_ID);
			fail();
		} catch (Exception e) {
			dependencyProvider.MCR.assertMethodWasCalled("getDataChangeSender");
		}
	}

	@Test
	public void deleteFromArchive_NotSetToBeStoredInArchive() {
		recordTypeHandler.MRV.setDefaultReturnValuesSupplier("storeInArchive", () -> false);

		recordDeleter.deleteRecord(SOME_AUTH_TOKEN, SOME_TYPE, SOME_ID);

		recordArchive.MCR.assertMethodNotCalled("delete");
	}

	@Test
	public void deleteFromArchive() {
		recordTypeHandler.MRV.setDefaultReturnValuesSupplier("storeInArchive", () -> true);

		DataRecordGroupSpy dataRecordGroup = new DataRecordGroupSpy();
		dataRecordGroup.MRV.setDefaultReturnValuesSupplier("getDataDivider",
				() -> "someDataDivider");
		recordStorage.MRV.setDefaultReturnValuesSupplier("read", () -> dataRecordGroup);

		recordDeleter.deleteRecord(SOME_AUTH_TOKEN, SOME_TYPE, SOME_ID);

		recordArchive.MCR.assertParameters("delete", 0, "someDataDivider", SOME_TYPE, SOME_ID);
	}

	@Test
	public void deletefromArchive_NotFound() {
		recordTypeHandler.MRV.setDefaultReturnValuesSupplier("storeInArchive", () -> true);
		recordArchive.MRV.setAlwaysThrowException("delete",
				se.uu.ub.cora.storage.RecordNotFoundException.withMessage("someError"));

		try {
			recordDeleter.deleteRecord(SOME_AUTH_TOKEN, SOME_TYPE, SOME_ID);
			fail("It should throw exception");
		} catch (Exception e) {
			assertTrue(e instanceof RecordNotFoundException);
			assertEquals(e.getMessage(),
					"Record with type: someType and id: someId could not be deleted.");
			assertEquals(e.getCause().getMessage(), "someError");
		}
	}
}
