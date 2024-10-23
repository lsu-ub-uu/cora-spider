/*
 * Copyright 2021, 2024 Uppsala University Library
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

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import se.uu.ub.cora.data.DataRecordGroup;
import se.uu.ub.cora.data.collected.CollectTerms;
import se.uu.ub.cora.data.spies.DataRecordGroupSpy;
import se.uu.ub.cora.logger.LoggerProvider;
import se.uu.ub.cora.spider.dependency.spy.RecordTypeHandlerOldSpy;
import se.uu.ub.cora.spider.log.LoggerFactorySpy;
import se.uu.ub.cora.spider.spy.DataGroupTermCollectorSpy;
import se.uu.ub.cora.spider.spy.RecordIndexerSpy;
import se.uu.ub.cora.spider.spy.SpiderDependencyProviderSpy;
import se.uu.ub.cora.storage.Filter;
import se.uu.ub.cora.storage.StorageReadResult;
import se.uu.ub.cora.storage.spies.RecordStorageSpy;

public class IndexBatchJobRunnerTest {
	private static final String FINISHED = "finished";
	private static final String STARTED = "started";
	private static final int TOTAL_NUMBER_TO_INDEX = 11700;
	private static final int INDEX_BATCH_SIZE = 1000;
	private SpiderDependencyProviderSpy dependencyProvider;
	private Filter indexBatchJobFilter;
	private FilterRecorderRecordStorage recordStorage;
	private IndexBatchJob indexBatchJob;
	private IndexBatchJobRunner batchRunner;
	private RecordIndexerSpy recordIndexer;
	private DataGroupTermCollectorSpy termCollector;
	private IndexBatchJobStorerSpy storerSpy;
	long lastLoopBeforeEnd;

	private int start = 0;
	private int totalNumberOfMatches = 2;
	private int numberToReturnForReadList = 2;
	private List<DataRecordGroup> listOfDataRecordGroups = new ArrayList<>();
	private RecordTypeHandlerOldSpy recordTypeHandlerSpy;

	@BeforeMethod
	public void setUp() {
		setUpProviders();
		createDefaultParameters();
		setUpSpies();
		storerSpy = new IndexBatchJobStorerSpy();
		batchRunner = new IndexBatchJobRunner(dependencyProvider, storerSpy, indexBatchJob);
		lastLoopBeforeEnd = (indexBatchJob.totalNumberToIndex / INDEX_BATCH_SIZE);
	}

	private void setUpProviders() {
		LoggerFactorySpy loggerFactory = new LoggerFactorySpy();
		LoggerProvider.setLoggerFactory(loggerFactory);
	}

	private void createDefaultParameters() {
		indexBatchJobFilter = new Filter();
		indexBatchJob = new IndexBatchJob("someRecordType", 45, indexBatchJobFilter);
		indexBatchJob.totalNumberToIndex = TOTAL_NUMBER_TO_INDEX;
	}

	private void setUpSpies() {
		dependencyProvider = new SpiderDependencyProviderSpy();
		recordStorage = new FilterRecorderRecordStorage();
		dependencyProvider.MRV.setDefaultReturnValuesSupplier("getRecordStorage",
				() -> recordStorage);
		recordStorage.MRV.setDefaultReturnValuesSupplier("readList",
				() -> createSpiderReadResult());

		termCollector = new DataGroupTermCollectorSpy();
		dependencyProvider.MRV.setDefaultReturnValuesSupplier("getDataGroupTermCollector",
				() -> termCollector);
		recordIndexer = new RecordIndexerSpy();
		dependencyProvider.MRV.setDefaultReturnValuesSupplier("getRecordIndexer",
				() -> recordIndexer);
		recordTypeHandlerSpy = new RecordTypeHandlerOldSpy();
		dependencyProvider.MRV.setDefaultReturnValuesSupplier("getRecordTypeHandler",
				() -> recordTypeHandlerSpy);
	}

	private StorageReadResult createSpiderReadResult() {
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

		recordStorage.MCR.assertParameters("readList", 0, "someRecordType", indexBatchJobFilter);
		recordStorage.MCR.assertNumberOfCallsToMethod("readList", 12);

		assertAllFiltersAreSentCorrectlyToReadList("readList");
	}

	class FilterRecorderRecordStorage extends RecordStorageSpy {
		List<Long> from = new ArrayList<>();
		List<Long> to = new ArrayList<>();

		public FilterRecorderRecordStorage() {
			super();
			MRV.setDefaultReturnValuesSupplier("readList", StorageReadResult::new);
		}

		public StorageReadResult readList(String type, Filter filter) {
			from.add(filter.fromNo);
			to.add(filter.toNo);
			return (StorageReadResult) MCR.addCallAndReturnFromMRV("type", type, "filter", filter);
		};
	}

	private void assertAllFiltersAreSentCorrectlyToReadList(String methodName) {
		long from = 1;
		long toNo;
		for (int batchLoopNumber = 0; batchLoopNumber < 12; batchLoopNumber++) {
			toNo = from + INDEX_BATCH_SIZE - 1;
			if (batchLoopNumber == lastLoopBeforeEnd) {
				toNo = indexBatchJob.totalNumberToIndex;
			}
			assertCorrectFilterForOneLoopInBatch(from, toNo, batchLoopNumber, methodName);
			from = from + INDEX_BATCH_SIZE;
		}
	}

	private void assertCorrectFilterForOneLoopInBatch(long from, long to, int batchLoopNumber,
			String methodName) {
		assertEquals(recordStorage.from.get(batchLoopNumber), from);
		assertEquals(recordStorage.to.get(batchLoopNumber), to);
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

		String definitionId = (String) recordTypeHandlerSpy.MCR.getReturnValue("getDefinitionId",
				0);
		StorageReadResult srr1 = (StorageReadResult) recordStorage.MCR.getReturnValue("readList",
				0);
		List<DataRecordGroup> firstList = srr1.listOfDataRecordGroups;
		termCollector.MCR.assertParameters("collectTerms", 0, definitionId, firstList.get(0));
		termCollector.MCR.assertParameters("collectTerms", 1, definitionId, firstList.get(1));

		int numOfBatchesTimesTwoWhichIsReturned = 24;
		termCollector.MCR.assertNumberOfCallsToMethod("collectTerms",
				numOfBatchesTimesTwoWhichIsReturned);
	}

	@Test
	public void testCorrectCallToIndexPerBatch() {
		batchRunner.run();

		termCollector.MCR.assertNumberOfCallsToMethod("collectTerms", 24);
		recordIndexer.MCR.assertNumberOfCallsToMethod("indexDataWithoutExplicitCommit", 24);
		int indexDataCall = 0;
		for (int i = 0; i < 12; i++) {
			assertCorrectParametersSentToIndex(recordTypeHandlerSpy, i, indexDataCall, 0);
			indexDataCall++;
			assertCorrectParametersSentToIndex(recordTypeHandlerSpy, i, indexDataCall, 1);
			indexDataCall++;
		}
	}

	private void assertCorrectParametersSentToIndex(RecordTypeHandlerOldSpy recordTypeHandler,
			int parameterIndex, int indexDataCall, int indexInReturnedList) {
		StorageReadResult storageReadResult = (StorageReadResult) recordStorage.MCR
				.getReturnValue("readList", parameterIndex);
		CollectTerms collectTerms = (CollectTerms) termCollector.MCR.getReturnValue("collectTerms",
				indexDataCall);
		recordIndexer.MCR.assertParameters("indexDataWithoutExplicitCommit", indexDataCall,
				"dummyRecordType", "someId" + indexInReturnedList, collectTerms.indexTerms,
				storageReadResult.listOfDataRecordGroups.get(indexInReturnedList));
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
					.getParameterForMethodAndCallNumberAndParameter("store", i,
							"indexBatchJob.errors");
			assertEquals(errors.size(), 1);
			assertEquals(errors.get(0).recordId, "someId1");
			assertEquals(errors.get(0).message, "Some error from spy");

		}

		List<IndexError> errors = (List<IndexError>) storerSpy.MCR
				.getParameterForMethodAndCallNumberAndParameter("store", 12,
						"indexBatchJob.errors");
		assertEquals(errors.size(), 0);
	}

	@Test
	public void testUnexpectedError() throws Exception {
		recordStorage.MRV.setAlwaysThrowException("readList",
				new RuntimeException("readList failed"));

		batchRunner.run();

		IndexBatchJob indexBatchJob = (IndexBatchJob) storerSpy.MCR
				.getParameterForMethodAndCallNumberAndParameter("store", 0, "indexBatchJob");
		List<IndexError> errors = indexBatchJob.errors;
		assertEquals(errors.size(), 1);
		assertEquals(errors.get(0).recordId, "IndexBatchJobRunner");
		assertEquals(errors.get(0).message, "readList failed");
		assertEquals(indexBatchJob.status, FINISHED);
	}

	@Test
	public void testStatusInIndexBatchJob() {
		numberToReturnForReadList = 2;

		batchRunner.run();

		storerSpy.MCR.assertNumberOfCallsToMethod("store", 13);
		storerSpy.MCR.assertParameter("store", 0, "indexBatchJob.status", STARTED);
		storerSpy.MCR.assertParameter("store", 0, "indexBatchJob.numberOfProcessedRecords", 2L);

		storerSpy.MCR.assertParameter("store", 10, "indexBatchJob.status", STARTED);
		storerSpy.MCR.assertParameter("store", 10, "indexBatchJob.numberOfProcessedRecords", 22L);

		storerSpy.MCR.assertParameter("store", 11, "indexBatchJob.status", STARTED);
		storerSpy.MCR.assertParameter("store", 11, "indexBatchJob.numberOfProcessedRecords", 24L);

		storerSpy.MCR.assertParameter("store", 12, "indexBatchJob.status", FINISHED);
		storerSpy.MCR.assertParameter("store", 12, "indexBatchJob.numberOfProcessedRecords", 24L);
	}

	@Test
	public void testCorrectToInFilterWhenSmallerThanDefaultTen() {
		indexBatchJob.totalNumberToIndex = 4;
		batchRunner = new IndexBatchJobRunner(dependencyProvider, storerSpy, indexBatchJob);

		batchRunner.run();

		Map<String, Object> parameters = recordStorage.MCR
				.getParametersForMethodAndCallNumber("readList", 0);
		Filter filter = (Filter) parameters.get("filter");
		assertEquals(filter.toNo, 4);
	}
}