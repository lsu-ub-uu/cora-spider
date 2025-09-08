/*
 * Copyright 2017, 2018, 2019, 2024 Uppsala University Library
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
package se.uu.ub.cora.spider.record.internal;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertNotNull;
import static org.testng.Assert.assertSame;
import static org.testng.Assert.assertTrue;
import static org.testng.Assert.fail;
import static se.uu.ub.cora.spider.extendedfunctionality.ExtendedFunctionalityPosition.SEARCH_AFTER_AUTHORIZATION;

import java.util.ArrayList;
import java.util.List;

import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import se.uu.ub.cora.beefeater.authentication.User;
import se.uu.ub.cora.data.DataGroup;
import se.uu.ub.cora.data.DataList;
import se.uu.ub.cora.data.DataProvider;
import se.uu.ub.cora.data.spies.DataFactorySpy;
import se.uu.ub.cora.data.spies.DataGroupSpy;
import se.uu.ub.cora.data.spies.DataListSpy;
import se.uu.ub.cora.data.spies.DataRecordGroupSpy;
import se.uu.ub.cora.data.spies.DataRecordLinkSpy;
import se.uu.ub.cora.search.SearchResult;
import se.uu.ub.cora.spider.dependency.SpiderDependencyProvider;
import se.uu.ub.cora.spider.extendedfunctionality.ExtendedFunctionalityData;
import se.uu.ub.cora.spider.extendedfunctionality.internal.ExtendedFunctionalityProviderSpy;
import se.uu.ub.cora.spider.record.DataException;
import se.uu.ub.cora.spider.record.DataGroupToRecordEnhancerSpy;
import se.uu.ub.cora.spider.record.RecordSearcher;
import se.uu.ub.cora.spider.spy.DataValidatorSpy;
import se.uu.ub.cora.spider.spy.SpiderDependencyProviderSpy;
import se.uu.ub.cora.spider.spy.ValidationAnswerSpy;
import se.uu.ub.cora.storage.spies.RecordStorageSpy;

public class RecordSearcherTest {
	private RecordSearcher recordSearcher;

	private static final String SOME_SEARCH_ID = "aSearchId";
	private static final String ANOTHER_SEARCH_ID = "anotherSearchId";
	private static final String SOME_AUTH_TOKEN = "someToken78678567";

	private final DataGroupSpy someSearchData = new DataGroupSpy();

	private SpiderDependencyProviderSpy dependencyProvider;
	private DataFactorySpy dataFactorySpy;
	private RecordStorageSpy recordStorage;
	private AuthenticatorSpy authenticator;
	private SpiderAuthorizatorSpy authorizator;
	private DataGroupToRecordEnhancerSpy dataGroupToRecordEnhancer;
	private DataValidatorSpy dataValidator;
	private RecordSearchSpy recordSearch;
	private DataRedactorSpy dataRedactor;
	private ExtendedFunctionalityProviderSpy extendedFunctionalityProvider;
	private DataRecordGroupSpy dataRecordGroupSpy;
	private DataGroupSpy dataGroupSpy1;
	private DataGroupSpy dataGroupSpy2;
	private DataListSpy dataListSpy;

	@BeforeMethod
	public void beforeMethod() {
		setUpFactoriesAndProviders();

		authenticator = new AuthenticatorSpy();
		authorizator = new SpiderAuthorizatorSpy();
		dataValidator = new DataValidatorSpy();
		recordStorage = new RecordStorageSpy();
		dataRedactor = new DataRedactorSpy();
		dataGroupToRecordEnhancer = new DataGroupToRecordEnhancerSpy();
		SearchResult searchResult = setRecordStorage();
		recordSearch.MRV.setDefaultReturnValuesSupplier(
				"searchUsingListOfRecordTypesToSearchInAndSearchData", () -> searchResult);

		setUpDependencyProvider();

		recordSearcher = RecordSearcherImp
				.usingDependencyProvider(dependencyProvider);

	}

	private void setUpFactoriesAndProviders() {
		dataFactorySpy = new DataFactorySpy();
		dataListSpy = new DataListSpy();
		setDataListValues(1);

		dataFactorySpy.MRV.setDefaultReturnValuesSupplier("factorListUsingNameOfDataType",
				() -> dataListSpy);
		DataProvider.onlyForTestSetDataFactory(dataFactorySpy);
	}

	private DataRecordGroupSpy setSearchRecordGroupWithSeveralRecordTypesToSearchIn() {
		dataGroupSpy1 = new DataGroupSpy();
		dataGroupSpy1.MRV.setDefaultReturnValuesSupplier("getFirstAtomicValueWithNameInData",
				() -> "someNameInData1");
		dataGroupSpy2 = new DataGroupSpy();
		dataGroupSpy2.MRV.setDefaultReturnValuesSupplier("getFirstAtomicValueWithNameInData",
				() -> "someNameInData2");
		DataRecordGroupSpy dataRecordGroupSpy = new DataRecordGroupSpy();
		dataRecordGroupSpy.MRV.setDefaultReturnValuesSupplier("getAllGroupsWithNameInData",
				() -> List.of(dataGroupSpy1, dataGroupSpy2));
		return dataRecordGroupSpy;
	}

	private SearchResult setRecordStorage() {
		dataRecordGroupSpy = setSearchRecordGroupWithSeveralRecordTypesToSearchIn();
		recordStorage.MRV.setDefaultReturnValuesSupplier("read", () -> dataRecordGroupSpy);
		recordSearch = new RecordSearchSpy();
		extendedFunctionalityProvider = new ExtendedFunctionalityProviderSpy();

		DataGroupSpy foundDataGroup = createFoundDataGroupForSearch("someNameInData1");
		SearchResult searchResult = new SearchResult();
		searchResult.listOfDataGroups = List.of(foundDataGroup);
		searchResult.start = 1;
		searchResult.totalNumberOfMatches = 1;
		return searchResult;
	}

	private DataGroupSpy createFoundDataGroupForSearch(String recordType) {
		DataRecordLinkSpy dataRecordLink = new DataRecordLinkSpy();
		dataRecordLink.MRV.setDefaultReturnValuesSupplier("getLinkedRecordId", () -> recordType);
		DataGroupSpy foundDataGroupRecordInfo = new DataGroupSpy();
		foundDataGroupRecordInfo.MRV.setSpecificReturnValuesSupplier("getFirstChildWithNameInData",
				() -> dataRecordLink, "type");

		DataGroupSpy foundDataGroup = new DataGroupSpy();
		foundDataGroup.MRV.setSpecificReturnValuesSupplier("getFirstGroupWithNameInData",
				() -> foundDataGroupRecordInfo, "recordInfo");

		return foundDataGroup;
	}

	private void setUpDependencyProvider() {
		dependencyProvider = new SpiderDependencyProviderSpy();
		dependencyProvider.MRV.setDefaultReturnValuesSupplier("getAuthenticator",
				() -> authenticator);
		dependencyProvider.MRV.setDefaultReturnValuesSupplier("getSpiderAuthorizator",
				() -> authorizator);
		dependencyProvider.MRV.setDefaultReturnValuesSupplier("getDataValidator",
				() -> dataValidator);
		dependencyProvider.MRV.setDefaultReturnValuesSupplier("getExtendedFunctionalityProvider",
				() -> extendedFunctionalityProvider);
		dependencyProvider.MRV.setDefaultReturnValuesSupplier("getRecordStorage",
				() -> recordStorage);
		dependencyProvider.MRV.setDefaultReturnValuesSupplier("getRecordSearch",
				() -> recordSearch);
		dependencyProvider.MRV.setDefaultReturnValuesSupplier("getDataRedactor",
				() -> dataRedactor);
		dependencyProvider.MRV.setDefaultReturnValuesSupplier("getDataGroupToRecordEnhancer",
				() -> dataGroupToRecordEnhancer);

	}

	private void setDataListValues(int amountDataInList) {
		List<DataGroup> dataGroupsList = new ArrayList<>();
		for (int i = 0; i < amountDataInList; i++) {
			dataGroupsList.add(new DataGroupSpy());
		}
		dataListSpy.MRV.setDefaultReturnValuesSupplier("getDataList", () -> dataGroupsList);
	}

	@Test
	public void testRecordSearchReturnValue() {
		DataList searchResult = recordSearcher.search(SOME_AUTH_TOKEN, SOME_SEARCH_ID,
				someSearchData);

		dataFactorySpy.MCR.assertMethodWasCalled("factorListUsingNameOfDataType");
		dataFactorySpy.MCR.assertReturn("factorListUsingNameOfDataType", 0, searchResult);

	}

	@Test
	public void testDefaultFromNoSetToOne() {
		recordSearcher.search(SOME_AUTH_TOKEN, SOME_SEARCH_ID, someSearchData);

		dataListSpy.MCR.assertParameters("setFromNo", 0, "1");
	}

	@Test
	public void testStartRowReadFromInput() {
		someSearchData.MRV.setSpecificReturnValuesSupplier("containsChildWithNameInData",
				() -> true, "start");
		someSearchData.MRV.setSpecificReturnValuesSupplier("getFirstAtomicValueWithNameInData",
				() -> "2", "start");

		recordSearcher.search(SOME_AUTH_TOKEN, SOME_SEARCH_ID, someSearchData);

		dataListSpy.MCR.assertParameters("setFromNo", 0, "2");
	}

	@Test
	public void testSearchAuthenticated() {
		DataList searchResult = recordSearcher.search(SOME_AUTH_TOKEN, SOME_SEARCH_ID,
				someSearchData);

		assertNotNull(searchResult);
		authenticator.MCR.assertParameters("getUserForToken", 0, SOME_AUTH_TOKEN);
		var authenticatedUser = getAuthenticatedUser();

		recordStorage.MCR.assertMethodWasCalled("read");
		dataRecordGroupSpy.MCR.assertParameters("getAllGroupsWithNameInData", 0,
				"recordTypeToSearchIn");

		authorizator.MCR.assertParameters("checkUserIsAuthorizedForActionOnRecordType", 0,
				authenticatedUser, "search", "someNameInData1");
		authorizator.MCR.assertParameters("checkUserIsAuthorizedForActionOnRecordType", 1,
				authenticatedUser, "search", "someNameInData2");

	}

	@Test
	public void testValidatesSearchMetadata() {
		recordSearcher.search(SOME_AUTH_TOKEN, SOME_SEARCH_ID, someSearchData);

		dataRecordGroupSpy.MCR.assertParameters("getFirstGroupWithNameInData", 0, "metadataId");
		DataGroupSpy definitionGroup = (DataGroupSpy) dataRecordGroupSpy.MCR
				.getReturnValue("getFirstGroupWithNameInData", 0);

		String metadataGroupIdToValidateAgainst = (String) definitionGroup.MCR
				.getReturnValue("getFirstAtomicValueWithNameInData", 0);

		dataValidator.MCR.assertParameters("validateData", 0, metadataGroupIdToValidateAgainst,
				someSearchData);
		dataValidator.MCR.assertNumberOfCallsToMethod("validateData", 1);
	}

	private Object getAuthenticatedUser() {
		return authenticator.MCR.getReturnValue("getUserForToken", 0);
	}

	@Test
	public void testSearchAuthenticatedAndAuthorizedInvalidData() {
		ValidationAnswerSpy validationAnswer = createValidationAnswerWhenInvalidV();
		dataValidator.MRV.setDefaultReturnValuesSupplier("validateData", () -> validationAnswer);

		try {
			recordSearcher.search(SOME_AUTH_TOKEN, SOME_SEARCH_ID, someSearchData);
			fail("Exception should have been thrown");
		} catch (Exception e) {
			assertTrue(e instanceof DataException);
			assertEquals(e.getMessage(), "Data is not valid: [someErrorMessageFromValidator]");
		}
	}

	private ValidationAnswerSpy createValidationAnswerWhenInvalidV() {
		ValidationAnswerSpy validationAnswer = new ValidationAnswerSpy();
		validationAnswer.MRV.setDefaultReturnValuesSupplier("dataIsInvalid", () -> true);
		validationAnswer.MRV.setDefaultReturnValuesSupplier("getErrorMessages",
				() -> List.of("someErrorMessageFromValidator"));
		return validationAnswer;
	}

	@Test
	public void testRecordSearchIsCalled() {
		recordSearcher.search(SOME_AUTH_TOKEN, SOME_SEARCH_ID, someSearchData);

		dataGroupSpy1.MCR.assertNumberOfCallsToMethod("getFirstAtomicValueWithNameInData", 2);
		dataGroupSpy2.MCR.assertNumberOfCallsToMethod("getFirstAtomicValueWithNameInData", 2);

		List<String> recordTypeList = (List<String>) recordSearch.MCR
				.getParameterForMethodAndCallNumberAndParameter(
						"searchUsingListOfRecordTypesToSearchInAndSearchData", 0, "recordTypes");
		assertEquals(recordTypeList.get(0), "someNameInData1");
		assertEquals(recordTypeList.get(1), "someNameInData2");
		assertEquals(recordTypeList.size(), 2);

		recordSearch.MCR.assertParameter("searchUsingListOfRecordTypesToSearchInAndSearchData", 0,
				"searchData", someSearchData);
	}

	@Test
	public void testSearchResultIsEnhancedForEachResult() {
		DataList searchList = recordSearcher.search(SOME_AUTH_TOKEN, SOME_SEARCH_ID,
				someSearchData);

		SearchResult searchResult = (SearchResult) recordSearch.MCR
				.getReturnValue("searchUsingListOfRecordTypesToSearchInAndSearchData", 0);

		DataGroup firstDataGroupFromSearchResult = searchResult.listOfDataGroups.get(0);
		assertEquals(dataGroupToRecordEnhancer.recordType, "someNameInData1");

		var recordAsDataRecordGroup = dataFactorySpy.MCR.assertCalledParametersReturn(
				"factorRecordGroupFromDataGroup", firstDataGroupFromSearchResult);

		dataGroupToRecordEnhancer.MCR.assertNumberOfCallsToMethod("enhance", 1);
		dataGroupToRecordEnhancer.MCR.assertParameters("enhance", 0, getAuthenticatedUser(),
				"someNameInData1", recordAsDataRecordGroup, dataRedactor);

		dataFactorySpy.MCR.assertNumberOfCallsToMethod("factorListUsingNameOfDataType", 1);
		dataFactorySpy.MCR.assertParameters("factorListUsingNameOfDataType", 0, "mix");

		dataListSpy.MCR.assertNumberOfCallsToMethod("addData", 1);

		dataListSpy.MCR.assertParameters("setFromNo", 0, "1");
		dataListSpy.MCR.assertParameters("setToNo", 0, "1");
		dataListSpy.MCR.assertParameters("setTotalNo", 0,
				String.valueOf(searchResult.totalNumberOfMatches));

		assertEquals(searchList, dataListSpy);
	}

	@Test
	public void testSameRedactorUsedWhenEnhancing() {
		recordSearcher.search(SOME_AUTH_TOKEN, ANOTHER_SEARCH_ID, someSearchData);

		dependencyProvider.MCR.assertNumberOfCallsToMethod("getDataRedactor", 1);
	}

	@Test
	public void testSearchResultIsFilteredAndEnhancedForEachResult() {
		SearchResult searchResult = setSearchResultWithThreeHits();
		recordSearch.MRV.setDefaultReturnValuesSupplier(
				"searchUsingListOfRecordTypesToSearchInAndSearchData", () -> searchResult);
		setDataListValues(3);

		recordSearcher.search(SOME_AUTH_TOKEN, ANOTHER_SEARCH_ID, someSearchData);

		dataListSpy.MCR.assertNumberOfCallsToMethod("addData", 3);

		dataListSpy.MCR.assertParameters("setFromNo", 0, "1");
		dataListSpy.MCR.assertParameters("setToNo", 0, "3");
		dataListSpy.MCR.assertParameters("setTotalNo", 0, "3");
	}

	@Test
	public void testSearchResultOnlyFirstResultHasReadAccess() {
		SearchResult searchResult = setSearchResultWithThreeHits();
		recordSearch.MRV.setDefaultReturnValuesSupplier(
				"searchUsingListOfRecordTypesToSearchInAndSearchData", () -> searchResult);

		dataGroupToRecordEnhancer.addReadActionOnlyFirst = true;

		recordSearcher.search(SOME_AUTH_TOKEN, ANOTHER_SEARCH_ID, someSearchData);

		dataListSpy.MCR.assertNumberOfCallsToMethod("addData", 1);

		dataListSpy.MCR.assertParameters("setFromNo", 0, "1");
		dataListSpy.MCR.assertParameters("setToNo", 0, "1");
		dataListSpy.MCR.assertParameters("setTotalNo", 0, "3");
	}

	private SearchResult setSearchResultWithThreeHits() {
		List<DataGroup> recordSearchList = List.of(createFoundDataGroupForSearch("someRecordType1"),
				createFoundDataGroupForSearch("someRecordType2"),
				createFoundDataGroupForSearch("someRecordType3"));

		SearchResult searchResult = new SearchResult();
		searchResult.listOfDataGroups = recordSearchList;
		searchResult.start = 1;
		searchResult.totalNumberOfMatches = recordSearchList.size();
		return searchResult;
	}

	@Test
	public void testExtendedFunctionalitySetUp() {
		recordSearcher.search(SOME_AUTH_TOKEN, SOME_SEARCH_ID, someSearchData);

		dependencyProvider.MCR.assertParameters("getExtendedFunctionalityProvider", 0);
		extendedFunctionalityProvider.MCR.assertParameters(
				"getFunctionalityForPositionAndRecordType", 0, SEARCH_AFTER_AUTHORIZATION,
				"search");
	}

	@Test
	public void testExtendedFunctionalityIsCalled() {
		recordSearcher.search(SOME_AUTH_TOKEN, SOME_SEARCH_ID, someSearchData);

		extendedFunctionalityProvider.assertCallToPositionAndFunctionalityCalledWithData(
				SEARCH_AFTER_AUTHORIZATION, getExpectedDataForAfterAuthorization(), 0);
	}

	private ExtendedFunctionalityData getExpectedDataForAfterAuthorization() {
		ExtendedFunctionalityData expectedData = new ExtendedFunctionalityData();
		expectedData.recordType = "search";
		expectedData.authToken = SOME_AUTH_TOKEN;
		expectedData.user = (User) authenticator.MCR.getReturnValue("getUserForToken", 0);
		expectedData.dataRecordGroup = dataRecordGroupSpy;
		return expectedData;
	}

	@Test
	public void testEnsureExtendedFunctionalityPositionFor_AfterAuthorization() {
		extendedFunctionalityProvider.setUpExtendedFunctionalityToThrowExceptionOnPosition(
				dependencyProvider, SEARCH_AFTER_AUTHORIZATION, "search");

		callReadIncomingLinksAndCatchStopExecution();

		authorizator.MCR.assertMethodWasCalled("checkUserIsAuthorizedForActionOnRecordType");
		extendedFunctionalityProvider.MCR
				.assertNumberOfCallsToMethod("getFunctionalityForPositionAndRecordType", 1);
		dataRecordGroupSpy.MCR.assertMethodNotCalled("getFirstGroupWithNameInData");
	}

	private void callReadIncomingLinksAndCatchStopExecution() {
		try {
			recordSearcher.search(SOME_AUTH_TOKEN, SOME_SEARCH_ID, someSearchData);
			fail("Should fail as we want to stop execution when the extended functionality is used,"
					+ " to determin that the extended functionality is called in the correct place"
					+ " in the code");
		} catch (Exception e) {
		}
	}

	@Test
	public void testOnlyForTestGetDataGroupToRecordEnhancer() {
		RecordSearcherImp recordSearcherImp = (RecordSearcherImp) recordSearcher;
		SpiderDependencyProvider dep = recordSearcherImp.onlyForTestGetDependencyProvider();

		assertSame(dependencyProvider, dep);
	}

}
