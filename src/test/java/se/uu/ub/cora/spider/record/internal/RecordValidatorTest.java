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

import java.util.Iterator;
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
import se.uu.ub.cora.storage.spies.RecordStorageSpy;

public class RecordValidatorTest {
	private static final String CREATE = "new";
	private static final String UPDATE = "existing";
	private static final String WITH_LINKS = "true";
	private static final String WITHOUT_LINKS = "false";
	private static final String SPECIFIED_RECORD_TYPE_TO_VALIDATE = "specifiedRecordTypeId";
	private static final String RECORD_ID = "someRecordId";
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

	private RecordTypeHandlerSpy recordTypeHandlerForValidationOrder;
	private RecordTypeHandlerSpy recordTypeHandlerForDataToValidate;
	private DataRecordGroupSpy recordToValidate;
	private User currentUser;
	private ValidationAnswerSpy validAnswer;
	private ValidationAnswerSpy invalidAnswer;
	private CollectTerms collectTerms;
	private List<Unique> uniqueList;

	@BeforeMethod
	public void beforeMethod() {
		setUpFactoriesAndProviders();
		authenticator = new AuthenticatorSpy();
		authorizator = new SpiderAuthorizatorSpy();
		dataValidator = new DataValidatorSpy();
		recordStorage = new RecordStorageSpy();
		linkCollector = new DataRecordLinkCollectorSpy();
		extendedFunctionalityProvider = new ExtendedFunctionalityProviderSpy();
		idGenerator = new RecordIdGeneratorSpy();
		termCollector = new DataGroupTermCollectorSpy();
		uniqueValidator = new UniqueValidatorSpy();
		validAnswer = new ValidationAnswerSpy();
		invalidAnswer = createInvalidAnswer();
		recordTypeHandlerForValidationOrder = new RecordTypeHandlerSpy();
		recordTypeHandlerForDataToValidate = new RecordTypeHandlerSpy();
		setUpRecordtypeHandlers();

		setUpAnswerForCollectTerms();

		setUpDependencyProvider();

		recordStorage.MRV.setDefaultReturnValuesSupplier("recordExists", () -> true);

		currentUser = new User("someUserId");

		authenticator.MRV.setDefaultReturnValuesSupplier("getUserForToken", () -> currentUser);

		recordToValidate = createRecordToValidate();
	}

	private void setUpRecordtypeHandlers() {
		setUpDefinitionIdsForRecordTypeHandler(recordTypeHandlerForValidationOrder,
				"validationOrder");
		setUpDefinitionIdsForRecordTypeHandler(recordTypeHandlerForDataToValidate,
				"dataToValidate");
		setUpAnswerGetUniqueDefinitions();
	}

	private void setUpAnswerGetUniqueDefinitions() {
		uniqueList = List.of(new Unique("", Set.of("")));
		recordTypeHandlerForDataToValidate.MRV
				.setDefaultReturnValuesSupplier("getUniqueDefinitions", () -> uniqueList);
	}

	private void setUpAnswerForCollectTerms() {
		collectTerms = new CollectTerms();
		collectTerms.storageTerms = Set.of(new StorageTerm("id", "key", "value"));
		termCollector.MRV.setDefaultReturnValuesSupplier("collectTerms", () -> collectTerms);
	}

	private ValidationAnswerSpy createInvalidAnswer() {
		ValidationAnswerSpy answer = new ValidationAnswerSpy();
		answer.MRV.setDefaultReturnValuesSupplier("dataIsInvalid", () -> true);
		answer.MRV.setDefaultReturnValuesSupplier("getErrorMessages",
				() -> List.of("some invalid answer 1", "some invalid answer 2"));
		return answer;
	}

	private void setUpFactoriesAndProviders() {
		loggerFactorySpy = new LoggerFactorySpy();
		LoggerProvider.setLoggerFactory(loggerFactorySpy);
		dataFactorySpy = new DataFactorySpy();
		DataProvider.onlyForTestSetDataFactory(dataFactorySpy);
	}

