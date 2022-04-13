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

import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import se.uu.ub.cora.password.texthasher.TextHasher;
import se.uu.ub.cora.spider.dependency.SpiderDependencyProvider;
import se.uu.ub.cora.spider.dependency.SpiderDependencyProviderSpy;
import se.uu.ub.cora.spider.extendedfunctionality.ExtendedFunctionalityData;
import se.uu.ub.cora.spider.record.DataGroupMCRSpy;

public class PasswordExtendedFunctionalityTest {
	SpiderDependencyProvider dependencyProvider;
	TextHasherSpy textHasher;
	PasswordExtendedFunctionality extended;
	private ExtendedFunctionalityData exData;
	private DataGroupMCRSpy dataRecordGroup;

	@BeforeMethod
	public void beforeMethod() {
		dependencyProvider = new SpiderDependencyProviderSpy(null);
		textHasher = new TextHasherSpy();
		extended = PasswordExtendedFunctionality
				.usingDependencyProviderAndTextHasher(dependencyProvider, textHasher);
		exData = new ExtendedFunctionalityData();
		dataRecordGroup = new DataGroupMCRSpy();
		exData.dataGroup = dataRecordGroup;
	}

	@Test
	public void testOnlyForTest() throws Exception {
		SpiderDependencyProvider returnedProvider = extended.onlyForTestGetDependencyProvider();
		assertEquals(returnedProvider, dependencyProvider);
		TextHasher returnedHasher = extended.onlyForTestGetTextHasher();
		assertEquals(returnedHasher, textHasher);
	}

	@Test
	public void testClearTextPasswordIsRemovedFromDataRecordGroup() throws Exception {
		dataRecordGroup.MRV.setReturnValues("containsChildWithNameInData", List.of(true),
				"plainTextPassword");

		extended.useExtendedFunctionality(exData);

		dataRecordGroup.MCR.assertParameters("containsChildWithNameInData", 0, "plainTextPassword");
		dataRecordGroup.MCR.assertParameters("getFirstAtomicValueWithNameInData", 0,
				"plainTextPassword");
		dataRecordGroup.MCR.assertParameters("removeAllChildrenWithNameInData", 0,
				"plainTextPassword");
	}

	@Test
	public void testIfClearTextPasswordIsNotPresentNoGetOrRemove() throws Exception {
		dataRecordGroup.MRV.setReturnValues("containsChildWithNameInData", List.of(false),
				"plainTextPassword");

		extended.useExtendedFunctionality(exData);

		dataRecordGroup.MCR.assertParameters("containsChildWithNameInData", 0, "plainTextPassword");
		dataRecordGroup.MCR.assertMethodNotCalled("getFirstAtomicValueWithNameInData");
		dataRecordGroup.MCR.assertMethodNotCalled("removeAllChildrenWithNameInData");
	}

	@Test
	public void testClearTextPasswordIsHashed() throws Exception {
		dataRecordGroup.MRV.setReturnValues("containsChildWithNameInData", List.of(true),
				"plainTextPassword");

		extended.useExtendedFunctionality(exData);

		var plainTextPassword = dataRecordGroup.MCR
				.getReturnValue("getFirstAtomicValueWithNameInData", 0);
		textHasher.MCR.assertParameters("hashText", 0, plainTextPassword);
	}

	@Test
	public void testName() throws Exception {
		dataRecordGroup.MRV.setReturnValues("containsChildWithNameInData", List.of(true),
				"plainTextPassword");
		dataRecordGroup.MRV.setReturnValues("containsChildWithNameInData", List.of(true),
				"passwordLink");

		extended.useExtendedFunctionality(exData);

	}
	// check if user has filled out clearTextPassword

	// hash password
	// empty clearTextPw field

	// check if password instance exists
	// -- if not, create password instance, link to user

	// -- if true, update password hash in instance

}
