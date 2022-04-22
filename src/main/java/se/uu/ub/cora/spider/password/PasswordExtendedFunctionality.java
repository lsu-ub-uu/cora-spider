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

/**
 * PasswordExtendedFunctionality encrypts and stores a users password as a systemSecret, if it is
 * set in the parameter plainTextPassword.
 * <p>
 * PasswordExtendedFunctionality is NOT threadsafe.
 */
public class PasswordExtendedFunctionality implements ExtendedFunctionality {

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
			encryptAndStoreUsersPassword();
		}
	}

	private boolean plainTextPasswordHasValueInUserGroup() {
		return userGroup.containsChildWithNameInData(PLAIN_TEXT_PASSWORD);
	}

	private void encryptAndStoreUsersPassword() {
		String hashedPassword = hashPasswordFromUserGroup();
		DataRecord systemSecretRecord = createAndStoreSystemSecretRecord(hashedPassword);

		var secretLink = createLinkPointingToSecretRecord(systemSecretRecord);
		userGroup.addChild(secretLink);

		DataAtomic tsPasswordUpdated = createtAtomicLatestTsUpdatedFromRecord(systemSecretRecord);
		userGroup.addChild(tsPasswordUpdated);

		userGroup.removeAllChildrenWithNameInData(PLAIN_TEXT_PASSWORD);
	}

	private DataRecord createAndStoreSystemSecretRecord(String hashedPassword) {
		DataGroup systemSecret = createSystemSecretGroupWithRecordInfoAndHashedPassword(
				hashedPassword);
		RecordCreator recordCreator = SpiderInstanceProvider.getRecordCreator();
		return recordCreator.createAndStoreRecord(data.authToken, SYSTEM_SECRET_TYPE, systemSecret);
	}

	private DataAtomic createtAtomicLatestTsUpdatedFromRecord(DataRecord systemSecretRecord) {
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
		return DataProvider.createRecordLinkUsingNameInDataAndTypeAndId("password",
				SYSTEM_SECRET_TYPE, systemSecretRecordId);
	}

	private DataGroup createSystemSecretGroupWithRecordInfoAndHashedPassword(
			String hashedPassword) {
		DataGroup systemSecret = DataProvider.createGroupUsingNameInData(SYSTEM_SECRET_TYPE);

		DataGroup recordInfo = DataProvider.createGroupUsingNameInData(RECORD_INFO);
		systemSecret.addChild(recordInfo);

		DataGroup usersRecordInfo = userGroup.getFirstGroupWithNameInData(RECORD_INFO);
		DataRecordLink userDataDivider = (DataRecordLink) usersRecordInfo
				.getFirstChildWithNameInData(DATA_DIVIDER);
		String usersDataDividerId = userDataDivider.getLinkedRecordId();

		DataRecordLink dataDivider = DataProvider.createRecordLinkUsingNameInDataAndTypeAndId(
				DATA_DIVIDER, "system", usersDataDividerId);
		recordInfo.addChild(dataDivider);

		DataAtomic hashedPasswordAtomic = DataProvider.createAtomicUsingNameInDataAndValue("secret",
				hashedPassword);
		systemSecret.addChild(hashedPasswordAtomic);
		return systemSecret;
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
