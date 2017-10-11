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

import se.uu.ub.cora.spider.data.SpiderDataAtomic;
import se.uu.ub.cora.spider.data.SpiderDataGroup;

public class WorkOrderEnhancerAsExtendedFunctionality implements ExtendedFunctionality {

   @Override
    public void useExtendedFunctionality(String authToken, SpiderDataGroup spiderDataGroup) {
        SpiderDataGroup recordInfo = SpiderDataGroup.withNameInData("recordInfo");

        SpiderDataGroup dataDivider = SpiderDataGroup.withNameInData("dataDivider");
        dataDivider.addChild(SpiderDataAtomic.withNameInDataAndValue("linkedRecordType", "system"));
        dataDivider.addChild(SpiderDataAtomic.withNameInDataAndValue("linkedRecordId", "cora"));
        recordInfo.addChild(dataDivider);
        spiderDataGroup.addChild(recordInfo);
    }
}
