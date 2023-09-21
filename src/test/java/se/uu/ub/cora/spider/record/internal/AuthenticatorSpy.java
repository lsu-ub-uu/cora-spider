/*
 * Copyright 2015, 2019 Uppsala University Library
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

import se.uu.ub.cora.beefeater.authentication.User;
import se.uu.ub.cora.spider.authentication.Authenticator;
import se.uu.ub.cora.testutils.mcr.MethodCallRecorder;
import se.uu.ub.cora.testutils.mrv.MethodReturnValues;

public class AuthenticatorSpy implements Authenticator {

	public MethodCallRecorder MCR = new MethodCallRecorder();
	public MethodReturnValues MRV = new MethodReturnValues();
	private User userSpy = new User("userSpy");

	public AuthenticatorSpy() {
		MCR.useMRV(MRV);
		MRV.setDefaultReturnValuesSupplier("getUserForToken", () -> userSpy);
	}

	@Override
	public User getUserForToken(String authToken) {
		return (User) MCR.addCallAndReturnFromMRV("authToken", authToken);
	}

}
