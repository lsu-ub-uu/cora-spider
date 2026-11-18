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

import java.util.List;

import se.uu.ub.cora.data.DataGroup;
import se.uu.ub.cora.data.DataProvider;
import se.uu.ub.cora.data.DataRecordGroup;
import se.uu.ub.cora.spider.data.DataGroupToFilter;
import se.uu.ub.cora.spider.dependency.SpiderDependencyProvider;
import se.uu.ub.cora.spider.extendedfunctionality.ExtendedFunctionality;
import se.uu.ub.cora.spider.extendedfunctionality.ExtendedFunctionalityData;
import se.uu.ub.cora.spider.index.IndexBatchHandler;
import se.uu.ub.cora.spider.index.internal.IndexBatchJob;
import se.uu.ub.cora.storage.Filter;
import se.uu.ub.cora.storage.RecordStorage;

public class RunBatchJobExtendedFunctionality implements ExtendedFunctionality {

	private static final String FILTER = "filter";
	private IndexBatchHandler batchHandler;
	private SpiderDependencyProvider dependencyProvider;

	public RunBatchJobExtendedFunctionality(SpiderDependencyProvider dependencyProvider,
			IndexBatchHandler batchHandler) {
		this.dependencyProvider = dependencyProvider;
		this.batchHandler = batchHandler;
	}

	@Override
	public void useExtendedFunctionality(ExtendedFunctionalityData data) {
		String recordTypeForBatch = getRecordTypeToIndex(data);
		IndexBatchJob batchJob = createBatchJobWithFilter(recordTypeForBatch, data.dataRecordGroup);
		batchHandler.runIndexBatchJob(batchJob);
	}

	private String getRecordTypeToIndex(ExtendedFunctionalityData data) {
		DataRecordGroup dataRecordGroup = data.dataRecordGroup;
		return dataRecordGroup.getFirstAtomicValueWithNameInData("recordTypeToIndex");
	}

	private IndexBatchJob createBatchJobWithFilter(String recordTypeForBatch,
			DataRecordGroup dataRecordGroup) {
		Filter filter = getFilter(dataRecordGroup);
		long matchesFromStorageUsingFilter = getTotalNumberOfMatchesFromStorageUsingFilter(
				recordTypeForBatch, filter);
		return new IndexBatchJob(recordTypeForBatch, matchesFromStorageUsingFilter, filter);
	}

	private Filter getFilter(DataRecordGroup dataRecordGroup) {
		DataGroup filterDG = extractFilterFromIndexSettingsOrCreateANewOne(dataRecordGroup);
		DataGroupToFilter converterDataGroupToFilter = dependencyProvider
				.getDataGroupToFilterConverter();
		return converterDataGroupToFilter.convert(filterDG);
	}

	private DataGroup extractFilterFromIndexSettingsOrCreateANewOne(
			DataRecordGroup dataRecordGroup) {
		if (dataRecordGroup.containsChildWithNameInData(FILTER)) {
			return extractFilterFromIndexSettings(dataRecordGroup);
		}
		return createEmptyFilter();
	}

	private DataGroup createEmptyFilter() {
		return DataProvider.createGroupUsingNameInData(FILTER);
	}

	private DataGroup extractFilterFromIndexSettings(DataRecordGroup indexSettings) {
		return indexSettings.getFirstGroupWithNameInData(FILTER);
	}

	private long getTotalNumberOfMatchesFromStorageUsingFilter(String recordTypeToIndex,
			Filter filter) {
		RecordStorage recordStorage = dependencyProvider.getRecordStorage();
		return recordStorage.getTotalNumberOfRecordsForTypes(List.of(recordTypeToIndex), filter);
	}

	public SpiderDependencyProvider onlyForTestGetDependencyProvider() {
		return dependencyProvider;
	}

	public IndexBatchHandler onlyForTestGetIndexBatchHandler() {
		return batchHandler;
	}

}
