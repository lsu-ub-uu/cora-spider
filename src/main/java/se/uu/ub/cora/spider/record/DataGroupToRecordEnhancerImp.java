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
import se.uu.ub.cora.data.DataGroup;
import se.uu.ub.cora.spider.authorization.SpiderAuthorizator;
import se.uu.ub.cora.spider.data.Action;
import se.uu.ub.cora.spider.data.SpiderDataElement;
import se.uu.ub.cora.spider.data.SpiderDataGroup;
import se.uu.ub.cora.spider.data.SpiderDataLink;
import se.uu.ub.cora.spider.data.SpiderDataRecord;
import se.uu.ub.cora.spider.data.SpiderDataRecordLink;
import se.uu.ub.cora.spider.data.SpiderDataResourceLink;
import se.uu.ub.cora.spider.dependency.SpiderDependencyProvider;
import se.uu.ub.cora.storage.RecordNotFoundException;
import se.uu.ub.cora.storage.RecordStorage;

public class DataGroupToRecordEnhancerImp implements DataGroupToRecordEnhancer {

	private static final String RECORD_TYPE = "recordType";
	private static final String PARENT_ID = "parentId";
	private static final String LINKED_RECORD_ID = "linkedRecordId";
	private static final String SEARCH = "search";
	private DataGroup dataGroup;
	private SpiderDataRecord record;
	private User user;
	private String recordType;
	private String handledRecordId;
	private SpiderAuthorizator spiderAuthorizator;
	private RecordStorage recordStorage;
	private DataGroupTermCollector collectTermCollector;
	private DataGroup collectedTerms;
	private Map<String, RecordTypeHandler> cachedRecordTypeHandlers = new HashMap<>();

	public DataGroupToRecordEnhancerImp(SpiderDependencyProvider dependencyProvider) {
		spiderAuthorizator = dependencyProvider.getSpiderAuthorizator();
		recordStorage = dependencyProvider.getRecordStorage();
		this.collectTermCollector = dependencyProvider.getDataGroupTermCollector();
	}

	@Override
	public SpiderDataRecord enhance(User user, String recordType, DataGroup dataGroup) {
		this.user = user;
		this.recordType = recordType;
		this.dataGroup = dataGroup;
		collectedTerms = getCollectedTermsForRecord(recordType, dataGroup);
		SpiderDataGroup spiderDataGroup = SpiderDataGroup.fromDataGroup(dataGroup);
		record = SpiderDataRecord.withSpiderDataGroup(spiderDataGroup);
		handledRecordId = getRecordIdFromDataRecord(record);
		addActions();
		addReadActionToDataRecordLinks(spiderDataGroup);
		return record;
	}

