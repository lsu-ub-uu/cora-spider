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

package se.uu.ub.cora.spider.record.internal;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import se.uu.ub.cora.data.DataGroup;
import se.uu.ub.cora.spider.data.DataAtomicSpy;
import se.uu.ub.cora.spider.data.DataGroupSpy;
import se.uu.ub.cora.spider.spy.MethodCallRecorder;
import se.uu.ub.cora.spider.testdata.DataCreator;
import se.uu.ub.cora.storage.RecordStorage;
import se.uu.ub.cora.storage.StorageReadResult;

public class RecordStorageSpy implements RecordStorage {
	public MethodCallRecorder MCR = new MethodCallRecorder();

	public long start = 0;
	public long totalNumberOfMatches = 0;
	public List<DataGroup> listOfDataGroups = Collections.emptyList();
	public String abstractString = "false";
	public Set<String> incomingLinksExistsForType = new HashSet<>();
	public DataGroup returnForRead = null;
	public int endNumberToReturn = 0;
	public List<List<DataGroup>> listOfListOfDataGroups = new ArrayList<>();

	@Override
	public DataGroup read(String type, String id) {
		MCR.addCall("type", type, "id", id);
		DataGroup dataGroup;
		if (null != returnForRead) {
			dataGroup = returnForRead;
		} else {
			dataGroup = new DataGroupSpy("recordType");

			DataGroup recordInfo = DataCreator
					.createRecordInfoWithRecordTypeAndRecordId("recordType", "metadata");
			dataGroup.addChild(recordInfo);
			dataGroup.addChild(new DataAtomicSpy("abstract", abstractString));
		}
		MCR.addReturned(dataGroup);
		return dataGroup;
	}

	@Override
	public void create(String type, String id, DataGroup record, DataGroup collectedTerms,
			DataGroup linkList, String dataDivider) {
		MCR.addCall("type", type, "id", id, "record", record, "collectedTerms", collectedTerms,
				"linkList", linkList, "dataDivider", dataDivider);
	}

	@Override
	public void deleteByTypeAndId(String type, String id) {
		MCR.addCall("type", type, "id", id);
	}

	@Override
	public boolean linksExistForRecord(String type, String id) {
		MCR.addCall("type", type, "id", id);
		boolean answer = false;
		if (incomingLinksExistsForType.contains(type)) {
			answer = true;
		}
		MCR.addReturned(answer);
		return answer;
	}

	@Override
	public void update(String type, String id, DataGroup record, DataGroup collectedTerms,
			DataGroup linkList, String dataDivider) {
		MCR.addCall("type", type, "id", id, "record", record, "collectedTerms", collectedTerms,
				"linkList", linkList, "dataDivider", dataDivider);
	}

	@Override
	public StorageReadResult readList(String type, DataGroup filter) {
		MCR.addCall("type", type, "filter", filter);
		StorageReadResult createSpiderReadResult = createSpiderReadResult();
		MCR.addReturned(createSpiderReadResult);
		listOfListOfDataGroups.add(createSpiderReadResult.listOfDataGroups);
		return createSpiderReadResult;
	}

	private StorageReadResult createSpiderReadResult() {
		StorageReadResult readResult = new StorageReadResult();
		readResult.start = start;
		readResult.totalNumberOfMatches = totalNumberOfMatches;
		if (endNumberToReturn > 0) {
			listOfDataGroups = new ArrayList<>();
			addRecordsToList();
		}
		readResult.listOfDataGroups = listOfDataGroups;
		return readResult;
	}

	private void addRecordsToList() {
		int i = (int) start;
		while (i < endNumberToReturn) {
			DataGroupSpy topDataGroup = new DataGroupSpy("dummy");
			DataGroupSpy recordInfo = new DataGroupSpy("recordInfo");
			topDataGroup.addChild(recordInfo);
			DataGroupSpy type = new DataGroupSpy("type");
			recordInfo.addChild(type);
			type.addChild(new DataAtomicSpy("linkedRecordId", "dummyRecordType"));
			recordInfo.addChild(new DataAtomicSpy("id", "someId" + i));
			listOfDataGroups.add(topDataGroup);
			i++;
		}
	}

	@Override
	public StorageReadResult readAbstractList(String type, DataGroup filter) {
		MCR.addCall("type", type, "filter", filter);
		StorageReadResult createSpiderReadResult = createSpiderReadResult();
		MCR.addReturned(createSpiderReadResult);
		return createSpiderReadResult;
	}

	@Override
	public DataGroup readLinkList(String type, String id) {
		MCR.addCall("type", type, "id", id);
		MCR.addReturned(null);
		return null;
	}

	@Override
	public Collection<DataGroup> generateLinkCollectionPointingToRecord(String type, String id) {
		MCR.addCall("type", type, "id", id);
		MCR.addReturned(null);
		return null;
	}

	@Override
	public boolean recordExistsForAbstractOrImplementingRecordTypeAndRecordId(String type,
			String id) {
		MCR.addCall("type", type, "id", id);
		MCR.addReturned(false);
		return false;
	}

	@Override
	public long getTotalNumberOfRecords(String type, DataGroup filter) {
		MCR.addCall("type", type, "filter", filter);
		MCR.addReturned(0);
		return 0;
	}

	@Override
	public long getTotalNumberOfAbstractRecords(String abstractType, List<String> implementingTypes,
			DataGroup filter) {
		MCR.addCall("abstractType", abstractType, "implementingTypes", implementingTypes, "filter",
				filter);
		MCR.addReturned(0);
		return 0;
	}

}
