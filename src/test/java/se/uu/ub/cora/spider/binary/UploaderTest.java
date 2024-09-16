/**Copyright 2015,2016,2019,2023 Uppsala University Library**This file is part of Cora.**Cora is free software:you can redistribute it and/or modify*it under the terms of the GNU General Public License as published by*the Free Software Foundation,either version 3 of the License,or*(at your option)any later version.**Cora is distributed in the hope that it will be useful,*but WITHOUT ANY WARRANTY;without even the implied warranty of*MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.See the*GNU General Public License for more details.**You should have received a copy of the GNU General Public License*along with Cora.If not,see<http://www.gnu.org/licenses/>.
*/

package se.uu.ub.cora.spider.binary;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertTrue;
import static org.testng.Assert.fail;

import java.text.MessageFormat;

import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import se.uu.ub.cora.binary.BinaryProvider;
import se.uu.ub.cora.data.DataChild;
import se.uu.ub.cora.data.DataProvider;
import se.uu.ub.cora.data.DataRecord;
import se.uu.ub.cora.data.collected.CollectTerms;
import se.uu.ub.cora.data.spies.DataAtomicSpy;
import se.uu.ub.cora.data.spies.DataFactorySpy;
import se.uu.ub.cora.data.spies.DataGroupSpy;
import se.uu.ub.cora.data.spies.DataRecordGroupSpy;
import se.uu.ub.cora.data.spies.DataResourceLinkSpy;
import se.uu.ub.cora.logger.LoggerFactory;
import se.uu.ub.cora.logger.LoggerProvider;
import se.uu.ub.cora.logger.spies.LoggerFactorySpy;
import se.uu.ub.cora.spider.authentication.AuthenticationException;
import se.uu.ub.cora.spider.authorization.AuthorizationException;
import se.uu.ub.cora.spider.binary.internal.UploaderImp;
import se.uu.ub.cora.spider.data.DataMissingException;
import se.uu.ub.cora.spider.dependency.SpiderInstanceProvider;
import se.uu.ub.cora.spider.dependency.spy.RecordTypeHandlerOldSpy;
import se.uu.ub.cora.spider.record.MisuseException;
import se.uu.ub.cora.spider.record.internal.AuthenticatorSpy;
import se.uu.ub.cora.spider.record.internal.SpiderAuthorizatorSpy;
import se.uu.ub.cora.spider.resourceconvert.spy.ResourceConvertSpy;
import se.uu.ub.cora.spider.spy.ContentAnalyzerInstanceProviderSpy;
import se.uu.ub.cora.spider.spy.ContentAnalyzerSpy;
import se.uu.ub.cora.spider.spy.DataGroupTermCollectorSpy;
import se.uu.ub.cora.spider.spy.MimeTypeToBinaryTypeSpy;
import se.uu.ub.cora.spider.spy.SpiderDependencyProviderSpy;
import se.uu.ub.cora.spider.testspies.RecordUpdaterSpy;
import se.uu.ub.cora.spider.testspies.SpiderInstanceFactorySpy;
import se.uu.ub.cora.storage.archive.record.ResourceMetadataToUpdate;
import se.uu.ub.cora.storage.spies.RecordStorageSpy;
import se.uu.ub.cora.storage.spies.archive.InputStreamSpy;
import se.uu.ub.cora.storage.spies.archive.ResourceArchiveSpy;

