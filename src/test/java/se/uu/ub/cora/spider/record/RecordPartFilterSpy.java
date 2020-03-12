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

import java.util.List;
import java.util.Map;

import se.uu.ub.cora.bookkeeper.recordpart.RecordPartFilter;
import se.uu.ub.cora.data.DataGroup;
import se.uu.ub.cora.spider.data.DataGroupSpy;

public class RecordPartFilterSpy implements RecordPartFilter {

	public boolean recordPartFilterForReadHasBeenCalled = false;
	public DataGroupSpy returnedDataGroup;
	public DataGroup lastRecordFilteredForRead;
	public Map<String, String> recordPartConstraints;
	public List<String> recordPartReadPermissions;

	@Override
	public DataGroup filterReadRecordPartsUsingPermissions(DataGroup recordRead,
			Map<String, String> recordPartConstraints, List<String> recordPartReadPermissions) {
		this.recordPartConstraints = recordPartConstraints;
		this.recordPartReadPermissions = recordPartReadPermissions;
		lastRecordFilteredForRead = recordRead;
		// recordRead.addChild(new DataAtomicSpy("someExtraStuff", "to"));
		recordRead = new DataGroupSpy("filteredDataGroup");
		recordPartFilterForReadHasBeenCalled = true;
		returnedDataGroup = new DataGroupSpy("someDataGroupSpy");
		return returnedDataGroup;
	}

}
