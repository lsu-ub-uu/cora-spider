/*
 * Copyright 2016, 2017, 2019, 2020 Uppsala University Library
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

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import se.uu.ub.cora.beefeater.authentication.User;
import se.uu.ub.cora.bookkeeper.metadata.Constraint;
import se.uu.ub.cora.bookkeeper.recordpart.DataRedactor;
import se.uu.ub.cora.bookkeeper.recordtype.RecordTypeHandler;
import se.uu.ub.cora.bookkeeper.termcollector.DataGroupTermCollector;
import se.uu.ub.cora.data.Action;
import se.uu.ub.cora.data.DataChild;
import se.uu.ub.cora.data.DataGroup;
import se.uu.ub.cora.data.DataLink;
import se.uu.ub.cora.data.DataRecord;
import se.uu.ub.cora.data.DataRecordLink;
import se.uu.ub.cora.data.DataRecordProvider;
import se.uu.ub.cora.data.DataResourceLink;
import se.uu.ub.cora.data.collected.CollectTerms;
import se.uu.ub.cora.spider.authorization.SpiderAuthorizator;
import se.uu.ub.cora.spider.dependency.SpiderDependencyProvider;
import se.uu.ub.cora.spider.record.DataGroupToRecordEnhancer;
import se.uu.ub.cora.storage.RecordNotFoundException;
import se.uu.ub.cora.storage.RecordStorage;

public class DataGroupToRecordEnhancerImp implements DataGroupToRecordEnhancer {

	private static final String LINKED_RECORD_ID = "linkedRecordId";
	private static final String SEARCH = "search";

	private SpiderDependencyProvider dependencyProvider;
	private SpiderAuthorizator spiderAuthorizator;
	private RecordStorage recordStorage;
	private DataGroupTermCollector termCollector;

	private User user;
	private String recordType;
	private String handledRecordId;

	private RecordTypeHandler recordTypeHandler;
	private CollectTerms collectedTerms;
	private Map<String, RecordTypeHandler> cachedRecordTypeHandlers = new HashMap<>();
	private Map<String, Boolean> cachedAuthorizedToReadRecordLink = new HashMap<>();
	private Set<String> readRecordPartPermissions;
	private Set<String> writeRecordPartPermissions;
	private boolean addActionRead = true;

	public DataGroupToRecordEnhancerImp(SpiderDependencyProvider dependencyProvider) {
		this.dependencyProvider = dependencyProvider;
		spiderAuthorizator = dependencyProvider.getSpiderAuthorizator();
		recordStorage = dependencyProvider.getRecordStorage();
		termCollector = dependencyProvider.getDataGroupTermCollector();
	}

	@Override
	public DataRecord enhance(User user, String recordType, DataGroup dataGroup,
			DataRedactor dataRedactor) {
		commonSetupForEnhance(user, recordType, dataGroup);
		return enhanceDataGroupToRecord(dataGroup, dataRedactor);
	}

	private void commonSetupForEnhance(User user, String recordType, DataGroup dataGroup) {
		this.user = user;
		this.recordType = recordType;
		recordTypeHandler = getRecordTypeHandlerForRecordType(recordType);
		collectedTerms = getCollectedTermsForRecordTypeAndRecord(recordType, dataGroup);
		handledRecordId = getRecordIdFromDataRecord(dataGroup);
	}

	private RecordTypeHandler getRecordTypeHandlerForRecordType(String recordType) {
		if (recordTypeHandlerForRecordTypeNotYetLoaded(recordType)) {
			loadRecordTypeHandlerForRecordType(recordType);
		}
		return cachedRecordTypeHandlers.get(recordType);
	}

	private boolean recordTypeHandlerForRecordTypeNotYetLoaded(String recordType) {
		return !cachedRecordTypeHandlers.containsKey(recordType);
	}

	private void loadRecordTypeHandlerForRecordType(String recordType) {
		RecordTypeHandler recordTypeHandlerToLoad = dependencyProvider
				.getRecordTypeHandler(recordType);
		cachedRecordTypeHandlers.put(recordType, recordTypeHandlerToLoad);
	}

	private CollectTerms getCollectedTermsForRecordTypeAndRecord(String recordType,
			DataGroup dataGroup) {
		RecordTypeHandler recordTypeHandlerForRecordType = getRecordTypeHandlerForRecordType(
				recordType);
		String definitionId = recordTypeHandlerForRecordType.getDefinitionId();
		return termCollector.collectTerms(definitionId, dataGroup);
	}

	private String getRecordIdFromDataRecord(DataGroup dataGroup) {
		DataGroup recordInfo = dataGroup.getFirstGroupWithNameInData("recordInfo");
		return recordInfo.getFirstAtomicValueWithNameInData("id");
	}

	private DataRecord enhanceDataGroupToRecord(DataGroup dataGroup, DataRedactor dataRedactor) {
		ensurePublicOrReadAccess();
		return enhanceDataGroupToRecordUsingReadRecordPartPermissions(dataGroup, dataRedactor);
	}

	private DataRecord enhanceDataGroupToRecordUsingReadRecordPartPermissions(DataGroup dataGroup,
			DataRedactor dataRedactor) {
		DataRecord dataRecord = createDataRecord(dataGroup);
		addActions(dataRecord);
		addRecordPartPermissions(dataRecord);
		DataGroup redactedDataGroup = redact(dataGroup, dataRedactor);
		dataRecord.setDataGroup(redactedDataGroup);
		addReadActionToAllRecordLinks(redactedDataGroup);
		return dataRecord;
	}

	private void ensurePublicOrReadAccess() {
		if (!recordTypeHandler.isPublicForRead()) {
			checkAndGetUserAuthorizationsForReadAction();
		} else {
			readRecordPartPermissions = Collections.emptySet();
		}
	}

	private void checkAndGetUserAuthorizationsForReadAction() {
		readRecordPartPermissions = spiderAuthorizator
				.checkGetUsersMatchedRecordPartPermissionsForActionOnRecordTypeAndCollectedData(
						user, "read", recordType, collectedTerms.permissionTerms, true);
	}

	private DataRecord createDataRecord(DataGroup dataGroup) {
		return DataRecordProvider.getDataRecordWithDataGroup(dataGroup);
	}

	private void addActions(DataRecord dataRecord) {
		possiblyAddReadAction(dataRecord);
		possiblyAddUpdateAction(dataRecord);
		possiblyAddIndexAction(dataRecord);
		boolean hasIncommingLinks = incomingLinksExistsForRecord();
		possiblyAddDeleteAction(dataRecord, hasIncommingLinks);
		possiblyAddIncomingLinksAction(dataRecord, hasIncommingLinks);
		possiblyAddUploadAction(dataRecord);
		possiblyAddSearchActionWhenDataRepresentsASearch(dataRecord);
		possiblyAddActionsWhenDataRepresentsARecordType(dataRecord);
	}

	private void possiblyAddReadAction(DataRecord dataRecord) {
		if (addActionRead) {
			dataRecord.addAction(Action.READ);
		}
	}

	private void possiblyAddUpdateAction(DataRecord dataRecord) {
		try {
			writeRecordPartPermissions = spiderAuthorizator
					.checkGetUsersMatchedRecordPartPermissionsForActionOnRecordTypeAndCollectedData(
							user, "update", recordType, collectedTerms.permissionTerms, true);

			dataRecord.addAction(Action.UPDATE);
		} catch (Exception catchedException) {
			writeRecordPartPermissions = Collections.emptySet();
		}
	}

	private void possiblyAddIndexAction(DataRecord dataRecord) {
		if (userIsAuthorizedForActionOnRecordTypeAndCollectedTerms("index", recordType)) {
			dataRecord.addAction(Action.INDEX);
		}
	}

	private boolean userIsAuthorizedForActionOnRecordTypeAndCollectedTerms(String action,
			String recordType) {
		return spiderAuthorizator.userIsAuthorizedForActionOnRecordTypeAndCollectedData(user,
				action, recordType, collectedTerms.permissionTerms);
	}

	private boolean incomingLinksExistsForRecord() {
		return linksExistForRecordTypeUsingCurrentHandledId(recordType)
				|| incomingLinksExistsForParentToRecordType();
	}

	private boolean linksExistForRecordTypeUsingCurrentHandledId(String recordTypeId) {
		return recordStorage.linksExistForRecord(recordTypeId, handledRecordId);
	}

	private boolean incomingLinksExistsForParentToRecordType() {
		if (recordTypeHandler.hasParent()) {
			String parentId = recordTypeHandler.getParentId();
			return linksExistForRecordTypeUsingCurrentHandledId(parentId);
		}
		return false;
	}

	private void possiblyAddDeleteAction(DataRecord dataRecord, boolean hasIncommingLinks) {
		if (userIsAuthorizedForActionOnRecordTypeAndCollectedTerms("delete", recordType)
				&& !hasIncommingLinks) {
			dataRecord.addAction(Action.DELETE);
		}
	}

	private void possiblyAddIncomingLinksAction(DataRecord dataRecord, boolean hasIncommingLinks) {
		if (hasIncommingLinks) {
			dataRecord.addAction(Action.READ_INCOMING_LINKS);
		}
	}

	private void possiblyAddUploadAction(DataRecord dataRecord) {
		// if (recordTypeHandler.isChildOfBinary()
		if ("binary".equals(recordType)
				&& userIsAuthorizedForActionOnRecordTypeAndCollectedTerms("upload", recordType)) {
			dataRecord.addAction(Action.UPLOAD);
		}
	}

	private void possiblyAddSearchActionWhenDataRepresentsASearch(DataRecord dataRecord) {
		if (theDataBeeingTurnedIntoARecordIsASearch()) {
			addSearchActionIfUserHasAccessToLinkedSearches(dataRecord, dataRecord.getDataGroup());
		}
	}

	private boolean theDataBeeingTurnedIntoARecordIsASearch() {
		return recordTypeHandler.representsTheRecordTypeDefiningSearches();
	}

	private void addSearchActionIfUserHasAccessToLinkedSearches(DataRecord dataRecord,
			DataGroup dataGroup) {
		List<DataGroup> recordTypeToSearchInGroups = dataGroup
				.getAllGroupsWithNameInData("recordTypeToSearchIn");
		if (checkUserHasSearchAccessOnAllRecordTypesToSearchIn(recordTypeToSearchInGroups)) {
			dataRecord.addAction(Action.SEARCH);
		}
	}

	private boolean checkUserHasSearchAccessOnAllRecordTypesToSearchIn(
			List<DataGroup> recordTypeToSearchInGroups) {
		return recordTypeToSearchInGroups.stream().allMatch(this::isAuthorized);
	}

	private boolean isAuthorized(DataGroup group) {
		String linkedRecordTypeId = group.getFirstAtomicValueWithNameInData(LINKED_RECORD_ID);
		return spiderAuthorizator.userIsAuthorizedForActionOnRecordType(user, SEARCH,
				linkedRecordTypeId);
	}

	private void possiblyAddActionsWhenDataRepresentsARecordType(DataRecord dataRecord) {
		if (theDataBeeingTurnedIntoARecordIsARecordType()) {
			RecordTypeHandler handledRecordTypeHandler = getRecordTypeHandlerForRecordType(
					handledRecordId);
			possiblyAddCreateAction(handledRecordTypeHandler, dataRecord);
			possiblyAddListAction(dataRecord);
			possiblyAddValidateAction(dataRecord);
			possiblyAddSearchAction(handledRecordTypeHandler, dataRecord);
			possiblyAddBatchIndexAction(dataRecord);
		}
	}

	private void possiblyAddBatchIndexAction(DataRecord dataRecord) {
		if (userIsAuthorizedForActionOnRecordType("batch_index", handledRecordId)) {
			dataRecord.addAction(Action.BATCH_INDEX);
		}
	}

	private boolean theDataBeeingTurnedIntoARecordIsARecordType() {
		return recordTypeHandler.representsTheRecordTypeDefiningRecordTypes();
	}

	private void possiblyAddCreateAction(RecordTypeHandler handledRecordTypeHandler,
			DataRecord dataRecord) {
		if (userIsAuthorizedForActionOnRecordType("create", handledRecordId)) {
			dataRecord.addAction(Action.CREATE);
		}
	}

	private boolean userIsAuthorizedForActionOnRecordType(String action, String handledRecordId) {
		return spiderAuthorizator.userIsAuthorizedForActionOnRecordType(user, action,
				handledRecordId);
	}

	private void possiblyAddListAction(DataRecord dataRecord) {
		if (userIsAuthorizedForActionOnRecordType("list", handledRecordId)) {
			dataRecord.addAction(Action.LIST);
		}
	}

	private void possiblyAddValidateAction(DataRecord dataRecord) {
		if (userIsAuthorizedForActionOnRecordType("validate", handledRecordId)) {
			dataRecord.addAction(Action.VALIDATE);
		}
	}

	private void possiblyAddSearchAction(RecordTypeHandler handledRecordTypeHandler,
			DataRecord dataRecord) {
		if (hasLinkedSearch(handledRecordTypeHandler)) {
			DataGroup searchGroup = getLinkedSearchForSearchForRecordHandledByRecordHandler(
					handledRecordTypeHandler);
			addSearchActionIfUserHasAccessToLinkedSearches(dataRecord, searchGroup);
		}
	}

	private boolean hasLinkedSearch(RecordTypeHandler handledRecordTypeHandler) {
		return handledRecordTypeHandler.hasLinkedSearch();
	}

	private DataGroup getLinkedSearchForSearchForRecordHandledByRecordHandler(
			RecordTypeHandler handledRecordTypeHandler) {
		String searchId = handledRecordTypeHandler.getSearchId();
		return readRecordFromStorageByTypeAndId(SEARCH, searchId);
	}

	private DataGroup readRecordFromStorageByTypeAndId(String linkedRecordType,
			String linkedRecordId) {
		RecordTypeHandler rth = getRecordTypeHandlerForRecordType(linkedRecordType);
		return recordStorage.read(rth.getListOfRecordTypeIdsToReadFromStorage(), linkedRecordId);
	}

	private void addRecordPartPermissions(DataRecord dataRecord) {
		dataRecord.addWritePermissions(writeRecordPartPermissions);
		dataRecord.addReadPermissions(readRecordPartPermissions);
		if (!recordTypeHandler.isPublicForRead()) {
			dataRecord.addReadPermissions(writeRecordPartPermissions);
		}
	}

	private DataGroup redact(DataGroup dataGroup, DataRedactor dataRedactor) {
		Set<Constraint> recordPartReadConstraints = recordTypeHandler
				.getReadRecordPartConstraints();
		return dataRedactor.removeChildrenForConstraintsWithoutPermissions(
				recordTypeHandler.getDefinitionId(), dataGroup, recordPartReadConstraints,
				readRecordPartPermissions);
	}

	private void addReadActionToAllRecordLinks(DataGroup dataGroup) {
		for (DataChild dataChild : dataGroup.getChildren()) {
			addReadActionToDataRecordLink(dataChild);
		}
	}

	private void addReadActionToDataRecordLink(DataChild dataChild) {
		possiblyAddReadActionIfLink(dataChild);

		if (isGroup(dataChild)) {
			addReadActionToAllRecordLinks((DataGroup) dataChild);
		}
	}

	private void possiblyAddReadActionIfLink(DataChild dataChild) {
		if (isLink(dataChild)) {
			possiblyAddReadAction(dataChild);
		}
	}

	private boolean isLink(DataChild dataChild) {
		return isRecordLink(dataChild) || isResourceLink(dataChild);
	}

	private boolean isRecordLink(DataChild dataChild) {
		return dataChild instanceof DataRecordLink;
	}

	private boolean isResourceLink(DataChild dataChild) {
		return dataChild instanceof DataResourceLink;
	}

	private void possiblyAddReadAction(DataChild dataChild) {
		if (isAuthorizedToReadLink(dataChild)) {
			((DataLink) dataChild).addAction(Action.READ);
		}
	}

	private boolean isAuthorizedToReadLink(DataChild dataChild) {
		if (isRecordLink(dataChild)) {
			return isAuthorizedToReadRecordLink((DataRecordLink) dataChild);
		}
		return isAuthorizedToReadResourceLink();
	}

	private boolean isAuthorizedToReadRecordLink(DataRecordLink dataChild) {
		String linkedRecordType = dataChild.getLinkedRecordType();
		String linkedRecordId = dataChild.getLinkedRecordId();

		if (isRecordLinksTypePublic(linkedRecordType)) {
			return true;
		}
		return isAuthorizedToReadNonPublicRecordLink(linkedRecordType, linkedRecordId);
	}

	private boolean isAuthorizedToReadNonPublicRecordLink(String linkedRecordType,
			String linkedRecordId) {
		String cacheId = linkedRecordType + linkedRecordId;
		if (existsCachedAuthorizationForRecordLink(cacheId)) {
			return cachedAuthorizedToReadRecordLink.get(cacheId);
		} else {
			boolean readAccess = readRecordLinkAuthorization(linkedRecordType, linkedRecordId);
			cachedAuthorizedToReadRecordLink.put(cacheId, readAccess);
			return readAccess;
		}
	}

	private boolean isRecordLinksTypePublic(String linkedRecordType) {
		RecordTypeHandler handledRecordTypeHandler = getRecordTypeHandlerForRecordType(
				linkedRecordType);
		return handledRecordTypeHandler.isPublicForRead();
	}

	private boolean existsCachedAuthorizationForRecordLink(String key) {
		return cachedAuthorizedToReadRecordLink.containsKey(key);
	}

	private boolean readRecordLinkAuthorization(String linkedRecordType, String linkedRecordId) {
		try {
			DataGroup linkedRecord = readRecordFromStorageByTypeAndId(linkedRecordType,
					linkedRecordId);
			return userIsAuthorizedForActionOnRecordLinkAndData("read", linkedRecordType,
					linkedRecord);
		} catch (RecordNotFoundException exception) {
			return false;
		}
	}

	private boolean userIsAuthorizedForActionOnRecordLinkAndData(String action, String recordType,
			DataGroup dataGroup) {
		CollectTerms linkedRecordCollectedTerms = getCollectedTermsForRecordTypeAndRecord(
				recordType, dataGroup);

		return spiderAuthorizator.userIsAuthorizedForActionOnRecordTypeAndCollectedData(user,
				action, recordType, linkedRecordCollectedTerms.permissionTerms);
	}

	private boolean isGroup(DataChild dataChild) {
		return dataChild instanceof DataGroup;
	}

	private boolean isAuthorizedToReadResourceLink() {
		return userIsAuthorizedForActionOnRecordTypeAndCollectedTerms("read", "binary");
	}

	@Override
	public DataRecord enhanceIgnoringReadAccess(User user, String recordType, DataGroup dataGroup,
			DataRedactor dataRedactor) {
		commonSetupForEnhance(user, recordType, dataGroup);
		return enhanceDataGroupToRecordIgnoringReadAccess(dataGroup, dataRedactor);
	}

	private DataRecord enhanceDataGroupToRecordIgnoringReadAccess(DataGroup dataGroup,
			DataRedactor dataRedactor) {
		setNoReadPermissionsIfUserHasNoReadAccess();
		return enhanceDataGroupToRecordUsingReadRecordPartPermissions(dataGroup, dataRedactor);
	}

	private void setNoReadPermissionsIfUserHasNoReadAccess() {
		try {
			ensurePublicOrReadAccess();
		} catch (Exception catchedException) {
			addActionRead = false;
			readRecordPartPermissions = Collections.emptySet();
		}
	}

	public SpiderDependencyProvider getDependencyProvider() {
		// needed for test
		return dependencyProvider;
	}
}
