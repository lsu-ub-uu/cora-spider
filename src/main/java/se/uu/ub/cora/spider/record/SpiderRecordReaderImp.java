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

import java.util.List;
import java.util.Map;

import se.uu.ub.cora.beefeater.authentication.User;
import se.uu.ub.cora.bookkeeper.recordpart.RecordPartFilter;
import se.uu.ub.cora.bookkeeper.termcollector.DataGroupTermCollector;
import se.uu.ub.cora.data.DataGroup;
import se.uu.ub.cora.data.DataRecord;
import se.uu.ub.cora.spider.authentication.Authenticator;
import se.uu.ub.cora.spider.authorization.SpiderAuthorizator;
import se.uu.ub.cora.spider.dependency.SpiderDependencyProvider;

public final class SpiderRecordReaderImp extends SpiderRecordHandler implements SpiderRecordReader {
	private static final String READ = "read";
	private DataGroupToRecordEnhancer dataGroupToRecordEnhancer;
	private Authenticator authenticator;
	private SpiderAuthorizator spiderAuthorizator;
	private User user;
	private String authToken;
	private DataGroupTermCollector dataGroupTermCollector;
	private RecordTypeHandler recordTypeHandler;
	private SpiderDependencyProvider dependencyProvider;

	private SpiderRecordReaderImp(SpiderDependencyProvider dependencyProvider,
			DataGroupToRecordEnhancer dataGroupToRecordEnhancer) {

		this.dependencyProvider = dependencyProvider;
		this.dataGroupToRecordEnhancer = dataGroupToRecordEnhancer;
		this.authenticator = dependencyProvider.getAuthenticator();
		this.spiderAuthorizator = dependencyProvider.getSpiderAuthorizator();
		this.recordStorage = dependencyProvider.getRecordStorage();
		this.dataGroupTermCollector = dependencyProvider.getDataGroupTermCollector();
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
		this.recordId = recordId;
		tryToGetActiveUser();
		recordTypeHandler = dependencyProvider.getRecordTypeHandler(recordType);
		checkUserIsAuthorizedForActionOnRecordType();
		DataGroup recordRead = recordStorage.read(recordType, recordId);

		setImplementingRecordTypeHandlerIfAbstract(recordRead);
		checkUserIsAuthorisedToReadData(recordRead);

		if (recordTypeHandler.hasRecordPartReadConstraint()) {
			return filterDataGroup(recordType, recordRead);
		}
		return dataGroupToRecordEnhancer.enhance(user, recordType, recordRead);
	}

	private String getMetadataGroupNameInData() {
		DataGroup metadataGroup = recordTypeHandler.getMetadataGroup();
		return metadataGroup.getFirstAtomicValueWithNameInData("nameInData");
	}

	private void tryToGetActiveUser() {
		user = authenticator.getUserForToken(authToken);
	}

	private void checkUserIsAuthorizedForActionOnRecordType() {
		if (readRecordTypeIsNotPublicRead()) {
			spiderAuthorizator.checkUserIsAuthorizedForActionOnRecordType(user, READ, recordType);
		}
	}

	private boolean readRecordTypeIsNotPublicRead() {
		return !recordTypeHandler.isPublicForRead();
	}

	private void setImplementingRecordTypeHandlerIfAbstract(DataGroup recordRead) {
		if (recordTypeHandler.isAbstract()) {
			String implementingRecordType = getImplementingRecordType(recordRead);
			recordTypeHandler = dependencyProvider.getRecordTypeHandler(implementingRecordType);
		}
	}

	private String getImplementingRecordType(DataGroup recordRead) {
		DataGroup recordInfo = recordRead.getFirstGroupWithNameInData("recordInfo");
		DataGroup type = recordInfo.getFirstGroupWithNameInData("type");
		return type.getFirstAtomicValueWithNameInData("linkedRecordId");
	}

	private void checkUserIsAuthorisedToReadData(DataGroup recordRead) {
		if (readRecordTypeIsNotPublicRead()) {
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
		return dataGroupTermCollector.collectTerms(metadataId, recordRead);
	}

	private String getMetadataIdFromRecordType() {
		return recordTypeHandler.getMetadataId();
	}

	private DataRecord filterDataGroup(String recordType, DataGroup recordRead) {
		List<String> usersReadRecordPartPermissions = spiderAuthorizator
				.getUsersReadRecordPartPermissions();
		Map<String, String> recordPartReadConstraints = recordTypeHandler
				.getRecordPartReadWriteConstraints();
		String groupNameInData = getMetadataGroupNameInData();
		RecordPartFilter recordPartFilter = dependencyProvider.getRecordPartFilter();
		DataGroup recordReadFiltered = recordPartFilter.filterReadRecordPartsUsingPermissions(
				groupNameInData, recordRead, recordPartReadConstraints,
				usersReadRecordPartPermissions);
		return dataGroupToRecordEnhancer.enhance(user, recordType, recordReadFiltered);
	}

}
