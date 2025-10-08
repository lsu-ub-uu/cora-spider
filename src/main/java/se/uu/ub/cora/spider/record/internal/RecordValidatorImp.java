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

public final class RecordValidatorImp extends RecordHandler implements RecordValidator {
	private static final String ERROR_MESSAGES = "errorMessages";
	private static final String VALIDATE = "validate";
	private Authenticator authenticator;
	private SpiderAuthorizator authorizator;
	private DataValidator dataValidator;
	private DataRecordLinkCollector linkCollector;
	private RecordTypeHandler recordTypeHandlerForDataToValidate;
	private ExtendedFunctionalityProvider extendedFunctionalityProvider;
	private String specifiedRecordTypeToValidate;
	private List<String> errorList = new ArrayList<>();
	private DataRecordGroup validationResult;
	private DataRecordGroup recordToValidate;
	private DataGroup validationOrder;

	private RecordValidatorImp(SpiderDependencyProvider dependencyProvider) {
		this.dependencyProvider = dependencyProvider;
		this.authenticator = dependencyProvider.getAuthenticator();
		this.authorizator = dependencyProvider.getSpiderAuthorizator();
		this.dataValidator = dependencyProvider.getDataValidator();
		this.recordStorage = dependencyProvider.getRecordStorage();
		this.linkCollector = dependencyProvider.getDataRecordLinkCollector();
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
		this.validationOrder = validationOrder;
		this.recordToValidate = recordToValidate;
		this.recordType = validationOrderType;

		try {
			return tryToValidateRecord(validationOrder, recordToValidate);
		} catch (se.uu.ub.cora.bookkeeper.metadata.DataMissingException e) {
			return createAnswerForDataMissing(e);
		}
	}

	private DataRecord tryToValidateRecord(DataGroup validationOrder,
			DataRecordGroup recordToValidate) {
		user = tryToGetActiveUser();
		checkUserIsAuthorizedForCreateOnValidationOrder();
		validateValidationOrderThrowErrorIfInvalid(validationOrder);

		specifiedRecordTypeToValidate = getRecordTypeIdToValidate(validationOrder);
		checkUserIsAuthorizedForValidateOnRecordType(specifiedRecordTypeToValidate);

		useExtendedFunctionalityForPosition(
				ExtendedFunctionalityPosition.VALIDATE_AFTER_AUTHORIZATION);

		recordTypeHandlerForDataToValidate = dependencyProvider
				.getRecordTypeHandlerUsingDataRecordGroup(recordToValidate);
		validateRecord(recordToValidate);
		validateUnique(recordToValidate);
		return createAnswerDataRecord();
	}

	private User tryToGetActiveUser() {
		return authenticator.getUserForToken(authToken);
	}

	private void checkUserIsAuthorizedForCreateOnValidationOrder() {
		authorizator.checkUserIsAuthorizedForActionOnRecordType(user, "create", recordType);
	}

	private void validateValidationOrderThrowErrorIfInvalid(DataGroup validationOrder) {
		String createDefinitionId = getMetadataIdForValidationOrder(validationOrder);
		ValidationAnswer validationAnswer = dataValidator.validateData(createDefinitionId,
				validationOrder);
		if (validationAnswer.dataIsInvalid()) {
			throw new DataException("Data is not valid: " + validationAnswer.getErrorMessages());
		}
	}

	private String getMetadataIdForValidationOrder(DataGroup validationOrder) {
		DataRecordGroup validationOrderAsDataRecordGroup = DataProvider
				.createRecordGroupFromDataGroup(validationOrder);
		RecordTypeHandler validationOrderRecordTypeHandler = dependencyProvider
				.getRecordTypeHandlerUsingDataRecordGroup(validationOrderAsDataRecordGroup);

		return validationOrderRecordTypeHandler.getCreateDefinitionId();
	}

	private String getRecordTypeIdToValidate(DataGroup validationOrder) {
		DataRecordLink typeLink = validationOrder.getFirstChildOfTypeAndName(DataRecordLink.class,
				"recordType");
		return typeLink.getLinkedRecordId();
	}

