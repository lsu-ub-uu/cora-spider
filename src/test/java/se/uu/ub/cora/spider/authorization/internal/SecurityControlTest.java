/*
 * Copyright 2026 Uppsala University Library
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
package se.uu.ub.cora.spider.authorization.internal;

import java.util.Optional;

import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import se.uu.ub.cora.beefeater.authentication.User;
import se.uu.ub.cora.data.spies.DataRecordGroupSpy;
import se.uu.ub.cora.data.spies.DataRecordLinkSpy;
import se.uu.ub.cora.spider.dependency.spy.RecordTypeHandlerSpy;
import se.uu.ub.cora.spider.record.internal.AuthenticatorSpy;
import se.uu.ub.cora.spider.record.internal.SpiderAuthorizatorSpy;
import se.uu.ub.cora.spider.spy.DataGroupTermCollectorSpy;
import se.uu.ub.cora.spider.spy.SpiderDependencyProviderSpy;
import se.uu.ub.cora.storage.spies.RecordStorageSpy;

public class SecurityControlTest {
	private static final String ACTION = "someAction";
	private static final String RECORD_TYPE = "someRecordType";
	private static final String AUTH_TOKEN = "someAuthToken";
	private static final String ACTION_READ = "read";
	private static final String ACTION_LIST = "list";
	private AuthenticatorSpy authenticator;
	private SpiderAuthorizatorSpy authorizator;
	private RecordStorageSpy recordStorage;
	private RecordTypeHandlerSpy recordTypeHandler;
	private DataGroupTermCollectorSpy termCollector;
	private SpiderDependencyProviderSpy dependencyProvider;
	private SecurityControl securityControl;
	private DataRecordGroupSpy dataRecordGroup;

	@BeforeMethod
	public void beforeMethod() {
		setUpDependencyProvider();
		dataRecordGroup = new DataRecordGroupSpy();

		securityControl = SecurityControlImp.usingDependencyProvider(dependencyProvider);
	}

	private void setUpDependencyProvider() {
		authenticator = new AuthenticatorSpy();
		authorizator = new SpiderAuthorizatorSpy();
		recordStorage = new RecordStorageSpy();
		recordTypeHandler = new RecordTypeHandlerSpy();
		termCollector = new DataGroupTermCollectorSpy();

		dependencyProvider = new SpiderDependencyProviderSpy();
		dependencyProvider.MRV.setDefaultReturnValuesSupplier("getRecordStorage",
				() -> recordStorage);
		dependencyProvider.MRV.setDefaultReturnValuesSupplier("getSpiderAuthorizator",
				() -> authorizator);
		dependencyProvider.MRV.setDefaultReturnValuesSupplier("getAuthenticator",
				() -> authenticator);
		dependencyProvider.MRV.setDefaultReturnValuesSupplier("getDataGroupTermCollector",
				() -> termCollector);
		dependencyProvider.MRV.setDefaultReturnValuesSupplier(
				"getRecordTypeHandlerUsingDataRecordGroup", () -> recordTypeHandler);
		dependencyProvider.MRV.setDefaultReturnValuesSupplier("getRecordTypeHandler",
				() -> recordTypeHandler);
	}

	@Test
	public void testAuthenticatorIsCalled() {
		User user = securityControl.checkActionAuthorizationForUser(AUTH_TOKEN, RECORD_TYPE, ACTION);

		assertAuthenticatorCalled(user);
	}

	private void assertAuthenticatorCalled(User user) {
		authenticator.MCR.assertParameters("getUserForToken", 0, AUTH_TOKEN);
		authenticator.MCR.assertReturn("getUserForToken", 0, user);
	}

	@Test
	public void testAuthorizatorIsCalled() {
		User user = securityControl.checkActionAuthorizationForUser(AUTH_TOKEN, RECORD_TYPE, ACTION);

		assertCheckUserIsAuthorizedForAction(user, ACTION, RECORD_TYPE);
	}

	private void assertCheckUserIsAuthorizedForAction(User user, String action, String type) {
		authorizator.MCR.assertParameters("checkUserIsAuthorizedForActionOnRecordType", 0, user,
				action, type);
	}

	@Test
	public void testAuthorizatorIsCalled_withDataRecordGroup() {
		User user = securityControl.checkActionAuthorizationForUser(AUTH_TOKEN, RECORD_TYPE, ACTION,
				dataRecordGroup);

		assertAuthenticatorCalled(user);
		assertCheckUserIsAuthorizedForAction(user, ACTION, RECORD_TYPE);
	}

	@Test
	public void testAuthorizatorIsCalled_hostRecord() {
		setUpToUseHostRecord();

		User user = securityControl.checkActionAuthorizationForUser(AUTH_TOKEN, RECORD_TYPE, ACTION,
				dataRecordGroup);

		assertAuthenticatorCalled(user);
		assertCheckUserIsAuthorizedForAction(user, ACTION, "someHostType." + RECORD_TYPE);
	}

	private void setUpToUseHostRecord() {
		recordTypeHandler.MRV.setDefaultReturnValuesSupplier("useHostRecord", () -> true);
		DataRecordLinkSpy hostRecordLink = new DataRecordLinkSpy();
		hostRecordLink.MRV.setDefaultReturnValuesSupplier("getLinkedRecordType",
				() -> "someHostType");
		hostRecordLink.MRV.setDefaultReturnValuesSupplier("getLinkedRecordId", () -> "someHostId");
		dataRecordGroup.MRV.setDefaultReturnValuesSupplier("getHostRecord",
				() -> Optional.of(hostRecordLink));
	}

	@Test
	public void testAuthorizatorIsCalled_IsNotPublicForRead() {
		setUpToIsPublicForRead(false);
		User user = securityControl.checkActionAuthorizationForUser(AUTH_TOKEN, RECORD_TYPE,
				ACTION_READ);

		assertAuthenticatorCalled(user);
		assertCheckUserIsAuthorizedForAction(user, ACTION_READ, RECORD_TYPE);
	}

	@Test
	public void testAuthorizatorIsNotCalled_isPublicForRead() {
		setUpToIsPublicForRead(true);

		User user = securityControl.checkActionAuthorizationForUser(AUTH_TOKEN, RECORD_TYPE,
				ACTION_READ);

		assertAuthenticatorCalled(user);
		authorizator.MCR.assertMethodNotCalled("checkUserIsAuthorizedForActionOnRecordType");
	}

	@Test
	public void testAuthorizatorIsNotCalled_isPublicForList() {
		setUpToIsPublicForRead(true);

		User user = securityControl.checkActionAuthorizationForUser(AUTH_TOKEN, RECORD_TYPE,
				ACTION_LIST);

		assertAuthenticatorCalled(user);
		authorizator.MCR.assertMethodNotCalled("checkUserIsAuthorizedForActionOnRecordType");
	}

	@Test
	public void testAuthorizatorIsCalled_isNotPublicForList() {
		setUpToIsPublicForRead(false);

		User user = securityControl.checkActionAuthorizationForUser(AUTH_TOKEN, RECORD_TYPE,
				ACTION_LIST);

		assertAuthenticatorCalled(user);
		assertCheckUserIsAuthorizedForAction(user, ACTION_LIST, RECORD_TYPE);
	}

	private void setUpToIsPublicForRead(boolean value) {
		recordTypeHandler.MRV.setDefaultReturnValuesSupplier("isPublicForRead", () -> value);
	}

}
