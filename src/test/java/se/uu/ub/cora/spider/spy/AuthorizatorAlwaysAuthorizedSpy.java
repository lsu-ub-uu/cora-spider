/*
 * Copyright 2015, 2018 Uppsala University Library
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

import se.uu.ub.cora.beefeater.authentication.User;
import se.uu.ub.cora.bookkeeper.data.DataGroup;
import se.uu.ub.cora.spider.authorization.SpiderAuthorizator;

public class AuthorizatorAlwaysAuthorizedSpy implements SpiderAuthorizator {

	public boolean authorizedWasCalled = false;
	public List<User> users = new ArrayList<>();
	public List<String> actions = new ArrayList<>();
	public List<String> recordTypes = new ArrayList<>();
	public List<DataGroup> records = new ArrayList<>();
	public List<DataGroup> collectedTerms = new ArrayList<>();
	public List<String> calledMethods = new ArrayList<>();

	public List<String> userIsAuthorizedParameters = new ArrayList<>();

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

	@Override
	public void checkUserIsAuthorizedForActionOnRecordTypeAndCollectedData(User user, String action,
			String recordType, DataGroup collectedData) {
		this.users.add(user);
		this.actions.add(action);
		this.recordTypes.add(recordType);
		this.collectedTerms.add(collectedData);
		calledMethods.add("checkUserIsAuthorizedForActionOnRecordTypeAndCollectedData");
		// always authorized
	}

	@Override
	public boolean userIsAuthorizedForActionOnRecordTypeAndCollectedData(User user, String action,
			String recordType, DataGroup collectedData) {
		// TODO Auto-generated method stub
		this.collectedTerms.add(collectedData);
		userIsAuthorizedParameters.add(user.id + ":" + action + ":" + recordType);
		calledMethods.add("userIsAuthorizedForActionOnRecordTypeAndCollectedData");

		return true;
	}

}