public class UploaderTest {
	private static final String SHA512 = "SHA-512";
	private static final String EXPECTED_ORIGINAL_FILE_NAME = "expectedOriginalFileName";
	private static final String EXPECTED_FILE_SIZE = "expectedFileSize";
	private static final String EXPECTED_CHECKSUM = "expectedChecksum";
	private static final String ACTION_UPLOAD = "upload";
	private static final String ERR_MSG_AUTHENTICATION = "Uploading error: Not possible to upload "
			+ "resource due the user could not be authenticated, for type {0} and id {1}.";
	private static final String ERR_MSG_AUTHORIZATION = "Uploading error: Not possible to upload "
			+ "resource due the user could not be authorizated, for type {0} and id {1}.";
	private static final String MIME_TYPE_GENERIC = "application/octet-stream";
	private static final String SOME_RECORD_ID = "someRecordId";
	private static final String SOME_AUTH_TOKEN = "someAuthToken";
	private static final String SOME_RECORD_TYPE = "someRecordType";
	private static final String BINARY_RECORD_TYPE = "binary";
	private static final String SOME_RESOURCE_TYPE = "someResourceType";
	private static final String SOME_EXCEPTION_MESSAGE = "someExceptionMessage";
	private static final String ERR_MESSAGE_MISUSE = "Uploading error: Invalid record type, "
			+ "for type {0} and {1}, must be (binary).";
	private static final String RESOURCE_TYPE_MASTER = "master";
	private static final String ERR_MESAGE_MISSING_RESOURCE_STREAM = "Uploading error: Nothing to "
			+ "upload, resource stream is missing for type {0} and id {1}.";
	private InputStreamSpy someStream;
	private RecordUpdaterSpy recordUpdater;
	private AuthenticatorSpy authenticator;
	private SpiderAuthorizatorSpy authorizator;
	private ResourceArchiveSpy resourceArchive;
	private RecordStorageSpy recordStorage;
	private UploaderImp uploader;
	private DataGroupTermCollectorSpy termCollector;
	private SpiderDependencyProviderSpy dependencyProvider;
	private SpiderInstanceFactorySpy spiderInstanceFactory;
	private DataFactorySpy dataFactorySpy;
	private RecordTypeHandlerOldSpy recordTypeHandler;
	private LoggerFactory loggerFactory;
	private ContentAnalyzerInstanceProviderSpy contentAnalyzeInstanceProviderSpy;
	private ResourceConvertSpy resourceConvert;
	private DataRecordGroupSpy dataRecordGroupSpy;
	private MimeTypeToBinaryTypeSpy mimeTypeToBinaryType;

	@BeforeMethod
	public void beforeMethod() {
		mimeTypeToBinaryType = new MimeTypeToBinaryTypeSpy();

		dependencyProvider = new SpiderDependencyProviderSpy();
		dataFactorySpy = new DataFactorySpy();

		DataProvider.onlyForTestSetDataFactory(dataFactorySpy);
		setUpSpiderInstanceProvider();
		setUpDependencyProvider();

		loggerFactory = new LoggerFactorySpy();
		LoggerProvider.setLoggerFactory(loggerFactory);

		contentAnalyzeInstanceProviderSpy = new ContentAnalyzerInstanceProviderSpy();
		BinaryProvider
				.onlyForTestSetContentAnalyzerInstanceProvider(contentAnalyzeInstanceProviderSpy);

		someStream = new InputStreamSpy();
		resourceConvert = new ResourceConvertSpy();

		uploader = UploaderImp.usingDependencyProviderAndResourceConvertAndMimeTypeToBinaryType(
				dependencyProvider, resourceConvert, mimeTypeToBinaryType);
	}

	private void setUpSpiderInstanceProvider() {
		recordUpdater = new RecordUpdaterSpy();
		spiderInstanceFactory = new SpiderInstanceFactorySpy();
		spiderInstanceFactory.MRV.setDefaultReturnValuesSupplier("factorRecordUpdater",
				() -> recordUpdater);
	}

	private void setUpDependencyProvider() {
		authenticator = new AuthenticatorSpy();
		dependencyProvider.MRV.setDefaultReturnValuesSupplier("getAuthenticator",
				() -> authenticator);

		authorizator = new SpiderAuthorizatorSpy();
		dependencyProvider.MRV.setDefaultReturnValuesSupplier("getSpiderAuthorizator",
				() -> authorizator);

		recordStorage = new RecordStorageSpy();
		dependencyProvider.MRV.setDefaultReturnValuesSupplier("getRecordStorage",
				() -> recordStorage);
		dataRecordGroupSpy = new DataRecordGroupSpy();
		dataRecordGroupSpy.MRV.setDefaultReturnValuesSupplier("getDataDivider",
				() -> "someDataDivider");

		recordStorage.MRV.setDefaultReturnValuesSupplier("read", () -> dataRecordGroupSpy);

		resourceArchive = new ResourceArchiveSpy();
		dependencyProvider.MRV.setDefaultReturnValuesSupplier("getResourceArchive",
				() -> resourceArchive);

		recordTypeHandler = new RecordTypeHandlerOldSpy();
		recordTypeHandler.MRV.setDefaultReturnValuesSupplier("getDefinitionId",
				() -> "someDefintion");
		dependencyProvider.MRV.setDefaultReturnValuesSupplier(
				"getRecordTypeHandlerUsingDataRecordGroup", () -> recordTypeHandler);

		termCollector = new DataGroupTermCollectorSpy();
		dependencyProvider.MRV.setDefaultReturnValuesSupplier("getDataGroupTermCollector",
				() -> termCollector);

		SpiderInstanceProvider.setSpiderInstanceFactory(spiderInstanceFactory);
	}

