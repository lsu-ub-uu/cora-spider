/*
 * Copyright 2017, 2022, 2024 Uppsala University Library
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
package se.uu.ub.cora.spider.extended.workorder;

import java.util.List;

import se.uu.ub.cora.beefeater.authentication.User;
import se.uu.ub.cora.bookkeeper.recordtype.RecordTypeHandler;
import se.uu.ub.cora.bookkeeper.termcollector.DataGroupTermCollector;
import se.uu.ub.cora.data.DataRecordGroup;
import se.uu.ub.cora.data.DataRecordLink;
import se.uu.ub.cora.data.collected.CollectTerms;
import se.uu.ub.cora.data.collected.IndexTerm;
import se.uu.ub.cora.search.RecordIndexer;
import se.uu.ub.cora.spider.authentication.Authenticator;
import se.uu.ub.cora.spider.authorization.SpiderAuthorizator;
import se.uu.ub.cora.spider.dependency.SpiderDependencyProvider;
import se.uu.ub.cora.spider.extendedfunctionality.ExtendedFunctionality;
import se.uu.ub.cora.spider.extendedfunctionality.ExtendedFunctionalityData;
import se.uu.ub.cora.storage.RecordStorage;

public class WorkOrderExecutor implements ExtendedFunctionality {

	private RecordIndexer recordIndexer;
	private DataGroupTermCollector collectTermCollector;
	private RecordStorage recordStorage;
	private SpiderAuthorizator spiderAuthorizator;
	private Authenticator authenticator;
	private String recordTypeToIndex;
	private String recordIdToIndex;
	private SpiderDependencyProvider dependencyProvider;

	public WorkOrderExecutor(SpiderDependencyProvider dependencyProvider) {
		this.dependencyProvider = dependencyProvider;
		this.recordIndexer = dependencyProvider.getRecordIndexer();
		this.collectTermCollector = dependencyProvider.getDataGroupTermCollector();
		this.recordStorage = dependencyProvider.getRecordStorage();
		this.spiderAuthorizator = dependencyProvider.getSpiderAuthorizator();
		this.authenticator = dependencyProvider.getAuthenticator();
	}

	public static WorkOrderExecutor usingDependencyProvider(
			SpiderDependencyProvider dependencyProvider) {
		return new WorkOrderExecutor(dependencyProvider);
	}

	@Override
	public void useExtendedFunctionality(ExtendedFunctionalityData data) {
		String authToken = data.authToken;
		DataRecordGroup workOrder = data.dataRecordGroup;
		recordTypeToIndex = getRecordTypeToIndexFromWorkOrder(workOrder);
		recordIdToIndex = getRecordIdToIndexFromWorkOrder(workOrder);

		boolean performExplicitCommit = getPerformExplicitCommit(workOrder);
		if (workOrderTypeIsDeleteFromIndex(workOrder)) {
			deleteFromIndexIfUserIsAuthorized(authToken);
		} else {
			indexDataIfUserIsAuthorized(authToken, performExplicitCommit);
		}
	}

	private boolean getPerformExplicitCommit(DataRecordGroup workOrder) {
		boolean performCommit = true;
		if (workOrder.containsChildWithNameInData("performCommit")
				&& performExplicitCommitIsFalse(workOrder)) {
			performCommit = false;
		}
		return performCommit;
	}

	private boolean performExplicitCommitIsFalse(DataRecordGroup workOrder) {
		return "false".equals(workOrder.getFirstAtomicValueWithNameInData("performCommit"));
	}

	private boolean workOrderTypeIsDeleteFromIndex(DataRecordGroup workOrder) {
		String workOrderType = workOrder.getFirstAtomicValueWithNameInData("type");
		return "removeFromIndex".equals(workOrderType);
	}

	private void deleteFromIndexIfUserIsAuthorized(String authToken) {
		if (userIsAuthorizedToIndex(authToken)) {
			recordIndexer.deleteFromIndex(recordTypeToIndex, recordIdToIndex);
		}
	}

	private String getRecordTypeToIndexFromWorkOrder(DataRecordGroup workOrder) {
		DataRecordLink recordTypeLink = workOrder.getFirstChildOfTypeAndName(DataRecordLink.class,
				"recordType");
		return recordTypeLink.getLinkedRecordId();
	}

	private String getRecordIdToIndexFromWorkOrder(DataRecordGroup workOrder) {
		return workOrder.getFirstAtomicValueWithNameInData("recordId");
	}

	private void indexDataIfUserIsAuthorized(String authToken, boolean performCommit) {
		if (userIsAuthorizedToIndex(authToken)) {
			indexData(performCommit);
		}
	}

	private boolean userIsAuthorizedToIndex(String authToken) {
		User user = authenticator.getUserForToken(authToken);
		return spiderAuthorizator.userIsAuthorizedForActionOnRecordType(user, "index",
				recordTypeToIndex);
	}

	private void indexData(boolean performCommit) {
		DataRecordGroup dataToIndex = readRecordToIndexFromStorage();
		CollectTerms collectedTerms = getCollectedTerms(dataToIndex);
		sendToIndex(collectedTerms, dataToIndex, performCommit);
	}

	private DataRecordGroup readRecordToIndexFromStorage() {
		return recordStorage.read(recordTypeToIndex, recordIdToIndex);
	}

	private CollectTerms getCollectedTerms(DataRecordGroup dataToIndex) {
		RecordTypeHandler recordTypeHandler = dependencyProvider
				.getRecordTypeHandlerUsingDataRecordGroup(dataToIndex);
		String metadataId = recordTypeHandler.getDefinitionId();
		return collectTermCollector.collectTerms(metadataId, dataToIndex);
	}

	private void sendToIndex(CollectTerms collectedTerms, DataRecordGroup dataToIndex,
			boolean performCommit) {
		List<IndexTerm> indexTerms = collectedTerms.indexTerms;
		if (performCommit) {
			recordIndexer.indexData(recordTypeToIndex, recordIdToIndex, indexTerms, dataToIndex);
		} else {
			recordIndexer.indexDataWithoutExplicitCommit(recordTypeToIndex, recordIdToIndex,
					indexTerms, dataToIndex);
		}
	}

	public SpiderDependencyProvider onlyForTestGetDependencyProvider() {
		return dependencyProvider;
	}
}
