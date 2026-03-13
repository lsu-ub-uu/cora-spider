/*
 * Copyright 2026 Uppsala University Library
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
import se.uu.ub.cora.bookkeeper.recordtype.RecordTypeHandler;
import se.uu.ub.cora.bookkeeper.termcollector.DataGroupTermCollector;
import se.uu.ub.cora.data.Action;
import se.uu.ub.cora.data.DataChild;
import se.uu.ub.cora.data.DataLink;
import se.uu.ub.cora.data.DataRecord;
import se.uu.ub.cora.data.DataRecordGroup;
import se.uu.ub.cora.data.DataRecordLink;
import se.uu.ub.cora.data.DataResourceLink;
import se.uu.ub.cora.data.collected.CollectTerms;
import se.uu.ub.cora.data.collected.PermissionTerm;
import se.uu.ub.cora.spider.authorization.SpiderAuthorizator;
import se.uu.ub.cora.spider.dependency.SpiderDependencyProvider;
import se.uu.ub.cora.spider.record.DataException;
import se.uu.ub.cora.storage.RecordNotFoundException;
import se.uu.ub.cora.storage.RecordStorage;

public class LinkAuthorizatorImp implements LinkAuthorizator {

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
	private Map<String, Boolean> cachedAuthorizedToReadRecordLink = new HashMap<>();
	private Set<String> readRecordPartPermissions = Collections.emptySet();
	private Set<String> writeRecordPartPermissions = Collections.emptySet();
	private boolean addActionRead = true;
	private List<PermissionTerm> permissionTerms;
	private DataRecordGroup hostRecord;

	private RecordTypeHandler recordTypeHandlerForLink;

	public LinkAuthorizatorImp(SpiderDependencyProvider dependencyProvider) {
		this.dependencyProvider = dependencyProvider;
		spiderAuthorizator = dependencyProvider.getSpiderAuthorizator();
		recordStorage = dependencyProvider.getRecordStorage();
		termCollector = dependencyProvider.getDataGroupTermCollector();
	}

	// @Override
	// public DataRecord enhance(User user, String recordType, DataRecordGroup dataRecordGroup,
	// DataRedactor dataRedactor) {
	// this.user = user;
	// this.recordType = recordType;
	// handledRecordId = dataRecordGroup.getId();
	// recordTypeHandler = getRecordTypeHandlerForRecordType(recordType);
	//
	// if (recordTypeHandler.isPublicForRead()) {
	// permissionTerms = Collections.emptyList();
	// return addActionsToRecord(dataRecordGroup);
	// }
	//
	// permissionTerms = getPermissionTerms(recordType, dataRecordGroup);
	// readRecordPartPermissions = ensureReadAccessAndReturnReadRecordPartPermission(
	// dataRecordGroup);
	// DataRecordGroup redactedDataRecordGroup = redact(dataRecordGroup, dataRedactor);
	// return addActionsToRecord(redactedDataRecordGroup);
	// }

	private List<PermissionTerm> getPermissionTerms(String recordType,
			DataRecordGroup dataRecordGroup) {
		if (recordTypeHandler.useHostRecord()) {
			hostRecord = readHostRecord(dataRecordGroup);
			return getCollectedTermsForRecordTypeAndRecord(hostRecord.getType(), hostRecord);
		}
		return getCollectedTermsForRecordTypeAndRecord(recordType, dataRecordGroup);

	}

	private DataRecordGroup readHostRecord(DataRecordGroup recordGroup) {
		Optional<DataRecordLink> hostRecord = recordGroup.getHostRecord();
		DataRecordLink hostLink = hostRecord.get();
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
		// TODO: if host record, read permission unit from host record
		if (isUnpublishedAndUsesPermissionUnits()) {
			checkUserIsAuthorizedForPermissionUnit(dataRecordGroup);
			return checkAndGetUserAuthorizationsForReadAction();
		}
		return checkAndGetUserAuthorizationsForReadAction();
	}

	private boolean isUnpublishedAndUsesPermissionUnits() {
		return recordTypeHandler.useVisibility() && recordTypeHandler.usePermissionUnit();
	}

	private Set<String> noRecordPartPermissions() {
		return Collections.emptySet();
	}

	// private DataRecord addActionsToRecord(DataRecordGroup redactedDataRecordGroup) {
	// addReadActionToAllRecordLinks(redactedDataRecordGroup);
	// DataRecord dataRecord = DataProvider
	// .createRecordWithDataRecordGroup(redactedDataRecordGroup);
	// addActions(dataRecord);
	// addRecordPartPermissions(dataRecord);
	// return dataRecord;
	// }

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
		// if (recordTypeHandler.useHostRecord()) {
		// return getPermissionUnitFromRecord(hostRecord);
		// }
		return getPermissionUnitFromRecord(dataRecordGroup);

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
		String recordTypeForAuthorization = getRecordTypeForAuthorizationCheck();
		return spiderAuthorizator
				.checkGetUsersMatchedRecordPartPermissionsForActionOnRecordTypeAndCollectedData(
						user, "read", recordTypeForAuthorization, permissionTerms, true);
	}

	// private void addActions(DataRecord dataRecord) {
	// boolean permissionUnitAccess = ifPermissionUnitIsUsedUserHasPermissionUnit(dataRecord);
	// possiblyAddReadAction(dataRecord);
	// boolean hasIncommingLinks = linksExistForRecordTypeUsingCurrentHandledId(recordType);
	// if (permissionUnitAccess) {
	// possiblyAddUpdateAction(dataRecord);
	// possiblyAddIndexAction(dataRecord);
	// possiblyAddDeleteAction(dataRecord, hasIncommingLinks);
	// possiblyAddUploadAction(dataRecord);
	// }
	// possiblyAddIncomingLinksAction(dataRecord, hasIncommingLinks);
	// possiblyAddSearchActionWhenDataRepresentsASearch(dataRecord);
	// possiblyAddActionsWhenDataRepresentsARecordType(dataRecord);
	// }

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

	// private void possiblyAddActionsWhenDataRepresentsARecordType(DataRecord dataRecord) {
	// if (theDataBeeingTurnedIntoARecordIsARecordType()) {
	// RecordTypeHandler handledRecordTypeHandler = getRecordTypeHandlerForRecordType(
	// handledRecordId);
	// possiblyAddCreateAction(dataRecord);
	// possiblyAddListAction(dataRecord);
	// possiblyAddValidateAction(dataRecord);
	// possiblyAddSearchAction(handledRecordTypeHandler, dataRecord);
	// possiblyAddBatchIndexAction(dataRecord);
	// }
	// }

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
		return recordStorage.read(SEARCH, searchId);
	}

	private void addRecordPartPermissions(DataRecord dataRecord) {
		dataRecord.addWritePermissions(writeRecordPartPermissions);
		dataRecord.addReadPermissions(readRecordPartPermissions);
		if (!recordTypeHandler.isPublicForRead()) {
			dataRecord.addReadPermissions(writeRecordPartPermissions);
		}
	}

	// private DataRecordGroup redact(DataRecordGroup dataRecordGroup, DataRedactor dataRedactor) {
	// Set<Constraint> recordPartReadConstraints = recordTypeHandler
	// .getReadRecordPartConstraints();
	// return dataRedactor.removeChildrenForConstraintsWithoutPermissions(
	// recordTypeHandler.getDefinitionId(), dataRecordGroup, recordPartReadConstraints,
	// readRecordPartPermissions);
	// }
	//
	// private void addReadActionToAllRecordLinks(DataParent redactedDataGroup) {
	// for (DataChild dataChild : redactedDataGroup.getChildren()) {
	// addReadActionToDataRecordLink(dataChild);
	// }
	// }
	//
	// private void addReadActionToDataRecordLink(DataChild dataChild) {
	// possiblyAddReadActionIfLink(dataChild);
	//
	// if (isGroup(dataChild)) {
	// addReadActionToAllRecordLinks((DataGroup) dataChild);
	// }
	// }

	// private void possiblyAddReadActionIfLink(DataChild dataChild) {
	// if (isLink(dataChild)) {
	// possiblyAddReadActionToLink(dataChild);
	// }
	// }
	//
	// private boolean isLink(DataChild dataChild) {
	// return isRecordLink(dataChild) || isResourceLink(dataChild);
	// }

	private boolean isRecordLink(DataChild dataChild) {
		return dataChild instanceof DataRecordLink;
	}

	private boolean isResourceLink(DataChild dataChild) {
		return dataChild instanceof DataResourceLink;
	}

	// private void possiblyAddReadActionToLink(DataChild dataChild) {
	// if (isAuthorizedToReadLink(dataChild)) {
	// ((DataLink) dataChild).addAction(Action.READ);
	// }
	// }

	@Override
	public boolean isAuthorizedToReadLink(User user, DataLink dataChild) {
		this.user = user;
		// if (isRecordLink(dataChild)) {
		recordTypeHandlerForLink = getRecordTypeHandlerForRecordLink((DataRecordLink) dataChild);
		return isAuthorizedToReadRecordLink((DataRecordLink) dataChild);
		// }
		// return isAuthorizedToReadResourceLink((DataResourceLink) dataChild);
		// return true;
	}

	private RecordTypeHandler getRecordTypeHandlerForRecordLink(DataRecordLink dataChild) {
		String linkedRecordType = dataChild.getLinkedRecordType();
		return getRecordTypeHandlerForRecordType(linkedRecordType);
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

	private boolean isAuthorizedToReadRecordLink(DataRecordLink dataChild) {
		String linkedRecordType = dataChild.getLinkedRecordType();
		String linkedRecordId = dataChild.getLinkedRecordId();

		if (isRecordLinksTypePublic()) {
			return true;
		}
		return isAuthorizedToReadNonPublicRecordLink(linkedRecordType, linkedRecordId);
	}

	private boolean isRecordLinksTypePublic() {
		return recordTypeHandlerForLink.isPublicForRead();
	}

	private boolean isAuthorizedToReadNonPublicRecordLink(String linkedRecordType,
			String linkedRecordId) {
		// String cacheId = linkedRecordType + linkedRecordId;
		// if (existsCachedAuthorizationForRecordLink(cacheId)) {
		// return cachedAuthorizedToReadRecordLink.get(cacheId);
		// } else {
		boolean readAccess = readRecordLinkAuthorization(linkedRecordType, linkedRecordId);
		// cachedAuthorizedToReadRecordLink.put(cacheId, readAccess);
		return readAccess;
	}

	private boolean existsCachedAuthorizationForRecordLink(String key) {
		return cachedAuthorizedToReadRecordLink.containsKey(key);
	}

	private boolean readRecordLinkAuthorization(String linkedRecordType, String linkedRecordId) {
		try {
			DataRecordGroup linkedRecord = recordStorage.read(linkedRecordType, linkedRecordId);
			// RecordTypeHandler recordTypeHandlerForLinkedRecord =
			// getRecordTypeHandlerForRecordType(
			// linkedRecord.getType());
			//
			if (recordTypeUsesVisibilityAndRecordIsPublished(recordTypeHandlerForLink,
					linkedRecord)) {
				return true;
			}
			boolean a = recordTypeHandlerForLink.useVisibility();
			boolean b = recordTypeHandlerForLink.usePermissionUnit();

			if (recordTypeHandlerForLink.useVisibility()
					&& recordTypeHandlerForLink.usePermissionUnit()) {
				Optional<String> permissionUnit = getPermissionUnit(linkedRecord);
				boolean userIsAuthorizedForPemissionUnit = spiderAuthorizator
						.getUserIsAuthorizedForPemissionUnit(user, permissionUnit.get());
				if (!userIsAuthorizedForPemissionUnit) {
					return false;
				}
			}
			// }
			return userIsAuthorizedForActionOnRecordLinkAndData("read", linkedRecordType,
					linkedRecord);
			// return true;
		} catch (RecordNotFoundException _) {
			return false;
		}
	}

	private boolean recordTypeUsesVisibilityAndRecordIsPublished(
			RecordTypeHandler recordTypeHandlerForRecordType, DataRecordGroup dataRecordGroup) {
		return recordTypeHandlerForRecordType.useVisibility() && recordIsPublished(dataRecordGroup);
	}

	private boolean userIsAuthorizedForActionOnRecordLinkAndData(String action, String recordType,
			DataRecordGroup linkedRecord) {
		// List<PermissionTerm> linkedRecordPermissionTerms =
		// getCollectedTermsForRecordTypeAndRecord(
		// recordType, linkedRecord);

		// return spiderAuthorizator.userIsAuthorizedForActionOnRecordTypeAndCollectedData(user,
		// action, recordType, linkedRecordPermissionTerms);
		return spiderAuthorizator.userIsAuthorizedForActionOnRecordTypeAndCollectedData(user,
				action, null, null);
	}

	// private boolean isGroup(DataChild dataChild) {
	// return dataChild instanceof DataGroup;
	// }

	private boolean isAuthorizedToReadResourceLink(DataResourceLink dataResourceLink) {
		String resourceLinkNameInData = dataResourceLink.getNameInData();
		String recordTypeForResourceLink = recordType + "." + resourceLinkNameInData;
		return userIsAuthorizedForActionOnRecordTypeAndCollectedTerms("read",
				recordTypeForResourceLink);
	}

	// @Override
	// public DataRecord enhanceIgnoringReadAccess(User user, String recordType,
	// DataRecordGroup dataRecordGroup, DataRedactor dataRedactor) {
	// this.user = user;
	// this.recordType = recordType;
	// handledRecordId = dataRecordGroup.getId();
	// recordTypeHandler = getRecordTypeHandlerForRecordType(recordType);
	//
	// if (recordTypeHandler.isPublicForRead()) {
	// permissionTerms = Collections.emptyList();
	// return addActionsToRecord(dataRecordGroup);
	// }
	// permissionTerms = getPermissionTerms(recordType, dataRecordGroup);
	// setNoReadPermissionsIfUserHasNoReadAccess(dataRecordGroup);
	// DataRecordGroup redactedDataRecordGroup = redact(dataRecordGroup, dataRedactor);
	// return addActionsToRecord(redactedDataRecordGroup);
	// }

	// private void setNoReadPermissionsIfUserHasNoReadAccess(DataRecordGroup dataRecordGroup) {
	// try {
	// readRecordPartPermissions = ensureReadAccessAndReturnReadRecordPartPermission(
	// dataRecordGroup);
	// } catch (Exception _) {
	// addActionRead = false;
	// readRecordPartPermissions = Collections.emptySet();
	// }
	// }

	public SpiderDependencyProvider onlyForTestGetDependencyProvider() {
		return dependencyProvider;
	}
}
