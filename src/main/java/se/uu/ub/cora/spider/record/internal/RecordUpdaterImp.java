/*
 * Copyright 2015, 2016, 2018, 2020, 2021, 2022, 2023, 2024 Uppsala University Library
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

import static se.uu.ub.cora.spider.extendedfunctionality.ExtendedFunctionalityPosition.UPDATE_AFTER_AUTHORIZATION;
import static se.uu.ub.cora.spider.extendedfunctionality.ExtendedFunctionalityPosition.UPDATE_AFTER_METADATA_VALIDATION;
import static se.uu.ub.cora.spider.extendedfunctionality.ExtendedFunctionalityPosition.UPDATE_AFTER_STORE;
import static se.uu.ub.cora.spider.extendedfunctionality.ExtendedFunctionalityPosition.UPDATE_BEFORE_METADATA_VALIDATION;
import static se.uu.ub.cora.spider.extendedfunctionality.ExtendedFunctionalityPosition.UPDATE_BEFORE_RETURN;
import static se.uu.ub.cora.spider.extendedfunctionality.ExtendedFunctionalityPosition.UPDATE_BEFORE_STORE;

import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import se.uu.ub.cora.bookkeeper.linkcollector.DataRecordLinkCollector;
import se.uu.ub.cora.bookkeeper.recordpart.DataRedactor;
import se.uu.ub.cora.bookkeeper.recordtype.RecordTypeHandler;
import se.uu.ub.cora.bookkeeper.termcollector.DataGroupTermCollector;
import se.uu.ub.cora.bookkeeper.validator.DataValidationException;
import se.uu.ub.cora.bookkeeper.validator.DataValidator;
import se.uu.ub.cora.bookkeeper.validator.ValidationAnswer;
import se.uu.ub.cora.data.DataAtomic;
import se.uu.ub.cora.data.DataGroup;
import se.uu.ub.cora.data.DataProvider;
import se.uu.ub.cora.data.DataRecord;
import se.uu.ub.cora.data.DataRecordGroup;
import se.uu.ub.cora.data.DataRecordLink;
import se.uu.ub.cora.data.collected.CollectTerms;
import se.uu.ub.cora.data.collected.Link;
import se.uu.ub.cora.search.RecordIndexer;
import se.uu.ub.cora.spider.authentication.Authenticator;
import se.uu.ub.cora.spider.authorization.SpiderAuthorizator;
import se.uu.ub.cora.spider.dependency.SpiderDependencyProvider;
import se.uu.ub.cora.spider.extendedfunctionality.ExtendedFunctionality;
import se.uu.ub.cora.spider.extendedfunctionality.ExtendedFunctionalityData;
import se.uu.ub.cora.spider.extendedfunctionality.ExtendedFunctionalityPosition;
import se.uu.ub.cora.spider.extendedfunctionality.ExtendedFunctionalityProvider;
import se.uu.ub.cora.spider.record.ConflictException;
import se.uu.ub.cora.spider.record.DataException;
import se.uu.ub.cora.spider.record.DataGroupToRecordEnhancer;
import se.uu.ub.cora.spider.record.RecordUpdater;
import se.uu.ub.cora.spider.unique.UniqueValidator;
import se.uu.ub.cora.storage.RecordNotFoundException;
import se.uu.ub.cora.storage.archive.RecordArchive;

public final class RecordUpdaterImp extends RecordHandler implements RecordUpdater {
	private static final String IGNORE_OVERWRITE_PROTECTION = "ignoreOverwriteProtection";
	private static final String RECORD_INFO = "recordInfo";
	private static final String UPDATED_STRING = "updated";
	private static final String TS_UPDATED = "tsUpdated";
	private static final String UPDATED_BY = "updatedBy";
	private static final String UPDATE = "update";
	private Authenticator authenticator;
	private SpiderAuthorizator spiderAuthorizator;
	private DataValidator dataValidator;
	private DataRecordLinkCollector linkCollector;
	private String definitionId;
	private ExtendedFunctionalityProvider extendedFunctionalityProvider;
	private DataGroupToRecordEnhancer dataGroupToRecordEnhancer;
	private DataGroupTermCollector dataGroupTermCollector;
	private RecordIndexer recordIndexer;
	private DataRecordGroup recordGroup;
	private RecordTypeHandler recordTypeHandler;
	private DataRecordGroup previouslyStoredRecord;
	private Set<String> writePermissions;
	private RecordArchive recordArchive;
	private String updateDefinitionId;
	private String dataDivider;

	private RecordUpdaterImp(SpiderDependencyProvider dependencyProvider,
			DataGroupToRecordEnhancer dataGroupToRecordEnhancer) {
		this.dependencyProvider = dependencyProvider;
		this.dataGroupToRecordEnhancer = dataGroupToRecordEnhancer;
		authenticator = dependencyProvider.getAuthenticator();
		spiderAuthorizator = dependencyProvider.getSpiderAuthorizator();
		dataValidator = dependencyProvider.getDataValidator();
		recordStorage = dependencyProvider.getRecordStorage();
		linkCollector = dependencyProvider.getDataRecordLinkCollector();
		dataGroupTermCollector = dependencyProvider.getDataGroupTermCollector();
		recordIndexer = dependencyProvider.getRecordIndexer();
		extendedFunctionalityProvider = dependencyProvider.getExtendedFunctionalityProvider();
		recordArchive = dependencyProvider.getRecordArchive();
	}

	public static RecordUpdaterImp usingDependencyProviderAndDataGroupToRecordEnhancer(
			SpiderDependencyProvider dependencyProvider,
			DataGroupToRecordEnhancer dataGroupToRecordEnhancer) {
		return new RecordUpdaterImp(dependencyProvider, dataGroupToRecordEnhancer);
	}

	@Override
	public DataRecord updateRecord(String authToken, String recordType, String recordId,
			DataRecordGroup recordGroup) {
		this.authToken = authToken;
		this.recordGroup = recordGroup;
		this.recordType = recordType;
		this.recordId = recordId;

		try {
			return tryToUpdateAndStoreRecord();
		} catch (DataValidationException dve) {
			throw new DataException("Data is not valid: " + dve.getMessage());
		}
	}

	private DataRecord tryToUpdateAndStoreRecord() {
		tryToGetActiveUser();
		checkUserIsAuthorizedForActionOnRecordType();
		useExtendedFunctionalityForPosition(UPDATE_AFTER_AUTHORIZATION);
		recordTypeHandler = dependencyProvider
				.getRecordTypeHandlerUsingDataRecordGroup(recordGroup);
		validateRecordTypeInDataIsSameAsSpecified(recordType);

		definitionId = recordTypeHandler.getDefinitionId();
		updateDefinitionId = recordTypeHandler.getUpdateDefinitionId();

		previouslyStoredRecord = recordStorage.read(recordType, recordId);
		checkUserIsAuthorisedToUpdatePreviouslyStoredRecord();

		doNotUpdateIfExistsNewerVersionAndCheckOverrideProtection();

		useExtendedFunctionalityForPosition(UPDATE_BEFORE_METADATA_VALIDATION);

		updateRecordInfo();
		possiblyReplaceRecordPartsUserIsNotAllowedToChange();

		validateIncomingDataAsSpecifiedInMetadata();
		useExtendedFunctionalityForPosition(UPDATE_AFTER_METADATA_VALIDATION);
		// checkRecordTypeAndIdIsSameAsInEnteredRecord();

		CollectTerms collectTerms = dataGroupTermCollector.collectTerms(definitionId, recordGroup);
		checkUserIsAuthorizedForActionOnRecordTypeAndCollectedData(recordType, collectTerms);
		validateDataForUniqueThrowErrorIfNot(collectTerms);

		DataGroup recordAsDataGroup = DataProvider.createGroupFromRecordGroup(recordGroup);
		Set<Link> collectedLinks = linkCollector.collectLinks(definitionId, recordAsDataGroup);
		checkToPartOfLinkedDataExistsInStorage(collectedLinks);

		useExtendedFunctionalityForPosition(UPDATE_BEFORE_STORE);
		// dataDivider = extractDataDividerFromData(recordGroup);
		dataDivider = recordGroup.getDataDivider();
		DataGroup recordAsDataGroupForStorage = DataProvider
				.createGroupFromRecordGroup(recordGroup);
		updateRecordInStorage(recordAsDataGroupForStorage, collectTerms, collectedLinks);

		if (recordTypeHandler.storeInArchive()) {
			try {
				recordArchive.update(dataDivider, recordType, recordId,
						recordAsDataGroupForStorage);
			} catch (RecordNotFoundException e) {
				recordArchive.create(dataDivider, recordType, recordId,
						recordAsDataGroupForStorage);
			}
		}
		indexData(collectTerms);
		useExtendedFunctionalityForPosition(UPDATE_AFTER_STORE);
		DataRedactor dataRedactor = dependencyProvider.getDataRedactor();
		DataRecord dataRecord = dataGroupToRecordEnhancer.enhance(user, recordType, recordGroup,
				dataRedactor);
		useExtendedFunctionalityBeforeReturn(dataRecord);
		return dataRecord;
	}

	private void validateDataForUniqueThrowErrorIfNot(CollectTerms collectedTerms) {
		UniqueValidator uniqueValidator = dependencyProvider.getUniqueValidator(recordStorage);
		ValidationAnswer uniqueAnswer = uniqueValidator.validateUniqueForExistingRecord(recordType,
				recordId, recordTypeHandler.getUniqueDefinitions(), collectedTerms.storageTerms);
		if (uniqueAnswer.dataIsInvalid()) {
			createAndThrowConflictExceptionForUnique(uniqueAnswer);
		}
	}

	private void createAndThrowConflictExceptionForUnique(ValidationAnswer uniqueAnswer) {
		String errorMessageTemplate = "The record could not be created as it fails unique validation with the "
				+ "following {0} error messages: {1}";
		Collection<String> errorMessages = uniqueAnswer.getErrorMessages();
		String errorMessage = MessageFormat.format(errorMessageTemplate, errorMessages.size(),
				errorMessages);
		throw ConflictException.withMessage(errorMessage);
	}

	private void tryToGetActiveUser() {
		user = authenticator.getUserForToken(authToken);
	}

	private void checkUserIsAuthorizedForActionOnRecordType() {
		spiderAuthorizator.checkUserIsAuthorizedForActionOnRecordType(user, UPDATE, recordType);
	}

	private void useExtendedFunctionalityForPosition(ExtendedFunctionalityPosition position) {
		List<ExtendedFunctionality> exFunctionality = extendedFunctionalityProvider
				.getFunctionalityForPositionAndRecordType(position, recordType);
		useExtendedFunctionality(recordGroup, exFunctionality);
	}

	private void doNotUpdateIfExistsNewerVersionAndCheckOverrideProtection() {
		// DataGroup recordInfo = recordGroup.getFirstGroupWithNameInData(RECORD_INFO);
		// boolean overwriteProtection = true;
		// overwriteProtection = readIgnoreOverwriteProtectionSettingIfExists(recordInfo,
		// overwriteProtection);
		// if (overwriteProtection) {
		// ifDifferentVersionThrowConflictException();
		// }
		if (!recordGroup.overwriteProtectionShouldBeEnforced()) {
			ifDifferentVersionThrowConflictException();
		}
		// recordGroup.setOverwriteProtectionShouldBeEnforced(false);
		// recordInfo.removeFirstChildWithNameInData(IGNORE_OVERWRITE_PROTECTION);
		// recordGroup.overwriteProtectionShouldBeEnforced()
	}

	// private boolean readIgnoreOverwriteProtectionSettingIfExists(DataGroup recordInfo,
	// boolean overwriteProtection) {
	// if (recordInfo.containsChildWithNameInData(IGNORE_OVERWRITE_PROTECTION)) {
	// overwriteProtection = getOverwriteProtectionSetting(recordInfo);
	// }
	// return overwriteProtection;
	// }

	// private boolean getOverwriteProtectionSetting(DataGroup recordInfo) {
	// String ignoreOverwriteProtection = recordInfo
	// .getFirstAtomicValueWithNameInData(IGNORE_OVERWRITE_PROTECTION);
	// return !"true".equals(ignoreOverwriteProtection);
	// }

	private void ifDifferentVersionThrowConflictException() {
		// String latestUpdatedTopDataG = getLatestDateFromARecord(recordGroup);
		String latestUpdatedTopDataG = recordGroup.getLatestTsUpdated();
		// String latestUpdatedPreviouslyStored = getLatestDateFromARecord(previouslyStoredRecord);
		String latestUpdatedPreviouslyStored = previouslyStoredRecord.getLatestTsUpdated();

		if (differentValues(latestUpdatedTopDataG, latestUpdatedPreviouslyStored)) {
			throw ConflictException
					.withMessage("Could not update record because it exist a newer version of the "
							+ "record in the storage.");
		}
	}

	private boolean differentValues(String latestUpdatedTopDataG,
			String latestUpdatedPreviouslyStored) {
		return !latestUpdatedTopDataG.equals(latestUpdatedPreviouslyStored);
	}

	// private String getLatestDateFromARecord(DataGroup dataGroup) {
	// DataGroup recordInfo = dataGroup.getFirstGroupWithNameInData(RECORD_INFO);
	// List<DataChild> updatedGsList = recordInfo.getAllChildrenWithNameInData(UPDATED_STRING);
	// if (listHasElements(updatedGsList)) {
	// DataGroup lastUpdatedG = (DataGroup) updatedGsList.get(updatedGsList.size() - 1);
	// if (lastUpdatedG.containsChildWithNameInData(TS_UPDATED)) {
	// return lastUpdatedG.getFirstAtomicValueWithNameInData(TS_UPDATED);
	// }
	// }
	// return "nonExistentUpdatedDate";
	// }

	// private boolean listHasElements(List<DataChild> updatedGsList) {
	// return !updatedGsList.isEmpty();
	// }

	private void validateRecordTypeInDataIsSameAsSpecified(String recordTypeToUpdate) {
		if (recordTypeDoesNotMatchRecordTypeFromValidationType(recordTypeToUpdate)) {
			throw new DataException("The record "
					+ "cannot be updated because the record type provided does not match the record type "
					+ "that the validation type is set to validate.");
		}
	}

	private boolean recordTypeDoesNotMatchRecordTypeFromValidationType(String recordTypeToUpdate) {
		return !recordTypeHandler.getRecordTypeId().equals(recordTypeToUpdate);
	}

	private void checkUserIsAuthorizedForActionOnRecordTypeAndCollectedData(String recordType,
			CollectTerms collectTerms) {
		spiderAuthorizator.checkUserIsAuthorizedForActionOnRecordTypeAndCollectedData(user, UPDATE,
				recordType, collectTerms.permissionTerms);
	}

	private void checkUserIsAuthorisedToUpdatePreviouslyStoredRecord() {
		CollectTerms collectedTerms = dataGroupTermCollector.collectTerms(definitionId,
				previouslyStoredRecord);

		checkUserIsAuthorisedToUpdateGivenCollectedData(collectedTerms);
	}

	private void checkUserIsAuthorisedToUpdateGivenCollectedData(CollectTerms collectedTerms) {
		writePermissions = spiderAuthorizator
				.checkGetUsersMatchedRecordPartPermissionsForActionOnRecordTypeAndCollectedData(
						user, UPDATE, recordType, collectedTerms.permissionTerms,
						recordTypeHandler.hasRecordPartWriteConstraint());
	}

	@Override
	protected ExtendedFunctionalityData createExtendedFunctionalityData(
			DataRecordGroup dataRecordGroup) {
		ExtendedFunctionalityData data = super.createExtendedFunctionalityData(dataRecordGroup);
		data.previouslyStoredDataRecordGroup = previouslyStoredRecord;
		return data;
	}

	private void updateRecordInfo() {
		DataGroup recordInfo = recordGroup.getFirstGroupWithNameInData(RECORD_INFO);
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
		addUpdateToRecordInfoFromReadData(recordInfo);
	}

	private void removeUpdateInfoFromIncomingData(DataGroup recordInfo) {
		while (recordInfo.containsChildWithNameInData(UPDATED_STRING)) {
			recordInfo.removeFirstChildWithNameInData(UPDATED_STRING);
		}
	}

	private void addUpdateToRecordInfoFromReadData(DataGroup recordInfo) {
		DataGroup recordInfoStoredRecord = getRecordInfoFromStoredData();
		List<DataGroup> updatedGroups = recordInfoStoredRecord
				.getAllGroupsWithNameInData(UPDATED_STRING);
		for (DataGroup dataGroup : updatedGroups) {
			recordInfo.addChild(dataGroup);
		}
	}

	private DataGroup getRecordInfoFromStoredData() {
		return previouslyStoredRecord.getFirstGroupWithNameInData(RECORD_INFO);
	}

	private DataGroup createUpdateInfoForThisUpdate(DataGroup recordInfo) {
		DataGroup updated = DataProvider.createGroupUsingNameInData(UPDATED_STRING);
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
		DataRecordLink updatedBy = DataProvider
				.createRecordLinkUsingNameInDataAndTypeAndId(UPDATED_BY, "user", user.id);
		updated.addChild(updatedBy);
	}

	private void setTsUpdated(DataGroup updated) {
		String currentLocalDateTime = getCurrentTimestampAsString();
		updated.addChild(
				DataProvider.createAtomicUsingNameInDataAndValue(TS_UPDATED, currentLocalDateTime));
	}

	private void possiblyReplaceRecordPartsUserIsNotAllowedToChange() {
		if (recordTypeHandler.hasRecordPartWriteConstraint()) {
			replaceRecordPartsUserIsNotAllowedToChange();
		}
	}

	private void replaceRecordPartsUserIsNotAllowedToChange() {
		DataRedactor dataRedactor = dependencyProvider.getDataRedactor();
		recordGroup = dataRedactor.replaceChildrenForConstraintsWithoutPermissions(definitionId,
				previouslyStoredRecord, recordGroup,
				recordTypeHandler.getUpdateWriteRecordPartConstraints(), writePermissions);
	}

	private void validateIncomingDataAsSpecifiedInMetadata() {
		DataGroup recordAsDataGroup = DataProvider.createGroupFromRecordGroup(recordGroup);
		ValidationAnswer validationAnswer = dataValidator.validateData(updateDefinitionId,
				recordAsDataGroup);
		boolean dataIsInvalid = validationAnswer.dataIsInvalid();
		if (dataIsInvalid) {
			throw new DataException("Data is not valid: " + validationAnswer.getErrorMessages());
		}
	}

	private void checkRecordTypeAndIdIsSameAsInEnteredRecord() {
		String id = recordGroup.getId();
		// DataGroup recordInfo = recordGroup.getFirstGroupWithNameInData(RECORD_INFO);
		// String valueFromRecord = recordInfo.getFirstAtomicValueWithNameInData("id");
		ensureValuesAreEqualThrowErrorIfNot(recordId, id);
		// DataRecordLink type1 = (DataRecordLink) recordInfo.getFirstChildWithNameInData("type");
		// // String type = type1.getLinkedRecordId();
		String type = recordGroup.getType();
		ensureValuesAreEqualThrowErrorIfNot(recordType, type);
	}

	private void checkIdIsSameAsInEnteredRecord(DataGroup recordInfo) {
		String valueFromRecord = recordInfo.getFirstAtomicValueWithNameInData("id");
		ensureValuesAreEqualThrowErrorIfNot(recordId, valueFromRecord);
	}

	private void ensureValuesAreEqualThrowErrorIfNot(String value,
			String extractedValueFromRecord) {
		if (differentValues(value, extractedValueFromRecord)) {
			throw new DataException("Value in data(" + extractedValueFromRecord
					+ ") does not match entered value(" + value + ")");
		}
	}

	private void checkTypeIsSameAsInEnteredRecord(DataGroup recordInfo) {
		String type = extractTypeFromRecordInfo(recordInfo);
		ensureValuesAreEqualThrowErrorIfNot(recordType, type);
	}

	private String extractTypeFromRecordInfo(DataGroup recordInfo) {
		DataRecordLink type = (DataRecordLink) recordInfo.getFirstChildWithNameInData("type");
		return type.getLinkedRecordId();
	}

	private void updateRecordInStorage(DataGroup recordAsDataGroupForStorage,
			CollectTerms collectTerms, Set<Link> collectedLinks) {
		recordStorage.update(recordType, recordId, recordAsDataGroupForStorage,
				collectTerms.storageTerms, collectedLinks, dataDivider);
	}

	private void indexData(CollectTerms collectTerms) {
		// List<String> ids = recordTypeHandler.getCombinedIdForIndex(recordId);
		recordIndexer.indexData(recordType, recordId, collectTerms.indexTerms, recordGroup);
	}

	private void useExtendedFunctionalityBeforeReturn(DataRecord dataRecord) {
		List<ExtendedFunctionality> extendedFunctionalityList = extendedFunctionalityProvider
				.getFunctionalityForPositionAndRecordType(UPDATE_BEFORE_RETURN, recordType);
		for (ExtendedFunctionality extendedFunctionality : extendedFunctionalityList) {
			ExtendedFunctionalityData data = createExtendedFunctionalityDataUsingDataRecord(
					dataRecord);
			extendedFunctionality.useExtendedFunctionality(data);
		}
	}

	private ExtendedFunctionalityData createExtendedFunctionalityDataUsingDataRecord(
			DataRecord dataRecord) {
		ExtendedFunctionalityData data = createExtendedFunctionalityData(
				dataRecord.getDataRecordGroup());
		data.dataRecord = dataRecord;
		data.previouslyStoredDataRecordGroup = previouslyStoredRecord;
		return data;
	}

}
