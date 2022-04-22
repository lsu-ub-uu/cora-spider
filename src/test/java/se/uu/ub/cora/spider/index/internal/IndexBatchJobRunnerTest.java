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
package se.uu.ub.cora.spider.index.internal;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertSame;
import static org.testng.Assert.assertTrue;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import se.uu.ub.cora.data.DataAtomicProvider;
import se.uu.ub.cora.logger.LoggerProvider;
import se.uu.ub.cora.spider.data.DataAtomicFactorySpy;
import se.uu.ub.cora.spider.data.DataGroupSpy;
import se.uu.ub.cora.spider.dependency.spy.RecordTypeHandlerSpy;
import se.uu.ub.cora.spider.dependency.spy.SpiderDependencyProviderSpy;
import se.uu.ub.cora.spider.log.LoggerFactorySpy;
import se.uu.ub.cora.spider.record.internal.RecordStorageSpy;
import se.uu.ub.cora.spider.spy.DataGroupTermCollectorSpy;
import se.uu.ub.cora.spider.spy.RecordIndexerSpy;
import se.uu.ub.cora.storage.StorageReadResult;

public class IndexBatchJobRunnerTest {

	private static final int INDEX_BATCH_SIZE = 1000;
	private SpiderDependencyProviderSpy dependencyProvider;
	private DataGroupSpy indexBatchJobFilter;
	private RecordStorageSpy recordStorage;
	private IndexBatchJob indexBatchJob;
	private IndexBatchJobRunner batchRunner;
	private RecordIndexerSpy recordIndexer;
	private DataGroupTermCollectorSpy termCollector;
	private DataAtomicFactorySpy dataAtomicFactory;
	private IndexBatchJobStorerSpy storerSpy;
	long lastLoopBeforeEnd;

	@BeforeMethod
	public void setUp() {
		setUpProviders();
		createDefaultParameters();
		setUpSpies();
		recordStorage.totalNumberOfMatches = 2;
		recordStorage.numberToReturnForReadList = 2;
		storerSpy = new IndexBatchJobStorerSpy();
		batchRunner = new IndexBatchJobRunner(dependencyProvider, storerSpy, indexBatchJob);
		lastLoopBeforeEnd = (indexBatchJob.totalNumberToIndex / INDEX_BATCH_SIZE);
	}

	private void setUpProviders() {
		LoggerFactorySpy loggerFactory = new LoggerFactorySpy();
		LoggerProvider.setLoggerFactory(loggerFactory);
		dataAtomicFactory = new DataAtomicFactorySpy();
		DataAtomicProvider.setDataAtomicFactory(dataAtomicFactory);
	}

	private void createDefaultParameters() {
		indexBatchJobFilter = new DataGroupSpy("filter");
		indexBatchJob = new IndexBatchJob("someRecordType", 45, indexBatchJobFilter);
		indexBatchJob.totalNumberToIndex = 11700;
	}

	private void setUpSpies() {
		Map<String, String> initInfo = new HashMap<>();
		dependencyProvider = new SpiderDependencyProviderSpy(initInfo);
		recordStorage = new RecordStorageSpy();
		termCollector = new DataGroupTermCollectorSpy();
		dependencyProvider.recordStorage = recordStorage;
		dependencyProvider.termCollector = termCollector;
		recordIndexer = new RecordIndexerSpy();
		dependencyProvider.recordIndexer = recordIndexer;
	}

	@Test
	public void testInit() {
		assertTrue(batchRunner instanceof Runnable);

		assertSame(batchRunner.getIndexBatchJob(), indexBatchJob);
		assertSame(batchRunner.getDependencyProvider(), dependencyProvider);
		assertSame(batchRunner.getBatchJobStorer(), storerSpy);
	}

	@Test
	public void testCorrectReadList() {
		batchRunner.run();

		recordStorage.MCR.assertParameter("readList", 0, "type", indexBatchJob.recordTypeToIndex);
		recordStorage.MCR.assertParameter("readList", 0, "filter", indexBatchJobFilter);
		recordStorage.MCR.assertNumberOfCallsToMethod("readList", 12);

		assertAllFiltersAreSentCorrectlyToReadList("readList");
	}

