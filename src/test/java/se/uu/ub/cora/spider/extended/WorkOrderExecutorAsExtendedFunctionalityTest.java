/*
 * Copyright 2017 Uppsala University Library
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

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertTrue;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import se.uu.ub.cora.bookkeeper.data.DataGroup;
import se.uu.ub.cora.spider.authentication.AuthenticatorSpy;
import se.uu.ub.cora.spider.authorization.AlwaysAuthorisedExceptStub;
import se.uu.ub.cora.spider.data.SpiderDataGroup;
import se.uu.ub.cora.spider.dependency.RecordStorageProviderSpy;
import se.uu.ub.cora.spider.dependency.SpiderDependencyProviderSpy;
import se.uu.ub.cora.spider.spy.DataGroupTermCollectorSpy;
import se.uu.ub.cora.spider.spy.RecordIndexerSpy;
import se.uu.ub.cora.spider.spy.RecordStorageCreateUpdateSpy;
import se.uu.ub.cora.spider.spy.RecordStorageSpy;
import se.uu.ub.cora.spider.testdata.DataCreator;

public class WorkOrderExecutorAsExtendedFunctionalityTest {

	SpiderDependencyProviderSpy dependencyProvider;
	WorkOrderExecutorAsExtendedFunctionality extendedFunctionality;
	DataGroupTermCollectorSpy termCollector;
	RecordIndexerSpy recordIndexer;
	AlwaysAuthorisedExceptStub authorizer;
	AuthenticatorSpy authenticator;

	@BeforeMethod
	public void setUp() {
		dependencyProvider = new SpiderDependencyProviderSpy(new HashMap<>());
		dependencyProvider.recordIndexer = new RecordIndexerSpy();
		dependencyProvider.searchTermCollector = new DataGroupTermCollectorSpy();
		RecordStorageProviderSpy recordStorageProviderSpy = new RecordStorageProviderSpy();
		recordStorageProviderSpy.recordStorage = new RecordStorageSpy();
		dependencyProvider.setRecordStorageProvider(recordStorageProviderSpy);
		dependencyProvider.authenticator = new AuthenticatorSpy();
		dependencyProvider.spiderAuthorizator = new AlwaysAuthorisedExceptStub();
		setUpDependencyProvider();
	}

	private void setUpDependencyProvider() {
		extendedFunctionality = WorkOrderExecutorAsExtendedFunctionality
				.usingDependencyProvider(dependencyProvider);
		termCollector = (DataGroupTermCollectorSpy) dependencyProvider.getDataGroupTermCollector();
		recordIndexer = (RecordIndexerSpy) dependencyProvider.getRecordIndexer();
		authorizer = (AlwaysAuthorisedExceptStub) dependencyProvider.getSpiderAuthorizator();
		authenticator = (AuthenticatorSpy) dependencyProvider.getAuthenticator();
	}

	@Test
	public void testIndexData() {
		SpiderDataGroup workOrder = DataCreator
				.createWorkOrderWithIdAndRecordTypeAndRecordIdToIndex("someGeneratedId", "book",
						"book1");
		extendedFunctionality.useExtendedFunctionality("someToken", workOrder);

		assertTrue(termCollector.collectTermsWasCalled);
		assertEquals(termCollector.metadataId, "bookGroup");

		DataGroup recordInfo = termCollector.dataGroup.getFirstGroupWithNameInData("recordInfo");
		assertEquals(recordInfo.getFirstAtomicValueWithNameInData("id"), "book1");

		assertTrue(recordIndexer.indexDataHasBeenCalled);
		assertCollectedTermsAreSentToIndex();
		DataGroup recordInfo2 = recordIndexer.record.getFirstGroupWithNameInData("recordInfo");
		assertEquals(recordInfo2.getFirstAtomicValueWithNameInData("id"), "book1");

		List<String> ids = recordIndexer.ids;
		assertEquals(ids.get(0), "book_book1");
		assertEquals(ids.size(), 1);
	}

	@Test
	public void testIndexDataForChildOfAbstract() {
		RecordStorageProviderSpy recordStorageProviderSpy = new RecordStorageProviderSpy();
		recordStorageProviderSpy.recordStorage = new RecordStorageCreateUpdateSpy();
		dependencyProvider.setRecordStorageProvider(recordStorageProviderSpy);
		setUpDependencyProvider();
		SpiderDataGroup workOrder = DataCreator
				.createWorkOrderWithIdAndRecordTypeAndRecordIdToIndex("someGeneratedId", "image",
						"image1");
		extendedFunctionality.useExtendedFunctionality("someToken", workOrder);

		List<String> ids = recordIndexer.ids;
		assertEquals(ids.get(0), "image_image1");
		assertEquals(ids.get(1), "binary_image1");
		assertEquals(ids.size(), 2);
	}

	private void assertCollectedTermsAreSentToIndex() {
		assertEquals(recordIndexer.recordIndexData, termCollector.collectedTerms);
	}

	@Test
	public void testIndexDataWithNoRightToIndexRecordType() {
		Set<String> actions = new HashSet<>();
		actions.add("index");
		authorizer.notAuthorizedForRecordTypeAndActions.put("book", actions);

		SpiderDataGroup workOrder = DataCreator
				.createWorkOrderWithIdAndRecordTypeAndRecordIdToIndex("someGeneratedId", "book",
						"book1");
		extendedFunctionality.useExtendedFunctionality("someToken", workOrder);

		assertFalse(termCollector.collectTermsWasCalled);
		assertFalse(recordIndexer.indexDataHasBeenCalled);
	}

}
