/*
 * Copyright 2015, 2016, 2018, 2019, 2020, 2022 Uppsala University Library
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

package se.uu.ub.cora.spider.record.internal;

import static se.uu.ub.cora.spider.extendedfunctionality.ExtendedFunctionalityPosition.READLIST_AFTER_AUTHORIZATION;

import java.util.Collection;
import java.util.List;

import se.uu.ub.cora.bookkeeper.recordpart.DataRedactor;
import se.uu.ub.cora.bookkeeper.recordtype.RecordTypeHandler;
import se.uu.ub.cora.bookkeeper.validator.DataValidationException;
import se.uu.ub.cora.bookkeeper.validator.DataValidator;
import se.uu.ub.cora.bookkeeper.validator.ValidationAnswer;
import se.uu.ub.cora.data.DataGroup;
import se.uu.ub.cora.data.DataList;
import se.uu.ub.cora.data.DataProvider;
import se.uu.ub.cora.data.DataRecord;
import se.uu.ub.cora.data.DataRecordGroup;
import se.uu.ub.cora.spider.authentication.Authenticator;
import se.uu.ub.cora.spider.authorization.AuthorizationException;
import se.uu.ub.cora.spider.authorization.SpiderAuthorizator;
import se.uu.ub.cora.spider.data.DataGroupToFilter;
import se.uu.ub.cora.spider.dependency.SpiderDependencyProvider;
import se.uu.ub.cora.spider.extendedfunctionality.ExtendedFunctionality;
import se.uu.ub.cora.spider.extendedfunctionality.ExtendedFunctionalityData;
import se.uu.ub.cora.spider.extendedfunctionality.ExtendedFunctionalityPosition;
import se.uu.ub.cora.spider.extendedfunctionality.ExtendedFunctionalityProvider;
import se.uu.ub.cora.spider.record.DataException;
import se.uu.ub.cora.spider.record.DataGroupToRecordEnhancer;
import se.uu.ub.cora.spider.record.RecordListReader;
import se.uu.ub.cora.storage.Filter;
import se.uu.ub.cora.storage.StorageReadResult;

public final class RecordListReaderImp extends RecordHandler implements RecordListReader {
	private Authenticator authenticator;
	private SpiderAuthorizator spiderAuthorizator;
	private DataList dataList;
	private DataGroupToRecordEnhancer dataGroupToRecordEnhancer;
	private DataValidator dataValidator;
	private StorageReadResult readResult;
	private RecordTypeHandler recordTypeHandler;
	private ExtendedFunctionalityProvider extendedFunctionalityProvider;

	private RecordListReaderImp(SpiderDependencyProvider dependencyProvider,
			DataGroupToRecordEnhancer dataGroupToRecordEnhancer) {
		this.dependencyProvider = dependencyProvider;
		this.dataGroupToRecordEnhancer = dataGroupToRecordEnhancer;
		this.extendedFunctionalityProvider = dependencyProvider.getExtendedFunctionalityProvider();
		authenticator = dependencyProvider.getAuthenticator();
		spiderAuthorizator = dependencyProvider.getSpiderAuthorizator();
		recordStorage = dependencyProvider.getRecordStorage();
		dataValidator = dependencyProvider.getDataValidator();
	}

	public static RecordListReaderImp usingDependencyProviderAndDataGroupToRecordEnhancer(
			SpiderDependencyProvider dependencyProvider,
			DataGroupToRecordEnhancer dataGroupToRecordEnhancer) {
		return new RecordListReaderImp(dependencyProvider, dataGroupToRecordEnhancer);
	}

	@Override
	public DataList readRecordList(String authToken, String recordType, DataGroup filter) {
		this.recordType = recordType;
		this.authToken = authToken;
		recordTypeHandler = dependencyProvider.getRecordTypeHandler(recordType);
		ensureActiveUserHasListPermissionUsingAuthToken();
		useExtendedFunctionalityForPosition(READLIST_AFTER_AUTHORIZATION);

		dataList = DataProvider.createListWithNameOfDataType(recordType);
		validateFilterIfNotEmpty(filter, recordType);

		readRecordsOfType(filter);
		setFromToInReadRecordList();

		return dataList;
	}

	private void ensureActiveUserHasListPermissionUsingAuthToken() {
		tryToGetActiveUser();
		checkUserIsAuthorizedForActionOnRecordType();
	}

	private void tryToGetActiveUser() {
		user = authenticator.getUserForToken(authToken);
	}

	private void checkUserIsAuthorizedForActionOnRecordType() {
		if (listedRecordTypeIsNotPublicRead()) {
			spiderAuthorizator.checkUserIsAuthorizedForActionOnRecordType(user, "list", recordType);
		}
	}

	private void useExtendedFunctionalityForPosition(ExtendedFunctionalityPosition position) {
		ExtendedFunctionalityData data = createExtendedFunctionalityData();
		useExtendedFunctionality(position, data);
	}

	private ExtendedFunctionalityData createExtendedFunctionalityData() {
		ExtendedFunctionalityData data = new ExtendedFunctionalityData();
		data.recordType = recordType;
		data.authToken = authToken;
		data.user = user;
		return data;
	}

	private void useExtendedFunctionality(ExtendedFunctionalityPosition position,
			ExtendedFunctionalityData data) {
		List<ExtendedFunctionality> functionalityList = extendedFunctionalityProvider
				.getFunctionalityForPositionAndRecordType(position, recordType);
		for (ExtendedFunctionality extendedFunctionality : functionalityList) {
			extendedFunctionality.useExtendedFunctionality(data);
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
		return filter.hasChildren();
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

	private void readRecordsOfType(DataGroup dataFilter) {
		DataGroupToFilter converter = dependencyProvider.getDataGroupToFilterConverter();
		Filter filter = converter.convert(dataFilter);

		DataRedactor dataRedactor = dependencyProvider.getDataRedactor();
		readAndAddToReadRecordList(filter, dataRedactor);
	}

	private void readAndAddToReadRecordList(Filter filter, DataRedactor dataRedactor) {
		readResult = recordStorage.readList(recordType, filter);
		Collection<DataRecordGroup> dataGroupList = readResult.listOfDataRecordGroups;
		for (DataRecordGroup dataRecordGroup : dataGroupList) {
			enhanceDataGroupAndPossiblyAddToRecordList(dataRecordGroup, dataRecordGroup.getType(),
					dataRedactor);
		}
	}

	private void enhanceDataGroupAndPossiblyAddToRecordList(DataRecordGroup dataRecordGroup,
			String recordTypeForRecord, DataRedactor dataRedactor) {
		try {
			DataRecord dataRecord = dataGroupToRecordEnhancer.enhance(user, recordTypeForRecord,
					dataRecordGroup, dataRedactor);
			dataList.addData(dataRecord);
		} catch (AuthorizationException noReadAuthorization) {
			// do nothing
		}
	}

	private void setFromToInReadRecordList() {
		dataList.setTotalNo(String.valueOf(readResult.totalNumberOfMatches));
		if (resultContainsRecords()) {
			setFromToValuesForReturnedRecords();
		} else {
			setFromToValuesToZeroForResultWithoutRecords();
		}
	}

	private boolean resultContainsRecords() {
		return !dataList.getDataList().isEmpty();
	}

	private void setFromToValuesForReturnedRecords() {
		dataList.setFromNo(String.valueOf(readResult.start));
		dataList.setToNo(String.valueOf(readResult.start + dataList.getDataList().size()));
	}

	private void setFromToValuesToZeroForResultWithoutRecords() {
		dataList.setFromNo("0");
		dataList.setToNo("0");
	}
}
