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

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertTrue;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.Collections;
import java.util.List;
import java.util.function.Supplier;

import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import se.uu.ub.cora.data.DataProvider;
import se.uu.ub.cora.data.spies.DataAtomicSpy;
import se.uu.ub.cora.data.spies.DataFactorySpy;
import se.uu.ub.cora.data.spies.DataGroupSpy;
import se.uu.ub.cora.data.spies.DataRecordLinkSpy;
import se.uu.ub.cora.data.spies.DataRecordSpy;
import se.uu.ub.cora.password.texthasher.TextHasher;
import se.uu.ub.cora.spider.dependency.SpiderDependencyProvider;
import se.uu.ub.cora.spider.dependency.SpiderInstanceProvider;
import se.uu.ub.cora.spider.dependency.spy.RecordIdGeneratorSpy;
import se.uu.ub.cora.spider.dependency.spy.SpiderDependencyProviderOldSpy;
import se.uu.ub.cora.spider.extendedfunctionality.ExtendedFunctionalityData;
import se.uu.ub.cora.spider.testspies.RecordCreatorSpy;
import se.uu.ub.cora.spider.testspies.RecordReaderSpy;
import se.uu.ub.cora.spider.testspies.RecordUpdaterSpy;
import se.uu.ub.cora.spider.testspies.SpiderInstanceFactorySpy;
import se.uu.ub.cora.storage.spies.RecordStorageSpy;
import se.uu.ub.cora.testutils.mcr.MethodCallRecorder;
import se.uu.ub.cora.testutils.mrv.MethodReturnValues;

public class PasswordExtendedFunctionalityTest {
	private static final String SECRET = "secret";
	private static final String PASSWORD_GROUP_NAME_IN_DATA = "password";
	private static final String SECRET_LINK_NAME_IN_DATA = "passwordLink";
	private static final String SYSTEM_SECRET_TYPE = "systemSecret";
	private static final String DATE_PATTERN = "yyyy-MM-dd HH:mm:ss";

	private SpiderDependencyProviderOldSpy dependencyProvider;
	private TextHasherSpy textHasher;
	private PasswordExtendedFunctionality extended;
	private ExtendedFunctionalityData efData;
	private DataFactorySpy dataFactory;
	private DataGroupSpy dataRecordGroup;
	private MethodCallRecorder rGroupMCR;
	private MethodReturnValues rGroupMRV;
	private DataRecordLinkSpy dataDivider;
	private SpiderInstanceFactorySpy spiderInstanceFactory;
	private DateTimeFormatter dateTimePattern = DateTimeFormatter.ofPattern(DATE_PATTERN);
	private RecordIdGeneratorSpy recordIdGeneratorSpy;;

	@BeforeMethod
	public void beforeMethod() {
		dataFactory = new DataFactorySpy();
		DataProvider.onlyForTestSetDataFactory(dataFactory);

		dependencyProvider = new SpiderDependencyProviderOldSpy();
		textHasher = new TextHasherSpy();
		extended = PasswordExtendedFunctionality
				.usingDependencyProviderAndTextHasher(dependencyProvider, textHasher);
		setupSpyForDataRecordGroup();
		createExtendedFunctionalityData();

		spiderInstanceFactory = new SpiderInstanceFactorySpy();
		SpiderInstanceProvider.setSpiderInstanceFactory(spiderInstanceFactory);

		RecordStorageSpy recordStorage = (RecordStorageSpy) dependencyProvider.recordStorage;
		recordStorage.MRV.setDefaultReturnValuesSupplier("read", DataGroupSpy::new);

		recordIdGeneratorSpy = new RecordIdGeneratorSpy();
		dependencyProvider.recordIdGenerator = recordIdGeneratorSpy;

	}

	private void setupSpyForDataRecordGroup() {
		dataRecordGroup = new DataGroupSpy();
		rGroupMCR = dataRecordGroup.MCR;
		rGroupMRV = dataRecordGroup.MRV;
		rGroupMRV.setSpecificReturnValuesSupplier("containsChildWithNameInData",
				((Supplier<Boolean>) () -> true), "plainTextPassword");

		setupDataDividerForDataRecordGroup();
	}

	private void setupDataDividerForDataRecordGroup() {
		DataGroupSpy recordInfo = new DataGroupSpy();
		rGroupMRV.setReturnValues("getFirstGroupWithNameInData", List.of(recordInfo), "recordInfo");
		dataDivider = new DataRecordLinkSpy();
		recordInfo.MRV.setReturnValues("getFirstChildWithNameInData", List.of(dataDivider),
				"dataDivider");
		dataDivider.MRV.setReturnValues("getLinkedRecordId", List.of("someDataDivider"));
	}

