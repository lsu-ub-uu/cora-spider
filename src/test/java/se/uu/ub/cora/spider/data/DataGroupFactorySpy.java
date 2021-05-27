/*
 * Copyright 2020 Uppsala University Library
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
package se.uu.ub.cora.spider.data;

import se.uu.ub.cora.data.DataGroup;
import se.uu.ub.cora.data.DataGroupFactory;
import se.uu.ub.cora.spider.spy.MethodCallRecorder;

public class DataGroupFactorySpy implements DataGroupFactory {

	public DataGroupSpy returnedDataGroup;

	public MethodCallRecorder MCR = new MethodCallRecorder();

	@Override
	public DataGroup factorUsingNameInData(String nameInData) {
		MCR.addCall("nameInData", nameInData);
		returnedDataGroup = new DataGroupSpy(nameInData);
		MCR.addReturned(returnedDataGroup);
		return returnedDataGroup;
	}

	@Override
	public DataGroup factorAsLinkWithNameInDataTypeAndId(String nameInData, String recordType,
			String recordId) {
		MCR.addCall("nameInData", nameInData, "recordType", recordType);
		DataGroupSpy dataGroupSpy = new DataGroupSpy(nameInData, recordType, recordId);
		MCR.addReturned(dataGroupSpy);
		return dataGroupSpy;
	}

}
