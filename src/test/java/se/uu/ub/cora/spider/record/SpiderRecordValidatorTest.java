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
import se.uu.ub.cora.spider.authentication.Authenticator;
import se.uu.ub.cora.spider.authentication.AuthenticatorSpy;
import se.uu.ub.cora.spider.authorization.AlwaysAuthorisedExceptStub;
import se.uu.ub.cora.spider.authorization.AuthorizationException;
import se.uu.ub.cora.spider.authorization.PermissionRuleCalculator;
import se.uu.ub.cora.spider.authorization.SpiderAuthorizator;
import se.uu.ub.cora.spider.data.SpiderDataAtomic;
import se.uu.ub.cora.spider.data.SpiderDataGroup;
import se.uu.ub.cora.spider.data.SpiderDataRecord;
import se.uu.ub.cora.spider.dependency.SpiderDependencyProviderSpy;
import se.uu.ub.cora.spider.extended.ExtendedFunctionalityProviderSpy;
import se.uu.ub.cora.spider.record.storage.RecordStorage;
import se.uu.ub.cora.spider.search.RecordIndexer;
import se.uu.ub.cora.spider.spy.AuthorizatorAlwaysAuthorizedSpy;
import se.uu.ub.cora.spider.spy.DataGroupTermCollectorSpy;
import se.uu.ub.cora.spider.spy.DataRecordLinkCollectorSpy;
import se.uu.ub.cora.spider.spy.DataValidatorAlwaysValidSpy;
import se.uu.ub.cora.spider.spy.DataValidatorValidExceptSpy;
import se.uu.ub.cora.spider.spy.NoRulesCalculatorStub;
import se.uu.ub.cora.spider.spy.RecordIndexerSpy;
import se.uu.ub.cora.spider.spy.RecordStorageForValidateDataSpy;
import se.uu.ub.cora.spider.spy.RecordStorageSpy;
import se.uu.ub.cora.spider.spy.RuleCalculatorSpy;
import se.uu.ub.cora.spider.testdata.DataCreator;
import se.uu.ub.cora.spider.testdata.RecordLinkTestsDataCreator;
import se.uu.ub.cora.spider.testdata.SpiderDataCreator;

public class SpiderRecordValidatorTest {
	private RecordStorage recordStorage;
	private Authenticator authenticator;
	private SpiderAuthorizator spiderAuthorizator;
	private PermissionRuleCalculator ruleCalculator;
	private SpiderRecordValidatorImp recordValidator;
	private DataValidator dataValidator;
	private DataRecordLinkCollector linkCollector;
	private SpiderDependencyProviderSpy dependencyProvider;
	private ExtendedFunctionalityProviderSpy extendedFunctionalityProvider;
	private DataGroupTermCollector termCollector;
	private RecordIndexer recordIndexer;

	@BeforeMethod
	public void beforeMethod() {
		authenticator = new AuthenticatorSpy();
		spiderAuthorizator = new AuthorizatorAlwaysAuthorizedSpy();
		dataValidator = new DataValidatorAlwaysValidSpy();
		recordStorage = new RecordStorageForValidateDataSpy();
		ruleCalculator = new NoRulesCalculatorStub();
		linkCollector = new DataRecordLinkCollectorSpy();
		termCollector = new DataGroupTermCollectorSpy();
		recordIndexer = new RecordIndexerSpy();
		extendedFunctionalityProvider = new ExtendedFunctionalityProviderSpy();
		setUpDependencyProvider();
	}

