/*
 * Copyright 2026 Uppsala University Library
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
package se.uu.ub.cora.spider.extended.batchjob;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertSame;

import java.util.List;

import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import se.uu.ub.cora.data.DataProvider;
import se.uu.ub.cora.data.spies.DataFactorySpy;
import se.uu.ub.cora.data.spies.DataRecordGroupSpy;
import se.uu.ub.cora.spider.dependency.SpiderDependencyProvider;
import se.uu.ub.cora.spider.dependency.spy.DataGroupToFilterSpy;
import se.uu.ub.cora.spider.extendedfunctionality.ExtendedFunctionalityData;
import se.uu.ub.cora.spider.index.IndexBatchHandler;
import se.uu.ub.cora.spider.index.internal.IndexBatchJob;
import se.uu.ub.cora.spider.record.internal.IndexBatchHandlerSpy;
import se.uu.ub.cora.spider.spy.SpiderDependencyProviderSpy;
import se.uu.ub.cora.storage.Filter;
import se.uu.ub.cora.storage.spies.RecordStorageSpy;

public class RunBatchJobExtendedFunctionalityTest {

	private RunBatchJobExtendedFunctionality extFunc;
	private DataRecordGroupSpy dataRecordGroup;
	private ExtendedFunctionalityData data;
	private IndexBatchHandlerSpy batchHandler;
	private SpiderDependencyProviderSpy dependencyProvider;
	private DataGroupToFilterSpy dataGroupToFilter;
	private DataFactorySpy dataFactory;
	private RecordStorageSpy recordStorage;

	@BeforeMethod
	private void beforeMethod() {
		batchHandler = new IndexBatchHandlerSpy();
		setupDataProvider();
		setupDependencyProvider();
		extFunc = new RunBatchJobExtendedFunctionality(dependencyProvider, batchHandler);
		setData();
	}

	private void setupDataProvider() {
		dataFactory = new DataFactorySpy();
		DataProvider.onlyForTestSetDataFactory(dataFactory);
	}

	public void setupDependencyProvider() {
		dependencyProvider = new SpiderDependencyProviderSpy();

		dataGroupToFilter = new DataGroupToFilterSpy();
		dependencyProvider.MRV.setDefaultReturnValuesSupplier("getDataGroupToFilterConverter",
				() -> dataGroupToFilter);

		recordStorage = new RecordStorageSpy();
		dependencyProvider.MRV.setDefaultReturnValuesSupplier("getRecordStorage",
				() -> recordStorage);
	}

	@Test
	public void testCallRunIndexBatchJob() {
		extFunc.useExtendedFunctionality(data);

		batchHandler.MCR.assertMethodWasCalled("runIndexBatchJob");
		IndexBatchJob batchJob = (IndexBatchJob) batchHandler.MCR
				.getParameterForMethodAndCallNumberAndParameter("runIndexBatchJob", 0,
						"indexBatchJob");

		assertBatchJob(batchJob);
	}

	private void assertBatchJob(IndexBatchJob batchJob) {
		assertRecordTypeForBatch(batchJob);
		Filter filter = assertAndReturnFilter(batchJob);
		assertTotalMatchesUsingFilterFromStorage(batchJob, filter);
	}

	private void assertRecordTypeForBatch(IndexBatchJob batchJob) {
		assertEquals(batchJob.recordTypeToIndex, "someRecordType");
	}

	private void assertTotalMatchesUsingFilterFromStorage(IndexBatchJob batchJob,
			Filter filter) {
		recordStorage.MCR.assertParameterAsEqual("getTotalNumberOfRecordsForTypes", 0, "types",
				List.of("someRecordType"));
		recordStorage.MCR.assertParameter("getTotalNumberOfRecordsForTypes", 0, "filter", filter);
		var totalNo = recordStorage.MCR.getReturnValue("getTotalNumberOfRecordsForTypes", 0);

		assertEquals(batchJob.totalNumberToIndex, totalNo);
	}

	private Filter assertAndReturnFilter(IndexBatchJob batchJob) {
		var filterDG = dataFactory.MCR.assertCalledParametersReturn("factorGroupUsingNameInData",
				"filter");
		var filter = dataGroupToFilter.MCR.assertCalledParametersReturn("convert", filterDG);
		assertEquals(batchJob.filter, filter);
		return (Filter) filter;
	}

	@Test
	public void testOnlyForTestGetDependencyProvider() {
		SpiderDependencyProvider dependecyProvider2 = extFunc.onlyForTestGetDependencyProvider();
		assertSame(dependecyProvider2, dependencyProvider);
	}

	@Test
	public void testOnlyForTestGetIndexBatchHandler() {
		IndexBatchHandler indexBatchHandler2 = extFunc.onlyForTestGetIndexBatchHandler();
		assertSame(indexBatchHandler2, batchHandler);
	}

	private void setData() {
		dataRecordGroup = new DataRecordGroupSpy();
		data = new ExtendedFunctionalityData();
		data.dataRecordGroup = dataRecordGroup;

		dataRecordGroup.MRV.setSpecificReturnValuesSupplier("getFirstAtomicValueWithNameInData",
				() -> "someRecordType", "recordTypeToIndex");
	}

}
