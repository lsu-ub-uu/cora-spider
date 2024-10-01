/*
 * Copyright 2021, 2024 Uppsala University Library
 * Copyright 2024 Olov McKie
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

import java.util.Set;

import se.uu.ub.cora.bookkeeper.linkcollector.DataRecordLinkCollector;
import se.uu.ub.cora.bookkeeper.recordtype.RecordTypeHandler;
import se.uu.ub.cora.bookkeeper.termcollector.DataGroupTermCollector;
import se.uu.ub.cora.data.DataGroup;
import se.uu.ub.cora.data.DataProvider;
import se.uu.ub.cora.data.DataRecordGroup;
import se.uu.ub.cora.data.collected.CollectTerms;
import se.uu.ub.cora.data.collected.Link;
import se.uu.ub.cora.spider.dependency.SpiderDependencyProvider;
import se.uu.ub.cora.storage.RecordStorage;

public class IndexBatchJobStorer implements BatchJobStorer {
	private IndexBatchJob indexBatchJob;
	private SpiderDependencyProvider dependencyProvider;
	private RecordStorage recordStorage;
	private static final String INDEX_BATCH_JOB = "indexBatchJob";
	private DataRecordGroupHandlerForIndexBatchJob dataGroupHandlerForIndexBatchJob;

	public IndexBatchJobStorer(SpiderDependencyProvider dependencyProvider,
			DataRecordGroupHandlerForIndexBatchJob dataGroupHandlerForIndexBatchJob) {
		this.dependencyProvider = dependencyProvider;
		this.dataGroupHandlerForIndexBatchJob = dataGroupHandlerForIndexBatchJob;
		recordStorage = dependencyProvider.getRecordStorage();
	}

	@Override
	public void store(IndexBatchJob indexBatchJob) {
		this.indexBatchJob = indexBatchJob;
		DataRecordGroup completedDataRecordGroup = completeStoredDataGroup(indexBatchJob);

		storeUpdatedDataGroup(completedDataRecordGroup);
	}

	private DataRecordGroup completeStoredDataGroup(IndexBatchJob indexBatchJob) {
		DataRecordGroup dataRecordGroup = recordStorage.read(INDEX_BATCH_JOB,
				indexBatchJob.recordId);
		dataGroupHandlerForIndexBatchJob.updateDataRecordGroup(indexBatchJob, dataRecordGroup);
		return dataRecordGroup;
	}

	private void storeUpdatedDataGroup(DataRecordGroup completedDataRecordGroup) {
		String metadataId = getMetadataIdFromRecordTypeHandler(completedDataRecordGroup);
		CollectTerms collectedTerms = collectTerms(completedDataRecordGroup, metadataId);
		DataGroup groupFromRecordGroup = DataProvider
				.createGroupFromRecordGroup(completedDataRecordGroup);
		Set<Link> collectedLinks = collectLinks(metadataId, groupFromRecordGroup);
		String dataDivider = completedDataRecordGroup.getDataDivider();

		recordStorage.update(INDEX_BATCH_JOB, indexBatchJob.recordId, groupFromRecordGroup,
				collectedTerms.storageTerms, collectedLinks, dataDivider);
	}

	private String getMetadataIdFromRecordTypeHandler(DataRecordGroup completedDataRecordGroup) {
		RecordTypeHandler recordTypeHandler = dependencyProvider
				.getRecordTypeHandlerUsingDataRecordGroup(completedDataRecordGroup);
		return recordTypeHandler.getDefinitionId();
	}

	private CollectTerms collectTerms(DataRecordGroup completedDataRecordGroup, String metadataId) {
		DataGroupTermCollector dataGroupTermCollector = dependencyProvider
				.getDataGroupTermCollector();
		return dataGroupTermCollector.collectTerms(metadataId, completedDataRecordGroup);
	}

	private Set<Link> collectLinks(String metadataId, DataGroup groupFromRecordGroup) {
		DataRecordLinkCollector linkCollector = dependencyProvider.getDataRecordLinkCollector();
		return linkCollector.collectLinks(metadataId, groupFromRecordGroup);
	}

	SpiderDependencyProvider getDependencyProvider() {
		return dependencyProvider;
	}

	DataRecordGroupHandlerForIndexBatchJob getDataGroupHandlerForIndexBatchJob() {
		return dataGroupHandlerForIndexBatchJob;
	}
}
