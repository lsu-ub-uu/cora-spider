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
import se.uu.ub.cora.data.DataRecordGroup;
import se.uu.ub.cora.data.DataRecordLink;
import se.uu.ub.cora.spider.extendedfunctionality.ExtendedFunctionality;
import se.uu.ub.cora.spider.extendedfunctionality.ExtendedFunctionalityData;
import se.uu.ub.cora.spider.systemsecret.SystemSecretOperations;

/**
 * AppTokenHandlerExtendedFunctionality creates, updates, and removes appTokens for the user beeing
 * updated. It also set the field with nameInData "appTokenClearText", in dataSharer for later
 * addition by {@link AppTokenClearTextExtendedFuncionality}.
 * <p>
 * PasswordExtendedFunctionality is NOT threadsafe.
 */
public class AppTokenHandlerExtendedFunctionality implements ExtendedFunctionality {
	private static final String APP_TOKENS_GROUP_NAME_IN_DATA = "appTokens";
	private static final String APP_TOKEN_GROUP_NAME_IN_DATA = "appToken";
	private static final String APP_TOKEN_LINK_NAME_IN_DATA = "appTokenLink";

	public static AppTokenHandlerExtendedFunctionality usingAppTokenGeneratorAndSystemSecretOperations(
			AppTokenGenerator appTokenGenerator, SystemSecretOperations systemSecretOperations) {
		return new AppTokenHandlerExtendedFunctionality(appTokenGenerator, systemSecretOperations);
	}

	private AppTokenGenerator appTokenGenerator;
	private SystemSecretOperations systemSecretOperations;
	private DataGroup currentAppTokensGroup;
	private List<String> previousAppTokenLinkIds;
	private List<DataGroup> currentAppTokenGroups;
	private String currentDataDivider;
	private HashMap<String, String> efSystemSecretIdAndClearTextToken;

	private AppTokenHandlerExtendedFunctionality(AppTokenGenerator appTokenGenerator,
			SystemSecretOperations systemSecretOperations) {
		this.appTokenGenerator = appTokenGenerator;
		this.systemSecretOperations = systemSecretOperations;
	}

	@Override
	public void useExtendedFunctionality(ExtendedFunctionalityData data) {
		setUpDataSharer(data);
		DataRecordGroup previousDataRecordGroup = data.previouslyStoredDataRecordGroup;
		DataRecordGroup currentDataRecordGroup = data.dataRecordGroup;
		currentDataDivider = currentDataRecordGroup.getDataDivider();

		possiblySetAppTokensGroup(currentDataRecordGroup);

		currentAppTokenGroups = getListOfCurrentAppTokens(currentDataRecordGroup);
		previousAppTokenLinkIds = getPreviousAppTokenLinkIds(previousDataRecordGroup);
		possiblyRemoveAllAppTokensBeforeProcessing(currentDataRecordGroup);

		handleApptokens();
		removeAppTokensGroupIfNoAppTokensExists(currentDataRecordGroup);
		removePreviousAppTokensNotInCurrent();
	}

	private void possiblyRemoveAllAppTokensBeforeProcessing(DataRecordGroup currentDataGroup) {
		if (hasAppTokens(currentDataGroup)) {
			removeAllAppTokensFromAppTokensGroupToKeepOriginalReferenceInParent(
					currentAppTokensGroup);
		}
	}

	private void setUpDataSharer(ExtendedFunctionalityData data) {
		efSystemSecretIdAndClearTextToken = new HashMap<>();
		data.dataSharer.put(this.getClass().getSimpleName(), efSystemSecretIdAndClearTextToken);
	}

	private void possiblySetAppTokensGroup(DataRecordGroup currentDataGroup) {
		if (hasAppTokens(currentDataGroup)) {
			currentAppTokensGroup = currentDataGroup
					.getFirstGroupWithNameInData(APP_TOKENS_GROUP_NAME_IN_DATA);
		}
	}

	private List<String> getPreviousAppTokenLinkIds(DataRecordGroup previousDataGroup) {
		if (previousDataGroup.containsChildOfTypeAndName(DataGroup.class,
				APP_TOKENS_GROUP_NAME_IN_DATA)) {
			List<DataGroup> previousAppTokenGroups = getPreviousAppTokensList(previousDataGroup);
			return getSystemSecretIdList(previousAppTokenGroups);
		}
		return Collections.emptyList();
	}

	private List<DataGroup> getPreviousAppTokensList(DataRecordGroup previousDataGroup) {
		DataGroup previousAppTokensGroup = previousDataGroup
				.getFirstGroupWithNameInData(APP_TOKENS_GROUP_NAME_IN_DATA);
		return previousAppTokensGroup.getChildrenOfTypeAndName(DataGroup.class,
				APP_TOKEN_GROUP_NAME_IN_DATA);
	}

	private List<DataGroup> getListOfCurrentAppTokens(DataRecordGroup currentDataGroup) {
		if (hasAppTokens(currentDataGroup)) {
			return currentAppTokensGroup.getChildrenOfTypeAndName(DataGroup.class,
					APP_TOKEN_GROUP_NAME_IN_DATA);
		}
		return Collections.emptyList();
	}

	private boolean hasAppTokens(DataRecordGroup currentDataGroup) {
		return currentDataGroup.containsChildOfTypeAndName(DataGroup.class,
				APP_TOKENS_GROUP_NAME_IN_DATA);
	}

	private void handleApptokens() {
		for (DataGroup appTokenGroup : currentAppTokenGroups) {
			possiblyRemoveAppTokenClearTextFromIncomingRecord(appTokenGroup);
			possiblyCreateNewAppToken(appTokenGroup);
			possiblyKeepExistingAppToken(appTokenGroup);
		}
	}

	private void possiblyRemoveAppTokenClearTextFromIncomingRecord(DataGroup appTokenGroup) {
		if (appTokenClearExistOnIncomingRecord(appTokenGroup)) {
			appTokenGroup.removeFirstChildWithNameInData("appTokenClearText");
		}
	}

	private boolean appTokenClearExistOnIncomingRecord(DataGroup appTokenGroup) {
		return appTokenGroup.containsChildWithNameInData("appTokenClearText");
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

	private void removeAppTokensGroupIfNoAppTokensExists(DataRecordGroup currentDataGroup) {
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

	public AppTokenGenerator onlyForTestGetAppTokenGenerator() {
		return appTokenGenerator;
	}

	public SystemSecretOperations onlyForTestGetSystemSecretOperations() {
		return systemSecretOperations;
	}
}
