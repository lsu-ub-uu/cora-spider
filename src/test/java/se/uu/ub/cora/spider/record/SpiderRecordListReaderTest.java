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

import java.util.HashMap;
import java.util.List;

import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import se.uu.ub.cora.beefeater.authentication.User;
import se.uu.ub.cora.data.Data;
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
import se.uu.ub.cora.spider.authentication.AuthenticatorSpy;
import se.uu.ub.cora.spider.authorization.AuthorizationException;
import se.uu.ub.cora.spider.authorization.PermissionRuleCalculator;
import se.uu.ub.cora.spider.data.DataAtomicFactorySpy;
import se.uu.ub.cora.spider.data.DataGroupFactorySpy;
import se.uu.ub.cora.spider.data.DataGroupSpy;
import se.uu.ub.cora.spider.dependency.RecordStorageProviderSpy;
import se.uu.ub.cora.spider.dependency.RecordTypeHandlerSpy;
import se.uu.ub.cora.spider.dependency.SpiderDependencyProviderSpy;
import se.uu.ub.cora.spider.log.LoggerFactorySpy;
import se.uu.ub.cora.spider.spy.DataValidatorSpy;
import se.uu.ub.cora.spider.spy.RuleCalculatorSpy;
import se.uu.ub.cora.spider.spy.SpiderAuthorizatorSpy;
import se.uu.ub.cora.storage.StorageReadResult;

public class SpiderRecordListReaderTest {

