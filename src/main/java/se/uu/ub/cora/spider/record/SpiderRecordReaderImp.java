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
import java.util.List;
import java.util.Map;
import java.util.Set;

import se.uu.ub.cora.beefeater.authentication.User;
import se.uu.ub.cora.bookkeeper.data.DataGroup;
import se.uu.ub.cora.spider.authentication.Authenticator;
import se.uu.ub.cora.spider.authorization.AuthorizationException;
import se.uu.ub.cora.spider.authorization.PermissionRuleCalculator;
import se.uu.ub.cora.spider.authorization.SpiderAuthorizator;
import se.uu.ub.cora.spider.data.Action;
import se.uu.ub.cora.spider.data.SpiderDataGroup;
import se.uu.ub.cora.spider.data.SpiderDataList;
import se.uu.ub.cora.spider.data.SpiderDataRecord;
import se.uu.ub.cora.spider.data.SpiderDataRecordLink;
import se.uu.ub.cora.spider.dependency.SpiderDependencyProvider;

public final class SpiderRecordReaderImp extends SpiderRecordHandler implements SpiderRecordReader {
	private Authenticator authenticator;
	private SpiderAuthorizator spiderAuthorizator;
	private PermissionRuleCalculator ruleCalculator;
	private String userId;
	private User user;
	private String authToken;
	private RecordTypeHandler recordTypeHandler;

	private SpiderRecordReaderImp(SpiderDependencyProvider dependencyProvider) {
		this.authenticator = dependencyProvider.getAuthenticator();
		this.spiderAuthorizator = dependencyProvider.getSpiderAuthorizator();
		this.recordStorage = dependencyProvider.getRecordStorage();
		this.ruleCalculator = dependencyProvider.getPermissionRuleCalculator();
	}

	public static SpiderRecordReaderImp usingDependencyProvider(
			SpiderDependencyProvider dependencyProvider) {
		return new SpiderRecordReaderImp(dependencyProvider);
	}

	@Override
	public SpiderDataRecord readRecord(String authToken, String recordType, String recordId) {

		this.authToken = authToken;
		this.recordType = recordType;
		this.recordId = recordId;
		recordTypeHandler = RecordTypeHandler.usingRecordStorageAndRecordTypeId(recordStorage,
				recordType);
		tryToGetActiveUser();
		checkRecordsRecordTypeNotAbstract();
		DataGroup recordRead = recordStorage.read(recordType, recordId);

		checkUserIsAuthorisedToReadData(recordRead);

		// filter data
		// TODO: filter hidden data if user does not have right to see it

		SpiderDataGroup spiderDataGroup = SpiderDataGroup.fromDataGroup(recordRead);
		return createDataRecordContainingDataGroup(spiderDataGroup);
	}

	private void tryToGetActiveUser() {
		user = authenticator.tryToGetActiveUser(authToken);
	}

	private void checkRecordsRecordTypeNotAbstract() {
		if (recordTypeHandler.isAbstract()) {
			throw new MisuseException("Reading for record: " + recordId
					+ " on the abstract recordType:" + recordType + " is not allowed");
		}
	}

	private void checkUserIsAuthorisedToReadData(DataGroup recordRead) {
		if (isNotAuthorizedToRead(recordRead)) {
			throw new AuthorizationException(
					"User:" + userId + " is not authorized to read for record:" + recordId
							+ " of type:" + recordType);
		}
	}

	private boolean isNotAuthorizedToRead(DataGroup recordRead) {
		String action = "read";
		List<Map<String, Set<String>>> requiredRules = ruleCalculator
				.calculateRulesForActionAndRecordTypeAndData(action, recordType, recordRead);
		return !spiderAuthorizator.userSatisfiesRequiredRules(user, requiredRules);
	}

	@Override
	public SpiderDataList readIncomingLinks(String authToken, String recordType, String recordId) {
		// TODO: break out this method
		this.authToken = authToken;
		this.recordType = recordType;
		this.recordId = recordId;
		recordTypeHandler = RecordTypeHandler.usingRecordStorageAndRecordTypeId(recordStorage,
				recordType);
		tryToGetActiveUser();
		checkRecordsRecordTypeNotAbstract();
		DataGroup recordRead = recordStorage.read(recordType, recordId);

		checkUserIsAuthorisedToReadData(recordRead);

		Collection<DataGroup> linksPointingToRecord = recordStorage
				.generateLinkCollectionPointingToRecord(recordType, recordId);

		return convertLinksPointingToRecordToSpiderDataList(linksPointingToRecord);
	}

	private SpiderDataList convertLinksPointingToRecordToSpiderDataList(
			Collection<DataGroup> dataGroupLinks) {
		SpiderDataList recordToRecordLinkList = createDataListForRecordToRecordLinks(
				dataGroupLinks);
		convertAndAddLinksToLinkList(dataGroupLinks, recordToRecordLinkList);
		return recordToRecordLinkList;
	}

	private SpiderDataList createDataListForRecordToRecordLinks(Collection<DataGroup> links) {
		SpiderDataList recordToRecordList = SpiderDataList
				.withContainDataOfType("recordToRecordLink");
		recordToRecordList.setFromNo("1");
		recordToRecordList.setToNo(Integer.toString(links.size()));
		recordToRecordList.setTotalNo(Integer.toString(links.size()));
		return recordToRecordList;
	}

	private void convertAndAddLinksToLinkList(Collection<DataGroup> links,
			SpiderDataList recordToRecordList) {
		for (DataGroup dataGroup : links) {
			SpiderDataGroup spiderDataGroup = SpiderDataGroup.fromDataGroup(dataGroup);
			addReadActionToIncomingLinks(spiderDataGroup);
			recordToRecordList.addData(spiderDataGroup);
		}
	}

	private void addReadActionToIncomingLinks(SpiderDataGroup spiderDataGroup) {
		SpiderDataRecordLink spiderRecordLink = (SpiderDataRecordLink) spiderDataGroup
				.getFirstChildWithNameInData("from");
		spiderRecordLink.addAction(Action.READ);
	}
}
