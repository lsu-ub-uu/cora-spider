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

import static se.uu.ub.cora.spider.extendedfunctionality.ExtendedFunctionalityPosition.INCOMING_LINKS_AFTER_AUTHORIZATION;

import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import se.uu.ub.cora.bookkeeper.recordtype.RecordTypeHandler;
import se.uu.ub.cora.bookkeeper.termcollector.DataGroupTermCollector;
import se.uu.ub.cora.data.DataGroup;
import se.uu.ub.cora.data.DataList;
import se.uu.ub.cora.data.DataProvider;
import se.uu.ub.cora.data.DataRecordGroup;
import se.uu.ub.cora.data.DataRecordLink;
import se.uu.ub.cora.data.collected.CollectTerms;
import se.uu.ub.cora.data.collected.Link;
import se.uu.ub.cora.spider.authentication.Authenticator;
import se.uu.ub.cora.spider.authorization.SpiderAuthorizator;
import se.uu.ub.cora.spider.dependency.SpiderDependencyProvider;
import se.uu.ub.cora.spider.extendedfunctionality.ExtendedFunctionality;
import se.uu.ub.cora.spider.extendedfunctionality.ExtendedFunctionalityData;
import se.uu.ub.cora.spider.extendedfunctionality.ExtendedFunctionalityPosition;
import se.uu.ub.cora.spider.extendedfunctionality.ExtendedFunctionalityProvider;
import se.uu.ub.cora.spider.record.IncomingLinksReader;

public class IncomingLinksReaderImp extends RecordHandler implements IncomingLinksReader {
	private static final String READ = "read";
	private Authenticator authenticator;
	private SpiderAuthorizator authorizator;
	private DataGroupTermCollector termCollector;
	private ExtendedFunctionalityProvider extendedFunctionalityProvider;

	public IncomingLinksReaderImp(SpiderDependencyProvider dependencyProvider) {
		this.dependencyProvider = dependencyProvider;
		authenticator = dependencyProvider.getAuthenticator();
		authorizator = dependencyProvider.getSpiderAuthorizator();
		recordStorage = dependencyProvider.getRecordStorage();
		termCollector = dependencyProvider.getDataGroupTermCollector();
		extendedFunctionalityProvider = dependencyProvider.getExtendedFunctionalityProvider();
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

		getActiveUserOrGuest();
		checkUserIsAuthorizedForActionOnRecordType(recordType);
		useExtendedFunctionalityUsingPosition(INCOMING_LINKS_AFTER_AUTHORIZATION);

		DataRecordGroup dataRecordGroup = recordStorage.read(recordType, recordId);
		checkUserIsAuthorisedToReadData(dataRecordGroup);

		return collectLinksAndConvertToDataList();
	}

	private void checkUserIsAuthorizedForActionOnRecordType(String recordType) {
		authorizator.checkUserIsAuthorizedForActionOnRecordType(user, READ, recordType);
	}

	private void getActiveUserOrGuest() {
		user = authenticator.getUserForToken(authToken);
	}

	private void useExtendedFunctionalityUsingPosition(ExtendedFunctionalityPosition position) {
		List<ExtendedFunctionality> extendedFunctionality = extendedFunctionalityProvider
				.getFunctionalityForPositionAndRecordType(position, recordType);
		useExtendedFunctionality(extendedFunctionality);
	}

	protected void useExtendedFunctionality(List<ExtendedFunctionality> functionalityList) {
		for (ExtendedFunctionality extendedFunctionality : functionalityList) {
			ExtendedFunctionalityData data = createExtendedFunctionalityData();
			extendedFunctionality.useExtendedFunctionality(data);
		}
	}

	protected ExtendedFunctionalityData createExtendedFunctionalityData() {
		ExtendedFunctionalityData data = new ExtendedFunctionalityData();
		data.recordType = recordType;
		data.recordId = recordId;
		data.authToken = authToken;
		data.user = user;
		return data;
	}

	private void checkUserIsAuthorisedToReadData(DataRecordGroup dataRecordGroup) {
		CollectTerms collectTerms = getCollectedTermsForRecord(dataRecordGroup);
		authorizator.checkUserIsAuthorizedForActionOnRecordTypeAndCollectedData(user, READ,
				recordType, collectTerms.permissionTerms);
	}

	private CollectTerms getCollectedTermsForRecord(DataRecordGroup dataRecordGroup) {
		RecordTypeHandler recordTypeHandler = dependencyProvider
				.getRecordTypeHandlerUsingDataRecordGroup(dataRecordGroup);
		String metadataId = recordTypeHandler.getDefinitionId();
		return termCollector.collectTerms(metadataId, dataRecordGroup);
	}

	private DataList collectLinksAndConvertToDataList() {
		Set<Link> links = new LinkedHashSet<>();
		addLinksPointingToRecord(recordType, links);

		return convertLinksPointingToRecordToDataList(links);
	}

	private void addLinksPointingToRecord(String recordType2, Set<Link> links) {
		Set<Link> linksPointingToRecord = recordStorage.getLinksToRecord(recordType2, recordId);
		links.addAll(linksPointingToRecord);
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
