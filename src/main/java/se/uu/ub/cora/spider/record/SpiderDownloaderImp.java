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

import java.io.OutputStream;
import java.util.Set;

import se.uu.ub.cora.beefeater.Authorizator;
import se.uu.ub.cora.bookkeeper.data.DataGroup;
import se.uu.ub.cora.spider.data.DataMissingException;
import se.uu.ub.cora.spider.data.SpiderDataGroup;
import se.uu.ub.cora.spider.dependency.SpiderDependencyProvider;
import se.uu.ub.cora.spider.dependency.SpiderDependencyProviderSpy;
import se.uu.ub.cora.spider.record.storage.RecordStorage;
import se.uu.ub.cora.spider.stream.storage.StreamStorage;

public class SpiderDownloaderImp implements SpiderDownloader {
	private String userId;
	private String recordType;
	private String recordId;
	private String resource;
	private Authorizator authorization;
	private RecordStorage recordStorage;
	private PermissionKeyCalculator keyCalculator;
	private StreamStorage streamStorage;
	private SpiderDataGroup spiderRecordRead;

	public static SpiderDownloader usingDependencyProvider(
			SpiderDependencyProviderSpy dependencyProvider) {
		return new SpiderDownloaderImp(dependencyProvider);
	}

	private SpiderDownloaderImp(SpiderDependencyProvider dependencyProvider) {
		authorization = dependencyProvider.getAuthorizator();
		recordStorage = dependencyProvider.getRecordStorage();
		keyCalculator = dependencyProvider.getPermissionKeyCalculator();
		streamStorage = dependencyProvider.getStreamStorage();
	}

	@Override
	public OutputStream download(String userId, String type, String id, String resource) {
		this.userId = userId;
		this.recordType = type;
		this.recordId = id;
		this.resource = resource;

		checkResourceIsPresent();

		checkRecordTypeIsChildOfBinary();

		DataGroup recordRead = recordStorage.read(type, id);
		spiderRecordRead = SpiderDataGroup.fromDataGroup(recordRead);
		checkUserIsAuthorisedToDownloadStream(recordRead);

		SpiderDataGroup resourceInfo = spiderRecordRead.extractGroup("resourceInfo");
		// TODO: make shure entered resource exists
		SpiderDataGroup requestedResource = resourceInfo.extractGroup(resource);
		String streamId = requestedResource.extractAtomicValue("streamId");

		String dataDivider = extractDataDividerFromData(spiderRecordRead);

		// TODO Auto-generated method stub
		// return streamStorage.read(streamId, dataDivider);
		return null;
	}

	private void checkResourceIsPresent() {
		if (resourceIsNull(resource) || resourceHasNoLength(resource)) {
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

	private void checkUserIsAuthorisedToDownloadStream(DataGroup recordRead) {
		if (isNotAuthorizedToDownload(recordRead)) {
			throwAuthorizationException("download");
		}
		if (isNotAuthorizedToResource(recordRead)) {
			throwAuthorizationException("download resource " + resource);
		}

	}

	private void throwAuthorizationException(String reason) {
		throw new AuthorizationException("User:" + userId + " is not authorized to " + reason
				+ "for record:" + recordId + " of type:" + recordType);
	}

	private boolean isNotAuthorizedToDownload(DataGroup recordRead) {
		return isNotAuthorizedTo("DOWNLOAD", recordRead);
	}

	private boolean isNotAuthorizedTo(String accessType, DataGroup recordRead) {
		Set<String> recordCalculateKeys = keyCalculator.calculateKeys(accessType, recordType,
				recordRead);
		return !authorization.isAuthorized(userId, recordCalculateKeys);
	}

	private boolean isNotAuthorizedToResource(DataGroup recordRead) {
		return isNotAuthorizedTo(resource.toUpperCase() + "_RESOURCE", recordRead);
	}

	private String extractDataDividerFromData(SpiderDataGroup spiderDataGroup) {
		SpiderDataGroup recordInfo = spiderDataGroup.extractGroup("recordInfo");
		SpiderDataGroup dataDivider = recordInfo.extractGroup("dataDivider");
		return dataDivider.extractAtomicValue("linkedRecordId");
	}
}