	private void checkUserIsAuthorizedForValidateOnRecordType(String recordTypeToValidate) {
		authorizator.checkUserIsAuthorizedForActionOnRecordType(user, VALIDATE,
				recordTypeToValidate);
	}

	private void useExtendedFunctionalityForPosition(ExtendedFunctionalityPosition position) {
		List<ExtendedFunctionality> exFunctionality = extendedFunctionalityProvider
				.getFunctionalityForPositionAndRecordType(position, specifiedRecordTypeToValidate);
		useExtendedFunctionality(recordToValidate, exFunctionality);
	}

	@Override
	protected ExtendedFunctionalityData createExtendedFunctionalityData(
			DataRecordGroup dataRecordGroup) {
		ExtendedFunctionalityData data = new ExtendedFunctionalityData();
		data.recordType = specifiedRecordTypeToValidate;
		possiblySetRecordId(data, dataRecordGroup);
		data.authToken = authToken;
		data.user = user;
		data.dataRecordGroup = dataRecordGroup;
		return data;
	}

	private void possiblySetRecordId(ExtendedFunctionalityData data,
			DataRecordGroup dataRecordGroup) {
		try {
			data.recordId = dataRecordGroup.getId();
		} catch (se.uu.ub.cora.data.DataMissingException e) {
			// do nothing as we do not know the recordId in this case
		}
	}

	private void validateRecord(DataRecordGroup recordToValidate) {
		validateRecordExistInStorageWhenActionToPerformIsUpdate();
		DataGroup recordToValidateAsDataGroup = DataProvider
				.createGroupFromRecordGroup(recordToValidate);
		possiblyEnsureLinksExist(recordToValidateAsDataGroup);
		validateRecordTypesMatchBetweenValidationTypeAndSpecifiedType();
		possiblyValidateRecordTypesMatchBetweenValidationOrderAndRecord();
		validateIncomingDataAsSpecifiedInMetadata(recordToValidateAsDataGroup);
	}

	private void possiblyValidateRecordTypesMatchBetweenValidationOrderAndRecord() {
		try {
			validateRecordTypesMatchBetweenValidationOrderAndRecord();
		} catch (Exception e) {
			// missing recordType is currently ok, do nothing if it is not set
		}
	}

	private void validateRecordTypesMatchBetweenValidationOrderAndRecord() {
		String recordTypeOfRecord = recordToValidate.getType();
		if (!recordTypeOfRecord.equals(specifiedRecordTypeToValidate)) {
			String message = "RecordType from record (" + recordTypeOfRecord
					+ ") does not match recordType from validationOrder ("
					+ specifiedRecordTypeToValidate + ")";
			errorList.add(message);
		}
	}

	private void validateRecordTypesMatchBetweenValidationTypeAndSpecifiedType() {
		String recordTypeFromValidationType = recordTypeHandlerForDataToValidate.getRecordTypeId();
		if (!recordTypeFromValidationType.equals(specifiedRecordTypeToValidate)) {
			String message = "RecordType from validationType (" + recordTypeFromValidationType
					+ ") does not match recordType from validationOrder ("
					+ specifiedRecordTypeToValidate + ")";
			errorList.add(message);
		}
	}

	private void validateRecordExistInStorageWhenActionToPerformIsUpdate() {
		if (validationIsForUpdate()) {
			checkIfRecordToValidateIdAlreadyExistsInStorage();
		}
	}

	private boolean validationIsForUpdate() {
		return "update".equals(getActionFromValidationOrder());
	}

	private String getActionFromValidationOrder() {
		String newOrExisting = validationOrder
				.getFirstAtomicValueWithNameInData("metadataToValidate");
		if ("new".equals(newOrExisting)) {
			return "create";
		}
		return "update";
	}

