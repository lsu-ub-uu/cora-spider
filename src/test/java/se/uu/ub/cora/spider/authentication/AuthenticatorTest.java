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

import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import se.uu.ub.cora.beefeater.authentication.User;

public class AuthenticatorTest {
	private UserPickerSpy userPicker;
	private Authenticator authenticator;
	private User logedInUser;

	@BeforeMethod
	public void setUp() {
		userPicker = new UserPickerSpy();
		authenticator = new AuthenticatorImp(userPicker);
	}

	@Test
	public void testNoToken() {
		logedInUser = authenticator.tryToGetActiveUser(null);
		assertPluggedInUserPickerWasUsed();
		assertUsedUserInfoLoginAndDomain("guest", "system");
		assertReturnedUserIsFromUserPicker();
	}

	private void assertPluggedInUserPickerWasUsed() {
		assertTrue(userPicker.userPickerWasCalled);
	}

	private void assertUsedUserInfoLoginAndDomain(String expectedLoginId,
			String expectedLoginDomain) {
		UserInfo usedUserInfo = userPicker.usedUserInfo;
		assertEquals(usedUserInfo.idFromLogin, expectedLoginId);
		assertEquals(usedUserInfo.domainFromLogin, expectedLoginDomain);
	}

	private void assertReturnedUserIsFromUserPicker() {
		assertEquals(logedInUser, userPicker.returnedUser);
	}

	@Test(expectedExceptions = AuthenticationException.class)
	public void testNonAuthenticatedUser() {
		authenticator.tryToGetActiveUser("dummyNonAuthenticatedToken");
	}

	@Test
	public void testSystemAdmin() {
		logedInUser = authenticator.tryToGetActiveUser("dummySystemAdminAuthenticatedToken");
		assertPluggedInUserPickerWasUsed();
		assertUsedUserInfoLoginAndDomain("systemAdmin", "system");
		assertReturnedUserIsFromUserPicker();
	}

	@Test
	public void testUser() {
		logedInUser = authenticator.tryToGetActiveUser("dummyUserAuthenticatedToken");
		assertPluggedInUserPickerWasUsed();
		assertUsedUserInfoLoginAndDomain("user", "system");
		assertReturnedUserIsFromUserPicker();
	}

	@Test
	public void testFitnesseUser() {
		logedInUser = authenticator.tryToGetActiveUser("fitnesseToken");
		assertPluggedInUserPickerWasUsed();
		assertUsedUserInfoLoginAndDomain("fitnesse", "system");
		assertReturnedUserIsFromUserPicker();
	}

}
