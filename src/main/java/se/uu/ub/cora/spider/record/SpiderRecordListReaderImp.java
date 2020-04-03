/*
 * Copyright 2015, 2016, 2018, 2019, 2020 Uppsala University Library
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
import se.uu.ub.cora.bookkeeper.validator.DataValidationException;
import se.uu.ub.cora.bookkeeper.validator.DataValidator;
import se.uu.ub.cora.bookkeeper.validator.ValidationAnswer;
import se.uu.ub.cora.data.Action;
import se.uu.ub.cora.data.DataGroup;
import se.uu.ub.cora.data.DataList;
import se.uu.ub.cora.data.DataListProvider;
import se.uu.ub.cora.data.DataRecord;
import se.uu.ub.cora.spider.authentication.Authenticator;
import se.uu.ub.cora.spider.authorization.SpiderAuthorizator;
import se.uu.ub.cora.spider.dependency.SpiderDependencyProvider;
import se.uu.ub.cora.storage.StorageReadResult;

public final class SpiderRecordListReaderImp extends SpiderRecordHandler
		implements SpiderRecordListReader {
	private static final String LIST = "list";
	private Authenticator authenticator;
	private SpiderAuthorizator spiderAuthorizator;
	private DataList readRecordList;
	private User user;
	private DataGroupToRecordEnhancer dataGroupToRecordEnhancer;
	private DataValidator dataValidator;
	private StorageReadResult readResult;
	private SpiderDependencyProvider dependencyProvider;
	private RecordTypeHandler recordTypeHandler;

	private SpiderRecordListReaderImp(SpiderDependencyProvider dependencyProvider,
			DataGroupToRecordEnhancer dataGroupToRecordEnhancer) {
		this.dependencyProvider = dependencyProvider;
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
	public DataList readRecordList(String authToken, String recordType, DataGroup filter) {
		this.recordType = recordType;
		recordTypeHandler = dependencyProvider.getRecordTypeHandler(recordType);
		ensureActiveUserHasListPermissionUsingAuthToken(authToken);

		readRecordList = DataListProvider.getDataListWithNameOfDataType(recordType);
		validateFilterIfNotEmpty(filter, recordType);
		readRecordsOfType(recordType, filter);
		setFromToInReadRecordList();

		return readRecordList;
	}

	private void ensureActiveUserHasListPermissionUsingAuthToken(String authToken) {
		tryToGetActiveUser(authToken);
		checkUserIsAuthorizedForActionOnRecordType();
	}

	private void tryToGetActiveUser(String authToken) {
		user = authenticator.getUserForToken(authToken);
	}

	private void checkUserIsAuthorizedForActionOnRecordType() {
		if (listedRecordTypeIsNotPublicRead()) {
			spiderAuthorizator.checkUserIsAuthorizedForActionOnRecordType(user, LIST, recordType);
		}
	}

	private boolean listedRecordTypeIsNotPublicRead() {
		return !recordTypeHandler.isPublicForRead();
	}

	private void validateFilterIfNotEmpty(DataGroup filter, String recordType) {
		if (filterIsNotEmpty(filter)) {
			validateFilterUsingDataValidator(recordType, filter);
		}
	}

	private boolean filterIsNotEmpty(DataGroup filter) {
		return filter.containsChildWithNameInData("part")
				|| filter.containsChildWithNameInData("start")
				|| filter.containsChildWithNameInData("rows");
	}

	private void validateFilterUsingDataValidator(String recordType, DataGroup filter) {
		try {
			tryToValidateFilter(recordType, filter);
		} catch (DataValidationException e) {
			throw new DataException("No filter exists for recordType: " + recordType);
		}
	}

	private void tryToValidateFilter(String recordType, DataGroup filter) {
		ValidationAnswer validationAnswer = dataValidator.validateListFilter(recordType, filter);
		if (validationAnswer.dataIsInvalid()) {
			throw new DataException("Data is not valid: " + validationAnswer.getErrorMessages());
		}
	}

	private void readRecordsOfType(String recordType, DataGroup filter) {

		// Kolla om man har rättigheter att läsa den posttyp

		if (recordTypeHandler.isAbstract()) {
			addChildrenOfAbstractTypeToReadRecordList(recordType, filter);
		} else {
			readRecordsOfSpecifiedRecordTypeAndAddToReadRecordList(recordType, filter);
		}
	}

	private void addChildrenOfAbstractTypeToReadRecordList(String abstractRecordType,
			DataGroup filter) {
		readResult = recordStorage.readAbstractList(abstractRecordType, filter);
		Collection<DataGroup> dataGroupList = readResult.listOfDataGroups;
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
		// FILTER DATA
		// DataGroup collectedTerms = getCollectedTermsForRecord(recordRead);
		// Set<String> usersReadRecordPartPermissions = spiderAuthorizator
		// .checkAndGetUserAuthorizationsForActionOnRecordTypeAndCollectedData(user, READ,
		// recordType, collectedTerms, hasRecordPartReadWriteConstraint());
		// recordTypeForRecord = redactDataGroup(recordTypeForRecord,
		// usersReadRecordPartPermissions);
		DataRecord dataRecord = dataGroupToRecordEnhancer.enhance(user, recordTypeForRecord,
				dataGroup);
		if (dataRecord.getActions().contains(Action.READ)) {
			readRecordList.addData(dataRecord);
		}
	}

	private void readRecordsOfSpecifiedRecordTypeAndAddToReadRecordList(String type,
			DataGroup filter) {
		readResult = recordStorage.readList(type, filter);
		Collection<DataGroup> dataGroupList = readResult.listOfDataGroups;
		this.recordType = type;
		// spiderAuthorizator.checkAndGetUserAuthorizationsForActionOnRecordTypeAndCollectedData(user,
		// null, null, null, false);

		for (DataGroup dataGroup : dataGroupList) {
			enhanceDataGroupAndAddToRecordList(dataGroup, type);
		}
	}

	private void setFromToInReadRecordList() {
		readRecordList.setTotalNo(String.valueOf(readResult.totalNumberOfMatches));
		if (resultContainsRecords()) {
			setFromToValuesForReturnedRecords();
		} else {
			setFromToValuesToZeroForResultWithoutRecords();
		}
	}

	private boolean resultContainsRecords() {
		return !readRecordList.getDataList().isEmpty();
	}

	private void setFromToValuesForReturnedRecords() {
		readRecordList.setFromNo(String.valueOf(readResult.start));
		readRecordList
				.setToNo(String.valueOf(readResult.start + readRecordList.getDataList().size()));
	}

	private void setFromToValuesToZeroForResultWithoutRecords() {
		readRecordList.setFromNo("0");
		readRecordList.setToNo("0");
	}
}
