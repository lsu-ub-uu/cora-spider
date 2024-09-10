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
import static se.uu.ub.cora.spider.record.RecordLinkTestsAsserter.assertRecordStorageWasNOTCalledForReadKey;
import static se.uu.ub.cora.spider.record.RecordLinkTestsAsserter.assertTopLevelTwoLinksContainReadActionOnly;
import static se.uu.ub.cora.spider.record.RecordLinkTestsAsserter.assertTopLevelTwoLinksDoesNotContainReadAction;

import java.util.Collections;
import java.util.List;
import java.util.Set;

import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import se.uu.ub.cora.beefeater.authentication.User;
import se.uu.ub.cora.data.Action;
import se.uu.ub.cora.data.DataGroup;
import se.uu.ub.cora.data.DataLink;
import se.uu.ub.cora.data.DataProvider;
import se.uu.ub.cora.data.DataRecord;
import se.uu.ub.cora.data.DataRecordGroup;
import se.uu.ub.cora.data.DataRecordLink;
import se.uu.ub.cora.data.collected.CollectTerms;
import se.uu.ub.cora.data.collected.PermissionTerm;
import se.uu.ub.cora.data.spies.DataFactorySpy;
import se.uu.ub.cora.data.spies.DataRecordGroupSpy;
import se.uu.ub.cora.data.spies.DataRecordLinkSpy;
import se.uu.ub.cora.data.spies.DataRecordSpy;
import se.uu.ub.cora.logger.LoggerProvider;
import se.uu.ub.cora.spider.authentication.OldAuthenticatorSpy;
import se.uu.ub.cora.spider.authorization.AuthorizationException;
import se.uu.ub.cora.spider.authorization.PermissionRuleCalculator;
import se.uu.ub.cora.spider.dependency.spy.RecordTypeHandlerSpy;
import se.uu.ub.cora.spider.dependency.spy.SpiderDependencyProviderOldSpy;
import se.uu.ub.cora.spider.log.LoggerFactorySpy;
import se.uu.ub.cora.spider.record.DataGroupToRecordEnhancer;
import se.uu.ub.cora.spider.record.RecordEnhancerTestsRecordStorage;
import se.uu.ub.cora.spider.record.RecordLinkTestsAsserter;
import se.uu.ub.cora.spider.spy.DataGroupTermCollectorSpy;
import se.uu.ub.cora.spider.spy.OldSpiderAuthorizatorSpy;
import se.uu.ub.cora.spider.spy.RuleCalculatorSpy;

public class DataGroupToRecordEnhancerTest {
	private static final String UPDATE = "update";
	private static final String LIST = "list";
	private static final String DATA_WITH_LINKS = "dataWithLinks";
	private static final List<String> LIST_DATA_WITH_LINKS = List.of(DATA_WITH_LINKS);
	private static final String CREATE = "create";
	private static final String SEARCH = "search";
	private static final String READ = "read";
	private static final String SOME_RECORD_TYPE = "someRecordType";
	private static final String BINARY_RECORD_TYPE = "binary";
	private RecordEnhancerTestsRecordStorage recordStorage;
	private OldAuthenticatorSpy authenticator;
	private OldSpiderAuthorizatorSpy authorizator;
	private PermissionRuleCalculator ruleCalculator;
	private SpiderDependencyProviderOldSpy dependencyProvider;
	private User user;
	private DataGroupToRecordEnhancer enhancer;
	private DataGroupTermCollectorSpy termCollector;
	private LoggerFactorySpy loggerFactorySpy;
	private DataFactorySpy dataFactorySpy;
	private RecordTypeHandlerSpy recordTypeHandlerSpy;
	private DataRedactorSpy dataRedactor;

	private DataRecordGroupSpy someDataRecordGroup;

	@BeforeMethod
	public void setUp() {
		setUpFactoriesAndProviders();
		// someDataGroup = createDummyDataGroupForRecord("someId");
		someDataRecordGroup = new DataRecordGroupSpy();
		someDataRecordGroup.MRV.setDefaultReturnValuesSupplier("getId", () -> "someId");
		user = new User("987654321");
		recordStorage = new RecordEnhancerTestsRecordStorage();
		authenticator = new OldAuthenticatorSpy();
		authorizator = new OldSpiderAuthorizatorSpy();
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
		dependencyProvider.spiderAuthorizator = authorizator;
		dependencyProvider.recordStorage = recordStorage;
		dependencyProvider.ruleCalculator = ruleCalculator;
		dependencyProvider.termCollector = termCollector;
		recordTypeHandlerSpy = dependencyProvider.recordTypeHandlerSpy;
		enhancer = new DataGroupToRecordEnhancerImp(dependencyProvider);
	}

	// private DataGroup createDummyDataGroupForRecord(String id) {
	// DataGroup dataGroup = new DataGroupOldSpy("someNameInData");
	// DataGroup recordInfo = new DataGroupOldSpy("recordInfo");
	// dataGroup.addChild(recordInfo);
	// recordInfo.addChild(new DataAtomicOldSpy("id", id));
	// recordInfo
	// .addChild(createLinkWithLinkedId("type", "linkedRecordType", "someLinkedRecordId"));
	//
	// return dataGroup;
	// }

