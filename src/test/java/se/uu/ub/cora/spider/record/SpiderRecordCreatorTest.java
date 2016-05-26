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

import org.testng.Assert;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import se.uu.ub.cora.beefeater.Authorizator;
import se.uu.ub.cora.beefeater.AuthorizatorImp;
import se.uu.ub.cora.bookkeeper.data.DataAtomic;
import se.uu.ub.cora.bookkeeper.data.DataGroup;
import se.uu.ub.cora.bookkeeper.linkcollector.DataRecordLinkCollector;
import se.uu.ub.cora.bookkeeper.validator.DataValidator;
import se.uu.ub.cora.spider.data.Action;
import se.uu.ub.cora.spider.data.SpiderDataAtomic;
import se.uu.ub.cora.spider.data.SpiderDataGroup;
import se.uu.ub.cora.spider.data.SpiderDataRecord;
import se.uu.ub.cora.spider.data.SpiderDataRecordLink;
import se.uu.ub.cora.spider.record.storage.RecordConflictException;
import se.uu.ub.cora.spider.record.storage.RecordIdGenerator;
import se.uu.ub.cora.spider.record.storage.RecordNotFoundException;
import se.uu.ub.cora.spider.record.storage.RecordStorage;
import se.uu.ub.cora.spider.record.storage.TimeStampIdGenerator;
import se.uu.ub.cora.spider.spy.AuthorizatorAlwaysAuthorizedSpy;
import se.uu.ub.cora.spider.spy.DataRecordLinkCollectorSpy;
import se.uu.ub.cora.spider.spy.DataValidatorAlwaysInvalidSpy;
import se.uu.ub.cora.spider.spy.DataValidatorAlwaysValidSpy;
import se.uu.ub.cora.spider.spy.IdGeneratorSpy;
import se.uu.ub.cora.spider.spy.KeyCalculatorSpy;
import se.uu.ub.cora.spider.spy.RecordPermissionKeyCalculatorStub;
import se.uu.ub.cora.spider.spy.RecordStorageCreateUpdateSpy;
import se.uu.ub.cora.spider.spy.RecordStorageSpy;
import se.uu.ub.cora.spider.testdata.DataCreator;
import se.uu.ub.cora.spider.testdata.RecordLinkTestsDataCreator;
import se.uu.ub.cora.spider.testdata.TestDataRecordInMemoryStorage;

import static org.testng.Assert.*;

public class SpiderRecordCreatorTest {
	private RecordStorage recordStorage;
	private Authorizator authorization;
	private RecordIdGenerator idGenerator;
	private PermissionKeyCalculator keyCalculator;
	private SpiderRecordCreator recordCreator;
	private DataValidator dataValidator;
	private DataRecordLinkCollector linkCollector;

	@BeforeMethod
	public void beforeMethod() {
		authorization = new AuthorizatorImp();
		dataValidator = new DataValidatorAlwaysValidSpy();
		recordStorage = TestDataRecordInMemoryStorage.createRecordStorageInMemoryWithTestData();
		idGenerator = new TimeStampIdGenerator();
		keyCalculator = new RecordPermissionKeyCalculatorStub();
		linkCollector = new DataRecordLinkCollectorSpy();
		recordCreator = SpiderRecordCreatorImp
				.usingAuthorizationAndDataValidatorAndRecordStorageAndIdGeneratorAndKeyCalculatorAndLinkCollector(
						authorization, dataValidator, recordStorage, idGenerator, keyCalculator,
						linkCollector);

	}

	@Test
	public void testExternalDependenciesAreCalled() {
		authorization = new AuthorizatorAlwaysAuthorizedSpy();
		dataValidator = new DataValidatorAlwaysValidSpy();
		recordStorage = new RecordStorageSpy();
		idGenerator = new IdGeneratorSpy();
		keyCalculator = new KeyCalculatorSpy();
		linkCollector = new DataRecordLinkCollectorSpy();

		SpiderRecordCreator recordCreator = SpiderRecordCreatorImp
				.usingAuthorizationAndDataValidatorAndRecordStorageAndIdGeneratorAndKeyCalculatorAndLinkCollector(
						authorization, dataValidator, recordStorage, idGenerator, keyCalculator,
						linkCollector);
		SpiderDataGroup spiderDataGroup = DataCreator
				.createRecordWithNameInDataAndLinkedRecordId("nameInData", "cora");
		recordCreator.createAndStoreRecord("userId", "spyType", spiderDataGroup);

		assertTrue(((AuthorizatorAlwaysAuthorizedSpy) authorization).authorizedWasCalled);
		assertTrue(((DataValidatorAlwaysValidSpy) dataValidator).validateDataWasCalled);
		assertTrue(((RecordStorageSpy) recordStorage).createWasCalled);
		assertTrue(((IdGeneratorSpy) idGenerator).getIdForTypeWasCalled);
		assertTrue(((KeyCalculatorSpy) keyCalculator).calculateKeysWasCalled);
		assertTrue(((DataRecordLinkCollectorSpy) linkCollector).collectLinksWasCalled);

	}

