package epc.spider.record;

import java.util.Set;

import epc.beefeater.Authorizator;
import epc.metadataformat.data.DataGroup;
import epc.spider.data.Action;
import epc.spider.data.SpiderDataAtomic;
import epc.spider.data.SpiderDataGroup;
import epc.spider.record.storage.RecordIdGenerator;
import epc.spider.record.storage.RecordStorage;

public final class SpiderRecordHandlerImp implements SpiderRecordHandler {

	private static final String USER = "User:";
	private RecordStorage recordStorage;
	private Authorizator authorization;
	private RecordIdGenerator idGenerator;
	private PermissionKeyCalculator keyCalculator;

	public static SpiderRecordHandlerImp usingAuthorizationAndRecordStorageAndIdGeneratorAndKeyCalculator(
			Authorizator authorization, RecordStorage recordStorage, RecordIdGenerator idGenerator,
			PermissionKeyCalculator keyCalculator) {
		return new SpiderRecordHandlerImp(authorization, recordStorage, idGenerator, keyCalculator);
	}

	private SpiderRecordHandlerImp(Authorizator authorization, RecordStorage recordStorage,
			RecordIdGenerator idGenerator, PermissionKeyCalculator keyCalculator) {
		this.authorization = authorization;
		this.recordStorage = recordStorage;
		this.idGenerator = idGenerator;
		this.keyCalculator = keyCalculator;

	}

	@Override
	public SpiderDataGroup readRecord(String userId, String recordType, String recordId) {
		DataGroup readRecord = recordStorage.read(recordType, recordId);

		// calculate permissionKey
		String accessType = "READ";
		Set<String> recordCalculateKeys = keyCalculator.calculateKeys(accessType, recordType,
				readRecord);
		if (!authorization.isAuthorized(userId, recordCalculateKeys)) {
			throw new AuthorizationException(USER + userId
					+ " is not authorized to read record:" + recordId + " of type:" + recordType);
		}

		SpiderDataGroup record = SpiderDataGroup.fromDataGroup(readRecord);
		// add links
		record.addAction(Action.READ);
		record.addAction(Action.UPDATE);
		record.addAction(Action.DELETE);
		return record;
	}

	@Override
	public SpiderDataGroup createAndStoreRecord(String userId, String recordType,
			SpiderDataGroup spiderRecord) {

		SpiderDataGroup recordInfo = SpiderDataGroup.withDataId("recordInfo");
		// id
		String id = idGenerator.getIdForType(recordType);
		SpiderDataAtomic idData = SpiderDataAtomic.withDataIdAndValue("id", id);
		recordInfo.addChild(idData);
		recordInfo.addChild(SpiderDataAtomic.withDataIdAndValue("type", recordType));
		recordInfo.addChild(SpiderDataAtomic.withDataIdAndValue("createdBy", userId));

		// set more stuff, user, tscreated, status (created, updated, deleted, etc), published
		// (true, false)
		// set owning organisation

		// set recordInfo in record
		spiderRecord.addChild(recordInfo);

		// validate
		// TODO: add validate here

		DataGroup record = spiderRecord.toDataGroup();

		// calculate permissionKey
		String accessType = "CREATE";
		Set<String> recordCalculateKeys = keyCalculator.calculateKeys(accessType, recordType,
				record);

		if (!authorization.isAuthorized(userId, recordCalculateKeys)) {
			throw new AuthorizationException(USER + userId
					+ " is not authorized to create a record  of type:" + recordType);
		}

		// send to storage
		recordStorage.create(recordType, id, record);

		// add links
		spiderRecord.addAction(Action.READ);
		spiderRecord.addAction(Action.UPDATE);
		spiderRecord.addAction(Action.DELETE);

		return spiderRecord;
	}

	@Override
	public void deleteRecord(String userId, String type, String id) {
		DataGroup readRecord = recordStorage.read(type, id);
		// calculate permissionKey
		String accessType = "DELETE";
		Set<String> recordCalculateKeys = keyCalculator.calculateKeys(accessType, type, readRecord);
		if (!authorization.isAuthorized(userId, recordCalculateKeys)) {
			throw new AuthorizationException(USER + userId
					+ " is not authorized to delete record:" + id + " of type:" + type);
		}
		recordStorage.deleteByTypeAndId(type, id);
	}
}
