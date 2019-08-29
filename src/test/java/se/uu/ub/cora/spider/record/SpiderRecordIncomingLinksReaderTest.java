/*
 * Copyright 2017, 2018, 2019 Uppsala University Library
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
import static org.testng.Assert.assertTrue;

import java.util.HashMap;

import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import se.uu.ub.cora.bookkeeper.termcollector.DataGroupTermCollector;
import se.uu.ub.cora.data.DataGroup;
import se.uu.ub.cora.logger.LoggerProvider;
import se.uu.ub.cora.spider.authentication.AuthenticationException;
import se.uu.ub.cora.spider.authentication.Authenticator;
import se.uu.ub.cora.spider.authentication.AuthenticatorSpy;
import se.uu.ub.cora.spider.authorization.AuthorizationException;
import se.uu.ub.cora.spider.authorization.NeverAuthorisedStub;
import se.uu.ub.cora.spider.authorization.PermissionRuleCalculator;
import se.uu.ub.cora.spider.authorization.SpiderAuthorizator;
import se.uu.ub.cora.spider.data.Action;
import se.uu.ub.cora.spider.data.SpiderDataAtomic;
import se.uu.ub.cora.spider.data.SpiderDataGroup;
import se.uu.ub.cora.spider.data.SpiderDataList;
import se.uu.ub.cora.spider.data.SpiderDataRecordLink;
import se.uu.ub.cora.spider.dependency.RecordStorageProviderSpy;
import se.uu.ub.cora.spider.dependency.SpiderDependencyProviderSpy;
import se.uu.ub.cora.spider.log.LoggerFactorySpy;
import se.uu.ub.cora.spider.spy.AuthorizatorAlwaysAuthorizedSpy;
import se.uu.ub.cora.spider.spy.DataGroupTermCollectorSpy;
import se.uu.ub.cora.spider.spy.NoRulesCalculatorStub;
import se.uu.ub.cora.spider.spy.RecordStorageSpy;
import se.uu.ub.cora.spider.testdata.TestDataRecordInMemoryStorage;
import se.uu.ub.cora.storage.RecordStorage;

public class SpiderRecordIncomingLinksReaderTest {

	private SpiderRecordIncomingLinksReader incomingLinksReader;

	private RecordStorage recordStorage;
	private Authenticator authenticator;
	private SpiderAuthorizator authorizator;
	private PermissionRuleCalculator keyCalculator;
	private SpiderDependencyProviderSpy dependencyProvider;
	private DataGroupTermCollector termCollector;
	private LoggerFactorySpy loggerFactorySpy;

	@BeforeMethod
	public void beforeMethod() {
		loggerFactorySpy = new LoggerFactorySpy();
		LoggerProvider.setLoggerFactory(loggerFactorySpy);
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

		RecordStorageProviderSpy recordStorageProviderSpy = new RecordStorageProviderSpy();
		recordStorageProviderSpy.recordStorage = recordStorage;
		dependencyProvider.setRecordStorageProvider(recordStorageProviderSpy);

		dependencyProvider.ruleCalculator = keyCalculator;
		dependencyProvider.searchTermCollector = termCollector;

		incomingLinksReader = SpiderRecordIncomingLinksReaderImp
				.usingDependencyProvider(dependencyProvider);
	}

	@Test
	public void testReadIncomingLinks() {
		SpiderDataList linksPointingToRecord = incomingLinksReader
				.readIncomingLinks("someToken78678567", "place", "place:0001");
		assertEquals(linksPointingToRecord.getTotalNumberOfTypeInStorage(), "1");
		assertEquals(linksPointingToRecord.getFromNo(), "1");
		assertEquals(linksPointingToRecord.getToNo(), "1");

		SpiderDataGroup link = (SpiderDataGroup) linksPointingToRecord.getDataList().iterator()
				.next();
		assertEquals(link.getNameInData(), "recordToRecordLink");

		SpiderDataRecordLink from = (SpiderDataRecordLink) link.getFirstChildWithNameInData("from");
		SpiderDataAtomic linkedRecordType = (SpiderDataAtomic) from
				.getFirstChildWithNameInData("linkedRecordType");
		SpiderDataAtomic linkedRecordId = (SpiderDataAtomic) from
				.getFirstChildWithNameInData("linkedRecordId");

		assertEquals(linkedRecordType.getValue(), "place");
		assertEquals(linkedRecordId.getValue(), "place:0002");
		assertEquals(from.getActions().size(), 1);
		assertTrue(from.getActions().contains(Action.READ));

		SpiderDataRecordLink to = (SpiderDataRecordLink) link.getFirstChildWithNameInData("to");
		SpiderDataAtomic toLinkedRecordType = (SpiderDataAtomic) to
				.getFirstChildWithNameInData("linkedRecordType");
		SpiderDataAtomic toLinkedRecordId = (SpiderDataAtomic) to
				.getFirstChildWithNameInData("linkedRecordId");

		assertEquals(toLinkedRecordType.getValue(), "place");
		assertEquals(toLinkedRecordId.getValue(), "place:0001");
		//
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
	public void testReadIncomingLinksWhenLinkPointsToParentRecordType() {
		SpiderDataList linksPointingToRecord = incomingLinksReader
				.readIncomingLinks("someToken78678567", "place", "place:0003");
		assertEquals(linksPointingToRecord.getTotalNumberOfTypeInStorage(), "1");
		assertEquals(linksPointingToRecord.getFromNo(), "1");
		assertEquals(linksPointingToRecord.getToNo(), "1");

		SpiderDataGroup link = (SpiderDataGroup) linksPointingToRecord.getDataList().iterator()
				.next();
		assertEquals(link.getNameInData(), "recordToRecordLink");

		SpiderDataRecordLink from = (SpiderDataRecordLink) link.getFirstChildWithNameInData("from");
		SpiderDataAtomic linkedRecordType = (SpiderDataAtomic) from
				.getFirstChildWithNameInData("linkedRecordType");
		SpiderDataAtomic linkedRecordId = (SpiderDataAtomic) from
				.getFirstChildWithNameInData("linkedRecordId");

		assertEquals(linkedRecordType.getValue(), "place");
		assertEquals(linkedRecordId.getValue(), "place:0004");
		assertEquals(from.getActions().size(), 1);
		assertTrue(from.getActions().contains(Action.READ));

		SpiderDataRecordLink to = (SpiderDataRecordLink) link.getFirstChildWithNameInData("to");
		SpiderDataAtomic toLinkedRecordType = (SpiderDataAtomic) to
				.getFirstChildWithNameInData("linkedRecordType");
		SpiderDataAtomic toLinkedRecordId = (SpiderDataAtomic) to
				.getFirstChildWithNameInData("linkedRecordId");

		assertEquals(toLinkedRecordType.getValue(), "authority");
		assertEquals(toLinkedRecordId.getValue(), "place:0003");

	}

	@Test
	public void testReadIncomingLinksNoParentRecordTypeNoLinks() {
		SpiderDataList linksPointingToRecord = incomingLinksReader
				.readIncomingLinks("someToken78678567", "search", "aSearchId");
		assertEquals(linksPointingToRecord.getTotalNumberOfTypeInStorage(), "0");
		assertEquals(linksPointingToRecord.getFromNo(), "1");
		assertEquals(linksPointingToRecord.getToNo(), "0");

	}

	@Test(expectedExceptions = AuthenticationException.class)
	public void testReadIncomingLinksAuthenticationNotAuthenticated() {
		recordStorage = new RecordStorageSpy();
		setUpDependencyProvider();
		incomingLinksReader.readIncomingLinks("dummyNonAuthenticatedToken", "place", "place:0001");
	}

	@Test(expectedExceptions = AuthorizationException.class)
	public void testReadIncomingLinksUnauthorized() {
		authorizator = new NeverAuthorisedStub();
		setUpDependencyProvider();
		incomingLinksReader.readIncomingLinks("unauthorizedUserId", "place", "place:0001");
	}

	@Test(expectedExceptions = MisuseException.class)
	public void testReadIncomingLinksAbstractType() {
		recordStorage = new RecordStorageSpy();
		setUpDependencyProvider();

		incomingLinksReader.readIncomingLinks("someToken78678567", "abstract", "place:0001");
	}
}
