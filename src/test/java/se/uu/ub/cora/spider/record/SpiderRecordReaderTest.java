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
import static org.testng.Assert.assertNotNull;
import static org.testng.Assert.assertTrue;

import java.util.HashMap;

import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import se.uu.ub.cora.bookkeeper.data.DataGroup;
import se.uu.ub.cora.bookkeeper.termcollector.DataGroupTermCollector;
import se.uu.ub.cora.spider.authentication.AuthenticationException;
import se.uu.ub.cora.spider.authentication.Authenticator;
import se.uu.ub.cora.spider.authentication.AuthenticatorSpy;
import se.uu.ub.cora.spider.authorization.AuthorizationException;
import se.uu.ub.cora.spider.authorization.NeverAuthorisedStub;
import se.uu.ub.cora.spider.authorization.PermissionRuleCalculator;
import se.uu.ub.cora.spider.authorization.SpiderAuthorizator;
import se.uu.ub.cora.spider.data.SpiderDataGroup;
import se.uu.ub.cora.spider.data.SpiderDataRecord;
import se.uu.ub.cora.spider.dependency.SpiderDependencyProviderSpy;
import se.uu.ub.cora.spider.record.storage.RecordNotFoundException;
import se.uu.ub.cora.spider.record.storage.RecordStorage;
import se.uu.ub.cora.spider.spy.AuthorizatorAlwaysAuthorizedSpy;
import se.uu.ub.cora.spider.spy.DataGroupTermCollectorSpy;
import se.uu.ub.cora.spider.spy.NoRulesCalculatorStub;
import se.uu.ub.cora.spider.spy.RecordStorageSpy;
import se.uu.ub.cora.spider.testdata.TestDataRecordInMemoryStorage;

public class SpiderRecordReaderTest {
	private RecordStorage recordStorage;
	private Authenticator authenticator;
	private SpiderAuthorizator authorizator;
	private PermissionRuleCalculator keyCalculator;
	private SpiderDependencyProviderSpy dependencyProvider;
	private SpiderRecordReader recordReader;
	private DataGroupToRecordEnhancerSpy dataGroupToRecordEnhancer;
	private DataGroupTermCollector termCollector;

	@BeforeMethod
	public void beforeMethod() {
		authenticator = new AuthenticatorSpy();
		authorizator = new AuthorizatorAlwaysAuthorizedSpy();
		recordStorage = TestDataRecordInMemoryStorage.createRecordStorageInMemoryWithTestData();
		keyCalculator = new NoRulesCalculatorStub();
		termCollector = new DataGroupTermCollectorSpy();
		setUpDependencyProvider();
	}

	private void setUpDependencyProvider() {
		dependencyProvider = new SpiderDependencyProviderSpy(new HashMap<>());
		dependencyProvider.authenticator = authenticator;
		dependencyProvider.spiderAuthorizator = authorizator;
		dependencyProvider.recordStorage = recordStorage;
		dependencyProvider.ruleCalculator = keyCalculator;
		dataGroupToRecordEnhancer = new DataGroupToRecordEnhancerSpy();
		dependencyProvider.searchTermCollector = termCollector;

		recordReader = SpiderRecordReaderImp.usingDependencyProviderAndDataGroupToRecordEnhancer(
				dependencyProvider, dataGroupToRecordEnhancer);
	}

	@Test(expectedExceptions = AuthenticationException.class)
	public void testAuthenticationNotAuthenticated() {
		recordStorage = new RecordStorageSpy();
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
		SpiderDataRecord record = recordReader.readRecord("someToken78678567", "place",
				"place:0001");
		SpiderDataGroup groupOut = record.getSpiderDataGroup();
		assertEquals(groupOut.getNameInData(), "authority",
				"recordOut.getNameInData should be authority");
	}

