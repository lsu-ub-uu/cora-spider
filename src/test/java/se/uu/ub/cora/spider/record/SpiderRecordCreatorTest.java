/*
 * Copyright 2015, 2016 Uppsala University Library
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

import java.util.List;

import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import se.uu.ub.cora.beefeater.Authorizator;
import se.uu.ub.cora.beefeater.AuthorizatorImp;
import se.uu.ub.cora.bookkeeper.data.DataGroup;
import se.uu.ub.cora.bookkeeper.linkcollector.DataRecordLinkCollector;
import se.uu.ub.cora.bookkeeper.validator.DataValidator;
import se.uu.ub.cora.spider.authentication.AuthenticationException;
import se.uu.ub.cora.spider.authentication.Authenticator;
import se.uu.ub.cora.spider.authentication.AuthenticatorSpy;
import se.uu.ub.cora.spider.authorization.AuthorizationException;
import se.uu.ub.cora.spider.authorization.NeverAuthorisedStub;
import se.uu.ub.cora.spider.authorization.PermissionKeyCalculator;
import se.uu.ub.cora.spider.data.Action;
import se.uu.ub.cora.spider.data.SpiderDataAtomic;
import se.uu.ub.cora.spider.data.SpiderDataGroup;
import se.uu.ub.cora.spider.data.SpiderDataRecord;
import se.uu.ub.cora.spider.data.SpiderDataRecordLink;
import se.uu.ub.cora.spider.dependency.SpiderDependencyProviderSpy;
import se.uu.ub.cora.spider.dependency.SpiderInstanceFactory;
import se.uu.ub.cora.spider.dependency.SpiderInstanceFactoryImp;
import se.uu.ub.cora.spider.dependency.SpiderInstanceProvider;
import se.uu.ub.cora.spider.extended.ExtendedFunctionalityProviderSpy;
import se.uu.ub.cora.spider.extended.ExtendedFunctionalitySpy;
import se.uu.ub.cora.spider.record.storage.RecordConflictException;
import se.uu.ub.cora.spider.record.storage.RecordIdGenerator;
import se.uu.ub.cora.spider.record.storage.RecordNotFoundException;
import se.uu.ub.cora.spider.record.storage.RecordStorage;
import se.uu.ub.cora.spider.record.storage.TimeStampIdGenerator;
import se.uu.ub.cora.spider.spy.AuthorizatorAlwaysAuthorizedSpy;
import se.uu.ub.cora.spider.spy.DataRecordLinkCollectorSpy;
import se.uu.ub.cora.spider.spy.DataValidatorAlwaysInvalidSpy;
import se.uu.ub.cora.spider.spy.DataValidatorAlwaysValidSpy;
import se.uu.ub.cora.spider.spy.IdGeneratorSpy;
import se.uu.ub.cora.spider.spy.KeyCalculatorSpy;
import se.uu.ub.cora.spider.spy.RecordPermissionKeyCalculatorStub;
import se.uu.ub.cora.spider.spy.RecordStorageCreateUpdateSpy;
import se.uu.ub.cora.spider.spy.RecordStorageSpy;
import se.uu.ub.cora.spider.testdata.DataCreator;
import se.uu.ub.cora.spider.testdata.RecordLinkTestsDataCreator;
import se.uu.ub.cora.spider.testdata.TestDataRecordInMemoryStorage;

public class SpiderRecordCreatorTest {
	private RecordStorage recordStorage;
	private Authenticator authenticator;
	private Authorizator authorizator;
	private RecordIdGenerator idGenerator;
	private PermissionKeyCalculator keyCalculator;
	private SpiderRecordCreator recordCreator;
	private DataValidator dataValidator;
	private DataRecordLinkCollector linkCollector;
	private SpiderDependencyProviderSpy dependencyProvider;
	private ExtendedFunctionalityProviderSpy extendedFunctionalityProvider;

	@BeforeMethod
	public void beforeMethod() {
		authenticator = new AuthenticatorSpy();
		authorizator = new AuthorizatorImp();
		dataValidator = new DataValidatorAlwaysValidSpy();
		recordStorage = TestDataRecordInMemoryStorage.createRecordStorageInMemoryWithTestData();
		idGenerator = new TimeStampIdGenerator();
		keyCalculator = new RecordPermissionKeyCalculatorStub();
		linkCollector = new DataRecordLinkCollectorSpy();
		extendedFunctionalityProvider = new ExtendedFunctionalityProviderSpy();
		setUpDependencyProvider();
	}

	private void setUpDependencyProvider() {
		dependencyProvider = new SpiderDependencyProviderSpy();
		dependencyProvider.authenticator = authenticator;
		dependencyProvider.authorizator = authorizator;
		dependencyProvider.dataValidator = dataValidator;
		dependencyProvider.recordStorage = recordStorage;
		dependencyProvider.keyCalculator = keyCalculator;
		dependencyProvider.linkCollector = linkCollector;
		dependencyProvider.idGenerator = idGenerator;
		dependencyProvider.extendedFunctionalityProvider = extendedFunctionalityProvider;
		SpiderInstanceFactory factory = SpiderInstanceFactoryImp
				.usingDependencyProvider(dependencyProvider);
		SpiderInstanceProvider.setSpiderInstanceFactory(factory);
		recordCreator = SpiderRecordCreatorImp.usingDependencyProvider(dependencyProvider);
	}

	@Test
	public void testExternalDependenciesAreCalled() {
		authorizator = new AuthorizatorAlwaysAuthorizedSpy();
		dataValidator = new DataValidatorAlwaysValidSpy();
		recordStorage = new RecordStorageSpy();
		idGenerator = new IdGeneratorSpy();
		keyCalculator = new KeyCalculatorSpy();
		linkCollector = new DataRecordLinkCollectorSpy();
		setUpDependencyProvider();
		SpiderDataGroup spiderDataGroup = DataCreator
				.createRecordWithNameInDataAndLinkedRecordId("nameInData", "cora");
		recordCreator.createAndStoreRecord("dummyAuthenticatedToken", "spyType", spiderDataGroup);

		assertTrue(((AuthenticatorSpy) authenticator).authenticationWasCalled);
		assertTrue(((AuthorizatorAlwaysAuthorizedSpy) authorizator).authorizedWasCalled);
		assertTrue(((DataValidatorAlwaysValidSpy) dataValidator).validateDataWasCalled);
		ExtendedFunctionalitySpy extendedFunctionality = extendedFunctionalityProvider.fetchedFunctionalityForCreateAfterMetadataValidation
				.get(0);
		assertTrue(extendedFunctionality.extendedFunctionalityHasBeenCalled);
		assertTrue(((RecordStorageSpy) recordStorage).createWasCalled);
		assertTrue(((IdGeneratorSpy) idGenerator).getIdForTypeWasCalled);
		assertTrue(((KeyCalculatorSpy) keyCalculator).calculateKeysWasCalled);
		assertTrue(((DataRecordLinkCollectorSpy) linkCollector).collectLinksWasCalled);
		assertEquals(((DataRecordLinkCollectorSpy) linkCollector).metadataId, "spyTypeNew");
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
		authorizator = new AuthorizatorAlwaysAuthorizedSpy();
		dataValidator = new DataValidatorAlwaysValidSpy();
		recordStorage = new RecordStorageSpy();
		idGenerator = new IdGeneratorSpy();
		keyCalculator = new KeyCalculatorSpy();
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

		assertEquals(recordInfo.extractAtomicValue("createdBy"), "12345");
		assertEquals(recordInfo.extractAtomicValue("type"), "typeWithAutoGeneratedId");

		DataGroup groupCreated = ((RecordStorageCreateUpdateSpy) recordStorage).createRecord;
		assertEquals(groupOut.getNameInData(), groupCreated.getNameInData(),
				"Returned and read record should have the same nameInData");
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

		assertEquals(recordInfo.extractAtomicValue("createdBy"), "12345");
		assertEquals(recordInfo.extractAtomicValue("type"), "typeWithUserGeneratedId");

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
		authorizator = new NeverAuthorisedStub();
		setUpDependencyProvider();

		SpiderDataGroup record = DataCreator
				.createRecordWithNameInDataAndLinkedRecordId("authority", "cora");
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

	@Test
	public void testActionsOnCreatedRecord() {
		recordStorage = new RecordStorageCreateUpdateSpy();
		setUpDependencyProvider();

		SpiderDataGroup record = DataCreator
				.createRecordWithNameInDataAndLinkedRecordId("typeWithAutoGeneratedId", "cora");

		SpiderDataRecord recordOut = recordCreator.createAndStoreRecord("someToken78678567",
				"typeWithAutoGeneratedId", record);
		assertEquals(recordOut.getActions().size(), 3);
		assertTrue(recordOut.getActions().contains(Action.READ));
		assertTrue(recordOut.getActions().contains(Action.UPDATE));
		assertTrue(recordOut.getActions().contains(Action.DELETE));
		assertFalse(recordOut.getActions().contains(Action.READ_INCOMING_LINKS));
	}

	@Test
	public void testActionsOnCreatedRecordInRecordInfo() {
		recordStorage = new RecordStorageCreateUpdateSpy();
		setUpDependencyProvider();

		SpiderDataGroup record = DataCreator
				.createRecordWithNameInDataAndLinkedRecordId("typeWithAutoGeneratedId", "cora");

		SpiderDataRecord recordOut = recordCreator.createAndStoreRecord("someToken78678567",
				"typeWithAutoGeneratedId", record);

		SpiderDataGroup recordInfo = recordOut.getSpiderDataGroup().extractGroup("recordInfo");
		SpiderDataRecordLink dataDivider = (SpiderDataRecordLink) recordInfo
				.extractGroup("dataDivider");

		assertTrue(dataDivider.getActions().contains(Action.READ));
		assertEquals(dataDivider.getActions().size(), 1);
	}

	@Test
	public void testActionsOnCreatedRecordRecordTypeImage() {
		recordStorage = new RecordStorageCreateUpdateSpy();
		setUpDependencyProvider();

		SpiderDataGroup record = DataCreator
				.createRecordWithNameInDataAndIdAndLinkedRecordId("recordType", "image", "cora");
		record.addChild(SpiderDataAtomic.withNameInDataAndValue("parentId", "binary"));

		SpiderDataRecord recordOut = recordCreator.createAndStoreRecord("someToken78678567",
				"recordType", record);
		assertEquals(recordOut.getActions().size(), 6);
		assertTrue(recordOut.getActions().contains(Action.READ));
		assertTrue(recordOut.getActions().contains(Action.UPDATE));
		assertTrue(recordOut.getActions().contains(Action.DELETE));
		assertTrue(recordOut.getActions().contains(Action.CREATE));

		assertTrue(recordOut.getActions().contains(Action.LIST));
		assertTrue(recordOut.getActions().contains(Action.SEARCH));
		assertFalse(recordOut.getActions().contains(Action.UPLOAD));
	}

	@Test
	public void testCreateRecordWithDataRecordLinkHasReadActionTopLevel() {
		SpiderDataGroup dataGroup = RecordLinkTestsDataCreator.createDataGroupWithLink();
		dataGroup.addChild(DataCreator.createRecordInfoWithLinkedRecordId("cora"));

		SpiderRecordCreator recordCreator = createRecordCreatorWithTestDataForLinkedData();
		SpiderDataRecord record = recordCreator.createAndStoreRecord("someToken78678567",
				"dataWithLinks", dataGroup);

		RecordLinkTestsAsserter.assertTopLevelLinkContainsReadActionOnly(record);
	}

	private SpiderRecordCreator createRecordCreatorWithTestDataForLinkedData() {
		recordStorage = new RecordLinkTestsRecordStorage();
		setUpDependencyProvider();

		return recordCreator;
	}

	@Test
	public void testCreateRecordWithDataRecordLinkHasReadActionOneLevelDown() {
		SpiderDataGroup dataGroup = RecordLinkTestsDataCreator
				.createDataGroupWithLinkOneLevelDown();
		dataGroup.addChild(DataCreator.createRecordInfoWithLinkedRecordId("cora"));

		SpiderRecordCreator recordCreator = createRecordCreatorWithTestDataForLinkedData();
		SpiderDataRecord record = recordCreator.createAndStoreRecord("someToken78678567",
				"dataWithLinks", dataGroup);

		RecordLinkTestsAsserter.assertOneLevelDownLinkContainsReadActionOnly(record);
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
		((RecordLinkTestsRecordStorage) recordStorage).recordIdExistsForRecordType = true;
		linkCollector = DataCreator.getDataRecordLinkCollectorSpyWithCollectedLinkAdded();
		setUpDependencyProvider();

		SpiderDataGroup dataGroup = RecordLinkTestsDataCreator.createDataGroupWithLink();
		dataGroup.addChild(DataCreator.createRecordInfoWithLinkedRecordId("cora"));

		recordCreator.createAndStoreRecord("someToken78678567", "dataWithLinks", dataGroup);
		assertTrue(((RecordLinkTestsRecordStorage) recordStorage).createWasRead);
	}
}
