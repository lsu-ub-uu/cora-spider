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
import se.uu.ub.cora.data.DataRecordGroup;
import se.uu.ub.cora.data.DataRecordLink;
import se.uu.ub.cora.spider.extended.systemsecret.SystemSecretOperations;
import se.uu.ub.cora.spider.extendedfunctionality.ExtendedFunctionality;
import se.uu.ub.cora.spider.extendedfunctionality.ExtendedFunctionalityData;

public class AppTokenHandlerExtendedFunctionality implements ExtendedFunctionality {

	private static final String APP_TOKENS_GROUP_NAME_IN_DATA = "appTokens";
	private static final String APP_TOKEN_GROUP_NAME_IN_DATA = "appToken";
	private static final String APP_TOKEN_LINK_NAME_IN_DATA = "appTokenLink";
	private AppTokenGenerator appTokenGenerator;
	private SystemSecretOperations systemSecretOperations;
	private DataGroup currentAppTokensGroup;
	private List<String> previousAppTokenLinkIds;

	public AppTokenHandlerExtendedFunctionality(AppTokenGenerator appTokenGenerator,
			SystemSecretOperations systemSecretOperations) {
		this.appTokenGenerator = appTokenGenerator;
		this.systemSecretOperations = systemSecretOperations;
	}

	@Override
	public void useExtendedFunctionality(ExtendedFunctionalityData data) {
		DataRecordGroup previousDataGroup = data.previouslyStoredDataRecordGroup;
		DataRecordGroup currentDataGroup = data.dataRecordGroup;

		String currentDataDivider = readDataDividerFromUserGroup(currentDataGroup);

		if (hasAppTokens(currentDataGroup)) {
			handleApptokens(previousDataGroup, currentDataGroup, currentDataDivider);
		}
	}

	private void handleApptokens(DataRecordGroup previousDataGroup,
			DataRecordGroup currentDataGroup, String currentDataDivider) {
		previousAppTokenLinkIds = getPreviousAppTokenLinkIds(previousDataGroup);
		currentAppTokensGroup = getApptokensGroup(currentDataGroup);
		List<DataGroup> currentAppTokenGroups = getListOfApptokenGroups(currentAppTokensGroup);
		removeAllAppTokensFromAppTokensGroupToKeepOriginalReferenceInParent(currentAppTokensGroup);
		boolean hasAppTokens = createUpdateOrRemoveAppTokens(currentDataDivider,
				currentAppTokenGroups);
		removeAppTokensGroupIfNoAppTokensExists(currentDataGroup, hasAppTokens);
	}

	private boolean createUpdateOrRemoveAppTokens(String currentDataDivider,
			List<DataGroup> currentAppTokenGroups) {
		boolean hasAppTokens = false;
		for (DataGroup appTokenGroup : currentAppTokenGroups) {
			if (!appTokenGroup.containsChildWithNameInData(APP_TOKEN_LINK_NAME_IN_DATA)) {
				addNewAppToken(currentDataDivider, appTokenGroup);
				currentAppTokensGroup.addChild(appTokenGroup);
				hasAppTokens = true;
			} else {
				DataRecordLink dataRecordLink = appTokenGroup.getFirstChildOfTypeAndName(
						DataRecordLink.class, APP_TOKEN_LINK_NAME_IN_DATA);
				if (previousAppTokenLinkIds.contains(dataRecordLink.getLinkedRecordId())) {
					currentAppTokensGroup.addChild(appTokenGroup);
					hasAppTokens = true;
				}
			}
		}
		return hasAppTokens;
	}

	private void removeAllAppTokensFromAppTokensGroupToKeepOriginalReferenceInParent(
			DataGroup currentAppTokensGroup) {
		currentAppTokensGroup.removeAllChildrenWithNameInData(APP_TOKEN_GROUP_NAME_IN_DATA);
	}

	private List<DataGroup> getListOfApptokenGroups(DataGroup currentAppTokensGroup) {
		return currentAppTokensGroup.getChildrenOfTypeAndName(DataGroup.class,
				APP_TOKEN_GROUP_NAME_IN_DATA);
	}

	private DataGroup getApptokensGroup(DataRecordGroup currentDataGroup) {
		return currentDataGroup.getFirstGroupWithNameInData(APP_TOKENS_GROUP_NAME_IN_DATA);
	}

	private void removeAppTokensGroupIfNoAppTokensExists(DataRecordGroup currentDataGroup,
			boolean hasAppTokens) {
		if (!hasAppTokens) {
			currentDataGroup.removeFirstChildWithNameInData(APP_TOKENS_GROUP_NAME_IN_DATA);
		}
	}

	private List<String> getPreviousAppTokenLinkIds(DataRecordGroup previousDataGroup) {
		if (previousDataGroup.containsChildOfTypeAndName(DataGroup.class,
				APP_TOKENS_GROUP_NAME_IN_DATA)) {
			List<DataGroup> previousAppTokenGroups = getPreviousAppTokensList(previousDataGroup);
			return getSystemSecretIdList(previousAppTokenGroups);
		}
		return new ArrayList<>();
	}

	private List<String> getSystemSecretIdList(List<DataGroup> previousAppTokenGroups) {
		List<String> systemSecretIds = new ArrayList<>();
		for (DataGroup previousAppTokenGroup : previousAppTokenGroups) {
			addSystemSecretIdFromRecordLinkToList(systemSecretIds, previousAppTokenGroup);
		}
		return systemSecretIds;
	}

	private void addSystemSecretIdFromRecordLinkToList(List<String> systemSecretIds,
			DataGroup previousAppTokenGroup) {
		DataRecordLink appTokenLink = previousAppTokenGroup
				.getFirstChildOfTypeAndName(DataRecordLink.class, APP_TOKEN_LINK_NAME_IN_DATA);
		systemSecretIds.add(appTokenLink.getLinkedRecordId());
	}

	private List<DataGroup> getPreviousAppTokensList(DataRecordGroup previousDataGroup) {
		DataGroup previousAppTokensGroup = getApptokensGroup(previousDataGroup);
		return getListOfApptokenGroups(previousAppTokensGroup);
	}

	private void addNewAppToken(String currentDataDivider, DataGroup appTokenGroup) {
		String systemSecretId = generateNewAppTokenAndCreateSystemSecretInStorage(
				currentDataDivider);
		DataRecordLink appTokenLink = DataProvider.createRecordLinkUsingNameInDataAndTypeAndId(
				APP_TOKEN_LINK_NAME_IN_DATA, "systemSecret", systemSecretId);
		appTokenGroup.addChild(appTokenLink);
	}

	private boolean hasAppTokens(DataRecordGroup currentDataGroup) {
		return currentDataGroup.containsChildOfTypeAndName(DataGroup.class,
				APP_TOKENS_GROUP_NAME_IN_DATA);
	}

	private String generateNewAppTokenAndCreateSystemSecretInStorage(String currentDataDivider) {
		String generatedAppToken = appTokenGenerator.generateAppToken();
		return systemSecretOperations.createAndStoreSystemSecretRecord(generatedAppToken,
				currentDataDivider);
	}

	private String readDataDividerFromUserGroup(DataRecordGroup currentDataGroup) {
		DataGroup usersRecordInfo = currentDataGroup.getFirstGroupWithNameInData("recordInfo");
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
