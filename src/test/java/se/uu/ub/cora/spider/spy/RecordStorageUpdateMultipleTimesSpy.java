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

import se.uu.ub.cora.data.DataGroup;
import se.uu.ub.cora.data.DataRecordLink;
import se.uu.ub.cora.data.collected.Link;
import se.uu.ub.cora.data.collected.StorageTerm;
import se.uu.ub.cora.spider.data.DataAtomicSpy;
import se.uu.ub.cora.spider.data.DataGroupOldSpy;
import se.uu.ub.cora.spider.testdata.DataCreator;
import se.uu.ub.cora.spider.testspies.DataRecordLinkSpy;
import se.uu.ub.cora.storage.Filter;
import se.uu.ub.cora.storage.RecordNotFoundException;
import se.uu.ub.cora.storage.RecordStorage;
import se.uu.ub.cora.storage.StorageReadResult;

public class RecordStorageUpdateMultipleTimesSpy implements RecordStorage {

	public Collection<List<String>> readLists = new ArrayList<>();
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
	public boolean alreadyCalled = false;
	public Collection<List<String>> types = new ArrayList<>();

	@Override
	public DataGroup read(List<String> types, String id) {
		// this.type = type;
		this.types.add(types);
		this.id = id;
		readWasCalled = true;
		if ("spyType".equals(id)) {
			return DataCreator.createRecordTypeWithIdAndUserSuppliedIdAndAbstractAndPublicRead(id,
					"false", "false", "false");
		}
		if (recordToReturnOnRead != null) {
			return recordToReturnOnRead;
		}
		DataGroup dataGroupToReturn = new DataGroupOldSpy("someNameInData");
		DataGroupOldSpy recordInfo = new DataGroupOldSpy("recordInfo");
		DataGroup createdBy = new DataGroupOldSpy("createdBy");
		createdBy.addChild(new DataAtomicSpy("linkedRecordType", "user"));
		createdBy.addChild(new DataAtomicSpy("linkedRecordId", "4422"));

		recordInfo.addChild(createdBy);
		recordInfo.addChild(new DataAtomicSpy("tsCreated", "2014-08-01T00:00:00.000000Z"));
		dataGroupToReturn.addChild(recordInfo);
		if (alreadyCalled) {
			DataGroup updated = new DataGroupOldSpy("updated");
			updated.setRepeatId("0");
			DataRecordLink updatedBy = new DataRecordLinkSpy("updatedBy");
			updated.addChild(updatedBy);
			updated.addChild(new DataAtomicSpy("tsUpdated", "2014-12-18 20:20:38.346"));

			recordInfo.addChild(updated);

		}
		return dataGroupToReturn;
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
	public void update(String type, String id, DataGroup dataRecord, List<StorageTerm> storageTerms,
			List<Link> links, String dataDivider) {
		updateWasCalled = true;

	}

	@Override
	public StorageReadResult readList(List<String> types, Filter filter) {
		StorageReadResult spiderReadResult = new StorageReadResult();
		spiderReadResult.listOfDataGroups = new ArrayList<>();
		readListWasCalled = true;
		readLists.add(types);
		filters.add(filter);

		for (String type : types) {

			if ("recordType".equals(type)) {
				ArrayList<DataGroup> recordTypes = new ArrayList<>();
				recordTypes.add(read(List.of("recordType"), "abstract"));
				recordTypes.add(read(List.of("recordType"), "child1"));
				recordTypes.add(read(List.of("recordType"), "child2"));
				recordTypes.add(read(List.of("recordType"), "abstract2"));
				recordTypes.add(read(List.of("recordType"), "child1_2"));
				recordTypes.add(read(List.of("recordType"), "child2_2"));
				recordTypes.add(read(List.of("recordType"), "otherType"));
				spiderReadResult.listOfDataGroups = recordTypes;
				return spiderReadResult;
			}
			if ("child1_2".equals(type)) {
				throw new RecordNotFoundException("No records exists with recordType: " + type);
			}
		}
		return spiderReadResult;
	}

	@Override
	public Collection<Link> getLinksToRecord(String type, String id) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public boolean recordExists(List<String> types,
			String id) {
		return false;
	}

	@Override
	public long getTotalNumberOfRecordsForTypes(List<String> types, DataGroup filter) {
		// TODO Auto-generated method stub
		return 0;
	}
}
