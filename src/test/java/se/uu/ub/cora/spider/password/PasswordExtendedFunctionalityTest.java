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

import static org.testng.Assert.assertEquals;

import java.util.List;
import java.util.function.Supplier;

import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import se.uu.ub.cora.data.DataProvider;
import se.uu.ub.cora.password.texthasher.TextHasher;
import se.uu.ub.cora.spider.dependency.SpiderDependencyProvider;
import se.uu.ub.cora.spider.dependency.SpiderInstanceProvider;
import se.uu.ub.cora.spider.dependency.spy.SpiderDependencyProviderSpy;
import se.uu.ub.cora.spider.extendedfunctionality.ExtendedFunctionalityData;
import se.uu.ub.cora.testspies.data.DataFactorySpy;
import se.uu.ub.cora.testspies.data.DataGroupSpy;
import se.uu.ub.cora.testspies.data.DataRecordLinkSpy;
import se.uu.ub.cora.testspies.data.DataRecordSpy;
import se.uu.ub.cora.testspies.spider.RecordCreatorSpy;
import se.uu.ub.cora.testspies.spider.RecordReaderSpy;
import se.uu.ub.cora.testspies.spider.RecordUpdaterSpy;
import se.uu.ub.cora.testspies.spider.SpiderInstanceFactorySpy;
import se.uu.ub.cora.testutils.mcr.MethodCallRecorder;
import se.uu.ub.cora.testutils.mrv.MethodReturnValues;

public class PasswordExtendedFunctionalityTest {
	private static final String PASSWORD = "password";
	private static final String SYSTEM_SECRET_TYPE = "systemSecret";
	SpiderDependencyProvider dependencyProvider;
	TextHasherSpy textHasher;
	PasswordExtendedFunctionality extended;
	private ExtendedFunctionalityData efData;

	private DataFactorySpy dataFactory;
	private DataGroupSpy dataRecordGroup;
	private MethodCallRecorder rGroupMCR;
	private MethodReturnValues rGroupMRV;
	private DataRecordLinkSpy dataDivider;
	private SpiderInstanceFactorySpy spiderInstanceFactory;

