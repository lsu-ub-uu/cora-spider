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
import static org.testng.Assert.assertNotNull;
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
	public void testReadActionNotAuthorized() throws Exception {
		createRecordStorageSpy();
		authorizator.authorizedForActionAndRecordTypeAndCollectedData = false;
		Exception auxiliaryException = null;
		try {
			enhancer.enhance(user, SOME_RECORD_TYPE, someDataGroup);
		} catch (AuthorizationException thrownException) {
			auxiliaryException = thrownException;
		}
		assertNotNull(auxiliaryException);
		DataGroup returnedCollectedTermsForRecordType = getAssertedCollectedTermsForRecordType(
				SOME_RECORD_TYPE, someDataGroup);
		authorizator.MCR.assertParameters(
				"checkAndGetUserAuthorizationsForActionOnRecordTypeAndCollectedData", 0, user,
				"read", SOME_RECORD_TYPE, returnedCollectedTermsForRecordType, true);
		authorizator.MCR.assertNumberOfCallsToMethod(
				"checkAndGetUserAuthorizationsForActionOnRecordTypeAndCollectedData", 1);
		assertTrue(auxiliaryException instanceof AuthorizationException);
	}

	@Test
	public void testReadActionNotAuthorizedButPublicData() throws Exception {
		createRecordStorageSpy();
		recordTypeHandlerSpy.isPublicForRead = true;
		authorizator.authorizedForActionAndRecordTypeAndCollectedData = false;

		DataRecord record = enhancer.enhance(user, SOME_RECORD_TYPE, someDataGroup);
		recordTypeHandlerSpy.MCR.assertMethodWasCalled("isPublicForRead");
		authorizator.MCR.assertNumberOfCallsToMethod(
				"checkAndGetUserAuthorizationsForActionOnRecordTypeAndCollectedData", 1);
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
		authorizator.setNotAutorizedForAction("update");

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
		authorizator.setNotAutorizedForAction("index");

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
		authorizator.setNotAutorizedForAction("delete");

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
		authorizator.setNotAutorizedForAction("upload");

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
		recordTypeHandlerSpy.representsTheRecordTypeDefiningSearches = true;
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
		recordTypeHandlerSpy.representsTheRecordTypeDefiningSearches = true;
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
		RecordTypeHandlerSpy recordTypeHandlerForRecordTypeInData = dependencyProvider
				.createRecordTypeHandlerSpy("someId");
		recordTypeHandlerSpy.representsTheRecordTypeDefiningRecordTypes = true;

		DataRecord record = enhancer.enhance(user, SOME_RECORD_TYPE, someDataGroup);

		recordTypeHandlerSpy.MCR
				.assertMethodWasCalled("representsTheRecordTypeDefiningRecordTypes");
		dependencyProvider.MCR.assertNumberOfCallsToMethod("getRecordTypeHandler", 2);
		dependencyProvider.MCR.assertParameters("getRecordTypeHandler", 1, "someId");

		recordTypeHandlerForRecordTypeInData.MCR.assertMethodWasCalled("isAbstract");
		authorizator.MCR.assertParameters("userIsAuthorizedForActionOnRecordType", 0, user,
				"create", "someId");
		assertTrue(record.getActions().contains(Action.CREATE));
	}

	@Test
	public void testCreateActionForRecordTypeRecordTypeNotAuthorized() throws Exception {
		createRecordStorageSpy();
		someDataGroup = createDummyDataGroupForRecord("otherId");
		recordTypeHandlerSpy.representsTheRecordTypeDefiningRecordTypes = true;
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
		recordTypeHandlerSpy.representsTheRecordTypeDefiningRecordTypes = true;
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
		recordTypeHandlerSpy.representsTheRecordTypeDefiningRecordTypes = true;

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
		recordTypeHandlerSpy.representsTheRecordTypeDefiningRecordTypes = true;
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
		recordTypeHandlerSpy.representsTheRecordTypeDefiningRecordTypes = true;

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
		recordTypeHandlerSpy.representsTheRecordTypeDefiningRecordTypes = true;
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

	@Test
	public void testSearchActionForRecordTypeRecordType() throws Exception {
		RecordStorageSpy recordStorage = createRecordStorageSpy();
		createAndSetReturnDataGroupForReadInStorageForReadOfLinkedSearch(recordStorage);
		recordTypeHandlerSpy.representsTheRecordTypeDefiningRecordTypes = true;

		RecordTypeHandlerSpy recordTypeHandlerForRecordTypeInData = dependencyProvider
				.createRecordTypeHandlerSpy("someId");
		recordTypeHandlerForRecordTypeInData.hasLinkedSearch = true;

		DataRecord record = enhancer.enhance(user, SOME_RECORD_TYPE, someDataGroup);

		recordTypeHandlerSpy.MCR
				.assertMethodWasCalled("representsTheRecordTypeDefiningRecordTypes");
		dependencyProvider.MCR.assertNumberOfCallsToMethod("getRecordTypeHandler", 2);
		dependencyProvider.MCR.assertParameters("getRecordTypeHandler", 1, "someId");
		dependencyProvider.MCR.assertReturn("getRecordTypeHandler", 1,
				recordTypeHandlerForRecordTypeInData);

		recordTypeHandlerForRecordTypeInData.MCR.assertMethodWasCalled("hasLinkedSearch");
		String returnedSearchId = (String) recordTypeHandlerForRecordTypeInData.MCR
				.getReturnValue("getSearchId", 0);

		recordStorage.MCR.assertParameters("read", 0, "search", returnedSearchId);
		authorizator.MCR.assertParameters("userIsAuthorizedForActionOnRecordType", 3, user,
				"search", "linkedSearchId1");
		authorizator.MCR.assertParameters("userIsAuthorizedForActionOnRecordType", 4, user,
				"search", "linkedSearchId2");
		authorizator.MCR.assertNumberOfCallsToMethod("userIsAuthorizedForActionOnRecordType", 5);
		assertTrue(record.getActions().contains(Action.SEARCH));
	}

	@Test
	public void testSearchActionForRecordTypeRecordTypeNotAuthorized() throws Exception {
		authorizator.authorizedForActionAndRecordType = false;
		RecordStorageSpy recordStorage = createRecordStorageSpy();
		createAndSetReturnDataGroupForReadInStorageForReadOfLinkedSearch(recordStorage);
		recordTypeHandlerSpy.representsTheRecordTypeDefiningRecordTypes = true;

		RecordTypeHandlerSpy recordTypeHandlerForRecordTypeInData = dependencyProvider
				.createRecordTypeHandlerSpy("someId");
		recordTypeHandlerForRecordTypeInData.hasLinkedSearch = true;

		DataRecord record = enhancer.enhance(user, SOME_RECORD_TYPE, someDataGroup);

		recordTypeHandlerSpy.MCR
				.assertMethodWasCalled("representsTheRecordTypeDefiningRecordTypes");
		dependencyProvider.MCR.assertNumberOfCallsToMethod("getRecordTypeHandler", 2);
		dependencyProvider.MCR.assertParameters("getRecordTypeHandler", 1, "someId");
		dependencyProvider.MCR.assertReturn("getRecordTypeHandler", 1,
				recordTypeHandlerForRecordTypeInData);

		recordTypeHandlerForRecordTypeInData.MCR.assertMethodWasCalled("hasLinkedSearch");
		String returnedSearchId = (String) recordTypeHandlerForRecordTypeInData.MCR
				.getReturnValue("getSearchId", 0);

		recordStorage.MCR.assertParameters("read", 0, "search", returnedSearchId);
		authorizator.MCR.assertParameters("userIsAuthorizedForActionOnRecordType", 3, user,
				"search", "linkedSearchId1");
		authorizator.MCR.assertNumberOfCallsToMethod("userIsAuthorizedForActionOnRecordType", 4);
		assertFalse(record.getActions().contains(Action.SEARCH));
	}

	private void createAndSetReturnDataGroupForReadInStorageForReadOfLinkedSearch(
			RecordStorageSpy recordStorage) {
		DataGroupSpy searchGroupLinkedFromRecordType = new DataGroupSpy("someSearchId");
		DataGroup recordTypeToSearchIn1 = new DataGroupSpy("recordTypeToSearchIn");
		searchGroupLinkedFromRecordType.addChild(recordTypeToSearchIn1);
		recordTypeToSearchIn1.addChild(new DataAtomicSpy("linkedRecordId", "linkedSearchId1"));
		DataGroup recordTypeToSearchIn2 = new DataGroupSpy("recordTypeToSearchIn");
		searchGroupLinkedFromRecordType.addChild(recordTypeToSearchIn2);
		recordTypeToSearchIn2.addChild(new DataAtomicSpy("linkedRecordId", "linkedSearchId2"));
		recordStorage.returnForRead = searchGroupLinkedFromRecordType;
	}

	// TODO: we are here
	// TODO: If NO READ permissions, throw exception.inte här men principiellt... :)

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
	public void testLinkedRecordForLinkIsOnlyReadOnceForSameLinkedRecordNotAuthorized() {
		String recordType = "dataWithLinks";

		authorizator.setNotAutorizedForActionOnRecordType("read", "system");
		authorizator.setNotAutorizedForActionOnRecordType("read", "toRecordType");

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
}
