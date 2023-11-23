/*
 * Copyright 2016, 2023 Uppsala University Library
 * Copyright 2016 Olov McKie
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

import se.uu.ub.cora.beefeater.authentication.User;
import se.uu.ub.cora.bookkeeper.recordtype.RecordTypeHandler;
import se.uu.ub.cora.bookkeeper.termcollector.DataGroupTermCollector;
import se.uu.ub.cora.contentanalyzer.ContentAnalyzer;
import se.uu.ub.cora.contentanalyzer.ContentAnalyzerProvider;
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
import se.uu.ub.cora.spider.resourceconvert.ResourceConvert;
import se.uu.ub.cora.storage.RecordStorage;
import se.uu.ub.cora.storage.archive.ResourceArchive;
import se.uu.ub.cora.storage.archive.ResourceMetadata;
import se.uu.ub.cora.storage.archive.record.ResourceMetadataToUpdate;

public final class UploaderImp implements Uploader {
	private static final String BINARY_RECORD_TYPE = "binary";
	private static final String RESOURCE_INFO = "resourceInfo";
	private static final String MIME_TYPE_GENERIC = "application/octet-stream";
	private SpiderAuthorizator spiderAuthorizator;
	private DataGroupTermCollector termCollector;
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
	private String resourceType;
	private InputStream resourceStream;
	private Authenticator authenticator;
	private RecordStorage recordStorage;
	private String authToken;
	private String type;
	private User user;
	private DataRecordGroup binaryDataRecordGroup;
	private ResourceConvert resourceConvert;
	private String detectedMimeType;

	private UploaderImp(SpiderDependencyProvider dependencyProvider,
			ResourceConvert resourceConvert) {
		this.dependencyProvider = dependencyProvider;
		this.resourceConvert = resourceConvert;
		authenticator = dependencyProvider.getAuthenticator();
		spiderAuthorizator = dependencyProvider.getSpiderAuthorizator();
		recordStorage = dependencyProvider.getRecordStorage();
		termCollector = dependencyProvider.getDataGroupTermCollector();
		resourceArchive = dependencyProvider.getResourceArchive();

	}

	public static UploaderImp usingDependencyProviderAndResourceConvert(
			SpiderDependencyProvider dependencyProvider, ResourceConvert resourceConvert) {
		return new UploaderImp(dependencyProvider, resourceConvert);
	}

	// IF ANY UPDATE to binary record a SuperUser should be used, not the user sent throug the
	// upload.
	// TODO: If the given user is not used to update the binary record, how do we log the
	// uploading of the resource by that user?
	@Override
	public DataRecord upload(String authToken, String type, String id, InputStream resourceStream,
			String resourceType) {
		setFieldVariables(authToken, type, id, resourceStream, resourceType);
		validateInputAndUser(type, id);
		storeResourceStreamInArchive(type, id, resourceStream);
		// dependencyProvider.getInitInfoValueUsingKey("imageConverterHost")
		// dependencyProvider.getInitInfoValueUsingKey("imageConverterPort")

		DataRecord updatedRecord = updateMetadataInArchiveAndStorage(authToken, type, id);

		// send message for "Read metadata and convert to small formats"
		sendImageToAnalyzeAndConvert(type, id);
		return updatedRecord;
	}

	private void sendImageToAnalyzeAndConvert(String type, String id) {
		if (detectedMimeType.startsWith("image/")) {
			String dataDivider = binaryDataRecordGroup.getDataDivider();
			resourceConvert.sendMessageForAnalyzeAndConvertToThumbnails(dataDivider, type, id);
		}
	}

	private void validateInputAndUser(String type, String id) {
		validateInputIsBinaryMasterAndHasStream();
		tryToGetUserForToken();
		binaryDataRecordGroup = recordStorage.read(type, id);
		tryToCheckUserIsAuthorisedToUploadData(binaryDataRecordGroup);
	}

	private void storeResourceStreamInArchive(String type, String id, InputStream resourceStream) {
		String dataDivider = binaryDataRecordGroup.getDataDivider();
		resourceArchive.create(dataDivider, type, id, resourceStream, MIME_TYPE_GENERIC);
	}

	private DataRecord updateMetadataInArchiveAndStorage(String authToken, String type, String id) {
		String dataDivider = binaryDataRecordGroup.getDataDivider();
		ResourceMetadata resourceMetadata = resourceArchive.readMetadata(dataDivider, type, id);

		detectedMimeType = detectMimeType(type, id, dataDivider);

		String originalFileName = binaryDataRecordGroup
				.getFirstAtomicValueWithNameInData("originalFileName");

		updateResourceMetadata(type, id, dataDivider, detectedMimeType, originalFileName);

		updateBinaryRecord(detectedMimeType, resourceMetadata);
		return updateBinaryRecordInStorage(authToken, type, id);
	}

	private void updateResourceMetadata(String type, String id, String dataDivider,
			String detectedMimeType, String originalFileName) {
		ResourceMetadataToUpdate resourceMetadataToUpdate = new ResourceMetadataToUpdate(
				originalFileName, detectedMimeType);
		resourceArchive.updateMetadata(dataDivider, type, id, resourceMetadataToUpdate);
	}

	private void setFieldVariables(String authToken, String type, String id,
			InputStream resourceStream, String resourceType) {
		this.authToken = authToken;
		this.type = type;
		this.id = id;
		this.resourceStream = resourceStream;
		this.resourceType = resourceType;
	}

	private void validateInputIsBinaryMasterAndHasStream() {
		ensureBinaryType();
		ensureResourceTypeIsMaster();
		ensureResourceStreamExists();
	}

	private DataRecord updateBinaryRecordInStorage(String authToken, String type, String id) {
		RecordUpdater recordUpdater = SpiderInstanceProvider.getRecordUpdater();

		DataGroup binaryDataGroup = DataProvider.createGroupFromRecordGroup(binaryDataRecordGroup);

		return recordUpdater.updateRecord(authToken, type, id, binaryDataGroup);
	}

	private void updateBinaryRecord(String detectedMimeType, ResourceMetadata resourceMetadata) {
		createResourceInfoAndMasterGroupAndAddedToBinaryRecord(resourceMetadata.fileSize(),
				resourceMetadata.checksumSHA512(), detectedMimeType);
		removeExpectedAtomicsFromBinaryRecord();
	}

	private String detectMimeType(String type, String id, String dataDivider) {
		InputStream resourceFromArchive = resourceArchive.read(dataDivider, type, id);
		ContentAnalyzer contentAnalyzer = ContentAnalyzerProvider.getContentAnalyzer();
		return contentAnalyzer.getMimeType(resourceFromArchive);
	}

	private void ensureBinaryType() {
		if (!BINARY_RECORD_TYPE.equals(type)) {
			throw new MisuseException(MessageFormat.format(ERR_MESSAGE_MISUSE, type, id));
		}
	}

	private void ensureResourceTypeIsMaster() {
		if (!"master".equals(resourceType)) {
			throw new MisuseException(
					"Not implemented yet for resource type different than master.");
		}
	}

	private void ensureResourceStreamExists() {
		if (null == resourceStream) {
			throw new DataMissingException(
					MessageFormat.format(ERR_MESAGE_MISSING_RESOURCE_STREAM, type, id));
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

	private void tryToCheckUserIsAuthorisedToUploadData(DataRecordGroup dataRecord) {
		try {
			checkUserIsAuthorisedToUploadData(dataRecord);
		} catch (Exception e) {
			throw new AuthorizationException(MessageFormat.format(ERR_MSG_AUTHORIZATION, type, id),
					e);
		}
	}

	private void checkUserIsAuthorisedToUploadData(DataRecordGroup dataRecord) {
		CollectTerms collectedTerms = getCollectedTermsForRecord(dataRecord);
		spiderAuthorizator.checkUserIsAuthorizedForActionOnRecordTypeAndCollectedData(user,
				"upload", type, collectedTerms.permissionTerms);
	}

	private CollectTerms getCollectedTermsForRecord(DataRecordGroup dataRecord) {
		DataGroup binaryDG = DataProvider.createGroupFromRecordGroup(dataRecord);
		RecordTypeHandler recordTypeHandler = dependencyProvider
				.getRecordTypeHandlerUsingDataRecordGroup(binaryDataRecordGroup);
		String definitionId = recordTypeHandler.getDefinitionId();
		return termCollector.collectTerms(definitionId, binaryDG);
	}

	private void createResourceInfoAndMasterGroupAndAddedToBinaryRecord(String fetchedFileSize,
			String fetchedChecksum, String detectedMimeType) {
		DataGroup resourceInfo = DataProvider.createGroupUsingNameInData(RESOURCE_INFO);
		DataGroup master = DataProvider.createGroupUsingNameInData(resourceType);

		DataAtomic resourceId = DataProvider.createAtomicUsingNameInDataAndValue("resourceId", id);
		DataResourceLink resourceLink = DataProvider
				.createResourceLinkUsingNameInDataAndMimeType("master", detectedMimeType);
		DataAtomic fileSize = DataProvider.createAtomicUsingNameInDataAndValue("fileSize",
				fetchedFileSize);
		DataAtomic mimeType = DataProvider.createAtomicUsingNameInDataAndValue("mimeType",
				detectedMimeType);
		DataAtomic checksum = DataProvider.createAtomicUsingNameInDataAndValue("checksum",
				fetchedChecksum);
		DataAtomic checksumType = DataProvider.createAtomicUsingNameInDataAndValue("checksumType",
				"SHA-512");

		binaryDataRecordGroup.addChild(resourceInfo);
		resourceInfo.addChild(master);

		master.addChild(resourceId);
		master.addChild(resourceLink);
		master.addChild(fileSize);
		master.addChild(mimeType);
		binaryDataRecordGroup.addChild(checksum);
		binaryDataRecordGroup.addChild(checksumType);
	}

	private void removeExpectedAtomicsFromBinaryRecord() {
		binaryDataRecordGroup.removeFirstChildWithNameInData("expectedFileSize");
		binaryDataRecordGroup.removeFirstChildWithNameInData("expectedChecksum");
	}

	public ResourceConvert onlyForTestGetResourceConvert() {
		return resourceConvert;
	}

	public SpiderDependencyProvider onlyForTestGetDependecyProvider() {
		return dependencyProvider;
	}

}
