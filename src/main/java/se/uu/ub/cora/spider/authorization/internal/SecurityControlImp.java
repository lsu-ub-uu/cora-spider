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

import se.uu.ub.cora.beefeater.authentication.User;
import se.uu.ub.cora.bookkeeper.recordtype.RecordTypeHandler;
import se.uu.ub.cora.spider.authentication.Authenticator;
import se.uu.ub.cora.spider.authorization.SpiderAuthorizator;
import se.uu.ub.cora.spider.dependency.SpiderDependencyProvider;

public class SecurityControlImp implements SecurityControl {
	private static final String ACTION_READ = "read";
	private User user;
	private Authenticator authenticator;
	private String recordType;
	private SpiderAuthorizator spiderAuthorizator;
	// private DataRecordGroup recordGroup;
	private SpiderDependencyProvider dependencyProvider;
	private RecordTypeHandler recordTypeHandler;

	public static SecurityControlImp usingDependencyProvider(
			SpiderDependencyProvider dependencyProvider) {
		return new SecurityControlImp(dependencyProvider);
	}

	private SecurityControlImp(SpiderDependencyProvider dependencyProvider) {
		this.dependencyProvider = dependencyProvider;
		authenticator = dependencyProvider.getAuthenticator();
		spiderAuthorizator = dependencyProvider.getSpiderAuthorizator();
	}

	@Override
	public User checkActionAuthorizationForUser(String authToken, String recordType,
			String action) {
		this.recordType = recordType;

		tryToGetActiveUser(authToken);
		recordTypeHandler = dependencyProvider.getRecordTypeHandler(recordType);
		possiblyCheckUserIsAuthorizedForActionOnRecordType(action);
		return user;
	}

	private void tryToGetActiveUser(String authToken) {
		user = authenticator.getUserForToken(authToken);
	}

	private void possiblyCheckUserIsAuthorizedForActionOnRecordType(String action) {
		if (recordTypeIsPublicForRead(action)) {
			return;
		}
		checkUserIsAuthorizedForActionOnRecordType(action);
	}

	private boolean recordTypeIsPublicForRead(String action) {
		return (ACTION_READ.equals(action) && recordTypeHandler.isPublicForRead());
	}

	private void checkUserIsAuthorizedForActionOnRecordType(String action) {
		spiderAuthorizator.checkUserIsAuthorizedForActionOnRecordType(user, action, recordType);
	}
	// String securityCheckRecordType = getSecurityCheckRecordType();
	//
	// private String getSecurityCheckRecordType() {
	// if (recordTypeHandler.useHostRecord()) {
	// DataRecordLink hostRecordLink = recordGroup.getHostRecord();
	// return hostRecordLink.getLinkedRecordType() + "." + recordType;
	// }
	// return recordType;
	// }

	public SpiderDependencyProvider onlyForTestGetDependencyProvider() {
		return dependencyProvider;
	}

}