	@Test(expectedExceptions = DataException.class)
	public void testCreateRecordInvalidData() {
		DataValidator dataValidator = new DataValidatorAlwaysInvalidSpy();
		SpiderRecordCreator recordCreator = SpiderRecordCreatorImp
				.usingAuthorizationAndDataValidatorAndRecordStorageAndIdGeneratorAndKeyCalculatorAndLinkCollector(
						authorization, dataValidator, recordStorage, idGenerator, keyCalculator,
						linkCollector);
		SpiderDataGroup spiderDataGroup = SpiderDataGroup.withNameInData("nameInData");
		recordCreator.createAndStoreRecord("userId", "recordType", spiderDataGroup);
	}

	@Test
	public void testCreateRecordAutogeneratedId() {
		RecordStorageCreateUpdateSpy recordStorage = new RecordStorageCreateUpdateSpy();
		setRecordCreatorWithRecordStorage(recordStorage);

		SpiderDataGroup record = DataCreator
				.createRecordWithNameInDataAndLinkedRecordId("typeWithAutoGeneratedId", "cora");

		SpiderDataRecord recordOut = recordCreator.createAndStoreRecord("userId",
				"typeWithAutoGeneratedId", record);
		SpiderDataGroup groupOut = recordOut.getSpiderDataGroup();
		SpiderDataGroup recordInfo = groupOut.extractGroup("recordInfo");
		String recordId = recordInfo.extractAtomicValue("id");

		assertNotNull(recordId);

		assertEquals(recordInfo.extractAtomicValue("createdBy"), "userId");
		assertEquals(recordInfo.extractAtomicValue("type"), "typeWithAutoGeneratedId");

		DataGroup groupCreated = recordStorage.createRecord;
		assertEquals(groupOut.getNameInData(), groupCreated.getNameInData(),
				"Returned and read record should have the same nameInData");
	}

	@Test
	public void testCreateRecordUserSuppliedId() {
		RecordStorageCreateUpdateSpy recordStorage = new RecordStorageCreateUpdateSpy();
		setRecordCreatorWithRecordStorage(recordStorage);

		SpiderDataGroup record = DataCreator.createRecordWithNameInDataAndIdAndLinkedRecordId(
				"typeWithUserGeneratedId", "somePlace", "cora");

		SpiderDataRecord recordOut = recordCreator.createAndStoreRecord("userId",
				"typeWithUserGeneratedId", record);
		SpiderDataGroup groupOut = recordOut.getSpiderDataGroup();
		SpiderDataGroup recordInfo = groupOut.extractGroup("recordInfo");
		String recordId = recordInfo.extractAtomicValue("id");
		assertNotNull(recordId, "A new record should have an id");

		assertEquals(recordInfo.extractAtomicValue("createdBy"), "userId");
		assertEquals(recordInfo.extractAtomicValue("type"), "typeWithUserGeneratedId");

		DataGroup groupCreated = recordStorage.createRecord;
		assertEquals(groupOut.getNameInData(), groupCreated.getNameInData(),
				"Returned and read record should have the same nameInData");
	}

	@Test
	public void testCreateRecordDataDividerExtractedFromData() {
		RecordStorageCreateUpdateSpy recordStorage = new RecordStorageCreateUpdateSpy();
		setRecordCreatorWithRecordStorage(recordStorage);

		SpiderDataGroup record = DataCreator.createRecordWithNameInDataAndIdAndLinkedRecordId(
				"typeWithUserGeneratedId", "somePlace", "cora");

		recordCreator.createAndStoreRecord("userId", "typeWithUserGeneratedId", record);

		assertEquals(recordStorage.dataDivider, "cora");
	}

	@Test(expectedExceptions = AuthorizationException.class)
	public void testCreateRecordUnauthorized() {
		SpiderDataGroup record = DataCreator
				.createRecordWithNameInDataAndLinkedRecordId("authority", "cora");
		recordCreator.createAndStoreRecord("unauthorizedUserId", "place", record);
	}

