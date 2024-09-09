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
import static org.testng.Assert.assertTrue;
import static org.testng.Assert.fail;
import static se.uu.ub.cora.spider.extendedfunctionality.ExtendedFunctionalityPosition.VALIDATE_AFTER_AUTHORIZATION;

import java.util.List;
import java.util.Set;
import java.util.function.Supplier;

import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import se.uu.ub.cora.beefeater.authentication.User;
import se.uu.ub.cora.bookkeeper.recordtype.Unique;
import se.uu.ub.cora.data.Action;
import se.uu.ub.cora.data.DataGroup;
import se.uu.ub.cora.data.DataProvider;
import se.uu.ub.cora.data.DataRecord;
import se.uu.ub.cora.data.collected.CollectTerms;
import se.uu.ub.cora.data.collected.Link;
import se.uu.ub.cora.data.collected.StorageTerm;
import se.uu.ub.cora.data.spies.DataAtomicSpy;
import se.uu.ub.cora.data.spies.DataFactorySpy;
import se.uu.ub.cora.data.spies.DataGroupSpy;
import se.uu.ub.cora.data.spies.DataRecordSpy;
import se.uu.ub.cora.logger.LoggerProvider;
import se.uu.ub.cora.spider.authorization.AuthorizationException;
import se.uu.ub.cora.spider.data.DataAtomicOldSpy;
import se.uu.ub.cora.spider.data.DataGroupOldSpy;
import se.uu.ub.cora.spider.dependency.SpiderInstanceFactory;
import se.uu.ub.cora.spider.dependency.SpiderInstanceFactoryImp;
import se.uu.ub.cora.spider.dependency.SpiderInstanceProvider;
import se.uu.ub.cora.spider.dependency.spy.RecordTypeHandlerSpy;
import se.uu.ub.cora.spider.extendedfunctionality.ExtendedFunctionalityData;
import se.uu.ub.cora.spider.extendedfunctionality.internal.ExtendedFunctionalityProviderSpy;
import se.uu.ub.cora.spider.log.LoggerFactorySpy;
import se.uu.ub.cora.spider.record.DataException;
import se.uu.ub.cora.spider.record.RecordLinkTestsRecordStorage;
import se.uu.ub.cora.spider.record.RecordValidator;
import se.uu.ub.cora.spider.spy.DataGroupTermCollectorSpy;
import se.uu.ub.cora.spider.spy.DataRecordLinkCollectorSpy;
import se.uu.ub.cora.spider.spy.DataValidatorOldSpy;
import se.uu.ub.cora.spider.spy.IdGeneratorSpy;
import se.uu.ub.cora.spider.spy.OldRecordStorageSpy;
import se.uu.ub.cora.spider.spy.OldSpiderAuthorizatorSpy;
import se.uu.ub.cora.spider.spy.RecordStorageForValidateDataSpy;
import se.uu.ub.cora.spider.spy.SpiderDependencyProviderSpy;
import se.uu.ub.cora.spider.spy.UniqueValidatorSpy;
import se.uu.ub.cora.spider.testdata.DataCreator2;
import se.uu.ub.cora.spider.testdata.RecordLinkTestsDataCreator;
import se.uu.ub.cora.storage.RecordStorage;
import se.uu.ub.cora.storage.idgenerator.RecordIdGenerator;

public class RecordValidatorTest {
	private static final String RECORD_TYPE_TO_VALIDATE_AGAINST = "fakeRecordTypeIdFromRecordTypeHandlerSpy";
	private static final String VALIDATION_ORDER_TYPE = "validationOrder";
	private static final String SOME_AUTH_TOKEN = "someToken78678567";
	private static final String TIMESTAMP_FORMAT = "\\d{4}-\\d{2}-\\d{2}T\\d{2}:\\d{2}:\\d{2}\\.\\d{6}Z";
	private RecordValidator recordValidator;
	private RecordStorage recordStorage;
	private AuthenticatorSpy authenticator;
	private OldSpiderAuthorizatorSpy authorizator;
	private DataValidatorOldSpy dataValidator;
	private DataRecordLinkCollectorSpy linkCollector;
	private SpiderDependencyProviderSpy dependencyProvider;
	private ExtendedFunctionalityProviderSpy extendedFunctionalityProvider;
	private RecordIdGenerator idGenerator;
	private LoggerFactorySpy loggerFactorySpy;
	private DataFactorySpy dataFactorySpy;
	private DataGroupTermCollectorSpy termCollector;
	private UniqueValidatorSpy uniqueValidator;

