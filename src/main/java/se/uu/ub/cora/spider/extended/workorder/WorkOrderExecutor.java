/*
 * Copyright 2017, 2022, 2024, 2026 Uppsala University Library
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
import se.uu.ub.cora.data.DataAtomic;
import se.uu.ub.cora.data.DataGroup;
import se.uu.ub.cora.data.DataProvider;
import se.uu.ub.cora.data.DataRecordGroup;
import se.uu.ub.cora.data.DataRecordLink;
import se.uu.ub.cora.data.collected.CollectTerms;
import se.uu.ub.cora.data.collected.IndexTerm;
import se.uu.ub.cora.logger.Logger;
import se.uu.ub.cora.logger.LoggerProvider;
import se.uu.ub.cora.search.RecordIndexer;
import se.uu.ub.cora.spider.authentication.Authenticator;
import se.uu.ub.cora.spider.authorization.SpiderAuthorizator;
import se.uu.ub.cora.spider.dependency.SpiderDependencyProvider;
import se.uu.ub.cora.spider.dependency.SpiderInstanceProvider;
import se.uu.ub.cora.spider.extendedfunctionality.ExtendedFunctionality;
import se.uu.ub.cora.spider.extendedfunctionality.ExtendedFunctionalityData;
import se.uu.ub.cora.spider.record.RecordUpdater;
import se.uu.ub.cora.spider.resourceconvert.ResourceConvert;
import se.uu.ub.cora.storage.RecordStorage;

public class WorkOrderExecutor implements ExtendedFunctionality {

	private RecordIndexer recordIndexer;
	private DataGroupTermCollector collectTermCollector;
	private RecordStorage recordStorage;
	private SpiderAuthorizator spiderAuthorizator;
	private Authenticator authenticator;
	private String recordType;
	private String recordId;
	private SpiderDependencyProvider dependencyProvider;
	private String userId;
	private Logger log;

	public WorkOrderExecutor(SpiderDependencyProvider dependencyProvider) {
		this.dependencyProvider = dependencyProvider;
		this.recordIndexer = dependencyProvider.getRecordIndexer();
		this.collectTermCollector = dependencyProvider.getDataGroupTermCollector();
		this.recordStorage = dependencyProvider.getRecordStorage();
		this.spiderAuthorizator = dependencyProvider.getSpiderAuthorizator();
		this.authenticator = dependencyProvider.getAuthenticator();
		log = LoggerProvider.getLoggerForClass(WorkOrderExecutor.class);
	}

	public static WorkOrderExecutor usingDependencyProvider(
			SpiderDependencyProvider dependencyProvider) {
		return new WorkOrderExecutor(dependencyProvider);
	}

	@Override
	public void useExtendedFunctionality(ExtendedFunctionalityData data) {
		String authToken = data.authToken;
		if (userIsAuthorizedToIndex(authToken)) {
			useExtendedForAuthorized(data);
		}
	}

	private boolean userIsAuthorizedToIndex(String authToken) {
		User user = authenticator.getUserForToken(authToken);
		userId = user.id;
		return spiderAuthorizator.userIsAuthorizedForActionOnRecordType(user, "index", recordType);
	}

	private void useExtendedForAuthorized(ExtendedFunctionalityData data) {
		DataRecordGroup workOrder = data.dataRecordGroup;
		recordType = getRecordTypeToIndexFromWorkOrder(workOrder);
		recordId = getRecordIdToIndexFromWorkOrder(workOrder);

		if (workOrderTypeIsDeleteFromIndex(workOrder)) {
			recordIndexer.deleteFromIndex(recordType, recordId);
		} else {
			regenerateMetadataAndPossiblyConvert(workOrder);
		}
	}

	private void regenerateMetadataAndPossiblyConvert(DataRecordGroup workOrder) {
		DataRecordGroup dataToIndex = readRecordToIndexFromStorage();
		if (standardRecord(dataToIndex)) {
			boolean performExplicitCommit = getPerformExplicitCommit(workOrder);
			reIndex(dataToIndex, performExplicitCommit);
		} else {
			tryToSendToConvert(dataToIndex);
		}
	}

	private DataRecordGroup readRecordToIndexFromStorage() {
		return recordStorage.read(recordType, recordId);
	}

	private boolean standardRecord(DataRecordGroup dataToIndex) {
		return !("binary".equals(recordType) && mimetypeToReconvert(dataToIndex));
	}

	private boolean mimetypeToReconvert(DataRecordGroup dataToIndex) {
		String mimeType = getMimeType(dataToIndex);
		return isImage(mimeType) || isPdf(mimeType);
	}

	private boolean isImage(String detectedMimeType) {
		return detectedMimeType.startsWith("image/");
	}

	private boolean isPdf(String detectedMimeType) {
		return "application/pdf".equals(detectedMimeType);
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

	private String getRecordTypeToIndexFromWorkOrder(DataRecordGroup workOrder) {
		DataRecordLink recordTypeLink = workOrder.getFirstChildOfTypeAndName(DataRecordLink.class,
				"recordType");
		return recordTypeLink.getLinkedRecordId();
	}

	private String getRecordIdToIndexFromWorkOrder(DataRecordGroup workOrder) {
		return workOrder.getFirstAtomicValueWithNameInData("recordId");
	}

	private String getMimeType(DataRecordGroup dataToIndex) {
		DataGroup masterGroup = dataToIndex.getFirstGroupWithNameInData("master");
		return masterGroup.getFirstAtomicValueWithNameInData("mimeType");
	}

	private void tryToSendToConvert(DataRecordGroup dataToIndex) {
		try {
			String mimeType = getMimeType(dataToIndex);
			String dataDivider = dataToIndex.getDataDivider();
			ResourceConvert resourceConvert = dependencyProvider.getResourceConvert();
			setStatus(dataToIndex, "reconvert");
			updateRecord(dataToIndex);
			sendToConverter(mimeType, dataDivider, resourceConvert);
		} catch (Exception e) {
			logException(e);
		}
	}

	private void logException(Exception e) {
		log.logErrorUsingMessageAndException(
				"Work order failed to carry out requested operation on " + "recordType: "
						+ recordType + ", recordId: " + recordId,
				e);
	}

	private void sendToConverter(String mimeType, String dataDivider,
			ResourceConvert resourceConvert) {
		if (isImage(mimeType)) {
			resourceConvert.sendMessageForAnalyzingAndConvertingImages(dataDivider, recordType,
					recordId, mimeType);
		}
		if (isPdf(mimeType)) {
			resourceConvert.sendMessageToConvertPdfToThumbnails(dataDivider, recordType, recordId);
		}
	}

	private void setStatus(DataRecordGroup dataRecordGroup, String status) {
		DataGroup recordInfo = dataRecordGroup.getFirstGroupWithNameInData("recordInfo");
		recordInfo.removeFirstChildWithTypeAndName(DataAtomic.class, "status");
		DataAtomic statusUploaded = DataProvider.createAtomicUsingNameInDataAndValue("status",
				status);
		recordInfo.addChild(statusUploaded);
	}

	private void updateRecord(DataRecordGroup dataRecordGroup) {
		RecordUpdater recordUpdater = SpiderInstanceProvider.getRecordUpdater();
		recordUpdater.internalUpdateRecord(dataRecordGroup, userId);
	}

	private void reIndex(DataRecordGroup dataToIndex, boolean performCommit) {
		CollectTerms collectedTerms = getCollectedTerms(dataToIndex);
		sendToIndex(collectedTerms, dataToIndex, performCommit);
		//TODO: 
		updateCollectedData
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
			recordIndexer.indexData(recordType, recordId, indexTerms, dataToIndex);
		} else {
			recordIndexer.indexDataWithoutExplicitCommit(recordType, recordId, indexTerms,
					dataToIndex);
		}
	}

	public SpiderDependencyProvider onlyForTestGetDependencyProvider() {
		return dependencyProvider;
	}
}
