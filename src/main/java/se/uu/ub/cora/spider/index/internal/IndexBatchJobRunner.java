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

import java.util.List;

import se.uu.ub.cora.bookkeeper.termcollector.DataGroupTermCollector;
import se.uu.ub.cora.data.DataAtomicProvider;
import se.uu.ub.cora.data.DataGroup;
import se.uu.ub.cora.search.RecordIndexer;
import se.uu.ub.cora.spider.dependency.SpiderDependencyProvider;
import se.uu.ub.cora.spider.index.BatchRunner;
import se.uu.ub.cora.spider.recordtype.RecordTypeHandler;
import se.uu.ub.cora.storage.RecordStorage;
import se.uu.ub.cora.storage.StorageReadResult;

public class IndexBatchJobRunner implements BatchRunner, Runnable {

	private SpiderDependencyProvider dependencyProvider;
	private IndexBatchJob indexBatchJob;

	public IndexBatchJobRunner(SpiderDependencyProvider dependencyProvider,
			IndexBatchJob indexBatchJob) {
		this.dependencyProvider = dependencyProvider;
		this.indexBatchJob = indexBatchJob;
	}

	@Override
	public void run() {
		RecordStorage recordStorage = dependencyProvider.getRecordStorage();
		DataGroup filter = indexBatchJob.filter;
		RecordTypeHandler recordTypeHandler = dependencyProvider
				.getRecordTypeHandler(indexBatchJob.recordType);
		int numberRequestedFromListing = 0;
		int from = 0;
		int to = 9;
		setFromAndToInFilter(filter, from, to);
		while (numberRequestedFromListing < indexBatchJob.totalNumberToIndex) {

			// hur veta om abstract list?
			// yttre loop som sätter antal att läsa i filtret
			// TODO:läsa abstract list om abstract recordtype??

			StorageReadResult list = recordStorage.readList(indexBatchJob.recordType, filter);

			String metadataId = recordTypeHandler.getMetadataId();

			RecordIndexer recordIndexer = dependencyProvider.getRecordIndexer();
			for (DataGroup dataGroup : list.listOfDataGroups) {
				List<String> combinedIds = recordTypeHandler
						.getCombinedIdsUsingRecordId(indexBatchJob.recordType);
				DataGroupTermCollector termCollector = dependencyProvider
						.getDataGroupTermCollector();
				DataGroup collectedTerms = termCollector.collectTerms(metadataId, dataGroup);
				// try {
				recordIndexer.indexData(combinedIds, collectedTerms, dataGroup);
				// }catch(){
				// add to error list
			}
			numberRequestedFromListing = to;
			from = to + 1;
			to = to + 10;
			setFromAndToInFilter(filter, from, to);

		}
		// set numOfIndexed
		// indexBatchJob.numOfIndexed = 10;
		// IndexBatchJob.errors

		// send indexBatchJob to other place - storeThisShit
		// convertBackToDataGRoup
		// send dataGroup to update
		// recordUpdater.update()
		// recordStorage.update(metadataId, metadataId, dataGroup, collectedTerms, collectedTerms,
		// metadataId);

		// loop records, send each to indexing
		// list records as specified in indexBatchJob, in groups of 10
		// read indexBatchJob (to see if it should be paused)

		// WorkOrderExecutor contains code that indexes 1 record, break out to new class, use from
		// there and in here

		// update indexBatchJob with info about the ten just indexed
		// get next group of 10 repeat

		// when finished write status to indexBatchJob
	}

	private void setFromAndToInFilter(DataGroup filter, int from, int to) {
		filter.removeFirstChildWithNameInData("from");
		filter.removeFirstChildWithNameInData("to");

		filter.addChild(DataAtomicProvider.getDataAtomicUsingNameInDataAndValue("from",
				String.valueOf(from)));
		filter.addChild(
				DataAtomicProvider.getDataAtomicUsingNameInDataAndValue("to", String.valueOf(to)));
	}

	SpiderDependencyProvider getDependencyProvider() {
		return dependencyProvider;
	}

	IndexBatchJob getIndexBatchJob() {
		return indexBatchJob;
	}

}
