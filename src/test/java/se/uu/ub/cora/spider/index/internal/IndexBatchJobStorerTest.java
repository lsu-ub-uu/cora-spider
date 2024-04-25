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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import se.uu.ub.cora.data.DataGroup;
import se.uu.ub.cora.data.DataProvider;
import se.uu.ub.cora.data.collected.CollectTerms;
import se.uu.ub.cora.data.spies.DataFactorySpy;
import se.uu.ub.cora.spider.data.DataAtomicSpy;
import se.uu.ub.cora.spider.data.DataGroupOldSpy;
import se.uu.ub.cora.spider.dependency.spy.SpiderDependencyProviderOldSpy;
import se.uu.ub.cora.spider.record.internal.RecordStorageOldSpy;
import se.uu.ub.cora.spider.spy.DataGroupTermCollectorSpy;
import se.uu.ub.cora.spider.spy.DataRecordLinkCollectorSpy;
import se.uu.ub.cora.storage.Filter;

public class IndexBatchJobStorerTest {
	private SpiderDependencyProviderOldSpy dependencyProvider;
	private RecordStorageOldSpy recordStorage;
	private IndexBatchJob indexBatchJob;
	private DataGroupTermCollectorSpy termCollector;
	private DataRecordLinkCollectorSpy linkCollector;
	private DataGroupHandlerForIndexBatchJobSpy dataGroupHandlerForIndexBatchJobSpy;
	private DataFactorySpy dataFactory;

	@BeforeMethod
	public void setUp() {
		dataFactory = new DataFactorySpy();
		DataProvider.onlyForTestSetDataFactory(dataFactory);

		Map<String, String> initInfo = new HashMap<>();
		recordStorage = new RecordStorageOldSpy();
		termCollector = new DataGroupTermCollectorSpy();
		linkCollector = new DataRecordLinkCollectorSpy();
		dependencyProvider = new SpiderDependencyProviderOldSpy();
		dependencyProvider.recordStorage = recordStorage;
		dependencyProvider.termCollector = termCollector;
		dependencyProvider.linkCollector = linkCollector;

		dataGroupHandlerForIndexBatchJobSpy = new DataGroupHandlerForIndexBatchJobSpy();

		createDefaultBatchJob();

		DataGroupOldSpy indexBatchJobDataGroup = createIndexBatchJobDataGroup();
		recordStorage.returnForRead = indexBatchJobDataGroup;
	}

	private DataGroupOldSpy createIndexBatchJobDataGroup() {
		DataGroupOldSpy defaultDataGroup = new DataGroupOldSpy("indexBatchJob");
		DataGroupOldSpy recordInfo = new DataGroupOldSpy("recordInfo");
		DataGroupOldSpy dataDivider = new DataGroupOldSpy("dataDivider");
		dataDivider.addChild(new DataAtomicSpy("linkedRecordId", "someDataDivider"));
		recordInfo.addChild(dataDivider);
		defaultDataGroup.addChild(recordInfo);
		return defaultDataGroup;
	}

	@Test
	public void testCorrectRead() {
		BatchJobStorer storer = new IndexBatchJobStorer(dependencyProvider,
				dataGroupHandlerForIndexBatchJobSpy);
		storer.store(indexBatchJob);

		List<?> types = (List<?>) recordStorage.MCR
				.getValueForMethodNameAndCallNumberAndParameterName("read", 0, "types");
		assertEquals(types.get(0), "indexBatchJob");
		assertEquals(types.size(), 1);

		recordStorage.MCR.assertParameter("read", 0, "id", "someRecordId");
	}

	@Test
	public void testCorrectCallToTermCollector() {
		BatchJobStorer storer = new IndexBatchJobStorer(dependencyProvider,
				dataGroupHandlerForIndexBatchJobSpy);
		storer.store(indexBatchJob);
		Map<String, Object> parameters = termCollector.MCR
				.getParametersForMethodAndCallNumber("collectTerms", 0);
		// String metadataIdFromTypeHandler = (String) dependencyProvider.recordTypeHandlerSpy.MCR
		// .getReturnValue("getMetadataId", 0);
		String metadataIdFromTypeHandler = (String) dependencyProvider.recordTypeHandlerSpy.MCR
				.getReturnValue("getDefinitionId", 0);
		assertEquals(parameters.get("metadataId"), metadataIdFromTypeHandler);
		assertSame(parameters.get("dataGroup"), recordStorage.MCR.getReturnValue("read", 0));
	}

	@Test
	public void testCorrectCallToLinkCollector() {
		BatchJobStorer storer = new IndexBatchJobStorer(dependencyProvider,
				dataGroupHandlerForIndexBatchJobSpy);

		storer.store(indexBatchJob);

		// String metadataIdFromTypeHandler = (String) dependencyProvider.recordTypeHandlerSpy.MCR
		// .getReturnValue("getMetadataId", 0);
		String metadataIdFromTypeHandler = (String) dependencyProvider.recordTypeHandlerSpy.MCR
				.getReturnValue("getDefinitionId", 0);
		var dataGroup = recordStorage.MCR.getReturnValue("read", 0);
		linkCollector.MCR.assertParameters("collectLinks", 0, metadataIdFromTypeHandler, dataGroup);
	}

	@Test
	public void testCorrectCallToConverter() {
		BatchJobStorer storer = new IndexBatchJobStorer(dependencyProvider,
				dataGroupHandlerForIndexBatchJobSpy);
		storer.store(indexBatchJob);
		assertSame(dataGroupHandlerForIndexBatchJobSpy.indexBatchJob, indexBatchJob);
		assertSame(dataGroupHandlerForIndexBatchJobSpy.dataGroup,
				recordStorage.MCR.getReturnValue("read", 0));
	}

	@Test
	public void testStore() {
		BatchJobStorer storer = new IndexBatchJobStorer(dependencyProvider,
				dataGroupHandlerForIndexBatchJobSpy);
		storer.store(indexBatchJob);

		Map<String, Object> parameters = recordStorage.MCR
				.getParametersForMethodAndCallNumber("update", 0);
		DataGroupOldSpy returnedDataGroupFromRead = (DataGroupOldSpy) recordStorage.MCR
				.getReturnValue("read", 0);
		assertSame(parameters.get("record"), returnedDataGroupFromRead);
		assertEquals(parameters.get("type"), "indexBatchJob");
		assertEquals(parameters.get("id"), "someRecordId");
		linkCollector.MCR.assertReturn("collectLinks", 0, parameters.get("linkList"));
		CollectTerms collectTerms = (CollectTerms) termCollector.MCR.getReturnValue("collectTerms",
				0);
		assertSame(parameters.get("storageTerms"), collectTerms.storageTerms);
		String dataDivider = extractDataDivider(returnedDataGroupFromRead);
		assertEquals(parameters.get("dataDivider"), dataDivider);
	}

	private String extractDataDivider(DataGroupOldSpy convertedDataGroup) {
		DataGroup recordInfo = convertedDataGroup.getFirstGroupWithNameInData("recordInfo");
		DataGroup dataDivider = recordInfo.getFirstGroupWithNameInData("dataDivider");
		String firstAtomicValueWithNameInData = dataDivider
				.getFirstAtomicValueWithNameInData("linkedRecordId");
		return firstAtomicValueWithNameInData;
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
