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

	public static SpiderUploaderImp usingDependencyProvider(
			SpiderDependencyProvider dependencyProvider) {
		return new SpiderUploaderImp(dependencyProvider);
	}

	private SpiderUploaderImp(SpiderDependencyProvider dependencyProvider) {
		authorization = dependencyProvider.getAuthorizator();
		recordStorage = dependencyProvider.getRecordStorage();
		keyCalculator = dependencyProvider.getPermissionKeyCalculator();
		idGenerator = dependencyProvider.getIdGenerator();
		streamStorage = dependencyProvider.getStreamStorage();
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

		addResourceInfoToMetdataRecord(fileName, fileSize);

		// - store recordRead
		SpiderRecordUpdater spiderRecordUpdater = SpiderInstanceProvider.getSpiderRecordUpdater();
		// - return recordRead
		return spiderRecordUpdater.updateRecord(userId, type, id, spiderRecordRead);
	}

	private void checkRecordTypeIsChildOfBinary() {
		DataGroup recordTypeDefinition = getRecordTypeDefinition();
		if (recordTypeIsChildOfBinary(recordTypeDefinition)) {
			throw new DataException(
					"It is only possible to upload files to recordTypes that are children of binary");
		}
	}

	private DataGroup getRecordTypeDefinition() {
		return recordStorage.read("recordType", recordType);
	}

	private boolean recordTypeIsChildOfBinary(DataGroup recordTypeDefinition) {
		return !recordTypeDefinition.containsChildWithNameInData("parentId")
				|| !recordTypeDefinition.getFirstAtomicValueWithNameInData("parentId")
						.equals("binary");
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

	private void addResourceInfoToMetdataRecord(String fileName, long fileSize) {
		SpiderDataGroup resourceInfo = SpiderDataGroup.withNameInData("resourceInfo");
		spiderRecordRead.addChild(resourceInfo);

		SpiderDataGroup master = SpiderDataGroup.withNameInData("master");
		resourceInfo.addChild(master);

		// - add master stream id to recordRead
		SpiderDataAtomic streamId2 = SpiderDataAtomic.withNameInDataAndValue("streamId", streamId);
		master.addChild(streamId2);

		// - set filename and filesize
		SpiderDataAtomic uploadedFileName = SpiderDataAtomic.withNameInDataAndValue("fileName",
				fileName);
		master.addChild(uploadedFileName);

		SpiderDataAtomic size = SpiderDataAtomic.withNameInDataAndValue("fileSize",
				String.valueOf(fileSize));
		master.addChild(size);
	}
}
