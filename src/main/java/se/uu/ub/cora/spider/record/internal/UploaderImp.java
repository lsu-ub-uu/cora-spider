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
import se.uu.ub.cora.bookkeeper.recordtype.RecordTypeHandler;
import se.uu.ub.cora.bookkeeper.termcollector.DataGroupTermCollector;
import se.uu.ub.cora.data.DataAtomic;
import se.uu.ub.cora.data.DataGroup;
import se.uu.ub.cora.data.DataProvider;
import se.uu.ub.cora.data.DataRecord;
import se.uu.ub.cora.data.DataRecordGroup;
import se.uu.ub.cora.data.DataResourceLink;
import se.uu.ub.cora.data.collected.CollectTerms;
import se.uu.ub.cora.spider.authentication.AuthenticationException;
import se.uu.ub.cora.spider.authentication.Authenticator;
import se.uu.ub.cora.spider.authorization.AuthorizationException;
import se.uu.ub.cora.spider.authorization.SpiderAuthorizator;
import se.uu.ub.cora.spider.data.DataMissingException;
import se.uu.ub.cora.spider.dependency.SpiderDependencyProvider;
import se.uu.ub.cora.spider.dependency.SpiderInstanceProvider;
import se.uu.ub.cora.spider.record.MisuseException;
import se.uu.ub.cora.spider.record.RecordUpdater;
import se.uu.ub.cora.spider.record.Uploader;
import se.uu.ub.cora.storage.RecordStorage;
import se.uu.ub.cora.storage.archive.ResourceArchive;

//Kolla om vi ska behålla SpiderBinary och vad egentligen vi behöver ha i den.
public final class UploaderImp implements Uploader {
	// private static final String FAKE_HEIGHT_WIDTH = "0";
	private static final String FAKE_CHECKSUM = "afAF09";
	private static final String BINARY_RECORD_TYPE = "binary";
	private static final String RESOURCE_INFO = "resourceInfo";
	private static final String MIME_TYPE_GENERIC = "application/octet-stream";
	// private static final String MIME_TYPE_JPEG = "image/jpeg";
	private SpiderAuthorizator spiderAuthorizator;
	private DataGroupTermCollector termCollector;
	private DataGroup binaryRecord;
	private SpiderDependencyProvider dependencyProvider;
	private ResourceArchive resourceArchive;
	private static final String ERR_MSG_AUTHENTICATION = "Uploading error: Not possible to upload "
			+ "resource due the user could not be authenticated, for type {0} and id {1}.";
	private static final String ERR_MSG_AUTHORIZATION = "Uploading error: Not possible to upload "
			+ "resource due the user could not be authorizated, for type {0} and id {1}.";
	private static final String ERR_MESSAGE_MISUSE = "Uploading error: Invalid record type, "
			+ "for type {0} and {1}, must be (binary).";
	private static final String ERR_MESAGE_MISSING_RESOURCE_STREAM = "Uploading error: Nothing to "
			+ "upload, resource stream is missing for type {0} and id {1}.";
	private String id;
	private RecordTypeHandler recordTypeHandler;
	private String resourceType;
	private InputStream resourceStream;
	private Authenticator authenticator;
	private RecordStorage recordStorage;
	private String authToken;
	private String type;
	private User user;

	private UploaderImp(SpiderDependencyProvider dependencyProvider) {
		this.dependencyProvider = dependencyProvider;
		authenticator = dependencyProvider.getAuthenticator();
		spiderAuthorizator = dependencyProvider.getSpiderAuthorizator();
		recordStorage = dependencyProvider.getRecordStorage();
		termCollector = dependencyProvider.getDataGroupTermCollector();
		resourceArchive = dependencyProvider.getResourceArchive();
	}

	public static UploaderImp usingDependencyProvider(SpiderDependencyProvider dependencyProvider) {
		return new UploaderImp(dependencyProvider);
	}

	@Override
	public DataRecord upload(String authToken, String type, String id, InputStream resourceStream,
			String resourceType) {
		// OBS: Vi behöver inte skicka filename utanför, den får räknas ut intern.
		// https://cora.epc.ub.uu.se/diva/rest/record/binary/binary:11750059111622259/master
		this.authToken = authToken;
		this.type = type;
		this.id = id;
		this.resourceStream = resourceStream;
		this.resourceType = resourceType;

		validateInput();
		tryToGetUserForToken();

		binaryRecord = recordStorage.read(List.of(type), id);
		DataRecordGroup readDataRecordGroup = DataProvider
				.createRecordGroupFromDataGroup(binaryRecord);
		tryToCheckUserIsAuthorisedToUploadData(readDataRecordGroup, binaryRecord);

		// streamId = idGenerator.getIdForType(type + "Binary");
		// long fileSize = streamStorage.store(streamId, dataDivider, stream);

		// Än så länge vi resurs id kommer att ha samma id som binär posten.
		// Mimetype är nu satt till "image/jpeg" så länge
		String dataDivider = readDataRecordGroup.getDataDivider();
		resourceArchive.create(dataDivider, type, id, resourceStream, MIME_TYPE_GENERIC);
		//
		// // Den här steg kommer inte att behövas. Vi kommer däremot behöva en redactor steg.
		// addOrReplaceResourceInfoToMetdataRecord(fileName, fileSize);
		//

		// Somwhere we must get some information from the file.

		String originalFileName = binaryRecord
				.getFirstAtomicValueWithNameInData("originalFileName");
		String expectedFileSize = binaryRecord
				.getFirstAtomicValueWithNameInData("expectedFileSize");
		String expectedChecksum = FAKE_CHECKSUM;
		if (binaryRecord.containsChildWithNameInData("expectedChecksum")) {
			expectedChecksum = binaryRecord.getFirstAtomicValueWithNameInData("expectedChecksum");
		}

		// IF ANY UPDATE to binary record a SuperUser should be used, not the user sent throug the
		// upload.
		// TODO: If the given user is not used to update the binary record, how do we log the
		// uploading of the resource by that user?

		createResourceInfoAndMasterGroupAndAddedToBinaryRecord(expectedFileSize, expectedChecksum);
		removeExpectedAtomicsFromBinaryRecord();

		RecordUpdater recordUpdater = SpiderInstanceProvider.getRecordUpdater();
		return recordUpdater.updateRecord(authToken, type, id, binaryRecord);
	}

