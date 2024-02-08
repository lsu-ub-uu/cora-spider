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
import se.uu.ub.cora.data.DataResourceLink;
import se.uu.ub.cora.spider.extendedfunctionality.ExtendedFunctionality;
import se.uu.ub.cora.spider.extendedfunctionality.ExtendedFunctionalityData;

public class BinaryProtocolsExtendedFunctionality implements ExtendedFunctionality {

	private static final String JP2 = "jp2";

	@Override
	public void useExtendedFunctionality(ExtendedFunctionalityData data) {
		DataRecord dataRecord = data.dataRecord;
		DataGroup binaryGroup = dataRecord.getDataGroup();
		if (existsJp2ChildWithReadAction(binaryGroup)) {
			dataRecord.addProtocol("iiif");
		}
	}

	private boolean existsJp2ChildWithReadAction(DataGroup binaryGroup) {
		if (binaryGroup.containsChildWithNameInData(JP2)) {
			return jp2ResourceLinkHasReadAction(binaryGroup);
		}
		return false;
	}

	private boolean jp2ResourceLinkHasReadAction(DataGroup binaryGroup) {
		DataResourceLink jp2ResourceLink = getResourceLink(binaryGroup);
		return jp2ResourceLink.hasReadAction();
	}

	private DataResourceLink getResourceLink(DataGroup binaryGroup) {
		DataGroup jp2Group = binaryGroup.getFirstGroupWithNameInData(JP2);
		return jp2Group.getFirstChildOfTypeAndName(DataResourceLink.class, JP2);
	}
}
