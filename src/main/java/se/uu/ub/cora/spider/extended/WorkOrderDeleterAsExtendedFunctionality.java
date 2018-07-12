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

import se.uu.ub.cora.spider.authorization.AuthorizationException;
import se.uu.ub.cora.spider.data.SpiderDataGroup;
import se.uu.ub.cora.spider.dependency.SpiderDependencyProvider;
import se.uu.ub.cora.spider.record.SpiderRecordDeleter;
import se.uu.ub.cora.spider.record.storage.RecordNotFoundException;

public class WorkOrderDeleterAsExtendedFunctionality implements ExtendedFunctionality {

    private SpiderRecordDeleter recordDeleter;

    private WorkOrderDeleterAsExtendedFunctionality(SpiderDependencyProvider dependencyProvider, SpiderRecordDeleter recordDeleter) {
        this.recordDeleter = recordDeleter;
    }

    public static WorkOrderDeleterAsExtendedFunctionality usingDependencyProviderAndDeleter(SpiderDependencyProvider dependencyProvider, SpiderRecordDeleter recordDeleter) {
        return new WorkOrderDeleterAsExtendedFunctionality(dependencyProvider, recordDeleter);
    }

    @Override
    public void useExtendedFunctionality(String authToken, SpiderDataGroup spiderDataGroup) {
        SpiderDataGroup recordInfo = spiderDataGroup.extractGroup("recordInfo");
        String recordType = extractRecordType(recordInfo);
        String recordId = recordInfo.extractAtomicValue("id");
        tryToDeleteRecord(authToken, recordType, recordId);
    }

    private String extractRecordType(SpiderDataGroup recordInfo) {
        SpiderDataGroup recordTypeGroup = recordInfo.extractGroup("type");
        return recordTypeGroup.extractAtomicValue("linkedRecordId");
    }

    private void tryToDeleteRecord(String authToken, String recordType, String recordId) {
        try {
            recordDeleter.deleteRecord(authToken, recordType, recordId);
        }catch (AuthorizationException |RecordNotFoundException e){
            //do nothing
        }
    }

}
