/*
 * Copyright 2017, 2024 Uppsala University Library
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

import static se.uu.ub.cora.spider.extendedfunctionality.ExtendedFunctionalityPosition.SEARCH_AFTER_AUTHORIZATION;

import java.util.Collection;
import java.util.List;

import se.uu.ub.cora.beefeater.authentication.User;
import se.uu.ub.cora.bookkeeper.recordpart.DataRedactor;
import se.uu.ub.cora.bookkeeper.validator.DataValidator;
import se.uu.ub.cora.bookkeeper.validator.ValidationAnswer;
import se.uu.ub.cora.data.DataGroup;
import se.uu.ub.cora.data.DataList;
import se.uu.ub.cora.data.DataProvider;
import se.uu.ub.cora.data.DataRecord;
import se.uu.ub.cora.data.DataRecordGroup;
import se.uu.ub.cora.data.DataRecordLink;
import se.uu.ub.cora.search.RecordSearch;
import se.uu.ub.cora.search.SearchResult;
import se.uu.ub.cora.spider.authentication.Authenticator;
import se.uu.ub.cora.spider.authorization.AuthorizationException;
import se.uu.ub.cora.spider.authorization.SpiderAuthorizator;
import se.uu.ub.cora.spider.dependency.SpiderDependencyProvider;
import se.uu.ub.cora.spider.extendedfunctionality.ExtendedFunctionality;
import se.uu.ub.cora.spider.extendedfunctionality.ExtendedFunctionalityData;
import se.uu.ub.cora.spider.extendedfunctionality.ExtendedFunctionalityPosition;
import se.uu.ub.cora.spider.extendedfunctionality.ExtendedFunctionalityProvider;
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
	private DataRecordGroup searchMetadataAsRecord;
	private List<DataGroup> recordTypeToSearchInGroups;
	private SpiderDependencyProvider dependencyProvider;
	private ExtendedFunctionalityProvider extendedFunctionalityProvider;
	private String authToken;

	private RecordSearcherImp(SpiderDependencyProvider dependencyProvider,
			DataGroupToRecordEnhancer dataGroupToRecordEnhancer) {
		this.dependencyProvider = dependencyProvider;
		this.dataGroupToRecordEnhancer = dataGroupToRecordEnhancer;
		this.authenticator = dependencyProvider.getAuthenticator();
		this.spiderAuthorizator = dependencyProvider.getSpiderAuthorizator();
		this.dataValidator = dependencyProvider.getDataValidator();
		this.recordStorage = dependencyProvider.getRecordStorage();
		this.recordSearch = dependencyProvider.getRecordSearch();
		extendedFunctionalityProvider = dependencyProvider.getExtendedFunctionalityProvider();
	}

	public static RecordSearcher usingDependencyProviderAndDataGroupToRecordEnhancer(
			SpiderDependencyProvider dependencyProvider,
			DataGroupToRecordEnhancer dataGroupToRecordEnhancer) {
		return new RecordSearcherImp(dependencyProvider, dataGroupToRecordEnhancer);
	}

	@Override
	public DataList search(String authToken, String searchId, DataGroup searchData) {
		this.authToken = authToken;
		this.searchData = searchData;
		tryToGetActiveUser();
		readSearchRecordFromStorageUsingSearchId(searchId);
		checkUserHasSearchAccessOnAllRecordTypesToSearchIn(recordTypeToSearchInGroups);
		useExtendedFunctionalityUsingPosition(SEARCH_AFTER_AUTHORIZATION);
		validateSearchInputForUser();

		SearchResult searchResult = callSearch();

		return filterAndEnhanceSearchResult(searchResult);
	}

	private void tryToGetActiveUser() {
		user = authenticator.getUserForToken(authToken);
	}

	private void readSearchRecordFromStorageUsingSearchId(String searchId) {
		searchMetadataAsRecord = recordStorage.read(SEARCH, searchId);
		recordTypeToSearchInGroups = getRecordTypesToSearchInFromSearchGroup();
	}

	private void checkUserHasSearchAccessOnAllRecordTypesToSearchIn(
			List<DataGroup> recordTypeToSearchInGroups) {
		recordTypeToSearchInGroups.stream().forEach(this::isAuthorized);
	}

	private void isAuthorized(DataGroup group) {
		String linkedRecordTypeId = getLinkedRecordId(group);
		spiderAuthorizator.checkUserIsAuthorizedForActionOnRecordType(user, SEARCH,
				linkedRecordTypeId);
	}

	private String getLinkedRecordId(DataGroup group) {
		return group.getFirstAtomicValueWithNameInData(LINKED_RECORD_ID);
	}

	private void useExtendedFunctionalityUsingPosition(ExtendedFunctionalityPosition position) {
		List<ExtendedFunctionality> extendedFunctionality = extendedFunctionalityProvider
				.getFunctionalityForPositionAndRecordType(position, "search");
		useExtendedFunctionality(extendedFunctionality);

	}

	protected void useExtendedFunctionality(List<ExtendedFunctionality> functionalityList) {
		for (ExtendedFunctionality extendedFunctionality : functionalityList) {
			ExtendedFunctionalityData data = createExtendedFunctionalityData(user);
			extendedFunctionality.useExtendedFunctionality(data);
		}
	}

	protected ExtendedFunctionalityData createExtendedFunctionalityData(User user) {
		ExtendedFunctionalityData data = new ExtendedFunctionalityData();
		data.recordType = "search";
		data.authToken = authToken;
		data.user = user;
		return data;
	}

	private void validateSearchInputForUser() {
		DataGroup metadataGroup = searchMetadataAsRecord.getFirstGroupWithNameInData("metadataId");
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

	private List<DataGroup> getRecordTypesToSearchInFromSearchGroup() {
		return searchMetadataAsRecord.getAllGroupsWithNameInData("recordTypeToSearchIn");
	}

	private SearchResult callSearch() {
		List<String> list = recordTypeToSearchInGroups.stream().map(this::getLinkedRecordId)
				.toList();
		return recordSearch.searchUsingListOfRecordTypesToSearchInAndSearchData(list, searchData);
	}

	private DataList filterAndEnhanceSearchResult(SearchResult spiderSearchResult) {
		dataList = DataProvider.createListWithNameOfDataType("mix");
		enhanceDataGroupsAndAddToList(spiderSearchResult);

		return fillDataList(spiderSearchResult);
	}

	private DataList fillDataList(SearchResult spiderSearchResult) {
		int startRow = getStartRow();
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
			DataRecord enhancedRecord = dataGroupToRecordEnhancer.enhance(user, recordType,
					dataGroup, dataRedactor);
			dataList.addData(enhancedRecord);
		} catch (AuthorizationException noReadAuthorization) {
			// do nothing
		}
	}

	private String extractRecordTypeFromRecordInfo(DataGroup dataGroup) {
		DataGroup recordInfo = dataGroup.getFirstGroupWithNameInData("recordInfo");
		DataRecordLink typeGroup = (DataRecordLink) recordInfo.getFirstChildWithNameInData("type");
		return typeGroup.getLinkedRecordId();
	}

	private int getStartRow() {
		if (searchData.containsChildWithNameInData("start")) {
			String start = searchData.getFirstAtomicValueWithNameInData("start");
			return Integer.parseInt(start);
		}
		return 1;
	}
}
