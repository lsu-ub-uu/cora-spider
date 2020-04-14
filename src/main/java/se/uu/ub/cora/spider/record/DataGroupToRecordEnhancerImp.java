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

package se.uu.ub.cora.spider.record;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import se.uu.ub.cora.beefeater.authentication.User;
import se.uu.ub.cora.bookkeeper.termcollector.DataGroupTermCollector;
import se.uu.ub.cora.data.Action;
import se.uu.ub.cora.data.DataElement;
import se.uu.ub.cora.data.DataGroup;
import se.uu.ub.cora.data.DataLink;
import se.uu.ub.cora.data.DataRecord;
import se.uu.ub.cora.data.DataRecordLink;
import se.uu.ub.cora.data.DataRecordProvider;
import se.uu.ub.cora.data.DataResourceLink;
import se.uu.ub.cora.spider.authorization.SpiderAuthorizator;
import se.uu.ub.cora.spider.dependency.SpiderDependencyProvider;
import se.uu.ub.cora.storage.RecordNotFoundException;
import se.uu.ub.cora.storage.RecordStorage;

public class DataGroupToRecordEnhancerImp implements DataGroupToRecordEnhancer {

	private static final String RECORD_TYPE = "recordType";
	private static final String PARENT_ID = "parentId";
	private static final String LINKED_RECORD_ID = "linkedRecordId";
	private static final String SEARCH = "search";
	private DataGroup dataGroup;
	private DataRecord dataRecord;
	private User user;
	private String recordType;
	private String handledRecordId;
	private SpiderAuthorizator spiderAuthorizator;
	private RecordStorage recordStorage;
	private DataGroupTermCollector termCollector;
	private DataGroup collectedTerms;
	private Map<String, RecordTypeHandler> cachedRecordTypeHandlers = new HashMap<>();
	private Map<String, Boolean> cachedAuthorizedToReadRecordLink = new HashMap<>();
	private SpiderDependencyProvider dependencyProvider;
	private RecordTypeHandler recordTypeHandler;

	public DataGroupToRecordEnhancerImp(SpiderDependencyProvider dependencyProvider) {
		this.dependencyProvider = dependencyProvider;
		spiderAuthorizator = dependencyProvider.getSpiderAuthorizator();
		recordStorage = dependencyProvider.getRecordStorage();
		termCollector = dependencyProvider.getDataGroupTermCollector();
	}

	@Override
	public DataRecord enhance(User user, String recordType, DataGroup dataGroup) {
		this.user = user;
		this.recordType = recordType;
		this.dataGroup = dataGroup;
		recordTypeHandler = getRecordTypeHandlerForRecordType(recordType);
		collectedTerms = getCollectedTermsForRecord(recordType, dataGroup);
		dataRecord = DataRecordProvider.getDataRecordWithDataGroup(dataGroup);
		handledRecordId = getRecordIdFromDataRecord(dataRecord);
		addActions();
		addReadActionToAllRecordLinks(dataGroup);
		return dataRecord;
	}

	private DataGroup getCollectedTermsForRecord(String recordType, DataGroup dataGroup) {

		String metadataId = getMetadataIdFromRecordType(recordType);
		return termCollector.collectTerms(metadataId, dataGroup);
	}

	private String getMetadataIdFromRecordType(String recordType) {
		return recordTypeHandler.getMetadataId();
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
		RecordTypeHandler recordTypeHandler = dependencyProvider.getRecordTypeHandler(recordType);
		cachedRecordTypeHandlers.put(recordType, recordTypeHandler);
	}

	private String getRecordIdFromDataRecord(DataRecord dataRecord) {
		DataGroup topLevelDataGroup = dataRecord.getDataGroup();
		DataGroup recordInfo = topLevelDataGroup.getFirstGroupWithNameInData("recordInfo");
		return recordInfo.getFirstAtomicValueWithNameInData("id");
	}

