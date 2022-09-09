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

package se.uu.ub.cora.spider.record.internal;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertTrue;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.List;

import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import se.uu.ub.cora.bookkeeper.linkcollector.DataRecordLinkCollector;
import se.uu.ub.cora.bookkeeper.validator.DataValidator;
import se.uu.ub.cora.data.DataGroup;
import se.uu.ub.cora.data.DataProvider;
import se.uu.ub.cora.data.DataRecord;
import se.uu.ub.cora.data.DataRecordLink;
import se.uu.ub.cora.data.collectterms.CollectTerms;
import se.uu.ub.cora.data.copier.DataCopierFactory;
import se.uu.ub.cora.data.copier.DataCopierProvider;
import se.uu.ub.cora.logger.LoggerProvider;
import se.uu.ub.cora.search.RecordIndexer;
import se.uu.ub.cora.spider.authentication.AuthenticationException;
import se.uu.ub.cora.spider.authentication.AuthenticatorSpy;
import se.uu.ub.cora.spider.authorization.AuthorizationException;
import se.uu.ub.cora.spider.authorization.PermissionRuleCalculator;
import se.uu.ub.cora.spider.data.DataGroupOldSpy;
import se.uu.ub.cora.spider.data.DataMissingException;
import se.uu.ub.cora.spider.dependency.SpiderInstanceFactory;
import se.uu.ub.cora.spider.dependency.SpiderInstanceProvider;
import se.uu.ub.cora.spider.dependency.spy.SpiderDependencyProviderOldSpy;
import se.uu.ub.cora.spider.dependency.spy.SpiderInstanceFactorySpy2;
import se.uu.ub.cora.spider.extendedfunctionality.internal.ExtendedFunctionalityProviderSpy;
import se.uu.ub.cora.spider.log.LoggerFactorySpy;
import se.uu.ub.cora.spider.record.DataCopierFactorySpy;
import se.uu.ub.cora.spider.record.MisuseException;
import se.uu.ub.cora.spider.record.StreamStorageSpy;
import se.uu.ub.cora.spider.record.Uploader;
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
import se.uu.ub.cora.storage.RecordNotFoundException;
import se.uu.ub.cora.storage.RecordStorage;
import se.uu.ub.cora.testspies.data.DataFactorySpy;
import se.uu.ub.cora.testspies.data.DataResourceLinkSpy;

public class SpiderUploaderTest {
	private RecordStorage recordStorage;
	private AuthenticatorSpy authenticator;
	private StreamStorageSpy streamStorage;
	private SpiderAuthorizatorSpy authorizator;
	private PermissionRuleCalculator keyCalculator;
	private Uploader uploader;
	private DataValidator dataValidator;
	private DataRecordLinkCollector linkCollector;
	private DataGroupTermCollectorSpy termCollector;
	private RecordIndexer recordIndexer;
	private IdGeneratorSpy idGenerator;
	private SpiderDependencyProviderOldSpy dependencyProvider;
	private ExtendedFunctionalityProviderSpy extendedFunctionalityProvider;
	private SpiderInstanceFactory spiderInstanceFactory;
	private LoggerFactorySpy loggerFactorySpy;
	private DataFactorySpy dataFactorySpy;

	private DataCopierFactory dataCopierFactory;

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
		dataFactorySpy = new DataFactorySpy();
		DataProvider.onlyForTestSetDataFactory(dataFactorySpy);

