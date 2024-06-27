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

package se.uu.ub.cora.spider.extended.apptoken;

import static org.testng.Assert.assertTrue;

import java.util.ArrayList;
import java.util.List;

import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import se.uu.ub.cora.data.DataGroup;
import se.uu.ub.cora.data.DataProvider;
import se.uu.ub.cora.data.DataRecordLink;
import se.uu.ub.cora.data.spies.DataFactorySpy;
import se.uu.ub.cora.data.spies.DataGroupSpy;
import se.uu.ub.cora.data.spies.DataRecordGroupSpy;
import se.uu.ub.cora.data.spies.DataRecordLinkSpy;
import se.uu.ub.cora.spider.extendedfunctionality.ExtendedFunctionality;
import se.uu.ub.cora.spider.extendedfunctionality.ExtendedFunctionalityData;
import se.uu.ub.cora.spider.spy.AppTokenGeneratorSpy;
import se.uu.ub.cora.spider.spy.SystemSecretOperationsSpy;

public class AppTokenHandlerExtendedFunctionalityTest {

	private static final String SOME_SECRET = "someSecret";
	private static final String SOME_DATA_DIVIDER = "someDataDivider";
	private DataRecordGroupSpy previousDataGroup;
	private DataRecordGroupSpy currentDataGroup;
	private ExtendedFunctionalityData efData;
	private AppTokenHandlerExtendedFunctionality appTokenHandler;
	private DataRecordLinkSpy appTokenLink1;
	private DataRecordLinkSpy appTokenLink2;
	private DataRecordLinkSpy appTokenLink3;
	private DataRecordLinkSpy dataDivider;
	// private SpiderDependencyProviderSpy dependencyProvider;
	// private RecordStorageSpy recordStorage;
	private SystemSecretOperationsSpy systemSecretOperations;
	private AppTokenGeneratorSpy appTokenGenerator;
	private DataGroupSpy previousAppTokensGroup;
	private DataGroupSpy currentAppTokensGroup;
	private DataFactorySpy dataFactory;

	@BeforeMethod
	public void beforeMethod() {
		dataFactory = new DataFactorySpy();
		DataProvider.onlyForTestSetDataFactory(dataFactory);

		// recordStorage = new RecordStorageSpy();
		//
		// dependencyProvider = new SpiderDependencyProviderSpy();
		// dependencyProvider.MRV.setDefaultReturnValuesSupplier("getRecordStorage",
		// () -> recordStorage);

		// textHasher = new TextHasherSpy();
		// extended = PasswordExtendedFunctionality
		// .usingDependencyProviderAndTextHasher(dependencyProvider, textHasher);
		previousDataGroup = new DataRecordGroupSpy();
		currentDataGroup = new DataRecordGroupSpy();

		// setupSpyForDataRecordGroup();
		createExtendedFunctionalityData();

		//
		// spiderInstanceFactory = new SpiderInstanceFactorySpy();
		// SpiderInstanceProvider.setSpiderInstanceFactory(spiderInstanceFactory);
		//
		// RecordStorageSpy recordStorage = (RecordStorageSpy) dependencyProvider.recordStorage;
		// recordStorage.MRV.setDefaultReturnValuesSupplier("read", DataGroupSpy::new);
		//
		// recordIdGeneratorSpy = new RecordIdGeneratorSpy();
		// dependencyProvider.recordIdGenerator = recordIdGeneratorSpy;

		previousAppTokensGroup = new DataGroupSpy();
		currentAppTokensGroup = new DataGroupSpy();

		// DataGroupSpy appTokenGroup1 = new DataGroupSpy();
		// DataGroupSpy appTokenGroup2 = new DataGroupSpy();
		// DataGroupSpy appTokenGroup3 = new DataGroupSpy();

		systemSecretOperations = new SystemSecretOperationsSpy();
		appTokenGenerator = new AppTokenGeneratorSpy();

		setupDataDividerForDataRecordGroup();

		appTokenHandler = new AppTokenHandlerExtendedFunctionality(appTokenGenerator,
				systemSecretOperations);

	}

	private void createExtendedFunctionalityData() {
		efData = new ExtendedFunctionalityData();
		efData.dataRecordGroup = currentDataGroup;
		efData.previouslyStoredDataRecordGroup = previousDataGroup;

	}

