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
import java.util.Set;

import se.uu.ub.cora.beefeater.Authorizator;
import se.uu.ub.cora.bookkeeper.data.DataGroup;
import se.uu.ub.cora.spider.data.DataMissingException;
import se.uu.ub.cora.spider.data.SpiderDataAtomic;
import se.uu.ub.cora.spider.data.SpiderDataGroup;
import se.uu.ub.cora.spider.data.SpiderDataRecord;
import se.uu.ub.cora.spider.dependency.SpiderDependencyProvider;
import se.uu.ub.cora.spider.dependency.SpiderInstanceProvider;
import se.uu.ub.cora.spider.record.storage.RecordIdGenerator;
import se.uu.ub.cora.spider.record.storage.RecordStorage;
import se.uu.ub.cora.spider.stream.storage.StreamStorage;

public final class SpiderUploaderImp implements SpiderUploader {
	private static final String RESOURCE_INFO = "resourceInfo";
	private static final String RECORD_INFO = "recordInfo";
	private Authorizator authorization;
	private RecordIdGenerator idGenerator;
	private RecordStorage recordStorage;
	private PermissionKeyCalculator keyCalculator;
	private StreamStorage streamStorage;
	private String userId;
	private String recordType;
	private String recordId;
	private SpiderDataGroup spiderRecordRead;
	private String streamId;

	private SpiderUploaderImp(SpiderDependencyProvider dependencyProvider) {
		authorization = dependencyProvider.getAuthorizator();
		recordStorage = dependencyProvider.getRecordStorage();
		keyCalculator = dependencyProvider.getPermissionKeyCalculator();
		idGenerator = dependencyProvider.getIdGenerator();
		streamStorage = dependencyProvider.getStreamStorage();
	}

	public static SpiderUploaderImp usingDependencyProvider(
			SpiderDependencyProvider dependencyProvider) {
		return new SpiderUploaderImp(dependencyProvider);
	}

	@Override
	public SpiderDataRecord upload(String userId, String type, String id, InputStream stream,
			String fileName) {
		this.userId = userId;
		this.recordType = type;
		this.recordId = id;
		checkRecordTypeIsChildOfBinary();

		DataGroup recordRead = recordStorage.read(type, id);
		spiderRecordRead = SpiderDataGroup.fromDataGroup(recordRead);
		checkUserIsAuthorisedToUploadData(recordRead);
		checkStreamIsPresent(stream);
		checkFileNameIsPresent(fileName);
		streamId = idGenerator.getIdForType(type + "Binary");

		String dataDivider = extractDataDividerFromData(spiderRecordRead);
		long fileSize = streamStorage.store(streamId, dataDivider, stream);

		addOrReplaceResourceInfoToMetdataRecord(fileName, fileSize);

		SpiderRecordUpdater spiderRecordUpdater = SpiderInstanceProvider.getSpiderRecordUpdater();
		return spiderRecordUpdater.updateRecord(userId, type, id, spiderRecordRead);
	}

	private void checkRecordTypeIsChildOfBinary() {
		DataGroup recordTypeDefinition = getRecordTypeDefinition();
		if (recordTypeIsChildOfBinary(recordTypeDefinition)) {
			throw new MisuseException(
					"It is only possible to upload files to recordTypes that are children of binary");
		}
	}

	private DataGroup getRecordTypeDefinition() {
		return recordStorage.read("recordType", recordType);
	}

	private boolean recordTypeIsChildOfBinary(DataGroup recordTypeDefinition) {
		return !recordTypeDefinition.containsChildWithNameInData("parentId") || !"binary"
				.equals(recordTypeDefinition.getFirstAtomicValueWithNameInData("parentId"));
	}

	private void checkUserIsAuthorisedToUploadData(DataGroup recordRead) {
		if (isNotAuthorizedToUpload(recordRead)) {
			throw new AuthorizationException(
					"User:" + userId + " is not authorized to upload for record:" + recordId
							+ " of type:" + recordType);
		}
	}

	private boolean isNotAuthorizedToUpload(DataGroup recordRead) {
		String accessType = "UPLOAD";
		Set<String> recordCalculateKeys = keyCalculator.calculateKeys(accessType, recordType,
				recordRead);
		return !authorization.isAuthorized(userId, recordCalculateKeys);
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

	private String extractDataDividerFromData(SpiderDataGroup spiderDataGroup) {
		SpiderDataGroup recordInfo = spiderDataGroup.extractGroup(RECORD_INFO);
		SpiderDataGroup dataDivider = recordInfo.extractGroup("dataDivider");
		return dataDivider.extractAtomicValue("linkedRecordId");
	}

	private void addOrReplaceResourceInfoToMetdataRecord(String fileName, long fileSize) {
		if (recordAlreadyHasResourceInfo()) {
			addResourceInfoToMetdataRecord(fileName, fileSize);
		} else {
			replaceResourceInfoToMetdataRecord(fileName, fileSize);
		}
	}

	private boolean recordAlreadyHasResourceInfo() {
		return !spiderRecordRead.containsChildWithNameInData(RESOURCE_INFO);
	}

	private void addResourceInfoToMetdataRecord(String fileName, long fileSize) {
		SpiderDataGroup resourceInfo = SpiderDataGroup.withNameInData(RESOURCE_INFO);
		spiderRecordRead.addChild(resourceInfo);

		SpiderDataGroup master = SpiderDataGroup.withNameInData("master");
		resourceInfo.addChild(master);

		SpiderDataAtomic streamId2 = SpiderDataAtomic.withNameInDataAndValue("streamId", streamId);
		master.addChild(streamId2);

		SpiderDataAtomic uploadedFileName = SpiderDataAtomic.withNameInDataAndValue("filename",
				fileName);
		master.addChild(uploadedFileName);

		SpiderDataAtomic size = SpiderDataAtomic.withNameInDataAndValue("filesize",
				String.valueOf(fileSize));
		master.addChild(size);

		SpiderDataAtomic mimeType = SpiderDataAtomic.withNameInDataAndValue("mimeType",
				"application/octet-stream");
		master.addChild(mimeType);
	}

	private void replaceResourceInfoToMetdataRecord(String fileName, long fileSize) {
		spiderRecordRead.removeChild(RESOURCE_INFO);
		addResourceInfoToMetdataRecord(fileName, fileSize);
	}
}
