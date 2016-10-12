/*
 * Copyright 2015, 2016 Uppsala University Library
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

import java.util.List;

import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import se.uu.ub.cora.beefeater.Authorizator;
import se.uu.ub.cora.beefeater.AuthorizatorImp;
import se.uu.ub.cora.bookkeeper.data.DataGroup;
import se.uu.ub.cora.bookkeeper.linkcollector.DataRecordLinkCollector;
import se.uu.ub.cora.bookkeeper.validator.DataValidator;
import se.uu.ub.cora.spider.data.Action;
import se.uu.ub.cora.spider.data.DataMissingException;
import se.uu.ub.cora.spider.data.SpiderDataAtomic;
import se.uu.ub.cora.spider.data.SpiderDataGroup;
import se.uu.ub.cora.spider.data.SpiderDataRecord;
import se.uu.ub.cora.spider.data.SpiderDataRecordLink;
import se.uu.ub.cora.spider.dependency.SpiderDependencyProviderSpy;
import se.uu.ub.cora.spider.dependency.SpiderInstanceFactory;
import se.uu.ub.cora.spider.dependency.SpiderInstanceFactoryImp;
import se.uu.ub.cora.spider.dependency.SpiderInstanceProvider;
import se.uu.ub.cora.spider.extended.ExtendedFunctionalityProviderSpy;
import se.uu.ub.cora.spider.extended.ExtendedFunctionalitySpy;
import se.uu.ub.cora.spider.record.storage.RecordNotFoundException;
import se.uu.ub.cora.spider.record.storage.RecordStorage;
import se.uu.ub.cora.spider.spy.AuthorizatorAlwaysAuthorizedSpy;
import se.uu.ub.cora.spider.spy.DataRecordLinkCollectorSpy;
import se.uu.ub.cora.spider.spy.DataValidatorAlwaysInvalidSpy;
import se.uu.ub.cora.spider.spy.DataValidatorAlwaysValidSpy;
import se.uu.ub.cora.spider.spy.KeyCalculatorSpy;
import se.uu.ub.cora.spider.spy.RecordPermissionKeyCalculatorStub;
import se.uu.ub.cora.spider.spy.RecordStorageCreateUpdateSpy;
import se.uu.ub.cora.spider.spy.RecordStorageSpy;
import se.uu.ub.cora.spider.testdata.DataCreator;
import se.uu.ub.cora.spider.testdata.RecordLinkTestsDataCreator;
import se.uu.ub.cora.spider.testdata.SpiderDataCreator;
import se.uu.ub.cora.spider.testdata.TestDataRecordInMemoryStorage;

public class SpiderRecordUpdaterTest {
	private RecordStorage recordStorage;
	private Authorizator authorizator;
	private PermissionKeyCalculator keyCalculator;
	private SpiderRecordUpdater recordUpdater;
	private DataValidator dataValidator;
	private DataRecordLinkCollector linkCollector;
	private SpiderDependencyProviderSpy dependencyProvider;
	private ExtendedFunctionalityProviderSpy extendedFunctionalityProvider;

	@BeforeMethod
	public void beforeMethod() {
		authorizator = new AuthorizatorImp();
		dataValidator = new DataValidatorAlwaysValidSpy();
		recordStorage = TestDataRecordInMemoryStorage.createRecordStorageInMemoryWithTestData();
		keyCalculator = new RecordPermissionKeyCalculatorStub();
		linkCollector = new DataRecordLinkCollectorSpy();
		extendedFunctionalityProvider = new ExtendedFunctionalityProviderSpy();
		setUpDependencyProvider();
	}

	private void setUpDependencyProvider() {
		dependencyProvider = new SpiderDependencyProviderSpy();
		dependencyProvider.authorizator = authorizator;
		dependencyProvider.dataValidator = dataValidator;
		dependencyProvider.recordStorage = recordStorage;
		dependencyProvider.keyCalculator = keyCalculator;
		dependencyProvider.linkCollector = linkCollector;
		dependencyProvider.extendedFunctionalityProvider = extendedFunctionalityProvider;
		SpiderInstanceFactory factory = SpiderInstanceFactoryImp
				.usingDependencyProvider(dependencyProvider);
		SpiderInstanceProvider.setSpiderInstanceFactory(factory);
		recordUpdater = SpiderRecordUpdaterImp.usingDependencyProvider(dependencyProvider);
	}

	@Test
	public void testExternalDependenciesAreCalled() {
		authorizator = new AuthorizatorAlwaysAuthorizedSpy();
		recordStorage = new RecordStorageSpy();
		keyCalculator = new KeyCalculatorSpy();
		setUpDependencyProvider();

		SpiderDataGroup spiderDataGroup = SpiderDataGroup.withNameInData("nameInData");
		spiderDataGroup.addChild(
				SpiderDataCreator.createRecordInfoWithRecordTypeAndRecordIdAndDataDivider("spyType",
						"spyId", "cora"));
		recordUpdater.updateRecord("userId", "spyType", "spyId", spiderDataGroup);

		assertTrue(((AuthorizatorAlwaysAuthorizedSpy) authorizator).authorizedWasCalled);
		assertTrue(((DataValidatorAlwaysValidSpy) dataValidator).validateDataWasCalled);
		assertTrue(((RecordStorageSpy) recordStorage).updateWasCalled);
		assertTrue(((KeyCalculatorSpy) keyCalculator).calculateKeysWasCalled);
		assertTrue(((DataRecordLinkCollectorSpy) linkCollector).collectLinksWasCalled);
		assertEquals(((DataRecordLinkCollectorSpy) linkCollector).metadataId, "spyType");
	}

	@Test
	public void testExtendedFunctionallityIsCalled() {
		authorizator = new AuthorizatorAlwaysAuthorizedSpy();
		recordStorage = new RecordStorageSpy();
		keyCalculator = new KeyCalculatorSpy();
		setUpDependencyProvider();

		SpiderDataGroup spiderDataGroup = DataCreator
				.createRecordWithNameInDataAndIdAndTypeAndLinkedRecordId("nameInData", "spyId",
						"spyType", "cora");
		recordUpdater.updateRecord("userId", "spyType", "spyId", spiderDataGroup);

		assertFetchedFunctionalityHasBeenCalled(
				extendedFunctionalityProvider.fetchedFunctionalityForUpdateBeforeMetadataValidation);
		assertFetchedFunctionalityHasBeenCalled(
				extendedFunctionalityProvider.fetchedFunctionalityForUpdateAfterMetadataValidation);
	}

	private void assertFetchedFunctionalityHasBeenCalled(
			List<ExtendedFunctionalitySpy> fetchedFunctionalityForCreateAfterMetadataValidation) {
		ExtendedFunctionalitySpy extendedFunctionality = fetchedFunctionalityForCreateAfterMetadataValidation
				.get(0);
		assertTrue(extendedFunctionality.extendedFunctionalityHasBeenCalled);
		ExtendedFunctionalitySpy extendedFunctionality2 = fetchedFunctionalityForCreateAfterMetadataValidation
				.get(0);
		assertTrue(extendedFunctionality2.extendedFunctionalityHasBeenCalled);
	}

	@Test
	public void testUpdateRecord() {
		recordStorage = new RecordStorageCreateUpdateSpy();
		setUpDependencyProvider();

		SpiderDataGroup dataGroup = getSpiderDataGroupToUpdate();

		SpiderDataRecord recordUpdated = recordUpdater.updateRecord("userId",
				"typeWithAutoGeneratedId", "somePlace", dataGroup);
		SpiderDataGroup groupUpdated = recordUpdated.getSpiderDataGroup();

		assertEquals(groupUpdated.extractAtomicValue("atomicId"), "atomicValue");

		DataGroup groupCreated = ((RecordStorageCreateUpdateSpy) recordStorage).updateRecord;
		assertEquals(groupCreated.getFirstAtomicValueWithNameInData("atomicId"), "atomicValue");
	}

	private SpiderDataGroup getSpiderDataGroupToUpdate() {
		SpiderDataGroup dataGroup = SpiderDataGroup.withNameInData("typeWithUserGeneratedId");
		SpiderDataGroup createRecordInfo = DataCreator
				.createRecordInfoWithIdAndLinkedRecordId("somePlace", "cora");
		createRecordInfo.addChild(
				SpiderDataAtomic.withNameInDataAndValue("type", "typeWithAutoGeneratedId"));
		dataGroup.addChild(createRecordInfo);
		dataGroup.addChild(SpiderDataAtomic.withNameInDataAndValue("atomicId", "atomicValue"));
		return dataGroup;
	}

	@Test
	public void testUpdateRecordDataDividerExtractedFromData() {
		recordStorage = new RecordStorageCreateUpdateSpy();
		setUpDependencyProvider();

		SpiderDataGroup dataGroup = getSpiderDataGroupToUpdate();

		recordUpdater.updateRecord("userId", "typeWithAutoGeneratedId", "somePlace", dataGroup);

		assertEquals(((RecordStorageCreateUpdateSpy) recordStorage).dataDivider, "cora");
	}

	@Test
	public void testActionsOnUpdatedRecordWithIncomingLinks() {
		recordStorage = new RecordStorageCreateUpdateSpy();
		setUpDependencyProvider();
		((RecordStorageCreateUpdateSpy) recordStorage).modifiableLinksExistsForRecord = true;

		SpiderDataGroup dataGroup = getSpiderDataGroupToUpdate();
		SpiderDataRecord recordUpdated = recordUpdater.updateRecord("userId",
				"typeWithAutoGeneratedId", "somePlace", dataGroup);
		assertEquals(recordUpdated.getActions().size(), 3);
		assertTrue(recordUpdated.getActions().contains(Action.READ));
		assertTrue(recordUpdated.getActions().contains(Action.UPDATE));
		assertTrue(recordUpdated.getActions().contains(Action.READ_INCOMING_LINKS));

		assertFalse(recordUpdated.getActions().contains(Action.DELETE));
	}

	@Test
	public void testActionsOnUpdatedRecordInRecordInfo() {
		recordStorage = new RecordStorageCreateUpdateSpy();
		setUpDependencyProvider();
		((RecordStorageCreateUpdateSpy) recordStorage).modifiableLinksExistsForRecord = true;

		SpiderDataGroup dataGroup = getSpiderDataGroupToUpdate();
		SpiderDataRecord recordUpdated = recordUpdater.updateRecord("userId",
				"typeWithAutoGeneratedId", "somePlace", dataGroup);

		SpiderDataGroup recordInfo = recordUpdated.getSpiderDataGroup().extractGroup("recordInfo");
		SpiderDataRecordLink dataDivider = (SpiderDataRecordLink) recordInfo
				.extractGroup("dataDivider");

		assertTrue(dataDivider.getActions().contains(Action.READ));
		assertEquals(dataDivider.getActions().size(), 1);
	}

	@Test
	public void testActionsOnUpdatedRecordNoIncomingLinks() {
		recordStorage = new RecordStorageCreateUpdateSpy();
		setUpDependencyProvider();

		SpiderDataGroup dataGroup = getSpiderDataGroupToUpdate();
		SpiderDataRecord recordUpdated = recordUpdater.updateRecord("userId",
				"typeWithAutoGeneratedId", "somePlace", dataGroup);
		assertEquals(recordUpdated.getActions().size(), 3);
		assertReadUpdateDelete(recordUpdated);
	}

	@Test
	public void testActionsOnUpdatedRecordTypeImageNoIncomingLinks() {
		recordStorage = new RecordStorageCreateUpdateSpy();
		setUpDependencyProvider();

		SpiderDataGroup dataGroup = createRecordTypeDataGroupWithIdAndAbstract("image", "false");
		dataGroup.addChild(SpiderDataAtomic.withNameInDataAndValue("parentId", "binary"));

		SpiderDataRecord recordUpdated = recordUpdater.updateRecord("userId", "recordType", "image",
				dataGroup);
		assertEquals(recordUpdated.getActions().size(), 6);
		assertReadUpdateDelete(recordUpdated);
		assertTrue(recordUpdated.getActions().contains(Action.CREATE));

		assertTrue(recordUpdated.getActions().contains(Action.LIST));
		assertTrue(recordUpdated.getActions().contains(Action.SEARCH));
		assertTrue(recordUpdated.getActions().contains(Action.DELETE));
		assertFalse(recordUpdated.getActions().contains(Action.UPLOAD));
	}

	private SpiderDataGroup createRecordTypeDataGroupWithIdAndAbstract(String id,
			String abstractString) {
		SpiderDataGroup dataGroup = SpiderDataGroup.withNameInData("recordType");
		SpiderDataGroup createRecordInfo = DataCreator.createRecordInfoWithIdAndLinkedRecordId(id,
				"cora");
		createRecordInfo.addChild(SpiderDataAtomic.withNameInDataAndValue("type", "recordType"));
		dataGroup.addChild(createRecordInfo);
		dataGroup.addChild(SpiderDataAtomic.withNameInDataAndValue("abstract", abstractString));
		return dataGroup;
	}

	private void assertReadUpdateDelete(SpiderDataRecord recordUpdated) {
		assertTrue(recordUpdated.getActions().contains(Action.READ));
		assertTrue(recordUpdated.getActions().contains(Action.UPDATE));
		assertTrue(recordUpdated.getActions().contains(Action.DELETE));
	}

	@Test
	public void testActionsOnUpdatedRecordTypeBinaryNoIncomingLinks() {
		recordStorage = new RecordStorageCreateUpdateSpy();
		setUpDependencyProvider();

		SpiderDataGroup dataGroup = createRecordTypeDataGroupWithIdAndAbstract("binary", "false");

		SpiderDataRecord recordUpdated = recordUpdater.updateRecord("userId", "recordType",
				"binary", dataGroup);
		assertEquals(recordUpdated.getActions().size(), 5);
		assertReadUpdateDelete(recordUpdated);

		assertTrue(recordUpdated.getActions().contains(Action.LIST));
		assertTrue(recordUpdated.getActions().contains(Action.SEARCH));
		assertFalse(recordUpdated.getActions().contains(Action.UPLOAD));
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
		recordStorage = new RecordStorageCreateUpdateSpy();
		setUpDependencyProvider();

		SpiderDataGroup group = SpiderDataGroup.withNameInData("authority");
		recordUpdater.updateRecord("userId", "typeWithAutoGeneratedId", "place", group);
	}

	@Test(expectedExceptions = DataMissingException.class)
	public void testUpdateRecordRecordInfoContentMissing() {
		recordStorage = new RecordStorageCreateUpdateSpy();
		setUpDependencyProvider();

		SpiderDataGroup group = SpiderDataGroup.withNameInData("authority");
		SpiderDataGroup createRecordInfo = SpiderDataGroup.withNameInData("recordInfo");
		group.addChild(createRecordInfo);
		recordUpdater.updateRecord("userId", "typeWithAutoGeneratedId", "place", group);
	}

	@Test(expectedExceptions = DataException.class)
	public void testUpdateRecordInvalidData() {
		dataValidator = new DataValidatorAlwaysInvalidSpy();
		recordStorage = new RecordStorageCreateUpdateSpy();
		setUpDependencyProvider();

		SpiderDataGroup dataGroup = SpiderDataGroup.withNameInData("typeWithUserGeneratedId");
		SpiderDataGroup createRecordInfo = SpiderDataGroup.withNameInData("recordInfo");
		createRecordInfo.addChild(SpiderDataAtomic.withNameInDataAndValue("id", "place"));
		createRecordInfo.addChild(SpiderDataAtomic.withNameInDataAndValue("type", "recordType"));
		dataGroup.addChild(createRecordInfo);
		dataGroup.addChild(SpiderDataAtomic.withNameInDataAndValue("atomicId", "atomicValue"));

		recordUpdater.updateRecord("userId", "typeWithAutoGeneratedId", "place", dataGroup);
	}

	@Test(expectedExceptions = RecordNotFoundException.class)
	public void testNonExistingRecordType() {
		SpiderDataGroup record = SpiderDataGroup.withNameInData("authority");
		recordUpdater.updateRecord("userId", "recordType_NOT_EXISTING", "id", record);
	}

	@Test(expectedExceptions = DataException.class)
	public void testUpdateRecordIncomingNameInDatasDoNotMatch() {
		recordStorage = new RecordStorageCreateUpdateSpy();
		setUpDependencyProvider();

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
		recordStorage = new RecordStorageCreateUpdateSpy();
		setUpDependencyProvider();

		SpiderDataGroup dataGroup = SpiderDataGroup
				.withNameInData("typeWithUserGeneratedId_NOT_THE_SAME");
		SpiderDataGroup createRecordInfo = SpiderDataGroup.withNameInData("recordInfo");
		createRecordInfo.addChild(SpiderDataAtomic.withNameInDataAndValue("id", "place"));
		createRecordInfo.addChild(SpiderDataAtomic.withNameInDataAndValue("type", "recordType"));
		dataGroup.addChild(createRecordInfo);
		dataGroup.addChild(SpiderDataAtomic.withNameInDataAndValue("atomicId", "atomicValue"));

		recordUpdater.updateRecord("userId", "typeWithAutoGeneratedId", "place", dataGroup);
	}

	@Test(expectedExceptions = DataException.class)
	public void testUpdateRecordIncomingDataIdDoNotMatch() {
		recordStorage = new RecordStorageCreateUpdateSpy();
		setUpDependencyProvider();

		SpiderDataGroup dataGroup = SpiderDataGroup
				.withNameInData("typeWithUserGeneratedId_NOT_THE_SAME");
		SpiderDataGroup createRecordInfo = SpiderDataGroup.withNameInData("recordInfo");
		createRecordInfo.addChild(SpiderDataAtomic.withNameInDataAndValue("id", "place"));
		createRecordInfo.addChild(SpiderDataAtomic.withNameInDataAndValue("type", "recordType"));
		dataGroup.addChild(createRecordInfo);
		dataGroup.addChild(SpiderDataAtomic.withNameInDataAndValue("atomicId", "atomicValue"));

		recordUpdater.updateRecord("userId", "recordType", "placeNOT", dataGroup);
	}

	@Test(expectedExceptions = MisuseException.class)
	public void testUpdateRecordAbstractRecordType() {
		recordStorage = new RecordStorageSpy();
		setUpDependencyProvider();

		SpiderDataGroup record = SpiderDataGroup.withNameInData("abstract");
		recordUpdater.updateRecord("userId", "abstract", "xxx", record);
	}

	@Test(expectedExceptions = AuthorizationException.class)
	public void testUpdateRecordUserNotAuthorisedToStoreIncomingData() {
		recordStorage = new RecordStorageCreateUpdateSpy();
		keyCalculator = new KeyCalculatorTest();
		authorizator = new AuthorisedForUppsala();
		setUpDependencyProvider();

		SpiderDataGroup dataGroup = SpiderDataGroup.withNameInData("typeWithUserGeneratedId");
		SpiderDataGroup createRecordInfo = SpiderDataGroup.withNameInData("recordInfo");
		createRecordInfo.addChild(SpiderDataAtomic.withNameInDataAndValue("id", "uppsalaRecord1"));
		createRecordInfo.addChild(
				SpiderDataAtomic.withNameInDataAndValue("type", "typeWithUserGeneratedId"));
		dataGroup.addChild(createRecordInfo);
		dataGroup.addChild(SpiderDataAtomic.withNameInDataAndValue("unit", "gothenburg"));
		recordUpdater.updateRecord("userId", "typeWithUserGeneratedId", "uppsalaRecord1",
				dataGroup);
	}

	@Test(expectedExceptions = AuthorizationException.class)
	public void testUpdateRecordUserNotAuthorisedToUpdateData() {
		recordStorage = new RecordStorageCreateUpdateSpy();
		keyCalculator = new KeyCalculatorTest();
		authorizator = new AuthorisedForUppsala();
		setUpDependencyProvider();

		SpiderDataGroup dataGroup = SpiderDataGroup.withNameInData("typeWithUserGeneratedId");
		SpiderDataGroup createRecordInfo = SpiderDataGroup.withNameInData("recordInfo");
		createRecordInfo
				.addChild(SpiderDataAtomic.withNameInDataAndValue("id", "gothenburgRecord1"));
		createRecordInfo.addChild(
				SpiderDataAtomic.withNameInDataAndValue("type", "typeWithUserGeneratedId"));
		dataGroup.addChild(createRecordInfo);
		dataGroup.addChild(SpiderDataAtomic.withNameInDataAndValue("unit", "uppsala"));
		recordUpdater.updateRecord("userId", "typeWithUserGeneratedId", "gothenburgRecord1",
				dataGroup);
	}

	@Test
	public void testUpdateRecordWithDataRecordLinkHasReadActionTopLevel() {
		recordStorage = new RecordLinkTestsRecordStorage();
		setUpDependencyProvider();
		SpiderDataGroup dataGroup = RecordLinkTestsDataCreator
				.createSpiderDataGroupWithRecordInfoAndLink();

		SpiderDataRecord record = recordUpdater.updateRecord("userId", "dataWithLinks",
				"oneLinkTopLevel", dataGroup);

		RecordLinkTestsAsserter.assertTopLevelLinkContainsReadActionOnly(record);
	}

	@Test
	public void testUpdateRecordWithDataRecordLinkHasReadActionOneLevelDown() {
		recordStorage = new RecordLinkTestsRecordStorage();
		setUpDependencyProvider();
		SpiderDataGroup dataGroup = RecordLinkTestsDataCreator
				.createDataGroupWithRecordInfoAndLinkOneLevelDown();

		SpiderDataRecord record = recordUpdater.updateRecord("userId", "dataWithLinks",
				"oneLinkOneLevelDown", dataGroup);

		RecordLinkTestsAsserter.assertOneLevelDownLinkContainsReadActionOnly(record);
	}

	@Test(expectedExceptions = DataException.class)
	public void testLinkedRecordIdDoesNotExist() {
		recordStorage = new RecordLinkTestsRecordStorage();
		linkCollector = DataCreator.getDataRecordLinkCollectorSpyWithCollectedLinkAdded();
		setUpDependencyProvider();

		((RecordLinkTestsRecordStorage) recordStorage).recordIdExistsForRecordType = false;

		SpiderDataGroup dataGroup = RecordLinkTestsDataCreator
				.createDataGroupWithRecordInfoAndLinkOneLevelDown();
		recordUpdater.updateRecord("userId", "dataWithLinks", "oneLinkOneLevelDown", dataGroup);
	}

	@Test(expectedExceptions = DataException.class, expectedExceptionsMessageRegExp = "Data is not valid: child does not exist in parent")
	public void testChildReferenceDoesNotExistInParent() {
		recordStorage = new RecordStorageCreateUpdateSpy();
		setUpDependencyProvider();

		SpiderDataGroup dataGroup = DataCreator.createMetadataGroupWithTwoChildren();

		SpiderDataAtomic refParent = SpiderDataAtomic.withNameInDataAndValue("refParentId",
				"testGroup");
		dataGroup.addChild(refParent);

		recordUpdater.updateRecord("userId", "metadataGroup", "testNewGroup", dataGroup);
	}

	@Test(expectedExceptions = DataException.class, expectedExceptionsMessageRegExp = "Data is not valid: childItem: thatItem does not exist in parent")
	public void testCollectionVariableItemDoesNotExistInParent() {
		recordStorage = new RecordStorageCreateUpdateSpy();
		setUpDependencyProvider();

		SpiderDataGroup dataGroup = DataCreator.createMetadataGroupWithCollectionVariableAsChild();

		dataGroup.addChild(SpiderDataAtomic.withNameInDataAndValue("refParentId",
				"testParentMissingItemCollectionVar"));

		recordUpdater.updateRecord("userId", "metadataCollectionVariable", "testCollectionVar",
				dataGroup);
	}

	@Test(expectedExceptions = DataException.class, expectedExceptionsMessageRegExp = "Data is not valid: final value does not exist in collection")
	public void testCollectionVariableFinalValueDoesNotExistInCollection() {
		recordStorage = new RecordStorageCreateUpdateSpy();
		setUpDependencyProvider();

		SpiderDataGroup dataGroup = DataCreator.createMetadataGroupWithCollectionVariableAsChild();
		dataGroup.addChild(SpiderDataAtomic.withNameInDataAndValue("finalValue", "doesNotExist"));

		recordUpdater.updateRecord("userId", "metadataCollectionVariable", "testCollectionVar",
				dataGroup);
	}
}