	@Test
	public void testUploadStoreResourceIntoArchive() throws Exception {
		DataRecord binaryRecord = uploader.upload(SOME_AUTH_TOKEN, BINARY_RECORD_TYPE,
				SOME_RECORD_ID, someStream, RESOURCE_TYPE_MASTER);

		DataRecordGroupSpy readDataRecordGroup = testReadRecord();
		var dataDivider = testGetDataDivider(readDataRecordGroup);
		testCreateInArchive(dataDivider);
		testUpdateRecord();

		recordUpdater.MCR.assertReturn("updateRecord", 0, binaryRecord);
		assertTrue(binaryRecord instanceof DataRecord);
	}

	private Object testGetDataDivider(DataRecordGroupSpy readDataRecordGroup) {
		readDataRecordGroup.MCR.assertParameters("getDataDivider", 0);
		return readDataRecordGroup.MCR.getReturnValue("getDataDivider", 0);
	}

	private DataRecordGroupSpy testReadRecord() {
		dependencyProvider.MCR.assertParameters("getRecordStorage", 0);
		dependencyProvider.MCR.assertReturn("getRecordStorage", 0, recordStorage);
		recordStorage.MCR.assertParameters("read", 0, BINARY_RECORD_TYPE, SOME_RECORD_ID);
		return (DataRecordGroupSpy) recordStorage.MCR.getReturnValue("read", 0);
	}

	private void testUpdateRecord() {
		spiderInstanceFactory.MCR.assertParameters("factorRecordUpdater", 0);
		spiderInstanceFactory.MCR.assertReturn("factorRecordUpdater", 0, recordUpdater);
		recordUpdater.MCR.assertParameters("updateRecord", 0, SOME_AUTH_TOKEN, BINARY_RECORD_TYPE,
				SOME_RECORD_ID);
	}

	private void testCreateInArchive(Object dataDivider) {
		dependencyProvider.MCR.assertParameters("getResourceArchive", 0);
		dependencyProvider.MCR.assertReturn("getResourceArchive", 0, resourceArchive);
		resourceArchive.MCR.assertParameters("createMasterResource", 0, dataDivider,
				BINARY_RECORD_TYPE, SOME_RECORD_ID, someStream, MIME_TYPE_GENERIC);
	}

	@Test
	public void testUploadUserAuthenticated() throws Exception {
		uploader.upload(SOME_AUTH_TOKEN, BINARY_RECORD_TYPE, SOME_RECORD_ID, someStream,
				RESOURCE_TYPE_MASTER);

		dependencyProvider.MCR.assertParameters("getAuthenticator", 0);
		authenticator.MCR.assertParameters("getUserForToken", 0, SOME_AUTH_TOKEN);
	}

	@Test
	public void testUploadUserNotAuthenticated() throws Exception {
		authenticator.MRV.setAlwaysThrowException("getUserForToken",
				new AuthenticationException(SOME_EXCEPTION_MESSAGE));
		try {
			uploader.upload(SOME_AUTH_TOKEN, BINARY_RECORD_TYPE, SOME_RECORD_ID, someStream,
					RESOURCE_TYPE_MASTER);
			fail("It should throw Exception");
		} catch (Exception e) {
			assertTrue(e instanceof AuthenticationException,
					"AuthenticationException should be thrown");
			assertEquals(e.getMessage(), MessageFormat.format(ERR_MSG_AUTHENTICATION,
					BINARY_RECORD_TYPE, SOME_RECORD_ID));
			assertEquals(e.getCause().getMessage(), SOME_EXCEPTION_MESSAGE);
		}
	}

	@Test
	public void testUploadUserAuthorized() throws Exception {
		uploader.upload(SOME_AUTH_TOKEN, BINARY_RECORD_TYPE, SOME_RECORD_ID, someStream,
				RESOURCE_TYPE_MASTER);

		dependencyProvider.MCR.assertParameters("getDataGroupTermCollector", 0);
		recordTypeHandler.MCR.assertParameters("getDefinitionId", 0);
		var definitionId = recordTypeHandler.MCR.getReturnValue("getDefinitionId", 0);
		var binaryRecord = (DataRecordGroupSpy) recordStorage.MCR.getReturnValue("read", 0);

		termCollector.MCR.assertParameters("collectTerms", 0, definitionId, binaryRecord);

		var user = authenticator.MCR.getReturnValue("getUserForToken", 0);
		CollectTerms collectedTerms = (CollectTerms) termCollector.MCR
				.getReturnValue("collectTerms", 0);
		dependencyProvider.MCR.assertParameters("getSpiderAuthorizator", 0);
		authorizator.MCR.assertParameters(
				"checkUserIsAuthorizedForActionOnRecordTypeAndCollectedData", 0, user,
				ACTION_UPLOAD, BINARY_RECORD_TYPE, collectedTerms.permissionTerms);
	}

