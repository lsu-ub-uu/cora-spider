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

import se.uu.ub.cora.bookkeeper.recordpart.RecordPartFilter;
import se.uu.ub.cora.data.DataGroup;
import se.uu.ub.cora.spider.data.DataGroupSpy;

public class RecordPartFilterSpy implements RecordPartFilter {

	public boolean recordPartFilterForReadHasBeenCalled = false;
	public List<String> collectedReadRecordPartPermissions;
	public DataGroup lastRecordFilteredForRead;

	@Override
	public void filterReadRecorPartsUsingPermissions(DataGroup recordRead,
			List<String> collectedReadRecordPartPermissions) {
		lastRecordFilteredForRead = recordRead;
		// recordRead.addChild(new DataAtomicSpy("someExtraStuff", "to"));
		recordRead = new DataGroupSpy("filteredDataGroup");
		this.collectedReadRecordPartPermissions = collectedReadRecordPartPermissions;
		recordPartFilterForReadHasBeenCalled = true;
	}

}
