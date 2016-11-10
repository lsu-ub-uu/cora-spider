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

import se.uu.ub.cora.beefeater.authentication.User;

public class AuthenticatorImp implements Authenticator {

	private static final String SYSTEM = "system";
	private UserPicker userPicker;

	public AuthenticatorImp(UserPicker userPicker) {
		this.userPicker = userPicker;
	}

	@Override
	public User tryToGetActiveUser(String authToken) {
		if ("dummyNonAuthenticatedToken".equals(authToken)) {
			throw new AuthenticationException("token not valid");
		}
		if ("dummySystemAdminAuthenticatedToken".equals(authToken)) {
			UserInfo userInfo = UserInfo.withLoginIdAndLoginDomain("systemAdmin", SYSTEM);
			return userPicker.pickUser(userInfo);
		}
		if ("dummyUserAuthenticatedToken".equals(authToken)) {
			UserInfo userInfo = UserInfo.withLoginIdAndLoginDomain("user", SYSTEM);
			return userPicker.pickUser(userInfo);
		}

		if ("fitnesseToken".equals(authToken)) {
			UserInfo userInfo = UserInfo.withLoginIdAndLoginDomain("fitnesse", SYSTEM);
			return userPicker.pickUser(userInfo);
		}

		UserInfo userInfo = UserInfo.withLoginIdAndLoginDomain("guest", SYSTEM);
		return userPicker.pickUser(userInfo);
	}

}
