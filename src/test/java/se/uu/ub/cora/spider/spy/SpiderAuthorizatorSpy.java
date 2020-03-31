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

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import se.uu.ub.cora.beefeater.authentication.User;
import se.uu.ub.cora.data.DataGroup;
import se.uu.ub.cora.spider.authorization.AuthorizationException;
import se.uu.ub.cora.spider.authorization.SpiderAuthorizator;

public class SpiderAuthorizatorSpy implements SpiderAuthorizator {

	// public boolean authorizedWasCalled = false;
	// public List<User> users = new ArrayList<>();
	// public List<String> actions = new ArrayList<>();
	// public List<String> recordTypes = new ArrayList<>();
	// public List<DataGroup> records = new ArrayList<>();
	// public List<DataGroup> collectedTerms = new ArrayList<>();
	// public List<String> calledMethods = new ArrayList<>();
	// public List<String> userIsAuthorizedParameters = new ArrayList<>();
	// public boolean getUsersReadRecordPartPermissionsHasBeenCalled = false;

	public Map<String, Integer> recordTypeAuthorizedNumberOfTimesMap = new HashMap<>();
	public Set<String> recordPartReadPermissions = new HashSet<>();
	public boolean calculateRecordPartPermissions;
	public boolean authorizedForActionAndRecordType = true;
	public boolean authorizedForActionAndRecordTypeAndCollectedData = true;

	public TestCallRecorder testCallRecorder = new TestCallRecorder();
	private Set<String> notAutorizedForAction = new HashSet<>();

	private Map<String, Set<String>> notAuthorizedForActionsOnRecordType = new HashMap<>();

	@Override
	public void checkUserIsAuthorizedForActionOnRecordType(User user, String action,
			String recordType) {
		testCallRecorder.addCall("checkUserIsAuthorizedForActionOnRecordType", "user", user,
				"action", action, "recordType", recordType);
		if (!authorizedForActionAndRecordType || notAutorizedForAction.contains(action)
				|| notAuthorizedForActionOnRecordType(action, recordType)) {
			throw new AuthorizationException("Exception from SpiderAuthorizatorSpy");
		}
	}

	private boolean notAuthorizedForActionOnRecordType(String action, String recordType) {
		if (notAuthorizedForActionsOnRecordType.containsKey(action)) {
			Set<String> actionRecordTypes = notAuthorizedForActionsOnRecordType.get(action);
			return actionRecordTypes.contains(recordType);
		}
		return false;
	}

	@Override
	public boolean userIsAuthorizedForActionOnRecordType(User user, String action,
			String recordType) {
		testCallRecorder.addCall("userIsAuthorizedForActionOnRecordType", "user", user, "action",
				action, "recordType", recordType);

		if (authorizedForActionAndRecordType) {
			// userIsAuthorizedParameters.add(user.id + ":" + action + ":" + recordType);
			return true;
		} else {
			return false;
		}
	}

	@Override
	public boolean userIsAuthorizedForActionOnRecordTypeAndCollectedData(User user, String action,
			String recordType, DataGroup collectedData) {
		testCallRecorder.addCall("userIsAuthorizedForActionOnRecordTypeAndCollectedData", "user",
				user, "action", action, "recordType", recordType, "collectedData", collectedData);

		if (!recordTypeAuthorizedNumberOfTimesMap.containsKey(recordType)) {
			recordTypeAuthorizedNumberOfTimesMap.put(recordType, 1);
		} else {
			recordTypeAuthorizedNumberOfTimesMap.put(recordType,
					recordTypeAuthorizedNumberOfTimesMap.get(recordType) + 1);
		}
		// this.collectedTerms.add(collectedData);
		// userIsAuthorizedParameters.add(user.id + ":" + action + ":" + recordType);
		// calledMethods.add("userIsAuthorizedForActionOnRecordTypeAndCollectedData");

		return authorizedForActionAndRecordTypeAndCollectedData;
	}

	@Override
	public Set<String> checkAndGetUserAuthorizationsForActionOnRecordTypeAndCollectedData(User user,
			String action, String recordType, DataGroup collectedData,
			boolean calculateRecordPartPermissions) {

		testCallRecorder.addCall(
				"checkAndGetUserAuthorizationsForActionOnRecordTypeAndCollectedData", "user", user,
				"action", action, "recordType", recordType, "collectedData", collectedData,
				"calculateRecordPartPermissions", calculateRecordPartPermissions);

		if (!authorizedForActionAndRecordTypeAndCollectedData) {
			throw new AuthorizationException(
					"Excpetion thrown from checkAndGetUserAuthorizationsForActionOnRecordTypeAndCollectedData from Spy");
		}

		this.calculateRecordPartPermissions = calculateRecordPartPermissions;
		// this.users.add(user);
		// this.actions.add(action);
		// this.recordTypes.add(recordType);
		// this.collectedTerms.add(collectedData);
		// calledMethods.add("checkAndGetUserAuthorizationsForActionOnRecordTypeAndCollectedData");
		recordPartReadPermissions.add("someRecordType.someMetadataId");
		return recordPartReadPermissions;
	}

	public void setNotAutorizedForActionOnRecordType(String action, String recordType) {
		possiblyAddHolderForAction(action);
		notAuthorizedForActionsOnRecordType.get(action).add(recordType);
	}

	private void possiblyAddHolderForAction(String action) {
		if (!notAuthorizedForActionsOnRecordType.containsKey(action)) {
			notAuthorizedForActionsOnRecordType.put(action, new HashSet<>());
		}
	}

	public void setNotAutorizedForAction(String action) {
		notAutorizedForAction.add(action);
	}

}
