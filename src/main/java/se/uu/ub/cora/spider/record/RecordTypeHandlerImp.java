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

import se.uu.ub.cora.bookkeeper.metadata.Constraint;
import se.uu.ub.cora.data.DataAttribute;
import se.uu.ub.cora.data.DataAttributeProvider;
import se.uu.ub.cora.data.DataGroup;
import se.uu.ub.cora.spider.data.DataMissingException;
import se.uu.ub.cora.storage.RecordStorage;

public final class RecordTypeHandlerImp implements RecordTypeHandler {
	private static final String NAME_IN_DATA = "nameInData";
	private static final String SEARCH = "search";
	private static final String PARENT_ID = "parentId";
	private static final String RECORD_PART_CONSTRAINT = "recordPartConstraint";
	private static final String LINKED_RECORD_TYPE = "linkedRecordType";
	private static final String LINKED_RECORD_ID = "linkedRecordId";
	private DataGroup recordType;
	private static final String RECORD_TYPE = "recordType";
	private String recordTypeId;
	private RecordStorage recordStorage;
	private DataGroup metadataGroup;
	private Set<Constraint> readWriteConstraints = new HashSet<>();
	private Set<Constraint> createConstraints = new HashSet<>();
	private Set<Constraint> writeConstraints = new HashSet<>();
	private boolean constraintsForUpdateLoaded = false;
	private boolean constraintsForCreateLoaded = false;

	public static RecordTypeHandlerImp usingRecordStorageAndRecordTypeId(
			RecordStorage recordStorage, String recordTypeId) {
		return new RecordTypeHandlerImp(recordStorage, recordTypeId);
	}

	private RecordTypeHandlerImp(RecordStorage recordStorage, String recordTypeId) {
		this.recordStorage = recordStorage;
		this.recordTypeId = recordTypeId;
		recordType = recordStorage.read(RECORD_TYPE, recordTypeId);
	}

	public RecordTypeHandlerImp(RecordStorage recordStorage, DataGroup dataGroup) {
		this.recordStorage = recordStorage;
		recordType = dataGroup;
	}

	public static RecordTypeHandler usingRecordStorageAndDataGroup(RecordStorage recordStorage,
			DataGroup dataGroup) {
		return new RecordTypeHandlerImp(recordStorage, dataGroup);
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
		String isPublic = recordType.getFirstAtomicValueWithNameInData("public");
		return "true".equals(isPublic);
	}

	@Override
	public DataGroup getMetadataGroup() {
		if (metadataGroup == null) {
			metadataGroup = recordStorage.read("metadataGroup", getMetadataId());
		}
		return metadataGroup;
	}

	@Override
	public Set<Constraint> getRecordPartReadConstraints() {
		if (constraintsForUpdateNotLoaded()) {
			collectAllConstraints();
		}
		return readWriteConstraints;
	}

	private boolean constraintsForUpdateNotLoaded() {
		return !constraintsForUpdateLoaded;
	}

	private void collectAllConstraints() {
		constraintsForUpdateLoaded = true;
		for (DataGroup childReference : getAllChildReferences()) {
			possiblyAddConstraint(childReference);
		}
	}

	private List<DataGroup> getAllChildReferences() {
		DataGroup metadataGroupForMetadata = getMetadataGroup();
		DataGroup childReferences = metadataGroupForMetadata
				.getFirstGroupWithNameInData("childReferences");
		return childReferences.getAllGroupsWithNameInData("childReference");
	}

	private void possiblyAddConstraint(DataGroup childReference) {
		if (hasConstraints(childReference)) {
			addWriteAndReadWriteConstraints(childReference);
		}
	}

	private boolean hasConstraints(DataGroup childReference) {
		return childReference.containsChildWithNameInData(RECORD_PART_CONSTRAINT);
	}

	private void addWriteAndReadWriteConstraints(DataGroup childReference) {
		String constraintType = getRecordPartConstraintType(childReference);

		Constraint constraint = createConstraintPossibyAddAttributes(childReference);
		writeConstraints.add(constraint);
		possiblyAddReadWriteConstraint(constraintType, constraint);
	}

	private Constraint createConstraintPossibyAddAttributes(DataGroup childReference) {
		DataGroup childRef = getChildRef(childReference);
		Constraint constraint = createConstraint(childRef);
		possiblyAddAttributes(childRef, constraint);
		return constraint;
	}

	private String getRecordPartConstraintType(DataGroup childReference) {
		return childReference.getFirstAtomicValueWithNameInData(RECORD_PART_CONSTRAINT);
	}

