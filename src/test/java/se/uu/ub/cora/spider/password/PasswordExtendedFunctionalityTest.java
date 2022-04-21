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
import se.uu.ub.cora.spider.dependency.SpiderDependencyProviderSpy;
import se.uu.ub.cora.spider.extendedfunctionality.ExtendedFunctionalityData;
import se.uu.ub.cora.testspies.data.DataFactorySpy;
import se.uu.ub.cora.testspies.data.DataGroupSpy;
import se.uu.ub.cora.testspies.data.DataRecordLinkSpy;
import se.uu.ub.cora.testutils.mcr.MethodCallRecorder;
import se.uu.ub.cora.testutils.mrv.MethodReturnValues;

public class PasswordExtendedFunctionalityTest {
	SpiderDependencyProvider dependencyProvider;
	TextHasherSpy textHasher;
	PasswordExtendedFunctionality extended;
	private ExtendedFunctionalityData exData;

	private DataFactorySpy dataFactory;
	private DataGroupSpy dataRecordGroup;
	private MethodCallRecorder rGroupMCR;
	private MethodReturnValues rGroupMRV;
	private DataRecordLinkSpy dataDivider;

	@BeforeMethod
	public void beforeMethod() {
		dataFactory = new DataFactorySpy();
		DataProvider.onlyForTestSetDataFactory(dataFactory);

		dependencyProvider = new SpiderDependencyProviderSpy(null);
		textHasher = new TextHasherSpy();
		extended = PasswordExtendedFunctionality
				.usingDependencyProviderAndTextHasher(dependencyProvider, textHasher);
		exData = new ExtendedFunctionalityData();
		setupSpyForDataRecordGroup();
		exData.dataGroup = dataRecordGroup;
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
		extended.useExtendedFunctionality(exData);

		rGroupMCR.assertParameters("containsChildWithNameInData", 0, "plainTextPassword");
		rGroupMCR.assertParameters("getFirstAtomicValueWithNameInData", 0, "plainTextPassword");
		rGroupMCR.assertParameters("removeAllChildrenWithNameInData", 0, "plainTextPassword");
	}

	@Test
	public void testClearTextPasswordIsHashed() throws Exception {
		extended.useExtendedFunctionality(exData);

		var plainTextPassword = rGroupMCR.getReturnValue("getFirstAtomicValueWithNameInData", 0);
		textHasher.MCR.assertParameters("hashText", 0, plainTextPassword);
	}

	@Test
	public void testCreateNewPasswordRecord() throws Exception {
		rGroupMRV.setReturnValues("containsChildWithNameInData", List.of(false), "passwordLink");

		extended.useExtendedFunctionality(exData);

		dataFactory.MCR.assertParameters("factorGroupUsingNameInData", 0, "systemSecret");
		DataGroupSpy secret = (DataGroupSpy) dataFactory.MCR
				.getReturnValue("factorGroupUsingNameInData", 0);

		assertRecordInfoWithDataDividerAdded(secret);
	}

	private void assertRecordInfoWithDataDividerAdded(DataGroupSpy secret) {
		dataFactory.MCR.assertParameters("factorGroupUsingNameInData", 1, "recordInfo");
		DataGroupSpy recordInfo = (DataGroupSpy) dataFactory.MCR
				.getReturnValue("factorGroupUsingNameInData", 1);
		secret.MCR.assertParameters("addChild", 0, recordInfo);

		String dataDividerValue = (String) dataDivider.MCR.getReturnValue("getLinkedRecordId", 0);
		dataFactory.MCR.assertParameters("factorRecordLinkUsingNameInDataAndTypeAndId", 0,
				"dataDivider", "system", dataDividerValue);
	}
	// check if user has filled out clearTextPassword

	// hash password
	// empty clearTextPw field

	// check if password instance exists
	// -- if not, create password instance, link to user

	// -- if true, update password hash in instance

}