	private void createExtendedFunctionalityData() {
		efData = new ExtendedFunctionalityData();
		efData.dataGroup = dataRecordGroup;
		efData.authToken = "fakeToken";
		efData.recordType = "fakeType";
		efData.recordId = "fakeId";
	}

	@Test
	public void testOnlyForTest() throws Exception {
		SpiderDependencyProvider returnedProvider = extended.onlyForTestGetDependencyProvider();
		assertEquals(returnedProvider, dependencyProvider);
		TextHasher returnedHasher = extended.onlyForTestGetTextHasher();
		assertEquals(returnedHasher, textHasher);
	}

	@Test
	public void testIfPlainTextPasswordIsNotPresentNoGetOrRemove() throws Exception {
		rGroupMRV.setReturnValues("containsChildWithNameInData", List.of(false),
				"plainTextPassword");

		extended.useExtendedFunctionality(efData);

		rGroupMCR.assertParameters("containsChildWithNameInData", 0, "plainTextPassword");
		rGroupMCR.assertMethodNotCalled("getFirstAtomicValueWithNameInData");
		rGroupMCR.assertMethodNotCalled("removeAllChildrenWithNameInData");
		dataFactory.MCR.assertMethodNotCalled("factorGroupUsingNameInData");
	}

	@Test
	public void testPlainTextPasswordIsRemovedFromDataRecordGroupCreate() throws Exception {
		setUpSpiesForCreateReturningDataRecordWithTsUpdated();

		extended.useExtendedFunctionality(efData);

		rGroupMCR.assertParameters("containsChildWithNameInData", 0, "plainTextPassword");
		rGroupMCR.assertParameters("getFirstAtomicValueWithNameInData", 0, "plainTextPassword");
		rGroupMCR.assertParameters("removeAllChildrenWithNameInData", 0, "plainTextPassword");
	}

	@Test
	public void testPlainTextPasswordIsRemovedFromDataRecordGroupUpdate() throws Exception {
		setUpSpiesForUpdateReturningDataRecordWithTsUpdated();

		extended.useExtendedFunctionality(efData);

		rGroupMCR.assertParameters("containsChildWithNameInData", 0, "plainTextPassword");
		rGroupMCR.assertParameters("getFirstAtomicValueWithNameInData", 0, "plainTextPassword");
		rGroupMCR.assertParameters("removeAllChildrenWithNameInData", 0, "plainTextPassword");
	}

	@Test
	public void testPlainTextPasswordIsHashed() throws Exception {
		setUpSpiesForCreateReturningDataRecordWithTsUpdated();

		extended.useExtendedFunctionality(efData);

		assertPlainTextHashed();
	}

	private void assertPlainTextHashed() {
		var plainTextPassword = rGroupMCR.getReturnValue("getFirstAtomicValueWithNameInData", 0);
		textHasher.MCR.assertParameters("hashText", 0, plainTextPassword);
	}

	@Test
	public void testCreateNewSystemSecretRecord() throws Exception {
		setUpSpiesForCreateReturningDataRecordWithTsUpdated();
		rGroupMRV.setReturnValues("containsChildWithNameInData", List.of(false),
				PASSWORD_GROUP_NAME_IN_DATA);

		extended.useExtendedFunctionality(efData);

		dataFactory.MCR.assertParameters("factorGroupUsingNameInData", 0, SYSTEM_SECRET_TYPE);
		DataGroupSpy systemSecret = (DataGroupSpy) dataFactory.MCR
				.getReturnValue("factorGroupUsingNameInData", 0);

		String systemSecretId = assertAndReturnSystemSecretId();
		assertRecordInfoWithDataDividerAddedTo(systemSecret, systemSecretId);
		assertHashedPasswordIsAddedToGroupAsNumber(systemSecret, 1, 1);

	}

