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
	private DataGroup recordTypeDefinition;
	private SpiderDataGroup spiderDataGroup;

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

		this.spiderDataGroup = spiderDataGroup;
		recordTypeDefinition = getRecordTypeDefinition(recordType);

		checkNoCreateForAbstractRecordType(recordType);
		validateDataInRecordAsSpecifiedInMetadata();

		ensureCompleteRecordInfo(userId, recordType);

		// set more stuff, user, tscreated, status (created, updated, deleted,
		// etc), published
		// (true, false)
		// set owning organisation

		DataGroup record = spiderDataGroup.toDataGroup();

		checkUserIsAuthorisedToCreateIncomingData(userId, recordType, record);

		// send to storage
		String id = extractIdFromData();
		recordStorage.create(recordType, id, record);

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
				.getFirstAtomicValueWithDataId("abstract");
		return "true".equals(abstractInRecordTypeDefinition);
	}

	private void validateDataInRecordAsSpecifiedInMetadata() {
		DataGroup record = spiderDataGroup.toDataGroup();

		String metadataId = recordTypeDefinition.getFirstAtomicValueWithDataId("newMetadataId");
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
		String userSuppliedId = recordTypeDataGroup.getFirstAtomicValueWithDataId("userSuppliedId");
		return "false".equals(userSuppliedId);
	}

	private void addRecordInfoToDataGroup(String recordType) {
		SpiderDataGroup recordInfo = createRecordInfo(recordType);
		spiderDataGroup.addChild(recordInfo);
	}

	private void addUserAndTypeToRecordInfo(String userId, String recordType) {
		SpiderDataGroup recordInfo = spiderDataGroup.extractGroup(RECORD_INFO);
		recordInfo.addChild(SpiderDataAtomic.withDataIdAndValue("type", recordType));
		recordInfo.addChild(SpiderDataAtomic.withDataIdAndValue("createdBy", userId));
	}

	private SpiderDataGroup createRecordInfo(String recordType) {
		SpiderDataGroup recordInfo = SpiderDataGroup.withDataId(RECORD_INFO);
		recordInfo.addChild(
				SpiderDataAtomic.withDataIdAndValue("id", idGenerator.getIdForType(recordType)));
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
		// add links
		spiderDataRecord.addAction(Action.READ);
		spiderDataRecord.addAction(Action.UPDATE);
		spiderDataRecord.addAction(Action.DELETE);
	}

}
