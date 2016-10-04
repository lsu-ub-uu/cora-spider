/*
 * Copyright 2015, 2016 Uppsala University Library
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

import java.util.Collection;

import se.uu.ub.cora.bookkeeper.data.DataAtomic;
import se.uu.ub.cora.bookkeeper.data.DataElement;
import se.uu.ub.cora.bookkeeper.data.DataGroup;
import se.uu.ub.cora.spider.data.Action;
import se.uu.ub.cora.spider.data.SpiderDataAtomic;
import se.uu.ub.cora.spider.data.SpiderDataElement;
import se.uu.ub.cora.spider.data.SpiderDataGroup;
import se.uu.ub.cora.spider.data.SpiderDataLink;
import se.uu.ub.cora.spider.data.SpiderDataRecord;
import se.uu.ub.cora.spider.data.SpiderDataRecordLink;
import se.uu.ub.cora.spider.data.SpiderDataResourceLink;
import se.uu.ub.cora.spider.record.storage.RecordNotFoundException;
import se.uu.ub.cora.spider.record.storage.RecordStorage;

public class SpiderRecordHandler {
	protected static final String RECORD_TYPE = "recordType";
	protected static final String RECORD_INFO = "recordInfo";
	private static final String PARENT_ID = "parentId";
	private static final String REF_PARENT_ID = "refParentId";
	protected RecordStorage recordStorage;
	protected String recordType;
	protected String recordId;
	protected SpiderDataGroup spiderDataGroup;

	protected DataGroup getRecordTypeDefinition() {
		return recordStorage.read(RECORD_TYPE, recordType);
	}

	protected boolean isRecordTypeAbstract() {
		String abstractInRecordTypeDefinition = getAbstractFromRecordTypeDefinition();
		return "true".equals(abstractInRecordTypeDefinition);
	}

	private String getAbstractFromRecordTypeDefinition() {
		DataGroup recordTypeDefinition = getRecordTypeDefinition();
		return recordTypeDefinition.getFirstAtomicValueWithNameInData("abstract");
	}

	protected void addReadActionToDataRecordLinks(SpiderDataGroup spiderDataGroup) {
		for (SpiderDataElement spiderDataChild : spiderDataGroup.getChildren()) {
			addReadActionToDataRecordLink(spiderDataChild);
		}
	}

	private void addReadActionToDataRecordLink(SpiderDataElement spiderDataChild) {
		if (isLink(spiderDataChild)) {
			((SpiderDataLink) spiderDataChild).addAction(Action.READ);
		}
		if (isGroup(spiderDataChild)) {
			addReadActionToDataRecordLinks((SpiderDataGroup) spiderDataChild);
		}
	}

	private boolean isLink(SpiderDataElement spiderDataChild) {
		return spiderDataChild instanceof SpiderDataRecordLink
				|| spiderDataChild instanceof SpiderDataResourceLink;
	}

	private boolean isGroup(SpiderDataElement spiderDataChild) {
		return spiderDataChild instanceof SpiderDataGroup;
	}

	protected void validateRules() {
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
		return spiderDataGroup.containsChildWithNameInData(REF_PARENT_ID);
	}

	private void ensureAllChildrenExistsInParent() {
		SpiderDataGroup childReferences = (SpiderDataGroup) spiderDataGroup
				.getFirstChildWithNameInData("childReferences");

		for (SpiderDataElement childReference : childReferences.getChildren()) {
			String childNameInData = getNameInDataFromChildReference(childReference);
			if (!ensureChildExistInParent(childNameInData)) {
				throw new DataException("Data is not valid: child does not exist in parent");
			}
		}
	}

	protected String getNameInDataFromChildReference(SpiderDataElement childReference) {
		SpiderDataGroup childReferenceGroup = (SpiderDataGroup) childReference;
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
		SpiderDataGroup parentChildReferences = getParentChildReferences();
		for (SpiderDataElement parentChildReference : parentChildReferences.getChildren()) {
			if (isSameNameInData(childNameInData, parentChildReference)) {
				return true;
			}
		}
		return false;
	}

	protected SpiderDataGroup getParentChildReferences() {
		SpiderDataAtomic refParentId = (SpiderDataAtomic) spiderDataGroup
				.getFirstChildWithNameInData(REF_PARENT_ID);
		SpiderDataGroup parent = SpiderDataGroup
				.fromDataGroup(recordStorage.read("metadataGroup", refParentId.getValue()));

		return (SpiderDataGroup) parent.getFirstChildWithNameInData("childReferences");
	}

	protected boolean isSameNameInData(String childNameInData,
			SpiderDataElement parentChildReference) {
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
			DataAtomic childItem = (DataAtomic) itemReference;
			if (!ensureChildItemExistsInParent(childItem, parentReferences)) {
				throw new DataException("Data is not valid: childItem: " + childItem.getValue()
						+ " does not exist in parent");
			}
		}
	}

	private DataGroup getItemReferences() {
		String refCollectionId = spiderDataGroup.extractAtomicValue("refCollectionId");
		return readItemCollectionAndExtractCollectionItemReferences(refCollectionId);
	}

	private DataGroup readItemCollectionAndExtractCollectionItemReferences(String refCollectionId) {
		DataGroup refCollection = recordStorage.read("metadataItemCollection", refCollectionId);
		return (DataGroup) refCollection.getFirstChildWithNameInData("collectionItemReferences");
	}

	private DataGroup extractParentItemReferences() {
		String refParentId = spiderDataGroup.extractAtomicValue(REF_PARENT_ID);
		DataGroup parentCollectionVar = recordStorage.read("metadataCollectionVariable",
				refParentId);
		String parentRefCollectionId = parentCollectionVar
				.getFirstAtomicValueWithNameInData("refCollectionId");

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

	private void possiblyValidateFinalValue() {
		if (hasFinalValue()) {
			String finalValue = spiderDataGroup.extractAtomicValue("finalValue");
			if (!validateFinalValue(finalValue)) {
				throw new DataException(
						"Data is not valid: final value does not exist in collection");
			}
		}
	}

	private boolean hasFinalValue() {
		return spiderDataGroup.containsChildWithNameInData("finalValue");
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

	protected void checkToPartOfLinkedDataExistsInStorage(DataGroup collectedLinks) {
		for (DataElement dataElement : collectedLinks.getChildren()) {
			extractToGroupAndCheckDataExistsInStorage((DataGroup) dataElement);
		}
	}

	private void extractToGroupAndCheckDataExistsInStorage(DataGroup dataElement) {
		DataGroup to = extractToGroupFromRecordLink(dataElement);
		String toRecordId = extractAtomicValueFromGroup("linkedRecordId", to);
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

	protected SpiderDataRecord createDataRecordContainingDataGroup(
			SpiderDataGroup spiderDataGroup) {
		addReadActionToDataRecordLinks(spiderDataGroup);
		SpiderDataRecord spiderDataRecord = SpiderDataRecord.withSpiderDataGroup(spiderDataGroup);
		addActions(spiderDataRecord);
		return spiderDataRecord;
	}

	protected void addActions(SpiderDataRecord spiderDataRecord) {
		spiderDataRecord.addAction(Action.READ);
		spiderDataRecord.addAction(Action.UPDATE);

		possiblyAddDeleteAction(spiderDataRecord);
		possiblyAddUploadAction(spiderDataRecord);
		possiblyAddIncomingLinksAction(spiderDataRecord);
		addActionsForRecordType(spiderDataRecord);
	}

	private void possiblyAddDeleteAction(SpiderDataRecord spiderDataRecord) {
		if (!incomingLinksExistsForRecord(spiderDataRecord)) {
			spiderDataRecord.addAction(Action.DELETE);
		}
	}

	protected boolean incomingLinksExistsForRecord(SpiderDataRecord spiderDataRecord) {
		SpiderDataGroup topLevelDataGroup = spiderDataRecord.getSpiderDataGroup();
		SpiderDataGroup recordInfo = topLevelDataGroup.extractGroup(RECORD_INFO);
		String recordTypeForThisRecord = recordInfo.extractAtomicValue("type");
		String recordIdForThisRecord = recordInfo.extractAtomicValue("id");
		return recordStorage.linksExistForRecord(recordTypeForThisRecord, recordIdForThisRecord);
	}

	private void possiblyAddUploadAction(SpiderDataRecord spiderDataRecord) {
		if (isHandledRecordIdChildOfBinary(recordType)) {
			spiderDataRecord.addAction(Action.UPLOAD);
		}
	}

	private boolean isHandledRecordIdChildOfBinary(String dataRecordRecordId) {
		String refParentId = extractParentId(dataRecordRecordId);
		return "binary".equals(refParentId);
	}

	private String extractParentId(String dataRecordRecordId) {
		DataGroup handledRecordTypeDataGroup = recordStorage.read(RECORD_TYPE, dataRecordRecordId);
		if (handledRecordHasParent(handledRecordTypeDataGroup)) {
			return handledRecordTypeDataGroup.getFirstAtomicValueWithNameInData(PARENT_ID);
		}
		return "";
	}

	private void possiblyAddIncomingLinksAction(SpiderDataRecord spiderDataRecord) {
		if (incomingLinksExistsForRecord(spiderDataRecord)) {
			spiderDataRecord.addAction(Action.READ_INCOMING_LINKS);
		}
	}

	private void addActionsForRecordType(SpiderDataRecord spiderDataRecord) {
		if (isRecordType()) {
			possiblyAddCreateAction(spiderDataRecord);

			spiderDataRecord.addAction(Action.LIST);
			spiderDataRecord.addAction(Action.SEARCH);
		}
	}

	protected boolean isRecordType() {
		return recordType.equals(RECORD_TYPE);
	}

	private void possiblyAddCreateAction(SpiderDataRecord spiderDataRecord) {
		String handledRecordId = getRecordIdFromDataRecord(spiderDataRecord);
		if (!isHandledRecordIdOfTypeAbstract(handledRecordId)) {
			spiderDataRecord.addAction(Action.CREATE);
		}
	}

	private String getRecordIdFromDataRecord(SpiderDataRecord spiderDataRecord) {
		SpiderDataGroup topLevelDataGroup = spiderDataRecord.getSpiderDataGroup();
		SpiderDataGroup recordInfo = (SpiderDataGroup) topLevelDataGroup
				.getFirstChildWithNameInData(RECORD_INFO);
		return recordInfo.extractAtomicValue("id");
	}

	private boolean isHandledRecordIdOfTypeAbstract(String recordId) {
		String abstractInRecordTypeDefinition = getAbstractFromHandledRecord(recordId);
		return "true".equals(abstractInRecordTypeDefinition);
	}

	private String getAbstractFromHandledRecord(String recordId) {
		DataGroup handleRecordTypeDataGroup = recordStorage.read(RECORD_TYPE, recordId);
		return handleRecordTypeDataGroup.getFirstAtomicValueWithNameInData("abstract");
	}

	protected String extractDataDividerFromData(SpiderDataGroup spiderDataGroup) {
		SpiderDataGroup recordInfo = spiderDataGroup.extractGroup(RECORD_INFO);
		SpiderDataGroup dataDivider = recordInfo.extractGroup("dataDivider");
		return dataDivider.extractAtomicValue("linkedRecordId");
	}

}
