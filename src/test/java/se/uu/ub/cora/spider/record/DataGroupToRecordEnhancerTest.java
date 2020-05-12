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

package se.uu.ub.cora.spider.record;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertNotNull;
import static org.testng.Assert.assertSame;
import static org.testng.Assert.assertTrue;
import static se.uu.ub.cora.spider.record.RecordLinkTestsAsserter.assertRecordStorageWasCalledOnlyOnceForReadKey;
import static se.uu.ub.cora.spider.record.RecordLinkTestsAsserter.assertRecordStorageWasNOTCalledForReadKey;
import static se.uu.ub.cora.spider.record.RecordLinkTestsAsserter.assertTopLevelTwoLinksContainReadActionOnly;
import static se.uu.ub.cora.spider.record.RecordLinkTestsAsserter.assertTopLevelTwoLinksDoesNotContainReadAction;

import java.util.Collections;
import java.util.HashMap;
import java.util.Set;

import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import se.uu.ub.cora.beefeater.authentication.User;
import se.uu.ub.cora.data.Action;
import se.uu.ub.cora.data.DataAtomicFactory;
import se.uu.ub.cora.data.DataAtomicProvider;
import se.uu.ub.cora.data.DataGroup;
import se.uu.ub.cora.data.DataGroupFactory;
import se.uu.ub.cora.data.DataGroupProvider;
import se.uu.ub.cora.data.DataLink;
import se.uu.ub.cora.data.DataRecord;
import se.uu.ub.cora.data.DataRecordFactory;
import se.uu.ub.cora.data.DataRecordProvider;
import se.uu.ub.cora.logger.LoggerProvider;
import se.uu.ub.cora.spider.authentication.AuthenticatorSpy;
import se.uu.ub.cora.spider.authorization.AuthorizationException;
import se.uu.ub.cora.spider.authorization.PermissionRuleCalculator;
import se.uu.ub.cora.spider.data.DataAtomicFactorySpy;
import se.uu.ub.cora.spider.data.DataAtomicSpy;
import se.uu.ub.cora.spider.data.DataGroupFactorySpy;
import se.uu.ub.cora.spider.data.DataGroupSpy;
import se.uu.ub.cora.spider.data.DataRecordFactorySpy;
import se.uu.ub.cora.spider.dependency.RecordStorageProviderSpy;
import se.uu.ub.cora.spider.dependency.RecordTypeHandlerSpy;
import se.uu.ub.cora.spider.dependency.SpiderDependencyProviderSpy;
import se.uu.ub.cora.spider.log.LoggerFactorySpy;
import se.uu.ub.cora.spider.spy.DataGroupTermCollectorSpy;
import se.uu.ub.cora.spider.spy.RuleCalculatorSpy;
import se.uu.ub.cora.spider.spy.SpiderAuthorizatorSpy;