	@Test
	public void testReadAuthorized2() {
		SpiderDataRecord record = recordReader.readRecord("someToken78678567", "place",
				"place:0001");
		SpiderDataGroup groupOut = record.getSpiderDataGroup();
		assertEquals(groupOut.getNameInData(), "authority");

		AuthorizatorAlwaysAuthorizedSpy authorizatorSpy = ((AuthorizatorAlwaysAuthorizedSpy) authorizator);
		assertEquals(authorizatorSpy.actions.get(0), "read");
		assertEquals(authorizatorSpy.users.get(0).id, "12345");
		assertEquals(authorizatorSpy.recordTypes.get(0), "place");

		DataGroupTermCollectorSpy dataGroupTermCollectorSpy = (DataGroupTermCollectorSpy) termCollector;
		assertEquals(dataGroupTermCollectorSpy.metadataId, "place");
		assertEquals(dataGroupTermCollectorSpy.dataGroup,
				recordStorage.read("place", "place:0001"));

		assertEquals(authorizatorSpy.calledMethods.get(0),
				"checkUserIsAuthorizedForActionOnRecordTypeAndCollectedData");
		DataGroup returnedCollectedTerms = dataGroupTermCollectorSpy.collectedTerms;
		assertEquals(authorizatorSpy.collectedTerms.get(0), returnedCollectedTerms);
	}

	@Test
	public void testReadRecordAbstractRecordType() {
		recordStorage = new RecordStorageSpy();
		setUpDependencyProvider();
		SpiderDataRecord readRecord = recordReader.readRecord("someToken78678567",
				"abstractAuthority", "place:0001");
		assertNotNull(readRecord);

		AuthorizatorAlwaysAuthorizedSpy authorizatorSpy = ((AuthorizatorAlwaysAuthorizedSpy) authorizator);
		assertEquals(authorizatorSpy.actions.get(0), "read");
		assertEquals(authorizatorSpy.users.get(0).id, "12345");
		assertEquals(authorizatorSpy.recordTypes.get(0), "abstractAuthority");

		DataGroupTermCollectorSpy dataGroupTermCollectorSpy = (DataGroupTermCollectorSpy) termCollector;
		assertEquals(dataGroupTermCollectorSpy.metadataId, "place");
		assertEquals(dataGroupTermCollectorSpy.dataGroup,
				recordStorage.read("abstractAuthority", "place:0001"));

		assertEquals(authorizatorSpy.calledMethods.get(0),
				"checkUserIsAuthorizedForActionOnRecordTypeAndCollectedData");
		DataGroup returnedCollectedTerms = dataGroupTermCollectorSpy.collectedTerms;
		assertEquals(authorizatorSpy.collectedTerms.get(0), returnedCollectedTerms);
	}

	@Test
	public void testUnauthorizedForRecordTypeShouldNeverReadFromStorage() {
		recordStorage = new RecordStorageSpy();
		authorizator = new NeverAuthorisedStub();
		setUpDependencyProvider();

		boolean exceptionWasCaught = false;
		try {
			recordReader.readRecord("unauthorizedUserId", "place", "place:0001");
		} catch (Exception e) {
			assertEquals(e.getClass(), AuthorizationException.class);
			exceptionWasCaught = true;
		}
		assertTrue(exceptionWasCaught);
		assertEquals(((RecordStorageSpy) recordStorage).readWasCalled, false);
	}

	@Test
	public void testUnauthorizedForRulesAgainsRecord() {
		recordStorage = new RecordStorageSpy();
		authorizator = new AuthorizatorNotAuthorizedRequiredRulesButForActionOnRecordType();
		setUpDependencyProvider();

		boolean exceptionWasCaught = false;
		try {
			recordReader.readRecord("unauthorizedUserId", "place", "place:0001");
		} catch (Exception e) {
			assertEquals(e.getClass(), AuthorizationException.class);
			exceptionWasCaught = true;
		}
		assertTrue(exceptionWasCaught);
		assertEquals(((RecordStorageSpy) recordStorage).readWasCalled, true);
	}

	@Test(expectedExceptions = RecordNotFoundException.class)
	public void testReadingDataForANonExistingRecordType() {
		recordReader.readRecord("someToken78678567", "nonExistingRecordType", "anId");
	}

	@Test
	public void testReadPublicRecordType() throws Exception {
		recordStorage = new RecordStorageSpy();
		authorizator = new AuthorizatorNotAuthorizedRequiredRulesButForActionOnRecordType();
		setUpDependencyProvider();
		// publicReadType
		recordReader.readRecord("unauthorizedUserId", "publicReadType", "publicReadType:0001");

	}
}
