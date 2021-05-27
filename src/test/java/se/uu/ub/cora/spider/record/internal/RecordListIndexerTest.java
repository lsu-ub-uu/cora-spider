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
import se.uu.ub.cora.spider.data.DataGroupFactorySpy;
import se.uu.ub.cora.spider.data.DataGroupSpy;
import se.uu.ub.cora.spider.dependency.SpiderDependencyProviderSpy;
import se.uu.ub.cora.spider.index.internal.IndexBatchJob;
import se.uu.ub.cora.spider.index.internal.IndexBatchJobStorerSpy;
import se.uu.ub.cora.spider.log.LoggerFactorySpy;
import se.uu.ub.cora.spider.record.RecordListIndexer;
import se.uu.ub.cora.spider.record.RecordStorageMCRSpy;
import se.uu.ub.cora.spider.spy.DataValidatorSpy;
import se.uu.ub.cora.spider.spy.MethodCallRecorder;
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
	private DataValidatorSpy dataValidatorSpy;
	private DataGroupFactorySpy dataGroupFactory;
	private DataAtomicFactorySpy dataAtomicFactory;
	private IndexBatchHandlerSpy indexBatchHandler;
	private IndexBatchJobStorerSpy batchJobStorer;
	private DataGroupSpy indexSettingsWithFilter;

	@BeforeMethod
	public void beforeMethod() {
		setUpDependencyProvider();
		setUpDataProviders();
		indexSettingsWithoutFilter = new DataGroupSpy("indexSettings");
		indexSettingsWithFilter = createIndexSettingsWithFilter();
		indexBatchHandler = new IndexBatchHandlerSpy();
		batchJobStorer = new IndexBatchJobStorerSpy();
		recordListIndexer = RecordListIndexerImp.usingDependencyProvider(dependencyProviderSpy,
				batchJobStorer, indexBatchHandler);

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
				indexSettingsWithoutFilter);

		dataValidatorSpy.MCR.assertParameters("validateIndexSettings", 0, SOME_RECORD_TYPE,
				indexSettingsWithoutFilter);
	}

	@Test(expectedExceptions = DataValidationException.class, expectedExceptionsMessageRegExp = ""
			+ "DataValidatorSpy, No indexSettings exists for recordType, " + SOME_RECORD_TYPE)
	public void testErrorInIndexSettingOnValidation() throws Exception {
		dataValidatorSpy.throwExcpetionIndexSettingsNotFound = true;

		recordListIndexer.indexRecordList(SOME_USER_TOKEN, SOME_RECORD_TYPE,
				indexSettingsWithoutFilter);
	}

	// @Test
	// public void testVerifyReadExtraInformationForRecord() throws Exception {
	// recordListIndexer.indexRecordList(SOME_USER_TOKEN, SOME_RECORD_TYPE,
	// indexSettingsWithoutFilter);
	// }

	// @Test
	// public void testExtractFilterFromIndesSettingsWhenIndexSettingsWithFilter() throws Exception
	// {
	// RecordStorageMCRSpy recordStorage = (RecordStorageMCRSpy)
	// dependencyProviderSpy.recordStorage;
	//
	// recordListIndexer.indexRecordList(SOME_USER_TOKEN, SOME_RECORD_TYPE,
	// indexSettingsWithFilter);
	//
	// indexSettingsWithFilter.MCR.assertParameters("getFirstGroupWithNameInData", 0, "filter");
	// DataGroup extractedFilterFromIndexSettings = (DataGroup) indexSettingsWithFilter.MCR
	// .getReturnValue("getFirstGroupWithNameInData", 0);
	//
	// recordStorage.MCR.assertParameter("getTotalNumberOfRecords", 0, "filter",
	// extractedFilterFromIndexSettings);
	//
	// }
	//
	// @Test
	// public void testCreateNewFilterWhenIndexSettingsWithoutFilter() throws Exception {
	// RecordStorageMCRSpy recordStorage = (RecordStorageMCRSpy)
	// dependencyProviderSpy.recordStorage;
	//
	// recordListIndexer.indexRecordList(SOME_USER_TOKEN, SOME_RECORD_TYPE,
	// indexSettingsWithoutFilter);
	//
	// indexSettingsWithoutFilter.MCR.assertParameters("getFirstGroupWithNameInData", 0, "filter");
	// Object noFilterReturned = indexSettingsWithoutFilter.MCR
	// .getReturnValue("getFirstGroupWithNameInData", 0);
	//
	// assertNull(noFilterReturned);
	//
	// Map<String, Object> parameters = recordStorage.MCR
	// .getParametersForMethodAndCallNumber("getTotalNumberOfRecords", 0);
	// DataGroup filter = (DataGroup) parameters.get("filter");
	//
	// assertTrue(filter instanceof DataGroup);
	// }

	@Test
	public void testGetTotalNumberOfMatchesFromStorageWithoutFilter() throws Exception {
		RecordStorageMCRSpy recordStorage = (RecordStorageMCRSpy) dependencyProviderSpy.recordStorage;

		recordListIndexer.indexRecordList(SOME_USER_TOKEN, SOME_RECORD_TYPE,
				indexSettingsWithoutFilter);

		DataGroup createdFilter = (DataGroup) dataGroupFactory.MCR
				.getReturnValue("factorUsingNameInData", 0);

		recordStorage.MCR.assertParameter("getTotalNumberOfRecords", 0, "filter", createdFilter);

	}

	@Test
	public void testGetTotalNumberOfMatchesFromStorageWithFilter() throws Exception {
		RecordStorageMCRSpy recordStorage = (RecordStorageMCRSpy) dependencyProviderSpy.recordStorage;

		recordListIndexer.indexRecordList(SOME_USER_TOKEN, SOME_RECORD_TYPE,
				indexSettingsWithFilter);

		indexSettingsWithFilter.MCR.assertParameters("getFirstGroupWithNameInData", 0, "filter");
		DataGroup extractedFilterFromIndexSettings = (DataGroup) indexSettingsWithFilter.MCR
				.getReturnValue("getFirstGroupWithNameInData", 0);

		recordStorage.MCR.assertParameter("getTotalNumberOfRecords", 0, "filter",
				extractedFilterFromIndexSettings);

	}

	@Test
	public void testIndexBatchJobIsCreatedAndStoredWithoutFilter() throws Exception {
		RecordStorageMCRSpy recordStorage = (RecordStorageMCRSpy) dependencyProviderSpy.recordStorage;
		recordStorage.totalNumberOfRecords = 45;

		recordListIndexer.indexRecordList(SOME_USER_TOKEN, SOME_RECORD_TYPE,
				indexSettingsWithoutFilter);

		MethodCallRecorder MCR = batchJobStorer.MCR;
		MCR.assertMethodWasCalled("create");
		IndexBatchJob indexBatchJob = (IndexBatchJob) MCR
				.getValueForMethodNameAndCallNumberAndParameterName("create", 0, "indexBatchJob");

		assertEquals(indexBatchJob.recordTypeToIndex, SOME_RECORD_TYPE);
		assertEquals(indexBatchJob.totalNumberToIndex, 45);
		assertEquals(indexBatchJob.filter.getNameInData(), "filter");
	}

	@Test
	public void testIndexBatchJobIsCreatedAndStoredWithFilter() throws Exception {

		recordListIndexer.indexRecordList(SOME_USER_TOKEN, SOME_RECORD_TYPE,
				indexSettingsWithFilter);

		MethodCallRecorder MCR = batchJobStorer.MCR;
		MCR.assertMethodWasCalled("create");
		IndexBatchJob indexBatchJob = (IndexBatchJob) MCR
				.getValueForMethodNameAndCallNumberAndParameterName("create", 0, "indexBatchJob");

		assertEquals(indexBatchJob.recordTypeToIndex, SOME_RECORD_TYPE);
		assertEquals(indexBatchJob.totalNumberToIndex, 0);
		assertEquals(indexBatchJob.filter.getNameInData(), "filter");

		indexSettingsWithFilter.MCR.assertParameters("getFirstGroupWithNameInData", 0, "filter");
	}

	@Test
	public void testIndexBatchJobIsStarted() throws Exception {

		recordListIndexer.indexRecordList(SOME_USER_TOKEN, SOME_RECORD_TYPE,
				indexSettingsWithoutFilter);

		IndexBatchJob indexBatchJob = (IndexBatchJob) batchJobStorer.MCR
				.getValueForMethodNameAndCallNumberAndParameterName("create", 0, "indexBatchJob");

		indexBatchHandler.MCR.assertParameters("runIndexBatchJob", 0, indexBatchJob);
	}

	@Test
	public void testReturnRecordFromCreate() throws Exception {

		DataRecord indexBatchJobRecord = recordListIndexer.indexRecordList(SOME_USER_TOKEN,
				SOME_RECORD_TYPE, indexSettingsWithoutFilter);

		DataRecord recordFromStorer = (DataRecord) batchJobStorer.MCR.getReturnValue("create", 0);

		assertSame(indexBatchJobRecord, recordFromStorer);
	}

}
