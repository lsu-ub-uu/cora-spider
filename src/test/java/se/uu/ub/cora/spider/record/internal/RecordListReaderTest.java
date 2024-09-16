/*
 * Copyright 2015, 2016, 2018, 2019, 2020, 2022 Uppsala University Library
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
import static org.testng.Assert.assertSame;
import static org.testng.Assert.fail;
import static se.uu.ub.cora.spider.extendedfunctionality.ExtendedFunctionalityPosition.READLIST_AFTER_AUTHORIZATION;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import se.uu.ub.cora.beefeater.authentication.User;
import se.uu.ub.cora.bookkeeper.validator.DataValidationException;
import se.uu.ub.cora.bookkeeper.validator.ValidationAnswer;
import se.uu.ub.cora.data.Data;
import se.uu.ub.cora.data.DataGroup;
import se.uu.ub.cora.data.DataList;
import se.uu.ub.cora.data.DataProvider;
import se.uu.ub.cora.data.DataRecord;
import se.uu.ub.cora.data.DataRecordGroup;
import se.uu.ub.cora.data.spies.DataFactorySpy;
import se.uu.ub.cora.data.spies.DataGroupSpy;
import se.uu.ub.cora.data.spies.DataListSpy;
import se.uu.ub.cora.data.spies.DataRecordGroupSpy;
import se.uu.ub.cora.logger.LoggerProvider;
import se.uu.ub.cora.spider.authentication.AuthenticationException;
import se.uu.ub.cora.spider.authentication.OldAuthenticatorSpy;
import se.uu.ub.cora.spider.authorization.AuthorizationException;
import se.uu.ub.cora.spider.authorization.PermissionRuleCalculator;
import se.uu.ub.cora.spider.dependency.spy.DataGroupToFilterSpy;
import se.uu.ub.cora.spider.dependency.spy.RecordTypeHandlerOldSpy;
import se.uu.ub.cora.spider.extendedfunctionality.ExtendedFunctionalityData;
import se.uu.ub.cora.spider.extendedfunctionality.internal.ExtendedFunctionalityProviderSpy;
import se.uu.ub.cora.spider.log.LoggerFactorySpy;
import se.uu.ub.cora.spider.record.DataException;
import se.uu.ub.cora.spider.record.DataGroupToRecordEnhancerSpy;
import se.uu.ub.cora.spider.record.DataRedactorOldSpy;
import se.uu.ub.cora.spider.record.RecordListReader;
import se.uu.ub.cora.spider.spy.DataValidatorSpy;
import se.uu.ub.cora.spider.spy.OldSpiderAuthorizatorSpy;
import se.uu.ub.cora.spider.spy.RuleCalculatorSpy;
import se.uu.ub.cora.spider.spy.SpiderDependencyProviderSpy;
import se.uu.ub.cora.storage.StorageReadResult;
import se.uu.ub.cora.storage.spies.RecordStorageSpy;

public class RecordListReaderTest {

	private static final String SOME_USER_TOKEN = "someToken78678567";
	private static final String SOME_RECORD_TYPE = "place";

	private RecordStorageSpy recordStorage;
	private OldAuthenticatorSpy authenticator;
	private OldSpiderAuthorizatorSpy authorizator;
	private PermissionRuleCalculator ruleCalculator;
	private RecordListReader recordListReader;
	private DataGroupToRecordEnhancerSpy recordEnhancer;
	private DataValidatorSpy dataValidator;
	private DataGroupSpy emptyFilter;
	private DataGroupSpy nonEmptyFilter;
	private LoggerFactorySpy loggerFactorySpy;
	private DataFactorySpy dataFactorySpy;

	private RecordTypeHandlerOldSpy recordTypeHandlerSpy;
	private DataRedactorOldSpy dataRedactor;
	private DataListSpy dataList;
	private SpiderDependencyProviderSpy dependencyProviderSpy;
	private ExtendedFunctionalityProviderSpy extendedFunctionalityProvider;

	private int start = 0;
	private int totalNumberOfMatches = 2;
	private int numberToReturnForReadList = 2;
	private List<DataRecordGroup> listOfDataRecordGroups = new ArrayList<>();
	private StorageReadResult storageReadResult;

	@BeforeMethod
	public void beforeMethod() {
		setUpFactoriesAndProviders();
		emptyFilter = createEmptyFilter();
		nonEmptyFilter = new DataGroupSpy();
		authenticator = new OldAuthenticatorSpy();
		authorizator = new OldSpiderAuthorizatorSpy();
		recordStorage = new RecordStorageSpy();
		ruleCalculator = new RuleCalculatorSpy();
		dataValidator = new DataValidatorSpy();
		dataRedactor = new DataRedactorOldSpy();
		recordEnhancer = new DataGroupToRecordEnhancerSpy();
		extendedFunctionalityProvider = new ExtendedFunctionalityProviderSpy();
		recordTypeHandlerSpy = new RecordTypeHandlerOldSpy();
		setUpDependencyProvider();

		dataList = new DataListSpy();
		dataList.MRV.setDefaultReturnValuesSupplier("getTotalNumberOfTypeInStorage",
				(Supplier<String>) () -> "2");

		dataFactorySpy.MRV.setDefaultReturnValuesSupplier("factorListUsingNameOfDataType",
				(Supplier<DataList>) () -> dataList);

		storageReadResult = createSpiderReadResult(0, 2, 2);
		recordStorage.MRV.setDefaultReturnValuesSupplier("readList", () -> storageReadResult);
	}

	private StorageReadResult createSpiderReadResult(int start, int totalNumberOfMatches,
			int numberToReturnForReadList) {
		StorageReadResult readResult = new StorageReadResult();
		readResult.start = start;
		readResult.totalNumberOfMatches = totalNumberOfMatches;
		if (numberToReturnForReadList > 0) {
			listOfDataRecordGroups = new ArrayList<>();
			addRecordsToList();
		}
		readResult.listOfDataRecordGroups = listOfDataRecordGroups;
		return readResult;
	}

	private void addRecordsToList() {
		int i = start;
		while (i < numberToReturnForReadList) {
			DataRecordGroupSpy dataRecordGroup = new DataRecordGroupSpy();
			String id = "someId" + i;
			dataRecordGroup.MRV.setDefaultReturnValuesSupplier("getType", () -> "dummyRecordType");
			dataRecordGroup.MRV.setDefaultReturnValuesSupplier("getId", () -> id);
			listOfDataRecordGroups.add(dataRecordGroup);
			i++;
		}
	}

	private DataGroupSpy createEmptyFilter() {
		DataGroupSpy filter = new DataGroupSpy();
		filter.MRV.setDefaultReturnValuesSupplier("hasChildren", () -> false);
		return filter;
	}

	private void setUpFactoriesAndProviders() {
		loggerFactorySpy = new LoggerFactorySpy();
		LoggerProvider.setLoggerFactory(loggerFactorySpy);
		dataFactorySpy = new DataFactorySpy();
		DataProvider.onlyForTestSetDataFactory(dataFactorySpy);
	}

	private void setUpDependencyProvider() {
		dependencyProviderSpy = new SpiderDependencyProviderSpy();
		dependencyProviderSpy.MRV.setDefaultReturnValuesSupplier("getAuthenticator",
				() -> authenticator);
		dependencyProviderSpy.MRV.setDefaultReturnValuesSupplier("getSpiderAuthorizator",
				() -> authorizator);
		dependencyProviderSpy.MRV.setDefaultReturnValuesSupplier("getRecordStorage",
				() -> recordStorage);
		dependencyProviderSpy.MRV.setDefaultReturnValuesSupplier("getRuleCalculator",
				() -> ruleCalculator);
		dependencyProviderSpy.MRV.setDefaultReturnValuesSupplier("getDataValidator",
				() -> dataValidator);
		dependencyProviderSpy.MRV.setDefaultReturnValuesSupplier("getExtendedFunctionalityProvider",
				() -> extendedFunctionalityProvider);
		dependencyProviderSpy.MRV.setDefaultReturnValuesSupplier("getDataRedactor",
				() -> dataRedactor);
		dependencyProviderSpy.MRV.setDefaultReturnValuesSupplier("getRecordTypeHandler",
				() -> recordTypeHandlerSpy);

		recordEnhancer = new DataGroupToRecordEnhancerSpy();
		recordListReader = RecordListReaderImp.usingDependencyProviderAndDataGroupToRecordEnhancer(
				dependencyProviderSpy, recordEnhancer);
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

		emptyFilter.MCR.assertMethodWasCalled("hasChildren");
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
		recordListReader.readRecordList(SOME_USER_TOKEN, SOME_RECORD_TYPE, nonEmptyFilter);

		dataValidator.MCR.assertParameters("validateListFilter", 0, SOME_RECORD_TYPE,
				nonEmptyFilter);
	}

	@Test(expectedExceptions = DataException.class, expectedExceptionsMessageRegExp = ""
			+ "Data is not valid: \\[Data for list filter not valid, DataValidatorSpy\\]")
	public void testFilterNotValid() throws Exception {
		setUpDataValidatorToReturnValidationAnswerWithErrorForValidateListFilter();

		recordListReader.readRecordList(SOME_USER_TOKEN, SOME_RECORD_TYPE, nonEmptyFilter);
	}

	private void setUpDataValidatorToReturnValidationAnswerWithErrorForValidateListFilter() {
		ValidationAnswer validationAnswer = new ValidationAnswer();
		validationAnswer.addErrorMessage("Data for list filter not valid, DataValidatorSpy");
		dataValidator.MRV.setDefaultReturnValuesSupplier("validateListFilter",
				() -> validationAnswer);
	}

	@Test(expectedExceptions = DataException.class, expectedExceptionsMessageRegExp = ""
			+ "No filter exists for recordType: " + SOME_RECORD_TYPE)
	public void testFilterNotPresentInRecordType() throws Exception {
		setUpDataValidatorToThrowErrorForValidateListFilter();

		recordListReader.readRecordList(SOME_USER_TOKEN, SOME_RECORD_TYPE, nonEmptyFilter);
	}

	private void setUpDataValidatorToThrowErrorForValidateListFilter() {
		DataValidationException e = DataValidationException
				.withMessage("No filter exists for recordType, " + SOME_RECORD_TYPE);
		dataValidator.MRV.setAlwaysThrowException("validateListFilter", e);
	}

	@Test
	public void testReadingFromStorageCalled() {
		recordListReader.readRecordList(SOME_USER_TOKEN, SOME_RECORD_TYPE, emptyFilter);

		DataGroupToFilterSpy converterToFilter = (DataGroupToFilterSpy) dependencyProviderSpy.MCR
				.getReturnValue("getDataGroupToFilterConverter", 0);

		converterToFilter.MCR.assertParameters("convert", 0, emptyFilter);
		var filter = converterToFilter.MCR.getReturnValue("convert", 0);
		recordStorage.MCR.assertParameters("readList", 0, SOME_RECORD_TYPE, filter);
	}

	@Test
	public void testEnhanceIsCalledForRecordsReturnedFromStorage() {
		recordListReader.readRecordList(SOME_USER_TOKEN, SOME_RECORD_TYPE, emptyFilter);

		recordStorage.MCR.assertNumberOfCallsToMethod("readList", 1);
		recordEnhancer.MCR.assertNumberOfCallsToMethod("enhance", 2);

		StorageReadResult storageReadResult = (StorageReadResult) recordStorage.MCR
				.getReturnValue("readList", 0);
		User returnedUser = (User) authenticator.MCR.getReturnValue("getUserForToken", 0);
		List<DataRecordGroup> listOfReturnedDataGroupsFromStorage = storageReadResult.listOfDataRecordGroups;
		DataRecordGroup returnedDataGroup1 = listOfReturnedDataGroupsFromStorage.get(0);
		String returnedRecordType1 = returnedDataGroup1.getType();

		recordEnhancer.MCR.assertParameters("enhance", 0, returnedUser, returnedRecordType1,
				returnedDataGroup1, dataRedactor);

		DataRecordGroup returnedDataGroup2 = listOfReturnedDataGroupsFromStorage.get(1);
		String returnedRecordType2 = returnedDataGroup1.getType();
		recordEnhancer.MCR.assertParameters("enhance", 1, returnedUser, returnedRecordType2,
				returnedDataGroup2, dataRedactor);
	}

	@Test
	public void testEnhancedDataIsReturnedInDataList() throws Exception {
		DataList readRecordList = recordListReader.readRecordList(SOME_USER_TOKEN, SOME_RECORD_TYPE,
				emptyFilter);

		DataListSpy dataListSpy = (DataListSpy) dataFactorySpy.MCR
				.getReturnValue("factorListUsingNameOfDataType", 0);
		assertSame(dataListSpy, readRecordList);
		DataRecord returnValue0 = (DataRecord) recordEnhancer.MCR.getReturnValue("enhance", 0);
		dataListSpy.MCR.assertParameters("addData", 0, returnValue0);

		DataRecord returnValue1 = (DataRecord) recordEnhancer.MCR.getReturnValue("enhance", 1);
		dataListSpy.MCR.assertParameters("addData", 1, returnValue1);
	}

	@Test
	public void testOnlyRecordsWithReadActionFromEnhancerIsReturnedNoRecordHasReadAction()
			throws Exception {
		recordEnhancer.addReadAction = false;

		DataList readRecordList = recordListReader.readRecordList(SOME_USER_TOKEN, SOME_RECORD_TYPE,
				emptyFilter);

		List<Data> dataList = readRecordList.getDataList();
		assertEquals(dataList.size(), 0);
	}

	@Test
	public void testOnlyRecordsWithReadActionFromEnhancerIsReturned() throws Exception {
		storageReadResult = createSpiderReadResult(0, 3, 3);
		recordEnhancer.addReadActionOnlyFirst = true;

		DataList readRecordList = recordListReader.readRecordList(SOME_USER_TOKEN, SOME_RECORD_TYPE,
				emptyFilter);

		DataListSpy dataListSpy = (DataListSpy) dataFactorySpy.MCR
				.getReturnValue("factorListUsingNameOfDataType", 0);
		assertSame(dataListSpy, readRecordList);
		DataRecord returnValue0 = (DataRecord) recordEnhancer.MCR.getReturnValue("enhance", 0);
		dataListSpy.MCR.assertParameters("addData", 0, returnValue0);

		dataListSpy.MCR.assertNumberOfCallsToMethod("addData", 1);
	}

	@Test
	public void testTotalNumberInDataListIsFromStorage() throws Exception {
		recordEnhancer.addReadActionOnlyFirst = false;
		recordEnhancer.addReadAction = true;

		storageReadResult = createSpiderReadResult(4, 25, 7);

		dataList.MRV.setDefaultReturnValuesSupplier("getDataList",
				(Supplier<List<DataGroup>>) () -> List.of(new DataGroupSpy(), new DataGroupSpy(),
						new DataGroupSpy()));

		DataList readRecordList = recordListReader.readRecordList(SOME_USER_TOKEN, SOME_RECORD_TYPE,
				emptyFilter);

		assertSame(dataList, readRecordList);
		dataList.MCR.assertParameters("setTotalNo", 0, "25");
		dataList.MCR.assertParameters("setFromNo", 0, "4");
		dataList.MCR.assertParameters("setToNo", 0, "7");
	}

	@Test(expectedExceptions = RuntimeException.class)
	public void testCallsToEnhancerThatThrowsOtherExceptionsPassedOn() throws Exception {
		recordEnhancer.throwOtherException = true;

		recordListReader.readRecordList(SOME_USER_TOKEN, SOME_RECORD_TYPE, emptyFilter);
	}

	@Test
	public void testTotalNumberInDataListIsFromStorageOtherNumbers() throws Exception {
		storageReadResult = createSpiderReadResult(1, 20, 10);

		dataList.MRV.setDefaultReturnValuesSupplier("getDataList",
				(Supplier<List<DataGroup>>) () -> List.of(new DataGroupSpy(), new DataGroupSpy(),
						new DataGroupSpy(), new DataGroupSpy(), new DataGroupSpy(),
						new DataGroupSpy(), new DataGroupSpy(), new DataGroupSpy(),
						new DataGroupSpy()));

		DataList readRecordList = recordListReader.readRecordList(SOME_USER_TOKEN, SOME_RECORD_TYPE,
				emptyFilter);

		assertSame(dataList, readRecordList);
		dataList.MCR.assertParameters("setTotalNo", 0, "20");
		dataList.MCR.assertParameters("setFromNo", 0, "1");
		dataList.MCR.assertParameters("setToNo", 0, "10");
	}

	@Test
	public void testTotalNumberInDataListIsFromStorageNonHasReadAction() throws Exception {
		storageReadResult = createSpiderReadResult(1, 20, 10);
		recordEnhancer.addReadAction = false;

		DataList readRecordList = recordListReader.readRecordList(SOME_USER_TOKEN, SOME_RECORD_TYPE,
				emptyFilter);

		assertSame(dataList, readRecordList);
		dataList.MCR.assertParameters("setTotalNo", 0, "20");
		dataList.MCR.assertParameters("setFromNo", 0, "0");
		dataList.MCR.assertParameters("setToNo", 0, "0");
	}

	@Test
	public void testUseExtendedFunctionalityExtendedFunctionalitiesExists() throws Exception {
		recordListReader.readRecordList(SOME_USER_TOKEN, SOME_RECORD_TYPE, nonEmptyFilter);

		ExtendedFunctionalityData expectedData = new ExtendedFunctionalityData();
		expectedData.recordType = SOME_RECORD_TYPE;
		expectedData.authToken = SOME_USER_TOKEN;
		expectedData.user = (User) authenticator.MCR.getReturnValue("getUserForToken", 0);
		expectedData.previouslyStoredTopDataGroup = null;
		extendedFunctionalityProvider.assertCallToPositionAndFunctionalityCalledWithData(
				READLIST_AFTER_AUTHORIZATION, expectedData, 0);
	}

	@Test
	public void testUseExtendedFunctionalityExtendedFunctionalitiesExists2() throws Exception {
		extendedFunctionalityProvider.setUpExtendedFunctionalityToThrowExceptionOnPosition(
				dependencyProviderSpy, READLIST_AFTER_AUTHORIZATION, SOME_RECORD_TYPE);
		try {
			recordListReader.readRecordList(SOME_USER_TOKEN, SOME_RECORD_TYPE, nonEmptyFilter);
			fail("Should fail as we want to stop execution when the extended functionality is used,"
					+ " to determin that the extended functionality is called in the correct place"
					+ " in the code");
		} catch (Exception e) {

		}

		authorizator.MCR.assertMethodWasCalled("checkUserIsAuthorizedForActionOnRecordType");
		dataFactorySpy.MCR.assertMethodNotCalled("factorListUsingNameOfDataType");
	}
}
