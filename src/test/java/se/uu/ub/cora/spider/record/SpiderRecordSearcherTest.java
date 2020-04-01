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
package se.uu.ub.cora.spider.record;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertNotNull;
import static org.testng.Assert.assertSame;
import static org.testng.Assert.assertTrue;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import se.uu.ub.cora.bookkeeper.termcollector.DataGroupTermCollector;
import se.uu.ub.cora.bookkeeper.validator.DataValidator;
import se.uu.ub.cora.data.DataAtomicFactory;
import se.uu.ub.cora.data.DataAtomicProvider;
import se.uu.ub.cora.data.DataGroup;
import se.uu.ub.cora.data.DataGroupFactory;
import se.uu.ub.cora.data.DataGroupProvider;
import se.uu.ub.cora.data.DataList;
import se.uu.ub.cora.data.DataListFactory;
import se.uu.ub.cora.data.DataListProvider;
import se.uu.ub.cora.data.copier.DataCopierFactory;
import se.uu.ub.cora.data.copier.DataCopierProvider;
import se.uu.ub.cora.logger.LoggerProvider;
import se.uu.ub.cora.search.RecordSearch;
import se.uu.ub.cora.spider.authentication.AuthenticationException;
import se.uu.ub.cora.spider.authentication.AuthenticatorSpy;
import se.uu.ub.cora.spider.authorization.AuthorizationException;
import se.uu.ub.cora.spider.authorization.PermissionRuleCalculator;
import se.uu.ub.cora.spider.data.DataAtomicFactorySpy;
import se.uu.ub.cora.spider.data.DataAtomicSpy;
import se.uu.ub.cora.spider.data.DataGroupFactorySpy;
import se.uu.ub.cora.spider.data.DataGroupSpy;
import se.uu.ub.cora.spider.dependency.RecordStorageProviderSpy;
import se.uu.ub.cora.spider.dependency.SpiderDependencyProviderSpy;
import se.uu.ub.cora.spider.log.LoggerFactorySpy;
import se.uu.ub.cora.spider.spy.DataGroupTermCollectorSpy;
import se.uu.ub.cora.spider.spy.DataValidatorAlwaysInvalidSpy;
import se.uu.ub.cora.spider.spy.DataValidatorAlwaysValidSpy;
import se.uu.ub.cora.spider.spy.NoRulesCalculatorStub;
import se.uu.ub.cora.spider.spy.OldRecordStorageSpy;
import se.uu.ub.cora.spider.spy.SpiderAuthorizatorSpy;
import se.uu.ub.cora.spider.testdata.TestDataRecordInMemoryStorage;
import se.uu.ub.cora.storage.RecordStorage;

public class SpiderRecordSearcherTest {
	private static final String A_SEARCH_ID = "aSearchId";
	private static final String ANOTHER_SEARCH_ID = "anotherSearchId";
	private static final String SOME_AUTH_TOKEN = "someToken78678567";
	private final DataGroup someSearchData = new DataGroupSpy("search");

	private RecordStorage recordStorage;
	private AuthenticatorSpy authenticator;
	private SpiderAuthorizatorSpy spiderAuthorizator;
	private PermissionRuleCalculator keyCalculator;
	private DataGroupToRecordEnhancerSpy dataGroupToRecordEnhancer;
	private SpiderRecordSearcher recordSearcher;
	private DataValidator dataValidator;
	private RecordSearch recordSearch;
	private DataGroupTermCollector termCollector;
	private LoggerFactorySpy loggerFactorySpy;
	private DataGroupFactory dataGroupFactorySpy;
	private DataAtomicFactory dataAtomicFactorySpy;
	private DataListFactory dataListFactory;
	private DataCopierFactory dataCopierFactorySpy;

	@BeforeMethod
	public void beforeMethod() {
		setUpFactoriesAndProviders();

		authenticator = new AuthenticatorSpy();
		spiderAuthorizator = new SpiderAuthorizatorSpy();
		dataValidator = new DataValidatorAlwaysValidSpy();
		recordStorage = TestDataRecordInMemoryStorage.createRecordStorageInMemoryWithTestData();
		keyCalculator = new NoRulesCalculatorStub();
		recordSearch = new RecordSearchSpy();
		termCollector = new DataGroupTermCollectorSpy();
		setUpDependencyProvider();
	}

	private void setUpFactoriesAndProviders() {
		loggerFactorySpy = new LoggerFactorySpy();
		LoggerProvider.setLoggerFactory(loggerFactorySpy);
		dataGroupFactorySpy = new DataGroupFactorySpy();
		DataGroupProvider.setDataGroupFactory(dataGroupFactorySpy);
		dataAtomicFactorySpy = new DataAtomicFactorySpy();
		DataAtomicProvider.setDataAtomicFactory(dataAtomicFactorySpy);
		dataListFactory = new DataListFactorySpy();
		DataListProvider.setDataListFactory(dataListFactory);
		dataCopierFactorySpy = new DataCopierFactorySpy();
		DataCopierProvider.setDataCopierFactory(dataCopierFactorySpy);
	}