	@Test(expectedExceptions = RecordNotFoundException.class)
	public void testNonExistingRecordType() {
		SpiderDataGroup record = SpiderDataGroup.withNameInData("authority");
		recordCreator.createAndStoreRecord("userId", "recordType_NOT_EXISTING", record);
	}

	@Test(expectedExceptions = MisuseException.class)
	public void testCreateRecordAbstractRecordType() {
		SpiderRecordCreator recordCreator = SpiderRecordCreatorImp
				.usingAuthorizationAndDataValidatorAndRecordStorageAndIdGeneratorAndKeyCalculatorAndLinkCollector(
						authorization, dataValidator, new RecordStorageSpy(), idGenerator,
						keyCalculator, linkCollector);

		SpiderDataGroup record = SpiderDataGroup.withNameInData("abstract");
		recordCreator.createAndStoreRecord("userId", "abstract", record);
	}

	@Test(expectedExceptions = RecordConflictException.class)
	public void testCreateRecordDuplicateUserSuppliedId() {
		SpiderRecordCreator recordCreator = SpiderRecordCreatorImp
				.usingAuthorizationAndDataValidatorAndRecordStorageAndIdGeneratorAndKeyCalculatorAndLinkCollector(
						authorization, dataValidator, recordStorage, idGenerator, keyCalculator,
						linkCollector);

		SpiderDataGroup record = DataCreator
				.createRecordWithNameInDataAndIdAndLinkedRecordId("place", "somePlace", "cora");

		recordCreator.createAndStoreRecord("userId", "place", record);
		recordCreator.createAndStoreRecord("userId", "place", record);
	}

	@Test
	public void testActionsOnCreatedRecord() {
		RecordStorageCreateUpdateSpy recordStorage = new RecordStorageCreateUpdateSpy();
		setRecordCreatorWithRecordStorage(recordStorage);

		SpiderDataGroup record = DataCreator
				.createRecordWithNameInDataAndLinkedRecordId("typeWithAutoGeneratedId", "cora");

		SpiderDataRecord recordOut = recordCreator.createAndStoreRecord("userId",
				"typeWithAutoGeneratedId", record);
		assertEquals(recordOut.getActions().size(), 3);
		assertTrue(recordOut.getActions().contains(Action.READ));
		assertTrue(recordOut.getActions().contains(Action.UPDATE));
		assertTrue(recordOut.getActions().contains(Action.DELETE));
		assertFalse(recordOut.getActions().contains(Action.READ_INCOMING_LINKS));
	}

	@Test
	public void testActionsOnCreatedRecordInRecordInfo() {
		RecordStorageCreateUpdateSpy recordStorage = new RecordStorageCreateUpdateSpy();
		setRecordCreatorWithRecordStorage(recordStorage);

		SpiderDataGroup record = DataCreator
				.createRecordWithNameInDataAndLinkedRecordId("typeWithAutoGeneratedId", "cora");

		SpiderDataRecord recordOut = recordCreator.createAndStoreRecord("userId",
				"typeWithAutoGeneratedId", record);

		SpiderDataGroup recordInfo = recordOut.getSpiderDataGroup().extractGroup("recordInfo");
		SpiderDataRecordLink dataDivider = (SpiderDataRecordLink) recordInfo
				.extractGroup("dataDivider");

		assertTrue(dataDivider.getActions().contains(Action.READ));
		assertEquals(dataDivider.getActions().size(), 1);
	}

	private void setRecordCreatorWithRecordStorage(RecordStorageCreateUpdateSpy recordStorage) {
		recordCreator = SpiderRecordCreatorImp
				.usingAuthorizationAndDataValidatorAndRecordStorageAndIdGeneratorAndKeyCalculatorAndLinkCollector(
						authorization, dataValidator, recordStorage, idGenerator, keyCalculator,
						linkCollector);
	}

	@Test
	public void testActionsOnCreatedRecordRecordTypeImage() {
		RecordStorageCreateUpdateSpy recordStorage = new RecordStorageCreateUpdateSpy();
		setRecordCreatorWithRecordStorage(recordStorage);

		SpiderDataGroup record = DataCreator
				.createRecordWithNameInDataAndIdAndLinkedRecordId("recordType", "image", "cora");
		record.addChild(SpiderDataAtomic.withNameInDataAndValue("parentId", "binary"));

		SpiderDataRecord recordOut = recordCreator.createAndStoreRecord("userId", "recordType",
				record);
		assertEquals(recordOut.getActions().size(), 7);
		assertTrue(recordOut.getActions().contains(Action.READ));
		assertTrue(recordOut.getActions().contains(Action.UPDATE));
		assertTrue(recordOut.getActions().contains(Action.DELETE));
		assertTrue(recordOut.getActions().contains(Action.CREATE));

		assertTrue(recordOut.getActions().contains(Action.LIST));
		assertTrue(recordOut.getActions().contains(Action.SEARCH));
		assertTrue(recordOut.getActions().contains(Action.CREATE_BY_UPLOAD));
	}

