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
package se.uu.ub.cora.spider.spy;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import se.uu.ub.cora.data.DataGroup;
import se.uu.ub.cora.data.collected.Link;
import se.uu.ub.cora.data.collected.StorageTerm;
import se.uu.ub.cora.spider.data.DataAtomicSpy;
import se.uu.ub.cora.spider.data.DataGroupOldSpy;
import se.uu.ub.cora.storage.RecordStorage;
import se.uu.ub.cora.storage.StorageReadResult;

public class RulesRecordPartRecordStorageSpy implements RecordStorage {

	public List<DataGroup> returnedReadDataGroups = new ArrayList<>();

	@Override
	public DataGroup read(List<String> types, String id) {
		if ("permissionRole".equals(types) && "roleWithReadRecordPartPermissions".equals(id)) {
			DataGroup createRoleForGuest = createRoleWithReadPermissions();
			returnedReadDataGroups.add(createRoleForGuest);
			return createRoleForGuest;
		}
		if ("permissionRule".equals(types) && "ruleWithOneReadPermissionPart".equals(id)) {
			DataGroup rule = createBasicPermissionRule();

			DataGroup readPermission = new DataGroupOldSpy("readPermissions");
			createAndAddReadPermission(readPermission, "organisation.rootOrganisation", "0");
			rule.addChild(readPermission);
			rule.addChild(new DataAtomicSpy("activeStatus", "active"));

			returnedReadDataGroups.add(rule);
			return rule;
		}
		if ("permissionRule".equals(types) && "ruleWithTwoReadPermissionPart".equals(id)) {
			DataGroup rule = createBasicPermissionRule();

			DataGroup readPermission = new DataGroupOldSpy("readPermissions");
			createAndAddReadPermission(readPermission, "organisation.showInPortal", "0");
			createAndAddReadPermission(readPermission, "organisation.showInDefence", "1");
			rule.addChild(readPermission);
			rule.addChild(new DataAtomicSpy("activeStatus", "active"));

			returnedReadDataGroups.add(rule);
			return rule;
		}

		if ("permissionRole".equals(types) && "roleWithWriteRecordPartPermissions".equals(id)) {
			DataGroup createRoleForGuest = createRoleWithWritePermissions();
			returnedReadDataGroups.add(createRoleForGuest);
			return createRoleForGuest;
		}
		if ("permissionRule".equals(types) && "ruleWithOneWritePermissionPart".equals(id)) {
			DataGroup rule = createBasicPermissionRule();

			DataGroup writePermission = new DataGroupOldSpy("writePermissions");
			createAndAddWritePermission(writePermission, "organisation.topOrganisation", "0");
			rule.addChild(writePermission);
			rule.addChild(new DataAtomicSpy("activeStatus", "active"));

			returnedReadDataGroups.add(rule);
			return rule;
		}
		if ("permissionRule".equals(types) && "ruleWithTwoWritePermissionPart".equals(id)) {
			DataGroup rule = createBasicPermissionRule();

			DataGroup writePermission = new DataGroupOldSpy("writePermissions");
			createAndAddWritePermission(writePermission, "organisation.showInAdvancedSearch", "0");
			createAndAddWritePermission(writePermission, "organisation.showInBrowse", "1");
			rule.addChild(writePermission);
			rule.addChild(new DataAtomicSpy("activeStatus", "active"));

			returnedReadDataGroups.add(rule);
			return rule;
		}
		if ("permissionRole".equals(types)
				&& "roleWithReadAndWriteRecordPartPermissions".equals(id)) {
			DataGroup createRole = createRoleWithReadAndWritePermissions();
			returnedReadDataGroups.add(createRole);
			return createRole;
		}
		if ("permissionRule".equals(types)
				&& "ruleWithOneReadPermissionPartTwoWritePermissionPart".equals(id)) {
			DataGroup rule = createBasicPermissionRule();

			DataGroup readPermission = new DataGroupOldSpy("readPermissions");
			createAndAddReadPermission(readPermission, "organisation.showInPortal", "0");
			rule.addChild(readPermission);

			DataGroup writePermission = new DataGroupOldSpy("writePermissions");
			createAndAddWritePermission(writePermission, "organisation.showInAdvancedSearch", "0");
			createAndAddWritePermission(writePermission, "organisation.showInBrowse", "1");
			rule.addChild(writePermission);
			rule.addChild(new DataAtomicSpy("activeStatus", "active"));

			returnedReadDataGroups.add(rule);
			return rule;
		}

		DataGroup dataGroupToReturn = new DataGroupOldSpy("someNameInData");
		dataGroupToReturn.addChild(new DataGroupOldSpy("recordInfo"));
		return dataGroupToReturn;
	}

