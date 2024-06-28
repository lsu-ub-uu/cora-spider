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
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import se.uu.ub.cora.data.DataAtomic;
import se.uu.ub.cora.data.DataGroup;
import se.uu.ub.cora.data.DataProvider;
import se.uu.ub.cora.data.DataRecordLink;
import se.uu.ub.cora.data.spies.DataFactorySpy;
import se.uu.ub.cora.data.spies.DataGroupSpy;
import se.uu.ub.cora.data.spies.DataRecordLinkSpy;
import se.uu.ub.cora.spider.extendedfunctionality.ExtendedFunctionality;
import se.uu.ub.cora.spider.extendedfunctionality.ExtendedFunctionalityData;

public class AppTokenClearTextExtendedFuncionalityTest {
	private DataFactorySpy dataFactory;
	private ExtendedFunctionalityData efData;
	private DataGroupSpy currentDataGroup;
	private DataGroupSpy linkAppTokenGroup1;
	private DataGroupSpy linkAppTokenGroup2;
	private AppTokenClearTextExtendedFuncionality exFunc;
	private DataGroupSpy currentAppTokensGroup;
	private Map<String, Object> appTokensClearText;

	@BeforeMethod
	public void beforeMethod() {
		dataFactory = new DataFactorySpy();
		DataProvider.onlyForTestSetDataFactory(dataFactory);

		linkAppTokenGroup1 = createAppTokenGroupWithNoteAndAppTokenLink("AppToken1");
		linkAppTokenGroup2 = createAppTokenGroupWithNoteAndAppTokenLink("AppToken2");
		createCurrentExtendedFunctionalityData();

		exFunc = new AppTokenClearTextExtendedFuncionality();
	}

	private void createCurrentExtendedFunctionalityData() {
		currentAppTokensGroup = new DataGroupSpy();
		appTokensClearText = new HashMap<>();
		setupCurrentDataRecordGroup();
		efData = new ExtendedFunctionalityData();
		efData.dataGroup = currentDataGroup;
		efData.dataSharer.put("AppTokenHandlerExtendedFunctionality", appTokensClearText);
	}

	private void setupCurrentDataRecordGroup() {
		currentDataGroup = new DataGroupSpy();

		DataGroupSpy recordInfo = new DataGroupSpy();
		currentDataGroup.MRV.setReturnValues("getFirstGroupWithNameInData", List.of(recordInfo),
				"recordInfo");
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
	private void testInitImplementsExtendedFunctionality() {
		assertTrue(exFunc instanceof ExtendedFunctionality);
	}

	@Test
	public void test_0clearText_0totalApptokens() throws Exception {
		exFunc.useExtendedFunctionality(efData);

		dataFactory.MCR.assertMethodNotCalled("factorAtomicUsingNameInDataAndValue");
	}

	@Test
	public void test_0clearText_1totalApptokens() throws Exception {
		setUpCurrentWithAppTokens(linkAppTokenGroup1);

		exFunc.useExtendedFunctionality(efData);

		dataFactory.MCR.assertMethodNotCalled("factorAtomicUsingNameInDataAndValue");
	}

	@Test
	public void test_1clearText_1totalApptokens() throws Exception {
		setUpCurrentWithAppTokens(linkAppTokenGroup1);
		appTokensClearText.put("someSystemSecretAppToken1", "someClearText1");

		exFunc.useExtendedFunctionality(efData);

		assertClearTextAddsToCorrectAppToken(linkAppTokenGroup1, "someClearText1");
		dataFactory.MCR.assertNumberOfCallsToMethod("factorAtomicUsingNameInDataAndValue", 1);
	}

	@Test
	public void test_1clearText_2totalApptokens() throws Exception {
		setUpCurrentWithAppTokens(linkAppTokenGroup1, linkAppTokenGroup2);
		appTokensClearText.put("someSystemSecretAppToken1", "someClearText1");

		exFunc.useExtendedFunctionality(efData);

		assertClearTextAddsToCorrectAppToken(linkAppTokenGroup1, "someClearText1");
		dataFactory.MCR.assertNumberOfCallsToMethod("factorAtomicUsingNameInDataAndValue", 1);
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

	private void assertClearTextAddsToCorrectAppToken(DataGroupSpy appTokenGroup,
			String clearText) {
		DataAtomic clearTextAtomic = (DataAtomic) dataFactory.MCR.assertCalledParametersReturn(
				"factorAtomicUsingNameInDataAndValue", "appTokenClearText", clearText);
		appTokenGroup.MCR.assertParameters("addChild", 0, clearTextAtomic);
	}
}
