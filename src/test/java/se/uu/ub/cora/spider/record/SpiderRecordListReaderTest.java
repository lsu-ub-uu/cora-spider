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

	// TODO: Flyttas till enhance.
	@Test(enabled = false)
	public void testPublicRecordTypeIsAddedToList() throws Exception {
		recordTypeHandlerSpy.isPublicForRead = true;
		recordStorage.totalNumberOfMatches = 2;
		DataList readRecordList = recordListReader.readRecordList(SOME_USER_TOKEN, SOME_RECORD_TYPE,
				emptyFilter);
		assertEquals(readRecordList.getDataList().size(), "2");
	}
}
