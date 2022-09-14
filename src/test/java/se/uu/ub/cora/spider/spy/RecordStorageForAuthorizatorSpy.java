/*
 * Copyright 2015, 2017, 2019 Uppsala University Library
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
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import se.uu.ub.cora.data.DataGroup;
import se.uu.ub.cora.data.collected.Link;
import se.uu.ub.cora.data.collected.StorageTerm;
import se.uu.ub.cora.spider.data.DataAtomicSpy;
import se.uu.ub.cora.spider.data.DataGroupOldSpy;
import se.uu.ub.cora.storage.RecordNotFoundException;
import se.uu.ub.cora.storage.RecordStorage;
import se.uu.ub.cora.storage.StorageReadResult;

public class RecordStorageForAuthorizatorSpy implements RecordStorage {

	public Collection<String> readLists = new ArrayList<>();
	public boolean readWasCalled = false;
	public boolean deleteWasCalled = false;
	public boolean createWasCalled = false;
	public boolean updateWasCalled = false;
	public boolean linksExist = false;
	public DataGroup createRecord;
	public String type;
	public String id;
	public int numOfTimesReadWasCalled = 0;
	public List<DataGroup> filters = new ArrayList<>();
	public boolean readListWasCalled = false;
	public Map<String, Integer> userReadNumberOfTimesMap = new HashMap<>();

	@Override
	public DataGroup read(String type, String id) {
		numOfTimesReadWasCalled++;
		this.type = type;
		this.id = id;
		readWasCalled = true;

		if (!userReadNumberOfTimesMap.containsKey(id)) {
			userReadNumberOfTimesMap.put(id, 1);
		} else {
			userReadNumberOfTimesMap.put(id, userReadNumberOfTimesMap.get(id) + 1);
		}

		if ("collectPermissionTerm".equals(type) && "publishedStatusPermissionTerm".equals(id)) {
			return createCollectPermissionTermWIthKey("PUBLISHED_STATUS");
		}
		if ("collectPermissionTerm".equals(type) && "deletedStatusPermissionTerm".equals(id)) {
			return createCollectPermissionTermWIthKey("DELETED_STATUS");
		}
		if ("collectPermissionTerm".equals(type) && "organisationPermissionTerm".equals(id)) {
			return createCollectPermissionTermWIthKey("OWNING_ORGANISATION");
		}
		if ("collectPermissionTerm".equals(type) && "journalPermissionTerm".equals(id)) {
			return createCollectPermissionTermWIthKey("JOURNAL_ACCESS");
		}

		if ("user".equals(type)) {
			if ("inactiveUserId".equals(id)) {
				DataGroup inactiveUser = createUserWithIdAndActiveStatus("inactiveUserId",
						"inactive");
				return inactiveUser;
			}

			if ("someUserId".equals(id)) {
				DataGroup user = createActiveUserWithIdAndAddDefaultRoles("someUserId");
				return user;
			}

			if ("userWithPermissionTerm".equals(id)) {
				DataGroup userWithPermissionTerm = createUserWithOneRoleWithOnePermission();
				return userWithPermissionTerm;
			}

			if ("userWithTwoRolesPermissionTerm".equals(id)) {
				DataGroup userWithTwoRolesAndTwoPermissionTerm = createActiveUserWithIdAndAddDefaultRoles(
						"userWithTwoRolesPermissionTerm");
				addRoleToUser("admin", userWithTwoRolesAndTwoPermissionTerm);

				List<DataGroup> userRoles = userWithTwoRolesAndTwoPermissionTerm
						.getAllGroupsWithNameInData("userRole");

				DataGroup permissionTerm = createPermissionTermWithIdAndValues(
						"organisationPermissionTerm", "system.*");
				DataGroup userRole = userRoles.get(0);
				userRole.addChild(permissionTerm);

				DataGroup permissionTerm2 = createPermissionTermWithIdAndValues(
						"journalPermissionTerm", "system.abc", "system.def");
				DataGroup userRole2 = userRoles.get(1);
				userRole2.addChild(permissionTerm2);

				DataGroup permissionTerm2_role2 = createPermissionTermWithIdAndValues(
						"organisationPermissionTerm", "system.*");
				userRole2.addChild(permissionTerm2_role2);

				return userWithTwoRolesAndTwoPermissionTerm;
			}
			if ("nonExistingUserId".equals(id)) {
				throw new RecordNotFoundException("No record exists with recordId: " + id);
			}
		}

		DataGroup dataGroupToReturn = new DataGroupOldSpy("someNameInData");
		dataGroupToReturn.addChild(new DataGroupOldSpy("recordInfo"));
		return dataGroupToReturn;
	}

	private DataGroup createCollectPermissionTermWIthKey(String key) {
		DataGroup collectTerm = new DataGroupOldSpy("collectTerm");
		DataGroup extraData = new DataGroupOldSpy("extraData");
		collectTerm.addChild(extraData);
		extraData.addChild(new DataAtomicSpy("permissionKey", key));

		return collectTerm;
	}

	@Override
	public void create(String type, String id, DataGroup record, List<StorageTerm> storageTerms,
			List<Link> links, String dataDivider) {
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
	public void update(String type, String id, DataGroup record, List<StorageTerm> collectedTerms,
			List<Link> links, String dataDivider) {
		updateWasCalled = true;
	}

	@Override
	public StorageReadResult readList(String type, DataGroup filter) {
		readListWasCalled = true;
		readLists.add(type);
		filters.add(filter);
		StorageReadResult spiderReadResult = new StorageReadResult();
		spiderReadResult.listOfDataGroups = new ArrayList<>();
		spiderReadResult.totalNumberOfMatches = 199;
		return spiderReadResult;
	}

	@Override
	public StorageReadResult readAbstractList(String type, DataGroup filter) {
		StorageReadResult spiderReadResult = new StorageReadResult();
		spiderReadResult.totalNumberOfMatches = 199;
		spiderReadResult.listOfDataGroups = new ArrayList<>();
		readLists.add(type);
		if ("user".equals(type)) {
			ArrayList<DataGroup> records = new ArrayList<>();

			DataGroup inactiveUser = createUserWithIdAndActiveStatus("inactiveUserId", "inactive");
			records.add(inactiveUser);

			DataGroup user = createActiveUserWithIdAndAddDefaultRoles("someUserId");
			records.add(user);

			DataGroup userWithPermissionTerm = createUserWithOneRoleWithOnePermission();
			records.add(userWithPermissionTerm);

			DataGroup userWithTwoRolesAndTwoPermissionTerm = createActiveUserWithIdAndAddDefaultRoles(
					"userWithTwoRolesPermissionTerm");
			addRoleToUser("admin", userWithTwoRolesAndTwoPermissionTerm);

			List<DataGroup> userRoles = userWithTwoRolesAndTwoPermissionTerm
					.getAllGroupsWithNameInData("userRole");

			DataGroup permissionTerm = createPermissionTermWithIdAndValues(
					"organisationPermissionTerm", "system.*");
			DataGroup userRole = userRoles.get(0);
			userRole.addChild(permissionTerm);

			DataGroup permissionTerm2 = createPermissionTermWithIdAndValues("journalPermissionTerm",
					"system.abc", "system.def");
			DataGroup userRole2 = userRoles.get(1);
			userRole2.addChild(permissionTerm2);

			DataGroup permissionTerm2_role2 = createPermissionTermWithIdAndValues(
					"organisationPermissionTerm", "system.*");
			userRole2.addChild(permissionTerm2_role2);

			records.add(userWithTwoRolesAndTwoPermissionTerm);

			spiderReadResult.listOfDataGroups = records;
			return spiderReadResult;
		}
		return spiderReadResult;
	}

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
		DataGroup user = createUserWithIdAndActiveStatus(userId, "active");
		addRoleToUser("guest", user);
		return user;
	}

	private DataGroup createPermissionTermWithIdAndValues(String permissionTermId,
			String... value) {
		DataGroup permissionTerm = new DataGroupOldSpy("permissionTermRulePart");
		DataGroup rule = createLinkWithNameInDataRecordtypeAndRecordId("rule",
				"collectPermissionTerm", permissionTermId);
		permissionTerm.addChild(rule);

		for (int i = 0; i < value.length; i++) {
			permissionTerm.addChild(new DataAtomicSpy("value", value[i], String.valueOf(i)));
		}
		return permissionTerm;
	}

	private DataGroup createLinkWithNameInDataRecordtypeAndRecordId(String nameInData,
			String linkedRecordType, String linkedRecordId) {
		DataGroup link = new DataGroupOldSpy(nameInData);
		link.addChild(new DataAtomicSpy("linkedRecordType", linkedRecordType));
		link.addChild(new DataAtomicSpy("linkedRecordId", linkedRecordId));
		return link;
	}

	private void addRoleToUser(String roleId, DataGroup user) {
		DataGroup outerUserRole = createUserRoleWithId(roleId);
		user.addChild(outerUserRole);
	}

	private DataGroup createUserRoleWithId(String roleId) {
		DataGroup outerUserRole = new DataGroupOldSpy("userRole");
		DataGroup innerUserRole = new DataGroupOldSpy("userRole");
		innerUserRole.addChild(new DataAtomicSpy("linkedRecordType", "permissionRole"));
		innerUserRole.addChild(new DataAtomicSpy("linkedRecordId", roleId));
		outerUserRole.addChild(innerUserRole);
		return outerUserRole;
	}

	private DataGroup createUserWithIdAndActiveStatus(String userId, String activeStatus) {
		DataGroup inactiveUser = new DataGroupOldSpy("user");
		DataGroup recordInfo = new DataGroupOldSpy("recordInfo");
		recordInfo.addChild(new DataAtomicSpy("id", userId));
		inactiveUser.addChild(recordInfo);
		inactiveUser.addChild(new DataAtomicSpy("activeStatus", activeStatus));
		return inactiveUser;
	}

	@Override
	public DataGroup readLinkList(String type, String id) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Collection<DataGroup> generateLinkCollectionPointingToRecord(String type, String id) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public boolean recordExistsForAbstractOrImplementingRecordTypeAndRecordId(String type,
			String id) {
		return false;
	}

	@Override
	public long getTotalNumberOfRecordsForType(String type, DataGroup filter) {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public long getTotalNumberOfRecordsForAbstractType(String abstractType,
			List<String> implementingTypes, DataGroup filter) {
		// TODO Auto-generated method stub
		return 0;
	}

}
