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

import java.util.Set;

import se.uu.ub.cora.beefeater.Authorizator;
import se.uu.ub.cora.bookkeeper.data.DataGroup;
import se.uu.ub.cora.bookkeeper.linkcollector.DataRecordLinkCollector;
import se.uu.ub.cora.bookkeeper.validator.DataValidator;
import se.uu.ub.cora.bookkeeper.validator.ValidationAnswer;
import se.uu.ub.cora.spider.data.Action;
import se.uu.ub.cora.spider.data.SpiderDataAtomic;
import se.uu.ub.cora.spider.data.SpiderDataGroup;
import se.uu.ub.cora.spider.data.SpiderDataRecord;
import se.uu.ub.cora.spider.record.storage.RecordIdGenerator;
import se.uu.ub.cora.spider.record.storage.RecordNotFoundException;
import se.uu.ub.cora.spider.record.storage.RecordStorage;

public final class SpiderRecordCreatorImp implements SpiderRecordCreator {
	private static final String RECORD_INFO = "recordInfo";
	private static final String USER = "User:";
	private RecordStorage recordStorage;
	private Authorizator authorization;
	private RecordIdGenerator idGenerator;
	private PermissionKeyCalculator keyCalculator;
	private DataValidator dataValidator;
	private DataGroup recordTypeDefinition;
	private SpiderDataGroup spiderDataGroup;
	private DataRecordLinkCollector linkCollector;

	public static SpiderRecordCreatorImp usingAuthorizationAndDataValidatorAndRecordStorageAndIdGeneratorAndKeyCalculator(
			Authorizator authorization, DataValidator dataValidator, RecordStorage recordStorage,
			RecordIdGenerator idGenerator, PermissionKeyCalculator keyCalculator,
			DataRecordLinkCollector linkCollector) {
		return new SpiderRecordCreatorImp(authorization, dataValidator, recordStorage, idGenerator,
				keyCalculator, linkCollector);
	}

	private SpiderRecordCreatorImp(Authorizator authorization, DataValidator dataValidator,
			RecordStorage recordStorage, RecordIdGenerator idGenerator,
			PermissionKeyCalculator keyCalculator, DataRecordLinkCollector linkCollector) {
		this.authorization = authorization;
		this.dataValidator = dataValidator;
		this.recordStorage = recordStorage;
		this.idGenerator = idGenerator;
		this.keyCalculator = keyCalculator;
		this.linkCollector = linkCollector;

	}

	@Override
	public SpiderDataRecord createAndStoreRecord(String userId, String recordType,
			SpiderDataGroup spiderDataGroup) {

		this.spiderDataGroup = spiderDataGroup;
		recordTypeDefinition = getRecordTypeDefinition(recordType);

		checkNoCreateForAbstractRecordType(recordType);
		validateDataInRecordAsSpecifiedInMetadata();

		ensureCompleteRecordInfo(userId, recordType);

		// set more stuff, user, tscreated, status (created, updated, deleted,
		// etc), published
		// (true, false)
		// set owning organisation

		DataGroup topLevelDataGroup = spiderDataGroup.toDataGroup();

		checkUserIsAuthorisedToCreateIncomingData(userId, recordType, topLevelDataGroup);

		// send to storage
		String id = extractIdFromData();

		String metadataId = recordTypeDefinition.getFirstAtomicValueWithNameInData("newMetadataId");
		DataGroup collectedLinks = linkCollector.collectLinks(metadataId, topLevelDataGroup,
				recordType, id);

		recordStorage.create(recordType, id, topLevelDataGroup, collectedLinks);

		return createDataRecordContainingDataGroup(spiderDataGroup);
	}

	private String extractIdFromData() {
		return spiderDataGroup.extractGroup("recordInfo").extractAtomicValue("id");

	}

	private DataGroup getRecordTypeDefinition(String recordType) {
		try {
			return recordStorage.read("recordType", recordType);
		} catch (RecordNotFoundException e) {
			throw new DataException("recordType:" + recordType + " does not exist", e);
		}
	}

	private void checkNoCreateForAbstractRecordType(String recordType) {
		if (isRecordTypeAbstract()) {
			throw new MisuseException(
					"Data creation on abstract recordType:" + recordType + " is not allowed");
		}
	}

	private boolean isRecordTypeAbstract() {
		String abstractInRecordTypeDefinition = recordTypeDefinition
				.getFirstAtomicValueWithNameInData("abstract");
		return "true".equals(abstractInRecordTypeDefinition);
	}

	private void validateDataInRecordAsSpecifiedInMetadata() {
		DataGroup record = spiderDataGroup.toDataGroup();

		String metadataId = recordTypeDefinition.getFirstAtomicValueWithNameInData("newMetadataId");
		ValidationAnswer validationAnswer = dataValidator.validateData(metadataId, record);
		if (validationAnswer.dataIsInvalid()) {
			throw new DataException("Data is not valid: " + validationAnswer.getErrorMessages());
		}
	}

	private void ensureCompleteRecordInfo(String userId, String recordType) {
		ensureRecordInfoExists(recordType);
		addUserAndTypeToRecordInfo(userId, recordType);
	}

	private void ensureRecordInfoExists(String recordType) {
		if (shouldAutoGenerateId(recordTypeDefinition)) {
			addRecordInfoToDataGroup(recordType);
		}
	}

	private boolean shouldAutoGenerateId(DataGroup recordTypeDataGroup) {
		String userSuppliedId = recordTypeDataGroup
				.getFirstAtomicValueWithNameInData("userSuppliedId");
		return "false".equals(userSuppliedId);
	}

	private void addRecordInfoToDataGroup(String recordType) {
		SpiderDataGroup recordInfo = createRecordInfo(recordType);
		spiderDataGroup.addChild(recordInfo);
	}

	private void addUserAndTypeToRecordInfo(String userId, String recordType) {
		SpiderDataGroup recordInfo = spiderDataGroup.extractGroup(RECORD_INFO);
		recordInfo.addChild(SpiderDataAtomic.withNameInDataAndValue("type", recordType));
		recordInfo.addChild(SpiderDataAtomic.withNameInDataAndValue("createdBy", userId));
	}

	private SpiderDataGroup createRecordInfo(String recordType) {
		SpiderDataGroup recordInfo = SpiderDataGroup.withNameInData(RECORD_INFO);
		recordInfo.addChild(SpiderDataAtomic.withNameInDataAndValue("id",
				idGenerator.getIdForType(recordType)));
		return recordInfo;
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

	private SpiderDataRecord createDataRecordContainingDataGroup(SpiderDataGroup spiderDataGroup) {
		// create record
		SpiderDataRecord spiderDataRecord = SpiderDataRecord.withSpiderDataGroup(spiderDataGroup);
		addLinks(spiderDataRecord);
		return spiderDataRecord;
	}

	private void addLinks(SpiderDataRecord spiderDataRecord) {
		spiderDataRecord.addAction(Action.READ);
		spiderDataRecord.addAction(Action.UPDATE);
		spiderDataRecord.addAction(Action.DELETE);
		spiderDataRecord.addAction(Action.READ_INCOMING_LINKS);
	}

}
