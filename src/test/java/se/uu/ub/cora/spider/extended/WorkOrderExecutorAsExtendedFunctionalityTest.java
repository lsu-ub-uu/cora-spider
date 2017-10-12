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
import static org.testng.Assert.assertTrue;

import java.util.HashMap;

import org.testng.annotations.Test;

import se.uu.ub.cora.bookkeeper.data.DataGroup;
import se.uu.ub.cora.spider.data.SpiderDataAtomic;
import se.uu.ub.cora.spider.data.SpiderDataGroup;
import se.uu.ub.cora.spider.dependency.SpiderDependencyProviderSpy;
import se.uu.ub.cora.spider.spy.DataGroupSearchTermCollectorSpy;
import se.uu.ub.cora.spider.spy.RecordIndexerSpy;
import se.uu.ub.cora.spider.spy.RecordStorageSpy;

public class WorkOrderExecutorAsExtendedFunctionalityTest {

	;

	// @BeforeMethod
	// public void setUp() {
	// extendedFunctionality = new WorkOrderExecutorAsExtendedFunctionality();
	// }
	//
	// @Test
	// public void useExtendedFunctionality() {
	// assertNotNull(extendedFunctionality);
	// }

	@Test
	public void testIndexData() {
		SpiderDependencyProviderSpy dependencyProvider = new SpiderDependencyProviderSpy(
				new HashMap<>());
		dependencyProvider.recordIndexer = new RecordIndexerSpy();
		dependencyProvider.searchTermCollector = new DataGroupSearchTermCollectorSpy();
		dependencyProvider.recordStorage = new RecordStorageSpy();
		// RecordIndexerSpy recordIndexer = new RecordIndexerSpy();
		// DataGroupSearchTermCollectorSpy termCollector = new
		// DataGroupSearchTermCollectorSpy();

		WorkOrderExecutorAsExtendedFunctionality extendedFunctionality = WorkOrderExecutorAsExtendedFunctionality
				.usingDependencyProvider(dependencyProvider);

		SpiderDataGroup workOrder = createWorkOrder();

		extendedFunctionality.useExtendedFunctionality("someToken", workOrder);
		DataGroupSearchTermCollectorSpy termCollector = (DataGroupSearchTermCollectorSpy) dependencyProvider
				.getDataGroupSearchTermCollector();
		assertTrue(termCollector.collectSearchTermsWasCalled);
		assertEquals(termCollector.metadataId, "bookGroup");
		DataGroup recordInfo = termCollector.dataGroup.getFirstGroupWithNameInData("recordInfo");
		assertEquals(recordInfo.getFirstAtomicValueWithNameInData("id"), "book1");

		RecordIndexerSpy recordIndexer = (RecordIndexerSpy) dependencyProvider.getRecordIndexer();
		assertTrue(recordIndexer.indexDataHasBeenCalled);
		DataGroup recordInfo2 = recordIndexer.record.getFirstGroupWithNameInData("recordInfo");
		assertEquals(recordInfo2.getFirstAtomicValueWithNameInData("id"), "book1");

		// SpiderDataGroup recordInfo = (SpiderDataGroup) workOrder
		// .getFirstChildWithNameInData("recordInfo");
		// assertTrue(recordInfo.containsChildWithNameInData("dataDivider"));
		// SpiderDataGroup dataDivider = (SpiderDataGroup) recordInfo
		// .getFirstChildWithNameInData("dataDivider");
		// SpiderDataAtomic linkedRecordType = (SpiderDataAtomic) dataDivider
		// .getFirstChildWithNameInData("linkedRecordType");
		// assertEquals(linkedRecordType.getValue(), "system");
		// SpiderDataAtomic linkedRecordId = (SpiderDataAtomic) dataDivider
		// .getFirstChildWithNameInData("linkedRecordId");
		// assertEquals(linkedRecordId.getValue(), "cora");
	}

	private SpiderDataGroup createWorkOrder() {
		SpiderDataGroup workOrder = SpiderDataGroup.withNameInData("workOrder");
		SpiderDataGroup recordInfo = SpiderDataGroup.withNameInData("recordInfo");
		recordInfo.addChild(SpiderDataAtomic.withNameInDataAndValue("id", "someGeneratedId"));
		workOrder.addChild(recordInfo);

		SpiderDataGroup recordType = SpiderDataGroup.withNameInData("recordType");
		recordType.addChild(
				SpiderDataAtomic.withNameInDataAndValue("linkedRecordType", "recordType"));
		recordType.addChild(SpiderDataAtomic.withNameInDataAndValue("linkedRecordId", "book"));
		workOrder.addChild(recordType);

		workOrder.addChild(SpiderDataAtomic.withNameInDataAndValue("recordId", "book1"));
		workOrder.addChild(SpiderDataAtomic.withNameInDataAndValue("type", "index"));
		return workOrder;
	}

}
