/*
 * Copyright 2017 Uppsala University Library
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

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import se.uu.ub.cora.bookkeeper.validator.DataValidator;
import se.uu.ub.cora.spider.authentication.AuthenticationException;
import se.uu.ub.cora.spider.authentication.Authenticator;
import se.uu.ub.cora.spider.authentication.AuthenticatorSpy;
import se.uu.ub.cora.spider.authorization.AlwaysAuthorisedExceptStub;
import se.uu.ub.cora.spider.authorization.AuthorizationException;
import se.uu.ub.cora.spider.authorization.NeverAuthorisedStub;
import se.uu.ub.cora.spider.authorization.PermissionRuleCalculator;
import se.uu.ub.cora.spider.authorization.SpiderAuthorizator;
import se.uu.ub.cora.spider.data.SpiderDataGroup;
import se.uu.ub.cora.spider.data.SpiderDataList;
import se.uu.ub.cora.spider.dependency.SpiderDependencyProviderSpy;
import se.uu.ub.cora.spider.record.storage.RecordStorage;
import se.uu.ub.cora.spider.spy.AuthorizatorAlwaysAuthorizedSpy;
import se.uu.ub.cora.spider.spy.DataValidatorAlwaysInvalidSpy;
import se.uu.ub.cora.spider.spy.DataValidatorAlwaysValidSpy;
import se.uu.ub.cora.spider.spy.NoRulesCalculatorStub;
import se.uu.ub.cora.spider.spy.RecordStorageSpy;
import se.uu.ub.cora.spider.testdata.TestDataRecordInMemoryStorage;

public class SpiderRecordSearcherTest {
	private RecordStorage recordStorage;
	private Authenticator authenticator;
	private SpiderAuthorizator authorizator;
	private PermissionRuleCalculator keyCalculator;
	private SpiderDependencyProviderSpy dependencyProvider;
	private DataGroupToRecordEnhancerSpy dataGroupToRecordEnhancer;
	private SpiderRecordSearcher recordSearcher;
	private DataValidator dataValidator;
	private RecordSearch recordSearch;

	@BeforeMethod
	public void beforeMethod() {
		authenticator = new AuthenticatorSpy();
		authorizator = new AuthorizatorAlwaysAuthorizedSpy();
		dataValidator = new DataValidatorAlwaysValidSpy();
		recordStorage = TestDataRecordInMemoryStorage.createRecordStorageInMemoryWithTestData();
		keyCalculator = new NoRulesCalculatorStub();
		recordSearch = new RecordSearchSpy();
		setUpDependencyProvider();
	}

	private void setUpDependencyProvider() {
		dependencyProvider = new SpiderDependencyProviderSpy(new HashMap<>());
		dependencyProvider.authenticator = authenticator;
		dependencyProvider.spiderAuthorizator = authorizator;
		dependencyProvider.dataValidator = dataValidator;
		dependencyProvider.recordStorage = recordStorage;
		dependencyProvider.keyCalculator = keyCalculator;
		dependencyProvider.recordSearch = recordSearch;
		dataGroupToRecordEnhancer = new DataGroupToRecordEnhancerSpy();
		recordSearcher = SpiderRecordSearcherImp
				.usingDependencyProviderAndDataGroupToRecordEnhancer(dependencyProvider,
						dataGroupToRecordEnhancer);
	}

	@Test(expectedExceptions = AuthenticationException.class)
	public void testAuthenticationNotAuthenticated() {
		recordStorage = new RecordStorageSpy();
		setUpDependencyProvider();
		SpiderDataGroup searchData = SpiderDataGroup.withNameInData("search");
		String searchId = "someSearchId";
		String authToken = "dummyNonAuthenticatedToken";
		recordSearcher.search(authToken, searchId, searchData);
	}

	@Test
	public void testReadListAuthenticatedAndAuthorized() {
		String authToken = "someToken78678567";
		String searchId = "aSearchId";
		SpiderDataGroup searchData = SpiderDataGroup.withNameInData("search");
		SpiderDataList searchResult = recordSearcher.search(authToken, searchId, searchData);
		assertNotNull(searchResult);
	}

	@Test(expectedExceptions = AuthorizationException.class)
	public void testReadListAuthenticatedAndUnauthorized() {
		authorizator = new NeverAuthorisedStub();
		setUpDependencyProvider();
		String authToken = "someToken78678567";
		String searchId = "aSearchId";
		SpiderDataGroup searchData = SpiderDataGroup.withNameInData("search");
		recordSearcher.search(authToken, searchId, searchData);
	}

	@Test(expectedExceptions = AuthorizationException.class)
	public void testReadListAuthenticatedAndUnauthorizedNoRightToOneRecordTypeToSearchIn() {
		authorizator = new AlwaysAuthorisedExceptStub();
		HashSet<String> hashSet = new HashSet<String>();
		hashSet.add("search");
		((AlwaysAuthorisedExceptStub) authorizator).notAuthorizedForRecordTypeAndActions
				.put("image", hashSet);
		setUpDependencyProvider();
		String authToken = "someToken78678567";
		String searchId = "anotherSearchId";
		SpiderDataGroup searchData = SpiderDataGroup.withNameInData("search");
		recordSearcher.search(authToken, searchId, searchData);
	}

	@Test(expectedExceptions = DataException.class)
	public void testReadListAuthenticatedAndAuthorizedInvalidData() {
		dataValidator = new DataValidatorAlwaysInvalidSpy();
		setUpDependencyProvider();
		String authToken = "someToken78678567";
		String searchId = "aSearchId";
		SpiderDataGroup searchData = SpiderDataGroup.withNameInData("search");
		recordSearcher.search(authToken, searchId, searchData);
	}

	@Test
	public void testRecordSearchIsCalled() {
		String authToken = "someToken78678567";
		String searchId = "aSearchId";
		SpiderDataGroup searchData = SpiderDataGroup.withNameInData("search");
		recordSearcher.search(authToken, searchId, searchData);
		RecordSearchSpy recordSearchSpy = (RecordSearchSpy) recordSearch;
		List<String> list = recordSearchSpy.listOfLists.get(0);
		assertEquals(list.get(0), "place");
		assertEquals(list.size(), 1);
		SpiderDataGroup searchData0 = recordSearchSpy.listOfSearchData.get(0);
		assertEquals(searchData0.getNameInData(), searchData.getNameInData());
	}

	@Test
	public void testSearchResultIsEnhancedForEachResult() {
		authorizator = new AlwaysAuthorisedExceptStub();
		Set<String> actions = new HashSet<>();
		actions.add("create");
		actions.add("list");
		actions.add("search");
		((AlwaysAuthorisedExceptStub) authorizator).notAuthorizedForRecordTypeAndActions
				.put("recordType", actions);
		setUpDependencyProvider();

		String authToken = "someToken78678567";
		String searchId = "aSearchId";
		SpiderDataGroup searchData = SpiderDataGroup.withNameInData("search");
		SpiderDataList searchResult = recordSearcher.search(authToken, searchId, searchData);
		assertEquals(searchResult.getDataList().size(),
				dataGroupToRecordEnhancer.enhancedDataGroups.size());
		assertEquals(dataGroupToRecordEnhancer.recordType, "place");
	}

	@Test
	public void testSearchResultIsFilteredAndEnhancedForEachResult() {
		authorizator = new AlwaysAuthorisedExceptStub();
		((AlwaysAuthorisedExceptStub) authorizator).notAuthorizedForIds.add("image45");
		setUpDependencyProvider();

		String authToken = "someToken78678567";
		String searchId = "anotherSearchId";
		SpiderDataGroup searchData = SpiderDataGroup.withNameInData("search");
		SpiderDataList searchResult = recordSearcher.search(authToken, searchId, searchData);
		assertEquals(searchResult.getDataList().size(), 2);

		// SpiderDataRecord spiderDataRecord = (SpiderDataRecord)
		// searchResult.getDataList().get(0);
		// assertEquals(spiderDataRecord.getActions().size(),2);

		// RecordSearchSpy recordSearchSpy = (RecordSearchSpy) recordSearch;
		// List<String> list = recordSearchSpy.listOfLists.get(0);
		// assertEquals(list.get(0), "place");
		// assertEquals(list.size(), 1);
		// DataGroup searchData0 = recordSearchSpy.listOfSearchData.get(0);
		// assertEquals(searchData0.getNameInData(),
		// searchData.getNameInData());
	}
}
