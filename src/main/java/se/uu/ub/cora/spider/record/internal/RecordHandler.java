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

package se.uu.ub.cora.spider.record.internal;

import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;

import se.uu.ub.cora.data.DataAtomicProvider;
import se.uu.ub.cora.data.DataElement;
import se.uu.ub.cora.data.DataGroup;
import se.uu.ub.cora.data.DataGroupProvider;
import se.uu.ub.cora.data.DataRecordLink;
import se.uu.ub.cora.data.DataRecordLinkProvider;
import se.uu.ub.cora.spider.record.DataException;
import se.uu.ub.cora.storage.RecordStorage;

public class RecordHandler {
	protected static final String LINKED_RECORD_ID = "linkedRecordId";
	protected static final String RECORD_TYPE = "recordType";
	protected static final String RECORD_INFO = "recordInfo";
	protected static final String TS_CREATED = "tsCreated";
	protected RecordStorage recordStorage;
	protected String recordType;
	protected String recordId;
	protected DataGroup recordAsDataGroup;

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

	protected String extractDataDividerFromData(DataGroup dataGroup) {
		DataGroup recordInfo = dataGroup.getFirstGroupWithNameInData(RECORD_INFO);
		DataGroup dataDivider = recordInfo.getFirstGroupWithNameInData("dataDivider");
		return dataDivider.getFirstAtomicValueWithNameInData(LINKED_RECORD_ID);
	}

	protected String getCurrentTimestampAsString() {
		return formatInstantKeepingTrailingZeros(Instant.now());
	}

	protected String formatInstantKeepingTrailingZeros(Instant instant) {
		DateTimeFormatter formatter = new DateTimeFormatterBuilder().appendInstant(6).toFormatter();
		return formatter.format(instant);
	}

	protected void addUpdatedInfoToRecordInfoUsingUserId(DataGroup recordInfo, String userId) {
		DataGroup updatedGroup = createUpdatedGroup();
		addUserInfoToUpdatedGroup(userId, updatedGroup);
		addTimestampToUpdateGroup(recordInfo, updatedGroup);
		recordInfo.addChild(updatedGroup);
	}

	private DataGroup createUpdatedGroup() {
		DataGroup updatedGroup = DataGroupProvider.getDataGroupUsingNameInData("updated");
		updatedGroup.setRepeatId("0");
		return updatedGroup;
	}

	private void addUserInfoToUpdatedGroup(String userId, DataGroup updatedGroup) {
		DataGroup updatedByGroup = createLinkToUserUsingUserIdAndNameInData(userId, "updatedBy");
		updatedGroup.addChild(updatedByGroup);
	}

	private void addTimestampToUpdateGroup(DataGroup recordInfo, DataGroup updatedGroup) {
		String tsCreatedUsedAsFirstTsUpdate = recordInfo
				.getFirstAtomicValueWithNameInData(TS_CREATED);
		updatedGroup.addChild(DataAtomicProvider.getDataAtomicUsingNameInDataAndValue("tsUpdated",
				tsCreatedUsedAsFirstTsUpdate));
	}

	protected DataGroup createLinkToUserUsingUserIdAndNameInData(String userId, String nameInData) {
		DataRecordLink createdByGroup = DataRecordLinkProvider
				.getDataRecordLinkUsingNameInData(nameInData);
		addLinkToUserUsingUserId(createdByGroup, userId);
		return createdByGroup;
	}

	private void addLinkToUserUsingUserId(DataGroup dataGroup, String userId) {
		dataGroup.addChild(DataAtomicProvider
				.getDataAtomicUsingNameInDataAndValue("linkedRecordType", "user"));
		dataGroup.addChild(
				DataAtomicProvider.getDataAtomicUsingNameInDataAndValue(LINKED_RECORD_ID, userId));
	}
}
