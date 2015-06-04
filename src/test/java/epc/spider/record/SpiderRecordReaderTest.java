package epc.spider.record;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertNotNull;

import java.util.List;

import org.testng.Assert;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import epc.beefeater.Authorizator;
import epc.beefeater.AuthorizatorImp;
import epc.spider.data.SpiderDataGroup;
import epc.spider.data.SpiderDataRecord;
import epc.spider.data.SpiderRecordList;
import epc.spider.record.storage.RecordStorage;
import epc.spider.testdata.TestDataRecordInMemoryStorage;

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
		Assert.assertEquals(groupOut.getDataId(), "authority",
				"recordOut.getDataId should be authority");
	}

	@Test(expectedExceptions = AuthorizationException.class)
	public void testReadUnauthorized() {
		recordReader.readRecord("unauthorizedUserId", "place", "place:0001");
	}

	@Test
	public void testReadListAuthorized() {
		String userId = "userId";
		String type = "place";
		SpiderRecordList readRecordList = recordReader.readRecordList(userId, type);
		assertEquals(readRecordList.getTotalNumberOfTypeInStorage(), "1",
				"Total number of records should be 1");
		assertEquals(readRecordList.getFromNo(), "0");
		assertEquals(readRecordList.getToNo(), "1");
		List<SpiderDataRecord> records = readRecordList.getRecords();
		SpiderDataRecord spiderDataRecord = records.iterator().next();
		assertNotNull(spiderDataRecord);
	}

	@Test(expectedExceptions = AuthorizationException.class)
	public void testReadListUnauthorized() {
		recordReader.readRecordList("unauthorizedUserId", "place");
	}
}
