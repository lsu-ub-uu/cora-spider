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
import java.util.Collections;
import java.util.HashMap;
import java.util.List;

import se.uu.ub.cora.data.DataGroup;
import se.uu.ub.cora.data.DataProvider;
import se.uu.ub.cora.data.DataRecordLink;
import se.uu.ub.cora.spider.extendedfunctionality.ExtendedFunctionality;
import se.uu.ub.cora.spider.extendedfunctionality.ExtendedFunctionalityData;
import se.uu.ub.cora.spider.systemsecret.SystemSecretOperations;

public class AppTokenHandlerExtendedFunctionality implements ExtendedFunctionality {

	private static final String APP_TOKENS_GROUP_NAME_IN_DATA = "appTokens";
	private static final String APP_TOKEN_GROUP_NAME_IN_DATA = "appToken";
	private static final String APP_TOKEN_LINK_NAME_IN_DATA = "appTokenLink";
	private AppTokenGenerator appTokenGenerator;
	private SystemSecretOperations systemSecretOperations;
	private DataGroup currentAppTokensGroup;
	private List<String> previousAppTokenLinkIds;
	private List<DataGroup> currentAppTokenGroups;
	private String currentDataDivider;
	private HashMap<String, String> efSystemSecretIdAndClearTextToken;

	public AppTokenHandlerExtendedFunctionality(AppTokenGenerator appTokenGenerator,
			SystemSecretOperations systemSecretOperations) {
		this.appTokenGenerator = appTokenGenerator;
		this.systemSecretOperations = systemSecretOperations;
	}

	@Override
	public void useExtendedFunctionality(ExtendedFunctionalityData data) {
		setUpDataSharer(data);
		DataGroup previousDataGroup = data.previouslyStoredTopDataGroup;
		DataGroup currentDataGroup = data.dataGroup;
		currentDataDivider = readDataDividerFromUserGroup(currentDataGroup);
		possiblySetAppTokensGroup(currentDataGroup);

		currentAppTokenGroups = getListOfCurrentAppTokens(currentDataGroup);
		previousAppTokenLinkIds = getPreviousAppTokenLinkIds(previousDataGroup);
		possiblyRemoveAllAppTokensBeforeProcessing(currentDataGroup);

		handleApptokens();
		removeAppTokensGroupIfNoAppTokensExists(currentDataGroup);
		removePreviousAppTokensNotInCurrent();
	}

	private void possiblyRemoveAllAppTokensBeforeProcessing(DataGroup currentDataGroup) {
		if (hasAppTokens(currentDataGroup)) {
			removeAllAppTokensFromAppTokensGroupToKeepOriginalReferenceInParent(
					currentAppTokensGroup);
		}
	}

	private void setUpDataSharer(ExtendedFunctionalityData data) {
		efSystemSecretIdAndClearTextToken = new HashMap<>();
		data.dataSharer.put(this.getClass().getSimpleName(), efSystemSecretIdAndClearTextToken);
	}

	private String readDataDividerFromUserGroup(DataGroup currentDataGroup) {
		DataGroup usersRecordInfo = currentDataGroup.getFirstGroupWithNameInData("recordInfo");
		DataRecordLink userDataDivider = (DataRecordLink) usersRecordInfo
				.getFirstChildWithNameInData("dataDivider");
		return userDataDivider.getLinkedRecordId();
	}

