/*
 * Copyright 2015 Uppsala University Library
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
import static org.testng.Assert.assertTrue;

import org.testng.Assert;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import se.uu.ub.cora.beefeater.Authorizator;
import se.uu.ub.cora.beefeater.AuthorizatorImp;
import se.uu.ub.cora.spider.data.Action;
import se.uu.ub.cora.spider.data.SpiderDataGroup;
import se.uu.ub.cora.spider.data.SpiderDataRecord;
import se.uu.ub.cora.spider.data.SpiderDataRecordLink;
import se.uu.ub.cora.spider.record.storage.RecordNotFoundException;
import se.uu.ub.cora.spider.record.storage.RecordStorage;
import se.uu.ub.cora.spider.testdata.TestDataRecordInMemoryStorage;

public class SpiderRecordReaderTest {
	private RecordStorage recordStorage;
	private Authorizator authorization;
	private PermissionKeyCalculator keyCalculator;
	private SpiderRecordReader recordReader;

	@BeforeMethod
	public void beforeMethod() {
		authorization = new AuthorizatorImp();
		recordStorage = TestDataRecordInMemoryStorage.createRecordStorageInMemoryWithTestData();
		keyCalculator = new RecordPermissionKeyCalculator();
		recordReader = SpiderRecordReaderImp.usingAuthorizationAndRecordStorageAndKeyCalculator(
				authorization, recordStorage, keyCalculator);
	}

	@Test
	public void testReadAuthorized() {
		SpiderDataRecord record = recordReader.readRecord("userId", "place", "place:0001");
		SpiderDataGroup groupOut = record.getSpiderDataGroup();
		Assert.assertEquals(groupOut.getNameInData(), "authority",
				"recordOut.getNameInData should be authority");
	}

	@Test(expectedExceptions = AuthorizationException.class)
	public void testReadUnauthorized() {
		recordReader.readRecord("unauthorizedUserId", "place", "place:0001");
	}

	@Test
	public void testReadIncomingLinks() {
		SpiderDataGroup linksPointingToRecord = recordReader.readIncomingLinks("userId", "place",
				"place:0001");
		assertEquals(linksPointingToRecord.getNameInData(), "incomingRecordLinks");
		assertEquals(linksPointingToRecord.getChildren().size(), 1);
		SpiderDataGroup link = (SpiderDataGroup) linksPointingToRecord.getChildren().iterator()
				.next();
		assertEquals(link.getNameInData(), "recordToRecordLink");
		SpiderDataRecordLink from = (SpiderDataRecordLink) link.getFirstChildWithNameInData("from");
		assertEquals(from.getLinkedRecordType(), "place");
		assertEquals(from.getLinkedRecordId(), "place:0002");
		assertEquals(from.getActions().size(), 1);
		assertTrue(from.getActions().contains(Action.READ));

		SpiderDataRecordLink to = (SpiderDataRecordLink) link.getFirstChildWithNameInData("to");
		assertEquals(to.getLinkedRecordType(), "place");
		assertEquals(to.getLinkedRecordId(), "place:0001");

	}

	@Test(expectedExceptions = AuthorizationException.class)
	public void testReadIncomingLinksUnauthorized() {
		recordReader.readIncomingLinks("unauthorizedUserId", "place", "place:0001");
	}

	@Test(expectedExceptions = MisuseException.class)
	public void testReadIncomingLinksAbstractType() {
		RecordStorageSpy recordStorageListReaderSpy = new RecordStorageSpy();
		SpiderRecordReader recordReader = SpiderRecordReaderImp
				.usingAuthorizationAndRecordStorageAndKeyCalculator(authorization,
						recordStorageListReaderSpy, keyCalculator);
		recordReader.readIncomingLinks("userId", "abstract", "place:0001");
	}

	@Test
	public void testActionsOnReadRecord() {
		SpiderDataRecord record = recordReader.readRecord("userId", "place", "place:0001");
		assertEquals(record.getActions().size(), 4);
		assertTrue(record.getActions().contains(Action.DELETE));
	}

	@Test
	public void testActionsOnReadRecordNoIncomingLinks() {
		SpiderDataRecord record = recordReader.readRecord("userId", "place", "place:0002");
		assertEquals(record.getActions().size(), 3);
		assertFalse(record.getActions().contains(Action.READ_INCOMING_LINKS));
	}

	@Test(expectedExceptions = MisuseException.class)
	public void testReadRecordAbstractRecordType() {
		SpiderRecordReader recordReader = SpiderRecordReaderImp
				.usingAuthorizationAndRecordStorageAndKeyCalculator(authorization,
						new RecordStorageSpy(), keyCalculator);
		recordReader.readRecord("userId", "abstract", "xxx");
	}

	@Test(expectedExceptions = RecordNotFoundException.class)
	public void testReadingDataForANonExistingRecordType() {
		recordReader.readRecord("userId", "nonExistingRecordType", "anId");
	}

	@Test
	public void testReadRecordWithDataRecordLinkHasReadActionTopLevel() {
		SpiderRecordReader recordReader = createRecordReaderWithTestDataForLinkedData();

		SpiderDataRecord record = recordReader.readRecord("userId", "dataWithLinks",
				"oneLinkTopLevel");

		RecordLinkTestsAsserter.assertTopLevelLinkContainsReadActionOnly(record);
	}

	private SpiderRecordReader createRecordReaderWithTestDataForLinkedData() {
		recordStorage = new RecordLinkTestsRecordStorage();
		return SpiderRecordReaderImp.usingAuthorizationAndRecordStorageAndKeyCalculator(
				authorization, recordStorage, keyCalculator);
	}

	@Test
	public void testReadRecordWithDataRecordLinkHasReadActionOneLevelDown() {
		SpiderRecordReader recordReader = createRecordReaderWithTestDataForLinkedData();

		SpiderDataRecord record = recordReader.readRecord("userId", "dataWithLinks",
				"oneLinkOneLevelDown");

		RecordLinkTestsAsserter.assertOneLevelDownLinkContainsReadActionOnly(record);
	}

}
