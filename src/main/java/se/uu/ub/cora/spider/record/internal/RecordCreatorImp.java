/*
 * Copyright 2015, 2016, 2017, 2022, 2023, 2024, 2025 Uppsala University Library
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

import static se.uu.ub.cora.spider.extendedfunctionality.ExtendedFunctionalityPosition.CREATE_AFTER_AUTHORIZATION;
import static se.uu.ub.cora.spider.extendedfunctionality.ExtendedFunctionalityPosition.CREATE_AFTER_METADATA_VALIDATION;
import static se.uu.ub.cora.spider.extendedfunctionality.ExtendedFunctionalityPosition.CREATE_BEFORE_ENHANCE;
import static se.uu.ub.cora.spider.extendedfunctionality.ExtendedFunctionalityPosition.CREATE_BEFORE_STORE;

import java.text.MessageFormat;
import java.util.Collection;
import java.util.List;
import java.util.Set;

import se.uu.ub.cora.bookkeeper.linkcollector.DataRecordLinkCollector;
import se.uu.ub.cora.bookkeeper.metadata.Constraint;
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
import se.uu.ub.cora.data.collected.IndexTerm;
import se.uu.ub.cora.data.collected.Link;
import se.uu.ub.cora.data.collected.StorageTerm;
import se.uu.ub.cora.search.RecordIndexer;
import se.uu.ub.cora.spider.authentication.Authenticator;
import se.uu.ub.cora.spider.authorization.SpiderAuthorizator;
import se.uu.ub.cora.spider.cache.DataChangedSender;
import se.uu.ub.cora.spider.dependency.SpiderDependencyProvider;
import se.uu.ub.cora.spider.extendedfunctionality.ExtendedFunctionality;
import se.uu.ub.cora.spider.extendedfunctionality.ExtendedFunctionalityPosition;
import se.uu.ub.cora.spider.extendedfunctionality.ExtendedFunctionalityProvider;
import se.uu.ub.cora.spider.record.ConflictException;
import se.uu.ub.cora.spider.record.DataException;
import se.uu.ub.cora.spider.record.DataGroupToRecordEnhancer;
import se.uu.ub.cora.spider.record.RecordCreator;
import se.uu.ub.cora.spider.unique.UniqueValidator;
import se.uu.ub.cora.storage.archive.RecordArchive;
import se.uu.ub.cora.storage.idgenerator.RecordIdGenerator;

public final class RecordCreatorImp extends RecordHandler implements RecordCreator {
	private static final String UNPUBLISHED = "unpublished";
	private static final String CREATE = "create";
	private Authenticator authenticator;
	private SpiderAuthorizator spiderAuthorizator;
	private RecordIdGenerator idGenerator;
	private DataValidator dataValidator;
	private DataRecordLinkCollector linkCollector;
	private String definitionId;
	private ExtendedFunctionalityProvider extendedFunctionalityProvider;
	private RecordTypeHandler recordTypeHandler;
	private DataGroupToRecordEnhancer dataGroupToRecordEnhancer;
	private DataGroupTermCollector dataGroupTermCollector;
	private RecordIndexer recordIndexer;
	private Set<String> writePermissions;
	private CollectTerms collectedTerms;
	private Set<Link> collectedLinks;
	private RecordArchive recordArchive;
	private DataRecordGroup recordGroup;
	private Set<Constraint> writeRecordPartConstraints;

	private RecordCreatorImp(SpiderDependencyProvider dependencyProvider,
			DataGroupToRecordEnhancer dataGroupToRecordEnhancer) {
		this.dependencyProvider = dependencyProvider;
		this.dataGroupToRecordEnhancer = dataGroupToRecordEnhancer;
		authenticator = dependencyProvider.getAuthenticator();
		spiderAuthorizator = dependencyProvider.getSpiderAuthorizator();
		dataValidator = dependencyProvider.getDataValidator();
		recordStorage = dependencyProvider.getRecordStorage();
		idGenerator = dependencyProvider.getRecordIdGenerator();
		linkCollector = dependencyProvider.getDataRecordLinkCollector();
		dataGroupTermCollector = dependencyProvider.getDataGroupTermCollector();
		recordIndexer = dependencyProvider.getRecordIndexer();
		extendedFunctionalityProvider = dependencyProvider.getExtendedFunctionalityProvider();
		recordArchive = dependencyProvider.getRecordArchive();
	}

	public static RecordCreatorImp usingDependencyProviderAndDataGroupToRecordEnhancer(
			SpiderDependencyProvider dependencyProvider,
			DataGroupToRecordEnhancer dataGroupToRecordEnhancer) {
		return new RecordCreatorImp(dependencyProvider, dataGroupToRecordEnhancer);
	}

	@Override
	public DataRecord createAndStoreRecord(String authToken, String recordTypeToCreate,
			DataRecordGroup dataRecordGroup) {
		this.authToken = authToken;
		recordType = recordTypeToCreate;
		recordGroup = dataRecordGroup;

		try {
			return tryToValidateAndStoreRecord();
		} catch (DataValidationException exception) {
			throw new DataException("Data is not valid: " + exception.getMessage());
		}
	}

	private DataRecord tryToValidateAndStoreRecord() {
		checkActionAuthorizationForUser();
		useExtendedFunctionalityForPosition(CREATE_AFTER_AUTHORIZATION);
		recordTypeHandler = createRecordTypeHandler();
		validateRecordTypeInDataIsSameAsSpecified(recordType);
		definitionId = recordTypeHandler.getDefinitionId();
		validateRecord();
		useExtendedFunctionalityForPosition(CREATE_AFTER_METADATA_VALIDATION);
		ensureCompleteRecordInfo(user.id, recordType);
		recordId = recordGroup.getId();
		collectInformationSpecifiedInMetadata();
		ensureNoDuplicateForTypeAndId();
		validateDataForUniqueThrowErrorIfNot();
		useExtendedFunctionalityForPosition(CREATE_BEFORE_STORE);
		DataGroup recordAsDataGroup = DataProvider.createGroupFromRecordGroup(recordGroup);
		createRecordInStorage(recordAsDataGroup, collectedLinks, collectedTerms.storageTerms);
		sendDataChanged();
		possiblyStoreInArchive(recordAsDataGroup);
		indexRecord(collectedTerms.indexTerms);
		useExtendedFunctionalityForPosition(CREATE_BEFORE_ENHANCE);
		return enhanceDataGroupToRecord();
	}

	private void validateRecordTypeInDataIsSameAsSpecified(String recordTypeToCreate) {
		if (recordTypeDoesNotMatchRecordTypeFromValidationType(recordTypeToCreate)) {
			throw new DataException("The record "
					+ "cannot be created because the record type provided does not match the record type "
					+ "that the validation type is set to validate.");
		}
	}

	private boolean recordTypeDoesNotMatchRecordTypeFromValidationType(String recordTypeToCreate) {
		return !recordTypeHandler.getRecordTypeId().equals(recordTypeToCreate);
	}

	private void checkActionAuthorizationForUser() {
		tryToGetActiveUser();
		checkUserIsAuthorizedForActionOnRecordType();
	}

	private void tryToGetActiveUser() {
		user = authenticator.getUserForToken(authToken);
	}

	private void checkUserIsAuthorizedForActionOnRecordType() {
		spiderAuthorizator.checkUserIsAuthorizedForActionOnRecordType(user, CREATE, recordType);
	}

	private void useExtendedFunctionalityForPosition(ExtendedFunctionalityPosition position) {
		List<ExtendedFunctionality> exFunctionality = extendedFunctionalityProvider
				.getFunctionalityForPositionAndRecordType(position, recordType);
		useExtendedFunctionality(recordGroup, exFunctionality);
	}

	private RecordTypeHandler createRecordTypeHandler() {
		return dependencyProvider.getRecordTypeHandlerUsingDataRecordGroup(recordGroup);
	}

	private void validateRecord() {
		checkRecordPartsUserIsNotAllowtoChange();
		validateDataInRecordAsSpecifiedInMetadata();
	}

	private void checkRecordPartsUserIsNotAllowtoChange() {
		checkUserIsAuthorisedToCreateIncomingData(recordType);
		possiblyRemoveRecordPartsUserIsNotAllowedToChange();
	}

	private void checkUserIsAuthorisedToCreateIncomingData(String recordType) {
		CollectTerms uncheckedCollectedTerms = dataGroupTermCollector.collectTerms(definitionId,
				recordGroup);
		writePermissions = spiderAuthorizator
				.checkGetUsersMatchedRecordPartPermissionsForActionOnRecordTypeAndCollectedData(
						user, CREATE, recordType, uncheckedCollectedTerms.permissionTerms, true);
	}

	private void possiblyRemoveRecordPartsUserIsNotAllowedToChange() {
		if (recordTypeHandler.hasRecordPartCreateConstraint()) {
			removeRecordPartsUserIsNotAllowedToChange();
		}
	}

	private void removeRecordPartsUserIsNotAllowedToChange() {
		writeRecordPartConstraints = recordTypeHandler.getCreateWriteRecordPartConstraints();
		DataRedactor dataRedactor = dependencyProvider.getDataRedactor();
		recordGroup = dataRedactor.removeChildrenForConstraintsWithoutPermissions(definitionId,
				recordGroup, writeRecordPartConstraints, writePermissions);
	}

	private void validateDataInRecordAsSpecifiedInMetadata() {
		String createDefinitionId = recordTypeHandler.getCreateDefinitionId();
		DataGroup dataGroup = DataProvider.createGroupFromRecordGroup(recordGroup);
		ValidationAnswer validationAnswer = dataValidator.validateData(createDefinitionId,
				dataGroup);
		if (validationAnswer.dataIsInvalid()) {
			throw new DataException("Data is not valid: " + validationAnswer.getErrorMessages());
		}
	}

	private void ensureCompleteRecordInfo(String userId, String recordType) {
		recordGroup.getFirstGroupWithNameInData(RECORD_INFO);
		ensureIdExists(recordType);
		recordGroup.setType(recordType);
		recordGroup.setCreatedBy(userId);
		recordGroup.setTsCreatedToNow();
		recordGroup.addUpdatedUsingUserIdAndTs(userId, recordGroup.getTsCreated());
		handleVisibility();
	}

	private void handleVisibility() {
		if (recordTypeHandler.useVisibility()) {
			ensureCorrectVisibilityValue();
			createVisibilityTimeStamp();
		}
	}

	private void ensureCorrectVisibilityValue() {
		if (noWritePermissionForVisibility()) {
			recordGroup.setVisibility(UNPUBLISHED);
		}
	}

	private boolean noWritePermissionForVisibility() {
		return writeRecordPartConstraints.stream().map(Constraint::getNameInData)
				.noneMatch(writePermissions::contains);
	}

	private void ensureIdExists(String recordType) {
		if (recordTypeHandler.shouldAutoGenerateId()) {
			generateAndAddIdToRecordInfo(recordType);
		}
	}

	private void generateAndAddIdToRecordInfo(String recordType) {
		recordGroup.setId(idGenerator.getIdForType(recordType));
	}

	private void collectInformationSpecifiedInMetadata() {
		collectedTerms = dataGroupTermCollector.collectTerms(definitionId, recordGroup);
		DataGroup dataGroup = DataProvider.createGroupFromRecordGroup(recordGroup);
		collectedLinks = linkCollector.collectLinks(definitionId, dataGroup);
		checkToPartOfLinkedDataExistsInStorage(collectedLinks);
	}

	private void indexRecord(List<IndexTerm> indexTerms) {
		recordIndexer.indexData(recordType, recordId, indexTerms, recordGroup);
	}

	private void ensureNoDuplicateForTypeAndId() {
		if (isItADuplicateInStorage()) {
			String duplicateMessage = "Record with type: {0} and id: {1} already exists in storage";
			String message = MessageFormat.format(duplicateMessage, recordType, recordId);
			throw ConflictException.withMessage(message);
		}
	}

	private boolean isItADuplicateInStorage() {
		List<String> types = List.of(recordType);
		return recordStorage.recordExists(types, recordId);
	}

	private void validateDataForUniqueThrowErrorIfNot() {
		UniqueValidator uniqueValidator = dependencyProvider.getUniqueValidator(recordStorage);
		ValidationAnswer uniqueAnswer = uniqueValidator.validateUniqueForNewRecord(recordType,
				recordTypeHandler.getUniqueDefinitions(), collectedTerms.storageTerms);
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

	private void createRecordInStorage(DataGroup recordAsDataGroup, Set<Link> collectedLinks,
			Set<StorageTerm> storageTerms) {
		recordStorage.create(recordType, recordId, recordAsDataGroup, storageTerms, collectedLinks,
				recordGroup.getDataDivider());
	}

	private void sendDataChanged() {
		DataChangedSender dataChangedSender = dependencyProvider.getDataChangeSender();
		dataChangedSender.sendDataChanged(recordType, recordId, CREATE);
	}

	private void possiblyStoreInArchive(DataGroup recordAsDataGroup) {
		if (recordTypeHandler.storeInArchive()) {
			recordArchive.create(recordGroup.getDataDivider(), recordType, recordId,
					recordAsDataGroup);
		}
	}

	private void createVisibilityTimeStamp() {
		recordGroup.setTsVisibility(recordGroup.getTsCreated());
	}

	private DataRecord enhanceDataGroupToRecord() {
		DataRedactor dataRedactor = dependencyProvider.getDataRedactor();
		return dataGroupToRecordEnhancer.enhanceIgnoringReadAccess(user, recordType, recordGroup,
				dataRedactor);
	}

	public DataGroupToRecordEnhancer onlyForTestGetDataGroupToRecordEnhancer() {
		return dataGroupToRecordEnhancer;
	}
}
