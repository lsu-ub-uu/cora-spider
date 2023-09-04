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

import static org.testng.Assert.assertTrue;

import java.util.List;

import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import se.uu.ub.cora.bookkeeper.linkcollector.DataRecordLinkCollector;
import se.uu.ub.cora.bookkeeper.validator.DataValidator;
import se.uu.ub.cora.data.DataProvider;
import se.uu.ub.cora.data.DataRecord;
import se.uu.ub.cora.data.copier.DataCopierFactory;
import se.uu.ub.cora.data.copier.DataCopierProvider;
import se.uu.ub.cora.data.spies.DataFactorySpy;
import se.uu.ub.cora.data.spies.DataGroupSpy;
import se.uu.ub.cora.data.spies.DataRecordGroupSpy;
import se.uu.ub.cora.logger.LoggerProvider;
import se.uu.ub.cora.search.RecordIndexer;
import se.uu.ub.cora.spider.authentication.OldAuthenticatorSpy;
import se.uu.ub.cora.spider.authorization.PermissionRuleCalculator;
import se.uu.ub.cora.spider.dependency.SpiderInstanceProvider;
import se.uu.ub.cora.spider.dependency.spy.InputStreamSpy;
import se.uu.ub.cora.spider.dependency.spy.ResourceArchiveSpy;
import se.uu.ub.cora.spider.extendedfunctionality.internal.ExtendedFunctionalityProviderSpy;
import se.uu.ub.cora.spider.log.LoggerFactorySpy;
import se.uu.ub.cora.spider.record.DataCopierFactorySpy;
import se.uu.ub.cora.spider.record.Uploader;
import se.uu.ub.cora.spider.spy.DataGroupTermCollectorSpy;
import se.uu.ub.cora.spider.spy.IdGeneratorSpy;
import se.uu.ub.cora.spider.spy.SpiderAuthorizatorSpy;
import se.uu.ub.cora.spider.spy.SpiderDependencyProviderSpy;
import se.uu.ub.cora.spider.testspies.RecordUpdaterSpy;
import se.uu.ub.cora.spider.testspies.SpiderInstanceFactorySpy;
import se.uu.ub.cora.storage.spies.RecordStorageSpy;

public class UploaderTest {
	private static final String MIME_TYPE_JPEG = "image/jpeg";
	private static final String SOME_RECORD_ID = "someRecordId";
	private static final String SOME_AUTH_TOKEN = "someAuthToken";
	private static final String BINARY_RECORD_TYPE = "binary";
	private static final String SOME_RESOURCE_TYPE = "someResourceType";
	private InputStreamSpy someStream;
	private RecordUpdaterSpy recordUpdater;

	private ResourceArchiveSpy resourceArchive;
	private RecordStorageSpy recordStorage;
	private OldAuthenticatorSpy oldAuthenticator;
	// private StreamStorageSpy streamStorage;
	private SpiderAuthorizatorSpy authorizator;
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
		// termCollector = new DataGroupTermCollectorSpy();
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
		recordStorage = new RecordStorageSpy();
		dependencyProvider.MRV.setDefaultReturnValuesSupplier("getRecordStorage",
				() -> recordStorage);
		resourceArchive = new ResourceArchiveSpy();
		dependencyProvider.MRV.setDefaultReturnValuesSupplier("getResourceArchive",
				() -> resourceArchive);

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
	public void testUploadStoreInputStreamInArchive() throws Exception {
		uploader.upload(SOME_AUTH_TOKEN, BINARY_RECORD_TYPE, SOME_RECORD_ID, someStream,
				SOME_RESOURCE_TYPE);

		dependencyProvider.MCR.assertParameters("getRecordStorage", 0);
		dependencyProvider.MCR.assertReturn("getRecordStorage", 0, recordStorage);
		recordStorage.MCR.assertParameterAsEqual("read", 0, "types", List.of(BINARY_RECORD_TYPE));
		recordStorage.MCR.assertParameter("read", 0, "id", SOME_RECORD_ID);

		DataGroupSpy readDataGroup = (DataGroupSpy) recordStorage.MCR.getReturnValue("read", 0);
		dataFactorySpy.MCR.assertParameters("factorRecordGroupFromDataGroup", 0, readDataGroup);
		DataRecordGroupSpy readDataRecordGroup = (DataRecordGroupSpy) dataFactorySpy.MCR
				.getReturnValue("factorRecordGroupFromDataGroup", 0);

		readDataRecordGroup.MCR.assertParameters("getDataDivider", 0);
		var dataDivider = readDataRecordGroup.MCR.getReturnValue("getDataDivider", 0);

		dependencyProvider.MCR.assertParameters("getResourceArchive", 0);
		dependencyProvider.MCR.assertReturn("getResourceArchive", 0, resourceArchive);
		resourceArchive.MCR.assertParameters("create", 0, dataDivider, BINARY_RECORD_TYPE,
				SOME_RECORD_ID, someStream, MIME_TYPE_JPEG);

	}

