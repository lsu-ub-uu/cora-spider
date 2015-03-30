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
			throw new AuthorizationException("User:" + userId
					+ " is not authorized to read record:" + recordId + " of type:" + recordType);
		}

		SpiderDataGroup record = SpiderDataGroup.fromDataGroup(readRecord);
		// add links
		record.addAction(Action.READ);
		record.addAction(Action.UPDATE);
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
		recordInfo.addChild(SpiderDataAtomic.withDataIdAndValue("recordType", recordType));
		recordInfo.addChild(SpiderDataAtomic.withDataIdAndValue("createdBy", userId));

		// set more stuff, user, tscreated
		// set owning organisation

		// set recordInfo in record
		spiderRecord.addChild(recordInfo);

		// validate

		DataGroup record = spiderRecord.toDataGroup();

		// calculate permissionKey
		String accessType = "CREATE";
		Set<String> recordCalculateKeys = keyCalculator.calculateKeys(accessType, recordType,
				record);

		if (!authorization.isAuthorized(userId, recordCalculateKeys)) {
			throw new AuthorizationException("User:" + userId
					+ " is not authorized to create a record  of type:" + recordType);
		}

		// send to storage
		recordStorage.create(recordType, id, record);

		// add links

		return spiderRecord;
	}
}
