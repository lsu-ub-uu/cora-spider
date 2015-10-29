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
import se.uu.ub.cora.bookkeeper.validator.DataValidator;
import se.uu.ub.cora.bookkeeper.validator.ValidationAnswer;
import se.uu.ub.cora.spider.data.Action;
import se.uu.ub.cora.spider.data.SpiderDataGroup;
import se.uu.ub.cora.spider.data.SpiderDataRecord;
import se.uu.ub.cora.spider.record.storage.RecordStorage;

public final class SpiderRecordUpdaterImp implements SpiderRecordUpdater {
	private static final String RECORD_INFO = "recordInfo";
	private static final String USER = "User:";
	private RecordStorage recordStorage;
	private Authorizator authorization;
	private PermissionKeyCalculator keyCalculator;
	private DataValidator dataValidator;
	private SpiderDataGroup spiderDataGroup;
	private DataGroup recordTypeDefinition;

	public static SpiderRecordUpdaterImp usingAuthorizationAndDataValidatorAndRecordStorageAndKeyCalculator(
			Authorizator authorization, DataValidator dataValidator, RecordStorage recordStorage,
			PermissionKeyCalculator keyCalculator) {
		return new SpiderRecordUpdaterImp(authorization, dataValidator, recordStorage,
				keyCalculator);
	}

	private SpiderRecordUpdaterImp(Authorizator authorization, DataValidator dataValidator,
			RecordStorage recordStorage, PermissionKeyCalculator keyCalculator) {
		this.authorization = authorization;
		this.dataValidator = dataValidator;
		this.recordStorage = recordStorage;
		this.keyCalculator = keyCalculator;
	}

	@Override
	public SpiderDataRecord updateRecord(String userId, String recordType, String id,
			SpiderDataGroup spiderDataGroup) {
		this.spiderDataGroup = spiderDataGroup;
		recordTypeDefinition = getRecordTypeDefinition(recordType);

		checkNoUpdateForAbstractRecordType(recordType);
		validateIncomingDataAsSpecifiedInMetadata();

		checkRecordTypeAndIdIsSameAsInEnteredRecord(recordType, id);

		checkUserIsAuthorisedToUpdate(userId, recordType, id);
		checkUserIsAuthorisedToStoreIncomingData(userId, recordType, spiderDataGroup);

		// validate (including protected data)
		// TODO: add validate here

		// merge possibly hidden data
		// TODO: merge incoming data with stored if user does not have right to
		// update some parts

		recordStorage.update(recordType, id, spiderDataGroup.toDataGroup());

		return createDataRecordContainingDataGroup(spiderDataGroup);
	}

	private void checkNoUpdateForAbstractRecordType(String recordType) {
		if ("true".equals(recordTypeDefinition.getFirstAtomicValueWithNameInData("abstract"))) {
			throw new MisuseException(
					"Data update on abstract recordType:" + recordType + " is not allowed");
		}
	}

	private DataGroup getRecordTypeDefinition(String recordType) {
		return recordStorage.read("recordType", recordType);
	}

	private void validateIncomingDataAsSpecifiedInMetadata() {
		String metadataId = recordTypeDefinition.getFirstAtomicValueWithNameInData("metadataId");
		DataGroup dataGroup = spiderDataGroup.toDataGroup();
		ValidationAnswer validationAnswer = dataValidator.validateData(metadataId, dataGroup);
		if (validationAnswer.dataIsInvalid()) {
			throw new DataException("Data is not valid: " + validationAnswer.getErrorMessages());
		}
	}

	private void checkRecordTypeAndIdIsSameAsInEnteredRecord(String recordType, String id) {
		checkValueIsSameAsInEnteredRecord(id, "id");
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

	private void checkUserIsAuthorisedToUpdate(String userId, String recordType, String id) {
		DataGroup recordRead = recordStorage.read(recordType, id);

		String accessType = "UPDATE";
		Set<String> recordCalculateKeys = keyCalculator.calculateKeys(accessType, recordType,
				recordRead);

		if (!authorization.isAuthorized(userId, recordCalculateKeys)) {
			throw new AuthorizationException(USER + userId
					+ " is not authorized to update record:" + id + "  of type:" + recordType);
		}
	}

	private void checkUserIsAuthorisedToStoreIncomingData(String userId, String recordType,
			SpiderDataGroup spiderDataGroup) {
		DataGroup incomingData = spiderDataGroup.toDataGroup();

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

	private SpiderDataRecord createDataRecordContainingDataGroup(SpiderDataGroup spiderDataGroup) {
		SpiderDataRecord spiderDataRecord = SpiderDataRecord.withSpiderDataGroup(spiderDataGroup);

		addLinks(spiderDataRecord);
		return spiderDataRecord;
	}

	private void addLinks(SpiderDataRecord spiderDataRecord) {
		// add links
		spiderDataRecord.addAction(Action.READ);
		spiderDataRecord.addAction(Action.UPDATE);
		spiderDataRecord.addAction(Action.DELETE);
		spiderDataRecord.addAction(Action.READ_INCOMING_LINKS);
	}

}
