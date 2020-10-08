/*
 * Copyright 2015, 2016, 2019, 2020 Uppsala University Library
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
import se.uu.ub.cora.data.DataGroup;
import se.uu.ub.cora.data.DataRecord;
import se.uu.ub.cora.logger.Logger;
import se.uu.ub.cora.logger.LoggerProvider;
import se.uu.ub.cora.spider.authentication.Authenticator;
import se.uu.ub.cora.spider.authorization.SpiderAuthorizator;
import se.uu.ub.cora.spider.dependency.SpiderDependencyProvider;
import se.uu.ub.cora.storage.RecordStorage;

public final class SpiderRecordReaderImp implements SpiderRecordReader {
	private static final String READ = "read";
	private DataGroupToRecordEnhancer dataGroupToRecordEnhancer;
	private Authenticator authenticator;
	private SpiderAuthorizator spiderAuthorizator;
	private User user;
	private String authToken;
	private RecordTypeHandler recordTypeHandler;
	private SpiderDependencyProvider dependencyProvider;
	private RecordStorage recordStorage;
	private String recordType;
	private Logger log = LoggerProvider.getLoggerForClass(SpiderRecordReaderImp.class);

	private SpiderRecordReaderImp(SpiderDependencyProvider dependencyProvider,
			DataGroupToRecordEnhancer dataGroupToRecordEnhancer) {

		this.dependencyProvider = dependencyProvider;
		this.dataGroupToRecordEnhancer = dataGroupToRecordEnhancer;
		this.authenticator = dependencyProvider.getAuthenticator();
		this.spiderAuthorizator = dependencyProvider.getSpiderAuthorizator();
		this.recordStorage = dependencyProvider.getRecordStorage();
	}

	public static SpiderRecordReaderImp usingDependencyProviderAndDataGroupToRecordEnhancer(
			SpiderDependencyProvider dependencyProvider,
			DataGroupToRecordEnhancer dataGroupToRecordEnhancer) {
		return new SpiderRecordReaderImp(dependencyProvider, dataGroupToRecordEnhancer);
	}

	@Override
	public DataRecord readRecord(String authToken, String recordType, String recordId) {
		this.authToken = authToken;
		this.recordType = recordType;
		recordTypeHandler = dependencyProvider.getRecordTypeHandler(recordType);

		return tryToReadRecord(recordId);
	}

	private DataRecord tryToReadRecord(String recordId) {
		log.logErrorUsingMessage("1");
		tryToGetUserWithActiveToken();
		log.logErrorUsingMessage("2");

		checkUserIsAuthorizedForActionOnRecordType();
		log.logErrorUsingMessage("2");
		DataGroup recordRead = recordStorage.read(recordType, recordId);
		log.logErrorUsingMessage("3");
		return tryToReadAndEnhanceRecord(recordRead);
	}

	private void tryToGetUserWithActiveToken() {
		user = authenticator.getUserForToken(authToken);
		log.logInfoUsingMessage("user: " + user);
		log.logInfoUsingMessage("user.roles: " + user.roles);
	}

	private void checkUserIsAuthorizedForActionOnRecordType() {
		if (isNotPublicForRead()) {
			spiderAuthorizator.checkUserIsAuthorizedForActionOnRecordType(user, READ, recordType);
		}
	}

	private boolean isNotPublicForRead() {
		return !recordTypeHandler.isPublicForRead();
	}

	private DataRecord tryToReadAndEnhanceRecord(DataGroup recordRead) {
		String implementingRecordType = ensureImplementingRecordType(recordRead);
		return dataGroupToRecordEnhancer.enhance(user, implementingRecordType, recordRead);
	}

	private String ensureImplementingRecordType(DataGroup recordRead) {
		if (recordTypeHandler.isAbstract()) {
			recordType = getImplementingRecordType(recordRead);
		}
		return recordType;
	}

	private String getImplementingRecordType(DataGroup recordRead) {
		DataGroup recordInfo = recordRead.getFirstGroupWithNameInData("recordInfo");
		DataGroup type = recordInfo.getFirstGroupWithNameInData("type");
		return type.getFirstAtomicValueWithNameInData("linkedRecordId");
	}
}