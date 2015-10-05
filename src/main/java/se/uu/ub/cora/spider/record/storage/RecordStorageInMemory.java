package se.uu.ub.cora.spider.record.storage;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import se.uu.ub.cora.metadataformat.data.DataGroup;
import se.uu.ub.cora.metadataformat.metadata.MetadataTypes;
import se.uu.ub.cora.metadataformat.storage.MetadataStorage;
import se.uu.ub.cora.spider.data.SpiderDataGroup;

public class RecordStorageInMemory implements RecordStorage, MetadataStorage {
	private Map<String, Map<String, DataGroup>> records = new HashMap<>();

	public RecordStorageInMemory() {
		// Make it possible to use default empty record storage
	}

	public RecordStorageInMemory(Map<String, Map<String, DataGroup>> records) {
		throwErrorIfConstructorArgumentIsNull(records);
		this.records = records;
	}

	private void throwErrorIfConstructorArgumentIsNull(
			Map<String, Map<String, DataGroup>> records) {
		if (null == records) {
			throw new IllegalArgumentException("Records must not be null");
		}
	}

	@Override
	public void create(String recordType, String recordId, DataGroup record) {
		ensureRecordTypeStorageExists(recordType);
		checkNoConflictOnRecordId(recordType, recordId);
		DataGroup recordIndependentOfEnteredRecord = createIndependentCopy(record);
		storeRecordByRecordTypeAndRecordId(recordType, recordId, recordIndependentOfEnteredRecord);
	}

	private void ensureRecordTypeStorageExists(String recordType) {
		if (holderForRecordTypeDoesNotExistInStorage(recordType)) {
			createHolderForRecordTypeInStorage(recordType);
		}
	}

	private boolean holderForRecordTypeDoesNotExistInStorage(String recordType) {
		return null == records.get(recordType);
	}

	private void createHolderForRecordTypeInStorage(String recordType) {
		records.put(recordType, new HashMap<String, DataGroup>());
	}

	private void checkNoConflictOnRecordId(String recordType, String recordId) {
		if (null != records.get(recordType).get(recordId)) {
			throw new RecordConflictException(
					"Record with recordId: " + recordId + " already exists");
		}
	}

	private DataGroup createIndependentCopy(DataGroup record) {
		return SpiderDataGroup.fromDataGroup(record).toDataGroup();
	}

	private DataGroup storeRecordByRecordTypeAndRecordId(String recordType, String recordId,
			DataGroup recordIndependentOfEnteredRecord) {
		return records.get(recordType).put(recordId, recordIndependentOfEnteredRecord);
	}

	@Override
	public Collection<DataGroup> readList(String type) {
		Map<String, DataGroup> typeRecords = records.get(type);
		if (null == typeRecords) {
			throw new RecordNotFoundException("No records exists with recordType: " + type);
		}
		return typeRecords.values();
	}

	@Override
	public DataGroup read(String recordType, String recordId) {
		checkRecordExists(recordType, recordId);
		return records.get(recordType).get(recordId);
	}

	private void checkRecordExists(String recordType, String recordId) {
		if (holderForRecordTypeDoesNotExistInStorage(recordType)) {
			throw new RecordNotFoundException("No records exists with recordType: " + recordType);
		}
		if (null == records.get(recordType).get(recordId)) {
			throw new RecordNotFoundException("No record exists with recordId: " + recordId);
		}
	}

	@Override
	public void deleteByTypeAndId(String recordType, String recordId) {
		checkRecordExists(recordType, recordId);
		records.get(recordType).remove(recordId);
	}

	@Override
	public void update(String type, String id, DataGroup record) {
		checkRecordExists(type, id);
		DataGroup recordIndependentOfEnteredRecord = createIndependentCopy(record);
		storeRecordByRecordTypeAndRecordId(type, id, recordIndependentOfEnteredRecord);
	}

	@Override
	public Collection<DataGroup> getMetadataElements() {
		Collection<DataGroup> readDataGroups = new ArrayList<>();
		for (MetadataTypes metadataType : MetadataTypes.values()) {
			readDataGroups.addAll(readList(metadataType.type));
		}
		return readDataGroups;
	}

	@Override
	public Collection<DataGroup> getPresentationElements() {
		return readList("presentation");
	}

	@Override
	public Collection<DataGroup> getTexts() {
		return readList("text");
	}

	@Override
	public Collection<DataGroup> getRecordTypes() {
		return readList("recordType");
	}

}
