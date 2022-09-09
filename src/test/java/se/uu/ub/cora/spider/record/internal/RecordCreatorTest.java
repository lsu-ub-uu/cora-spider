/*
 * Copyright 2015, 2016, 2017 Uppsala University Library
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

import java.util.HashMap;
import java.util.List;
import java.util.Set;
import java.util.function.Supplier;

import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import se.uu.ub.cora.beefeater.authentication.User;
import se.uu.ub.cora.bookkeeper.linkcollector.DataRecordLinkCollector;
import se.uu.ub.cora.data.DataAtomicProvider;
import se.uu.ub.cora.data.DataChild;
import se.uu.ub.cora.data.DataGroup;
import se.uu.ub.cora.data.DataGroupFactory;
import se.uu.ub.cora.data.DataGroupProvider;
import se.uu.ub.cora.data.DataProvider;
import se.uu.ub.cora.data.DataRecord;
import se.uu.ub.cora.data.collectterms.CollectTerms;
import se.uu.ub.cora.data.copier.DataCopierFactory;
import se.uu.ub.cora.data.copier.DataCopierProvider;
import se.uu.ub.cora.logger.LoggerProvider;
import se.uu.ub.cora.spider.authentication.AuthenticationException;
import se.uu.ub.cora.spider.authentication.AuthenticatorSpy;
import se.uu.ub.cora.spider.authorization.AuthorizationException;
import se.uu.ub.cora.spider.authorization.PermissionRuleCalculator;
import se.uu.ub.cora.spider.data.DataAtomicFactorySpy;
import se.uu.ub.cora.spider.data.DataAtomicSpy;
import se.uu.ub.cora.spider.data.DataGroupFactorySpy;
import se.uu.ub.cora.spider.data.DataGroupOldSpy;
import se.uu.ub.cora.spider.dependency.spy.RecordTypeHandlerSpy;
import se.uu.ub.cora.spider.dependency.spy.SpiderDependencyProviderOldSpy;
import se.uu.ub.cora.spider.extendedfunctionality.ExtendedFunctionalityData;
import se.uu.ub.cora.spider.extendedfunctionality.internal.ExtendedFunctionalityProviderSpy;
import se.uu.ub.cora.spider.log.LoggerFactorySpy;
import se.uu.ub.cora.spider.record.DataCopierFactorySpy;
import se.uu.ub.cora.spider.record.DataException;
import se.uu.ub.cora.spider.record.DataGroupToRecordEnhancerSpy;
import se.uu.ub.cora.spider.record.DataRedactorSpy;
import se.uu.ub.cora.spider.record.MisuseException;
import se.uu.ub.cora.spider.record.RecordCreator;
import se.uu.ub.cora.spider.record.RecordLinkTestsRecordStorage;
import se.uu.ub.cora.spider.spy.DataGroupTermCollectorSpy;
import se.uu.ub.cora.spider.spy.DataRecordLinkCollectorSpy;
import se.uu.ub.cora.spider.spy.DataValidatorSpy;
import se.uu.ub.cora.spider.spy.IdGeneratorSpy;
import se.uu.ub.cora.spider.spy.OldRecordStorageSpy;
import se.uu.ub.cora.spider.spy.RecordArchiveSpy;
import se.uu.ub.cora.spider.spy.RecordIndexerSpy;
import se.uu.ub.cora.spider.spy.RecordStorageCreateUpdateSpy;
import se.uu.ub.cora.spider.spy.RecordStorageDuplicateSpy;
import se.uu.ub.cora.spider.spy.RuleCalculatorSpy;
import se.uu.ub.cora.spider.spy.SpiderAuthorizatorSpy;
import se.uu.ub.cora.spider.testdata.DataCreator;
import se.uu.ub.cora.spider.testdata.DataCreator2;
import se.uu.ub.cora.spider.testdata.RecordLinkTestsDataCreator;
import se.uu.ub.cora.spider.testdata.TestDataRecordInMemoryStorage;
import se.uu.ub.cora.storage.RecordConflictException;
import se.uu.ub.cora.storage.RecordIdGenerator;
import se.uu.ub.cora.storage.RecordStorage;
import se.uu.ub.cora.testspies.data.DataFactorySpy;
import se.uu.ub.cora.testspies.data.DataGroupSpy;
import se.uu.ub.cora.testspies.data.DataRecordLinkSpy;

public class RecordCreatorTest {
	private static final String TIMESTAMP_FORMAT = "\\d{4}-\\d{2}-\\d{2}T\\d{2}:\\d{2}:\\d{2}\\.\\d{6}Z";
	private RecordStorage recordStorage;
	private AuthenticatorSpy authenticator;
	private SpiderAuthorizatorSpy spiderAuthorizator;
	private RecordIdGenerator idGenerator;
	private PermissionRuleCalculator ruleCalculator;
	private RecordCreator recordCreator;
	private DataValidatorSpy dataValidator;
	private DataRecordLinkCollector linkCollector;
	private SpiderDependencyProviderOldSpy dependencyProvider;
	private ExtendedFunctionalityProviderSpy extendedFunctionalityProvider;
	private DataGroupToRecordEnhancerSpy dataGroupToRecordEnhancer;
	private DataGroupTermCollectorSpy termCollector;
	private RecordIndexerSpy recordIndexer;
	private LoggerFactorySpy loggerFactorySpy;
	private DataFactorySpy dataFactorySpy;

	private DataGroupFactory dataGroupFactory;
	private DataAtomicFactorySpy dataAtomicFactory;
	private DataCopierFactory dataCopierFactory;
	private RecordTypeHandlerSpy recordTypeHandlerSpy;
	private DataRedactorSpy dataRedactor;
	private RecordArchiveSpy recordArchive;

	@BeforeMethod
	public void beforeMethod() {
		setUpFactoriesAndProviders();
		authenticator = new AuthenticatorSpy();
		spiderAuthorizator = new SpiderAuthorizatorSpy();
		dataValidator = new DataValidatorSpy();
		recordStorage = TestDataRecordInMemoryStorage.createRecordStorageInMemoryWithTestData();
		idGenerator = new IdGeneratorSpy();
		ruleCalculator = new RuleCalculatorSpy();
		linkCollector = new DataRecordLinkCollectorSpy();
		extendedFunctionalityProvider = new ExtendedFunctionalityProviderSpy();
		termCollector = new DataGroupTermCollectorSpy();
		recordIndexer = new RecordIndexerSpy();
		recordArchive = new RecordArchiveSpy();
		setUpDependencyProvider();
	}

	private void setUpFactoriesAndProviders() {
		loggerFactorySpy = new LoggerFactorySpy();
		LoggerProvider.setLoggerFactory(loggerFactorySpy);
		dataFactorySpy = new DataFactorySpy();
		DataProvider.onlyForTestSetDataFactory(dataFactorySpy);

		dataGroupFactory = new DataGroupFactorySpy();
		DataGroupProvider.setDataGroupFactory(dataGroupFactory);
		dataAtomicFactory = new DataAtomicFactorySpy();
		DataAtomicProvider.setDataAtomicFactory(dataAtomicFactory);
		dataCopierFactory = new DataCopierFactorySpy();
		DataCopierProvider.setDataCopierFactory(dataCopierFactory);
		dataRedactor = new DataRedactorSpy();
	}

	private void setUpDependencyProvider() {
		dependencyProvider = new SpiderDependencyProviderOldSpy(new HashMap<>());
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
		dataGroupToRecordEnhancer = new DataGroupToRecordEnhancerSpy();
		recordTypeHandlerSpy = dependencyProvider.recordTypeHandlerSpy;
		dependencyProvider.dataRedactor = dataRedactor;
		recordCreator = RecordCreatorImp.usingDependencyProviderAndDataGroupToRecordEnhancer(
				dependencyProvider, dataGroupToRecordEnhancer);
	}

	@Test
	public void testRecordTypeHandlerFetchedFromDependencyProvider() {
		DataGroup dataGroup = setupForAutoGenerateId();

		recordCreator.createAndStoreRecord("dummyAuthenticatedToken", "spyType", dataGroup);

		dependencyProvider.MCR.assertParameters("getRecordTypeHandler", 0, "spyType");
		dependencyProvider.MCR.assertNumberOfCallsToMethod("getRecordTypeHandler", 1);
	}

	private DataGroup setupRecordStorageAndDataGroup() {
		recordStorage = new OldRecordStorageSpy();
		setUpDependencyProvider();
		DataGroup dataGroup = DataCreator2.createRecordWithNameInDataAndLinkedRecordId("nameInData",
				"cora");
		return dataGroup;
	}

	@Test
	public void testAuthenticatorIsCalled() throws Exception {
		DataGroup dataGroup = setupForAutoGenerateId();

		String authToken = "dummyAuthenticatedToken";
		recordCreator.createAndStoreRecord(authToken, "spyType", dataGroup);

		authenticator.MCR.assertParameters("getUserForToken", 0, authToken);

	}

	@Test
	public void testAuthorizatorIsCalled() throws Exception {
		DataGroup dataGroup = setupForAutoGenerateId();
		String recordType = "spyType";

		recordCreator.createAndStoreRecord("dummyAuthenticatedToken", recordType, dataGroup);

		User user = (User) authenticator.MCR.getReturnValue("getUserForToken", 0);
		spiderAuthorizator.MCR.assertParameters("checkUserIsAuthorizedForActionOnRecordType", 0,
				user, "create", recordType);
	}

	@Test(expectedExceptions = MisuseException.class)
	public void testCreateRecordAbstractRecordType() {
		recordStorage = new OldRecordStorageSpy();
		setUpDependencyProvider();
		DataGroup record = new DataGroupOldSpy("abstract");
		recordTypeHandlerSpy.shouldAutoGenerateId = true;
		recordTypeHandlerSpy.isAbstract = true;

		recordCreator.createAndStoreRecord("someToken78678567", "abstract", record);
	}

	@Test
	public void testExtendedFunctionalityIsCalled() {
		RecordStorageCreateUpdateSpy recordStorageSpy = new RecordStorageCreateUpdateSpy();
		recordStorage = recordStorageSpy;
		String recordType = "spyType";
		String authToken = "someToken78678567";
		DataGroup dataGroup = setupRecordStorageAndDataGroup();
		recordTypeHandlerSpy.shouldAutoGenerateId = true;

		recordCreator.createAndStoreRecord(authToken, recordType, dataGroup);

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

	@Test
	public void testRecordEnhancerCalled() {
		DataGroup dataGroup = setupForAutoGenerateId();

		recordCreator.createAndStoreRecord("dummyAuthenticatedToken", "spyType", dataGroup);

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

		DataGroup dataGroup = setupRecordStorageAndDataGroup();

		recordCreator.createAndStoreRecord("dummyNonAuthenticatedToken", "spyType", dataGroup);
	}

	@Test
	public void testIndexerIsCalled() {
		String authToken = "someToken78678567";
		DataGroup dataGroup = setupRecordStorageAndDataGroup();
		recordTypeHandlerSpy.shouldAutoGenerateId = true;

		recordCreator.createAndStoreRecord(authToken, "spyType", dataGroup);

		DataGroup createdRecord = ((OldRecordStorageSpy) recordStorage).createRecord;

		List<?> ids = (List<?>) recordTypeHandlerSpy.MCR
				.getReturnValue("getCombinedIdsUsingRecordId", 0);
		CollectTerms collectTerms = (CollectTerms) termCollector.MCR.getReturnValue("collectTerms",
				1);
		recordIndexer.MCR.assertParameters("indexData", 0, ids, collectTerms.indexTerms,
				createdRecord);
	}

	@Test(expectedExceptions = DataException.class)
	public void testCreateRecordInvalidData() {
		dataValidator.validValidation = false;

		DataGroup dataGroup = new DataGroupOldSpy("nameInData");

		recordCreator.createAndStoreRecord("someToken78678567", "recordType", dataGroup);
	}

	@Test
	public void testCreateRecordAutogeneratedId() {

		recordStorage = new RecordStorageCreateUpdateSpy();
		setUpDependencyProvider();
		recordTypeHandlerSpy.shouldAutoGenerateId = true;

		DataGroup record = DataCreator2
				.createRecordWithNameInDataAndLinkedRecordId("typeWithAutoGeneratedId", "cora");

		DataRecord recordOut = recordCreator.createAndStoreRecord("someToken78678567",
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

		DataGroup groupCreated = ((RecordStorageCreateUpdateSpy) recordStorage).createRecord;
		assertEquals(groupOut.getNameInData(), groupCreated.getNameInData(),
				"Returned and read record should have the same nameInData");
	}

	@Test
	public void testAutogeneratedIdSentToStorageUsingGeneratedId() {
		recordStorage = new RecordStorageCreateUpdateSpy();
		setUpDependencyProvider();
		recordTypeHandlerSpy.shouldAutoGenerateId = true;

		DataGroup record = DataCreator2
				.createRecordWithNameInDataAndLinkedRecordId("typeWithAutoGeneratedId", "cora");

		DataRecord recordOut = recordCreator.createAndStoreRecord("someToken78678567",
				"typeWithAutoGeneratedId", record);

		DataGroup groupOut = recordOut.getDataGroup();
		DataGroup recordInfo = groupOut.getFirstGroupWithNameInData("recordInfo");
		String recordId = recordInfo.getFirstAtomicValueWithNameInData("id");
		assertEquals(recordId, "1");
		assertEquals(((RecordStorageCreateUpdateSpy) recordStorage).type,
				"typeWithAutoGeneratedId");
		assertEquals(((RecordStorageCreateUpdateSpy) recordStorage).id, "1");
	}

	@Test
	public void testCreateRecordAutogeneratedIdSentInIdIsIgnored() {
		recordStorage = new RecordStorageCreateUpdateSpy();
		setUpDependencyProvider();
		recordTypeHandlerSpy.shouldAutoGenerateId = true;
		DataGroup record = DataCreator2.createRecordWithNameInDataAndLinkedRecordId(
				"typeWithAutoGeneratedIdWrongRecordInfo", "cora");

		DataGroupOldSpy createdRecordInfo = (DataGroupOldSpy) record
				.getFirstGroupWithNameInData("recordInfo");
		createdRecordInfo.addChild(new DataAtomicSpy("id", "someIdThatShouldNotBeHere"));
		DataRecord recordOut = recordCreator.createAndStoreRecord("someToken78678567",
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
		recordStorage = new OldRecordStorageSpy();
		setUpDependencyProvider();
		DataGroup record = DataCreator2.createRecordWithNameInDataAndIdAndLinkedRecordId(
				"testingRecordInfo", "someId", "cora");

		DataRecord recordOut = recordCreator.createAndStoreRecord("someToken78678567", "spyType2",
				record);
		DataGroup groupOut = recordOut.getDataGroup();
		DataGroup recordInfo = groupOut.getFirstGroupWithNameInData("recordInfo");
		String recordId = recordInfo.getFirstAtomicValueWithNameInData("id");
		assertEquals(recordId, "someId");

		assertCorrectUserInfoInRecordInfo(recordInfo);

		String tsCreated = recordInfo.getFirstAtomicValueWithNameInData("tsCreated");
		// assertEquals(tsCreated, "");
		assertTrue(tsCreated.matches(TIMESTAMP_FORMAT));

		DataGroup updated = recordInfo.getFirstGroupWithNameInData("updated");
		String tsUpdated = updated.getFirstAtomicValueWithNameInData("tsUpdated");
		assertTrue(tsUpdated.matches(TIMESTAMP_FORMAT));
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

		DataGroup updated = recordInfo.getFirstGroupWithNameInData("updated");
		assertEquals(updated.getRepeatId(), "0");
		dataFactorySpy.MCR.assertParameters("factorRecordLinkUsingNameInDataAndTypeAndId", 2,
				"updatedBy", "user", "12345");
		var updatedByLink = dataFactorySpy.MCR
				.getReturnValue("factorRecordLinkUsingNameInDataAndTypeAndId", 2);
		assertDataChildFoundInChildren(updatedByLink, updated.getChildren());
	}

	@Test
	public void testCreateRecordUserSuppliedId() {
		recordStorage = new RecordStorageCreateUpdateSpy();
		setUpDependencyProvider();

		DataGroup record = DataCreator2.createRecordWithNameInDataAndIdAndLinkedRecordId(
				"typeWithUserGeneratedId", "somePlace", "cora");

		DataRecord recordOut = recordCreator.createAndStoreRecord("someToken78678567",
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

		DataGroup groupCreated = ((RecordStorageCreateUpdateSpy) recordStorage).createRecord;
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
		recordStorage = new RecordStorageCreateUpdateSpy();
		setUpDependencyProvider();

		DataGroup record = DataCreator2.createRecordWithNameInDataAndIdAndLinkedRecordId(
				"typeWithUserGeneratedId", "somePlace", "cora");

		recordCreator.createAndStoreRecord("someToken78678567", "typeWithUserGeneratedId", record);

		assertEquals(((RecordStorageCreateUpdateSpy) recordStorage).dataDivider, "cora");
	}

	@Test
	public void testCreateRecordCollectedTermsSentAlong() {
		recordStorage = new RecordStorageCreateUpdateSpy();
		setUpDependencyProvider();

		recordTypeHandlerSpy.shouldAutoGenerateId = true;

		DataGroup record = DataCreator2.createRecordWithNameInDataAndIdAndLinkedRecordId(
				"typeWithUserGeneratedId", "somePlace", "cora");

		recordCreator.createAndStoreRecord("someToken78678567", "typeWithUserGeneratedId", record);

		assertEquals(((RecordStorageCreateUpdateSpy) recordStorage).dataDivider, "cora");

		termCollector.MCR.assertParameter("collectTerms", 0, "metadataId",
				"fakeMetadataIdFromRecordTypeHandlerSpy");
		CollectTerms collectTerms = (CollectTerms) termCollector.MCR.getReturnValue("collectTerms",
				0);
		assertEquals(((RecordStorageCreateUpdateSpy) recordStorage).storageTerms,
				collectTerms.storageTerms);
	}

	@Test(expectedExceptions = AuthorizationException.class)
	public void testCreateRecordUnauthorizedForActionCreate() {
		spiderAuthorizator.authorizedForActionAndRecordType = false;
		DataGroup record = DataCreator2.createRecordWithNameInDataAndLinkedRecordId("authority",
				"cora");

		recordCreator.createAndStoreRecord("someToken78678567", "place", record);
	}

	@Test
	public void testUnauthorizedForCreateOnRecordTypeShouldShouldNotAccessStorage() {
		OldRecordStorageSpy oldRecordStorage = new OldRecordStorageSpy();
		recordStorage = oldRecordStorage;
		spiderAuthorizator.authorizedForActionAndRecordType = false;
		setUpDependencyProvider();

		DataGroup record = DataCreator2.createRecordWithNameInDataAndLinkedRecordId("spyType",
				"cora");
		boolean exceptionWasCaught = false;
		try {
			recordCreator.createAndStoreRecord("someToken78678567", "spyType", record);
		} catch (Exception e) {
			assertEquals(e.getClass(), AuthorizationException.class);
			exceptionWasCaught = true;
		}
		assertTrue(exceptionWasCaught);
		assertFalse(oldRecordStorage.readWasCalled);
		assertFalse(oldRecordStorage.updateWasCalled);
		assertFalse(oldRecordStorage.deleteWasCalled);
		assertFalse(oldRecordStorage.createWasCalled);
		extendedFunctionalityProvider.MCR.assertNumberOfCallsToMethod(
				"getFunctionalityForCreateBeforeMetadataValidation", 0);
	}

	@Test(expectedExceptions = AuthorizationException.class, expectedExceptionsMessageRegExp = ""
			+ "Excpetion thrown from "
			+ "checkGetUsersMatchedRecordPartPermissionsForActionOnRecordTypeAndCollectedData from Spy")
	public void testCreateRecordUnauthorizedForDataInRecord() {
		spiderAuthorizator.authorizedForActionAndRecordTypeAndCollectedData = false;
		recordTypeHandlerSpy.shouldAutoGenerateId = true;

		DataGroup record = DataCreator2.createRecordWithNameInDataAndLinkedRecordId("place",
				"cora");
		recordCreator.createAndStoreRecord("someToken78678567", "place", record);
	}

	@Test

	public void testCreateRecordCollectedDataUsedForAuthorization() {
		recordStorage = new RecordStorageCreateUpdateSpy();
		setUpDependencyProvider();

		DataGroup record = DataCreator2.createRecordWithNameInDataAndIdAndLinkedRecordId(
				"typeWithUserGeneratedId", "somePlace", "cora");

		recordCreator.createAndStoreRecord("someToken78678567", "typeWithUserGeneratedId", record);

		String methodName = "checkGetUsersMatchedRecordPartPermissionsForActionOnRecordTypeAndCollectedData";
		CollectTerms collectTerms = (CollectTerms) termCollector.MCR.getReturnValue("collectTerms",
				0);
		spiderAuthorizator.MCR.assertParameters(methodName, 0, authenticator.returnedUser, "create",
				"typeWithUserGeneratedId", collectTerms.permissionTerms);
	}

	@Test(expectedExceptions = RecordConflictException.class)
	public void testCreateRecordDuplicateUserSuppliedId() {
		recordStorage = new RecordStorageDuplicateSpy();
		setUpDependencyProvider();
		DataGroup record = DataCreator2.createRecordWithNameInDataAndIdAndLinkedRecordId("place",
				"somePlace", "cora");

		recordCreator.createAndStoreRecord("someToken78678567", "place", record);
		recordCreator.createAndStoreRecord("someToken78678567", "place", record);
	}

	@Test(expectedExceptions = DataException.class)
	public void testLinkedRecordIdDoesNotExist() {
		recordStorage = new RecordLinkTestsRecordStorage();
		((RecordLinkTestsRecordStorage) recordStorage).recordIdExistsForRecordType = false;
		linkCollector = DataCreator.getDataRecordLinkCollectorSpyWithCollectedLinkAdded();
		setUpDependencyProvider();
		DataGroup dataGroup = RecordLinkTestsDataCreator.createDataDataGroupWithLink();
		dataGroup.addChild(DataCreator2.createRecordInfoWithLinkedRecordId("cora"));
		recordTypeHandlerSpy.shouldAutoGenerateId = true;

		recordCreator.createAndStoreRecord("someToken78678567", "dataWithLinks", dataGroup);
	}

	@Test
	public void testLinkedRecordIdExists() {
		recordStorage = new RecordLinkTestsRecordStorage();
		RecordLinkTestsRecordStorage recordLinkTestsRecordStorage = (RecordLinkTestsRecordStorage) recordStorage;
		recordLinkTestsRecordStorage.recordIdExistsForRecordType = true;
		linkCollector = DataCreator.getDataRecordLinkCollectorSpyWithCollectedLinkAdded();
		setUpDependencyProvider();
		DataGroup dataGroup = RecordLinkTestsDataCreator.createDataDataGroupWithLink();
		dataGroup.addChild(DataCreator2.createRecordInfoWithLinkedRecordId("cora"));
		recordTypeHandlerSpy.shouldAutoGenerateId = true;

		recordCreator.createAndStoreRecord("someToken78678567", "dataWithLinks", dataGroup);

		assertTrue(recordLinkTestsRecordStorage.createWasRead);
		assertEquals(recordLinkTestsRecordStorage.type, "toRecordType");
		assertEquals(recordLinkTestsRecordStorage.id, "toRecordId");
	}

	@Test
	public void testReturnRecordWithoutReadActionIfUserHasCreateButNotReadAccess() {
		DataGroup dataGroup = setupForAutoGenerateId();
		dataGroupToRecordEnhancer.addReadAction = false;

		DataRecord createdRecord = recordCreator.createAndStoreRecord("dummyAuthenticatedToken",
				"spyType", dataGroup);

		dataGroupToRecordEnhancer.MCR.assertReturn("enhanceIgnoringReadAccess", 0, createdRecord);
	}

	@Test
	public void testRedactorNotCalledWhenNoRecordPartConstraints() throws Exception {
		DataGroup dataGroup = setupForNoRecordPartConstraints();

		recordCreator.createAndStoreRecord("dummyAuthenticatedToken", "spyType", dataGroup);

		recordTypeHandlerSpy.MCR.assertMethodWasCalled("hasRecordPartCreateConstraint");
		dataRedactor.MCR.assertMethodNotCalled("removeChildrenForConstraintsWithoutPermissions");
	}

	private DataGroup setupForNoRecordPartConstraints() {
		DataGroup dataGroup = setupForAutoGenerateId();
		recordTypeHandlerSpy.recordPartConstraint = "";
		return dataGroup;
	}

	@Test
	public void testRedactorCalledCorrectlyWhenRecordPartConstraints() throws Exception {
		DataGroup dataGroup = setupForRecordPartConstraints();

		recordCreator.createAndStoreRecord("dummyAuthenticatedToken", "spyType", dataGroup);

		recordTypeHandlerSpy.MCR.assertMethodWasCalled("hasRecordPartCreateConstraint");

		assertDataRedactorRemoveChildrenForConstraintsWithoutPermissions(dataGroup);
	}

	private DataGroup setupForRecordPartConstraints() {
		DataGroup dataGroup = setupForAutoGenerateId();
		DataGroup dataGroupReturnedFromRedactor = DataCreator2
				.createRecordWithNameInDataAndLinkedRecordId("nameInData", "cora");
		dataRedactor.returnDataGroup = dataGroupReturnedFromRedactor;
		recordTypeHandlerSpy.recordPartConstraint = "write";
		return dataGroup;
	}

	private DataGroup setupForAutoGenerateId() {
		DataGroup dataGroup = setupRecordStorageAndDataGroup();
		recordTypeHandlerSpy.shouldAutoGenerateId = true;
		return dataGroup;

	}

	private void assertDataRedactorRemoveChildrenForConstraintsWithoutPermissions(
			DataGroup dataGroup) {
		Set<?> recordPartWriteConstraints = (Set<?>) recordTypeHandlerSpy.MCR
				.getReturnValue("getRecordPartCreateWriteConstraints", 0);
		Set<?> writePermissions = (Set<?>) spiderAuthorizator.MCR.getReturnValue(
				"checkGetUsersMatchedRecordPartPermissionsForActionOnRecordTypeAndCollectedData",
				0);
		dataRedactor.MCR.assertParameters("removeChildrenForConstraintsWithoutPermissions", 0,
				recordTypeHandlerSpy.getMetadataId(), dataGroup, recordPartWriteConstraints,
				writePermissions);
	}

	@Test
	public void testValidateIsNotCalledWithRedactedDataGroupWhenNoRecordPartConstraints()
			throws Exception {
		DataGroup dataGroup = setupForNoRecordPartConstraints();

		recordCreator.createAndStoreRecord("dummyAuthenticatedToken", "spyType", dataGroup);

		String newMetadataId = (String) recordTypeHandlerSpy.MCR.getReturnValue("getNewMetadataId",
				0);

		dataValidator.MCR.assertParameters("validateData", 0, newMetadataId, dataGroup);
	}

	@Test
	public void testValidateIsCalledWithRedactedDataGroupWhenRecordPartConstraints()
			throws Exception {
		DataGroup dataGroup = setupForRecordPartConstraints();

		recordCreator.createAndStoreRecord("dummyAuthenticatedToken", "spyType", dataGroup);

		DataGroup redactedDataGroup = (DataGroup) dataRedactor.MCR
				.getReturnValue("removeChildrenForConstraintsWithoutPermissions", 0);

		String newMetadataId = (String) recordTypeHandlerSpy.MCR.getReturnValue("getNewMetadataId",
				0);

		dataValidator.MCR.assertParameters("validateData", 0, newMetadataId, redactedDataGroup);
	}

	@Test
	public void testStoreInArchiveTrue() throws Exception {
		recordStorage = new RecordStorageOldSpy();
		setUpDependencyProvider();
		recordTypeHandlerSpy.storeInArchive = true;
		DataGroupSpy recordSpy = createDataGroupForCreate();

		recordCreator.createAndStoreRecord("dummyAuthenticatedToken", "spyType", recordSpy);

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
		recordStorage = new RecordStorageOldSpy();
		setUpDependencyProvider();
		recordTypeHandlerSpy.storeInArchive = false;
		DataGroupSpy recordSpy = createDataGroupForCreate();

		recordCreator.createAndStoreRecord("dummyAuthenticatedToken", "spyType", recordSpy);

		recordArchive.MCR.assertMethodNotCalled("create");
	}
}