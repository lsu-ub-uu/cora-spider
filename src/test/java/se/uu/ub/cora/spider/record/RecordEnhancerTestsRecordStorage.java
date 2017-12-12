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

package se.uu.ub.cora.spider.record;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import se.uu.ub.cora.bookkeeper.data.DataAtomic;
import se.uu.ub.cora.bookkeeper.data.DataGroup;
import se.uu.ub.cora.spider.record.storage.RecordNotFoundException;
import se.uu.ub.cora.spider.record.storage.RecordStorage;
import se.uu.ub.cora.spider.testdata.DataCreator;
import se.uu.ub.cora.spider.testdata.RecordLinkTestsDataCreator;
import se.uu.ub.cora.spider.testdata.SpiderDataCreator;

public class RecordEnhancerTestsRecordStorage implements RecordStorage {

	public boolean recordIdExistsForRecordType = true;
	public boolean createWasRead = false;

	@Override
	public DataGroup read(String type, String id) {
		if (type.equals("recordType")) {
			if ("binary".equals(id)) {
				return DataCreator.createRecordTypeWithIdAndUserSuppliedIdAndAbstract(id, "false",
						"true");
			} else if ("image".equals(id)) {
				return DataCreator.createRecordTypeWithIdAndUserSuppliedIdAndParentId(id, "false",
						"binary");
			} else if (("place".equals(id))) {
				DataGroup dataGroup = DataCreator.createDataGroupWithNameInDataTypeAndId("authority",
						"recordType", id);
				dataGroup.addChild(DataAtomic.withNameInDataAndValue("abstract", "false"));
				DataGroup parent = DataGroup.withNameInData("parentId");
				parent.addChild(DataAtomic.withNameInDataAndValue("linkedRecordType", "recordType"));
				parent.addChild(DataAtomic.withNameInDataAndValue("linkedRecordId", "authority"));
				dataGroup.addChild(parent);
				return dataGroup;
			}
			return DataCreator.createRecordTypeWithIdAndUserSuppliedIdAndAbstract("dataWithLinks",
					"false", "false");
		}
		if (type.equals("dataWithLinks")) {
			if (id.equals("oneLinkTopLevel")) {
				return RecordLinkTestsDataCreator.createSpiderDataGroupWithRecordInfoAndLink()
						.toDataGroup();
			}
			if (id.equals("oneLinkTopLevelNotAuthorized")) {
				return RecordLinkTestsDataCreator
						.createSpiderDataGroupWithRecordInfoAndLinkNotAuthorized().toDataGroup();
			}
			if (id.equals("oneLinkOneLevelDown")) {
				return RecordLinkTestsDataCreator.createDataGroupWithRecordInfoAndLinkOneLevelDown()
						.toDataGroup();
			}
			if (id.equals("oneLinkOneLevelDownTargetDoesNotExist")) {
				return RecordLinkTestsDataCreator
						.createDataGroupWithRecordInfoAndLinkOneLevelDownTargetDoesNotExist()
						.toDataGroup();
			}
		}
		if (type.equals("dataWithResourceLinks")) {
			if (id.equals("oneResourceLinkTopLevel")) {
				return RecordLinkTestsDataCreator.createSpiderDataGroupWithRecordInfoAndResourceLink()
						.toDataGroup();
			}
			if (id.equals("oneResourceLinkOneLevelDown")) {
				return RecordLinkTestsDataCreator
						.createDataGroupWithRecordInfoAndResourceLinkOneLevelDown().toDataGroup();
			}
		}
		if (type.equals("toRecordType")) {
			if (id.equals("recordLinkNotAuthorized")) {
				return RecordLinkTestsDataCreator.createLinkChildAsRecordDataGroup().toDataGroup();
			}
		}
		if (type.equals("search")) {
			if ("aSearchId".equals(id)) {
				return SpiderDataCreator.createSearchWithIdAndRecordTypeToSearchIn("aSearchId", "place")
						.toDataGroup();
			} else if ("anotherSearchId".equals(id)) {
				return SpiderDataCreator
						.createSearchWithIdAndRecordTypeToSearchIn("anotherSearchId", "image")
						.toDataGroup();
			}
		}
		if (type.equals("system")) {
			if (id.equals("cora")) {
				return DataCreator.createDataGroupWithNameInDataTypeAndId("system", type, id);
			}
		}
		if ("image".equals(type)) {
			return DataCreator.createDataGroupWithNameInDataTypeAndId("binary", "image", "image:0001");
		}
		if ("place".equals(type)) {
			return DataCreator.createDataGroupWithNameInDataTypeAndId("authority", "place", id);
		}
		if ("nonExistingRecordId".equals(id)) {
			throw new RecordNotFoundException("no record with id " + id + " exists");
		}
		return null;
	}

	@Override
	public void create(String type, String id, DataGroup record, DataGroup collectedTerms,
			DataGroup linkList, String dataDivider) {
		createWasRead = true;

	}

	@Override
	public void deleteByTypeAndId(String type, String id) {
		// TODO Auto-generated method stub

	}

	@Override
	public boolean linksExistForRecord(String type, String id) {
		if ("place".equals(type)) {
			if (id.equals("place:0001")) {
				return true;
			}
		} else if ("authority".equals(type)) {
			if ("place:0003".equals(id)) {
				return true;
			}
		}
		return false;
	}

	@Override
	public void update(String type, String id, DataGroup record, DataGroup collectedTerms,
			DataGroup linkList, String dataDivider) {
		// TODO Auto-generated method stub

	}

	@Override
	public Collection<DataGroup> readList(String type, DataGroup filter) {
		List<DataGroup> list = new ArrayList<>();
		list.add(read(type, "oneLinkTopLevel"));
		list.add(read(type, "oneLinkOneLevelDown"));
		return list;
	}

	@Override
	public Collection<DataGroup> readAbstractList(String type, DataGroup filter) {
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
	public boolean recordExistsForAbstractOrImplementingRecordTypeAndRecordId(String type, String id) {
		return recordIdExistsForRecordType;
	}

}
