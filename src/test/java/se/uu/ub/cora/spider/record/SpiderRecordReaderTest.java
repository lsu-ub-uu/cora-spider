package se.uu.ub.cora.spider.record;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertNotNull;
import static org.testng.Assert.assertTrue;

import java.util.List;

import org.testng.Assert;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import se.uu.ub.cora.beefeater.Authorizator;
import se.uu.ub.cora.beefeater.AuthorizatorImp;
import se.uu.ub.cora.spider.data.*;
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
	public void testReadListAuthorized() {
		String userId = "userId";
		String type = "place";
		SpiderRecordList readRecordList = recordReader.readRecordList(userId, type);
		assertEquals(readRecordList.getTotalNumberOfTypeInStorage(), "2",
				"Total number of records should be 2");
		assertEquals(readRecordList.getFromNo(), "0");
		assertEquals(readRecordList.getToNo(), "1");
		List<SpiderDataRecord> records = readRecordList.getRecords();
		SpiderDataRecord spiderDataRecord = records.iterator().next();
		assertNotNull(spiderDataRecord);
	}

	@Test
	public void testReadListAbstractRecordType() {
		RecordStorageSpy recordStorageListReaderSpy = new RecordStorageSpy();
		SpiderRecordReader recordReader = SpiderRecordReaderImp
				.usingAuthorizationAndRecordStorageAndKeyCalculator(authorization,
						recordStorageListReaderSpy, keyCalculator);
		recordReader.readRecordList("userId", "abstract");

		Assert.assertTrue(recordStorageListReaderSpy.readLists.contains("child1"));
		Assert.assertTrue(recordStorageListReaderSpy.readLists.contains("child2"));
	}

	@Test
	public void testActionsOnReadRecord(){
		SpiderDataRecord record = recordReader.readRecord("userId", "place", "place:0001");
		assertEquals(record.getActions().size(), 4);
		assertTrue(record.getActions().contains(Action.DELETE));
	}

	@Test(expectedExceptions = AuthorizationException.class)
	public void testReadListUnauthorized() {
		recordReader.readRecordList("unauthorizedUserId", "place");
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
}
