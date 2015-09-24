package epc.spider.record;

import java.util.Collection;
import java.util.Set;

import epc.beefeater.Authorizator;
import epc.metadataformat.data.DataGroup;
import epc.spider.data.Action;
import epc.spider.data.SpiderDataGroup;
import epc.spider.data.SpiderDataRecord;
import epc.spider.data.SpiderRecordList;
import epc.spider.record.storage.RecordNotFoundException;
import epc.spider.record.storage.RecordStorage;

public final class SpiderRecordReaderImp implements SpiderRecordReader {
	private static final String RECORD_TYPE = "recordType";
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
		DataGroup recordTypeDataGroup = getRecordType(recordType);
		if ("true".equals(recordTypeDataGroup.getFirstAtomicValueWithDataId("abstract"))) {
			throw new MisuseException("Reading record: " + recordId + " on the abstract recordType:"
					+ recordType + " is not allowed");
		}
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

	private DataGroup getRecordType(String recordType) {
		try {
			return recordStorage.read(RECORD_TYPE, recordType);
		} catch (RecordNotFoundException e) {
			throw new DataException("recordType:" + recordType + " does not exist", e);
		}
	}

	@Override
	public SpiderRecordList readRecordList(String userId, String recordType) {

		checkUserIsAuthorizedToReadListRecordType(userId, recordType);
		readRecordList = SpiderRecordList.withContainRecordsOfType(recordType);

		readRecordsOfType(recordType);
		setFromToInReadRecordList();

		return readRecordList;
	}

	private void checkUserIsAuthorizedToReadListRecordType(String userId, String recordType) {
		// calculate permissionKey
		String accessType = "READ";
		Set<String> recordCalculateKeys = keyCalculator.calculateKeysForList(accessType,
				recordType);
		if (!authorization.isAuthorized(userId, recordCalculateKeys)) {
			throw new AuthorizationException("User:" + userId + " is not authorized to read records"
					+ "of type:" + recordType);
		}
	}

	private void readRecordsOfType(String recordType) {
		if (recordTypeIsAbstract(recordType)) {
			addChildrenOfAbstractTypeToReadRecordList(recordType);
		} else {
			readRecordsOfSpecifiedRecordTypeAndAddToReadRecordList(recordType);
		}
	}

	private boolean recordTypeIsAbstract(String recordType) {
		DataGroup recordTypeDataGroup = recordStorage.read(RECORD_TYPE, recordType);
		String abstractString = recordTypeDataGroup.getFirstAtomicValueWithDataId("abstract");
		return "true".equals(abstractString);
	}

	private void addChildrenOfAbstractTypeToReadRecordList(String abstractRecordType) {
		// find child recordTypes
		Collection<DataGroup> recordTypes = recordStorage.readList(RECORD_TYPE);

		for (DataGroup recordTypePossibleChild : recordTypes) {
			if (isChildOfAbstractRecordType(abstractRecordType, recordTypePossibleChild)) {
				addChildToReadRecordList(recordTypePossibleChild);
			}
		}
	}

	private boolean isChildOfAbstractRecordType(String abstractRecordType,
			DataGroup recordTypePossibleChild) {
		String parentId = "parentId";
		if (recordTypePossibleChild.containsChildWithDataId(parentId)) {
			String parentIdValue = recordTypePossibleChild.getFirstAtomicValueWithDataId(parentId);
			if (parentIdValue.equals(abstractRecordType)) {
				return true;
			}
		}
		return false;
	}

	private void addChildToReadRecordList(DataGroup recordTypePossibleChild) {
		// get this recordTypes data from storage
		String childRecordType = recordTypePossibleChild.getFirstGroupWithDataId("recordInfo")
				.getFirstAtomicValueWithDataId("id");
		readRecordsOfSpecifiedRecordTypeAndAddToReadRecordList(childRecordType);
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

	private void setFromToInReadRecordList() {
		readRecordList.setTotalNo(String.valueOf(readRecordList.getRecords().size()));
		readRecordList.setFromNo("0");
		readRecordList.setToNo(String.valueOf(readRecordList.getRecords().size() - 1));
	}
}
