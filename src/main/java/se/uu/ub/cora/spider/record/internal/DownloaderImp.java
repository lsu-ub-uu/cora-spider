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
import se.uu.ub.cora.spider.authorization.SpiderAuthorizator;
import se.uu.ub.cora.spider.data.DataMissingException;
import se.uu.ub.cora.spider.data.SpiderInputStream;
import se.uu.ub.cora.spider.dependency.SpiderDependencyProvider;
import se.uu.ub.cora.spider.record.Downloader;
import se.uu.ub.cora.storage.RecordNotFoundException;
import se.uu.ub.cora.storage.StreamStorage;

public final class DownloaderImp extends SpiderBinary implements Downloader {
	private static final String RESOURCE_INFO = "resourceInfo";
	private static final String DOWNLOAD = "download";
	private String resourceName;
	private SpiderAuthorizator spiderAuthorizator;
	private StreamStorage streamStorage;
	private DataGroup recordRead;

	private DownloaderImp(SpiderDependencyProvider dependencyProvider) {
		this.authenticator = dependencyProvider.getAuthenticator();
		spiderAuthorizator = dependencyProvider.getSpiderAuthorizator();
		recordStorage = dependencyProvider.getRecordStorage();
		streamStorage = dependencyProvider.getStreamStorage();
	}

	public static Downloader usingDependencyProvider(
			SpiderDependencyProvider spiderDependencyProvider) {
		return new DownloaderImp(spiderDependencyProvider);
	}

	@Override
	public SpiderInputStream download(String authToken, String type, String id,
			String resourceName) {
		this.authToken = authToken;
		this.recordType = type;
		this.resourceName = resourceName;

		tryToGetActiveUser();
		checkUserIsAuthorizedForActionOnRecordTypeAndResourceName();
		checkResourceIsPresent();

		checkRecordTypeIsBinary();

		recordRead = recordStorage.read(List.of(type), id);

		String streamId = tryToExtractStreamIdFromResource(resourceName);

		String dataDivider = extractDataDividerFromData(recordRead);

		InputStream stream = streamStorage.retrieve(streamId, dataDivider);

		String name = extractStreamNameFromData();
		long size = extractStreamSizeFromData();
		return SpiderInputStream.withNameSizeInputStream(name, size, "application/octet-stream",
				stream);

	}

	private void checkUserIsAuthorizedForActionOnRecordTypeAndResourceName() {
		spiderAuthorizator.checkUserIsAuthorizedForActionOnRecordType(user, DOWNLOAD,
				recordType + "." + resourceName);
	}

	private String tryToExtractStreamIdFromResource(String resource) {
		try {
			DataGroup resourceInfo = recordRead.getFirstGroupWithNameInData(RESOURCE_INFO);
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
		DataGroup resourceInfo = recordRead.getFirstGroupWithNameInData(RESOURCE_INFO);
		DataGroup requestedResource = resourceInfo.getFirstGroupWithNameInData(resourceName);
		return requestedResource.getFirstAtomicValueWithNameInData("filename");
	}

	private long extractStreamSizeFromData() {
		DataGroup resourceInfo = recordRead.getFirstGroupWithNameInData(RESOURCE_INFO);
		DataGroup requestedResource = resourceInfo.getFirstGroupWithNameInData(resourceName);
		return Long.parseLong(requestedResource.getFirstAtomicValueWithNameInData("filesize"));
	}
}
