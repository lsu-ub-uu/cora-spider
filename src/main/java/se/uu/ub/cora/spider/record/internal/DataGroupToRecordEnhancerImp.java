/*
 * Copyright 2016, 2017, 2019, 2020, 2024, 2025, 2026 Uppsala University Library
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
import se.uu.ub.cora.data.collected.PermissionTerm;
import se.uu.ub.cora.spider.authorization.SpiderAuthorizator;
import se.uu.ub.cora.spider.authorization.internal.LinkAuthorizator;
import se.uu.ub.cora.spider.dependency.SpiderDependencyProvider;
import se.uu.ub.cora.spider.record.DataException;
import se.uu.ub.cora.spider.record.DataGroupToRecordEnhancer;
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
	private Map<String, RecordTypeHandler> cachedRecordTypeHandlers = new HashMap<>();
	private Set<String> readRecordPartPermissions = Collections.emptySet();
	private Set<String> writeRecordPartPermissions = Collections.emptySet();
	private boolean addActionRead = true;
	private List<PermissionTerm> permissionTerms;
	private DataRecordGroup hostRecord;

	private LinkAuthorizator linkAuthorizator;

	public static DataGroupToRecordEnhancerImp usingDependencyProviderAndLinkAuthorizator(
			SpiderDependencyProvider dependencyProvider, LinkAuthorizator linkAuthorizator) {
		return new DataGroupToRecordEnhancerImp(dependencyProvider, linkAuthorizator);
	}

	protected DataGroupToRecordEnhancerImp(SpiderDependencyProvider dependencyProvider,
			LinkAuthorizator linkAuthorizator) {
		this.dependencyProvider = dependencyProvider;
		this.linkAuthorizator = linkAuthorizator;
		spiderAuthorizator = dependencyProvider.getSpiderAuthorizator();
		recordStorage = dependencyProvider.getRecordStorage();
		termCollector = dependencyProvider.getDataGroupTermCollector();
	}

	@Override
	public DataRecord enhance(User user, String recordType, DataRecordGroup dataRecordGroup,
			DataRedactor dataRedactor) {
		this.user = user;
		this.recordType = recordType;
		handledRecordId = dataRecordGroup.getId();
		recordTypeHandler = getRecordTypeHandlerForRecordType(recordType);

		permissionTerms = getPermissionTerms(recordType, dataRecordGroup);
		if (recordTypeHandler.isPublicForRead()) {
			return addActionsToRecord(dataRecordGroup);
		}

		DataRecordGroup redactedDataRecordGroup = ensureReadAccesAndRedact(dataRecordGroup,
				dataRedactor);
		return addActionsToRecord(redactedDataRecordGroup);
	}

	private DataRecordGroup ensureReadAccesAndRedact(DataRecordGroup dataRecordGroup,
			DataRedactor dataRedactor) {
		readRecordPartPermissions = ensureReadAccessAndReturnReadRecordPartPermission(
				dataRecordGroup);
		return redact(dataRecordGroup, dataRedactor);
	}

	private List<PermissionTerm> getPermissionTerms(String recordType,
			DataRecordGroup dataRecordGroup) {
		if (recordTypeHandler.useHostRecord()) {
			hostRecord = readHostRecord(dataRecordGroup);
			return getCollectedTermsForRecordTypeAndRecord(hostRecord.getType(), hostRecord);
		}
		return getCollectedTermsForRecordTypeAndRecord(recordType, dataRecordGroup);

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

	private DataRecordGroup readHostRecord(DataRecordGroup recordGroup) {
		return readRecord(recordGroup.getHostRecord());
	}

	private DataRecordGroup readRecord(DataRecordLink hostLink) {
		String hostType = hostLink.getLinkedRecordType();
		String hostId = hostLink.getLinkedRecordId();
		return recordStorage.read(hostType, hostId);
	}

	private List<PermissionTerm> getCollectedTermsForRecordTypeAndRecord(String recordType,
			DataRecordGroup dataRecordGroup) {
		RecordTypeHandler recordTypeHandlerForRecordType = getRecordTypeHandlerForRecordType(
				recordType);
		String definitionId = recordTypeHandlerForRecordType.getDefinitionId();
		CollectTerms collectedTerms = termCollector.collectTerms(definitionId, dataRecordGroup);
		return collectedTerms.permissionTerms;
	}

	Set<String> ensureReadAccessAndReturnReadRecordPartPermission(DataRecordGroup dataRecordGroup) {
		if (isPublished(dataRecordGroup)) {
			return tryToGetUsersRecordPartPermissions();
		}
		if (usesVisibilityAndPermissionUnit()) {
			checkUserIsAuthorizedForPermissionUnit(dataRecordGroup);
			return checkAndGetUserAuthorizationsForReadAction();
		}
		return checkAndGetUserAuthorizationsForReadAction();
	}

	private boolean usesVisibilityAndPermissionUnit() {
		return recordTypeHandler.useVisibility() && recordTypeHandler.usePermissionUnit();
	}

	private Set<String> noRecordPartPermissions() {
		return Collections.emptySet();
	}

	private DataRecord addActionsToRecord(DataRecordGroup redactedDataRecordGroup) {
		addReadActionToAllRecordLinks(redactedDataRecordGroup);
		DataRecord dataRecord = DataProvider
				.createRecordWithDataRecordGroup(redactedDataRecordGroup);
		addActions(dataRecord);
		addRecordPartPermissions(dataRecord);
		return dataRecord;
	}

	private Set<String> tryToGetUsersRecordPartPermissions() {
		try {
			return checkAndGetUserAuthorizationsForReadAction();
		} catch (Exception _) {
			return noRecordPartPermissions();
		}
	}

	private boolean isPublished(DataRecordGroup dataRecordGroup) {
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
		Optional<String> permissionUnit = getPermissionUnit(dataRecordGroup);
		spiderAuthorizator.checkUserIsAuthorizedForPemissionUnit(user, permissionUnit.get());
	}

	private Optional<String> getPermissionUnit(DataRecordGroup dataRecordGroup) {
		if (recordTypeHandler.useHostRecord()) {
			return getPermissionUnitFromRecord(hostRecord);
		}
		return getPermissionUnitFromRecord(dataRecordGroup);

	}

	private Optional<String> getPermissionUnitFromRecord(DataRecordGroup dataRecordGroup) {
		Optional<String> permissionUnit = dataRecordGroup.getPermissionUnit();
		if (permissionUnit.isEmpty()) {
			throw new DataException("PermissionUnit is missing in the record.");
		}
		return permissionUnit;
	}

	private Set<String> checkAndGetUserAuthorizationsForReadAction() {
		String recordTypeForAuthorization = getRecordTypeForAuthorizationCheck();
		return spiderAuthorizator
				.checkGetUsersMatchedRecordPartPermissionsForActionOnRecordTypeAndCollectedData(
						user, "read", recordTypeForAuthorization, permissionTerms, true);
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
			String recordTypeForAuthorization = getRecordTypeForAuthorizationCheck();
			writeRecordPartPermissions = spiderAuthorizator
					.checkGetUsersMatchedRecordPartPermissionsForActionOnRecordTypeAndCollectedData(
							user, "update", recordTypeForAuthorization, permissionTerms, true);

			dataRecord.addAction(Action.UPDATE);
		} catch (Exception _) {
			writeRecordPartPermissions = Collections.emptySet();
		}
	}

	private String getRecordTypeForAuthorizationCheck() {
		if (recordTypeHandler.useHostRecord()) {
			return hostRecord.getType() + "." + recordType;
		}
		return recordType;
	}

	private String getRecordTypeForAuthorizationCheckUsingRecordType(String recordTypeIn) {
		if (recordTypeHandler.useHostRecord()) {
			return hostRecord.getType() + "." + recordTypeIn;
		}
		return recordTypeIn;
	}

	private void possiblyAddIndexAction(DataRecord dataRecord) {
		if (userIsAuthorizedForActionOnRecordTypeAndCollectedTerms("index", recordType)) {
			dataRecord.addAction(Action.INDEX);
		}
	}

	private boolean userIsAuthorizedForActionOnRecordTypeAndCollectedTerms(String action,
			String recordType) {
		String recordTypeForAuthorization = getRecordTypeForAuthorizationCheckUsingRecordType(
				recordType);
		return spiderAuthorizator.userIsAuthorizedForActionOnRecordTypeAndCollectedData(user,
				action, recordTypeForAuthorization, permissionTerms);
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
		if (isGroup(dataChild)) {
			addReadActionToAllRecordLinks((DataGroup) dataChild);
		}
		if (isRecordLink(dataChild)) {
			possiblyAddReadActionToLink(dataChild);
		}
		if (isResourceLink(dataChild)) {
			addReadActionToLink(dataChild);
		}
	}

	private boolean isRecordLink(DataChild dataChild) {
		return dataChild instanceof DataRecordLink;
	}

	private boolean isResourceLink(DataChild dataChild) {
		return dataChild instanceof DataResourceLink;
	}

	private void possiblyAddReadActionToLink(DataChild dataChild) {
		if (isAuthorizedToReadRecordLink(dataChild)) {
			addReadActionToLink(dataChild);
		}
	}

	private void addReadActionToLink(DataChild dataChild) {
		((DataLink) dataChild).addAction(Action.READ);
	}

	private boolean isAuthorizedToReadRecordLink(DataChild dataChild) {
		return linkAuthorizator.isAuthorizedToReadRecordLink(user, (DataRecordLink) dataChild);
	}

	private boolean isGroup(DataChild dataChild) {
		return dataChild instanceof DataGroup;
	}

	@Override
	public DataRecord enhanceIgnoringReadAccess(User user, String recordType,
			DataRecordGroup dataRecordGroup, DataRedactor dataRedactor) {
		this.user = user;
		this.recordType = recordType;
		handledRecordId = dataRecordGroup.getId();
		recordTypeHandler = getRecordTypeHandlerForRecordType(recordType);

		permissionTerms = getPermissionTerms(recordType, dataRecordGroup);

		if (recordTypeHandler.isPublicForRead()) {
			return addActionsToRecord(dataRecordGroup);
		}
		setNoReadPermissionsIfUserHasNoReadAccess(dataRecordGroup);
		DataRecordGroup redactedDataRecordGroup = redact(dataRecordGroup, dataRedactor);
		return addActionsToRecord(redactedDataRecordGroup);
	}

	private void setNoReadPermissionsIfUserHasNoReadAccess(DataRecordGroup dataRecordGroup) {
		try {
			readRecordPartPermissions = ensureReadAccessAndReturnReadRecordPartPermission(
					dataRecordGroup);
		} catch (Exception _) {
			addActionRead = false;
			readRecordPartPermissions = Collections.emptySet();
		}
	}

	public SpiderDependencyProvider onlyForTestGetDependencyProvider() {
		return dependencyProvider;
	}

	public LinkAuthorizator onlyForTestGetLinkAuthorizator() {
		return linkAuthorizator;
	}
}
