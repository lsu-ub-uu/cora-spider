/*
 * Copyright 2021 Uppsala University Library
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
package se.uu.ub.cora.spider.dependency.spy;

import se.uu.ub.cora.data.DataGroup;
import se.uu.ub.cora.data.DataRecord;
import se.uu.ub.cora.spider.data.DataRecordSpy;
import se.uu.ub.cora.spider.record.RecordCreator;
import se.uu.ub.cora.testutils.mcr.MethodCallRecorder;

public class RecordCreatorSpy implements RecordCreator {

	public DataRecord recordToReturn = null;

	public MethodCallRecorder MCR = new MethodCallRecorder();

	@Override
	public DataRecord createAndStoreRecord(String authToken, String type, DataGroup record) {
		MCR.addCall("authToken", authToken, "type", type, "record", record);
		DataRecordSpy dataRecordSpy = null;
		if (recordToReturn != null) {

			dataRecordSpy = (DataRecordSpy) recordToReturn;
		} else {
			dataRecordSpy = new DataRecordSpy(record);
		}
		MCR.addReturned(dataRecordSpy);
		return (DataRecord) MCR.addCallAndReturnFromMRV();
	}

}
