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

package se.uu.ub.cora.spider.authorization.internal;

import java.text.MessageFormat;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import se.uu.ub.cora.beefeater.authentication.User;
import se.uu.ub.cora.bookkeeper.recordtype.RecordTypeHandler;
import se.uu.ub.cora.bookkeeper.termcollector.DataGroupTermCollector;
import se.uu.ub.cora.data.DataMissingException;
import se.uu.ub.cora.data.DataRecordGroup;
import se.uu.ub.cora.data.DataRecordLink;
import se.uu.ub.cora.data.collected.CollectTerms;
import se.uu.ub.cora.data.collected.PermissionTerm;
import se.uu.ub.cora.spider.authorization.SpiderAuthorizator;
import se.uu.ub.cora.spider.dependency.SpiderDependencyProvider;
import se.uu.ub.cora.spider.record.DataException;
import se.uu.ub.cora.spider.record.InternalDataMissmatchException;
import se.uu.ub.cora.storage.RecordNotFoundException;
import se.uu.ub.cora.storage.RecordStorage;

public class LinkAuthorizatorImp implements LinkAuthorizator {

	private SpiderDependencyProvider dependencyProvider;
	private SpiderAuthorizator spiderAuthorizator;
	private RecordStorage recordStorage;
	private DataGroupTermCollector termCollector;
	private User user;
	private Map<String, RecordTypeHandler> cachedRecordTypeHandlers = new HashMap<>();
	private Map<String, Boolean> recordLinksHolder = new HashMap<>();
	private RecordTypeHandler recordTypeHandlerForLink;

	public LinkAuthorizatorImp(SpiderDependencyProvider dependencyProvider) {
		this.dependencyProvider = dependencyProvider;
		spiderAuthorizator = dependencyProvider.getSpiderAuthorizator();
		recordStorage = dependencyProvider.getRecordStorage();
		termCollector = dependencyProvider.getDataGroupTermCollector();
	}

	@Override
	public boolean isAuthorizedToReadRecordLink(User user, DataRecordLink recordLink) {
		try {
			return tryIsAuthorizedToReadRecordLink(user, recordLink);
		} catch (DataMissingException e) {
			throw newInternalDataMissmatchException(recordLink, e);
		}
	}

	private boolean tryIsAuthorizedToReadRecordLink(User user, DataRecordLink recordLink) {
		this.user = user;
		String linkedRecordType = recordLink.getLinkedRecordType();
		String linkedRecordId = recordLink.getLinkedRecordId();
		String recordLinksKey = linkedRecordType + linkedRecordId;
		return recordLinksHolder.computeIfAbsent(recordLinksKey,
				_ -> isAuthorizedToReadUnkownRecordLink(linkedRecordType, linkedRecordId));
	}

	private boolean isAuthorizedToReadUnkownRecordLink(String linkedRecordType,
			String linkedRecordId) {
		recordTypeHandlerForLink = getRecordTypeHandlerForRecordType(linkedRecordType);
		if (recordTypeHandlerForLink.isPublicForRead()) {
			return true;
		}
		return tryReadRecordLinkAuthorization(linkedRecordType, linkedRecordId);
	}

	private RecordTypeHandler getRecordTypeHandlerForRecordType(String recordType) {
		return cachedRecordTypeHandlers.computeIfAbsent(recordType,
				this::loadRecordTypeHandlerForRecordType);
	}

	private RecordTypeHandler loadRecordTypeHandlerForRecordType(String recordType) {
		return dependencyProvider.getRecordTypeHandler(recordType);
	}

	private boolean tryReadRecordLinkAuthorization(String linkedRecordType, String linkedRecordId) {
		try {
			return readRecordLinkAuthorization(linkedRecordType, linkedRecordId);
		} catch (RecordNotFoundException _) {
			return false;
		}
	}

	private boolean readRecordLinkAuthorization(String linkedRecordType, String linkedRecordId) {
		DataRecordGroup linkedRecord = recordStorage.read(linkedRecordType, linkedRecordId);

		if (isPublished(linkedRecord)) {
			return true;
		}

		if (usesHostRecord()) {
			return authorizedToReadLinkUsingHostRecord(linkedRecordType, linkedRecord);
		}

		if (usesVisibilityAndPermissionUnit()) {
			return authorizedToReadWhenUnpublishedAndUsesPermissionUnits(linkedRecord,
					linkedRecordType);
		}
		return authorizedToReadLinkUsingRulesAndCollectedData(linkedRecord, linkedRecordType);
	}

	private boolean isPublished(DataRecordGroup dataRecordGroup) {
		return recordTypeHandlerForLink.useVisibility() && recordIsPublished(dataRecordGroup);
	}