	protected void addActions() {
		// if (userIsAuthorizedForActionOnRecordTypeAndCollectedTerms("read", recordType)) {
		// dataRecord.addAction(Action.READ);
		// }
		try {
			Set<String> usersReadRecordPartPermissions = spiderAuthorizator
					.checkAndGetUserAuthorizationsForActionOnRecordTypeAndCollectedData(user,
							"read", recordType, collectedTerms, true);
			// recordRead = redactDataGroup(recordRead, usersReadRecordPartPermissions);
			dataRecord.addAction(Action.READ);

		} catch (Exception e) {
			/**
			 * TODO: should we throw this error? That would prevent us from the reset of processing
			 * here, for a record we will not return to the user anyway....
			 */
		}
		// if (userIsAuthorizedForActionOnRecordTypeAndCollectedTerms("update", recordType)) {
		// dataRecord.addAction(Action.UPDATE);
		// }
		try {
			/**
			 * TODO: we need a boolean version of this method....
			 */
			Set<String> usersWriteRecordPartPermissions = spiderAuthorizator
					.checkAndGetUserAuthorizationsForActionOnRecordTypeAndCollectedData(user,
							"update", recordType, collectedTerms, true);
			// recordRead = redactDataGroup(recordRead, usersReadRecordPartPermissions);
			dataRecord.addAction(Action.UPDATE);

		} catch (Exception e) {

		}

		if (userIsAuthorizedForActionOnRecordTypeAndCollectedTerms("index", recordType)) {
			dataRecord.addAction(Action.INDEX);
		}
		boolean hasIncommingLinks = incomingLinksExistsForRecord(dataRecord);
		possiblyAddDeleteAction(dataRecord, hasIncommingLinks);
		possiblyAddIncomingLinksAction(dataRecord, hasIncommingLinks);
		possiblyAddUploadAction(dataRecord);
		possiblyAddSearchActionWhenDataRepresentsASearch(dataRecord);
		possiblyAddActionsWhenDataRepresentsARecordType(dataRecord);
	}

	private boolean userIsAuthorizedForActionOnRecordTypeAndCollectedTerms(String action,
			String recordType) {
		return spiderAuthorizator.userIsAuthorizedForActionOnRecordTypeAndCollectedData(user,
				action, recordType, collectedTerms);
	}

	private void possiblyAddDeleteAction(DataRecord dataRecord, boolean hasIncommingLinks) {
		if (userIsAuthorizedForActionOnRecordTypeAndCollectedTerms("delete", recordType)
				&& !hasIncommingLinks) {
			dataRecord.addAction(Action.DELETE);
		}
	}