	private DataRecordLink createLinkSpyWithLinkedId(String nameInData, String linkedRecordType,
			String linkedRecordId) {
		DataRecordLinkSpy linkSpy = new DataRecordLinkSpy();
		linkSpy.MRV.setDefaultReturnValuesSupplier("getNameInData", () -> nameInData);
		linkSpy.MRV.setDefaultReturnValuesSupplier("getLinkedRecordType", () -> linkedRecordType);
		linkSpy.MRV.setDefaultReturnValuesSupplier("getLinkedRecordId", () -> linkedRecordId);
		return linkSpy;
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
		authorizator.MCR.assertParameters(
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
		RecordTypeHandlerSpy recordTypeHandler = (RecordTypeHandlerSpy) dependencyProvider.MCR
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
		authorizator.authorizedForActionAndRecordTypeAndCollectedData = false;
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
		authorizator.MCR.assertNumberOfCallsToMethod(
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
		authorizator.authorizedForActionAndRecordTypeAndCollectedData = false;
	}

	private void assertReadActionPartOfEnhanceNotAuthorizedButPublicData(DataRecordSpy record) {
		recordTypeHandlerSpy.MCR.assertMethodWasCalled("isPublicForRead");
		authorizator.MCR.assertNumberOfCallsToMethod(
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
		authorizator.MCR.assertParameters(
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
		authorizator.setNotAutorizedForAction(UPDATE);
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
		authorizator.MCR.assertParameters("userIsAuthorizedForActionOnRecordTypeAndCollectedData",
				0, user, "index", SOME_RECORD_TYPE, permissionTerms);
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
		authorizator.MCR.assertParameters("userIsAuthorizedForActionOnRecordType", 3, user,
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
		authorizator.setNotAutorizedForAction("batch_index");
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
		authorizator.setNotAutorizedForAction("index");
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
		authorizator.MCR.assertParameters("userIsAuthorizedForActionOnRecordTypeAndCollectedData",
				1, user, "delete", SOME_RECORD_TYPE, permissionTerms);
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
		authorizator.setNotAutorizedForAction("delete");
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
		authorizator.MCR.assertParameters("userIsAuthorizedForActionOnRecordTypeAndCollectedData",
				2, user, "upload", BINARY_RECORD_TYPE, permissionTerms);
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
		authorizator.setNotAutorizedForAction("upload");
	}

	private void assertUploadActionNotAuthorized(DataRecord record) {
		assertTypeAndCollectedDataAuthorizationCalledForUploadActionPartOfEnhance();
		assertRecordDoesNotContainUploadAction(record);

		authorizator.MCR.assertNumberOfCallsToMethod(
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
		authorizator.MCR.assertNumberOfCallsToMethod(
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
		// DataGroup recordTypeToSearchIn1 = new DataGroupOldSpy("recordTypeToSearchIn");
		// someDataGroup.addChild(recordTypeToSearchIn1);
		// recordTypeToSearchIn1.addChild(new DataAtomicOldSpy("linkedRecordId",
		// "linkedSearchId1"));
		//
		// DataGroup recordTypeToSearchIn2 = new DataGroupOldSpy("recordTypeToSearchIn");
		// someDataGroup.addChild(recordTypeToSearchIn2);
		// recordTypeToSearchIn2.addChild(new DataAtomicOldSpy("linkedRecordId",
		// "linkedSearchId2"));
		DataRecordLinkSpy linkSpy1 = createRecordLinkSpyUsingId("linkedSearchId1");
		DataRecordLinkSpy linkSpy2 = createRecordLinkSpyUsingId("linkedSearchId2");
		List<DataRecordLinkSpy> linkList = List.of(linkSpy1, linkSpy2);

		DataRecordGroupSpy convertedDataRecordGroup = new DataRecordGroupSpy();
		convertedDataRecordGroup.MRV.setSpecificReturnValuesSupplier("getChildrenOfTypeAndName",
				() -> linkList, DataRecordLink.class, "recordTypeToSearchIn");

		// convertedDataRecord.MRV.setDefaultReturnValuesSupplier("getChildrenOfTypeAndName",
		// () -> list);
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
		linkSpy.MRV.setDefaultReturnValuesSupplier("getLinkedRecordId", () -> linkedRecordId);
		return linkSpy;
	}

	private void assertSearchActionForRecordTypeSearch(DataRecordSpy record) {
		recordTypeHandlerSpy.MCR.assertMethodWasCalled("representsTheRecordTypeDefiningSearches");
		authorizator.MCR.assertParameters("userIsAuthorizedForActionOnRecordType", 0, user, SEARCH,
				"linkedSearchId1");
		authorizator.MCR.assertParameters("userIsAuthorizedForActionOnRecordType", 1, user, SEARCH,
				"linkedSearchId2");
		authorizator.MCR.assertNumberOfCallsToMethod("userIsAuthorizedForActionOnRecordType", 2);
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
		authorizator.authorizedForActionAndRecordType = false;
		recordTypeHandlerSpy.representsTheRecordTypeDefiningSearches = true;
		addSearchChildsToSomeType();
	}

	private void assertSearchActionForRecordTypeSearchNotAuthorized(DataRecord record) {
		recordTypeHandlerSpy.MCR.assertMethodWasCalled("representsTheRecordTypeDefiningSearches");
		authorizator.MCR.assertParameters("userIsAuthorizedForActionOnRecordType", 0, user, SEARCH,
				"linkedSearchId1");
		authorizator.MCR.assertNumberOfCallsToMethod("userIsAuthorizedForActionOnRecordType", 1);
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
		RecordTypeHandlerSpy recordTypeHandlerForRecordTypeInData = setupForCreateAction();

		DataRecordSpy record = (DataRecordSpy) enhancer.enhance(user, SOME_RECORD_TYPE,
				someDataRecordGroup, dataRedactor);

		assertCreateActionForRecordTypeRecordType(recordTypeHandlerForRecordTypeInData, record);
	}

	private RecordTypeHandlerSpy setupForCreateAction() {
		createRecordStorageSpy();
		RecordTypeHandlerSpy recordTypeHandlerForRecordTypeInData = dependencyProvider
				.createRecordTypeHandlerSpy("someId");
		recordTypeHandlerSpy.representsTheRecordTypeDefiningRecordTypes = true;
		return recordTypeHandlerForRecordTypeInData;
	}

	private void assertCreateActionForRecordTypeRecordType(
			RecordTypeHandlerSpy recordTypeHandlerForRecordTypeInData, DataRecordSpy record) {
		recordTypeHandlerSpy.MCR
				.assertMethodWasCalled("representsTheRecordTypeDefiningRecordTypes");
		dependencyProvider.MCR.assertNumberOfCallsToMethod("getRecordTypeHandler", 2);
		dependencyProvider.MCR.assertParameters("getRecordTypeHandler", 1, "someId");

		authorizator.MCR.assertParameters("userIsAuthorizedForActionOnRecordType", 0, user, CREATE,
				"someId");
		assertRecordContainsCreateAction(record);
	}

	private void assertRecordContainsCreateAction(DataRecordSpy record) {
		record.MCR.assertCalledParameters("addAction", Action.CREATE);
	}

	@Test
	public void testEnhanceIgnoreReadAccessCreateActionPartOfEnhanceForRecordTypeRecordType()
			throws Exception {
		RecordTypeHandlerSpy recordTypeHandlerForRecordTypeInData = setupForCreateAction();

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
		authorizator.authorizedForActionAndRecordType = false;
	}

	private void assertCreateActionForRecordTypeRecordTypeNotAuthorized(DataRecord record) {
		recordTypeHandlerSpy.MCR
				.assertMethodWasCalled("representsTheRecordTypeDefiningRecordTypes");
		authorizator.MCR.assertParameters("userIsAuthorizedForActionOnRecordType", 0, user, CREATE,
				"otherId");
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
		authorizator.MCR.assertParameters("userIsAuthorizedForActionOnRecordType", 1, user, LIST,
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
		authorizator.authorizedForActionAndRecordType = false;
	}

	private void assertListActionForRecordTypeRecordTypeNotAuthorized(DataRecord record) {
		recordTypeHandlerSpy.MCR
				.assertMethodWasCalled("representsTheRecordTypeDefiningRecordTypes");
		authorizator.MCR.assertParameters("userIsAuthorizedForActionOnRecordType", 1, user, LIST,
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
		authorizator.MCR.assertParameters("userIsAuthorizedForActionOnRecordType", 2, user,
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
		authorizator.MCR.assertParameters("userIsAuthorizedForActionOnRecordType", 2, user,
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
		RecordTypeHandlerSpy recordTypeHandlerForRecordTypeInData = setupForSearchActionWhenEnhancingTypeOfRecordType();

		DataRecordSpy record = (DataRecordSpy) enhancer.enhance(user, SOME_RECORD_TYPE,
				someDataRecordGroup, dataRedactor);

		assertSearchActionForRecordTypeRecordType(recordTypeHandlerForRecordTypeInData, record);
	}

	private RecordTypeHandlerSpy setupForSearchActionWhenEnhancingTypeOfRecordType() {
		createRecordStorageSpy();
		createAndSetReturnDataGroupForReadInStorageForReadOfLinkedSearch();
		recordTypeHandlerSpy.representsTheRecordTypeDefiningRecordTypes = true;
		return getRecordTypeHandlerForRecordTypeInData();
	}

	private RecordTypeHandlerSpy getRecordTypeHandlerForRecordTypeInData() {
		RecordTypeHandlerSpy recordTypeHandlerForRecordTypeInData = dependencyProvider
				.createRecordTypeHandlerSpy("someId");
		recordTypeHandlerForRecordTypeInData.hasLinkedSearch = true;
		return recordTypeHandlerForRecordTypeInData;
	}

	private void assertSearchActionForRecordTypeRecordType(
			RecordTypeHandlerSpy recordTypeHandlerForRecordTypeInData, DataRecordSpy record) {
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

		authorizator.MCR.assertParameters("userIsAuthorizedForActionOnRecordType", 3, user, SEARCH,
				"linkedSearchId1");
		authorizator.MCR.assertParameters("userIsAuthorizedForActionOnRecordType", 4, user, SEARCH,
				"linkedSearchId2");
		authorizator.MCR.assertNumberOfCallsToMethod("userIsAuthorizedForActionOnRecordType", 6);
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
		RecordTypeHandlerSpy recordTypeHandlerForRecordTypeInData = setupForSearchActionWhenEnhancingTypeOfRecordType();

		DataRecordSpy record = (DataRecordSpy) enhancer.enhanceIgnoringReadAccess(user,
				SOME_RECORD_TYPE, someDataRecordGroup, dataRedactor);

		assertSearchActionForRecordTypeRecordType(recordTypeHandlerForRecordTypeInData, record);
	}

	@Test
	public void testSearchActionPartOfEnhanceForRecordTypeRecordTypeNotAuthorized()
			throws Exception {
		RecordTypeHandlerSpy recordTypeHandlerForRecordTypeInData = setupForSearchActionWhenEnhancingTypeOfRecordTypeNotAuthorized();

		DataRecordSpy record = (DataRecordSpy) enhancer.enhance(user, SOME_RECORD_TYPE,
				someDataRecordGroup, dataRedactor);

		assertSearchActionForRecordTypeRecordTypeNotAuthorized(recordTypeHandlerForRecordTypeInData,
				record);
	}

	private RecordTypeHandlerSpy setupForSearchActionWhenEnhancingTypeOfRecordTypeNotAuthorized() {
		authorizator.authorizedForActionAndRecordType = false;
		createRecordStorageSpy();
		createAndSetReturnDataGroupForReadInStorageForReadOfLinkedSearch();
		recordTypeHandlerSpy.representsTheRecordTypeDefiningRecordTypes = true;

		return getRecordTypeHandlerForRecordTypeInData();
	}

	private void assertSearchActionForRecordTypeRecordTypeNotAuthorized(
			RecordTypeHandlerSpy recordTypeHandlerForRecordTypeInData, DataRecord record) {
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
		authorizator.MCR.assertParameters("userIsAuthorizedForActionOnRecordType", 3, user, SEARCH,
				"linkedSearchId1");
		authorizator.MCR.assertNumberOfCallsToMethod("userIsAuthorizedForActionOnRecordType", 5);
		assertRecordDoesNotContainSearchAction(record);
	}

	private void createAndSetReturnDataGroupForReadInStorageForReadOfLinkedSearch() {
		// DataGroupOldSpy searchGroupLinkedFromRecordType = new DataGroupOldSpy("someSearchId");
		// DataGroup recordTypeToSearchIn1 = new DataGroupOldSpy("recordTypeToSearchIn");
		// searchGroupLinkedFromRecordType.addChild(recordTypeToSearchIn1);
		// recordTypeToSearchIn1.addChild(new DataAtomicOldSpy("linkedRecordId",
		// "linkedSearchId1"));
		// DataGroup recordTypeToSearchIn2 = new DataGroupOldSpy("recordTypeToSearchIn");
		// searchGroupLinkedFromRecordType.addChild(recordTypeToSearchIn2);
		// recordTypeToSearchIn2.addChild(new DataAtomicOldSpy("linkedRecordId",
		// "linkedSearchId2"));

		// RecordStorageOldSpy recordStorage = (RecordStorageOldSpy) dependencyProvider
		// .getRecordStorage();
		// recordStorage.returnForRead = searchGroupLinkedFromRecordType;

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
		RecordTypeHandlerSpy recordTypeHandlerForRecordTypeInData = setupForSearchActionWhenEnhancingTypeOfRecordTypeNotAuthorized();

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

		authorizator.MCR.assertParameter(
				"checkGetUsersMatchedRecordPartPermissionsForActionOnRecordTypeAndCollectedData", 0,
				"action", READ);
		Set<?> readPermissions = (Set<?>) authorizator.MCR.getReturnValue(
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
		authorizator.MCR.assertParameter(
				"checkGetUsersMatchedRecordPartPermissionsForActionOnRecordTypeAndCollectedData", 1,
				"action", UPDATE);
		var writePermissions = record.MCR.getValueForMethodNameAndCallNumberAndParameterName(
				"addWritePermissions", 0, "writePermissions");
		authorizator.MCR.assertReturn(
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
		authorizator.setNotAutorizedForActionOnRecordType(UPDATE, SOME_RECORD_TYPE);
	}

	private void assertWritePermissionsAreAddedToRecordNotAutorized(DataRecord record) {
		authorizator.MCR.assertParameter(
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
		Set<?> usersReadRecordPartPermissions = (Set<?>) authorizator.MCR.getReturnValue(
				"checkGetUsersMatchedRecordPartPermissionsForActionOnRecordTypeAndCollectedData",
				0);

		Set<?> recordPartConstraints = (Set<?>) recordTypeHandlerSpy.MCR
				.getReturnValue("getReadRecordPartConstraints", 0);

		var dataGroupFromSomeDataRecordGroup = dataFactorySpy.MCR.assertCalledParametersReturn(
				"factorGroupFromDataRecordGroup", someDataRecordGroup);

		dataRedactor.MCR.assertParameters("removeChildrenForConstraintsWithoutPermissions", 0,
				recordTypeHandlerSpy.getDefinitionId(), dataGroupFromSomeDataRecordGroup,
				recordPartConstraints, usersReadRecordPartPermissions);
	}

	private void assertAswerFromRedactorIsReturned(DataRecord record) {
		DataGroup redactedDataGroup = (DataGroup) dataRedactor.MCR
				.getReturnValue("removeChildrenForConstraintsWithoutPermissions", 0);
		var redactedRecordGroup = dataFactorySpy.MCR
				.assertCalledParametersReturn("factorRecordGroupFromDataGroup", redactedDataGroup);
		var redactedRecord = dataFactorySpy.MCR.assertCalledParametersReturn(
				"factorRecordUsingDataRecordGroup", redactedRecordGroup);

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

	// TODO: fix tests below this line....
	// TODO: fix tests below this line....
	// TODO: fix tests below this line....
	// TODO: fix tests below this line....
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
		setupReturnedDataGroupOnDataRedactorSpy();

		DataRecordSpy record = (DataRecordSpy) enhancer.enhance(user, DATA_WITH_LINKS,
				someDataRecordGroup, dataRedactor);

		RecordLinkTestsAsserter.assertTopLevelLinkContainsReadActionOnly(record);
	}

	private DataGroup setupReturnedDataGroupOnDataRedactorSpy() {
		DataGroup dataGroup = recordStorage.read(LIST_DATA_WITH_LINKS, "oneLinkTopLevel");
		dataRedactor.returnDataGroup = dataGroup;
		return dataGroup;
	}

	@Test
	public void testEnhanceIgnoreReadAccessLinksAreAddedToRedactedDataGroup() throws Exception {
		setupReturnedDataGroupOnDataRedactorSpy();

		DataRecordSpy record = (DataRecordSpy) enhancer.enhanceIgnoringReadAccess(user,
				DATA_WITH_LINKS, someDataRecordGroup, dataRedactor);

		RecordLinkTestsAsserter.assertTopLevelLinkContainsReadActionOnly(record);
	}

	@Test
	public void testReadRecordWithDataRecordLinkHasReadActionTopLevel() {
		DataGroup dataGroup = setupReturnedDataGroupOnDataRedactorSpy();

		DataRecordSpy record = (DataRecordSpy) enhancer.enhance(user, DATA_WITH_LINKS, dataGroup,
				dataRedactor);

		RecordLinkTestsAsserter.assertTopLevelLinkContainsReadActionOnly(record);
	}

	@Test
	public void testEnhanceIgnoreReadAccessReadRecordWithDataRecordLinkHasReadActionTopLevel() {
		DataGroup dataGroup = setupReturnedDataGroupOnDataRedactorSpy();

		DataRecordSpy record = (DataRecordSpy) enhancer.enhanceIgnoringReadAccess(user,
				DATA_WITH_LINKS, dataGroup, dataRedactor);

		RecordLinkTestsAsserter.assertTopLevelLinkContainsReadActionOnly(record);
	}

	@Test
	public void testReadRecordWithDataRecordLinkHasNOReadAction() {
		DataGroup dataGroup = recordStorage.read(LIST_DATA_WITH_LINKS,
				"oneLinkTopLevelNotAuthorized");
		dataRedactor.returnDataGroup = dataGroup;
		authorizator.setNotAutorizedForActionOnRecordType(CREATE, "toRecordType");
		authorizator.setNotAutorizedForActionOnRecordType(LIST, "toRecordType");
		authorizator.setNotAutorizedForActionOnRecordType(SEARCH, "toRecordType");
		authorizator.setNotAutorizedForActionOnRecordType(READ, "toRecordType");

		DataRecordSpy record = (DataRecordSpy) enhancer.enhance(user, DATA_WITH_LINKS, dataGroup,
				dataRedactor);
		RecordLinkTestsAsserter.assertTopLevelLinkDoesNotContainReadAction(record);
	}

	@Test
	public void testReadRecordWithDataRecordLinkIsMissing() {
		DataGroup dataGroup = recordStorage.read(LIST_DATA_WITH_LINKS,
				"oneLinkTopLevelMissingLink");
		dataRedactor.returnDataGroup = dataGroup;
		authorizator.setNotAutorizedForActionOnRecordType(CREATE, "toRecordType");
		authorizator.setNotAutorizedForActionOnRecordType(LIST, "toRecordType");
		authorizator.setNotAutorizedForActionOnRecordType(SEARCH, "toRecordType");
		authorizator.setNotAutorizedForActionOnRecordType(READ, "toRecordType");

		DataRecordSpy record = (DataRecordSpy) enhancer.enhance(user, DATA_WITH_LINKS, dataGroup,
				dataRedactor);
		RecordLinkTestsAsserter.assertTopLevelLinkDoesNotContainReadAction(record);
	}

	@Test
	public void testEnhanceIgnoreReadAccessReadRecordWithDataRecordLinkHasNOReadAction() {
		DataGroup dataGroup = recordStorage.read(LIST_DATA_WITH_LINKS,
				"oneLinkTopLevelNotAuthorized");
		dataRedactor.returnDataGroup = dataGroup;
		authorizator.setNotAutorizedForActionOnRecordType(CREATE, "toRecordType");
		authorizator.setNotAutorizedForActionOnRecordType(LIST, "toRecordType");
		authorizator.setNotAutorizedForActionOnRecordType(SEARCH, "toRecordType");
		authorizator.setNotAutorizedForActionOnRecordType(READ, "toRecordType");

		DataRecordSpy record = (DataRecordSpy) enhancer.enhanceIgnoringReadAccess(user,
				DATA_WITH_LINKS, dataGroup, dataRedactor);
		RecordLinkTestsAsserter.assertTopLevelLinkDoesNotContainReadAction(record);
	}

	@Test
	public void testReadRecordWithDataRecordLinkHasReadActionOneLevelDown() {
		DataGroup dataGroup = recordStorage.read(LIST_DATA_WITH_LINKS, "oneLinkOneLevelDown");
		dataRedactor.returnDataGroup = dataGroup;
		DataRecordSpy record = (DataRecordSpy) enhancer.enhance(user, DATA_WITH_LINKS, dataGroup,
				dataRedactor);

		RecordLinkTestsAsserter.assertOneLevelDownLinkContainsReadActionOnly(record);
	}

	@Test
	public void testEnhanceIgnoreReadAccessReadRecordWithDataRecordLinkHasReadActionOneLevelDown() {
		DataGroup dataGroup = recordStorage.read(LIST_DATA_WITH_LINKS, "oneLinkOneLevelDown");
		dataRedactor.returnDataGroup = dataGroup;
		DataRecordSpy record = (DataRecordSpy) enhancer.enhanceIgnoringReadAccess(user,
				DATA_WITH_LINKS, dataGroup, dataRedactor);

		RecordLinkTestsAsserter.assertOneLevelDownLinkContainsReadActionOnly(record);
	}

	@Test
	public void testReadRecordWithDataResourceLinkHasReadActionTopLevel() {
		String recordType = "dataWithResourceLinks";
		DataGroup dataGroup = recordStorage.read(List.of(recordType), "oneResourceLinkTopLevel");
		dataRedactor.returnDataGroup = dataGroup;
		DataRecordSpy record = (DataRecordSpy) enhancer.enhance(user, recordType, dataGroup,
				dataRedactor);

		RecordLinkTestsAsserter.assertTopLevelResourceLinkContainsReadActionOnly(record);
	}

	@Test
	public void testReadRecordWithDataResourceLinkNoReadActionTopLevel() {
		String recordType = "dataWithResourceLinks";
		DataGroup dataGroup = recordStorage.read(List.of(recordType), "oneResourceLinkTopLevel");
		dataRedactor.returnDataGroup = dataGroup;
		String resourceLinkNameInData = "link";
		String actionForResourceLink = "binary." + resourceLinkNameInData;
		authorizator.setNotAutorizedForActionOnRecordType(READ, actionForResourceLink);

		DataRecordSpy record = (DataRecordSpy) enhancer.enhance(user, recordType, dataGroup,
				dataRedactor);

		authorizator.MCR.assertParameter("userIsAuthorizedForActionOnRecordTypeAndCollectedData", 2,
				"recordType", actionForResourceLink);
		RecordLinkTestsAsserter.assertTopLevelResourceLinkDoesNotContainReadAction(record);
	}

	@Test
	public void testEnhanceIgnoreReadAccessReadRecordWithDataResourceLinkHasReadActionTopLevel() {
		String recordType = "dataWithResourceLinks";
		DataGroup dataGroup = recordStorage.read(List.of(recordType), "oneResourceLinkTopLevel");
		dataRedactor.returnDataGroup = dataGroup;
		DataRecordSpy record = (DataRecordSpy) enhancer.enhanceIgnoringReadAccess(user, recordType,
				dataGroup, dataRedactor);

		RecordLinkTestsAsserter.assertTopLevelResourceLinkContainsReadActionOnly(record);
	}

	@Test
	public void testReadRecordWithDataResourceLinkHasReadActionOneLevelDown() {
		String recordType = "dataWithResourceLinks";
		DataGroup dataGroup = recordStorage.read(List.of(recordType),
				"oneResourceLinkOneLevelDown");
		dataRedactor.returnDataGroup = dataGroup;

		DataRecordSpy record = (DataRecordSpy) enhancer.enhance(user, recordType, dataGroup,
				dataRedactor);

		RecordLinkTestsAsserter.assertOneLevelDownResourceLinkContainsReadActionOnly(record);
	}

	@Test
	public void testEnhanceIgnoreReadAccessReadRecordWithDataResourceLinkHasReadActionOneLevelDown() {
		String recordType = "dataWithResourceLinks";
		DataGroup dataGroup = recordStorage.read(List.of(recordType),
				"oneResourceLinkOneLevelDown");
		dataRedactor.returnDataGroup = dataGroup;

		DataRecordSpy record = (DataRecordSpy) enhancer.enhanceIgnoringReadAccess(user, recordType,
				dataGroup, dataRedactor);

		RecordLinkTestsAsserter.assertOneLevelDownResourceLinkContainsReadActionOnly(record);
	}

	@Test
	public void testReadRecordWithDataRecordLinkTargetDoesNotExist() {
		DataGroup dataGroup = recordStorage.read(LIST_DATA_WITH_LINKS,
				"oneLinkOneLevelDownTargetDoesNotExist");
		dataRedactor.returnDataGroup = dataGroup;

		DataRecordSpy record = (DataRecordSpy) enhancer.enhance(user, DATA_WITH_LINKS,
				someDataRecordGroup, dataRedactor);

		assertReadRecordWithDataRecordLinkTargetDoesNotExist(record);
	}

	private void assertReadRecordWithDataRecordLinkTargetDoesNotExist(DataRecord record) {
		DataRecordGroup recordDataGroup = record.getDataRecordGroup();
		DataGroup dataGroupOneLevelDown = recordDataGroup
				.getFirstGroupWithNameInData("oneLevelDownTargetDoesNotExist");
		DataLink link = (DataLink) dataGroupOneLevelDown.getFirstChildWithNameInData("link");
		assertFalse(link.hasReadAction());
	}

	@Test
	public void testEnhanceIgnoreReadAccessReadRecordWithDataRecordLinkTargetDoesNotExist() {
		DataGroup dataGroup = recordStorage.read(LIST_DATA_WITH_LINKS,
				"oneLinkOneLevelDownTargetDoesNotExist");
		dataRedactor.returnDataGroup = dataGroup;

		DataRecordSpy record = (DataRecordSpy) enhancer.enhanceIgnoringReadAccess(user,
				DATA_WITH_LINKS, someDataRecordGroup, dataRedactor);

		assertReadRecordWithDataRecordLinkTargetDoesNotExist(record);
	}

	@Test
	public void testLinkIsNotReadWhenRecordTypeIsPublic() {
		recordTypeHandlerSpy.isPublicForRead = true;
		DataGroup dataGroup = recordStorage.read(LIST_DATA_WITH_LINKS,
				"oneLinkTopLevelNotAuthorized");
		dataRedactor.returnDataGroup = dataGroup;
		recordStorage.publicReadForToRecordType = "true";

		DataRecordSpy record = (DataRecordSpy) enhancer.enhance(user, DATA_WITH_LINKS,
				someDataRecordGroup, dataRedactor);

		assertLinkIsNotReadWhenRecordTypeIsPublic(record);
	}

	private void assertLinkIsNotReadWhenRecordTypeIsPublic(DataRecordSpy record) {
		assertContainsReadAction(record);
		assertRecordStorageWasNOTCalledForReadKey(recordStorage,
				"toRecordType:recordLinkNotAuthorized");

		dependencyProvider.MCR.assertParameters("getRecordTypeHandler", 0, DATA_WITH_LINKS);
		dependencyProvider.MCR.assertParameters("getRecordTypeHandler", 1, "toRecordType");
		dependencyProvider.MCR.assertParameters("getRecordTypeHandler", 2, "recordType");
		dependencyProvider.MCR.assertParameters("getRecordTypeHandler", 3, "system");
		dependencyProvider.MCR.assertNumberOfCallsToMethod("getRecordTypeHandler", 4);
	}

	private void assertContainsReadAction(DataRecordSpy record) {
		record.MCR.assertCalledParameters("addAction", Action.READ);
	}

	@Test
	public void testEnhanceIgnoreReadAccessLinkIsNotReadWhenRecordTypeIsPublic() {
		DataGroup dataGroup = recordStorage.read(LIST_DATA_WITH_LINKS,
				"oneLinkTopLevelNotAuthorized");
		dataRedactor.returnDataGroup = dataGroup;
		recordStorage.publicReadForToRecordType = "true";

		recordTypeHandlerSpy.MRV.setDefaultReturnValuesSupplier("isPublicForRead", () -> true);

		// dataFactorySpy.MCR.assertParameters("factorRecordGroupFromDataGroup", 0, dataGroup);

		DataRecordSpy record = (DataRecordSpy) enhancer.enhanceIgnoringReadAccess(user,
				DATA_WITH_LINKS, someDataRecordGroup, dataRedactor);

		assertLinkIsNotReadWhenRecordTypeIsPublic(record);
	}

	@Test
	public void testRecordTypeForLinkIsOnlyReadOnce() {
		recordTypeHandlerSpy.isPublicForRead = true;
		recordStorage.publicReadForToRecordType = "true";
		DataGroup dataGroup = recordStorage.read(LIST_DATA_WITH_LINKS, "twoLinksTopLevel");
		dataRedactor.returnDataGroup = dataGroup;

		DataRecordSpy record = (DataRecordSpy) enhancer.enhance(user, DATA_WITH_LINKS,
				someDataRecordGroup, dataRedactor);

		assertRecordTypeForLinkIsOnlyReadOnce(record);
	}

	private void assertRecordTypeForLinkIsOnlyReadOnce(DataRecord record) {
		assertTopLevelTwoLinksContainReadActionOnly(record);

		assertRecordStorageWasNOTCalledForReadKey(recordStorage,
				"toRecordType:recordLinkNotAuthorized");

		dependencyProvider.MCR.assertParameters("getRecordTypeHandler", 0, DATA_WITH_LINKS);
		dependencyProvider.MCR.assertParameters("getRecordTypeHandler", 1, "recordType");
		dependencyProvider.MCR.assertParameters("getRecordTypeHandler", 2, "system");
		dependencyProvider.MCR.assertParameters("getRecordTypeHandler", 3, "toRecordType");
		dependencyProvider.MCR.assertNumberOfCallsToMethod("getRecordTypeHandler", 4);
	}

	@Test
	public void testEnhanceIgnoreReadAccessRecordTypeForLinkIsOnlyReadOnce() {
		recordTypeHandlerSpy.isPublicForRead = true;
		recordStorage.publicReadForToRecordType = "true";
		DataGroup dataGroup = recordStorage.read(LIST_DATA_WITH_LINKS, "twoLinksTopLevel");
		dataRedactor.returnDataGroup = dataGroup;

		DataRecordSpy record = (DataRecordSpy) enhancer.enhanceIgnoringReadAccess(user,
				DATA_WITH_LINKS, someDataRecordGroup, dataRedactor);

		assertRecordTypeForLinkIsOnlyReadOnce(record);
	}

	@Test
	public void testLinkedRecordForLinkIsOnlyReadOnceForSameLinkedRecord() {
		DataGroup dataGroup = recordStorage.read(LIST_DATA_WITH_LINKS, "twoLinksTopLevel");
		dataRedactor.returnDataGroup = dataGroup;

		DataRecordSpy record = (DataRecordSpy) enhancer.enhance(user, DATA_WITH_LINKS,
				someDataRecordGroup, dataRedactor);

		assertTwoLinksConatainReadActionOnly(record);
		assertLinkedRecordForLinkIsOnlyReadOnceForSameLinkedRecord(record);
	}

	private void assertLinkedRecordForLinkIsOnlyReadOnceForSameLinkedRecord(DataRecord record) {
		dependencyProvider.MCR.assertNumberOfCallsToMethod("getRecordTypeHandler", 4);
		dependencyProvider.MCR.assertParameters("getRecordTypeHandler", 0, "dataWithLinks");
		dependencyProvider.MCR.assertParameters("getRecordTypeHandler", 1, "recordType");
		dependencyProvider.MCR.assertParameters("getRecordTypeHandler", 2, "system");
		dependencyProvider.MCR.assertParameters("getRecordTypeHandler", 3, "toRecordType");

		recordStorage.MCR.assertNumberOfCallsToMethod("read", 4);
		assertCallToReadForTypeParameter(0, "dataWithLinks");
		recordStorage.MCR.assertParameter("read", 0, "id", "twoLinksTopLevel");
		assertCallToReadForTypeParameter(1, "recordType");
		recordStorage.MCR.assertParameter("read", 1, "id", "dataWithLinks");
		assertCallToReadForTypeParameter(2, "system");
		recordStorage.MCR.assertParameter("read", 2, "id", "cora");
		assertCallToReadForTypeParameter(3, "toRecordType");
		recordStorage.MCR.assertParameter("read", 3, "id", "toRecordId");

		String metadataId = (String) recordTypeHandlerSpy.MCR.getReturnValue("getDefinitionId", 0);

		termCollector.MCR.assertParameter("collectTerms", 0, "metadataId", metadataId);
		termCollector.MCR.assertParameter("collectTerms", 1, "metadataId", metadataId);
		termCollector.MCR.assertParameter("collectTerms", 2, "metadataId", metadataId);
		termCollector.MCR.assertParameter("collectTerms", 3, "metadataId", metadataId);
		termCollector.MCR.assertNumberOfCallsToMethod("collectTerms", 4);

		String methodName = "checkGetUsersMatchedRecordPartPermissionsForActionOnRecordTypeAndCollectedData";
		authorizator.MCR.assertParameters(methodName, 0, user, READ, DATA_WITH_LINKS);
		authorizator.MCR.assertParameters(methodName, 1, user, UPDATE, DATA_WITH_LINKS);
		authorizator.MCR.assertNumberOfCallsToMethod(methodName, 2);

		String methodName2 = "userIsAuthorizedForActionOnRecordTypeAndCollectedData";
		authorizator.MCR.assertParameters(methodName2, 0, user, "index", DATA_WITH_LINKS);
		authorizator.MCR.assertParameters(methodName2, 1, user, "delete", DATA_WITH_LINKS);
		authorizator.MCR.assertParameters(methodName2, 2, user, READ, "recordType");
		authorizator.MCR.assertParameters(methodName2, 3, user, READ, "system");
		authorizator.MCR.assertParameters(methodName2, 4, user, READ, "toRecordType");
		authorizator.MCR.assertNumberOfCallsToMethod(methodName2, 5);
	}

	private void assertCallToReadForTypeParameter(int callNumber, String recordType) {
		List<String> types = (List<String>) recordStorage.MCR
				.getValueForMethodNameAndCallNumberAndParameterName("read", callNumber, "types");
		assertEquals(types.size(), 1);
		assertEquals(types.get(0), recordType);
	}

	@Test
	public void testLinkedRecordUsesCorrectRecordTypeHandlerForReadLinkCollectTermPermission() {
		RecordTypeHandlerSpy toRecordTypeRecordTypeHandler = new RecordTypeHandlerSpy();
		dependencyProvider.mapOfRecordTypeHandlerSpies.put("toRecordType",
				toRecordTypeRecordTypeHandler);
		toRecordTypeRecordTypeHandler.MRV.setDefaultReturnValuesSupplier("getDefinitionId",
				() -> "toRecordType_DefId");

		RecordTypeHandlerSpy toOtherRecordTypeRecordTypeHandler = new RecordTypeHandlerSpy();
		dependencyProvider.mapOfRecordTypeHandlerSpies.put("toOtherRecordType",
				toOtherRecordTypeRecordTypeHandler);
		toOtherRecordTypeRecordTypeHandler.MRV.setDefaultReturnValuesSupplier("getDefinitionId",
				() -> "toOtherRecordType_DefId");

		DataGroup dataGroup = recordStorage.read(LIST_DATA_WITH_LINKS,
				"twoLinksDifferentRecordTypeTopLevel");
		dataRedactor.returnDataGroup = dataGroup;

		DataRecordSpy record = (DataRecordSpy) enhancer.enhance(user, DATA_WITH_LINKS,
				someDataRecordGroup, dataRedactor);

		toRecordTypeRecordTypeHandler.MCR.assertParameters("isPublicForRead", 0);
		toRecordTypeRecordTypeHandler.MCR.assertReturn("isPublicForRead", 0, false);

		dependencyProvider.MCR.assertParameters("getRecordTypeHandler", 0, "dataWithLinks");
		dependencyProvider.MCR.assertParameters("getRecordTypeHandler", 1, "recordType");
		dependencyProvider.MCR.assertParameters("getRecordTypeHandler", 2, "system");

		dependencyProvider.MCR.assertParameters("getRecordTypeHandler", 3, "toRecordType");
		toRecordTypeRecordTypeHandler.MCR.assertNumberOfCallsToMethod("getDefinitionId", 1);
		termCollector.MCR.assertParameters("collectTerms", 3, "toRecordType_DefId");

		dependencyProvider.MCR.assertParameters("getRecordTypeHandler", 4, "toOtherRecordType");
		toOtherRecordTypeRecordTypeHandler.MCR.assertNumberOfCallsToMethod("getDefinitionId", 1);
		termCollector.MCR.assertParameters("collectTerms", 4, "toOtherRecordType_DefId");

		dependencyProvider.MCR.assertNumberOfCallsToMethod("getRecordTypeHandler", 5);
	}

	@Test
	public void testEnhanceIgnoreReadAccessLinkedRecordForLinkIsOnlyReadOnceForSameLinkedRecord() {
		DataGroup dataGroup = recordStorage.read(LIST_DATA_WITH_LINKS, "twoLinksTopLevel");
		dataRedactor.returnDataGroup = dataGroup;

		DataRecordSpy record = (DataRecordSpy) enhancer.enhanceIgnoringReadAccess(user,
				DATA_WITH_LINKS, someDataRecordGroup, dataRedactor);
		assertTwoLinksConatainReadActionOnly(record);
		assertLinkedRecordForLinkIsOnlyReadOnceForSameLinkedRecord(record);
	}

	private void assertTwoLinksConatainReadActionOnly(DataRecordSpy record) {
		// assertTopLevelTwoLinksContainReadActionOnly(record);
		record.MCR.assertNumberOfCallsToMethod("addAction", 4);
		record.MCR.assertParameters("addAction", 0, Action.READ);
		record.MCR.assertParameters("addAction", 1, Action.UPDATE);
		record.MCR.assertParameters("addAction", 2, Action.INDEX);
		record.MCR.assertParameters("addAction", 3, Action.DELETE);
	}

	@Test
	public void testLinkedRecordForLinkIsOnlyReadOnceForSameLinkedRecordNotAuthorized() {
		DataGroup dataGroup = recordStorage.read(LIST_DATA_WITH_LINKS, "twoLinksTopLevel");
		dataRedactor.returnDataGroup = dataGroup;

		authorizator.setNotAutorizedForActionOnRecordType(READ, "system");
		authorizator.setNotAutorizedForActionOnRecordType(READ, "toRecordType");

		DataRecordSpy record = (DataRecordSpy) enhancer.enhance(user, DATA_WITH_LINKS, dataGroup,
				dataRedactor);

		assertTopLevelTwoLinksDoesNotContainReadAction(record);
		assertLinkedRecordForLinkIsOnlyReadOnceForSameLinkedRecord(record);
	}

	@Test
	public void testEnhanceIgnoreReadAccessLinkedRecordForLinkIsOnlyReadOnceForSameLinkedRecordNotAuthorized() {
		DataGroup dataGroup = recordStorage.read(LIST_DATA_WITH_LINKS, "twoLinksTopLevel");
		dataRedactor.returnDataGroup = dataGroup;

		authorizator.setNotAutorizedForActionOnRecordType(READ, "system");
		authorizator.setNotAutorizedForActionOnRecordType(READ, "toRecordType");

		DataRecordSpy record = (DataRecordSpy) enhancer.enhanceIgnoringReadAccess(user,
				DATA_WITH_LINKS, dataGroup, dataRedactor);

		assertTopLevelTwoLinksDoesNotContainReadAction(record);
		assertLinkedRecordForLinkIsOnlyReadOnceForSameLinkedRecord(record);
	}
}
