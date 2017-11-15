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
import java.util.HashSet;
import java.util.List;

import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import se.uu.ub.cora.bookkeeper.data.DataGroup;
import se.uu.ub.cora.bookkeeper.linkcollector.DataRecordLinkCollector;
import se.uu.ub.cora.bookkeeper.termcollector.DataGroupTermCollector;
import se.uu.ub.cora.bookkeeper.validator.DataValidator;
import se.uu.ub.cora.spider.authentication.AuthenticationException;
import se.uu.ub.cora.spider.authentication.Authenticator;
import se.uu.ub.cora.spider.authentication.AuthenticatorSpy;
import se.uu.ub.cora.spider.authorization.AlwaysAuthorisedExceptStub;
import se.uu.ub.cora.spider.authorization.AuthorizationException;
import se.uu.ub.cora.spider.authorization.NeverAuthorisedStub;
import se.uu.ub.cora.spider.authorization.PermissionRuleCalculator;
import se.uu.ub.cora.spider.authorization.SpiderAuthorizator;
import se.uu.ub.cora.spider.data.SpiderDataAtomic;
import se.uu.ub.cora.spider.data.SpiderDataGroup;
import se.uu.ub.cora.spider.data.SpiderDataRecord;
import se.uu.ub.cora.spider.dependency.SpiderDependencyProviderSpy;
import se.uu.ub.cora.spider.extended.ExtendedFunctionalityProviderSpy;
import se.uu.ub.cora.spider.extended.ExtendedFunctionalitySpy;
import se.uu.ub.cora.spider.record.storage.RecordConflictException;
import se.uu.ub.cora.spider.record.storage.RecordIdGenerator;
import se.uu.ub.cora.spider.record.storage.RecordNotFoundException;
import se.uu.ub.cora.spider.record.storage.RecordStorage;
import se.uu.ub.cora.spider.record.storage.TimeStampIdGenerator;
import se.uu.ub.cora.spider.search.RecordIndexer;
import se.uu.ub.cora.spider.spy.AuthorizatorAlwaysAuthorizedSpy;
import se.uu.ub.cora.spider.spy.DataGroupSearchTermCollectorSpy;
import se.uu.ub.cora.spider.spy.DataRecordLinkCollectorSpy;
import se.uu.ub.cora.spider.spy.DataValidatorAlwaysInvalidSpy;
import se.uu.ub.cora.spider.spy.DataValidatorAlwaysValidSpy;
import se.uu.ub.cora.spider.spy.IdGeneratorSpy;
import se.uu.ub.cora.spider.spy.NoRulesCalculatorStub;
import se.uu.ub.cora.spider.spy.RecordIndexerSpy;
import se.uu.ub.cora.spider.spy.RecordStorageCreateUpdateSpy;
import se.uu.ub.cora.spider.spy.RecordStorageSpy;
import se.uu.ub.cora.spider.spy.RuleCalculatorSpy;
import se.uu.ub.cora.spider.testdata.DataCreator;
import se.uu.ub.cora.spider.testdata.RecordLinkTestsDataCreator;
import se.uu.ub.cora.spider.testdata.TestDataRecordInMemoryStorage;

public class SpiderRecordCreatorTest {
	private RecordStorage recordStorage;
	private Authenticator authenticator;
	private SpiderAuthorizator spiderAuthorizator;
	private RecordIdGenerator idGenerator;
	private PermissionRuleCalculator ruleCalculator;
	private SpiderRecordCreator recordCreator;
	private DataValidator dataValidator;
	private DataRecordLinkCollector linkCollector;
	private SpiderDependencyProviderSpy dependencyProvider;
	private ExtendedFunctionalityProviderSpy extendedFunctionalityProvider;
	private DataGroupToRecordEnhancerSpy dataGroupToRecordEnhancer;
	private DataGroupTermCollector searchTermCollector;
	private RecordIndexer recordIndexer;

