package se.uu.ub.cora.spider.record.storage;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import se.uu.ub.cora.metadataformat.data.DataElement;
import se.uu.ub.cora.metadataformat.data.DataGroup;
import se.uu.ub.cora.metadataformat.data.DataRecordLink;
import se.uu.ub.cora.metadataformat.metadata.MetadataTypes;
import se.uu.ub.cora.metadataformat.storage.MetadataStorage;
import se.uu.ub.cora.spider.data.SpiderDataGroup;

public class RecordStorageInMemory implements RecordStorage, MetadataStorage {
	private Map<String, Map<String, DataGroup>> records = new HashMap<>();
	private Map<String, Map<String, DataGroup>> linkListStorage = new HashMap<>();
	private Map<String, Map<String, Map<String, Map<String, DataGroup>>>> linkStorage = new HashMap<>();

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
	public void create(String recordType, String recordId, DataGroup record, DataGroup linkList) {
		ensureRecordTypeStorageExists(recordType);
		checkNoConflictOnRecordId(recordType, recordId);
		storeIndependentRecordByRecordTypeAndId(recordType, recordId, record);
		storeLinks(recordType, recordId, linkList);
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
		linkListStorage.put(recordType, new HashMap<String, DataGroup>());
	}

	private void checkNoConflictOnRecordId(String recordType, String recordId) {
		if (recordWithTypeAndIdAlreadyExists(recordType, recordId)) {
			throw new RecordConflictException(
					"Record with recordId: " + recordId + " already exists");
		}
	}

	private boolean recordWithTypeAndIdAlreadyExists(String recordType, String recordId) {
		return null != records.get(recordType).get(recordId);
	}

	private void storeIndependentRecordByRecordTypeAndId(String recordType, String recordId,
			DataGroup record) {
		DataGroup recordIndependentOfEnteredRecord = createIndependentCopy(record);
		storeRecordByRecordTypeAndRecordId(recordType, recordId, recordIndependentOfEnteredRecord);
	}

	private DataGroup createIndependentCopy(DataGroup record) {
		return SpiderDataGroup.fromDataGroup(record).toDataGroup();
	}

	private DataGroup storeRecordByRecordTypeAndRecordId(String recordType, String recordId,
			DataGroup recordIndependentOfEnteredRecord) {
		return records.get(recordType).put(recordId, recordIndependentOfEnteredRecord);
	}

	private void storeLinks(String recordType, String recordId, DataGroup linkList) {
		DataGroup linkListIndependentFromEntered = createIndependentCopy(linkList);
		storeLinkList(recordType, recordId, linkListIndependentFromEntered);
		storeLinksForReadingForTargetRecord(linkListIndependentFromEntered);
	}

	private void storeLinkList(String recordType, String recordId,
			DataGroup linkListIndependentFromEntered) {
		linkListStorage.get(recordType).put(recordId, linkListIndependentFromEntered);
	}

	private void storeLinksForReadingForTargetRecord(DataGroup incomingLinkList) {
		for (DataElement linkElement : incomingLinkList.getChildren()) {
			storeLinkForReadingForTargetRecord((DataGroup) linkElement);
		}
	}

	private void storeLinkForReadingForTargetRecord(DataGroup link) {
		Map<String, Map<String, DataGroup>> toRecordsMapOfLinks = getStorageForLink(link);
		storeLink(link, toRecordsMapOfLinks);
	}

	private Map<String, Map<String, DataGroup>> getStorageForLink(DataGroup link) {
		DataRecordLink to = (DataRecordLink) link.getFirstChildWithNameInData("to");
		String toType = to.getRecordType();
		String toId = to.getRecordId();

		ensureLinkStorageForTargetTypeAndId(toType, toId);

		return linkStorage.get(toType).get(toId);
	}

	private void ensureLinkStorageForTargetTypeAndId(String toType, String toId) {
		if (isLinkStorageForTargetTypeMissing(toType)) {
			linkStorage.put(toType, new HashMap<>());
		}
		if (isLinkStorageForTargetIdMissing(toType, toId)) {
			linkStorage.get(toType).put(toId, new HashMap<>());
		}
	}

	private boolean isLinkStorageForTargetTypeMissing(String toType) {
		return !linkStorageForTargetTypeExists(toType);
	}

	private boolean isLinkStorageForTargetIdMissing(String toType, String toId) {
		return !linkStorageForTargetIdExists(toType, toId);
	}

	private void storeLink(DataGroup link,
			Map<String, Map<String, DataGroup>> toRecordsMapOfLinks) {
		DataRecordLink from = (DataRecordLink) link.getFirstChildWithNameInData("from");
		String fromType = from.getRecordType();
		String fromId = from.getRecordId();

		ensureLinkStorageForFromType(toRecordsMapOfLinks, fromType);
		toRecordsMapOfLinks.get(fromType).put(fromId, link);
	}

	private void ensureLinkStorageForFromType(
			Map<String, Map<String, DataGroup>> toRecordsMapOfLinks, String fromType) {
		if (!toRecordsMapOfLinks.containsKey(fromType)) {
			toRecordsMapOfLinks.put(fromType, new HashMap<>());
		}
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
	public DataGroup readLinkList(String recordType, String recordId) {
		checkRecordExists(recordType, recordId);
		return linkListStorage.get(recordType).get(recordId);
	}

	@Override
	public void deleteByTypeAndId(String recordType, String recordId) {
		checkRecordExists(recordType, recordId);
		records.get(recordType).remove(recordId);
	}

	@Override
	public DataGroup generateLinkCollectionPointingToRecord(String type, String id) {
		if (linksExistForRecord(type, id)) {
			return generateLinkCollectionFromStoredLinks(type, id);
		}
		return DataGroup.withNameInData("incomingRecordLinks");
	}

	private DataGroup generateLinkCollectionFromStoredLinks(String type, String id) {
		DataGroup generatedLinkList = DataGroup.withNameInData("incomingRecordLinks");
		Map<String, Map<String, DataGroup>> linkStorageForRecord = linkStorage.get(type).get(id);
		addLinksForRecordFromAllRecordTypes(generatedLinkList, linkStorageForRecord);
		return generatedLinkList;
	}

	private void addLinksForRecordFromAllRecordTypes(DataGroup generatedLinkList,
			Map<String, Map<String, DataGroup>> linkStorageForRecord) {
		for (Map<String, DataGroup> mapOfId : linkStorageForRecord.values()) {
			addLinksForRecordForThisRecordType(generatedLinkList, mapOfId);
		}
	}

	private void addLinksForRecordForThisRecordType(DataGroup generatedLinkList, Map<String, DataGroup> mapOfId) {
		for (DataGroup dataGroup : mapOfId.values()) {
			generatedLinkList.addChild(dataGroup);
		}
	}

	private boolean linksExistForRecord(String type, String id) {
		return linkStorageForTargetTypeExists(type) && linkStorageForTargetIdExists(type, id);
	}

	private boolean linkStorageForTargetIdExists(String type, String id) {
		return linkStorage.get(type).containsKey(id);
	}

	private boolean linkStorageForTargetTypeExists(String type) {
		return linkStorage.containsKey(type);
	}

	@Override
	public void update(String type, String id, DataGroup record) {
		checkRecordExists(type, id);
		storeIndependentRecordByRecordTypeAndId(type, id, record);
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
