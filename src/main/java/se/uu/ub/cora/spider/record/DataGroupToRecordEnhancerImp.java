/*
 * Copyright 2016, 2017, 2019 Uppsala University Library
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
	private DataRecord record;
	private User user;
	private String recordType;
	private String handledRecordId;
	private SpiderAuthorizator spiderAuthorizator;
	private RecordStorage recordStorage;
	private DataGroupTermCollector collectTermCollector;
	private DataGroup collectedTerms;
	private Map<String, RecordTypeHandlerImp> cachedRecordTypeHandlers = new HashMap<>();
	private Map<String, Boolean> cachedAuthorizedToReadRecordLink = new HashMap<>();

	public DataGroupToRecordEnhancerImp(SpiderDependencyProvider dependencyProvider) {
		spiderAuthorizator = dependencyProvider.getSpiderAuthorizator();
		recordStorage = dependencyProvider.getRecordStorage();
		this.collectTermCollector = dependencyProvider.getDataGroupTermCollector();
	}

	@Override
	public DataRecord enhance(User user, String recordType, DataGroup dataGroup) {
		this.user = user;
		this.recordType = recordType;
		this.dataGroup = dataGroup;
		collectedTerms = getCollectedTermsForRecord(recordType, dataGroup);
		record = DataRecordProvider.getDataRecordWithDataGroup(dataGroup);
		handledRecordId = getRecordIdFromDataRecord(record);
		addActions();
		addReadActionToDataRecordLinks(dataGroup);
		return record;
	}

	private DataGroup getCollectedTermsForRecord(String recordType, DataGroup dataGroup) {

		String metadataId = getMetadataIdFromRecordType(recordType);
		return collectTermCollector.collectTerms(metadataId, dataGroup);
	}

	private String getMetadataIdFromRecordType(String recordType) {
		RecordTypeHandler recordTypeHandler = getRecordTypeHandlerForRecordType(recordType);
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
		RecordTypeHandlerImp recordTypeHandler = RecordTypeHandlerImp
				.usingRecordStorageAndRecordTypeId(recordStorage, recordType);
		cachedRecordTypeHandlers.put(recordType, recordTypeHandler);
	}

	private String getRecordIdFromDataRecord(DataRecord dataRecord) {
		DataGroup topLevelDataGroup = dataRecord.getDataGroup();
		DataGroup recordInfo = topLevelDataGroup.getFirstGroupWithNameInData("recordInfo");
		return recordInfo.getFirstAtomicValueWithNameInData("id");
	}

	protected void addActions() {
		if (userIsAuthorizedForActionOnRecordTypeAndCollectedTerms("read", recordType)) {
			record.addAction(Action.READ);
		}
		if (userIsAuthorizedForActionOnRecordTypeAndCollectedTerms("update", recordType)) {
			record.addAction(Action.UPDATE);
		}
		if (userIsAuthorizedForActionOnRecordTypeAndCollectedTerms("index", recordType)) {
			record.addAction(Action.INDEX);
		}
		possiblyAddDeleteAction(record);
		possiblyAddIncomingLinksAction(record);
		possiblyAddUploadAction(record);
		possiblyAddSearchActionWhenRecordTypeSearch(record);
		addActionsForRecordType(record);
	}

	private boolean userIsAuthorizedForActionOnRecordTypeAndCollectedTerms(String action,
			String recordType) {

		return spiderAuthorizator.userIsAuthorizedForActionOnRecordTypeAndCollectedData(user,
				action, recordType, collectedTerms);
	}

	private void possiblyAddDeleteAction(DataRecord dataRecord) {
		if (!incomingLinksExistsForRecord(dataRecord)
				&& userIsAuthorizedForActionOnRecordTypeAndCollectedTerms("delete", recordType)) {
			dataRecord.addAction(Action.DELETE);
		}
	}

	private boolean incomingLinksExistsForRecord(DataRecord dataRecord) {
		DataGroup topLevelDataGroup = dataRecord.getDataGroup();
		DataGroup recordInfo = topLevelDataGroup.getFirstGroupWithNameInData("recordInfo");
		DataGroup typeGroup = recordInfo.getFirstGroupWithNameInData("type");
		String recordTypeForThisRecord = typeGroup
				.getFirstAtomicValueWithNameInData(LINKED_RECORD_ID);

		return linksExistForRecord(recordTypeForThisRecord)
				|| incomingLinksExistsForParentToRecordType(recordTypeForThisRecord);
	}

	private boolean linksExistForRecord(String recordTypeForThisRecord) {
		return recordStorage.linksExistForRecord(recordTypeForThisRecord, handledRecordId);
	}

	private boolean incomingLinksExistsForParentToRecordType(String recordTypeForThisRecord) {
		DataGroup recordTypeDataGroup = readRecordFromStorageByTypeAndId(RECORD_TYPE,
				recordTypeForThisRecord);
		if (handledRecordHasParent(recordTypeDataGroup)) {
			String parentId = extractParentId(recordTypeDataGroup);
			return recordStorage.linksExistForRecord(parentId, handledRecordId);
		}
		return false;
	}

	private DataGroup readRecordFromStorageByTypeAndId(String linkedRecordType,
			String linkedRecordId) {
		return recordStorage.read(linkedRecordType, linkedRecordId);
	}

	private boolean handledRecordHasParent(DataGroup handledRecordTypeDataGroup) {
		return handledRecordTypeDataGroup.containsChildWithNameInData(PARENT_ID);
	}

	private String extractParentId(DataGroup handledRecordTypeDataGroup) {
		DataGroup parentGroup = handledRecordTypeDataGroup.getFirstGroupWithNameInData(PARENT_ID);
		return parentGroup.getFirstAtomicValueWithNameInData(LINKED_RECORD_ID);
	}

	private void possiblyAddIncomingLinksAction(DataRecord dataRecord) {
		if (incomingLinksExistsForRecord(dataRecord)) {
			dataRecord.addAction(Action.READ_INCOMING_LINKS);
		}
	}

	private void possiblyAddUploadAction(DataRecord dataRecord) {
		if (isHandledRecordIdChildOfBinary(recordType)
				&& userIsAuthorizedForActionOnRecordTypeAndCollectedTerms("upload", recordType)) {
			dataRecord.addAction(Action.UPLOAD);
		}
	}

	private boolean isHandledRecordIdChildOfBinary(String dataRecordRecordId) {
		DataGroup handledRecordTypeDataGroup = recordStorage.read(RECORD_TYPE, dataRecordRecordId);
		return isHandledRecordTypeChildOfBinary(handledRecordTypeDataGroup);
	}

	private boolean isHandledRecordTypeChildOfBinary(DataGroup handledRecordTypeDataGroup) {
		if (handledRecordHasParent(handledRecordTypeDataGroup)) {
			return recordTypeWithParentIsChildOfBinary(handledRecordTypeDataGroup);
		}
		return false;
	}

	private boolean recordTypeWithParentIsChildOfBinary(DataGroup handledRecordTypeDataGroup) {
		String refParentId = extractParentId(handledRecordTypeDataGroup);
		return "binary".equals(refParentId);
	}

	private void possiblyAddSearchActionWhenRecordTypeSearch(DataRecord dataRecord) {
		if (isRecordTypeSearch()) {
			List<DataGroup> recordTypeToSearchInGroups = getRecordTypesToSearchInFromSearchGroup();
			addSearchActionIfUserHasAccess(dataRecord, recordTypeToSearchInGroups);
		}
	}

	private boolean isRecordTypeSearch() {
		return SEARCH.equals(recordType);
	}

	private List<DataGroup> getRecordTypesToSearchInFromSearchGroup() {
		return dataGroup.getAllGroupsWithNameInData("recordTypeToSearchIn");
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

	private void addActionsForRecordType(DataRecord dataRecord) {
		if (isRecordType()) {
			possiblyAddCreateAction(dataRecord);
			possiblyAddListAction(dataRecord);
			possiblyAddValidateAction();
			possiblyAddSearchAction(dataRecord);
		}
	}

	private boolean isRecordType() {
		return RECORD_TYPE.equals(recordType);
	}

	private void possiblyAddCreateAction(DataRecord dataRecord) {
		if (!isHandledRecordIdOfTypeAbstract(handledRecordId)
				&& userIsAuthorizedForActionOnRecordType("create", handledRecordId)) {
			dataRecord.addAction(Action.CREATE);
		}
	}

	private boolean isHandledRecordIdOfTypeAbstract(String recordId) {
		String abstractInRecordTypeDefinition = getAbstractFromHandledRecord(recordId);
		return "true".equals(abstractInRecordTypeDefinition);
	}

	private String getAbstractFromHandledRecord(String recordId) {
		DataGroup handleRecordTypeDataGroup = recordStorage.read(RECORD_TYPE, recordId);
		return handleRecordTypeDataGroup.getFirstAtomicValueWithNameInData("abstract");
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

	private void possiblyAddValidateAction() {
		if (userIsAuthorizedForActionOnRecordTypeAndCollectedTerms("validate", recordType)) {
			record.addAction(Action.VALIDATE);
		}
	}

	private void possiblyAddSearchAction(DataRecord dataRecord) {
		if (dataGroup.containsChildWithNameInData(SEARCH)) {
			List<DataGroup> recordTypesToSearchIn = getRecordTypesToSearchInFromLInkedSearch();
			addSearchActionIfUserHasAccess(dataRecord, recordTypesToSearchIn);
		}
	}

	private List<DataGroup> getRecordTypesToSearchInFromLInkedSearch() {
		DataGroup searchChildInRecordType = dataGroup.getFirstGroupWithNameInData(SEARCH);
		String searchId = searchChildInRecordType
				.getFirstAtomicValueWithNameInData(LINKED_RECORD_ID);
		DataGroup searchGroup = recordStorage.read(SEARCH, searchId);
		return searchGroup.getAllGroupsWithNameInData("recordTypeToSearchIn");
	}

	private void addReadActionToDataRecordLinks(DataGroup dataGroup) {
		for (DataElement dataChild : dataGroup.getChildren()) {
			addReadActionToDataRecordLink(dataChild);
		}
	}

	private void addReadActionToDataRecordLink(DataElement dataChild) {
		possiblyAddReadActionIfLink(dataChild);

		if (isGroup(dataChild)) {
			addReadActionToDataRecordLinks((DataGroup) dataChild);
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

		if (isPublicRecordType(linkedRecordType)) {
			return true;
		}
		return whenSecuredRecordType(linkedRecordType, linkedRecordId);
	}

	private boolean whenSecuredRecordType(String linkedRecordType, String linkedRecordId) {
		if (isAlreadyCachedAuthorization(linkedRecordType + linkedRecordId)) {
			return cachedAuthorizedToReadRecordLink.get(linkedRecordType + linkedRecordId);
		} else {
			boolean readAccess = readAuthorizationAndSaveOnCache(linkedRecordType, linkedRecordId);
			cachedAuthorizedToReadRecordLink.put(linkedRecordType + linkedRecordId, readAccess);
			return readAccess;
		}
	}

	private boolean isPublicRecordType(String linkedRecordType) {
		RecordTypeHandler recordTypeHandler = getRecordTypeHandlerForRecordType(linkedRecordType);
		return recordTypeHandler.isPublicForRead();
	}

	private boolean isAlreadyCachedAuthorization(String key) {
		return cachedAuthorizedToReadRecordLink.containsKey(key);
	}

	private boolean readAuthorizationAndSaveOnCache(String linkedRecordType,
			String linkedRecordId) {
		DataGroup linkedRecord = null;
		try {
			linkedRecord = readRecordFromStorageByTypeAndId(linkedRecordType, linkedRecordId);
		} catch (RecordNotFoundException exception) {
			return false;
		}
		return userIsAuthorizedForActionOnRecordTypeAndData("read", linkedRecordType, linkedRecord);
	}

	private boolean userIsAuthorizedForActionOnRecordTypeAndData(String action, String recordType,
			DataGroup dataGroup) {
		DataGroup linkedRecordCollectedTerms = getCollectedTermsForRecord(recordType, null);

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
