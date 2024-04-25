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

import static se.uu.ub.cora.spider.extendedfunctionality.ExtendedFunctionalityPosition.INDEX_BATCH_JOB_AFTER_AUTHORIZATION;

import java.util.List;

import se.uu.ub.cora.beefeater.authentication.User;
import se.uu.ub.cora.bookkeeper.recordtype.RecordTypeHandler;
import se.uu.ub.cora.bookkeeper.validator.DataValidationException;
import se.uu.ub.cora.bookkeeper.validator.DataValidator;
import se.uu.ub.cora.bookkeeper.validator.ValidationAnswer;
import se.uu.ub.cora.data.DataGroup;
import se.uu.ub.cora.data.DataProvider;
import se.uu.ub.cora.data.DataRecord;
import se.uu.ub.cora.spider.authentication.Authenticator;
import se.uu.ub.cora.spider.authorization.SpiderAuthorizator;
import se.uu.ub.cora.spider.data.DataGroupToFilter;
import se.uu.ub.cora.spider.dependency.SpiderDependencyProvider;
import se.uu.ub.cora.spider.dependency.SpiderInstanceProvider;
import se.uu.ub.cora.spider.extendedfunctionality.ExtendedFunctionality;
import se.uu.ub.cora.spider.extendedfunctionality.ExtendedFunctionalityData;
import se.uu.ub.cora.spider.extendedfunctionality.ExtendedFunctionalityPosition;
import se.uu.ub.cora.spider.extendedfunctionality.ExtendedFunctionalityProvider;
import se.uu.ub.cora.spider.index.IndexBatchHandler;
import se.uu.ub.cora.spider.index.internal.DataGroupHandlerForIndexBatchJob;
import se.uu.ub.cora.spider.index.internal.IndexBatchJob;
import se.uu.ub.cora.spider.record.RecordCreator;
import se.uu.ub.cora.spider.record.RecordListIndexer;
import se.uu.ub.cora.storage.Filter;
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
	private ExtendedFunctionalityProvider extendedFunctionalityProvider;

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
		extendedFunctionalityProvider = dependencyProvider.getExtendedFunctionalityProvider();
		recordTypeHandler = dependencyProvider.getRecordTypeHandler(recordType);
		this.indexSettings = indexSettings;
		return storeAndRunBatchJob();
	}

	private DataRecord storeAndRunBatchJob() {
		User user = checkUserIsAuthenticatedAndAuthorized();

		useExtendedFunctionalityUsingPosition(INDEX_BATCH_JOB_AFTER_AUTHORIZATION, user);

		validateIndexSettingAccordingToMetadataIfNotEmpty();
		DataGroup filterAsData = extractFilterFromIndexSettingsOrCreateANewOne(indexSettings);
		IndexBatchJob indexBatchJob = collectInformationForIndexBatchJob(indexSettings,
				filterAsData);
		DataRecord createdRecord = storeIndexBatchJobInStorage(authToken, indexBatchJob,
				filterAsData);

		setRecordIdInIndexBatchJobFromCreatedRecord(indexBatchJob, createdRecord);

		indexBatchHandler.runIndexBatchJob(indexBatchJob);
		return createdRecord;
	}

	private User checkUserIsAuthenticatedAndAuthorized() {
		Authenticator authenticator = dependencyProvider.getAuthenticator();
		User user = authenticator.getUserForToken(authToken);
		SpiderAuthorizator spiderAuthorizator = dependencyProvider.getSpiderAuthorizator();
		spiderAuthorizator.checkUserIsAuthorizedForActionOnRecordType(user, "index", recordType);
		return user;
	}

	private void useExtendedFunctionalityUsingPosition(ExtendedFunctionalityPosition position,
			User user) {
		List<ExtendedFunctionality> extendedFunctionality = extendedFunctionalityProvider
				.getFunctionalityForPositionAndRecordType(position, recordType);
		useExtendedFunctionality(extendedFunctionality, user);
	}

	protected void useExtendedFunctionality(List<ExtendedFunctionality> functionalityList,
			User user) {
		for (ExtendedFunctionality extendedFunctionality : functionalityList) {
			ExtendedFunctionalityData data = createExtendedFunctionalityData(user);
			extendedFunctionality.useExtendedFunctionality(data);
		}
	}

	protected ExtendedFunctionalityData createExtendedFunctionalityData(User user) {
		ExtendedFunctionalityData data = new ExtendedFunctionalityData();
		data.recordType = recordType;
		data.authToken = authToken;
		data.user = user;
		return data;
	}

	private void validateIndexSettingAccordingToMetadataIfNotEmpty() {
		if (indexSettings.hasChildren()) {
			validateIndexSettingAccordingToMetadata();
		}
	}

	private void validateIndexSettingAccordingToMetadata() {
		DataValidator dataValidator = dependencyProvider.getDataValidator();
		ValidationAnswer validationAnswer = dataValidator.validateIndexSettings(recordType,
				indexSettings);
		throwDataValidationExceptionIfValidationHasErrors(validationAnswer);
	}

	private void throwDataValidationExceptionIfValidationHasErrors(
			ValidationAnswer validationAnswer) {
		if (validationAnswer.dataIsInvalid()) {
			throw DataValidationException
					.withMessage("Error while validating index settings against defined metadata: "
							+ validationAnswer.getErrorMessages());
		}
	}

	private IndexBatchJob collectInformationForIndexBatchJob(DataGroup indexSettings,
			DataGroup filterAsData) {

		DataGroupToFilter converterToFilter = dependencyProvider.getDataGroupToFilterConverter();
		Filter filter = converterToFilter.convert(filterAsData);
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
		return DataProvider.createGroupUsingNameInData(FILTER);
	}

	private long getTotalNumberOfMatchesFromStorageUsingFilter(Filter filter) {
		RecordStorage recordStorage = dependencyProvider.getRecordStorage();
		List<String> listOfTypes = recordTypeHandler.getListOfRecordTypeIdsToReadFromStorage();
		return recordStorage.getTotalNumberOfRecordsForTypes(listOfTypes, filter);
	}

	private IndexBatchJob createIndexBatchJobFromTotalNumAndFilter(Filter filter,
			long totalNumberOfMatches) {
		return new IndexBatchJob(recordType, totalNumberOfMatches, filter);
	}

	private DataRecord storeIndexBatchJobInStorage(String authToken, IndexBatchJob indexBatchJob,
			DataGroup filterAsData) {
		DataGroup createDataGroup = batchJobConverter.createDataGroup(indexBatchJob, filterAsData);
		RecordCreator recordCreator = SpiderInstanceProvider.getRecordCreator();
		return recordCreator.createAndStoreRecord(authToken, "indexBatchJob", createDataGroup);
	}

	private void setRecordIdInIndexBatchJobFromCreatedRecord(IndexBatchJob indexBatchJob,
			DataRecord createdRecord) {
		indexBatchJob.recordId = createdRecord.getId();
	}

	public SpiderDependencyProvider onlyForTestGetDependencyProvider() {
		return dependencyProvider;
	}

	public IndexBatchHandler onlyForTestGetIndexBatchHandler() {
		return indexBatchHandler;
	}

	public DataGroupHandlerForIndexBatchJob onlyForTestGetBatchJobConverter() {
		return batchJobConverter;
	}
}
