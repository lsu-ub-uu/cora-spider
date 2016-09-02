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
import static org.testng.Assert.assertTrue;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import se.uu.ub.cora.beefeater.Authorizator;
import se.uu.ub.cora.beefeater.AuthorizatorImp;
import se.uu.ub.cora.bookkeeper.linkcollector.DataRecordLinkCollector;
import se.uu.ub.cora.bookkeeper.validator.DataValidator;
import se.uu.ub.cora.spider.data.DataMissingException;
import se.uu.ub.cora.spider.data.SpiderDataGroup;
import se.uu.ub.cora.spider.data.SpiderDataRecord;
import se.uu.ub.cora.spider.dependency.SpiderDependencyProviderSpy;
import se.uu.ub.cora.spider.dependency.SpiderInstanceProvider;
import se.uu.ub.cora.spider.record.storage.RecordIdGenerator;
import se.uu.ub.cora.spider.record.storage.RecordNotFoundException;
import se.uu.ub.cora.spider.record.storage.RecordStorage;
import se.uu.ub.cora.spider.record.storage.TimeStampIdGenerator;
import se.uu.ub.cora.spider.spy.AuthorizatorAlwaysAuthorizedSpy;
import se.uu.ub.cora.spider.spy.DataRecordLinkCollectorSpy;
import se.uu.ub.cora.spider.spy.DataValidatorAlwaysValidSpy;
import se.uu.ub.cora.spider.spy.KeyCalculatorSpy;
import se.uu.ub.cora.spider.spy.RecordPermissionKeyCalculatorStub;
import se.uu.ub.cora.spider.spy.RecordStorageCreateUpdateSpy;
import se.uu.ub.cora.spider.spy.RecordStorageSpy;
import se.uu.ub.cora.spider.testdata.SpiderDataCreator;
import se.uu.ub.cora.spider.testdata.TestDataRecordInMemoryStorage;

public class SpiderUploaderTest {
	private RecordStorage recordStorage;
	private StreamStorageSpy streamStorage;
	private Authorizator authorizator;
	private PermissionKeyCalculator keyCalculator;
	private SpiderUploader uploader;
	private DataValidator dataValidator;
	private DataRecordLinkCollector linkCollector;
	private RecordIdGenerator idGenerator;
	private SpiderDependencyProviderSpy dependencyProvider;

	@BeforeMethod
	public void beforeMethod() {
		authorizator = new AuthorizatorImp();
		dataValidator = new DataValidatorAlwaysValidSpy();
		recordStorage = TestDataRecordInMemoryStorage.createRecordStorageInMemoryWithTestData();
		keyCalculator = new RecordPermissionKeyCalculatorStub();
		linkCollector = new DataRecordLinkCollectorSpy();
		idGenerator = new TimeStampIdGenerator();
		streamStorage = new StreamStorageSpy();

		setUpDependencyProvider();

	}

	private void setUpDependencyProvider() {
		dependencyProvider = new SpiderDependencyProviderSpy();
		dependencyProvider.authorizator = authorizator;
		dependencyProvider.dataValidator = dataValidator;
		dependencyProvider.recordStorage = recordStorage;
		dependencyProvider.keyCalculator = keyCalculator;
		dependencyProvider.linkCollector = linkCollector;
		dependencyProvider.idGenerator = idGenerator;
		dependencyProvider.streamStorage = streamStorage;
		SpiderInstanceProvider.setSpiderDependencyProvider(dependencyProvider);
		uploader = SpiderUploaderImp.usingDependencyProvider(dependencyProvider);
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
		InputStream stream = new ByteArrayInputStream("a string".getBytes(StandardCharsets.UTF_8));

		uploader.upload("userId", "image", "image:123456789", stream, "someFileName");

		assertTrue(((RecordStorageSpy) recordStorage).readWasCalled);

		assertTrue(((AuthorizatorAlwaysAuthorizedSpy) authorizator).authorizedWasCalled);
		// assertTrue(((DataValidatorAlwaysValidSpy)
		// dataValidator).validateDataWasCalled);
		// assertTrue(((RecordStorageSpy) recordStorage).updateWasCalled);
		assertTrue(((KeyCalculatorSpy) keyCalculator).calculateKeysWasCalled);
		// assertTrue(((DataRecordLinkCollectorSpy)
		// linkCollector).collectLinksWasCalled);
		// assertEquals(((DataRecordLinkCollectorSpy) linkCollector).metadataId,
		// "spyType");
	}

