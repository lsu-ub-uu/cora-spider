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
import se.uu.ub.cora.spider.data.DataAtomicSpy;
import se.uu.ub.cora.spider.data.DataGroupSpy;
import se.uu.ub.cora.spider.dependency.spy.SpiderDependencyProviderSpy;
import se.uu.ub.cora.spider.record.internal.RecordStorageSpy;
import se.uu.ub.cora.spider.spy.DataGroupTermCollectorSpy;
import se.uu.ub.cora.spider.spy.DataRecordLinkCollectorSpy;

public class IndexBatchJobStorerTest {
	private SpiderDependencyProviderSpy dependencyProvider;
	private RecordStorageSpy recordStorage;
	private IndexBatchJob indexBatchJob;
	private DataGroupTermCollectorSpy termCollector;
	private DataRecordLinkCollectorSpy linkCollector;
	private DataGroupHandlerForIndexBatchJobSpy dataGroupHandlerForIndexBatchJobSpy;

	@BeforeMethod
	public void setUp() {
		Map<String, String> initInfo = new HashMap<>();
		recordStorage = new RecordStorageSpy();
		termCollector = new DataGroupTermCollectorSpy();
		linkCollector = new DataRecordLinkCollectorSpy();
		dependencyProvider = new SpiderDependencyProviderSpy(initInfo);
		dependencyProvider.recordStorage = recordStorage;
		dependencyProvider.termCollector = termCollector;
		dependencyProvider.linkCollector = linkCollector;

		dataGroupHandlerForIndexBatchJobSpy = new DataGroupHandlerForIndexBatchJobSpy();

		createDefaultBatchJob();

		DataGroupSpy indexBatchJobDataGroup = createIndexBatchJobDataGroup();
		recordStorage.returnForRead = indexBatchJobDataGroup;
	}

	private DataGroupSpy createIndexBatchJobDataGroup() {
		DataGroupSpy defaultDataGroup = new DataGroupSpy("indexBatchJob");
		DataGroupSpy recordInfo = new DataGroupSpy("recordInfo");
		DataGroupSpy dataDivider = new DataGroupSpy("dataDivider");
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

		recordStorage.MCR.assertParameter("read", 0, "type", "indexBatchJob");
		recordStorage.MCR.assertParameter("read", 0, "id", "someRecordId");
	}

	@Test
	public void testCorrectCallToTermCollector() {
		BatchJobStorer storer = new IndexBatchJobStorer(dependencyProvider,
				dataGroupHandlerForIndexBatchJobSpy);
		storer.store(indexBatchJob);
		Map<String, Object> parameters = termCollector.MCR
				.getParametersForMethodAndCallNumber("collectTerms", 0);
		String metadataIdFromTypeHandler = (String) dependencyProvider.recordTypeHandlerSpy.MCR
				.getReturnValue("getMetadataId", 0);
		assertEquals(parameters.get("metadataId"), metadataIdFromTypeHandler);
		assertSame(parameters.get("dataGroup"), recordStorage.MCR.getReturnValue("read", 0));

	}

	@Test
	public void testCorrectCallToLinkCollector() {
		BatchJobStorer storer = new IndexBatchJobStorer(dependencyProvider,
				dataGroupHandlerForIndexBatchJobSpy);
		storer.store(indexBatchJob);

		assertEquals(linkCollector.recordType, "indexBatchJob");
		assertEquals(linkCollector.recordId, "someRecordId");
		assertSame(linkCollector.dataGroup, recordStorage.MCR.getReturnValue("read", 0));
		String metadataIdFromTypeHandler = (String) dependencyProvider.recordTypeHandlerSpy.MCR
				.getReturnValue("getMetadataId", 0);
		assertEquals(linkCollector.metadataId, metadataIdFromTypeHandler);
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
		DataGroupSpy returnedDataGroupFromRead = (DataGroupSpy) recordStorage.MCR
				.getReturnValue("read", 0);
		assertSame(parameters.get("record"), returnedDataGroupFromRead);
		assertEquals(parameters.get("type"), "indexBatchJob");
		assertEquals(parameters.get("id"), "someRecordId");
		assertSame(parameters.get("linkList"), linkCollector.collectedDataLinks);
		assertSame(parameters.get("collectedTerms"),
				termCollector.MCR.getReturnValue("collectTerms", 0));
		String dataDivider = extractDataDivider(returnedDataGroupFromRead);
		assertEquals(parameters.get("dataDivider"), dataDivider);
	}

	private String extractDataDivider(DataGroupSpy convertedDataGroup) {
		DataGroup recordInfo = convertedDataGroup.getFirstGroupWithNameInData("recordInfo");
		DataGroup dataDivider = recordInfo.getFirstGroupWithNameInData("dataDivider");
		String firstAtomicValueWithNameInData = dataDivider
				.getFirstAtomicValueWithNameInData("linkedRecordId");
		return firstAtomicValueWithNameInData;
	}

	private void createDefaultBatchJob() {
		indexBatchJob = new IndexBatchJob("someRecordType", 45, new DataGroupSpy("filter"));
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
