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
import se.uu.ub.cora.spider.extended.systemsecret.SystemSecretOperations;
import se.uu.ub.cora.spider.extendedfunctionality.ExtendedFunctionality;
import se.uu.ub.cora.spider.extendedfunctionality.ExtendedFunctionalityData;

public class AppTokenHandlerExtendedFunctionality implements ExtendedFunctionality {

	private AppTokenGenerator appTokenGenerator;
	private SystemSecretOperations systemSecretOperations;

	public AppTokenHandlerExtendedFunctionality(AppTokenGenerator appTokenGenerator,
			SystemSecretOperations systemSecretOperations) {
		this.appTokenGenerator = appTokenGenerator;
		this.systemSecretOperations = systemSecretOperations;
	}

	@Override
	public void useExtendedFunctionality(ExtendedFunctionalityData data) {
		DataGroup previousDataGroup = data.previouslyStoredTopDataGroup;
		DataGroup currentDataGroup = data.dataGroup;
		String currentDataDivider = readDataDividerFromUserGroup(currentDataGroup);

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
			createNewAppTokenUsingSystemSecret(currentDataDivider);

			// addLinkToSystemSecret(systemSecretId);
			// setTimestampWhenPasswordHasBeenStored();

		}
	}

	private void createNewAppTokenUsingSystemSecret(String currentDataDivider) {
		String generatedAppToken = appTokenGenerator.generateAppToken();
		String systemSecretId = systemSecretOperations
				.createAndStoreSystemSecretRecord(generatedAppToken, currentDataDivider);
	}

	private String readDataDividerFromUserGroup(DataGroup currentUserGroup) {
		DataGroup usersRecordInfo = currentUserGroup.getFirstGroupWithNameInData("recordInfo");
		DataRecordLink userDataDivider = (DataRecordLink) usersRecordInfo
				.getFirstChildWithNameInData("dataDivider");
		return userDataDivider.getLinkedRecordId();
	}

	// private void addLinkToSystemSecret(String systemSecretRecordId) {
	// var secretLink = createLinkPointingToSecretRecord(systemSecretRecordId);
	// currentUserGroup.addChild(secretLink);
	// }
	//
	// private DataRecordLink createLinkPointingToSecretRecord(String systemSecretRecordId) {
	// return DataProvider.createRecordLinkUsingNameInDataAndTypeAndId(PASSWORD_LINK_NAME_IN_DATA,
	// SYSTEM_SECRET_TYPE, systemSecretRecordId);
	// }
	//
	// private void setTimestampWhenPasswordHasBeenStored() {
	// DataAtomic tsPasswordUpdated = createAtomicLatestTsUpdatedFromRecord();
	// currentUserGroup.addChild(tsPasswordUpdated);
	// }

}
