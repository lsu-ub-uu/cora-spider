/*
 * Copyright 2021 Uppsala University Library
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
import static org.testng.Assert.assertTrue;
import static org.testng.Assert.fail;
import static se.uu.ub.cora.spider.extendedfunctionality.ExtendedFunctionalityPosition.INDEX_BATCH_JOB_AFTER_AUTHORIZATION;

import java.util.List;
import java.util.function.Supplier;

import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import se.uu.ub.cora.beefeater.authentication.User;
import se.uu.ub.cora.bookkeeper.validator.DataValidationException;
import se.uu.ub.cora.bookkeeper.validator.ValidationAnswer;
import se.uu.ub.cora.data.DataGroup;
import se.uu.ub.cora.data.DataProvider;
import se.uu.ub.cora.data.DataRecord;
import se.uu.ub.cora.data.spies.DataFactorySpy;
import se.uu.ub.cora.data.spies.DataGroupSpy;
import se.uu.ub.cora.data.spies.DataRecordGroupSpy;
import se.uu.ub.cora.data.spies.DataRecordSpy;
import se.uu.ub.cora.spider.dependency.SpiderInstanceProvider;
import se.uu.ub.cora.spider.dependency.spy.DataGroupToFilterSpy;
import se.uu.ub.cora.spider.dependency.spy.RecordCreatorSpy;
import se.uu.ub.cora.spider.dependency.spy.RecordTypeHandlerSpy;
import se.uu.ub.cora.spider.extendedfunctionality.ExtendedFunctionalityData;
import se.uu.ub.cora.spider.extendedfunctionality.internal.ExtendedFunctionalityProviderSpy;
import se.uu.ub.cora.spider.index.internal.DataRecordGroupHandlerForIndexBatchJobSpy;
import se.uu.ub.cora.spider.index.internal.IndexBatchJob;
import se.uu.ub.cora.spider.record.RecordListIndexer;
import se.uu.ub.cora.spider.spy.DataValidatorSpy;
import se.uu.ub.cora.spider.spy.SpiderDependencyProviderSpy;
import se.uu.ub.cora.spider.testspies.SpiderInstanceFactorySpy;
import se.uu.ub.cora.storage.Filter;
import se.uu.ub.cora.storage.spies.RecordStorageSpy;

public class RecordListIndexerTest {

	private static final String SOME_USER_TOKEN = "someToken123456789";
	private static final String SOME_RECORD_TYPE = "someRecordType";

	private DataFactorySpy dataFactory;
	private SpiderDependencyProviderSpy dependencyProvider;
	private RecordListIndexerImp recordListIndexer;
	private AuthenticatorSpy authenticatorSpy;

	private SpiderAuthorizatorSpy authorizatorSpy;
	private DataGroupSpy indexSettingsWithoutFilter;
	private DataGroupSpy indexSettingsWithFilter;
	private DataValidatorSpy dataValidatorSpy;
	private IndexBatchHandlerSpy indexBatchHandler;
	private DataRecordGroupHandlerForIndexBatchJobSpy batchJobConverterSpy;
	private SpiderInstanceFactorySpy spiderInstanceFactory;
	private ExtendedFunctionalityProviderSpy extendedFunctionalityProvider;
	private RecordTypeHandlerSpy recordTypeHandler;
	private RecordStorageSpy recordStorage;

	@BeforeMethod
	public void beforeMethod() {
		dataFactory = new DataFactorySpy();
		DataProvider.onlyForTestSetDataFactory(dataFactory);

		authenticatorSpy = new AuthenticatorSpy();
		authorizatorSpy = new SpiderAuthorizatorSpy();
		dataValidatorSpy = new DataValidatorSpy();
		recordStorage = new RecordStorageSpy();
		recordStorage.MRV.setDefaultReturnValuesSupplier("read", DataRecordGroupSpy::new);
		extendedFunctionalityProvider = new ExtendedFunctionalityProviderSpy();
		recordTypeHandler = new RecordTypeHandlerSpy();

		setUpDependencyProvider();
		setUpDataProviders();
		indexSettingsWithoutFilter = new DataGroupSpy();
		indexSettingsWithoutFilter.MRV.setDefaultReturnValuesSupplier("hasChildren", () -> false);
		indexSettingsWithFilter = new DataGroupSpy();
		indexSettingsWithFilter.MRV.setDefaultReturnValuesSupplier("containsChildWithNameInData",
				() -> true);
		indexBatchHandler = new IndexBatchHandlerSpy();
		batchJobConverterSpy = new DataRecordGroupHandlerForIndexBatchJobSpy();
		setUpRecordCreatorToReturnRecordWithId("someRecordId");
		recordListIndexer = RecordListIndexerImp.usingDependencyProvider(dependencyProvider,
				indexBatchHandler, batchJobConverterSpy);
	}

	private void setUpDependencyProvider() {
		dependencyProvider = new SpiderDependencyProviderSpy();
		dependencyProvider.MRV.setDefaultReturnValuesSupplier("getAuthenticator",
				() -> authenticatorSpy);
		dependencyProvider.MRV.setDefaultReturnValuesSupplier("getSpiderAuthorizator",
				() -> authorizatorSpy);
		dependencyProvider.MRV.setDefaultReturnValuesSupplier("getDataValidator",
				() -> dataValidatorSpy);
		dependencyProvider.MRV.setDefaultReturnValuesSupplier("getExtendedFunctionalityProvider",
				() -> extendedFunctionalityProvider);
		dependencyProvider.MRV.setDefaultReturnValuesSupplier(
				"getRecordTypeHandlerUsingDataRecordGroup", () -> recordTypeHandler);
		dependencyProvider.MRV.setDefaultReturnValuesSupplier("getRecordTypeHandler",
				() -> recordTypeHandler);
		dependencyProvider.MRV.setDefaultReturnValuesSupplier("getRecordStorage",
				() -> recordStorage);
	}

	private void setUpDataProviders() {
		spiderInstanceFactory = new SpiderInstanceFactorySpy();
		SpiderInstanceProvider.setSpiderInstanceFactory(spiderInstanceFactory);
	}

	@Test
	public void testImplementsInterface() throws Exception {
		assertTrue(recordListIndexer instanceof RecordListIndexer);
	}

	@Test
	public void testAuthTokenIsPassedOnToAuthenticator() throws Exception {
		recordListIndexer.indexRecordList(SOME_USER_TOKEN, SOME_RECORD_TYPE,
				indexSettingsWithoutFilter);

		authenticatorSpy.MCR.assertParameters("getUserForToken", 0, SOME_USER_TOKEN);
		authenticatorSpy.MCR.assertNumberOfCallsToMethod("getUserForToken", 1);
	}

	@Test
	public void testUserIsAuthorizedForActionOnRecordTypeIncomingParameters() throws Exception {
		recordListIndexer.indexRecordList(SOME_USER_TOKEN, SOME_RECORD_TYPE,
				indexSettingsWithoutFilter);
		User user = (User) authenticatorSpy.MCR.getReturnValue("getUserForToken", 0);

		authorizatorSpy.MCR.assertParameters("checkUserIsAuthorizedForActionOnRecordType", 0, user,
				"index", SOME_RECORD_TYPE);
	}

	@Test
	public void testNonEmptyIndexSettingsContainsPartGroupValidateListFilterIsCalled() {
		recordListIndexer.indexRecordList(SOME_USER_TOKEN, SOME_RECORD_TYPE,
				indexSettingsWithFilter);

		dataValidatorSpy.MCR.assertParameters("validateIndexSettings", 0, SOME_RECORD_TYPE,
				indexSettingsWithFilter);
	}

	@Test
	public void testEmptyIndexSettingsValidateListFilterIsNOTCalled() {
		recordListIndexer.indexRecordList(SOME_USER_TOKEN, SOME_RECORD_TYPE,
				indexSettingsWithoutFilter);

		dataValidatorSpy.MCR.assertMethodNotCalled("validateIndexSettings");
	}

	// TODO: rewrite to a Spider exception
	@Test(expectedExceptions = DataValidationException.class, expectedExceptionsMessageRegExp = ""
			+ "someMessage")
	public void testErrorInIndexSettingOnValidation() throws Exception {
		dataValidatorSpy.MRV.setAlwaysThrowException("validateIndexSettings",
				DataValidationException.withMessage("someMessage"));

		recordListIndexer.indexRecordList(SOME_USER_TOKEN, SOME_RECORD_TYPE,
				indexSettingsWithFilter);
	}

	@Test(expectedExceptions = DataValidationException.class, expectedExceptionsMessageRegExp = ""
			+ "Error while validating index settings against defined "
			+ "metadata: \\[DataValidatorSpy not valid 1, DataValidatorSpy not valid 2\\]")
	public void testErrorIsThrownOnFilterValidationFailure() throws Exception {
		ValidationAnswer validationAnswer = new ValidationAnswer();
		validationAnswer.addErrorMessage("DataValidatorSpy not valid 1");
		validationAnswer.addErrorMessage("DataValidatorSpy not valid 2");
		dataValidatorSpy.MRV.setDefaultReturnValuesSupplier("validateIndexSettings",
				() -> validationAnswer);

		recordListIndexer.indexRecordList(SOME_USER_TOKEN, SOME_RECORD_TYPE,
				indexSettingsWithFilter);
	}

	@Test
	public void testGetTotalNumberOfMatchesFromStorageWithoutFilter() throws Exception {
		recordListIndexer.indexRecordList(SOME_USER_TOKEN, SOME_RECORD_TYPE,
				indexSettingsWithoutFilter);

		DataGroup createdFilter = (DataGroup) dataFactory.MCR
				.getReturnValue("factorGroupUsingNameInData", 0);

		assertGetTotalNumberOfRecords(SOME_RECORD_TYPE, createdFilter);
	}

	private void assertGetTotalNumberOfRecords(String recordType, DataGroup filterAsDataGroup) {
		recordStorage.MCR.assertParameterAsEqual("getTotalNumberOfRecordsForTypes", 0, "types",
				List.of(recordType));
		var filter = getFilter(filterAsDataGroup);
		recordStorage.MCR.assertParameter("getTotalNumberOfRecordsForTypes", 0, "filter", filter);
	}

	@Test
	public void testGetTotalNumberOfMatchesFromStorageWithFilter() throws Exception {
		recordListIndexer.indexRecordList(SOME_USER_TOKEN, SOME_RECORD_TYPE,
				indexSettingsWithFilter);

		indexSettingsWithFilter.MCR.assertParameters("getFirstGroupWithNameInData", 0, "filter");
		DataGroup extractedFilterFromIndexSettings = (DataGroup) indexSettingsWithFilter.MCR
				.getReturnValue("getFirstGroupWithNameInData", 0);

		assertGetTotalNumberOfRecords(SOME_RECORD_TYPE, extractedFilterFromIndexSettings);
	}

	@Test
	public void testIndexBatchJobIsCreatedWithoutFilter() throws Exception {
		recordStorage.MRV.setDefaultReturnValuesSupplier("getTotalNumberOfRecordsForTypes",
				(Supplier<Long>) () -> 45L);

		recordListIndexer.indexRecordList(SOME_USER_TOKEN, SOME_RECORD_TYPE,
				indexSettingsWithoutFilter);

		batchJobConverterSpy.MCR.assertMethodWasCalled("createDataRecordGroup");

		DataGroup filterAsDataGroup = (DataGroup) dataFactory.MCR
				.getReturnValue("factorGroupUsingNameInData", 0);
		Filter filter = getFilter(filterAsDataGroup);

		IndexBatchJob indexBatchJob = getParameterIndexBatchJobFromConverterSpy();
		batchJobConverterSpy.MCR.assertParameters("createDataRecordGroup", 0, indexBatchJob,
				filterAsDataGroup);

		assertEquals(indexBatchJob.recordTypeToIndex, SOME_RECORD_TYPE);
		assertEquals(indexBatchJob.totalNumberToIndex, 45);
		assertSame(indexBatchJob.filter, filter);
	}

	@Test
	public void testIndexBatchJobIsCreatedWithFilter() throws Exception {
		recordListIndexer.indexRecordList(SOME_USER_TOKEN, SOME_RECORD_TYPE,
				indexSettingsWithFilter);

		DataGroup extractedFilterFromIndexSettings = (DataGroup) indexSettingsWithFilter.MCR
				.getReturnValue("getFirstGroupWithNameInData", 0);

		Filter filter = getFilter(extractedFilterFromIndexSettings);

		IndexBatchJob indexBatchJob = getParameterIndexBatchJobFromConverterSpy();

		batchJobConverterSpy.MCR.assertParameters("createDataRecordGroup", 0, indexBatchJob,
				extractedFilterFromIndexSettings);

		assertEquals(indexBatchJob.recordTypeToIndex, SOME_RECORD_TYPE);
		assertEquals(indexBatchJob.totalNumberToIndex, 0);
		assertSame(indexBatchJob.filter, filter);
	}

	private Filter getFilter(DataGroup filterAsDataGroup) {
		DataGroupToFilterSpy converterToFilter = (DataGroupToFilterSpy) dependencyProvider.MCR
				.getReturnValue("getDataGroupToFilterConverter", 0);
		converterToFilter.MCR.assertParameters("convert", 0, filterAsDataGroup);
		return (Filter) converterToFilter.MCR.getReturnValue("convert", 0);
	}

	@Test
	public void testConvertAndStoreIndexBatchJobRecord() throws Exception {
		DataRecord finalRecord = recordListIndexer.indexRecordList(SOME_USER_TOKEN,
				SOME_RECORD_TYPE, indexSettingsWithoutFilter);

		RecordCreatorSpy recordCreatorSpy = (RecordCreatorSpy) spiderInstanceFactory.MCR
				.getReturnValue("factorRecordCreator", 0);
		recordCreatorSpy.MCR.assertMethodWasCalled("createAndStoreRecord");

		var indexBatchJobDataGroup = batchJobConverterSpy.MCR
				.getReturnValue("createDataRecordGroup", 0);

		recordCreatorSpy.MCR.assertParameters("createAndStoreRecord", 0, SOME_USER_TOKEN,
				"indexBatchJob", indexBatchJobDataGroup);

		DataRecord recordFromRecordCreator = (DataRecord) recordCreatorSpy.MCR
				.getReturnValue("createAndStoreRecord", 0);

		assertSame(finalRecord, recordFromRecordCreator);
	}

	@Test
	public void testRecordIdInIndexBatchJobIsSetFromIdCreatedInRecordWhenCreating()
			throws Exception {
		recordListIndexer.indexRecordList(SOME_USER_TOKEN, SOME_RECORD_TYPE,
				indexSettingsWithoutFilter);

		IndexBatchJob indexBatchJob = getParameterIndexBatchJobFromConverterSpy();

		assertEquals(indexBatchJob.recordId, "someRecordId");
	}

	@Test
	public void testRecordIdInIndexBatchJobIsSetFromIdCreatedInRecordWhenCreatingDifferentId()
			throws Exception {
		RecordCreatorSpy recordCreatorSpy = new RecordCreatorSpy();
		spiderInstanceFactory.MRV.setReturnValues("factorRecordCreator", List.of(recordCreatorSpy));
		DataRecordSpy dataRecord = new DataRecordSpy();
		recordCreatorSpy.MRV.setDefaultReturnValuesSupplier("createAndStoreRecord",
				(Supplier<DataRecordSpy>) () -> dataRecord);
		dataRecord.MRV.setReturnValues("getId", List.of("someOtherRecordId"));
		recordListIndexer.indexRecordList(SOME_USER_TOKEN, SOME_RECORD_TYPE,
				indexSettingsWithoutFilter);

		IndexBatchJob indexBatchJob = getParameterIndexBatchJobFromConverterSpy();
		assertEquals(indexBatchJob.recordId, "someOtherRecordId");
	}

	@Test
	public void testIndexBatchJobIsStarted() throws Exception {
		recordListIndexer.indexRecordList(SOME_USER_TOKEN, SOME_RECORD_TYPE,
				indexSettingsWithoutFilter);

		IndexBatchJob indexBatchJob = getParameterIndexBatchJobFromConverterSpy();

		indexBatchHandler.MCR.assertParameters("runIndexBatchJob", 0, indexBatchJob);
	}

	private IndexBatchJob getParameterIndexBatchJobFromConverterSpy() {
		return (IndexBatchJob) batchJobConverterSpy.MCR
				.getParameterForMethodAndCallNumberAndParameter("createDataRecordGroup", 0,
						"indexBatchJob");
	}

	private void setUpRecordCreatorToReturnRecordWithId(String recordId) {
		RecordCreatorSpy recordCreatorSpy = new RecordCreatorSpy();
		spiderInstanceFactory.MRV.setReturnValues("factorRecordCreator", List.of(recordCreatorSpy));
		DataRecordSpy dataRecord = new DataRecordSpy();
		recordCreatorSpy.MRV.setDefaultReturnValuesSupplier("createAndStoreRecord",
				(Supplier<DataRecordSpy>) () -> dataRecord);
		dataRecord.MRV.setReturnValues("getId", List.of("someRecordId"));
	}

	@Test
	public void testExtendedFunctionalitySetUp() {
		recordListIndexer.indexRecordList(SOME_USER_TOKEN, SOME_RECORD_TYPE,
				indexSettingsWithoutFilter);

		dependencyProvider.MCR.assertParameters("getExtendedFunctionalityProvider", 0);
		extendedFunctionalityProvider.MCR.assertParameters(
				"getFunctionalityForPositionAndRecordType", 0, INDEX_BATCH_JOB_AFTER_AUTHORIZATION,
				SOME_RECORD_TYPE);
	}

	@Test
	public void testExtendedFunctionalityIsCalled() {
		recordListIndexer.indexRecordList(SOME_USER_TOKEN, SOME_RECORD_TYPE,
				indexSettingsWithoutFilter);

		extendedFunctionalityProvider.assertCallToPositionAndFunctionalityCalledWithData(
				INDEX_BATCH_JOB_AFTER_AUTHORIZATION, getExpectedDataForAfterAuthorization(), 0);
	}

	private ExtendedFunctionalityData getExpectedDataForAfterAuthorization() {
		ExtendedFunctionalityData expectedData = new ExtendedFunctionalityData();
		expectedData.recordType = SOME_RECORD_TYPE;
		expectedData.authToken = SOME_USER_TOKEN;
		expectedData.user = (User) authenticatorSpy.MCR.getReturnValue("getUserForToken", 0);
		return expectedData;
	}

	@Test
	public void testEnsureExtendedFunctionalityPositionFor_AfterAuthorization() throws Exception {
		extendedFunctionalityProvider.setUpExtendedFunctionalityToThrowExceptionOnPosition(
				dependencyProvider, INDEX_BATCH_JOB_AFTER_AUTHORIZATION, SOME_RECORD_TYPE);

		callReadIncomingLinksAndCatchStopExecution();

		authorizatorSpy.MCR.assertMethodWasCalled("checkUserIsAuthorizedForActionOnRecordType");
		extendedFunctionalityProvider.MCR
				.assertNumberOfCallsToMethod("getFunctionalityForPositionAndRecordType", 1);
		dependencyProvider.MCR.assertMethodNotCalled("getDataValidator");
	}

	private void callReadIncomingLinksAndCatchStopExecution() {
		try {
			recordListIndexer.indexRecordList(SOME_USER_TOKEN, SOME_RECORD_TYPE,
					indexSettingsWithoutFilter);
			fail("Should fail as we want to stop execution when the extended functionality is used,"
					+ " to determin that the extended functionality is called in the correct place"
					+ " in the code");
		} catch (Exception e) {
		}
	}

	@Test
	public void testOnlyForTestMethods() throws Exception {
		assertEquals(recordListIndexer.onlyForTestGetDependencyProvider(), dependencyProvider);
		assertEquals(recordListIndexer.onlyForTestGetIndexBatchHandler(), indexBatchHandler);
		assertEquals(recordListIndexer.onlyForTestGetBatchJobConverter(), batchJobConverterSpy);
	}
}
