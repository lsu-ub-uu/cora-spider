/*
 * Copyright 2019, 2024 Uppsala University Library
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
import java.util.Optional;
import java.util.Set;

import se.uu.ub.cora.beefeater.authentication.User;
import se.uu.ub.cora.bookkeeper.linkcollector.DataRecordLinkCollector;
import se.uu.ub.cora.bookkeeper.recordtype.RecordTypeHandler;
import se.uu.ub.cora.bookkeeper.termcollector.DataGroupTermCollector;
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
import se.uu.ub.cora.spider.authentication.Authenticator;
import se.uu.ub.cora.spider.authorization.SpiderAuthorizator;
import se.uu.ub.cora.spider.dependency.SpiderDependencyProvider;
import se.uu.ub.cora.spider.extendedfunctionality.ExtendedFunctionality;
import se.uu.ub.cora.spider.extendedfunctionality.ExtendedFunctionalityData;
import se.uu.ub.cora.spider.extendedfunctionality.ExtendedFunctionalityPosition;
import se.uu.ub.cora.spider.extendedfunctionality.ExtendedFunctionalityProvider;
import se.uu.ub.cora.spider.record.DataException;
import se.uu.ub.cora.spider.record.RecordValidator;
import se.uu.ub.cora.spider.unique.UniqueValidator;
import se.uu.ub.cora.storage.RecordNotFoundException;
import se.uu.ub.cora.storage.idgenerator.RecordIdGenerator;

public final class RecordValidatorImp extends RecordHandler implements RecordValidator {
	private static final String ERROR_MESSAGES = "errorMessages";
	private static final String VALIDATE = "validate";
	private Authenticator authenticator;
	private SpiderAuthorizator spiderAuthorizator;
	private DataValidator dataValidator;
	private DataRecordLinkCollector linkCollector;
	private String metadataToValidate;
	private DataGroup validationResult;
	private RecordIdGenerator idGenerator;
	private RecordTypeHandler recordTypeHandler;
	private ExtendedFunctionalityProvider extendedFunctionalityProvider;
	private String recordTypeToValidate;

	private RecordValidatorImp(SpiderDependencyProvider dependencyProvider) {
		this.dependencyProvider = dependencyProvider;
		this.authenticator = dependencyProvider.getAuthenticator();
		this.spiderAuthorizator = dependencyProvider.getSpiderAuthorizator();
		this.dataValidator = dependencyProvider.getDataValidator();
		this.recordStorage = dependencyProvider.getRecordStorage();
		this.linkCollector = dependencyProvider.getDataRecordLinkCollector();
		this.idGenerator = dependencyProvider.getRecordIdGenerator();
		this.extendedFunctionalityProvider = dependencyProvider.getExtendedFunctionalityProvider();
	}

	public static RecordValidator usingDependencyProvider(
			SpiderDependencyProvider dependencyProvider) {
		return new RecordValidatorImp(dependencyProvider);
	}

	@Override
	public DataRecord validateRecord(String authToken, String validationOrderType,
			DataGroup validationOrder, DataGroup recordToValidate) {
		this.authToken = authToken;
		this.recordToValidate = recordToValidate;
		this.recordType = validationOrderType;

		user = tryToGetActiveUser();
		checkUserIsAuthorizedForCreateOnValidationOrder();
		validateValidationOrder(validationOrder);
		recordTypeToValidate = getRecordTypeToValidate(validationOrder);
		useExtendedFunctionalityForPosition(
				ExtendedFunctionalityPosition.VALIDATE_AFTER_AUTHORIZATION);

		createRecordTypeHandlerForRecordToValidate(recordToValidate);

		DataGroupTermCollector termCollector = dependencyProvider.getDataGroupTermCollector();
		// CollectTerms collectedTerms = termCollector
		// .collectTerms(recordTypeHandler.getDefinitionId(), recordToValidate);
		CollectTerms collectedTerms = termCollector.collectTerms(null, null);

		UniqueValidator validateUniques = dependencyProvider.getUniqueValidator(recordStorage);
		validateUniques.validateUniqueForNewRecord(recordTypeToValidate,
				recordTypeHandler.getUniqueDefinitions(), collectedTerms.storageTerms);

		return validateRecordAndCreateValidationResult(validationOrder);
	}

	// private void validateDataForUniqueThrowErrorIfNot() {
	// UniqueValidator uniqueValidator = dependencyProvider.getUniqueValidator(recordStorage);
	// ValidationAnswer uniqueAnswer = uniqueValidator.validateUnique(recordType, recordId,
	// recordTypeHandler.getUniqueDefinitions(), collectedTerms.storageTerms);
	// if (uniqueAnswer.dataIsInvalid()) {
	// add to
	// }
	// }

	// private void createAndThrowConflictExceptionForUnique(ValidationAnswer uniqueAnswer) {
	// String errorMessageTemplate = "The record could not be created as it fails unique validation
	// with the "
	// + "following {0} error messages: {1}";
	// Collection<String> errorMessages = uniqueAnswer.getErrorMessages();
	// String errorMessage = MessageFormat.format(errorMessageTemplate, errorMessages.size(),
	// errorMessages);
	// throw ConflictException.withMessage(errorMessage);
	// }

	private User tryToGetActiveUser() {
		return authenticator.getUserForToken(authToken);
	}

	private void checkUserIsAuthorizedForCreateOnValidationOrder() {
		spiderAuthorizator.checkUserIsAuthorizedForActionOnRecordType(user, "create", recordType);
	}

	private void validateValidationOrder(DataGroup validationOrder) {
		String metadataIdForWorkOrder = getMetadataIdForWorkOrder(validationOrder);
		ValidationAnswer validationAnswer = dataValidator.validateData(metadataIdForWorkOrder,
				validationOrder);
		if (validationAnswer.dataIsInvalid()) {
			throw new DataException("Data is not valid: " + validationAnswer.getErrorMessages());
		}
	}

	private String getMetadataIdForWorkOrder(DataGroup validationOrder) {
		DataRecordGroup validationOrderAsDataRecordGroup = DataProvider
				.createRecordGroupFromDataGroup(validationOrder);
		RecordTypeHandler validationOrderRecordTypeHandler = dependencyProvider
				.getRecordTypeHandlerUsingDataRecordGroup(validationOrderAsDataRecordGroup);

		return validationOrderRecordTypeHandler.getCreateDefinitionId();
	}

	private String getRecordTypeToValidate(DataGroup validationOrder) {
		DataGroup recordTypeGroup = validationOrder.getFirstGroupWithNameInData("recordType");
		return recordTypeGroup.getFirstAtomicValueWithNameInData(LINKED_RECORD_ID);
	}

	private void createRecordTypeHandlerForRecordToValidate(DataGroup recordToValidate) {
		DataRecordGroup recordToValidateAsDataRecordGroup = DataProvider
				.createRecordGroupFromDataGroup(recordToValidate);
		recordTypeHandler = dependencyProvider
				.getRecordTypeHandlerUsingDataRecordGroup(recordToValidateAsDataRecordGroup);
	}

	private void useExtendedFunctionalityForPosition(ExtendedFunctionalityPosition position) {
		// read from validationorder
		List<ExtendedFunctionality> exFunctionality = extendedFunctionalityProvider
				.getFunctionalityForPositionAndRecordType(position, recordTypeToValidate);
		useExtendedFunctionality(recordToValidate, exFunctionality);
	}

	@Override
	protected ExtendedFunctionalityData createExtendedFunctionalityData(DataGroup dataGroup) {
		ExtendedFunctionalityData data = new ExtendedFunctionalityData();
		data.recordType = recordTypeToValidate;
		data.recordId = recordId;
		data.authToken = authToken;
		data.user = user;
		data.dataGroup = dataGroup;
		return data;
	}

	private DataRecord validateRecordAndCreateValidationResult(DataGroup validationOrder) {
		checkUserIsAuthorizedForValidateOnRecordType(recordTypeToValidate);

		createValidationResultDataGroup();
		validateRecordUsingValidationRecord(validationOrder, recordTypeToValidate);

		DataRecord dataRecord = DataProvider.createRecordWithDataGroup(validationResult);
		addReadActionToComplyWithRecordStructure(dataRecord);
		return dataRecord;
	}

	private void checkUserIsAuthorizedForValidateOnRecordType(String recordTypeToValidate) {
		spiderAuthorizator.checkUserIsAuthorizedForActionOnRecordType(user, VALIDATE,
				recordTypeToValidate);
	}

	private void createValidationResultDataGroup() {
		validationResult = DataProvider.createGroupUsingNameInData("validationResult");
		DataGroup recordInfo = DataProvider.createGroupUsingNameInData("recordInfo");
		recordInfo.addChild(DataProvider.createAtomicUsingNameInDataAndValue("id",
				idGenerator.getIdForType(recordType)));

		DataRecordLink typeLink = DataProvider.createRecordLinkUsingNameInDataAndTypeAndId("type",
				"recordType", recordType);
		recordInfo.addChild(typeLink);

		addCreatedInfoToRecordInfoUsingUserId(recordInfo, user.id);
		addUpdatedInfoToRecordInfoUsingUserId(recordInfo, user.id);
		validationResult.addChild(recordInfo);
	}

	private void addCreatedInfoToRecordInfoUsingUserId(DataGroup recordInfo, String userId) {
		DataRecordLink createdByLink = DataProvider
				.createRecordLinkUsingNameInDataAndTypeAndId("createdBy", "user", userId);
		recordInfo.addChild(createdByLink);
		String currentLocalDateTime = getCurrentTimestampAsString();
		recordInfo.addChild(
				DataProvider.createAtomicUsingNameInDataAndValue(TS_CREATED, currentLocalDateTime));
	}

	private void validateRecordUsingValidationRecord(DataGroup validationOrder,
			String recordTypeToValidate) {
		metadataToValidate = validationOrder
				.getFirstAtomicValueWithNameInData("metadataToValidate");

		Optional<String> recordId = extractRecordIdIfUpdate();
		ensureRecordExistWhenActionToPerformIsUpdate(recordTypeToValidate, recordId);

		String metadataId = getMetadataId();
		possiblyEnsureLinksExist(validationOrder);

		ensureRecordTypesMatch();

		validateIncomingDataAsSpecifiedInMetadata(metadataId);
	}

	private void ensureRecordTypesMatch() {
		String recordTypeOfRecord = recordTypeHandler.getRecordTypeId();
		if (!recordTypeOfRecord.equals(recordTypeToValidate)) {
			addErrorToValidationResult("RecordType from record (" + recordTypeOfRecord
					+ ") does not match recordType from validationOrder (" + recordTypeToValidate
					+ ")");
		}
	}

	private String getMetadataId() {
		return validateNew() ? recordTypeHandler.getCreateDefinitionId()
				: recordTypeHandler.getUpdateDefinitionId();
	}

	private boolean validateNew() {
		return "new".equals(metadataToValidate);
	}

	private Optional<String> extractRecordIdIfUpdate() {
		return "existing".equals(metadataToValidate) ? Optional.of(extractIdFromData())
				: Optional.empty();
	}

	private String extractIdFromData() {
		return recordToValidate.getFirstGroupWithNameInData("recordInfo")
				.getFirstAtomicValueWithNameInData("id");
	}

	private void ensureRecordExistWhenActionToPerformIsUpdate(String recordTypeToValidate,
			Optional<String> recordId) {
		if (recordId.isPresent()) {
			checkIfRecordExist(recordTypeToValidate, recordId.get());
		}
	}

	private void checkIfRecordExist(String recordTypeToValidate, String recordIdToUse) {
		try {
			recordStorage.read(List.of(recordTypeToValidate), recordIdToUse);
		} catch (RecordNotFoundException exception) {
			addErrorToValidationResult(exception.getMessage());
		}
	}

	private void addErrorToValidationResult(String message) {
		DataGroup errorMessages = getErrorMessagesGroup();
		int repeatId = calculateRepeatId(errorMessages);
		DataAtomic error = createErrorWithMessageAndRepeatId(message, repeatId);
		errorMessages.addChild(error);
	}

	private DataGroup getErrorMessagesGroup() {
		ensureErrorMessagesGroupExist();
		return validationResult.getFirstGroupWithNameInData(ERROR_MESSAGES);
	}

	private void ensureErrorMessagesGroupExist() {
		if (!validationResult.containsChildWithNameInData(ERROR_MESSAGES)) {
			validationResult.addChild(DataProvider.createGroupUsingNameInData(ERROR_MESSAGES));
		}
	}

	private int calculateRepeatId(DataGroup errorMessages) {
		return !errorMessages.hasChildren() ? 0 : errorMessages.getChildren().size();
	}

	private DataAtomic createErrorWithMessageAndRepeatId(String message, int repeatId) {
		DataAtomic error = DataProvider.createAtomicUsingNameInDataAndValue("errorMessage",
				message);
		error.setRepeatId(String.valueOf(repeatId));
		return error;
	}

	private void possiblyEnsureLinksExist(DataGroup validationOrder) {
		String validateLinks = validationOrder.getFirstAtomicValueWithNameInData("validateLinks");
		if ("true".equals(validateLinks)) {
			ensureLinksExist();
		}
	}

	private void ensureLinksExist() {
		String definitionId = recordTypeHandler.getDefinitionId();
		Set<Link> collectedLinks = linkCollector.collectLinks(definitionId, recordToValidate);
		checkIfLinksExist(collectedLinks);
	}

	private void checkIfLinksExist(Set<Link> collectedLinks) {
		try {
			checkToPartOfLinkedDataExistsInStorage(collectedLinks);
		} catch (DataException exception) {
			addErrorToValidationResult(exception.getMessage());
		}
	}

	private void validateIncomingDataAsSpecifiedInMetadata(String metadataId) {
		System.err.println("here" + metadataId);
		ValidationAnswer validationAnswer = dataValidator.validateData(metadataId,
				recordToValidate);
		System.err.println("here2" + validationAnswer);
		possiblyAddErrorMessages(validationAnswer);
		if (validationResult.containsChildWithNameInData(ERROR_MESSAGES)) {
			validationResult
					.addChild(DataProvider.createAtomicUsingNameInDataAndValue("valid", "false"));
		} else {
			validationResult
					.addChild(DataProvider.createAtomicUsingNameInDataAndValue("valid", "true"));
		}
	}

	private void possiblyAddErrorMessages(ValidationAnswer validationAnswer) {
		if (validationAnswer.dataIsInvalid()) {
			addErrorMessages(validationAnswer);
		}
	}

	private void addErrorMessages(ValidationAnswer validationAnswer) {
		for (String errorMessage : validationAnswer.getErrorMessages()) {
			System.err.println("here3" + errorMessage);
			addErrorToValidationResult(errorMessage);
		}
	}

	private void addReadActionToComplyWithRecordStructure(DataRecord dataRecord) {
		dataRecord.addAction(se.uu.ub.cora.data.Action.READ);
	}
}