	private DataGroup getChildRef(DataGroup childReference) {
		DataGroup ref = childReference.getFirstGroupWithNameInData("ref");
		String linkedRecordType = ref.getFirstAtomicValueWithNameInData(LINKED_RECORD_TYPE);
		String linkedRecordId = ref.getFirstAtomicValueWithNameInData(LINKED_RECORD_ID);
		return recordStorage.read(linkedRecordType, linkedRecordId);
	}

	private Constraint createConstraint(DataGroup childRef) {
		String refNameInData = childRef.getFirstAtomicValueWithNameInData(NAME_IN_DATA);
		return new Constraint(refNameInData);
	}

	private void possiblyAddAttributes(DataGroup childRef, Constraint constraint) {
		if (childRef.containsChildWithNameInData("attributeReferences")) {
			addAttributes(childRef, constraint);
		}
	}

	private void addAttributes(DataGroup childRef, Constraint constraint) {
		DataGroup attributeReferences = childRef.getFirstGroupWithNameInData("attributeReferences");
		List<DataGroup> attributes = attributeReferences.getAllGroupsWithNameInData("ref");

		for (DataGroup attribute : attributes) {
			addAttribute(constraint, attribute);
		}
	}

	private void addAttribute(Constraint constraint, DataGroup attribute) {
		DataGroup collectionVar = getCollectionVar(attribute);
		DataAttribute dataAttribute = createDataAttribute(collectionVar);
		constraint.addAttribute(dataAttribute);
	}

	private DataGroup getCollectionVar(DataGroup attribute) {
		return recordStorage.read(attribute.getFirstAtomicValueWithNameInData(LINKED_RECORD_TYPE),
				attribute.getFirstAtomicValueWithNameInData(LINKED_RECORD_ID));
	}

	private DataAttribute createDataAttribute(DataGroup collectionVar) {
		String attributeName = collectionVar.getFirstAtomicValueWithNameInData(NAME_IN_DATA);
		String attributeValue = collectionVar.getFirstAtomicValueWithNameInData("finalValue");
		return DataAttributeProvider.getDataAttributeUsingNameInDataAndValue(attributeName,
				attributeValue);
	}

	private void possiblyAddReadWriteConstraint(String constraintValue, Constraint constraint) {
		if (isReadWriteConstraint(constraintValue)) {
			readWriteConstraints.add(constraint);
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
	public Set<Constraint> getRecordPartWriteConstraints() {
		if (constraintsForUpdateNotLoaded()) {
			collectAllConstraints();
		}
		return writeConstraints;
	}

	public String getRecordTypeId() {
		// needed for test
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
		return SEARCH.equals(id);
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
		return recordType.containsChildWithNameInData(SEARCH);
	}

	@Override
	public String getSearchId() {
		throwErrorIfNoSearch();
		DataGroup searchGroup = recordType.getFirstGroupWithNameInData(SEARCH);
		return searchGroup.getFirstAtomicValueWithNameInData(LINKED_RECORD_ID);
	}

	private void throwErrorIfNoSearch() {
		if (!hasLinkedSearch()) {
			throw new DataMissingException("Unable to get searchId, no search exists");
		}
	}

	@Override
	public Set<Constraint> getRecordPartCreateWriteConstraints() {
		if (constraintsForCreateNotLoaded()) {
			collectAllConstraintsForCreate();
		}
		return createConstraints;
	}

	private boolean constraintsForCreateNotLoaded() {
		return !constraintsForCreateLoaded;
	}

	private void collectAllConstraintsForCreate() {
		constraintsForCreateLoaded = true;
		for (DataGroup childReference : getAllChildReferencesForCreate()) {
			possiblyAddCreateConstraint(childReference);
		}
	}

	private List<DataGroup> getAllChildReferencesForCreate() {
		DataGroup metadataGroupForMetadata = getNewMetadataGroup();
		DataGroup childReferences = metadataGroupForMetadata
				.getFirstGroupWithNameInData("childReferences");
		return childReferences.getAllGroupsWithNameInData("childReference");
	}

	private DataGroup getNewMetadataGroup() {
		return recordStorage.read("metadataGroup", getNewMetadataId());
	}

	private void possiblyAddCreateConstraint(DataGroup childReference) {
		if (hasConstraints(childReference)) {
			addCreateConstraints(childReference);
		}
	}

	private void addCreateConstraints(DataGroup childReference) {
		Constraint constraint = createConstraintPossibyAddAttributes(childReference);
		createConstraints.add(constraint);
	}

	@Override
	public boolean hasRecordPartCreateConstraint() {
		return !getRecordPartCreateWriteConstraints().isEmpty();
	}

}