	private void setUpDependencyProvider() {
		dependencyProvider = new SpiderDependencyProviderSpy(new HashMap<>());
		dependencyProvider.authenticator = authenticator;
		dependencyProvider.spiderAuthorizator = spiderAuthorizator;
		dependencyProvider.dataValidator = dataValidator;
		dependencyProvider.recordStorage = recordStorage;
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

		SpiderDataGroup recordToValidate = SpiderDataGroup.withNameInData("nameInData");
		recordToValidate.addChild(
				SpiderDataCreator.createRecordInfoWithRecordTypeAndRecordIdAndDataDivider("spyType",
						"spyId", "cora"));

		SpiderDataGroup validationOrder = createValidationOrderWithMetadataToValidateAndValidateLinks(
				"new", "true");
		recordValidator.validateRecord("someToken78678567", "workOrder", validationOrder,
				recordToValidate);

		AuthorizatorAlwaysAuthorizedSpy authorizatorSpy = (AuthorizatorAlwaysAuthorizedSpy) spiderAuthorizator;
		assertTrue(authorizatorSpy.authorizedWasCalled);

		assertTrue(((DataValidatorAlwaysValidSpy) dataValidator).validateDataWasCalled);
		assertEquals(((DataRecordLinkCollectorSpy) linkCollector).metadataId, "textNew");
		assertEquals(((DataRecordLinkCollectorSpy) linkCollector).recordType, "text");
		assertEquals(((DataRecordLinkCollectorSpy) linkCollector).recordId, null);

	}

	private SpiderDataGroup createValidationOrderWithMetadataToValidateAndValidateLinks(
			String metadataToValidate, String validateLinks) {
		SpiderDataGroup validationOrder = SpiderDataGroup.withNameInData("validationOrder");
		validationOrder.addChild(
				SpiderDataAtomic.withNameInDataAndValue("metadataToValidate", metadataToValidate));
		validationOrder
				.addChild(SpiderDataAtomic.withNameInDataAndValue("validateLinks", validateLinks));

		SpiderDataGroup recordTypeGroup = SpiderDataGroup.withNameInData("recordType");
		recordTypeGroup.addChild(
				SpiderDataAtomic.withNameInDataAndValue("linkedRecordType", "recordType"));
		recordTypeGroup.addChild(SpiderDataAtomic.withNameInDataAndValue("linkedRecordId", "text"));
		validationOrder.addChild(recordTypeGroup);
		return validationOrder;
	}

	@Test
	public void testExternalDependenciesAreCalledForValidateExisting() {
		spiderAuthorizator = new AuthorizatorAlwaysAuthorizedSpy();
		recordStorage = new RecordStorageForValidateDataSpy();
		ruleCalculator = new RuleCalculatorSpy();
		setUpDependencyProvider();

		SpiderDataGroup spiderDataGroup = SpiderDataGroup.withNameInData("nameInData");
		spiderDataGroup.addChild(
				SpiderDataCreator.createRecordInfoWithRecordTypeAndRecordIdAndDataDivider("spyType",
						"spyId", "cora"));
		SpiderDataGroup validationOrder = createValidationOrderWithMetadataToValidateAndValidateLinks(
				"existing", "true");
		recordValidator.validateRecord("someToken78678567", "text", validationOrder,
				spiderDataGroup);

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

		SpiderDataGroup spiderDataGroup = SpiderDataGroup.withNameInData("nameInData");
		spiderDataGroup.addChild(
				SpiderDataCreator.createRecordInfoWithRecordTypeAndRecordIdAndDataDivider("spyType",
						"spyId", "cora"));
		SpiderDataGroup validationOrder = createValidationOrderWithMetadataToValidateAndValidateLinks(
				"existing", "false");
		recordValidator.validateRecord("someToken78678567", "text", validationOrder,
				spiderDataGroup);

		assertFalse(((DataRecordLinkCollectorSpy) linkCollector).collectLinksWasCalled);
	}

	@Test
	public void testValidatRecordDataValidDataForCreateUsesNewMetadataId() {
		recordStorage = new RecordStorageForValidateDataSpy();
		setUpDependencyProvider();

		SpiderDataGroup dataGroup = createDataGroupPlace();
		SpiderDataGroup validationOrder = createValidationOrderWithMetadataToValidateAndValidateLinks(
				"new", "true");
		SpiderDataRecord validationResultRecord = recordValidator
				.validateRecord("someToken78678567", "text", validationOrder, dataGroup);
		SpiderDataGroup validationResult = validationResultRecord.getSpiderDataGroup();
		assertEquals(validationResult.extractAtomicValue("valid"), "true");

		DataValidatorAlwaysValidSpy dataValidatorSpy = (DataValidatorAlwaysValidSpy) dataValidator;
		assertTrue(dataValidatorSpy.validateDataWasCalled);
		assertEquals(dataValidatorSpy.metadataId, "textNew");
	}

