/*
 * Copyright 2015, 2016, 2017, 2022 Uppsala University Library
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

import java.text.MessageFormat;
import java.util.List;
import java.util.Set;

import se.uu.ub.cora.bookkeeper.linkcollector.DataRecordLinkCollector;
import se.uu.ub.cora.bookkeeper.recordpart.DataRedactor;
import se.uu.ub.cora.bookkeeper.recordtype.RecordTypeHandler;
import se.uu.ub.cora.bookkeeper.termcollector.DataGroupTermCollector;
import se.uu.ub.cora.bookkeeper.validator.DataValidator;
import se.uu.ub.cora.bookkeeper.validator.ValidationAnswer;
import se.uu.ub.cora.data.DataGroup;
import se.uu.ub.cora.data.DataProvider;
import se.uu.ub.cora.data.DataRecord;
import se.uu.ub.cora.data.DataRecordGroup;
import se.uu.ub.cora.data.DataRecordLink;
import se.uu.ub.cora.data.collected.CollectTerms;
import se.uu.ub.cora.data.collected.IndexTerm;
import se.uu.ub.cora.data.collected.Link;
import se.uu.ub.cora.data.collected.StorageTerm;
import se.uu.ub.cora.search.RecordIndexer;
import se.uu.ub.cora.spider.authentication.Authenticator;
import se.uu.ub.cora.spider.authorization.SpiderAuthorizator;
import se.uu.ub.cora.spider.dependency.SpiderDependencyProvider;
import se.uu.ub.cora.spider.extendedfunctionality.ExtendedFunctionality;
import se.uu.ub.cora.spider.extendedfunctionality.ExtendedFunctionalityProvider;
import se.uu.ub.cora.spider.record.ConflictException;
import se.uu.ub.cora.spider.record.DataException;
import se.uu.ub.cora.spider.record.DataGroupToRecordEnhancer;
import se.uu.ub.cora.spider.record.MisuseException;
import se.uu.ub.cora.spider.record.RecordCreator;
import se.uu.ub.cora.storage.archive.RecordArchive;
import se.uu.ub.cora.storage.idgenerator.RecordIdGenerator;

public final class RecordCreatorImp extends RecordHandler implements RecordCreator {
	private static final String TS_CREATED = "tsCreated";
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
	private String createDefinitionId;

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
			DataGroup dataGroup) {
		this.authToken = authToken;
		recordType = recordTypeToCreate;
		recordToValidate = dataGroup;
		DataRecordGroup dataGroupAsRecordGroup = DataProvider
				.createRecordGroupFromDataGroup(dataGroup);
		recordTypeHandler = dependencyProvider
				.getRecordTypeHandlerUsingDataRecordGroup(dataGroupAsRecordGroup);

		// TODO: reactivate after abstract types are removed
		validateRecordTypeInDataIsSameAsSpecified(recordTypeToCreate);

		createDefinitionId = recordTypeHandler.getCreateDefinitionId();
		definitionId = recordTypeHandler.getDefinitionId();

		return validateCreateAndStoreRecord();
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

	private DataRecord validateCreateAndStoreRecord() {
		checkActionAuthorizationForUser();
		checkNoCreateForAbstractRecordType();
		useExtendedFunctionalityBeforeMetadataValidation(recordType, recordToValidate);
		validateRecord();
		useExtendedFunctionalityAfterMetadataValidation(recordType, recordToValidate);
		completeRecordAndCollectInformationSpecifiedInMetadata();
		ensureNoDuplicateForTypeFamilyAndId();
		createRecordInStorage(recordToValidate, collectedLinks, collectedTerms.storageTerms);
		if (recordTypeHandler.storeInArchive()) {
			recordArchive.create(recordType, recordId, recordToValidate);
		}
		indexRecord(collectedTerms.indexTerms);
		useExtendedFunctionalityBeforeReturn(recordType, recordToValidate);
		return enhanceDataGroupToRecord();
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

	private void checkNoCreateForAbstractRecordType() {
		if (recordTypeHandler.isAbstract()) {
			throw new MisuseException(
					"Data creation on abstract recordType: " + recordType + " is not allowed");
		}
	}

	private void useExtendedFunctionalityBeforeMetadataValidation(String recordTypeToCreate,
			DataGroup dataGroup) {
		List<ExtendedFunctionality> functionalityForCreateBeforeMetadataValidation = extendedFunctionalityProvider
				.getFunctionalityForCreateBeforeMetadataValidation(recordTypeToCreate);
		useExtendedFunctionality(dataGroup, functionalityForCreateBeforeMetadataValidation);
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
				recordToValidate);
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
		DataRedactor dataRedactor = dependencyProvider.getDataRedactor();
		recordToValidate = dataRedactor.removeChildrenForConstraintsWithoutPermissions(
				definitionId, recordToValidate,
				recordTypeHandler.getCreateWriteRecordPartConstraints(), writePermissions);
	}

	private void validateDataInRecordAsSpecifiedInMetadata() {
		ValidationAnswer validationAnswer = dataValidator.validateData(createDefinitionId,
				recordToValidate);
		if (validationAnswer.dataIsInvalid()) {
			throw new DataException("Data is not valid: " + validationAnswer.getErrorMessages());
		}
	}

	private void useExtendedFunctionalityAfterMetadataValidation(String recordTypeToCreate,
			DataGroup dataGroup) {
		List<ExtendedFunctionality> functionalityForCreateAfterMetadataValidation = extendedFunctionalityProvider
				.getFunctionalityForCreateAfterMetadataValidation(recordTypeToCreate);
		useExtendedFunctionality(dataGroup, functionalityForCreateAfterMetadataValidation);
	}

	private void completeRecordAndCollectInformationSpecifiedInMetadata() {
		ensureCompleteRecordInfo(user.id, recordType);
		recordId = extractIdFromData();
		collectedTerms = dataGroupTermCollector.collectTerms(definitionId, recordToValidate);
		collectedLinks = linkCollector.collectLinks(definitionId, recordToValidate);
		checkToPartOfLinkedDataExistsInStorage(collectedLinks);
	}

	private void ensureCompleteRecordInfo(String userId, String recordType) {
		DataGroup recordInfo = recordToValidate.getFirstGroupWithNameInData(RECORD_INFO);
		ensureIdExists(recordType, recordInfo);
		addTypeToRecordInfo(recordType, recordInfo);
		addCreatedInfoToRecordInfoUsingUserId(recordInfo, userId);
		addUpdatedInfoToRecordInfoUsingUserId(recordInfo, userId);
	}

	private void ensureIdExists(String recordType, DataGroup recordInfo) {
		if (recordTypeHandler.shouldAutoGenerateId()) {
			removeIdIfPresentInData(recordInfo);
			generateAndAddIdToRecordInfo(recordType, recordInfo);
		}
	}

	private void removeIdIfPresentInData(DataGroup recordInfo) {
		if (recordInfo.containsChildWithNameInData("id")) {
			recordInfo.removeFirstChildWithNameInData("id");
		}
	}

	private void generateAndAddIdToRecordInfo(String recordType, DataGroup recordInfo) {
		recordInfo.addChild(DataProvider.createAtomicUsingNameInDataAndValue("id",
				idGenerator.getIdForType(recordType)));
	}

	private void addTypeToRecordInfo(String recordType, DataGroup recordInfo) {
		DataRecordLink typeLink = DataProvider.createRecordLinkUsingNameInDataAndTypeAndId("type",
				"recordType", recordType);
		recordInfo.addChild(typeLink);
	}

	private void addCreatedInfoToRecordInfoUsingUserId(DataGroup recordInfo, String userId) {
		DataRecordLink createdByLink = createLinkToUserUsingNameInDataAndUserId("createdBy",
				userId);
		recordInfo.addChild(createdByLink);
		String currentTimestamp = getCurrentTimestampAsString();
		recordInfo.addChild(
				DataProvider.createAtomicUsingNameInDataAndValue(TS_CREATED, currentTimestamp));
	}

	private void indexRecord(List<IndexTerm> indexTerms) {
		List<String> ids = recordTypeHandler.getCombinedIdsUsingRecordId(recordId);
		recordIndexer.indexData(ids, indexTerms, recordToValidate);
	}

	private void ensureNoDuplicateForTypeFamilyAndId() {
		if (isItADuplicateInStorage()) {
			String duplicateMessage = "Record with type: {0} and id: {1} already exists in storage";
			String message = MessageFormat.format(duplicateMessage, recordType, recordId);
			throw ConflictException.withMessage(message);
		}
	}

	private boolean isItADuplicateInStorage() {
		if (recordTypeHandler.hasParent()) {
			return duplicateExistsForAnyImplementingType();
		}
		return duplicateExistsForThisType();
	}

	private boolean duplicateExistsForAnyImplementingType() {
		String parentId = recordTypeHandler.getParentId();
		RecordTypeHandler parentRecordTypeHandler = dependencyProvider
				.getRecordTypeHandler(parentId);
		List<String> types = parentRecordTypeHandler.getListOfRecordTypeIdsToReadFromStorage();
		return recordStorage.recordExists(types, recordId);
	}

	private boolean duplicateExistsForThisType() {
		List<String> types = List.of(recordType);
		return recordStorage.recordExists(types, recordId);
	}

	private void createRecordInStorage(DataGroup topLevelDataGroup, Set<Link> collectedLinks2,
			Set<StorageTerm> storageTerms) {
		String dataDivider = extractDataDividerFromData(recordToValidate);
		recordStorage.create(recordType, recordId, topLevelDataGroup, storageTerms, collectedLinks2,
				dataDivider);
	}

	private String extractIdFromData() {
		return recordToValidate.getFirstGroupWithNameInData("recordInfo")
				.getFirstAtomicValueWithNameInData("id");
	}

	private void useExtendedFunctionalityBeforeReturn(String recordTypeToCreate,
			DataGroup dataGroup) {
		List<ExtendedFunctionality> extendedFunctionalityList = extendedFunctionalityProvider
				.getFunctionalityForCreateBeforeReturn(recordTypeToCreate);
		useExtendedFunctionality(dataGroup, extendedFunctionalityList);
	}

	private DataRecord enhanceDataGroupToRecord() {
		DataRedactor dataRedactor = dependencyProvider.getDataRedactor();
		return dataGroupToRecordEnhancer.enhanceIgnoringReadAccess(user, recordType,
				recordToValidate, dataRedactor);
	}

	public DataGroupToRecordEnhancer onlyForTestGetDataGroupToRecordEnhancer() {
		return dataGroupToRecordEnhancer;
	}
}
