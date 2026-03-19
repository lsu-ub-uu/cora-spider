/*
 * Copyright 2016, 2017, 2019, 2023, 2024, 2026 Uppsala University Library
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

package se.uu.ub.cora.spider.binary;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertNotNull;
import static org.testng.Assert.assertTrue;
import static org.testng.Assert.fail;

import java.text.MessageFormat;
import java.util.Optional;

import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import se.uu.ub.cora.data.DataProvider;
import se.uu.ub.cora.data.collected.CollectTerms;
import se.uu.ub.cora.data.spies.DataFactorySpy;
import se.uu.ub.cora.data.spies.DataGroupSpy;
import se.uu.ub.cora.data.spies.DataRecordGroupSpy;
import se.uu.ub.cora.data.spies.DataRecordLinkSpy;
import se.uu.ub.cora.spider.authentication.AuthenticationException;
import se.uu.ub.cora.spider.authorization.AuthorizationException;
import se.uu.ub.cora.spider.binary.internal.DownloaderImp;
import se.uu.ub.cora.spider.dependency.spy.RecordTypeHandlerSpy;
import se.uu.ub.cora.spider.record.InternalDataMismatchException;
import se.uu.ub.cora.spider.record.MisuseException;
import se.uu.ub.cora.spider.record.RecordNotFoundException;
import se.uu.ub.cora.spider.record.ResourceNotFoundException;
import se.uu.ub.cora.spider.record.internal.AuthenticatorSpy;
import se.uu.ub.cora.spider.record.internal.SpiderAuthorizatorSpy;
import se.uu.ub.cora.spider.spy.DataGroupTermCollectorSpy;
import se.uu.ub.cora.spider.spy.SpiderDependencyProviderSpy;
import se.uu.ub.cora.storage.spies.RecordStorageSpy;
import se.uu.ub.cora.storage.spies.StreamStorageSpy;
import se.uu.ub.cora.storage.spies.archive.ResourceArchiveSpy;

public class DownloaderTest {
	private static final String DATA_DIVIDER = "someDataDivider";
	private static final String MIME_TYPE = "someMimeType";
	private static final String MASTER = "master";
	private static final String THUMBNAIL = "thumbnail";
	private static final String MEDIUM = "medium";
	private static final String LARGE = "large";
	private static final String JP2 = "jp2";
	private static final String RECORD_TYPE = "someRecordType";
	private static final String RECORD_ID = "someId";
	private static final String AUTH_TOKEN = "someAuthToken";
	private static final String BINARY_RECORD_TYPE = "binary";
	private static final String ACTION_READ = "read";
	private static final String REPRESENTATION = "someOtherResourceType";
	private static final String EXCEPTION_MESSAGE = "someExceptionMessage";
	private static final String ERR_MESSAGE_MISUSE = "Downloading error: Invalid record type, "
			+ "for type {0} and {1}, must be (binary).";
	private static final String FILE_SIZE = "2123";
	private static final String ORIGINAL_FILE_NAME = "someOriginalFilename";
	private AuthenticatorSpy authenticator;
	private SpiderAuthorizatorSpy authorizator;
	private RecordStorageSpy recordStorage;
	private Downloader downloader;
	private SpiderDependencyProviderSpy dependencyProvider;

	private DataFactorySpy dataFactorySpy;
	private ResourceArchiveSpy resourceArchive;

	private DataRecordGroupSpy readBinaryDGS;
	private StreamStorageSpy streamStorage;
	private DataGroupSpy thumbnailResourceTypeDGS;
	private DataGroupSpy mediumResourceTypeDGS;
	private DataGroupSpy largeResourceTypeDGS;
	private DataGroupSpy jp2ResourceTypeDGS;
	private DataGroupSpy masterResourceTypeDGS;
	private DataRecordGroupSpy hostRecordDGS;
	private RecordTypeHandlerSpy recordTypeHandler;

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

		recordTypeHandler = new RecordTypeHandlerSpy();
		dependencyProvider.MRV.setDefaultReturnValuesSupplier(
				"getRecordTypeHandlerUsingDataRecordGroup", () -> recordTypeHandler);
	}

	private void setupBinaryRecord() {
		masterResourceTypeDGS = createResourceDataGroupForResourceId(MASTER);
		thumbnailResourceTypeDGS = createResourceDataGroupForResourceId(THUMBNAIL);
		mediumResourceTypeDGS = createResourceDataGroupForResourceId(MEDIUM);
		largeResourceTypeDGS = createResourceDataGroupForResourceId(LARGE);
		jp2ResourceTypeDGS = createResourceDataGroupForResourceId(JP2);

		readBinaryDGS = new DataRecordGroupSpy();
		setHostRecord();

		readBinaryDGS.MRV.setSpecificReturnValuesSupplier("getFirstGroupWithNameInData",
				() -> masterResourceTypeDGS, MASTER);
		readBinaryDGS.MRV.setSpecificReturnValuesSupplier("getFirstGroupWithNameInData",
				() -> thumbnailResourceTypeDGS, THUMBNAIL);
		readBinaryDGS.MRV.setSpecificReturnValuesSupplier("getFirstGroupWithNameInData",
				() -> mediumResourceTypeDGS, MEDIUM);
		readBinaryDGS.MRV.setSpecificReturnValuesSupplier("getFirstGroupWithNameInData",
				() -> largeResourceTypeDGS, LARGE);
		readBinaryDGS.MRV.setSpecificReturnValuesSupplier("getFirstGroupWithNameInData",
				() -> jp2ResourceTypeDGS, JP2);
		readBinaryDGS.MRV.setDefaultReturnValuesSupplier("containsChildWithNameInData", () -> true);

		readBinaryDGS.MRV.setSpecificReturnValuesSupplier("getFirstAtomicValueWithNameInData",
				() -> ORIGINAL_FILE_NAME, "originalFileName");
		readBinaryDGS.MRV.setDefaultReturnValuesSupplier("getDataDivider", () -> DATA_DIVIDER);
		recordStorage.MRV.setDefaultReturnValuesSupplier("read", () -> readBinaryDGS);

		hostRecordDGS = new DataRecordGroupSpy();
		hostRecordDGS.MRV.setDefaultReturnValuesSupplier("getType", () -> "someHostType");

		recordStorage.MRV.setSpecificReturnValuesSupplier("read", () -> hostRecordDGS,
				"someHostType", "someHostId");
	}

	private void setHostRecord() {
		DataRecordLinkSpy hostLink = new DataRecordLinkSpy();
		hostLink.MRV.setDefaultReturnValuesSupplier("getLinkedRecordType", () -> "someHostType");
		hostLink.MRV.setDefaultReturnValuesSupplier("getLinkedRecordId", () -> "someHostId");

		readBinaryDGS.MRV.setDefaultReturnValuesSupplier("getHostRecord",
				() -> Optional.of(hostLink));
	}

	private DataGroupSpy createResourceDataGroupForResourceId(String resourceId) {
		DataGroupSpy resourceTypeDG = new DataGroupSpy();
		resourceTypeDG.MRV.setSpecificReturnValuesSupplier("getFirstAtomicValueWithNameInData",
				() -> FILE_SIZE, "fileSize");
		resourceTypeDG.MRV.setSpecificReturnValuesSupplier("getFirstAtomicValueWithNameInData",
				() -> MIME_TYPE, "mimeType");
		resourceTypeDG.MRV.setSpecificReturnValuesSupplier("getFirstAtomicValueWithNameInData",
				() -> "someId-" + resourceId, "resourceId");
		return resourceTypeDG;
	}

	@Test
	public void testDownloadMustReturnNonEmptyInputStreamInit() {
		var downloadedResource = downloader.download(AUTH_TOKEN, BINARY_RECORD_TYPE, RECORD_ID,
				MASTER);
		assertNotNull(downloadedResource);
		assertTrue(downloadedResource instanceof ResourceInputStream);
	}

	@Test
	public void testReadRelatedResourceFromArchive() {
		ResourceInputStream downloadResource = downloader.download(AUTH_TOKEN, BINARY_RECORD_TYPE,
				RECORD_ID, MASTER);

		dependencyProvider.MCR.assertParameters("getRecordStorage", 0);
		recordStorage.MCR.assertParameters("read", 0, BINARY_RECORD_TYPE, RECORD_ID);
		recordStorage.MCR.assertParameter("read", 0, "id", RECORD_ID);
		DataRecordGroupSpy readDataRecordGroup = (DataRecordGroupSpy) recordStorage.MCR
				.getReturnValue("read", 0);

		readDataRecordGroup.MCR.assertParameters("getDataDivider", 0);
		var dataDivider = readDataRecordGroup.MCR.getReturnValue("getDataDivider", 0);

		dependencyProvider.MCR.assertParameters("getResourceArchive", 0);
		resourceArchive.MCR.assertParameters("readMasterResource", 0, dataDivider,
				BINARY_RECORD_TYPE, RECORD_ID);
		resourceArchive.MCR.assertReturn("readMasterResource", 0, downloadResource.stream);
	}

	@Test
	public void testMasterResourceNotFound() {
		var resourceNotFoundException = se.uu.ub.cora.storage.ResourceNotFoundException
				.withMessage(ERR_MESSAGE_MISUSE);
		resourceArchive.MRV.setAlwaysThrowException("readMasterResource",
				resourceNotFoundException);

		try {
			downloader.download(AUTH_TOKEN, BINARY_RECORD_TYPE, RECORD_ID, MASTER);
			fail("It should throw a resourceNotFoundException");
		} catch (Exception e) {
			assertTrue(e instanceof ResourceNotFoundException);
			String errorNotFoundMessage = "Could not download the stream because it could not be"
					+ " found in storage. Type: {0}, id: {1} and representation: {2}";
			assertEquals(e.getMessage(), MessageFormat.format(errorNotFoundMessage,
					BINARY_RECORD_TYPE, RECORD_ID, MASTER));
			assertEquals(e.getCause(), resourceNotFoundException);
		}
	}

	@Test
	public void testReadRecordNotFound() {
		recordStorage.MRV.setAlwaysThrowException("read",
				se.uu.ub.cora.storage.RecordNotFoundException.withMessage(EXCEPTION_MESSAGE));
		try {
			downloader.download(AUTH_TOKEN, BINARY_RECORD_TYPE, RECORD_ID, MASTER);
			fail("It should throw Exception");
		} catch (Exception e) {
			assertTrue(e instanceof RecordNotFoundException);
			assertEquals(e.getMessage(), "Could not find record with type: " + BINARY_RECORD_TYPE
					+ " and id: " + RECORD_ID);
			assertEquals(e.getCause().getMessage(), EXCEPTION_MESSAGE);
		}
	}

	@Test
	public void testDownload_Published() {
		readBinaryDGS.MRV.setDefaultReturnValuesSupplier("getVisibility",
				() -> Optional.of("published"));

		var resourceDownloaded = downloader.download(AUTH_TOKEN, BINARY_RECORD_TYPE, RECORD_ID,
				JP2);

		assertIgnoreAuthenticationAndAuthorization();
		assertRepresentationAllowedUsingCallNo(resourceDownloaded, JP2, 0, jp2ResourceTypeDGS);

	}

	private void assertIgnoreAuthenticationAndAuthorization() {
		authenticator.MCR.assertMethodNotCalled("getUserForToken");
		authorizator.MCR.assertMethodNotCalled(
				"checkUserIsAuthorizedForActionOnRecordTypeAndCollectedData");
	}

	@Test
	public void testDownloadAuthenticate() {
		downloader.download(AUTH_TOKEN, BINARY_RECORD_TYPE, RECORD_ID, MASTER);

		dependencyProvider.MCR.assertParameters("getAuthenticator", 0);
		authenticator.MCR.assertParameters("getUserForToken", 0, AUTH_TOKEN);
	}

	@Test
	public void testDownloadUserNotAuthenticated() {
		authenticator.MRV.setAlwaysThrowException("getUserForToken",
				new AuthenticationException(EXCEPTION_MESSAGE));
		try {
			downloader.download(AUTH_TOKEN, BINARY_RECORD_TYPE, RECORD_ID, MASTER);
			fail("It should throw Exception");
		} catch (Exception e) {
			assertTrue(e instanceof AuthenticationException,
					"AuthenticationException should be thrown");
			assertEquals(e.getMessage(), EXCEPTION_MESSAGE);
		}
	}

	@Test
	public void testDownloadUserNotAuthorizated() {
		authorizator.MRV.setAlwaysThrowException(
				"checkUserIsAuthorizedForActionOnRecordTypeAndCollectedData",
				new AuthorizationException(EXCEPTION_MESSAGE));
		try {
			downloader.download(AUTH_TOKEN, BINARY_RECORD_TYPE, RECORD_ID, MASTER);
			fail("It should throw Exception");
		} catch (Exception e) {
			assertTrue(e instanceof AuthorizationException,
					"AuthenticationException should be thrown");
			assertEquals(e.getMessage(), EXCEPTION_MESSAGE);
		}
	}

	@Test
	public void testHostRecordMissing() {
		var originalError = new se.uu.ub.cora.data.DataMissingException("someError");
		readBinaryDGS.MRV.setAlwaysThrowException("getHostRecord", originalError);
		try {
			downloader.download(AUTH_TOKEN, BINARY_RECORD_TYPE, RECORD_ID, MASTER);
			fail();
		} catch (Exception e) {
			assertIntenalDataMissmatch(e, originalError);
		}
	}

	private void assertIntenalDataMissmatch(Exception e, Exception expectedException) {
		assertTrue(e instanceof InternalDataMismatchException);
		assertEquals(e.getMessage(), "Could not download the stream because of missing data. "
				+ "Type: binary, id: someId and representation: master, due to: someError");
		assertEquals(e.getCause(), expectedException);
	}

	@Test
	public void testDownloadUserAuthorized() {
		downloader.download(AUTH_TOKEN, BINARY_RECORD_TYPE, RECORD_ID, MASTER);

		assertCheckAuthorizedUsingHostRecordCollectedTerms();
	}

	private void assertCheckAuthorizedUsingHostRecordCollectedTerms() {
		CollectTerms collectedTerms = assertAndReturnUsedCollectedValuesFromHostRecord();

		var user = authenticator.MCR.getReturnValue("getUserForToken", 0);
		authorizator.MCR.assertParameters(
				"checkUserIsAuthorizedForActionOnRecordTypeAndCollectedData", 0, user, ACTION_READ,
				"someHostType." + BINARY_RECORD_TYPE, collectedTerms.permissionTerms);
	}

	private CollectTerms assertAndReturnUsedCollectedValuesFromHostRecord() {
		var definitionId = recordTypeHandler.MCR.getReturnValue("getDefinitionId", 0);

		DataGroupTermCollectorSpy termCollector = (DataGroupTermCollectorSpy) dependencyProvider.MCR
				.assertCalledParametersReturn("getDataGroupTermCollector");

		return (CollectTerms) termCollector.MCR.assertCalledParametersReturn("collectTerms",
				definitionId, hostRecordDGS);
	}

	@Test
	public void testDownloadExceptionResourceTypeOtherThanKnow() {
		try {
			downloader.download(AUTH_TOKEN, BINARY_RECORD_TYPE, RECORD_ID, REPRESENTATION);
			fail("It should throw exception");
		} catch (Exception e) {
			assertTrue(e instanceof MisuseException);
			assertEquals(e.getMessage(), "Representation " + REPRESENTATION + " does not exist.");
			ensureNoDownloadLogicStarts();
		}
	}

	private void ensureNoDownloadLogicStarts() {
		assertIgnoreAuthenticationAndAuthorization();
		recordStorage.MCR.assertMethodNotCalled("read");
		resourceArchive.MCR.assertMethodNotCalled("create");
	}

	@Test
	public void testDownloadExceptionTypeDifferentThanBinary() {
		try {
			downloader.download(AUTH_TOKEN, RECORD_TYPE, RECORD_ID, MASTER);
			fail("It should throw exception");
		} catch (Exception e) {
			assertTrue(e instanceof MisuseException);
			assertEquals(e.getMessage(),
					MessageFormat.format(ERR_MESSAGE_MISUSE, RECORD_TYPE, RECORD_ID));

			ensureNoDownloadLogicStarts();
		}
	}

	@Test
	public void testReturnCorrectInfoForMaster() {
		ResourceInputStream resourceDownloaded = downloader.download(AUTH_TOKEN, BINARY_RECORD_TYPE,
				RECORD_ID, MASTER);
		readBinaryDGS.MCR.assertParameters("getFirstGroupWithNameInData", 0, MASTER);

		assertReturnedDataFromCorrectResourceType(MASTER, resourceDownloaded,
				masterResourceTypeDGS);
	}

	private void assertReturnedDataFromCorrectResourceType(String resourceType,
			ResourceInputStream resourceDownloaded, DataGroupSpy resourceTypeDGS) {
		readBinaryDGS.MCR.assertParameters("getFirstGroupWithNameInData", 0, resourceType);
		resourceTypeDGS.MCR.assertParameters("getFirstAtomicValueWithNameInData", 0, "resourceId");
		var fileName = resourceTypeDGS.MCR.getReturnValue("getFirstAtomicValueWithNameInData", 0);
		resourceTypeDGS.MCR.assertParameters("getFirstAtomicValueWithNameInData", 1, "fileSize");
		resourceTypeDGS.MCR.assertParameters("getFirstAtomicValueWithNameInData", 2, "mimeType");

		assertEquals(resourceDownloaded.name, fileName);
		assertEquals(String.valueOf(resourceDownloaded.size), FILE_SIZE);
		assertEquals(resourceDownloaded.mimeType, MIME_TYPE);
	}

	@Test(expectedExceptions = ResourceNotFoundException.class, expectedExceptionsMessageRegExp = ""
			+ "Could not download the stream because the binary does not have the requested "
			+ "representation. Type: binary, id: someId and representation: thumbnail")
	public void testDownloadANonExistingRepresentation_thumbnail() {
		readBinaryDGS.MRV.setSpecificReturnValuesSupplier("containsChildWithNameInData",
				() -> false, THUMBNAIL);

		downloader.download(AUTH_TOKEN, BINARY_RECORD_TYPE, RECORD_ID, THUMBNAIL);
	}

	@Test(expectedExceptions = ResourceNotFoundException.class, expectedExceptionsMessageRegExp = ""
			+ "Could not download the stream because the binary does not have the requested "
			+ "representation. Type: binary, id: someId and representation: master")
	public void testDownloadANonExistingRepresentation_master() {
		readBinaryDGS.MRV.setSpecificReturnValuesSupplier("containsChildWithNameInData",
				() -> false, MASTER);

		downloader.download(AUTH_TOKEN, BINARY_RECORD_TYPE, RECORD_ID, MASTER);
	}

	@Test
	public void testDownloadAThumbnail() {
		var resourceDownloaded = downloader.download(AUTH_TOKEN, BINARY_RECORD_TYPE, RECORD_ID,
				THUMBNAIL);

		assertRepresentationAllowed(resourceDownloaded, THUMBNAIL, thumbnailResourceTypeDGS);
	}

	private void assertRepresentationAllowed(ResourceInputStream resourceDownloaded,
			String representation, DataGroupSpy resourceTypeDGS) {
		assertRepresentationAllowedUsingCallNo(resourceDownloaded, representation, 0,
				resourceTypeDGS);
	}

	private void assertRepresentationAllowedUsingCallNo(ResourceInputStream resourceDownloaded,
			String representation, int callNo, DataGroupSpy resourceTypeDGS) {
		streamStorage.MCR.assertParameters("retrieve", callNo, DATA_DIVIDER, BINARY_RECORD_TYPE,
				RECORD_ID, representation);
		streamStorage.MCR.assertReturn("retrieve", callNo, resourceDownloaded.stream);

		assertReturnedDataFromCorrectResourceType(representation, resourceDownloaded,
				resourceTypeDGS);
	}

	@Test
	public void testDownloadAMedium() {
		var resourceDownloaded = downloader.download(AUTH_TOKEN, BINARY_RECORD_TYPE, RECORD_ID,
				MEDIUM);

		assertRepresentationAllowed(resourceDownloaded, MEDIUM, mediumResourceTypeDGS);
	}

	@Test
	public void testDownloadALarge() {
		var resourceDownloaded = downloader.download(AUTH_TOKEN, BINARY_RECORD_TYPE, RECORD_ID,
				LARGE);

		assertRepresentationAllowed(resourceDownloaded, LARGE, largeResourceTypeDGS);
	}

	@Test
	public void testDownloadJp2() {
		var resourceDownloaded = downloader.download(AUTH_TOKEN, BINARY_RECORD_TYPE, RECORD_ID,
				JP2);

		assertRepresentationAllowed(resourceDownloaded, JP2, jp2ResourceTypeDGS);
	}

	@Test
	public void testDownloadStreamOnFedoraStorageHasException() {
		var resourceNotFoundException = se.uu.ub.cora.storage.ResourceNotFoundException
				.withMessage(ERR_MESSAGE_MISUSE);
		streamStorage.MRV.setAlwaysThrowException("retrieve", resourceNotFoundException);

		try {
			downloader.download(AUTH_TOKEN, BINARY_RECORD_TYPE, RECORD_ID, JP2);
			fail("It should throw an exception");
		} catch (Exception e) {
			assertTrue(e instanceof ResourceNotFoundException);
			String errorNotFoundMessage = "Could not download the stream because it could not be found in storage. Type: {0}, id: {1} and representation: {2}";
			assertEquals(e.getMessage(),
					MessageFormat.format(errorNotFoundMessage, BINARY_RECORD_TYPE, RECORD_ID, JP2));
			assertEquals(e.getCause(), resourceNotFoundException);
		}
	}

}
