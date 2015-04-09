package epc.spider.record.storage;

import java.util.HashMap;
import java.util.Map;

import epc.metadataformat.data.DataGroup;
import epc.spider.data.SpiderDataGroup;

public class RecordStorageInMemory implements RecordStorage {
	private Map<String, Map<String, DataGroup>> records = new HashMap<>();

	public RecordStorageInMemory() {
		// Make it possible to use default empty record storage
	}

	public RecordStorageInMemory(Map<String, Map<String, DataGroup>> records) {
		throwErrorIfConstructorArgumentIsNull(records);
		this.records = records;
	}

	private void throwErrorIfConstructorArgumentIsNull(Map<String, Map<String, DataGroup>> records) {
		if (null == records) {
			throw new IllegalArgumentException("Records must not be null");
		}
	}

	@Override
	public void create(String recordType, String recordId, DataGroup record) {
		ensureRecordTypeStorageExists(recordType);
		DataGroup recordIndependentOfEnteredRecord = SpiderDataGroup.fromDataGroup(record)
				.toDataGroup();
		records.get(recordType).put(recordId, recordIndependentOfEnteredRecord);
	}

	private void ensureRecordTypeStorageExists(String recordType) {
		if (null == records.get(recordType)) {
			records.put(recordType, new HashMap<String, DataGroup>());
		}
	}

	@Override
	public DataGroup read(String recordType, String recordId) {
		ensureRecordExists(recordType, recordId);
		return records.get(recordType).get(recordId);
	}

	private void ensureRecordExists(String recordType, String recordId) {
		if (null == records.get(recordType)) {
			throw new RecordNotFoundException("No records exists with recordType: " + recordType);
		}
		if (null == records.get(recordType).get(recordId)) {
			throw new RecordNotFoundException("No record exists with recordId: " + recordId);
		}
	}

	@Override
	public void deleteByTypeAndId(String recordType, String recordId) {
		ensureRecordExists(recordType, recordId);
		records.get(recordType).remove(recordId);
	}

	@Override
	public void update(String type, String id, DataGroup record) {
		ensureRecordExists(type, id);
		DataGroup recordIndependentOfEnteredRecord = SpiderDataGroup.fromDataGroup(record)
				.toDataGroup();
		records.get(type).put(id, recordIndependentOfEnteredRecord);
	}

}
