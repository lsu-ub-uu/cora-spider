/*
 * Copyright 2019 Uppsala University Library
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

import java.util.HashMap;

import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import se.uu.ub.cora.bookkeeper.linkcollector.DataRecordLinkCollector;
import se.uu.ub.cora.bookkeeper.termcollector.DataGroupTermCollector;
import se.uu.ub.cora.bookkeeper.validator.DataValidator;
import se.uu.ub.cora.spider.authentication.Authenticator;
import se.uu.ub.cora.spider.authentication.AuthenticatorSpy;
import se.uu.ub.cora.spider.authorization.PermissionRuleCalculator;
import se.uu.ub.cora.spider.authorization.SpiderAuthorizator;
import se.uu.ub.cora.spider.data.SpiderDataAtomic;
import se.uu.ub.cora.spider.data.SpiderDataGroup;
import se.uu.ub.cora.spider.dependency.SpiderDependencyProviderSpy;
import se.uu.ub.cora.spider.extended.ExtendedFunctionalityProviderSpy;
import se.uu.ub.cora.spider.record.storage.RecordStorage;
import se.uu.ub.cora.spider.search.RecordIndexer;
import se.uu.ub.cora.spider.spy.AuthorizatorAlwaysAuthorizedSpy;
import se.uu.ub.cora.spider.spy.DataGroupTermCollectorSpy;
import se.uu.ub.cora.spider.spy.DataRecordLinkCollectorSpy;
import se.uu.ub.cora.spider.spy.DataValidatorAlwaysInvalidSpy;
import se.uu.ub.cora.spider.spy.DataValidatorAlwaysValidSpy;
import se.uu.ub.cora.spider.spy.NoRulesCalculatorStub;
import se.uu.ub.cora.spider.spy.RecordIndexerSpy;
import se.uu.ub.cora.spider.spy.RecordStorageForValidateDataSpy;
import se.uu.ub.cora.spider.spy.RecordStorageSpy;
import se.uu.ub.cora.spider.testdata.DataCreator;

public class SpiderRecordValidatorTest {
	private RecordStorage recordStorage;
	private Authenticator authenticator;
	private SpiderAuthorizator spiderAuthorizator;
	private PermissionRuleCalculator ruleCalculator;
	private SpiderRecordValidatorImp recordValidator;
	private DataValidator dataValidator;
	private DataRecordLinkCollector linkCollector;
	private SpiderDependencyProviderSpy dependencyProvider;
	private ExtendedFunctionalityProviderSpy extendedFunctionalityProvider;
	private DataGroupToRecordEnhancerSpy dataGroupToRecordEnhancer;
	private DataGroupTermCollector termCollector;
	private RecordIndexer recordIndexer;

	@BeforeMethod
	public void beforeMethod() {
		authenticator = new AuthenticatorSpy();
		spiderAuthorizator = new AuthorizatorAlwaysAuthorizedSpy();
		dataValidator = new DataValidatorAlwaysValidSpy();
		recordStorage = new RecordStorageSpy();
		// recordStorage =
		// TestDataRecordInMemoryStorage.createRecordStorageInMemoryWithTestData();
		ruleCalculator = new NoRulesCalculatorStub();
		linkCollector = new DataRecordLinkCollectorSpy();
		termCollector = new DataGroupTermCollectorSpy();
		recordIndexer = new RecordIndexerSpy();
		extendedFunctionalityProvider = new ExtendedFunctionalityProviderSpy();
		setUpDependencyProvider();
	}

	private void setUpDependencyProvider() {
		dependencyProvider = new SpiderDependencyProviderSpy(new HashMap<>());
		dependencyProvider.authenticator = authenticator;
		dependencyProvider.spiderAuthorizator = spiderAuthorizator;
		dependencyProvider.dataValidator = dataValidator;
		dependencyProvider.recordStorage = recordStorage;
		dependencyProvider.ruleCalculator = ruleCalculator;
		dependencyProvider.linkCollector = linkCollector;
		dependencyProvider.extendedFunctionalityProvider = extendedFunctionalityProvider;
		dependencyProvider.searchTermCollector = termCollector;
		dependencyProvider.recordIndexer = recordIndexer;
		dataGroupToRecordEnhancer = new DataGroupToRecordEnhancerSpy();
		recordValidator = SpiderRecordValidatorImp
				.usingDependencyProviderAndDataGroupToRecordEnhancer(dependencyProvider,
						dataGroupToRecordEnhancer);
	}

	@Test
	public void testValidateRecordInvalidData() {
		dataValidator = new DataValidatorAlwaysInvalidSpy();
		recordStorage = new RecordStorageForValidateDataSpy();
		setUpDependencyProvider();

		SpiderDataGroup dataGroup = SpiderDataGroup.withNameInData("typeWithUserGeneratedId");
		SpiderDataGroup createRecordInfo = SpiderDataGroup.withNameInData("recordInfo");
		createRecordInfo.addChild(SpiderDataAtomic.withNameInDataAndValue("id", "place"));
		createRecordInfo.addChild(SpiderDataAtomic.withNameInDataAndValue("type", "recordType"));
		dataGroup.addChild(createRecordInfo);
		dataGroup.addChild(SpiderDataAtomic.withNameInDataAndValue("atomicId", "atomicValue"));

		assertFalse(recordValidator.validateRecord("someToken78678567", "place", "place:001",
				dataGroup));
		assertTrue(((DataValidatorAlwaysInvalidSpy) dataValidator).validateDataWasCalled);
	}

	@Test
	public void testValidatenRecordDataValidDataForUpdate() {
		recordStorage = new RecordStorageForValidateDataSpy();
		setUpDependencyProvider();

		SpiderDataGroup dataGroup = SpiderDataGroup.withNameInData("typeWithUserGeneratedId");
		SpiderDataGroup createRecordInfo = SpiderDataGroup.withNameInData("recordInfo");
		createRecordInfo.addChild(SpiderDataAtomic.withNameInDataAndValue("id", "place"));
		createRecordInfo.addChild(SpiderDataAtomic.withNameInDataAndValue("type", "recordType"));
		dataGroup.addChild(createRecordInfo);
		dataGroup.addChild(SpiderDataAtomic.withNameInDataAndValue("atomicId", "atomicValue"));

		assertTrue(
				recordValidator.validateRecord("someToken78678567", "place", "update", dataGroup));

		DataValidatorAlwaysValidSpy dataValidatorSpy = (DataValidatorAlwaysValidSpy) dataValidator;
		assertTrue(dataValidatorSpy.validateDataWasCalled);
		assertEquals(dataValidatorSpy.metadataId, "place");
	}

	@Test
	public void testValidatenRecordDataValidDataForCreate() {
		recordStorage = new RecordStorageForValidateDataSpy();
		setUpDependencyProvider();

		SpiderDataGroup dataGroup = SpiderDataGroup.withNameInData("typeWithUserGeneratedId");
		SpiderDataGroup createRecordInfo = SpiderDataGroup.withNameInData("recordInfo");
		createRecordInfo.addChild(SpiderDataAtomic.withNameInDataAndValue("id", "place"));
		createRecordInfo.addChild(SpiderDataAtomic.withNameInDataAndValue("type", "recordType"));
		dataGroup.addChild(createRecordInfo);
		dataGroup.addChild(SpiderDataAtomic.withNameInDataAndValue("atomicId", "atomicValue"));

		assertTrue(
				recordValidator.validateRecord("someToken78678567", "place", "create", dataGroup));

		DataValidatorAlwaysValidSpy dataValidatorSpy = (DataValidatorAlwaysValidSpy) dataValidator;
		assertTrue(dataValidatorSpy.validateDataWasCalled);
		assertEquals(dataValidatorSpy.metadataId, "placeNew");
	}

	// @Test
	// public void testExternalDependenciesAreCalled() {
	// spiderAuthorizator = new AuthorizatorAlwaysAuthorizedSpy();
	// recordStorage = new RecordStorageSpy();
	// ruleCalculator = new RuleCalculatorSpy();
	// setUpDependencyProvider();
	//
	// SpiderDataGroup spiderDataGroup =
	// SpiderDataGroup.withNameInData("nameInData");
	// spiderDataGroup.addChild(
	// SpiderDataCreator.createRecordInfoWithRecordTypeAndRecordIdAndDataDivider("spyType",
	// "spyId", "cora"));
	// recordUpdater.updateRecord("someToken78678567", "spyType", "spyId",
	// spiderDataGroup);
	//
	// AuthorizatorAlwaysAuthorizedSpy authorizatorSpy =
	// (AuthorizatorAlwaysAuthorizedSpy) spiderAuthorizator;
	// assertTrue(authorizatorSpy.authorizedWasCalled);
	//
	// assertTrue(((DataValidatorAlwaysValidSpy)
	// dataValidator).validateDataWasCalled);
	// assertTrue(((RecordStorageSpy) recordStorage).updateWasCalled);
	// assertTrue(((DataRecordLinkCollectorSpy)
	// linkCollector).collectLinksWasCalled);
	// assertEquals(((DataRecordLinkCollectorSpy) linkCollector).metadataId,
	// "spyType");
	//
	// assertCorrectSearchTermCollectorAndIndexer();
	// }
	//
	// private void assertCorrectSearchTermCollectorAndIndexer() {
	// DataGroupTermCollectorSpy searchTermCollectorSpy =
	// (DataGroupTermCollectorSpy) termCollector;
	// assertEquals(searchTermCollectorSpy.metadataId, "spyType");
	// assertTrue(searchTermCollectorSpy.collectTermsWasCalled);
	// assertEquals(((RecordIndexerSpy) recordIndexer).recordIndexData,
	// searchTermCollectorSpy.collectedTerms);
	// }
	//
	// @Test
	// public void testRecordEnhancerCalled() {
	// spiderAuthorizator = new AuthorizatorAlwaysAuthorizedSpy();
	// recordStorage = new RecordStorageSpy();
	// ruleCalculator = new RuleCalculatorSpy();
	// setUpDependencyProvider();
	// SpiderDataGroup spiderDataGroup =
	// SpiderDataGroup.withNameInData("nameInData");
	// spiderDataGroup.addChild(
	// SpiderDataCreator.createRecordInfoWithRecordTypeAndRecordIdAndDataDivider("spyType",
	// "spyId", "cora"));
	// recordUpdater.updateRecord("someToken78678567", "spyType", "spyId",
	// spiderDataGroup);
	// assertEquals(dataGroupToRecordEnhancer.user.id, "12345");
	// assertEquals(dataGroupToRecordEnhancer.recordType, "spyType");
	// assertEquals(dataGroupToRecordEnhancer.dataGroup.getFirstGroupWithNameInData("recordInfo")
	// .getFirstAtomicValueWithNameInData("id"), "spyId");
	// }

	// @Test(expectedExceptions = AuthenticationException.class)
	// public void testAuthenticationNotAuthenticated() {
	// recordStorage = new RecordStorageSpy();
	// setUpDependencyProvider();
	// SpiderDataGroup spiderDataGroup = DataCreator
	// .createRecordWithNameInDataAndIdAndTypeAndLinkedRecordId("nameInData",
	// "spyId",
	// "spyType", "cora");
	// recordUpdater.updateRecord("dummyNonAuthenticatedToken", "spyType", "spyId",
	// spiderDataGroup);
	// }

	private SpiderDataGroup getSpiderDataGroupToUpdate() {
		SpiderDataGroup dataGroup = SpiderDataGroup.withNameInData("typeWithUserGeneratedId");
		SpiderDataGroup createRecordInfo = DataCreator
				.createRecordInfoWithIdAndLinkedRecordId("somePlace", "cora");
		SpiderDataGroup typeGroup = SpiderDataGroup.withNameInData("type");
		typeGroup.addChild(
				SpiderDataAtomic.withNameInDataAndValue("linkedRecordType", "recordType"));
		typeGroup.addChild(SpiderDataAtomic.withNameInDataAndValue("linkedRecordId",
				"typeWithAutoGeneratedId"));
		createRecordInfo.addChild(typeGroup);
		dataGroup.addChild(createRecordInfo);
		dataGroup.addChild(SpiderDataAtomic.withNameInDataAndValue("atomicId", "atomicValue"));
		return dataGroup;
	}

	// @Test(expectedExceptions = DataMissingException.class)
	// public void testUpdateRecordRecordInfoMissing() {
	// recordStorage = new RecordStorageCreateUpdateSpy();
	// setUpDependencyProvider();
	//
	// SpiderDataGroup group = SpiderDataGroup.withNameInData("authority");
	// recordUpdater.updateRecord("someToken78678567", "typeWithAutoGeneratedId",
	// "place", group);
	// }
	//
	// @Test(expectedExceptions = DataMissingException.class)
	// public void testUpdateRecordRecordInfoContentMissing() {
	// recordStorage = new RecordStorageCreateUpdateSpy();
	// setUpDependencyProvider();
	//
	// SpiderDataGroup group = SpiderDataGroup.withNameInData("authority");
	// SpiderDataGroup createRecordInfo =
	// SpiderDataGroup.withNameInData("recordInfo");
	// group.addChild(createRecordInfo);
	// recordUpdater.updateRecord("someToken78678567", "typeWithAutoGeneratedId",
	// "place", group);
	// }

	// @Test(expectedExceptions = RecordNotFoundException.class)
	// public void testNonExistingRecordType() {
	// SpiderDataGroup record = SpiderDataGroup.withNameInData("authority");
	// recordUpdater.updateRecord("someToken78678567", "recordType_NOT_EXISTING",
	// "id", record);
	// }

	// @Test(expectedExceptions = DataException.class)
	// public void testUpdateRecordIncomingDataTypesDoNotMatch() {
	// recordStorage = new RecordStorageCreateUpdateSpy();
	// setUpDependencyProvider();
	//
	// SpiderDataGroup dataGroup = SpiderDataGroup
	// .withNameInData("typeWithUserGeneratedId_NOT_THE_SAME");
	// SpiderDataGroup createRecordInfo =
	// SpiderDataGroup.withNameInData("recordInfo");
	// createRecordInfo.addChild(SpiderDataAtomic.withNameInDataAndValue("id",
	// "place"));
	// SpiderDataGroup typeGroup = SpiderDataGroup.withNameInData("type");
	// typeGroup.addChild(
	// SpiderDataAtomic.withNameInDataAndValue("linkedRecordType", "recordType"));
	// typeGroup.addChild(SpiderDataAtomic.withNameInDataAndValue("linkedRecordId",
	// "recordType"));
	// createRecordInfo.addChild(typeGroup);
	// dataGroup.addChild(createRecordInfo);
	// dataGroup.addChild(SpiderDataAtomic.withNameInDataAndValue("atomicId",
	// "atomicValue"));
	//
	// recordUpdater.updateRecord("someToken78678567", "typeWithAutoGeneratedId",
	// "place",
	// dataGroup);
	// }

	// @Test(expectedExceptions = DataException.class)
	// public void testUpdateRecordIncomingDataIdDoNotMatch() {
	// recordStorage = new RecordStorageCreateUpdateSpy();
	// setUpDependencyProvider();
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
	// recordUpdater.updateRecord("someToken78678567", "recordType", "placeNOT",
	// dataGroup);
	// }

	// @Test(expectedExceptions = DataException.class)
	// public void testLinkedRecordIdDoesNotExist() {
	// recordStorage = new RecordLinkTestsRecordStorage();
	// linkCollector =
	// DataCreator.getDataRecordLinkCollectorSpyWithCollectedLinkAdded();
	// setUpDependencyProvider();
	//
	// ((RecordLinkTestsRecordStorage) recordStorage).recordIdExistsForRecordType =
	// false;
	//
	// SpiderDataGroup dataGroup = RecordLinkTestsDataCreator
	// .createDataGroupWithRecordInfoAndLinkOneLevelDown();
	// recordUpdater.updateRecord("someToken78678567", "dataWithLinks",
	// "oneLinkOneLevelDown",
	// dataGroup);
	// }
}
