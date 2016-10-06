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
import se.uu.ub.cora.spider.data.SpiderDataAtomic;
import se.uu.ub.cora.spider.data.SpiderDataGroup;
import se.uu.ub.cora.spider.data.SpiderDataRecord;
import se.uu.ub.cora.spider.extended.ExtendedFunctionality;
import se.uu.ub.cora.spider.extended.ExtendedFunctionalityProvider;
import se.uu.ub.cora.spider.record.storage.RecordIdGenerator;
import se.uu.ub.cora.spider.record.storage.RecordStorage;

public final class SpiderRecordCreatorImp extends SpiderRecordHandler
		implements SpiderRecordCreator {
	private static final String USER = "User:";
	private Authorizator authorization;
	private RecordIdGenerator idGenerator;
	private PermissionKeyCalculator keyCalculator;
	private DataValidator dataValidator;
	private DataGroup recordTypeDefinition;
	private DataRecordLinkCollector linkCollector;
	private String metadataId;
	private ExtendedFunctionalityProvider extendedFunctionalityProvider;
	private String userId;

	public static SpiderRecordCreatorImp usingAuthorizationAndDataValidatorAndRecordStorageAndIdGeneratorAndKeyCalculatorAndLinkCollectorAndExtendedFunctionalityProvider(
			Authorizator authorization, DataValidator dataValidator, RecordStorage recordStorage,
			RecordIdGenerator idGenerator, PermissionKeyCalculator keyCalculator,
			DataRecordLinkCollector linkCollector,
			ExtendedFunctionalityProvider extendedFunctionalityProvider) {
		return new SpiderRecordCreatorImp(authorization, dataValidator, recordStorage, idGenerator,
				keyCalculator, linkCollector, extendedFunctionalityProvider);
	}

	private SpiderRecordCreatorImp(Authorizator authorization, DataValidator dataValidator,
			RecordStorage recordStorage, RecordIdGenerator idGenerator,
			PermissionKeyCalculator keyCalculator, DataRecordLinkCollector linkCollector,
			ExtendedFunctionalityProvider extendedFunctionalityProvider) {
		this.authorization = authorization;
		this.dataValidator = dataValidator;
		this.recordStorage = recordStorage;
		this.idGenerator = idGenerator;
		this.keyCalculator = keyCalculator;
		this.linkCollector = linkCollector;
		this.extendedFunctionalityProvider = extendedFunctionalityProvider;
	}

	@Override
	public SpiderDataRecord createAndStoreRecord(String userId, String recordTypeToCreate,
			SpiderDataGroup spiderDataGroup) {
		this.userId = userId;
		this.recordType = recordTypeToCreate;
		this.spiderDataGroup = spiderDataGroup;
		recordTypeDefinition = getRecordTypeDefinition();
		metadataId = recordTypeDefinition.getFirstAtomicValueWithNameInData("newMetadataId");

		checkNoCreateForAbstractRecordType();
		useExtendedFunctionalityBeforeMetadataValidation(recordTypeToCreate, spiderDataGroup);
		validateDataInRecordAsSpecifiedInMetadata();

		useExtendedFunctionalityAfterMetadataValidation(recordTypeToCreate, spiderDataGroup);
		validateRules();

		ensureCompleteRecordInfo(userId, recordType);

		// set more stuff, user, tscreated, status (created, updated, deleted,
		// etc), published
		// (true, false)
		// set owning organisation

		DataGroup topLevelDataGroup = spiderDataGroup.toDataGroup();

		checkUserIsAuthorisedToCreateIncomingData(userId, recordType, topLevelDataGroup);

		String id = extractIdFromData();

		DataGroup collectedLinks = linkCollector.collectLinks(metadataId, topLevelDataGroup,
				recordType, id);
		checkToPartOfLinkedDataExistsInStorage(collectedLinks);

		String dataDivider = extractDataDividerFromData(spiderDataGroup);
		recordStorage.create(recordType, id, topLevelDataGroup, collectedLinks, dataDivider);

		SpiderDataGroup spiderDataGroupWithActions = SpiderDataGroup
				.fromDataGroup(topLevelDataGroup);

		SpiderDataRecord createDataRecordContainingDataGroup = createDataRecordContainingDataGroup(
				spiderDataGroupWithActions);
		useExtendedFunctionalityBeforeReturn(recordTypeToCreate, spiderDataGroupWithActions);

		return createDataRecordContainingDataGroup;

	}

	private void checkNoCreateForAbstractRecordType() {
		if (isRecordTypeAbstract()) {
			throw new MisuseException(
					"Data creation on abstract recordType:" + recordType + " is not allowed");
		}
	}

	private String extractIdFromData() {
		return spiderDataGroup.extractGroup("recordInfo").extractAtomicValue("id");
	}

	private void validateDataInRecordAsSpecifiedInMetadata() {
		DataGroup record = spiderDataGroup.toDataGroup();

		ValidationAnswer validationAnswer = dataValidator.validateData(metadataId, record);
		if (validationAnswer.dataIsInvalid()) {
			throw new DataException("Data is not valid: " + validationAnswer.getErrorMessages());
		}
	}

	private void useExtendedFunctionalityBeforeMetadataValidation(String recordTypeToCreate,
			SpiderDataGroup spiderDataGroup) {
		List<ExtendedFunctionality> functionalityForCreateBeforeMetadataValidation = extendedFunctionalityProvider
				.getFunctionalityForCreateBeforeMetadataValidation(recordTypeToCreate);
		useExtendedFunctionality(spiderDataGroup, functionalityForCreateBeforeMetadataValidation);
	}

	private void useExtendedFunctionality(SpiderDataGroup spiderDataGroup,
			List<ExtendedFunctionality> functionalityForCreateAfterMetadataValidation) {
		for (ExtendedFunctionality extendedFunctionality : functionalityForCreateAfterMetadataValidation) {
			extendedFunctionality.useExtendedFunctionality(userId, spiderDataGroup);
		}
	}

	private void useExtendedFunctionalityAfterMetadataValidation(String recordTypeToCreate,
			SpiderDataGroup spiderDataGroup) {
		List<ExtendedFunctionality> functionalityForCreateAfterMetadataValidation = extendedFunctionalityProvider
				.getFunctionalityForCreateAfterMetadataValidation(recordTypeToCreate);
		useExtendedFunctionality(spiderDataGroup, functionalityForCreateAfterMetadataValidation);
	}

	private void useExtendedFunctionalityBeforeReturn(String recordTypeToCreate,
			SpiderDataGroup spiderDataGroup) {
		List<ExtendedFunctionality> extendedFunctionalityList = extendedFunctionalityProvider
				.getFunctionalityForCreateBeforeReturn(recordTypeToCreate);
		useExtendedFunctionality(spiderDataGroup, extendedFunctionalityList);
	}

	private void ensureCompleteRecordInfo(String userId, String recordType) {
		ensureIdExists(recordType);
		addUserAndTypeToRecordInfo(userId, recordType);
	}

	private void ensureIdExists(String recordType) {
		if (shouldAutoGenerateId(recordTypeDefinition)) {
			generateAndAddIdToRecordInfo(recordType);
		}
	}

	private boolean shouldAutoGenerateId(DataGroup recordTypeDataGroup) {
		String userSuppliedId = recordTypeDataGroup
				.getFirstAtomicValueWithNameInData("userSuppliedId");
		return "false".equals(userSuppliedId);
	}

	private void generateAndAddIdToRecordInfo(String recordType) {
		SpiderDataGroup recordInfo = spiderDataGroup.extractGroup(RECORD_INFO);
		recordInfo.addChild(SpiderDataAtomic.withNameInDataAndValue("id",
				idGenerator.getIdForType(recordType)));
	}

	private void addUserAndTypeToRecordInfo(String userId, String recordType) {
		SpiderDataGroup recordInfo = spiderDataGroup.extractGroup(RECORD_INFO);
		recordInfo.addChild(SpiderDataAtomic.withNameInDataAndValue("type", recordType));
		recordInfo.addChild(SpiderDataAtomic.withNameInDataAndValue("createdBy", userId));
	}

	private void checkUserIsAuthorisedToCreateIncomingData(String userId, String recordType,
			DataGroup record) {
		// calculate permissionKey
		String accessType = "CREATE";
		Set<String> recordCalculateKeys = keyCalculator.calculateKeys(accessType, recordType,
				record);

		if (!authorization.isAuthorized(userId, recordCalculateKeys)) {
			throw new AuthorizationException(
					USER + userId + " is not authorized to create a record  of type:" + recordType);
		}
	}

	@Override
	protected boolean incomingLinksExistsForRecord(SpiderDataRecord spiderDataRecord) {
		// a record that is being created, can not yet be linked from any other
		// record
		return false;
	}

}