	private RecordStorageSpy recordStorage;
	private AuthenticatorSpy authenticator;
	private SpiderAuthorizatorSpy authorizator;
	private PermissionRuleCalculator ruleCalculator;
	private SpiderRecordListReader recordListReader;
	private DataGroupToRecordEnhancerSpy recordEnhancer;
	private DataValidatorSpy dataValidator;
	private DataGroup emptyFilter;
	private DataGroup nonEmptyFilter;

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
		nonEmptyFilter = createNonEmptyFilter();
		exampleFilter = new DataGroupSpy("filter");
		authenticator = new AuthenticatorSpy();
		authorizator = new SpiderAuthorizatorSpy();
		recordStorage = new RecordStorageSpy();
		ruleCalculator = new RuleCalculatorSpy();
		dataValidator = new DataValidatorSpy();
		recordEnhancer = new DataGroupToRecordEnhancerSpy();
		setUpDependencyProvider();
	}

	private DataGroup createNonEmptyFilter() {
		DataGroup filter = new DataGroupSpy("filter");
		DataGroup part = new DataGroupSpy("part");
		filter.addChild(part);
		return filter;
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

		dependencyProvider.ruleCalculator = ruleCalculator;
		dependencyProvider.dataValidator = dataValidator;
		recordEnhancer = new DataGroupToRecordEnhancerSpy();
		recordListReader = SpiderRecordListReaderImp
				.usingDependencyProviderAndDataGroupToRecordEnhancer(dependencyProvider,
						recordEnhancer);
		recordTypeHandlerSpy = dependencyProvider.recordTypeHandlerSpy;
	}

	@Test(expectedExceptions = AuthenticationException.class, expectedExceptionsMessageRegExp = ""
			+ "Error from AuthenticatorSpy")
	public void testUserNotAuthenticated() {
		authenticator.throwAuthenticationException = true;

		recordListReader.readRecordList(SOME_USER_TOKEN, SOME_RECORD_TYPE, emptyFilter);
	}

	@Test
	public void testAuthTokenIsPassedOnToAuthenticator() throws Exception {
		recordListReader.readRecordList(SOME_USER_TOKEN, SOME_RECORD_TYPE, emptyFilter);

		authenticator.MCR.assertParameters("getUserForToken", 0, SOME_USER_TOKEN);
		authenticator.MCR.assertNumberOfCallsToMethod("getUserForToken", 1);
	}

	@Test(expectedExceptions = AuthorizationException.class, expectedExceptionsMessageRegExp = ""
			+ "Exception from SpiderAuthorizatorSpy")
	public void testUserIsNotAuthorizedForActionOnRecordType() {
		authorizator.authorizedForActionAndRecordType = false;

		recordListReader.readRecordList(SOME_USER_TOKEN, SOME_RECORD_TYPE, emptyFilter);
	}

	@Test
	public void testUserIsAuthorizedForActionOnRecordTypeIncomingParameters() {
		recordListReader.readRecordList(SOME_USER_TOKEN, SOME_RECORD_TYPE, emptyFilter);

		authorizator.MCR.assertParameters("checkUserIsAuthorizedForActionOnRecordType", 0,
				authenticator.returnedUser, "list", SOME_RECORD_TYPE);
	}

	@Test
	public void testUserAuthorizationForReadOnListNOTDoneForPublicRecordTypes() throws Exception {
		recordTypeHandlerSpy.isPublicForRead = true;

		recordListReader.readRecordList(SOME_USER_TOKEN, SOME_RECORD_TYPE, emptyFilter);

		authorizator.MCR.assertMethodNotCalled("checkUserIsAuthorizedForActionOnRecordType");
	}

	@Test
	public void testEmptyFilter() throws Exception {
		recordListReader.readRecordList(SOME_USER_TOKEN, SOME_RECORD_TYPE, emptyFilter);
		dataValidator.MCR.assertMethodNotCalled("validateListFilter");
	}

	@Test
	public void testNonEmptyFilterContainsPartGroupValidateListFilterIsCalled() {
		recordListReader.readRecordList(SOME_USER_TOKEN, SOME_RECORD_TYPE, nonEmptyFilter);

		dataValidator.MCR.assertParameters("validateListFilter", 0, SOME_RECORD_TYPE,
				nonEmptyFilter);
	}

	@Test
	public void testNonEmptyFilterContainsStartGroupValidateListFilterIsCalled() {
		emptyFilter.addChild(new DataGroupSpy("start"));
		recordListReader.readRecordList(SOME_USER_TOKEN, SOME_RECORD_TYPE, emptyFilter);

		dataValidator.MCR.assertParameters("validateListFilter", 0, SOME_RECORD_TYPE, emptyFilter);
	}

	@Test
	public void testNonEmptyFilterContainsRowsGroupValidateListFilterIsCalled() {
		emptyFilter.addChild(new DataGroupSpy("rows"));
		recordListReader.readRecordList(SOME_USER_TOKEN, SOME_RECORD_TYPE, emptyFilter);

		dataValidator.MCR.assertParameters("validateListFilter", 0, SOME_RECORD_TYPE, emptyFilter);
	}

	@Test(expectedExceptions = DataException.class, expectedExceptionsMessageRegExp = ""
			+ "Data is not valid: \\[Data for list filter not vaild, DataValidatorSpy\\]")
	public void testFilterNotValid() throws Exception {
		dataValidator.validListFilterValidation = false;

		recordListReader.readRecordList(SOME_USER_TOKEN, SOME_RECORD_TYPE, nonEmptyFilter);
	}

	@Test(expectedExceptions = DataException.class, expectedExceptionsMessageRegExp = ""
			+ "No filter exists for recordType: " + SOME_RECORD_TYPE)
	public void testFilterNotPresentInRecordType() throws Exception {
		dataValidator.throwFilterNotFoundException = true;

		recordListReader.readRecordList(SOME_USER_TOKEN, SOME_RECORD_TYPE, nonEmptyFilter);
	}

	@Test
	public void testReadingFromStorageCalled() {
		recordListReader.readRecordList(SOME_USER_TOKEN, SOME_RECORD_TYPE, emptyFilter);

		recordTypeHandlerSpy.MCR.assertMethodWasCalled("isAbstract");
		recordStorage.MCR.assertParameters("readList", 0, SOME_RECORD_TYPE, emptyFilter);
	}

	@Test
	public void testReadingFromStorageCalledForAbstract() {
		recordTypeHandlerSpy.isAbstract = true;

		recordListReader.readRecordList(SOME_USER_TOKEN, SOME_RECORD_TYPE, emptyFilter);

		recordTypeHandlerSpy.MCR.assertMethodWasCalled("isAbstract");
		recordStorage.MCR.assertParameters("readAbstractList", 0, SOME_RECORD_TYPE, emptyFilter);
	}

	@Test
	public void testEnhanceIsCalledForRecordsReturnedFromStorage() throws Exception {
		recordStorage.endNumberToReturn = 2;

		recordListReader.readRecordList(SOME_USER_TOKEN, SOME_RECORD_TYPE, emptyFilter);

		recordEnhancer.MCR.assertNumberOfCallsToMethod("enhance", 2);

		StorageReadResult storageReadResult = (StorageReadResult) recordStorage.MCR
				.getReturnValue("readList", 0);
		List<DataGroup> listOfReturnedDataGroupsFromStorage = storageReadResult.listOfDataGroups;
		User returnedUser = (User) authenticator.MCR.getReturnValue("getUserForToken", 0);

		recordEnhancer.MCR.assertParameters("enhance", 0, returnedUser, SOME_RECORD_TYPE,
				listOfReturnedDataGroupsFromStorage.get(0));
		recordEnhancer.MCR.assertParameters("enhance", 1, returnedUser, SOME_RECORD_TYPE,
				listOfReturnedDataGroupsFromStorage.get(1));
	}

	// TODO: we are HERE, from the top!
	@Test
	public void testEnhanceIsCalledForRecordsReturnedFromStorageForAbstract() {
		recordTypeHandlerSpy.isAbstract = true;
		recordStorage.endNumberToReturn = 2;

		recordListReader.readRecordList(SOME_USER_TOKEN, SOME_RECORD_TYPE, emptyFilter);

		recordEnhancer.MCR.assertNumberOfCallsToMethod("enhance", 2);

		StorageReadResult storageReadResult = (StorageReadResult) recordStorage.MCR
				.getReturnValue("readAbstractList", 0);
		User returnedUser = (User) authenticator.MCR.getReturnValue("getUserForToken", 0);
		List<DataGroup> listOfReturnedDataGroupsFromStorage = storageReadResult.listOfDataGroups;
		DataGroup returnedDataGroup1 = listOfReturnedDataGroupsFromStorage.get(0);
		String returnedRecordType1 = extractRecordTypeFromDataGroup(returnedDataGroup1);

		recordEnhancer.MCR.assertParameters("enhance", 0, returnedUser, returnedRecordType1,
				returnedDataGroup1);

		DataGroup returnedDataGroup2 = listOfReturnedDataGroupsFromStorage.get(1);
		String returnedRecordType2 = extractRecordTypeFromDataGroup(returnedDataGroup2);
		recordEnhancer.MCR.assertParameters("enhance", 1, returnedUser, returnedRecordType2,
				returnedDataGroup2);
	}

	private String extractRecordTypeFromDataGroup(DataGroup dataGroup) {
		DataGroup recordInfo = dataGroup.getFirstGroupWithNameInData("recordInfo");
		DataGroup typeGroup = recordInfo.getFirstGroupWithNameInData("type");

		return typeGroup.getFirstAtomicValueWithNameInData("linkedRecordId");
	}

	@Test
	public void testEnhancedDataIsReturnedInDataList() throws Exception {
		recordStorage.endNumberToReturn = 2;

		DataList readRecordList = recordListReader.readRecordList(SOME_USER_TOKEN, SOME_RECORD_TYPE,
				emptyFilter);

		List<Data> dataList = readRecordList.getDataList();

		DataRecord returnValue0 = (DataRecord) recordEnhancer.MCR.getReturnValue("enhance", 0);
		DataRecord dataRecord0 = (DataRecord) dataList.get(0);
		assertSame(dataRecord0, returnValue0);

		DataRecord returnValue1 = (DataRecord) recordEnhancer.MCR.getReturnValue("enhance", 1);
		DataRecord dataRecord1 = (DataRecord) dataList.get(1);
		assertSame(dataRecord1, returnValue1);
	}

	@Test
	public void testEnhancedDataIsReturnedInDataListForAbstract() throws Exception {
		recordStorage.endNumberToReturn = 2;
		recordTypeHandlerSpy.isAbstract = true;

		DataList readRecordList = recordListReader.readRecordList(SOME_USER_TOKEN, SOME_RECORD_TYPE,
				emptyFilter);

		List<Data> dataList = readRecordList.getDataList();

		DataRecord returnValue0 = (DataRecord) recordEnhancer.MCR.getReturnValue("enhance", 0);
		DataRecord dataRecord0 = (DataRecord) dataList.get(0);
		assertSame(dataRecord0, returnValue0);

		DataRecord returnValue1 = (DataRecord) recordEnhancer.MCR.getReturnValue("enhance", 1);
		DataRecord dataRecord1 = (DataRecord) dataList.get(1);
		assertSame(dataRecord1, returnValue1);
	}

	@Test
	public void testOnlyRecordsWithReadActionFromEnhancerIsReturnedNoRecordHasReadAction()
			throws Exception {
		recordStorage.endNumberToReturn = 2;
		recordEnhancer.addReadAction = false;

		DataList readRecordList = recordListReader.readRecordList(SOME_USER_TOKEN, SOME_RECORD_TYPE,
				emptyFilter);

		List<Data> dataList = readRecordList.getDataList();
		assertEquals(dataList.size(), 0);
	}

	@Test
	public void testOnlyRecordsWithReadActionFromEnhancerIsReturned() throws Exception {
		recordStorage.endNumberToReturn = 3;
		recordEnhancer.addReadActionOnlyFirst = true;

		DataList readRecordList = recordListReader.readRecordList(SOME_USER_TOKEN, SOME_RECORD_TYPE,
				emptyFilter);

		List<Data> dataList = readRecordList.getDataList();
		assertEquals(dataList.size(), 1);
	}

	@Test
	public void testTotalNumberInDataListIsFromStorage() throws Exception {
		recordEnhancer.addReadActionOnlyFirst = false;
		recordEnhancer.addReadAction = true;

		recordStorage.totalNumberOfMatches = 25;
		recordStorage.endNumberToReturn = 7;
		recordStorage.start = 4;

		DataList readRecordList = recordListReader.readRecordList(SOME_USER_TOKEN, SOME_RECORD_TYPE,
				emptyFilter);

		assertEquals(readRecordList.getTotalNumberOfTypeInStorage(), "25");
		assertEquals(readRecordList.getFromNo(), "4");
		assertEquals(readRecordList.getToNo(), "7");
	}

	@Test
	public void testTotalNumberInDataListIsFromStorageOtherNumbers() throws Exception {
		recordStorage.totalNumberOfMatches = 20;
		recordStorage.endNumberToReturn = 10;
		recordStorage.start = 1;

		DataList readRecordList = recordListReader.readRecordList(SOME_USER_TOKEN, SOME_RECORD_TYPE,
				emptyFilter);

		assertEquals(readRecordList.getTotalNumberOfTypeInStorage(), "20");
		assertEquals(readRecordList.getFromNo(), "1");
		assertEquals(readRecordList.getToNo(), "10");
	}

	@Test
	public void testTotalNumberInDataListIsFromStorageNonHasReadAction() throws Exception {
		recordStorage.totalNumberOfMatches = 20;
		recordStorage.endNumberToReturn = 10;
		recordStorage.start = 1;
		recordEnhancer.addReadAction = false;

		DataList readRecordList = recordListReader.readRecordList(SOME_USER_TOKEN, SOME_RECORD_TYPE,
				emptyFilter);

		assertEquals(readRecordList.getTotalNumberOfTypeInStorage(), "20");
		assertEquals(readRecordList.getFromNo(), "0");
		assertEquals(readRecordList.getToNo(), "0");
	}

	// @Test
	// public void testTotalNumberInDataListIsFromStorageOtherNumbers() throws Exception {
	// recordStorage.totalNumberOfMatches = 20;
	// recordStorage.numberOfRecordsToReturn = 3;
	// recordStorage.start = 1;
	//
	// DataList readRecordList = recordListReader.readRecordList(SOME_USER_TOKEN, SOME_RECORD_TYPE,
	// emptyFilter);
	//
	// assertEquals(readRecordList.getTotalNumberOfTypeInStorage(), "20");
	// assertEquals(readRecordList.getFromNo(), "1");
	// assertEquals(readRecordList.getToNo(), "3");
	// }
	// @Test
	// public void testPublicRecordIsAddedAfterEnhance() throws Exception {
	// recordTypeHandlerSpy.isPublicForRead = true;
	//
	// recordListReader.readRecordList(SOME_USER_TOKEN, SOME_RECORD_TYPE, emptyFilter);
	//
	// //Vi förvänta oss att dataRecord är adderad
	// }
	// @Test(expectedExceptions = DataException.class, expectedExceptionsMessageRegExp = ""
	// + "No filter exists for recordType: image")
	// public void testReadListAuthenticatedAndAuthorizedNoFilterMetadataNonEmptyFilter() {
	// setUpDependencyProvider();
	// DataGroup filter = createNonEmptyFilter();
	// recordListReader.readRecordList(SOME_USER_TOKEN, "image", filter);
	// }
	//

	// @Test
	// public void testFilterValidationIsCalledCorrectlyWithOtherFilter() {
	// recordStorage = new OldRecordStorageSpy();
	// setUpDependencyProvider();
	// DataGroup filter = new DataGroupSpy("filter2");
	// DataGroup part = new DataGroupSpy("part");
	// filter.addChild(part);
	//
	// DataGroup nonEmptyFilter = filter;
	// recordListReader.readRecordList(SOME_USER_TOKEN, SOME_RECORD_TYPE, nonEmptyFilter);
	//
	// dataValidator.MCR.assertParameters("validateData", 0, "placeFilterGroup", nonEmptyFilter);
	// }
	//
	// @Test
	// public void testFilterValidationIsCalledCorrectlyForStart() {
	// recordStorage = new OldRecordStorageSpy();
	// setUpDependencyProvider();
	//
	// exampleFilter.addChild(new DataAtomicSpy("start", "1"));
	//
	// recordListReader.readRecordList(SOME_USER_TOKEN, SOME_RECORD_TYPE, exampleFilter);
	//
	// dataValidator.MCR.assertParameters("validateData", 0, "placeFilterGroup", exampleFilter);
	// }
	//
	// @Test
	// public void testFilterValidationIsCalledCorrectlyForRows() {
	// recordStorage = new OldRecordStorageSpy();
	// setUpDependencyProvider();
	//
	// exampleFilter.addChild(new DataAtomicSpy("rows", "1"));
	//
	// recordListReader.readRecordList(SOME_USER_TOKEN, SOME_RECORD_TYPE, exampleFilter);
	//
	// dataValidator.MCR.assertParameters("validateData", 0, "placeFilterGroup", exampleFilter);
	// }
	//
	// @Test
	// public void testReadListReturnedNumbersAreFromStorage() {
	// RecordStorageSpy recordStorageSpy = (RecordStorageSpy) recordStorage;
	// recordStorageSpy.start = 3;
	// recordStorageSpy.totalNumberOfMatches = 1500;
	// List<DataGroup> list = new ArrayList<>();
	// list.add(new DataGroupSpy("someName"));
	// recordStorageSpy.listOfDataGroups = list;
	//
	// DataList readRecordList = recordListReader.readRecordList(SOME_USER_TOKEN, SOME_RECORD_TYPE,
	// emptyFilter);
	//
	// assertEquals(readRecordList.getFromNo(), "3");
	// assertEquals(readRecordList.getToNo(), "4");
	// assertEquals(readRecordList.getTotalNumberOfTypeInStorage(), "1500");
	// }
	//
	// @Test
	// public void testReadListReturnedOtherNumbersAreFromStorage() {
	// RecordStorageSpy recordStorageSpy = (RecordStorageSpy) recordStorage;
	// recordStorageSpy.start = 50;
	// recordStorageSpy.totalNumberOfMatches = 1300;
	// recordStorageSpy.listOfDataGroups = createListOfDummyDataGroups(50);
	//
	// DataList readRecordList = recordListReader.readRecordList(SOME_USER_TOKEN, SOME_RECORD_TYPE,
	// emptyFilter);
	//
	// assertEquals(readRecordList.getFromNo(), "50");
	// assertEquals(readRecordList.getToNo(), "100");
	// assertEquals(readRecordList.getTotalNumberOfTypeInStorage(), "1300");
	// }
	//
	// @Test
	// public void testReadListReturnedNoMatches() {
	// RecordStorageSpy recordStorageSpy = (RecordStorageSpy) recordStorage;
	// recordStorageSpy.start = 0;
	// recordStorageSpy.totalNumberOfMatches = 0;
	// recordStorageSpy.listOfDataGroups = createListOfDummyDataGroups(0);
	//
	// DataList readRecordList = recordListReader.readRecordList(SOME_USER_TOKEN, SOME_RECORD_TYPE,
	// emptyFilter);
	//
	// assertEquals(readRecordList.getFromNo(), "0");
	// assertEquals(readRecordList.getToNo(), "0");
	// assertEquals(readRecordList.getTotalNumberOfTypeInStorage(), "0");
	// }
	//
	// @Test
	// public void testReadListReturnedOneMatches() {
	// RecordStorageSpy recordStorageSpy = (RecordStorageSpy) recordStorage;
	// recordStorageSpy.start = 0;
	// recordStorageSpy.totalNumberOfMatches = 1;
	// recordStorageSpy.listOfDataGroups = createListOfDummyDataGroups(1);
	//
	// DataList readRecordList = recordListReader.readRecordList(SOME_USER_TOKEN, SOME_RECORD_TYPE,
	// emptyFilter);
	//
	// assertEquals(readRecordList.getFromNo(), "0");
	// assertEquals(readRecordList.getToNo(), "1");
	// assertEquals(readRecordList.getTotalNumberOfTypeInStorage(), "1");
	// }
	//
	// @Test
	// public void testReadListReturnedNoMatchesButHasMatches() {
	// RecordStorageSpy recordStorageSpy = (RecordStorageSpy) recordStorage;
	// recordStorageSpy.start = 0;
	// recordStorageSpy.totalNumberOfMatches = 15;
	// recordStorageSpy.listOfDataGroups = createListOfDummyDataGroups(0);
	//
	// DataList readRecordList = recordListReader.readRecordList(SOME_USER_TOKEN, SOME_RECORD_TYPE,
	// emptyFilter);
	//
	// assertEquals(readRecordList.getFromNo(), "0");
	// assertEquals(readRecordList.getToNo(), "0");
	// assertEquals(readRecordList.getTotalNumberOfTypeInStorage(), "15");
	// }
	//
	// @Test
	// public void testReadAbstractListReturnedStartIsFromStorage() {
	// RecordStorageSpy recordStorageSpy = (RecordStorageSpy) recordStorage;
	// recordStorageSpy.abstractString = "true";
	// recordStorageSpy.start = 3;
	// recordStorageSpy.totalNumberOfMatches = 765;
	// recordStorageSpy.listOfDataGroups = createListOfDummyDataGroups(3);
	//
	// DataList readRecordList = recordListReader.readRecordList(SOME_USER_TOKEN, SOME_RECORD_TYPE,
	// emptyFilter);
	//
	// assertEquals(readRecordList.getFromNo(), "3");
	// assertEquals(readRecordList.getToNo(), "6");
	// assertEquals(readRecordList.getTotalNumberOfTypeInStorage(), "765");
	// }
	//
	// private List<DataGroup> createListOfDummyDataGroups(int numberOfGroups) {
	// List<DataGroup> list = new ArrayList<>();
	// for (int i = 0; i < numberOfGroups; i++) {
	// list.add(createDataGroupWithRecordInfo());
	// }
	// return list;
	// }
	//
	// private DataGroup createDataGroupWithRecordInfo() {
	// DataGroup dataGroup = new DataGroupSpy("someName");
	// DataGroup recordInfo = new DataGroupSpy("recordInfo");
	// dataGroup.addChild(recordInfo);
	// DataGroup typeGroup = new DataGroupSpy("type");
	// recordInfo.addChild(typeGroup);
	// typeGroup.addChild(new DataAtomicSpy("linkedRecordId", "someType"));
	// return dataGroup;
	// }
	//
	// @Test
	// public void testReadListFilterIsPassedOnToStorage() {
	// recordStorage = new OldRecordStorageSpy();
	// setUpDependencyProvider();
	//
	// DataGroup filter = new DataGroupSpy("filter");
	// DataGroup part = new DataGroupSpy("part");
	// filter.addChild(part);
	// part.addChild(new DataAtomicSpy("key", "someKey"));
	// part.addChild(new DataAtomicSpy("value", "someValue"));
	//
	// recordListReader.readRecordList(SOME_USER_TOKEN, SOME_RECORD_TYPE, filter);
	//
	// DataGroup filterFromStorage = ((OldRecordStorageSpy) recordStorage).filters.get(0);
	// assertEquals(filterFromStorage.getNameInData(), "filter");
	// DataGroup extractedPart = filterFromStorage.getFirstGroupWithNameInData("part");
	// assertEquals(extractedPart.getFirstAtomicValueWithNameInData("key"), "someKey");
	// assertEquals(extractedPart.getFirstAtomicValueWithNameInData("value"), "someValue");
	// }
	//
	// @Test
	// public void testReadListAuthorizedButNoReadLinks() {
	// recordStorage = TestDataRecordInMemoryStorage.createRecordStorageInMemoryWithTestData();
	// setUpDependencyProvider();
	// recordEnhancer.addReadAction = false;
	// DataList readRecordList = recordListReader.readRecordList(SOME_USER_TOKEN, SOME_RECORD_TYPE,
	// emptyFilter);
	// assertEquals(readRecordList.getTotalNumberOfTypeInStorage(), "177");
	// List<Data> records = readRecordList.getDataList();
	// assertEquals(records.size(), 0);
	// }
	//
	// @Test
	// public void testRecordEnhancerCalled() {
	// recordStorage = TestDataRecordInMemoryStorage.createRecordStorageInMemoryWithTestData();
	// setUpDependencyProvider();
	// recordListReader.readRecordList(SOME_USER_TOKEN, SOME_RECORD_TYPE, emptyFilter);
	//
	// recordEnhancer.MCR.assertParameter("enhance", 0, "user", authenticator.returnedUser);
	// recordEnhancer.MCR.assertParameter("enhance", 0, "recordType", SOME_RECORD_TYPE);
	// assertEquals(recordEnhancer.dataGroup.getFirstGroupWithNameInData("recordInfo")
	// .getFirstAtomicValueWithNameInData("id"), "place:0004");
	// recordEnhancer.MCR.assertNumberOfCallsToMethod("enhance", 4);
	// // recordEnhancer.MCR.assertParameters("enhance", 0, authenticator.returnedUser,
	// // SOME_RECORD_TYPE, null);
	// }
	//
	// @Test
	// public void testReadListAbstractRecordType() {
	// recordStorage = new OldRecordStorageSpy();
	// setUpDependencyProvider();
	// recordTypeHandlerSpy.isAbstract = true;
	//
	// DataList dataList = recordListReader.readRecordList(SOME_USER_TOKEN, "abstract",
	// emptyFilter);
	// assertEquals(dataList.getTotalNumberOfTypeInStorage(), "199");
	//
	// String type1 = extractTypeFromChildInListUsingIndex(dataList, 0);
	// assertEquals(type1, "implementing1");
	// String type2 = extractTypeFromChildInListUsingIndex(dataList, 1);
	// assertEquals(type2, "implementing2");
	// }
	//
	// @Test
	// public void testReadListAbstractFilterPassedOnToStorage() {
	// recordStorage = new OldRecordStorageSpy();
	// setUpDependencyProvider();
	// recordTypeHandlerSpy.isAbstract = true;
	//
	// recordListReader.readRecordList(SOME_USER_TOKEN, "abstract", exampleFilter);
	// DataGroup sentListRecordFilter = ((OldRecordStorageSpy) recordStorage).filter;
	//
	// assertSame(sentListRecordFilter, exampleFilter);
	// }
	//
	// @Test
	// public void testRecordEnhancerCalledForType() {
	// recordStorage = new OldRecordStorageSpy();
	// setUpDependencyProvider();
	// recordTypeHandlerSpy.isAbstract = true;
	//
	// recordListReader.readRecordList(SOME_USER_TOKEN, "abstract", emptyFilter);
	// assertEquals(recordEnhancer.user.id, "12345");
	// assertEquals(recordEnhancer.recordType, "implementing2");
	// assertEquals(recordEnhancer.dataGroup.getFirstGroupWithNameInData("recordInfo")
	// .getFirstAtomicValueWithNameInData("id"), "child2_2");
	// }
	//
	// private String extractTypeFromChildInListUsingIndex(DataList dataList, int index) {
	// DataRecord data1 = (DataRecord) dataList.getDataList().get(index);
	// DataGroup dataGroup1 = data1.getDataGroup();
	// DataGroup recordInfo = dataGroup1.getFirstGroupWithNameInData("recordInfo");
	// DataGroup typeGroup = recordInfo.getFirstGroupWithNameInData("type");
	// return typeGroup.getFirstAtomicValueWithNameInData("linkedRecordId");
	// }
	//
	// @Test
	// public void testReadListAbstractRecordTypeNoDataForOneRecordType() {
	// recordStorage = new OldRecordStorageSpy();
	// setUpDependencyProvider();
	// recordTypeHandlerSpy.isAbstract = true;
	//
	// DataList dataList = recordListReader.readRecordList(SOME_USER_TOKEN, "abstract2",
	// emptyFilter);
	// assertEquals(dataList.getTotalNumberOfTypeInStorage(), "199");
	//
	// String type1 = extractTypeFromChildInListUsingIndex(dataList, 0);
	// assertEquals(type1, "implementing2");
	//
	// }
	//
	// @Test(expectedExceptions = DataException.class)
	// public void testReadListAuthenticatedAndAuthorizedInvalidData() {
	// recordStorage = TestDataRecordInMemoryStorage.createRecordStorageInMemoryWithTestData();
	// setUpDependencyProvider();
	// dataValidator.validValidation = false;
	//
	// recordListReader.readRecordList(SOME_USER_TOKEN, SOME_RECORD_TYPE, createNonEmptyFilter());
	// }
	//
	// @Test
	// public void testReadListCorrectFilterMetadataIsRead() {
	// recordStorage = TestDataRecordInMemoryStorage.createRecordStorageInMemoryWithTestData();
	// setUpDependencyProvider();
	// DataGroup nonEmptyFilter = createNonEmptyFilter();
	// recordListReader.readRecordList(SOME_USER_TOKEN, SOME_RECORD_TYPE, nonEmptyFilter);
	//
	// dataValidator.MCR.assertParameters("validateData", 0, "placeFilterGroup", nonEmptyFilter);
	// }
	//
	// @Test(expectedExceptions = DataException.class, expectedExceptionsMessageRegExp = ""
	// + "No filter exists for recordType: image")
	// public void testReadListAuthenticatedAndAuthorizedNoFilterMetadataNonEmptyFilter() {
	// setUpDependencyProvider();
	// DataGroup filter = createNonEmptyFilter();
	// recordListReader.readRecordList(SOME_USER_TOKEN, "image", filter);
	// }
	//
	//
	// @Test
	// public void testReadListAuthenticatedAndAuthorizedNoFilterMetadataEmptyFilter() {
	// recordStorage = TestDataRecordInMemoryStorage.createRecordStorageInMemoryWithTestData();
	// setUpDependencyProvider();
	// DataList readRecordList = recordListReader.readRecordList(SOME_USER_TOKEN, "image",
	// emptyFilter);
	// assertEquals(readRecordList.getFromNo(), "1");
	// assertEquals(readRecordList.getToNo(), "3");
	// assertEquals(readRecordList.getTotalNumberOfTypeInStorage(), "177");
	// }
	//
	// @Test
	// public void testReadListNotAuthorizedButPublicRecordType() {
	// recordStorage = new OldRecordStorageSpy();
	// authorizator.authorizedForActionAndRecordType = false;
	// setUpDependencyProvider();
	//
	// recordListReader.readRecordList("unauthorizedUserId", "publicReadType", emptyFilter);
	//
	// assertTrue(((OldRecordStorageSpy) recordStorage).readListWasCalled);
	// }

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
	@Test
	public void testReadListAuthorized() {
		RecordStorageSpy recordStorageSpy = recordStorage;
		recordStorageSpy.totalNumberOfMatches = 177;
		recordStorageSpy.start = 1;
		recordStorageSpy.endNumberToReturn = 5;

		DataList readRecordList = recordListReader.readRecordList(SOME_USER_TOKEN, SOME_RECORD_TYPE,
				emptyFilter);
		assertEquals(readRecordList.getContainDataOfType(), SOME_RECORD_TYPE);
		assertEquals(readRecordList.getTotalNumberOfTypeInStorage(), "177");
		assertEquals(readRecordList.getFromNo(), "1");
		assertEquals(readRecordList.getToNo(), "5");

		// TODO: possibly remove if checked elsewere in this test
		List<Data> records = readRecordList.getDataList();
		DataRecord dataRecord = (DataRecord) records.get(0);
		assertNotNull(dataRecord);
	}
}
