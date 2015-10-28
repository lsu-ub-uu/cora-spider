package se.uu.ub.cora.spider.record;

import static org.testng.Assert.assertEquals;

import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import se.uu.ub.cora.beefeater.Authorizator;
import se.uu.ub.cora.beefeater.AuthorizatorImp;
import se.uu.ub.cora.bookkeeper.data.DataGroup;
import se.uu.ub.cora.bookkeeper.validator.DataValidator;
import se.uu.ub.cora.spider.data.DataMissingException;
import se.uu.ub.cora.spider.data.SpiderDataAtomic;
import se.uu.ub.cora.spider.data.SpiderDataGroup;
import se.uu.ub.cora.spider.data.SpiderDataRecord;
import se.uu.ub.cora.spider.record.AuthorizationException;
import se.uu.ub.cora.spider.record.DataException;
import se.uu.ub.cora.spider.record.MisuseException;
import se.uu.ub.cora.spider.record.PermissionKeyCalculator;
import se.uu.ub.cora.spider.record.RecordPermissionKeyCalculator;
import se.uu.ub.cora.spider.record.SpiderRecordUpdater;
import se.uu.ub.cora.spider.record.SpiderRecordUpdaterImp;
import se.uu.ub.cora.spider.record.storage.RecordNotFoundException;
import se.uu.ub.cora.spider.record.storage.RecordStorage;
import se.uu.ub.cora.spider.testdata.TestDataRecordInMemoryStorage;

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

		SpiderDataGroup dataGroup = getSpiderDataGroupToUpdate();

		SpiderDataRecord recordUpdated = recordUpdater.updateRecord("userId",
				"typeWithAutoGeneratedId", "place", dataGroup);
		SpiderDataGroup groupUpdated = recordUpdated.getSpiderDataGroup();

		assertEquals(groupUpdated.extractAtomicValue("atomicId"), "atomicValue");

		DataGroup groupCreated = recordStorage.updateRecord;
		assertEquals(groupCreated.getFirstAtomicValueWithNameInData("atomicId"), "atomicValue");
	}

	private SpiderDataGroup getSpiderDataGroupToUpdate() {
		SpiderDataGroup dataGroup = SpiderDataGroup.withNameInData("typeWithUserGeneratedId");
		SpiderDataGroup createRecordInfo = SpiderDataGroup.withNameInData("recordInfo");
		createRecordInfo.addChild(SpiderDataAtomic.withNameInDataAndValue("id", "place"));
		createRecordInfo
				.addChild(SpiderDataAtomic.withNameInDataAndValue("type", "typeWithAutoGeneratedId"));
		dataGroup.addChild(createRecordInfo);
		dataGroup.addChild(SpiderDataAtomic.withNameInDataAndValue("atomicId", "atomicValue"));
		return dataGroup;
	}

	@Test
	public void testActionsOnUpdatedRecord(){
		RecordStorageCreateUpdateSpy recordStorage = new RecordStorageCreateUpdateSpy();
		recordUpdater = SpiderRecordUpdaterImp
				.usingAuthorizationAndDataValidatorAndRecordStorageAndKeyCalculator(authorization,
						dataValidator, recordStorage, keyCalculator);

		SpiderDataGroup dataGroup = getSpiderDataGroupToUpdate();
		SpiderDataRecord recordUpdated = recordUpdater.updateRecord("userId",
				"typeWithAutoGeneratedId", "place", dataGroup);
		assertEquals(recordUpdated.getActions().size(), 4);
	}


	@Test(expectedExceptions = RecordNotFoundException.class)
	public void testUpdateNotFound() {
		SpiderDataGroup record = SpiderDataGroup.withNameInData("authority");
		SpiderDataGroup recordInfo = SpiderDataGroup.withNameInData("recordInfo");
		SpiderDataAtomic idData = SpiderDataAtomic.withNameInDataAndValue("id", "NOT_FOUND");
		recordInfo.addChild(idData);
		recordInfo.addChild(SpiderDataAtomic.withNameInDataAndValue("type", "recordType"));
		recordInfo.addChild(SpiderDataAtomic.withNameInDataAndValue("createdBy", "userId"));
		record.addChild(recordInfo);
		recordUpdater.updateRecord("userId", "recordType", "NOT_FOUND", record);
	}

	@Test(expectedExceptions = DataMissingException.class)
	public void testUpdateRecordRecordInfoMissing() {
		RecordStorageCreateUpdateSpy recordStorage = new RecordStorageCreateUpdateSpy();
		recordUpdater = SpiderRecordUpdaterImp
				.usingAuthorizationAndDataValidatorAndRecordStorageAndKeyCalculator(authorization,
						dataValidator, recordStorage, keyCalculator);

		SpiderDataGroup group = SpiderDataGroup.withNameInData("authority");
		recordUpdater.updateRecord("userId", "typeWithAutoGeneratedId", "place", group);
	}

	@Test(expectedExceptions = DataMissingException.class)
	public void testUpdateRecordRecordInfoContenceMissing() {
		RecordStorageCreateUpdateSpy recordStorage = new RecordStorageCreateUpdateSpy();
		recordUpdater = SpiderRecordUpdaterImp
				.usingAuthorizationAndDataValidatorAndRecordStorageAndKeyCalculator(authorization,
						dataValidator, recordStorage, keyCalculator);

		SpiderDataGroup group = SpiderDataGroup.withNameInData("authority");
		SpiderDataGroup createRecordInfo = SpiderDataGroup.withNameInData("recordInfo");
		group.addChild(createRecordInfo);
		recordUpdater.updateRecord("userId", "typeWithAutoGeneratedId", "place", group);
	}

	@Test(expectedExceptions = DataException.class)
	public void testUpdateRecordInvalidData() {
		DataValidator dataValidator = new DataValidatorAlwaysInvalidSpy();
		RecordStorageCreateUpdateSpy recordStorage = new RecordStorageCreateUpdateSpy();
		SpiderRecordUpdater recordUpdater = SpiderRecordUpdaterImp
				.usingAuthorizationAndDataValidatorAndRecordStorageAndKeyCalculator(authorization,
						dataValidator, recordStorage, keyCalculator);

		SpiderDataGroup dataGroup = SpiderDataGroup.withNameInData("typeWithUserGeneratedId");
		SpiderDataGroup createRecordInfo = SpiderDataGroup.withNameInData("recordInfo");
		createRecordInfo.addChild(SpiderDataAtomic.withNameInDataAndValue("id", "place"));
		createRecordInfo.addChild(SpiderDataAtomic.withNameInDataAndValue("type", "recordType"));
		dataGroup.addChild(createRecordInfo);
		dataGroup.addChild(SpiderDataAtomic.withNameInDataAndValue("atomicId", "atomicValue"));

		recordUpdater.updateRecord("userId", "typeWithAutoGeneratedId", "place", dataGroup);
	}

	@Test(expectedExceptions = DataException.class)
	public void testNonExistingRecordType() {
		SpiderDataGroup record = SpiderDataGroup.withNameInData("authority");
		recordUpdater.updateRecord("userId", "recordType_NOT_EXISTING", "id", record);
	}

	@Test(expectedExceptions = DataException.class)
	public void testUpdateRecordIncomingNameInDatasDoNotMatch() {
		RecordStorageCreateUpdateSpy recordStorage = new RecordStorageCreateUpdateSpy();
		SpiderRecordUpdater recordUpdater = SpiderRecordUpdaterImp
				.usingAuthorizationAndDataValidatorAndRecordStorageAndKeyCalculator(authorization,
						dataValidator, recordStorage, keyCalculator);

		SpiderDataGroup dataGroup = SpiderDataGroup.withNameInData("typeWithUserGeneratedId");
		SpiderDataGroup createRecordInfo = SpiderDataGroup.withNameInData("recordInfo");
		createRecordInfo.addChild(SpiderDataAtomic.withNameInDataAndValue("id", "place"));
		createRecordInfo.addChild(SpiderDataAtomic.withNameInDataAndValue("type", "recordType"));
		dataGroup.addChild(createRecordInfo);
		dataGroup.addChild(SpiderDataAtomic.withNameInDataAndValue("atomicId", "atomicValue"));

		recordUpdater.updateRecord("userId", "typeWithAutoGeneratedId", "place_NOT_THE_SAME",
				dataGroup);
	}

	@Test(expectedExceptions = DataException.class)
	public void testUpdateRecordIncomingDataTypesDoNotMatch() {
		RecordStorageCreateUpdateSpy recordStorage = new RecordStorageCreateUpdateSpy();
		SpiderRecordUpdater recordUpdater = SpiderRecordUpdaterImp
				.usingAuthorizationAndDataValidatorAndRecordStorageAndKeyCalculator(authorization,
						dataValidator, recordStorage, keyCalculator);

		SpiderDataGroup dataGroup = SpiderDataGroup
				.withNameInData("typeWithUserGeneratedId_NOT_THE_SAME");
		SpiderDataGroup createRecordInfo = SpiderDataGroup.withNameInData("recordInfo");
		createRecordInfo.addChild(SpiderDataAtomic.withNameInDataAndValue("id", "place"));
		createRecordInfo.addChild(SpiderDataAtomic.withNameInDataAndValue("type", "recordType"));
		dataGroup.addChild(createRecordInfo);
		dataGroup.addChild(SpiderDataAtomic.withNameInDataAndValue("atomicId", "atomicValue"));

		recordUpdater.updateRecord("userId", "typeWithAutoGeneratedId", "place", dataGroup);
	}

	@Test(expectedExceptions = MisuseException.class)
	public void testUpdateRecordAbstractRecordType() {
		SpiderRecordUpdater recordUpdater = SpiderRecordUpdaterImp
				.usingAuthorizationAndDataValidatorAndRecordStorageAndKeyCalculator(authorization,
						dataValidator, new RecordStorageSpy(), keyCalculator);

		SpiderDataGroup record = SpiderDataGroup.withNameInData("abstract");
		recordUpdater.updateRecord("userId", "abstract", "xxx", record);
	}

	@Test(expectedExceptions = AuthorizationException.class)
	public void testUpdateRecordUserNotAuthorisedToStoreIncomingData() {

		SpiderRecordUpdater recordUpdater = setupWithUserAuthorizedForUppsala();

		SpiderDataGroup dataGroup = SpiderDataGroup.withNameInData("typeWithUserGeneratedId");
		SpiderDataGroup createRecordInfo = SpiderDataGroup.withNameInData("recordInfo");
		createRecordInfo.addChild(SpiderDataAtomic.withNameInDataAndValue("id", "uppsalaRecord1"));
		createRecordInfo
				.addChild(SpiderDataAtomic.withNameInDataAndValue("type", "typeWithUserGeneratedId"));
		dataGroup.addChild(createRecordInfo);
		dataGroup.addChild(SpiderDataAtomic.withNameInDataAndValue("unit", "gothenburg"));
		recordUpdater.updateRecord("userId", "typeWithUserGeneratedId", "uppsalaRecord1",
				dataGroup);
	}

	private SpiderRecordUpdater setupWithUserAuthorizedForUppsala() {
		RecordStorageCreateUpdateSpy recordStorage = new RecordStorageCreateUpdateSpy();
		PermissionKeyCalculator testKeyCalculator = new KeyCalculatorTest();
		Authorizator testAuthorizator = new AuthorisedForUppsala();

		SpiderRecordUpdater recordUpdater = SpiderRecordUpdaterImp
				.usingAuthorizationAndDataValidatorAndRecordStorageAndKeyCalculator(
						testAuthorizator, dataValidator, recordStorage, testKeyCalculator);
		return recordUpdater;
	}

	@Test(expectedExceptions = AuthorizationException.class)
	public void testUpdateRecordUserNotAuthorisedToUpdateData() {

		SpiderRecordUpdater recordUpdater = setupWithUserAuthorizedForUppsala();

		SpiderDataGroup dataGroup = SpiderDataGroup.withNameInData("typeWithUserGeneratedId");
		SpiderDataGroup createRecordInfo = SpiderDataGroup.withNameInData("recordInfo");
		createRecordInfo.addChild(SpiderDataAtomic.withNameInDataAndValue("id", "gothenburgRecord1"));
		createRecordInfo
				.addChild(SpiderDataAtomic.withNameInDataAndValue("type", "typeWithUserGeneratedId"));
		dataGroup.addChild(createRecordInfo);
		dataGroup.addChild(SpiderDataAtomic.withNameInDataAndValue("unit", "uppsala"));
		recordUpdater.updateRecord("userId", "typeWithUserGeneratedId", "gothenburgRecord1",
				dataGroup);
	}
}
