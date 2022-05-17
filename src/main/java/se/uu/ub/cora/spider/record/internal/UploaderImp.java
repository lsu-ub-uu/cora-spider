/*
 * Copyright 2016 Uppsala University Library
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

import java.io.InputStream;

import se.uu.ub.cora.bookkeeper.termcollector.DataGroupTermCollector;
import se.uu.ub.cora.data.DataGroup;
import se.uu.ub.cora.data.DataProvider;
import se.uu.ub.cora.data.DataRecord;
import se.uu.ub.cora.data.DataResourceLink;
import se.uu.ub.cora.spider.authorization.SpiderAuthorizator;
import se.uu.ub.cora.spider.data.DataMissingException;
import se.uu.ub.cora.spider.dependency.SpiderDependencyProvider;
import se.uu.ub.cora.spider.dependency.SpiderInstanceProvider;
import se.uu.ub.cora.spider.record.RecordUpdater;
import se.uu.ub.cora.spider.record.Uploader;
import se.uu.ub.cora.spider.recordtype.RecordTypeHandler;
import se.uu.ub.cora.spider.recordtype.internal.RecordTypeHandlerImp;
import se.uu.ub.cora.storage.RecordIdGenerator;
import se.uu.ub.cora.storage.StreamStorage;

public final class UploaderImp extends SpiderBinary implements Uploader {
	private static final String RESOURCE_INFO = "resourceInfo";
	private SpiderAuthorizator spiderAuthorizator;
	private RecordIdGenerator idGenerator;
	private StreamStorage streamStorage;
	private String streamId;
	private DataGroupTermCollector termCollector;
	private DataGroup recordRead;

	private UploaderImp(SpiderDependencyProvider dependencyProvider) {
		authenticator = dependencyProvider.getAuthenticator();
		spiderAuthorizator = dependencyProvider.getSpiderAuthorizator();
		recordStorage = dependencyProvider.getRecordStorage();
		idGenerator = dependencyProvider.getRecordIdGenerator();
		streamStorage = dependencyProvider.getStreamStorage();
		termCollector = dependencyProvider.getDataGroupTermCollector();
	}

	public static UploaderImp usingDependencyProvider(SpiderDependencyProvider dependencyProvider) {
		return new UploaderImp(dependencyProvider);
	}

	@Override
	public DataRecord upload(String authToken, String type, String id, InputStream stream,
			String fileName) {
		this.authToken = authToken;
		this.recordType = type;
		tryToGetActiveUser();
		checkUserIsAuthorizedForActionOnRecordType();

		checkRecordTypeIsChildOfBinary();

		recordRead = recordStorage.read(type, id);
		checkUserIsAuthorisedToUploadData(recordRead);
		checkStreamIsPresent(stream);
		checkFileNameIsPresent(fileName);
		streamId = idGenerator.getIdForType(type + "Binary");

		String dataDivider = extractDataDividerFromData(recordRead);
		long fileSize = streamStorage.store(streamId, dataDivider, stream);

		addOrReplaceResourceInfoToMetdataRecord(fileName, fileSize);

		RecordUpdater spiderRecordUpdater = SpiderInstanceProvider.getRecordUpdater();
		return spiderRecordUpdater.updateRecord(authToken, type, id, recordRead);
	}

	private void checkUserIsAuthorizedForActionOnRecordType() {
		spiderAuthorizator.checkUserIsAuthorizedForActionOnRecordType(user, "upload", recordType);
	}

	private void checkUserIsAuthorisedToUploadData(DataGroup recordRead) {
		DataGroup collectedTerms = getCollectedTermsForRecord(recordType, recordRead);
		spiderAuthorizator.checkUserIsAuthorizedForActionOnRecordTypeAndCollectedData(user,
				"upload", recordType, collectedTerms);
	}

	private DataGroup getCollectedTermsForRecord(String recordType, DataGroup recordRead) {
		String metadataId = getMetadataIdFromRecordType(recordType);
		return termCollector.collectTerms(metadataId, recordRead);
	}

	private String getMetadataIdFromRecordType(String recordType) {
		RecordTypeHandler recordTypeHandler = RecordTypeHandlerImp
				.usingRecordStorageAndRecordTypeId(null, recordStorage, recordType);
		return recordTypeHandler.getMetadataId();
	}

	private void checkStreamIsPresent(InputStream inputStream) {
		if (null == inputStream) {
			throw new DataMissingException("No stream to store");
		}
	}

	private void checkFileNameIsPresent(String fileName) {
		if (fileNameIsNull(fileName) || fileNameHasNoLength(fileName)) {
			throw new DataMissingException("No fileName to store");
		}
	}

	private boolean fileNameIsNull(String fileName) {
		return null == fileName;
	}

	private boolean fileNameHasNoLength(String fileName) {
		return fileName.length() == 0;
	}

	private void addOrReplaceResourceInfoToMetdataRecord(String fileName, long fileSize) {
		if (recordHasNoResourceInfo()) {
			addResourceInfoToMetadataRecord(fileName, fileSize);
		} else {
			replaceResourceInfoToMetadataRecord(fileName, fileSize);
		}
	}

	private boolean recordHasNoResourceInfo() {
		return !recordRead.containsChildWithNameInData(RESOURCE_INFO);
	}

	private void addResourceInfoToMetadataRecord(String fileName, long fileSize) {
		DataGroup resourceInfo = DataProvider.createGroupUsingNameInData(RESOURCE_INFO);
		recordRead.addChild(resourceInfo);
		DataResourceLink master = DataProvider.createResourceLinkUsingNameInData("master");
		resourceInfo.addChild(master);
		master.setStreamId(streamId);
		master.setFileName(fileName);
		master.setFileSize(String.valueOf(fileSize));
		master.setMimeType("application/octet-stream");
	}

	private void replaceResourceInfoToMetadataRecord(String fileName, long fileSize) {
		recordRead.removeFirstChildWithNameInData(RESOURCE_INFO);
		addResourceInfoToMetadataRecord(fileName, fileSize);
	}
}
