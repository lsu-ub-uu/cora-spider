/*
 * Copyright 2017, 2022 Uppsala University Library
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
package se.uu.ub.cora.spider.workorder;

import se.uu.ub.cora.data.DataGroup;
import se.uu.ub.cora.data.DataGroupProvider;
import se.uu.ub.cora.spider.extendedfunctionality.ExtendedFunctionality;
import se.uu.ub.cora.spider.extendedfunctionality.ExtendedFunctionalityData;

public class WorkOrderEnhancer implements ExtendedFunctionality {

	@Override
	public void useExtendedFunctionality(ExtendedFunctionalityData data) {
		DataGroup dataGroup = data.dataGroup;
		if (recordInfoIsMissing(dataGroup)) {
			addRecordInfo(dataGroup);
		}
	}

	private boolean recordInfoIsMissing(DataGroup dataGroup) {
		return !dataGroup.containsChildWithNameInData("recordInfo");
	}

	private void addRecordInfo(DataGroup dataGroup) {
		DataGroup recordInfo = DataGroupProvider.getDataGroupUsingNameInData("recordInfo");
		DataGroup dataDivider = createDataDivider();
		recordInfo.addChild(dataDivider);
		dataGroup.addChild(recordInfo);
	}

	private DataGroup createDataDivider() {
		return DataGroupProvider.getDataGroupAsLinkUsingNameInDataTypeAndId("dataDivider", "system",
				"cora");
	}
}