	private void setUpDefinitionIdsForRecordTypeHandler(RecordTypeHandlerSpy recordTypeHandler,
			String suffix) {
		recordTypeHandler.MRV.setDefaultReturnValuesSupplier("getCreateDefinitionId",
				() -> suffix + "CreatedefinitionId");
		recordTypeHandler.MRV.setDefaultReturnValuesSupplier("getUpdateDefinitionId",
				() -> suffix + "UpdatedefinitionId");
		recordTypeHandler.MRV.setDefaultReturnValuesSupplier("getDefinitionId",
				() -> suffix + "DefinitionId");
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

		Iterator<RecordTypeHandlerSpy> recordTypeHandlers = List
				.of(recordTypeHandlerForValidationOrder, recordTypeHandlerForDataToValidate)
				.iterator();
		dependencyProvider.MRV.setDefaultReturnValuesSupplier(
				"getRecordTypeHandlerUsingDataRecordGroup", () -> recordTypeHandlers.next());

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
		recordSpy.MRV.setDefaultReturnValuesSupplier("getType",
				() -> SPECIFIED_RECORD_TYPE_TO_VALIDATE);
		recordSpy.MRV.setDefaultReturnValuesSupplier("getId", () -> RECORD_ID);
		recordSpy.MRV.setDefaultReturnValuesSupplier("getDataDivider", () -> "uu");
		recordSpy.MRV.setDefaultReturnValuesSupplier("overwriteProtectionShouldBeEnforced",
				() -> true);
		return recordSpy;
	}

	private void setUpCollectLinksReturnValue() {
		Link link = new Link("toRecordType", "toRecordId");
		linkCollector.MRV.setDefaultReturnValuesSupplier("collectLinks", () -> Set.of(link));
	}

	private DataGroup createValidationOrderWithMetadataToValidateAndValidateLinks(String action,
			String validateLinks) {
		DataGroupSpy validationOrder = new DataGroupSpy();

		DataRecordLinkSpy recordTypeToValidate = new DataRecordLinkSpy();
		validationOrder.MRV.setSpecificReturnValuesSupplier("getFirstChildOfTypeAndName",
				() -> recordTypeToValidate, DataRecordLink.class, "recordType");
		recordTypeToValidate.MRV.setDefaultReturnValuesSupplier("getLinkedRecordId",
				() -> SPECIFIED_RECORD_TYPE_TO_VALIDATE);

		validationOrder.MRV.setSpecificReturnValuesSupplier("getFirstAtomicValueWithNameInData",
				() -> action, "metadataToValidate");
		validationOrder.MRV.setSpecificReturnValuesSupplier("getFirstAtomicValueWithNameInData",
				() -> validateLinks, "validateLinks");

		return validationOrder;
	}

	@Test
	public void testGetActiveUserIsUsedToAuthorizeCreateForValidationOrder() {
		DataGroup validationOrder = createValidationOrderWithMetadataToValidateAndValidateLinks(
				CREATE, WITH_LINKS);

		recordValidator.validateRecord(SOME_AUTH_TOKEN, VALIDATION_ORDER_TYPE, validationOrder,
				recordToValidate);

		authenticator.MCR.assertParameters("getUserForToken", 0, SOME_AUTH_TOKEN);
		var activeUser = authenticator.MCR.assertCalledParametersReturn("getUserForToken",
				SOME_AUTH_TOKEN);

		authorizator.MCR.assertParameters("checkUserIsAuthorizedForActionOnRecordType", 0,
				activeUser, "create", VALIDATION_ORDER_TYPE);
	}

