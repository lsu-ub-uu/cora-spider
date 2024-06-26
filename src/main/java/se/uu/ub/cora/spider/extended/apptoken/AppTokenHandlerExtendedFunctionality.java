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
import se.uu.ub.cora.data.DataProvider;
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

		if (hasAppTokens(currentDataGroup)) {
			List<String> previousAppTokenLinkIds = getPreviousAppTokenLinkIds(previousDataGroup);

			DataGroup currentAppTokensGroup = currentDataGroup
					.getFirstGroupWithNameInData("appTokens");
			List<DataGroup> currentAppTokenGroups = currentAppTokensGroup
					.getChildrenOfTypeAndName(DataGroup.class, "appToken");

			currentAppTokensGroup.removeAllChildrenWithNameInData("appToken");

			int appTokensAdded = 0;
			for (DataGroup appTokenGroup : currentAppTokenGroups) {
				if (!appTokenGroup.containsChildWithNameInData("appTokenLink")) {
					addNewAppToken(currentDataDivider, appTokenGroup);
					currentAppTokensGroup.addChild(appTokenGroup);
					appTokensAdded++;
				} else {
					DataRecordLink dataRecordLink = appTokenGroup
							.getFirstChildOfTypeAndName(DataRecordLink.class, "appTokenLink");
					if (previousAppTokenLinkIds.contains(dataRecordLink.getLinkedRecordId())) {
						currentAppTokensGroup.addChild(appTokenGroup);
						appTokensAdded++;
					}
				}
			}
			removeAppTokensGroupIfNoAppTokensExists(currentDataGroup, appTokensAdded);
		}
	}

	private void removeAppTokensGroupIfNoAppTokensExists(DataGroup currentDataGroup,
			int appTokensAdded) {
		if (appTokensAdded == 0) {
			currentDataGroup.removeFirstChildWithNameInData("appTokens");
		}
	}

	private List<String> getPreviousAppTokenLinkIds(DataGroup previousDataGroup) {
		if (previousDataGroup.containsChildOfTypeAndName(DataGroup.class, "appTokens")) {
			DataGroup previousAppTokensGroup = previousDataGroup
					.getFirstGroupWithNameInData("appTokens");
			List<DataGroup> previousAppTokenGroups = previousAppTokensGroup
					.getChildrenOfTypeAndName(DataGroup.class, "appToken");

			List<String> systemSecretIds = new ArrayList<>();
			for (DataGroup previousAppTokenGroup : previousAppTokenGroups) {
				DataRecordLink appTokenLink = previousAppTokenGroup
						.getFirstChildOfTypeAndName(DataRecordLink.class, "appTokenLink");
				systemSecretIds.add(appTokenLink.getLinkedRecordId());
			}
			return systemSecretIds;
		}
		return new ArrayList<>();
	}

	private void addNewAppToken(String currentDataDivider, DataGroup appTokenGroup) {
		String systemSecretId = generateNewAppTokenAndCreateSystemSecretInStorage(
				currentDataDivider);
		DataRecordLink appTokenLink = DataProvider.createRecordLinkUsingNameInDataAndTypeAndId(
				"appTokenLink", "systemSecret", systemSecretId);
		appTokenGroup.addChild(appTokenLink);
	}

	private boolean hasAppTokens(DataGroup currentDataGroup) {
		return currentDataGroup.containsChildOfTypeAndName(DataGroup.class, "appTokens");
	}

	private String generateNewAppTokenAndCreateSystemSecretInStorage(String currentDataDivider) {
		String generatedAppToken = appTokenGenerator.generateAppToken();
		return systemSecretOperations.createAndStoreSystemSecretRecord(generatedAppToken,
				currentDataDivider);
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
