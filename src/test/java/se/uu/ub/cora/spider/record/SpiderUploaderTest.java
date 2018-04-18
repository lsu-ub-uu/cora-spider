/*
 * Copyright 2015, 2016 Uppsala University Library
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
import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertTrue;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.HashSet;

import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import se.uu.ub.cora.bookkeeper.linkcollector.DataRecordLinkCollector;
import se.uu.ub.cora.bookkeeper.termcollector.DataGroupTermCollector;
import se.uu.ub.cora.bookkeeper.validator.DataValidator;
import se.uu.ub.cora.spider.authentication.AuthenticationException;
import se.uu.ub.cora.spider.authentication.Authenticator;
import se.uu.ub.cora.spider.authentication.AuthenticatorSpy;
import se.uu.ub.cora.spider.authorization.AlwaysAuthorisedExceptStub;
import se.uu.ub.cora.spider.authorization.AuthorizationException;
import se.uu.ub.cora.spider.authorization.NeverAuthorisedStub;
import se.uu.ub.cora.spider.authorization.PermissionRuleCalculator;
import se.uu.ub.cora.spider.authorization.SpiderAuthorizator;
import se.uu.ub.cora.spider.data.DataMissingException;
import se.uu.ub.cora.spider.data.SpiderDataGroup;
import se.uu.ub.cora.spider.data.SpiderDataRecord;
import se.uu.ub.cora.spider.dependency.SpiderDependencyProviderSpy;
import se.uu.ub.cora.spider.dependency.SpiderInstanceFactory;
import se.uu.ub.cora.spider.dependency.SpiderInstanceFactoryImp;
import se.uu.ub.cora.spider.dependency.SpiderInstanceFactorySpy2;
import se.uu.ub.cora.spider.dependency.SpiderInstanceProvider;
import se.uu.ub.cora.spider.extended.ExtendedFunctionalityProviderSpy;
import se.uu.ub.cora.spider.record.storage.RecordIdGenerator;
import se.uu.ub.cora.spider.record.storage.RecordNotFoundException;
import se.uu.ub.cora.spider.record.storage.RecordStorage;
import se.uu.ub.cora.spider.record.storage.TimeStampIdGenerator;
import se.uu.ub.cora.spider.search.RecordIndexer;
import se.uu.ub.cora.spider.spy.AuthorizatorAlwaysAuthorizedSpy;
import se.uu.ub.cora.spider.spy.DataGroupTermCollectorSpy;
import se.uu.ub.cora.spider.spy.DataRecordLinkCollectorSpy;
import se.uu.ub.cora.spider.spy.DataValidatorAlwaysValidSpy;
import se.uu.ub.cora.spider.spy.NoRulesCalculatorStub;
import se.uu.ub.cora.spider.spy.RecordIndexerSpy;
import se.uu.ub.cora.spider.spy.RecordStorageCreateUpdateSpy;
import se.uu.ub.cora.spider.spy.RecordStorageSpy;
import se.uu.ub.cora.spider.spy.RuleCalculatorSpy;
import se.uu.ub.cora.spider.testdata.SpiderDataCreator;
import se.uu.ub.cora.spider.testdata.TestDataRecordInMemoryStorage;

public class SpiderUploaderTest {
	private RecordStorage recordStorage;
	private Authenticator authenticator;
	private StreamStorageSpy streamStorage;
	private SpiderAuthorizator spiderAuthorizator;
	private PermissionRuleCalculator keyCalculator;
	private SpiderUploader uploader;
	private DataValidator dataValidator;
	private DataRecordLinkCollector linkCollector;
	private DataGroupTermCollector searchTermCollector;
	private RecordIndexer recordIndexer;
	private RecordIdGenerator idGenerator;
	private SpiderDependencyProviderSpy dependencyProvider;
	private ExtendedFunctionalityProviderSpy extendedFunctionalityProvider;
	private SpiderInstanceFactory factory;

	@BeforeMethod
	public void beforeMethod() {
		authenticator = new AuthenticatorSpy();
		spiderAuthorizator = new AuthorizatorAlwaysAuthorizedSpy();
		dataValidator = new DataValidatorAlwaysValidSpy();
		recordStorage = TestDataRecordInMemoryStorage.createRecordStorageInMemoryWithTestData();
		keyCalculator = new NoRulesCalculatorStub();
		linkCollector = new DataRecordLinkCollectorSpy();
		idGenerator = new TimeStampIdGenerator();
		streamStorage = new StreamStorageSpy();
		searchTermCollector = new DataGroupTermCollectorSpy();
		recordIndexer = new RecordIndexerSpy();
		extendedFunctionalityProvider = new ExtendedFunctionalityProviderSpy();
		factory = SpiderInstanceFactoryImp.usingDependencyProvider(dependencyProvider);

		setUpDependencyProvider();

	}

	private void setUpDependencyProvider() {
		dependencyProvider = new SpiderDependencyProviderSpy(new HashMap<>());
		dependencyProvider.authenticator = authenticator;
		dependencyProvider.spiderAuthorizator = spiderAuthorizator;
		dependencyProvider.dataValidator = dataValidator;
		dependencyProvider.recordStorage = recordStorage;
		dependencyProvider.ruleCalculator = keyCalculator;
		dependencyProvider.linkCollector = linkCollector;
		dependencyProvider.idGenerator = idGenerator;
		dependencyProvider.streamStorage = streamStorage;
		dependencyProvider.searchTermCollector = searchTermCollector;
		dependencyProvider.recordIndexer = recordIndexer;
		dependencyProvider.extendedFunctionalityProvider = extendedFunctionalityProvider;
		SpiderInstanceProvider.setSpiderInstanceFactory(factory);
		uploader = SpiderUploaderImp.usingDependencyProvider(dependencyProvider);
	}

	@Test
	public void testExternalDependenciesAreCalled() {
		factory = new SpiderInstanceFactorySpy2();
		setUpDependencyProvider();
		recordStorage = new RecordStorageSpy();
		keyCalculator = new RuleCalculatorSpy();
		setUpDependencyProvider();

		SpiderDataGroup spiderDataGroup = SpiderDataGroup.withNameInData("nameInData");
		spiderDataGroup.addChild(
				SpiderDataCreator.createRecordInfoWithRecordTypeAndRecordIdAndDataDivider("spyType",
						"spyId", "cora"));
		InputStream stream = new ByteArrayInputStream("a string".getBytes(StandardCharsets.UTF_8));

		SpiderDataRecord recordUpdated = uploader.upload("someToken78678567", "image",
				"image:123456789", stream, "someFileName");
		assertResourceInfoIsCorrect(recordUpdated);

		assertTrue(((RecordStorageSpy) recordStorage).readWasCalled);

		assertTrue(((AuthorizatorAlwaysAuthorizedSpy) spiderAuthorizator).authorizedWasCalled);

		assertEquals(((SpiderInstanceFactorySpy2) factory).createdUpdaters.get(0).authToken,
				"someToken78678567");

	}

	@Test(expectedExceptions = AuthenticationException.class)
	public void testAuthenticationNotAuthenticated() {
		recordStorage = new RecordStorageSpy();
		setUpDependencyProvider();
		uploader.upload("dummyNonAuthenticatedToken", "place", "place:0002", null, "someFileName");
	}

	@Test
	public void testUnauthorizedForDownloadOnRecordTypeShouldShouldNotAccessStorage() {
		recordStorage = new RecordStorageSpy();
		spiderAuthorizator = new AlwaysAuthorisedExceptStub();
		HashSet<String> hashSet = new HashSet<String>();
		hashSet.add("upload");
		((AlwaysAuthorisedExceptStub) spiderAuthorizator).notAuthorizedForRecordTypeAndActions
				.put("image", hashSet);
		setUpDependencyProvider();

		boolean exceptionWasCaught = false;
		try {
			InputStream stream = new ByteArrayInputStream(
					"a string".getBytes(StandardCharsets.UTF_8));

			uploader.upload("someToken78678567", "image", "image:123456789", stream,
					"someFileName");
		} catch (Exception e) {
			assertEquals(e.getClass(), AuthorizationException.class);
			exceptionWasCaught = true;
		}
		assertTrue(exceptionWasCaught);
		assertFalse(((RecordStorageSpy) recordStorage).readWasCalled);
		assertFalse(((RecordStorageSpy) recordStorage).updateWasCalled);
		assertFalse(((RecordStorageSpy) recordStorage).deleteWasCalled);
		assertFalse(((RecordStorageSpy) recordStorage).createWasCalled);
	}

	@Test(expectedExceptions = AuthorizationException.class)
	public void testUnauthorizedForDataInRecord() {
		spiderAuthorizator = new AuthorizatorNotAuthorizedRequiredRulesButForActionOnRecordType();
		setUpDependencyProvider();

		InputStream stream = new ByteArrayInputStream("a string".getBytes(StandardCharsets.UTF_8));

		uploader.upload("someToken78678567", "image", "image:123456789", stream, "someFileName");
	}

	@Test
	public void testUploadStream() {
		InputStream stream = new ByteArrayInputStream("a string".getBytes(StandardCharsets.UTF_8));

		SpiderDataRecord recordUpdated = uploader.upload("someToken78678567", "image",
				"image:123456789", stream, "someFileName");

		assertEquals(streamStorage.stream, stream);

		assertStreamStorageCalledCorrectly(recordUpdated);
		assertResourceInfoIsCorrect(recordUpdated);
	}

	private void assertStreamStorageCalledCorrectly(SpiderDataRecord recordUpdated) {
		SpiderDataGroup groupUpdated = recordUpdated.getSpiderDataGroup();
		SpiderDataGroup recordInfo = groupUpdated.extractGroup("recordInfo");
		SpiderDataGroup dataDivider = recordInfo.extractGroup("dataDivider");

		SpiderDataGroup resourceInfo = groupUpdated.extractGroup("resourceInfo");
		SpiderDataGroup master = resourceInfo.extractGroup("master");

		String dataDividerRecordId = dataDivider.extractAtomicValue("linkedRecordId");
		assertEquals(dataDividerRecordId, streamStorage.dataDivider);

		String streamId = master.extractAtomicValue("streamId");
		assertEquals(streamId, streamStorage.streamId);

		String size = master.extractAtomicValue("filesize");
		assertEquals(size, String.valueOf(streamStorage.size));
	}

	private void assertResourceInfoIsCorrect(SpiderDataRecord recordUpdated) {
		SpiderDataGroup groupUpdated = recordUpdated.getSpiderDataGroup();

		SpiderDataGroup resourceInfo = groupUpdated.extractGroup("resourceInfo");
		SpiderDataGroup master = resourceInfo.extractGroup("master");

		String fileName = master.extractAtomicValue("filename");
		assertEquals(fileName, "someFileName");

		String mimeType = master.extractAtomicValue("mimeType");
		assertEquals(mimeType, "application/octet-stream");
	}

	@Test(expectedExceptions = MisuseException.class)
	public void testUploadStreamNotChildOfBinary() {
		InputStream stream = new ByteArrayInputStream("a string".getBytes(StandardCharsets.UTF_8));

		uploader.upload("someToken78678567", "place", "place:0002", stream, "someFileName");
	}

	@Test(expectedExceptions = MisuseException.class)
	public void testUploadStreamNotChildOfBinary2() {
		InputStream stream = new ByteArrayInputStream("a string".getBytes(StandardCharsets.UTF_8));

		uploader.upload("someToken78678567", "recordTypeAutoGeneratedId", "someId", stream,
				"someFileName");
	}

	@Test(expectedExceptions = RecordNotFoundException.class)
	public void testUploadNotFound() {
		InputStream stream = new ByteArrayInputStream("a string".getBytes(StandardCharsets.UTF_8));
		uploader.upload("someToken78678567", "image", "NOT_FOUND", stream, "someFileName");
	}

	@Test(expectedExceptions = DataMissingException.class)
	public void testUploadStreamIsMissing() {
		RecordStorageCreateUpdateSpy recordStorageSpy = new RecordStorageCreateUpdateSpy();
		recordStorage = recordStorageSpy;
		setUpDependencyProvider();
		uploader.upload("someToken78678567", "image", "image:123456789", null, "someFileName");

	}

	@Test(expectedExceptions = DataMissingException.class)
	public void testUploadFileNameIsMissing() {
		RecordStorageCreateUpdateSpy recordStorageSpy = new RecordStorageCreateUpdateSpy();
		recordStorage = recordStorageSpy;
		setUpDependencyProvider();
		InputStream stream = new ByteArrayInputStream("a string".getBytes(StandardCharsets.UTF_8));
		uploader.upload("someToken78678567", "image", "image:123456789", stream, null);

	}

	@Test(expectedExceptions = DataMissingException.class)
	public void testUploadFileNameIsEmpty() {
		RecordStorageCreateUpdateSpy recordStorageSpy = new RecordStorageCreateUpdateSpy();
		recordStorage = recordStorageSpy;
		setUpDependencyProvider();
		InputStream stream = new ByteArrayInputStream("a string".getBytes(StandardCharsets.UTF_8));
		uploader.upload("someToken78678567", "image", "image:123456789", stream, "");

	}

	@Test(expectedExceptions = RecordNotFoundException.class)
	public void testNonExistingRecordType() {
		InputStream stream = new ByteArrayInputStream("a string".getBytes(StandardCharsets.UTF_8));
		uploader.upload("someToken78678567", "recordType_NOT_EXISTING", "id", stream,
				"someFileName");
	}

	@Test(expectedExceptions = AuthorizationException.class)
	public void testUpdateRecordUserNotAuthorisedToUpdateData() {

		SpiderUploader uploader = setupWithUserNotAuthorized();
		InputStream stream = new ByteArrayInputStream("a string".getBytes(StandardCharsets.UTF_8));
		uploader.upload("someToken78678567", "image", "image:123456789", stream, "someFileName");
	}

	private SpiderUploader setupWithUserNotAuthorized() {
		spiderAuthorizator = new NeverAuthorisedStub();
		setUpDependencyProvider();

		SpiderUploader uploader = SpiderUploaderImp.usingDependencyProvider(dependencyProvider);
		return uploader;
	}

}