	@Test
	public void testUnauthorizedForCreateOnValidationOrderRecordTypeShouldNotValidate() {
		authorizator.MRV.setThrowException("checkUserIsAuthorizedForActionOnRecordType",
				new AuthorizationException("Exception from SpiderAuthorizatorSpy"), currentUser,
				"create", VALIDATION_ORDER_TYPE);
		DataGroup validationOrder = createValidationOrderWithMetadataToValidateAndValidateLinks(
				UPDATE, WITH_LINKS);

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
	public void testValidationOrderIsValidated() {
		DataGroup validationOrder = createValidationOrderWithMetadataToValidateAndValidateLinks(
				CREATE, WITH_LINKS);

		recordValidator.validateRecord(SOME_AUTH_TOKEN, VALIDATION_ORDER_TYPE, validationOrder,
				recordToValidate);

		String validationOrderDefId = assertValidationOrderDefIdFetchedCorrectly(validationOrder);
		dataValidator.MCR.assertParameters("validateData", 0, validationOrderDefId,
				validationOrder);
	}

	private String assertValidationOrderDefIdFetchedCorrectly(DataGroup validationOrder) {
		dataFactorySpy.MCR.assertParameters("factorRecordGroupFromDataGroup", 0, validationOrder);
		var validationOrderAsRecordGroup = dataFactorySpy.MCR
				.getReturnValue("factorRecordGroupFromDataGroup", 0);

		RecordTypeHandlerSpy recordTypeHandlerForValidationOrder = (RecordTypeHandlerSpy) dependencyProvider.MCR
				.assertCalledParametersReturn("getRecordTypeHandlerUsingDataRecordGroup",
						validationOrderAsRecordGroup);

		String validationOrderDefId = (String) recordTypeHandlerForValidationOrder.MCR
				.assertCalledParametersReturn("getCreateDefinitionId");
		return validationOrderDefId;
	}

	@Test
	public void testInvalidDataForCreateOnValidationOrderShouldThrowException() {
		setupDataValidatorToReturnAnInvalidValidationAnswer();
		DataGroup validationOrder = createValidationOrderWithMetadataToValidateAndValidateLinks(
				UPDATE, WITH_LINKS);

		try {
			recordValidator.validateRecord(SOME_AUTH_TOKEN, VALIDATION_ORDER_TYPE, validationOrder,
					recordToValidate);
			fail("It should throw exception");
		} catch (Exception e) {
			assertEquals(e.getClass(), DataException.class);
			assertEquals(e.getMessage(), "Data is not valid: [Error from dataValidator]");
		}
	}

	private void setupDataValidatorToReturnAnInvalidValidationAnswer() {
		ValidationAnswerSpy invalidForValidationOrder = new ValidationAnswerSpy();
		invalidForValidationOrder.MRV.setDefaultReturnValuesSupplier("dataIsInvalid", () -> true);
		invalidForValidationOrder.MRV.setDefaultReturnValuesSupplier("getErrorMessages",
				() -> List.of("Error from dataValidator"));
		dataValidator.MRV.setDefaultReturnValuesSupplier("validateData",
				() -> invalidForValidationOrder);
	}

	@Test
	public void testGetActiveUserIsUsedToAuthorizeValidateOnRecordTypeToValidate() {
		DataGroup validationOrder = createValidationOrderWithMetadataToValidateAndValidateLinks(
				CREATE, WITH_LINKS);

		recordValidator.validateRecord(SOME_AUTH_TOKEN, VALIDATION_ORDER_TYPE, validationOrder,
				recordToValidate);

		authorizator.MCR.assertParameters("checkUserIsAuthorizedForActionOnRecordType", 1,
				currentUser, "validate", SPECIFIED_RECORD_TYPE_TO_VALIDATE);
	}

	@Test
	public void testUnauthorizedForValidateOnRecordTypeToValidateShouldNotValidate() {
		authorizator.MRV.setThrowException("checkUserIsAuthorizedForActionOnRecordType",
				new AuthorizationException("Exception from SpiderAuthorizatorSpy"), currentUser,
				"validate", SPECIFIED_RECORD_TYPE_TO_VALIDATE);
		DataGroup validationOrder = createValidationOrderWithMetadataToValidateAndValidateLinks(
				UPDATE, WITH_LINKS);

		try {
			recordValidator.validateRecord(SOME_AUTH_TOKEN, VALIDATION_ORDER_TYPE, validationOrder,
					recordToValidate);
			fail("It should throw exception");
		} catch (Exception e) {
			assertEquals(e.getClass(), AuthorizationException.class);
			assertEquals(e.getMessage(), "Exception from SpiderAuthorizatorSpy");
		}
	}

	@Test
	public void testUseExtendedFunctionalityExtendedFunctionalitiesExists() throws Exception {
		DataGroup validationOrder = createValidationOrderWithMetadataToValidateAndValidateLinks(
				CREATE, WITH_LINKS);

		recordValidator.validateRecord(SOME_AUTH_TOKEN, VALIDATION_ORDER_TYPE, validationOrder,
				recordToValidate);

		ExtendedFunctionalityData expectedData = new ExtendedFunctionalityData();
		expectedData.authToken = SOME_AUTH_TOKEN;
		expectedData.recordType = SPECIFIED_RECORD_TYPE_TO_VALIDATE;
		expectedData.recordId = recordToValidate.getId();
		expectedData.user = (User) authenticator.MCR.getReturnValue("getUserForToken", 0);
		expectedData.dataRecordGroup = recordToValidate;
		extendedFunctionalityProvider.assertCallToPositionAndFunctionalityCalledWithData(
				VALIDATE_AFTER_AUTHORIZATION, expectedData, 0);
	}

	@Test
	public void testEnsureExtendedFunctionalityPosition_AfterAuthorizathion() throws Exception {
		extendedFunctionalityProvider.setUpExtendedFunctionalityToThrowExceptionOnPosition(
				dependencyProvider, VALIDATE_AFTER_AUTHORIZATION,
				SPECIFIED_RECORD_TYPE_TO_VALIDATE);

		callReadRecordAndCatchStopExecution();

		dataValidator.MCR.assertNumberOfCallsToMethod("validateData", 1);
		dataFactorySpy.MCR.assertNumberOfCallsToMethod("factorRecordGroupFromDataGroup", 1);
	}

	private void callReadRecordAndCatchStopExecution() {
		DataGroup validationOrder = createValidationOrderWithMetadataToValidateAndValidateLinks(
				CREATE, WITH_LINKS);
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
				CREATE, WITH_LINKS);

		recordToValidate.MRV.setDefaultReturnValuesSupplier("getType", () -> "someOtherRecordType");

		DataRecord validationRecord = recordValidator.validateRecord(SOME_AUTH_TOKEN,
				VALIDATION_ORDER_TYPE, validationOrder, recordToValidate);

		String errorString = "RecordType from record (someOtherRecordType) does not match recordType"
				+ " from validationOrder (" + SPECIFIED_RECORD_TYPE_TO_VALIDATE + ")";
		assertAnswerFromRecordValidator(validationRecord, errorString);
	}

