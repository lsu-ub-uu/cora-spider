/*
 * Copyright 2015, 2016, 2017, 2023 Uppsala University Library
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
import static org.testng.Assert.assertTrue;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Supplier;

import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import se.uu.ub.cora.beefeater.authentication.User;
import se.uu.ub.cora.bookkeeper.validator.DataValidationException;
import se.uu.ub.cora.data.DataChild;
import se.uu.ub.cora.data.DataGroup;
import se.uu.ub.cora.data.DataProvider;
import se.uu.ub.cora.data.DataRecord;
import se.uu.ub.cora.data.collected.CollectTerms;
import se.uu.ub.cora.data.collected.Link;
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
import se.uu.ub.cora.spider.data.DataGroupOldSpy;
import se.uu.ub.cora.spider.dependency.spy.RecordTypeHandlerSpy;
import se.uu.ub.cora.spider.dependency.spy.SpiderDependencyProviderOldSpy;
import se.uu.ub.cora.spider.extendedfunctionality.ExtendedFunctionality;
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
import se.uu.ub.cora.spider.spy.DataValidatorSpy;
import se.uu.ub.cora.spider.spy.IdGeneratorSpy;
import se.uu.ub.cora.spider.spy.OldSpiderAuthorizatorSpy;
import se.uu.ub.cora.spider.spy.RecordArchiveSpy;
import se.uu.ub.cora.spider.spy.RecordIndexerSpy;
import se.uu.ub.cora.spider.spy.RuleCalculatorSpy;
import se.uu.ub.cora.spider.spy.SpiderDependencyProviderSpy;
import se.uu.ub.cora.spider.testdata.DataCreator2;
import se.uu.ub.cora.spider.testdata.RecordLinkTestsDataCreator;
import se.uu.ub.cora.storage.RecordConflictException;
import se.uu.ub.cora.storage.idgenerator.RecordIdGenerator;
import se.uu.ub.cora.storage.spies.RecordStorageSpy;

public class RecordCreatorTest {
	private static final String TIMESTAMP_FORMAT = "\\d{4}-\\d{2}-\\d{2}T\\d{2}:\\d{2}:\\d{2}\\.\\d{6}Z";
	private RecordStorageSpy recordStorage;
	private OldAuthenticatorSpy authenticator;
	private OldSpiderAuthorizatorSpy spiderAuthorizator;
	private RecordIdGenerator idGenerator;
	private PermissionRuleCalculator ruleCalculator;
	private RecordCreator recordCreatorOld;
	private RecordCreator recordCreator;
	private DataValidatorSpy dataValidator;
	private DataRecordLinkCollectorSpy linkCollector;
	private SpiderDependencyProviderOldSpy dependencyProvider;
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

	@BeforeMethod
	public void beforeMethod() {
		setUpFactoriesAndProviders();

		setUpToReturn1ForCreatedAtomicIds();
		setUpToReturnFakeDataForCreatedTS();

		authenticator = new OldAuthenticatorSpy();
		spiderAuthorizator = new OldSpiderAuthorizatorSpy();
		dataValidator = new DataValidatorSpy();
		recordStorage = new RecordStorageSpy();
		idGenerator = new IdGeneratorSpy();
		ruleCalculator = new RuleCalculatorSpy();
		linkCollector = new DataRecordLinkCollectorSpy();
		extendedFunctionalityProvider = new ExtendedFunctionalityProviderSpy();
		termCollector = new DataGroupTermCollectorSpy();
		recordIndexer = new RecordIndexerSpy();
		recordArchive = new RecordArchiveSpy();
		dataRedactor = new DataRedactorSpy();
		setUpDependencyProvider();

		recordTypeHandlerSpy.MRV.setDefaultReturnValuesSupplier("getRecordTypeId", () -> "spyType");

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

	private void setUpDependencyProvider() {
		dependencyProvider = new SpiderDependencyProviderOldSpy();
		dependencyProvider.authenticator = authenticator;
		dependencyProvider.spiderAuthorizator = spiderAuthorizator;
		dependencyProvider.dataValidator = dataValidator;
		dependencyProvider.recordStorage = recordStorage;
		dependencyProvider.recordIdGenerator = idGenerator;
		dependencyProvider.ruleCalculator = ruleCalculator;
		dependencyProvider.linkCollector = linkCollector;
		dependencyProvider.extendedFunctionalityProvider = extendedFunctionalityProvider;
		dependencyProvider.termCollector = termCollector;
		dependencyProvider.recordIndexer = recordIndexer;
		dependencyProvider.recordArchive = recordArchive;
		recordTypeHandlerSpy = dependencyProvider.recordTypeHandlerSpy;
		dependencyProvider.dataRedactor = dataRedactor;

		dependencyProviderSpy = new SpiderDependencyProviderSpy();

		dataGroupToRecordEnhancer = new DataGroupToRecordEnhancerSpy();
		recordCreatorOld = RecordCreatorImp.usingDependencyProviderAndDataGroupToRecordEnhancer(
				dependencyProvider, dataGroupToRecordEnhancer);
		recordCreator = RecordCreatorImp.usingDependencyProviderAndDataGroupToRecordEnhancer(
				dependencyProviderSpy, dataGroupToRecordEnhancer);
	}

	@Test
	public void testRecordTypeHandlerFetchedFromDependencyProvider() {
		recordTypeHandlerSpy.MRV.setDefaultReturnValuesSupplier("shouldAutoGenerateId", () -> true);
		dependencyProviderSpy.MRV.setDefaultReturnValuesSupplier(
				"getRecordTypeHandlerUsingDataRecordGroup", () -> recordTypeHandlerSpy);
		DataGroup dataGroup = createRecordWithNameInDataNameInDataAndDataDividerCora();

		recordCreator.createAndStoreRecord("dummyAuthenticatedToken", "spyType", dataGroup);

		dataFactorySpy.MCR.assertParameters("factorRecordGroupFromDataGroup", 0, dataGroup);
		var dataGroupAsRecordGroup = dataFactorySpy.MCR
				.getReturnValue("factorRecordGroupFromDataGroup", 0);
		dependencyProviderSpy.MCR.assertParameters("getRecordTypeHandlerUsingDataRecordGroup", 0,
				dataGroupAsRecordGroup);
	}

	private DataGroup createRecordWithNameInDataNameInDataAndDataDividerCora() {
		return DataCreator2.createRecordWithNameInDataAndLinkedDataDividerId("nameInData", "cora");
	}

	@Test(expectedExceptions = DataException.class, expectedExceptionsMessageRegExp = "The record "
			+ "cannot be created because the record type provided does not match the record type "
			+ "that the validation type is set to validate.")
	public void testRecordTypePassedNOTEqualsTheLinkedInValidationType() throws Exception {
		recordTypeHandlerSpy.MRV.setDefaultReturnValuesSupplier("getRecordTypeId",
				() -> "NOTRecordTypeId");

		dependencyProviderSpy.MRV.setDefaultReturnValuesSupplier(
				"getRecordTypeHandlerUsingDataRecordGroup", () -> recordTypeHandlerSpy);

		DataGroupSpy dataGroupSpy = new DataGroupSpy();

		recordCreator.createAndStoreRecord("dummyAuthenticatedToken", "recordTypeInput",
				dataGroupSpy);

	}

	@Test(expectedExceptions = DataException.class, expectedExceptionsMessageRegExp = ""
			+ "Data is not valid: some message")
	public void testValidationTypeDoesNotExist() throws Exception {
		recordTypeHandlerSpy.MRV.setDefaultReturnValuesSupplier("shouldAutoGenerateId", () -> true);
		dependencyProviderSpy.MRV.setDefaultReturnValuesSupplier(
				"getRecordTypeHandlerUsingDataRecordGroup", () -> recordTypeHandlerSpy);
		DataGroup dataGroup = createRecordWithNameInDataNameInDataAndDataDividerCora();
		recordTypeHandlerSpy.MRV.setAlwaysThrowException("getCreateDefinitionId",
				DataValidationException.withMessage("some message"));

		dependencyProviderSpy.MRV.setDefaultReturnValuesSupplier(
				"getRecordTypeHandlerUsingDataRecordGroup", () -> recordTypeHandlerSpy);

		recordCreator.createAndStoreRecord("dummyAuthenticatedToken", "spyType", dataGroup);
	}

	@Test
	public void testAuthenticatorIsCalled() throws Exception {
		recordTypeHandlerSpy.MRV.setDefaultReturnValuesSupplier("getRecordTypeId", () -> "spyType");
		recordTypeHandlerSpy.MRV.setDefaultReturnValuesSupplier("shouldAutoGenerateId", () -> true);
		DataGroup dataGroup = createRecordWithNameInDataNameInDataAndDataDividerCora();

		String authToken = "dummyAuthenticatedToken";
		recordCreatorOld.createAndStoreRecord(authToken, "spyType", dataGroup);

		authenticator.MCR.assertParameters("getUserForToken", 0, authToken);
	}

	@Test
	public void testAuthorizatorIsCalled() throws Exception {
		recordTypeHandlerSpy.MRV.setDefaultReturnValuesSupplier("shouldAutoGenerateId", () -> true);
		DataGroup dataGroup = createRecordWithNameInDataNameInDataAndDataDividerCora();
		String recordType = "spyType";

		recordCreatorOld.createAndStoreRecord("dummyAuthenticatedToken", recordType, dataGroup);

		User user = (User) authenticator.MCR.getReturnValue("getUserForToken", 0);
		spiderAuthorizator.MCR.assertParameters("checkUserIsAuthorizedForActionOnRecordType", 0,
				user, "create", recordType);
	}

	@Test
	public void testNotPossibleToCreateRecordWithTypeAndIdWhichAlreadyExists() throws Exception {
		recordTypeHandlerSpy.MRV.setDefaultReturnValuesSupplier("shouldAutoGenerateId", () -> true);
		DataGroup dataGroup = createRecordWithNameInDataNameInDataAndDataDividerCora();
		String authToken = "dummyAuthenticatedToken";

		recordStorage.MRV.setSpecificReturnValuesSupplier("recordExists",
				(Supplier<Boolean>) () -> true, List.of("spyType"), "1");
		try {
			recordCreatorOld.createAndStoreRecord(authToken, "spyType", dataGroup);
			assertTrue(false);
		} catch (Exception e) {
			assertTrue(e instanceof ConflictException);
			assertEquals(e.getMessage(),
					"Record " + "with type: spyType and id: 1 already exists in storage");

			recordTypeHandlerSpy.MCR.assertMethodNotCalled("getParentId");

			Map<String, Object> parameters = recordStorage.MCR
					.getParametersForMethodAndCallNumber("recordExists", 0);
			List<?> types = (List<?>) parameters.get("types");
			assertEquals(types.get(0), "spyType");
			assertEquals(parameters.get("id"), "1");
		}
	}

	@Test
	public void testNotPossibleToCreateRecordWithTypeAndIdWhichAlreadyExistsForImplementingType() {
		recordTypeHandlerSpy.MRV.setDefaultReturnValuesSupplier("shouldAutoGenerateId", () -> true);
		DataGroup dataGroup = createRecordWithNameInDataNameInDataAndDataDividerCora();
		String authToken = "dummyAuthenticatedToken";

		recordTypeHandlerSpy.MRV.setDefaultReturnValuesSupplier("hasParent", () -> true);
		recordStorage.MRV.setSpecificReturnValuesSupplier("recordExists",
				(Supplier<Boolean>) () -> true, List.of("oneImplementingTypeId"), "1");

		try {
			recordCreatorOld.createAndStoreRecord(authToken, "spyType", dataGroup);
			assertTrue(false);
		} catch (Exception e) {
			assertTrue(e instanceof ConflictException);
			assertEquals(e.getMessage(),
					"Record " + "with type: spyType and id: 1 already exists in storage");

			recordTypeHandlerSpy.MCR.assertMethodWasCalled("getParentId");

			var parentId = recordTypeHandlerSpy.MCR.getReturnValue("getParentId", 0);

			dependencyProvider.MCR.assertParameters("getRecordTypeHandler", 0, parentId);
			RecordTypeHandlerSpy parentRecordTypeHandler = (RecordTypeHandlerSpy) dependencyProvider.MCR
					.getReturnValue("getRecordTypeHandler", 0);
			parentRecordTypeHandler.MCR.assertParameters("getListOfRecordTypeIdsToReadFromStorage",
					0);
			var types = parentRecordTypeHandler.MCR
					.getReturnValue("getListOfRecordTypeIdsToReadFromStorage", 0);

			recordStorage.MCR.assertParameters("recordExists", 0, types, "1");
		}
	}

	@Test
	public void testExtendedFunctionalityIsCalled() {
		String recordType = "spyType";
		String authToken = "someToken78678567";
		DataGroup dataGroup = createRecordWithNameInDataNameInDataAndDataDividerCora();
		recordTypeHandlerSpy.MRV.setDefaultReturnValuesSupplier("shouldAutoGenerateId", () -> true);

		recordCreatorOld.createAndStoreRecord(authToken, recordType, dataGroup);

		ExtendedFunctionalityData expectedData = new ExtendedFunctionalityData();
		expectedData.recordType = recordType;
		expectedData.recordId = null;
		expectedData.authToken = authToken;
		expectedData.user = (User) authenticator.MCR.getReturnValue("getUserForToken", 0);
		expectedData.previouslyStoredTopDataGroup = null;
		expectedData.dataGroup = dataGroup;

		extendedFunctionalityProvider.assertCallToMethodAndFunctionalityCalledWithData(
				"getFunctionalityForCreateBeforeMetadataValidation", expectedData);
		extendedFunctionalityProvider.assertCallToMethodAndFunctionalityCalledWithData(
				"getFunctionalityForCreateAfterMetadataValidation", expectedData);
		expectedData.recordId = "1";
		extendedFunctionalityProvider.assertCallToMethodAndFunctionalityCalledWithData(
				"getFunctionalityForCreateBeforeReturn", expectedData);
	}

	class ExtendedMock implements ExtendedFunctionality {
		@Override
		public void useExtendedFunctionality(ExtendedFunctionalityData data) {
			throw new RuntimeException();
		}
	}

	@Test
	public void testExtendedFunctionalityBeforeMetadataCalledBeforeRecordTypeHandlerCreatedSoWeDoNotNeedToHaveARecordInfoForSomeTypes() {
		recordTypeHandlerSpy.MRV.setDefaultReturnValuesSupplier("shouldAutoGenerateId", () -> true);
		dependencyProviderSpy.MRV.setDefaultReturnValuesSupplier(
				"getRecordTypeHandlerUsingDataRecordGroup", () -> recordTypeHandlerSpy);
		dependencyProviderSpy.MRV.setDefaultReturnValuesSupplier("getExtendedFunctionalityProvider",
				() -> extendedFunctionalityProvider);

		extendedFunctionalityProvider.MRV.setDefaultReturnValuesSupplier(
				"getFunctionalityForCreateBeforeMetadataValidation",
				() -> List.of(new ExtendedMock()));
		DataGroup dataGroup = createRecordWithNameInDataNameInDataAndDataDividerCora();
		recordCreator = RecordCreatorImp.usingDependencyProviderAndDataGroupToRecordEnhancer(
				dependencyProviderSpy, dataGroupToRecordEnhancer);
		try {
			recordCreator.createAndStoreRecord("dummyAuthenticatedToken", "spyType", dataGroup);
		} catch (Exception e) {

		}

		dataFactorySpy.MCR.assertMethodNotCalled("factorRecordGroupFromDataGroup");
		dependencyProviderSpy.MCR.assertMethodNotCalled("getRecordTypeHandlerUsingDataRecordGroup");
	}

	@Test
	public void testRecordEnhancerCalled() {
		recordTypeHandlerSpy.MRV.setDefaultReturnValuesSupplier("shouldAutoGenerateId", () -> true);
		DataGroup dataGroup = createRecordWithNameInDataNameInDataAndDataDividerCora();

		recordCreatorOld.createAndStoreRecord("dummyAuthenticatedToken", "spyType", dataGroup);

		assertCallEnhanceIgnoringReadAccess(dataGroup);
	}

	private void assertCallEnhanceIgnoringReadAccess(DataGroup dataGroup) {
		User user = (User) authenticator.MCR.getReturnValue("getUserForToken", 0);
		dataGroupToRecordEnhancer.MCR.assertParameters("enhanceIgnoringReadAccess", 0, user,
				"spyType", dataGroup, dataRedactor);
	}

	@Test(expectedExceptions = AuthenticationException.class)
	public void testGetActiveUserFails() {
		authenticator.throwAuthenticationException = true;

		DataGroup dataGroup = createRecordWithNameInDataNameInDataAndDataDividerCora();

		recordCreatorOld.createAndStoreRecord("dummyNonAuthenticatedToken", "spyType", dataGroup);
	}

	@Test
	public void testIndexerIsCalled() {
		String authToken = "someToken78678567";
		DataGroup dataGroup = createRecordWithNameInDataNameInDataAndDataDividerCora();
		recordTypeHandlerSpy.MRV.setDefaultReturnValuesSupplier("shouldAutoGenerateId", () -> true);

		recordCreatorOld.createAndStoreRecord(authToken, "spyType", dataGroup);

		var dataRecord = recordStorage.MCR
				.getValueForMethodNameAndCallNumberAndParameterName("create", 0, "dataRecord");

		List<?> ids = (List<?>) recordTypeHandlerSpy.MCR
				.getReturnValue("getCombinedIdsUsingRecordId", 0);
		CollectTerms collectTerms = (CollectTerms) termCollector.MCR.getReturnValue("collectTerms",
				1);
		recordIndexer.MCR.assertParameters("indexData", 0, ids, collectTerms.indexTerms,
				dataRecord);
	}

	@Test(expectedExceptions = DataException.class)
	public void testCreateRecordInvalidData() {
		dataValidator.validValidation = false;

		DataGroup dataGroup = new DataGroupOldSpy("nameInData");

		recordCreatorOld.createAndStoreRecord("someToken78678567", "recordType", dataGroup);
	}

	@Test
	public void testCreateRecordAutogeneratedId() {
		setUpDependencyProvider();
		recordTypeHandlerSpy.MRV.setDefaultReturnValuesSupplier("getRecordTypeId",
				() -> "typeWithAutoGeneratedId");
		recordTypeHandlerSpy.MRV.setDefaultReturnValuesSupplier("shouldAutoGenerateId", () -> true);

		DataGroup record = DataCreator2.createRecordWithNameInDataAndLinkedDataDividerId(
				"typeWithAutoGeneratedId", "cora");

		DataRecord recordOut = recordCreatorOld.createAndStoreRecord("someToken78678567",
				"typeWithAutoGeneratedId", record);

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
				"type", "recordType", "typeWithAutoGeneratedId");
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
		setUpDependencyProvider();
		recordTypeHandlerSpy.MRV.setDefaultReturnValuesSupplier("getRecordTypeId",
				() -> "typeWithAutoGeneratedId");
		recordTypeHandlerSpy.MRV.setDefaultReturnValuesSupplier("shouldAutoGenerateId", () -> true);

		DataGroup record = DataCreator2.createRecordWithNameInDataAndLinkedDataDividerId(
				"typeWithAutoGeneratedId", "cora");

		DataRecord recordOut = recordCreatorOld.createAndStoreRecord("someToken78678567",
				"typeWithAutoGeneratedId", record);

		DataGroup groupOut = recordOut.getDataGroup();
		DataGroup recordInfo = groupOut.getFirstGroupWithNameInData("recordInfo");
		String recordId = recordInfo.getFirstAtomicValueWithNameInData("id");
		assertEquals(recordId, "1");

		recordStorage.MCR.assertParameter("create", 0, "type", "typeWithAutoGeneratedId");
		recordStorage.MCR.assertParameter("create", 0, "id", "1");

	}

	@Test
	public void testCreateRecordAutogeneratedIdSentInIdIsIgnored() {
		setUpDependencyProvider();
		recordTypeHandlerSpy.MRV.setDefaultReturnValuesSupplier("getRecordTypeId",
				() -> "typeWithAutoGeneratedIdWrongRecordInfo");
		recordTypeHandlerSpy.MRV.setDefaultReturnValuesSupplier("shouldAutoGenerateId", () -> true);
		DataGroup record = DataCreator2.createRecordWithNameInDataAndLinkedDataDividerId(
				"typeWithAutoGeneratedIdWrongRecordInfo", "cora");

		DataGroupOldSpy createdRecordInfo = (DataGroupOldSpy) record
				.getFirstGroupWithNameInData("recordInfo");
		createdRecordInfo.addChild(
				new se.uu.ub.cora.spider.data.DataAtomicSpy("id", "someIdThatShouldNotBeHere"));
		DataRecord recordOut = recordCreatorOld.createAndStoreRecord("someToken78678567",
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
		setUpDependencyProvider();
		recordTypeHandlerSpy.MRV.setDefaultReturnValuesSupplier("getRecordTypeId",
				() -> "spyType2");

		DataGroup record = DataCreator2.createRecordWithNameInDataAndIdAndLinkedRecordId(
				"testingRecordInfo", "someId", "cora");

		DataRecord recordOut = recordCreatorOld.createAndStoreRecord("someToken78678567",
				"spyType2", record);
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
				"type", "recordType", "spyType2");
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
		setUpDependencyProvider();
		recordTypeHandlerSpy.MRV.setDefaultReturnValuesSupplier("getRecordTypeId",
				() -> "typeWithUserGeneratedId");

		DataGroup record = DataCreator2.createRecordWithNameInDataAndIdAndLinkedRecordId(
				"typeWithUserGeneratedId", "somePlace", "cora");

		DataRecord recordOut = recordCreatorOld.createAndStoreRecord("someToken78678567",
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
		setUpDependencyProvider();
		recordTypeHandlerSpy.MRV.setDefaultReturnValuesSupplier("getRecordTypeId",
				() -> "typeWithUserGeneratedId");

		DataGroup record = DataCreator2.createRecordWithNameInDataAndIdAndLinkedRecordId(
				"typeWithUserGeneratedId", "somePlace", "cora");

		recordCreatorOld.createAndStoreRecord("someToken78678567", "typeWithUserGeneratedId",
				record);

		recordStorage.MCR.assertParameter("create", 0, "dataDivider", "cora");
	}

	@Test
	public void testCreateRecordCollectedTermsSentAlong() {
		setUpDependencyProvider();
		recordTypeHandlerSpy.MRV.setDefaultReturnValuesSupplier("getRecordTypeId",
				() -> "typeWithUserGeneratedId");
		recordTypeHandlerSpy.MRV.setDefaultReturnValuesSupplier("shouldAutoGenerateId", () -> true);

		DataGroup record = DataCreator2.createRecordWithNameInDataAndIdAndLinkedRecordId(
				"typeWithUserGeneratedId", "somePlace", "cora");

		recordCreatorOld.createAndStoreRecord("someToken78678567", "typeWithUserGeneratedId",
				record);

		recordStorage.MCR.assertParameter("create", 0, "dataDivider", "cora");

		termCollector.MCR.assertParameter("collectTerms", 0, "metadataId",
				"fakeDefMetadataIdFromRecordTypeHandlerSpy");
		CollectTerms collectTerms = (CollectTerms) termCollector.MCR.getReturnValue("collectTerms",
				1);
		recordStorage.MCR.assertParameter("create", 0, "storageTerms", collectTerms.storageTerms);
	}

	@Test(expectedExceptions = AuthorizationException.class)
	public void testCreateRecordUnauthorizedForActionCreate() {
		recordTypeHandlerSpy.MRV.setDefaultReturnValuesSupplier("getRecordTypeId", () -> "place");
		spiderAuthorizator.authorizedForActionAndRecordType = false;
		DataGroup record = DataCreator2
				.createRecordWithNameInDataAndLinkedDataDividerId("authority", "cora");

		recordCreatorOld.createAndStoreRecord("someToken78678567", "place", record);
	}

	@Test
	public void testUnauthorizedForCreateOnRecordTypeShouldShouldNotAccessStorage() {
		spiderAuthorizator.authorizedForActionAndRecordType = false;
		setUpDependencyProvider();
		recordTypeHandlerSpy.MRV.setDefaultReturnValuesSupplier("getRecordTypeId", () -> "spyType");

		DataGroup record = DataCreator2.createRecordWithNameInDataAndLinkedDataDividerId("spyType",
				"cora");
		boolean exceptionWasCaught = false;
		try {
			recordCreatorOld.createAndStoreRecord("someToken78678567", "spyType", record);
		} catch (Exception e) {
			assertEquals(e.getClass(), AuthorizationException.class);
			exceptionWasCaught = true;
		}
		assertTrue(exceptionWasCaught);
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
		recordTypeHandlerSpy.MRV.setDefaultReturnValuesSupplier("getRecordTypeId", () -> "place");
		spiderAuthorizator.authorizedForActionAndRecordTypeAndCollectedData = false;
		recordTypeHandlerSpy.MRV.setDefaultReturnValuesSupplier("shouldAutoGenerateId", () -> true);

		DataGroup record = DataCreator2.createRecordWithNameInDataAndLinkedDataDividerId("place",
				"cora");
		recordCreatorOld.createAndStoreRecord("someToken78678567", "place", record);
	}

	@Test

	public void testCreateRecordCollectedDataUsedForAuthorization() {
		recordTypeHandlerSpy.MRV.setDefaultReturnValuesSupplier("getRecordTypeId",
				() -> "typeWithUserGeneratedId");
		DataGroup record = DataCreator2.createRecordWithNameInDataAndIdAndLinkedRecordId(
				"typeWithUserGeneratedId", "somePlace", "cora");

		recordCreatorOld.createAndStoreRecord("someToken78678567", "typeWithUserGeneratedId",
				record);

		String methodName = "checkGetUsersMatchedRecordPartPermissionsForActionOnRecordTypeAndCollectedData";
		CollectTerms collectTerms = (CollectTerms) termCollector.MCR.getReturnValue("collectTerms",
				0);
		spiderAuthorizator.MCR.assertParameters(methodName, 0, authenticator.returnedUser, "create",
				"typeWithUserGeneratedId", collectTerms.permissionTerms);
	}

	@Test(expectedExceptions = RecordConflictException.class)
	public void testCreateRecordDuplicateUserSuppliedId() {
		recordTypeHandlerSpy.MRV.setDefaultReturnValuesSupplier("getRecordTypeId", () -> "place");
		recordStorage.MRV.setAlwaysThrowException("create", RecordConflictException
				.withMessage("Record with recordId: " + "somePlace" + " already exists"));

		DataGroup record = DataCreator2.createRecordWithNameInDataAndIdAndLinkedRecordId("place",
				"somePlace", "cora");

		recordCreatorOld.createAndStoreRecord("someToken78678567", "place", record);
		recordCreatorOld.createAndStoreRecord("someToken78678567", "place", record);
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
		recordTypeHandlerSpy.MRV.setDefaultReturnValuesSupplier("shouldAutoGenerateId", () -> true);

		recordCreatorOld.createAndStoreRecord("someToken78678567", "dataWithLinks", dataGroup);
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
				(Supplier<Boolean>) () -> true, List.of("oneImplementingTypeId"), "toId");
		DataGroup dataGroup = RecordLinkTestsDataCreator.createDataDataGroupWithLink();
		dataGroup.addChild(DataCreator2.createRecordInfoWithLinkedDataDividerId("cora"));
		recordTypeHandlerSpy.MRV.setDefaultReturnValuesSupplier("shouldAutoGenerateId", () -> true);

		recordCreatorOld.createAndStoreRecord("someToken78678567", "dataWithLinks", dataGroup);

		List<?> types = (List<?>) recordStorage.MCR
				.getValueForMethodNameAndCallNumberAndParameterName("recordExists", 0, "types");
		assertEquals(types.size(), 1);
		assertEquals(types.get(0), "oneImplementingTypeId");
		recordStorage.MCR.assertParameter("recordExists", 0, "id", "toId");

	}

	@Test
	public void testReturnRecordWithoutReadActionIfUserHasCreateButNotReadAccess() {
		recordTypeHandlerSpy.MRV.setDefaultReturnValuesSupplier("shouldAutoGenerateId", () -> true);
		DataGroup dataGroup = createRecordWithNameInDataNameInDataAndDataDividerCora();
		dataGroupToRecordEnhancer.addReadAction = false;

		DataRecord createdRecord = recordCreatorOld.createAndStoreRecord("dummyAuthenticatedToken",
				"spyType", dataGroup);

		dataGroupToRecordEnhancer.MCR.assertReturn("enhanceIgnoringReadAccess", 0, createdRecord);
	}

	@Test
	public void testRedactorNotCalledWhenNoRecordPartConstraints() throws Exception {
		DataGroup dataGroup = setupForNoRecordPartConstraints();

		recordCreatorOld.createAndStoreRecord("dummyAuthenticatedToken", "spyType", dataGroup);

		recordTypeHandlerSpy.MCR.assertMethodWasCalled("hasRecordPartCreateConstraint");
		dataRedactor.MCR.assertMethodNotCalled("removeChildrenForConstraintsWithoutPermissions");
	}

	private DataGroup setupForNoRecordPartConstraints() {
		recordTypeHandlerSpy.MRV.setDefaultReturnValuesSupplier("shouldAutoGenerateId", () -> true);
		DataGroup dataGroup = createRecordWithNameInDataNameInDataAndDataDividerCora();
		recordTypeHandlerSpy.recordPartConstraint = "";
		return dataGroup;
	}

	@Test
	public void testRedactorCalledCorrectlyWhenRecordPartConstraints() throws Exception {
		DataGroup dataGroup = setupForRecordPartConstraints();

		recordCreatorOld.createAndStoreRecord("dummyAuthenticatedToken", "spyType", dataGroup);

		recordTypeHandlerSpy.MCR.assertMethodWasCalled("hasRecordPartCreateConstraint");

		assertDataRedactorRemoveChildrenForConstraintsWithoutPermissions(dataGroup);
	}

	private DataGroup setupForRecordPartConstraints() {
		recordTypeHandlerSpy.MRV.setDefaultReturnValuesSupplier("shouldAutoGenerateId", () -> true);
		DataGroup dataGroup = createRecordWithNameInDataNameInDataAndDataDividerCora();
		DataGroup dataGroupReturnedFromRedactor = DataCreator2
				.createRecordWithNameInDataAndLinkedDataDividerId("nameInData", "cora");
		dataRedactor.returnDataGroup = dataGroupReturnedFromRedactor;
		recordTypeHandlerSpy.recordPartConstraint = "write";
		return dataGroup;
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
		DataGroup dataGroup = setupForNoRecordPartConstraints();

		recordCreatorOld.createAndStoreRecord("dummyAuthenticatedToken", "spyType", dataGroup);

		String newMetadataId = (String) recordTypeHandlerSpy.MCR
				.getReturnValue("getCreateDefinitionId", 0);

		dataValidator.MCR.assertParameters("validateData", 0, newMetadataId, dataGroup);
	}

	@Test
	public void testValidateIsCalledWithRedactedDataGroupWhenRecordPartConstraints()
			throws Exception {
		DataGroup dataGroup = setupForRecordPartConstraints();

		recordCreatorOld.createAndStoreRecord("dummyAuthenticatedToken", "spyType", dataGroup);

		DataGroup redactedDataGroup = (DataGroup) dataRedactor.MCR
				.getReturnValue("removeChildrenForConstraintsWithoutPermissions", 0);

		String newMetadataId = (String) recordTypeHandlerSpy.MCR
				.getReturnValue("getCreateDefinitionId", 0);

		dataValidator.MCR.assertParameters("validateData", 0, newMetadataId, redactedDataGroup);
	}

	@Test
	public void testStoreInArchiveTrue() throws Exception {
		recordTypeHandlerSpy.MRV.setDefaultReturnValuesSupplier("storeInArchive", () -> true);
		DataGroupSpy recordSpy = createDataGroupForCreate();

		recordCreatorOld.createAndStoreRecord("dummyAuthenticatedToken", "spyType", recordSpy);

		recordArchive.MCR.assertParameters("create", 0, "spyType", "someRecordId", recordSpy);
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
	public void testStoreInArchiveFalse() throws Exception {
		DataGroupSpy recordSpy = createDataGroupForCreate();

		recordCreatorOld.createAndStoreRecord("dummyAuthenticatedToken", "spyType", recordSpy);

		recordArchive.MCR.assertMethodNotCalled("create");
	}

}