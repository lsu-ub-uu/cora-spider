/*
 * Copyright 2024 Uppsala University Library
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
package se.uu.ub.cora.spider.extended.binary;

import se.uu.ub.cora.data.DataGroup;
import se.uu.ub.cora.data.DataRecord;
import se.uu.ub.cora.data.DataRecordGroup;
import se.uu.ub.cora.data.DataResourceLink;
import se.uu.ub.cora.spider.extendedfunctionality.ExtendedFunctionality;
import se.uu.ub.cora.spider.extendedfunctionality.ExtendedFunctionalityData;

public class BinaryProtocolsExtendedFunctionality implements ExtendedFunctionality {

	private static final String JP2 = "jp2";

	@Override
	public void useExtendedFunctionality(ExtendedFunctionalityData data) {
		DataRecord dataRecord = data.dataRecord;
		DataRecordGroup binaryRecordGroup = dataRecord.getDataRecordGroup();
		if (existsJp2ChildWithReadAction(binaryRecordGroup)) {
			dataRecord.addProtocol("iiif");
		}
	}

	private boolean existsJp2ChildWithReadAction(DataRecordGroup binaryRecordGroup) {
		if (binaryRecordGroup.containsChildWithNameInData(JP2)) {
			return jp2ResourceLinkHasReadAction(binaryRecordGroup);
		}
		return false;
	}

	private boolean jp2ResourceLinkHasReadAction(DataRecordGroup binaryRecordGroup) {
		DataResourceLink jp2ResourceLink = getResourceLink(binaryRecordGroup);
		return jp2ResourceLink.hasReadAction();
	}

	private DataResourceLink getResourceLink(DataRecordGroup binaryRecordGroup) {
		DataGroup jp2Group = binaryRecordGroup.getFirstGroupWithNameInData(JP2);
		return jp2Group.getFirstChildOfTypeAndName(DataResourceLink.class, JP2);
	}
}
