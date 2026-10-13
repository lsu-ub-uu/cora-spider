/*
 * Copyright 2016, 2023, 2025, 2026 Uppsala University Library
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
import java.util.Optional;

import se.uu.ub.cora.beefeater.authentication.User;
import se.uu.ub.cora.binary.BinaryProvider;
import se.uu.ub.cora.binary.contentanalyzer.ContentAnalyzer;
import se.uu.ub.cora.bookkeeper.recordtype.RecordTypeHandler;
import se.uu.ub.cora.bookkeeper.termcollector.DataGroupTermCollector;
import se.uu.ub.cora.data.DataAtomic;
import se.uu.ub.cora.data.DataChild;
import se.uu.ub.cora.data.DataGroup;
import se.uu.ub.cora.data.DataProvider;
import se.uu.ub.cora.data.DataRecord;
import se.uu.ub.cora.data.DataRecordGroup;
import se.uu.ub.cora.data.DataResourceLink;
import se.uu.ub.cora.data.collected.CollectTerms;
import se.uu.ub.cora.logger.Logger;
import se.uu.ub.cora.logger.LoggerProvider;
import se.uu.ub.cora.spider.authentication.AuthenticationException;
import se.uu.ub.cora.spider.authentication.Authenticator;
import se.uu.ub.cora.spider.authorization.AuthorizationException;
import se.uu.ub.cora.spider.authorization.SpiderAuthorizator;
import se.uu.ub.cora.spider.binary.ArchiveDataIntergrityException;
import se.uu.ub.cora.spider.binary.Uploader;
import se.uu.ub.cora.spider.data.DataMissingException;
import se.uu.ub.cora.spider.dependency.SpiderDependencyProvider;
import se.uu.ub.cora.spider.dependency.SpiderInstanceProvider;
import se.uu.ub.cora.spider.record.DataException;
import se.uu.ub.cora.spider.record.MisuseException;
import se.uu.ub.cora.spider.record.RecordReader;
import se.uu.ub.cora.spider.record.RecordUpdater;
import se.uu.ub.cora.spider.resourceconvert.ResourceConvert;
import se.uu.ub.cora.storage.RecordStorage;
import se.uu.ub.cora.storage.archive.ResourceArchive;
import se.uu.ub.cora.storage.archive.ResourceMetadata;
import se.uu.ub.cora.storage.archive.record.ResourceMetadataToUpdate;

public final class UploaderImp implements Uploader {
	private static final String STATUS = "status";
	private static final String ORIGINAL_FILE_NAME = "originalFileName";
	private static final String EXPECTED_CHECKSUM = "expectedChecksum";
	private static final String EXPECTED_FILE_SIZE = "expectedFileSize";
	private static final String MASTER = "master";
	private static final String BINARY_RECORD_TYPE = "binary";
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
	private RecordTypeHandler recordTypeHandler;
	private Logger logger = LoggerProvider.getLoggerForClass(null);

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
		recordTypeHandler = dependencyProvider
				.getRecordTypeHandlerUsingDataRecordGroup(dataRecordGroup);

		tryToCheckUserIsAuthorisedToUploadData(dataRecordGroup);
		// TODO: Surround with Cora exception
		storeBinaryToArchive(resourceStream);

		return analyzeAndProcessBinary(dataRecordGroup);
	}

	protected void tryToGetUserForToken() {
		try {
			user = authenticator.getUserForToken(authToken);
		} catch (Exception e) {
			throw new AuthenticationException(
					MessageFormat.format("Uploading error: Not possible to upload "
							+ "resource due the user could not be authenticated, for type {0} "
							+ "and id {1}.", type, id),
					e);
		}
	}

	private void validateInputIsBinaryMasterAndHasStream() {
		ensureBinaryType();
		ensureResourceTypeIsMaster();
		ensureResourceStreamExists();
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
			throw new DataMissingException(MessageFormat.format(
					"Uploading error: Nothing to upload, resource stream is missing for type {0}"
							+ " and id {1}.",
					type, id));
		}
	}

	private void tryToCheckUserIsAuthorisedToUploadData(DataRecordGroup dataRecord) {
		checkUserIsAuthorizedIfRecorTypeUsesPermissionUnits(dataRecord);
		try {
			checkUserIsAuthorisedToUploadData(dataRecord);
		} catch (Exception e) {
			throw new AuthorizationException(
					MessageFormat.format("Uploading error: Not possible to upload "
							+ "resource due the user could not be authorizated, for type {0} and"
							+ " id {1}.", type, id),
					e);
		}
	}

	private void checkUserIsAuthorizedIfRecorTypeUsesPermissionUnits(
			DataRecordGroup dataRecordGroup) {
		if (recordTypeHandler.usePermissionUnit()) {
			checkUserIsAuthorizedToUploadForPemissionUnit(dataRecordGroup);
		}
	}

	private void checkUserIsAuthorizedToUploadForPemissionUnit(DataRecordGroup dataRecordGroup) {
		Optional<String> oPermissionUnit = dataRecordGroup.getPermissionUnit();
		if (oPermissionUnit.isEmpty()) {
			throw notAuthorizedDueToMissingPermissionUnitInRecord();
		}
		tryToCheckUserIsAuthorizedForPemissionUnit(oPermissionUnit.get());
	}

	private AuthorizationException notAuthorizedDueToMissingPermissionUnitInRecord() {
		return new AuthorizationException(
				MessageFormat.format("Uploading error: Not possible to upload "
						+ "resource due the binary not having any permission unit, for type {0} and"
						+ " id {1}.", type, id));
	}

	private void tryToCheckUserIsAuthorizedForPemissionUnit(String permissionUnit) {
		try {
			spiderAuthorizator.checkUserIsAuthorizedForPemissionUnit(user, permissionUnit);
		} catch (Exception _) {
			throw notAuthorizedDueToUserDoNotMatchRecordsPermissionUnit();
		}
	}

	private AuthorizationException notAuthorizedDueToUserDoNotMatchRecordsPermissionUnit() {
		return new AuthorizationException(
				MessageFormat.format("Uploading error: Not possible to upload "
						+ "resource due the user not having required permission unit, for type {0} and"
						+ " id {1}.", type, id));
	}

	private void checkUserIsAuthorisedToUploadData(DataRecordGroup dataRecord) {
		CollectTerms collectedTerms = getCollectedTermsForRecord(dataRecord);
		spiderAuthorizator.checkUserIsAuthorizedForActionOnRecordTypeAndCollectedData(user,
				"upload", type, collectedTerms.permissionTerms);
	}

	private CollectTerms getCollectedTermsForRecord(DataRecordGroup dataRecordGroup) {
		String definitionId = recordTypeHandler.getDefinitionId();
		return termCollector.collectTerms(definitionId, dataRecordGroup);
	}

	private DataRecord analyzeAndProcessBinary(DataRecordGroup dataRecordGroup) {
		verifyArchiveDataIntegrity(dataRecordGroup);
		String detectedMimeType = tryToDetectMimeType();
		tryToUpdateRecordAccordingUploadedBinary(dataRecordGroup, detectedMimeType);

		try {
			possiblySendToConvert(detectedMimeType); // -> Failed
		} catch (Exception t) {
			// setStatus(dataRecordGroup, "failed");
			// addProcessingMessage(dataRecordGroup, t.getMessage());
			// updateRecord(dataRecordGroup);
			// throw something
		}

		try {
			return readRecord(); // Do not know what to do.
		} catch (Exception e) {
			throw new DataException("Something went wrong while processing the binary.", e);
		}
	}

	private void verifyArchiveDataIntegrity(DataRecordGroup dataRecordGroup) {
		ResourceMetadata resourceMetadata = resourceArchive.readMasterResourceMetadata(dataDivider,
				type, id);
		possiblyVerifyExpectedFileSize(dataRecordGroup, resourceMetadata);
		possiblyVerifyExpectedChecksum(dataRecordGroup, resourceMetadata);
	}

	private void possiblyVerifyExpectedFileSize(DataRecordGroup dataRecordGroup,
			ResourceMetadata resourceMetadata) {
		if (dataRecordGroup.containsChildWithNameInData(EXPECTED_FILE_SIZE)) {
			verifyExpectedFileSize(dataRecordGroup, resourceMetadata);
		}
	}

	private void verifyExpectedFileSize(DataRecordGroup dataRecordGroup,
			ResourceMetadata resourceMetadata) {
		String expectedFileSize = dataRecordGroup
				.getFirstAtomicValueWithNameInData(EXPECTED_FILE_SIZE);
		String archiveFileSize = resourceMetadata.fileSize();
		if (!expectedFileSize.equals(archiveFileSize)) {
			removeUploadedBinaryAndThowException("file size", expectedFileSize, archiveFileSize);
		}
	}

	private void removeUploadedBinaryAndThowException(String verificationType, String expectedVaule,
			String actualValue) {
		resourceArchive.delete(dataDivider, type, id);
		String verificationMessage = MessageFormat.format(
				"The {0} verification of uploaded data failed: the actual value was: {1}"
						+ " but the expected value was: {2}.",
				verificationType, actualValue, expectedVaule);
		throw ArchiveDataIntergrityException.withMessage(verificationMessage);
	}

	private void possiblyVerifyExpectedChecksum(DataRecordGroup dataRecordGroup,
			ResourceMetadata resourceMetadata) {
		if (dataRecordGroup.containsChildWithNameInData(EXPECTED_CHECKSUM)) {
			verifyExpectedChecksum(dataRecordGroup, resourceMetadata);
		}
	}

	private void verifyExpectedChecksum(DataRecordGroup dataRecordGroup,
			ResourceMetadata resourceMetadata) {
		String expectedChecksum = dataRecordGroup
				.getFirstAtomicValueWithNameInData(EXPECTED_CHECKSUM);
		String archiveChecksum = resourceMetadata.checksumSHA512();
		if (!expectedChecksum.equals(archiveChecksum)) {
			removeUploadedBinaryAndThowException("checksum", expectedChecksum, archiveChecksum);
		}
	}

	private void tryToUpdateRecordAccordingUploadedBinary(DataRecordGroup dataRecordGroup,
			String detectedMimeType) {
		try {
			updateRecordAccordingUploadedBinary(dataRecordGroup, detectedMimeType);
		} catch (Exception e) {
			resourceArchive.delete(dataDivider, type, id);

			logErrorWithMessageToFormat("Metadata could not be updated for binary record, with "
					+ "dataDivider: %s, type: %s and id: %s.", e);

			String exceptionMessage = "The metadata for the updated file could not be stored in the system. "
					+ "Please try again later. If the problem persists, contact support.";
			throw ArchiveDataIntergrityException.withMessage(exceptionMessage);
		}
	}

	private void logErrorWithMessageToFormat(String internalMessage, Exception e) {
		String formattedInternalMessage = String.format(internalMessage, dataDivider, type, id);
		logger.logErrorUsingMessageAndException(formattedInternalMessage, e);
	}

	private void updateRecordAccordingUploadedBinary(DataRecordGroup dataRecordGroup,
			String detectedMimeType) {
		updateOriginalFileNameAndMimeTypeInArchive(dataRecordGroup, detectedMimeType);
		updateRecordInStorageUsingCalculatedAndInfoFromArchive(dataRecordGroup, detectedMimeType);
	}

	private void storeBinaryToArchive(InputStream resourceStream) {
		resourceArchive.createMasterResource(dataDivider, type, id, resourceStream,
				MIME_TYPE_GENERIC);
	}

	private String tryToDetectMimeType() {
		try {
			return detectMimeTypeFromResourceInArchive(dataDivider);
		} catch (Exception e) {
			resourceArchive.delete(dataDivider, type, id);
			logErrorWithMessageToFormat("Could not determine the mimeType of the file for "
					+ "binary record, with dataDivider: %s, type: %s and id: %s.", e);
			throw throwExceptionForFailedMimeTypeDetection(e);
		}
	}

	private ArchiveDataIntergrityException throwExceptionForFailedMimeTypeDetection(Exception e) {
		String message = "The mimeType of the file could not determined. Make sure the file is"
				+ " readable and has a known mimeType, and try to upload again.";
		return ArchiveDataIntergrityException.withMessageAndException(message, e);
	}

	private void setFailedStatusAndAddProcessingMessageToRecordAndStore(
			DataRecordGroup dataRecordGroup, String verificationMessage) {
		setStatus(dataRecordGroup, "failed");
		addProcessingMessage(dataRecordGroup, verificationMessage);
		updateRecord(dataRecordGroup);
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

	private void updateOriginalFileNameAndMimeTypeInArchive(DataRecordGroup dataRecordGroup,
			String detectedMimeType) {
		String originalFileName = dataRecordGroup
				.getFirstAtomicValueWithNameInData(ORIGINAL_FILE_NAME);

		resourceArchive.updateMasterResourceMetadata(dataDivider, type, id,
				new ResourceMetadataToUpdate(originalFileName, detectedMimeType));
	}

	private void updateRecordInStorageUsingCalculatedAndInfoFromArchive(
			DataRecordGroup dataRecordGroup, String detectedMimeType) {
		removeExpectedAtomicsFromBinaryRecord(dataRecordGroup);
		setCorrectStatusForMediaType(dataRecordGroup, detectedMimeType);
		createMasterGroupMoveOriginalFileNameAndAddToBinaryRecord(dataRecordGroup,
				detectedMimeType);
		updateRecord(dataRecordGroup);
	}

	private void setCorrectStatusForMediaType(DataRecordGroup dataRecordGroup,
			String detectedMimeType) {
		if (isImage(detectedMimeType) || isPdf(detectedMimeType)) {
			setStatus(dataRecordGroup, "uploaded");
		} else {
			setStatus(dataRecordGroup, "done");
		}
	}

	private void setStatus(DataRecordGroup dataRecordGroup, String status) {
		DataGroup recordInfo = dataRecordGroup.getFirstChildOfTypeAndName(DataGroup.class,
				"recordInfo");
		recordInfo.removeFirstChildWithTypeAndName(DataAtomic.class, STATUS);
		DataAtomic statusUploaded = DataProvider.createAtomicUsingNameInDataAndValue(STATUS,
				status);
		recordInfo.addChild(statusUploaded);
	}

	private void addProcessingMessage(DataRecordGroup dataRecordGroup, String message) {
		DataGroup recordInfo = dataRecordGroup.getFirstChildOfTypeAndName(DataGroup.class,
				"recordInfo");
		DataAtomic processingMessage = DataProvider
				.createAtomicUsingNameInDataAndValue("processingMessage", message);
		recordInfo.addChild(processingMessage);
	}

	private void updateRecord(DataRecordGroup dataRecordGroup) {
		RecordUpdater recordUpdater = SpiderInstanceProvider.getRecordUpdater();
		recordUpdater.internalUpdateRecord(dataRecordGroup, user.id);
	}

	private DataRecord readRecord() {
		RecordReader recordReader = SpiderInstanceProvider.getRecordReader();
		return recordReader.readRecord(authToken, BINARY_RECORD_TYPE, id);
	}

	private String detectMimeTypeFromResourceInArchive(String dataDivider) {
		InputStream resourceFromArchive = resourceArchive.readMasterResource(dataDivider, type, id);
		ContentAnalyzer contentAnalyzer = BinaryProvider.getContentAnalyzer();
		return contentAnalyzer.getMimeType(resourceFromArchive);
	}

	private void createMasterGroupMoveOriginalFileNameAndAddToBinaryRecord(
			DataRecordGroup dataRecordGroup, String detectedMimeType) {
		ResourceMetadata resourceMetadata = resourceArchive.readMasterResourceMetadata(dataDivider,
				type, id);
		DataGroup masterGroup = createMasterGroup(resourceMetadata.fileSize(), detectedMimeType);

		dataRecordGroup.addChild(masterGroup);

		DataAtomic checksum = DataProvider.createAtomicUsingNameInDataAndValue("checksum",
				resourceMetadata.checksumSHA512());
		DataAtomic checksumType = DataProvider.createAtomicUsingNameInDataAndValue("checksumType",
				"SHA-512");
		masterGroup.addChild(checksum);
		masterGroup.addChild(checksumType);

		DataChild originalFileName = dataRecordGroup
				.getFirstChildWithNameInData(ORIGINAL_FILE_NAME);
		masterGroup.addChild(originalFileName);
		dataRecordGroup.removeFirstChildWithNameInData(ORIGINAL_FILE_NAME);

		dataRecordGroup.addAttributeByIdWithValue("type",
				mimeTypeToBinaryType.toBinaryType(detectedMimeType));
	}

	private DataGroup createMasterGroup(String fetchedFileSize, String detectedMimeType) {
		DataGroup master = DataProvider.createGroupUsingNameInData(MASTER);

		DataAtomic resourceId = DataProvider.createAtomicUsingNameInDataAndValue("resourceId",
				id + "-master");
		DataResourceLink resourceLink = DataProvider
				.createResourceLinkUsingNameInDataAndTypeAndIdAndMimeType(MASTER, type, id,
						detectedMimeType);
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
		dataRecordGroup.removeFirstChildWithNameInData(EXPECTED_FILE_SIZE);
		dataRecordGroup.removeFirstChildWithNameInData(EXPECTED_CHECKSUM);
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
