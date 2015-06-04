package epc.spider.record;

import java.util.Collection;
import java.util.Set;

import epc.beefeater.Authorizator;
import epc.metadataformat.data.DataGroup;
import epc.spider.data.Action;
import epc.spider.data.SpiderDataGroup;
import epc.spider.data.SpiderDataRecord;
import epc.spider.data.SpiderRecordList;
import epc.spider.record.storage.RecordStorage;

public final class SpiderRecordReaderImp implements SpiderRecordReader {
	private Authorizator authorization;
	private RecordStorage recordStorage;
	private PermissionKeyCalculator keyCalculator;

	public static SpiderRecordReaderImp usingAuthorizationAndRecordStorageAndKeyCalculator(
			Authorizator authorization, RecordStorage recordStorage,
			PermissionKeyCalculator keyCalculator) {
		return new SpiderRecordReaderImp(authorization, recordStorage, keyCalculator);
	}

	private SpiderRecordReaderImp(Authorizator authorization, RecordStorage recordStorage,
			PermissionKeyCalculator keyCalculator) {
		this.authorization = authorization;
		this.recordStorage = recordStorage;
		this.keyCalculator = keyCalculator;
	}

	@Override
	public SpiderDataRecord readRecord(String userId, String recordType, String recordId) {
		DataGroup recordRead = recordStorage.read(recordType, recordId);

		// calculate permissionKey
		String accessType = "READ";
		Set<String> recordCalculateKeys = keyCalculator.calculateKeys(accessType, recordType,
				recordRead);
		if (!authorization.isAuthorized(userId, recordCalculateKeys)) {
			throw new AuthorizationException("User:" + userId
					+ " is not authorized to read record:" + recordId + " of type:" + recordType);
		}

		// filter data
		// TODO: filter hidden data if user does not have right to see it

		return convertToSpiderDataGroup(recordRead);
	}

	@Override
	public SpiderRecordList readRecordList(String userId, String recordType) {

		// calculate permissionKey
		String accessType = "READ";
		Set<String> recordCalculateKeys = keyCalculator
				.calculateKeysForList(accessType, recordType);
		if (!authorization.isAuthorized(userId, recordCalculateKeys)) {
			throw new AuthorizationException("User:" + userId
					+ " is not authorized to read records" + "of type:" + recordType);
		}

		Collection<DataGroup> dataGroupList = recordStorage.readList(recordType);

		SpiderRecordList readRecordList = SpiderRecordList.withContainRecordsOfType(recordType);
		for (DataGroup dataGroup : dataGroupList) {
			SpiderDataRecord spiderDataRecord = convertToSpiderDataGroup(dataGroup);
			readRecordList.addRecord(spiderDataRecord);
		}
		readRecordList.setTotalNo(String.valueOf(dataGroupList.size()));
		readRecordList.setFromNo("0");
		readRecordList.setToNo(String.valueOf(dataGroupList.size()));

		return readRecordList;
	}

	private SpiderDataRecord convertToSpiderDataGroup(DataGroup dataGroup) {
		SpiderDataGroup spiderDataGroup = SpiderDataGroup.fromDataGroup(dataGroup);
		SpiderDataRecord spiderDataRecord = SpiderDataRecord.withSpiderDataGroup(spiderDataGroup);
		// add links
		spiderDataRecord.addAction(Action.READ);
		spiderDataRecord.addAction(Action.UPDATE);
		spiderDataRecord.addAction(Action.DELETE);
		return spiderDataRecord;
	}
}