	private void assertRecordInfoWithDataDividerAddedTo(DataGroupSpy systemSecret,
			String systemSecretId) {
		dataFactory.MCR.assertParameters("factorGroupUsingNameInData", 1, "recordInfo");
		DataGroupSpy recordInfo = (DataGroupSpy) dataFactory.MCR
				.getReturnValue("factorGroupUsingNameInData", 1);
		systemSecret.MCR.assertParameters("addChild", 0, recordInfo);

		dataFactory.MCR.assertParameters("factorRecordLinkUsingNameInDataAndTypeAndId", 0, "type",
				"recordType", SYSTEM_SECRET_TYPE);
		var typeLink = dataFactory.MCR.getReturnValue("factorRecordLinkUsingNameInDataAndTypeAndId",
				0);
		recordInfo.MCR.assertParameters("addChild", 0, typeLink);

		dataFactory.MCR.assertParameters("factorAtomicUsingNameInDataAndValue", 0, "id",
				systemSecretId);
		var id = dataFactory.MCR.getReturnValue("factorAtomicUsingNameInDataAndValue", 0);
		recordInfo.MCR.assertParameters("addChild", 1, id);
	}

	private void assertHashedPasswordIsAddedToGroupAsNumber(DataGroupSpy secret,
			int factorAtomicCallNumber, int addedAsNo) {
		var hashedPassword = textHasher.MCR.getReturnValue("hashText", 0);
		dataFactory.MCR.assertParameters("factorAtomicUsingNameInDataAndValue",
				factorAtomicCallNumber, SECRET, hashedPassword);
		var hashedPasswordAtomic = dataFactory.MCR
				.getReturnValue("factorAtomicUsingNameInDataAndValue", factorAtomicCallNumber);
		secret.MCR.assertParameters("addChild", addedAsNo, hashedPasswordAtomic);
	}

	@Test
	public void testSystemSecretIsCreatedForNewPassword() throws Exception {
		setUpSpiesForCreateReturningDataRecordWithTsUpdated();

		extended.useExtendedFunctionality(efData);

		DataGroupSpy secret = (DataGroupSpy) dataFactory.MCR
				.getReturnValue("factorGroupUsingNameInData", 0);

		RecordStorageSpy recordStorage = (RecordStorageSpy) dependencyProvider.MCR
				.getReturnValue("getRecordStorage", 0);

		String systemSecretId = assertAndReturnSystemSecretId();
		String dataDividerValue = (String) dataDivider.MCR.getReturnValue("getLinkedRecordId", 0);

		recordStorage.MCR.assertParameters("create", 0, SYSTEM_SECRET_TYPE, systemSecretId, secret,
				Collections.emptySet(), Collections.emptySet(), dataDividerValue);

		assertLinkToSystemSecret(systemSecretId, 2, 1);
	}

	private String assertAndReturnSystemSecretId() {
		recordIdGeneratorSpy.MCR.assertParameters("getIdForType", 0, SYSTEM_SECRET_TYPE);
		return (String) recordIdGeneratorSpy.MCR.getReturnValue("getIdForType", 0);
	}

	private void assertLinkToSystemSecret(String systemSecretId, int factoredGroupCallNumber,
			int factorRecordLinkCallNumber) {
		dataFactory.MCR.assertParameters("factorRecordLinkUsingNameInDataAndTypeAndId",
				factorRecordLinkCallNumber, SECRET_LINK_NAME_IN_DATA, SYSTEM_SECRET_TYPE,
				systemSecretId);

		var secretLink = dataFactory.MCR.getReturnValue(
				"factorRecordLinkUsingNameInDataAndTypeAndId", factorRecordLinkCallNumber);

		dataFactory.MCR.assertParameters("factorGroupUsingNameInData", factoredGroupCallNumber,
				PASSWORD_GROUP_NAME_IN_DATA);
		DataGroupSpy passwordGroup = (DataGroupSpy) dataFactory.MCR
				.getReturnValue("factorGroupUsingNameInData", factoredGroupCallNumber);

		rGroupMCR.assertParameters("addChild", 0, passwordGroup);
		passwordGroup.MCR.assertParameters("addChild", 0, secretLink);
	}

	@Test
	public void testNewPasswordWithoutPreviousPassword() throws Exception {
		LocalDateTime dateTimeBefore = whatTimeIsIt().minus(2, ChronoUnit.SECONDS);

		extended.useExtendedFunctionality(efData);

		LocalDateTime dateTimeAfter = whatTimeIsIt().plus(2, ChronoUnit.SECONDS);

		DataGroupSpy passwordGroup = assertAndReturnPasswordGroupCreatedInUser();
		assertTsUpdatedCreatedNowAndAddedToPasswordGroup(2, 1, passwordGroup, dateTimeBefore,
				dateTimeAfter);
	}

