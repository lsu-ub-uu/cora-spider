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

import org.testng.annotations.Test;

import se.uu.ub.cora.data.spies.DataGroupSpy;
import se.uu.ub.cora.spider.spy.RecordStorageMCRSpy;
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
		DataGroupSpy dataGroup = createTopDataGroup();
		RecordTypeHandlerImp recordTypeHandler = (RecordTypeHandlerImp) factory
				.factorUsingDataGroup(dataGroup);
		assertSame(recordTypeHandler.getRecordStorage(), recordStorage);
		assertSame(recordTypeHandler.getRecordTypeHandlerFactory(), factory);

	}

	private DataGroupSpy createTopDataGroup() {
		return new DataGroupSpy();
	}

}
