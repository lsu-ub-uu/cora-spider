/*
 * Copyright 2015, 2016 Uppsala University Library
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

package se.uu.ub.cora.spider.record;

import java.util.Collection;
import se.uu.ub.cora.beefeater.authentication.User;
import se.uu.ub.cora.bookkeeper.data.DataGroup;
import se.uu.ub.cora.bookkeeper.validator.DataValidator;
import se.uu.ub.cora.bookkeeper.validator.ValidationAnswer;
import se.uu.ub.cora.spider.authentication.Authenticator;
import se.uu.ub.cora.spider.authorization.SpiderAuthorizator;
import se.uu.ub.cora.spider.data.Action;
import se.uu.ub.cora.spider.data.SpiderDataGroup;
import se.uu.ub.cora.spider.data.SpiderDataList;
import se.uu.ub.cora.spider.data.SpiderDataRecord;
import se.uu.ub.cora.spider.dependency.SpiderDependencyProvider;

public final class SpiderRecordListReaderImp extends SpiderRecordHandler
        implements SpiderRecordListReader {
    private static final String FILTER_STRING = "filter";
    private static final String LIST = "list";
    private Authenticator authenticator;
    private SpiderAuthorizator spiderAuthorizator;
    private SpiderDataList readRecordList;
    private String authToken;
    private User user;
    private DataGroupToRecordEnhancer dataGroupToRecordEnhancer;
    private DataValidator dataValidator;
    private int requestedStartPositionOfRead = 1;
    private int requestedNumberOfRowsToRead = 100;

    private SpiderRecordListReaderImp(SpiderDependencyProvider dependencyProvider,
                                      DataGroupToRecordEnhancer dataGroupToRecordEnhancer) {
        this.dataGroupToRecordEnhancer = dataGroupToRecordEnhancer;
        this.authenticator = dependencyProvider.getAuthenticator();
        this.spiderAuthorizator = dependencyProvider.getSpiderAuthorizator();
        this.recordStorage = dependencyProvider.getRecordStorage();
        dataValidator = dependencyProvider.getDataValidator();
    }

    public static SpiderRecordListReaderImp usingDependencyProviderAndDataGroupToRecordEnhancer(
            SpiderDependencyProvider dependencyProvider,
            DataGroupToRecordEnhancer dataGroupToRecordEnhancer) {
        return new SpiderRecordListReaderImp(dependencyProvider, dataGroupToRecordEnhancer);
    }

    @Override
    public SpiderDataList readRecordList(String authToken, String recordType,
                                         SpiderDataGroup filter) {
        validateActiveUserWithPermissionsOnRecordType(authToken, recordType);

        readRecordList = SpiderDataList.withContainDataOfType(recordType);
        DataGroup recordTypeDataGroup = recordStorage.read(RECORD_TYPE, recordType);
        DataGroup filterAsDataGroup = filter.toDataGroup();
        extractRequestedReadWindowInformationFromFilter(filterAsDataGroup);
        validateFilterIfNotEmpty(filter, recordType, recordTypeDataGroup);
        readRecordsOfType(recordType, filterAsDataGroup, recordTypeDataGroup);
        setFromToInReadRecordList();

        return readRecordList;
    }

    private void validateActiveUserWithPermissionsOnRecordType(String authToken, String recordType) {
        this.authToken = authToken;
        this.recordType = recordType;

        tryToGetActiveUser();
        checkUserIsAuthorizedForActionOnRecordType();
    }

    private int getStartFromSearchDataAsInteger(DataGroup searchData) {
        String start = searchData.getFirstAtomicValueWithNameInDataOrDefault("start","1");
        return Integer.valueOf(start);
    }

    private int getRowsFromSearchDataAsInteger(DataGroup searchData) {
        String rows = searchData.getFirstAtomicValueWithNameInDataOrDefault("rows","100");
        return Integer.valueOf(rows);
    }

    private void extractRequestedReadWindowInformationFromFilter(DataGroup filter) {
        requestedStartPositionOfRead = getStartFromSearchDataAsInteger(filter);
        requestedNumberOfRowsToRead = getRowsFromSearchDataAsInteger(filter);
    }

    private void tryToGetActiveUser() {
        user = authenticator.getUserForToken(authToken);
    }

    private void checkUserIsAuthorizedForActionOnRecordType() {
        spiderAuthorizator.checkUserIsAuthorizedForActionOnRecordType(user, LIST, recordType);
    }

    private void validateFilterIfNotEmpty(SpiderDataGroup filter, String recordType,
                                          DataGroup recordTypeDataGroup) {
        if (filterIsNotEmpty(filter)) {
            validateFilter(filter, recordType, recordTypeDataGroup);
        }
    }

    private void validateFilter(SpiderDataGroup filter, String recordType,
                                DataGroup recordTypeDataGroup) {
        throwErrorIfRecordTypeHasNoDefinedFilter(recordType, recordTypeDataGroup);

        String filterMetadataId = getMetadataIdForFilter(recordTypeDataGroup);
        validateFilterAsSpecifiedInMetadata(filter, filterMetadataId);
    }

    private void validateFilterAsSpecifiedInMetadata(SpiderDataGroup filter,
                                                     String filterMetadataId) {
        ValidationAnswer validationAnswer = dataValidator.validateData(filterMetadataId,
                filter.toDataGroup());
        if (validationAnswer.dataIsInvalid()) {
            throw new DataException("Data is not valid: " + validationAnswer.getErrorMessages());
        }
    }

    private String getMetadataIdForFilter(DataGroup recordTypeDataGroup) {
        DataGroup filterGroup = recordTypeDataGroup.getFirstGroupWithNameInData(FILTER_STRING);
        return filterGroup.getFirstAtomicValueWithNameInData("linkedRecordId");
    }

    private void throwErrorIfRecordTypeHasNoDefinedFilter(String recordType,
                                                          DataGroup recordTypeDataGroup) {
        if (!recordTypeDataGroup.containsChildWithNameInData(FILTER_STRING)) {
            throw new DataException("No filter exists for recordType: " + recordType);
        }
    }

    private boolean filterIsNotEmpty(SpiderDataGroup filter) {
        return filter.containsChildWithNameInData("part");
    }

    private void readRecordsOfType(String recordType, DataGroup filter,
                                   DataGroup recordTypeDataGroup) {
        if (recordTypeIsAbstract(recordTypeDataGroup)) {
            addChildrenOfAbstractTypeToReadRecordList(recordType);
        } else {
            readRecordsOfSpecifiedRecordTypeAndAddToReadRecordList(recordType, filter);
        }
    }

    private boolean recordTypeIsAbstract(DataGroup recordTypeDataGroup) {

        String abstractString = recordTypeDataGroup.getFirstAtomicValueWithNameInData("abstract");
        return "true".equals(abstractString);
    }

    private void addChildrenOfAbstractTypeToReadRecordList(String abstractRecordType) {
        DataGroup emptyFilter = DataGroup.withNameInData(FILTER_STRING);
        Collection<DataGroup> dataGroupList = recordStorage.readAbstractList(abstractRecordType,
                emptyFilter).listOfDataGroups;
        for (DataGroup dataGroup : dataGroupList) {
            String type = extractRecordTypeFromDataGroup(dataGroup);
            this.recordType = type;
            enhanceDataGroupAndAddToRecordList(dataGroup, type);
        }
    }

    private String extractRecordTypeFromDataGroup(DataGroup dataGroup) {
        DataGroup recordInfo = dataGroup.getFirstGroupWithNameInData("recordInfo");
        DataGroup typeGroup = recordInfo.getFirstGroupWithNameInData("type");

        return typeGroup.getFirstAtomicValueWithNameInData("linkedRecordId");
    }

    private void enhanceDataGroupAndAddToRecordList(DataGroup dataGroup,
                                                    String recordTypeForRecord) {
        SpiderDataRecord spiderDataRecord = dataGroupToRecordEnhancer.enhance(user,
                recordTypeForRecord, dataGroup);
        if (spiderDataRecord.getActions().contains(Action.READ)) {
            readRecordList.addData(spiderDataRecord);
        }
    }

    private void readRecordsOfSpecifiedRecordTypeAndAddToReadRecordList(String type,
                                                                        DataGroup filter) {
        Collection<DataGroup> dataGroupList = recordStorage.readList(type, filter).listOfDataGroups;
        this.recordType = type;
        for (DataGroup dataGroup : dataGroupList) {
            enhanceDataGroupAndAddToRecordList(dataGroup, type);
        }
    }

    private void setFromToInReadRecordList() {
        readRecordList.setTotalNo(String.valueOf(readRecordList.getDataList().size()));
        readRecordList.setFromNo(String.valueOf(requestedStartPositionOfRead));
        readRecordList.setToNo(String.valueOf(requestedStartPositionOfRead - 1 + readRecordList.getDataList().size()));
    }
}
