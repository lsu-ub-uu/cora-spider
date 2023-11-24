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
import se.uu.ub.cora.spider.log.LoggerFactorySpy;
import se.uu.ub.cora.spider.record.DataCopierFactorySpy;
import se.uu.ub.cora.spider.record.Downloader;
import se.uu.ub.cora.spider.record.MisuseException;
import se.uu.ub.cora.spider.spy.SpiderDependencyProviderSpy;
import se.uu.ub.cora.spider.spy.StreamStorageSpy;
import se.uu.ub.cora.storage.RecordNotFoundException;
import se.uu.ub.cora.storage.spies.RecordStorageSpy;
import se.uu.ub.cora.storage.spies.archive.ResourceArchiveSpy;

public class DownloaderTest {
	private static final String SOME_DATA_DIVIDER = "someDataDivider";
	private static final String SOME_MIME_TYPE = "someMimeType";
	private static final String REPRESENTATION_MASTER = "master";
	private static final String REPRESENTATION_THUMBNAIL = "thumbnail";
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
	// private OldStreamStorageSpy streamStorage;
	private Downloader downloader;
	private SpiderDependencyProviderSpy dependencyProvider;
	private LoggerFactorySpy loggerFactorySpy;
	private DataCopierFactorySpy dataCopierFactory;

	private DataFactorySpy dataFactorySpy;
	private ResourceArchiveSpy resourceArchive;

	private RecordTypeHandlerSpy recordTypeHandler;
	private DataGroupSpy resourceTypeDGS;
	private DataGroupSpy thumbnailDGS;
	private DataGroupSpy resourceInfoDGS;
	private DataRecordGroupSpy readBinaryDGS;
	private StreamStorageSpy streamStorage;

	@BeforeMethod
	public void beforeMethod() {
		dataFactorySpy = new DataFactorySpy();
		DataProvider.onlyForTestSetDataFactory(dataFactorySpy);

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

		streamStorage = new StreamStorageSpy();
		dependencyProvider.MRV.setDefaultReturnValuesSupplier("getStreamStorage",
				() -> streamStorage);

	}

	private void setupBinaryRecord() {
		resourceTypeDGS = new DataGroupSpy();
		resourceTypeDGS.MRV.setSpecificReturnValuesSupplier("getFirstAtomicValueWithNameInData",
				() -> SOME_FILE_SIZE, "fileSize");
		resourceTypeDGS.MRV.setSpecificReturnValuesSupplier("getFirstAtomicValueWithNameInData",
				() -> SOME_MIME_TYPE, "mimeType");

		resourceInfoDGS = new DataGroupSpy();
		resourceInfoDGS.MRV.setSpecificReturnValuesSupplier("getFirstGroupWithNameInData",
				() -> resourceTypeDGS, "master");
		resourceInfoDGS.MRV.setSpecificReturnValuesSupplier("getFirstGroupWithNameInData",
				() -> resourceTypeDGS, "thumbnail");
		resourceInfoDGS.MRV.setSpecificReturnValuesSupplier("getFirstGroupWithNameInData",
				() -> resourceTypeDGS, "medium");
		resourceInfoDGS.MRV.setSpecificReturnValuesSupplier("getFirstGroupWithNameInData",
				() -> resourceTypeDGS, "large");

		readBinaryDGS = new DataRecordGroupSpy();
		readBinaryDGS.MRV.setSpecificReturnValuesSupplier("getFirstGroupWithNameInData",
				() -> resourceInfoDGS, "resourceInfo");
		readBinaryDGS.MRV.setSpecificReturnValuesSupplier("getFirstAtomicValueWithNameInData",
				() -> ORIGINAL_FILE_NAME, "originalFileName");
		readBinaryDGS.MRV.setDefaultReturnValuesSupplier("getDataDivider", () -> SOME_DATA_DIVIDER);

		recordStorage.MRV.setDefaultReturnValuesSupplier("read", () -> readBinaryDGS);
	}

	@Test
	public void testDownloadMustReturnNonEmptyInputSreamInit() {
		var downloadedResource = downloader.download(SOME_AUTH_TOKEN, BINARY_RECORD_TYPE,
				SOME_RECORD_ID, REPRESENTATION_MASTER);
		assertNotNull(downloadedResource);
		assertTrue(downloadedResource instanceof ResourceInputStream);
	}

