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
import se.uu.ub.cora.spider.spy.MethodCallRecorder;

public class AuthenticatorSpy implements Authenticator {
	public MethodCallRecorder MCR = new MethodCallRecorder();

	public boolean authenticationWasCalled = false;
	public String authToken;
	public User returnedUser;
	public boolean throwAuthenticationException = false;

	@Override
	public User getUserForToken(String authToken) {
		MCR.addCall("getUserForToken", "authToken", authToken);
		this.authToken = authToken;
		authenticationWasCalled = true;

		if (throwAuthenticationException) {
			throw new AuthenticationException("Error from AuthenticatorSpy");
		}

		if ("dummy1Token".equals(authToken)) {
			User user = new User("dummy1");
			user.loginId = "knownUser";
			user.loginDomain = "system";
			user.roles.add("guest");
			MCR.addReturned("getUserForToken", user);
			return user;

		}

		User user = new User("12345");
		user.loginId = "knownUser";
		user.loginDomain = "system";
		user.roles.add("guest");
		returnedUser = user;

		MCR.addReturned("getUserForToken", user);
		return user;
	}

}
