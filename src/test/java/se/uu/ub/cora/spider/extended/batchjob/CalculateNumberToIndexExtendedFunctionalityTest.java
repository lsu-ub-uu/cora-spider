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

import se.uu.ub.cora.data.DataGroup;
import se.uu.ub.cora.data.DataProvider;
import se.uu.ub.cora.data.spies.DataFactorySpy;
import se.uu.ub.cora.data.spies.DataGroupSpy;
import se.uu.ub.cora.data.spies.DataRecordGroupSpy;
import se.uu.ub.cora.spider.dependency.SpiderDependencyProvider;
import se.uu.ub.cora.spider.dependency.spy.DataGroupToFilterSpy;
import se.uu.ub.cora.spider.extendedfunctionality.ExtendedFunctionalityData;
import se.uu.ub.cora.spider.spy.SpiderDependencyProviderSpy;
import se.uu.ub.cora.storage.spies.RecordStorageSpy;

public class CalculateNumberToIndexExtendedFunctionalityTest {

	private CalculateNumberToIndexExtendedFunctionality extFunc;
	private DataRecordGroupSpy dataRecordGroup;
	private ExtendedFunctionalityData data;
	private SpiderDependencyProviderSpy dependencyProvider;
	private DataGroupToFilterSpy dataGroupToFilter;
	private DataFactorySpy dataFactory;
	private RecordStorageSpy recordStorage;

	@BeforeMethod
	private void beforeMethod() {
		setupDataProvider();
		setupDependencyProvider();
		extFunc = new CalculateNumberToIndexExtendedFunctionality(dependencyProvider);
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
		recordStorage.MRV.setDefaultReturnValuesSupplier("getTotalNumberOfRecordsForTypes",
				() -> 32L);

		dependencyProvider.MRV.setDefaultReturnValuesSupplier("getRecordStorage",
				() -> recordStorage);

	}

	private void setData() {
		dataRecordGroup = new DataRecordGroupSpy();
		data = new ExtendedFunctionalityData();
		data.dataRecordGroup = dataRecordGroup;

		dataRecordGroup.MRV.setSpecificReturnValuesSupplier("getId", () -> "someRecordId");
		dataRecordGroup.MRV.setSpecificReturnValuesSupplier("getFirstAtomicValueWithNameInData",
				() -> "someRecordType", "recordTypeToIndex");
	}

	@Test
	public void testFilterAndTotalNumberPassedWithDataSharer() {
		dataRecordGroup.MRV.setSpecificReturnValuesSupplier("containsChildWithNameInData",
				() -> true, "filter");
		DataGroup filterGroup = new DataGroupSpy();
		dataRecordGroup.MRV.setSpecificReturnValuesSupplier("getFirstGroupWithNameInData",
				() -> filterGroup, "filter");

		extFunc.useExtendedFunctionality(data);

		var filter = assertFilter(filterGroup);
		assertTotalNoOfMatches(filter);
	}

	private void assertTotalNoOfMatches(Object filter) {
		recordStorage.MCR.assertParameterAsEqual("getTotalNumberOfRecordsForTypes", 0, "types",
				List.of("someRecordType"));
		recordStorage.MCR.assertParameter("getTotalNumberOfRecordsForTypes", 0, "filter", filter);
		var totalNo = recordStorage.MCR.getReturnValue("getTotalNumberOfRecordsForTypes", 0);
		assertEquals(data.dataSharer.get("IndexBatchJob_totalNumberOfMatches"), totalNo);
	}

	private Object assertFilter(DataGroup filterGroup) {
		var filter = dataGroupToFilter.MCR.assertCalledParametersReturn("convert", filterGroup);
		assertEquals(data.dataSharer.get("IndexBatchJob_filter"), filter);
		return filter;
	}

	@Test
	public void testAllDataStoreInDataRecordGroup() {
		extFunc.useExtendedFunctionality(data);

		var totalNumberToIndexChild = dataFactory.MCR.assertCalledParametersReturn(
				"factorAtomicUsingNameInDataAndValue", "totalNumberToIndex", "32");
		dataRecordGroup.MCR.assertParameters("addChild", 0, totalNumberToIndexChild);
	}

	@Test
	public void testOnlyForTestGetDependencyProvider() {
		SpiderDependencyProvider dependecyProvider2 = extFunc.onlyForTestGetDependencyProvider();
		assertSame(dependecyProvider2, dependencyProvider);
	}
}