	@Test
	public void testUploadUpdatesBinaryRecord() throws Exception {
		DataRecord binaryRecord = uploader.upload(SOME_AUTH_TOKEN, BINARY_RECORD_TYPE,
				SOME_RECORD_ID, someStream, SOME_RESOURCE_TYPE);

		spiderInstanceFactory.MCR.assertParameters("factorRecordUpdater", 0);
		spiderInstanceFactory.MCR.assertReturn("factorRecordUpdater", 0, recordUpdater);
		recordUpdater.MCR.assertParameters("updateRecord", 0, SOME_AUTH_TOKEN, BINARY_RECORD_TYPE,
				SOME_RECORD_ID);
		recordUpdater.MCR.assertReturn("updateRecord", 0, binaryRecord);

		assertTrue(binaryRecord instanceof DataRecord);
	}

	@Test
	public void testExternalDependenciesAreCalled() {
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
		DataRecord recordUpdated = uploader.upload(SOME_AUTH_TOKEN, BINARY_RECORD_TYPE,
				SOME_RECORD_ID, someStream, SOME_RESOURCE_TYPE);

		// assertResourceInfoIsCorrect(recordUpdated);
		//
		// assertTrue(((OldRecordStorageSpy) recordStorage).readWasCalled);
		//
		// authorizator.MCR.assertMethodWasCalled("checkUserIsAuthorizedForActionOnRecordType");
		//
	}
	//
	// @Test(expectedExceptions = AuthenticationException.class)
	// public void testAuthenticationNotAuthenticated() {
	// authenticator.throwAuthenticationException = true;
	// recordStorage = new OldRecordStorageSpy();
	// setUpDependencyProvider();
	// uploader.upload("dummyNonAuthenticatedToken", "place", "place:0002", null, resourceType);
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
	// private void assertResourceInfoIsCorrect(DataRecord recordUpdated) {
	// dataFactorySpy.MCR.assertParameters("factorGroupUsingNameInData", 0, "resourceInfo");
	// DataGroupSpy resourceInfo = (DataGroupSpy) dataFactorySpy.MCR
	// .getReturnValue("factorGroupUsingNameInData", 0);
	//
	// dataFactorySpy.MCR.assertParameters("factorResourceLinkUsingNameInData", 0, "master");
	// DataResourceLinkSpy masterLink = (DataResourceLinkSpy) dataFactorySpy.MCR
	// .getReturnValue("factorResourceLinkUsingNameInData", 0);
	// resourceInfo.MCR.assertParameters("addChild", 0, masterLink);
	//
	// masterLink.MCR.assertParameters("setStreamId", 0,
	// idGenerator.MCR.getReturnValue("getIdForType", 0));
	// masterLink.MCR.assertParameters("setFileName", 0, "someFileName");
	// masterLink.MCR.assertParameters("setFileSize", 0, "8");
	// masterLink.MCR.assertParameters("setMimeType", 0, "application/octet-stream");
	// }
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
