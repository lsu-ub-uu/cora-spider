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
	private SpiderRecordList readRecordList;

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
			throw new AuthorizationException("User:" + userId + " is not authorized to read record:"
					+ recordId + " of type:" + recordType);
		}

		// filter data
		// TODO: filter hidden data if user does not have right to see it

		return convertToSpiderDataGroup(recordRead);
	}

	@Override
	public SpiderRecordList readRecordList(String userId, String recordType) {

		// calculate permissionKey
		String accessType = "READ";
		Set<String> recordCalculateKeys = keyCalculator.calculateKeysForList(accessType,
				recordType);
		if (!authorization.isAuthorized(userId, recordCalculateKeys)) {
			throw new AuthorizationException("User:" + userId + " is not authorized to read records"
					+ "of type:" + recordType);
		}
		readRecordList = SpiderRecordList.withContainRecordsOfType(recordType);

		DataGroup recordTypeDataGroup = recordStorage.read("recordType", recordType);
		String abstractString = recordTypeDataGroup.getFirstAtomicValueWithDataId("abstract");
		if ("true".equals(abstractString)) {
			// find child recordTypes
			Collection<DataGroup> recordTypes = recordStorage.readList("recordType");
			for (DataGroup recordTypePossibleChild : recordTypes) {
				if (recordTypePossibleChild.containsChildWithDataId("parentId")) {
					String parentId = recordTypePossibleChild
							.getFirstAtomicValueWithDataId("parentId");
					if (parentId.equals(recordType)) {
						// get this recordTypes data from storage
						String childRecordType = recordTypePossibleChild
								.getFirstGroupWithDataId("recordInfo")
								.getFirstAtomicValueWithDataId("id");
						readRecordsOfSpecifiedRecordTypeAndAddToReadRecordList(childRecordType);
					}
				}
			}
		} else {

			readRecordsOfSpecifiedRecordTypeAndAddToReadRecordList(recordType);
		}
		readRecordList.setTotalNo(String.valueOf(readRecordList.getRecords().size()));
		readRecordList.setFromNo("0");
		readRecordList.setToNo(String.valueOf(readRecordList.getRecords().size() - 1));

		return readRecordList;
	}

	private void readRecordsOfSpecifiedRecordTypeAndAddToReadRecordList(String recordType) {
		Collection<DataGroup> dataGroupList = recordStorage.readList(recordType);
		for (DataGroup dataGroup : dataGroupList) {
			SpiderDataRecord spiderDataRecord = convertToSpiderDataGroup(dataGroup);
			readRecordList.addRecord(spiderDataRecord);
		}
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
