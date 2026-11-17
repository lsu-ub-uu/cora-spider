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

import se.uu.ub.cora.data.DataRecordGroup;
import se.uu.ub.cora.spider.dependency.SpiderDependencyProvider;
import se.uu.ub.cora.spider.extendedfunctionality.ExtendedFunctionality;
import se.uu.ub.cora.spider.extendedfunctionality.ExtendedFunctionalityData;
import se.uu.ub.cora.spider.index.IndexBatchHandler;
import se.uu.ub.cora.spider.index.internal.IndexBatchJob;

public class RunBatchJobExtendedFunctionality implements ExtendedFunctionality {

	private IndexBatchHandler indexBatchHandler;
	private SpiderDependencyProvider dependencyProvider;

	public RunBatchJobExtendedFunctionality(SpiderDependencyProvider dependencyProvider,
			IndexBatchHandler indexBatchHandler) {
		this.dependencyProvider = dependencyProvider;
		this.indexBatchHandler = indexBatchHandler;
	}

	@Override
	public void useExtendedFunctionality(ExtendedFunctionalityData data) {

		DataRecordGroup dataRecordGroup = data.dataRecordGroup;

		var recordTypeToIndex = dataRecordGroup
				.getFirstAtomicValueWithNameInData("recordTypeToIndex");
		IndexBatchJob batchJob = new IndexBatchJob(recordTypeToIndex, 0L, null);

		indexBatchHandler.runIndexBatchJob(batchJob);
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
		return indexBatchHandler;
	}

}
