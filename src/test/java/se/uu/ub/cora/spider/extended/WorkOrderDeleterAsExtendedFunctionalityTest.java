/*
 * Copyright 2018 Uppsala University Library
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
package se.uu.ub.cora.spider.extended;

import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;
import se.uu.ub.cora.spider.authentication.AuthenticatorSpy;
import se.uu.ub.cora.spider.authorization.AlwaysAuthorisedExceptStub;
import se.uu.ub.cora.spider.authorization.AuthorizationException;
import se.uu.ub.cora.spider.data.SpiderDataAtomic;
import se.uu.ub.cora.spider.data.SpiderDataGroup;
import se.uu.ub.cora.spider.dependency.SpiderDependencyProviderSpy;
import se.uu.ub.cora.spider.spy.*;
import se.uu.ub.cora.spider.testdata.DataCreator;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;

import static org.testng.Assert.*;

public class WorkOrderDeleterAsExtendedFunctionalityTest {

	SpiderDependencyProviderSpy dependencyProvider;
	WorkOrderDeleterAsExtendedFunctionality extendedFunctionality;
	DataGroupTermCollectorSpy termCollector;
	AlwaysAuthorisedExceptStub authorizer;
	AuthenticatorSpy authenticator;
	SpiderRecordDeleterSpy recordDeleter;

	@BeforeMethod
	public void setUp() {
		dependencyProvider = new SpiderDependencyProviderSpy(new HashMap<>());
		dependencyProvider.recordIndexer = new RecordIndexerSpy();
		dependencyProvider.searchTermCollector = new DataGroupTermCollectorSpy();
		dependencyProvider.authenticator = new AuthenticatorSpy();
		dependencyProvider.recordStorage = new RecordStorageSpy();
		dependencyProvider.spiderAuthorizator = new AlwaysAuthorisedExceptStub();
		setUpDependencyProvider();
	}

	private void setUpDependencyProvider() {
		recordDeleter = new SpiderRecordDeleterSpy();
		extendedFunctionality = WorkOrderDeleterAsExtendedFunctionality
				.usingDependencyProviderAndDeleter(dependencyProvider, recordDeleter);
		termCollector = (DataGroupTermCollectorSpy) dependencyProvider
				.getDataGroupTermCollector();
		authorizer = (AlwaysAuthorisedExceptStub) dependencyProvider.getSpiderAuthorizator();
		authenticator = (AuthenticatorSpy) dependencyProvider.getAuthenticator();
	}

	@Test
	public void testDeleteData() {
		SpiderDataGroup workOrder = createWorkOrderUsingId("someGeneratedId");

		extendedFunctionality.useExtendedFunctionality("someToken", workOrder);
		assertEquals(recordDeleter.deletedTypes.size(), 1);
		assertEquals(recordDeleter.deletedTypes.get(0), "workOrder");
		assertEquals(recordDeleter.deletedIds.get(0), "someGeneratedId");
	}

	private SpiderDataGroup createWorkOrderUsingId(String id) {
		SpiderDataGroup workOrder = DataCreator.createWorkOrderWithIdAndRecordTypeAndRecordIdToIndex(id, "book", "book1");
		addTypeToRecordInfo(workOrder);
		return workOrder;
	}

	private void addTypeToRecordInfo(SpiderDataGroup workOrder) {
		SpiderDataGroup recordInfo = workOrder.extractGroup("recordInfo");
		SpiderDataGroup type = SpiderDataGroup.withNameInData("type");
		type.addChild(SpiderDataAtomic.withNameInDataAndValue("linkedRecordType", "recordType"));
		type.addChild(SpiderDataAtomic.withNameInDataAndValue("linkedRecordId", "workOrder"));
		recordInfo.addChild(type);
	}

	@Test
	public void testDeleteDataWithNoRightToDeleteRecordType() {
		Set<String> actions = new HashSet<>();
		actions.add("delete");

		SpiderDataGroup workOrder = createWorkOrderUsingId("someGeneratedIdDeleteNotAllowed");
		extendedFunctionality.useExtendedFunctionality("someToken", workOrder);
		assertEquals(recordDeleter.deletedTypes.size(), 0);
	}

	@Test
	public void testDeleteDataWhenNoRecordExists() {
		Set<String> actions = new HashSet<>();
		actions.add("delete");

		SpiderDataGroup workOrder = createWorkOrderUsingId("nonExistingId");
		extendedFunctionality.useExtendedFunctionality("someToken", workOrder);
		assertEquals(recordDeleter.deletedTypes.size(), 0);
	}



}
