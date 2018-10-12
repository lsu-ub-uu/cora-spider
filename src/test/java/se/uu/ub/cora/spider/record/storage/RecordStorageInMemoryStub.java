/*
 * Copyright 2015 Uppsala University Library
 *
 * This file is part of Cora.
 *
 *     Cora is free software: you can redistribute it and/or modify
 *     it under the terms of the GNU General Public License as published by
 *     the Free Software Foundation, either version 3 of the License, or
 *     (at your option) any later version.
 *
 *     Cora is distributed in the hope that it will be useful,
 *     but WITHOUT ANY WARRANTY; without even the implied warranty of
 *     MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *     GNU General Public License for more details.
 *
 *     You should have received a copy of the GNU General Public License
 *     along with Cora.  If not, see <http://www.gnu.org/licenses/>.
 */

package se.uu.ub.cora.spider.record.storage;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import se.uu.ub.cora.bookkeeper.data.DataElement;
import se.uu.ub.cora.bookkeeper.data.DataGroup;
import se.uu.ub.cora.bookkeeper.metadata.MetadataTypes;
import se.uu.ub.cora.bookkeeper.storage.MetadataStorage;
import se.uu.ub.cora.spider.data.SpiderDataGroup;
import se.uu.ub.cora.spider.data.SpiderReadResult;

public class RecordStorageInMemoryStub implements RecordStorage, MetadataStorage {
	private DataGroup emptyFilter = DataGroup.withNameInData("filter");
	protected Map<String, Map<String, DataGroup>> records = new HashMap<>();
	protected Map<String, Map<String, DataGroup>> linkLists = new HashMap<>();
	protected Map<String, Map<String, Map<String, Map<String, List<DataGroup>>>>> incomingLinks = new HashMap<>();

	public RecordStorageInMemoryStub() {
		// Make it possible to use default empty record storage
	}

