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

import se.uu.ub.cora.bookkeeper.data.DataElement;
import se.uu.ub.cora.bookkeeper.data.DataGroup;
import se.uu.ub.cora.spider.data.Action;
import se.uu.ub.cora.spider.data.SpiderDataElement;
import se.uu.ub.cora.spider.data.SpiderDataGroup;
import se.uu.ub.cora.spider.data.SpiderDataRecord;
import se.uu.ub.cora.spider.data.SpiderDataRecordLink;
import se.uu.ub.cora.spider.record.storage.RecordStorage;

public class SpiderRecordHandler {
	protected static final String RECORD_TYPE = "recordType";
	protected static final String RECORD_INFO = "recordInfo";
	protected RecordStorage recordStorage;
	protected String recordType;
	protected String recordId;

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
			((SpiderDataRecordLink) spiderDataChild).addAction(Action.READ);
		}
		if (isGroup(spiderDataChild)) {
			addReadActionToDataRecordLinks((SpiderDataGroup) spiderDataChild);
		}
	}

	private boolean isLink(SpiderDataElement spiderDataChild) {
		return spiderDataChild instanceof SpiderDataRecordLink;
	}

	private boolean isGroup(SpiderDataElement spiderDataChild) {
		return spiderDataChild instanceof SpiderDataGroup;
	}

	protected void checkToPartOfLinkedDataExistsInStorage(DataGroup collectedLinks) {
		for(DataElement dataElement : collectedLinks.getChildren()){
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
		if(!recordStorage.recordExistsForRecordTypeAndRecordId(recordType, recordId)){
			throw new DataException("Data is not valid: linkedRecord does not exists in storage for recordType: "
					+recordType + " and recordId: "+recordId);
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
		possiblyAddIncomingLinksAction(spiderDataRecord);
		addActionsForRecordType(spiderDataRecord);
	}

	private void possiblyAddDeleteAction(SpiderDataRecord spiderDataRecord) {
		if(!incomingLinksExistsForRecord(spiderDataRecord)){
			spiderDataRecord.addAction(Action.DELETE);
		}
	}

	private void possiblyAddIncomingLinksAction(SpiderDataRecord spiderDataRecord) {
		if (incomingLinksExistsForRecord(spiderDataRecord)) {
			spiderDataRecord.addAction(Action.READ_INCOMING_LINKS);
		}
	}

	private void addActionsForRecordType(SpiderDataRecord spiderDataRecord) {
		if (isRecordType()) {
			possiblyAddCreateAction(spiderDataRecord);
			possiblyAddCreateByUploadAction(spiderDataRecord);

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
		SpiderDataGroup spiderDataGroup = spiderDataRecord.getSpiderDataGroup();
		SpiderDataGroup recordInfo = (SpiderDataGroup) spiderDataGroup
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

	private void possiblyAddCreateByUploadAction(SpiderDataRecord spiderDataRecord) {
		String dataRecordRecordId = getRecordIdFromDataRecord(spiderDataRecord);
		if (isHandledRecordIdBinary(dataRecordRecordId)
				|| isHandledRecordIdBinaryChild(dataRecordRecordId)) {
			spiderDataRecord.addAction(Action.CREATE_BY_UPLOAD);
		}
	}

	private boolean isHandledRecordIdBinary(String dataRecordRecordId) {
		return "binary".equals(dataRecordRecordId);
	}

	private boolean isHandledRecordIdBinaryChild(String dataRecordRecordId) {
		String refParentId = extractParentId(dataRecordRecordId);
		return "binary".equals(refParentId);
	}

	private String extractParentId(String dataRecordRecordId) {
		DataGroup handledRecordTypeDataGroup = recordStorage.read(RECORD_TYPE, dataRecordRecordId);
		if (handledRecordHasParent(handledRecordTypeDataGroup)) {
			return handledRecordTypeDataGroup.getFirstAtomicValueWithNameInData("parentId");
		}
		return "";
	}

	private boolean handledRecordHasParent(DataGroup handledRecordTypeDataGroup) {
		return handledRecordTypeDataGroup.containsChildWithNameInData("parentId");
	}

	protected boolean incomingLinksExistsForRecord(SpiderDataRecord spiderDataRecord) {
		SpiderDataGroup spiderDataGroup = spiderDataRecord.getSpiderDataGroup();
		SpiderDataGroup recordInfo = spiderDataGroup.extractGroup(RECORD_INFO);
		String recordTypeForThisRecord = recordInfo.extractAtomicValue("type");
		String recordIdForThisRecord = recordInfo.extractAtomicValue("id");
		return recordStorage.linksExistForRecord(recordTypeForThisRecord, recordIdForThisRecord);
	}

	protected String extractDataDividerFromData(SpiderDataGroup spiderDataGroup) {
		SpiderDataGroup recordInfo = spiderDataGroup.extractGroup(RECORD_INFO);
		SpiderDataGroup dataDivider = recordInfo.extractGroup("dataDivider");
		return dataDivider.extractAtomicValue("linkedRecordId");
	}

	protected boolean isChildOfAbstractRecordType(String abstractRecordType,
												  DataGroup recordTypePossibleChild) {
		String parentId = "parentId";
		if (recordTypePossibleChild.containsChildWithNameInData(parentId)) {
			String parentIdValue = recordTypePossibleChild
					.getFirstAtomicValueWithNameInData(parentId);
			if (parentIdValue.equals(abstractRecordType)) {
				return true;
			}
		}
		return false;
	}
}