	@BeforeMethod
	public void beforeMethod() {
		authenticator = new AuthenticatorSpy();
		spiderAuthorizator = new AuthorizatorAlwaysAuthorizedSpy();
		dataValidator = new DataValidatorAlwaysValidSpy();
		recordStorage = TestDataRecordInMemoryStorage.createRecordStorageInMemoryWithTestData();
		idGenerator = new TimeStampIdGenerator();
		ruleCalculator = new NoRulesCalculatorStub();
		linkCollector = new DataRecordLinkCollectorSpy();
		extendedFunctionalityProvider = new ExtendedFunctionalityProviderSpy();
		searchTermCollector = new DataGroupSearchTermCollectorSpy();
		recordIndexer = new RecordIndexerSpy();
		setUpDependencyProvider();
	}

	private void setUpDependencyProvider() {
		dependencyProvider = new SpiderDependencyProviderSpy(new HashMap<>());
		dependencyProvider.authenticator = authenticator;
		dependencyProvider.spiderAuthorizator = spiderAuthorizator;
		dependencyProvider.dataValidator = dataValidator;
		dependencyProvider.recordStorage = recordStorage;
		dependencyProvider.keyCalculator = ruleCalculator;
		dependencyProvider.linkCollector = linkCollector;
		dependencyProvider.idGenerator = idGenerator;
		dependencyProvider.extendedFunctionalityProvider = extendedFunctionalityProvider;
		dependencyProvider.searchTermCollector = searchTermCollector;
		dependencyProvider.recordIndexer = recordIndexer;
		dataGroupToRecordEnhancer = new DataGroupToRecordEnhancerSpy();
		recordCreator = SpiderRecordCreatorImp.usingDependencyProviderAndDataGroupToRecordEnhancer(
				dependencyProvider, dataGroupToRecordEnhancer);
	}

	@Test
	public void testExternalDependenciesAreCalled() {
		spiderAuthorizator = new AuthorizatorAlwaysAuthorizedSpy();
		dataValidator = new DataValidatorAlwaysValidSpy();
		recordStorage = new RecordStorageSpy();
		idGenerator = new IdGeneratorSpy();
		ruleCalculator = new RuleCalculatorSpy();
		setUpDependencyProvider();
		SpiderDataGroup spiderDataGroup = DataCreator
				.createRecordWithNameInDataAndLinkedRecordId("nameInData", "cora");
		recordCreator.createAndStoreRecord("dummyAuthenticatedToken", "spyType", spiderDataGroup);

		assertTrue(((AuthenticatorSpy) authenticator).authenticationWasCalled);
		assertTrue(((AuthorizatorAlwaysAuthorizedSpy) spiderAuthorizator).authorizedWasCalled);
		assertTrue(((DataValidatorAlwaysValidSpy) dataValidator).validateDataWasCalled);
		ExtendedFunctionalitySpy extendedFunctionality = extendedFunctionalityProvider.fetchedFunctionalityForCreateAfterMetadataValidation
				.get(0);
		assertTrue(extendedFunctionality.extendedFunctionalityHasBeenCalled);
		assertTrue(((RecordStorageSpy) recordStorage).createWasCalled);
		assertTrue(((IdGeneratorSpy) idGenerator).getIdForTypeWasCalled);
		assertTrue(((DataRecordLinkCollectorSpy) linkCollector).collectLinksWasCalled);
		assertEquals(((DataRecordLinkCollectorSpy) linkCollector).metadataId, "spyTypeNew");

		assertCorrectSearchTermCollectorAndIndexer(spiderDataGroup);

	}

	private void assertCorrectSearchTermCollectorAndIndexer(SpiderDataGroup spiderDataGroup) {
		DataGroupSearchTermCollectorSpy searchTermCollectorSpy = (DataGroupSearchTermCollectorSpy) searchTermCollector;
		assertEquals(searchTermCollectorSpy.metadataId, "spyTypeNew");
		assertTrue(searchTermCollectorSpy.collectSearchTermsWasCalled);
		assertEquals(((RecordIndexerSpy) recordIndexer).recordIndexData,
				searchTermCollectorSpy.collectedSearchTerms);
	}

