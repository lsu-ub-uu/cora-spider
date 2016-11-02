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

package se.uu.ub.cora.spider.authorization;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertTrue;

import java.util.ArrayList;

import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import se.uu.ub.cora.beefeater.Authorizator;
import se.uu.ub.cora.beefeater.authentication.User;
import se.uu.ub.cora.bookkeeper.data.DataGroup;
import se.uu.ub.cora.bookkeeper.linkcollector.DataRecordLinkCollector;
import se.uu.ub.cora.bookkeeper.validator.DataValidator;
import se.uu.ub.cora.spider.authentication.Authenticator;
import se.uu.ub.cora.spider.authentication.AuthenticatorSpy;
import se.uu.ub.cora.spider.dependency.SpiderDependencyProviderSpy;
import se.uu.ub.cora.spider.extended.ExtendedFunctionalityProviderSpy;
import se.uu.ub.cora.spider.record.storage.RecordStorage;
import se.uu.ub.cora.spider.spy.DataRecordLinkCollectorSpy;
import se.uu.ub.cora.spider.spy.DataValidatorAlwaysValidSpy;
import se.uu.ub.cora.spider.spy.NoRulesCalculatorStub;
import se.uu.ub.cora.spider.spy.RecordStorageSpy;
import se.uu.ub.cora.spider.testdata.DataCreator;

public class SpiderAuthorizatorTest {
	private RecordStorage recordStorage;
	private Authenticator authenticator;
	private PermissionRuleCalculator rulesCalculator;
	private DataValidator dataValidator;
	private DataRecordLinkCollector linkCollector;
	private SpiderDependencyProviderSpy dependencyProvider;
	private ExtendedFunctionalityProviderSpy extendedFunctionalityProvider;
	private User user;
	private RulesProviderSpy rulesProvider;
	private Authorizator authorizator;
	private SpiderAuthorizator spiderAuthorizator;

	@BeforeMethod
	public void beforeMethod() {
		user = new User("someUserId");
		user.roles.add("guest");

		authenticator = new AuthenticatorSpy();
		dataValidator = new DataValidatorAlwaysValidSpy();
		recordStorage = new RecordStorageSpy();
		rulesCalculator = new NoRulesCalculatorStub();
		linkCollector = new DataRecordLinkCollectorSpy();
		extendedFunctionalityProvider = new ExtendedFunctionalityProviderSpy();
		rulesProvider = new RulesProviderSpy();
		setUpDependencyProvider();
	}

	private void setUpDependencyProvider() {
		dependencyProvider = new SpiderDependencyProviderSpy();
		dependencyProvider.authenticator = authenticator;
		dependencyProvider.dataValidator = dataValidator;
		dependencyProvider.recordStorage = recordStorage;
		dependencyProvider.keyCalculator = rulesCalculator;
		dependencyProvider.linkCollector = linkCollector;
		dependencyProvider.extendedFunctionalityProvider = extendedFunctionalityProvider;

		spiderAuthorizator = SpiderAuthorizatorImp
				.usingSpiderDependencyProviderAndAuthorizatorAndRulesProvider(dependencyProvider,
						authorizator, rulesProvider);
	}

	@Test
	public void testRulesProviderCalled() {
		authorizator = new BeefeaterAuthorizatorAlwaysAuthorizedSpy();
		setUpDependencyProvider();
		spiderAuthorizator.userSatisfiesRequiredRules(user, new ArrayList<>());
		assertEquals(rulesProvider.roleId, "guest");
	}

	@Test
	public void testAuthorized() {
		authorizator = new BeefeaterAuthorizatorAlwaysAuthorizedSpy();
		setUpDependencyProvider();
		boolean userAuthorized = spiderAuthorizator.userSatisfiesRequiredRules(user,
				new ArrayList<>());
		assertTrue(userAuthorized);
	}

	@Test
	public void testNotAuthorized() {
		authorizator = new BeefeaterNeverAlwaysAuthorizedSpy();
		setUpDependencyProvider();
		boolean userAuthorized = spiderAuthorizator.userSatisfiesRequiredRules(user,
				new ArrayList<>());
		assertFalse(userAuthorized);
	}

	@Test
	public void userSatisfiesActionForRecordType() {
		authorizator = new BeefeaterAuthorizatorAlwaysAuthorizedSpy();
		setUpDependencyProvider();
		String action = "read";
		String recordType = "book";
		boolean userAuthorized = spiderAuthorizator.userIsAuthorizedForActionOnRecordType(user,
				action, recordType);
		assertTrue(userAuthorized);
		assertRuleCalculatorIsCalled();
	}

