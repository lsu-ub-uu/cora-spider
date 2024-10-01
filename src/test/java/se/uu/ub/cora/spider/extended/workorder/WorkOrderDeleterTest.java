/*
 * Copyright 2018, 2019, 2022 Uppsala University Library
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

import static org.testng.Assert.assertEquals;

import java.util.HashSet;
import java.util.Set;

import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import se.uu.ub.cora.data.DataRecordGroup;
import se.uu.ub.cora.data.spies.DataRecordGroupSpy;
import se.uu.ub.cora.logger.LoggerProvider;
import se.uu.ub.cora.spider.extendedfunctionality.ExtendedFunctionalityData;
import se.uu.ub.cora.spider.log.LoggerFactorySpy;
import se.uu.ub.cora.spider.record.internal.AuthenticatorSpy;
import se.uu.ub.cora.spider.record.internal.SpiderAuthorizatorSpy;
import se.uu.ub.cora.spider.spy.DataGroupTermCollectorSpy;
import se.uu.ub.cora.spider.spy.RecordDeleterSpy;
import se.uu.ub.cora.spider.spy.RecordIndexerSpy;
import se.uu.ub.cora.spider.spy.SpiderDependencyProviderSpy;
import se.uu.ub.cora.storage.spies.RecordStorageSpy;

public class WorkOrderDeleterTest {

	SpiderDependencyProviderSpy dependencyProviderSpy;
	WorkOrderDeleter extendedFunctionality;
	DataGroupTermCollectorSpy termCollector;
	SpiderAuthorizatorSpy authorizator;
	AuthenticatorSpy authenticator;
	RecordDeleterSpy recordDeleter;
	private LoggerFactorySpy loggerFactorySpy;
	private RecordIndexerSpy recordIndexer;
	private RecordStorageSpy recordStorage;
	private SpiderAuthorizatorSpy spiderAuthorizator;

	@BeforeMethod
	public void setUp() {
		setUpFactoriesAndProviders();
		setUpDependencyProvider();

		recordDeleter = new RecordDeleterSpy();
		extendedFunctionality = WorkOrderDeleter.usingDeleter(recordDeleter);
	}

	private void setUpFactoriesAndProviders() {
		loggerFactorySpy = new LoggerFactorySpy();
		LoggerProvider.setLoggerFactory(loggerFactorySpy);
	}

	private void setUpDependencyProvider() {
		dependencyProviderSpy = new SpiderDependencyProviderSpy();
		recordStorage = new RecordStorageSpy();
		spiderAuthorizator = new SpiderAuthorizatorSpy();
		authenticator = new AuthenticatorSpy();
		termCollector = new DataGroupTermCollectorSpy();
		recordIndexer = new RecordIndexerSpy();

		dependencyProviderSpy.MRV.setDefaultReturnValuesSupplier("getRecordStorage",
				() -> recordStorage);
		dependencyProviderSpy.MRV.setDefaultReturnValuesSupplier("getSpiderAuthorizator",
				() -> spiderAuthorizator);
		dependencyProviderSpy.MRV.setDefaultReturnValuesSupplier("getAuthenticator",
				() -> authenticator);
		dependencyProviderSpy.MRV.setDefaultReturnValuesSupplier("getDataGroupTermCollector",
				() -> termCollector);
		dependencyProviderSpy.MRV.setDefaultReturnValuesSupplier("getRecordIndexer",
				() -> recordIndexer);
	}

	@Test
	public void testDeleteData() {
		DataRecordGroup workOrder = createWorkOrderUsingId("someGeneratedId");

		callExtendedFunctionalityWithGroup(workOrder);
		assertEquals(recordDeleter.deletedTypes.size(), 1);
		assertEquals(recordDeleter.deletedTypes.get(0), "workOrder");
		assertEquals(recordDeleter.deletedIds.get(0), "someGeneratedId");
	}

	private DataRecordGroup createWorkOrderUsingId(String id) {
		DataRecordGroupSpy workOrder = new DataRecordGroupSpy();
		workOrder.MRV.setDefaultReturnValuesSupplier("getType", () -> "workOrder");
		workOrder.MRV.setDefaultReturnValuesSupplier("getId", () -> id);
		return workOrder;
	}

	private void callExtendedFunctionalityWithGroup(DataRecordGroup workOrder) {
		ExtendedFunctionalityData data = new ExtendedFunctionalityData();
		data.authToken = "someToken";
		data.dataRecordGroup = workOrder;
		extendedFunctionality.useExtendedFunctionality(data);
	}

	@Test
	public void testDeleteDataWithNoRightToDeleteRecordType() {
		Set<String> actions = new HashSet<>();
		actions.add("delete");

		DataRecordGroup workOrder = createWorkOrderUsingId("someGeneratedIdDeleteNotAllowed");
		callExtendedFunctionalityWithGroup(workOrder);
		assertEquals(recordDeleter.deletedTypes.size(), 0);
	}

	@Test
	public void testDeleteDataWhenNoRecordExists() {
		Set<String> actions = new HashSet<>();
		actions.add("delete");

		DataRecordGroup workOrder = createWorkOrderUsingId("nonExistingId");
		callExtendedFunctionalityWithGroup(workOrder);
		assertEquals(recordDeleter.deletedTypes.size(), 0);
	}

}