	private DataGroupSpy assertAndReturnPasswordGroupCreatedInUser() {
		int factoredGroupCallNumber = 2;
		dataFactory.MCR.assertParameters("factorGroupUsingNameInData", factoredGroupCallNumber,
				PASSWORD_GROUP_NAME_IN_DATA);
		DataGroupSpy passwordGroup = (DataGroupSpy) dataFactory.MCR
				.getReturnValue("factorGroupUsingNameInData", factoredGroupCallNumber);
		rGroupMCR.assertParameters("addChild", 0, passwordGroup);
		return passwordGroup;
	}

	private void assertTsUpdatedCreatedNowAndAddedToPasswordGroup(int factoredAtomicCallNumber,
			int updatedAddedAsChildNo, DataGroupSpy passwordGroup, LocalDateTime dateTimeBefore,
			LocalDateTime dateTimeAfter) {
		dataFactory.MCR.assertParameters("factorAtomicUsingNameInDataAndValue",
				factoredAtomicCallNumber, "tsPasswordUpdated");
		String tsUpdatedValue = (String) dataFactory.MCR
				.getValueForMethodNameAndCallNumberAndParameterName(
						"factorAtomicUsingNameInDataAndValue", factoredAtomicCallNumber, "value");

		LocalDateTime updatedDateTime = LocalDateTime.parse(tsUpdatedValue, dateTimePattern);
		assertTrue(dateTimeBefore.isBefore(updatedDateTime));
		assertTrue(dateTimeAfter.isAfter(updatedDateTime));

		DataAtomicSpy tsUpdated = (DataAtomicSpy) dataFactory.MCR
				.getReturnValue("factorAtomicUsingNameInDataAndValue", factoredAtomicCallNumber);
		passwordGroup.MCR.assertParameters("addChild", updatedAddedAsChildNo, tsUpdated);
	}

	private LocalDateTime whatTimeIsIt() {
		return LocalDateTime.now();
	}

	private String setUpSpiesForCreateReturningDataRecordWithTsUpdated() {
		RecordCreatorSpy recordCreatorSpy = new RecordCreatorSpy();
		spiderInstanceFactory.MRV.setReturnValues("factorRecordCreator", List.of(recordCreatorSpy));
		DataRecordSpy dataRecord = new DataRecordSpy();
		recordCreatorSpy.MRV.setDefaultReturnValuesSupplier("createAndStoreRecord",
				(Supplier<DataRecordSpy>) () -> dataRecord);

		DataGroupSpy recordGroup = new DataGroupSpy();
		dataRecord.MRV.setReturnValues("getDataGroup", List.of(recordGroup));

		DataGroupSpy recordInfo = new DataGroupSpy();
		recordGroup.MRV.setReturnValues("getFirstGroupWithNameInData", List.of(recordInfo),
				"recordInfo");
		DataGroupSpy updatedGroup = new DataGroupSpy();
		List<DataGroupSpy> updatedList = List.of(new DataGroupSpy(), updatedGroup);
		recordInfo.MRV.setReturnValues("getAllGroupsWithNameInData", List.of(updatedList),
				"updated");

		String tsUpdated = "2018-11-29T13:55:55.827000Z";
		updatedGroup.MRV.setReturnValues("getFirstAtomicValueWithNameInData", List.of(tsUpdated),
				"tsUpdated");
		return tsUpdated;
	}