	@BeforeMethod
	public void beforeMethod() {
		dataFactory = new DataFactorySpy();
		DataProvider.onlyForTestSetDataFactory(dataFactory);

		dependencyProvider = new SpiderDependencyProviderSpy(null);
		textHasher = new TextHasherSpy();
		extended = PasswordExtendedFunctionality
				.usingDependencyProviderAndTextHasher(dependencyProvider, textHasher);
		setupSpyForDataRecordGroup();
		createExtendedFunctionallityData();

		spiderInstanceFactory = new SpiderInstanceFactorySpy();
		SpiderInstanceProvider.setSpiderInstanceFactory(spiderInstanceFactory);

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

	private void createExtendedFunctionallityData() {
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
	public void testIfClearTextPasswordIsNotPresentNoGetOrRemove() throws Exception {
		rGroupMRV.setReturnValues("containsChildWithNameInData", List.of(false),
				"plainTextPassword");

		extended.useExtendedFunctionality(efData);

		rGroupMCR.assertParameters("containsChildWithNameInData", 0, "plainTextPassword");
		rGroupMCR.assertMethodNotCalled("getFirstAtomicValueWithNameInData");
		rGroupMCR.assertMethodNotCalled("removeAllChildrenWithNameInData");
		dataFactory.MCR.assertMethodNotCalled("factorGroupUsingNameInData");
	}

	@Test
	public void testClearTextPasswordIsRemovedFromDataRecordGroupCreate() throws Exception {
		setUpSpiesForCreateReturningDataRecordWithTsUpdated();

		extended.useExtendedFunctionality(efData);

		rGroupMCR.assertParameters("containsChildWithNameInData", 0, "plainTextPassword");
		rGroupMCR.assertParameters("getFirstAtomicValueWithNameInData", 0, "plainTextPassword");
		rGroupMCR.assertParameters("removeAllChildrenWithNameInData", 0, "plainTextPassword");
	}

	@Test
	public void testClearTextPasswordIsRemovedFromDataRecordGroupUpdate() throws Exception {
		setUpSpiesForUpdateReturningDataRecordWithTsUpdated();

		extended.useExtendedFunctionality(efData);

		rGroupMCR.assertParameters("containsChildWithNameInData", 0, "plainTextPassword");
		rGroupMCR.assertParameters("getFirstAtomicValueWithNameInData", 0, "plainTextPassword");
		rGroupMCR.assertParameters("removeAllChildrenWithNameInData", 0, "plainTextPassword");
	}

	@Test
	public void testClearTextPasswordIsHashed() throws Exception {
		setUpSpiesForCreateReturningDataRecordWithTsUpdated();

		extended.useExtendedFunctionality(efData);

		assertPlainTextHashed();
	}

	private void assertPlainTextHashed() {
		var plainTextPassword = rGroupMCR.getReturnValue("getFirstAtomicValueWithNameInData", 0);
		textHasher.MCR.assertParameters("hashText", 0, plainTextPassword);
	}

	@Test
	public void testCreateNewPasswordRecord() throws Exception {
		setUpSpiesForCreateReturningDataRecordWithTsUpdated();
		rGroupMRV.setReturnValues("containsChildWithNameInData", List.of(false), "passwordLink");

		extended.useExtendedFunctionality(efData);

		dataFactory.MCR.assertParameters("factorGroupUsingNameInData", 0, SYSTEM_SECRET_TYPE);
		DataGroupSpy secret = (DataGroupSpy) dataFactory.MCR
				.getReturnValue("factorGroupUsingNameInData", 0);

		assertRecordInfoWithDataDividerAddedTo(secret);

		assertHashedPasswordIsAddedToGroupAsNumber(secret, 1);

	}

	private void assertRecordInfoWithDataDividerAddedTo(DataGroupSpy secret) {
		dataFactory.MCR.assertParameters("factorGroupUsingNameInData", 1, "recordInfo");
		DataGroupSpy recordInfo = (DataGroupSpy) dataFactory.MCR
				.getReturnValue("factorGroupUsingNameInData", 1);
		secret.MCR.assertParameters("addChild", 0, recordInfo);

		String dataDividerValue = (String) dataDivider.MCR.getReturnValue("getLinkedRecordId", 0);
		dataFactory.MCR.assertParameters("factorRecordLinkUsingNameInDataAndTypeAndId", 0,
				"dataDivider", "system", dataDividerValue);

		DataRecordLinkSpy dataDivider = (DataRecordLinkSpy) dataFactory.MCR
				.getReturnValue("factorRecordLinkUsingNameInDataAndTypeAndId", 0);
		recordInfo.MCR.assertParameters("addChild", 0, dataDivider);
	}

	private void assertHashedPasswordIsAddedToGroupAsNumber(DataGroupSpy secret, int addedAsNo) {
		var hashedPassword = textHasher.MCR.getReturnValue("hashText", 0);
		dataFactory.MCR.assertParameters("factorAtomicUsingNameInDataAndValue", 0, "secret",
				hashedPassword);
		var hashedPasswordAtomic = dataFactory.MCR
				.getReturnValue("factorAtomicUsingNameInDataAndValue", 0);
		secret.MCR.assertParameters("addChild", addedAsNo, hashedPasswordAtomic);
	}

	@Test
	public void testSystemSecretIsCreatedForNewPassword() throws Exception {
		setUpSpiesForCreateReturningDataRecordWithTsUpdated();

		extended.useExtendedFunctionality(efData);

		DataGroupSpy secret = (DataGroupSpy) dataFactory.MCR
				.getReturnValue("factorGroupUsingNameInData", 0);
		RecordCreatorSpy recordCreator = (RecordCreatorSpy) spiderInstanceFactory.MCR
				.getReturnValue("factorRecordCreator", 0);
		recordCreator.MCR.assertParameters("createAndStoreRecord", 0, efData.authToken,
				SYSTEM_SECRET_TYPE, secret);
	}

	@Test
	public void testUserUpdatedWithInformationAboutNewPassword() throws Exception {
		String tsUpdated = setUpSpiesForCreateReturningDataRecordWithTsUpdated();

		extended.useExtendedFunctionality(efData);

		RecordCreatorSpy recordCreator = (RecordCreatorSpy) spiderInstanceFactory.MCR
				.getReturnValue("factorRecordCreator", 0);
		DataRecordSpy secretRecord = (DataRecordSpy) recordCreator.MCR
				.getReturnValue("createAndStoreRecord", 0);
		var secretId = secretRecord.MCR.getReturnValue("getId", 0);

		dataFactory.MCR.assertParameters("factorRecordLinkUsingNameInDataAndTypeAndId", 1, PASSWORD,
				SYSTEM_SECRET_TYPE, secretId);
		var secretLink = dataFactory.MCR
				.getReturnValue("factorRecordLinkUsingNameInDataAndTypeAndId", 1);
		rGroupMCR.assertParameters("addChild", 0, secretLink);

		DataGroupSpy secretGroup = (DataGroupSpy) secretRecord.MCR.getReturnValue("getDataGroup",
				0);

		secretGroup.MCR.assertParameter("getFirstGroupWithNameInData", 0, "nameInData",
				"recordInfo");
		DataGroupSpy recordInfoGroup = (DataGroupSpy) secretGroup.MCR
				.getReturnValue("getFirstGroupWithNameInData", 0);
		recordInfoGroup.MCR.assertParameter("getAllGroupsWithNameInData", 0, "nameInData",
				"updated");
		int assertChildCall = 1;
		assertCreateAtomicTsUpdatedPassword(tsUpdated, assertChildCall);

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
	public void testUpdatePassword() throws Exception {
		setUpSpiesForUpdateReturningDataRecordWithTsUpdated();

		extended.useExtendedFunctionality(efData);

		assertPlainTextHashed();
		spiderInstanceFactory.MCR.assertMethodNotCalled("factorRecordCreator");

		RecordReaderSpy recordReader = (RecordReaderSpy) spiderInstanceFactory.MCR
				.getReturnValue("factorRecordReader", 0);

		DataRecordLinkSpy passwordLink = (DataRecordLinkSpy) rGroupMCR
				.getReturnValue("getFirstChildWithNameInData", 0);

		recordReader.MCR.assertParameters("readRecord", 0, efData.authToken, SYSTEM_SECRET_TYPE,
				passwordLink.getLinkedRecordId());

		DataRecordSpy systemSecret = (DataRecordSpy) recordReader.MCR.getReturnValue("readRecord",
				0);

		DataGroupSpy systemSecretG = (DataGroupSpy) systemSecret.MCR.getReturnValue("getDataGroup",
				0);
		systemSecretG.MCR.assertParameters("removeAllChildrenWithNameInData", 0, PASSWORD);
		assertHashedPasswordIsAddedToGroupAsNumber(systemSecretG, 0);

		RecordUpdaterSpy recordUpdater = (RecordUpdaterSpy) spiderInstanceFactory.MCR
				.getReturnValue("factorRecordUpdater", 0);

		recordUpdater.MCR.assertParameters("updateRecord", 0, efData.authToken, SYSTEM_SECRET_TYPE,
				passwordLink.getLinkedRecordId(), systemSecretG);

		String tsUpdated = "2022-04-23T13:55:55.827000Z";

		rGroupMCR.assertParameters("removeAllChildrenWithNameInData", 1, "tsPasswordUpdated");
		int assertChildCall = 0;
		assertCreateAtomicTsUpdatedPassword(tsUpdated, assertChildCall);

	}

	private void assertCreateAtomicTsUpdatedPassword(String tsUpdated, int assertChildCall) {
		dataFactory.MCR.assertParameters("factorAtomicUsingNameInDataAndValue", 1,
				"tsPasswordUpdated", tsUpdated);
		var tsUpdatedAtomic = dataFactory.MCR.getReturnValue("factorAtomicUsingNameInDataAndValue",
				1);
		rGroupMCR.assertParameters("addChild", assertChildCall, tsUpdatedAtomic);
	}

	private String setUpSpiesForUpdateReturningDataRecordWithTsUpdated() {
		rGroupMRV.setReturnValues("containsChildWithNameInData", List.of(true), "passwordLink");

		DataRecordLinkSpy passwordLink = new DataRecordLinkSpy();

		passwordLink.MRV.setSpecificReturnValuesSupplier("getLinkedRecordId",
				(Supplier<String>) () -> "passwordLinkId");

		rGroupMRV.setSpecificReturnValuesSupplier("getFirstChildWithNameInData",
				(Supplier<DataRecordLinkSpy>) () -> passwordLink, "passwordLink");

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
