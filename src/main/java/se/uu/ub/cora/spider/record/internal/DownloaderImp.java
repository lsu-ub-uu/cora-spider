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

package se.uu.ub.cora.spider.record.internal;

import java.io.InputStream;
import java.util.List;

import se.uu.ub.cora.data.DataGroup;
import se.uu.ub.cora.data.DataProvider;
import se.uu.ub.cora.data.DataRecordGroup;
import se.uu.ub.cora.spider.authorization.SpiderAuthorizator;
import se.uu.ub.cora.spider.data.DataMissingException;
import se.uu.ub.cora.spider.data.ResourceInputStream;
import se.uu.ub.cora.spider.dependency.SpiderDependencyProvider;
import se.uu.ub.cora.spider.record.Downloader;
import se.uu.ub.cora.storage.RecordNotFoundException;
import se.uu.ub.cora.storage.StreamStorage;
import se.uu.ub.cora.storage.archive.ResourceArchive;

public final class DownloaderImp extends SpiderBinary implements Downloader {
	private static final String RESOURCE_INFO = "resourceInfo";
	private static final String DOWNLOAD = "download";
	private String resourceName;
	private SpiderAuthorizator spiderAuthorizator;
	private StreamStorage streamStorage;
	private DataGroup binaryDataGroup;
	private ResourceArchive resourceArchive;

	private DownloaderImp(SpiderDependencyProvider dependencyProvider) {
		this.authenticator = dependencyProvider.getAuthenticator();
		spiderAuthorizator = dependencyProvider.getSpiderAuthorizator();
		recordStorage = dependencyProvider.getRecordStorage();
		resourceArchive = dependencyProvider.getResourceArchive();
		streamStorage = dependencyProvider.getStreamStorage();

	}

	public static Downloader usingDependencyProvider(
			SpiderDependencyProvider spiderDependencyProvider) {
		return new DownloaderImp(spiderDependencyProvider);
	}

	@Override
	public ResourceInputStream download(String authToken, String type, String id,
			String resourceType) {
		// this.authToken = authToken;
		// this.type = type;
		// this.resourceName = resourceType;
		//
		// tryToGetActiveUser();
		// checkUserIsAuthorizedForActionOnRecordTypeAndResourceName();
		// checkResourceIsPresent();
		//
		// checkRecordTypeIsBinary();
		//
		binaryDataGroup = recordStorage.read(List.of(type), id);

		DataRecordGroup binaryRecordGroup = DataProvider
				.createRecordGroupFromDataGroup(binaryDataGroup);
		String dataDivider = binaryRecordGroup.getDataDivider();

		//
		// String streamId = tryToExtractStreamIdFromResource(resourceType);
		//
		// String dataDivider = extractDataDividerFromData(recordRead);
		//
		// InputStream stream = streamStorage.retrieve(streamId, dataDivider);
		//
		InputStream stream = resourceArchive.read(dataDivider, type, id);

		// String name = extractStreamNameFromData();
		// long size = extractStreamSizeFromData();
		// return ResourceInputStream.withNameSizeInputStream(name, size,
		// "application/octet-stream",
		// stream);

		return ResourceInputStream.withNameSizeInputStream(null, 0, null, stream);

	}

	private void checkUserIsAuthorizedForActionOnRecordTypeAndResourceName() {
		spiderAuthorizator.checkUserIsAuthorizedForActionOnRecordType(user, DOWNLOAD,
				type + "." + resourceName);
	}

	private String tryToExtractStreamIdFromResource(String resource) {
		try {
			DataGroup resourceInfo = binaryDataGroup.getFirstGroupWithNameInData(RESOURCE_INFO);
			DataGroup requestedResource = resourceInfo.getFirstGroupWithNameInData(resource);
			return requestedResource.getFirstAtomicValueWithNameInData("streamId");
		} catch (DataMissingException e) {
			throw RecordNotFoundException.withMessageAndException("resource not found", e);
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

	private String extractStreamNameFromData() {
		DataGroup resourceInfo = binaryDataGroup.getFirstGroupWithNameInData(RESOURCE_INFO);
		DataGroup requestedResource = resourceInfo.getFirstGroupWithNameInData(resourceName);
		return requestedResource.getFirstAtomicValueWithNameInData("filename");
	}

	private long extractStreamSizeFromData() {
		DataGroup resourceInfo = binaryDataGroup.getFirstGroupWithNameInData(RESOURCE_INFO);
		DataGroup requestedResource = resourceInfo.getFirstGroupWithNameInData(resourceName);
		return Long.parseLong(requestedResource.getFirstAtomicValueWithNameInData("filesize"));
	}
}
