/*
 * Copyright 2022 Uppsala University Library
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
package se.uu.ub.cora.spider.password;

import java.util.List;

import se.uu.ub.cora.data.DataAtomic;
import se.uu.ub.cora.data.DataGroup;
import se.uu.ub.cora.data.DataProvider;
import se.uu.ub.cora.data.DataRecord;
import se.uu.ub.cora.data.DataRecordLink;
import se.uu.ub.cora.password.texthasher.TextHasher;
import se.uu.ub.cora.spider.dependency.SpiderDependencyProvider;
import se.uu.ub.cora.spider.dependency.SpiderInstanceProvider;
import se.uu.ub.cora.spider.extendedfunctionality.ExtendedFunctionality;
import se.uu.ub.cora.spider.extendedfunctionality.ExtendedFunctionalityData;
import se.uu.ub.cora.spider.record.RecordCreator;
import se.uu.ub.cora.spider.record.RecordReader;
import se.uu.ub.cora.spider.record.RecordUpdater;

/**
 * PasswordExtendedFunctionality encrypts and stores a users password as a systemSecret, if it is
 * set in the parameter plainTextPassword.
 * <p>
 * PasswordExtendedFunctionality is NOT threadsafe.
 */
public class PasswordExtendedFunctionality implements ExtendedFunctionality {

	private static final String PASSWORD = "password";
	private static final String DATA_DIVIDER = "dataDivider";
	private static final String RECORD_INFO = "recordInfo";
	private static final String PLAIN_TEXT_PASSWORD = "plainTextPassword";
	private static final String SYSTEM_SECRET_TYPE = "systemSecret";
	private SpiderDependencyProvider dependencyProvider;
	private TextHasher textHasher;
	private ExtendedFunctionalityData data;
	private DataGroup userGroup;

	public static PasswordExtendedFunctionality usingDependencyProviderAndTextHasher(
			SpiderDependencyProvider dependencyProvider, TextHasher textHasher) {
		return new PasswordExtendedFunctionality(dependencyProvider, textHasher);
	}

	private PasswordExtendedFunctionality(SpiderDependencyProvider dependencyProvider,
			TextHasher textHasher) {
		this.dependencyProvider = dependencyProvider;
		this.textHasher = textHasher;
	}

	@Override
	public void useExtendedFunctionality(ExtendedFunctionalityData data) {
		this.data = data;
		userGroup = data.dataGroup;
		if (plainTextPasswordHasValueInUserGroup()) {
			handleNewOrUpdatedPassword();
		}
	}

	private boolean plainTextPasswordHasValueInUserGroup() {
		return userGroup.containsChildWithNameInData(PLAIN_TEXT_PASSWORD);
	}

	private void handleNewOrUpdatedPassword() {
		String hashedPassword = hashPlainTextPasswordAndRemoveItFromUserGroup();
		if (newPasswordNeedsToBeCreated()) {
			createSystemSecretAndUpdateUser(hashedPassword);
		} else {
			updateSystemSecretAndUpdateUser(hashedPassword);
		}
	}

	private String hashPlainTextPasswordAndRemoveItFromUserGroup() {
		String hashedPassword = hashPasswordFromUserGroup();
		userGroup.removeAllChildrenWithNameInData(PLAIN_TEXT_PASSWORD);
		return hashedPassword;
	}

	private boolean newPasswordNeedsToBeCreated() {
		return !userGroup.containsChildWithNameInData("passwordLink");
	}

	private void createSystemSecretAndUpdateUser(String hashedPassword) {
		DataRecord systemSecretRecord = createAndStoreSystemSecretRecord(hashedPassword);
		addLinkToSystemSecret(systemSecretRecord);
		setTimestampWhenPasswordHasBeenStored(systemSecretRecord);
	}

	private DataRecord createAndStoreSystemSecretRecord(String hashedPassword) {
		DataGroup systemSecret = createSystemSecretGroupWithRecordInfoAndHashedPassword(
				hashedPassword);
		RecordCreator recordCreator = SpiderInstanceProvider.getRecordCreator();
		return recordCreator.createAndStoreRecord(data.authToken, SYSTEM_SECRET_TYPE, systemSecret);
	}

	private DataGroup createSystemSecretGroupWithRecordInfoAndHashedPassword(
			String hashedPassword) {
		DataGroup systemSecret = DataProvider.createGroupUsingNameInData(SYSTEM_SECRET_TYPE);

		createAndAddRecordInfoForSystemSecret(systemSecret);
		addHashedPasswordToGroup(hashedPassword, systemSecret);
		return systemSecret;
	}

	private void createAndAddRecordInfoForSystemSecret(DataGroup systemSecret) {
		DataGroup recordInfo = DataProvider.createGroupUsingNameInData(RECORD_INFO);
		systemSecret.addChild(recordInfo);

		DataRecordLink dataDivider = createDataDivider();
		recordInfo.addChild(dataDivider);
	}

	private void addLinkToSystemSecret(DataRecord systemSecretRecord) {
		var secretLink = createLinkPointingToSecretRecord(systemSecretRecord);
		userGroup.addChild(secretLink);
	}

