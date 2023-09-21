/*
 * Copyright 2016 Olov McKie
 * Copyright 2016, 2017, 2019, 2023 Uppsala University Library
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
import static org.testng.Assert.assertNotNull;
import static org.testng.Assert.assertTrue;
import static org.testng.Assert.fail;

import java.text.MessageFormat;
import java.util.List;

import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import se.uu.ub.cora.data.DataProvider;
import se.uu.ub.cora.data.spies.DataFactorySpy;
import se.uu.ub.cora.data.spies.DataGroupSpy;
import se.uu.ub.cora.data.spies.DataRecordGroupSpy;
import se.uu.ub.cora.spider.authentication.AuthenticationException;
import se.uu.ub.cora.spider.authorization.AuthorizationException;
import se.uu.ub.cora.spider.data.ResourceInputStream;
import se.uu.ub.cora.spider.dependency.spy.RecordTypeHandlerSpy;
import se.uu.ub.cora.spider.dependency.spy.ResourceArchiveSpy;
import se.uu.ub.cora.spider.log.LoggerFactorySpy;
import se.uu.ub.cora.spider.record.DataCopierFactorySpy;
import se.uu.ub.cora.spider.record.Downloader;
import se.uu.ub.cora.spider.record.MisuseException;
import se.uu.ub.cora.spider.record.StreamStorageSpy;
import se.uu.ub.cora.spider.spy.SpiderDependencyProviderSpy;
import se.uu.ub.cora.storage.RecordNotFoundException;
import se.uu.ub.cora.storage.spies.RecordStorageSpy;

public class DownloaderTest {
	private static final String SOME_MIME_TYPE = "someMimeType";
	private static final String RESOURCE_TYPE_MASTER = "master";
	private static final String SOME_RECORD_ID = "someId";
	private static final String SOME_AUTH_TOKEN = "someAuthToken";
	private static final String BINARY_RECORD_TYPE = "binary";
	private static final String ACTION_DOWNLOAD = "download";
	private static final String SOME_RESOURCE_TYPE = "someOtherResourceType";
	private static final String SOME_EXCEPTION_MESSAGE = "someExceptionMessage";
	private static final String SOME_RECORD_TYPE = "someRecordType";
	private static final String ERR_MESSAGE_MISUSE = "Downloading error: Invalid record type, "
			+ "for type {0} and {1}, must be (binary).";
	private static final String SOME_FILE_SIZE = "2123";
	private static final String ORIGINAL_FILE_NAME = "someOriginalFilename";
	private AuthenticatorSpy authenticator;
	private SpiderAuthorizatorSpy authorizator;
	private RecordStorageSpy recordStorage;
	private StreamStorageSpy streamStorage;
	private Downloader downloader;
	private SpiderDependencyProviderSpy dependencyProvider;
	private LoggerFactorySpy loggerFactorySpy;
	private DataCopierFactorySpy dataCopierFactory;

	private DataFactorySpy dataFactorySpy;
	private ResourceArchiveSpy resourceArchive;

	private RecordTypeHandlerSpy recordTypeHandler;
	private DataGroupSpy masterDGS;
	private DataGroupSpy resourceInfoDGS;
	private DataRecordGroupSpy readBinaryDGS;

	@BeforeMethod
	public void beforeMethod() {
		// setUpFactoriesAndProviders();
		dataFactorySpy = new DataFactorySpy();
		DataProvider.onlyForTestSetDataFactory(dataFactorySpy);

		// authenticator = new OldAuthenticatorSpy();
		// authorizator = new OldSpiderAuthorizatorSpy();
		// recordStorage = TestDataRecordInMemoryStorage.createRecordStorageInMemoryWithTestData();
		// streamStorage = new StreamStorageSpy();

		dependencyProvider = new SpiderDependencyProviderSpy();
		setUpDependencyProvider();
		downloader = DownloaderImp.usingDependencyProvider(dependencyProvider);

		setupBinaryRecord();
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

	}

	private void setupBinaryRecord() {
		masterDGS = new DataGroupSpy();
		masterDGS.MRV.setSpecificReturnValuesSupplier("getFirstAtomicValueWithNameInData",
				() -> SOME_FILE_SIZE, "fileSize");
		masterDGS.MRV.setSpecificReturnValuesSupplier("getFirstAtomicValueWithNameInData",
				() -> SOME_MIME_TYPE, "mimeType");

		resourceInfoDGS = new DataGroupSpy();
		resourceInfoDGS.MRV.setSpecificReturnValuesSupplier("getFirstGroupWithNameInData",
				() -> masterDGS, "master");

		readBinaryDGS = new DataRecordGroupSpy();
		readBinaryDGS.MRV.setSpecificReturnValuesSupplier("getFirstGroupWithNameInData",
				() -> resourceInfoDGS, "resourceInfo");
		readBinaryDGS.MRV.setSpecificReturnValuesSupplier("getFirstAtomicValueWithNameInData",
				() -> ORIGINAL_FILE_NAME, "originalFileName");

		dataFactorySpy.MRV.setDefaultReturnValuesSupplier("factorRecordGroupFromDataGroup",
				() -> readBinaryDGS);
	}

	@Test
	public void testDownloadMustReturnNonEmptyInputSreamInit() {
		var downloadedResource = downloader.download(SOME_AUTH_TOKEN, BINARY_RECORD_TYPE,
				SOME_RECORD_ID, RESOURCE_TYPE_MASTER);
		assertNotNull(downloadedResource);
		assertTrue(downloadedResource instanceof ResourceInputStream);
	}

	@Test
	public void testReadRelatedResourceFromArchive() throws Exception {
		ResourceInputStream downloadResource = downloader.download(SOME_AUTH_TOKEN,
				BINARY_RECORD_TYPE, SOME_RECORD_ID, RESOURCE_TYPE_MASTER);

		dependencyProvider.MCR.assertParameters("getRecordStorage", 0);
		recordStorage.MCR.assertParameterAsEqual("read", 0, "types", List.of(BINARY_RECORD_TYPE));
		recordStorage.MCR.assertParameter("read", 0, "id", SOME_RECORD_ID);
		DataGroupSpy readDataGroup = (DataGroupSpy) recordStorage.MCR.getReturnValue("read", 0);
		dataFactorySpy.MCR.assertParameters("factorRecordGroupFromDataGroup", 0, readDataGroup);
		DataRecordGroupSpy readDataRecordGroup = (DataRecordGroupSpy) dataFactorySpy.MCR
				.getReturnValue("factorRecordGroupFromDataGroup", 0);

		readDataRecordGroup.MCR.assertParameters("getDataDivider", 0);
		var dataDivider = readDataRecordGroup.MCR.getReturnValue("getDataDivider", 0);

		dependencyProvider.MCR.assertParameters("getResourceArchive", 0);
		resourceArchive.MCR.assertParameters("read", 0, dataDivider, BINARY_RECORD_TYPE,
				SOME_RECORD_ID);
		resourceArchive.MCR.assertReturn("read", 0, downloadResource.stream);
	}

	@Test
	public void testReadRecordNotFound() throws Exception {
		// TODO: We should use an exception from Spider.
		recordStorage.MRV.setAlwaysThrowException("read",
				RecordNotFoundException.withMessage(SOME_EXCEPTION_MESSAGE));
		try {
			downloader.download(SOME_AUTH_TOKEN, BINARY_RECORD_TYPE, SOME_RECORD_ID,
					RESOURCE_TYPE_MASTER);
			fail("It should throw Exception");
		} catch (Exception e) {
			assertTrue(e instanceof RecordNotFoundException,
					"AuthenticationException should be thrown");
			assertEquals(e.getMessage(), SOME_EXCEPTION_MESSAGE);
		}

	}

	@Test
	public void testDownloadAuthenticate() throws Exception {
		downloader.download(SOME_AUTH_TOKEN, BINARY_RECORD_TYPE, SOME_RECORD_ID,
				RESOURCE_TYPE_MASTER);

		dependencyProvider.MCR.assertParameters("getAuthenticator", 0);
		authenticator.MCR.assertParameters("getUserForToken", 0, SOME_AUTH_TOKEN);
	}

	@Test
	public void testUploadUserNotAuthenticated() throws Exception {
		authenticator.MRV.setAlwaysThrowException("getUserForToken",
				new AuthenticationException(SOME_EXCEPTION_MESSAGE));
		try {
			downloader.download(SOME_AUTH_TOKEN, BINARY_RECORD_TYPE, SOME_RECORD_ID,
					RESOURCE_TYPE_MASTER);
			fail("It should throw Exception");
		} catch (Exception e) {
			assertTrue(e instanceof AuthenticationException,
					"AuthenticationException should be thrown");
			assertEquals(e.getMessage(), SOME_EXCEPTION_MESSAGE);
		}
	}

	@Test
	public void testUploadUserNotAuthorizated() throws Exception {
		authorizator.MRV.setAlwaysThrowException("checkUserIsAuthorizedForActionOnRecordType",
				new AuthorizationException(SOME_EXCEPTION_MESSAGE));
		try {
			downloader.download(SOME_AUTH_TOKEN, BINARY_RECORD_TYPE, SOME_RECORD_ID,
					RESOURCE_TYPE_MASTER);
			fail("It should throw Exception");
		} catch (Exception e) {
			assertTrue(e instanceof AuthorizationException,
					"AuthenticationException should be thrown");
			assertEquals(e.getMessage(), SOME_EXCEPTION_MESSAGE);
		}
	}

	@Test
	public void testUploadUserAuthorized() throws Exception {
		downloader.download(SOME_AUTH_TOKEN, BINARY_RECORD_TYPE, SOME_RECORD_ID,
				RESOURCE_TYPE_MASTER);

		var user = authenticator.MCR.getReturnValue("getUserForToken", 0);

		dependencyProvider.MCR.assertParameters("getSpiderAuthorizator", 0);
		authorizator.MCR.assertParameters("checkUserIsAuthorizedForActionOnRecordType", 0, user,
				ACTION_DOWNLOAD, BINARY_RECORD_TYPE + "." + RESOURCE_TYPE_MASTER);
	}

	@Test
	public void testDownloadExceptionResourceTypeDifferentThanMaster() throws Exception {
		try {
			downloader.download(SOME_AUTH_TOKEN, BINARY_RECORD_TYPE, SOME_RECORD_ID,
					SOME_RESOURCE_TYPE);
			fail("It should throw exception");
		} catch (Exception e) {
			assertTrue(e instanceof MisuseException);
			assertEquals(e.getMessage(),
					"Not implemented yet for resource type different than master.");
			ensureNoDownloadLogicStarts();
		}
	}

	private void ensureNoDownloadLogicStarts() {
		authenticator.MCR.assertMethodNotCalled("getUserForToken");
		authorizator.MCR.assertMethodNotCalled("checkUserIsAuthorizedForActionOnRecordType");
		recordStorage.MCR.assertMethodNotCalled("read");
		resourceArchive.MCR.assertMethodNotCalled("create");
	}

	@Test
	public void testUploadExceptionTypeDifferentThanBinary() throws Exception {
		try {
			downloader.download(SOME_AUTH_TOKEN, SOME_RECORD_TYPE, SOME_RECORD_ID,
					RESOURCE_TYPE_MASTER);
			fail("It should throw exception");
		} catch (Exception e) {
			assertTrue(e instanceof MisuseException);
			assertEquals(e.getMessage(),
					MessageFormat.format(ERR_MESSAGE_MISUSE, SOME_RECORD_TYPE, SOME_RECORD_ID));

			ensureNoDownloadLogicStarts();
		}
	}

	@Test
	public void testReturnCorrectInfo() throws Exception {

		ResourceInputStream resourceDownloaded = downloader.download(SOME_AUTH_TOKEN,
				BINARY_RECORD_TYPE, SOME_RECORD_ID, RESOURCE_TYPE_MASTER);

		readBinaryDGS.MCR.assertParameters("getFirstAtomicValueWithNameInData", 0,
				"originalFileName");
		readBinaryDGS.MCR.getReturnValue("getFirstAtomicValueWithNameInData", 0);
		readBinaryDGS.MCR.assertParameters("getFirstGroupWithNameInData", 0, "resourceInfo");
		resourceInfoDGS.MCR.assertParameters("getFirstGroupWithNameInData", 0, "master");
		masterDGS.MCR.assertParameters("getFirstAtomicValueWithNameInData", 0, "fileSize");
		masterDGS.MCR.assertParameters("getFirstAtomicValueWithNameInData", 1, "mimeType");

		assertEquals(resourceDownloaded.name, ORIGINAL_FILE_NAME);
		assertEquals(String.valueOf(resourceDownloaded.size), SOME_FILE_SIZE);
		assertEquals(resourceDownloaded.mimeType, SOME_MIME_TYPE);

	}

}

// @Test
// public void testUnauthorizedForDownloadOnRecordTypeShouldShouldNotAccessStorage() {
// recordStorage = new OldRecordStorageSpy();
// authorizator.authorizedForActionAndRecordType = false;
// setUpDependencyProvider();
//
// boolean exceptionWasCaught = false;
// try {
// downloader.download("someToken78678567", BINARY, "image:123456789", "master");
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
// @Test
// public void testExternalDependenciesAreCalled() {
// InputStream stream = new ByteArrayInputStream("a string".getBytes(StandardCharsets.UTF_8));
// streamStorage.stream = stream;
//
// downloader.download("someToken78678567", BINARY, "image:123456789", "master");
//
// authorizator.MCR.assertMethodWasCalled("checkUserIsAuthorizedForActionOnRecordType");
// }
//
// @Test(expectedExceptions = AuthenticationException.class)
// public void testAuthenticationNotAuthenticated() {
// authenticator.throwAuthenticationException = true;
// recordStorage = new OldRecordStorageSpy();
// setUpDependencyProvider();
// downloader.download("dummyNonAuthenticatedToken", BINARY, "image:123456789", "master");
// }
//
// @Test
// public void testDownloadStream() {
// InputStream stream = new ByteArrayInputStream("a string".getBytes(StandardCharsets.UTF_8));
// streamStorage.stream = stream;
//
// ResourceInputStream spiderStream = downloader.download("someToken78678567", BINARY,
// "image:123456789", "master");
//
// assertEquals(spiderStream.stream, stream);
// assertEquals(spiderStream.name, "adele.png");
// assertEquals(spiderStream.size, 123);
// assertEquals(spiderStream.mimeType, "application/octet-stream");
// }
//
// @Test
// public void testDownloadStreamStorageCalledCorrectly() {
// InputStream stream = new ByteArrayInputStream("a string".getBytes(StandardCharsets.UTF_8));
// streamStorage.stream = stream;
//
// downloader.download("someToken78678567", BINARY, "image:123456789", "master");
//
// assertEquals(streamStorage.streamId, "678912345");
// assertEquals(streamStorage.dataDivider, "cora");
// }
//
// @Test(expectedExceptions = MisuseException.class)
// public void testDownloadStreamNotChildOfBinary() {
// downloader.download("someToken78678567", "place", "place:0002", "master");
// }
//
// @Test(expectedExceptions = MisuseException.class)
// public void testDownloadStreamNotChildOfBinary2() {
//
// downloader.download("someToken78678567", "recordTypeAutoGeneratedId", "someId", "master");
// }
//
// @Test(expectedExceptions = RecordNotFoundException.class)
// public void testDownloadNotFound() {
// downloader.download("someToken78678567", BINARY, "NOT_FOUND", "master");
// }
//
// @Test(expectedExceptions = DataMissingException.class)
// public void testDownloadResourceIsMissing() {
// downloader.download("someToken78678567", BINARY, "image:123456789", null);
//
// }
//
// @Test(expectedExceptions = DataMissingException.class)
// public void testDownloadResourceIsEmpty() {
// downloader.download("someToken78678567", BINARY, "image:123456789", "");
// }
//
// @Test(expectedExceptions = RecordNotFoundException.class)
// public void testDownloadResourceDoesNotExistInRecord() {
// downloader.download("someToken78678567", BINARY, "image:123456789", "NonExistingResource");
// }
//
// @Test
// public void testDownloadResourceDoesNotExistInRecordExceptionInitialIsSentAlong() {
// try {
// downloader.download("someToken78678567", BINARY, "image:123456789",
// "NonExistingResource");
// } catch (Exception e) {
// assertTrue(e.getCause() instanceof DataMissingException);
// }
// }
//
// @Test(expectedExceptions = RecordNotFoundException.class)
// public void testNonExistingRecordType() {
// downloader.download("someToken78678567", "image_NOT_EXISTING", "image:123456789", "master");
// }
// }
