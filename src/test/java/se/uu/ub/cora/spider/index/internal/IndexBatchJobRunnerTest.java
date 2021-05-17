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
import java.util.Map;

import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import se.uu.ub.cora.data.DataAtomicProvider;
import se.uu.ub.cora.logger.LoggerProvider;
import se.uu.ub.cora.spider.data.DataAtomicFactorySpy;
import se.uu.ub.cora.spider.data.DataGroupSpy;
import se.uu.ub.cora.spider.dependency.RecordTypeHandlerSpy;
import se.uu.ub.cora.spider.dependency.SpiderDependencyProviderSpy;
import se.uu.ub.cora.spider.log.LoggerFactorySpy;
import se.uu.ub.cora.spider.record.internal.RecordStorageSpy;
import se.uu.ub.cora.spider.spy.DataGroupTermCollectorSpy;
import se.uu.ub.cora.spider.spy.RecordIndexerSpy;
import se.uu.ub.cora.storage.StorageReadResult;

public class IndexBatchJobRunnerTest {

	private SpiderDependencyProviderSpy dependencyProvider;
	private DataGroupSpy dataGroupFilter;
	private RecordStorageSpy recordStorage;
	private IndexBatchJob indexBatchJob;
	private IndexBatchJobRunner batchRunner;
	private RecordIndexerSpy recordIndexer;
	private DataGroupTermCollectorSpy termCollector;
	private DataAtomicFactorySpy dataAtomicFactory;

	@BeforeMethod
	public void setUp() {
		setUpProviders();
		createDefaultParameters();
		setUpSpies();
		recordStorage.totalNumberOfMatches = 2;
		recordStorage.endNumberToReturn = 2;
		batchRunner = new IndexBatchJobRunner(dependencyProvider, indexBatchJob);
	}

	private void setUpProviders() {
		LoggerFactorySpy loggerFactory = new LoggerFactorySpy();
		LoggerProvider.setLoggerFactory(loggerFactory);
		dataAtomicFactory = new DataAtomicFactorySpy();
		DataAtomicProvider.setDataAtomicFactory(dataAtomicFactory);
	}

	private void createDefaultParameters() {
		dataGroupFilter = new DataGroupSpy("filter");
		indexBatchJob = new IndexBatchJob("someRecordType", dataGroupFilter);
		indexBatchJob.totalNumberToIndex = 167;
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

	}

	@Test
	public void testCorrectReadList() {
		batchRunner.run();

		recordStorage.MCR.assertParameter("readList", 0, "type", indexBatchJob.recordType);
		recordStorage.MCR.assertParameter("readList", 0, "filter", dataGroupFilter);
		recordStorage.MCR.assertNumberOfCallsToMethod("readList", 17);

		Map<String, Object> parameters = recordStorage.MCR
				.getParametersForMethodAndCallNumber("readList", 0);

		assertEquals(dataGroupFilter.removedNameInDatas.get(0), "from");
		assertEquals(dataGroupFilter.removedNameInDatas.get(1), "to");

		assertEquals(dataAtomicFactory.nameInDatas.get(0), "from");
		assertEquals(dataAtomicFactory.values.get(0), "0");
		assertEquals(dataAtomicFactory.nameInDatas.get(1), "to");
		assertEquals(dataAtomicFactory.values.get(1), "9");

		DataGroupSpy filterSentToReadList = (DataGroupSpy) parameters.get("filter");
		assertSame(filterSentToReadList.addedChildren.get(0),
				dataAtomicFactory.returnedDataAtomics.get(0));
		assertSame(filterSentToReadList.addedChildren.get(1),
				dataAtomicFactory.returnedDataAtomics.get(1));

		assertEquals(dataGroupFilter.removedNameInDatas.get(2), "from");
		assertEquals(dataGroupFilter.removedNameInDatas.get(3), "to");

		assertEquals(dataAtomicFactory.nameInDatas.get(2), "from");
		assertEquals(dataAtomicFactory.values.get(2), "10");
		assertEquals(dataAtomicFactory.nameInDatas.get(3), "to");
		assertEquals(dataAtomicFactory.values.get(3), "19");

		Map<String, Object> parameters2 = recordStorage.MCR
				.getParametersForMethodAndCallNumber("readList", 1);
		DataGroupSpy filterSentToReadList2 = (DataGroupSpy) parameters2.get("filter");
		assertSame(filterSentToReadList2.addedChildren.get(2),
				dataAtomicFactory.returnedDataAtomics.get(2));
		assertSame(filterSentToReadList2.addedChildren.get(3),
				dataAtomicFactory.returnedDataAtomics.get(3));

	}

	// @Test
	// public void testCorrectReadListWhenAbstract() {
	// // recordTypeHandler.
	// batchRunner.run();
	//
	// recordStorage.MCR.assertParameter("readAbstractList", 0, "type", indexBatchJob.recordType);
	// recordStorage.MCR.assertParameter("readAbstractList", 0, "filter", dataGroupFilter);
	//
	// assertEquals(indexBatchJob.totalNumberToIndex, recordStorage.totalNumberOfMatches);
	// }

	@Test
	public void testCorrectCallForRecordTypeWhenRun() {
		batchRunner.run();

		dependencyProvider.MCR.assertParameter("getRecordTypeHandler", 0, "recordTypeId",
				indexBatchJob.recordType);
	}

	@Test
	public void testCorrectCallForTermCollectorWhenRun() {
		batchRunner.run();

		RecordTypeHandlerSpy recordTypeHandler = dependencyProvider.recordTypeHandlerSpy;

		termCollector.MCR.assertParameter("collectTerms", 0, "metadataId",
				recordTypeHandler.MCR.getReturnValue("getMetadataId", 0));
		termCollector.MCR.assertParameter("collectTerms", 0, "dataGroup",
				recordStorage.listOfDataGroups.get(0));

		termCollector.MCR.assertParameter("collectTerms", 1, "metadataId",
				recordTypeHandler.MCR.getReturnValue("getMetadataId", 0));

		termCollector.MCR.assertParameter("collectTerms", 1, "dataGroup",
				recordStorage.listOfDataGroups.get(1));
	}

	@Test
	public void testCorrectCallToIndex() {
		batchRunner.run();
		RecordTypeHandlerSpy recordTypeHandler = dependencyProvider.recordTypeHandlerSpy;
		StorageReadResult storageReadResult = (StorageReadResult) recordStorage.MCR
				.getReturnValue("readList", 0);

		assertCorrectParametersSentToIndex(recordTypeHandler, storageReadResult, 0);
		assertCorrectParametersSentToIndex(recordTypeHandler, storageReadResult, 1);
	}

	private void assertCorrectParametersSentToIndex(RecordTypeHandlerSpy recordTypeHandler,
			StorageReadResult storageReadResult, int index) {
		recordIndexer.MCR.assertParameter("indexData", index, "ids",
				recordTypeHandler.MCR.getReturnValue("getCombinedIdsUsingRecordId", index));

		recordIndexer.MCR.assertParameter("indexData", index, "recordIndexData",
				termCollector.MCR.getReturnValue("collectTerms", index));

		recordIndexer.MCR.assertParameter("indexData", index, "record",
				storageReadResult.listOfDataGroups.get(index));
	}

}
