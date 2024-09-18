/*
 * Copyright 2019, 2024 Uppsala University Library
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
import static org.testng.Assert.fail;
import static se.uu.ub.cora.spider.extendedfunctionality.ExtendedFunctionalityPosition.VALIDATE_AFTER_AUTHORIZATION;

import java.util.List;
import java.util.Set;

import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import se.uu.ub.cora.beefeater.authentication.User;
import se.uu.ub.cora.bookkeeper.recordtype.Unique;
import se.uu.ub.cora.data.Action;
import se.uu.ub.cora.data.DataGroup;
import se.uu.ub.cora.data.DataProvider;
import se.uu.ub.cora.data.DataRecord;
import se.uu.ub.cora.data.DataRecordLink;
import se.uu.ub.cora.data.collected.CollectTerms;
import se.uu.ub.cora.data.collected.Link;
import se.uu.ub.cora.data.collected.StorageTerm;
import se.uu.ub.cora.data.spies.DataAtomicSpy;
import se.uu.ub.cora.data.spies.DataFactorySpy;
import se.uu.ub.cora.data.spies.DataGroupSpy;
import se.uu.ub.cora.data.spies.DataRecordGroupSpy;
import se.uu.ub.cora.data.spies.DataRecordLinkSpy;
import se.uu.ub.cora.data.spies.DataRecordSpy;
import se.uu.ub.cora.logger.LoggerProvider;
import se.uu.ub.cora.spider.authorization.AuthorizationException;
import se.uu.ub.cora.spider.dependency.SpiderInstanceFactory;
import se.uu.ub.cora.spider.dependency.SpiderInstanceFactoryImp;
import se.uu.ub.cora.spider.dependency.SpiderInstanceProvider;
import se.uu.ub.cora.spider.dependency.spy.RecordIdGeneratorSpy;
import se.uu.ub.cora.spider.dependency.spy.RecordTypeHandlerSpy;
import se.uu.ub.cora.spider.extendedfunctionality.ExtendedFunctionalityData;
import se.uu.ub.cora.spider.extendedfunctionality.internal.ExtendedFunctionalityProviderSpy;
import se.uu.ub.cora.spider.log.LoggerFactorySpy;
import se.uu.ub.cora.spider.record.DataException;
import se.uu.ub.cora.spider.record.RecordValidator;
import se.uu.ub.cora.spider.spy.DataGroupTermCollectorSpy;
import se.uu.ub.cora.spider.spy.DataRecordLinkCollectorSpy;
import se.uu.ub.cora.spider.spy.DataValidatorSpy;
import se.uu.ub.cora.spider.spy.SpiderDependencyProviderSpy;
import se.uu.ub.cora.spider.spy.UniqueValidatorSpy;
import se.uu.ub.cora.spider.spy.ValidationAnswerSpy;
import se.uu.ub.cora.storage.RecordNotFoundException;
import se.uu.ub.cora.storage.spies.RecordStorageSpy;

public class RecordValidatorTest {
	private static final String RECORD_ID = "someRecordId";
	private static final String RECORD_TYPE = "spyType";
	private static final String RECORD_TYPE_TO_VALIDATE_AGAINST = "fakeRecordTypeIdFromRecordTypeHandlerSpy";
	private static final String VALIDATION_ORDER_TYPE = "validationOrder";
	private static final String SOME_AUTH_TOKEN = "someToken78678567";
	private RecordValidator recordValidator;
	private RecordStorageSpy recordStorage;
	private AuthenticatorSpy authenticator;
	private SpiderAuthorizatorSpy authorizator;
	private DataValidatorSpy dataValidator;
	private DataRecordLinkCollectorSpy linkCollector;
	private SpiderDependencyProviderSpy dependencyProvider;
	private ExtendedFunctionalityProviderSpy extendedFunctionalityProvider;
	private RecordIdGeneratorSpy idGenerator;
	private LoggerFactorySpy loggerFactorySpy;
	private DataFactorySpy dataFactorySpy;
	private DataGroupTermCollectorSpy termCollector;
	private UniqueValidatorSpy uniqueValidator;

	private RecordTypeHandlerSpy recordTypeHandler;
	private DataRecordGroupSpy recordToValidate;
	private User currentUser;

	@BeforeMethod
	public void beforeMethod() {
		setUpFactoriesAndProviders();
		authenticator = new AuthenticatorSpy();
		authorizator = new SpiderAuthorizatorSpy();
		dataValidator = new DataValidatorSpy();
		recordStorage = new RecordStorageSpy();
		linkCollector = new DataRecordLinkCollectorSpy();
		extendedFunctionalityProvider = new ExtendedFunctionalityProviderSpy();
		recordTypeHandler = new RecordTypeHandlerSpy();
		idGenerator = new RecordIdGeneratorSpy();
		termCollector = new DataGroupTermCollectorSpy();
		uniqueValidator = new UniqueValidatorSpy();
		setUpDependencyProvider();
		recordToValidate = createRecordToValidate();
		currentUser = new User("someUserId");

		authenticator.MRV.setDefaultReturnValuesSupplier("getUserForToken", () -> currentUser);

	}

	private void setUpFactoriesAndProviders() {
		loggerFactorySpy = new LoggerFactorySpy();
		LoggerProvider.setLoggerFactory(loggerFactorySpy);
		dataFactorySpy = new DataFactorySpy();
		DataProvider.onlyForTestSetDataFactory(dataFactorySpy);
	}

	private void setUpDependencyProvider() {
		dependencyProvider = new SpiderDependencyProviderSpy();
		dependencyProvider.MRV.setDefaultReturnValuesSupplier("getAuthenticator",
				() -> authenticator);
		dependencyProvider.MRV.setDefaultReturnValuesSupplier("getSpiderAuthorizator",
				() -> authorizator);
		dependencyProvider.MRV.setDefaultReturnValuesSupplier("getExtendedFunctionalityProvider",
				() -> extendedFunctionalityProvider);
		dependencyProvider.MRV.setDefaultReturnValuesSupplier("getRecordStorage",
				() -> recordStorage);
		dependencyProvider.MRV.setDefaultReturnValuesSupplier(
				"getRecordTypeHandlerUsingDataRecordGroup", () -> recordTypeHandler);
		dependencyProvider.MRV.setDefaultReturnValuesSupplier("getDataValidator",
				() -> dataValidator);
		dependencyProvider.MRV.setDefaultReturnValuesSupplier("getDataRecordLinkCollector",
				() -> linkCollector);
		dependencyProvider.MRV.setDefaultReturnValuesSupplier("getRecordIdGenerator",
				() -> idGenerator);
		dependencyProvider.MRV.setDefaultReturnValuesSupplier("getDataGroupTermCollector",
				() -> termCollector);
		dependencyProvider.MRV.setDefaultReturnValuesSupplier("getUniqueValidator",
				() -> uniqueValidator);

		SpiderInstanceFactory factory = SpiderInstanceFactoryImp
				.usingDependencyProvider(dependencyProvider);
		SpiderInstanceProvider.setSpiderInstanceFactory(factory);

		recordValidator = RecordValidatorImp.usingDependencyProvider(dependencyProvider);
	}

	private DataRecordGroupSpy createRecordToValidate() {
		DataRecordGroupSpy recordSpy = new DataRecordGroupSpy();
		recordSpy.MRV.setDefaultReturnValuesSupplier("getType", () -> RECORD_TYPE);
		recordSpy.MRV.setDefaultReturnValuesSupplier("getId", () -> RECORD_ID);
		recordSpy.MRV.setDefaultReturnValuesSupplier("getDataDivider", () -> "uu");
		recordSpy.MRV.setDefaultReturnValuesSupplier("overwriteProtectionShouldBeEnforced",
				() -> true);
		return recordSpy;
	}

	@Test
	public void testExternalDependenciesAreCalledForValidateNew() {
		DataGroup validationOrder = createValidationOrderWithMetadataToValidateAndValidateLinks(
				"new", "true");

		recordValidator.validateRecord(SOME_AUTH_TOKEN, VALIDATION_ORDER_TYPE, validationOrder,
				recordToValidate);

		authorizator.MCR.assertMethodWasCalled("checkUserIsAuthorizedForActionOnRecordType");

		dataValidator.MCR.assertParameters("validateData", 0,
				"fakeCreateMetadataIdFromRecordTypeHandlerSpy", validationOrder);
		var recordToValidateAsDataGroup1 = dataFactorySpy.MCR
				.getReturnValue("factorGroupFromDataRecordGroup", 1);
		dataValidator.MCR.assertParameters("validateData", 1,
				"fakeCreateMetadataIdFromRecordTypeHandlerSpy", recordToValidateAsDataGroup1);

		var recordToValidateAsDataGroup0 = dataFactorySpy.MCR
				.getReturnValue("factorGroupFromDataRecordGroup", 0);
		linkCollector.MCR.assertParameters("collectLinks", 0,
				"fakeDefMetadataIdFromRecordTypeHandlerSpy", recordToValidateAsDataGroup0);
	}

	private DataGroup createValidationOrderWithMetadataToValidateAndValidateLinks(
			String metadataToValidate, String validateLinks) {
		DataGroupSpy validationOrder = new DataGroupSpy();
		DataRecordLinkSpy recordTypeToValidate = new DataRecordLinkSpy();
		validationOrder.MRV.setSpecificReturnValuesSupplier("getFirstChildOfTypeAndName",
				() -> recordTypeToValidate, DataRecordLink.class, "recordType");
		recordTypeToValidate.MRV.setDefaultReturnValuesSupplier("getLinkedRecordId",
				() -> RECORD_TYPE_TO_VALIDATE_AGAINST);
		validationOrder.MRV.setSpecificReturnValuesSupplier("getFirstAtomicValueWithNameInData",
				() -> metadataToValidate, "metadataToValidate");
		validationOrder.MRV.setSpecificReturnValuesSupplier("getFirstAtomicValueWithNameInData",
				() -> validateLinks, "validateLinks");

		return validationOrder;
		// DataGroup validationOrder = new DataGroupOldSpy(VALIDATION_ORDER_TYPE);
		// validationOrder.addChild(new DataAtomicOldSpy("metadataToValidate", metadataToValidate));
		// validationOrder.addChild(new DataAtomicOldSpy("validateLinks", validateLinks));
		//
		// DataGroup recordTypeToValidateAgainstGroup = new DataGroupOldSpy("recordType");
		// recordTypeToValidateAgainstGroup
		// .addChild(new DataAtomicOldSpy("linkedRecordType", "recordType"));
		// recordTypeToValidateAgainstGroup
		// .addChild(new DataAtomicOldSpy("linkedRecordId", RECORD_TYPE_TO_VALIDATE_AGAINST));
		// validationOrder.addChild(recordTypeToValidateAgainstGroup);
		// return validationOrder;
	}

	@Test
	public void testExternalDependenciesAreCalledForValidateExisting() {
		DataGroup validationOrder = createValidationOrderWithMetadataToValidateAndValidateLinks(
				"existing", "true");

		recordValidator.validateRecord(SOME_AUTH_TOKEN, VALIDATION_ORDER_TYPE, validationOrder,
				recordToValidate);

		authorizator.MCR.assertMethodWasCalled("checkUserIsAuthorizedForActionOnRecordType");

		dataValidator.MCR.assertParameters("validateData", 0,
				"fakeCreateMetadataIdFromRecordTypeHandlerSpy", validationOrder);
		var recordToValidateAsDataGroup1 = dataFactorySpy.MCR
				.getReturnValue("factorGroupFromDataRecordGroup", 1);

		dataValidator.MCR.assertParameters("validateData", 1,
				"fakeUpdateMetadataIdFromRecordTypeHandlerSpy", recordToValidateAsDataGroup1);

		var recordToValidateAsDataGroup0 = dataFactorySpy.MCR
				.getReturnValue("factorGroupFromDataRecordGroup", 0);
		linkCollector.MCR.assertParameters("collectLinks", 0,
				"fakeDefMetadataIdFromRecordTypeHandlerSpy", recordToValidateAsDataGroup0);
	}

	@Test
	public void testLinkCollectorIsNotCalledWhenValidateLinksIsFalse() {
		recordStorage.MRV.setDefaultReturnValuesSupplier("recordExists", () -> true);

		DataGroup validationOrder = createValidationOrderWithMetadataToValidateAndValidateLinks(
				"existing", "true");

		recordValidator.validateRecord(SOME_AUTH_TOKEN, VALIDATION_ORDER_TYPE, validationOrder,
				recordToValidate);
		linkCollector.MCR.methodWasCalled("collectLinks");
	}

	@Test
	public void testValidatRecordDataValidDataForCreateUsesNewMetadataId() {
		DataGroup validationOrder = createValidationOrderWithMetadataToValidateAndValidateLinks(
				"new", "true");
		DataRecordGroupSpy validationResult = setUpDataProviderValidationResultForValid();

		DataRecord validationRecord = recordValidator.validateRecord(SOME_AUTH_TOKEN,
				VALIDATION_ORDER_TYPE, validationOrder, recordToValidate);

		assertDataRecordCreatedWithValidationResult(validationResult, validationRecord);
		assertValidSetInResultWithValue(validationResult, "true");

		dataValidator.MCR.assertParameter("validateData", 0, "metadataGroupId",
				"fakeCreateMetadataIdFromRecordTypeHandlerSpy");
		dataValidator.MCR.assertParameter("validateData", 1, "metadataGroupId",
				"fakeCreateMetadataIdFromRecordTypeHandlerSpy");
	}

	@Test
	public void testValidatRecordDataValidDataForUpdate() {
		DataGroup validationOrder = createValidationOrderWithMetadataToValidateAndValidateLinks(
				"existing", "true");
		DataRecordGroupSpy validationResult = setUpDataProviderValidationResultForValid();

		DataRecord validationRecord = recordValidator.validateRecord(SOME_AUTH_TOKEN,
				VALIDATION_ORDER_TYPE, validationOrder, recordToValidate);

		assertDataRecordCreatedWithValidationResult(validationResult, validationRecord);
		assertValidSetInResultWithValue(validationResult, "true");

		String methodName = "validateData";
		dataValidator.MCR.assertParameter(methodName, 0, "metadataGroupId",
				"fakeCreateMetadataIdFromRecordTypeHandlerSpy");
		dataValidator.MCR.assertParameter(methodName, 1, "metadataGroupId",
				"fakeUpdateMetadataIdFromRecordTypeHandlerSpy");
		dataValidator.MCR.assertNumberOfCallsToMethod(methodName, 2);
	}

	@Test
	public void testValidatRecordCheckValidationResult() {
		DataGroup validationOrder = createValidationOrderWithMetadataToValidateAndValidateLinks(
				"existing", "true");

		DataRecordGroupSpy validationResult = setUpDataProviderValidationResultForValid();

		DataRecordSpy validationRecord = (DataRecordSpy) recordValidator.validateRecord(
				SOME_AUTH_TOKEN, VALIDATION_ORDER_TYPE, validationOrder, recordToValidate);

		assertValidSetInResultWithValue(validationResult, "true");

		assertCorrectRecordInfo(validationResult);

		validationRecord.MCR.assertCalledParameters("addAction", Action.READ);
	}

	private void assertCorrectRecordInfo(DataRecordGroupSpy validationResult) {
		validationResult.MCR.assertParameters("setId", 0,
				idGenerator.MCR.getReturnValue("getIdForType", 0));
		validationResult.MCR.assertParameters("setType", 0, VALIDATION_ORDER_TYPE);
		validationResult.MCR.assertParameters("setCreatedBy", 0, currentUser.id);
		validationResult.MCR.methodWasCalled("setTsCreatedToNow");
		validationResult.MCR.assertParameters("addUpdatedUsingUserIdAndTs", 0, currentUser.id,
				validationResult.MCR.getReturnValue("getTsCreated", 0));
	}

	@Test
	public void testValidateRecordInvalidData() {
		ValidationAnswerSpy invalid = new ValidationAnswerSpy();
		invalid.MRV.setReturnValues("dataIsInvalid", List.of(false, true));
		dataValidator.MRV.setDefaultReturnValuesSupplier("validateData", () -> invalid);
		recordTypeHandler.MRV.setDefaultReturnValuesSupplier("getCreateDefinitonId",
				() -> RECORD_TYPE_TO_VALIDATE_AGAINST);
		recordTypeHandler.MRV.setDefaultReturnValuesSupplier("getUpdateDefinitionId",
				() -> RECORD_TYPE_TO_VALIDATE_AGAINST);
		recordTypeHandler.MRV.setDefaultReturnValuesSupplier("getDefinitionId",
				() -> RECORD_TYPE_TO_VALIDATE_AGAINST);
		DataGroup validationOrder = createValidationOrderWithMetadataToValidateAndValidateLinks(
				"existing", "true");
		DataRecordGroupSpy validationResult = setUpValidationResultForError();

		DataRecord validationRecord = recordValidator.validateRecord(SOME_AUTH_TOKEN,
				VALIDATION_ORDER_TYPE, validationOrder, recordToValidate);

		assertDataRecordCreatedWithValidationResult(validationResult, validationRecord);
		assertValidSetInResultWithValue(validationResult, "false");
		dataValidator.MCR.assertParameters("validateData", 0,
				"fakeCreateMetadataIdFromRecordTypeHandlerSpy");
		dataValidator.MCR.assertParameters("validateData", 1, RECORD_TYPE_TO_VALIDATE_AGAINST);
		dataValidator.MCR.assertNumberOfCallsToMethod("validateData", 2);
	}

	@Test
	public void testLinkedRecordIdDoesNotExist() {
		fillCollectLinksReturnValue();
		DataGroup validationOrder = createValidationOrderWithMetadataToValidateAndValidateLinks(
				"new", "true");

		DataRecordGroupSpy validationResult = setUpValidationResultForError();

		DataRecord validationRecord = recordValidator.validateRecord(SOME_AUTH_TOKEN,
				VALIDATION_ORDER_TYPE, validationOrder, recordToValidate);

		String errorString = "Data is not valid: linkedRecord does not exists in storage for "
				+ "recordType: toRecordType and recordId: toRecordId";

		assertDataRecordCreatedWithValidationResult(validationResult, validationRecord);
		assertValidSetInResultWithValue(validationResult, "false");
		assertErrorMessages(validationResult, errorString);
	}

	private void assertDataRecordCreatedWithValidationResult(DataRecordGroupSpy validationResult,
			DataRecord validationRecord) {
		var factoredValidationResultRecord = dataFactorySpy.MCR
				.assertCalledParametersReturn("factorRecordUsingDataRecordGroup", validationResult);
		assertSame(validationRecord, factoredValidationResultRecord);
	}

	private DataRecordGroupSpy setUpValidationResultForError() {
		DataRecordGroupSpy validationResult = new DataRecordGroupSpy();
		validationResult.MRV.setSpecificReturnValuesSupplier("containsChildWithNameInData",
				() -> true, "errorMessages");
		dataFactorySpy.MRV.setSpecificReturnValuesSupplier("factorRecordGroupUsingNameInData",
				() -> validationResult, "validationResult");
		return validationResult;
	}

	private void assertErrorMessages(DataRecordGroupSpy validationResult, String... errorStrings) {
		int repeatId = 0;
		for (String errorString : errorStrings) {
			DataGroupSpy errorMessages = (DataGroupSpy) validationResult.MCR
					.getReturnValue("getFirstGroupWithNameInData", repeatId);
			DataAtomicSpy errorMessage = (DataAtomicSpy) dataFactorySpy.MCR
					.assertCalledParametersReturn("factorAtomicUsingNameInDataAndValue",
							"errorMessage", errorString);
			errorMessage.MCR.assertNumberOfCallsToMethod("setRepeatId", 1);
			errorMessage.MCR.assertParameters("setRepeatId", 0, "0");
			errorMessages.MCR.assertCalledParameters("addChild", errorMessage);
			repeatId++;
		}
	}

	private void assertValidSetInResultWithValue(DataRecordGroupSpy validationResult,
			String validValue) {
		var valid = dataFactorySpy.MCR.assertCalledParametersReturn(
				"factorAtomicUsingNameInDataAndValue", "valid", validValue);
		validationResult.MCR.assertCalledParameters("addChild", valid);
	}

	@Test
	public void testValidateRecordInvalidDataAndLinksDoesNotExist() {
		ValidationAnswerSpy invalid = new ValidationAnswerSpy();
		invalid.MRV.setReturnValues("dataIsInvalid", List.of(false, true));
		invalid.MRV.setDefaultReturnValuesSupplier("getErrorMessages",
				() -> List.of("Error from dataValidator"));
		dataValidator.MRV.setDefaultReturnValuesSupplier("validateData", () -> invalid);

		fillCollectLinksReturnValue();
		DataGroup validationOrder = createValidationOrderWithMetadataToValidateAndValidateLinks(
				"new", "true");

		DataRecordGroupSpy validationResult = setUpValidationResultForError();

		DataRecord validationRecord = recordValidator.validateRecord(SOME_AUTH_TOKEN,
				VALIDATION_ORDER_TYPE, validationOrder, recordToValidate);

		assertDataRecordCreatedWithValidationResult(validationResult, validationRecord);
		assertValidSetInResultWithValue(validationResult, "false");

		String errorString = "Data is not valid: linkedRecord does not exists in storage for recordType: toRecordType and recordId: toRecordId";
		String errorString2 = "Error from dataValidator";

		assertErrorMessages(validationResult, errorString, errorString2);
	}

	@Test
	public void testLinkedRecordIdDoesNotExistDoesNotMatterWhenLinksAreNotChecked() {
		fillCollectLinksReturnValue();
		DataGroup validationOrder = createValidationOrderWithMetadataToValidateAndValidateLinks(
				"new", "false");
		DataRecordGroupSpy validationResult = setUpDataProviderValidationResultForValid();

		DataRecord validationRecord = recordValidator.validateRecord(SOME_AUTH_TOKEN,
				VALIDATION_ORDER_TYPE, validationOrder, recordToValidate);

		assertDataRecordCreatedWithValidationResult(validationResult, validationRecord);
		assertValidSetInResultWithValue(validationResult, "true");
	}

	private DataRecordGroupSpy setUpDataProviderValidationResultForValid() {
		DataRecordGroupSpy validationResult = new DataRecordGroupSpy();
		validationResult.MRV.setSpecificReturnValuesSupplier("containsChildWithNameInData",
				() -> false, "errorMessages");
		dataFactorySpy.MRV.setSpecificReturnValuesSupplier("factorRecordGroupUsingNameInData",
				() -> validationResult, "validationResult");
		return validationResult;
	}

	private void fillCollectLinksReturnValue() {
		Link link = new Link("toRecordType", "toRecordId");
		linkCollector.MRV.setDefaultReturnValuesSupplier("collectLinks", () -> Set.of(link));
	}

	@Test
	public void testUnauthorizedForCreateOnValidationorderRecordTypeShouldNotValidate() {
		authorizator.MRV.setThrowException("checkUserIsAuthorizedForActionOnRecordType",
				new AuthorizationException("Exception from SpiderAuthorizatorSpy"), currentUser,
				"create", VALIDATION_ORDER_TYPE);
		DataGroup validationOrder = createValidationOrderWithMetadataToValidateAndValidateLinks(
				"existing", "true");

		try {
			recordValidator.validateRecord(SOME_AUTH_TOKEN, VALIDATION_ORDER_TYPE, validationOrder,
					recordToValidate);
			fail("It should throw exception");
		} catch (Exception e) {
			assertEquals(e.getClass(), AuthorizationException.class);
			assertEquals(e.getMessage(), "Exception from SpiderAuthorizatorSpy");
		}

		authorizator.MCR.assertParameter("checkUserIsAuthorizedForActionOnRecordType", 0, "action",
				"create");
		dataValidator.MCR.assertMethodNotCalled("validateData");
	}

	@Test

	public void testInvalidDataForCreateOnValidationOrderShouldThrowException() {
		ValidationAnswerSpy invalidForValidationOrder = new ValidationAnswerSpy();
		invalidForValidationOrder.MRV.setDefaultReturnValuesSupplier("dataIsInvalid", () -> true);
		invalidForValidationOrder.MRV.setDefaultReturnValuesSupplier("getErrorMessages",
				() -> List.of("Error from dataValidator"));
		dataValidator.MRV.setDefaultReturnValuesSupplier("validateData",
				() -> invalidForValidationOrder);
		DataGroup validationOrder = createValidationOrderWithMetadataToValidateAndValidateLinks(
				"existing", "true");

		try {
			recordValidator.validateRecord(SOME_AUTH_TOKEN, VALIDATION_ORDER_TYPE, validationOrder,
					recordToValidate);
			fail("It should throw exception");
		} catch (Exception e) {
			assertEquals(e.getClass(), DataException.class);
			assertEquals(e.getMessage(), "Data is not valid: [Error from dataValidator]");
		}
	}

	@Test
	public void testUnauthorizedForValidateOnRecordTypeShouldNotValidateDataForThatType() {
		authorizator.MRV.setThrowException("checkUserIsAuthorizedForActionOnRecordType",
				new AuthorizationException("Exception from SpiderAuthorizatorSpy"), currentUser,
				"validate", RECORD_TYPE_TO_VALIDATE_AGAINST);
		DataGroup validationOrder = createValidationOrderWithMetadataToValidateAndValidateLinks(
				"existing", "true");

		try {
			recordValidator.validateRecord(SOME_AUTH_TOKEN, VALIDATION_ORDER_TYPE, validationOrder,
					recordToValidate);
			fail("It should throw exception");
		} catch (Exception e) {
			assertEquals(e.getClass(), AuthorizationException.class);
		}
		dataValidator.MCR.assertNumberOfCallsToMethod("validateData", 1);
	}

	@Test
	public void testNoExisting() {
		recordStorage.MRV.setAlwaysThrowException("read",
				RecordNotFoundException.withMessage("someMessage"));

		// DataGroup dataGroup = createDataGroupPlace();
		//
		DataGroup validationOrder = createValidationOrderWithMetadataToValidateAndValidateLinks(
				"existing", "true");
		// changeRecordTypeTo(validationOrder, "recordType_NOT_EXISTING");
		DataRecordGroupSpy validationResult = setUpValidationResultForError();

		DataRecord validationRecord = recordValidator.validateRecord(SOME_AUTH_TOKEN,
				VALIDATION_ORDER_TYPE, validationOrder, recordToValidate);
		// DataRecord validationRecord = recordValidator.validateRecord(SOME_AUTH_TOKEN,
		// VALIDATION_ORDER_TYPE, validationOrder, dataGroup);

		String errorString = "someMessage";
		assertDataRecordCreatedWithValidationResult(validationResult, validationRecord);
		assertValidSetInResultWithValue(validationResult, "false");
		assertErrorMessages(validationResult, errorString);
	}

	@Test
	public void testUseExtendedFunctionalityExtendedFunctionalitiesExists() throws Exception {
		DataGroup validationOrder = createValidationOrderWithMetadataToValidateAndValidateLinks(
				"new", "true");

		recordValidator.validateRecord(SOME_AUTH_TOKEN, VALIDATION_ORDER_TYPE, validationOrder,
				recordToValidate);

		ExtendedFunctionalityData expectedData = new ExtendedFunctionalityData();
		expectedData.authToken = SOME_AUTH_TOKEN;
		expectedData.recordType = RECORD_TYPE_TO_VALIDATE_AGAINST;
		expectedData.recordId = null;
		expectedData.user = (User) authenticator.MCR.getReturnValue("getUserForToken", 0);
		expectedData.dataRecordGroup = recordToValidate;
		extendedFunctionalityProvider.assertCallToPositionAndFunctionalityCalledWithData(
				VALIDATE_AFTER_AUTHORIZATION, expectedData, 0);
	}

	@Test
	public void testEnsureExtendedFunctionalityPosition_AfterAuthorizathion() throws Exception {
		extendedFunctionalityProvider.setUpExtendedFunctionalityToThrowExceptionOnPosition(
				dependencyProvider, VALIDATE_AFTER_AUTHORIZATION, RECORD_TYPE_TO_VALIDATE_AGAINST);

		callReadRecordAndCatchStopExecution();

		dataValidator.MCR.assertNumberOfCallsToMethod("validateData", 1);
		dataFactorySpy.MCR.assertNumberOfCallsToMethod("factorRecordGroupFromDataGroup", 1);
	}

	private void callReadRecordAndCatchStopExecution() {
		DataGroup validationOrder = createValidationOrderWithMetadataToValidateAndValidateLinks(
				"new", "true");
		try {
			recordValidator.validateRecord(SOME_AUTH_TOKEN, VALIDATION_ORDER_TYPE, validationOrder,
					recordToValidate);
			fail("Should fail as we want to stop execution when the extended functionality is used,"
					+ " to determin that the extended functionality is called in the correct place"
					+ " in the code");
		} catch (Exception e) {
		}
	}

	@Test
	public void testValidateRecordTypesOfRecordAndValidationOrderDoesNotMatch() throws Exception {
		DataGroup validationOrder = createValidationOrderWithMetadataToValidateAndValidateLinks(
				"new", "true");
		recordTypeHandler.MRV.setDefaultReturnValuesSupplier("getRecordTypeId",
				() -> "someOtherRecordType");
		DataRecordGroupSpy validationResult = setUpValidationResultForError();

		DataRecord validationRecord = recordValidator.validateRecord(SOME_AUTH_TOKEN,
				VALIDATION_ORDER_TYPE, validationOrder, recordToValidate);

		String errorString = "RecordType from record (someOtherRecordType) does not match recordType from validationOrder ("
				+ RECORD_TYPE_TO_VALIDATE_AGAINST + ")";
		assertDataRecordCreatedWithValidationResult(validationResult, validationRecord);
		assertValidSetInResultWithValue(validationResult, "false");
		assertErrorMessages(validationResult, errorString);
	}

	@Test
	public void testRecordUpdaterGetsUniqueValiadatorFromDependencyProvider() throws Exception {
		DataGroup validationOrder = createValidationOrderWithMetadataToValidateAndValidateLinks(
				"new", "true");

		recordValidator.validateRecord(SOME_AUTH_TOKEN, VALIDATION_ORDER_TYPE, validationOrder,
				recordToValidate);

		dependencyProvider.MCR.assertCalledParameters("getUniqueValidator", recordStorage);
	}

	@Test
	public void uniqueValidatorCalledWithCorrectParameters() throws Exception {
		List<Unique> uniqueList = List.of(new Unique("", Set.of("")));
		recordTypeHandler.MRV.setDefaultReturnValuesSupplier("getUniqueDefinitions",
				() -> uniqueList);
		recordTypeHandler.MRV.setDefaultReturnValuesSupplier("getDefinitionId",
				() -> "someDefinition");
		CollectTerms collectTerms = new CollectTerms();
		collectTerms.storageTerms = Set.of(new StorageTerm("id", "key", "value"));
		termCollector.MRV.setDefaultReturnValuesSupplier("collectTerms", () -> collectTerms);

		DataGroup validationOrder = createValidationOrderWithMetadataToValidateAndValidateLinks(
				"new", "true");

		// DataGroupSpy validationResult = setUpValidationResultForValid();

		recordValidator.validateRecord(SOME_AUTH_TOKEN, VALIDATION_ORDER_TYPE, validationOrder,
				recordToValidate);

		termCollector.MCR.assertParameters("collectTerms", 0, "someDefinition", recordToValidate);
		uniqueValidator.MCR.assertParameters("validateUniqueForNewRecord", 0,
				RECORD_TYPE_TO_VALIDATE_AGAINST, uniqueList, collectTerms.storageTerms);
	}
	//
	// @Test
	// public void testUniqueValidationFails_throwsSpiderConflictException() throws Exception {
	// DataGroupSpy recordSpy = createDataGroupForUpdate();
	// setupUniqueValidatorToReturnInvalidAnswerWithThreeErrors();
	//
	// try {
	// recordUpdater.updateRecord(AUTH_TOKEN, RECORD_TYPE, RECORD_ID, recordSpy);
	//
	// fail("A ConclictException should have been thrown");
	// } catch (Exception e) {
	// assertTrue(e instanceof ConflictException);
	// assertEquals(e.getMessage(),
	// "The record could not be created as it fails unique validation with the "
	// + "following 3 error messages: [" + "error1, error2, error3]");
	// recordStorage.MCR.assertMethodNotCalled("create");
	// }
	// }
	//
	// private void setupUniqueValidatorToReturnInvalidAnswerWithThreeErrors() {
	// ValidationAnswerSpy validationAnswer = new ValidationAnswerSpy();
	// validationAnswer.MRV.setDefaultReturnValuesSupplier("dataIsInvalid", () -> true);
	// validationAnswer.MRV.setDefaultReturnValuesSupplier("getErrorMessages",
	// () -> List.of("error1", "error2", "error3"));
	// uniqueValidator.MRV.setDefaultReturnValuesSupplier("validateUnique",
	// () -> validationAnswer);
	// }
}
