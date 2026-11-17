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

import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import se.uu.ub.cora.data.spies.DataRecordGroupSpy;
import se.uu.ub.cora.spider.dependency.SpiderDependencyProvider;
import se.uu.ub.cora.spider.extendedfunctionality.ExtendedFunctionalityData;
import se.uu.ub.cora.spider.index.IndexBatchHandler;
import se.uu.ub.cora.spider.index.internal.IndexBatchJob;
import se.uu.ub.cora.spider.record.internal.IndexBatchHandlerSpy;
import se.uu.ub.cora.spider.spy.SpiderDependencyProviderSpy;

public class RunBatchJobExtendedFunctionalityTest {

	private RunBatchJobExtendedFunctionality extFunc;
	private DataRecordGroupSpy dataRecordGroup;
	private ExtendedFunctionalityData data;
	private IndexBatchHandlerSpy indexBatchHandler;
	private SpiderDependencyProvider dependencyProvider;

	@BeforeMethod
	private void beforeMethod() {
		indexBatchHandler = new IndexBatchHandlerSpy();
		dependencyProvider = new SpiderDependencyProviderSpy();
		extFunc = new RunBatchJobExtendedFunctionality(dependencyProvider, indexBatchHandler);
		setData();
	}

	@Test
	public void testCallRunIndexBatchJob() {
		extFunc.useExtendedFunctionality(data);

		indexBatchHandler.MCR.assertMethodWasCalled("runIndexBatchJob");
		IndexBatchJob indexBatchJob = (IndexBatchJob) indexBatchHandler.MCR
				.getParameterForMethodAndCallNumberAndParameter("runIndexBatchJob", 0,
						"indexBatchJob");
		assertEquals(indexBatchJob.recordTypeToIndex, "someRecordType");

	}

	@Test
	public void testOnlyForTestGetDependencyProvider() {
		SpiderDependencyProvider dependecyProvider2 = extFunc.onlyForTestGetDependencyProvider();
		assertSame(dependecyProvider2, dependencyProvider);
	}

	@Test
	public void testOnlyForTestGetIndexBatchHandler() {
		IndexBatchHandler indexBatchHandler2 = extFunc.onlyForTestGetIndexBatchHandler();
		assertSame(indexBatchHandler2, indexBatchHandler);
	}

	private void setData() {
		dataRecordGroup = new DataRecordGroupSpy();
		data = new ExtendedFunctionalityData();
		data.dataRecordGroup = dataRecordGroup;

		dataRecordGroup.MRV.setSpecificReturnValuesSupplier("getFirstAtomicValueWithNameInData",
				() -> "someRecordType", "recordTypeToIndex");
	}

}