	private void setupDataDividerForDataRecordGroup() {
		DataGroupSpy recordInfo = new DataGroupSpy();
		currentDataGroup.MRV.setReturnValues("getFirstGroupWithNameInData", List.of(recordInfo),
				"recordInfo");
		dataDivider = new DataRecordLinkSpy();
		recordInfo.MRV.setSpecificReturnValuesSupplier("getFirstChildWithNameInData",
				() -> dataDivider, "dataDivider");
		dataDivider.MRV.setDefaultReturnValuesSupplier("getLinkedRecordId",
				() -> SOME_DATA_DIVIDER);
	}

	@Test
	public void testIsExtendedFunctionality() throws Exception {
		assertTrue(appTokenHandler instanceof ExtendedFunctionality);
	}

	@Test
	public void testIncommingAppTokensGroupDoNotExists() throws Exception {
		setupAppTokensGroupsUsingAppTokenGroups(currentDataGroup, currentAppTokensGroup);

		appTokenHandler.useExtendedFunctionality(efData);

		currentDataGroup.MCR.assertParameters("containsChildOfTypeAndName", 0, DataGroup.class,
				"appTokens");
		assertNoApptokenCreated();
		assertNotCalledRemoveAppTokensGroup();
	}

	private void assertNoApptokenCreated() {
		appTokenGenerator.MCR.assertMethodNotCalled("generateAppToken");
		systemSecretOperations.MCR.assertMethodNotCalled("createAndStoreSystemSecretRecord");
	}

	private void assertNotCalledRemoveAppTokensGroup() {
		currentDataGroup.MCR.assertMethodNotCalled("removeFirstChildWithNameInData");
	}

	@Test
	public void testIncommingAppTokensGroupExists() throws Exception {
		DataGroupSpy appTokenGroup1 = createAppTokenGroupWithNewAppToken("someNewNoteAppToken1");
		setupAppTokensGroupsUsingAppTokenGroups(currentDataGroup, currentAppTokensGroup,
				appTokenGroup1);

		appTokenHandler.useExtendedFunctionality(efData);

		assertAppTokenGroups();
		currentAppTokensGroup.MCR.assertParameters("removeAllChildrenWithNameInData", 0,
				"appToken");
		assertNotCalledRemoveAppTokensGroup();
	}

	private void assertAppTokenGroups() {
		currentDataGroup.MCR.assertParameters("containsChildOfTypeAndName", 0, DataGroup.class,
				"appTokens");
		DataGroupSpy appTokensGroup = (DataGroupSpy) currentDataGroup.MCR
				.assertCalledParametersReturn("getFirstGroupWithNameInData", "appTokens");
		appTokensGroup.MCR.assertParameters("getChildrenOfTypeAndName", 0, DataGroup.class,
				"appToken");
	}

	private void assertRemoveAppTokensGroupWhenNoAppTokenExists() {
		currentDataGroup.MCR.assertParameters("removeFirstChildWithNameInData", 0, "appTokens");
		currentDataGroup.MCR.assertNumberOfCallsToMethod("removeFirstChildWithNameInData", 1);
	}

	@Test
	public void testIncommingTwoNewAppTokens() throws Exception {
		DataGroupSpy appTokenGroup1 = createAppTokenGroupWithNewAppToken("AppToken1");
		DataGroupSpy appTokenGroup2 = createAppTokenGroupWithNewAppToken("AppToken2");
		setupAppTokensGroupsUsingAppTokenGroups(currentDataGroup, currentAppTokensGroup,
				appTokenGroup1, appTokenGroup2);

		appTokenHandler.useExtendedFunctionality(efData);

		assertNewAppToken(appTokenGroup1, 0);
		assertNewAppToken(appTokenGroup2, 1);
		currentAppTokensGroup.MCR.assertNumberOfCallsToMethod("addChild", 2);
		assertNotCalledRemoveAppTokensGroup();
	}

	private DataGroupSpy createAppTokenGroupWithNewAppToken(String postfix) {
		DataGroupSpy appTokenGroup = new DataGroupSpy();
		appTokenGroup.MRV.setDefaultReturnValuesSupplier("note", () -> "someNote" + postfix);
		return appTokenGroup;
	}

