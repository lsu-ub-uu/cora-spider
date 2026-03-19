/*
 * Copyright 2015, 2016, 2019, 2024 Uppsala University Library
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

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import se.uu.ub.cora.beefeater.authentication.User;
import se.uu.ub.cora.data.DataRecordGroup;
import se.uu.ub.cora.data.collected.Link;
import se.uu.ub.cora.spider.dependency.SpiderDependencyProvider;
import se.uu.ub.cora.spider.extendedfunctionality.ExtendedFunctionality;
import se.uu.ub.cora.spider.extendedfunctionality.ExtendedFunctionalityData;
import se.uu.ub.cora.spider.record.DataException;
import se.uu.ub.cora.storage.RecordStorage;

public class RecordHandler {
	protected static final String LINKED_RECORD_ID = "linkedRecordId";
	protected static final String RECORD_TYPE = "recordType";
	protected static final String RECORD_INFO = "recordInfo";
	protected static final String TS_CREATED = "tsCreated";
	protected static final String VISIBILITY = "visibility";
	protected static final String TS_VISIBILITY = "tsVisibility";
	protected SpiderDependencyProvider dependencyProvider;
	protected RecordStorage recordStorage;
	protected String recordType;
	protected String recordId;
	protected String authToken;
	protected User user;
	protected Map<String, Object> dataSharer = new HashMap<>();

	protected void checkToPartOfLinkedDataExistsInStorage(Set<Link> collectedLinks) {
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
		if (!recordStorage.recordExists(List.of(recordType), recordId)) {
			throw new DataException(
					"Data is not valid: linkedRecord does not exists in storage for recordType: "
							+ recordType + " and recordId: " + recordId);
		}
	}

	protected void useExtendedFunctionality(DataRecordGroup dataRecordGroup,
			List<ExtendedFunctionality> functionalityList) {
		for (ExtendedFunctionality extendedFunctionality : functionalityList) {
			ExtendedFunctionalityData data = createExtendedFunctionalityData(dataRecordGroup);
			extendedFunctionality.useExtendedFunctionality(data);
		}
	}

	protected ExtendedFunctionalityData createExtendedFunctionalityData(
			DataRecordGroup dataRecordGroup) {
		ExtendedFunctionalityData exData = new ExtendedFunctionalityData();
		exData.recordType = recordType;
		exData.recordId = recordId;
		exData.authToken = authToken;
		exData.user = user;
		exData.dataRecordGroup = dataRecordGroup;
		exData.dataSharer = dataSharer;
		return exData;
	}
}
