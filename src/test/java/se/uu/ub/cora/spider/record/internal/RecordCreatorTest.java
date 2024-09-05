/*
 * Copyright 2015, 2016, 2017, 2023, 2024 Uppsala University Library
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
import static org.testng.Assert.assertNotNull;
import static org.testng.Assert.assertSame;
import static org.testng.Assert.assertTrue;
import static org.testng.Assert.fail;
import static se.uu.ub.cora.spider.extendedfunctionality.ExtendedFunctionalityPosition.CREATE_AFTER_AUTHORIZATION;
import static se.uu.ub.cora.spider.extendedfunctionality.ExtendedFunctionalityPosition.CREATE_AFTER_METADATA_VALIDATION;
import static se.uu.ub.cora.spider.extendedfunctionality.ExtendedFunctionalityPosition.CREATE_BEFORE_ENHANCE;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Supplier;

import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import se.uu.ub.cora.beefeater.authentication.User;
import se.uu.ub.cora.bookkeeper.recordtype.Unique;
import se.uu.ub.cora.bookkeeper.validator.DataValidationException;
import se.uu.ub.cora.data.DataChild;
import se.uu.ub.cora.data.DataGroup;
import se.uu.ub.cora.data.DataProvider;
import se.uu.ub.cora.data.DataRecord;
import se.uu.ub.cora.data.collected.CollectTerms;
import se.uu.ub.cora.data.collected.Link;
import se.uu.ub.cora.data.collected.StorageTerm;
import se.uu.ub.cora.data.copier.DataCopierFactory;
import se.uu.ub.cora.data.copier.DataCopierProvider;
import se.uu.ub.cora.data.spies.DataAtomicSpy;
import se.uu.ub.cora.data.spies.DataFactorySpy;
import se.uu.ub.cora.data.spies.DataGroupSpy;
import se.uu.ub.cora.data.spies.DataRecordLinkSpy;
import se.uu.ub.cora.logger.LoggerProvider;
import se.uu.ub.cora.spider.authentication.AuthenticationException;
import se.uu.ub.cora.spider.authentication.OldAuthenticatorSpy;
import se.uu.ub.cora.spider.authorization.AuthorizationException;
import se.uu.ub.cora.spider.authorization.PermissionRuleCalculator;
import se.uu.ub.cora.spider.data.DataAtomicOldSpy;
import se.uu.ub.cora.spider.data.DataGroupOldSpy;
import se.uu.ub.cora.spider.dependency.spy.RecordTypeHandlerSpy;
import se.uu.ub.cora.spider.dependency.spy.SpiderDependencyProviderOldSpy;
import se.uu.ub.cora.spider.extendedfunctionality.ExtendedFunctionalityData;
import se.uu.ub.cora.spider.extendedfunctionality.internal.ExtendedFunctionalityProviderSpy;
import se.uu.ub.cora.spider.log.LoggerFactorySpy;
import se.uu.ub.cora.spider.record.ConflictException;
import se.uu.ub.cora.spider.record.DataCopierFactorySpy;
import se.uu.ub.cora.spider.record.DataException;
import se.uu.ub.cora.spider.record.DataGroupToRecordEnhancerSpy;
import se.uu.ub.cora.spider.record.DataRedactorSpy;
import se.uu.ub.cora.spider.record.RecordCreator;
import se.uu.ub.cora.spider.spy.DataGroupTermCollectorSpy;
import se.uu.ub.cora.spider.spy.DataRecordLinkCollectorSpy;
import se.uu.ub.cora.spider.spy.DataValidatorOldSpy;
import se.uu.ub.cora.spider.spy.DataValidatorSpy;
import se.uu.ub.cora.spider.spy.IdGeneratorSpy;
import se.uu.ub.cora.spider.spy.OldSpiderAuthorizatorSpy;
import se.uu.ub.cora.spider.spy.RecordArchiveSpy;
import se.uu.ub.cora.spider.spy.RecordIndexerSpy;
import se.uu.ub.cora.spider.spy.RuleCalculatorSpy;
import se.uu.ub.cora.spider.spy.SpiderDependencyProviderSpy;
import se.uu.ub.cora.spider.spy.UniqueValidatorSpy;
import se.uu.ub.cora.spider.spy.ValidationAnswerSpy;
import se.uu.ub.cora.spider.testdata.DataCreator2;
import se.uu.ub.cora.spider.testdata.RecordLinkTestsDataCreator;
import se.uu.ub.cora.storage.RecordConflictException;
import se.uu.ub.cora.storage.idgenerator.RecordIdGenerator;
import se.uu.ub.cora.storage.spies.RecordStorageSpy;

public class RecordCreatorTest {
	private static final String RECORD_TYPE = "someType";
	private static final String AUTH_TOKEN = "someToken";
	private static final String TIMESTAMP_FORMAT = "\\d{4}-\\d{2}-\\d{2}T\\d{2}:\\d{2}:\\d{2}\\.\\d{6}Z";
	private RecordStorageSpy recordStorage;
	private OldAuthenticatorSpy authenticatorOld;
	private OldSpiderAuthorizatorSpy spiderAuthorizator;
	private RecordIdGenerator idGenerator;
	private PermissionRuleCalculator ruleCalculator;
	private RecordCreator recordCreatorOld;
	private RecordCreator recordCreator;
	private DataValidatorOldSpy dataValidatorOld;
	private DataRecordLinkCollectorSpy linkCollector;
	private SpiderDependencyProviderOldSpy dependencyProviderOldSpy;
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
	private RecordArchiveSpy recordArchive;
	private UniqueValidatorSpy uniqueValidator;
	private DataGroup recordWithoutIdOld;
	private DataGroupSpy recordWithId;

	@BeforeMethod
	public void beforeMethod() {
		setUpFactoriesAndProviders();

		setUpToReturn1ForCreatedAtomicIds();
		setUpToReturnFakeDataForCreatedTS();

		authenticatorOld = new OldAuthenticatorSpy();
		spiderAuthorizator = new OldSpiderAuthorizatorSpy();
		dataValidatorOld = new DataValidatorOldSpy();
		recordStorage = new RecordStorageSpy();
		idGenerator = new IdGeneratorSpy();
		ruleCalculator = new RuleCalculatorSpy();
		linkCollector = new DataRecordLinkCollectorSpy();
		extendedFunctionalityProvider = new ExtendedFunctionalityProviderSpy();
		termCollector = new DataGroupTermCollectorSpy();
		recordIndexer = new RecordIndexerSpy();
		recordArchive = new RecordArchiveSpy();
		dataRedactor = new DataRedactorSpy();
		recordTypeHandlerSpy = new RecordTypeHandlerSpy();
		uniqueValidator = new UniqueValidatorSpy();
		dataGroupToRecordEnhancer = new DataGroupToRecordEnhancerSpy();

		setUpDependencyProviderOld();
		setUpDependencyProvider();

		recordTypeHandlerSpy.MRV.setDefaultReturnValuesSupplier("getRecordTypeId",
				() -> RECORD_TYPE);
		recordTypeHandlerSpy.MRV.setDefaultReturnValuesSupplier("shouldAutoGenerateId", () -> true);

		recordWithoutIdOld = createRecordExampleRecordWithoutId();
		recordWithId = createDataGroupForCreate();
	}

	private void setUpFactoriesAndProviders() {
		loggerFactorySpy = new LoggerFactorySpy();
		LoggerProvider.setLoggerFactory(loggerFactorySpy);
		dataFactorySpy = new DataFactorySpy();
		DataProvider.onlyForTestSetDataFactory(dataFactorySpy);
		dataCopierFactory = new DataCopierFactorySpy();
		DataCopierProvider.setDataCopierFactory(dataCopierFactory);
	}

	private void setUpToReturn1ForCreatedAtomicIds() {
		DataAtomicSpy atomicId = new DataAtomicSpy();
		atomicId.MRV.setDefaultReturnValuesSupplier("getNameInData", () -> "id");
		atomicId.MRV.setDefaultReturnValuesSupplier("getValue", () -> "1");
		dataFactorySpy.MRV.setSpecificReturnValuesSupplier("factorAtomicUsingNameInDataAndValue",
				() -> atomicId, "id", "1");
	}

	private void setUpToReturnFakeDataForCreatedTS() {
		DataAtomicSpy atomicTS = new DataAtomicSpy();
		dataFactorySpy.MRV.setDefaultReturnValuesSupplier("factorAtomicUsingNameInDataAndValue",
				() -> atomicTS);
		atomicTS.MRV.setDefaultReturnValuesSupplier("getNameInData", () -> "tsCreated");
		atomicTS.MRV.setDefaultReturnValuesSupplier("getValue", () -> "fakeTimestampForTsCreated");
	}

	private void setUpDependencyProviderOld() {
		dependencyProviderOldSpy = new SpiderDependencyProviderOldSpy();
		dependencyProviderOldSpy.authenticator = authenticatorOld;
		dependencyProviderOldSpy.spiderAuthorizator = spiderAuthorizator;
		dependencyProviderOldSpy.dataValidator = dataValidatorOld;
		dependencyProviderOldSpy.recordStorage = recordStorage;
		dependencyProviderOldSpy.recordIdGenerator = idGenerator;
		dependencyProviderOldSpy.ruleCalculator = ruleCalculator;
		dependencyProviderOldSpy.linkCollector = linkCollector;
		dependencyProviderOldSpy.extendedFunctionalityProvider = extendedFunctionalityProvider;
		dependencyProviderOldSpy.termCollector = termCollector;
		dependencyProviderOldSpy.recordIndexer = recordIndexer;
		dependencyProviderOldSpy.recordArchive = recordArchive;
		dependencyProviderOldSpy.dataRedactor = dataRedactor;
		dependencyProviderOldSpy.MRV.setDefaultReturnValuesSupplier(
				"getRecordTypeHandlerUsingDataRecordGroup", () -> recordTypeHandlerSpy);
		dependencyProviderOldSpy.MRV.setDefaultReturnValuesSupplier("getUniqueValidator",
				() -> uniqueValidator);

		recordCreatorOld = RecordCreatorImp.usingDependencyProviderAndDataGroupToRecordEnhancer(
				dependencyProviderOldSpy, dataGroupToRecordEnhancer);
	}

	private void setUpDependencyProvider() {
		dependencyProviderSpy = new SpiderDependencyProviderSpy();
		dependencyProviderSpy.MRV.setDefaultReturnValuesSupplier("getRecordStorage",
				() -> recordStorage);
		dependencyProviderSpy.MRV.setDefaultReturnValuesSupplier("getUniqueValidator",
				() -> uniqueValidator);
		dependencyProviderSpy.MRV.setDefaultReturnValuesSupplier(
				"getRecordTypeHandlerUsingDataRecordGroup", () -> recordTypeHandlerSpy);
		dependencyProviderSpy.MRV.setDefaultReturnValuesSupplier("getDataGroupTermCollector",
				() -> termCollector);

		recordCreator = RecordCreatorImp.usingDependencyProviderAndDataGroupToRecordEnhancer(
				dependencyProviderSpy, dataGroupToRecordEnhancer);
	}

	private DataGroup createRecordExampleRecordWithoutId() {
		return DataCreator2.createRecordWithNameInDataAndLinkedDataDividerId("someNameInData",
				"someDataDivider");
	}

	private DataGroupSpy createDataGroupForCreate() {
		DataGroupSpy recordSpy = new DataGroupSpy();
		DataGroupSpy recordInfoSpy = new DataGroupSpy();
		recordSpy.MRV.setSpecificReturnValuesSupplier("getFirstGroupWithNameInData",
				(Supplier<DataGroupSpy>) () -> recordInfoSpy, "recordInfo");
		DataRecordLinkSpy dataDividerSpy = new DataRecordLinkSpy();
		recordInfoSpy.MRV.setReturnValues("getFirstChildWithNameInData", List.of(dataDividerSpy),
				"dataDivider");
		recordInfoSpy.MRV.setSpecificReturnValuesSupplier("getFirstAtomicValueWithNameInData",
				(Supplier<String>) () -> "someRecordId", "id");
		dataDividerSpy.MRV.setReturnValues("getFirstAtomicValueWithNameInData", List.of("uu"),
				"linkedRecordId");
		return recordSpy;
	}

	@Test
	public void testOnlyForTestGetDataGroupToRecordEnhancer() throws Exception {
		RecordCreatorImp creatorImp = (RecordCreatorImp) recordCreator;
		assertSame(creatorImp.onlyForTestGetDataGroupToRecordEnhancer(), dataGroupToRecordEnhancer);
	}

	@Test
	public void testRecordTypeHandlerFetchedFromDependencyProvider() {
		recordCreator.createAndStoreRecord(AUTH_TOKEN, RECORD_TYPE, recordWithoutIdOld);

		dataFactorySpy.MCR.assertParameters("factorRecordGroupFromDataGroup", 0,
				recordWithoutIdOld);
		var dataGroupAsRecordGroup = dataFactorySpy.MCR
				.getReturnValue("factorRecordGroupFromDataGroup", 0);
		dependencyProviderSpy.MCR.assertParameters("getRecordTypeHandlerUsingDataRecordGroup", 0,
				dataGroupAsRecordGroup);
	}

	@Test(expectedExceptions = DataException.class, expectedExceptionsMessageRegExp = "The record "
			+ "cannot be created because the record type provided does not match the record type "
			+ "that the validation type is set to validate.")
	public void testRecordTypePassedNOTEqualsTheLinkedInValidationType() throws Exception {
		recordTypeHandlerSpy.MRV.setDefaultReturnValuesSupplier("getRecordTypeId",
				() -> "NOTRecordTypeId");

		dependencyProviderSpy.MRV.setDefaultReturnValuesSupplier(
				"getRecordTypeHandlerUsingDataRecordGroup", () -> recordTypeHandlerSpy);

		recordCreator.createAndStoreRecord(AUTH_TOKEN, RECORD_TYPE, recordWithoutIdOld);
	}

	@Test(expectedExceptions = DataException.class, expectedExceptionsMessageRegExp = ""
			+ "Data is not valid: some message")
	public void testValidationTypeDoesNotExist() throws Exception {
		recordTypeHandlerSpy.MRV.setAlwaysThrowException("getCreateDefinitionId",
				DataValidationException.withMessage("some message"));

		dependencyProviderSpy.MRV.setDefaultReturnValuesSupplier(
				"getRecordTypeHandlerUsingDataRecordGroup", () -> recordTypeHandlerSpy);

		recordCreator.createAndStoreRecord(AUTH_TOKEN, RECORD_TYPE, recordWithoutIdOld);
	}

	@Test
	public void testAuthenticatorIsCalled() throws Exception {
		recordTypeHandlerSpy.MRV.setDefaultReturnValuesSupplier("getRecordTypeId",
				() -> RECORD_TYPE);

		recordCreatorOld.createAndStoreRecord(AUTH_TOKEN, RECORD_TYPE, recordWithoutIdOld);

		authenticatorOld.MCR.assertParameters("getUserForToken", 0, AUTH_TOKEN);
	}

	@Test
	public void testAuthorizatorIsCalled() throws Exception {
		recordCreatorOld.createAndStoreRecord(AUTH_TOKEN, RECORD_TYPE, recordWithoutIdOld);

		User user = (User) authenticatorOld.MCR.getReturnValue("getUserForToken", 0);
		spiderAuthorizator.MCR.assertParameters("checkUserIsAuthorizedForActionOnRecordType", 0,
				user, "create", RECORD_TYPE);
	}

	@Test
	public void testNotPossibleToCreateRecordWithTypeAndIdWhichAlreadyExists() throws Exception {
		recordStorage.MRV.setSpecificReturnValuesSupplier("recordExists",
				(Supplier<Boolean>) () -> true, List.of(RECORD_TYPE), "1");
		try {
			recordCreatorOld.createAndStoreRecord(AUTH_TOKEN, RECORD_TYPE, recordWithoutIdOld);
			assertTrue(false);
		} catch (Exception e) {
			assertTrue(e instanceof ConflictException);
			assertEquals(e.getMessage(),
					"Record " + "with type: someType and id: 1 already exists in storage");

			recordTypeHandlerSpy.MCR.assertMethodNotCalled("getParentId");

			Map<String, Object> parameters = recordStorage.MCR
					.getParametersForMethodAndCallNumber("recordExists", 0);
			List<?> types = (List<?>) parameters.get("types");
			assertEquals(types.get(0), RECORD_TYPE);
			assertEquals(parameters.get("id"), "1");
		}
	}

	@Test
	public void testExtendedFunctionalityIsCalled() {

		recordCreatorOld.createAndStoreRecord(AUTH_TOKEN, RECORD_TYPE, recordWithoutIdOld);

		ExtendedFunctionalityData expectedData = new ExtendedFunctionalityData();
		expectedData.recordType = RECORD_TYPE;
		expectedData.recordId = null;
		expectedData.authToken = AUTH_TOKEN;
		expectedData.user = (User) authenticatorOld.MCR.getReturnValue("getUserForToken", 0);
		expectedData.previouslyStoredTopDataGroup = null;
		expectedData.dataGroup = recordWithoutIdOld;

		extendedFunctionalityProvider.assertCallToPositionAndFunctionalityCalledWithData(
				CREATE_AFTER_AUTHORIZATION, expectedData, 0);
		extendedFunctionalityProvider.assertCallToPositionAndFunctionalityCalledWithData(
				CREATE_AFTER_METADATA_VALIDATION, expectedData, 1);
		expectedData.recordId = "1";
		extendedFunctionalityProvider.assertCallToPositionAndFunctionalityCalledWithData(
				CREATE_BEFORE_ENHANCE, expectedData, 2);
		extendedFunctionalityProvider.MCR
				.assertNumberOfCallsToMethod("getFunctionalityForPositionAndRecordType", 3);
	}

	@Test
	public void testExtendedFunctionalityAfterAuthorizationCalledBeforeRecordTypeHandlerCreatedSoWeDoNotNeedToHaveARecordInfoForSomeTypes() {
		extendedFunctionalityProvider.setUpExtendedFunctionalityToThrowExceptionOnPosition(
				dependencyProviderSpy, CREATE_AFTER_AUTHORIZATION, RECORD_TYPE);
		recordCreator = RecordCreatorImp.usingDependencyProviderAndDataGroupToRecordEnhancer(
				dependencyProviderSpy, dataGroupToRecordEnhancer);
		try {
			recordCreator.createAndStoreRecord(AUTH_TOKEN, RECORD_TYPE, recordWithoutIdOld);

			fail("Should fail as we want to stop execution when the extended functionality is used,"
					+ " to determin that the extended functionality is called in the correct place"
					+ " in the code");
		} catch (Exception e) {

		}
		SpiderAuthorizatorSpy authorizator = getCorrectAuthorizator();
		authorizator.MCR.assertMethodWasCalled("checkUserIsAuthorizedForActionOnRecordType");
		dataFactorySpy.MCR.assertMethodNotCalled("factorRecordGroupFromDataGroup");
		dependencyProviderSpy.MCR.assertMethodNotCalled("getRecordTypeHandlerUsingDataRecordGroup");
	}

	private SpiderAuthorizatorSpy getCorrectAuthorizator() {
		int callNumberIsOneAs_recordCreatorIsCreatedTwiceOneInSetUpAndOneHereWithNewDependencies = 1;
		SpiderAuthorizatorSpy authorizator = (SpiderAuthorizatorSpy) dependencyProviderSpy.MCR
				.getReturnValue("getSpiderAuthorizator",
						callNumberIsOneAs_recordCreatorIsCreatedTwiceOneInSetUpAndOneHereWithNewDependencies);
		return authorizator;
	}

	@Test
	public void testRecordEnhancerCalled() {

		recordCreatorOld.createAndStoreRecord(AUTH_TOKEN, RECORD_TYPE, recordWithoutIdOld);

		assertCallEnhanceIgnoringReadAccess(recordWithoutIdOld);
	}

	private void assertCallEnhanceIgnoringReadAccess(DataGroup dataGroup) {
		User user = (User) authenticatorOld.MCR.getReturnValue("getUserForToken", 0);
		dataGroupToRecordEnhancer.MCR.assertParameters("enhanceIgnoringReadAccess", 0, user,
				RECORD_TYPE, dataGroup, dataRedactor);
	}

	@Test(expectedExceptions = AuthenticationException.class)
	public void testGetActiveUserFails() {
		authenticatorOld.throwAuthenticationException = true;

		recordCreatorOld.createAndStoreRecord(AUTH_TOKEN, RECORD_TYPE, recordWithoutIdOld);
	}

	@Test
	public void testIndexerIsCalled() {
		recordCreatorOld.createAndStoreRecord(AUTH_TOKEN, RECORD_TYPE, recordWithoutIdOld);

		var dataRecord = recordStorage.MCR
				.getValueForMethodNameAndCallNumberAndParameterName("create", 0, "dataRecord");

		List<?> ids = (List<?>) recordTypeHandlerSpy.MCR.getReturnValue("getCombinedIdForIndex", 0);
		CollectTerms collectTerms = (CollectTerms) termCollector.MCR.getReturnValue("collectTerms",
				1);
		recordIndexer.MCR.assertParameters("indexData", 0, ids, collectTerms.indexTerms,
				dataRecord);

		recordIndexer.MCR.assertParameter("indexData", 0, "indexTerms", collectTerms.indexTerms);
		recordIndexer.MCR.assertParameter("indexData", 0, "record", dataRecord);
	}

	@Test(expectedExceptions = DataException.class, expectedExceptionsMessageRegExp = ""
			+ "Data is not valid: \\[Error1, Error2\\]")
	public void testX() {
		setUpDataValidatorToReturnInvalidWithErrors();
		recordCreator = RecordCreatorImp.usingDependencyProviderAndDataGroupToRecordEnhancer(
				dependencyProviderSpy, dataGroupToRecordEnhancer);

		recordCreator.createAndStoreRecord(AUTH_TOKEN, RECORD_TYPE, recordWithoutIdOld);
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
		DataRecord recordOut = recordCreatorOld.createAndStoreRecord(AUTH_TOKEN, RECORD_TYPE,
				recordWithoutIdOld);

		DataGroup groupOut = recordOut.getDataGroup();
		DataGroup recordInfo = groupOut.getFirstGroupWithNameInData("recordInfo");
		String recordId = recordInfo.getFirstAtomicValueWithNameInData("id");
		assertNotNull(recordId);

		dataFactorySpy.MCR.assertParameters("factorRecordLinkUsingNameInDataAndTypeAndId", 1,
				"createdBy", "user", "12345");
		var createdByLink = dataFactorySpy.MCR
				.getReturnValue("factorRecordLinkUsingNameInDataAndTypeAndId", 1);
		assertDataChildFoundInChildren(createdByLink, recordInfo.getChildren());

		dataFactorySpy.MCR.assertParameters("factorRecordLinkUsingNameInDataAndTypeAndId", 0,
				"type", "recordType", RECORD_TYPE);
		var typeLink = dataFactorySpy.MCR
				.getReturnValue("factorRecordLinkUsingNameInDataAndTypeAndId", 0);
		assertDataChildFoundInChildren(typeLink, recordInfo.getChildren());

		DataGroup groupCreated = (DataGroup) recordStorage.MCR
				.getValueForMethodNameAndCallNumberAndParameterName("create", 0, "dataRecord");
		assertEquals(groupOut.getNameInData(), groupCreated.getNameInData(),
				"Returned and read record should have the same nameInData");
	}

	@Test
	public void testAutogeneratedIdSentToStorageUsingGeneratedId() {
		DataRecord recordOut = recordCreatorOld.createAndStoreRecord(AUTH_TOKEN, RECORD_TYPE,
				recordWithoutIdOld);

		DataGroup groupOut = recordOut.getDataGroup();
		DataGroup recordInfo = groupOut.getFirstGroupWithNameInData("recordInfo");
		String recordId = recordInfo.getFirstAtomicValueWithNameInData("id");
		assertEquals(recordId, "1");

		recordStorage.MCR.assertParameter("create", 0, "type", RECORD_TYPE);
		recordStorage.MCR.assertParameter("create", 0, "id", "1");
	}

	@Test
	public void testCreateRecordAutogeneratedIdSentInIdIsIgnored() {
		recordTypeHandlerSpy.MRV.setDefaultReturnValuesSupplier("getRecordTypeId",
				() -> "typeWithAutoGeneratedIdWrongRecordInfo");
		DataGroup record = DataCreator2.createRecordWithNameInDataAndLinkedDataDividerId(
				"typeWithAutoGeneratedIdWrongRecordInfo", "cora");

		DataGroupOldSpy createdRecordInfo = (DataGroupOldSpy) record
				.getFirstGroupWithNameInData("recordInfo");
		createdRecordInfo.addChild(new DataAtomicOldSpy("id", "someIdThatShouldNotBeHere"));

		DataRecord recordOut = recordCreatorOld.createAndStoreRecord(AUTH_TOKEN,
				"typeWithAutoGeneratedIdWrongRecordInfo", record);

		DataGroup groupOut = recordOut.getDataGroup();
		DataGroup recordInfo = groupOut.getFirstGroupWithNameInData("recordInfo");
		int numOfIds = 0;
		for (DataChild dataElement : recordInfo.getChildren()) {
			if ("id".equals(dataElement.getNameInData())) {
				numOfIds++;
			}
		}
		assertEquals(numOfIds, 1);
		List<String> removedNameInDatas = createdRecordInfo.removedNameInDatas;
		assertEquals(removedNameInDatas.size(), 1);
		assertEquals(removedNameInDatas.get(0), "id");
	}

	@Test
	public void testCorrectRecordInfoInCreatedRecord() {
		recordTypeHandlerSpy.MRV.setDefaultReturnValuesSupplier("shouldAutoGenerateId",
				() -> false);

		DataGroup record = DataCreator2.createRecordWithNameInDataAndIdAndLinkedRecordId(
				"testingRecordInfo", "someId", "cora");

		DataRecord recordOut = recordCreatorOld.createAndStoreRecord(AUTH_TOKEN, RECORD_TYPE,
				record);
		DataGroup groupOut = recordOut.getDataGroup();
		DataGroup recordInfo = groupOut.getFirstGroupWithNameInData("recordInfo");
		String recordId = recordInfo.getFirstAtomicValueWithNameInData("id");
		assertEquals(recordId, "someId");

		assertCorrectUserInfoInRecordInfo(recordInfo);

		dataFactorySpy.MCR.assertNumberOfCallsToMethod("factorAtomicUsingNameInDataAndValue", 2);
		dataFactorySpy.MCR.assertReturn("factorAtomicUsingNameInDataAndValue", 0,
				recordInfo.getFirstChildWithNameInData("tsCreated"));

		String tsCreated = (String) dataFactorySpy.MCR
				.getValueForMethodNameAndCallNumberAndParameterName(
						"factorAtomicUsingNameInDataAndValue", 0, "value");
		assertTrue(tsCreated.matches(TIMESTAMP_FORMAT));

		DataAtomicSpy tsCreatedSpy = (DataAtomicSpy) dataFactorySpy.MCR
				.getReturnValue("factorAtomicUsingNameInDataAndValue", 0);

		DataGroupSpy updated = (DataGroupSpy) dataFactorySpy.MCR
				.getReturnValue("factorGroupUsingNameInData", 0);
		dataFactorySpy.MCR.assertParameters("factorAtomicUsingNameInDataAndValue", 1, "tsUpdated",
				tsCreatedSpy.getValue());

		var createdTsCreated = dataFactorySpy.MCR
				.getReturnValue("factorAtomicUsingNameInDataAndValue", 0);
		updated.MCR.assertParameters("addChild", 1, createdTsCreated);
		assertFalse(recordInfo.containsChildWithNameInData("tsUpdated"));

		dataFactorySpy.MCR.assertParameters("factorRecordLinkUsingNameInDataAndTypeAndId", 0,
				"type", "recordType", RECORD_TYPE);
		var typeLink = dataFactorySpy.MCR
				.getReturnValue("factorRecordLinkUsingNameInDataAndTypeAndId", 0);
		assertDataChildFoundInChildren(typeLink, recordInfo.getChildren());
	}

	private void assertCorrectUserInfoInRecordInfo(DataGroup recordInfo) {
		dataFactorySpy.MCR.assertParameters("factorRecordLinkUsingNameInDataAndTypeAndId", 1,
				"createdBy", "user", "12345");
		var createdByLink = dataFactorySpy.MCR
				.getReturnValue("factorRecordLinkUsingNameInDataAndTypeAndId", 1);
		assertDataChildFoundInChildren(createdByLink, recordInfo.getChildren());

		dataFactorySpy.MCR.assertParameters("factorGroupUsingNameInData", 0, "updated");
		DataGroupSpy updated = (DataGroupSpy) dataFactorySpy.MCR
				.getReturnValue("factorGroupUsingNameInData", 0);
		updated.MCR.assertParameters("setRepeatId", 0, "0");

		dataFactorySpy.MCR.assertParameters("factorRecordLinkUsingNameInDataAndTypeAndId", 2,
				"updatedBy", "user", "12345");
		var updatedByLink = dataFactorySpy.MCR
				.getReturnValue("factorRecordLinkUsingNameInDataAndTypeAndId", 2);
		updated.MCR.assertParameters("addChild", 0, updatedByLink);
	}

	@Test
	public void testCreateRecordUserSuppliedId() {
		recordTypeHandlerSpy.MRV.setDefaultReturnValuesSupplier("getRecordTypeId",
				() -> "typeWithUserGeneratedId");

		DataGroup record = DataCreator2.createRecordWithNameInDataAndIdAndLinkedRecordId(
				"typeWithUserGeneratedId", "somePlace", "cora");

		DataRecord recordOut = recordCreatorOld.createAndStoreRecord(AUTH_TOKEN,
				"typeWithUserGeneratedId", record);
		DataGroup groupOut = recordOut.getDataGroup();
		DataGroup recordInfo = groupOut.getFirstGroupWithNameInData("recordInfo");
		String recordId = recordInfo.getFirstAtomicValueWithNameInData("id");
		assertNotNull(recordId, "A new record should have an id");

		dataFactorySpy.MCR.assertParameters("factorRecordLinkUsingNameInDataAndTypeAndId", 1,
				"createdBy", "user", "12345");
		var createdByLink = dataFactorySpy.MCR
				.getReturnValue("factorRecordLinkUsingNameInDataAndTypeAndId", 1);
		assertDataChildFoundInChildren(createdByLink, recordInfo.getChildren());

		dataFactorySpy.MCR.assertParameters("factorRecordLinkUsingNameInDataAndTypeAndId", 0,
				"type", "recordType", "typeWithUserGeneratedId");
		var typeLink = dataFactorySpy.MCR
				.getReturnValue("factorRecordLinkUsingNameInDataAndTypeAndId", 0);
		assertDataChildFoundInChildren(typeLink, recordInfo.getChildren());

		DataGroup groupCreated = (DataGroup) recordStorage.MCR
				.getValueForMethodNameAndCallNumberAndParameterName("create", 0, "dataRecord");

		assertEquals(groupOut.getNameInData(), groupCreated.getNameInData(),
				"Returned and read record should have the same nameInData");
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
	public void testCreateRecordDataDividerExtractedFromData() {
		recordCreator.createAndStoreRecord(AUTH_TOKEN, RECORD_TYPE, recordWithoutIdOld);

		recordStorage.MCR.assertParameter("create", 0, "dataDivider", "someDataDivider");
	}

	@Test
	public void testCreateRecordCollectedTermsSentAlong() {
		recordTypeHandlerSpy.MRV.setDefaultReturnValuesSupplier("getRecordTypeId",
				() -> "typeWithUserGeneratedId");
		DataGroup record = DataCreator2.createRecordWithNameInDataAndIdAndLinkedRecordId(
				"typeWithUserGeneratedId", "somePlace", "cora");

		recordCreatorOld.createAndStoreRecord(AUTH_TOKEN, "typeWithUserGeneratedId", record);

		termCollector.MCR.assertParameter("collectTerms", 0, "metadataId",
				"fakeDefMetadataIdFromRecordTypeHandlerSpy");
		CollectTerms collectTerms = (CollectTerms) termCollector.MCR.getReturnValue("collectTerms",
				1);
		recordStorage.MCR.assertParameter("create", 0, "storageTerms", collectTerms.storageTerms);
	}

	@Test(expectedExceptions = AuthorizationException.class)
	public void testCreateRecordUnauthorizedForActionCreate() {
		spiderAuthorizator.authorizedForActionAndRecordType = false;

		recordCreatorOld.createAndStoreRecord(AUTH_TOKEN, RECORD_TYPE, recordWithoutIdOld);
	}

	@Test
	public void testUnauthorizedForCreateOnRecordTypeShouldShouldNotAccessStorage() {
		spiderAuthorizator.authorizedForActionAndRecordType = false;

		try {
			recordCreatorOld.createAndStoreRecord(AUTH_TOKEN, RECORD_TYPE, recordWithoutIdOld);
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
			+ "Excpetion thrown from "
			+ "checkGetUsersMatchedRecordPartPermissionsForActionOnRecordTypeAndCollectedData from Spy")
	public void testCreateRecordUnauthorizedForDataInRecord() {
		spiderAuthorizator.authorizedForActionAndRecordTypeAndCollectedData = false;

		recordCreatorOld.createAndStoreRecord(AUTH_TOKEN, RECORD_TYPE, recordWithoutIdOld);
	}

	@Test

	public void testCreateRecordCollectedDataUsedForAuthorization() {
		recordCreatorOld.createAndStoreRecord(AUTH_TOKEN, RECORD_TYPE, recordWithoutIdOld);

		String methodName = "checkGetUsersMatchedRecordPartPermissionsForActionOnRecordTypeAndCollectedData";
		CollectTerms collectTerms = (CollectTerms) termCollector.MCR.getReturnValue("collectTerms",
				0);
		spiderAuthorizator.MCR.assertParameters(methodName, 0, authenticatorOld.returnedUser,
				"create", RECORD_TYPE, collectTerms.permissionTerms);
	}

	@Test(expectedExceptions = RecordConflictException.class)
	public void testCreateRecordDuplicateUserSuppliedId() {
		recordTypeHandlerSpy.MRV.setDefaultReturnValuesSupplier("getRecordTypeId", () -> "place");
		recordStorage.MRV.setAlwaysThrowException("create", RecordConflictException
				.withMessage("Record with recordId: " + "somePlace" + " already exists"));

		DataGroup record = DataCreator2.createRecordWithNameInDataAndIdAndLinkedRecordId("place",
				"somePlace", "cora");

		recordCreatorOld.createAndStoreRecord(AUTH_TOKEN, "place", record);
		recordCreatorOld.createAndStoreRecord(AUTH_TOKEN, "place", record);
	}

	@Test(expectedExceptions = DataException.class, expectedExceptionsMessageRegExp = ""
			+ "Data is not valid: linkedRecord does not exists in storage for "
			+ "recordType: toType and recordId: toId")
	public void testErrorThrownIfCollectedLinkDoesNotExistInStorage() {
		setupLinkCollectorToReturnALinkSoThatWeCheckThatTheLinkedRecordExistsInStorage();
		recordTypeHandlerSpy.MRV.setDefaultReturnValuesSupplier("getRecordTypeId",
				() -> "dataWithLinks");

		DataGroup dataGroup = RecordLinkTestsDataCreator.createDataDataGroupWithLink();
		dataGroup.addChild(DataCreator2.createRecordInfoWithLinkedDataDividerId("cora"));

		recordCreatorOld.createAndStoreRecord(AUTH_TOKEN, "dataWithLinks", dataGroup);
	}

	private void setupLinkCollectorToReturnALinkSoThatWeCheckThatTheLinkedRecordExistsInStorage() {
		Link link = new Link("toType", "toId");
		linkCollector.MRV.setDefaultReturnValuesSupplier("collectLinks", () -> Set.of(link));
	}

	@Test
	public void testLinkedRecordIdExists() {
		setupLinkCollectorToReturnALinkSoThatWeCheckThatTheLinkedRecordExistsInStorage();
		recordTypeHandlerSpy.MRV.setDefaultReturnValuesSupplier("getRecordTypeId",
				() -> "dataWithLinks");
		recordStorage.MRV.setSpecificReturnValuesSupplier("recordExists",
				(Supplier<Boolean>) () -> true, List.of("toType"), "toId");
		DataGroup dataGroup = RecordLinkTestsDataCreator.createDataDataGroupWithLink();
		dataGroup.addChild(DataCreator2.createRecordInfoWithLinkedDataDividerId("cora"));

		recordCreatorOld.createAndStoreRecord(AUTH_TOKEN, "dataWithLinks", dataGroup);

		List<?> types = (List<?>) recordStorage.MCR
				.getValueForMethodNameAndCallNumberAndParameterName("recordExists", 0, "types");
		assertEquals(types.size(), 1);
		assertEquals(types.get(0), "toType");
		recordStorage.MCR.assertParameter("recordExists", 0, "id", "toId");
	}

	@Test
	public void testReturnRecordWithoutReadActionIfUserHasCreateButNotReadAccess() {
		dataGroupToRecordEnhancer.addReadAction = false;

		DataRecord createdRecord = recordCreatorOld.createAndStoreRecord(AUTH_TOKEN, RECORD_TYPE,
				recordWithoutIdOld);

		dataGroupToRecordEnhancer.MCR.assertReturn("enhanceIgnoringReadAccess", 0, createdRecord);
	}

	@Test
	public void testRedactorNotCalledWhenNoRecordPartConstraints() throws Exception {
		recordCreatorOld.createAndStoreRecord(AUTH_TOKEN, RECORD_TYPE, recordWithoutIdOld);

		recordTypeHandlerSpy.MCR.assertMethodWasCalled("hasRecordPartCreateConstraint");
		dataRedactor.MCR.assertMethodNotCalled("removeChildrenForConstraintsWithoutPermissions");
	}

	@Test
	public void testRedactorCalledCorrectlyWhenRecordPartConstraints() throws Exception {
		setupForRecordPartConstraints();

		recordCreatorOld.createAndStoreRecord(AUTH_TOKEN, RECORD_TYPE, recordWithoutIdOld);

		recordTypeHandlerSpy.MCR.assertMethodWasCalled("hasRecordPartCreateConstraint");

		assertDataRedactorRemoveChildrenForConstraintsWithoutPermissions(recordWithoutIdOld);
	}

	private void setupForRecordPartConstraints() {
		DataGroup dataGroupReturnedFromRedactor = DataCreator2
				.createRecordWithNameInDataAndLinkedDataDividerId("nameInData", "cora");
		dataRedactor.returnDataGroup = dataGroupReturnedFromRedactor;
		recordTypeHandlerSpy.recordPartConstraint = "write";
	}

	private void assertDataRedactorRemoveChildrenForConstraintsWithoutPermissions(
			DataGroup dataGroup) {
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
	public void testValidateIsNotCalledWithRedactedDataGroupWhenNoRecordPartConstraints()
			throws Exception {
		recordCreatorOld.createAndStoreRecord(AUTH_TOKEN, RECORD_TYPE, recordWithoutIdOld);

		String newMetadataId = (String) recordTypeHandlerSpy.MCR
				.getReturnValue("getCreateDefinitionId", 0);

		dataValidatorOld.MCR.assertParameters("validateData", 0, newMetadataId, recordWithoutIdOld);
	}

	@Test
	public void testValidateIsCalledWithRedactedDataGroupWhenRecordPartConstraints()
			throws Exception {
		setupForRecordPartConstraints();

		recordCreatorOld.createAndStoreRecord(AUTH_TOKEN, RECORD_TYPE, recordWithoutIdOld);

		DataGroup redactedDataGroup = (DataGroup) dataRedactor.MCR
				.getReturnValue("removeChildrenForConstraintsWithoutPermissions", 0);
		String newMetadataId = (String) recordTypeHandlerSpy.MCR
				.getReturnValue("getCreateDefinitionId", 0);
		dataValidatorOld.MCR.assertParameters("validateData", 0, newMetadataId, redactedDataGroup);
	}

	@Test
	public void testStoreInArchiveTrue() throws Exception {
		recordTypeHandlerSpy.MRV.setDefaultReturnValuesSupplier("storeInArchive", () -> true);

		recordCreatorOld.createAndStoreRecord(AUTH_TOKEN, RECORD_TYPE, recordWithId);

		recordArchive.MCR.assertParameters("create", 0, RECORD_TYPE, "someRecordId", recordWithId);
	}

	@Test
	public void testStoreInArchiveFalse() throws Exception {
		recordCreatorOld.createAndStoreRecord(AUTH_TOKEN, RECORD_TYPE, recordWithId);

		recordArchive.MCR.assertMethodNotCalled("create");
	}

	@Test
	public void testRecordCreatorGetsUniqueValiadatorFromDependencyProvider() throws Exception {
		recordCreator.createAndStoreRecord(AUTH_TOKEN, RECORD_TYPE, recordWithId);

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

		recordCreator.createAndStoreRecord(AUTH_TOKEN, RECORD_TYPE, recordWithId);

		uniqueValidator.MCR.assertMethodWasCalled("validateUnique");
		uniqueValidator.MCR.assertParameters("validateUnique", 0, RECORD_TYPE, uniqueList,
				collectTerms.storageTerms);
	}

	@Test
	public void testUniqueValidationFails_throwsSpiderConflictException() throws Exception {
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
		uniqueValidator.MRV.setDefaultReturnValuesSupplier("validateUnique",
				() -> validationAnswer);
	}

}