	private void setupAppTokensGroupsUsingAppTokenGroups(DataRecordGroupSpy userDataGroup,
			DataGroupSpy appTokensGroup, DataGroupSpy... appTokenGroups) {

		if (appTokenGroups.length > 0) {
			userDataGroup.MRV.setSpecificReturnValuesSupplier("containsChildOfTypeAndName",
					() -> true, DataGroup.class, "appTokens");
			userDataGroup.MRV.setSpecificReturnValuesSupplier("getFirstGroupWithNameInData",
					() -> appTokensGroup, "appTokens");

			List<DataGroup> appTokensGroups = new ArrayList<>();
			for (DataGroupSpy appTokenGroup : appTokenGroups) {
				appTokensGroups.add(appTokenGroup);
			}
			appTokensGroup.MRV.setDefaultReturnValuesSupplier("getChildrenOfTypeAndName",
					() -> appTokensGroups);
		}
	}

	private void assertNewAppToken(DataGroupSpy appTokenGroup, int callNr) {
		var genreatedAppToken = appTokenGenerator.MCR.getReturnValue("generateAppToken", callNr);
		var systemSecretId = systemSecretOperations.MCR.assertCalledParametersReturn(
				"createAndStoreSystemSecretRecord", genreatedAppToken, SOME_DATA_DIVIDER);
		var appTokenLink = assertRecordLinkCreationAndReturn(callNr, systemSecretId);
		appTokenGroup.MCR.assertParameters("addChild", 0, appTokenLink);
	}

	private Object assertRecordLinkCreationAndReturn(int callNr, Object systemSecretId) {
		dataFactory.MCR.assertParameters("factorRecordLinkUsingNameInDataAndTypeAndId", callNr,
				"appTokenLink", "systemSecret", systemSecretId);
		var appTokenLink = dataFactory.MCR
				.getReturnValue("factorRecordLinkUsingNameInDataAndTypeAndId", callNr);
		return appTokenLink;
	}

	@Test
	public void testIncomingAppTokensWithAppTokenLinksDoNotExistPreviously() throws Exception {
		DataGroupSpy appTokenGroup1 = createAppTokenGroupWithNoteAndAppTokenLink("AppToken1");
		DataGroupSpy appTokenGroup2 = createAppTokenGroupWithNoteAndAppTokenLink("AppToken2");
		setupAppTokensGroupsUsingAppTokenGroups(currentDataGroup, currentAppTokensGroup,
				appTokenGroup1, appTokenGroup2);

		appTokenHandler.useExtendedFunctionality(efData);

		currentAppTokensGroup.MCR.assertNumberOfCallsToMethod("addChild", 0);
		assertRemoveAppTokensGroupWhenNoAppTokenExists();
	}

	private DataGroupSpy createAppTokenGroupWithNoteAndAppTokenLink(String postfix) {
		DataGroupSpy appTokenGroup = new DataGroupSpy();
		appTokenGroup.MRV.setDefaultReturnValuesSupplier("note", () -> "someNote" + postfix);
		appTokenGroup.MRV.setSpecificReturnValuesSupplier("containsChildWithNameInData", () -> true,
				"appTokenLink");
		DataRecordLinkSpy appTokenLink = new DataRecordLinkSpy();
		appTokenLink.MRV.setDefaultReturnValuesSupplier("getLinkedRecordId",
				() -> "someSystemSecret" + postfix);
		appTokenGroup.MRV.setSpecificReturnValuesSupplier("getFirstChildOfTypeAndName",
				() -> appTokenLink, DataRecordLink.class, "appTokenLink");
		return appTokenGroup;
	}

	@Test
	public void testIncomingAppTokensWithAppTokenLinksDoExistPreviously() throws Exception {
		DataGroupSpy appTokenGroup1 = createAppTokenGroupWithNoteAndAppTokenLink("AppToken1");
		DataGroupSpy appTokenGroup2 = createAppTokenGroupWithNoteAndAppTokenLink("AppToken2");
		setupAppTokensGroupsUsingAppTokenGroups(previousDataGroup, previousAppTokensGroup,
				appTokenGroup1, appTokenGroup2);
		setupAppTokensGroupsUsingAppTokenGroups(currentDataGroup, currentAppTokensGroup,
				appTokenGroup1, appTokenGroup2);

		appTokenHandler.useExtendedFunctionality(efData);

		appTokenGroup1.MCR.assertParameters("getFirstChildOfTypeAndName", 0, DataRecordLink.class,
				"appTokenLink");

		currentAppTokensGroup.MCR.assertNumberOfCallsToMethod("addChild", 2);
		assertNotCalledRemoveAppTokensGroup();

	}
}
