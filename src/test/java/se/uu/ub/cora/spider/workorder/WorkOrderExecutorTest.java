/*
 * Copyright 2017, 2019, 2022 Uppsala University Library
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
package se.uu.ub.cora.spider.workorder;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertTrue;

import java.util.HashMap;
import java.util.List;

import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import se.uu.ub.cora.data.DataAtomicProvider;
import se.uu.ub.cora.data.DataGroup;
import se.uu.ub.cora.data.DataGroupFactory;
import se.uu.ub.cora.data.DataGroupProvider;
import se.uu.ub.cora.logger.LoggerProvider;
import se.uu.ub.cora.spider.authentication.AuthenticatorSpy;
import se.uu.ub.cora.spider.data.DataAtomicFactorySpy;
import se.uu.ub.cora.spider.data.DataAtomicSpy;
import se.uu.ub.cora.spider.data.DataGroupFactorySpy;
import se.uu.ub.cora.spider.dependency.spy.SpiderDependencyProviderSpy;
import se.uu.ub.cora.spider.extendedfunctionality.ExtendedFunctionalityData;
import se.uu.ub.cora.spider.log.LoggerFactorySpy;
import se.uu.ub.cora.spider.spy.DataGroupTermCollectorSpy;
import se.uu.ub.cora.spider.spy.OldRecordStorageSpy;
import se.uu.ub.cora.spider.spy.RecordIndexerSpy;
import se.uu.ub.cora.spider.spy.RecordStorageCreateUpdateSpy;
import se.uu.ub.cora.spider.spy.SpiderAuthorizatorSpy;
import se.uu.ub.cora.spider.testdata.DataCreator2;

public class WorkOrderExecutorTest {

	SpiderDependencyProviderSpy dependencyProvider;
	WorkOrderExecutor extendedFunctionality;
	DataGroupTermCollectorSpy termCollector;
	RecordIndexerSpy recordIndexer;
	SpiderAuthorizatorSpy authorizator;
	AuthenticatorSpy authenticator;
	private LoggerFactorySpy loggerFactorySpy;
	private DataGroupFactory dataGroupFactory;
	private DataAtomicFactorySpy dataAtomicFactory;

	@BeforeMethod
	public void setUp() {
		setUpFactoriesAndProviders();

		dependencyProvider = new SpiderDependencyProviderSpy(new HashMap<>());
		dependencyProvider.recordIndexer = new RecordIndexerSpy();
		dependencyProvider.termCollector = new DataGroupTermCollectorSpy();
		dependencyProvider.recordStorage = new OldRecordStorageSpy();
		dependencyProvider.authenticator = new AuthenticatorSpy();
		dependencyProvider.spiderAuthorizator = new SpiderAuthorizatorSpy();
		setUpDependencyProvider();
	}

	private void setUpFactoriesAndProviders() {
		loggerFactorySpy = new LoggerFactorySpy();
		LoggerProvider.setLoggerFactory(loggerFactorySpy);
		dataGroupFactory = new DataGroupFactorySpy();
		DataGroupProvider.setDataGroupFactory(dataGroupFactory);
		dataAtomicFactory = new DataAtomicFactorySpy();
		DataAtomicProvider.setDataAtomicFactory(dataAtomicFactory);
	}

	private void setUpDependencyProvider() {
		extendedFunctionality = WorkOrderExecutor.usingDependencyProvider(dependencyProvider);
		termCollector = (DataGroupTermCollectorSpy) dependencyProvider.getDataGroupTermCollector();
		recordIndexer = (RecordIndexerSpy) dependencyProvider.getRecordIndexer();
		authorizator = (SpiderAuthorizatorSpy) dependencyProvider.getSpiderAuthorizator();
		authenticator = (AuthenticatorSpy) dependencyProvider.getAuthenticator();
	}

	@Test
	public void testIndexData() {
		DataGroup workOrder = DataCreator2.createWorkOrderWithIdAndRecordTypeAndRecordIdToIndex(
				"someGeneratedId", "book", "book1");
		callExtendedFunctionalityWithGroup(workOrder);

		termCollector.MCR.assertParameter("collectTerms", 0, "metadataId", "bookGroup");

		DataGroup dataGroup = (DataGroup) termCollector.MCR
				.getValueForMethodNameAndCallNumberAndParameterName("collectTerms", 0, "dataGroup");
		DataGroup recordInfo = dataGroup.getFirstGroupWithNameInData("recordInfo");
		assertEquals(recordInfo.getFirstAtomicValueWithNameInData("id"), "book1");

		assertTrue(recordIndexer.indexDataHasBeenCalled);

		recordIndexer.MCR.assertParameter("indexData", 0, "collectedData",
				termCollector.MCR.getReturnValue("collectTerms", 0));

		DataGroup recordInfo2 = recordIndexer.record.getFirstGroupWithNameInData("recordInfo");
		assertEquals(recordInfo2.getFirstAtomicValueWithNameInData("id"), "book1");

		List<String> ids = recordIndexer.ids;
		assertEquals(ids.get(0), "book_book1");
		assertEquals(ids.size(), 1);
	}

	private void callExtendedFunctionalityWithGroup(DataGroup workOrder) {
		ExtendedFunctionalityData data = new ExtendedFunctionalityData();
		data.authToken = "someToken";
		data.dataGroup = workOrder;
		extendedFunctionality.useExtendedFunctionality(data);
	}

	@Test
	public void testIndexDataForChildOfAbstract() {
		dependencyProvider.recordStorage = new RecordStorageCreateUpdateSpy();
		setUpDependencyProvider();
		DataGroup workOrder = DataCreator2.createWorkOrderWithIdAndRecordTypeAndRecordIdToIndex(
				"someGeneratedId", "image", "image1");
		callExtendedFunctionalityWithGroup(workOrder);

		List<String> ids = recordIndexer.ids;
		assertEquals(ids.get(0), "image_image1");
		assertEquals(ids.get(1), "binary_image1");
		assertEquals(ids.size(), 2);
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
		workOrder.addChild(new DataAtomicSpy("performCommit", "false"));
		callExtendedFunctionalityWithGroup(workOrder);

		termCollector.MCR.assertParameter("collectTerms", 0, "metadataId", "bookGroup");

		DataGroup dataGroup = (DataGroup) termCollector.MCR
				.getValueForMethodNameAndCallNumberAndParameterName("collectTerms", 0, "dataGroup");
		DataGroup recordInfo = dataGroup.getFirstGroupWithNameInData("recordInfo");
		assertEquals(recordInfo.getFirstAtomicValueWithNameInData("id"), "book1");

		recordIndexer.MCR.assertMethodWasCalled("indexDataWithoutExplicitCommit");
		recordIndexer.MCR.assertParameter("indexDataWithoutExplicitCommit", 0, "collectedData",
				termCollector.MCR.getReturnValue("collectTerms", 0));

		DataGroup recordInfo2 = recordIndexer.record.getFirstGroupWithNameInData("recordInfo");
		assertEquals(recordInfo2.getFirstAtomicValueWithNameInData("id"), "book1");

		List<String> ids = recordIndexer.ids;
		assertEquals(ids.get(0), "book_book1");
		assertEquals(ids.size(), 1);
	}

	@Test
	public void testIndexDataPerformCommitTrueInWorkOrder() {
		DataGroup workOrder = DataCreator2.createWorkOrderWithIdAndRecordTypeAndRecordIdToIndex(
				"someGeneratedId", "book", "book1");
		workOrder.addChild(new DataAtomicSpy("performCommit", "true"));
		callExtendedFunctionalityWithGroup(workOrder);

		recordIndexer.MCR.assertMethodWasCalled("indexData");
		recordIndexer.MCR.assertParameter("indexData", 0, "collectedData",
				termCollector.MCR.getReturnValue("collectTerms", 0));

		DataGroup recordInfo2 = recordIndexer.record.getFirstGroupWithNameInData("recordInfo");
		assertEquals(recordInfo2.getFirstAtomicValueWithNameInData("id"), "book1");

		List<String> ids = recordIndexer.ids;
		assertEquals(ids.get(0), "book_book1");
		assertEquals(ids.size(), 1);
	}
}