	private boolean incomingLinksExistsForRecord(DataRecord dataRecord) {
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

	private DataGroup readRecordFromStorageByTypeAndId(String linkedRecordType,
			String linkedRecordId) {
		return recordStorage.read(linkedRecordType, linkedRecordId);
	}

	private void possiblyAddIncomingLinksAction(DataRecord dataRecord, boolean hasIncommingLinks) {
		if (hasIncommingLinks) {
			dataRecord.addAction(Action.READ_INCOMING_LINKS);
		}
	}

	private void possiblyAddUploadAction(DataRecord dataRecord) {
		if (recordTypeHandler.isChildOfBinary()
				&& userIsAuthorizedForActionOnRecordTypeAndCollectedTerms("upload", recordType)) {
			dataRecord.addAction(Action.UPLOAD);
		}
	}

	// private boolean isHandledRecordIdChildOfBinary(String dataRecordRecordId) {
	// DataGroup handledRecordTypeDataGroup = readRecordFromStorageByTypeAndId(RECORD_TYPE,
	// dataRecordRecordId);
	// return isHandledRecordTypeChildOfBinary(handledRecordTypeDataGroup);
	// }
	//
	// private boolean isHandledRecordTypeChildOfBinary(DataGroup handledRecordTypeDataGroup) {
	// if (handledRecordHasParent(handledRecordTypeDataGroup)) {
	// return recordTypeWithParentIsChildOfBinary(handledRecordTypeDataGroup);
	// }
	// return false;
	// }

	// private boolean recordTypeWithParentIsChildOfBinary(DataGroup handledRecordTypeDataGroup) {
	// String refParentId = extractParentId(handledRecordTypeDataGroup);
	// return "binary".equals(refParentId);
	// }

	private void possiblyAddSearchActionWhenDataRepresentsASearch(DataRecord dataRecord) {
		// if (recordTypeOfTheDataGroupBeeingTurnedIntoARecordIsSearch()) {
		if (theDataBeeingTurnedIntoARecordIsASearch()) {
			List<DataGroup> recordTypeToSearchInGroups = dataGroup
					.getAllGroupsWithNameInData("recordTypeToSearchIn");
			addSearchActionIfUserHasAccess(dataRecord, recordTypeToSearchInGroups);
		}
	}

	private boolean theDataBeeingTurnedIntoARecordIsASearch() {
		return recordTypeHandler.representsTheRecordTypeDefiningSearches();
	}

	private void addSearchActionIfUserHasAccess(DataRecord dataRecord,
			List<DataGroup> recordTypeToSearchInGroups) {
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
		// if (recordTypeOfTheDataGroupBeeingTurnedIntoARecordIsRecordType()) {
		if (theDataBeeingTurnedIntoARecordIsARecordType()) {
			RecordTypeHandler handledRecordTypeHandler = getRecordTypeHandlerForRecordType(
					handledRecordId);
			// TODO: special recordTypeHandler for these..
			// dependencyProvider.getRecordTypeHandler(recordTypeId)
			possiblyAddCreateAction(handledRecordTypeHandler, dataRecord);
			possiblyAddListAction(dataRecord);
			possiblyAddValidateAction();
			possiblyAddSearchAction(handledRecordTypeHandler, dataRecord);
		}
	}

	private boolean theDataBeeingTurnedIntoARecordIsARecordType() {
		return recordTypeHandler.representsTheRecordTypeDefiningRecordTypes();
	}

	private void possiblyAddCreateAction(RecordTypeHandler handledRecordTypeHandler,
			DataRecord dataRecord) {
		if (!handledRecordTypeHandler.isAbstract()
				&& userIsAuthorizedForActionOnRecordType("create", handledRecordId)) {
			dataRecord.addAction(Action.CREATE);
		}
	}

	// private boolean isHandledRecordIdOfTypeAbstract(String recordId) {
	// String abstractInRecordTypeDefinition = getAbstractFromHandledRecord(recordId);
	// return "true".equals(abstractInRecordTypeDefinition);
	// }

	// private String getAbstractFromHandledRecord(String recordId) {
	// DataGroup handleRecordTypeDataGroup = readRecordFromStorageByTypeAndId(RECORD_TYPE,
	// recordId);
	// return handleRecordTypeDataGroup.getFirstAtomicValueWithNameInData("abstract");
	// }

	private boolean userIsAuthorizedForActionOnRecordType(String action, String handledRecordId) {
		return spiderAuthorizator.userIsAuthorizedForActionOnRecordType(user, action,
				handledRecordId);
	}

	private void possiblyAddListAction(DataRecord dataRecord) {
		if (userIsAuthorizedForActionOnRecordType("list", handledRecordId)) {
			dataRecord.addAction(Action.LIST);
		}
	}

	private void possiblyAddValidateAction() {
		if (userIsAuthorizedForActionOnRecordType("validate", handledRecordId)) {
			dataRecord.addAction(Action.VALIDATE);
		}
	}

	private void possiblyAddSearchAction(RecordTypeHandler handledRecordTypeHandler,
			DataRecord dataRecord) {
		if (hasLinkedSearch(handledRecordTypeHandler)) {
			List<DataGroup> recordTypesToSearchIn = getRecordTypesToSearchInFromLinkedSearch();
			addSearchActionIfUserHasAccess(dataRecord, recordTypesToSearchIn);
		}
	}

	private boolean hasLinkedSearch(RecordTypeHandler handledRecordTypeHandler) {
		// TODO: add into recordTypeHandler
		// return dataGroup.containsChildWithNameInData(SEARCH);
		return handledRecordTypeHandler.hasLinkedSearch();
	}

	private List<DataGroup> getRecordTypesToSearchInFromLinkedSearch() {
		// TODO: måste kolla contains här innan
		DataGroup searchChildInRecordType = dataGroup.getFirstGroupWithNameInData(SEARCH);
		String searchId = searchChildInRecordType
				.getFirstAtomicValueWithNameInData(LINKED_RECORD_ID);
		// TODO: searchId = recordTypeHandler.getSearchId()
		DataGroup searchGroup = readRecordFromStorageByTypeAndId(SEARCH, searchId);
		// här borde vara samma som ovan
		return searchGroup.getAllGroupsWithNameInData("recordTypeToSearchIn");
	}

	private void addReadActionToAllRecordLinks(DataGroup dataGroup) {
		for (DataElement dataChild : dataGroup.getChildren()) {
			addReadActionToDataRecordLink(dataChild);
		}
	}

	private void addReadActionToDataRecordLink(DataElement dataChild) {
		possiblyAddReadActionIfLink(dataChild);

		if (isGroup(dataChild)) {
			addReadActionToAllRecordLinks((DataGroup) dataChild);
		}
	}

	private void possiblyAddReadActionIfLink(DataElement dataChild) {
		if (isLink(dataChild)) {
			possiblyAddReadAction(dataChild);
		}
	}

	private boolean isLink(DataElement dataChild) {
		return isRecordLink(dataChild) || isResourceLink(dataChild);
	}

	private boolean isRecordLink(DataElement dataChild) {
		return dataChild instanceof DataRecordLink;
	}

	private boolean isResourceLink(DataElement dataChild) {
		return dataChild instanceof DataResourceLink;
	}

	private void possiblyAddReadAction(DataElement dataChild) {
		if (isAuthorizedToReadLink(dataChild)) {
			((DataLink) dataChild).addAction(Action.READ);
		}
	}

	private boolean isAuthorizedToReadLink(DataElement dataChild) {
		if (isRecordLink(dataChild)) {
			return isAuthorizedToReadRecordLink((DataLink) dataChild);
		}
		return isAuthorizedToReadResourceLink();
	}

	private boolean isAuthorizedToReadRecordLink(DataLink dataChild) {
		String linkedRecordType = dataChild.getFirstAtomicValueWithNameInData("linkedRecordType");
		String linkedRecordId = dataChild.getFirstAtomicValueWithNameInData(LINKED_RECORD_ID);

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
		RecordTypeHandler recordTypeHandler = getRecordTypeHandlerForRecordType(linkedRecordType);
		return recordTypeHandler.isPublicForRead();
	}

	private boolean existsCachedAuthorizationForRecordLink(String key) {
		return cachedAuthorizedToReadRecordLink.containsKey(key);
	}

	private boolean readRecordLinkAuthorization(String linkedRecordType, String linkedRecordId) {
		DataGroup linkedRecord = null;
		try {
			linkedRecord = readRecordFromStorageByTypeAndId(linkedRecordType, linkedRecordId);
		} catch (RecordNotFoundException exception) {
			return false;
		}
		return userIsAuthorizedForActionOnRecordLinkAndData("read", linkedRecordType, linkedRecord);
	}

	private boolean userIsAuthorizedForActionOnRecordLinkAndData(String action, String recordType,
			DataGroup dataGroup) {
		DataGroup linkedRecordCollectedTerms = getCollectedTermsForRecord(recordType, dataGroup);

		return spiderAuthorizator.userIsAuthorizedForActionOnRecordTypeAndCollectedData(user,
				action, recordType, linkedRecordCollectedTerms);
	}

	private boolean isGroup(DataElement dataChild) {
		return dataChild instanceof DataGroup;
	}

	private boolean isAuthorizedToReadResourceLink() {
		return userIsAuthorizedForActionOnRecordTypeAndCollectedTerms("read", "image");
	}

}
