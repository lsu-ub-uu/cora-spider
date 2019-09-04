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
package se.uu.ub.cora.spider.record;

import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

import se.uu.ub.cora.beefeater.authentication.User;
import se.uu.ub.cora.bookkeeper.termcollector.DataGroupTermCollector;
import se.uu.ub.cora.bookkeeper.validator.DataValidator;
import se.uu.ub.cora.bookkeeper.validator.ValidationAnswer;
import se.uu.ub.cora.data.DataGroup;
import se.uu.ub.cora.search.RecordSearch;
import se.uu.ub.cora.search.SearchResult;
import se.uu.ub.cora.spider.authentication.Authenticator;
import se.uu.ub.cora.spider.authorization.SpiderAuthorizator;
import se.uu.ub.cora.spider.data.SpiderDataGroup;
import se.uu.ub.cora.spider.data.SpiderDataList;
import se.uu.ub.cora.spider.dependency.SpiderDependencyProvider;
import se.uu.ub.cora.storage.RecordStorage;

public final class SpiderRecordSearcherImp implements SpiderRecordSearcher {
	private static final String LINKED_RECORD_ID = "linkedRecordId";
	private static final String READ = "read";
	private static final String SEARCH = "search";
	private Authenticator authenticator;
	private SpiderAuthorizator spiderAuthorizator;
	private DataValidator dataValidator;
	private User user;
	private DataGroupToRecordEnhancer dataGroupToRecordEnhancer;
	private RecordStorage recordStorage;
	private DataGroup searchData;
	private RecordSearch recordSearch;
	private SpiderDataList spiderDataList;
	private DataGroup searchMetadata;
	private List<DataGroup> recordTypeToSearchInGroups;
	private DataGroupTermCollector collectTermCollector;
	private int startRow = 1;

	private SpiderRecordSearcherImp(SpiderDependencyProvider dependencyProvider,
			DataGroupToRecordEnhancer dataGroupToRecordEnhancer) {
		this.dataGroupToRecordEnhancer = dataGroupToRecordEnhancer;
		this.authenticator = dependencyProvider.getAuthenticator();
		this.spiderAuthorizator = dependencyProvider.getSpiderAuthorizator();
		this.dataValidator = dependencyProvider.getDataValidator();
		this.recordStorage = dependencyProvider.getRecordStorage();
		this.recordSearch = dependencyProvider.getRecordSearch();
		collectTermCollector = dependencyProvider.getDataGroupTermCollector();

	}

	public static SpiderRecordSearcher usingDependencyProviderAndDataGroupToRecordEnhancer(
			SpiderDependencyProvider dependencyProvider,
			DataGroupToRecordEnhancer dataGroupToRecordEnhancer) {
		return new SpiderRecordSearcherImp(dependencyProvider, dataGroupToRecordEnhancer);
	}

	@Override
	public SpiderDataList search(String authToken, String searchId,
			SpiderDataGroup spiderSearchData) {
		this.searchData = spiderSearchData.toDataGroup();
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

	private SpiderDataList filterAndEnhanceSearchResult(SearchResult spiderSearchResult) {
		spiderDataList = SpiderDataList.withContainDataOfType("mix");
		Collection<DataGroup> dataGroupList = spiderSearchResult.listOfDataGroups;
		dataGroupList.forEach(this::filterEnhanceAndAddToList);

		spiderDataList.setFromNo(String.valueOf(startRow));
		spiderDataList.setToNo(String.valueOf(startRow - 1 + spiderDataList.getDataList().size()));
		spiderDataList.setTotalNo(String.valueOf(spiderSearchResult.totalNumberOfMatches));
		return spiderDataList;
	}

	private void filterEnhanceAndAddToList(DataGroup dataGroup) {
		String recordType = extractRecordTypeFromRecordInfo(dataGroup);
		if (isUserAuthorisedToReadData(recordType, dataGroup)) {
			spiderDataList.addData(dataGroupToRecordEnhancer.enhance(user, recordType, dataGroup));
		}
	}

	private String extractRecordTypeFromRecordInfo(DataGroup dataGroup) {
		DataGroup recordInfo = dataGroup.getFirstGroupWithNameInData("recordInfo");
		DataGroup typeGroup = recordInfo.getFirstGroupWithNameInData("type");
		return typeGroup.getFirstAtomicValueWithNameInData(LINKED_RECORD_ID);
	}

	private boolean isUserAuthorisedToReadData(String recordType, DataGroup recordFromIndex) {
		DataGroup collectedTerms = getCollectedTermsForRecord(recordType, recordFromIndex);

		return spiderAuthorizator.userIsAuthorizedForActionOnRecordTypeAndCollectedData(user, READ,
				recordType, collectedTerms);
	}

	private DataGroup getCollectedTermsForRecord(String recordType, DataGroup recordFromIndex) {

		String metadataId = getMetadataIdFromRecordType(recordType);
		return collectTermCollector.collectTerms(metadataId, recordFromIndex);
	}

	private String getMetadataIdFromRecordType(String recordType) {
		RecordTypeHandler recordTypeHandler = RecordTypeHandler
				.usingRecordStorageAndRecordTypeId(recordStorage, recordType);
		return recordTypeHandler.getMetadataId();
	}
}
