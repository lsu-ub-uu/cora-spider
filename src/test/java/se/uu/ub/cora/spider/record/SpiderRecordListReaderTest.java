/*
 * Copyright 2015, 2016 Uppsala University Library
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

import java.util.List;

import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import se.uu.ub.cora.beefeater.Authorizator;
import se.uu.ub.cora.beefeater.AuthorizatorImp;
import se.uu.ub.cora.spider.authentication.AuthenticationException;
import se.uu.ub.cora.spider.authentication.Authenticator;
import se.uu.ub.cora.spider.authentication.AuthenticatorSpy;
import se.uu.ub.cora.spider.authorization.AuthorizationException;
import se.uu.ub.cora.spider.authorization.NeverAuthorisedStub;
import se.uu.ub.cora.spider.authorization.PermissionRuleCalculator;
import se.uu.ub.cora.spider.data.Action;
import se.uu.ub.cora.spider.data.SpiderData;
import se.uu.ub.cora.spider.data.SpiderDataList;
import se.uu.ub.cora.spider.data.SpiderDataRecord;
import se.uu.ub.cora.spider.dependency.SpiderDependencyProviderSpy;
import se.uu.ub.cora.spider.dependency.SpiderInstanceFactory;
import se.uu.ub.cora.spider.dependency.SpiderInstanceFactoryImp;
import se.uu.ub.cora.spider.dependency.SpiderInstanceProvider;
import se.uu.ub.cora.spider.record.storage.RecordStorage;
import se.uu.ub.cora.spider.spy.RecordPermissionKeyCalculatorStub;
import se.uu.ub.cora.spider.spy.RecordStorageSpy;
import se.uu.ub.cora.spider.testdata.TestDataRecordInMemoryStorage;

public class SpiderRecordListReaderTest {

	private RecordStorage recordStorage;
	private Authenticator authenticator;
	private Authorizator authorizator;
	private PermissionRuleCalculator keyCalculator;
	private SpiderDependencyProviderSpy dependencyProvider;
	private SpiderRecordListReader recordListReader;

	@BeforeMethod
	public void beforeMethod() {
		authenticator = new AuthenticatorSpy();
		authorizator = new AuthorizatorImp();
		recordStorage = TestDataRecordInMemoryStorage.createRecordStorageInMemoryWithTestData();
		keyCalculator = new RecordPermissionKeyCalculatorStub();
		setUpDependencyProvider();
	}

	private void setUpDependencyProvider() {
		dependencyProvider = new SpiderDependencyProviderSpy();
		dependencyProvider.authenticator = authenticator;
		dependencyProvider.authorizator = authorizator;
		dependencyProvider.recordStorage = recordStorage;
		dependencyProvider.keyCalculator = keyCalculator;
		SpiderInstanceFactory factory = SpiderInstanceFactoryImp
				.usingDependencyProvider(dependencyProvider);
		SpiderInstanceProvider.setSpiderInstanceFactory(factory);
		recordListReader = SpiderRecordListReaderImp.usingDependencyProvider(dependencyProvider);
	}

	@Test(expectedExceptions = AuthenticationException.class)
	public void testAuthenticationNotAuthenticated() {
		recordStorage = new RecordStorageSpy();
		setUpDependencyProvider();
		recordListReader.readRecordList("dummyNonAuthenticatedToken", "spyType");
	}

	@Test
	public void testReadListAuthorized() {
		String userId = "someToken78678567";
		String type = "place";
		SpiderDataList readRecordList = recordListReader.readRecordList(userId, type);
		assertEquals(readRecordList.getTotalNumberOfTypeInStorage(), "2",
				"Total number of records should be 2");
		assertEquals(readRecordList.getFromNo(), "1");
		assertEquals(readRecordList.getToNo(), "2");
		List<SpiderData> records = readRecordList.getDataList();
		SpiderDataRecord spiderDataRecord = (SpiderDataRecord) records.iterator().next();
		assertNotNull(spiderDataRecord);
	}

	@Test
	public void testReadListAbstractRecordType() {
		recordStorage = new RecordStorageSpy();
		setUpDependencyProvider();
		recordListReader.readRecordList("someToken78678567", "abstract");

		assertTrue(((RecordStorageSpy) recordStorage).readLists.contains("child1"));
		assertTrue(((RecordStorageSpy) recordStorage).readLists.contains("child2"));
	}

	@Test
	public void testReadListAbstractRecordTypeNoDataForOneRecordType() {
		recordStorage = new RecordStorageSpy();
		setUpDependencyProvider();

		recordListReader.readRecordList("someToken78678567", "abstract2");
		assertFalse(((RecordStorageSpy) recordStorage).readLists.contains("child1_2"));
		assertTrue(((RecordStorageSpy) recordStorage).readLists.contains("child2_2"));
	}

	@Test(expectedExceptions = AuthorizationException.class)
	public void testReadListUnauthorized() {
		authorizator = new NeverAuthorisedStub();
		setUpDependencyProvider();
		recordListReader.readRecordList("someToken78678567", "place");
	}

	@Test
	public void testActionsOnReadRecord() {
		SpiderDataList recordList = recordListReader.readRecordList("someToken78678567", "place");

		SpiderDataRecord recordWithIncomingLinks = (SpiderDataRecord) recordList.getDataList()
				.get(0);
		assertEquals(recordWithIncomingLinks.getActions().size(), 3);
		assertTrue(recordWithIncomingLinks.getActions().contains(Action.READ_INCOMING_LINKS));
		assertFalse(recordWithIncomingLinks.getActions().contains(Action.DELETE));

		SpiderDataRecord recordWithNoIncomingLinks = (SpiderDataRecord) recordList.getDataList()
				.get(1);
		assertEquals(recordWithNoIncomingLinks.getActions().size(), 3);
		assertTrue(recordWithNoIncomingLinks.getActions().contains(Action.DELETE));
		assertFalse(recordWithNoIncomingLinks.getActions().contains(Action.READ_INCOMING_LINKS));
	}

	@Test
	public void testActionsOnReadRecordType() {
		SpiderDataList recordList = recordListReader.readRecordList("someToken78678567",
				"recordType");
		SpiderDataRecord firstInListWhichIsImage = (SpiderDataRecord) recordList.getDataList()
				.get(0);
		assertEquals(firstInListWhichIsImage.getActions().size(), 6);
		assertTrue(firstInListWhichIsImage.getActions().contains(Action.READ));
		assertTrue(firstInListWhichIsImage.getActions().contains(Action.UPDATE));
		assertTrue(firstInListWhichIsImage.getActions().contains(Action.DELETE));
		assertTrue(firstInListWhichIsImage.getActions().contains(Action.CREATE));
		assertTrue(firstInListWhichIsImage.getActions().contains(Action.LIST));
		assertTrue(firstInListWhichIsImage.getActions().contains(Action.SEARCH));

		SpiderDataRecord secondInListWhichIsMetadata = (SpiderDataRecord) recordList.getDataList()
				.get(1);
		assertEquals(secondInListWhichIsMetadata.getActions().size(), 6);
		assertTrue(secondInListWhichIsMetadata.getActions().contains(Action.READ));
		assertTrue(secondInListWhichIsMetadata.getActions().contains(Action.UPDATE));
		assertTrue(secondInListWhichIsMetadata.getActions().contains(Action.DELETE));
		assertTrue(secondInListWhichIsMetadata.getActions().contains(Action.CREATE));
		assertTrue(secondInListWhichIsMetadata.getActions().contains(Action.LIST));
		assertTrue(secondInListWhichIsMetadata.getActions().contains(Action.SEARCH));
	}

	@Test
	public void testActionsOnReadRecordNoIncomingLinks() {
		SpiderDataList recordList = recordListReader.readRecordList("someToken78678567", "place");
		assertEquals(((SpiderDataRecord) recordList.getDataList().get(1)).getActions().size(), 3);
		assertFalse(((SpiderDataRecord) recordList.getDataList().get(1)).getActions()
				.contains(Action.READ_INCOMING_LINKS));
	}
}
