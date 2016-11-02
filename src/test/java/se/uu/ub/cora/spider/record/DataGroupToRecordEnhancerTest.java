/*
 * Copyright 2016 Uppsala University Library
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

import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import se.uu.ub.cora.beefeater.authentication.User;
import se.uu.ub.cora.bookkeeper.data.DataGroup;
import se.uu.ub.cora.spider.authentication.Authenticator;
import se.uu.ub.cora.spider.authentication.AuthenticatorSpy;
import se.uu.ub.cora.spider.authorization.NeverAuthorisedStub;
import se.uu.ub.cora.spider.authorization.PermissionRuleCalculator;
import se.uu.ub.cora.spider.authorization.SpiderAuthorizator;
import se.uu.ub.cora.spider.data.Action;
import se.uu.ub.cora.spider.data.SpiderDataRecord;
import se.uu.ub.cora.spider.dependency.SpiderDependencyProviderSpy;
import se.uu.ub.cora.spider.record.storage.RecordStorage;
import se.uu.ub.cora.spider.spy.AuthorizatorAlwaysAuthorizedSpy;
import se.uu.ub.cora.spider.spy.NoRulesCalculatorStub;
import se.uu.ub.cora.spider.testdata.TestDataRecordInMemoryStorage;

public class DataGroupToRecordEnhancerTest {
	private RecordStorage recordStorage;
	private Authenticator authenticator;
	private SpiderAuthorizator authorizator;
	private PermissionRuleCalculator keyCalculator;
	private SpiderDependencyProviderSpy dependencyProvider;
	private User user;
	private DataGroupToRecordEnhancer enhancer;

	@BeforeMethod
	public void setUp() {
		user = new User("987654321");
		recordStorage = TestDataRecordInMemoryStorage.createRecordStorageInMemoryWithTestData();
		authenticator = new AuthenticatorSpy();
		authorizator = new AuthorizatorAlwaysAuthorizedSpy();
		keyCalculator = new NoRulesCalculatorStub();
		setUpDependencyProvider();
	}

	private void setUpDependencyProvider() {
		dependencyProvider = new SpiderDependencyProviderSpy();
		dependencyProvider.authenticator = authenticator;
		dependencyProvider.spiderAuthorizator = authorizator;
		dependencyProvider.recordStorage = recordStorage;
		dependencyProvider.keyCalculator = keyCalculator;
		enhancer = new DataGroupToRecordEnhancerImp(dependencyProvider);
	}

	@Test
	public void testAllDataIndependentActions() {
		User user = new User("987654321");
		String recordType = "place";
		DataGroup dataGroup = recordStorage.read("place", "place:0001");
		SpiderDataRecord record = enhancer.enhance(user, recordType, dataGroup);

		assertEquals(record.getActions().size(), 3);
		assertTrue(record.getActions().contains(Action.READ));
		assertTrue(record.getActions().contains(Action.UPDATE));

		assertTrue(record.getActions().contains(Action.READ_INCOMING_LINKS));
	}

	@Test
	public void testAuthorizedToDeleteAndNoIncomingLink() {
		DataGroup dataGroup = recordStorage.read("place", "place:0002");
		String recordType = "place";
		SpiderDataRecord record = enhancer.enhance(user, recordType, dataGroup);
		assertEquals(record.getActions().size(), 3);
		assertTrue(record.getActions().contains(Action.READ));
		assertTrue(record.getActions().contains(Action.UPDATE));
		assertTrue(record.getActions().contains(Action.DELETE));

		assertFalse(record.getActions().contains(Action.READ_INCOMING_LINKS));
	}

	@Test
	public void testNotAuthorizedToDeleteAndNoIncomingLink() {
		authorizator = new NeverAuthorisedStub();
		setUpDependencyProvider();

		DataGroup dataGroup = recordStorage.read("place", "place:0002");
		String recordType = "place";
		SpiderDataRecord record = enhancer.enhance(user, recordType, dataGroup);
		assertEquals(record.getActions().size(), 0);
	}

	@Test
	public void testAuthorizedToDeleteAndIncomingLink() {
		DataGroup dataGroup = recordStorage.read("place", "place:0001");
		String recordType = "place";
		SpiderDataRecord record = enhancer.enhance(user, recordType, dataGroup);
		assertEquals(record.getActions().size(), 3);
		assertTrue(record.getActions().contains(Action.READ));
		assertTrue(record.getActions().contains(Action.UPDATE));
		assertTrue(record.getActions().contains(Action.READ_INCOMING_LINKS));

		assertFalse(record.getActions().contains(Action.DELETE));
	}

	@Test
	public void testNotAuthorizedToDeleteAndIncomingLink() {
		authorizator = new NeverAuthorisedStub();
		setUpDependencyProvider();

		String recordType = "place";
		DataGroup dataGroup = recordStorage.read("place", "place:0001");
		SpiderDataRecord record = enhancer.enhance(user, recordType, dataGroup);

		assertEquals(record.getActions().size(), 1);
		assertTrue(record.getActions().contains(Action.READ_INCOMING_LINKS));
	}

	@Test
	public void testActionsOnReadRecordTypeBinary() {
		DataGroup dataGroup = recordStorage.read("recordType", "binary");
		String recordType = "recordType";
		SpiderDataRecord record = enhancer.enhance(user, recordType, dataGroup);
		assertEquals(record.getActions().size(), 5);
		assertTrue(record.getActions().contains(Action.READ));
		assertTrue(record.getActions().contains(Action.UPDATE));
		assertTrue(record.getActions().contains(Action.DELETE));
		assertTrue(record.getActions().contains(Action.LIST));
		assertTrue(record.getActions().contains(Action.SEARCH));

		assertFalse(record.getActions().contains(Action.UPLOAD));

	}

	@Test
	public void testActionsOnReadRecordTypeImage() {
		DataGroup dataGroup = recordStorage.read("recordType", "image");
		String recordType = "recordType";
		SpiderDataRecord record = enhancer.enhance(user, recordType, dataGroup);
		assertEquals(record.getActions().size(), 6);
		assertTrue(record.getActions().contains(Action.READ));
		assertTrue(record.getActions().contains(Action.UPDATE));
		assertTrue(record.getActions().contains(Action.DELETE));
		assertTrue(record.getActions().contains(Action.CREATE));
		assertTrue(record.getActions().contains(Action.LIST));
		assertTrue(record.getActions().contains(Action.SEARCH));

		assertFalse(record.getActions().contains(Action.UPLOAD));

	}

	@Test
	public void testAuthorizedOnReadImage() {
		DataGroup dataGroup = recordStorage.read("image", "image:0001");
		String recordType = "image";
		SpiderDataRecord record = enhancer.enhance(user, recordType, dataGroup);
		assertEquals(record.getActions().size(), 4);
		assertTrue(record.getActions().contains(Action.READ));
		assertTrue(record.getActions().contains(Action.UPDATE));
		assertTrue(record.getActions().contains(Action.DELETE));
		assertTrue(record.getActions().contains(Action.UPLOAD));
	}

	@Test
	public void testNotAuthorizedOnReadImage() {
		authorizator = new NeverAuthorisedStub();
		setUpDependencyProvider();
		DataGroup dataGroup = recordStorage.read("image", "image:0001");
		String recordType = "image";
		SpiderDataRecord record = enhancer.enhance(user, recordType, dataGroup);
		assertEquals(record.getActions().size(), 0);
	}

	@Test
	public void testAuthorizedOnReadRecordType() {
		DataGroup dataGroup = recordStorage.read("recordType", "recordType");
		String recordType = "recordType";
		SpiderDataRecord record = enhancer.enhance(user, recordType, dataGroup);
		assertEquals(record.getActions().size(), 6);
		assertTrue(record.getActions().contains(Action.READ));
		assertTrue(record.getActions().contains(Action.UPDATE));
		assertTrue(record.getActions().contains(Action.DELETE));

		assertTrue(record.getActions().contains(Action.CREATE));
		assertTrue(record.getActions().contains(Action.LIST));
		assertTrue(record.getActions().contains(Action.SEARCH));
	}

	@Test
	public void testNotAuthorizedOnReadRecordType() {
		authorizator = new NeverAuthorisedStub();
		setUpDependencyProvider();
		DataGroup dataGroup = recordStorage.read("recordType", "recordType");
		String recordType = "recordType";
		SpiderDataRecord record = enhancer.enhance(user, recordType, dataGroup);
		assertEquals(record.getActions().size(), 0);
	}

	@Test
	public void testReadRecordWithDataRecordLinkHasReadActionTopLevel() {
		recordStorage = new RecordLinkTestsRecordStorage();
		setUpDependencyProvider();
		DataGroup dataGroup = recordStorage.read("dataWithLinks", "oneLinkTopLevel");
		String recordType = "dataWithLinks";
		SpiderDataRecord record = enhancer.enhance(user, recordType, dataGroup);
		RecordLinkTestsAsserter.assertTopLevelLinkContainsReadActionOnly(record);
	}

	@Test
	public void testReadRecordWithDataRecordLinkHasReadActionOneLevelDown() {
		recordStorage = new RecordLinkTestsRecordStorage();
		setUpDependencyProvider();

		DataGroup dataGroup = recordStorage.read("dataWithLinks", "oneLinkOneLevelDown");
		String recordType = "dataWithLinks";
		SpiderDataRecord record = enhancer.enhance(user, recordType, dataGroup);

		RecordLinkTestsAsserter.assertOneLevelDownLinkContainsReadActionOnly(record);
	}

	@Test
	public void testReadRecordWithDataResourceLinkHasReadActionTopLevel() {
		recordStorage = new RecordLinkTestsRecordStorage();
		setUpDependencyProvider();
		DataGroup dataGroup = recordStorage.read("dataWithResourceLinks",
				"oneResourceLinkTopLevel");
		String recordType = "dataWithResourceLinks";
		SpiderDataRecord record = enhancer.enhance(user, recordType, dataGroup);

		RecordLinkTestsAsserter.assertTopLevelResourceLinkContainsReadActionOnly(record);
	}

	@Test
	public void testReadRecordWithDataResourceLinkHasReadActionOneLevelDown() {
		recordStorage = new RecordLinkTestsRecordStorage();
		setUpDependencyProvider();

		DataGroup dataGroup = recordStorage.read("dataWithResourceLinks",
				"oneResourceLinkOneLevelDown");
		String recordType = "dataWithResourceLinks";
		SpiderDataRecord record = enhancer.enhance(user, recordType, dataGroup);

		RecordLinkTestsAsserter.assertOneLevelDownResourceLinkContainsReadActionOnly(record);
	}

}
