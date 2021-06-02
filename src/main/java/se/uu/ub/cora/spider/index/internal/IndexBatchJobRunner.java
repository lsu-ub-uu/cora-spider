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

import java.util.ArrayList;
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
	private RecordStorage recordStorage;
	private RecordTypeHandler recordTypeHandler;
	private DataGroupTermCollector termCollector;
	private RecordIndexer recordIndexer;
	private BatchJobStorer storer;
	private List<IndexError> errors = new ArrayList<>();

	public IndexBatchJobRunner(SpiderDependencyProvider dependencyProvider, BatchJobStorer storer,
			IndexBatchJob indexBatchJob) {
		this.dependencyProvider = dependencyProvider;
		this.storer = storer;
		this.indexBatchJob = indexBatchJob;
	}

	@Override
	public void run() {
		setNeededDependenciesInClass();
		String metadataId = recordTypeHandler.getMetadataId();
		ensureNumberOfIndexedIsZero();
		readListAndIndexDataInBatches(metadataId);
		updateIndexBatchJobAsFinished();
	}

	private void setNeededDependenciesInClass() {
		recordStorage = dependencyProvider.getRecordStorage();
		recordTypeHandler = dependencyProvider
				.getRecordTypeHandler(indexBatchJob.recordTypeToIndex);
		termCollector = dependencyProvider.getDataGroupTermCollector();
		recordIndexer = dependencyProvider.getRecordIndexer();
	}

	private void ensureNumberOfIndexedIsZero() {
		indexBatchJob.numberOfProcessedRecords = 0;
	}

	private void readListAndIndexDataInBatches(String metadataId) {
		int numberRequestedFromListing = 0;
		int from = 0;
		int to = 9;

		while (numberRequestedFromListing < indexBatchJob.totalNumberToIndex) {
			setFromAndToInFilter(from, to);
			// TODO: ?? read indexBatchJob (to see if it should be paused)
			readListAndIndexData(metadataId);

			numberRequestedFromListing = to;
			from = to + 1;
			to = to + 10;
		}
	}

	private void setFromAndToInFilter(int from, int to) {
		DataGroup filter = indexBatchJob.filter;
		removePreviousFromAndTo(filter);
		addFromAndTo(from, to, filter);
	}

	private void removePreviousFromAndTo(DataGroup filter) {
		filter.removeFirstChildWithNameInData("fromNo");
		filter.removeFirstChildWithNameInData("toNo");
	}

	private void addFromAndTo(int from, int to, DataGroup filter) {
		filter.addChild(DataAtomicProvider.getDataAtomicUsingNameInDataAndValue("fromNo",
				String.valueOf(from)));
		filter.addChild(DataAtomicProvider.getDataAtomicUsingNameInDataAndValue("toNo",
				String.valueOf(to)));
	}

	private void readListAndIndexData(String metadataId) {
		StorageReadResult readResult = readList(recordStorage);

		for (DataGroup dataGroup : readResult.listOfDataGroups) {
			indexData(metadataId, dataGroup);
		}

		updateAndStoreIndexBatchJob(readResult);
		clearErrors();
	}

	private StorageReadResult readList(RecordStorage recordStorage) {
		DataGroup filter = indexBatchJob.filter;
		StorageReadResult list;
		if (recordTypeHandler.isAbstract()) {
			list = recordStorage.readAbstractList(indexBatchJob.recordTypeToIndex, filter);
		} else {
			list = recordStorage.readList(indexBatchJob.recordTypeToIndex, filter);
		}
		return list;
	}

	private void indexData(String metadataId, DataGroup dataGroup) {
		String recordId = getRecordId(dataGroup);
		try {
			tryToIndexData(metadataId, recordId, dataGroup);
		} catch (Exception e) {
			IndexError error = new IndexError(recordId, e.getMessage());
			errors.add(error);
		}
	}

	private String getRecordId(DataGroup dataGroup) {
		DataGroup recordInfo = dataGroup.getFirstGroupWithNameInData("recordInfo");
		return recordInfo.getFirstAtomicValueWithNameInData("id");
	}

	private void tryToIndexData(String metadataId, String recordId, DataGroup dataGroup) {
		List<String> combinedIds = recordTypeHandler.getCombinedIdsUsingRecordId(recordId);
		DataGroup collectedTerms = termCollector.collectTerms(metadataId, dataGroup);
		recordIndexer.indexData(combinedIds, collectedTerms, dataGroup);
	}

	private void updateAndStoreIndexBatchJob(StorageReadResult readResult) {
		indexBatchJob.errors.addAll(errors);
		increaseNumOfIndexedInBatchJob(readResult);
		storeBatchJob();
	}

	private void increaseNumOfIndexedInBatchJob(StorageReadResult readResult) {
		int numberOfRecordsSentToIndex = readResult.listOfDataGroups.size();
		indexBatchJob.numberOfProcessedRecords += numberOfRecordsSentToIndex;
	}

	private void storeBatchJob() {
		storer.store(indexBatchJob);
	}

	private void clearErrors() {
		indexBatchJob.errors.clear();
		errors.clear();
	}

	private void updateIndexBatchJobAsFinished() {
		indexBatchJob.status = "finished";
		storeBatchJob();
	}

	SpiderDependencyProvider getDependencyProvider() {
		return dependencyProvider;
	}

	IndexBatchJob getIndexBatchJob() {
		return indexBatchJob;
	}

	BatchJobStorer getBatchJobStorer() {
		return storer;
	}

}
