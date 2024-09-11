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

import se.uu.ub.cora.data.DataGroup;
import se.uu.ub.cora.data.DataProvider;
import se.uu.ub.cora.data.collected.CollectTerms;
import se.uu.ub.cora.data.spies.DataFactorySpy;
import se.uu.ub.cora.data.spies.DataRecordGroupSpy;
import se.uu.ub.cora.logger.LoggerProvider;
import se.uu.ub.cora.spider.authentication.OldAuthenticatorSpy;
import se.uu.ub.cora.spider.data.DataAtomicOldSpy;
import se.uu.ub.cora.spider.dependency.spy.SpiderDependencyProviderOldSpy;
import se.uu.ub.cora.spider.extendedfunctionality.ExtendedFunctionalityData;
import se.uu.ub.cora.spider.log.LoggerFactorySpy;
import se.uu.ub.cora.spider.spy.DataGroupTermCollectorSpy;
import se.uu.ub.cora.spider.spy.OldSpiderAuthorizatorSpy;
import se.uu.ub.cora.spider.spy.RecordIndexerSpy;
import se.uu.ub.cora.spider.testdata.DataCreator2;
import se.uu.ub.cora.storage.spies.RecordStorageSpy;

public class WorkOrderExecutorTest {
	SpiderDependencyProviderOldSpy dependencyProvider;
	WorkOrderExecutor extendedFunctionality;
	DataGroupTermCollectorSpy termCollector;
	RecordIndexerSpy recordIndexer;
	OldSpiderAuthorizatorSpy authorizator;
	OldAuthenticatorSpy authenticator;
	private LoggerFactorySpy loggerFactorySpy;
	private DataFactorySpy dataFactorySpy;
	private RecordStorageSpy recordStorage;

	@BeforeMethod
	public void setUp() {
		setUpFactoriesAndProviders();
		recordStorage = new RecordStorageSpy();

		setUpDependencyProvider();
	}

	private void setUpFactoriesAndProviders() {
		loggerFactorySpy = new LoggerFactorySpy();
		LoggerProvider.setLoggerFactory(loggerFactorySpy);
		dataFactorySpy = new DataFactorySpy();
		DataProvider.onlyForTestSetDataFactory(dataFactorySpy);
	}

	private void setUpDependencyProvider() {
		dependencyProvider = new SpiderDependencyProviderOldSpy();
		dependencyProvider.recordIndexer = new RecordIndexerSpy();
		dependencyProvider.termCollector = new DataGroupTermCollectorSpy();
		dependencyProvider.recordStorage = recordStorage;
		dependencyProvider.authenticator = new OldAuthenticatorSpy();
		dependencyProvider.spiderAuthorizator = new OldSpiderAuthorizatorSpy();
		extendedFunctionality = WorkOrderExecutor.usingDependencyProvider(dependencyProvider);
		termCollector = (DataGroupTermCollectorSpy) dependencyProvider.getDataGroupTermCollector();
		recordIndexer = (RecordIndexerSpy) dependencyProvider.getRecordIndexer();
		authorizator = (OldSpiderAuthorizatorSpy) dependencyProvider.getSpiderAuthorizator();
		authenticator = (OldAuthenticatorSpy) dependencyProvider.getAuthenticator();

		recordStorage.MRV.setDefaultReturnValuesSupplier("read", DataRecordGroupSpy::new);
	}

	@Test
	public void testIndexData() {
		DataGroup workOrder = DataCreator2.createWorkOrderWithIdAndRecordTypeAndRecordIdToIndex(
				"someGeneratedId", "book", "book1");

		callExtendedFunctionalityWithGroup(workOrder);

		DataRecordGroupSpy dataToIndex = (DataRecordGroupSpy) recordStorage.MCR
				.assertCalledParametersReturn("read", "book", "book1");
		dependencyProvider.MCR.assertCalledParameters("getRecordTypeHandlerUsingDataRecordGroup",
				dataToIndex);
		CollectTerms collectTerms = (CollectTerms) termCollector.MCR.assertCalledParametersReturn(
				"collectTerms", "fakeDefMetadataIdFromRecordTypeHandlerSpy", dataToIndex);
		recordIndexer.MCR.assertMethodWasCalled("indexData");
		recordIndexer.MCR.assertParameters("indexData", 0, "book", "book1", collectTerms.indexTerms,
				dataToIndex);
	}

	private void callExtendedFunctionalityWithGroup(DataGroup workOrder) {
		ExtendedFunctionalityData data = new ExtendedFunctionalityData();
		data.authToken = "someToken";
		data.dataGroup = workOrder;
		extendedFunctionality.useExtendedFunctionality(data);
	}

	@Test
	public void testIndexDataWithNoRightToIndexRecordType() {
		authorizator.setNotAutorizedForActionOnRecordType("index", "book");

		DataGroup workOrder = DataCreator2.createWorkOrderWithIdAndRecordTypeAndRecordIdToIndex(
				"someGeneratedId", "book", "book1");
		callExtendedFunctionalityWithGroup(workOrder);

		termCollector.MCR.assertMethodNotCalled("collectTerms");
		termCollector.MCR.assertMethodNotCalled("indexData");
	}

	@Test
	public void testRemoveFromIndex() {
		DataGroup workOrder = DataCreator2.createWorkOrderWithIdRecordTypeRecordIdAndWorkOrderType(
				"someGeneratedId", "book", "book1", "removeFromIndex");

		callExtendedFunctionalityWithGroup(workOrder);
		recordIndexer.MCR.assertParameter("deleteFromIndex", 0, "type", "book");
		recordIndexer.MCR.assertParameter("deleteFromIndex", 0, "id", "book1");
	}

	@Test
	public void testRemoveFromIndexNoRightToIndexRecordType() {
		authorizator.setNotAutorizedForActionOnRecordType("index", "book");

		DataGroup workOrder = DataCreator2.createWorkOrderWithIdRecordTypeRecordIdAndWorkOrderType(
				"someGeneratedId", "book", "book1", "removeFromIndex");

		callExtendedFunctionalityWithGroup(workOrder);

		recordIndexer.MCR.assertMethodNotCalled("deleteFromIndex");
	}

	@Test
	public void testIndexDataPerformCommitFalseInWorkOrder() {
		DataGroup workOrder = DataCreator2.createWorkOrderWithIdAndRecordTypeAndRecordIdToIndex(
				"someGeneratedId", "book", "book1");
		workOrder.addChild(new DataAtomicOldSpy("performCommit", "false"));

		callExtendedFunctionalityWithGroup(workOrder);

		DataRecordGroupSpy dataToIndex = (DataRecordGroupSpy) recordStorage.MCR
				.assertCalledParametersReturn("read", "book", "book1");
		CollectTerms collectTerms = (CollectTerms) termCollector.MCR.assertCalledParametersReturn(
				"collectTerms", "fakeDefMetadataIdFromRecordTypeHandlerSpy", dataToIndex);
		recordIndexer.MCR.assertMethodNotCalled("indexData");
		recordIndexer.MCR.assertParameters("indexDataWithoutExplicitCommit", 0, "book", "book1",
				collectTerms.indexTerms, dataToIndex);
	}

	@Test
	public void testIndexDataPerformCommitTrueInWorkOrder() {
		DataGroup workOrder = DataCreator2.createWorkOrderWithIdAndRecordTypeAndRecordIdToIndex(
				"someGeneratedId", "book", "book1");
		workOrder.addChild(new DataAtomicOldSpy("performCommit", "true"));
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
