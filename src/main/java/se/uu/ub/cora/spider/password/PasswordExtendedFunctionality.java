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

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

import se.uu.ub.cora.data.DataAtomic;
import se.uu.ub.cora.data.DataChild;
import se.uu.ub.cora.data.DataGroup;
import se.uu.ub.cora.data.DataProvider;
import se.uu.ub.cora.data.DataRecordLink;
import se.uu.ub.cora.password.texthasher.TextHasher;
import se.uu.ub.cora.spider.dependency.SpiderDependencyProvider;
import se.uu.ub.cora.spider.extendedfunctionality.ExtendedFunctionality;
import se.uu.ub.cora.spider.extendedfunctionality.ExtendedFunctionalityData;
import se.uu.ub.cora.storage.RecordIdGenerator;
import se.uu.ub.cora.storage.RecordStorage;

/**
 * PasswordExtendedFunctionality encrypts and stores a users password as a systemSecret, if it is
 * set in the parameter plainTextPassword.
 * <p>
 * PasswordExtendedFunctionality is NOT threadsafe.
 */
public class PasswordExtendedFunctionality implements ExtendedFunctionality {

	private static final String TYPE = "type";
	private static final String PASSWORD = "password";
	private static final String DATA_DIVIDER = "dataDivider";
	private static final String RECORD_INFO = "recordInfo";
	private static final String PLAIN_TEXT_PASSWORD = "plainTextPassword";
	private static final String SYSTEM_SECRET_TYPE = "systemSecret";