	@Test
	public void userDoesNotSatisfyActionForRecordType() {
		authorizator = new BeefeaterNeverAlwaysAuthorizedSpy();
		setUpDependencyProvider();
		String action = "read";
		String recordType = "book";
		boolean userAuthorized = spiderAuthorizator.userIsAuthorizedForActionOnRecordType(user,
				action, recordType);
		assertFalse(userAuthorized);
		assertRuleCalculatorIsCalled();
	}

	@Test
	public void checkUserSatisfiesActionForRecordType() {
		authorizator = new BeefeaterAuthorizatorAlwaysAuthorizedSpy();
		setUpDependencyProvider();
		String action = "read";
		String recordType = "book";
		spiderAuthorizator.checkUserIsAuthorizedForActionOnRecordType(user, action, recordType);

		assertRuleCalculatorIsCalled();
	}

	@Test(expectedExceptions = AuthorizationException.class)
	public void checkUserDoesNotSatisfyActionForRecordType() {
		authorizator = new BeefeaterNeverAlwaysAuthorizedSpy();
		setUpDependencyProvider();
		String action = "read";
		String recordType = "book";
		spiderAuthorizator.checkUserIsAuthorizedForActionOnRecordType(user, action, recordType);
		assertRuleCalculatorIsCalled();
	}

	@Test
	public void userSatisfiesActionForRecord() {
		authorizator = new BeefeaterAuthorizatorAlwaysAuthorizedSpy();
		setUpDependencyProvider();
		String action = "read";
		DataGroup record = DataCreator
				.createRecordWithNameInDataAndIdAndTypeAndLinkedRecordIdAndCreatedBy("book",
						"myBook", "book", "systemOne", "12345")
				.toDataGroup();
		boolean userAuthorized = spiderAuthorizator
				.userIsAuthorizedForActionOnRecordTypeAndRecord(user, action, "book", record);
		assertTrue(userAuthorized);
		assertRuleCalculatorIsCalled();
	}

	private void assertRuleCalculatorIsCalled() {
		assertEquals(((NoRulesCalculatorStub) rulesCalculator).action, "read");
		assertEquals(((NoRulesCalculatorStub) rulesCalculator).recordType, "book");
	}

	@Test
	public void userDoesNotSatisfyActionForRecord() {
		authorizator = new BeefeaterNeverAlwaysAuthorizedSpy();
		setUpDependencyProvider();
		String action = "read";
		DataGroup record = DataCreator
				.createRecordWithNameInDataAndIdAndTypeAndLinkedRecordIdAndCreatedBy("book",
						"myBook", "book", "systemOne", "12345")
				.toDataGroup();
		boolean userAuthorized = spiderAuthorizator
				.userIsAuthorizedForActionOnRecordTypeAndRecord(user, action, "book", record);
		assertFalse(userAuthorized);
		assertRuleCalculatorIsCalled();
	}

	@Test
	public void checkUserSatisfiesActionForRecord() {
		authorizator = new BeefeaterAuthorizatorAlwaysAuthorizedSpy();
		setUpDependencyProvider();
		String action = "read";
		DataGroup record = DataCreator
				.createRecordWithNameInDataAndIdAndTypeAndLinkedRecordIdAndCreatedBy("book",
						"myBook", "book", "systemOne", "12345")
				.toDataGroup();
		spiderAuthorizator.checkUserIsAuthorizedForActionOnRecordTypeAndRecord(user, action, "book",
				record);

		assertRuleCalculatorIsCalled();
	}

	@Test(expectedExceptions = AuthorizationException.class)
	public void checkUserDoesNotSatisfiesActionForRecord() {
		authorizator = new BeefeaterNeverAlwaysAuthorizedSpy();
		setUpDependencyProvider();
		String action = "read";
		DataGroup record = DataCreator
				.createRecordWithNameInDataAndIdAndTypeAndLinkedRecordIdAndCreatedBy("book",
						"myBook", "book", "systemOne", "12345")
				.toDataGroup();
		spiderAuthorizator.checkUserIsAuthorizedForActionOnRecordTypeAndRecord(user, action, "book",
				record);
		assertRuleCalculatorIsCalled();
	}

}
