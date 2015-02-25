package epc.spider.record;

import org.testng.Assert;
import org.testng.annotations.Test;

import epc.beefeater.AuthorizationInputBoundary;
import epc.beefeater.Authorizator;
import epc.metadataformat.data.DataGroup;
import epc.spider.record.RecordInputBoundary;
import epc.spider.record.RecordHandler;
import epc.spider.record.storage.RecordStorageGateway;
import epc.spider.testdata.TestDataRecordInMemoryStorage;

public class RecordStorageTest {
	@Test
	public void testInit() {
		RecordStorageGateway recordStorage = TestDataRecordInMemoryStorage
				.createRecordInMemoryStorageWithTestData();
		AuthorizationInputBoundary authorization = new Authorizator();
		RecordInputBoundary recordRetriever = new RecordHandler(authorization,
				recordStorage);

		DataGroup record = recordRetriever.readRecord("userId","place", "place:0001");

		Assert.assertEquals(record.getDataId(), "authority",
				"recordOut.getDataId should be authority");
	}
}
