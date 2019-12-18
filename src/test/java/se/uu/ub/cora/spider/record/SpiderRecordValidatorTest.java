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

package se.uu.ub.cora.spider.record;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertTrue;

import java.util.HashMap;
import java.util.HashSet;

import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import se.uu.ub.cora.bookkeeper.linkcollector.DataRecordLinkCollector;
import se.uu.ub.cora.bookkeeper.termcollector.DataGroupTermCollector;
import se.uu.ub.cora.bookkeeper.validator.DataValidator;
import se.uu.ub.cora.data.DataAtomic;
import se.uu.ub.cora.data.DataAtomicFactory;
import se.uu.ub.cora.data.DataAtomicProvider;
import se.uu.ub.cora.data.DataGroup;
import se.uu.ub.cora.data.DataGroupFactory;
import se.uu.ub.cora.data.DataGroupProvider;
import se.uu.ub.cora.data.DataRecord;
import se.uu.ub.cora.data.DataRecordFactory;
import se.uu.ub.cora.data.DataRecordLinkFactory;
import se.uu.ub.cora.data.DataRecordLinkProvider;
import se.uu.ub.cora.data.DataRecordProvider;
import se.uu.ub.cora.logger.LoggerProvider;
import se.uu.ub.cora.search.RecordIndexer;
import se.uu.ub.cora.spider.authentication.Authenticator;
import se.uu.ub.cora.spider.authentication.AuthenticatorSpy;
import se.uu.ub.cora.spider.authorization.AlwaysAuthorisedExceptStub;
import se.uu.ub.cora.spider.authorization.AuthorizationException;
import se.uu.ub.cora.spider.authorization.PermissionRuleCalculator;
import se.uu.ub.cora.spider.authorization.SpiderAuthorizator;
import se.uu.ub.cora.spider.data.DataAtomicFactorySpy;
import se.uu.ub.cora.spider.data.DataAtomicSpy;
import se.uu.ub.cora.spider.data.DataGroupFactorySpy;
import se.uu.ub.cora.spider.data.DataGroupSpy;
import se.uu.ub.cora.spider.data.DataRecordFactorySpy;
import se.uu.ub.cora.spider.dependency.RecordIdGeneratorProviderSpy;
import se.uu.ub.cora.spider.dependency.RecordStorageProviderSpy;
import se.uu.ub.cora.spider.dependency.SpiderDependencyProviderSpy;
import se.uu.ub.cora.spider.extended.ExtendedFunctionalityProviderSpy;
import se.uu.ub.cora.spider.log.LoggerFactorySpy;
import se.uu.ub.cora.spider.spy.AuthorizatorAlwaysAuthorizedSpy;
import se.uu.ub.cora.spider.spy.DataGroupTermCollectorSpy;
import se.uu.ub.cora.spider.spy.DataRecordLinkCollectorSpy;
import se.uu.ub.cora.spider.spy.DataValidatorAlwaysValidSpy;
import se.uu.ub.cora.spider.spy.DataValidatorValidExceptSpy;
import se.uu.ub.cora.spider.spy.IdGeneratorSpy;
import se.uu.ub.cora.spider.spy.NoRulesCalculatorStub;
import se.uu.ub.cora.spider.spy.RecordIndexerSpy;
import se.uu.ub.cora.spider.spy.RecordStorageForValidateDataSpy;
import se.uu.ub.cora.spider.spy.RecordStorageSpy;
import se.uu.ub.cora.spider.spy.RuleCalculatorSpy;
import se.uu.ub.cora.spider.testdata.DataCreator;
import se.uu.ub.cora.spider.testdata.DataCreator2;
import se.uu.ub.cora.spider.testdata.RecordLinkTestsDataCreator;
import se.uu.ub.cora.storage.RecordIdGenerator;
import se.uu.ub.cora.storage.RecordStorage;

public class SpiderRecordValidatorTest {
	private static final String TIMESTAMP_FORMAT = "\\d{4}-\\d{2}-\\d{2}T\\d{2}:\\d{2}:\\d{2}\\.\\d{6}Z";
	private RecordStorage recordStorage;
	private Authenticator authenticator;
	private SpiderAuthorizator spiderAuthorizator;
	private PermissionRuleCalculator ruleCalculator;
	private SpiderRecordValidator recordValidator;
	private DataValidator dataValidator;
	private DataRecordLinkCollector linkCollector;
	private SpiderDependencyProviderSpy dependencyProvider;
	private ExtendedFunctionalityProviderSpy extendedFunctionalityProvider;
	private DataGroupTermCollector termCollector;
	private RecordIndexer recordIndexer;
	private RecordIdGenerator idGenerator;
	private LoggerFactorySpy loggerFactorySpy;
	private DataGroupFactory dataGroupFactorySpy;
	private DataAtomicFactory dataAtomicFactorySpy;
	private DataRecordFactory dataRecordFactorySpy;
	private DataRecordLinkFactory dataRecordLinkFactory;

