/*
 * Copyright 2015, 2016 Uppsala University Library
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

package se.uu.ub.cora.spider.record;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertNotNull;
import static org.testng.Assert.assertTrue;

import java.util.List;

import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import se.uu.ub.cora.beefeater.Authorizator;
import se.uu.ub.cora.beefeater.AuthorizatorImp;
import se.uu.ub.cora.spider.data.Action;
import se.uu.ub.cora.spider.data.SpiderData;
import se.uu.ub.cora.spider.data.SpiderDataList;
import se.uu.ub.cora.spider.data.SpiderDataRecord;
import se.uu.ub.cora.spider.record.storage.RecordStorage;
import se.uu.ub.cora.spider.spy.RecordPermissionKeyCalculatorStub;
import se.uu.ub.cora.spider.spy.RecordStorageSpy;
import se.uu.ub.cora.spider.testdata.TestDataRecordInMemoryStorage;

public class SpiderRecordListReaderTest {

	private RecordStorage recordStorage;
	private Authorizator authorization;
	private PermissionKeyCalculator keyCalculator;
	private SpiderRecordListReader recordListReader;

	@BeforeMethod
	public void beforeMethod() {
		authorization = new AuthorizatorImp();
		recordStorage = TestDataRecordInMemoryStorage.createRecordStorageInMemoryWithTestData();
		keyCalculator = new RecordPermissionKeyCalculatorStub();
		recordListReader = SpiderRecordListReaderImp
				.usingAuthorizationAndRecordStorageAndKeyCalculator(authorization, recordStorage,
						keyCalculator);
	}

	@Test
	public void testReadListAuthorized() {
		String userId = "userId";
		String type = "place";
		SpiderDataList readRecordList = recordListReader.readRecordList(userId, type);
		assertEquals(readRecordList.getTotalNumberOfTypeInStorage(), "2",
				"Total number of records should be 2");
		assertEquals(readRecordList.getFromNo(), "1");
		assertEquals(readRecordList.getToNo(), "2");
		List<SpiderData> records = readRecordList.getDataList();
		SpiderDataRecord spiderDataRecord = (SpiderDataRecord) records.iterator().next();
		assertNotNull(spiderDataRecord);
	}

	@Test
	public void testReadListAbstractRecordType() {
		RecordStorageSpy recordStorageListReaderSpy = new RecordStorageSpy();
		SpiderRecordListReader recordReader = SpiderRecordListReaderImp
				.usingAuthorizationAndRecordStorageAndKeyCalculator(authorization,
						recordStorageListReaderSpy, keyCalculator);
		recordReader.readRecordList("userId", "abstract");

		assertTrue(recordStorageListReaderSpy.readLists.contains("child1"));
		assertTrue(recordStorageListReaderSpy.readLists.contains("child2"));
	}

	@Test
	public void testReadListAbstractRecordTypeNoDataForOneRecordType() {
		RecordStorageSpy recordStorageListReaderSpy = new RecordStorageSpy();
		SpiderRecordListReader recordReader = SpiderRecordListReaderImp
				.usingAuthorizationAndRecordStorageAndKeyCalculator(authorization,
						recordStorageListReaderSpy, keyCalculator);
		recordReader.readRecordList("userId", "abstract2");

		assertFalse(recordStorageListReaderSpy.readLists.contains("child1_2"));
		assertTrue(recordStorageListReaderSpy.readLists.contains("child2_2"));
	}

	@Test(expectedExceptions = AuthorizationException.class)
	public void testReadListUnauthorized() {
		recordListReader.readRecordList("unauthorizedUserId", "place");
	}

	@Test
	public void testActionsOnReadRecord() {
		SpiderDataList recordList = recordListReader.readRecordList("userId", "place");
		assertEquals(((SpiderDataRecord) recordList.getDataList().get(0)).getActions().size(), 4);
		assertTrue(((SpiderDataRecord) recordList.getDataList().get(0)).getActions()
				.contains(Action.DELETE));
	}

	@Test
	public void testActionsOnReadRecordType() {
		SpiderDataList recordList = recordListReader.readRecordList("userId", "recordType");
		SpiderDataRecord firstInListWhichIsMetadata = (SpiderDataRecord) recordList.getDataList()
				.get(0);
		assertEquals(firstInListWhichIsMetadata.getActions().size(), 6);
		assertTrue(firstInListWhichIsMetadata.getActions().contains(Action.READ));
		assertTrue(firstInListWhichIsMetadata.getActions().contains(Action.UPDATE));
		assertTrue(firstInListWhichIsMetadata.getActions().contains(Action.DELETE));
		assertTrue(firstInListWhichIsMetadata.getActions().contains(Action.CREATE));
		assertTrue(firstInListWhichIsMetadata.getActions().contains(Action.LIST));
		assertTrue(firstInListWhichIsMetadata.getActions().contains(Action.SEARCH));

		SpiderDataRecord thirdInListWhichIsBinary = (SpiderDataRecord) recordList.getDataList()
				.get(3);
		assertEquals(thirdInListWhichIsBinary.getActions().size(), 7);
		assertTrue(thirdInListWhichIsBinary.getActions().contains(Action.READ));
		assertTrue(thirdInListWhichIsBinary.getActions().contains(Action.UPDATE));
		assertTrue(thirdInListWhichIsBinary.getActions().contains(Action.DELETE));
		assertTrue(thirdInListWhichIsBinary.getActions().contains(Action.CREATE));
		assertTrue(thirdInListWhichIsBinary.getActions().contains(Action.LIST));
		assertTrue(thirdInListWhichIsBinary.getActions().contains(Action.SEARCH));
		assertTrue(thirdInListWhichIsBinary.getActions().contains(Action.CREATE_BY_UPLOAD));

	}

	@Test
	public void testActionsOnReadRecordNoIncomingLinks() {
		SpiderDataList recordList = recordListReader.readRecordList("userId", "place");
		assertEquals(((SpiderDataRecord) recordList.getDataList().get(1)).getActions().size(), 3);
		assertFalse(((SpiderDataRecord) recordList.getDataList().get(1)).getActions()
				.contains(Action.READ_INCOMING_LINKS));
	}
}
