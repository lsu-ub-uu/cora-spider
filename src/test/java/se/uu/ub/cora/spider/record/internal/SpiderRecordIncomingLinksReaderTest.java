/*
 * Copyright 2017, 2018, 2019, 2022 Uppsala University Library
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

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Supplier;

import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import se.uu.ub.cora.data.DataList;
import se.uu.ub.cora.data.DataProvider;
import se.uu.ub.cora.data.collected.CollectTerms;
import se.uu.ub.cora.data.collected.Link;
import se.uu.ub.cora.data.spies.DataFactorySpy;
import se.uu.ub.cora.data.spies.DataGroupSpy;
import se.uu.ub.cora.data.spies.DataListSpy;
import se.uu.ub.cora.data.spies.DataRecordLinkSpy;
import se.uu.ub.cora.logger.LoggerProvider;
import se.uu.ub.cora.spider.authentication.AuthenticationException;
import se.uu.ub.cora.spider.authentication.OldAuthenticatorSpy;
import se.uu.ub.cora.spider.authorization.AuthorizationException;
import se.uu.ub.cora.spider.authorization.PermissionRuleCalculator;
import se.uu.ub.cora.spider.dependency.spy.RecordTypeHandlerSpy;
import se.uu.ub.cora.spider.dependency.spy.SpiderDependencyProviderOldSpy;
import se.uu.ub.cora.spider.log.LoggerFactorySpy;
import se.uu.ub.cora.spider.record.IncomingLinksReader;
import se.uu.ub.cora.spider.spy.DataGroupTermCollectorSpy;
import se.uu.ub.cora.spider.spy.OldSpiderAuthorizatorSpy;
import se.uu.ub.cora.spider.spy.RuleCalculatorSpy;
import se.uu.ub.cora.storage.spies.RecordStorageSpy;

public class SpiderRecordIncomingLinksReaderTest {
	private LoggerFactorySpy loggerFactorySpy;
	private DataFactorySpy dataFactorySpy;

	private IncomingLinksReader incomingLinksReader;

	private RecordStorageSpy recordStorage;
	private OldAuthenticatorSpy authenticator;
	private OldSpiderAuthorizatorSpy authorizator;
	private PermissionRuleCalculator keyCalculator;
	private SpiderDependencyProviderOldSpy dependencyProvider;
	private DataGroupTermCollectorSpy termCollector;

	private RecordTypeHandlerSpy recordTypeHandlerSpy;

	@BeforeMethod
	public void beforeMethod() {
		setUpFactoriesAndProviders();
		authenticator = new OldAuthenticatorSpy();
		authorizator = new OldSpiderAuthorizatorSpy();
		recordStorage = new RecordStorageSpy();
		keyCalculator = new RuleCalculatorSpy();
		termCollector = new DataGroupTermCollectorSpy();
		setUpDependencyProvider();
	}

	private void setUpFactoriesAndProviders() {
		dataFactorySpy = new DataFactorySpy();
		DataProvider.onlyForTestSetDataFactory(dataFactorySpy);
		loggerFactorySpy = new LoggerFactorySpy();
		LoggerProvider.setLoggerFactory(loggerFactorySpy);
	}

	private void setUpDependencyProvider() {
		dependencyProvider = new SpiderDependencyProviderOldSpy();
		dependencyProvider.authenticator = authenticator;
		dependencyProvider.spiderAuthorizator = authorizator;
		dependencyProvider.recordStorage = recordStorage;
		dependencyProvider.ruleCalculator = keyCalculator;
		dependencyProvider.termCollector = termCollector;
		incomingLinksReader = IncomingLinksReaderImp.usingDependencyProvider(dependencyProvider);
		recordTypeHandlerSpy = dependencyProvider.recordTypeHandlerSpy;
	}

	@Test
	public void testReadIncomingLinksNoParentRecordTypeNoLinks() {
		DataListSpy linksPointingToRecord = (DataListSpy) incomingLinksReader
				.readIncomingLinks("someToken78678567", "search", "aSearchId");

		linksPointingToRecord.MCR.assertParameters("setFromNo", 0, "1");
		linksPointingToRecord.MCR.assertParameters("setToNo", 0, "0");
		linksPointingToRecord.MCR.assertParameters("setTotalNo", 0, "0");
		linksPointingToRecord.MCR.assertMethodNotCalled("addData");
	}

	@Test
	public void testStorageIsCalledToGetLinksToReturn() throws Exception {
		Set<Link> links = new LinkedHashSet<>();
		links.add(new Link("type0", "id0"));
		links.add(new Link("type1", "id1"));
		recordStorage.MRV.setDefaultReturnValuesSupplier("getLinksToRecord", () -> links);

		DataListSpy linksPointingToRecord = (DataListSpy) incomingLinksReader
				.readIncomingLinks("someToken78678567", "aType", "anId");

		recordTypeHandlerSpy.MCR.assertMethodNotCalled("getParentId");
		recordStorage.MCR.assertParameters("getLinksToRecord", 0, "aType", "anId");
		linksPointingToRecord.MCR.assertParameters("setTotalNo", 0, "2");
		linksPointingToRecord.MCR.assertParameters("setToNo", 0, "2");
		linksPointingToRecord.MCR.assertParameters("setFromNo", 0, "1");

		extracted(linksPointingToRecord, 0);
		extracted(linksPointingToRecord, 1);
	}

	private void extracted(DataListSpy linksPointingToRecord, int linkNo) {
		dataFactorySpy.MCR.assertParameters("factorGroupUsingNameInData", linkNo,
				"recordToRecordLink");
		DataGroupSpy rTRLink0 = (DataGroupSpy) dataFactorySpy.MCR
				.getReturnValue("factorGroupUsingNameInData", linkNo);
		linksPointingToRecord.MCR.assertParameters("addData", linkNo, rTRLink0);

		dataFactorySpy.MCR.assertParameters("factorRecordLinkUsingNameInDataAndTypeAndId",
				linkNo * 2, "from", "type" + linkNo, "id" + linkNo);
		DataRecordLinkSpy fFrom = (DataRecordLinkSpy) dataFactorySpy.MCR
				.getReturnValue("factorRecordLinkUsingNameInDataAndTypeAndId", linkNo * 2);
		rTRLink0.MCR.assertParameters("addChild", 0, fFrom);
		fFrom.MCR.assertParameters("addAction", 0, se.uu.ub.cora.data.Action.READ);

		dataFactorySpy.MCR.assertParameters("factorRecordLinkUsingNameInDataAndTypeAndId",
				linkNo * 2 + 1, "to", "aType", "anId");
		DataRecordLinkSpy fTo = (DataRecordLinkSpy) dataFactorySpy.MCR
				.getReturnValue("factorRecordLinkUsingNameInDataAndTypeAndId", linkNo * 2 + 1);
		rTRLink0.MCR.assertParameters("addChild", 1, fTo);
	}

	@Test
	public void testStorageIsCalledToGetLinksToReturnForParentTypeIfAbstract() throws Exception {
		recordTypeHandlerSpy.MRV.setDefaultReturnValuesSupplier("hasParent", () -> true);
		recordTypeHandlerSpy.MRV.setDefaultReturnValuesSupplier("getParentId",
				(Supplier<String>) () -> "someParentId");

		Link link0 = new Link("type0", "id0");
		Link link1 = new Link("type1", "id1");
		Link link2 = new Link("type2", "id2");
		recordStorage.MRV.setSpecificReturnValuesSupplier("getLinksToRecord", () -> Set.of(link0),
				"aType", "anId");
		Set<Link> links = new LinkedHashSet<>();
		links.add(link1);
		links.add(link2);
		recordStorage.MRV.setSpecificReturnValuesSupplier("getLinksToRecord", () -> links,
				"someParentId", "anId");

		DataListSpy linksPointingToRecord = (DataListSpy) incomingLinksReader
				.readIncomingLinks("someToken78678567", "aType", "anId");

		recordStorage.MCR.assertParameters("getLinksToRecord", 0, "aType", "anId");

		recordTypeHandlerSpy.MCR.assertMethodWasCalled("hasParent");

		var parentId = recordTypeHandlerSpy.MCR.getReturnValue("getParentId", 0);
		recordStorage.MCR.assertParameters("getLinksToRecord", 1, parentId, "anId");

		linksPointingToRecord.MCR.assertParameters("setToNo", 0, "3");
		linksPointingToRecord.MCR.assertParameters("setFromNo", 0, "1");
		linksPointingToRecord.MCR.assertParameters("setTotalNo", 0, "3");

		extracted(linksPointingToRecord, 0);
		extracted(linksPointingToRecord, 1);
		extracted(linksPointingToRecord, 2);

	}

	@Test
	public void testDependenciesAreConnectedCorrectly() throws Exception {
		DataList linksPointingToRecord = incomingLinksReader.readIncomingLinks("someToken78678567",
				"search", "aSearchId");

		dependencyProvider.MCR.assertParameters("getRecordTypeHandler", 0, "search");
		dependencyProvider.MCR.assertNumberOfCallsToMethod("getRecordTypeHandler", 1);

		// var metadataIdForType = recordTypeHandlerSpy.MCR.getReturnValue("getMetadataId", 0);
		var metadataIdForType = recordTypeHandlerSpy.MCR.getReturnValue("getDefinitionId", 0);
		List<?> types = (List<?>) recordStorage.MCR
				.getValueForMethodNameAndCallNumberAndParameterName("read", 0, "types");
		assertEquals(types.get(0), "search");
		recordStorage.MCR.assertParameter("read", 0, "id", "aSearchId");
		recordStorage.MCR.assertNumberOfCallsToMethod("read", 1);
		var recordRead = recordStorage.MCR.getReturnValue("read", 0);

		termCollector.MCR.assertParameters("collectTerms", 0, metadataIdForType, recordRead);
		CollectTerms collectTerms = (CollectTerms) termCollector.MCR.getReturnValue("collectTerms",
				0);

		authenticator.MCR.assertParameters("getUserForToken", 0, "someToken78678567");
		var user = authenticator.MCR.getReturnValue("getUserForToken", 0);
		authorizator.MCR.assertParameters(
				"checkUserIsAuthorizedForActionOnRecordTypeAndCollectedData", 0, user, "read",
				"search", collectTerms.permissionTerms);

		dataFactorySpy.MCR.assertParameters("factorListUsingNameOfDataType", 0,
				"recordToRecordLink");
		dataFactorySpy.MCR.assertReturn("factorListUsingNameOfDataType", 0, linksPointingToRecord);
	}

	@Test(expectedExceptions = AuthenticationException.class)
	public void testReadIncomingLinksAuthenticationNotAuthenticated() {
		authenticator.throwAuthenticationException = true;

		incomingLinksReader.readIncomingLinks("dummyNonAuthenticatedToken", "place", "place:0001");
	}

	@Test(expectedExceptions = AuthorizationException.class)
	public void testReadIncomingLinksUnauthorized() {
		authorizator.authorizedForActionAndRecordTypeAndCollectedData = false;

		incomingLinksReader.readIncomingLinks("unauthorizedUserId", "place", "place:0001");
	}

}
