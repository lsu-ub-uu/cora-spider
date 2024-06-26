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
import java.util.function.Supplier;

import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import se.uu.ub.cora.beefeater.authentication.User;
import se.uu.ub.cora.data.Action;
import se.uu.ub.cora.data.DataGroup;
import se.uu.ub.cora.data.DataLink;
import se.uu.ub.cora.data.DataProvider;
import se.uu.ub.cora.data.DataRecord;
import se.uu.ub.cora.data.DataRecordFactory;
import se.uu.ub.cora.data.DataRecordLink;
import se.uu.ub.cora.data.DataRecordProvider;
import se.uu.ub.cora.data.collected.CollectTerms;
import se.uu.ub.cora.data.collected.PermissionTerm;
import se.uu.ub.cora.data.spies.DataFactorySpy;
import se.uu.ub.cora.data.spies.DataRecordLinkSpy;
import se.uu.ub.cora.logger.LoggerProvider;
import se.uu.ub.cora.spider.authentication.OldAuthenticatorSpy;
import se.uu.ub.cora.spider.authorization.AuthorizationException;
import se.uu.ub.cora.spider.authorization.PermissionRuleCalculator;
import se.uu.ub.cora.spider.data.DataAtomicSpy;
import se.uu.ub.cora.spider.data.DataGroupOldSpy;
import se.uu.ub.cora.spider.data.DataRecordFactorySpy;
import se.uu.ub.cora.spider.data.DataRecordOldSpy;
import se.uu.ub.cora.spider.dependency.spy.RecordTypeHandlerSpy;
import se.uu.ub.cora.spider.dependency.spy.SpiderDependencyProviderOldSpy;
import se.uu.ub.cora.spider.log.LoggerFactorySpy;
import se.uu.ub.cora.spider.record.DataGroupToRecordEnhancer;
import se.uu.ub.cora.spider.record.DataRedactorSpy;
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
	private DataRecordFactory dataRecordFactorySpy;
	private RecordTypeHandlerSpy recordTypeHandlerSpy;
	private DataRedactorSpy dataRedactor;

	private DataGroup someDataGroup;

	@BeforeMethod
	public void setUp() {
		setUpFactoriesAndProviders();
		someDataGroup = createDummyDataGroupForRecord("someId");
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

		dataRecordFactorySpy = new DataRecordFactorySpy();
		DataRecordProvider.setDataRecordFactory(dataRecordFactorySpy);
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

	private DataGroup createDummyDataGroupForRecord(String id) {
		DataGroup dataGroup = new DataGroupOldSpy("someNameInData");
		DataGroup recordInfo = new DataGroupOldSpy("recordInfo");
		dataGroup.addChild(recordInfo);
		recordInfo.addChild(new DataAtomicSpy("id", id));
		recordInfo
				.addChild(createLinkWithLinkedId("type", "linkedRecordType", "someLinkedRecordId"));

		return dataGroup;
	}

	private DataRecordLink createLinkWithLinkedId(String nameInData, String linkedRecordType,
			String id) {
		DataRecordLinkSpy linkSpy = new DataRecordLinkSpy();
		linkSpy.MRV.setDefaultReturnValuesSupplier("getNameInData",
				(Supplier<String>) () -> nameInData);
		linkSpy.MRV.setDefaultReturnValuesSupplier("getLinkedRecordId",
				(Supplier<String>) () -> id);
		return linkSpy;
	}

	@Test
	public void testReadActionPartOfEnhance() throws Exception {
		createRecordStorageSpy();

		DataRecord record = enhancer.enhance(user, SOME_RECORD_TYPE, someDataGroup, dataRedactor);

		assertReadActionPartOfEnhance(record);
	}

	private void assertReadActionPartOfEnhance(DataRecord record) {
		assertCheckAndGetAuthorizationCalledForReadActionPartOfEnhance();
		assertRecordContainsReadAction(record);
	}

	private void assertCheckAndGetAuthorizationCalledForReadActionPartOfEnhance() {
		var permissionTerms = getAssertedCollectedPermissionTermsForRecordType(SOME_RECORD_TYPE,
				someDataGroup);
		authorizator.MCR.assertParameters(
				"checkGetUsersMatchedRecordPartPermissionsForActionOnRecordTypeAndCollectedData", 0,
				user, READ, SOME_RECORD_TYPE, permissionTerms, true);
	}

	private void assertRecordContainsReadAction(DataRecord record) {
		assertTrue(record.getActions().contains(Action.READ));
	}

	private RecordStorageOldSpy createRecordStorageSpy() {
		RecordStorageOldSpy recordStorageSpy = new RecordStorageOldSpy();
		dependencyProvider.recordStorage = recordStorageSpy;
		enhancer = new DataGroupToRecordEnhancerImp(dependencyProvider);
		return recordStorageSpy;
	}

	private List<PermissionTerm> getAssertedCollectedPermissionTermsForRecordType(String recordType,
			DataGroup dataGroup) {
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
				dataGroup);
		return ((CollectTerms) termCollectorSpy.MCR.getReturnValue("collectTerms",
				0)).permissionTerms;
	}

	@Test
	public void testEnhanceIgnoreReadAccessReadActionPartOfEnhance() throws Exception {
		createRecordStorageSpy();

		DataRecord record = enhancer.enhanceIgnoringReadAccess(user, SOME_RECORD_TYPE,
				someDataGroup, dataRedactor);

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
			enhancer.enhance(user, SOME_RECORD_TYPE, someDataGroup, null);
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

		DataRecord record = enhancer.enhanceIgnoringReadAccess(user, SOME_RECORD_TYPE,
				someDataGroup, dataRedactor);

		assertCheckAndGetAuthorizationCalledForReadActionPartOfEnhance();

		assertRecordDoesNotContainReadAction(record);
	}

	private void assertRecordDoesNotContainReadAction(DataRecord record) {
		assertFalse(record.getActions().contains(Action.READ));
	}

	@Test
	public void testReadActionPartOfEnhanceNotAuthorizedButPublicData() throws Exception {
		setupForNoReadAccessButPublicData();

		DataRecord record = enhancer.enhance(user, SOME_RECORD_TYPE, someDataGroup, dataRedactor);

		assertReadActionPartOfEnhanceNotAuthorizedButPublicData(record);
	}

	private void setupForNoReadAccessButPublicData() {
		createRecordStorageSpy();
		recordTypeHandlerSpy.isPublicForRead = true;
		authorizator.authorizedForActionAndRecordTypeAndCollectedData = false;
	}

	private void assertReadActionPartOfEnhanceNotAuthorizedButPublicData(DataRecord record) {
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

		DataRecord record = enhancer.enhanceIgnoringReadAccess(user, SOME_RECORD_TYPE,
				someDataGroup, dataRedactor);

		assertReadActionPartOfEnhanceNotAuthorizedButPublicData(record);
	}

	@Test
	public void testUpdateActionPartOfEnhance() throws Exception {
		createRecordStorageSpy();

		DataRecord record = enhancer.enhance(user, SOME_RECORD_TYPE, someDataGroup, dataRedactor);

		assertUpdateActionPartOfEnhance(record);
	}

	private void assertUpdateActionPartOfEnhance(DataRecord record) {
		assertCheckAndGetAuthorizationCalledForUpdateActionPartOfEnhance();
		assertRecordContainsUpdateAction(record);
	}

	private void assertRecordContainsUpdateAction(DataRecord record) {
		assertTrue(record.getActions().contains(Action.UPDATE));
	}

	private void assertCheckAndGetAuthorizationCalledForUpdateActionPartOfEnhance() {
		var permissionTerms = getAssertedCollectedPermissionTermsForRecordType(SOME_RECORD_TYPE,
				someDataGroup);
		authorizator.MCR.assertParameters(
				"checkGetUsersMatchedRecordPartPermissionsForActionOnRecordTypeAndCollectedData", 1,
				user, UPDATE, SOME_RECORD_TYPE, permissionTerms, true);
	}

	@Test
	public void testEnhanceIgnoreReadAccessUpdateActionPartOfEnhance() throws Exception {
		createRecordStorageSpy();

		DataRecord record = enhancer.enhanceIgnoringReadAccess(user, SOME_RECORD_TYPE,
				someDataGroup, dataRedactor);

		assertUpdateActionPartOfEnhance(record);
	}

	@Test
	public void testUpdateActionPartOfEnhanceNotAuthorized() throws Exception {
		setupForNoUpdateAccess();

		DataRecord record = enhancer.enhance(user, SOME_RECORD_TYPE, someDataGroup, dataRedactor);

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

		DataRecord record = enhancer.enhanceIgnoringReadAccess(user, SOME_RECORD_TYPE,
				someDataGroup, dataRedactor);

		assertUpdateActionPartOfEnhanceNotAuthorized(record);
	}

	@Test
	public void testIndexActionPartOfEnhance() throws Exception {
		createRecordStorageSpy();

		DataRecord record = enhancer.enhance(user, SOME_RECORD_TYPE, someDataGroup, dataRedactor);

		assertIndexAction(record);
	}

	private void assertIndexAction(DataRecord record) {
		assertTypeAndCollectedDataAuthorizationCalledForIndexActionPartOfEnhance();
		assertRecordContainsIndexAction(record);
	}

	private void assertTypeAndCollectedDataAuthorizationCalledForIndexActionPartOfEnhance() {
		var permissionTerms = getAssertedCollectedPermissionTermsForRecordType(SOME_RECORD_TYPE,
				someDataGroup);
		authorizator.MCR.assertParameters("userIsAuthorizedForActionOnRecordTypeAndCollectedData",
				0, user, "index", SOME_RECORD_TYPE, permissionTerms);
	}

	private void assertRecordContainsIndexAction(DataRecord record) {
		assertTrue(record.getActions().contains(Action.INDEX));
	}

	@Test
	public void testBatchIndexActionPartOfEnhance() throws Exception {
		createRecordStorageSpy();
		recordTypeHandlerSpy.representsTheRecordTypeDefiningRecordTypes = true;

		DataRecord record = enhancer.enhance(user, SOME_RECORD_TYPE, someDataGroup, dataRedactor);

		assertBatchIndexAction(record);
	}

	private void assertBatchIndexAction(DataRecord record) {
		assertTypeAndCollectedDataAuthorizationCalledForBatchIndexActionPartOfEnhance();
		assertRecordContainsBatchIndexAction(record);
	}

	private void assertTypeAndCollectedDataAuthorizationCalledForBatchIndexActionPartOfEnhance() {
		authorizator.MCR.assertParameters("userIsAuthorizedForActionOnRecordType", 3, user,
				"batch_index", "someId");
	}

	private void assertRecordContainsBatchIndexAction(DataRecord record) {
		assertTrue(record.getActions().contains(Action.BATCH_INDEX));
	}

	@Test
	public void testEnhanceIgnoreReadAccessBatchIndexActionPartOfEnhance() throws Exception {
		createRecordStorageSpy();
		recordTypeHandlerSpy.representsTheRecordTypeDefiningRecordTypes = true;

		DataRecord record = enhancer.enhanceIgnoringReadAccess(user, SOME_RECORD_TYPE,
				someDataGroup, dataRedactor);

		assertBatchIndexAction(record);
	}

	@Test
	public void testBatchIndexActionPartOfEnhanceNotAuthorized() throws Exception {
		setupForNoBatchIndexAccess();

		DataRecord record = enhancer.enhance(user, SOME_RECORD_TYPE, someDataGroup, dataRedactor);

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

		DataRecord record = enhancer.enhanceIgnoringReadAccess(user, SOME_RECORD_TYPE,
				someDataGroup, dataRedactor);

		assertIndexAction(record);
	}

	@Test
	public void testIndexActionPartOfEnhanceNotAuthorized() throws Exception {
		setupForNoIndexAccess();

		DataRecord record = enhancer.enhance(user, SOME_RECORD_TYPE, someDataGroup, dataRedactor);

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

		DataRecord record = enhancer.enhanceIgnoringReadAccess(user, SOME_RECORD_TYPE,
				someDataGroup, dataRedactor);

		assertIndexActionNotAuthorized(record);
	}

	@Test
	public void testDeleteActionPartOfEnhance() throws Exception {
		createRecordStorageSpy();

		DataRecord record = enhancer.enhance(user, SOME_RECORD_TYPE, someDataGroup, dataRedactor);

		assertDeleteAction(record);
	}

	private void assertDeleteAction(DataRecord record) {
		assertTypeAndCollectedDataAuthorizationCalledForDeleteActionPartOfEnhance();
		assertRecordContainsDeleteAction(record);
	}

	private void assertRecordContainsDeleteAction(DataRecord record) {
		assertTrue(record.getActions().contains(Action.DELETE));
	}

	private void assertTypeAndCollectedDataAuthorizationCalledForDeleteActionPartOfEnhance() {
		var permissionTerms = getAssertedCollectedPermissionTermsForRecordType(SOME_RECORD_TYPE,
				someDataGroup);
		authorizator.MCR.assertParameters("userIsAuthorizedForActionOnRecordTypeAndCollectedData",
				1, user, "delete", SOME_RECORD_TYPE, permissionTerms);
	}

	@Test
	public void testEnhanceIgnoreReadAccessDeleteActionPartOfEnhance() throws Exception {
		createRecordStorageSpy();

		DataRecord record = enhancer.enhanceIgnoringReadAccess(user, SOME_RECORD_TYPE,
				someDataGroup, dataRedactor);

		assertDeleteAction(record);
	}

	@Test
	public void testDeleteActionPartOfEnhanceNotAuthorized() throws Exception {
		setupForNoDeleteAccess();

		DataRecord record = enhancer.enhance(user, SOME_RECORD_TYPE, someDataGroup, dataRedactor);

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

		DataRecord record = enhancer.enhanceIgnoringReadAccess(user, SOME_RECORD_TYPE,
				someDataGroup, dataRedactor);

		assertDeleteActionNotAuthorized(record);
	}

	@Test
	public void testDeleteActionPartOfEnhanceAuthorizedButHasIncomingLinks() throws Exception {
		setupForDeleteButIncomingLinksExists();

		DataRecord record = enhancer.enhance(user, SOME_RECORD_TYPE, someDataGroup, dataRedactor);

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

		DataRecord record = enhancer.enhanceIgnoringReadAccess(user, SOME_RECORD_TYPE,
				someDataGroup, dataRedactor);

		assertDeleteActionAuthorizedButHasIncomingLinks(record);
	}

	@Test
	public void testDeleteActionPartOfEnhanceAuthorizedButHasIncomingLinksAsParentRecordType()
			throws Exception {
		setupForDeleteButIncomingLinksExistsForParentRecordType();

		DataRecord record = enhancer.enhance(user, SOME_RECORD_TYPE, someDataGroup, dataRedactor);

		assertDeleteActionAuthorizedButHasIncomingLinksAsParentRecordType(record);
	}

	private void setupForDeleteButIncomingLinksExistsForParentRecordType() {
		createRecordStorageSpy();
		recordTypeHandlerSpy.MRV.setDefaultReturnValuesSupplier("hasParent", () -> true);
		RecordStorageOldSpy recordStorageSpy = (RecordStorageOldSpy) dependencyProvider
				.getRecordStorage();
		recordStorageSpy.incomingLinksExistsForType.add("someParentId");
	}

	private void assertDeleteActionAuthorizedButHasIncomingLinksAsParentRecordType(
			DataRecord record) {
		assertTypeAndCollectedDataAuthorizationCalledForDeleteActionPartOfEnhance();
		RecordStorageOldSpy recordStorageSpy = (RecordStorageOldSpy) dependencyProvider
				.getRecordStorage();

		recordStorageSpy.MCR.assertParameters("linksExistForRecord", 0, SOME_RECORD_TYPE, "someId");

		recordTypeHandlerSpy.MCR.assertMethodWasCalled("hasParent");
		String parentIdFromRecordTypeHandler = (String) recordTypeHandlerSpy.MCR
				.getReturnValue("getParentId", 0);
		recordStorageSpy.MCR.assertParameters("linksExistForRecord", 1,
				parentIdFromRecordTypeHandler, "someId");
		recordStorageSpy.MCR.assertNumberOfCallsToMethod("linksExistForRecord", 2);

		assertRecordDoesNotContainDeleteAction(record);
	}

	@Test
	public void testEnhanceIgnoreReadAccessDeleteActionPartOfEnhanceAuthorizedButHasIncomingLinksAsParentRecordType()
			throws Exception {
		setupForDeleteButIncomingLinksExistsForParentRecordType();

		DataRecord record = enhancer.enhanceIgnoringReadAccess(user, SOME_RECORD_TYPE,
				someDataGroup, dataRedactor);

		assertDeleteActionAuthorizedButHasIncomingLinksAsParentRecordType(record);
	}

	@Test
	public void testIncomingLinksActionPartOfEnhanceHasNoIncommingLinks() throws Exception {
		createRecordStorageSpy();

		DataRecord record = enhancer.enhance(user, SOME_RECORD_TYPE, someDataGroup, dataRedactor);

		assertRecordDoesNotContainIncomingLinksAction(record);
	}

	private void assertRecordDoesNotContainIncomingLinksAction(DataRecord record) {
		assertFalse(record.getActions().contains(Action.READ_INCOMING_LINKS));
	}

	@Test
	public void testEnhanceIgnoreReadAccessIncomingLinksActionPartOfEnhanceHasNoIncommingLinks()
			throws Exception {
		createRecordStorageSpy();

		DataRecord record = enhancer.enhanceIgnoringReadAccess(user, SOME_RECORD_TYPE,
				someDataGroup, dataRedactor);

		assertRecordDoesNotContainIncomingLinksAction(record);
	}

	@Test
	public void testIncomingLinksActionPartOfEnhanceHasIncomingLinks() throws Exception {
		setupForDeleteButIncomingLinksExists();

		DataRecord record = enhancer.enhance(user, SOME_RECORD_TYPE, someDataGroup, dataRedactor);

		assertRecordContainsIncomingLinksAction(record);
	}

	private void assertRecordContainsIncomingLinksAction(DataRecord record) {
		assertTrue(record.getActions().contains(Action.READ_INCOMING_LINKS));
	}

	@Test
	public void testEnhanceIgnoreReadAccessIncomingLinksActionPartOfEnhanceHasIncomingLinks()
			throws Exception {
		setupForDeleteButIncomingLinksExists();

		DataRecord record = enhancer.enhanceIgnoringReadAccess(user, SOME_RECORD_TYPE,
				someDataGroup, dataRedactor);

		assertRecordContainsIncomingLinksAction(record);
	}

	@Test
	public void testUploadActionPartOfEnhance() throws Exception {
		setupForUploadAction();

		DataRecord record = enhancer.enhance(user, BINARY_RECORD_TYPE, someDataGroup, dataRedactor);

		assertUploadAction(record);
	}

	private void setupForUploadAction() {
		createRecordStorageSpy();
	}

	private void assertUploadAction(DataRecord record) {
		assertTypeAndCollectedDataAuthorizationCalledForUploadActionPartOfEnhance();
		assertRecordContainsUploadAction(record);
	}

	private void assertTypeAndCollectedDataAuthorizationCalledForUploadActionPartOfEnhance() {
		var permissionTerms = getAssertedCollectedPermissionTermsForRecordType(BINARY_RECORD_TYPE,
				someDataGroup);
		authorizator.MCR.assertParameters("userIsAuthorizedForActionOnRecordTypeAndCollectedData",
				2, user, "upload", BINARY_RECORD_TYPE, permissionTerms);
	}

	private void assertRecordContainsUploadAction(DataRecord record) {
		assertTrue(record.getActions().contains(Action.UPLOAD));
	}

	@Test
	public void testEnhanceIgnoreReadAccessUploadActionPartOfEnhance() throws Exception {
		setupForUploadAction();

		DataRecord record = enhancer.enhanceIgnoringReadAccess(user, BINARY_RECORD_TYPE,
				someDataGroup, dataRedactor);

		assertUploadAction(record);
	}

	@Test
	public void testUploadActionPartOfEnhanceNotAuthorized() throws Exception {
		setupForNoUploadAccess();

		DataRecord record = enhancer.enhance(user, BINARY_RECORD_TYPE, someDataGroup, dataRedactor);

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

		DataRecord record = enhancer.enhanceIgnoringReadAccess(user, BINARY_RECORD_TYPE,
				someDataGroup, dataRedactor);

		assertUploadActionNotAuthorized(record);
	}

	@Test
	public void testUploadActionPartOfEnhanceNotBinary() throws Exception {
		createRecordStorageSpy();

		DataRecord record = enhancer.enhance(user, SOME_RECORD_TYPE, someDataGroup, dataRedactor);

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

		DataRecord record = enhancer.enhanceIgnoringReadAccess(user, SOME_RECORD_TYPE,
				someDataGroup, dataRedactor);

		assertUploadActionNotChildOfBinary(record);
	}

	@Test
	public void testSearchActionPartOfEnhanceForRecordTypeSearch() throws Exception {
		setupForSearchAction();

		DataRecord record = enhancer.enhance(user, SOME_RECORD_TYPE, someDataGroup, dataRedactor);

		assertSearchActionForRecordTypeSearch(record);
	}

	private void setupForSearchAction() {
		createRecordStorageSpy();
		recordTypeHandlerSpy.representsTheRecordTypeDefiningSearches = true;
		addSearchChildsToSomeType();
	}

	private void addSearchChildsToSomeType() {
		DataGroup recordTypeToSearchIn1 = new DataGroupOldSpy("recordTypeToSearchIn");
		someDataGroup.addChild(recordTypeToSearchIn1);
		recordTypeToSearchIn1.addChild(new DataAtomicSpy("linkedRecordId", "linkedSearchId1"));
		DataGroup recordTypeToSearchIn2 = new DataGroupOldSpy("recordTypeToSearchIn");
		someDataGroup.addChild(recordTypeToSearchIn2);
		recordTypeToSearchIn2.addChild(new DataAtomicSpy("linkedRecordId", "linkedSearchId2"));
	}

	private void assertSearchActionForRecordTypeSearch(DataRecord record) {
		recordTypeHandlerSpy.MCR.assertMethodWasCalled("representsTheRecordTypeDefiningSearches");
		authorizator.MCR.assertParameters("userIsAuthorizedForActionOnRecordType", 0, user, SEARCH,
				"linkedSearchId1");
		authorizator.MCR.assertParameters("userIsAuthorizedForActionOnRecordType", 1, user, SEARCH,
				"linkedSearchId2");
		authorizator.MCR.assertNumberOfCallsToMethod("userIsAuthorizedForActionOnRecordType", 2);
		assertRecordContainsSearchAction(record);
	}

	private void assertRecordContainsSearchAction(DataRecord record) {
		assertTrue(record.getActions().contains(Action.SEARCH));
	}

	@Test
	public void testEnhanceIgnoreReadAccessSearchActionPartOfEnhanceForRecordTypeSearch()
			throws Exception {
		setupForSearchAction();

		DataRecord record = enhancer.enhanceIgnoringReadAccess(user, SOME_RECORD_TYPE,
				someDataGroup, dataRedactor);

		assertSearchActionForRecordTypeSearch(record);
	}

	@Test
	public void testSearchActionPartOfEnhanceForRecordTypeSearchNotAuthorized() throws Exception {
		setupForSearchActionNotAuthorized();

		DataRecord record = enhancer.enhance(user, SOME_RECORD_TYPE, someDataGroup, dataRedactor);

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

		DataRecord record = enhancer.enhanceIgnoringReadAccess(user, SOME_RECORD_TYPE,
				someDataGroup, dataRedactor);

		assertSearchActionForRecordTypeSearchNotAuthorized(record);
	}

	@Test
	public void testSearchActionPartOfEnhanceForRecordTypeSearchNoSearchType() throws Exception {
		createRecordStorageSpy();

		DataRecord record = enhancer.enhance(user, SOME_RECORD_TYPE, someDataGroup, dataRedactor);

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

		DataRecord record = enhancer.enhanceIgnoringReadAccess(user, SOME_RECORD_TYPE,
				someDataGroup, dataRedactor);

		assertSearchActionForRecordTypeSearchNoSearchType(record);
	}

	@Test
	public void testCreateActionPartOfEnhanceForRecordTypeRecordType() throws Exception {
		RecordTypeHandlerSpy recordTypeHandlerForRecordTypeInData = setupForCreateAction();

		DataRecord record = enhancer.enhance(user, SOME_RECORD_TYPE, someDataGroup, dataRedactor);

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
			RecordTypeHandlerSpy recordTypeHandlerForRecordTypeInData, DataRecord record) {
		recordTypeHandlerSpy.MCR
				.assertMethodWasCalled("representsTheRecordTypeDefiningRecordTypes");
		dependencyProvider.MCR.assertNumberOfCallsToMethod("getRecordTypeHandler", 2);
		dependencyProvider.MCR.assertParameters("getRecordTypeHandler", 1, "someId");

		authorizator.MCR.assertParameters("userIsAuthorizedForActionOnRecordType", 0, user, CREATE,
				"someId");
		assertRecordContainsCreateAction(record);
	}

	private void assertRecordContainsCreateAction(DataRecord record) {
		assertTrue(record.getActions().contains(Action.CREATE));
	}

	@Test
	public void testEnhanceIgnoreReadAccessCreateActionPartOfEnhanceForRecordTypeRecordType()
			throws Exception {
		RecordTypeHandlerSpy recordTypeHandlerForRecordTypeInData = setupForCreateAction();

		DataRecord record = enhancer.enhanceIgnoringReadAccess(user, SOME_RECORD_TYPE,
				someDataGroup, dataRedactor);

		assertCreateActionForRecordTypeRecordType(recordTypeHandlerForRecordTypeInData, record);
	}

	@Test
	public void testCreateActionPartOfEnhanceForRecordTypeRecordTypeNotAuthorized()
			throws Exception {
		setupForCreateActionNotAuthorized();

		DataRecord record = enhancer.enhance(user, SOME_RECORD_TYPE, someDataGroup, dataRedactor);

		assertCreateActionForRecordTypeRecordTypeNotAuthorized(record);
	}

	private void setupForCreateActionNotAuthorized() {
		createRecordStorageSpy();
		someDataGroup = createDummyDataGroupForRecord("otherId");
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

		DataRecord record = enhancer.enhanceIgnoringReadAccess(user, SOME_RECORD_TYPE,
				someDataGroup, dataRedactor);

		assertCreateActionForRecordTypeRecordTypeNotAuthorized(record);
	}

	@Test
	public void testCreateActionPartOfEnhanceForRecordTypeRecordTypeIsNotRecordType()
			throws Exception {
		createRecordStorageSpy();

		DataRecord record = enhancer.enhance(user, SOME_RECORD_TYPE, someDataGroup, dataRedactor);

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

		DataRecord record = enhancer.enhanceIgnoringReadAccess(user, SOME_RECORD_TYPE,
				someDataGroup, dataRedactor);

		assertCreateActionForRecordTypeRecordTypeIsNotRecordType(record);
	}

	@Test
	public void testListActionPartOfEnhanceForRecordTypeRecordType() throws Exception {
		setupForListAction();

		DataRecord record = enhancer.enhance(user, SOME_RECORD_TYPE, someDataGroup, dataRedactor);

		assertListActionForRecordTypeRecordType(record);
	}

	private void setupForListAction() {
		createRecordStorageSpy();
		recordTypeHandlerSpy.representsTheRecordTypeDefiningRecordTypes = true;
	}

	private void assertListActionForRecordTypeRecordType(DataRecord record) {
		recordTypeHandlerSpy.MCR
				.assertMethodWasCalled("representsTheRecordTypeDefiningRecordTypes");
		authorizator.MCR.assertParameters("userIsAuthorizedForActionOnRecordType", 1, user, LIST,
				"someId");
		assertRecordContainsListAction(record);
	}

	private void assertRecordContainsListAction(DataRecord record) {
		assertTrue(record.getActions().contains(Action.LIST));
	}

	@Test
	public void testEnhanceIgnoreReadAccessListActionPartOfEnhanceForRecordTypeRecordType()
			throws Exception {
		setupForListAction();

		DataRecord record = enhancer.enhanceIgnoringReadAccess(user, SOME_RECORD_TYPE,
				someDataGroup, dataRedactor);

		assertListActionForRecordTypeRecordType(record);
	}

	@Test
	public void testListActionPartOfEnhanceForRecordTypeRecordTypeNotAuthorized() throws Exception {
		setupForListActionNotAuthorized();

		DataRecord record = enhancer.enhance(user, SOME_RECORD_TYPE, someDataGroup, dataRedactor);

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

		DataRecord record = enhancer.enhanceIgnoringReadAccess(user, SOME_RECORD_TYPE,
				someDataGroup, dataRedactor);

		assertListActionForRecordTypeRecordTypeNotAuthorized(record);
	}

	@Test
	public void testListActionPartOfEnhanceForRecordTypeRecordTypeNotRecordType() throws Exception {
		createRecordStorageSpy();

		DataRecord record = enhancer.enhance(user, SOME_RECORD_TYPE, someDataGroup, dataRedactor);

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

		DataRecord record = enhancer.enhanceIgnoringReadAccess(user, SOME_RECORD_TYPE,
				someDataGroup, dataRedactor);

		assertListActionForRecordTypeRecordTypeNotRecordType(record);
	}

	@Test
	public void testValidateActionPartOfEnhanceForRecordTypeRecordType() throws Exception {
		setupForListAction();

		DataRecord record = enhancer.enhance(user, SOME_RECORD_TYPE, someDataGroup, dataRedactor);

		assertValidateActionForRecordTypeRecordType(record);
	}

	private void assertValidateActionForRecordTypeRecordType(DataRecord record) {
		recordTypeHandlerSpy.MCR
				.assertMethodWasCalled("representsTheRecordTypeDefiningRecordTypes");
		authorizator.MCR.assertParameters("userIsAuthorizedForActionOnRecordType", 2, user,
				"validate", "someId");
		assertTrue(record.getActions().contains(Action.VALIDATE));
	}

	@Test
	public void testEnhanceIgnoreReadAccessValidateActionPartOfEnhanceForRecordTypeRecordType()
			throws Exception {
		setupForListAction();

		DataRecord record = enhancer.enhanceIgnoringReadAccess(user, SOME_RECORD_TYPE,
				someDataGroup, dataRedactor);

		assertValidateActionForRecordTypeRecordType(record);
	}

	@Test
	public void testValidateActionPartOfEnhanceForRecordTypeRecordTypeNotAuthorized()
			throws Exception {
		setupForCreateActionNotAuthorized();

		DataRecord record = enhancer.enhance(user, SOME_RECORD_TYPE, someDataGroup, dataRedactor);

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

		DataRecord record = enhancer.enhanceIgnoringReadAccess(user, SOME_RECORD_TYPE,
				someDataGroup, dataRedactor);

		assertValidateActionForRecordTypeRecordTypeNotAuthorized(record);
	}

	@Test
	public void testValidateActionPartOfEnhanceForRecordTypeRecordTypeNotRecordType()
			throws Exception {
		createRecordStorageSpy();

		DataRecord record = enhancer.enhance(user, SOME_RECORD_TYPE, someDataGroup, dataRedactor);

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

		DataRecord record = enhancer.enhanceIgnoringReadAccess(user, SOME_RECORD_TYPE,
				someDataGroup, dataRedactor);

		assertValidateActionForRecordTypeRecordTypeNotRecordType(record);
	}

	@Test
	public void testSearchActionPartOfEnhancedWhenEnhancingDataGroupContainingRecordTypeRecord()
			throws Exception {
		RecordTypeHandlerSpy recordTypeHandlerForRecordTypeInData = setupForSearchActionWhenEnhancingTypeOfRecordType();

		DataRecord record = enhancer.enhance(user, SOME_RECORD_TYPE, someDataGroup, dataRedactor);

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
			RecordTypeHandlerSpy recordTypeHandlerForRecordTypeInData, DataRecord record) {
		recordTypeHandlerSpy.MCR
				.assertMethodWasCalled("representsTheRecordTypeDefiningRecordTypes");
		dependencyProvider.MCR.assertNumberOfCallsToMethod("getRecordTypeHandler", 3);
		dependencyProvider.MCR.assertParameters("getRecordTypeHandler", 1, "someId");
		dependencyProvider.MCR.assertReturn("getRecordTypeHandler", 1,
				recordTypeHandlerForRecordTypeInData);

		recordTypeHandlerForRecordTypeInData.MCR.assertMethodWasCalled("hasLinkedSearch");
		String returnedSearchId = (String) recordTypeHandlerForRecordTypeInData.MCR
				.getReturnValue("getSearchId", 0);

		RecordStorageOldSpy recordStorage = (RecordStorageOldSpy) dependencyProvider
				.getRecordStorage();
		RecordTypeHandlerSpy recordTypeHandlerX = (RecordTypeHandlerSpy) dependencyProvider.MCR
				.getReturnValue("getRecordTypeHandler", 2);
		var types = recordTypeHandlerX.MCR.getReturnValue("getListOfRecordTypeIdsToReadFromStorage",
				0);
		// List<?> types = (List<?>) recordStorage.MCR
		// .getValueForMethodNameAndCallNumberAndParameterName("read", 0, "type");

		// assertEquals(types.get(0), SEARCH);

		recordStorage.MCR.assertParameters("read", 0, types, returnedSearchId);

		recordStorage.MCR.assertParameter("read", 0, "id", returnedSearchId);
		authorizator.MCR.assertParameters("userIsAuthorizedForActionOnRecordType", 3, user, SEARCH,
				"linkedSearchId1");
		authorizator.MCR.assertParameters("userIsAuthorizedForActionOnRecordType", 4, user, SEARCH,
				"linkedSearchId2");
		authorizator.MCR.assertNumberOfCallsToMethod("userIsAuthorizedForActionOnRecordType", 6);
		assertRecordContainsSearchAction(record);
	}

	@Test
	public void testEnhanceIgnoreReadAccessSearchActionPartOfEnhanceForRecordTypeRecordType()
			throws Exception {
		RecordTypeHandlerSpy recordTypeHandlerForRecordTypeInData = setupForSearchActionWhenEnhancingTypeOfRecordType();

		DataRecord record = enhancer.enhanceIgnoringReadAccess(user, SOME_RECORD_TYPE,
				someDataGroup, dataRedactor);

		assertSearchActionForRecordTypeRecordType(recordTypeHandlerForRecordTypeInData, record);
	}

	@Test
	public void testSearchActionPartOfEnhanceForRecordTypeRecordTypeNotAuthorized()
			throws Exception {
		RecordTypeHandlerSpy recordTypeHandlerForRecordTypeInData = setupForSearchActionWhenEnhancingTypeOfRecordTypeNotAuthorized();

		DataRecord record = enhancer.enhance(user, SOME_RECORD_TYPE, someDataGroup, dataRedactor);

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
		dependencyProvider.MCR.assertNumberOfCallsToMethod("getRecordTypeHandler", 3);
		dependencyProvider.MCR.assertParameters("getRecordTypeHandler", 1, "someId");
		dependencyProvider.MCR.assertReturn("getRecordTypeHandler", 1,
				recordTypeHandlerForRecordTypeInData);

		recordTypeHandlerForRecordTypeInData.MCR.assertMethodWasCalled("hasLinkedSearch");
		String returnedSearchId = (String) recordTypeHandlerForRecordTypeInData.MCR
				.getReturnValue("getSearchId", 0);
		RecordStorageOldSpy recordStorage = (RecordStorageOldSpy) dependencyProvider
				.getRecordStorage();

		RecordTypeHandlerSpy recordTypeHandlerX = (RecordTypeHandlerSpy) dependencyProvider.MCR
				.getReturnValue("getRecordTypeHandler", 2);
		var types = recordTypeHandlerX.MCR.getReturnValue("getListOfRecordTypeIdsToReadFromStorage",
				0);

		recordStorage.MCR.assertParameters("read", 0, types, returnedSearchId);

		recordStorage.MCR.assertParameter("read", 0, "id", returnedSearchId);
		authorizator.MCR.assertParameters("userIsAuthorizedForActionOnRecordType", 3, user, SEARCH,
				"linkedSearchId1");
		authorizator.MCR.assertNumberOfCallsToMethod("userIsAuthorizedForActionOnRecordType", 5);
		assertRecordDoesNotContainSearchAction(record);
	}

	private void createAndSetReturnDataGroupForReadInStorageForReadOfLinkedSearch() {
		DataGroupOldSpy searchGroupLinkedFromRecordType = new DataGroupOldSpy("someSearchId");
		DataGroup recordTypeToSearchIn1 = new DataGroupOldSpy("recordTypeToSearchIn");
		searchGroupLinkedFromRecordType.addChild(recordTypeToSearchIn1);
		recordTypeToSearchIn1.addChild(new DataAtomicSpy("linkedRecordId", "linkedSearchId1"));
		DataGroup recordTypeToSearchIn2 = new DataGroupOldSpy("recordTypeToSearchIn");
		searchGroupLinkedFromRecordType.addChild(recordTypeToSearchIn2);
		recordTypeToSearchIn2.addChild(new DataAtomicSpy("linkedRecordId", "linkedSearchId2"));
		RecordStorageOldSpy recordStorage = (RecordStorageOldSpy) dependencyProvider
				.getRecordStorage();
		recordStorage.returnForRead = searchGroupLinkedFromRecordType;
	}

	@Test
	public void testEnhanceIgnoreReadAccessSearchActionPartOfEnhanceForRecordTypeRecordTypeNotAuthorized()
			throws Exception {
		RecordTypeHandlerSpy recordTypeHandlerForRecordTypeInData = setupForSearchActionWhenEnhancingTypeOfRecordTypeNotAuthorized();

		DataRecord record = enhancer.enhanceIgnoringReadAccess(user, SOME_RECORD_TYPE,
				someDataGroup, dataRedactor);

		assertSearchActionForRecordTypeRecordTypeNotAuthorized(recordTypeHandlerForRecordTypeInData,
				record);
	}

	@Test
	public void testReadPermissionsAreAddedToRecord() throws Exception {
		createRecordStorageSpy();

		DataRecord record = enhancer.enhance(user, SOME_RECORD_TYPE, someDataGroup, dataRedactor);

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

		DataRecord record = enhancer.enhanceIgnoringReadAccess(user, SOME_RECORD_TYPE,
				someDataGroup, dataRedactor);

		assertReadPermissionsAreAddedToRecord(record);
	}

	@Test
	public void testEnhanceIgnoreReadAccessReadPermissionsAreAddedToRecordWhenNoReadAccess()
			throws Exception {
		setupForNoReadAccess();

		DataRecord record = enhancer.enhanceIgnoringReadAccess(user, SOME_RECORD_TYPE,
				someDataGroup, dataRedactor);

		assertReadPermissionsAreAddedToRecordPublicData(record);
	}

	@Test
	public void testReadPermissionsAreAddedToRecordPublicData() throws Exception {
		setupForPublicRecord();

		DataRecord record = enhancer.enhance(user, SOME_RECORD_TYPE, someDataGroup, dataRedactor);

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

		DataRecord record = enhancer.enhanceIgnoringReadAccess(user, SOME_RECORD_TYPE,
				someDataGroup, dataRedactor);

		assertReadPermissionsAreAddedToRecordPublicData(record);
	}

	@Test
	public void testWritePermissionsAreAddedToRecord() throws Exception {
		createRecordStorageSpy();

		DataRecordOldSpy record = (DataRecordOldSpy) enhancer.enhance(user, SOME_RECORD_TYPE,
				someDataGroup, dataRedactor);

		assertWritePermissionsAreAddedToRecord(record);
	}

	private void assertWritePermissionsAreAddedToRecord(DataRecordOldSpy record) {
		authorizator.MCR.assertParameter(
				"checkGetUsersMatchedRecordPartPermissionsForActionOnRecordTypeAndCollectedData", 1,
				"action", UPDATE);
		authorizator.MCR.assertReturn(
				"checkGetUsersMatchedRecordPartPermissionsForActionOnRecordTypeAndCollectedData", 1,
				record.getWritePermissions());

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

		DataRecordOldSpy record = (DataRecordOldSpy) enhancer.enhanceIgnoringReadAccess(user,
				SOME_RECORD_TYPE, someDataGroup, dataRedactor);

		assertWritePermissionsAreAddedToRecord(record);
	}

	@Test
	public void testWritePermissionsAreAddedToRecordNotAutorized() throws Exception {
		setupForNoAccessOnUpdateActionOnRecordType();

		DataRecord record = enhancer.enhance(user, SOME_RECORD_TYPE, someDataGroup, dataRedactor);

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

		DataRecord record = enhancer.enhanceIgnoringReadAccess(user, SOME_RECORD_TYPE,
				someDataGroup, dataRedactor);

		assertWritePermissionsAreAddedToRecordNotAutorized(record);
	}

	@Test
	public void testRedactData() throws Exception {
		createRecordStorageSpy();

		DataRecord record = enhancer.enhance(user, SOME_RECORD_TYPE, someDataGroup, dataRedactor);

		assertRedactCalledWithCorrectArguments();
		assertAswerFromRedactorIsReturned(record);
	}

	private void assertRedactCalledWithCorrectArguments() {
		Set<?> usersReadRecordPartPermissions = (Set<?>) authorizator.MCR.getReturnValue(
				"checkGetUsersMatchedRecordPartPermissionsForActionOnRecordTypeAndCollectedData",
				0);

		Set<?> recordPartConstraints = (Set<?>) recordTypeHandlerSpy.MCR
				.getReturnValue("getReadRecordPartConstraints", 0);

		dataRedactor.MCR.assertParameters("removeChildrenForConstraintsWithoutPermissions", 0,
				recordTypeHandlerSpy.getDefinitionId(), someDataGroup, recordPartConstraints,
				usersReadRecordPartPermissions);
	}

	private void assertAswerFromRedactorIsReturned(DataRecord record) {
		DataGroup redactedDataGroup = (DataGroup) dataRedactor.MCR
				.getReturnValue("removeChildrenForConstraintsWithoutPermissions", 0);

		assertSame(record.getDataGroup(), redactedDataGroup);
	}

	@Test
	public void testEnhanceIgnoreReadAccessRedactData() throws Exception {
		createRecordStorageSpy();

		DataRecord record = enhancer.enhanceIgnoringReadAccess(user, SOME_RECORD_TYPE,
				someDataGroup, dataRedactor);

		assertRedactCalledWithCorrectArguments();
		assertAswerFromRedactorIsReturned(record);
	}

	@Test
	public void testEnhanceIgnoreReadAccessRedactDataIsCalledCorrectlyWhenNoAccess()
			throws Exception {
		setupForNoReadAccess();

		DataRecord record = enhancer.enhanceIgnoringReadAccess(user, SOME_RECORD_TYPE,
				someDataGroup, dataRedactor);

		Set<?> recordPartConstraints = (Set<?>) recordTypeHandlerSpy.MCR
				.getReturnValue("getReadRecordPartConstraints", 0);

		dataRedactor.MCR.assertParameters("removeChildrenForConstraintsWithoutPermissions", 0,
				recordTypeHandlerSpy.getDefinitionId(), someDataGroup, recordPartConstraints,
				Collections.emptySet());
		assertAswerFromRedactorIsReturned(record);
	}

	@Test
	public void testLinksAreAddedToRedactedDataGroup() throws Exception {
		setupReturnedDataGroupOnDataRedactorSpy();

		DataRecord record = enhancer.enhance(user, DATA_WITH_LINKS, someDataGroup, dataRedactor);

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

		DataRecord record = enhancer.enhanceIgnoringReadAccess(user, DATA_WITH_LINKS, someDataGroup,
				dataRedactor);

		RecordLinkTestsAsserter.assertTopLevelLinkContainsReadActionOnly(record);
	}

	@Test
	public void testReadRecordWithDataRecordLinkHasReadActionTopLevel() {
		DataGroup dataGroup = setupReturnedDataGroupOnDataRedactorSpy();

		DataRecord record = enhancer.enhance(user, DATA_WITH_LINKS, dataGroup, dataRedactor);

		RecordLinkTestsAsserter.assertTopLevelLinkContainsReadActionOnly(record);
	}

	@Test
	public void testEnhanceIgnoreReadAccessReadRecordWithDataRecordLinkHasReadActionTopLevel() {
		DataGroup dataGroup = setupReturnedDataGroupOnDataRedactorSpy();

		DataRecord record = enhancer.enhanceIgnoringReadAccess(user, DATA_WITH_LINKS, dataGroup,
				dataRedactor);

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

		DataRecord record = enhancer.enhance(user, DATA_WITH_LINKS, dataGroup, dataRedactor);
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

		DataRecord record = enhancer.enhance(user, DATA_WITH_LINKS, dataGroup, dataRedactor);
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

		DataRecord record = enhancer.enhanceIgnoringReadAccess(user, DATA_WITH_LINKS, dataGroup,
				dataRedactor);
		RecordLinkTestsAsserter.assertTopLevelLinkDoesNotContainReadAction(record);
	}

	@Test
	public void testReadRecordWithDataRecordLinkHasReadActionOneLevelDown() {
		DataGroup dataGroup = recordStorage.read(LIST_DATA_WITH_LINKS, "oneLinkOneLevelDown");
		dataRedactor.returnDataGroup = dataGroup;
		DataRecord record = enhancer.enhance(user, DATA_WITH_LINKS, dataGroup, dataRedactor);

		RecordLinkTestsAsserter.assertOneLevelDownLinkContainsReadActionOnly(record);
	}

	@Test
	public void testEnhanceIgnoreReadAccessReadRecordWithDataRecordLinkHasReadActionOneLevelDown() {
		DataGroup dataGroup = recordStorage.read(LIST_DATA_WITH_LINKS, "oneLinkOneLevelDown");
		dataRedactor.returnDataGroup = dataGroup;
		DataRecord record = enhancer.enhanceIgnoringReadAccess(user, DATA_WITH_LINKS, dataGroup,
				dataRedactor);

		RecordLinkTestsAsserter.assertOneLevelDownLinkContainsReadActionOnly(record);
	}

	@Test
	public void testReadRecordWithDataResourceLinkHasReadActionTopLevel() {
		String recordType = "dataWithResourceLinks";
		DataGroup dataGroup = recordStorage.read(List.of(recordType), "oneResourceLinkTopLevel");
		dataRedactor.returnDataGroup = dataGroup;
		DataRecord record = enhancer.enhance(user, recordType, dataGroup, dataRedactor);

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

		DataRecord record = enhancer.enhance(user, recordType, dataGroup, dataRedactor);

		authorizator.MCR.assertParameter("userIsAuthorizedForActionOnRecordTypeAndCollectedData", 2,
				"recordType", actionForResourceLink);
		RecordLinkTestsAsserter.assertTopLevelResourceLinkDoesNotContainReadAction(record);
	}

	@Test
	public void testEnhanceIgnoreReadAccessReadRecordWithDataResourceLinkHasReadActionTopLevel() {
		String recordType = "dataWithResourceLinks";
		DataGroup dataGroup = recordStorage.read(List.of(recordType), "oneResourceLinkTopLevel");
		dataRedactor.returnDataGroup = dataGroup;
		DataRecord record = enhancer.enhanceIgnoringReadAccess(user, recordType, dataGroup,
				dataRedactor);

		RecordLinkTestsAsserter.assertTopLevelResourceLinkContainsReadActionOnly(record);
	}

	@Test
	public void testReadRecordWithDataResourceLinkHasReadActionOneLevelDown() {
		String recordType = "dataWithResourceLinks";
		DataGroup dataGroup = recordStorage.read(List.of(recordType),
				"oneResourceLinkOneLevelDown");
		dataRedactor.returnDataGroup = dataGroup;

		DataRecord record = enhancer.enhance(user, recordType, dataGroup, dataRedactor);

		RecordLinkTestsAsserter.assertOneLevelDownResourceLinkContainsReadActionOnly(record);
	}

	@Test
	public void testEnhanceIgnoreReadAccessReadRecordWithDataResourceLinkHasReadActionOneLevelDown() {
		String recordType = "dataWithResourceLinks";
		DataGroup dataGroup = recordStorage.read(List.of(recordType),
				"oneResourceLinkOneLevelDown");
		dataRedactor.returnDataGroup = dataGroup;

		DataRecord record = enhancer.enhanceIgnoringReadAccess(user, recordType, dataGroup,
				dataRedactor);

		RecordLinkTestsAsserter.assertOneLevelDownResourceLinkContainsReadActionOnly(record);
	}

	@Test
	public void testReadRecordWithDataRecordLinkTargetDoesNotExist() {
		DataGroup dataGroup = recordStorage.read(LIST_DATA_WITH_LINKS,
				"oneLinkOneLevelDownTargetDoesNotExist");
		dataRedactor.returnDataGroup = dataGroup;

		DataRecord record = enhancer.enhance(user, DATA_WITH_LINKS, someDataGroup, dataRedactor);

		assertReadRecordWithDataRecordLinkTargetDoesNotExist(record);
	}

	private void assertReadRecordWithDataRecordLinkTargetDoesNotExist(DataRecord record) {
		DataGroup recordDataGroup = record.getDataGroup();
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

		DataRecord record = enhancer.enhanceIgnoringReadAccess(user, DATA_WITH_LINKS, someDataGroup,
				dataRedactor);

		assertReadRecordWithDataRecordLinkTargetDoesNotExist(record);
	}

	@Test
	public void testLinkIsNotReadWhenRecordTypeIsPublic() {
		recordTypeHandlerSpy.isPublicForRead = true;
		DataGroup dataGroup = recordStorage.read(LIST_DATA_WITH_LINKS,
				"oneLinkTopLevelNotAuthorized");
		dataRedactor.returnDataGroup = dataGroup;
		recordStorage.publicReadForToRecordType = "true";

		DataRecord record = enhancer.enhance(user, DATA_WITH_LINKS, someDataGroup, dataRedactor);

		assertLinkIsNotReadWhenRecordTypeIsPublic(record);
	}

	private void assertLinkIsNotReadWhenRecordTypeIsPublic(DataRecord record) {
		RecordLinkTestsAsserter.assertTopLevelLinkContainsReadActionOnly(record);
		assertRecordStorageWasNOTCalledForReadKey(recordStorage,
				"toRecordType:recordLinkNotAuthorized");

		dependencyProvider.MCR.assertParameters("getRecordTypeHandler", 0, DATA_WITH_LINKS);
		dependencyProvider.MCR.assertParameters("getRecordTypeHandler", 1, "toRecordType");
		dependencyProvider.MCR.assertParameters("getRecordTypeHandler", 2, "recordType");
		dependencyProvider.MCR.assertParameters("getRecordTypeHandler", 3, "system");
		dependencyProvider.MCR.assertNumberOfCallsToMethod("getRecordTypeHandler", 4);
	}

	@Test
	public void testEnhanceIgnoreReadAccessLinkIsNotReadWhenRecordTypeIsPublic() {
		recordTypeHandlerSpy.isPublicForRead = true;
		DataGroup dataGroup = recordStorage.read(LIST_DATA_WITH_LINKS,
				"oneLinkTopLevelNotAuthorized");
		dataRedactor.returnDataGroup = dataGroup;
		recordStorage.publicReadForToRecordType = "true";

		DataRecord record = enhancer.enhanceIgnoringReadAccess(user, DATA_WITH_LINKS, someDataGroup,
				dataRedactor);

		assertLinkIsNotReadWhenRecordTypeIsPublic(record);
	}

	@Test
	public void testRecordTypeForLinkIsOnlyReadOnce() {
		recordTypeHandlerSpy.isPublicForRead = true;
		recordStorage.publicReadForToRecordType = "true";
		DataGroup dataGroup = recordStorage.read(LIST_DATA_WITH_LINKS, "twoLinksTopLevel");
		dataRedactor.returnDataGroup = dataGroup;

		DataRecord record = enhancer.enhance(user, DATA_WITH_LINKS, someDataGroup, dataRedactor);

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

		DataRecord record = enhancer.enhanceIgnoringReadAccess(user, DATA_WITH_LINKS, someDataGroup,
				dataRedactor);

		assertRecordTypeForLinkIsOnlyReadOnce(record);
	}

	@Test
	public void testLinkedRecordForLinkIsOnlyReadOnceForSameLinkedRecord() {
		DataGroup dataGroup = recordStorage.read(LIST_DATA_WITH_LINKS, "twoLinksTopLevel");
		dataRedactor.returnDataGroup = dataGroup;

		DataRecord record = enhancer.enhance(user, DATA_WITH_LINKS, someDataGroup, dataRedactor);

		assertTopLevelTwoLinksContainReadActionOnly(record);
		assertLinkedRecordForLinkIsOnlyReadOnceForSameLinkedRecord(record);
	}

	private void assertLinkedRecordForLinkIsOnlyReadOnceForSameLinkedRecord(DataRecord record) {
		dependencyProvider.MCR.assertNumberOfCallsToMethod("getRecordTypeHandler", 4);
		dependencyProvider.MCR.assertParameters("getRecordTypeHandler", 0, "dataWithLinks");
		dependencyProvider.MCR.assertParameters("getRecordTypeHandler", 1, "recordType");
		dependencyProvider.MCR.assertParameters("getRecordTypeHandler", 2, "system");
		dependencyProvider.MCR.assertParameters("getRecordTypeHandler", 3, "toRecordType");

		RecordTypeHandlerSpy recordTypeHandlerX = (RecordTypeHandlerSpy) dependencyProvider.MCR
				.getReturnValue("getRecordTypeHandler", 3);

		recordTypeHandlerX.MCR
				.assertNumberOfCallsToMethod("getListOfRecordTypeIdsToReadFromStorage", 3);

		var types = recordTypeHandlerX.MCR.getReturnValue("getListOfRecordTypeIdsToReadFromStorage",
				2);

		recordStorage.MCR.assertNumberOfCallsToMethod("read", 4);
		recordStorage.MCR.assertParameter("read", 0, "id", "twoLinksTopLevel");
		recordStorage.MCR.assertParameter("read", 1, "id", "dataWithLinks");
		recordStorage.MCR.assertParameter("read", 2, "id", "cora");
		recordStorage.MCR.assertParameter("read", 3, "id", "toRecordId");
		recordStorage.MCR.assertParameters("read", 3, types, "toRecordId");

		// String metadataId = (String) recordTypeHandlerSpy.MCR.getReturnValue("getMetadataId", 0);
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

		DataRecord record = enhancer.enhance(user, DATA_WITH_LINKS, someDataGroup, dataRedactor);

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

		DataRecord record = enhancer.enhanceIgnoringReadAccess(user, DATA_WITH_LINKS, someDataGroup,
				dataRedactor);
		assertTopLevelTwoLinksContainReadActionOnly(record);
		assertLinkedRecordForLinkIsOnlyReadOnceForSameLinkedRecord(record);
	}

	@Test
	public void testLinkedRecordForLinkIsOnlyReadOnceForSameLinkedRecordNotAuthorized() {
		DataGroup dataGroup = recordStorage.read(LIST_DATA_WITH_LINKS, "twoLinksTopLevel");
		dataRedactor.returnDataGroup = dataGroup;

		authorizator.setNotAutorizedForActionOnRecordType(READ, "system");
		authorizator.setNotAutorizedForActionOnRecordType(READ, "toRecordType");

		DataRecord record = enhancer.enhance(user, DATA_WITH_LINKS, dataGroup, dataRedactor);

		assertTopLevelTwoLinksDoesNotContainReadAction(record);
		assertLinkedRecordForLinkIsOnlyReadOnceForSameLinkedRecord(record);
	}

	@Test
	public void testEnhanceIgnoreReadAccessLinkedRecordForLinkIsOnlyReadOnceForSameLinkedRecordNotAuthorized() {
		DataGroup dataGroup = recordStorage.read(LIST_DATA_WITH_LINKS, "twoLinksTopLevel");
		dataRedactor.returnDataGroup = dataGroup;

		authorizator.setNotAutorizedForActionOnRecordType(READ, "system");
		authorizator.setNotAutorizedForActionOnRecordType(READ, "toRecordType");

		DataRecord record = enhancer.enhanceIgnoringReadAccess(user, DATA_WITH_LINKS, dataGroup,
				dataRedactor);

		assertTopLevelTwoLinksDoesNotContainReadAction(record);
		assertLinkedRecordForLinkIsOnlyReadOnceForSameLinkedRecord(record);
	}
}
