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
import se.uu.ub.cora.spider.data.SpiderDataAtomic;
import se.uu.ub.cora.spider.data.SpiderDataElement;
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
	private SpiderDataGroup recordAsSpiderDataGroup;

	public MetadataConsistencyGroupAndCollectionValidatorImp(RecordStorage recordStorage,
			String recordType) {
		this.recordStorage = recordStorage;
		this.recordType = recordType;
	}

	@Override
	public void validateRules(SpiderDataGroup recordAsSpiderDataGroup) {
		this.recordAsSpiderDataGroup = recordAsSpiderDataGroup;
		if (dataGroupHasParent()) {
			validateInheritanceRules();
		}
		if (recordTypeIsMetadataCollectionVariable()) {
			possiblyValidateFinalValue();
		}
	}

	private boolean dataGroupHasParent() {
		return recordAsSpiderDataGroup.containsChildWithNameInData(REF_PARENT_ID);
	}

	private void validateInheritanceRules() {
		if (recordTypeIsMetadataGroup()) {
			ensureAllChildrenExistsInParent();
		} else if (recordTypeIsMetadataCollectionVariable()) {
			ensureAllCollectionItemsExistInParent();
		}
	}

	private boolean recordTypeIsMetadataCollectionVariable() {
		return "metadataCollectionVariable".equals(recordType);
	}

	private void possiblyValidateFinalValue() {
		if (hasFinalValue()) {
			String finalValue = recordAsSpiderDataGroup.extractAtomicValue("finalValue");
			if (!validateFinalValue(finalValue)) {
				throw new DataException(
						"Data is not valid: final value does not exist in collection");
			}
		}
	}

	private boolean recordTypeIsMetadataGroup() {
		return "metadataGroup".equals(recordType);
	}

	private void ensureAllChildrenExistsInParent() {
		SpiderDataGroup childReferences = (SpiderDataGroup) recordAsSpiderDataGroup
				.getFirstChildWithNameInData("childReferences");

		for (SpiderDataElement childReference : childReferences.getChildren()) {
			String childNameInData = getNameInDataFromChildReference(childReference);
			if (!ensureChildExistInParent(childNameInData)) {
				throw new DataException("Data is not valid: child does not exist in parent");
			}
		}
	}

	private String getNameInDataFromChildReference(SpiderDataElement childReference) {
		SpiderDataGroup childReferenceGroup = (SpiderDataGroup) childReference;
		String refId = childReferenceGroup.extractAtomicValue("ref");
		DataGroup childDataGroup = findChildOfUnknownMetadataType(refId);

		DataAtomic nameInData = (DataAtomic) childDataGroup
				.getFirstChildWithNameInData("nameInData");
		return nameInData.getValue();
	}

	private DataGroup findChildOfUnknownMetadataType(String refId) {
		Collection<DataGroup> recordTypes = recordStorage.readList(RECORD_TYPE);

		for (DataGroup recordTypePossibleChild : recordTypes) {
			DataGroup childDataGroup = findChildInMetadata(refId, recordTypePossibleChild);
			if (childDataGroup != null) {
				return childDataGroup;
			}
		}
		throw new DataException("Data is not valid: referenced child does not exist");
	}

	private DataGroup findChildInMetadata(String refId, DataGroup recordTypePossibleChild) {
		DataGroup childDataGroup = null;
		if (isChildOfAbstractRecordType("metadata", recordTypePossibleChild)) {
			String id = extractIdFromRecordInfo(recordTypePossibleChild);
			childDataGroup = tryReadChildFromStorage(refId, id);
		}
		return childDataGroup;
	}

	private boolean isChildOfAbstractRecordType(String abstractRecordType,
			DataGroup recordTypePossibleChild) {
		String parentIdValue = recordTypePossibleChild.getFirstAtomicValueWithNameInData(PARENT_ID);
		if (parentIdValue.equals(abstractRecordType)) {
			return true;
		}
		return false;
	}

	private String extractIdFromRecordInfo(DataGroup recordTypePossibleChild) {
		DataGroup recordInfo = (DataGroup) recordTypePossibleChild
				.getFirstChildWithNameInData("recordInfo");
		return recordInfo.getFirstAtomicValueWithNameInData("id");
	}

	private DataGroup tryReadChildFromStorage(String refId, String id) {
		DataGroup childDataGroup;
		try {
			childDataGroup = recordStorage.read(id, refId);
			return childDataGroup;
		} catch (RecordNotFoundException exception) {
			return null;
		}
	}

	private boolean ensureChildExistInParent(String childNameInData) {
		SpiderDataGroup parentChildReferences = getParentChildReferences();
		for (SpiderDataElement parentChildReference : parentChildReferences.getChildren()) {
			if (isSameNameInData(childNameInData, parentChildReference)) {
				return true;
			}
		}
		return false;
	}

	private SpiderDataGroup getParentChildReferences() {
		SpiderDataAtomic refParentId = (SpiderDataAtomic) recordAsSpiderDataGroup
				.getFirstChildWithNameInData(REF_PARENT_ID);
		SpiderDataGroup parent = SpiderDataGroup
				.fromDataGroup(recordStorage.read("metadataGroup", refParentId.getValue()));

		return (SpiderDataGroup) parent.getFirstChildWithNameInData("childReferences");
	}

	private boolean isSameNameInData(String childNameInData,
			SpiderDataElement parentChildReference) {
		String parentChildNameInData = getNameInDataFromChildReference(parentChildReference);
		return childNameInData.equals(parentChildNameInData);
	}

	private void ensureAllCollectionItemsExistInParent() {
		DataGroup references = getItemReferences();
		DataGroup parentReferences = extractParentItemReferences();

		for (DataElement itemReference : references.getChildren()) {
			DataAtomic childItem = (DataAtomic) itemReference;
			if (!ensureChildItemExistsInParent(childItem, parentReferences)) {
				throw new DataException("Data is not valid: childItem: " + childItem.getValue()
						+ " does not exist in parent");
			}
		}
	}

	private DataGroup getItemReferences() {
		SpiderDataGroup refCollection = (SpiderDataGroup) recordAsSpiderDataGroup
				.getFirstChildWithNameInData("refCollection");
		String refCollectionId = refCollection.extractAtomicValue(LINKED_RECORD_ID);
		return readItemCollectionAndExtractCollectionItemReferences(refCollectionId);
	}

	private DataGroup readItemCollectionAndExtractCollectionItemReferences(String refCollectionId) {
		DataGroup refCollection = recordStorage.read("metadataItemCollection", refCollectionId);
		return (DataGroup) refCollection.getFirstChildWithNameInData("collectionItemReferences");
	}

	private DataGroup extractParentItemReferences() {
		String refParentId = recordAsSpiderDataGroup.extractAtomicValue(REF_PARENT_ID);
		DataGroup parentCollectionVar = recordStorage.read("metadataCollectionVariable",
				refParentId);
		DataGroup parentRefCollection = (DataGroup) parentCollectionVar
				.getFirstChildWithNameInData("refCollection");
		String parentRefCollectionId = parentRefCollection
				.getFirstAtomicValueWithNameInData(LINKED_RECORD_ID);

		return readItemCollectionAndExtractCollectionItemReferences(parentRefCollectionId);
	}

	private boolean ensureChildItemExistsInParent(DataAtomic childItem,
			DataGroup parentReferences) {
		for (DataElement itemReference : parentReferences.getChildren()) {
			DataAtomic parentItem = (DataAtomic) itemReference;
			if (isParentItemSameAsChildItem(childItem, parentItem)) {
				return true;
			}
		}
		return false;
	}

	private boolean isParentItemSameAsChildItem(DataAtomic childItem, DataAtomic parentItem) {
		return parentItem.getValue().equals(childItem.getValue());
	}

	private boolean hasFinalValue() {
		return recordAsSpiderDataGroup.containsChildWithNameInData("finalValue");
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
		DataAtomic itemReference = (DataAtomic) reference;
		DataGroup collectionItem = recordStorage.read("metadataCollectionItem",
				itemReference.getValue());
		return collectionItem.getFirstAtomicValueWithNameInData("nameInData");
	}

}
