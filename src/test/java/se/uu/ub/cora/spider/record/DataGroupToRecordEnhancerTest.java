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
import java.util.List;

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
import se.uu.ub.cora.spider.data.DataGroupFactorySpy;
import se.uu.ub.cora.spider.data.DataRecordFactorySpy;
import se.uu.ub.cora.spider.dependency.RecordStorageProviderSpy;
import se.uu.ub.cora.spider.dependency.SpiderDependencyProviderSpy;
import se.uu.ub.cora.spider.log.LoggerFactorySpy;
import se.uu.ub.cora.spider.spy.DataGroupTermCollectorSpy;
import se.uu.ub.cora.spider.spy.NoRulesCalculatorStub;
import se.uu.ub.cora.spider.spy.SpiderAuthorizatorSpy;

public class DataGroupToRecordEnhancerTest {
	private RecordEnhancerTestsRecordStorage recordStorage;
	private AuthenticatorSpy authenticator;
	private SpiderAuthorizatorSpy authorizator;
	private PermissionRuleCalculator keyCalculator;
	private SpiderDependencyProviderSpy dependencyProvider;
	private User user;
	private DataGroupToRecordEnhancer enhancer;
	private DataGroupTermCollectorSpy termCollector;
	private LoggerFactorySpy loggerFactorySpy;
	private DataGroupFactory dataGroupFactorySpy;
	private DataAtomicFactory dataAtomicFactorySpy;
	private DataRecordFactory dataRecordFactorySpy;

	@BeforeMethod
	public void setUp() {
		setUpFactoriesAndProviders();
		user = new User("987654321");
		recordStorage = new RecordEnhancerTestsRecordStorage();
		authenticator = new AuthenticatorSpy();
		authorizator = new SpiderAuthorizatorSpy();
		keyCalculator = new NoRulesCalculatorStub();
		termCollector = new DataGroupTermCollectorSpy();
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
		dependencyProvider.ruleCalculator = keyCalculator;
		dependencyProvider.searchTermCollector = termCollector;
		enhancer = new DataGroupToRecordEnhancerImp(dependencyProvider);
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

	@Test
	public void testAuthorizedToDeleteAndIncomingLinkToAbstractParent() {
		DataGroup dataGroup = recordStorage.read("place", "place:0003");
		String recordType = "place";

		DataRecord record = enhancer.enhance(user, recordType, dataGroup);
		assertEquals(record.getActions().size(), 4);
		assertTrue(record.getActions().contains(Action.READ));
		assertTrue(record.getActions().contains(Action.UPDATE));
		assertTrue(record.getActions().contains(Action.READ_INCOMING_LINKS));
		assertTrue(record.getActions().contains(Action.INDEX));

		assertFalse(record.getActions().contains(Action.DELETE));

	}

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
		DataGroup dataGroup = recordStorage.read("recordType", "recordType");
		String recordType = "recordType";
		DataRecord record = enhancer.enhance(user, recordType, dataGroup);
		assertEquals(record.getActions().size(), 7);
		assertTrue(record.getActions().contains(Action.READ));
		assertTrue(record.getActions().contains(Action.UPDATE));
		assertTrue(record.getActions().contains(Action.DELETE));
		assertTrue(record.getActions().contains(Action.INDEX));
		assertTrue(record.getActions().contains(Action.VALIDATE));
		assertTrue(record.getActions().contains(Action.SEARCH));

		assertTrue(record.getActions().contains(Action.LIST));

		authorizator.TCR.assertParameters("userIsAuthorizedForActionOnRecordType", 0, user, "list",
				"recordType");
		authorizator.TCR.assertParameters("userIsAuthorizedForActionOnRecordType", 1, user,
				"search", "someRecordType");
		authorizator.TCR.assertNumberOfCallsToMethod("userIsAuthorizedForActionOnRecordType", 2);

		List<DataGroup> expectedCollectedTerms = termCollector.returnedCollectedTerms;

		String methodName = "userIsAuthorizedForActionOnRecordTypeAndCollectedData";
		authorizator.TCR.assertParameters(methodName, 0, user, "read", "recordType",
				expectedCollectedTerms.get(0));
		authorizator.TCR.assertParameters(methodName, 1, user, "update", "recordType",
				expectedCollectedTerms.get(0));
		authorizator.TCR.assertParameters(methodName, 2, user, "index", "recordType",
				expectedCollectedTerms.get(0));
		authorizator.TCR.assertParameters(methodName, 3, user, "delete", "recordType",
				expectedCollectedTerms.get(0));
		authorizator.TCR.assertParameters(methodName, 4, user, "validate", "recordType",
				expectedCollectedTerms.get(0));

		authorizator.TCR.assertParameters(methodName, 5, user, "read", "recordType",
				expectedCollectedTerms.get(1));
		authorizator.TCR.assertParameters(methodName, 6, user, "read", "metadataGroup",
				expectedCollectedTerms.get(2));
		authorizator.TCR.assertParameters(methodName, 7, user, "read", "presentationGroup",
				expectedCollectedTerms.get(3));
		authorizator.TCR.assertParameters(methodName, 8, user, "read", "presentationGroup",
				expectedCollectedTerms.get(4));
		authorizator.TCR.assertParameters(methodName, 9, user, "read", "metadataGroup",
				expectedCollectedTerms.get(5));
		authorizator.TCR.assertParameters(methodName, 10, user, "read", "presentationGroup",
				expectedCollectedTerms.get(6));
		authorizator.TCR.assertParameters(methodName, 11, user, "read", "presentationGroup",
				expectedCollectedTerms.get(7));
		authorizator.TCR.assertNumberOfCallsToMethod(methodName, 12);

		assertEquals(termCollector.metadataId, "dataWithLinks");
		assertEquals(termCollector.dataGroups.get(0), dataGroup);
	}

