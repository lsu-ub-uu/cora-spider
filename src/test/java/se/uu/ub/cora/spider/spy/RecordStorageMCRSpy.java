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
package se.uu.ub.cora.spider.spy;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.function.Supplier;

import se.uu.ub.cora.data.DataGroup;
import se.uu.ub.cora.data.collected.Link;
import se.uu.ub.cora.data.collected.StorageTerm;
import se.uu.ub.cora.storage.RecordStorage;
import se.uu.ub.cora.storage.StorageReadResult;
import se.uu.ub.cora.testspies.data.DataGroupSpy;
import se.uu.ub.cora.testutils.mcr.MethodCallRecorder;
import se.uu.ub.cora.testutils.mrv.MethodReturnValues;

public class RecordStorageMCRSpy implements RecordStorage {

	public DataGroupSpy dataGroupForRead = new DataGroupSpy();
	public List<DataGroup> dataGroupsForReadList = new ArrayList<>();
	public int totalNumberOfRecords = 0;

	public MethodCallRecorder MCR = new MethodCallRecorder();
	public MethodReturnValues MRV = new MethodReturnValues();

	public RecordStorageMCRSpy() {
		MCR.useMRV(MRV);
		MRV.setDefaultReturnValuesSupplier("read",
				((Supplier<DataGroupSpy>) () -> dataGroupForRead));
		MRV.setDefaultReturnValuesSupplier(
				"recordExistsForListOfImplementingRecordTypesAndRecordId",
				(Supplier<Boolean>) () -> false);
		MRV.setDefaultReturnValuesSupplier("generateLinkCollectionPointingToRecord",
				(Supplier<List<DataGroup>>) () -> Collections.emptyList());
	}

	@Override
	public DataGroup read(List<String> types, String id) {
		return (DataGroup) MCR.addCallAndReturnFromMRV("types", types, "id", id);
	}

	@Override
	public void create(String type, String id, DataGroup record, List<StorageTerm> storageTerms,
			List<Link> links, String dataDivider) {
		MCR.addCall("type", type, "id", id, "record", record, "storageTerms", storageTerms,
				"linkList", links, "dataDivider", dataDivider);
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
	public void update(String type, String id, DataGroup record, List<StorageTerm> storageTerms,
			List<Link> links, String dataDivider) {
		MCR.addCall("type", type, "id", id, "record", record, "storageTerms", storageTerms,
				"linkList", links, "dataDivider", dataDivider);

	}

	@Override
	public StorageReadResult readList(List<String> types, DataGroup filter) {
		MCR.addCall("types", types, "filter", filter);
		StorageReadResult result = new StorageReadResult();
		result.listOfDataGroups = dataGroupsForReadList;
		result.totalNumberOfMatches = totalNumberOfRecords;
		return result;
	}

	public void createFakeGroupsInAnswerToList(int numberOfFakeGroups) {
		for (int i = 0; i < numberOfFakeGroups; i++) {
			dataGroupsForReadList.add(new DataGroupSpy());
		}

		// DataGroupSpy spy2 = new DataGroupSpy();
		// DataGroupSpy spy3 = new DataGroupSpy();
		// dataGroups.add(spy2);
		// dataGroups.add(spy3);
	}

	@Override
	public Collection<DataGroup> generateLinkCollectionPointingToRecord(String type, String id) {
		return (Collection<DataGroup>) MCR.addCallAndReturnFromMRV("type", type, "id", id);
	}

	@Override
	public boolean recordExistsForListOfImplementingRecordTypesAndRecordId(List<String> types,
			String id) {
		return (boolean) MCR.addCallAndReturnFromMRV("types", types, "id", id);
	}

	@Override
	public long getTotalNumberOfRecordsForTypes(List<String> types, DataGroup filter) {
		MCR.addCall("type", types, "filter", filter);
		MCR.addReturned(totalNumberOfRecords);
		return totalNumberOfRecords;
	}

}
