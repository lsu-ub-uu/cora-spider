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

import static org.testng.Assert.assertNotNull;
import static se.uu.ub.cora.spider.extendedfunctionality.ExtendedFunctionalityPosition.READ_AFTER_AUTHORIZATION;
import static se.uu.ub.cora.spider.extendedfunctionality.ExtendedFunctionalityPosition.READ_BEFORE_RETURN;

import java.util.List;

import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import se.uu.ub.cora.beefeater.authentication.User;
import se.uu.ub.cora.data.DataGroup;
import se.uu.ub.cora.data.DataProvider;
import se.uu.ub.cora.data.DataRecord;
import se.uu.ub.cora.data.spies.DataFactorySpy;
import se.uu.ub.cora.logger.LoggerProvider;
import se.uu.ub.cora.spider.authentication.AuthenticationException;
import se.uu.ub.cora.spider.authentication.OldAuthenticatorSpy;
import se.uu.ub.cora.spider.data.DataRecordOldSpy;
import se.uu.ub.cora.spider.dependency.spy.RecordTypeHandlerSpy;
import se.uu.ub.cora.spider.dependency.spy.SpiderDependencyProviderOldSpy;
import se.uu.ub.cora.spider.extendedfunctionality.ExtendedFunctionalityData;
import se.uu.ub.cora.spider.extendedfunctionality.internal.ExtendedFunctionalityProviderSpy;
import se.uu.ub.cora.spider.log.LoggerFactorySpy;
import se.uu.ub.cora.spider.record.DataGroupToRecordEnhancerSpy;
import se.uu.ub.cora.spider.record.DataRedactorSpy;
import se.uu.ub.cora.spider.record.RecordReader;
import se.uu.ub.cora.spider.spy.OldSpiderAuthorizatorSpy;

public class RecordReaderTest {
	private static final String SOME_RECORD_ID = "place:0001";
	private static final String SOME_USER_TOKEN = "someToken78678567";
	private static final String SOME_RECORD_TYPE = "someRecordType";
	private DataFactorySpy dataFactorySpy;
	private RecordStorageOldSpy recordStorage;
	private OldAuthenticatorSpy authenticator;
	private OldSpiderAuthorizatorSpy authorizator;
	private SpiderDependencyProviderOldSpy dependencyProvider;
	private RecordReader recordReader;
	private DataGroupToRecordEnhancerSpy dataGroupToRecordEnhancer;
	private LoggerFactorySpy loggerFactorySpy;
	private RecordTypeHandlerSpy recordTypeHandlerSpy;
	private DataRedactorSpy dataRedactor;
	private ExtendedFunctionalityProviderSpy extendedFunctionalityProvider;

	@BeforeMethod
	public void beforeMethod() {
		setUpFactoriesAndProviders();
		authenticator = new OldAuthenticatorSpy();
		authorizator = new OldSpiderAuthorizatorSpy();
		recordStorage = new RecordStorageOldSpy();
		dataRedactor = new DataRedactorSpy();
		extendedFunctionalityProvider = new ExtendedFunctionalityProviderSpy();

		setUpDependencyProvider();
	}

	private void setUpFactoriesAndProviders() {
		loggerFactorySpy = new LoggerFactorySpy();
		LoggerProvider.setLoggerFactory(loggerFactorySpy);
		dataFactorySpy = new DataFactorySpy();
		DataProvider.onlyForTestSetDataFactory(dataFactorySpy);
	}

	private void setUpDependencyProvider() {
		dependencyProvider = new SpiderDependencyProviderOldSpy();
		dependencyProvider.authenticator = authenticator;
		dependencyProvider.spiderAuthorizator = authorizator;
		dependencyProvider.extendedFunctionalityProvider = extendedFunctionalityProvider;

		dependencyProvider.recordStorage = recordStorage;
		dependencyProvider.dataRedactor = dataRedactor;
		dataGroupToRecordEnhancer = new DataGroupToRecordEnhancerSpy();

		recordReader = RecordReaderImp.usingDependencyProviderAndDataGroupToRecordEnhancer(
				dependencyProvider, dataGroupToRecordEnhancer);
		recordTypeHandlerSpy = dependencyProvider.recordTypeHandlerSpy;
	}

	@Test
	public void testAuthenticatorIsCalledCorrectly() throws Exception {
		recordReader.readRecord(SOME_USER_TOKEN, SOME_RECORD_TYPE, SOME_RECORD_ID);

		authenticator.MCR.assertParameters("getUserForToken", 0, SOME_USER_TOKEN);

		recordTypeHandlerSpy.MCR.assertMethodWasCalled("isPublicForRead");
		authorizator.MCR.assertMethodWasCalled("checkUserIsAuthorizedForActionOnRecordType");
		recordStorage.MCR.assertMethodWasCalled("read");
		dataGroupToRecordEnhancer.MCR.assertMethodWasCalled("enhance");
	}

