/*
 * Copyright 2016 Olov McKie
 * Copyright 2016, 2023 Uppsala University Library
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
import java.text.MessageFormat;
import java.util.List;

import se.uu.ub.cora.beefeater.authentication.User;
import se.uu.ub.cora.data.DataGroup;
import se.uu.ub.cora.data.DataProvider;
import se.uu.ub.cora.data.DataRecordGroup;
import se.uu.ub.cora.spider.authentication.Authenticator;
import se.uu.ub.cora.spider.authorization.SpiderAuthorizator;
import se.uu.ub.cora.spider.data.ResourceInputStream;
import se.uu.ub.cora.spider.dependency.SpiderDependencyProvider;
import se.uu.ub.cora.spider.record.Downloader;
import se.uu.ub.cora.spider.record.MisuseException;
import se.uu.ub.cora.storage.RecordStorage;
import se.uu.ub.cora.storage.StreamStorage;
import se.uu.ub.cora.storage.archive.ResourceArchive;

public final class DownloaderImp implements Downloader {
	private static final String ACTION_DOWNLOAD = "download";
	private static final String ERR_MESSAGE_MISUSE = "Downloading error: Invalid record type, "
			+ "for type {0} and {1}, must be (binary).";
	private List<String> allowedResourceTypes = List.of("master", "thumbnail", "medium", "large");
	private String resourceType;
	private SpiderAuthorizator spiderAuthorizator;
	private StreamStorage streamStorage;
	private DataGroup binaryDataGroup;
	private ResourceArchive resourceArchive;
	private Authenticator authenticator;
	private RecordStorage recordStorage;
	private String type;
	private String id;

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
		this.type = type;
		this.id = id;
		this.resourceType = resourceType;

		validateInput();

		authenticateAndAuthorizeUser(authToken, type, resourceType);

		binaryDataGroup = recordStorage.read(List.of(type), id);

		DataRecordGroup binaryRecordGroup = DataProvider
				.createRecordGroupFromDataGroup(binaryDataGroup);

		String dataDivider = binaryRecordGroup.getDataDivider();

		if ("master".equals(resourceType)) {
			InputStream stream = resourceArchive.read(dataDivider, type, id);
			return prepareResponseForResourceInputStream(resourceType, binaryRecordGroup, stream);
		}

		InputStream stream = streamStorage.retrieve(id, dataDivider);
		return prepareResponseForResourceInputStream(resourceType, binaryRecordGroup, stream);

	}

	private ResourceInputStream prepareResponseForResourceInputStream(String representation,
			DataRecordGroup binaryRecordGroup, InputStream stream) {
		String originalFileName = binaryRecordGroup
				.getFirstAtomicValueWithNameInData("originalFileName");
		DataGroup resourceInfo = binaryRecordGroup.getFirstGroupWithNameInData("resourceInfo");
		DataGroup resourceGroup = resourceInfo.getFirstGroupWithNameInData(representation);
		String fileSize = resourceGroup.getFirstAtomicValueWithNameInData("fileSize");
		String mimeType = resourceGroup.getFirstAtomicValueWithNameInData("mimeType");

		return ResourceInputStream.withNameSizeInputStream(originalFileName, Long.valueOf(fileSize),
				mimeType, stream);
	}

	private void validateInput() {
		if (typeNotBinary(type)) {
			throw new MisuseException(MessageFormat.format(ERR_MESSAGE_MISUSE, type, id));
		}
		if (notValidResourceType(resourceType)) {
			throw new MisuseException(
					"Not implemented yet for resource type different than master.");
		}
	}

	private boolean notValidResourceType(String resourceType) {
		return !allowedResourceTypes.contains(resourceType);
	}

	private boolean typeNotBinary(String type) {
		return !"binary".equals(type);
	}

	private void authenticateAndAuthorizeUser(String authToken, String type, String resourceType) {
		User user = authenticator.getUserForToken(authToken);
		spiderAuthorizator.checkUserIsAuthorizedForActionOnRecordType(user, ACTION_DOWNLOAD,
				type + "." + resourceType);
	}
}
