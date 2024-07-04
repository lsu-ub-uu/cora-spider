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
package se.uu.ub.cora.spider.extended.password;

import static org.testng.Assert.assertSame;

import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import se.uu.ub.cora.data.DataRecordLink;
import se.uu.ub.cora.data.spies.DataGroupSpy;
import se.uu.ub.cora.data.spies.DataRecordLinkSpy;
import se.uu.ub.cora.spider.dependency.SpiderDependencyProvider;
import se.uu.ub.cora.spider.extendedfunctionality.ExtendedFunctionality;
import se.uu.ub.cora.spider.extendedfunctionality.ExtendedFunctionalityData;
import se.uu.ub.cora.spider.spy.SpiderDependencyProviderSpy;
import se.uu.ub.cora.spider.spy.SystemSecretOperationsSpy;
import se.uu.ub.cora.spider.systemsecret.SystemSecretOperations;
import se.uu.ub.cora.storage.spies.RecordStorageSpy;

public class PasswordSystemSecretRemoverExtendedFunctionalityTest {
	private static final String TRUE = "true";
	private static final String FALSE = "false";
	private SpiderDependencyProviderSpy dependencyProviderSpy;
	private ExtendedFunctionalityData exData;
	private ExtendedFunctionality exFunc;
	private DataGroupSpy previousGroup;
	private DataGroupSpy currentGroup;
	private RecordStorageSpy recordStorage;
	private SystemSecretOperationsSpy systemSecretOperations;

	@BeforeMethod
	private void beforeMethod() {
		dependencyProviderSpy = new SpiderDependencyProviderSpy();
		recordStorage = new RecordStorageSpy();
		dependencyProviderSpy.MRV.setDefaultReturnValuesSupplier("getRecordStorage",
				() -> recordStorage);
		systemSecretOperations = new SystemSecretOperationsSpy();

		exFunc = PasswordSystemSecretRemoverExtendedFunctionality
				.usingDependencyProviderAndSystemSecretOperations(dependencyProviderSpy,
						systemSecretOperations);
		exData = new ExtendedFunctionalityData();
		previousGroup = new DataGroupSpy();
		currentGroup = new DataGroupSpy();
	}

	@Test
	public void testPreviouslyPasswordFalseDoNothing() throws Exception {
		setUpPreviousPasswordWithValue(FALSE);
		setUpCurrentPasswordWithValue(TRUE);

		exFunc.useExtendedFunctionality(exData);

		recordStorage.MCR.assertMethodNotCalled("deleteByTypeAndId");
	}

	private void setUpPreviousPasswordWithValue(String usePasswordValue) {
		exData.previouslyStoredTopDataGroup = previousGroup;
		previousGroup.MRV.setSpecificReturnValuesSupplier("getFirstAtomicValueWithNameInData",
				() -> usePasswordValue, "usePassword");
	}

	private void setUpCurrentPasswordWithValue(String usePasswordValue) {
		exData.dataGroup = currentGroup;
		currentGroup.MRV.setSpecificReturnValuesSupplier("getFirstAtomicValueWithNameInData",
				() -> usePasswordValue, "usePassword");
	}

	@Test
	public void testPreviouslyPasswordTrueCurrentTrueDoNothing() throws Exception {
		setUpPreviousPasswordWithValue(TRUE);
		setUpCurrentPasswordWithValue(TRUE);

		exFunc.useExtendedFunctionality(exData);

		recordStorage.MCR.assertMethodNotCalled("deleteByTypeAndId");
	}

	@Test
	public void testPreviouslyPasswordTrueCurrentFalseRemoveSystemSecret() throws Exception {
		setUpPreviousPasswordWithValue(TRUE);
		setUpPreviousSystemSecretLink();
		setUpCurrentPasswordWithValue(FALSE);

		exFunc.useExtendedFunctionality(exData);

		systemSecretOperations.MCR.assertParameters("deleteSystemSecretFromStorage", 0,
				"systemSecretId");
	}

	private void setUpPreviousSystemSecretLink() {
		DataRecordLinkSpy systemSecretLink = new DataRecordLinkSpy();
		systemSecretLink.MRV.setDefaultReturnValuesSupplier("getLinkedRecordType",
				() -> "systemSecretType");
		systemSecretLink.MRV.setDefaultReturnValuesSupplier("getLinkedRecordId",
				() -> "systemSecretId");
		previousGroup.MRV.setSpecificReturnValuesSupplier("getFirstChildOfTypeAndName",
				() -> systemSecretLink, DataRecordLink.class, "passwordLink");
	}

	@Test
	public void testOnlyForTestGetDependencyProvider() throws Exception {
		SpiderDependencyProvider dependencyProvider = ((PasswordSystemSecretRemoverExtendedFunctionality) exFunc)
				.onlyForTestGetDependencyProvider();
		assertSame(dependencyProvider, dependencyProviderSpy);
	}

	@Test
	public void testOnlyForTestGetSystemSecretOperations() throws Exception {
		SystemSecretOperations readSystemSecretOperations = ((PasswordSystemSecretRemoverExtendedFunctionality) exFunc)
				.onlyForTestGetSystemSecretOperations();
		assertSame(readSystemSecretOperations, this.systemSecretOperations);
	}

}
