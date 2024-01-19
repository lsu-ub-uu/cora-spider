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

package se.uu.ub.cora.spider.binary.internal;

import java.io.InputStream;
import java.text.MessageFormat;

import se.uu.ub.cora.beefeater.authentication.User;
import se.uu.ub.cora.bookkeeper.recordtype.RecordTypeHandler;
import se.uu.ub.cora.bookkeeper.termcollector.DataGroupTermCollector;
import se.uu.ub.cora.contentanalyzer.ContentAnalyzer;
import se.uu.ub.cora.contentanalyzer.ContentAnalyzerProvider;
import se.uu.ub.cora.data.DataAtomic;
import se.uu.ub.cora.data.DataChild;
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
import se.uu.ub.cora.spider.binary.Uploader;
import se.uu.ub.cora.spider.data.DataMissingException;
import se.uu.ub.cora.spider.dependency.SpiderDependencyProvider;
import se.uu.ub.cora.spider.dependency.SpiderInstanceProvider;
import se.uu.ub.cora.spider.record.MisuseException;
import se.uu.ub.cora.spider.record.RecordUpdater;
import se.uu.ub.cora.spider.resourceconvert.ResourceConvert;
import se.uu.ub.cora.storage.RecordStorage;
import se.uu.ub.cora.storage.archive.ResourceArchive;
import se.uu.ub.cora.storage.archive.ResourceMetadata;
import se.uu.ub.cora.storage.archive.record.ResourceMetadataToUpdate;

public final class UploaderImp implements Uploader {
	private static final String MASTER = "master";
	private static final String BINARY_RECORD_TYPE = "binary";
	// private static final String RESOURCE_INFO = "resourceInfo";
	private static final String MIME_TYPE_GENERIC = "application/octet-stream";
	private SpiderAuthorizator spiderAuthorizator;
	private DataGroupTermCollector termCollector;
	private SpiderDependencyProvider dependencyProvider;
	private ResourceArchive resourceArchive;
	private String id;
	private String resourceType;
	private InputStream resourceStream;
	private Authenticator authenticator;
	private RecordStorage recordStorage;
	private String authToken;
	private String type;
	private User user;
	private ResourceConvert resourceConvert;
	private String dataDivider;
	private MimeTypeToBinaryType mimeTypeToBinaryType;

	private UploaderImp(SpiderDependencyProvider dependencyProvider,
			ResourceConvert resourceConvert, MimeTypeToBinaryType mimeTypeToBinaryType) {
		this.dependencyProvider = dependencyProvider;
		this.resourceConvert = resourceConvert;
		this.mimeTypeToBinaryType = mimeTypeToBinaryType;
		authenticator = dependencyProvider.getAuthenticator();
		spiderAuthorizator = dependencyProvider.getSpiderAuthorizator();
		recordStorage = dependencyProvider.getRecordStorage();
		termCollector = dependencyProvider.getDataGroupTermCollector();
		resourceArchive = dependencyProvider.getResourceArchive();

	}

	public static UploaderImp usingDependencyProviderAndResourceConvertAndMimeTypeToBinaryType(
			SpiderDependencyProvider dependencyProvider, ResourceConvert resourceConvert,
			MimeTypeToBinaryType mimeTypeToBinaryType) {
		return new UploaderImp(dependencyProvider, resourceConvert, mimeTypeToBinaryType);
	}

	// IF ANY UPDATE to binary record a SuperUser should be used, not the user sent throug the
	// upload.
	// TODO: If the given user is not used to update the binary record, how do we log the
	// uploading of the resource by that user?
	@Override
	public DataRecord upload(String authToken, String type, String id, InputStream resourceStream,
			String resourceType) {
		this.authToken = authToken;
		this.type = type;
		this.id = id;
		this.resourceStream = resourceStream;
		this.resourceType = resourceType;

		tryToGetUserForToken();
		validateInputIsBinaryMasterAndHasStream();
		DataRecordGroup dataRecordGroup = recordStorage.read(type, id);
		dataDivider = dataRecordGroup.getDataDivider();
		tryToCheckUserIsAuthorisedToUploadData(dataRecordGroup);

		return uploadAnalyzeStoreAndCallConvert(resourceStream, dataRecordGroup);
	}

