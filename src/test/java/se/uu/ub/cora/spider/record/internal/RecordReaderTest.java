/*
 * Copyright 2015, 2016, 2017, 2019, 2020, 2024, 2026 Uppsala University Library
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
import static se.uu.ub.cora.spider.extendedfunctionality.ExtendedFunctionalityPosition.READ_BEFORE_ENHANCE;
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
import se.uu.ub.cora.spider.dependency.spy.RecordTypeHandlerSpy;
import se.uu.ub.cora.spider.extendedfunctionality.ExtendedFunctionalityData;
import se.uu.ub.cora.spider.extendedfunctionality.internal.ExtendedFunctionalityProviderSpy;
import se.uu.ub.cora.spider.log.LoggerFactorySpy;
import se.uu.ub.cora.spider.record.DataGroupToRecordEnhancerSpy;
import se.uu.ub.cora.spider.record.DataRedactorOldSpy;
import se.uu.ub.cora.spider.record.RecordReader;
import se.uu.ub.cora.spider.spy.SpiderDependencyProviderSpy;
import se.uu.ub.cora.spider.spy.SecurityControlSpy;
import se.uu.ub.cora.storage.spies.RecordStorageSpy;

public class RecordReaderTest {
	private static final String RECORD_ID = "place:0001";
	private static final String USER_TOKEN = "someToken78678567";
	private static final String RECORD_TYPE = "someRecordType";
	private DataFactorySpy dataFactorySpy;
	private RecordStorageSpy recordStorage;
	private SecurityControlSpy securityControl;
	private SpiderDependencyProviderSpy dependencyProvider;
	private RecordReader recordReader;
	private DataGroupToRecordEnhancerSpy dataGroupToRecordEnhancer;
	private LoggerFactorySpy loggerFactorySpy;
	private DataRedactorOldSpy dataRedactor;
	private ExtendedFunctionalityProviderSpy extendedFunctionalityProvider;
	private RecordTypeHandlerSpy recordTypeHandler;
	private DataRecordGroupSpy dataRecordGroupFromStorage;

	@BeforeMethod
	public void beforeMethod() {
		setUpFactoriesAndProviders();
		setUpDependencyProvider();
		setUpRecordStorage();

		recordReader = createRecordReader();
	}

	private void setUpFactoriesAndProviders() {
		loggerFactorySpy = new LoggerFactorySpy();
		LoggerProvider.setLoggerFactory(loggerFactorySpy);
		dataFactorySpy = new DataFactorySpy();
		DataProvider.onlyForTestSetDataFactory(dataFactorySpy);
	}

	private void setUpRecordStorage() {
		recordStorage = new RecordStorageSpy();
		dataRecordGroupFromStorage = new DataRecordGroupSpy();
		recordStorage.MRV.setDefaultReturnValuesSupplier("read", () -> dataRecordGroupFromStorage);
	}

	private void setUpDependencyProvider() {
		dependencyProvider = new SpiderDependencyProviderSpy();

		securityControl = new SecurityControlSpy();
		dependencyProvider.MRV.setDefaultReturnValuesSupplier("getSecurityControl",
				() -> securityControl);

		extendedFunctionalityProvider = new ExtendedFunctionalityProviderSpy();
		dependencyProvider.MRV.setDefaultReturnValuesSupplier("getExtendedFunctionalityProvider",
				() -> extendedFunctionalityProvider);
		dependencyProvider.MRV.setDefaultReturnValuesSupplier("getRecordStorage",
				() -> recordStorage);

		dataRedactor = new DataRedactorOldSpy();
		dependencyProvider.MRV.setDefaultReturnValuesSupplier("getDataRedactor",
				() -> dataRedactor);

		recordTypeHandler = new RecordTypeHandlerSpy();
		dependencyProvider.MRV.setDefaultReturnValuesSupplier(
				"getRecordTypeHandlerUsingDataRecordGroup", () -> recordTypeHandler);
		dependencyProvider.MRV.setDefaultReturnValuesSupplier("getRecordTypeHandler",
				() -> recordTypeHandler);
	}

	private RecordReader createRecordReader() {
		var createdRecordReader = RecordReaderImp.usingDependencyProvider(dependencyProvider);
		dataGroupToRecordEnhancer = (DataGroupToRecordEnhancerSpy) dependencyProvider.MCR
				.getReturnValue("getDataGroupToRecordEnhancer", 0);
		return createdRecordReader;
	}

	@Test
	public void testAuthenticatorIsCalledCorrectly() {
		recordReader.readRecord(USER_TOKEN, RECORD_TYPE, RECORD_ID);

		securityControl.MCR.assertParameters("checkActionAuthorizationForUser", 0, USER_TOKEN,
				RECORD_TYPE, "read");

		recordStorage.MCR.assertParameters("read", 0, RECORD_TYPE, RECORD_ID);
		recordStorage.MCR.getReturnValue("read", 0);

		securityControl.MCR.assertMethodWasCalled("checkActionAuthorizationForUser");

		dataGroupToRecordEnhancer.MCR.assertMethodWasCalled("enhance");
	}

	@Test
	public void testReadNotPublicAndNoAccessToRead() {
		securityControl.MRV.setAlwaysThrowException("checkActionAuthorizationForUser",
				new AuthorizationException("someError"));

		try {
			recordReader.readRecord(USER_TOKEN, RECORD_TYPE, RECORD_ID);
			fail("it should throw exception");
		} catch (Exception e) {
			assertTrue(e instanceof AuthorizationException);
			securityControl.MCR.assertParameters("checkActionAuthorizationForUser", 0, USER_TOKEN,
					RECORD_TYPE, "read");
			dataGroupToRecordEnhancer.MCR.assertMethodNotCalled("enhance");
		}

	}

	@Test
	public void testReadIsPublicAuthorizationShouldNotBeChecked() {
		recordTypeHandler.MRV.setDefaultReturnValuesSupplier("isPublicForRead", () -> true);

		recordReader.readRecord(USER_TOKEN, RECORD_TYPE, RECORD_ID);

		User user = (User) securityControl.MCR.getReturnValue("checkActionAuthorizationForUser", 0);
		var readRecord = recordStorage.MCR.assertCalledParametersReturn("read", RECORD_TYPE,
				RECORD_ID);
		var redactor = dependencyProvider.MCR.assertCalledParametersReturn("getDataRedactor");
		dataGroupToRecordEnhancer.MCR.assertCalledParameters("enhance", user, RECORD_TYPE,
				readRecord, redactor);
	}

	@Test
	public void testReadNotPublicButAuthorized() {
		recordReader.readRecord(USER_TOKEN, RECORD_TYPE, RECORD_ID);

		securityControl.MCR.assertMethodWasCalled("checkActionAuthorizationForUser");
		recordStorage.MCR.assertMethodWasCalled("read");
		dataGroupToRecordEnhancer.MCR.assertMethodWasCalled("enhance");
	}

	@Test
	public void testStorageIsCalledCorrectly() {
		recordReader.readRecord(USER_TOKEN, RECORD_TYPE, RECORD_ID);

		recordStorage.MCR.assertParameters("read", 0, RECORD_TYPE, RECORD_ID);
	}

	@Test
	public void testEnhancerCalledCorrectly() {
		recordReader.readRecord(USER_TOKEN, RECORD_TYPE, RECORD_ID);

		User user = (User) securityControl.MCR.getReturnValue("checkActionAuthorizationForUser", 0);
		DataRecordGroup dataGroupFromStorage = getDataRecordGroupFromStorage();
		dataGroupToRecordEnhancer.MCR.assertParameters("enhance", 0, user, RECORD_TYPE,
				dataGroupFromStorage, dataRedactor);
	}

	@Test
	public void testAnswerFromEnhancerIsReturned() {
		DataRecord readRecord = recordReader.readRecord(USER_TOKEN, RECORD_TYPE, RECORD_ID);

		dataGroupToRecordEnhancer.MCR.assertReturn("enhance", 0, readRecord);
	}

	@Test
	public void testUseExtendedFunctionalityExtendedFunctionalitiesExists() {
		recordReader.readRecord(USER_TOKEN, RECORD_TYPE, RECORD_ID);

		ExtendedFunctionalityData expectedData = new ExtendedFunctionalityData();
		expectedData.recordType = RECORD_TYPE;
		expectedData.recordId = RECORD_ID;
		expectedData.authToken = USER_TOKEN;
		expectedData.user = (User) securityControl.MCR.getReturnValue("checkActionAuthorizationForUser", 0);
		extendedFunctionalityProvider.assertCallToPositionAndFunctionalityCalledWithData(
				READ_AFTER_AUTHORIZATION, expectedData, 0);

		expectedData.dataRecordGroup = getDataRecordGroupFromStorage();
		extendedFunctionalityProvider.assertCallToPositionAndFunctionalityCalledWithData(
				READ_BEFORE_ENHANCE, expectedData, 1);

		DataRecordOldSpy enhancedRecord = getDataRecordFromEnhancer();
		expectedData.dataRecordGroup = enhancedRecord.getDataRecordGroup();
		expectedData.dataRecord = enhancedRecord;
		extendedFunctionalityProvider.assertCallToPositionAndFunctionalityCalledWithData(
				READ_BEFORE_RETURN, expectedData, 2);
	}

	private DataRecordOldSpy getDataRecordFromEnhancer() {
		return (DataRecordOldSpy) dataGroupToRecordEnhancer.MCR.getReturnValue("enhance", 0);
	}

	private DataRecordGroup getDataRecordGroupFromStorage() {
		return (DataRecordGroup) recordStorage.MCR.getReturnValue("read", 0);
	}

	@Test
	public void testEnsureExtendedFunctionalityPosition_AfterAuthorizathion() {
		extendedFunctionalityProvider.setUpExtendedFunctionalityToThrowExceptionOnPosition(
				dependencyProvider, READ_AFTER_AUTHORIZATION, RECORD_TYPE);

		callReadRecordAndCatchStopExecution();

		securityControl.MCR.assertMethodWasCalled("checkActionAuthorizationForUser");
		dataFactorySpy.MCR.assertMethodNotCalled("factorRecordGroupFromDataGroup");
	}

	@Test
	public void testEnsureExtendedFunctionalityPosition_BeforeReturn() {
		extendedFunctionalityProvider.setUpExtendedFunctionalityToThrowExceptionOnPosition(
				dependencyProvider, READ_BEFORE_RETURN, RECORD_TYPE);

		callReadRecordAndCatchStopExecution();

		dataGroupToRecordEnhancer.MCR.assertMethodWasCalled("enhance");
	}

	private void callReadRecordAndCatchStopExecution() {
		try {
			recordReader.readRecord(USER_TOKEN, RECORD_TYPE, RECORD_ID);
			fail("Should fail as we want to stop execution when the extended functionality is used,"
					+ " to determin that the extended functionality is called in the correct place"
					+ " in the code");
		} catch (Exception _) {
		}
	}

	// @Test
	// public void testUsesVisibility_Unpublished() {
	// recordTypeHandler.MRV.setDefaultReturnValuesSupplier("useVisibility", () -> true);
	// dataRecordGroupFromStorage.MRV.setDefaultReturnValuesSupplier("getVisibility",
	// () -> Optional.of("unpublished"));
	//
	// recordReader.readRecord(USER_TOKEN, RECORD_TYPE, RECORD_ID);
	//
	// authorizator.MCR.assertMethodWasCalled("checkUserIsAuthorizedForActionOnRecordType");
	// }
	//
	// @Test
	// public void testUsesVisibility_Published() {
	// recordTypeHandler.MRV.setDefaultReturnValuesSupplier("useVisibility", () -> true);
	// dataRecordGroupFromStorage.MRV.setDefaultReturnValuesSupplier("getVisibility",
	// () -> Optional.of("published"));
	//
	// recordReader.readRecord(USER_TOKEN, RECORD_TYPE, RECORD_ID);
	//
	// authorizator.MCR.assertMethodNotCalled("checkUserIsAuthorizedForActionOnRecordType");
	//
	// }

	// TODO: Create extended functionality that make sure that hostRecord exists on create or update
	// when recordtype uses host record
	// public void testUsesHostRecord_NoHostRecord() {
	// recordTypeHandler.MRV.setDefaultReturnValuesSupplier("useHostRecord", () -> true);
	// dataRecordGroupFromStorage.MRV.setDefaultReturnValuesSupplier("getHostRecord",
	// Optional::empty);
	//
	// recordReader.readRecord(USER_TOKEN, RECORD_TYPE, RECORD_ID);
	//
	// authorizator.MCR.assertMethodNotCalled("checkUserIsAuthorizedForActionOnRecordType");
	// }
	// public void testUsesHostRecord_NoHostRecord() {
	// recordTypeHandler.MRV.setDefaultReturnValuesSupplier("useHostRecord", () -> true);
	//
	// DataRecordLinkSpy hostRecordLink = new DataRecordLinkSpy();
	// dataRecordGroupFromStorage.MRV.setDefaultReturnValuesSupplier("getHostRecord",
	// () -> Optional.of(hostRecordLink));
	//
	// recordReader.readRecord(USER_TOKEN, RECORD_TYPE, RECORD_ID);
	//
	// }
}
