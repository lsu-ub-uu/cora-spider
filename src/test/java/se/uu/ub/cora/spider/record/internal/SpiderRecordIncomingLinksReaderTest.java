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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.function.Supplier;

import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import se.uu.ub.cora.data.Action;
import se.uu.ub.cora.data.DataGroup;
import se.uu.ub.cora.data.DataList;
import se.uu.ub.cora.data.DataProvider;
import se.uu.ub.cora.data.DataRecordLink;
import se.uu.ub.cora.data.collected.CollectTerms;
import se.uu.ub.cora.logger.LoggerProvider;
import se.uu.ub.cora.spider.authentication.AuthenticationException;
import se.uu.ub.cora.spider.authentication.AuthenticatorSpy;
import se.uu.ub.cora.spider.authorization.AuthorizationException;
import se.uu.ub.cora.spider.authorization.PermissionRuleCalculator;
import se.uu.ub.cora.spider.dependency.spy.RecordTypeHandlerSpy;
import se.uu.ub.cora.spider.dependency.spy.SpiderDependencyProviderOldSpy;
import se.uu.ub.cora.spider.log.LoggerFactorySpy;
import se.uu.ub.cora.spider.record.IncomingLinksReader;
import se.uu.ub.cora.spider.record.MisuseException;
import se.uu.ub.cora.spider.spy.DataGroupTermCollectorSpy;
import se.uu.ub.cora.spider.spy.RecordStorageMCRSpy;
import se.uu.ub.cora.spider.spy.RuleCalculatorSpy;
import se.uu.ub.cora.spider.spy.SpiderAuthorizatorSpy;
import se.uu.ub.cora.testspies.data.DataFactorySpy;
import se.uu.ub.cora.testspies.data.DataGroupSpy;
import se.uu.ub.cora.testspies.data.DataListSpy;
import se.uu.ub.cora.testspies.data.DataRecordLinkSpy;

public class SpiderRecordIncomingLinksReaderTest {
	private LoggerFactorySpy loggerFactorySpy;
	private DataFactorySpy dataFactorySpy;

	private IncomingLinksReader incomingLinksReader;

	private RecordStorageMCRSpy recordStorage;
	private AuthenticatorSpy authenticator;
	private SpiderAuthorizatorSpy authorizator;
	private PermissionRuleCalculator keyCalculator;
	private SpiderDependencyProviderOldSpy dependencyProvider;
	private DataGroupTermCollectorSpy termCollector;

	private RecordTypeHandlerSpy recordTypeHandlerSpy;

	@BeforeMethod
	public void beforeMethod() {
		setUpFactoriesAndProviders();
		authenticator = new AuthenticatorSpy();
		authorizator = new SpiderAuthorizatorSpy();
		recordStorage = new RecordStorageMCRSpy();
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
		dependencyProvider = new SpiderDependencyProviderOldSpy(new HashMap<>());
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
		List<DataGroup> linksAsDataGroupFromStorage = new ArrayList<>();
		DataGroupSpy dataGroupSpy1 = new DataGroupSpy();
		DataRecordLinkSpy dataRecordLinkSpy1 = new DataRecordLinkSpy();
		dataGroupSpy1.MRV.setSpecificReturnValuesSupplier("getFirstChildWithNameInData",
				(Supplier<DataRecordLink>) () -> dataRecordLinkSpy1, "from");
		linksAsDataGroupFromStorage.add(dataGroupSpy1);

		DataGroupSpy dataGroupSpy2 = new DataGroupSpy();
		DataRecordLinkSpy dataRecordLinkSpy2 = new DataRecordLinkSpy();
		dataGroupSpy2.MRV.setSpecificReturnValuesSupplier("getFirstChildWithNameInData",
				(Supplier<DataRecordLink>) () -> dataRecordLinkSpy2, "from");
		linksAsDataGroupFromStorage.add(dataGroupSpy2);

		recordStorage.MRV.setDefaultReturnValuesSupplier("generateLinkCollectionPointingToRecord",
				(Supplier<List<DataGroup>>) () -> linksAsDataGroupFromStorage);

		DataListSpy linksPointingToRecord = (DataListSpy) incomingLinksReader
				.readIncomingLinks("someToken78678567", "aType", "anId");

		recordTypeHandlerSpy.MCR.assertMethodNotCalled("getParentId");
		recordStorage.MCR.assertParameters("generateLinkCollectionPointingToRecord", 0, "aType",
				"anId");
		linksPointingToRecord.MCR.assertParameters("setTotalNo", 0, "2");
		linksPointingToRecord.MCR.assertParameters("setToNo", 0, "2");
		linksPointingToRecord.MCR.assertParameters("setFromNo", 0, "1");

		linksPointingToRecord.MCR.assertParameters("addData", 0, dataGroupSpy1);
		dataRecordLinkSpy1.MCR.assertParameters("addAction", 0, Action.READ);

		linksPointingToRecord.MCR.assertParameters("addData", 1, dataGroupSpy2);
		dataRecordLinkSpy2.MCR.assertParameters("addAction", 0, Action.READ);

	}