	@Test
	public void testRecordEnhancerCalled() {
		spiderAuthorizator = new AuthorizatorAlwaysAuthorizedSpy();
		dataValidator = new DataValidatorAlwaysValidSpy();
		recordStorage = new RecordStorageSpy();
		idGenerator = new IdGeneratorSpy();
		ruleCalculator = new RuleCalculatorSpy();
		linkCollector = new DataRecordLinkCollectorSpy();
		setUpDependencyProvider();
		SpiderDataGroup spiderDataGroup = DataCreator
				.createRecordWithNameInDataAndLinkedRecordId("nameInData", "cora");
		recordCreator.createAndStoreRecord("dummyAuthenticatedToken", "spyType", spiderDataGroup);
		assertEquals(dataGroupToRecordEnhancer.user.id, "12345");
		assertEquals(dataGroupToRecordEnhancer.recordType, "spyType");
		assertEquals(dataGroupToRecordEnhancer.dataGroup.getFirstGroupWithNameInData("recordInfo")
				.getFirstAtomicValueWithNameInData("id"), "1");
	}

	@Test(expectedExceptions = AuthenticationException.class)
	public void testAuthenticationNotAuthenticated() {
		recordStorage = new RecordStorageSpy();
		setUpDependencyProvider();
		SpiderDataGroup spiderDataGroup = DataCreator
				.createRecordWithNameInDataAndLinkedRecordId("nameInData", "cora");
		recordCreator.createAndStoreRecord("dummyNonAuthenticatedToken", "spyType",
				spiderDataGroup);
	}

	@Test
	public void testExtendedFunctionallityIsCalled() {
		spiderAuthorizator = new AuthorizatorAlwaysAuthorizedSpy();
		dataValidator = new DataValidatorAlwaysValidSpy();
		recordStorage = new RecordStorageSpy();
		idGenerator = new IdGeneratorSpy();
		ruleCalculator = new RuleCalculatorSpy();
		linkCollector = new DataRecordLinkCollectorSpy();
		setUpDependencyProvider();
		SpiderDataGroup spiderDataGroup = DataCreator
				.createRecordWithNameInDataAndLinkedRecordId("nameInData", "cora");
		String authToken = "someToken78678567";
		recordCreator.createAndStoreRecord(authToken, "spyType", spiderDataGroup);

		assertFetchedFunctionalityHasBeenCalled(authToken,
				extendedFunctionalityProvider.fetchedFunctionalityForCreateBeforeMetadataValidation);
		assertFetchedFunctionalityHasBeenCalled(authToken,
				extendedFunctionalityProvider.fetchedFunctionalityForCreateAfterMetadataValidation);
		assertFetchedFunctionalityHasBeenCalled(authToken,
				extendedFunctionalityProvider.fetchedFunctionalityForCreateBeforeReturn);
	}

	@Test
	public void testIndexerIsCalled() {
		recordStorage = new RecordStorageSpy();
		idGenerator = new IdGeneratorSpy();
		ruleCalculator = new RuleCalculatorSpy();
		setUpDependencyProvider();
		SpiderDataGroup spiderDataGroup = DataCreator
				.createRecordWithNameInDataAndLinkedRecordId("nameInData", "cora");
		String authToken = "someToken78678567";
		recordCreator.createAndStoreRecord(authToken, "spyType", spiderDataGroup);

		DataGroup createdRecord = ((RecordStorageSpy) recordStorage).createRecord;
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
		ruleCalculator = new RuleCalculatorSpy();
		setUpDependencyProvider();
		SpiderDataGroup spiderDataGroup = getSpiderDataGroupForImageToCreate();
		String authToken = "someToken78678567";
		recordCreator.createAndStoreRecord(authToken, "image", spiderDataGroup);

		DataGroup createdRecord = ((RecordStorageCreateUpdateSpy) recordStorage).createRecord;
		RecordIndexerSpy recordIndexerSpy = (RecordIndexerSpy) recordIndexer;
		DataGroup indexedRecord = recordIndexerSpy.record;
		assertEquals(indexedRecord, createdRecord);

		List<String> ids = recordIndexerSpy.ids;
		assertEquals(ids.get(0), "image_someImage");
		assertEquals(ids.get(1), "binary_someImage");
		assertEquals(ids.size(), 2);
	}

