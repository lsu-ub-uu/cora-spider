/*
 * Copyright 2016 Olov McKie
 * Copyright 2016, 2017, 2019 Uppsala University Library
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
import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertNotNull;
import static org.testng.Assert.assertTrue;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;

import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import se.uu.ub.cora.data.DataAtomicProvider;
import se.uu.ub.cora.data.DataGroupFactory;
import se.uu.ub.cora.data.DataGroupProvider;
import se.uu.ub.cora.data.copier.DataCopierProvider;
import se.uu.ub.cora.logger.LoggerProvider;
import se.uu.ub.cora.spider.authentication.AuthenticationException;
import se.uu.ub.cora.spider.authentication.AuthenticatorSpy;
import se.uu.ub.cora.spider.authorization.AuthorizationException;
import se.uu.ub.cora.spider.data.DataAtomicFactorySpy;
import se.uu.ub.cora.spider.data.DataGroupFactorySpy;
import se.uu.ub.cora.spider.data.DataMissingException;
import se.uu.ub.cora.spider.data.SpiderInputStream;
import se.uu.ub.cora.spider.dependency.RecordStorageProviderSpy;
import se.uu.ub.cora.spider.dependency.SpiderDependencyProviderSpy;
import se.uu.ub.cora.spider.dependency.SpiderInstanceFactory;
import se.uu.ub.cora.spider.dependency.SpiderInstanceFactoryImp;
import se.uu.ub.cora.spider.dependency.SpiderInstanceProvider;
import se.uu.ub.cora.spider.dependency.StreamStorageProviderSpy;
import se.uu.ub.cora.spider.log.LoggerFactorySpy;
import se.uu.ub.cora.spider.record.DataCopierFactorySpy;
import se.uu.ub.cora.spider.record.MisuseException;
import se.uu.ub.cora.spider.record.SpiderDownloader;
import se.uu.ub.cora.spider.record.StreamStorageSpy;
import se.uu.ub.cora.spider.spy.OldRecordStorageSpy;
import se.uu.ub.cora.spider.spy.RuleCalculatorSpy;
import se.uu.ub.cora.spider.spy.SpiderAuthorizatorSpy;
import se.uu.ub.cora.spider.testdata.TestDataRecordInMemoryStorage;
import se.uu.ub.cora.storage.RecordNotFoundException;
import se.uu.ub.cora.storage.RecordStorage;

public class SpiderDownloaderTest {
	private RecordStorage recordStorage;
	private AuthenticatorSpy authenticator;
	private StreamStorageSpy streamStorage;
	private SpiderAuthorizatorSpy authorizator;
	private RuleCalculatorSpy ruleCalculator;
	private SpiderDownloader downloader;
	private SpiderDependencyProviderSpy dependencyProvider;
	private LoggerFactorySpy loggerFactorySpy;
	private DataGroupFactory dataGroupFactory;
	private DataAtomicFactorySpy dataAtomicFactory;
	private DataCopierFactorySpy dataCopierFactory;

	@BeforeMethod
	public void beforeMethod() {
		setUpFactoriesAndProviders();

		authenticator = new AuthenticatorSpy();
		authorizator = new SpiderAuthorizatorSpy();
		ruleCalculator = new RuleCalculatorSpy();
		recordStorage = TestDataRecordInMemoryStorage.createRecordStorageInMemoryWithTestData();
		streamStorage = new StreamStorageSpy();
		setUpDependencyProvider();
	}

	private void setUpFactoriesAndProviders() {
		loggerFactorySpy = new LoggerFactorySpy();
		LoggerProvider.setLoggerFactory(loggerFactorySpy);
		dataGroupFactory = new DataGroupFactorySpy();
		DataGroupProvider.setDataGroupFactory(dataGroupFactory);
		dataAtomicFactory = new DataAtomicFactorySpy();
		DataAtomicProvider.setDataAtomicFactory(dataAtomicFactory);
		dataCopierFactory = new DataCopierFactorySpy();
		DataCopierProvider.setDataCopierFactory(dataCopierFactory);
	}

	private void setUpDependencyProvider() {
		dependencyProvider = new SpiderDependencyProviderSpy(new HashMap<>());
		dependencyProvider.authenticator = authenticator;
		dependencyProvider.spiderAuthorizator = authorizator;
		dependencyProvider.ruleCalculator = ruleCalculator;
		RecordStorageProviderSpy recordStorageProviderSpy = new RecordStorageProviderSpy();
		recordStorageProviderSpy.recordStorage = recordStorage;
		dependencyProvider.setRecordStorageProvider(recordStorageProviderSpy);
		StreamStorageProviderSpy streamStorageProviderSpy = new StreamStorageProviderSpy();
		streamStorageProviderSpy.streamStorage = streamStorage;
		dependencyProvider.setStreamStorageProvider(streamStorageProviderSpy);
		SpiderInstanceFactory factory = SpiderInstanceFactoryImp
				.usingDependencyProvider(dependencyProvider);
		SpiderInstanceProvider.setSpiderInstanceFactory(factory);
		downloader = SpiderDownloaderImp.usingDependencyProvider(dependencyProvider);
	}

	@Test
	public void testInit() {
		assertNotNull(downloader);
	}

	@Test
	public void testUnauthorizedForDownloadOnRecordTypeShouldShouldNotAccessStorage() {
		recordStorage = new OldRecordStorageSpy();
		authorizator.authorizedForActionAndRecordType = false;
		setUpDependencyProvider();

		boolean exceptionWasCaught = false;
		try {
			downloader.download("someToken78678567", "image", "image:123456789", "master");
		} catch (Exception e) {
			assertEquals(e.getClass(), AuthorizationException.class);
			exceptionWasCaught = true;
		}
		assertTrue(exceptionWasCaught);
		assertFalse(((OldRecordStorageSpy) recordStorage).readWasCalled);
		assertFalse(((OldRecordStorageSpy) recordStorage).updateWasCalled);
		assertFalse(((OldRecordStorageSpy) recordStorage).deleteWasCalled);
		assertFalse(((OldRecordStorageSpy) recordStorage).createWasCalled);
	}

	@Test
	public void testExternalDependenciesAreCalled() {
		InputStream stream = new ByteArrayInputStream("a string".getBytes(StandardCharsets.UTF_8));
		streamStorage.stream = stream;

		downloader.download("someToken78678567", "image", "image:123456789", "master");

		authorizator.MCR.assertMethodWasCalled("checkUserIsAuthorizedForActionOnRecordType");
	}

	@Test(expectedExceptions = AuthenticationException.class)
	public void testAuthenticationNotAuthenticated() {
		authenticator.throwAuthenticationException = true;
		recordStorage = new OldRecordStorageSpy();
		setUpDependencyProvider();
		downloader.download("dummyNonAuthenticatedToken", "image", "image:123456789", "master");
	}

	@Test
	public void testDownloadStream() {
		InputStream stream = new ByteArrayInputStream("a string".getBytes(StandardCharsets.UTF_8));
		streamStorage.stream = stream;

		SpiderInputStream spiderStream = downloader.download("someToken78678567", "image",
				"image:123456789", "master");

		assertEquals(spiderStream.stream, stream);
		assertEquals(spiderStream.name, "adele.png");
		assertEquals(spiderStream.size, 123);
		assertEquals(spiderStream.mimeType, "application/octet-stream");
	}

	@Test
	public void testDownloadStreamStorageCalledCorrectly() {
		InputStream stream = new ByteArrayInputStream("a string".getBytes(StandardCharsets.UTF_8));
		streamStorage.stream = stream;

		downloader.download("someToken78678567", "image", "image:123456789", "master");

		assertEquals(streamStorage.streamId, "678912345");
		assertEquals(streamStorage.dataDivider, "cora");
	}

	@Test(expectedExceptions = MisuseException.class)
	public void testDownloadStreamNotChildOfBinary() {
		downloader.download("someToken78678567", "place", "place:0002", "master");
	}

	@Test(expectedExceptions = MisuseException.class)
	public void testDownloadStreamNotChildOfBinary2() {

		downloader.download("someToken78678567", "recordTypeAutoGeneratedId", "someId", "master");
	}

	@Test(expectedExceptions = RecordNotFoundException.class)
	public void testDownloadNotFound() {
		downloader.download("someToken78678567", "image", "NOT_FOUND", "master");
	}

	@Test(expectedExceptions = DataMissingException.class)
	public void testDownloadResourceIsMissing() {
		downloader.download("someToken78678567", "image", "image:123456789", null);

	}

	@Test(expectedExceptions = DataMissingException.class)
	public void testDownloadResourceIsEmpty() {
		downloader.download("someToken78678567", "image", "image:123456789", "");
	}

	@Test(expectedExceptions = RecordNotFoundException.class)
	public void testDownloadResourceDoesNotExistInRecord() {
		downloader.download("someToken78678567", "image", "image:123456789", "NonExistingResource");
	}

	@Test
	public void testDownloadResourceDoesNotExistInRecordExceptionInitialIsSentAlong() {
		try {
			downloader.download("someToken78678567", "image", "image:123456789",
					"NonExistingResource");
		} catch (Exception e) {
			assertTrue(e.getCause() instanceof DataMissingException);
		}
	}

	@Test(expectedExceptions = RecordNotFoundException.class)
	public void testNonExistingRecordType() {
		downloader.download("someToken78678567", "image_NOT_EXISTING", "image:123456789", "master");
	}
}
