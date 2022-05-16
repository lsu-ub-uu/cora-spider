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

import static org.testng.Assert.assertSame;

import java.util.List;

import org.testng.annotations.Test;

import se.uu.ub.cora.spider.record.DataGroupMCRSpy;
import se.uu.ub.cora.spider.record.RecordStorageMCRSpy;
import se.uu.ub.cora.storage.RecordStorage;

public class RecordTypeHandlerFactoryTest {

	@Test
	public void testInit() {
		RecordStorage recordStorage = new RecordStorageMCRSpy();
		RecordTypeHandlerFactoryImp factory = new RecordTypeHandlerFactoryImp(recordStorage);
		assertSame(factory.getStorage(), recordStorage);
	}

	@Test
	public void testFactor() {
		RecordStorage recordStorage = new RecordStorageMCRSpy();
		RecordTypeHandlerFactoryImp factory = new RecordTypeHandlerFactoryImp(recordStorage);
		DataGroupMCRSpy dataGroup = createTopDataGroup();
		RecordTypeHandlerImp recordTypeHandler = (RecordTypeHandlerImp) factory
				.factorUsingDataGroup(dataGroup);
		assertSame(recordTypeHandler.getRecordStorage(), recordStorage);
		assertSame(recordTypeHandler.getRecordTypeHandlerFactory(), factory);

	}

	private DataGroupMCRSpy createTopDataGroup() {
		DataGroupMCRSpy dataGroup = new DataGroupMCRSpy();
		DataGroupMCRSpy recordInfoGroup = new DataGroupMCRSpy();
		recordInfoGroup.MRV.setReturnValues("getFirstAtomicValueWithNameInData",
				List.of("recordIdFromSpy"), "id");
		dataGroup.groupValues.put("recordInfo", recordInfoGroup);
		return dataGroup;
	}

}
