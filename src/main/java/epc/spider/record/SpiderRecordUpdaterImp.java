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
		DataGroup recordTypeDataGroup = getRecordType(recordType);
		if ("true".equals(recordTypeDataGroup.getFirstAtomicValueWithDataId("abstract"))) {
			throw new MisuseException(
					"Data update on abstract recordType:" + recordType + " is not allowed");
		}
		DataGroup record = spiderDataGroup.toDataGroup();

		String metadataId = recordTypeDataGroup.getFirstAtomicValueWithDataId("metadataId");
		ValidationAnswer validationAnswer = dataValidator.validateData(metadataId, record);
		if (validationAnswer.dataIsInvalid()) {
			throw new DataException("Data is not valid: " + validationAnswer.getErrorMessages());
		}

		SpiderDataGroup recordInfo = spiderDataGroup.extractGroup(RECORD_INFO);
		// TODO: check entered type and id are the same as in the entered record
		String idFromRecord = recordInfo.extractAtomicValue("id");

		if (!id.equals(idFromRecord)) {
			throw new DataException("Id in data(" + idFromRecord + ") does not match entered id("
					+ id + ")");
		}
		String typeFromRecord = recordInfo.extractAtomicValue("type");
		if (!recordType.equals(typeFromRecord)) {
			throw new DataException("Type in data(" + typeFromRecord
					+ ") does not match entered type(" + recordType + ")");
		}

		DataGroup recordRead = recordStorage.read(recordType, id);

		// calculate permissionKey
		String accessType = "UPDATE";
		Set<String> recordCalculateKeys = keyCalculator.calculateKeys(accessType, recordType,
				recordRead);

		if (!authorization.isAuthorized(userId, recordCalculateKeys)) {
			throw new AuthorizationException(USER + userId
					+ " is not authorized to update a record  of type:" + recordType);
		}

		// validate (including protected data)
		// TODO: add validate here

		// merge possibly hidden data
		// TODO: merge incoming data with stored if user does not have right to update some parts

		recordStorage.update(recordType, id, spiderDataGroup.toDataGroup());

		// create record
		SpiderDataRecord spiderDataRecord = SpiderDataRecord.withSpiderDataGroup(spiderDataGroup);

		// add links
		spiderDataRecord.addAction(Action.READ);
		spiderDataRecord.addAction(Action.UPDATE);
		spiderDataRecord.addAction(Action.DELETE);

		return spiderDataRecord;
	}

	private DataGroup getRecordType(String recordType) {
		try {
			return recordStorage.read("recordType", recordType);
		} catch (RecordNotFoundException e) {
			throw new DataException("recordType:" + recordType + " does not exist", e);
		}
	}
}
