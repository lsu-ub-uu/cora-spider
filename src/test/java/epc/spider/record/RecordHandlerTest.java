package epc.spider.record;

import org.testng.Assert;
import org.testng.annotations.Test;

import epc.beefeater.Authorizator;
import epc.beefeater.AuthorizatorImp;
import epc.spider.data.SpiderDataAtomic;
import epc.spider.data.SpiderDataGroup;
import epc.spider.record.storage.RecordIdGenerator;
import epc.spider.record.storage.RecordStorage;
import epc.spider.record.storage.TimeStampIdGenerator;
import epc.spider.testdata.TestDataRecordInMemoryStorage;

public class RecordHandlerTest {
	@Test
	public void testReadAuthorized() {
		RecordStorage recordStorage = TestDataRecordInMemoryStorage
				.createRecordStorageInMemoryWithTestData();
		Authorizator authorization = new AuthorizatorImp();
		RecordIdGenerator idGenerator = new TimeStampIdGenerator();
		PermissionKeyCalculator keyCalculator = new RecordPermissionKeyCalculator();
		SpiderRecordHandler recordHandler = SpiderRecordHandlerImp
				.usingAuthorizationAndRecordStorageAndIdGeneratorAndKeyCalculator(authorization,
						recordStorage, idGenerator, keyCalculator);

		SpiderDataGroup record = recordHandler.readRecord("userId", "place", "place:0001");

		Assert.assertEquals(record.getDataId(), "authority",
				"recordOut.getDataId should be authority");
	}

	@Test(expectedExceptions = AuthorizationException.class)
	public void testReadUnauthorized() {
		RecordStorage recordStorage = TestDataRecordInMemoryStorage
				.createRecordStorageInMemoryWithTestData();
		Authorizator authorization = new AuthorizatorImp();
		RecordIdGenerator idGenerator = new TimeStampIdGenerator();
		PermissionKeyCalculator keyCalculator = new RecordPermissionKeyCalculator();
		SpiderRecordHandler recordHandler = SpiderRecordHandlerImp
				.usingAuthorizationAndRecordStorageAndIdGeneratorAndKeyCalculator(authorization,
						recordStorage, idGenerator, keyCalculator);

		recordHandler.readRecord("unauthorizedUserId", "place", "place:0001");
	}

	@Test
	public void testCreateRecord() {
		RecordStorage recordStorage = TestDataRecordInMemoryStorage
				.createRecordStorageInMemoryWithTestData();
		Authorizator authorization = new AuthorizatorImp();
		RecordIdGenerator idGenerator = new TimeStampIdGenerator();
		PermissionKeyCalculator keyCalculator = new RecordPermissionKeyCalculator();

		SpiderRecordHandler recordHandler = SpiderRecordHandlerImp
				.usingAuthorizationAndRecordStorageAndIdGeneratorAndKeyCalculator(authorization,
						recordStorage, idGenerator, keyCalculator);

		SpiderDataGroup record = SpiderDataGroup.withDataId("authority");

		SpiderDataGroup recordOut = recordHandler.createAndStoreRecord("userId", "type", record);

		SpiderDataGroup recordInfo = (SpiderDataGroup) recordOut.getChildren().stream()
				.filter(p -> p.getDataId().equals("recordInfo")).findFirst().get();
		SpiderDataAtomic recordId = (SpiderDataAtomic) recordInfo.getChildren().stream()
				.filter(p -> p.getDataId().equals("id")).findFirst().get();

		Assert.assertNotNull(recordId.getValue(), "A new record should have an id");

		SpiderDataAtomic createdBy = (SpiderDataAtomic) recordInfo.getChildren().stream()
				.filter(p -> p.getDataId().equals("createdBy")).findFirst().get();
		Assert.assertEquals(createdBy.getValue(), "userId");
		SpiderDataAtomic recordType = (SpiderDataAtomic) recordInfo.getChildren().stream()
				.filter(p -> p.getDataId().equals("recordType")).findFirst().get();
		Assert.assertEquals(recordType.getValue(), "type");

		SpiderDataGroup recordRead = recordHandler
				.readRecord("userId", "type", recordId.getValue());
		Assert.assertEquals(recordOut, recordRead, "Returned and read record should be the same");

	}

	@Test(expectedExceptions = AuthorizationException.class)
	public void testCreateRecordUnauthorized() {
		RecordStorage recordStorage = TestDataRecordInMemoryStorage
				.createRecordStorageInMemoryWithTestData();
		Authorizator authorization = new AuthorizatorImp();
		RecordIdGenerator idGenerator = new TimeStampIdGenerator();
		PermissionKeyCalculator keyCalculator = new RecordPermissionKeyCalculator();

		SpiderRecordHandler recordHandler = SpiderRecordHandlerImp
				.usingAuthorizationAndRecordStorageAndIdGeneratorAndKeyCalculator(authorization,
						recordStorage, idGenerator, keyCalculator);

		SpiderDataGroup record = SpiderDataGroup.withDataId("authority");

		recordHandler.createAndStoreRecord("unauthorizedUserId", "type", record);
	}
}
