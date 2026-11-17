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

	private IndexBatchHandler batchHandler;
	private SpiderDependencyProvider dependencyProvider;

	public RunBatchJobExtendedFunctionality(SpiderDependencyProvider dependencyProvider,
			IndexBatchHandler batchHandler) {
		this.dependencyProvider = dependencyProvider;
		this.batchHandler = batchHandler;
	}

	@Override
	public void useExtendedFunctionality(ExtendedFunctionalityData data) {

		DataRecordGroup dataRecordGroup = data.dataRecordGroup;

		var recordTypeForBatch = dataRecordGroup
				.getFirstAtomicValueWithNameInData("recordTypeToIndex");

		Filter emptyFilter = createEmptyFilter();
		long matchesFromStorageUsingFilter = getTotalNumberOfMatchesFromStorageUsingFilter(
				recordTypeForBatch, emptyFilter);
		IndexBatchJob batchJob = new IndexBatchJob(recordTypeForBatch, matchesFromStorageUsingFilter,
				emptyFilter);

		batchHandler.runIndexBatchJob(batchJob);
	}

	private Filter createEmptyFilter() {
		DataGroupToFilter converterDataGroupToFilter = dependencyProvider
				.getDataGroupToFilterConverter();
		DataGroup newFilterDG = DataProvider.createGroupUsingNameInData("filter");
		return converterDataGroupToFilter.convert(newFilterDG);
	}

	private long getTotalNumberOfMatchesFromStorageUsingFilter(String recordTypeToIndex,
			Filter filter) {
		RecordStorage recordStorage = dependencyProvider.getRecordStorage();
		return recordStorage.getTotalNumberOfRecordsForTypes(List.of(recordTypeToIndex), filter);
	}

	// private DataRecord storeAndRunBatchJob() {
	// User user = checkUserIsAuthenticatedAndAuthorized();
	//
	// useExtendedFunctionalityUsingPosition(INDEX_BATCH_JOB_AFTER_AUTHORIZATION, user);
	//
	// validateIndexSettingAccordingToMetadataIfNotEmpty();
	// DataGroup filterAsData = extractFilterFromIndexSettingsOrCreateANewOne(indexSettings);
	// IndexBatchJob indexBatchJob = collectInformationForIndexBatchJob(indexSettings,
	// filterAsData);
	// DataRecord createdRecord = storeIndexBatchJobInStorage(authToken, indexBatchJob,
	// filterAsData);
	//
	// setRecordIdInIndexBatchJobFromCreatedRecord(indexBatchJob, createdRecord);
	//
	// indexBatchHandler.runIndexBatchJob(indexBatchJob);
	// return createdRecord;
	// }

	public SpiderDependencyProvider onlyForTestGetDependencyProvider() {
		return dependencyProvider;
	}

	public IndexBatchHandler onlyForTestGetIndexBatchHandler() {
		return batchHandler;
	}

}
