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
import java.util.Collection;

import se.uu.ub.cora.bookkeeper.data.DataAtomic;
import se.uu.ub.cora.bookkeeper.data.DataGroup;
import se.uu.ub.cora.spider.record.storage.RecordNotFoundException;
import se.uu.ub.cora.spider.record.storage.RecordStorage;
import se.uu.ub.cora.spider.testdata.DataCreator;

public class RecordStorageSpy implements RecordStorage {

	public Collection<String> readLists = new ArrayList<>();
	public boolean readWasCalled = false;
	public boolean deleteWasCalled = false;
	public boolean createWasCalled = false;
	public boolean updateWasCalled = false;
	public boolean linksExist = false;
	public DataGroup createRecord;

	@Override
	public DataGroup read(String type, String id) {
		readWasCalled = true;
		if ("abstract".equals(id)) {
			return DataCreator.createRecordTypeWithIdAndUserSuppliedIdAndAbstract(id, "false",
					"true");
		}
		if ("abstract2".equals(id)) {
			return DataCreator.createRecordTypeWithIdAndUserSuppliedIdAndAbstract(id, "false",
					"true");
		}
		if ("child1".equals(id)) {
			return DataCreator.createRecordTypeWithIdAndUserSuppliedIdAndAbstract(id, "true",
					"false");
		}
		if ("child2".equals(id)) {
			return DataCreator.createRecordTypeWithIdAndUserSuppliedIdAndParentId(id, "true",
					"abstract");
		}
		if ("child1_2".equals(id)) {
			return DataCreator.createRecordTypeWithIdAndUserSuppliedIdAndParentId(id, "true",
					"abstract2");
		}
		if ("child2_2".equals(id)) {
			return DataCreator.createRecordTypeWithIdAndUserSuppliedIdAndParentId(id, "true",
					"abstract2");
		}
		if ("otherType".equals(id)) {
			return DataCreator.createRecordTypeWithIdAndUserSuppliedIdAndParentId(id, "true",
					"NOT_ABSTRACT");
		}
		if ("spyType".equals(id)) {
			return DataCreator.createRecordTypeWithIdAndUserSuppliedIdAndAbstract(id, "false",
					"false");
		}
		if ("recordType".equals(type) && "image".equals(id)) {
			return DataCreator.createRecordTypeWithIdAndUserSuppliedIdAndParentId("image", "true",
					"binary");

		}
		if (type.equals("recordType") && ("place".equals(id))) {
			DataGroup dataGroup = DataCreator.createDataGroupWithNameInDataTypeAndId("authority",
					"recordType", id);
			dataGroup.addChild(DataAtomic.withNameInDataAndValue("abstract", "false"));
			DataGroup parent = DataGroup.withNameInData("parentId");
			parent.addChild(DataAtomic.withNameInDataAndValue("linkedRecordType", "recordType"));
			parent.addChild(DataAtomic.withNameInDataAndValue("linkedRecordId", "authority"));
			dataGroup.addChild(parent);
			return dataGroup;
		}

		if ("image".equals(type) && "image:123456789".equals(id)) {
			return DataCreator.createRecordWithNameInDataAndIdAndTypeAndLinkedRecordId("image",
					"image:123456789", "image", "cora").toDataGroup();
		}
		if ("recordType".equals(type) && "book".equals(id)) {
			DataGroup book = DataGroup.withNameInData("recordType");
			book.addChild(DataCreator.createChildWithNamInDataLinkedTypeLinkedId("metadataId",
					"metadataGroup", "bookGroup"));
			return book;

		}
		if ("book".equals(type) && "book1".equals(id)) {
			DataGroup book = DataGroup.withNameInData("book");
			DataGroup recordInfo = DataCreator
					.createRecordInfoWithIdAndTypeAndLinkedRecordId("book1", "book", "testSystem")
					.toDataGroup();
			book.addChild(recordInfo);
			// book.addChild(DataCreator.createChildWithNamInDataLinkedTypeLinkedId("metadataId",
			// "metadataGroup", "bookGroup"));
			return book;

		}
		if ("permissionRole".equals(type) && "guest".equals(id)) {
			DataGroup permissionRole = DataGroup.withNameInData("permissionRole");

			DataGroup permissionRuleLink = DataGroup.withNameInData("permissionRuleLink");
			permissionRuleLink.addChild(
					DataAtomic.withNameInDataAndValue("linkedRecordType", "permissionRule"));
			permissionRuleLink.addChild(
					DataAtomic.withNameInDataAndValue("linkedRecordId", "authorityReader"));
			permissionRole.addChild(permissionRuleLink);

			DataGroup permissionRuleLink2 = DataGroup.withNameInData("permissionRuleLink");
			permissionRuleLink2.addChild(
					DataAtomic.withNameInDataAndValue("linkedRecordType", "permissionRule"));
			permissionRuleLink2.addChild(
					DataAtomic.withNameInDataAndValue("linkedRecordId", "metadataReader"));
			permissionRole.addChild(permissionRuleLink2);

			permissionRole.addChild(DataAtomic.withNameInDataAndValue("activeStatus", "active"));

			return permissionRole;
		}
		if ("permissionRole".equals(type) && "inactive".equals(id)) {
			DataGroup permissionRole = DataGroup.withNameInData("permissionRole");

			DataGroup permissionRuleLink = DataGroup.withNameInData("permissionRuleLink");
			permissionRuleLink.addChild(
					DataAtomic.withNameInDataAndValue("linkedRecordType", "permissionRule"));
			permissionRuleLink.addChild(
					DataAtomic.withNameInDataAndValue("linkedRecordId", "authorityReader"));
			permissionRole.addChild(permissionRuleLink);

			permissionRole.addChild(DataAtomic.withNameInDataAndValue("activeStatus", "inactive"));

			return permissionRole;
		}
		if ("permissionRule".equals(type) && "authorityReader".equals(id)) {
			DataGroup rule = DataGroup.withNameInData("permissionRule");

			DataGroup permissionRulePart = DataGroup.withNameInData("permissionRulePart");
			permissionRulePart.addChild(DataAtomic.withNameInDataAndValueAndRepeatId(
					"permissionRulePartValue", "system.create", "1"));
			permissionRulePart.addChild(DataAtomic.withNameInDataAndValueAndRepeatId(
					"permissionRulePartValue", "system.read", "2"));
			permissionRulePart.addAttributeByIdWithValue("type", "action");
			rule.addChild(permissionRulePart);

			DataGroup permissionRulePart2 = DataGroup.withNameInData("permissionRulePart");
			permissionRulePart2.addChild(DataAtomic.withNameInDataAndValueAndRepeatId(
					"permissionRulePartValue", "system.person", "1"));
			permissionRulePart2.addChild(DataAtomic.withNameInDataAndValueAndRepeatId(
					"permissionRulePartValue", "system.place", "2"));
			permissionRulePart2.addAttributeByIdWithValue("type", "recordType");
			rule.addChild(permissionRulePart2);

			rule.addChild(DataAtomic.withNameInDataAndValue("activeStatus", "active"));
			return rule;
		}
		if ("permissionRule".equals(type) && "metadataReader".equals(id)) {
			DataGroup rule = DataGroup.withNameInData("permissionRule");

			DataGroup permissionRulePart = DataGroup.withNameInData("permissionRulePart");
			permissionRulePart.addChild(DataAtomic.withNameInDataAndValueAndRepeatId(
					"permissionRulePartValue", "system.create", "1"));
			permissionRulePart.addChild(DataAtomic.withNameInDataAndValueAndRepeatId(
					"permissionRulePartValue", "system.read", "2"));
			permissionRulePart.addAttributeByIdWithValue("type", "action");
			rule.addChild(permissionRulePart);

			DataGroup permissionRulePart2 = DataGroup.withNameInData("permissionRulePart");
			permissionRulePart2.addChild(DataAtomic.withNameInDataAndValueAndRepeatId(
					"permissionRulePartValue", "system.person", "1"));
			permissionRulePart2.addChild(DataAtomic.withNameInDataAndValueAndRepeatId(
					"permissionRulePartValue", "system.place", "2"));
			permissionRulePart2.addAttributeByIdWithValue("type", "recordType");
			rule.addChild(permissionRulePart2);

			rule.addChild(DataAtomic.withNameInDataAndValue("activeStatus", "inactive"));
			return rule;
		}
		if ("inactiveUserId".equals(id)) {
			DataGroup user = DataGroup.withNameInData("user");
			user.addChild(DataAtomic.withNameInDataAndValue("activeStatus", "inactive"));
			return user;
		}
		if ("someUserId".equals(id)) {
			DataGroup user = DataGroup.withNameInData("user");
			user.addChild(DataAtomic.withNameInDataAndValue("activeStatus", "active"));
			addRolesToUser(user);

			return user;
		}
		if ("abstractAuthority".equals(type)) {
			DataGroup abstractAuthority = DataGroup.withNameInData("abstractAuthority");
			DataGroup recordInfo = DataGroup.withNameInData("recordInfo");
			recordInfo.addChild(DataAtomic.withNameInDataAndValue("id", "place001"));
			abstractAuthority.addChild(recordInfo);
			return abstractAuthority;

		}
		if ("place".equals(type)) {
			return DataCreator.createDataGroupWithNameInDataTypeAndId("authority", "place", id);
		}
		return null;
	}

