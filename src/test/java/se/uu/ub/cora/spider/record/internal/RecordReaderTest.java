/*
 * Copyright 2015, 2016, 2017, 2019, 2020, 2024 Uppsala University Library
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

import static org.testng.Assert.assertTrue;
import static org.testng.Assert.fail;
import static se.uu.ub.cora.spider.extendedfunctionality.ExtendedFunctionalityPosition.READ_AFTER_AUTHORIZATION;
import static se.uu.ub.cora.spider.extendedfunctionality.ExtendedFunctionalityPosition.READ_BEFORE_RETURN;

import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import se.uu.ub.cora.beefeater.authentication.User;
import se.uu.ub.cora.data.DataProvider;
import se.uu.ub.cora.data.DataRecord;
import se.uu.ub.cora.data.DataRecordGroup;
import se.uu.ub.cora.data.spies.DataFactorySpy;
import se.uu.ub.cora.data.spies.DataRecordGroupSpy;
import se.uu.ub.cora.logger.LoggerProvider;
import se.uu.ub.cora.spider.authorization.AuthorizationException;
import se.uu.ub.cora.spider.data.DataRecordOldSpy;
import se.uu.ub.cora.spider.dependency.SpiderInstanceFactory;
import se.uu.ub.cora.spider.dependency.SpiderInstanceFactoryImp;
import se.uu.ub.cora.spider.dependency.SpiderInstanceProvider;
import se.uu.ub.cora.spider.dependency.spy.RecordTypeHandlerSpy;
import se.uu.ub.cora.spider.extendedfunctionality.ExtendedFunctionalityData;
import se.uu.ub.cora.spider.extendedfunctionality.internal.ExtendedFunctionalityProviderSpy;
import se.uu.ub.cora.spider.log.LoggerFactorySpy;
import se.uu.ub.cora.spider.record.DataGroupToRecordEnhancerSpy;
import se.uu.ub.cora.spider.record.DataRedactorSpy;
import se.uu.ub.cora.spider.record.RecordReader;
import se.uu.ub.cora.spider.spy.SpiderDependencyProviderSpy;
import se.uu.ub.cora.storage.spies.RecordStorageSpy;

public class RecordReaderTest {
	private static final String SOME_RECORD_ID = "place:0001";
	private static final String SOME_USER_TOKEN = "someToken78678567";
	private static final String SOME_RECORD_TYPE = "someRecordType";
	private DataFactorySpy dataFactorySpy;
	private RecordStorageSpy recordStorage;
	private AuthenticatorSpy authenticator;
	private SpiderAuthorizatorSpy authorizator;
	private SpiderDependencyProviderSpy dependencyProvider;
	private RecordReader recordReader;
	private DataGroupToRecordEnhancerSpy dataGroupToRecordEnhancer;
	private LoggerFactorySpy loggerFactorySpy;
	private DataRedactorSpy dataRedactor;
	private ExtendedFunctionalityProviderSpy extendedFunctionalityProvider;
	private RecordTypeHandlerSpy recordTypeHandler;

	@BeforeMethod
	public void beforeMethod() {
		setUpFactoriesAndProviders();
		authenticator = new AuthenticatorSpy();
		authorizator = new SpiderAuthorizatorSpy();
		recordStorage = new RecordStorageSpy();
		recordStorage.MRV.setDefaultReturnValuesSupplier("read", DataRecordGroupSpy::new);
		dataRedactor = new DataRedactorSpy();
		extendedFunctionalityProvider = new ExtendedFunctionalityProviderSpy();
		recordTypeHandler = new RecordTypeHandlerSpy();

		recordReader = setUpDependencyProvider();
	}

	private void setUpFactoriesAndProviders() {
		loggerFactorySpy = new LoggerFactorySpy();
		LoggerProvider.setLoggerFactory(loggerFactorySpy);
		dataFactorySpy = new DataFactorySpy();
		DataProvider.onlyForTestSetDataFactory(dataFactorySpy);
	}

	private RecordReader setUpDependencyProvider() {
		dependencyProvider = new SpiderDependencyProviderSpy();
		dependencyProvider.MRV.setDefaultReturnValuesSupplier("getAuthenticator",
				() -> authenticator);
		dependencyProvider.MRV.setDefaultReturnValuesSupplier("getSpiderAuthorizator",
				() -> authorizator);
		dependencyProvider.MRV.setDefaultReturnValuesSupplier("getExtendedFunctionalityProvider",
				() -> extendedFunctionalityProvider);
		dependencyProvider.MRV.setDefaultReturnValuesSupplier("getRecordStorage",
				() -> recordStorage);
		dependencyProvider.MRV.setDefaultReturnValuesSupplier("getDataRedactor",
				() -> dataRedactor);
		dependencyProvider.MRV.setDefaultReturnValuesSupplier(
				"getRecordTypeHandlerUsingDataRecordGroup", () -> recordTypeHandler);
		dependencyProvider.MRV.setDefaultReturnValuesSupplier("getRecordTypeHandler",
				() -> recordTypeHandler);

		SpiderInstanceFactory factory = SpiderInstanceFactoryImp
				.usingDependencyProvider(dependencyProvider);
		SpiderInstanceProvider.setSpiderInstanceFactory(factory);

		dataGroupToRecordEnhancer = new DataGroupToRecordEnhancerSpy();
		return recordReader = RecordReaderImp.usingDependencyProviderAndDataGroupToRecordEnhancer(
				dependencyProvider, dataGroupToRecordEnhancer);
	}

	@Test
	public void testAuthenticatorIsCalledCorrectly() throws Exception {
		recordReader.readRecord(SOME_USER_TOKEN, SOME_RECORD_TYPE, SOME_RECORD_ID);

		authenticator.MCR.assertParameters("getUserForToken", 0, SOME_USER_TOKEN);

		recordStorage.MCR.assertParameters("read", 0, SOME_RECORD_TYPE, SOME_RECORD_ID);
		var dataRecordGroup = recordStorage.MCR.getReturnValue("read", 0);

		dependencyProvider.MCR.assertParameters("getRecordTypeHandler", 0, SOME_RECORD_TYPE);
		// dependencyProvider.MCR.assertParameters("getRecordTypeHandlerUsingDataRecordGroup", 0,
		// dataRecordGroup);

		recordTypeHandler.MCR.assertMethodWasCalled("isPublicForRead");
		authorizator.MCR.assertMethodWasCalled("checkUserIsAuthorizedForActionOnRecordType");

		dataGroupToRecordEnhancer.MCR.assertMethodWasCalled("enhance");
	}

	@Test
	public void testReadNotPublicAndNoAccessToRead() throws Exception {
		authorizator.MRV.setAlwaysThrowException("checkUserIsAuthorizedForActionOnRecordType",
				new AuthorizationException("someError"));

		try {
			recordReader.readRecord(SOME_USER_TOKEN, SOME_RECORD_TYPE, SOME_RECORD_ID);
			fail("it should throw exception");
		} catch (Exception e) {
			User user = (User) authenticator.MCR.getReturnValue("getUserForToken", 0);
			assertTrue(e instanceof AuthorizationException);
			dependencyProvider.MCR.assertMethodWasCalled("getRecordTypeHandler");
			recordTypeHandler.MCR.assertMethodWasCalled("isPublicForRead");

			authorizator.MCR.assertParameters("checkUserIsAuthorizedForActionOnRecordType", 0, user,
					"read", SOME_RECORD_TYPE);

			recordTypeHandler.MCR.assertReturn("isPublicForRead", 0, false);
			authorizator.MCR.assertMethodWasCalled("checkUserIsAuthorizedForActionOnRecordType");
			dataGroupToRecordEnhancer.MCR.assertMethodNotCalled("enhance");
		}

	}

	@Test
	public void testReadIsPublicAuthorizationShouldNotBeChecked() throws Exception {
		recordTypeHandler.isPublicForRead = true;
		authorizator.MRV.setAlwaysThrowException("checkUserIsAuthorizedForActionOnRecordType",
				new AuthorizationException("someError"));

		recordReader.readRecord(SOME_USER_TOKEN, SOME_RECORD_TYPE, SOME_RECORD_ID);

		recordTypeHandler.MCR.assertReturn("isPublicForRead", 0, true);
		authorizator.MCR.assertMethodNotCalled("checkUserIsAuthorizedForActionOnRecordType");
		recordStorage.MCR.assertMethodWasCalled("read");
		dataGroupToRecordEnhancer.MCR.assertMethodWasCalled("enhance");
	}

	@Test
	public void testReadNotPublicButAuthorized() throws Exception {
		recordReader.readRecord(SOME_USER_TOKEN, SOME_RECORD_TYPE, SOME_RECORD_ID);

		recordTypeHandler.MCR.assertReturn("isPublicForRead", 0, false);
		authorizator.MCR.assertMethodWasCalled("checkUserIsAuthorizedForActionOnRecordType");
		recordStorage.MCR.assertMethodWasCalled("read");
		dataGroupToRecordEnhancer.MCR.assertMethodWasCalled("enhance");
	}

	@Test
	public void testStorageIsCalledCorrectly() throws Exception {
		recordReader.readRecord(SOME_USER_TOKEN, SOME_RECORD_TYPE, SOME_RECORD_ID);

		recordStorage.MCR.assertParameters("read", 0, SOME_RECORD_TYPE, SOME_RECORD_ID);
	}

	@Test
	public void testEnhancerCalledCorrectly() throws Exception {
		recordReader.readRecord(SOME_USER_TOKEN, SOME_RECORD_TYPE, SOME_RECORD_ID);

		User user = (User) authenticator.MCR.getReturnValue("getUserForToken", 0);
		DataRecordGroup dataGroupFromStorage = (DataRecordGroup) recordStorage.MCR
				.getReturnValue("read", 0);

		dataFactorySpy.MCR.assertParameters("factorGroupFromDataRecordGroup", 0,
				dataGroupFromStorage);
		var dataGroupFromDataProvider = dataFactorySpy.MCR
				.getReturnValue("factorGroupFromDataRecordGroup", 0);
		dataGroupToRecordEnhancer.MCR.assertParameters("enhance", 0, user, SOME_RECORD_TYPE,
				dataGroupFromDataProvider, dataRedactor);
	}

	@Test
	public void testAnswerFromEnhancerIsReturned() throws Exception {
		DataRecord readRecord = recordReader.readRecord(SOME_USER_TOKEN, SOME_RECORD_TYPE,
				SOME_RECORD_ID);

		dataGroupToRecordEnhancer.MCR.assertReturn("enhance", 0, readRecord);
	}

	@Test
	public void testUseExtendedFunctionalityExtendedFunctionalitiesExists() throws Exception {
		recordReader.readRecord(SOME_USER_TOKEN, SOME_RECORD_TYPE, SOME_RECORD_ID);

		ExtendedFunctionalityData expectedData = new ExtendedFunctionalityData();
		expectedData.recordType = SOME_RECORD_TYPE;
		expectedData.recordId = SOME_RECORD_ID;
		expectedData.authToken = SOME_USER_TOKEN;
		expectedData.user = (User) authenticator.MCR.getReturnValue("getUserForToken", 0);
		expectedData.previouslyStoredTopDataGroup = null;
		extendedFunctionalityProvider.assertCallToPositionAndFunctionalityCalledWithData(
				READ_AFTER_AUTHORIZATION, expectedData, 0);

		DataRecordOldSpy enhancedRecord = (DataRecordOldSpy) dataGroupToRecordEnhancer.MCR
				.getReturnValue("enhance", 0);
		expectedData.dataGroup = enhancedRecord.getDataRecordGroup();
		expectedData.dataRecord = enhancedRecord;
		extendedFunctionalityProvider.assertCallToPositionAndFunctionalityCalledWithData(
				READ_BEFORE_RETURN, expectedData, 1);
	}

	@Test
	public void testEnsureExtendedFunctionalityPosition_AfterAuthorizathion() throws Exception {
		extendedFunctionalityProvider.setUpExtendedFunctionalityToThrowExceptionOnPosition(
				dependencyProvider, READ_AFTER_AUTHORIZATION, SOME_RECORD_TYPE);

		callReadRecordAndCatchStopExecution();

		authorizator.MCR.assertMethodWasCalled("checkUserIsAuthorizedForActionOnRecordType");
		dataFactorySpy.MCR.assertMethodNotCalled("factorRecordGroupFromDataGroup");
	}

	@Test
	public void testEnsureExtendedFunctionalityPosition_BeforeReturn() throws Exception {
		extendedFunctionalityProvider.setUpExtendedFunctionalityToThrowExceptionOnPosition(
				dependencyProvider, READ_BEFORE_RETURN, SOME_RECORD_TYPE);

		callReadRecordAndCatchStopExecution();

		dataGroupToRecordEnhancer.MCR.assertMethodWasCalled("enhance");
	}

	private void callReadRecordAndCatchStopExecution() {
		try {
			recordReader.readRecord(SOME_USER_TOKEN, SOME_RECORD_TYPE, SOME_RECORD_ID);
			fail("Should fail as we want to stop execution when the extended functionality is used,"
					+ " to determin that the extended functionality is called in the correct place"
					+ " in the code");
		} catch (Exception e) {
		}
	}
}
