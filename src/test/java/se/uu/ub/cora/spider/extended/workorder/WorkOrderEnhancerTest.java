/*
 * Copyright 2017, 2022, 2023 Uppsala University Library
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

import static org.testng.Assert.assertNotNull;

import java.util.List;

import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import se.uu.ub.cora.data.DataProvider;
import se.uu.ub.cora.data.spies.DataFactorySpy;
import se.uu.ub.cora.data.spies.DataRecordGroupSpy;
import se.uu.ub.cora.spider.extendedfunctionality.ExtendedFunctionalityData;

public class WorkOrderEnhancerTest {

	WorkOrderEnhancer extendedFunctionality;
	private DataFactorySpy dataFactorySpy;

	@BeforeMethod
	public void setUp() {
		dataFactorySpy = new DataFactorySpy();
		DataProvider.onlyForTestSetDataFactory(dataFactorySpy);

		extendedFunctionality = new WorkOrderEnhancer();
	}

	@Test
	public void useExtendedFunctionality() {
		assertNotNull(extendedFunctionality);
	}

	@Test
	public void testAddRecordInfo() {
		DataRecordGroupSpy workOrder = new DataRecordGroupSpy();

		callExtendedFunctionalityWithGroup(workOrder);

		workOrder.MCR.assertParameters("setDataDivider", 0, "cora");
		workOrder.MCR.assertParameters("setValidationType", 0, "workOrder");
	}

	private void callExtendedFunctionalityWithGroup(DataRecordGroupSpy workOrder) {
		ExtendedFunctionalityData data = new ExtendedFunctionalityData();
		data.authToken = "someToken";
		data.dataRecordGroup = workOrder;
		extendedFunctionality.useExtendedFunctionality(data);
	}

	@Test
	public void testRecordInfoAlreadyExistsNotReplacedByExtendedFunctionality() {
		DataRecordGroupSpy workOrder = new DataRecordGroupSpy();
		workOrder.MRV.setReturnValues("containsChildWithNameInData", List.of(true), "recordInfo");

		callExtendedFunctionalityWithGroup(workOrder);

		workOrder.MCR.assertMethodNotCalled("setDataDivider");
		workOrder.MCR.assertMethodNotCalled("setValidationType");
	}
}