	@Test
	public void testUploadUserNotAuthorizated() throws Exception {
		authorizator.MRV.setAlwaysThrowException(
				"checkUserIsAuthorizedForActionOnRecordTypeAndCollectedData",
				new AuthorizationException(SOME_EXCEPTION_MESSAGE));
		try {
			uploader.upload(SOME_AUTH_TOKEN, BINARY_RECORD_TYPE, SOME_RECORD_ID, someStream,
					RESOURCE_TYPE_MASTER);
			fail("It should throw Exception");
		} catch (Exception e) {
			assertTrue(e instanceof AuthorizationException,
					"AuthorizationException should be thrown");
			assertEquals(e.getMessage(), MessageFormat.format(ERR_MSG_AUTHORIZATION,
					BINARY_RECORD_TYPE, SOME_RECORD_ID));
			assertEquals(e.getCause().getMessage(), SOME_EXCEPTION_MESSAGE);
		}
	}

	@Test
	public void testUploadExceptionTypeDifferentThanBinary() throws Exception {
		try {
			uploader.upload(SOME_AUTH_TOKEN, SOME_RECORD_TYPE, SOME_RECORD_ID, someStream,
					RESOURCE_TYPE_MASTER);
			fail("It should throw exception");
		} catch (Exception e) {
			assertTrue(e instanceof MisuseException);
			assertEquals(e.getMessage(),
					MessageFormat.format(ERR_MESSAGE_MISUSE, SOME_RECORD_TYPE, SOME_RECORD_ID));

			ensureNoUploadLogicStarts();
		}
	}

	private void ensureNoUploadLogicStarts() {
		authenticator.MCR.assertMethodWasCalled("getUserForToken");
		authorizator.MCR.assertMethodNotCalled(
				"checkUserIsAuthorizedForActionOnRecordTypeAndCollectedData");
		recordStorage.MCR.assertMethodNotCalled("read");
		resourceArchive.MCR.assertMethodNotCalled("create");
		recordUpdater.MCR.assertMethodNotCalled("updateRecord");
	}

	@Test
	public void testUploadExceptionResourceTypeDifferentThanMaster() throws Exception {
		try {
			uploader.upload(SOME_AUTH_TOKEN, BINARY_RECORD_TYPE, SOME_RECORD_ID, someStream,
					SOME_RESOURCE_TYPE);
			fail("It should throw exception");
		} catch (Exception e) {
			assertTrue(e instanceof MisuseException);
			assertEquals(e.getMessage(), "Only master can be uploaded.");
			ensureNoUploadLogicStarts();
		}
	}

	@Test
	public void testUploadExceptionWhenResourceStreamIsMissing() throws Exception {
		try {
			uploader.upload(SOME_AUTH_TOKEN, BINARY_RECORD_TYPE, SOME_RECORD_ID, null,
					RESOURCE_TYPE_MASTER);
			fail("It should throw exception");
		} catch (Exception e) {
			assertTrue(e instanceof DataMissingException);
			assertEquals(e.getMessage(), MessageFormat.format(ERR_MESAGE_MISSING_RESOURCE_STREAM,
					BINARY_RECORD_TYPE, SOME_RECORD_ID));
			ensureNoUploadLogicStarts();
		}
	}

	@Test
	public void testUploadStoreResourceDataintoStorageWithoutChecksum() throws Exception {
		DataRecordGroupSpy readBinarySpy = new DataRecordGroupSpy();
		readBinarySpy.MRV.setSpecificReturnValuesSupplier("getFirstAtomicValueWithNameInData",
				() -> EXPECTED_ORIGINAL_FILE_NAME, "originalFileName");
		readBinarySpy.MRV.setSpecificReturnValuesSupplier("getFirstAtomicValueWithNameInData",
				() -> EXPECTED_FILE_SIZE, "expectedFileSize");
		recordStorage.MRV.setDefaultReturnValuesSupplier("read", () -> readBinarySpy);

		uploader.upload(SOME_AUTH_TOKEN, BINARY_RECORD_TYPE, SOME_RECORD_ID, someStream,
				RESOURCE_TYPE_MASTER);

		var binaryDG = (DataGroupSpy) dataFactorySpy.MCR
				.getReturnValue("factorGroupFromDataRecordGroup", 0);

		recordUpdater.MCR.assertParameter("updateRecord", 0, "record", binaryDG);

		assertMasterIsCorrect(readBinarySpy, RESOURCE_TYPE_MASTER);
		assertRemoveExpectedFieldsFromBinaryRecord(readBinarySpy);
	}

