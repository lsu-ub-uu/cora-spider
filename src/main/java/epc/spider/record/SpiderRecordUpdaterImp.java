package epc.spider.record;

import java.util.Set;

import epc.beefeater.Authorizator;
import epc.metadataformat.data.DataGroup;
import epc.metadataformat.validator.DataValidator;
import epc.metadataformat.validator.ValidationAnswer;
import epc.spider.data.Action;
import epc.spider.data.SpiderDataGroup;
import epc.spider.data.SpiderDataRecord;
import epc.spider.record.storage.RecordNotFoundException;
import epc.spider.record.storage.RecordStorage;

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
		try {
			return recordStorage.read("recordType", recordType);
		} catch (RecordNotFoundException e) {
			throw new DataException("recordType:" + recordType + " does not exist", e);
		}
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
	}

}
