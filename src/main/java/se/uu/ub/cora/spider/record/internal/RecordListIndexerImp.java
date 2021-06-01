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

import java.util.List;

import se.uu.ub.cora.beefeater.authentication.User;
import se.uu.ub.cora.bookkeeper.validator.DataValidator;
import se.uu.ub.cora.data.DataGroup;
import se.uu.ub.cora.data.DataGroupProvider;
import se.uu.ub.cora.data.DataRecord;
import se.uu.ub.cora.spider.authentication.Authenticator;
import se.uu.ub.cora.spider.authorization.SpiderAuthorizator;
import se.uu.ub.cora.spider.dependency.SpiderDependencyProvider;
import se.uu.ub.cora.spider.dependency.SpiderInstanceProvider;
import se.uu.ub.cora.spider.index.IndexBatchHandler;
import se.uu.ub.cora.spider.index.internal.DataGroupHandlerForIndexBatchJob;
import se.uu.ub.cora.spider.index.internal.IndexBatchJob;
import se.uu.ub.cora.spider.record.RecordCreator;
import se.uu.ub.cora.spider.record.RecordListIndexer;
import se.uu.ub.cora.spider.recordtype.RecordTypeHandler;
import se.uu.ub.cora.storage.RecordStorage;

/**
 * 
 */
public class RecordListIndexerImp implements RecordListIndexer {
	private static final String FILTER = "filter";
	private SpiderDependencyProvider dependencyProvider;
	private IndexBatchHandler indexBatchHandler;
	private DataGroupHandlerForIndexBatchJob batchJobConverter;
	private String authToken;
	private String recordType;
	private DataGroup indexSettings;
	private RecordTypeHandler recordTypeHandler;

	private RecordListIndexerImp(SpiderDependencyProvider dependencyProvider,
			IndexBatchHandler indexBatchHandler,
			DataGroupHandlerForIndexBatchJob batchJobConverter) {
		this.dependencyProvider = dependencyProvider;
		this.indexBatchHandler = indexBatchHandler;
		this.batchJobConverter = batchJobConverter;
	}

	public static RecordListIndexerImp usingDependencyProvider(
			SpiderDependencyProvider dependencyProvider, IndexBatchHandler indexBatchHandler,
			DataGroupHandlerForIndexBatchJob batchJobConverter) {
		return new RecordListIndexerImp(dependencyProvider, indexBatchHandler, batchJobConverter);
	}

	@Override
	public DataRecord indexRecordList(String authToken, String recordType,
			DataGroup indexSettings) {
		this.authToken = authToken;
		this.recordType = recordType;
		recordTypeHandler = dependencyProvider.getRecordTypeHandler(recordType);
		this.indexSettings = indexSettings;
		return storeAndRunBatchJob();
	}

	private DataRecord storeAndRunBatchJob() {
		checkUserIsAuthenticatedAndAuthorized();
		validateIndexSettingAccordingToMetadata();
		IndexBatchJob indexBatchJob = collectInformationForIndexBatchJob(indexSettings);
		DataRecord createdRecord = storeIndexBatchJobInStorage(authToken, indexBatchJob);

		setRecordIdInIndexBatchJobFromCreatedRecord(indexBatchJob, createdRecord);

		indexBatchHandler.runIndexBatchJob(indexBatchJob);
		return createdRecord;
	}

	private void checkUserIsAuthenticatedAndAuthorized() {
		Authenticator authenticator = dependencyProvider.getAuthenticator();
		User user = authenticator.getUserForToken(authToken);
		SpiderAuthorizator spiderAuthorizator = dependencyProvider.getSpiderAuthorizator();
		spiderAuthorizator.checkUserIsAuthorizedForActionOnRecordType(user, "index", recordType);
	}

	private void validateIndexSettingAccordingToMetadata() {
		DataValidator dataValidator = dependencyProvider.getDataValidator();
		dataValidator.validateIndexSettings(recordType, indexSettings);
	}

	private IndexBatchJob collectInformationForIndexBatchJob(DataGroup indexSettings) {
		DataGroup filter = extractFilterFromIndexSettingsOrCreateANewOne(indexSettings);
		long totalNumberOfMatches = getTotalNumberOfMatchesFromStorageUsingFilter(filter);
		return createIndexBatchJobFromTotalNumAndFilter(filter, totalNumberOfMatches);
	}

	private DataGroup extractFilterFromIndexSettingsOrCreateANewOne(DataGroup indexSettings) {
		if (indexSettings.containsChildWithNameInData(FILTER)) {
			return extractFilterFromIndexSettings(indexSettings);
		}
		return createNewFilter();
	}

	private DataGroup extractFilterFromIndexSettings(DataGroup indexSettings) {
		return indexSettings.getFirstGroupWithNameInData(FILTER);
	}

	private DataGroup createNewFilter() {
		return DataGroupProvider.getDataGroupUsingNameInData(FILTER);
	}

	private long getTotalNumberOfMatchesFromStorageUsingFilter(DataGroup filter) {
		RecordStorage recordStorage = dependencyProvider.getRecordStorage();
		if (recordTypeHandler.isAbstract()) {
			return getTotalNumberOfRecordsForAbstractType(filter, recordStorage);
		}
		return recordStorage.getTotalNumberOfRecordsForType(recordType, filter);
	}

	private long getTotalNumberOfRecordsForAbstractType(DataGroup filter,
			RecordStorage recordStorage) {
		List<String> implementingRecordTypeIds = recordTypeHandler
				.getListOfImplementingRecordTypeIds();
		return recordStorage.getTotalNumberOfRecordsForAbstractType(recordType,
				implementingRecordTypeIds, filter);
	}

	private IndexBatchJob createIndexBatchJobFromTotalNumAndFilter(DataGroup filter,
			long totalNumberOfMatches) {
		return new IndexBatchJob(recordType, totalNumberOfMatches, filter);
	}

	private DataRecord storeIndexBatchJobInStorage(String authToken, IndexBatchJob indexBatchJob) {
		DataGroup createDataGroup = batchJobConverter.createDataGroup(indexBatchJob);
		RecordCreator recordCreator = SpiderInstanceProvider.getRecordCreator();
		return recordCreator.createAndStoreRecord(authToken, "indexBatchJob", createDataGroup);
	}

	private void setRecordIdInIndexBatchJobFromCreatedRecord(IndexBatchJob indexBatchJob,
			DataRecord createdRecord) {
		indexBatchJob.recordId = extractRecordIdFromDataRecord(createdRecord);
	}

	private String extractRecordIdFromDataRecord(DataRecord dataRecord) {
		DataGroup topLevelDataGroup = dataRecord.getDataGroup();
		DataGroup recordInfo = topLevelDataGroup.getFirstGroupWithNameInData("recordInfo");
		return recordInfo.getFirstAtomicValueWithNameInData("recordId");
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
	public DataGroupHandlerForIndexBatchJob getBatchJobConverter() {
		return batchJobConverter;
	}
}
