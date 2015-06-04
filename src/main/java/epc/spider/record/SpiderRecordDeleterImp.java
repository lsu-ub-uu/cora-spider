package epc.spider.record;

import java.util.Set;

import epc.beefeater.Authorizator;
import epc.metadataformat.data.DataGroup;
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
	public void deleteRecord(String userId, String type, String id) {
		DataGroup readRecord = recordStorage.read(type, id);
		// calculate permissionKey
		String accessType = "DELETE";
		Set<String> recordCalculateKeys = keyCalculator.calculateKeys(accessType, type, readRecord);
		if (!authorization.isAuthorized(userId, recordCalculateKeys)) {
			throw new AuthorizationException("User:" + userId
					+ " is not authorized to delete record:" + id + " of type:" + type);
		}
		recordStorage.deleteByTypeAndId(type, id);
	}
}