	private void validateInput() {
		ensureBinaryType();
		ensureResourceTypeIsMaster();
		ensureResourceStreamExists(resourceStream);
	}

	private void ensureResourceTypeIsMaster() {
		if (!"master".equals(resourceType)) {
			throw new MisuseException(
					"Not implemented yet for resource type different than master.");
		}
	}

	private void ensureBinaryType() {
		if (!BINARY_RECORD_TYPE.equals(type)) {
			throw new MisuseException(MessageFormat.format(ERR_MESSAGE_MISUSE, type, id));
		}
	}

	protected void tryToGetUserForToken() {
		try {
			user = authenticator.getUserForToken(authToken);
		} catch (Exception e) {
			throw new AuthenticationException(
					MessageFormat.format(ERR_MSG_AUTHENTICATION, type, id), e);
		}
	}

	private void tryToCheckUserIsAuthorisedToUploadData(DataRecordGroup dataRecordGroup,
			DataGroup dataGroup) {
		try {
			checkUserIsAuthorisedToUploadData(dataRecordGroup, dataGroup);
		} catch (Exception e) {
			throw new AuthorizationException(MessageFormat.format(ERR_MSG_AUTHORIZATION, type, id),
					e);
		}
	}

	private void checkUserIsAuthorisedToUploadData(DataRecordGroup recordGroup,
			DataGroup dataGroup) {
		CollectTerms collectedTerms = getCollectedTermsForRecord(recordGroup, dataGroup);
		spiderAuthorizator.checkUserIsAuthorizedForActionOnRecordTypeAndCollectedData(user,
				"upload", type, collectedTerms.permissionTerms);
	}

	private CollectTerms getCollectedTermsForRecord(DataRecordGroup recordGroup,
			DataGroup dataGroup) {
		recordTypeHandler = dependencyProvider
				.getRecordTypeHandlerUsingDataRecordGroup(recordGroup);
		String definitionId = recordTypeHandler.getDefinitionId();
		return termCollector.collectTerms(definitionId, dataGroup);
	}

	private void ensureResourceStreamExists(InputStream inputStream) {
		if (null == inputStream) {
			throw new DataMissingException(
					MessageFormat.format(ERR_MESAGE_MISSING_RESOURCE_STREAM, type, id));
		}
	}

	private void createResourceInfoAndMasterGroupAndAddedToBinaryRecord(String expectedFileSize,
			String expectedChecksum) {
		DataGroup resourceInfo = DataProvider.createGroupUsingNameInData(RESOURCE_INFO);
		DataGroup master = DataProvider.createGroupUsingNameInData(resourceType);

		DataAtomic resourceId = DataProvider.createAtomicUsingNameInDataAndValue("resourceId", id);
		DataResourceLink resourceLink = DataProvider.createResourceLinkUsingNameInData("master");
		DataAtomic fileSize = DataProvider.createAtomicUsingNameInDataAndValue("fileSize",
				expectedFileSize);
		DataAtomic mimeType = DataProvider.createAtomicUsingNameInDataAndValue("mimeType",
				MIME_TYPE_GENERIC);
		// DataAtomic height = DataProvider.createAtomicUsingNameInDataAndValue("height",
		// FAKE_HEIGHT_WIDTH);
		// DataAtomic width = DataProvider.createAtomicUsingNameInDataAndValue("width",
		// FAKE_HEIGHT_WIDTH);
		// DataAtomic resolution = DataProvider.createAtomicUsingNameInDataAndValue("resolution",
		// FAKE_HEIGHT_WIDTH);
		DataAtomic checksum = DataProvider.createAtomicUsingNameInDataAndValue("checksum",
				expectedChecksum);
		DataAtomic checksumType = DataProvider.createAtomicUsingNameInDataAndValue("checksumType",
				"SHA512");

		binaryRecord.addChild(resourceInfo);
		resourceInfo.addChild(master);

		master.addChild(resourceId);
		master.addChild(resourceLink);
		master.addChild(fileSize);
		master.addChild(mimeType);
		// master.addChild(height);
		// master.addChild(width);
		// master.addChild(resolution);
		binaryRecord.addChild(checksum);
		binaryRecord.addChild(checksumType);
	}

	private void removeExpectedAtomicsFromBinaryRecord() {
		binaryRecord.removeFirstChildWithNameInData("expectedFileSize");
		binaryRecord.removeFirstChildWithNameInData("expectedChecksum");
	}

}
