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
import se.uu.ub.cora.data.DataRecordLink;
import se.uu.ub.cora.data.spies.DataGroupSpy;
import se.uu.ub.cora.data.spies.DataRecordLinkSpy;
import se.uu.ub.cora.spider.extendedfunctionality.ExtendedFunctionality;
import se.uu.ub.cora.spider.extendedfunctionality.ExtendedFunctionalityData;
import se.uu.ub.cora.spider.spy.SpiderDependencyProviderSpy;
import se.uu.ub.cora.storage.spies.RecordStorageSpy;

public class AppTokenHandlerExtendedFunctionalityTest {

	private DataGroupSpy previousDataGroup;
	private DataGroupSpy currentDataGroup;
	private ExtendedFunctionalityData efData;
	private AppTokenHandlerExtendedFunctionality appTokenHandler;
	private DataRecordLinkSpy appTokenLink1;
	private DataRecordLinkSpy appTokenLink2;
	private DataRecordLinkSpy appTokenLink3;
	private SpiderDependencyProviderSpy dependencyProvider;
	private RecordStorageSpy recordStorage;

	@BeforeMethod
	public void beforeMethod() {
		// dataFactory = new DataFactorySpy();
		// DataProvider.onlyForTestSetDataFactory(dataFactory);

		recordStorage = new RecordStorageSpy();

		dependencyProvider = new SpiderDependencyProviderSpy();
		dependencyProvider.MRV.setDefaultReturnValuesSupplier("getRecordStorage",
				() -> recordStorage);

		// textHasher = new TextHasherSpy();
		// extended = PasswordExtendedFunctionality
		// .usingDependencyProviderAndTextHasher(dependencyProvider, textHasher);
		previousDataGroup = new DataGroupSpy();
		currentDataGroup = new DataGroupSpy();

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

		appTokenLink1 = new DataRecordLinkSpy();
		appTokenLink1.MRV.setDefaultReturnValuesSupplier("getLinkedRecordId",
				() -> "appTokenLink1");
		appTokenLink2 = new DataRecordLinkSpy();
		appTokenLink2.MRV.setDefaultReturnValuesSupplier("getLinkedRecordId",
				() -> "appTokenLink2");
		appTokenLink3 = new DataRecordLinkSpy();
		appTokenLink3.MRV.setDefaultReturnValuesSupplier("getLinkedRecordId",
				() -> "appTokenLink3");

		appTokenHandler = new AppTokenHandlerExtendedFunctionality();

	}

	private void createExtendedFunctionalityData() {
		efData = new ExtendedFunctionalityData();
		efData.dataGroup = currentDataGroup;
		efData.previouslyStoredTopDataGroup = previousDataGroup;
		efData.authToken = "fakeToken";
		efData.recordType = "fakeType";
		efData.recordId = "fakeId";
	}

	@Test
	public void testIsExtendedFunctionality() throws Exception {
		assertTrue(appTokenHandler instanceof ExtendedFunctionality);
	}

	@Test
	public void testAnyStoreAppTokensAnyNewAppTokens() throws Exception {
		appTokenHandler.useExtendedFunctionality(efData);

		previousDataGroup.MCR.assertParameters("containsChildOfTypeAndName", 0, DataGroup.class,
				"appToken");
		currentDataGroup.MCR.assertParameters("containsChildOfTypeAndName", 0, DataGroup.class,
				"appToken");
		previousDataGroup.MCR.assertMethodNotCalled("getFirstAtomicValueWithNameInData");
		currentDataGroup.MCR.assertMethodNotCalled("getFirstAtomicValueWithNameInData");
	}

	private DataGroup createDataGroupWithAppTokens(DataGroupSpy userDataGroups,
			DataRecordLink... appTokenLinks) {

		if (appTokenLinks.length > 0) {
			userDataGroups.MRV.setSpecificReturnValuesSupplier("containsChildOfTypeAndName",
					() -> true, DataGroup.class, "appToken");

			List<DataGroup> appTokensGroups = new ArrayList<>();

			userDataGroups.MRV.setDefaultReturnValuesSupplier("getAllGroupsWithNameInData",
					() -> appTokensGroups);

			for (DataRecordLink appTokenLink : appTokenLinks) {
				DataGroupSpy appTokenGroup = new DataGroupSpy();
				appTokenGroup.MRV.setSpecificReturnValuesSupplier("getFirstChildOfTypeAndName",
						() -> appTokenLink, DataRecordLink.class, "appTokenLink");

				appTokensGroups.add(appTokenGroup);
			}
		}
		return userDataGroups;
	}

	@Test
	public void testTwoNewAppToken() throws Exception {
		createDataGroupWithAppTokens(previousDataGroup);
		createDataGroupWithAppTokens(currentDataGroup, appTokenLink1, appTokenLink2);

		appTokenHandler.useExtendedFunctionality(efData);

		// previousDataGroup.MCR.assertParameters("getFirstGroupWithNameInData", 0, "appToken");
		currentDataGroup.MCR.assertParameters("getAllGroupsWithNameInData", 0, "appToken");

		appTokenLink1.MCR.assertMethodWasCalled("getLinkedRecordId");

		// recordStorage.MCR.assertNumberOfCallsToMethod("create", 2);

	}

}