	private void setUpDependencyProvider() {
		SpiderDependencyProviderSpy dependencyProvider;
		dependencyProvider = new SpiderDependencyProviderSpy(new HashMap<>());
		dependencyProvider.authenticator = authenticator;
		dependencyProvider.spiderAuthorizator = spiderAuthorizator;
		dependencyProvider.dataValidator = dataValidator;
		RecordStorageProviderSpy recordStorageProviderSpy = new RecordStorageProviderSpy();
		recordStorageProviderSpy.recordStorage = recordStorage;
		dependencyProvider.setRecordStorageProvider(recordStorageProviderSpy);
		dependencyProvider.ruleCalculator = keyCalculator;
		dependencyProvider.recordSearch = recordSearch;
		dataGroupToRecordEnhancer = new DataGroupToRecordEnhancerSpy();
		dependencyProvider.searchTermCollector = termCollector;
		recordSearcher = SpiderRecordSearcherImp
				.usingDependencyProviderAndDataGroupToRecordEnhancer(dependencyProvider,
						dataGroupToRecordEnhancer);
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
	public void testReadListAuthenticatedAndAuthorized() {
		DataList searchResult = recordSearcher.search(SOME_AUTH_TOKEN, A_SEARCH_ID, someSearchData);
		assertNotNull(searchResult);

		Map<String, Object> firstParameters = spiderAuthorizator.TCR
				.getParametersForMethodAndCallNumber("checkUserIsAuthorizedForActionOnRecordType",
						0);

		assertSame(firstParameters.get("user"), authenticator.returnedUser);
		assertEquals(firstParameters.get("action"), "search");
		assertEquals(firstParameters.get("recordType"), "place");

		DataGroupTermCollectorSpy dataGroupTermCollectorSpy = (DataGroupTermCollectorSpy) termCollector;
		assertEquals(dataGroupTermCollectorSpy.metadataId, "place");
		assertEquals(dataGroupTermCollectorSpy.dataGroup, ((RecordSearchSpy) recordSearch).place44);

		Map<String, Object> secondParameters = spiderAuthorizator.TCR
				.getParametersForMethodAndCallNumber(
						"userIsAuthorizedForActionOnRecordTypeAndCollectedData", 0);

		assertTrue(spiderAuthorizator.TCR
				.methodWasCalled("userIsAuthorizedForActionOnRecordTypeAndCollectedData"));

		DataGroup returnedCollectedTerms = dataGroupTermCollectorSpy.collectedTerms;
		assertEquals(secondParameters.get("collectedData"), returnedCollectedTerms);
	}

	@Test(expectedExceptions = AuthorizationException.class)
	public void testReadListAuthenticatedAndUnauthorized() {
		spiderAuthorizator.authorizedForActionAndRecordType = false;
		recordSearcher.search(SOME_AUTH_TOKEN, A_SEARCH_ID, someSearchData);
	}

	@Test(expectedExceptions = AuthorizationException.class)
	public void testReadListAuthenticatedAndUnauthorizedNoRightToOneRecordTypeToSearchIn() {
		spiderAuthorizator.setNotAutorizedForActionOnRecordType("search", "image");

		recordSearcher.search(SOME_AUTH_TOKEN, ANOTHER_SEARCH_ID, someSearchData);
	}

	@Test(expectedExceptions = DataException.class)
	public void testReadListAuthenticatedAndAuthorizedInvalidData() {
		dataValidator = new DataValidatorAlwaysInvalidSpy();
		setUpDependencyProvider();
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
		spiderAuthorizator.setNotAutorizedForActionOnRecordType("create", "recordType");
		spiderAuthorizator.setNotAutorizedForActionOnRecordType("list", "recordType");
		spiderAuthorizator.setNotAutorizedForActionOnRecordType("search", "recordType");

		DataList searchResult = recordSearcher.search(SOME_AUTH_TOKEN, A_SEARCH_ID, someSearchData);
		assertEquals(searchResult.getDataList().size(),
				dataGroupToRecordEnhancer.enhancedDataGroups.size());
		assertEquals(dataGroupToRecordEnhancer.recordType, "place");
		assertEquals(searchResult.getFromNo(), "1");
		assertEquals(searchResult.getToNo(), String.valueOf(searchResult.getDataList().size()));
		assertEquals(searchResult.getTotalNumberOfTypeInStorage(),
				String.valueOf(searchResult.getDataList().size()));
	}

	@Test
	public void testSearchResultIsFilteredAndEnhancedForEachResult() {
		spiderAuthorizator.setNotAutorizedForActionOnRecordType("read", "binary");

		DataList searchResult = recordSearcher.search(SOME_AUTH_TOKEN, ANOTHER_SEARCH_ID,
				someSearchData);
		int numberOfMatchesFetched = searchResult.getDataList().size();
		assertEquals(numberOfMatchesFetched, 2);
		assertEquals(searchResult.getFromNo(), "1");
		assertEquals(searchResult.getToNo(), String.valueOf(numberOfMatchesFetched));
	}

	@Test
	public void testSearchResultHasTotalNumberOfMatches() {
		long searchMatches = 42;

		((RecordSearchSpy) recordSearch).totalNumberOfMatches = searchMatches;

		setUpDependencyProvider();
		spiderAuthorizator.authorizedForActionAndRecordTypeAndCollectedData = false;

		DataList searchResult = recordSearcher.search(SOME_AUTH_TOKEN, A_SEARCH_ID, someSearchData);

		assertEquals(searchResult.getTotalNumberOfTypeInStorage(), String.valueOf(searchMatches));
	}

	@Test
	public void testSearchResultHasAnotherTotalNumberOfMatches() {
		long searchMatches = Long.MAX_VALUE;
		((RecordSearchSpy) recordSearch).totalNumberOfMatches = searchMatches;

		setUpDependencyProvider();
		spiderAuthorizator.authorizedForActionAndRecordTypeAndCollectedData = false;

		DataList searchResult = recordSearcher.search(SOME_AUTH_TOKEN, A_SEARCH_ID, someSearchData);

		assertEquals(searchResult.getTotalNumberOfTypeInStorage(), String.valueOf(searchMatches));
	}

}