public class DataGroupToRecordEnhancerTest {
	private static final String UPDATE = "update";
	private static final String LIST = "list";
	private static final String DATA_WITH_LINKS = "dataWithLinks";
	private static final String CREATE = "create";
	private static final String SEARCH = "search";
	private static final String READ = "read";
	private static final String SOME_RECORD_TYPE = "someRecordType";
	private RecordEnhancerTestsRecordStorage recordStorage;
	private AuthenticatorSpy authenticator;
	private SpiderAuthorizatorSpy authorizator;
	private PermissionRuleCalculator ruleCalculator;
	private SpiderDependencyProviderSpy dependencyProvider;
	private User user;
	private DataGroupToRecordEnhancer enhancer;
	private DataGroupTermCollectorSpy termCollector;
	private LoggerFactorySpy loggerFactorySpy;
	private DataGroupFactory dataGroupFactorySpy;
	private DataAtomicFactory dataAtomicFactorySpy;
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
		authenticator = new AuthenticatorSpy();
		authorizator = new SpiderAuthorizatorSpy();
		ruleCalculator = new RuleCalculatorSpy();
		termCollector = new DataGroupTermCollectorSpy();
		dataRedactor = new DataRedactorSpy();
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
	}

	private void setUpDependencyProvider() {
		dependencyProvider = new SpiderDependencyProviderSpy(new HashMap<>());
		dependencyProvider.authenticator = authenticator;
		dependencyProvider.spiderAuthorizator = authorizator;
		RecordStorageProviderSpy recordStorageProviderSpy = new RecordStorageProviderSpy();
		recordStorageProviderSpy.recordStorage = recordStorage;
		dependencyProvider.setRecordStorageProvider(recordStorageProviderSpy);
		dependencyProvider.ruleCalculator = ruleCalculator;
		dependencyProvider.termCollector = termCollector;
		dependencyProvider.dataRedactor = dataRedactor;
		recordTypeHandlerSpy = dependencyProvider.recordTypeHandlerSpy;
		enhancer = new DataGroupToRecordEnhancerImp(dependencyProvider);
	}

	private DataGroup createDummyDataGroupForRecord(String id) {
		DataGroup dataGroup = new DataGroupSpy("someNameInData");
		DataGroup recordInfo = new DataGroupSpy("recordInfo");
		dataGroup.addChild(recordInfo);
		recordInfo.addChild(new DataAtomicSpy("id", id));
		DataGroup type = new DataGroupSpy("type");
		recordInfo.addChild(type);
		type.addChild(new DataAtomicSpy("linkedRecordId", "someLinkedRecordId"));
		return dataGroup;
	}

	@Test
	public void testReadActionPartOfEnhance() throws Exception {
		createRecordStorageSpy();

		DataRecord record = enhancer.enhance(user, SOME_RECORD_TYPE, someDataGroup);

		assertReadActionPartOfEnhance(record);
	}

	private void assertReadActionPartOfEnhance(DataRecord record) {
		assertCheckAndGetAuthorizationCalledForReadActionPartOfEnhance();
		assertRecordContainsReadAction(record);
	}

	private void assertCheckAndGetAuthorizationCalledForReadActionPartOfEnhance() {
		DataGroup returnedCollectedTermsForRecordType = getAssertedCollectedTermsForRecordType(
				SOME_RECORD_TYPE, someDataGroup);
		authorizator.MCR.assertParameters(
				"checkGetUsersMatchedRecordPartPermissionsForActionOnRecordTypeAndCollectedData", 0,
				user, READ, SOME_RECORD_TYPE, returnedCollectedTermsForRecordType, true);
	}

	private void assertRecordContainsReadAction(DataRecord record) {
		assertTrue(record.getActions().contains(Action.READ));
	}

	private RecordStorageSpy createRecordStorageSpy() {
		RecordStorageSpy recordStorageSpy = new RecordStorageSpy();
		RecordStorageProviderSpy recordStorageProviderSpy = new RecordStorageProviderSpy();
		recordStorageProviderSpy.recordStorage = recordStorageSpy;
		dependencyProvider.setRecordStorageProvider(recordStorageProviderSpy);
		enhancer = new DataGroupToRecordEnhancerImp(dependencyProvider);
		return recordStorageSpy;
	}

	private DataGroup getAssertedCollectedTermsForRecordType(String recordType,
			DataGroup dataGroup) {
		dependencyProvider.MCR.assertParameters("getRecordTypeHandler", 0, recordType);
		RecordTypeHandlerSpy recordTypeHandler = (RecordTypeHandlerSpy) dependencyProvider.MCR
				.getReturnValue("getRecordTypeHandler", 0);

		recordTypeHandler.MCR.assertMethodWasCalled("getMetadataId");
		String metadataIdFromRecordTypeHandler = (String) recordTypeHandler.MCR
				.getReturnValue("getMetadataId", 0);

		dependencyProvider.MCR.assertParameters("getDataGroupTermCollector", 0);
		DataGroupTermCollectorSpy termCollectorSpy = (DataGroupTermCollectorSpy) dependencyProvider.MCR
				.getReturnValue("getDataGroupTermCollector", 0);

		termCollectorSpy.MCR.assertParameters("collectTerms", 0, metadataIdFromRecordTypeHandler,
				dataGroup);
		DataGroup returnedCollectedTermsForRecordType = (DataGroup) termCollectorSpy.MCR
				.getReturnValue("collectTerms", 0);

		return returnedCollectedTermsForRecordType;
	}

	@Test
	public void testIRAReadActionPartOfEnhance() throws Exception {
		createRecordStorageSpy();

		DataRecord record = enhancer.enhanceIgnoringReadAccess(user, SOME_RECORD_TYPE,
				someDataGroup);

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
			enhancer.enhance(user, SOME_RECORD_TYPE, someDataGroup);
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
	public void testIRAReadActionPartOfEnhanceNotAuthorized() throws Exception {
		setupForNoReadAccess();

		DataRecord record = enhancer.enhanceIgnoringReadAccess(user, SOME_RECORD_TYPE,
				someDataGroup);

		assertCheckAndGetAuthorizationCalledForReadActionPartOfEnhance();

		assertRecordDoesNotContainReadAction(record);
	}

	private void assertRecordDoesNotContainReadAction(DataRecord record) {
		assertFalse(record.getActions().contains(Action.READ));
	}

	@Test
	public void testReadActionPartOfEnhanceNotAuthorizedButPublicData() throws Exception {
		setupForNoReadAccessButPublicData();

		DataRecord record = enhancer.enhance(user, SOME_RECORD_TYPE, someDataGroup);

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
	public void testIRAReadActionPartOfEnhanceNotAuthorizedButPublicData() throws Exception {
		setupForNoReadAccessButPublicData();

		DataRecord record = enhancer.enhanceIgnoringReadAccess(user, SOME_RECORD_TYPE,
				someDataGroup);

		assertReadActionPartOfEnhanceNotAuthorizedButPublicData(record);
	}

	@Test
	public void testUpdateActionPartOfEnhance() throws Exception {
		createRecordStorageSpy();

		DataRecord record = enhancer.enhance(user, SOME_RECORD_TYPE, someDataGroup);

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
		DataGroup returnedCollectedTermsForRecordType = getAssertedCollectedTermsForRecordType(
				SOME_RECORD_TYPE, someDataGroup);
		authorizator.MCR.assertParameters(
				"checkGetUsersMatchedRecordPartPermissionsForActionOnRecordTypeAndCollectedData", 1,
				user, UPDATE, SOME_RECORD_TYPE, returnedCollectedTermsForRecordType, true);
	}

	@Test
	public void testIRAUpdateActionPartOfEnhance() throws Exception {
		createRecordStorageSpy();

		DataRecord record = enhancer.enhanceIgnoringReadAccess(user, SOME_RECORD_TYPE,
				someDataGroup);

		assertUpdateActionPartOfEnhance(record);
	}

	@Test
	public void testUpdateActionPartOfEnhanceNotAuthorized() throws Exception {
		setupForNoUpdateAccess();

		DataRecord record = enhancer.enhance(user, SOME_RECORD_TYPE, someDataGroup);

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
	public void testIRAUpdateActionPartOfEnhanceNotAuthorized() throws Exception {
		setupForNoUpdateAccess();

		DataRecord record = enhancer.enhanceIgnoringReadAccess(user, SOME_RECORD_TYPE,
				someDataGroup);

		assertUpdateActionPartOfEnhanceNotAuthorized(record);
	}

	@Test
	public void testIndexActionPartOfEnhance() throws Exception {
		createRecordStorageSpy();

		DataRecord record = enhancer.enhance(user, SOME_RECORD_TYPE, someDataGroup);

		assertIndexAction(record);
	}

	private void assertIndexAction(DataRecord record) {
		assertTypeAndCollectedDataAuthorizationCalledForIndexActionPartOfEnhance();
		assertRecordContainsIndexAction(record);
	}

	private void assertTypeAndCollectedDataAuthorizationCalledForIndexActionPartOfEnhance() {
		DataGroup returnedCollectedTermsForRecordType = getAssertedCollectedTermsForRecordType(
				SOME_RECORD_TYPE, someDataGroup);
		authorizator.MCR.assertParameters("userIsAuthorizedForActionOnRecordTypeAndCollectedData",
				0, user, "index", SOME_RECORD_TYPE, returnedCollectedTermsForRecordType);
	}

	private void assertRecordContainsIndexAction(DataRecord record) {
		assertTrue(record.getActions().contains(Action.INDEX));
	}

	@Test
	public void testIRAIndexActionPartOfEnhance() throws Exception {
		createRecordStorageSpy();

		DataRecord record = enhancer.enhanceIgnoringReadAccess(user, SOME_RECORD_TYPE,
				someDataGroup);

		assertIndexAction(record);
	}

	@Test
	public void testIndexActionPartOfEnhanceNotAuthorized() throws Exception {
		setupForNoIndexAccess();

		DataRecord record = enhancer.enhance(user, SOME_RECORD_TYPE, someDataGroup);

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
	public void testIRAIndexActionPartOfEnhanceNotAuthorized() throws Exception {
		setupForNoIndexAccess();

		DataRecord record = enhancer.enhanceIgnoringReadAccess(user, SOME_RECORD_TYPE,
				someDataGroup);

		assertIndexActionNotAuthorized(record);
	}

	@Test
	public void testDeleteActionPartOfEnhance() throws Exception {
		createRecordStorageSpy();

		DataRecord record = enhancer.enhance(user, SOME_RECORD_TYPE, someDataGroup);

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
		DataGroup returnedCollectedTermsForRecordType = getAssertedCollectedTermsForRecordType(
				SOME_RECORD_TYPE, someDataGroup);
		authorizator.MCR.assertParameters("userIsAuthorizedForActionOnRecordTypeAndCollectedData",
				1, user, "delete", SOME_RECORD_TYPE, returnedCollectedTermsForRecordType);
	}

	@Test
	public void testIRADeleteActionPartOfEnhance() throws Exception {
		createRecordStorageSpy();

		DataRecord record = enhancer.enhanceIgnoringReadAccess(user, SOME_RECORD_TYPE,
				someDataGroup);

		assertDeleteAction(record);
	}

	@Test
	public void testDeleteActionPartOfEnhanceNotAuthorized() throws Exception {
		setupForNoDeleteAccess();

		DataRecord record = enhancer.enhance(user, SOME_RECORD_TYPE, someDataGroup);

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
	public void testIRADeleteActionPartOfEnhanceNotAuthorized() throws Exception {
		setupForNoDeleteAccess();

		DataRecord record = enhancer.enhanceIgnoringReadAccess(user, SOME_RECORD_TYPE,
				someDataGroup);

		assertDeleteActionNotAuthorized(record);
	}

	@Test
	public void testDeleteActionPartOfEnhanceAuthorizedButHasIncomingLinks() throws Exception {
		setupForDeleteButIncomingLinksExists();

		DataRecord record = enhancer.enhance(user, SOME_RECORD_TYPE, someDataGroup);

		assertDeleteActionAuthorizedButHasIncomingLinks(record);
	}

	private void setupForDeleteButIncomingLinksExists() {
		RecordStorageSpy recordStorageSpy = createRecordStorageSpy();
		recordStorageSpy.incomingLinksExistsForType.add(SOME_RECORD_TYPE);
	}

	private void assertDeleteActionAuthorizedButHasIncomingLinks(DataRecord record) {
		assertTypeAndCollectedDataAuthorizationCalledForDeleteActionPartOfEnhance();

		RecordStorageSpy recordStorageSpy = (RecordStorageSpy) dependencyProvider
				.getRecordStorage();
		recordStorageSpy.MCR.assertNumberOfCallsToMethod("linksExistForRecord", 1);
		recordStorageSpy.MCR.assertParameters("linksExistForRecord", 0, SOME_RECORD_TYPE, "someId");

		assertRecordDoesNotContainDeleteAction(record);
	}

	@Test
	public void testIRADeleteActionPartOfEnhanceAuthorizedButHasIncomingLinks() throws Exception {
		setupForDeleteButIncomingLinksExists();

		DataRecord record = enhancer.enhanceIgnoringReadAccess(user, SOME_RECORD_TYPE,
				someDataGroup);

		assertDeleteActionAuthorizedButHasIncomingLinks(record);
	}

	@Test
	public void testDeleteActionPartOfEnhanceAuthorizedButHasIncomingLinksAsParentRecordType()
			throws Exception {
		setupForDeleteButIncomingLinksExistsForParentRecordType();

		DataRecord record = enhancer.enhance(user, SOME_RECORD_TYPE, someDataGroup);

		assertDeleteActionAuthorizedButHasIncomingLinksAsParentRecordType(record);
	}

	private void setupForDeleteButIncomingLinksExistsForParentRecordType() {
		createRecordStorageSpy();
		recordTypeHandlerSpy.hasParent = true;
		RecordStorageSpy recordStorageSpy = (RecordStorageSpy) dependencyProvider
				.getRecordStorage();
		recordStorageSpy.incomingLinksExistsForType.add("someParentId");
	}

	private void assertDeleteActionAuthorizedButHasIncomingLinksAsParentRecordType(
			DataRecord record) {
		assertTypeAndCollectedDataAuthorizationCalledForDeleteActionPartOfEnhance();
		RecordStorageSpy recordStorageSpy = (RecordStorageSpy) dependencyProvider
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
	public void testIRADeleteActionPartOfEnhanceAuthorizedButHasIncomingLinksAsParentRecordType()
			throws Exception {
		setupForDeleteButIncomingLinksExistsForParentRecordType();

		DataRecord record = enhancer.enhanceIgnoringReadAccess(user, SOME_RECORD_TYPE,
				someDataGroup);

		assertDeleteActionAuthorizedButHasIncomingLinksAsParentRecordType(record);
	}

	@Test
	public void testIncomingLinksActionPartOfEnhanceHasNoIncommingLinks() throws Exception {
		createRecordStorageSpy();

		DataRecord record = enhancer.enhance(user, SOME_RECORD_TYPE, someDataGroup);

		assertRecordDoesNotContainIncomingLinksAction(record);
	}

	private void assertRecordDoesNotContainIncomingLinksAction(DataRecord record) {
		assertFalse(record.getActions().contains(Action.READ_INCOMING_LINKS));
	}

	@Test
	public void testIRAIncomingLinksActionPartOfEnhanceHasNoIncommingLinks() throws Exception {
		createRecordStorageSpy();

		DataRecord record = enhancer.enhanceIgnoringReadAccess(user, SOME_RECORD_TYPE,
				someDataGroup);

		assertRecordDoesNotContainIncomingLinksAction(record);
	}

	@Test
	public void testIncomingLinksActionPartOfEnhanceHasIncomingLinks() throws Exception {
		setupForDeleteButIncomingLinksExists();

		DataRecord record = enhancer.enhance(user, SOME_RECORD_TYPE, someDataGroup);

		assertRecordContainsIncomingLinksAction(record);
	}

	private void assertRecordContainsIncomingLinksAction(DataRecord record) {
		assertTrue(record.getActions().contains(Action.READ_INCOMING_LINKS));
	}

	@Test
	public void testIRAIncomingLinksActionPartOfEnhanceHasIncomingLinks() throws Exception {
		setupForDeleteButIncomingLinksExists();

		DataRecord record = enhancer.enhanceIgnoringReadAccess(user, SOME_RECORD_TYPE,
				someDataGroup);

		assertRecordContainsIncomingLinksAction(record);
	}

	@Test
	public void testUploadActionPartOfEnhance() throws Exception {
		setupForUploadAction();

		DataRecord record = enhancer.enhance(user, SOME_RECORD_TYPE, someDataGroup);

		assertUploadAction(record);
	}

	private void setupForUploadAction() {
		createRecordStorageSpy();
		recordTypeHandlerSpy.isChildOfBinary = true;
	}

	private void assertUploadAction(DataRecord record) {
		recordTypeHandlerSpy.MCR.assertMethodWasCalled("isChildOfBinary");

		assertTypeAndCollectedDataAuthorizationCalledForUploadActionPartOfEnhance();
		assertRecordContainsUploadAction(record);
	}

	private void assertTypeAndCollectedDataAuthorizationCalledForUploadActionPartOfEnhance() {
		DataGroup returnedCollectedTermsForRecordType = getAssertedCollectedTermsForRecordType(
				SOME_RECORD_TYPE, someDataGroup);
		authorizator.MCR.assertParameters("userIsAuthorizedForActionOnRecordTypeAndCollectedData",
				2, user, "upload", SOME_RECORD_TYPE, returnedCollectedTermsForRecordType);
	}

	private void assertRecordContainsUploadAction(DataRecord record) {
		assertTrue(record.getActions().contains(Action.UPLOAD));
	}

	@Test
	public void testIRAUploadActionPartOfEnhance() throws Exception {
		setupForUploadAction();

		DataRecord record = enhancer.enhanceIgnoringReadAccess(user, SOME_RECORD_TYPE,
				someDataGroup);

		assertUploadAction(record);
	}

	@Test
	public void testUploadActionPartOfEnhanceNotAuthorized() throws Exception {
		setupForNoUploadAccess();

		DataRecord record = enhancer.enhance(user, SOME_RECORD_TYPE, someDataGroup);

		assertUploadActionNotAuthorized(record);
	}

	private void setupForNoUploadAccess() {
		setupForUploadAction();
		authorizator.setNotAutorizedForAction("upload");
	}

	private void assertUploadActionNotAuthorized(DataRecord record) {
		recordTypeHandlerSpy.MCR.assertMethodWasCalled("isChildOfBinary");

		assertTypeAndCollectedDataAuthorizationCalledForUploadActionPartOfEnhance();
		assertRecordDoesNotContainUploadAction(record);

		authorizator.MCR.assertNumberOfCallsToMethod(
				"userIsAuthorizedForActionOnRecordTypeAndCollectedData", 3);
	}

	@Test
	public void testIRAUploadActionPartOfEnhanceNotAuthorized() throws Exception {
		setupForNoUploadAccess();

		DataRecord record = enhancer.enhanceIgnoringReadAccess(user, SOME_RECORD_TYPE,
				someDataGroup);

		assertUploadActionNotAuthorized(record);
	}

	@Test
	public void testUploadActionPartOfEnhanceNotChildOfBinary() throws Exception {
		createRecordStorageSpy();

		DataRecord record = enhancer.enhance(user, SOME_RECORD_TYPE, someDataGroup);

		assertUploadActionNotChildOfBinary(record);
	}

	private void assertUploadActionNotChildOfBinary(DataRecord record) {
		recordTypeHandlerSpy.MCR.assertMethodWasCalled("isChildOfBinary");

		authorizator.MCR.assertNumberOfCallsToMethod(
				"userIsAuthorizedForActionOnRecordTypeAndCollectedData", 2);
		assertRecordDoesNotContainUploadAction(record);
	}

	private void assertRecordDoesNotContainUploadAction(DataRecord record) {
		assertFalse(record.getActions().contains(Action.UPLOAD));
	}

	@Test
	public void testIRAUploadActionPartOfEnhanceNotChildOfBinary() throws Exception {
		createRecordStorageSpy();

		DataRecord record = enhancer.enhanceIgnoringReadAccess(user, SOME_RECORD_TYPE,
				someDataGroup);

		assertUploadActionNotChildOfBinary(record);
	}

	@Test
	public void testSearchActionPartOfEnhanceForRecordTypeSearch() throws Exception {
		setupForSearchAction();

		DataRecord record = enhancer.enhance(user, SOME_RECORD_TYPE, someDataGroup);

		assertSearchActionForRecordTypeSearch(record);
	}

	private void setupForSearchAction() {
		createRecordStorageSpy();
		recordTypeHandlerSpy.representsTheRecordTypeDefiningSearches = true;
		addSearchChildsToSomeType();
	}

	private void addSearchChildsToSomeType() {
		DataGroup recordTypeToSearchIn1 = new DataGroupSpy("recordTypeToSearchIn");
		someDataGroup.addChild(recordTypeToSearchIn1);
		recordTypeToSearchIn1.addChild(new DataAtomicSpy("linkedRecordId", "linkedSearchId1"));
		DataGroup recordTypeToSearchIn2 = new DataGroupSpy("recordTypeToSearchIn");
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
	public void testIRASearchActionPartOfEnhanceForRecordTypeSearch() throws Exception {
		setupForSearchAction();

		DataRecord record = enhancer.enhanceIgnoringReadAccess(user, SOME_RECORD_TYPE,
				someDataGroup);

		assertSearchActionForRecordTypeSearch(record);
	}

	@Test
	public void testSearchActionPartOfEnhanceForRecordTypeSearchNotAuthorized() throws Exception {
		setupForSearchActionNotAuthorized();

		DataRecord record = enhancer.enhance(user, SOME_RECORD_TYPE, someDataGroup);

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
	public void testIRASearchActionPartOfEnhanceForRecordTypeSearchNotAuthorized()
			throws Exception {
		setupForSearchActionNotAuthorized();

		DataRecord record = enhancer.enhanceIgnoringReadAccess(user, SOME_RECORD_TYPE,
				someDataGroup);

		assertSearchActionForRecordTypeSearchNotAuthorized(record);
	}

	@Test
	public void testSearchActionPartOfEnhanceForRecordTypeSearchNoSearchType() throws Exception {
		createRecordStorageSpy();

		DataRecord record = enhancer.enhance(user, SOME_RECORD_TYPE, someDataGroup);

		assertSearchActionForRecordTypeSearchNoSearchType(record);
	}

	private void assertSearchActionForRecordTypeSearchNoSearchType(DataRecord record) {
		recordTypeHandlerSpy.MCR.assertMethodWasCalled("representsTheRecordTypeDefiningSearches");
		assertRecordDoesNotContainSearchAction(record);
	}

	@Test
	public void testIRASearchActionPartOfEnhanceForRecordTypeSearchNoSearchType() throws Exception {
		createRecordStorageSpy();

		DataRecord record = enhancer.enhanceIgnoringReadAccess(user, SOME_RECORD_TYPE,
				someDataGroup);

		assertSearchActionForRecordTypeSearchNoSearchType(record);
	}

	@Test
	public void testCreateActionPartOfEnhanceForRecordTypeRecordType() throws Exception {
		RecordTypeHandlerSpy recordTypeHandlerForRecordTypeInData = setupForCreateAction();

		DataRecord record = enhancer.enhance(user, SOME_RECORD_TYPE, someDataGroup);

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

		recordTypeHandlerForRecordTypeInData.MCR.assertMethodWasCalled("isAbstract");
		authorizator.MCR.assertParameters("userIsAuthorizedForActionOnRecordType", 0, user, CREATE,
				"someId");
		assertRecordContainsCreateAction(record);
	}

	private void assertRecordContainsCreateAction(DataRecord record) {
		assertTrue(record.getActions().contains(Action.CREATE));
	}

	@Test
	public void testIRACreateActionPartOfEnhanceForRecordTypeRecordType() throws Exception {
		RecordTypeHandlerSpy recordTypeHandlerForRecordTypeInData = setupForCreateAction();

		DataRecord record = enhancer.enhanceIgnoringReadAccess(user, SOME_RECORD_TYPE,
				someDataGroup);

		assertCreateActionForRecordTypeRecordType(recordTypeHandlerForRecordTypeInData, record);
	}

	@Test
	public void testCreateActionPartOfEnhanceForRecordTypeRecordTypeNotAuthorized()
			throws Exception {
		setupForCreateActionNotAuthorized();

		DataRecord record = enhancer.enhance(user, SOME_RECORD_TYPE, someDataGroup);

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
		recordTypeHandlerSpy.MCR.assertMethodWasCalled("isAbstract");
		authorizator.MCR.assertParameters("userIsAuthorizedForActionOnRecordType", 0, user, CREATE,
				"otherId");
		assertRecordDoesNotContainCreateAction(record);
	}

	private void assertRecordDoesNotContainCreateAction(DataRecord record) {
		assertFalse(record.getActions().contains(Action.CREATE));
	}

	@Test
	public void testIRACreateActionPartOfEnhanceForRecordTypeRecordTypeNotAuthorized()
			throws Exception {
		setupForCreateActionNotAuthorized();

		DataRecord record = enhancer.enhanceIgnoringReadAccess(user, SOME_RECORD_TYPE,
				someDataGroup);

		assertCreateActionForRecordTypeRecordTypeNotAuthorized(record);
	}

	@Test
	public void testCreateActionPartOfEnhanceForRecordTypeRecordTypeIsAbstract() throws Exception {
		setupForCreateActionAndAbstract();

		DataRecord record = enhancer.enhance(user, SOME_RECORD_TYPE, someDataGroup);

		assertCreateActionForRecordTypeRecordTypeIsAbstract(record);
	}

	private void setupForCreateActionAndAbstract() {
		setupForListAction();
		recordTypeHandlerSpy.isAbstract = true;
	}

	private void assertCreateActionForRecordTypeRecordTypeIsAbstract(DataRecord record) {
		recordTypeHandlerSpy.MCR
				.assertMethodWasCalled("representsTheRecordTypeDefiningRecordTypes");
		recordTypeHandlerSpy.MCR.assertMethodWasCalled("isAbstract");
		assertRecordDoesNotContainCreateAction(record);
	}

	@Test
	public void testIRACreateActionPartOfEnhanceForRecordTypeRecordTypeIsAbstract()
			throws Exception {
		setupForCreateActionAndAbstract();

		DataRecord record = enhancer.enhanceIgnoringReadAccess(user, SOME_RECORD_TYPE,
				someDataGroup);

		assertCreateActionForRecordTypeRecordTypeIsAbstract(record);
	}

	@Test
	public void testCreateActionPartOfEnhanceForRecordTypeRecordTypeIsNotRecordType()
			throws Exception {
		createRecordStorageSpy();

		DataRecord record = enhancer.enhance(user, SOME_RECORD_TYPE, someDataGroup);

		assertCreateActionForRecordTypeRecordTypeIsNotRecordType(record);
	}

	private void assertCreateActionForRecordTypeRecordTypeIsNotRecordType(DataRecord record) {
		recordTypeHandlerSpy.MCR
				.assertMethodWasCalled("representsTheRecordTypeDefiningRecordTypes");
		recordTypeHandlerSpy.MCR.assertMethodNotCalled("isAbstract");
		assertRecordDoesNotContainCreateAction(record);
	}

	@Test
	public void testIRACreateActionPartOfEnhanceForRecordTypeRecordTypeIsNotRecordType()
			throws Exception {
		createRecordStorageSpy();

		DataRecord record = enhancer.enhanceIgnoringReadAccess(user, SOME_RECORD_TYPE,
				someDataGroup);

		assertCreateActionForRecordTypeRecordTypeIsNotRecordType(record);
	}

	@Test
	public void testListActionPartOfEnhanceForRecordTypeRecordType() throws Exception {
		setupForListAction();

		DataRecord record = enhancer.enhance(user, SOME_RECORD_TYPE, someDataGroup);

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
	public void testIRAListActionPartOfEnhanceForRecordTypeRecordType() throws Exception {
		setupForListAction();

		DataRecord record = enhancer.enhanceIgnoringReadAccess(user, SOME_RECORD_TYPE,
				someDataGroup);

		assertListActionForRecordTypeRecordType(record);
	}

	@Test
	public void testListActionPartOfEnhanceForRecordTypeRecordTypeNotAuthorized() throws Exception {
		setupForListActionNotAuthorized();

		DataRecord record = enhancer.enhance(user, SOME_RECORD_TYPE, someDataGroup);

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
	public void testIRAListActionPartOfEnhanceForRecordTypeRecordTypeNotAuthorized()
			throws Exception {
		setupForListActionNotAuthorized();

		DataRecord record = enhancer.enhanceIgnoringReadAccess(user, SOME_RECORD_TYPE,
				someDataGroup);

		assertListActionForRecordTypeRecordTypeNotAuthorized(record);
	}

	@Test
	public void testListActionPartOfEnhanceForRecordTypeRecordTypeNotRecordType() throws Exception {
		createRecordStorageSpy();

		DataRecord record = enhancer.enhance(user, SOME_RECORD_TYPE, someDataGroup);

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
	public void testIRAListActionPartOfEnhanceForRecordTypeRecordTypeNotRecordType()
			throws Exception {
		createRecordStorageSpy();

		DataRecord record = enhancer.enhanceIgnoringReadAccess(user, SOME_RECORD_TYPE,
				someDataGroup);

		assertListActionForRecordTypeRecordTypeNotRecordType(record);
	}

	@Test
	public void testValidateActionPartOfEnhanceForRecordTypeRecordType() throws Exception {
		setupForListAction();

		DataRecord record = enhancer.enhance(user, SOME_RECORD_TYPE, someDataGroup);

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
	public void testIRAValidateActionPartOfEnhanceForRecordTypeRecordType() throws Exception {
		setupForListAction();

		DataRecord record = enhancer.enhanceIgnoringReadAccess(user, SOME_RECORD_TYPE,
				someDataGroup);

		assertValidateActionForRecordTypeRecordType(record);
	}

	@Test
	public void testValidateActionPartOfEnhanceForRecordTypeRecordTypeNotAuthorized()
			throws Exception {
		setupForCreateActionNotAuthorized();

		DataRecord record = enhancer.enhance(user, SOME_RECORD_TYPE, someDataGroup);

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
	public void testIRAValidateActionPartOfEnhanceForRecordTypeRecordTypeNotAuthorized()
			throws Exception {
		setupForCreateActionNotAuthorized();

		DataRecord record = enhancer.enhanceIgnoringReadAccess(user, SOME_RECORD_TYPE,
				someDataGroup);

		assertValidateActionForRecordTypeRecordTypeNotAuthorized(record);
	}

	@Test
	public void testValidateActionPartOfEnhanceForRecordTypeRecordTypeNotRecordType()
			throws Exception {
		createRecordStorageSpy();

		DataRecord record = enhancer.enhance(user, SOME_RECORD_TYPE, someDataGroup);

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
	public void testIRAValidateActionPartOfEnhanceForRecordTypeRecordTypeNotRecordType()
			throws Exception {
		createRecordStorageSpy();

		DataRecord record = enhancer.enhanceIgnoringReadAccess(user, SOME_RECORD_TYPE,
				someDataGroup);

		assertValidateActionForRecordTypeRecordTypeNotRecordType(record);
	}

	@Test
	public void testSearchActionPartOfEnhancedWhenEnhancingDataGroupContainingRecordTypeRecord()
			throws Exception {
		RecordTypeHandlerSpy recordTypeHandlerForRecordTypeInData = setupForSearchActionWhenEnhancingTypeOfRecordType();

		DataRecord record = enhancer.enhance(user, SOME_RECORD_TYPE, someDataGroup);

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
		dependencyProvider.MCR.assertNumberOfCallsToMethod("getRecordTypeHandler", 2);
		dependencyProvider.MCR.assertParameters("getRecordTypeHandler", 1, "someId");
		dependencyProvider.MCR.assertReturn("getRecordTypeHandler", 1,
				recordTypeHandlerForRecordTypeInData);

		recordTypeHandlerForRecordTypeInData.MCR.assertMethodWasCalled("hasLinkedSearch");
		String returnedSearchId = (String) recordTypeHandlerForRecordTypeInData.MCR
				.getReturnValue("getSearchId", 0);

		RecordStorageSpy recordStorage = (RecordStorageSpy) dependencyProvider.getRecordStorage();
		recordStorage.MCR.assertParameters("read", 0, SEARCH, returnedSearchId);
		authorizator.MCR.assertParameters("userIsAuthorizedForActionOnRecordType", 3, user, SEARCH,
				"linkedSearchId1");
		authorizator.MCR.assertParameters("userIsAuthorizedForActionOnRecordType", 4, user, SEARCH,
				"linkedSearchId2");
		authorizator.MCR.assertNumberOfCallsToMethod("userIsAuthorizedForActionOnRecordType", 5);
		assertRecordContainsSearchAction(record);
	}

	@Test
	public void testIRASearchActionPartOfEnhanceForRecordTypeRecordType() throws Exception {
		RecordTypeHandlerSpy recordTypeHandlerForRecordTypeInData = setupForSearchActionWhenEnhancingTypeOfRecordType();

		DataRecord record = enhancer.enhanceIgnoringReadAccess(user, SOME_RECORD_TYPE,
				someDataGroup);

		assertSearchActionForRecordTypeRecordType(recordTypeHandlerForRecordTypeInData, record);
	}

	@Test
	public void testSearchActionPartOfEnhanceForRecordTypeRecordTypeNotAuthorized()
			throws Exception {
		RecordTypeHandlerSpy recordTypeHandlerForRecordTypeInData = setupForSearchActionWhenEnhancingTypeOfRecordTypeNotAuthorized();

		DataRecord record = enhancer.enhance(user, SOME_RECORD_TYPE, someDataGroup);

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
		RecordStorageSpy recordStorage = (RecordStorageSpy) dependencyProvider.getRecordStorage();
		recordStorage.MCR.assertParameters("read", 0, SEARCH, returnedSearchId);
		authorizator.MCR.assertParameters("userIsAuthorizedForActionOnRecordType", 3, user, SEARCH,
				"linkedSearchId1");
		authorizator.MCR.assertNumberOfCallsToMethod("userIsAuthorizedForActionOnRecordType", 4);
		assertRecordDoesNotContainSearchAction(record);
	}

	private void createAndSetReturnDataGroupForReadInStorageForReadOfLinkedSearch() {
		DataGroupSpy searchGroupLinkedFromRecordType = new DataGroupSpy("someSearchId");
		DataGroup recordTypeToSearchIn1 = new DataGroupSpy("recordTypeToSearchIn");
		searchGroupLinkedFromRecordType.addChild(recordTypeToSearchIn1);
		recordTypeToSearchIn1.addChild(new DataAtomicSpy("linkedRecordId", "linkedSearchId1"));
		DataGroup recordTypeToSearchIn2 = new DataGroupSpy("recordTypeToSearchIn");
		searchGroupLinkedFromRecordType.addChild(recordTypeToSearchIn2);
		recordTypeToSearchIn2.addChild(new DataAtomicSpy("linkedRecordId", "linkedSearchId2"));
		RecordStorageSpy recordStorage = (RecordStorageSpy) dependencyProvider.getRecordStorage();
		recordStorage.returnForRead = searchGroupLinkedFromRecordType;
	}

	@Test
	public void testIRASearchActionPartOfEnhanceForRecordTypeRecordTypeNotAuthorized()
			throws Exception {
		RecordTypeHandlerSpy recordTypeHandlerForRecordTypeInData = setupForSearchActionWhenEnhancingTypeOfRecordTypeNotAuthorized();

		DataRecord record = enhancer.enhanceIgnoringReadAccess(user, SOME_RECORD_TYPE,
				someDataGroup);

		assertSearchActionForRecordTypeRecordTypeNotAuthorized(recordTypeHandlerForRecordTypeInData,
				record);
	}

	@Test
	public void testReadPermissionsAreAddedToRecord() throws Exception {
		createRecordStorageSpy();

		DataRecord record = enhancer.enhance(user, SOME_RECORD_TYPE, someDataGroup);

		assertReadPermissionsAreAddedToRecord(record);
	}

	private void assertReadPermissionsAreAddedToRecord(DataRecord record) {
		authorizator.MCR.assertParameter(
				"checkGetUsersMatchedRecordPartPermissionsForActionOnRecordTypeAndCollectedData", 0,
				"action", READ);
		authorizator.MCR.assertReturn(
				"checkGetUsersMatchedRecordPartPermissionsForActionOnRecordTypeAndCollectedData", 0,
				record.getReadPermissions());
	}

	@Test
	public void testIRAReadPermissionsAreAddedToRecord() throws Exception {
		createRecordStorageSpy();

		DataRecord record = enhancer.enhanceIgnoringReadAccess(user, SOME_RECORD_TYPE,
				someDataGroup);

		assertReadPermissionsAreAddedToRecord(record);
	}

	@Test
	public void testIRAReadPermissionsAreAddedToRecordWhenNoReadAccess() throws Exception {
		setupForNoReadAccess();

		DataRecord record = enhancer.enhanceIgnoringReadAccess(user, SOME_RECORD_TYPE,
				someDataGroup);

		assertEquals(record.getReadPermissions(), Collections.emptySet());
	}

	@Test
	public void testReadPermissionsAreAddedToRecordPublicData() throws Exception {
		setupForPublicRecord();

		DataRecord record = enhancer.enhance(user, SOME_RECORD_TYPE, someDataGroup);

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
	public void testIRAReadPermissionsAreAddedToRecordPublicData() throws Exception {
		setupForPublicRecord();

		DataRecord record = enhancer.enhanceIgnoringReadAccess(user, SOME_RECORD_TYPE,
				someDataGroup);

		assertReadPermissionsAreAddedToRecordPublicData(record);
	}

	@Test
	public void testWritePermissionsAreAddedToRecord() throws Exception {
		createRecordStorageSpy();

		DataRecord record = enhancer.enhance(user, SOME_RECORD_TYPE, someDataGroup);

		assertWritePermissionsAreAddedToRecord(record);
	}

	private void assertWritePermissionsAreAddedToRecord(DataRecord record) {
		authorizator.MCR.assertParameter(
				"checkGetUsersMatchedRecordPartPermissionsForActionOnRecordTypeAndCollectedData", 1,
				"action", UPDATE);
		authorizator.MCR.assertReturn(
				"checkGetUsersMatchedRecordPartPermissionsForActionOnRecordTypeAndCollectedData", 1,
				record.getWritePermissions());
	}

	@Test
	public void testIRAWritePermissionsAreAddedToRecord() throws Exception {
		createRecordStorageSpy();

		DataRecord record = enhancer.enhanceIgnoringReadAccess(user, SOME_RECORD_TYPE,
				someDataGroup);

		assertWritePermissionsAreAddedToRecord(record);
	}

	@Test
	public void testWritePermissionsAreAddedToRecordNotAutorized() throws Exception {
		setupForNoAccessOnUpdateActionOnRecordType();

		DataRecord record = enhancer.enhance(user, SOME_RECORD_TYPE, someDataGroup);

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
	public void testIRAWritePermissionsAreAddedToRecordNotAutorized() throws Exception {
		setupForNoAccessOnUpdateActionOnRecordType();

		DataRecord record = enhancer.enhanceIgnoringReadAccess(user, SOME_RECORD_TYPE,
				someDataGroup);

		assertWritePermissionsAreAddedToRecordNotAutorized(record);
	}

	@Test
	public void testRedactData() throws Exception {
		createRecordStorageSpy();

		DataRecord record = enhancer.enhance(user, SOME_RECORD_TYPE, someDataGroup);

		assertRedactCalledWithCorrectArguments();
		assertAswerFromRedactorIsReturned(record);
	}

	private void assertRedactCalledWithCorrectArguments() {
		Set<String> usersReadRecordPartPermissions = (Set<String>) authorizator.MCR.getReturnValue(
				"checkGetUsersMatchedRecordPartPermissionsForActionOnRecordTypeAndCollectedData",
				0);

		Set<String> recordPartConstraints = (Set<String>) recordTypeHandlerSpy.MCR
				.getReturnValue("getRecordPartReadConstraints", 0);

		dataRedactor.MCR.assertParameters("removeChildrenForConstraintsWithoutPermissions", 0,
				someDataGroup, recordPartConstraints, usersReadRecordPartPermissions);
	}

	private void assertAswerFromRedactorIsReturned(DataRecord record) {
		DataGroup redactedDataGroup = (DataGroup) dataRedactor.MCR
				.getReturnValue("removeChildrenForConstraintsWithoutPermissions", 0);

		assertSame(record.getDataGroup(), redactedDataGroup);
	}

	@Test
	public void testIRARedactData() throws Exception {
		createRecordStorageSpy();

		DataRecord record = enhancer.enhanceIgnoringReadAccess(user, SOME_RECORD_TYPE,
				someDataGroup);

		assertRedactCalledWithCorrectArguments();
		assertAswerFromRedactorIsReturned(record);
	}

	@Test
	public void testIRARedactDataIsCalledCorrectlyWhenNoAccess() throws Exception {
		setupForNoReadAccess();

		DataRecord record = enhancer.enhanceIgnoringReadAccess(user, SOME_RECORD_TYPE,
				someDataGroup);

		Set<String> recordPartConstraints = (Set<String>) recordTypeHandlerSpy.MCR
				.getReturnValue("getRecordPartReadConstraints", 0);

		dataRedactor.MCR.assertParameters("removeChildrenForConstraintsWithoutPermissions", 0,
				someDataGroup, recordPartConstraints, Collections.emptySet());
		assertAswerFromRedactorIsReturned(record);
	}

	@Test
	public void testLinksAreAddedToRedactedDataGroup() throws Exception {
		setupReturnedDataGroupOnDataRedactorSpy();

		DataRecord record = enhancer.enhance(user, DATA_WITH_LINKS, someDataGroup);

		RecordLinkTestsAsserter.assertTopLevelLinkContainsReadActionOnly(record);
	}

	private DataGroup setupReturnedDataGroupOnDataRedactorSpy() {
		DataGroup dataGroup = recordStorage.read(DATA_WITH_LINKS, "oneLinkTopLevel");
		dataRedactor.returnDataGroup = dataGroup;
		return dataGroup;
	}

	@Test
	public void testIRALinksAreAddedToRedactedDataGroup() throws Exception {
		setupReturnedDataGroupOnDataRedactorSpy();

		DataRecord record = enhancer.enhanceIgnoringReadAccess(user, DATA_WITH_LINKS,
				someDataGroup);

		RecordLinkTestsAsserter.assertTopLevelLinkContainsReadActionOnly(record);
	}

	@Test
	public void testReadRecordWithDataRecordLinkHasReadActionTopLevel() {
		// DataGroup dataGroup = recordStorage.read(DATA_WITH_LINKS, "oneLinkTopLevel");
		// dataRedactor.returnDataGroup = dataGroup;
		DataGroup dataGroup = setupReturnedDataGroupOnDataRedactorSpy();

		DataRecord record = enhancer.enhance(user, DATA_WITH_LINKS, dataGroup);

		RecordLinkTestsAsserter.assertTopLevelLinkContainsReadActionOnly(record);
	}

	@Test
	public void testIRAReadRecordWithDataRecordLinkHasReadActionTopLevel() {
		// DataGroup dataGroup = recordStorage.read(DATA_WITH_LINKS, "oneLinkTopLevel");
		// dataRedactor.returnDataGroup = dataGroup;
		DataGroup dataGroup = setupReturnedDataGroupOnDataRedactorSpy();

		DataRecord record = enhancer.enhanceIgnoringReadAccess(user, DATA_WITH_LINKS, dataGroup);

		RecordLinkTestsAsserter.assertTopLevelLinkContainsReadActionOnly(record);
	}

	@Test
	public void testReadRecordWithDataRecordLinkHasNOReadAction() {
		DataGroup dataGroup = recordStorage.read(DATA_WITH_LINKS, "oneLinkTopLevelNotAuthorized");
		dataRedactor.returnDataGroup = dataGroup;
		authorizator.setNotAutorizedForActionOnRecordType(CREATE, "toRecordType");
		authorizator.setNotAutorizedForActionOnRecordType(LIST, "toRecordType");
		authorizator.setNotAutorizedForActionOnRecordType(SEARCH, "toRecordType");
		authorizator.setNotAutorizedForActionOnRecordType(READ, "toRecordType");

		DataRecord record = enhancer.enhance(user, DATA_WITH_LINKS, dataGroup);
		RecordLinkTestsAsserter.assertTopLevelLinkDoesNotContainReadAction(record);
	}

	@Test
	public void testIRAReadRecordWithDataRecordLinkHasNOReadAction() {
		String recordType = DATA_WITH_LINKS;
		DataGroup dataGroup = recordStorage.read(recordType, "oneLinkTopLevelNotAuthorized");
		dataRedactor.returnDataGroup = dataGroup;
		authorizator.setNotAutorizedForActionOnRecordType(CREATE, "toRecordType");
		authorizator.setNotAutorizedForActionOnRecordType(LIST, "toRecordType");
		authorizator.setNotAutorizedForActionOnRecordType(SEARCH, "toRecordType");
		authorizator.setNotAutorizedForActionOnRecordType(READ, "toRecordType");

		DataRecord record = enhancer.enhanceIgnoringReadAccess(user, recordType, dataGroup);
		RecordLinkTestsAsserter.assertTopLevelLinkDoesNotContainReadAction(record);
	}

	@Test
	public void testReadRecordWithDataRecordLinkHasReadActionOneLevelDown() {
		String recordType = DATA_WITH_LINKS;
		DataGroup dataGroup = recordStorage.read(recordType, "oneLinkOneLevelDown");
		dataRedactor.returnDataGroup = dataGroup;
		DataRecord record = enhancer.enhance(user, recordType, dataGroup);

		RecordLinkTestsAsserter.assertOneLevelDownLinkContainsReadActionOnly(record);
	}

	@Test
	public void testIRAReadRecordWithDataRecordLinkHasReadActionOneLevelDown() {
		String recordType = DATA_WITH_LINKS;
		DataGroup dataGroup = recordStorage.read(recordType, "oneLinkOneLevelDown");
		dataRedactor.returnDataGroup = dataGroup;
		DataRecord record = enhancer.enhanceIgnoringReadAccess(user, recordType, dataGroup);

		RecordLinkTestsAsserter.assertOneLevelDownLinkContainsReadActionOnly(record);
	}

	@Test
	public void testReadRecordWithDataResourceLinkHasReadActionTopLevel() {
		String recordType = "dataWithResourceLinks";
		DataGroup dataGroup = recordStorage.read(recordType, "oneResourceLinkTopLevel");
		dataRedactor.returnDataGroup = dataGroup;
		DataRecord record = enhancer.enhance(user, recordType, dataGroup);

		RecordLinkTestsAsserter.assertTopLevelResourceLinkContainsReadActionOnly(record);
	}

	@Test
	public void testIRAReadRecordWithDataResourceLinkHasReadActionTopLevel() {
		String recordType = "dataWithResourceLinks";
		DataGroup dataGroup = recordStorage.read(recordType, "oneResourceLinkTopLevel");
		dataRedactor.returnDataGroup = dataGroup;
		DataRecord record = enhancer.enhanceIgnoringReadAccess(user, recordType, dataGroup);

		RecordLinkTestsAsserter.assertTopLevelResourceLinkContainsReadActionOnly(record);
	}

	@Test
	public void testReadRecordWithDataResourceLinkHasReadActionOneLevelDown() {
		String recordType = "dataWithResourceLinks";
		DataGroup dataGroup = recordStorage.read(recordType, "oneResourceLinkOneLevelDown");
		dataRedactor.returnDataGroup = dataGroup;

		DataRecord record = enhancer.enhance(user, recordType, dataGroup);

		RecordLinkTestsAsserter.assertOneLevelDownResourceLinkContainsReadActionOnly(record);
	}

	@Test
	public void testIRAReadRecordWithDataResourceLinkHasReadActionOneLevelDown() {
		String recordType = "dataWithResourceLinks";
		DataGroup dataGroup = recordStorage.read(recordType, "oneResourceLinkOneLevelDown");
		dataRedactor.returnDataGroup = dataGroup;

		DataRecord record = enhancer.enhanceIgnoringReadAccess(user, recordType, dataGroup);

		RecordLinkTestsAsserter.assertOneLevelDownResourceLinkContainsReadActionOnly(record);
	}

	@Test
	public void testReadRecordWithDataRecordLinkTargetDoesNotExist() {
		String recordType = DATA_WITH_LINKS;
		DataGroup dataGroup = recordStorage.read(recordType,
				"oneLinkOneLevelDownTargetDoesNotExist");
		dataRedactor.returnDataGroup = dataGroup;

		DataRecord record = enhancer.enhance(user, recordType, someDataGroup);

		assertReadRecordWithDataRecordLinkTargetDoesNotExist(record);
	}

	private void assertReadRecordWithDataRecordLinkTargetDoesNotExist(DataRecord record) {
		DataGroup recordDataGroup = record.getDataGroup();
		DataGroup dataGroupOneLevelDown = recordDataGroup
				.getFirstGroupWithNameInData("oneLevelDownTargetDoesNotExist");
		DataLink link = (DataLink) dataGroupOneLevelDown.getFirstChildWithNameInData("link");
		assertFalse(link.getActions().contains(Action.READ));
		assertEquals(link.getActions().size(), 0);
	}

	@Test
	public void testIRAReadRecordWithDataRecordLinkTargetDoesNotExist() {
		String recordType = DATA_WITH_LINKS;
		DataGroup dataGroup = recordStorage.read(recordType,
				"oneLinkOneLevelDownTargetDoesNotExist");
		dataRedactor.returnDataGroup = dataGroup;

		DataRecord record = enhancer.enhanceIgnoringReadAccess(user, recordType, someDataGroup);

		assertReadRecordWithDataRecordLinkTargetDoesNotExist(record);
	}

	@Test
	public void testLinkIsNotReadWhenRecordTypeIsPublic() {
		recordTypeHandlerSpy.isPublicForRead = true;
		String recordType = DATA_WITH_LINKS;
		DataGroup dataGroup = recordStorage.read(recordType, "oneLinkTopLevelNotAuthorized");
		dataRedactor.returnDataGroup = dataGroup;
		recordStorage.publicReadForToRecordType = "true";

		DataRecord record = enhancer.enhance(user, recordType, someDataGroup);

		assertLinkIsNotReadWhenRecordTypeIsPublic(record);
	}

	private void assertLinkIsNotReadWhenRecordTypeIsPublic(DataRecord record) {
		RecordLinkTestsAsserter.assertTopLevelLinkContainsReadActionOnly(record);
		assertRecordStorageWasNOTCalledForReadKey(recordStorage,
				"toRecordType:recordLinkNotAuthorized");

		dependencyProvider.MCR.assertParameters("getRecordTypeHandler", 0, DATA_WITH_LINKS);
		dependencyProvider.MCR.assertParameters("getRecordTypeHandler", 1, "toRecordType");
		dependencyProvider.MCR.assertParameters("getRecordTypeHandler", 2, "system");
		dependencyProvider.MCR.assertNumberOfCallsToMethod("getRecordTypeHandler", 3);
	}

	@Test
	public void testIRALinkIsNotReadWhenRecordTypeIsPublic() {
		recordTypeHandlerSpy.isPublicForRead = true;
		String recordType = DATA_WITH_LINKS;
		DataGroup dataGroup = recordStorage.read(recordType, "oneLinkTopLevelNotAuthorized");
		dataRedactor.returnDataGroup = dataGroup;
		recordStorage.publicReadForToRecordType = "true";

		DataRecord record = enhancer.enhanceIgnoringReadAccess(user, recordType, someDataGroup);

		assertLinkIsNotReadWhenRecordTypeIsPublic(record);
	}

	@Test
	public void testRecordTypeForLinkIsOnlyReadOnce() {
		recordTypeHandlerSpy.isPublicForRead = true;
		recordStorage.publicReadForToRecordType = "true";
		String recordType = DATA_WITH_LINKS;
		DataGroup dataGroup = recordStorage.read(recordType, "twoLinksTopLevel");
		dataRedactor.returnDataGroup = dataGroup;

		DataRecord record = enhancer.enhance(user, recordType, someDataGroup);

		assertRecordTypeForLinkIsOnlyReadOnce(record);
	}

	private void assertRecordTypeForLinkIsOnlyReadOnce(DataRecord record) {
		assertTopLevelTwoLinksContainReadActionOnly(record);

		assertRecordStorageWasNOTCalledForReadKey(recordStorage,
				"toRecordType:recordLinkNotAuthorized");

		dependencyProvider.MCR.assertParameters("getRecordTypeHandler", 0, DATA_WITH_LINKS);
		dependencyProvider.MCR.assertParameters("getRecordTypeHandler", 1, "system");
		dependencyProvider.MCR.assertParameters("getRecordTypeHandler", 2, "toRecordType");
		dependencyProvider.MCR.assertNumberOfCallsToMethod("getRecordTypeHandler", 3);
	}

	@Test
	public void testIRARecordTypeForLinkIsOnlyReadOnce() {
		recordTypeHandlerSpy.isPublicForRead = true;
		recordStorage.publicReadForToRecordType = "true";
		String recordType = DATA_WITH_LINKS;
		DataGroup dataGroup = recordStorage.read(recordType, "twoLinksTopLevel");
		dataRedactor.returnDataGroup = dataGroup;

		DataRecord record = enhancer.enhanceIgnoringReadAccess(user, recordType, someDataGroup);

		assertRecordTypeForLinkIsOnlyReadOnce(record);
	}

	@Test
	public void testLinkedRecordForLinkIsOnlyReadOnceForSameLinkedRecord() {
		String recordType = DATA_WITH_LINKS;
		DataGroup dataGroup = recordStorage.read(recordType, "twoLinksTopLevel");
		dataRedactor.returnDataGroup = dataGroup;

		DataRecord record = enhancer.enhance(user, recordType, someDataGroup);

		assertLinkedRecordForLinkIsOnlyReadOnceForSameLinkedRecord(record);
	}

	private void assertLinkedRecordForLinkIsOnlyReadOnceForSameLinkedRecord(DataRecord record) {
		assertTopLevelTwoLinksContainReadActionOnly(record);
		assertRecordStorageWasCalledOnlyOnceForReadKey(recordStorage, "toRecordType:toRecordId");

		String metadataId = (String) recordTypeHandlerSpy.MCR.getReturnValue("getMetadataId", 0);

		termCollector.MCR.assertParameter("collectTerms", 0, "metadataId", metadataId);
		termCollector.MCR.assertParameter("collectTerms", 1, "metadataId", metadataId);
		termCollector.MCR.assertParameter("collectTerms", 2, "metadataId", metadataId);
		termCollector.MCR.assertNumberOfCallsToMethod("collectTerms", 3);

		String methodName = "checkGetUsersMatchedRecordPartPermissionsForActionOnRecordTypeAndCollectedData";
		authorizator.MCR.assertParameters(methodName, 0, user, READ, DATA_WITH_LINKS);
		authorizator.MCR.assertParameters(methodName, 1, user, UPDATE, DATA_WITH_LINKS);
		authorizator.MCR.assertNumberOfCallsToMethod(methodName, 2);

		String methodName2 = "userIsAuthorizedForActionOnRecordTypeAndCollectedData";
		authorizator.MCR.assertParameters(methodName2, 0, user, "index", DATA_WITH_LINKS);
		authorizator.MCR.assertParameters(methodName2, 1, user, "delete", DATA_WITH_LINKS);
		authorizator.MCR.assertParameters(methodName2, 2, user, READ, "system");
		authorizator.MCR.assertParameters(methodName2, 3, user, READ, "toRecordType");
		authorizator.MCR.assertNumberOfCallsToMethod(methodName2, 4);
	}

	@Test
	public void testIRALinkedRecordForLinkIsOnlyReadOnceForSameLinkedRecord() {
		String recordType = DATA_WITH_LINKS;
		DataGroup dataGroup = recordStorage.read(recordType, "twoLinksTopLevel");
		dataRedactor.returnDataGroup = dataGroup;

		DataRecord record = enhancer.enhanceIgnoringReadAccess(user, recordType, someDataGroup);

		assertLinkedRecordForLinkIsOnlyReadOnceForSameLinkedRecord(record);
	}

	@Test
	public void testLinkedRecordForLinkIsOnlyReadOnceForSameLinkedRecordNotAuthorized() {
		String recordType = DATA_WITH_LINKS;
		DataGroup dataGroup = recordStorage.read(recordType, "twoLinksTopLevel");
		dataRedactor.returnDataGroup = dataGroup;

		authorizator.setNotAutorizedForActionOnRecordType(READ, "system");
		authorizator.setNotAutorizedForActionOnRecordType(READ, "toRecordType");

		DataRecord record = enhancer.enhance(user, recordType, dataGroup);

		assertLinkedRecordForLinkIsOnlyReadOnceForSameLinkedRecordNotAuthorized(record);
	}

	private void assertLinkedRecordForLinkIsOnlyReadOnceForSameLinkedRecordNotAuthorized(
			DataRecord record) {
		assertTopLevelTwoLinksDoesNotContainReadAction(record);
		assertRecordStorageWasCalledOnlyOnceForReadKey(recordStorage, "toRecordType:toRecordId");

		String metadataId = (String) recordTypeHandlerSpy.MCR.getReturnValue("getMetadataId", 0);

		termCollector.MCR.assertParameter("collectTerms", 0, "metadataId", metadataId);
		termCollector.MCR.assertParameter("collectTerms", 1, "metadataId", metadataId);
		termCollector.MCR.assertParameter("collectTerms", 2, "metadataId", metadataId);
		termCollector.MCR.assertNumberOfCallsToMethod("collectTerms", 3);

		String methodName = "checkGetUsersMatchedRecordPartPermissionsForActionOnRecordTypeAndCollectedData";
		authorizator.MCR.assertParameters(methodName, 0, user, READ, DATA_WITH_LINKS);
		authorizator.MCR.assertParameters(methodName, 1, user, UPDATE, DATA_WITH_LINKS);
		authorizator.MCR.assertNumberOfCallsToMethod(methodName, 2);

		String methodName2 = "userIsAuthorizedForActionOnRecordTypeAndCollectedData";
		authorizator.MCR.assertParameters(methodName2, 0, user, "index", DATA_WITH_LINKS);
		authorizator.MCR.assertParameters(methodName2, 1, user, "delete", DATA_WITH_LINKS);
		authorizator.MCR.assertParameters(methodName2, 2, user, READ, "system");
		authorizator.MCR.assertParameters(methodName2, 3, user, READ, "toRecordType");
		authorizator.MCR.assertNumberOfCallsToMethod(methodName2, 4);
	}

	@Test
	public void testIRALinkedRecordForLinkIsOnlyReadOnceForSameLinkedRecordNotAuthorized() {
		String recordType = DATA_WITH_LINKS;
		DataGroup dataGroup = recordStorage.read(recordType, "twoLinksTopLevel");
		dataRedactor.returnDataGroup = dataGroup;

		authorizator.setNotAutorizedForActionOnRecordType(READ, "system");
		authorizator.setNotAutorizedForActionOnRecordType(READ, "toRecordType");

		DataRecord record = enhancer.enhanceIgnoringReadAccess(user, recordType, dataGroup);

		assertTopLevelTwoLinksDoesNotContainReadAction(record);
		assertRecordStorageWasCalledOnlyOnceForReadKey(recordStorage, "toRecordType:toRecordId");

		String metadataId = (String) recordTypeHandlerSpy.MCR.getReturnValue("getMetadataId", 0);

		termCollector.MCR.assertParameter("collectTerms", 0, "metadataId", metadataId);
		termCollector.MCR.assertParameter("collectTerms", 1, "metadataId", metadataId);
		termCollector.MCR.assertParameter("collectTerms", 2, "metadataId", metadataId);
		termCollector.MCR.assertNumberOfCallsToMethod("collectTerms", 3);

		String methodName = "checkGetUsersMatchedRecordPartPermissionsForActionOnRecordTypeAndCollectedData";
		authorizator.MCR.assertParameters(methodName, 0, user, READ, DATA_WITH_LINKS);
		authorizator.MCR.assertParameters(methodName, 1, user, UPDATE, DATA_WITH_LINKS);
		authorizator.MCR.assertNumberOfCallsToMethod(methodName, 2);

		String methodName2 = "userIsAuthorizedForActionOnRecordTypeAndCollectedData";
		authorizator.MCR.assertParameters(methodName2, 0, user, "index", DATA_WITH_LINKS);
		authorizator.MCR.assertParameters(methodName2, 1, user, "delete", DATA_WITH_LINKS);
		authorizator.MCR.assertParameters(methodName2, 2, user, READ, "system");
		authorizator.MCR.assertParameters(methodName2, 3, user, READ, "toRecordType");
		authorizator.MCR.assertNumberOfCallsToMethod(methodName2, 4);
	}
}
