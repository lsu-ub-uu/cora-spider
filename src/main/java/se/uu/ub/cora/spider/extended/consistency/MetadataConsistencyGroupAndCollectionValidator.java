/*
 * Copyright 2016, 2022 Uppsala University Library
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

package se.uu.ub.cora.spider.extended.consistency;

import java.util.List;
import java.util.Optional;

import se.uu.ub.cora.data.DataChild;
import se.uu.ub.cora.data.DataGroup;
import se.uu.ub.cora.data.DataRecordLink;
import se.uu.ub.cora.spider.dependency.SpiderDependencyProvider;
import se.uu.ub.cora.spider.extendedfunctionality.ExtendedFunctionality;
import se.uu.ub.cora.spider.extendedfunctionality.ExtendedFunctionalityData;
import se.uu.ub.cora.spider.record.DataException;
import se.uu.ub.cora.storage.RecordNotFoundException;
import se.uu.ub.cora.storage.RecordStorage;

public class MetadataConsistencyGroupAndCollectionValidator implements ExtendedFunctionality {
	private static final String METADATA = "metadata";
	private static final String REF_PARENT_ID = "refParentId";
	private RecordStorage recordStorage;
	private String recordType;
	private DataGroup recordAsDataGroup;
	private SpiderDependencyProvider dependencyProvider;

	public MetadataConsistencyGroupAndCollectionValidator(
			SpiderDependencyProvider dependencyProvider, String recordType) {
		this.dependencyProvider = dependencyProvider;
		this.recordStorage = dependencyProvider.getRecordStorage();
		this.recordType = recordType;
	}

	@Override
	public void useExtendedFunctionality(ExtendedFunctionalityData data) {
		this.recordAsDataGroup = data.dataGroup;
		if (dataGroupHasParent()) {
			validateInheritanceRules();
		}
		if (dataToHandleIsOfTypeMetadataCollectionVariable()) {
			possiblyValidateFinalValue();
		}
	}

	private void validateInheritanceRules() {
		if (dataToHandleIsOfTypeMetadataGroup()) {
			ensureAllChildrenExistsInParent();
		} else if (dataToHandleIsOfTypeMetadataCollectionVariable()) {
			ensureAllCollectionItemsExistInParent();
		}
	}

	private boolean dataToHandleIsOfTypeMetadataGroup() {
		Optional<String> type = recordAsDataGroup.getAttributeValue("type");
		return type.isPresent() && "group".equals(type.get());
	}

	private boolean dataGroupHasParent() {
		return recordAsDataGroup.containsChildWithNameInData(REF_PARENT_ID);
	}

	private void ensureAllChildrenExistsInParent() {
		DataGroup childReferences = (DataGroup) recordAsDataGroup
				.getFirstChildWithNameInData("childReferences");

		for (DataChild childReference : childReferences.getChildren()) {
			String childNameInData = getNameInDataFromChildReference(childReference);
			if (!ensureChildExistInParent(childNameInData)) {
				throw new DataException("Data is not valid: childItem: " + childNameInData
						+ " does not exist in parent");
			}
		}
	}

	protected String getNameInDataFromChildReference(DataChild childReference) {
		DataGroup childReferenceGroup = (DataGroup) childReference;

		DataRecordLink ref = childReferenceGroup.getFirstChildOfTypeAndName(DataRecordLink.class,
				"ref");
		String linkedRecordId = ref.getLinkedRecordId();
		DataGroup childDataGroup;
		try {
			childDataGroup = recordStorage.read(List.of(METADATA), linkedRecordId);
		} catch (RecordNotFoundException exception) {
			throw new DataException("Data is not valid: referenced child:  does not exist",
					exception);
		}
		return childDataGroup.getFirstAtomicValueWithNameInData("nameInData");
	}

	protected boolean ensureChildExistInParent(String childNameInData) {
		DataGroup parentChildReferences = getParentChildReferences();
		for (DataChild parentChildReference : parentChildReferences.getChildren()) {
			if (isSameNameInData(childNameInData, parentChildReference)) {
				return true;
			}
		}
		return false;
	}

	protected DataGroup getParentChildReferences() {
		String refParentId = extractParentId();
		DataGroup parent = recordStorage.read(List.of(METADATA), refParentId);
		return (DataGroup) parent.getFirstChildWithNameInData("childReferences");
	}

	private String extractParentId() {
		DataRecordLink ref = recordAsDataGroup.getFirstChildOfTypeAndName(DataRecordLink.class,
				REF_PARENT_ID);
		return ref.getLinkedRecordId();
	}

	protected boolean isSameNameInData(String childNameInData, DataChild parentChildReference) {
		String parentChildNameInData = getNameInDataFromChildReference(parentChildReference);
		return childNameInData.equals(parentChildNameInData);
	}

	private boolean dataToHandleIsOfTypeMetadataCollectionVariable() {
		Optional<String> type = recordAsDataGroup.getAttributeValue("type");
		return type.isPresent() && "collectionVariable".equals(type.get());
	}

	private void ensureAllCollectionItemsExistInParent() {
		DataGroup references = getItemReferencesFromLinkedItemCollection();
		DataGroup parentReferences = extractParentItemReferences();

		for (DataChild itemReference : references.getChildren()) {
			String childItemId = ((DataRecordLink) itemReference).getLinkedRecordId();
			if (!ensureChildItemExistsInParent(childItemId, parentReferences)) {
				throw new DataException("Data is not valid: childItem: " + childItemId
						+ " does not exist in parent");
			}
		}
	}

	private DataGroup getItemReferencesFromLinkedItemCollection() {
		DataRecordLink refCollection = recordAsDataGroup
				.getFirstChildOfTypeAndName(DataRecordLink.class, "refCollection");
		String refCollectionId = refCollection.getLinkedRecordId();
		return readItemCollectionAndExtractCollectionItemReferences(refCollectionId);
	}

	private DataGroup extractParentItemReferences() {
		String refParentId = extractParentId();
		DataGroup parentCollectionVar = recordStorage.read(List.of(METADATA), refParentId);
		DataRecordLink parentRefCollection = parentCollectionVar
				.getFirstChildOfTypeAndName(DataRecordLink.class, "refCollection");
		String parentRefCollectionId = parentRefCollection.getLinkedRecordId();

		return readItemCollectionAndExtractCollectionItemReferences(parentRefCollectionId);
	}

	private DataGroup readItemCollectionAndExtractCollectionItemReferences(String refCollectionId) {
		DataGroup refCollection = recordStorage.read(List.of(METADATA), refCollectionId);
		return (DataGroup) refCollection.getFirstChildWithNameInData("collectionItemReferences");
	}

	private boolean ensureChildItemExistsInParent(String childItemId, DataGroup parentReferences) {
		for (DataChild itemReference : parentReferences.getChildren()) {
			String parentItemId = ((DataRecordLink) itemReference).getLinkedRecordId();
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
				throw new DataException(
						"Data is not valid: final value does not exist in collection");
			}
		}
	}

	private boolean hasFinalValue() {
		return recordAsDataGroup.containsChildWithNameInData("finalValue");
	}

	private boolean validateFinalValue(String finalValue) {
		DataGroup references = getItemReferencesFromLinkedItemCollection();
		for (DataChild reference : references.getChildren()) {
			String itemNameInData = extractNameInDataFromReference((DataRecordLink) reference);
			if (finalValue.equals(itemNameInData)) {
				return true;
			}
		}
		return false;
	}

	private String extractNameInDataFromReference(DataRecordLink reference) {
		String itemId = reference.getLinkedRecordId();
		DataGroup collectionItem = recordStorage.read(List.of(METADATA), itemId);

		return collectionItem.getFirstAtomicValueWithNameInData("nameInData");
	}

	public String getRecordType() {
		return recordType;
	}

	public RecordStorage getRecordStorage() {
		return recordStorage;
	}

	public SpiderDependencyProvider onlyForTestGetDependencyProvider() {
		return dependencyProvider;
	}

}
