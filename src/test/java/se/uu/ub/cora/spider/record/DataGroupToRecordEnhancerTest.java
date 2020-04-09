/*
 * Copyright 2016, 2017, 2019 Uppsala University Library
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
import static se.uu.ub.cora.spider.record.RecordLinkTestsAsserter.assertRecordStorageWasCalledOnlyOnceForReadKey;
import static se.uu.ub.cora.spider.record.RecordLinkTestsAsserter.assertRecordStorageWasNOTCalledForReadKey;
import static se.uu.ub.cora.spider.record.RecordLinkTestsAsserter.assertTopLevelTwoLinksContainReadActionOnly;
import static se.uu.ub.cora.spider.record.RecordLinkTestsAsserter.assertTopLevelTwoLinksDoesNotContainReadAction;

import java.util.HashMap;

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
	public void testReadAction() throws Exception {
		createRecordStorageSpy();

		DataRecord record = enhancer.enhance(user, SOME_RECORD_TYPE, someDataGroup);

		DataGroup returnedCollectedTermsForRecordType = getAssertedCollectedTermsForRecordType(
				SOME_RECORD_TYPE, someDataGroup);
		authorizator.MCR.assertParameters(
				"checkAndGetUserAuthorizationsForActionOnRecordTypeAndCollectedData", 0, user,
				"read", SOME_RECORD_TYPE, returnedCollectedTermsForRecordType, true);
		assertTrue(record.getActions().contains(Action.READ));
	}

	private void createRecordStorageSpy() {
		RecordStorageSpy recordStorageSpy = new RecordStorageSpy();
		RecordStorageProviderSpy recordStorageProviderSpy = new RecordStorageProviderSpy();
		recordStorageProviderSpy.recordStorage = recordStorageSpy;
		dependencyProvider.setRecordStorageProvider(recordStorageProviderSpy);
		enhancer = new DataGroupToRecordEnhancerImp(dependencyProvider);
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
	public void testReadActionNotAuthorized() throws Exception {
		createRecordStorageSpy();
		authorizator.authorizedForActionAndRecordTypeAndCollectedData = false;

		DataRecord record = enhancer.enhance(user, SOME_RECORD_TYPE, someDataGroup);

		DataGroup returnedCollectedTermsForRecordType = getAssertedCollectedTermsForRecordType(
				SOME_RECORD_TYPE, someDataGroup);
		authorizator.MCR.assertParameters(
				"checkAndGetUserAuthorizationsForActionOnRecordTypeAndCollectedData", 0, user,
				"read", SOME_RECORD_TYPE, returnedCollectedTermsForRecordType, true);
		assertFalse(record.getActions().contains(Action.READ));
	}

	// TODO: activate this test that checks that we get Read if it is public recordType
	@Test(enabled = false)
	public void testReadActionNotAuthorizedButPublicData() throws Exception {
		createRecordStorageSpy();
		recordTypeHandlerSpy.isPublicForRead = true;
		authorizator.authorizedForActionAndRecordTypeAndCollectedData = false;

		DataRecord record = enhancer.enhance(user, SOME_RECORD_TYPE, someDataGroup);

		DataGroup returnedCollectedTermsForRecordType = getAssertedCollectedTermsForRecordType(
				SOME_RECORD_TYPE, someDataGroup);
		authorizator.MCR.assertParameters(
				"checkAndGetUserAuthorizationsForActionOnRecordTypeAndCollectedData", 0, user,
				"read", SOME_RECORD_TYPE, returnedCollectedTermsForRecordType, true);
		assertTrue(record.getActions().contains(Action.READ));
	}

	@Test
	public void testUpdateAction() throws Exception {
		createRecordStorageSpy();

		DataRecord record = enhancer.enhance(user, SOME_RECORD_TYPE, someDataGroup);

		DataGroup returnedCollectedTermsForRecordType = getAssertedCollectedTermsForRecordType(
				SOME_RECORD_TYPE, someDataGroup);
		authorizator.MCR.assertParameters(
				"checkAndGetUserAuthorizationsForActionOnRecordTypeAndCollectedData", 1, user,
				"update", SOME_RECORD_TYPE, returnedCollectedTermsForRecordType, true);
		assertTrue(record.getActions().contains(Action.UPDATE));
	}

	@Test
	public void testUpdateActionNotAuthorized() throws Exception {
		createRecordStorageSpy();
		authorizator.authorizedForActionAndRecordTypeAndCollectedData = false;

		DataRecord record = enhancer.enhance(user, SOME_RECORD_TYPE, someDataGroup);

		DataGroup returnedCollectedTermsForRecordType = getAssertedCollectedTermsForRecordType(
				SOME_RECORD_TYPE, someDataGroup);
		authorizator.MCR.assertParameters(
				"checkAndGetUserAuthorizationsForActionOnRecordTypeAndCollectedData", 1, user,
				"update", SOME_RECORD_TYPE, returnedCollectedTermsForRecordType, true);
		assertFalse(record.getActions().contains(Action.UPDATE));
	}

	@Test
	public void testIndexAction() throws Exception {
		createRecordStorageSpy();

		DataRecord record = enhancer.enhance(user, SOME_RECORD_TYPE, someDataGroup);

		DataGroup returnedCollectedTermsForRecordType = getAssertedCollectedTermsForRecordType(
				SOME_RECORD_TYPE, someDataGroup);
		authorizator.MCR.assertParameters("userIsAuthorizedForActionOnRecordTypeAndCollectedData",
				0, user, "index", SOME_RECORD_TYPE, returnedCollectedTermsForRecordType);
		assertTrue(record.getActions().contains(Action.INDEX));
	}

	@Test
	public void testIndexActionNotAuthorized() throws Exception {
		createRecordStorageSpy();
		authorizator.authorizedForActionAndRecordTypeAndCollectedData = false;

		DataRecord record = enhancer.enhance(user, SOME_RECORD_TYPE, someDataGroup);

		DataGroup returnedCollectedTermsForRecordType = getAssertedCollectedTermsForRecordType(
				SOME_RECORD_TYPE, someDataGroup);
		authorizator.MCR.assertParameters("userIsAuthorizedForActionOnRecordTypeAndCollectedData",
				0, user, "index", SOME_RECORD_TYPE, returnedCollectedTermsForRecordType);
		assertFalse(record.getActions().contains(Action.INDEX));
	}

	@Test
	public void testDeleteAction() throws Exception {
		createRecordStorageSpy();

		DataRecord record = enhancer.enhance(user, SOME_RECORD_TYPE, someDataGroup);

		DataGroup returnedCollectedTermsForRecordType = getAssertedCollectedTermsForRecordType(
				SOME_RECORD_TYPE, someDataGroup);
		authorizator.MCR.assertParameters("userIsAuthorizedForActionOnRecordTypeAndCollectedData",
				1, user, "delete", SOME_RECORD_TYPE, returnedCollectedTermsForRecordType);
		assertTrue(record.getActions().contains(Action.DELETE));
	}

	@Test
	public void testDeleteActionNotAuthorized() throws Exception {
		createRecordStorageSpy();
		authorizator.authorizedForActionAndRecordTypeAndCollectedData = false;

		DataRecord record = enhancer.enhance(user, SOME_RECORD_TYPE, someDataGroup);

		DataGroup returnedCollectedTermsForRecordType = getAssertedCollectedTermsForRecordType(
				SOME_RECORD_TYPE, someDataGroup);
		authorizator.MCR.assertParameters("userIsAuthorizedForActionOnRecordTypeAndCollectedData",
				1, user, "delete", SOME_RECORD_TYPE, returnedCollectedTermsForRecordType);
		assertFalse(record.getActions().contains(Action.DELETE));
	}

	@Test
	public void testDeleteActionAuthorizedButHasIncomingLinks() throws Exception {
		createRecordStorageSpy();
		RecordStorageSpy recordStorageSpy = (RecordStorageSpy) dependencyProvider
				.getRecordStorage();
		recordStorageSpy.incomingLinksExistsForType.add(SOME_RECORD_TYPE);

		DataRecord record = enhancer.enhance(user, SOME_RECORD_TYPE, someDataGroup);

		DataGroup returnedCollectedTermsForRecordType = getAssertedCollectedTermsForRecordType(
				SOME_RECORD_TYPE, someDataGroup);
		authorizator.MCR.assertParameters("userIsAuthorizedForActionOnRecordTypeAndCollectedData",
				1, user, "delete", SOME_RECORD_TYPE, returnedCollectedTermsForRecordType);

		// TODO: not good record id...
		recordStorageSpy.MCR.assertNumberOfCallsToMethod("linksExistForRecord", 1);
		recordStorageSpy.MCR.assertParameters("linksExistForRecord", 0, SOME_RECORD_TYPE, "someId");

		assertFalse(record.getActions().contains(Action.DELETE));
	}

	@Test
	public void testDeleteActionAuthorizedButHasIncomingLinksAsParentRecordType() throws Exception {
		createRecordStorageSpy();
		recordTypeHandlerSpy.hasParent = true;
		RecordStorageSpy recordStorageSpy = (RecordStorageSpy) dependencyProvider
				.getRecordStorage();
		recordStorageSpy.incomingLinksExistsForType.add("someParentId");

		DataRecord record = enhancer.enhance(user, SOME_RECORD_TYPE, someDataGroup);

		DataGroup returnedCollectedTermsForRecordType = getAssertedCollectedTermsForRecordType(
				SOME_RECORD_TYPE, someDataGroup);
		authorizator.MCR.assertParameters("userIsAuthorizedForActionOnRecordTypeAndCollectedData",
				1, user, "delete", SOME_RECORD_TYPE, returnedCollectedTermsForRecordType);

		// TODO: someId from data, not good.....
		recordStorageSpy.MCR.assertParameters("linksExistForRecord", 0, SOME_RECORD_TYPE, "someId");

		recordTypeHandlerSpy.MCR.assertMethodWasCalled("hasParent");
		String parentIdFromRecordTypeHandler = (String) recordTypeHandlerSpy.MCR
				.getReturnValue("getParentId", 0);
		recordStorageSpy.MCR.assertParameters("linksExistForRecord", 1,
				parentIdFromRecordTypeHandler, "someId");
		recordStorageSpy.MCR.assertNumberOfCallsToMethod("linksExistForRecord", 2);

		assertFalse(record.getActions().contains(Action.DELETE));
	}

	@Test
	public void testIncomingLinksActionHasNoIncommingLinks() throws Exception {
		createRecordStorageSpy();

		DataRecord record = enhancer.enhance(user, SOME_RECORD_TYPE, someDataGroup);

		assertFalse(record.getActions().contains(Action.READ_INCOMING_LINKS));
	}

	@Test
	public void testIncomingLinksActionHasIncomingLinks() throws Exception {
		createRecordStorageSpy();
		RecordStorageSpy recordStorageSpy = (RecordStorageSpy) dependencyProvider
				.getRecordStorage();
		recordStorageSpy.incomingLinksExistsForType.add(SOME_RECORD_TYPE);

		DataRecord record = enhancer.enhance(user, SOME_RECORD_TYPE, someDataGroup);

		assertTrue(record.getActions().contains(Action.READ_INCOMING_LINKS));
	}

	@Test
	public void testUploadAction() throws Exception {
		createRecordStorageSpy();
		recordTypeHandlerSpy.isChildOfBinary = true;

		DataRecord record = enhancer.enhance(user, SOME_RECORD_TYPE, someDataGroup);

		recordTypeHandlerSpy.MCR.assertMethodWasCalled("isChildOfBinary");

		DataGroup returnedCollectedTermsForRecordType = getAssertedCollectedTermsForRecordType(
				SOME_RECORD_TYPE, someDataGroup);
		authorizator.MCR.assertParameters("userIsAuthorizedForActionOnRecordTypeAndCollectedData",
				2, user, "upload", SOME_RECORD_TYPE, returnedCollectedTermsForRecordType);
		assertTrue(record.getActions().contains(Action.UPLOAD));
	}

	@Test
	public void testUploadActionNotAuthorized() throws Exception {
		createRecordStorageSpy();
		recordTypeHandlerSpy.isChildOfBinary = true;
		authorizator.authorizedForActionAndRecordTypeAndCollectedData = false;

		DataRecord record = enhancer.enhance(user, SOME_RECORD_TYPE, someDataGroup);

		recordTypeHandlerSpy.MCR.assertMethodWasCalled("isChildOfBinary");

		DataGroup returnedCollectedTermsForRecordType = getAssertedCollectedTermsForRecordType(
				SOME_RECORD_TYPE, someDataGroup);
		authorizator.MCR.assertParameters("userIsAuthorizedForActionOnRecordTypeAndCollectedData",
				2, user, "upload", SOME_RECORD_TYPE, returnedCollectedTermsForRecordType);
		assertFalse(record.getActions().contains(Action.UPLOAD));

		authorizator.MCR.assertNumberOfCallsToMethod(
				"userIsAuthorizedForActionOnRecordTypeAndCollectedData", 3);
	}

	@Test
	public void testUploadActionNotChildOfBinary() throws Exception {
		createRecordStorageSpy();

		DataRecord record = enhancer.enhance(user, SOME_RECORD_TYPE, someDataGroup);

		recordTypeHandlerSpy.MCR.assertMethodWasCalled("isChildOfBinary");

		authorizator.MCR.assertNumberOfCallsToMethod(
				"userIsAuthorizedForActionOnRecordTypeAndCollectedData", 2);
		assertFalse(record.getActions().contains(Action.UPLOAD));
	}

	@Test
	public void testSearchActionForRecordTypeSearch() throws Exception {
		createRecordStorageSpy();
		recordTypeHandlerSpy.isSearchType = true;
		DataGroup recordTypeToSearchIn1 = new DataGroupSpy("recordTypeToSearchIn");
		someDataGroup.addChild(recordTypeToSearchIn1);
		recordTypeToSearchIn1.addChild(new DataAtomicSpy("linkedRecordId", "linkedSearchId1"));
		DataGroup recordTypeToSearchIn2 = new DataGroupSpy("recordTypeToSearchIn");
		someDataGroup.addChild(recordTypeToSearchIn2);
		recordTypeToSearchIn2.addChild(new DataAtomicSpy("linkedRecordId", "linkedSearchId2"));

		DataRecord record = enhancer.enhance(user, SOME_RECORD_TYPE, someDataGroup);

		recordTypeHandlerSpy.MCR.assertMethodWasCalled("representsTheRecordTypeDefiningSearches");
		authorizator.MCR.assertParameters("userIsAuthorizedForActionOnRecordType", 0, user,
				"search", "linkedSearchId1");
		authorizator.MCR.assertParameters("userIsAuthorizedForActionOnRecordType", 1, user,
				"search", "linkedSearchId2");
		authorizator.MCR.assertNumberOfCallsToMethod("userIsAuthorizedForActionOnRecordType", 2);
		assertTrue(record.getActions().contains(Action.SEARCH));
	}

	@Test
	public void testSearchActionForRecordTypeSearchNotAuthorized() throws Exception {
		createRecordStorageSpy();
		authorizator.authorizedForActionAndRecordType = false;
		recordTypeHandlerSpy.isSearchType = true;
		DataGroup recordTypeToSearchIn1 = new DataGroupSpy("recordTypeToSearchIn");
		someDataGroup.addChild(recordTypeToSearchIn1);
		recordTypeToSearchIn1.addChild(new DataAtomicSpy("linkedRecordId", "linkedSearchId1"));
		DataGroup recordTypeToSearchIn2 = new DataGroupSpy("recordTypeToSearchIn");
		someDataGroup.addChild(recordTypeToSearchIn2);
		recordTypeToSearchIn2.addChild(new DataAtomicSpy("linkedRecordId", "linkedSearchId2"));

		DataRecord record = enhancer.enhance(user, SOME_RECORD_TYPE, someDataGroup);

		recordTypeHandlerSpy.MCR.assertMethodWasCalled("representsTheRecordTypeDefiningSearches");
		authorizator.MCR.assertParameters("userIsAuthorizedForActionOnRecordType", 0, user,
				"search", "linkedSearchId1");
		authorizator.MCR.assertNumberOfCallsToMethod("userIsAuthorizedForActionOnRecordType", 1);
		assertFalse(record.getActions().contains(Action.SEARCH));
	}

	@Test
	public void testSearchActionForRecordTypeSearchNoSearchType() throws Exception {
		createRecordStorageSpy();

		DataRecord record = enhancer.enhance(user, SOME_RECORD_TYPE, someDataGroup);

		recordTypeHandlerSpy.MCR.assertMethodWasCalled("representsTheRecordTypeDefiningSearches");
		assertFalse(record.getActions().contains(Action.SEARCH));
	}

	@Test
	public void testCreateActionForRecordTypeRecordType() throws Exception {
		createRecordStorageSpy();
		dependencyProvider.createRecordTypeHandlerSpy("someId");
		recordTypeHandlerSpy.isRecordType = true;

		DataRecord record = enhancer.enhance(user, SOME_RECORD_TYPE, someDataGroup);

		recordTypeHandlerSpy.MCR
				.assertMethodWasCalled("representsTheRecordTypeDefiningRecordTypes");
		dependencyProvider.MCR.assertNumberOfCallsToMethod("getRecordTypeHandler", 2);
		dependencyProvider.MCR.assertParameters("getRecordTypeHandler", 1, "someId");
		RecordTypeHandlerSpy recordTypeHandler2 = (RecordTypeHandlerSpy) dependencyProvider.MCR
				.getReturnValue("getRecordTypeHandler", 1);

		recordTypeHandler2.MCR.assertMethodWasCalled("isAbstract");
		authorizator.MCR.assertParameters("userIsAuthorizedForActionOnRecordType", 0, user,
				"create", "someId");
		assertTrue(record.getActions().contains(Action.CREATE));
	}

	@Test
	public void testCreateActionForRecordTypeRecordTypeNotAuthorized() throws Exception {
		createRecordStorageSpy();
		someDataGroup = createDummyDataGroupForRecord("otherId");
		recordTypeHandlerSpy.isRecordType = true;
		authorizator.authorizedForActionAndRecordType = false;

		DataRecord record = enhancer.enhance(user, SOME_RECORD_TYPE, someDataGroup);

		recordTypeHandlerSpy.MCR
				.assertMethodWasCalled("representsTheRecordTypeDefiningRecordTypes");
		recordTypeHandlerSpy.MCR.assertMethodWasCalled("isAbstract");
		authorizator.MCR.assertParameters("userIsAuthorizedForActionOnRecordType", 0, user,
				"create", "otherId");
		assertFalse(record.getActions().contains(Action.CREATE));
	}

	@Test
	public void testCreateActionForRecordTypeRecordTypeIsAbstract() throws Exception {
		createRecordStorageSpy();
		recordTypeHandlerSpy.isRecordType = true;
		recordTypeHandlerSpy.isAbstract = true;

		DataRecord record = enhancer.enhance(user, SOME_RECORD_TYPE, someDataGroup);

		recordTypeHandlerSpy.MCR
				.assertMethodWasCalled("representsTheRecordTypeDefiningRecordTypes");
		recordTypeHandlerSpy.MCR.assertMethodWasCalled("isAbstract");
		assertFalse(record.getActions().contains(Action.CREATE));
	}

	@Test
	public void testCreateActionForRecordTypeRecordTypeIsNotRecordType() throws Exception {
		createRecordStorageSpy();

		DataRecord record = enhancer.enhance(user, SOME_RECORD_TYPE, someDataGroup);

		recordTypeHandlerSpy.MCR
				.assertMethodWasCalled("representsTheRecordTypeDefiningRecordTypes");
		recordTypeHandlerSpy.MCR.assertMethodNotCalled("isAbstract");
		assertFalse(record.getActions().contains(Action.CREATE));
	}

	@Test
	public void testListActionForRecordTypeRecordType() throws Exception {
		createRecordStorageSpy();
		recordTypeHandlerSpy.isRecordType = true;

		DataRecord record = enhancer.enhance(user, SOME_RECORD_TYPE, someDataGroup);

		recordTypeHandlerSpy.MCR
				.assertMethodWasCalled("representsTheRecordTypeDefiningRecordTypes");
		authorizator.MCR.assertParameters("userIsAuthorizedForActionOnRecordType", 1, user, "list",
				"someId");
		assertTrue(record.getActions().contains(Action.LIST));
	}

	@Test
	public void testListActionForRecordTypeRecordTypeNotAuthorized() throws Exception {
		createRecordStorageSpy();
		recordTypeHandlerSpy.isRecordType = true;
		authorizator.authorizedForActionAndRecordType = false;

		DataRecord record = enhancer.enhance(user, SOME_RECORD_TYPE, someDataGroup);

		recordTypeHandlerSpy.MCR
				.assertMethodWasCalled("representsTheRecordTypeDefiningRecordTypes");
		authorizator.MCR.assertParameters("userIsAuthorizedForActionOnRecordType", 1, user, "list",
				"someId");
		assertFalse(record.getActions().contains(Action.LIST));
	}

	@Test
	public void testListActionForRecordTypeRecordTypeNotRecordType() throws Exception {
		createRecordStorageSpy();

		DataRecord record = enhancer.enhance(user, SOME_RECORD_TYPE, someDataGroup);

		recordTypeHandlerSpy.MCR
				.assertMethodWasCalled("representsTheRecordTypeDefiningRecordTypes");

		assertFalse(record.getActions().contains(Action.LIST));
	}

	@Test
	public void testValidateActionForRecordTypeRecordType() throws Exception {
		createRecordStorageSpy();
		recordTypeHandlerSpy.isRecordType = true;

		DataRecord record = enhancer.enhance(user, SOME_RECORD_TYPE, someDataGroup);

		recordTypeHandlerSpy.MCR
				.assertMethodWasCalled("representsTheRecordTypeDefiningRecordTypes");
		authorizator.MCR.assertParameters("userIsAuthorizedForActionOnRecordType", 2, user,
				"validate", "someId");
		assertTrue(record.getActions().contains(Action.VALIDATE));
	}

	@Test
	public void testValidateActionForRecordTypeRecordTypeNotAuthorized() throws Exception {
		createRecordStorageSpy();
		someDataGroup = createDummyDataGroupForRecord("otherId");
		recordTypeHandlerSpy.isRecordType = true;
		authorizator.authorizedForActionAndRecordType = false;

		DataRecord record = enhancer.enhance(user, SOME_RECORD_TYPE, someDataGroup);

		recordTypeHandlerSpy.MCR
				.assertMethodWasCalled("representsTheRecordTypeDefiningRecordTypes");
		authorizator.MCR.assertParameters("userIsAuthorizedForActionOnRecordType", 2, user,
				"validate", "otherId");
		assertFalse(record.getActions().contains(Action.VALIDATE));
	}

	@Test
	public void testValidateActionForRecordTypeRecordTypeNotRecordType() throws Exception {
		createRecordStorageSpy();

		DataRecord record = enhancer.enhance(user, SOME_RECORD_TYPE, someDataGroup);
		recordTypeHandlerSpy.MCR
				.assertMethodWasCalled("representsTheRecordTypeDefiningRecordTypes");

		assertFalse(record.getActions().contains(Action.VALIDATE));
	}

	// TODO: we are here
	// TODO: If NO READ permissions, throw exception.inte här men principiellt... :)
	@Test
	public void testSearchActionForRecordTypeRecordType() throws Exception {
		createRecordStorageSpy();
		recordTypeHandlerSpy.isRecordType = true;
		dependencyProvider.createRecordTypeHandlerSpy("someId");
		RecordTypeHandlerSpy recordTypeHandler2 = dependencyProvider.mapOfRecordTypeHandlerSpies
				.get("someId");
		recordTypeHandler2.hasLinkedSearch = true;

		DataRecord record = enhancer.enhance(user, SOME_RECORD_TYPE, someDataGroup);

		recordTypeHandlerSpy.MCR
				.assertMethodWasCalled("representsTheRecordTypeDefiningRecordTypes");
		dependencyProvider.MCR.assertNumberOfCallsToMethod("getRecordTypeHandler", 2);
		dependencyProvider.MCR.assertParameters("getRecordTypeHandler", 1, "someId");
		// RecordTypeHandlerSpy recordTypeHandler2 = (RecordTypeHandlerSpy) dependencyProvider.MCR
		// .getReturnValue("getRecordTypeHandler", 1);
		dependencyProvider.MCR.assertReturn("getRecordTypeHandler", 1, recordTypeHandler2);

		recordTypeHandler2.MCR.assertMethodWasCalled("hasLinkedSearch");
		// authorizator.MCR.assertParameters("userIsAuthorizedForActionOnRecordTypeAndCollectedData",
		// 2, user, "validate", SOME_RECORD_TYPE, returnedCollectedTermsForRecordType);
		assertTrue(record.getActions().contains(Action.SEARCH));
	}

	@Test
	public void testAllDataIndependentActions() {
		User user = new User("987654321");
		String recordType = "place";
		DataGroup dataGroup = recordStorage.read("place", "place:0001");
		DataRecord record = enhancer.enhance(user, recordType, dataGroup);

		assertEquals(record.getActions().size(), 4);
		assertTrue(record.getActions().contains(Action.READ));
		assertTrue(record.getActions().contains(Action.UPDATE));

		assertTrue(record.getActions().contains(Action.READ_INCOMING_LINKS));
		assertTrue(record.getActions().contains(Action.INDEX));
	}

	@Test
	public void testAuthorizedToDeleteAndNoIncomingLink() {
		DataGroup dataGroup = recordStorage.read("place", "place:0002");
		String recordType = "place";
		DataRecord record = enhancer.enhance(user, recordType, dataGroup);
		assertEquals(record.getActions().size(), 4);
		assertTrue(record.getActions().contains(Action.READ));
		assertTrue(record.getActions().contains(Action.UPDATE));
		assertTrue(record.getActions().contains(Action.DELETE));
		assertTrue(record.getActions().contains(Action.INDEX));

		assertFalse(record.getActions().contains(Action.READ_INCOMING_LINKS));
	}

	@Test
	public void testNotAuthorizedToDeleteAndNoIncomingLink() {
		authorizator.authorizedForActionAndRecordTypeAndCollectedData = false;

		DataGroup dataGroup = recordStorage.read("place", "place:0002");
		String recordType = "place";
		DataRecord record = enhancer.enhance(user, recordType, dataGroup);
		assertEquals(record.getActions().size(), 0);
	}

	@Test
	public void testAuthorizedToDeleteAndIncomingLink() {
		DataGroup dataGroup = recordStorage.read("place", "place:0001");
		String recordType = "place";
		DataRecord record = enhancer.enhance(user, recordType, dataGroup);
		assertEquals(record.getActions().size(), 4);
		assertTrue(record.getActions().contains(Action.READ));
		assertTrue(record.getActions().contains(Action.UPDATE));
		assertTrue(record.getActions().contains(Action.READ_INCOMING_LINKS));
		assertTrue(record.getActions().contains(Action.INDEX));

		assertFalse(record.getActions().contains(Action.DELETE));
	}

	// @Test
	// public void testAuthorizedToDeleteAndIncomingLinkToAbstractParent() {
	// recordTypeHandlerSpy.hasParent = true;
	// // RecordStorageSpy recordStorageSpy = (RecordStorageSpy) dependencyProvider
	// // .getRecordStorage();
	// // recordStorageSpy.incomingLinksExistsForType.add("someParentId");
	//
	// DataGroup dataGroup = recordStorage.read("place", "place:0003");
	//
	// String recordType = "place";
	//
	// DataRecord record = enhancer.enhance(user, recordType, dataGroup);
	// assertEquals(record.getActions().size(), 4);
	// assertTrue(record.getActions().contains(Action.READ));
	// assertTrue(record.getActions().contains(Action.UPDATE));
	// assertTrue(record.getActions().contains(Action.READ_INCOMING_LINKS));
	// assertTrue(record.getActions().contains(Action.INDEX));
	//
	// assertFalse(record.getActions().contains(Action.DELETE));
	//
	// }

	@Test
	public void testNotAuthorizedToDeleteAndIncomingLink() {
		authorizator.authorizedForActionAndRecordTypeAndCollectedData = false;

		String recordType = "place";
		DataGroup dataGroup = recordStorage.read("place", "place:0001");
		DataRecord record = enhancer.enhance(user, recordType, dataGroup);

		assertEquals(record.getActions().size(), 1);
		assertTrue(record.getActions().contains(Action.READ_INCOMING_LINKS));
	}

	@Test
	public void testActionsOnReadRecordTypeBinary() {
		recordTypeHandlerSpy.isRecordType = true;
		recordTypeHandlerSpy.isAbstract = true;
		DataGroup dataGroup = recordStorage.read("recordType", "binary");
		String recordType = "recordType";
		DataRecord record = enhancer.enhance(user, recordType, dataGroup);
		assertEquals(record.getActions().size(), 6);
		assertTrue(record.getActions().contains(Action.READ));
		assertTrue(record.getActions().contains(Action.UPDATE));
		assertTrue(record.getActions().contains(Action.DELETE));
		assertTrue(record.getActions().contains(Action.LIST));
		assertTrue(record.getActions().contains(Action.INDEX));
		assertTrue(record.getActions().contains(Action.VALIDATE));

		assertFalse(record.getActions().contains(Action.UPLOAD));

	}

	@Test
	public void testActionsOnReadRecordTypeImage() {
		recordTypeHandlerSpy.isRecordType = true;
		DataGroup dataGroup = recordStorage.read("recordType", "image");
		String recordType = "recordType";
		DataRecord record = enhancer.enhance(user, recordType, dataGroup);
		assertEquals(record.getActions().size(), 7);
		assertTrue(record.getActions().contains(Action.READ));
		assertTrue(record.getActions().contains(Action.UPDATE));
		assertTrue(record.getActions().contains(Action.DELETE));
		assertTrue(record.getActions().contains(Action.CREATE));
		assertTrue(record.getActions().contains(Action.LIST));
		assertTrue(record.getActions().contains(Action.INDEX));
		assertTrue(record.getActions().contains(Action.VALIDATE));

		assertFalse(record.getActions().contains(Action.UPLOAD));
	}

	@Test
	public void testAuthorizedOnReadImage() {
		recordTypeHandlerSpy.isChildOfBinary = true;
		DataGroup dataGroup = recordStorage.read("image", "image:0001");
		String recordType = "image";
		DataRecord record = enhancer.enhance(user, recordType, dataGroup);
		assertEquals(record.getActions().size(), 5);
		assertTrue(record.getActions().contains(Action.READ));
		assertTrue(record.getActions().contains(Action.UPDATE));
		assertTrue(record.getActions().contains(Action.DELETE));
		assertTrue(record.getActions().contains(Action.UPLOAD));
		assertTrue(record.getActions().contains(Action.INDEX));
	}

	@Test
	public void testNotAuthorizedOnReadImage() {
		authorizator.authorizedForActionAndRecordTypeAndCollectedData = false;

		DataGroup dataGroup = recordStorage.read("image", "image:0001");
		String recordType = "image";
		DataRecord record = enhancer.enhance(user, recordType, dataGroup);
		assertEquals(record.getActions().size(), 0);
	}

	@Test
	public void testAuthorizedOnReadRecordType() {
		recordTypeHandlerSpy.isRecordType = true;
		recordTypeHandlerSpy.isAbstract = true;
		recordTypeHandlerSpy.hasLinkedSearch = true;

		String recordType = "recordType";
		DataGroup dataGroup = recordStorage.read(recordType, "recordType");

		DataRecord record = enhancer.enhance(user, recordType, dataGroup);

		recordStorage.MCR.assertParameters("read", 0, "recordType", "recordType");
		DataGroup returnedRecordType = (DataGroup) recordStorage.MCR.getReturnValue("read", 0);

		String metadataId = (String) recordTypeHandlerSpy.MCR.getReturnValue("getMetadataId", 0);

		termCollector.MCR.assertParameters("collectTerms", 0, metadataId, returnedRecordType);
		DataGroup returnedCollectedTermsForRecordType = (DataGroup) termCollector.MCR
				.getReturnValue("collectTerms", 0);

		// authorizator.MCR.assertNumberOfCallsToMethod(
		// "userIsAuthorizedForActionOnRecordTypeAndCollectedData", 12);
		// authorizator.MCR.assertNumberOfCallsToMethod(
		// "checkAndGetUserAuthorizationsForActionOnRecordTypeAndCollectedData", 3);

		assertTrue(record.getActions().contains(Action.READ));
		// authorizator.MCR.assertParameters("userIsAuthorizedForActionOnRecordTypeAndCollectedData",
		// 0, user, "read", "recordType", returnedCollectedTermsForRecordType);
		authorizator.MCR.assertParameters(
				"checkAndGetUserAuthorizationsForActionOnRecordTypeAndCollectedData", 0, user,
				"read", "recordType", returnedCollectedTermsForRecordType, true);

		assertTrue(record.getActions().contains(Action.UPDATE));
		authorizator.MCR.assertParameters(
				"checkAndGetUserAuthorizationsForActionOnRecordTypeAndCollectedData", 1, user,
				"update", "recordType", returnedCollectedTermsForRecordType);

		assertTrue(record.getActions().contains(Action.INDEX));
		authorizator.MCR.assertParameters("userIsAuthorizedForActionOnRecordTypeAndCollectedData",
				0, user, "index", "recordType", returnedCollectedTermsForRecordType);

		assertEquals(record.getActions().size(), 7);
		assertTrue(record.getActions().contains(Action.DELETE));
		assertTrue(record.getActions().contains(Action.VALIDATE));
		assertTrue(record.getActions().contains(Action.SEARCH));
		assertTrue(record.getActions().contains(Action.LIST));

		authorizator.MCR.assertParameters("userIsAuthorizedForActionOnRecordType", 0, user, "list",
				"recordType");
		authorizator.MCR.assertParameters("userIsAuthorizedForActionOnRecordType", 1, user,
				"validate", "recordType");
		authorizator.MCR.assertParameters("userIsAuthorizedForActionOnRecordType", 2, user,
				"search", SOME_RECORD_TYPE);
		authorizator.MCR.assertNumberOfCallsToMethod("userIsAuthorizedForActionOnRecordType", 3);

		termCollector.MCR.assertParameters("collectTerms", 0,
				"fakeMetadataIdFromRecordTypeHandlerSpy", dataGroup);

		recordStorage.MCR.assertNumberOfCallsToMethod("read", 9);

		recordStorage.MCR.assertParameters("read", 0, "recordType", "recordType");
		recordStorage.MCR.assertParameters("read", 1, "search", "someDefaultSearch");
		recordStorage.MCR.assertParameters("read", 2, "recordType", "recordType");
		recordStorage.MCR.assertParameters("read", 3, "metadataGroup", "recordType");
		recordStorage.MCR.assertParameters("read", 4, "presentationGroup", "pgRecordTypeView");
		recordStorage.MCR.assertParameters("read", 5, "presentationGroup", "pgRecordTypeForm");
		recordStorage.MCR.assertParameters("read", 6, "metadataGroup", "recordTypeNew");
		recordStorage.MCR.assertParameters("read", 7, "presentationGroup", "pgRecordTypeFormNew");
		recordStorage.MCR.assertParameters("read", 8, "presentationGroup", "pgRecordTypeList");

		termCollector.MCR.assertNumberOfCallsToMethod("collectTerms", 8);

		termCollector.MCR.assertParameters("collectTerms", 1, metadataId,
				recordStorage.MCR.getReturnValue("read", 2));
		termCollector.MCR.assertParameters("collectTerms", 2, metadataId,
				recordStorage.MCR.getReturnValue("read", 3));

		String methodName = "userIsAuthorizedForActionOnRecordTypeAndCollectedData";
		authorizator.MCR.assertNumberOfCallsToMethod(methodName, 9);

		// authorizator.MCR.assertParameters(methodName, 0, user, "read", "recordType",
		// returnedCollectedTermsForRecordType);
		// authorizator.MCR.assertParameters(methodName, 1, user, "update", "recordType",
		// returnedCollectedTermsForRecordType);
		// authorizator.MCR.assertParameters(methodName, 2, user, "index", "recordType",
		// returnedCollectedTermsForRecordType);
		authorizator.MCR.assertParameters(methodName, 1, user, "delete", "recordType",
				returnedCollectedTermsForRecordType);
		// authorizator.MCR.assertParameters(methodName, 2, user, "validate", "recordType",
		// returnedCollectedTermsForRecordType);

		authorizator.MCR.assertParameters(methodName, 2, user, "read", "recordType",
				termCollector.MCR.getReturnValue("collectTerms", 1));
		authorizator.MCR.assertParameters(methodName, 3, user, "read", "metadataGroup",
				termCollector.MCR.getReturnValue("collectTerms", 2));
		authorizator.MCR.assertParameters(methodName, 4, user, "read", "presentationGroup",
				termCollector.MCR.getReturnValue("collectTerms", 3));
		authorizator.MCR.assertParameters(methodName, 5, user, "read", "presentationGroup",
				termCollector.MCR.getReturnValue("collectTerms", 4));
		authorizator.MCR.assertParameters(methodName, 6, user, "read", "metadataGroup",
				termCollector.MCR.getReturnValue("collectTerms", 5));
		authorizator.MCR.assertParameters(methodName, 7, user, "read", "presentationGroup",
				termCollector.MCR.getReturnValue("collectTerms", 6));
		authorizator.MCR.assertParameters(methodName, 8, user, "read", "presentationGroup",
				termCollector.MCR.getReturnValue("collectTerms", 7));
	}

	@Test
	public void testAuthorizedOnReadRecordTypePlaceWithNoCreateOnRecordTypeRecordType() {
		recordTypeHandlerSpy.isRecordType = true;
		authorizator.setNotAutorizedForActionOnRecordType("create", "place");
		authorizator.setNotAutorizedForActionOnRecordType("list", "place");
		authorizator.setNotAutorizedForActionOnRecordType("search", "place");

		DataGroup dataGroup = recordStorage.read("recordType", "place");
		String recordType = "recordType";
		DataRecord record = enhancer.enhance(user, recordType, dataGroup);
		assertEquals(record.getActions().size(), 5);
		assertTrue(record.getActions().contains(Action.READ));
		assertTrue(record.getActions().contains(Action.UPDATE));
		assertTrue(record.getActions().contains(Action.DELETE));
		assertTrue(record.getActions().contains(Action.INDEX));
		assertTrue(record.getActions().contains(Action.VALIDATE));

		assertFalse(record.getActions().contains(Action.CREATE));
		assertFalse(record.getActions().contains(Action.LIST));

		String methodName = "userIsAuthorizedForActionOnRecordType";
		authorizator.MCR.assertParameters(methodName, 0, user, "create", "place");
		authorizator.MCR.assertParameters(methodName, 1, user, "list", "place");
		authorizator.MCR.assertParameters(methodName, 2, user, "validate", "place");
		authorizator.MCR.assertNumberOfCallsToMethod(methodName, 3);
	}

	@Test
	public void testNotAuthorizedOnReadRecordType() {
		authorizator.authorizedForActionAndRecordType = false;
		authorizator.authorizedForActionAndRecordTypeAndCollectedData = false;

		DataGroup dataGroup = recordStorage.read("recordType", "recordType");
		String recordType = "recordType";
		DataRecord record = enhancer.enhance(user, recordType, dataGroup);
		assertEquals(record.getActions().size(), 0);
	}

	@Test
	public void testReadRecordWithDataRecordLinkHasReadActionTopLevel() {
		String recordType = "dataWithLinks";
		DataGroup dataGroup = recordStorage.read(recordType, "oneLinkTopLevel");
		DataRecord record = enhancer.enhance(user, recordType, dataGroup);
		RecordLinkTestsAsserter.assertTopLevelLinkContainsReadActionOnly(record);
	}

	@Test
	public void testReadRecordWithDataRecordLinkHasNOReadAction() {
		authorizator.setNotAutorizedForActionOnRecordType("create", "toRecordType");
		authorizator.setNotAutorizedForActionOnRecordType("list", "toRecordType");
		authorizator.setNotAutorizedForActionOnRecordType("search", "toRecordType");
		authorizator.setNotAutorizedForActionOnRecordType("read", "toRecordType");

		DataGroup dataGroup = recordStorage.read("dataWithLinks", "oneLinkTopLevelNotAuthorized");
		String recordType = "dataWithLinks";
		DataRecord record = enhancer.enhance(user, recordType, dataGroup);
		RecordLinkTestsAsserter.assertTopLevelLinkDoesNotContainReadAction(record);
	}

	@Test
	public void testReadRecordWithDataRecordLinkHasReadActionOneLevelDown() {
		DataGroup dataGroup = recordStorage.read("dataWithLinks", "oneLinkOneLevelDown");
		String recordType = "dataWithLinks";
		DataRecord record = enhancer.enhance(user, recordType, dataGroup);

		RecordLinkTestsAsserter.assertOneLevelDownLinkContainsReadActionOnly(record);
	}

	@Test
	public void testReadRecordWithDataResourceLinkHasReadActionTopLevel() {
		DataGroup dataGroup = recordStorage.read("dataWithResourceLinks",
				"oneResourceLinkTopLevel");
		String recordType = "dataWithResourceLinks";
		DataRecord record = enhancer.enhance(user, recordType, dataGroup);

		RecordLinkTestsAsserter.assertTopLevelResourceLinkContainsReadActionOnly(record);
	}

	@Test
	public void testReadRecordWithDataResourceLinkHasReadActionOneLevelDown() {
		DataGroup dataGroup = recordStorage.read("dataWithResourceLinks",
				"oneResourceLinkOneLevelDown");
		String recordType = "dataWithResourceLinks";
		DataRecord record = enhancer.enhance(user, recordType, dataGroup);

		RecordLinkTestsAsserter.assertOneLevelDownResourceLinkContainsReadActionOnly(record);
	}

	@Test
	public void testActionsOnReadRecordTypeSearch() {
		recordTypeHandlerSpy.isSearchType = true;
		String recordType = "search";
		DataGroup dataGroup = recordStorage.read(recordType, "aSearchId");
		DataRecord record = enhancer.enhance(user, recordType, dataGroup);
		assertTrue(record.getActions().contains(Action.READ));
		assertTrue(record.getActions().contains(Action.UPDATE));
		assertTrue(record.getActions().contains(Action.DELETE));
		assertTrue(record.getActions().contains(Action.SEARCH));
		assertTrue(record.getActions().contains(Action.INDEX));
		assertEquals(record.getActions().size(), 5);

		String methodName = "checkAndGetUserAuthorizationsForActionOnRecordTypeAndCollectedData";
		authorizator.MCR.assertParameters(methodName, 0, user, "read", "search");
		authorizator.MCR.assertParameters(methodName, 1, user, "update", "search");
		authorizator.MCR.assertNumberOfCallsToMethod(methodName, 2);

		String methodName2 = "userIsAuthorizedForActionOnRecordTypeAndCollectedData";
		authorizator.MCR.assertParameters(methodName2, 0, user, "index", "search");
		authorizator.MCR.assertParameters(methodName2, 1, user, "delete", "search");
		authorizator.MCR.assertNumberOfCallsToMethod(methodName2, 2);

		String methodName3 = "userIsAuthorizedForActionOnRecordType";
		authorizator.MCR.assertParameters(methodName3, 0, user, "search", "place");
		authorizator.MCR.assertNumberOfCallsToMethod(methodName3, 1);
	}

	@Test
	public void testActionsOnReadRecordTypeSearchWhereWeDoNotHaveSearchOnOneRecordTypeToSearchIn() {
		authorizator.setNotAutorizedForActionOnRecordType("search", "image");
		String recordType = "search";
		DataGroup dataGroup = recordStorage.read(recordType, "anotherSearchId");

		DataRecord record = enhancer.enhance(user, recordType, dataGroup);

		assertTrue(record.getActions().contains(Action.READ));
		assertTrue(record.getActions().contains(Action.UPDATE));
		assertTrue(record.getActions().contains(Action.DELETE));
		assertTrue(record.getActions().contains(Action.INDEX));
		assertEquals(record.getActions().size(), 4);
	}

	@Test
	public void testReadRecordWithDataRecordLinkTargetDoesNotExist() {
		DataGroup dataGroup = recordStorage.read("dataWithLinks",
				"oneLinkOneLevelDownTargetDoesNotExist");
		String recordType = "dataWithLinks";
		DataRecord record = enhancer.enhance(user, recordType, dataGroup);

		DataGroup recordDataGroup = record.getDataGroup();
		DataGroup dataGroupOneLevelDown = recordDataGroup
				.getFirstGroupWithNameInData("oneLevelDownTargetDoesNotExist");
		DataLink link = (DataLink) dataGroupOneLevelDown.getFirstChildWithNameInData("link");
		assertFalse(link.getActions().contains(Action.READ));
		assertEquals(link.getActions().size(), 0);
	}

	@Test
	public void testLinkIsNotReadWhenRecordTypeIsPublic() {
		recordTypeHandlerSpy.isPublicForRead = true;
		recordStorage.publicReadForToRecordType = "true";

		DataGroup dataGroup = recordStorage.read("dataWithLinks", "oneLinkTopLevelNotAuthorized");
		String recordType = "dataWithLinks";
		DataRecord record = enhancer.enhance(user, recordType, dataGroup);

		RecordLinkTestsAsserter.assertTopLevelLinkContainsReadActionOnly(record);
		assertRecordStorageWasNOTCalledForReadKey(recordStorage,
				"toRecordType:recordLinkNotAuthorized");

		dependencyProvider.MCR.assertParameters("getRecordTypeHandler", 0, "dataWithLinks");
		dependencyProvider.MCR.assertParameters("getRecordTypeHandler", 1, "toRecordType");
		dependencyProvider.MCR.assertParameters("getRecordTypeHandler", 2, "system");
		dependencyProvider.MCR.assertNumberOfCallsToMethod("getRecordTypeHandler", 3);
	}

	@Test
	public void testRecordTypeForLinkIsOnlyReadOnce() {
		recordTypeHandlerSpy.isPublicForRead = true;
		recordStorage.publicReadForToRecordType = "true";

		String recordType = "dataWithLinks";
		DataGroup dataGroup = recordStorage.read(recordType, "twoLinksTopLevel");
		DataRecord record = enhancer.enhance(user, recordType, dataGroup);

		assertTopLevelTwoLinksContainReadActionOnly(record);

		assertRecordStorageWasNOTCalledForReadKey(recordStorage,
				"toRecordType:recordLinkNotAuthorized");
		// assertRecordStorageWasCalledOnlyOnceForReadKey(recordStorage, "recordType:toRecordType");
		dependencyProvider.MCR.assertParameters("getRecordTypeHandler", 0, "dataWithLinks");
		dependencyProvider.MCR.assertParameters("getRecordTypeHandler", 1, "system");
		dependencyProvider.MCR.assertParameters("getRecordTypeHandler", 2, "toRecordType");
		dependencyProvider.MCR.assertNumberOfCallsToMethod("getRecordTypeHandler", 3);
	}

	@Test
	public void testLinkedRecordForLinkIsOnlyReadOnceForSameLinkedRecord() {
		String recordType = "dataWithLinks";
		DataGroup dataGroup = recordStorage.read(recordType, "twoLinksTopLevel");
		DataRecord record = enhancer.enhance(user, recordType, dataGroup);

		assertTopLevelTwoLinksContainReadActionOnly(record);
		assertRecordStorageWasCalledOnlyOnceForReadKey(recordStorage, "toRecordType:toRecordId");

		String metadataId = (String) recordTypeHandlerSpy.MCR.getReturnValue("getMetadataId", 0);

		termCollector.MCR.assertParameter("collectTerms", 0, "metadataId", metadataId);
		termCollector.MCR.assertParameter("collectTerms", 1, "metadataId", metadataId);
		termCollector.MCR.assertParameter("collectTerms", 2, "metadataId", metadataId);
		termCollector.MCR.assertNumberOfCallsToMethod("collectTerms", 3);

		String methodName = "checkAndGetUserAuthorizationsForActionOnRecordTypeAndCollectedData";
		authorizator.MCR.assertParameters(methodName, 0, user, "read", "dataWithLinks");
		authorizator.MCR.assertParameters(methodName, 1, user, "update", "dataWithLinks");
		authorizator.MCR.assertNumberOfCallsToMethod(methodName, 2);

		String methodName2 = "userIsAuthorizedForActionOnRecordTypeAndCollectedData";
		authorizator.MCR.assertParameters(methodName2, 0, user, "index", "dataWithLinks");
		authorizator.MCR.assertParameters(methodName2, 1, user, "delete", "dataWithLinks");
		authorizator.MCR.assertParameters(methodName2, 2, user, "read", "system");
		authorizator.MCR.assertParameters(methodName2, 3, user, "read", "toRecordType");
		authorizator.MCR.assertNumberOfCallsToMethod(methodName2, 4);
	}

	@Test
	public void testLinkedRecordForLinkIsOnlyReadOnceForSameLinkedRecord2() {
		authorizator.authorizedForActionAndRecordType = false;
		authorizator.authorizedForActionAndRecordTypeAndCollectedData = false;

		String recordType = "dataWithLinks";
		DataGroup dataGroup = recordStorage.read(recordType, "twoLinksTopLevel");
		DataRecord record = enhancer.enhance(user, recordType, dataGroup);

		assertTopLevelTwoLinksDoesNotContainReadAction(record);
		assertRecordStorageWasCalledOnlyOnceForReadKey(recordStorage, "toRecordType:toRecordId");

		String metadataId = (String) recordTypeHandlerSpy.MCR.getReturnValue("getMetadataId", 0);

		termCollector.MCR.assertParameter("collectTerms", 0, "metadataId", metadataId);
		termCollector.MCR.assertParameter("collectTerms", 1, "metadataId", metadataId);
		termCollector.MCR.assertParameter("collectTerms", 2, "metadataId", metadataId);
		termCollector.MCR.assertNumberOfCallsToMethod("collectTerms", 3);

		String methodName = "checkAndGetUserAuthorizationsForActionOnRecordTypeAndCollectedData";
		authorizator.MCR.assertParameters(methodName, 0, user, "read", "dataWithLinks");
		authorizator.MCR.assertParameters(methodName, 1, user, "update", "dataWithLinks");
		authorizator.MCR.assertNumberOfCallsToMethod(methodName, 2);

		String methodName2 = "userIsAuthorizedForActionOnRecordTypeAndCollectedData";
		authorizator.MCR.assertParameters(methodName2, 0, user, "index", "dataWithLinks");
		authorizator.MCR.assertParameters(methodName2, 1, user, "delete", "dataWithLinks");
		authorizator.MCR.assertParameters(methodName2, 2, user, "read", "system");
		authorizator.MCR.assertParameters(methodName2, 3, user, "read", "toRecordType");
		authorizator.MCR.assertNumberOfCallsToMethod(methodName2, 4);
	}

	// WE ARE HERE
	// @Test
	// public void testCallRedactor() throws Exception {
	//
	// DataGroup dataGroup = recordStorage.read(recordType, "twoLinksTopLevel");
	// DataRecord record = enhancer.enhance(user, recordType, dataGroup);
	//
	// dataRedactor.MCR.assertParameters("removeChildrenForConstraintsWithoutPermissions", 0,
	// recordStorage.);
	//
	// }

}
