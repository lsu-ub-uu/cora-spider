/*
 * Copyright 2016, 2017, 2019, 2020 Uppsala University Library
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
import se.uu.ub.cora.spider.authentication.OldAuthenticatorSpy;
import se.uu.ub.cora.spider.authorization.AuthorizationException;
import se.uu.ub.cora.spider.authorization.PermissionRuleCalculator;
import se.uu.ub.cora.spider.dependency.spy.RecordTypeHandlerOldSpy;
import se.uu.ub.cora.spider.dependency.spy.SpiderDependencyProviderOldSpy;
import se.uu.ub.cora.spider.log.LoggerFactorySpy;
import se.uu.ub.cora.spider.record.DataGroupToRecordEnhancer;
import se.uu.ub.cora.spider.record.RecordEnhancerTestsRecordStorage;
import se.uu.ub.cora.spider.spy.DataGroupTermCollectorSpy;
import se.uu.ub.cora.spider.spy.OldSpiderAuthorizatorSpy;
import se.uu.ub.cora.spider.spy.RuleCalculatorSpy;
import se.uu.ub.cora.storage.RecordNotFoundException;
import se.uu.ub.cora.storage.spies.RecordStorageSpy;

public class DataGroupToRecordEnhancerTest {
	private static final String UPDATE = "update";
	private static final String LIST = "list";
	private static final String DATA_WITH_LINKS = "dataWithLinks";
	private static final String CREATE = "create";
	private static final String SEARCH = "search";
	private static final String READ = "read";
	private static final String SOME_RECORD_TYPE = "someRecordType";
	private static final String BINARY_RECORD_TYPE = "binary";
	private RecordEnhancerTestsRecordStorage oldRecordStorage;
	private OldAuthenticatorSpy authenticator;
	private OldSpiderAuthorizatorSpy oldAuthorizator;
	private SpiderAuthorizatorSpy authorizator;
	private PermissionRuleCalculator ruleCalculator;
	private SpiderDependencyProviderOldSpy dependencyProvider;
	private User user;
	private DataGroupToRecordEnhancer enhancer;
	private DataGroupTermCollectorSpy termCollector;
	private LoggerFactorySpy loggerFactorySpy;
	private DataFactorySpy dataFactorySpy;
	private RecordTypeHandlerOldSpy recordTypeHandlerSpy;
	private DataRedactorSpy dataRedactor;

	private DataRecordGroupSpy someDataRecordGroup;
	private RecordStorageSpy recordStorage;

	@BeforeMethod
	public void setUp() {
		setUpFactoriesAndProviders();
		someDataRecordGroup = new DataRecordGroupSpy();
		someDataRecordGroup.MRV.setDefaultReturnValuesSupplier("getId", () -> "someId");
		user = new User("987654321");
		recordStorage = new RecordStorageSpy();
		oldRecordStorage = new RecordEnhancerTestsRecordStorage();
		authenticator = new OldAuthenticatorSpy();
		oldAuthorizator = new OldSpiderAuthorizatorSpy();
		authorizator = new SpiderAuthorizatorSpy();
		ruleCalculator = new RuleCalculatorSpy();
		termCollector = new DataGroupTermCollectorSpy();
		dataRedactor = new DataRedactorSpy();
		setUpDependencyProvider();
	}

	private void setUpFactoriesAndProviders() {
		loggerFactorySpy = new LoggerFactorySpy();
		LoggerProvider.setLoggerFactory(loggerFactorySpy);
		dataFactorySpy = new DataFactorySpy();
		DataProvider.onlyForTestSetDataFactory(dataFactorySpy);
	}

	private void setUpDependencyProvider() {
		dependencyProvider = new SpiderDependencyProviderOldSpy();
		dependencyProvider.authenticator = authenticator;
		dependencyProvider.spiderAuthorizator = oldAuthorizator;
		dependencyProvider.recordStorage = oldRecordStorage;
		dependencyProvider.ruleCalculator = ruleCalculator;
		dependencyProvider.termCollector = termCollector;
		recordTypeHandlerSpy = dependencyProvider.recordTypeHandlerSpy;
		enhancer = new DataGroupToRecordEnhancerImp(dependencyProvider);
	}

	@Test
	public void testReadActionPartOfEnhance() throws Exception {
		createRecordStorageSpy();

		DataRecordSpy record = (DataRecordSpy) enhancer.enhance(user, SOME_RECORD_TYPE,
				someDataRecordGroup, dataRedactor);

		assertReadActionPartOfEnhance(record);
	}

	private void assertReadActionPartOfEnhance(DataRecordSpy record) {
		assertCheckAndGetAuthorizationCalledForReadActionPartOfEnhance();
		assertRecordContainsReadAction(record);
	}

	private void assertCheckAndGetAuthorizationCalledForReadActionPartOfEnhance() {
		var permissionTerms = getAssertedCollectedPermissionTermsForRecordType(SOME_RECORD_TYPE,
				someDataRecordGroup);
		oldAuthorizator.MCR.assertParameters(
				"checkGetUsersMatchedRecordPartPermissionsForActionOnRecordTypeAndCollectedData", 0,
				user, READ, SOME_RECORD_TYPE, permissionTerms, true);
	}

	private void assertRecordContainsReadAction(DataRecordSpy record) {
		record.MCR.assertParameters("addAction", 0, Action.READ);
	}

	private RecordStorageOldSpy createRecordStorageSpy() {
		RecordStorageOldSpy recordStorageSpy = new RecordStorageOldSpy();
		dependencyProvider.recordStorage = recordStorageSpy;
		enhancer = new DataGroupToRecordEnhancerImp(dependencyProvider);
		return recordStorageSpy;
	}

	private List<PermissionTerm> getAssertedCollectedPermissionTermsForRecordType(String recordType,
			DataRecordGroup dataRecordGroup) {
		dependencyProvider.MCR.assertParameters("getRecordTypeHandler", 0, recordType);
		RecordTypeHandlerOldSpy recordTypeHandler = (RecordTypeHandlerOldSpy) dependencyProvider.MCR
				.getReturnValue("getRecordTypeHandler", 0);

		recordTypeHandler.MCR.assertMethodWasCalled("getDefinitionId");
		String metadataIdFromRecordTypeHandler = (String) recordTypeHandler.MCR
				.getReturnValue("getDefinitionId", 0);

		dependencyProvider.MCR.assertParameters("getDataGroupTermCollector", 0);
		DataGroupTermCollectorSpy termCollectorSpy = (DataGroupTermCollectorSpy) dependencyProvider.MCR
				.getReturnValue("getDataGroupTermCollector", 0);

		termCollectorSpy.MCR.assertParameters("collectTerms", 0, metadataIdFromRecordTypeHandler,
				dataRecordGroup);
		return ((CollectTerms) termCollectorSpy.MCR.getReturnValue("collectTerms",
				0)).permissionTerms;
	}

	@Test
	public void testEnhanceIgnoreReadAccessReadActionPartOfEnhance() throws Exception {
		createRecordStorageSpy();

		DataRecordSpy record = (DataRecordSpy) enhancer.enhanceIgnoringReadAccess(user,
				SOME_RECORD_TYPE, someDataRecordGroup, dataRedactor);

		assertReadActionPartOfEnhance(record);
	}

	@Test
	public void testReadActionPartOfEnhanceNotAuthorized() throws Exception {
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
	public void testEnhanceIgnoreReadAccessReadActionPartOfEnhanceNotAuthorized() throws Exception {
		setupForNoReadAccess();

		DataRecordSpy record = (DataRecordSpy) enhancer.enhanceIgnoringReadAccess(user,
				SOME_RECORD_TYPE, someDataRecordGroup, dataRedactor);

		assertCheckAndGetAuthorizationCalledForReadActionPartOfEnhance();

		assertRecordDoesNotContainReadAction(record);
	}

	private void assertRecordDoesNotContainReadAction(DataRecord record) {
		assertFalse(record.getActions().contains(Action.READ));
	}

	@Test
	public void testReadActionPartOfEnhanceNotAuthorizedButPublicData() throws Exception {
		setupForNoReadAccessButPublicData();

		DataRecordSpy record = (DataRecordSpy) enhancer.enhance(user, SOME_RECORD_TYPE,
				someDataRecordGroup, dataRedactor);

		assertReadActionPartOfEnhanceNotAuthorizedButPublicData(record);
	}

	private void setupForNoReadAccessButPublicData() {
		createRecordStorageSpy();
		recordTypeHandlerSpy.isPublicForRead = true;
		oldAuthorizator.authorizedForActionAndRecordTypeAndCollectedData = false;
	}

	private void assertReadActionPartOfEnhanceNotAuthorizedButPublicData(DataRecordSpy record) {
		recordTypeHandlerSpy.MCR.assertMethodWasCalled("isPublicForRead");
		oldAuthorizator.MCR.assertNumberOfCallsToMethod(
				"checkGetUsersMatchedRecordPartPermissionsForActionOnRecordTypeAndCollectedData",
				1);
		assertRecordContainsReadAction(record);
	}

	@Test
	public void testEnhanceIgnoreReadAccessReadActionPartOfEnhanceNotAuthorizedButPublicData()
			throws Exception {
		setupForNoReadAccessButPublicData();

		DataRecordSpy record = (DataRecordSpy) enhancer.enhanceIgnoringReadAccess(user,
				SOME_RECORD_TYPE, someDataRecordGroup, dataRedactor);

		assertReadActionPartOfEnhanceNotAuthorizedButPublicData(record);
	}

	@Test
	public void testUpdateActionPartOfEnhance() throws Exception {
		createRecordStorageSpy();

		DataRecordSpy record = (DataRecordSpy) enhancer.enhance(user, SOME_RECORD_TYPE,
				someDataRecordGroup, dataRedactor);

		assertUpdateActionPartOfEnhance(record);
	}

	private void assertUpdateActionPartOfEnhance(DataRecordSpy record) {
		assertCheckAndGetAuthorizationCalledForUpdateActionPartOfEnhance();
		assertRecordContainsUpdateAction(record);
	}

	private void assertRecordContainsUpdateAction(DataRecordSpy record) {
		record.MCR.assertCalledParameters("addAction", Action.UPDATE);
	}

	private void assertCheckAndGetAuthorizationCalledForUpdateActionPartOfEnhance() {
		var permissionTerms = getAssertedCollectedPermissionTermsForRecordType(SOME_RECORD_TYPE,
				someDataRecordGroup);
		oldAuthorizator.MCR.assertParameters(
				"checkGetUsersMatchedRecordPartPermissionsForActionOnRecordTypeAndCollectedData", 1,
				user, UPDATE, SOME_RECORD_TYPE, permissionTerms, true);
	}

	@Test
	public void testEnhanceIgnoreReadAccessUpdateActionPartOfEnhance() throws Exception {
		createRecordStorageSpy();

		DataRecordSpy record = (DataRecordSpy) enhancer.enhanceIgnoringReadAccess(user,
				SOME_RECORD_TYPE, someDataRecordGroup, dataRedactor);

		assertUpdateActionPartOfEnhance(record);
	}

	@Test
	public void testUpdateActionPartOfEnhanceNotAuthorized() throws Exception {
		setupForNoUpdateAccess();

		DataRecordSpy record = (DataRecordSpy) enhancer.enhance(user, SOME_RECORD_TYPE,
				someDataRecordGroup, dataRedactor);

		assertUpdateActionPartOfEnhanceNotAuthorized(record);
	}

	private void setupForNoUpdateAccess() {
		createRecordStorageSpy();
		oldAuthorizator.setNotAutorizedForAction(UPDATE);
	}

	private void assertUpdateActionPartOfEnhanceNotAuthorized(DataRecord record) {
		assertCheckAndGetAuthorizationCalledForUpdateActionPartOfEnhance();
		assertFalse(record.getActions().contains(Action.UPDATE));
	}

	@Test
	public void testEnhanceIgnoreReadAccessUpdateActionPartOfEnhanceNotAuthorized()
			throws Exception {
		setupForNoUpdateAccess();

		DataRecordSpy record = (DataRecordSpy) enhancer.enhanceIgnoringReadAccess(user,
				SOME_RECORD_TYPE, someDataRecordGroup, dataRedactor);

		assertUpdateActionPartOfEnhanceNotAuthorized(record);
	}

	@Test
	public void testIndexActionPartOfEnhance() throws Exception {
		createRecordStorageSpy();

		DataRecordSpy record = (DataRecordSpy) enhancer.enhance(user, SOME_RECORD_TYPE,
				someDataRecordGroup, dataRedactor);

		assertIndexAction(record);
	}

	private void assertIndexAction(DataRecordSpy record) {
		assertTypeAndCollectedDataAuthorizationCalledForIndexActionPartOfEnhance();
		assertRecordContainsIndexAction(record);
	}

	private void assertTypeAndCollectedDataAuthorizationCalledForIndexActionPartOfEnhance() {
		var permissionTerms = getAssertedCollectedPermissionTermsForRecordType(SOME_RECORD_TYPE,
				someDataRecordGroup);
		oldAuthorizator.MCR.assertParameters(
				"userIsAuthorizedForActionOnRecordTypeAndCollectedData", 0, user, "index",
				SOME_RECORD_TYPE, permissionTerms);
	}

	private void assertRecordContainsIndexAction(DataRecordSpy record) {
		record.MCR.assertCalledParameters("addAction", Action.INDEX);
	}

	@Test
	public void testBatchIndexActionPartOfEnhance() throws Exception {
		createRecordStorageSpy();
		recordTypeHandlerSpy.representsTheRecordTypeDefiningRecordTypes = true;

		DataRecordSpy record = (DataRecordSpy) enhancer.enhance(user, SOME_RECORD_TYPE,
				someDataRecordGroup, dataRedactor);

		assertBatchIndexAction(record);
	}

	private void assertBatchIndexAction(DataRecordSpy record) {
		assertTypeAndCollectedDataAuthorizationCalledForBatchIndexActionPartOfEnhance();
		assertRecordContainsBatchIndexAction(record);
	}

	private void assertTypeAndCollectedDataAuthorizationCalledForBatchIndexActionPartOfEnhance() {
		oldAuthorizator.MCR.assertParameters("userIsAuthorizedForActionOnRecordType", 3, user,
				"batch_index", "someId");
	}

	private void assertRecordContainsBatchIndexAction(DataRecordSpy record) {
		record.MCR.assertCalledParameters("addAction", Action.BATCH_INDEX);
	}

	@Test
	public void testEnhanceIgnoreReadAccessBatchIndexActionPartOfEnhance() throws Exception {
		createRecordStorageSpy();
		recordTypeHandlerSpy.representsTheRecordTypeDefiningRecordTypes = true;

		DataRecordSpy record = (DataRecordSpy) enhancer.enhanceIgnoringReadAccess(user,
				SOME_RECORD_TYPE, someDataRecordGroup, dataRedactor);

		assertBatchIndexAction(record);
	}

	@Test
	public void testBatchIndexActionPartOfEnhanceNotAuthorized() throws Exception {
		setupForNoBatchIndexAccess();

		DataRecordSpy record = (DataRecordSpy) enhancer.enhance(user, SOME_RECORD_TYPE,
				someDataRecordGroup, dataRedactor);

		assertBatchIndexActionNotAuthorized(record);
	}

	private void setupForNoBatchIndexAccess() {
		createRecordStorageSpy();
		recordTypeHandlerSpy.representsTheRecordTypeDefiningRecordTypes = true;
		oldAuthorizator.setNotAutorizedForAction("batch_index");
	}

	private void assertBatchIndexActionNotAuthorized(DataRecord record) {
		assertTypeAndCollectedDataAuthorizationCalledForBatchIndexActionPartOfEnhance();
		assertRecordDoesNotContainBatchIndexAction(record);
	}

	private void assertRecordDoesNotContainBatchIndexAction(DataRecord record) {
		assertFalse(record.getActions().contains(Action.BATCH_INDEX));
	}

	@Test
	public void testEnhanceIgnoreReadAccessIndexActionPartOfEnhance() throws Exception {
		createRecordStorageSpy();

		DataRecordSpy record = (DataRecordSpy) enhancer.enhanceIgnoringReadAccess(user,
				SOME_RECORD_TYPE, someDataRecordGroup, dataRedactor);

		assertIndexAction(record);
	}

	@Test
	public void testIndexActionPartOfEnhanceNotAuthorized() throws Exception {
		setupForNoIndexAccess();

		DataRecordSpy record = (DataRecordSpy) enhancer.enhance(user, SOME_RECORD_TYPE,
				someDataRecordGroup, dataRedactor);

		assertIndexActionNotAuthorized(record);
	}

	private void setupForNoIndexAccess() {
		createRecordStorageSpy();
		oldAuthorizator.setNotAutorizedForAction("index");
	}

	private void assertIndexActionNotAuthorized(DataRecord record) {
		assertTypeAndCollectedDataAuthorizationCalledForIndexActionPartOfEnhance();
		assertRecordDoesNotContainIndexAction(record);
	}

	private void assertRecordDoesNotContainIndexAction(DataRecord record) {
		assertFalse(record.getActions().contains(Action.INDEX));
	}

	@Test
	public void testEnhanceIgnoreReadAccessIndexActionPartOfEnhanceNotAuthorized()
			throws Exception {
		setupForNoIndexAccess();

		DataRecordSpy record = (DataRecordSpy) enhancer.enhanceIgnoringReadAccess(user,
				SOME_RECORD_TYPE, someDataRecordGroup, dataRedactor);

		assertIndexActionNotAuthorized(record);
	}

	@Test
	public void testDeleteActionPartOfEnhance() throws Exception {
		createRecordStorageSpy();

		DataRecordSpy record = (DataRecordSpy) enhancer.enhance(user, SOME_RECORD_TYPE,
				someDataRecordGroup, dataRedactor);

		assertDeleteAction(record);
	}

	private void assertDeleteAction(DataRecordSpy record) {
		assertTypeAndCollectedDataAuthorizationCalledForDeleteActionPartOfEnhance();
		assertRecordContainsDeleteAction(record);
	}

	private void assertRecordContainsDeleteAction(DataRecordSpy record) {
		record.MCR.assertCalledParameters("addAction", Action.DELETE);
	}

	private void assertTypeAndCollectedDataAuthorizationCalledForDeleteActionPartOfEnhance() {
		var permissionTerms = getAssertedCollectedPermissionTermsForRecordType(SOME_RECORD_TYPE,
				someDataRecordGroup);
		oldAuthorizator.MCR.assertParameters(
				"userIsAuthorizedForActionOnRecordTypeAndCollectedData", 1, user, "delete",
				SOME_RECORD_TYPE, permissionTerms);
	}

	@Test
	public void testEnhanceIgnoreReadAccessDeleteActionPartOfEnhance() throws Exception {
		createRecordStorageSpy();

		DataRecordSpy record = (DataRecordSpy) enhancer.enhanceIgnoringReadAccess(user,
				SOME_RECORD_TYPE, someDataRecordGroup, dataRedactor);

		assertDeleteAction(record);
	}

	@Test
	public void testDeleteActionPartOfEnhanceNotAuthorized() throws Exception {
		setupForNoDeleteAccess();

		DataRecordSpy record = (DataRecordSpy) enhancer.enhance(user, SOME_RECORD_TYPE,
				someDataRecordGroup, dataRedactor);

		assertDeleteActionNotAuthorized(record);
	}

	private void setupForNoDeleteAccess() {
		createRecordStorageSpy();
		oldAuthorizator.setNotAutorizedForAction("delete");
	}

	private void assertDeleteActionNotAuthorized(DataRecord record) {
		assertTypeAndCollectedDataAuthorizationCalledForDeleteActionPartOfEnhance();
		assertRecordDoesNotContainDeleteAction(record);
	}

	private void assertRecordDoesNotContainDeleteAction(DataRecord record) {
		assertFalse(record.getActions().contains(Action.DELETE));
	}

	@Test
	public void testEnhanceIgnoreReadAccessDeleteActionPartOfEnhanceNotAuthorized()
			throws Exception {
		setupForNoDeleteAccess();

		DataRecordSpy record = (DataRecordSpy) enhancer.enhanceIgnoringReadAccess(user,
				SOME_RECORD_TYPE, someDataRecordGroup, dataRedactor);

		assertDeleteActionNotAuthorized(record);
	}

	@Test
	public void testDeleteActionPartOfEnhanceAuthorizedButHasIncomingLinks() throws Exception {
		setupForDeleteButIncomingLinksExists();

		DataRecordSpy record = (DataRecordSpy) enhancer.enhance(user, SOME_RECORD_TYPE,
				someDataRecordGroup, dataRedactor);

		assertDeleteActionAuthorizedButHasIncomingLinks(record);
	}

	private void setupForDeleteButIncomingLinksExists() {
		RecordStorageOldSpy recordStorageSpy = createRecordStorageSpy();
		recordStorageSpy.incomingLinksExistsForType.add(SOME_RECORD_TYPE);
	}

	private void assertDeleteActionAuthorizedButHasIncomingLinks(DataRecord record) {
		assertTypeAndCollectedDataAuthorizationCalledForDeleteActionPartOfEnhance();

		RecordStorageOldSpy recordStorageSpy = (RecordStorageOldSpy) dependencyProvider
				.getRecordStorage();
		recordStorageSpy.MCR.assertNumberOfCallsToMethod("linksExistForRecord", 1);
		recordStorageSpy.MCR.assertParameters("linksExistForRecord", 0, SOME_RECORD_TYPE, "someId");

		assertRecordDoesNotContainDeleteAction(record);
	}

	@Test
	public void testEnhanceIgnoreReadAccessDeleteActionPartOfEnhanceAuthorizedButHasIncomingLinks()
			throws Exception {
		setupForDeleteButIncomingLinksExists();

		DataRecordSpy record = (DataRecordSpy) enhancer.enhanceIgnoringReadAccess(user,
				SOME_RECORD_TYPE, someDataRecordGroup, dataRedactor);

		assertDeleteActionAuthorizedButHasIncomingLinks(record);
	}

	@Test
	public void testIncomingLinksActionPartOfEnhanceHasNoIncommingLinks() throws Exception {
		createRecordStorageSpy();

		DataRecordSpy record = (DataRecordSpy) enhancer.enhance(user, SOME_RECORD_TYPE,
				someDataRecordGroup, dataRedactor);

		assertRecordDoesNotContainIncomingLinksAction(record);
	}

	private void assertRecordDoesNotContainIncomingLinksAction(DataRecord record) {
		assertFalse(record.getActions().contains(Action.READ_INCOMING_LINKS));
	}

	@Test
	public void testEnhanceIgnoreReadAccessIncomingLinksActionPartOfEnhanceHasNoIncommingLinks()
			throws Exception {
		createRecordStorageSpy();

		DataRecordSpy record = (DataRecordSpy) enhancer.enhanceIgnoringReadAccess(user,
				SOME_RECORD_TYPE, someDataRecordGroup, dataRedactor);

		assertRecordDoesNotContainIncomingLinksAction(record);
	}

	@Test
	public void testIncomingLinksActionPartOfEnhanceHasIncomingLinks() throws Exception {
		setupForDeleteButIncomingLinksExists();

		DataRecordSpy record = (DataRecordSpy) enhancer.enhance(user, SOME_RECORD_TYPE,
				someDataRecordGroup, dataRedactor);

		assertRecordContainsIncomingLinksAction(record);
	}

	private void assertRecordContainsIncomingLinksAction(DataRecordSpy record) {
		record.MCR.assertCalledParameters("addAction", Action.READ_INCOMING_LINKS);
	}

	@Test
	public void testEnhanceIgnoreReadAccessIncomingLinksActionPartOfEnhanceHasIncomingLinks()
			throws Exception {
		setupForDeleteButIncomingLinksExists();

		DataRecordSpy record = (DataRecordSpy) enhancer.enhanceIgnoringReadAccess(user,
				SOME_RECORD_TYPE, someDataRecordGroup, dataRedactor);

		assertRecordContainsIncomingLinksAction(record);
	}

	@Test
	public void testUploadActionPartOfEnhance() throws Exception {
		setupForUploadAction();

		DataRecordSpy record = (DataRecordSpy) enhancer.enhance(user, BINARY_RECORD_TYPE,
				someDataRecordGroup, dataRedactor);

		assertUploadAction(record);
	}

	private void setupForUploadAction() {
		createRecordStorageSpy();
	}

	private void assertUploadAction(DataRecordSpy record) {
		assertTypeAndCollectedDataAuthorizationCalledForUploadActionPartOfEnhance();
		assertRecordContainsUploadAction(record);
	}

	private void assertTypeAndCollectedDataAuthorizationCalledForUploadActionPartOfEnhance() {
		var permissionTerms = getAssertedCollectedPermissionTermsForRecordType(BINARY_RECORD_TYPE,
				someDataRecordGroup);
		oldAuthorizator.MCR.assertParameters(
				"userIsAuthorizedForActionOnRecordTypeAndCollectedData", 2, user, "upload",
				BINARY_RECORD_TYPE, permissionTerms);
	}

	private void assertRecordContainsUploadAction(DataRecordSpy record) {
		record.MCR.assertCalledParameters("addAction", Action.UPLOAD);
	}

	@Test
	public void testEnhanceIgnoreReadAccessUploadActionPartOfEnhance() throws Exception {
		setupForUploadAction();

		DataRecordSpy record = (DataRecordSpy) enhancer.enhanceIgnoringReadAccess(user,
				BINARY_RECORD_TYPE, someDataRecordGroup, dataRedactor);

		assertUploadAction(record);
	}

	@Test
	public void testUploadActionPartOfEnhanceNotAuthorized() throws Exception {
		setupForNoUploadAccess();

		DataRecordSpy record = (DataRecordSpy) enhancer.enhance(user, BINARY_RECORD_TYPE,
				someDataRecordGroup, dataRedactor);

		assertUploadActionNotAuthorized(record);
	}

	private void setupForNoUploadAccess() {
		setupForUploadAction();
		oldAuthorizator.setNotAutorizedForAction("upload");
	}

	private void assertUploadActionNotAuthorized(DataRecord record) {
		assertTypeAndCollectedDataAuthorizationCalledForUploadActionPartOfEnhance();
		assertRecordDoesNotContainUploadAction(record);

		oldAuthorizator.MCR.assertNumberOfCallsToMethod(
				"userIsAuthorizedForActionOnRecordTypeAndCollectedData", 3);
	}

	@Test
	public void testEnhanceIgnoreReadAccessUploadActionPartOfEnhanceNotAuthorized()
			throws Exception {
		setupForNoUploadAccess();

		DataRecordSpy record = (DataRecordSpy) enhancer.enhanceIgnoringReadAccess(user,
				BINARY_RECORD_TYPE, someDataRecordGroup, dataRedactor);

		assertUploadActionNotAuthorized(record);
	}

	@Test
	public void testUploadActionPartOfEnhanceNotBinary() throws Exception {
		createRecordStorageSpy();

		DataRecordSpy record = (DataRecordSpy) enhancer.enhance(user, SOME_RECORD_TYPE,
				someDataRecordGroup, dataRedactor);

		assertUploadActionNotChildOfBinary(record);
	}

	private void assertUploadActionNotChildOfBinary(DataRecord record) {
		oldAuthorizator.MCR.assertNumberOfCallsToMethod(
				"userIsAuthorizedForActionOnRecordTypeAndCollectedData", 2);
		assertRecordDoesNotContainUploadAction(record);
	}

	private void assertRecordDoesNotContainUploadAction(DataRecord record) {
		assertFalse(record.getActions().contains(Action.UPLOAD));
	}

	@Test
	public void testEnhanceIgnoreReadAccessUploadActionPartOfEnhanceNotChildOfBinary()
			throws Exception {
		createRecordStorageSpy();

		DataRecordSpy record = (DataRecordSpy) enhancer.enhanceIgnoringReadAccess(user,
				SOME_RECORD_TYPE, someDataRecordGroup, dataRedactor);

		assertUploadActionNotChildOfBinary(record);
	}

	@Test
	public void testSearchActionPartOfEnhanceForRecordTypeSearch() throws Exception {
		setupForSearchAction();

		DataRecordSpy record = (DataRecordSpy) enhancer.enhance(user, SOME_RECORD_TYPE,
				someDataRecordGroup, dataRedactor);

		assertSearchActionForRecordTypeSearch(record);
	}

	private void setupForSearchAction() {
		createRecordStorageSpy();
		recordTypeHandlerSpy.representsTheRecordTypeDefiningSearches = true;
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

		// dataFactorySpy.MRV.setSpecificReturnValuesSupplier("factorGroupFromDataRecordGroup",
		// () -> someDataGroup, someDataRecordGroup);

		// DataGroupSpy redactedSomeDataGroup = new DataGroupSpy();
		// dataRedactor.MRV.setDefaultReturnValuesSupplier(
		// "removeChildrenForConstraintsWithoutPermissions", () -> redactedSomeDataGroup);
		//
		// DataRecordGroupSpy convertedSomeDataRecordGroup = new DataRecordGroupSpy();
		// // obs double dataProvider....
		// dataFactorySpy.MRV.setSpecificReturnValuesSupplier("factorRecordGroupFromDataGroup",
		// () -> convertedSomeDataRecordGroup);

		// obs double dataProvider....
		dataFactorySpy.MRV.setDefaultReturnValuesSupplier("factorRecordUsingDataRecordGroup",
				() -> convertedSomeDataRecord);
		// dataFactorySpy.MRV.setSpecificReturnValuesSupplier("factorRecordUsingDataRecordGroup",
		// () -> convertedSomeDataRecord, convertedSomeDataRecordGroup);
	}

	private DataRecordLinkSpy createRecordLinkSpyUsingId(String linkedRecordId) {
		DataRecordLinkSpy linkSpy = new DataRecordLinkSpy();
		linkSpy.MRV.setDefaultReturnValuesSupplier("getLinkedRecordType",
				() -> "someRecordLinkType");
		linkSpy.MRV.setDefaultReturnValuesSupplier("getLinkedRecordId", () -> linkedRecordId);
		return linkSpy;
	}

	private DataRecordLinkSpy createRecordLinkSpyUsingTypeAndId(String linkedRecordType,
			String linkedRecordId) {
		DataRecordLinkSpy linkSpy = new DataRecordLinkSpy();
		linkSpy.MRV.setDefaultReturnValuesSupplier("getLinkedRecordType", () -> linkedRecordType);
		linkSpy.MRV.setDefaultReturnValuesSupplier("getLinkedRecordId", () -> linkedRecordId);
		return linkSpy;
	}

	private DataResourceLinkSpy createResoureLinkSpyUsingId(String linkedRecordId) {
		DataResourceLinkSpy linkSpy = new DataResourceLinkSpy();
		linkSpy.MRV.setDefaultReturnValuesSupplier("getLinkedRecordType",
				() -> "someResourceLinkType");
		linkSpy.MRV.setDefaultReturnValuesSupplier("getLinkedRecordId", () -> linkedRecordId);
		return linkSpy;
	}

	private void assertSearchActionForRecordTypeSearch(DataRecordSpy record) {
		recordTypeHandlerSpy.MCR.assertMethodWasCalled("representsTheRecordTypeDefiningSearches");
		oldAuthorizator.MCR.assertParameters("userIsAuthorizedForActionOnRecordType", 0, user,
				SEARCH, "linkedSearchId1");
		oldAuthorizator.MCR.assertParameters("userIsAuthorizedForActionOnRecordType", 1, user,
				SEARCH, "linkedSearchId2");
		oldAuthorizator.MCR.assertNumberOfCallsToMethod("userIsAuthorizedForActionOnRecordType", 2);
		assertRecordContainsSearchAction(record);
	}

	private void assertRecordContainsSearchAction(DataRecordSpy record) {
		record.MCR.assertCalledParameters("addAction", Action.SEARCH);
	}

	@Test
	public void testEnhanceIgnoreReadAccessSearchActionPartOfEnhanceForRecordTypeSearch()
			throws Exception {
		setupForSearchAction();

		DataRecordSpy record = (DataRecordSpy) enhancer.enhanceIgnoringReadAccess(user,
				SOME_RECORD_TYPE, someDataRecordGroup, dataRedactor);

		assertSearchActionForRecordTypeSearch(record);
	}

	@Test
	public void testSearchActionPartOfEnhanceForRecordTypeSearchNotAuthorized() throws Exception {
		setupForSearchActionNotAuthorized();

		DataRecordSpy record = (DataRecordSpy) enhancer.enhance(user, SOME_RECORD_TYPE,
				someDataRecordGroup, dataRedactor);

		assertSearchActionForRecordTypeSearchNotAuthorized(record);
	}

	private void setupForSearchActionNotAuthorized() {
		createRecordStorageSpy();
		oldAuthorizator.authorizedForActionAndRecordType = false;
		recordTypeHandlerSpy.representsTheRecordTypeDefiningSearches = true;
		addSearchChildsToSomeType();
	}

	private void assertSearchActionForRecordTypeSearchNotAuthorized(DataRecord record) {
		recordTypeHandlerSpy.MCR.assertMethodWasCalled("representsTheRecordTypeDefiningSearches");
		oldAuthorizator.MCR.assertParameters("userIsAuthorizedForActionOnRecordType", 0, user,
				SEARCH, "linkedSearchId1");
		oldAuthorizator.MCR.assertNumberOfCallsToMethod("userIsAuthorizedForActionOnRecordType", 1);
		assertRecordDoesNotContainSearchAction(record);
	}

	private void assertRecordDoesNotContainSearchAction(DataRecord record) {
		assertFalse(record.getActions().contains(Action.SEARCH));
	}

	@Test
	public void testEnhanceIgnoreReadAccessSearchActionPartOfEnhanceForRecordTypeSearchNotAuthorized()
			throws Exception {
		setupForSearchActionNotAuthorized();

		DataRecordSpy record = (DataRecordSpy) enhancer.enhanceIgnoringReadAccess(user,
				SOME_RECORD_TYPE, someDataRecordGroup, dataRedactor);

		assertSearchActionForRecordTypeSearchNotAuthorized(record);
	}

	@Test
	public void testSearchActionPartOfEnhanceForRecordTypeSearchNoSearchType() throws Exception {
		createRecordStorageSpy();

		DataRecordSpy record = (DataRecordSpy) enhancer.enhance(user, SOME_RECORD_TYPE,
				someDataRecordGroup, dataRedactor);

		assertSearchActionForRecordTypeSearchNoSearchType(record);
	}

	private void assertSearchActionForRecordTypeSearchNoSearchType(DataRecord record) {
		recordTypeHandlerSpy.MCR.assertMethodWasCalled("representsTheRecordTypeDefiningSearches");
		assertRecordDoesNotContainSearchAction(record);
	}

	@Test
	public void testEnhanceIgnoreReadAccessSearchActionPartOfEnhanceForRecordTypeSearchNoSearchType()
			throws Exception {
		createRecordStorageSpy();

		DataRecordSpy record = (DataRecordSpy) enhancer.enhanceIgnoringReadAccess(user,
				SOME_RECORD_TYPE, someDataRecordGroup, dataRedactor);

		assertSearchActionForRecordTypeSearchNoSearchType(record);
	}

	@Test
	public void testCreateActionPartOfEnhanceForRecordTypeRecordType() throws Exception {
		RecordTypeHandlerOldSpy recordTypeHandlerForRecordTypeInData = setupForCreateAction();

		DataRecordSpy record = (DataRecordSpy) enhancer.enhance(user, SOME_RECORD_TYPE,
				someDataRecordGroup, dataRedactor);

		assertCreateActionForRecordTypeRecordType(recordTypeHandlerForRecordTypeInData, record);
	}

	private RecordTypeHandlerOldSpy setupForCreateAction() {
		createRecordStorageSpy();
		RecordTypeHandlerOldSpy recordTypeHandlerForRecordTypeInData = dependencyProvider
				.createRecordTypeHandlerSpy("someId");
		recordTypeHandlerSpy.representsTheRecordTypeDefiningRecordTypes = true;
		return recordTypeHandlerForRecordTypeInData;
	}

	private void assertCreateActionForRecordTypeRecordType(
			RecordTypeHandlerOldSpy recordTypeHandlerForRecordTypeInData, DataRecordSpy record) {
		recordTypeHandlerSpy.MCR
				.assertMethodWasCalled("representsTheRecordTypeDefiningRecordTypes");
		dependencyProvider.MCR.assertNumberOfCallsToMethod("getRecordTypeHandler", 2);
		dependencyProvider.MCR.assertParameters("getRecordTypeHandler", 1, "someId");

		oldAuthorizator.MCR.assertParameters("userIsAuthorizedForActionOnRecordType", 0, user,
				CREATE, "someId");
		assertRecordContainsCreateAction(record);
	}

	private void assertRecordContainsCreateAction(DataRecordSpy record) {
		record.MCR.assertCalledParameters("addAction", Action.CREATE);
	}

	@Test
	public void testEnhanceIgnoreReadAccessCreateActionPartOfEnhanceForRecordTypeRecordType()
			throws Exception {
		RecordTypeHandlerOldSpy recordTypeHandlerForRecordTypeInData = setupForCreateAction();

		DataRecordSpy record = (DataRecordSpy) enhancer.enhanceIgnoringReadAccess(user,
				SOME_RECORD_TYPE, someDataRecordGroup, dataRedactor);

		assertCreateActionForRecordTypeRecordType(recordTypeHandlerForRecordTypeInData, record);
	}

	@Test
	public void testCreateActionPartOfEnhanceForRecordTypeRecordTypeNotAuthorized()
			throws Exception {
		setupForCreateActionNotAuthorized();

		DataRecordSpy record = (DataRecordSpy) enhancer.enhance(user, SOME_RECORD_TYPE,
				someDataRecordGroup, dataRedactor);

		assertCreateActionForRecordTypeRecordTypeNotAuthorized(record);
	}

	private void setupForCreateActionNotAuthorized() {
		createRecordStorageSpy();
		someDataRecordGroup = new DataRecordGroupSpy();
		someDataRecordGroup.MRV.setDefaultReturnValuesSupplier("getId", () -> "otherId");
		recordTypeHandlerSpy.representsTheRecordTypeDefiningRecordTypes = true;
		oldAuthorizator.authorizedForActionAndRecordType = false;
	}

	private void assertCreateActionForRecordTypeRecordTypeNotAuthorized(DataRecord record) {
		recordTypeHandlerSpy.MCR
				.assertMethodWasCalled("representsTheRecordTypeDefiningRecordTypes");
		oldAuthorizator.MCR.assertParameters("userIsAuthorizedForActionOnRecordType", 0, user,
				CREATE, "otherId");
		assertRecordDoesNotContainCreateAction(record);
	}

	private void assertRecordDoesNotContainCreateAction(DataRecord record) {
		assertFalse(record.getActions().contains(Action.CREATE));
	}

	@Test
	public void testEnhanceIgnoreReadAccessCreateActionPartOfEnhanceForRecordTypeRecordTypeNotAuthorized()
			throws Exception {
		setupForCreateActionNotAuthorized();

		DataRecordSpy record = (DataRecordSpy) enhancer.enhanceIgnoringReadAccess(user,
				SOME_RECORD_TYPE, someDataRecordGroup, dataRedactor);

		assertCreateActionForRecordTypeRecordTypeNotAuthorized(record);
	}

	@Test
	public void testCreateActionPartOfEnhanceForRecordTypeRecordTypeIsNotRecordType()
			throws Exception {
		createRecordStorageSpy();

		DataRecordSpy record = (DataRecordSpy) enhancer.enhance(user, SOME_RECORD_TYPE,
				someDataRecordGroup, dataRedactor);

		assertCreateActionForRecordTypeRecordTypeIsNotRecordType(record);
	}

	private void assertCreateActionForRecordTypeRecordTypeIsNotRecordType(DataRecord record) {
		recordTypeHandlerSpy.MCR
				.assertMethodWasCalled("representsTheRecordTypeDefiningRecordTypes");
		recordTypeHandlerSpy.MCR.assertMethodNotCalled("isAbstract");
		assertRecordDoesNotContainCreateAction(record);
	}

	@Test
	public void testEnhanceIgnoreReadAccessCreateActionPartOfEnhanceForRecordTypeRecordTypeIsNotRecordType()
			throws Exception {
		createRecordStorageSpy();

		DataRecordSpy record = (DataRecordSpy) enhancer.enhanceIgnoringReadAccess(user,
				SOME_RECORD_TYPE, someDataRecordGroup, dataRedactor);

		assertCreateActionForRecordTypeRecordTypeIsNotRecordType(record);
	}

	@Test
	public void testListActionPartOfEnhanceForRecordTypeRecordType() throws Exception {
		setupForListAction();

		DataRecordSpy record = (DataRecordSpy) enhancer.enhance(user, SOME_RECORD_TYPE,
				someDataRecordGroup, dataRedactor);

		assertListActionForRecordTypeRecordType(record);
	}

	private void setupForListAction() {
		createRecordStorageSpy();
		recordTypeHandlerSpy.representsTheRecordTypeDefiningRecordTypes = true;
	}

	private void assertListActionForRecordTypeRecordType(DataRecordSpy record) {
		recordTypeHandlerSpy.MCR
				.assertMethodWasCalled("representsTheRecordTypeDefiningRecordTypes");
		oldAuthorizator.MCR.assertParameters("userIsAuthorizedForActionOnRecordType", 1, user, LIST,
				"someId");
		assertRecordContainsListAction(record);
	}

	private void assertRecordContainsListAction(DataRecordSpy record) {
		record.MCR.assertCalledParameters("addAction", Action.LIST);
	}

	@Test
	public void testEnhanceIgnoreReadAccessListActionPartOfEnhanceForRecordTypeRecordType()
			throws Exception {
		setupForListAction();

		DataRecordSpy record = (DataRecordSpy) enhancer.enhanceIgnoringReadAccess(user,
				SOME_RECORD_TYPE, someDataRecordGroup, dataRedactor);

		assertListActionForRecordTypeRecordType(record);
	}

	@Test
	public void testListActionPartOfEnhanceForRecordTypeRecordTypeNotAuthorized() throws Exception {
		setupForListActionNotAuthorized();

		DataRecordSpy record = (DataRecordSpy) enhancer.enhance(user, SOME_RECORD_TYPE,
				someDataRecordGroup, dataRedactor);

		assertListActionForRecordTypeRecordTypeNotAuthorized(record);
	}

	private void setupForListActionNotAuthorized() {
		setupForListAction();
		oldAuthorizator.authorizedForActionAndRecordType = false;
	}

	private void assertListActionForRecordTypeRecordTypeNotAuthorized(DataRecord record) {
		recordTypeHandlerSpy.MCR
				.assertMethodWasCalled("representsTheRecordTypeDefiningRecordTypes");
		oldAuthorizator.MCR.assertParameters("userIsAuthorizedForActionOnRecordType", 1, user, LIST,
				"someId");
		assertRecordDoesNotContainListAction(record);
	}

	@Test
	public void testEnhanceIgnoreReadAccessListActionPartOfEnhanceForRecordTypeRecordTypeNotAuthorized()
			throws Exception {
		setupForListActionNotAuthorized();

		DataRecordSpy record = (DataRecordSpy) enhancer.enhanceIgnoringReadAccess(user,
				SOME_RECORD_TYPE, someDataRecordGroup, dataRedactor);

		assertListActionForRecordTypeRecordTypeNotAuthorized(record);
	}

	@Test
	public void testListActionPartOfEnhanceForRecordTypeRecordTypeNotRecordType() throws Exception {
		createRecordStorageSpy();

		DataRecordSpy record = (DataRecordSpy) enhancer.enhance(user, SOME_RECORD_TYPE,
				someDataRecordGroup, dataRedactor);

		assertListActionForRecordTypeRecordTypeNotRecordType(record);
	}

	private void assertListActionForRecordTypeRecordTypeNotRecordType(DataRecord record) {
		recordTypeHandlerSpy.MCR
				.assertMethodWasCalled("representsTheRecordTypeDefiningRecordTypes");

		assertRecordDoesNotContainListAction(record);
	}

	private void assertRecordDoesNotContainListAction(DataRecord record) {
		assertFalse(record.getActions().contains(Action.LIST));
	}

	@Test
	public void testEnhanceIgnoreReadAccessListActionPartOfEnhanceForRecordTypeRecordTypeNotRecordType()
			throws Exception {
		createRecordStorageSpy();

		DataRecordSpy record = (DataRecordSpy) enhancer.enhanceIgnoringReadAccess(user,
				SOME_RECORD_TYPE, someDataRecordGroup, dataRedactor);

		assertListActionForRecordTypeRecordTypeNotRecordType(record);
	}

	@Test
	public void testValidateActionPartOfEnhanceForRecordTypeRecordType() throws Exception {
		setupForListAction();

		DataRecordSpy record = (DataRecordSpy) enhancer.enhance(user, SOME_RECORD_TYPE,
				someDataRecordGroup, dataRedactor);

		assertValidateActionForRecordTypeRecordType(record);
	}

	private void assertValidateActionForRecordTypeRecordType(DataRecordSpy record) {
		recordTypeHandlerSpy.MCR
				.assertMethodWasCalled("representsTheRecordTypeDefiningRecordTypes");
		oldAuthorizator.MCR.assertParameters("userIsAuthorizedForActionOnRecordType", 2, user,
				"validate", "someId");
		record.MCR.assertCalledParameters("addAction", Action.VALIDATE);
	}

	@Test
	public void testEnhanceIgnoreReadAccessValidateActionPartOfEnhanceForRecordTypeRecordType()
			throws Exception {
		setupForListAction();

		DataRecordSpy record = (DataRecordSpy) enhancer.enhanceIgnoringReadAccess(user,
				SOME_RECORD_TYPE, someDataRecordGroup, dataRedactor);

		assertValidateActionForRecordTypeRecordType(record);
	}

	@Test
	public void testValidateActionPartOfEnhanceForRecordTypeRecordTypeNotAuthorized()
			throws Exception {
		setupForCreateActionNotAuthorized();

		DataRecordSpy record = (DataRecordSpy) enhancer.enhance(user, SOME_RECORD_TYPE,
				someDataRecordGroup, dataRedactor);

		assertValidateActionForRecordTypeRecordTypeNotAuthorized(record);
	}

	private void assertValidateActionForRecordTypeRecordTypeNotAuthorized(DataRecord record) {
		recordTypeHandlerSpy.MCR
				.assertMethodWasCalled("representsTheRecordTypeDefiningRecordTypes");
		oldAuthorizator.MCR.assertParameters("userIsAuthorizedForActionOnRecordType", 2, user,
				"validate", "otherId");
		assertValidateActionForRecordTypeRecordTypeNotRecordType(record);
	}

	@Test
	public void testEnhanceIgnoreReadAccessValidateActionPartOfEnhanceForRecordTypeRecordTypeNotAuthorized()
			throws Exception {
		setupForCreateActionNotAuthorized();

		DataRecordSpy record = (DataRecordSpy) enhancer.enhanceIgnoringReadAccess(user,
				SOME_RECORD_TYPE, someDataRecordGroup, dataRedactor);

		assertValidateActionForRecordTypeRecordTypeNotAuthorized(record);
	}

	@Test
	public void testValidateActionPartOfEnhanceForRecordTypeRecordTypeNotRecordType()
			throws Exception {
		createRecordStorageSpy();

		DataRecordSpy record = (DataRecordSpy) enhancer.enhance(user, SOME_RECORD_TYPE,
				someDataRecordGroup, dataRedactor);

		assertValidateActionForRecordTypeRecordTypeNotRecordType(record);
	}

	private void assertValidateActionForRecordTypeRecordTypeNotRecordType(DataRecord record) {
		recordTypeHandlerSpy.MCR
				.assertMethodWasCalled("representsTheRecordTypeDefiningRecordTypes");
		assertRecordContainsValidateAction(record);
	}

	private void assertRecordContainsValidateAction(DataRecord record) {
		assertFalse(record.getActions().contains(Action.VALIDATE));
	}

	@Test
	public void testEnhanceIgnoreReadAccessValidateActionPartOfEnhanceForRecordTypeRecordTypeNotRecordType()
			throws Exception {
		createRecordStorageSpy();

		DataRecordSpy record = (DataRecordSpy) enhancer.enhanceIgnoringReadAccess(user,
				SOME_RECORD_TYPE, someDataRecordGroup, dataRedactor);

		assertValidateActionForRecordTypeRecordTypeNotRecordType(record);
	}

	@Test
	public void testSearchActionPartOfEnhancedWhenEnhancingDataGroupContainingRecordTypeRecord()
			throws Exception {
		RecordTypeHandlerOldSpy recordTypeHandlerForRecordTypeInData = setupForSearchActionWhenEnhancingTypeOfRecordType();

		DataRecordSpy record = (DataRecordSpy) enhancer.enhance(user, SOME_RECORD_TYPE,
				someDataRecordGroup, dataRedactor);

		assertSearchActionForRecordTypeRecordType(recordTypeHandlerForRecordTypeInData, record);
	}

	private RecordTypeHandlerOldSpy setupForSearchActionWhenEnhancingTypeOfRecordType() {
		createRecordStorageSpy();
		createAndSetReturnDataGroupForReadInStorageForReadOfLinkedSearch();
		recordTypeHandlerSpy.representsTheRecordTypeDefiningRecordTypes = true;
		return getRecordTypeHandlerForRecordTypeInData();
	}

	private RecordTypeHandlerOldSpy getRecordTypeHandlerForRecordTypeInData() {
		RecordTypeHandlerOldSpy recordTypeHandlerForRecordTypeInData = dependencyProvider
				.createRecordTypeHandlerSpy("someId");
		recordTypeHandlerForRecordTypeInData.hasLinkedSearch = true;
		return recordTypeHandlerForRecordTypeInData;
	}

	private void assertSearchActionForRecordTypeRecordType(
			RecordTypeHandlerOldSpy recordTypeHandlerForRecordTypeInData, DataRecordSpy record) {
		recordTypeHandlerSpy.MCR
				.assertMethodWasCalled("representsTheRecordTypeDefiningRecordTypes");
		dependencyProvider.MCR.assertNumberOfCallsToMethod("getRecordTypeHandler", 2);
		dependencyProvider.MCR.assertParameters("getRecordTypeHandler", 1, "someId");
		dependencyProvider.MCR.assertReturn("getRecordTypeHandler", 1,
				recordTypeHandlerForRecordTypeInData);

		recordTypeHandlerForRecordTypeInData.MCR.assertMethodWasCalled("hasLinkedSearch");
		recordTypeHandlerForRecordTypeInData.MCR.getReturnValue("getSearchId", 0);

		RecordStorageOldSpy recordStorage = (RecordStorageOldSpy) dependencyProvider
				.getRecordStorage();

		assertReadForSearchRecordToOldRecordStorageSpy(recordStorage);

		oldAuthorizator.MCR.assertParameters("userIsAuthorizedForActionOnRecordType", 3, user,
				SEARCH, "linkedSearchId1");
		oldAuthorizator.MCR.assertParameters("userIsAuthorizedForActionOnRecordType", 4, user,
				SEARCH, "linkedSearchId2");
		oldAuthorizator.MCR.assertNumberOfCallsToMethod("userIsAuthorizedForActionOnRecordType", 6);
		assertRecordContainsSearchAction(record);
	}

	private void assertReadForSearchRecordToOldRecordStorageSpy(RecordStorageOldSpy recordStorage) {
		recordStorage.MCR.assertNumberOfCallsToMethod("read", 1);
		recordStorage.MCR.assertParameterAsEqual("read", 0, "type", SEARCH);
		recordStorage.MCR.assertParameter("read", 0, "id", "someSearchId");
	}

	@Test
	public void testEnhanceIgnoreReadAccessSearchActionPartOfEnhanceForRecordTypeRecordType()
			throws Exception {
		RecordTypeHandlerOldSpy recordTypeHandlerForRecordTypeInData = setupForSearchActionWhenEnhancingTypeOfRecordType();

		DataRecordSpy record = (DataRecordSpy) enhancer.enhanceIgnoringReadAccess(user,
				SOME_RECORD_TYPE, someDataRecordGroup, dataRedactor);

		assertSearchActionForRecordTypeRecordType(recordTypeHandlerForRecordTypeInData, record);
	}

	@Test
	public void testSearchActionPartOfEnhanceForRecordTypeRecordTypeNotAuthorized()
			throws Exception {
		RecordTypeHandlerOldSpy recordTypeHandlerForRecordTypeInData = setupForSearchActionWhenEnhancingTypeOfRecordTypeNotAuthorized();

		DataRecordSpy record = (DataRecordSpy) enhancer.enhance(user, SOME_RECORD_TYPE,
				someDataRecordGroup, dataRedactor);

		assertSearchActionForRecordTypeRecordTypeNotAuthorized(recordTypeHandlerForRecordTypeInData,
				record);
	}

	private RecordTypeHandlerOldSpy setupForSearchActionWhenEnhancingTypeOfRecordTypeNotAuthorized() {
		oldAuthorizator.authorizedForActionAndRecordType = false;
		createRecordStorageSpy();
		createAndSetReturnDataGroupForReadInStorageForReadOfLinkedSearch();
		recordTypeHandlerSpy.representsTheRecordTypeDefiningRecordTypes = true;

		return getRecordTypeHandlerForRecordTypeInData();
	}

	private void assertSearchActionForRecordTypeRecordTypeNotAuthorized(
			RecordTypeHandlerOldSpy recordTypeHandlerForRecordTypeInData, DataRecord record) {
		recordTypeHandlerSpy.MCR
				.assertMethodWasCalled("representsTheRecordTypeDefiningRecordTypes");
		dependencyProvider.MCR.assertNumberOfCallsToMethod("getRecordTypeHandler", 2);
		dependencyProvider.MCR.assertParameters("getRecordTypeHandler", 1, "someId");
		dependencyProvider.MCR.assertReturn("getRecordTypeHandler", 1,
				recordTypeHandlerForRecordTypeInData);

		recordTypeHandlerForRecordTypeInData.MCR.assertMethodWasCalled("hasLinkedSearch");
		String returnedSearchId = (String) recordTypeHandlerForRecordTypeInData.MCR
				.getReturnValue("getSearchId", 0);
		RecordStorageOldSpy recordStorage = (RecordStorageOldSpy) dependencyProvider
				.getRecordStorage();

		assertReadForSearchRecordToOldRecordStorageSpy(recordStorage);

		recordStorage.MCR.assertParameter("read", 0, "id", returnedSearchId);
		oldAuthorizator.MCR.assertParameters("userIsAuthorizedForActionOnRecordType", 3, user,
				SEARCH, "linkedSearchId1");
		oldAuthorizator.MCR.assertNumberOfCallsToMethod("userIsAuthorizedForActionOnRecordType", 5);
		assertRecordDoesNotContainSearchAction(record);
	}

	private void createAndSetReturnDataGroupForReadInStorageForReadOfLinkedSearch() {
		DataRecordLinkSpy linkSpy21 = createRecordLinkSpyUsingId("linkedSearchId1");
		DataRecordLinkSpy linkSpy22 = createRecordLinkSpyUsingId("linkedSearchId2");
		List<DataRecordLinkSpy> linkList2 = List.of(linkSpy21, linkSpy22);

		DataRecordGroupSpy searchGroupLinkedFromRecordType = new DataRecordGroupSpy();
		searchGroupLinkedFromRecordType.MRV.setSpecificReturnValuesSupplier(
				"getChildrenOfTypeAndName", () -> linkList2, DataRecordLink.class,
				"recordTypeToSearchIn");

		RecordStorageOldSpy recordStorage = (RecordStorageOldSpy) dependencyProvider
				.getRecordStorage();
		recordStorage.MRV.setSpecificReturnValuesSupplier("read",
				() -> searchGroupLinkedFromRecordType, "search", "someSearchId");
	}

	@Test
	public void testEnhanceIgnoreReadAccessSearchActionPartOfEnhanceForRecordTypeRecordTypeNotAuthorized()
			throws Exception {
		RecordTypeHandlerOldSpy recordTypeHandlerForRecordTypeInData = setupForSearchActionWhenEnhancingTypeOfRecordTypeNotAuthorized();

		DataRecordSpy record = (DataRecordSpy) enhancer.enhanceIgnoringReadAccess(user,
				SOME_RECORD_TYPE, someDataRecordGroup, dataRedactor);

		assertSearchActionForRecordTypeRecordTypeNotAuthorized(recordTypeHandlerForRecordTypeInData,
				record);
	}

	@Test
	public void testReadPermissionsAreAddedToRecord() throws Exception {
		createRecordStorageSpy();

		DataRecordSpy record = (DataRecordSpy) enhancer.enhance(user, SOME_RECORD_TYPE,
				someDataRecordGroup, dataRedactor);

		assertReadPermissionsAreAddedToRecord(record);
	}

	private void assertReadPermissionsAreAddedToRecord(DataRecord record) {
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
	public void testEnhanceIgnoreReadAccessReadPermissionsAreAddedToRecord() throws Exception {
		createRecordStorageSpy();

		DataRecordSpy record = (DataRecordSpy) enhancer.enhanceIgnoringReadAccess(user,
				SOME_RECORD_TYPE, someDataRecordGroup, dataRedactor);

		assertReadPermissionsAreAddedToRecord(record);
	}

	@Test
	public void testEnhanceIgnoreReadAccessReadPermissionsAreAddedToRecordWhenNoReadAccess()
			throws Exception {
		setupForNoReadAccess();

		DataRecordSpy record = (DataRecordSpy) enhancer.enhanceIgnoringReadAccess(user,
				SOME_RECORD_TYPE, someDataRecordGroup, dataRedactor);

		assertReadPermissionsAreAddedToRecordPublicData(record);
	}

	@Test
	public void testReadPermissionsAreAddedToRecordPublicData() throws Exception {
		setupForPublicRecord();

		DataRecordSpy record = (DataRecordSpy) enhancer.enhance(user, SOME_RECORD_TYPE,
				someDataRecordGroup, dataRedactor);

		assertReadPermissionsAreAddedToRecordPublicData(record);
	}

	private void setupForPublicRecord() {
		recordTypeHandlerSpy.isPublicForRead = true;
		createRecordStorageSpy();
	}

	private void assertReadPermissionsAreAddedToRecordPublicData(DataRecord record) {
		assertEquals(record.getReadPermissions(), Collections.emptySet());
	}

	@Test
	public void testEnhanceIgnoreReadAccessReadPermissionsAreAddedToRecordPublicData()
			throws Exception {
		setupForPublicRecord();

		DataRecordSpy record = (DataRecordSpy) enhancer.enhanceIgnoringReadAccess(user,
				SOME_RECORD_TYPE, someDataRecordGroup, dataRedactor);

		assertReadPermissionsAreAddedToRecordPublicData(record);
	}

	@Test
	public void testWritePermissionsAreAddedToRecord() throws Exception {
		createRecordStorageSpy();

		DataRecordSpy record = (DataRecordSpy) enhancer.enhance(user, SOME_RECORD_TYPE,
				someDataRecordGroup, dataRedactor);

		assertWritePermissionsAreAddedToRecord(record);
	}

	private void assertWritePermissionsAreAddedToRecord(DataRecordSpy record) {
		oldAuthorizator.MCR.assertParameter(
				"checkGetUsersMatchedRecordPartPermissionsForActionOnRecordTypeAndCollectedData", 1,
				"action", UPDATE);
		var writePermissions = record.MCR.getValueForMethodNameAndCallNumberAndParameterName(
				"addWritePermissions", 0, "writePermissions");
		oldAuthorizator.MCR.assertReturn(
				"checkGetUsersMatchedRecordPartPermissionsForActionOnRecordTypeAndCollectedData", 1,
				writePermissions);

		record.MCR.assertNumberOfCallsToMethod("addReadPermissions", 2);
		Set<?> call1 = (Set<?>) record.MCR.getValueForMethodNameAndCallNumberAndParameterName(
				"addReadPermissions", 0, "readPermissions");
		Set<?> call2 = (Set<?>) record.MCR.getValueForMethodNameAndCallNumberAndParameterName(
				"addReadPermissions", 1, "readPermissions");

		assertTrue(call1.contains("someRecordType.someReadMetadataId"));
		assertTrue(call2.contains("someRecordType.someWriteMetadataId"));
	}

	@Test
	public void testEnhanceIgnoreReadAccessWritePermissionsAreAddedToRecord() throws Exception {
		createRecordStorageSpy();

		DataRecordSpy record = (DataRecordSpy) enhancer.enhanceIgnoringReadAccess(user,
				SOME_RECORD_TYPE, someDataRecordGroup, dataRedactor);

		assertWritePermissionsAreAddedToRecord(record);
	}

	@Test
	public void testWritePermissionsAreAddedToRecordNotAutorized() throws Exception {
		setupForNoAccessOnUpdateActionOnRecordType();

		DataRecordSpy record = (DataRecordSpy) enhancer.enhance(user, SOME_RECORD_TYPE,
				someDataRecordGroup, dataRedactor);

		assertWritePermissionsAreAddedToRecordNotAutorized(record);
	}

	private void setupForNoAccessOnUpdateActionOnRecordType() {
		createRecordStorageSpy();
		oldAuthorizator.setNotAutorizedForActionOnRecordType(UPDATE, SOME_RECORD_TYPE);
	}

	private void assertWritePermissionsAreAddedToRecordNotAutorized(DataRecord record) {
		oldAuthorizator.MCR.assertParameter(
				"checkGetUsersMatchedRecordPartPermissionsForActionOnRecordTypeAndCollectedData", 1,
				"action", UPDATE);
		assertEquals(record.getWritePermissions(), Collections.emptySet());
	}

	@Test
	public void testEnhanceIgnoreReadAccessWritePermissionsAreAddedToRecordNotAutorized()
			throws Exception {
		setupForNoAccessOnUpdateActionOnRecordType();

		DataRecordSpy record = (DataRecordSpy) enhancer.enhanceIgnoringReadAccess(user,
				SOME_RECORD_TYPE, someDataRecordGroup, dataRedactor);

		assertWritePermissionsAreAddedToRecordNotAutorized(record);
	}

	@Test
	public void testRedactData() throws Exception {
		createRecordStorageSpy();

		DataRecordSpy record = (DataRecordSpy) enhancer.enhance(user, SOME_RECORD_TYPE,
				someDataRecordGroup, dataRedactor);

		assertRedactCalledWithCorrectArguments();
		assertAswerFromRedactorIsReturned(record);
	}

	private void assertRedactCalledWithCorrectArguments() {
		Set<?> usersReadRecordPartPermissions = (Set<?>) oldAuthorizator.MCR.getReturnValue(
				"checkGetUsersMatchedRecordPartPermissionsForActionOnRecordTypeAndCollectedData",
				0);

		Set<?> recordPartConstraints = (Set<?>) recordTypeHandlerSpy.MCR
				.getReturnValue("getReadRecordPartConstraints", 0);

		dataRedactor.MCR.assertParameters("removeChildrenForConstraintsWithoutPermissions", 0,
				recordTypeHandlerSpy.getDefinitionId(), someDataRecordGroup, recordPartConstraints,
				usersReadRecordPartPermissions);
	}

	private void assertAswerFromRedactorIsReturned(DataRecord record) {
		DataRecordGroup redactedDataGroup = (DataRecordGroup) dataRedactor.MCR
				.getReturnValue("removeChildrenForConstraintsWithoutPermissions", 0);
		var redactedRecord = dataFactorySpy.MCR.assertCalledParametersReturn(
				"factorRecordUsingDataRecordGroup", redactedDataGroup);
		assertSame(record, redactedRecord);
	}

	@Test
	public void testEnhanceIgnoreReadAccessRedactData() throws Exception {
		createRecordStorageSpy();

		DataRecordSpy record = (DataRecordSpy) enhancer.enhanceIgnoringReadAccess(user,
				SOME_RECORD_TYPE, someDataRecordGroup, dataRedactor);

		assertRedactCalledWithCorrectArguments();
		assertAswerFromRedactorIsReturned(record);
	}

	@Test
	public void testEnhanceIgnoreReadAccessRedactDataIsCalledCorrectlyWhenNoAccess()
			throws Exception {
		setupForNoReadAccess();

		DataRecordSpy record = (DataRecordSpy) enhancer.enhanceIgnoringReadAccess(user,
				SOME_RECORD_TYPE, someDataRecordGroup, dataRedactor);

		Set<?> recordPartConstraints = (Set<?>) recordTypeHandlerSpy.MCR
				.getReturnValue("getReadRecordPartConstraints", 0);

		dataRedactor.MCR.assertParameters("removeChildrenForConstraintsWithoutPermissions", 0,
				recordTypeHandlerSpy.getDefinitionId(), someDataRecordGroup, recordPartConstraints,
				Collections.emptySet());
		assertAswerFromRedactorIsReturned(record);
	}

	@Test
	public void testLinksAreAddedToRedactedDataGroup() throws Exception {
		DataRecordLinkSpy linkSpy1 = createRecordLinkSpyUsingId("linkedSearchId1");
		DataRecordLinkSpy linkSpy2 = createRecordLinkSpyUsingId("linkedSearchId2");
		setupReturnedDataGroupOnDataRedactorSpy(linkSpy1, linkSpy2);

		enhancer.enhance(user, DATA_WITH_LINKS, someDataRecordGroup, dataRedactor);

		assertLinkOnlyHasReadAction(linkSpy1);
		assertLinkOnlyHasReadAction(linkSpy2);
	}

	private void assertLinkOnlyHasReadAction(DataRecordLinkSpy linkSpy1) {
		linkSpy1.MCR.assertParameters("addAction", 0, Action.READ);
		linkSpy1.MCR.assertNumberOfCallsToMethod("addAction", 1);
	}

	private void assertResourceLinkOnlyHasReadAction(DataResourceLinkSpy linkSpy1) {
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

	@Test
	public void testEnhanceIgnoreReadAccessLinksAreAddedToRedactedDataGroup() throws Exception {
		DataRecordLinkSpy linkSpy1 = createRecordLinkSpyUsingId("linkedSearchId1");
		DataRecordLinkSpy linkSpy2 = createRecordLinkSpyUsingId("linkedSearchId2");
		setupReturnedDataGroupOnDataRedactorSpy(linkSpy1, linkSpy2);

		enhancer.enhanceIgnoringReadAccess(user, SOME_RECORD_TYPE, someDataRecordGroup,
				dataRedactor);

		assertLinkOnlyHasReadAction(linkSpy1);
		assertLinkOnlyHasReadAction(linkSpy2);
	}

	@Test
	public void testReadRecordWithDataRecordLinkHasNOReadAction() {
		DataRecordLinkSpy linkSpy1 = createRecordLinkSpyUsingId("linkedSearchId1");
		DataRecordLinkSpy linkSpy2 = createRecordLinkSpyUsingId("linkedSearchId2");
		setupReturnedDataGroupOnDataRedactorSpy(linkSpy1, linkSpy2);
		changeToModernSpies();
		recordTypeHandlerSpy.MRV.setSpecificReturnValuesSupplier("isPublicForRead", () -> false);
		authorizator.MRV.setDefaultReturnValuesSupplier(
				"userIsAuthorizedForActionOnRecordTypeAndCollectedData", () -> false);

		enhancer.enhance(user, SOME_RECORD_TYPE, someDataRecordGroup, dataRedactor);

		authorizator.MCR.assertNumberOfCallsToMethod(
				"userIsAuthorizedForActionOnRecordTypeAndCollectedData", 4);

		assertLinkHasNoAction(linkSpy1);
		assertLinkHasNoAction(linkSpy2);
	}

	private void assertLinkHasNoAction(DataRecordLinkSpy linkSpy1) {
		linkSpy1.MCR.assertNumberOfCallsToMethod("addAction", 0);
	}

	private void assertResourceLinkHasNoAction(DataResourceLinkSpy linkSpy1) {
		linkSpy1.MCR.assertNumberOfCallsToMethod("addAction", 0);
	}

	@Test
	public void testReadRecordWithDataRecordLinkIsMissing() {
		DataRecordLinkSpy linkSpy1 = createRecordLinkSpyUsingId("linkedSearchId1");
		DataRecordLinkSpy linkSpy2 = createRecordLinkSpyUsingId("linkedSearchId2");
		setupReturnedDataGroupOnDataRedactorSpy(linkSpy1, linkSpy2);
		changeToModernSpies();
		recordTypeHandlerSpy.MRV.setSpecificReturnValuesSupplier("isPublicForRead", () -> false,
				"someRecordLinkType");
		recordStorage.MRV.setAlwaysThrowException("read",
				RecordNotFoundException.withMessage("fromSpy"));
		authorizator.MRV.setDefaultReturnValuesSupplier(
				"userIsAuthorizedForActionOnRecordTypeAndCollectedData", () -> true);

		enhancer.enhance(user, SOME_RECORD_TYPE, someDataRecordGroup, dataRedactor);

		recordStorage.MCR.assertNumberOfCallsToMethod("read", 2);
		assertLinkHasNoAction(linkSpy1);
		assertLinkHasNoAction(linkSpy2);
	}

	private void changeToModernSpies() {
		dependencyProvider.spiderAuthorizator = authorizator;
		dependencyProvider.recordStorage = recordStorage;
		recordStorage.MRV.setDefaultReturnValuesSupplier("read", DataRecordGroupSpy::new);
		enhancer = new DataGroupToRecordEnhancerImp(dependencyProvider);
	}

	@Test
	public void testEnhanceIgnoreReadAccessReadRecordWithDataRecordLinkHasNOReadAction() {
		DataRecordLinkSpy linkSpy1 = createRecordLinkSpyUsingId("linkedSearchId1");
		DataRecordLinkSpy linkSpy2 = createRecordLinkSpyUsingId("linkedSearchId2");
		setupReturnedDataGroupOnDataRedactorSpy(linkSpy1, linkSpy2);
		changeToModernSpies();
		recordTypeHandlerSpy.MRV.setSpecificReturnValuesSupplier("isPublicForRead", () -> false);
		recordStorage.MRV.setAlwaysThrowException("read",
				RecordNotFoundException.withMessage("fromSpy"));
		authorizator.MRV.setDefaultReturnValuesSupplier(
				"userIsAuthorizedForActionOnRecordTypeAndCollectedData", () -> true);

		enhancer.enhanceIgnoringReadAccess(user, SOME_RECORD_TYPE, someDataRecordGroup,
				dataRedactor);

		recordStorage.MCR.assertNumberOfCallsToMethod("read", 2);
		assertLinkHasNoAction(linkSpy1);
		assertLinkHasNoAction(linkSpy2);
	}

	@Test
	public void testReadRecordWithDataRecordLinkHasReadActionOneLevelDown() {
		DataRecordLinkSpy linkSpy1 = createRecordLinkSpyUsingId("linkedSearchId1");
		DataRecordLinkSpy linkSpy2 = createRecordLinkSpyUsingId("linkedSearchId2");
		setupReturnedDataGroupLinksOneLevelDownOnDataRedactorSpy(linkSpy1, linkSpy2);
		changeToModernSpies();
		recordTypeHandlerSpy.MRV.setSpecificReturnValuesSupplier("isPublicForRead", () -> true);

		enhancer.enhance(user, SOME_RECORD_TYPE, someDataRecordGroup, dataRedactor);

		assertLinkOnlyHasReadAction(linkSpy1);
		assertLinkOnlyHasReadAction(linkSpy2);
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
	public void testEnhanceIgnoreReadAccessReadRecordWithDataRecordLinkHasReadActionOneLevelDown() {
		DataRecordLinkSpy linkSpy1 = createRecordLinkSpyUsingId("linkedSearchId1");
		DataRecordLinkSpy linkSpy2 = createRecordLinkSpyUsingId("linkedSearchId2");
		setupReturnedDataGroupLinksOneLevelDownOnDataRedactorSpy(linkSpy1, linkSpy2);
		changeToModernSpies();
		recordTypeHandlerSpy.MRV.setSpecificReturnValuesSupplier("isPublicForRead", () -> true);

		enhancer.enhanceIgnoringReadAccess(user, SOME_RECORD_TYPE, someDataRecordGroup,
				dataRedactor);

		assertLinkOnlyHasReadAction(linkSpy1);
		assertLinkOnlyHasReadAction(linkSpy2);
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
	public void testReadRecordWithDataResourceLinkNoReadActionTopLevel() {
		DataResourceLinkSpy linkSpy1 = createResoureLinkSpyUsingId("linkedSearchId1");
		DataResourceLinkSpy linkSpy2 = createResoureLinkSpyUsingId("linkedSearchId2");
		setupReturnedDataGroupOnDataRedactorSpy(linkSpy1, linkSpy2);
		changeToModernSpies();
		authorizator.MRV.setDefaultReturnValuesSupplier(
				"userIsAuthorizedForActionOnRecordTypeAndCollectedData", () -> false);

		enhancer.enhance(user, SOME_RECORD_TYPE, someDataRecordGroup, dataRedactor);

		assertResourceLinkHasNoAction(linkSpy1);
		assertResourceLinkHasNoAction(linkSpy2);
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
	public void testLinkIsNotReadWhenRecordTypeIsPublic() {
		DataRecordLinkSpy linkSpy1 = createRecordLinkSpyUsingId("linkedSearchId1");
		DataRecordLinkSpy linkSpy2 = createRecordLinkSpyUsingId("linkedSearchId2");
		setupReturnedDataGroupOnDataRedactorSpy(linkSpy1, linkSpy2);
		changeToModernSpies();
		recordTypeHandlerSpy.MRV.setDefaultReturnValuesSupplier("isPublicForRead", () -> true);

		DataRecordSpy record = (DataRecordSpy) enhancer.enhance(user, SOME_RECORD_TYPE,
				someDataRecordGroup, dataRedactor);

		assertLinkOnlyHasReadAction(linkSpy1);
		assertLinkOnlyHasReadAction(linkSpy2);
		assertLinkIsNotReadWhenRecordTypeIsPublic(record);
	}

	private void assertLinkIsNotReadWhenRecordTypeIsPublic(DataRecordSpy record) {
		recordStorage.MCR.assertNumberOfCallsToMethod("read", 0);

		dependencyProvider.MCR.assertParameters("getRecordTypeHandler", 0, SOME_RECORD_TYPE);
		dependencyProvider.MCR.assertParameters("getRecordTypeHandler", 1, "someRecordLinkType");
		dependencyProvider.MCR.assertNumberOfCallsToMethod("getRecordTypeHandler", 2);
	}

	@Test
	public void testEnhanceIgnoreReadAccessLinkIsNotReadWhenRecordTypeIsPublic() {
		DataRecordLinkSpy linkSpy1 = createRecordLinkSpyUsingId("linkedSearchId1");
		DataRecordLinkSpy linkSpy2 = createRecordLinkSpyUsingId("linkedSearchId2");
		setupReturnedDataGroupOnDataRedactorSpy(linkSpy1, linkSpy2);
		changeToModernSpies();
		recordTypeHandlerSpy.MRV.setDefaultReturnValuesSupplier("isPublicForRead", () -> true);

		DataRecordSpy record = (DataRecordSpy) enhancer.enhanceIgnoringReadAccess(user,
				SOME_RECORD_TYPE, someDataRecordGroup, dataRedactor);

		assertLinkOnlyHasReadAction(linkSpy1);
		assertLinkOnlyHasReadAction(linkSpy2);
		assertLinkIsNotReadWhenRecordTypeIsPublic(record);
	}

	@Test
	public void testLinkedRecordForLinkIsOnlyReadOnceForSameLinkedRecord() {
		DataRecordLinkSpy linkSpy1 = createRecordLinkSpyUsingId("linkedSearchId1");
		DataRecordLinkSpy linkSpy2 = createRecordLinkSpyUsingId("linkedSearchId1");
		setupReturnedDataGroupOnDataRedactorSpy(linkSpy1, linkSpy2);
		changeToModernSpies();
		recordTypeHandlerSpy.MRV.setDefaultReturnValuesSupplier("isPublicForRead", () -> false);

		enhancer.enhanceIgnoringReadAccess(user, SOME_RECORD_TYPE, someDataRecordGroup,
				dataRedactor);

		recordStorage.MCR.assertNumberOfCallsToMethod("read", 1);
	}

	private void assertCallToReadForTypeParameter(int callNumber, String recordType) {
		List<String> types = (List<String>) oldRecordStorage.MCR
				.getValueForMethodNameAndCallNumberAndParameterName("read", callNumber, "types");
		assertEquals(types.size(), 1);
		assertEquals(types.get(0), recordType);
	}

	@Test
	public void testLinkedRecordUsesCorrectRecordTypeHandlerForReadLinkCollectTermPermission() {
		RecordTypeHandlerOldSpy toRecordTypeRecordTypeHandler = new RecordTypeHandlerOldSpy();
		dependencyProvider.mapOfRecordTypeHandlerSpies.put("toRecordType",
				toRecordTypeRecordTypeHandler);
		toRecordTypeRecordTypeHandler.MRV.setDefaultReturnValuesSupplier("getDefinitionId",
				() -> "toRecordType_DefId");

		RecordTypeHandlerOldSpy toOtherRecordTypeRecordTypeHandler = new RecordTypeHandlerOldSpy();
		dependencyProvider.mapOfRecordTypeHandlerSpies.put("toOtherRecordType",
				toOtherRecordTypeRecordTypeHandler);
		toOtherRecordTypeRecordTypeHandler.MRV.setDefaultReturnValuesSupplier("getDefinitionId",
				() -> "toOtherRecordType_DefId");

		DataRecordLinkSpy linkSpy1 = createRecordLinkSpyUsingTypeAndId("toRecordType",
				"linkedSearchId1");
		DataRecordLinkSpy linkSpy2 = createRecordLinkSpyUsingTypeAndId("toOtherRecordType",
				"linkedSearchId2");
		setupReturnedDataGroupOnDataRedactorSpy(linkSpy1, linkSpy2);

		enhancer.enhance(user, DATA_WITH_LINKS, someDataRecordGroup, dataRedactor);

		toRecordTypeRecordTypeHandler.MCR.assertParameters("isPublicForRead", 0);
		toRecordTypeRecordTypeHandler.MCR.assertReturn("isPublicForRead", 0, false);

		dependencyProvider.MCR.assertParameters("getRecordTypeHandler", 0, "dataWithLinks");

		dependencyProvider.MCR.assertParameters("getRecordTypeHandler", 1, "toRecordType");
		toRecordTypeRecordTypeHandler.MCR.assertNumberOfCallsToMethod("getDefinitionId", 1);

		termCollector.MCR.assertParameters("collectTerms", 1, "toRecordType_DefId");

		dependencyProvider.MCR.assertParameters("getRecordTypeHandler", 2, "toOtherRecordType");
		toOtherRecordTypeRecordTypeHandler.MCR.assertNumberOfCallsToMethod("getDefinitionId", 1);

		termCollector.MCR.assertParameters("collectTerms", 2, "toOtherRecordType_DefId");

		dependencyProvider.MCR.assertNumberOfCallsToMethod("getRecordTypeHandler", 3);
	}

	@Test
	public void testEnhanceIgnoreReadAccessLinkedRecordForLinkIsOnlyReadOnceForSameLinkedRecord() {
		DataRecordLinkSpy linkSpy1 = createRecordLinkSpyUsingId("linkedSearchId1");
		DataRecordLinkSpy linkSpy2 = createRecordLinkSpyUsingId("linkedSearchId2");
		setupReturnedDataGroupOnDataRedactorSpy(linkSpy1, linkSpy2);
		changeToModernSpies();

		DataRecordSpy record = (DataRecordSpy) enhancer.enhanceIgnoringReadAccess(user,
				DATA_WITH_LINKS, someDataRecordGroup, dataRedactor);

		assertLinkOnlyHasReadAction(linkSpy1);
		assertLinkOnlyHasReadAction(linkSpy2);
		assertTwoLinksConatainReadActionOnly(record);
		assertLinkedRecordForLinkIsOnlyReadOnceForSameLinkedRecord(record);
	}

	private void assertLinkedRecordForLinkIsOnlyReadOnceForSameLinkedRecord(DataRecord record) {
		dependencyProvider.MCR.assertParameters("getRecordTypeHandler", 0, "dataWithLinks");
		dependencyProvider.MCR.assertParameters("getRecordTypeHandler", 1, "someRecordLinkType");
		dependencyProvider.MCR.assertNumberOfCallsToMethod("getRecordTypeHandler", 2);

		recordStorage.MCR.assertNumberOfCallsToMethod("read", 2);
		recordStorage.MCR.assertParameters("read", 0, "someRecordLinkType", "linkedSearchId1");
		recordStorage.MCR.assertParameters("read", 1, "someRecordLinkType", "linkedSearchId2");

		String metadataId = (String) recordTypeHandlerSpy.MCR.getReturnValue("getDefinitionId", 0);

		termCollector.MCR.assertParameter("collectTerms", 0, "metadataId", metadataId);
		termCollector.MCR.assertParameter("collectTerms", 1, "metadataId", metadataId);
		termCollector.MCR.assertParameter("collectTerms", 2, "metadataId", metadataId);
		termCollector.MCR.assertNumberOfCallsToMethod("collectTerms", 3);

		String methodName = "checkGetUsersMatchedRecordPartPermissionsForActionOnRecordTypeAndCollectedData";
		authorizator.MCR.assertParameters(methodName, 0, user, READ, DATA_WITH_LINKS);
		authorizator.MCR.assertParameters(methodName, 1, user, UPDATE, DATA_WITH_LINKS);
		authorizator.MCR.assertNumberOfCallsToMethod(methodName, 2);

		String methodName2 = "userIsAuthorizedForActionOnRecordTypeAndCollectedData";
		authorizator.MCR.assertNumberOfCallsToMethod(methodName2, 4);
		authorizator.MCR.assertParameters(methodName2, 0, user, READ, "someRecordLinkType");
		authorizator.MCR.assertParameters(methodName2, 1, user, READ, "someRecordLinkType");
		authorizator.MCR.assertParameters(methodName2, 2, user, "index", DATA_WITH_LINKS);
		authorizator.MCR.assertParameters(methodName2, 3, user, "delete", DATA_WITH_LINKS);
	}

	private void assertTwoLinksConatainReadActionOnly(DataRecordSpy record) {
		record.MCR.assertNumberOfCallsToMethod("addAction", 4);
		record.MCR.assertParameters("addAction", 0, Action.READ);
		record.MCR.assertParameters("addAction", 1, Action.UPDATE);
		record.MCR.assertParameters("addAction", 2, Action.INDEX);
		record.MCR.assertParameters("addAction", 3, Action.DELETE);
	}

	@Test
	public void testLinkedRecordForLinkIsOnlyReadOnceForSameLinkedRecordNotAuthorized() {
		DataRecordLinkSpy linkSpy1 = createRecordLinkSpyUsingId("linkedSearchId1");
		DataRecordLinkSpy linkSpy2 = createRecordLinkSpyUsingId("linkedSearchId2");
		setupReturnedDataGroupLinksOneLevelDownOnDataRedactorSpy(linkSpy1, linkSpy2);
		changeToModernSpies();
		authorizator.MRV.setDefaultReturnValuesSupplier(
				"userIsAuthorizedForActionOnRecordTypeAndCollectedData", () -> false);

		DataRecordSpy record = (DataRecordSpy) enhancer.enhance(user, DATA_WITH_LINKS,
				someDataRecordGroup, dataRedactor);

		assertLinkHasNoAction(linkSpy1);
		assertLinkHasNoAction(linkSpy2);
		assertLinkedRecordForLinkIsOnlyReadOnceForSameLinkedRecord(record);
	}

	@Test
	public void testEnhanceIgnoreReadAccessLinkedRecordForLinkIsOnlyReadOnceForSameLinkedRecordNotAuthorized() {
		DataRecordLinkSpy linkSpy1 = createRecordLinkSpyUsingId("linkedSearchId1");
		DataRecordLinkSpy linkSpy2 = createRecordLinkSpyUsingId("linkedSearchId2");
		setupReturnedDataGroupLinksOneLevelDownOnDataRedactorSpy(linkSpy1, linkSpy2);
		changeToModernSpies();
		recordTypeHandlerSpy.MRV.setDefaultReturnValuesSupplier("isPublicForRead", () -> false);
		authorizator.MRV.setDefaultReturnValuesSupplier(
				"userIsAuthorizedForActionOnRecordTypeAndCollectedData", () -> false);

		DataRecordSpy record = (DataRecordSpy) enhancer.enhanceIgnoringReadAccess(user,
				DATA_WITH_LINKS, someDataRecordGroup, dataRedactor);

		assertLinkHasNoAction(linkSpy1);
		assertLinkHasNoAction(linkSpy2);
		assertLinkedRecordForLinkIsOnlyReadOnceForSameLinkedRecord(record);
	}
}
