/*
 * Copyright 2016, 2019, 2020 Uppsala University Library
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

package se.uu.ub.cora.spider.record;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import se.uu.ub.cora.data.DataGroup;
import se.uu.ub.cora.spider.data.DataMissingException;
import se.uu.ub.cora.storage.RecordStorage;

public final class RecordTypeHandlerImp implements RecordTypeHandler {
	private static final String PARENT_ID = "parentId";
	private static final String RECORD_PART_CONSTRAINT = "recordPartConstraint";
	private static final String LINKED_RECORD_TYPE = "linkedRecordType";
	private static final String LINKED_RECORD_ID = "linkedRecordId";
	private DataGroup recordType;
	private static final String RECORD_TYPE = "recordType";
	private String recordTypeId;
	private RecordStorage recordStorage;
	private DataGroup metadataGroup;
	private Set<String> readWriteConstraints = new HashSet<>();
	private Set<String> writeConstraints = new HashSet<>();
	private boolean constraintsNotLoaded = true;

	public static RecordTypeHandlerImp usingRecordStorageAndRecordTypeId(
			RecordStorage recordStorage, String recordTypeId) {
		return new RecordTypeHandlerImp(recordStorage, recordTypeId);
	}

	private RecordTypeHandlerImp(RecordStorage recordStorage, String recordTypeId) {
		this.recordStorage = recordStorage;
		this.recordTypeId = recordTypeId;
		recordType = recordStorage.read(RECORD_TYPE, recordTypeId);
	}

	@Override
	public boolean isAbstract() {
		String abstractInRecordTypeDefinition = getAbstractFromRecordTypeDefinition();
		return "true".equals(abstractInRecordTypeDefinition);
	}

	private String getAbstractFromRecordTypeDefinition() {
		return recordType.getFirstAtomicValueWithNameInData("abstract");
	}

	@Override
	public boolean shouldAutoGenerateId() {
		String userSuppliedId = recordType.getFirstAtomicValueWithNameInData("userSuppliedId");
		return "false".equals(userSuppliedId);
	}

	@Override
	public String getNewMetadataId() {
		DataGroup newMetadataGroup = recordType.getFirstGroupWithNameInData("newMetadataId");
		return newMetadataGroup.getFirstAtomicValueWithNameInData(LINKED_RECORD_ID);
	}

	@Override
	public String getMetadataId() {
		DataGroup metadataIdGroup = recordType.getFirstGroupWithNameInData("metadataId");
		return metadataIdGroup.getFirstAtomicValueWithNameInData(LINKED_RECORD_ID);
	}

	@Override
	public List<String> createListOfPossibleIdsToThisRecord(String recordId) {
		List<String> ids = new ArrayList<>();
		ids.add(recordTypeId + "_" + recordId);
		possiblyCreateIdForAbstractType(recordId, recordType, ids);
		return ids;
	}

	private void possiblyCreateIdForAbstractType(String recordId, DataGroup recordTypeDefinition,
			List<String> ids) {
		if (hasParent()) {
			createIdAsAbstractType(recordId, recordTypeDefinition, ids);
		}
	}

	private void createIdAsAbstractType(String recordId, DataGroup recordTypeDefinition,
			List<String> ids) {
		DataGroup parentGroup = recordTypeDefinition.getFirstGroupWithNameInData(PARENT_ID);
		String abstractParentType = parentGroup.getFirstAtomicValueWithNameInData(LINKED_RECORD_ID);
		ids.add(abstractParentType + "_" + recordId);
	}

	@Override
	public boolean isPublicForRead() {
		if (recordType.containsChildWithNameInData("public")) {
			String isPublic = recordType.getFirstAtomicValueWithNameInData("public");
			return "true".equals(isPublic);
		}
		return false;
	}

	@Override
	public DataGroup getMetadataGroup() {
		if (metadataGroup == null) {
			metadataGroup = recordStorage.read("metadataGroup", getMetadataId());
		}
		return metadataGroup;
	}

	@Override
	public Set<String> getRecordPartReadConstraints() {
		if (constraintsNotLoaded) {
			collectAllConstraints();
		}
		return readWriteConstraints;
	}

	private void collectAllConstraints() {
		constraintsNotLoaded = false;
		for (DataGroup childReference : getAllChildReferences()) {
			possiblyAddConstraint(childReference);
		}
	}

	private List<DataGroup> getAllChildReferences() {
		DataGroup childReferences = getMetadataGroup()
				.getFirstGroupWithNameInData("childReferences");
		return childReferences.getAllGroupsWithNameInData("childReference");
	}

	private void possiblyAddConstraint(DataGroup childReference) {
		if (hasConstraints(childReference)) {
			addWriteAndOrReadWriteConstraints(childReference);
		}
	}

	private boolean hasConstraints(DataGroup childReference) {
		return childReference.containsChildWithNameInData(RECORD_PART_CONSTRAINT);
	}

	private void addWriteAndOrReadWriteConstraints(DataGroup childReference) {
		String refNameInData = getRefNameInData(childReference);
		String constraintValue = getRecordPartConstraintValue(childReference);
		writeConstraints.add(refNameInData);
		possiblyAddReadWriteConstraint(refNameInData, constraintValue);
	}

	private String getRefNameInData(DataGroup childReference) {
		DataGroup ref = childReference.getFirstGroupWithNameInData("ref");
		String linkedRecordType = ref.getFirstAtomicValueWithNameInData(LINKED_RECORD_TYPE);
		String linkedRecordId = ref.getFirstAtomicValueWithNameInData(LINKED_RECORD_ID);
		DataGroup metadataRefElement = recordStorage.read(linkedRecordType, linkedRecordId);
		return metadataRefElement.getFirstAtomicValueWithNameInData("nameInData");
	}

	private String getRecordPartConstraintValue(DataGroup childReference) {
		return childReference.getFirstAtomicValueWithNameInData(RECORD_PART_CONSTRAINT);
	}

	private void possiblyAddReadWriteConstraint(String refNameInData, String constraintValue) {
		if (isReadWriteConstraint(constraintValue)) {
			readWriteConstraints.add(refNameInData);
		}
	}

	private boolean isReadWriteConstraint(String constraintValue) {
		return "readWrite".equals(constraintValue);
	}

	public RecordStorage getRecordStorage() {
		return recordStorage;
	}

	@Override
	public boolean hasRecordPartReadConstraint() {
		return !getRecordPartReadConstraints().isEmpty();
	}

	@Override
	public boolean hasRecordPartWriteConstraint() {
		return hasRecordPartReadConstraint() || !getRecordPartWriteConstraints().isEmpty();
	}

	@Override
	public Set<String> getRecordPartWriteConstraints() {
		if (constraintsNotLoaded) {
			collectAllConstraints();
		}
		return writeConstraints;
	}

	// Only for test
	public String getRecordTypeId() {
		return recordTypeId;
	}

	@Override
	public boolean hasParent() {
		return recordType.containsChildWithNameInData(PARENT_ID);
	}

	@Override
	public String getParentId() {
		throwErrorIfNoParent();
		return extractParentId();
	}

	private String extractParentId() {
		DataGroup parentGroup = recordType.getFirstGroupWithNameInData(PARENT_ID);
		return parentGroup.getFirstAtomicValueWithNameInData(LINKED_RECORD_ID);
	}

	private void throwErrorIfNoParent() {
		if (!hasParent()) {
			throw new DataMissingException("Unable to get parentId, no parents exists");
		}
	}

	@Override
	public boolean isChildOfBinary() {
		return hasParent() && parentIsBinary();
	}

	private boolean parentIsBinary() {
		String parentId = extractParentId();
		return "binary".equals(parentId);
	}

	@Override
	public boolean representsTheRecordTypeDefiningSearches() {
		String id = extractIdFromRecordInfo();
		return "search".equals(id);
	}

	private String extractIdFromRecordInfo() {
		DataGroup recordInfo = recordType.getFirstGroupWithNameInData("recordInfo");
		return recordInfo.getFirstAtomicValueWithNameInData("id");
	}

	@Override
	public boolean representsTheRecordTypeDefiningRecordTypes() {
		String id = extractIdFromRecordInfo();
		return RECORD_TYPE.equals(id);
	}

	@Override
	public boolean hasLinkedSearch() {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public String getSearchId() {
		// TODO Auto-generated method stub
		return null;
	}
}
