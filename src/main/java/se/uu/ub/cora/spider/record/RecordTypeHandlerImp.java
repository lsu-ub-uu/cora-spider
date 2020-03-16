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
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import se.uu.ub.cora.data.DataGroup;
import se.uu.ub.cora.storage.RecordStorage;

public final class RecordTypeHandlerImp implements RecordTypeHandler {
	private static final String RECORD_PART_CONSTRAINT = "recordPartConstraint";
	private static final String LINKED_RECORD_TYPE = "linkedRecordType";
	private static final String LINKED_RECORD_ID = "linkedRecordId";
	private DataGroup recordType;
	private static final String RECORD_TYPE = "recordType";
	private String recordTypeId;
	private RecordStorage recordStorage;
	private DataGroup metadataGroup;
	private Map<String, String> readWriteConstraints;

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
		if (recordTypeHasAbstractParent(recordTypeDefinition)) {
			createIdAsAbstractType(recordId, recordTypeDefinition, ids);
		}
	}

	private boolean recordTypeHasAbstractParent(DataGroup recordTypeDefinition) {
		return recordTypeDefinition.containsChildWithNameInData("parentId");
	}

	private void createIdAsAbstractType(String recordId, DataGroup recordTypeDefinition,
			List<String> ids) {
		DataGroup parentGroup = recordTypeDefinition.getFirstGroupWithNameInData("parentId");
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
	public Map<String, String> getRecordPartReadWriteConstraints() {
		if (readWriteConstraints == null) {
			collectAllReadWriteConstraints();
		}
		return readWriteConstraints;
	}

	private void collectAllReadWriteConstraints() {
		readWriteConstraints = new HashMap<>();
		for (DataGroup childReference : getAllChildReferences()) {
			possiblyAddReadWriteConstraint(readWriteConstraints, childReference);
		}
	}

	private List<DataGroup> getAllChildReferences() {
		DataGroup childReferences = getMetadataGroup()
				.getFirstGroupWithNameInData("childReferences");
		return childReferences.getAllGroupsWithNameInData("childReference");
	}

	private void possiblyAddReadWriteConstraint(Map<String, String> constraints,
			DataGroup childReference) {
		if (hasReadWriteConstraints(childReference)) {
			String refNameInData = getRefNameInData(childReference);
			constraints.put(refNameInData, getRecordPartConstraintValue(childReference));
		}
	}

	private boolean hasReadWriteConstraints(DataGroup childReference) {
		return hasRecordPartConstrains(childReference) && isReadWriteConstraint(childReference);
	}

	private boolean hasRecordPartConstrains(DataGroup childReference) {
		return childReference.containsChildWithNameInData(RECORD_PART_CONSTRAINT);
	}

	private boolean isReadWriteConstraint(DataGroup childReference) {
		return "readWrite".equals(getRecordPartConstraintValue(childReference));
	}

	private String getRecordPartConstraintValue(DataGroup childReference) {
		return childReference.getFirstAtomicValueWithNameInData(RECORD_PART_CONSTRAINT);
	}

	private String getRefNameInData(DataGroup childReference) {
		DataGroup ref = childReference.getFirstGroupWithNameInData("ref");
		String linkedRecordType = ref.getFirstAtomicValueWithNameInData(LINKED_RECORD_TYPE);
		String linkedRecordId = ref.getFirstAtomicValueWithNameInData(LINKED_RECORD_ID);
		DataGroup metadataRefElement = recordStorage.read(linkedRecordType, linkedRecordId);
		return metadataRefElement.getFirstAtomicValueWithNameInData("nameInData");
	}

	// Only for test
	public String getRecordTypeId() {
		return recordTypeId;
	}

	public RecordStorage getRecordStorage() {
		return recordStorage;
	}

	@Override
	public boolean hasRecordPartReadWriteConstraint() {
		return !getRecordPartReadWriteConstraints().isEmpty();
	}

}
