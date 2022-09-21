/*
 * Copyright 2015, 2018, 2020 Uppsala University Library
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
import java.util.List;
import java.util.Map;
import java.util.Set;

import se.uu.ub.cora.beefeater.authentication.User;
import se.uu.ub.cora.data.collected.PermissionTerm;
import se.uu.ub.cora.spider.authorization.AuthorizationException;
import se.uu.ub.cora.spider.authorization.SpiderAuthorizator;
import se.uu.ub.cora.testutils.mcr.MethodCallRecorder;

public class SpiderAuthorizatorSpy implements SpiderAuthorizator {
	public MethodCallRecorder MCR = new MethodCallRecorder();

	/**
	 * authorizedForActionAndRecordType is used to authorize the a user for an action and
	 * recordType. Default is true. If set to false the user is not authorized.
	 * 
	 */
	public boolean authorizedForActionAndRecordType = true;
	public boolean authorizedForActionAndRecordTypeAndCollectedData = true;
	private Set<String> notAutorizedForAction = new HashSet<>();
	private Map<String, Set<String>> notAuthorizedForActionsOnRecordType = new HashMap<>();

	@Override
	public void checkUserIsAuthorizedForActionOnRecordType(User user, String action,
			String recordType) {
		MCR.addCall("user", user, "action", action, "recordType", recordType);
		if (!authorizedForActionAndRecordType
				|| notAuthorizedForActionOnRecordType(action, recordType)) {
			throw new AuthorizationException("Exception from SpiderAuthorizatorSpy");
		}
	}

	private boolean notAuthorizedForActionOnRecordType(String action, String recordType) {
		boolean temp = false;
		if (notAutorizedForAction.contains(action)) {
			temp = true;
		}
		if (notAuthorizedForActionsOnRecordType.containsKey(action)) {
			Set<String> actionRecordTypes = notAuthorizedForActionsOnRecordType.get(action);
			if (actionRecordTypes.contains(recordType)) {
				temp = true;
			}
		}
		return temp;
	}

	@Override
	public boolean userIsAuthorizedForActionOnRecordType(User user, String action,
			String recordType) {
		MCR.addCall("user", user, "action", action, "recordType", recordType);

		if (!authorizedForActionAndRecordType
				|| notAuthorizedForActionOnRecordType(action, recordType)) {
			MCR.addReturned(false);
			return false;
		} else {
			MCR.addReturned(true);
			return true;
		}
	}

	@Override
	public boolean userIsAuthorizedForActionOnRecordTypeAndCollectedData(User user, String action,
			String recordType, List<PermissionTerm> permissionTerms) {
		MCR.addCall("user", user, "action", action, "recordType", recordType, "collectedData",
				permissionTerms);

		if (!authorizedForActionAndRecordTypeAndCollectedData
				|| notAuthorizedForActionOnRecordType(action, recordType)) {

			MCR.addReturned(false);
			return false;
		}
		MCR.addReturned(true);
		return true;
	}

	@Override
	public Set<String> checkGetUsersMatchedRecordPartPermissionsForActionOnRecordTypeAndCollectedData(
			User user, String action, String recordType, List<PermissionTerm> permissionTerms,
			boolean calculateRecordPartPermissions) {

		MCR.addCall("user", user, "action", action, "recordType", recordType, "collectedData",
				permissionTerms, "calculateRecordPartPermissions", calculateRecordPartPermissions);

		if (!authorizedForActionAndRecordTypeAndCollectedData
				|| notAuthorizedForActionOnRecordType(action, recordType)) {
			throw new AuthorizationException("Excpetion thrown from "
					+ "checkGetUsersMatchedRecordPartPermissionsForActionOnRecordTypeAndCollectedData from Spy");
		}

		Set<String> recordPartPermissions = new HashSet<>();
		if ("read".equals(action)) {
			recordPartPermissions.add("someRecordType.someReadMetadataId");
		} else if ("update".equals(action)) {
			recordPartPermissions.add("someRecordType.someWriteMetadataId");
		}
		MCR.addReturned(recordPartPermissions);
		return recordPartPermissions;
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

	// @Override
	// public Set<String> getUsersMatchedRecordPartPermissionsForActionOnRecordTypeAndCollectedData(
	// User user, String action, String recordType, DataGroup collectedData) {
	// MCR.addCall("user", user, "action", action, "recordType", recordType, "collectedData",
	// collectedData);
	// MCR.addReturned(null);
	// return null;
	// }

	@Override
	public void checkUserIsAuthorizedForActionOnRecordTypeAndCollectedData(User user, String action,
			String recordType, List<PermissionTerm> permissionTerms) {
		MCR.addCall("user", user, "action", action, "recordType", recordType, "collectedData",
				permissionTerms);
		if (!authorizedForActionAndRecordTypeAndCollectedData
				|| notAuthorizedForActionOnRecordType(action, recordType)) {
			throw new AuthorizationException(
					"Excpetion thrown from checkUserIsAuthorizedForActionOnRecordTypeAndCollectedData from Spy");
		}
	}

}
