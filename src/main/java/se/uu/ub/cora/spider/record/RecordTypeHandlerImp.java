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
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import se.uu.ub.cora.bookkeeper.metadata.Constraint;
import se.uu.ub.cora.bookkeeper.metadata.ConstraintType;
import se.uu.ub.cora.data.DataAttribute;
import se.uu.ub.cora.data.DataAttributeProvider;
import se.uu.ub.cora.data.DataGroup;
import se.uu.ub.cora.data.DataGroupProvider;
import se.uu.ub.cora.spider.data.DataMissingException;
import se.uu.ub.cora.storage.RecordStorage;
import se.uu.ub.cora.storage.StorageReadResult;

public final class RecordTypeHandlerImp implements RecordTypeHandler {
	private static final String METADATA_GROUP = "metadataGroup";
	private static final String REPEAT_MAX_WHEN_NOT_REPEATEBLE = "1";
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
	private RecordTypeHandlerFactory recordTypeHandlerFactory;
	private Set<String> readChildren = new HashSet<>();

	public static RecordTypeHandler usingRecordStorageAndRecordTypeId(
			RecordTypeHandlerFactory recordTypeHandlerFactory, RecordStorage recordStorage,
			String recordTypeId) {
		return new RecordTypeHandlerImp(recordTypeHandlerFactory, recordStorage, recordTypeId);
	}

	private RecordTypeHandlerImp(RecordTypeHandlerFactory recordTypeHandlerFactory,
			RecordStorage recordStorage, String recordTypeId) {
		this.recordTypeHandlerFactory = recordTypeHandlerFactory;
		this.recordStorage = recordStorage;
		this.recordTypeId = recordTypeId;
		recordType = recordStorage.read(RECORD_TYPE, recordTypeId);
	}

	private RecordTypeHandlerImp(RecordTypeHandlerFactory recordTypeHandlerFactory,
			RecordStorage recordStorage, DataGroup dataGroup) {
		this.recordTypeHandlerFactory = recordTypeHandlerFactory;
		this.recordStorage = recordStorage;
		recordType = dataGroup;
		recordTypeId = getIdFromMetadatagGroup(dataGroup);
	}

	public static RecordTypeHandler usingRecordStorageAndDataGroup(
			RecordTypeHandlerFactory recordTypeHandlerFactory, RecordStorage recordStorage,
			DataGroup dataGroup) {
		return new RecordTypeHandlerImp(recordTypeHandlerFactory, recordStorage, dataGroup);
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
	public List<String> getCombinedIdsUsingRecordId(String recordId) {
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
			metadataGroup = recordStorage.read(METADATA_GROUP, getMetadataId());
		}
		return metadataGroup;
	}

	@Override
	public Set<Constraint> getRecordPartReadConstraints() {
		if (constraintsForUpdateNotLoaded()) {
			collectAllConstraintsForUpdate();
		}
		return readWriteConstraints;
	}

	private boolean constraintsForUpdateNotLoaded() {
		return !constraintsForUpdateLoaded;
	}

	private void collectAllConstraintsForUpdate() {
		constraintsForUpdateLoaded = true;
		List<DataGroup> allChildReferences = getAllChildReferences(getMetadataGroup());
		Set<Constraint> collectedConstraints = new HashSet<>();
		collectConstraintsForChildReferences(allChildReferences, collectedConstraints);
		for (Constraint constraint : collectedConstraints) {
			writeConstraints.add(constraint);
			possiblyAddReadWriteConstraint(constraint);
		}
	}

	private void collectConstraintsForChildReferences(List<DataGroup> allChildReferences,
			Set<Constraint> tempSet) {
		for (DataGroup childReference : allChildReferences) {
			collectConstraintForChildReference(childReference, tempSet);
		}
	}

	private void collectConstraintForChildReference(DataGroup childReference,
			Set<Constraint> tempSet) {
		DataGroup childRef = null;
		if (hasConstraints(childReference)) {
			childRef = readChildRefFromStorage(childReference);
			addWriteAndReadWriteConstraints(childReference, childRef, tempSet);
		}
		possiblyCollectConstraintsFromChildrenToChildReference(childReference, childRef, tempSet);
	}

	private void possiblyCollectConstraintsFromChildrenToChildReference(DataGroup childReference,
			DataGroup childRef, Set<Constraint> tempSet) {
		String repeatMax = getRepeatMax(childReference);
		String linkedRecordType = getLinkedRecordType(childReference);
		if (isGroup(linkedRecordType) && notRepetable(repeatMax)) {
			childRef = ensureChildRefReadFromStorage(childReference, childRef);
			List<DataGroup> allChildReferences = getAllChildReferences(childRef);
			collectConstraintsForChildReferences(allChildReferences, tempSet);
		}
	}

	private String getRepeatMax(DataGroup childReference) {
		return childReference.getFirstAtomicValueWithNameInData("repeatMax");
	}

	private String getLinkedRecordType(DataGroup childReference) {
		DataGroup ref = childReference.getFirstGroupWithNameInData("ref");
		return ref.getFirstAtomicValueWithNameInData(LINKED_RECORD_TYPE);
	}

	private boolean isGroup(String linkedRecordType) {
		return METADATA_GROUP.equals(linkedRecordType);
	}