	@BeforeMethod
	public void beforeMethod() {
		setUpFactoriesAndProviders();
		authenticator = new AuthenticatorSpy();
		spiderAuthorizator = new AuthorizatorAlwaysAuthorizedSpy();
		dataValidator = new DataValidatorAlwaysValidSpy();
		recordStorage = new RecordStorageForValidateDataSpy();
		ruleCalculator = new NoRulesCalculatorStub();
		linkCollector = new DataRecordLinkCollectorSpy();
		termCollector = new DataGroupTermCollectorSpy();
		recordIndexer = new RecordIndexerSpy();
		extendedFunctionalityProvider = new ExtendedFunctionalityProviderSpy();
		idGenerator = new IdGeneratorSpy();
		setUpDependencyProvider();
	}

	private void setUpFactoriesAndProviders() {
		loggerFactorySpy = new LoggerFactorySpy();
		LoggerProvider.setLoggerFactory(loggerFactorySpy);
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
		dependencyProvider = new SpiderDependencyProviderSpy(new HashMap<>());
		dependencyProvider.authenticator = authenticator;
		dependencyProvider.spiderAuthorizator = spiderAuthorizator;
		dependencyProvider.dataValidator = dataValidator;
		RecordStorageProviderSpy recordStorageProviderSpy = new RecordStorageProviderSpy();
		recordStorageProviderSpy.recordStorage = recordStorage;
		dependencyProvider.setRecordStorageProvider(recordStorageProviderSpy);
		RecordIdGeneratorProviderSpy recordIdGeneratorProviderSpy = new RecordIdGeneratorProviderSpy();
		recordIdGeneratorProviderSpy.recordIdGenerator = idGenerator;
		dependencyProvider.setRecordIdGeneratorProvider(recordIdGeneratorProviderSpy);
		dependencyProvider.ruleCalculator = ruleCalculator;
		dependencyProvider.linkCollector = linkCollector;
		dependencyProvider.extendedFunctionalityProvider = extendedFunctionalityProvider;
		dependencyProvider.searchTermCollector = termCollector;
		dependencyProvider.recordIndexer = recordIndexer;
		recordValidator = SpiderRecordValidatorImp.usingDependencyProvider(dependencyProvider);
	}

	@Test
	public void testExternalDependenciesAreCalledForValidateNew() {
		spiderAuthorizator = new AuthorizatorAlwaysAuthorizedSpy();
		recordStorage = new RecordStorageForValidateDataSpy();
		ruleCalculator = new RuleCalculatorSpy();
		setUpDependencyProvider();

		DataGroup recordToValidate = new DataGroupSpy("nameInData");
		recordToValidate.addChild(
				DataCreator2.createRecordInfoWithRecordTypeAndRecordIdAndDataDivider("spyType",
						"spyId", "cora"));

		DataGroup validationOrder = createValidationOrderWithMetadataToValidateAndValidateLinks(
				"new", "true");
		recordValidator.validateRecord("someToken78678567", "validationOrder", validationOrder,
				recordToValidate);

		AuthorizatorAlwaysAuthorizedSpy authorizatorSpy = (AuthorizatorAlwaysAuthorizedSpy) spiderAuthorizator;
		assertTrue(authorizatorSpy.authorizedWasCalled);

		assertTrue(((DataValidatorAlwaysValidSpy) dataValidator).validateDataWasCalled);
		assertEquals(((DataRecordLinkCollectorSpy) linkCollector).metadataId, "textNew");
		assertEquals(((DataRecordLinkCollectorSpy) linkCollector).recordType, "text");
		assertEquals(((DataRecordLinkCollectorSpy) linkCollector).recordId, null);

	}

	private DataGroup createValidationOrderWithMetadataToValidateAndValidateLinks(
			String metadataToValidate, String validateLinks) {
		DataGroup validationOrder = new DataGroupSpy("validationOrder");
		validationOrder.addChild(new DataAtomicSpy("metadataToValidate", metadataToValidate));
		validationOrder.addChild(new DataAtomicSpy("validateLinks", validateLinks));

		DataGroup recordTypeGroup = new DataGroupSpy("recordType");
		recordTypeGroup.addChild(new DataAtomicSpy("linkedRecordType", "recordType"));
		recordTypeGroup.addChild(new DataAtomicSpy("linkedRecordId", "text"));
		validationOrder.addChild(recordTypeGroup);
		return validationOrder;
	}