	private DataGroup createBasicPermissionRule() {
		DataGroup rule = new DataGroupOldSpy("permissionRule");
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
		DataGroup permissionRole = new DataGroupOldSpy("permissionRole");
		addPermissionRuleToRoleUsingRuleId(permissionRole, "ruleWithOneReadPermissionPart");
		addPermissionRuleToRoleUsingRuleId(permissionRole, "ruleWithTwoReadPermissionPart");
		permissionRole.addChild(new DataAtomicSpy("activeStatus", "active"));

		return permissionRole;
	}

	private void addPermissionRuleToRoleUsingRuleId(DataGroup permissionRole, String ruleId) {
		DataGroup permissionRuleLink = new DataGroupOldSpy("permissionRuleLink");
		permissionRuleLink.addChild(new DataAtomicSpy("linkedRecordType", "permissionRule"));
		permissionRuleLink.addChild(new DataAtomicSpy("linkedRecordId", ruleId));
		permissionRole.addChild(permissionRuleLink);
	}

	private DataGroup createRoleWithWritePermissions() {
		DataGroup permissionRole = new DataGroupOldSpy("permissionRole");
		addPermissionRuleToRoleUsingRuleId(permissionRole, "ruleWithOneWritePermissionPart");
		addPermissionRuleToRoleUsingRuleId(permissionRole, "ruleWithTwoWritePermissionPart");

		permissionRole.addChild(new DataAtomicSpy("activeStatus", "active"));

		return permissionRole;
	}

	private DataGroup createRoleWithReadAndWritePermissions() {
		DataGroup permissionRole = new DataGroupOldSpy("permissionRole");
		addPermissionRuleToRoleUsingRuleId(permissionRole,
				"ruleWithOneReadPermissionPartTwoWritePermissionPart");

		permissionRole.addChild(new DataAtomicSpy("activeStatus", "active"));

		return permissionRole;
	}

	private DataGroup createPermissionRulePart(String permissionType, String... value) {
		DataGroup permissionRulePart = new DataGroupOldSpy("permissionRulePart");
		for (int idx = 0; idx < value.length; idx++) {
			permissionRulePart.addChild(
					new DataAtomicSpy("permissionRulePartValue", value[idx], String.valueOf(idx)));
		}
		permissionRulePart.addAttributeByIdWithValue("type", permissionType);
		return permissionRulePart;
	}

	@Override
	public void create(String type, String id, DataGroup record, List<StorageTerm> storageTerms,
			List<Link> links, String dataDivider) {
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
	public void update(String type, String id, DataGroup record, List<StorageTerm> collectedTerms,
			List<Link> links, String dataDivider) {
		// TODO Auto-generated method stub

	}

	@Override
	public StorageReadResult readList(List<String> type, DataGroup filter) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Collection<DataGroup> generateLinkCollectionPointingToRecord(String type, String id) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public boolean recordExistsForListOfImplementingRecordTypesAndRecordId(List<String> types,
			String id) {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public long getTotalNumberOfRecordsForTypes(List<String> types, DataGroup filter) {
		// TODO Auto-generated method stub
		return 0;
	}
}
