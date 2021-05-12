/*
 * Copyright 2015, 2016, 2017, 2019, 2020 Uppsala University Library
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

import java.util.HashMap;

import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import se.uu.ub.cora.beefeater.authentication.User;
import se.uu.ub.cora.data.DataGroup;
import se.uu.ub.cora.data.DataRecord;
import se.uu.ub.cora.logger.LoggerProvider;
import se.uu.ub.cora.spider.authentication.AuthenticationException;
import se.uu.ub.cora.spider.authentication.AuthenticatorSpy;
import se.uu.ub.cora.spider.data.DataAtomicSpy;
import se.uu.ub.cora.spider.data.DataGroupSpy;
import se.uu.ub.cora.spider.dependency.RecordTypeHandlerSpy;
import se.uu.ub.cora.spider.dependency.SpiderDependencyProviderSpy;
import se.uu.ub.cora.spider.log.LoggerFactorySpy;
import se.uu.ub.cora.spider.record.DataGroupToRecordEnhancerSpy;
import se.uu.ub.cora.spider.record.DataRedactorSpy;
import se.uu.ub.cora.spider.record.RecordReader;
import se.uu.ub.cora.spider.spy.SpiderAuthorizatorSpy;

public class SpiderRecordReaderTest {
	private static final String SOME_RECORD_ID = "place:0001";
	private static final String SOME_USER_TOKEN = "someToken78678567";
	private static final String SOME_RECORD_TYPE = "someRecordType";
	private RecordStorageSpy recordStorage;
	private AuthenticatorSpy authenticator;
	private SpiderAuthorizatorSpy authorizator;
	private SpiderDependencyProviderSpy dependencyProvider;
	private RecordReader recordReader;
	private DataGroupToRecordEnhancerSpy dataGroupToRecordEnhancer;
	private LoggerFactorySpy loggerFactorySpy;
	private RecordTypeHandlerSpy recordTypeHandlerSpy;
	private DataRedactorSpy dataRedactor;

	@BeforeMethod
	public void beforeMethod() {
		setUpFactoriesAndProviders();
		authenticator = new AuthenticatorSpy();
		authorizator = new SpiderAuthorizatorSpy();
		recordStorage = new RecordStorageSpy();
		dataRedactor = new DataRedactorSpy();
		setUpDependencyProvider();
	}

	private void setUpFactoriesAndProviders() {
		loggerFactorySpy = new LoggerFactorySpy();
		LoggerProvider.setLoggerFactory(loggerFactorySpy);
	}

	private void setUpDependencyProvider() {
		dependencyProvider = new SpiderDependencyProviderSpy(new HashMap<>());
		dependencyProvider.authenticator = authenticator;
		dependencyProvider.spiderAuthorizator = authorizator;

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

		recordStorage.MCR.assertParameters("read", 0, SOME_RECORD_TYPE, SOME_RECORD_ID);
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
	public void testEnhancerCalledCorrectlyWhenAbstractRecordType() throws Exception {
		recordTypeHandlerSpy.isAbstract = true;

		DataGroup dataGroup = new DataGroupSpy("someNameInData");
		DataGroup recordInfo = new DataGroupSpy("recordInfo");
		dataGroup.addChild(recordInfo);
		DataGroup type = new DataGroupSpy("type");
		recordInfo.addChild(type);
		type.addChild(new DataAtomicSpy("linkedRecordId", "someNotAbstractType"));
		recordStorage.returnForRead = dataGroup;

		recordReader.readRecord(SOME_USER_TOKEN, SOME_RECORD_TYPE, SOME_RECORD_ID);

		User user = (User) authenticator.MCR.getReturnValue("getUserForToken", 0);
		recordStorage.MCR.assertMethodWasCalled("read");
		DataGroup dataGroupFromStorage = (DataGroup) recordStorage.MCR.getReturnValue("read", 0);
		dataGroupToRecordEnhancer.MCR.assertParameters("enhance", 0, user, "someNotAbstractType",
				dataGroupFromStorage, dataRedactor);
	}

	@Test
	public void testAnswerFromEnhancerIsReturned() throws Exception {
		DataRecord readRecord = recordReader.readRecord(SOME_USER_TOKEN, SOME_RECORD_TYPE,
				SOME_RECORD_ID);

		dataGroupToRecordEnhancer.MCR.assertReturn("enhance", 0, readRecord);
	}
}
