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

import se.uu.ub.cora.beefeater.authentication.User;
import se.uu.ub.cora.data.DataRecordGroup;

public interface SecurityControl {

	User checkActionAuthorizationForUser(String authToken, String type, String action);

	/**
	 * checkActionAuthorizationForUser authenticates the user and checks that the user is
	 * authorized to perform the given action on the given type, considering host record if
	 * applicable. When the record type uses a host record the security check will use
	 * "hostType.type" as the record type for the authorization check, where "hostType" is the
	 * linked record type of the host record link found in the provided dataRecordGroup.
	 * 
	 * @param authToken
	 *            the auth token to authenticate the user
	 * @param type
	 *            the record type to check authorization for
	 * @param action
	 *            the action to check authorization for
	 * @param dataRecordGroup
	 *            the {@link DataRecordGroup} containing the host record link if applicable
	 * @return the authenticated {@link User}
	 */
	User checkActionAuthorizationForUser(String authToken, String type, String action,
			DataRecordGroup dataRecordGroup);

}
