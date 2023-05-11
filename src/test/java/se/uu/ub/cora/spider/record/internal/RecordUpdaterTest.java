/*
 * Copyright 2015, 2016, 2018, 2020, 2021, 2022, 2023 Uppsala University Library
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

import static org.testng.Assert.assertTrue;

import java.time.Instant;
import java.util.List;
import java.util.Set;
import java.util.function.Supplier;

import org.testng.AssertJUnit;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import se.uu.ub.cora.beefeater.authentication.User;
import se.uu.ub.cora.bookkeeper.validator.DataValidationException;
import se.uu.ub.cora.data.DataChild;
import se.uu.ub.cora.data.DataGroup;
import se.uu.ub.cora.data.DataLink;
import se.uu.ub.cora.data.DataProvider;
import se.uu.ub.cora.data.DataRecord;
import se.uu.ub.cora.data.DataRecordLink;
import se.uu.ub.cora.data.collected.CollectTerms;
import se.uu.ub.cora.data.collected.Link;
import se.uu.ub.cora.data.collected.PermissionTerm;
import se.uu.ub.cora.data.copier.DataCopierFactory;
import se.uu.ub.cora.data.copier.DataCopierProvider;
import se.uu.ub.cora.data.spies.DataAtomicSpy;
import se.uu.ub.cora.data.spies.DataFactorySpy;
import se.uu.ub.cora.data.spies.DataGroupSpy;
import se.uu.ub.cora.data.spies.DataRecordLinkSpy;
import se.uu.ub.cora.logger.LoggerProvider;
import se.uu.ub.cora.spider.authentication.AuthenticationException;
import se.uu.ub.cora.spider.authentication.AuthenticatorSpy;
import se.uu.ub.cora.spider.authorization.AuthorizationException;
import se.uu.ub.cora.spider.authorization.PermissionRuleCalculator;
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
import se.uu.ub.cora.spider.record.MisuseException;
import se.uu.ub.cora.spider.record.RecordUpdater;
import se.uu.ub.cora.spider.spy.DataGroupTermCollectorSpy;
import se.uu.ub.cora.spider.spy.DataRecordLinkCollectorSpy;
import se.uu.ub.cora.spider.spy.DataValidatorSpy;
import se.uu.ub.cora.spider.spy.OldRecordStorageSpy;
import se.uu.ub.cora.spider.spy.RecordArchiveSpy;
import se.uu.ub.cora.spider.spy.RecordIndexerSpy;
import se.uu.ub.cora.spider.spy.RecordStorageUpdateMultipleTimesSpy;
import se.uu.ub.cora.spider.spy.RuleCalculatorSpy;
import se.uu.ub.cora.spider.spy.SpiderAuthorizatorSpy;
import se.uu.ub.cora.spider.spy.SpiderDependencyProviderSpy;
import se.uu.ub.cora.spider.testdata.DataCreator2;
import se.uu.ub.cora.spider.testdata.RecordLinkTestsDataCreator;
import se.uu.ub.cora.storage.RecordNotFoundException;
import se.uu.ub.cora.storage.RecordStorage;
import se.uu.ub.cora.storage.spies.RecordStorageSpy;

public class RecordUpdaterTest {
	private static final String TIMESTAMP_FORMAT = "\\d{4}-\\d{2}-\\d{2}T\\d{2}:\\d{2}:\\d{2}\\.\\d{6}Z";
	private RecordStorageSpy recordStorage;
	private RecordArchiveSpy recordArchive;
	private AuthenticatorSpy authenticator;
	private SpiderAuthorizatorSpy authorizator;
	private PermissionRuleCalculator ruleCalculator;
	private RecordUpdater recordUpdaterOld;
	private RecordUpdater recordUpdater;
	private DataValidatorSpy dataValidator;
	private DataRecordLinkCollectorSpy linkCollector;
	private SpiderDependencyProviderOldSpy dependencyProvider;
	private SpiderDependencyProviderSpy dependencyProviderSpy;
	private ExtendedFunctionalityProviderSpy extendedFunctionalityProvider;
	private DataGroupToRecordEnhancerSpy dataGroupToRecordEnhancer;
	private DataGroupTermCollectorSpy termCollector;
	private RecordIndexerSpy recordIndexer;
	private LoggerFactorySpy loggerFactorySpy;
	private DataFactorySpy dataFactorySpy;

	private DataCopierFactory dataCopierFactory;
	private RecordTypeHandlerSpy recordTypeHandlerSpy;
	private DataRedactorSpy dataRedactor;

	@BeforeMethod
	public void beforeMethod() {
		setUpFactoriesAndProviders();
		authenticator = new AuthenticatorSpy();
		authorizator = new SpiderAuthorizatorSpy();
		dataValidator = new DataValidatorSpy();
		recordStorage = new RecordStorageSpy();
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

		dataCopierFactory = new DataCopierFactorySpy();
		DataCopierProvider.setDataCopierFactory(dataCopierFactory);
	}

	private void setUpDependencyProvider() {
		setUpDependencyProvider(recordStorage);
	}

	private void setUpDependencyProvider(RecordStorage recordStorage) {
		dependencyProvider = new SpiderDependencyProviderOldSpy();
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
		recordTypeHandlerSpy.MRV.setDefaultReturnValuesSupplier("getRecordTypeId", () -> "spyType");
		dependencyProvider.dataRedactor = dataRedactor;
		dependencyProviderSpy = new SpiderDependencyProviderSpy();

		recordUpdaterOld = RecordUpdaterImp.usingDependencyProviderAndDataGroupToRecordEnhancer(
				dependencyProvider, dataGroupToRecordEnhancer);
		recordUpdater = RecordUpdaterImp.usingDependencyProviderAndDataGroupToRecordEnhancer(
				dependencyProviderSpy, dataGroupToRecordEnhancer);
		setUpToReturnFakeDataForUpdatedTS();
	}

	private void setUpToReturnFakeDataForUpdatedTS() {
		DataAtomicSpy atomicTS = new DataAtomicSpy();
		dataFactorySpy.MRV.setDefaultReturnValuesSupplier("factorAtomicUsingNameInDataAndValue",
				() -> atomicTS);
		atomicTS.MRV.setDefaultReturnValuesSupplier("getNameInData", () -> "tsUpdated");
		atomicTS.MRV.setDefaultReturnValuesSupplier("getValue", () -> "fakeTimestampForTsUpdated");
	}

	@Test
	public void testExternalDependenciesAreCalled() {
		DataGroup dataGroup = new DataGroupOldSpy("nameInData");

		dataGroup.addChild(DataCreator2.createRecordInfoWithRecordTypeAndRecordIdAndDataDivider(
				"spyType", "spyId", "cora"));
		recordUpdaterOld.updateRecord("someToken78678567", "spyType", "spyId", dataGroup);

		authorizator.MCR.assertMethodWasCalled("checkUserIsAuthorizedForActionOnRecordType");
		dataValidator.MCR.assertMethodWasCalled("validateData");
		dataValidator.MCR.assertParameters("validateData", 0,
				"fakeUpdateMetadataIdFromRecordTypeHandlerSpy", dataGroup);

		linkCollector.MCR.assertParameters("collectLinks", 0,
				"fakeDefMetadataIdFromRecordTypeHandlerSpy", dataGroup);

		CollectTerms collectedTerms = (CollectTerms) termCollector.MCR
				.getReturnValue("collectTerms", 1);

		var links = linkCollector.MCR.getReturnValue("collectLinks", 0);

		recordStorage.MCR.assertParameters("update", 0, "spyType", "spyId", dataGroup,
				collectedTerms.storageTerms, links);

		assertCorrectSearchTermCollectorAndIndexer();
	}

	private void assertCorrectSearchTermCollectorAndIndexer() {

		termCollector.MCR.assertParameter("collectTerms", 1, "metadataId",
				"fakeDefMetadataIdFromRecordTypeHandlerSpy");

		CollectTerms collectTerms = (CollectTerms) termCollector.MCR.getReturnValue("collectTerms",
				1);
		recordIndexer.MCR.assertParameter("indexData", 0, "indexTerms", collectTerms.indexTerms);

	}

	@Test(expectedExceptions = DataException.class, expectedExceptionsMessageRegExp = "The record "
			+ "cannot be updated because the record type provided does not match the record type "
			+ "that the validation type is set to validate.")
	public void testRecordTypePassedNOTEqualsTheLinkedInValidationType() throws Exception {
		recordTypeHandlerSpy.MRV.setDefaultReturnValuesSupplier("getRecordTypeId",
				() -> "NOTRecordTypeId");

		dependencyProviderSpy.MRV.setDefaultReturnValuesSupplier(
				"getRecordTypeHandlerUsingDataRecordGroup", () -> recordTypeHandlerSpy);

		DataGroupSpy dataGroupSpy = new DataGroupSpy();

		recordUpdaterOld.updateRecord("someToken78678567", "spyType", "spyId", dataGroupSpy);
	}

	@Test
	public void testRecordTypeHandlerFetchedFromDependencyProvider() {
		recordTypeHandlerSpy.MRV.setDefaultReturnValuesSupplier("shouldAutoGenerateId", () -> true);
		dependencyProviderSpy.MRV.setDefaultReturnValuesSupplier(
				"getRecordTypeHandlerUsingDataRecordGroup", () -> recordTypeHandlerSpy);
		DataGroup dataGroup = new DataGroupOldSpy("nameInData");
		dataGroup.addChild(DataCreator2.createRecordInfoWithRecordTypeAndRecordIdAndDataDivider(
				"spyType", "spyId", "cora"));

		recordUpdater.updateRecord("someToken78678567", "spyType", "spyId", dataGroup);

		dataFactorySpy.MCR.assertParameters("factorRecordGroupFromDataGroup", 0, dataGroup);
		var dataGroupAsRecordGroup = dataFactorySpy.MCR
				.getReturnValue("factorRecordGroupFromDataGroup", 0);
		dependencyProviderSpy.MCR.assertParameters("getRecordTypeHandlerUsingDataRecordGroup", 0,
				dataGroupAsRecordGroup);
	}

	@Test(expectedExceptions = DataException.class, expectedExceptionsMessageRegExp = ""
			+ "Data is not valid: some message")
	public void testValidationTypeDoesNotExist() throws Exception {
		recordTypeHandlerSpy.MRV.setAlwaysThrowException("getUpdateDefinitionId",
				DataValidationException.withMessage("some message"));
		recordTypeHandlerSpy.MRV.setDefaultReturnValuesSupplier("shouldAutoGenerateId", () -> true);
		dependencyProviderSpy.MRV.setDefaultReturnValuesSupplier(
				"getRecordTypeHandlerUsingDataRecordGroup", () -> recordTypeHandlerSpy);
		DataGroup dataGroup = new DataGroupOldSpy("nameInData");
		dataGroup.addChild(DataCreator2.createRecordInfoWithRecordTypeAndRecordIdAndDataDivider(
				"spyType", "spyId", "cora"));

		recordUpdater.updateRecord("someToken78678567", "spyType", "spyId", dataGroup);
	}

	@Test
	public void testCorrectSpiderAuthorizatorForNoRecordPartConstraints() throws Exception {

		recordTypeHandlerSpy.recordPartConstraint = "";
		DataGroup dataGroup = new DataGroupOldSpy("nameInData");

		dataGroup.addChild(DataCreator2.createRecordInfoWithRecordTypeAndRecordIdAndDataDivider(
				"spyType", "spyId", "cora"));
		recordUpdaterOld.updateRecord("someToken78678567", "spyType", "spyId", dataGroup);

		authorizator.MCR.assertParameters(
				"checkGetUsersMatchedRecordPartPermissionsForActionOnRecordTypeAndCollectedData", 0,
				authenticator.returnedUser, "update", "spyType", getPermissionTermUsingCallNo(0),
				false);

		authorizator.MCR.assertParameters(
				"checkUserIsAuthorizedForActionOnRecordTypeAndCollectedData", 0,
				authenticator.returnedUser, "update", "spyType", getPermissionTermUsingCallNo(1));
		authorizator.MCR.assertNumberOfCallsToMethod(
				"checkUserIsAuthorizedForActionOnRecordTypeAndCollectedData", 1);

		dataRedactor.MCR.assertMethodNotCalled("replaceChildrenForConstraintsWithoutPermissions");
		dataRedactor.MCR.assertMethodNotCalled("removeChildrenForConstraintsWithoutPermissions");

		dataValidator.MCR.assertParameter("validateData", 0, "dataGroup", dataGroup);
	}

	@Test
	public void testCorrectSpiderAuthorizatorForWriteRecordPartConstraints() throws Exception {
		recordTypeHandlerSpy.recordPartConstraint = "write";
		DataGroup dataGroup = new DataGroupOldSpy("nameInData");

		dataGroup.addChild(DataCreator2.createRecordInfoWithRecordTypeAndRecordIdAndDataDivider(
				"spyType777", "spyId", "cora"));

		recordUpdaterOld.updateRecord("someToken78678567", "spyType", "spyId", dataGroup);

		authorizator.MCR.assertParameters(
				"checkGetUsersMatchedRecordPartPermissionsForActionOnRecordTypeAndCollectedData", 0,
				authenticator.returnedUser, "update", "spyType", getPermissionTermUsingCallNo(0),
				true);
		authorizator.MCR.assertNumberOfCallsToMethod(
				"checkGetUsersMatchedRecordPartPermissionsForActionOnRecordTypeAndCollectedData",
				1);

		authorizator.MCR.assertParameters(
				"checkUserIsAuthorizedForActionOnRecordTypeAndCollectedData", 0,
				authenticator.returnedUser, "update", "spyType", getPermissionTermUsingCallNo(1));
		authorizator.MCR.assertNumberOfCallsToMethod(
				"checkUserIsAuthorizedForActionOnRecordTypeAndCollectedData", 1);

		dataRedactor.MCR.assertMethodWasCalled("replaceChildrenForConstraintsWithoutPermissions");

		Set<?> expectedPermissions = (Set<?>) authorizator.MCR.getReturnValue(
				"checkGetUsersMatchedRecordPartPermissionsForActionOnRecordTypeAndCollectedData",
				0);
		dataRedactor.MCR.assertParameters("replaceChildrenForConstraintsWithoutPermissions", 0,
				"fakeDefMetadataIdFromRecordTypeHandlerSpy",
				recordStorage.MCR.getReturnValue("read", 0), dataGroup,
				recordTypeHandlerSpy.writeConstraints, expectedPermissions);

		DataGroup returnedRedactedDataGroup = (DataGroup) dataRedactor.MCR
				.getReturnValue("replaceChildrenForConstraintsWithoutPermissions", 0);
		dataValidator.MCR.assertParameter("validateData", 0, "dataGroup",
				returnedRedactedDataGroup);

		// reading updated data
		dataRedactor.MCR.assertMethodNotCalled("removeChildrenForConstraintsWithoutPermissions");
	}

	private List<PermissionTerm> getPermissionTermUsingCallNo(int callNumber) {
		return ((CollectTerms) termCollector.MCR.getReturnValue("collectTerms",
				callNumber)).permissionTerms;
	}

	@Test
	public void testRecordEnhancerCalled() {
		DataGroup dataGroup = new DataGroupOldSpy("nameInData");
		dataGroup.addChild(DataCreator2.createRecordInfoWithRecordTypeAndRecordIdAndDataDivider(
				"spyType", "spyId", "cora"));
		recordUpdaterOld.updateRecord("someToken78678567", "spyType", "spyId", dataGroup);

		dataGroupToRecordEnhancer.MCR.assertParameters("enhance", 0, authenticator.returnedUser,
				"spyType", dataGroup, dataRedactor);
	}

	@Test(expectedExceptions = MisuseException.class, expectedExceptionsMessageRegExp = ""
			+ "Update on abstract recordType: abstract is not allowed")
	public void testUpdateRecordAbstractRecordType() {
		setUpDependencyProvider();
		recordTypeHandlerSpy.MRV.setDefaultReturnValuesSupplier("getRecordTypeId",
				() -> "abstract");
		DataGroup record = new DataGroupOldSpy("abstract");
		recordTypeHandlerSpy.MRV.setDefaultReturnValuesSupplier("shouldAutoGenerateId", () -> true);
		recordTypeHandlerSpy.MRV.setDefaultReturnValuesSupplier("isAbstract", () -> true);

		recordUpdaterOld.updateRecord("someToken78678567", "abstract", "spyId", record);
	}

	@Test
	public void testUpdateRecordAbstractRecordTypeBeforeReadRecord() {
		setUpDependencyProvider();
		DataGroup record = new DataGroupOldSpy("abstract");
		recordTypeHandlerSpy.MRV.setDefaultReturnValuesSupplier("shouldAutoGenerateId", () -> true);
		recordTypeHandlerSpy.MRV.setDefaultReturnValuesSupplier("isAbstract", () -> true);
		try {
			recordUpdaterOld.updateRecord("someToken78678567", "abstract", "spyId", record);
		} catch (Exception e) {
			recordStorage.MCR.assertNumberOfCallsToMethod("read", 0);
		}
	}

	@Test
	public void testCorrectRecordInfoInUpdatedRecord() {
		OldRecordStorageSpy oldRecordStorage = new OldRecordStorageSpy();
		setUpDependencyProvider(oldRecordStorage);
		setUpToReturnFakeDataGroupWhenCreatingUpdatedGroup();
		recordTypeHandlerSpy.MRV.setDefaultReturnValuesSupplier("getRecordTypeId", () -> "spyType");
		DataGroup dataGroup = new DataGroupOldSpy("someRecordDataGroup");
		addRecordInfoWithCreatedInfo(dataGroup);

		DataRecord updatedOnce = recordUpdaterOld.updateRecord("someToken78678567", "spyType",
				"spyId", dataGroup);

		assertUpdatedRepeatIdsInGroupAsListed(updatedOnce, "0");
		assertCorrectUserInfo(updatedOnce);
	}

	private void setUpToReturnFakeDataGroupWhenCreatingUpdatedGroup() {
		DataGroup dataGroup = new DataGroupOldSpy("updated");
		dataFactorySpy.MRV.setSpecificReturnValuesSupplier("factorGroupUsingNameInData",
				() -> dataGroup, "updated");
	}

	private void addRecordInfoWithCreatedInfo(DataGroup topDataGroup) {
		DataGroup recordInfo = DataCreator2.createRecordInfoWithRecordTypeAndRecordIdAndDataDivider(
				"spyType", "spyId", "cora");
		DataLink createdBy = createLinkWithNameInDataRecordTypeAndRecordId("createdBy", "user",
				"6789");
		recordInfo.addChild(createdBy);
		recordInfo.addChild(new se.uu.ub.cora.spider.data.DataAtomicSpy("tsCreated",
				"2016-10-01T00:00:00.000000Z"));
		topDataGroup.addChild(recordInfo);
	}

	private void assertUpdatedRepeatIdsInGroupAsListed(DataRecord updatedRecord,
			String... expectedRepeatIds) {
		DataGroup updatedDataGroup = updatedRecord.getDataGroup();
		DataGroup updatedRecordInfo = updatedDataGroup.getFirstGroupWithNameInData("recordInfo");

		List<DataGroup> updatedGroups = updatedRecordInfo.getAllGroupsWithNameInData("updated");
		AssertJUnit.assertEquals(updatedGroups.size(), expectedRepeatIds.length);

		for (int i = 0; i < expectedRepeatIds.length; i++) {
			DataGroup updatedGroup = updatedGroups.get(i);
			String repeatId = updatedGroup.getRepeatId();
			AssertJUnit.assertEquals(repeatId, expectedRepeatIds[i]);
		}
	}

	private void assertCorrectUserInfo(DataRecord updatedOnce) {
		DataGroup updatedOnceDataGroup = updatedOnce.getDataGroup();
		DataGroup updatedOnceRecordInfo = updatedOnceDataGroup
				.getFirstGroupWithNameInData("recordInfo");
		assertCorrectDataUsingGroupNameInDataAndLinkedRecordId(updatedOnceRecordInfo, "createdBy",
				"4422");

		String tsCreated = updatedOnceRecordInfo.getFirstAtomicValueWithNameInData("tsCreated");
		AssertJUnit.assertTrue(tsCreated.matches(TIMESTAMP_FORMAT));

		DataGroup updated = updatedOnceRecordInfo.getFirstGroupWithNameInData("updated");
		dataFactorySpy.MCR.assertParameters("factorRecordLinkUsingNameInDataAndTypeAndId", 0,
				"updatedBy", "user", "12345");
		var updatedByLink = dataFactorySpy.MCR
				.getReturnValue("factorRecordLinkUsingNameInDataAndTypeAndId", 0);
		assertDataChildFoundInChildren(updatedByLink, updated.getChildren());

		DataAtomicSpy tsUpdatedSpy = (DataAtomicSpy) updated
				.getFirstChildWithNameInData("tsUpdated");
		dataFactorySpy.MCR.assertNumberOfCallsToMethod("factorAtomicUsingNameInDataAndValue", 1);
		dataFactorySpy.MCR.assertReturn("factorAtomicUsingNameInDataAndValue", 0, tsUpdatedSpy);
		dataFactorySpy.MCR.assertParameter("factorAtomicUsingNameInDataAndValue", 0, "nameInData",
				"tsUpdated");
		String tsUpdated = (String) dataFactorySpy.MCR
				.getValueForMethodNameAndCallNumberAndParameterName(
						"factorAtomicUsingNameInDataAndValue", 0, "value");
		assertTrue(tsUpdated.matches(TIMESTAMP_FORMAT));
	}

	private void assertDataChildFoundInChildren(Object createdByLink, List<DataChild> children) {
		boolean createdByAdded = false;
		for (DataChild dataChild : children) {
			if (dataChild == createdByLink) {
				createdByAdded = true;
			}
		}
		AssertJUnit.assertTrue(createdByAdded);
	}

	@Test
	public void testFormatKeepsNanoZeros() throws Exception {
		Instant now = Instant.now();
		int nano = now.getNano();
		Instant minusNanos = now.minusNanos(nano);
		RecordUpdaterImp recordUpdater2 = (RecordUpdaterImp) recordUpdaterOld;
		String formattedTS = recordUpdater2.formatInstantKeepingTrailingZeros(minusNanos);
		AssertJUnit.assertEquals(formattedTS.substring(20), "000000Z");

	}

	private void assertCorrectDataUsingGroupNameInDataAndLinkedRecordId(DataGroup updatedRecordInfo,
			String groupNameInData, String linkedRecordId) {
		DataGroup createdByGroup = updatedRecordInfo.getFirstGroupWithNameInData(groupNameInData);
		AssertJUnit.assertEquals(
				createdByGroup.getFirstAtomicValueWithNameInData("linkedRecordType"), "user");
		AssertJUnit.assertEquals(createdByGroup.getFirstAtomicValueWithNameInData("linkedRecordId"),
				linkedRecordId);
	}

	private DataLink createLinkWithNameInDataRecordTypeAndRecordId(String nameInData,
			String linkedRecordType, String linkedRecordId) {
		DataRecordLinkSpy dataRecordLinkSpy = new DataRecordLinkSpy();
		dataRecordLinkSpy.MRV.setDefaultReturnValuesSupplier("getNameInData", () -> nameInData);
		dataRecordLinkSpy.MRV.setDefaultReturnValuesSupplier("getLinkedRecordId",
				() -> linkedRecordId);
		dataRecordLinkSpy.MRV.setDefaultReturnValuesSupplier("getLinkedRecordType",
				() -> linkedRecordType);
		return dataRecordLinkSpy;
	}

	@Test
	public void testCorrectUpdateInfoWhenRecordHasAlreadyBeenUpdated() {
		RecordStorageUpdateMultipleTimesSpy recordStorage = new RecordStorageUpdateMultipleTimesSpy();
		setUpDependencyProvider(recordStorage);
		setUpToReturnFakeDataGroupWhenCreatingUpdatedGroup();

		DataGroup dataGroup = new DataGroupOldSpy("nameInData");
		addRecordInfoWithCreatedInfo(dataGroup);

		updateStorageToReturnDataGroupIncludingUpdateInfo(recordStorage);
		DataRecord updatedOnce = recordUpdaterOld.updateRecord("someToken78678567", "spyType",
				"spyId", dataGroup);
		AssertJUnit.assertEquals(recordStorage.types.size(), 1);
		assertUpdatedRepeatIdsInGroupAsListed(updatedOnce, "0", "1");

	}

	private void updateStorageToReturnDataGroupIncludingUpdateInfo(
			RecordStorageUpdateMultipleTimesSpy recordStorage) {
		recordStorage.alreadyCalled = true;
	}

	@Test
	public void testNewUpdatedInRecordInfoHasRepeatIdPlusOne() {
		RecordStorageUpdateMultipleTimesSpy recordStorage = new RecordStorageUpdateMultipleTimesSpy();
		setUpDependencyProvider(recordStorage);
		setUpToReturnFakeDataGroupWhenCreatingUpdatedGroup();
		DataGroup dataGroup = new DataGroupOldSpy("nameInData");
		addRecordInfoWithCreatedInfo(dataGroup);

		DataRecord updatedOnceRecord = recordUpdaterOld.updateRecord("someToken78678567", "spyType",
				"spyId", dataGroup);
		assertCorrectUserInfo(updatedOnceRecord);

		updateStorageToReturnDataGroupIncludingUpdateInfo(recordStorage);
		DataRecord updatedTwice = recordUpdaterOld.updateRecord("someToken78678567", "spyType",
				"spyId", dataGroup);
		assertUpdatedRepeatIdsInGroupAsListed(updatedTwice, "0", "1");
	}

	@Test
	public void testCreateAndUpdateInfoSetFromPreviousUpdateIsNotReplacedByAlteredData() {
		RecordStorageUpdateMultipleTimesSpy recordStorage = new RecordStorageUpdateMultipleTimesSpy();
		setUpDependencyProvider(recordStorage);

		DataGroup dataGroupToUpdate = new DataGroupOldSpy("nameInData");
		addRecordInfoWithCreatedInfo(dataGroupToUpdate);
		addUpdatedInfoToRecordInfo(dataGroupToUpdate);

		updateStorageToReturnDataGroupIncludingUpdateInfo(recordStorage);
		DataRecord updatedRecord = recordUpdaterOld.updateRecord("someToken78678567", "spyType",
				"spyId", dataGroupToUpdate);

		DataGroup firstUpdated = getFirstUpdatedInfo(updatedRecord);
		String firstTsUpdated = firstUpdated.getFirstAtomicValueWithNameInData("tsUpdated");
		String correctTsUpdated = "2014-12-18 20:20:38.346";

		AssertJUnit.assertEquals(firstTsUpdated, correctTsUpdated);

		DataGroup updatedDataGroup = updatedRecord.getDataGroup();
		DataGroup recordInfo = updatedDataGroup.getFirstGroupWithNameInData("recordInfo");
		DataGroup createdBy = recordInfo.getFirstGroupWithNameInData("createdBy");
		AssertJUnit.assertEquals(createdBy.getFirstAtomicValueWithNameInData("linkedRecordId"),
				"4422");
		AssertJUnit.assertEquals(recordInfo.getFirstAtomicValueWithNameInData("tsCreated"),
				"2014-08-01T00:00:00.000000Z");
	}

	private void addUpdatedInfoToRecordInfo(DataGroup dataGroup) {
		DataGroup recordInfo = dataGroup.getFirstGroupWithNameInData("recordInfo");
		DataGroup updated = new DataGroupOldSpy("updated");
		updated.setRepeatId("0");
		DataRecordLinkSpy updatedBy = new DataRecordLinkSpy();
		updated.addChild(updatedBy);
		updated.addChild(new se.uu.ub.cora.spider.data.DataAtomicSpy("tsUpdated",
				"2010-12-13 20:20:38.346"));
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
		recordTypeHandlerSpy.MRV.setDefaultReturnValuesSupplier("getRecordTypeId",
				() -> "typeWithAutoGeneratedId");
		DataGroup dataGroup = getDataGroupToUpdate();
		dataGroup.addChild(new se.uu.ub.cora.spider.data.DataAtomicSpy("notStoredValue", "hej"));

		recordUpdaterOld.updateRecord("someToken78678567", "typeWithAutoGeneratedId", "somePlace",
				dataGroup);

		authorizator.MCR.assertParameters(
				"checkGetUsersMatchedRecordPartPermissionsForActionOnRecordTypeAndCollectedData", 0,
				authenticator.returnedUser, "update", "typeWithAutoGeneratedId",
				getPermissionTermUsingCallNo(0), false);

		authorizator.MCR.assertNumberOfCallsToMethod(
				"checkGetUsersMatchedRecordPartPermissionsForActionOnRecordTypeAndCollectedData",
				1);

		authorizator.MCR.assertParameters(
				"checkUserIsAuthorizedForActionOnRecordTypeAndCollectedData", 0,
				authenticator.returnedUser, "update", "typeWithAutoGeneratedId",
				getPermissionTermUsingCallNo(1));
		authorizator.MCR.assertNumberOfCallsToMethod(
				"checkUserIsAuthorizedForActionOnRecordTypeAndCollectedData", 1);
	}

	@Test(expectedExceptions = AuthenticationException.class)
	public void testAuthenticationNotAuthenticated() {
		authenticator.throwAuthenticationException = true;
		DataGroup dataGroup = DataCreator2.createRecordWithNameInDataAndIdAndTypeAndLinkedRecordId(
				"nameInData", "spyId", "spyType", "cora");
		recordUpdaterOld.updateRecord("dummyNonAuthenticatedToken", "spyType", "spyId", dataGroup);
	}

	@Test
	public void testUnauthorizedForUpdateOnRecordTypeShouldNotAccessStorage() {
		authorizator.authorizedForActionAndRecordType = false;

		DataGroup dataGroup = new DataGroupOldSpy("nameInData");
		dataGroup.addChild(DataCreator2.createRecordInfoWithRecordTypeAndRecordIdAndDataDivider(
				"spyType", "spyId", "cora"));

		boolean exceptionWasCaught = false;
		try {
			recordUpdaterOld.updateRecord("someToken78678567", "spyType", "spyId", dataGroup);
		} catch (Exception e) {
			AssertJUnit.assertEquals(e.getClass(), AuthorizationException.class);
			exceptionWasCaught = true;
		}

		AssertJUnit.assertTrue(exceptionWasCaught);
		recordStorage.MCR.assertMethodNotCalled("read");
		recordStorage.MCR.assertMethodNotCalled("update");
		recordStorage.MCR.assertMethodNotCalled("delete");
		recordStorage.MCR.assertMethodNotCalled("create");
	}

	@Test
	public void testExtendedFunctionalityIsCalled() {
		String recordType = "spyType";
		String recordId = "spyId";
		DataGroup dataGroup = DataCreator2.createRecordWithNameInDataAndIdAndTypeAndLinkedRecordId(
				"nameInData", recordId, recordType, "cora");
		String authToken = "someToken78678567";

		recordUpdaterOld.updateRecord(authToken, recordType, recordId, dataGroup);

		ExtendedFunctionalityData expectedData = new ExtendedFunctionalityData();
		expectedData.recordType = recordType;
		expectedData.recordId = recordId;
		expectedData.authToken = authToken;
		expectedData.user = (User) authenticator.MCR.getReturnValue("getUserForToken", 0);
		expectedData.previouslyStoredTopDataGroup = (DataGroup) recordStorage.MCR
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
		recordTypeHandlerSpy.MRV.setDefaultReturnValuesSupplier("getRecordTypeId",
				() -> "typeWithAutoGeneratedId");
		DataGroup dataGroup = getDataGroupToUpdate();

		recordUpdaterOld.updateRecord("someToken78678567", "typeWithAutoGeneratedId", "somePlace",
				dataGroup);

		var recordToSentToStorage = (DataGroup) recordStorage.MCR
				.getValueForMethodNameAndCallNumberAndParameterName("update", 0, "dataRecord");
		var ids = recordTypeHandlerSpy.MCR.getReturnValue("getCombinedIdsUsingRecordId", 0);
		CollectTerms collectTerms = (CollectTerms) termCollector.MCR.getReturnValue("collectTerms",
				1);
		recordIndexer.MCR.assertParameters("indexData", 0, ids, collectTerms.indexTerms,
				recordToSentToStorage);
	}

	@Test
	public void testIndexerIsCalledForChildOfAbstract() {
		recordTypeHandlerSpy.MRV.setDefaultReturnValuesSupplier("getRecordTypeId", () -> "image");
		DataGroup dataGroup = getDataGroupForImageToUpdate();

		recordUpdaterOld.updateRecord("someToken78678567", "image", "someImage", dataGroup);

		var recordToSentToStorage = (DataGroup) recordStorage.MCR
				.getValueForMethodNameAndCallNumberAndParameterName("update", 0, "dataRecord");
		var ids = recordTypeHandlerSpy.MCR.getReturnValue("getCombinedIdsUsingRecordId", 0);
		CollectTerms collectTerms = (CollectTerms) termCollector.MCR.getReturnValue("collectTerms",
				1);
		recordIndexer.MCR.assertParameters("indexData", 0, ids, collectTerms.indexTerms,
				recordToSentToStorage);
	}

	private DataGroup getDataGroupForImageToUpdate() {
		DataGroup dataGroup = new DataGroupOldSpy("binary");
		DataGroup createRecordInfo = DataCreator2
				.createRecordInfoWithIdAndLinkedRecordId("someImage", "cora");
		createRecordInfo.addChild(createLinkWithLinkedId("type", "linkedRecordType", "image"));
		dataGroup.addChild(createRecordInfo);
		dataGroup.addChild(new se.uu.ub.cora.spider.data.DataAtomicSpy("atomicId", "atomicValue"));
		return dataGroup;
	}

	private DataRecordLink createLinkWithLinkedId(String nameInData, String linkedRecordType,
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
		createRecordInfo.addChild(
				createLinkWithLinkedId("type", "linkedRecordType", "typeWithAutoGeneratedId"));

		dataGroup.addChild(createRecordInfo);
		dataGroup.addChild(new se.uu.ub.cora.spider.data.DataAtomicSpy("atomicId", "atomicValue"));
		return dataGroup;
	}

	@Test
	public void testUpdateRecordDataDividerExtractedFromData() {
		recordTypeHandlerSpy.MRV.setDefaultReturnValuesSupplier("getRecordTypeId",
				() -> "typeWithAutoGeneratedId");
		DataGroup dataGroup = getDataGroupToUpdate();

		recordUpdaterOld.updateRecord("someToken78678567", "typeWithAutoGeneratedId", "somePlace",
				dataGroup);

		recordStorage.MCR.assertParameter("update", 0, "dataDivider", "cora");
	}

	@Test
	public void testCreateRecordCollectedTermsSentAlong() {
		recordTypeHandlerSpy.MRV.setDefaultReturnValuesSupplier("getRecordTypeId",
				() -> "typeWithAutoGeneratedId");
		DataGroup dataGroup = getDataGroupToUpdate();

		recordUpdaterOld.updateRecord("someToken78678567", "typeWithAutoGeneratedId", "somePlace",
				dataGroup);

		termCollector.MCR.assertParameter("collectTerms", 1, "metadataId",
				"fakeDefMetadataIdFromRecordTypeHandlerSpy");
		CollectTerms collectTerms = (CollectTerms) termCollector.MCR.getReturnValue("collectTerms",
				1);
		recordStorage.MCR.assertParameter("update", 0, "storageTerms", collectTerms.storageTerms);
	}

	@Test(expectedExceptions = RecordNotFoundException.class)
	public void testUpdateNotFound() {
		recordTypeHandlerSpy.MRV.setDefaultReturnValuesSupplier("getRecordTypeId",
				() -> "recordType");
		recordStorage.MRV.setAlwaysThrowException("read", new RecordNotFoundException("message"));

		DataGroup record = new DataGroupOldSpy("authority");
		DataGroup recordInfo = new DataGroupOldSpy("recordInfo");

		record.addChild(recordInfo);
		recordUpdaterOld.updateRecord("someToken78678567", "recordType", "NOT_FOUND", record);
	}

	@Test(expectedExceptions = DataMissingException.class)
	public void testUpdateRecordRecordInfoMissing() {
		recordTypeHandlerSpy.MRV.setDefaultReturnValuesSupplier("getRecordTypeId",
				() -> "typeWithAutoGeneratedId");
		DataGroup group = new DataGroupOldSpy("authority");
		recordUpdaterOld.updateRecord("someToken78678567", "typeWithAutoGeneratedId", "place",
				group);
	}

	@Test(expectedExceptions = DataMissingException.class)
	public void testUpdateRecordRecordInfoContentMissing() {
		recordTypeHandlerSpy.MRV.setDefaultReturnValuesSupplier("getRecordTypeId",
				() -> "typeWithAutoGeneratedId");
		DataGroup group = new DataGroupOldSpy("authority");
		DataGroup createRecordInfo = new DataGroupOldSpy("recordInfo");
		group.addChild(createRecordInfo);
		recordUpdaterOld.updateRecord("someToken78678567", "typeWithAutoGeneratedId", "place",
				group);
	}

	@Test(expectedExceptions = DataException.class)
	public void testUpdateRecordInvalidData() {
		dataValidator.validValidation = false;

		DataGroup dataGroup = new DataGroupOldSpy("typeWithUserGeneratedId");
		DataGroup createRecordInfo = new DataGroupOldSpy("recordInfo");
		createRecordInfo.addChild(new se.uu.ub.cora.spider.data.DataAtomicSpy("id", "place"));
		createRecordInfo
				.addChild(new se.uu.ub.cora.spider.data.DataAtomicSpy("type", "recordType"));

		dataGroup.addChild(createRecordInfo);
		dataGroup.addChild(new se.uu.ub.cora.spider.data.DataAtomicSpy("atomicId", "atomicValue"));

		recordUpdaterOld.updateRecord("someToken78678567", "typeWithAutoGeneratedId", "place",
				dataGroup);
	}

	@Test(expectedExceptions = DataException.class)
	public void testUpdateRecordIncomingNameInDatasDoNotMatch() {
		DataGroup dataGroup = new DataGroupOldSpy("typeWithUserGeneratedId");
		DataGroup createRecordInfo = new DataGroupOldSpy("recordInfo");
		createRecordInfo.addChild(new se.uu.ub.cora.spider.data.DataAtomicSpy("id", "place"));
		createRecordInfo
				.addChild(new se.uu.ub.cora.spider.data.DataAtomicSpy("type", "recordType"));
		dataGroup.addChild(createRecordInfo);
		dataGroup.addChild(new se.uu.ub.cora.spider.data.DataAtomicSpy("atomicId", "atomicValue"));

		recordUpdaterOld.updateRecord("someToken78678567", "typeWithAutoGeneratedId",
				"place_NOT_THE_SAME", dataGroup);
	}

	@Test(expectedExceptions = DataException.class)
	public void testUpdateRecordIncomingDataTypesDoNotMatch() {
		DataGroup dataGroup = new DataGroupOldSpy("typeWithUserGeneratedId_NOT_THE_SAME");
		DataGroup createRecordInfo = new DataGroupOldSpy("recordInfo");
		createRecordInfo.addChild(new se.uu.ub.cora.spider.data.DataAtomicSpy("id", "place"));
		createRecordInfo.addChild(createLinkWithLinkedId("type", "linkedRecordType", "recordType"));

		dataGroup.addChild(createRecordInfo);
		dataGroup.addChild(new se.uu.ub.cora.spider.data.DataAtomicSpy("atomicId", "atomicValue"));

		recordUpdaterOld.updateRecord("someToken78678567", "typeWithAutoGeneratedId", "place",
				dataGroup);
	}

	@Test(expectedExceptions = DataException.class)
	public void testUpdateRecordIncomingDataIdDoNotMatch() {
		DataGroup dataGroup = new DataGroupOldSpy("typeWithUserGeneratedId_NOT_THE_SAME");
		DataGroup createRecordInfo = new DataGroupOldSpy("recordInfo");
		createRecordInfo.addChild(new se.uu.ub.cora.spider.data.DataAtomicSpy("id", "place"));
		createRecordInfo
				.addChild(new se.uu.ub.cora.spider.data.DataAtomicSpy("type", "recordType"));
		dataGroup.addChild(createRecordInfo);
		dataGroup.addChild(new se.uu.ub.cora.spider.data.DataAtomicSpy("atomicId", "atomicValue"));

		recordUpdaterOld.updateRecord("someToken78678567", "recordType", "placeNOT", dataGroup);
	}

	@Test(expectedExceptions = DataException.class)
	public void testLinkedRecordIdDoesNotExist() {
		Link links = new Link("toType", "toId");
		linkCollector.MRV.setDefaultReturnValuesSupplier("collectLinks", () -> Set.of(links));

		DataGroup dataGroup = RecordLinkTestsDataCreator
				.createDataDataGroupWithRecordInfoAndLinkOneLevelDown();

		recordUpdaterOld.updateRecord("someToken78678567", "dataWithLinks", "oneLinkOneLevelDown",
				dataGroup);
	}

	@Test
	public void testStoreInArchiveTrue() throws Exception {
		recordTypeHandlerSpy.MRV.setDefaultReturnValuesSupplier("storeInArchive", () -> true);
		DataGroupSpy recordSpy = createDataGroupForUpdate();

		recordUpdaterOld.updateRecord("someToken", "spyType", "someRecordId", recordSpy);

		recordArchive.MCR.assertParameters("update", 0, "spyType", "someRecordId", recordSpy);
	}

	private DataGroupSpy createDataGroupForUpdate() {
		DataGroupSpy recordSpy = new DataGroupSpy();
		DataGroupSpy recordInfoSpy = new DataGroupSpy();
		recordSpy.MRV.setSpecificReturnValuesSupplier("getFirstGroupWithNameInData",
				(Supplier<DataGroupSpy>) () -> recordInfoSpy, "recordInfo");
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
		DataGroupSpy recordSpy = createDataGroupForUpdate();

		recordUpdaterOld.updateRecord("someToken", "spyType", "someRecordId", recordSpy);

		recordArchive.MCR.assertMethodNotCalled("update");
	}
}
