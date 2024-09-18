/*
 * Copyright 2017, 2019, 2022, 2024 Uppsala University Library
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
package se.uu.ub.cora.spider.extended.workorder;

import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import se.uu.ub.cora.data.DataProvider;
import se.uu.ub.cora.data.DataRecordGroup;
import se.uu.ub.cora.data.DataRecordLink;
import se.uu.ub.cora.data.collected.CollectTerms;
import se.uu.ub.cora.data.spies.DataFactorySpy;
import se.uu.ub.cora.data.spies.DataRecordGroupSpy;
import se.uu.ub.cora.data.spies.DataRecordLinkSpy;
import se.uu.ub.cora.logger.LoggerProvider;
import se.uu.ub.cora.spider.extendedfunctionality.ExtendedFunctionalityData;
import se.uu.ub.cora.spider.log.LoggerFactorySpy;
import se.uu.ub.cora.spider.record.internal.AuthenticatorSpy;
import se.uu.ub.cora.spider.record.internal.SpiderAuthorizatorSpy;
import se.uu.ub.cora.spider.spy.DataGroupTermCollectorSpy;
import se.uu.ub.cora.spider.spy.RecordIndexerSpy;
import se.uu.ub.cora.spider.spy.SpiderDependencyProviderSpy;
import se.uu.ub.cora.storage.spies.RecordStorageSpy;

public class WorkOrderExecutorTest {
	SpiderDependencyProviderSpy dependencyProviderSpy;
	WorkOrderExecutor extendedFunctionality;
	DataGroupTermCollectorSpy termCollector;
	RecordIndexerSpy recordIndexer;
	SpiderAuthorizatorSpy authorizator;
	AuthenticatorSpy authenticator;
	private LoggerFactorySpy loggerFactorySpy;
	private DataFactorySpy dataFactorySpy;
	private RecordStorageSpy recordStorage;
	private DataRecordGroupSpy workOrder;

	@BeforeMethod
	public void setUp() {
		setUpFactoriesAndProviders();
		setUpDependencyProvider();
		createWorkOrderWithTypeAndId("book", "book1");
		extendedFunctionality = WorkOrderExecutor.usingDependencyProvider(dependencyProviderSpy);
	}

	private void createWorkOrderWithTypeAndId(String recordTypeToIndex, String recordIdToIndex) {
		DataRecordLinkSpy recordTypeLink = new DataRecordLinkSpy();
		recordTypeLink.MRV.setDefaultReturnValuesSupplier("getLinkedRecordId",
				() -> recordTypeToIndex);

		workOrder = new DataRecordGroupSpy();
		workOrder.MRV.setSpecificReturnValuesSupplier("getFirstChildOfTypeAndName",
				() -> recordTypeLink, DataRecordLink.class, "recordType");
		workOrder.MRV.setSpecificReturnValuesSupplier("getFirstAtomicValueWithNameInData",
				() -> recordIdToIndex, "recordId");
	}

	private void setUpIndexActionForWorkOrder(String indexAction) {
		workOrder.MRV.setSpecificReturnValuesSupplier("getFirstAtomicValueWithNameInData",
				() -> indexAction, "type");
	}

	private void setUpFactoriesAndProviders() {
		loggerFactorySpy = new LoggerFactorySpy();
		LoggerProvider.setLoggerFactory(loggerFactorySpy);
		dataFactorySpy = new DataFactorySpy();
		DataProvider.onlyForTestSetDataFactory(dataFactorySpy);
	}

	private void setUpDependencyProvider() {
		dependencyProviderSpy = new SpiderDependencyProviderSpy();
		recordStorage = new RecordStorageSpy();
		authorizator = new SpiderAuthorizatorSpy();
		authenticator = new AuthenticatorSpy();
		termCollector = new DataGroupTermCollectorSpy();
		recordIndexer = new RecordIndexerSpy();

		dependencyProviderSpy.MRV.setDefaultReturnValuesSupplier("getRecordStorage",
				() -> recordStorage);
		dependencyProviderSpy.MRV.setDefaultReturnValuesSupplier("getSpiderAuthorizator",
				() -> authorizator);
		dependencyProviderSpy.MRV.setDefaultReturnValuesSupplier("getAuthenticator",
				() -> authenticator);
		dependencyProviderSpy.MRV.setDefaultReturnValuesSupplier("getDataGroupTermCollector",
				() -> termCollector);
		dependencyProviderSpy.MRV.setDefaultReturnValuesSupplier("getRecordIndexer",
				() -> recordIndexer);
	}

	@Test
	public void testIndexData() {
		setUpIndexActionForWorkOrder("NotRemoveFromIndex");

		callExtendedFunctionalityWithGroup(workOrder);

		DataRecordGroupSpy dataToIndex = (DataRecordGroupSpy) recordStorage.MCR
				.assertCalledParametersReturn("read", "book", "book1");
		dependencyProviderSpy.MCR.assertCalledParameters("getRecordTypeHandlerUsingDataRecordGroup",
				dataToIndex);
		CollectTerms collectTerms = (CollectTerms) termCollector.MCR.assertCalledParametersReturn(
				"collectTerms", "fakeDefMetadataIdFromRecordTypeHandlerSpy", dataToIndex);
		recordIndexer.MCR.assertMethodWasCalled("indexData");
		recordIndexer.MCR.assertParameters("indexData", 0, "book", "book1", collectTerms.indexTerms,
				dataToIndex);
	}

	private void callExtendedFunctionalityWithGroup(DataRecordGroup workOrder) {
		ExtendedFunctionalityData data = new ExtendedFunctionalityData();
		data.authToken = "someToken";
		data.dataRecordGroup = workOrder;
		extendedFunctionality.useExtendedFunctionality(data);
	}

	@Test
	public void testIndexDataWithNoRightToIndexRecordType() {
		setUpIndexActionForWorkOrder("NotRemoveFromIndex");
		authorizator.MRV.setDefaultReturnValuesSupplier("userIsAuthorizedForActionOnRecordType",
				() -> false);

		callExtendedFunctionalityWithGroup(workOrder);

		termCollector.MCR.assertMethodNotCalled("collectTerms");
		termCollector.MCR.assertMethodNotCalled("indexData");
	}

	@Test
	public void testRemoveFromIndex() {
		setUpIndexActionForWorkOrder("removeFromIndex");

		callExtendedFunctionalityWithGroup(workOrder);

		recordIndexer.MCR.assertParameter("deleteFromIndex", 0, "type", "book");
		recordIndexer.MCR.assertParameter("deleteFromIndex", 0, "id", "book1");
	}

	@Test
	public void testRemoveFromIndexNoRightToIndexRecordType() {
		setUpIndexActionForWorkOrder("removeFromIndex");
		authorizator.MRV.setDefaultReturnValuesSupplier("userIsAuthorizedForActionOnRecordType",
				() -> false);

		callExtendedFunctionalityWithGroup(workOrder);

		recordIndexer.MCR.assertMethodNotCalled("deleteFromIndex");
	}

	@Test
	public void testIndexDataPerformCommitFalseInWorkOrder() {
		setUpIndexActionForWorkOrder("NotRemoveFromIndex");
		setUpPerfromCommitForWorkOrder("false");

		callExtendedFunctionalityWithGroup(workOrder);

		DataRecordGroupSpy dataToIndex = (DataRecordGroupSpy) recordStorage.MCR
				.assertCalledParametersReturn("read", "book", "book1");
		CollectTerms collectTerms = (CollectTerms) termCollector.MCR.assertCalledParametersReturn(
				"collectTerms", "fakeDefMetadataIdFromRecordTypeHandlerSpy", dataToIndex);
		recordIndexer.MCR.assertMethodNotCalled("indexData");
		recordIndexer.MCR.assertParameters("indexDataWithoutExplicitCommit", 0, "book", "book1",
				collectTerms.indexTerms, dataToIndex);
	}

	private void setUpPerfromCommitForWorkOrder(String performCommit) {
		workOrder.MRV.setSpecificReturnValuesSupplier("containsChildWithNameInData", () -> true,
				"performCommit");
		workOrder.MRV.setSpecificReturnValuesSupplier("getFirstAtomicValueWithNameInData",
				() -> performCommit, "performCommit");
	}

	@Test
	public void testIndexDataPerformCommitTrueInWorkOrder() {
		setUpIndexActionForWorkOrder("NotRemoveFromIndex");
		setUpPerfromCommitForWorkOrder("true");

		callExtendedFunctionalityWithGroup(workOrder);

		DataRecordGroupSpy dataToIndex = (DataRecordGroupSpy) recordStorage.MCR
				.assertCalledParametersReturn("read", "book", "book1");
		CollectTerms collectTerms = (CollectTerms) termCollector.MCR.assertCalledParametersReturn(
				"collectTerms", "fakeDefMetadataIdFromRecordTypeHandlerSpy", dataToIndex);
		recordIndexer.MCR.assertMethodNotCalled("indexDataWithoutExplicitCommit");
		recordIndexer.MCR.assertParameters("indexData", 0, "book", "book1", collectTerms.indexTerms,
				dataToIndex);
	}
}
