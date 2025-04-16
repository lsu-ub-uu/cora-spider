/*
 * Copyright 2016, 2017, 2019, 2020, 2024, 2025 Uppsala University Library
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
import java.util.Optional;
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
import se.uu.ub.cora.data.DataParent;
import se.uu.ub.cora.data.DataProvider;
import se.uu.ub.cora.data.DataRecord;
import se.uu.ub.cora.data.DataRecordGroup;
import se.uu.ub.cora.data.DataRecordLink;
import se.uu.ub.cora.data.DataResourceLink;
import se.uu.ub.cora.data.collected.CollectTerms;
import se.uu.ub.cora.spider.authorization.SpiderAuthorizator;
import se.uu.ub.cora.spider.dependency.SpiderDependencyProvider;
import se.uu.ub.cora.spider.record.DataException;
import se.uu.ub.cora.spider.record.DataGroupToRecordEnhancer;
import se.uu.ub.cora.storage.RecordNotFoundException;
import se.uu.ub.cora.storage.RecordStorage;

public class DataGroupToRecordEnhancerImp implements DataGroupToRecordEnhancer {
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
	private Set<String> readRecordPartPermissions = Collections.emptySet();
	private Set<String> writeRecordPartPermissions = Collections.emptySet();
	private boolean addActionRead = true;

	public DataGroupToRecordEnhancerImp(SpiderDependencyProvider dependencyProvider) {
		this.dependencyProvider = dependencyProvider;
		spiderAuthorizator = dependencyProvider.getSpiderAuthorizator();
		recordStorage = dependencyProvider.getRecordStorage();
		termCollector = dependencyProvider.getDataGroupTermCollector();
	}

	@Override
	public DataRecord enhance(User user, String recordType, DataRecordGroup dataRecordGroup,
			DataRedactor dataRedactor) {
		commonSetupForEnhance(user, recordType, dataRecordGroup);
		return enhanceDataGroupToRecord(dataRecordGroup, dataRedactor);
	}

	private void commonSetupForEnhance(User user, String recordType,
			DataRecordGroup dataRecordGroup) {
		this.user = user;
		this.recordType = recordType;
		recordTypeHandler = getRecordTypeHandlerForRecordType(recordType);
		collectedTerms = getCollectedTermsForRecordTypeAndRecord(recordType, dataRecordGroup);
		handledRecordId = dataRecordGroup.getId();
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
			DataRecordGroup dataRecordGroup) {
		RecordTypeHandler recordTypeHandlerForRecordType = getRecordTypeHandlerForRecordType(
				recordType);
		String definitionId = recordTypeHandlerForRecordType.getDefinitionId();
		return termCollector.collectTerms(definitionId, dataRecordGroup);
	}

	private DataRecord enhanceDataGroupToRecord(DataRecordGroup dataRecordGroup,
			DataRedactor dataRedactor) {
		readRecordPartPermissions = ensureReadAccessAndReturnReadRecordPartPemission(
				dataRecordGroup);
		return enhanceDataGroupToRecordUsingReadRecordPartPermissions(dataRecordGroup,
				dataRedactor);
	}

	private DataRecord enhanceDataGroupToRecordUsingReadRecordPartPermissions(
			DataRecordGroup dataRecordGroup, DataRedactor dataRedactor) {
		DataRecordGroup redactedDataRecordGroup = redact(dataRecordGroup, dataRedactor);
		addReadActionToAllRecordLinks(redactedDataRecordGroup);

		DataRecord dataRecord = DataProvider
				.createRecordWithDataRecordGroup(redactedDataRecordGroup);
		addActions(dataRecord);
		addRecordPartPermissions(dataRecord);
		return dataRecord;
	}

	Set<String> ensureReadAccessAndReturnReadRecordPartPemission(DataRecordGroup dataRecordGroup) {
		if (recordTypeHandler.isPublicForRead()) {
			return noRecordPartPermissions();
		}
		if (recordTypeUsesVisibilityAndRecordIsPublished(dataRecordGroup)) {
			return tryToGetUsersRecordPartPermissions();
		}
		if (recordTypeHandler.usePermissionUnit()) {
			checkUserIsAuthorizedForPermissionUnit(dataRecordGroup);
		}
		return checkAndGetUserAuthorizationsForReadAction();
	}

	private Set<String> noRecordPartPermissions() {
		return Collections.emptySet();
	}

	private Set<String> tryToGetUsersRecordPartPermissions() {
		try {
			return spiderAuthorizator
					.checkGetUsersMatchedRecordPartPermissionsForActionOnRecordTypeAndCollectedData(
							user, "read", recordType, collectedTerms.permissionTerms, true);
		} catch (Exception e) {
			return noRecordPartPermissions();
		}
	}

	private boolean recordTypeUsesVisibilityAndRecordIsPublished(DataRecordGroup dataRecordGroup) {
		return recordTypeHandler.useVisibility() && recordIsPublished(dataRecordGroup);
	}

	private boolean recordIsPublished(DataRecordGroup dataRecordGroup) {
		Optional<String> visibility = dataRecordGroup.getVisibility();
		throwExceptionIfVisibilityIsMissing(visibility);
		return "published".equals(visibility.get());
	}

	private void throwExceptionIfVisibilityIsMissing(Optional<String> visibility) {
		if (visibility.isEmpty()) {
			throw new DataException("Visibility is missing in the record.");
		}
	}

	private void checkUserIsAuthorizedForPermissionUnit(DataRecordGroup dataRecordGroup) {
		Optional<String> permissionUnit = getPermissionUnitFromRecord(dataRecordGroup);
		spiderAuthorizator.checkUserIsAuthorizedForPemissionUnit(user, permissionUnit.get());
	}

	private Optional<String> getPermissionUnitFromRecord(DataRecordGroup dataRecordGroup) {
		Optional<String> permissionUnit = dataRecordGroup.getPermissionUnit();
		if (permissionUnit.isEmpty()) {
			throwDataExceptionPermissionUnitMissing();
		}
		return permissionUnit;
	}

	private void throwDataExceptionPermissionUnitMissing() {
		throw new DataException("PermissionUnit is missing in the record.");
	}

	private Set<String> checkAndGetUserAuthorizationsForReadAction() {
		return spiderAuthorizator
				.checkGetUsersMatchedRecordPartPermissionsForActionOnRecordTypeAndCollectedData(
						user, "read", recordType, collectedTerms.permissionTerms, true);
	}

	private void addActions(DataRecord dataRecord) {
		boolean permissionUnitAccess = ifPermissionUnitIsUsedUserHasPermissionUnit(dataRecord);
		possiblyAddReadAction(dataRecord);
		boolean hasIncommingLinks = linksExistForRecordTypeUsingCurrentHandledId(recordType);
		if (permissionUnitAccess) {
			possiblyAddUpdateAction(dataRecord);
			possiblyAddIndexAction(dataRecord);
			possiblyAddDeleteAction(dataRecord, hasIncommingLinks);
			possiblyAddUploadAction(dataRecord);
		}
		possiblyAddIncomingLinksAction(dataRecord, hasIncommingLinks);
		possiblyAddSearchActionWhenDataRepresentsASearch(dataRecord);
		possiblyAddActionsWhenDataRepresentsARecordType(dataRecord);
	}

	private boolean ifPermissionUnitIsUsedUserHasPermissionUnit(DataRecord dataRecord) {
		if (!recordTypeHandler.usePermissionUnit()) {
			return true;
		}
		Optional<String> oPermissionUnit = dataRecord.getDataRecordGroup().getPermissionUnit();
		if (oPermissionUnit.isEmpty()) {
			return false;
		}
		return spiderAuthorizator.getUserIsAuthorizedForPemissionUnit(user, oPermissionUnit.get());
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

	private boolean linksExistForRecordTypeUsingCurrentHandledId(String recordTypeId) {
		return recordStorage.linksExistForRecord(recordTypeId, handledRecordId);
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
		if ("binary".equals(recordType)
				&& userIsAuthorizedForActionOnRecordTypeAndCollectedTerms("upload", recordType)) {
			dataRecord.addAction(Action.UPLOAD);
		}
	}

	private void possiblyAddSearchActionWhenDataRepresentsASearch(DataRecord dataRecord) {
		if (theDataBeeingTurnedIntoARecordIsASearch()) {
			addSearchActionIfUserHasAccessToLinkedSearches(dataRecord,
					dataRecord.getDataRecordGroup());
		}
	}

	private boolean theDataBeeingTurnedIntoARecordIsASearch() {
		return recordTypeHandler.representsTheRecordTypeDefiningSearches();
	}

	private void addSearchActionIfUserHasAccessToLinkedSearches(DataRecord dataRecord,
			DataRecordGroup dataRecordGroup) {
		List<DataRecordLink> links = dataRecordGroup.getChildrenOfTypeAndName(DataRecordLink.class,
				"recordTypeToSearchIn");
		if (checkUserHasSearchAccessOnAllRecordTypesToSearchIn(links)) {
			dataRecord.addAction(Action.SEARCH);
		}
	}

	private boolean checkUserHasSearchAccessOnAllRecordTypesToSearchIn(List<DataRecordLink> links) {
		return links.stream().allMatch(this::isAuthorized);
	}

	private boolean isAuthorized(DataRecordLink link) {
		String linkedRecordTypeId = link.getLinkedRecordId();
		return spiderAuthorizator.userIsAuthorizedForActionOnRecordType(user, SEARCH,
				linkedRecordTypeId);
	}

	private void possiblyAddActionsWhenDataRepresentsARecordType(DataRecord dataRecord) {
		if (theDataBeeingTurnedIntoARecordIsARecordType()) {
			RecordTypeHandler handledRecordTypeHandler = getRecordTypeHandlerForRecordType(
					handledRecordId);
			possiblyAddCreateAction(dataRecord);
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

	private void possiblyAddCreateAction(DataRecord dataRecord) {
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
			DataRecordGroup searchGroup = getLinkedSearchForSearchForRecordHandledByRecordHandler(
					handledRecordTypeHandler);
			addSearchActionIfUserHasAccessToLinkedSearches(dataRecord, searchGroup);
		}
	}

	private boolean hasLinkedSearch(RecordTypeHandler handledRecordTypeHandler) {
		return handledRecordTypeHandler.hasLinkedSearch();
	}

	private DataRecordGroup getLinkedSearchForSearchForRecordHandledByRecordHandler(
			RecordTypeHandler handledRecordTypeHandler) {
		String searchId = handledRecordTypeHandler.getSearchId();
		return readRecordFromStorageByTypeAndId(SEARCH, searchId);
	}

	private DataRecordGroup readRecordFromStorageByTypeAndId(String linkedRecordType,
			String linkedRecordId) {
		return recordStorage.read(linkedRecordType, linkedRecordId);
	}

	private void addRecordPartPermissions(DataRecord dataRecord) {
		dataRecord.addWritePermissions(writeRecordPartPermissions);
		dataRecord.addReadPermissions(readRecordPartPermissions);
		if (!recordTypeHandler.isPublicForRead()) {
			dataRecord.addReadPermissions(writeRecordPartPermissions);
		}
	}

	private DataRecordGroup redact(DataRecordGroup dataRecordGroup, DataRedactor dataRedactor) {
		Set<Constraint> recordPartReadConstraints = recordTypeHandler
				.getReadRecordPartConstraints();
		return dataRedactor.removeChildrenForConstraintsWithoutPermissions(
				recordTypeHandler.getDefinitionId(), dataRecordGroup, recordPartReadConstraints,
				readRecordPartPermissions);
	}

	private void addReadActionToAllRecordLinks(DataParent redactedDataGroup) {
		for (DataChild dataChild : redactedDataGroup.getChildren()) {
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
			possiblyAddReadActionToLink(dataChild);
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

	private void possiblyAddReadActionToLink(DataChild dataChild) {
		if (isAuthorizedToReadLink(dataChild)) {
			((DataLink) dataChild).addAction(Action.READ);
		}
	}

	private boolean isAuthorizedToReadLink(DataChild dataChild) {
		if (isRecordLink(dataChild)) {
			return isAuthorizedToReadRecordLink((DataRecordLink) dataChild);
		}
		return isAuthorizedToReadResourceLink((DataResourceLink) dataChild);
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
			DataRecordGroup linkedRecord = readRecordFromStorageByTypeAndId(linkedRecordType,
					linkedRecordId);

			if (recordTypeUsesVisibilityAndRecordIsPublished2(linkedRecord)) {
				return true;
			}
			return userIsAuthorizedForActionOnRecordLinkAndData("read", linkedRecordType,
					linkedRecord);
		} catch (RecordNotFoundException exception) {
			return false;
		}
	}

	private boolean recordTypeUsesVisibilityAndRecordIsPublished2(DataRecordGroup dataRecordGroup) {
		RecordTypeHandler recordTypeHandlerForRecordType = getRecordTypeHandlerForRecordType(
				dataRecordGroup.getType());
		return recordTypeHandlerForRecordType.useVisibility() && recordIsPublished(dataRecordGroup);
	}

	private boolean userIsAuthorizedForActionOnRecordLinkAndData(String action, String recordType,
			DataRecordGroup linkedRecord) {
		CollectTerms linkedRecordCollectedTerms = getCollectedTermsForRecordTypeAndRecord(
				recordType, linkedRecord);

		return spiderAuthorizator.userIsAuthorizedForActionOnRecordTypeAndCollectedData(user,
				action, recordType, linkedRecordCollectedTerms.permissionTerms);
	}

	private boolean isGroup(DataChild dataChild) {
		return dataChild instanceof DataGroup;
	}

	private boolean isAuthorizedToReadResourceLink(DataResourceLink dataResourceLink) {
		String resourceLinkNameInData = dataResourceLink.getNameInData();
		String actionForResourceLink = "binary." + resourceLinkNameInData;
		return userIsAuthorizedForActionOnRecordTypeAndCollectedTerms("read",
				actionForResourceLink);
	}

	@Override
	public DataRecord enhanceIgnoringReadAccess(User user, String recordType,
			DataRecordGroup dataRecordGroup, DataRedactor dataRedactor) {
		commonSetupForEnhance(user, recordType, dataRecordGroup);
		return enhanceDataGroupToRecordIgnoringReadAccess(dataRecordGroup, dataRedactor);
	}

	private DataRecord enhanceDataGroupToRecordIgnoringReadAccess(DataRecordGroup dataRecordGroup,
			DataRedactor dataRedactor) {
		setNoReadPermissionsIfUserHasNoReadAccess(dataRecordGroup);
		return enhanceDataGroupToRecordUsingReadRecordPartPermissions(dataRecordGroup,
				dataRedactor);
	}

	private void setNoReadPermissionsIfUserHasNoReadAccess(DataRecordGroup dataRecordGroup) {
		try {
			readRecordPartPermissions = ensureReadAccessAndReturnReadRecordPartPemission(
					dataRecordGroup);
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
