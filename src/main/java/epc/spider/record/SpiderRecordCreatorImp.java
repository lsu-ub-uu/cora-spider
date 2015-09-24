package epc.spider.record;

import java.util.Set;

import epc.beefeater.Authorizator;
import epc.metadataformat.data.DataGroup;
import epc.metadataformat.validator.DataValidator;
import epc.metadataformat.validator.ValidationAnswer;
import epc.spider.data.Action;
import epc.spider.data.SpiderDataAtomic;
import epc.spider.data.SpiderDataGroup;
import epc.spider.data.SpiderDataRecord;
import epc.spider.record.storage.RecordIdGenerator;
import epc.spider.record.storage.RecordNotFoundException;
import epc.spider.record.storage.RecordStorage;

public final class SpiderRecordCreatorImp implements SpiderRecordCreator {
	private static final String RECORD_INFO = "recordInfo";
	private static final String USER = "User:";
	private RecordStorage recordStorage;
	private Authorizator authorization;
	private RecordIdGenerator idGenerator;
	private PermissionKeyCalculator keyCalculator;
	private DataValidator dataValidator;

	public static SpiderRecordCreatorImp usingAuthorizationAndDataValidatorAndRecordStorageAndIdGeneratorAndKeyCalculator(
			Authorizator authorization, DataValidator dataValidator, RecordStorage recordStorage,
			RecordIdGenerator idGenerator, PermissionKeyCalculator keyCalculator) {
		return new SpiderRecordCreatorImp(authorization, dataValidator, recordStorage, idGenerator,
				keyCalculator);
	}

	private SpiderRecordCreatorImp(Authorizator authorization, DataValidator dataValidator,
			RecordStorage recordStorage, RecordIdGenerator idGenerator,
			PermissionKeyCalculator keyCalculator) {
		this.authorization = authorization;
		this.dataValidator = dataValidator;
		this.recordStorage = recordStorage;
		this.idGenerator = idGenerator;
		this.keyCalculator = keyCalculator;

	}

	@Override
	public SpiderDataRecord createAndStoreRecord(String userId, String recordType,
			SpiderDataGroup spiderDataGroup) {

		DataGroup recordTypeDataGroup = getRecordType(recordType);
		validateRecord(spiderDataGroup, recordTypeDataGroup);

		SpiderDataGroup recordInfo = fetchOrCreateRecordInfo(recordType, spiderDataGroup, recordTypeDataGroup);
		recordInfo.addChild(SpiderDataAtomic.withDataIdAndValue("type", recordType));
		recordInfo.addChild(SpiderDataAtomic.withDataIdAndValue("createdBy", userId));

		// set more stuff, user, tscreated, status (created, updated, deleted, etc), published
		// (true, false)
		// set owning organisation

		storeCreatedRecordIfAuthorised(userId, recordType, spiderDataGroup, recordInfo);

		// create record
		SpiderDataRecord spiderDataRecord = SpiderDataRecord.withSpiderDataGroup(spiderDataGroup);
		addLinks(spiderDataRecord);

		return spiderDataRecord;
	}


	private void validateRecord(SpiderDataGroup spiderDataGroup, DataGroup recordTypeDataGroup) {
		DataGroup record = spiderDataGroup.toDataGroup();

		String metadataId = recordTypeDataGroup.getFirstAtomicValueWithDataId("newMetadataId");
		ValidationAnswer validationAnswer = dataValidator.validateData(metadataId, record);
		if (validationAnswer.dataIsInvalid()) {
			throw new DataException("Data is not valid: " + validationAnswer.getErrorMessages());
		}
	}

	private SpiderDataGroup fetchOrCreateRecordInfo(String recordType, SpiderDataGroup spiderDataGroup, DataGroup recordTypeDataGroup) {
		SpiderDataGroup recordInfo;
		if (shouldAutoGenerateId(recordTypeDataGroup)) {
			recordInfo = createRecordInfo(recordType);
			spiderDataGroup.addChild(recordInfo);
		} else {
			recordInfo = spiderDataGroup.extractGroup(RECORD_INFO);
		}
		return recordInfo;
	}

	private boolean shouldAutoGenerateId(DataGroup recordTypeDataGroup) {
		String userSuppliedId = recordTypeDataGroup.getFirstAtomicValueWithDataId("userSuppliedId");
		return "false".equals(userSuppliedId);
	}

	private SpiderDataGroup createRecordInfo(String recordType) {
		SpiderDataGroup recordInfo = SpiderDataGroup.withDataId(RECORD_INFO);
		recordInfo.addChild(SpiderDataAtomic.withDataIdAndValue("id", idGenerator.getIdForType(recordType)));
		return recordInfo;
	}

	private void storeCreatedRecordIfAuthorised(String userId, String recordType, SpiderDataGroup spiderDataGroup, SpiderDataGroup recordInfo) {
		DataGroup record = spiderDataGroup.toDataGroup();

		checkUserIsAuthorisedToCreate(userId, recordType, record);

		// send to storage
		String id = recordInfo.extractAtomicValue("id");
		recordStorage.create(recordType, id, record);
	}

	private void checkUserIsAuthorisedToCreate(String userId, String recordType, DataGroup record) {
		// calculate permissionKey
		String accessType = "CREATE";
		Set<String> recordCalculateKeys = keyCalculator.calculateKeys(accessType, recordType,
				record);

		if (!authorization.isAuthorized(userId, recordCalculateKeys)) {
			throw new AuthorizationException(USER + userId
					+ " is not authorized to create a record  of type:" + recordType);
		}
	}

	private void addLinks(SpiderDataRecord spiderDataRecord) {
		// add links
		spiderDataRecord.addAction(Action.READ);
		spiderDataRecord.addAction(Action.UPDATE);
		spiderDataRecord.addAction(Action.DELETE);
	}

	private DataGroup getRecordType(String recordType) {
		try {
			return recordStorage.read("recordType", recordType);
		} catch (RecordNotFoundException e) {
			throw new DataException("recordType:" + recordType + " does not exist", e);
		}
	}
}
