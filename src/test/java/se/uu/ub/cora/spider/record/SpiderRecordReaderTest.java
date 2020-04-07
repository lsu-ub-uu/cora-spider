/*
 * Copyright 2015, 2016, 2017, 2019 Uppsala University Library
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
import static org.testng.Assert.assertSame;
import static org.testng.Assert.assertTrue;

import java.util.HashMap;
import java.util.Set;

import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import se.uu.ub.cora.data.DataAtomicProvider;
import se.uu.ub.cora.data.DataGroup;
import se.uu.ub.cora.data.DataGroupFactory;
import se.uu.ub.cora.data.DataGroupProvider;
import se.uu.ub.cora.data.DataRecord;
import se.uu.ub.cora.data.copier.DataCopierProvider;
import se.uu.ub.cora.logger.LoggerProvider;
import se.uu.ub.cora.spider.authentication.AuthenticationException;
import se.uu.ub.cora.spider.authentication.AuthenticatorSpy;
import se.uu.ub.cora.spider.authorization.AuthorizationException;
import se.uu.ub.cora.spider.authorization.PermissionRuleCalculator;
import se.uu.ub.cora.spider.data.DataAtomicFactorySpy;
import se.uu.ub.cora.spider.data.DataGroupFactorySpy;
import se.uu.ub.cora.spider.dependency.MetadataStorageProviderSpy;
import se.uu.ub.cora.spider.dependency.RecordStorageProviderSpy;
import se.uu.ub.cora.spider.dependency.RecordTypeHandlerSpy;
import se.uu.ub.cora.spider.dependency.SpiderDependencyProviderSpy;
import se.uu.ub.cora.spider.log.LoggerFactorySpy;
import se.uu.ub.cora.spider.spy.DataGroupTermCollectorSpy;
import se.uu.ub.cora.spider.spy.OldRecordStorageSpy;
import se.uu.ub.cora.spider.spy.RuleCalculatorSpy;
import se.uu.ub.cora.spider.spy.SpiderAuthorizatorSpy;
import se.uu.ub.cora.spider.testdata.TestDataRecordInMemoryStorage;
import se.uu.ub.cora.storage.MetadataStorage;
import se.uu.ub.cora.storage.RecordNotFoundException;
import se.uu.ub.cora.storage.RecordStorage;

public class SpiderRecordReaderTest {
	private RecordStorage recordStorage;
	private AuthenticatorSpy authenticator;
	private SpiderAuthorizatorSpy authorizator;
	private PermissionRuleCalculator ruleCalculator;
	private SpiderDependencyProviderSpy dependencyProvider;
	private SpiderRecordReader recordReader;
	private DataGroupToRecordEnhancerSpy dataGroupToRecordEnhancer;
	private DataGroupTermCollectorSpy termCollector;
	private LoggerFactorySpy loggerFactorySpy;
	private DataGroupFactory dataGroupFactory;
	private DataAtomicFactorySpy dataAtomicFactory;
	private DataCopierFactorySpy dataCopierFactory;
	private RecordTypeHandlerSpy recordTypeHandlerSpy;
	private DataRedactorSpy dataRedactor;

	@BeforeMethod
	public void beforeMethod() {
		setUpFactoriesAndProviders();
		authenticator = new AuthenticatorSpy();
		authorizator = new SpiderAuthorizatorSpy();
		recordStorage = TestDataRecordInMemoryStorage.createRecordStorageInMemoryWithTestData();
		ruleCalculator = new RuleCalculatorSpy();
		termCollector = new DataGroupTermCollectorSpy();
		dataRedactor = new DataRedactorSpy();
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

		MetadataStorageProviderSpy metadataStorageProviderSpy = new MetadataStorageProviderSpy();
		metadataStorageProviderSpy.metadataStorage = (MetadataStorage) recordStorage;
		dependencyProvider.setMetadataStorageProvider(metadataStorageProviderSpy);

		dependencyProvider.ruleCalculator = ruleCalculator;
		dataGroupToRecordEnhancer = new DataGroupToRecordEnhancerSpy();
		dependencyProvider.termCollector = termCollector;

		recordReader = SpiderRecordReaderImp.usingDependencyProviderAndDataGroupToRecordEnhancer(
				dependencyProvider, dataGroupToRecordEnhancer);
		recordTypeHandlerSpy = dependencyProvider.recordTypeHandlerSpy;

		dependencyProvider.dataRedactor = dataRedactor;

	}

	@Test(expectedExceptions = AuthenticationException.class)
	public void testAuthenticationNotAuthenticated() {
		authenticator.throwAuthenticationException = true;
		recordStorage = new OldRecordStorageSpy();
		setUpDependencyProvider();
		recordReader.readRecord("dummyNonAuthenticatedToken", "spyType", "spyId");
	}

	@Test
	public void testRecordEnhancerCalled() {
		recordReader.readRecord("someToken78678567", "place", "place:0001");
		assertEquals(dataGroupToRecordEnhancer.user.id, "12345");
		assertEquals(dataGroupToRecordEnhancer.recordType, "place");
		assertEquals(dataGroupToRecordEnhancer.dataGroup.getFirstGroupWithNameInData("recordInfo")
				.getFirstAtomicValueWithNameInData("id"), "place:0001");
	}

	@Test
	public void testReadAuthorized() {
		DataRecord record = recordReader.readRecord("someToken78678567", "place", "place:0001");
		DataGroup groupOut = record.getDataGroup();
		assertEquals(groupOut.getNameInData(), "authority",
				"recordOut.getNameInData should be authority");
	}

	@Test
	public void testReadAuthorized2() {
		recordTypeHandlerSpy.recordPartConstraint = "";
		DataRecord record = recordReader.readRecord("someToken78678567", "place", "place:0001");
		DataGroup groupOut = record.getDataGroup();
		assertEquals(groupOut.getNameInData(), "authority");

		String methodName = "checkUserIsAuthorizedForActionOnRecordType";
		authorizator.MCR.assertParameters(methodName, 0, authenticator.returnedUser, "read",
				"place");

		termCollector.MCR.assertParameter("collectTerms", 0, "metadataId",
				"fakeMetadataIdFromRecordTypeHandlerSpy");
		termCollector.MCR.assertParameter("collectTerms", 0, "dataGroup",
				recordStorage.read("place", "place:0001"));

		String methodName2 = "checkAndGetUserAuthorizationsForActionOnRecordTypeAndCollectedData";
		authorizator.MCR.assertParameters(methodName2, 0, authenticator.returnedUser, "read",
				"place", termCollector.MCR.getReturnValue("collectTerms", 0), false);
	}

	@Test
	public void testReadAuthorizedHasRecordPartConstraints() {
		recordTypeHandlerSpy.recordPartConstraint = "readWrite";

		recordReader.readRecord("someToken78678567", "place", "place:0001");

		String methodName2 = "checkAndGetUserAuthorizationsForActionOnRecordTypeAndCollectedData";
		authorizator.MCR.assertParameters(methodName2, 0, authenticator.returnedUser, "read",
				"place", termCollector.MCR.getReturnValue("collectTerms", 0), true);

		termCollector.MCR.assertParameter("collectTerms", 0, "metadataId",
				"fakeMetadataIdFromRecordTypeHandlerSpy");
		termCollector.MCR.assertParameter("collectTerms", 0, "dataGroup",
				recordStorage.read("place", "place:0001"));
	}

	@Test
	public void testReadHasRecordPartConstraintsNotAuthorized() {
		authorizator.authorizedForActionAndRecordTypeAndCollectedData = false;

		recordTypeHandlerSpy.recordPartConstraint = "write";
		try {
			recordReader.readRecord("someToken78678567", "place", "place:0001");
		} catch (Exception e) {
			authorizator.MCR.assertMethodWasCalled(
					"checkAndGetUserAuthorizationsForActionOnRecordTypeAndCollectedData");
			dataRedactor.MCR
					.assertMethodNotCalled("removeChildrenForConstraintsWithoutPermissions");
		}
	}

	@Test
	public void testReadRecordAbstractRecordType() {
		recordStorage = new OldRecordStorageSpy();
		setUpDependencyProvider();
		recordTypeHandlerSpy.isAbstract = true;
		DataRecord readRecord = recordReader.readRecord("someToken78678567", "abstractAuthority",
				"place:0001");
		assertNotNull(readRecord);

		String methodName = "checkAndGetUserAuthorizationsForActionOnRecordTypeAndCollectedData";
		authorizator.MCR.assertParameters(methodName, 0, authenticator.returnedUser, "read",
				"abstractAuthority", termCollector.MCR.getReturnValue("collectTerms", 0), false);

		termCollector.MCR.assertParameter("collectTerms", 0, "metadataId",
				"fakeMetadataIdFromRecordTypeHandlerSpy");
		termCollector.MCR.assertParameter("collectTerms", 0, "dataGroup",
				recordStorage.read("abstractAuthority", "place:0001"));
	}

	@Test
	public void testUnauthorizedForRecordTypeShouldNeverReadRecordFromStorage() {
		recordStorage = new OldRecordStorageSpy();
		setUpDependencyProvider();
		authorizator.authorizedForActionAndRecordType = false;

		boolean exceptionWasCaught = false;
		try {
			recordReader.readRecord("unauthorizedUserId", "place", "place:0001");
		} catch (Exception e) {
			assertEquals(e.getClass(), AuthorizationException.class);
			exceptionWasCaught = true;
		}
		assertTrue(exceptionWasCaught);
		assertEquals(((OldRecordStorageSpy) recordStorage).numOfTimesReadWasCalled, 0);
	}

	@Test
	public void testUnauthorizedForRulesAgainsRecord() {
		recordStorage = new OldRecordStorageSpy();
		setUpDependencyProvider();
		authorizator.authorizedForActionAndRecordTypeAndCollectedData = false;

		boolean exceptionWasCaught = false;
		try {
			recordReader.readRecord("unauthorizedUserId", "place", "place:0001");
		} catch (Exception e) {
			assertEquals(e.getClass(), AuthorizationException.class);
			exceptionWasCaught = true;
		}
		assertTrue(exceptionWasCaught);
		assertEquals(((OldRecordStorageSpy) recordStorage).readWasCalled, true);
	}

	@Test(expectedExceptions = RecordNotFoundException.class)
	public void testReadingDataForANonExistingRecordType() {
		recordReader.readRecord("someToken78678567", "nonExistingRecordType", "anId");
	}

	@Test
	public void testReadNotAuthorizedToReadRecordTypeButPublicRecordType() throws Exception {
		recordStorage = new OldRecordStorageSpy();
		setUpDependencyProvider();
		recordTypeHandlerSpy.isPublicForRead = true;
		authorizator.authorizedForActionAndRecordType = false;
		// publicReadType
		DataRecord readRecord = recordReader.readRecord("unauthorizedUserId", "publicReadType",
				"publicReadType:0001");
		assertNotNull(readRecord);
	}

	@Test
	public void testReadPublicRecordType() throws Exception {
		recordStorage = new OldRecordStorageSpy();
		authorizator.authorizedForActionAndRecordTypeAndCollectedData = false;
		setUpDependencyProvider();
		recordTypeHandlerSpy.isPublicForRead = true;
		// publicReadType
		DataRecord readRecord = recordReader.readRecord("unauthorizedUserId", "publicReadType",
				"publicReadType:0001");

		assertNotNull(readRecord);
		assertFalse(recordTypeHandlerSpy.hasRecordPartReadContraintHasBeenCalled);
		authorizator.MCR.assertMethodNotCalled(
				"checkAndGetUserAuthorizationsForActionOnRecordTypeAndCollectedData");
		dataRedactor.MCR.assertMethodNotCalled("removeChildrenForConstraintsWithoutPermissions");
	}

	@Test
	public void testRecordHasNoPartConstraints() throws Exception {
		OldRecordStorageSpy recordStorageSpy = new OldRecordStorageSpy();
		recordStorage = recordStorageSpy;
		setUpDependencyProvider();
		recordTypeHandlerSpy.recordPartConstraint = "";
		recordTypeHandlerSpy.isPublicForRead = true;

		recordReader.readRecord("someUserId", "someType", "someId");

		authorizator.MCR.assertMethodNotCalled(
				"checkAndGetUserAuthorizationsForActionOnRecordTypeAndCollectedData");
		dataRedactor.MCR.assertMethodNotCalled("removeChildrenForConstraintsWithoutPermissions");
	}

	@Test
	public void testRecordHasReadPartConstraints() throws Exception {
		OldRecordStorageSpy recordStorageSpy = new OldRecordStorageSpy();
		recordStorage = recordStorageSpy;
		setUpDependencyProvider();
		recordTypeHandlerSpy.recordPartConstraint = "readWrite";

		recordReader.readRecord("someUserId", "spyType", "spyId");

		assertTrue(recordTypeHandlerSpy.hasRecordPartReadContraintHasBeenCalled);
		SpiderAuthorizatorSpy authorizatorSpy = authorizator;
		dataRedactor.MCR.assertMethodWasCalled("removeChildrenForConstraintsWithoutPermissions");
		Set<String> recordPartReadPermissions = (Set<String>) dataRedactor.MCR
				.getValueForMethodNameAndCallNumberAndParameterName(
						"removeChildrenForConstraintsWithoutPermissions", 0,
						"recordPartReadPermissions");
		assertEquals(authorizatorSpy.recordPartReadPermissions, recordPartReadPermissions);
		DataGroup lastDataRedactedForRead = (DataGroup) dataRedactor.MCR
				.getValueForMethodNameAndCallNumberAndParameterName(
						"removeChildrenForConstraintsWithoutPermissions", 0, "recordRead");
		assertSame(recordStorageSpy.aRecord, lastDataRedactedForRead);
		DataGroup lastEnhancedDataGroup = dataGroupToRecordEnhancer.dataGroup;
		dataRedactor.MCR.assertReturn("removeChildrenForConstraintsWithoutPermissions", 0,
				lastEnhancedDataGroup);
	}

	@Test
	public void testRecordHasReadPartConstraintsRedactorParameters() throws Exception {
		OldRecordStorageSpy recordStorageSpy = new OldRecordStorageSpy();
		recordStorage = recordStorageSpy;
		setUpDependencyProvider();
		recordTypeHandlerSpy.recordPartConstraint = "readWrite";

		recordReader.readRecord("someUserId", "someType", "someId");

		DataGroup lastReadRecord = recordStorageSpy.dataGroupToReturn;

		Set<String> recordPartReadConstraints = (Set<String>) recordTypeHandlerSpy.MCR
				.getReturnValue("getRecordPartReadConstraints", 0);
		dataRedactor.MCR.assertParameters("removeChildrenForConstraintsWithoutPermissions", 0,
				lastReadRecord, recordPartReadConstraints, authorizator.recordPartReadPermissions);

	}
}
