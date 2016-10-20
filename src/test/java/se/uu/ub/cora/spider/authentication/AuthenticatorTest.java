/*
 * Copyright 2016 Uppsala University Library
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

package se.uu.ub.cora.spider.authentication;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertTrue;

import org.testng.annotations.Test;

import se.uu.ub.cora.beefeater.authentication.User;

public class AuthenticatorTest {
	@Test
	public void testNoToken() {
		UserPickerSpy userPicker = new UserPickerSpy();
		Authenticator authenticator = new AuthenticatorImp(userPicker);
		String authToken = null;
		User logedInUser = authenticator.tryToGetActiveUser(authToken);
		assertTrue(userPicker.userPickerWasCalled);
		UserInfo usedUserInfo = userPicker.usedUserInfo;
		assertEquals(usedUserInfo.idFromLogin, "guest");
		assertEquals(usedUserInfo.domainFromLogin, "system");
		assertEquals(logedInUser.id, "12345");
	}

	@Test(expectedExceptions = AuthenticationException.class)
	public void testNonAuthenticatedUser() {
		UserPickerSpy userPicker = new UserPickerSpy();
		Authenticator authenticator = new AuthenticatorImp(userPicker);
		String authToken = "dummyNonAuthenticatedToken";
		authenticator.tryToGetActiveUser(authToken);
	}

	@Test
	public void testSystemAdmin() {
		UserPickerSpy userPicker = new UserPickerSpy();
		Authenticator authenticator = new AuthenticatorImp(userPicker);
		String authToken = "dummySystemAdminAuthenticatedToken";
		authenticator.tryToGetActiveUser(authToken);
		assertTrue(userPicker.userPickerWasCalled);
		UserInfo usedUserInfo = userPicker.usedUserInfo;
		assertEquals(usedUserInfo.idFromLogin, "systemAdmin");
		assertEquals(usedUserInfo.domainFromLogin, "system");
	}

	@Test
	public void testUser() {
		UserPickerSpy userPicker = new UserPickerSpy();
		Authenticator authenticator = new AuthenticatorImp(userPicker);
		String authToken = "dummyUserAuthenticatedToken";
		authenticator.tryToGetActiveUser(authToken);
		assertTrue(userPicker.userPickerWasCalled);
		UserInfo usedUserInfo = userPicker.usedUserInfo;
		assertEquals(usedUserInfo.idFromLogin, "user");
		assertEquals(usedUserInfo.domainFromLogin, "system");
	}

}