	@Test
	public void testUploadStream() {
		InputStream stream = new ByteArrayInputStream("a string".getBytes(StandardCharsets.UTF_8));

		SpiderDataRecord recordUpdated = uploader.upload("userId", "image", "image:123456789",
				stream, "someFileName");

		SpiderDataGroup groupUpdated = recordUpdated.getSpiderDataGroup();
		SpiderDataGroup resourceInfo = groupUpdated.extractGroup("resourceInfo");
		SpiderDataGroup master = resourceInfo.extractGroup("master");

		String streamId = master.extractAtomicValue("streamId");
		assertEquals(streamId, streamStorage.streamId);
	}

	// private SpiderDataGroup getSpiderDataGroupToUpdate() {
	// SpiderDataGroup dataGroup =
	// SpiderDataGroup.withNameInData("typeWithUserGeneratedId");
	// SpiderDataGroup createRecordInfo = DataCreator
	// .createRecordInfoWithIdAndLinkedRecordId("somePlace", "cora");
	// createRecordInfo.addChild(
	// SpiderDataAtomic.withNameInDataAndValue("type",
	// "typeWithAutoGeneratedId"));
	// dataGroup.addChild(createRecordInfo);
	// dataGroup.addChild(SpiderDataAtomic.withNameInDataAndValue("atomicId",
	// "atomicValue"));
	// return dataGroup;
	// }
	//

