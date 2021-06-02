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

import se.uu.ub.cora.bookkeeper.linkcollector.DataRecordLinkCollector;
import se.uu.ub.cora.bookkeeper.termcollector.DataGroupTermCollector;
import se.uu.ub.cora.data.DataGroup;
import se.uu.ub.cora.data.DataRecord;
import se.uu.ub.cora.spider.dependency.SpiderDependencyProvider;
import se.uu.ub.cora.spider.recordtype.RecordTypeHandler;
import se.uu.ub.cora.storage.RecordStorage;

public class IndexBatchJobStorer implements BatchJobStorer {

	private IndexBatchJob indexBatchJob;
	private SpiderDependencyProvider dependencyProvider;
	private BatchJobConverterFactory batchJobConverterFactory;
	private RecordStorage recordStorage;
	private static final String INDEX_BATCH_JOB = "indexBatchJob";

	public IndexBatchJobStorer(SpiderDependencyProvider dependencyProvider,
			BatchJobConverterFactory converterFactory) {
		this.dependencyProvider = dependencyProvider;
		this.batchJobConverterFactory = converterFactory;
		recordStorage = dependencyProvider.getRecordStorage();
		// TODO: send one BatchJobConverter instead of BatchJobConverterFactory
	}

	@Override
	public void store(IndexBatchJob indexBatchJob) {
		this.indexBatchJob = indexBatchJob;
		DataGroup completedDataGroup = completeStoredDataGroup(indexBatchJob);

		storeUpdatedDataGroup(completedDataGroup);
	}

	private void storeUpdatedDataGroup(DataGroup completedDataGroup) {
		String metadataId = getMetadataIdFromRecordTypeHandler();
		DataGroup collectedTerms = collectTerms(completedDataGroup, metadataId);
		DataGroup collectedLinks = collectLinks(metadataId, completedDataGroup);
		String dataDivider = extractDataDivider(completedDataGroup);

		recordStorage.update(INDEX_BATCH_JOB, indexBatchJob.recordId, completedDataGroup,
				collectedTerms, collectedLinks, dataDivider);
	}

	private String getMetadataIdFromRecordTypeHandler() {
		RecordTypeHandler recordTypeHandler = dependencyProvider
				.getRecordTypeHandler(INDEX_BATCH_JOB);
		return recordTypeHandler.getMetadataId();
	}

	private DataGroup completeStoredDataGroup(IndexBatchJob indexBatchJob) {
		DataGroup dataGroup = recordStorage.read(INDEX_BATCH_JOB, indexBatchJob.recordId);
		DataGroupHandlerForIndexBatchJob converter = batchJobConverterFactory.factor();
		converter.updateDataGroup(indexBatchJob, dataGroup);
		return dataGroup;
	}

	private DataGroup collectTerms(DataGroup completedDataGroup, String metadataId) {
		DataGroupTermCollector dataGroupTermCollector = dependencyProvider
				.getDataGroupTermCollector();
		return dataGroupTermCollector.collectTerms(metadataId, completedDataGroup);
	}

	private DataGroup collectLinks(String metadataId, DataGroup completedDataGroup) {
		DataRecordLinkCollector linkCollector = dependencyProvider.getDataRecordLinkCollector();
		return linkCollector.collectLinks(metadataId, completedDataGroup, INDEX_BATCH_JOB,
				indexBatchJob.recordId);
	}

	private String extractDataDivider(DataGroup convertedDataGroup) {
		DataGroup recordInfo = convertedDataGroup.getFirstGroupWithNameInData("recordInfo");
		DataGroup dataDivider = recordInfo.getFirstGroupWithNameInData("dataDivider");
		return dataDivider.getFirstAtomicValueWithNameInData("linkedRecordId");
	}

	SpiderDependencyProvider getDependencyProvider() {
		return dependencyProvider;
	}

	BatchJobConverterFactory getBatchJobConverterFactory() {
		return batchJobConverterFactory;
	}

	@Override
	public DataRecord create(IndexBatchJob indexBatchJob) {
		// TODO Auto-generated method stub
		// recordStorage.create(INDEX_BATCH_JOB, INDEX_BATCH_JOB, null, null, null,
		// INDEX_BATCH_JOB);
		// return recordStorage.read(INDEX_BATCH_JOB, INDEX_BATCH_JOB);
		// dataGroupToRecordEnhancer.enhance(user, implementingRecordType, recordRead,
		// dataRedactor);
		return null;
	}

}