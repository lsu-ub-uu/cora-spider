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
package se.uu.ub.cora.spider.recordtype.internal;

import java.util.ArrayList;
import java.util.List;

import se.uu.ub.cora.data.DataGroup;
import se.uu.ub.cora.spider.recordtype.RecordTypeHandler;
import se.uu.ub.cora.testutils.mcr.MethodCallRecorder;

public class RecordTypeHandlerFactorySpy implements RecordTypeHandlerFactory {

	// public RecordTypeHandlerFactorySpy(RecordStorageMCRSpy recordStorage) {
	// // TODO Auto-generated constructor stub
	// }
	public MethodCallRecorder MCR = new MethodCallRecorder();
	public List<RecordTypeHandler> recordTypeHandlersToReturn = new ArrayList<>();
	private int returnedHandlers = 0;

	@Override
	public RecordTypeHandler factorUsingDataGroup(DataGroup dataGroup) {
		MCR.addCall("dataGroup", dataGroup);
		RecordTypeHandler returned = recordTypeHandlersToReturn.get(returnedHandlers);
		MCR.addReturned(returned);
		returnedHandlers++;
		return returned;
	}

}
