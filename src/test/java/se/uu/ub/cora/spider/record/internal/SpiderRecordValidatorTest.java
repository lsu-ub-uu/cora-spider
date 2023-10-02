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

package se.uu.ub.cora.spider.record.internal;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertTrue;

import java.util.List;
import java.util.Set;
import java.util.function.Supplier;

import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import se.uu.ub.cora.bookkeeper.termcollector.DataGroupTermCollector;
import se.uu.ub.cora.data.DataAtomic;
import se.uu.ub.cora.data.DataAtomicFactory;
import se.uu.ub.cora.data.DataAtomicProvider;
import se.uu.ub.cora.data.DataChild;
import se.uu.ub.cora.data.DataGroup;
import se.uu.ub.cora.data.DataGroupFactory;
import se.uu.ub.cora.data.DataGroupProvider;
import se.uu.ub.cora.data.DataProvider;
import se.uu.ub.cora.data.DataRecord;
import se.uu.ub.cora.data.DataRecordFactory;
import se.uu.ub.cora.data.DataRecordLinkFactory;
import se.uu.ub.cora.data.DataRecordLinkProvider;
import se.uu.ub.cora.data.DataRecordProvider;
import se.uu.ub.cora.data.collected.Link;
import se.uu.ub.cora.data.spies.DataFactorySpy;
import se.uu.ub.cora.data.spies.DataGroupSpy;
import se.uu.ub.cora.logger.LoggerProvider;
import se.uu.ub.cora.search.RecordIndexer;
import se.uu.ub.cora.spider.authentication.Authenticator;
import se.uu.ub.cora.spider.authentication.OldAuthenticatorSpy;
import se.uu.ub.cora.spider.authorization.AuthorizationException;
import se.uu.ub.cora.spider.authorization.PermissionRuleCalculator;
import se.uu.ub.cora.spider.data.DataAtomicFactorySpy;
import se.uu.ub.cora.spider.data.DataAtomicSpy;
import se.uu.ub.cora.spider.data.DataGroupFactorySpy;
import se.uu.ub.cora.spider.data.DataGroupOldSpy;
import se.uu.ub.cora.spider.data.DataRecordFactorySpy;
import se.uu.ub.cora.spider.dependency.spy.RecordTypeHandlerSpy;
import se.uu.ub.cora.spider.dependency.spy.SpiderDependencyProviderOldSpy;
import se.uu.ub.cora.spider.extendedfunctionality.internal.ExtendedFunctionalityProviderSpy;
import se.uu.ub.cora.spider.log.LoggerFactorySpy;
import se.uu.ub.cora.spider.record.DataException;
import se.uu.ub.cora.spider.record.DataRecordLinkFactorySpy;
import se.uu.ub.cora.spider.record.RecordLinkTestsRecordStorage;
import se.uu.ub.cora.spider.record.RecordValidator;
import se.uu.ub.cora.spider.spy.DataGroupTermCollectorSpy;
import se.uu.ub.cora.spider.spy.DataRecordLinkCollectorSpy;
import se.uu.ub.cora.spider.spy.DataValidatorSpy;
import se.uu.ub.cora.spider.spy.IdGeneratorSpy;
import se.uu.ub.cora.spider.spy.OldRecordStorageSpy;
import se.uu.ub.cora.spider.spy.OldSpiderAuthorizatorSpy;
import se.uu.ub.cora.spider.spy.RecordIndexerSpy;
import se.uu.ub.cora.spider.spy.RecordStorageForValidateDataSpy;
import se.uu.ub.cora.spider.spy.RuleCalculatorSpy;
import se.uu.ub.cora.spider.testdata.DataCreator2;
import se.uu.ub.cora.spider.testdata.RecordLinkTestsDataCreator;
import se.uu.ub.cora.storage.RecordStorage;
import se.uu.ub.cora.storage.idgenerator.RecordIdGenerator;

