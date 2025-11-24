/*
 * Copyright 2015, 2016, 2017, 2023, 2024, 2025 Uppsala University Library
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
import static se.uu.ub.cora.spider.extendedfunctionality.ExtendedFunctionalityPosition.CREATE_AFTER_AUTHORIZATION;
import static se.uu.ub.cora.spider.extendedfunctionality.ExtendedFunctionalityPosition.CREATE_AFTER_METADATA_VALIDATION;
import static se.uu.ub.cora.spider.extendedfunctionality.ExtendedFunctionalityPosition.CREATE_BEFORE_ENHANCE;
import static se.uu.ub.cora.spider.extendedfunctionality.ExtendedFunctionalityPosition.CREATE_BEFORE_STORE;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Supplier;

import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import se.uu.ub.cora.beefeater.authentication.User;
import se.uu.ub.cora.bookkeeper.recordtype.Unique;
import se.uu.ub.cora.bookkeeper.validator.DataValidationException;
import se.uu.ub.cora.data.DataProvider;
import se.uu.ub.cora.data.DataRecord;
import se.uu.ub.cora.data.DataRecordGroup;
import se.uu.ub.cora.data.collected.CollectTerms;
import se.uu.ub.cora.data.collected.Link;
import se.uu.ub.cora.data.collected.StorageTerm;
import se.uu.ub.cora.data.spies.DataFactorySpy;
import se.uu.ub.cora.data.spies.DataGroupSpy;
import se.uu.ub.cora.data.spies.DataRecordGroupSpy;
import se.uu.ub.cora.logger.LoggerProvider;
import se.uu.ub.cora.spider.authentication.AuthenticationException;
import se.uu.ub.cora.spider.authorization.AuthorizationException;
import se.uu.ub.cora.spider.dependency.spy.RecordTypeHandlerSpy;
import se.uu.ub.cora.spider.extendedfunctionality.ExtendedFunctionalityData;
import se.uu.ub.cora.spider.extendedfunctionality.ExtendedFunctionalityPosition;
import se.uu.ub.cora.spider.extendedfunctionality.internal.ExtendedFunctionalityProviderSpy;
import se.uu.ub.cora.spider.log.LoggerFactorySpy;
import se.uu.ub.cora.spider.record.ConflictException;
import se.uu.ub.cora.spider.record.DataException;
import se.uu.ub.cora.spider.record.DataGroupToRecordEnhancerSpy;
import se.uu.ub.cora.spider.record.RecordCreator;
import se.uu.ub.cora.spider.spy.DataChangedSenderSpy;
import se.uu.ub.cora.spider.spy.DataGroupTermCollectorSpy;
import se.uu.ub.cora.spider.spy.DataRecordLinkCollectorSpy;
import se.uu.ub.cora.spider.spy.DataValidatorSpy;
import se.uu.ub.cora.spider.spy.RecordIndexerSpy;
import se.uu.ub.cora.spider.spy.SpiderDependencyProviderSpy;
import se.uu.ub.cora.spider.spy.UniqueValidatorSpy;
import se.uu.ub.cora.spider.spy.ValidationAnswerSpy;
import se.uu.ub.cora.storage.RecordConflictException;
import se.uu.ub.cora.storage.spies.RecordStorageSpy;
import se.uu.ub.cora.storage.spies.archive.RecordArchiveSpy;

public class RecordCreatorTest {
	private static final String RECORD_TYPE = "someType";
	private static final String AUTH_TOKEN = "someToken";
	private RecordStorageSpy recordStorage;
	private SpiderAuthorizatorSpy spiderAuthorizator;
	private RecordCreator recordCreator;
	private DataValidatorSpy dataValidator;
	private DataRecordLinkCollectorSpy linkCollector;
	private SpiderDependencyProviderSpy dependencyProviderSpy;
	private ExtendedFunctionalityProviderSpy extendedFunctionalityProvider;
	private DataGroupToRecordEnhancerSpy dataGroupToRecordEnhancer;
	private DataGroupTermCollectorSpy termCollector;
	private UniqueValidatorSpy uniqueValidator;
	private RecordIndexerSpy recordIndexer;
	private LoggerFactorySpy loggerFactorySpy;
	private DataFactorySpy dataFactorySpy;

	private RecordTypeHandlerSpy recordTypeHandlerSpy;
	private DataRedactorSpy dataRedactor;
	private RecordArchiveSpy recordArchive;
	private AuthenticatorSpy authenticator;

	private DataRecordGroupSpy recordWithoutId;
	private DataRecordGroupSpy recordWithId;
	private DataGroupSpy recordInfoWithoutId;

	@BeforeMethod
	public void beforeMethod() {
		setUpFactoriesAndProviders();

		authenticator = new AuthenticatorSpy();
		spiderAuthorizator = new SpiderAuthorizatorSpy();
		dataValidator = new DataValidatorSpy();
		recordStorage = new RecordStorageSpy();
		linkCollector = new DataRecordLinkCollectorSpy();
		extendedFunctionalityProvider = new ExtendedFunctionalityProviderSpy();
		recordIndexer = new RecordIndexerSpy();
		recordArchive = new RecordArchiveSpy();
		dataRedactor = new DataRedactorSpy();
		recordTypeHandlerSpy = new RecordTypeHandlerSpy();
		dataGroupToRecordEnhancer = new DataGroupToRecordEnhancerSpy();
		termCollector = new DataGroupTermCollectorSpy();
		uniqueValidator = new UniqueValidatorSpy();

		setUpDependencyProvider();

		recordTypeHandlerSpy.MRV.setDefaultReturnValuesSupplier("getRecordTypeId",
				() -> RECORD_TYPE);
		recordWithoutId = createRecordExampleRecordWithoutId();
		recordWithId = createDataGroupForCreate();
	}

	private void setUpFactoriesAndProviders() {
		loggerFactorySpy = new LoggerFactorySpy();
		LoggerProvider.setLoggerFactory(loggerFactorySpy);
		dataFactorySpy = new DataFactorySpy();
		DataProvider.onlyForTestSetDataFactory(dataFactorySpy);
	}

	private void setUpDependencyProvider() {
		dependencyProviderSpy = new SpiderDependencyProviderSpy();
		dependencyProviderSpy.MRV.setDefaultReturnValuesSupplier("getRecordStorage",
				() -> recordStorage);
		dependencyProviderSpy.MRV.setDefaultReturnValuesSupplier("getRecordArchive",
				() -> recordArchive);
		dependencyProviderSpy.MRV.setDefaultReturnValuesSupplier("getDataValidator",
				() -> dataValidator);
		dependencyProviderSpy.MRV.setDefaultReturnValuesSupplier("getSpiderAuthorizator",
				() -> spiderAuthorizator);
		dependencyProviderSpy.MRV.setDefaultReturnValuesSupplier("getAuthenticator",
				() -> authenticator);
		dependencyProviderSpy.MRV.setDefaultReturnValuesSupplier("getDataGroupTermCollector",
				() -> termCollector);
		dependencyProviderSpy.MRV.setDefaultReturnValuesSupplier("getUniqueValidator",
				() -> uniqueValidator);
		dependencyProviderSpy.MRV.setDefaultReturnValuesSupplier("getRecordIndexer",
				() -> recordIndexer);
		dependencyProviderSpy.MRV.setDefaultReturnValuesSupplier("getDataRecordLinkCollector",
				() -> linkCollector);
		dependencyProviderSpy.MRV.setDefaultReturnValuesSupplier("getDataRedactor",
				() -> dataRedactor);
		dependencyProviderSpy.MRV.setDefaultReturnValuesSupplier(
				"getRecordTypeHandlerUsingDataRecordGroup", () -> recordTypeHandlerSpy);
		dependencyProviderSpy.MRV.setDefaultReturnValuesSupplier("getExtendedFunctionalityProvider",
				() -> extendedFunctionalityProvider);
		dependencyProviderSpy.MRV.setDefaultReturnValuesSupplier("getDataRedactor",
				() -> dataRedactor);

		recordCreator = RecordCreatorImp.usingDependencyProviderAndDataGroupToRecordEnhancer(
				dependencyProviderSpy, dataGroupToRecordEnhancer);
	}

	private DataRecordGroupSpy createRecordExampleRecordWithoutId() {
		DataRecordGroupSpy recordSpy = new DataRecordGroupSpy();
		recordSpy.MRV.setDefaultReturnValuesSupplier("getType", () -> "someType");
		recordSpy.MRV.setDefaultReturnValuesSupplier("getDataDivider", () -> "someDataDivider");
		recordInfoWithoutId = new DataGroupSpy();
		recordSpy.MRV.setSpecificReturnValuesSupplier("getFirstGroupWithNameInData",
				() -> recordInfoWithoutId, "recordInfo");

		return recordSpy;
	}

	private DataRecordGroupSpy createDataGroupForCreate() {
		DataRecordGroupSpy recordSpy = new DataRecordGroupSpy();
		recordSpy.MRV.setDefaultReturnValuesSupplier("getType", () -> "someType");
		recordSpy.MRV.setDefaultReturnValuesSupplier("getId", () -> "someRecordId");
		recordSpy.MRV.setDefaultReturnValuesSupplier("getDataDivider", () -> "someDataDivider");
		return recordSpy;
	}

	@Test
	public void testOnlyForTestGetDataGroupToRecordEnhancer() {
		RecordCreatorImp creatorImp = (RecordCreatorImp) recordCreator;
		assertSame(creatorImp.onlyForTestGetDataGroupToRecordEnhancer(), dataGroupToRecordEnhancer);
	}

	@Test
	public void testRecordTypeHandlerFetchedFromDependencyProvider() {
		recordCreator.createAndStoreRecord(AUTH_TOKEN, RECORD_TYPE, recordWithoutId);

		dependencyProviderSpy.MCR.assertParameters("getRecordTypeHandlerUsingDataRecordGroup", 0,
				recordWithoutId);
	}

	@Test(expectedExceptions = DataException.class, expectedExceptionsMessageRegExp = "The record "
			+ "cannot be created because the record type provided does not match the record type "
			+ "that the validation type is set to validate.")
	public void testRecordTypePassedNOTEqualsTheLinkedInValidationType() {
		recordTypeHandlerSpy.MRV.setDefaultReturnValuesSupplier("getRecordTypeId",
				() -> "NOTRecordTypeId");

		dependencyProviderSpy.MRV.setDefaultReturnValuesSupplier(
				"getRecordTypeHandlerUsingDataRecordGroup", () -> recordTypeHandlerSpy);

		recordCreator.createAndStoreRecord(AUTH_TOKEN, RECORD_TYPE, recordWithoutId);
	}

	@Test(expectedExceptions = DataException.class, expectedExceptionsMessageRegExp = ""
			+ "Data is not valid: some message")
	public void testValidationTypeDoesNotExist() {
		recordTypeHandlerSpy.MRV.setAlwaysThrowException("getCreateDefinitionId",
				DataValidationException.withMessage("some message"));

		dependencyProviderSpy.MRV.setDefaultReturnValuesSupplier(
				"getRecordTypeHandlerUsingDataRecordGroup", () -> recordTypeHandlerSpy);

		recordCreator.createAndStoreRecord(AUTH_TOKEN, RECORD_TYPE, recordWithoutId);
	}

	@Test
	public void testAuthenticatorIsCalled() {
		recordTypeHandlerSpy.MRV.setDefaultReturnValuesSupplier("getRecordTypeId",
				() -> RECORD_TYPE);

		recordCreator.createAndStoreRecord(AUTH_TOKEN, RECORD_TYPE, recordWithoutId);

		authenticator.MCR.assertParameters("getUserForToken", 0, AUTH_TOKEN);
	}

	@Test
	public void testAuthorizatorIsCalled() {
		recordCreator.createAndStoreRecord(AUTH_TOKEN, RECORD_TYPE, recordWithoutId);

		User user = getAuthenticatedUser();
		spiderAuthorizator.MCR.assertParameters("checkUserIsAuthorizedForActionOnRecordType", 0,
				user, "create", RECORD_TYPE);
	}

	private User getAuthenticatedUser() {
		return (User) authenticator.MCR.getReturnValue("getUserForToken", 0);
	}

	@Test
	public void testNotPossibleToCreateRecordWithTypeAndIdWhichAlreadyExists() {
		recordStorage.MRV.setSpecificReturnValuesSupplier("recordExists",
				(Supplier<Boolean>) () -> true, List.of(RECORD_TYPE), recordWithId.getId());
		try {
			recordCreator.createAndStoreRecord(AUTH_TOKEN, RECORD_TYPE, recordWithId);
			fail("It should thow exception");
		} catch (Exception e) {
			assertTrue(e instanceof ConflictException);
			assertEquals(e.getMessage(), "Record "
					+ "with type: someType and id: someRecordId already exists in storage");

			recordTypeHandlerSpy.MCR.assertMethodNotCalled("getParentId");
		}
	}

	@Test
	public void testExtendedFunctionalityIsCalled() {
		recordCreator.createAndStoreRecord(AUTH_TOKEN, RECORD_TYPE, recordWithId);

		ExtendedFunctionalityData expectedData = new ExtendedFunctionalityData();
		expectedData.recordType = RECORD_TYPE;
		expectedData.recordId = null;
		expectedData.authToken = AUTH_TOKEN;
		expectedData.user = getAuthenticatedUser();
		expectedData.previouslyStoredDataRecordGroup = null;
		expectedData.dataRecordGroup = recordWithId;

		extendedFunctionalityProvider.assertCallToPositionAndFunctionalityCalledWithData(
				CREATE_AFTER_AUTHORIZATION, expectedData, 0);
		extendedFunctionalityProvider.assertCallToPositionAndFunctionalityCalledWithData(
				CREATE_AFTER_METADATA_VALIDATION, expectedData, 1);
		expectedData.recordId = "someRecordId";

		extendedFunctionalityProvider.assertCallToPositionAndFunctionalityCalledWithData(
				CREATE_BEFORE_STORE, expectedData, 2);

		extendedFunctionalityProvider.assertCallToPositionAndFunctionalityCalledWithData(
				CREATE_BEFORE_ENHANCE, expectedData, 3);
		extendedFunctionalityProvider.MCR
				.assertNumberOfCallsToMethod("getFunctionalityForPositionAndRecordType", 4);
	}

	@Test
	public void testCreateBeforeStorePosition() {
		recordStorage.MRV.setAlwaysThrowException("create", new RuntimeException("someError"));

		try {
			recordCreator.createAndStoreRecord(AUTH_TOKEN, RECORD_TYPE, recordWithId);
		} catch (Exception e) {
			assertEquals(e.getMessage(), "someError");

			uniqueValidator.MCR.assertMethodWasCalled("validateUniqueForNewRecord");
			extendedFunctionalityProvider.MCR.assertParameter(
					"getFunctionalityForPositionAndRecordType", 2, "position", CREATE_BEFORE_STORE);
			recordTypeHandlerSpy.MCR.assertMethodNotCalled("storeInArchive");
		}
	}

	@Test
	public void testCreateBeforeStorePositionAfterValidateUnique() {
		uniqueValidator.MRV.setAlwaysThrowException("validateUniqueForNewRecord",
				new RuntimeException("someError"));

		try {
			recordCreator.createAndStoreRecord(AUTH_TOKEN, RECORD_TYPE, recordWithId);
		} catch (Exception e) {
			assertEquals(e.getMessage(), "someError");
			assertNumberOfCallsAndExtFuncPositionWasNotCalled(2, CREATE_BEFORE_STORE);
		}
	}

	private void assertNumberOfCallsAndExtFuncPositionWasNotCalled(int expectedCalls,
			ExtendedFunctionalityPosition position) {
		extendedFunctionalityProvider.MCR.assertNumberOfCallsToMethod(
				"getFunctionalityForPositionAndRecordType", expectedCalls);

		for (int i = 0; i < expectedCalls; i++) {
			Map<String, Object> parametersUsed = extendedFunctionalityProvider.MCR
					.getParametersForMethodAndCallNumber("getFunctionalityForPositionAndRecordType",
							i);
			if (position.equals(parametersUsed.get("position"))) {
				fail("Position " + position + " was not expected to be called.");
			}
		}
	}

	@Test
	public void testExtendedFunctionalityAfterAuthorizationCalledBeforeRecordTypeHandlerCreatedSoWeDoNotNeedToHaveARecordInfoForSomeTypes() {
		extendedFunctionalityProvider.setUpExtendedFunctionalityToThrowExceptionOnPosition(
				dependencyProviderSpy, CREATE_AFTER_AUTHORIZATION, RECORD_TYPE);
		recordCreator = RecordCreatorImp.usingDependencyProviderAndDataGroupToRecordEnhancer(
				dependencyProviderSpy, dataGroupToRecordEnhancer);
		try {
			recordCreator.createAndStoreRecord(AUTH_TOKEN, RECORD_TYPE, recordWithoutId);

			fail("Should fail as we want to stop execution when the extended functionality is used,"
					+ " to determin that the extended functionality is called in the correct place"
					+ " in the code");
		} catch (Exception e) {
			SpiderAuthorizatorSpy authorizator = getCorrectAuthorizator();
			authorizator.MCR.assertMethodWasCalled("checkUserIsAuthorizedForActionOnRecordType");
			dataFactorySpy.MCR.assertMethodNotCalled("factorRecordGroupFromDataGroup");
			dependencyProviderSpy.MCR
					.assertMethodNotCalled("getRecordTypeHandlerUsingDataRecordGroup");
		}
	}

	private SpiderAuthorizatorSpy getCorrectAuthorizator() {
		int callNumberIsOneAsRecordCreatorIsCreatedTwiceOneInSetUpAndOneHereWithNewDependencies = 1;
		return (SpiderAuthorizatorSpy) dependencyProviderSpy.MCR.getReturnValue(
				"getSpiderAuthorizator",
				callNumberIsOneAsRecordCreatorIsCreatedTwiceOneInSetUpAndOneHereWithNewDependencies);
	}

	@Test
	public void testRecordEnhancerCalled() {
		recordCreator.createAndStoreRecord(AUTH_TOKEN, RECORD_TYPE, recordWithoutId);

		User user = getAuthenticatedUser();
		dataGroupToRecordEnhancer.MCR.assertParameters("enhanceIgnoringReadAccess", 0, user,
				RECORD_TYPE, recordWithoutId, dataRedactor);
	}

	@Test(expectedExceptions = AuthenticationException.class, expectedExceptionsMessageRegExp = ""
			+ "someMessage")
	public void testGetActiveUserFails() {
		authenticator.MRV.setAlwaysThrowException("getUserForToken",
				new AuthenticationException("someMessage"));

		recordCreator.createAndStoreRecord(AUTH_TOKEN, RECORD_TYPE, recordWithoutId);
	}

	@Test
	public void testIndexerIsCalled() {
		recordCreator.createAndStoreRecord(AUTH_TOKEN, RECORD_TYPE, recordWithId);

		CollectTerms collectTerms = (CollectTerms) termCollector.MCR.getReturnValue("collectTerms",
				1);
		recordIndexer.MCR.assertParameters("indexData", 0, RECORD_TYPE, "someRecordId",
				collectTerms.indexTerms, recordWithId);
	}

	@Test(expectedExceptions = DataException.class, expectedExceptionsMessageRegExp = ""
			+ "Data is not valid: \\[Error1, Error2\\]")
	public void testX() {
		setUpDataValidatorToReturnInvalidWithErrors();
		recordCreator = RecordCreatorImp.usingDependencyProviderAndDataGroupToRecordEnhancer(
				dependencyProviderSpy, dataGroupToRecordEnhancer);

		recordCreator.createAndStoreRecord(AUTH_TOKEN, RECORD_TYPE, recordWithoutId);
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

	@Test
	public void testCreateRecordAutogeneratedId() {
		recordTypeHandlerSpy.MRV.setDefaultReturnValuesSupplier("shouldAutoGenerateId", () -> true);
		recordTypeHandlerSpy.MRV.setDefaultReturnValuesSupplier("getNextId",
				() -> "someGeneratedRecordId");

		recordCreator.createAndStoreRecord(AUTH_TOKEN, RECORD_TYPE, recordWithoutId);

		recordWithoutId.MCR.assertCalledParameters("setId", "someGeneratedRecordId");
		recordWithoutId.MCR.assertCalledParameters("setCreatedBy", "userSpy");
		recordWithoutId.MCR.assertCalledParameters("setTsCreatedToNow");
		recordWithoutId.MCR.assertCalledParameters("addUpdatedUsingUserIdAndTs", "userSpy",
				recordWithoutId.MCR.getReturnValue("getTsCreated", 0));
	}

	@Test
	public void testAutogeneratedIdSentToStorageUsingGeneratedId() {
		recordTypeHandlerSpy.MRV.setDefaultReturnValuesSupplier("shouldAutoGenerateId", () -> true);
		recordTypeHandlerSpy.MRV.setDefaultReturnValuesSupplier("getNextId",
				() -> "someGeneratedRecordId");

		recordCreator.createAndStoreRecord(AUTH_TOKEN, RECORD_TYPE, recordWithoutId);

		recordWithoutId.MCR.assertParameters("setId", 0, "someGeneratedRecordId");
	}

	@Test
	public void testCorrectRecordInfoInCreatedRecord() {
		recordTypeHandlerSpy.MRV.setDefaultReturnValuesSupplier("shouldAutoGenerateId",
				() -> false);

		recordCreator.createAndStoreRecord(AUTH_TOKEN, RECORD_TYPE, recordWithoutId);

		recordWithoutId.MCR.assertMethodNotCalled("setId");
		recordWithoutId.MCR.assertCalledParameters("setCreatedBy", "userSpy");
		recordWithoutId.MCR.assertCalledParameters("setTsCreatedToNow");
		recordWithoutId.MCR.assertCalledParameters("addUpdatedUsingUserIdAndTs", "userSpy",
				recordWithoutId.MCR.getReturnValue("getTsCreated", 0));
	}

	@Test
	public void testCreateRecordDataDividerExtractedFromData() {
		recordCreator.createAndStoreRecord(AUTH_TOKEN, RECORD_TYPE, recordWithoutId);

		recordStorage.MCR.assertParameter("create", 0, "dataDivider", "someDataDivider");
	}

	@Test
	public void testCallCollectTermWithCorrectValues() {
		recordCreator.createAndStoreRecord(AUTH_TOKEN, RECORD_TYPE, recordWithId);

		termCollector.MCR.assertNumberOfCallsToMethod("collectTerms", 2);
		termCollector.MCR.assertParameters("collectTerms", 0,
				"fakeDefMetadataIdFromRecordTypeHandlerSpy", recordWithId);
		termCollector.MCR.assertParameters("collectTerms", 1,
				"fakeDefMetadataIdFromRecordTypeHandlerSpy", recordWithId);
	}

	@Test
	public void testCreateRecordCollectedTermsSentToStorage() {
		recordCreator.createAndStoreRecord(AUTH_TOKEN, RECORD_TYPE, recordWithId);

		CollectTerms collectTerms = (CollectTerms) termCollector.MCR.getReturnValue("collectTerms",
				1);
		var collectedLinks = linkCollector.MCR.getReturnValue("collectLinks", 0);
		var recordAsDataGroup = dataFactorySpy.MCR.getReturnValue("factorGroupFromDataRecordGroup",
				2);
		recordStorage.MCR.assertParameters("create", 0, RECORD_TYPE, "someRecordId",
				recordAsDataGroup, collectTerms.storageTerms, collectedLinks, "someDataDivider");
	}

	@Test(expectedExceptions = AuthorizationException.class)
	public void testCreateRecordUnauthorizedForActionCreate() {
		spiderAuthorizator.MRV.setAlwaysThrowException("checkUserIsAuthorizedForActionOnRecordType",
				new AuthorizationException("message"));

		recordCreator.createAndStoreRecord(AUTH_TOKEN, RECORD_TYPE, recordWithoutId);
	}

	@Test
	public void testUnauthorizedForCreateOnRecordTypeShouldShouldNotAccessStorage() {
		spiderAuthorizator.MRV.setAlwaysThrowException(
				"checkGetUsersMatchedRecordPartPermissionsForActionOnRecordTypeAndCollectedData",
				new AuthorizationException("message"));

		try {
			recordCreator.createAndStoreRecord(AUTH_TOKEN, RECORD_TYPE, recordWithoutId);
			fail("It should fail with an AuthorizationException");
		} catch (Exception e) {
			assertEquals(e.getClass(), AuthorizationException.class);
		}
		recordStorage.MCR.assertMethodNotCalled("read");
		recordStorage.MCR.assertMethodNotCalled("update");
		recordStorage.MCR.assertMethodNotCalled("delete");
		recordStorage.MCR.assertMethodNotCalled("create");

		extendedFunctionalityProvider.MCR.assertNumberOfCallsToMethod(
				"getFunctionalityForCreateBeforeMetadataValidation", 0);
	}

	@Test(expectedExceptions = AuthorizationException.class, expectedExceptionsMessageRegExp = ""
			+ "someMessage")
	public void testCreateRecordUnauthorizedForDataInRecord() {
		spiderAuthorizator.MRV.setAlwaysThrowException(
				"checkGetUsersMatchedRecordPartPermissionsForActionOnRecordTypeAndCollectedData",
				new AuthorizationException("someMessage"));

		recordCreator.createAndStoreRecord(AUTH_TOKEN, RECORD_TYPE, recordWithoutId);
	}

	@Test

	public void testCreateRecordCollectedDataUsedForAuthorization() {
		recordCreator.createAndStoreRecord(AUTH_TOKEN, RECORD_TYPE, recordWithoutId);

		String methodName = "checkGetUsersMatchedRecordPartPermissionsForActionOnRecordTypeAndCollectedData";
		CollectTerms collectTerms = (CollectTerms) termCollector.MCR.getReturnValue("collectTerms",
				0);
		var returnedUser = authenticator.MCR.getReturnValue("getUserForToken", 0);
		spiderAuthorizator.MCR.assertParameters(methodName, 0, returnedUser, "create", RECORD_TYPE,
				collectTerms.permissionTerms);
	}

	@Test(expectedExceptions = RecordConflictException.class)
	public void testCreateRecordDuplicateUserSuppliedId() {
		recordStorage.MRV.setAlwaysThrowException("create", RecordConflictException
				.withMessage("Record with recordId: " + "somePlace" + " already exists"));

		recordCreator.createAndStoreRecord(AUTH_TOKEN, RECORD_TYPE, recordWithoutId);
	}

	@Test(expectedExceptions = DataException.class, expectedExceptionsMessageRegExp = ""
			+ "Data is not valid: linkedRecord does not exists in storage for "
			+ "recordType: toType and recordId: toId")
	public void testErrorThrownIfCollectedLinkDoesNotExistInStorage() {
		setupLinkCollectorToReturnALinkSoThatWeCheckThatTheLinkedRecordExistsInStorage();

		recordStorage.MRV.setSpecificReturnValuesSupplier("recordExists", () -> false,
				List.of("toType"), "toId");
		recordStorage.MRV.setSpecificReturnValuesSupplier("recordExists", () -> false,
				List.of(RECORD_TYPE), recordWithoutId);

		recordCreator.createAndStoreRecord(AUTH_TOKEN, RECORD_TYPE, recordWithoutId);
	}

	private void setupLinkCollectorToReturnALinkSoThatWeCheckThatTheLinkedRecordExistsInStorage() {
		Link link = new Link("toType", "toId");
		linkCollector.MRV.setDefaultReturnValuesSupplier("collectLinks", () -> Set.of(link));
	}

	@Test
	public void testLinkedRecordIdExists() {
		setupLinkCollectorToReturnALinkSoThatWeCheckThatTheLinkedRecordExistsInStorage();
		recordStorage.MRV.setSpecificReturnValuesSupplier("recordExists", () -> true,
				List.of("toType"), "toId");
		recordStorage.MRV.setSpecificReturnValuesSupplier("recordExists", () -> true,
				List.of(RECORD_TYPE), recordWithoutId);

		recordCreator.createAndStoreRecord(AUTH_TOKEN, RECORD_TYPE, recordWithoutId);

		List<?> types = (List<?>) recordStorage.MCR
				.getParameterForMethodAndCallNumberAndParameter("recordExists", 0, "types");
		assertEquals(types.size(), 1);
		assertEquals(types.get(0), "toType");
		recordStorage.MCR.assertParameter("recordExists", 0, "id", "toId");
	}

	@Test
	public void testReturnRecordWithoutReadActionIfUserHasCreateButNotReadAccess() {
		dataGroupToRecordEnhancer.addReadAction = false;

		DataRecord createdRecord = recordCreator.createAndStoreRecord(AUTH_TOKEN, RECORD_TYPE,
				recordWithoutId);

		dataGroupToRecordEnhancer.MCR.assertReturn("enhanceIgnoringReadAccess", 0, createdRecord);
	}

	@Test
	public void testRedactorNotCalledWhenNoRecordPartConstraints() {
		recordCreator.createAndStoreRecord(AUTH_TOKEN, RECORD_TYPE, recordWithoutId);

		recordTypeHandlerSpy.MCR.assertMethodWasCalled("hasRecordPartCreateConstraint");
		dataRedactor.MCR.assertMethodNotCalled("removeChildrenForConstraintsWithoutPermissions");
	}

	@Test
	public void testRedactorCalledCorrectlyWhenRecordPartConstraints() {
		setupForRecordPartConstraints();

		recordCreator.createAndStoreRecord(AUTH_TOKEN, RECORD_TYPE, recordWithoutId);

		recordTypeHandlerSpy.MCR.assertMethodWasCalled("hasRecordPartCreateConstraint");

		assertDataRedactorRemoveChildrenForConstraintsWithoutPermissions(recordWithoutId);
	}

	private void setupForRecordPartConstraints() {
		recordTypeHandlerSpy.MRV.setDefaultReturnValuesSupplier("hasRecordPartCreateConstraint",
				() -> true);
	}

	private void assertDataRedactorRemoveChildrenForConstraintsWithoutPermissions(
			DataRecordGroup dataGroup) {
		Set<?> recordPartWriteConstraints = (Set<?>) recordTypeHandlerSpy.MCR
				.getReturnValue("getCreateWriteRecordPartConstraints", 0);
		Set<?> writePermissions = (Set<?>) spiderAuthorizator.MCR.getReturnValue(
				"checkGetUsersMatchedRecordPartPermissionsForActionOnRecordTypeAndCollectedData",
				0);
		dataRedactor.MCR.assertParameters("removeChildrenForConstraintsWithoutPermissions", 0,
				recordTypeHandlerSpy.getDefinitionId(), dataGroup, recordPartWriteConstraints,
				writePermissions);
	}

	@Test
	public void testValidateIsNotCalledWithRedactedDataGroupWhenNoRecordPartConstraints() {
		recordCreator.createAndStoreRecord(AUTH_TOKEN, RECORD_TYPE, recordWithoutId);

		String newMetadataId = (String) recordTypeHandlerSpy.MCR
				.getReturnValue("getCreateDefinitionId", 0);
		var recordAsDataGroup = dataFactorySpy.MCR.getReturnValue("factorGroupFromDataRecordGroup",
				0);
		dataValidator.MCR.assertParameters("validateData", 0, newMetadataId, recordAsDataGroup);
	}

	@Test
	public void testValidateIsCalledWithRedactedDataGroupWhenRecordPartConstraints() {
		setupForRecordPartConstraints();

		recordCreator.createAndStoreRecord(AUTH_TOKEN, RECORD_TYPE, recordWithoutId);

		DataRecordGroup redactedDataGroup = (DataRecordGroup) dataRedactor.MCR
				.getReturnValue("removeChildrenForConstraintsWithoutPermissions", 0);
		String newMetadataId = (String) recordTypeHandlerSpy.MCR
				.getReturnValue("getCreateDefinitionId", 0);
		Object redactedAsDataGroup = dataFactorySpy.MCR
				.assertCalledParametersReturn("factorGroupFromDataRecordGroup", redactedDataGroup);
		dataValidator.MCR.assertParameters("validateData", 0, newMetadataId, redactedAsDataGroup);
	}

	@Test
	public void testCallSendDataChanged() {
		recordCreator.createAndStoreRecord(AUTH_TOKEN, RECORD_TYPE, recordWithId);

		var sender = getDataChangedSender();
		sender.MCR.assertParameters("sendDataChanged", 0, RECORD_TYPE, "someRecordId", "create");
	}

	private DataChangedSenderSpy getDataChangedSender() {
		return (DataChangedSenderSpy) dependencyProviderSpy.MCR
				.assertCalledParametersReturn("getDataChangeSender");
	}

	@Test
	public void testSendDataChangedAfterStoreInStorage() {
		recordStorage.MRV.setAlwaysThrowException("create", new RuntimeException("someError"));

		try {
			recordCreator.createAndStoreRecord(AUTH_TOKEN, RECORD_TYPE, recordWithId);
			fail();
		} catch (Exception e) {
			dependencyProviderSpy.MCR.assertMethodNotCalled("getDataChangeSender");
		}
	}

	@Test
	public void testSendDataChangedBeforeStoreInArchive() {
		recordTypeHandlerSpy.MRV.setDefaultReturnValuesSupplier("storeInArchive", () -> true);
		recordArchive.MRV.setAlwaysThrowException("create", new RuntimeException("someError"));

		try {
			recordCreator.createAndStoreRecord(AUTH_TOKEN, RECORD_TYPE, recordWithId);
			fail();
		} catch (Exception e) {
			dependencyProviderSpy.MCR.assertMethodWasCalled("getDataChangeSender");
		}
	}

	@Test
	public void testStoreInArchiveTrue() {
		recordTypeHandlerSpy.MRV.setDefaultReturnValuesSupplier("storeInArchive", () -> true);

		recordCreator.createAndStoreRecord(AUTH_TOKEN, RECORD_TYPE, recordWithId);

		var recordAsDataGroup = dataFactorySpy.MCR.getReturnValue("factorGroupFromDataRecordGroup",
				2);
		recordArchive.MCR.assertParameters("create", 0, "someDataDivider", RECORD_TYPE,
				"someRecordId", recordAsDataGroup);
	}

	@Test
	public void testStoreInArchiveFalse() {
		recordCreator.createAndStoreRecord(AUTH_TOKEN, RECORD_TYPE, recordWithId);

		recordArchive.MCR.assertMethodNotCalled("create");
	}

	@Test
	public void testRecordCreatorGetsUniqueValiadatorFromDependencyProvider() {
		recordCreator.createAndStoreRecord(AUTH_TOKEN, RECORD_TYPE, recordWithId);

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

		recordCreator.createAndStoreRecord(AUTH_TOKEN, RECORD_TYPE, recordWithId);

		uniqueValidator.MCR.assertMethodWasCalled("validateUniqueForNewRecord");
		uniqueValidator.MCR.assertParameters("validateUniqueForNewRecord", 0, RECORD_TYPE,
				uniqueList, collectTerms.storageTerms);
	}

	@Test
	public void testUniqueValidationFails_throwsSpiderConflictException() {
		setupUniqueValidatorToReturnInvalidAnswerWithThreeErrors();
		try {
			recordCreator.createAndStoreRecord(AUTH_TOKEN, RECORD_TYPE, recordWithId);
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
		uniqueValidator.MRV.setDefaultReturnValuesSupplier("validateUniqueForNewRecord",
				() -> validationAnswer);
	}

	@Test
	public void testVisibilityIsSetIfMissing() {
		recordTypeHandlerSpy.MRV.setDefaultReturnValuesSupplier("useVisibility", () -> true);

		recordWithoutId.MRV.setDefaultReturnValuesSupplier("getVisibility", Optional::empty);

		recordCreator.createAndStoreRecord(AUTH_TOKEN, RECORD_TYPE, recordWithoutId);

		recordWithoutId.MCR.assertParameters("setVisibility", 0, "unpublished");
		recordWithoutId.MCR.assertMethodWasCalled("setTsVisibilityNow");
	}

	@Test
	public void testVisibilityIsNotSetToDefaultIfPresent() {
		recordTypeHandlerSpy.MRV.setDefaultReturnValuesSupplier("useVisibility", () -> true);

		recordWithoutId.MRV.setDefaultReturnValuesSupplier("getVisibility",
				() -> Optional.of("hidden"));

		recordCreator.createAndStoreRecord(AUTH_TOKEN, RECORD_TYPE, recordWithoutId);

		recordWithoutId.MCR.assertMethodNotCalled("setVisibility");
		recordWithoutId.MCR.assertMethodWasCalled("setTsVisibilityNow");
	}

	@Test
	public void testUseVisibilityFalse() {
		recordTypeHandlerSpy.MRV.setDefaultReturnValuesSupplier("useVisibility", () -> false);

		recordCreator.createAndStoreRecord(AUTH_TOKEN, RECORD_TYPE, recordWithoutId);

		recordWithoutId.MCR.assertMethodNotCalled("getVisibility");
		recordWithoutId.MCR.assertMethodNotCalled("setVisibility");
		recordWithoutId.MCR.assertMethodNotCalled("setTsVisibility");
	}

	@Test(expectedExceptions = AuthorizationException.class, expectedExceptionsMessageRegExp = ""
			+ "someExceptionFromSpy")
	public void testPermissionUnitAuthorizationCheck_usePermissionUnit_NotAuthorized() {
		setUpRecordTypeUsesPermissionUnits();
		spiderAuthorizator.MRV.setAlwaysThrowException("checkUserIsAuthorizedForPemissionUnit",
				new AuthorizationException("someExceptionFromSpy"));

		recordCreator.createAndStoreRecord(AUTH_TOKEN, RECORD_TYPE, recordWithoutId);
	}

	@Test
	public void testPermissionUnitAuthorizationCheck_DoNotUsePermissionUnit() {
		recordTypeHandlerSpy.MRV.setDefaultReturnValuesSupplier("usePermissionUnit", () -> false);

		recordCreator.createAndStoreRecord(AUTH_TOKEN, RECORD_TYPE, recordWithoutId);

		spiderAuthorizator.MCR.assertMethodNotCalled("checkUserIsAuthorizedForPemissionUnit");
	}

	@Test(expectedExceptions = DataException.class, expectedExceptionsMessageRegExp = ""
			+ "PermissionUnit is missing in the record.")
	public void testPermissionUnitAuthorizationCheck_usePermissionUnit_missingPermissionUnitInRecord() {
		recordTypeHandlerSpy.MRV.setDefaultReturnValuesSupplier("usePermissionUnit", () -> true);
		recordWithoutId.MRV.setDefaultReturnValuesSupplier("getPermissionUnit", Optional::empty);

		recordCreator.createAndStoreRecord(AUTH_TOKEN, RECORD_TYPE, recordWithoutId);
	}

	@Test
	public void testPermissionUnitAuthorizationCheck_usePermissionUnit_isAuthorized() {
		setUpRecordTypeUsesPermissionUnits();

		recordCreator.createAndStoreRecord(AUTH_TOKEN, RECORD_TYPE, recordWithoutId);

		User user = getAuthenticatedUser();
		recordTypeHandlerSpy.MCR.assertMethodWasCalled("usePermissionUnit");
		Optional<String> recordPermissionUnit = assertAndReturnGetPermissionUnit();
		spiderAuthorizator.MCR.assertParameters("checkUserIsAuthorizedForPemissionUnit", 0, user,
				recordPermissionUnit.get());
	}

	@SuppressWarnings("unchecked")
	private Optional<String> assertAndReturnGetPermissionUnit() {
		return (Optional<String>) recordWithoutId.MCR
				.assertCalledParametersReturn("getPermissionUnit");
	}

	@Test
	public void testPermissionUnitAuthorizationCheck_positionAfter() {
		setUpRecordTypeUsesPermissionUnits();
		recordTypeHandlerSpy.MRV.setAlwaysThrowException("getRecordTypeId", new RuntimeException());

		try {
			recordCreator.createAndStoreRecord(AUTH_TOKEN, RECORD_TYPE, recordWithoutId);
			fail();
		} catch (Exception _) {
			spiderAuthorizator.MCR.assertMethodNotCalled("checkUserIsAuthorizedForPemissionUnit");
		}
	}

	@Test
	public void testPermissionUnitAuthorizationCheck_positionBefore() {
		setUpRecordTypeUsesPermissionUnits();
		recordTypeHandlerSpy.MRV.setAlwaysThrowException("getDefinitionId", new RuntimeException());

		try {
			recordCreator.createAndStoreRecord(AUTH_TOKEN, RECORD_TYPE, recordWithoutId);
			fail();
		} catch (Exception _) {
			spiderAuthorizator.MCR.assertMethodWasCalled("checkUserIsAuthorizedForPemissionUnit");
		}
	}

	private void setUpRecordTypeUsesPermissionUnits() {
		recordTypeHandlerSpy.MRV.setDefaultReturnValuesSupplier("usePermissionUnit", () -> true);
		recordWithoutId.MRV.setDefaultReturnValuesSupplier("getPermissionUnit",
				() -> Optional.of("permissionUnit001"));
	}

	@Test(expectedExceptions = DataException.class, expectedExceptionsMessageRegExp = ""
			+ "To use the trash bin function, you must first activate it on the record type.")
	public void testTrashBin_NotUsed_But_RecordSetsInTrashBin() {
		recordTypeHandlerSpy.MRV.setDefaultReturnValuesSupplier("useTrashBin", () -> false);
		recordWithoutId.MRV.setDefaultReturnValuesSupplier("isInTrashBin",
				() -> Optional.of(Boolean.FALSE));

		recordCreator.createAndStoreRecord(AUTH_TOKEN, RECORD_TYPE, recordWithoutId);
	}

	@Test
	public void testTrashBin_UseTrashBinAndIsInTrashNoSet_DefaultToNotInTrash() {
		recordTypeHandlerSpy.MRV.setDefaultReturnValuesSupplier("useTrashBin", () -> true);
		recordWithoutId.MRV.setDefaultReturnValuesSupplier("isInTrashBin", Optional::empty);

		DataRecord createdRecord = recordCreator.createAndStoreRecord(AUTH_TOKEN, RECORD_TYPE,
				recordWithoutId);

		recordWithoutId.MCR.assertParameters("setInTrashBin", 0, false);
		assertEquals(createdRecord.getDataRecordGroup(), recordWithoutId);
	}
}