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
		DataGroup previousDataGroup = data.previouslyStoredTopDataGroup;
		DataGroup currentDataGroup = data.dataGroup;

		String currentDataDivider = readDataDividerFromUserGroup(currentDataGroup);

		previousAppTokenLinkIds = getPreviousAppTokenLinkIds(previousDataGroup);
		if (hasAppTokens(currentDataGroup)) {
			handleApptokens(currentDataGroup, currentDataDivider);
		}
		removePreviousAppTokensNotInCurrent();
	}

	private void removePreviousAppTokensNotInCurrent() {
		for (String id : previousAppTokenLinkIds) {
			systemSecretOperations.deleteSystemSecretFromStorage(id);
		}
	}

	private boolean hasAppTokens(DataGroup currentDataGroup) {
		return currentDataGroup.containsChildOfTypeAndName(DataGroup.class,
				APP_TOKENS_GROUP_NAME_IN_DATA);
	}

	private void handleApptokens(DataGroup currentDataGroup, String currentDataDivider) {
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
			if (appTokenIsNew(appTokenGroup)) {
				addNewAppTokenToAppTokensGroup(currentDataDivider, appTokenGroup);
				hasAppTokens = true;
			} else {
				DataRecordLink dataRecordLink = appTokenGroup.getFirstChildOfTypeAndName(
						DataRecordLink.class, APP_TOKEN_LINK_NAME_IN_DATA);
				if (appTokenExistsAndCanBeUpdated(dataRecordLink)) {
					currentAppTokensGroup.addChild(appTokenGroup);
					hasAppTokens = true;
					previousAppTokenLinkIds.remove(dataRecordLink.getLinkedRecordId());
				}
			}
		}
		// currentAppTokensGroup.containsChildWithNameInData("appToken");
		return hasAppTokens;
	}

	private boolean appTokenExistsAndCanBeUpdated(DataRecordLink dataRecordLink) {
		return previousAppTokenLinkIds.contains(dataRecordLink.getLinkedRecordId());
	}

	private void addNewAppTokenToAppTokensGroup(String currentDataDivider,
			DataGroup appTokenGroup) {
		addNewAppToken(currentDataDivider, appTokenGroup);
		currentAppTokensGroup.addChild(appTokenGroup);
	}

	private boolean appTokenIsNew(DataGroup appTokenGroup) {
		return !appTokenGroup.containsChildWithNameInData(APP_TOKEN_LINK_NAME_IN_DATA);
	}

	private void removeAllAppTokensFromAppTokensGroupToKeepOriginalReferenceInParent(
			DataGroup currentAppTokensGroup) {
		currentAppTokensGroup.removeAllChildrenWithNameInData(APP_TOKEN_GROUP_NAME_IN_DATA);
	}

	private List<DataGroup> getListOfApptokenGroups(DataGroup currentAppTokensGroup) {
		return currentAppTokensGroup.getChildrenOfTypeAndName(DataGroup.class,
				APP_TOKEN_GROUP_NAME_IN_DATA);
	}

	private DataGroup getApptokensGroup(DataGroup previousDataGroup) {
		return previousDataGroup.getFirstGroupWithNameInData(APP_TOKENS_GROUP_NAME_IN_DATA);
	}

	private void removeAppTokensGroupIfNoAppTokensExists(DataGroup currentDataGroup,
			boolean hasAppTokens) {
		if (!hasAppTokens) {
			currentDataGroup.removeFirstChildWithNameInData(APP_TOKENS_GROUP_NAME_IN_DATA);
		}
	}

	private List<String> getPreviousAppTokenLinkIds(DataGroup previousDataGroup) {
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

	private List<DataGroup> getPreviousAppTokensList(DataGroup previousDataGroup) {
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

	private String generateNewAppTokenAndCreateSystemSecretInStorage(String currentDataDivider) {
		String generatedAppToken = appTokenGenerator.generateAppToken();
		return systemSecretOperations.createAndStoreSystemSecretRecord(generatedAppToken,
				currentDataDivider);
	}

	private String readDataDividerFromUserGroup(DataGroup currentDataGroup) {
		DataGroup usersRecordInfo = currentDataGroup.getFirstGroupWithNameInData("recordInfo");
		DataRecordLink userDataDivider = (DataRecordLink) usersRecordInfo
				.getFirstChildWithNameInData("dataDivider");
		return userDataDivider.getLinkedRecordId();
	}
}
