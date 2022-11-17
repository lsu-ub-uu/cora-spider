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

import java.util.Collection;
import java.util.List;
import java.util.Set;

import se.uu.ub.cora.bookkeeper.linkcollector.DataRecordLinkCollector;
import se.uu.ub.cora.bookkeeper.recordpart.DataRedactor;
import se.uu.ub.cora.bookkeeper.termcollector.DataGroupTermCollector;
import se.uu.ub.cora.bookkeeper.validator.DataValidationException;
import se.uu.ub.cora.bookkeeper.validator.DataValidator;
import se.uu.ub.cora.bookkeeper.validator.ValidationAnswer;
import se.uu.ub.cora.data.DataGroup;
import se.uu.ub.cora.data.DataList;
import se.uu.ub.cora.data.DataProvider;
import se.uu.ub.cora.data.DataRecord;
import se.uu.ub.cora.data.collected.CollectTerms;
import se.uu.ub.cora.data.collected.Link;
import se.uu.ub.cora.spider.authentication.Authenticator;
import se.uu.ub.cora.spider.authorization.AuthorizationException;
import se.uu.ub.cora.spider.authorization.SpiderAuthorizator;
import se.uu.ub.cora.spider.data.DataGroupToFilter;
import se.uu.ub.cora.spider.dependency.SpiderDependencyProvider;
import se.uu.ub.cora.spider.record.DataException;
import se.uu.ub.cora.spider.record.DataGroupToRecordEnhancer;
import se.uu.ub.cora.spider.record.RecordListReader;
import se.uu.ub.cora.spider.recordtype.RecordTypeHandler;
import se.uu.ub.cora.storage.Filter;
import se.uu.ub.cora.storage.RecordStorage;
import se.uu.ub.cora.storage.StorageReadResult;

public final class RecordListReaderImp extends RecordHandler implements RecordListReader {
	private Authenticator authenticator;
	private SpiderAuthorizator spiderAuthorizator;
	private DataList readRecordList;
	private DataGroupToRecordEnhancer dataGroupToRecordEnhancer;
	private DataValidator dataValidator;
	private StorageReadResult readResult;
	private RecordTypeHandler recordTypeHandler;

	private RecordListReaderImp(SpiderDependencyProvider dependencyProvider,
			DataGroupToRecordEnhancer dataGroupToRecordEnhancer) {
		this.dependencyProvider = dependencyProvider;
		this.dataGroupToRecordEnhancer = dataGroupToRecordEnhancer;
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
		recordTypeHandler = dependencyProvider.getRecordTypeHandler(recordType);
		ensureActiveUserHasListPermissionUsingAuthToken(authToken);

		readRecordList = DataProvider.createListWithNameOfDataType(recordType);
		validateFilterIfNotEmpty(filter, recordType);

		readRecordsOfType(filter);
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
			spiderAuthorizator.checkUserIsAuthorizedForActionOnRecordType(user, "list", recordType);
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
		List<String> listOfTypes = recordTypeHandler.getListOfRecordTypeIdsToReadFromStorage();

		readResult = recordStorage.readList(listOfTypes, filter);
		Collection<DataGroup> dataGroupList = readResult.listOfDataGroups;

		// DatabaseStorageInstanceProvider ip = new DatabaseStorageInstanceProvider();
		// RecordStorage db = ip.getRecordStorage();
		for (DataGroup dataGroup : dataGroupList) {
			String type = extractRecordTypeFromDataGroup(dataGroup);
			recordType = type;
			// hack(db, type, dataGroup);
			enhanceDataGroupAndPossiblyAddToRecordList(dataGroup, type, dataRedactor);
		}
	}

	private String extractRecordTypeFromDataGroup(DataGroup dataGroup) {
		DataGroup recordInfo = dataGroup.getFirstGroupWithNameInData("recordInfo");
		DataGroup typeGroup = recordInfo.getFirstGroupWithNameInData("type");

		return typeGroup.getFirstAtomicValueWithNameInData("linkedRecordId");
	}

	private void hack(RecordStorage db, String type, DataGroup dataGroup) {
		String id = extractRecordIdFromDataGroup(dataGroup);
		if (!db.recordExists(List.of(type), id)) {
			String dataDivider = extractRecordDataDividerFromDataGroup(dataGroup);

			String metadataId = recordTypeHandler.getMetadataId();

			DataGroupTermCollector dataGroupTermCollector = dependencyProvider
					.getDataGroupTermCollector();
			CollectTerms collectTerms = dataGroupTermCollector.collectTerms(metadataId, dataGroup);

			// TODO: links can be identical... will not work
			DataRecordLinkCollector dataRecordLinkCollector = dependencyProvider
					.getDataRecordLinkCollector();
			Set<Link> collectedLinks = dataRecordLinkCollector.collectLinks(metadataId, dataGroup);

			try {
				db.create(type, id, dataGroup, collectTerms.storageTerms, collectedLinks,
						dataDivider);
				// db.create(type, id, dataGroup, collectTerms.storageTerms, Collections.emptySet(),
				// dataDivider);
			} catch (Exception e) {
				// DO nothing for now :)
				String x = "";
				x += "Y";
			}
		}
	}

	private String extractRecordIdFromDataGroup(DataGroup dataGroup) {
		DataGroup recordInfo = dataGroup.getFirstGroupWithNameInData("recordInfo");
		return recordInfo.getFirstAtomicValueWithNameInData("id");
	}

	private String extractRecordDataDividerFromDataGroup(DataGroup dataGroup) {
		DataGroup recordInfo = dataGroup.getFirstGroupWithNameInData("recordInfo");
		DataGroup dividerGroup = recordInfo.getFirstGroupWithNameInData("dataDivider");

		return dividerGroup.getFirstAtomicValueWithNameInData("linkedRecordId");
	}

	private void enhanceDataGroupAndPossiblyAddToRecordList(DataGroup dataGroup,
			String recordTypeForRecord, DataRedactor dataRedactor) {
		try {
			DataRecord dataRecord = dataGroupToRecordEnhancer.enhance(user, recordTypeForRecord,
					dataGroup, dataRedactor);
			readRecordList.addData(dataRecord);
		} catch (AuthorizationException noReadAuthorization) {
			// do nothing
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