	@Test
	public void testCallsToValidateRecord_Valid_Create_WithoutLinks() throws Exception {
		DataGroup validationOrder = createValidationOrderWithMetadataToValidateAndValidateLinks(
				CREATE, WITHOUT_LINKS);

		DataRecord validationRecord = recordValidator.validateRecord(SOME_AUTH_TOKEN,
				VALIDATION_ORDER_TYPE, validationOrder, recordToValidate);

		var recordToValidateAsDataGroup = dataFactorySpy.MCR
				.assertCalledParametersReturn("factorGroupFromDataRecordGroup", recordToValidate);
		linkCollector.MCR.assertMethodNotCalled("collectLinks");
		dataValidator.MCR.assertCalledParameters("validateData",
				recordTypeHandlerForDataToValidate.getCreateDefinitionId(),
				recordToValidateAsDataGroup);
		recordStorage.MCR.assertMethodNotCalled("recordExists");

		assertAnswerFromRecordValidator(validationRecord);
	}

	@Test
	public void testCallsToValidateRecord_Create_WithLinks() throws Exception {
		DataGroup validationOrder = createValidationOrderWithMetadataToValidateAndValidateLinks(
				CREATE, WITH_LINKS);

		DataRecord validationRecord = recordValidator.validateRecord(SOME_AUTH_TOKEN,
				VALIDATION_ORDER_TYPE, validationOrder, recordToValidate);

		var recordToValidateAsDataGroup = dataFactorySpy.MCR
				.assertCalledParametersReturn("factorGroupFromDataRecordGroup", recordToValidate);
		linkCollector.MCR.assertCalledParameters("collectLinks",
				recordTypeHandlerForDataToValidate.getDefinitionId(), recordToValidateAsDataGroup);
		dataValidator.MCR.assertCalledParameters("validateData",
				recordTypeHandlerForDataToValidate.getCreateDefinitionId(),
				recordToValidateAsDataGroup);
		recordStorage.MCR.assertMethodNotCalled("recordExists");

		assertAnswerFromRecordValidator(validationRecord);
	}

	@Test
	public void testCallsToValidateRecord_Update_WithoutLinks() throws Exception {
		DataGroup validationOrder = createValidationOrderWithMetadataToValidateAndValidateLinks(
				UPDATE, WITHOUT_LINKS);

		DataRecord validationRecord = recordValidator.validateRecord(SOME_AUTH_TOKEN,
				VALIDATION_ORDER_TYPE, validationOrder, recordToValidate);

		dataFactorySpy.MCR.assertCalledParameters("factorGroupFromDataRecordGroup",
				recordToValidate);

		var recordToValidateAsDataGroup = dataFactorySpy.MCR
				.assertCalledParametersReturn("factorGroupFromDataRecordGroup", recordToValidate);
		linkCollector.MCR.assertMethodNotCalled("collectLinks");
		dataValidator.MCR.assertCalledParameters("validateData",
				recordTypeHandlerForDataToValidate.getUpdateDefinitionId(),
				recordToValidateAsDataGroup);
		assertCallToRecordExistsInStorage();
		assertAnswerFromRecordValidator(validationRecord);
	}

