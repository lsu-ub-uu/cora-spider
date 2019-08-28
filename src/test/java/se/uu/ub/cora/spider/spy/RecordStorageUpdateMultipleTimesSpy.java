/*
 * Copyright 2018, 2019 Uppsala University Library
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

import se.uu.ub.cora.data.DataAtomic;
import se.uu.ub.cora.data.DataGroup;
import se.uu.ub.cora.spider.record.storage.RecordNotFoundException;
import se.uu.ub.cora.spider.testdata.DataCreator;
import se.uu.ub.cora.storage.RecordStorage;
import se.uu.ub.cora.storage.SpiderReadResult;

public class RecordStorageUpdateMultipleTimesSpy implements RecordStorage {

	public Collection<String> readLists = new ArrayList<>();
	public boolean readWasCalled = false;
	public boolean deleteWasCalled = false;
	public boolean createWasCalled = false;
	public boolean updateWasCalled = false;
	public boolean linksExist = false;
	public DataGroup createRecord;
	public String type;
	public String id;
	public List<DataGroup> filters = new ArrayList<>();
	public boolean readListWasCalled = false;
	public DataGroup recordToReturnOnRead = null;

	@Override
	public DataGroup read(String type, String id) {
		this.type = type;
		this.id = id;
		readWasCalled = true;
		if ("spyType".equals(id)) {
			return DataCreator.createRecordTypeWithIdAndUserSuppliedIdAndAbstractAndPublicRead(id, "false",
					"false", "false");
		}
		if (recordToReturnOnRead != null) {
			return recordToReturnOnRead;
		}
		DataGroup dataGroupToReturn = DataGroup.withNameInData("someNameInData");
		dataGroupToReturn.addChild(DataGroup.withNameInData("recordInfo"));
		return dataGroupToReturn;
	}

	@Override
	public void create(String type, String id, DataGroup record, DataGroup collectedTerms,
			DataGroup linkList, String dataDivider) {
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
	public void update(String type, String id, DataGroup record, DataGroup collectedTerms,
			DataGroup linkList, String dataDivider) {
		updateWasCalled = true;
	}

	@Override
	public SpiderReadResult readList(String type, DataGroup filter) {
		SpiderReadResult spiderReadResult = new SpiderReadResult();
		spiderReadResult.listOfDataGroups = new ArrayList<>();
		readListWasCalled = true;
		readLists.add(type);
		filters.add(filter);
		if ("recordType".equals(type)) {
			ArrayList<DataGroup> recordTypes = new ArrayList<>();
			recordTypes.add(read("recordType", "abstract"));
			recordTypes.add(read("recordType", "child1"));
			recordTypes.add(read("recordType", "child2"));
			recordTypes.add(read("recordType", "abstract2"));
			recordTypes.add(read("recordType", "child1_2"));
			recordTypes.add(read("recordType", "child2_2"));
			recordTypes.add(read("recordType", "otherType"));
			spiderReadResult.listOfDataGroups = recordTypes;
			return spiderReadResult;
		}
		if ("child1_2".equals(type)) {
			throw new RecordNotFoundException("No records exists with recordType: " + type);
		}
		return spiderReadResult;
	}

	@Override
	public SpiderReadResult readAbstractList(String type, DataGroup filter) {
		SpiderReadResult spiderReadResult = new SpiderReadResult();
		spiderReadResult.listOfDataGroups = new ArrayList<>();
		readLists.add(type);
		if ("abstract".equals(type)) {
			ArrayList<DataGroup> records = new ArrayList<>();
			records.add(createChildWithRecordTypeAndRecordId("implementing1", "child1_2"));

			records.add(createChildWithRecordTypeAndRecordId("implementing2", "child2_2"));
			spiderReadResult.listOfDataGroups = records;
			return spiderReadResult;
		}
		if ("abstract2".equals(type)) {
			ArrayList<DataGroup> records = new ArrayList<>();

			records.add(createChildWithRecordTypeAndRecordId("implementing2", "child2_2"));
			spiderReadResult.listOfDataGroups = records;
			return spiderReadResult;
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
			spiderReadResult.listOfDataGroups = records;
			return spiderReadResult;
		}
		return spiderReadResult;
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
