/*
 * Copyright 2015, 2016 Uppsala University Library
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
import java.util.List;

import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import se.uu.ub.cora.bookkeeper.data.DataGroup;
import se.uu.ub.cora.spider.authentication.AuthenticationException;
import se.uu.ub.cora.spider.authentication.Authenticator;
import se.uu.ub.cora.spider.authentication.AuthenticatorSpy;
import se.uu.ub.cora.spider.authorization.AuthorizationException;
import se.uu.ub.cora.spider.authorization.NeverAuthorisedStub;
import se.uu.ub.cora.spider.authorization.PermissionRuleCalculator;
import se.uu.ub.cora.spider.authorization.SpiderAuthorizator;
import se.uu.ub.cora.spider.data.SpiderData;
import se.uu.ub.cora.spider.data.SpiderDataAtomic;
import se.uu.ub.cora.spider.data.SpiderDataGroup;
import se.uu.ub.cora.spider.data.SpiderDataList;
import se.uu.ub.cora.spider.data.SpiderDataRecord;
import se.uu.ub.cora.spider.dependency.SpiderDependencyProviderSpy;
import se.uu.ub.cora.spider.record.storage.RecordStorage;
import se.uu.ub.cora.spider.spy.AuthorizatorAlwaysAuthorizedSpy;
import se.uu.ub.cora.spider.spy.NoRulesCalculatorStub;
import se.uu.ub.cora.spider.spy.RecordStorageSpy;
import se.uu.ub.cora.spider.testdata.TestDataRecordInMemoryStorage;

public class SpiderRecordListReaderTest {

	private RecordStorage recordStorage;
	private Authenticator authenticator;
	private SpiderAuthorizator authorizator;
	private PermissionRuleCalculator keyCalculator;
	private SpiderDependencyProviderSpy dependencyProvider;
	private SpiderRecordListReader recordListReader;
	private DataGroupToRecordEnhancerSpy dataGroupToRecordEnhancer;
	private SpiderDataGroup emptyFilter = SpiderDataGroup.withNameInData("filter");

	@BeforeMethod
	public void beforeMethod() {
		authenticator = new AuthenticatorSpy();
		authorizator = new AuthorizatorAlwaysAuthorizedSpy();
		recordStorage = TestDataRecordInMemoryStorage.createRecordStorageInMemoryWithTestData();
		keyCalculator = new NoRulesCalculatorStub();
		setUpDependencyProvider();
	}

	private void setUpDependencyProvider() {
		dependencyProvider = new SpiderDependencyProviderSpy(new HashMap<>());
		dependencyProvider.authenticator = authenticator;
		dependencyProvider.spiderAuthorizator = authorizator;
		dependencyProvider.recordStorage = recordStorage;
		dependencyProvider.keyCalculator = keyCalculator;
		dataGroupToRecordEnhancer = new DataGroupToRecordEnhancerSpy();
		recordListReader = SpiderRecordListReaderImp.usingDependencyProviderAndDataGroupToRecordEnhancer(
				dependencyProvider, dataGroupToRecordEnhancer);
	}

	@Test(expectedExceptions = AuthenticationException.class)
	public void testAuthenticationNotAuthenticated() {
		recordStorage = new RecordStorageSpy();
		setUpDependencyProvider();
		recordListReader.readRecordList("dummyNonAuthenticatedToken", "spyType", emptyFilter);
	}

	@Test
	public void testReadListAuthorized() {
		String userId = "someToken78678567";
		String type = "place";
		SpiderDataList readRecordList = recordListReader.readRecordList(userId, type, emptyFilter);
		assertEquals(readRecordList.getTotalNumberOfTypeInStorage(), "4",
				"Total number of records should be 4");
		assertEquals(readRecordList.getFromNo(), "1");
		assertEquals(readRecordList.getToNo(), "4");
		List<SpiderData> records = readRecordList.getDataList();
		SpiderDataRecord spiderDataRecord = (SpiderDataRecord) records.iterator().next();
		assertNotNull(spiderDataRecord);
	}

	@Test
	public void testReadListFilterIsPassedOnToStorage() {
		recordStorage = new RecordStorageSpy();
		setUpDependencyProvider();

		SpiderDataGroup filter = SpiderDataGroup.withNameInData("filter");
		SpiderDataGroup part = SpiderDataGroup.withNameInData("part");
		filter.addChild(part);
		part.addChild(SpiderDataAtomic.withNameInDataAndValue("key", "someKey"));
		part.addChild(SpiderDataAtomic.withNameInDataAndValue("value", "someValue"));

		String userId = "someToken78678567";
		String type = "place";
		SpiderDataList readRecordList = recordListReader.readRecordList(userId, type, filter);

		DataGroup filterFromStorage = ((RecordStorageSpy) recordStorage).filters.get(0);
		assertEquals(filterFromStorage.getNameInData(), "filter");
		DataGroup extractedPart = filterFromStorage.getFirstGroupWithNameInData("part");
		assertEquals(extractedPart.getFirstAtomicValueWithNameInData("key"), "someKey");
		assertEquals(extractedPart.getFirstAtomicValueWithNameInData("value"), "someValue");
	}

	@Test
	public void testReadListAuthorizedButNoReadLinks() {
		dataGroupToRecordEnhancer.addReadAction = false;
		String userId = "someToken78678567";
		String type = "place";
		SpiderDataList readRecordList = recordListReader.readRecordList(userId, type, emptyFilter);
		assertEquals(readRecordList.getTotalNumberOfTypeInStorage(), "0",
				"Total number of records should be ");
		assertEquals(readRecordList.getFromNo(), "1");
		assertEquals(readRecordList.getToNo(), "0");
		List<SpiderData> records = readRecordList.getDataList();
		assertEquals(records.size(), 0);
	}

	@Test
	public void testRecordEnhancerCalled() {
		recordListReader.readRecordList("someToken78678567", "place", emptyFilter);
		assertEquals(dataGroupToRecordEnhancer.user.id, "12345");
		assertEquals(dataGroupToRecordEnhancer.recordType, "place");
		assertEquals(dataGroupToRecordEnhancer.dataGroup.getFirstGroupWithNameInData("recordInfo")
				.getFirstAtomicValueWithNameInData("id"), "place:0004");
	}

	@Test
	public void testReadListAbstractRecordType() {
		recordStorage = new RecordStorageSpy();
		setUpDependencyProvider();
		SpiderDataList spiderDataList = recordListReader.readRecordList("someToken78678567", "abstract",
				emptyFilter);
		assertEquals(spiderDataList.getTotalNumberOfTypeInStorage(), "2");

		String type1 = extractTypeFromChildInListUsingIndex(spiderDataList, 0);
		assertEquals(type1, "implementing1");
		String type2 = extractTypeFromChildInListUsingIndex(spiderDataList, 1);
		assertEquals(type2, "implementing2");
	}

	@Test
	public void testRecordEnhancerCalledForAbstractType() {
		recordStorage = new RecordStorageSpy();
		setUpDependencyProvider();
		recordListReader.readRecordList("someToken78678567", "abstract", emptyFilter);
		assertEquals(dataGroupToRecordEnhancer.user.id, "12345");
		assertEquals(dataGroupToRecordEnhancer.recordType, "implementing2");
		assertEquals(dataGroupToRecordEnhancer.dataGroup.getFirstGroupWithNameInData("recordInfo")
				.getFirstAtomicValueWithNameInData("id"), "child2_2");
	}

	private String extractTypeFromChildInListUsingIndex(SpiderDataList spiderDataList, int index) {
		SpiderDataRecord spiderData1 = (SpiderDataRecord) spiderDataList.getDataList().get(index);
		SpiderDataGroup spiderDataGroup1 = spiderData1.getSpiderDataGroup();
		SpiderDataGroup recordInfo = spiderDataGroup1.extractGroup("recordInfo");
		SpiderDataGroup typeGroup = recordInfo.extractGroup("type");
		return typeGroup.extractAtomicValue("linkedRecordId");
	}

	@Test
	public void testReadListAbstractRecordTypeNoDataForOneRecordType() {
		recordStorage = new RecordStorageSpy();
		setUpDependencyProvider();

		SpiderDataList spiderDataList = recordListReader.readRecordList("someToken78678567", "abstract2",
				emptyFilter);
		assertEquals(spiderDataList.getTotalNumberOfTypeInStorage(), "1");

		String type1 = extractTypeFromChildInListUsingIndex(spiderDataList, 0);
		assertEquals(type1, "implementing2");

	}

	@Test(expectedExceptions = AuthorizationException.class)
	public void testReadListUnauthorized() {
		authorizator = new NeverAuthorisedStub();
		setUpDependencyProvider();
		recordListReader.readRecordList("someToken78678567", "place", emptyFilter);
	}
}
