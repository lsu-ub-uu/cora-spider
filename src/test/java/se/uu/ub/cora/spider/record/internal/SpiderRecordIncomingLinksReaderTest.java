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
package se.uu.ub.cora.spider.record.internal;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertTrue;

import java.util.HashMap;

import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import se.uu.ub.cora.data.DataAtomic;
import se.uu.ub.cora.data.DataAtomicProvider;
import se.uu.ub.cora.data.DataGroup;
import se.uu.ub.cora.data.DataGroupFactory;
import se.uu.ub.cora.data.DataGroupProvider;
import se.uu.ub.cora.data.DataList;
import se.uu.ub.cora.data.DataListProvider;
import se.uu.ub.cora.data.copier.DataCopierFactory;
import se.uu.ub.cora.data.copier.DataCopierProvider;
import se.uu.ub.cora.logger.LoggerProvider;
import se.uu.ub.cora.spider.authentication.AuthenticationException;
import se.uu.ub.cora.spider.authentication.AuthenticatorSpy;
import se.uu.ub.cora.spider.authorization.AuthorizationException;
import se.uu.ub.cora.spider.authorization.PermissionRuleCalculator;
import se.uu.ub.cora.spider.data.DataAtomicFactorySpy;
import se.uu.ub.cora.spider.data.DataGroupFactorySpy;
import se.uu.ub.cora.spider.dependency.spy.SpiderDependencyProviderOldSpy;
import se.uu.ub.cora.spider.log.LoggerFactorySpy;
import se.uu.ub.cora.spider.record.DataCopierFactorySpy;
import se.uu.ub.cora.spider.record.DataListFactorySpy;
import se.uu.ub.cora.spider.record.IncomingLinksReader;
import se.uu.ub.cora.spider.record.MisuseException;
import se.uu.ub.cora.spider.spy.DataGroupTermCollectorSpy;
import se.uu.ub.cora.spider.spy.OldRecordStorageSpy;
import se.uu.ub.cora.spider.spy.RuleCalculatorSpy;
import se.uu.ub.cora.spider.spy.SpiderAuthorizatorSpy;
import se.uu.ub.cora.spider.testdata.DataRecordLinkSpy;
import se.uu.ub.cora.spider.testdata.TestDataRecordInMemoryStorage;
import se.uu.ub.cora.storage.RecordStorage;

public class SpiderRecordIncomingLinksReaderTest {

	private IncomingLinksReader incomingLinksReader;

	private RecordStorage recordStorage;
	private AuthenticatorSpy authenticator;
	private SpiderAuthorizatorSpy authorizator;
	private PermissionRuleCalculator keyCalculator;
	private SpiderDependencyProviderOldSpy dependencyProvider;
	private DataGroupTermCollectorSpy termCollector;
	private LoggerFactorySpy loggerFactorySpy;
	private DataGroupFactory dataGroupFactory;
	private DataAtomicFactorySpy dataAtomicFactory;
	private DataListFactorySpy dataListFactory;

	private DataCopierFactory dataCopierFactory;

	@BeforeMethod
	public void beforeMethod() {
		setUpFactoriesAndProviders();
		authenticator = new AuthenticatorSpy();
		authorizator = new SpiderAuthorizatorSpy();
		recordStorage = TestDataRecordInMemoryStorage.createRecordStorageInMemoryWithTestData();
		keyCalculator = new RuleCalculatorSpy();
		termCollector = new DataGroupTermCollectorSpy();
		setUpDependencyProvider();
	}

	private void setUpFactoriesAndProviders() {
		loggerFactorySpy = new LoggerFactorySpy();
		LoggerProvider.setLoggerFactory(loggerFactorySpy);
		dataGroupFactory = new DataGroupFactorySpy();
		DataGroupProvider.setDataGroupFactory(dataGroupFactory);
		dataAtomicFactory = new DataAtomicFactorySpy();
		DataAtomicProvider.setDataAtomicFactory(dataAtomicFactory);
		dataListFactory = new DataListFactorySpy();
		DataListProvider.setDataListFactory(dataListFactory);
		dataCopierFactory = new DataCopierFactorySpy();
		DataCopierProvider.setDataCopierFactory(dataCopierFactory);
	}

	private void setUpDependencyProvider() {
		dependencyProvider = new SpiderDependencyProviderOldSpy(new HashMap<>());
		dependencyProvider.authenticator = authenticator;
		dependencyProvider.spiderAuthorizator = authorizator;
		dependencyProvider.recordStorage = recordStorage;
		dependencyProvider.ruleCalculator = keyCalculator;
		dependencyProvider.termCollector = termCollector;

		incomingLinksReader = IncomingLinksReaderImp.usingDependencyProvider(dependencyProvider);
	}