	@Test(expectedExceptions = AuthenticationException.class)
	public void testAuthenticationExceptionShouldBeThrownIfNotAuthenticated() throws Exception {
		authenticator.throwAuthenticationException = true;

		recordReader.readRecord(SOME_USER_TOKEN, SOME_RECORD_TYPE, SOME_RECORD_ID);
	}

	@Test
	public void testReadNotPublicAndNoAccessToRead() throws Exception {
		authorizator.setNotAutorizedForActionOnRecordType("read", SOME_RECORD_TYPE);
		Exception caughtException = null;

		try {
			recordReader.readRecord(SOME_USER_TOKEN, SOME_RECORD_TYPE, SOME_RECORD_ID);
		} catch (Exception e) {
			caughtException = e;
		}

		User user = (User) authenticator.MCR.getReturnValue("getUserForToken", 0);
		assertNotNull(caughtException);
		dependencyProvider.MCR.assertParameters("getRecordTypeHandler", 0, SOME_RECORD_TYPE);
		recordTypeHandlerSpy.MCR.assertMethodWasCalled("isPublicForRead");

		authorizator.MCR.assertParameters("checkUserIsAuthorizedForActionOnRecordType", 0, user,
				"read", SOME_RECORD_TYPE);

		recordTypeHandlerSpy.MCR.assertReturn("isPublicForRead", 0, false);
		authorizator.MCR.assertMethodWasCalled("checkUserIsAuthorizedForActionOnRecordType");
		recordStorage.MCR.assertMethodNotCalled("read");
		dataGroupToRecordEnhancer.MCR.assertMethodNotCalled("enhance");
	}

	@Test
	public void testReadIsPublicAuthorizationShouldNotBeChecked() throws Exception {
		recordTypeHandlerSpy.isPublicForRead = true;
		authorizator.setNotAutorizedForActionOnRecordType("read", SOME_RECORD_TYPE);

		recordReader.readRecord(SOME_USER_TOKEN, SOME_RECORD_TYPE, SOME_RECORD_ID);

		recordTypeHandlerSpy.MCR.assertReturn("isPublicForRead", 0, true);
		authorizator.MCR.assertMethodNotCalled("checkUserIsAuthorizedForActionOnRecordType");
		recordStorage.MCR.assertMethodWasCalled("read");
		dataGroupToRecordEnhancer.MCR.assertMethodWasCalled("enhance");
	}

	@Test
	public void testReadNotPublicButAuthorized() throws Exception {
		recordReader.readRecord(SOME_USER_TOKEN, SOME_RECORD_TYPE, SOME_RECORD_ID);

		recordTypeHandlerSpy.MCR.assertReturn("isPublicForRead", 0, false);
		authorizator.MCR.assertMethodWasCalled("checkUserIsAuthorizedForActionOnRecordType");
		recordStorage.MCR.assertMethodWasCalled("read");
		dataGroupToRecordEnhancer.MCR.assertMethodWasCalled("enhance");
	}

	@Test
	public void testStorageIsCalledCorrectly() throws Exception {
		recordReader.readRecord(SOME_USER_TOKEN, SOME_RECORD_TYPE, SOME_RECORD_ID);

		recordStorage.MCR.assertParameterAsEqual("read", 0, "types", List.of(SOME_RECORD_TYPE));
		recordStorage.MCR.assertParameter("read", 0, "id", SOME_RECORD_ID);
	}

	@Test
	public void testEnhancerCalledCorrectly() throws Exception {
		recordReader.readRecord(SOME_USER_TOKEN, SOME_RECORD_TYPE, SOME_RECORD_ID);

		User user = (User) authenticator.MCR.getReturnValue("getUserForToken", 0);
		DataGroup dataGroupFromStorage = (DataGroup) recordStorage.MCR.getReturnValue("read", 0);
		dataGroupToRecordEnhancer.MCR.assertParameters("enhance", 0, user, SOME_RECORD_TYPE,
				dataGroupFromStorage, dataRedactor);
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
		expectedData.dataGroup = enhancedRecord.getDataGroup();
		expectedData.dataRecord = enhancedRecord;
		extendedFunctionalityProvider.assertCallToPositionAndFunctionalityCalledWithData(
				READ_BEFORE_RETURN, expectedData, 1);
	}

}
