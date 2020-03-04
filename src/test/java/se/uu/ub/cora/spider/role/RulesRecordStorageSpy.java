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

import java.util.Collection;

import se.uu.ub.cora.data.DataGroup;
import se.uu.ub.cora.spider.data.DataAtomicSpy;
import se.uu.ub.cora.spider.data.DataGroupSpy;
import se.uu.ub.cora.storage.RecordStorage;
import se.uu.ub.cora.storage.StorageReadResult;

public class RulesRecordStorageSpy implements RecordStorage {

	@Override
	public DataGroup read(String type, String id) {
		if ("permissionRole".equals(type) && "ruleWithReadRecordPartPermissions".equals(id)) {
			return createRoleForGuest();
		}
		if ("permissionRule".equals(type) && "ruleWithPermissionPart".equals(id)) {
			DataGroup rule = new DataGroupSpy("permissionRule");

			DataGroup permissionRulePart = createPermissionRulePart("action", "system.create",
					"system.read");
			rule.addChild(permissionRulePart);

			DataGroup readConstraints = new DataGroupSpy("readConstraints");
			readConstraints.addChild(
					new DataAtomicSpy("readConstraint", "organisation.rootOrganisation", "0"));
			rule.addChild(readConstraints);
			// createPermissionTermRulePart(
			// "publishedStatusPermissionTerm", "system.published");
			// rule.addChild(permissionTermRulePart);

			rule.addChild(new DataAtomicSpy("activeStatus", "active"));
			return rule;
		}
		DataGroup dataGroupToReturn = new DataGroupSpy("someNameInData");
		dataGroupToReturn.addChild(new DataGroupSpy("recordInfo"));
		return dataGroupToReturn;
	}

	private DataGroup createRoleForGuest() {
		DataGroup permissionRole = new DataGroupSpy("permissionRole");

		// DataGroup permissionRuleLink = new DataGroupSpy("permissionRuleLink");
		// permissionRuleLink.addChild(new DataAtomicSpy("linkedRecordType", "permissionRule"));
		// permissionRuleLink.addChild(new DataAtomicSpy("linkedRecordId", "authorityReader"));
		// permissionRole.addChild(permissionRuleLink);

		DataGroup permissionRuleLink2 = new DataGroupSpy("permissionRuleLink");
		permissionRuleLink2.addChild(new DataAtomicSpy("linkedRecordType", "permissionRule"));
		permissionRuleLink2.addChild(new DataAtomicSpy("linkedRecordId", "ruleWithPermissionPart"));
		permissionRole.addChild(permissionRuleLink2);

		// DataGroup permissionRuleLink2 = new DataGroupSpy("permissionRuleLink");
		// permissionRuleLink2.addChild(new DataAtomicSpy("linkedRecordType", "permissionRule"));
		// permissionRuleLink2.addChild(new DataAtomicSpy("linkedRecordId", "metadataReader"));
		// permissionRole.addChild(permissionRuleLink2);

		// DataGroup permissionRuleLink3 = new DataGroupSpy("permissionRuleLink");
		// permissionRuleLink3.addChild(new DataAtomicSpy("linkedRecordType", "permissionRule"));
		// permissionRuleLink3.addChild(new DataAtomicSpy("linkedRecordId", "inactive"));
		// permissionRole.addChild(permissionRuleLink3);

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
	public boolean recordsExistForRecordType(String type) {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public boolean recordExistsForAbstractOrImplementingRecordTypeAndRecordId(String type,
			String id) {
		// TODO Auto-generated method stub
		return false;
	}

}