	private SpiderDataGroup getSpiderDataGroupForImageToCreate() {
		SpiderDataGroup dataGroup = SpiderDataGroup.withNameInData("binary");
		SpiderDataGroup createRecordInfo = DataCreator
				.createRecordInfoWithIdAndLinkedRecordId("someImage", "cora");
		dataGroup.addChild(createRecordInfo);
		dataGroup.addChild(SpiderDataAtomic.withNameInDataAndValue("atomicId", "atomicValue"));
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
		dataValidator = new DataValidatorAlwaysInvalidSpy();
		setUpDependencyProvider();
		SpiderDataGroup spiderDataGroup = SpiderDataGroup.withNameInData("nameInData");
		recordCreator.createAndStoreRecord("someToken78678567", "recordType", spiderDataGroup);
	}

	@Test
	public void testCreateRecordAutogeneratedId() {
		recordStorage = new RecordStorageCreateUpdateSpy();
		setUpDependencyProvider();
		SpiderDataGroup record = DataCreator
				.createRecordWithNameInDataAndLinkedRecordId("typeWithAutoGeneratedId", "cora");

		SpiderDataRecord recordOut = recordCreator.createAndStoreRecord("someToken78678567",
				"typeWithAutoGeneratedId", record);
		SpiderDataGroup groupOut = recordOut.getSpiderDataGroup();
		SpiderDataGroup recordInfo = groupOut.extractGroup("recordInfo");
		String recordId = recordInfo.extractAtomicValue("id");
		assertNotNull(recordId);

		SpiderDataGroup createdByGroup = recordInfo.extractGroup("createdBy");
		assertEquals(createdByGroup.extractAtomicValue("linkedRecordType"), "user");
		assertEquals(createdByGroup.extractAtomicValue("linkedRecordId"), "12345");

		SpiderDataGroup typeGroup = recordInfo.extractGroup("type");
		assertEquals(typeGroup.extractAtomicValue("linkedRecordType"), "recordType");
		assertEquals(typeGroup.extractAtomicValue("linkedRecordId"), "typeWithAutoGeneratedId");

		DataGroup groupCreated = ((RecordStorageCreateUpdateSpy) recordStorage).createRecord;
		assertEquals(groupOut.getNameInData(), groupCreated.getNameInData(),
				"Returned and read record should have the same nameInData");
	}

	@Test
	public void testAutogeneratedIdSentToStorageUsingGeneratedId() {
		recordStorage = new RecordStorageCreateUpdateSpy();
		idGenerator = new IdGeneratorSpy();
		setUpDependencyProvider();
		SpiderDataGroup record = DataCreator
				.createRecordWithNameInDataAndLinkedRecordId("typeWithAutoGeneratedId", "cora");

		SpiderDataRecord recordOut = recordCreator.createAndStoreRecord("someToken78678567",
				"typeWithAutoGeneratedId", record);

		SpiderDataGroup groupOut = recordOut.getSpiderDataGroup();
		SpiderDataGroup recordInfo = groupOut.extractGroup("recordInfo");
		String recordId = recordInfo.extractAtomicValue("id");
		assertEquals(recordId, "1");
		assertEquals(((RecordStorageCreateUpdateSpy) recordStorage).type,
				"typeWithAutoGeneratedId");
		assertEquals(((RecordStorageCreateUpdateSpy) recordStorage).id, "1");
	}

	@Test
	public void testCorrectRecordInfoInCreatedRecord() {
		recordStorage = new RecordStorageSpy();
		setUpDependencyProvider();
		SpiderDataGroup record = DataCreator.createRecordWithNameInDataAndIdAndLinkedRecordId(
				"testingRecordInfo", "someId", "cora");

		SpiderDataRecord recordOut = recordCreator.createAndStoreRecord("someToken78678567",
				"spyType2", record);
		SpiderDataGroup groupOut = recordOut.getSpiderDataGroup();
		SpiderDataGroup recordInfo = groupOut.extractGroup("recordInfo");
		String recordId = recordInfo.extractAtomicValue("id");
		assertEquals(recordId, "someId");

		assertCorrectUserInfoInRecordInfo(recordInfo);

		String tsCreated = recordInfo.extractAtomicValue("tsCreated");
		String tsUpdated = recordInfo.extractAtomicValue("tsUpdated");
		assertTrue(tsCreated.matches("\\d{4}-\\d{2}-\\d{2}\\s\\d{2}:\\d{2}:\\d{2}\\.\\d{3}"));
		assertTrue(tsUpdated.matches("\\d{4}-\\d{2}-\\d{2}\\s\\d{2}:\\d{2}:\\d{2}\\.\\d{3}"));

		SpiderDataGroup typeGroup = recordInfo.extractGroup("type");
		assertEquals(typeGroup.extractAtomicValue("linkedRecordType"), "recordType");
		assertEquals(typeGroup.extractAtomicValue("linkedRecordId"), "spyType2");
	}