	@Override
	public void create(String type, String id, DataGroup record, DataGroup linkList,
			String dataDivider) {
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
	public void update(String type, String id, DataGroup record, DataGroup linkList,
			String dataDivider) {
		updateWasCalled = true;
	}

	@Override
	public Collection<DataGroup> readList(String type) {
		readLists.add(type);
		if ("recordType".equals(type)) {
			ArrayList<DataGroup> recordTypes = new ArrayList<>();
			recordTypes.add(read("recordType", "abstract"));
			recordTypes.add(read("recordType", "child1"));
			recordTypes.add(read("recordType", "child2"));
			recordTypes.add(read("recordType", "abstract2"));
			recordTypes.add(read("recordType", "child1_2"));
			recordTypes.add(read("recordType", "child2_2"));
			recordTypes.add(read("recordType", "otherType"));
			return recordTypes;
		}
		if ("child1_2".equals(type)) {
			throw new RecordNotFoundException("No records exists with recordType: " + type);
		}
		return new ArrayList<DataGroup>();
	}

	@Override
	public Collection<DataGroup> readAbstractList(String type) {
		readLists.add(type);
		if ("abstract".equals(type)) {
			ArrayList<DataGroup> records = new ArrayList<>();
			records.add(createChildWithRecordTypeAndRecordId("implementing1", "child1_2"));

			records.add(createChildWithRecordTypeAndRecordId("implementing2", "child2_2"));
			return records;
		}
		if ("abstract2".equals(type)) {
			ArrayList<DataGroup> records = new ArrayList<>();

			records.add(createChildWithRecordTypeAndRecordId("implementing2", "child2_2"));
			return records;
		}
		if ("user".equals(type)) {
			ArrayList<DataGroup> records = new ArrayList<>();

			DataGroup inactiveUser = createUserWithIdAndActiveStatus("inactiveUserId", "inactive");
			records.add(inactiveUser);

			// DataGroup user = DataGroup.withNameInData("user");
			// DataGroup recordInfo2 = DataGroup.withNameInData("recordInfo");
			// recordInfo2.addChild(DataAtomic.withNameInDataAndValue("id",
			// "someUserId"));
			// user.addChild(recordInfo2);
			// user.addChild(DataAtomic.withNameInDataAndValue("activeStatus",
			// "active"));
			DataGroup user = createUserWithIdAndActiveStatus("someUserId", "active");

			addRolesToUser(user);
			records.add(user);
			return records;
		}
		return new ArrayList<DataGroup>();
	}

	private void addRolesToUser(DataGroup user) {
		DataGroup outerUserRole = DataGroup.withNameInData("userRole");
		DataGroup innerUserRole = DataGroup.withNameInData("userRole");
		innerUserRole
				.addChild(DataAtomic.withNameInDataAndValue("linkedRecordType", "permissionRole"));
		innerUserRole.addChild(DataAtomic.withNameInDataAndValue("linkedRecordId", "guest"));
		outerUserRole.addChild(innerUserRole);
		user.addChild(outerUserRole);
	}

	private DataGroup createUserWithIdAndActiveStatus(String userId, String activeStatus) {
		DataGroup inactiveUser = DataGroup.withNameInData("user");
		DataGroup recordInfo = DataGroup.withNameInData("recordInfo");
		recordInfo.addChild(DataAtomic.withNameInDataAndValue("id", userId));
		inactiveUser.addChild(recordInfo);
		inactiveUser.addChild(DataAtomic.withNameInDataAndValue("activeStatus", activeStatus));
		return inactiveUser;
	}

	private DataGroup createChildWithRecordTypeAndRecordId(String recordType, String recordId) {
		DataGroup child1 = DataGroup.withNameInData(recordId);
		child1.addChild(
				DataCreator.createRecordInfoWithRecordTypeAndRecordId(recordType, recordId));
		return child1;
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
		if ("child1_2".equals(type)) {
			return false;
		}
		return true;
	}

	@Override
	public boolean recordExistsForAbstractOrImplementingRecordTypeAndRecordId(String type,
			String id) {
		return false;
	}

}
