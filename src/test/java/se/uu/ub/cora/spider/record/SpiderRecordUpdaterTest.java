/*
 * Copyright 2015, 2016, 2018 Uppsala University Library
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

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;

import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import se.uu.ub.cora.bookkeeper.data.DataGroup;
import se.uu.ub.cora.bookkeeper.linkcollector.DataRecordLinkCollector;
import se.uu.ub.cora.bookkeeper.termcollector.DataGroupTermCollector;
import se.uu.ub.cora.bookkeeper.validator.DataValidator;
import se.uu.ub.cora.spider.authentication.AuthenticationException;
import se.uu.ub.cora.spider.authentication.Authenticator;
import se.uu.ub.cora.spider.authentication.AuthenticatorSpy;
import se.uu.ub.cora.spider.authorization.AlwaysAuthorisedExceptStub;
import se.uu.ub.cora.spider.authorization.AuthorizationException;
import se.uu.ub.cora.spider.authorization.PermissionRuleCalculator;
import se.uu.ub.cora.spider.authorization.SpiderAuthorizator;
import se.uu.ub.cora.spider.data.DataMissingException;
import se.uu.ub.cora.spider.data.SpiderDataAtomic;
import se.uu.ub.cora.spider.data.SpiderDataGroup;
import se.uu.ub.cora.spider.data.SpiderDataRecord;
import se.uu.ub.cora.spider.dependency.RecordStorageProviderSpy;
import se.uu.ub.cora.spider.dependency.SpiderDependencyProviderSpy;
import se.uu.ub.cora.spider.extended.ExtendedFunctionalityProviderSpy;
import se.uu.ub.cora.spider.extended.ExtendedFunctionalitySpy;
import se.uu.ub.cora.spider.record.storage.RecordNotFoundException;
import se.uu.ub.cora.spider.search.RecordIndexer;
import se.uu.ub.cora.spider.spy.AuthorizatorAlwaysAuthorizedSpy;
import se.uu.ub.cora.spider.spy.DataGroupTermCollectorSpy;
import se.uu.ub.cora.spider.spy.DataRecordLinkCollectorSpy;
import se.uu.ub.cora.spider.spy.DataValidatorAlwaysInvalidSpy;
import se.uu.ub.cora.spider.spy.DataValidatorAlwaysValidSpy;
import se.uu.ub.cora.spider.spy.DataValidatorForRecordInfoSpy;
import se.uu.ub.cora.spider.spy.NoRulesCalculatorStub;
import se.uu.ub.cora.spider.spy.RecordIndexerSpy;
import se.uu.ub.cora.spider.spy.RecordStorageCreateUpdateSpy;
import se.uu.ub.cora.spider.spy.RecordStorageSpy;
import se.uu.ub.cora.spider.spy.RecordStorageUpdateMultipleTimesSpy;
import se.uu.ub.cora.spider.spy.RuleCalculatorSpy;
import se.uu.ub.cora.spider.testdata.DataCreator;
import se.uu.ub.cora.spider.testdata.RecordLinkTestsDataCreator;
import se.uu.ub.cora.spider.testdata.SpiderDataCreator;
import se.uu.ub.cora.spider.testdata.TestDataRecordInMemoryStorage;
import se.uu.ub.cora.storage.RecordStorage;

public class SpiderRecordUpdaterTest {
	private RecordStorage recordStorage;
	private Authenticator authenticator;
	private SpiderAuthorizator spiderAuthorizator;
	private PermissionRuleCalculator ruleCalculator;
	private SpiderRecordUpdater recordUpdater;
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
		recordStorage = TestDataRecordInMemoryStorage.createRecordStorageInMemoryWithTestData();
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
		RecordStorageProviderSpy recordStorageProviderSpy = new RecordStorageProviderSpy();
		recordStorageProviderSpy.recordStorage = recordStorage;
		dependencyProvider.setRecordStorageProvider(recordStorageProviderSpy);
		dependencyProvider.ruleCalculator = ruleCalculator;
		dependencyProvider.linkCollector = linkCollector;
		dependencyProvider.extendedFunctionalityProvider = extendedFunctionalityProvider;
		dependencyProvider.searchTermCollector = termCollector;
		dependencyProvider.recordIndexer = recordIndexer;
		dataGroupToRecordEnhancer = new DataGroupToRecordEnhancerSpy();
		recordUpdater = SpiderRecordUpdaterImp.usingDependencyProviderAndDataGroupToRecordEnhancer(
				dependencyProvider, dataGroupToRecordEnhancer);
	}

	@Test
	public void testExternalDependenciesAreCalled() {
		spiderAuthorizator = new AuthorizatorAlwaysAuthorizedSpy();
		recordStorage = new RecordStorageSpy();
		ruleCalculator = new RuleCalculatorSpy();
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

		assertCorrectSearchTermCollectorAndIndexer();
	}

	private void assertCorrectSearchTermCollectorAndIndexer() {
		DataGroupTermCollectorSpy searchTermCollectorSpy = (DataGroupTermCollectorSpy) termCollector;
		assertEquals(searchTermCollectorSpy.metadataId, "spyType");
		assertTrue(searchTermCollectorSpy.collectTermsWasCalled);
		assertEquals(((RecordIndexerSpy) recordIndexer).recordIndexData,
				searchTermCollectorSpy.collectedTerms);
	}

	@Test
	public void testRecordEnhancerCalled() {
		spiderAuthorizator = new AuthorizatorAlwaysAuthorizedSpy();
		recordStorage = new RecordStorageSpy();
		ruleCalculator = new RuleCalculatorSpy();
		setUpDependencyProvider();
		SpiderDataGroup spiderDataGroup = SpiderDataGroup.withNameInData("nameInData");
		spiderDataGroup.addChild(
				SpiderDataCreator.createRecordInfoWithRecordTypeAndRecordIdAndDataDivider("spyType",
						"spyId", "cora"));
		recordUpdater.updateRecord("someToken78678567", "spyType", "spyId", spiderDataGroup);
		assertEquals(dataGroupToRecordEnhancer.user.id, "12345");
		assertEquals(dataGroupToRecordEnhancer.recordType, "spyType");
		assertEquals(dataGroupToRecordEnhancer.dataGroup.getFirstGroupWithNameInData("recordInfo")
				.getFirstAtomicValueWithNameInData("id"), "spyId");
	}

	@Test
	public void testCorrectRecordInfoInUpdatedRecord() {
		recordStorage = new RecordStorageSpy();
		dataValidator = new DataValidatorForRecordInfoSpy();
		ruleCalculator = new RuleCalculatorSpy();
		setUpDependencyProvider();
		SpiderDataGroup spiderDataGroup = SpiderDataGroup.withNameInData("someRecordDataGroup");
		addRecordInfo(spiderDataGroup);

		SpiderDataRecord updatedOnce = recordUpdater.updateRecord("someToken78678567", "spyType",
				"spyId", spiderDataGroup);

		assertUpdatedRepeatIdsInGroupAsListed(updatedOnce, "0");
		assertCorrectUserInfo(updatedOnce);
	}

	private void assertUpdatedRepeatIdsInGroupAsListed(SpiderDataRecord updatedRecord,
			String... expectedRepeatIds) {
		SpiderDataGroup updatedDataGroup = updatedRecord.getSpiderDataGroup();
		SpiderDataGroup updatedRecordInfo = updatedDataGroup.extractGroup("recordInfo");

		List<SpiderDataGroup> updatedGroups = updatedRecordInfo
				.getAllGroupsWithNameInData("updated");
		assertEquals(updatedGroups.size(), expectedRepeatIds.length);

		for (int i = 0; i < expectedRepeatIds.length; i++) {
			SpiderDataGroup updatedGroup = updatedGroups.get(i);
			String repeatId = updatedGroup.getRepeatId();
			assertEquals(repeatId, expectedRepeatIds[i]);
		}
	}

	private void assertCorrectUserInfo(SpiderDataRecord updatedOnce) {
		SpiderDataGroup updatedOnceDataGroup = updatedOnce.getSpiderDataGroup();
		SpiderDataGroup updatedOnceRecordInfo = updatedOnceDataGroup.extractGroup("recordInfo");
		assertCorrectDataUsingGroupNameInDataAndLinkedRecordId(updatedOnceRecordInfo, "createdBy",
				"6789");
		String tsCreated = updatedOnceRecordInfo.extractAtomicValue("tsCreated");
		assertTrue(tsCreated.matches("\\d{4}-\\d{2}-\\d{2}\\s\\d{2}:\\d{2}:\\d{2}\\.\\d{3}"));

		SpiderDataGroup updated = updatedOnceRecordInfo.extractGroup("updated");

		assertCorrectDataUsingGroupNameInDataAndLinkedRecordId(updated, "updatedBy", "12345");
		String tsUpdated = updated.extractAtomicValue("tsUpdated");
		assertTrue(tsUpdated.matches("\\d{4}-\\d{2}-\\d{2}\\s\\d{2}:\\d{2}:\\d{2}\\.\\d{3}"));
		assertFalse(tsUpdated.equals(tsCreated));
	}

	private void assertCorrectDataUsingGroupNameInDataAndLinkedRecordId(
			SpiderDataGroup updatedRecordInfo, String groupNameInData, String linkedRecordId) {
		SpiderDataGroup createdByGroup = updatedRecordInfo.extractGroup(groupNameInData);
		assertEquals(createdByGroup.extractAtomicValue("linkedRecordType"), "user");
		assertEquals(createdByGroup.extractAtomicValue("linkedRecordId"), linkedRecordId);
	}

	private void addRecordInfo(SpiderDataGroup spiderDataGroup) {
		SpiderDataGroup recordInfo = SpiderDataCreator
				.createRecordInfoWithRecordTypeAndRecordIdAndDataDivider("spyType", "spyId",
						"cora");
		SpiderDataGroup createdBy = createLinkWithNameInDataRecordTypeAndRecordId("createdBy",
				"user", "6789");
		recordInfo.addChild(createdBy);

		LocalDateTime tsCreated = LocalDateTime.of(2016, 10, 01, 00, 00, 00, 000);
		DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS");
		recordInfo.addChild(
				SpiderDataAtomic.withNameInDataAndValue("tsCreated", tsCreated.format(formatter)));
		spiderDataGroup.addChild(recordInfo);
	}

	private SpiderDataGroup createLinkWithNameInDataRecordTypeAndRecordId(String nameInData,
			String linkedRecordType, String linkedRecordId) {
		SpiderDataGroup recordLink = SpiderDataGroup.withNameInData(nameInData);
		recordLink.addChild(
				SpiderDataAtomic.withNameInDataAndValue("linkedRecordType", linkedRecordType));
		recordLink.addChild(
				SpiderDataAtomic.withNameInDataAndValue("linkedRecordId", linkedRecordId));
		return recordLink;
	}

	@Test
	public void testCorrectUpdateInfoWhenRecordHasAlreadyBeenUpdated() {
		recordStorage = new RecordStorageUpdateMultipleTimesSpy();
		ruleCalculator = new RuleCalculatorSpy();
		setUpDependencyProvider();
		SpiderDataGroup spiderDataGroup = SpiderDataGroup.withNameInData("nameInData");
		addRecordInfo(spiderDataGroup);

		SpiderDataRecord updatedOnce = recordUpdater.updateRecord("someToken78678567", "spyType",
				"spyId", spiderDataGroup);
		setUpdatedRecordInStorageSpyToReturnOnRead(updatedOnce);

		SpiderDataRecord updatedTwice = recordUpdater.updateRecord("someToken78678567", "spyType",
				"spyId", updatedOnce.getSpiderDataGroup());
		assertUpdatedRepeatIdsInGroupAsListed(updatedTwice, "0", "1");

		setUpdatedRecordInStorageSpyToReturnOnRead(updatedTwice);
		SpiderDataRecord updatedThree = recordUpdater.updateRecord("someToken78678567", "spyType",
				"spyId", updatedOnce.getSpiderDataGroup());
		assertUpdatedRepeatIdsInGroupAsListed(updatedThree, "0", "1", "2");
	}

	@Test
	public void testNewUpdatedInRecordInfoHasRepeatIdPlusOne() {
		recordStorage = new RecordStorageUpdateMultipleTimesSpy();
		ruleCalculator = new RuleCalculatorSpy();
		setUpDependencyProvider();
		SpiderDataGroup spiderDataGroup = SpiderDataGroup.withNameInData("nameInData");
		addRecordInfo(spiderDataGroup);

		SpiderDataRecord updatedOnceRecord = recordUpdater.updateRecord("someToken78678567",
				"spyType", "spyId", spiderDataGroup);
		assertCorrectUserInfo(updatedOnceRecord);

		SpiderDataGroup updatedOnce = updatedOnceRecord.getSpiderDataGroup();
		SpiderDataGroup recordInfo = updatedOnce.extractGroup("recordInfo");

		SpiderDataGroup secondUpdated = SpiderDataGroup.withNameInData("updated");
		secondUpdated.setRepeatId("334");
		recordInfo.addChild(secondUpdated);

		setUpdatedRecordInStorageSpyToReturnOnRead(updatedOnceRecord);

		SpiderDataRecord updatedTwice = recordUpdater.updateRecord("someToken78678567", "spyType",
				"spyId", spiderDataGroup);
		assertUpdatedRepeatIdsInGroupAsListed(updatedTwice, "0", "334", "335");
	}

	private void setUpdatedRecordInStorageSpyToReturnOnRead(SpiderDataRecord updatedRecord) {
		RecordStorageUpdateMultipleTimesSpy storageSpy = (RecordStorageUpdateMultipleTimesSpy) recordStorage;
		storageSpy.recordToReturnOnRead = updatedRecord.getSpiderDataGroup().toDataGroup();
	}

	@Test
	public void testUpdateInfoSetFromPreviousUpdateIsNotReplacedByAlteredData() {
		recordStorage = new RecordStorageUpdateMultipleTimesSpy();
		ruleCalculator = new RuleCalculatorSpy();
		setUpDependencyProvider();
		SpiderDataGroup spiderDataGroup = SpiderDataGroup.withNameInData("nameInData");
		addRecordInfo(spiderDataGroup);

		SpiderDataRecord updatedRecord = recordUpdater.updateRecord("someToken78678567", "spyType",
				"spyId", spiderDataGroup);
		setUpdatedRecordInStorageSpyToReturnOnRead(updatedRecord);

		SpiderDataGroup firstUpdated = getFirstUpdatedInfo(updatedRecord);
		String correctTsUpdated = firstUpdated.extractAtomicValue("tsUpdated");

		changeUpdatedValue(firstUpdated);

		SpiderDataRecord recordAlreadyContainingUpdateInfo = recordUpdater.updateRecord(
				"someToken78678567", "spyType", "spyId", updatedRecord.getSpiderDataGroup());

		SpiderDataGroup updatedRecordInfo = getRecordInfo(recordAlreadyContainingUpdateInfo);
		List<SpiderDataGroup> updatedGroups = updatedRecordInfo
				.getAllGroupsWithNameInData("updated");
		assertEquals(updatedGroups.size(), 2);

		SpiderDataGroup firstUpdated2 = updatedRecordInfo.extractGroup("updated");
		String tsUpdated = firstUpdated2.extractAtomicValue("tsUpdated");

		assertEquals(tsUpdated, correctTsUpdated);
	}

	private SpiderDataGroup getRecordInfo(SpiderDataRecord record) {
		SpiderDataGroup updatedSpiderDataGroup = record.getSpiderDataGroup();
		return updatedSpiderDataGroup.extractGroup("recordInfo");
	}

	private SpiderDataGroup getFirstUpdatedInfo(SpiderDataRecord updatedRecord) {
		SpiderDataGroup updatedRecordInfo = updatedRecord.getSpiderDataGroup()
				.extractGroup("recordInfo");
		SpiderDataGroup firstUpdated = updatedRecordInfo.extractGroup("updated");
		return firstUpdated;
	}

	private void changeUpdatedValue(SpiderDataGroup firstUpdated) {
		firstUpdated.removeChild("tsUpdated");
		firstUpdated
				.addChild(SpiderDataAtomic.withNameInDataAndValue("tsUpdated", "someAlteredValue"));
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
		DataGroupTermCollectorSpy dataGroupTermCollectorSpy = (DataGroupTermCollectorSpy) termCollector;
		DataGroup returnedCollectedTerms = dataGroupTermCollectorSpy.collectedTerms;
		assertEquals(authorizatorSpy.calledMethods.get(0),
				"checkUserIsAuthorizedForActionOnRecordTypeAndCollectedData");
		assertEquals(authorizatorSpy.collectedTerms.get(0), returnedCollectedTerms);

		assertEquals(authorizatorSpy.actions.get(1), "update");
		assertEquals(authorizatorSpy.users.get(1).id, "12345");
		assertEquals(authorizatorSpy.recordTypes.get(1), "typeWithAutoGeneratedId");
		assertEquals(authorizatorSpy.calledMethods.get(1),
				"checkUserIsAuthorizedForActionOnRecordTypeAndCollectedData");
		assertEquals(authorizatorSpy.collectedTerms.get(1), returnedCollectedTerms);

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
	public void testUnauthorizedForUpdateOnRecordTypeShouldNotAccessStorage() {
		recordStorage = new RecordStorageSpy();
		spiderAuthorizator = new AlwaysAuthorisedExceptStub();
		HashSet<String> hashSet = new HashSet<String>();
		hashSet.add("update");
		((AlwaysAuthorisedExceptStub) spiderAuthorizator).notAuthorizedForRecordTypeAndActions
				.put("spyType", hashSet);
		setUpDependencyProvider();

		SpiderDataGroup spiderDataGroup = SpiderDataGroup.withNameInData("nameInData");
		spiderDataGroup.addChild(
				SpiderDataCreator.createRecordInfoWithRecordTypeAndRecordIdAndDataDivider("spyType",
						"spyId", "cora"));

		boolean exceptionWasCaught = false;
		try {
			recordUpdater.updateRecord("someToken78678567", "spyType", "spyId", spiderDataGroup);
		} catch (Exception e) {
			assertEquals(e.getClass(), AuthorizationException.class);
			exceptionWasCaught = true;
		}
		assertTrue(exceptionWasCaught);
		assertFalse(((RecordStorageSpy) recordStorage).readWasCalled);
		assertFalse(((RecordStorageSpy) recordStorage).updateWasCalled);
		assertFalse(((RecordStorageSpy) recordStorage).deleteWasCalled);
		assertFalse(((RecordStorageSpy) recordStorage).createWasCalled);
	}

	@Test
	public void testExtendedFunctionallityIsCalled() {
		spiderAuthorizator = new AuthorizatorAlwaysAuthorizedSpy();
		recordStorage = new RecordStorageSpy();
		ruleCalculator = new RuleCalculatorSpy();
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
	public void testIndexerIsCalled() {
		recordStorage = new RecordStorageCreateUpdateSpy();
		setUpDependencyProvider();

		SpiderDataGroup dataGroup = getSpiderDataGroupToUpdate();

		recordUpdater.updateRecord("someToken78678567", "typeWithAutoGeneratedId", "somePlace",
				dataGroup);

		DataGroup updatedRecord = ((RecordStorageCreateUpdateSpy) recordStorage).updateRecord;
		RecordIndexerSpy recordIndexerSpy = (RecordIndexerSpy) recordIndexer;
		DataGroup indexedRecord = recordIndexerSpy.record;
		assertEquals(indexedRecord, updatedRecord);
		List<String> ids = recordIndexerSpy.ids;
		assertEquals(ids.get(0), "typeWithAutoGeneratedId_somePlace");
		assertEquals(ids.size(), 1);
	}

	@Test
	public void testIndexerIsCalledForChildOfAbstract() {
		recordStorage = new RecordStorageCreateUpdateSpy();
		setUpDependencyProvider();

		SpiderDataGroup dataGroup = getSpiderDataGroupForImageToUpdate();

		recordUpdater.updateRecord("someToken78678567", "image", "someImage", dataGroup);

		DataGroup updatedRecord = ((RecordStorageCreateUpdateSpy) recordStorage).updateRecord;
		RecordIndexerSpy recordIndexerSpy = (RecordIndexerSpy) recordIndexer;
		DataGroup indexedRecord = recordIndexerSpy.record;
		assertEquals(indexedRecord, updatedRecord);
		List<String> ids = recordIndexerSpy.ids;
		assertEquals(ids.get(0), "image_someImage");
		assertEquals(ids.get(1), "binary_someImage");
		assertEquals(ids.size(), 2);
	}

	private SpiderDataGroup getSpiderDataGroupForImageToUpdate() {
		SpiderDataGroup dataGroup = SpiderDataGroup.withNameInData("binary");
		SpiderDataGroup createRecordInfo = DataCreator
				.createRecordInfoWithIdAndLinkedRecordId("someImage", "cora");
		SpiderDataGroup typeGroup = SpiderDataGroup.withNameInData("type");
		typeGroup.addChild(
				SpiderDataAtomic.withNameInDataAndValue("linkedRecordType", "recordType"));
		typeGroup.addChild(SpiderDataAtomic.withNameInDataAndValue("linkedRecordId", "image"));
		createRecordInfo.addChild(typeGroup);
		dataGroup.addChild(createRecordInfo);
		dataGroup.addChild(SpiderDataAtomic.withNameInDataAndValue("atomicId", "atomicValue"));
		return dataGroup;
	}

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

	@Test
	public void testUpdateRecordDataDividerExtractedFromData() {
		recordStorage = new RecordStorageCreateUpdateSpy();
		setUpDependencyProvider();

		SpiderDataGroup dataGroup = getSpiderDataGroupToUpdate();

		recordUpdater.updateRecord("someToken78678567", "typeWithAutoGeneratedId", "somePlace",
				dataGroup);

		assertEquals(((RecordStorageCreateUpdateSpy) recordStorage).dataDivider, "cora");
	}

	@Test
	public void testCreateRecordCollectedTermsSentAlong() {
		recordStorage = new RecordStorageCreateUpdateSpy();
		setUpDependencyProvider();

		SpiderDataGroup dataGroup = getSpiderDataGroupToUpdate();

		recordUpdater.updateRecord("someToken78678567", "typeWithAutoGeneratedId", "somePlace",
				dataGroup);

		DataGroupTermCollectorSpy termCollectorSpy = (DataGroupTermCollectorSpy) termCollector;
		assertEquals(termCollectorSpy.metadataId, "placeNew");
		assertTrue(termCollectorSpy.collectTermsWasCalled);
		assertEquals(((RecordStorageCreateUpdateSpy) recordStorage).collectedTerms,
				termCollectorSpy.collectedTerms);
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
		SpiderDataGroup typeGroup = SpiderDataGroup.withNameInData("type");
		typeGroup.addChild(
				SpiderDataAtomic.withNameInDataAndValue("linkedRecordType", "recordType"));
		typeGroup.addChild(SpiderDataAtomic.withNameInDataAndValue("linkedRecordId", "recordType"));
		createRecordInfo.addChild(typeGroup);
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
