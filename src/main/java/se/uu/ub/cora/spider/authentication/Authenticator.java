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

/**
 * Authenticator is the interface that defines how to retreive a authenticated User.
 *
 */
public interface Authenticator {
	/**
	 * getUserForToken returns the User to which the provided authToken was issued, providing the
	 * User is still loggedin and active (has not timed out).
	 * <p>
	 * An {@link AuthenticationException} MUST be throw if the provided authToken is not issues to
	 * any user, or if the user has been deactivated in storage, or if the user has logged out, or
	 * if the user is no longer active. If the user is active or not is determined by the part of
	 * the system that provided the authToken to the User.
	 * 
	 * @param authToken
	 *            to retreive a logged in User for
	 * @return A User, which is logged in and active
	 */
	User getUserForToken(String authToken);

}
