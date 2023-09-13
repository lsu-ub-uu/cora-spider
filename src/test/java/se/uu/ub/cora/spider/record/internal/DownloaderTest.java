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

import static org.testng.Assert.assertNotNull;
import static org.testng.Assert.assertTrue;

import java.util.List;

import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import se.uu.ub.cora.data.DataProvider;
import se.uu.ub.cora.data.spies.DataFactorySpy;
import se.uu.ub.cora.data.spies.DataGroupSpy;
import se.uu.ub.cora.data.spies.DataRecordGroupSpy;
import se.uu.ub.cora.spider.authentication.OldAuthenticatorSpy;
import se.uu.ub.cora.spider.data.ResourceInputStream;
import se.uu.ub.cora.spider.dependency.spy.ResourceArchiveSpy;
import se.uu.ub.cora.spider.log.LoggerFactorySpy;
import se.uu.ub.cora.spider.record.DataCopierFactorySpy;
import se.uu.ub.cora.spider.record.Downloader;
import se.uu.ub.cora.spider.record.StreamStorageSpy;
import se.uu.ub.cora.spider.spy.OldSpiderAuthorizatorSpy;
import se.uu.ub.cora.spider.spy.SpiderDependencyProviderSpy;
import se.uu.ub.cora.storage.spies.RecordStorageSpy;

public class DownloaderTest {
	private static final String RESOURCE_TYPE_MASTER = "master";
	private static final String SOME_RECORD_ID = "someId";
	private static final String SOME_AUTH_TOKEN = "someAuthToken";
	private static final String BINARY_RECORD_TYPE = "binary";
	private RecordStorageSpy recordStorage;
	private OldAuthenticatorSpy authenticator;
	private StreamStorageSpy streamStorage;
	private OldSpiderAuthorizatorSpy authorizator;
	private Downloader downloader;
	private SpiderDependencyProviderSpy dependencyProvider;
	private LoggerFactorySpy loggerFactorySpy;
	private DataCopierFactorySpy dataCopierFactory;

	private DataFactorySpy dataFactorySpy;
	private ResourceArchiveSpy resourceArchive;

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
	}

	// private void setUpFactoriesAndProviders() {
	// loggerFactorySpy = new LoggerFactorySpy();
	// LoggerProvider.setLoggerFactory(loggerFactorySpy);
	//
	// dataFactory = new DataFactorySpy();
	// DataProvider.onlyForTestSetDataFactory(dataFactory);
	//
	// dataCopierFactory = new DataCopierFactorySpy();
	// DataCopierProvider.setDataCopierFactory(dataCopierFactory);
	// }

	private void setUpDependencyProvider() {

		recordStorage = new RecordStorageSpy();
		dependencyProvider.MRV.setDefaultReturnValuesSupplier("getRecordStorage",
				() -> recordStorage);

		resourceArchive = new ResourceArchiveSpy();
		dependencyProvider.MRV.setDefaultReturnValuesSupplier("getResourceArchive",
				() -> resourceArchive);

		// dependencyProvider = new SpiderDependencyProviderOldSpy();
		// dependencyProvider.authenticator = authenticator;
		// dependencyProvider.spiderAuthorizator = authorizator;
		// dependencyProvider.recordStorage = recordStorage;
		// dependencyProvider.streamStorage = streamStorage;
		// SpiderInstanceFactory factory = SpiderInstanceFactoryImp
		// .usingDependencyProvider(dependencyProvider);
		// SpiderInstanceProvider.setSpiderInstanceFactory(factory);
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
