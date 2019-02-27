/*
 * Copyright 2015, 2016, 2019 Uppsala University Library
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
import se.uu.ub.cora.bookkeeper.termcollector.DataGroupTermCollector;
import se.uu.ub.cora.spider.authentication.Authenticator;
import se.uu.ub.cora.spider.authorization.SpiderAuthorizator;
import se.uu.ub.cora.spider.data.SpiderDataRecord;
import se.uu.ub.cora.spider.dependency.SpiderDependencyProvider;

public final class SpiderRecordReaderImp extends SpiderRecordHandler implements SpiderRecordReader {
	private static final String READ = "read";
	private DataGroupToRecordEnhancer dataGroupToRecordEnhancer;
	private Authenticator authenticator;
	private SpiderAuthorizator spiderAuthorizator;
	private User user;
	private String authToken;
	private DataGroupTermCollector collectTermCollector;
	private RecordTypeHandler recordTypeHandler;

	private SpiderRecordReaderImp(SpiderDependencyProvider dependencyProvider,
			DataGroupToRecordEnhancer dataGroupToRecordEnhancer) {

		this.dataGroupToRecordEnhancer = dataGroupToRecordEnhancer;
		this.authenticator = dependencyProvider.getAuthenticator();
		this.spiderAuthorizator = dependencyProvider.getSpiderAuthorizator();
		this.recordStorage = dependencyProvider.getRecordStorage();
		this.collectTermCollector = dependencyProvider.getDataGroupTermCollector();
	}

	public static SpiderRecordReaderImp usingDependencyProviderAndDataGroupToRecordEnhancer(
			SpiderDependencyProvider dependencyProvider,
			DataGroupToRecordEnhancer dataGroupToRecordEnhancer) {
		return new SpiderRecordReaderImp(dependencyProvider, dataGroupToRecordEnhancer);
	}

	@Override
	public SpiderDataRecord readRecord(String authToken, String recordType, String recordId) {
		this.authToken = authToken;
		this.recordType = recordType;
		this.recordId = recordId;
		tryToGetActiveUser();
		checkUserIsAuthorizedForActionOnRecordType();
		DataGroup recordRead = recordStorage.read(recordType, recordId);

		setRecordTypeHandlerForCurrentRecord(recordType, recordRead);

		checkUserIsAuthorisedToReadData(recordRead);

		return dataGroupToRecordEnhancer.enhance(user, recordType, recordRead);
	}

	private void tryToGetActiveUser() {
		user = authenticator.getUserForToken(authToken);
	}

	private void checkUserIsAuthorizedForActionOnRecordType() {
		RecordTypeHandler recordTypeHandlerForSentInRecordType = RecordTypeHandler
				.usingRecordStorageAndRecordTypeId(recordStorage, recordType);
		if (!recordTypeHandlerForSentInRecordType.isPublicForRead()) {
			spiderAuthorizator.checkUserIsAuthorizedForActionOnRecordType(user, READ, recordType);
		}

	}

	private void setRecordTypeHandlerForCurrentRecord(String recordType, DataGroup recordRead) {
		recordTypeHandler = RecordTypeHandler.usingRecordStorageAndRecordTypeId(recordStorage,
				recordType);
		setImplementingRecordTypeHandlerIfAbstract(recordRead);
	}

	private void setImplementingRecordTypeHandlerIfAbstract(DataGroup recordRead) {
		if (recordTypeHandler.isAbstract()) {
			String implementingRecordType = getImplementingRecordType(recordRead);
			recordTypeHandler = RecordTypeHandler.usingRecordStorageAndRecordTypeId(recordStorage,
					implementingRecordType);
		}
	}

	private String getImplementingRecordType(DataGroup recordRead) {
		DataGroup recordInfo = recordRead.getFirstGroupWithNameInData("recordInfo");
		DataGroup type = recordInfo.getFirstGroupWithNameInData("type");
		return type.getFirstAtomicValueWithNameInData("linkedRecordId");
	}

	private void checkUserIsAuthorisedToReadData(DataGroup recordRead) {
		if (!recordTypeHandler.isPublicForRead()) {
			checkUserIsAuthorisedToReadNonPublicData(recordRead);
		}
	}

	private void checkUserIsAuthorisedToReadNonPublicData(DataGroup recordRead) {
		DataGroup collectedTerms = getCollectedTermsForRecord(recordRead);
		spiderAuthorizator.checkUserIsAuthorizedForActionOnRecordTypeAndCollectedData(user, READ,
				recordType, collectedTerms);
	}

	private DataGroup getCollectedTermsForRecord(DataGroup recordRead) {
		String metadataId = getMetadataIdFromRecordType();
		return collectTermCollector.collectTerms(metadataId, recordRead);
	}

	private String getMetadataIdFromRecordType() {
		return recordTypeHandler.getMetadataId();
	}

}
