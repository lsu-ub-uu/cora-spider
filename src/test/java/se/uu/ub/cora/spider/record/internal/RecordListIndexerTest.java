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

import java.util.HashMap;

import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import se.uu.ub.cora.beefeater.authentication.User;
import se.uu.ub.cora.bookkeeper.validator.DataValidationException;
import se.uu.ub.cora.data.DataAtomicProvider;
import se.uu.ub.cora.data.DataGroup;
import se.uu.ub.cora.data.DataGroupProvider;
import se.uu.ub.cora.data.DataRecord;
import se.uu.ub.cora.logger.LoggerProvider;
import se.uu.ub.cora.spider.authentication.AuthenticationException;
import se.uu.ub.cora.spider.authentication.AuthenticatorSpy;
import se.uu.ub.cora.spider.authorization.AuthorizationException;
import se.uu.ub.cora.spider.data.DataAtomicFactorySpy;
import se.uu.ub.cora.spider.data.DataAtomicSpy;
import se.uu.ub.cora.spider.data.DataGroupFactorySpy;
import se.uu.ub.cora.spider.data.DataGroupSpy;
import se.uu.ub.cora.spider.data.DataRecordSpy;
import se.uu.ub.cora.spider.dependency.RecordCreatorSpy;
import se.uu.ub.cora.spider.dependency.RecordTypeHandlerSpy;
import se.uu.ub.cora.spider.dependency.SpiderDependencyProviderSpy;
import se.uu.ub.cora.spider.dependency.SpiderInstanceFactorySpy;
import se.uu.ub.cora.spider.dependency.SpiderInstanceProvider;
import se.uu.ub.cora.spider.index.internal.DataGroupHandlerForIndexBatchJobSpy;
import se.uu.ub.cora.spider.index.internal.IndexBatchJob;
import se.uu.ub.cora.spider.log.LoggerFactorySpy;
import se.uu.ub.cora.spider.record.RecordListIndexer;
import se.uu.ub.cora.spider.record.RecordStorageMCRSpy;
import se.uu.ub.cora.spider.spy.DataValidatorSpy;
import se.uu.ub.cora.spider.spy.SpiderAuthorizatorSpy;

public class RecordListIndexerTest {

	private static final String SOME_USER_TOKEN = "someToken123456789";
	private static final String SOME_RECORD_TYPE = "someRecordType";

	private LoggerFactorySpy loggerFactorySpy;
	private SpiderDependencyProviderSpy dependencyProviderSpy;
	private RecordListIndexerImp recordListIndexer;
	private AuthenticatorSpy authenticatorSpy;

	private SpiderAuthorizatorSpy authorizatorSpy;
	private DataGroupSpy indexSettingsWithoutFilter;
	private DataGroupSpy indexSettingsWithFilter;
	private DataValidatorSpy dataValidatorSpy;
	private DataGroupFactorySpy dataGroupFactory;
	private DataAtomicFactorySpy dataAtomicFactory;
	private IndexBatchHandlerSpy indexBatchHandler;
	private DataGroupHandlerForIndexBatchJobSpy batchJobConverterSpy;
	private SpiderInstanceFactorySpy spiderInstanceFactorySpy;

	@BeforeMethod
	public void beforeMethod() {
		setUpDependencyProvider();
		setUpDataProviders();
		indexSettingsWithoutFilter = new DataGroupSpy("indexSettings");
		indexSettingsWithFilter = createIndexSettingsWithFilter();
		indexBatchHandler = new IndexBatchHandlerSpy();
		batchJobConverterSpy = new DataGroupHandlerForIndexBatchJobSpy();
		setUpRecordCreatorToReturnRecordWithId("someRecordId");
		recordListIndexer = RecordListIndexerImp.usingDependencyProvider(dependencyProviderSpy,
				indexBatchHandler, batchJobConverterSpy);

	}

	private void setUpDependencyProvider() {
		loggerFactorySpy = new LoggerFactorySpy();
		LoggerProvider.setLoggerFactory(loggerFactorySpy);
		dependencyProviderSpy = new SpiderDependencyProviderSpy(new HashMap<>());
		authenticatorSpy = new AuthenticatorSpy();
		authorizatorSpy = new SpiderAuthorizatorSpy();
		dataValidatorSpy = new DataValidatorSpy();

		dependencyProviderSpy.authenticator = authenticatorSpy;
		dependencyProviderSpy.spiderAuthorizator = authorizatorSpy;
		dependencyProviderSpy.dataValidator = dataValidatorSpy;
	}

