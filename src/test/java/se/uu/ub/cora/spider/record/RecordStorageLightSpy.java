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

import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import se.uu.ub.cora.data.DataGroup;
import se.uu.ub.cora.spider.record.internal.DataGroupCheckCallsSpy;
import se.uu.ub.cora.storage.RecordStorage;
import se.uu.ub.cora.storage.StorageReadResult;

public class RecordStorageLightSpy implements RecordStorage {

	public DataGroupCheckCallsSpy dataGroupReturnedFromSpy;
	public Map<String, List<String>> childrenToContainInDataGroup = new HashMap<>();

	@Override
	public DataGroup read(String type, String id) {
		if (dataGroupNotAlreadySetFromOutside()) {
			dataGroupReturnedFromSpy = new DataGroupCheckCallsSpy();
		}
		possiblyAddChildrenToNotContain(id);
		return dataGroupReturnedFromSpy;

	}

	private boolean dataGroupNotAlreadySetFromOutside() {
		return dataGroupReturnedFromSpy == null;
	}

	private void possiblyAddChildrenToNotContain(String id) {
		if (childrenToContainInDataGroup.containsKey(id)) {
			dataGroupReturnedFromSpy.nameInDatasToContain
					.addAll(childrenToContainInDataGroup.get(id));
		}
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
		// TODO Auto-generated method stub
		return null;
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
		// TODO Auto-generated method stub
		return 0;
	}

}
