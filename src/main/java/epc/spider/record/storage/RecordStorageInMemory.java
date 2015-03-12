package epc.spider.record.storage;

import java.util.HashMap;
import java.util.Map;

import epc.spider.data.SpiderDataGroup;

public class RecordStorageInMemory implements RecordStorage {
	private Map<String, Map<String, SpiderDataGroup>> records = new HashMap<>();

	public RecordStorageInMemory() {
		// Make it possible to use default empty record storage
	}

	public RecordStorageInMemory(Map<String, Map<String, SpiderDataGroup>> records) {
		throwErrorIfConstructorArgumentIsNull(records);
		this.records = records;
	}

	private void throwErrorIfConstructorArgumentIsNull(
			Map<String, Map<String, SpiderDataGroup>> records) {
		if (null == records) {
			throw new IllegalArgumentException("Records must not be null");
		}
	}

	@Override
	public void create(String recordType, String recordId, SpiderDataGroup dataGroup) {
		ensureRecordTypeStorageExists(recordType);
		records.get(recordType).put(recordId, dataGroup);
	}

	@Override
	public SpiderDataGroup read(String recordType, String recordId) {
		if (null == records.get(recordType)) {
			throw new RecordNotFoundException("No records exists with recordType: " + recordType);
		}
		if (null == records.get(recordType).get(recordId)) {
			throw new RecordNotFoundException("No records exists with recordId: " + recordId);
		}
		return records.get(recordType).get(recordId);
	}

	private void ensureRecordTypeStorageExists(String recordType) {
		if (null == records.get(recordType)) {
			records.put(recordType, new HashMap<String, SpiderDataGroup>());
		}
	}

}