	// @Test
	// public void testActionsOnUpdatedRecordWithIncomingLinks() {
	// RecordStorageCreateUpdateSpy recordStorage = new
	// RecordStorageCreateUpdateSpy();
	// recordStorage.modifiableLinksExistsForRecord = true;
	// setRecordUpdaterWithrecordStorage(recordStorage);
	//
	// SpiderDataGroup dataGroup = getSpiderDataGroupToUpdate();
	// SpiderDataRecord recordUpdated = recordUpdater.updateRecord("userId",
	// "typeWithAutoGeneratedId", "somePlace", dataGroup);
	// assertEquals(recordUpdated.getActions().size(), 3);
	// assertTrue(recordUpdated.getActions().contains(Action.READ));
	// assertTrue(recordUpdated.getActions().contains(Action.UPDATE));
	// assertTrue(recordUpdated.getActions().contains(Action.READ_INCOMING_LINKS));
	//
	// assertFalse(recordUpdated.getActions().contains(Action.DELETE));
	// }
	//
	// @Test
	// public void testActionsOnUpdatedRecordInRecordInfo() {
	// RecordStorageCreateUpdateSpy recordStorage = new
	// RecordStorageCreateUpdateSpy();
	// recordStorage.modifiableLinksExistsForRecord = true;
	// setRecordUpdaterWithrecordStorage(recordStorage);
	//
	// SpiderDataGroup dataGroup = getSpiderDataGroupToUpdate();
	// SpiderDataRecord recordUpdated = recordUpdater.updateRecord("userId",
	// "typeWithAutoGeneratedId", "somePlace", dataGroup);
	//
	// SpiderDataGroup recordInfo =
	// recordUpdated.getSpiderDataGroup().extractGroup("recordInfo");
	// SpiderDataRecordLink dataDivider = (SpiderDataRecordLink) recordInfo
	// .extractGroup("dataDivider");
	//
	// assertTrue(dataDivider.getActions().contains(Action.READ));
	// assertEquals(dataDivider.getActions().size(), 1);
	// }
	//
	// @Test
	// public void testActionsOnUpdatedRecordNoIncomingLinks() {
	// RecordStorageCreateUpdateSpy recordStorage = new
	// RecordStorageCreateUpdateSpy();
	// setRecordUpdaterWithrecordStorage(recordStorage);
	//
	// SpiderDataGroup dataGroup = getSpiderDataGroupToUpdate();
	// SpiderDataRecord recordUpdated = recordUpdater.updateRecord("userId",
	// "typeWithAutoGeneratedId", "somePlace", dataGroup);
	// assertEquals(recordUpdated.getActions().size(), 3);
	// assertReadUpdateDelete(recordUpdated);
	// }
	//
	// @Test
	// public void testActionsOnUpdatedRecordTypeImageNoIncomingLinks() {
	// RecordStorageCreateUpdateSpy recordStorage = new
	// RecordStorageCreateUpdateSpy();
	// setRecordUpdaterWithrecordStorage(recordStorage);
	//
	// SpiderDataGroup dataGroup =
	// createRecordTypeDataGroupWithIdAndAbstract("image", "false");
	// dataGroup.addChild(SpiderDataAtomic.withNameInDataAndValue("parentId",
	// "binary"));
	//
	// SpiderDataRecord recordUpdated = recordUpdater.updateRecord("userId",
	// "recordType", "image",
	// dataGroup);
	// assertEquals(recordUpdated.getActions().size(), 7);
	// assertReadUpdateDelete(recordUpdated);
	// assertTrue(recordUpdated.getActions().contains(Action.CREATE));
	//
	// assertTrue(recordUpdated.getActions().contains(Action.LIST));
	// assertTrue(recordUpdated.getActions().contains(Action.SEARCH));
	// assertTrue(recordUpdated.getActions().contains(Action.CREATE_BY_UPLOAD));
	// assertTrue(recordUpdated.getActions().contains(Action.DELETE));
	// }
	//
	// private SpiderDataGroup createRecordTypeDataGroupWithIdAndAbstract(String
	// id,
	// String abstractString) {
	// SpiderDataGroup dataGroup = SpiderDataGroup.withNameInData("recordType");
	// SpiderDataGroup createRecordInfo =
	// DataCreator.createRecordInfoWithIdAndLinkedRecordId(id,
	// "cora");
	// createRecordInfo.addChild(SpiderDataAtomic.withNameInDataAndValue("type",
	// "recordType"));
	// dataGroup.addChild(createRecordInfo);
	// dataGroup.addChild(SpiderDataAtomic.withNameInDataAndValue("abstract",
	// abstractString));
	// return dataGroup;
	// }
	//
	// private void assertReadUpdateDelete(SpiderDataRecord recordUpdated) {
	// assertTrue(recordUpdated.getActions().contains(Action.READ));
	// assertTrue(recordUpdated.getActions().contains(Action.UPDATE));
	// assertTrue(recordUpdated.getActions().contains(Action.DELETE));
	// }
	//
	// @Test
	// public void testActionsOnUpdatedRecordTypeBinaryNoIncomingLinks() {
	// RecordStorageCreateUpdateSpy recordStorage = new
	// RecordStorageCreateUpdateSpy();
	// setRecordUpdaterWithrecordStorage(recordStorage);
	//
	// SpiderDataGroup dataGroup =
	// createRecordTypeDataGroupWithIdAndAbstract("binary", "false");
	//
	// SpiderDataRecord recordUpdated = recordUpdater.updateRecord("userId",
	// "recordType",
	// "binary", dataGroup);
	// assertEquals(recordUpdated.getActions().size(), 6);
	// assertReadUpdateDelete(recordUpdated);
	//
	// assertTrue(recordUpdated.getActions().contains(Action.LIST));
	// assertTrue(recordUpdated.getActions().contains(Action.SEARCH));
	// assertTrue(recordUpdated.getActions().contains(Action.CREATE_BY_UPLOAD));
	// }

	@Test(expectedExceptions = RecordNotFoundException.class)
	public void testUploadNotFound() {
		InputStream stream = new ByteArrayInputStream("a string".getBytes(StandardCharsets.UTF_8));
		uploader.upload("userId", "recordType", "NOT_FOUND", stream, "someFileName");
	}

	@Test(expectedExceptions = DataMissingException.class)
	public void testUploadStreamIsMissing() {
		RecordStorageCreateUpdateSpy recordStorageSpy = new RecordStorageCreateUpdateSpy();
		recordStorage = recordStorageSpy;
		setUpDependencyProvider();
		uploader.upload("userId", "typeWithAutoGeneratedId", "place", null, "someFileName");
	}

