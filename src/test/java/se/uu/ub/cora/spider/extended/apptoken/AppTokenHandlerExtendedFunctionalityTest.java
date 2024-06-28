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

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertTrue;
import static org.testng.Assert.fail;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import se.uu.ub.cora.data.DataGroup;
import se.uu.ub.cora.data.DataProvider;
import se.uu.ub.cora.data.DataRecordLink;
import se.uu.ub.cora.data.spies.DataFactorySpy;
import se.uu.ub.cora.data.spies.DataGroupSpy;
import se.uu.ub.cora.data.spies.DataRecordLinkSpy;
import se.uu.ub.cora.spider.extendedfunctionality.ExtendedFunctionality;
import se.uu.ub.cora.spider.extendedfunctionality.ExtendedFunctionalityData;
import se.uu.ub.cora.spider.spy.AppTokenGeneratorSpy;
import se.uu.ub.cora.spider.spy.SystemSecretOperationsSpy;

public class AppTokenHandlerExtendedFunctionalityTest {

	private static final String SOME_DATA_DIVIDER = "someDataDivider";
	private DataGroupSpy previousDataGroup;
	private DataGroupSpy currentDataGroup;
	private ExtendedFunctionalityData efData;
	private AppTokenHandlerExtendedFunctionality appTokenHandler;
	private DataRecordLinkSpy dataDivider;
	private SystemSecretOperationsSpy systemSecretOperations;
	private AppTokenGeneratorSpy appTokenGenerator;
	private DataGroupSpy previousAppTokensGroup;
	private DataGroupSpy currentAppTokensGroup;
	private DataFactorySpy dataFactory;
	private DataGroupSpy onlyNoteAppTokenGroup1;
	private DataGroupSpy onlyNoteAppTokenGroup2;
	private DataGroupSpy linkAppTokenGroup1;
	private DataGroupSpy linkAppTokenGroup2;
	private DataGroupSpy linkAppTokenGroup3;
	private DataGroupSpy linkAppTokenGroup4;
	private DataGroupSpy linkAppTokenGroup5;
	private DataGroupSpy linkAppTokenGroup6;

	@BeforeMethod
	public void beforeMethod() {
		dataFactory = new DataFactorySpy();
		DataProvider.onlyForTestSetDataFactory(dataFactory);

		createPreviousData();
		createCurrentExtendedFunctionalityData();

		systemSecretOperations = new SystemSecretOperationsSpy();
		appTokenGenerator = new AppTokenGeneratorSpy();

		appTokenHandler = new AppTokenHandlerExtendedFunctionality(appTokenGenerator,
				systemSecretOperations);

		onlyNoteAppTokenGroup1 = createAppTokenGroupWithOnlyNote("AppToken1");
		onlyNoteAppTokenGroup2 = createAppTokenGroupWithOnlyNote("AppToken2");

		linkAppTokenGroup1 = createAppTokenGroupWithNoteAndAppTokenLink("AppToken1");
		linkAppTokenGroup2 = createAppTokenGroupWithNoteAndAppTokenLink("AppToken2");
		linkAppTokenGroup3 = createAppTokenGroupWithNoteAndAppTokenLink("AppToken3");
		linkAppTokenGroup4 = createAppTokenGroupWithNoteAndAppTokenLink("AppToken4");
		linkAppTokenGroup5 = createAppTokenGroupWithNoteAndAppTokenLink("AppToken5");
		linkAppTokenGroup6 = createAppTokenGroupWithNoteAndAppTokenLink("AppToken6");
	}

	private void createPreviousData() {
		previousAppTokensGroup = new DataGroupSpy();
		previousDataGroup = new DataGroupSpy();
	}

	private void createCurrentExtendedFunctionalityData() {
		currentAppTokensGroup = new DataGroupSpy();
		setupCurrentDataRecordGroupWithDataDivider();
		efData = new ExtendedFunctionalityData();
		efData.dataGroup = currentDataGroup;
		efData.previouslyStoredTopDataGroup = previousDataGroup;
	}

