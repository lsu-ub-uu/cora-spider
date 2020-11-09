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
package se.uu.ub.cora.spider.workorder;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertNotNull;
import static org.testng.Assert.assertTrue;

import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import se.uu.ub.cora.data.DataAtomic;
import se.uu.ub.cora.data.DataGroup;
import se.uu.ub.cora.data.DataGroupFactory;
import se.uu.ub.cora.data.DataGroupProvider;
import se.uu.ub.cora.spider.data.DataGroupFactorySpy;
import se.uu.ub.cora.spider.data.DataGroupSpy;

public class WorkOrderEnhancerTest {

	WorkOrderEnhancer extendedFunctionality;
	private DataGroupFactory dataGroupFactory;

	@BeforeMethod
	public void setUp() {
		dataGroupFactory = new DataGroupFactorySpy();
		DataGroupProvider.setDataGroupFactory(dataGroupFactory);
		extendedFunctionality = new WorkOrderEnhancer();
	}

	@Test
	public void useExtendedFunctionality() {
		assertNotNull(extendedFunctionality);
	}

	@Test
	public void testAddRecordInfo() {
		DataGroup workOrder = new DataGroupSpy("workOrder");
		extendedFunctionality.useExtendedFunctionality("someToken", workOrder);

		DataGroup recordInfo = (DataGroup) workOrder.getFirstChildWithNameInData("recordInfo");
		assertTrue(recordInfo.containsChildWithNameInData("dataDivider"));
		DataGroup dataDivider = (DataGroup) recordInfo.getFirstChildWithNameInData("dataDivider");
		DataAtomic linkedRecordType = (DataAtomic) dataDivider
				.getFirstChildWithNameInData("linkedRecordType");
		assertEquals(linkedRecordType.getValue(), "system");
		DataAtomic linkedRecordId = (DataAtomic) dataDivider
				.getFirstChildWithNameInData("linkedRecordId");
		assertEquals(linkedRecordId.getValue(), "cora");
	}

	@Test
	public void testRecordInfoAlreadyExistsNotReplacedByExtendedFunctionality() {
		DataGroup workOrder = new DataGroupSpy("workOrder");
		workOrder.addChild(new DataGroupSpy("recordInfo"));
		extendedFunctionality.useExtendedFunctionality("someToken", workOrder);

		assertEquals(workOrder.getChildren().size(), 1);

		DataGroup recordInfo = (DataGroup) workOrder.getFirstChildWithNameInData("recordInfo");
		assertFalse(recordInfo.containsChildWithNameInData("dataDivider"));
	}
}
