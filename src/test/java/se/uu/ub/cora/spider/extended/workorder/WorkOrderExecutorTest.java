/*
 * Copyright 2017, 2019, 2022, 2024, 2026 Uppsala University Library
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
package se.uu.ub.cora.spider.extended.workorder;

import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import se.uu.ub.cora.data.DataAtomic;
import se.uu.ub.cora.data.DataProvider;
import se.uu.ub.cora.data.DataRecordGroup;
import se.uu.ub.cora.data.DataRecordLink;
import se.uu.ub.cora.data.collected.CollectTerms;
import se.uu.ub.cora.data.spies.DataFactorySpy;
import se.uu.ub.cora.data.spies.DataGroupSpy;
import se.uu.ub.cora.data.spies.DataRecordGroupSpy;
import se.uu.ub.cora.data.spies.DataRecordLinkSpy;
import se.uu.ub.cora.logger.LoggerProvider;
import se.uu.ub.cora.logger.spies.LoggerFactorySpy;
import se.uu.ub.cora.logger.spies.LoggerSpy;
import se.uu.ub.cora.spider.dependency.SpiderInstanceProvider;
import se.uu.ub.cora.spider.extendedfunctionality.ExtendedFunctionalityData;
import se.uu.ub.cora.spider.record.internal.AuthenticatorSpy;
import se.uu.ub.cora.spider.record.internal.SpiderAuthorizatorSpy;
import se.uu.ub.cora.spider.resourceconvert.spy.ResourceConvertSpy;
import se.uu.ub.cora.spider.spy.DataGroupTermCollectorSpy;
import se.uu.ub.cora.spider.spy.RecordIndexerSpy;
import se.uu.ub.cora.spider.spy.SpiderDependencyProviderSpy;
import se.uu.ub.cora.spider.testspies.RecordUpdaterSpy;
import se.uu.ub.cora.spider.testspies.SpiderInstanceFactorySpy;
import se.uu.ub.cora.storage.spies.RecordStorageSpy;

public class WorkOrderExecutorTest {
	SpiderDependencyProviderSpy dependencyProviderSpy;
	WorkOrderExecutor extendedFunctionality;
	DataGroupTermCollectorSpy termCollector;
	RecordIndexerSpy recordIndexer;
	SpiderAuthorizatorSpy authorizator;
	AuthenticatorSpy authenticator;
	private DataFactorySpy dataFactorySpy;
	private RecordStorageSpy recordStorage;
	private DataRecordGroupSpy workOrder;
	private RecordUpdaterSpy recordUpdater;
	private SpiderInstanceFactorySpy spiderInstanceFactory;
	private ResourceConvertSpy resourceConvert;
	private LoggerFactorySpy loggerFactorySpy;
	private LoggerSpy logger;

	@BeforeMethod
	public void setUp() {
		setUpFactoriesAndProviders();
		setUpDependencyProvider();
		setUpSpiderInstanceProvider();

		createWorkOrderWithTypeAndId("book", "book1");
		extendedFunctionality = WorkOrderExecutor.usingDependencyProvider(dependencyProviderSpy);
	}

	private void createWorkOrderWithTypeAndId(String recordTypeToIndex, String recordIdToIndex) {
		DataRecordLinkSpy recordTypeLink = new DataRecordLinkSpy();
		recordTypeLink.MRV.setDefaultReturnValuesSupplier("getLinkedRecordId",
				() -> recordTypeToIndex);

		workOrder = new DataRecordGroupSpy();
		workOrder.MRV.setSpecificReturnValuesSupplier("getFirstChildOfTypeAndName",
				() -> recordTypeLink, DataRecordLink.class, "recordType");
		workOrder.MRV.setSpecificReturnValuesSupplier("getFirstAtomicValueWithNameInData",
				() -> recordIdToIndex, "recordId");
	}

	private void setUpIndexActionForWorkOrder(String indexAction) {
		workOrder.MRV.setSpecificReturnValuesSupplier("getFirstAtomicValueWithNameInData",
				() -> indexAction, "type");
	}

	private void setUpFactoriesAndProviders() {
		loggerFactorySpy = new LoggerFactorySpy();
		LoggerProvider.setLoggerFactory(loggerFactorySpy);
		logger = new LoggerSpy();
		loggerFactorySpy.MRV.setDefaultReturnValuesSupplier("factorForClass", () -> logger);

		dataFactorySpy = new DataFactorySpy();
		DataProvider.onlyForTestSetDataFactory(dataFactorySpy);
	}

	private void setUpDependencyProvider() {
		dependencyProviderSpy = new SpiderDependencyProviderSpy();
		recordStorage = new RecordStorageSpy();
		authorizator = new SpiderAuthorizatorSpy();
		authenticator = new AuthenticatorSpy();
		termCollector = new DataGroupTermCollectorSpy();
		recordIndexer = new RecordIndexerSpy();
		recordUpdater = new RecordUpdaterSpy();
		resourceConvert = new ResourceConvertSpy();

		dependencyProviderSpy.MRV.setDefaultReturnValuesSupplier("getRecordStorage",
				() -> recordStorage);
		dependencyProviderSpy.MRV.setDefaultReturnValuesSupplier("getSpiderAuthorizator",
				() -> authorizator);
		dependencyProviderSpy.MRV.setDefaultReturnValuesSupplier("getAuthenticator",
				() -> authenticator);
		dependencyProviderSpy.MRV.setDefaultReturnValuesSupplier("getDataGroupTermCollector",
				() -> termCollector);
		dependencyProviderSpy.MRV.setDefaultReturnValuesSupplier("getRecordIndexer",
				() -> recordIndexer);
		dependencyProviderSpy.MRV.setDefaultReturnValuesSupplier("getResourceConvert",
				() -> resourceConvert);
	}

	private void setUpSpiderInstanceProvider() {
		spiderInstanceFactory = new SpiderInstanceFactorySpy();
		SpiderInstanceProvider.setSpiderInstanceFactory(spiderInstanceFactory);
		spiderInstanceFactory.MRV.setDefaultReturnValuesSupplier("factorRecordUpdater",
				() -> recordUpdater);
	}

	@Test
	public void testIndexData() {
		String type = "book";
		String id = "book1";
		setUpIndexActionForWorkOrder("NotRemoveFromIndex");

		callExtendedFunctionalityWithGroup(workOrder);

		DataRecordGroupSpy dataToIndex = (DataRecordGroupSpy) recordStorage.MCR
				.assertCalledParametersReturn("read", "book", id);
		dependencyProviderSpy.MCR.assertCalledParameters("getRecordTypeHandlerUsingDataRecordGroup",
				dataToIndex);
		CollectTerms collectTerms = (CollectTerms) termCollector.MCR.assertCalledParametersReturn(
				"collectTerms", "fakeDefMetadataIdFromRecordTypeHandlerSpy", dataToIndex);
		recordIndexer.MCR.assertMethodWasCalled("indexData");
		recordIndexer.MCR.assertParameters("indexData", 0, type, id, collectTerms.indexTerms,
				dataToIndex);

		recordStorage.MCR.assertCalledParameters("updateCollectedData", type, id,
				collectTerms.storageTerms, dataToIndex.getDataDivider());
	}

	private void callExtendedFunctionalityWithGroup(DataRecordGroup workOrder) {
		ExtendedFunctionalityData data = new ExtendedFunctionalityData();
		data.authToken = "someToken";
		data.dataRecordGroup = workOrder;
		extendedFunctionality.useExtendedFunctionality(data);
	}

	@Test
	public void testIndexDataWithNoRightToIndexRecordType() {
		setUpIndexActionForWorkOrder("NotRemoveFromIndex");
		authorizator.MRV.setDefaultReturnValuesSupplier("userIsAuthorizedForActionOnRecordType",
				() -> false);

		callExtendedFunctionalityWithGroup(workOrder);

		termCollector.MCR.assertMethodNotCalled("collectTerms");
		termCollector.MCR.assertMethodNotCalled("indexData");
	}

	@Test
	public void testRemoveFromIndex() {
		setUpIndexActionForWorkOrder("removeFromIndex");

		callExtendedFunctionalityWithGroup(workOrder);

		recordIndexer.MCR.assertParameter("deleteFromIndex", 0, "type", "book");
		recordIndexer.MCR.assertParameter("deleteFromIndex", 0, "id", "book1");
	}

	@Test
	public void testRemoveFromIndexNoRightToIndexRecordType() {
		setUpIndexActionForWorkOrder("removeFromIndex");
		authorizator.MRV.setDefaultReturnValuesSupplier("userIsAuthorizedForActionOnRecordType",
				() -> false);

		callExtendedFunctionalityWithGroup(workOrder);

		recordIndexer.MCR.assertMethodNotCalled("deleteFromIndex");
	}

	@Test
	public void testIndexDataPerformCommitFalseInWorkOrder() {
		setUpIndexActionForWorkOrder("NotRemoveFromIndex");
		setUpPerfromCommitForWorkOrder("false");

		callExtendedFunctionalityWithGroup(workOrder);

		DataRecordGroupSpy dataToIndex = (DataRecordGroupSpy) recordStorage.MCR
				.assertCalledParametersReturn("read", "book", "book1");
		CollectTerms collectTerms = (CollectTerms) termCollector.MCR.assertCalledParametersReturn(
				"collectTerms", "fakeDefMetadataIdFromRecordTypeHandlerSpy", dataToIndex);
		recordIndexer.MCR.assertMethodNotCalled("indexData");
		recordIndexer.MCR.assertParameters("indexDataWithoutExplicitCommit", 0, "book", "book1",
				collectTerms.indexTerms, dataToIndex);
	}

	private void setUpPerfromCommitForWorkOrder(String performCommit) {
		workOrder.MRV.setSpecificReturnValuesSupplier("containsChildWithNameInData", () -> true,
				"performCommit");
		workOrder.MRV.setSpecificReturnValuesSupplier("getFirstAtomicValueWithNameInData",
				() -> performCommit, "performCommit");
	}

	@Test
	public void testIndexDataPerformCommitTrueInWorkOrder() {
		setUpIndexActionForWorkOrder("NotRemoveFromIndex");
		setUpPerfromCommitForWorkOrder("true");

		callExtendedFunctionalityWithGroup(workOrder);

		DataRecordGroupSpy dataToIndex = (DataRecordGroupSpy) recordStorage.MCR
				.assertCalledParametersReturn("read", "book", "book1");
		CollectTerms collectTerms = (CollectTerms) termCollector.MCR.assertCalledParametersReturn(
				"collectTerms", "fakeDefMetadataIdFromRecordTypeHandlerSpy", dataToIndex);
		recordIndexer.MCR.assertMethodNotCalled("indexDataWithoutExplicitCommit");
		recordIndexer.MCR.assertParameters("indexData", 0, "book", "book1", collectTerms.indexTerms,
				dataToIndex);
	}

	@Test
	public void testCallImageReconvertIfWorkOrderIndexesABinary() {
		setUpIndexActionForWorkOrder("index");
		createWorkOrderWithTypeAndId("binary", "someId");
		DataRecordGroupSpy binaryRecordGroup = setMimeTypeToBinaryRecordGroupFromStorageAndReturnRecordGroup(
				"image/some");

		callExtendedFunctionalityWithGroup(workOrder);

		assertRemoveCurrentStatusAndAddReconvertStatus(binaryRecordGroup);
		assertBinaryUpdatedInStorage(binaryRecordGroup);
		assertBinaryIsSentToImageReconvert("image/some");
	}

	private void assertBinaryUpdatedInStorage(DataRecordGroupSpy binaryRecordGroup) {
		recordUpdater.MCR.assertParameters("internalUpdateRecord", 0, binaryRecordGroup, "userSpy");
	}

	private void assertRemoveCurrentStatusAndAddReconvertStatus(
			DataRecordGroupSpy binaryRecordGroup) {
		DataGroupSpy recordInfo = (DataGroupSpy) binaryRecordGroup.MCR
				.assertCalledParametersReturn("getFirstGroupWithNameInData", "recordInfo");
		recordInfo.MCR.assertParameters("removeFirstChildWithTypeAndName", 0, DataAtomic.class,
				"status");
		var statusAtomic = dataFactorySpy.MCR.assertCalledParametersReturn(
				"factorAtomicUsingNameInDataAndValue", "status", "reconvert");
		recordInfo.MCR.assertParameters("addChild", 0, statusAtomic);
	}

	private DataRecordGroupSpy setMimeTypeToBinaryRecordGroupFromStorageAndReturnRecordGroup(
			String expectedMimeType) {
		DataGroupSpy masterGroup = new DataGroupSpy();
		masterGroup.MRV.setSpecificReturnValuesSupplier("getFirstAtomicValueWithNameInData",
				() -> expectedMimeType, "mimeType");
		DataRecordGroupSpy binaryRecordGroup = new DataRecordGroupSpy();
		binaryRecordGroup.MRV.setSpecificReturnValuesSupplier("getFirstGroupWithNameInData",
				() -> masterGroup, "master");
		recordStorage.MRV.setSpecificReturnValuesSupplier("read", () -> binaryRecordGroup, "binary",
				"someId");
		return binaryRecordGroup;
	}

	private void assertBinaryIsSentToImageReconvert(String mediaType) {

		DataRecordGroupSpy binaryRecordGroup = (DataRecordGroupSpy) recordStorage.MCR
				.assertCalledParametersReturn("read", "binary", "someId");
		ResourceConvertSpy resourceConverter = (ResourceConvertSpy) dependencyProviderSpy.MCR
				.assertCalledParametersReturn("getResourceConvert");
		String datadivider = (String) binaryRecordGroup.MCR
				.assertCalledParametersReturn("getDataDivider");
		resourceConverter.MCR.assertParameters("sendMessageForAnalyzingAndConvertingImages", 0,
				datadivider, "binary", "someId", mediaType);
	}

	@Test
	public void testCallPDFReconvertIfWorkOrderIndexesABinary() {
		setUpIndexActionForWorkOrder("index");
		createWorkOrderWithTypeAndId("binary", "someId");
		DataRecordGroupSpy binaryRecordGroup = setMimeTypeToBinaryRecordGroupFromStorageAndReturnRecordGroup(
				"application/pdf");

		callExtendedFunctionalityWithGroup(workOrder);

		assertRemoveCurrentStatusAndAddReconvertStatus(binaryRecordGroup);
		assertBinaryUpdatedInStorage(binaryRecordGroup);
		assertBinaryIsSentToPDFReconvert();
	}

	private void assertBinaryIsSentToPDFReconvert() {

		DataRecordGroupSpy binaryRecordGroup = (DataRecordGroupSpy) recordStorage.MCR
				.assertCalledParametersReturn("read", "binary", "someId");
		ResourceConvertSpy resourceConverter = (ResourceConvertSpy) dependencyProviderSpy.MCR
				.assertCalledParametersReturn("getResourceConvert");
		String datadivider = (String) binaryRecordGroup.MCR
				.assertCalledParametersReturn("getDataDivider");
		resourceConverter.MCR.assertParameters("sendMessageToConvertPdfToThumbnails", 0,
				datadivider, "binary", "someId");
	}

	@Test
	public void testDoNotCallReconvertIfWorkOrderIndexesABinaryNoPdfNoImage() {
		setUpIndexActionForWorkOrder("index");
		createWorkOrderWithTypeAndId("binary", "someId");
		setMimeTypeToBinaryRecordGroupFromStorageAndReturnRecordGroup("someOtherMimeType");

		callExtendedFunctionalityWithGroup(workOrder);

		dependencyProviderSpy.MCR.assertMethodNotCalled("getResourceConvert");
	}

	@Test
	public void testDoNotCallReconvertIfWorkOrderIsNotBinary() {
		setUpIndexActionForWorkOrder("index");
		createWorkOrderWithTypeAndId("someType", "someId");

		callExtendedFunctionalityWithGroup(workOrder);

		DataRecordGroupSpy binaryRecordGroup = (DataRecordGroupSpy) recordStorage.MCR
				.assertCalledParametersReturn("read", "someType", "someId");
		binaryRecordGroup.MCR.assertMethodNotCalled("getFirstGroupWithNameInData");
		dependencyProviderSpy.MCR.assertMethodNotCalled("getResourceConvert");
	}

	@Test
	public void testIfBinaryAndPdfDoNotCallIndex() {
		setUpIndexActionForWorkOrder("index");
		createWorkOrderWithTypeAndId("binary", "someId");
		setMimeTypeToBinaryRecordGroupFromStorageAndReturnRecordGroup("application/pdf");

		callExtendedFunctionalityWithGroup(workOrder);

		termCollector.MCR.assertMethodNotCalled("collectTerms");
	}

	@Test
	public void testIfBinaryAndImageDoNotCallIndex() {
		setUpIndexActionForWorkOrder("index");
		createWorkOrderWithTypeAndId("binary", "someId");
		setMimeTypeToBinaryRecordGroupFromStorageAndReturnRecordGroup("image/some");

		callExtendedFunctionalityWithGroup(workOrder);

		termCollector.MCR.assertMethodNotCalled("collectTerms");
	}

	@Test
	public void testIfBinaryAndSomeOtherMediaTypeCallIndex() {
		setUpIndexActionForWorkOrder("index");
		createWorkOrderWithTypeAndId("binary", "someId");
		setMimeTypeToBinaryRecordGroupFromStorageAndReturnRecordGroup("someOtherMimeType");

		callExtendedFunctionalityWithGroup(workOrder);

		termCollector.MCR.assertMethodWasCalled("collectTerms");
	}

	@Test
	public void testWhenExceptionOnImageReconvert() {
		setUpIndexActionForWorkOrder("index");
		createWorkOrderWithTypeAndId("binary", "someId");
		setMimeTypeToBinaryRecordGroupFromStorageAndReturnRecordGroup("image/some");
		RuntimeException exception = new RuntimeException();
		resourceConvert.MRV.setAlwaysThrowException("sendMessageForAnalyzingAndConvertingImages",
				exception);

		callExtendedFunctionalityWithGroup(workOrder);

		logger.MCR.assertCalledParameters("logErrorUsingMessageAndException",
				"Work order failed to carry out requested operation on recordType: binary, "
						+ "recordId: someId",
				exception);
	}

	@Test
	public void testWhenExceptionOnPdfReconvert() {
		setUpIndexActionForWorkOrder("index");
		createWorkOrderWithTypeAndId("binary", "someId");
		setMimeTypeToBinaryRecordGroupFromStorageAndReturnRecordGroup("application/pdf");
		RuntimeException exception = new RuntimeException();
		resourceConvert.MRV.setAlwaysThrowException("sendMessageToConvertPdfToThumbnails",
				exception);

		callExtendedFunctionalityWithGroup(workOrder);

		logger.MCR.assertCalledParameters("logErrorUsingMessageAndException",
				"Work order failed to carry out requested operation on recordType: binary, "
						+ "recordId: someId",
				exception);
	}

	// TODO: next,
	// .. - UploaderImp must get ResourceConvert from SpiderDependencyProvider

}
