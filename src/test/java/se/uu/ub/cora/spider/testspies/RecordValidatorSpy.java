/*
 * Copyright 2022 Olov McKie
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
package se.uu.ub.cora.spider.testspies;

import se.uu.ub.cora.data.DataGroup;
import se.uu.ub.cora.data.DataRecord;
import se.uu.ub.cora.spider.record.RecordValidator;
import se.uu.ub.cora.testspies.data.DataRecordSpy;
import se.uu.ub.cora.testutils.mcr.MethodCallRecorder;
import se.uu.ub.cora.testutils.mrv.MethodReturnValues;

public class RecordValidatorSpy implements RecordValidator {
	public MethodCallRecorder MCR = new MethodCallRecorder();
	public MethodReturnValues MRV = new MethodReturnValues();

	public RecordValidatorSpy() {
		MCR.useMRV(MRV);
		MRV.setDefaultReturnValuesSupplier("validateRecord", DataRecordSpy::new);
	}

	@Override
	public DataRecord validateRecord(String authToken, String recordType,
			DataGroup validationRecord, DataGroup recordToValidate) {
		return (DataRecord) MCR.addCallAndReturnFromMRV("authToken", authToken, "recordType",
				recordType, "validationRecord", validationRecord, "recordToValidate",
				recordToValidate);
	}
}