	private void setTimestampWhenPasswordHasBeenStored(DataRecord systemSecretRecord) {
		DataAtomic tsPasswordUpdated = createAtomicLatestTsUpdatedFromRecord(systemSecretRecord);
		userGroup.addChild(tsPasswordUpdated);
	}

	private void updateSystemSecretAndUpdateUser(String hashedPassword) {
		String systemSecretId = readSystemSecretId();
		DataRecord systemSecretRecord = replacePasswordInSystemSecret(systemSecretId,
				hashedPassword);
		DataAtomic tsPasswordUpdated = updateTsPasswordUpdatedUsingTsUppdate(systemSecretRecord);
		userGroup.addChild(tsPasswordUpdated);
	}

	private DataAtomic updateTsPasswordUpdatedUsingTsUppdate(DataRecord systemSecretRecord) {
		userGroup.removeAllChildrenWithNameInData("tsPasswordUpdated");
		return createAtomicLatestTsUpdatedFromRecord(systemSecretRecord);
	}

	private DataRecord replacePasswordInSystemSecret(String systemSecretId, String hashedPassword) {
		DataRecord systemSecret = readSystemSecretRecord(systemSecretId);
		DataGroup systemSecretG = removeOldPassword(systemSecret);
		addHashedPasswordToGroup(hashedPassword, systemSecretG);
		return updateSystemSecretRecord(systemSecretId, systemSecretG);
	}

	private DataRecord updateSystemSecretRecord(String systemSecretId, DataGroup systemSecretG) {
		RecordUpdater recordUpdater = SpiderInstanceProvider.getRecordUpdater();
		return recordUpdater.updateRecord(data.authToken, SYSTEM_SECRET_TYPE, systemSecretId,
				systemSecretG);
	}

	private DataGroup removeOldPassword(DataRecord systemRecord) {
		DataGroup systemGroup = systemRecord.getDataGroup();
		systemGroup.removeAllChildrenWithNameInData(PASSWORD);
		return systemGroup;
	}

	private DataRecord readSystemSecretRecord(String systemSecretId) {
		RecordReader recordReader = SpiderInstanceProvider.getRecordReader();
		return recordReader.readRecord(data.authToken, SYSTEM_SECRET_TYPE, systemSecretId);
	}

	private String readSystemSecretId() {
		DataRecordLink passwordLink = (DataRecordLink) userGroup
				.getFirstChildWithNameInData("passwordLink");
		return passwordLink.getLinkedRecordId();
	}

	private DataAtomic createAtomicLatestTsUpdatedFromRecord(DataRecord systemSecretRecord) {
		DataGroup recordInfo = getRecordInfoFromRecord(systemSecretRecord);
		String tsUpdated = getLatestTsUpdatedValue(recordInfo);

		return DataProvider.createAtomicUsingNameInDataAndValue("tsPasswordUpdated", tsUpdated);
	}

	private DataGroup getRecordInfoFromRecord(DataRecord systemSecretRecord) {
		DataGroup secretGroup = systemSecretRecord.getDataGroup();
		return secretGroup.getFirstGroupWithNameInData(RECORD_INFO);
	}

	private String getLatestTsUpdatedValue(DataGroup recordInfo) {
		List<DataGroup> allGroupsWithNameInData = recordInfo.getAllGroupsWithNameInData("updated");
		DataGroup latestTsUpdated = allGroupsWithNameInData.get(allGroupsWithNameInData.size() - 1);
		return latestTsUpdated.getFirstAtomicValueWithNameInData("tsUpdated");
	}

	private DataRecordLink createLinkPointingToSecretRecord(DataRecord systemSecretRecord) {
		String systemSecretRecordId = systemSecretRecord.getId();
		return DataProvider.createRecordLinkUsingNameInDataAndTypeAndId(PASSWORD,
				SYSTEM_SECRET_TYPE, systemSecretRecordId);
	}

	private DataRecordLink createDataDivider() {
		DataGroup usersRecordInfo = userGroup.getFirstGroupWithNameInData(RECORD_INFO);
		DataRecordLink userDataDivider = (DataRecordLink) usersRecordInfo
				.getFirstChildWithNameInData(DATA_DIVIDER);
		String usersDataDividerId = userDataDivider.getLinkedRecordId();

		return DataProvider.createRecordLinkUsingNameInDataAndTypeAndId(DATA_DIVIDER, "system",
				usersDataDividerId);
	}

	private void addHashedPasswordToGroup(String hashedPassword, DataGroup systemSecret) {
		DataAtomic hashedPasswordAtomic = DataProvider.createAtomicUsingNameInDataAndValue("secret",
				hashedPassword);
		systemSecret.addChild(hashedPasswordAtomic);
	}

	private String hashPasswordFromUserGroup() {
		String plainTextPassword = userGroup.getFirstAtomicValueWithNameInData(PLAIN_TEXT_PASSWORD);
		return textHasher.hashText(plainTextPassword);
	}

	SpiderDependencyProvider onlyForTestGetDependencyProvider() {
		return dependencyProvider;
	}

	TextHasher onlyForTestGetTextHasher() {
		return textHasher;
	}

}