	@Test
	public void testStorageIsCalledToGetLinksToReturnForParentTypeIfAbstract() throws Exception {
		// recordTypeHandlerSpy.MRV.setDefaultReturnValuesSupplier("hasParent",
		// (Supplier<Boolean>) () -> true);
		recordTypeHandlerSpy.hasParent = true;
		recordTypeHandlerSpy.MRV.setDefaultReturnValuesSupplier("getParentId",
				(Supplier<String>) () -> "someParentId");

		DataGroupSpy dataGroupSpy0 = new DataGroupSpy();
		DataRecordLinkSpy dataRecordLinkSpy0 = new DataRecordLinkSpy();
		dataGroupSpy0.MRV.setSpecificReturnValuesSupplier("getFirstChildWithNameInData",
				(Supplier<DataRecordLink>) () -> dataRecordLinkSpy0, "from");

		List<DataGroup> linksAsDataGroupFromStorage = new ArrayList<>();
		DataGroupSpy dataGroupSpy1 = new DataGroupSpy();
		DataRecordLinkSpy dataRecordLinkSpy1 = new DataRecordLinkSpy();
		dataGroupSpy1.MRV.setSpecificReturnValuesSupplier("getFirstChildWithNameInData",
				(Supplier<DataRecordLink>) () -> dataRecordLinkSpy1, "from");
		linksAsDataGroupFromStorage.add(dataGroupSpy1);

		DataGroupSpy dataGroupSpy2 = new DataGroupSpy();
		DataRecordLinkSpy dataRecordLinkSpy2 = new DataRecordLinkSpy();
		dataGroupSpy2.MRV.setSpecificReturnValuesSupplier("getFirstChildWithNameInData",
				(Supplier<DataRecordLink>) () -> dataRecordLinkSpy2, "from");
		linksAsDataGroupFromStorage.add(dataGroupSpy2);

		recordStorage.MRV.setSpecificReturnValuesSupplier("generateLinkCollectionPointingToRecord",
				(Supplier<List<DataGroup>>) () -> linksAsDataGroupFromStorage, "someParentId",
				"anId");

		recordStorage.MRV.setSpecificReturnValuesSupplier("generateLinkCollectionPointingToRecord",
				(Supplier<List<DataGroup>>) () -> List.of(dataGroupSpy0), "aType", "anId");

		DataListSpy linksPointingToRecord = (DataListSpy) incomingLinksReader
				.readIncomingLinks("someToken78678567", "aType", "anId");

		recordStorage.MCR.assertParameters("generateLinkCollectionPointingToRecord", 0, "aType",
				"anId");

		recordTypeHandlerSpy.MCR.assertMethodWasCalled("hasParent");

		var parentId = recordTypeHandlerSpy.MCR.getReturnValue("getParentId", 0);
		recordStorage.MCR.assertParameters("generateLinkCollectionPointingToRecord", 1, parentId,
				"anId");

		linksPointingToRecord.MCR.assertParameters("setToNo", 0, "3");
		linksPointingToRecord.MCR.assertParameters("setFromNo", 0, "1");
		linksPointingToRecord.MCR.assertParameters("setTotalNo", 0, "3");

		linksPointingToRecord.MCR.assertParameters("addData", 1, dataGroupSpy1);
		dataRecordLinkSpy1.MCR.assertParameters("addAction", 0, Action.READ);

		linksPointingToRecord.MCR.assertParameters("addData", 2, dataGroupSpy2);
		dataRecordLinkSpy2.MCR.assertParameters("addAction", 0, Action.READ);

	}

	@Test
	public void testDependenciesAreConnectedCorrectly() throws Exception {
		DataList linksPointingToRecord = incomingLinksReader.readIncomingLinks("someToken78678567",
				"search", "aSearchId");

		dependencyProvider.MCR.assertParameters("getRecordTypeHandler", 0, "search");
		dependencyProvider.MCR.assertNumberOfCallsToMethod("getRecordTypeHandler", 1);

		var metadataIdForType = recordTypeHandlerSpy.MCR.getReturnValue("getMetadataId", 0);
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

	@Test(expectedExceptions = MisuseException.class, expectedExceptionsMessageRegExp = ""
			+ "Read incomming links is not allowed for abstract "
			+ "recordType: kalleAnka and recordId: place:0001")
	public void testReadIncomingLinksAbstractType() {
		recordTypeHandlerSpy.isAbstract = true;

		incomingLinksReader.readIncomingLinks("someToken78678567", "kalleAnka", "place:0001");
	}
}
