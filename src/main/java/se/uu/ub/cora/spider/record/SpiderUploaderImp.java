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
import se.uu.ub.cora.bookkeeper.linkcollector.DataRecordLinkCollector;
import se.uu.ub.cora.bookkeeper.validator.DataValidator;
import se.uu.ub.cora.spider.data.DataMissingException;
import se.uu.ub.cora.spider.data.SpiderDataRecord;
import se.uu.ub.cora.spider.record.storage.RecordStorage;

public final class SpiderUploaderImp implements SpiderUploader {

	private Authorizator authorization;
	private DataValidator dataValidator;
	private RecordStorage recordStorage;
	private PermissionKeyCalculator keyCalculator;
	private DataRecordLinkCollector linkCollector;
	private String userId;
	private String recordType;
	private String recordId;

	public static SpiderUploaderImp usingAuthorizationAndDataValidatorAndRecordStorageAndKeyCalculatorAndLinkCollector(
			Authorizator authorizator, DataValidator dataValidator, RecordStorage recordStorage,
			PermissionKeyCalculator permissionKeyCalculator,
			DataRecordLinkCollector linkCollector) {
		return new SpiderUploaderImp(authorizator, dataValidator, recordStorage,
				permissionKeyCalculator, linkCollector);
	}

	private SpiderUploaderImp(Authorizator authorization, DataValidator dataValidator,
			RecordStorage recordStorage, PermissionKeyCalculator keyCalculator,
			DataRecordLinkCollector linkCollector) {
		this.authorization = authorization;
		this.dataValidator = dataValidator;
		this.recordStorage = recordStorage;
		this.keyCalculator = keyCalculator;
		this.linkCollector = linkCollector;
	}

	@Override
	public SpiderDataRecord upload(String userId, String type, String id, InputStream inputStream,
			String fileName) {
		this.userId = userId;
		this.recordType = type;
		this.recordId = id;
		DataGroup recordRead = recordStorage.read(type, id);
		checkUserIsAuthorisedToUploadData(recordRead);
		checkStreamIsPresent(inputStream);
		checkFileNameIsPresent(fileName);
		return null;
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
}