	@Test
	public void testExternalDependenciesAreCalledForValidateExisting() {
		spiderAuthorizator = new AuthorizatorAlwaysAuthorizedSpy();
		recordStorage = new RecordStorageForValidateDataSpy();
		ruleCalculator = new RuleCalculatorSpy();
		setUpDependencyProvider();

		DataGroup dataGroup = new DataGroupSpy("nameInData");
		dataGroup.addChild(DataCreator2.createRecordInfoWithRecordTypeAndRecordIdAndDataDivider(
				"spyType", "spyId", "cora"));
		DataGroup validationOrder = createValidationOrderWithMetadataToValidateAndValidateLinks(
				"existing", "true");
		recordValidator.validateRecord("someToken78678567", "validationOrder", validationOrder,
				dataGroup);

		AuthorizatorAlwaysAuthorizedSpy authorizatorSpy = (AuthorizatorAlwaysAuthorizedSpy) spiderAuthorizator;
		assertTrue(authorizatorSpy.authorizedWasCalled);

		assertTrue(((DataValidatorAlwaysValidSpy) dataValidator).validateDataWasCalled);
		assertEquals(((DataRecordLinkCollectorSpy) linkCollector).metadataId, "text");
		assertEquals(((DataRecordLinkCollectorSpy) linkCollector).recordType, "text");
		assertEquals(((DataRecordLinkCollectorSpy) linkCollector).recordId, "spyId");

	}

	@Test
	public void testLinkCollectorIsNotCalledWhenValidateLinksIsFalse() {
		spiderAuthorizator = new AuthorizatorAlwaysAuthorizedSpy();
		recordStorage = new RecordStorageForValidateDataSpy();
		ruleCalculator = new RuleCalculatorSpy();
		setUpDependencyProvider();

		DataGroup dataGroup = new DataGroupSpy("nameInData");
		dataGroup.addChild(DataCreator2.createRecordInfoWithRecordTypeAndRecordIdAndDataDivider(
				"spyType", "spyId", "cora"));
		DataGroup validationOrder = createValidationOrderWithMetadataToValidateAndValidateLinks(
				"existing", "false");
		recordValidator.validateRecord("someToken78678567", "validationOrder", validationOrder,
				dataGroup);

		assertFalse(((DataRecordLinkCollectorSpy) linkCollector).collectLinksWasCalled);
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

		DataValidatorAlwaysValidSpy dataValidatorSpy = (DataValidatorAlwaysValidSpy) dataValidator;
		assertTrue(dataValidatorSpy.validateDataWasCalled);
		assertEquals(dataValidatorSpy.metadataId, "textNew");
	}

	private DataGroup createDataGroupPlace() {
		DataGroup dataGroup = new DataGroupSpy("typeWithUserGeneratedId");
		createAndAddRecordInfo(dataGroup);
		dataGroup.addChild(new DataAtomicSpy("atomicId", "atomicValue"));
		return dataGroup;
	}

	private void createAndAddRecordInfo(DataGroup dataGroup) {
		DataGroup createRecordInfo = new DataGroupSpy("recordInfo");
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

		DataValidatorAlwaysValidSpy dataValidatorSpy = (DataValidatorAlwaysValidSpy) dataValidator;
		assertTrue(dataValidatorSpy.validateDataWasCalled);
		assertEquals(dataValidatorSpy.metadataId, "text");
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

		DataGroup recordTypeGroup = recordInfo.getFirstGroupWithNameInData("type");
		assertEquals(recordTypeGroup.getFirstAtomicValueWithNameInData("linkedRecordType"),
				"recordType");
		assertEquals(recordTypeGroup.getFirstAtomicValueWithNameInData("linkedRecordId"),
				"validationOrder");

		String tsCreated = recordInfo.getFirstAtomicValueWithNameInData("tsCreated");
		assertTrue(tsCreated.matches(TIMESTAMP_FORMAT));

		DataGroup updated = recordInfo.getFirstGroupWithNameInData("updated");
		String tsUpdated = updated.getFirstAtomicValueWithNameInData("tsUpdated");
		assertTrue(tsUpdated.matches(TIMESTAMP_FORMAT));
		assertFalse(recordInfo.containsChildWithNameInData("tsUpdated"));
		assertEquals(updated.getRepeatId(), "0");
	}

