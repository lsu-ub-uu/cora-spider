/*
 * Copyright 2017, 2022 Uppsala University Library
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
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import se.uu.ub.cora.bookkeeper.recordtype.RecordTypeHandler;
import se.uu.ub.cora.bookkeeper.termcollector.DataGroupTermCollector;
import se.uu.ub.cora.data.DataGroup;
import se.uu.ub.cora.data.DataList;
import se.uu.ub.cora.data.DataProvider;
import se.uu.ub.cora.data.DataRecordLink;
import se.uu.ub.cora.data.collected.CollectTerms;
import se.uu.ub.cora.data.collected.Link;
import se.uu.ub.cora.spider.authentication.Authenticator;
import se.uu.ub.cora.spider.authorization.SpiderAuthorizator;
import se.uu.ub.cora.spider.dependency.SpiderDependencyProvider;
import se.uu.ub.cora.spider.record.IncomingLinksReader;

public class IncomingLinksReaderImp extends RecordHandler implements IncomingLinksReader {
	private static final String READ = "read";
	private Authenticator authenticator;
	private SpiderAuthorizator spiderAuthorizator;
	private RecordTypeHandler recordTypeHandler;
	private DataGroupTermCollector collectTermCollector;

	public IncomingLinksReaderImp(SpiderDependencyProvider dependencyProvider) {
		this.dependencyProvider = dependencyProvider;
		this.authenticator = dependencyProvider.getAuthenticator();
		this.spiderAuthorizator = dependencyProvider.getSpiderAuthorizator();
		this.recordStorage = dependencyProvider.getRecordStorage();
		this.collectTermCollector = dependencyProvider.getDataGroupTermCollector();
	}

	public static IncomingLinksReader usingDependencyProvider(
			SpiderDependencyProvider dependencyProvider) {
		return new IncomingLinksReaderImp(dependencyProvider);
	}

	@Override
	public DataList readIncomingLinks(String authToken, String recordType, String recordId) {
		this.authToken = authToken;
		this.recordType = recordType;
		this.recordId = recordId;
		recordTypeHandler = dependencyProvider.getRecordTypeHandler(recordType);

		tryToGetActiveUser();
		DataGroup recordRead = recordStorage.read(List.of(recordType), recordId);
		checkUserIsAuthorisedToReadData(recordRead);

		return collectLinksAndConvertToDataList();
	}

	private void tryToGetActiveUser() {
		user = authenticator.getUserForToken(authToken);
	}

	private void checkUserIsAuthorisedToReadData(DataGroup recordRead) {
		CollectTerms collectTerms = getCollectedTermsForRecord(recordRead);
		spiderAuthorizator.checkUserIsAuthorizedForActionOnRecordTypeAndCollectedData(user, READ,
				recordType, collectTerms.permissionTerms);
	}

	private CollectTerms getCollectedTermsForRecord(DataGroup recordRead) {
		String metadataId = recordTypeHandler.getDefinitionId();
		return collectTermCollector.collectTerms(metadataId, recordRead);
	}

	private DataList collectLinksAndConvertToDataList() {
		Set<Link> links = new LinkedHashSet<>();
		addLinksPointingToRecord(recordType, links);
		possiblyAddLinksPointingToRecordByParentRecordType(links);

		return convertLinksPointingToRecordToDataList(links);
	}

	private void addLinksPointingToRecord(String recordType2, Set<Link> links) {
		Set<Link> linksPointingToRecord = recordStorage.getLinksToRecord(recordType2, recordId);
		links.addAll(linksPointingToRecord);
	}

	private void possiblyAddLinksPointingToRecordByParentRecordType(Set<Link> links) {
		if (recordTypeHandler.hasParent()) {
			String parentId = recordTypeHandler.getParentId();
			addLinksPointingToRecord(parentId, links);
		}
	}

	private DataList convertLinksPointingToRecordToDataList(Collection<Link> links) {
		DataList recordToRecordLinkList = createDataListForRecordToRecordLinks(links);
		convertAndAddLinksToLinkList(links, recordToRecordLinkList);
		return recordToRecordLinkList;
	}

	private DataList createDataListForRecordToRecordLinks(Collection<Link> links) {
		DataList recordToRecordList = DataProvider
				.createListWithNameOfDataType("recordToRecordLink");

		recordToRecordList.setFromNo("1");
		recordToRecordList.setToNo(Integer.toString(links.size()));
		recordToRecordList.setTotalNo(Integer.toString(links.size()));
		return recordToRecordList;
	}

	private void convertAndAddLinksToLinkList(Collection<Link> links, DataList recordToRecordList) {
		for (Link link : links) {
			DataGroup recordToRecordLink = convertLinkToDataGroup(link);
			recordToRecordList.addData(recordToRecordLink);
		}
	}

	private DataGroup convertLinkToDataGroup(Link link) {
		DataGroup recordToRecordLink = DataProvider
				.createGroupUsingNameInData("recordToRecordLink");
		DataRecordLink from = DataProvider.createRecordLinkUsingNameInDataAndTypeAndId("from",
				link.type(), link.id());
		recordToRecordLink.addChild(from);
		from.addAction(se.uu.ub.cora.data.Action.READ);

		DataRecordLink to = DataProvider.createRecordLinkUsingNameInDataAndTypeAndId("to",
				recordType, recordId);
		recordToRecordLink.addChild(to);
		return recordToRecordLink;
	}

}
