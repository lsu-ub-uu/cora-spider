/*
 * Copyright 2016, 2017, 2018, 2019 Uppsala University Library
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
import static org.testng.Assert.assertNotNull;
import static org.testng.Assert.assertSame;
import static org.testng.Assert.assertTrue;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import se.uu.ub.cora.beefeater.Authorizator;
import se.uu.ub.cora.beefeater.authentication.User;
import se.uu.ub.cora.beefeater.authorization.Rule;
import se.uu.ub.cora.bookkeeper.linkcollector.DataRecordLinkCollector;
import se.uu.ub.cora.bookkeeper.validator.DataValidator;
import se.uu.ub.cora.data.DataGroup;
import se.uu.ub.cora.logger.LoggerProvider;
import se.uu.ub.cora.spider.authentication.Authenticator;
import se.uu.ub.cora.spider.authentication.AuthenticatorSpy;
import se.uu.ub.cora.spider.data.DataGroupSpy;
import se.uu.ub.cora.spider.dependency.RecordStorageProviderSpy;
import se.uu.ub.cora.spider.dependency.SpiderDependencyProviderSpy;
import se.uu.ub.cora.spider.extended.ExtendedFunctionalityProviderSpy;
import se.uu.ub.cora.spider.log.LoggerFactorySpy;
import se.uu.ub.cora.spider.spy.DataRecordLinkCollectorSpy;
import se.uu.ub.cora.spider.spy.DataValidatorAlwaysValidSpy;
import se.uu.ub.cora.spider.spy.NoRulesCalculatorStub;
import se.uu.ub.cora.spider.spy.RecordStorageForAuthorizatorSpy;
import se.uu.ub.cora.storage.RecordNotFoundException;
import se.uu.ub.cora.storage.RecordStorage;

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
	private SpiderAuthorizatorImp spiderAuthorizator;

	private String action = "read";
	private String recordType = "book";
	private LoggerFactorySpy loggerFactorySpy;

	@BeforeMethod
	public void beforeMethod() {
		loggerFactorySpy = new LoggerFactorySpy();
		LoggerProvider.setLoggerFactory(loggerFactorySpy);
		user = new User("someUserId");
		user.roles.add("guest");

		authenticator = new AuthenticatorSpy();
		dataValidator = new DataValidatorAlwaysValidSpy();
		recordStorage = new RecordStorageForAuthorizatorSpy();
		rulesCalculator = new NoRulesCalculatorStub();
		linkCollector = new DataRecordLinkCollectorSpy();
		extendedFunctionalityProvider = new ExtendedFunctionalityProviderSpy();
		rulesProvider = new RulesProviderSpy();
		setUpDependencyProvider();
	}

	private void setUpDependencyProvider() {
		dependencyProvider = new SpiderDependencyProviderSpy(new HashMap<>());
		dependencyProvider.authenticator = authenticator;
		dependencyProvider.dataValidator = dataValidator;

		RecordStorageProviderSpy recordStorageProviderSpy = new RecordStorageProviderSpy();
		recordStorageProviderSpy.recordStorage = recordStorage;
		dependencyProvider.setRecordStorageProvider(recordStorageProviderSpy);

		dependencyProvider.ruleCalculator = rulesCalculator;
		dependencyProvider.linkCollector = linkCollector;
		dependencyProvider.extendedFunctionalityProvider = extendedFunctionalityProvider;

		spiderAuthorizator = SpiderAuthorizatorImp
				.usingSpiderDependencyProviderAndAuthorizatorAndRulesProvider(dependencyProvider,
						authorizator, rulesProvider);
	}

	@Test
	public void testGetDependencyProvider() {
		setUpDependencyProvider();
		assertSame(spiderAuthorizator.getDependencyProvider(), dependencyProvider);
	}

	@Test
	public void testGetAuthorizator() {
		setUpDependencyProvider();
		assertSame(spiderAuthorizator.getAuthorizator(), authorizator);
	}

	@Test
	public void testGetRulesProvider() {
		setUpDependencyProvider();
		assertSame(spiderAuthorizator.getRulesProvider(), rulesProvider);
	}

	@Test
	public void testRulesProviderCalled() {
		authorizator = new BeefeaterAuthorizatorAlwaysAuthorizedSpy();
		setUpDependencyProvider();
		spiderAuthorizator.userIsAuthorizedForActionOnRecordType(user, action, recordType);
		assertEquals(rulesProvider.roleId, "guest");
	}

	@Test
	public void testAuthorizatorCalled() {
		BeefeaterAuthorizatorAlwaysAuthorizedSpy authorizatorSpy = new BeefeaterAuthorizatorAlwaysAuthorizedSpy();
		authorizator = authorizatorSpy;
		setUpDependencyProvider();

		spiderAuthorizator.userIsAuthorizedForActionOnRecordType(user, action, recordType);

		assertEquals(authorizatorSpy.providedRules.size(),
				rulesProvider.getActiveRules("anyId").size());
	}

	@Test
	public void testAuthorized() {
		authorizator = new BeefeaterAuthorizatorAlwaysAuthorizedSpy();
		setUpDependencyProvider();
		boolean userAuthorized = spiderAuthorizator.userIsAuthorizedForActionOnRecordType(user,
				action, recordType);
		assertTrue(userAuthorized);
	}

	@Test
	public void testNotAuthorized() {
		authorizator = new BeefeaterNeverAuthorizedSpy();
		setUpDependencyProvider();
		boolean userAuthorized = spiderAuthorizator.userIsAuthorizedForActionOnRecordType(user,
				action, recordType);
		assertFalse(userAuthorized);
	}

	@Test
	public void testUserIsActiveCalledOncePerUser() {
		authorizator = new BeefeaterAuthorizatorAlwaysAuthorizedSpy();
		setUpDependencyProvider();
		spiderAuthorizator.userIsAuthorizedForActionOnRecordType(user, action, recordType);
		spiderAuthorizator.userIsAuthorizedForActionOnRecordType(user, action, recordType);
		assertEquals(((RecordStorageForAuthorizatorSpy) recordStorage).userReadNumberOfTimesMap
				.get(user.id).intValue(), 2);
		assertEquals(rulesProvider.returnedRules.size(), 1);
	}

	@Test
	public void userSatisfiesActionForRecordType() {
		authorizator = new BeefeaterAuthorizatorAlwaysAuthorizedSpy();
		setUpDependencyProvider();
		boolean userAuthorized = spiderAuthorizator.userIsAuthorizedForActionOnRecordType(user,
				action, recordType);
		assertTrue(userAuthorized);
		assertRuleCalculatorIsCalled();
	}

	private void assertRuleCalculatorIsCalled() {
		assertEquals(((NoRulesCalculatorStub) rulesCalculator).action, "read");
		assertEquals(((NoRulesCalculatorStub) rulesCalculator).recordType, "book");
	}

	@Test
	public void userDoesNotSatisfyActionForRecordType() {
		authorizator = new BeefeaterNeverAuthorizedSpy();
		setUpDependencyProvider();
		boolean userAuthorized = spiderAuthorizator.userIsAuthorizedForActionOnRecordType(user,
				action, recordType);
		assertFalse(userAuthorized);
		assertRuleCalculatorIsCalled();
	}

	@Test(expectedExceptions = AuthorizationException.class, expectedExceptionsMessageRegExp = ""
			+ "user with id inactiveUserId is inactive")
	public void userInactiveAndDoesNotSatisfyActionForRecordType() {
		authorizator = new BeefeaterAuthorizatorAlwaysAuthorizedSpy();
		setUpDependencyProvider();

		User inactiveUser = new User("inactiveUserId");
		spiderAuthorizator.userIsAuthorizedForActionOnRecordType(inactiveUser, action, recordType);
	}

	@Test
	public void checkUserSatisfiesActionForRecordType() {
		authorizator = new BeefeaterAuthorizatorAlwaysAuthorizedSpy();
		setUpDependencyProvider();
		spiderAuthorizator.checkUserIsAuthorizedForActionOnRecordType(user, action, recordType);

		assertRuleCalculatorIsCalled();
	}

	@Test(expectedExceptions = AuthorizationException.class, expectedExceptionsMessageRegExp = ""
			+ "user with id someUserId is not authorized to create a record  of type:book")
	public void checkUserDoesNotSatisfyActionForRecordType() {
		authorizator = new BeefeaterNeverAuthorizedSpy();
		setUpDependencyProvider();
		spiderAuthorizator.checkUserIsAuthorizedForActionOnRecordType(user, action, recordType);
		assertRuleCalculatorIsCalled();
	}

	@Test(expectedExceptions = AuthorizationException.class, expectedExceptionsMessageRegExp = ""
			+ "user with id nonExistingUserId does not exist")
	public void checkUserDoesNotSatisfiesActionForRecordUserDoesNotExist() {
		authorizator = new BeefeaterAuthorizatorAlwaysAuthorizedSpy();
		setUpDependencyProvider();
		User nonExistingUser = new User("nonExistingUserId");

		DataGroup collectedData = new DataGroupSpy("collectedData");
		spiderAuthorizator.checkUserIsAuthorizedForActionOnRecordTypeAndCollectedData(
				nonExistingUser, action, "book", collectedData);
	}

	@Test
	public void checkUserDoesNotSatisfiesActionForRecordUserDoesNotExistInitialExceptionIsSentAlong() {
		authorizator = new BeefeaterAuthorizatorAlwaysAuthorizedSpy();
		setUpDependencyProvider();
		User nonExistingUser = new User("nonExistingUserId");

		DataGroup collectedData = new DataGroupSpy("collectedData");
		try {
			spiderAuthorizator.checkUserIsAuthorizedForActionOnRecordTypeAndCollectedData(
					nonExistingUser, action, "book", collectedData);
		} catch (Exception e) {
			assertTrue(e.getCause() instanceof RecordNotFoundException);
		}
	}

	@Test(expectedExceptions = AuthorizationException.class, expectedExceptionsMessageRegExp = ""
			+ "user with id inactiveUserId is inactive")
	public void userInactiveForActionOnRecordTypeAndCollectedData() {
		authorizator = new BeefeaterAuthorizatorAlwaysAuthorizedSpy();
		setUpDependencyProvider();

		User inactiveUser = new User("inactiveUserId");
		DataGroup collectedData = new DataGroupSpy("collectedData");
		spiderAuthorizator.checkUserIsAuthorizedForActionOnRecordTypeAndCollectedData(inactiveUser,
				action, "book", collectedData);
	}

	@Test
	public void checkUserSatisfiesActionForCollectedData() {
		user.roles.add("guest2");
		authorizator = new BeefeaterAuthorizatorAlwaysAuthorizedSpy();
		setUpDependencyProvider();
		DataGroup collectedData = new DataGroupSpy("collectedData");
		spiderAuthorizator.checkUserIsAuthorizedForActionOnRecordTypeAndCollectedData(user, action,
				"book", collectedData);

		assertTrue(((NoRulesCalculatorStub) rulesCalculator).calledMethods
				.contains("calculateRulesForActionAndRecordTypeAndCollectedData"));
		assertEquals(((BeefeaterAuthorizatorAlwaysAuthorizedSpy) authorizator).requiredRules,
				((NoRulesCalculatorStub) rulesCalculator).returnedRules);

		List<Rule> providedRules = ((BeefeaterAuthorizatorAlwaysAuthorizedSpy) authorizator).providedRules;

		Iterator<String> iterator = user.roles.iterator();
		assertEquals(rulesProvider.roleIds.get(0), iterator.next());
		assertEquals(providedRules.get(0), rulesProvider.returnedRules.get(0).get(0));
		assertEquals(providedRules.get(1), rulesProvider.returnedRules.get(0).get(1));

		assertEquals(rulesProvider.roleIds.get(1), iterator.next());
		assertEquals(providedRules.get(2), rulesProvider.returnedRules.get(1).get(0));
		assertEquals(providedRules.get(3), rulesProvider.returnedRules.get(1).get(1));
	}

	@Test
	public void checkUserSatisfiesActionForCollectedDataWithPermissionTermForUser() {
		user = new User("userWithPermissionTerm");
		user.roles.add("guest");
		authorizator = new BeefeaterAuthorizatorAlwaysAuthorizedSpy();
		setUpDependencyProvider();
		DataGroup collectedData = new DataGroupSpy("collectedData");
		spiderAuthorizator.checkUserIsAuthorizedForActionOnRecordTypeAndCollectedData(user, action,
				"book", collectedData);

		assertTrue(((NoRulesCalculatorStub) rulesCalculator).calledMethods
				.contains("calculateRulesForActionAndRecordTypeAndCollectedData"));
		assertEquals(((BeefeaterAuthorizatorAlwaysAuthorizedSpy) authorizator).requiredRules,
				((NoRulesCalculatorStub) rulesCalculator).returnedRules);

		List<Rule> providedRules = ((BeefeaterAuthorizatorAlwaysAuthorizedSpy) authorizator).providedRules;

		Iterator<String> iterator = user.roles.iterator();
		assertEquals(rulesProvider.roleIds.get(0), iterator.next());
		List<Rule> rulesFromFirstRole = rulesProvider.returnedRules.get(0);
		Rule firstRule = rulesFromFirstRole.get(0);
		assertEquals(firstRule.getNumberOfRuleParts(), 2);
		assertNotNull(firstRule.getRulePartValuesForKey("action"));
		assertNotNull(firstRule.getRulePartValuesForKey("OWNING_ORGANISATION"));
		assertEquals(providedRules.get(0), firstRule);

		Rule secondRule = rulesFromFirstRole.get(1);
		assertEquals(providedRules.get(1), secondRule);
		assertEquals(secondRule.getNumberOfRuleParts(), 2);
		assertNotNull(secondRule.getRulePartValuesForKey("action"));
		assertNotNull(secondRule.getRulePartValuesForKey("OWNING_ORGANISATION"));

	}

	@Test
	public void checkUserSatisfiesActionForCollectedDataWithTwoRolesAndPermissionTermsForUser() {
		user = new User("userWithTwoRolesPermissionTerm");
		user.roles.add("admin");
		user.roles.add("guest");
		authorizator = new BeefeaterAuthorizatorAlwaysAuthorizedSpy();
		setUpDependencyProvider();
		DataGroup collectedData = new DataGroupSpy("collectedData");
		spiderAuthorizator.checkUserIsAuthorizedForActionOnRecordTypeAndCollectedData(user, action,
				"book", collectedData);

		assertTrue(((NoRulesCalculatorStub) rulesCalculator).calledMethods
				.contains("calculateRulesForActionAndRecordTypeAndCollectedData"));
		assertEquals(((BeefeaterAuthorizatorAlwaysAuthorizedSpy) authorizator).requiredRules,
				((NoRulesCalculatorStub) rulesCalculator).returnedRules);

		List<Rule> providedRules = ((BeefeaterAuthorizatorAlwaysAuthorizedSpy) authorizator).providedRules;

		List<Rule> rulesFromFirstRole = rulesProvider.returnedRules.get(0);
		Rule firstRule = rulesFromFirstRole.get(0);
		assertEquals(providedRules.get(0), firstRule);
		assertEquals(firstRule.getNumberOfRuleParts(), 3);
		assertEquals(firstRule.getRulePartValuesForKey("JOURNAL_ACCESS").size(), 2);
		assertEquals(firstRule.getRulePartValuesForKey("OWNING_ORGANISATION").size(), 1);
		assertRuleContainsAllKeys(firstRule, "JOURNAL_ACCESS", "OWNING_ORGANISATION", "action");

		Rule secondRule = rulesFromFirstRole.get(1);
		assertEquals(providedRules.get(1), secondRule);
		assertEquals(secondRule.getNumberOfRuleParts(), 3);
		assertEquals(secondRule.getRulePartValuesForKey("JOURNAL_ACCESS").size(), 2);
		assertEquals(secondRule.getRulePartValuesForKey("OWNING_ORGANISATION").size(), 1);
		assertRuleContainsAllKeys(secondRule, "JOURNAL_ACCESS", "OWNING_ORGANISATION", "action");

		List<Rule> rulesFromSecondRole = rulesProvider.returnedRules.get(1);
		Rule thirdRule = rulesFromSecondRole.get(0);
		assertEquals(providedRules.get(2), thirdRule);
		assertEquals(thirdRule.getNumberOfRuleParts(), 2);
		assertEquals(thirdRule.getRulePartValuesForKey("OWNING_ORGANISATION").size(), 1);
		assertRuleContainsAllKeys(thirdRule, "OWNING_ORGANISATION", "action");

		Rule fourthRule = rulesFromSecondRole.get(1);
		assertEquals(providedRules.get(3), fourthRule);
		assertEquals(fourthRule.getNumberOfRuleParts(), 2);
		assertEquals(fourthRule.getRulePartValuesForKey("OWNING_ORGANISATION").size(), 1);
		assertRuleContainsAllKeys(fourthRule, "OWNING_ORGANISATION", "action");
	}

	private void assertRuleContainsAllKeys(Rule rule, String... expectedKeys) {
		List<String> expectedKeysList = Arrays.asList(expectedKeys);
		Set<String> ruleKeySet = rule.keySet();
		assertTrue(ruleKeySet.containsAll(expectedKeysList));
	}

	@Test(expectedExceptions = AuthorizationException.class, expectedExceptionsMessageRegExp = ""
			+ "user with id someUserId is not authorized to read a record of type: book")
	public void checkUserDoesNotSatisfiesActionForCollectedData() {
		authorizator = new BeefeaterNeverAuthorizedSpy();
		setUpDependencyProvider();

		user.roles.add("guest2");
		DataGroup collectedData = new DataGroupSpy("collectedData");
		spiderAuthorizator.checkUserIsAuthorizedForActionOnRecordTypeAndCollectedData(user, action,
				"book", collectedData);
	}

	@Test
	public void userSatisfiesActionForCollectedData() {
		user.roles.add("guest2");
		authorizator = new BeefeaterAuthorizatorAlwaysAuthorizedSpy();
		setUpDependencyProvider();
		DataGroup collectedData = new DataGroupSpy("collectedData");
		boolean authorized = spiderAuthorizator
				.userIsAuthorizedForActionOnRecordTypeAndCollectedData(user, action, "book",
						collectedData);
		assertTrue(authorized);

		assertTrue(((NoRulesCalculatorStub) rulesCalculator).calledMethods
				.contains("calculateRulesForActionAndRecordTypeAndCollectedData"));
		assertEquals(((BeefeaterAuthorizatorAlwaysAuthorizedSpy) authorizator).requiredRules,
				((NoRulesCalculatorStub) rulesCalculator).returnedRules);

		List<Rule> providedRules = ((BeefeaterAuthorizatorAlwaysAuthorizedSpy) authorizator).providedRules;

		Iterator<String> iterator = user.roles.iterator();
		assertEquals(rulesProvider.roleIds.get(0), iterator.next());
		assertEquals(providedRules.get(0), rulesProvider.returnedRules.get(0).get(0));
		assertEquals(providedRules.get(1), rulesProvider.returnedRules.get(0).get(1));

		assertEquals(rulesProvider.roleIds.get(1), iterator.next());
		assertEquals(providedRules.get(2), rulesProvider.returnedRules.get(1).get(0));
		assertEquals(providedRules.get(3), rulesProvider.returnedRules.get(1).get(1));
	}

	@Test
	public void userSatisfiesActionForCollectedDataWithPermissionTermForUser() {
		user = new User("userWithPermissionTerm");
		user.roles.add("guest");
		authorizator = new BeefeaterAuthorizatorAlwaysAuthorizedSpy();
		setUpDependencyProvider();
		DataGroup collectedData = new DataGroupSpy("collectedData");
		boolean authorized = spiderAuthorizator
				.userIsAuthorizedForActionOnRecordTypeAndCollectedData(user, action, "book",
						collectedData);
		assertTrue(authorized);

		assertTrue(((NoRulesCalculatorStub) rulesCalculator).calledMethods
				.contains("calculateRulesForActionAndRecordTypeAndCollectedData"));
		assertEquals(((BeefeaterAuthorizatorAlwaysAuthorizedSpy) authorizator).requiredRules,
				((NoRulesCalculatorStub) rulesCalculator).returnedRules);

		List<Rule> providedRules = ((BeefeaterAuthorizatorAlwaysAuthorizedSpy) authorizator).providedRules;

		List<Rule> rulesFromFirstRole = rulesProvider.returnedRules.get(0);
		Rule firstRule = rulesFromFirstRole.get(0);
		assertEquals(firstRule.getNumberOfRuleParts(), 2);
		assertNotNull(firstRule.getRulePartValuesForKey("action"));
		assertNotNull(firstRule.getRulePartValuesForKey("OWNING_ORGANISATION"));
		assertEquals(providedRules.get(0), firstRule);

		Rule secondRule = rulesFromFirstRole.get(1);
		assertEquals(providedRules.get(1), secondRule);
		assertEquals(secondRule.getNumberOfRuleParts(), 2);
		assertNotNull(secondRule.getRulePartValuesForKey("action"));
		assertNotNull(secondRule.getRulePartValuesForKey("OWNING_ORGANISATION"));

	}

	@Test
	public void userSatisfiesActionForCollectedDataWithPermissionTermWithTwoRolesAndPermissionTermsForUser() {
		user = new User("userWithTwoRolesPermissionTerm");
		user.roles.add("admin");
		user.roles.add("guest");
		authorizator = new BeefeaterAuthorizatorAlwaysAuthorizedSpy();
		setUpDependencyProvider();
		DataGroup collectedData = new DataGroupSpy("collectedData");
		boolean authorized = spiderAuthorizator
				.userIsAuthorizedForActionOnRecordTypeAndCollectedData(user, action, "book",
						collectedData);
		assertTrue(authorized);

		assertTrue(((NoRulesCalculatorStub) rulesCalculator).calledMethods
				.contains("calculateRulesForActionAndRecordTypeAndCollectedData"));
		assertEquals(((BeefeaterAuthorizatorAlwaysAuthorizedSpy) authorizator).requiredRules,
				((NoRulesCalculatorStub) rulesCalculator).returnedRules);
		List<Rule> providedRules = ((BeefeaterAuthorizatorAlwaysAuthorizedSpy) authorizator).providedRules;

		List<Rule> rulesFromFirstRole = rulesProvider.returnedRules.get(0);
		Rule firstRule = rulesFromFirstRole.get(0);
		assertEquals(providedRules.get(0), firstRule);
		assertEquals(firstRule.getNumberOfRuleParts(), 3);
		assertEquals(firstRule.getRulePartValuesForKey("JOURNAL_ACCESS").size(), 2);
		assertEquals(firstRule.getRulePartValuesForKey("OWNING_ORGANISATION").size(), 1);
		assertRuleContainsAllKeys(firstRule, "JOURNAL_ACCESS", "action");

		Rule secondRule = rulesFromFirstRole.get(1);
		assertEquals(providedRules.get(1), secondRule);
		assertEquals(secondRule.getNumberOfRuleParts(), 3);
		assertEquals(secondRule.getRulePartValuesForKey("JOURNAL_ACCESS").size(), 2);
		assertEquals(secondRule.getRulePartValuesForKey("OWNING_ORGANISATION").size(), 1);
		assertRuleContainsAllKeys(secondRule, "JOURNAL_ACCESS", "OWNING_ORGANISATION", "action");

		List<Rule> rulesFromSecondRole = rulesProvider.returnedRules.get(1);
		Rule thirdRule = rulesFromSecondRole.get(0);
		assertEquals(providedRules.get(2), thirdRule);
		assertEquals(thirdRule.getNumberOfRuleParts(), 2);
		assertEquals(thirdRule.getRulePartValuesForKey("OWNING_ORGANISATION").size(), 1);
		assertRuleContainsAllKeys(thirdRule, "OWNING_ORGANISATION", "action");

		Rule fourthRule = rulesFromSecondRole.get(1);
		assertEquals(providedRules.get(3), fourthRule);
		assertEquals(fourthRule.getNumberOfRuleParts(), 2);
		assertEquals(fourthRule.getRulePartValuesForKey("OWNING_ORGANISATION").size(), 1);
		assertRuleContainsAllKeys(fourthRule, "OWNING_ORGANISATION", "action");

	}

	@Test
	public void userDoesNotSatisfiesActionForCollectedData() {
		authorizator = new BeefeaterNeverAuthorizedSpy();
		setUpDependencyProvider();

		user.roles.add("guest2");
		DataGroup collectedData = new DataGroupSpy("collectedData");
		boolean authorized = spiderAuthorizator
				.userIsAuthorizedForActionOnRecordTypeAndCollectedData(user, action, "book",
						collectedData);
		assertFalse(authorized);
	}
}
