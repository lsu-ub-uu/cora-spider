/*
 * Copyright 2022, 2024 Uppsala University Library
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
package se.uu.ub.cora.spider.extended.password;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

import se.uu.ub.cora.data.DataAtomic;
import se.uu.ub.cora.data.DataProvider;
import se.uu.ub.cora.data.DataRecordGroup;
import se.uu.ub.cora.data.DataRecordLink;
import se.uu.ub.cora.spider.dependency.SpiderDependencyProvider;
import se.uu.ub.cora.spider.extendedfunctionality.ExtendedFunctionality;
import se.uu.ub.cora.spider.extendedfunctionality.ExtendedFunctionalityData;
import se.uu.ub.cora.spider.record.DataException;
import se.uu.ub.cora.spider.systemsecret.SystemSecretOperations;

/**
 * PasswordExtendedFunctionality encrypts and stores a users password as a systemSecret, if it is
 * set in the parameter plainTextPassword.
 * <p>
 * PasswordExtendedFunctionality is NOT threadsafe.
 */
public class PasswordExtendedFunctionality implements ExtendedFunctionality {
	private static final String TS_PASSWORD_UPDATED_NAME_IN_DATA = "tsPasswordUpdated";
	private static final String PASSWORD_LINK_NAME_IN_DATA = "passwordLink";
	private static final String PLAIN_TEXT_PASSWORD_NAME_IN_DATA = "plainTextPassword";
	private static final String SYSTEM_SECRET_TYPE = "systemSecret";
	private static final String DATE_TIME_PATTERN = "yyyy-MM-dd HH:mm:ss";

	private static DateTimeFormatter dateTimeFormatter = DateTimeFormatter
			.ofPattern(DATE_TIME_PATTERN);

	private SpiderDependencyProvider dependencyProvider;
	private DataRecordGroup currentUserRecord;
	private DataRecordGroup previousUserRecord;
	private SystemSecretOperations systemSecretOperations;

	public static PasswordExtendedFunctionality usingDependencyProviderAndSystemSecretOperations(
			SpiderDependencyProvider dependencyProvider,
			SystemSecretOperations systemSecretOperations) {
		return new PasswordExtendedFunctionality(dependencyProvider, systemSecretOperations);
	}

	private PasswordExtendedFunctionality(SpiderDependencyProvider dependencyProvider,
			SystemSecretOperations systemSecretCreator) {
		this.dependencyProvider = dependencyProvider;
		this.systemSecretOperations = systemSecretCreator;
	}

	@Override
	public void useExtendedFunctionality(ExtendedFunctionalityData data) {
		currentUserRecord = data.dataRecordGroup;
		previousUserRecord = data.previouslyStoredDataRecordGroup;
		validateData();
		possiblyCreateNewPassword();
		possiblyRemovePassword();
		ensurePlainTextPasswordIsRemoved();
	}

	private void validateData() {
		if (dataForPasswordIsNotValid()) {
			throw new DataException(
					"UsePassword set to true but no old password or new password exists.");
		}
	}

	private boolean dataForPasswordIsNotValid() {
		return currentUsePassword() && previousDoNotUsePassword() && !newPasswordSet();
	}

	private boolean currentUsePassword() {
		return "true".equals(currentUserRecord.getFirstAtomicValueWithNameInData("usePassword"));
	}

	private boolean previousDoNotUsePassword() {
		return "false".equals(previousUserRecord.getFirstAtomicValueWithNameInData("usePassword"));
	}

	private boolean newPasswordSet() {
		return currentUserRecord.containsChildWithNameInData(PLAIN_TEXT_PASSWORD_NAME_IN_DATA);
	}

	private void possiblyCreateNewPassword() {
		if (plainTextPasswordHasValueInUserGroup()) {
			handleNewOrUpdatedPassword();
		}
	}

	private boolean plainTextPasswordHasValueInUserGroup() {
		return currentUsePassword() && newPasswordSet();
	}

	private void handleNewOrUpdatedPassword() {
		String newPassword = getNewPasswordAndRemoveItFromUserGroup();
		if (newPasswordNeedsToBeCreated()) {
			createSystemSecretAndUpdateUser(newPassword);
		} else {
			updateSystemSecretAndUpdateUser(newPassword);
		}
	}

