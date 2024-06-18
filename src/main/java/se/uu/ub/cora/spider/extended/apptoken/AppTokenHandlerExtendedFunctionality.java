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
package se.uu.ub.cora.spider.extended.apptoken;

import java.util.ArrayList;
import java.util.List;

import se.uu.ub.cora.data.DataGroup;
import se.uu.ub.cora.data.DataRecordLink;
import se.uu.ub.cora.spider.extendedfunctionality.ExtendedFunctionality;
import se.uu.ub.cora.spider.extendedfunctionality.ExtendedFunctionalityData;

public class AppTokenHandlerExtendedFunctionality implements ExtendedFunctionality {

	@Override
	public void useExtendedFunctionality(ExtendedFunctionalityData data) {
		DataGroup previousDataGroup = data.previouslyStoredTopDataGroup;
		DataGroup currentDataGroup = data.dataGroup;

		previousDataGroup.containsChildOfTypeAndName(DataGroup.class, "appToken");
		// previousDataGroup.getFirstGroupWithNameInData("appToken");
		List<String> allPreviousAppTokenLinksId = new ArrayList<String>();

		currentDataGroup.containsChildOfTypeAndName(DataGroup.class, "appToken");
		List<DataGroup> allCurrentAppTokens = currentDataGroup
				.getAllGroupsWithNameInData("appToken");

		for (DataGroup aCurrentAppToken : allCurrentAppTokens) {
			DataRecordLink anAppTokenLink = aCurrentAppToken
					.getFirstChildOfTypeAndName(DataRecordLink.class, "appTokenLink");
			anAppTokenLink.getLinkedRecordId();
			// if (!allPreviousAppTokenLinksId.contains(anAppTokenLink.getLinkedRecordId())){;
			// createSystemSecret
			// }
		}
	}

}
