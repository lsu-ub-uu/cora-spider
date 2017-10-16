/*
 * Copyright 2015 Uppsala University Library
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

import se.uu.ub.cora.beefeater.authentication.User;
import se.uu.ub.cora.bookkeeper.data.DataGroup;
import se.uu.ub.cora.spider.authentication.Authenticator;
import se.uu.ub.cora.spider.authorization.SpiderAuthorizator;
import se.uu.ub.cora.spider.dependency.SpiderDependencyProvider;
import se.uu.ub.cora.spider.search.RecordIndexer;

public final class SpiderRecordDeleterImp extends SpiderRecordHandler
		implements SpiderRecordDeleter {
	private static final String DELETE = "delete";
	private Authenticator authenticator;
	private SpiderAuthorizator spiderAuthorizator;
	private String authToken;
	private User user;
	private RecordIndexer recordIndexer;

	private SpiderRecordDeleterImp(SpiderDependencyProvider dependencyProvider) {
		this.authenticator = dependencyProvider.getAuthenticator();
		this.spiderAuthorizator = dependencyProvider.getSpiderAuthorizator();
		this.recordStorage = dependencyProvider.getRecordStorage();
		this.recordIndexer = dependencyProvider.getRecordIndexer();
	}

	public static SpiderRecordDeleterImp usingDependencyProvider(
			SpiderDependencyProvider dependencyProvider) {
		return new SpiderRecordDeleterImp(dependencyProvider);
	}

	@Override
	public void deleteRecord(String authToken, String recordType, String recordId) {
		this.authToken = authToken;
		this.recordType = recordType;
		tryToGetActiveUser();
		checkUserIsAuthorizedToDeleteStoredRecord(recordType, recordId);
		checkNoIncomingLinksExists(recordType, recordId);
		recordStorage.deleteByTypeAndId(recordType, recordId);
		recordIndexer.deleteFromIndex(recordType, recordId);
	}

	private void tryToGetActiveUser() {
		user = authenticator.getUserForToken(authToken);
	}

	private void checkUserIsAuthorizedToDeleteStoredRecord(String recordType, String recordId) {
		DataGroup record = recordStorage.read(recordType, recordId);
		spiderAuthorizator.checkUserIsAuthorizedForActionOnRecordTypeAndRecord(user, DELETE,
				recordType, record);
	}

	private void checkNoIncomingLinksExists(String recordType, String recordId) {
		if (recordStorage.linksExistForRecord(recordType, recordId)
				|| incomingLinksExistsForParentToRecordType(recordType, recordId)) {
			throw new MisuseException("Deleting record: " + recordId
					+ " is not allowed since other records are linking to it");
		}
	}

	private Boolean incomingLinksExistsForParentToRecordType(String recordTypeForThisRecord,
			String recordId) {
		DataGroup recordTypeDataGroup = recordStorage.read(RECORD_TYPE, recordTypeForThisRecord);
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
		DataGroup parentGroup = handledRecordTypeDataGroup.getFirstGroupWithNameInData("parentId");
		return parentGroup.getFirstAtomicValueWithNameInData("linkedRecordId");
	}

}