	private void setupCurrentDataRecordGroupWithDataDivider() {
		currentDataGroup = new DataGroupSpy();

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
	public void test_0currentOnlyNote_0currentLink_0previousMatch_0previousNotMatch()
			throws Exception {
		setUpCurrentWithAppTokens();

		appTokenHandler.useExtendedFunctionality(efData);

		currentDataGroup.MCR.assertParameters("containsChildOfTypeAndName", 0, DataGroup.class,
				"appTokens");

		assertNewAppTokensCreated();
		assertAppTokensInReturnedData();
		assertAppTokensRemovedFromStorage();
		assertNotCalledRemoveAppTokensGroup();
	}

	@Test
	public void test_1currentOnlyNote_0currentLink_0previousMatch_0previousNotMatch()
			throws Exception {
		setUpCurrentWithAppTokens(onlyNoteAppTokenGroup2);
		setUpAppTokenRemainsInCurrentGroup();

		appTokenHandler.useExtendedFunctionality(efData);

		assertAllCurrentAppTokenGroupsAreRead();
		assertRemoveAllAppTokensFromAppTokensCurrentGroupBeforeHandling();

		assertNewAppTokensCreated(onlyNoteAppTokenGroup2);
		assertAppTokensInReturnedData(onlyNoteAppTokenGroup2);
		assertAppTokensRemovedFromStorage();
		assertNotCalledRemoveAppTokensGroup();
	}

	@Test
	public void test_2currentOnlyNote_0currentLink_0previousMatch_0previousNotMatch()
			throws Exception {
		setUpCurrentWithAppTokens(onlyNoteAppTokenGroup1, onlyNoteAppTokenGroup2);
		setUpAppTokenRemainsInCurrentGroup();

		appTokenHandler.useExtendedFunctionality(efData);

		assertNewAppTokensCreated(onlyNoteAppTokenGroup1, onlyNoteAppTokenGroup2);
		assertAppTokensInReturnedData(onlyNoteAppTokenGroup2, onlyNoteAppTokenGroup2);
		assertAppTokensRemovedFromStorage();
		assertNotCalledRemoveAppTokensGroup();
	}

	@Test
	public void test_0currentOnlyNote_2currentLink_0previousMatch_0previousNotMatch()
			throws Exception {
		setUpCurrentWithAppTokens(linkAppTokenGroup1, linkAppTokenGroup2);

		appTokenHandler.useExtendedFunctionality(efData);

		assertNewAppTokensCreated();
		assertAppTokensInReturnedData();
		assertAppTokensRemovedFromStorage();
		assertRemoveAppTokensGroupWhenNoAppTokenExists();
	}

	@Test
	public void test_0currentOnlyNote_2currentLink_2previousMatch_0previousNotMatch()
			throws Exception {
		setUpPreviousWithAppTokens(linkAppTokenGroup1, linkAppTokenGroup2);
		setUpCurrentWithAppTokens(linkAppTokenGroup1, linkAppTokenGroup2);
		setUpAppTokenRemainsInCurrentGroup();

		appTokenHandler.useExtendedFunctionality(efData);

		assertNewAppTokensCreated();
		assertAppTokensInReturnedData(linkAppTokenGroup1, linkAppTokenGroup2);
		assertAppTokensRemovedFromStorage();
		assertNotCalledRemoveAppTokensGroup();
	}

	@Test
	public void test_0currentOnlyNote_0currentLink_0previousMatch_2previousNotMatch()
			throws Exception {
		setUpPreviousWithAppTokens(linkAppTokenGroup1, linkAppTokenGroup2);

		appTokenHandler.useExtendedFunctionality(efData);

		assertNewAppTokensCreated();
		assertAppTokensInReturnedData();
		assertAppTokensRemovedFromStorage(linkAppTokenGroup1, linkAppTokenGroup2);
	}

	@Test
	public void test_0currentOnlyNote_1currentLink_1previousMatch_2previousNotMatch()
			throws Exception {
		setUpPreviousWithAppTokens(linkAppTokenGroup1, linkAppTokenGroup2, linkAppTokenGroup3);
		setUpCurrentWithAppTokens(linkAppTokenGroup2);
		setUpAppTokenRemainsInCurrentGroup();

		appTokenHandler.useExtendedFunctionality(efData);

		assertNewAppTokensCreated();
		assertAppTokensInReturnedData(linkAppTokenGroup2);
		assertAppTokensRemovedFromStorage(linkAppTokenGroup1, linkAppTokenGroup3);
		assertNotCalledRemoveAppTokensGroup();
	}

	private void setUpAppTokenRemainsInCurrentGroup() {
		currentAppTokensGroup.MRV.setSpecificReturnValuesSupplier("containsChildWithNameInData",
				() -> true, "appToken");
	}

	@Test
	public void test_2currentOnlyNote_4currentLink_2previousMatch_2previousNotMatch()
			throws Exception {
		setUpPreviousWithAppTokens(linkAppTokenGroup1, linkAppTokenGroup2, linkAppTokenGroup3,
				linkAppTokenGroup4);
		setUpCurrentWithAppTokens(linkAppTokenGroup1, onlyNoteAppTokenGroup1, linkAppTokenGroup2,
				onlyNoteAppTokenGroup2, linkAppTokenGroup5, linkAppTokenGroup6);
		setUpAppTokenRemainsInCurrentGroup();

		appTokenHandler.useExtendedFunctionality(efData);

		assertNewAppTokensCreated(onlyNoteAppTokenGroup1, onlyNoteAppTokenGroup2);
		assertAppTokensInReturnedData(linkAppTokenGroup1, linkAppTokenGroup2,
				onlyNoteAppTokenGroup1, onlyNoteAppTokenGroup2);
		assertAppTokensRemovedFromStorage(linkAppTokenGroup3, linkAppTokenGroup4);
		assertNotCalledRemoveAppTokensGroup();
	}

	@Test
	public void testEnsureRemoveAllChildrenAfterFetchingAppTokensList() throws Exception {
		setUpPreviousWithAppTokens(linkAppTokenGroup1, linkAppTokenGroup2);
		setUpCurrentWithAppTokens(linkAppTokenGroup1, linkAppTokenGroup2);
		setUpAppTokenRemainsInCurrentGroup();
		setExceptionToStopExecutionOnRemoveAllChildren();

		try {
			appTokenHandler.useExtendedFunctionality(efData);
			fail();
		} catch (Exception e) {
			currentAppTokensGroup.MCR.assertMethodWasCalled("getChildrenOfTypeAndName");
		}
	}

	private void setExceptionToStopExecutionOnRemoveAllChildren() {
		currentAppTokensGroup.MRV.setAlwaysThrowException("removeAllChildrenWithNameInData",
				new RuntimeException());
	}

	private void assertAppTokensInReturnedData(DataGroup... appTokenGroups) {
		for (DataGroup appTokenGroup : appTokenGroups) {
			currentAppTokensGroup.MCR.assertCalledParameters("addChild", appTokenGroup);
		}
		currentAppTokensGroup.MCR.assertNumberOfCallsToMethod("addChild", appTokenGroups.length);
	}

	private void assertNotCalledRemoveAppTokensGroup() {
		currentDataGroup.MCR.assertMethodNotCalled("removeFirstChildWithNameInData");
	}

	private void assertRemoveAllAppTokensFromAppTokensCurrentGroupBeforeHandling() {
		currentAppTokensGroup.MCR.assertParameters("removeAllChildrenWithNameInData", 0,
				"appToken");
	}

	private void assertAllCurrentAppTokenGroupsAreRead() {
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

	private void setUpCurrentWithAppTokens(DataGroupSpy... appTokenGroup) {
		setupAppTokensGroupsUsingAppTokenGroups(currentDataGroup, currentAppTokensGroup,
				appTokenGroup);
	}

	private void setupAppTokensGroupsUsingAppTokenGroups(DataGroupSpy userDataGroup,
			DataGroupSpy appTokensGroup, DataGroupSpy... appTokenGroups) {
		if (appTokenGroups.length > 0) {
			userDataGroup.MRV.setSpecificReturnValuesSupplier("containsChildOfTypeAndName",
					() -> true, DataGroup.class, "appTokens");
			userDataGroup.MRV.setSpecificReturnValuesSupplier("getFirstGroupWithNameInData",
					() -> appTokensGroup, "appTokens");

			List<DataGroup> appTokenGroupList = new ArrayList<>();
			for (DataGroupSpy appTokenGroup : appTokenGroups) {
				appTokenGroupList.add(appTokenGroup);
			}
			appTokensGroup.MRV.setDefaultReturnValuesSupplier("getChildrenOfTypeAndName",
					() -> appTokenGroupList);
		}
	}

	private DataGroupSpy createAppTokenGroupWithOnlyNote(String postfix) {
		DataGroupSpy appTokenGroup = new DataGroupSpy();
		appTokenGroup.MRV.setDefaultReturnValuesSupplier("note", () -> "someNote" + postfix);
		return appTokenGroup;
	}

	private void assertNewAppTokensCreated(DataGroupSpy... appTokenGroups) {
		int nrAssertedAppTokens = 0;
		for (DataGroupSpy appTokenGroup : appTokenGroups) {
			assertCreationOfAnAppToken(nrAssertedAppTokens, appTokenGroup);
			nrAssertedAppTokens++;
		}
		if (nrAssertedAppTokens == 0) {
			assertNoApptokenCreated();
		}
	}

	private void assertCreationOfAnAppToken(int callNr, DataGroupSpy appTokenGroup) {
		var generatedAppToken = appTokenGenerator.MCR.getReturnValue("generateAppToken", callNr);
		var systemSecretId = systemSecretOperations.MCR.assertCalledParametersReturn(
				"createAndStoreSystemSecretRecord", generatedAppToken, SOME_DATA_DIVIDER);
		var appTokenLink = dataFactory.MCR.assertCalledParametersReturn(
				"factorRecordLinkUsingNameInDataAndTypeAndId", "appTokenLink", "systemSecret",
				systemSecretId);
		appTokenGroup.MCR.assertParameters("addChild", 0, appTokenLink);
		assertCreatedTokensInEFSharedData(generatedAppToken, systemSecretId);
	}

	private void assertCreatedTokensInEFSharedData(Object generatedAppToken,
			Object systemSecretId) {
		String className = appTokenHandler.getClass().getSimpleName();
		Map<String, String> storedLinkTokenMap = (Map<String, String>) efData.dataSharer
				.get(className);
		assertEquals(storedLinkTokenMap.get(systemSecretId), generatedAppToken);
	}

	private void assertNoApptokenCreated() {
		appTokenGenerator.MCR.assertMethodNotCalled("generateAppToken");
		systemSecretOperations.MCR.assertMethodNotCalled("createAndStoreSystemSecretRecord");
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

	private void setUpPreviousWithAppTokens(DataGroupSpy... appTokenGroup) {
		setupAppTokensGroupsUsingAppTokenGroups(previousDataGroup, previousAppTokensGroup,
				appTokenGroup);
	}

	private void assertAppTokensRemovedFromStorage(DataGroup... appTokenGroups) {
		for (DataGroup appToken : appTokenGroups) {
			DataRecordLink appTokenLink = appToken.getFirstChildOfTypeAndName(DataRecordLink.class,
					"appTokenLink");
			systemSecretOperations.MCR.assertCalledParameters("deleteSystemSecretFromStorage",
					appTokenLink.getLinkedRecordId());
		}
		systemSecretOperations.MCR.assertNumberOfCallsToMethod("deleteSystemSecretFromStorage",
				appTokenGroups.length);
	}
}
