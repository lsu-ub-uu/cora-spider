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
import se.uu.ub.cora.bookkeeper.data.DataGroup;
import se.uu.ub.cora.bookkeeper.validator.DataValidator;
import se.uu.ub.cora.bookkeeper.validator.ValidationAnswer;
import se.uu.ub.cora.spider.authentication.Authenticator;
import se.uu.ub.cora.spider.authorization.SpiderAuthorizator;
import se.uu.ub.cora.spider.data.SpiderDataGroup;
import se.uu.ub.cora.spider.data.SpiderDataList;
import se.uu.ub.cora.spider.data.SpiderSearchResult;
import se.uu.ub.cora.spider.dependency.SpiderDependencyProvider;
import se.uu.ub.cora.spider.record.storage.RecordStorage;

public final class SpiderRecordSearcherImp implements SpiderRecordSearcher {
	private static final String LINKED_RECORD_ID = "linkedRecordId";
	private static final String READ = "read";
	private Authenticator authenticator;
	private SpiderAuthorizator spiderAuthorizator;
	private DataValidator dataValidator;
	private User user;
	private DataGroupToRecordEnhancer dataGroupToRecordEnhancer;
	private RecordStorage recordStorage;
	private SpiderDataGroup searchData;
	private RecordSearch recordSearch;
	private SpiderDataList spiderDataList;
	private DataGroup searchMetadata;
	private List<DataGroup> recordTypeToSearchInGroups;

	private SpiderRecordSearcherImp(SpiderDependencyProvider dependencyProvider,
			DataGroupToRecordEnhancer dataGroupToRecordEnhancer) {
		this.dataGroupToRecordEnhancer = dataGroupToRecordEnhancer;
		this.authenticator = dependencyProvider.getAuthenticator();
		this.spiderAuthorizator = dependencyProvider.getSpiderAuthorizator();
		this.dataValidator = dependencyProvider.getDataValidator();
		this.recordStorage = dependencyProvider.getRecordStorage();
		this.recordSearch = dependencyProvider.getRecordSearch();

	}

	public static SpiderRecordSearcher usingDependencyProviderAndDataGroupToRecordEnhancer(
			SpiderDependencyProvider dependencyProvider,
			DataGroupToRecordEnhancer dataGroupToRecordEnhancer) {
		return new SpiderRecordSearcherImp(dependencyProvider, dataGroupToRecordEnhancer);
	}

	@Override
	public SpiderDataList search(String authToken, String searchId,
			SpiderDataGroup spiderSearchData) {
		this.searchData = spiderSearchData;
		tryToGetActiveUser(authToken);
		readSearchDataFromStorage(searchId);
		validateSearchInputForUser();
		SpiderSearchResult searchResult = searchUsingValidatedInput();
		return filterAndEnahanceSearchResult(searchResult);
	}

	private void tryToGetActiveUser(String authToken) {
		user = authenticator.getUserForToken(authToken);
	}

	private void readSearchDataFromStorage(String searchId) {
		searchMetadata = readSearchFromStorageUsingId(searchId);
		recordTypeToSearchInGroups = getRecordTypesToSearchInFromSearchGroup();
	}

	private DataGroup readSearchFromStorageUsingId(String searchId) {
		return recordStorage.read("search", searchId);
	}

	private List<DataGroup> getRecordTypesToSearchInFromSearchGroup() {
		return searchMetadata.getAllGroupsWithNameInData("recordTypeToSearchIn");
	}

	private void validateSearchInputForUser() {
		checkUserHasReadAccessOnAllRecordTypesToSearchIn(recordTypeToSearchInGroups);
		validateIncomingSearchDataAsSpecifiedInSearchGroup();
	}

	private void checkUserHasReadAccessOnAllRecordTypesToSearchIn(
			List<DataGroup> recordTypeToSearchInGroups) {
		recordTypeToSearchInGroups.stream().forEach(this::isAuthorized);
	}

	private void isAuthorized(DataGroup group) {
		String linkedRecordTypeId = group.getFirstAtomicValueWithNameInData(LINKED_RECORD_ID);
		spiderAuthorizator.checkUserIsAuthorizedForActionOnRecordType(user, READ,
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
				.validateData(metadataGroupIdToValidateAgainst, searchData.toDataGroup());
		if (validationAnswer.dataIsInvalid()) {
			throw new DataException("Data is not valid: " + validationAnswer.getErrorMessages());
		}
	}

	private SpiderSearchResult searchUsingValidatedInput() {
		List<String> list = recordTypeToSearchInGroups.stream().map(this::getLinkedRecordId)
				.collect(Collectors.toList());
		return recordSearch.searchUsingListOfRecordTypesToSearchInAndSearchData(list, searchData);
	}

	private String getLinkedRecordId(DataGroup group) {
		return group.getFirstAtomicValueWithNameInData(LINKED_RECORD_ID);
	}

	private SpiderDataList filterAndEnahanceSearchResult(SpiderSearchResult spiderSearchResult) {
		spiderDataList = SpiderDataList.withContainDataOfType("mix");
		Collection<DataGroup> dataGroupList = spiderSearchResult.listOfDataGroups;
		dataGroupList.forEach(this::filterEnhanceAndAddToList);
		return spiderDataList;
	}

	private void filterEnhanceAndAddToList(DataGroup dataGroup) {
		String recordType = dataGroup.getFirstGroupWithNameInData("recordInfo")
				.getFirstAtomicValueWithNameInData("type");
		if (isUserAuthorisedToReadData(recordType, dataGroup)) {
			spiderDataList.addData(dataGroupToRecordEnhancer.enhance(user, recordType, dataGroup));
		}
	}

	private boolean isUserAuthorisedToReadData(String recordType, DataGroup recordRead) {
		return spiderAuthorizator.userIsAuthorizedForActionOnRecordTypeAndRecord(user, READ,
				recordType, recordRead);
	}

}
