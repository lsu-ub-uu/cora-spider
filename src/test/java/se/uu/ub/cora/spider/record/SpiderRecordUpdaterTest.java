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

import se.uu.ub.cora.bookkeeper.data.DataGroup;
import se.uu.ub.cora.bookkeeper.linkcollector.DataRecordLinkCollector;
import se.uu.ub.cora.bookkeeper.validator.DataValidator;
import se.uu.ub.cora.spider.authentication.AuthenticationException;
import se.uu.ub.cora.spider.authentication.Authenticator;
import se.uu.ub.cora.spider.authentication.AuthenticatorSpy;
import se.uu.ub.cora.spider.authorization.PermissionRuleCalculator;
import se.uu.ub.cora.spider.authorization.SpiderAuthorizator;
import se.uu.ub.cora.spider.data.DataMissingException;
import se.uu.ub.cora.spider.data.SpiderDataAtomic;
import se.uu.ub.cora.spider.data.SpiderDataGroup;
import se.uu.ub.cora.spider.data.SpiderDataRecord;
import se.uu.ub.cora.spider.dependency.SpiderDependencyProviderSpy;
import se.uu.ub.cora.spider.extended.ExtendedFunctionalityProviderSpy;
import se.uu.ub.cora.spider.extended.ExtendedFunctionalitySpy;
import se.uu.ub.cora.spider.record.storage.RecordNotFoundException;
import se.uu.ub.cora.spider.record.storage.RecordStorage;
import se.uu.ub.cora.spider.spy.AuthorizatorAlwaysAuthorizedSpy;
import se.uu.ub.cora.spider.spy.DataRecordLinkCollectorSpy;
import se.uu.ub.cora.spider.spy.DataValidatorAlwaysInvalidSpy;
import se.uu.ub.cora.spider.spy.DataValidatorAlwaysValidSpy;
import se.uu.ub.cora.spider.spy.NoRulesCalculatorStub;
import se.uu.ub.cora.spider.spy.RecordStorageCreateUpdateSpy;
import se.uu.ub.cora.spider.spy.RecordStorageSpy;
import se.uu.ub.cora.spider.spy.RuleCalculatorSpy;
import se.uu.ub.cora.spider.testdata.DataCreator;
import se.uu.ub.cora.spider.testdata.RecordLinkTestsDataCreator;
import se.uu.ub.cora.spider.testdata.SpiderDataCreator;
import se.uu.ub.cora.spider.testdata.TestDataRecordInMemoryStorage;

public class SpiderRecordUpdaterTest {
	private RecordStorage recordStorage;
	private Authenticator authenticator;
	private SpiderAuthorizator spiderAuthorizator;
	private PermissionRuleCalculator keyCalculator;
	private SpiderRecordUpdater recordUpdater;
	private DataValidator dataValidator;
	private DataRecordLinkCollector linkCollector;
	private SpiderDependencyProviderSpy dependencyProvider;
	private ExtendedFunctionalityProviderSpy extendedFunctionalityProvider;
	private DataGroupToRecordEnhancerSpy dataGroupToRecordEnhancer;

	@BeforeMethod
	public void beforeMethod() {
		authenticator = new AuthenticatorSpy();
		spiderAuthorizator = new AuthorizatorAlwaysAuthorizedSpy();
		dataValidator = new DataValidatorAlwaysValidSpy();
		recordStorage = TestDataRecordInMemoryStorage.createRecordStorageInMemoryWithTestData();
		keyCalculator = new NoRulesCalculatorStub();
		linkCollector = new DataRecordLinkCollectorSpy();
		extendedFunctionalityProvider = new ExtendedFunctionalityProviderSpy();
		setUpDependencyProvider();
	}

	private void setUpDependencyProvider() {
		dependencyProvider = new SpiderDependencyProviderSpy();
		dependencyProvider.authenticator = authenticator;
		dependencyProvider.spiderAuthorizator = spiderAuthorizator;
		dependencyProvider.dataValidator = dataValidator;
		dependencyProvider.recordStorage = recordStorage;
		dependencyProvider.keyCalculator = keyCalculator;
		dependencyProvider.linkCollector = linkCollector;
		dependencyProvider.extendedFunctionalityProvider = extendedFunctionalityProvider;
		dataGroupToRecordEnhancer = new DataGroupToRecordEnhancerSpy();
		// SpiderInstanceFactory factory = SpiderInstanceFactoryImp
		// .usingDependencyProvider(dependencyProvider);
		// SpiderInstanceProvider.setSpiderInstanceFactory(factory);
		recordUpdater = SpiderRecordUpdaterImp.usingDependencyProviderAndDataGroupToRecordEnhancer(
				dependencyProvider, dataGroupToRecordEnhancer);
	}

