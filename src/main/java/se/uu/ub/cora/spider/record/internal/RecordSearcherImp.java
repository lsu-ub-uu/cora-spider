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
package se.uu.ub.cora.spider.record.internal;

import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

import se.uu.ub.cora.beefeater.authentication.User;
import se.uu.ub.cora.bookkeeper.recordpart.DataRedactor;
import se.uu.ub.cora.bookkeeper.validator.DataValidator;
import se.uu.ub.cora.bookkeeper.validator.ValidationAnswer;
import se.uu.ub.cora.data.DataGroup;
import se.uu.ub.cora.data.DataList;
import se.uu.ub.cora.data.DataListProvider;
import se.uu.ub.cora.data.DataRecord;
import se.uu.ub.cora.search.RecordSearch;
import se.uu.ub.cora.search.SearchResult;
import se.uu.ub.cora.spider.authentication.Authenticator;
import se.uu.ub.cora.spider.authorization.AuthorizationException;
import se.uu.ub.cora.spider.authorization.SpiderAuthorizator;
import se.uu.ub.cora.spider.dependency.SpiderDependencyProvider;
import se.uu.ub.cora.spider.record.DataException;
import se.uu.ub.cora.spider.record.DataGroupToRecordEnhancer;
import se.uu.ub.cora.spider.record.RecordSearcher;
import se.uu.ub.cora.storage.RecordStorage;

public final class RecordSearcherImp implements RecordSearcher {
	private static final String LINKED_RECORD_ID = "linkedRecordId";
	private static final String SEARCH = "search";
	private Authenticator authenticator;
	private SpiderAuthorizator spiderAuthorizator;
	private DataValidator dataValidator;
	private User user;
	private DataGroupToRecordEnhancer dataGroupToRecordEnhancer;
	private RecordStorage recordStorage;
	private DataGroup searchData;
	private RecordSearch recordSearch;
	private DataList dataList;
	private DataGroup searchMetadata;
	private List<DataGroup> recordTypeToSearchInGroups;
	private int startRow = 1;
	private SpiderDependencyProvider dependencyProvider;

	private RecordSearcherImp(SpiderDependencyProvider dependencyProvider,
			DataGroupToRecordEnhancer dataGroupToRecordEnhancer) {
		this.dependencyProvider = dependencyProvider;
		this.dataGroupToRecordEnhancer = dataGroupToRecordEnhancer;
		this.authenticator = dependencyProvider.getAuthenticator();
		this.spiderAuthorizator = dependencyProvider.getSpiderAuthorizator();
		this.dataValidator = dependencyProvider.getDataValidator();
		this.recordStorage = dependencyProvider.getRecordStorage();
		this.recordSearch = dependencyProvider.getRecordSearch();

	}

	public static RecordSearcher usingDependencyProviderAndDataGroupToRecordEnhancer(
			SpiderDependencyProvider dependencyProvider,
			DataGroupToRecordEnhancer dataGroupToRecordEnhancer) {
		return new RecordSearcherImp(dependencyProvider, dataGroupToRecordEnhancer);
	}

	@Override
	public DataList search(String authToken, String searchId, DataGroup spiderSearchData) {
		this.searchData = spiderSearchData;
		tryToGetActiveUser(authToken);
		readSearchDataFromStorage(searchId);
		validateSearchInputForUser();
		storeStartRowValueOrSetDefault();
		SearchResult searchResult = searchUsingValidatedInput();
		return filterAndEnhanceSearchResult(searchResult);
	}

	private void storeStartRowValueOrSetDefault() {
		String start = "1";
		if (searchData.containsChildWithNameInData("start")) {
			start = searchData.getFirstAtomicValueWithNameInData("start");
		}
		startRow = Integer.parseInt(start);
	}

	private void tryToGetActiveUser(String authToken) {
		user = authenticator.getUserForToken(authToken);
	}

	private void readSearchDataFromStorage(String searchId) {
		searchMetadata = readSearchFromStorageUsingId(searchId);
		recordTypeToSearchInGroups = getRecordTypesToSearchInFromSearchGroup();
	}

