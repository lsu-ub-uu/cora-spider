/*
 * Copyright 2015, 2016, 2018, 2020, 2021, 2022 Uppsala University Library
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

package se.uu.ub.cora.spider.record.internal;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertTrue;

import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Set;
import java.util.function.Supplier;

import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import se.uu.ub.cora.beefeater.authentication.User;
import se.uu.ub.cora.bookkeeper.linkcollector.DataRecordLinkCollector;
import se.uu.ub.cora.data.DataAtomic;
import se.uu.ub.cora.data.DataAtomicFactory;
import se.uu.ub.cora.data.DataAtomicProvider;
import se.uu.ub.cora.data.DataChild;
import se.uu.ub.cora.data.DataGroup;
import se.uu.ub.cora.data.DataGroupFactory;
import se.uu.ub.cora.data.DataGroupProvider;
import se.uu.ub.cora.data.DataProvider;
import se.uu.ub.cora.data.DataRecord;
import se.uu.ub.cora.data.DataRecordLink;
import se.uu.ub.cora.data.collectterms.CollectTerms;
import se.uu.ub.cora.data.copier.DataCopierFactory;
import se.uu.ub.cora.data.copier.DataCopierProvider;
import se.uu.ub.cora.logger.LoggerProvider;
import se.uu.ub.cora.spider.authentication.AuthenticationException;
import se.uu.ub.cora.spider.authentication.AuthenticatorSpy;
import se.uu.ub.cora.spider.authorization.AuthorizationException;
import se.uu.ub.cora.spider.authorization.PermissionRuleCalculator;
import se.uu.ub.cora.spider.data.DataAtomicFactorySpy;
import se.uu.ub.cora.spider.data.DataAtomicSpy;
import se.uu.ub.cora.spider.data.DataGroupFactorySpy;
import se.uu.ub.cora.spider.data.DataGroupOldSpy;
import se.uu.ub.cora.spider.data.DataMissingException;
import se.uu.ub.cora.spider.dependency.spy.RecordTypeHandlerSpy;
import se.uu.ub.cora.spider.dependency.spy.SpiderDependencyProviderOldSpy;
import se.uu.ub.cora.spider.extendedfunctionality.ExtendedFunctionalityData;
import se.uu.ub.cora.spider.extendedfunctionality.internal.ExtendedFunctionalityProviderSpy;
import se.uu.ub.cora.spider.log.LoggerFactorySpy;
import se.uu.ub.cora.spider.record.DataCopierFactorySpy;
import se.uu.ub.cora.spider.record.DataException;
import se.uu.ub.cora.spider.record.DataGroupToRecordEnhancerSpy;
import se.uu.ub.cora.spider.record.DataRedactorSpy;
import se.uu.ub.cora.spider.record.RecordLinkTestsRecordStorage;
import se.uu.ub.cora.spider.record.RecordUpdater;
import se.uu.ub.cora.spider.spy.DataGroupTermCollectorSpy;
import se.uu.ub.cora.spider.spy.DataRecordLinkCollectorSpy;
import se.uu.ub.cora.spider.spy.DataValidatorSpy;
import se.uu.ub.cora.spider.spy.OldRecordStorageSpy;
import se.uu.ub.cora.spider.spy.RecordArchiveSpy;
import se.uu.ub.cora.spider.spy.RecordIndexerSpy;
import se.uu.ub.cora.spider.spy.RecordStorageCreateUpdateSpy;
import se.uu.ub.cora.spider.spy.RecordStorageUpdateMultipleTimesSpy;
import se.uu.ub.cora.spider.spy.RuleCalculatorSpy;
import se.uu.ub.cora.spider.spy.SpiderAuthorizatorSpy;
import se.uu.ub.cora.spider.testdata.DataCreator;
import se.uu.ub.cora.spider.testdata.DataCreator2;
import se.uu.ub.cora.spider.testdata.RecordLinkTestsDataCreator;
import se.uu.ub.cora.spider.testdata.TestDataRecordInMemoryStorage;
import se.uu.ub.cora.storage.RecordNotFoundException;
import se.uu.ub.cora.storage.RecordStorage;
import se.uu.ub.cora.testspies.data.DataFactorySpy;
import se.uu.ub.cora.testspies.data.DataGroupSpy;
import se.uu.ub.cora.testspies.data.DataRecordLinkSpy;

public class RecordUpdaterTest {
	private static final String TIMESTAMP_FORMAT = "\\d{4}-\\d{2}-\\d{2}T\\d{2}:\\d{2}:\\d{2}\\.\\d{6}Z";
	private RecordStorage recordStorage;
	private RecordArchiveSpy recordArchive;
	private AuthenticatorSpy authenticator;
	private SpiderAuthorizatorSpy authorizator;
	private PermissionRuleCalculator ruleCalculator;
	private RecordUpdater recordUpdater;
	private DataValidatorSpy dataValidator;
	private DataRecordLinkCollector linkCollector;
	private SpiderDependencyProviderOldSpy dependencyProvider;
	private ExtendedFunctionalityProviderSpy extendedFunctionalityProvider;
	private DataGroupToRecordEnhancerSpy dataGroupToRecordEnhancer;
	private DataGroupTermCollectorSpy termCollector;
	private RecordIndexerSpy recordIndexer;
	private LoggerFactorySpy loggerFactorySpy;
	private DataFactorySpy dataFactorySpy;

	private DataGroupFactory dataGroupFactorySpy;
	private DataAtomicFactory dataAtomicFactorySpy;
	private DataCopierFactory dataCopierFactory;
	private RecordTypeHandlerSpy recordTypeHandlerSpy;
	private DataRedactorSpy dataRedactor;

	@BeforeMethod
	public void beforeMethod() {
		setUpFactoriesAndProviders();
		authenticator = new AuthenticatorSpy();
		authorizator = new SpiderAuthorizatorSpy();
		dataValidator = new DataValidatorSpy();
		recordStorage = TestDataRecordInMemoryStorage.createRecordStorageInMemoryWithTestData();
		ruleCalculator = new RuleCalculatorSpy();
		linkCollector = new DataRecordLinkCollectorSpy();
		termCollector = new DataGroupTermCollectorSpy();
		recordIndexer = new RecordIndexerSpy();
		dataRedactor = new DataRedactorSpy();
		extendedFunctionalityProvider = new ExtendedFunctionalityProviderSpy();
		recordArchive = new RecordArchiveSpy();
		setUpDependencyProvider();
	}

	private void setUpFactoriesAndProviders() {
		loggerFactorySpy = new LoggerFactorySpy();
		LoggerProvider.setLoggerFactory(loggerFactorySpy);
		dataFactorySpy = new DataFactorySpy();
		DataProvider.onlyForTestSetDataFactory(dataFactorySpy);

		dataGroupFactorySpy = new DataGroupFactorySpy();
		DataGroupProvider.setDataGroupFactory(dataGroupFactorySpy);
		dataAtomicFactorySpy = new DataAtomicFactorySpy();
		DataAtomicProvider.setDataAtomicFactory(dataAtomicFactorySpy);
		dataCopierFactory = new DataCopierFactorySpy();
		DataCopierProvider.setDataCopierFactory(dataCopierFactory);
	}

	private void setUpDependencyProvider() {
		dependencyProvider = new SpiderDependencyProviderOldSpy(new HashMap<>());
		dependencyProvider.authenticator = authenticator;
		dependencyProvider.spiderAuthorizator = authorizator;
		dependencyProvider.dataValidator = dataValidator;
		dependencyProvider.recordStorage = recordStorage;
		dependencyProvider.ruleCalculator = ruleCalculator;
		dependencyProvider.linkCollector = linkCollector;
		dependencyProvider.extendedFunctionalityProvider = extendedFunctionalityProvider;
		dependencyProvider.termCollector = termCollector;
		dependencyProvider.recordIndexer = recordIndexer;
		dependencyProvider.recordArchive = recordArchive;
		dataGroupToRecordEnhancer = new DataGroupToRecordEnhancerSpy();
		recordTypeHandlerSpy = dependencyProvider.recordTypeHandlerSpy;
		dependencyProvider.dataRedactor = dataRedactor;
		recordUpdater = RecordUpdaterImp.usingDependencyProviderAndDataGroupToRecordEnhancer(
				dependencyProvider, dataGroupToRecordEnhancer);
	}

	@Test
	public void testExternalDependenciesAreCalled() {
		recordStorage = new OldRecordStorageSpy();
		ruleCalculator = new RuleCalculatorSpy();
		setUpDependencyProvider();

		DataGroup dataGroup = new DataGroupOldSpy("nameInData");

		dataGroup.addChild(DataCreator2.createRecordInfoWithRecordTypeAndRecordIdAndDataDivider(
				"spyType", "spyId", "cora"));
		recordUpdater.updateRecord("someToken78678567", "spyType", "spyId", dataGroup);

		authorizator.MCR.assertMethodWasCalled("checkUserIsAuthorizedForActionOnRecordType");
		dataValidator.MCR.assertMethodWasCalled("validateData");

		assertTrue(((OldRecordStorageSpy) recordStorage).updateWasCalled);
		assertTrue(((DataRecordLinkCollectorSpy) linkCollector).collectLinksWasCalled);
		assertEquals(((DataRecordLinkCollectorSpy) linkCollector).metadataId,
				"fakeMetadataIdFromRecordTypeHandlerSpy");

		assertCorrectSearchTermCollectorAndIndexer();
	}

	@Test
	public void testRecordTypeHandlerFetchedFromDependencyProvider() {
		recordStorage = new OldRecordStorageSpy();
		ruleCalculator = new RuleCalculatorSpy();
		setUpDependencyProvider();

		DataGroup dataGroup = new DataGroupOldSpy("nameInData");

		dataGroup.addChild(DataCreator2.createRecordInfoWithRecordTypeAndRecordIdAndDataDivider(
				"spyType", "spyId", "cora"));
		recordUpdater.updateRecord("someToken78678567", "spyType", "spyId", dataGroup);

		dependencyProvider.MCR.assertParameters("getRecordTypeHandler", 0, "spyType");
		dependencyProvider.MCR.assertNumberOfCallsToMethod("getRecordTypeHandler", 1);
	}

	private void assertCorrectSearchTermCollectorAndIndexer() {

		termCollector.MCR.assertParameter("collectTerms", 1, "metadataId",
				"fakeMetadataIdFromRecordTypeHandlerSpy");

		CollectTerms collectTerms = (CollectTerms) termCollector.MCR.getReturnValue("collectTerms",
				1);
		recordIndexer.MCR.assertParameter("indexData", 0, "indexTerms", collectTerms.indexTerms);

	}

	@Test
	public void testCorrectSpiderAuthorizatorForNoRecordPartConstraints() throws Exception {
		recordStorage = new OldRecordStorageSpy();
		ruleCalculator = new RuleCalculatorSpy();
		setUpDependencyProvider();

		recordTypeHandlerSpy.recordPartConstraint = "";
		DataGroup dataGroup = new DataGroupOldSpy("nameInData");

		dataGroup.addChild(DataCreator2.createRecordInfoWithRecordTypeAndRecordIdAndDataDivider(
				"spyType", "spyId", "cora"));
		recordUpdater.updateRecord("someToken78678567", "spyType", "spyId", dataGroup);

		authorizator.MCR.assertParameters(
				"checkGetUsersMatchedRecordPartPermissionsForActionOnRecordTypeAndCollectedData", 0,
				authenticator.returnedUser, "update", "spyType",
				termCollector.MCR.getReturnValue("collectTerms", 0), false);

		authorizator.MCR.assertParameters(
				"checkUserIsAuthorizedForActionOnRecordTypeAndCollectedData", 0,
				authenticator.returnedUser, "update", "spyType",
				termCollector.MCR.getReturnValue("collectTerms", 1));
		authorizator.MCR.assertNumberOfCallsToMethod(
				"checkUserIsAuthorizedForActionOnRecordTypeAndCollectedData", 1);

		dataRedactor.MCR.assertMethodNotCalled("replaceChildrenForConstraintsWithoutPermissions");
		dataRedactor.MCR.assertMethodNotCalled("removeChildrenForConstraintsWithoutPermissions");

		dataValidator.MCR.assertParameter("validateData", 0, "dataGroup", dataGroup);
	}

	@Test
	public void testCorrectSpiderAuthorizatorForWriteRecordPartConstraints() throws Exception {
		recordStorage = new OldRecordStorageSpy();
		ruleCalculator = new RuleCalculatorSpy();
		setUpDependencyProvider();

		recordTypeHandlerSpy.recordPartConstraint = "write";
		DataGroup dataGroup = new DataGroupOldSpy("nameInData");

		dataGroup.addChild(DataCreator2.createRecordInfoWithRecordTypeAndRecordIdAndDataDivider(
				"spyType777", "spyId", "cora"));

		recordUpdater.updateRecord("someToken78678567", "spyType", "spyId", dataGroup);

		authorizator.MCR.assertParameters(
				"checkGetUsersMatchedRecordPartPermissionsForActionOnRecordTypeAndCollectedData", 0,
				authenticator.returnedUser, "update", "spyType",
				termCollector.MCR.getReturnValue("collectTerms", 0), true);
		authorizator.MCR.assertNumberOfCallsToMethod(
				"checkGetUsersMatchedRecordPartPermissionsForActionOnRecordTypeAndCollectedData",
				1);

		authorizator.MCR.assertParameters(
				"checkUserIsAuthorizedForActionOnRecordTypeAndCollectedData", 0,
				authenticator.returnedUser, "update", "spyType",
				termCollector.MCR.getReturnValue("collectTerms", 1));
		authorizator.MCR.assertNumberOfCallsToMethod(
				"checkUserIsAuthorizedForActionOnRecordTypeAndCollectedData", 1);

		dataRedactor.MCR.assertMethodWasCalled("replaceChildrenForConstraintsWithoutPermissions");

		Set<?> expectedPermissions = (Set<?>) authorizator.MCR.getReturnValue(
				"checkGetUsersMatchedRecordPartPermissionsForActionOnRecordTypeAndCollectedData",
				0);
		dataRedactor.MCR.assertParameters("replaceChildrenForConstraintsWithoutPermissions", 0,
				recordTypeHandlerSpy.getMetadataId(), ((OldRecordStorageSpy) recordStorage).aRecord,
				dataGroup, recordTypeHandlerSpy.writeConstraints, expectedPermissions);

		DataGroup returnedRedactedDataGroup = (DataGroup) dataRedactor.MCR
				.getReturnValue("replaceChildrenForConstraintsWithoutPermissions", 0);
		dataValidator.MCR.assertParameter("validateData", 0, "dataGroup",
				returnedRedactedDataGroup);

		// reading updated data
		dataRedactor.MCR.assertMethodNotCalled("removeChildrenForConstraintsWithoutPermissions");
	}

	@Test
	public void testRecordEnhancerCalled() {
		authorizator = new SpiderAuthorizatorSpy();
		recordStorage = new OldRecordStorageSpy();
		ruleCalculator = new RuleCalculatorSpy();
		setUpDependencyProvider();
		DataGroup dataGroup = new DataGroupOldSpy("nameInData");
		dataGroup.addChild(DataCreator2.createRecordInfoWithRecordTypeAndRecordIdAndDataDivider(
				"spyType", "spyId", "cora"));
		recordUpdater.updateRecord("someToken78678567", "spyType", "spyId", dataGroup);

		dataGroupToRecordEnhancer.MCR.assertParameters("enhance", 0, authenticator.returnedUser,
				"spyType", dataGroup, dataRedactor);

	}

	@Test
	public void testCorrectRecordInfoInUpdatedRecord() {
		recordStorage = new OldRecordStorageSpy();
		ruleCalculator = new RuleCalculatorSpy();
		setUpDependencyProvider();

		DataGroup dataGroup = new DataGroupOldSpy("someRecordDataGroup");
		addRecordInfoWithCreatedInfo(dataGroup);

		DataRecord updatedOnce = recordUpdater.updateRecord("someToken78678567", "spyType", "spyId",
				dataGroup);

		assertUpdatedRepeatIdsInGroupAsListed(updatedOnce, "0");
		assertCorrectUserInfo(updatedOnce);
	}

	private void addRecordInfoWithCreatedInfo(DataGroup topDataGroup) {
		DataGroup recordInfo = DataCreator2.createRecordInfoWithRecordTypeAndRecordIdAndDataDivider(
				"spyType", "spyId", "cora");
		DataGroup createdBy = createLinkWithNameInDataRecordTypeAndRecordId("createdBy", "user",
				"6789");
		recordInfo.addChild(createdBy);
		recordInfo.addChild(new DataAtomicSpy("tsCreated", "2016-10-01T00:00:00.000000Z"));
		topDataGroup.addChild(recordInfo);
	}

	private void assertUpdatedRepeatIdsInGroupAsListed(DataRecord updatedRecord,
			String... expectedRepeatIds) {
		DataGroup updatedDataGroup = updatedRecord.getDataGroup();
		DataGroup updatedRecordInfo = updatedDataGroup.getFirstGroupWithNameInData("recordInfo");

		List<DataGroup> updatedGroups = updatedRecordInfo.getAllGroupsWithNameInData("updated");
		assertEquals(updatedGroups.size(), expectedRepeatIds.length);

		for (int i = 0; i < expectedRepeatIds.length; i++) {
			DataGroup updatedGroup = updatedGroups.get(i);
			String repeatId = updatedGroup.getRepeatId();
			assertEquals(repeatId, expectedRepeatIds[i]);
		}
	}

	private void assertCorrectUserInfo(DataRecord updatedOnce) {
		DataGroup updatedOnceDataGroup = updatedOnce.getDataGroup();
		DataGroup updatedOnceRecordInfo = updatedOnceDataGroup
				.getFirstGroupWithNameInData("recordInfo");
		assertCorrectDataUsingGroupNameInDataAndLinkedRecordId(updatedOnceRecordInfo, "createdBy",
				"4422");

		String tsCreated = updatedOnceRecordInfo.getFirstAtomicValueWithNameInData("tsCreated");
		assertTrue(tsCreated.matches(TIMESTAMP_FORMAT));

		DataGroup updated = updatedOnceRecordInfo.getFirstGroupWithNameInData("updated");
		dataFactorySpy.MCR.assertParameters("factorRecordLinkUsingNameInDataAndTypeAndId", 0,
				"updatedBy", "user", "12345");
		var updatedByLink = dataFactorySpy.MCR
				.getReturnValue("factorRecordLinkUsingNameInDataAndTypeAndId", 0);
		assertDataChildFoundInChildren(updatedByLink, updated.getChildren());

		String tsUpdated = updated.getFirstAtomicValueWithNameInData("tsUpdated");
		assertTrue(tsUpdated.matches(TIMESTAMP_FORMAT));
		assertFalse(tsUpdated.equals(tsCreated));
	}

	private void assertDataChildFoundInChildren(Object createdByLink, List<DataChild> children) {
		boolean createdByAdded = false;
		for (DataChild dataChild : children) {
			if (dataChild == createdByLink) {
				createdByAdded = true;
			}
		}
		assertTrue(createdByAdded);
	}

	@Test
	public void testFormatKeepsNanoZeros() throws Exception {
		Instant now = Instant.now();
		int nano = now.getNano();
		Instant minusNanos = now.minusNanos(nano);
		RecordUpdaterImp recordUpdater2 = (RecordUpdaterImp) recordUpdater;
		String formattedTS = recordUpdater2.formatInstantKeepingTrailingZeros(minusNanos);
		assertEquals(formattedTS.substring(20), "000000Z");

	}

	private void assertCorrectDataUsingGroupNameInDataAndLinkedRecordId(DataGroup updatedRecordInfo,
			String groupNameInData, String linkedRecordId) {
		DataGroup createdByGroup = updatedRecordInfo.getFirstGroupWithNameInData(groupNameInData);
		assertEquals(createdByGroup.getFirstAtomicValueWithNameInData("linkedRecordType"), "user");
		assertEquals(createdByGroup.getFirstAtomicValueWithNameInData("linkedRecordId"),
				linkedRecordId);
	}

	private DataGroup createLinkWithNameInDataRecordTypeAndRecordId(String nameInData,
			String linkedRecordType, String linkedRecordId) {
		DataGroup recordLink = new DataGroupOldSpy(nameInData);
		recordLink.addChild(new DataAtomicSpy("linkedRecordType", linkedRecordType));
		recordLink.addChild(new DataAtomicSpy("linkedRecordId", linkedRecordId));
		return recordLink;
	}

	@Test
	public void testCorrectUpdateInfoWhenRecordHasAlreadyBeenUpdated() {
		recordStorage = new RecordStorageUpdateMultipleTimesSpy();
		ruleCalculator = new RuleCalculatorSpy();
		setUpDependencyProvider();
		DataGroup dataGroup = new DataGroupOldSpy("nameInData");
		addRecordInfoWithCreatedInfo(dataGroup);

		RecordStorageUpdateMultipleTimesSpy recordStorageSpy = (RecordStorageUpdateMultipleTimesSpy) recordStorage;
		updateStorageToReturnDataGroupIncludingUpdateInfo();
		DataRecord updatedOnce = recordUpdater.updateRecord("someToken78678567", "spyType", "spyId",
				dataGroup);
		assertEquals(recordStorageSpy.types.size(), 1);

		assertUpdatedRepeatIdsInGroupAsListed(updatedOnce, "0", "1");

	}

	private void updateStorageToReturnDataGroupIncludingUpdateInfo() {
		((RecordStorageUpdateMultipleTimesSpy) recordStorage).alreadyCalled = true;
	}

	@Test
	public void testNewUpdatedInRecordInfoHasRepeatIdPlusOne() {
		recordStorage = new RecordStorageUpdateMultipleTimesSpy();
		ruleCalculator = new RuleCalculatorSpy();
		setUpDependencyProvider();
		DataGroup dataGroup = new DataGroupOldSpy("nameInData");
		addRecordInfoWithCreatedInfo(dataGroup);

		DataRecord updatedOnceRecord = recordUpdater.updateRecord("someToken78678567", "spyType",
				"spyId", dataGroup);
		assertCorrectUserInfo(updatedOnceRecord);

		updateStorageToReturnDataGroupIncludingUpdateInfo();
		DataRecord updatedTwice = recordUpdater.updateRecord("someToken78678567", "spyType",
				"spyId", dataGroup);
		assertUpdatedRepeatIdsInGroupAsListed(updatedTwice, "0", "1");
	}

	@Test
	public void testCreateAndUpdateInfoSetFromPreviousUpdateIsNotReplacedByAlteredData() {
		recordStorage = new RecordStorageUpdateMultipleTimesSpy();
		ruleCalculator = new RuleCalculatorSpy();
		setUpDependencyProvider();

		DataGroup dataGroupToUpdate = new DataGroupOldSpy("nameInData");
		addRecordInfoWithCreatedInfo(dataGroupToUpdate);
		addUpdatedInfoToRecordInfo(dataGroupToUpdate);

		updateStorageToReturnDataGroupIncludingUpdateInfo();
		DataRecord updatedRecord = recordUpdater.updateRecord("someToken78678567", "spyType",
				"spyId", dataGroupToUpdate);

		DataGroup firstUpdated = getFirstUpdatedInfo(updatedRecord);
		String firstTsUpdated = firstUpdated.getFirstAtomicValueWithNameInData("tsUpdated");
		String correctTsUpdated = "2014-12-18 20:20:38.346";

		assertEquals(firstTsUpdated, correctTsUpdated);

		DataGroup updatedDataGroup = updatedRecord.getDataGroup();
		DataGroup recordInfo = updatedDataGroup.getFirstGroupWithNameInData("recordInfo");
		DataGroup createdBy = recordInfo.getFirstGroupWithNameInData("createdBy");
		assertEquals(createdBy.getFirstAtomicValueWithNameInData("linkedRecordId"), "4422");
		assertEquals(recordInfo.getFirstAtomicValueWithNameInData("tsCreated"),
				"2014-08-01T00:00:00.000000Z");
	}

	private void addUpdatedInfoToRecordInfo(DataGroup dataGroup) {
		DataGroup recordInfo = dataGroup.getFirstGroupWithNameInData("recordInfo");
		DataGroup updated = new DataGroupOldSpy("updated");
		updated.setRepeatId("0");
		// DataRecordLink updatedBy = new DataRecordLinkSpy("updatedBy");
		DataRecordLinkSpy updatedBy = new DataRecordLinkSpy();
		updated.addChild(updatedBy);
		updated.addChild(new DataAtomicSpy("tsUpdated", "2010-12-13 20:20:38.346"));
		recordInfo.addChild(updated);
	}

	private DataGroup getFirstUpdatedInfo(DataRecord updatedRecord) {
		DataGroup updatedRecordInfo = updatedRecord.getDataGroup()
				.getFirstGroupWithNameInData("recordInfo");
		DataGroup firstUpdated = updatedRecordInfo.getFirstGroupWithNameInData("updated");
		return firstUpdated;
	}

	@Test
	public void testUserIsAuthorizedForPreviousVersionOfData() {
		recordStorage = new RecordStorageCreateUpdateSpy();
		setUpDependencyProvider();

		DataGroup dataGroup = getDataGroupToUpdate();
		dataGroup.addChild(new DataAtomicSpy("notStoredValue", "hej"));

		recordUpdater.updateRecord("someToken78678567", "typeWithAutoGeneratedId", "somePlace",
				dataGroup);

		authorizator.MCR.assertParameters(
				"checkGetUsersMatchedRecordPartPermissionsForActionOnRecordTypeAndCollectedData", 0,
				authenticator.returnedUser, "update", "typeWithAutoGeneratedId",
				termCollector.MCR.getReturnValue("collectTerms", 0), false);

		authorizator.MCR.assertNumberOfCallsToMethod(
				"checkGetUsersMatchedRecordPartPermissionsForActionOnRecordTypeAndCollectedData",
				1);

		authorizator.MCR.assertParameters(
				"checkUserIsAuthorizedForActionOnRecordTypeAndCollectedData", 0,
				authenticator.returnedUser, "update", "typeWithAutoGeneratedId",
				termCollector.MCR.getReturnValue("collectTerms", 1));
		authorizator.MCR.assertNumberOfCallsToMethod(
				"checkUserIsAuthorizedForActionOnRecordTypeAndCollectedData", 1);
	}

	@Test(expectedExceptions = AuthenticationException.class)
	public void testAuthenticationNotAuthenticated() {
		authenticator.throwAuthenticationException = true;
		recordStorage = new OldRecordStorageSpy();
		setUpDependencyProvider();
		DataGroup dataGroup = DataCreator2.createRecordWithNameInDataAndIdAndTypeAndLinkedRecordId(
				"nameInData", "spyId", "spyType", "cora");
		recordUpdater.updateRecord("dummyNonAuthenticatedToken", "spyType", "spyId", dataGroup);
	}

	@Test
	public void testUnauthorizedForUpdateOnRecordTypeShouldNotAccessStorage() {
		recordStorage = new OldRecordStorageSpy();
		setUpDependencyProvider();

		authorizator.authorizedForActionAndRecordType = false;

		DataGroup dataGroup = new DataGroupOldSpy("nameInData");
		dataGroup.addChild(DataCreator2.createRecordInfoWithRecordTypeAndRecordIdAndDataDivider(
				"spyType", "spyId", "cora"));

		boolean exceptionWasCaught = false;
		try {
			recordUpdater.updateRecord("someToken78678567", "spyType", "spyId", dataGroup);
		} catch (Exception e) {
			assertEquals(e.getClass(), AuthorizationException.class);
			exceptionWasCaught = true;
		}
		assertTrue(exceptionWasCaught);
		assertFalse(((OldRecordStorageSpy) recordStorage).readWasCalled);
		assertFalse(((OldRecordStorageSpy) recordStorage).updateWasCalled);
		assertFalse(((OldRecordStorageSpy) recordStorage).deleteWasCalled);
		assertFalse(((OldRecordStorageSpy) recordStorage).createWasCalled);
	}

	@Test
	public void testExtendedFunctionalityIsCalled() {
		authorizator = new SpiderAuthorizatorSpy();
		RecordStorageCreateUpdateSpy recordStorageSpy = new RecordStorageCreateUpdateSpy();
		recordStorage = recordStorageSpy;
		ruleCalculator = new RuleCalculatorSpy();
		setUpDependencyProvider();

		String recordType = "spyType";
		String recordId = "spyId";
		DataGroup dataGroup = DataCreator2.createRecordWithNameInDataAndIdAndTypeAndLinkedRecordId(
				"nameInData", recordId, recordType, "cora");
		String authToken = "someToken78678567";

		recordUpdater.updateRecord(authToken, recordType, recordId, dataGroup);

		ExtendedFunctionalityData expectedData = new ExtendedFunctionalityData();
		expectedData.recordType = recordType;
		expectedData.recordId = recordId;
		expectedData.authToken = authToken;
		expectedData.user = (User) authenticator.MCR.getReturnValue("getUserForToken", 0);
		expectedData.previouslyStoredTopDataGroup = (DataGroup) recordStorageSpy.MCR
				.getReturnValue("read", 0);
		expectedData.dataGroup = dataGroup;

		extendedFunctionalityProvider.assertCallToMethodAndFunctionalityCalledWithData(
				"getFunctionalityForUpdateBeforeMetadataValidation", expectedData);
		extendedFunctionalityProvider.assertCallToMethodAndFunctionalityCalledWithData(
				"getFunctionalityForUpdateAfterMetadataValidation", expectedData);
		extendedFunctionalityProvider.assertCallToMethodAndFunctionalityCalledWithData(
				"getFunctionalityForUpdateBeforeStore", expectedData);
		extendedFunctionalityProvider.assertCallToMethodAndFunctionalityCalledWithData(
				"getFunctionalityForUpdateAfterStore", expectedData);
	}

	@Test
	public void testIndexerIsCalled() {
		recordStorage = new RecordStorageCreateUpdateSpy();
		setUpDependencyProvider();

		DataGroup dataGroup = getDataGroupToUpdate();

		recordUpdater.updateRecord("someToken78678567", "typeWithAutoGeneratedId", "somePlace",
				dataGroup);

		DataGroup updatedRecord = ((RecordStorageCreateUpdateSpy) recordStorage).updateRecord;
		RecordIndexerSpy recordIndexerSpy = recordIndexer;
		DataGroup indexedRecord = recordIndexerSpy.record;
		assertEquals(indexedRecord, updatedRecord);
		List<String> ids = recordIndexerSpy.ids;
		assertEquals(ids.get(0), "fakeCombinedIdFromRecordTypeHandlerSpy");
		assertEquals(ids.size(), 1);
	}

	@Test
	public void testIndexerIsCalledForChildOfAbstract() {
		recordStorage = new RecordStorageCreateUpdateSpy();
		setUpDependencyProvider();

		DataGroup dataGroup = getDataGroupForImageToUpdate();

		recordUpdater.updateRecord("someToken78678567", "image", "someImage", dataGroup);

		DataGroup updatedRecord = ((RecordStorageCreateUpdateSpy) recordStorage).updateRecord;
		RecordIndexerSpy recordIndexerSpy = recordIndexer;
		DataGroup indexedRecord = recordIndexerSpy.record;
		assertEquals(indexedRecord, updatedRecord);
		List<String> ids = recordIndexerSpy.ids;
		assertEquals(ids.get(0), "fakeCombinedIdFromRecordTypeHandlerSpy");
		assertEquals(ids.size(), 1);
	}

	private DataGroup getDataGroupForImageToUpdate() {
		DataGroup dataGroup = new DataGroupOldSpy("binary");
		DataGroup createRecordInfo = DataCreator2
				.createRecordInfoWithIdAndLinkedRecordId("someImage", "cora");
		// DataGroup typeGroup = new DataGroupOldSpy("type");
		// typeGroup.addChild(new DataAtomicSpy("linkedRecordType", "recordType"));
		// typeGroup.addChild(new DataAtomicSpy("linkedRecordId", "image"));
		// createRecordInfo.addChild(typeGroup);
		createRecordInfo.addChild(createLinkWithLinkedId("type", "linkedRecordType", "image"));
		dataGroup.addChild(createRecordInfo);
		dataGroup.addChild(new DataAtomicSpy("atomicId", "atomicValue"));
		return dataGroup;
	}

	public static DataRecordLink createLinkWithLinkedId(String nameInData, String linkedRecordType,
			String id) {
		DataRecordLinkSpy linkSpy = new DataRecordLinkSpy();
		linkSpy.MRV.setDefaultReturnValuesSupplier("getNameInData",
				(Supplier<String>) () -> nameInData);
		linkSpy.MRV.setDefaultReturnValuesSupplier("getLinkedRecordId",
				(Supplier<String>) () -> id);
		return linkSpy;
	}

	private DataGroup getDataGroupToUpdate() {
		DataGroup dataGroup = new DataGroupOldSpy("typeWithUserGeneratedId");
		DataGroup createRecordInfo = DataCreator2
				.createRecordInfoWithIdAndLinkedRecordId("somePlace", "cora");
		// DataGroup typeGroup = new DataGroupOldSpy("type");
		// typeGroup.addChild(new DataAtomicSpy("linkedRecordType", "recordType"));
		// typeGroup.addChild(new DataAtomicSpy("linkedRecordId", "typeWithAutoGeneratedId"));
		createRecordInfo.addChild(
				createLinkWithLinkedId("type", "linkedRecordType", "typeWithAutoGeneratedId"));

		dataGroup.addChild(createRecordInfo);
		dataGroup.addChild(new DataAtomicSpy("atomicId", "atomicValue"));
		return dataGroup;
	}

	@Test
	public void testUpdateRecordDataDividerExtractedFromData() {
		recordStorage = new RecordStorageCreateUpdateSpy();
		setUpDependencyProvider();

		DataGroup dataGroup = getDataGroupToUpdate();

		recordUpdater.updateRecord("someToken78678567", "typeWithAutoGeneratedId", "somePlace",
				dataGroup);

		assertEquals(((RecordStorageCreateUpdateSpy) recordStorage).dataDivider, "cora");
	}

	@Test
	public void testCreateRecordCollectedTermsSentAlong() {
		recordStorage = new RecordStorageCreateUpdateSpy();
		setUpDependencyProvider();

		DataGroup dataGroup = getDataGroupToUpdate();

		recordUpdater.updateRecord("someToken78678567", "typeWithAutoGeneratedId", "somePlace",
				dataGroup);

		termCollector.MCR.assertParameter("collectTerms", 1, "metadataId",
				"fakeMetadataIdFromRecordTypeHandlerSpy");
		assertEquals(((RecordStorageCreateUpdateSpy) recordStorage).collectedTerms,
				termCollector.MCR.getReturnValue("collectTerms", 1));

	}

	@Test(expectedExceptions = RecordNotFoundException.class)
	public void testUpdateNotFound() {
		DataGroup record = new DataGroupOldSpy("authority");
		DataGroup recordInfo = new DataGroupOldSpy("recordInfo");
		DataAtomic idData = new DataAtomicSpy("id", "NOT_FOUND");
		recordInfo.addChild(idData);
		recordInfo.addChild(new DataAtomicSpy("type", "recordType"));
		recordInfo.addChild(new DataAtomicSpy("createdBy", "someToken78678567"));

		record.addChild(recordInfo);
		recordUpdater.updateRecord("someToken78678567", "recordType", "NOT_FOUND", record);
	}

	@Test(expectedExceptions = DataMissingException.class)
	public void testUpdateRecordRecordInfoMissing() {
		recordStorage = new RecordStorageCreateUpdateSpy();
		setUpDependencyProvider();

		DataGroup group = new DataGroupOldSpy("authority");
		recordUpdater.updateRecord("someToken78678567", "typeWithAutoGeneratedId", "place", group);
	}

	@Test(expectedExceptions = DataMissingException.class)
	public void testUpdateRecordRecordInfoContentMissing() {
		recordStorage = new RecordStorageCreateUpdateSpy();
		setUpDependencyProvider();

		DataGroup group = new DataGroupOldSpy("authority");
		DataGroup createRecordInfo = new DataGroupOldSpy("recordInfo");
		group.addChild(createRecordInfo);
		recordUpdater.updateRecord("someToken78678567", "typeWithAutoGeneratedId", "place", group);
	}

	@Test(expectedExceptions = DataException.class)
	public void testUpdateRecordInvalidData() {
		recordStorage = new RecordStorageCreateUpdateSpy();
		setUpDependencyProvider();

		dataValidator.validValidation = false;

		DataGroup dataGroup = new DataGroupOldSpy("typeWithUserGeneratedId");
		DataGroup createRecordInfo = new DataGroupOldSpy("recordInfo");
		createRecordInfo.addChild(new DataAtomicSpy("id", "place"));
		createRecordInfo.addChild(new DataAtomicSpy("type", "recordType"));

		dataGroup.addChild(createRecordInfo);
		dataGroup.addChild(new DataAtomicSpy("atomicId", "atomicValue"));

		recordUpdater.updateRecord("someToken78678567", "typeWithAutoGeneratedId", "place",
				dataGroup);
	}

	@Test(expectedExceptions = RecordNotFoundException.class)
	public void testNonExistingRecordType() {
		DataGroup record = new DataGroupOldSpy("authority");
		recordUpdater.updateRecord("someToken78678567", "recordType_NOT_EXISTING", "id", record);
	}

	@Test(expectedExceptions = DataException.class)
	public void testUpdateRecordIncomingNameInDatasDoNotMatch() {
		recordStorage = new RecordStorageCreateUpdateSpy();
		setUpDependencyProvider();

		DataGroup dataGroup = new DataGroupOldSpy("typeWithUserGeneratedId");
		DataGroup createRecordInfo = new DataGroupOldSpy("recordInfo");
		createRecordInfo.addChild(new DataAtomicSpy("id", "place"));
		createRecordInfo.addChild(new DataAtomicSpy("type", "recordType"));
		dataGroup.addChild(createRecordInfo);
		dataGroup.addChild(new DataAtomicSpy("atomicId", "atomicValue"));

		recordUpdater.updateRecord("someToken78678567", "typeWithAutoGeneratedId",
				"place_NOT_THE_SAME", dataGroup);
	}

	@Test(expectedExceptions = DataException.class)
	public void testUpdateRecordIncomingDataTypesDoNotMatch() {
		recordStorage = new RecordStorageCreateUpdateSpy();
		setUpDependencyProvider();

		DataGroup dataGroup = new DataGroupOldSpy("typeWithUserGeneratedId_NOT_THE_SAME");
		DataGroup createRecordInfo = new DataGroupOldSpy("recordInfo");
		createRecordInfo.addChild(new DataAtomicSpy("id", "place"));
		createRecordInfo.addChild(createLinkWithLinkedId("type", "linkedRecordType", "recordType"));

		dataGroup.addChild(createRecordInfo);
		dataGroup.addChild(new DataAtomicSpy("atomicId", "atomicValue"));

		recordUpdater.updateRecord("someToken78678567", "typeWithAutoGeneratedId", "place",
				dataGroup);
	}

	@Test(expectedExceptions = DataException.class)
	public void testUpdateRecordIncomingDataIdDoNotMatch() {
		recordStorage = new RecordStorageCreateUpdateSpy();
		setUpDependencyProvider();

		DataGroup dataGroup = new DataGroupOldSpy("typeWithUserGeneratedId_NOT_THE_SAME");
		DataGroup createRecordInfo = new DataGroupOldSpy("recordInfo");
		createRecordInfo.addChild(new DataAtomicSpy("id", "place"));
		createRecordInfo.addChild(new DataAtomicSpy("type", "recordType"));
		dataGroup.addChild(createRecordInfo);
		dataGroup.addChild(new DataAtomicSpy("atomicId", "atomicValue"));

		recordUpdater.updateRecord("someToken78678567", "recordType", "placeNOT", dataGroup);
	}

	@Test(expectedExceptions = DataException.class)
	public void testLinkedRecordIdDoesNotExist() {
		recordStorage = new RecordLinkTestsRecordStorage();
		linkCollector = DataCreator.getDataRecordLinkCollectorSpyWithCollectedLinkAdded();
		setUpDependencyProvider();

		((RecordLinkTestsRecordStorage) recordStorage).recordIdExistsForRecordType = false;

		DataGroup dataGroup = RecordLinkTestsDataCreator
				.createDataDataGroupWithRecordInfoAndLinkOneLevelDown();
		recordUpdater.updateRecord("someToken78678567", "dataWithLinks", "oneLinkOneLevelDown",
				dataGroup);
	}

	@Test
	public void testStoreInArchiveTrue() throws Exception {
		recordStorage = new RecordStorageOldSpy();
		RecordStorageOldSpy oldRecordStorage = (RecordStorageOldSpy) recordStorage;
		setUpDependencyProvider();
		oldRecordStorage.returnForRead = new DataGroupSpy();

		recordTypeHandlerSpy.storeInArchive = true;
		DataGroupSpy recordSpy = createDataGroupForUpdate();

		recordUpdater.updateRecord("someToken", "spyType", "someRecordId", recordSpy);

		recordArchive.MCR.assertParameters("update", 0, "spyType", "someRecordId", recordSpy);
	}

	private DataGroupSpy createDataGroupForUpdate() {
		DataGroupSpy recordSpy = new DataGroupSpy();
		DataGroupSpy recordInfoSpy = new DataGroupSpy();
		recordSpy.MRV.setSpecificReturnValuesSupplier("getFirstGroupWithNameInData",
				(Supplier<DataGroupSpy>) () -> recordInfoSpy, "recordInfo");
		// DataGroupSpy dataDividerSpy = new DataGroupSpy();
		DataRecordLinkSpy dataDividerSpy = new DataRecordLinkSpy();
		recordInfoSpy.MRV.setReturnValues("getFirstChildWithNameInData", List.of(dataDividerSpy),
				"dataDivider");
		recordInfoSpy.MRV.setSpecificReturnValuesSupplier("getFirstAtomicValueWithNameInData",
				(Supplier<String>) () -> "someRecordId", "id");
		recordInfoSpy.MRV.setReturnValues("containsChildWithNameInData", List.of(false), "updated");

		DataRecordLinkSpy typeSpy = new DataRecordLinkSpy();
		recordInfoSpy.MRV.setReturnValues("getFirstChildWithNameInData", List.of(typeSpy), "type");
		typeSpy.MRV.setReturnValues("getLinkedRecordId", List.of("spyType"));

		dataDividerSpy.MRV.setReturnValues("getFirstAtomicValueWithNameInData", List.of("uu"),
				"linkedRecordId");
		return recordSpy;
	}

	@Test
	public void testStoreInArchiveFalse() throws Exception {
		recordStorage = new RecordStorageOldSpy();
		RecordStorageOldSpy oldRecordStorage = (RecordStorageOldSpy) recordStorage;
		setUpDependencyProvider();
		oldRecordStorage.returnForRead = new DataGroupSpy();

		recordTypeHandlerSpy.storeInArchive = false;
		DataGroupSpy recordSpy = createDataGroupForUpdate();

		recordUpdater.updateRecord("someToken", "spyType", "someRecordId", recordSpy);

		recordArchive.MCR.assertMethodNotCalled("update");
	}
}