	private boolean usesHostRecord() {
		return recordTypeHandlerForLink.useHostRecord();
	}

	private boolean authorizedToReadLinkUsingHostRecord(String linkedRecordType,
			DataRecordGroup linkedRecord) {
		DataRecordGroup hostRecord = readHostRecord(linkedRecord);
		RecordTypeHandler recordTypeHandlerForHostRecord = getRecordTypeHandlerForRecordType(
				hostRecord.getType());
		String recordTypeForRule = constructRecordTypeForRuleUsingHostRecord(linkedRecordType,
				hostRecord);

		if (recordTypeHandlerForHostRecord.usePermissionUnit()) {
			return authorizedToReadWhenUnpublishedAndUsesPermissionUnits(hostRecord,
					recordTypeForRule);
		}
		return authorizedToReadLinkUsingRulesAndCollectedData(hostRecord, recordTypeForRule);
	}

	private String constructRecordTypeForRuleUsingHostRecord(String linkedRecordType,
			DataRecordGroup hostRecord) {
		return hostRecord.getType() + "." + linkedRecordType;
	}

	private boolean usesVisibilityAndPermissionUnit() {
		return recordTypeHandlerForLink.useVisibility()
				&& recordTypeHandlerForLink.usePermissionUnit();
	}

	private boolean authorizedToReadWhenUnpublishedAndUsesPermissionUnits(
			DataRecordGroup linkedRecord, String recordTypeForRule) {
		if (userIsNotAuthorizedForPemissionUnit(linkedRecord)) {
			return false;
		}
		return authorizedToReadLinkUsingRulesAndCollectedData(linkedRecord, recordTypeForRule);
	}

	private boolean userIsNotAuthorizedForPemissionUnit(DataRecordGroup linkedRecord) {
		Optional<String> permissionUnit = getPermissionUnit(linkedRecord);
		return !spiderAuthorizator.getUserIsAuthorizedForPemissionUnit(user, permissionUnit.get());
	}

	private boolean authorizedToReadLinkUsingRulesAndCollectedData(DataRecordGroup dataRecordGroup,
			String recordTypeForRules) {
		List<PermissionTerm> linkedRecordPermissionTerms = getCollectedTermsForRecordTypeAndRecord(
				dataRecordGroup.getType(), dataRecordGroup);

		return spiderAuthorizator.userIsAuthorizedForActionOnRecordTypeAndCollectedData(user,
				"read", recordTypeForRules, linkedRecordPermissionTerms);
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

	private Optional<String> getPermissionUnit(DataRecordGroup dataRecordGroup) {
		return getPermissionUnitFromRecord(dataRecordGroup);

	}

	private Optional<String> getPermissionUnitFromRecord(DataRecordGroup dataRecordGroup) {
		Optional<String> permissionUnit = dataRecordGroup.getPermissionUnit();
		throwExceptionIfPermissionUnitIsMissing(permissionUnit);
		return permissionUnit;
	}

	private void throwExceptionIfPermissionUnitIsMissing(Optional<String> permissionUnit) {
		if (permissionUnit.isEmpty()) {
			throw new DataException("PermissionUnit is missing in the record.");
		}
	}

	private List<PermissionTerm> getCollectedTermsForRecordTypeAndRecord(String recordType,
			DataRecordGroup dataRecordGroup) {
		var recordTypeHandlerForRecordType = getRecordTypeHandlerForRecordType(recordType);
		String definitionId = recordTypeHandlerForRecordType.getDefinitionId();
		CollectTerms collectedTerms = termCollector.collectTerms(definitionId, dataRecordGroup);
		return collectedTerms.permissionTerms;
	}

	private DataRecordGroup readHostRecord(DataRecordGroup recordGroup) {
		return readRecordForLink(recordGroup.getHostRecord());
	}

	private DataRecordGroup readRecordForLink(DataRecordLink hostLink) {
		String hostType = hostLink.getLinkedRecordType();
		String hostId = hostLink.getLinkedRecordId();
		return recordStorage.read(hostType, hostId);
	}

	public SpiderDependencyProvider onlyForTestGetDependencyProvider() {
		return dependencyProvider;
	}

	private InternalDataMissmatchException newInternalDataMissmatchException(
			DataRecordLink recordLink, DataMissingException e) {
		String messageTemplate = "Could not read link because of missing data. "
				+ "Type: {0} and id: {1}, due to: {2}";
		String type = recordLink.getLinkedRecordType();
		String id = recordLink.getLinkedRecordId();
		String originalError = e.getMessage();
		return InternalDataMissmatchException.withMessageAndException(
				MessageFormat.format(messageTemplate, type, id, originalError), e);
	}

}
