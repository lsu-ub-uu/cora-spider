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

import se.uu.ub.cora.bookkeeper.data.DataAtomic;
import se.uu.ub.cora.bookkeeper.data.DataElement;
import se.uu.ub.cora.bookkeeper.data.DataGroup;
import se.uu.ub.cora.spider.data.SpiderDataGroup;
import se.uu.ub.cora.spider.record.DataException;
import se.uu.ub.cora.spider.record.storage.RecordNotFoundException;
import se.uu.ub.cora.spider.record.storage.RecordStorage;

public class MetadataConsistencyGroupAndCollectionValidatorImp implements MetadataConsistencyValidator {
	private static final String LINKED_RECORD_ID = "linkedRecordId";
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
				throw new DataException("Data is not valid: childItem: " + childNameInData
						+ " does not exist in parent");
			}
		}
	}

	protected String getNameInDataFromChildReference(DataElement childReference) {
		DataGroup childReferenceGroup = (DataGroup) childReference;

		DataGroup ref = childReferenceGroup.getFirstGroupWithNameInData("ref");
		String linkedRecordType = ref.getFirstAtomicValueWithNameInData("linkedRecordType");
		String linkedRecordId = ref.getFirstAtomicValueWithNameInData(LINKED_RECORD_ID);
		DataGroup childDataGroup;
		try {
			childDataGroup = recordStorage.read(linkedRecordType, linkedRecordId);
		} catch (RecordNotFoundException exception) {
			throw new DataException("Data is not valid: referenced child:  does not exist");
		}
		DataAtomic nameInData = (DataAtomic) childDataGroup.getFirstChildWithNameInData("nameInData");
		return nameInData.getValue();
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
		String refParentId = extractParentId();
		DataGroup parent = recordStorage.read("metadataGroup", refParentId);

		return (DataGroup) parent.getFirstChildWithNameInData("childReferences");
	}

	private String extractParentId() {
		DataGroup refParentGroup = recordAsDataGroup.getFirstGroupWithNameInData(REF_PARENT_ID);
		return refParentGroup.getFirstAtomicValueWithNameInData(LINKED_RECORD_ID);
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
				throw new DataException(
						"Data is not valid: childItem: " + childItemId + " does not exist in parent");
			}
		}
	}

	private DataGroup getItemReferences() {
		DataGroup refCollection = (DataGroup) recordAsDataGroup
				.getFirstChildWithNameInData("refCollection");
		String refCollectionId = refCollection.getFirstAtomicValueWithNameInData(LINKED_RECORD_ID);
		return readItemCollectionAndExtractCollectionItemReferences(refCollectionId);
	}

	private DataGroup extractParentItemReferences() {
		String refParentId = extractParentId();
		DataGroup parentCollectionVar = recordStorage.read("metadataCollectionVariable", refParentId);
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
		return childItem.getFirstAtomicValueWithNameInData(LINKED_RECORD_ID);
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
			String finalValue = recordAsDataGroup.getFirstAtomicValueWithNameInData("finalValue");
			if (!validateFinalValue(finalValue)) {
				throw new DataException("Data is not valid: final value does not exist in collection");
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
}
