/*
 * Copyright 2020 Uppsala University Library
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
package se.uu.ub.cora.spider.role;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import se.uu.ub.cora.data.DataGroup;
import se.uu.ub.cora.spider.data.DataAtomicSpy;
import se.uu.ub.cora.spider.data.DataGroupSpy;
import se.uu.ub.cora.storage.RecordStorage;
import se.uu.ub.cora.storage.StorageReadResult;

public class RulesRecordPartRecordStorageSpy implements RecordStorage {

	List<DataGroup> returnedReadDataGroups = new ArrayList<>();

	@Override
	public DataGroup read(String type, String id) {
		if ("permissionRole".equals(type) && "roleWithReadRecordPartPermissions".equals(id)) {
			DataGroup createRoleForGuest = createRoleWithReadPermissions();
			returnedReadDataGroups.add(createRoleForGuest);
			return createRoleForGuest;
		}
		if ("permissionRule".equals(type) && "ruleWithOneReadPermissionPart".equals(id)) {
			DataGroup rule = createBasicPermissionRule();

			DataGroup readPermission = new DataGroupSpy("readPermissions");
			createAndAddReadPermission(readPermission, "organisation.rootOrganisation", "0");
			rule.addChild(readPermission);
			rule.addChild(new DataAtomicSpy("activeStatus", "active"));

			returnedReadDataGroups.add(rule);
			return rule;
		}
		if ("permissionRule".equals(type) && "ruleWithTwoReadPermissionPart".equals(id)) {
			DataGroup rule = createBasicPermissionRule();

			DataGroup readPermission = new DataGroupSpy("readPermissions");
			createAndAddReadPermission(readPermission, "organisation.showInPortal", "0");
			createAndAddReadPermission(readPermission, "organisation.showInDefence", "1");
			rule.addChild(readPermission);
			rule.addChild(new DataAtomicSpy("activeStatus", "active"));

			returnedReadDataGroups.add(rule);
			return rule;
		}

		if ("permissionRole".equals(type) && "roleWithWriteRecordPartPermissions".equals(id)) {
			DataGroup createRoleForGuest = createRoleWithWritePermissions();
			returnedReadDataGroups.add(createRoleForGuest);
			return createRoleForGuest;
		}
		if ("permissionRule".equals(type) && "ruleWithOneWritePermissionPart".equals(id)) {
			DataGroup rule = createBasicPermissionRule();

			DataGroup writePermission = new DataGroupSpy("writePermissions");
			createAndAddWritePermission(writePermission, "organisation.topOrganisation", "0");
			rule.addChild(writePermission);
			rule.addChild(new DataAtomicSpy("activeStatus", "active"));

			returnedReadDataGroups.add(rule);
			return rule;
		}
		if ("permissionRule".equals(type) && "ruleWithTwoWritePermissionPart".equals(id)) {
			DataGroup rule = createBasicPermissionRule();

			DataGroup writePermission = new DataGroupSpy("writePermissions");
			createAndAddWritePermission(writePermission, "organisation.showInAdvancedSearch", "0");
			createAndAddWritePermission(writePermission, "organisation.showInBrowse", "1");
			rule.addChild(writePermission);
			rule.addChild(new DataAtomicSpy("activeStatus", "active"));

			returnedReadDataGroups.add(rule);
			return rule;
		}
		if ("permissionRole".equals(type)
				&& "roleWithReadAndWriteRecordPartPermissions".equals(id)) {
			DataGroup createRole = createRoleWithReadAndWritePermissions();
			returnedReadDataGroups.add(createRole);
			return createRole;
		}
		if ("permissionRule".equals(type)
				&& "ruleWithOneReadPermissionPartTwoWritePermissionPart".equals(id)) {
			DataGroup rule = createBasicPermissionRule();

			DataGroup readPermission = new DataGroupSpy("readPermissions");
			createAndAddReadPermission(readPermission, "organisation.showInPortal", "0");
			rule.addChild(readPermission);

			DataGroup writePermission = new DataGroupSpy("writePermissions");
			createAndAddWritePermission(writePermission, "organisation.showInAdvancedSearch", "0");
			createAndAddWritePermission(writePermission, "organisation.showInBrowse", "1");
			rule.addChild(writePermission);
			rule.addChild(new DataAtomicSpy("activeStatus", "active"));

			returnedReadDataGroups.add(rule);
			return rule;
		}

		DataGroup dataGroupToReturn = new DataGroupSpy("someNameInData");
		dataGroupToReturn.addChild(new DataGroupSpy("recordInfo"));
		return dataGroupToReturn;
	}

	private DataGroup createBasicPermissionRule() {
		DataGroup rule = new DataGroupSpy("permissionRule");
		DataGroup permissionRulePart = createPermissionRulePart("action", "system.create",
				"system.read");
		rule.addChild(permissionRulePart);
		return rule;
	}

	private void createAndAddReadPermission(DataGroup readConstraints, String readPermissionValue,
			String repeatId) {
		readConstraints
				.addChild(new DataAtomicSpy("readPermission", readPermissionValue, repeatId));
	}

	private void createAndAddWritePermission(DataGroup readConstraints, String readPermissionValue,
			String repeatId) {
		readConstraints
				.addChild(new DataAtomicSpy("writePermission", readPermissionValue, repeatId));
	}

	private DataGroup createRoleWithReadPermissions() {
		DataGroup permissionRole = new DataGroupSpy("permissionRole");
		addPermissionRuleToRoleUsingRuleId(permissionRole, "ruleWithOneReadPermissionPart");
		addPermissionRuleToRoleUsingRuleId(permissionRole, "ruleWithTwoReadPermissionPart");
		permissionRole.addChild(new DataAtomicSpy("activeStatus", "active"));

		return permissionRole;
	}

	private void addPermissionRuleToRoleUsingRuleId(DataGroup permissionRole, String ruleId) {
		DataGroup permissionRuleLink = new DataGroupSpy("permissionRuleLink");
		permissionRuleLink.addChild(new DataAtomicSpy("linkedRecordType", "permissionRule"));
		permissionRuleLink.addChild(new DataAtomicSpy("linkedRecordId", ruleId));
		permissionRole.addChild(permissionRuleLink);
	}

	private DataGroup createRoleWithWritePermissions() {
		DataGroup permissionRole = new DataGroupSpy("permissionRole");
		addPermissionRuleToRoleUsingRuleId(permissionRole, "ruleWithOneWritePermissionPart");
		addPermissionRuleToRoleUsingRuleId(permissionRole, "ruleWithTwoWritePermissionPart");

		permissionRole.addChild(new DataAtomicSpy("activeStatus", "active"));

		return permissionRole;
	}

	private DataGroup createRoleWithReadAndWritePermissions() {
		DataGroup permissionRole = new DataGroupSpy("permissionRole");
		addPermissionRuleToRoleUsingRuleId(permissionRole,
				"ruleWithOneReadPermissionPartTwoWritePermissionPart");

		permissionRole.addChild(new DataAtomicSpy("activeStatus", "active"));

		return permissionRole;
	}

	private DataGroup createPermissionRulePart(String permissionType, String... value) {
		DataGroup permissionRulePart = new DataGroupSpy("permissionRulePart");
		for (int idx = 0; idx < value.length; idx++) {
			permissionRulePart.addChild(
					new DataAtomicSpy("permissionRulePartValue", value[idx], String.valueOf(idx)));
		}
		permissionRulePart.addAttributeByIdWithValue("type", permissionType);
		return permissionRulePart;
	}

	@Override
	public void create(String type, String id, DataGroup record, DataGroup collectedTerms,
			DataGroup linkList, String dataDivider) {
		// TODO Auto-generated method stub

	}

	@Override
	public void deleteByTypeAndId(String type, String id) {
		// TODO Auto-generated method stub

	}

	@Override
	public boolean linksExistForRecord(String type, String id) {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public void update(String type, String id, DataGroup record, DataGroup collectedTerms,
			DataGroup linkList, String dataDivider) {
		// TODO Auto-generated method stub

	}

	@Override
	public StorageReadResult readList(String type, DataGroup filter) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public StorageReadResult readAbstractList(String type, DataGroup filter) {
		// TODO Auto-generated method stub
		return null;
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
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public long getTotalNumberOfRecordsForType(String type, DataGroup filter) {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public long getTotalNumberOfRecordsForAbstractType(String abstractType, List<String> implementingTypes,
			DataGroup filter) {
		// TODO Auto-generated method stub
		return 0;
	}

}