	@Test
	public void testReadRelatedResourceFromArchive() throws Exception {
		ResourceInputStream downloadResource = downloader.download(SOME_AUTH_TOKEN,
				BINARY_RECORD_TYPE, SOME_RECORD_ID, REPRESENTATION_MASTER);

		dependencyProvider.MCR.assertParameters("getRecordStorage", 0);
		recordStorage.MCR.assertParameters("read", 0, BINARY_RECORD_TYPE, SOME_RECORD_ID);
		recordStorage.MCR.assertParameter("read", 0, "id", SOME_RECORD_ID);
		DataRecordGroupSpy readDataRecordGroup = (DataRecordGroupSpy) recordStorage.MCR
				.getReturnValue("read", 0);

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
					REPRESENTATION_MASTER);
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
				REPRESENTATION_MASTER);

		dependencyProvider.MCR.assertParameters("getAuthenticator", 0);
		authenticator.MCR.assertParameters("getUserForToken", 0, SOME_AUTH_TOKEN);
	}

	@Test
	public void testUploadUserNotAuthenticated() throws Exception {
		authenticator.MRV.setAlwaysThrowException("getUserForToken",
				new AuthenticationException(SOME_EXCEPTION_MESSAGE));
		try {
			downloader.download(SOME_AUTH_TOKEN, BINARY_RECORD_TYPE, SOME_RECORD_ID,
					REPRESENTATION_MASTER);
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
					REPRESENTATION_MASTER);
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
				REPRESENTATION_MASTER);

		var user = authenticator.MCR.getReturnValue("getUserForToken", 0);

		dependencyProvider.MCR.assertParameters("getSpiderAuthorizator", 0);
		authorizator.MCR.assertParameters("checkUserIsAuthorizedForActionOnRecordType", 0, user,
				ACTION_DOWNLOAD, BINARY_RECORD_TYPE + "." + REPRESENTATION_MASTER);
	}

	@Test
	public void testDownloadExceptionResourceTypeOtherThanKnow() throws Exception {
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
					REPRESENTATION_MASTER);
			fail("It should throw exception");
		} catch (Exception e) {
			assertTrue(e instanceof MisuseException);
			assertEquals(e.getMessage(),
					MessageFormat.format(ERR_MESSAGE_MISUSE, SOME_RECORD_TYPE, SOME_RECORD_ID));

			ensureNoDownloadLogicStarts();
		}
	}

	@Test
	public void testReturnCorrectInfoForMaster() throws Exception {

		ResourceInputStream resourceDownloaded = downloader.download(SOME_AUTH_TOKEN,
				BINARY_RECORD_TYPE, SOME_RECORD_ID, REPRESENTATION_MASTER);
		assertReturnedDataFromCorrectResourceType("master", resourceDownloaded);

	}

	private void assertReturnedDataFromCorrectResourceType(String resourceType,
			ResourceInputStream resourceDownloaded) {
		readBinaryDGS.MCR.assertParameters("getFirstAtomicValueWithNameInData", 0,
				"originalFileName");
		readBinaryDGS.MCR.getReturnValue("getFirstAtomicValueWithNameInData", 0);
		readBinaryDGS.MCR.assertParameters("getFirstGroupWithNameInData", 0, "resourceInfo");
		resourceInfoDGS.MCR.assertParameters("getFirstGroupWithNameInData", 0, resourceType);
		resourceTypeDGS.MCR.assertParameters("getFirstAtomicValueWithNameInData", 0, "fileSize");
		resourceTypeDGS.MCR.assertParameters("getFirstAtomicValueWithNameInData", 1, "mimeType");

		assertEquals(resourceDownloaded.name, ORIGINAL_FILE_NAME);
		assertEquals(String.valueOf(resourceDownloaded.size), SOME_FILE_SIZE);
		assertEquals(resourceDownloaded.mimeType, SOME_MIME_TYPE);
	}

	@Test
	public void testDownloadAThumbnail() throws Exception {
		ResourceInputStream resourceDownloaded = downloader.download(SOME_AUTH_TOKEN,
				BINARY_RECORD_TYPE, SOME_RECORD_ID, REPRESENTATION_THUMBNAIL);

		streamStorage.MCR.assertParameters("retrieve", 0, SOME_RECORD_ID + "-thumbnail",
				SOME_DATA_DIVIDER);
		streamStorage.MCR.assertReturn("retrieve", 0, resourceDownloaded.stream);

		assertReturnedDataFromCorrectResourceType(REPRESENTATION_THUMBNAIL, resourceDownloaded);
	}

	@Test
	public void testDownloadAMedium() throws Exception {
		ResourceInputStream resourceDownloaded = downloader.download(SOME_AUTH_TOKEN,
				BINARY_RECORD_TYPE, SOME_RECORD_ID, "medium");

		streamStorage.MCR.assertParameters("retrieve", 0, SOME_RECORD_ID + "-medium",
				SOME_DATA_DIVIDER);
		streamStorage.MCR.assertReturn("retrieve", 0, resourceDownloaded.stream);

		assertReturnedDataFromCorrectResourceType("medium", resourceDownloaded);
	}

	@Test
	public void testDownloadALarge() throws Exception {
		ResourceInputStream resourceDownloaded = downloader.download(SOME_AUTH_TOKEN,
				BINARY_RECORD_TYPE, SOME_RECORD_ID, "large");

		streamStorage.MCR.assertParameters("retrieve", 0, SOME_RECORD_ID + "-large",
				SOME_DATA_DIVIDER);
		streamStorage.MCR.assertReturn("retrieve", 0, resourceDownloaded.stream);

		assertReturnedDataFromCorrectResourceType("large", resourceDownloaded);
	}
}