	private void assertCorrectUserInfoInRecordInfo(SpiderDataGroup recordInfo) {
		SpiderDataGroup createdByGroup = recordInfo.extractGroup("createdBy");
		assertEquals(createdByGroup.extractAtomicValue("linkedRecordType"), "user");
		assertEquals(createdByGroup.extractAtomicValue("linkedRecordId"), "12345");

		SpiderDataGroup updatedByGroup = recordInfo.extractGroup("updatedBy");
		assertEquals(updatedByGroup.extractAtomicValue("linkedRecordType"), "user");
		assertEquals(updatedByGroup.extractAtomicValue("linkedRecordId"), "12345");
	}

	@Test
	public void testCreateRecordUserSuppliedId() {
		recordStorage = new RecordStorageCreateUpdateSpy();
		setUpDependencyProvider();

		SpiderDataGroup record = DataCreator.createRecordWithNameInDataAndIdAndLinkedRecordId(
				"typeWithUserGeneratedId", "somePlace", "cora");

		SpiderDataRecord recordOut = recordCreator.createAndStoreRecord("someToken78678567",
				"typeWithUserGeneratedId", record);
		SpiderDataGroup groupOut = recordOut.getSpiderDataGroup();
		SpiderDataGroup recordInfo = groupOut.extractGroup("recordInfo");
		String recordId = recordInfo.extractAtomicValue("id");
		assertNotNull(recordId, "A new record should have an id");

		SpiderDataGroup createdByGroup = recordInfo.extractGroup("createdBy");
		assertEquals(createdByGroup.extractAtomicValue("linkedRecordId"), "12345");
		SpiderDataGroup typeGroup = recordInfo.extractGroup("type");
		assertEquals(typeGroup.extractAtomicValue("linkedRecordId"), "typeWithUserGeneratedId");

		DataGroup groupCreated = ((RecordStorageCreateUpdateSpy) recordStorage).createRecord;
		assertEquals(groupOut.getNameInData(), groupCreated.getNameInData(),
				"Returned and read record should have the same nameInData");
	}

	@Test
	public void testCreateRecordDataDividerExtractedFromData() {
		recordStorage = new RecordStorageCreateUpdateSpy();
		setUpDependencyProvider();

		SpiderDataGroup record = DataCreator.createRecordWithNameInDataAndIdAndLinkedRecordId(
				"typeWithUserGeneratedId", "somePlace", "cora");

		recordCreator.createAndStoreRecord("someToken78678567", "typeWithUserGeneratedId", record);

		assertEquals(((RecordStorageCreateUpdateSpy) recordStorage).dataDivider, "cora");
	}

	@Test(expectedExceptions = AuthorizationException.class)
	public void testCreateRecordUnauthorized() {
		spiderAuthorizator = new NeverAuthorisedStub();
		setUpDependencyProvider();

		SpiderDataGroup record = DataCreator
				.createRecordWithNameInDataAndLinkedRecordId("authority", "cora");
		recordCreator.createAndStoreRecord("someToken78678567", "place", record);
	}