	private void setUpDataProviders() {
		dataGroupFactory = new DataGroupFactorySpy();
		DataGroupProvider.setDataGroupFactory(dataGroupFactory);
		dataAtomicFactory = new DataAtomicFactorySpy();
		DataAtomicProvider.setDataAtomicFactory(dataAtomicFactory);
		spiderInstanceFactorySpy = new SpiderInstanceFactorySpy();
		SpiderInstanceProvider.setSpiderInstanceFactory(spiderInstanceFactorySpy);
	}

	private DataGroupSpy createIndexSettingsWithFilter() {
		DataGroupSpy indexSettingsWithFilter = new DataGroupSpy("indexSettings");
		DataGroup filter = new DataGroupSpy("filter");
		indexSettingsWithFilter.addChild(filter);
		return indexSettingsWithFilter;
	}

	@Test
	public void testImplementsInterface() throws Exception {
		assertTrue(recordListIndexer instanceof RecordListIndexer);
	}

	@Test(expectedExceptions = AuthenticationException.class, expectedExceptionsMessageRegExp = ""
			+ "Error from AuthenticatorSpy")
	public void testUserNotAuthenticated() {
		authenticatorSpy.throwAuthenticationException = true;

		recordListIndexer.indexRecordList(SOME_USER_TOKEN, null, indexSettingsWithoutFilter);
	}

	@Test
	public void testAuthTokenIsPassedOnToAuthenticator() throws Exception {
		recordListIndexer.indexRecordList(SOME_USER_TOKEN, null, indexSettingsWithoutFilter);

		authenticatorSpy.MCR.assertParameters("getUserForToken", 0, SOME_USER_TOKEN);
		authenticatorSpy.MCR.assertNumberOfCallsToMethod("getUserForToken", 1);
	}

