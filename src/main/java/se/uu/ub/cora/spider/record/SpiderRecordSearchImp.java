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
import se.uu.ub.cora.spider.dependency.SpiderDependencyProvider;
import se.uu.ub.cora.spider.dependency.SpiderDependencyProviderSpy;
import se.uu.ub.cora.spider.record.storage.RecordStorage;

public class SpiderRecordSearchImp implements SpiderRecordSearcher {
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
			SpiderDependencyProviderSpy dependencyProvider,
			DataGroupToRecordEnhancerSpy dataGroupToRecordEnhancer) {
		return new SpiderRecordSearchImp(dependencyProvider, dataGroupToRecordEnhancer);
	}

	@Override
	public SpiderDataList search(String authToken, String searchId, SpiderDataGroup searchData) {
		this.authToken = authToken;
		this.searchData = searchData;
		tryToGetActiveUser();

		// TODO: read search by searchId from storage,
		DataGroup searchGroup = recordStorage.read("search", searchId);

		// TODO: check that we have READ access on all recordTypeToSearchIn from
		// stored search
		List<DataGroup> recordTypeToSearchInGroups = searchGroup
				.getAllGroupsWithNameInData("recordTypeToSearchIn");
		recordTypeToSearchInGroups.stream().forEach(this::isAuthorized);
		// List<String> list =
		// recordTypeToSearchInGroups.stream().map(this::getLinkedRecordId)
		// .collect(Collectors.toList());
		List<String> list = recordTypeToSearchInGroups.stream()
				.map(group -> group.getFirstAtomicValueWithNameInData("linkedRecordId"))
				.collect(Collectors.toList());

		// TODO: validate incoming search data against metadataId stored in
		// search
		DataGroup metadataGroup = searchGroup.getFirstGroupWithNameInData("metadataId");
		String metadataGroupIdToValidateAgainst = metadataGroup
				.getFirstAtomicValueWithNameInData("linkedRecordId");
		validateIncomingDataAsSpecifiedInMetadata(metadataGroupIdToValidateAgainst);

		// TODO: search
		Collection<DataGroup> dataGroupList = recordSearch
				.searchUsingListOfRecordTypesToSearchInAndSearchData(list,
						searchData.toDataGroup());

		// TODO: check read access and enhance records

		// TODO: return result

		return SpiderDataList.withContainDataOfType("mix");
	}

	private String getLinkedRecordId(DataGroup group) {
		return group.getFirstAtomicValueWithNameInData("linkedRecordId");
	}

	private void isAuthorized(DataGroup group) {
		// TODO Auto-generated method stub
		String linkedRecordTypeId = group.getFirstAtomicValueWithNameInData("linkedRecordId");
		spiderAuthorizator.checkUserIsAuthorizedForActionOnRecordType(user, "read",
				linkedRecordTypeId);
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

	private void tryToGetActiveUser() {
		user = authenticator.getUserForToken(authToken);
	}
}