	@Test
	public void testUploadStoreResourceDataintoStorage() throws Exception {
		DataRecordGroupSpy readBinarySpy = new DataRecordGroupSpy();
		readBinarySpy.MRV.setSpecificReturnValuesSupplier("getFirstAtomicValueWithNameInData",
				() -> EXPECTED_ORIGINAL_FILE_NAME, "originalFileName");
		readBinarySpy.MRV.setSpecificReturnValuesSupplier("getFirstAtomicValueWithNameInData",
				() -> EXPECTED_FILE_SIZE, "expectedFileSize");
		readBinarySpy.MRV.setSpecificReturnValuesSupplier("containsChildWithNameInData",
				() -> false, "expectedChecksum");
		readBinarySpy.MRV.setSpecificReturnValuesSupplier("getFirstAtomicValueWithNameInData",
				() -> EXPECTED_CHECKSUM, "expectedChecksum");
		recordStorage.MRV.setDefaultReturnValuesSupplier("read", () -> readBinarySpy);

		uploader.upload(SOME_AUTH_TOKEN, BINARY_RECORD_TYPE, SOME_RECORD_ID, someStream,
				RESOURCE_TYPE_MASTER);

		var binaryDG = (DataGroupSpy) dataFactorySpy.MCR
				.getReturnValue("factorGroupFromDataRecordGroup", 0);

		recordUpdater.MCR.assertParameter("updateRecord", 0, "record", binaryDG);

		assertMasterIsCorrect(readBinarySpy, RESOURCE_TYPE_MASTER);
		assertRemoveExpectedFieldsFromBinaryRecord(readBinarySpy);
	}

	private void assertMasterIsCorrect(DataRecordGroupSpy readBinarySpy,
			String resourceTypeMaster) {
		dataFactorySpy.MCR.assertParameters("factorGroupUsingNameInData", 0, resourceTypeMaster);

		dataFactorySpy.MCR.assertParameters("factorAtomicUsingNameInDataAndValue", 0, "resourceId",
				SOME_RECORD_ID + "-master");
		ContentAnalyzerSpy contentAnalyzer = (ContentAnalyzerSpy) contentAnalyzeInstanceProviderSpy.MCR
				.getReturnValue("getContentAnalyzer", 0);
		var detectedMimeType = contentAnalyzer.MCR.getReturnValue("getMimeType", 0);
		dataFactorySpy.MCR.assertParameters("factorResourceLinkUsingNameInDataAndMimeType", 0,
				"master", detectedMimeType);

		dataFactorySpy.MCR.assertParameters("factorAtomicUsingNameInDataAndValue", 1, "fileSize",
				"someFileSize");
		dataFactorySpy.MCR.assertParameters("factorAtomicUsingNameInDataAndValue", 2, "mimeType",
				"someMimeType");
		dataFactorySpy.MCR.assertParameters("factorAtomicUsingNameInDataAndValue", 3, "checksum",
				"someChecksumSHA512");
		dataFactorySpy.MCR.assertParameters("factorAtomicUsingNameInDataAndValue", 4,
				"checksumType", SHA512);

		DataGroupSpy master = (DataGroupSpy) dataFactorySpy.MCR
				.getReturnValue("factorGroupUsingNameInData", 0);

		DataAtomicSpy resourceId = (DataAtomicSpy) dataFactorySpy.MCR
				.getReturnValue("factorAtomicUsingNameInDataAndValue", 0);

		DataResourceLinkSpy resourceLink = (DataResourceLinkSpy) dataFactorySpy.MCR
				.getReturnValue("factorResourceLinkUsingNameInDataAndMimeType", 0);

		DataAtomicSpy fileSize = (DataAtomicSpy) dataFactorySpy.MCR
				.getReturnValue("factorAtomicUsingNameInDataAndValue", 1);
		DataAtomicSpy mimeType = (DataAtomicSpy) dataFactorySpy.MCR
				.getReturnValue("factorAtomicUsingNameInDataAndValue", 2);
		DataAtomicSpy checksum = (DataAtomicSpy) dataFactorySpy.MCR
				.getReturnValue("factorAtomicUsingNameInDataAndValue", 3);
		DataAtomicSpy checksumType = (DataAtomicSpy) dataFactorySpy.MCR
				.getReturnValue("factorAtomicUsingNameInDataAndValue", 4);
		DataChild originalFileName = (DataChild) readBinarySpy.MCR
				.getReturnValue("getFirstChildWithNameInData", 0);

		readBinarySpy.MCR.assertParameters("addChild", 0, master);
		master.MCR.assertParameters("addChild", 0, resourceId);
		master.MCR.assertParameters("addChild", 1, resourceLink);
		master.MCR.assertParameters("addChild", 2, fileSize);
		master.MCR.assertParameters("addChild", 3, mimeType);
		master.MCR.assertParameters("addChild", 4, checksum);
		master.MCR.assertParameters("addChild", 5, checksumType);
		master.MCR.assertParameters("addChild", 6, originalFileName);
	}