	@Test
	public void testAuthorizedOnReadRecordTypePlaceWithNoCreateOnRecordTypeRecordType() {
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
		authorizator.TCR.assertParameters(methodName, 0, user, "create", "place");
		authorizator.TCR.assertParameters(methodName, 1, user, "list", "place");
		authorizator.TCR.assertNumberOfCallsToMethod(methodName, 2);
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
		String recordType = "search";
		DataGroup dataGroup = recordStorage.read(recordType, "aSearchId");
		DataRecord record = enhancer.enhance(user, recordType, dataGroup);
		assertTrue(record.getActions().contains(Action.READ));
		assertTrue(record.getActions().contains(Action.UPDATE));
		assertTrue(record.getActions().contains(Action.DELETE));
		assertTrue(record.getActions().contains(Action.SEARCH));
		assertTrue(record.getActions().contains(Action.INDEX));
		assertEquals(record.getActions().size(), 5);

		String methodName = "userIsAuthorizedForActionOnRecordTypeAndCollectedData";
		authorizator.TCR.assertParameters(methodName, 0, user, "read", "search");
		authorizator.TCR.assertParameters(methodName, 1, user, "update", "search");
		authorizator.TCR.assertParameters(methodName, 2, user, "index", "search");
		authorizator.TCR.assertParameters(methodName, 3, user, "delete", "search");
		authorizator.TCR.assertNumberOfCallsToMethod(methodName, 4);

		String methodName2 = "userIsAuthorizedForActionOnRecordType";
		authorizator.TCR.assertParameters(methodName2, 0, user, "search", "place");
		authorizator.TCR.assertNumberOfCallsToMethod(methodName2, 1);
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
		recordStorage.publicReadForToRecordType = "true";

		DataGroup dataGroup = recordStorage.read("dataWithLinks", "oneLinkTopLevelNotAuthorized");
		String recordType = "dataWithLinks";
		DataRecord record = enhancer.enhance(user, recordType, dataGroup);

		RecordLinkTestsAsserter.assertTopLevelLinkContainsReadActionOnly(record);
		assertRecordStorageWasNOTCalledForReadKey(recordStorage,
				"toRecordType:recordLinkNotAuthorized");
		assertRecordStorageWasCalledOnlyOnceForReadKey(recordStorage, "recordType:toRecordType");
	}

	@Test
	public void testRecordTypeForLinkIsOnlyReadOnce() {
		recordStorage.publicReadForToRecordType = "true";

		String recordType = "dataWithLinks";
		DataGroup dataGroup = recordStorage.read(recordType, "twoLinksTopLevel");
		DataRecord record = enhancer.enhance(user, recordType, dataGroup);

		assertTopLevelTwoLinksContainReadActionOnly(record);

		assertRecordStorageWasNOTCalledForReadKey(recordStorage,
				"toRecordType:recordLinkNotAuthorized");
		assertRecordStorageWasCalledOnlyOnceForReadKey(recordStorage, "recordType:toRecordType");
	}

	@Test
	public void testLinkedRecordForLinkIsOnlyReadOnceForSameLinkedRecord() {
		String recordType = "dataWithLinks";
		DataGroup dataGroup = recordStorage.read(recordType, "twoLinksTopLevel");
		DataRecord record = enhancer.enhance(user, recordType, dataGroup);

		assertTopLevelTwoLinksContainReadActionOnly(record);
		assertRecordStorageWasCalledOnlyOnceForReadKey(recordStorage, "toRecordType:toRecordId");
		assertEquals(termCollector.metadataIdsReadNumberOfTimesMap.get("toRecordType").intValue(),
				1);

		String methodName = "userIsAuthorizedForActionOnRecordTypeAndCollectedData";
		authorizator.TCR.assertParameters(methodName, 0, user, "read", "dataWithLinks");
		authorizator.TCR.assertParameters(methodName, 1, user, "update", "dataWithLinks");
		authorizator.TCR.assertParameters(methodName, 2, user, "index", "dataWithLinks");
		authorizator.TCR.assertParameters(methodName, 3, user, "delete", "dataWithLinks");
		authorizator.TCR.assertParameters(methodName, 4, user, "read", "system");
		authorizator.TCR.assertParameters(methodName, 5, user, "read", "toRecordType");
		authorizator.TCR.assertNumberOfCallsToMethod(methodName, 6);
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
		assertEquals(termCollector.metadataIdsReadNumberOfTimesMap.get("toRecordType").intValue(),
				1);

		String methodName = "userIsAuthorizedForActionOnRecordTypeAndCollectedData";
		authorizator.TCR.assertParameters(methodName, 0, user, "read", "dataWithLinks");
		authorizator.TCR.assertParameters(methodName, 1, user, "update", "dataWithLinks");
		authorizator.TCR.assertParameters(methodName, 2, user, "index", "dataWithLinks");
		authorizator.TCR.assertParameters(methodName, 3, user, "delete", "dataWithLinks");
		authorizator.TCR.assertParameters(methodName, 4, user, "read", "system");
		authorizator.TCR.assertParameters(methodName, 5, user, "read", "toRecordType");
		authorizator.TCR.assertNumberOfCallsToMethod(methodName, 6);
	}

}
