/*
 * Copyright 2025 Uppsala University Library
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
package se.uu.ub.cora.spider.extended.urn;

import se.uu.ub.cora.data.DataRecordGroup;
import se.uu.ub.cora.spider.extendedfunctionality.ExtendedFunctionality;
import se.uu.ub.cora.spider.extendedfunctionality.ExtendedFunctionalityData;

public class UrnExtendedFunctionality implements ExtendedFunctionality {

	private static final String RECORD_INFO = "recordInfo";
	private static final String URN = "urn";

	@Override
	public void useExtendedFunctionality(ExtendedFunctionalityData data) {
		DataRecordGroup recordGroup = data.dataRecordGroup;
		// if (hasRecordInfo(recordGroup)) {
		// possiblyAddUrn(recordGroup);
		// }
	}

	// private void possiblyAddUrn(DataRecordGroup recordGroup) {
	// DataGroup recordInfo = recordGroup.getFirstGroupWithNameInData(RECORD_INFO);
	// if (hasRecordInfo(recordGroup)) {
	// addUrnIfNotPresent(recordInfo);
	// }
	// }
	//
	// private boolean hasRecordInfo(DataRecordGroup recordGroup) {
	// return recordGroup != null && recordGroup.containsChildWithNameInData(RECORD_INFO);
	// }
	//
	// private void addUrnIfNotPresent(DataGroup recordInfo) {
	// if (!recordInfoHasUrn(recordInfo)) {
	// createAndAddUrn(recordInfo);
	// }
	// }
	//
	// private boolean recordInfoHasUrn(DataGroup recordInfo) {
	// return recordInfo.containsChildWithNameInData(URN);
	// }
	//
	// private void createAndAddUrn(DataGroup recordInfo) {
	// String recordId = "someIdToAdd";
	// DataAtomic urnNumber = DataProvider.createAtomicUsingNameInDataAndValue(URN,
	// "urn:nbn:se:alvin:portal:----" + recordId);
	// recordInfo.addChild(urnNumber);
	// }
}
