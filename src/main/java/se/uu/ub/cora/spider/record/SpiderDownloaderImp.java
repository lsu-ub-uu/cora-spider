/*
 * Copyright 2016 Olov McKie
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

import se.uu.ub.cora.beefeater.authentication.User;
import se.uu.ub.cora.bookkeeper.data.DataGroup;
import se.uu.ub.cora.spider.authentication.Authenticator;
import se.uu.ub.cora.spider.authorization.SpiderAuthorizator;
import se.uu.ub.cora.spider.data.DataMissingException;
import se.uu.ub.cora.spider.data.SpiderDataGroup;
import se.uu.ub.cora.spider.data.SpiderInputStream;
import se.uu.ub.cora.spider.dependency.SpiderDependencyProvider;
import se.uu.ub.cora.spider.record.storage.RecordNotFoundException;
import se.uu.ub.cora.spider.record.storage.RecordStorage;
import se.uu.ub.cora.spider.stream.storage.StreamStorage;

public final class SpiderDownloaderImp implements SpiderDownloader {
	private static final String RESOURCE_INFO = "resourceInfo";
	private static final String DOWNLOAD = "download";
	private String recordType;
	private String resourceName;
	private Authenticator authenticator;
	private SpiderAuthorizator spiderAuthorizator;
	private RecordStorage recordStorage;
	private StreamStorage streamStorage;
	private SpiderDataGroup spiderRecordRead;
	private String authToken;
	private User user;

	private SpiderDownloaderImp(SpiderDependencyProvider dependencyProvider) {
		this.authenticator = dependencyProvider.getAuthenticator();
		spiderAuthorizator = dependencyProvider.getSpiderAuthorizator();
		recordStorage = dependencyProvider.getRecordStorage();
		streamStorage = dependencyProvider.getStreamStorage();
	}

	public static SpiderDownloader usingDependencyProvider(
			SpiderDependencyProvider spiderDependencyProvider) {
		return new SpiderDownloaderImp(spiderDependencyProvider);
	}

	@Override
	public SpiderInputStream download(String authToken, String type, String id,
			String resourceName) {
		this.authToken = authToken;
		this.recordType = type;
		this.resourceName = resourceName;

		tryToGetActiveUser();
		checkUserIsAuthorizedForActionOnRecordType();
		checkResourceIsPresent();

		checkRecordTypeIsChildOfBinary();

		DataGroup recordRead = recordStorage.read(type, id);
		spiderRecordRead = SpiderDataGroup.fromDataGroup(recordRead);

		String streamId = tryToExtractStreamIdFromResource(resourceName);

		String dataDivider = extractDataDividerFromData();

		InputStream stream = streamStorage.retrieve(streamId, dataDivider);

		String name = extractStreamNameFromData();
		long size = extractStreamSizeFromData();
		return SpiderInputStream.withNameSizeInputStream(name, size, "application/octet-stream",
				stream);

	}

	private void checkUserIsAuthorizedForActionOnRecordType() {
		spiderAuthorizator.checkUserIsAuthorizedForActionOnRecordType(user, DOWNLOAD,
				recordType + "." + resourceName);
	}

	private void tryToGetActiveUser() {
		user = authenticator.getUserForToken(authToken);
	}

	private String tryToExtractStreamIdFromResource(String resource) {
		try {
			SpiderDataGroup resourceInfo = spiderRecordRead.extractGroup(RESOURCE_INFO);
			SpiderDataGroup requestedResource = resourceInfo.extractGroup(resource);
			return requestedResource.extractAtomicValue("streamId");
		} catch (DataMissingException e) {
			throw new RecordNotFoundException("resource not found");
		}
	}

	private void checkResourceIsPresent() {
		if (resourceIsNull(resourceName) || resourceHasNoLength(resourceName)) {
			throw new DataMissingException("No resource to download");
		}
	}

	private boolean resourceIsNull(String fileName) {
		return null == fileName;
	}

	private boolean resourceHasNoLength(String fileName) {
		return fileName.length() == 0;
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

	private String extractDataDividerFromData() {
		SpiderDataGroup recordInfo = spiderRecordRead.extractGroup("recordInfo");
		SpiderDataGroup dataDivider = recordInfo.extractGroup("dataDivider");
		return dataDivider.extractAtomicValue("linkedRecordId");
	}

	private String extractStreamNameFromData() {
		SpiderDataGroup resourceInfo = spiderRecordRead.extractGroup(RESOURCE_INFO);
		SpiderDataGroup requestedResource = resourceInfo.extractGroup(resourceName);
		return requestedResource.extractAtomicValue("filename");
	}

	private long extractStreamSizeFromData() {
		SpiderDataGroup resourceInfo = spiderRecordRead.extractGroup(RESOURCE_INFO);
		SpiderDataGroup requestedResource = resourceInfo.extractGroup(resourceName);
		return Long.valueOf(requestedResource.extractAtomicValue("filesize"));
	}
}
