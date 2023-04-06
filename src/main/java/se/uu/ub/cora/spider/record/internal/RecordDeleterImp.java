/*
 * Copyright 2015, 2022 Uppsala University Library
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

import java.util.List;

import se.uu.ub.cora.bookkeeper.recordtype.RecordTypeHandler;
import se.uu.ub.cora.bookkeeper.termcollector.DataGroupTermCollector;
import se.uu.ub.cora.data.DataGroup;
import se.uu.ub.cora.data.DataRecordLink;
import se.uu.ub.cora.data.collected.CollectTerms;
import se.uu.ub.cora.search.RecordIndexer;
import se.uu.ub.cora.spider.authentication.Authenticator;
import se.uu.ub.cora.spider.authorization.SpiderAuthorizator;
import se.uu.ub.cora.spider.dependency.SpiderDependencyProvider;
import se.uu.ub.cora.spider.extendedfunctionality.ExtendedFunctionality;
import se.uu.ub.cora.spider.extendedfunctionality.ExtendedFunctionalityProvider;
import se.uu.ub.cora.spider.record.MisuseException;
import se.uu.ub.cora.spider.record.RecordDeleter;

public final class RecordDeleterImp extends RecordHandler implements RecordDeleter {
	private static final String DELETE = "delete";
	private Authenticator authenticator;
	private SpiderAuthorizator spiderAuthorizator;
	private RecordIndexer recordIndexer;
	private DataGroupTermCollector collectTermCollector;
	private ExtendedFunctionalityProvider extendedFunctionalityProvider;
	private DataGroup dataGroupReadFromStorage;

	private RecordDeleterImp(SpiderDependencyProvider dependencyProvider) {
		this.dependencyProvider = dependencyProvider;
		authenticator = dependencyProvider.getAuthenticator();
		spiderAuthorizator = dependencyProvider.getSpiderAuthorizator();
		recordStorage = dependencyProvider.getRecordStorage();
		recordIndexer = dependencyProvider.getRecordIndexer();
		collectTermCollector = dependencyProvider.getDataGroupTermCollector();
		this.extendedFunctionalityProvider = dependencyProvider.getExtendedFunctionalityProvider();
	}

	public static RecordDeleterImp usingDependencyProvider(
			SpiderDependencyProvider dependencyProvider) {
		return new RecordDeleterImp(dependencyProvider);
	}

	@Override
	public void deleteRecord(String authToken, String recordType, String recordId) {
		this.authToken = authToken;
		this.recordType = recordType;
		this.recordId = recordId;
		tryToGetActiveUser();
		checkUserIsAuthorizedForActionOnRecordType();
		dataGroupReadFromStorage = recordStorage.read(List.of(recordType), recordId);

		checkUserIsAuthorizedToDeleteStoredRecord(recordType);
		checkNoIncomingLinksExists(recordType, recordId);

		useExtendedFunctionalityBeforeDelete();
		recordStorage.deleteByTypeAndId(recordType, recordId);
		recordIndexer.deleteFromIndex(recordType, recordId);
		useExtendedFunctionalityAfterDelete();
	}

	private void tryToGetActiveUser() {
		user = authenticator.getUserForToken(authToken);
	}

	private void checkUserIsAuthorizedForActionOnRecordType() {
		spiderAuthorizator.checkUserIsAuthorizedForActionOnRecordType(user, DELETE, recordType);
	}

	private void checkUserIsAuthorizedToDeleteStoredRecord(String recordType) {
		CollectTerms collectTerms = getCollectedTermsForPreviouslyReadRecord(recordType);
		spiderAuthorizator.checkUserIsAuthorizedForActionOnRecordTypeAndCollectedData(user, DELETE,
				recordType, collectTerms.permissionTerms);
	}

	private CollectTerms getCollectedTermsForPreviouslyReadRecord(String recordType) {
		String metadataId = getMetadataIdFromRecordType(recordType);
		return collectTermCollector.collectTerms(metadataId, dataGroupReadFromStorage);
	}

	private String getMetadataIdFromRecordType(String recordType) {
		RecordTypeHandler recordTypeHandler = dependencyProvider.getRecordTypeHandler(recordType);
		return recordTypeHandler.getDefinitionId();
	}

	private void checkNoIncomingLinksExists(String recordType, String recordId) {
		if (recordStorage.linksExistForRecord(recordType, recordId)
				|| incomingLinksExistsForParentToRecordType(recordType, recordId)) {
			throw new MisuseException("Deleting record: " + recordId
					+ " is not allowed since other records are linking to it");
		}
	}

	private boolean incomingLinksExistsForParentToRecordType(String recordTypeForThisRecord,
			String recordId) {
		DataGroup recordTypeDataGroup = recordStorage.read(List.of(RECORD_TYPE),
				recordTypeForThisRecord);
		if (handledRecordHasParent(recordTypeDataGroup)) {
			String parentId = extractParentId(recordTypeDataGroup);
			return recordStorage.linksExistForRecord(parentId, recordId);
		}
		return false;
	}

	private boolean handledRecordHasParent(DataGroup handledRecordTypeDataGroup) {
		return handledRecordTypeDataGroup.containsChildWithNameInData("parentId");
	}

	private String extractParentId(DataGroup handledRecordTypeDataGroup) {
		DataRecordLink parentGroup = (DataRecordLink) handledRecordTypeDataGroup
				.getFirstChildWithNameInData("parentId");
		return parentGroup.getLinkedRecordId();
	}

	private void useExtendedFunctionalityBeforeDelete() {
		List<ExtendedFunctionality> functionalityBeforeDelete = extendedFunctionalityProvider
				.getFunctionalityBeforeDelete(recordType);
		useExtendedFunctionality(dataGroupReadFromStorage, functionalityBeforeDelete);
	}

	private void useExtendedFunctionalityAfterDelete() {
		List<ExtendedFunctionality> functionalityAfterDelete = extendedFunctionalityProvider
				.getFunctionalityAfterDelete(recordType);
		useExtendedFunctionality(dataGroupReadFromStorage, functionalityAfterDelete);
	}

	public SpiderDependencyProvider getDependencyProvider() {
		return dependencyProvider;
	}

}