	private DataRecord uploadAnalyzeStoreAndCallConvert(InputStream resourceStream,
			DataRecordGroup dataRecordGroup) {

		storeResourceStreamInArchive(resourceStream);

		String detectedMimeType = detectMimeTypeFromResourceInArchive(dataDivider);

		String originalFileName = dataRecordGroup
				.getFirstAtomicValueWithNameInData("originalFileName");
		updateOriginalFileNameAndMimeTypeInArchive(originalFileName, detectedMimeType);

		// uppdatera posten i storage
		// skicka till analys och konvertering

		DataRecord updatedRecord = updateRecordInStorageUsingCalculatedAndInfoFromArchive(
				dataRecordGroup, detectedMimeType);

		// send message for "Read metadata and convert to small formats"
		possiblySendToConvert(detectedMimeType);
		return updatedRecord;
	}

	private void possiblySendToConvert(String detectedMimeType) {
		if (isImage(detectedMimeType)) {
			resourceConvert.sendMessageForAnalyzingAndConvertingImages(dataDivider, type, id,
					detectedMimeType);
		}
		if (isPdf(detectedMimeType)) {
			resourceConvert.sendMessageToConvertPdfToThumbnails(dataDivider, type, id);
		}
	}

	private boolean isPdf(String detectedMimeType) {
		return "application/pdf".equals(detectedMimeType);
	}

	private boolean isImage(String detectedMimeType) {
		return detectedMimeType.startsWith("image/");
	}

	private void storeResourceStreamInArchive(InputStream resourceStream) {
		resourceArchive.createMasterResource(dataDivider, type, id, resourceStream, MIME_TYPE_GENERIC);
	}

	private void updateOriginalFileNameAndMimeTypeInArchive(String originalFileName,
			String detectedMimeType) {
		ResourceMetadataToUpdate resourceMetadataToUpdate = new ResourceMetadataToUpdate(
				originalFileName, detectedMimeType);
		resourceArchive.updateMasterResourceMetadata(dataDivider, type, id, resourceMetadataToUpdate);
	}

	private void validateInputIsBinaryMasterAndHasStream() {
		ensureBinaryType();
		ensureResourceTypeIsMaster();
		ensureResourceStreamExists();
	}

	private DataRecord updateRecordInStorageUsingCalculatedAndInfoFromArchive(
			DataRecordGroup dataRecordGroup, String detectedMimeType) {

		ResourceMetadata resourceMetadata = resourceArchive.readMasterResourceMetadata(dataDivider, type, id);
		createMasterGroupMoveOriginalFileNameAndAddToBinaryRecord(dataRecordGroup, resourceMetadata,
				detectedMimeType);

		removeExpectedAtomicsFromBinaryRecord(dataRecordGroup);

		return updateRecord(dataRecordGroup);
	}

	private DataRecord updateRecord(DataRecordGroup dataRecordGroup) {
		RecordUpdater recordUpdater = SpiderInstanceProvider.getRecordUpdater();
		DataGroup binaryDataGroup = DataProvider.createGroupFromRecordGroup(dataRecordGroup);
		return recordUpdater.updateRecord(authToken, type, id, binaryDataGroup);
	}

	private String detectMimeTypeFromResourceInArchive(String dataDivider) {
		InputStream resourceFromArchive = resourceArchive.readMasterResource(dataDivider, type, id);
		ContentAnalyzer contentAnalyzer = ContentAnalyzerProvider.getContentAnalyzer();
		return contentAnalyzer.getMimeType(resourceFromArchive);
	}

	private void ensureBinaryType() {
		if (!BINARY_RECORD_TYPE.equals(type)) {
			throw new MisuseException(MessageFormat.format("Uploading error: Invalid record type, "
					+ "for type {0} and {1}, must be (binary).", type, id));
		}
	}

	private void ensureResourceTypeIsMaster() {
		if (!MASTER.equals(resourceType)) {
			throw new MisuseException("Only master can be uploaded.");
		}
	}

	private void ensureResourceStreamExists() {
		if (null == resourceStream) {
			throw new DataMissingException(

					MessageFormat.format(
							"Uploading error: Nothing to "
									+ "upload, resource stream is missing for type {0} and id {1}.",
							type, id));
		}
	}