	private SpiderDataGroup createDataGroupPlace() {
		SpiderDataGroup dataGroup = SpiderDataGroup.withNameInData("typeWithUserGeneratedId");
		SpiderDataGroup createRecordInfo = SpiderDataGroup.withNameInData("recordInfo");
		createRecordInfo.addChild(SpiderDataAtomic.withNameInDataAndValue("id", "place"));
		createRecordInfo.addChild(SpiderDataAtomic.withNameInDataAndValue("type", "recordType"));
		dataGroup.addChild(createRecordInfo);
		dataGroup.addChild(SpiderDataAtomic.withNameInDataAndValue("atomicId", "atomicValue"));
		return dataGroup;
	}

	@Test
	public void testValidatRecordDataValidDataForUpdate() {
		recordStorage = new RecordStorageForValidateDataSpy();
		setUpDependencyProvider();

		SpiderDataGroup dataGroup = createDataGroupPlace();
		SpiderDataGroup validationOrder = createValidationOrderWithMetadataToValidateAndValidateLinks(
				"existing", "true");
		SpiderDataRecord validationResultRecord = recordValidator
				.validateRecord("someToken78678567", "text", validationOrder, dataGroup);
		SpiderDataGroup validationResult = validationResultRecord.getSpiderDataGroup();
		assertEquals(validationResult.extractAtomicValue("valid"), "true");

		DataValidatorAlwaysValidSpy dataValidatorSpy = (DataValidatorAlwaysValidSpy) dataValidator;
		assertTrue(dataValidatorSpy.validateDataWasCalled);
		assertEquals(dataValidatorSpy.metadataId, "text");
	}

	@Test
	public void testValidateRecordInvalidData() {
		recordStorage = new RecordStorageForValidateDataSpy();
		dataValidator = new DataValidatorValidExceptSpy();
		((DataValidatorValidExceptSpy) dataValidator).notValidForMetadataId.add("text");
		setUpDependencyProvider();

		SpiderDataGroup dataGroup = createDataGroupPlace();
		SpiderDataGroup validationOrder = createValidationOrderWithMetadataToValidateAndValidateLinks(
				"existing", "true");
		SpiderDataRecord validationResultRecord = recordValidator
				.validateRecord("someToken78678567", "text", validationOrder, dataGroup);
		SpiderDataGroup validationResult = validationResultRecord.getSpiderDataGroup();
		assertEquals(validationResult.extractAtomicValue("valid"), "false");

		assertEquals(((DataValidatorValidExceptSpy) dataValidator).numOfCallsToValidate, 2);
	}

