package epc.spider.record;

import org.testng.Assert;
import org.testng.annotations.Test;

import epc.beefeater.AuthorizationInputBoundary;
import epc.beefeater.Authorizator;
import epc.metadataformat.data.DataAtomic;
import epc.metadataformat.data.DataGroup;
import epc.spider.record.storage.RecordIdGenerator;
import epc.spider.record.storage.RecordStorageGateway;
import epc.spider.record.storage.TimeStampIdGenerator;
import epc.spider.testdata.TestDataRecordInMemoryStorage;

public class RecordHandlerTest {
	@Test
	public void testReadAuthorized() {
		RecordStorageGateway recordStorage = TestDataRecordInMemoryStorage
				.createRecordInMemoryStorageWithTestData();
		AuthorizationInputBoundary authorization = new Authorizator();
		RecordIdGenerator idGenerator = new TimeStampIdGenerator();
		PermissionKeyCalculator keyCalculator = new RecordPermissionKeyCalculator();
		RecordInputBoundary recordHandler = new RecordHandler(authorization,
				recordStorage, idGenerator, keyCalculator);

		DataGroup record = recordHandler.readRecord("userId", "place",
				"place:0001");

		Assert.assertEquals(record.getDataId(), "authority",
				"recordOut.getDataId should be authority");
	}

	@Test(expectedExceptions = AuthorizationException.class)
	public void testReadUnauthorized() {
		RecordStorageGateway recordStorage = TestDataRecordInMemoryStorage
				.createRecordInMemoryStorageWithTestData();
		AuthorizationInputBoundary authorization = new Authorizator();
		RecordIdGenerator idGenerator = new TimeStampIdGenerator();
		PermissionKeyCalculator keyCalculator = new RecordPermissionKeyCalculator();
		RecordInputBoundary recordHandler = new RecordHandler(authorization,
				recordStorage, idGenerator, keyCalculator);

		recordHandler.readRecord("unauthorizedUserId", "place", "place:0001");
	}

	@Test
	public void testCreateRecord() {
		RecordStorageGateway recordStorage = TestDataRecordInMemoryStorage
				.createRecordInMemoryStorageWithTestData();
		AuthorizationInputBoundary authorization = new Authorizator();
		RecordIdGenerator idGenerator = new TimeStampIdGenerator();
		PermissionKeyCalculator keyCalculator = new RecordPermissionKeyCalculator();

		RecordInputBoundary recordHandler = new RecordHandler(authorization,
				recordStorage, idGenerator, keyCalculator);

		DataGroup record = new DataGroup("authority");

		DataGroup recordOut = recordHandler.createAndStoreRecord("userId",
				"type", record);

		DataGroup recordInfo = (DataGroup) recordOut.getChildren().stream()
				.filter(p -> p.getDataId().equals("recordInfo")).findFirst()
				.get();
		DataAtomic recordId = (DataAtomic) recordInfo.getChildren().stream()
				.filter(p -> p.getDataId().equals("id")).findFirst().get();

		Assert.assertNotNull(recordId.getValue(),
				"A new record should have an id");

		DataGroup recordRead = recordHandler.readRecord("userId", "type",
				recordId.getValue());
		Assert.assertEquals(recordOut, recordRead,
				"Returned and read record should be the same");

	}

	@Test(expectedExceptions = AuthorizationException.class)
	public void testCreateRecordUnauthorized() {
		RecordStorageGateway recordStorage = TestDataRecordInMemoryStorage
				.createRecordInMemoryStorageWithTestData();
		AuthorizationInputBoundary authorization = new Authorizator();
		RecordIdGenerator idGenerator = new TimeStampIdGenerator();
		PermissionKeyCalculator keyCalculator = new RecordPermissionKeyCalculator();

		RecordInputBoundary recordHandler = new RecordHandler(authorization,
				recordStorage, idGenerator, keyCalculator);

		DataGroup record = new DataGroup("authority");

		recordHandler
				.createAndStoreRecord("unauthorizedUserId", "type", record);
	}
}
