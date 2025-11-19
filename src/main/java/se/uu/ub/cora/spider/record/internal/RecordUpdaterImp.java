/*
 * Copyright 2015, 2016, 2018, 2020, 2021, 2022, 2023, 2024, 2025 Uppsala University Library
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
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.function.Consumer;

import se.uu.ub.cora.bookkeeper.linkcollector.DataRecordLinkCollector;
import se.uu.ub.cora.bookkeeper.recordpart.DataRedactor;
import se.uu.ub.cora.bookkeeper.recordtype.RecordTypeHandler;
import se.uu.ub.cora.bookkeeper.termcollector.DataGroupTermCollector;
import se.uu.ub.cora.bookkeeper.validator.DataValidationException;
import se.uu.ub.cora.bookkeeper.validator.DataValidator;
import se.uu.ub.cora.bookkeeper.validator.ValidationAnswer;
import se.uu.ub.cora.data.DataGroup;
import se.uu.ub.cora.data.DataProvider;
import se.uu.ub.cora.data.DataRecord;
import se.uu.ub.cora.data.DataRecordGroup;
import se.uu.ub.cora.data.collected.CollectTerms;
import se.uu.ub.cora.data.collected.Link;
import se.uu.ub.cora.search.RecordIndexer;
import se.uu.ub.cora.spider.authentication.Authenticator;
import se.uu.ub.cora.spider.authorization.AuthorizationException;
import se.uu.ub.cora.spider.authorization.SpiderAuthorizator;
import se.uu.ub.cora.spider.cache.DataChangedSender;
import se.uu.ub.cora.spider.dependency.SpiderDependencyProvider;
import se.uu.ub.cora.spider.extendedfunctionality.ExtendedFunctionality;
import se.uu.ub.cora.spider.extendedfunctionality.ExtendedFunctionalityData;
import se.uu.ub.cora.spider.extendedfunctionality.ExtendedFunctionalityPosition;
import se.uu.ub.cora.spider.extendedfunctionality.ExtendedFunctionalityProvider;
import se.uu.ub.cora.spider.record.ConflictException;
import se.uu.ub.cora.spider.record.DataException;
import se.uu.ub.cora.spider.record.DataGroupToRecordEnhancer;
import se.uu.ub.cora.spider.record.MisuseException;
import se.uu.ub.cora.spider.record.RecordUpdater;
import se.uu.ub.cora.spider.unique.UniqueValidator;
import se.uu.ub.cora.storage.RecordNotFoundException;
import se.uu.ub.cora.storage.archive.RecordArchive;

public final class RecordUpdaterImp extends RecordHandler implements RecordUpdater {
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
		previouslyStoredRecord = recordStorage.read(recordType, recordId);
		checkUserIsAuthorizedForPermissionUnit();
		definitionId = recordTypeHandler.getDefinitionId();
		updateDefinitionId = recordTypeHandler.getUpdateDefinitionId();

		checkUserIsAuthorisedToUpdatePreviouslyStoredRecord();

		doNotUpdateIfExistsNewerVersionAndCheckOverrideProtection();

		useExtendedFunctionalityForPosition(UPDATE_BEFORE_METADATA_VALIDATION);

		replaceImmutableFieldsInRecordInfoFromPreviouslyStoredRecord();
		recordGroup.addUpdatedUsingUserIdAndTsNow(user.id);

		possiblyReplaceRecordPartsUserIsNotAllowedToChange();

		possiblyHandleVisibility();
		possiblyUseTrashBin();

		validateIncomingDataAsSpecifiedInMetadata();
		useExtendedFunctionalityForPosition(UPDATE_AFTER_METADATA_VALIDATION);
		checkRecordTypeAndIdIsSameAsInEnteredRecord();

		CollectTerms collectTerms = dataGroupTermCollector.collectTerms(definitionId, recordGroup);
		checkUserIsAuthorizedForActionOnRecordTypeAndCollectedData(recordType, collectTerms);
		validateDataForUniqueThrowErrorIfNot(collectTerms);

		DataGroup recordAsDataGroup = DataProvider.createGroupFromRecordGroup(recordGroup);
		Set<Link> collectedLinks = linkCollector.collectLinks(definitionId, recordAsDataGroup);
		checkToPartOfLinkedDataExistsInStorage(collectedLinks);

		useExtendedFunctionalityForPosition(UPDATE_BEFORE_STORE);
		dataDivider = recordGroup.getDataDivider();
		DataGroup recordAsDataGroupForStorage = DataProvider
				.createGroupFromRecordGroup(recordGroup);

		if (isInTrashBin()) {
			collectedLinks = Collections.emptySet();
		}
		updateRecordInStorage(recordAsDataGroupForStorage, collectTerms, collectedLinks);
		sendDataChanged();
		possiblyStoreInArchive(recordAsDataGroupForStorage);

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

	private void checkUserIsAuthorizedForPermissionUnit() {
		if (recordTypeUsesPermissionUnits()) {
			getPermissionUnitFromPrevoiusRecordAndCheckIfAuthorized();
			getPermissionUnitFromUpdatedRecordAndCheckIfAuthorized();
		}
	}

	private boolean recordTypeUsesPermissionUnits() {
		return recordTypeHandler.usePermissionUnit();
	}

	private void getPermissionUnitFromPrevoiusRecordAndCheckIfAuthorized() {
		previouslyStoredRecord.getPermissionUnit().ifPresentOrElse(
				checkUserIsAuthorizedForPemissionUnit(), this::userIsUnAuthorizedForPermissionUnit);
	}

	private Consumer<? super String> checkUserIsAuthorizedForPemissionUnit() {
		return recordPermissionUnit -> spiderAuthorizator
				.checkUserIsAuthorizedForPemissionUnit(user, recordPermissionUnit);
	}

	private void userIsUnAuthorizedForPermissionUnit() {
		throw new AuthorizationException(
				MessageFormat.format("User {0} is not authorized to delete record.", user.id));
	}

	private void getPermissionUnitFromUpdatedRecordAndCheckIfAuthorized() {
		recordGroup.getPermissionUnit().ifPresentOrElse(checkUserIsAuthorizedForPemissionUnit(),
				this::permissionUnitMissingButExpected);
	}

	private void permissionUnitMissingButExpected() {
		throw new DataException("PermissionUnit is missing in the record.");
	}

	private void doNotUpdateIfExistsNewerVersionAndCheckOverrideProtection() {
		if (recordGroup.overwriteProtectionShouldBeEnforced()) {
			ifDifferentVersionThrowConflictException();
		}
		recordGroup.removeOverwriteProtection();
	}

	private void ifDifferentVersionThrowConflictException() {
		String latestUpdatedTopDataG = recordGroup.getLatestTsUpdated();
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

	private void replaceImmutableFieldsInRecordInfoFromPreviouslyStoredRecord() {
		recordGroup.setId(previouslyStoredRecord.getId());
		recordGroup.setType(previouslyStoredRecord.getType());
		recordGroup.setCreatedBy(previouslyStoredRecord.getCreatedBy());
		recordGroup.setTsCreated(previouslyStoredRecord.getTsCreated());
		recordGroup.setAllUpdated(Collections.emptyList());

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
		ensureValuesAreEqualThrowErrorIfNot(recordId, id);
		String type = recordGroup.getType();
		ensureValuesAreEqualThrowErrorIfNot(recordType, type);
	}

	private void ensureValuesAreEqualThrowErrorIfNot(String value,
			String extractedValueFromRecord) {
		if (differentValues(value, extractedValueFromRecord)) {
			throw new DataException("Value in data(" + extractedValueFromRecord
					+ ") does not match entered value(" + value + ")");
		}
	}

	private void possiblyHandleVisibility() {
		if (recordTypeHandler.useVisibility()) {
			possiblySetVisibilityToDefault();
			possiblyUpdateVisibilityTimeStamp();

		}
	}

	private void possiblySetVisibilityToDefault() {
		if (recordGroup.getVisibility().isEmpty()) {
			recordGroup.setVisibility(previouslyStoredRecord.getVisibility().orElse("unpublished"));
		}
	}

	private void possiblyUpdateVisibilityTimeStamp() {
		if (isVisibilityChanged()) {
			recordGroup.setTsVisibilityNow();
		}
	}

	private boolean isVisibilityChanged() {
		Optional<String> previousVisibility = previouslyStoredRecord.getVisibility();
		Optional<String> updatedVisibility = recordGroup.getVisibility();
		return !previousVisibility.equals(updatedVisibility);
	}

	private void possiblyUseTrashBin() {
		if (isInTrashBin()) {
			checkNoIncomingLinksExists();
		}
		if (recordTypeNotUsingTrashBinButRecordSetsRecordToTrashBin()) {
			throw new DataException(
					"To use the trash bin function, you must first activate it on the record type.");
		}
		if (ifUsingTrashBinButNoInTrashBinNotSet()) {
			throw new DataException(
					"The isInTrashBin field must be set when using the trash bin functionality.");
		}
		if (useTrashBinAndVisibilityRecorSetToInTrash()) {
			recordGroup.setVisibility("unpublished");
		}
	}

	private void checkNoIncomingLinksExists() {
		if (recordStorage.linksExistForRecord(recordType, recordId)) {
			throw new MisuseException("Record with type: " + recordType + " and id: " + recordId
					+ " could not be put in trash bin since other records are linking to it");
		}
	}

	private boolean useTrashBinAndVisibilityRecorSetToInTrash() {
		return usesTrashBinAndVisibility() && isInTrashBin();
	}

	private boolean isInTrashBin() {
		Optional<Boolean> inTrashBin = recordGroup.isInTrashBin();
		return inTrashBin.isPresent() && inTrashBin.get().booleanValue();
	}

	private boolean usesTrashBinAndVisibility() {
		return recordTypeHandler.useVisibility() && recordTypeHandler.useTrashBin();
	}

	private boolean ifUsingTrashBinButNoInTrashBinNotSet() {
		return recordTypeHandler.useTrashBin() && recordGroup.isInTrashBin().isEmpty();
	}

	private boolean recordTypeNotUsingTrashBinButRecordSetsRecordToTrashBin() {
		return !recordTypeHandler.useTrashBin() && recordGroup.isInTrashBin().isPresent();
	}

	private void updateRecordInStorage(DataGroup recordAsDataGroupForStorage,
			CollectTerms collectTerms, Set<Link> collectedLinks) {
		recordStorage.update(recordType, recordId, recordAsDataGroupForStorage,
				collectTerms.storageTerms, collectedLinks, dataDivider);
	}

	private void sendDataChanged() {
		DataChangedSender dataChangedSender = dependencyProvider.getDataChangeSender();
		dataChangedSender.sendDataChanged(recordType, recordId, UPDATE);
	}

	private void possiblyStoreInArchive(DataGroup recordAsDataGroupForStorage) {
		if (recordTypeHandler.storeInArchive()) {
			storeInArchive(recordAsDataGroupForStorage);
		}
	}

	private void storeInArchive(DataGroup recordAsDataGroupForStorage) {
		try {
			recordArchive.update(dataDivider, recordType, recordId, recordAsDataGroupForStorage);
		} catch (RecordNotFoundException _) {
			recordArchive.create(dataDivider, recordType, recordId, recordAsDataGroupForStorage);
		}
	}

	private void indexData(CollectTerms collectTerms) {
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