	@Test(expectedExceptions = DataMissingException.class)
	public void testUploadFileNameIsMissing() {
		RecordStorageCreateUpdateSpy recordStorageSpy = new RecordStorageCreateUpdateSpy();
		recordStorage = recordStorageSpy;
		setUpDependencyProvider();
		InputStream stream = new ByteArrayInputStream("a string".getBytes(StandardCharsets.UTF_8));
		uploader.upload("userId", "typeWithAutoGeneratedId", "place", stream, null);
	}

	@Test(expectedExceptions = DataMissingException.class)
	public void testUploadFileNameIsEmpty() {
		RecordStorageCreateUpdateSpy recordStorageSpy = new RecordStorageCreateUpdateSpy();
		recordStorage = recordStorageSpy;
		setUpDependencyProvider();
		InputStream stream = new ByteArrayInputStream("a string".getBytes(StandardCharsets.UTF_8));
		uploader.upload("userId", "typeWithAutoGeneratedId", "place", stream, "");
	}

	// @Test(expectedExceptions = DataException.class)
	// public void testUpdateRecordInvalidData() {
	// DataValidator dataValidator = new DataValidatorAlwaysInvalidSpy();
	// RecordStorageCreateUpdateSpy recordStorage = new
	// RecordStorageCreateUpdateSpy();
	// SpiderRecordUpdater recordUpdater = SpiderRecordUpdaterImp
	// .usingAuthorizationAndDataValidatorAndRecordStorageAndKeyCalculatorAndLinkCollector(
	// authorization, dataValidator, recordStorage, keyCalculator,
	// linkCollector);
	//
	// SpiderDataGroup dataGroup =
	// SpiderDataGroup.withNameInData("typeWithUserGeneratedId");
	// SpiderDataGroup createRecordInfo =
	// SpiderDataGroup.withNameInData("recordInfo");
	// createRecordInfo.addChild(SpiderDataAtomic.withNameInDataAndValue("id",
	// "place"));
	// createRecordInfo.addChild(SpiderDataAtomic.withNameInDataAndValue("type",
	// "recordType"));
	// dataGroup.addChild(createRecordInfo);
	// dataGroup.addChild(SpiderDataAtomic.withNameInDataAndValue("atomicId",
	// "atomicValue"));
	//
	// recordUpdater.updateRecord("userId", "typeWithAutoGeneratedId", "place",
	// dataGroup);
	// }

	@Test(expectedExceptions = RecordNotFoundException.class)
	public void testNonExistingRecordType() {
		InputStream stream = new ByteArrayInputStream("a string".getBytes(StandardCharsets.UTF_8));
		uploader.upload("userId", "recordType_NOT_EXISTING", "id", stream, "someFileName");
	}

	// @Test(expectedExceptions = DataException.class)
	// public void testUpdateRecordIncomingNameInDatasDoNotMatch() {
	// RecordStorageCreateUpdateSpy recordStorage = new
	// RecordStorageCreateUpdateSpy();
	// SpiderRecordUpdater recordUpdater = SpiderRecordUpdaterImp
	// .usingAuthorizationAndDataValidatorAndRecordStorageAndKeyCalculatorAndLinkCollector(
	// authorization, dataValidator, recordStorage, keyCalculator,
	// linkCollector);
	//
	// SpiderDataGroup dataGroup =
	// SpiderDataGroup.withNameInData("typeWithUserGeneratedId");
	// SpiderDataGroup createRecordInfo =
	// SpiderDataGroup.withNameInData("recordInfo");
	// createRecordInfo.addChild(SpiderDataAtomic.withNameInDataAndValue("id",
	// "place"));
	// createRecordInfo.addChild(SpiderDataAtomic.withNameInDataAndValue("type",
	// "recordType"));
	// dataGroup.addChild(createRecordInfo);
	// dataGroup.addChild(SpiderDataAtomic.withNameInDataAndValue("atomicId",
	// "atomicValue"));
	//
	// recordUpdater.updateRecord("userId", "typeWithAutoGeneratedId",
	// "place_NOT_THE_SAME",
	// dataGroup);
	// }
	//
	// @Test(expectedExceptions = DataException.class)
	// public void testUpdateRecordIncomingDataTypesDoNotMatch() {
	// RecordStorageCreateUpdateSpy recordStorage = new
	// RecordStorageCreateUpdateSpy();
	// SpiderRecordUpdater recordUpdater = SpiderRecordUpdaterImp
	// .usingAuthorizationAndDataValidatorAndRecordStorageAndKeyCalculatorAndLinkCollector(
	// authorization, dataValidator, recordStorage, keyCalculator,
	// linkCollector);
	//
	// SpiderDataGroup dataGroup = SpiderDataGroup
	// .withNameInData("typeWithUserGeneratedId_NOT_THE_SAME");
	// SpiderDataGroup createRecordInfo =
	// SpiderDataGroup.withNameInData("recordInfo");
	// createRecordInfo.addChild(SpiderDataAtomic.withNameInDataAndValue("id",
	// "place"));
	// createRecordInfo.addChild(SpiderDataAtomic.withNameInDataAndValue("type",
	// "recordType"));
	// dataGroup.addChild(createRecordInfo);
	// dataGroup.addChild(SpiderDataAtomic.withNameInDataAndValue("atomicId",
	// "atomicValue"));
	//
	// recordUpdater.updateRecord("userId", "typeWithAutoGeneratedId", "place",
	// dataGroup);
	// }
	//
	// @Test(expectedExceptions = MisuseException.class)
	// public void testUpdateRecordAbstractRecordType() {
	// SpiderRecordUpdater recordUpdater = SpiderRecordUpdaterImp
	// .usingAuthorizationAndDataValidatorAndRecordStorageAndKeyCalculatorAndLinkCollector(
	// authorization, dataValidator, new RecordStorageSpy(), keyCalculator,
	// linkCollector);
	//
	// SpiderDataGroup record = SpiderDataGroup.withNameInData("abstract");
	// recordUpdater.updateRecord("userId", "abstract", "xxx", record);
	// }