	private DataGroup readSearchFromStorageUsingId(String searchId) {
		return recordStorage.read(SEARCH, searchId);
	}

	private List<DataGroup> getRecordTypesToSearchInFromSearchGroup() {
		return searchMetadata.getAllGroupsWithNameInData("recordTypeToSearchIn");
	}

	private void validateSearchInputForUser() {
		checkUserHasSearchAccessOnAllRecordTypesToSearchIn(recordTypeToSearchInGroups);
		validateIncomingSearchDataAsSpecifiedInSearchGroup();
	}

	private void checkUserHasSearchAccessOnAllRecordTypesToSearchIn(
			List<DataGroup> recordTypeToSearchInGroups) {
		recordTypeToSearchInGroups.stream().forEach(this::isAuthorized);
	}

	private void isAuthorized(DataGroup group) {
		String linkedRecordTypeId = group.getFirstAtomicValueWithNameInData(LINKED_RECORD_ID);
		spiderAuthorizator.checkUserIsAuthorizedForActionOnRecordType(user, SEARCH,
				linkedRecordTypeId);
	}

	private void validateIncomingSearchDataAsSpecifiedInSearchGroup() {
		DataGroup metadataGroup = searchMetadata.getFirstGroupWithNameInData("metadataId");
		String metadataGroupIdToValidateAgainst = metadataGroup
				.getFirstAtomicValueWithNameInData(LINKED_RECORD_ID);
		validateIncomingDataAsSpecifiedInMetadata(metadataGroupIdToValidateAgainst);
	}

	private void validateIncomingDataAsSpecifiedInMetadata(
			String metadataGroupIdToValidateAgainst) {
		ValidationAnswer validationAnswer = dataValidator
				.validateData(metadataGroupIdToValidateAgainst, searchData);
		if (validationAnswer.dataIsInvalid()) {
			throw new DataException("Data is not valid: " + validationAnswer.getErrorMessages());
		}
	}

	private SearchResult searchUsingValidatedInput() {
		List<String> list = recordTypeToSearchInGroups.stream().map(this::getLinkedRecordId)
				.collect(Collectors.toList());
		return recordSearch.searchUsingListOfRecordTypesToSearchInAndSearchData(list, searchData);
	}

	private String getLinkedRecordId(DataGroup group) {
		return group.getFirstAtomicValueWithNameInData(LINKED_RECORD_ID);
	}

	private DataList filterAndEnhanceSearchResult(SearchResult spiderSearchResult) {
		dataList = DataListProvider.getDataListWithNameOfDataType("mix");
		enhanceDataGroupsAndAddToList(spiderSearchResult);

		dataList.setFromNo(String.valueOf(startRow));
		dataList.setToNo(String.valueOf(startRow - 1 + dataList.getDataList().size()));
		dataList.setTotalNo(String.valueOf(spiderSearchResult.totalNumberOfMatches));
		return dataList;
	}

	private void enhanceDataGroupsAndAddToList(SearchResult spiderSearchResult) {
		Collection<DataGroup> dataGroupList = spiderSearchResult.listOfDataGroups;
		DataRedactor dataRedactor = dependencyProvider.getDataRedactor();
		dataGroupList.forEach(dataGroup -> filterEnhanceAndAddToList(dataGroup, dataRedactor));
	}

	private void filterEnhanceAndAddToList(DataGroup dataGroup, DataRedactor dataRedactor) {
		String recordType = extractRecordTypeFromRecordInfo(dataGroup);
		try {
			DataRecord record = dataGroupToRecordEnhancer.enhance(user, recordType, dataGroup,
					dataRedactor);
			dataList.addData(record);
		} catch (AuthorizationException noReadAuthorization) {
			// do nothing
		}
	}

	private String extractRecordTypeFromRecordInfo(DataGroup dataGroup) {
		DataGroup recordInfo = dataGroup.getFirstGroupWithNameInData("recordInfo");
		DataGroup typeGroup = recordInfo.getFirstGroupWithNameInData("type");
		return typeGroup.getFirstAtomicValueWithNameInData(LINKED_RECORD_ID);
	}
}