	private boolean notRepetable(String repeatMax) {
		return REPEAT_MAX_WHEN_NOT_REPEATEBLE.equals(repeatMax);
	}

	private DataGroup ensureChildRefReadFromStorage(DataGroup childReference, DataGroup childRef) {
		if (childRef == null) {
			childRef = readChildRefFromStorage(childReference);
		}
		return childRef;
	}

	private List<DataGroup> getAllChildReferences(DataGroup metadataGroupForMetadata) {
		String id = getIdFromMetadatagGroup(metadataGroupForMetadata);
		if (childrenToGroupHasAlreadyBeenChecked(id)) {
			return Collections.emptyList();
		}
		return addGroupToCheckedAndGetChildReferences(metadataGroupForMetadata, id);
	}

	private boolean childrenToGroupHasAlreadyBeenChecked(String id) {
		return readChildren.contains(id);
	}

	private List<DataGroup> addGroupToCheckedAndGetChildReferences(
			DataGroup metadataGroupForMetadata, String id) {
		readChildren.add(id);
		DataGroup childReferences = metadataGroupForMetadata
				.getFirstGroupWithNameInData("childReferences");
		return childReferences.getAllGroupsWithNameInData("childReference");
	}

	private String getIdFromMetadatagGroup(DataGroup metadataGroupForMetadata) {
		DataGroup recordInfo = metadataGroupForMetadata.getFirstGroupWithNameInData("recordInfo");
		return recordInfo.getFirstAtomicValueWithNameInData("id");
	}

	private boolean hasConstraints(DataGroup childReference) {
		return childReference.containsChildWithNameInData(RECORD_PART_CONSTRAINT);
	}

	private void addWriteAndReadWriteConstraints(DataGroup childReference, DataGroup childRef,
			Set<Constraint> constraints) {
		String constraintType = getRecordPartConstraintType(childReference);

		Constraint constraint = createConstraintPossibyAddAttributes(childRef);
		constraint.setType(ConstraintType.fromString(constraintType));
		constraints.add(constraint);
	}

	private Constraint createConstraintPossibyAddAttributes(DataGroup childRef) {
		Constraint constraint = createConstraint(childRef);
		possiblyAddAttributes(childRef, constraint);
		return constraint;
	}

	private String getRecordPartConstraintType(DataGroup childReference) {
		return childReference.getFirstAtomicValueWithNameInData(RECORD_PART_CONSTRAINT);
	}

	private DataGroup readChildRefFromStorage(DataGroup childReference) {
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

	private void possiblyAddReadWriteConstraint(Constraint constraint) {
		if (isReadWriteConstraint(constraint.getType())) {
			readWriteConstraints.add(constraint);
		}
	}

	private boolean isReadWriteConstraint(ConstraintType constraintType) {
		return ConstraintType.READ_WRITE.equals(constraintType);
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
			collectAllConstraintsForUpdate();
		}
		return writeConstraints;
	}

	@Override
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

		List<DataGroup> allChildReferences = getAllChildReferencesForCreate();
		Set<Constraint> collectedConstraints = new HashSet<>();
		collectConstraintsForChildReferences(allChildReferences, collectedConstraints);
		for (Constraint constraint : collectedConstraints) {
			createConstraints.add(constraint);
		}
	}

	private List<DataGroup> getAllChildReferencesForCreate() {
		DataGroup metadataGroupForMetadata = getNewMetadataGroup();
		DataGroup childReferences = metadataGroupForMetadata
				.getFirstGroupWithNameInData("childReferences");
		return childReferences.getAllGroupsWithNameInData("childReference");
	}

	private DataGroup getNewMetadataGroup() {
		return recordStorage.read(METADATA_GROUP, getNewMetadataId());
	}

	@Override
	public boolean hasRecordPartCreateConstraint() {
		return !getRecordPartCreateWriteConstraints().isEmpty();
	}

	@Override
	public List<RecordTypeHandler> getImplementingRecordTypeHandlers() {
		if (isAbstract()) {
			return createListOfImplementingRecordTypeHandlers();
		}
		return Collections.emptyList();
	}

	private List<RecordTypeHandler> createListOfImplementingRecordTypeHandlers() {
		List<RecordTypeHandler> list = new ArrayList<>();
		StorageReadResult recordTypeList = getRecordTypeListFromStorage();
		for (DataGroup dataGroup : recordTypeList.listOfDataGroups) {
			addIfChildToCurrent(list, dataGroup);
		}
		return list;
	}

	private StorageReadResult getRecordTypeListFromStorage() {
		return recordStorage.readList(RECORD_TYPE,
				DataGroupProvider.getDataGroupUsingNameInData("filter"));
	}

	private void addIfChildToCurrent(List<RecordTypeHandler> list, DataGroup dataGroup) {
		RecordTypeHandler recordTypeHandler = recordTypeHandlerFactory
				.factorUsingDataGroup(dataGroup);
		if (currentRecordTypeIsParentTo(recordTypeHandler)) {
			list.add(recordTypeHandler);
		}
	}

	private boolean currentRecordTypeIsParentTo(RecordTypeHandler recordTypeHandler) {
		return recordTypeHandler.hasParent()
				&& recordTypeHandler.getParentId().equals(recordTypeId);
	}

	public RecordTypeHandlerFactory getRecordTypeHandlerFactory() {
		return recordTypeHandlerFactory;
	}

}