	@Test
	public void testCreateRecordWithDataRecordLinkHasReadActionTopLevel() {
		SpiderDataGroup dataGroup = RecordLinkTestsDataCreator.createDataGroupWithLink();
		dataGroup.addChild(DataCreator.createRecordInfoWithLinkedRecordId("cora"));

		SpiderRecordCreator recordCreator = createRecordCreatorWithTestDataForLinkedData();
		SpiderDataRecord record = recordCreator.createAndStoreRecord("userId", "dataWithLinks",
				dataGroup);

		RecordLinkTestsAsserter.assertTopLevelLinkContainsReadActionOnly(record);
	}

	private SpiderRecordCreator createRecordCreatorWithTestDataForLinkedData() {
		recordStorage = new RecordLinkTestsRecordStorage();
		return SpiderRecordCreatorImp
				.usingAuthorizationAndDataValidatorAndRecordStorageAndIdGeneratorAndKeyCalculatorAndLinkCollector(
						authorization, dataValidator, recordStorage, idGenerator, keyCalculator,
						linkCollector);
	}

	@Test
	public void testCreateRecordWithDataRecordLinkHasReadActionOneLevelDown() {
		SpiderDataGroup dataGroup = RecordLinkTestsDataCreator
				.createDataGroupWithLinkOneLevelDown();
		dataGroup.addChild(DataCreator.createRecordInfoWithLinkedRecordId("cora"));

		SpiderRecordCreator recordCreator = createRecordCreatorWithTestDataForLinkedData();
		SpiderDataRecord record = recordCreator.createAndStoreRecord("userId", "dataWithLinks",
				dataGroup);

		RecordLinkTestsAsserter.assertOneLevelDownLinkContainsReadActionOnly(record);
	}

	@Test(expectedExceptions = DataException.class)
	public void testLinkedRecordIdDoesNotExist(){
		RecordLinkTestsRecordStorage recordStorage = new RecordLinkTestsRecordStorage();
		recordStorage.recordIdExistsForRecordType = false;

		DataRecordLinkCollectorSpy linkCollector =
				DataCreator.getDataRecordLinkCollectorSpyWithCollectedLinkAdded();

		SpiderDataGroup dataGroup = RecordLinkTestsDataCreator.createDataGroupWithLink();
		dataGroup.addChild(DataCreator.createRecordInfoWithLinkedRecordId("cora"));

		SpiderRecordCreator recordCreator =
				getRecordCreatorAndSetRecordStorageAndLinkCollector(recordStorage, linkCollector);
		recordCreator.createAndStoreRecord("userId", "dataWithLinks",dataGroup);
	}

	@Test
	public void testLinkedRecordIdExists(){
		RecordLinkTestsRecordStorage recordStorage = new RecordLinkTestsRecordStorage();
		recordStorage.recordIdExistsForRecordType = true;

		DataRecordLinkCollectorSpy linkCollector =
				DataCreator.getDataRecordLinkCollectorSpyWithCollectedLinkAdded();

		SpiderDataGroup dataGroup = RecordLinkTestsDataCreator.createDataGroupWithLink();
		dataGroup.addChild(DataCreator.createRecordInfoWithLinkedRecordId("cora"));

		SpiderRecordCreator recordCreator = getRecordCreatorAndSetRecordStorageAndLinkCollector(recordStorage, linkCollector);
		recordCreator.createAndStoreRecord("userId", "dataWithLinks",dataGroup);
		assertTrue(recordStorage.createWasRead);
	}

	private SpiderRecordCreator getRecordCreatorAndSetRecordStorageAndLinkCollector(RecordLinkTestsRecordStorage recordStorage, DataRecordLinkCollectorSpy linkCollector) {
		return SpiderRecordCreatorImp
                    .usingAuthorizationAndDataValidatorAndRecordStorageAndIdGeneratorAndKeyCalculatorAndLinkCollector(
                            authorization, dataValidator, recordStorage, idGenerator, keyCalculator,
                            linkCollector);
	}

}