	@Test
	public void testExternalDependenciesAreCalled() {
		spiderAuthorizator = new AuthorizatorAlwaysAuthorizedSpy();
		recordStorage = new RecordStorageSpy();
		keyCalculator = new RuleCalculatorSpy();
		setUpDependencyProvider();

		SpiderDataGroup spiderDataGroup = SpiderDataGroup.withNameInData("nameInData");
		spiderDataGroup.addChild(
				SpiderDataCreator.createRecordInfoWithRecordTypeAndRecordIdAndDataDivider("spyType",
						"spyId", "cora"));
		recordUpdater.updateRecord("someToken78678567", "spyType", "spyId", spiderDataGroup);

		AuthorizatorAlwaysAuthorizedSpy authorizatorSpy = (AuthorizatorAlwaysAuthorizedSpy) spiderAuthorizator;
		assertTrue(authorizatorSpy.authorizedWasCalled);

		assertTrue(((DataValidatorAlwaysValidSpy) dataValidator).validateDataWasCalled);
		assertTrue(((RecordStorageSpy) recordStorage).updateWasCalled);
		assertTrue(((DataRecordLinkCollectorSpy) linkCollector).collectLinksWasCalled);
		assertEquals(((DataRecordLinkCollectorSpy) linkCollector).metadataId, "spyType");

	}

	@Test
	public void testRecordEnhancerCalled() {
		spiderAuthorizator = new AuthorizatorAlwaysAuthorizedSpy();
		recordStorage = new RecordStorageSpy();
		keyCalculator = new RuleCalculatorSpy();
		setUpDependencyProvider();
		SpiderDataGroup spiderDataGroup = SpiderDataGroup.withNameInData("nameInData");
		spiderDataGroup.addChild(
				SpiderDataCreator.createRecordInfoWithRecordTypeAndRecordIdAndDataDivider("spyType",
						"spyId", "cora"));
		recordUpdater.updateRecord("someToken78678567", "spyType", "spyId", spiderDataGroup);
		assertEquals(dataGroupToRecordEnhancer.user.id, "12345");
		assertEquals(dataGroupToRecordEnhancer.recordType, "spyType");
		assertEquals(dataGroupToRecordEnhancer.dataGroup.extractGroup("recordInfo")
				.extractAtomicValue("id"), "spyId");
	}

	@Test
	public void testUserIsAuthorizedForPreviousVersionOfData() {
		recordStorage = new RecordStorageCreateUpdateSpy();
		setUpDependencyProvider();

		SpiderDataGroup dataGroup = getSpiderDataGroupToUpdate();
		dataGroup.addChild(SpiderDataAtomic.withNameInDataAndValue("notStoredValue", "hej"));

		recordUpdater.updateRecord("someToken78678567", "typeWithAutoGeneratedId", "somePlace",
				dataGroup);

		AuthorizatorAlwaysAuthorizedSpy authorizatorSpy = (AuthorizatorAlwaysAuthorizedSpy) spiderAuthorizator;
		assertTrue(authorizatorSpy.authorizedWasCalled);
		assertEquals(authorizatorSpy.actions.get(0), "update");
		assertEquals(authorizatorSpy.users.get(0).id, "12345");
		assertEquals(authorizatorSpy.recordTypes.get(0), "typeWithAutoGeneratedId");
		assertEquals(authorizatorSpy.records.get(0),
				recordStorage.read("typeWithAutoGeneratedId", "somePlace"));
		assertFalse(authorizatorSpy.records.get(0).containsChildWithNameInData("notStoredValue"));

		assertEquals(authorizatorSpy.actions.get(1), "update");
		assertEquals(authorizatorSpy.users.get(1).id, "12345");
		assertEquals(authorizatorSpy.recordTypes.get(1), "typeWithAutoGeneratedId");
		assertEquals(
				authorizatorSpy.records.get(1).getFirstAtomicValueWithNameInData("notStoredValue"),
				"hej");
	}

	@Test(expectedExceptions = AuthenticationException.class)
	public void testAuthenticationNotAuthenticated() {
		recordStorage = new RecordStorageSpy();
		setUpDependencyProvider();
		SpiderDataGroup spiderDataGroup = DataCreator
				.createRecordWithNameInDataAndIdAndTypeAndLinkedRecordId("nameInData", "spyId",
						"spyType", "cora");
		recordUpdater.updateRecord("dummyNonAuthenticatedToken", "spyType", "spyId",
				spiderDataGroup);
	}

