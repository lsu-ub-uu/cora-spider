package epc.spider.record;

import java.util.Set;

import epc.beefeater.Authorizator;
import epc.metadataformat.data.DataGroup;
import epc.spider.record.storage.RecordNotFoundException;
import epc.spider.record.storage.RecordStorage;

public final class SpiderRecordDeleterImp implements SpiderRecordDeleter {
	private RecordStorage recordStorage;
	private Authorizator authorization;
	private PermissionKeyCalculator keyCalculator;

	public static SpiderRecordDeleterImp usingAuthorizationAndRecordStorageAndKeyCalculator(
			Authorizator authorization, RecordStorage recordStorage,
			PermissionKeyCalculator keyCalculator) {
		return new SpiderRecordDeleterImp(authorization, recordStorage, keyCalculator);
	}

	private SpiderRecordDeleterImp(Authorizator authorization, RecordStorage recordStorage,
			PermissionKeyCalculator keyCalculator) {
		this.authorization = authorization;
		this.recordStorage = recordStorage;
		this.keyCalculator = keyCalculator;

	}

	@Override
	public void deleteRecord(String userId, String recordType, String recordId) {
		DataGroup recordTypeDataGroup = getRecordType(recordType);
		if ("true".equals(recordTypeDataGroup.getFirstAtomicValueWithNameInData("abstract"))) {
			throw new MisuseException("Deleting record: " + recordId
					+ " on the abstract recordType:" + recordType + " is not allowed");
		}
		DataGroup readRecord = recordStorage.read(recordType, recordId);
		// calculate permissionKey
		String accessType = "DELETE";
		Set<String> recordCalculateKeys = keyCalculator.calculateKeys(accessType, recordType,
				readRecord);
		if (!authorization.isAuthorized(userId, recordCalculateKeys)) {
			throw new AuthorizationException("User:" + userId
					+ " is not authorized to delete record:" + recordId + " of type:" + recordType);
		}
		recordStorage.deleteByTypeAndId(recordType, recordId);
	}

	private DataGroup getRecordType(String recordType) {
		try {
			return recordStorage.read("recordType", recordType);
		} catch (RecordNotFoundException e) {
			throw new DataException("recordType:" + recordType + " does not exist", e);
		}
	}
}
