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
import se.uu.ub.cora.spider.data.SpiderDataRecord;
import se.uu.ub.cora.spider.data.SpiderSearchResult;
import se.uu.ub.cora.spider.dependency.SpiderDependencyProvider;
import se.uu.ub.cora.spider.record.storage.RecordStorage;

public class SpiderRecordSearchImp implements SpiderRecordSearcher {
	private static final String LINKED_RECORD_ID = "linkedRecordId";
	private static final String READ = "read";
	private Authenticator authenticator;
	private SpiderAuthorizator spiderAuthorizator;
	private DataValidator dataValidator;
	private SpiderDataList readRecordList;
	private String authToken;
	private User user;
	private DataGroupToRecordEnhancer dataGroupToRecordEnhancer;
	private RecordStorage recordStorage;
	private SpiderDataGroup searchData;
	private RecordSearch recordSearch;
	private SpiderDataList spiderDataList;
	private DataGroup searchGroup;

	private SpiderRecordSearchImp(SpiderDependencyProvider dependencyProvider,
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
		return new SpiderRecordSearchImp(dependencyProvider, dataGroupToRecordEnhancer);
	}

	@Override
	public SpiderDataList search(String authToken, String searchId, SpiderDataGroup searchData) {
		this.authToken = authToken;
		this.searchData = searchData;
		tryToGetActiveUser();
		searchGroup = readSearchFromStorageUsingId(searchId);
		List<DataGroup> recordTypeToSearchInGroups = getRecordTypesToSearchInFromSearchGroup();
		checkUserHasReadAccessOnAllRecordTypesToSearchIn(recordTypeToSearchInGroups);
		validateIncomingSearchDataAsSpecifiedInSearchGroup();

		spiderDataList = SpiderDataList.withContainDataOfType("mix");

		SpiderSearchResult spiderSearchResult = searchUsingSearchDataAndRecordTypeToSearchInGroups(
				searchData, recordTypeToSearchInGroups);

		Collection<DataGroup> dataGroupList = spiderSearchResult.listOfDataGroups;

		// TODO: check read access and enhance records
		// dataGroupList.stream().forEach(action);
		// List<SpiderDataRecord> listOfRecords = dataGroupList.stream()
		// .map(dataGroup ->
		// enhanceGroupToRecord(dataGroup)).collect(Collectors.toList());

		// listOfRecords.forEach(dataGroup ->
		// addFilteredAndEnhancedToList(dataGroup));
		dataGroupList.forEach(this::addFilteredAndEnhancedToList);

		// TODO: return result

		// spiderDataList.
		return spiderDataList;
	}

	private void tryToGetActiveUser() {
		user = authenticator.getUserForToken(authToken);
	}

	private DataGroup readSearchFromStorageUsingId(String searchId) {
		return recordStorage.read("search", searchId);
	}

	private List<DataGroup> getRecordTypesToSearchInFromSearchGroup() {
		return searchGroup.getAllGroupsWithNameInData("recordTypeToSearchIn");
	}

	private void checkUserHasReadAccessOnAllRecordTypesToSearchIn(
			List<DataGroup> recordTypeToSearchInGroups) {
		recordTypeToSearchInGroups.stream().forEach(this::isAuthorized);
	}

	private void validateIncomingSearchDataAsSpecifiedInSearchGroup() {
		DataGroup metadataGroup = searchGroup.getFirstGroupWithNameInData("metadataId");
		String metadataGroupIdToValidateAgainst = metadataGroup
				.getFirstAtomicValueWithNameInData(LINKED_RECORD_ID);
		validateIncomingDataAsSpecifiedInMetadata(metadataGroupIdToValidateAgainst);
	}

	private void validateIncomingDataAsSpecifiedInMetadata(
			String metadataGroupIdToValidateAgainst) {
		DataGroup dataGroup = searchData.toDataGroup();
		ValidationAnswer validationAnswer = dataValidator
				.validateData(metadataGroupIdToValidateAgainst, dataGroup);
		if (validationAnswer.dataIsInvalid()) {
			throw new DataException("Data is not valid: " + validationAnswer.getErrorMessages());
		}
	}

	private SpiderSearchResult searchUsingSearchDataAndRecordTypeToSearchInGroups(
			SpiderDataGroup searchData, List<DataGroup> recordTypeToSearchInGroups) {
		List<String> list = recordTypeToSearchInGroups.stream()
				// .map(group ->
				// group.getFirstAtomicValueWithNameInData(LINKED_RECORD_ID))
				// .collect(Collectors.toList());
				.map(this::getLinkedRecordId).collect(Collectors.toList());
		SpiderSearchResult spiderSearchResult = recordSearch
				.searchUsingListOfRecordTypesToSearchInAndSearchData(list,
						searchData.toDataGroup());
		return spiderSearchResult;
	}

	private String getLinkedRecordId(DataGroup group) {
		return group.getFirstAtomicValueWithNameInData(LINKED_RECORD_ID);
	}

	private void addFilteredAndEnhancedToList(DataGroup dataGroup) {
		String recordType = dataGroup.getFirstGroupWithNameInData("recordInfo")
				.getFirstAtomicValueWithNameInData("type");
		spiderDataList.addData(dataGroupToRecordEnhancer.enhance(user, recordType, dataGroup));
	}

	private SpiderDataRecord enhanceGroupToRecord(DataGroup dataGroup) {
		return null;

	}
	// private void checkUserIsAuthorisedToReadData(DataGroup recordRead) {
	// spiderAuthorizator.checkUserIsAuthorizedForActionOnRecordTypeAndRecord(user,
	// READ,
	// recordType, recordRead);
	// }

	private void isAuthorized(DataGroup group) {
		String linkedRecordTypeId = group.getFirstAtomicValueWithNameInData(LINKED_RECORD_ID);
		spiderAuthorizator.checkUserIsAuthorizedForActionOnRecordType(user, READ,
				linkedRecordTypeId);
	}

}
