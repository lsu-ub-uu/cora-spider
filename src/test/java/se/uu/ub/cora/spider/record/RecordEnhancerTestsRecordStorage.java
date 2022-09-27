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

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import se.uu.ub.cora.data.DataGroup;
import se.uu.ub.cora.data.collected.Link;
import se.uu.ub.cora.data.collected.StorageTerm;
import se.uu.ub.cora.spider.data.DataAtomicSpy;
import se.uu.ub.cora.spider.data.DataGroupOldSpy;
import se.uu.ub.cora.spider.testdata.DataCreator;
import se.uu.ub.cora.spider.testdata.DataCreator2;
import se.uu.ub.cora.spider.testdata.RecordLinkTestsDataCreator;
import se.uu.ub.cora.storage.RecordNotFoundException;
import se.uu.ub.cora.storage.RecordStorage;
import se.uu.ub.cora.storage.StorageReadResult;
import se.uu.ub.cora.testutils.mcr.MethodCallRecorder;

public class RecordEnhancerTestsRecordStorage implements RecordStorage {
	MethodCallRecorder MCR = new MethodCallRecorder();

	public boolean recordIdExistsForRecordType = true;
	public boolean createWasRead = false;
	public String publicReadForToRecordType = "false";

	public List<String> readList = new ArrayList<>();
	public Map<String, Integer> readNumberMap = new TreeMap<>();

	private DataGroup datagroup;

	@Override
	public DataGroup read(List<String> types, String id) {

		MCR.addCall("types", types, "id", id);
		for (String type : types) {

			String readKey = type + ":" + id;
			readList.add(readKey);
			if (!readNumberMap.containsKey(readKey)) {
				readNumberMap.put(readKey, 1);
			} else {
				readNumberMap.put(readKey, readNumberMap.get(readKey) + 1);
			}

			if (type.equals("recordType")) {
				if ("binary".equals(id)) {
					datagroup = DataCreator
							.createRecordTypeWithIdAndUserSuppliedIdAndAbstractAndPublicRead(id,
									"false", "true", "false");
				} else if ("image".equals(id)) {
					datagroup = DataCreator.createRecordTypeWithIdAndUserSuppliedIdAndParentId(id,
							"false", "binary");
				} else if ("recordType".equals(id)) {
					DataGroup dataGroup = DataCreator
							.createRecordTypeWithIdAndUserSuppliedIdAndAbstractAndPublicRead(id,
									"false", "true", "false");
					DataGroup search = new DataGroupOldSpy("search");
					search.addChild(new DataAtomicSpy("linkedRecordType", "search"));
					search.addChild(new DataAtomicSpy("linkedRecordId", "someDefaultSearch"));
					// .asLinkWithNameInDataAndTypeAndId("search", "search",
					// "someDefaultSearch");
					dataGroup.addChild(search);
					datagroup = dataGroup;
				} else if (("place".equals(id))) {
					datagroup = DataCreator.createRecordTypeWithIdAndUserSuppliedIdAndParentId(id,
							"false", "authority");
				} else if (("toRecordType".equals(id))) {
					datagroup = DataCreator
							.createRecordTypeWithIdAndUserSuppliedIdAndAbstractAndPublicRead(id,
									"false", "true", publicReadForToRecordType);
				} else {
					datagroup = DataCreator
							.createRecordTypeWithIdAndUserSuppliedIdAndAbstractAndPublicRead(
									"dataWithLinks", "false", "false", "false");
				}
			} else if (type.equals("dataWithLinks")) {
				if (id.equals("oneLinkTopLevel")) {
					datagroup = RecordLinkTestsDataCreator.createDataGroupWithRecordInfoAndLink();
				} else if (id.equals("twoLinksTopLevel")) {
					datagroup = RecordLinkTestsDataCreator
							.createDataGroupWithRecordInfoAndTwoLinks();
				} else if (id.equals("oneLinkTopLevelNotAuthorized")) {
					datagroup = RecordLinkTestsDataCreator
							.createDataGroupWithRecordInfoAndLinkNotAuthorized();
				} else if (id.equals("oneLinkOneLevelDown")) {
					datagroup = RecordLinkTestsDataCreator
							.createDataDataGroupWithRecordInfoAndLinkOneLevelDown();
				} else if (id.equals("oneLinkOneLevelDownTargetDoesNotExist")) {
					datagroup = RecordLinkTestsDataCreator
							.createDataDataGroupWithRecordInfoAndLinkOneLevelDownTargetDoesNotExist();
				}
			} else if (type.equals("dataWithResourceLinks")) {
				if (id.equals("oneResourceLinkTopLevel")) {
					datagroup = RecordLinkTestsDataCreator
							.createDataGroupWithRecordInfoAndResourceLink();
				} else if (id.equals("oneResourceLinkOneLevelDown")) {
					datagroup = RecordLinkTestsDataCreator
							.createDataDataGroupWithRecordInfoAndResourceLinkOneLevelDown();
				}
			} else if (type.equals("toRecordType")) {
				if (id.equals("recordLinkNotAuthorized")) {
					datagroup = RecordLinkTestsDataCreator.createLinkChildAsDataRecordDataGroup();
				} else if ("nonExistingRecordId".equals(id)) {
					throw new RecordNotFoundException("no record with id " + id + " exists");
				}

			} else if (type.equals("search")) {
				if ("aSearchId".equals(id)) {
					datagroup = DataCreator2.createSearchWithIdAndRecordTypeToSearchIn("aSearchId",
							"place");
				} else if ("anotherSearchId".equals(id)) {
					datagroup = DataCreator2
							.createSearchWithIdAndRecordTypeToSearchIn("anotherSearchId", "image");
				} else if ("someDefaultSearch".equals(id)) {
					datagroup = DataCreator2.createSearchWithIdAndRecordTypeToSearchIn(
							"someDefaultSearch", "someRecordType");
				}
			} else if (type.equals("system")) {
				if (id.equals("cora")) {
					datagroup = DataCreator.createDataGroupWithNameInDataTypeAndId("system", type,
							id);
				}
			} else if ("image".equals(type)) {
				datagroup = DataCreator.createDataGroupWithNameInDataTypeAndId("binary", "image",
						"image:0001");
			} else if ("place".equals(type)) {
				datagroup = DataCreator.createDataGroupWithNameInDataTypeAndId("authority", "place",
						id);
			} else {
				// return null;
				datagroup = DataCreator.createDataGroupWithNameInDataTypeAndId("noData", type, id);
			}
		}
		MCR.addReturned(datagroup);
		return datagroup;
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
	public void update(String type, String id, DataGroup record, List<StorageTerm> collectedTerms,
			List<Link> links, String dataDivider) {
		// TODO Auto-generated method stub

	}

	@Override
	public StorageReadResult readList(List<String> types, DataGroup filter) {
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
		return recordIdExistsForRecordType;
	}

	@Override
	public long getTotalNumberOfRecordsForTypes(List<String> types, DataGroup filter) {
		// TODO Auto-generated method stub
		return 0;
	}

}
