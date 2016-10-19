/*
 * Copyright 2016 Olov McKie
 * Copyright 2016 Uppsala University Library
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

package se.uu.ub.cora.spider.record;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertNotNull;
import static org.testng.Assert.assertTrue;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.HashSet;
import java.util.Set;

import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import se.uu.ub.cora.beefeater.Authorizator;
import se.uu.ub.cora.beefeater.AuthorizatorImp;
import se.uu.ub.cora.spider.authentication.AuthenticationException;
import se.uu.ub.cora.spider.authentication.Authenticator;
import se.uu.ub.cora.spider.authentication.AuthenticatorSpy;
import se.uu.ub.cora.spider.data.DataMissingException;
import se.uu.ub.cora.spider.data.SpiderInputStream;
import se.uu.ub.cora.spider.dependency.SpiderDependencyProviderSpy;
import se.uu.ub.cora.spider.dependency.SpiderInstanceFactory;
import se.uu.ub.cora.spider.dependency.SpiderInstanceFactoryImp;
import se.uu.ub.cora.spider.dependency.SpiderInstanceProvider;
import se.uu.ub.cora.spider.record.storage.RecordNotFoundException;
import se.uu.ub.cora.spider.record.storage.RecordStorage;
import se.uu.ub.cora.spider.spy.AuthorizatorAlwaysAuthorizedSpy;
import se.uu.ub.cora.spider.spy.RecordPermissionKeyCalculatorStub;
import se.uu.ub.cora.spider.spy.RecordStorageSpy;
import se.uu.ub.cora.spider.testdata.TestDataRecordInMemoryStorage;

public class SpiderDownloaderTest {
	private RecordStorage recordStorage;
	private Authenticator authenticator;
	private StreamStorageSpy streamStorage;
	private Authorizator authorizator;
	private RecordPermissionKeyCalculatorStub keyCalculator;
	private SpiderDownloader downloader;
	private SpiderDependencyProviderSpy dependencyProvider;

	@BeforeMethod
	public void beforeMethod() {
		authenticator = new AuthenticatorSpy();
		authorizator = new AuthorizatorImp();
		keyCalculator = new RecordPermissionKeyCalculatorStub();
		recordStorage = TestDataRecordInMemoryStorage.createRecordStorageInMemoryWithTestData();
		streamStorage = new StreamStorageSpy();
		setUpDependencyProvider();
	}

	private void setUpDependencyProvider() {
		dependencyProvider = new SpiderDependencyProviderSpy();
		dependencyProvider.authenticator = authenticator;
		dependencyProvider.authorizator = authorizator;
		dependencyProvider.keyCalculator = keyCalculator;
		dependencyProvider.recordStorage = recordStorage;
		dependencyProvider.streamStorage = streamStorage;
		SpiderInstanceFactory factory = SpiderInstanceFactoryImp
				.usingDependencyProvider(dependencyProvider);
		SpiderInstanceProvider.setSpiderInstanceFactory(factory);
		downloader = SpiderDownloaderImp.usingDependencyProvider(dependencyProvider);
	}

	@Test
	public void testInit() {
		assertNotNull(downloader);
	}

	// @Test
	public void testExternalDependenciesAreCalled() {
		authorizator = new AuthorizatorAlwaysAuthorizedSpy();
		recordStorage = new RecordStorageSpy();
		setUpDependencyProvider();

		downloader.download("someToken78678567", "image", "image:123456789", "master");

		assertTrue(((RecordStorageSpy) recordStorage).readWasCalled);

		assertTrue(((AuthorizatorAlwaysAuthorizedSpy) authorizator).authorizedWasCalled);
	}

	@Test(expectedExceptions = AuthenticationException.class)
	public void testAuthenticationNotAuthenticated() {
		recordStorage = new RecordStorageSpy();
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

	@Test(expectedExceptions = RecordNotFoundException.class)
	public void testNonExistingRecordType() {
		downloader.download("someToken78678567", "image_NOT_EXISTING", "image:123456789", "master");
	}

	@Test(expectedExceptions = AuthorizationException.class)
	public void testUpdateRecordUserNotAuthorisedToDownload() {
		HashSet<String> notAuthorizedKeys = new HashSet<>();
		notAuthorizedKeys.add("DOWNLOAD:IMAGE:SYSTEM:*");
		SpiderDownloader downloader = setupWithUserNotAuthorized(notAuthorizedKeys);
		downloader.download("someToken78678567", "image", "image:123456789", "master");
	}

	private SpiderDownloader setupWithUserNotAuthorized(Set<String> notAuthorizedKeys) {
		authorizator = new AlwaysAuthorisedExceptStub();
		AlwaysAuthorisedExceptStub authorizator2 = (AlwaysAuthorisedExceptStub) authorizator;
		authorizator2.notAuthorizedForKeys = notAuthorizedKeys;
		setUpDependencyProvider();

		return SpiderDownloaderImp.usingDependencyProvider(dependencyProvider);
	}

	@Test(expectedExceptions = AuthorizationException.class)
	public void testUpdateRecordUserNotAuthorisedToResource() {
		HashSet<String> notAuthorizedKeys = new HashSet<>();
		notAuthorizedKeys.add("MASTER_RESOURCE:IMAGE:SYSTEM:*");
		SpiderDownloader downloader = setupWithUserNotAuthorized(notAuthorizedKeys);
		downloader.download("someToken78678567", "image", "image:123456789", "master");
	}
}
