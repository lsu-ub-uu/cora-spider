/*
 * Copyright 2016, 2018, 2019 Uppsala University Library
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

package se.uu.ub.cora.spider.record;

import java.util.HashSet;
import java.util.Set;

import se.uu.ub.cora.beefeater.authentication.User;
import se.uu.ub.cora.data.DataGroup;
import se.uu.ub.cora.spider.authorization.AuthorizationException;
import se.uu.ub.cora.spider.authorization.SpiderAuthorizator;

public class AuthorizatorNotAuthorizedRequiredRulesButForActionOnRecordType
		implements SpiderAuthorizator {

	public Set<String> recordPartReadPermissions = new HashSet<>();
	public boolean getUsersReadRecordPartPermissionsHasBeenCalled = false;

	@Override
	public void checkUserIsAuthorizedForActionOnRecordType(User user, String action,
			String recordType) {
		// authorized
	}

	@Override
	public boolean userIsAuthorizedForActionOnRecordType(User user, String action,
			String recordType) {
		return true;
	}

	@Override
	public boolean userIsAuthorizedForActionOnRecordTypeAndCollectedData(User user, String action,
			String string, DataGroup collectedData) {
		return false;
	}

	@Override
	public Set<String> checkAndGetUserAuthorizationsForActionOnRecordTypeAndCollectedData(User user,
			String action, String recordType, DataGroup collectedData,
			boolean calculateRecordPartPermissions) {

		throw new AuthorizationException("not authorized");
		// recordPartReadPermissions.add("someRecordType.someMetadataId");
		// return recordPartReadPermissions;
	}

}