		dataCopierFactory = new DataCopierFactorySpy();
		DataCopierProvider.setDataCopierFactory(dataCopierFactory);
	}

	private void setUpDependencyProvider() {
		dependencyProvider = new SpiderDependencyProviderOldSpy(new HashMap<>());
		dependencyProvider.authenticator = authenticator;
		dependencyProvider.spiderAuthorizator = authorizator;
		dependencyProvider.dataValidator = dataValidator;

		dependencyProvider.recordStorage = recordStorage;
		dependencyProvider.streamStorage = streamStorage;
		dependencyProvider.recordIdGenerator = idGenerator;

		dependencyProvider.ruleCalculator = keyCalculator;
		dependencyProvider.linkCollector = linkCollector;
		dependencyProvider.termCollector = termCollector;
		dependencyProvider.recordIndexer = recordIndexer;
		dependencyProvider.extendedFunctionalityProvider = extendedFunctionalityProvider;
		SpiderInstanceProvider.setSpiderInstanceFactory(spiderInstanceFactory);
		uploader = UploaderImp.usingDependencyProvider(dependencyProvider);
	}

	@Test
	public void testExternalDependenciesAreCalled() {
		dataFactorySpy.MRV.setReturnValues("factorGroupUsingNameInData",
				List.of(new se.uu.ub.cora.testspies.data.DataGroupSpy()), "resourceInfo");
		spiderInstanceFactory = new SpiderInstanceFactorySpy2();
		setUpDependencyProvider();
		recordStorage = new OldRecordStorageSpy();
		keyCalculator = new RuleCalculatorSpy();
		setUpDependencyProvider();

		DataGroup dataGroup = new DataGroupOldSpy("nameInData");
		dataGroup.addChild(DataCreator2.createRecordInfoWithRecordTypeAndRecordIdAndDataDivider(
				"spyType", "spyId", "cora"));
		InputStream stream = new ByteArrayInputStream("a string".getBytes(StandardCharsets.UTF_8));

		DataRecord recordUpdated = uploader.upload("someToken78678567", "image", "image:123456789",
				stream, "someFileName");
		assertResourceInfoIsCorrect(recordUpdated);

		assertTrue(((OldRecordStorageSpy) recordStorage).readWasCalled);

		authorizator.MCR.assertMethodWasCalled("checkUserIsAuthorizedForActionOnRecordType");

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
		dataFactorySpy.MRV.setReturnValues("factorGroupUsingNameInData",
				List.of(new se.uu.ub.cora.testspies.data.DataGroupSpy()), "resourceInfo");
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
		CollectTerms collectedTerms = (CollectTerms) termCollector.MCR
				.getReturnValue("collectTerms", 0);
		authorizator.MCR.assertParameters(methodName2, 0, authenticator.returnedUser, "upload",
				"image", collectedTerms.permissionTerms);
	}

	private void assertStreamStorageCalledCorrectly(DataRecord recordUpdated) {
		DataGroup groupUpdated = recordUpdated.getDataGroup();
		DataGroup recordInfo = groupUpdated.getFirstGroupWithNameInData("recordInfo");
		DataRecordLink dataDivider = (DataRecordLink) recordInfo
				.getFirstChildWithNameInData("dataDivider");

		String dataDividerRecordId = dataDivider.getLinkedRecordId();

		assertEquals(dataDividerRecordId, streamStorage.dataDivider);
	}

	private void assertResourceInfoIsCorrect(DataRecord recordUpdated) {
		DataGroup groupUpdated = recordUpdated.getDataGroup();
		// DataGroup resourceInfo = groupUpdated.getFirstGroupWithNameInData("resourceInfo");
		dataFactorySpy.MCR.assertParameters("factorGroupUsingNameInData", 0, "resourceInfo");
		se.uu.ub.cora.testspies.data.DataGroupSpy resourceInfo = (se.uu.ub.cora.testspies.data.DataGroupSpy) dataFactorySpy.MCR
				.getReturnValue("factorGroupUsingNameInData", 0);

		dataFactorySpy.MCR.assertParameters("factorResourceLinkUsingNameInData", 0, "master");
		DataResourceLinkSpy masterLink = (DataResourceLinkSpy) dataFactorySpy.MCR
				.getReturnValue("factorResourceLinkUsingNameInData", 0);
		resourceInfo.MCR.assertParameters("addChild", 0, masterLink);

		masterLink.MCR.assertParameters("setStreamId", 0,
				idGenerator.MCR.getReturnValue("getIdForType", 0));
		masterLink.MCR.assertParameters("setFileName", 0, "someFileName");
		masterLink.MCR.assertParameters("setFileSize", 0, "8");
		masterLink.MCR.assertParameters("setMimeType", 0, "application/octet-stream");
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

		Uploader uploader = setupWithUserNotAuthorized();
		InputStream stream = new ByteArrayInputStream("a string".getBytes(StandardCharsets.UTF_8));
		uploader.upload("someToken78678567", "image", "image:123456789", stream, "someFileName");
	}

	private Uploader setupWithUserNotAuthorized() {
		authorizator.authorizedForActionAndRecordType = false;

		Uploader uploader = UploaderImp.usingDependencyProvider(dependencyProvider);
		return uploader;
	}

}
