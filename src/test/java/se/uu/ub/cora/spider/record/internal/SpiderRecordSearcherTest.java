/*
 * Copyright 2017, 2018, 2019 Uppsala University Library
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

import java.util.List;

import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import se.uu.ub.cora.data.DataGroup;
import se.uu.ub.cora.data.DataList;
import se.uu.ub.cora.data.DataProvider;
import se.uu.ub.cora.data.DataRecord;
import se.uu.ub.cora.data.copier.DataCopierFactory;
import se.uu.ub.cora.data.copier.DataCopierProvider;
import se.uu.ub.cora.data.spies.DataFactorySpy;
import se.uu.ub.cora.logger.LoggerProvider;
import se.uu.ub.cora.search.RecordSearch;
import se.uu.ub.cora.spider.authentication.AuthenticationException;
import se.uu.ub.cora.spider.authentication.OldAuthenticatorSpy;
import se.uu.ub.cora.spider.authorization.AuthorizationException;
import se.uu.ub.cora.spider.authorization.PermissionRuleCalculator;
import se.uu.ub.cora.spider.data.DataAtomicSpy;
import se.uu.ub.cora.spider.data.DataGroupOldSpy;
import se.uu.ub.cora.spider.dependency.spy.SpiderDependencyProviderOldSpy;
import se.uu.ub.cora.spider.log.LoggerFactorySpy;
import se.uu.ub.cora.spider.record.DataCopierFactorySpy;
import se.uu.ub.cora.spider.record.DataException;
import se.uu.ub.cora.spider.record.DataGroupToRecordEnhancerSpy;
import se.uu.ub.cora.spider.record.DataListSpy;
import se.uu.ub.cora.spider.record.DataRedactorSpy;
import se.uu.ub.cora.spider.record.RecordSearcher;
import se.uu.ub.cora.spider.spy.DataGroupTermCollectorSpy;
import se.uu.ub.cora.spider.spy.DataValidatorSpy;
import se.uu.ub.cora.spider.spy.OldRecordStorageSpy;
import se.uu.ub.cora.spider.spy.RuleCalculatorSpy;
import se.uu.ub.cora.spider.spy.OldSpiderAuthorizatorSpy;
import se.uu.ub.cora.spider.testdata.TestDataRecordInMemoryStorage;
import se.uu.ub.cora.storage.RecordStorage;

public class SpiderRecordSearcherTest {
	private static final String A_SEARCH_ID = "aSearchId";
	private static final String ANOTHER_SEARCH_ID = "anotherSearchId";
	private static final String SOME_AUTH_TOKEN = "someToken78678567";
	private DataFactorySpy dataFactorySpy;

	private final DataGroup someSearchData = new DataGroupOldSpy("search");

	private RecordStorage recordStorage;
	private OldAuthenticatorSpy authenticator;
	private OldSpiderAuthorizatorSpy authorizator;
	private PermissionRuleCalculator keyCalculator;
	private DataGroupToRecordEnhancerSpy dataGroupToRecordEnhancer;
	private RecordSearcher recordSearcher;
	private DataValidatorSpy dataValidator;
	private RecordSearch recordSearch;
	private DataGroupTermCollectorSpy termCollector;
	private LoggerFactorySpy loggerFactorySpy;
	private DataCopierFactory dataCopierFactorySpy;
	private DataRedactorSpy dataRedactor;
	private SpiderDependencyProviderOldSpy dependencyProvider;

	@BeforeMethod
	public void beforeMethod() {
		setUpFactoriesAndProviders();

		authenticator = new OldAuthenticatorSpy();
		authorizator = new OldSpiderAuthorizatorSpy();
		dataValidator = new DataValidatorSpy();
		recordStorage = TestDataRecordInMemoryStorage.createRecordStorageInMemoryWithTestData();
		keyCalculator = new RuleCalculatorSpy();
		recordSearch = new RecordSearchSpy();
		termCollector = new DataGroupTermCollectorSpy();
		dataRedactor = new DataRedactorSpy();
		setUpDependencyProvider();
	}

	private void setUpFactoriesAndProviders() {
		loggerFactorySpy = new LoggerFactorySpy();
		LoggerProvider.setLoggerFactory(loggerFactorySpy);
		dataFactorySpy = new DataFactorySpy();
		DataProvider.onlyForTestSetDataFactory(dataFactorySpy);
		dataCopierFactorySpy = new DataCopierFactorySpy();
		DataCopierProvider.setDataCopierFactory(dataCopierFactorySpy);
	}

	private void setUpDependencyProvider() {
		dependencyProvider = new SpiderDependencyProviderOldSpy();
		dependencyProvider.authenticator = authenticator;
		dependencyProvider.spiderAuthorizator = authorizator;
		dependencyProvider.dataValidator = dataValidator;
		dependencyProvider.recordStorage = recordStorage;
		dependencyProvider.ruleCalculator = keyCalculator;
		dependencyProvider.recordSearch = recordSearch;
		dataGroupToRecordEnhancer = new DataGroupToRecordEnhancerSpy();
		dependencyProvider.termCollector = termCollector;
		dependencyProvider.dataRedactor = dataRedactor;
		recordSearcher = RecordSearcherImp.usingDependencyProviderAndDataGroupToRecordEnhancer(
				dependencyProvider, dataGroupToRecordEnhancer);
		DataListSpy dataListSpy = new DataListSpy("mix");
		dataFactorySpy.MRV.setDefaultReturnValuesSupplier("factorListUsingNameOfDataType",
				() -> dataListSpy);
	}

	@Test(expectedExceptions = AuthenticationException.class)
	public void testAuthenticationNotAuthenticated() {
		authenticator.throwAuthenticationException = true;
		recordStorage = new OldRecordStorageSpy();
		setUpDependencyProvider();
		String searchId = "someSearchId";
		String authToken = "dummyNonAuthenticatedToken";
		recordSearcher.search(authToken, searchId, someSearchData);
	}

	@Test
	public void testDefaultFromNoSetToOne() {
		DataList searchResult = recordSearcher.search(SOME_AUTH_TOKEN, A_SEARCH_ID, someSearchData);
		assertEquals(searchResult.getFromNo(), "1");
	}

	@Test
	public void testStartRowReadFromInput() throws Exception {
		someSearchData.addChild(new DataAtomicSpy("start", "2"));

		DataList searchResult = recordSearcher.search(SOME_AUTH_TOKEN, A_SEARCH_ID, someSearchData);

		assertEquals(searchResult.getFromNo(), "2");
	}

	@Test
	public void testSearchAuthenticated() {
		DataList searchResult = recordSearcher.search(SOME_AUTH_TOKEN, A_SEARCH_ID, someSearchData);
		assertNotNull(searchResult);

		String methodName = "checkUserIsAuthorizedForActionOnRecordType";
		authorizator.MCR.assertParameters(methodName, 0, authenticator.returnedUser, "search",
				"place");

		// termCollector.MCR.assertParameter("collectTerms", 0, "metadataId", "place");
		// termCollector.MCR.assertParameter("collectTerms", 0, "dataGroup",
		// ((RecordSearchSpy) recordSearch).place44);

		// String methodName2 = "userIsAuthorizedForActionOnRecordTypeAndCollectedData";
		// authorizator.MCR.assertParameters(methodName2, 0, authenticator.returnedUser, "read",
		// "place", termCollector.MCR.getReturnValue("collectTerms", 0));
	}

	@Test(expectedExceptions = AuthorizationException.class)
	public void testReadListAuthenticatedAndUnauthorized() {
		authorizator.authorizedForActionAndRecordType = false;
		recordSearcher.search(SOME_AUTH_TOKEN, A_SEARCH_ID, someSearchData);
	}

	@Test(expectedExceptions = AuthorizationException.class)
	public void testReadListAuthenticatedAndUnauthorizedNoRightToOneRecordTypeToSearchIn() {
		authorizator.setNotAutorizedForActionOnRecordType("search", "image");

		recordSearcher.search(SOME_AUTH_TOKEN, ANOTHER_SEARCH_ID, someSearchData);
	}

	@Test(expectedExceptions = DataException.class)
	public void testSearchAuthenticatedAndAuthorizedInvalidData() {
		dataValidator.validValidation = false;
		recordSearcher.search(SOME_AUTH_TOKEN, A_SEARCH_ID, someSearchData);
	}

	@Test
	public void testRecordSearchIsCalled() {
		recordSearcher.search(SOME_AUTH_TOKEN, A_SEARCH_ID, someSearchData);
		RecordSearchSpy recordSearchSpy = (RecordSearchSpy) recordSearch;
		List<String> list = recordSearchSpy.listOfLists.get(0);
		assertEquals(list.get(0), "place");
		assertEquals(list.size(), 1);
		DataGroup searchData0 = recordSearchSpy.listOfSearchData.get(0);
		assertEquals(searchData0.getNameInData(), someSearchData.getNameInData());
	}

	@Test
	public void testSearchResultIsEnhancedForEachResult() {
		authorizator.setNotAutorizedForActionOnRecordType("create", "recordType");
		authorizator.setNotAutorizedForActionOnRecordType("list", "recordType");
		authorizator.setNotAutorizedForActionOnRecordType("search", "recordType");

		DataList searchResult = recordSearcher.search(SOME_AUTH_TOKEN, A_SEARCH_ID, someSearchData);
		assertEquals(searchResult.getDataList().size(),
				dataGroupToRecordEnhancer.enhancedDataGroups.size());
		assertEquals(dataGroupToRecordEnhancer.recordType, "place");

		DataRecord dataRecord = (DataRecord) searchResult.getDataList().get(0);

		dataGroupToRecordEnhancer.MCR.assertParameters("enhance", 0, authenticator.returnedUser,
				"place", dataRecord.getDataGroup(), dataRedactor);

		assertEquals(searchResult.getFromNo(), "1");
		assertEquals(searchResult.getToNo(), String.valueOf(searchResult.getDataList().size()));
		assertEquals(searchResult.getTotalNumberOfTypeInStorage(),
				String.valueOf(searchResult.getDataList().size()));
	}

	@Test
	public void testSearchResultIsFilteredAndEnhancedForEachResult() {

		DataList searchResult = recordSearcher.search(SOME_AUTH_TOKEN, ANOTHER_SEARCH_ID,
				someSearchData);
		int numberOfMatchesFetched = searchResult.getDataList().size();
		assertEquals(numberOfMatchesFetched, 3);
		assertEquals(searchResult.getFromNo(), "1");
		assertEquals(searchResult.getToNo(), String.valueOf(numberOfMatchesFetched));
	}

	@Test
	public void testSameRedactorUsedWhenEnhancing() {
		DataList searchResult = recordSearcher.search(SOME_AUTH_TOKEN, ANOTHER_SEARCH_ID,
				someSearchData);
		int numberOfMatchesFetched = searchResult.getDataList().size();
		assertEquals(numberOfMatchesFetched, 3);
		dependencyProvider.MCR.assertNumberOfCallsToMethod("getDataRedactor", 1);
	}

	@Test
	public void testSearchResultOnlyFirstResultHasReadAccess() {
		dataGroupToRecordEnhancer.addReadActionOnlyFirst = true;

		DataList searchResult = recordSearcher.search(SOME_AUTH_TOKEN, ANOTHER_SEARCH_ID,
				someSearchData);
		int numberOfMatchesFetched = searchResult.getDataList().size();
		assertEquals(numberOfMatchesFetched, 1);
		assertEquals(searchResult.getFromNo(), "1");
		assertEquals(searchResult.getToNo(), String.valueOf(numberOfMatchesFetched));
	}

	@Test
	public void testSearchResultHasTotalNumberOfMatches() {
		long searchMatches = 42;

		((RecordSearchSpy) recordSearch).totalNumberOfMatches = searchMatches;

		setUpDependencyProvider();
		authorizator.authorizedForActionAndRecordTypeAndCollectedData = false;

		DataList searchResult = recordSearcher.search(SOME_AUTH_TOKEN, A_SEARCH_ID, someSearchData);

		assertEquals(searchResult.getTotalNumberOfTypeInStorage(), String.valueOf(searchMatches));
	}

	@Test
	public void testSearchResultHasAnotherTotalNumberOfMatches() {
		long searchMatches = Long.MAX_VALUE;
		((RecordSearchSpy) recordSearch).totalNumberOfMatches = searchMatches;

		setUpDependencyProvider();
		authorizator.authorizedForActionAndRecordTypeAndCollectedData = false;

		DataList searchResult = recordSearcher.search(SOME_AUTH_TOKEN, A_SEARCH_ID, someSearchData);

		assertEquals(searchResult.getTotalNumberOfTypeInStorage(), String.valueOf(searchMatches));
	}

}
