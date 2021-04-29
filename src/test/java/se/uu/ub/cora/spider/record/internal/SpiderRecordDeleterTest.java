/*
 * Copyright 2015, 2019 Uppsala University Library
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
import static org.testng.Assert.assertTrue;

import java.util.HashMap;

import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import se.uu.ub.cora.data.DataAtomicFactory;
import se.uu.ub.cora.data.DataAtomicProvider;
import se.uu.ub.cora.data.DataGroup;
import se.uu.ub.cora.data.DataGroupFactory;
import se.uu.ub.cora.data.DataGroupProvider;
import se.uu.ub.cora.data.copier.DataCopierProvider;
import se.uu.ub.cora.logger.LoggerProvider;
import se.uu.ub.cora.search.RecordIndexer;
import se.uu.ub.cora.spider.authentication.AuthenticationException;
import se.uu.ub.cora.spider.authentication.AuthenticatorSpy;
import se.uu.ub.cora.spider.authorization.AuthorizationException;
import se.uu.ub.cora.spider.authorization.PermissionRuleCalculator;
import se.uu.ub.cora.spider.data.DataAtomicFactorySpy;
import se.uu.ub.cora.spider.data.DataGroupFactorySpy;
import se.uu.ub.cora.spider.dependency.RecordStorageProviderSpy;
import se.uu.ub.cora.spider.dependency.SpiderDependencyProviderSpy;
import se.uu.ub.cora.spider.dependency.SpiderInstanceFactory;
import se.uu.ub.cora.spider.dependency.SpiderInstanceFactoryImp;
import se.uu.ub.cora.spider.dependency.SpiderInstanceProvider;
import se.uu.ub.cora.spider.extendedfunctionality.ExtendedFunctionalitySpy;
import se.uu.ub.cora.spider.extendedfunctionality.internal.ExtendedFunctionalityProviderSpy;
import se.uu.ub.cora.spider.log.LoggerFactorySpy;
import se.uu.ub.cora.spider.record.DataCopierFactorySpy;
import se.uu.ub.cora.spider.record.MisuseException;
import se.uu.ub.cora.spider.record.RecordDeleter;
import se.uu.ub.cora.spider.spy.DataGroupTermCollectorSpy;
import se.uu.ub.cora.spider.spy.OldRecordStorageSpy;
import se.uu.ub.cora.spider.spy.RecordIndexerSpy;
import se.uu.ub.cora.spider.spy.RuleCalculatorSpy;
import se.uu.ub.cora.spider.spy.SpiderAuthorizatorSpy;
import se.uu.ub.cora.spider.testdata.TestDataRecordInMemoryStorage;
import se.uu.ub.cora.storage.RecordNotFoundException;
import se.uu.ub.cora.storage.RecordStorage;

public class SpiderRecordDeleterTest {
	private RecordStorage recordStorage;
	private AuthenticatorSpy authenticator;
	private SpiderAuthorizatorSpy authorizator;
	private PermissionRuleCalculator keyCalculator;
	private SpiderDependencyProviderSpy dependencyProvider;
	private RecordDeleter recordDeleter;
	private RecordIndexer recordIndexer;
	private DataGroupTermCollectorSpy termCollector;
	private LoggerFactorySpy loggerFactorySpy;
	private ExtendedFunctionalityProviderSpy extendedFunctionalityProvider;
	private DataGroupFactory dataGroupFactory;
	private DataAtomicFactory dataAtomicFactory;
	private DataCopierFactorySpy dataCopierFactory;

	@BeforeMethod
	public void beforeMethod() {
		setUpFactoriesAndProviders();
		authenticator = new AuthenticatorSpy();
		authorizator = new SpiderAuthorizatorSpy();
		recordStorage = TestDataRecordInMemoryStorage.createRecordStorageInMemoryWithTestData();
		keyCalculator = new RuleCalculatorSpy();
		recordIndexer = new RecordIndexerSpy();
		termCollector = new DataGroupTermCollectorSpy();
		extendedFunctionalityProvider = new ExtendedFunctionalityProviderSpy();
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
	}

	private void setUpDependencyProvider() {
		dependencyProvider = new SpiderDependencyProviderSpy(new HashMap<>());
		dependencyProvider.authenticator = authenticator;
		dependencyProvider.spiderAuthorizator = authorizator;

		RecordStorageProviderSpy recordStorageProviderSpy = new RecordStorageProviderSpy();
		recordStorageProviderSpy.recordStorage = recordStorage;
		dependencyProvider.setRecordStorageProvider(recordStorageProviderSpy);
		dependencyProvider.extendedFunctionalityProvider = extendedFunctionalityProvider;
		dependencyProvider.ruleCalculator = keyCalculator;
		dependencyProvider.recordIndexer = recordIndexer;
		dependencyProvider.termCollector = termCollector;
		SpiderInstanceFactory factory = SpiderInstanceFactoryImp
				.usingDependencyProvider(dependencyProvider);
		SpiderInstanceProvider.setSpiderInstanceFactory(factory);
		recordDeleter = RecordDeleterImp.usingDependencyProvider(dependencyProvider);
	}

	@Test(expectedExceptions = AuthenticationException.class)
	public void testAuthenticationNotAuthenticated() {
		authenticator.throwAuthenticationException = true;
		recordStorage = new OldRecordStorageSpy();
		setUpDependencyProvider();
		recordDeleter.deleteRecord("dummyNonAuthenticatedToken", "spyType", "spyId");
	}

	@Test
	public void testDeleteAuthorizedNoIncomingLinks() {
		recordStorage = new OldRecordStorageSpy();
		setUpDependencyProvider();

		recordDeleter.deleteRecord("userId", "child1", "place:0002");
		assertTrue(((OldRecordStorageSpy) recordStorage).deleteWasCalled);
		assertEquals(((RecordIndexerSpy) recordIndexer).type, "child1");
		assertEquals(((RecordIndexerSpy) recordIndexer).id, "place:0002");
	}

	@Test
	public void testDeleteAuthorizedNoIncomingLinksCheckExternalDependenciesAreCalled() {
		recordStorage = new OldRecordStorageSpy();
		setUpDependencyProvider();

		recordDeleter.deleteRecord("userId", "child1", "place:0002");

		String methodName = "checkUserIsAuthorizedForActionOnRecordTypeAndCollectedData";
		authorizator.MCR.assertParameters(methodName, 0, authenticator.returnedUser, "delete",
				"child1", termCollector.MCR.getReturnValue("collectTerms", 0));

		termCollector.MCR.assertParameter("collectTerms", 0, "metadataId", "child1");

		OldRecordStorageSpy recordStorageSpy = (OldRecordStorageSpy) recordStorage;
		termCollector.MCR.assertParameter("collectTerms", 0, "dataGroup",
				recordStorageSpy.readDataGroup);
	}

	@Test
	public void testExtendedFunctionalityBeforeDelete() {
		recordStorage = new OldRecordStorageSpy();
		setUpDependencyProvider();
		String recordId = "place:0002";
		String recordType = "child1";
		recordDeleter.deleteRecord("userId", recordType, recordId);

		OldRecordStorageSpy recordStorageSpy = (OldRecordStorageSpy) recordStorage;
		assertEquals(recordStorageSpy.numOfTimesReadWasCalled, 3);
		assertEquals(recordStorageSpy.types.get(0), recordType);
		assertEquals(recordStorageSpy.ids.get(0), recordId);

		ExtendedFunctionalitySpy extendedFunctionality = extendedFunctionalityProvider.fetchedFunctionalityBeforeDelete
				.get(0);
		assertTrue(extendedFunctionality.extendedFunctionalityHasBeenCalled);
		DataGroup dataGoupSentToExtended = extendedFunctionality.dataGroupSentToExtendedFunctionality;
		assertCorrectDataInGroupSentToExtended(recordId, recordType, dataGoupSentToExtended);

	}

	private void assertCorrectDataInGroupSentToExtended(String recordId, String recordType,
			DataGroup dataGoupSentToExtended) {
		DataGroup recordInfo = dataGoupSentToExtended.getFirstGroupWithNameInData("recordInfo");
		String id = recordInfo.getFirstAtomicValueWithNameInData("id");
		assertEquals(id, recordId);
		DataGroup type = recordInfo.getFirstGroupWithNameInData("type");
		assertEquals(type.getFirstAtomicValueWithNameInData("linkedRecordId"), recordType);
	}

	@Test(expectedExceptions = MisuseException.class)
	public void testDeleteAuthorizedWithIncomingLinks() {
		recordStorage = new OldRecordStorageSpy();
		setUpDependencyProvider();

		((OldRecordStorageSpy) recordStorage).linksExist = true;
		recordDeleter.deleteRecord("userId", "child1", "place:0001");
	}

	@Test(expectedExceptions = MisuseException.class)
	public void testAuthorizedToDeleteAndIncomingLinkToAbstractParent() {
		recordStorage = new OldRecordStorageSpy();
		setUpDependencyProvider();

		recordDeleter.deleteRecord("userId", "place", "place:0003");
	}

	@Test
	public void testAuthorizedToDeleteAndNoIncomingLinkToAbstractParent() {
		recordStorage = new OldRecordStorageSpy();
		setUpDependencyProvider();

		recordDeleter.deleteRecord("userId", "place", "place:0001");

		assertTrue(((OldRecordStorageSpy) recordStorage).deleteWasCalled);
		assertEquals(((RecordIndexerSpy) recordIndexer).type, "place");
		assertEquals(((RecordIndexerSpy) recordIndexer).id, "place:0001");
	}

	@Test(expectedExceptions = AuthorizationException.class)
	public void testDeleteUnauthorized() {
		authorizator.authorizedForActionAndRecordType = false;

		recordDeleter.deleteRecord("unauthorizedUserId", "place", "place:0001");
	}

	@Test(expectedExceptions = AuthorizationException.class)
	public void testDeleteUnauthorizedRecodNotFound() {
		authorizator.authorizedForActionAndRecordType = false;

		recordDeleter.deleteRecord("unauthorizedUserId", "place", "place:0001_NOT_FOUND");
	}

	@Test(expectedExceptions = RecordNotFoundException.class)
	public void testDeleteNotFound() {
		recordDeleter.deleteRecord("userId", "place", "place:0001_NOT_FOUND");
	}

	@Test(expectedExceptions = RecordNotFoundException.class)
	public void testReadingDataForANonExistingRecordType() {
		recordDeleter.deleteRecord("userId", "nonExistingRecordType", "anId");
	}
}
