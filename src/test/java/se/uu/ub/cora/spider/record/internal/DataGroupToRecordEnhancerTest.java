/*
 * Copyright 2016, 2017, 2019, 2020, 2025 Uppsala University Library
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
import static org.testng.Assert.assertSame;
import static org.testng.Assert.assertTrue;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import se.uu.ub.cora.beefeater.authentication.User;
import se.uu.ub.cora.data.Action;
import se.uu.ub.cora.data.DataLink;
import se.uu.ub.cora.data.DataProvider;
import se.uu.ub.cora.data.DataRecord;
import se.uu.ub.cora.data.DataRecordGroup;
import se.uu.ub.cora.data.DataRecordLink;
import se.uu.ub.cora.data.collected.CollectTerms;
import se.uu.ub.cora.data.collected.PermissionTerm;
import se.uu.ub.cora.data.spies.DataFactorySpy;
import se.uu.ub.cora.data.spies.DataGroupSpy;
import se.uu.ub.cora.data.spies.DataRecordGroupSpy;
import se.uu.ub.cora.data.spies.DataRecordLinkSpy;
import se.uu.ub.cora.data.spies.DataRecordSpy;
import se.uu.ub.cora.data.spies.DataResourceLinkSpy;
import se.uu.ub.cora.logger.LoggerProvider;
import se.uu.ub.cora.spider.authorization.AuthorizationException;
import se.uu.ub.cora.spider.dependency.SpiderDependencyProvider;
import se.uu.ub.cora.spider.dependency.spy.RecordTypeHandlerOldSpy;
import se.uu.ub.cora.spider.dependency.spy.SpiderDependencyProviderOldSpy;
import se.uu.ub.cora.spider.log.LoggerFactorySpy;
import se.uu.ub.cora.spider.record.DataException;
import se.uu.ub.cora.spider.record.DataGroupToRecordEnhancer;
import se.uu.ub.cora.spider.record.RecordEnhancerTestsRecordStorage;
import se.uu.ub.cora.spider.spy.DataGroupTermCollectorSpy;
import se.uu.ub.cora.spider.spy.OldSpiderAuthorizatorSpy;
import se.uu.ub.cora.storage.spies.RecordStorageSpy;
import se.uu.ub.cora.testutils.mcr.MethodCallRecorder;
import se.uu.ub.cora.testutils.mrv.MethodReturnValues;

public class DataGroupToRecordEnhancerTest {
	private static final String SOME_PERMISSION_UNIT = "somePermissionUnit";
	private static final String UPDATE = "update";
	private static final String LIST = "list";
	private static final String DATA_WITH_LINKS = "dataWithLinks";
	private static final String CREATE = "create";
	private static final String SEARCH = "search";
	private static final String READ = "read";
	private static final String SOME_RECORD_TYPE = "someRecordType";
	private static final String BINARY_RECORD_TYPE = "binary";
	private RecordEnhancerTestsRecordStorage oldRecordStorage;
	private OldSpiderAuthorizatorSpy oldAuthorizator;
	private SpiderAuthorizatorSpy authorizator;
	private SpiderDependencyProviderOldSpy dependencyProviderOldSpy;
	private User user;
	private DataGroupToRecordEnhancer enhancer;
	private DataGroupTermCollectorSpy termCollector;
	private LoggerFactorySpy loggerFactorySpy;
	private DataFactorySpy dataFactorySpy;
	private RecordTypeHandlerOldSpy recordTypeHandlerOldSpy;
	private DataRedactorSpy dataRedactor;

	private DataRecordGroupSpy someDataRecordGroup;
	private RecordStorageSpy recordStorage;
	private LinkAuthorizatorSpy linkAuthorizator;

	@BeforeMethod
	public void setUp() {
		setUpFactoriesAndProviders();
		someDataRecordGroup = new DataRecordGroupSpy();
		someDataRecordGroup.MRV.setDefaultReturnValuesSupplier("getId", () -> "someId");
		user = new User("987654321");
		recordStorage = new RecordStorageSpy();
		oldRecordStorage = new RecordEnhancerTestsRecordStorage();
		oldAuthorizator = new OldSpiderAuthorizatorSpy();
		authorizator = new SpiderAuthorizatorSpy();
		termCollector = new DataGroupTermCollectorSpy();
		dataRedactor = new DataRedactorSpy();
		setUpDependencyProviderOld();

		linkAuthorizator = new LinkAuthorizatorSpy();
		enhancer = DataGroupToRecordEnhancerImp.usingDependencyProviderAndLinkAuthorizator(
				dependencyProviderOldSpy, linkAuthorizator);
	}

	private void setUpFactoriesAndProviders() {
		loggerFactorySpy = new LoggerFactorySpy();
		LoggerProvider.setLoggerFactory(loggerFactorySpy);
		dataFactorySpy = new DataFactorySpy();
		DataProvider.onlyForTestSetDataFactory(dataFactorySpy);
	}

	private void setUpDependencyProviderOld() {
		dependencyProviderOldSpy = new SpiderDependencyProviderOldSpy();
		dependencyProviderOldSpy.spiderAuthorizator = oldAuthorizator;
		dependencyProviderOldSpy.recordStorage = oldRecordStorage;
		dependencyProviderOldSpy.termCollector = termCollector;
		recordTypeHandlerOldSpy = dependencyProviderOldSpy.recordTypeHandlerSpy;
	}

	// private void setUpDependencyProvider() {
	// dependencyProvider = new SpiderDependencyProviderSpy();
	//
	// // authorizator = new SpiderAuthorizatorSpy();
	// dependencyProvider.MRV.setDefaultReturnValuesSupplier("getSpiderAuthorizator",
	// () -> authorizator);
	//
	// // recordStorage = new RecordStorageSpy();
	// dependencyProvider.MRV.setDefaultReturnValuesSupplier("getRecordStorage",
	// () -> recordStorage);
	//
	// // termCollector = new DataGroupTermCollectorSpy();
	// // dependencyProvider.MRV.setDefaultReturnValuesSupplier("getPermissionRuleCalculator",
	// // () -> ruleCalculator);
	//
	// // termCollector = new DataGroupTermCollectorSpy();
	// dependencyProvider.MRV.setDefaultReturnValuesSupplier("getDataGroupTermCollector",
	// () -> termCollector);
	//
	// recordTypeHandler = new RecordTypeHandlerSpy();
	// recordTypeHandler.MRV.setDefaultReturnValuesSupplier("getDefinitionId",
	// () -> "someDefintion");
	// dependencyProvider.MRV.setDefaultReturnValuesSupplier("getRecordTypeHandler",
	// () -> recordTypeHandler);
	// dependencyProvider.MRV.setDefaultReturnValuesSupplier(
	// "getRecordTypeHandlerUsingDataRecordGroup", () -> recordTypeHandler);
	//
	// }

	@Test
	public void testReadActionPartOfEnhance() {
		createRecordStorageSpy();

		DataRecordSpy recordToEnhance = (DataRecordSpy) enhancer.enhance(user, SOME_RECORD_TYPE,
				someDataRecordGroup, dataRedactor);

		assertReadActionPartOfEnhance(recordToEnhance);
	}

	private void assertReadActionPartOfEnhance(DataRecordSpy recordToEnhance) {
		assertCheckAndGetAuthorizationCalledForReadActionPartOfEnhance();
		assertRecordContainsReadAction(recordToEnhance);
	}

	private void assertCheckAndGetAuthorizationCalledForReadActionPartOfEnhance() {
		var permissionTerms = getAssertedCollectedPermissionTermsForRecordType(SOME_RECORD_TYPE,
				someDataRecordGroup);
		oldAuthorizator.MCR.assertParameters(
				"checkGetUsersMatchedRecordPartPermissionsForActionOnRecordTypeAndCollectedData", 0,
				user, READ, SOME_RECORD_TYPE, permissionTerms, true);
	}

	private void assertRecordContainsReadAction(DataRecordSpy recordToEnhance) {
		recordToEnhance.MCR.assertParameters("addAction", 0, Action.READ);
	}

	private RecordStorageOldSpy createRecordStorageSpy() {
		RecordStorageOldSpy recordStorageSpy = new RecordStorageOldSpy();
		dependencyProviderOldSpy.recordStorage = recordStorageSpy;
		enhancer = new DataGroupToRecordEnhancerImp(dependencyProviderOldSpy, linkAuthorizator);
		return recordStorageSpy;
	}

	private List<PermissionTerm> getAssertedCollectedPermissionTermsForRecordType(String recordType,
			DataRecordGroup dataRecordGroup) {
		dependencyProviderOldSpy.MCR.assertParameters("getRecordTypeHandler", 0, recordType);
		var returnedRecordTypeHandler = (RecordTypeHandlerOldSpy) dependencyProviderOldSpy.MCR
				.getReturnValue("getRecordTypeHandler", 0);

		returnedRecordTypeHandler.MCR.assertMethodWasCalled("getDefinitionId");
		String metadataIdFromRecordTypeHandler = (String) returnedRecordTypeHandler.MCR
				.getReturnValue("getDefinitionId", 0);

		dependencyProviderOldSpy.MCR.assertParameters("getDataGroupTermCollector", 0);
		DataGroupTermCollectorSpy termCollectorSpy = (DataGroupTermCollectorSpy) dependencyProviderOldSpy.MCR
				.getReturnValue("getDataGroupTermCollector", 0);

		termCollectorSpy.MCR.assertParameters("collectTerms", 0, metadataIdFromRecordTypeHandler,
				dataRecordGroup);
		return ((CollectTerms) termCollectorSpy.MCR.getReturnValue("collectTerms",
				0)).permissionTerms;
	}

	@Test
	public void testEnhanceIgnoreReadAccessReadActionPartOfEnhance() {
		createRecordStorageSpy();

		DataRecordSpy recordToEnhance = (DataRecordSpy) enhancer.enhanceIgnoringReadAccess(user,
				SOME_RECORD_TYPE, someDataRecordGroup, dataRedactor);

		assertReadActionPartOfEnhance(recordToEnhance);
	}

	@Test
	public void testReadActionPartOfEnhanceNotAuthorized() {
		setupForNoReadAccess();

		Exception auxiliaryException = runEnhanceAndCatchException();

		assertAuthorizationCheckedAndExceptionThrown(auxiliaryException);
	}

	private void setupForNoReadAccess() {
		createRecordStorageSpy();
		oldAuthorizator.authorizedForActionAndRecordTypeAndCollectedData = false;
	}

	private Exception runEnhanceAndCatchException() {
		Exception auxiliaryException = null;
		try {
			enhancer.enhance(user, SOME_RECORD_TYPE, someDataRecordGroup, null);
		} catch (AuthorizationException thrownException) {
			auxiliaryException = thrownException;
		}
		return auxiliaryException;
	}

	private void assertAuthorizationCheckedAndExceptionThrown(Exception auxiliaryException) {
		assertNotNull(auxiliaryException);
		assertTrue(auxiliaryException instanceof AuthorizationException);
		assertCheckAndGetAuthorizationCalledForReadActionPartOfEnhance();
		oldAuthorizator.MCR.assertNumberOfCallsToMethod(
				"checkGetUsersMatchedRecordPartPermissionsForActionOnRecordTypeAndCollectedData",
				1);
	}

	@Test
	public void testEnhanceIgnoreReadAccessReadActionPartOfEnhanceNotAuthorized() {
		setupForNoReadAccess();

		DataRecordSpy recordToEnhance = (DataRecordSpy) enhancer.enhanceIgnoringReadAccess(user,
				SOME_RECORD_TYPE, someDataRecordGroup, dataRedactor);

		assertCheckAndGetAuthorizationCalledForReadActionPartOfEnhance();

		assertRecordDoesNotContainReadAction(recordToEnhance);
	}

	private void assertRecordDoesNotContainReadAction(DataRecord recordToEnhance) {
		assertFalse(recordToEnhance.getActions().contains(Action.READ));
	}

	@Test
	public void testReadActionPartOfEnhanceNotAuthorizedButPublicData() {
		setupForNoReadAccessButPublicData();

		DataRecordSpy recordToEnhance = (DataRecordSpy) enhancer.enhance(user, SOME_RECORD_TYPE,
				someDataRecordGroup, dataRedactor);

		assertReadActionPartOfEnhanceNotAuthorizedButPublicData(recordToEnhance);

		var collectTerms = getCollectTermsFirstCall();
		oldAuthorizator.MCR.assertParameters(
				"checkGetUsersMatchedRecordPartPermissionsForActionOnRecordTypeAndCollectedData", 0,
				user, "update", "someRecordType", collectTerms.permissionTerms, true);
		oldAuthorizator.MCR.assertParameters(
				"userIsAuthorizedForActionOnRecordTypeAndCollectedData", 0, user, "index",
				"someRecordType", collectTerms.permissionTerms);
		oldAuthorizator.MCR.assertParameters(
				"userIsAuthorizedForActionOnRecordTypeAndCollectedData", 1, user, "delete",
				"someRecordType", collectTerms.permissionTerms);
	}

	private CollectTerms getCollectTermsFirstCall() {
		termCollector.MCR.assertParameters("collectTerms", 0,
				"fakeDefMetadataIdFromRecordTypeHandlerSpy");
		return (CollectTerms) termCollector.MCR.assertCalledParametersReturn("collectTerms",
				"fakeDefMetadataIdFromRecordTypeHandlerSpy", someDataRecordGroup);
	}

	private void setupForNoReadAccessButPublicData() {
		createRecordStorageSpy();
		recordTypeHandlerOldSpy.isPublicForRead = true;
		oldAuthorizator.authorizedForActionAndRecordTypeAndCollectedData = false;
	}

	private void assertReadActionPartOfEnhanceNotAuthorizedButPublicData(
			DataRecordSpy recordToEnhance) {
		recordTypeHandlerOldSpy.MCR.assertMethodWasCalled("isPublicForRead");
		oldAuthorizator.MCR.assertNumberOfCallsToMethod(
				"checkGetUsersMatchedRecordPartPermissionsForActionOnRecordTypeAndCollectedData",
				1);
		assertRecordContainsReadAction(recordToEnhance);
	}

	@Test
	public void testEnhanceIgnoreReadAccessReadActionPartOfEnhanceNotAuthorizedButPublicData() {
		setupForNoReadAccessButPublicData();

		DataRecordSpy recordToEnhance = (DataRecordSpy) enhancer.enhanceIgnoringReadAccess(user,
				SOME_RECORD_TYPE, someDataRecordGroup, dataRedactor);

		assertReadActionPartOfEnhanceNotAuthorizedButPublicData(recordToEnhance);

		var collectTerms = getCollectTermsFirstCall();
		oldAuthorizator.MCR.assertParameters(
				"checkGetUsersMatchedRecordPartPermissionsForActionOnRecordTypeAndCollectedData", 0,
				user, "update", "someRecordType", collectTerms.permissionTerms, true);
		oldAuthorizator.MCR.assertParameters(
				"userIsAuthorizedForActionOnRecordTypeAndCollectedData", 0, user, "index",
				"someRecordType", collectTerms.permissionTerms);
		oldAuthorizator.MCR.assertParameters(
				"userIsAuthorizedForActionOnRecordTypeAndCollectedData", 1, user, "delete",
				"someRecordType", collectTerms.permissionTerms);
	}

	@Test
	public void testUpdateActionPartOfEnhance() {
		createRecordStorageSpy();

		DataRecordSpy recordToEnhance = (DataRecordSpy) enhancer.enhance(user, SOME_RECORD_TYPE,
				someDataRecordGroup, dataRedactor);

		assertUpdateActionPartOfEnhance(recordToEnhance);
	}

	private void assertUpdateActionPartOfEnhance(DataRecordSpy recordToEnhance) {
		assertCheckAndGetAuthorizationCalledForUpdateActionPartOfEnhance();
		assertRecordContainsUpdateAction(recordToEnhance);
	}

	private void assertRecordContainsUpdateAction(DataRecordSpy recordToEnhance) {
		recordToEnhance.MCR.assertCalledParameters("addAction", Action.UPDATE);
	}

	private void assertCheckAndGetAuthorizationCalledForUpdateActionPartOfEnhance() {
		var permissionTerms = getAssertedCollectedPermissionTermsForRecordType(SOME_RECORD_TYPE,
				someDataRecordGroup);
		oldAuthorizator.MCR.assertParameters(
				"checkGetUsersMatchedRecordPartPermissionsForActionOnRecordTypeAndCollectedData", 1,
				user, UPDATE, SOME_RECORD_TYPE, permissionTerms, true);
	}

	@Test
	public void testEnhanceIgnoreReadAccessUpdateActionPartOfEnhance() {
		createRecordStorageSpy();

		DataRecordSpy enhancedRecord = (DataRecordSpy) enhancer.enhanceIgnoringReadAccess(user,
				SOME_RECORD_TYPE, someDataRecordGroup, dataRedactor);

		assertUpdateActionPartOfEnhance(enhancedRecord);
	}

	@Test
	public void testActionPartOfEnhance_recordDoesNotUsePermissionUnit() {
		changeToModernSpies();

		DataRecordSpy enhancedRecord = (DataRecordSpy) enhancer.enhance(user, BINARY_RECORD_TYPE,
				someDataRecordGroup, dataRedactor);

		enhancedRecord.MCR.assertCalledParameters("addAction", Action.READ);
		enhancedRecord.MCR.assertCalledParameters("addAction", Action.UPDATE);
		enhancedRecord.MCR.assertCalledParameters("addAction", Action.DELETE);
		enhancedRecord.MCR.assertCalledParameters("addAction", Action.INDEX);
		enhancedRecord.MCR.assertCalledParameters("addAction", Action.UPLOAD);
		enhancedRecord.MCR.assertNumberOfCallsToMethod("addAction", 5);
	}

	@Test
	public void testActionPartOfEnhance_recordUsesUsePermissionUnit_userHasPermissionUnit() {
		changeToModernSpies();
		setupRecordTypeToUsePermissionUnit(false);
		setupSomeDataRecordGroupToHaveSomePermissionUnit();
		setupDataRecordGroupInEnhancedRecordToHaveSomePermissionUnit();
		setupAuthorizatorToReturnTrueForGetUserIsAuthorizedForPemissionUnit();

		DataRecordSpy enhancedRecord = (DataRecordSpy) enhancer.enhance(user, BINARY_RECORD_TYPE,
				someDataRecordGroup, dataRedactor);

		enhancedRecord.MCR.assertCalledParameters("addAction", Action.READ);
		enhancedRecord.MCR.assertCalledParameters("addAction", Action.UPDATE);
		enhancedRecord.MCR.assertCalledParameters("addAction", Action.DELETE);
		enhancedRecord.MCR.assertCalledParameters("addAction", Action.INDEX);
		enhancedRecord.MCR.assertCalledParameters("addAction", Action.UPLOAD);
		enhancedRecord.MCR.assertNumberOfCallsToMethod("addAction", 5);
	}

	@Test
	public void testActionPartOfEnhance_recordUsesUsePermissionUnit_recordDoesNotHavePermissionUnit() {
		changeToModernSpies();
		setupRecordTypeToUsePermissionUnit(true);
		setupSomeDataRecordGroupToHaveSomePermissionUnit();
		setupDataRecordGroupInEnhancedRecordToEmptyPermissionUnit();
		setupAuthorizatorToReturnFalseForGetUserIsAuthorizedForPemissionUnit();

		DataRecordSpy enhancedRecord = (DataRecordSpy) enhancer.enhance(user, BINARY_RECORD_TYPE,
				someDataRecordGroup, dataRedactor);

		enhancedRecord.MCR.assertCalledParameters("addAction", Action.READ);
		enhancedRecord.MCR.assertNumberOfCallsToMethod("addAction", 1);
	}

	@Test
	public void testActionPartOfEnhance_recordUsesUsePermissionUnit_userDoesNotHavePermissionUnit() {
		changeToModernSpies();
		setupRecordTypeToUsePermissionUnit(true);
		setupSomeDataRecordGroupToHaveSomePermissionUnit();
		setupDataRecordGroupInEnhancedRecordToHaveSomePermissionUnit();
		setupAuthorizatorToReturnFalseForGetUserIsAuthorizedForPemissionUnit();

		DataRecordSpy enhancedRecord = (DataRecordSpy) enhancer.enhance(user, BINARY_RECORD_TYPE,
				someDataRecordGroup, dataRedactor);

		enhancedRecord.MCR.assertCalledParameters("addAction", Action.READ);
		enhancedRecord.MCR.assertNumberOfCallsToMethod("addAction", 1);
	}

	private void setupRecordTypeToUsePermissionUnit(boolean value) {
		recordTypeHandlerOldSpy.MRV.setDefaultReturnValuesSupplier("usePermissionUnit",
				() -> value);
	}

	private void setupSomeDataRecordGroupToHaveSomePermissionUnit() {
		someDataRecordGroup.MRV.setDefaultReturnValuesSupplier("getPermissionUnit",
				() -> Optional.of(SOME_PERMISSION_UNIT));
	}

	private void setupDataRecordGroupInEnhancedRecordToEmptyPermissionUnit() {
		DataRecordGroupSpy convertedDataRecordGroup = new DataRecordGroupSpy();
		convertedDataRecordGroup.MRV.setDefaultReturnValuesSupplier("getPermissionUnit",
				Optional::empty);

		DataRecordSpy convertedDataRecord = new DataRecordSpy();
		convertedDataRecord.MRV.setDefaultReturnValuesSupplier("getDataRecordGroup",
				() -> convertedDataRecordGroup);

		dataFactorySpy.MRV.setDefaultReturnValuesSupplier("factorRecordUsingDataRecordGroup",
				() -> convertedDataRecord);
	}

	private void setupDataRecordGroupInEnhancedRecordToHaveSomePermissionUnit() {
		DataRecordGroupSpy convertedDataRecordGroup = new DataRecordGroupSpy();
		convertedDataRecordGroup.MRV.setDefaultReturnValuesSupplier("getPermissionUnit",
				() -> Optional.of(SOME_PERMISSION_UNIT));

		DataRecordSpy convertedDataRecord = new DataRecordSpy();
		convertedDataRecord.MRV.setDefaultReturnValuesSupplier("getDataRecordGroup",
				() -> convertedDataRecordGroup);

		dataFactorySpy.MRV.setDefaultReturnValuesSupplier("factorRecordUsingDataRecordGroup",
				() -> convertedDataRecord);
	}

	private void setupAuthorizatorToReturnTrueForGetUserIsAuthorizedForPemissionUnit() {
		authorizator.MRV.setDefaultReturnValuesSupplier("getUserIsAuthorizedForPemissionUnit",
				() -> true);
	}

	private void setupAuthorizatorToReturnFalseForGetUserIsAuthorizedForPemissionUnit() {
		authorizator.MRV.setDefaultReturnValuesSupplier("getUserIsAuthorizedForPemissionUnit",
				() -> false);
	}

	@Test
	public void testUpdateActionPartOfEnhanceNotAuthorized() {
		setupForNoUpdateAccess();

		DataRecordSpy recordToEnhance = (DataRecordSpy) enhancer.enhance(user, SOME_RECORD_TYPE,
				someDataRecordGroup, dataRedactor);

		assertUpdateActionPartOfEnhanceNotAuthorized(recordToEnhance);
	}

	private void setupForNoUpdateAccess() {
		createRecordStorageSpy();
		oldAuthorizator.setNotAutorizedForAction(UPDATE);
	}

	private void assertUpdateActionPartOfEnhanceNotAuthorized(DataRecord recordToEnhance) {
		assertCheckAndGetAuthorizationCalledForUpdateActionPartOfEnhance();
		assertFalse(recordToEnhance.getActions().contains(Action.UPDATE));
	}

	@Test
	public void testEnhanceIgnoreReadAccessUpdateActionPartOfEnhanceNotAuthorized() {
		setupForNoUpdateAccess();

		DataRecordSpy recordToEnhance = (DataRecordSpy) enhancer.enhanceIgnoringReadAccess(user,
				SOME_RECORD_TYPE, someDataRecordGroup, dataRedactor);

		assertUpdateActionPartOfEnhanceNotAuthorized(recordToEnhance);
	}

	@Test
	public void testIndexActionPartOfEnhance() {
		createRecordStorageSpy();

		DataRecordSpy recordToEnhance = (DataRecordSpy) enhancer.enhance(user, SOME_RECORD_TYPE,
				someDataRecordGroup, dataRedactor);

		assertIndexAction(recordToEnhance);
	}

	private void assertIndexAction(DataRecordSpy recordToEnhance) {
		assertTypeAndCollectedDataAuthorizationCalledForIndexActionPartOfEnhance();
		assertRecordContainsIndexAction(recordToEnhance);
	}

	private void assertTypeAndCollectedDataAuthorizationCalledForIndexActionPartOfEnhance() {
		var permissionTerms = getAssertedCollectedPermissionTermsForRecordType(SOME_RECORD_TYPE,
				someDataRecordGroup);
		oldAuthorizator.MCR.assertParameters(
				"userIsAuthorizedForActionOnRecordTypeAndCollectedData", 0, user, "index",
				SOME_RECORD_TYPE, permissionTerms);
	}

	private void assertRecordContainsIndexAction(DataRecordSpy recordToEnhance) {
		recordToEnhance.MCR.assertCalledParameters("addAction", Action.INDEX);
	}

	@Test
	public void testBatchIndexActionPartOfEnhance() {
		createRecordStorageSpy();
		recordTypeHandlerOldSpy.MRV.setDefaultReturnValuesSupplier(
				"representsTheRecordTypeDefiningRecordTypes", () -> true);

		DataRecordSpy recordToEnhance = (DataRecordSpy) enhancer.enhance(user, SOME_RECORD_TYPE,
				someDataRecordGroup, dataRedactor);

		assertBatchIndexAction(recordToEnhance);
	}

	private void assertBatchIndexAction(DataRecordSpy recordToEnhance) {
		assertTypeAndCollectedDataAuthorizationCalledForBatchIndexActionPartOfEnhance();
		assertRecordContainsBatchIndexAction(recordToEnhance);
	}

	private void assertTypeAndCollectedDataAuthorizationCalledForBatchIndexActionPartOfEnhance() {
		oldAuthorizator.MCR.assertParameters("userIsAuthorizedForActionOnRecordType", 3, user,
				"batch_index", "someId");
	}

	private void assertRecordContainsBatchIndexAction(DataRecordSpy recordToEnhance) {
		recordToEnhance.MCR.assertCalledParameters("addAction", Action.BATCH_INDEX);
	}

	@Test
	public void testEnhanceIgnoreReadAccessBatchIndexActionPartOfEnhance() {
		createRecordStorageSpy();
		recordTypeHandlerOldSpy.MRV.setDefaultReturnValuesSupplier(
				"representsTheRecordTypeDefiningRecordTypes", () -> true);

		DataRecordSpy recordToEnhance = (DataRecordSpy) enhancer.enhanceIgnoringReadAccess(user,
				SOME_RECORD_TYPE, someDataRecordGroup, dataRedactor);

		assertBatchIndexAction(recordToEnhance);
	}

	@Test
	public void testBatchIndexActionPartOfEnhanceNotAuthorized() {
		setupForNoBatchIndexAccess();

		DataRecordSpy recordToEnhance = (DataRecordSpy) enhancer.enhance(user, SOME_RECORD_TYPE,
				someDataRecordGroup, dataRedactor);

		assertBatchIndexActionNotAuthorized(recordToEnhance);
	}

	private void setupForNoBatchIndexAccess() {
		createRecordStorageSpy();
		recordTypeHandlerOldSpy.MRV.setDefaultReturnValuesSupplier(
				"representsTheRecordTypeDefiningRecordTypes", () -> true);
		oldAuthorizator.setNotAutorizedForAction("batch_index");
	}

	private void assertBatchIndexActionNotAuthorized(DataRecord recordToEnhance) {
		assertTypeAndCollectedDataAuthorizationCalledForBatchIndexActionPartOfEnhance();
		assertRecordDoesNotContainBatchIndexAction(recordToEnhance);
	}

	private void assertRecordDoesNotContainBatchIndexAction(DataRecord recordToEnhance) {
		assertFalse(recordToEnhance.getActions().contains(Action.BATCH_INDEX));
	}

	@Test
	public void testEnhanceIgnoreReadAccessIndexActionPartOfEnhance() {
		createRecordStorageSpy();

		DataRecordSpy recordToEnhance = (DataRecordSpy) enhancer.enhanceIgnoringReadAccess(user,
				SOME_RECORD_TYPE, someDataRecordGroup, dataRedactor);

		assertIndexAction(recordToEnhance);
	}

	@Test
	public void testIndexActionPartOfEnhanceNotAuthorized() {
		setupForNoIndexAccess();

		DataRecordSpy recordToEnhance = (DataRecordSpy) enhancer.enhance(user, SOME_RECORD_TYPE,
				someDataRecordGroup, dataRedactor);

		assertIndexActionNotAuthorized(recordToEnhance);
	}

	private void setupForNoIndexAccess() {
		createRecordStorageSpy();
		oldAuthorizator.setNotAutorizedForAction("index");
	}

	private void assertIndexActionNotAuthorized(DataRecord recordToEnhance) {
		assertTypeAndCollectedDataAuthorizationCalledForIndexActionPartOfEnhance();
		assertRecordDoesNotContainIndexAction(recordToEnhance);
	}

	private void assertRecordDoesNotContainIndexAction(DataRecord recordToEnhance) {
		assertFalse(recordToEnhance.getActions().contains(Action.INDEX));
	}

	@Test
	public void testEnhanceIgnoreReadAccessIndexActionPartOfEnhanceNotAuthorized() {
		setupForNoIndexAccess();

		DataRecordSpy recordToEnhance = (DataRecordSpy) enhancer.enhanceIgnoringReadAccess(user,
				SOME_RECORD_TYPE, someDataRecordGroup, dataRedactor);

		assertIndexActionNotAuthorized(recordToEnhance);
	}

	@Test
	public void testDeleteActionPartOfEnhance() {
		createRecordStorageSpy();

		DataRecordSpy recordToEnhance = (DataRecordSpy) enhancer.enhance(user, SOME_RECORD_TYPE,
				someDataRecordGroup, dataRedactor);

		assertDeleteAction(recordToEnhance);
	}

	private void assertDeleteAction(DataRecordSpy recordToEnhance) {
		assertTypeAndCollectedDataAuthorizationCalledForDeleteActionPartOfEnhance();
		assertRecordContainsDeleteAction(recordToEnhance);
	}

	private void assertRecordContainsDeleteAction(DataRecordSpy recordToEnhance) {
		recordToEnhance.MCR.assertCalledParameters("addAction", Action.DELETE);
	}

	private void assertTypeAndCollectedDataAuthorizationCalledForDeleteActionPartOfEnhance() {
		var permissionTerms = getAssertedCollectedPermissionTermsForRecordType(SOME_RECORD_TYPE,
				someDataRecordGroup);
		oldAuthorizator.MCR.assertParameters(
				"userIsAuthorizedForActionOnRecordTypeAndCollectedData", 1, user, "delete",
				SOME_RECORD_TYPE, permissionTerms);
	}

	@Test
	public void testEnhanceIgnoreReadAccessDeleteActionPartOfEnhance() {
		createRecordStorageSpy();

		DataRecordSpy recordToEnhance = (DataRecordSpy) enhancer.enhanceIgnoringReadAccess(user,
				SOME_RECORD_TYPE, someDataRecordGroup, dataRedactor);

		assertDeleteAction(recordToEnhance);
	}

	@Test
	public void testDeleteActionPartOfEnhanceNotAuthorized() {
		setupForNoDeleteAccess();

		DataRecordSpy recordToEnhance = (DataRecordSpy) enhancer.enhance(user, SOME_RECORD_TYPE,
				someDataRecordGroup, dataRedactor);

		assertDeleteActionNotAuthorized(recordToEnhance);
	}

	private void setupForNoDeleteAccess() {
		createRecordStorageSpy();
		oldAuthorizator.setNotAutorizedForAction("delete");
	}

	private void assertDeleteActionNotAuthorized(DataRecord recordToEnhance) {
		assertTypeAndCollectedDataAuthorizationCalledForDeleteActionPartOfEnhance();
		assertRecordDoesNotContainDeleteAction(recordToEnhance);
	}

	private void assertRecordDoesNotContainDeleteAction(DataRecord recordToEnhance) {
		assertFalse(recordToEnhance.getActions().contains(Action.DELETE));
	}

	@Test
	public void testEnhanceIgnoreReadAccessDeleteActionPartOfEnhanceNotAuthorized() {
		setupForNoDeleteAccess();

		DataRecordSpy recordToEnhance = (DataRecordSpy) enhancer.enhanceIgnoringReadAccess(user,
				SOME_RECORD_TYPE, someDataRecordGroup, dataRedactor);

		assertDeleteActionNotAuthorized(recordToEnhance);
	}

	@Test
	public void testDeleteActionPartOfEnhanceAuthorizedButHasIncomingLinks() {
		setupForDeleteButIncomingLinksExists();

		DataRecordSpy recordToEnhance = (DataRecordSpy) enhancer.enhance(user, SOME_RECORD_TYPE,
				someDataRecordGroup, dataRedactor);

		assertDeleteActionAuthorizedButHasIncomingLinks(recordToEnhance);
	}

	private void setupForDeleteButIncomingLinksExists() {
		RecordStorageOldSpy recordStorageSpy = createRecordStorageSpy();
		recordStorageSpy.incomingLinksExistsForType.add(SOME_RECORD_TYPE);
	}

	private void assertDeleteActionAuthorizedButHasIncomingLinks(DataRecord recordToEnhance) {
		assertTypeAndCollectedDataAuthorizationCalledForDeleteActionPartOfEnhance();

		RecordStorageOldSpy recordStorageSpy = (RecordStorageOldSpy) dependencyProviderOldSpy
				.getRecordStorage();
		recordStorageSpy.MCR.assertNumberOfCallsToMethod("linksExistForRecord", 1);
		recordStorageSpy.MCR.assertParameters("linksExistForRecord", 0, SOME_RECORD_TYPE, "someId");

		assertRecordDoesNotContainDeleteAction(recordToEnhance);
	}

	@Test
	public void testEnhanceIgnoreReadAccessDeleteActionPartOfEnhanceAuthorizedButHasIncomingLinks() {
		setupForDeleteButIncomingLinksExists();

		DataRecordSpy recordToEnhance = (DataRecordSpy) enhancer.enhanceIgnoringReadAccess(user,
				SOME_RECORD_TYPE, someDataRecordGroup, dataRedactor);

		assertDeleteActionAuthorizedButHasIncomingLinks(recordToEnhance);
	}

	@Test
	public void testIncomingLinksActionPartOfEnhanceHasNoIncommingLinks() {
		createRecordStorageSpy();

		DataRecordSpy recordToEnhance = (DataRecordSpy) enhancer.enhance(user, SOME_RECORD_TYPE,
				someDataRecordGroup, dataRedactor);

		assertRecordDoesNotContainIncomingLinksAction(recordToEnhance);
	}

	private void assertRecordDoesNotContainIncomingLinksAction(DataRecord recordToEnhance) {
		assertFalse(recordToEnhance.getActions().contains(Action.READ_INCOMING_LINKS));
	}

	@Test
	public void testEnhanceIgnoreReadAccessIncomingLinksActionPartOfEnhanceHasNoIncommingLinks() {
		createRecordStorageSpy();

		DataRecordSpy recordToEnhance = (DataRecordSpy) enhancer.enhanceIgnoringReadAccess(user,
				SOME_RECORD_TYPE, someDataRecordGroup, dataRedactor);

		assertRecordDoesNotContainIncomingLinksAction(recordToEnhance);
	}

	@Test
	public void testIncomingLinksActionPartOfEnhanceHasIncomingLinks() {
		setupForDeleteButIncomingLinksExists();

		DataRecordSpy recordToEnhance = (DataRecordSpy) enhancer.enhance(user, SOME_RECORD_TYPE,
				someDataRecordGroup, dataRedactor);

		assertRecordContainsIncomingLinksAction(recordToEnhance);
	}

	private void assertRecordContainsIncomingLinksAction(DataRecordSpy recordToEnhance) {
		recordToEnhance.MCR.assertCalledParameters("addAction", Action.READ_INCOMING_LINKS);
	}

	@Test
	public void testEnhanceIgnoreReadAccessIncomingLinksActionPartOfEnhanceHasIncomingLinks() {
		setupForDeleteButIncomingLinksExists();

		DataRecordSpy recordToEnhance = (DataRecordSpy) enhancer.enhanceIgnoringReadAccess(user,
				SOME_RECORD_TYPE, someDataRecordGroup, dataRedactor);

		assertRecordContainsIncomingLinksAction(recordToEnhance);
	}

	@Test
	public void testUploadActionPartOfEnhance() {
		setupForUploadAction();

		DataRecordSpy recordToEnhance = (DataRecordSpy) enhancer.enhance(user, BINARY_RECORD_TYPE,
				someDataRecordGroup, dataRedactor);

		assertUploadAction(recordToEnhance);
	}

	private void setupForUploadAction() {
		createRecordStorageSpy();
	}

	private void assertUploadAction(DataRecordSpy recordToEnhance) {
		assertTypeAndCollectedDataAuthorizationCalledForUploadActionPartOfEnhance();
		assertRecordContainsUploadAction(recordToEnhance);
	}

	private void assertTypeAndCollectedDataAuthorizationCalledForUploadActionPartOfEnhance() {
		var permissionTerms = getAssertedCollectedPermissionTermsForRecordType(BINARY_RECORD_TYPE,
				someDataRecordGroup);
		oldAuthorizator.MCR.assertParameters(
				"userIsAuthorizedForActionOnRecordTypeAndCollectedData", 2, user, "upload",
				BINARY_RECORD_TYPE, permissionTerms);
	}

	private void assertRecordContainsUploadAction(DataRecordSpy recordToEnhance) {
		recordToEnhance.MCR.assertCalledParameters("addAction", Action.UPLOAD);
	}

	@Test
	public void testEnhanceIgnoreReadAccessUploadActionPartOfEnhance() {
		setupForUploadAction();

		DataRecordSpy recordToEnhance = (DataRecordSpy) enhancer.enhanceIgnoringReadAccess(user,
				BINARY_RECORD_TYPE, someDataRecordGroup, dataRedactor);

		assertUploadAction(recordToEnhance);
	}

	@Test
	public void testUploadActionPartOfEnhanceNotAuthorized() {
		setupForNoUploadAccess();

		DataRecordSpy recordToEnhance = (DataRecordSpy) enhancer.enhance(user, BINARY_RECORD_TYPE,
				someDataRecordGroup, dataRedactor);

		assertUploadActionNotAuthorized(recordToEnhance);
	}

	private void setupForNoUploadAccess() {
		setupForUploadAction();
		oldAuthorizator.setNotAutorizedForAction("upload");
	}

	private void assertUploadActionNotAuthorized(DataRecord recordToEnhance) {
		assertTypeAndCollectedDataAuthorizationCalledForUploadActionPartOfEnhance();
		assertRecordDoesNotContainUploadAction(recordToEnhance);

		oldAuthorizator.MCR.assertNumberOfCallsToMethod(
				"userIsAuthorizedForActionOnRecordTypeAndCollectedData", 3);
	}

	@Test
	public void testEnhanceIgnoreReadAccessUploadActionPartOfEnhanceNotAuthorized() {
		setupForNoUploadAccess();

		DataRecordSpy recordToEnhance = (DataRecordSpy) enhancer.enhanceIgnoringReadAccess(user,
				BINARY_RECORD_TYPE, someDataRecordGroup, dataRedactor);

		assertUploadActionNotAuthorized(recordToEnhance);
	}

	@Test
	public void testUploadActionPartOfEnhanceNotBinary() {
		createRecordStorageSpy();

		DataRecordSpy recordToEnhance = (DataRecordSpy) enhancer.enhance(user, SOME_RECORD_TYPE,
				someDataRecordGroup, dataRedactor);

		assertUploadActionNotChildOfBinary(recordToEnhance);
	}

	private void assertUploadActionNotChildOfBinary(DataRecord recordToEnhance) {
		oldAuthorizator.MCR.assertNumberOfCallsToMethod(
				"userIsAuthorizedForActionOnRecordTypeAndCollectedData", 2);
		assertRecordDoesNotContainUploadAction(recordToEnhance);
	}

	private void assertRecordDoesNotContainUploadAction(DataRecord recordToEnhance) {
		assertFalse(recordToEnhance.getActions().contains(Action.UPLOAD));
	}

	@Test
	public void testEnhanceIgnoreReadAccessUploadActionPartOfEnhanceNotChildOfBinary() {
		createRecordStorageSpy();

		DataRecordSpy recordToEnhance = (DataRecordSpy) enhancer.enhanceIgnoringReadAccess(user,
				SOME_RECORD_TYPE, someDataRecordGroup, dataRedactor);

		assertUploadActionNotChildOfBinary(recordToEnhance);
	}

	@Test
	public void testSearchActionPartOfEnhanceForRecordTypeSearch() {
		setupForSearchAction();

		DataRecordSpy recordToEnhance = (DataRecordSpy) enhancer.enhance(user, SOME_RECORD_TYPE,
				someDataRecordGroup, dataRedactor);

		assertSearchActionForRecordTypeSearch(recordToEnhance);
	}

	private void setupForSearchAction() {
		createRecordStorageSpy();
		recordTypeHandlerOldSpy.representsTheRecordTypeDefiningSearches = true;
		addSearchChildsToSomeType();
	}

	private void addSearchChildsToSomeType() {
		DataRecordLinkSpy linkSpy1 = createRecordLinkSpyUsingId("linkedSearchId1");
		DataRecordLinkSpy linkSpy2 = createRecordLinkSpyUsingId("linkedSearchId2");
		List<DataRecordLinkSpy> linkList = List.of(linkSpy1, linkSpy2);

		DataRecordGroupSpy convertedDataRecordGroup = new DataRecordGroupSpy();
		convertedDataRecordGroup.MRV.setSpecificReturnValuesSupplier("getChildrenOfTypeAndName",
				() -> linkList, DataRecordLink.class, "recordTypeToSearchIn");

		DataRecordSpy convertedSomeDataRecord = new DataRecordSpy();
		convertedSomeDataRecord.MRV.setDefaultReturnValuesSupplier("getDataRecordGroup",
				() -> convertedDataRecordGroup);

		dataFactorySpy.MRV.setDefaultReturnValuesSupplier("factorRecordUsingDataRecordGroup",
				() -> convertedSomeDataRecord);
	}

	private DataRecordLinkSpy createRecordLinkSpyUsingId(String linkedRecordId) {
		DataRecordLinkSpy linkSpy = new DataRecordLinkSpy();
		linkSpy.MRV.setDefaultReturnValuesSupplier("getLinkedRecordType",
				() -> "someRecordLinkType");
		linkSpy.MRV.setDefaultReturnValuesSupplier("getLinkedRecordId", () -> linkedRecordId);
		return linkSpy;
	}

	private void assertSearchActionForRecordTypeSearch(DataRecordSpy recordToEnhance) {
		recordTypeHandlerOldSpy.MCR
				.assertMethodWasCalled("representsTheRecordTypeDefiningSearches");
		oldAuthorizator.MCR.assertParameters("userIsAuthorizedForActionOnRecordType", 0, user,
				SEARCH, "linkedSearchId1");
		oldAuthorizator.MCR.assertParameters("userIsAuthorizedForActionOnRecordType", 1, user,
				SEARCH, "linkedSearchId2");
		oldAuthorizator.MCR.assertNumberOfCallsToMethod("userIsAuthorizedForActionOnRecordType", 2);
		assertRecordContainsSearchAction(recordToEnhance);
	}

	private void assertRecordContainsSearchAction(DataRecordSpy recordToEnhance) {
		recordToEnhance.MCR.assertCalledParameters("addAction", Action.SEARCH);
	}

	@Test
	public void testEnhanceIgnoreReadAccessSearchActionPartOfEnhanceForRecordTypeSearch() {
		setupForSearchAction();

		DataRecordSpy recordToEnhance = (DataRecordSpy) enhancer.enhanceIgnoringReadAccess(user,
				SOME_RECORD_TYPE, someDataRecordGroup, dataRedactor);

		assertSearchActionForRecordTypeSearch(recordToEnhance);
	}

	@Test
	public void testSearchActionPartOfEnhanceForRecordTypeSearchNotAuthorized() {
		setupForSearchActionNotAuthorized();

		DataRecordSpy recordToEnhance = (DataRecordSpy) enhancer.enhance(user, SOME_RECORD_TYPE,
				someDataRecordGroup, dataRedactor);

		assertSearchActionForRecordTypeSearchNotAuthorized(recordToEnhance);
	}

	private void setupForSearchActionNotAuthorized() {
		createRecordStorageSpy();
		oldAuthorizator.authorizedForActionAndRecordType = false;
		recordTypeHandlerOldSpy.representsTheRecordTypeDefiningSearches = true;
		addSearchChildsToSomeType();
	}

	private void assertSearchActionForRecordTypeSearchNotAuthorized(DataRecord recordToEnhance) {
		recordTypeHandlerOldSpy.MCR
				.assertMethodWasCalled("representsTheRecordTypeDefiningSearches");
		oldAuthorizator.MCR.assertParameters("userIsAuthorizedForActionOnRecordType", 0, user,
				SEARCH, "linkedSearchId1");
		oldAuthorizator.MCR.assertNumberOfCallsToMethod("userIsAuthorizedForActionOnRecordType", 1);
		assertRecordDoesNotContainSearchAction(recordToEnhance);
	}

	private void assertRecordDoesNotContainSearchAction(DataRecord recordToEnhance) {
		assertFalse(recordToEnhance.getActions().contains(Action.SEARCH));
	}

	@Test
	public void testEnhanceIgnoreReadAccessSearchActionPartOfEnhanceForRecordTypeSearchNotAuthorized() {
		setupForSearchActionNotAuthorized();

		DataRecordSpy recordToEnhance = (DataRecordSpy) enhancer.enhanceIgnoringReadAccess(user,
				SOME_RECORD_TYPE, someDataRecordGroup, dataRedactor);

		assertSearchActionForRecordTypeSearchNotAuthorized(recordToEnhance);
	}

	@Test
	public void testSearchActionPartOfEnhanceForRecordTypeSearchNoSearchType() {
		createRecordStorageSpy();

		DataRecordSpy recordToEnhance = (DataRecordSpy) enhancer.enhance(user, SOME_RECORD_TYPE,
				someDataRecordGroup, dataRedactor);

		assertSearchActionForRecordTypeSearchNoSearchType(recordToEnhance);
	}

	private void assertSearchActionForRecordTypeSearchNoSearchType(DataRecord recordToEnhance) {
		recordTypeHandlerOldSpy.MCR
				.assertMethodWasCalled("representsTheRecordTypeDefiningSearches");
		assertRecordDoesNotContainSearchAction(recordToEnhance);
	}

	@Test
	public void testEnhanceIgnoreReadAccessSearchActionPartOfEnhanceForRecordTypeSearchNoSearchType() {
		createRecordStorageSpy();

		DataRecordSpy recordToEnhance = (DataRecordSpy) enhancer.enhanceIgnoringReadAccess(user,
				SOME_RECORD_TYPE, someDataRecordGroup, dataRedactor);

		assertSearchActionForRecordTypeSearchNoSearchType(recordToEnhance);
	}

	@Test
	public void testCreateActionPartOfEnhanceForRecordTypeRecordType() {
		setupForCreateAction();

		DataRecordSpy recordToEnhance = (DataRecordSpy) enhancer.enhance(user, SOME_RECORD_TYPE,
				someDataRecordGroup, dataRedactor);

		assertCreateActionForRecordTypeRecordType(recordToEnhance);
	}

	private void setupForCreateAction() {
		createRecordStorageSpy();
		recordTypeHandlerOldSpy.MRV.setDefaultReturnValuesSupplier(
				"representsTheRecordTypeDefiningRecordTypes", () -> true);
	}

	private void assertCreateActionForRecordTypeRecordType(DataRecordSpy recordToEnhance) {
		recordTypeHandlerOldSpy.MCR
				.assertMethodWasCalled("representsTheRecordTypeDefiningRecordTypes");
		dependencyProviderOldSpy.MCR.assertNumberOfCallsToMethod("getRecordTypeHandler", 2);
		dependencyProviderOldSpy.MCR.assertParameters("getRecordTypeHandler", 1, "someId");

		oldAuthorizator.MCR.assertParameters("userIsAuthorizedForActionOnRecordType", 0, user,
				CREATE, "someId");
		assertRecordContainsCreateAction(recordToEnhance);
	}

	private void assertRecordContainsCreateAction(DataRecordSpy recordToEnhance) {
		recordToEnhance.MCR.assertCalledParameters("addAction", Action.CREATE);
	}

	@Test
	public void testEnhanceIgnoreReadAccessCreateActionPartOfEnhanceForRecordTypeRecordType() {
		setupForCreateAction();

		DataRecordSpy recordToEnhance = (DataRecordSpy) enhancer.enhanceIgnoringReadAccess(user,
				SOME_RECORD_TYPE, someDataRecordGroup, dataRedactor);

		assertCreateActionForRecordTypeRecordType(recordToEnhance);
	}

	@Test
	public void testCreateActionPartOfEnhanceForRecordTypeRecordTypeNotAuthorized() {
		setupForCreateActionNotAuthorized();

		DataRecordSpy recordToEnhance = (DataRecordSpy) enhancer.enhance(user, SOME_RECORD_TYPE,
				someDataRecordGroup, dataRedactor);

		assertCreateActionForRecordTypeRecordTypeNotAuthorized(recordToEnhance);
	}

	private void setupForCreateActionNotAuthorized() {
		createRecordStorageSpy();
		someDataRecordGroup = new DataRecordGroupSpy();
		someDataRecordGroup.MRV.setDefaultReturnValuesSupplier("getId", () -> "otherId");
		recordTypeHandlerOldSpy.MRV.setDefaultReturnValuesSupplier(
				"representsTheRecordTypeDefiningRecordTypes", () -> true);
		oldAuthorizator.authorizedForActionAndRecordType = false;
	}

	private void assertCreateActionForRecordTypeRecordTypeNotAuthorized(
			DataRecord recordToEnhance) {
		recordTypeHandlerOldSpy.MCR
				.assertMethodWasCalled("representsTheRecordTypeDefiningRecordTypes");
		oldAuthorizator.MCR.assertParameters("userIsAuthorizedForActionOnRecordType", 0, user,
				CREATE, "otherId");
		assertRecordDoesNotContainCreateAction(recordToEnhance);
	}

	private void assertRecordDoesNotContainCreateAction(DataRecord recordToEnhance) {
		assertFalse(recordToEnhance.getActions().contains(Action.CREATE));
	}

	@Test
	public void testEnhanceIgnoreReadAccessCreateActionPartOfEnhanceForRecordTypeRecordTypeNotAuthorized() {
		setupForCreateActionNotAuthorized();

		DataRecordSpy recordToEnhance = (DataRecordSpy) enhancer.enhanceIgnoringReadAccess(user,
				SOME_RECORD_TYPE, someDataRecordGroup, dataRedactor);

		assertCreateActionForRecordTypeRecordTypeNotAuthorized(recordToEnhance);
	}

	@Test
	public void testCreateActionPartOfEnhanceForRecordTypeRecordTypeIsNotRecordType() {
		createRecordStorageSpy();

		DataRecordSpy recordToEnhance = (DataRecordSpy) enhancer.enhance(user, SOME_RECORD_TYPE,
				someDataRecordGroup, dataRedactor);

		assertCreateActionForRecordTypeRecordTypeIsNotRecordType(recordToEnhance);
	}

	private void assertCreateActionForRecordTypeRecordTypeIsNotRecordType(
			DataRecord recordToEnhance) {
		recordTypeHandlerOldSpy.MCR
				.assertMethodWasCalled("representsTheRecordTypeDefiningRecordTypes");
		recordTypeHandlerOldSpy.MCR.assertMethodNotCalled("isAbstract");
		assertRecordDoesNotContainCreateAction(recordToEnhance);
	}

	@Test
	public void testEnhanceIgnoreReadAccessCreateActionPartOfEnhanceForRecordTypeRecordTypeIsNotRecordType() {
		createRecordStorageSpy();

		DataRecordSpy recordToEnhance = (DataRecordSpy) enhancer.enhanceIgnoringReadAccess(user,
				SOME_RECORD_TYPE, someDataRecordGroup, dataRedactor);

		assertCreateActionForRecordTypeRecordTypeIsNotRecordType(recordToEnhance);
	}

	@Test
	public void testListActionPartOfEnhanceForRecordTypeRecordType() {
		setupForCreateAction();

		DataRecordSpy recordToEnhance = (DataRecordSpy) enhancer.enhance(user, SOME_RECORD_TYPE,
				someDataRecordGroup, dataRedactor);

		assertListActionForRecordTypeRecordType(recordToEnhance);
	}

	private void assertListActionForRecordTypeRecordType(DataRecordSpy recordToEnhance) {
		recordTypeHandlerOldSpy.MCR
				.assertMethodWasCalled("representsTheRecordTypeDefiningRecordTypes");
		oldAuthorizator.MCR.assertParameters("userIsAuthorizedForActionOnRecordType", 1, user, LIST,
				"someId");
		assertRecordContainsListAction(recordToEnhance);
	}

	private void assertRecordContainsListAction(DataRecordSpy recordToEnhance) {
		recordToEnhance.MCR.assertCalledParameters("addAction", Action.LIST);
	}

	@Test
	public void testEnhanceIgnoreReadAccessListActionPartOfEnhanceForRecordTypeRecordType() {
		setupForCreateAction();

		DataRecordSpy recordToEnhance = (DataRecordSpy) enhancer.enhanceIgnoringReadAccess(user,
				SOME_RECORD_TYPE, someDataRecordGroup, dataRedactor);

		assertListActionForRecordTypeRecordType(recordToEnhance);
	}

	@Test
	public void testListActionPartOfEnhanceForRecordTypeRecordTypeNotAuthorized() {
		setupForListActionNotAuthorized();

		DataRecordSpy recordToEnhance = (DataRecordSpy) enhancer.enhance(user, SOME_RECORD_TYPE,
				someDataRecordGroup, dataRedactor);

		assertListActionForRecordTypeRecordTypeNotAuthorized(recordToEnhance);
	}

	private void setupForListActionNotAuthorized() {
		setupForCreateAction();
		oldAuthorizator.authorizedForActionAndRecordType = false;
	}

	private void assertListActionForRecordTypeRecordTypeNotAuthorized(DataRecord recordToEnhance) {
		recordTypeHandlerOldSpy.MCR
				.assertMethodWasCalled("representsTheRecordTypeDefiningRecordTypes");
		oldAuthorizator.MCR.assertParameters("userIsAuthorizedForActionOnRecordType", 1, user, LIST,
				"someId");
		assertRecordDoesNotContainListAction(recordToEnhance);
	}

	@Test
	public void testEnhanceIgnoreReadAccessListActionPartOfEnhanceForRecordTypeRecordTypeNotAuthorized() {
		setupForListActionNotAuthorized();

		DataRecordSpy recordToEnhance = (DataRecordSpy) enhancer.enhanceIgnoringReadAccess(user,
				SOME_RECORD_TYPE, someDataRecordGroup, dataRedactor);

		assertListActionForRecordTypeRecordTypeNotAuthorized(recordToEnhance);
	}

	@Test
	public void testListActionPartOfEnhanceForRecordTypeRecordTypeNotRecordType() {
		createRecordStorageSpy();

		DataRecordSpy recordToEnhance = (DataRecordSpy) enhancer.enhance(user, SOME_RECORD_TYPE,
				someDataRecordGroup, dataRedactor);

		assertListActionForRecordTypeRecordTypeNotRecordType(recordToEnhance);
	}

	private void assertListActionForRecordTypeRecordTypeNotRecordType(DataRecord recordToEnhance) {
		recordTypeHandlerOldSpy.MCR
				.assertMethodWasCalled("representsTheRecordTypeDefiningRecordTypes");

		assertRecordDoesNotContainListAction(recordToEnhance);
	}

	private void assertRecordDoesNotContainListAction(DataRecord recordToEnhance) {
		assertFalse(recordToEnhance.getActions().contains(Action.LIST));
	}

	@Test
	public void testEnhanceIgnoreReadAccessListActionPartOfEnhanceForRecordTypeRecordTypeNotRecordType() {
		createRecordStorageSpy();

		DataRecordSpy recordToEnhance = (DataRecordSpy) enhancer.enhanceIgnoringReadAccess(user,
				SOME_RECORD_TYPE, someDataRecordGroup, dataRedactor);

		assertListActionForRecordTypeRecordTypeNotRecordType(recordToEnhance);
	}

	@Test
	public void testValidateActionPartOfEnhanceForRecordTypeRecordType() {
		setupForCreateAction();

		DataRecordSpy recordToEnhance = (DataRecordSpy) enhancer.enhance(user, SOME_RECORD_TYPE,
				someDataRecordGroup, dataRedactor);

		assertValidateActionForRecordTypeRecordType(recordToEnhance);
	}

	private void assertValidateActionForRecordTypeRecordType(DataRecordSpy recordToEnhance) {
		recordTypeHandlerOldSpy.MCR
				.assertMethodWasCalled("representsTheRecordTypeDefiningRecordTypes");
		oldAuthorizator.MCR.assertParameters("userIsAuthorizedForActionOnRecordType", 2, user,
				"validate", "someId");
		recordToEnhance.MCR.assertCalledParameters("addAction", Action.VALIDATE);
	}

	@Test
	public void testEnhanceIgnoreReadAccessValidateActionPartOfEnhanceForRecordTypeRecordType() {
		setupForCreateAction();

		DataRecordSpy recordToEnhance = (DataRecordSpy) enhancer.enhanceIgnoringReadAccess(user,
				SOME_RECORD_TYPE, someDataRecordGroup, dataRedactor);

		assertValidateActionForRecordTypeRecordType(recordToEnhance);
	}

	@Test
	public void testValidateActionPartOfEnhanceForRecordTypeRecordTypeNotAuthorized() {
		setupForCreateActionNotAuthorized();

		DataRecordSpy recordToEnhance = (DataRecordSpy) enhancer.enhance(user, SOME_RECORD_TYPE,
				someDataRecordGroup, dataRedactor);

		assertValidateActionForRecordTypeRecordTypeNotAuthorized(recordToEnhance);
	}

	private void assertValidateActionForRecordTypeRecordTypeNotAuthorized(
			DataRecord recordToEnhance) {
		recordTypeHandlerOldSpy.MCR
				.assertMethodWasCalled("representsTheRecordTypeDefiningRecordTypes");
		oldAuthorizator.MCR.assertParameters("userIsAuthorizedForActionOnRecordType", 2, user,
				"validate", "otherId");
		assertValidateActionForRecordTypeRecordTypeNotRecordType(recordToEnhance);
	}

	@Test
	public void testEnhanceIgnoreReadAccessValidateActionPartOfEnhanceForRecordTypeRecordTypeNotAuthorized() {
		setupForCreateActionNotAuthorized();

		DataRecordSpy recordToEnhance = (DataRecordSpy) enhancer.enhanceIgnoringReadAccess(user,
				SOME_RECORD_TYPE, someDataRecordGroup, dataRedactor);

		assertValidateActionForRecordTypeRecordTypeNotAuthorized(recordToEnhance);
	}

	@Test
	public void testValidateActionPartOfEnhanceForRecordTypeRecordTypeNotRecordType() {
		createRecordStorageSpy();

		DataRecordSpy recordToEnhance = (DataRecordSpy) enhancer.enhance(user, SOME_RECORD_TYPE,
				someDataRecordGroup, dataRedactor);

		assertValidateActionForRecordTypeRecordTypeNotRecordType(recordToEnhance);
	}

	private void assertValidateActionForRecordTypeRecordTypeNotRecordType(
			DataRecord recordToEnhance) {
		recordTypeHandlerOldSpy.MCR
				.assertMethodWasCalled("representsTheRecordTypeDefiningRecordTypes");
		assertRecordContainsValidateAction(recordToEnhance);
	}

	private void assertRecordContainsValidateAction(DataRecord recordToEnhance) {
		assertFalse(recordToEnhance.getActions().contains(Action.VALIDATE));
	}

	@Test
	public void testEnhanceIgnoreReadAccessValidateActionPartOfEnhanceForRecordTypeRecordTypeNotRecordType() {
		createRecordStorageSpy();

		DataRecordSpy recordToEnhance = (DataRecordSpy) enhancer.enhanceIgnoringReadAccess(user,
				SOME_RECORD_TYPE, someDataRecordGroup, dataRedactor);

		assertValidateActionForRecordTypeRecordTypeNotRecordType(recordToEnhance);
	}

	@Test
	public void testSearchActionPartOfEnhancedWhenEnhancingDataGroupContainingRecordTypeRecord() {
		RecordTypeHandlerOldSpy recordTypeHandlerForRecordTypeInData = setupForSearchActionWhenEnhancingTypeOfRecordType();

		DataRecordSpy recordToEnhance = (DataRecordSpy) enhancer.enhance(user, SOME_RECORD_TYPE,
				someDataRecordGroup, dataRedactor);

		assertSearchActionForRecordTypeRecordType(recordTypeHandlerForRecordTypeInData,
				recordToEnhance);
	}

	private RecordTypeHandlerOldSpy setupForSearchActionWhenEnhancingTypeOfRecordType() {
		createRecordStorageSpy();
		createAndSetReturnDataGroupForReadInStorageForReadOfLinkedSearch();
		recordTypeHandlerOldSpy.MRV.setDefaultReturnValuesSupplier(
				"representsTheRecordTypeDefiningRecordTypes", () -> true);
		return getRecordTypeHandlerForRecordTypeInData();
	}

	private RecordTypeHandlerOldSpy getRecordTypeHandlerForRecordTypeInData() {
		RecordTypeHandlerOldSpy recordTypeHandlerForRecordTypeInData = dependencyProviderOldSpy
				.createRecordTypeHandlerSpy("someId");
		recordTypeHandlerForRecordTypeInData.hasLinkedSearch = true;
		return recordTypeHandlerForRecordTypeInData;
	}

	private void assertSearchActionForRecordTypeRecordType(
			RecordTypeHandlerOldSpy recordTypeHandlerForRecordTypeInData,
			DataRecordSpy recordToEnhance) {
		recordTypeHandlerOldSpy.MCR
				.assertMethodWasCalled("representsTheRecordTypeDefiningRecordTypes");
		dependencyProviderOldSpy.MCR.assertNumberOfCallsToMethod("getRecordTypeHandler", 2);
		dependencyProviderOldSpy.MCR.assertParameters("getRecordTypeHandler", 1, "someId");
		dependencyProviderOldSpy.MCR.assertReturn("getRecordTypeHandler", 1,
				recordTypeHandlerForRecordTypeInData);

		recordTypeHandlerForRecordTypeInData.MCR.assertMethodWasCalled("hasLinkedSearch");
		recordTypeHandlerForRecordTypeInData.MCR.getReturnValue("getSearchId", 0);

		var oldReturnedRecordStorage = (RecordStorageOldSpy) dependencyProviderOldSpy
				.getRecordStorage();

		assertReadForSearchRecordToOldRecordStorageSpy(oldReturnedRecordStorage);

		oldAuthorizator.MCR.assertParameters("userIsAuthorizedForActionOnRecordType", 3, user,
				SEARCH, "linkedSearchId1");
		oldAuthorizator.MCR.assertParameters("userIsAuthorizedForActionOnRecordType", 4, user,
				SEARCH, "linkedSearchId2");
		oldAuthorizator.MCR.assertNumberOfCallsToMethod("userIsAuthorizedForActionOnRecordType", 6);
		assertRecordContainsSearchAction(recordToEnhance);
	}

	private void assertReadForSearchRecordToOldRecordStorageSpy(RecordStorageOldSpy recordStorage) {
		recordStorage.MCR.assertNumberOfCallsToMethod("read", 1);
		recordStorage.MCR.assertParameterAsEqual("read", 0, "type", SEARCH);
		recordStorage.MCR.assertParameter("read", 0, "id", "someSearchId");
	}

	@Test
	public void testEnhanceIgnoreReadAccessSearchActionPartOfEnhanceForRecordTypeRecordType() {
		RecordTypeHandlerOldSpy recordTypeHandlerForRecordTypeInData = setupForSearchActionWhenEnhancingTypeOfRecordType();

		DataRecordSpy recordToEnhance = (DataRecordSpy) enhancer.enhanceIgnoringReadAccess(user,
				SOME_RECORD_TYPE, someDataRecordGroup, dataRedactor);

		assertSearchActionForRecordTypeRecordType(recordTypeHandlerForRecordTypeInData,
				recordToEnhance);
	}

	@Test
	public void testSearchActionPartOfEnhanceForRecordTypeRecordTypeNotAuthorized() {
		RecordTypeHandlerOldSpy recordTypeHandlerForRecordTypeInData = setupForSearchActionWhenEnhancingTypeOfRecordTypeNotAuthorized();

		DataRecordSpy recordToEnhance = (DataRecordSpy) enhancer.enhance(user, SOME_RECORD_TYPE,
				someDataRecordGroup, dataRedactor);

		assertSearchActionForRecordTypeRecordTypeNotAuthorized(recordTypeHandlerForRecordTypeInData,
				recordToEnhance);
	}

	private RecordTypeHandlerOldSpy setupForSearchActionWhenEnhancingTypeOfRecordTypeNotAuthorized() {
		oldAuthorizator.authorizedForActionAndRecordType = false;
		createRecordStorageSpy();
		createAndSetReturnDataGroupForReadInStorageForReadOfLinkedSearch();
		recordTypeHandlerOldSpy.MRV.setDefaultReturnValuesSupplier(
				"representsTheRecordTypeDefiningRecordTypes", () -> true);

		return getRecordTypeHandlerForRecordTypeInData();
	}

	private void assertSearchActionForRecordTypeRecordTypeNotAuthorized(
			RecordTypeHandlerOldSpy recordTypeHandlerForRecordTypeInData,
			DataRecord recordToEnhance) {
		recordTypeHandlerOldSpy.MCR
				.assertMethodWasCalled("representsTheRecordTypeDefiningRecordTypes");
		dependencyProviderOldSpy.MCR.assertNumberOfCallsToMethod("getRecordTypeHandler", 2);
		dependencyProviderOldSpy.MCR.assertParameters("getRecordTypeHandler", 1, "someId");
		dependencyProviderOldSpy.MCR.assertReturn("getRecordTypeHandler", 1,
				recordTypeHandlerForRecordTypeInData);

		recordTypeHandlerForRecordTypeInData.MCR.assertMethodWasCalled("hasLinkedSearch");
		String returnedSearchId = (String) recordTypeHandlerForRecordTypeInData.MCR
				.getReturnValue("getSearchId", 0);
		var oldReturnedRecordStorage = (RecordStorageOldSpy) dependencyProviderOldSpy
				.getRecordStorage();

		assertReadForSearchRecordToOldRecordStorageSpy(oldReturnedRecordStorage);

		oldReturnedRecordStorage.MCR.assertParameter("read", 0, "id", returnedSearchId);
		oldAuthorizator.MCR.assertParameters("userIsAuthorizedForActionOnRecordType", 3, user,
				SEARCH, "linkedSearchId1");
		oldAuthorizator.MCR.assertNumberOfCallsToMethod("userIsAuthorizedForActionOnRecordType", 5);
		assertRecordDoesNotContainSearchAction(recordToEnhance);
	}

	private void createAndSetReturnDataGroupForReadInStorageForReadOfLinkedSearch() {
		DataRecordLinkSpy linkSpy21 = createRecordLinkSpyUsingId("linkedSearchId1");
		DataRecordLinkSpy linkSpy22 = createRecordLinkSpyUsingId("linkedSearchId2");
		List<DataRecordLinkSpy> linkList2 = List.of(linkSpy21, linkSpy22);

		DataRecordGroupSpy searchGroupLinkedFromRecordType = new DataRecordGroupSpy();
		searchGroupLinkedFromRecordType.MRV.setSpecificReturnValuesSupplier(
				"getChildrenOfTypeAndName", () -> linkList2, DataRecordLink.class,
				"recordTypeToSearchIn");

		var oldReturnedRecordStorage = (RecordStorageOldSpy) dependencyProviderOldSpy
				.getRecordStorage();
		oldReturnedRecordStorage.MRV.setSpecificReturnValuesSupplier("read",
				() -> searchGroupLinkedFromRecordType, "search", "someSearchId");
	}

	@Test
	public void testEnhanceIgnoreReadAccessSearchActionPartOfEnhanceForRecordTypeRecordTypeNotAuthorized() {
		RecordTypeHandlerOldSpy recordTypeHandlerForRecordTypeInData = setupForSearchActionWhenEnhancingTypeOfRecordTypeNotAuthorized();

		DataRecordSpy recordToEnhance = (DataRecordSpy) enhancer.enhanceIgnoringReadAccess(user,
				SOME_RECORD_TYPE, someDataRecordGroup, dataRedactor);

		assertSearchActionForRecordTypeRecordTypeNotAuthorized(recordTypeHandlerForRecordTypeInData,
				recordToEnhance);
	}

	@Test
	public void testReadPermissionsAreAddedToRecord() {
		createRecordStorageSpy();

		enhancer.enhance(user, SOME_RECORD_TYPE, someDataRecordGroup, dataRedactor);

		assertReadPermissionsAreAddedToRecord();
	}

	private void assertReadPermissionsAreAddedToRecord() {
		String expectedPermissions = "someRecordType.someReadMetadataId";

		oldAuthorizator.MCR.assertParameter(
				"checkGetUsersMatchedRecordPartPermissionsForActionOnRecordTypeAndCollectedData", 0,
				"action", READ);
		Set<?> readPermissions = (Set<?>) oldAuthorizator.MCR.getReturnValue(
				"checkGetUsersMatchedRecordPartPermissionsForActionOnRecordTypeAndCollectedData",
				0);
		assertTrue(readPermissions.contains(expectedPermissions));
		assertTrue(readPermissions.size() == 1);
	}

	@Test
	public void testEnhanceIgnoreReadAccessReadPermissionsAreAddedToRecord() {
		createRecordStorageSpy();

		enhancer.enhanceIgnoringReadAccess(user, SOME_RECORD_TYPE, someDataRecordGroup,
				dataRedactor);

		assertReadPermissionsAreAddedToRecord();
	}

	@Test
	public void testEnhanceIgnoreReadAccessReadPermissionsAreAddedToRecordWhenNoReadAccess() {
		setupForNoReadAccess();

		DataRecordSpy recordToEnhance = (DataRecordSpy) enhancer.enhanceIgnoringReadAccess(user,
				SOME_RECORD_TYPE, someDataRecordGroup, dataRedactor);

		assertReadPermissionsAreAddedToRecordPublicData(recordToEnhance);
	}

	@Test
	public void testReadPermissionsAreAddedToRecordPublicData() {
		setupForPublicRecord();

		DataRecordSpy recordToEnhance = (DataRecordSpy) enhancer.enhance(user, SOME_RECORD_TYPE,
				someDataRecordGroup, dataRedactor);

		assertReadPermissionsAreAddedToRecordPublicData(recordToEnhance);
	}

	private void setupForPublicRecord() {
		recordTypeHandlerOldSpy.isPublicForRead = true;
		createRecordStorageSpy();
	}

	private void assertReadPermissionsAreAddedToRecordPublicData(DataRecord recordToEnhance) {
		assertEquals(recordToEnhance.getReadPermissions(), Collections.emptySet());
	}

	@Test
	public void testEnhanceIgnoreReadAccessReadPermissionsAreAddedToRecordPublicData() {
		setupForPublicRecord();

		DataRecordSpy recordToEnhance = (DataRecordSpy) enhancer.enhanceIgnoringReadAccess(user,
				SOME_RECORD_TYPE, someDataRecordGroup, dataRedactor);

		assertReadPermissionsAreAddedToRecordPublicData(recordToEnhance);
	}

	@Test
	public void testWritePermissionsAreAddedToRecord() {
		createRecordStorageSpy();

		DataRecordSpy recordToEnhance = (DataRecordSpy) enhancer.enhance(user, SOME_RECORD_TYPE,
				someDataRecordGroup, dataRedactor);

		assertWritePermissionsAreAddedToRecord(recordToEnhance);
	}

	@Test
	public void testEmptyWritePermissionsAreAddedToRecord_whenNoPermissionUnitAccess() {
		changeToModernSpies();
		setupRecordTypeToUsePermissionUnit(false);
		setupSomeDataRecordGroupToHaveSomePermissionUnit();
		setupDataRecordGroupInEnhancedRecordToEmptyPermissionUnit();
		setupAuthorizatorToReturnFalseForGetUserIsAuthorizedForPemissionUnit();

		DataRecordSpy recordToEnhance = (DataRecordSpy) enhancer.enhance(user, SOME_RECORD_TYPE,
				someDataRecordGroup, dataRedactor);

		Set<?> call1 = (Set<?>) recordToEnhance.MCR.getParameterForMethodAndCallNumberAndParameter(
				"addWritePermissions", 0, "writePermissions");
		assertEquals(call1, Collections.emptySet());
	}

	private void assertWritePermissionsAreAddedToRecord(DataRecordSpy recordToEnhance) {
		oldAuthorizator.MCR.assertParameter(
				"checkGetUsersMatchedRecordPartPermissionsForActionOnRecordTypeAndCollectedData", 1,
				"action", UPDATE);
		var writePermissions = recordToEnhance.MCR.getParameterForMethodAndCallNumberAndParameter(
				"addWritePermissions", 0, "writePermissions");
		oldAuthorizator.MCR.assertReturn(
				"checkGetUsersMatchedRecordPartPermissionsForActionOnRecordTypeAndCollectedData", 1,
				writePermissions);

		recordToEnhance.MCR.assertNumberOfCallsToMethod("addReadPermissions", 2);
		Set<?> call1 = (Set<?>) recordToEnhance.MCR.getParameterForMethodAndCallNumberAndParameter(
				"addReadPermissions", 0, "readPermissions");
		Set<?> call2 = (Set<?>) recordToEnhance.MCR.getParameterForMethodAndCallNumberAndParameter(
				"addReadPermissions", 1, "readPermissions");

		assertTrue(call1.contains("someRecordType.someReadMetadataId"));
		assertTrue(call2.contains("someRecordType.someWriteMetadataId"));
	}

	@Test
	public void testEnhanceIgnoreReadAccessWritePermissionsAreAddedToRecord() {
		createRecordStorageSpy();

		DataRecordSpy recordToEnhance = (DataRecordSpy) enhancer.enhanceIgnoringReadAccess(user,
				SOME_RECORD_TYPE, someDataRecordGroup, dataRedactor);

		assertWritePermissionsAreAddedToRecord(recordToEnhance);
	}

	@Test
	public void testWritePermissionsAreAddedToRecordNotAutorized() {
		setupForNoAccessOnUpdateActionOnRecordType();

		DataRecordSpy recordToEnhance = (DataRecordSpy) enhancer.enhance(user, SOME_RECORD_TYPE,
				someDataRecordGroup, dataRedactor);

		assertWritePermissionsAreAddedToRecordNotAutorized(recordToEnhance);
	}

	private void setupForNoAccessOnUpdateActionOnRecordType() {
		createRecordStorageSpy();
		oldAuthorizator.setNotAutorizedForActionOnRecordType(UPDATE, SOME_RECORD_TYPE);
	}

	private void assertWritePermissionsAreAddedToRecordNotAutorized(DataRecord recordToEnhance) {
		oldAuthorizator.MCR.assertParameter(
				"checkGetUsersMatchedRecordPartPermissionsForActionOnRecordTypeAndCollectedData", 1,
				"action", UPDATE);
		assertEquals(recordToEnhance.getWritePermissions(), Collections.emptySet());
	}

	@Test
	public void testEnhanceIgnoreReadAccessWritePermissionsAreAddedToRecordNotAutorized() {
		setupForNoAccessOnUpdateActionOnRecordType();

		DataRecordSpy recordToEnhance = (DataRecordSpy) enhancer.enhanceIgnoringReadAccess(user,
				SOME_RECORD_TYPE, someDataRecordGroup, dataRedactor);

		assertWritePermissionsAreAddedToRecordNotAutorized(recordToEnhance);
	}

	@Test
	public void testRedactData() {
		createRecordStorageSpy();

		DataRecordSpy recordToEnhance = (DataRecordSpy) enhancer.enhance(user, SOME_RECORD_TYPE,
				someDataRecordGroup, dataRedactor);

		assertRedactCalledWithCorrectArguments();
		assertAswerFromRedactorIsReturned(recordToEnhance);
	}

	private void assertRedactCalledWithCorrectArguments() {
		Set<?> usersReadRecordPartPermissions = (Set<?>) oldAuthorizator.MCR.getReturnValue(
				"checkGetUsersMatchedRecordPartPermissionsForActionOnRecordTypeAndCollectedData",
				0);

		Set<?> recordPartConstraints = (Set<?>) recordTypeHandlerOldSpy.MCR
				.getReturnValue("getReadRecordPartConstraints", 0);

		dataRedactor.MCR.assertParameters("removeChildrenForConstraintsWithoutPermissions", 0,
				recordTypeHandlerOldSpy.getDefinitionId(), someDataRecordGroup,
				recordPartConstraints, usersReadRecordPartPermissions);
	}

	private void assertAswerFromRedactorIsReturned(DataRecord recordToEnhance) {
		DataRecordGroup redactedDataGroup = (DataRecordGroup) dataRedactor.MCR
				.getReturnValue("removeChildrenForConstraintsWithoutPermissions", 0);
		var redactedRecord = dataFactorySpy.MCR.assertCalledParametersReturn(
				"factorRecordUsingDataRecordGroup", redactedDataGroup);
		assertSame(recordToEnhance, redactedRecord);
	}

	@Test
	public void testEnhanceIgnoreReadAccessRedactData() {
		createRecordStorageSpy();

		DataRecordSpy recordToEnhance = (DataRecordSpy) enhancer.enhanceIgnoringReadAccess(user,
				SOME_RECORD_TYPE, someDataRecordGroup, dataRedactor);

		assertRedactCalledWithCorrectArguments();
		assertAswerFromRedactorIsReturned(recordToEnhance);
	}

	@Test
	public void testEnhanceIgnoreReadAccessRedactDataIsCalledCorrectlyWhenNoAccess() {
		setupForNoReadAccess();

		DataRecordSpy recordToEnhance = (DataRecordSpy) enhancer.enhanceIgnoringReadAccess(user,
				SOME_RECORD_TYPE, someDataRecordGroup, dataRedactor);

		Set<?> recordPartConstraints = (Set<?>) recordTypeHandlerOldSpy.MCR
				.getReturnValue("getReadRecordPartConstraints", 0);

		dataRedactor.MCR.assertParameters("removeChildrenForConstraintsWithoutPermissions", 0,
				recordTypeHandlerOldSpy.getDefinitionId(), someDataRecordGroup,
				recordPartConstraints, Collections.emptySet());
		assertAswerFromRedactorIsReturned(recordToEnhance);
	}

	@Test
	public void testLinksAreAddedToRedactedDataGroup() {
		DataRecordLinkSpy linkSpy1 = createRecordLinkSpyUsingId("linkedSearchId1");
		DataRecordLinkSpy linkSpy2 = createRecordLinkSpyUsingId("linkedSearchId2");
		changeToModernSpies();
		setupReturnedDataGroupOnDataRedactorSpy(linkSpy1, linkSpy2);
		linkAuthorizator.MRV.setSpecificReturnValuesSupplier("isAuthorizedToReadRecordLink",
				() -> true, user, linkSpy1);

		enhancer.enhance(user, DATA_WITH_LINKS, someDataRecordGroup, dataRedactor);

		linkAuthorizator.MCR.assertParameters("isAuthorizedToReadRecordLink", 0, user, linkSpy1);
		linkAuthorizator.MCR.assertParameters("isAuthorizedToReadRecordLink", 1, user, linkSpy2);
		assertLinkOnlyHasReadAction(linkSpy1);
		assertLinkHasNoAction(linkSpy2);
	}

	private void assertLinkOnlyHasReadAction(DataRecordLinkSpy linkSpy1) {
		linkSpy1.MCR.assertParameters("addAction", 0, Action.READ);
		linkSpy1.MCR.assertNumberOfCallsToMethod("addAction", 1);
	}

	private void setupReturnedDataGroupOnDataRedactorSpy(DataLink... links) {
		List<DataLink> linkList = Arrays.asList(links);
		DataRecordGroupSpy dataRecordGroup = new DataRecordGroupSpy();
		dataRecordGroup.MRV.setSpecificReturnValuesSupplier("getChildrenOfTypeAndName",
				() -> linkList, DataRecordLink.class, "recordTypeToSearchIn");
		dataRecordGroup.MRV.setDefaultReturnValuesSupplier("getChildren", () -> linkList);

		dataRedactor.MRV.setDefaultReturnValuesSupplier(
				"removeChildrenForConstraintsWithoutPermissions", () -> dataRecordGroup);
		dataRedactor.MRV.setDefaultReturnValuesSupplier(
				"replaceChildrenForConstraintsWithoutPermissions", () -> dataRecordGroup);
	}

	private void assertLinkHasNoAction(DataRecordLinkSpy linkSpy1) {
		linkSpy1.MCR.assertNumberOfCallsToMethod("addAction", 0);
	}

	private void changeToModernSpies() {
		dependencyProviderOldSpy.spiderAuthorizator = authorizator;
		dependencyProviderOldSpy.recordStorage = recordStorage;
		recordStorage.MRV.setDefaultReturnValuesSupplier("read", DataRecordGroupSpy::new);
		enhancer = DataGroupToRecordEnhancerImp.usingDependencyProviderAndLinkAuthorizator(
				dependencyProviderOldSpy, linkAuthorizator);
	}

	@Test
	public void testReadRecordWithDataRecordLinkHasReadActionOneLevelDown() {
		DataRecordLinkSpy linkSpy1 = createRecordLinkSpyUsingId("linkedSearchId1");
		DataRecordLinkSpy linkSpy2 = createRecordLinkSpyUsingId("linkedSearchId2");
		setupLinksOneLevelDownUsingDataRecordGroup(someDataRecordGroup, linkSpy1, linkSpy2);
		changeToModernSpies();
		linkAuthorizator.MRV.setSpecificReturnValuesSupplier("isAuthorizedToReadRecordLink",
				() -> true, user, linkSpy1);
		recordTypeHandlerOldSpy.MRV.setSpecificReturnValuesSupplier("isPublicForRead", () -> true);

		enhancer.enhance(user, SOME_RECORD_TYPE, someDataRecordGroup, dataRedactor);

		linkAuthorizator.MCR.assertParameters("isAuthorizedToReadRecordLink", 0, user, linkSpy1);
		linkAuthorizator.MCR.assertParameters("isAuthorizedToReadRecordLink", 1, user, linkSpy2);
		assertLinkOnlyHasReadAction(linkSpy1);
		assertLinkHasNoAction(linkSpy2);
	}

	private void setupLinksOneLevelDownUsingDataRecordGroup(DataRecordGroupSpy recordGroup,
			DataLink... links) {
		List<DataLink> linkList = Arrays.asList(links);

		DataGroupSpy dataGroup = new DataGroupSpy();
		dataGroup.MRV.setSpecificReturnValuesSupplier("getChildrenOfTypeAndName", () -> linkList,
				DataRecordLink.class, "recordTypeToSearchIn");
		dataGroup.MRV.setDefaultReturnValuesSupplier("getChildren", () -> linkList);

		recordGroup.MRV.setDefaultReturnValuesSupplier("getChildren", () -> List.of(dataGroup));
	}

	private void setupReturnedDataGroupLinksOneLevelDownOnDataRedactorSpy(DataLink... links) {
		List<DataLink> linkList = Arrays.asList(links);

		DataGroupSpy dataGroup = new DataGroupSpy();
		dataGroup.MRV.setSpecificReturnValuesSupplier("getChildrenOfTypeAndName", () -> linkList,
				DataRecordLink.class, "recordTypeToSearchIn");
		dataGroup.MRV.setDefaultReturnValuesSupplier("getChildren", () -> linkList);

		DataRecordGroupSpy recordGroup = new DataRecordGroupSpy();
		recordGroup.MRV.setDefaultReturnValuesSupplier("getChildren", () -> List.of(dataGroup));

		dataRedactor.MRV.setDefaultReturnValuesSupplier(
				"removeChildrenForConstraintsWithoutPermissions", () -> recordGroup);
		dataRedactor.MRV.setDefaultReturnValuesSupplier(
				"replaceChildrenForConstraintsWithoutPermissions", () -> recordGroup);
	}

	@Test
	public void testReadRecordWithDataResourceLinkHasReadActionTopLevel() {
		DataResourceLinkSpy linkSpy1 = createResoureLinkSpyUsingId("linkedSearchId1");
		DataResourceLinkSpy linkSpy2 = createResoureLinkSpyUsingId("linkedSearchId2");
		setupReturnedDataGroupOnDataRedactorSpy(linkSpy1, linkSpy2);
		changeToModernSpies();

		enhancer.enhance(user, SOME_RECORD_TYPE, someDataRecordGroup, dataRedactor);

		assertResourceLinkOnlyHasReadAction(linkSpy1);
		assertResourceLinkOnlyHasReadAction(linkSpy2);

		var collectTerms = (CollectTerms) termCollector.MCR.getReturnValue("collectTerms", 0);
		authorizator.MCR.assertNumberOfCallsToMethod(
				"userIsAuthorizedForActionOnRecordTypeAndCollectedData", 2);
		authorizator.MCR.assertParameters(
				"checkGetUsersMatchedRecordPartPermissionsForActionOnRecordTypeAndCollectedData", 0,
				user, "read", SOME_RECORD_TYPE, collectTerms.permissionTerms);
		authorizator.MCR.assertParameters(
				"checkGetUsersMatchedRecordPartPermissionsForActionOnRecordTypeAndCollectedData", 1,
				user, "update", SOME_RECORD_TYPE, collectTerms.permissionTerms);

		authorizator.MCR.assertParameters("userIsAuthorizedForActionOnRecordTypeAndCollectedData",
				0, user, "index", SOME_RECORD_TYPE, collectTerms.permissionTerms);
		authorizator.MCR.assertParameters("userIsAuthorizedForActionOnRecordTypeAndCollectedData",
				1, user, "delete", SOME_RECORD_TYPE, collectTerms.permissionTerms);
	}

	private DataResourceLinkSpy createResoureLinkSpyUsingId(String linkedRecordId) {
		DataResourceLinkSpy linkSpy = new DataResourceLinkSpy();
		linkSpy.MRV.setDefaultReturnValuesSupplier("getNameInData", () -> linkedRecordId);
		linkSpy.MRV.setDefaultReturnValuesSupplier("getLinkedRecordType",
				() -> "someResourceLinkType");
		linkSpy.MRV.setDefaultReturnValuesSupplier("getLinkedRecordId", () -> linkedRecordId);
		return linkSpy;
	}

	private void assertResourceLinkOnlyHasReadAction(DataResourceLinkSpy linkSpy1) {
		linkSpy1.MCR.assertParameters("addAction", 0, Action.READ);
		linkSpy1.MCR.assertNumberOfCallsToMethod("addAction", 1);
	}

	@Test
	public void testEnhanceIgnoreReadAccessReadRecordWithDataResourceLinkHasReadActionTopLevel() {
		DataResourceLinkSpy linkSpy1 = createResoureLinkSpyUsingId("linkedSearchId1");
		DataResourceLinkSpy linkSpy2 = createResoureLinkSpyUsingId("linkedSearchId2");
		setupReturnedDataGroupOnDataRedactorSpy(linkSpy1, linkSpy2);
		changeToModernSpies();

		enhancer.enhanceIgnoringReadAccess(user, SOME_RECORD_TYPE, someDataRecordGroup,
				dataRedactor);

		assertResourceLinkOnlyHasReadAction(linkSpy1);
		assertResourceLinkOnlyHasReadAction(linkSpy2);
	}

	@Test
	public void testReadRecordWithDataResourceLinkHasReadActionOneLevelDown() {
		DataResourceLinkSpy linkSpy1 = createResoureLinkSpyUsingId("linkedSearchId1");
		DataResourceLinkSpy linkSpy2 = createResoureLinkSpyUsingId("linkedSearchId2");
		setupReturnedDataGroupLinksOneLevelDownOnDataRedactorSpy(linkSpy1, linkSpy2);
		changeToModernSpies();

		enhancer.enhance(user, SOME_RECORD_TYPE, someDataRecordGroup, dataRedactor);

		assertResourceLinkOnlyHasReadAction(linkSpy1);
		assertResourceLinkOnlyHasReadAction(linkSpy2);
	}

	@Test
	public void testEnhanceIgnoreReadAccessReadRecordWithDataResourceLinkHasReadActionOneLevelDown() {
		DataResourceLinkSpy linkSpy1 = createResoureLinkSpyUsingId("linkedSearchId1");
		DataResourceLinkSpy linkSpy2 = createResoureLinkSpyUsingId("linkedSearchId2");
		setupReturnedDataGroupLinksOneLevelDownOnDataRedactorSpy(linkSpy1, linkSpy2);
		changeToModernSpies();

		enhancer.enhanceIgnoringReadAccess(user, SOME_RECORD_TYPE, someDataRecordGroup,
				dataRedactor);

		assertResourceLinkOnlyHasReadAction(linkSpy1);
		assertResourceLinkOnlyHasReadAction(linkSpy2);
	}

	@Test
	public void testIfPermissionUnitsNotUsedInRecordTypeDoNotCheckUserAuthorization() {
		changeToModernSpies();

		enhancer.enhance(user, DATA_WITH_LINKS, someDataRecordGroup, dataRedactor);

		recordTypeHandlerOldSpy.MCR.assertMethodWasCalled("usePermissionUnit");
		authorizator.MCR.assertMethodNotCalled("checkUserIsAuthorizedForPemissionUnit");
	}

	@Test(expectedExceptions = DataException.class, expectedExceptionsMessageRegExp = ""
			+ "PermissionUnit is missing in the record.")
	public void testIfPermissionUnitsUsedInRecordType_NoPermissionUnitInData_DoNotCheckUserAuthorization() {
		changeToModernSpies();
		setupRecordTypeToUsePermissionUnit(true);
		setupRecordTypeToUseVisibility(true);
		setupSomeDataRecordGroupToHaveVisibilityUnpublished();
		someDataRecordGroup.MRV.setDefaultReturnValuesSupplier("getPermissionUnit",
				Optional::empty);

		enhancer.enhance(user, DATA_WITH_LINKS, someDataRecordGroup, dataRedactor);
	}

	@Test
	public void testCallCheckUserIsAuthorizedForPermissionUnits_notCalledForOnlyPermissionUnit() {
		changeToModernSpies();
		setupRecordTypeToUsePermissionUnit(false);
		setupSomeDataRecordGroupToHaveSomePermissionUnit();

		enhancer.enhance(user, DATA_WITH_LINKS, someDataRecordGroup, dataRedactor);

		recordTypeHandlerOldSpy.MCR.assertMethodWasCalled("usePermissionUnit");
		authorizator.MCR.assertMethodNotCalled("checkUserIsAuthorizedForPemissionUnit");
	}

	@Test
	public void testCallHostRecords_DoNotUseHostRecords() {
		changeToModernSpies();
		recordTypeHandlerOldSpy.MRV.setDefaultReturnValuesSupplier("useHostRecord", () -> false);

		enhancer.enhance(user, DATA_WITH_LINKS, someDataRecordGroup, dataRedactor);

		recordTypeHandlerOldSpy.MCR.assertMethodWasCalled("useHostRecord");
		someDataRecordGroup.MCR.assertMethodNotCalled("getHostRecord");

	}

	@Test(expectedExceptions = DataException.class, expectedExceptionsMessageRegExp = ""
			+ "HostRecord is missing in the record, for record with type: binary and id: someId.")
	public void test_UsesHostRecords_HostRecordEmpty() {
		changeToModernSpies();
		setupRecordTypeToIsPublicForRead(false);
		setupRecordTypeToUseHostRecord(true);

		enhancer.enhance(user, BINARY_RECORD_TYPE, someDataRecordGroup, dataRedactor);
	}

	@Test
	public void testCallHostRecords_UsesHostRecords_NoPublic_NoPermissionUnits() {
		changeToModernSpies();
		setupRecordTypeToIsPublicForRead(false);
		setupRecordTypeToUseHostRecord(true);

		DataRecordGroupSpy hostRecordGroup;
		hostRecordGroup = setRecordHostInStorage();
		createAndAddHostRecordLinkToRecordGroup(someDataRecordGroup);

		enhancer.enhance(user, BINARY_RECORD_TYPE, someDataRecordGroup, dataRedactor);

		recordTypeHandlerOldSpy.MCR.assertMethodWasCalled("useHostRecord");
		someDataRecordGroup.MCR.assertMethodWasCalled("getHostRecord");

		var recordTypeHandlerHostRecord = (RecordTypeHandlerOldSpy) dependencyProviderOldSpy.MCR
				.assertCalledParametersReturn("getRecordTypeHandler", "hostLinkedType");
		var hostRecordDefinitionId = (String) recordTypeHandlerHostRecord.MCR
				.assertCalledParametersReturn("getDefinitionId");

		CollectTerms collectTerms = (CollectTerms) termCollector.MCR.assertCalledParametersReturn(
				"collectTerms", hostRecordDefinitionId, hostRecordGroup);

		authorizator.MCR.assertParameters(
				"checkGetUsersMatchedRecordPartPermissionsForActionOnRecordTypeAndCollectedData", 0,
				user, "read", "hostLinkedType." + BINARY_RECORD_TYPE, collectTerms.permissionTerms,
				true);
		authorizator.MCR.assertParameters(
				"checkGetUsersMatchedRecordPartPermissionsForActionOnRecordTypeAndCollectedData", 1,
				user, "update", "hostLinkedType." + BINARY_RECORD_TYPE,
				collectTerms.permissionTerms, true);
		authorizator.MCR.assertParameters("userIsAuthorizedForActionOnRecordTypeAndCollectedData",
				0, user, "index", "hostLinkedType." + BINARY_RECORD_TYPE,
				collectTerms.permissionTerms);
		authorizator.MCR.assertParameters("userIsAuthorizedForActionOnRecordTypeAndCollectedData",
				1, user, "delete", "hostLinkedType." + BINARY_RECORD_TYPE,
				collectTerms.permissionTerms);
		authorizator.MCR.assertParameters("userIsAuthorizedForActionOnRecordTypeAndCollectedData",
				2, user, "upload", "hostLinkedType." + BINARY_RECORD_TYPE,
				collectTerms.permissionTerms);
	}

	@Test
	public void testCallHostRecords_UsesHostRecords_NoPublic_PermissionUnits() {
		changeToModernSpies();
		setupRecordTypeToIsPublicForRead(false);
		setupRecordTypeToUseHostRecord(true);
		setupRecordTypeToUsePermissionUnit(true);
		setupRecordTypeToUseVisibility(true);

		setupSomeDataRecordGroupToHaveVisibilityUnpublished();
		DataRecordGroupSpy hostRecordGroup = setRecordHostInStorage();
		hostRecordGroup.MRV.setDefaultReturnValuesSupplier("getPermissionUnit",
				() -> Optional.of(SOME_PERMISSION_UNIT));
		createAndAddHostRecordLinkToRecordGroup(someDataRecordGroup);

		enhancer.enhance(user, BINARY_RECORD_TYPE, someDataRecordGroup, dataRedactor);

		hostRecordGroup.MCR.assertMethodWasCalled("getPermissionUnit");
	}

	private void setupRecordTypeToIsPublicForRead(boolean value) {
		recordTypeHandlerOldSpy.MRV.setDefaultReturnValuesSupplier("isPublicForRead", () -> value);
	}

	private void setupRecordTypeToUseHostRecord(boolean value) {
		recordTypeHandlerOldSpy.MRV.setDefaultReturnValuesSupplier("useHostRecord", () -> value);
	}

	private void createAndAddHostRecordLinkToRecordGroup(DataRecordGroupSpy dataRecordGroup) {
		DataRecordLinkSpy hostRecordLink = new DataRecordLinkSpy();
		hostRecordLink.MRV.setDefaultReturnValuesSupplier("getLinkedRecordType",
				() -> "hostLinkedType");
		hostRecordLink.MRV.setDefaultReturnValuesSupplier("getLinkedRecordId",
				() -> "hostLinkedId");
		dataRecordGroup.MRV.setDefaultReturnValuesSupplier("getHostRecord",
				() -> Optional.of(hostRecordLink));
	}

	private DataRecordGroupSpy setRecordHostInStorage() {
		DataRecordGroupSpy hostRecordGroup;
		hostRecordGroup = new DataRecordGroupSpy();
		hostRecordGroup.MRV.setDefaultReturnValuesSupplier("getType", () -> "hostLinkedType");
		recordStorage.MRV.setSpecificReturnValuesSupplier("read", () -> hostRecordGroup,
				"hostLinkedType", "hostLinkedId");
		return hostRecordGroup;
	}

	@Test
	public void testCallCheckUserIsAuthorizedForPermissionUnits_IfPermissionUnitsAndVisibilityAreUsedInRecordType() {
		changeToModernSpies();
		setupRecordTypeToUsePermissionUnit(true);
		setupRecordTypeToUseVisibility(true);
		setupSomeDataRecordGroupToHaveVisibilityUnpublished();
		setupSomeDataRecordGroupToHaveSomePermissionUnit();

		enhancer.enhance(user, DATA_WITH_LINKS, someDataRecordGroup, dataRedactor);

		recordTypeHandlerOldSpy.MCR.assertMethodWasCalled("usePermissionUnit");
		authorizator.MCR.assertParameters("checkUserIsAuthorizedForPemissionUnit", 0, user,
				SOME_PERMISSION_UNIT);
	}

	@Test(expectedExceptions = DataException.class, expectedExceptionsMessageRegExp = ""
			+ "Visibility is missing in the record.")
	public void testTypesUsesVisibilityButRecordHasEmptyVisibility() {
		changeToModernSpies();
		setupRecordTypeToUseVisibility(true);
		someDataRecordGroup.MRV.setDefaultReturnValuesSupplier("getVisibility", Optional::empty);

		enhancer.enhance(user, DATA_WITH_LINKS, someDataRecordGroup, dataRedactor);
	}

	private void setupRecordTypeToUseVisibility(boolean value) {
		recordTypeHandlerOldSpy.MRV.setDefaultReturnValuesSupplier("useVisibility", () -> value);
	}

	private void setupSomeDataRecordGroupToHaveVisibilityUnpublished() {
		someDataRecordGroup.MRV.setDefaultReturnValuesSupplier("getVisibility",
				() -> Optional.of("unpublished"));
	}

	private void setupSomeDataRecordGroupToHaveVisibilityPublished() {
		someDataRecordGroup.MRV.setDefaultReturnValuesSupplier("getVisibility",
				() -> Optional.of("published"));
	}

	@Test
	public void testTypesUsesVisibilityButRecordNotPublished() {
		changeToModernSpies();
		setupRecordTypeToUseVisibility(true);
		setupSomeDataRecordGroupToHaveVisibilityUnpublished();

		DataRecordSpy enhancedRecord = (DataRecordSpy) enhancer.enhance(user, DATA_WITH_LINKS,
				someDataRecordGroup, dataRedactor);

		recordTypeHandlerOldSpy.MCR.assertParameters("useVisibility", 0);
		someDataRecordGroup.MCR.assertParameters("getVisibility", 0);
		assertUsePermissionUnitsCalledFromGetRecordPartsAndAddActions();
		assertCheckAuthorized(enhancedRecord);
	}

	private void assertUsePermissionUnitsCalledFromGetRecordPartsAndAddActions() {
		recordTypeHandlerOldSpy.MCR.assertNumberOfCallsToMethod("usePermissionUnit", 2);
		recordTypeHandlerOldSpy.MCR.assertMethodWasCalled("usePermissionUnit");
	}

	@Test
	public void testTypesUsesVisibilityAndRecordIsPublished() {
		changeToModernSpies();
		setupRecordTypeToUseVisibility(true);
		setupSomeDataRecordGroupToHaveVisibilityPublished();

		DataRecordSpy enhancedRecord = (DataRecordSpy) enhancer.enhance(user, DATA_WITH_LINKS,
				someDataRecordGroup, dataRedactor);

		recordTypeHandlerOldSpy.MCR.assertParameters("useVisibility", 0);
		someDataRecordGroup.MCR.assertParameters("getVisibility", 0);
		assertCheckAuthorized(enhancedRecord);
		assertUsePermissionUnitOnlyCalledFromAddActions();
	}

	private void assertUsePermissionUnitOnlyCalledFromAddActions() {
		recordTypeHandlerOldSpy.MCR.assertNumberOfCallsToMethod("usePermissionUnit", 1);
	}

	@Test
	public void testTypesUsesVisibilityAndRecordIsPublishedButUserNotAuthorized() {
		changeToModernSpies();
		setupRecordTypeToUseVisibility(true);
		setupSomeDataRecordGroupToHaveVisibilityPublished();

		authorizator.MRV.setAlwaysThrowException(
				"checkGetUsersMatchedRecordPartPermissionsForActionOnRecordTypeAndCollectedData",
				new RuntimeException("someException"));

		DataRecordSpy enhancedRecord = (DataRecordSpy) enhancer.enhance(user, DATA_WITH_LINKS,
				someDataRecordGroup, dataRedactor);

		recordTypeHandlerOldSpy.MCR.assertParameters("useVisibility", 0);
		someDataRecordGroup.MCR.assertParameters("getVisibility", 0);

		assertCheckAuthorizedWhenRecordIsPublishedButNotAuthorized(enhancedRecord);
		assertUsePermissionUnitOnlyCalledFromAddActions();
	}

	private void assertCheckAuthorizedWhenRecordIsPublishedButNotAuthorized(
			DataRecordSpy enhancedRecord) {
		var permissionTerms = getAssertedCollectedPermissionTermsForRecordType(DATA_WITH_LINKS,
				someDataRecordGroup);
		authorizator.MCR.assertCalledParameters(
				"checkGetUsersMatchedRecordPartPermissionsForActionOnRecordTypeAndCollectedData",
				user, READ, DATA_WITH_LINKS, permissionTerms, true);
		enhancedRecord.MCR.assertParameters("addReadPermissions", 0, Collections.emptySet());
	}

	private void assertCheckAuthorized(DataRecordSpy enhancedRecord) {
		var permissionTerms = getAssertedCollectedPermissionTermsForRecordType(DATA_WITH_LINKS,
				someDataRecordGroup);
		var readRecordPartPermissions = authorizator.MCR.assertCalledParametersReturn(
				"checkGetUsersMatchedRecordPartPermissionsForActionOnRecordTypeAndCollectedData",
				user, READ, DATA_WITH_LINKS, permissionTerms, true);
		enhancedRecord.MCR.assertParameters("addReadPermissions", 0, readRecordPartPermissions);
	}

	@Test
	public void testEnhanceIgnoringReadAccess_IfPermissionUnitsNotUsedInRecordTypeDoNotCheckUserAuthorization() {
		changeToModernSpies();
		var onlyForTestEnhancer = new OnlyForTestDataGroupToRecordEnhancerImp(
				dependencyProviderOldSpy, linkAuthorizator);

		var enhancedRecord = (DataRecordSpy) onlyForTestEnhancer.enhanceIgnoringReadAccess(user,
				DATA_WITH_LINKS, someDataRecordGroup, dataRedactor);

		onlyForTestEnhancer.MCR.assertParameters(
				"ensureReadAccessAndReturnReadRecordPartPermission", 0, someDataRecordGroup);
		var readRecordPartPermissions = onlyForTestEnhancer.MCR
				.getReturnValue("ensureReadAccessAndReturnReadRecordPartPermission", 0);
		enhancedRecord.MCR.assertParameters("addReadPermissions", 0, readRecordPartPermissions);
	}

	class OnlyForTestDataGroupToRecordEnhancerImp extends DataGroupToRecordEnhancerImp {
		public MethodCallRecorder MCR = new MethodCallRecorder();
		public MethodReturnValues MRV = new MethodReturnValues();

		public OnlyForTestDataGroupToRecordEnhancerImp(SpiderDependencyProvider dependencyProvider,
				LinkAuthorizator linkAuthorizator) {
			super(dependencyProvider, linkAuthorizator);
			MCR.useMRV(MRV);
			MRV.setDefaultReturnValuesSupplier("ensureReadAccessAndReturnReadRecordPartPermission",
					Collections::emptySet);
		}

		@SuppressWarnings("unchecked")
		@Override
		Set<String> ensureReadAccessAndReturnReadRecordPartPermission(
				DataRecordGroup dataRecordGroup) {
			return (Set<String>) MCR.addCallAndReturnFromMRV("dataRecordGroup", dataRecordGroup);
		}
	}

	@Test
	public void testOnlyForTests() {
		var enhancerImp = (DataGroupToRecordEnhancerImp) enhancer;
		assertEquals(enhancerImp.onlyForTestGetDependencyProvider(), dependencyProviderOldSpy);
		assertEquals(enhancerImp.onlyForTestGetLinkAuthorizator(), linkAuthorizator);

	}
}
