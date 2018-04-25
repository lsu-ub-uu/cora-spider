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

import java.util.ArrayList;
import java.util.Collection;

import se.uu.ub.cora.beefeater.authentication.User;
import se.uu.ub.cora.bookkeeper.data.DataGroup;
import se.uu.ub.cora.bookkeeper.termcollector.DataGroupTermCollector;
import se.uu.ub.cora.spider.authentication.Authenticator;
import se.uu.ub.cora.spider.authorization.SpiderAuthorizator;
import se.uu.ub.cora.spider.data.Action;
import se.uu.ub.cora.spider.data.SpiderDataGroup;
import se.uu.ub.cora.spider.data.SpiderDataList;
import se.uu.ub.cora.spider.data.SpiderDataRecordLink;
import se.uu.ub.cora.spider.dependency.SpiderDependencyProvider;

public class SpiderRecordIncomingLinksReaderImp extends SpiderRecordHandler
		implements SpiderRecordIncomingLinksReader {
	private static final String READ = "read";
	private Authenticator authenticator;
	private SpiderAuthorizator spiderAuthorizator;
	private String authToken;
	private RecordTypeHandler recordTypeHandler;
	private User user;
	private DataGroupTermCollector collectTermCollector;

	public SpiderRecordIncomingLinksReaderImp(SpiderDependencyProvider dependencyProvider) {
		this.authenticator = dependencyProvider.getAuthenticator();
		this.spiderAuthorizator = dependencyProvider.getSpiderAuthorizator();
		this.recordStorage = dependencyProvider.getRecordStorage();
		this.collectTermCollector = dependencyProvider.getDataGroupTermCollector();
	}

	public static SpiderRecordIncomingLinksReader usingDependencyProvider(
			SpiderDependencyProvider dependencyProvider) {
		return new SpiderRecordIncomingLinksReaderImp(dependencyProvider);
	}

	@Override
	public SpiderDataList readIncomingLinks(String authToken, String recordType, String recordId) {
		this.authToken = authToken;
		this.recordType = recordType;
		this.recordId = recordId;
		recordTypeHandler = RecordTypeHandler.usingRecordStorageAndRecordTypeId(recordStorage,
				recordType);
		tryToGetActiveUser();
		checkRecordsRecordTypeNotAbstract();
		DataGroup recordRead = recordStorage.read(recordType, recordId);

		checkUserIsAuthorisedToReadData(recordRead);

		return collectLinksAndConvertToSpiderDataList();
	}

	private void tryToGetActiveUser() {
		user = authenticator.getUserForToken(authToken);
	}

	private void checkRecordsRecordTypeNotAbstract() {
		if (recordTypeHandler.isAbstract()) {
			throw new MisuseException("Reading for record: " + recordId
					+ " on the abstract recordType:" + recordType + " is not allowed");
		}
	}

	private void checkUserIsAuthorisedToReadData(DataGroup recordRead) {
		DataGroup collectedTerms = getCollectedTermsForRecord(recordRead);
		spiderAuthorizator.checkUserIsAuthorizedForActionOnRecordTypeAndCollectedData(user, READ,
				recordType, collectedTerms);
	}

	private DataGroup getCollectedTermsForRecord(DataGroup recordRead) {
		String metadataId = recordTypeHandler.getMetadataId();
		return collectTermCollector.collectTerms(metadataId, recordRead);
	}

	private SpiderDataList collectLinksAndConvertToSpiderDataList() {
		Collection<DataGroup> links = new ArrayList<>();
		addLinksPointingToRecord(links);
		possiblyAddLinksPointingToRecordByParentRecordType(links);

		return convertLinksPointingToRecordToSpiderDataList(links);
	}

	private void addLinksPointingToRecord(Collection<DataGroup> links) {
		Collection<DataGroup> linksPointingToRecord = recordStorage
				.generateLinkCollectionPointingToRecord(recordType, recordId);
		links.addAll(linksPointingToRecord);
	}

	private void possiblyAddLinksPointingToRecordByParentRecordType(Collection<DataGroup> links) {
		DataGroup recordTypeDataGroup = getRecordTypeDefinition();
		if (recordTypeHasParent(recordTypeDataGroup)) {
			addLinksPointingToRecordByParentRecordType(links, recordTypeDataGroup);
		}
	}

	private boolean recordTypeHasParent(DataGroup recordTypeDataGroup) {
		return recordTypeDataGroup.containsChildWithNameInData("parentId");
	}

	private void addLinksPointingToRecordByParentRecordType(Collection<DataGroup> links,
			DataGroup recordTypeDataGroup) {
		String parentId = extractParentId(recordTypeDataGroup);
		Collection<DataGroup> linksPointingToParentType = recordStorage
				.generateLinkCollectionPointingToRecord(parentId, recordId);
		links.addAll(linksPointingToParentType);
	}

	private String extractParentId(DataGroup recordTypeDataGroup) {
		DataGroup parent = recordTypeDataGroup.getFirstGroupWithNameInData("parentId");
		return parent.getFirstAtomicValueWithNameInData("linkedRecordId");
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