	public RecordStorageInMemoryStub(Map<String, Map<String, DataGroup>> records) {
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
	public void create(String recordType, String recordId, DataGroup record,
			DataGroup collectedTerms, DataGroup linkList, String dataDivider) {
		ensureStorageExistsForRecordType(recordType);
		checkNoConflictOnRecordId(recordType, recordId);
		storeIndependentRecordByRecordTypeAndRecordId(recordType, recordId, record);
		storeLinks(recordType, recordId, linkList);
	}

	protected void ensureStorageExistsForRecordType(String recordType) {
		if (holderForRecordTypeDoesNotExistInStorage(recordType)) {
			createHolderForRecordTypeInStorage(recordType);
		}
	}

	private boolean holderForRecordTypeDoesNotExistInStorage(String recordType) {
		return !records.containsKey(recordType);
	}

	private void createHolderForRecordTypeInStorage(String recordType) {
		records.put(recordType, new HashMap<String, DataGroup>());
		linkLists.put(recordType, new HashMap<String, DataGroup>());
	}

	private void checkNoConflictOnRecordId(String recordType, String recordId) {
		if (recordWithTypeAndIdAlreadyExists(recordType, recordId)) {
			throw new RecordConflictException(
					"Record with recordId: " + recordId + " already exists");
		}
	}

	private boolean recordWithTypeAndIdAlreadyExists(String recordType, String recordId) {
		return records.get(recordType).containsKey(recordId);
	}

	private void storeIndependentRecordByRecordTypeAndRecordId(String recordType, String recordId,
			DataGroup record) {
		DataGroup recordIndependentOfEnteredRecord = createIndependentCopy(record);
		storeRecordByRecordTypeAndRecordId(recordType, recordId, recordIndependentOfEnteredRecord);
	}

	private DataGroup createIndependentCopy(DataGroup record) {
		return SpiderDataGroup.fromDataGroup(record).toDataGroup();
	}

	protected DataGroup storeRecordByRecordTypeAndRecordId(String recordType, String recordId,
			DataGroup recordIndependentOfEnteredRecord) {
		return records.get(recordType).put(recordId, recordIndependentOfEnteredRecord);
	}

	protected void storeLinks(String recordType, String recordId, DataGroup linkList) {
		if (linkList.getChildren().size() > 0) {
			DataGroup linkListIndependentFromEntered = createIndependentCopy(linkList);
			storeLinkList(recordType, recordId, linkListIndependentFromEntered);
			storeLinksInIncomingLinks(linkListIndependentFromEntered);
		}
	}

	private void storeLinkList(String recordType, String recordId,
			DataGroup linkListIndependentFromEntered) {
		linkLists.get(recordType).put(recordId, linkListIndependentFromEntered);
	}

	private void storeLinksInIncomingLinks(DataGroup incomingLinkList) {
		for (DataElement linkElement : incomingLinkList.getChildren()) {
			storeLinkInIncomingLinks((DataGroup) linkElement);
		}
	}

	private void storeLinkInIncomingLinks(DataGroup link) {
		Map<String, Map<String, List<DataGroup>>> toPartOfIncomingLinks = getIncomingLinkStorageForLink(
				link);
		storeLinkInIncomingLinks(link, toPartOfIncomingLinks);
	}

	private Map<String, Map<String, List<DataGroup>>> getIncomingLinkStorageForLink(
			DataGroup link) {
		DataGroup to = link.getFirstGroupWithNameInData("to");
		String toType = extractLinkedRecordTypeValue(to);
		String toId = extractLinkedRecordIdValue(to);

		ensureInIncomingLinksHolderForRecordTypeAndRecordId(toType, toId);

		return incomingLinks.get(toType).get(toId);
	}

	private String extractLinkedRecordIdValue(DataGroup to) {
		return to.getFirstAtomicValueWithNameInData("linkedRecordId");
	}

	private String extractLinkedRecordTypeValue(DataGroup dataGroup) {
		return dataGroup.getFirstAtomicValueWithNameInData("linkedRecordType");
	}

	private void ensureInIncomingLinksHolderForRecordTypeAndRecordId(String toType, String toId) {
		if (isIncomingLinksHolderForRecordTypeMissing(toType)) {
			incomingLinks.put(toType, new HashMap<>());
		}
		if (isIncomingLinksHolderForRecordIdMissing(toType, toId)) {
			incomingLinks.get(toType).put(toId, new HashMap<>());
		}
	}

	private boolean isIncomingLinksHolderForRecordTypeMissing(String toType) {
		return !incomingLinkStorageForRecordTypeExists(toType);
	}

	private boolean isIncomingLinksHolderForRecordIdMissing(String toType, String toId) {
		return !incomingLinksHolderForRecordIdExists(toType, toId);
	}

	private void storeLinkInIncomingLinks(DataGroup link,
			Map<String, Map<String, List<DataGroup>>> toPartOfIncomingLinks) {
		DataGroup from = link.getFirstGroupWithNameInData("from");
		String fromType = extractLinkedRecordTypeValue(from);
		String fromId = extractLinkedRecordIdValue(from);

		ensureIncomingLinksHolderExistsForFromRecordType(toPartOfIncomingLinks, fromType);

		ensureIncomingLinksHolderExistsForFromRecordId(toPartOfIncomingLinks.get(fromType), fromId);
		toPartOfIncomingLinks.get(fromType).get(fromId).add(link);
	}

	private void ensureIncomingLinksHolderExistsForFromRecordType(
			Map<String, Map<String, List<DataGroup>>> toPartOfIncomingLinks, String fromType) {
		if (!toPartOfIncomingLinks.containsKey(fromType)) {
			toPartOfIncomingLinks.put(fromType, new HashMap<>());
		}
	}

	private void ensureIncomingLinksHolderExistsForFromRecordId(
			Map<String, List<DataGroup>> fromPartOfIncomingLinks, String fromId) {
		if (!fromPartOfIncomingLinks.containsKey(fromId)) {
			fromPartOfIncomingLinks.put(fromId, new ArrayList<>());
		}
	}

	@Override
	public SpiderReadResult readList(String type, DataGroup filter) {
		Map<String, DataGroup> typeRecords = records.get(type);
		if (null == typeRecords) {
			throw new RecordNotFoundException("No records exists with recordType: " + type);
		}
		SpiderReadResult spiderReadResult = new SpiderReadResult();
		spiderReadResult.start = 1;
		spiderReadResult.listOfDataGroups = new ArrayList<>(typeRecords.values());
		return spiderReadResult;
	}

	@Override
	public SpiderReadResult readAbstractList(String type, DataGroup filter) {
		SpiderReadResult spiderReadResult = new SpiderReadResult();
		spiderReadResult.start = 1;
		// spiderReadResult.listOfDataGroups = new ArrayList<>(typeRecords.values());
		return spiderReadResult;
	}

	@Override
	public boolean recordsExistForRecordType(String type) {
		Map<String, DataGroup> typeRecords = records.get(type);
		if (null == typeRecords) {
			return false;
		}
		return true;
	}

	@Override
	public boolean recordExistsForAbstractOrImplementingRecordTypeAndRecordId(String type,
			String id) {
		return false;
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
		if (!linkLists.get(recordType).containsKey(recordId)) {
			return DataGroup.withNameInData("collectedDataLinks");
		}
		return linkLists.get(recordType).get(recordId);
	}

	@Override
	public void deleteByTypeAndId(String recordType, String recordId) {
		checkRecordExists(recordType, recordId);
		removeIncomingLinks(recordType, recordId);
		linkLists.get(recordType).remove(recordId);
		if (linkLists.get(recordType).isEmpty()) {
			linkLists.remove(recordType);
		}
		records.get(recordType).remove(recordId);
		if (records.get(recordType).isEmpty()) {
			records.remove(recordType);
		}
	}

	@Override
	public Collection<DataGroup> generateLinkCollectionPointingToRecord(String type, String id) {
		if (linksExistForRecord(type, id)) {
			return generateLinkCollectionFromStoredLinks(type, id);
		}
		return Collections.emptyList();
	}

	private Collection<DataGroup> generateLinkCollectionFromStoredLinks(String type, String id) {
		List<DataGroup> generatedLinkList = new ArrayList<>();
		Map<String, Map<String, List<DataGroup>>> linkStorageForRecord = incomingLinks.get(type)
				.get(id);
		addLinksForRecordFromAllRecordTypes(generatedLinkList, linkStorageForRecord);
		return generatedLinkList;
	}

	private void addLinksForRecordFromAllRecordTypes(List<DataGroup> generatedLinkList,
			Map<String, Map<String, List<DataGroup>>> linkStorageForRecord) {
		for (Map<String, List<DataGroup>> mapOfId : linkStorageForRecord.values()) {
			addLinksForRecordForThisRecordType(generatedLinkList, mapOfId);
		}
	}

	private void addLinksForRecordForThisRecordType(List<DataGroup> generatedLinkList,
			Map<String, List<DataGroup>> mapOfId) {
		for (List<DataGroup> recordToRecordLinkList : mapOfId.values()) {
			addLinksFromRecordToRecordLinkList(generatedLinkList, recordToRecordLinkList);
		}
	}

	private void addLinksFromRecordToRecordLinkList(List<DataGroup> generatedLinkList,
			List<DataGroup> recordToRecordLinkList) {
		for (DataGroup recordToRecordLink : recordToRecordLinkList) {
			generatedLinkList.add(recordToRecordLink);
		}
	}

	@Override
	public boolean linksExistForRecord(String type, String id) {
		return incomingLinkStorageForRecordTypeExists(type)
				&& incomingLinksHolderForRecordIdExists(type, id);
	}

	private boolean incomingLinksHolderForRecordIdExists(String type, String id) {
		return incomingLinks.get(type).containsKey(id);
	}

	private boolean incomingLinkStorageForRecordTypeExists(String type) {
		return incomingLinks.containsKey(type);
	}

	@Override
	public void update(String recordType, String recordId, DataGroup record,
			DataGroup collectedTerms, DataGroup linkList, String dataDivider) {
		checkRecordExists(recordType, recordId);
		removeIncomingLinks(recordType, recordId);
		storeIndependentRecordByRecordTypeAndRecordId(recordType, recordId, record);
		storeLinks(recordType, recordId, linkList);
	}

	private void removeIncomingLinks(String recordType, String recordId) {
		DataGroup oldLinkList = readLinkList(recordType, recordId);
		for (DataElement linkElement : oldLinkList.getChildren()) {
			removeIncomingLink(linkElement);
		}
	}

	private void removeIncomingLink(DataElement linkElement) {
		DataGroup link = (DataGroup) linkElement;
		DataGroup recordLinkTo = link.getFirstGroupWithNameInData("to");
		if (incomingLinksContainsToType(recordLinkTo)) {
			Map<String, Map<String, List<DataGroup>>> toPartOfIncomingLinks = extractToPartOfIncomingLinks(
					recordLinkTo);

			removeLinkAndFromHolderFromIncomingLinks(link, toPartOfIncomingLinks);

			removeToHolderFromIncomingLinks(recordLinkTo, toPartOfIncomingLinks);
		}
	}

	private boolean incomingLinksContainsToType(DataGroup to) {
		String toType = extractLinkedRecordTypeValue(to);
		return incomingLinks.containsKey(toType);
	}

	private Map<String, Map<String, List<DataGroup>>> extractToPartOfIncomingLinks(DataGroup to) {
		String toType = extractLinkedRecordTypeValue(to);
		String toId = extractLinkedRecordIdValue(to);
		return incomingLinks.get(toType).get(toId);
	}

	private void removeLinkAndFromHolderFromIncomingLinks(DataGroup link,
			Map<String, Map<String, List<DataGroup>>> linksForToPart) {
		DataGroup from = link.getFirstGroupWithNameInData("from");
		String fromType = extractLinkedRecordTypeValue(from);
		String fromId = extractLinkedRecordIdValue(from);

		linksForToPart.get(fromType).remove(fromId);

		if (linksForToPart.get(fromType).isEmpty()) {
			linksForToPart.remove(fromType);
		}
	}

	private void removeToHolderFromIncomingLinks(DataGroup to,
			Map<String, Map<String, List<DataGroup>>> toPartOfIncomingLinks) {
		String toType = extractLinkedRecordTypeValue(to);
		String toId = extractLinkedRecordIdValue(to);
		if (toPartOfIncomingLinks.isEmpty()) {
			incomingLinks.get(toType).remove(toId);
		}
		if (incomingLinks.get(toType).isEmpty()) {
			incomingLinks.remove(toType);
		}
	}

	@Override
	public Collection<DataGroup> getMetadataElements() {
		Collection<DataGroup> readDataGroups = new ArrayList<>();
		for (MetadataTypes metadataType : MetadataTypes.values()) {
			readDataGroups.addAll(readList(metadataType.type, emptyFilter).listOfDataGroups);
		}
		return readDataGroups;
	}

	@Override
	public Collection<DataGroup> getPresentationElements() {
		return readList("presentation", emptyFilter).listOfDataGroups;
	}

	@Override
	public Collection<DataGroup> getTexts() {
		return readList("text", emptyFilter).listOfDataGroups;
	}

	@Override
	public Collection<DataGroup> getRecordTypes() {
		return readList("recordType", emptyFilter).listOfDataGroups;
	}

	@Override
	public Collection<DataGroup> getCollectTerms() {
		return null;
	}

}
