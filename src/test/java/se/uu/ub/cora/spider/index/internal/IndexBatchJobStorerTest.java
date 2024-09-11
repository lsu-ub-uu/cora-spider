/*
 * Copyright 2021, 2024 Uppsala University Library
 * Copyright 2024 Olov McKie
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

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import se.uu.ub.cora.data.DataProvider;
import se.uu.ub.cora.data.collected.CollectTerms;
import se.uu.ub.cora.data.spies.DataFactorySpy;
import se.uu.ub.cora.data.spies.DataRecordGroupSpy;
import se.uu.ub.cora.spider.dependency.spy.SpiderDependencyProviderOldSpy;
import se.uu.ub.cora.spider.spy.DataGroupTermCollectorSpy;
import se.uu.ub.cora.spider.spy.DataRecordLinkCollectorSpy;
import se.uu.ub.cora.storage.Filter;
import se.uu.ub.cora.storage.spies.RecordStorageSpy;

public class IndexBatchJobStorerTest {
	private SpiderDependencyProviderOldSpy dependencyProvider;
	private RecordStorageSpy recordStorage;
	private IndexBatchJob indexBatchJob;
	private DataGroupTermCollectorSpy termCollector;
	private DataRecordLinkCollectorSpy linkCollector;
	private DataRecordGroupHandlerForIndexBatchJobSpy dataRecordGroupHandlerForIndexBatchJobSpy;
	private DataFactorySpy dataFactory;
	private BatchJobStorer storer;

	@BeforeMethod
	public void setUp() {
		dataFactory = new DataFactorySpy();
		DataProvider.onlyForTestSetDataFactory(dataFactory);

		recordStorage = new RecordStorageSpy();
		termCollector = new DataGroupTermCollectorSpy();
		linkCollector = new DataRecordLinkCollectorSpy();
		dependencyProvider = new SpiderDependencyProviderOldSpy();
		dependencyProvider.recordStorage = recordStorage;
		dependencyProvider.termCollector = termCollector;
		dependencyProvider.linkCollector = linkCollector;

		dataRecordGroupHandlerForIndexBatchJobSpy = new DataRecordGroupHandlerForIndexBatchJobSpy();

		createDefaultBatchJob();

		DataRecordGroupSpy indexBatchJobDataGroup = createIndexBatchJobDataGroup();
		recordStorage.MRV.setDefaultReturnValuesSupplier("read", () -> indexBatchJobDataGroup);

		storer = new IndexBatchJobStorer(dependencyProvider,
				dataRecordGroupHandlerForIndexBatchJobSpy);
	}

	private DataRecordGroupSpy createIndexBatchJobDataGroup() {
		DataRecordGroupSpy dataRecordGroup = new DataRecordGroupSpy();
		dataRecordGroup.MRV.setDefaultReturnValuesSupplier("getDataDivider",
				() -> "someDataDivider");
		return dataRecordGroup;
	}

	@Test
	public void testCorrectRead() {
		storer.store(indexBatchJob);

		recordStorage.MCR.assertParameters("read", 0, "indexBatchJob", indexBatchJob.recordId);
	}

	@Test
	public void testCorrectCallToTermCollector() {
		storer.store(indexBatchJob);

		Map<String, Object> parameters = termCollector.MCR
				.getParametersForMethodAndCallNumber("collectTerms", 0);
		String metadataIdFromTypeHandler = (String) dependencyProvider.recordTypeHandlerSpy.MCR
				.getReturnValue("getDefinitionId", 0);
		assertEquals(parameters.get("metadataId"), metadataIdFromTypeHandler);
		assertSame(parameters.get("dataGroup"), recordStorage.MCR.getReturnValue("read", 0));

		DataRecordGroupSpy returnedDataGroupFromRead = (DataRecordGroupSpy) recordStorage.MCR
				.getReturnValue("read", 0);
		dependencyProvider.MCR.assertParameters("getRecordTypeHandlerUsingDataRecordGroup", 0,
				returnedDataGroupFromRead);
	}

	@Test
	public void testCorrectCallToLinkCollector() {
		storer.store(indexBatchJob);

		String metadataIdFromTypeHandler = (String) dependencyProvider.recordTypeHandlerSpy.MCR
				.getReturnValue("getDefinitionId", 0);
		var readBatchJob = recordStorage.MCR.getReturnValue("read", 0);
		var convertedBatchJob = dataFactory.MCR
				.assertCalledParametersReturn("factorGroupFromDataRecordGroup", readBatchJob);
		linkCollector.MCR.assertParameters("collectLinks", 0, metadataIdFromTypeHandler,
				convertedBatchJob);
	}

	@Test
	public void testCorrectCallToConverter() {
		storer.store(indexBatchJob);

		var readBatchJob = recordStorage.MCR.getReturnValue("read", 0);
		dataRecordGroupHandlerForIndexBatchJobSpy.MCR.assertParameters("updateDataRecordGroup", 0,
				indexBatchJob, readBatchJob);
	}

	@Test
	public void testStore() {
		storer.store(indexBatchJob);

		Map<String, Object> parameters = recordStorage.MCR
				.getParametersForMethodAndCallNumber("update", 0);
		DataRecordGroupSpy returnedDataGroupFromRead = (DataRecordGroupSpy) recordStorage.MCR
				.getReturnValue("read", 0);
		var convertedBatchJob = dataFactory.MCR.assertCalledParametersReturn(
				"factorGroupFromDataRecordGroup", returnedDataGroupFromRead);
		assertSame(parameters.get("dataRecord"), convertedBatchJob);
		assertEquals(parameters.get("type"), "indexBatchJob");
		assertEquals(parameters.get("id"), "someRecordId");
		linkCollector.MCR.assertReturn("collectLinks", 0, parameters.get("links"));
		CollectTerms collectTerms = (CollectTerms) termCollector.MCR.getReturnValue("collectTerms",
				0);
		assertSame(parameters.get("storageTerms"), collectTerms.storageTerms);
		assertEquals(parameters.get("dataDivider"), "someDataDivider");
	}

	private void createDefaultBatchJob() {
		indexBatchJob = new IndexBatchJob("someRecordType", 45, new Filter());
		indexBatchJob.numberOfProcessedRecords = 80;
		indexBatchJob.recordId = "someRecordId";
		createAndAddErrors();
	}

	private void createAndAddErrors() {
		List<IndexError> errors = new ArrayList<>();
		errors.add(new IndexError("someId3", "Error while indexing"));
		errors.add(new IndexError("someId89", "IOException while indexing"));
		indexBatchJob.errors.addAll(errors);
	}
}
