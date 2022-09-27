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
import java.util.function.Supplier;

import se.uu.ub.cora.data.DataGroup;
import se.uu.ub.cora.data.collected.Link;
import se.uu.ub.cora.data.collected.StorageTerm;
import se.uu.ub.cora.spider.data.DataAtomicSpy;
import se.uu.ub.cora.spider.data.DataGroupOldSpy;
import se.uu.ub.cora.spider.testdata.DataCreator;
import se.uu.ub.cora.storage.RecordStorage;
import se.uu.ub.cora.storage.StorageReadResult;
import se.uu.ub.cora.testutils.mcr.MethodCallRecorder;
import se.uu.ub.cora.testutils.mrv.MethodReturnValues;

public class RecordStorageOldSpy implements RecordStorage {
	public MethodCallRecorder MCR = new MethodCallRecorder();
	public MethodReturnValues MRV = new MethodReturnValues();

	public long start = 0;
	public long totalNumberOfMatches = 0;
	public List<DataGroup> listOfDataGroups = Collections.emptyList();
	public String abstractString = "false";
	public Set<String> incomingLinksExistsForType = new HashSet<>();
	public DataGroup returnForRead = null;
	public int numberToReturnForReadList = 0;
	public List<List<DataGroup>> listOfListOfDataGroups = new ArrayList<>();

	public RecordStorageOldSpy() {
		MCR.useMRV(MRV);
		MRV.setDefaultReturnValuesSupplier("readList",
				(Supplier<StorageReadResult>) () -> createSpiderReadResult());
	}

	@Override
	public DataGroup read(List<String> types, String id) {
		MCR.addCall("type", types, "id", id);
		DataGroup dataGroup;
		if (null != returnForRead) {
			dataGroup = returnForRead;
		} else {
			dataGroup = new DataGroupOldSpy("recordType");

			DataGroup recordInfo = DataCreator
					.createRecordInfoWithRecordTypeAndRecordId("recordType", "metadata");
			dataGroup.addChild(recordInfo);
			dataGroup.addChild(new DataAtomicSpy("abstract", abstractString));
		}
		MCR.addReturned(dataGroup);
		return dataGroup;
	}

	@Override
	public void create(String type, String id, DataGroup record, List<StorageTerm> storageTerms,
			List<Link> links, String dataDivider) {
		MCR.addCall("type", type, "id", id, "record", record, "collectedTerms", storageTerms,
				"linkList", links, "dataDivider", dataDivider);
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
	public void update(String type, String id, DataGroup record, List<StorageTerm> storageTerms,
			List<Link> links, String dataDivider) {
		MCR.addCall("type", type, "id", id, "record", record, "storageTerms", storageTerms,
				"linkList", links, "dataDivider", dataDivider);
	}

	@Override
	public StorageReadResult readList(List<String> types, DataGroup filter) {
		return (StorageReadResult) MCR.addCallAndReturnFromMRV("types", types, "filter", filter);
	}

	private StorageReadResult createSpiderReadResult() {
		StorageReadResult readResult = new StorageReadResult();
		readResult.start = start;
		readResult.totalNumberOfMatches = totalNumberOfMatches;
		if (numberToReturnForReadList > 0) {
			listOfDataGroups = new ArrayList<>();
			addRecordsToList();
		}
		readResult.listOfDataGroups = listOfDataGroups;
		return readResult;
	}

	private void addRecordsToList() {
		int i = (int) start;
		while (i < numberToReturnForReadList) {
			DataGroup topDataGroup = new DataGroupOldSpy("dummy");
			DataGroup recordInfo = new DataGroupOldSpy("recordInfo");
			topDataGroup.addChild(recordInfo);
			DataGroup type = new DataGroupOldSpy("type");
			recordInfo.addChild(type);
			type.addChild(new DataAtomicSpy("linkedRecordId", "dummyRecordType"));
			recordInfo.addChild(new DataAtomicSpy("id", "someId" + i));
			listOfDataGroups.add(topDataGroup);
			i++;
		}
	}

	// @Override
	// public StorageReadResult readAbstractList(String type, DataGroup filter) {
	// MCR.addCall("type", type, "filter", filter);
	// StorageReadResult createSpiderReadResult = createSpiderReadResult();
	// MCR.addReturned(createSpiderReadResult);
	// return createSpiderReadResult;
	// }

	@Override
	public Collection<DataGroup> generateLinkCollectionPointingToRecord(String type, String id) {
		MCR.addCall("type", type, "id", id);
		MCR.addReturned(null);
		return null;
	}

	@Override
	public boolean recordExistsForListOfImplementingRecordTypesAndRecordId(List<String> types,
			String id) {
		MCR.addCall("type", types, "id", id);
		MCR.addReturned(false);
		return false;
	}

	@Override
	public long getTotalNumberOfRecordsForTypes(List<String> types, DataGroup filter) {
		MCR.addCall("type", types, "filter", filter);
		MCR.addReturned(0);
		return 0;
	}

}