	private void checkIfRecordToValidateIdAlreadyExistsInStorage() {
		boolean recordExists = recordStorage.recordExists(List.of(specifiedRecordTypeToValidate),
				recordToValidate.getId());
		if (!recordExists) {
			errorList.add("The record validated for update does not exist in storage.");
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

	private void possiblyEnsureLinksExist(DataGroup recordToValidateAsDataGroup) {
		if (getValidateLinksFromValidationOrder()) {
			ensureLinksExist(recordToValidateAsDataGroup);
		}
	}

	private boolean getValidateLinksFromValidationOrder() {
		return "true".equals(validationOrder.getFirstAtomicValueWithNameInData("validateLinks"));
	}

	private void ensureLinksExist(DataGroup recordToValidateAsDataGroup) {
		String definitionId = recordTypeHandlerForDataToValidate.getDefinitionId();
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

	private void validateIncomingDataAsSpecifiedInMetadata(DataGroup recordToValidateAsDataGroup) {
		String metadataId = getDefinitionId();
		ValidationAnswer validationAnswer = dataValidator.validateData(metadataId,
				recordToValidateAsDataGroup);

		possiblyAddErrorMessages(validationAnswer);
	}

	private String getDefinitionId() {
		if (validationIsForUpdate()) {
			return recordTypeHandlerForDataToValidate.getUpdateDefinitionId();
		}
		return recordTypeHandlerForDataToValidate.getCreateDefinitionId();
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

	private void validateUnique(DataRecordGroup recordToValidate) {
		DataGroupTermCollector termCollector = dependencyProvider.getDataGroupTermCollector();
		CollectTerms collectedTerms = termCollector.collectTerms(
				recordTypeHandlerForDataToValidate.getDefinitionId(), recordToValidate);
		UniqueValidator validateUniques = dependencyProvider.getUniqueValidator(recordStorage);
		validateUniqueForCreateOrUpdate(recordToValidate, collectedTerms, validateUniques);
	}

	private void validateUniqueForCreateOrUpdate(DataRecordGroup recordToValidate,
			CollectTerms collectedTerms, UniqueValidator validateUniques) {
		if (validationIsForUpdate()) {
			validateUniqueForUpdate(recordToValidate, collectedTerms, validateUniques);
		} else {
			validateUniqueForNewRecord(collectedTerms, validateUniques);
		}
	}

	private void validateUniqueForUpdate(DataRecordGroup recordToValidate,
			CollectTerms collectedTerms, UniqueValidator validateUniques) {
		ValidationAnswer validationAnswer = validateUniques.validateUniqueForExistingRecord(
				specifiedRecordTypeToValidate, recordToValidate.getId(),
				recordTypeHandlerForDataToValidate.getUniqueDefinitions(),
				collectedTerms.storageTerms);
		possiblyAddErrorMessages(validationAnswer);
	}

	private void validateUniqueForNewRecord(CollectTerms collectedTerms,
			UniqueValidator validateUniques) {
		ValidationAnswer validationAnswer = validateUniques.validateUniqueForNewRecord(
				specifiedRecordTypeToValidate,
				recordTypeHandlerForDataToValidate.getUniqueDefinitions(),
				collectedTerms.storageTerms);
		possiblyAddErrorMessages(validationAnswer);
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
		RecordTypeHandler recordTypeHandler = dependencyProvider.getRecordTypeHandler(recordType);

		validationResult.setId(recordTypeHandler.getNextId());
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
		if (errorList.isEmpty()) {
			validationResult
					.addChild(DataProvider.createAtomicUsingNameInDataAndValue("valid", "true"));
		} else {
			validationResult
					.addChild(DataProvider.createAtomicUsingNameInDataAndValue("valid", "false"));
		}
	}

	private void addReadActionToComplyWithRecordStructure(DataRecord dataRecord) {
		dataRecord.addAction(Action.READ);
	}

	private DataRecord createAnswerForDataMissing(
			se.uu.ub.cora.bookkeeper.metadata.DataMissingException e) {
		String errorMessage = "Validation of record to validate not possible due to missing data: ";
		errorList.add(errorMessage + e.getMessage());
		return createAnswerDataRecord();
	}
}