	private void assertAllFiltersAreSentCorrectlyToReadList(String methodName) {
		int from = 1;
		String toNo;
		for (int batchLoopNumber = 0; batchLoopNumber < 12; batchLoopNumber++) {
			toNo = String.valueOf(from + INDEX_BATCH_SIZE - 1);
			if (batchLoopNumber == lastLoopBeforeEnd) {
				toNo = Long.valueOf(indexBatchJob.totalNumberToIndex).toString();
			}
			assertCorrectFilterForOneLoopInBatch(String.valueOf(from), toNo, batchLoopNumber,
					methodName);
			from = from + INDEX_BATCH_SIZE;
		}
	}

	private void assertCorrectFilterForOneLoopInBatch(String from, String to, int batchLoopNumber,
			String methodName) {
		DataGroupSpy filterSentToReadList = getFilterFromRecordStorageUsingBatchLoopNumberAndMethodName(
				batchLoopNumber, methodName);
		int callNoForLoop1 = batchLoopNumber * 2;
		int callNoForLoop2 = callNoForLoop1 + 1;

		assertAtomicValueUpdatedInFilter(filterSentToReadList, "fromNo", from, callNoForLoop1);
		assertAtomicValueUpdatedInFilter(filterSentToReadList, "toNo", to, callNoForLoop2);
	}

	private void assertAtomicValueUpdatedInFilter(DataGroupSpy filterSentToReadList, String name,
			String value, int callNoForLoop) {
		indexBatchJobFilter.MCR.assertParameters("removeFirstChildWithNameInData", callNoForLoop,
				name);
		dataAtomicFactory.MCR.assertParameters("factorUsingNameInDataAndValue", callNoForLoop, name,
				value);
		filterSentToReadList.MCR.assertParameters("addChild", callNoForLoop, dataAtomicFactory.MCR
				.getReturnValue("factorUsingNameInDataAndValue", callNoForLoop));
	}

	private DataGroupSpy getFilterFromRecordStorageUsingBatchLoopNumberAndMethodName(
			int batchLoopNumber, String methodName) {
		DataGroupSpy filterSentToReadList = (DataGroupSpy) recordStorage.MCR
				.getValueForMethodNameAndCallNumberAndParameterName(methodName, batchLoopNumber,
						"filter");
		return filterSentToReadList;
	}

	@Test
	public void testCorrectReadListWhenAbstract() {
		RecordTypeHandlerSpy recordTypeHandler = dependencyProvider.recordTypeHandlerSpy;
		recordTypeHandler.isAbstract = true;
		batchRunner.run();

		recordStorage.MCR.assertParameter("readAbstractList", 0, "type",
				indexBatchJob.recordTypeToIndex);
		recordStorage.MCR.assertParameter("readAbstractList", 0, "filter", indexBatchJobFilter);

		recordStorage.MCR.assertNumberOfCallsToMethod("readAbstractList", 12);
		assertAllFiltersAreSentCorrectlyToReadList("readAbstractList");
	}

	@Test
	public void testCorrectCallForRecordTypeWhenRun() {
		batchRunner.run();

		dependencyProvider.MCR.assertParameter("getRecordTypeHandler", 0, "recordTypeId",
				indexBatchJob.recordTypeToIndex);
	}

	@Test
	public void testCorrectCallForTermCollectorWhenRun() {
		batchRunner.run();

		RecordTypeHandlerSpy recordTypeHandler = dependencyProvider.recordTypeHandlerSpy;

		termCollector.MCR.assertParameter("collectTerms", 0, "metadataId",
				recordTypeHandler.MCR.getReturnValue("getMetadataId", 0));

		termCollector.MCR.assertParameter("collectTerms", 0, "dataGroup",
				recordStorage.listOfListOfDataGroups.get(0).get(0));

		termCollector.MCR.assertParameter("collectTerms", 1, "metadataId",
				recordTypeHandler.MCR.getReturnValue("getMetadataId", 0));

		termCollector.MCR.assertParameter("collectTerms", 1, "dataGroup",
				recordStorage.listOfListOfDataGroups.get(0).get(1));

		int numOfBatchesTimesTwoWhichIsReturned = 24;
		termCollector.MCR.assertNumberOfCallsToMethod("collectTerms",
				numOfBatchesTimesTwoWhichIsReturned);
	}

	@Test
	public void testCorrectCallToIndex() {
		batchRunner.run();
		RecordTypeHandlerSpy recordTypeHandler = dependencyProvider.recordTypeHandlerSpy;

		int indexDataCall = 0;
		for (int i = 0; i < 12; i++) {
			assertCorrectParametersSentToIndex(recordTypeHandler, i, indexDataCall, 0);
			indexDataCall++;
			assertCorrectParametersSentToIndex(recordTypeHandler, i, indexDataCall, 1);
			indexDataCall++;
		}
	}

