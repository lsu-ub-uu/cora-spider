/*
 * Copyright 2015, 2016, 2018, 2020, 2021 Uppsala University Library
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

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import se.uu.ub.cora.beefeater.authentication.User;
import se.uu.ub.cora.bookkeeper.linkcollector.DataRecordLinkCollector;
import se.uu.ub.cora.bookkeeper.recordpart.DataRedactor;
import se.uu.ub.cora.bookkeeper.termcollector.DataGroupTermCollector;
import se.uu.ub.cora.bookkeeper.validator.DataValidator;
import se.uu.ub.cora.bookkeeper.validator.ValidationAnswer;
import se.uu.ub.cora.data.DataAtomic;
import se.uu.ub.cora.data.DataAtomicProvider;
import se.uu.ub.cora.data.DataGroup;
import se.uu.ub.cora.data.DataGroupProvider;
import se.uu.ub.cora.data.DataRecord;
import se.uu.ub.cora.search.RecordIndexer;
import se.uu.ub.cora.spider.authentication.Authenticator;
import se.uu.ub.cora.spider.authorization.SpiderAuthorizator;
import se.uu.ub.cora.spider.dependency.SpiderDependencyProvider;
import se.uu.ub.cora.spider.extendedfunctionality.ExtendedFunctionality;
import se.uu.ub.cora.spider.extendedfunctionality.ExtendedFunctionalityProvider;
import se.uu.ub.cora.spider.record.DataException;
import se.uu.ub.cora.spider.record.DataGroupToRecordEnhancer;
import se.uu.ub.cora.spider.record.RecordUpdater;
import se.uu.ub.cora.spider.recordtype.RecordTypeHandler;

public final class RecordUpdaterImp extends RecordHandler
		implements RecordUpdater {
	private static final String UPDATED_STRING = "updated";
	private static final String TS_UPDATED = "tsUpdated";
	private static final String UPDATED_BY = "updatedBy";
	private static final String UPDATE = "update";
	private static final String LINKED_RECORD_ID = "linkedRecordId";
	private Authenticator authenticator;
	private SpiderAuthorizator spiderAuthorizator;
	private DataValidator dataValidator;
	private DataRecordLinkCollector linkCollector;
	private String metadataId;
	private ExtendedFunctionalityProvider extendedFunctionalityProvider;
	private String authToken;
	private User user;
	private DataGroupToRecordEnhancer dataGroupToRecordEnhancer;
	private DataGroupTermCollector dataGroupTermCollector;
	private RecordIndexer recordIndexer;
	private DataGroup topDataGroup;
	private RecordTypeHandler recordTypeHandler;
	private SpiderDependencyProvider dependencyProvider;
	private DataGroup previouslyStoredRecord;
	private Set<String> writePermissions;

	private RecordUpdaterImp(SpiderDependencyProvider dependencyProvider,
			DataGroupToRecordEnhancer dataGroupToRecordEnhancer) {
		this.dependencyProvider = dependencyProvider;
		this.dataGroupToRecordEnhancer = dataGroupToRecordEnhancer;
		this.authenticator = dependencyProvider.getAuthenticator();
		this.spiderAuthorizator = dependencyProvider.getSpiderAuthorizator();
		this.dataValidator = dependencyProvider.getDataValidator();
		this.recordStorage = dependencyProvider.getRecordStorage();
		this.linkCollector = dependencyProvider.getDataRecordLinkCollector();
		this.dataGroupTermCollector = dependencyProvider.getDataGroupTermCollector();
		this.recordIndexer = dependencyProvider.getRecordIndexer();
		this.extendedFunctionalityProvider = dependencyProvider.getExtendedFunctionalityProvider();
	}

	public static RecordUpdaterImp usingDependencyProviderAndDataGroupToRecordEnhancer(
			SpiderDependencyProvider dependencyProvider,
			DataGroupToRecordEnhancer dataGroupToRecordEnhancer) {
		return new RecordUpdaterImp(dependencyProvider, dataGroupToRecordEnhancer);
	}

	@Override
	public DataRecord updateRecord(String authToken, String recordType, String recordId,
			DataGroup dataGroup) {
		this.authToken = authToken;
		this.topDataGroup = dataGroup;
		this.recordType = recordType;
		this.recordId = recordId;
		user = tryToGetActiveUser();
		checkUserIsAuthorizedForActionOnRecordType();

		recordTypeHandler = dependencyProvider.getRecordTypeHandler(recordType);
		metadataId = recordTypeHandler.getMetadataId();

		checkUserIsAuthorisedToUpdatePreviouslyStoredRecord();
		useExtendedFunctionalityBeforeMetadataValidation(recordType, dataGroup);

		updateRecordInfo();
		possiblyReplaceRecordPartsUserIsNotAllowedToChange();

		validateIncomingDataAsSpecifiedInMetadata();
		useExtendedFunctionalityAfterMetadataValidation(recordType, dataGroup);
		checkRecordTypeAndIdIsSameAsInEnteredRecord();

		DataGroup collectedTerms = dataGroupTermCollector.collectTerms(metadataId, topDataGroup);
		checkUserIsAuthorizedForActionOnRecordTypeAndCollectedData(recordType, collectedTerms);

		DataGroup collectedLinks = linkCollector.collectLinks(metadataId, topDataGroup, recordType,
				recordId);
		checkToPartOfLinkedDataExistsInStorage(collectedLinks);

		useExtendedFunctionalityBeforeStore(recordType, dataGroup);
		updateRecordInStorage(collectedTerms, collectedLinks);
		indexData(collectedTerms);
		useExtendedFunctionalityAfterStore(recordType, dataGroup);
		DataRedactor dataRedactor = dependencyProvider.getDataRedactor();
		return dataGroupToRecordEnhancer.enhance(user, recordType, topDataGroup, dataRedactor);
	}

	private void checkUserIsAuthorizedForActionOnRecordTypeAndCollectedData(String recordType,
			DataGroup collectedTerms) {
		spiderAuthorizator.checkUserIsAuthorizedForActionOnRecordTypeAndCollectedData(user, UPDATE,
				recordType, collectedTerms);
	}

	private User tryToGetActiveUser() {
		return authenticator.getUserForToken(authToken);
	}

	private void checkUserIsAuthorizedForActionOnRecordType() {
		spiderAuthorizator.checkUserIsAuthorizedForActionOnRecordType(user, UPDATE, recordType);
	}

	private void checkUserIsAuthorisedToUpdatePreviouslyStoredRecord() {
		previouslyStoredRecord = recordStorage.read(recordType, recordId);
		DataGroup collectedTerms = dataGroupTermCollector.collectTerms(metadataId,
				previouslyStoredRecord);

		checkUserIsAuthorisedToUpdateGivenCollectedData(collectedTerms);
	}

	private void checkUserIsAuthorisedToUpdateGivenCollectedData(DataGroup collectedTerms) {
		writePermissions = spiderAuthorizator
				.checkGetUsersMatchedRecordPartPermissionsForActionOnRecordTypeAndCollectedData(
						user, UPDATE, recordType, collectedTerms,
						recordTypeHandler.hasRecordPartWriteConstraint());
	}

	private void useExtendedFunctionalityBeforeMetadataValidation(String recordTypeToCreate,
			DataGroup dataGroup) {
		List<ExtendedFunctionality> functionalityForUpdateBeforeMetadataValidation = extendedFunctionalityProvider
				.getFunctionalityForUpdateBeforeMetadataValidation(recordTypeToCreate);
		useExtendedFunctionality(dataGroup, functionalityForUpdateBeforeMetadataValidation);
	}

	private void useExtendedFunctionality(DataGroup dataGroup,
			List<ExtendedFunctionality> functionalityForCreateAfterMetadataValidation) {
		for (ExtendedFunctionality extendedFunctionality : functionalityForCreateAfterMetadataValidation) {
			extendedFunctionality.useExtendedFunctionality(authToken, dataGroup);
		}
	}

	private void updateRecordInfo() {
		DataGroup recordInfo = topDataGroup.getFirstGroupWithNameInData("recordInfo");
		replaceUpdatedInfoWithInfoFromPreviousRecord(recordInfo);
		DataGroup updated = createUpdateInfoForThisUpdate(recordInfo);
		recordInfo.addChild(updated);
		recordInfo.removeFirstChildWithNameInData("createdBy");

		DataGroup recordInfoStoredRecord = getRecordInfoFromStoredData();

		DataGroup originalCreatedBy = recordInfoStoredRecord
				.getFirstGroupWithNameInData("createdBy");
		recordInfo.addChild(originalCreatedBy);
		recordInfo.removeFirstChildWithNameInData("tsCreated");
		DataAtomic originalTscreated = recordInfoStoredRecord
				.getFirstDataAtomicWithNameInData("tsCreated");
		recordInfo.addChild(originalTscreated);
	}

	private void replaceUpdatedInfoWithInfoFromPreviousRecord(DataGroup recordInfo) {
		removeUpdateInfoFromIncomingData(recordInfo);
		addRecordInfoFromReadData(recordInfo);
	}

	private void removeUpdateInfoFromIncomingData(DataGroup recordInfo) {
		while (recordInfo.containsChildWithNameInData(UPDATED_STRING)) {
			recordInfo.removeFirstChildWithNameInData(UPDATED_STRING);
		}
	}

	private void addRecordInfoFromReadData(DataGroup recordInfo) {
		DataGroup recordInfoStoredRecord = getRecordInfoFromStoredData();
		List<DataGroup> updatedGroups = recordInfoStoredRecord
				.getAllGroupsWithNameInData(UPDATED_STRING);
		updatedGroups.forEach(recordInfo::addChild);
	}

	private DataGroup getRecordInfoFromStoredData() {
		return previouslyStoredRecord.getFirstGroupWithNameInData("recordInfo");
	}

	private DataGroup createUpdateInfoForThisUpdate(DataGroup recordInfo) {
		DataGroup updated = DataGroupProvider.getDataGroupUsingNameInData(UPDATED_STRING);
		String repeatId = getRepeatId(recordInfo);
		updated.setRepeatId(repeatId);

		setUpdatedBy(updated);
		setTsUpdated(updated);
		return updated;
	}

	private String getRepeatId(DataGroup recordInfo) {
		List<DataGroup> updatedList = recordInfo.getAllGroupsWithNameInData(UPDATED_STRING);
		if (updatedList.isEmpty()) {
			return "0";
		}
		return calculateRepeatId(updatedList);
	}

	private String calculateRepeatId(List<DataGroup> updatedList) {
		List<Integer> repeatIds = getAllCurrentRepeatIds(updatedList);
		Integer max = Collections.max(repeatIds);
		return String.valueOf(max + 1);
	}

	private List<Integer> getAllCurrentRepeatIds(List<DataGroup> updatedList) {
		List<Integer> repeatIds = new ArrayList<>(updatedList.size());
		for (DataGroup updated : updatedList) {
			repeatIds.add(Integer.valueOf(updated.getRepeatId()));
		}
		return repeatIds;
	}

	private void setUpdatedBy(DataGroup updated) {
		DataGroup updatedBy = createUpdatedByLink();
		updated.addChild(updatedBy);
	}

	private void setTsUpdated(DataGroup updated) {
		String currentLocalDateTime = getCurrentTimestampAsString();
		updated.addChild(DataAtomicProvider.getDataAtomicUsingNameInDataAndValue(TS_UPDATED,
				currentLocalDateTime));
	}

	private DataGroup createUpdatedByLink() {
		DataGroup updatedBy = DataGroupProvider.getDataGroupUsingNameInData(UPDATED_BY);
		updatedBy.addChild(DataAtomicProvider
				.getDataAtomicUsingNameInDataAndValue("linkedRecordType", "user"));
		updatedBy.addChild(
				DataAtomicProvider.getDataAtomicUsingNameInDataAndValue(LINKED_RECORD_ID, user.id));
		return updatedBy;
	}

	private void possiblyReplaceRecordPartsUserIsNotAllowedToChange() {
		if (recordTypeHandler.hasRecordPartWriteConstraint()) {
			replaceRecordPartsUserIsNotAllowedToChange();
		}
	}

	private void replaceRecordPartsUserIsNotAllowedToChange() {
		DataRedactor dataRedactor = dependencyProvider.getDataRedactor();
		topDataGroup = dataRedactor.replaceChildrenForConstraintsWithoutPermissions(metadataId,
				previouslyStoredRecord, topDataGroup,
				recordTypeHandler.getRecordPartWriteConstraints(), writePermissions);
	}

	private void validateIncomingDataAsSpecifiedInMetadata() {
		ValidationAnswer validationAnswer = dataValidator.validateData(metadataId, topDataGroup);
		if (validationAnswer.dataIsInvalid()) {
			throw new DataException("Data is not valid: " + validationAnswer.getErrorMessages());
		}
	}

	private void useExtendedFunctionalityAfterMetadataValidation(String recordTypeToCreate,
			DataGroup dataGroup) {
		List<ExtendedFunctionality> functionalityForUpdateAfterMetadataValidation = extendedFunctionalityProvider
				.getFunctionalityForUpdateAfterMetadataValidation(recordTypeToCreate);
		useExtendedFunctionality(dataGroup, functionalityForUpdateAfterMetadataValidation);
	}

	private void checkRecordTypeAndIdIsSameAsInEnteredRecord() {
		DataGroup recordInfo = topDataGroup.getFirstGroupWithNameInData(RECORD_INFO);
		checkIdIsSameAsInEnteredRecord(recordInfo);
		checkTypeIsSameAsInEnteredRecord(recordInfo);
	}

	private void checkIdIsSameAsInEnteredRecord(DataGroup recordInfo) {
		String valueFromRecord = recordInfo.getFirstAtomicValueWithNameInData("id");
		checkValueIsSameAsValueInEnteredRecord(recordId, valueFromRecord);
	}

	private void checkValueIsSameAsValueInEnteredRecord(String value,
			String extractedValueFromRecord) {
		if (!value.equals(extractedValueFromRecord)) {
			throw new DataException("Value in data(" + extractedValueFromRecord
					+ ") does not match entered value(" + value + ")");
		}
	}

	private void checkTypeIsSameAsInEnteredRecord(DataGroup recordInfo) {
		String type = extractTypeFromRecordInfo(recordInfo);
		checkValueIsSameAsValueInEnteredRecord(recordType, type);
	}

	private String extractTypeFromRecordInfo(DataGroup recordInfo) {
		DataGroup typeGroup = recordInfo.getFirstGroupWithNameInData("type");
		return typeGroup.getFirstAtomicValueWithNameInData(LINKED_RECORD_ID);
	}

	private void updateRecordInStorage(DataGroup collectedTerms, DataGroup collectedLinks) {

		String dataDivider = extractDataDividerFromData(topDataGroup);

		recordStorage.update(recordType, recordId, topDataGroup, collectedTerms, collectedLinks,
				dataDivider);
	}

	private void useExtendedFunctionalityBeforeStore(String recordTypeToCreate,
			DataGroup dataGroup) {
		List<ExtendedFunctionality> functionalityForUpdateBeforeStore = extendedFunctionalityProvider
				.getFunctionalityForUpdateBeforeStore(recordTypeToCreate);
		useExtendedFunctionality(dataGroup, functionalityForUpdateBeforeStore);
	}

	private void indexData(DataGroup collectedTerms) {
		List<String> ids = recordTypeHandler.getCombinedIdsUsingRecordId(recordId);
		recordIndexer.indexData(ids, collectedTerms, topDataGroup);
	}

	private void useExtendedFunctionalityAfterStore(String recordTypeToCreate,
			DataGroup dataGroup) {
		List<ExtendedFunctionality> functionalityForUpdateAfterStore = extendedFunctionalityProvider
				.getFunctionalityForUpdateAfterStore(recordTypeToCreate);
		useExtendedFunctionality(dataGroup, functionalityForUpdateAfterStore);
	}

}
