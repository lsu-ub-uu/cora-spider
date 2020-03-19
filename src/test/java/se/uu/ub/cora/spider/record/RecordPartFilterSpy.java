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

import java.util.Set;

import se.uu.ub.cora.bookkeeper.recordpart.RecordPartFilter;
import se.uu.ub.cora.data.DataGroup;
import se.uu.ub.cora.spider.data.DataAtomicSpy;
import se.uu.ub.cora.spider.data.DataGroupSpy;

public class RecordPartFilterSpy implements RecordPartFilter {

	public boolean recordPartFilterForReadHasBeenCalled = false;
	public boolean replaceRecordPartsUsingPermissionsHasBeenCalled = false;
	public DataGroupSpy returnedDataGroup;
	public DataGroup lastRecordFilteredForRead;
	public Set<String> replaceRecordPartConstraints;
	public Set<String> recordPartReadPermissions;
	public DataGroup originalDataGroup;
	public DataGroup changedDataGroup;
	public Set<String> replaceRecordPartPermissions;

	@Override
	public DataGroup removeChildrenForConstraintsWithoutPermissions(DataGroup recordRead,
			Set<String> recordPartConstraints, Set<String> recordPartReadPermissions) {
		this.replaceRecordPartConstraints = recordPartConstraints;
		this.recordPartReadPermissions = recordPartReadPermissions;
		lastRecordFilteredForRead = recordRead;
		// recordRead.addChild(new DataAtomicSpy("someExtraStuff", "to"));
		// recordRead = new DataGroupSpy("filteredDataGroup");
		recordPartFilterForReadHasBeenCalled = true;
		returnedDataGroup = new DataGroupSpy("someDataGroupSpy");
		return returnedDataGroup;
	}

	@Override
	public DataGroup replaceChildrenForConstraintsWithoutPermissions(DataGroup originalDataGroup,
			DataGroup changedDataGroup, Set<String> recordPartConstraints,
			Set<String> recordPartPermissions) {
		this.originalDataGroup = originalDataGroup;
		this.changedDataGroup = changedDataGroup;
		this.replaceRecordPartConstraints = recordPartConstraints;
		this.replaceRecordPartPermissions = recordPartPermissions;
		// recordRead.addChild(new DataAtomicSpy("someExtraStuff", "to"));
		// recordRead = new DataGroupSpy("filteredDataGroup");
		replaceRecordPartsUsingPermissionsHasBeenCalled = true;
		returnedDataGroup = new DataGroupSpy("someDataGroupSpy");
		DataGroupSpy recordInfo = createRecordInfo();
		returnedDataGroup.addChild(recordInfo);

		return returnedDataGroup;
	}

	private DataGroupSpy createRecordInfo() {
		DataGroupSpy recordInfo = new DataGroupSpy("recordInfo");
		recordInfo.addChild(new DataAtomicSpy("id", "spyId"));
		DataGroupSpy type = new DataGroupSpy("type");
		type.addChild(new DataAtomicSpy("linkedRecordId", "spyType"));
		recordInfo.addChild(type);
		DataGroupSpy dataDivider = new DataGroupSpy("dataDivider");
		dataDivider.addChild(new DataAtomicSpy("linkedRecordId", "someSystem"));
		recordInfo.addChild(dataDivider);
		return recordInfo;
	}

}
