/*
 * Copyright 2021 Uppsala University Library
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

import static org.testng.Assert.assertSame;
import static org.testng.Assert.assertTrue;

import java.util.HashMap;

import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import se.uu.ub.cora.beefeater.authentication.User;
import se.uu.ub.cora.logger.LoggerProvider;
import se.uu.ub.cora.spider.authentication.AuthenticationException;
import se.uu.ub.cora.spider.authentication.AuthenticatorSpy;
import se.uu.ub.cora.spider.authorization.AuthorizationException;
import se.uu.ub.cora.spider.data.DataGroupSpy;
import se.uu.ub.cora.spider.dependency.SpiderDependencyProvider;
import se.uu.ub.cora.spider.dependency.SpiderDependencyProviderSpy;
import se.uu.ub.cora.spider.log.LoggerFactorySpy;
import se.uu.ub.cora.spider.record.DataGroupToRecordEnhancer;
import se.uu.ub.cora.spider.record.RecordListIndexer;
import se.uu.ub.cora.spider.spy.DataValidatorSpy;
import se.uu.ub.cora.spider.spy.SpiderAuthorizatorSpy;

public class RecordListIndexerTest {

	private static final String SOME_USER_TOKEN = "someToken123456789";
	private static final String SOME_RECORD_TYPE = "someRecordType";

	private LoggerFactorySpy loggerFactorySpy;
	// private DataGroupToRecordEnhancerSpy enhancerSpy;
	private SpiderDependencyProviderSpy dependencyProviderSpy;
	private RecordListIndexerImp recordListIndexer;
	private AuthenticatorSpy authenticatorSpy;

	private SpiderAuthorizatorSpy authorizatorSpy;
	private DataGroupSpy indexSettings;
	private DataValidatorSpy dataValidatorSpy;

	@BeforeMethod
	public void beforeMethod() {
		setUpDependencyProvider();
		// enhancerSpy = new DataGroupToRecordEnhancerSpy();
		indexSettings = new DataGroupSpy("indexInfo");

		recordListIndexer = RecordListIndexerImp
				.usingDependencyProviderAndDataGroupToRecordEnhancer(dependencyProviderSpy);
	}

	private void setUpDependencyProvider() {
		loggerFactorySpy = new LoggerFactorySpy();
		LoggerProvider.setLoggerFactory(loggerFactorySpy);
		dependencyProviderSpy = new SpiderDependencyProviderSpy(new HashMap<>());
		authenticatorSpy = new AuthenticatorSpy();
		authorizatorSpy = new SpiderAuthorizatorSpy();
		dataValidatorSpy = new DataValidatorSpy();

		dependencyProviderSpy.authenticator = authenticatorSpy;
		dependencyProviderSpy.spiderAuthorizator = authorizatorSpy;
		dependencyProviderSpy.dataValidator = dataValidatorSpy;

	}

	@Test
	public void testImplementsInterface() throws Exception {
		assertTrue(recordListIndexer instanceof RecordListIndexer);
	}

	@Test
	public void testConstructorStoresDependencyProviderAndEnhancer() throws Exception {
		SpiderDependencyProvider dependencyProvider = recordListIndexer.getDependencyProvider();
		DataGroupToRecordEnhancer enhancer = recordListIndexer.getRecordEnhancer();
		assertSame(dependencyProvider, dependencyProviderSpy);

	}

	@Test(expectedExceptions = AuthenticationException.class, expectedExceptionsMessageRegExp = ""
			+ "Error from AuthenticatorSpy")
	public void testUserNotAuthenticated() {
		authenticatorSpy.throwAuthenticationException = true;

		recordListIndexer.indexRecordList(SOME_USER_TOKEN, null, null);
	}

	@Test
	public void testAuthTokenIsPassedOnToAuthenticator() throws Exception {
		recordListIndexer.indexRecordList(SOME_USER_TOKEN, null, null);

		authenticatorSpy.MCR.assertParameters("getUserForToken", 0, SOME_USER_TOKEN);
		authenticatorSpy.MCR.assertNumberOfCallsToMethod("getUserForToken", 1);
	}

	@Test(expectedExceptions = AuthorizationException.class, expectedExceptionsMessageRegExp = ""
			+ "Exception from SpiderAuthorizatorSpy")
	public void testUserIsNotAuthorizedForActionOnRecordType() {
		authorizatorSpy.authorizedForActionAndRecordType = false;

		recordListIndexer.indexRecordList(SOME_USER_TOKEN, null, null);
	}

	@Test
	public void testUserIsAuthorizedForActionOnRecordTypeIncomingParameters() throws Exception {
		recordListIndexer.indexRecordList(SOME_USER_TOKEN, SOME_RECORD_TYPE, indexSettings);
		User user = (User) authenticatorSpy.MCR.getReturnValue("getUserForToken", 0);

		authorizatorSpy.MCR.assertParameters("checkUserIsAuthorizedForActionOnRecordType", 0, user,
				"index", SOME_RECORD_TYPE);

	}

	// @Test
	// public void testEmptyFilter() throws Exception {
	// recordListIndexer.indexRecordList(SOME_USER_TOKEN, SOME_RECORD_TYPE, indexInfo);
	// dataValidator.MCR.assertMethodNotCalled("validateListFilter");
	// }
	//

	@Test
	public void testNonEmptyFilterContainsPartGroupValidateListFilterIsCalled() {
		recordListIndexer.indexRecordList(SOME_USER_TOKEN, SOME_RECORD_TYPE, indexSettings);

		dataValidatorSpy.MCR.assertParameters("validateIndexFilter", 0, SOME_RECORD_TYPE,
				indexSettings);
	}

}
