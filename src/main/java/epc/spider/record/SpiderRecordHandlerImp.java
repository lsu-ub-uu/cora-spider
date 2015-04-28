package epc.spider.record;

import java.util.Collection;
import java.util.Set;

import epc.beefeater.Authorizator;
import epc.metadataformat.data.DataGroup;
import epc.spider.data.Action;
import epc.spider.data.SpiderDataAtomic;
import epc.spider.data.SpiderDataGroup;
import epc.spider.data.SpiderDataRecord;
import epc.spider.data.SpiderRecordList;
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
	public SpiderRecordList readRecordList(String userId, String recordType) {

		// calculate permissionKey
		String accessType = "READ";
		Set<String> recordCalculateKeys = keyCalculator
				.calculateKeysForList(accessType, recordType);
		if (!authorization.isAuthorized(userId, recordCalculateKeys)) {
			throw new AuthorizationException(USER + userId + " is not authorized to read records"
					+ "of type:" + recordType);
		}

		Collection<DataGroup> dataGroupList = recordStorage.readList(recordType);

		SpiderRecordList readRecordList = SpiderRecordList.withContainRecordsOfType(recordType);
		for (DataGroup dataGroup : dataGroupList) {
			SpiderDataGroup spiderDataGroup = SpiderDataGroup.fromDataGroup(dataGroup);
			SpiderDataRecord spiderDataRecord = SpiderDataRecord
					.withSpiderDataGroup(spiderDataGroup);
			// add links
			spiderDataRecord.addAction(Action.READ);
			spiderDataRecord.addAction(Action.UPDATE);
			spiderDataRecord.addAction(Action.DELETE);
			readRecordList.addRecord(spiderDataRecord);
		}
		readRecordList.setTotalNo(String.valueOf(dataGroupList.size()));
		readRecordList.setFromNo("0");
		readRecordList.setToNo(String.valueOf(dataGroupList.size()));

		return readRecordList;
	}

	@Override
	public SpiderDataRecord readRecord(String userId, String recordType, String recordId) {
		DataGroup recordRead = recordStorage.read(recordType, recordId);

		// calculate permissionKey
		String accessType = "READ";
		Set<String> recordCalculateKeys = keyCalculator.calculateKeys(accessType, recordType,
				recordRead);
		if (!authorization.isAuthorized(userId, recordCalculateKeys)) {
			throw new AuthorizationException(USER + userId + " is not authorized to read record:"
					+ recordId + " of type:" + recordType);
		}

		// filter data
		// TODO: filter hidden data if user does not have right to see it

		SpiderDataGroup spiderDataGroup = SpiderDataGroup.fromDataGroup(recordRead);
		// create record
		SpiderDataRecord spiderDataRecord = SpiderDataRecord.withSpiderDataGroup(spiderDataGroup);

		// add links
		spiderDataRecord.addAction(Action.READ);
		spiderDataRecord.addAction(Action.UPDATE);
		spiderDataRecord.addAction(Action.DELETE);
		return spiderDataRecord;
	}

	@Override
	public SpiderDataRecord createAndStoreRecord(String userId, String recordType,
			SpiderDataGroup spiderDataGroup) {

		// TODO: check incoming spiderRecord does not contain a recordInfo

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
		spiderDataGroup.addChild(recordInfo);

		// validate
		// TODO: add validate here

		DataGroup record = spiderDataGroup.toDataGroup();

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

		// create record
		SpiderDataRecord spiderDataRecord = SpiderDataRecord.withSpiderDataGroup(spiderDataGroup);

		// add links
		spiderDataRecord.addAction(Action.READ);
		spiderDataRecord.addAction(Action.UPDATE);
		spiderDataRecord.addAction(Action.DELETE);

		return spiderDataRecord;
	}

	@Override
	public void deleteRecord(String userId, String type, String id) {
		DataGroup readRecord = recordStorage.read(type, id);
		// calculate permissionKey
		String accessType = "DELETE";
		Set<String> recordCalculateKeys = keyCalculator.calculateKeys(accessType, type, readRecord);
		if (!authorization.isAuthorized(userId, recordCalculateKeys)) {
			throw new AuthorizationException(USER + userId + " is not authorized to delete record:"
					+ id + " of type:" + type);
		}
		recordStorage.deleteByTypeAndId(type, id);
	}

	@Override
	public SpiderDataRecord updateRecord(String userId, String type, String id,
			SpiderDataGroup spiderDataGroup) {
		SpiderDataGroup recordInfo = spiderDataGroup.extractGroup("recordInfo");
		// TODO: check entered type and id are the same as in the entered record
		String idFromRecord = recordInfo.extractAtomicValue("id");
		String typeFromRecord = recordInfo.extractAtomicValue("type");

		DataGroup recordRead = recordStorage.read(type, id);

		// calculate permissionKey
		String accessType = "UPDATE";
		Set<String> recordCalculateKeys = keyCalculator.calculateKeys(accessType, type, recordRead);

		if (!authorization.isAuthorized(userId, recordCalculateKeys)) {
			throw new AuthorizationException(USER + userId
					+ " is not authorized to update a record  of type:" + type);
		}

		// validate (including protected data)
		// TODO: add validate here

		// merge possibly hidden data
		// TODO: merge incoming data with stored if user does not have right to update some parts

		recordStorage.update(type, id, spiderDataGroup.toDataGroup());

		// create record
		SpiderDataRecord spiderDataRecord = SpiderDataRecord.withSpiderDataGroup(spiderDataGroup);

		// add links
		spiderDataRecord.addAction(Action.READ);
		spiderDataRecord.addAction(Action.UPDATE);
		spiderDataRecord.addAction(Action.DELETE);

		return spiderDataRecord;
	}

}
