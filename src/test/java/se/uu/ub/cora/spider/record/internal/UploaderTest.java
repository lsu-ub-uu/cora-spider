/*
 * Copyright 2015, 2016, 2019, 2023 Uppsala University Library
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

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertTrue;
import static org.testng.Assert.fail;

import java.text.MessageFormat;
import java.util.List;

import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import se.uu.ub.cora.bookkeeper.linkcollector.DataRecordLinkCollector;
import se.uu.ub.cora.bookkeeper.validator.DataValidator;
import se.uu.ub.cora.data.DataProvider;
import se.uu.ub.cora.data.DataRecord;
import se.uu.ub.cora.data.collected.CollectTerms;
import se.uu.ub.cora.data.copier.DataCopierFactory;
import se.uu.ub.cora.data.copier.DataCopierProvider;
import se.uu.ub.cora.data.spies.DataAtomicSpy;
import se.uu.ub.cora.data.spies.DataFactorySpy;
import se.uu.ub.cora.data.spies.DataGroupSpy;
import se.uu.ub.cora.data.spies.DataRecordGroupSpy;
import se.uu.ub.cora.data.spies.DataResourceLinkSpy;
import se.uu.ub.cora.logger.LoggerProvider;
import se.uu.ub.cora.search.RecordIndexer;
import se.uu.ub.cora.spider.authentication.AuthenticationException;
import se.uu.ub.cora.spider.authentication.OldAuthenticatorSpy;
import se.uu.ub.cora.spider.authorization.AuthorizationException;
import se.uu.ub.cora.spider.authorization.PermissionRuleCalculator;
import se.uu.ub.cora.spider.data.DataMissingException;
import se.uu.ub.cora.spider.dependency.SpiderInstanceProvider;
import se.uu.ub.cora.spider.dependency.spy.InputStreamSpy;
import se.uu.ub.cora.spider.dependency.spy.RecordTypeHandlerSpy;
import se.uu.ub.cora.spider.dependency.spy.ResourceArchiveSpy;
import se.uu.ub.cora.spider.extendedfunctionality.internal.ExtendedFunctionalityProviderSpy;
import se.uu.ub.cora.spider.log.LoggerFactorySpy;
import se.uu.ub.cora.spider.record.DataCopierFactorySpy;
import se.uu.ub.cora.spider.record.MisuseException;
import se.uu.ub.cora.spider.record.Uploader;
import se.uu.ub.cora.spider.spy.DataGroupTermCollectorSpy;
import se.uu.ub.cora.spider.spy.IdGeneratorSpy;
import se.uu.ub.cora.spider.spy.OldSpiderAuthorizatorSpy;
import se.uu.ub.cora.spider.spy.SpiderDependencyProviderSpy;
import se.uu.ub.cora.spider.testspies.RecordUpdaterSpy;
import se.uu.ub.cora.spider.testspies.SpiderInstanceFactorySpy;
import se.uu.ub.cora.storage.spies.RecordStorageSpy;

public class UploaderTest {
	private static final String EXPECTED_ORIGINAL_FILE_NAME = "expectedOriginalFileName";
	private static final String FAKE_HEIGHT_WIDTH_RESOLUTION = "0";
	private static final String FAKE_FILE_SIZE = "10";
	private static final String FAKE_CHECKSUM = "afAF09";
	private static final String ACTION_UPLOAD = "upload";
	private static final String ERR_MSG_AUTHENTICATION = "Uploading error: Not possible to upload "
			+ "resource due the user could not be authenticated, for type {0} and id {1}.";
	private static final String ERR_MSG_AUTHORIZATION = "Uploading error: Not possible to upload "
			+ "resource due the user could not be authorizated, for type {0} and id {1}.";
	private static final String MIME_TYPE_JPEG = "image/jpeg";
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
	private OldAuthenticatorSpy oldAuthenticator;
	// private StreamStorageSpy streamStorage;
	private OldSpiderAuthorizatorSpy oldAuthorizator;
	private PermissionRuleCalculator keyCalculator;
	private Uploader uploader;
	private DataValidator dataValidator;
	private DataRecordLinkCollector linkCollector;
	private DataGroupTermCollectorSpy termCollector;
	private RecordIndexer recordIndexer;
	private IdGeneratorSpy idGenerator;
	private SpiderDependencyProviderSpy dependencyProvider;
	private ExtendedFunctionalityProviderSpy extendedFunctionalityProvider;
	private SpiderInstanceFactorySpy spiderInstanceFactory;
	private LoggerFactorySpy loggerFactorySpy;
	private DataFactorySpy dataFactorySpy;

	private DataCopierFactory dataCopierFactory;
	private RecordTypeHandlerSpy recordTypeHandler;

	@BeforeMethod
	public void beforeMethod() {

		dependencyProvider = new SpiderDependencyProviderSpy();
		dataFactorySpy = new DataFactorySpy();

		DataProvider.onlyForTestSetDataFactory(dataFactorySpy);
		// setUpFactoriesAndProviders();
		// oldAuthenticator = new OldAuthenticatorSpy();
		// authorizator = new SpiderAuthorizatorSpy();
		// dataValidator = new DataValidatorSpy();
		// recordStorage = TestDataRecordInMemoryStorage.createRecordStorageInMemoryWithTestData();
		// keyCalculator = new RuleCalculatorSpy();
		// linkCollector = new DataRecordLinkCollectorSpy();
		// idGenerator = new IdGeneratorSpy();
		// streamStorage = new StreamStorageSpy();

		// recordIndexer = new RecordIndexerSpy();
		// extendedFunctionalityProvider = new ExtendedFunctionalityProviderSpy();

		setUpSpiderInstanceProvider();
		setUpDependencyProvider();

		someStream = new InputStreamSpy();

		uploader = UploaderImp.usingDependencyProvider(dependencyProvider);
	}

	private void setUpSpiderInstanceProvider() {
		recordUpdater = new RecordUpdaterSpy();
		spiderInstanceFactory = new SpiderInstanceFactorySpy();
		spiderInstanceFactory.MRV.setDefaultReturnValuesSupplier("factorRecordUpdater",
				() -> recordUpdater);
	}

	private void setUpFactoriesAndProviders() {
		loggerFactorySpy = new LoggerFactorySpy();
		LoggerProvider.setLoggerFactory(loggerFactorySpy);
		dataFactorySpy = new DataFactorySpy();
		DataProvider.onlyForTestSetDataFactory(dataFactorySpy);

		dataCopierFactory = new DataCopierFactorySpy();
		DataCopierProvider.setDataCopierFactory(dataCopierFactory);
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
		resourceArchive = new ResourceArchiveSpy();
		dependencyProvider.MRV.setDefaultReturnValuesSupplier("getResourceArchive",
				() -> resourceArchive);

		recordTypeHandler = new RecordTypeHandlerSpy();
		recordTypeHandler.MRV.setDefaultReturnValuesSupplier("getDefinitionId",
				() -> "someDefintion");
		dependencyProvider.MRV.setDefaultReturnValuesSupplier(
				"getRecordTypeHandlerUsingDataRecordGroup", () -> recordTypeHandler);

		termCollector = new DataGroupTermCollectorSpy();
		dependencyProvider.MRV.setDefaultReturnValuesSupplier("getDataGroupTermCollector",
				() -> termCollector);

		// dependencyProvider = new SpiderDependencyProviderOldSpy();
		// dependencyProvider.authenticator = oldAuthenticator;
		// dependencyProvider.spiderAuthorizator = authorizator;
		// dependencyProvider.dataValidator = dataValidator;
		//
		// dependencyProvider.recordStorage = recordStorage;
		// // dependencyProvider.streamStorage = streamStorage;
		// dependencyProvider.recordIdGenerator = idGenerator;
		//
		// dependencyProvider.ruleCalculator = keyCalculator;
		// dependencyProvider.linkCollector = linkCollector;
		// dependencyProvider.termCollector = termCollector;
		// dependencyProvider.recordIndexer = recordIndexer;
		// dependencyProvider.extendedFunctionalityProvider = extendedFunctionalityProvider;

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
		var dataDivider = readDataRecordGroup.MCR.getReturnValue("getDataDivider", 0);
		return dataDivider;
	}

	private DataRecordGroupSpy testReadRecord() {
		dependencyProvider.MCR.assertParameters("getRecordStorage", 0);
		dependencyProvider.MCR.assertReturn("getRecordStorage", 0, recordStorage);
		recordStorage.MCR.assertParameterAsEqual("read", 0, "types", List.of(BINARY_RECORD_TYPE));
		recordStorage.MCR.assertParameter("read", 0, "id", SOME_RECORD_ID);
		DataGroupSpy readDataGroup = (DataGroupSpy) recordStorage.MCR.getReturnValue("read", 0);
		dataFactorySpy.MCR.assertParameters("factorRecordGroupFromDataGroup", 0, readDataGroup);
		DataRecordGroupSpy readDataRecordGroup = (DataRecordGroupSpy) dataFactorySpy.MCR
				.getReturnValue("factorRecordGroupFromDataGroup", 0);
		return readDataRecordGroup;
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
		resourceArchive.MCR.assertParameters("create", 0, dataDivider, BINARY_RECORD_TYPE,
				SOME_RECORD_ID, someStream, MIME_TYPE_JPEG);
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
		var binaryRecord = (DataGroupSpy) recordStorage.MCR.getReturnValue("read", 0);
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
		authenticator.MCR.assertMethodNotCalled("getUserForToken");
		authorizator.MCR.assertMethodNotCalled(
				"checkUserIsAuthorizedForActionOnRecordTypeAndCollectedData");
		recordStorage.MCR.assertMethodNotCalled("read");
		recordUpdater.MCR.assertMethodNotCalled("updateRecord");
		resourceArchive.MCR.assertMethodNotCalled("create");
	}

	@Test
	public void testUploadExceptionResourceTypeDifferentThanMaster() throws Exception {
		try {
			uploader.upload(SOME_AUTH_TOKEN, BINARY_RECORD_TYPE, SOME_RECORD_ID, someStream,
					SOME_RESOURCE_TYPE);
			fail("It should throw exception");
		} catch (Exception e) {
			assertTrue(e instanceof MisuseException);
			assertEquals(e.getMessage(),
					"Not implemented yet for resource type different than master.");
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
			System.out.println(e.getClass());
			assertTrue(e instanceof DataMissingException);
			assertEquals(e.getMessage(), MessageFormat.format(ERR_MESAGE_MISSING_RESOURCE_STREAM,
					BINARY_RECORD_TYPE, SOME_RECORD_ID));
			ensureNoUploadLogicStarts();
		}
	}

	@Test
	public void testUploadStoreResourceDataintoStorage() throws Exception {
		DataGroupSpy readBinarySpy = new DataGroupSpy();
		readBinarySpy.MRV.setSpecificReturnValuesSupplier("getFirstAtomicValueWithNameInData",
				() -> EXPECTED_ORIGINAL_FILE_NAME, "originalFileName");
		recordStorage.MRV.setDefaultReturnValuesSupplier("read", () -> readBinarySpy);

		uploader.upload(SOME_AUTH_TOKEN, BINARY_RECORD_TYPE, SOME_RECORD_ID, someStream,
				RESOURCE_TYPE_MASTER);

		DataGroupSpy binaryUpdatedGroup = (DataGroupSpy) recordUpdater.MCR
				.getValueForMethodNameAndCallNumberAndParameterName("updateRecord", 0, "record");

		assertResourceInfoIsCorrect(binaryUpdatedGroup);
		assertRemoveExpectedFieldsFromBinaryRecord(binaryUpdatedGroup);
	}

	private void assertResourceInfoIsCorrect(DataGroupSpy binaryUpdatedGroup) {

		dataFactorySpy.MCR.assertParameters("factorGroupUsingNameInData", 0, "resourceInfo");
		dataFactorySpy.MCR.assertParameters("factorGroupUsingNameInData", 1, "master");

		dataFactorySpy.MCR.assertParameters("factorAtomicUsingNameInDataAndValue", 0, "resourceId",
				SOME_RECORD_ID);
		dataFactorySpy.MCR.assertParameters("factorResourceLinkUsingNameInData", 0, "resourceLink");
		dataFactorySpy.MCR.assertParameters("factorAtomicUsingNameInDataAndValue", 1, "fileSize",
				FAKE_FILE_SIZE);
		dataFactorySpy.MCR.assertParameters("factorAtomicUsingNameInDataAndValue", 2, "mimeType",
				MIME_TYPE_JPEG);
		dataFactorySpy.MCR.assertParameters("factorAtomicUsingNameInDataAndValue", 3, "height",
				FAKE_HEIGHT_WIDTH_RESOLUTION);
		dataFactorySpy.MCR.assertParameters("factorAtomicUsingNameInDataAndValue", 4, "width",
				FAKE_HEIGHT_WIDTH_RESOLUTION);
		dataFactorySpy.MCR.assertParameters("factorAtomicUsingNameInDataAndValue", 5, "resolution",
				FAKE_HEIGHT_WIDTH_RESOLUTION);
		dataFactorySpy.MCR.assertParameters("factorAtomicUsingNameInDataAndValue", 6,
				"originalFileName", EXPECTED_ORIGINAL_FILE_NAME);
		dataFactorySpy.MCR.assertParameters("factorAtomicUsingNameInDataAndValue", 7, "checksum",
				FAKE_CHECKSUM);
		dataFactorySpy.MCR.assertParameters("factorAtomicUsingNameInDataAndValue", 8,
				"checksumType", "SHA512");

		DataGroupSpy resourceInfo = (DataGroupSpy) dataFactorySpy.MCR
				.getReturnValue("factorGroupUsingNameInData", 0);
		DataGroupSpy master = (DataGroupSpy) dataFactorySpy.MCR
				.getReturnValue("factorGroupUsingNameInData", 1);

		DataAtomicSpy resourceId = (DataAtomicSpy) dataFactorySpy.MCR
				.getReturnValue("factorAtomicUsingNameInDataAndValue", 0);

		DataResourceLinkSpy resourceLink = (DataResourceLinkSpy) dataFactorySpy.MCR
				.getReturnValue("factorResourceLinkUsingNameInData", 0);

		DataAtomicSpy fileSize = (DataAtomicSpy) dataFactorySpy.MCR
				.getReturnValue("factorAtomicUsingNameInDataAndValue", 1);
		DataAtomicSpy mimeType = (DataAtomicSpy) dataFactorySpy.MCR
				.getReturnValue("factorAtomicUsingNameInDataAndValue", 2);
		DataAtomicSpy height = (DataAtomicSpy) dataFactorySpy.MCR
				.getReturnValue("factorAtomicUsingNameInDataAndValue", 3);
		DataAtomicSpy width = (DataAtomicSpy) dataFactorySpy.MCR
				.getReturnValue("factorAtomicUsingNameInDataAndValue", 4);
		DataAtomicSpy resolution = (DataAtomicSpy) dataFactorySpy.MCR
				.getReturnValue("factorAtomicUsingNameInDataAndValue", 5);
		DataAtomicSpy originalFileName = (DataAtomicSpy) dataFactorySpy.MCR
				.getReturnValue("factorAtomicUsingNameInDataAndValue", 6);
		DataAtomicSpy checksum = (DataAtomicSpy) dataFactorySpy.MCR
				.getReturnValue("factorAtomicUsingNameInDataAndValue", 7);
		DataAtomicSpy checksumType = (DataAtomicSpy) dataFactorySpy.MCR
				.getReturnValue("factorAtomicUsingNameInDataAndValue", 8);

		binaryUpdatedGroup.MCR.assertParameters("addChild", 0, resourceInfo);
		resourceInfo.MCR.assertParameters("addChild", 0, master);
		master.MCR.assertParameters("addChild", 0, resourceId);
		master.MCR.assertParameters("addChild", 1, resourceLink);
		master.MCR.assertParameters("addChild", 2, fileSize);
		master.MCR.assertParameters("addChild", 3, mimeType);
		master.MCR.assertParameters("addChild", 4, height);
		master.MCR.assertParameters("addChild", 5, width);
		master.MCR.assertParameters("addChild", 6, resolution);
		binaryUpdatedGroup.MCR.assertParameters("addChild", 1, originalFileName);
		binaryUpdatedGroup.MCR.assertParameters("addChild", 2, checksum);
		binaryUpdatedGroup.MCR.assertParameters("addChild", 3, checksumType);

	}

	private void assertRemoveExpectedFieldsFromBinaryRecord(DataGroupSpy binaryUpdatedGroup) {
		binaryUpdatedGroup.MCR.assertParameters("removeFirstChildWithNameInData", 0,
				"expectedFileSize");
		binaryUpdatedGroup.MCR.assertParameters("removeFirstChildWithNameInData", 1,
				"expectedChecksum");
	}

	// @Test
	// public void testExternalDependenciesAreCalled() {
	// dataFactorySpy.MRV.setReturnValues("factorGroupUsingNameInData",
	// List.of(new DataGroupSpy()), "resourceInfo");
	// spiderInstanceFactory = new SpiderInstanceFactorySpy2();
	// setUpDependencyProvider();
	// recordStorage = new OldRecordStorageSpy();
	// keyCalculator = new RuleCalculatorSpy();
	// setUpDependencyProvider();
	//
	// DataGroup dataGroup = new DataGroupOldSpy("nameInData");
	// dataGroup.addChild(DataCreator2.createRecordInfoWithRecordTypeAndRecordIdAndDataDivider(
	// "spyType", "spyId", "cora"));
	//
	// DataRecord recordUpdated = uploader.upload(SOME_AUTH_TOKEN, BINARY_RECORD_TYPE,
	// SOME_RECORD_ID, someStream, SOME_RESOURCE_TYPE);
	//
	// assertResourceInfoIsCorrect(recordUpdated);
	//
	// assertTrue(((OldRecordStorageSpy) recordStorage).readWasCalled);
	//
	// authorizator.MCR.assertMethodWasCalled("checkUserIsAuthorizedForActionOnRecordType");
	//
	// }

	//
	// @Test
	// public void testUnauthorizedForDownloadOnRecordTypeShouldShouldNotAccessStorage() {
	// recordStorage = new OldRecordStorageSpy();
	// setUpDependencyProvider();
	// authorizator.authorizedForActionAndRecordType = false;
	//
	// boolean exceptionWasCaught = false;
	// try {
	// InputStream stream = new ByteArrayInputStream(
	// "a string".getBytes(StandardCharsets.UTF_8));
	//
	// uploader.upload("someToken78678567", BINARY, "image:123456789", stream, resourceType);
	// } catch (Exception e) {
	// assertEquals(e.getClass(), AuthorizationException.class);
	// exceptionWasCaught = true;
	// }
	// assertTrue(exceptionWasCaught);
	// assertFalse(((OldRecordStorageSpy) recordStorage).readWasCalled);
	// assertFalse(((OldRecordStorageSpy) recordStorage).updateWasCalled);
	// assertFalse(((OldRecordStorageSpy) recordStorage).deleteWasCalled);
	// assertFalse(((OldRecordStorageSpy) recordStorage).createWasCalled);
	// }
	//
	// @Test(expectedExceptions = AuthorizationException.class)
	// public void testUnauthorizedForDataInRecord() {
	// authorizator.authorizedForActionAndRecordTypeAndCollectedData = false;
	//
	// InputStream stream = new ByteArrayInputStream("a string".getBytes(StandardCharsets.UTF_8));
	//
	// uploader.upload("someToken78678567", BINARY, "image:123456789", stream, resourceType);
	// }
	//
	// @Test
	// public void testUploadStream() {
	// dataFactorySpy.MRV.setReturnValues("factorGroupUsingNameInData",
	// List.of(new DataGroupSpy()), "resourceInfo");
	// InputStream stream = new ByteArrayInputStream("a string".getBytes(StandardCharsets.UTF_8));
	// DataRecord recordUpdated = uploader.upload("someToken78678567", BINARY, "image:123456789",
	// stream, resourceType);
	//
	// assertEquals(streamStorage.stream, stream);
	//
	// assertStreamStorageCalledCorrectly(recordUpdated);
	// assertResourceInfoIsCorrect(recordUpdated);
	//
	// String methodName = "checkUserIsAuthorizedForActionOnRecordType";
	// authorizator.MCR.assertParameters(methodName, 0, authenticator.returnedUser, "upload",
	// BINARY);
	//
	// termCollector.MCR.assertParameter("collectTerms", 0, "metadataId",
	// "fakeDefMetadataIdFromRecordTypeHandlerSpy");
	// termCollector.MCR.assertParameter("collectTerms", 0, "dataGroup",
	// recordStorage.read(List.of(BINARY), "image:123456789"));
	//
	// String methodName2 = "checkUserIsAuthorizedForActionOnRecordTypeAndCollectedData";
	// CollectTerms collectedTerms = (CollectTerms) termCollector.MCR
	// .getReturnValue("collectTerms", 0);
	// authorizator.MCR.assertParameters(methodName2, 0, authenticator.returnedUser, "upload",
	// BINARY, collectedTerms.permissionTerms);
	// }
	//
	// private void assertStreamStorageCalledCorrectly(DataRecord recordUpdated) {
	// DataGroup groupUpdated = recordUpdated.getDataGroup();
	// DataGroup recordInfo = groupUpdated.getFirstGroupWithNameInData("recordInfo");
	// DataRecordLink dataDivider = (DataRecordLink) recordInfo
	// .getFirstChildWithNameInData("dataDivider");
	//
	// String dataDividerRecordId = dataDivider.getLinkedRecordId();
	//
	// assertEquals(dataDividerRecordId, streamStorage.dataDivider);
	// }
	//

	//
	// @Test(expectedExceptions = MisuseException.class, expectedExceptionsMessageRegExp = ""
	// + "It is only possible to upload files to recordType binary")
	// public void testUploadStreamNotChildOfBinary() {
	// InputStream stream = new ByteArrayInputStream("a string".getBytes(StandardCharsets.UTF_8));
	//
	// uploader.upload("someToken78678567", "place", "place:0002", stream, resourceType);
	// }
	//
	// @Test(expectedExceptions = MisuseException.class)
	// public void testUploadStreamNotChildOfBinary2() {
	// InputStream stream = new ByteArrayInputStream("a string".getBytes(StandardCharsets.UTF_8));
	//
	// uploader.upload("someToken78678567", "recordTypeAutoGeneratedId", "someId", stream,
	// resourceType);
	// }
	//
	// @Test(expectedExceptions = RecordNotFoundException.class)
	// public void testUploadNotFound() {
	// InputStream stream = new ByteArrayInputStream("a string".getBytes(StandardCharsets.UTF_8));
	// uploader.upload("someToken78678567", BINARY, "NOT_FOUND", stream, resourceType);
	// }
	//
	// @Test(expectedExceptions = DataMissingException.class)
	// public void testUploadStreamIsMissing() {
	// RecordStorageCreateUpdateSpy recordStorageSpy = new RecordStorageCreateUpdateSpy();
	// recordStorage = recordStorageSpy;
	// setUpDependencyProvider();
	// uploader.upload("someToken78678567", BINARY, "image:123456789", null, resourceType);
	//
	// }
	//
	// @Test(expectedExceptions = DataMissingException.class)
	// public void testUploadFileNameIsMissing() {
	// RecordStorageCreateUpdateSpy recordStorageSpy = new RecordStorageCreateUpdateSpy();
	// recordStorage = recordStorageSpy;
	// setUpDependencyProvider();
	// InputStream stream = new ByteArrayInputStream("a string".getBytes(StandardCharsets.UTF_8));
	// uploader.upload("someToken78678567", BINARY, "image:123456789", stream, resourceType);
	//
	// }
	//
	// @Test(expectedExceptions = DataMissingException.class)
	// public void testUploadFileNameIsEmpty() {
	// RecordStorageCreateUpdateSpy recordStorageSpy = new RecordStorageCreateUpdateSpy();
	// recordStorage = recordStorageSpy;
	// setUpDependencyProvider();
	// InputStream stream = new ByteArrayInputStream("a string".getBytes(StandardCharsets.UTF_8));
	// uploader.upload("someToken78678567", BINARY, "image:123456789", stream, resourceType);
	//
	// }
	//
	// @Test(expectedExceptions = RecordNotFoundException.class)
	// public void testNonExistingRecordType() {
	// InputStream stream = new ByteArrayInputStream("a string".getBytes(StandardCharsets.UTF_8));
	// uploader.upload("someToken78678567", "recordType_NOT_EXISTING", "id", stream, resourceType);
	// }
	//
	// @Test(expectedExceptions = AuthorizationException.class)
	// public void testUpdateRecordUserNotAuthorisedToUpdateData() {
	//
	// Uploader uploader = setupWithUserNotAuthorized();
	// InputStream stream = new ByteArrayInputStream("a string".getBytes(StandardCharsets.UTF_8));
	// uploader.upload("someToken78678567", BINARY, "image:123456789", stream, resourceType);
	// }
	//
	// private Uploader setupWithUserNotAuthorized() {
	// authorizator.authorizedForActionAndRecordType = false;
	//
	// Uploader uploader = UploaderImp.usingDependencyProvider(dependencyProvider);
	// return uploader;
	// }

}
