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

import java.util.Set;

import se.uu.ub.cora.beefeater.Authorizator;
import se.uu.ub.cora.bookkeeper.data.DataGroup;
import se.uu.ub.cora.spider.dependency.SpiderDependencyProvider;

public final class SpiderRecordDeleterImp extends SpiderRecordHandler
		implements SpiderRecordDeleter {
	private Authorizator authorization;
	private PermissionKeyCalculator keyCalculator;
	private String userId;

	private SpiderRecordDeleterImp(SpiderDependencyProvider dependencyProvider) {
		this.authorization = dependencyProvider.getAuthorizator();
		this.recordStorage = dependencyProvider.getRecordStorage();
		this.keyCalculator = dependencyProvider.getPermissionKeyCalculator();
	}

	public static SpiderRecordDeleterImp usingDependencyProvider(
			SpiderDependencyProvider dependencyProvider) {
		return new SpiderRecordDeleterImp(dependencyProvider);
	}

	@Override
	public void deleteRecord(String userId, String recordType, String recordId) {
		this.userId = userId;
		this.recordType = recordType;
		checkRecordIsNotAbstract(recordType, recordId);
		checkUserIsAuthorized(recordType, recordId);
		checkNoIncomingLinksExists(recordType, recordId);
		recordStorage.deleteByTypeAndId(recordType, recordId);
	}

	private void checkRecordIsNotAbstract(String recordType, String recordId) {
		if (isRecordTypeAbstract()) {
			throw new MisuseException("Deleting record: " + recordId
					+ " on the abstract recordType:" + recordType + " is not allowed");
		}
	}

	private void checkUserIsAuthorized(String recordType, String recordId) {
		if (userIsNotAuthorized(recordType, recordId)) {
			throw new AuthorizationException("User:" + userId
					+ " is not authorized to delete record:" + recordId + " of type:" + recordType);
		}
	}

	private boolean userIsNotAuthorized(String recordType, String recordId) {
		DataGroup readRecord = recordStorage.read(recordType, recordId);
		String accessType = "DELETE";
		Set<String> recordCalculateKeys = keyCalculator.calculateKeys(accessType, recordType,
				readRecord);
		return !authorization.isAuthorized(userId, recordCalculateKeys);
	}

	private void checkNoIncomingLinksExists(String recordType, String recordId) {
		if (recordStorage.linksExistForRecord(recordType, recordId)) {
			throw new MisuseException("Deleting record: " + recordId
					+ " is not allowed since other records are linking to it");
		}
	}

}
