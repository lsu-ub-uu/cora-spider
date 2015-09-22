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
		checkNoRecordInfoForAutoGeneratedData(spiderDataGroup, recordTypeDataGroup);
		setDataInRecordInfo(userId, recordType, spiderDataGroup, recordTypeDataGroup);
		validateSubmittedDataGroup(spiderDataGroup, recordTypeDataGroup);
		DataGroup dataGroup = spiderDataGroup.toDataGroup();
		checkUserIsAuthorizedToCreateSubmittedData(userId, recordType, dataGroup);
		storeDataGroup(recordType, dataGroup);
		return createDataRecordFromDataGroup(spiderDataGroup);
	}

	private void checkNoRecordInfoForAutoGeneratedData(SpiderDataGroup spiderDataGroup,
			DataGroup recordTypeDataGroup) {
		if (shouldAutogenerateId(recordTypeDataGroup)
				&& spiderDataGroup.containsChildWithDataId("recordInfo")) {
			throw new DataException(
					"Data is not valid: " + "Found too many data children with dataId: RecordInfo");
		}
	}

	private DataGroup getRecordType(String recordType) {
		try {
			return recordStorage.read("recordType", recordType);
		} catch (RecordNotFoundException e) {
			throw new DataException("recordType:" + recordType + " does not exist", e);
		}
	}

	private void validateSubmittedDataGroup(SpiderDataGroup spiderDataGroup,
			DataGroup recordTypeDataGroup) {
		DataGroup record = spiderDataGroup.toDataGroup();

		String metadataId = recordTypeDataGroup.getFirstAtomicValueWithDataId("newMetadataId");
		ValidationAnswer validationAnswer = dataValidator.validateData(metadataId, record);
		if (validationAnswer.dataIsInvalid()) {
			throw new DataException("Data is not valid: " + validationAnswer.getErrorMessages());
		}
	}

	private void setDataInRecordInfo(String userId, String recordType,
			SpiderDataGroup spiderDataGroup, DataGroup recordTypeDataGroup) {
		if (shouldAutogenerateId(recordTypeDataGroup)) {
			createRecordInfo(recordType, spiderDataGroup);
		}
		addDataToRecordInfo(userId, recordType, spiderDataGroup);
	}

	private void addDataToRecordInfo(String userId, String recordType,
			SpiderDataGroup spiderDataGroup) {
		SpiderDataGroup recordInfo = spiderDataGroup.extractGroup(RECORD_INFO);
		recordInfo.addChild(SpiderDataAtomic.withDataIdAndValue("type", recordType));
		recordInfo.addChild(SpiderDataAtomic.withDataIdAndValue("createdBy", userId));

		/**
		 * set more stuff, user, tscreated, status (created, updated, deleted,
		 * etc), published (true, false) set owning organisation
		 */
	}

	private boolean shouldAutogenerateId(DataGroup recordTypeDataGroup) {
		String userSuppliedId = recordTypeDataGroup.getFirstAtomicValueWithDataId("userSuppliedId");
		return "false".equals(userSuppliedId);
	}

	private void createRecordInfo(String recordType, SpiderDataGroup spiderDataGroup) {
		String id2 = idGenerator.getIdForType(recordType);
		SpiderDataGroup recordInfo2 = SpiderDataGroup.withDataId(RECORD_INFO);
		recordInfo2.addChild(SpiderDataAtomic.withDataIdAndValue("id", id2));
		spiderDataGroup.addChild(recordInfo2);
	}

	private void checkUserIsAuthorizedToCreateSubmittedData(String userId, String recordType,
			DataGroup dataGroup) {
		String accessType = "CREATE";
		Set<String> recordCalculateKeys = keyCalculator.calculateKeys(accessType, recordType,
				dataGroup);

		if (!authorization.isAuthorized(userId, recordCalculateKeys)) {
			throw new AuthorizationException(
					USER + userId + " is not authorized to create a record  of type:" + recordType);
		}
	}

	private void storeDataGroup(String recordType, DataGroup dataGroup) {
		DataGroup recordInfoDataGroup = dataGroup.getFirstGroupWithDataId(RECORD_INFO);
		String id = recordInfoDataGroup.getFirstAtomicValueWithDataId("id");
		recordStorage.create(recordType, id, dataGroup);
	}

	private SpiderDataRecord createDataRecordFromDataGroup(SpiderDataGroup spiderDataGroup) {
		SpiderDataRecord spiderDataRecord = SpiderDataRecord.withSpiderDataGroup(spiderDataGroup);
		addActionsToRecord(spiderDataRecord);
		return spiderDataRecord;
	}

	private void addActionsToRecord(SpiderDataRecord spiderDataRecord) {
		spiderDataRecord.addAction(Action.READ);
		spiderDataRecord.addAction(Action.UPDATE);
		spiderDataRecord.addAction(Action.DELETE);
	}

}