	@Test(expectedExceptions = AuthorizationException.class, expectedExceptionsMessageRegExp = ""
			+ "Exception from SpiderAuthorizatorSpy")
	public void testUserIsNotAuthorizedForActionOnRecordType() {
		authorizatorSpy.authorizedForActionAndRecordType = false;

		recordListIndexer.indexRecordList(SOME_USER_TOKEN, null, indexSettingsWithoutFilter);
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

	@Test(expectedExceptions = DataValidationException.class, expectedExceptionsMessageRegExp = ""
			+ "DataValidatorSpy, No indexSettings exists for recordType, " + SOME_RECORD_TYPE)
	public void testErrorInIndexSettingOnValidation() throws Exception {
		dataValidatorSpy.throwExcpetionIndexSettingsNotFound = true;

		recordListIndexer.indexRecordList(SOME_USER_TOKEN, SOME_RECORD_TYPE,
				indexSettingsWithFilter);
	}

	@Test(expectedExceptions = DataValidationException.class, expectedExceptionsMessageRegExp = ""
			+ "Error while validating index settings against defined "
			+ "metadata: \\[DataValidatorSpy not valid 1, DataValidatorSpy not valid 2\\]")
	public void testErrorIsThrownOnFilterValidationFailure() throws Exception {
		dataValidatorSpy.invalidIndexSettingsValidation = true;

		recordListIndexer.indexRecordList(SOME_USER_TOKEN, SOME_RECORD_TYPE,
				indexSettingsWithFilter);
	}

	@Test
	public void testGetTotalNumberOfMatchesFromStorageWithoutFilter() throws Exception {
		RecordStorageMCRSpy recordStorage = (RecordStorageMCRSpy) dependencyProviderSpy.recordStorage;

		recordListIndexer.indexRecordList(SOME_USER_TOKEN, SOME_RECORD_TYPE,
				indexSettingsWithoutFilter);

		DataGroup createdFilter = (DataGroup) dataGroupFactory.MCR
				.getReturnValue("factorUsingNameInData", 0);

		recordStorage.MCR.assertParameter("getTotalNumberOfRecordsForType", 0, "filter",
				createdFilter);

	}

	@Test
	public void testGetTotalNumberOfMatchesFromStorageWithFilter() throws Exception {
		RecordStorageMCRSpy recordStorage = (RecordStorageMCRSpy) dependencyProviderSpy.recordStorage;

		recordListIndexer.indexRecordList(SOME_USER_TOKEN, SOME_RECORD_TYPE,
				indexSettingsWithFilter);

		indexSettingsWithFilter.MCR.assertParameters("getFirstGroupWithNameInData", 0, "filter");
		DataGroup extractedFilterFromIndexSettings = (DataGroup) indexSettingsWithFilter.MCR
				.getReturnValue("getFirstGroupWithNameInData", 0);

		recordStorage.MCR.assertParameter("getTotalNumberOfRecordsForType", 0, "filter",
				extractedFilterFromIndexSettings);

	}

	@Test
	public void testIndexBatchJobIsCreatedWithoutFilter() throws Exception {
		RecordStorageMCRSpy recordStorage = (RecordStorageMCRSpy) dependencyProviderSpy.recordStorage;
		recordStorage.totalNumberOfRecords = 45;

		recordListIndexer.indexRecordList(SOME_USER_TOKEN, SOME_RECORD_TYPE,
				indexSettingsWithoutFilter);

		batchJobConverterSpy.MCR.assertMethodWasCalled("createDataGroup");

		DataGroup createdFilter = (DataGroup) dataGroupFactory.MCR
				.getReturnValue("factorUsingNameInData", 0);

		IndexBatchJob indexBatchJob = getParameterIndexBatchJobFromConverterSpy();

		assertEquals(indexBatchJob.recordTypeToIndex, SOME_RECORD_TYPE);
		assertEquals(indexBatchJob.totalNumberToIndex, 45);
		assertSame(indexBatchJob.filter, createdFilter);

	}

	@Test
	public void testIndexBatchJobIsCreatedWithFilter() throws Exception {

		recordListIndexer.indexRecordList(SOME_USER_TOKEN, SOME_RECORD_TYPE,
				indexSettingsWithFilter);

		DataGroup extractedFilterFromIndexSettings = (DataGroup) indexSettingsWithFilter.MCR
				.getReturnValue("getFirstGroupWithNameInData", 0);

		IndexBatchJob indexBatchJob = getParameterIndexBatchJobFromConverterSpy();

		assertEquals(indexBatchJob.recordTypeToIndex, SOME_RECORD_TYPE);
		assertEquals(indexBatchJob.totalNumberToIndex, 0);
		assertEquals(indexBatchJob.filter.getNameInData(), "filter");

		assertSame(indexBatchJob.filter, extractedFilterFromIndexSettings);
	}

	@Test
	public void testConvertAndStoreIndexBatchJobRecord() throws Exception {

		DataRecord finalRecord = recordListIndexer.indexRecordList(SOME_USER_TOKEN,
				SOME_RECORD_TYPE, indexSettingsWithoutFilter);

		RecordCreatorSpy recordCreatorSpy = (RecordCreatorSpy) spiderInstanceFactorySpy.MCR
				.getReturnValue("factorRecordCreator", 0);
		recordCreatorSpy.MCR.assertMethodWasCalled("createAndStoreRecord");

		var indexBatchJobDataGroup = batchJobConverterSpy.MCR.getReturnValue("createDataGroup", 0);

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
		setUpRecordCreatorToReturnRecordWithId("someOtherRecordId");

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
				.getValueForMethodNameAndCallNumberAndParameterName("createDataGroup", 0,
						"indexBatchJob");
	}

	@Test
	public void testRecordTypeIsAbstract() throws Exception {
		dependencyProviderSpy.recordTypeHandlerSpy.isAbstract = true;

		recordListIndexer.indexRecordList(SOME_USER_TOKEN, SOME_RECORD_TYPE,
				indexSettingsWithoutFilter);

		RecordTypeHandlerSpy recordTypeHandler = (RecordTypeHandlerSpy) dependencyProviderSpy.MCR
				.getReturnValue("getRecordTypeHandler", 0);

		recordTypeHandler.MCR.assertMethodWasCalled("isAbstract");

		recordTypeHandler.MCR.assertMethodWasCalled("getListOfImplementingRecordTypeIds");

		RecordStorageMCRSpy recordStorage = (RecordStorageMCRSpy) dependencyProviderSpy.recordStorage;

		var listOfImplementingRecordTypeIds = recordTypeHandler.MCR
				.getReturnValue("getListOfImplementingRecordTypeIds", 0);

		DataGroup createdFilter = (DataGroup) dataGroupFactory.MCR
				.getReturnValue("factorUsingNameInData", 0);

		recordStorage.MCR.assertParameters("getTotalNumberOfRecordsForAbstractType", 0,
				SOME_RECORD_TYPE, listOfImplementingRecordTypeIds, createdFilter);

	}

	private void setUpRecordCreatorToReturnRecordWithId(String recordId) {
		DataGroupSpy dataGroupSpy = new DataGroupSpy("indexBatchJob");
		DataGroupSpy recordInfo = new DataGroupSpy("recordInfo");
		dataGroupSpy.addChild(recordInfo);
		recordInfo.addChild(new DataAtomicSpy("id", recordId));

		DataRecordSpy dataRecordSpy = new DataRecordSpy(dataGroupSpy);
		spiderInstanceFactorySpy.recordToReturnForRecordCreator = dataRecordSpy;
	}
}
