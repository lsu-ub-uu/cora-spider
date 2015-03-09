package epc.spider.record;

import java.util.Set;

import epc.beefeater.AuthorizationInputBoundary;
import epc.metadataformat.data.DataAtomic;
import epc.metadataformat.data.DataGroup;
import epc.spider.record.storage.RecordIdGenerator;
import epc.spider.record.storage.RecordStorage;

public class SpiderRecordHandlerImp implements SpiderRecordHandler {

	private RecordStorage recordStorage;
	private AuthorizationInputBoundary authorization;
	private RecordIdGenerator idGenerator;
	private PermissionKeyCalculator keyCalculator;

	public static SpiderRecordHandlerImp usingAuthorizationAndRecordStorageAndIdGeneratorAndKeyCalculator(
			AuthorizationInputBoundary authorization, RecordStorage recordStorage,
			RecordIdGenerator idGenerator, PermissionKeyCalculator keyCalculator) {
		return new SpiderRecordHandlerImp(authorization, recordStorage, idGenerator, keyCalculator);
	}

	private SpiderRecordHandlerImp(AuthorizationInputBoundary authorization,
			RecordStorage recordStorage, RecordIdGenerator idGenerator,
			PermissionKeyCalculator keyCalculator) {
		this.authorization = authorization;
		this.recordStorage = recordStorage;
		this.idGenerator = idGenerator;
		this.keyCalculator = keyCalculator;

	}

	@Override
	public DataGroup readRecord(String userId, String recordType, String recordId) {
		DataGroup readRecord = recordStorage.read(recordType, recordId);

		// calculate permissionKey
		String accessType = "READ";
		Set<String> recordCalculateKeys = keyCalculator.calculateKeys(accessType, recordType,
				readRecord);
		if (!authorization.isAuthorized(userId, recordCalculateKeys)) {
			throw new AuthorizationException("User:" + userId
					+ " is not authorized to read record:" + recordId + " of type:" + recordType);
		}
		return readRecord;
	}

	@Override
	public DataGroup createAndStoreRecord(String userId, String recordType, DataGroup record) {

		DataGroup recordInfo = DataGroup.withDataId("recordInfo");
		// id
		String id = idGenerator.getIdForType(recordType);
		DataAtomic idData = DataAtomic.withDataIdAndValue("id", id);
		recordInfo.addChild(idData);

		// set more stuff, user, tscreated
		// set owning organisation

		// set recordInfo in record
		record.addChild(recordInfo);

		// validate

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

		return record;
	}

}
