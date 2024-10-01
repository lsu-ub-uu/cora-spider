/*
 * Copyright 2018, 2022 Uppsala University Library
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
package se.uu.ub.cora.spider.extended.workorder;

import se.uu.ub.cora.data.DataRecordGroup;
import se.uu.ub.cora.spider.authorization.AuthorizationException;
import se.uu.ub.cora.spider.extendedfunctionality.ExtendedFunctionality;
import se.uu.ub.cora.spider.extendedfunctionality.ExtendedFunctionalityData;
import se.uu.ub.cora.spider.record.RecordDeleter;
import se.uu.ub.cora.storage.RecordNotFoundException;

public final class WorkOrderDeleter implements ExtendedFunctionality {

	private RecordDeleter recordDeleter;

	private WorkOrderDeleter(RecordDeleter recordDeleter) {
		this.recordDeleter = recordDeleter;
	}

	public static WorkOrderDeleter usingDeleter(RecordDeleter recordDeleter) {
		return new WorkOrderDeleter(recordDeleter);
	}

	@Override
	public void useExtendedFunctionality(ExtendedFunctionalityData data) {
		String authToken = data.authToken;
		DataRecordGroup dataRecordGroup = data.dataRecordGroup;
		String recordType = dataRecordGroup.getType();
		String recordId = dataRecordGroup.getId();
		tryToDeleteRecord(authToken, recordType, recordId);
	}

	private void tryToDeleteRecord(String authToken, String recordType, String recordId) {
		try {
			recordDeleter.deleteRecord(authToken, recordType, recordId);
		} catch (AuthorizationException | RecordNotFoundException e) {
			// do nothing
		}
	}

	public RecordDeleter getRecordDeleter() {
		// needed for test
		return recordDeleter;
	}
}