	@Test
	public void testExtendedFunctionallityIsCalled() {
		spiderAuthorizator = new AuthorizatorAlwaysAuthorizedSpy();
		recordStorage = new RecordStorageSpy();
		keyCalculator = new RuleCalculatorSpy();
		setUpDependencyProvider();

		SpiderDataGroup spiderDataGroup = DataCreator
				.createRecordWithNameInDataAndIdAndTypeAndLinkedRecordId("nameInData", "spyId",
						"spyType", "cora");
		recordUpdater.updateRecord("someToken78678567", "spyType", "spyId", spiderDataGroup);

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

		SpiderDataRecord recordUpdated = recordUpdater.updateRecord("someToken78678567",
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

		recordUpdater.updateRecord("someToken78678567", "typeWithAutoGeneratedId", "somePlace",
				dataGroup);

		assertEquals(((RecordStorageCreateUpdateSpy) recordStorage).dataDivider, "cora");
	}

	@Test(expectedExceptions = RecordNotFoundException.class)
	public void testUpdateNotFound() {
		SpiderDataGroup record = SpiderDataGroup.withNameInData("authority");
		SpiderDataGroup recordInfo = SpiderDataGroup.withNameInData("recordInfo");
		SpiderDataAtomic idData = SpiderDataAtomic.withNameInDataAndValue("id", "NOT_FOUND");
		recordInfo.addChild(idData);
		recordInfo.addChild(SpiderDataAtomic.withNameInDataAndValue("type", "recordType"));
		recordInfo.addChild(
				SpiderDataAtomic.withNameInDataAndValue("createdBy", "someToken78678567"));
		record.addChild(recordInfo);
		recordUpdater.updateRecord("someToken78678567", "recordType", "NOT_FOUND", record);
	}

	@Test(expectedExceptions = DataMissingException.class)
	public void testUpdateRecordRecordInfoMissing() {
		recordStorage = new RecordStorageCreateUpdateSpy();
		setUpDependencyProvider();

		SpiderDataGroup group = SpiderDataGroup.withNameInData("authority");
		recordUpdater.updateRecord("someToken78678567", "typeWithAutoGeneratedId", "place", group);
	}

	@Test(expectedExceptions = DataMissingException.class)
	public void testUpdateRecordRecordInfoContentMissing() {
		recordStorage = new RecordStorageCreateUpdateSpy();
		setUpDependencyProvider();

		SpiderDataGroup group = SpiderDataGroup.withNameInData("authority");
		SpiderDataGroup createRecordInfo = SpiderDataGroup.withNameInData("recordInfo");
		group.addChild(createRecordInfo);
		recordUpdater.updateRecord("someToken78678567", "typeWithAutoGeneratedId", "place", group);
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

		recordUpdater.updateRecord("someToken78678567", "typeWithAutoGeneratedId", "place",
				dataGroup);
	}

	@Test(expectedExceptions = RecordNotFoundException.class)
	public void testNonExistingRecordType() {
		SpiderDataGroup record = SpiderDataGroup.withNameInData("authority");
		recordUpdater.updateRecord("someToken78678567", "recordType_NOT_EXISTING", "id", record);
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

		recordUpdater.updateRecord("someToken78678567", "typeWithAutoGeneratedId",
				"place_NOT_THE_SAME", dataGroup);
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

		recordUpdater.updateRecord("someToken78678567", "typeWithAutoGeneratedId", "place",
				dataGroup);
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

		recordUpdater.updateRecord("someToken78678567", "recordType", "placeNOT", dataGroup);
	}

	@Test(expectedExceptions = DataException.class)
	public void testLinkedRecordIdDoesNotExist() {
		recordStorage = new RecordLinkTestsRecordStorage();
		linkCollector = DataCreator.getDataRecordLinkCollectorSpyWithCollectedLinkAdded();
		setUpDependencyProvider();

		((RecordLinkTestsRecordStorage) recordStorage).recordIdExistsForRecordType = false;

		SpiderDataGroup dataGroup = RecordLinkTestsDataCreator
				.createDataGroupWithRecordInfoAndLinkOneLevelDown();
		recordUpdater.updateRecord("someToken78678567", "dataWithLinks", "oneLinkOneLevelDown",
				dataGroup);
	}
}
