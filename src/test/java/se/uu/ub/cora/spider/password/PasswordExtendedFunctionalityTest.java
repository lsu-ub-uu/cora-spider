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
import se.uu.ub.cora.testspies.spider.SpiderInstanceFactorySpy;
import se.uu.ub.cora.testutils.mcr.MethodCallRecorder;
import se.uu.ub.cora.testutils.mrv.MethodReturnValues;

public class PasswordExtendedFunctionalityTest {
	private static final String SYSTEM_SECRET_TYPE = "systemSecret";
	SpiderDependencyProvider dependencyProvider;
	TextHasherSpy textHasher;
	PasswordExtendedFunctionality extended;
	private ExtendedFunctionalityData exData;

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
		rGroupMRV.setDefaultReturnValuesSupplier("containsChildWithNameInData",
				(Supplier<Boolean>) () -> true);

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
		exData = new ExtendedFunctionalityData();
		exData.dataGroup = dataRecordGroup;
		exData.authToken = "fakeToken";
		exData.recordType = "fakeType";
		exData.recordId = "fakeId";
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

		extended.useExtendedFunctionality(exData);

		rGroupMCR.assertParameters("containsChildWithNameInData", 0, "plainTextPassword");
		rGroupMCR.assertMethodNotCalled("getFirstAtomicValueWithNameInData");
		rGroupMCR.assertMethodNotCalled("removeAllChildrenWithNameInData");
		dataFactory.MCR.assertMethodNotCalled("factorGroupUsingNameInData");
	}

	@Test
	public void testClearTextPasswordIsRemovedFromDataRecordGroup() throws Exception {
		String tsUpdated = setUpSpiesForCreateReturningDataRecordWithTsUpdated();

		extended.useExtendedFunctionality(exData);

		rGroupMCR.assertParameters("containsChildWithNameInData", 0, "plainTextPassword");
		rGroupMCR.assertParameters("getFirstAtomicValueWithNameInData", 0, "plainTextPassword");
		rGroupMCR.assertParameters("removeAllChildrenWithNameInData", 0, "plainTextPassword");
	}

	@Test
	public void testClearTextPasswordIsHashed() throws Exception {
		String tsUpdated = setUpSpiesForCreateReturningDataRecordWithTsUpdated();

		extended.useExtendedFunctionality(exData);

		var plainTextPassword = rGroupMCR.getReturnValue("getFirstAtomicValueWithNameInData", 0);
		textHasher.MCR.assertParameters("hashText", 0, plainTextPassword);
	}

	@Test
	public void testCreateNewPasswordRecord() throws Exception {
		String tsUpdated = setUpSpiesForCreateReturningDataRecordWithTsUpdated();
		rGroupMRV.setReturnValues("containsChildWithNameInData", List.of(false), "passwordLink");

		extended.useExtendedFunctionality(exData);

		dataFactory.MCR.assertParameters("factorGroupUsingNameInData", 0, SYSTEM_SECRET_TYPE);
		DataGroupSpy secret = (DataGroupSpy) dataFactory.MCR
				.getReturnValue("factorGroupUsingNameInData", 0);

		assertRecordInfoWithDataDividerAddedTo(secret);

		assertHashedPasswordIsAddedTo(secret);

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

	private void assertHashedPasswordIsAddedTo(DataGroupSpy secret) {
		var hashedPassword = textHasher.MCR.getReturnValue("hashText", 0);
		dataFactory.MCR.assertParameters("factorAtomicUsingNameInDataAndValue", 0, "secret",
				hashedPassword);
		var hashedPasswordAtomic = dataFactory.MCR
				.getReturnValue("factorAtomicUsingNameInDataAndValue", 0);
		secret.MCR.assertParameters("addChild", 1, hashedPasswordAtomic);
	}

	@Test
	public void testSystemSecretIsCreatedForNewPassword() throws Exception {
		String tsUpdated = setUpSpiesForCreateReturningDataRecordWithTsUpdated();

		extended.useExtendedFunctionality(exData);

		DataGroupSpy secret = (DataGroupSpy) dataFactory.MCR
				.getReturnValue("factorGroupUsingNameInData", 0);
		RecordCreatorSpy recordCreator = (RecordCreatorSpy) spiderInstanceFactory.MCR
				.getReturnValue("factorRecordCreator", 0);
		recordCreator.MCR.assertParameters("createAndStoreRecord", 0, exData.authToken,
				SYSTEM_SECRET_TYPE, secret);
	}

	@Test
	public void testUserUpdatedWithInformationAboutNewPassword() throws Exception {
		String tsUpdated = setUpSpiesForCreateReturningDataRecordWithTsUpdated();

		extended.useExtendedFunctionality(exData);

		RecordCreatorSpy recordCreator = (RecordCreatorSpy) spiderInstanceFactory.MCR
				.getReturnValue("factorRecordCreator", 0);
		DataRecordSpy secretRecord = (DataRecordSpy) recordCreator.MCR
				.getReturnValue("createAndStoreRecord", 0);
		var secretId = secretRecord.MCR.getReturnValue("getId", 0);

		dataFactory.MCR.assertParameters("factorRecordLinkUsingNameInDataAndTypeAndId", 1,
				"password", SYSTEM_SECRET_TYPE, secretId);
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
		dataFactory.MCR.assertParameters("factorAtomicUsingNameInDataAndValue", 1,
				"tsPasswordUpdated", tsUpdated);
		var tsUpdatedAtomic = dataFactory.MCR.getReturnValue("factorAtomicUsingNameInDataAndValue",
				1);
		rGroupMCR.assertParameters("addChild", 1, tsUpdatedAtomic);

	}
	// check if user has filled out clearTextPassword

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

	// hash password
	// empty clearTextPw field

	// check if password instance exists
	// -- if not, create password instance, link to user

	// -- if true, update password hash in instance

}