	@Test
	public void testUnauthorizedForCreateOnRecordTypeShouldShouldNotAccessStorage() {
		recordStorage = new RecordStorageSpy();
		spiderAuthorizator = new AlwaysAuthorisedExceptStub();
		HashSet<String> hashSet = new HashSet<String>();
		hashSet.add("create");
		((AlwaysAuthorisedExceptStub) spiderAuthorizator).notAuthorizedForRecordTypeAndActions
				.put("spyType", hashSet);
		setUpDependencyProvider();

		SpiderDataGroup record = DataCreator.createRecordWithNameInDataAndLinkedRecordId("spyType",
				"cora");
		boolean exceptionWasCaught = false;
		try {
			recordCreator.createAndStoreRecord("someToken78678567", "spyType", record);
		} catch (Exception e) {
			assertEquals(e.getClass(), AuthorizationException.class);
			exceptionWasCaught = true;
		}
		assertTrue(exceptionWasCaught);
		assertFalse(((RecordStorageSpy) recordStorage).readWasCalled);
		assertFalse(((RecordStorageSpy) recordStorage).updateWasCalled);
		assertFalse(((RecordStorageSpy) recordStorage).deleteWasCalled);
		assertFalse(((RecordStorageSpy) recordStorage).createWasCalled);
		assertEquals(
				extendedFunctionalityProvider.fetchedFunctionalityForCreateBeforeMetadataValidation
						.size(),
				0);
	}

	@Test(expectedExceptions = AuthorizationException.class)
	public void testCreateRecordUnauthorizedForDataInRecord() {
		spiderAuthorizator = new AuthorizatorNotAuthorizedRequiredRulesButForActionOnRecordType();
		setUpDependencyProvider();

		SpiderDataGroup record = DataCreator.createRecordWithNameInDataAndLinkedRecordId("place",
				"cora");
		recordCreator.createAndStoreRecord("someToken78678567", "place", record);
	}

	@Test(expectedExceptions = RecordNotFoundException.class)
	public void testNonExistingRecordType() {
		SpiderDataGroup record = SpiderDataGroup.withNameInData("authority");
		recordCreator.createAndStoreRecord("someToken78678567", "recordType_NOT_EXISTING", record);
	}

	@Test(expectedExceptions = MisuseException.class)
	public void testCreateRecordAbstractRecordType() {
		recordStorage = new RecordStorageSpy();
		setUpDependencyProvider();

		SpiderDataGroup record = SpiderDataGroup.withNameInData("abstract");
		recordCreator.createAndStoreRecord("someToken78678567", "abstract", record);
	}

	@Test(expectedExceptions = RecordConflictException.class)
	public void testCreateRecordDuplicateUserSuppliedId() {
		SpiderDataGroup record = DataCreator
				.createRecordWithNameInDataAndIdAndLinkedRecordId("place", "somePlace", "cora");

		recordCreator.createAndStoreRecord("someToken78678567", "place", record);
		recordCreator.createAndStoreRecord("someToken78678567", "place", record);
	}

	@Test(expectedExceptions = DataException.class)
	public void testLinkedRecordIdDoesNotExist() {
		recordStorage = new RecordLinkTestsRecordStorage();
		((RecordLinkTestsRecordStorage) recordStorage).recordIdExistsForRecordType = false;

		linkCollector = DataCreator.getDataRecordLinkCollectorSpyWithCollectedLinkAdded();
		setUpDependencyProvider();

		SpiderDataGroup dataGroup = RecordLinkTestsDataCreator.createDataGroupWithLink();
		dataGroup.addChild(DataCreator.createRecordInfoWithLinkedRecordId("cora"));

		recordCreator.createAndStoreRecord("someToken78678567", "dataWithLinks", dataGroup);
	}

	@Test
	public void testLinkedRecordIdExists() {
		recordStorage = new RecordLinkTestsRecordStorage();
		RecordLinkTestsRecordStorage recordLinkTestsRecordStorage = (RecordLinkTestsRecordStorage) recordStorage;
		recordLinkTestsRecordStorage.recordIdExistsForRecordType = true;
		linkCollector = DataCreator.getDataRecordLinkCollectorSpyWithCollectedLinkAdded();
		setUpDependencyProvider();

		SpiderDataGroup dataGroup = RecordLinkTestsDataCreator.createDataGroupWithLink();
		dataGroup.addChild(DataCreator.createRecordInfoWithLinkedRecordId("cora"));

		recordCreator.createAndStoreRecord("someToken78678567", "dataWithLinks", dataGroup);
		assertTrue(recordLinkTestsRecordStorage.createWasRead);

		assertEquals(recordLinkTestsRecordStorage.type, "toRecordType");
		assertEquals(recordLinkTestsRecordStorage.id, "toRecordId");
	}
}
