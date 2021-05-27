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
package se.uu.ub.cora.spider.record;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import se.uu.ub.cora.data.DataGroup;
import se.uu.ub.cora.spider.spy.MethodCallRecorder;
import se.uu.ub.cora.storage.RecordStorage;
import se.uu.ub.cora.storage.StorageReadResult;

public class RecordStorageMCRSpy implements RecordStorage {
	public MethodCallRecorder MCR = new MethodCallRecorder();
	public Map<String, String> atomicValues = new HashMap<>();

	public DataGroupMCRSpy dataGroup = new DataGroupMCRSpy();
	public List<DataGroup> dataGroups = new ArrayList<>();
	public int totalNumberOfRecords = 0;

	@Override
	public DataGroup read(String type, String id) {
		MCR.addCall("type", type, "id", id);
		MCR.addReturned(dataGroup);
		return dataGroup;
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
		MCR.addCall("type", type, "filter", filter);
		StorageReadResult result = new StorageReadResult();
		result.listOfDataGroups = dataGroups;
		result.totalNumberOfMatches = totalNumberOfRecords;
		return result;
	}

	public void createThreeFakeGroupsInAnswerToList() {
		dataGroups.add(new DataGroupMCRSpy("parentId1"));
		dataGroups.add(new DataGroupMCRSpy("parentId2"));
		dataGroups.add(new DataGroupMCRSpy("parentId3"));
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

	@Override
	public long getTotalNumberOfRecords(String type, DataGroup filter) {
		MCR.addCall("type", type, "filter", filter);
		// TODO Auto-generated method stub
		MCR.addReturned(totalNumberOfRecords);
		return totalNumberOfRecords;
	}

	@Override
	public long getTotalNumberOfAbstractRecords(String type, DataGroup filter) {
		// TODO Auto-generated method stub
		return 0;
	}

}
