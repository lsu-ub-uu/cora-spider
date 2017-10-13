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
package se.uu.ub.cora.spider.extended;

import se.uu.ub.cora.beefeater.authentication.User;
import se.uu.ub.cora.bookkeeper.data.DataGroup;
import se.uu.ub.cora.bookkeeper.termcollector.DataGroupTermCollector;
import se.uu.ub.cora.spider.authentication.Authenticator;
import se.uu.ub.cora.spider.authorization.SpiderAuthorizator;
import se.uu.ub.cora.spider.data.SpiderDataGroup;
import se.uu.ub.cora.spider.dependency.SpiderDependencyProvider;
import se.uu.ub.cora.spider.record.storage.RecordStorage;
import se.uu.ub.cora.spider.search.RecordIndexer;

public class WorkOrderExecutorAsExtendedFunctionality implements ExtendedFunctionality {

	private RecordIndexer recordIndexer;
	private DataGroupTermCollector collectTermCollector;
	private RecordStorage recordStorage;
	private SpiderAuthorizator spiderAuthorizator;
	private	Authenticator authenticator;

	public WorkOrderExecutorAsExtendedFunctionality(SpiderDependencyProvider dependencyProvider) {
		this.recordIndexer = dependencyProvider.getRecordIndexer();
		this.collectTermCollector = dependencyProvider.getDataGroupSearchTermCollector();
		this.recordStorage = dependencyProvider.getRecordStorage();
		this.spiderAuthorizator = dependencyProvider.getSpiderAuthorizator();
		this.authenticator = dependencyProvider.getAuthenticator();
	}

	public static WorkOrderExecutorAsExtendedFunctionality usingDependencyProvider(
			SpiderDependencyProvider dependencyProvider) {
		return new WorkOrderExecutorAsExtendedFunctionality(dependencyProvider);
	}

	@Override
	public void useExtendedFunctionality(String authToken, SpiderDataGroup spiderDataGroup) {
		DataGroup workOrder = spiderDataGroup.toDataGroup();
		String recordType = getRecordTypeToIndexFromWorkOrder(workOrder);
		if(userIsAuthorizedToIndex(authToken, recordType)) {
			indexData(workOrder, recordType);
		}
	}

	private void indexData(DataGroup workOrder, String recordType) {
		String metadataId = getMetadataIdFromRecordType(recordType);
		DataGroup dataToIndex = readRecordToIndexFromStorage(workOrder, recordType);
		sendToIndex(metadataId, dataToIndex);
	}

	private boolean userIsAuthorizedToIndex(String authToken, String recordType) {
		User user = authenticator.getUserForToken(authToken);
		return spiderAuthorizator.userIsAuthorizedForActionOnRecordType(user, "index", recordType);
	}

	private String getRecordTypeToIndexFromWorkOrder(DataGroup workOrder) {
		DataGroup recordTypeLink = workOrder.getFirstGroupWithNameInData("recordType");
		return recordTypeLink.getFirstAtomicValueWithNameInData("linkedRecordId");
	}

	private String getMetadataIdFromRecordType(String recordType) {
		DataGroup readRecordType = recordStorage.read("recordType", recordType);
		DataGroup metadataIdLink = readRecordType.getFirstGroupWithNameInData("metadataId");
		return metadataIdLink.getFirstAtomicValueWithNameInData("linkedRecordId");
	}

	private DataGroup readRecordToIndexFromStorage(DataGroup workOrder, String recordType) {
		return recordStorage.read(recordType,
				workOrder.getFirstAtomicValueWithNameInData("recordId"));
	}

	private void sendToIndex(String metadataId, DataGroup dataToIndex) {
		DataGroup collectedTerms = collectTermCollector.collectTerms(metadataId, dataToIndex);
		recordIndexer.indexData(collectedTerms, dataToIndex);
	}
}