	private void assertRemoveExpectedFieldsFromBinaryRecord(DataRecordGroupSpy readBinarySpy) {
		readBinarySpy.MCR.assertParameters("removeFirstChildWithNameInData", 0, "originalFileName");
		readBinarySpy.MCR.assertParameters("removeFirstChildWithNameInData", 1, "expectedFileSize");
		readBinarySpy.MCR.assertParameters("removeFirstChildWithNameInData", 2, "expectedChecksum");
	}

	@Test
	public void testResourceReadFromArchive() throws Exception {
		uploader.upload(SOME_AUTH_TOKEN, BINARY_RECORD_TYPE, SOME_RECORD_ID, someStream,
				RESOURCE_TYPE_MASTER);

		DataRecordGroupSpy readDataRecordGroup = (DataRecordGroupSpy) recordStorage.MCR
				.getReturnValue("read", 0);
		var dataDivider = testGetDataDivider(readDataRecordGroup);

		resourceArchive.MCR.assertParameters("readMasterResourceMetadata", 0, dataDivider,
				BINARY_RECORD_TYPE, SOME_RECORD_ID);
	}

	@Test
	public void testResourceReadFromArchiveSentToContentAnalyzer() throws Exception {
		uploader.upload(SOME_AUTH_TOKEN, BINARY_RECORD_TYPE, SOME_RECORD_ID, someStream,
				RESOURCE_TYPE_MASTER);

		var resourceFromArchive = resourceArchive.MCR.getReturnValue("readMasterResource", 0);

		contentAnalyzeInstanceProviderSpy.MCR.assertParameters("getContentAnalyzer", 0);
		ContentAnalyzerSpy contentAnalyzer = (ContentAnalyzerSpy) contentAnalyzeInstanceProviderSpy.MCR
				.getReturnValue("getContentAnalyzer", 0);

		contentAnalyzer.MCR.assertParameters("getMimeType", 0, resourceFromArchive);
	}

	@Test
	public void testReadResourceMetadataCalled() throws Exception {
		uploader.upload(SOME_AUTH_TOKEN, BINARY_RECORD_TYPE, SOME_RECORD_ID, someStream,
				RESOURCE_TYPE_MASTER);

		DataRecordGroupSpy readDataRecordGroup = (DataRecordGroupSpy) recordStorage.MCR
				.getReturnValue("read", 0);

		var dataDivider = testGetDataDivider(readDataRecordGroup);

		resourceArchive.MCR.assertParameters("readMasterResourceMetadata", 0, dataDivider,
				BINARY_RECORD_TYPE, SOME_RECORD_ID);
	}

	@Test
	public void testCallUpdateResourceArchiveMetadata() throws Exception {
		uploader.upload(SOME_AUTH_TOKEN, BINARY_RECORD_TYPE, SOME_RECORD_ID, someStream,
				RESOURCE_TYPE_MASTER);

		DataRecordGroupSpy readDataRecordGroup = (DataRecordGroupSpy) recordStorage.MCR
				.getReturnValue("read", 0);
		var dataDivider = testGetDataDivider(readDataRecordGroup);
		resourceArchive.MCR.assertParameters("updateMasterResourceMetadata", 0, dataDivider,
				BINARY_RECORD_TYPE, SOME_RECORD_ID);

		assertResourceMetadata(readDataRecordGroup);
	}

	private void assertResourceMetadata(DataRecordGroupSpy readDataRecordGroup) {
		ResourceMetadataToUpdate resourceMetadata = (ResourceMetadataToUpdate) resourceArchive.MCR
				.getValueForMethodNameAndCallNumberAndParameterName("updateMasterResourceMetadata",
						0, "resourceMetadataToUpdate");

		contentAnalyzeInstanceProviderSpy.MCR.assertParameters("getContentAnalyzer", 0);
		ContentAnalyzerSpy contentAnalyzer = (ContentAnalyzerSpy) contentAnalyzeInstanceProviderSpy.MCR
				.getReturnValue("getContentAnalyzer", 0);

		contentAnalyzer.MCR.assertReturn("getMimeType", 0, resourceMetadata.mimeType());

		readDataRecordGroup.MCR.assertParameters("getFirstAtomicValueWithNameInData", 0,
				"originalFileName");
		readDataRecordGroup.MCR.assertReturn("getFirstAtomicValueWithNameInData", 0,
				resourceMetadata.originalFileName());
	}

