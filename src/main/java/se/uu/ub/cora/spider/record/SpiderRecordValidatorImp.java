/*
 * Copyright 2019 Uppsala University Library
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

package se.uu.ub.cora.spider.record;

import java.time.LocalDateTime;

import se.uu.ub.cora.beefeater.authentication.User;
import se.uu.ub.cora.bookkeeper.linkcollector.DataRecordLinkCollector;
import se.uu.ub.cora.bookkeeper.validator.DataValidator;
import se.uu.ub.cora.bookkeeper.validator.ValidationAnswer;
import se.uu.ub.cora.data.DataGroup;
import se.uu.ub.cora.spider.authentication.Authenticator;
import se.uu.ub.cora.spider.authorization.SpiderAuthorizator;
import se.uu.ub.cora.spider.data.Action;
import se.uu.ub.cora.spider.data.SpiderDataAtomic;
import se.uu.ub.cora.spider.data.SpiderDataGroup;
import se.uu.ub.cora.spider.data.SpiderDataRecord;
import se.uu.ub.cora.spider.dependency.SpiderDependencyProvider;
import se.uu.ub.cora.storage.RecordIdGenerator;
import se.uu.ub.cora.storage.RecordNotFoundException;

public final class SpiderRecordValidatorImp extends SpiderRecordHandler
		implements SpiderRecordValidator {
	private static final String ERROR_MESSAGES = "errorMessages";
	private static final String VALIDATE = "validate";
	private Authenticator authenticator;
	private SpiderAuthorizator spiderAuthorizator;
	private DataValidator dataValidator;
	private DataRecordLinkCollector linkCollector;
	private String authToken;
	private User user;
	private String metadataToValidate;
	private SpiderDataGroup validationResult;
	private RecordIdGenerator idGenerator;

	private SpiderRecordValidatorImp(SpiderDependencyProvider dependencyProvider) {
		this.authenticator = dependencyProvider.getAuthenticator();
		this.spiderAuthorizator = dependencyProvider.getSpiderAuthorizator();
		this.dataValidator = dependencyProvider.getDataValidator();
		this.recordStorage = dependencyProvider.getRecordStorage();
		this.linkCollector = dependencyProvider.getDataRecordLinkCollector();
		this.idGenerator = dependencyProvider.getRecordIdGenerator();
	}

	public static SpiderRecordValidator usingDependencyProvider(
			SpiderDependencyProvider dependencyProvider) {
		return new SpiderRecordValidatorImp(dependencyProvider);
	}

	@Override
	public SpiderDataRecord validateRecord(String authToken, String recordType,
			SpiderDataGroup validationRecord, SpiderDataGroup recordToValidate) {
		this.authToken = authToken;
		this.recordAsDataGroup = recordToValidate;
		this.recordType = recordType;
		user = tryToGetActiveUser();
		checkValidationRecordIsOkBeforeValidation(validationRecord);
		return validateRecord(validationRecord);
	}

	private User tryToGetActiveUser() {
		return authenticator.getUserForToken(authToken);
	}

	private void checkValidationRecordIsOkBeforeValidation(SpiderDataGroup validationRecord) {
		checkUserIsAuthorizedForCreateOnRecordType();
		validateWorkOrderAsSpecifiedInMetadata(validationRecord.toDataGroup());
	}

	private String getMetadataIdForWorkOrder(String recordType) {
		RecordTypeHandler recordTypeHandler = RecordTypeHandler
				.usingRecordStorageAndRecordTypeId(recordStorage, recordType);
		return recordTypeHandler.getNewMetadataId();
	}

	private void checkUserIsAuthorizedForCreateOnRecordType() {
		spiderAuthorizator.checkUserIsAuthorizedForActionOnRecordType(user, "create", recordType);
	}

	private void validateWorkOrderAsSpecifiedInMetadata(DataGroup dataGroup) {
		String metadataIdForWorkOrder = getMetadataIdForWorkOrder(recordType);
		ValidationAnswer validationAnswer = dataValidator.validateData(metadataIdForWorkOrder,
				dataGroup);
		if (validationAnswer.dataIsInvalid()) {
			throw new DataException("Data is not valid: " + validationAnswer.getErrorMessages());
		}
	}

	private SpiderDataRecord validateRecord(SpiderDataGroup validationRecord) {
		String recordTypeToValidate = getRecordTypeToValidate(validationRecord);

		checkUserIsAuthorizedForValidateOnRecordType(recordTypeToValidate);

		createValidationResultDataGroup();
		validateRecordUsingValidationRecord(validationRecord, recordTypeToValidate);
		SpiderDataRecord record = SpiderDataRecord.withSpiderDataGroup(validationResult);
		addReadActionToComplyWithRecordStructure(record);
		return record;
	}

	private String getRecordTypeToValidate(SpiderDataGroup validationRecord) {
		SpiderDataGroup recordTypeGroup = validationRecord.extractGroup("recordType");
		return recordTypeGroup.extractAtomicValue(LINKED_RECORD_ID);
	}

	private void checkUserIsAuthorizedForValidateOnRecordType(String recordTypeToValidate) {
		spiderAuthorizator.checkUserIsAuthorizedForActionOnRecordType(user, VALIDATE,
				recordTypeToValidate);
	}

	private void createValidationResultDataGroup() {
		validationResult = SpiderDataGroup.withNameInData("validationResult");
		SpiderDataGroup recordInfo = SpiderDataGroup.withNameInData("recordInfo");
		recordInfo.addChild(SpiderDataAtomic.withNameInDataAndValue("id",
				idGenerator.getIdForType(recordType)));

		SpiderDataGroup recordTypeGroup = createTypeDataGroup(recordType);
		recordInfo.addChild(recordTypeGroup);

		addCreatedInfoToRecordInfoUsingUserId(recordInfo, user.id);
		addUpdatedInfoToRecordInfoUsingUserId(recordInfo, user.id);
		validationResult.addChild(recordInfo);
	}

	private SpiderDataGroup createTypeDataGroup(String recordType) {
		SpiderDataGroup type = SpiderDataGroup.withNameInData("type");
		type.addChild(SpiderDataAtomic.withNameInDataAndValue("linkedRecordType", "recordType"));
		type.addChild(SpiderDataAtomic.withNameInDataAndValue("linkedRecordId", recordType));
		return type;
	}

	private void addCreatedInfoToRecordInfoUsingUserId(SpiderDataGroup recordInfo, String userId) {
		SpiderDataGroup createdByGroup = createLinkToUserUsingUserIdAndNameInData(userId,
				"createdBy");
		recordInfo.addChild(createdByGroup);
		String currentLocalDateTime = getLocalTimeDateAsString(LocalDateTime.now());
		recordInfo.addChild(
				SpiderDataAtomic.withNameInDataAndValue(TS_CREATED, currentLocalDateTime));
	}

	private void validateRecordUsingValidationRecord(SpiderDataGroup validationRecord,
			String recordTypeToValidate) {
		metadataToValidate = validationRecord.extractAtomicValue("metadataToValidate");

		String recordIdOrNullIfCreate = extractRecordIdIfUpdate();
		ensureRecordExistWhenActionToPerformIsUpdate(recordTypeToValidate, recordIdOrNullIfCreate);

		String metadataId = getMetadataId(recordTypeToValidate);
		possiblyEnsureLinksExist(validationRecord, recordTypeToValidate, recordIdOrNullIfCreate,
				metadataId);

		validateIncomingDataAsSpecifiedInMetadata(metadataId);
	}

	private String getMetadataId(String recordTypeToValidate) {
		RecordTypeHandler recordTypeHandler = RecordTypeHandler
				.usingRecordStorageAndRecordTypeId(recordStorage, recordTypeToValidate);
		return validateNew() ? recordTypeHandler.getNewMetadataId()
				: recordTypeHandler.getMetadataId();
	}

	private boolean validateNew() {
		return "new".equals(metadataToValidate);
	}

	private String extractRecordIdIfUpdate() {
		return "existing".equals(metadataToValidate) ? extractIdFromData() : null;
	}

	private String extractIdFromData() {
		return recordAsDataGroup.extractGroup("recordInfo").extractAtomicValue("id");
	}

	private void ensureRecordExistWhenActionToPerformIsUpdate(String recordTypeToValidate,
			String recordIdToUse) {
		if ("existing".equals(metadataToValidate)) {
			checkIfRecordExist(recordTypeToValidate, recordIdToUse);
		}
	}

	private void checkIfRecordExist(String recordTypeToValidate, String recordIdToUse) {
		try {
			recordStorage.read(recordTypeToValidate, recordIdToUse);
		} catch (RecordNotFoundException exception) {
			addErrorToValidationResult(exception.getMessage());
		}
	}

	private void addErrorToValidationResult(String message) {
		SpiderDataGroup errorMessages = getErrorMessagesGroup();
		int repeatId = calculateRepeatId(errorMessages);
		SpiderDataAtomic error = createErrorWithMessageAndRepeatId(message, repeatId);
		errorMessages.addChild(error);

	}

	private SpiderDataGroup getErrorMessagesGroup() {
		ensureErrorMessagesGroupExist();
		return validationResult.extractGroup(ERROR_MESSAGES);
	}

	private void ensureErrorMessagesGroupExist() {
		if (!validationResult.containsChildWithNameInData(ERROR_MESSAGES)) {
			validationResult.addChild(SpiderDataGroup.withNameInData(ERROR_MESSAGES));
		}
	}

	private int calculateRepeatId(SpiderDataGroup errorMessages) {
		return errorMessages.getChildren().isEmpty() ? 0 : errorMessages.getChildren().size();
	}

	private SpiderDataAtomic createErrorWithMessageAndRepeatId(String message, int repeatId) {
		SpiderDataAtomic error = SpiderDataAtomic.withNameInDataAndValue("errorMessage", message);
		error.setRepeatId(String.valueOf(repeatId));
		return error;
	}

	private void possiblyEnsureLinksExist(SpiderDataGroup validationRecord,
			String recordTypeToValidate, String recordIdOrNullIfCreate, String metadataId) {
		String validateLinks = validationRecord.extractAtomicValue("validateLinks");
		if ("true".equals(validateLinks)) {
			ensureLinksExist(recordTypeToValidate, recordIdOrNullIfCreate, metadataId);
		}
	}

	private void ensureLinksExist(String recordTypeToValidate, String recordIdToUse,
			String metadataId) {
		DataGroup topLevelDataGroup = recordAsDataGroup.toDataGroup();
		DataGroup collectedLinks = linkCollector.collectLinks(metadataId, topLevelDataGroup,
				recordTypeToValidate, recordIdToUse);
		checkIfLinksExist(collectedLinks);
	}

	private void checkIfLinksExist(DataGroup collectedLinks) {
		try {
			checkToPartOfLinkedDataExistsInStorage(collectedLinks);
		} catch (DataException exception) {
			addErrorToValidationResult(exception.getMessage());
		}
	}

	private void validateIncomingDataAsSpecifiedInMetadata(String metadataId) {
		DataGroup dataGroup = recordAsDataGroup.toDataGroup();
		ValidationAnswer validationAnswer = dataValidator.validateData(metadataId, dataGroup);
		possiblyAddErrorMessages(validationAnswer);
		if (validationResult.containsChildWithNameInData(ERROR_MESSAGES)) {
			validationResult.addChild(SpiderDataAtomic.withNameInDataAndValue("valid", "false"));
		} else {
			validationResult.addChild(SpiderDataAtomic.withNameInDataAndValue("valid", "true"));
		}
	}

	private void possiblyAddErrorMessages(ValidationAnswer validationAnswer) {
		if (validationAnswer.dataIsInvalid()) {
			addErrorMessages(validationAnswer);
		}
	}

	private void addErrorMessages(ValidationAnswer validationAnswer) {
		for (String errorMessage : validationAnswer.getErrorMessages()) {
			addErrorToValidationResult(errorMessage);
		}
	}

	private void addReadActionToComplyWithRecordStructure(SpiderDataRecord record) {
		record.addAction(Action.READ);
	}
}