	@Test
	public void testValidateRecordInvalidData() {
		recordStorage = new RecordStorageForValidateDataSpy();
		dataValidator = new DataValidatorValidExceptSpy();
		((DataValidatorValidExceptSpy) dataValidator).notValidForMetadataId.add("text");
		setUpDependencyProvider();

		DataGroup dataGroup = createDataGroupPlace();
		DataGroup validationOrder = createValidationOrderWithMetadataToValidateAndValidateLinks(
				"existing", "true");
		DataRecord validationResultRecord = recordValidator.validateRecord("someToken78678567",
				"validationOrder", validationOrder, dataGroup);
		DataGroup validationResult = validationResultRecord.getDataGroup();
		assertEquals(validationResult.getFirstAtomicValueWithNameInData("valid"), "false");

		assertEquals(((DataValidatorValidExceptSpy) dataValidator).numOfCallsToValidate, 2);
	}

	@Test
	public void testLinkedRecordIdDoesNotExist() {
		recordStorage = new RecordLinkTestsRecordStorage();
		linkCollector = DataCreator.getDataRecordLinkCollectorSpyWithCollectedLinkAdded();
		setUpDependencyProvider();

		((RecordLinkTestsRecordStorage) recordStorage).recordIdExistsForRecordType = false;

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
		dataValidator = new DataValidatorValidExceptSpy();
		((DataValidatorValidExceptSpy) dataValidator).notValidForMetadataId.add("dataWithLinksNew");
		linkCollector = DataCreator.getDataRecordLinkCollectorSpyWithCollectedLinkAdded();
		setUpDependencyProvider();

		((RecordLinkTestsRecordStorage) recordStorage).recordIdExistsForRecordType = false;

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

	@Test
	public void testLinkedRecordIdDoesNotExistDoesNotMatterWhenLinksAreNotChecked() {
		recordStorage = new RecordLinkTestsRecordStorage();
		linkCollector = DataCreator.getDataRecordLinkCollectorSpyWithCollectedLinkAdded();
		setUpDependencyProvider();

		((RecordLinkTestsRecordStorage) recordStorage).recordIdExistsForRecordType = false;

		DataGroup dataGroup = RecordLinkTestsDataCreator
				.createDataDataGroupWithRecordInfoAndLinkOneLevelDown();
		DataGroup validationOrder = createValidationOrderWithMetadataToValidateAndValidateLinks(
				"new", "false");
		DataRecord validationResultRecord = recordValidator.validateRecord("someToken78678567",
				"text", validationOrder, dataGroup);
		DataGroup validationResult = validationResultRecord.getDataGroup();
		assertEquals(validationResult.getFirstAtomicValueWithNameInData("valid"), "true");

	}

	@Test
	public void testUnauthorizedForCreateOnValidationorderRecordTypeShouldNotValidate() {
		recordStorage = new RecordStorageSpy();
		spiderAuthorizator = new AlwaysAuthorisedExceptStub();
		HashSet<String> hashSet = new HashSet<String>();
		hashSet.add("create");
		((AlwaysAuthorisedExceptStub) spiderAuthorizator).notAuthorizedForRecordTypeAndActions
				.put("validationOrder", hashSet);
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
			assertEquals(e.getMessage(), "not authorized for create on recordType validationOrder");
			exceptionWasCaught = true;
		}
		assertTrue(exceptionWasCaught);
		DataValidatorAlwaysValidSpy dataValidatorSpy = (DataValidatorAlwaysValidSpy) dataValidator;
		assertFalse(dataValidatorSpy.validateDataWasCalled);
	}

	@Test
	public void testInvalidDataForCreateOnValidationOrderShouldThrowException() {
		dataValidator = new DataValidatorValidExceptSpy();
		recordStorage = new RecordStorageForValidateDataSpy();
		((DataValidatorValidExceptSpy) dataValidator).notValidForMetadataId
				.add("validationOrderNew");
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
					"Data is not valid: [Data invalid for metadataId validationOrderNew]");
			exceptionWasCaught = true;
		}
		assertTrue(exceptionWasCaught);
	}

	@Test
	public void testUnauthorizedForValidateOnRecordTypeShouldNotValidateData() {
		recordStorage = new RecordStorageForValidateDataSpy();
		spiderAuthorizator = new AlwaysAuthorisedExceptStub();
		HashSet<String> hashSet = new HashSet<String>();
		hashSet.add("validate");
		((AlwaysAuthorisedExceptStub) spiderAuthorizator).notAuthorizedForRecordTypeAndActions
				.put("spyType", hashSet);
		setUpDependencyProvider();

		DataGroup dataGroup = new DataGroupSpy("nameInData");
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
		DataValidatorAlwaysValidSpy dataValidatorSpy = (DataValidatorAlwaysValidSpy) dataValidator;
		assertEquals(dataValidatorSpy.numOfCallsToValidate, 1);
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