	@Test
	public void testUpdatePasswordExistingPassword() throws Exception {
		setUpSpiesForUpdateReturningDataRecordWithTsUpdated();

		LocalDateTime dateTimeBefore = whatTimeIsIt().minus(2, ChronoUnit.SECONDS);

		extended.useExtendedFunctionality(efData);

		LocalDateTime dateTimeAfter = whatTimeIsIt().plus(2, ChronoUnit.SECONDS);

		assertPlainTextHashed();
		spiderInstanceFactory.MCR.assertMethodNotCalled("factorRecordCreator");

		DataGroupSpy passwordGroup = (DataGroupSpy) dataRecordGroup
				.getFirstGroupWithNameInData(PASSWORD_GROUP_NAME_IN_DATA);
		DataRecordLinkSpy passwordLink = (DataRecordLinkSpy) passwordGroup
				.getFirstChildWithNameInData(SECRET_LINK_NAME_IN_DATA);
		String systemSecretId = passwordLink.getLinkedRecordId();

		RecordStorageSpy recordStorage = (RecordStorageSpy) dependencyProvider.MCR
				.getReturnValue("getRecordStorage", 0);

		List<?> types = (List<?>) recordStorage.MCR
				.getValueForMethodNameAndCallNumberAndParameterName("read", 0, "types");
		assertEquals(types.get(0), "systemSecret");
		assertEquals(types.size(), 1);
		recordStorage.MCR.assertParameter("read", 0, "id", systemSecretId);

		DataGroupSpy secretFromStorage = (DataGroupSpy) recordStorage.MCR.getReturnValue("read", 0);

		secretFromStorage.MCR.assertParameters("removeAllChildrenWithNameInData", 0, SECRET);
		assertHashedPasswordIsAddedToGroupAsNumber(secretFromStorage, 0, 0);

		var dataDividerValue = dataDivider.MCR.getReturnValue("getLinkedRecordId", 0);

		recordStorage.MCR.assertParameters("update", 0, SYSTEM_SECRET_TYPE, systemSecretId,
				secretFromStorage, Collections.emptySet(), Collections.emptySet(),
				dataDividerValue);

		passwordGroup.MCR.assertParameters("removeAllChildrenWithNameInData", 0,
				"tsPasswordUpdated");
		assertTsUpdatedCreatedNowAndAddedToPasswordGroup(1, 0, passwordGroup, dateTimeBefore,
				dateTimeAfter);
	}

	private String setUpSpiesForUpdateReturningDataRecordWithTsUpdated() {
		rGroupMRV.setReturnValues("containsChildWithNameInData", List.of(true),
				PASSWORD_GROUP_NAME_IN_DATA);

		DataGroupSpy passwordGroup = new DataGroupSpy();
		rGroupMRV.setSpecificReturnValuesSupplier("getFirstGroupWithNameInData",
				() -> passwordGroup, PASSWORD_GROUP_NAME_IN_DATA);
		DataRecordLinkSpy passwordLink = new DataRecordLinkSpy();
		passwordGroup.MRV.setSpecificReturnValuesSupplier("getFirstChildWithNameInData",
				() -> passwordLink, SECRET_LINK_NAME_IN_DATA);

		passwordLink.MRV.setSpecificReturnValuesSupplier("getLinkedRecordId",
				(Supplier<String>) () -> "passwordLinkId");

		// rGroupMRV.setSpecificReturnValuesSupplier("getFirstChildWithNameInData",
		// (Supplier<DataRecordLinkSpy>) () -> passwordLink, SECRET_LINK_NAME_IN_DATA);

		RecordReaderSpy recordReaderSpy = new RecordReaderSpy();
		spiderInstanceFactory.MRV.setReturnValues("factorRecordReader", List.of(recordReaderSpy));
		DataRecordSpy readDataRecord = new DataRecordSpy();
		recordReaderSpy.MRV.setDefaultReturnValuesSupplier("readRecord",
				(Supplier<DataRecordSpy>) () -> readDataRecord);

		DataGroupSpy readRecordGroup = new DataGroupSpy();
		readDataRecord.MRV.setReturnValues("getDataGroup", List.of(readRecordGroup));

		RecordUpdaterSpy recordUpdaterSpy = new RecordUpdaterSpy();
		spiderInstanceFactory.MRV.setReturnValues("factorRecordUpdater", List.of(recordUpdaterSpy));

		DataRecordSpy updatedDataRecord = new DataRecordSpy();
		recordUpdaterSpy.MRV.setDefaultReturnValuesSupplier("updateRecord",
				(Supplier<DataRecordSpy>) () -> updatedDataRecord);

		DataGroupSpy updatedRecordGroup = new DataGroupSpy();
		updatedDataRecord.MRV.setReturnValues("getDataGroup", List.of(updatedRecordGroup));

		DataGroupSpy updatedRecordInfo = new DataGroupSpy();
		updatedRecordGroup.MRV.setReturnValues("getFirstGroupWithNameInData",
				List.of(updatedRecordInfo), "recordInfo");

		DataGroupSpy updatedGroup = new DataGroupSpy();
		List<DataGroupSpy> updatedList = List.of(new DataGroupSpy(), updatedGroup);
		updatedRecordInfo.MRV.setReturnValues("getAllGroupsWithNameInData", List.of(updatedList),
				"updated");

		String tsUpdated = "2022-04-23T13:55:55.827000Z";
		updatedGroup.MRV.setReturnValues("getFirstAtomicValueWithNameInData", List.of(tsUpdated),
				"tsUpdated");
		return tsUpdated;
	}
}