	private void possiblySetAppTokensGroup(DataGroup currentDataGroup) {
		if (hasAppTokens(currentDataGroup)) {
			currentAppTokensGroup = currentDataGroup
					.getFirstGroupWithNameInData(APP_TOKENS_GROUP_NAME_IN_DATA);
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

	private List<DataGroup> getPreviousAppTokensList(DataGroup previousDataGroup) {
		DataGroup previousAppTokensGroup = previousDataGroup
				.getFirstGroupWithNameInData(APP_TOKENS_GROUP_NAME_IN_DATA);
		return previousAppTokensGroup.getChildrenOfTypeAndName(DataGroup.class,
				APP_TOKEN_GROUP_NAME_IN_DATA);
	}

	private List<DataGroup> getListOfCurrentAppTokens(DataGroup currentDataGroup) {
		if (hasAppTokens(currentDataGroup)) {
			return currentAppTokensGroup.getChildrenOfTypeAndName(DataGroup.class,
					APP_TOKEN_GROUP_NAME_IN_DATA);
		}
		return Collections.emptyList();
	}

	private boolean hasAppTokens(DataGroup currentDataGroup) {
		return currentDataGroup.containsChildOfTypeAndName(DataGroup.class,
				APP_TOKENS_GROUP_NAME_IN_DATA);
	}

	private void handleApptokens() {
		for (DataGroup appTokenGroup : currentAppTokenGroups) {
			possiblyCreateNewAppToken(appTokenGroup);
			possiblyKeepExistingAppToken(appTokenGroup);
		}
	}

	private void possiblyCreateNewAppToken(DataGroup appTokenGroup) {
		if (appTokenIsNew(appTokenGroup)) {
			String systemSecretId = createSystemSecretAndKeepClearTextToken();
			creatRecordLinkAndAddToAppTokensGroup(appTokenGroup, systemSecretId);
		}
	}

	private boolean appTokenIsNew(DataGroup appTokenGroup) {
		return !appTokenGroup.containsChildWithNameInData(APP_TOKEN_LINK_NAME_IN_DATA);
	}

	private String createSystemSecretAndKeepClearTextToken() {
		String generatedAppToken = appTokenGenerator.generateAppToken();
		String systemSecretId = systemSecretOperations
				.createAndStoreSystemSecretRecord(generatedAppToken, currentDataDivider);
		efSystemSecretIdAndClearTextToken.put(systemSecretId, generatedAppToken);
		return systemSecretId;
	}

	private void creatRecordLinkAndAddToAppTokensGroup(DataGroup appTokenGroup,
			String systemSecretId) {
		DataRecordLink appTokenLink = DataProvider.createRecordLinkUsingNameInDataAndTypeAndId(
				APP_TOKEN_LINK_NAME_IN_DATA, "systemSecret", systemSecretId);
		appTokenGroup.addChild(appTokenLink);
		currentAppTokensGroup.addChild(appTokenGroup);
	}

	private void possiblyKeepExistingAppToken(DataGroup appTokenGroup) {
		if (appTokenIsExisting(appTokenGroup)) {
			DataRecordLink dataRecordLink = appTokenGroup
					.getFirstChildOfTypeAndName(DataRecordLink.class, APP_TOKEN_LINK_NAME_IN_DATA);
			currentAppTokensGroup.addChild(appTokenGroup);
			previousAppTokenLinkIds.remove(dataRecordLink.getLinkedRecordId());
		}
	}

	private boolean appTokenIsExisting(DataGroup appTokenGroup) {
		if (appTokenGroup.containsChildWithNameInData(APP_TOKEN_LINK_NAME_IN_DATA)) {
			DataRecordLink dataRecordLink = appTokenGroup
					.getFirstChildOfTypeAndName(DataRecordLink.class, APP_TOKEN_LINK_NAME_IN_DATA);
			return appTokenExistsAndCanBeUpdated(dataRecordLink);
		}
		return false;
	}

	private boolean appTokenExistsAndCanBeUpdated(DataRecordLink dataRecordLink) {
		return previousAppTokenLinkIds.contains(dataRecordLink.getLinkedRecordId());
	}

	private void removeAllAppTokensFromAppTokensGroupToKeepOriginalReferenceInParent(
			DataGroup currentAppTokensGroup) {
		currentAppTokensGroup.removeAllChildrenWithNameInData(APP_TOKEN_GROUP_NAME_IN_DATA);
	}

	private void removeAppTokensGroupIfNoAppTokensExists(DataGroup currentDataGroup) {
		if (hasAppTokens(currentDataGroup) && !currentAppTokensGroup
				.containsChildWithNameInData(APP_TOKEN_GROUP_NAME_IN_DATA)) {
			currentDataGroup.removeFirstChildWithNameInData(APP_TOKENS_GROUP_NAME_IN_DATA);
		}
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

	private void removePreviousAppTokensNotInCurrent() {
		for (String id : previousAppTokenLinkIds) {
			systemSecretOperations.deleteSystemSecretFromStorage(id);
		}
	}
}