	protected void tryToGetUserForToken() {
		try {
			user = authenticator.getUserForToken(authToken);
		} catch (Exception e) {
			throw new AuthenticationException(MessageFormat.format(
					"Uploading error: Not possible to upload "
							+ "resource due the user could not be authenticated, for type {0} and id {1}.",
					type, id), e);
		}
	}

	private void tryToCheckUserIsAuthorisedToUploadData(DataRecordGroup dataRecord) {
		try {
			checkUserIsAuthorisedToUploadData(dataRecord);
		} catch (Exception e) {
			throw new AuthorizationException(MessageFormat.format(
					"Uploading error: Not possible to upload "
							+ "resource due the user could not be authorizated, for type {0} and id {1}.",
					type, id), e);
		}
	}

	private void checkUserIsAuthorisedToUploadData(DataRecordGroup dataRecord) {
		CollectTerms collectedTerms = getCollectedTermsForRecord(dataRecord);
		spiderAuthorizator.checkUserIsAuthorizedForActionOnRecordTypeAndCollectedData(user,
				"upload", type, collectedTerms.permissionTerms);
	}

	private CollectTerms getCollectedTermsForRecord(DataRecordGroup dataRecordGroup) {
		DataGroup binaryDG = DataProvider.createGroupFromRecordGroup(dataRecordGroup);
		RecordTypeHandler recordTypeHandler = dependencyProvider
				.getRecordTypeHandlerUsingDataRecordGroup(dataRecordGroup);
		String definitionId = recordTypeHandler.getDefinitionId();
		return termCollector.collectTerms(definitionId, binaryDG);
	}

	private void createMasterGroupMoveOriginalFileNameAndAddToBinaryRecord(
			DataRecordGroup dataRecordGroup, ResourceMetadata resourceMetadata,
			String detectedMimeType) {
		DataGroup masterGroup = createMasterGroup(resourceMetadata.fileSize(), detectedMimeType);

		dataRecordGroup.addChild(masterGroup);

		DataAtomic checksum = DataProvider.createAtomicUsingNameInDataAndValue("checksum",
				resourceMetadata.checksumSHA512());
		DataAtomic checksumType = DataProvider.createAtomicUsingNameInDataAndValue("checksumType",
				"SHA-512");
		masterGroup.addChild(checksum);
		masterGroup.addChild(checksumType);

		DataChild originalFileName = dataRecordGroup
				.getFirstChildWithNameInData("originalFileName");
		masterGroup.addChild(originalFileName);
		dataRecordGroup.removeFirstChildWithNameInData("originalFileName");

		dataRecordGroup.addAttributeByIdWithValue("type",
				mimeTypeToBinaryType.toBinaryType(detectedMimeType));
	}

	private DataGroup createMasterGroup(String fetchedFileSize, String detectedMimeType) {
		DataGroup master = DataProvider.createGroupUsingNameInData(MASTER);

		DataAtomic resourceId = DataProvider.createAtomicUsingNameInDataAndValue("resourceId",
				id + "-master");
		DataResourceLink resourceLink = DataProvider
				.createResourceLinkUsingNameInDataAndMimeType(MASTER, detectedMimeType);
		DataAtomic fileSize = DataProvider.createAtomicUsingNameInDataAndValue("fileSize",
				fetchedFileSize);
		DataAtomic mimeType = DataProvider.createAtomicUsingNameInDataAndValue("mimeType",
				detectedMimeType);

		master.addChild(resourceId);
		master.addChild(resourceLink);
		master.addChild(fileSize);
		master.addChild(mimeType);
		return master;
	}

	private void removeExpectedAtomicsFromBinaryRecord(DataRecordGroup dataRecordGroup) {
		dataRecordGroup.removeFirstChildWithNameInData("expectedFileSize");
		dataRecordGroup.removeFirstChildWithNameInData("expectedChecksum");
	}

	public ResourceConvert onlyForTestGetResourceConvert() {
		return resourceConvert;
	}

	public SpiderDependencyProvider onlyForTestGetDependecyProvider() {
		return dependencyProvider;
	}

	public MimeTypeToBinaryType onlyForTestGetMimeTypeToBinaryTypeConvert() {
		return mimeTypeToBinaryType;
	}

}
