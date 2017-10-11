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

import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;
import se.uu.ub.cora.spider.data.SpiderDataAtomic;
import se.uu.ub.cora.spider.data.SpiderDataElement;
import se.uu.ub.cora.spider.data.SpiderDataGroup;

import static org.testng.Assert.assertNotNull;
import static org.testng.Assert.assertTrue;
import static org.testng.AssertJUnit.assertEquals;

public class WorkOrderEnhancerAsExtendedFunctionalityTest {

    WorkOrderEnhancerAsExtendedFunctionality extendedFunctionality;

    @BeforeMethod
    public void setUp() {
        extendedFunctionality = new WorkOrderEnhancerAsExtendedFunctionality();
    }

    @Test
    public void useExtendedFunctionality() {
        assertNotNull(extendedFunctionality);
    }

    @Test
    public void testAddRecordInfo(){
        SpiderDataGroup workOrder = SpiderDataGroup.withNameInData("workOrder");
        extendedFunctionality.useExtendedFunctionality("someToken", workOrder);


        SpiderDataGroup recordInfo = (SpiderDataGroup) workOrder.getFirstChildWithNameInData("recordInfo");
        assertTrue(recordInfo.containsChildWithNameInData("dataDivider"));
        SpiderDataGroup dataDivider = (SpiderDataGroup) recordInfo.getFirstChildWithNameInData("dataDivider");
        SpiderDataAtomic linkedRecordType = (SpiderDataAtomic) dataDivider.getFirstChildWithNameInData("linkedRecordType");
        assertEquals(linkedRecordType.getValue(), "system");
        SpiderDataAtomic linkedRecordId = (SpiderDataAtomic) dataDivider.getFirstChildWithNameInData("linkedRecordId");
        assertEquals(linkedRecordId.getValue(), "cora");
    }
}
