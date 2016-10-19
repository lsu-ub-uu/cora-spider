/*
 * Copyright 2016 Uppsala University Library
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

package se.uu.ub.cora.spider.consistency;

import java.util.Collection;

import se.uu.ub.cora.bookkeeper.data.DataAtomic;
import se.uu.ub.cora.bookkeeper.data.DataElement;
import se.uu.ub.cora.bookkeeper.data.DataGroup;
import se.uu.ub.cora.spider.data.SpiderDataGroup;
import se.uu.ub.cora.spider.record.DataException;
import se.uu.ub.cora.spider.record.storage.RecordNotFoundException;
import se.uu.ub.cora.spider.record.storage.RecordStorage;

public class MetadataConsistencyGroupAndCollectionValidatorImp
		implements MetadataConsistencyValidator {
	private static final String LINKED_RECORD_ID = "linkedRecordId";
	private static final String RECORD_TYPE = "recordType";
	private static final String PARENT_ID = "parentId";
	private static final String REF_PARENT_ID = "refParentId";
	private RecordStorage recordStorage;
	private String recordType;
	private DataGroup recordAsDataGroup;

	public MetadataConsistencyGroupAndCollectionValidatorImp(RecordStorage recordStorage,
			String recordType) {
		this.recordStorage = recordStorage;
		this.recordType = recordType;
	}

	@Override
	public void validateRules(SpiderDataGroup recordAsSpiderDataGroup) {
		recordAsDataGroup = recordAsSpiderDataGroup.toDataGroup();
		if (dataGroupHasParent()) {
			validateInheritanceRules();
		}
		if (recordTypeIsMetadataCollectionVariable()) {
			possiblyValidateFinalValue();
		}
	}

	private void validateInheritanceRules() {
		if (recordTypeIsMetadataGroup()) {
			ensureAllChildrenExistsInParent();
		} else if (recordTypeIsMetadataCollectionVariable()) {
			ensureAllCollectionItemsExistInParent();
		}
	}

	private boolean recordTypeIsMetadataGroup() {
		return "metadataGroup".equals(recordType);
	}

	private boolean dataGroupHasParent() {
		return recordAsDataGroup.containsChildWithNameInData(REF_PARENT_ID);
	}

	private void ensureAllChildrenExistsInParent() {
		DataGroup childReferences = (DataGroup) recordAsDataGroup
				.getFirstChildWithNameInData("childReferences");

		for (DataElement childReference : childReferences.getChildren()) {
			String childNameInData = getNameInDataFromChildReference(childReference);
			if (!ensureChildExistInParent(childNameInData)) {
				throw new DataException("Data is not valid: child does not exist in parent");
			}
		}
	}

	protected String getNameInDataFromChildReference(DataElement childReference) {
		DataGroup childReferenceGroup = (DataGroup) childReference;
		String refId = childReferenceGroup.extractAtomicValue("ref");
		DataGroup childDataGroup = findChildOfUnknownMetadataType(refId);

		DataAtomic nameInData = (DataAtomic) childDataGroup
				.getFirstChildWithNameInData("nameInData");
		return nameInData.getValue();
	}

	protected DataGroup findChildOfUnknownMetadataType(String refId) {
		Collection<DataGroup> recordTypes = recordStorage.readList(RECORD_TYPE);

		for (DataGroup recordTypePossibleChild : recordTypes) {
			DataGroup childDataGroup = findChildInMetadata(refId, recordTypePossibleChild);
			if (childDataGroup != null) {
				return childDataGroup;
			}
		}
		throw new DataException("Data is not valid: referenced child does not exist");
	}

	protected DataGroup findChildInMetadata(String refId, DataGroup recordTypePossibleChild) {
		DataGroup childDataGroup = null;
		if (isChildOfAbstractRecordType("metadata", recordTypePossibleChild)) {
			String id = extractIdFromRecordInfo(recordTypePossibleChild);
			childDataGroup = tryReadChildFromStorage(refId, id);
		}
		return childDataGroup;
	}

	protected boolean isChildOfAbstractRecordType(String abstractRecordType,
			DataGroup recordTypePossibleChild) {
		if (handledRecordHasParent(recordTypePossibleChild)) {
			String parentIdValue = recordTypePossibleChild
					.getFirstAtomicValueWithNameInData(PARENT_ID);
			if (parentIdValue.equals(abstractRecordType)) {
				return true;
			}
		}
		return false;
	}

	private boolean handledRecordHasParent(DataGroup handledRecordTypeDataGroup) {
		return handledRecordTypeDataGroup.containsChildWithNameInData(PARENT_ID);
	}

	protected String extractIdFromRecordInfo(DataGroup recordTypePossibleChild) {
		DataGroup recordInfo = (DataGroup) recordTypePossibleChild
				.getFirstChildWithNameInData("recordInfo");
		return recordInfo.getFirstAtomicValueWithNameInData("id");
	}

	protected DataGroup tryReadChildFromStorage(String refId, String id) {
		DataGroup childDataGroup;
		try {
			childDataGroup = recordStorage.read(id, refId);
			return childDataGroup;
		} catch (RecordNotFoundException exception) {
			return null;
		}
	}

	protected boolean ensureChildExistInParent(String childNameInData) {
		DataGroup parentChildReferences = getParentChildReferences();
		for (DataElement parentChildReference : parentChildReferences.getChildren()) {
			if (isSameNameInData(childNameInData, parentChildReference)) {
				return true;
			}
		}
		return false;
	}

	protected DataGroup getParentChildReferences() {
		DataAtomic refParentId = (DataAtomic) recordAsDataGroup
				.getFirstChildWithNameInData(REF_PARENT_ID);
		DataGroup parent = recordStorage.read("metadataGroup", refParentId.getValue());

		return (DataGroup) parent.getFirstChildWithNameInData("childReferences");
	}

	protected boolean isSameNameInData(String childNameInData, DataElement parentChildReference) {
		String parentChildNameInData = getNameInDataFromChildReference(parentChildReference);
		return childNameInData.equals(parentChildNameInData);
	}

	private boolean recordTypeIsMetadataCollectionVariable() {
		return "metadataCollectionVariable".equals(recordType);
	}

	private void ensureAllCollectionItemsExistInParent() {
		DataGroup references = getItemReferences();
		DataGroup parentReferences = extractParentItemReferences();

		for (DataElement itemReference : references.getChildren()) {
			String childItemId = extractRefItemIdFromRefItemGroup(itemReference);
			if (!ensureChildItemExistsInParent(childItemId, parentReferences)) {
				throw new DataException("Data is not valid: childItem: " + childItemId
						+ " does not exist in parent");
			}
		}
	}

	private DataGroup getItemReferences() {
		DataGroup refCollection = (DataGroup) recordAsDataGroup
				.getFirstChildWithNameInData("refCollection");
		String refCollectionId = refCollection.extractAtomicValue(LINKED_RECORD_ID);
		return readItemCollectionAndExtractCollectionItemReferences(refCollectionId);
	}

	private DataGroup extractParentItemReferences() {
		String refParentId = recordAsDataGroup.extractAtomicValue(REF_PARENT_ID);
		DataGroup parentCollectionVar = recordStorage.read("metadataCollectionVariable",
				refParentId);
		DataGroup parentRefCollection = (DataGroup) parentCollectionVar
				.getFirstChildWithNameInData("refCollection");
		String parentRefCollectionId = parentRefCollection
				.getFirstAtomicValueWithNameInData(LINKED_RECORD_ID);

		return readItemCollectionAndExtractCollectionItemReferences(parentRefCollectionId);
	}

	private DataGroup readItemCollectionAndExtractCollectionItemReferences(String refCollectionId) {
		DataGroup refCollection = recordStorage.read("metadataItemCollection", refCollectionId);
		return (DataGroup) refCollection.getFirstChildWithNameInData("collectionItemReferences");
	}

	private String extractRefItemIdFromRefItemGroup(DataElement itemReference) {
		DataGroup childItem = (DataGroup) itemReference;
		return childItem.getFirstAtomicValueWithNameInData("linkedRecordId");
	}

	private boolean ensureChildItemExistsInParent(String childItemId, DataGroup parentReferences) {
		for (DataElement itemReference : parentReferences.getChildren()) {
			String parentItemId = extractRefItemIdFromRefItemGroup(itemReference);
			if (isParentItemSameAsChildItem(childItemId, parentItemId)) {
				return true;
			}
		}
		return false;
	}

	private boolean isParentItemSameAsChildItem(String childItemId, String parentItem) {
		return parentItem.equals(childItemId);
	}

	private void possiblyValidateFinalValue() {
		if (hasFinalValue()) {
			String finalValue = recordAsDataGroup.extractAtomicValue("finalValue");
			if (!validateFinalValue(finalValue)) {
				throw new DataException(
						"Data is not valid: final value does not exist in collection");
			}
		}
	}

	private boolean hasFinalValue() {
		return recordAsDataGroup.containsChildWithNameInData("finalValue");
	}

	private boolean validateFinalValue(String finalValue) {
		DataGroup references = getItemReferences();
		for (DataElement reference : references.getChildren()) {
			String itemNameInData = extractNameInDataFromReference(reference);
			if (finalValue.equals(itemNameInData)) {
				return true;
			}
		}
		return false;
	}

	private String extractNameInDataFromReference(DataElement reference) {
		String itemId = extractRefItemIdFromRefItemGroup(reference);
		DataGroup collectionItem = recordStorage.read("metadataCollectionItem", itemId);
		return collectionItem.getFirstAtomicValueWithNameInData("nameInData");
	}

	protected void checkToPartOfLinkedDataExistsInStorage(DataGroup collectedLinks) {
		for (DataElement dataElement : collectedLinks.getChildren()) {
			extractToGroupAndCheckDataExistsInStorage((DataGroup) dataElement);
		}
	}

	private void extractToGroupAndCheckDataExistsInStorage(DataGroup dataElement) {
		DataGroup to = extractToGroupFromRecordLink(dataElement);
		String toRecordId = extractAtomicValueFromGroup(LINKED_RECORD_ID, to);
		String toRecordType = extractAtomicValueFromGroup("linkedRecordType", to);
		checkRecordTypeAndRecordIdExistsInStorage(toRecordId, toRecordType);
	}

	private String extractAtomicValueFromGroup(String nameInDataToExtract, DataGroup to) {
		return to.getFirstAtomicValueWithNameInData(nameInDataToExtract);
	}

	private DataGroup extractToGroupFromRecordLink(DataGroup recordToRecordLink) {
		return recordToRecordLink.getFirstGroupWithNameInData("to");
	}

	private void checkRecordTypeAndRecordIdExistsInStorage(String recordId, String recordType) {
		if (!recordStorage.recordExistsForRecordTypeAndRecordId(recordType, recordId)) {
			throw new DataException(
					"Data is not valid: linkedRecord does not exists in storage for recordType: "
							+ recordType + " and recordId: " + recordId);
		}
	}
}