	@Test
	public void testCallUpdateSendsMessageForReadMetadataAndConvertSmallFormats() throws Exception {
		createContentAnalyzerUsingMediaTypeToReturn("image/whatever");

		uploader.upload(SOME_AUTH_TOKEN, BINARY_RECORD_TYPE, SOME_RECORD_ID, someStream,
				RESOURCE_TYPE_MASTER);

		DataRecordGroupSpy readDataRecordGroup = testReadRecord();
		var dataDivider = testGetDataDivider(readDataRecordGroup);

		resourceConvert.MCR.assertParameters("sendMessageForAnalyzingAndConvertingImages", 0,
				dataDivider, BINARY_RECORD_TYPE, SOME_RECORD_ID, "image/whatever");
		resourceConvert.MCR.assertMethodNotCalled("sendMessageToConvertPdfToThumbnails");
	}

	private void createContentAnalyzerUsingMediaTypeToReturn(String mediaType) {
		ContentAnalyzerSpy contentAnalyzerSpy = new ContentAnalyzerSpy();

		contentAnalyzerSpy.MRV.setDefaultReturnValuesSupplier("getMimeType", () -> mediaType);
		contentAnalyzeInstanceProviderSpy.MRV.setDefaultReturnValuesSupplier("getContentAnalyzer",
				() -> contentAnalyzerSpy);
	}

	@Test
	public void testCallUpdateSendsMessageToConvertPdf() throws Exception {
		createContentAnalyzerUsingMediaTypeToReturn("application/pdf");

		uploader.upload(SOME_AUTH_TOKEN, BINARY_RECORD_TYPE, SOME_RECORD_ID, someStream,
				RESOURCE_TYPE_MASTER);

		DataRecordGroupSpy readDataRecordGroup = testReadRecord();
		var dataDivider = testGetDataDivider(readDataRecordGroup);

		resourceConvert.MCR.assertParameters("sendMessageToConvertPdfToThumbnails", 0, dataDivider,
				BINARY_RECORD_TYPE, SOME_RECORD_ID);
		resourceConvert.MCR.assertMethodNotCalled("sendMessageForAnalyzingAndConvertingImages");
	}

	@Test
	public void testUploadOtherThanImageIsNotSentToAnalyzeAndConvert() throws Exception {
		uploader.upload(SOME_AUTH_TOKEN, BINARY_RECORD_TYPE, SOME_RECORD_ID, someStream,
				RESOURCE_TYPE_MASTER);

		resourceConvert.MCR.assertMethodNotCalled("sendMessageForAnalyzingAndConvertingImages");
		resourceConvert.MCR.assertMethodNotCalled("sendMessageToConvertPdfToThumbnails");
	}

	@Test
	public void testSetBinaryTypeToCorrectType() throws Exception {
		createContentAnalyzerUsingMediaTypeToReturn("image/whatever");

		uploader.upload(SOME_AUTH_TOKEN, BINARY_RECORD_TYPE, SOME_RECORD_ID, someStream,
				RESOURCE_TYPE_MASTER);

		mimeTypeToBinaryType.MCR.assertParameters("toBinaryType", 0, "image/whatever");
		String binaryType = (String) mimeTypeToBinaryType.MCR.getReturnValue("toBinaryType", 0);
		dataRecordGroupSpy.MCR.assertParameters("addAttributeByIdWithValue", 0, "type", binaryType);
	}

	@Test
	public void testFileSizeMismatch() throws Exception {
		DataRecordGroupSpy readBinarySpy = setupReadSpyForIntegrityCheck();
		readBinarySpy.MRV.setSpecificReturnValuesSupplier("containsChildWithNameInData", () -> true,
				"expectedFileSize");
		readBinarySpy.MRV.setSpecificReturnValuesSupplier("getFirstAtomicValueWithNameInData",
				() -> EXPECTED_FILE_SIZE, "expectedFileSize");
		try {
			uploader.upload(SOME_AUTH_TOKEN, BINARY_RECORD_TYPE, SOME_RECORD_ID, someStream,
					RESOURCE_TYPE_MASTER);
			fail("An exception should have been thrown");
		} catch (Exception e) {
			assertTrue(e instanceof ArchiveDataIntergrityException);
			assertEquals(e.getMessage(), "The file size verification of uploaded data failed: the "
					+ "actual value was: someFileSize but the expected value was: expectedFileSize.");
			resourceArchive.MCR.assertMethodWasCalled("delete");
			resourceArchive.MCR.assertMethodNotCalled("readMasterResource");
		}
	}

