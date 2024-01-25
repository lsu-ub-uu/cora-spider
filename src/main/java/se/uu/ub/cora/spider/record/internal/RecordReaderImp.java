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

package se.uu.ub.cora.spider.record.internal;

import java.util.List;

import se.uu.ub.cora.beefeater.authentication.User;
import se.uu.ub.cora.bookkeeper.recordpart.DataRedactor;
import se.uu.ub.cora.bookkeeper.recordtype.RecordTypeHandler;
import se.uu.ub.cora.data.DataGroup;
import se.uu.ub.cora.data.DataRecord;
import se.uu.ub.cora.spider.authentication.Authenticator;
import se.uu.ub.cora.spider.authorization.SpiderAuthorizator;
import se.uu.ub.cora.spider.dependency.SpiderDependencyProvider;
import se.uu.ub.cora.spider.extendedfunctionality.ExtendedFunctionality;
import se.uu.ub.cora.spider.extendedfunctionality.ExtendedFunctionalityData;
import se.uu.ub.cora.spider.extendedfunctionality.ExtendedFunctionalityProvider;
import se.uu.ub.cora.spider.record.DataGroupToRecordEnhancer;
import se.uu.ub.cora.spider.record.RecordReader;
import se.uu.ub.cora.storage.RecordStorage;

public final class RecordReaderImp implements RecordReader {
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
	private ExtendedFunctionalityProvider extendedFunctionalityProvider;

	private RecordReaderImp(SpiderDependencyProvider dependencyProvider,
			DataGroupToRecordEnhancer dataGroupToRecordEnhancer) {

		this.dependencyProvider = dependencyProvider;
		this.dataGroupToRecordEnhancer = dataGroupToRecordEnhancer;
		this.authenticator = dependencyProvider.getAuthenticator();
		this.spiderAuthorizator = dependencyProvider.getSpiderAuthorizator();
		this.recordStorage = dependencyProvider.getRecordStorage();
		this.extendedFunctionalityProvider = dependencyProvider.getExtendedFunctionalityProvider();
	}

	public static RecordReaderImp usingDependencyProviderAndDataGroupToRecordEnhancer(
			SpiderDependencyProvider dependencyProvider,
			DataGroupToRecordEnhancer dataGroupToRecordEnhancer) {
		return new RecordReaderImp(dependencyProvider, dataGroupToRecordEnhancer);
	}

	@Override
	public DataRecord readRecord(String authToken, String recordType, String recordId) {
		this.authToken = authToken;
		this.recordType = recordType;
		recordTypeHandler = dependencyProvider.getRecordTypeHandler(recordType);

		return tryToReadRecord(recordId);
	}

	private DataRecord tryToReadRecord(String recordId) {
		tryToGetUserWithActiveToken();
		checkUserIsAuthorizedForActionOnRecordType();

		DataGroup recordRead = recordStorage
				.read(recordTypeHandler.getListOfRecordTypeIdsToReadFromStorage(), recordId);
		DataRecord dataRecord = tryToReadAndEnhanceRecord(recordRead);
		useExtendedFunctionalityBeforeReturn(dataRecord);
		return dataRecord;

	}

	// Spike
	private void useExtendedFunctionalityBeforeReturn(DataRecord dataRecord) {
		List<ExtendedFunctionality> extendedFunctionalityList = extendedFunctionalityProvider
				.getFunctionalityForReadBeforeReturn(recordType);
		for (ExtendedFunctionality extendedFunctionality : extendedFunctionalityList) {
			ExtendedFunctionalityData data = new ExtendedFunctionalityData();
			data.dataRecord = dataRecord;
			extendedFunctionality.useExtendedFunctionality(data);
		}
	}

	private void tryToGetUserWithActiveToken() {
		user = authenticator.getUserForToken(authToken);
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
		DataRedactor dataRedactor = dependencyProvider.getDataRedactor();
		return dataGroupToRecordEnhancer.enhance(user, recordType, recordRead, dataRedactor);
	}

}