	@Test
	public void testLinkedRecordIdDoesNotExist() {
		recordStorage = new RecordLinkTestsRecordStorage();
		linkCollector = DataCreator.getDataRecordLinkCollectorSpyWithCollectedLinkAdded();
		setUpDependencyProvider();

		((RecordLinkTestsRecordStorage) recordStorage).recordIdExistsForRecordType = false;

		SpiderDataGroup dataGroup = RecordLinkTestsDataCreator
				.createDataGroupWithRecordInfoAndLinkOneLevelDown();
		SpiderDataGroup validationOrder = createValidationOrderWithMetadataToValidateAndValidateLinks(
				"new", "true");
		SpiderDataRecord validationResultRecord = recordValidator
				.validateRecord("someToken78678567", "text", validationOrder, dataGroup);
		SpiderDataGroup validationResult = validationResultRecord.getSpiderDataGroup();
		assertEquals(validationResult.extractAtomicValue("valid"), "false");

		SpiderDataGroup errorMessages = validationResult.extractGroup("errorMessages");
		assertEquals(errorMessages.getChildren().size(), 1);
		SpiderDataAtomic error = (SpiderDataAtomic) errorMessages.getChildren().get(0);
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

		SpiderDataGroup dataGroup = RecordLinkTestsDataCreator
				.createDataGroupWithRecordInfoAndLinkOneLevelDown();
		SpiderDataGroup validationOrder = createValidationOrderWithMetadataToValidateAndValidateLinks(
				"new", "true");

		SpiderDataRecord validationResultRecord = recordValidator
				.validateRecord("someToken78678567", "workOrder", validationOrder, dataGroup);
		SpiderDataGroup validationResult = validationResultRecord.getSpiderDataGroup();
		assertEquals(validationResult.extractAtomicValue("valid"), "false");

		SpiderDataGroup errorMessages = validationResult.extractGroup("errorMessages");

		assertEquals(errorMessages.getChildren().size(), 2);
		SpiderDataAtomic error = (SpiderDataAtomic) errorMessages.getChildren().get(0);
		assertEquals(error.getValue(),
				"Data is not valid: linkedRecord does not exists in storage for recordType: toRecordType and recordId: toRecordId");
		assertEquals(error.getRepeatId(), "0");

		SpiderDataAtomic error2 = (SpiderDataAtomic) errorMessages.getChildren().get(1);
		assertEquals(error2.getValue(), "Data invalid for metadataId dataWithLinksNew");
		assertEquals(error2.getRepeatId(), "1");
	}

	@Test
	public void testLinkedRecordIdDoesNotExistDoesNotMatterWhenLinksAreNotChecked() {
		recordStorage = new RecordLinkTestsRecordStorage();
		linkCollector = DataCreator.getDataRecordLinkCollectorSpyWithCollectedLinkAdded();
		setUpDependencyProvider();

		((RecordLinkTestsRecordStorage) recordStorage).recordIdExistsForRecordType = false;

		SpiderDataGroup dataGroup = RecordLinkTestsDataCreator
				.createDataGroupWithRecordInfoAndLinkOneLevelDown();
		SpiderDataGroup validationOrder = createValidationOrderWithMetadataToValidateAndValidateLinks(
				"new", "false");
		SpiderDataRecord validationResultRecord = recordValidator
				.validateRecord("someToken78678567", "text", validationOrder, dataGroup);
		SpiderDataGroup validationResult = validationResultRecord.getSpiderDataGroup();
		assertEquals(validationResult.extractAtomicValue("valid"), "true");

	}

	@Test
	public void testUnauthorizedForCreateOnWorkorderRecordTypeShouldNotValidate() {
		recordStorage = new RecordStorageSpy();
		spiderAuthorizator = new AlwaysAuthorisedExceptStub();
		HashSet<String> hashSet = new HashSet<String>();
		hashSet.add("create");
		((AlwaysAuthorisedExceptStub) spiderAuthorizator).notAuthorizedForRecordTypeAndActions
				.put("workOrder", hashSet);
		setUpDependencyProvider();

		SpiderDataGroup dataGroup = createDataGroupPlace();
		SpiderDataGroup validationOrder = createValidationOrderWithMetadataToValidateAndValidateLinks(
				"existing", "true");
		changeRecordTypeTo(validationOrder, "spyType");
		boolean exceptionWasCaught = false;
		try {
			recordValidator.validateRecord("someToken78678567", "workOrder", validationOrder,
					dataGroup);
		} catch (Exception e) {
			assertEquals(e.getClass(), AuthorizationException.class);
			assertEquals(e.getMessage(), "not authorized for create on recordType workOrder");
			exceptionWasCaught = true;
		}
		assertTrue(exceptionWasCaught);
		DataValidatorAlwaysValidSpy dataValidatorSpy = (DataValidatorAlwaysValidSpy) dataValidator;
		assertFalse(dataValidatorSpy.validateDataWasCalled);
	}