	private void assertCorrectParametersSentToIndex(RecordTypeHandlerSpy recordTypeHandler,
			int parameterIndex, int indexDataCall, int indexInReturnedList) {
		StorageReadResult storageReadResult = (StorageReadResult) recordStorage.MCR
				.getReturnValue("readList", parameterIndex);

		recordTypeHandler.MCR.assertParameter("getCombinedIdsUsingRecordId", indexDataCall,
				"recordId", "someId" + indexInReturnedList);

		recordIndexer.MCR.assertParameter("indexDataWithoutExplicitCommit", parameterIndex, "ids",
				recordTypeHandler.MCR.getReturnValue("getCombinedIdsUsingRecordId",
						parameterIndex));
		recordIndexer.MCR.assertParameter("indexDataWithoutExplicitCommit", parameterIndex,
				"collectedData", termCollector.MCR.getReturnValue("collectTerms", parameterIndex));

		recordIndexer.MCR.assertParameter("indexDataWithoutExplicitCommit", indexDataCall, "record",
				storageReadResult.listOfDataGroups.get(indexInReturnedList));
	}

	@Test
	public void testCorrectCallToBatchJobStorer() {
		indexBatchJob.numberOfProcessedRecords = 3;
		batchRunner.run();
		storerSpy.MCR.assertNumberOfCallsToMethod("store", 13);

		long expectedNumberOfIndexedReturnedFromSpy = 2;
		for (int i = 0; i < 12; i++) {
			storerSpy.MCR.assertParameter("store", i, "indexBatchJob.numberOfProcessedRecords",
					expectedNumberOfIndexedReturnedFromSpy);
			expectedNumberOfIndexedReturnedFromSpy += 2;
		}
	}

	@Test
	public void testErrorIsStoredInIndexBatchJob() {
		recordIndexer.throwErrorOnEvenCalls = true;
		batchRunner.run();

		for (int i = 0; i < 12; i++) {
			List<IndexError> errors = (List<IndexError>) storerSpy.MCR
					.getValueForMethodNameAndCallNumberAndParameterName("store", i,
							"indexBatchJob.errors");
			assertEquals(errors.size(), 1);
			assertEquals(errors.get(0).recordId, "someId1");
			assertEquals(errors.get(0).message, "Some error from spy");

		}

		List<IndexError> errors = (List<IndexError>) storerSpy.MCR
				.getValueForMethodNameAndCallNumberAndParameterName("store", 12,
						"indexBatchJob.errors");
		assertEquals(errors.size(), 0);
	}

	@Test
	public void testStatusInIndexBatchJob() {
		recordStorage.numberToReturnForReadList = 2;

		batchRunner.run();

		storerSpy.MCR.assertNumberOfCallsToMethod("store", 13);
		storerSpy.MCR.assertParameter("store", 0, "indexBatchJob.status", "started");
		storerSpy.MCR.assertParameter("store", 0, "indexBatchJob.numberOfProcessedRecords", 2L);

		storerSpy.MCR.assertParameter("store", 10, "indexBatchJob.status", "started");
		storerSpy.MCR.assertParameter("store", 10, "indexBatchJob.numberOfProcessedRecords", 22L);

		storerSpy.MCR.assertParameter("store", 11, "indexBatchJob.status", "started");
		storerSpy.MCR.assertParameter("store", 11, "indexBatchJob.numberOfProcessedRecords", 24L);

		storerSpy.MCR.assertParameter("store", 12, "indexBatchJob.status", "finished");
		storerSpy.MCR.assertParameter("store", 12, "indexBatchJob.numberOfProcessedRecords", 24L);
	}

	@Test
	public void testCorrectToInFilterWhenSmallerThanDefaultTen() {
		indexBatchJob.totalNumberToIndex = 4;
		batchRunner = new IndexBatchJobRunner(dependencyProvider, storerSpy, indexBatchJob);
		batchRunner.run();

		recordStorage.MCR.assertParameter("readList", 0, "type", indexBatchJob.recordTypeToIndex);
		Map<String, Object> parameters = recordStorage.MCR
				.getParametersForMethodAndCallNumber("readList", 0);
		DataGroupSpy filter = (DataGroupSpy) parameters.get("filter");

		assertEquals(filter.getFirstAtomicValueWithNameInData("toNo"), "4");
	}

}