	private void assertCallToRecordExistsInStorage() {
		recordStorage.MCR.assertParameterAsEqual("recordExists", 0, "types",
				List.of(SPECIFIED_RECORD_TYPE_TO_VALIDATE));
		recordStorage.MCR.assertParameter("recordExists", 0, "id", recordToValidate.getId());
	}

	@Test
	public void testCallsToValidateRecord_Update_WithLinks() throws Exception {
		DataGroup validationOrder = createValidationOrderWithMetadataToValidateAndValidateLinks(
				UPDATE, WITH_LINKS);

		DataRecord validationRecord = recordValidator.validateRecord(SOME_AUTH_TOKEN,
				VALIDATION_ORDER_TYPE, validationOrder, recordToValidate);

		var recordToValidateAsDataGroup = dataFactorySpy.MCR
				.assertCalledParametersReturn("factorGroupFromDataRecordGroup", recordToValidate);
		linkCollector.MCR.assertCalledParameters("collectLinks",
				recordTypeHandlerForDataToValidate.getDefinitionId(), recordToValidateAsDataGroup);
		dataValidator.MCR.assertCalledParameters("validateData",
				recordTypeHandlerForDataToValidate.getUpdateDefinitionId(),
				recordToValidateAsDataGroup);
		assertCallToRecordExistsInStorage();

		assertAnswerFromRecordValidator(validationRecord);
	}

	@Test
	public void testValidateRecord_Update_RecordNotExistingInStorage() {
		recordStorage.MRV.setDefaultReturnValuesSupplier("recordExists", () -> false);
		DataGroup validationOrder = createValidationOrderWithMetadataToValidateAndValidateLinks(
				UPDATE, WITH_LINKS);

		DataRecord validationRecord = recordValidator.validateRecord(SOME_AUTH_TOKEN,
				VALIDATION_ORDER_TYPE, validationOrder, recordToValidate);

		String errorString = "The record validated for update does not exist in storage.";
		assertAnswerFromRecordValidator(validationRecord, errorString);
	}

	private void assertAnswerFromRecordValidator(DataRecord validationRecord,
			String... errorStrings) {
		DataRecordGroupSpy validationResult = (DataRecordGroupSpy) dataFactorySpy.MCR
				.assertCalledParametersReturn("factorRecordGroupUsingNameInData",
						"validationResult");
		assertDataRecordCreatedWithValidationResult(validationResult, validationRecord);
		assertCorrectRecordInfo(validationResult);
		int numberOfErrors = errorStrings.length;
		assertAnswerStatus(validationResult, numberOfErrors);
		assertErrorMessages(validationResult, errorStrings);
		assertReadActionAddedToValidationRecord(validationRecord);
	}

	private void assertAnswerStatus(DataRecordGroupSpy validationResult, int numberOfErrors) {
		if (numberOfErrors == 0) {
			assertAnswerIsValid(validationResult);
		} else {
			assertAnswerIsInvalid(validationResult);
		}
	}

	private void assertAnswerIsInvalid(DataRecordGroupSpy validationResult) {
		assertValidSetInResultWithValue(validationResult, "false");
	}

	private void assertAnswerIsValid(DataRecordGroupSpy validationResult) {
		assertValidSetInResultWithValue(validationResult, "true");
	}

	private void assertReadActionAddedToValidationRecord(DataRecord validationRecord) {
		((DataRecordSpy) validationRecord).MCR.assertCalledParameters("addAction", Action.READ);
	}

	@Test
	public void testValidatRecordCheckValidationResult() {
		DataGroup validationOrder = createValidationOrderWithMetadataToValidateAndValidateLinks(
				UPDATE, WITH_LINKS);

		DataRecordSpy validationRecord = (DataRecordSpy) recordValidator.validateRecord(
				SOME_AUTH_TOKEN, VALIDATION_ORDER_TYPE, validationOrder, recordToValidate);

		assertAnswerFromRecordValidator(validationRecord);
	}

