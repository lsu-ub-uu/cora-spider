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
import se.uu.ub.cora.data.DataGroup;
import se.uu.ub.cora.spider.dependency.SpiderDependencyProvider;
import se.uu.ub.cora.spider.recordtype.RecordTypeHandler;
import se.uu.ub.cora.storage.RecordStorage;

public class IndexBatchJobStorer implements BatchJobStorer {

	public IndexBatchJob indexBatchJob;
	private SpiderDependencyProvider dependencyProvider;
	private BatchJobConverterFactory converterFactory;
	private static String INDEX_BATCH_JOB = "indexBatchJob";

	// public String recordType;
	// public DataGroup filter;
	// public long totalNumberToIndex;
	// public long numberSentToIndex;
	// public List<IndexError> errors = new ArrayList<>();
	// public String status;

	public IndexBatchJobStorer(SpiderDependencyProvider dependencyProvider,
			BatchJobConverterFactory converterFactory) {
		this.dependencyProvider = dependencyProvider;
		this.converterFactory = converterFactory;
	}

	@Override
	public String store(IndexBatchJob indexBatchJob) {
		this.indexBatchJob = indexBatchJob;
		RecordStorage recordStorage = dependencyProvider.getRecordStorage();
		DataGroup dataGroup = recordStorage.read(INDEX_BATCH_JOB, indexBatchJob.recordId);
		BatchJobConverter converter = converterFactory.factor();
		DataGroup completedDataGroup = converter.updateDataGroup(indexBatchJob, dataGroup);

		RecordTypeHandler recordTypeHandler = dependencyProvider
				.getRecordTypeHandler(INDEX_BATCH_JOB);
		String metadataId = recordTypeHandler.getMetadataId();

		DataRecordLinkCollector linkCollector = dependencyProvider.getDataRecordLinkCollector();
		DataGroup collectedLinks = linkCollector.collectLinks(metadataId, completedDataGroup,
				INDEX_BATCH_JOB, indexBatchJob.recordId);

		recordStorage.update(INDEX_BATCH_JOB, indexBatchJob.recordId, completedDataGroup, null,
				collectedLinks, null);
		return "";
	}

	// DataGroup collectedTerms = dataGroupTermCollector.collectTerms(metadataId, topDataGroup);
	//

	// void update(String type, String id, DataGroup record, DataGroup collectedTerms,
	// DataGroup linkList, String dataDivider);

	// read from storage
	// update with info from indexBatchJob: numberSentToIndex, errors, status (kolla att vi inte
	// skriver över vissa status, vi måste kunna skriva "finish")
	// store

	// update(String type, String id, DataGroup record, DataGroup collectedTerms,
	// DataGroup linkList, String dataDivider);

	// convertBackToDataGRoup
	// send dataGroup to update
	// recordUpdater.update()
	// recordStorage.update(type, id, dataGroup, collectedTerms, linkList,
	// dataDivider);

}