	@Test
	public void testReadIncomingLinks() {
		DataList linksPointingToRecord = incomingLinksReader.readIncomingLinks("someToken78678567",
				"place", "place:0001");
		assertEquals(linksPointingToRecord.getTotalNumberOfTypeInStorage(), "1");
		assertEquals(linksPointingToRecord.getFromNo(), "1");
		assertEquals(linksPointingToRecord.getToNo(), "1");

		DataGroup link = (DataGroup) linksPointingToRecord.getDataList().iterator().next();
		assertEquals(link.getNameInData(), "recordToRecordLink");

		DataRecordLinkSpy from = (DataRecordLinkSpy) link.getFirstChildWithNameInData("from");
		DataAtomic linkedRecordType = (DataAtomic) from
				.getFirstChildWithNameInData("linkedRecordType");
		DataAtomic linkedRecordId = (DataAtomic) from.getFirstChildWithNameInData("linkedRecordId");

		assertEquals(linkedRecordType.getValue(), "place");
		assertEquals(linkedRecordId.getValue(), "place:0002");
		assertTrue(from.hasReadAction());

		DataRecordLinkSpy to = (DataRecordLinkSpy) link.getFirstChildWithNameInData("to");
		DataAtomic toLinkedRecordType = (DataAtomic) to
				.getFirstChildWithNameInData("linkedRecordType");
		DataAtomic toLinkedRecordId = (DataAtomic) to.getFirstChildWithNameInData("linkedRecordId");

		assertEquals(toLinkedRecordType.getValue(), "place");
		assertEquals(toLinkedRecordId.getValue(), "place:0001");

		String methodName = "checkUserIsAuthorizedForActionOnRecordTypeAndCollectedData";
		authorizator.MCR.assertParameters(methodName, 0, authenticator.returnedUser, "read",
				"place", termCollector.MCR.getReturnValue("collectTerms", 0));

		termCollector.MCR.assertParameter("collectTerms", 0, "metadataId", "place");
		termCollector.MCR.assertParameter("collectTerms", 0, "dataGroup",
				recordStorage.read("place", "place:0001"));
	}

	@Test
	public void testReadIncomingLinksWhenLinkPointsToParentRecordType() {
		DataList linksPointingToRecord = incomingLinksReader.readIncomingLinks("someToken78678567",
				"place", "place:0003");
		assertEquals(linksPointingToRecord.getTotalNumberOfTypeInStorage(), "1");
		assertEquals(linksPointingToRecord.getFromNo(), "1");
		assertEquals(linksPointingToRecord.getToNo(), "1");

		DataGroup link = (DataGroup) linksPointingToRecord.getDataList().iterator().next();
		assertEquals(link.getNameInData(), "recordToRecordLink");

		DataRecordLinkSpy from = (DataRecordLinkSpy) link.getFirstChildWithNameInData("from");
		DataAtomic linkedRecordType = (DataAtomic) from
				.getFirstChildWithNameInData("linkedRecordType");
		DataAtomic linkedRecordId = (DataAtomic) from.getFirstChildWithNameInData("linkedRecordId");

		assertEquals(linkedRecordType.getValue(), "place");
		assertEquals(linkedRecordId.getValue(), "place:0004");
		assertTrue(from.hasReadAction());

		DataRecordLinkSpy to = (DataRecordLinkSpy) link.getFirstChildWithNameInData("to");
		DataAtomic toLinkedRecordType = (DataAtomic) to
				.getFirstChildWithNameInData("linkedRecordType");
		DataAtomic toLinkedRecordId = (DataAtomic) to.getFirstChildWithNameInData("linkedRecordId");

		assertEquals(toLinkedRecordType.getValue(), "authority");
		assertEquals(toLinkedRecordId.getValue(), "place:0003");

	}

	@Test
	public void testReadIncomingLinksNoParentRecordTypeNoLinks() {
		DataList linksPointingToRecord = incomingLinksReader.readIncomingLinks("someToken78678567",
				"search", "aSearchId");
		assertEquals(linksPointingToRecord.getTotalNumberOfTypeInStorage(), "0");
		assertEquals(linksPointingToRecord.getFromNo(), "1");
		assertEquals(linksPointingToRecord.getToNo(), "0");

	}

	@Test(expectedExceptions = AuthenticationException.class)
	public void testReadIncomingLinksAuthenticationNotAuthenticated() {
		authenticator.throwAuthenticationException = true;
		recordStorage = new OldRecordStorageSpy();
		setUpDependencyProvider();
		incomingLinksReader.readIncomingLinks("dummyNonAuthenticatedToken", "place", "place:0001");
	}

	@Test(expectedExceptions = AuthorizationException.class)
	public void testReadIncomingLinksUnauthorized() {
		authorizator.authorizedForActionAndRecordTypeAndCollectedData = false;
		setUpDependencyProvider();
		incomingLinksReader.readIncomingLinks("unauthorizedUserId", "place", "place:0001");
	}

	@Test(expectedExceptions = MisuseException.class)
	public void testReadIncomingLinksAbstractType() {
		recordStorage = new OldRecordStorageSpy();
		setUpDependencyProvider();

		incomingLinksReader.readIncomingLinks("someToken78678567", "abstract", "place:0001");
	}
}