	@Test
	public void testValidateRecordInvalidData() {
		Iterator<ValidationAnswerSpy> of = List.of(validAnswer, invalidAnswer).iterator();
		dataValidator.MRV.setDefaultReturnValuesSupplier("validateData", () -> of.next());

		DataGroup validationOrder = createValidationOrderWithMetadataToValidateAndValidateLinks(
				UPDATE, WITH_LINKS);

		DataRecord validationRecord = recordValidator.validateRecord(SOME_AUTH_TOKEN,
				VALIDATION_ORDER_TYPE, validationOrder, recordToValidate);

		assertAnswerFromRecordValidator(validationRecord, "some invalid answer 1",
				"some invalid answer 2");
	}

	@Test
	public void testLinkedRecordIdExist() {
		setUpCollectLinksReturnValue();
		DataGroup validationOrder = createValidationOrderWithMetadataToValidateAndValidateLinks(
				CREATE, WITH_LINKS);

		DataRecord validationRecord = recordValidator.validateRecord(SOME_AUTH_TOKEN,
				VALIDATION_ORDER_TYPE, validationOrder, recordToValidate);

		assertAnswerFromRecordValidator(validationRecord);
	}

	@Test
	public void testLinkedRecordIdDoesNotExist() {
		setUpCollectLinksReturnValue();
		recordStorage.MRV.setSpecificReturnValuesSupplier("recordExists", () -> false,
				List.of("toRecordType"), "toRecordId");
		DataGroup validationOrder = createValidationOrderWithMetadataToValidateAndValidateLinks(
				CREATE, WITH_LINKS);

		DataRecord validationRecord = recordValidator.validateRecord(SOME_AUTH_TOKEN,
				VALIDATION_ORDER_TYPE, validationOrder, recordToValidate);

		String errorString = "Data is not valid: linkedRecord does not exists in storage for "
				+ "recordType: toRecordType and recordId: toRecordId";
		assertAnswerFromRecordValidator(validationRecord, errorString);
	}

	@Test
	public void testLinkedRecordIdDoesNotExistDoesNotMatterWhenLinksAreNotChecked() {
		setUpCollectLinksReturnValue();
		recordStorage.MRV.setSpecificReturnValuesSupplier("recordExists", () -> false,
				List.of("toRecordType"), "toRecordId");
		DataGroup validationOrder = createValidationOrderWithMetadataToValidateAndValidateLinks(
				CREATE, WITHOUT_LINKS);

		DataRecord validationRecord = recordValidator.validateRecord(SOME_AUTH_TOKEN,
				VALIDATION_ORDER_TYPE, validationOrder, recordToValidate);

		assertAnswerFromRecordValidator(validationRecord);
	}

	@Test
	public void testRecordUpdaterGetsUniqueValiadatorFromDependencyProvider() throws Exception {
		DataGroup validationOrder = createValidationOrderWithMetadataToValidateAndValidateLinks(
				CREATE, WITH_LINKS);

		DataRecord validationRecord = recordValidator.validateRecord(SOME_AUTH_TOKEN,
				VALIDATION_ORDER_TYPE, validationOrder, recordToValidate);

		assertAnswerFromRecordValidator(validationRecord);
		dependencyProvider.MCR.assertCalledParameters("getUniqueValidator", recordStorage);
	}

	@Test
	public void uniqueValidatorCalledWithCorrectParameters_Create() throws Exception {
		DataGroup validationOrder = createValidationOrderWithMetadataToValidateAndValidateLinks(
				CREATE, WITH_LINKS);

		DataRecord validationRecord = recordValidator.validateRecord(SOME_AUTH_TOKEN,
				VALIDATION_ORDER_TYPE, validationOrder, recordToValidate);

		assertAnswerFromRecordValidator(validationRecord);
		termCollector.MCR.assertParameters("collectTerms", 0,
				recordTypeHandlerForDataToValidate.getDefinitionId(), recordToValidate);
		uniqueValidator.MCR.assertParameters("validateUniqueForNewRecord", 0,
				SPECIFIED_RECORD_TYPE_TO_VALIDATE, uniqueList, collectTerms.storageTerms);
	}

