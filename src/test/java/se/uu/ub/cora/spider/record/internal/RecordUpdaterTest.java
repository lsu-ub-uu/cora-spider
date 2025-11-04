/*
 * Copyright 2015, 2016, 2018, 2020, 2021, 2022, 2023, 2024, 2025 Uppsala University Library
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
import static org.testng.Assert.assertSame;
import static org.testng.Assert.assertTrue;
import static org.testng.Assert.fail;
import static se.uu.ub.cora.spider.extendedfunctionality.ExtendedFunctionalityPosition.UPDATE_AFTER_AUTHORIZATION;
import static se.uu.ub.cora.spider.extendedfunctionality.ExtendedFunctionalityPosition.UPDATE_AFTER_METADATA_VALIDATION;
import static se.uu.ub.cora.spider.extendedfunctionality.ExtendedFunctionalityPosition.UPDATE_AFTER_STORE;
import static se.uu.ub.cora.spider.extendedfunctionality.ExtendedFunctionalityPosition.UPDATE_BEFORE_METADATA_VALIDATION;
import static se.uu.ub.cora.spider.extendedfunctionality.ExtendedFunctionalityPosition.UPDATE_BEFORE_RETURN;
import static se.uu.ub.cora.spider.extendedfunctionality.ExtendedFunctionalityPosition.UPDATE_BEFORE_STORE;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import se.uu.ub.cora.beefeater.authentication.User;
import se.uu.ub.cora.bookkeeper.recordtype.Unique;
import se.uu.ub.cora.bookkeeper.validator.DataValidationException;
import se.uu.ub.cora.data.DataProvider;
import se.uu.ub.cora.data.DataRecordGroup;
import se.uu.ub.cora.data.collected.CollectTerms;
import se.uu.ub.cora.data.collected.Link;
import se.uu.ub.cora.data.collected.PermissionTerm;
import se.uu.ub.cora.data.collected.StorageTerm;
import se.uu.ub.cora.data.copier.DataCopierFactory;
import se.uu.ub.cora.data.copier.DataCopierProvider;
import se.uu.ub.cora.data.spies.DataAtomicSpy;
import se.uu.ub.cora.data.spies.DataFactorySpy;
import se.uu.ub.cora.data.spies.DataRecordGroupSpy;
import se.uu.ub.cora.logger.LoggerProvider;
import se.uu.ub.cora.spider.authorization.AuthorizationException;
import se.uu.ub.cora.spider.data.DataRecordOldSpy;
import se.uu.ub.cora.spider.dependency.spy.RecordTypeHandlerSpy;
import se.uu.ub.cora.spider.extendedfunctionality.ExtendedFunctionalityData;
import se.uu.ub.cora.spider.extendedfunctionality.ExtendedFunctionalitySpy;
import se.uu.ub.cora.spider.extendedfunctionality.internal.ExtendedFunctionalityProviderSpy;
import se.uu.ub.cora.spider.log.LoggerFactorySpy;
import se.uu.ub.cora.spider.record.ConflictException;
import se.uu.ub.cora.spider.record.DataCopierFactorySpy;
import se.uu.ub.cora.spider.record.DataException;
import se.uu.ub.cora.spider.record.DataGroupToRecordEnhancerSpy;
import se.uu.ub.cora.spider.record.RecordUpdater;
import se.uu.ub.cora.spider.spy.DataChangedSenderSpy;
import se.uu.ub.cora.spider.spy.DataGroupTermCollectorSpy;
import se.uu.ub.cora.spider.spy.DataRecordLinkCollectorSpy;
import se.uu.ub.cora.spider.spy.DataValidatorSpy;
import se.uu.ub.cora.spider.spy.RecordIndexerSpy;
import se.uu.ub.cora.spider.spy.SpiderDependencyProviderSpy;
import se.uu.ub.cora.spider.spy.UniqueValidatorSpy;
import se.uu.ub.cora.spider.spy.ValidationAnswerSpy;
import se.uu.ub.cora.storage.RecordNotFoundException;
import se.uu.ub.cora.storage.spies.RecordStorageSpy;
import se.uu.ub.cora.storage.spies.archive.RecordArchiveSpy;

public class RecordUpdaterTest {
	private static final String AUTH_TOKEN = "someAuthToken";
	private static final String RECORD_ID = "someRecordId";
	private static final String RECORD_TYPE = "spyType";
	private RecordStorageSpy recordStorage;
	private RecordArchiveSpy recordArchive;
	private RecordUpdater recordUpdater;
	private DataRecordLinkCollectorSpy linkCollector;
	private SpiderDependencyProviderSpy dependencyProviderSpy;
	private ExtendedFunctionalityProviderSpy extendedFunctionalityProvider;
	private DataGroupToRecordEnhancerSpy dataGroupToRecordEnhancer;
	private DataGroupTermCollectorSpy termCollector;
	private RecordIndexerSpy recordIndexer;
	private LoggerFactorySpy loggerFactorySpy;
	private DataFactorySpy dataFactorySpy;

	private DataCopierFactory dataCopierFactory;
	private RecordTypeHandlerSpy recordTypeHandlerSpy;
	private UniqueValidatorSpy uniqueValidator;
	private AuthenticatorSpy authenticator;
	private SpiderAuthorizatorSpy authorizator;
	private DataValidatorSpy dataValidator;
	private DataRedactorSpy dataRedactor;

	private DataRecordGroupSpy recordWithId;
	private DataRecordGroupSpy previouslyStoredRecordGroup;
	private User currentUser;

	@BeforeMethod
	public void beforeMethod() {
		setUpFactoriesAndProviders();
		authenticator = new AuthenticatorSpy();
		authorizator = new SpiderAuthorizatorSpy();
		dataValidator = new DataValidatorSpy();
		recordStorage = new RecordStorageSpy();
		linkCollector = new DataRecordLinkCollectorSpy();
		termCollector = new DataGroupTermCollectorSpy();
		recordIndexer = new RecordIndexerSpy();
		dataRedactor = new DataRedactorSpy();
		dataGroupToRecordEnhancer = new DataGroupToRecordEnhancerSpy();
		extendedFunctionalityProvider = new ExtendedFunctionalityProviderSpy();
		recordArchive = new RecordArchiveSpy();
		uniqueValidator = new UniqueValidatorSpy();
		recordTypeHandlerSpy = new RecordTypeHandlerSpy();
		currentUser = new User("someUserId");

		setUpDependencyProvider();
		recordTypeHandlerSpy.MRV.setDefaultReturnValuesSupplier("getRecordTypeId",
				() -> RECORD_TYPE);
		recordStorage.MRV.setDefaultReturnValuesSupplier("read", () -> previouslyStoredRecordGroup);
		authenticator.MRV.setDefaultReturnValuesSupplier("getUserForToken", () -> currentUser);

		setUpToReturnFakeDataForUpdatedTS();

		recordWithId = createDataGroupForUpdate();
		previouslyStoredRecordGroup = new DataRecordGroupSpy();
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
		dependencyProviderSpy.MRV.setDefaultReturnValuesSupplier("getRecordArchive",
				() -> recordArchive);
		dependencyProviderSpy.MRV.setDefaultReturnValuesSupplier("getDataValidator",
				() -> dataValidator);
		dependencyProviderSpy.MRV.setDefaultReturnValuesSupplier("getSpiderAuthorizator",
				() -> authorizator);
		dependencyProviderSpy.MRV.setDefaultReturnValuesSupplier("getAuthenticator",
				() -> authenticator);
		dependencyProviderSpy.MRV.setDefaultReturnValuesSupplier("getRecordIndexer",
				() -> recordIndexer);
		dependencyProviderSpy.MRV.setDefaultReturnValuesSupplier("getDataRecordLinkCollector",
				() -> linkCollector);
		dependencyProviderSpy.MRV.setDefaultReturnValuesSupplier("getDataRedactor",
				() -> dataRedactor);

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

	private DataRecordGroupSpy createDataGroupForUpdate() {
		DataRecordGroupSpy recordSpy = new DataRecordGroupSpy();
		recordSpy.MRV.setDefaultReturnValuesSupplier("getType", () -> RECORD_TYPE);
		recordSpy.MRV.setDefaultReturnValuesSupplier("getId", () -> RECORD_ID);
		recordSpy.MRV.setDefaultReturnValuesSupplier("getDataDivider", () -> "someDataDivider");
		recordSpy.MRV.setDefaultReturnValuesSupplier("overwriteProtectionShouldBeEnforced",
				() -> true);
		return recordSpy;
	}

	@Test
	public void testExternalDependenciesAreCalled() {
		recordUpdater.updateRecord(AUTH_TOKEN, RECORD_TYPE, RECORD_ID, recordWithId);

		authorizator.MCR.assertMethodWasCalled("checkUserIsAuthorizedForActionOnRecordType");

		dataFactorySpy.MCR.assertNumberOfCallsToMethod("factorGroupFromDataRecordGroup", 3);
		var recordAsDataGroup = dataFactorySpy.MCR.getReturnValue("factorGroupFromDataRecordGroup",
				0);
		dataValidator.MCR.assertParameters("validateData", 0,
				"fakeUpdateMetadataIdFromRecordTypeHandlerSpy", recordAsDataGroup);

		var recordAsDataGroup2 = dataFactorySpy.MCR.getReturnValue("factorGroupFromDataRecordGroup",
				1);
		linkCollector.MCR.assertParameters("collectLinks", 0,
				"fakeDefMetadataIdFromRecordTypeHandlerSpy", recordAsDataGroup2);

		CollectTerms collectedTerms = (CollectTerms) termCollector.MCR
				.getReturnValue("collectTerms", 1);

		var links = linkCollector.MCR.getReturnValue("collectLinks", 0);

		var recordAsDataGroup3 = dataFactorySpy.MCR.getReturnValue("factorGroupFromDataRecordGroup",
				2);
		recordStorage.MCR.assertParameters("update", 0, RECORD_TYPE, RECORD_ID, recordAsDataGroup3,
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
	public void testRecordTypePassedNOTEqualsTheLinkedInValidationType() {
		recordTypeHandlerSpy.MRV.setDefaultReturnValuesSupplier("getRecordTypeId",
				() -> "NOTRecordTypeId");

		recordUpdater.updateRecord(AUTH_TOKEN, RECORD_TYPE, RECORD_ID, recordWithId);
	}

	@Test
	public void testRecordTypeHandlerFetchedFromDependencyProvider() {
		recordUpdater.updateRecord(AUTH_TOKEN, RECORD_TYPE, RECORD_ID, recordWithId);

		dependencyProviderSpy.MCR.assertParameters("getRecordTypeHandlerUsingDataRecordGroup", 0,
				recordWithId);
	}

	@Test
	public void testValidationTypeDoesNotExist() {
		recordTypeHandlerSpy.MRV.setAlwaysThrowException("getUpdateDefinitionId",
				DataValidationException.withMessage("some message"));

		try {
			recordUpdater.updateRecord(AUTH_TOKEN, RECORD_TYPE, RECORD_ID, recordWithId);
			fail("Exception should have been thrown");
		} catch (Exception e) {
			assertTrue(e instanceof DataException);
			assertEquals(e.getMessage(), "Data is not valid: some message");
		}
	}

	@Test
	public void testIgnoreOverwriteProtection_removedFromRecordInfo() {
		setUpExtFuncToImmediatelyAssertOverwriteProtectionHasBeenCalledOnPositionUPDATE_BEFORE_METADATA_VALIDATION();
		recordWithId.MRV.setDefaultReturnValuesSupplier("overwriteProtectionShouldBeEnforced",
				() -> true);

		recordUpdater.updateRecord(AUTH_TOKEN, RECORD_TYPE, RECORD_ID, recordWithId);
	}

	private void setUpExtFuncToImmediatelyAssertOverwriteProtectionHasBeenCalledOnPositionUPDATE_BEFORE_METADATA_VALIDATION() {
		class ExtendedFunctionalityAssertRemovedOverwriteProtectionSpy
				extends ExtendedFunctionalitySpy {
			@Override
			public void useExtendedFunctionality(ExtendedFunctionalityData data) {
				((DataRecordGroupSpy) data.dataRecordGroup).MCR
						.assertCalledParameters("removeOverwriteProtection");
			}
		}

		var extendedFunctionalitySpy = new ExtendedFunctionalityAssertRemovedOverwriteProtectionSpy();
		extendedFunctionalityProvider.MRV.setSpecificReturnValuesSupplier(
				"getFunctionalityForPositionAndRecordType", () -> List.of(extendedFunctionalitySpy),
				UPDATE_BEFORE_METADATA_VALIDATION, RECORD_TYPE);
	}

	@org.testng.annotations.DataProvider(name = "overwriteProtection")
	public Object[][] testDataForOverwriteProtection() {
		return new Boolean[][] { { false, true }, { false, false }, { true, false } };
	}

	@Test(dataProvider = "overwriteProtection")
	public void testIgnoreOverwriteProtection(boolean overwriteProtectionShouldBeEnforced,
			boolean lastUpdatedTsIsDifferent) {
		setUpExtFuncToImmediatelyAssertOverwriteProtectionHasBeenCalledOnPositionUPDATE_BEFORE_METADATA_VALIDATION();
		setUpEnforceAndLastUpdatedTsIsDifferent(overwriteProtectionShouldBeEnforced,
				lastUpdatedTsIsDifferent);

		recordUpdater.updateRecord(AUTH_TOKEN, RECORD_TYPE, RECORD_ID, recordWithId);

		recordStorage.MCR.assertMethodWasCalled("update");
	}

	private void setUpEnforceAndLastUpdatedTsIsDifferent(
			boolean overwriteProtectionShouldBeEnforced, boolean lastUpdatedTsIsDifferent) {
		String ts = "2024-01-01T00:00:00.000000Z";
		String otherTs = "2020-01-01T00:00:00.000000Z";
		previouslyStoredRecordGroup.MRV.setDefaultReturnValuesSupplier("getLatestTsUpdated",
				() -> ts);
		recordWithId.MRV.setDefaultReturnValuesSupplier("overwriteProtectionShouldBeEnforced",
				() -> overwriteProtectionShouldBeEnforced);
		if (lastUpdatedTsIsDifferent) {
			recordWithId.MRV.setDefaultReturnValuesSupplier("getLatestTsUpdated", () -> otherTs);
		} else {
			recordWithId.MRV.setDefaultReturnValuesSupplier("getLatestTsUpdated", () -> ts);
		}
	}

	@Test
	public void testIgnoreOverwriteProtection_DifferentLatestUpdated_noIgnoreFlag_ConflictException() {
		setUpExtFuncToImmediatelyAssertOverwriteProtectionHasBeenCalledOnPositionUPDATE_BEFORE_METADATA_VALIDATION();
		setUpEnforceAndLastUpdatedTsIsDifferent(true, true);

		try {
			recordUpdater.updateRecord(AUTH_TOKEN, RECORD_TYPE, RECORD_ID, recordWithId);
			fail();
		} catch (Exception e) {
			assertTrue(e instanceof ConflictException);
			assertEquals(e.getMessage(), "Could not update record because it exist a newer "
					+ "version of the record in the storage.");
			recordStorage.MCR.assertMethodNotCalled("update");
		}
	}

	@Test
	public void testCorrectSpiderAuthorizatorForNoRecordPartConstraints() {
		recordUpdater.updateRecord(AUTH_TOKEN, RECORD_TYPE, RECORD_ID, recordWithId);

		var returnedUser = getAuthenticatedUser();
		authorizator.MCR.assertParameters(
				"checkGetUsersMatchedRecordPartPermissionsForActionOnRecordTypeAndCollectedData", 0,
				returnedUser, "update", RECORD_TYPE, getPermissionTermUsingCallNo(0), false);

		authorizator.MCR.assertParameters(
				"checkUserIsAuthorizedForActionOnRecordTypeAndCollectedData", 0, returnedUser,
				"update", RECORD_TYPE, getPermissionTermUsingCallNo(1));
		authorizator.MCR.assertNumberOfCallsToMethod(
				"checkUserIsAuthorizedForActionOnRecordTypeAndCollectedData", 1);

		dataRedactor.MCR.assertMethodNotCalled("replaceChildrenForConstraintsWithoutPermissions");
		dataRedactor.MCR.assertMethodNotCalled("removeChildrenForConstraintsWithoutPermissions");

		var recordGroupAsDataGroup = dataFactorySpy.MCR
				.assertCalledParametersReturn("factorGroupFromDataRecordGroup", recordWithId);
		dataValidator.MCR.assertParameter("validateData", 0, "dataGroup", recordGroupAsDataGroup);
	}

	private Object getAuthenticatedUser() {
		return authenticator.MCR.getReturnValue("getUserForToken", 0);
	}

	private List<PermissionTerm> getPermissionTermUsingCallNo(int callNumber) {
		return ((CollectTerms) termCollector.MCR.getReturnValue("collectTerms",
				callNumber)).permissionTerms;
	}

	@Test
	public void testCorrectSpiderAuthorizatorForWriteRecordPartConstraints() {
		recordTypeHandlerSpy.MRV.setDefaultReturnValuesSupplier("hasRecordPartWriteConstraint",
				() -> true);
		Set<String> writeConstraints = Set.of("someConstraint");
		recordTypeHandlerSpy.MRV.setDefaultReturnValuesSupplier(
				"getUpdateWriteRecordPartConstraints", () -> writeConstraints);
		dataRedactor.MRV.setDefaultReturnValuesSupplier(
				"replaceChildrenForConstraintsWithoutPermissions", () -> recordWithId);

		recordUpdater.updateRecord(AUTH_TOKEN, RECORD_TYPE, RECORD_ID, recordWithId);

		var returnedUser = getAuthenticatedUser();
		authorizator.MCR.assertParameters(
				"checkGetUsersMatchedRecordPartPermissionsForActionOnRecordTypeAndCollectedData", 0,
				returnedUser, "update", RECORD_TYPE, getPermissionTermUsingCallNo(0), true);
		authorizator.MCR.assertNumberOfCallsToMethod(
				"checkGetUsersMatchedRecordPartPermissionsForActionOnRecordTypeAndCollectedData",
				1);

		authorizator.MCR.assertParameters(
				"checkUserIsAuthorizedForActionOnRecordTypeAndCollectedData", 0, returnedUser,
				"update", RECORD_TYPE, getPermissionTermUsingCallNo(1));
		authorizator.MCR.assertNumberOfCallsToMethod(
				"checkUserIsAuthorizedForActionOnRecordTypeAndCollectedData", 1);

		dataRedactor.MCR.assertMethodWasCalled("replaceChildrenForConstraintsWithoutPermissions");

		Set<?> expectedPermissions = (Set<?>) authorizator.MCR.getReturnValue(
				"checkGetUsersMatchedRecordPartPermissionsForActionOnRecordTypeAndCollectedData",
				0);
		dataRedactor.MCR.assertParameters("replaceChildrenForConstraintsWithoutPermissions", 0,
				"fakeDefMetadataIdFromRecordTypeHandlerSpy",
				recordStorage.MCR.getReturnValue("read", 0), recordWithId, writeConstraints,
				expectedPermissions);

		DataRecordGroup returnedRedactedDataGroup = (DataRecordGroup) dataRedactor.MCR
				.getReturnValue("replaceChildrenForConstraintsWithoutPermissions", 0);

		dataFactorySpy.MCR.assertParameters("factorGroupFromDataRecordGroup", 0,
				returnedRedactedDataGroup);
		var recordGroupAsDataGroup = dataFactorySpy.MCR
				.getReturnValue("factorGroupFromDataRecordGroup", 0);
		dataValidator.MCR.assertParameter("validateData", 0, "dataGroup", recordGroupAsDataGroup);

		// reading updated data
		dataRedactor.MCR.assertMethodNotCalled("removeChildrenForConstraintsWithoutPermissions");
	}

	@Test
	public void testRecordEnhancerCalled() {
		recordUpdater.updateRecord(AUTH_TOKEN, RECORD_TYPE, RECORD_ID, recordWithId);

		var returnedUser = getAuthenticatedUser();
		dataGroupToRecordEnhancer.MCR.assertParameters("enhance", 0, returnedUser, RECORD_TYPE,
				recordWithId, dataRedactor);
	}

	@Test
	public void testReplaceImmutableFieldsInRecordInfoFromPreviouslyStoredRecord() {
		recordUpdater.updateRecord(AUTH_TOKEN, RECORD_TYPE, RECORD_ID, recordWithId);

		assertResultFromGetInPreviousSetInCurrent("setId", "getId");
		assertResultFromGetInPreviousSetInCurrent("setType", "getType");
		assertResultFromGetInPreviousSetInCurrent("setCreatedBy", "getCreatedBy");
		assertResultFromGetInPreviousSetInCurrent("setTsCreated", "getTsCreated");
		recordWithId.MCR.assertParameters("setAllUpdated", 0, Collections.emptyList());
	}

	@Test
	public void testCurrentUserAddedAsLastUpdated() {
		recordUpdater.updateRecord(AUTH_TOKEN, RECORD_TYPE, RECORD_ID, recordWithId);

		recordWithId.MCR.assertParameters("addUpdatedUsingUserIdAndTsNow", 0, currentUser.id);
	}

	private void assertResultFromGetInPreviousSetInCurrent(String setMethod, String getMethod) {
		recordWithId.MCR.assertParameters(setMethod, 0,
				previouslyStoredRecordGroup.MCR.getReturnValue(getMethod, 0));
	}

	@Test
	public void testUserIsAuthorizedForPreviousVersionOfData() {
		recordUpdater.updateRecord(AUTH_TOKEN, RECORD_TYPE, RECORD_ID, recordWithId);

		authorizator.MCR.assertParameters(
				"checkGetUsersMatchedRecordPartPermissionsForActionOnRecordTypeAndCollectedData", 0,
				currentUser, "update", RECORD_TYPE, getPermissionTermUsingCallNo(0), false);

		authorizator.MCR.assertNumberOfCallsToMethod(
				"checkGetUsersMatchedRecordPartPermissionsForActionOnRecordTypeAndCollectedData",
				1);

		authorizator.MCR.assertParameters(
				"checkUserIsAuthorizedForActionOnRecordTypeAndCollectedData", 0, currentUser,
				"update", RECORD_TYPE, getPermissionTermUsingCallNo(1));
		authorizator.MCR.assertNumberOfCallsToMethod(
				"checkUserIsAuthorizedForActionOnRecordTypeAndCollectedData", 1);
	}

	@Test
	public void testUnauthorizedForUpdateOnRecordTypeShouldNotAccessStorage() {
		authorizator.MRV.setAlwaysThrowException("checkUserIsAuthorizedForActionOnRecordType",
				new AuthorizationException("someMessage"));
		try {
			recordUpdater.updateRecord(AUTH_TOKEN, RECORD_TYPE, RECORD_ID, recordWithId);
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
		recordUpdater.updateRecord(AUTH_TOKEN, RECORD_TYPE, RECORD_ID, recordWithId);

		ExtendedFunctionalityData expectedData = new ExtendedFunctionalityData();
		expectedData.recordType = RECORD_TYPE;
		expectedData.recordId = RECORD_ID;
		expectedData.authToken = AUTH_TOKEN;
		expectedData.user = (User) getAuthenticatedUser();
		expectedData.dataRecordGroup = recordWithId;

		extendedFunctionalityProvider.assertCallToPositionAndFunctionalityCalledWithData(
				UPDATE_AFTER_AUTHORIZATION, expectedData, 0);

		expectedData.previouslyStoredDataRecordGroup = previouslyStoredRecordGroup;
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
			var exFuncList = (List<ExtendedFunctionalitySpy>) exFunctionality;
			for (ExtendedFunctionalitySpy exSpy : exFuncList) {
				totalExDataList.add((ExtendedFunctionalityData) exSpy.MCR
						.getParameterForMethodAndCallNumberAndParameter("useExtendedFunctionality",
								0, "data"));
			}
		}
		for (int i = 0; i < totalExDataList.size() - 2; i++) {
			assertSame(totalExDataList.get(i).dataSharer, totalExDataList.get(i + 1).dataSharer);
		}
	}

	@Test
	public void testIndexerIsCalled() {
		recordUpdater.updateRecord(AUTH_TOKEN, RECORD_TYPE, RECORD_ID, recordWithId);

		CollectTerms collectTerms = (CollectTerms) termCollector.MCR.getReturnValue("collectTerms",
				1);
		recordIndexer.MCR.assertParameters("indexData", 0, RECORD_TYPE, RECORD_ID,
				collectTerms.indexTerms, recordWithId);
	}

	@Test
	public void testUpdateRecordDataDividerExtractedFromData() {
		recordUpdater.updateRecord(AUTH_TOKEN, RECORD_TYPE, RECORD_ID, recordWithId);

		recordStorage.MCR.assertParameter("update", 0, "dataDivider", "someDataDivider");
	}

	@Test
	public void testCreateRecordCollectedTermsSentAlong() {
		recordUpdater.updateRecord(AUTH_TOKEN, RECORD_TYPE, RECORD_ID, recordWithId);

		termCollector.MCR.assertParameter("collectTerms", 1, "metadataId",
				"fakeDefMetadataIdFromRecordTypeHandlerSpy");
		CollectTerms collectTerms = (CollectTerms) termCollector.MCR.getReturnValue("collectTerms",
				1);
		recordStorage.MCR.assertParameter("update", 0, "storageTerms", collectTerms.storageTerms);
	}

	@Test(expectedExceptions = RecordNotFoundException.class)
	public void testUpdateNotFound() {
		recordStorage.MRV.setAlwaysThrowException("read",
				RecordNotFoundException.withMessage("message"));

		recordUpdater.updateRecord(AUTH_TOKEN, RECORD_TYPE, RECORD_ID, recordWithId);
	}

	@Test(expectedExceptions = DataException.class, expectedExceptionsMessageRegExp = ""
			+ "Data is not valid: \\[Error1, Error2\\]")
	public void testUpdateRecordInvalidData() {
		setUpDataValidatorToReturnInvalidWithErrors();

		recordUpdater.updateRecord(AUTH_TOKEN, RECORD_TYPE, RECORD_ID, recordWithId);

	}

	private void setUpDataValidatorToReturnInvalidWithErrors() {
		ValidationAnswerSpy validationAnswer = new ValidationAnswerSpy();
		validationAnswer.MRV.setDefaultReturnValuesSupplier("dataIsInvalid", () -> true);
		validationAnswer.MRV.setDefaultReturnValuesSupplier("getErrorMessages",
				() -> List.of("Error1", "Error2"));

		dataValidator.MRV.setDefaultReturnValuesSupplier("validateData", () -> validationAnswer);
	}

	@Test(expectedExceptions = DataException.class, expectedExceptionsMessageRegExp = ""
			+ "Value in data\\(NOT_someRecordType\\) does not match entered value\\(" + RECORD_TYPE
			+ "\\)")
	public void testUpdateRecordIncomingDataTypesDoNotMatch() {
		recordWithId.MRV.setDefaultReturnValuesSupplier("getType", () -> "NOT_someRecordType");

		recordUpdater.updateRecord(AUTH_TOKEN, RECORD_TYPE, RECORD_ID, recordWithId);
	}

	@Test(expectedExceptions = DataException.class, expectedExceptionsMessageRegExp = ""
			+ "Value in data\\(NOT_someRecordId\\) does not match entered value\\(" + RECORD_ID
			+ "\\)")
	public void testUpdateRecordIncomingDataIdDoNotMatch() {
		recordWithId.MRV.setDefaultReturnValuesSupplier("getId", () -> "NOT_someRecordId");

		recordUpdater.updateRecord(AUTH_TOKEN, RECORD_TYPE, RECORD_ID, recordWithId);
	}

	@Test(expectedExceptions = DataException.class)
	public void testLinkedRecordIdDoesNotExist() {
		Link links = new Link("toType", "toId");
		linkCollector.MRV.setDefaultReturnValuesSupplier("collectLinks", () -> Set.of(links));

		recordUpdater.updateRecord(AUTH_TOKEN, RECORD_TYPE, RECORD_ID, recordWithId);
	}

	@Test
	public void testCallSendDataChanged() {
		recordUpdater.updateRecord(AUTH_TOKEN, RECORD_TYPE, RECORD_ID, recordWithId);

		var sender = getDataChangedSender();
		sender.MCR.assertParameters("sendDataChanged", 0, RECORD_TYPE, "someRecordId", "update");
	}

	private DataChangedSenderSpy getDataChangedSender() {
		return (DataChangedSenderSpy) dependencyProviderSpy.MCR
				.assertCalledParametersReturn("getDataChangeSender");
	}

	@Test
	public void testSendDataChangedAfterStoreInStorage() {
		recordStorage.MRV.setAlwaysThrowException("update", new RuntimeException("someError"));

		try {
			recordUpdater.updateRecord(AUTH_TOKEN, RECORD_TYPE, RECORD_ID, recordWithId);
			fail();
		} catch (Exception _) {
			dependencyProviderSpy.MCR.assertMethodNotCalled("getDataChangeSender");
		}
	}

	@Test
	public void testSendDataChangedBeforeStoreInArchive() {
		recordTypeHandlerSpy.MRV.setDefaultReturnValuesSupplier("storeInArchive", () -> true);
		recordArchive.MRV.setAlwaysThrowException("update", new RuntimeException("someError"));

		try {
			recordUpdater.updateRecord(AUTH_TOKEN, RECORD_TYPE, RECORD_ID, recordWithId);
			fail();
		} catch (Exception _) {
			dependencyProviderSpy.MCR.assertMethodWasCalled("getDataChangeSender");
		}
	}

	@Test
	public void testStoreInArchiveTrue() {
		recordTypeHandlerSpy.MRV.setDefaultReturnValuesSupplier("storeInArchive", () -> true);

		recordUpdater.updateRecord(AUTH_TOKEN, RECORD_TYPE, RECORD_ID, recordWithId);

		dataFactorySpy.MCR.assertParameters("factorGroupFromDataRecordGroup", 2, recordWithId);
		var recordAsDataGroup = dataFactorySpy.MCR.getReturnValue("factorGroupFromDataRecordGroup",
				2);
		recordArchive.MCR.assertParameters("update", 0, "someDataDivider", RECORD_TYPE, RECORD_ID,
				recordAsDataGroup);
	}

	@Test
	public void testStoreInArchive_MissingInArchiveThrowsException_createInstead() {
		recordTypeHandlerSpy.MRV.setDefaultReturnValuesSupplier("storeInArchive", () -> true);
		recordArchive.MRV.setAlwaysThrowException("update",
				RecordNotFoundException.withMessage("sorry not found"));

		recordUpdater.updateRecord(AUTH_TOKEN, RECORD_TYPE, RECORD_ID, recordWithId);

		dataFactorySpy.MCR.assertParameters("factorGroupFromDataRecordGroup", 2, recordWithId);
		var recordAsDataGroup = dataFactorySpy.MCR.getReturnValue("factorGroupFromDataRecordGroup",
				2);
		recordArchive.MCR.assertParameters("update", 0, "someDataDivider", RECORD_TYPE, RECORD_ID,
				recordAsDataGroup);
		recordArchive.MCR.assertParameters("create", 0, "someDataDivider", RECORD_TYPE, RECORD_ID,
				recordAsDataGroup);
	}

	@Test
	public void testStoreInArchiveFalse() {
		recordUpdater.updateRecord(AUTH_TOKEN, RECORD_TYPE, RECORD_ID, recordWithId);

		recordArchive.MCR.assertMethodNotCalled("update");
	}

	@Test
	public void testUseExtendedFunctionalityExtendedFunctionalitiesExists() {
		recordUpdater.updateRecord(AUTH_TOKEN, RECORD_TYPE, RECORD_ID, recordWithId);

		DataRecordOldSpy enhancedRecord = (DataRecordOldSpy) dataGroupToRecordEnhancer.MCR
				.getReturnValue("enhance", 0);
		ExtendedFunctionalityData expectedData = new ExtendedFunctionalityData();
		expectedData.recordType = RECORD_TYPE;
		expectedData.recordId = RECORD_ID;
		expectedData.authToken = AUTH_TOKEN;
		expectedData.user = (User) getAuthenticatedUser();
		expectedData.previouslyStoredDataRecordGroup = (DataRecordGroup) recordStorage.MCR
				.getReturnValue("read", 0);
		expectedData.dataRecordGroup = enhancedRecord.getDataRecordGroup();
		expectedData.dataRecord = enhancedRecord;
		extendedFunctionalityProvider.assertCallToPositionAndFunctionalityCalledWithData(
				UPDATE_BEFORE_RETURN, expectedData, 5);
	}

	@Test
	public void testUseExtendedFunctionalityExtendedFunctionalitiesExists2() {
		extendedFunctionalityProvider.setUpExtendedFunctionalityToThrowExceptionOnPosition(
				dependencyProviderSpy, UPDATE_AFTER_AUTHORIZATION, RECORD_TYPE);
		try {
			recordUpdater.updateRecord(AUTH_TOKEN, RECORD_TYPE, RECORD_ID, recordWithId);
			fail("Should fail as we want to stop execution when the extended functionality is used,"
					+ " to determin that the extended functionality is called in the correct place"
					+ " in the code");
		} catch (Exception _) {
			authorizator.MCR.assertMethodWasCalled("checkUserIsAuthorizedForActionOnRecordType");
			dataFactorySpy.MCR.assertMethodNotCalled("factorRecordGroupFromDataGroup");
		}
	}

	@Test
	public void testRecordUpdaterGetsUniqueValiadatorFromDependencyProvider() {
		recordUpdater.updateRecord(AUTH_TOKEN, RECORD_TYPE, RECORD_ID, recordWithId);

		dependencyProviderSpy.MCR.assertCalledParameters("getUniqueValidator", recordStorage);
	}

	@Test
	public void uniqueValidatorCalledWithCorrectParameters() {
		List<Unique> uniqueList = List.of(new Unique("", Set.of("")));
		recordTypeHandlerSpy.MRV.setDefaultReturnValuesSupplier("getUniqueDefinitions",
				() -> uniqueList);
		CollectTerms collectTerms = new CollectTerms();
		collectTerms.storageTerms = Set.of(new StorageTerm("id", "key", "value"));
		termCollector.MRV.setDefaultReturnValuesSupplier("collectTerms", () -> collectTerms);

		recordUpdater.updateRecord(AUTH_TOKEN, RECORD_TYPE, RECORD_ID, recordWithId);

		uniqueValidator.MCR.assertParameters("validateUniqueForExistingRecord", 0, RECORD_TYPE,
				RECORD_ID, uniqueList, collectTerms.storageTerms);
	}

	@Test
	public void testUniqueValidationFails_throwsSpiderConflictException() {
		setupUniqueValidatorToReturnInvalidAnswerWithThreeErrors();

		try {
			recordUpdater.updateRecord(AUTH_TOKEN, RECORD_TYPE, RECORD_ID, recordWithId);

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

	@Test
	public void testUpdateVisibilityWhenVisibilityDoNotMatch() {
		recordTypeHandlerSpy.MRV.setDefaultReturnValuesSupplier("useVisibility", () -> true);

		recordWithId.MRV.setDefaultReturnValuesSupplier("getVisibility",
				() -> Optional.of("published"));
		previouslyStoredRecordGroup.MRV.setDefaultReturnValuesSupplier("getVisibility",
				() -> Optional.of("unpublished"));

		recordUpdater.updateRecord(AUTH_TOKEN, RECORD_TYPE, RECORD_ID, recordWithId);

		previouslyStoredRecordGroup.MCR.assertMethodWasCalled("getVisibility");
		recordWithId.MCR.assertMethodWasCalled("getVisibility");
		recordWithId.MCR.assertMethodNotCalled("setVisiblity");

		recordWithId.MCR.assertMethodWasCalled("setTsVisibilityNow");
	}

	@Test
	public void testUpdateVisibilityWhenVisibilityMatch() {
		recordTypeHandlerSpy.MRV.setDefaultReturnValuesSupplier("useVisibility", () -> true);

		recordWithId.MRV.setDefaultReturnValuesSupplier("getVisibility",
				() -> Optional.of("published"));
		previouslyStoredRecordGroup.MRV.setDefaultReturnValuesSupplier("getVisibility",
				() -> Optional.of("published"));

		recordUpdater.updateRecord(AUTH_TOKEN, RECORD_TYPE, RECORD_ID, recordWithId);

		previouslyStoredRecordGroup.MCR.assertMethodWasCalled("getVisibility");
		recordWithId.MCR.assertMethodWasCalled("getVisibility");
		recordWithId.MCR.assertMethodNotCalled("setVisiblity");

		recordWithId.MCR.assertMethodNotCalled("setTsVisibilityNow");
	}

	@Test
	public void testUpdateVisibilityWhenVisibilityIsOnlyPresentInUpdatedVersion() {
		recordTypeHandlerSpy.MRV.setDefaultReturnValuesSupplier("useVisibility", () -> true);

		recordWithId.MRV.setDefaultReturnValuesSupplier("getVisibility",
				() -> Optional.of("published"));
		previouslyStoredRecordGroup.MRV.setDefaultReturnValuesSupplier("getVisibility",
				Optional::empty);

		recordUpdater.updateRecord(AUTH_TOKEN, RECORD_TYPE, RECORD_ID, recordWithId);

		previouslyStoredRecordGroup.MCR.assertMethodWasCalled("getVisibility");
		recordWithId.MCR.assertMethodWasCalled("getVisibility");
		recordWithId.MCR.assertMethodNotCalled("setVisiblity");

		recordWithId.MCR.assertMethodWasCalled("setTsVisibilityNow");
	}

	@Test
	public void testUpdateVisibilityWhenVisibilityOnlyPresentInPreviousVersion() {
		recordTypeHandlerSpy.MRV.setDefaultReturnValuesSupplier("useVisibility", () -> true);

		recordWithId.MRV.setReturnValues("getVisibility",
				List.of(Optional.empty(), Optional.of("hidden")));

		previouslyStoredRecordGroup.MRV.setDefaultReturnValuesSupplier("getVisibility",
				() -> Optional.of("hidden"));

		recordUpdater.updateRecord(AUTH_TOKEN, RECORD_TYPE, RECORD_ID, recordWithId);

		previouslyStoredRecordGroup.MCR.assertMethodWasCalled("getVisibility");
		recordWithId.MCR.assertMethodWasCalled("getVisibility");
		recordWithId.MCR.assertCalledParameters("setVisibility", "hidden");

		recordWithId.MCR.assertMethodNotCalled("setTsVisibilityNow");
	}

	@Test
	public void testUpdateVisibilityWhenVisibilityIsMissingInBothRecords() {
		recordTypeHandlerSpy.MRV.setDefaultReturnValuesSupplier("useVisibility", () -> true);

		// The second value is just used to simulate a change in visibility, we
		// assert that the actual expected "unpublished" is set below.
		recordWithId.MRV.setReturnValues("getVisibility", List.of(Optional.empty(),
				Optional.of("placeholderNonEmptyValueForVisiblityChangedPurpose")));

		previouslyStoredRecordGroup.MRV.setDefaultReturnValuesSupplier("getVisibility",
				Optional::empty);

		recordUpdater.updateRecord(AUTH_TOKEN, RECORD_TYPE, RECORD_ID, recordWithId);

		previouslyStoredRecordGroup.MCR.assertMethodWasCalled("getVisibility");
		recordWithId.MCR.assertMethodWasCalled("getVisibility");
		// assert default is set if missing in new AND old
		recordWithId.MCR.assertCalledParameters("setVisibility", "unpublished");

		recordWithId.MCR.assertMethodWasCalled("setTsVisibilityNow");
	}

	@Test
	public void testUpdateTsVisibilityWhenNotUsingVisibility() {
		recordTypeHandlerSpy.MRV.setDefaultReturnValuesSupplier("useVisibility", () -> false);

		recordUpdater.updateRecord(AUTH_TOKEN, RECORD_TYPE, RECORD_ID, recordWithId);

		previouslyStoredRecordGroup.MCR.assertMethodNotCalled("getVisibility");
		recordWithId.MCR.assertMethodNotCalled("getVisibility");
		recordWithId.MCR.assertMethodNotCalled("setTsVisibilityNow");
	}

	@Test
	public void testPermissionUnitAuthorizationCheck_DoNotUsePermissionUnit() {
		recordTypeHandlerSpy.MRV.setDefaultReturnValuesSupplier("usePermissionUnit", () -> false);

		recordUpdater.updateRecord(AUTH_TOKEN, RECORD_TYPE, RECORD_ID, recordWithId);

		authorizator.MCR.assertMethodNotCalled("checkUserIsAuthorizedForPemissionUnit");
	}

	private void setUpRecordTypeUsesPermissionUnits() {
		recordTypeHandlerSpy.MRV.setDefaultReturnValuesSupplier("usePermissionUnit", () -> true);
		previouslyStoredRecordGroup.MRV.setDefaultReturnValuesSupplier("getPermissionUnit",
				() -> Optional.of("updatedRecordPermissionUnit"));
		recordWithId.MRV.setDefaultReturnValuesSupplier("getPermissionUnit",
				() -> Optional.of("previousRecordPermissionUnit"));
	}

	@Test(expectedExceptions = AuthorizationException.class, expectedExceptionsMessageRegExp = ""
			+ "someExceptionFromSpy")
	public void testPermissionUnitAuthorizationCheck_usePermissionUnit_NotAuthorizedOnPreviousRecord() {
		setUpRecordTypeUsesPermissionUnits();
		authorizator.MRV.setThrowException("checkUserIsAuthorizedForPemissionUnit",
				new AuthorizationException("someExceptionFromSpy"), currentUser,
				"updatedRecordPermissionUnit");

		recordUpdater.updateRecord(AUTH_TOKEN, RECORD_TYPE, RECORD_ID, recordWithId);
	}

	@Test(expectedExceptions = AuthorizationException.class, expectedExceptionsMessageRegExp = ""
			+ "someExceptionFromSpy")
	public void testPermissionUnitAuthorizationCheck_usePermissionUnit_NotAuthorizedOnUpdatedRecord() {
		setUpRecordTypeUsesPermissionUnits();
		authorizator.MRV.setThrowException("checkUserIsAuthorizedForPemissionUnit",
				new AuthorizationException("someExceptionFromSpy"), currentUser,
				"previousRecordPermissionUnit");

		recordUpdater.updateRecord(AUTH_TOKEN, RECORD_TYPE, RECORD_ID, recordWithId);
	}

	@Test(expectedExceptions = AuthorizationException.class, expectedExceptionsMessageRegExp = ""
			+ "User someUserId is not authorized to delete record.")
	public void testPermissionUnitAuthorizationCheck_usePermissionUnit_missingPermissionUnitInPreviousRecord() {
		recordTypeHandlerSpy.MRV.setDefaultReturnValuesSupplier("usePermissionUnit", () -> true);
		previouslyStoredRecordGroup.MRV.setDefaultReturnValuesSupplier("getPermissionUnit",
				Optional::empty);

		recordUpdater.updateRecord(AUTH_TOKEN, RECORD_TYPE, RECORD_ID, recordWithId);
	}

	@Test(expectedExceptions = DataException.class, expectedExceptionsMessageRegExp = ""
			+ "PermissionUnit is missing in the record.")
	public void testPermissionUnitAuthorizationCheck_usePermissionUnit_missingPermissionUnitInUpdatedRecord() {
		setUpRecordTypeUsesPermissionUnits();
		recordWithId.MRV.setDefaultReturnValuesSupplier("getPermissionUnit", Optional::empty);

		recordUpdater.updateRecord(AUTH_TOKEN, RECORD_TYPE, RECORD_ID, recordWithId);
	}

	@SuppressWarnings("unchecked")
	@Test
	public void testPermissionUnitAuthorizationCheck_usePermissionUnit_isAuthorized() {
		setUpRecordTypeUsesPermissionUnits();

		recordUpdater.updateRecord(AUTH_TOKEN, RECORD_TYPE, RECORD_ID, recordWithId);

		var user = getAuthenticatedUser();
		recordTypeHandlerSpy.MCR.assertMethodWasCalled("usePermissionUnit");
		var previousRecordPermissionUnit = (Optional<String>) previouslyStoredRecordGroup.MCR
				.assertCalledParametersReturn("getPermissionUnit");
		authorizator.MCR.assertParameters("checkUserIsAuthorizedForPemissionUnit", 0, user,
				previousRecordPermissionUnit.get());

		var recordPermissionUnit = (Optional<String>) recordWithId.MCR
				.assertCalledParametersReturn("getPermissionUnit");
		authorizator.MCR.assertParameters("checkUserIsAuthorizedForPemissionUnit", 1, user,
				recordPermissionUnit.get());

	}

	@Test
	public void testPermissionUnitAuthorizationCheck_positionAfter() {
		setUpRecordTypeUsesPermissionUnits();
		recordStorage.MRV.setThrowException("read", new RuntimeException(), RECORD_TYPE, RECORD_ID);

		try {
			recordUpdater.updateRecord(AUTH_TOKEN, RECORD_TYPE, RECORD_ID, recordWithId);
			fail();
		} catch (Exception _) {
			authorizator.MCR.assertMethodNotCalled("checkUserIsAuthorizedForPemissionUnit");
		}
	}

	@Test
	public void testPermissionUnitAuthorizationCheck_positionBefore() {
		setUpRecordTypeUsesPermissionUnits();
		recordTypeHandlerSpy.MRV.setAlwaysThrowException("getDefinitionId", new RuntimeException());

		try {
			recordUpdater.updateRecord(AUTH_TOKEN, RECORD_TYPE, RECORD_ID, recordWithId);
			fail();
		} catch (Exception _) {
			authorizator.MCR.assertMethodWasCalled("checkUserIsAuthorizedForPemissionUnit");
		}
	}
}
