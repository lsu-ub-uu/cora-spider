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

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import se.uu.ub.cora.beefeater.authentication.User;
import se.uu.ub.cora.bookkeeper.linkcollector.DataRecordLinkCollector;
import se.uu.ub.cora.bookkeeper.recordtype.RecordTypeHandler;
import se.uu.ub.cora.bookkeeper.termcollector.DataGroupTermCollector;
import se.uu.ub.cora.bookkeeper.validator.DataValidator;
import se.uu.ub.cora.bookkeeper.validator.ValidationAnswer;
import se.uu.ub.cora.data.Action;
import se.uu.ub.cora.data.DataAtomic;
import se.uu.ub.cora.data.DataGroup;
import se.uu.ub.cora.data.DataProvider;
import se.uu.ub.cora.data.DataRecord;
import se.uu.ub.cora.data.DataRecordGroup;
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
	private SpiderAuthorizator authorizator;
	private DataValidator dataValidator;
	private DataRecordLinkCollector linkCollector;
	private String newOrExisting;
	private RecordIdGenerator idGenerator;
	private RecordTypeHandler recordTypeHandler;
	private ExtendedFunctionalityProvider extendedFunctionalityProvider;
	private String recordTypeToValidate;
	private List<String> errorList = new ArrayList<>();
	private DataRecordGroup validationResult;
	private DataRecordGroup recordToValidate;

	private RecordValidatorImp(SpiderDependencyProvider dependencyProvider) {
		this.dependencyProvider = dependencyProvider;
		this.authenticator = dependencyProvider.getAuthenticator();
		this.authorizator = dependencyProvider.getSpiderAuthorizator();
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
			DataGroup validationOrder, DataRecordGroup recordToValidate) {
		this.authToken = authToken;
		this.recordToValidate = recordToValidate;
		this.recordType = validationOrderType;

		user = tryToGetActiveUser();

		checkUserIsAuthorizedForCreateOnValidationOrder();
		validateValidationOrderThrowErrorIfInvalid(validationOrder);

		recordTypeToValidate = getRecordTypeToValidate(validationOrder);
		useExtendedFunctionalityForPosition(
				ExtendedFunctionalityPosition.VALIDATE_AFTER_AUTHORIZATION);

		// createRecordTypeHandlerForRecordToValidate(recordToValidate);
		recordTypeHandler = dependencyProvider
				.getRecordTypeHandlerUsingDataRecordGroup(recordToValidate);

		checkUserIsAuthorizedForValidateOnRecordType(recordTypeToValidate);

		validateRecordUsingValidationRecord(validationOrder, recordTypeToValidate);

		// here!
		DataGroupTermCollector termCollector = dependencyProvider.getDataGroupTermCollector();
		CollectTerms collectedTerms = termCollector
				.collectTerms(recordTypeHandler.getDefinitionId(), recordToValidate);

		UniqueValidator validateUniques = dependencyProvider.getUniqueValidator(recordStorage);
		validateUniques.validateUniqueForNewRecord(recordTypeToValidate,
				recordTypeHandler.getUniqueDefinitions(), collectedTerms.storageTerms);

		return createAnswerDataRecord();
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
		authorizator.checkUserIsAuthorizedForActionOnRecordType(user, "create", recordType);
	}

	private void validateValidationOrderThrowErrorIfInvalid(DataGroup validationOrder) {
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

	// private void createRecordTypeHandlerForRecordToValidate(DataGroup recordToValidate) {
	// DataRecordGroup recordToValidateAsDataRecordGroup = DataProvider
	// .createRecordGroupFromDataGroup(recordToValidate);
	// recordTypeHandler = dependencyProvider
	// .getRecordTypeHandlerUsingDataRecordGroup(recordToValidateAsDataRecordGroup);
	// }

	private void useExtendedFunctionalityForPosition(ExtendedFunctionalityPosition position) {
		// read from validationorder
		List<ExtendedFunctionality> exFunctionality = extendedFunctionalityProvider
				.getFunctionalityForPositionAndRecordType(position, recordTypeToValidate);
		useExtendedFunctionality(recordToValidate, exFunctionality);
	}

	@Override
	protected ExtendedFunctionalityData createExtendedFunctionalityData(
			DataRecordGroup dataRecordGroup) {
		ExtendedFunctionalityData data = new ExtendedFunctionalityData();
		data.recordType = recordTypeToValidate;
		data.recordId = recordId;
		data.authToken = authToken;
		data.user = user;
		// data.dataGroup = dataRecordGroup;
		data.dataRecordGroup = dataRecordGroup;
		return data;
	}

	private void checkUserIsAuthorizedForValidateOnRecordType(String recordTypeToValidate) {
		authorizator.checkUserIsAuthorizedForActionOnRecordType(user, VALIDATE,
				recordTypeToValidate);
	}

	private void validateRecordUsingValidationRecord(DataGroup validationOrder,
			String recordTypeToValidate) {
		newOrExisting = validationOrder.getFirstAtomicValueWithNameInData("metadataToValidate");

		validateRecordExistInStorageWhenActionToPerformIsUpdate(recordTypeToValidate);
		possiblyEnsureLinksExist(validationOrder);
		validateRecordTypesMatchBetweenValidationOrderAndRecord();
		validateIncomingDataAsSpecifiedInMetadata();
	}

	private void validateRecordTypesMatchBetweenValidationOrderAndRecord() {
		String recordTypeOfRecord = recordTypeHandler.getRecordTypeId();
		if (!recordTypeOfRecord.equals(recordTypeToValidate)) {

			String message = "RecordType from record (" + recordTypeOfRecord
					+ ") does not match recordType from validationOrder (" + recordTypeToValidate
					+ ")";
			errorList.add(message);
		}
	}

	private void validateRecordExistInStorageWhenActionToPerformIsUpdate(
			String recordTypeToValidate) {
		if (validationIsForUpdate()) {
			String recordId = extractIdFromData();
			checkIfRecordExist(recordTypeToValidate, recordId);
		}
	}

	private boolean validationIsForUpdate() {
		return "existing".equals(newOrExisting);
	}

	private String extractIdFromData() {
		return recordToValidate.getFirstGroupWithNameInData("recordInfo")
				.getFirstAtomicValueWithNameInData("id");
	}

	private void checkIfRecordExist(String recordType, String recordId) {
		try {
			recordStorage.read(List.of(recordType), recordId);
		} catch (RecordNotFoundException exception) {
			errorList.add(exception.getMessage());
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
		DataGroup recordToValidateAsDataGroup = DataProvider
				.createGroupFromRecordGroup(recordToValidate);
		Set<Link> collectedLinks = linkCollector.collectLinks(definitionId,
				recordToValidateAsDataGroup);
		checkIfLinksExist(collectedLinks);
	}

	private void checkIfLinksExist(Set<Link> collectedLinks) {
		try {
			checkToPartOfLinkedDataExistsInStorage(collectedLinks);
		} catch (DataException exception) {
			errorList.add(exception.getMessage());
		}
	}

	private void validateIncomingDataAsSpecifiedInMetadata() {
		String metadataId = getDefinitionId();
		DataGroup recordToValidateAsDataGroup = DataProvider
				.createGroupFromRecordGroup(recordToValidate);
		ValidationAnswer validationAnswer = dataValidator.validateData(metadataId,
				recordToValidateAsDataGroup);

		possiblyAddErrorMessages(validationAnswer);
	}

	private String getDefinitionId() {
		if (validationIsForUpdate()) {
			return recordTypeHandler.getUpdateDefinitionId();
		}
		return recordTypeHandler.getCreateDefinitionId();
	}

	private void possiblyAddErrorMessages(ValidationAnswer validationAnswer) {
		if (validationAnswer.dataIsInvalid()) {
			addErrorMessages(validationAnswer);
		}
	}

	private void addErrorMessages(ValidationAnswer validationAnswer) {
		for (String errorMessage : validationAnswer.getErrorMessages()) {
			errorList.add(errorMessage);
		}
	}

	private DataRecord createAnswerDataRecord() {
		DataRecordGroup validationRecordGroup = createValidationRecordGroup();
		addErrorToValidationResult();
		setValidStatus();
		DataRecord dataRecord = DataProvider.createRecordWithDataRecordGroup(validationRecordGroup);
		addReadActionToComplyWithRecordStructure(dataRecord);
		return dataRecord;
	}

	private DataRecordGroup createValidationRecordGroup() {
		validationResult = DataProvider.createRecordGroupUsingNameInData("validationResult");
		validationResult.setId(idGenerator.getIdForType(recordType));
		validationResult.setType(recordType);
		validationResult.setCreatedBy(user.id);
		validationResult.setTsCreatedToNow();
		validationResult.addUpdatedUsingUserIdAndTs(user.id, validationResult.getTsCreated());
		return validationResult;
	}

	private void addErrorToValidationResult() {
		for (String message : errorList) {
			DataGroup errorMessages = getErrorMessagesGroup();
			int repeatId = calculateRepeatId(errorMessages);
			DataAtomic error = createErrorWithMessageAndRepeatId(message, repeatId);
			errorMessages.addChild(error);
		}
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

	private void setValidStatus() {
		if (validationResult.containsChildWithNameInData(ERROR_MESSAGES)) {
			validationResult
					.addChild(DataProvider.createAtomicUsingNameInDataAndValue("valid", "false"));
		} else {
			validationResult
					.addChild(DataProvider.createAtomicUsingNameInDataAndValue("valid", "true"));
		}
	}

	private void addReadActionToComplyWithRecordStructure(DataRecord dataRecord) {
		dataRecord.addAction(Action.READ);
	}
}