	private static final String DATE_TIME_PATTERN = "yyyy-MM-dd HH:mm:ss";
	private static DateTimeFormatter dateTimeFormatter = DateTimeFormatter
			.ofPattern(DATE_TIME_PATTERN);

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
		return !userGroup.containsChildWithNameInData(PASSWORD);
	}

	private void createSystemSecretAndUpdateUser(String hashedPassword) {
		String systemSecretId = createAndStoreSystemSecretRecord(hashedPassword);
		addLinkToSystemSecret(systemSecretId);
		setTimestampWhenPasswordHasBeenStored();
	}

	private String createAndStoreSystemSecretRecord(String hashedPassword) {
		RecordIdGenerator recordIdGenerator = dependencyProvider.getRecordIdGenerator();
		String systemSecretId = recordIdGenerator.getIdForType(SYSTEM_SECRET_TYPE);

		DataGroup systemSecret = createSystemSecretGroupWithRecordInfoAndHashedPassword(
				hashedPassword, systemSecretId);
		RecordStorage recordStorage = dependencyProvider.getRecordStorage();

		DataGroup collectedTerms = createCollectedTerms(systemSecretId);
		DataGroup collectedDataLinks = createCollectedDataLinks();

		String dataDivider = readDataDividerFromUserGroup();

		recordStorage.create(SYSTEM_SECRET_TYPE, systemSecretId, systemSecret, collectedTerms,
				collectedDataLinks, dataDivider);
		return systemSecretId;
	}

	private DataGroup createCollectedDataLinks() {
		return DataProvider.createGroupUsingNameInData("collectedDataLinks");
	}

	private DataGroup createCollectedTerms(String systemSecretId) {
		DataGroup collectedTerms = DataProvider.createGroupUsingNameInData("collectedData");
		DataChild collectedTermsType = DataProvider.createAtomicUsingNameInDataAndValue(TYPE,
				SYSTEM_SECRET_TYPE);
		DataChild collectedTermsId = DataProvider.createAtomicUsingNameInDataAndValue("id",
				systemSecretId);
		collectedTerms.addChild(collectedTermsType);
		collectedTerms.addChild(collectedTermsId);
		return collectedTerms;
	}

	// private DataGroup createDataGroupWithTypeAndId(String type, String id) {
	// DataGroup collectedData = DataGroupProvider.getDataGroupUsingNameInData("collectedData");
	// collectedData
	// .addChild(DataAtomicProvider.getDataAtomicUsingNameInDataAndValue("type", type));
	// collectedData.addChild(DataAtomicProvider.getDataAtomicUsingNameInDataAndValue("id", id));
	// return collectedData;
	// }
	// DataGroup collectedDataLinks = DataGroupProvider
	// .getDataGroupUsingNameInData("collectedDataLinks");

	private DataGroup createSystemSecretGroupWithRecordInfoAndHashedPassword(String hashedPassword,
			String systemSecretId) {
		DataGroup systemSecret = DataProvider.createGroupUsingNameInData(SYSTEM_SECRET_TYPE);

		createAndAddRecordInfoForSystemSecret(systemSecret, systemSecretId);
		addHashedPasswordToGroup(hashedPassword, systemSecret);
		return systemSecret;
	}

	private void createAndAddRecordInfoForSystemSecret(DataGroup systemSecret,
			String systemSecretId) {
		DataGroup recordInfo = DataProvider.createGroupUsingNameInData(RECORD_INFO);
		systemSecret.addChild(recordInfo);

		DataRecordLink typeLink = DataProvider.createRecordLinkUsingNameInDataAndTypeAndId(TYPE,
				"recordType", SYSTEM_SECRET_TYPE);

		DataChild atomicId = DataProvider.createAtomicUsingNameInDataAndValue("id", systemSecretId);

		recordInfo.addChild(typeLink);
		recordInfo.addChild(atomicId);
	}

	private void addLinkToSystemSecret(String systemSecretRecordId) {
		var secretLink = createLinkPointingToSecretRecord(systemSecretRecordId);
		userGroup.addChild(secretLink);
	}

	private void setTimestampWhenPasswordHasBeenStored() {
		DataAtomic tsPasswordUpdated = createAtomicLatestTsUpdatedFromRecord();
		userGroup.addChild(tsPasswordUpdated);
	}

	private void updateSystemSecretAndUpdateUser(String hashedPassword) {
		String systemSecretId = readSystemSecretId();
		replacePasswordInSystemSecret(systemSecretId, hashedPassword);
		DataAtomic tsPasswordUpdated = updateTsPasswordUpdatedUsingTsUppdate();
		userGroup.addChild(tsPasswordUpdated);
	}

	private DataAtomic updateTsPasswordUpdatedUsingTsUppdate() {
		userGroup.removeAllChildrenWithNameInData("tsPasswordUpdated");
		return createAtomicLatestTsUpdatedFromRecord();
	}

	private void replacePasswordInSystemSecret(String systemSecretId, String hashedPassword) {
		RecordStorage recordStorage = dependencyProvider.getRecordStorage();
		DataGroup systemSecretG = recordStorage.read(SYSTEM_SECRET_TYPE, systemSecretId);
		systemSecretG.removeAllChildrenWithNameInData(PASSWORD);

		// DataRecord systemSecret = readSystemSecretRecord(systemSecretId);
		// DataGroup systemSecretG = removeOldPassword(systemSecret);
		addHashedPasswordToGroup(hashedPassword, systemSecretG);
		DataGroup collectedTerms = createCollectedTerms(systemSecretId);
		DataGroup collectedDataLinks = createCollectedDataLinks();
		String dataDivider = readDataDividerFromUserGroup();

		recordStorage.update(SYSTEM_SECRET_TYPE, systemSecretId, systemSecretG, collectedTerms,
				collectedDataLinks, dataDivider);
	}

	// private void updateSystemSecretRecord(String systemSecretId, DataGroup systemSecretG) {
	// RecordUpdater recordUpdater = SpiderInstanceProvider.getRecordUpdater();
	// recordUpdater.updateRecord(data.authToken, SYSTEM_SECRET_TYPE, systemSecretId,
	// systemSecretG);
	// }
	//
	// private DataGroup removeOldPassword(DataRecord systemRecord) {
	// DataGroup systemGroup = systemRecord.getDataGroup();
	// systemGroup.removeAllChildrenWithNameInData(PASSWORD);
	// return systemGroup;
	// }
	//
	// private DataRecord readSystemSecretRecord(String systemSecretId) {
	// RecordStorage recordStorage = dependencyProvider.getRecordStorage();
	// DataGroup read = recordStorage.read(SYSTEM_SECRET_TYPE, systemSecretId);
	//
	// RecordReader recordReader = SpiderInstanceProvider.getRecordReader();
	// return recordReader.readRecord(data.authToken, SYSTEM_SECRET_TYPE, systemSecretId);
	// }

	private String readSystemSecretId() {
		DataRecordLink passwordLink = (DataRecordLink) userGroup
				.getFirstChildWithNameInData(PASSWORD);
		return passwordLink.getLinkedRecordId();
	}

	private DataAtomic createAtomicLatestTsUpdatedFromRecord() {
		// DataGroup recordInfo = getRecordInfoFromRecord(systemSecretRecord);

		// String tsUpdated = getLatestTsUpdatedValue(recordInfo);
		String tsUpdated = getCurrentFormattedTime();

		return DataProvider.createAtomicUsingNameInDataAndValue("tsPasswordUpdated", tsUpdated);
	}

	private static String getCurrentFormattedTime() {
		LocalDateTime currentDateTime = LocalDateTime.now();
		return currentDateTime.format(dateTimeFormatter);
	}

	// private DataGroup getRecordInfoFromRecord(DataRecord systemSecretRecord) {
	// DataGroup secretGroup = systemSecretRecord.getDataGroup();
	// return secretGroup.getFirstGroupWithNameInData(RECORD_INFO);
	// }
	//
	// private String getLatestTsUpdatedValue(DataGroup recordInfo) {
	// List<DataGroup> allGroupsWithNameInData = recordInfo.getAllGroupsWithNameInData("updated");
	// DataGroup latestTsUpdated = allGroupsWithNameInData.get(allGroupsWithNameInData.size() - 1);
	// return latestTsUpdated.getFirstAtomicValueWithNameInData("tsUpdated");
	// }

	private DataRecordLink createLinkPointingToSecretRecord(String systemSecretRecordId) {
		return DataProvider.createRecordLinkUsingNameInDataAndTypeAndId(PASSWORD,
				SYSTEM_SECRET_TYPE, systemSecretRecordId);
	}

	// private DataRecordLink createDataDivider() {
	// String usersDataDividerId = readDataDividerFromUserGroup();
	// return DataProvider.createRecordLinkUsingNameInDataAndTypeAndId(DATA_DIVIDER, "system",
	// usersDataDividerId);
	// }

	private String readDataDividerFromUserGroup() {
		DataGroup usersRecordInfo = userGroup.getFirstGroupWithNameInData(RECORD_INFO);
		DataRecordLink userDataDivider = (DataRecordLink) usersRecordInfo
				.getFirstChildWithNameInData(DATA_DIVIDER);
		return userDataDivider.getLinkedRecordId();
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
