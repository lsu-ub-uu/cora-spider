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
package se.uu.ub.cora.spider.extended.visibility;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

import se.uu.ub.cora.data.DataAtomic;
import se.uu.ub.cora.data.DataGroup;
import se.uu.ub.cora.data.DataProvider;
import se.uu.ub.cora.spider.extendedfunctionality.ExtendedFunctionality;
import se.uu.ub.cora.spider.extendedfunctionality.ExtendedFunctionalityData;

public class VisibilityExtendedFunctionality implements ExtendedFunctionality {

	@Override
	public void useExtendedFunctionality(ExtendedFunctionalityData data) {
		DataGroup updatedDataGroup = data.dataGroup;
		DataGroup storedDataGroup = data.previouslyStoredTopDataGroup;

		if (hasAdminInfo(updatedDataGroup)) {
			DataGroup updatedAdminInfo = updatedDataGroup.getFirstGroupWithNameInData("adminInfo");
			updatedAdminInfo.getFirstAtomicValueWithNameInData("visibility");

			// TODO
			storedDataGroup.containsChildWithNameInData("adminInfo");
			DataGroup storedAdminInfo = storedDataGroup.getFirstGroupWithNameInData("adminInfo");

			storedAdminInfo.getFirstAtomicValueWithNameInData("visibility");

			DataAtomic tsVisibility = DataProvider
					.createAtomicUsingNameInDataAndValue("tsVisibility", getCurrentTimestamp());

			updatedAdminInfo.addChild(tsVisibility);
		}
	}

	private boolean hasAdminInfo(DataGroup dataGroup) {
		return dataGroup.containsChildWithNameInData("adminInfo");
	}

	private String getCurrentTimestamp() {
		String format = "yyyy-MM-dd'T'HH:mm:ss.SSSSSS'Z'";
		DateTimeFormatter timeFormater = DateTimeFormatter.ofPattern(format);
		LocalDateTime currentTime = LocalDateTime.now();
		return timeFormater.format(currentTime);
	}

}
