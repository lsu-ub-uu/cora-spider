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
import java.util.List;

import se.uu.ub.cora.beefeater.authentication.User;
import se.uu.ub.cora.data.DataGroup;
import se.uu.ub.cora.data.DataProvider;
import se.uu.ub.cora.data.DataRecordLink;
import se.uu.ub.cora.data.collected.Link;
import se.uu.ub.cora.spider.dependency.SpiderDependencyProvider;
import se.uu.ub.cora.spider.extendedfunctionality.ExtendedFunctionality;
import se.uu.ub.cora.spider.extendedfunctionality.ExtendedFunctionalityData;
import se.uu.ub.cora.spider.record.DataException;
import se.uu.ub.cora.spider.recordtype.RecordTypeHandler;
import se.uu.ub.cora.storage.RecordStorage;

public class RecordHandler {
	protected static final String LINKED_RECORD_ID = "linkedRecordId";
	protected static final String RECORD_TYPE = "recordType";
	protected static final String RECORD_INFO = "recordInfo";
	protected static final String TS_CREATED = "tsCreated";
	protected SpiderDependencyProvider dependencyProvider;
	protected RecordStorage recordStorage;
	protected String recordType;
	protected String recordId;
	protected DataGroup recordAsDataGroup;
	protected String authToken;
	protected User user;

	protected void checkToPartOfLinkedDataExistsInStorage(List<Link> collectedLinks) {
		for (Link recordToRecordLink : collectedLinks) {
			extractToGroupAndCheckDataExistsInStorage(recordToRecordLink);
		}
	}

	private void extractToGroupAndCheckDataExistsInStorage(Link recordToRecordLink) {
		String toRecordType = recordToRecordLink.type();
		String toRecordId = recordToRecordLink.id();
		checkRecordTypeAndRecordIdExistsInStorage(toRecordType, toRecordId);
	}

	private void checkRecordTypeAndRecordIdExistsInStorage(String recordType, String recordId) {
		RecordTypeHandler recordTypeHandler = dependencyProvider.getRecordTypeHandler(recordType);
		List<String> listOfTypes = recordTypeHandler.getListOfRecordTypeIdsToReadFromStorage();
		if (!recordStorage.recordExists(listOfTypes,
				recordId)) {
			throw new DataException(
					"Data is not valid: linkedRecord does not exists in storage for recordType: "
							+ recordType + " and recordId: " + recordId);
		}
	}

	protected String extractDataDividerFromData(DataGroup dataGroup) {
		DataGroup recordInfo = dataGroup.getFirstGroupWithNameInData(RECORD_INFO);
		DataRecordLink dataDivider = (DataRecordLink) recordInfo
				.getFirstChildWithNameInData("dataDivider");
		return dataDivider.getLinkedRecordId();
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
		DataGroup updatedGroup = DataProvider.createGroupUsingNameInData("updated");
		updatedGroup.setRepeatId("0");
		return updatedGroup;
	}

	private void addUserInfoToUpdatedGroup(String userId, DataGroup updatedGroup) {
		DataRecordLink updatedByLink = createLinkToUserUsingNameInDataAndUserId("updatedBy",
				userId);
		updatedGroup.addChild(updatedByLink);
	}

	private void addTimestampToUpdateGroup(DataGroup recordInfo, DataGroup updatedGroup) {
		String tsCreatedUsedAsFirstTsUpdate = recordInfo
				.getFirstAtomicValueWithNameInData(TS_CREATED);
		updatedGroup.addChild(DataProvider.createAtomicUsingNameInDataAndValue("tsUpdated",
				tsCreatedUsedAsFirstTsUpdate));
	}

	protected DataRecordLink createLinkToUserUsingNameInDataAndUserId(String nameInData,
			String userId) {
		return DataProvider.createRecordLinkUsingNameInDataAndTypeAndId(nameInData, "user", userId);
	}

	protected void useExtendedFunctionality(DataGroup dataGroup,
			List<ExtendedFunctionality> functionalityList) {
		for (ExtendedFunctionality extendedFunctionality : functionalityList) {
			ExtendedFunctionalityData data = createExtendedFunctionalityData(dataGroup);
			extendedFunctionality.useExtendedFunctionality(data);
		}
	}

	protected ExtendedFunctionalityData createExtendedFunctionalityData(DataGroup dataGroup) {
		ExtendedFunctionalityData data = new ExtendedFunctionalityData();
		data.recordType = recordType;
		data.recordId = recordId;
		data.authToken = authToken;
		data.user = user;
		data.dataGroup = dataGroup;
		return data;
	}
}
