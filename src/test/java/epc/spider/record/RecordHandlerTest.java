package epc.spider.record;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertNotNull;
import static org.testng.Assert.assertTrue;

import java.util.List;

import org.testng.Assert;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import epc.beefeater.Authorizator;
import epc.beefeater.AuthorizatorImp;
import epc.spider.data.DataMissingException;
import epc.spider.data.SpiderDataAtomic;
import epc.spider.data.SpiderDataGroup;
import epc.spider.data.SpiderDataRecord;
import epc.spider.data.SpiderRecordList;
import epc.spider.record.storage.RecordIdGenerator;
import epc.spider.record.storage.RecordNotFoundException;
import epc.spider.record.storage.RecordStorage;
import epc.spider.record.storage.TimeStampIdGenerator;
import epc.spider.testdata.TestDataRecordInMemoryStorage;

public class RecordHandlerTest {
	private RecordStorage recordStorage;
	private Authorizator authorization;
	private RecordIdGenerator idGenerator;
	private PermissionKeyCalculator keyCalculator;
	private SpiderRecordHandler recordHandler;

	@BeforeMethod
	public void beforeMethod() {
		recordStorage = TestDataRecordInMemoryStorage.createRecordStorageInMemoryWithTestData();
		authorization = new AuthorizatorImp();
		idGenerator = new TimeStampIdGenerator();
		keyCalculator = new RecordPermissionKeyCalculator();
		recordHandler = SpiderRecordHandlerImp
				.usingAuthorizationAndRecordStorageAndIdGeneratorAndKeyCalculator(authorization,
						recordStorage, idGenerator, keyCalculator);

	}

	@Test
	public void testReadListAuthorized() {
		String userId = "userId";
		String type = "place";
		SpiderRecordList readRecordList = recordHandler.readRecordList(userId, type);
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
		recordHandler.readRecordList("unauthorizedUserId", "place");
	}

	@Test
	public void testReadAuthorized() {
		SpiderDataRecord record = recordHandler.readRecord("userId", "place", "place:0001");
		SpiderDataGroup groupOut = record.getSpiderDataGroup();
		Assert.assertEquals(groupOut.getDataId(), "authority",
				"recordOut.getDataId should be authority");
	}

	@Test(expectedExceptions = AuthorizationException.class)
	public void testReadUnauthorized() {
		recordHandler.readRecord("unauthorizedUserId", "place", "place:0001");
	}

	@Test
	public void testCreateRecord() {
		SpiderDataGroup record = SpiderDataGroup.withDataId("authority");

		SpiderDataRecord recordOut = recordHandler.createAndStoreRecord("userId", "type", record);
		SpiderDataGroup groupOut = recordOut.getSpiderDataGroup();
		SpiderDataGroup recordInfo = (SpiderDataGroup) groupOut.getChildren().stream()
				.filter(p -> p.getDataId().equals("recordInfo")).findFirst().get();
		SpiderDataAtomic recordId = (SpiderDataAtomic) recordInfo.getChildren().stream()
				.filter(p -> p.getDataId().equals("id")).findFirst().get();

		Assert.assertNotNull(recordId.getValue(), "A new record should have an id");

		SpiderDataAtomic createdBy = (SpiderDataAtomic) recordInfo.getChildren().stream()
				.filter(p -> p.getDataId().equals("createdBy")).findFirst().get();
		Assert.assertEquals(createdBy.getValue(), "userId");
		SpiderDataAtomic recordType = (SpiderDataAtomic) recordInfo.getChildren().stream()
				.filter(p -> p.getDataId().equals("type")).findFirst().get();
		Assert.assertEquals(recordType.getValue(), "type");

		SpiderDataRecord recordRead = recordHandler.readRecord("userId", "type",
				recordId.getValue());
		SpiderDataGroup groupRead = recordRead.getSpiderDataGroup();
		Assert.assertEquals(groupOut.getDataId(), groupRead.getDataId(),
				"Returned and read record should have the same dataId");

	}

	@Test(expectedExceptions = AuthorizationException.class)
	public void testCreateRecordUnauthorized() {
		SpiderDataGroup record = SpiderDataGroup.withDataId("authority");
		recordHandler.createAndStoreRecord("unauthorizedUserId", "type", record);
	}

	@Test
	public void testDeleteAuthorized() {
		RecordStorageDeleteSpy recordStorage = new RecordStorageDeleteSpy();
		recordHandler = SpiderRecordHandlerImp
				.usingAuthorizationAndRecordStorageAndIdGeneratorAndKeyCalculator(authorization,
						recordStorage, idGenerator, keyCalculator);
		recordHandler.deleteRecord("userId", "place", "place:0001");
		assertTrue(recordStorage.deleteWasCalled);
	}

	@Test(expectedExceptions = AuthorizationException.class)
	public void testDeleteUnauthorized() {
		recordHandler.deleteRecord("unauthorizedUserId", "place", "place:0001");
	}

