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

package se.uu.ub.cora.spider.record;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertNotNull;
import static org.testng.Assert.assertTrue;

import java.util.HashMap;
import java.util.List;

import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import se.uu.ub.cora.beefeater.authentication.User;
import se.uu.ub.cora.bookkeeper.linkcollector.DataRecordLinkCollector;
import se.uu.ub.cora.data.DataAtomicProvider;
import se.uu.ub.cora.data.DataElement;
import se.uu.ub.cora.data.DataGroup;
import se.uu.ub.cora.data.DataGroupFactory;
import se.uu.ub.cora.data.DataGroupProvider;
import se.uu.ub.cora.data.DataRecord;
import se.uu.ub.cora.data.DataRecordFactory;
import se.uu.ub.cora.data.DataRecordLink;
import se.uu.ub.cora.data.DataRecordLinkFactory;
import se.uu.ub.cora.data.DataRecordLinkProvider;
import se.uu.ub.cora.data.DataRecordProvider;
import se.uu.ub.cora.data.copier.DataCopierFactory;
import se.uu.ub.cora.data.copier.DataCopierProvider;
import se.uu.ub.cora.logger.LoggerProvider;
import se.uu.ub.cora.search.RecordIndexer;
import se.uu.ub.cora.spider.authentication.AuthenticationException;
import se.uu.ub.cora.spider.authentication.AuthenticatorSpy;
import se.uu.ub.cora.spider.authorization.AuthorizationException;
import se.uu.ub.cora.spider.authorization.PermissionRuleCalculator;
import se.uu.ub.cora.spider.data.DataAtomicFactorySpy;
import se.uu.ub.cora.spider.data.DataAtomicSpy;
import se.uu.ub.cora.spider.data.DataGroupFactorySpy;
import se.uu.ub.cora.spider.data.DataGroupSpy;
import se.uu.ub.cora.spider.data.DataRecordFactorySpy;
import se.uu.ub.cora.spider.dependency.RecordIdGeneratorProviderSpy;
import se.uu.ub.cora.spider.dependency.RecordStorageProviderSpy;
import se.uu.ub.cora.spider.dependency.SpiderDependencyProviderSpy;
import se.uu.ub.cora.spider.extended.ExtendedFunctionalityProviderSpy;
import se.uu.ub.cora.spider.extended.ExtendedFunctionalitySpy;
import se.uu.ub.cora.spider.log.LoggerFactorySpy;
import se.uu.ub.cora.spider.spy.DataGroupTermCollectorSpy;
import se.uu.ub.cora.spider.spy.DataRecordLinkCollectorSpy;
import se.uu.ub.cora.spider.spy.DataValidatorSpy;
import se.uu.ub.cora.spider.spy.IdGeneratorSpy;
import se.uu.ub.cora.spider.spy.OldRecordStorageSpy;
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
import se.uu.ub.cora.storage.RecordNotFoundException;
import se.uu.ub.cora.storage.RecordStorage;

