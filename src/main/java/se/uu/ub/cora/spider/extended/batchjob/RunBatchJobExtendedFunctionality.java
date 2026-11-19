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
import se.uu.ub.cora.storage.Filter;

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
		IndexBatchJob batchJob = fecthAndCreateBatchJob(data);
		batchHandler.runIndexBatchJob(batchJob);
	}

	private IndexBatchJob fecthAndCreateBatchJob(ExtendedFunctionalityData data) {
		DataRecordGroup dataRecordGroup = data.dataRecordGroup;
		String recordTypeForBatch = getRecordTypeToIndex(dataRecordGroup);
		long totalNumberOfMatches = getTotalNumberOfMatches(data);
		Filter filter = getFilter(data);

		return createBatchJob(recordTypeForBatch, totalNumberOfMatches, filter,
				dataRecordGroup.getId());
	}

	private IndexBatchJob createBatchJob(String recordTypeForBatch, long totalNumberOfMatches,
			Filter filter, String id) {
		IndexBatchJob batchJob = new IndexBatchJob(recordTypeForBatch, totalNumberOfMatches,
				filter);
		batchJob.recordId = id;
		return batchJob;
	}

	private Filter getFilter(ExtendedFunctionalityData data) {
		return (Filter) data.dataSharer.get("IndexBatchJob_filter");
	}

	private long getTotalNumberOfMatches(ExtendedFunctionalityData data) {
		return (long) data.dataSharer.get("IndexBatchJob_totalNumberOfMatches");
	}

	private String getRecordTypeToIndex(DataRecordGroup dataRecordGroup) {
		return dataRecordGroup.getFirstAtomicValueWithNameInData("recordTypeToIndex");
	}

	public SpiderDependencyProvider onlyForTestGetDependencyProvider() {
		return dependencyProvider;
	}

	public IndexBatchHandler onlyForTestGetIndexBatchHandler() {
		return batchHandler;
	}

}