	private String getNewPasswordAndRemoveItFromUserGroup() {
		return currentUserRecord
				.getFirstAtomicValueWithNameInData(PLAIN_TEXT_PASSWORD_NAME_IN_DATA);
	}

	private boolean newPasswordNeedsToBeCreated() {
		return !currentUserRecord.containsChildWithNameInData(PASSWORD_LINK_NAME_IN_DATA);
	}

	private void createSystemSecretAndUpdateUser(String plainTextPassword) {
		String dataDivider = currentUserRecord.getDataDivider();
		String systemSecretId = systemSecretOperations
				.createAndStoreSystemSecretRecord(plainTextPassword, dataDivider);
		addLinkToSystemSecret(systemSecretId);
		setTimestampWhenPasswordHasBeenStored();
	}

	private void addLinkToSystemSecret(String systemSecretRecordId) {
		var secretLink = createLinkPointingToSecretRecord(systemSecretRecordId);
		currentUserRecord.addChild(secretLink);
	}

	private DataRecordLink createLinkPointingToSecretRecord(String systemSecretRecordId) {
		return DataProvider.createRecordLinkUsingNameInDataAndTypeAndId(PASSWORD_LINK_NAME_IN_DATA,
				SYSTEM_SECRET_TYPE, systemSecretRecordId);
	}

	private void setTimestampWhenPasswordHasBeenStored() {
		DataAtomic tsPasswordUpdated = createAtomicLatestTsUpdatedFromRecord();
		currentUserRecord.addChild(tsPasswordUpdated);
	}

	private void updateSystemSecretAndUpdateUser(String passwordPlainText) {
		String systemSecretId = readSystemSecretIdFromLinkInUser();
		String dataDivider = currentUserRecord.getDataDivider();
		systemSecretOperations.updateSecretForASystemSecret(systemSecretId, dataDivider,
				passwordPlainText);
		updateTsPasswordUpdatedUsingTsUpdate();
	}

	private String readSystemSecretIdFromLinkInUser() {
		DataRecordLink passwordLink = currentUserRecord
				.getFirstChildOfTypeAndName(DataRecordLink.class, PASSWORD_LINK_NAME_IN_DATA);
		return passwordLink.getLinkedRecordId();
	}

	private void updateTsPasswordUpdatedUsingTsUpdate() {
		currentUserRecord.removeAllChildrenWithNameInData(TS_PASSWORD_UPDATED_NAME_IN_DATA);
		DataAtomic tsPasswordUpdated = createAtomicLatestTsUpdatedFromRecord();
		currentUserRecord.addChild(tsPasswordUpdated);
	}

	private DataAtomic createAtomicLatestTsUpdatedFromRecord() {
		String tsUpdated = getCurrentFormattedTime();
		return DataProvider.createAtomicUsingNameInDataAndValue(TS_PASSWORD_UPDATED_NAME_IN_DATA,
				tsUpdated);
	}

	private static String getCurrentFormattedTime() {
		LocalDateTime currentDateTime = LocalDateTime.now();
		return currentDateTime.format(dateTimeFormatter);
	}

	private void possiblyRemovePassword() {
		if (!currentUsePassword()
				&& currentUserRecord.containsChildWithNameInData(PASSWORD_LINK_NAME_IN_DATA)) {
			currentUserRecord.removeAllChildrenWithNameInData(PASSWORD_LINK_NAME_IN_DATA);
			currentUserRecord.removeAllChildrenWithNameInData(TS_PASSWORD_UPDATED_NAME_IN_DATA);
		}
	}

	private void ensurePlainTextPasswordIsRemoved() {
		currentUserRecord.removeAllChildrenWithNameInData(PLAIN_TEXT_PASSWORD_NAME_IN_DATA);
	}

	SpiderDependencyProvider onlyForTestGetDependencyProvider() {
		return dependencyProvider;
	}

	public SystemSecretOperations onlyForTestGetSystemSecretOperations() {
		return systemSecretOperations;
	}
}
