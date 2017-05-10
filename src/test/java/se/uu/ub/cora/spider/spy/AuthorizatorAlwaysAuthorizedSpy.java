/*
 * Copyright 2015 Uppsala University Library
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

package se.uu.ub.cora.spider.spy;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import se.uu.ub.cora.beefeater.authentication.User;
import se.uu.ub.cora.bookkeeper.data.DataGroup;
import se.uu.ub.cora.spider.authorization.SpiderAuthorizator;

public class AuthorizatorAlwaysAuthorizedSpy implements SpiderAuthorizator {

	public boolean authorizedWasCalled = false;
	public List<User> users = new ArrayList<>();
	public List<String> actions = new ArrayList<>();
	public List<String> recordTypes = new ArrayList<>();
	public List<DataGroup> records = new ArrayList<>();

	// public Map<String, Map<String, Map<String, List<DataGroup>>>>
	// userIsAuthorizedParameters = new HashMap<>();
	public List<String> userIsAuthorizedParameters = new ArrayList<>();

	@Override
	public boolean userSatisfiesRequiredRules(User user,
			List<Map<String, Set<String>>> requiredRules) {
		authorizedWasCalled = true;
		return true;
	}

	@Override
	public void checkUserIsAuthorizedForActionOnRecordType(User user, String action,
			String recordType) {
		// always authorized
		authorizedWasCalled = true;
	}

	@Override
	public void checkUserIsAuthorizedForActionOnRecordTypeAndRecord(User user, String action,
			String recordType, DataGroup record) {
		this.users.add(user);
		this.actions.add(action);
		this.recordTypes.add(recordType);
		this.records.add(record);
		// always authorized
		authorizedWasCalled = true;

	}

	@Override
	public boolean userIsAuthorizedForActionOnRecordTypeAndRecord(User user, String action,
			String recordType, DataGroup record) {
		authorizedWasCalled = true;
		this.recordTypes.add(recordType);
		userIsAuthorizedParameters.add(user.id + ":" + action + ":" + recordType);
		return true;
	}

	@Override
	public boolean userIsAuthorizedForActionOnRecordType(User user, String action,
			String recordType) {
		authorizedWasCalled = true;
		userIsAuthorizedParameters.add(user.id + ":" + action + ":" + recordType);
		return true;
	}

}
