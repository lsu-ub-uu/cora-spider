/*
 * Copyright 2017, 2022 Uppsala University Library
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

import se.uu.ub.cora.data.DataGroup;
import se.uu.ub.cora.data.DataProvider;
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
		DataGroupSpy workOrder = new DataGroupSpy();
		callExtendedFunctionalityWithGroup(workOrder);

		DataGroupSpy recordInfo = (DataGroupSpy) dataFactorySpy.MCR
				.getReturnValue("factorGroupUsingNameInData", 0);
		workOrder.MCR.assertParameters("addChild", 0, recordInfo);

		dataFactorySpy.MCR.assertParameters("factorRecordLinkUsingNameInDataAndTypeAndId", 0,
				"dataDivider", "system", "cora");
		var dataDivider = dataFactorySpy.MCR
				.getReturnValue("factorRecordLinkUsingNameInDataAndTypeAndId", 0);
		recordInfo.MCR.assertParameters("addChild", 0, dataDivider);
	}

	private void callExtendedFunctionalityWithGroup(DataGroup workOrder) {
		ExtendedFunctionalityData data = new ExtendedFunctionalityData();
		data.authToken = "someToken";
		data.dataGroup = workOrder;
		extendedFunctionality.useExtendedFunctionality(data);
	}

	@Test
	public void testRecordInfoAlreadyExistsNotReplacedByExtendedFunctionality() {
		DataGroupSpy workOrder = new DataGroupSpy();
		workOrder.MRV.setReturnValues("containsChildWithNameInData", List.of(true), "recordInfo");

		callExtendedFunctionalityWithGroup(workOrder);

		dataFactorySpy.MCR.assertMethodNotCalled("factorGroupUsingNameInData");
		dataFactorySpy.MCR.assertMethodNotCalled("factorRecordLinkUsingNameInDataAndTypeAndId");
		workOrder.MCR.assertMethodNotCalled("addChild");
	}
}
