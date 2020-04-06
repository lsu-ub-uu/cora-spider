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

import se.uu.ub.cora.bookkeeper.recordpart.DataRedactor;
import se.uu.ub.cora.data.DataGroup;
import se.uu.ub.cora.spider.data.DataAtomicSpy;
import se.uu.ub.cora.spider.data.DataGroupSpy;
import se.uu.ub.cora.spider.spy.MethodCallRecorder;

public class DataRedactorSpy implements DataRedactor {
	public MethodCallRecorder MCR = new MethodCallRecorder();

	@Override
	public DataGroup removeChildrenForConstraintsWithoutPermissions(DataGroup recordRead,
			Set<String> recordPartConstraints, Set<String> recordPartReadPermissions) {
		MCR.addCall("recordRead", recordRead, "recordPartConstraints", recordPartConstraints,
				"recordPartReadPermissions", recordPartReadPermissions);
		DataGroupSpy returnedRemovedDataGroup = new DataGroupSpy("someDataGroupSpy");
		MCR.addReturned(returnedRemovedDataGroup);
		return returnedRemovedDataGroup;
	}

	@Override
	public DataGroup replaceChildrenForConstraintsWithoutPermissions(DataGroup originalDataGroup,
			DataGroup changedDataGroup, Set<String> recordPartConstraints,
			Set<String> recordPartPermissions) {
		MCR.addCall("originalDataGroup", originalDataGroup, "changedDataGroup", changedDataGroup,
				"recordPartConstraints", recordPartConstraints, "recordPartPermissions",
				recordPartPermissions);
		DataGroupSpy returnedReplacedDataGroup = new DataGroupSpy("someDataGroupSpy");
		DataGroupSpy recordInfo = createRecordInfo();
		returnedReplacedDataGroup.addChild(recordInfo);

		MCR.addReturned(returnedReplacedDataGroup);
		return returnedReplacedDataGroup;
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
