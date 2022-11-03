/*
 * Copyright 2015, 2019 Uppsala University Library
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

package se.uu.ub.cora.spider.record;

import java.util.Collection;
import java.util.List;

import se.uu.ub.cora.data.DataGroup;
import se.uu.ub.cora.data.collected.Link;
import se.uu.ub.cora.data.collected.StorageTerm;
import se.uu.ub.cora.spider.data.DataAtomicSpy;
import se.uu.ub.cora.spider.data.DataGroupOldSpy;
import se.uu.ub.cora.spider.testdata.DataCreator;
import se.uu.ub.cora.spider.testdata.RecordLinkTestsDataCreator;
import se.uu.ub.cora.storage.RecordStorage;
import se.uu.ub.cora.storage.StorageReadResult;

public class RecordLinkTestsRecordStorage implements RecordStorage {

	public boolean recordIdExistsForRecordType = true;
	public boolean createWasRead = false;
	public List<String> types;
	public String id;

	@Override
	public DataGroup read(List<String> types, String id) {
		if (types.equals("recordType")) {
			if ("validationOrder".equals(id)) {
				return DataCreator.createRecordTypeWithIdAndUserSuppliedIdAndAbstractAndPublicRead(
						id, "false", "false", "false");
			}
			return DataCreator.createRecordTypeWithIdAndUserSuppliedIdAndAbstractAndPublicRead(
					"dataWithLinks", "false", "false", "false");
		}
		if (types.equals("dataWithLinks")) {
			if (id.equals("oneLinkTopLevel")) {
				return RecordLinkTestsDataCreator.createDataGroupWithRecordInfoAndLink();
			}
			if (id.equals("oneLinkOneLevelDown")) {
				DataGroup recordLinkGroup = RecordLinkTestsDataCreator
						.createDataDataGroupWithRecordInfoAndLinkOneLevelDown();
				addCreatedInfoToRecordInfo(recordLinkGroup);
				return recordLinkGroup;
			}
		}
		if (types.equals("toRecordType")) {
			if (id.equals("recordLinkNotAuthorized")) {
				return RecordLinkTestsDataCreator.createLinkChildAsDataRecordDataGroup();
			}
		}
		return null;
	}

	private void addCreatedInfoToRecordInfo(DataGroup readDataGroup) {
		DataGroup recordInfo = readDataGroup.getFirstGroupWithNameInData("recordInfo");
		DataGroup createdBy = new DataGroupOldSpy("createdBy");
		createdBy.addChild(new DataAtomicSpy("linkedRecordType", "user"));
		createdBy.addChild(new DataAtomicSpy("linkedRecordId", "6789"));
		recordInfo.addChild(createdBy);
		recordInfo.addChild(new DataAtomicSpy("tsCreated", "2016-10-01T00:00:00.000000Z"));
		readDataGroup.addChild(recordInfo);
	}

	@Override
	public void create(String type, String id, DataGroup record, List<StorageTerm> storageTerms,
			List<Link> links, String dataDivider) {
		createWasRead = true;

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
	public StorageReadResult readList(List<String> types, DataGroup filter) {
		return null;
	}

	@Override
	public Collection<Link> getLinksToRecord(String type, String id) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public boolean recordExists(List<String> types,
			String id) {
		this.types = types;
		this.id = id;
		return recordIdExistsForRecordType;
	}

	@Override
	public long getTotalNumberOfRecordsForTypes(List<String> types, DataGroup filter) {
		// TODO Auto-generated method stub
		return 0;
	}
}
