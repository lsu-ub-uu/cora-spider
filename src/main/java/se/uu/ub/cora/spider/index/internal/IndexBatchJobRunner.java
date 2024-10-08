/*
 * Copyright 2021, 2024 Uppsala University Library
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

import se.uu.ub.cora.bookkeeper.recordtype.RecordTypeHandler;
import se.uu.ub.cora.bookkeeper.termcollector.DataGroupTermCollector;
import se.uu.ub.cora.data.DataRecordGroup;
import se.uu.ub.cora.data.collected.CollectTerms;
import se.uu.ub.cora.search.RecordIndexer;
import se.uu.ub.cora.spider.dependency.SpiderDependencyProvider;
import se.uu.ub.cora.spider.index.BatchRunner;
import se.uu.ub.cora.storage.Filter;
import se.uu.ub.cora.storage.RecordStorage;
import se.uu.ub.cora.storage.StorageReadResult;

public class IndexBatchJobRunner implements BatchRunner, Runnable {

	private static final long FROM_NUMBER = 1;
	private static final long TO_NUMBER = 1000;
	private long from = FROM_NUMBER;
	private long to = TO_NUMBER;
	private SpiderDependencyProvider dependencyProvider;
	private IndexBatchJob indexBatchJob;
	private RecordStorage recordStorage;
	private RecordTypeHandler recordTypeHandler;
	private DataGroupTermCollector termCollector;
	private RecordIndexer recordIndexer;
	private BatchJobStorer storer;
	private List<IndexError> errors = new ArrayList<>();
	private long numberRequestedFromListing = 0;

	public IndexBatchJobRunner(SpiderDependencyProvider dependencyProvider, BatchJobStorer storer,
			IndexBatchJob indexBatchJob) {
		this.dependencyProvider = dependencyProvider;
		this.storer = storer;
		this.indexBatchJob = indexBatchJob;
	}

	@Override
	public void run() {
		setNeededDependenciesInClass();
		String metadataId = recordTypeHandler.getDefinitionId();
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
		try {
			while (numberRequestedFromListing < indexBatchJob.totalNumberToIndex) {
				indexOneBatch(metadataId);
			}
		} catch (Exception e) {
			IndexError error = new IndexError("IndexBatchJobRunner", e.getMessage());
			indexBatchJob.errors.add(error);
			updateIndexBatchJobAsFinished();
		}
	}

	private void indexOneBatch(String metadataId) {
		possiblySetToNumToTotalNumberToIndex();
		setFromAndToInFilter(from, to);
		readListAndIndexData(metadataId);
		prepareNextIteration();
	}

	private void prepareNextIteration() {
		numberRequestedFromListing = to;
		from = to + FROM_NUMBER;
		to = to + TO_NUMBER;
		to = possiblySetToNoToTotalNumberOfRecords(to);
	}

	private void possiblySetToNumToTotalNumberToIndex() {
		if (indexBatchJob.totalNumberToIndex < to) {
			to = indexBatchJob.totalNumberToIndex;
		}
	}

	private void setFromAndToInFilter(long from, long to) {
		Filter filter = indexBatchJob.filter;
		filter.fromNo = from;
		filter.toNo = to;
	}

	private void readListAndIndexData(String metadataId) {
		StorageReadResult readResult = readList();
		indexData(metadataId, readResult);
		updateAndStoreIndexBatchJob(readResult);
		clearErrors();
	}

	private StorageReadResult readList() {
		Filter filter = indexBatchJob.filter;
		return recordStorage.readList(indexBatchJob.recordTypeToIndex, filter);
	}

	private void indexData(String metadataId, StorageReadResult readResult) {
		for (DataRecordGroup dataRecordGroup : readResult.listOfDataRecordGroups) {
			indexRecord(metadataId, dataRecordGroup);
		}
	}

	private void indexRecord(String metadataId, DataRecordGroup dataRecordGroup) {
		try {
			tryToIndexData(metadataId, dataRecordGroup);
		} catch (Exception e) {
			String recordId = dataRecordGroup.getId();
			IndexError error = new IndexError(recordId, e.getMessage());
			errors.add(error);
		}
	}

	private void tryToIndexData(String metadataId, DataRecordGroup dataRecordGroup) {
		CollectTerms collectedTerms = termCollector.collectTerms(metadataId, dataRecordGroup);
		String recordId = dataRecordGroup.getId();
		String recordType = dataRecordGroup.getType();
		recordIndexer.indexDataWithoutExplicitCommit(recordType, recordId,
				collectedTerms.indexTerms, dataRecordGroup);
	}

	private void updateAndStoreIndexBatchJob(StorageReadResult readResult) {
		indexBatchJob.errors.addAll(errors);
		increaseNumOfIndexedInBatchJob(readResult);
		storeBatchJob();
	}

	private void increaseNumOfIndexedInBatchJob(StorageReadResult readResult) {
		int numberOfRecordsSentToIndex = readResult.listOfDataRecordGroups.size();
		indexBatchJob.numberOfProcessedRecords += numberOfRecordsSentToIndex;
	}

	private void storeBatchJob() {
		storer.store(indexBatchJob);
	}

	private void clearErrors() {
		indexBatchJob.errors.clear();
		errors.clear();
	}

	private long possiblySetToNoToTotalNumberOfRecords(long to) {
		if (to > indexBatchJob.totalNumberToIndex) {
			to = indexBatchJob.totalNumberToIndex;
		}
		return to;
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