	@Test(expectedExceptions = AuthorizationException.class)
	public void testUpdateRecordUserNotAuthorisedToUpdateData() {

		SpiderUploader uploader = setupWithUserAuthorizedForUppsala();

		InputStream stream = new ByteArrayInputStream("a string".getBytes(StandardCharsets.UTF_8));
		uploader.upload("userId", "typeWithUserGeneratedId", "gothenburgRecord1", stream,
				"someFileName");
	}

	private SpiderUploader setupWithUserAuthorizedForUppsala() {
		recordStorage = new RecordStorageCreateUpdateSpy();
		keyCalculator = new KeyCalculatorTest();
		authorizator = new AuthorisedForUppsala();
		setUpDependencyProvider();

		SpiderUploader uploader = SpiderUploaderImp.usingDependencyProvider(dependencyProvider);
		return uploader;
	}

	// @Test
	// public void testUpdateRecordWithDataRecordLinkHasReadActionTopLevel() {
	// SpiderDataGroup dataGroup = RecordLinkTestsDataCreator
	// .createSpiderDataGroupWithRecordInfoAndLink();
	//
	// SpiderRecordUpdater recordUpdater =
	// createRecordUpdaterWithTestDataForLinkedData();
	// SpiderDataRecord record = recordUpdater.updateRecord("userId",
	// "dataWithLinks",
	// "oneLinkTopLevel", dataGroup);
	//
	// RecordLinkTestsAsserter.assertTopLevelLinkContainsReadActionOnly(record);
	// }
	//
	// private SpiderRecordUpdater
	// createRecordUpdaterWithTestDataForLinkedData() {
	// recordStorage = new RecordLinkTestsRecordStorage();
	// return SpiderRecordUpdaterImp
	// .usingAuthorizationAndDataValidatorAndRecordStorageAndKeyCalculatorAndLinkCollector(
	// authorization, dataValidator, recordStorage, keyCalculator,
	// linkCollector);
	// }
	//
	// @Test
	// public void testUpdateRecordWithDataRecordLinkHasReadActionOneLevelDown()
	// {
	// SpiderDataGroup dataGroup = RecordLinkTestsDataCreator
	// .createDataGroupWithRecordInfoAndLinkOneLevelDown();
	//
	// SpiderRecordUpdater recordUpdater =
	// createRecordUpdaterWithTestDataForLinkedData();
	// SpiderDataRecord record = recordUpdater.updateRecord("userId",
	// "dataWithLinks",
	// "oneLinkOneLevelDown", dataGroup);
	//
	// RecordLinkTestsAsserter.assertOneLevelDownLinkContainsReadActionOnly(record);
	// }
	//
	// @Test(expectedExceptions = DataException.class)
	// public void testLinkedRecordIdDoesNotExist() {
	// RecordLinkTestsRecordStorage recordStorage = new
	// RecordLinkTestsRecordStorage();
	// recordStorage.recordIdExistsForRecordType = false;
	//
	// DataRecordLinkCollectorSpy linkCollector = DataCreator
	// .getDataRecordLinkCollectorSpyWithCollectedLinkAdded();
	//
	// SpiderRecordUpdater recordUpdater = SpiderRecordUpdaterImp
	// .usingAuthorizationAndDataValidatorAndRecordStorageAndKeyCalculatorAndLinkCollector(
	// authorization, dataValidator, recordStorage, keyCalculator,
	// linkCollector);
	//
	// SpiderDataGroup dataGroup = RecordLinkTestsDataCreator
	// .createDataGroupWithRecordInfoAndLinkOneLevelDown();
	// recordUpdater.updateRecord("userId", "dataWithLinks",
	// "oneLinkOneLevelDown", dataGroup);
	// }
	//
	// @Test(expectedExceptions = DataException.class,
	// expectedExceptionsMessageRegExp = "Data is not valid: child does not
	// exist in parent")
	// public void testChildReferenceDoesNotExistInParent(){
	// RecordStorageCreateUpdateSpy recordStorage = new
	// RecordStorageCreateUpdateSpy();
	// SpiderRecordUpdater recordUpdater = SpiderRecordUpdaterImp
	// .usingAuthorizationAndDataValidatorAndRecordStorageAndKeyCalculatorAndLinkCollector(
	// authorization, dataValidator, recordStorage, keyCalculator,
	// linkCollector);
	//
	// SpiderDataGroup dataGroup =
	// DataCreator.createMetadataGroupWithTwoChildren();
	//
	// SpiderDataAtomic refParent =
	// SpiderDataAtomic.withNameInDataAndValue("refParentId", "testGroup");
	// dataGroup.addChild(refParent);
	//
	// recordUpdater.updateRecord("userId", "metadataGroup", "testNewGroup",
	// dataGroup);
	// }
	//
	// @Test(expectedExceptions = DataException.class,
	// expectedExceptionsMessageRegExp = "Data is not valid: childItem: thatItem
	// does not exist in parent")
	// public void testCollectionVariableItemDoesNotExistInParent(){
	// RecordStorageCreateUpdateSpy recordStorage = new
	// RecordStorageCreateUpdateSpy();
	// SpiderRecordUpdater recordUpdater = SpiderRecordUpdaterImp
	// .usingAuthorizationAndDataValidatorAndRecordStorageAndKeyCalculatorAndLinkCollector(
	// authorization, dataValidator, recordStorage, keyCalculator,
	// linkCollector);
	//
	// SpiderDataGroup dataGroup =
	// DataCreator.createMetadataGroupWithCollectionVariableAsChild();
	//
	// dataGroup.addChild(SpiderDataAtomic.withNameInDataAndValue("refParentId",
	// "testParentMissingItemCollectionVar"));
	//
	// recordUpdater.updateRecord("userId", "metadataCollectionVariable",
	// "testCollectionVar", dataGroup);
	// }
	//
	// @Test(expectedExceptions = DataException.class,
	// expectedExceptionsMessageRegExp = "Data is not valid: final value does
	// not exist in collection")
	// public void testCollectionVariableFinalValueDoesNotExistInCollection(){
	// RecordStorageCreateUpdateSpy recordStorage = new
	// RecordStorageCreateUpdateSpy();
	// SpiderRecordUpdater recordUpdater = SpiderRecordUpdaterImp
	// .usingAuthorizationAndDataValidatorAndRecordStorageAndKeyCalculatorAndLinkCollector(
	// authorization, dataValidator, recordStorage, keyCalculator,
	// linkCollector);
	//
	// SpiderDataGroup dataGroup =
	// DataCreator.createMetadataGroupWithCollectionVariableAsChild();
	// dataGroup.addChild(SpiderDataAtomic.withNameInDataAndValue("finalValue",
	// "doesNotExist"));
	//
	// recordUpdater.updateRecord("userId", "metadataCollectionVariable",
	// "testCollectionVar", dataGroup);
	// }
}
