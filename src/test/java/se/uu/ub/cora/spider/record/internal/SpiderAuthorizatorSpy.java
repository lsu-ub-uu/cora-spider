/*
 * Copyright 2023, 2025 Uppsala University Library
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

import java.util.Collections;
import java.util.List;
import java.util.Set;

import se.uu.ub.cora.beefeater.authentication.User;
import se.uu.ub.cora.data.collected.PermissionTerm;
import se.uu.ub.cora.spider.authorization.SpiderAuthorizator;
import se.uu.ub.cora.testutils.mcr.MethodCallRecorder;
import se.uu.ub.cora.testutils.mrv.MethodReturnValues;

public class SpiderAuthorizatorSpy implements SpiderAuthorizator {
	public MethodCallRecorder MCR = new MethodCallRecorder();
	public MethodReturnValues MRV = new MethodReturnValues();

	public SpiderAuthorizatorSpy() {
		MCR.useMRV(MRV);
		MRV.setDefaultReturnValuesSupplier("userIsAuthorizedForActionOnRecordType", () -> true);
		MRV.setDefaultReturnValuesSupplier("userIsAuthorizedForActionOnRecordTypeAndCollectedData",
				() -> true);
		MRV.setDefaultReturnValuesSupplier(
				"checkGetUsersMatchedRecordPartPermissionsForActionOnRecordTypeAndCollectedData",
				Collections::emptySet);
	}

	@Override
	public boolean userIsAuthorizedForActionOnRecordType(User user, String action,
			String recordType) {
		return (boolean) MCR.addCallAndReturnFromMRV("user", user, "action", action, "recordType",
				recordType);
	}

	@Override
	public void checkUserIsAuthorizedForActionOnRecordType(User user, String action,
			String recordType) {
		MCR.addCall("user", user, "action", action, "recordType", recordType);
	}

	@Override
	public boolean userIsAuthorizedForActionOnRecordTypeAndCollectedData(User user, String action,
			String recordType, List<PermissionTerm> permissionTerms) {
		return (boolean) MCR.addCallAndReturnFromMRV("user", user, "action", action, "recordType",
				recordType, "permissionTerms", permissionTerms);
	}

	@Override
	public void checkUserIsAuthorizedForActionOnRecordTypeAndCollectedData(User user, String action,
			String recordType, List<PermissionTerm> permissionTerms) {
		MCR.addCall("user", user, "action", action, "recordType", recordType, "permissionTerms",
				permissionTerms);
	}

	@SuppressWarnings("unchecked")
	@Override
	public Set<String> checkGetUsersMatchedRecordPartPermissionsForActionOnRecordTypeAndCollectedData(
			User user, String action, String recordType, List<PermissionTerm> permissionTerms,
			boolean calculateRecordPartPermissions) {
		return (Set<String>) MCR.addCallAndReturnFromMRV("user", user, "action", action,
				"recordType", recordType, "permissionTerms", permissionTerms,
				"calculateRecordPartPermissions", calculateRecordPartPermissions);
	}

	@Override
	public void checkUserIsAuthorizedForPemissionUnit(User user,
			boolean recordTypeUsesPermissionUnit, String recordPermissionUnit) {
		MCR.addCall("user", user, "recordTypeUsesPermissionUnit", recordTypeUsesPermissionUnit,
				"recordPermissionUnit", recordPermissionUnit);
	}
}