	@Test(expectedExceptions = RecordNotFoundException.class)
	public void testDeleteNotFound() {
		recordHandler.deleteRecord("userId", "place", "place:0001_NOT_FOUND");
	}

	@Test
	public void testUpdateRecord() {
		SpiderDataGroup dataGroup = SpiderDataGroup.withDataId("authority");
		SpiderDataRecord dataRecord = recordHandler.createAndStoreRecord("userId", "type",
				dataGroup);
		dataGroup = dataRecord.getSpiderDataGroup();
		dataGroup.addChild(SpiderDataAtomic.withDataIdAndValue("atomicId", "atomicValue"));

		SpiderDataGroup recordInfo = (SpiderDataGroup) dataGroup.getChildren().stream()
				.filter(p -> p.getDataId().equals("recordInfo")).findFirst().get();
		SpiderDataAtomic id = (SpiderDataAtomic) recordInfo.getChildren().stream()
				.filter(p -> p.getDataId().equals("id")).findFirst().get();

		SpiderDataRecord recordUpdated = recordHandler.updateRecord("userId", "type",
				id.getValue(), dataGroup);
		SpiderDataGroup groupUpdated = recordUpdated.getSpiderDataGroup();
		SpiderDataAtomic childOut = (SpiderDataAtomic) groupUpdated.getChildren().get(1);
		assertEquals(childOut.getValue(), "atomicValue");

		SpiderDataRecord recordRead = recordHandler.readRecord("userId", "type", id.getValue());
		SpiderDataGroup groupRead = recordRead.getSpiderDataGroup();
		SpiderDataAtomic childRead = (SpiderDataAtomic) groupRead.getChildren().get(1);
		assertEquals(childRead.getValue(), "atomicValue");

	}

	@Test(expectedExceptions = AuthorizationException.class)
	public void testUpdateUnauthorized() {
		SpiderDataGroup record = SpiderDataGroup.withDataId("authority");
		recordHandler.createAndStoreRecord("userId", "type", record);

		SpiderDataGroup recordInfo = (SpiderDataGroup) record.getChildren().stream()
				.filter(p -> p.getDataId().equals("recordInfo")).findFirst().get();
		SpiderDataAtomic id = (SpiderDataAtomic) recordInfo.getChildren().stream()
				.filter(p -> p.getDataId().equals("id")).findFirst().get();

		record.addChild(SpiderDataAtomic.withDataIdAndValue("atomicId", "atomicValue"));
		recordHandler.updateRecord("unauthorizedUserId", "type", id.getValue(), record);
	}

	@Test(expectedExceptions = RecordNotFoundException.class)
	public void testUpdateNotFound() {
		SpiderDataGroup record = SpiderDataGroup.withDataId("authority");
		SpiderDataGroup recordInfo = SpiderDataGroup.withDataId("recordInfo");
		SpiderDataAtomic idData = SpiderDataAtomic.withDataIdAndValue("id", "NOT_FOUND");
		recordInfo.addChild(idData);
		recordInfo.addChild(SpiderDataAtomic.withDataIdAndValue("type", "type"));
		recordInfo.addChild(SpiderDataAtomic.withDataIdAndValue("createdBy", "userId"));
		record.addChild(recordInfo);
		recordHandler.updateRecord("userId", "type", "id", record);
	}

	@Test(expectedExceptions = DataMissingException.class)
	public void testUpdateRecordRecordInfoMissing() {
		SpiderDataGroup group = SpiderDataGroup.withDataId("authority");
		SpiderDataRecord record = recordHandler.createAndStoreRecord("userId", "type", group);
		group = record.getSpiderDataGroup();
		SpiderDataGroup recordInfo = (SpiderDataGroup) group.getChildren().stream()
				.filter(p -> p.getDataId().equals("recordInfo")).findFirst().get();
		SpiderDataAtomic id = (SpiderDataAtomic) recordInfo.getChildren().stream()
				.filter(p -> p.getDataId().equals("id")).findFirst().get();
		group.getChildren().clear();
		group.addChild(SpiderDataGroup.withDataId("childGroupId"));
		recordHandler.updateRecord("userId", "type", id.getValue(), group);
	}

	@Test(expectedExceptions = DataMissingException.class)
	public void testUpdateRecordRecordInfoContenceMissing() {
		SpiderDataGroup group = SpiderDataGroup.withDataId("authority");
		SpiderDataRecord record = recordHandler.createAndStoreRecord("userId", "type", group);
		group = record.getSpiderDataGroup();
		SpiderDataGroup recordInfo = (SpiderDataGroup) group.getChildren().stream()
				.filter(p -> p.getDataId().equals("recordInfo")).findFirst().get();
		SpiderDataAtomic id = (SpiderDataAtomic) recordInfo.getChildren().stream()
				.filter(p -> p.getDataId().equals("id")).findFirst().get();
		((SpiderDataGroup) group.getChildren().get(0)).getChildren().clear();
		recordHandler.updateRecord("userId", "type", id.getValue(), group);
	}

}
