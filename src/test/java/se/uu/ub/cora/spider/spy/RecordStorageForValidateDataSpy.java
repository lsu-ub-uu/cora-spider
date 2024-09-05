/*
 * Copyright 2019, 2022 Uppsala University Library
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
import java.util.Collection;
import java.util.List;
import java.util.Set;

import se.uu.ub.cora.data.DataGroup;
import se.uu.ub.cora.data.DataRecordGroup;
import se.uu.ub.cora.data.collected.Link;
import se.uu.ub.cora.data.collected.StorageTerm;
import se.uu.ub.cora.spider.data.DataAtomicOldSpy;
import se.uu.ub.cora.spider.data.DataGroupOldSpy;
import se.uu.ub.cora.spider.testdata.DataCreator;
import se.uu.ub.cora.storage.Filter;
import se.uu.ub.cora.storage.RecordNotFoundException;
import se.uu.ub.cora.storage.RecordStorage;
import se.uu.ub.cora.storage.StorageReadResult;

public class RecordStorageForValidateDataSpy implements RecordStorage {

	public Collection<List<String>> readLists = new ArrayList<>();
	public boolean readWasCalled = false;
	public boolean deleteWasCalled = false;
	public boolean createWasCalled = false;
	public boolean updateWasCalled = false;
	public boolean linksExist = false;
	public DataGroup createRecord;
	public List<String> typesList;
	public String id;
	public List<Filter> filters = new ArrayList<>();
	public boolean readListWasCalled = false;

	@Override
	public DataGroup read(List<String> types, String id) {
		this.typesList = types;
		this.id = id;
		readWasCalled = true;
		for (String type : types) {

			if ("recordType".equals(type)) {
				return DataCreator.createRecordTypeWithIdAndUserSuppliedIdAndAbstractAndPublicRead(
						id, "true", "false", "false");
			}
			if ("recordType_NOT_EXISTING".equals(type)) {
				throw RecordNotFoundException.withMessage(
						"No records exists with recordType: " + type + " and recordId " + id);
			}
		}

		DataGroup dataGroupToReturn = new DataGroupOldSpy("someNameInData");
		dataGroupToReturn.addChild(new DataGroupOldSpy("recordInfo"));
		return dataGroupToReturn;
	}

	@Override
	public void create(String type, String id, DataGroup record, Set<StorageTerm> storageTerms,
			Set<Link> links, String dataDivider) {
		createWasCalled = true;
		createRecord = record;
	}

	@Override
	public void deleteByTypeAndId(String type, String id) {
		deleteWasCalled = true;
	}

	@Override
	public boolean linksExistForRecord(String type, String id) {
		if ("place".equals(type)) {
			if (id.equals("place:0001")) {
				return false;
			}
		} else if ("authority".equals(type)) {
			if ("place:0003".equals(id)) {
				return true;
			}
		}
		return linksExist;
	}

	@Override
	public void update(String type, String id, DataGroup record, Set<StorageTerm> collectedTerms,
			Set<Link> links, String dataDivider) {
		updateWasCalled = true;
	}

	@Override
	public StorageReadResult readList(List<String> type, Filter filter) {
		readListWasCalled = true;
		readLists.add(type);
		filters.add(filter);
		if ("recordType".equals(type)) {
			ArrayList<DataGroup> recordTypes = new ArrayList<>();
			recordTypes.add(read(List.of("recordType"), "abstract"));
			recordTypes.add(read(List.of("recordType"), "child1"));
			recordTypes.add(read(List.of("recordType"), "child2"));
			recordTypes.add(read(List.of("recordType"), "abstract2"));
			recordTypes.add(read(List.of("recordType"), "child1_2"));
			recordTypes.add(read(List.of("recordType"), "child2_2"));
			recordTypes.add(read(List.of("recordType"), "otherType"));
			StorageReadResult spiderReadResult = new StorageReadResult();
			spiderReadResult.listOfDataGroups = recordTypes;
			return spiderReadResult;
		}
		if ("child1_2".equals(type)) {
			throw RecordNotFoundException.withMessage("No records exists with recordType: " + type);
		}
		StorageReadResult spiderReadResult = new StorageReadResult();
		spiderReadResult.listOfDataGroups = new ArrayList<>();
		spiderReadResult.totalNumberOfMatches = 199;
		return spiderReadResult;
	}

	// @Override
	// public StorageReadResult readAbstractList(String type, DataGroup filter) {
	// StorageReadResult spiderReadResult = new StorageReadResult();
	// spiderReadResult.totalNumberOfMatches = 199;
	// spiderReadResult.listOfDataGroups = new ArrayList<>();
	// readLists.add(type);
	// if ("abstract".equals(type)) {
	// ArrayList<DataGroup> records = new ArrayList<>();
	// records.add(createChildWithRecordTypeAndRecordId("implementing1", "child1_2"));
	//
	// records.add(createChildWithRecordTypeAndRecordId("implementing2", "child2_2"));
	// spiderReadResult.listOfDataGroups = records;
	// return spiderReadResult;
	// }
	// if ("abstract2".equals(type)) {
	// ArrayList<DataGroup> records = new ArrayList<>();
	//
	// records.add(createChildWithRecordTypeAndRecordId("implementing2", "child2_2"));
	// spiderReadResult.listOfDataGroups = records;
	// return spiderReadResult;
	// }
	// if ("user".equals(type)) {
	// ArrayList<DataGroup> records = new ArrayList<>();
	//
	// DataGroup inactiveUser = createUserWithIdAndActiveStatus("inactiveUserId", "inactive");
	// records.add(inactiveUser);
	//
	// DataGroup user = createActiveUserWithIdAndAddDefaultRoles("someUserId");
	// records.add(user);
	//
	// DataGroup userWithPermissionTerm = createUserWithOneRoleWithOnePermission();
	// records.add(userWithPermissionTerm);
	//
	// DataGroup userWithTwoRolesAndTwoPermissionTerm = createActiveUserWithIdAndAddDefaultRoles(
	// "userWithTwoRolesPermissionTerm");
	// addRoleToUser("admin", userWithTwoRolesAndTwoPermissionTerm);
	//
	// List<DataGroup> userRoles = userWithTwoRolesAndTwoPermissionTerm
	// .getAllGroupsWithNameInData("userRole");
	//
	// DataGroup permissionTerm = createPermissionTermWithIdAndValues(
	// "organisationPermissionTerm", "system.*");
	// DataGroup userRole = userRoles.get(0);
	// userRole.addChild(permissionTerm);
	//
	// DataGroup permissionTerm2 = createPermissionTermWithIdAndValues("journalPermissionTerm",
	// "system.abc", "system.def");
	// DataGroup userRole2 = userRoles.get(1);
	// userRole2.addChild(permissionTerm2);
	//
	// DataGroup permissionTerm2_role2 = createPermissionTermWithIdAndValues(
	// "organisationPermissionTerm", "system.*");
	// userRole2.addChild(permissionTerm2_role2);
	//
	// records.add(userWithTwoRolesAndTwoPermissionTerm);
	//
	// spiderReadResult.listOfDataGroups = records;
	// return spiderReadResult;
	// }
	// return spiderReadResult;
	// }

	private DataGroup createUserWithOneRoleWithOnePermission() {
		DataGroup userWithPermissionTerm = createActiveUserWithIdAndAddDefaultRoles(
				"userWithPermissionTerm");
		DataGroup permissionTerm = createPermissionTermWithIdAndValues("organisationPermissionTerm",
				"system.*");

		DataGroup userRole = userWithPermissionTerm.getFirstGroupWithNameInData("userRole");
		userRole.addChild(permissionTerm);
		return userWithPermissionTerm;
	}

	private DataGroup createActiveUserWithIdAndAddDefaultRoles(String userId) {
		DataGroup userWithPermissionTerm = createUserWithIdAndActiveStatus(userId, "active");
		addRoleToUser("guest", userWithPermissionTerm);
		return userWithPermissionTerm;
	}

	private DataGroup createPermissionTermWithIdAndValues(String permissionTermId,
			String... value) {
		DataGroup permissionTerm = new DataGroupOldSpy("permissionTermRulePart");
		DataGroup rule = createLinkWithNameInDataRecordtypeAndRecordId("rule", "collectTerm",
				permissionTermId);
		permissionTerm.addChild(rule);

		for (int i = 0; i < value.length; i++) {
			permissionTerm.addChild(new DataAtomicOldSpy("value", value[i], String.valueOf(i)));
		}
		return permissionTerm;
	}

	private DataGroup createLinkWithNameInDataRecordtypeAndRecordId(String nameInData,
			String linkedRecordType, String linkedRecordId) {
		DataGroup link = new DataGroupOldSpy(nameInData);
		link.addChild(new DataAtomicOldSpy("linkedRecordType", linkedRecordType));
		link.addChild(new DataAtomicOldSpy("linkedRecordId", linkedRecordId));
		return link;
	}

	private void addRoleToUser(String roleId, DataGroup user) {
		DataGroup outerUserRole = createUserRoleWithId(roleId);
		user.addChild(outerUserRole);
	}

	private DataGroup createUserRoleWithId(String roleId) {
		DataGroup outerUserRole = new DataGroupOldSpy("userRole");
		DataGroup innerUserRole = new DataGroupOldSpy("userRole");
		innerUserRole.addChild(new DataAtomicOldSpy("linkedRecordType", "permissionRole"));
		innerUserRole.addChild(new DataAtomicOldSpy("linkedRecordId", roleId));
		outerUserRole.addChild(innerUserRole);
		return outerUserRole;
	}

	private DataGroup createUserWithIdAndActiveStatus(String userId, String activeStatus) {
		DataGroup inactiveUser = new DataGroupOldSpy("user");
		DataGroup recordInfo = new DataGroupOldSpy("recordInfo");
		recordInfo.addChild(new DataAtomicOldSpy("id", userId));
		inactiveUser.addChild(recordInfo);
		inactiveUser.addChild(new DataAtomicOldSpy("activeStatus", activeStatus));
		return inactiveUser;
	}

	private DataGroup createChildWithRecordTypeAndRecordId(String recordType, String recordId) {
		DataGroup child1 = new DataGroupOldSpy(recordId);
		child1.addChild(
				DataCreator.createRecordInfoWithRecordTypeAndRecordId(recordType, recordId));
		return child1;
	}

	@Override
	public Set<Link> getLinksToRecord(String type, String id) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public boolean recordExists(List<String> types, String id) {
		return false;
	}

	@Override
	public long getTotalNumberOfRecordsForTypes(List<String> types, Filter filter) {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public DataRecordGroup read(String type, String id) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public StorageReadResult readList(String type, Filter filter) {
		// TODO Auto-generated method stub
		return null;
	}
}
