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
import java.util.List;

import se.uu.ub.cora.bookkeeper.recordtype.RecordTypeHandler;
import se.uu.ub.cora.bookkeeper.termcollector.DataGroupTermCollector;
import se.uu.ub.cora.data.DataGroup;
import se.uu.ub.cora.data.DataProvider;
import se.uu.ub.cora.data.DataRecord;
import se.uu.ub.cora.data.DataRecordGroup;
import se.uu.ub.cora.data.DataResourceLink;
import se.uu.ub.cora.data.collected.CollectTerms;
import se.uu.ub.cora.spider.authorization.SpiderAuthorizator;
import se.uu.ub.cora.spider.data.DataMissingException;
import se.uu.ub.cora.spider.dependency.SpiderDependencyProvider;
import se.uu.ub.cora.spider.dependency.SpiderInstanceProvider;
import se.uu.ub.cora.spider.record.RecordUpdater;
import se.uu.ub.cora.spider.record.Uploader;
import se.uu.ub.cora.storage.StreamStorage;
import se.uu.ub.cora.storage.archive.ResourceArchive;
import se.uu.ub.cora.storage.idgenerator.RecordIdGenerator;

//Kolla om vi ska behålla SpiderBinary och vad egentligen vi behöver ha i den.
public final class UploaderImp extends SpiderBinary implements Uploader {
	private static final String RESOURCE_INFO = "resourceInfo";
	private static final String MIME_TYPE_JPEG = "image/jpeg";
	private SpiderAuthorizator spiderAuthorizator;
	private RecordIdGenerator idGenerator;
	private StreamStorage streamStorage;
	private String streamId;
	private DataGroupTermCollector termCollector;
	private DataGroup recordRead;
	private SpiderDependencyProvider dependencyProvider;
	private ResourceArchive resourceArchive;

	private UploaderImp(SpiderDependencyProvider dependencyProvider) {
		// this.dependencyProvider = dependencyProvider;
		// authenticator = dependencyProvider.getAuthenticator();
		// spiderAuthorizator = dependencyProvider.getSpiderAuthorizator();
		recordStorage = dependencyProvider.getRecordStorage();
		// idGenerator = dependencyProvider.getRecordIdGenerator();
		// streamStorage = dependencyProvider.getStreamStorage();
		// termCollector = dependencyProvider.getDataGroupTermCollector();
		resourceArchive = dependencyProvider.getResourceArchive();
	}

	public static UploaderImp usingDependencyProvider(SpiderDependencyProvider dependencyProvider) {
		return new UploaderImp(dependencyProvider);
	}

	@Override
	public DataRecord upload(String authToken, String type, String id, InputStream stream,
			String resourceType) {
		// OBS: Vi behöver inte skicka filename utanför, den får räknas ut intern.
		// https://cora.epc.ub.uu.se/diva/rest/record/binary/binary:11750059111622259/master
		// this.authToken = authToken;
		// this.recordType = type;
		// tryToGetActiveUser();
		// checkUserIsAuthorizedForActionOnRecordType();
		//
		// checkRecordTypeIsBinary();
		//

		recordRead = recordStorage.read(List.of(type), id);
		DataRecordGroup readDataRecordGroup = DataProvider
				.createRecordGroupFromDataGroup(recordRead);
		String dataDivider = readDataRecordGroup.getDataDivider();

		// checkUserIsAuthorisedToUploadData(recordRead);
		// checkStreamIsPresent(stream);
		// checkFileNameIsPresent(fileName);
		// streamId = idGenerator.getIdForType(type + "Binary");
		//
		// // Vi borde kunna läsa dataDivider from (DataRecordGroup) recordRead istället
		// String dataDivider = extractDataDividerFromData(recordRead);
		//
		// long fileSize = streamStorage.store(streamId, dataDivider, stream);

		// Än så länge vi resurs id kommer att ha samma id som binär posten.
		// Mimetype är nu satt till "image/jpeg" så länge
		resourceArchive.create(dataDivider, type, id, stream, MIME_TYPE_JPEG);
		//
		// // Den här steg kommer inte att behövas. Vi kommer däremot behöva en redactor steg.
		// addOrReplaceResourceInfoToMetdataRecord(fileName, fileSize);
		//
		DataGroup recordRead2 = null;
		RecordUpdater spiderRecordUpdater = SpiderInstanceProvider.getRecordUpdater();
		return spiderRecordUpdater.updateRecord(authToken, type, id, recordRead2);
	}

	private void checkUserIsAuthorizedForActionOnRecordType() {
		spiderAuthorizator.checkUserIsAuthorizedForActionOnRecordType(user, "upload", recordType);
	}

	private void checkUserIsAuthorisedToUploadData(DataGroup recordRead) {
		CollectTerms collectedTerms = getCollectedTermsForRecord(recordType, recordRead);
		spiderAuthorizator.checkUserIsAuthorizedForActionOnRecordTypeAndCollectedData(user,
				"upload", recordType, collectedTerms.permissionTerms);
	}

	private CollectTerms getCollectedTermsForRecord(String recordType, DataGroup recordRead) {
		String metadataId = getMetadataIdFromRecordType(recordType);
		return termCollector.collectTerms(metadataId, recordRead);
	}

	private String getMetadataIdFromRecordType(String recordType) {
		RecordTypeHandler recordTypeHandler = dependencyProvider.getRecordTypeHandler(recordType);
		return recordTypeHandler.getDefinitionId();
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