	private DataRecordGroupSpy setupReadSpyForIntegrityCheck() {
		DataRecordGroupSpy readBinarySpy = new DataRecordGroupSpy();
		readBinarySpy.MRV.setSpecificReturnValuesSupplier("getFirstAtomicValueWithNameInData",
				() -> EXPECTED_ORIGINAL_FILE_NAME, "originalFileName");
		readBinarySpy.MRV.setSpecificReturnValuesSupplier("getFirstAtomicValueWithNameInData",
				() -> EXPECTED_CHECKSUM, "expectedChecksum");
		recordStorage.MRV.setDefaultReturnValuesSupplier("read", () -> readBinarySpy);
		return readBinarySpy;
	}

	@Test
	public void testExpectedFileSizeMatch() throws Exception {
		DataRecordGroupSpy readBinarySpy = setupReadSpyForIntegrityCheck();
		readBinarySpy.MRV.setSpecificReturnValuesSupplier("containsChildWithNameInData", () -> true,
				"expectedFileSize");
		readBinarySpy.MRV.setSpecificReturnValuesSupplier("getFirstAtomicValueWithNameInData",
				() -> "someFileSize", "expectedFileSize");

		uploader.upload(SOME_AUTH_TOKEN, BINARY_RECORD_TYPE, SOME_RECORD_ID, someStream,
				RESOURCE_TYPE_MASTER);
		resourceArchive.MCR.assertMethodNotCalled("delete");
		resourceArchive.MCR.assertMethodWasCalled("readMasterResource");
	}

	@Test
	public void testChecksumMismatch() throws Exception {
		DataRecordGroupSpy readBinarySpy = setupReadSpyForIntegrityCheck();
		readBinarySpy.MRV.setSpecificReturnValuesSupplier("containsChildWithNameInData", () -> true,
				"expectedChecksum");
		readBinarySpy.MRV.setSpecificReturnValuesSupplier("getFirstAtomicValueWithNameInData",
				() -> EXPECTED_FILE_SIZE, "expectedFileSize");

		try {
			uploader.upload(SOME_AUTH_TOKEN, BINARY_RECORD_TYPE, SOME_RECORD_ID, someStream,
					RESOURCE_TYPE_MASTER);
			fail("An exception should have been thrown");
		} catch (Exception e) {
			assertTrue(e instanceof ArchiveDataIntergrityException);
			assertEquals(e.getMessage(), "The checksum verification of uploaded data failed: the "
					+ "actual value was: someChecksumSHA512 but the expected value was: expectedChecksum.");
			resourceArchive.MCR.assertMethodWasCalled("delete");
			resourceArchive.MCR.assertMethodNotCalled("readMasterResource");
		}
	}

	@Test
	public void testExpectedChecksumMatch() throws Exception {
		DataRecordGroupSpy readBinarySpy = setupReadSpyForIntegrityCheck();
		readBinarySpy.MRV.setSpecificReturnValuesSupplier("containsChildWithNameInData", () -> true,
				"expectedChecksum");
		readBinarySpy.MRV.setSpecificReturnValuesSupplier("getFirstAtomicValueWithNameInData",
				() -> "someChecksumSHA512", "expectedChecksum");

		uploader.upload(SOME_AUTH_TOKEN, BINARY_RECORD_TYPE, SOME_RECORD_ID, someStream,
				RESOURCE_TYPE_MASTER);
		resourceArchive.MCR.assertMethodNotCalled("delete");
		resourceArchive.MCR.assertMethodWasCalled("readMasterResource");
	}

	@Test
	public void testOnlyForTestGetResourceConvert() throws Exception {
		assertEquals(uploader.onlyForTestGetResourceConvert(), resourceConvert);
	}

	@Test
	public void testOnlyForTestGetDependencyConverter() throws Exception {
		assertEquals(uploader.onlyForTestGetDependecyProvider(), dependencyProvider);
	}

	@Test
	public void testOnlyForTestGetMimeTypeToBinaryConvert() throws Exception {
		assertEquals(uploader.onlyForTestGetMimeTypeToBinaryTypeConvert(), mimeTypeToBinaryType);
	}
}