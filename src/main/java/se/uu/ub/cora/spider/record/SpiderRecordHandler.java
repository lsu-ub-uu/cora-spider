/*
 * Copyright 2015, 2016, 2019 Uppsala University Library
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

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

import se.uu.ub.cora.bookkeeper.data.DataElement;
import se.uu.ub.cora.bookkeeper.data.DataGroup;
import se.uu.ub.cora.spider.data.SpiderDataAtomic;
import se.uu.ub.cora.spider.data.SpiderDataGroup;
import se.uu.ub.cora.spider.record.storage.RecordStorage;

public class SpiderRecordHandler {
	protected static final String LINKED_RECORD_ID = "linkedRecordId";
	protected static final String RECORD_TYPE = "recordType";
	protected static final String RECORD_INFO = "recordInfo";
	static final String TS_CREATED = "tsCreated";
	protected RecordStorage recordStorage;
	protected String recordType;
	protected String recordId;
	protected SpiderDataGroup recordAsSpiderDataGroup;

	protected DataGroup getRecordTypeDefinition() {
		return recordStorage.read(RECORD_TYPE, recordType);
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
		if (!recordStorage.recordExistsForAbstractOrImplementingRecordTypeAndRecordId(recordType,
				recordId)) {
			throw new DataException(
					"Data is not valid: linkedRecord does not exists in storage for recordType: "
							+ recordType + " and recordId: " + recordId);
		}
	}

	protected String extractDataDividerFromData(SpiderDataGroup spiderDataGroup) {
		SpiderDataGroup recordInfo = spiderDataGroup.extractGroup(RECORD_INFO);
		SpiderDataGroup dataDivider = recordInfo.extractGroup("dataDivider");
		return dataDivider.extractAtomicValue(LINKED_RECORD_ID);
	}

	protected String getLocalTimeDateAsString(LocalDateTime localDateTime) {
		DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS");
		return localDateTime.format(formatter);
	}

	protected void addUpdatedInfoToRecordInfoUsingUserId(SpiderDataGroup recordInfo,
			String userId) {
		SpiderDataGroup updatedGroup = createUpdatedGroup();
		addUserInfoToUpdatedGroup(userId, updatedGroup);
		addTimestampToUpdateGroup(recordInfo, updatedGroup);
		recordInfo.addChild(updatedGroup);
	}

	SpiderDataGroup createUpdatedGroup() {
		SpiderDataGroup updatedGroup = SpiderDataGroup.withNameInData("updated");
		updatedGroup.setRepeatId("0");
		return updatedGroup;
	}

	void addUserInfoToUpdatedGroup(String userId, SpiderDataGroup updatedGroup) {
		SpiderDataGroup updatedByGroup = createLinkToUserUsingUserIdAndNameInData(userId,
				"updatedBy");
		updatedGroup.addChild(updatedByGroup);
	}

	public void addTimestampToUpdateGroup(SpiderDataGroup recordInfo,
			SpiderDataGroup updatedGroup) {
		String tsCreatedUsedAsFirstTsUpdate = recordInfo.extractAtomicValue(TS_CREATED);
		updatedGroup.addChild(
				SpiderDataAtomic.withNameInDataAndValue("tsUpdated", tsCreatedUsedAsFirstTsUpdate));
	}

	protected SpiderDataGroup createLinkToUserUsingUserIdAndNameInData(String userId,
			String nameInData) {
		SpiderDataGroup createdByGroup = SpiderDataGroup.withNameInData(nameInData);
		addLinkToUserUsingUserId(createdByGroup, userId);
		return createdByGroup;
	}

	void addLinkToUserUsingUserId(SpiderDataGroup dataGroup, String userId) {
		dataGroup.addChild(SpiderDataAtomic.withNameInDataAndValue("linkedRecordType", "user"));
		dataGroup.addChild(SpiderDataAtomic.withNameInDataAndValue(LINKED_RECORD_ID, userId));
	}
}