	private String getRecordIdFromDataRecord(SpiderDataRecord spiderDataRecord) {
		SpiderDataGroup topLevelDataGroup = spiderDataRecord.getSpiderDataGroup();
		SpiderDataGroup recordInfo = topLevelDataGroup.extractGroup("recordInfo");
		return recordInfo.extractAtomicValue("id");
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

	private void possiblyAddValidateAction() {
		if (userIsAuthorizedForActionOnRecordTypeAndCollectedTerms("validate", recordType)) {
			record.addAction(Action.VALIDATE);
		}
	}

	private boolean userIsAuthorizedForActionOnRecordTypeAndCollectedTerms(String action,
			String recordType) {

		return spiderAuthorizator.userIsAuthorizedForActionOnRecordTypeAndCollectedData(user,
				action, recordType, collectedTerms);
	}

	private void possiblyAddDeleteAction(SpiderDataRecord spiderDataRecord) {
		if (!incomingLinksExistsForRecord(spiderDataRecord)
				&& userIsAuthorizedForActionOnRecordTypeAndCollectedTerms("delete", recordType)) {
			spiderDataRecord.addAction(Action.DELETE);
		}
	}

	private boolean incomingLinksExistsForRecord(SpiderDataRecord spiderDataRecord) {
		SpiderDataGroup topLevelDataGroup = spiderDataRecord.getSpiderDataGroup();
		SpiderDataGroup recordInfo = topLevelDataGroup.extractGroup("recordInfo");
		SpiderDataGroup typeGroup = recordInfo.extractGroup("type");
		String recordTypeForThisRecord = typeGroup.extractAtomicValue(LINKED_RECORD_ID);

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

	private DataGroup getCollectedTermsForRecord(String recordType, DataGroup record) {

		String metadataId = getMetadataIdFromRecordType(recordType);
		return collectTermCollector.collectTerms(metadataId, record);
	}

	private String getMetadataIdFromRecordType(String recordType) {
		RecordTypeHandler recordTypeHandler = getRecordTypeHandlerForRecordType(recordType);
		return recordTypeHandler.getMetadataId();
	}

	private void possiblyAddIncomingLinksAction(SpiderDataRecord spiderDataRecord) {
		if (incomingLinksExistsForRecord(spiderDataRecord)) {
			spiderDataRecord.addAction(Action.READ_INCOMING_LINKS);
		}
	}

	private void possiblyAddUploadAction(SpiderDataRecord spiderDataRecord) {
		if (isHandledRecordIdChildOfBinary(recordType)
				&& userIsAuthorizedForActionOnRecordTypeAndCollectedTerms("upload", recordType)) {
			spiderDataRecord.addAction(Action.UPLOAD);
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

	private void addActionsForRecordType(SpiderDataRecord spiderDataRecord) {
		if (isRecordType()) {
			possiblyAddCreateAction(spiderDataRecord);
			possiblyAddListAction(spiderDataRecord);
			possiblyAddValidateAction();
			possiblyAddSearchAction(spiderDataRecord);
		}
	}

	private boolean isRecordType() {
		return RECORD_TYPE.equals(recordType);
	}

	private void possiblyAddCreateAction(SpiderDataRecord spiderDataRecord) {
		if (!isHandledRecordIdOfTypeAbstract(handledRecordId)
				&& userIsAuthorizedForActionOnRecordType("create", handledRecordId)) {
			spiderDataRecord.addAction(Action.CREATE);
		}
	}

	private boolean userIsAuthorizedForActionOnRecordType(String action, String handledRecordId) {
		return spiderAuthorizator.userIsAuthorizedForActionOnRecordType(user, action,
				handledRecordId);
	}

	private void possiblyAddListAction(SpiderDataRecord spiderDataRecord) {
		if (userIsAuthorizedForActionOnRecordType("list", handledRecordId)) {
			spiderDataRecord.addAction(Action.LIST);
		}
	}

	private void possiblyAddSearchAction(SpiderDataRecord spiderDataRecord) {
		if (dataGroup.containsChildWithNameInData(SEARCH)) {
			List<DataGroup> recordTypesToSearchIn = getRecordTypesToSearchInFromLInkedSearch();
			addSearchActionIfUserHasAccess(spiderDataRecord, recordTypesToSearchIn);
		}
	}

	private List<DataGroup> getRecordTypesToSearchInFromLInkedSearch() {
		DataGroup searchChildInRecordType = dataGroup.getFirstGroupWithNameInData(SEARCH);
		String searchId = searchChildInRecordType
				.getFirstAtomicValueWithNameInData(LINKED_RECORD_ID);
		DataGroup searchGroup = recordStorage.read(SEARCH, searchId);
		return searchGroup.getAllGroupsWithNameInData("recordTypeToSearchIn");
	}

	private void possiblyAddSearchActionWhenRecordTypeSearch(SpiderDataRecord spiderDataRecord) {
		if (isRecordTypeSearch()) {
			List<DataGroup> recordTypeToSearchInGroups = getRecordTypesToSearchInFromSearchGroup();
			addSearchActionIfUserHasAccess(spiderDataRecord, recordTypeToSearchInGroups);
		}
	}

	private List<DataGroup> getRecordTypesToSearchInFromSearchGroup() {
		return dataGroup.getAllGroupsWithNameInData("recordTypeToSearchIn");
	}

	private void addSearchActionIfUserHasAccess(SpiderDataRecord spiderDataRecord,
			List<DataGroup> recordTypeToSearchInGroups) {
		if (checkUserHasSearchAccessOnAllRecordTypesToSearchIn(recordTypeToSearchInGroups)) {
			spiderDataRecord.addAction(Action.SEARCH);
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

	private boolean isRecordTypeSearch() {
		return SEARCH.equals(recordType);
	}

	private boolean isHandledRecordIdOfTypeAbstract(String recordId) {
		String abstractInRecordTypeDefinition = getAbstractFromHandledRecord(recordId);
		return "true".equals(abstractInRecordTypeDefinition);
	}

	private String getAbstractFromHandledRecord(String recordId) {
		DataGroup handleRecordTypeDataGroup = recordStorage.read(RECORD_TYPE, recordId);
		return handleRecordTypeDataGroup.getFirstAtomicValueWithNameInData("abstract");
	}

	private void addReadActionToDataRecordLinks(SpiderDataGroup spiderDataGroup) {
		for (SpiderDataElement spiderDataChild : spiderDataGroup.getChildren()) {
			addReadActionToDataRecordLink(spiderDataChild);
		}
	}

	private void addReadActionToDataRecordLink(SpiderDataElement spiderDataChild) {
		possiblyAddReadActionIfLink(spiderDataChild);

		if (isGroup(spiderDataChild)) {
			addReadActionToDataRecordLinks((SpiderDataGroup) spiderDataChild);
		}
	}

	private void possiblyAddReadActionIfLink(SpiderDataElement spiderDataChild) {
		if (isLink(spiderDataChild)) {
			possiblyAddReadAction(spiderDataChild);
		}
	}

	private boolean isLink(SpiderDataElement spiderDataChild) {
		return isRecordLink(spiderDataChild) || isResourceLink(spiderDataChild);
	}

	private boolean isRecordLink(SpiderDataElement spiderDataChild) {
		return spiderDataChild instanceof SpiderDataRecordLink;
	}

	private boolean isResourceLink(SpiderDataElement spiderDataChild) {
		return spiderDataChild instanceof SpiderDataResourceLink;
	}

	private void possiblyAddReadAction(SpiderDataElement spiderDataChild) {
		if (isAuthorizedToReadLink(spiderDataChild)) {
			((SpiderDataLink) spiderDataChild).addAction(Action.READ);
		}
	}

	private boolean isAuthorizedToReadLink(SpiderDataElement spiderDataChild) {
		if (isRecordLink(spiderDataChild)) {
			return isAuthorizedToReadRecordLink((SpiderDataRecordLink) spiderDataChild);
		}
		return isAuthorizedToReadResourceLink();
	}

	private boolean isAuthorizedToReadRecordLink(SpiderDataRecordLink spiderDataChild) {
		String linkedRecordType = spiderDataChild.extractAtomicValue("linkedRecordType");
		String linkedRecordId = spiderDataChild.extractAtomicValue(LINKED_RECORD_ID);
		if (isPublicRecordType(linkedRecordType)) {
			return true;
		}
		DataGroup linkedRecord = null;
		try {
			linkedRecord = readRecordFromStorageByTypeAndId(linkedRecordType, linkedRecordId);
		} catch (RecordNotFoundException exception) {
			return false;
		}
		return userIsAuthorizedForActionOnRecordTypeAndData("read", linkedRecordType, linkedRecord);
	}

	private boolean isPublicRecordType(String linkedRecordType) {
		RecordTypeHandler recordTypeHandler = getRecordTypeHandlerForRecordType(linkedRecordType);
		return recordTypeHandler.isPublicForRead();
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
		RecordTypeHandler recordTypeHandler = RecordTypeHandler
				.usingRecordStorageAndRecordTypeId(recordStorage, recordType);
		cachedRecordTypeHandlers.put(recordType, recordTypeHandler);
	}

	private boolean userIsAuthorizedForActionOnRecordTypeAndData(String action, String recordType,
			DataGroup dataGroup) {
		DataGroup linkedRecordCollectedTerms = getCollectedTermsForRecord(recordType, dataGroup);

		return spiderAuthorizator.userIsAuthorizedForActionOnRecordTypeAndCollectedData(user,
				action, recordType, linkedRecordCollectedTerms);
	}

	private boolean isAuthorizedToReadResourceLink() {
		return userIsAuthorizedForActionOnRecordTypeAndCollectedTerms("read", "image");
	}

	private boolean isGroup(SpiderDataElement spiderDataChild) {
		return spiderDataChild instanceof SpiderDataGroup;
	}

}
