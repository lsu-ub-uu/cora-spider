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

package se.uu.ub.cora.spider.record;

import java.io.InputStream;

import se.uu.ub.cora.bookkeeper.termcollector.DataGroupTermCollector;
import se.uu.ub.cora.data.DataAtomic;
import se.uu.ub.cora.data.DataAtomicProvider;
import se.uu.ub.cora.data.DataGroup;
import se.uu.ub.cora.data.DataGroupProvider;
import se.uu.ub.cora.data.DataRecord;
import se.uu.ub.cora.data.DataResourceLink;
import se.uu.ub.cora.data.DataResourceLinkProvider;
import se.uu.ub.cora.spider.authorization.SpiderAuthorizator;
import se.uu.ub.cora.spider.data.DataMissingException;
import se.uu.ub.cora.spider.dependency.SpiderDependencyProvider;
import se.uu.ub.cora.spider.dependency.SpiderInstanceProvider;
import se.uu.ub.cora.storage.RecordIdGenerator;
import se.uu.ub.cora.storage.StreamStorage;

public final class SpiderUploaderImp extends SpiderBinary implements SpiderUploader {
	private static final String RESOURCE_INFO = "resourceInfo";
	private SpiderAuthorizator spiderAuthorizator;
	private RecordIdGenerator idGenerator;
	private StreamStorage streamStorage;
	private String streamId;
	private DataGroupTermCollector termCollector;
	private DataGroup recordRead;

	private SpiderUploaderImp(SpiderDependencyProvider dependencyProvider) {
		authenticator = dependencyProvider.getAuthenticator();
		spiderAuthorizator = dependencyProvider.getSpiderAuthorizator();
		recordStorage = dependencyProvider.getRecordStorage();
		idGenerator = dependencyProvider.getRecordIdGenerator();
		streamStorage = dependencyProvider.getStreamStorage();
		termCollector = dependencyProvider.getDataGroupTermCollector();
	}

	public static SpiderUploaderImp usingDependencyProvider(
			SpiderDependencyProvider dependencyProvider) {
		return new SpiderUploaderImp(dependencyProvider);
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

		SpiderRecordUpdater spiderRecordUpdater = SpiderInstanceProvider.getSpiderRecordUpdater();
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
				.usingRecordStorageAndRecordTypeId(recordStorage, recordType);
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
		DataGroup resourceInfo = DataGroupProvider.getDataGroupUsingNameInData(RESOURCE_INFO);
		recordRead.addChild(resourceInfo);

		DataResourceLink master = DataResourceLinkProvider
				.getDataResourceLinkUsingNameInData("master");
		resourceInfo.addChild(master);

		DataAtomic streamId2 = DataAtomicProvider.getDataAtomicUsingNameInDataAndValue("streamId",
				streamId);
		master.addChild(streamId2);

		DataAtomic uploadedFileName = DataAtomicProvider
				.getDataAtomicUsingNameInDataAndValue("filename", fileName);
		master.addChild(uploadedFileName);

		DataAtomic size = DataAtomicProvider.getDataAtomicUsingNameInDataAndValue("filesize",
				String.valueOf(fileSize));
		master.addChild(size);

		DataAtomic mimeType = DataAtomicProvider.getDataAtomicUsingNameInDataAndValue("mimeType",
				"application/octet-stream");
		master.addChild(mimeType);
	}

	private void replaceResourceInfoToMetadataRecord(String fileName, long fileSize) {
		recordRead.removeFirstChildWithNameInData(RESOURCE_INFO);
		addResourceInfoToMetadataRecord(fileName, fileSize);
	}
}
