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
package se.uu.ub.cora.spider.record.internal;

import se.uu.ub.cora.beefeater.authentication.User;
import se.uu.ub.cora.data.DataGroup;
import se.uu.ub.cora.data.DataGroupProvider;
import se.uu.ub.cora.data.DataRecord;
import se.uu.ub.cora.spider.authentication.Authenticator;
import se.uu.ub.cora.spider.authorization.SpiderAuthorizator;
import se.uu.ub.cora.spider.dependency.SpiderDependencyProvider;
import se.uu.ub.cora.spider.dependency.SpiderInstanceProvider;
import se.uu.ub.cora.spider.index.IndexBatchHandler;
import se.uu.ub.cora.spider.index.internal.BatchJobConverter;
import se.uu.ub.cora.spider.index.internal.IndexBatchJob;
import se.uu.ub.cora.spider.record.RecordCreator;
import se.uu.ub.cora.spider.record.RecordListIndexer;
import se.uu.ub.cora.storage.RecordStorage;

/**
 * 
 */
public class RecordListIndexerImp implements RecordListIndexer {

	private static final String FILTER = "filter";

	private SpiderDependencyProvider dependencyProvider;
	private IndexBatchHandler indexBatchHandler;
	private BatchJobConverter batchJobConverter;

	private RecordListIndexerImp(SpiderDependencyProvider dependencyProvider,
			IndexBatchHandler indexBatchHandler, BatchJobConverter batchJobConverter) {
		this.dependencyProvider = dependencyProvider;
		this.indexBatchHandler = indexBatchHandler;
		this.batchJobConverter = batchJobConverter;
	}

	public static RecordListIndexerImp usingDependencyProvider(
			SpiderDependencyProvider dependencyProvider, IndexBatchHandler indexBatchHandler,
			BatchJobConverter batchJobConverter) {
		return new RecordListIndexerImp(dependencyProvider, indexBatchHandler, batchJobConverter);
	}

	@Override
	public DataRecord indexRecordList(String authToken, String recordType,
			DataGroup indexSettings) {
		checkUserIsAuthenticatedAndAuthorized(authToken, recordType);

		dependencyProvider.getDataValidator().validateIndexSettings(recordType, indexSettings);

		DataGroup filter = extractFilterFromIndexSettingsOrCreateANewOne(indexSettings);

		long totalNumberOfMatches = getTotalNumberOfMatchesFromStorage(recordType, filter);

		IndexBatchJob indexBatchJob = new IndexBatchJob(recordType, totalNumberOfMatches, filter);

		// TODO: indexBatchJob has no recordId nor status
		DataGroup createDataGroup = batchJobConverter.createDataGroup(indexBatchJob);
		RecordCreator recordCreator = SpiderInstanceProvider.getRecordCreator();
		DataRecord record = recordCreator.createAndStoreRecord(authToken, "indexBatchJob",
				createDataGroup);

		indexBatchHandler.runIndexBatchJob(indexBatchJob);

		return record;
	}

	private void checkUserIsAuthenticatedAndAuthorized(String authToken, String recordType) {
		Authenticator authenticator = dependencyProvider.getAuthenticator();
		User user = authenticator.getUserForToken(authToken);
		SpiderAuthorizator spiderAuthorizator = dependencyProvider.getSpiderAuthorizator();
		spiderAuthorizator.checkUserIsAuthorizedForActionOnRecordType(user, "index", recordType);
	}

	private DataGroup extractFilterFromIndexSettingsOrCreateANewOne(DataGroup indexSettings) {
		if (indexSettings.containsChildWithNameInData(FILTER)) {
			return extractedFilter(indexSettings);
		}
		return createNewFilter();
	}

	private DataGroup extractedFilter(DataGroup indexSettings) {
		return indexSettings.getFirstGroupWithNameInData(FILTER);
	}

	private DataGroup createNewFilter() {
		return DataGroupProvider.getDataGroupUsingNameInData(FILTER);
	}

	private long getTotalNumberOfMatchesFromStorage(String recordType, DataGroup filter) {
		RecordStorage recordStorage = dependencyProvider.getRecordStorage();
		return recordStorage.getTotalNumberOfRecords(recordType, filter);
		// TODO: If recordTypeAbstrtact call:
		// recordStorage.getTotalNumberOfAbstractRecords(recordType, implementationList, filter)
	}

	// needed for test
	public SpiderDependencyProvider getDependencyProvider() {
		return dependencyProvider;
	}

	// needed for test
	public IndexBatchHandler getIndexBatchHandler() {
		return indexBatchHandler;
	}

	// needed for test
	public BatchJobConverter getBatchJobConverter() {
		return batchJobConverter;
	}
}
