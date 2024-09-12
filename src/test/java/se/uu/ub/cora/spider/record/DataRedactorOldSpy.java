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

import se.uu.ub.cora.bookkeeper.metadata.Constraint;
import se.uu.ub.cora.bookkeeper.recordpart.DataRedactor;
import se.uu.ub.cora.data.DataGroup;
import se.uu.ub.cora.data.DataRecordGroup;
import se.uu.ub.cora.spider.data.DataGroupOldSpy;
import se.uu.ub.cora.spider.testdata.DataCreator2;
import se.uu.ub.cora.testutils.mcr.MethodCallRecorder;

public class DataRedactorOldSpy implements DataRedactor {
	public MethodCallRecorder MCR = new MethodCallRecorder();
	/**
	 * returnEnteredDataGroupAsAnswer is default false, if set to true is the dataGroup entered in
	 * calls to methods retured as answer instead of a new DataGroupSpy
	 */
	public boolean returnEnteredDataGroupAsAnswer = false;
	/**
	 * returnDataGroup is initially null, if set to a DataGroup is that dataGroup returned as
	 * answer.
	 */
	public DataGroup returnDataGroup;

	@Override
	public DataRecordGroup removeChildrenForConstraintsWithoutPermissions(String metadataId,
			DataRecordGroup originalDataGroup, Set<Constraint> recordPartConstraints,
			Set<String> recordPartReadPermissions) {
		MCR.addCall("metadataId", metadataId, "recordRead", originalDataGroup,
				"recordPartConstraints", recordPartConstraints, "recordPartReadPermissions",
				recordPartReadPermissions);
		DataGroupOldSpy returnedRemovedDataGroup = new DataGroupOldSpy("someDataGroupSpy");
		if (returnEnteredDataGroupAsAnswer) {
			MCR.addReturned(originalDataGroup);
			return originalDataGroup;
		}
		if (null != returnDataGroup) {
			MCR.addReturned(returnDataGroup);
			return returnDataGroup;
		}
		MCR.addReturned(returnedRemovedDataGroup);
		return returnedRemovedDataGroup;
	}

	@Override
	public DataRecordGroup replaceChildrenForConstraintsWithoutPermissions(String metadataId,
			DataRecordGroup originalDataRecordGroup, DataRecordGroup changedDataRecordGroup,
			Set<Constraint> recordPartConstraints, Set<String> recordPartPermissions) {
		MCR.addCall("metadataId", metadataId, "originalDataGroup", originalDataRecordGroup,
				"changedDataGroup", changedDataRecordGroup, "recordPartConstraints",
				recordPartConstraints, "recordPartPermissions", recordPartPermissions);
		DataGroupOldSpy returnedReplacedDataGroup = new DataGroupOldSpy("someDataGroupSpy");
		DataGroupOldSpy recordInfo = createRecordInfo();
		returnedReplacedDataGroup.addChild(recordInfo);
		if (returnEnteredDataGroupAsAnswer) {
			MCR.addReturned(originalDataRecordGroup);
			return originalDataRecordGroup;
		}
		if (null != returnDataGroup) {
			MCR.addReturned(returnDataGroup);
			return returnDataGroup;
		}
		MCR.addReturned(returnedReplacedDataGroup);
		return returnedReplacedDataGroup;
	}

	private DataGroupOldSpy createRecordInfo() {
		// DataGroupOldSpy recordInfo = new DataGroupOldSpy("recordInfo");
		// recordInfo.addChild(new DataAtomicSpy("id", "spyId"));
		// DataGroupOldSpy type = new DataGroupOldSpy("type");
		// type.addChild(new DataAtomicSpy("linkedRecordId", "spyType"));
		// recordInfo.addChild(type);
		// DataGroupOldSpy dataDivider = new DataGroupOldSpy("dataDivider");
		// dataDivider.addChild(new DataAtomicSpy("linkedRecordId", "someSystem"));
		// recordInfo.addChild(dataDivider);
		// return recordInfo;
		return (DataGroupOldSpy) DataCreator2
				.createRecordInfoWithRecordTypeAndRecordIdAndDataDivider("spyType", "spyId",
						"someSystem");
	}

}
