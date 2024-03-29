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
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import se.uu.ub.cora.data.DataGroup;
import se.uu.ub.cora.data.DataRecordGroup;
import se.uu.ub.cora.data.collected.Link;
import se.uu.ub.cora.data.collected.StorageTerm;
import se.uu.ub.cora.storage.Filter;
import se.uu.ub.cora.storage.RecordConflictException;
import se.uu.ub.cora.storage.RecordNotFoundException;
import se.uu.ub.cora.storage.RecordStorage;
import se.uu.ub.cora.storage.StorageReadResult;

public class RecordStorageInMemoryStub implements RecordStorage {
	protected Map<String, Map<String, DataGroup>> records = new HashMap<>();
	protected Map<String, Map<String, List<Link>>> linkLists = new HashMap<>();
	protected Map<String, Map<String, Map<String, Map<String, List<Link>>>>> incomingLinks = new HashMap<>();

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
			Set<StorageTerm> storageTerms, Set<Link> links, String dataDivider) {
		ensureStorageExistsForRecordType(recordType);
		checkNoConflictOnRecordId(recordType, recordId);
		// storeIndependentRecordByRecordTypeAndRecordId(recordType, recordId, record);
		storeRecordByRecordTypeAndRecordId(recordType, recordId, record);
		// storeLinks(recordType, recordId, links);
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
		// TODO: must do stuff :)
		// linkLists.put(recordType, new HashMap<String, DataGroup>());

	}

	private void checkNoConflictOnRecordId(String recordType, String recordId) {
		if (recordWithTypeAndIdAlreadyExists(recordType, recordId)) {
			throw RecordConflictException
					.withMessage("Record with recordId: " + recordId + " already exists");
		}
	}

	private boolean recordWithTypeAndIdAlreadyExists(String recordType, String recordId) {
		return records.get(recordType).containsKey(recordId);
	}

	protected DataGroup storeRecordByRecordTypeAndRecordId(String recordType, String recordId,
			DataGroup recordIndependentOfEnteredRecord) {
		return records.get(recordType).put(recordId, recordIndependentOfEnteredRecord);
	}

	protected void storeLinks(String recordType, String recordId, List<Link> links) {
		if (links.size() > 0) {
			// DataGroup linkListIndependentFromEntered = createIndependentCopy(links);

			storeLinkList(recordType, recordId, links);
			storeLinksInIncomingLinks(recordType, recordId, links);
		}
	}

	private void storeLinkList(String recordType, String recordId, List<Link> links) {
		linkLists.get(recordType).put(recordId, links);
	}

	private void storeLinksInIncomingLinks(String recordType, String recordId, List<Link> links) {
		for (Link linkElement : links) {
			storeLinkInIncomingLinks2(recordType, recordId, linkElement);
		}
	}

	private void storeLinkInIncomingLinks2(String recordType, String recordId, Link linkElement) {
		Map<String, Map<String, List<Link>>> toPartOfIncomingLinks = getIncomingLinkStorageForLink(
				linkElement);
		storeLinkInIncomingLinks(recordType, recordId, linkElement, toPartOfIncomingLinks);
	}

	private Map<String, Map<String, List<Link>>> getIncomingLinkStorageForLink(Link link) {
		String toType = link.type();
		String toId = link.id();

		ensureInIncomingLinksHolderForRecordTypeAndRecordId(toType, toId);

		return incomingLinks.get(toType).get(toId);
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

	private void storeLinkInIncomingLinks(String recordType, String recordId, Link link,
			Map<String, Map<String, List<Link>>> toPartOfIncomingLinks) {
		String fromType = recordType;
		String fromId = recordId;

		ensureIncomingLinksHolderExistsForFromRecordType(toPartOfIncomingLinks, fromType);

		ensureIncomingLinksHolderExistsForFromRecordId(toPartOfIncomingLinks.get(fromType), fromId);
		toPartOfIncomingLinks.get(fromType).get(fromId).add(link);
	}

	private void ensureIncomingLinksHolderExistsForFromRecordType(
			Map<String, Map<String, List<Link>>> toPartOfIncomingLinks, String fromType) {
		if (!toPartOfIncomingLinks.containsKey(fromType)) {
			toPartOfIncomingLinks.put(fromType, new HashMap<>());
		}
	}

	private void ensureIncomingLinksHolderExistsForFromRecordId(
			Map<String, List<Link>> fromPartOfIncomingLinks, String fromId) {
		if (!fromPartOfIncomingLinks.containsKey(fromId)) {
			fromPartOfIncomingLinks.put(fromId, new ArrayList<>());
		}
	}

	@Override
	public StorageReadResult readList(List<String> type, Filter filter) {
		Map<String, DataGroup> typeRecords = records.get(type);
		if (null == typeRecords) {
			throw RecordNotFoundException.withMessage("No records exists with recordType: " + type);
		}
		StorageReadResult spiderReadResult = new StorageReadResult();
		spiderReadResult.start = 1;
		spiderReadResult.listOfDataGroups = new ArrayList<>(typeRecords.values());
		spiderReadResult.totalNumberOfMatches = 177;
		return spiderReadResult;
	}

	@Override
	public boolean recordExists(List<String> types, String id) {
		return false;
	}

	@Override
	public DataGroup read(List<String> recordTypes, String recordId) {
		for (String recordType : recordTypes) {

			checkRecordExists(recordType, recordId);
			return records.get(recordType).get(recordId);
		}
		return null;
	}

	private void checkRecordExists(String recordType, String recordId) {
		if (holderForRecordTypeDoesNotExistInStorage(recordType)) {
			throw RecordNotFoundException
					.withMessage("No records exists with recordType: " + recordType);
		}
		if (null == records.get(recordType).get(recordId)) {
			throw RecordNotFoundException
					.withMessage("No record exists with recordId: " + recordId);
		}
	}

	private List<Link> readLinkList(String recordType, String recordId) {
		checkRecordExists(recordType, recordId);
		if (!linkLists.get(recordType).containsKey(recordId)) {
			return Collections.emptyList();
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
	public Set<Link> getLinksToRecord(String type, String id) {
		return null;
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
			Set<StorageTerm> collectedTerms, Set<Link> links, String dataDivider) {
		checkRecordExists(recordType, recordId);
		removeIncomingLinks(recordType, recordId);
		// storeIndependentRecordByRecordTypeAndRecordId(recordType, recordId, record);
		storeRecordByRecordTypeAndRecordId(recordType, recordId, record);
		// storeLinks(recordType, recordId, links);
	}

	private void removeIncomingLinks(String recordType, String recordId) {
		List<Link> oldLinkList = readLinkList(recordType, recordId);
		for (Link linkElement : oldLinkList) {
			removeIncomingLink(recordType, recordId, linkElement);
		}
	}

	private void removeIncomingLink(String recordType, String recordId, Link linkElement) {
		// DataGroup link = (DataGroup) linkElement;
		// DataGroup recordLinkTo = link.getFirstGroupWithNameInData("to");
		if (incomingLinks.containsKey(linkElement.type())) {
			// if (incomingLinksContainsToType(recordLinkTo)) {
			Map<String, Map<String, List<Link>>> toPartOfIncomingLinks = extractToPartOfIncomingLinks(
					linkElement);
			// TODO: must du stuff :)
			// removeLinkAndFromHolderFromIncomingLinks(recordType, recordId,
			// toPartOfIncomingLinks);
			//
			// removeToHolderFromIncomingLinks(recordLinkTo, toPartOfIncomingLinks);
		}
	}

	private Map<String, Map<String, List<Link>>> extractToPartOfIncomingLinks(Link linkElement) {
		return incomingLinks.get(linkElement.type()).get(linkElement.id());
	}

	@Override
	public long getTotalNumberOfRecordsForTypes(List<String> types, Filter filter) {
		return 0;
	}

	@Override
	public DataRecordGroup read(String type, String id) {
		// TODO Auto-generated method stub
		return null;
	}

}