public class SpiderRecordCreatorTest {
	private static final String TIMESTAMP_FORMAT = "\\d{4}-\\d{2}-\\d{2}T\\d{2}:\\d{2}:\\d{2}\\.\\d{6}Z";
	private RecordStorage recordStorage;
	private AuthenticatorSpy authenticator;
	private SpiderAuthorizatorSpy spiderAuthorizator;
	private RecordIdGenerator idGenerator;
	private PermissionRuleCalculator ruleCalculator;
	private SpiderRecordCreator recordCreator;
	private DataValidatorSpy dataValidator;
	private DataRecordLinkCollector linkCollector;
	private SpiderDependencyProviderSpy dependencyProvider;
	private ExtendedFunctionalityProviderSpy extendedFunctionalityProvider;
	private DataGroupToRecordEnhancerSpy dataGroupToRecordEnhancer;
	private DataGroupTermCollectorSpy termCollector;
	private RecordIndexer recordIndexer;
	private LoggerFactorySpy loggerFactorySpy;
	private DataGroupFactory dataGroupFactory;
	private DataAtomicFactorySpy dataAtomicFactory;
	private DataRecordLinkFactory dataRecordLinkFactory;
	private DataCopierFactory dataCopierFactory;
	private DataRecordFactory dataRecordFactorySpy;

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
		setUpDependencyProvider();
	}

	private void setUpFactoriesAndProviders() {
		loggerFactorySpy = new LoggerFactorySpy();
		LoggerProvider.setLoggerFactory(loggerFactorySpy);
		dataGroupFactory = new DataGroupFactorySpy();
		DataGroupProvider.setDataGroupFactory(dataGroupFactory);
		dataAtomicFactory = new DataAtomicFactorySpy();
		DataAtomicProvider.setDataAtomicFactory(dataAtomicFactory);
		dataCopierFactory = new DataCopierFactorySpy();
		DataCopierProvider.setDataCopierFactory(dataCopierFactory);
		dataRecordLinkFactory = new DataRecordLinkFactorySpy();
		DataRecordLinkProvider.setDataRecordLinkFactory(dataRecordLinkFactory);
		dataRecordFactorySpy = new DataRecordFactorySpy();
		DataRecordProvider.setDataRecordFactory(dataRecordFactorySpy);
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
		dependencyProvider.termCollector = termCollector;
		dependencyProvider.recordIndexer = recordIndexer;
		dataGroupToRecordEnhancer = new DataGroupToRecordEnhancerSpy();
		recordCreator = SpiderRecordCreatorImp.usingDependencyProviderAndDataGroupToRecordEnhancer(
				dependencyProvider, dataGroupToRecordEnhancer);
	}

	@Test
	public void testExternalDependenciesAreCalled() {
		DataGroup dataGroup = setupRecordStorageAndDataGroup();

		recordCreator.createAndStoreRecord("dummyAuthenticatedToken", "spyType", dataGroup);

		assertForExternalDependenciesAreCalled(dataGroup);
	}

	private DataGroup setupRecordStorageAndDataGroup() {
		recordStorage = new OldRecordStorageSpy();
		setUpDependencyProvider();
		DataGroup dataGroup = DataCreator2.createRecordWithNameInDataAndLinkedRecordId("nameInData",
				"cora");
		return dataGroup;
	}

	private void assertForExternalDependenciesAreCalled(DataGroup dataGroup) {
		assertTrue(authenticator.authenticationWasCalled);
		spiderAuthorizator.MCR.assertMethodWasCalled("checkUserIsAuthorizedForActionOnRecordType");
		dataValidator.MCR.assertMethodWasCalled("validateData");

		ExtendedFunctionalitySpy extendedFunctionality = extendedFunctionalityProvider.fetchedFunctionalityForCreateAfterMetadataValidation
				.get(0);
		assertTrue(extendedFunctionality.extendedFunctionalityHasBeenCalled);
		assertTrue(((OldRecordStorageSpy) recordStorage).createWasCalled);
		assertTrue(((IdGeneratorSpy) idGenerator).getIdForTypeWasCalled);
		assertTrue(((DataRecordLinkCollectorSpy) linkCollector).collectLinksWasCalled);
		assertEquals(((DataRecordLinkCollectorSpy) linkCollector).metadataId, "spyTypeNew");

		termCollector.MCR.assertParameters("collectTerms", 0, "spyTypeNew", dataGroup);
		assertEquals(((RecordIndexerSpy) recordIndexer).recordIndexData,
				termCollector.MCR.getReturnValue("collectTerms", 0));
	}

	@Test
	public void testRecordEnhancerCalled() {
		DataGroup dataGroup = setupRecordStorageAndDataGroup();

		recordCreator.createAndStoreRecord("dummyAuthenticatedToken", "spyType", dataGroup);

		assertCallEnhanceIgnoringReadAccess(dataGroup);
	}

	private void assertCallEnhanceIgnoringReadAccess(DataGroup dataGroup) {
		User user = (User) authenticator.MCR.getReturnValue("getUserForToken", 0);
		dataGroupToRecordEnhancer.MCR.assertParameters("enhanceIgnoringReadAccess", 0, user,
				"spyType", dataGroup);
	}

	@Test(expectedExceptions = AuthenticationException.class)
	public void testGetActiveUserFails() {
		authenticator.throwAuthenticationException = true;

		DataGroup dataGroup = setupRecordStorageAndDataGroup();

		recordCreator.createAndStoreRecord("dummyNonAuthenticatedToken", "spyType", dataGroup);
	}

	@Test
	public void testExtendedFunctionallityIsCalled() {
		DataGroup dataGroup = setupRecordStorageAndDataGroup();

		String authToken = "someToken78678567";
		recordCreator.createAndStoreRecord(authToken, "spyType", dataGroup);

		assertFetchedFunctionalityHasBeenCalled(authToken,
				extendedFunctionalityProvider.fetchedFunctionalityForCreateBeforeMetadataValidation);
		assertFetchedFunctionalityHasBeenCalled(authToken,
				extendedFunctionalityProvider.fetchedFunctionalityForCreateAfterMetadataValidation);
		assertFetchedFunctionalityHasBeenCalled(authToken,
				extendedFunctionalityProvider.fetchedFunctionalityForCreateBeforeReturn);
	}

	@Test
	public void testIndexerIsCalled() {
		DataGroup dataGroup = setupRecordStorageAndDataGroup();

		String authToken = "someToken78678567";

		recordCreator.createAndStoreRecord(authToken, "spyType", dataGroup);

		DataGroup createdRecord = ((OldRecordStorageSpy) recordStorage).createRecord;
		RecordIndexerSpy recordIndexerSpy = (RecordIndexerSpy) recordIndexer;
		DataGroup indexedRecord = recordIndexerSpy.record;
		assertEquals(indexedRecord, createdRecord);

		List<String> ids = recordIndexerSpy.ids;
		assertEquals(ids.get(0), "spyType_1");
		assertEquals(ids.size(), 1);
	}

	@Test
	public void testIndexerIsCalledForChildOfAbstract() {
		recordStorage = new RecordStorageCreateUpdateSpy();
		// ruleCalculator = new RuleCalculatorSpy();
		setUpDependencyProvider();
		DataGroup dataGroup = getDataGroupForImageToCreate();

		String authToken = "someToken78678567";
		recordCreator.createAndStoreRecord(authToken, "image", dataGroup);

		DataGroup createdRecord = ((RecordStorageCreateUpdateSpy) recordStorage).createRecord;
		RecordIndexerSpy recordIndexerSpy = (RecordIndexerSpy) recordIndexer;
		DataGroup indexedRecord = recordIndexerSpy.record;
		assertEquals(indexedRecord, createdRecord);

		List<String> ids = recordIndexerSpy.ids;
		assertEquals(ids.get(0), "image_someImage");
		assertEquals(ids.get(1), "binary_someImage");
		assertEquals(ids.size(), 2);
	}

	private DataGroup getDataGroupForImageToCreate() {
		DataGroup dataGroup = new DataGroupSpy("binary");
		DataGroup createRecordInfo = DataCreator2
				.createRecordInfoWithIdAndLinkedRecordId("someImage", "cora");
		dataGroup.addChild(createRecordInfo);
		dataGroup.addChild(new DataAtomicSpy("atomicId", "atomicValue"));
		return dataGroup;
	}

	private void assertFetchedFunctionalityHasBeenCalled(String authToken,
			List<ExtendedFunctionalitySpy> fetchedFunctionalityForCreateAfterMetadataValidation) {
		ExtendedFunctionalitySpy extendedFunctionality = fetchedFunctionalityForCreateAfterMetadataValidation
				.get(0);
		assertEquals(extendedFunctionality.token, authToken);
		assertTrue(extendedFunctionality.extendedFunctionalityHasBeenCalled);
		ExtendedFunctionalitySpy extendedFunctionality2 = fetchedFunctionalityForCreateAfterMetadataValidation
				.get(0);
		assertEquals(extendedFunctionality2.token, authToken);
		assertTrue(extendedFunctionality2.extendedFunctionalityHasBeenCalled);
	}

	@Test(expectedExceptions = DataException.class)
	public void testCreateRecordInvalidData() {
		dataValidator.validValidation = false;

		DataGroup dataGroup = new DataGroupSpy("nameInData");

		recordCreator.createAndStoreRecord("someToken78678567", "recordType", dataGroup);
	}

	@Test
	public void testCreateRecordAutogeneratedId() {
		recordStorage = new RecordStorageCreateUpdateSpy();
		setUpDependencyProvider();
		DataGroup record = DataCreator2
				.createRecordWithNameInDataAndLinkedRecordId("typeWithAutoGeneratedId", "cora");

		DataRecord recordOut = recordCreator.createAndStoreRecord("someToken78678567",
				"typeWithAutoGeneratedId", record);
		DataGroup groupOut = recordOut.getDataGroup();
		DataGroup recordInfo = groupOut.getFirstGroupWithNameInData("recordInfo");
		String recordId = recordInfo.getFirstAtomicValueWithNameInData("id");
		assertNotNull(recordId);

		DataRecordLink createdByGroup = (DataRecordLink) recordInfo
				.getFirstGroupWithNameInData("createdBy");
		assertEquals(createdByGroup.getFirstAtomicValueWithNameInData("linkedRecordType"), "user");
		assertEquals(createdByGroup.getFirstAtomicValueWithNameInData("linkedRecordId"), "12345");

		DataGroup typeGroup = recordInfo.getFirstGroupWithNameInData("type");
		assertEquals(typeGroup.getFirstAtomicValueWithNameInData("linkedRecordType"), "recordType");
		assertEquals(typeGroup.getFirstAtomicValueWithNameInData("linkedRecordId"),
				"typeWithAutoGeneratedId");

		DataGroup groupCreated = ((RecordStorageCreateUpdateSpy) recordStorage).createRecord;
		assertEquals(groupOut.getNameInData(), groupCreated.getNameInData(),
				"Returned and read record should have the same nameInData");
	}

	@Test
	public void testAutogeneratedIdSentToStorageUsingGeneratedId() {
		recordStorage = new RecordStorageCreateUpdateSpy();
		setUpDependencyProvider();
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
		DataGroup record = DataCreator2.createRecordWithNameInDataAndLinkedRecordId(
				"typeWithAutoGeneratedIdWrongRecordInfo", "cora");

		DataGroup createdRecordInfo = record.getFirstGroupWithNameInData("recordInfo");
		createdRecordInfo.addChild(new DataAtomicSpy("id", "someIdThatShouldNotBeHere"));
		DataRecord recordOut = recordCreator.createAndStoreRecord("someToken78678567",
				"typeWithAutoGeneratedIdWrongRecordInfo", record);
		DataGroup groupOut = recordOut.getDataGroup();
		DataGroup recordInfo = groupOut.getFirstGroupWithNameInData("recordInfo");
		int numOfIds = 0;
		for (DataElement dataElement : recordInfo.getChildren()) {
			if ("id".equals(dataElement.getNameInData())) {
				numOfIds++;
			}
		}
		assertEquals(numOfIds, 1);
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

		DataGroup typeGroup = recordInfo.getFirstGroupWithNameInData("type");
		assertEquals(typeGroup.getFirstAtomicValueWithNameInData("linkedRecordType"), "recordType");
		assertEquals(typeGroup.getFirstAtomicValueWithNameInData("linkedRecordId"), "spyType2");
	}

	private void assertCorrectUserInfoInRecordInfo(DataGroup recordInfo) {
		DataRecordLink createdByGroup = (DataRecordLink) recordInfo
				.getFirstGroupWithNameInData("createdBy");
		assertEquals(createdByGroup.getFirstAtomicValueWithNameInData("linkedRecordType"), "user");
		assertEquals(createdByGroup.getFirstAtomicValueWithNameInData("linkedRecordId"), "12345");

		DataGroup updated = recordInfo.getFirstGroupWithNameInData("updated");
		assertEquals(updated.getRepeatId(), "0");
		DataRecordLink updatedByGroup = (DataRecordLink) updated
				.getFirstGroupWithNameInData("updatedBy");

		assertEquals(updatedByGroup.getFirstAtomicValueWithNameInData("linkedRecordType"), "user");
		assertEquals(updatedByGroup.getFirstAtomicValueWithNameInData("linkedRecordId"), "12345");
		assertFalse(recordInfo.containsChildWithNameInData("updatedBy"));
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

		DataRecordLink createdByGroup = (DataRecordLink) recordInfo
				.getFirstGroupWithNameInData("createdBy");
		assertEquals(createdByGroup.getFirstAtomicValueWithNameInData("linkedRecordId"), "12345");
		DataGroup typeGroup = recordInfo.getFirstGroupWithNameInData("type");
		assertEquals(typeGroup.getFirstAtomicValueWithNameInData("linkedRecordId"),
				"typeWithUserGeneratedId");

		DataGroup groupCreated = ((RecordStorageCreateUpdateSpy) recordStorage).createRecord;
		assertEquals(groupOut.getNameInData(), groupCreated.getNameInData(),
				"Returned and read record should have the same nameInData");
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
		DataGroup record = DataCreator2.createRecordWithNameInDataAndIdAndLinkedRecordId(
				"typeWithUserGeneratedId", "somePlace", "cora");

		recordCreator.createAndStoreRecord("someToken78678567", "typeWithUserGeneratedId", record);

		assertEquals(((RecordStorageCreateUpdateSpy) recordStorage).dataDivider, "cora");

		termCollector.MCR.assertParameter("collectTerms", 0, "metadataId", "place");
		assertEquals(((RecordStorageCreateUpdateSpy) recordStorage).collectedTerms,
				termCollector.MCR.getReturnValue("collectTerms", 0));
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
		recordStorage = new OldRecordStorageSpy();
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
		assertFalse(((OldRecordStorageSpy) recordStorage).readWasCalled);
		assertFalse(((OldRecordStorageSpy) recordStorage).updateWasCalled);
		assertFalse(((OldRecordStorageSpy) recordStorage).deleteWasCalled);
		assertFalse(((OldRecordStorageSpy) recordStorage).createWasCalled);
		assertEquals(
				extendedFunctionalityProvider.fetchedFunctionalityForCreateBeforeMetadataValidation
						.size(),
				0);
	}

	@Test(expectedExceptions = AuthorizationException.class, expectedExceptionsMessageRegExp = ""
			+ "Excpetion thrown from checkUserIsAuthorizedForActionOnRecordTypeAndCollectedData from Spy")
	public void testCreateRecordUnauthorizedForDataInRecord() {
		spiderAuthorizator.authorizedForActionAndRecordTypeAndCollectedData = false;

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

		String methodName = "checkUserIsAuthorizedForActionOnRecordTypeAndCollectedData";
		spiderAuthorizator.MCR.assertParameters(methodName, 0, authenticator.returnedUser, "create",
				"typeWithUserGeneratedId", termCollector.MCR.getReturnValue("collectTerms", 0));
	}

	@Test(expectedExceptions = RecordNotFoundException.class)
	public void testNonExistingRecordType() {
		DataGroup record = new DataGroupSpy("authority");
		recordCreator.createAndStoreRecord("someToken78678567", "recordType_NOT_EXISTING", record);
	}

	@Test(expectedExceptions = MisuseException.class)
	public void testCreateRecordAbstractRecordType() {
		recordStorage = new OldRecordStorageSpy();
		setUpDependencyProvider();

		DataGroup record = new DataGroupSpy("abstract");
		recordCreator.createAndStoreRecord("someToken78678567", "abstract", record);
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

		recordCreator.createAndStoreRecord("someToken78678567", "dataWithLinks", dataGroup);

		assertTrue(recordLinkTestsRecordStorage.createWasRead);
		assertEquals(recordLinkTestsRecordStorage.type, "toRecordType");
		assertEquals(recordLinkTestsRecordStorage.id, "toRecordId");
	}

	@Test
	public void testReturnRecordWithoutReadActionIfUserHasCreateButNotReadAccess() {
		DataGroup dataGroup = setupRecordStorageAndDataGroup();
		dataGroupToRecordEnhancer.addReadAction = false;

		DataRecord createdRecord = recordCreator.createAndStoreRecord("dummyAuthenticatedToken",
				"spyType", dataGroup);

		dataGroupToRecordEnhancer.MCR.assertReturn("enhanceIgnoringReadAccess", 0, createdRecord);
	}
}
