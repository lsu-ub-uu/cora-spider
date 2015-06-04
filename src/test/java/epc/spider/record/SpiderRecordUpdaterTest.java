package epc.spider.record;

import static org.testng.Assert.assertEquals;

import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import epc.beefeater.Authorizator;
import epc.beefeater.AuthorizatorImp;
import epc.metadataformat.data.DataGroup;
import epc.metadataformat.validator.DataValidator;
import epc.spider.data.DataMissingException;
import epc.spider.data.SpiderDataAtomic;
import epc.spider.data.SpiderDataGroup;
import epc.spider.data.SpiderDataRecord;
import epc.spider.record.storage.RecordNotFoundException;
import epc.spider.record.storage.RecordStorage;
import epc.spider.testdata.TestDataRecordInMemoryStorage;

public class SpiderRecordUpdaterTest {
	private RecordStorage recordStorage;
	private Authorizator authorization;
	private PermissionKeyCalculator keyCalculator;
	private SpiderRecordUpdater recordUpdater;
	private DataValidator dataValidator;

	@BeforeMethod
	public void beforeMethod() {
		authorization = new AuthorizatorImp();
		dataValidator = new DataValidatorAlwaysValidSpy();
		recordStorage = TestDataRecordInMemoryStorage.createRecordStorageInMemoryWithTestData();
		keyCalculator = new RecordPermissionKeyCalculator();
		recordUpdater = SpiderRecordUpdaterImp
				.usingAuthorizationAndDataValidatorAndRecordStorageAndKeyCalculator(authorization,
						dataValidator, recordStorage, keyCalculator);

	}

	@Test
	public void testUpdateRecord() {
		RecordStorageCreateUpdateSpy recordStorage = new RecordStorageCreateUpdateSpy();
		recordUpdater = SpiderRecordUpdaterImp
				.usingAuthorizationAndDataValidatorAndRecordStorageAndKeyCalculator(authorization,
						dataValidator, recordStorage, keyCalculator);

		SpiderDataGroup dataGroup = SpiderDataGroup.withDataId("typeWithUserGeneratedId");
		SpiderDataGroup createRecordInfo = SpiderDataGroup.withDataId("recordInfo");
		createRecordInfo.addChild(SpiderDataAtomic.withDataIdAndValue("id", "place"));
		createRecordInfo.addChild(SpiderDataAtomic.withDataIdAndValue("type", "recordType"));
		dataGroup.addChild(createRecordInfo);
		dataGroup.addChild(SpiderDataAtomic.withDataIdAndValue("atomicId", "atomicValue"));

		SpiderDataRecord recordUpdated = recordUpdater.updateRecord("userId", "recordType",
				"place", dataGroup);
		SpiderDataGroup groupUpdated = recordUpdated.getSpiderDataGroup();
		assertEquals(groupUpdated.extractAtomicValue("atomicId"), "atomicValue");

		DataGroup groupCreated = recordStorage.updateRecord;
		assertEquals(groupCreated.getFirstAtomicValueWithDataId("atomicId"), "atomicValue");

	}

	@Test(expectedExceptions = AuthorizationException.class)
	public void testUpdateUnauthorized() {
		RecordStorageCreateUpdateSpy recordStorage = new RecordStorageCreateUpdateSpy();
		recordUpdater = SpiderRecordUpdaterImp
				.usingAuthorizationAndDataValidatorAndRecordStorageAndKeyCalculator(authorization,
						dataValidator, recordStorage, keyCalculator);
		SpiderDataGroup record = SpiderDataGroup.withDataId("authority");
		SpiderDataGroup createRecordInfo = SpiderDataGroup.withDataId("recordInfo");
		createRecordInfo.addChild(SpiderDataAtomic.withDataIdAndValue("id", "place"));
		createRecordInfo.addChild(SpiderDataAtomic.withDataIdAndValue("type", "recordType"));
		record.addChild(createRecordInfo);

		record.addChild(SpiderDataAtomic.withDataIdAndValue("atomicId", "atomicValue"));
		recordUpdater.updateRecord("unauthorizedUserId", "recordType", "place", record);
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
		recordUpdater.updateRecord("userId", "type", "id", record);
	}

	@Test(expectedExceptions = DataMissingException.class)
	public void testUpdateRecordRecordInfoMissing() {
		RecordStorageCreateUpdateSpy recordStorage = new RecordStorageCreateUpdateSpy();
		recordUpdater = SpiderRecordUpdaterImp
				.usingAuthorizationAndDataValidatorAndRecordStorageAndKeyCalculator(authorization,
						dataValidator, recordStorage, keyCalculator);

		SpiderDataGroup group = SpiderDataGroup.withDataId("authority");
		recordUpdater.updateRecord("userId", "recordType", "place", group);
	}

	@Test(expectedExceptions = DataMissingException.class)
	public void testUpdateRecordRecordInfoContenceMissing() {
		RecordStorageCreateUpdateSpy recordStorage = new RecordStorageCreateUpdateSpy();
		recordUpdater = SpiderRecordUpdaterImp
				.usingAuthorizationAndDataValidatorAndRecordStorageAndKeyCalculator(authorization,
						dataValidator, recordStorage, keyCalculator);

		SpiderDataGroup group = SpiderDataGroup.withDataId("authority");
		SpiderDataGroup createRecordInfo = SpiderDataGroup.withDataId("recordInfo");
		group.addChild(createRecordInfo);
		recordUpdater.updateRecord("userId", "recordType", "place", group);
	}

}