	private RecordTypeHandlerSpy recordTypeHandler;
	private int index;

	@BeforeMethod
	public void beforeMethod() {
		setUpFactoriesAndProviders();
		authenticator = new AuthenticatorSpy();
		authorizator = new OldSpiderAuthorizatorSpy();
		dataValidator = new DataValidatorOldSpy();
		recordStorage = new RecordStorageForValidateDataSpy();
		linkCollector = new DataRecordLinkCollectorSpy();
		extendedFunctionalityProvider = new ExtendedFunctionalityProviderSpy();
		recordTypeHandler = new RecordTypeHandlerSpy();
		idGenerator = new IdGeneratorSpy();
		termCollector = new DataGroupTermCollectorSpy();
		uniqueValidator = new UniqueValidatorSpy();
		index = -1;
		setUpDependencyProvider();
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
		dependencyProvider.MRV.setDefaultReturnValuesSupplier("getRecordTypeHandler",
				() -> recordTypeHandler);
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

	@Test
	public void testExternalDependenciesAreCalledForValidateNew() {
		recordStorage = new RecordStorageForValidateDataSpy();
		setUpDependencyProvider();
		DataGroup recordToValidate = createRecordToValidate();
		DataGroup validationOrder = createValidationOrderWithMetadataToValidateAndValidateLinks(
				"new", "true");

		recordValidator.validateRecord(SOME_AUTH_TOKEN, VALIDATION_ORDER_TYPE, validationOrder,
				recordToValidate);

		authorizator.MCR.assertMethodWasCalled("checkUserIsAuthorizedForActionOnRecordType");

		dataValidator.MCR.assertParameters("validateData", 0,
				"fakeCreateMetadataIdFromRecordTypeHandlerSpy", validationOrder);
		dataValidator.MCR.assertParameters("validateData", 1,
				"fakeCreateMetadataIdFromRecordTypeHandlerSpy", recordToValidate);

		linkCollector.MCR.assertParameters("collectLinks", 0,
				"fakeDefMetadataIdFromRecordTypeHandlerSpy", recordToValidate);
	}

	private DataGroup createRecordToValidate() {
		DataGroup recordToValidate = new DataGroupOldSpy("nameInData");
		recordToValidate.addChild(
				DataCreator2.createRecordInfoWithRecordTypeAndRecordIdAndDataDivider("spyType",
						"spyId", "cora"));
		return recordToValidate;
	}

	private DataGroup createValidationOrderWithMetadataToValidateAndValidateLinks(
			String metadataToValidate, String validateLinks) {
		DataGroup validationOrder = new DataGroupOldSpy(VALIDATION_ORDER_TYPE);
		validationOrder.addChild(new DataAtomicOldSpy("metadataToValidate", metadataToValidate));
		validationOrder.addChild(new DataAtomicOldSpy("validateLinks", validateLinks));

		DataGroup recordTypeToValidateAgainstGroup = new DataGroupOldSpy("recordType");
		recordTypeToValidateAgainstGroup
				.addChild(new DataAtomicOldSpy("linkedRecordType", "recordType"));
		recordTypeToValidateAgainstGroup
				.addChild(new DataAtomicOldSpy("linkedRecordId", RECORD_TYPE_TO_VALIDATE_AGAINST));
		validationOrder.addChild(recordTypeToValidateAgainstGroup);
		return validationOrder;
	}

	@Test
	public void testExternalDependenciesAreCalledForValidateExisting() {
		recordStorage = new RecordStorageForValidateDataSpy();
		setUpDependencyProvider();

		DataGroup dataGroup = createRecordToValidate();
		DataGroup validationOrder = createValidationOrderWithMetadataToValidateAndValidateLinks(
				"existing", "true");
		recordValidator.validateRecord(SOME_AUTH_TOKEN, VALIDATION_ORDER_TYPE, validationOrder,
				dataGroup);

		authorizator.MCR.assertMethodWasCalled("checkUserIsAuthorizedForActionOnRecordType");

		dataValidator.MCR.assertParameters("validateData", 0,
				"fakeCreateMetadataIdFromRecordTypeHandlerSpy", validationOrder);
		dataValidator.MCR.assertParameters("validateData", 1,
				"fakeUpdateMetadataIdFromRecordTypeHandlerSpy", dataGroup);

		linkCollector.MCR.assertParameters("collectLinks", 0,
				"fakeDefMetadataIdFromRecordTypeHandlerSpy", dataGroup);
	}

	@Test
	public void testLinkCollectorIsNotCalledWhenValidateLinksIsFalse() {
		authorizator = new OldSpiderAuthorizatorSpy();
		recordStorage = new RecordStorageForValidateDataSpy();
		setUpDependencyProvider();

		DataGroup dataGroup = new DataGroupOldSpy("nameInData");
		dataGroup.addChild(DataCreator2.createRecordInfoWithRecordTypeAndRecordIdAndDataDivider(
				"spyType", "spyId", "cora"));
		DataGroup validationOrder = createValidationOrderWithMetadataToValidateAndValidateLinks(
				"existing", "false");
		recordValidator.validateRecord(SOME_AUTH_TOKEN, VALIDATION_ORDER_TYPE, validationOrder,
				dataGroup);
		linkCollector.MCR.methodWasCalled("collectLinks");
	}

	@Test
	public void testValidatRecordDataValidDataForCreateUsesNewMetadataId() {
		recordStorage = new RecordStorageForValidateDataSpy();
		setUpDependencyProvider();

		DataGroup dataGroup = createDataGroupPlace();
		DataGroup validationOrder = createValidationOrderWithMetadataToValidateAndValidateLinks(
				"new", "true");

		DataGroupSpy validationResult = setUpValidationResultForValid();
		DataRecord validationResultRecord = recordValidator.validateRecord(SOME_AUTH_TOKEN,
				RECORD_TYPE_TO_VALIDATE_AGAINST, validationOrder, dataGroup);

		assertDataRecordCreatedWithValidationResult(validationResult, validationResultRecord);
		assertValidSetInResultWithValue(validationResult, "true");

		dataValidator.MCR.assertParameter("validateData", 0, "metadataGroupId",
				"fakeCreateMetadataIdFromRecordTypeHandlerSpy");
		dataValidator.MCR.assertParameter("validateData", 1, "metadataGroupId",
				"fakeCreateMetadataIdFromRecordTypeHandlerSpy");
	}

	private DataGroup createDataGroupPlace() {
		DataGroup dataGroup = new DataGroupOldSpy("typeWithUserGeneratedId");
		createAndAddRecordInfo(dataGroup);
		dataGroup.addChild(new DataAtomicOldSpy("atomicId", "atomicValue"));
		return dataGroup;
	}

	private void createAndAddRecordInfo(DataGroup dataGroup) {
		DataGroup createRecordInfo = new DataGroupOldSpy("recordInfo");
		createRecordInfo.addChild(new DataAtomicOldSpy("id", "place"));
		createRecordInfo.addChild(new DataAtomicOldSpy("type", "recordType"));
		dataGroup.addChild(createRecordInfo);
	}

	@Test
	public void testValidatRecordDataValidDataForUpdate() {
		recordStorage = new RecordStorageForValidateDataSpy();
		setUpDependencyProvider();

		DataGroup dataGroup = createDataGroupPlace();
		DataGroup validationOrder = createValidationOrderWithMetadataToValidateAndValidateLinks(
				"existing", "true");
		DataGroupSpy validationResult = setUpValidationResultForValid();

		DataRecord validationResultRecord = recordValidator.validateRecord(SOME_AUTH_TOKEN,
				VALIDATION_ORDER_TYPE, validationOrder, dataGroup);

		assertDataRecordCreatedWithValidationResult(validationResult, validationResultRecord);
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
		recordStorage = new RecordStorageForValidateDataSpy();
		setUpDependencyProvider();

		DataGroup dataGroup = createDataGroupPlace();
		DataGroup validationOrder = createValidationOrderWithMetadataToValidateAndValidateLinks(
				"existing", "true");

		DataGroupSpy validationResult = setUpValidationResultForValid();

		DataRecordSpy validationResultRecord = (DataRecordSpy) recordValidator
				.validateRecord(SOME_AUTH_TOKEN, VALIDATION_ORDER_TYPE, validationOrder, dataGroup);

		assertValidSetInResultWithValue(validationResult, "true");

		IdGeneratorSpy generatorSpy = (IdGeneratorSpy) idGenerator;
		assertCorrectRecordInfo(validationResult, generatorSpy);

		validationResultRecord.MCR.assertCalledParameters("addAction", Action.READ);
	}

	private void assertCorrectRecordInfo(DataGroupSpy validationResult,
			IdGeneratorSpy generatorSpy) {
		DataGroupSpy recordInfo = (DataGroupSpy) dataFactorySpy.MCR
				.assertCalledParametersReturn("factorGroupUsingNameInData", "recordInfo");
		validationResult.MCR.assertCalledParameters("addChild", recordInfo);

		dataFactorySpy.MCR.assertCalledParametersReturn("factorAtomicUsingNameInDataAndValue", "id",
				generatorSpy.generatedId);

		dataFactorySpy.MCR.assertParameters("factorRecordLinkUsingNameInDataAndTypeAndId", 0,
				"type", "recordType", VALIDATION_ORDER_TYPE);
		var typeLink = dataFactorySpy.MCR
				.getReturnValue("factorRecordLinkUsingNameInDataAndTypeAndId", 0);
		recordInfo.MCR.assertCalledParameters("addChild", typeLink);

		dataFactorySpy.MCR.assertParameters("factorAtomicUsingNameInDataAndValue", 1, "tsCreated");
		String tsCreatedAsString = (String) dataFactorySpy.MCR
				.getValueForMethodNameAndCallNumberAndParameterName(
						"factorAtomicUsingNameInDataAndValue", 1, "value");
		assertTrue(tsCreatedAsString.matches(TIMESTAMP_FORMAT));

		var createdByLink = dataFactorySpy.MCR.assertCalledParametersReturn(
				"factorRecordLinkUsingNameInDataAndTypeAndId", "createdBy", "user", "userSpy");
		recordInfo.MCR.assertCalledParameters("addChild", createdByLink);

		DataGroupSpy updated = (DataGroupSpy) dataFactorySpy.MCR
				.assertCalledParametersReturn("factorGroupUsingNameInData", "updated");
		var tsUpdated = dataFactorySpy.MCR.getReturnValue("factorAtomicUsingNameInDataAndValue", 2);
		updated.MCR.assertCalledParameters("addChild", tsUpdated);
	}

	@Test
	public void testValidateRecordInvalidData() {
		dataValidator.setNotValidForMetadataGroupId(RECORD_TYPE_TO_VALIDATE_AGAINST);
		RecordTypeHandlerSpy placeTypeHandler = new RecordTypeHandlerSpy();
		placeTypeHandler.MRV.setDefaultReturnValuesSupplier("getCreateDefinitonId",
				(Supplier<String>) () -> RECORD_TYPE_TO_VALIDATE_AGAINST);
		placeTypeHandler.MRV.setDefaultReturnValuesSupplier("getUpdateDefinitionId",
				(Supplier<String>) () -> RECORD_TYPE_TO_VALIDATE_AGAINST);
		placeTypeHandler.MRV.setDefaultReturnValuesSupplier("getDefinitionId",
				(Supplier<String>) () -> RECORD_TYPE_TO_VALIDATE_AGAINST);
		dependencyProvider.MRV.setDefaultReturnValuesSupplier(
				"getRecordTypeHandlerUsingDataRecordGroup", () -> placeTypeHandler);

		DataGroup dataGroup = createDataGroupPlace();
		DataGroup validationOrder = createValidationOrderWithMetadataToValidateAndValidateLinks(
				"existing", "true");

		DataGroupSpy validationResult = setUpValidationResultForError();

		DataRecord validationResultRecord = recordValidator.validateRecord(SOME_AUTH_TOKEN,
				VALIDATION_ORDER_TYPE, validationOrder, dataGroup);

		assertDataRecordCreatedWithValidationResult(validationResult, validationResultRecord);

		assertValidSetInResultWithValue(validationResult, "false");

		dataValidator.MCR.assertParameters("validateData", 0,
				"fakeCreateMetadataIdFromRecordTypeHandlerSpy");
		dataValidator.MCR.assertParameters("validateData", 1, RECORD_TYPE_TO_VALIDATE_AGAINST);
		dataValidator.MCR.assertNumberOfCallsToMethod("validateData", 2);
	}

	@Test
	public void testLinkedRecordIdDoesNotExist() {
		fillCollectLinksReturnValue();

		DataGroup dataGroup = RecordLinkTestsDataCreator
				.createDataDataGroupWithRecordInfoAndLinkOneLevelDown();
		DataGroup validationOrder = createValidationOrderWithMetadataToValidateAndValidateLinks(
				"new", "true");

		DataGroupSpy validationResult = setUpValidationResultForError();

		DataRecord validationResultRecord = recordValidator.validateRecord(SOME_AUTH_TOKEN,
				RECORD_TYPE_TO_VALIDATE_AGAINST, validationOrder, dataGroup);

		String errorString = "Data is not valid: linkedRecord does not exists in storage for "
				+ "recordType: toRecordType and recordId: toRecordId";

		assertDataRecordCreatedWithValidationResult(validationResult, validationResultRecord);
		assertValidSetInResultWithValue(validationResult, "false");
		assertErrorMessages(validationResult, errorString);
	}

	private void assertDataRecordCreatedWithValidationResult(DataGroup validationResult,
			DataRecord validationResultRecord) {
		var factoredValidationResultRecord = dataFactorySpy.MCR
				.assertCalledParametersReturn("factorRecordUsingDataGroup", validationResult);
		assertSame(validationResultRecord, factoredValidationResultRecord);
	}

	private DataGroupSpy setUpValidationResultForError() {
		DataGroupSpy validationResult = new DataGroupSpy();
		validationResult.MRV.setSpecificReturnValuesSupplier("containsChildWithNameInData",
				() -> true, "errorMessages");
		dataFactorySpy.MRV.setSpecificReturnValuesSupplier("factorGroupUsingNameInData",
				() -> validationResult, "validationResult");
		return validationResult;
	}

	private void assertErrorMessages(DataGroupSpy validationResult, String... errorStrings) {
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

	private void assertValidSetInResultWithValue(DataGroupSpy validationResult, String validValue) {
		var valid = dataFactorySpy.MCR.assertCalledParametersReturn(
				"factorAtomicUsingNameInDataAndValue", "valid", validValue);
		validationResult.MCR.assertCalledParameters("addChild", valid);
	}

	@Test
	public void testValidateRecordInvalidDataAndLinksDoesNotExist() {
		recordStorage = new RecordLinkTestsRecordStorage();
		dataValidator.setNotValidForMetadataGroupId("dataWithLinksNew");
		setUpDependencyProvider();

		dependencyProvider.MRV.setDefaultReturnValuesSupplier(
				"getRecordTypeHandlerUsingDataRecordGroup",
				() -> createDifferentRecordTypeHandlers());

		((RecordLinkTestsRecordStorage) recordStorage).recordIdExistsForRecordType = false;

		fillCollectLinksReturnValue();

		DataGroup dataGroup = RecordLinkTestsDataCreator
				.createDataDataGroupWithRecordInfoAndLinkOneLevelDown();
		DataGroup validationOrder = createValidationOrderWithMetadataToValidateAndValidateLinks(
				"new", "true");

		DataGroupSpy validationResult = setUpValidationResultForError();

		DataRecord validationResultRecord = recordValidator.validateRecord(SOME_AUTH_TOKEN,
				VALIDATION_ORDER_TYPE, validationOrder, dataGroup);

		assertDataRecordCreatedWithValidationResult(validationResult, validationResultRecord);
		assertValidSetInResultWithValue(validationResult, "false");

		String errorString = "Data is not valid: linkedRecord does not exists in storage for recordType: toRecordType and recordId: toRecordId";
		String errorString2 = "Data invalid for metadataId dataWithLinksNew";

		assertErrorMessages(validationResult, errorString, errorString2);
	}

	private RecordTypeHandlerSpy createDifferentRecordTypeHandlers() {
		RecordTypeHandlerSpy validationOrderTypeHandler = new RecordTypeHandlerSpy();
		RecordTypeHandlerSpy placeTypeHandler = new RecordTypeHandlerSpy();
		placeTypeHandler.MRV.setDefaultReturnValuesSupplier("getCreateDefinitionId",
				(Supplier<String>) () -> "dataWithLinksNew");
		placeTypeHandler.MRV.setDefaultReturnValuesSupplier("getUpdateDefinitionId",
				(Supplier<String>) () -> "dataWithLinksNew");
		placeTypeHandler.MRV.setDefaultReturnValuesSupplier("getDefinitionId",
				(Supplier<String>) () -> "dataWithLinksNew");
		List<RecordTypeHandlerSpy> handlers = List.of(validationOrderTypeHandler, placeTypeHandler);
		index++;
		return handlers.get(index);
	}

	@Test
	public void testLinkedRecordIdDoesNotExistDoesNotMatterWhenLinksAreNotChecked() {
		fillCollectLinksReturnValue();

		DataGroup dataGroup = RecordLinkTestsDataCreator
				.createDataDataGroupWithRecordInfoAndLinkOneLevelDown();
		DataGroup validationOrder = createValidationOrderWithMetadataToValidateAndValidateLinks(
				"new", "false");
		DataGroupSpy validationResult = setUpValidationResultForValid();

		DataRecord validationResultRecord = recordValidator.validateRecord(SOME_AUTH_TOKEN,
				RECORD_TYPE_TO_VALIDATE_AGAINST, validationOrder, dataGroup);

		assertDataRecordCreatedWithValidationResult(validationResult, validationResultRecord);
		assertValidSetInResultWithValue(validationResult, "true");
	}

	private DataGroupSpy setUpValidationResultForValid() {
		DataGroupSpy validationResult = new DataGroupSpy();
		validationResult.MRV.setSpecificReturnValuesSupplier("containsChildWithNameInData",
				() -> false, "errorMessages");
		dataFactorySpy.MRV.setSpecificReturnValuesSupplier("factorGroupUsingNameInData",
				() -> validationResult, "validationResult");
		return validationResult;
	}

	private void fillCollectLinksReturnValue() {
		Link link = new Link("toRecordType", "toRecordId");
		linkCollector.MRV.setDefaultReturnValuesSupplier("collectLinks", () -> Set.of(link));
	}

	@Test
	public void testUnauthorizedForCreateOnValidationorderRecordTypeShouldNotValidate() {
		recordStorage = new OldRecordStorageSpy();
		authorizator.authorizedForActionAndRecordType = false;
		setUpDependencyProvider();

		DataGroup dataGroup = createDataGroupPlace();
		DataGroup validationOrder = createValidationOrderWithMetadataToValidateAndValidateLinks(
				"existing", "true");
		changeRecordTypeTo(validationOrder, "spyType");
		boolean exceptionWasCaught = false;
		try {
			recordValidator.validateRecord(SOME_AUTH_TOKEN, VALIDATION_ORDER_TYPE, validationOrder,
					dataGroup);
		} catch (Exception e) {
			assertEquals(e.getClass(), AuthorizationException.class);
			assertEquals(e.getMessage(), "Exception from SpiderAuthorizatorSpy");
			exceptionWasCaught = true;
		}

		authorizator.MCR.assertParameter("checkUserIsAuthorizedForActionOnRecordType", 0, "action",
				"create");

		assertTrue(exceptionWasCaught);
		dataValidator.MCR.assertMethodNotCalled("validateData");
	}

	@Test

	public void testInvalidDataForCreateOnValidationOrderShouldThrowException() {
		recordStorage = new RecordStorageForValidateDataSpy();
		dataValidator.setNotValidForMetadataGroupId("fakeCreateMetadataIdFromRecordTypeHandlerSpy");
		setUpDependencyProvider();

		DataGroup dataGroup = createDataGroupPlace();
		DataGroup validationOrder = createValidationOrderWithMetadataToValidateAndValidateLinks(
				"existing", "true");
		boolean exceptionWasCaught = false;
		try {
			recordValidator.validateRecord(SOME_AUTH_TOKEN, VALIDATION_ORDER_TYPE, validationOrder,
					dataGroup);
		} catch (Exception e) {
			assertEquals(e.getClass(), DataException.class);
			assertEquals(e.getMessage(),
					"Data is not valid: [Data invalid for metadataId fakeCreateMetadataIdFromRecordTypeHandlerSpy]");
			exceptionWasCaught = true;
		}
		assertTrue(exceptionWasCaught);
	}

	@Test
	public void testUnauthorizedForValidateOnRecordTypeShouldNotValidateDataForThatType() {
		recordStorage = new RecordStorageForValidateDataSpy();
		authorizator.setNotAutorizedForAction("validate");

		setUpDependencyProvider();

		DataGroup dataGroup = createRecordToValidate();

		DataGroup validationOrder = createValidationOrderWithMetadataToValidateAndValidateLinks(
				"existing", "true");

		changeRecordTypeTo(validationOrder, "spyType");
		boolean exceptionWasCaught = false;
		try {
			recordValidator.validateRecord(SOME_AUTH_TOKEN, VALIDATION_ORDER_TYPE, validationOrder,
					dataGroup);
		} catch (Exception e) {
			assertEquals(e.getClass(), AuthorizationException.class);
			exceptionWasCaught = true;
		}
		assertTrue(exceptionWasCaught);
		dataValidator.MCR.assertNumberOfCallsToMethod("validateData", 1);
	}

	private void changeRecordTypeTo(DataGroup validationOrder, String recordType) {
		DataGroup recordTypeGroup = validationOrder.getFirstGroupWithNameInData("recordType");
		recordTypeGroup.removeFirstChildWithNameInData("linkedRecordId");
		recordTypeGroup.addChild(new DataAtomicOldSpy("linkedRecordId", recordType));
	}

	@Test
	public void testNonExistingRecordType() {
		DataGroup dataGroup = createDataGroupPlace();

		DataGroup validationOrder = createValidationOrderWithMetadataToValidateAndValidateLinks(
				"existing", "true");
		changeRecordTypeTo(validationOrder, "recordType_NOT_EXISTING");
		DataGroupSpy validationResult = setUpValidationResultForError();

		DataRecord validationResultRecord = recordValidator.validateRecord(SOME_AUTH_TOKEN,
				VALIDATION_ORDER_TYPE, validationOrder, dataGroup);

		String errorString = "No records exists with recordType: recordType_NOT_EXISTING and "
				+ "recordId place";
		assertDataRecordCreatedWithValidationResult(validationResult, validationResultRecord);
		assertValidSetInResultWithValue(validationResult, "false");
		assertErrorMessages(validationResult, errorString);
	}

	@Test
	public void testUseExtendedFunctionalityExtendedFunctionalitiesExists() throws Exception {
		DataGroup recordToValidate = createRecordToValidate();
		DataGroup validationOrder = createValidationOrderWithMetadataToValidateAndValidateLinks(
				"new", "true");

		recordValidator.validateRecord(SOME_AUTH_TOKEN, VALIDATION_ORDER_TYPE, validationOrder,
				recordToValidate);

		ExtendedFunctionalityData expectedData = new ExtendedFunctionalityData();
		expectedData.authToken = SOME_AUTH_TOKEN;
		expectedData.recordType = RECORD_TYPE_TO_VALIDATE_AGAINST;
		expectedData.recordId = null;
		expectedData.user = (User) authenticator.MCR.getReturnValue("getUserForToken", 0);
		expectedData.previouslyStoredTopDataGroup = null;
		expectedData.dataGroup = recordToValidate;
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
		DataGroup recordToValidate = createRecordToValidate();
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
		DataGroup recordToValidate = createRecordToValidate();
		recordTypeHandler.MRV.setDefaultReturnValuesSupplier("getRecordTypeId",
				() -> "someOtherRecordType");
		DataGroupSpy validationResult = setUpValidationResultForError();

		DataRecord validationResultRecord = recordValidator.validateRecord(SOME_AUTH_TOKEN,
				VALIDATION_ORDER_TYPE, validationOrder, recordToValidate);

		String errorString = "RecordType from record (someOtherRecordType) does not match recordType from validationOrder ("
				+ RECORD_TYPE_TO_VALIDATE_AGAINST + ")";
		assertDataRecordCreatedWithValidationResult(validationResult, validationResultRecord);
		assertValidSetInResultWithValue(validationResult, "false");
		assertErrorMessages(validationResult, errorString);
	}

	@Test
	public void testRecordUpdaterGetsUniqueValiadatorFromDependencyProvider() throws Exception {
		recordStorage = new RecordStorageForValidateDataSpy();
		setUpDependencyProvider();

		DataGroup recordToValidate = createDataGroupPlace();
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
		CollectTerms collectTerms = new CollectTerms();
		collectTerms.storageTerms = Set.of(new StorageTerm("id", "key", "value"));
		termCollector.MRV.setDefaultReturnValuesSupplier("collectTerms", () -> collectTerms);

		recordStorage = new RecordStorageForValidateDataSpy();
		setUpDependencyProvider();

		DataGroup recordToValidate = createDataGroupPlace();
		DataGroup validationOrder = createValidationOrderWithMetadataToValidateAndValidateLinks(
				"new", "true");

		// DataGroupSpy validationResult = setUpValidationResultForValid();

		recordValidator.validateRecord(SOME_AUTH_TOKEN, VALIDATION_ORDER_TYPE, validationOrder,
				recordToValidate);

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