public class SpiderRecordValidatorTest {
	private static final String TIMESTAMP_FORMAT = "\\d{4}-\\d{2}-\\d{2}T\\d{2}:\\d{2}:\\d{2}\\.\\d{6}Z";
	private RecordStorage recordStorage;
	private Authenticator authenticator;
	private OldSpiderAuthorizatorSpy authorizator;
	private PermissionRuleCalculator ruleCalculator;
	private RecordValidator recordValidator;
	private DataValidatorSpy dataValidator;
	private DataRecordLinkCollectorSpy linkCollector;
	private SpiderDependencyProviderOldSpy dependencyProvider;
	private ExtendedFunctionalityProviderSpy extendedFunctionalityProvider;
	private DataGroupTermCollector termCollector;
	private RecordIndexer recordIndexer;
	private RecordIdGenerator idGenerator;
	private LoggerFactorySpy loggerFactorySpy;
	private DataFactorySpy dataFactorySpy;

	private DataGroupFactory dataGroupFactorySpy;
	private DataAtomicFactory dataAtomicFactorySpy;
	private DataRecordFactory dataRecordFactorySpy;
	private DataRecordLinkFactory dataRecordLinkFactory;

	@BeforeMethod
	public void beforeMethod() {
		setUpFactoriesAndProviders();
		authenticator = new OldAuthenticatorSpy();
		authorizator = new OldSpiderAuthorizatorSpy();
		dataValidator = new DataValidatorSpy();
		recordStorage = new RecordStorageForValidateDataSpy();
		ruleCalculator = new RuleCalculatorSpy();
		linkCollector = new DataRecordLinkCollectorSpy();
		termCollector = new DataGroupTermCollectorSpy();
		recordIndexer = new RecordIndexerSpy();
		extendedFunctionalityProvider = new ExtendedFunctionalityProviderSpy();
		idGenerator = new IdGeneratorSpy();
		index = -1;
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
		dataRecordFactorySpy = new DataRecordFactorySpy();
		DataRecordProvider.setDataRecordFactory(dataRecordFactorySpy);
		dataRecordLinkFactory = new DataRecordLinkFactorySpy();
		DataRecordLinkProvider.setDataRecordLinkFactory(dataRecordLinkFactory);
	}

	private void setUpDependencyProvider() {
		dependencyProvider = new SpiderDependencyProviderOldSpy();
		dependencyProvider.authenticator = authenticator;
		dependencyProvider.spiderAuthorizator = authorizator;
		dependencyProvider.dataValidator = dataValidator;
		dependencyProvider.recordStorage = recordStorage;
		dependencyProvider.recordIdGenerator = idGenerator;
		dependencyProvider.ruleCalculator = ruleCalculator;
		dependencyProvider.linkCollector = linkCollector;
		dependencyProvider.extendedFunctionalityProvider = extendedFunctionalityProvider;
		dependencyProvider.termCollector = termCollector;
		dependencyProvider.recordIndexer = recordIndexer;
		recordValidator = RecordValidatorImp.usingDependencyProvider(dependencyProvider);
	}

	@Test
	public void testExternalDependenciesAreCalledForValidateNew() {
		recordStorage = new RecordStorageForValidateDataSpy();
		ruleCalculator = new RuleCalculatorSpy();
		setUpDependencyProvider();

		DataGroup recordToValidate = new DataGroupOldSpy("nameInData");
		recordToValidate.addChild(
				DataCreator2.createRecordInfoWithRecordTypeAndRecordIdAndDataDivider("spyType",
						"spyId", "cora"));

		DataGroup validationOrder = createValidationOrderWithMetadataToValidateAndValidateLinks(
				"new", "true");
		recordValidator.validateRecord("someToken78678567", "validationOrder", validationOrder,
				recordToValidate);

		authorizator.MCR.assertMethodWasCalled("checkUserIsAuthorizedForActionOnRecordType");

		dataValidator.MCR.assertParameters("validateData", 0,
				"fakeCreateMetadataIdFromRecordTypeHandlerSpy", validationOrder);
		dataValidator.MCR.assertParameters("validateData", 1,
				"fakeCreateMetadataIdFromRecordTypeHandlerSpy", recordToValidate);

		linkCollector.MCR.assertParameters("collectLinks", 0,
				"fakeDefMetadataIdFromRecordTypeHandlerSpy", recordToValidate);
	}

