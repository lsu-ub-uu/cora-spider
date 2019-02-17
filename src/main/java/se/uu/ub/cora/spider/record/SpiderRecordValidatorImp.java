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

import se.uu.ub.cora.beefeater.authentication.User;
import se.uu.ub.cora.bookkeeper.data.DataGroup;
import se.uu.ub.cora.bookkeeper.linkcollector.DataRecordLinkCollector;
import se.uu.ub.cora.bookkeeper.validator.DataValidator;
import se.uu.ub.cora.bookkeeper.validator.ValidationAnswer;
import se.uu.ub.cora.spider.authentication.Authenticator;
import se.uu.ub.cora.spider.authorization.SpiderAuthorizator;
import se.uu.ub.cora.spider.data.SpiderDataGroup;
import se.uu.ub.cora.spider.dependency.SpiderDependencyProvider;

public final class SpiderRecordValidatorImp extends SpiderRecordHandler
		implements SpiderRecordValidator {
	private static final String VALIDATE = "validate";
	private Authenticator authenticator;
	private SpiderAuthorizator spiderAuthorizator;
	private DataValidator dataValidator;
	private DataRecordLinkCollector linkCollector;
	private String authToken;
	private User user;
	private String metadataToValidate;

	private SpiderRecordValidatorImp(SpiderDependencyProvider dependencyProvider) {
		this.authenticator = dependencyProvider.getAuthenticator();
		this.spiderAuthorizator = dependencyProvider.getSpiderAuthorizator();
		this.dataValidator = dependencyProvider.getDataValidator();
		this.recordStorage = dependencyProvider.getRecordStorage();
		this.linkCollector = dependencyProvider.getDataRecordLinkCollector();
	}

	public static SpiderRecordValidatorImp usingDependencyProvider(
			SpiderDependencyProvider dependencyProvider) {
		return new SpiderRecordValidatorImp(dependencyProvider);
	}

	@Override
	public ValidationResult validateRecord(String authToken, String recordType,
			SpiderDataGroup validationRecord, SpiderDataGroup recordToValidate) {
		this.authToken = authToken;
		this.recordAsSpiderDataGroup = recordToValidate;
		this.recordType = recordType;
		user = tryToGetActiveUser();
		checkUserIsAuthorizedForActionOnRecordType();

		return validateRecordUsingValidationRecord(validationRecord);
	}

	private User tryToGetActiveUser() {
		return authenticator.getUserForToken(authToken);
	}

	private void checkUserIsAuthorizedForActionOnRecordType() {
		spiderAuthorizator.checkUserIsAuthorizedForActionOnRecordType(user, VALIDATE, recordType);
	}

	private ValidationResult validateRecordUsingValidationRecord(SpiderDataGroup validationRecord) {
		metadataToValidate = validationRecord.extractAtomicValue("metadataToValidate");
		String metadataId = getMetadataId();

		String recordIdOrNullIfCreate = extractRecordIdIfUpdate();
		ensureRecordExistWhenActionToPerformIsUpdate(recordIdOrNullIfCreate);
		possiblyEnsureLinksExist(validationRecord, recordIdOrNullIfCreate, metadataId);

		return validateIncomingDataAsSpecifiedInMetadata(metadataId);
	}

	private String getMetadataId() {
		RecordTypeHandler recordTypeHandler = RecordTypeHandler
				.usingRecordStorageAndRecordTypeId(recordStorage, recordType);
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
		return recordAsSpiderDataGroup.extractGroup("recordInfo").extractAtomicValue("id");
	}

	private void ensureRecordExistWhenActionToPerformIsUpdate(String recordIdToUse) {
		if ("existing".equals(metadataToValidate)) {
			recordStorage.read(recordType, recordIdToUse);
		}
	}

	private void possiblyEnsureLinksExist(SpiderDataGroup validationRecord,
			String recordIdOrNullIfCreate, String metadataId) {
		String validateLinks = validationRecord.extractAtomicValue("validateLinks");
		if ("true".equals(validateLinks)) {
			ensureLinksExist(recordIdOrNullIfCreate, metadataId);
		}
	}

	private void ensureLinksExist(String recordIdToUse, String metadataId) {
		DataGroup topLevelDataGroup = recordAsSpiderDataGroup.toDataGroup();
		DataGroup collectedLinks = linkCollector.collectLinks(metadataId, topLevelDataGroup,
				recordType, recordIdToUse);
		checkToPartOfLinkedDataExistsInStorage(collectedLinks);
	}

	private ValidationResult validateIncomingDataAsSpecifiedInMetadata(String metadataId) {
		DataGroup dataGroup = recordAsSpiderDataGroup.toDataGroup();
		ValidationAnswer validationAnswer = dataValidator.validateData(metadataId, dataGroup);
		return createValidationResult(validationAnswer);
	}

	private ValidationResult createValidationResult(ValidationAnswer validationAnswer) {
		ValidationResult validationResult = new ValidationResult();
		possiblyAddErrorMessages(validationAnswer, validationResult);
		return validationResult;
	}

	private void possiblyAddErrorMessages(ValidationAnswer validationAnswer,
			ValidationResult validationResult) {
		if (validationAnswer.dataIsInvalid()) {
			addErrorMessages(validationAnswer, validationResult);
		}
	}

	private void addErrorMessages(ValidationAnswer validationAnswer,
			ValidationResult validationResult) {
		for (String errorMessage : validationAnswer.getErrorMessages()) {
			validationResult.addErrorMessage(errorMessage);
		}
	}

}
