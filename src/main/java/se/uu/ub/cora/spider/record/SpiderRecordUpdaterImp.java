/*
 * Copyright 2015 Uppsala University Library
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

import java.util.List;
import java.util.Set;

import se.uu.ub.cora.beefeater.Authorizator;
import se.uu.ub.cora.bookkeeper.data.DataGroup;
import se.uu.ub.cora.bookkeeper.linkcollector.DataRecordLinkCollector;
import se.uu.ub.cora.bookkeeper.validator.DataValidator;
import se.uu.ub.cora.bookkeeper.validator.ValidationAnswer;
import se.uu.ub.cora.spider.data.SpiderDataGroup;
import se.uu.ub.cora.spider.data.SpiderDataRecord;
import se.uu.ub.cora.spider.extended.ExtendedFunctionality;
import se.uu.ub.cora.spider.extended.ExtendedFunctionalityProvider;
import se.uu.ub.cora.spider.record.storage.RecordStorage;

public final class SpiderRecordUpdaterImp extends SpiderRecordHandler
		implements SpiderRecordUpdater {
	private static final String USER = "User:";
	private Authorizator authorization;
	private PermissionKeyCalculator keyCalculator;
	private DataValidator dataValidator;
	private DataRecordLinkCollector linkCollector;
	private String metadataId;
	private ExtendedFunctionalityProvider extendedFunctionalityProvider;
	private String userId;

	public static SpiderRecordUpdaterImp usingAuthorizationAndDataValidatorAndRecordStorageAndKeyCalculatorAndLinkCollectorAndExtendedFunctionalityProvider(
			Authorizator authorization, DataValidator dataValidator, RecordStorage recordStorage,
			PermissionKeyCalculator keyCalculator, DataRecordLinkCollector linkCollector,
			ExtendedFunctionalityProvider extendedFunctionalityProvider) {
		return new SpiderRecordUpdaterImp(authorization, dataValidator, recordStorage,
				keyCalculator, linkCollector, extendedFunctionalityProvider);
	}

	private SpiderRecordUpdaterImp(Authorizator authorization, DataValidator dataValidator,
			RecordStorage recordStorage, PermissionKeyCalculator keyCalculator,
			DataRecordLinkCollector linkCollector,
			ExtendedFunctionalityProvider extendedFunctionalityProvider) {
		this.authorization = authorization;
		this.dataValidator = dataValidator;
		this.recordStorage = recordStorage;
		this.keyCalculator = keyCalculator;
		this.linkCollector = linkCollector;
		this.extendedFunctionalityProvider = extendedFunctionalityProvider;
	}

	@Override
	public SpiderDataRecord updateRecord(String userId, String recordType, String recordId,
			SpiderDataGroup spiderDataGroup) {
		this.userId = userId;
		this.spiderDataGroup = spiderDataGroup;
		this.recordType = recordType;
		this.recordId = recordId;
		DataGroup recordTypeDefinition = getRecordTypeDefinition();
		metadataId = recordTypeDefinition.getFirstAtomicValueWithNameInData("metadataId");

		checkNoUpdateForAbstractRecordType();
		useExtendedFunctionalityBeforeMetadataValidation(recordType, spiderDataGroup);

		validateIncomingDataAsSpecifiedInMetadata();
		useExtendedFunctionalityAfterMetadataValidation(recordType, spiderDataGroup);

		validateRules();

		checkRecordTypeAndIdIsSameAsInEnteredRecord();

		DataGroup topLevelDataGroup = spiderDataGroup.toDataGroup();

		checkUserIsAuthorisedToUpdate(userId);
		checkUserIsAuthorisedToStoreIncomingData(userId, topLevelDataGroup);

		// validate (including protected data)
		// TODO: add validate here

		// merge possibly hidden data
		// TODO: merge incoming data with stored if user does not have right to
		// update some parts

		DataGroup collectedLinks = linkCollector.collectLinks(metadataId, topLevelDataGroup,
				recordType, recordId);
		checkToPartOfLinkedDataExistsInStorage(collectedLinks);

		String dataDivider = extractDataDividerFromData(spiderDataGroup);
		recordStorage.update(recordType, recordId, spiderDataGroup.toDataGroup(), collectedLinks,
				dataDivider);

		SpiderDataGroup spiderDataGroupWithActions = SpiderDataGroup
				.fromDataGroup(topLevelDataGroup);

		return createDataRecordContainingDataGroup(spiderDataGroupWithActions);
	}

	private void checkNoUpdateForAbstractRecordType() {
		if (isRecordTypeAbstract()) {
			throw new MisuseException(
					"Data update on abstract recordType:" + recordType + " is not allowed");
		}
	}

	private void useExtendedFunctionalityBeforeMetadataValidation(String recordTypeToCreate,
			SpiderDataGroup spiderDataGroup) {
		List<ExtendedFunctionality> functionalityForUpdateBeforeMetadataValidation = extendedFunctionalityProvider
				.getFunctionalityForUpdateBeforeMetadataValidation(recordTypeToCreate);
		useExtendedFunctionality(spiderDataGroup, functionalityForUpdateBeforeMetadataValidation);
	}

	private void useExtendedFunctionality(SpiderDataGroup spiderDataGroup,
			List<ExtendedFunctionality> functionalityForCreateAfterMetadataValidation) {
		for (ExtendedFunctionality extendedFunctionality : functionalityForCreateAfterMetadataValidation) {
			extendedFunctionality.useExtendedFunctionality(userId, spiderDataGroup);
		}
	}

	private void useExtendedFunctionalityAfterMetadataValidation(String recordTypeToCreate,
			SpiderDataGroup spiderDataGroup) {
		List<ExtendedFunctionality> functionalityForUpdateAfterMetadataValidation = extendedFunctionalityProvider
				.getFunctionalityForUpdateAfterMetadataValidation(recordTypeToCreate);
		useExtendedFunctionality(spiderDataGroup, functionalityForUpdateAfterMetadataValidation);
	}

	private void validateIncomingDataAsSpecifiedInMetadata() {
		DataGroup dataGroup = spiderDataGroup.toDataGroup();
		ValidationAnswer validationAnswer = dataValidator.validateData(metadataId, dataGroup);
		if (validationAnswer.dataIsInvalid()) {
			throw new DataException("Data is not valid: " + validationAnswer.getErrorMessages());
		}
	}

	private void checkRecordTypeAndIdIsSameAsInEnteredRecord() {
		checkValueIsSameAsInEnteredRecord(recordId, "id");
		checkValueIsSameAsInEnteredRecord(recordType, "type");
	}

	private void checkValueIsSameAsInEnteredRecord(String value, String valueToExtract) {
		SpiderDataGroup recordInfo = spiderDataGroup.extractGroup(RECORD_INFO);
		String valueFromRecord = recordInfo.extractAtomicValue(valueToExtract);
		if (!value.equals(valueFromRecord)) {
			throw new DataException("Value in data(" + valueFromRecord
					+ ") does not match entered value(" + value + ")");
		}
	}

	private void checkUserIsAuthorisedToUpdate(String userId) {
		DataGroup recordRead = recordStorage.read(recordType, recordId);

		String accessType = "UPDATE";
		Set<String> recordCalculateKeys = keyCalculator.calculateKeys(accessType, recordType,
				recordRead);

		if (!authorization.isAuthorized(userId, recordCalculateKeys)) {
			throw new AuthorizationException(USER + userId + " is not authorized to update record:"
					+ recordId + "  of type:" + recordType);
		}
	}

	private void checkUserIsAuthorisedToStoreIncomingData(String userId, DataGroup incomingData) {

		// calculate permissionKey
		String accessType = "UPDATE";
		Set<String> recordCalculateKeys = keyCalculator.calculateKeys(accessType, recordType,
				incomingData);

		if (!authorization.isAuthorized(userId, recordCalculateKeys)) {
			throw new AuthorizationException(
					USER + userId + " is not authorized to store this incoming data for recordType:"
							+ recordType);
		}
	}

}
