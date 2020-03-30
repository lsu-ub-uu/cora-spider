/*
 * Copyright 2015, 2016, 2018, 2019 Uppsala University Library
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
import static org.testng.Assert.fail;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.stream.Collectors;

import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import se.uu.ub.cora.bookkeeper.validator.DataValidator;
import se.uu.ub.cora.data.Data;
import se.uu.ub.cora.data.DataAtomic;
import se.uu.ub.cora.data.DataAtomicProvider;
import se.uu.ub.cora.data.DataGroup;
import se.uu.ub.cora.data.DataGroupFactory;
import se.uu.ub.cora.data.DataGroupProvider;
import se.uu.ub.cora.data.DataList;
import se.uu.ub.cora.data.DataListFactory;
import se.uu.ub.cora.data.DataListProvider;
import se.uu.ub.cora.data.DataRecord;
import se.uu.ub.cora.data.copier.DataCopierFactory;
import se.uu.ub.cora.data.copier.DataCopierProvider;
import se.uu.ub.cora.logger.LoggerProvider;
import se.uu.ub.cora.spider.authentication.AuthenticationException;
import se.uu.ub.cora.spider.authentication.Authenticator;
import se.uu.ub.cora.spider.authentication.AuthenticatorSpy;
import se.uu.ub.cora.spider.authorization.AlwaysAuthorisedExceptStub;
import se.uu.ub.cora.spider.authorization.AuthorizationException;
import se.uu.ub.cora.spider.authorization.NeverAuthorisedStub;
import se.uu.ub.cora.spider.authorization.PermissionRuleCalculator;
import se.uu.ub.cora.spider.authorization.SpiderAuthorizator;
import se.uu.ub.cora.spider.data.DataAtomicFactorySpy;
import se.uu.ub.cora.spider.data.DataAtomicSpy;
import se.uu.ub.cora.spider.data.DataGroupFactorySpy;
import se.uu.ub.cora.spider.data.DataGroupSpy;
import se.uu.ub.cora.spider.dependency.RecordStorageProviderSpy;
import se.uu.ub.cora.spider.dependency.RecordTypeHandlerSpy;
import se.uu.ub.cora.spider.dependency.SpiderDependencyProviderSpy;
import se.uu.ub.cora.spider.log.LoggerFactorySpy;
import se.uu.ub.cora.spider.spy.AuthorizatorAlwaysAuthorizedSpy;
import se.uu.ub.cora.spider.spy.DataValidatorAlwaysInvalidSpy;
import se.uu.ub.cora.spider.spy.DataValidatorAlwaysValidSpy;
import se.uu.ub.cora.spider.spy.NoRulesCalculatorStub;
import se.uu.ub.cora.spider.spy.OldRecordStorageSpy;
import se.uu.ub.cora.spider.spy.RuleCalculatorSpy;
import se.uu.ub.cora.spider.testdata.TestDataRecordInMemoryStorage;
import se.uu.ub.cora.storage.RecordStorage;

public class SpiderRecordListReaderTest {

	private RecordStorage recordStorage;
	private Authenticator authenticator;
	private SpiderAuthorizator authorizator;
	private PermissionRuleCalculator keyCalculator;
	private SpiderRecordListReader recordListReader;
	private DataGroupToRecordEnhancerSpy dataGroupToRecordEnhancer;
	private DataValidator dataValidator;
	private DataGroup emptyFilter;
	private DataGroup exampleFilter;
	private LoggerFactorySpy loggerFactorySpy;

	private static final String SOME_USER_TOKEN = "someToken78678567";
	private static final String SOME_RECORD_TYPE = "place";
	private DataGroupFactory dataGroupFactory;
	private DataAtomicFactorySpy dataAtomicFactory;
	private DataListFactory dataListFactory;
	private DataCopierFactory dataCopierFactory;
	private RecordTypeHandlerSpy recordTypeHandlerSpy;

	@BeforeMethod
	public void beforeMethod() {
		setUpFactoriesAndProviders();
		emptyFilter = new DataGroupSpy("filter");
		exampleFilter = new DataGroupSpy("filter");
		authenticator = new AuthenticatorSpy();
		authorizator = new AuthorizatorAlwaysAuthorizedSpy();
		recordStorage = TestDataRecordInMemoryStorage.createRecordStorageInMemoryWithTestData();
		keyCalculator = new NoRulesCalculatorStub();
		dataValidator = new DataValidatorAlwaysValidSpy();
		setUpDependencyProvider();
	}

	private void setUpFactoriesAndProviders() {
		loggerFactorySpy = new LoggerFactorySpy();
		LoggerProvider.setLoggerFactory(loggerFactorySpy);
		dataGroupFactory = new DataGroupFactorySpy();
		DataGroupProvider.setDataGroupFactory(dataGroupFactory);
		dataAtomicFactory = new DataAtomicFactorySpy();
		DataAtomicProvider.setDataAtomicFactory(dataAtomicFactory);
		dataListFactory = new DataListFactorySpy();
		DataListProvider.setDataListFactory(dataListFactory);
		dataCopierFactory = new DataCopierFactorySpy();
		DataCopierProvider.setDataCopierFactory(dataCopierFactory);
	}

	private void setUpDependencyProvider() {
		SpiderDependencyProviderSpy dependencyProvider = new SpiderDependencyProviderSpy(
				new HashMap<>());
		dependencyProvider.authenticator = authenticator;
		dependencyProvider.spiderAuthorizator = authorizator;

		RecordStorageProviderSpy recordStorageProviderSpy = new RecordStorageProviderSpy();
		recordStorageProviderSpy.recordStorage = recordStorage;
		dependencyProvider.setRecordStorageProvider(recordStorageProviderSpy);

		dependencyProvider.ruleCalculator = keyCalculator;
		dependencyProvider.dataValidator = dataValidator;
		dataGroupToRecordEnhancer = new DataGroupToRecordEnhancerSpy();
		recordListReader = SpiderRecordListReaderImp
				.usingDependencyProviderAndDataGroupToRecordEnhancer(dependencyProvider,
						dataGroupToRecordEnhancer);
		recordTypeHandlerSpy = dependencyProvider.recordTypeHandlerSpy;
	}

	@Test
	public void testExternalDependenciesAreCalled() {
		recordStorage = new OldRecordStorageSpy();
		keyCalculator = new RuleCalculatorSpy();
		setUpDependencyProvider();

		DataGroup nonEmptyFilter = createNonEmptyFilter();
		recordListReader.readRecordList(SOME_USER_TOKEN, SOME_RECORD_TYPE, nonEmptyFilter);

		AuthorizatorAlwaysAuthorizedSpy authorizatorSpy = (AuthorizatorAlwaysAuthorizedSpy) authorizator;
		assertTrue(authorizatorSpy.authorizedWasCalled);

		DataValidatorAlwaysValidSpy dataValidatorAlwaysValidSpy = (DataValidatorAlwaysValidSpy) dataValidator;
		assertTrue(dataValidatorAlwaysValidSpy.validateDataWasCalled);

		assertDataGroupEquality(dataValidatorAlwaysValidSpy.dataGroup, nonEmptyFilter);

		assertTrue(((OldRecordStorageSpy) recordStorage).readListWasCalled);
	}

	@Test
	public void testFilterValidationIsCalledCorrectlyWithOtherFilter() {
		recordStorage = new OldRecordStorageSpy();
		setUpDependencyProvider();
		DataGroup filter = new DataGroupSpy("filter2");
		DataGroup part = new DataGroupSpy("part");
		filter.addChild(part);

		DataGroup nonEmptyFilter = filter;
		recordListReader.readRecordList(SOME_USER_TOKEN, SOME_RECORD_TYPE, nonEmptyFilter);

		DataGroup dataGroup = ((DataValidatorAlwaysValidSpy) dataValidator).dataGroup;
		assertDataGroupEquality(dataGroup, nonEmptyFilter);
	}

	@Test
	public void testFilterValidationIsCalledCorrectlyForStart() {
		recordStorage = new OldRecordStorageSpy();
		setUpDependencyProvider();

		exampleFilter.addChild(new DataAtomicSpy("start", "1"));

		recordListReader.readRecordList(SOME_USER_TOKEN, SOME_RECORD_TYPE, exampleFilter);

		DataGroup dataGroup = ((DataValidatorAlwaysValidSpy) dataValidator).dataGroup;
		assertDataGroupEquality(dataGroup, exampleFilter);
	}

	@Test
	public void testFilterValidationIsCalledCorrectlyForRows() {
		recordStorage = new OldRecordStorageSpy();
		setUpDependencyProvider();

		exampleFilter.addChild(new DataAtomicSpy("rows", "1"));

		recordListReader.readRecordList(SOME_USER_TOKEN, SOME_RECORD_TYPE, exampleFilter);

		DataGroup dataGroup = ((DataValidatorAlwaysValidSpy) dataValidator).dataGroup;
		assertDataGroupEquality(dataGroup, exampleFilter);
	}

	@Test(expectedExceptions = AuthenticationException.class)
	public void testAuthenticationNotAuthenticated() {
		recordStorage = new OldRecordStorageSpy();
		setUpDependencyProvider();
		recordListReader.readRecordList("dummyNonAuthenticatedToken", "spyType", emptyFilter);
	}

	@Test
	public void testReadListAuthorized() {
		DataList readRecordList = recordListReader.readRecordList(SOME_USER_TOKEN, SOME_RECORD_TYPE,
				emptyFilter);
		assertEquals(readRecordList.getContainDataOfType(), SOME_RECORD_TYPE);
		assertEquals(readRecordList.getTotalNumberOfTypeInStorage(), "177");
		assertEquals(readRecordList.getFromNo(), "1");
		assertEquals(readRecordList.getToNo(), "5");
		List<Data> records = readRecordList.getDataList();
		DataRecord dataRecord = (DataRecord) records.iterator().next();
		assertNotNull(dataRecord);
	}

	@Test
	public void testReadListReturnedNumbersAreFromStorage() {
		recordStorage = new RecordStorageResultListCreatorSpy();
		setUpDependencyProvider();
		RecordStorageResultListCreatorSpy recordStorageSpy = (RecordStorageResultListCreatorSpy) recordStorage;
		recordStorageSpy.start = 3;
		recordStorageSpy.totalNumberOfMatches = 1500;
		List<DataGroup> list = new ArrayList<>();
		list.add(new DataGroupSpy("someName"));
		recordStorageSpy.listOfDataGroups = list;

		DataList readRecordList = recordListReader.readRecordList(SOME_USER_TOKEN, SOME_RECORD_TYPE,
				emptyFilter);

		assertEquals(readRecordList.getFromNo(), "3");
		assertEquals(readRecordList.getToNo(), "4");
		assertEquals(readRecordList.getTotalNumberOfTypeInStorage(), "1500");
	}

	@Test
	public void testReadListReturnedOtherNumbersAreFromStorage() {
		recordStorage = new RecordStorageResultListCreatorSpy();
		setUpDependencyProvider();
		RecordStorageResultListCreatorSpy recordStorageSpy = (RecordStorageResultListCreatorSpy) recordStorage;
		recordStorageSpy.start = 50;
		recordStorageSpy.totalNumberOfMatches = 1300;
		recordStorageSpy.listOfDataGroups = createListOfDummyDataGroups(50);

		DataList readRecordList = recordListReader.readRecordList(SOME_USER_TOKEN, SOME_RECORD_TYPE,
				emptyFilter);

		assertEquals(readRecordList.getFromNo(), "50");
		assertEquals(readRecordList.getToNo(), "100");
		assertEquals(readRecordList.getTotalNumberOfTypeInStorage(), "1300");
	}

	@Test
	public void testReadListReturnedNoMatches() {
		recordStorage = new RecordStorageResultListCreatorSpy();
		setUpDependencyProvider();
		RecordStorageResultListCreatorSpy recordStorageSpy = (RecordStorageResultListCreatorSpy) recordStorage;
		recordStorageSpy.start = 0;
		recordStorageSpy.totalNumberOfMatches = 0;
		recordStorageSpy.listOfDataGroups = createListOfDummyDataGroups(0);

		DataList readRecordList = recordListReader.readRecordList(SOME_USER_TOKEN, SOME_RECORD_TYPE,
				emptyFilter);

		assertEquals(readRecordList.getFromNo(), "0");
		assertEquals(readRecordList.getToNo(), "0");
		assertEquals(readRecordList.getTotalNumberOfTypeInStorage(), "0");
	}

	@Test
	public void testReadListReturnedOneMatches() {
		recordStorage = new RecordStorageResultListCreatorSpy();
		setUpDependencyProvider();
		RecordStorageResultListCreatorSpy recordStorageSpy = (RecordStorageResultListCreatorSpy) recordStorage;
		recordStorageSpy.start = 0;
		recordStorageSpy.totalNumberOfMatches = 1;
		recordStorageSpy.listOfDataGroups = createListOfDummyDataGroups(1);

		DataList readRecordList = recordListReader.readRecordList(SOME_USER_TOKEN, SOME_RECORD_TYPE,
				emptyFilter);

		assertEquals(readRecordList.getFromNo(), "0");
		assertEquals(readRecordList.getToNo(), "1");
		assertEquals(readRecordList.getTotalNumberOfTypeInStorage(), "1");
	}

	@Test
	public void testReadListReturnedNoMatchesButHasMatches() {
		recordStorage = new RecordStorageResultListCreatorSpy();
		setUpDependencyProvider();
		RecordStorageResultListCreatorSpy recordStorageSpy = (RecordStorageResultListCreatorSpy) recordStorage;
		recordStorageSpy.start = 0;
		recordStorageSpy.totalNumberOfMatches = 15;
		recordStorageSpy.listOfDataGroups = createListOfDummyDataGroups(0);

		DataList readRecordList = recordListReader.readRecordList(SOME_USER_TOKEN, SOME_RECORD_TYPE,
				emptyFilter);

		assertEquals(readRecordList.getFromNo(), "0");
		assertEquals(readRecordList.getToNo(), "0");
		assertEquals(readRecordList.getTotalNumberOfTypeInStorage(), "15");
	}

	@Test
	public void testReadAbstractListReturnedStartIsFromStorage() {
		recordStorage = new RecordStorageResultListCreatorSpy();

		setUpDependencyProvider();
		RecordStorageResultListCreatorSpy recordStorageSpy = (RecordStorageResultListCreatorSpy) recordStorage;
		recordStorageSpy.abstractString = "true";
		recordStorageSpy.start = 3;
		recordStorageSpy.totalNumberOfMatches = 765;
		recordStorageSpy.listOfDataGroups = createListOfDummyDataGroups(3);

		DataList readRecordList = recordListReader.readRecordList(SOME_USER_TOKEN, SOME_RECORD_TYPE,
				emptyFilter);

		assertEquals(readRecordList.getFromNo(), "3");
		assertEquals(readRecordList.getToNo(), "6");
		assertEquals(readRecordList.getTotalNumberOfTypeInStorage(), "765");
	}

	private List<DataGroup> createListOfDummyDataGroups(int numberOfGroups) {
		List<DataGroup> list = new ArrayList<>();
		for (int i = 0; i < numberOfGroups; i++) {
			list.add(createDataGroupWithRecordInfo());
		}
		return list;
	}

	private DataGroup createDataGroupWithRecordInfo() {
		DataGroup dataGroup = new DataGroupSpy("someName");
		DataGroup recordInfo = new DataGroupSpy("recordInfo");
		dataGroup.addChild(recordInfo);
		DataGroup typeGroup = new DataGroupSpy("type");
		recordInfo.addChild(typeGroup);
		typeGroup.addChild(new DataAtomicSpy("linkedRecordId", "someType"));
		return dataGroup;
	}

	@Test
	public void testReadListFilterIsPassedOnToStorage() {
		recordStorage = new OldRecordStorageSpy();
		setUpDependencyProvider();

		DataGroup filter = new DataGroupSpy("filter");
		DataGroup part = new DataGroupSpy("part");
		filter.addChild(part);
		part.addChild(new DataAtomicSpy("key", "someKey"));
		part.addChild(new DataAtomicSpy("value", "someValue"));

		recordListReader.readRecordList(SOME_USER_TOKEN, SOME_RECORD_TYPE, filter);

		DataGroup filterFromStorage = ((OldRecordStorageSpy) recordStorage).filters.get(0);
		assertEquals(filterFromStorage.getNameInData(), "filter");
		DataGroup extractedPart = filterFromStorage.getFirstGroupWithNameInData("part");
		assertEquals(extractedPart.getFirstAtomicValueWithNameInData("key"), "someKey");
		assertEquals(extractedPart.getFirstAtomicValueWithNameInData("value"), "someValue");
	}

	@Test
	public void testReadListAuthorizedButNoReadLinks() {
		dataGroupToRecordEnhancer.addReadAction = false;
		DataList readRecordList = recordListReader.readRecordList(SOME_USER_TOKEN, SOME_RECORD_TYPE,
				emptyFilter);
		assertEquals(readRecordList.getTotalNumberOfTypeInStorage(), "177");
		List<Data> records = readRecordList.getDataList();
		assertEquals(records.size(), 0);
	}

	@Test
	public void testRecordEnhancerCalled() {
		recordListReader.readRecordList(SOME_USER_TOKEN, SOME_RECORD_TYPE, emptyFilter);
		assertEquals(dataGroupToRecordEnhancer.user.id, "12345");
		assertEquals(dataGroupToRecordEnhancer.recordType, SOME_RECORD_TYPE);
		assertEquals(dataGroupToRecordEnhancer.dataGroup.getFirstGroupWithNameInData("recordInfo")
				.getFirstAtomicValueWithNameInData("id"), "place:0004");
	}

	@Test
	public void testReadListAbstractRecordType() {
		recordStorage = new OldRecordStorageSpy();
		setUpDependencyProvider();
		recordTypeHandlerSpy.isAbstract = true;

		DataList dataList = recordListReader.readRecordList(SOME_USER_TOKEN, "abstract",
				emptyFilter);
		assertEquals(dataList.getTotalNumberOfTypeInStorage(), "199");

		String type1 = extractTypeFromChildInListUsingIndex(dataList, 0);
		assertEquals(type1, "implementing1");
		String type2 = extractTypeFromChildInListUsingIndex(dataList, 1);
		assertEquals(type2, "implementing2");
	}

	@Test
	public void testReadListAbstractFilterPassedOnToStorage() {
		recordStorage = new OldRecordStorageSpy();
		setUpDependencyProvider();
		recordTypeHandlerSpy.isAbstract = true;

		recordListReader.readRecordList(SOME_USER_TOKEN, "abstract", exampleFilter);
		DataGroup sentListRecordFilter = ((OldRecordStorageSpy) recordStorage).filter;

		assertSame(sentListRecordFilter, exampleFilter);
	}

	@Test
	public void testRecordEnhancerCalledForType() {
		recordStorage = new OldRecordStorageSpy();
		setUpDependencyProvider();
		recordTypeHandlerSpy.isAbstract = true;

		recordListReader.readRecordList(SOME_USER_TOKEN, "abstract", emptyFilter);
		assertEquals(dataGroupToRecordEnhancer.user.id, "12345");
		assertEquals(dataGroupToRecordEnhancer.recordType, "implementing2");
		assertEquals(dataGroupToRecordEnhancer.dataGroup.getFirstGroupWithNameInData("recordInfo")
				.getFirstAtomicValueWithNameInData("id"), "child2_2");
	}

	private String extractTypeFromChildInListUsingIndex(DataList dataList, int index) {
		DataRecord data1 = (DataRecord) dataList.getDataList().get(index);
		DataGroup dataGroup1 = data1.getDataGroup();
		DataGroup recordInfo = dataGroup1.getFirstGroupWithNameInData("recordInfo");
		DataGroup typeGroup = recordInfo.getFirstGroupWithNameInData("type");
		return typeGroup.getFirstAtomicValueWithNameInData("linkedRecordId");
	}

	@Test
	public void testReadListAbstractRecordTypeNoDataForOneRecordType() {
		recordStorage = new OldRecordStorageSpy();
		setUpDependencyProvider();
		recordTypeHandlerSpy.isAbstract = true;

		DataList dataList = recordListReader.readRecordList(SOME_USER_TOKEN, "abstract2",
				emptyFilter);
		assertEquals(dataList.getTotalNumberOfTypeInStorage(), "199");

		String type1 = extractTypeFromChildInListUsingIndex(dataList, 0);
		assertEquals(type1, "implementing2");

	}

	@Test(expectedExceptions = AuthorizationException.class)
	public void testReadListUnauthorized() {
		authorizator = new NeverAuthorisedStub();
		setUpDependencyProvider();
		recordListReader.readRecordList(SOME_USER_TOKEN, SOME_RECORD_TYPE, emptyFilter);
	}

	@Test(expectedExceptions = DataException.class)
	public void testReadListAuthenticatedAndAuthorizedInvalidData() {
		dataValidator = new DataValidatorAlwaysInvalidSpy();
		setUpDependencyProvider();
		recordListReader.readRecordList(SOME_USER_TOKEN, SOME_RECORD_TYPE, createNonEmptyFilter());
	}

	@Test
	public void testReadListCorrectFilterMetadataIsRead() {
		recordListReader.readRecordList(SOME_USER_TOKEN, SOME_RECORD_TYPE, createNonEmptyFilter());

		DataValidatorAlwaysValidSpy dataValidatorSpy = (DataValidatorAlwaysValidSpy) dataValidator;
		assertEquals(dataValidatorSpy.metadataId, "placeFilterGroup");
	}

	@Test(expectedExceptions = DataException.class, expectedExceptionsMessageRegExp = ""
			+ "No filter exists for recordType: image")
	public void testReadListAuthenticatedAndAuthorizedNoFilterMetadataNonEmptyFilter() {
		setUpDependencyProvider();
		DataGroup filter = createNonEmptyFilter();
		recordListReader.readRecordList(SOME_USER_TOKEN, "image", filter);
	}

	private DataGroup createNonEmptyFilter() {
		DataGroup filter = new DataGroupSpy("filter");
		DataGroup part = new DataGroupSpy("part");
		filter.addChild(part);
		return filter;
	}

	@Test
	public void testReadListAuthenticatedAndAuthorizedNoFilterMetadataEmptyFilter() {
		setUpDependencyProvider();
		DataList readRecordList = recordListReader.readRecordList(SOME_USER_TOKEN, "image",
				emptyFilter);
		assertEquals(readRecordList.getFromNo(), "1");
		assertEquals(readRecordList.getToNo(), "3");
		assertEquals(readRecordList.getTotalNumberOfTypeInStorage(), "177");
	}

	@Test
	public void testReadListNotAuthorizedButPublicRecordType() {
		recordStorage = new OldRecordStorageSpy();
		authorizator = new AlwaysAuthorisedExceptStub();
		AlwaysAuthorisedExceptStub authorisedExceptStub = (AlwaysAuthorisedExceptStub) authorizator;
		HashSet<String> hashSet = new HashSet<String>();
		hashSet.add("list");

		authorisedExceptStub.notAuthorizedForRecordTypeAndActions.put("publicReadType", hashSet);
		setUpDependencyProvider();

		recordListReader.readRecordList("unauthorizedUserId", "publicReadType", emptyFilter);
		assertTrue(((OldRecordStorageSpy) recordStorage).readListWasCalled);
	}

	private void assertDataGroupEquality(DataGroup actual, DataGroup expected) {
		assertEquals(actual.getNameInData(), expected.getNameInData());
		var actualAtomicChildren = actual.getChildren().stream()
				.filter(elem -> elem instanceof DataAtomic).map(elem -> (DataAtomic) elem)
				.collect(Collectors.toList());
		var expectedAtomicChildren = expected.getChildren().stream()
				.filter(elem -> elem instanceof DataAtomic).map(elem -> (DataAtomic) elem)
				.collect(Collectors.toList());
		if (actualAtomicChildren.size() == expectedAtomicChildren.size()) {
			if (!actualAtomicChildren.isEmpty()) {
				for (int idx = 0; idx < actualAtomicChildren.size(); idx++) {
					assertDataAtomicEquality(actualAtomicChildren.get(idx),
							expectedAtomicChildren.get(idx));
				}
			}
		} else {
			fail();
		}

		var actualGroupChildren = actual.getChildren().stream()
				.filter(elem -> elem instanceof DataGroup).map(elem -> (DataGroup) elem)
				.collect(Collectors.toList());
		var expectedGroupChildren = expected.getChildren().stream()
				.filter(elem -> elem instanceof DataGroup).map(elem -> (DataGroup) elem)
				.collect(Collectors.toList());

		if (actualGroupChildren.size() == expectedGroupChildren.size()) {
			if (!actualGroupChildren.isEmpty()) {
				for (int idx = 0; idx < actualAtomicChildren.size(); idx++) {
					assertDataGroupEquality(actualGroupChildren.get(idx),
							expectedGroupChildren.get(idx));
				}
			}
		} else {
			fail();
		}

		assertEquals(actual.getRepeatId(), expected.getRepeatId());
		assertEquals(actual.getAttributes(), expected.getAttributes());
	}

	private void assertDataAtomicEquality(DataAtomic actual, DataAtomic expected) {
		assertEquals(actual.getNameInData(), expected.getNameInData());
		assertEquals(actual.getValue(), expected.getValue());
		assertEquals(actual.getRepeatId(), expected.getRepeatId());
		assertEquals(actual.getAttributes(), expected.getAttributes());
	}

	// @Test
	// public void testReadListIsAuthorizedPerRecord() {
	//
	// ((OldRecordStorageSpy) recordStorage).numberOfRecordsToReturnForReadRecordList = 2;
	//
	// recordListReader.readRecordList(SOME_USER_TOKEN, SOME_RECORD_TYPE, exampleFilter);
	//
	// AuthorizatorAlwaysAuthorizedSpy authorizatorSpy = ((AuthorizatorAlwaysAuthorizedSpy)
	// authorizator);
	// assertFalse(authorizatorSpy.calledMethods.isEmpty());
	// assertEquals(authorizatorSpy.calledMethods.get(0),
	// "checkAndGetUserAuthorizationsForActionOnRecordTypeAndCollectedData");
	// assertEquals(authorizatorSpy.calledMethods.size(), "2");
	//
	// }
}
