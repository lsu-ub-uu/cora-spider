/*
 * Copyright 2017, 2022, 2023 Uppsala University Library
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
package se.uu.ub.cora.spider.extended.workorder;

import se.uu.ub.cora.data.DataRecordGroup;
import se.uu.ub.cora.spider.extendedfunctionality.ExtendedFunctionality;
import se.uu.ub.cora.spider.extendedfunctionality.ExtendedFunctionalityData;

public class WorkOrderEnhancer implements ExtendedFunctionality {

	@Override
	public void useExtendedFunctionality(ExtendedFunctionalityData data) {
		DataRecordGroup dataRecordGroup = data.dataRecordGroup;
		if (recordInfoIsMissing(dataRecordGroup)) {
			addRecordInfo(dataRecordGroup);
		}
	}

	private boolean recordInfoIsMissing(DataRecordGroup dataRecordGroup) {
		return !dataRecordGroup.containsChildWithNameInData("recordInfo");
	}

	private void addRecordInfo(DataRecordGroup dataRecordGroup) {
		dataRecordGroup.setDataDivider("cora");
		dataRecordGroup.setValidationType("workOrder");
	}
}