	@Test
	public void testInvalidDataForCreateOnWorkorderShouldThrowException() {
		dataValidator = new DataValidatorValidExceptSpy();
		recordStorage = new RecordStorageForValidateDataSpy();
		((DataValidatorValidExceptSpy) dataValidator).notValidForMetadataId.add("workOrderNew");
		setUpDependencyProvider();

		SpiderDataGroup spiderDataGroup = createDataGroupPlace();
		SpiderDataGroup validationOrder = createValidationOrderWithMetadataToValidateAndValidateLinks(
				"existing", "true");
		boolean exceptionWasCaught = false;
		try {
			recordValidator.validateRecord("someToken78678567", "workOrder", validationOrder,
					spiderDataGroup);
		} catch (Exception e) {
			assertEquals(e.getClass(), DataException.class);
			assertEquals(e.getMessage(),
					"Data is not valid: [Data invalid for metadataId workOrderNew]");
			exceptionWasCaught = true;
		}
		assertTrue(exceptionWasCaught);
	}

	@Test
	public void testUnauthorizedForValidateOnRecordTypeShouldNotValidateData() {
		recordStorage = new RecordStorageSpy();
		spiderAuthorizator = new AlwaysAuthorisedExceptStub();
		HashSet<String> hashSet = new HashSet<String>();
		hashSet.add("validate");
		((AlwaysAuthorisedExceptStub) spiderAuthorizator).notAuthorizedForRecordTypeAndActions
				.put("spyType", hashSet);
		setUpDependencyProvider();

		SpiderDataGroup spiderDataGroup = SpiderDataGroup.withNameInData("nameInData");
		spiderDataGroup.addChild(
				SpiderDataCreator.createRecordInfoWithRecordTypeAndRecordIdAndDataDivider("spyType",
						"spyId", "cora"));
		SpiderDataGroup validationOrder = createValidationOrderWithMetadataToValidateAndValidateLinks(
				"existing", "true");
		changeRecordTypeTo(validationOrder, "spyType");
		boolean exceptionWasCaught = false;
		try {
			recordValidator.validateRecord("someToken78678567", "spyType", validationOrder,
					spiderDataGroup);
		} catch (Exception e) {
			assertEquals(e.getClass(), AuthorizationException.class);
			exceptionWasCaught = true;
		}
		assertTrue(exceptionWasCaught);
		DataValidatorAlwaysValidSpy dataValidatorSpy = (DataValidatorAlwaysValidSpy) dataValidator;
		assertEquals(dataValidatorSpy.numOfCallsToValidate, 1);
	}

	private void changeRecordTypeTo(SpiderDataGroup validationOrder, String recordType) {
		SpiderDataGroup recordTypeGroup = validationOrder.extractGroup("recordType");
		recordTypeGroup.removeChild("linkedRecordId");
		recordTypeGroup
				.addChild(SpiderDataAtomic.withNameInDataAndValue("linkedRecordId", recordType));
	}

	@Test
	public void testNonExistingRecordType() {
		SpiderDataGroup dataGroup = createDataGroupPlace();
		SpiderDataGroup validationOrder = createValidationOrderWithMetadataToValidateAndValidateLinks(
				"existing", "true");
		changeRecordTypeTo(validationOrder, "recordType_NOT_EXISTING");
		SpiderDataRecord validationResultRecord = recordValidator.validateRecord(
				"someToken78678567", "recordType_NOT_EXISTING", validationOrder, dataGroup);
		SpiderDataGroup validationResult = validationResultRecord.getSpiderDataGroup();
		assertEquals(validationResult.extractAtomicValue("valid"), "false");

		SpiderDataGroup errorMessages = validationResult.extractGroup("errorMessages");
		SpiderDataAtomic error = (SpiderDataAtomic) errorMessages.getChildren().get(0);
		assertEquals(error.getValue(),
				"No records exists with recordType: recordType_NOT_EXISTING and recordId place");

	}
}
