/*
 * Copyright 2015, 2016, 2018, 2020, 2021, 2022, 2023, 2024 Uppsala University Library
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
import static org.testng.Assert.assertSame;
import static org.testng.Assert.assertTrue;
import static org.testng.Assert.fail;
import static se.uu.ub.cora.spider.extendedfunctionality.ExtendedFunctionalityPosition.UPDATE_AFTER_AUTHORIZATION;
import static se.uu.ub.cora.spider.extendedfunctionality.ExtendedFunctionalityPosition.UPDATE_AFTER_METADATA_VALIDATION;
import static se.uu.ub.cora.spider.extendedfunctionality.ExtendedFunctionalityPosition.UPDATE_AFTER_STORE;
import static se.uu.ub.cora.spider.extendedfunctionality.ExtendedFunctionalityPosition.UPDATE_BEFORE_METADATA_VALIDATION;
import static se.uu.ub.cora.spider.extendedfunctionality.ExtendedFunctionalityPosition.UPDATE_BEFORE_RETURN;
import static se.uu.ub.cora.spider.extendedfunctionality.ExtendedFunctionalityPosition.UPDATE_BEFORE_STORE;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.function.Supplier;

import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import se.uu.ub.cora.beefeater.authentication.User;
import se.uu.ub.cora.bookkeeper.recordtype.Unique;
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
import se.uu.ub.cora.data.collected.StorageTerm;
import se.uu.ub.cora.data.copier.DataCopierFactory;
import se.uu.ub.cora.data.copier.DataCopierProvider;
import se.uu.ub.cora.data.spies.DataAtomicSpy;
import se.uu.ub.cora.data.spies.DataFactorySpy;
import se.uu.ub.cora.data.spies.DataGroupSpy;
import se.uu.ub.cora.data.spies.DataRecordLinkSpy;
import se.uu.ub.cora.logger.LoggerProvider;
import se.uu.ub.cora.spider.authentication.OldAuthenticatorSpy;
import se.uu.ub.cora.spider.authorization.AuthorizationException;
import se.uu.ub.cora.spider.authorization.PermissionRuleCalculator;
import se.uu.ub.cora.spider.data.DataAtomicOldSpy;
import se.uu.ub.cora.spider.data.DataGroupOldSpy;
import se.uu.ub.cora.spider.data.DataMissingException;
import se.uu.ub.cora.spider.data.DataRecordOldSpy;
import se.uu.ub.cora.spider.dependency.spy.RecordTypeHandlerSpy;
import se.uu.ub.cora.spider.dependency.spy.SpiderDependencyProviderOldSpy;
import se.uu.ub.cora.spider.extendedfunctionality.ExtendedFunctionalityData;
import se.uu.ub.cora.spider.extendedfunctionality.ExtendedFunctionalitySpy;
import se.uu.ub.cora.spider.extendedfunctionality.internal.ExtendedFunctionalityProviderSpy;
import se.uu.ub.cora.spider.log.LoggerFactorySpy;
import se.uu.ub.cora.spider.record.ConflictException;
import se.uu.ub.cora.spider.record.DataCopierFactorySpy;
import se.uu.ub.cora.spider.record.DataException;
import se.uu.ub.cora.spider.record.DataGroupToRecordEnhancerSpy;
import se.uu.ub.cora.spider.record.DataRedactorSpy;
import se.uu.ub.cora.spider.record.RecordUpdater;
import se.uu.ub.cora.spider.spy.DataGroupTermCollectorSpy;
import se.uu.ub.cora.spider.spy.DataRecordLinkCollectorSpy;
import se.uu.ub.cora.spider.spy.DataValidatorOldSpy;
import se.uu.ub.cora.spider.spy.DataValidatorSpy;
import se.uu.ub.cora.spider.spy.OldRecordStorageSpy;
import se.uu.ub.cora.spider.spy.OldSpiderAuthorizatorSpy;
import se.uu.ub.cora.spider.spy.RecordArchiveSpy;
import se.uu.ub.cora.spider.spy.RecordIndexerSpy;
import se.uu.ub.cora.spider.spy.RecordStorageUpdateMultipleTimesSpy;
import se.uu.ub.cora.spider.spy.RuleCalculatorSpy;
import se.uu.ub.cora.spider.spy.SpiderDependencyProviderSpy;
import se.uu.ub.cora.spider.spy.UniqueValidatorSpy;
import se.uu.ub.cora.spider.spy.ValidationAnswerSpy;
import se.uu.ub.cora.spider.testdata.DataCreator2;
import se.uu.ub.cora.spider.testdata.RecordLinkTestsDataCreator;
import se.uu.ub.cora.storage.RecordNotFoundException;
import se.uu.ub.cora.storage.RecordStorage;
import se.uu.ub.cora.storage.spies.RecordStorageSpy;

public class RecordUpdaterTest {
	private static final String AUTH_TOKEN = "someAuthToken";
	private static final String RECORD_ID = "someRecordId";
	private static final String RECORD_TYPE = "spyType";
	private static final String TIMESTAMP_FORMAT = "\\d{4}-\\d{2}-\\d{2}T\\d{2}:\\d{2}:\\d{2}\\.\\d{6}Z";
	private RecordStorageSpy recordStorage;
	private RecordArchiveSpy recordArchive;
	private OldAuthenticatorSpy authenticator;
	private OldSpiderAuthorizatorSpy authorizator;
	private PermissionRuleCalculator ruleCalculator;
	private RecordUpdater recordUpdaterOld;
	private RecordUpdater recordUpdater;
	private DataValidatorOldSpy dataValidator;
	private DataRecordLinkCollectorSpy linkCollector;
	private SpiderDependencyProviderOldSpy dependencyProviderOld;
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
	private UniqueValidatorSpy uniqueValidator;

	@BeforeMethod
	public void beforeMethod() {
		setUpFactoriesAndProviders();
		authenticator = new OldAuthenticatorSpy();
		authorizator = new OldSpiderAuthorizatorSpy();
		dataValidator = new DataValidatorOldSpy();
		recordStorage = new RecordStorageSpy();
		ruleCalculator = new RuleCalculatorSpy();
		linkCollector = new DataRecordLinkCollectorSpy();
		termCollector = new DataGroupTermCollectorSpy();
		recordIndexer = new RecordIndexerSpy();
		dataRedactor = new DataRedactorSpy();
		extendedFunctionalityProvider = new ExtendedFunctionalityProviderSpy();
		recordArchive = new RecordArchiveSpy();
		uniqueValidator = new UniqueValidatorSpy();

		setUpDependencyProviderOld();
		setUpDependencyProvider();

		setUpToReturnFakeDataForUpdatedTS();
	}

	private void setUpFactoriesAndProviders() {
		loggerFactorySpy = new LoggerFactorySpy();
		LoggerProvider.setLoggerFactory(loggerFactorySpy);
		dataFactorySpy = new DataFactorySpy();
		DataProvider.onlyForTestSetDataFactory(dataFactorySpy);

		dataCopierFactory = new DataCopierFactorySpy();
		DataCopierProvider.setDataCopierFactory(dataCopierFactory);
	}

	private void setUpDependencyProviderOld() {
		setUpDependencyProviderOldWithRecordStorage(recordStorage);
	}

	private void setUpDependencyProviderOldWithRecordStorage(RecordStorage recordStorage) {
		dependencyProviderOld = new SpiderDependencyProviderOldSpy();
		dependencyProviderOld.authenticator = authenticator;
		dependencyProviderOld.spiderAuthorizator = authorizator;
		dependencyProviderOld.dataValidator = dataValidator;
		dependencyProviderOld.recordStorage = recordStorage;
		dependencyProviderOld.ruleCalculator = ruleCalculator;
		dependencyProviderOld.linkCollector = linkCollector;
		dependencyProviderOld.extendedFunctionalityProvider = extendedFunctionalityProvider;
		dependencyProviderOld.termCollector = termCollector;
		dependencyProviderOld.recordIndexer = recordIndexer;
		dependencyProviderOld.recordArchive = recordArchive;
		dataGroupToRecordEnhancer = new DataGroupToRecordEnhancerSpy();
		recordTypeHandlerSpy = dependencyProviderOld.recordTypeHandlerSpy;
		recordTypeHandlerSpy.MRV.setDefaultReturnValuesSupplier("getRecordTypeId",
				() -> RECORD_TYPE);
		dependencyProviderOld.dataRedactor = dataRedactor;

		recordUpdaterOld = RecordUpdaterImp.usingDependencyProviderAndDataGroupToRecordEnhancer(
				dependencyProviderOld, dataGroupToRecordEnhancer);
	}

	private void setUpDependencyProvider() {
		dependencyProviderSpy = new SpiderDependencyProviderSpy();
		dependencyProviderSpy.MRV.setDefaultReturnValuesSupplier(
				"getRecordTypeHandlerUsingDataRecordGroup", () -> recordTypeHandlerSpy);
		dependencyProviderSpy.MRV.setDefaultReturnValuesSupplier("getRecordStorage",
				() -> recordStorage);
		dependencyProviderSpy.MRV.setDefaultReturnValuesSupplier("getExtendedFunctionalityProvider",
				() -> extendedFunctionalityProvider);
		dependencyProviderSpy.MRV.setDefaultReturnValuesSupplier("getUniqueValidator",
				() -> uniqueValidator);
		dependencyProviderSpy.MRV.setDefaultReturnValuesSupplier("getDataGroupTermCollector",
				() -> termCollector);

		recordUpdater = RecordUpdaterImp.usingDependencyProviderAndDataGroupToRecordEnhancer(
				dependencyProviderSpy, dataGroupToRecordEnhancer);
	}

	private void setUpToReturnFakeDataForUpdatedTS() {
		DataAtomicSpy atomicTS = new DataAtomicSpy();
		dataFactorySpy.MRV.setDefaultReturnValuesSupplier("factorAtomicUsingNameInDataAndValue",
				() -> atomicTS);
		atomicTS.MRV.setDefaultReturnValuesSupplier("getNameInData", () -> "tsUpdated");
		atomicTS.MRV.setDefaultReturnValuesSupplier("getValue",
				() -> "2018-10-01T00:00:00.000000Z");
	}

	@Test
	public void testExternalDependenciesAreCalled() {
		DataGroup dataGroup = new DataGroupOldSpy("nameInData");

		dataGroup.addChild(DataCreator2.createRecordInfoWithRecordTypeAndRecordIdAndDataDivider(
				RECORD_TYPE, "spyId", "cora"));
		recordUpdaterOld.updateRecord(AUTH_TOKEN, RECORD_TYPE, "spyId", dataGroup);

		authorizator.MCR.assertMethodWasCalled("checkUserIsAuthorizedForActionOnRecordType");
		dataValidator.MCR.assertMethodWasCalled("validateData");
		dataValidator.MCR.assertParameters("validateData", 0,
				"fakeUpdateMetadataIdFromRecordTypeHandlerSpy", dataGroup);

		linkCollector.MCR.assertParameters("collectLinks", 0,
				"fakeDefMetadataIdFromRecordTypeHandlerSpy", dataGroup);

		CollectTerms collectedTerms = (CollectTerms) termCollector.MCR
				.getReturnValue("collectTerms", 1);

		var links = linkCollector.MCR.getReturnValue("collectLinks", 0);

		recordStorage.MCR.assertParameters("update", 0, RECORD_TYPE, "spyId", dataGroup,
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

		recordUpdaterOld.updateRecord(AUTH_TOKEN, RECORD_TYPE, "spyId", dataGroupSpy);
	}

	@Test
	public void testRecordTypeHandlerFetchedFromDependencyProvider() {
		dependencyProviderSpy.MRV.setDefaultReturnValuesSupplier(
				"getRecordTypeHandlerUsingDataRecordGroup", () -> recordTypeHandlerSpy);
		DataGroup dataGroup = new DataGroupOldSpy("nameInData");
		dataGroup.addChild(DataCreator2.createRecordInfoWithRecordTypeAndRecordIdAndDataDivider(
				RECORD_TYPE, "spyId", "cora"));

		recordUpdater.updateRecord(AUTH_TOKEN, RECORD_TYPE, "spyId", dataGroup);

		dataFactorySpy.MCR.assertParameters("factorRecordGroupFromDataGroup", 0, dataGroup);
		var dataGroupAsRecordGroup = dataFactorySpy.MCR
				.getReturnValue("factorRecordGroupFromDataGroup", 0);
		dependencyProviderSpy.MCR.assertParameters("getRecordTypeHandlerUsingDataRecordGroup", 0,
				dataGroupAsRecordGroup);
	}

	@Test
	public void testValidationTypeDoesNotExist() throws Exception {
		recordTypeHandlerSpy.MRV.setAlwaysThrowException("getUpdateDefinitionId",
				DataValidationException.withMessage("some message"));
		dependencyProviderSpy.MRV.setDefaultReturnValuesSupplier(
				"getRecordTypeHandlerUsingDataRecordGroup", () -> recordTypeHandlerSpy);
		DataGroup dataGroup = new DataGroupOldSpy("nameInData");
		dataGroup.addChild(DataCreator2.createRecordInfoWithRecordTypeAndRecordIdAndDataDivider(
				RECORD_TYPE, "spyId", "cora"));

		try {
			recordUpdater.updateRecord(AUTH_TOKEN, RECORD_TYPE, "spyId", dataGroup);
			fail("Exception should have been thrown");
		} catch (Exception e) {
			assertTrue(e instanceof DataException);
			assertEquals(e.getMessage(), "Data is not valid: some message");
		}
	}

	@Test
	public void testIgnoreOverwriteProtection_removedFromRecordInfo() throws Exception {
		String ignoreOverwriteProtectionValue = "true";
		ExtendedFunctionalitySpy extendedFunctionalitySpy = new ExtendedFunctionalitySpy();
		DataGroup dataGroup = setUpRecordUpdaterWithExtFunctionallityAndValue(
				Optional.of(ignoreOverwriteProtectionValue), extendedFunctionalitySpy,
				Optional.of("2023-12-05T08:51:01.730741Z"));

		recordUpdater.updateRecord(AUTH_TOKEN, RECORD_TYPE, "spyId", dataGroup);

		assertIgnoreOverwriteProtectionDeletedFromTopDataGroup(extendedFunctionalitySpy);
	}

	@Test
	public void testIgnoreOverwriteProtection_DifferntLatestUpdated_update() throws Exception {
		String ignoreOverwriteProtectionValue = "true";
		ExtendedFunctionalitySpy extendedFunctionalitySpy = new ExtendedFunctionalitySpy();

		setupPreviouslyStoredRecord("2020-01-01T00:00:00.000001Z");

		DataGroup dataGroup = setUpRecordUpdaterWithExtFunctionallityAndValue(
				Optional.of(ignoreOverwriteProtectionValue), extendedFunctionalitySpy,
				Optional.of("2023-12-05T08:51:01.730741Z"));

		recordUpdater.updateRecord(AUTH_TOKEN, RECORD_TYPE, "spyId", dataGroup);

		assertIgnoreOverwriteProtectionDeletedFromTopDataGroup(extendedFunctionalitySpy);
	}

	@Test
	public void testIgnoreOverwriteProtection_SameLatestUpdated_update() throws Exception {
		String ignoreOverwriteProtectionValue = "false";
		String sameLatestUpdatedTimestamp = "2020-01-01T00:00:00.000001Z";
		ExtendedFunctionalitySpy extendedFunctionalitySpy = new ExtendedFunctionalitySpy();

		setupPreviouslyStoredRecord(sameLatestUpdatedTimestamp);
		DataGroup dataGroup = setUpRecordUpdaterWithExtFunctionallityAndValue(
				Optional.of(ignoreOverwriteProtectionValue), extendedFunctionalitySpy,
				Optional.of(sameLatestUpdatedTimestamp));

		recordUpdater.updateRecord(AUTH_TOKEN, RECORD_TYPE, "spyId", dataGroup);

		assertIgnoreOverwriteProtectionDeletedFromTopDataGroup(extendedFunctionalitySpy);
	}

	@Test
	public void testIgnoreOverwriteProtection_SameLatestUpdated_noIgnoreFlag_update()
			throws Exception {
		String sameLatestUpdatedTimestamp = "2020-01-01T00:00:00.000001Z";
		ExtendedFunctionalitySpy extendedFunctionalitySpy = new ExtendedFunctionalitySpy();

		setupPreviouslyStoredRecord(sameLatestUpdatedTimestamp);
		DataGroup dataGroup = setUpRecordUpdaterWithExtFunctionallityAndValue(Optional.empty(),
				extendedFunctionalitySpy, Optional.of(sameLatestUpdatedTimestamp));

		recordUpdater.updateRecord(AUTH_TOKEN, RECORD_TYPE, "spyId", dataGroup);

		assertIgnoreOverwriteProtectionDeletedFromTopDataGroup(extendedFunctionalitySpy);
	}

	@Test
	public void testIgnoreOverwriteProtection_DifferntLatestUpdated_noIgnoreFlag_ConflictException()
			throws Exception {
		ExtendedFunctionalitySpy extendedFunctionalitySpy = new ExtendedFunctionalitySpy();

		setupPreviouslyStoredRecord("2020-01-01T00:00:00.000001Z");
		DataGroup dataGroup = setUpRecordUpdaterWithExtFunctionallityAndValue(Optional.empty(),
				extendedFunctionalitySpy, Optional.of("2023-01-01T00:00:00.000001Z"));

		try {
			recordUpdater.updateRecord(AUTH_TOKEN, RECORD_TYPE, "spyId", dataGroup);
			fail();
		} catch (Exception e) {
			System.err.println(e.getMessage());
			assertTrue(e instanceof ConflictException);
			assertEquals(e.getMessage(),
					"Could not update record because it exist a newer version of the "
							+ "record in the storage.");
		}
	}

	private void assertIgnoreOverwriteProtectionDeletedFromTopDataGroup(
			ExtendedFunctionalitySpy extendedFunctionalitySpy) {
		ExtendedFunctionalityData extData = (ExtendedFunctionalityData) extendedFunctionalitySpy.MCR
				.getValueForMethodNameAndCallNumberAndParameterName("useExtendedFunctionality", 0,
						"data");
		DataGroup recordInfoHandled = extData.dataGroup.getFirstGroupWithNameInData("recordInfo");
		assertFalse(recordInfoHandled.containsChildWithNameInData("ignoreOverwriteProtection"));
	}

	@Test
	public void testIgnoreOverwriteProtection_DiffrentLatestUpdated_ConflictException()
			throws Exception {
		String ignoreOverwriteProtectionValue = "false";
		ExtendedFunctionalitySpy extendedFunctionalitySpy = new ExtendedFunctionalitySpy();

		setupPreviouslyStoredRecord("2020-01-01T00:00:00.000001Z");
		DataGroup dataGroup = setUpRecordUpdaterWithExtFunctionallityAndValue(
				Optional.of(ignoreOverwriteProtectionValue), extendedFunctionalitySpy,
				Optional.of("2023-12-05T08:51:01.730741Z"));

		try {
			recordUpdater.updateRecord(AUTH_TOKEN, RECORD_TYPE, "spyId", dataGroup);
			fail();
		} catch (Exception e) {
			System.err.println(e.getMessage());
			assertTrue(e instanceof ConflictException);
			assertEquals(e.getMessage(),
					"Could not update record because it exist a newer version of the "
							+ "record in the storage.");
		}
	}

	@Test
	public void testIgnoreOverwriteProtection_MissingTSLatestUpdated_ConflictException()
			throws Exception {
		// String ignoreOverwriteProtectionValue = "false";
		ExtendedFunctionalitySpy extendedFunctionalitySpy = new ExtendedFunctionalitySpy();

		setupPreviouslyStoredRecord("2020-01-01T00:00:00.000001Z");
		DataGroup dataGroup = setUpRecordUpdaterWithExtFunctionallityAndValue(Optional.empty(),
				extendedFunctionalitySpy, Optional.empty());

		try {
			recordUpdater.updateRecord(AUTH_TOKEN, RECORD_TYPE, "spyId", dataGroup);
			fail();
		} catch (Exception e) {
			System.err.println(e.getMessage());
			assertTrue(e instanceof ConflictException);
			assertEquals(e.getMessage(),
					"Could not update record because it exist a newer version of the "
							+ "record in the storage.");
		}
	}

	private void setupPreviouslyStoredRecord(String updatedTimestamp) {
		DataGroup dataGroupStored = new DataGroupOldSpy("nameInData");
		DataGroup recordInfo = DataCreator2.createRecordInfoWithRecordTypeAndRecordIdAndDataDivider(
				RECORD_TYPE, "spyId", "cora");

		dataGroupStored.addChild(recordInfo);
		DataGroupSpy createdG = new DataGroupSpy();
		createdG.MRV.setDefaultReturnValuesSupplier("getNameInData", () -> "createdBy");
		recordInfo.addChild(createdG);

		DataAtomicSpy tsCreated = new DataAtomicSpy();
		tsCreated.MRV.setDefaultReturnValuesSupplier("getNameInData", () -> "tsCreated");
		recordInfo.addChild(tsCreated);
		//
		DataGroupSpy updatedG = new DataGroupSpy();
		updatedG.MRV.setDefaultReturnValuesSupplier("containsChildWithNameInData", () -> true);
		updatedG.MRV.setDefaultReturnValuesSupplier("getNameInData", () -> "updated");
		updatedG.MRV.setDefaultReturnValuesSupplier("getFirstAtomicValueWithNameInData",
				() -> updatedTimestamp);
		updatedG.MRV.setDefaultReturnValuesSupplier("getRepeatId", () -> "1");
		recordInfo.addChild(updatedG);

		recordStorage.MRV.setDefaultReturnValuesSupplier("read", () -> dataGroupStored);
	}

	private DataGroupSpy setUpRecordUpdaterWithExtFunctionallityAndValue(
			Optional<String> ignoreOverwriteProtectionValue,
			ExtendedFunctionalitySpy extendedFunctionalitySpy, Optional<String> updatedTimestamp) {
		extendedFunctionalityProvider.MRV.setSpecificReturnValuesSupplier(
				"getFunctionalityForPositionAndRecordType", () -> List.of(extendedFunctionalitySpy),
				UPDATE_BEFORE_METADATA_VALIDATION, RECORD_TYPE);

		recordUpdater = RecordUpdaterImp.usingDependencyProviderAndDataGroupToRecordEnhancer(
				dependencyProviderSpy, dataGroupToRecordEnhancer);

		DataGroupSpy dataGroup = createRecordWithRecordInfo(ignoreOverwriteProtectionValue,
				updatedTimestamp);
		return dataGroup;
	}

	private DataGroupSpy createRecordWithRecordInfo(Optional<String> ignoreOverwriteProtectionValue,
			Optional<String> updatedTimestamp) {
		DataGroup recordInfo = DataCreator2.createRecordInfoWithRecordTypeAndRecordIdAndDataDivider(
				RECORD_TYPE, "spyId", "cora");

		DataGroupSpy dataGroup = new DataGroupSpy();
		dataGroup.MRV.setDefaultReturnValuesSupplier("getNameInData", () -> "nameInData");
		dataGroup.MRV.setSpecificReturnValuesSupplier("getFirstGroupWithNameInData",
				() -> recordInfo, "recordInfo");

		setIgnoreOverwriteProtection(ignoreOverwriteProtectionValue, recordInfo);

		DataGroupSpy updatedG = new DataGroupSpy();
		updatedG.MRV.setDefaultReturnValuesSupplier("getNameInData", () -> "updated");
		if (updatedTimestamp.isPresent()) {
			updatedG.MRV.setSpecificReturnValuesSupplier("containsChildWithNameInData", () -> true,
					"tsUpdated");
			updatedG.MRV.setSpecificReturnValuesSupplier("getFirstAtomicValueWithNameInData",
					() -> updatedTimestamp.get(), "tsUpdated");
		} else {
			updatedG.MRV.setThrowException("getFirstAtomicValueWithNameInData",
					new DataMissingException("spy value not found"), "tsUpdated");
		}
		updatedG.MRV.setDefaultReturnValuesSupplier("getRepeatId", () -> "1");

		recordInfo.addChild(updatedG);
		return dataGroup;
	}

	private void setIgnoreOverwriteProtection(Optional<String> ignoreOverwriteProtectionValue,
			DataGroup recordInfo) {
		if (ignoreOverwriteProtectionValue.isPresent()) {
			DataAtomicSpy ignoreOverwriteProtection = createAtomicForOverwriteProtection(
					ignoreOverwriteProtectionValue.get());
			recordInfo.addChild(ignoreOverwriteProtection);
		}
	}

	private DataAtomicSpy createAtomicForOverwriteProtection(
			String ignoreOverwriteProtectionValue) {
		DataAtomicSpy ignoreOverwriteProtection = new DataAtomicSpy();
		ignoreOverwriteProtection.MRV.setDefaultReturnValuesSupplier("getNameInData",
				() -> "ignoreOverwriteProtection");
		ignoreOverwriteProtection.MRV.setDefaultReturnValuesSupplier("getValue",
				() -> ignoreOverwriteProtectionValue);
		return ignoreOverwriteProtection;
	}

	@Test
	public void testCorrectSpiderAuthorizatorForNoRecordPartConstraints() throws Exception {
		recordTypeHandlerSpy.recordPartConstraint = "";
		DataGroup dataGroup = new DataGroupOldSpy("nameInData");

		dataGroup.addChild(DataCreator2.createRecordInfoWithRecordTypeAndRecordIdAndDataDivider(
				RECORD_TYPE, "spyId", "cora"));
		recordUpdaterOld.updateRecord(AUTH_TOKEN, RECORD_TYPE, "spyId", dataGroup);

		authorizator.MCR.assertParameters(
				"checkGetUsersMatchedRecordPartPermissionsForActionOnRecordTypeAndCollectedData", 0,
				authenticator.returnedUser, "update", RECORD_TYPE, getPermissionTermUsingCallNo(0),
				false);

		authorizator.MCR.assertParameters(
				"checkUserIsAuthorizedForActionOnRecordTypeAndCollectedData", 0,
				authenticator.returnedUser, "update", RECORD_TYPE, getPermissionTermUsingCallNo(1));
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

		recordUpdaterOld.updateRecord(AUTH_TOKEN, RECORD_TYPE, "spyId", dataGroup);

		authorizator.MCR.assertParameters(
				"checkGetUsersMatchedRecordPartPermissionsForActionOnRecordTypeAndCollectedData", 0,
				authenticator.returnedUser, "update", RECORD_TYPE, getPermissionTermUsingCallNo(0),
				true);
		authorizator.MCR.assertNumberOfCallsToMethod(
				"checkGetUsersMatchedRecordPartPermissionsForActionOnRecordTypeAndCollectedData",
				1);

		authorizator.MCR.assertParameters(
				"checkUserIsAuthorizedForActionOnRecordTypeAndCollectedData", 0,
				authenticator.returnedUser, "update", RECORD_TYPE, getPermissionTermUsingCallNo(1));
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
				RECORD_TYPE, "spyId", "cora"));
		recordUpdaterOld.updateRecord(AUTH_TOKEN, RECORD_TYPE, "spyId", dataGroup);

		dataGroupToRecordEnhancer.MCR.assertParameters("enhance", 0, authenticator.returnedUser,
				RECORD_TYPE, dataGroup, dataRedactor);
	}

	@Test
	public void testCorrectRecordInfoInUpdatedRecord() {
		OldRecordStorageSpy oldRecordStorage = new OldRecordStorageSpy();
		setUpDependencyProviderOldWithRecordStorage(oldRecordStorage);
		setUpToReturnFakeDataGroupWhenCreatingUpdatedGroup();
		recordTypeHandlerSpy.MRV.setDefaultReturnValuesSupplier("getRecordTypeId",
				() -> RECORD_TYPE);
		DataGroup dataGroup = new DataGroupOldSpy("someRecordDataGroup");
		addRecordInfoWithCreatedInfo(dataGroup);

		DataRecord updatedOnce = recordUpdaterOld.updateRecord(AUTH_TOKEN, RECORD_TYPE, "spyId",
				dataGroup);

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
				RECORD_TYPE, "spyId", "cora");
		DataLink createdBy = createLinkWithNameInDataRecordTypeAndRecordId("createdBy", "user",
				"6789");
		recordInfo.addChild(createdBy);
		recordInfo.addChild(new DataAtomicOldSpy("tsCreated", "2016-10-01T00:00:00.000000Z"));

		setIgnoreOverwriteProtection(Optional.of("true"), recordInfo);

		DataGroupSpy updatedG = new DataGroupSpy();
		updatedG.MRV.setDefaultReturnValuesSupplier("getNameInData", () -> "updated");
		updatedG.MRV.setDefaultReturnValuesSupplier("getFirstAtomicValueWithNameInData",
				() -> "2018-10-01T00:00:00.000000Z");
		updatedG.MRV.setDefaultReturnValuesSupplier("getRepeatId", () -> "1");
		recordInfo.addChild(updatedG);

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
		assertTrue(createdByAdded);
	}

	@Test
	public void testFormatKeepsNanoZeros() throws Exception {
		Instant now = Instant.now();
		int nano = now.getNano();
		Instant minusNanos = now.minusNanos(nano);
		RecordUpdaterImp recordUpdater2 = (RecordUpdaterImp) recordUpdaterOld;
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
		setUpDependencyProviderOldWithRecordStorage(recordStorage);
		setUpToReturnFakeDataGroupWhenCreatingUpdatedGroup();

		DataGroup dataGroup = new DataGroupOldSpy("nameInData");
		addRecordInfoWithCreatedInfo(dataGroup);

		updateStorageToReturnDataGroupIncludingUpdateInfo(recordStorage);
		DataRecord updatedOnce = recordUpdaterOld.updateRecord(AUTH_TOKEN, RECORD_TYPE, "spyId",
				dataGroup);
		assertEquals(recordStorage.types.size(), 1);
		assertUpdatedRepeatIdsInGroupAsListed(updatedOnce, "0", "1");
	}

	private void updateStorageToReturnDataGroupIncludingUpdateInfo(
			RecordStorageUpdateMultipleTimesSpy recordStorage) {
		recordStorage.alreadyCalled = true;
	}

	@Test
	public void testNewUpdatedInRecordInfoHasRepeatIdPlusOne() {
		RecordStorageUpdateMultipleTimesSpy recordStorage = new RecordStorageUpdateMultipleTimesSpy();

		setUpDependencyProviderOldWithRecordStorage(recordStorage);
		setUpToReturnFakeDataGroupWhenCreatingUpdatedGroup();
		DataGroup dataGroup = new DataGroupOldSpy("nameInData");
		addRecordInfoWithCreatedInfo(dataGroup);

		DataRecord updatedOnceRecord = recordUpdaterOld.updateRecord(AUTH_TOKEN, RECORD_TYPE,
				"spyId", dataGroup);
		assertCorrectUserInfo(updatedOnceRecord);

		updateStorageToReturnDataGroupIncludingUpdateInfo(recordStorage);
		resetIgnoreOverwriteProtectionToTrueAsItWasRemovedByTheServer(dataGroup);
		DataRecord updatedTwice = recordUpdaterOld.updateRecord(AUTH_TOKEN, RECORD_TYPE, "spyId",
				dataGroup);
		assertUpdatedRepeatIdsInGroupAsListed(updatedTwice, "0", "1");
	}

	private void resetIgnoreOverwriteProtectionToTrueAsItWasRemovedByTheServer(
			DataGroup dataGroup) {
		dataGroup.getFirstGroupWithNameInData("recordInfo")
				.addChild(createAtomicForOverwriteProtection("true"));
	}

	@Test
	public void testCreateAndUpdateInfoSetFromPreviousUpdateIsNotReplacedByAlteredData() {
		RecordStorageUpdateMultipleTimesSpy recordStorage = new RecordStorageUpdateMultipleTimesSpy();
		setUpDependencyProviderOldWithRecordStorage(recordStorage);

		DataGroup dataGroupToUpdate = new DataGroupOldSpy("nameInData");
		addRecordInfoWithCreatedInfo(dataGroupToUpdate);
		addUpdatedInfoToRecordInfo(dataGroupToUpdate);

		updateStorageToReturnDataGroupIncludingUpdateInfo(recordStorage);
		DataRecord updatedRecord = recordUpdaterOld.updateRecord(AUTH_TOKEN, RECORD_TYPE, "spyId",
				dataGroupToUpdate);

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
		DataRecordLinkSpy updatedBy = new DataRecordLinkSpy();
		updated.addChild(updatedBy);
		updated.addChild(new DataAtomicOldSpy("tsUpdated", "2014-12-18 20:20:38.346"));
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
		dataGroup.addChild(new DataAtomicOldSpy("notStoredValue", "hej"));

		recordUpdaterOld.updateRecord(AUTH_TOKEN, "typeWithAutoGeneratedId", "somePlace",
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

	@Test
	public void testUnauthorizedForUpdateOnRecordTypeShouldNotAccessStorage() {
		authorizator.authorizedForActionAndRecordType = false;

		DataGroup dataGroup = new DataGroupOldSpy("nameInData");
		dataGroup.addChild(DataCreator2.createRecordInfoWithRecordTypeAndRecordIdAndDataDivider(
				RECORD_TYPE, "spyId", "cora"));

		try {
			recordUpdaterOld.updateRecord(AUTH_TOKEN, RECORD_TYPE, "spyId", dataGroup);
			fail("AuthorizationException should have been thrown");
		} catch (Exception e) {
			assertTrue(e instanceof AuthorizationException);
		}

		recordStorage.MCR.assertMethodNotCalled("read");
		recordStorage.MCR.assertMethodNotCalled("update");
		recordStorage.MCR.assertMethodNotCalled("delete");
		recordStorage.MCR.assertMethodNotCalled("create");
	}

	@Test
	public void testExtendedFunctionalityIsCalled() {
		String recordType = RECORD_TYPE;
		String recordId = "spyId";
		DataGroup dataGroup = DataCreator2.createRecordWithNameInDataAndIdAndTypeAndLinkedRecordId(
				"nameInData", recordId, recordType, "cora");
		String authToken = AUTH_TOKEN;

		recordUpdaterOld.updateRecord(authToken, recordType, recordId, dataGroup);

		ExtendedFunctionalityData expectedData = new ExtendedFunctionalityData();
		expectedData.recordType = recordType;
		expectedData.recordId = recordId;
		expectedData.authToken = authToken;
		expectedData.user = (User) authenticator.MCR.getReturnValue("getUserForToken", 0);
		expectedData.dataGroup = dataGroup;

		extendedFunctionalityProvider.assertCallToPositionAndFunctionalityCalledWithData(
				UPDATE_AFTER_AUTHORIZATION, expectedData, 0);

		expectedData.previouslyStoredTopDataGroup = (DataGroup) recordStorage.MCR
				.getReturnValue("read", 0);
		extendedFunctionalityProvider.assertCallToPositionAndFunctionalityCalledWithData(
				UPDATE_BEFORE_METADATA_VALIDATION, expectedData, 1);
		extendedFunctionalityProvider.assertCallToPositionAndFunctionalityCalledWithData(
				UPDATE_AFTER_METADATA_VALIDATION, expectedData, 2);
		extendedFunctionalityProvider.assertCallToPositionAndFunctionalityCalledWithData(
				UPDATE_BEFORE_STORE, expectedData, 3);
		extendedFunctionalityProvider.assertCallToPositionAndFunctionalityCalledWithData(
				UPDATE_AFTER_STORE, expectedData, 4);

		Collection<Object> exFunctionalities = extendedFunctionalityProvider.MCR
				.getReturnValues("getFunctionalityForPositionAndRecordType");

		assertExtendedFunctionalityCalledWithSameExDataInstanceSoThatSharedDataWorks(
				exFunctionalities);
	}

	private void assertExtendedFunctionalityCalledWithSameExDataInstanceSoThatSharedDataWorks(
			Collection<Object> exFunctionalities) {
		List<ExtendedFunctionalityData> totalExDataList = new ArrayList<>();
		for (Object exFunctionality : exFunctionalities) {
			List<ExtendedFunctionalitySpy> exFuncList = (List<ExtendedFunctionalitySpy>) exFunctionality;
			for (ExtendedFunctionalitySpy exSpy : exFuncList) {
				totalExDataList.add((ExtendedFunctionalityData) exFuncList.get(0).MCR
						.getValueForMethodNameAndCallNumberAndParameterName(
								"useExtendedFunctionality", 0, "data"));
			}
		}
		for (int i = 0; i < totalExDataList.size() - 2; i++) {
			assertSame(totalExDataList.get(i).dataSharer, totalExDataList.get(i + 1).dataSharer);
		}
	}

	@Test
	public void testIndexerIsCalled() {
		recordTypeHandlerSpy.MRV.setDefaultReturnValuesSupplier("getRecordTypeId",
				() -> "typeWithAutoGeneratedId");
		DataGroup dataGroup = getDataGroupToUpdate();

		recordUpdaterOld.updateRecord(AUTH_TOKEN, "typeWithAutoGeneratedId", "somePlace",
				dataGroup);

		var recordToSentToStorage = (DataGroup) recordStorage.MCR
				.getValueForMethodNameAndCallNumberAndParameterName("update", 0, "dataRecord");
		CollectTerms collectTerms = (CollectTerms) termCollector.MCR.getReturnValue("collectTerms",
				1);

		var ids = recordTypeHandlerSpy.MCR.getReturnValue("getCombinedIdForIndex", 0);

		recordIndexer.MCR.assertParameters("indexData", 0, ids, collectTerms.indexTerms,
				recordToSentToStorage);
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
		dataGroup.addChild(new DataAtomicOldSpy("atomicId", "atomicValue"));
		return dataGroup;
	}

	@Test
	public void testUpdateRecordDataDividerExtractedFromData() {
		recordTypeHandlerSpy.MRV.setDefaultReturnValuesSupplier("getRecordTypeId",
				() -> "typeWithAutoGeneratedId");
		DataGroup dataGroup = getDataGroupToUpdate();

		recordUpdaterOld.updateRecord(AUTH_TOKEN, "typeWithAutoGeneratedId", "somePlace",
				dataGroup);

		recordStorage.MCR.assertParameter("update", 0, "dataDivider", "cora");
	}

	@Test
	public void testCreateRecordCollectedTermsSentAlong() {
		recordTypeHandlerSpy.MRV.setDefaultReturnValuesSupplier("getRecordTypeId",
				() -> "typeWithAutoGeneratedId");
		DataGroup dataGroup = getDataGroupToUpdate();

		recordUpdaterOld.updateRecord(AUTH_TOKEN, "typeWithAutoGeneratedId", "somePlace",
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
		recordStorage.MRV.setAlwaysThrowException("read",
				RecordNotFoundException.withMessage("message"));

		DataGroup record = new DataGroupOldSpy("authority");
		DataGroup recordInfo = new DataGroupOldSpy("recordInfo");

		record.addChild(recordInfo);
		recordUpdaterOld.updateRecord(AUTH_TOKEN, "recordType", "NOT_FOUND", record);
	}

	@Test(expectedExceptions = DataMissingException.class)
	public void testUpdateRecordRecordInfoMissing() {
		recordTypeHandlerSpy.MRV.setDefaultReturnValuesSupplier("getRecordTypeId",
				() -> "typeWithAutoGeneratedId");
		DataGroup group = new DataGroupOldSpy("authority");
		recordUpdaterOld.updateRecord(AUTH_TOKEN, "typeWithAutoGeneratedId", "place", group);
	}

	@Test(expectedExceptions = DataMissingException.class)
	public void testUpdateRecordRecordInfoContentMissing() {
		recordTypeHandlerSpy.MRV.setDefaultReturnValuesSupplier("getRecordTypeId",
				() -> "typeWithAutoGeneratedId");
		DataGroup group = new DataGroupOldSpy("authority");
		DataGroup createRecordInfo = new DataGroupOldSpy("recordInfo");
		group.addChild(createRecordInfo);
		recordUpdaterOld.updateRecord(AUTH_TOKEN, "typeWithAutoGeneratedId", "place", group);
	}

	@Test(expectedExceptions = DataException.class, expectedExceptionsMessageRegExp = ""
			+ "Data is not valid: \\[Error1, Error2\\]")
	public void testUpdateRecordInvalidData() throws Exception {
		ExtendedFunctionalitySpy extendedFunctionalitySpy = new ExtendedFunctionalitySpy();
		setUpDataValidatorToReturnInvalidWithErrors();

		String sameLatestUpdatedTimestamp = "2020-01-01T00:00:00.000001Z";
		setupPreviouslyStoredRecord(sameLatestUpdatedTimestamp);
		DataGroup dataGroup = setUpRecordUpdaterWithExtFunctionallityAndValue(Optional.empty(),
				extendedFunctionalitySpy, Optional.of(sameLatestUpdatedTimestamp));

		recordUpdater.updateRecord(AUTH_TOKEN, RECORD_TYPE, "spyId", dataGroup);
	}

	private void setUpDataValidatorToReturnInvalidWithErrors() {
		DataValidatorSpy dataValidatorSpy = new DataValidatorSpy();
		ValidationAnswerSpy validationAnswer = new ValidationAnswerSpy();
		validationAnswer.MRV.setDefaultReturnValuesSupplier("dataIsInvalid", () -> true);
		validationAnswer.MRV.setDefaultReturnValuesSupplier("getErrorMessages",
				() -> List.of("Error1", "Error2"));

		dataValidatorSpy.MRV.setDefaultReturnValuesSupplier("validateData", () -> validationAnswer);
		dependencyProviderSpy.MRV.setDefaultReturnValuesSupplier("getDataValidator",
				() -> dataValidatorSpy);
	}

	@Test(expectedExceptions = DataException.class)
	public void testUpdateRecordIncomingNameInDatasDoNotMatch() {
		DataGroup dataGroup = new DataGroupOldSpy("typeWithUserGeneratedId");
		DataGroup createRecordInfo = new DataGroupOldSpy("recordInfo");
		createRecordInfo.addChild(new DataAtomicOldSpy("id", "place"));
		createRecordInfo.addChild(new DataAtomicOldSpy("type", "recordType"));
		dataGroup.addChild(createRecordInfo);
		dataGroup.addChild(new DataAtomicOldSpy("atomicId", "atomicValue"));

		recordUpdaterOld.updateRecord(AUTH_TOKEN, "typeWithAutoGeneratedId", "place_NOT_THE_SAME",
				dataGroup);
	}

	@Test(expectedExceptions = DataException.class, expectedExceptionsMessageRegExp = ""
			+ "Value in data\\(recordType\\) does not match entered value\\(typeWithAutoGeneratedId\\)")
	public void testUpdateRecordIncomingDataTypesDoNotMatch() {
		DataGroup dataGroup = new DataGroupOldSpy("typeWithUserGeneratedId_NOT_THE_SAME");
		DataGroup createRecordInfo = new DataGroupOldSpy("recordInfo");
		createRecordInfo.addChild(new DataAtomicOldSpy("id", "place"));
		createRecordInfo.addChild(createLinkWithLinkedId("type", "linkedRecordType", "recordType"));

		dataGroup.addChild(createRecordInfo);
		dataGroup.addChild(new DataAtomicOldSpy("atomicId", "atomicValue"));

		recordTypeHandlerSpy.MRV.setDefaultReturnValuesSupplier("getRecordTypeId",
				() -> "typeWithAutoGeneratedId");

		recordUpdaterOld.updateRecord(AUTH_TOKEN, "typeWithAutoGeneratedId", "place", dataGroup);
	}

	@Test(expectedExceptions = DataException.class, expectedExceptionsMessageRegExp = ""
			+ "Value in data\\(place\\) does not match entered value\\(placeNOT\\)")
	public void testUpdateRecordIncomingDataIdDoNotMatch() {
		DataGroup dataGroup = new DataGroupOldSpy("typeWithUserGeneratedId_NOT_THE_SAME");
		DataGroup createRecordInfo = new DataGroupOldSpy("recordInfo");
		createRecordInfo.addChild(new DataAtomicOldSpy("id", "place"));
		createRecordInfo.addChild(createLinkWithLinkedId("type", "linkedRecordType", "recordType"));
		dataGroup.addChild(createRecordInfo);
		dataGroup.addChild(new DataAtomicOldSpy("atomicId", "atomicValue"));
		recordTypeHandlerSpy.MRV.setDefaultReturnValuesSupplier("getRecordTypeId",
				() -> "typeWithAutoGeneratedId");

		recordUpdaterOld.updateRecord(AUTH_TOKEN, "typeWithAutoGeneratedId", "placeNOT", dataGroup);
	}

	@Test(expectedExceptions = DataException.class)
	public void testLinkedRecordIdDoesNotExist() {
		Link links = new Link("toType", "toId");
		linkCollector.MRV.setDefaultReturnValuesSupplier("collectLinks", () -> Set.of(links));

		DataGroup dataGroup = RecordLinkTestsDataCreator
				.createDataDataGroupWithRecordInfoAndLinkOneLevelDown();

		recordUpdaterOld.updateRecord(AUTH_TOKEN, "dataWithLinks", "oneLinkOneLevelDown",
				dataGroup);
	}

	@Test
	public void testStoreInArchiveTrue() throws Exception {
		recordTypeHandlerSpy.MRV.setDefaultReturnValuesSupplier("storeInArchive", () -> true);
		DataGroupSpy recordSpy = createDataGroupForUpdate();

		recordUpdaterOld.updateRecord(AUTH_TOKEN, RECORD_TYPE, RECORD_ID, recordSpy);

		recordArchive.MCR.assertParameters("update", 0, RECORD_TYPE, RECORD_ID, recordSpy);
	}

	@Test
	public void testStoreInArchive_MissingInArchiveThrowsException_createInstead()
			throws Exception {
		recordTypeHandlerSpy.MRV.setDefaultReturnValuesSupplier("storeInArchive", () -> true);
		recordArchive.MRV.setAlwaysThrowException("update",
				RecordNotFoundException.withMessage("sorry not found"));
		DataGroupSpy recordSpy = createDataGroupForUpdate();

		recordUpdaterOld.updateRecord(AUTH_TOKEN, RECORD_TYPE, RECORD_ID, recordSpy);

		recordArchive.MCR.assertParameters("update", 0, RECORD_TYPE, RECORD_ID, recordSpy);
		recordArchive.MCR.assertParameters("create", 0, RECORD_TYPE, RECORD_ID, recordSpy);
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
				(Supplier<String>) () -> RECORD_ID, "id");
		recordInfoSpy.MRV.setReturnValues("containsChildWithNameInData", List.of(false), "updated");

		DataRecordLinkSpy typeSpy = new DataRecordLinkSpy();
		recordInfoSpy.MRV.setReturnValues("getFirstChildWithNameInData", List.of(typeSpy), "type");
		typeSpy.MRV.setReturnValues("getLinkedRecordId", List.of(RECORD_TYPE));

		dataDividerSpy.MRV.setReturnValues("getFirstAtomicValueWithNameInData", List.of("uu"),
				"linkedRecordId");
		return recordSpy;
	}

	@Test
	public void testStoreInArchiveFalse() throws Exception {
		DataGroupSpy recordSpy = createDataGroupForUpdate();

		recordUpdaterOld.updateRecord(AUTH_TOKEN, RECORD_TYPE, RECORD_ID, recordSpy);

		recordArchive.MCR.assertMethodNotCalled("update");
	}

	@Test
	public void testUseExtendedFunctionalityExtendedFunctionalitiesExists() throws Exception {
		DataGroupSpy recordSpy = createDataGroupForUpdate();

		recordUpdaterOld.updateRecord(AUTH_TOKEN, RECORD_TYPE, RECORD_ID, recordSpy);

		DataRecordOldSpy enhancedRecord = (DataRecordOldSpy) dataGroupToRecordEnhancer.MCR
				.getReturnValue("enhance", 0);
		ExtendedFunctionalityData expectedData = new ExtendedFunctionalityData();
		expectedData.recordType = RECORD_TYPE;
		expectedData.recordId = RECORD_ID;
		expectedData.authToken = AUTH_TOKEN;
		expectedData.user = (User) authenticator.MCR.getReturnValue("getUserForToken", 0);
		expectedData.previouslyStoredTopDataGroup = (DataGroup) recordStorage.MCR
				.getReturnValue("read", 0);
		expectedData.dataGroup = enhancedRecord.getDataGroup();
		expectedData.dataRecord = enhancedRecord;
		extendedFunctionalityProvider.assertCallToPositionAndFunctionalityCalledWithData(
				UPDATE_BEFORE_RETURN, expectedData, 5);
	}

	@Test
	public void testUseExtendedFunctionalityExtendedFunctionalitiesExists2() throws Exception {
		extendedFunctionalityProvider.setUpExtendedFunctionalityToThrowExceptionOnPosition(
				dependencyProviderSpy, UPDATE_AFTER_AUTHORIZATION, RECORD_TYPE);
		DataGroupSpy recordSpy = createDataGroupForUpdate();
		try {
			recordUpdaterOld.updateRecord(AUTH_TOKEN, RECORD_TYPE, RECORD_ID, recordSpy);
			fail("Should fail as we want to stop execution when the extended functionality is used,"
					+ " to determin that the extended functionality is called in the correct place"
					+ " in the code");
		} catch (Exception e) {
		}

		authorizator.MCR.assertMethodWasCalled("checkUserIsAuthorizedForActionOnRecordType");
		dataFactorySpy.MCR.assertMethodNotCalled("factorRecordGroupFromDataGroup");
	}

	@Test
	public void testRecordUpdaterGetsUniqueValiadatorFromDependencyProvider() throws Exception {
		DataGroupSpy recordSpy = createDataGroupForUpdate();

		recordUpdater.updateRecord(AUTH_TOKEN, RECORD_TYPE, RECORD_ID, recordSpy);

		dependencyProviderSpy.MCR.assertCalledParameters("getUniqueValidator", recordStorage);
	}

	@Test
	public void uniqueValidatorCalledWithCorrectParameters() throws Exception {
		List<Unique> uniqueList = List.of(new Unique("", Set.of("")));
		recordTypeHandlerSpy.MRV.setDefaultReturnValuesSupplier("getUniqueDefinitions",
				() -> uniqueList);
		CollectTerms collectTerms = new CollectTerms();
		collectTerms.storageTerms = Set.of(new StorageTerm("id", "key", "value"));
		termCollector.MRV.setDefaultReturnValuesSupplier("collectTerms", () -> collectTerms);
		DataGroupSpy recordSpy = createDataGroupForUpdate();

		recordUpdater.updateRecord(AUTH_TOKEN, RECORD_TYPE, RECORD_ID, recordSpy);

		uniqueValidator.MCR.assertParameters("validateUniqueForExistingRecord", 0, RECORD_TYPE,
				RECORD_ID, uniqueList, collectTerms.storageTerms);
	}

	@Test
	public void testUniqueValidationFails_throwsSpiderConflictException() throws Exception {
		DataGroupSpy recordSpy = createDataGroupForUpdate();
		setupUniqueValidatorToReturnInvalidAnswerWithThreeErrors();

		try {
			recordUpdater.updateRecord(AUTH_TOKEN, RECORD_TYPE, RECORD_ID, recordSpy);

			fail("A ConclictException should have been thrown");
		} catch (Exception e) {
			assertTrue(e instanceof ConflictException);
			assertEquals(e.getMessage(),
					"The record could not be created as it fails unique validation with the "
							+ "following 3 error messages: [" + "error1, error2, error3]");
			recordStorage.MCR.assertMethodNotCalled("create");
		}
	}

	private void setupUniqueValidatorToReturnInvalidAnswerWithThreeErrors() {
		ValidationAnswerSpy validationAnswer = new ValidationAnswerSpy();
		validationAnswer.MRV.setDefaultReturnValuesSupplier("dataIsInvalid", () -> true);
		validationAnswer.MRV.setDefaultReturnValuesSupplier("getErrorMessages",
				() -> List.of("error1", "error2", "error3"));
		uniqueValidator.MRV.setDefaultReturnValuesSupplier("validateUniqueForExistingRecord",
				() -> validationAnswer);
	}
}