	@Test
	public void uniqueValidatorCalledWithCorrectParameters_Update() throws Exception {
		DataGroup validationOrder = createValidationOrderWithMetadataToValidateAndValidateLinks(
				UPDATE, WITH_LINKS);

		DataRecord validationRecord = recordValidator.validateRecord(SOME_AUTH_TOKEN,
				VALIDATION_ORDER_TYPE, validationOrder, recordToValidate);

		assertAnswerFromRecordValidator(validationRecord);
		termCollector.MCR.assertParameters("collectTerms", 0,
				recordTypeHandlerForDataToValidate.getDefinitionId(), recordToValidate);
		uniqueValidator.MCR.assertParameters("validateUniqueForExistingRecord", 0,
				SPECIFIED_RECORD_TYPE_TO_VALIDATE, recordToValidate.getId(), uniqueList,
				collectTerms.storageTerms);
	}

	@Test
	public void testUniqueValidatorFails_Create() throws Exception {
		uniqueValidator.MRV.setDefaultReturnValuesSupplier("validateUniqueForNewRecord",
				() -> invalidAnswer);
		DataGroup validationOrder = createValidationOrderWithMetadataToValidateAndValidateLinks(
				CREATE, WITH_LINKS);

		DataRecord validationRecord = recordValidator.validateRecord(SOME_AUTH_TOKEN,
				VALIDATION_ORDER_TYPE, validationOrder, recordToValidate);

		assertAnswerFromRecordValidator(validationRecord, "some invalid answer 1",
				"some invalid answer 2");
	}

	@Test
	public void testUniqueValidatorFails_Update() throws Exception {
		uniqueValidator.MRV.setDefaultReturnValuesSupplier("validateUniqueForExistingRecord",
				() -> invalidAnswer);
		DataGroup validationOrder = createValidationOrderWithMetadataToValidateAndValidateLinks(
				UPDATE, WITH_LINKS);

		DataRecord validationRecord = recordValidator.validateRecord(SOME_AUTH_TOKEN,
				VALIDATION_ORDER_TYPE, validationOrder, recordToValidate);

		assertAnswerFromRecordValidator(validationRecord, "some invalid answer 1",
				"some invalid answer 2");
	}

	@Test
	public void testValidateRecordRecordDoesNotExistInvalidDataNotUniqueAndLinksDoesNotExist() {
		setUpAnswerForUniqueValidation();

		Iterator<ValidationAnswerSpy> of = List.of(validAnswer, invalidAnswer).iterator();
		dataValidator.MRV.setDefaultReturnValuesSupplier("validateData", () -> of.next());
		setUpCollectLinksReturnValue();
		recordStorage.MRV.setDefaultReturnValuesSupplier("recordExists", () -> false);
		recordToValidate.MRV.setDefaultReturnValuesSupplier("getType", () -> "someOtherRecordType");

		DataGroup validationOrder = createValidationOrderWithMetadataToValidateAndValidateLinks(
				UPDATE, WITH_LINKS);

		DataRecord validationRecord = recordValidator.validateRecord(SOME_AUTH_TOKEN,
				VALIDATION_ORDER_TYPE, validationOrder, recordToValidate);

		String error0 = "The record validated for update does not exist in storage.";
		String error1 = "Data is not valid: linkedRecord does not exists in storage for "
				+ "recordType: toRecordType and recordId: toRecordId";
		String error2 = "RecordType from record (someOtherRecordType) does not match recordType"
				+ " from validationOrder (" + SPECIFIED_RECORD_TYPE_TO_VALIDATE + ")";
		String error3 = "some invalid answer 1";
		String error4 = "some invalid answer 2";
		String error5 = "Error from unique 1";
		String error6 = "Error from unique 2";

		assertAnswerFromRecordValidator(validationRecord, error0, error1, error2, error3, error4,
				error5, error6);
	}

	private void setUpAnswerForUniqueValidation() {
		ValidationAnswerSpy invalidForUnique = new ValidationAnswerSpy();
		invalidForUnique.MRV.setDefaultReturnValuesSupplier("dataIsInvalid", () -> true);
		invalidForUnique.MRV.setDefaultReturnValuesSupplier("getErrorMessages",
				() -> List.of("Error from unique 1", "Error from unique 2"));
		uniqueValidator.MRV.setDefaultReturnValuesSupplier("validateUniqueForExistingRecord",
				() -> invalidForUnique);
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

	private void assertDataRecordCreatedWithValidationResult(DataRecordGroupSpy validationResult,
			DataRecord validationRecord) {
		var factoredValidationResultRecord = dataFactorySpy.MCR
				.assertCalledParametersReturn("factorRecordUsingDataRecordGroup", validationResult);
		assertSame(validationRecord, factoredValidationResultRecord);
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
}