	private DataGroup createValidationOrderWithMetadataToValidateAndValidateLinks(
			String metadataToValidate, String validateLinks) {
		DataGroup validationOrder = new DataGroupOldSpy("validationOrder");
		validationOrder.addChild(new DataAtomicSpy("metadataToValidate", metadataToValidate));
		validationOrder.addChild(new DataAtomicSpy("validateLinks", validateLinks));

		DataGroup recordTypeGroup = new DataGroupOldSpy("recordType");
		recordTypeGroup.addChild(new DataAtomicSpy("linkedRecordType", "recordType"));
		recordTypeGroup.addChild(new DataAtomicSpy("linkedRecordId", "text"));
		validationOrder.addChild(recordTypeGroup);
		return validationOrder;
	}

	@Test
	public void testExternalDependenciesAreCalledForValidateExisting() {
		recordStorage = new RecordStorageForValidateDataSpy();
		ruleCalculator = new RuleCalculatorSpy();
		setUpDependencyProvider();

		DataGroup dataGroup = new DataGroupOldSpy("nameInData");
		dataGroup.addChild(DataCreator2.createRecordInfoWithRecordTypeAndRecordIdAndDataDivider(
				"spyType", "spyId", "cora"));
		DataGroup validationOrder = createValidationOrderWithMetadataToValidateAndValidateLinks(
				"existing", "true");
		recordValidator.validateRecord("someToken78678567", "validationOrder", validationOrder,
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
		ruleCalculator = new RuleCalculatorSpy();
		setUpDependencyProvider();

		DataGroup dataGroup = new DataGroupOldSpy("nameInData");
		dataGroup.addChild(DataCreator2.createRecordInfoWithRecordTypeAndRecordIdAndDataDivider(
				"spyType", "spyId", "cora"));
		DataGroup validationOrder = createValidationOrderWithMetadataToValidateAndValidateLinks(
				"existing", "false");
		recordValidator.validateRecord("someToken78678567", "validationOrder", validationOrder,
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
		DataRecord validationResultRecord = recordValidator.validateRecord("someToken78678567",
				"text", validationOrder, dataGroup);
		DataGroup validationResult = validationResultRecord.getDataGroup();
		assertEquals(validationResult.getFirstAtomicValueWithNameInData("valid"), "true");

		dataValidator.MCR.assertParameter("validateData", 0, "metadataGroupId",
				"fakeCreateMetadataIdFromRecordTypeHandlerSpy");
		dataValidator.MCR.assertParameter("validateData", 1, "metadataGroupId",
				"fakeCreateMetadataIdFromRecordTypeHandlerSpy");
	}

	private DataGroup createDataGroupPlace() {
		DataGroup dataGroup = new DataGroupOldSpy("typeWithUserGeneratedId");
		createAndAddRecordInfo(dataGroup);
		dataGroup.addChild(new DataAtomicSpy("atomicId", "atomicValue"));
		return dataGroup;
	}

	private void createAndAddRecordInfo(DataGroup dataGroup) {
		DataGroup createRecordInfo = new DataGroupOldSpy("recordInfo");
		createRecordInfo.addChild(new DataAtomicSpy("id", "place"));
		createRecordInfo.addChild(new DataAtomicSpy("type", "recordType"));
		dataGroup.addChild(createRecordInfo);
	}

	@Test
	public void testValidatRecordDataValidDataForUpdate() {
		recordStorage = new RecordStorageForValidateDataSpy();
		setUpDependencyProvider();

		DataGroup dataGroup = createDataGroupPlace();
		DataGroup validationOrder = createValidationOrderWithMetadataToValidateAndValidateLinks(
				"existing", "true");
		DataRecord validationResultRecord = recordValidator.validateRecord("someToken78678567",
				"validationOrder", validationOrder, dataGroup);
		DataGroup validationResult = validationResultRecord.getDataGroup();
		assertEquals(validationResult.getFirstAtomicValueWithNameInData("valid"), "true");

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
		DataRecord validationResultRecord = recordValidator.validateRecord("someToken78678567",
				"validationOrder", validationOrder, dataGroup);
		DataGroup validationResult = validationResultRecord.getDataGroup();
		assertEquals(validationResult.getFirstAtomicValueWithNameInData("valid"), "true");

		IdGeneratorSpy generatorSpy = (IdGeneratorSpy) idGenerator;
		assertCorrectRecordInfo(validationResult, generatorSpy);

		assertTrue(validationResultRecord.getActions().contains(se.uu.ub.cora.data.Action.READ));
	}

	private void assertCorrectRecordInfo(DataGroup validationResult, IdGeneratorSpy generatorSpy) {
		DataGroup recordInfo = validationResult.getFirstGroupWithNameInData("recordInfo");
		assertEquals(recordInfo.getFirstAtomicValueWithNameInData("id"), generatorSpy.generatedId);

		dataFactorySpy.MCR.assertParameters("factorRecordLinkUsingNameInDataAndTypeAndId", 0,
				"type", "recordType", "validationOrder");
		var typeLink = dataFactorySpy.MCR
				.getReturnValue("factorRecordLinkUsingNameInDataAndTypeAndId", 0);
		assertDataChildFoundInChildren(typeLink, recordInfo.getChildren());

		String tsCreated = recordInfo.getFirstAtomicValueWithNameInData("tsCreated");
		assertTrue(tsCreated.matches(TIMESTAMP_FORMAT));

		dataFactorySpy.MCR.assertParameters("factorRecordLinkUsingNameInDataAndTypeAndId", 1,
				"createdBy", "user", "12345");
		var createdByLink = dataFactorySpy.MCR
				.getReturnValue("factorRecordLinkUsingNameInDataAndTypeAndId", 1);
		assertDataChildFoundInChildren(createdByLink, recordInfo.getChildren());

		DataGroupSpy updated = (DataGroupSpy) dataFactorySpy.MCR
				.getReturnValue("factorGroupUsingNameInData", 0);
		dataFactorySpy.MCR.assertParameters("factorAtomicUsingNameInDataAndValue", 0, "tsUpdated",
				tsCreated);
		var createdTsCreated = dataFactorySpy.MCR
				.getReturnValue("factorAtomicUsingNameInDataAndValue", 0);
		updated.MCR.assertParameters("addChild", 1, createdTsCreated);
		assertFalse(recordInfo.containsChildWithNameInData("tsUpdated"));

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
	public void testValidateRecordInvalidData() {
		dataValidator.setNotValidForMetadataGroupId("text");
		RecordTypeHandlerSpy placeTypeHandler = new RecordTypeHandlerSpy();
		placeTypeHandler.MRV.setDefaultReturnValuesSupplier("getCreateDefinitonId",
				(Supplier<String>) () -> "text");
		placeTypeHandler.MRV.setDefaultReturnValuesSupplier("getUpdateDefinitionId",
				(Supplier<String>) () -> "text");
		placeTypeHandler.MRV.setDefaultReturnValuesSupplier("getDefinitionId",
				(Supplier<String>) () -> "text");
		dependencyProvider.mapOfRecordTypeHandlerSpies.put("text", placeTypeHandler);
		dependencyProvider.MRV.setDefaultReturnValuesSupplier(
				"getRecordTypeHandlerUsingDataRecordGroup", () -> placeTypeHandler);

		DataGroup dataGroup = createDataGroupPlace();
		DataGroup validationOrder = createValidationOrderWithMetadataToValidateAndValidateLinks(
				"existing", "true");

		DataRecord validationResultRecord = recordValidator.validateRecord("someToken78678567",
				"validationOrder", validationOrder, dataGroup);

		dataValidator.MCR.assertParameters("validateData", 0,
				"fakeCreateMetadataIdFromRecordTypeHandlerSpy");
		dataValidator.MCR.assertParameters("validateData", 1, "text");

		DataGroup validationResult = validationResultRecord.getDataGroup();
		assertEquals(validationResult.getFirstAtomicValueWithNameInData("valid"), "false");

		dataValidator.MCR.assertNumberOfCallsToMethod("validateData", 2);
	}

	@Test
	public void testLinkedRecordIdDoesNotExist() {
		fillCollectLinksReturnValue();

		DataGroup dataGroup = RecordLinkTestsDataCreator
				.createDataDataGroupWithRecordInfoAndLinkOneLevelDown();
		DataGroup validationOrder = createValidationOrderWithMetadataToValidateAndValidateLinks(
				"new", "true");
		DataRecord validationResultRecord = recordValidator.validateRecord("someToken78678567",
				"text", validationOrder, dataGroup);
		DataGroup validationResult = validationResultRecord.getDataGroup();
		assertEquals(validationResult.getFirstAtomicValueWithNameInData("valid"), "false");

		DataGroup errorMessages = validationResult.getFirstGroupWithNameInData("errorMessages");
		assertEquals(errorMessages.getChildren().size(), 1);
		DataAtomic error = (DataAtomic) errorMessages.getChildren().get(0);
		assertEquals(error.getValue(),
				"Data is not valid: linkedRecord does not exists in storage for recordType: toRecordType and recordId: toRecordId");
		assertEquals(error.getRepeatId(), "0");
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

		DataRecord validationResultRecord = recordValidator.validateRecord("someToken78678567",
				"validationOrder", validationOrder, dataGroup);
		DataGroup validationResult = validationResultRecord.getDataGroup();
		assertEquals(validationResult.getFirstAtomicValueWithNameInData("valid"), "false");

		DataGroup errorMessages = validationResult.getFirstGroupWithNameInData("errorMessages");

		assertEquals(errorMessages.getChildren().size(), 2);
		DataAtomic error = (DataAtomic) errorMessages.getChildren().get(0);
		assertEquals(error.getValue(),
				"Data is not valid: linkedRecord does not exists in storage for recordType: toRecordType and recordId: toRecordId");
		assertEquals(error.getRepeatId(), "0");

		DataAtomic error2 = (DataAtomic) errorMessages.getChildren().get(1);
		assertEquals(error2.getValue(), "Data invalid for metadataId dataWithLinksNew");
		assertEquals(error2.getRepeatId(), "1");
	}

	int index = -1;

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
		DataRecord validationResultRecord = recordValidator.validateRecord("someToken78678567",
				"text", validationOrder, dataGroup);
		DataGroup validationResult = validationResultRecord.getDataGroup();
		assertEquals(validationResult.getFirstAtomicValueWithNameInData("valid"), "true");

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
			recordValidator.validateRecord("someToken78678567", "validationOrder", validationOrder,
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
			recordValidator.validateRecord("someToken78678567", "validationOrder", validationOrder,
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

		DataGroup dataGroup = new DataGroupOldSpy("nameInData");
		dataGroup.addChild(DataCreator2.createRecordInfoWithRecordTypeAndRecordIdAndDataDivider(
				"spyType", "spyId", "cora"));

		DataGroup validationOrder = createValidationOrderWithMetadataToValidateAndValidateLinks(
				"existing", "true");

		changeRecordTypeTo(validationOrder, "spyType");
		boolean exceptionWasCaught = false;
		try {
			recordValidator.validateRecord("someToken78678567", "validationOrder", validationOrder,
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
		recordTypeGroup.addChild(new DataAtomicSpy("linkedRecordId", recordType));
	}

	@Test
	public void testNonExistingRecordType() {
		DataGroup dataGroup = createDataGroupPlace();

		DataGroup validationOrder = createValidationOrderWithMetadataToValidateAndValidateLinks(
				"existing", "true");
		changeRecordTypeTo(validationOrder, "recordType_NOT_EXISTING");
		DataRecord validationResultRecord = recordValidator.validateRecord("someToken78678567",
				"validationOrder", validationOrder, dataGroup);
		DataGroup validationResult = validationResultRecord.getDataGroup();
		assertEquals(validationResult.getFirstAtomicValueWithNameInData("valid"), "false");

		DataGroup errorMessages = validationResult.getFirstGroupWithNameInData("errorMessages");
		DataAtomic error = (DataAtomic) errorMessages.getChildren().get(0);
		assertEquals(error.getValue(),
				"No records exists with recordType: recordType_NOT_EXISTING and recordId place");

	}
}
