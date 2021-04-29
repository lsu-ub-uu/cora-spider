/*
 * Copyright 2015, 2016, 2019 Uppsala University Library
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

import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import se.uu.ub.cora.bookkeeper.linkcollector.DataRecordLinkCollector;
import se.uu.ub.cora.bookkeeper.validator.DataValidator;
import se.uu.ub.cora.data.DataAtomicFactory;
import se.uu.ub.cora.data.DataAtomicProvider;
import se.uu.ub.cora.data.DataGroup;
import se.uu.ub.cora.data.DataGroupFactory;
import se.uu.ub.cora.data.DataGroupProvider;
import se.uu.ub.cora.data.DataRecord;
import se.uu.ub.cora.data.DataRecordFactory;
import se.uu.ub.cora.data.DataRecordProvider;
import se.uu.ub.cora.data.DataResourceLink;
import se.uu.ub.cora.data.DataResourceLinkFactory;
import se.uu.ub.cora.data.DataResourceLinkProvider;
import se.uu.ub.cora.data.copier.DataCopierFactory;
import se.uu.ub.cora.data.copier.DataCopierProvider;
import se.uu.ub.cora.logger.LoggerProvider;
import se.uu.ub.cora.search.RecordIndexer;
import se.uu.ub.cora.spider.authentication.AuthenticationException;
import se.uu.ub.cora.spider.authentication.AuthenticatorSpy;
import se.uu.ub.cora.spider.authorization.AuthorizationException;
import se.uu.ub.cora.spider.authorization.PermissionRuleCalculator;
import se.uu.ub.cora.spider.data.DataAtomicFactorySpy;
import se.uu.ub.cora.spider.data.DataGroupFactorySpy;
import se.uu.ub.cora.spider.data.DataGroupSpy;
import se.uu.ub.cora.spider.data.DataMissingException;
import se.uu.ub.cora.spider.data.DataRecordFactorySpy;
import se.uu.ub.cora.spider.dependency.RecordIdGeneratorProviderSpy;
import se.uu.ub.cora.spider.dependency.RecordStorageProviderSpy;
import se.uu.ub.cora.spider.dependency.SpiderDependencyProviderSpy;
import se.uu.ub.cora.spider.dependency.SpiderInstanceFactory;
import se.uu.ub.cora.spider.dependency.SpiderInstanceFactorySpy2;
import se.uu.ub.cora.spider.dependency.SpiderInstanceProvider;
import se.uu.ub.cora.spider.dependency.StreamStorageProviderSpy;
import se.uu.ub.cora.spider.extendedfunctionality.internal.ExtendedFunctionalityProviderSpy;
import se.uu.ub.cora.spider.log.LoggerFactorySpy;
import se.uu.ub.cora.spider.spy.DataGroupTermCollectorSpy;
import se.uu.ub.cora.spider.spy.DataRecordLinkCollectorSpy;
import se.uu.ub.cora.spider.spy.DataValidatorSpy;
import se.uu.ub.cora.spider.spy.IdGeneratorSpy;
import se.uu.ub.cora.spider.spy.OldRecordStorageSpy;
import se.uu.ub.cora.spider.spy.RecordIndexerSpy;
import se.uu.ub.cora.spider.spy.RecordStorageCreateUpdateSpy;
import se.uu.ub.cora.spider.spy.RuleCalculatorSpy;
import se.uu.ub.cora.spider.spy.SpiderAuthorizatorSpy;
import se.uu.ub.cora.spider.testdata.DataCreator2;
import se.uu.ub.cora.spider.testdata.TestDataRecordInMemoryStorage;
import se.uu.ub.cora.storage.RecordIdGenerator;
import se.uu.ub.cora.storage.RecordNotFoundException;
import se.uu.ub.cora.storage.RecordStorage;

public class SpiderUploaderTest {
	private RecordStorage recordStorage;
	private AuthenticatorSpy authenticator;
	private StreamStorageSpy streamStorage;
	private SpiderAuthorizatorSpy authorizator;
	private PermissionRuleCalculator keyCalculator;
	private SpiderUploader uploader;
	private DataValidator dataValidator;
	private DataRecordLinkCollector linkCollector;
	private DataGroupTermCollectorSpy termCollector;
	private RecordIndexer recordIndexer;
	private RecordIdGenerator idGenerator;
	private SpiderDependencyProviderSpy dependencyProvider;
	private ExtendedFunctionalityProviderSpy extendedFunctionalityProvider;
	private SpiderInstanceFactory spiderInstanceFactory;
	private LoggerFactorySpy loggerFactorySpy;
	private DataGroupFactory dataGroupFactorySpy;
	private DataAtomicFactory dataAtomicFactorySpy;
	private DataRecordFactory dataRecordFactorySpy;
	private DataCopierFactory dataCopierFactory;
	private DataResourceLinkFactory dataResourceLinkFactory;

	@BeforeMethod
	public void beforeMethod() {
		setUpFactoriesAndProviders();
		authenticator = new AuthenticatorSpy();
		authorizator = new SpiderAuthorizatorSpy();
		dataValidator = new DataValidatorSpy();
		recordStorage = TestDataRecordInMemoryStorage.createRecordStorageInMemoryWithTestData();
		keyCalculator = new RuleCalculatorSpy();
		linkCollector = new DataRecordLinkCollectorSpy();
		idGenerator = new IdGeneratorSpy();
		streamStorage = new StreamStorageSpy();
		termCollector = new DataGroupTermCollectorSpy();
		recordIndexer = new RecordIndexerSpy();
		extendedFunctionalityProvider = new ExtendedFunctionalityProviderSpy();
		spiderInstanceFactory = new SpiderInstanceFactorySpy2();
		setUpDependencyProvider();

	}

	private void setUpFactoriesAndProviders() {
		loggerFactorySpy = new LoggerFactorySpy();
		LoggerProvider.setLoggerFactory(loggerFactorySpy);
		dataGroupFactorySpy = new DataGroupFactorySpy();
		DataGroupProvider.setDataGroupFactory(dataGroupFactorySpy);
		dataAtomicFactorySpy = new DataAtomicFactorySpy();
		DataAtomicProvider.setDataAtomicFactory(dataAtomicFactorySpy);
		dataRecordFactorySpy = new DataRecordFactorySpy();
		DataRecordProvider.setDataRecordFactory(dataRecordFactorySpy);
		dataCopierFactory = new DataCopierFactorySpy();
		DataCopierProvider.setDataCopierFactory(dataCopierFactory);
		dataResourceLinkFactory = new DataResourceLinkFactorySpy();
		DataResourceLinkProvider.setDataResourceLinkFactory(dataResourceLinkFactory);
	}

	private void setUpDependencyProvider() {
		dependencyProvider = new SpiderDependencyProviderSpy(new HashMap<>());
		dependencyProvider.authenticator = authenticator;
		dependencyProvider.spiderAuthorizator = authorizator;
		dependencyProvider.dataValidator = dataValidator;

		RecordStorageProviderSpy recordStorageProviderSpy = new RecordStorageProviderSpy();
		recordStorageProviderSpy.recordStorage = recordStorage;
		dependencyProvider.setRecordStorageProvider(recordStorageProviderSpy);
		StreamStorageProviderSpy streamStorageProviderSpy = new StreamStorageProviderSpy();
		streamStorageProviderSpy.streamStorage = streamStorage;
		dependencyProvider.setStreamStorageProvider(streamStorageProviderSpy);
		RecordIdGeneratorProviderSpy recordIdGeneratorProviderSpy = new RecordIdGeneratorProviderSpy();
		recordIdGeneratorProviderSpy.recordIdGenerator = idGenerator;
		dependencyProvider.setRecordIdGeneratorProvider(recordIdGeneratorProviderSpy);

		dependencyProvider.ruleCalculator = keyCalculator;
		dependencyProvider.linkCollector = linkCollector;
		dependencyProvider.termCollector = termCollector;
		dependencyProvider.recordIndexer = recordIndexer;
		dependencyProvider.extendedFunctionalityProvider = extendedFunctionalityProvider;
		SpiderInstanceProvider.setSpiderInstanceFactory(spiderInstanceFactory);
		uploader = SpiderUploaderImp.usingDependencyProvider(dependencyProvider);
	}

	@Test
	public void testExternalDependenciesAreCalled() {
		spiderInstanceFactory = new SpiderInstanceFactorySpy2();
		setUpDependencyProvider();
		recordStorage = new OldRecordStorageSpy();
		keyCalculator = new RuleCalculatorSpy();
		setUpDependencyProvider();

		DataGroup dataGroup = new DataGroupSpy("nameInData");
		dataGroup.addChild(DataCreator2.createRecordInfoWithRecordTypeAndRecordIdAndDataDivider(
				"spyType", "spyId", "cora"));
		InputStream stream = new ByteArrayInputStream("a string".getBytes(StandardCharsets.UTF_8));

		DataRecord recordUpdated = uploader.upload("someToken78678567", "image", "image:123456789",
				stream, "someFileName");
		assertResourceInfoIsCorrect(recordUpdated);

		assertTrue(((OldRecordStorageSpy) recordStorage).readWasCalled);

		authorizator.MCR.assertMethodWasCalled("checkUserIsAuthorizedForActionOnRecordType");

		SpiderInstanceFactorySpy2 spiderInstanceFactorySpy2 = (SpiderInstanceFactorySpy2) spiderInstanceFactory;
		assertEquals(spiderInstanceFactorySpy2.createdUpdaters.get(0).authToken,
				"someToken78678567");
		assertEquals(spiderInstanceFactorySpy2.recordType, "image");

	}

	@Test(expectedExceptions = AuthenticationException.class)
	public void testAuthenticationNotAuthenticated() {
		authenticator.throwAuthenticationException = true;
		recordStorage = new OldRecordStorageSpy();
		setUpDependencyProvider();
		uploader.upload("dummyNonAuthenticatedToken", "place", "place:0002", null, "someFileName");
	}

	@Test
	public void testUnauthorizedForDownloadOnRecordTypeShouldShouldNotAccessStorage() {
		recordStorage = new OldRecordStorageSpy();
		setUpDependencyProvider();
		authorizator.authorizedForActionAndRecordType = false;

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
		assertFalse(((OldRecordStorageSpy) recordStorage).readWasCalled);
		assertFalse(((OldRecordStorageSpy) recordStorage).updateWasCalled);
		assertFalse(((OldRecordStorageSpy) recordStorage).deleteWasCalled);
		assertFalse(((OldRecordStorageSpy) recordStorage).createWasCalled);
	}

	@Test(expectedExceptions = AuthorizationException.class)
	public void testUnauthorizedForDataInRecord() {
		authorizator.authorizedForActionAndRecordTypeAndCollectedData = false;

		InputStream stream = new ByteArrayInputStream("a string".getBytes(StandardCharsets.UTF_8));

		uploader.upload("someToken78678567", "image", "image:123456789", stream, "someFileName");
	}

	@Test
	public void testUploadStream() {
		InputStream stream = new ByteArrayInputStream("a string".getBytes(StandardCharsets.UTF_8));
		DataRecord recordUpdated = uploader.upload("someToken78678567", "image", "image:123456789",
				stream, "someFileName");

		assertEquals(streamStorage.stream, stream);

		assertStreamStorageCalledCorrectly(recordUpdated);
		assertResourceInfoIsCorrect(recordUpdated);

		String methodName = "checkUserIsAuthorizedForActionOnRecordType";
		authorizator.MCR.assertParameters(methodName, 0, authenticator.returnedUser, "upload",
				"image");

		termCollector.MCR.assertParameter("collectTerms", 0, "metadataId", "image");
		termCollector.MCR.assertParameter("collectTerms", 0, "dataGroup",
				recordStorage.read("image", "image:123456789"));

		String methodName2 = "checkUserIsAuthorizedForActionOnRecordTypeAndCollectedData";
		authorizator.MCR.assertParameters(methodName2, 0, authenticator.returnedUser, "upload",
				"image", termCollector.MCR.getReturnValue("collectTerms", 0));
	}

	private void assertStreamStorageCalledCorrectly(DataRecord recordUpdated) {
		DataGroup groupUpdated = recordUpdated.getDataGroup();
		DataGroup recordInfo = groupUpdated.getFirstGroupWithNameInData("recordInfo");
		DataGroup dataDivider = recordInfo.getFirstGroupWithNameInData("dataDivider");

		DataGroup resourceInfo = groupUpdated.getFirstGroupWithNameInData("resourceInfo");
		DataResourceLink master = (DataResourceLink) resourceInfo
				.getFirstGroupWithNameInData("master");

		String dataDividerRecordId = dataDivider
				.getFirstAtomicValueWithNameInData("linkedRecordId");
		assertEquals(dataDividerRecordId, streamStorage.dataDivider);

		String streamId = master.getFirstAtomicValueWithNameInData("streamId");
		assertEquals(streamId, streamStorage.streamId);

		String size = master.getFirstAtomicValueWithNameInData("filesize");
		assertEquals(size, String.valueOf(streamStorage.size));
	}

	private void assertResourceInfoIsCorrect(DataRecord recordUpdated) {
		DataGroup groupUpdated = recordUpdated.getDataGroup();

		DataGroup resourceInfo = groupUpdated.getFirstGroupWithNameInData("resourceInfo");
		DataGroup master = resourceInfo.getFirstGroupWithNameInData("master");

		String fileName = master.getFirstAtomicValueWithNameInData("filename");
		assertEquals(fileName, "someFileName");

		String mimeType = master.getFirstAtomicValueWithNameInData("mimeType");
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
		authorizator.authorizedForActionAndRecordType = false;

		SpiderUploader uploader = SpiderUploaderImp.usingDependencyProvider(dependencyProvider);
		return uploader;
	}

}
