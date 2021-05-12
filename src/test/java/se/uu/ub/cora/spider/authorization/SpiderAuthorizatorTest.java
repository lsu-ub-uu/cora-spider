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

import se.uu.ub.cora.beefeater.authentication.User;
import se.uu.ub.cora.beefeater.authorization.Rule;
import se.uu.ub.cora.beefeater.authorization.RulePartValues;
import se.uu.ub.cora.data.DataGroup;
import se.uu.ub.cora.logger.LoggerProvider;
import se.uu.ub.cora.spider.authentication.Authenticator;
import se.uu.ub.cora.spider.authentication.AuthenticatorSpy;
import se.uu.ub.cora.spider.data.DataGroupSpy;
import se.uu.ub.cora.spider.dependency.SpiderDependencyProviderSpy;
import se.uu.ub.cora.spider.log.LoggerFactorySpy;
import se.uu.ub.cora.spider.spy.RecordStorageForAuthorizatorSpy;
import se.uu.ub.cora.spider.spy.RuleCalculatorSpy;
import se.uu.ub.cora.storage.RecordNotFoundException;
import se.uu.ub.cora.storage.RecordStorage;

public class SpiderAuthorizatorTest {

	private static final String READ = "read";
	private static final String BOOK = "book";

	private LoggerFactorySpy loggerFactorySpy;
	private User user;
	private BeefeaterAuthorizatorSpy beefeaterAuthorizator;
	private Authenticator authenticator;
	private RecordStorage recordStorage;
	private RuleCalculatorSpy ruleCalculator;
	private RulesProviderSpy rulesProvider;
	private SpiderDependencyProviderSpy dependencyProvider;

	private SpiderAuthorizatorImp spiderAuthorizator;

	private DataGroup collectedData;

	@BeforeMethod
	public void beforeMethod() {
		loggerFactorySpy = new LoggerFactorySpy();
		LoggerProvider.setLoggerFactory(loggerFactorySpy);
		createTestUser();
		beefeaterAuthorizator = new BeefeaterAuthorizatorSpy();
		authenticator = new AuthenticatorSpy();
		recordStorage = new RecordStorageForAuthorizatorSpy();
		ruleCalculator = new RuleCalculatorSpy();
		rulesProvider = new RulesProviderSpy();
		setUpDependencyProvider();
		collectedData = new DataGroupSpy("collectedData");
	}

	private void createTestUser() {
		user = new User("someUserId");
		user.roles.add("guest");
	}

	private void setUpDependencyProvider() {
		dependencyProvider = new SpiderDependencyProviderSpy(new HashMap<>());
		dependencyProvider.authenticator = authenticator;
		dependencyProvider.recordStorage = recordStorage;
		dependencyProvider.ruleCalculator = ruleCalculator;

		spiderAuthorizator = SpiderAuthorizatorImp
				.usingSpiderDependencyProviderAndAuthorizatorAndRulesProvider(dependencyProvider,
						beefeaterAuthorizator, rulesProvider);
	}

	@Test
	public void testGetDependencyProvider() {
		assertSame(spiderAuthorizator.getDependencyProvider(), dependencyProvider);
	}

	@Test
	public void testGetAuthorizator() {
		assertSame(spiderAuthorizator.getAuthorizator(), beefeaterAuthorizator);
	}

	@Test
	public void testGetRulesProvider() {
		assertSame(spiderAuthorizator.getRulesProvider(), rulesProvider);
	}

	@Test
	public void testRulesProviderCalled() {
		setUpDependencyProvider();

		spiderAuthorizator.userIsAuthorizedForActionOnRecordType(user, READ, BOOK);

		assertEquals(rulesProvider.roleId, "guest");
	}

	@Test
	public void testAuthorizatorCalled() {
		setUpDependencyProvider();

		spiderAuthorizator.userIsAuthorizedForActionOnRecordType(user, READ, BOOK);

		assertAuthorizatorCalled();
	}

	private void assertAuthorizatorCalled() {
		List<Rule> providedRules = getProvidedRulesForFirstCallToProvidedRulesSatisfiesRequiredRules();
		assertEquals(providedRules.size(), rulesProvider.getActiveRules("anyId").size());
	}

	@Test
	public void testAuthorized() {
		setUpDependencyProvider();

		boolean userAuthorized = spiderAuthorizator.userIsAuthorizedForActionOnRecordType(user,
				READ, BOOK);

		assertTrue(userAuthorized);
	}

	@Test
	public void testNotAuthorized() {
		setupForprovidedRulesDoesNotSatisfiyRequiredRules();

		boolean userAuthorized = spiderAuthorizator.userIsAuthorizedForActionOnRecordType(user,
				READ, BOOK);

		assertFalse(userAuthorized);
	}

	private void setupForprovidedRulesDoesNotSatisfiyRequiredRules() {
		beefeaterAuthorizator.providedRulesSatisfiesRequiredRules = false;
		setUpDependencyProvider();
	}

	@Test
	public void testUserIsActiveCalledOncePerUser() {
		setUpDependencyProvider();

		spiderAuthorizator.userIsAuthorizedForActionOnRecordType(user, READ, BOOK);
		spiderAuthorizator.userIsAuthorizedForActionOnRecordType(user, READ, BOOK);

		assertUserIsActiveCalledOncePerUser();
	}

	private void assertUserIsActiveCalledOncePerUser() {
		assertEquals(((RecordStorageForAuthorizatorSpy) recordStorage).userReadNumberOfTimesMap
				.get(user.id).intValue(), 2);
		assertEquals(rulesProvider.returnedRules.size(), 1);
	}

	@Test
	public void userSatisfiesActionForRecordType() {
		setUpDependencyProvider();

		boolean userAuthorized = spiderAuthorizator.userIsAuthorizedForActionOnRecordType(user,
				READ, BOOK);

		assertSatisfiesActionForRecordType(userAuthorized);
	}

	private void assertSatisfiesActionForRecordType(boolean userAuthorized) {
		assertTrue(userAuthorized);
		assertRuleCalculatorIsCalled();
	}

	private void assertRuleCalculatorIsCalled() {
		ruleCalculator.MCR.assertParameters("calculateRulesForActionAndRecordType", 0, READ, BOOK);
	}

	@Test
	public void userDoesNotSatisfyActionForRecordType() {
		setupForprovidedRulesDoesNotSatisfiyRequiredRules();

		boolean userAuthorized = spiderAuthorizator.userIsAuthorizedForActionOnRecordType(user,
				READ, BOOK);

		assertDoesNotSatisfyActionForRecordType(userAuthorized);
	}

	private void assertDoesNotSatisfyActionForRecordType(boolean userAuthorized) {
		assertFalse(userAuthorized);
		assertRuleCalculatorIsCalled();
	}

	@Test(expectedExceptions = AuthorizationException.class, expectedExceptionsMessageRegExp = ""
			+ "user with id inactiveUserId is inactive")
	public void userInactiveAndDoesNotSatisfyActionForRecordType() {
		User inactiveUser = setupForInactiveAndNotSatisfyActionForRecordType();

		spiderAuthorizator.userIsAuthorizedForActionOnRecordType(inactiveUser, READ, BOOK);
	}

	private User setupForInactiveAndNotSatisfyActionForRecordType() {
		setUpDependencyProvider();
		User inactiveUser = new User("inactiveUserId");
		return inactiveUser;
	}

	@Test
	public void checkUserSatisfiesActionForRecordType() {
		setUpDependencyProvider();

		spiderAuthorizator.checkUserIsAuthorizedForActionOnRecordType(user, READ, BOOK);

		assertRuleCalculatorIsCalled();
	}

	@Test(expectedExceptions = AuthorizationException.class, expectedExceptionsMessageRegExp = ""
			+ "user with id someUserId is not authorized to create a record  of type:book")
	public void checkUserDoesNotSatisfyActionForRecordType() {
		setupForprovidedRulesDoesNotSatisfiyRequiredRules();

		spiderAuthorizator.checkUserIsAuthorizedForActionOnRecordType(user, READ, BOOK);

		assertRuleCalculatorIsCalled();
	}

	@Test(expectedExceptions = AuthorizationException.class, expectedExceptionsMessageRegExp = ""
			+ "user with id nonExistingUserId does not exist")
	public void checkUserDoesNotSatisfiesActionForRecordUserDoesNotExist() {
		User nonExistingUser = setupForNonExistingUser();
		//

		spiderAuthorizator
				.checkGetUsersMatchedRecordPartPermissionsForActionOnRecordTypeAndCollectedData(
						nonExistingUser, READ, BOOK, collectedData, false);
	}

	private User setupForNonExistingUser() {
		setUpDependencyProvider();
		User nonExistingUser = new User("nonExistingUserId");
		return nonExistingUser;
	}

	@Test
	public void checkUserDoesNotSatisfiesActionForRecordUserDoesNotExistInitialExceptionIsSentAlong() {
		User nonExistingUser = setupForNonExistingUser();

		try {
			spiderAuthorizator
					.checkGetUsersMatchedRecordPartPermissionsForActionOnRecordTypeAndCollectedData(
							nonExistingUser, READ, BOOK, collectedData, false);
		} catch (Exception e) {
			assertTrue(e.getCause() instanceof RecordNotFoundException);
		}
	}

	@Test(expectedExceptions = AuthorizationException.class, expectedExceptionsMessageRegExp = ""
			+ "user with id inactiveUserId is inactive")
	public void userInactiveForActionOnRecordTypeAndCollectedData() {
		User inactiveUser = setupForInactiveAndNotSatisfyActionForRecordType();

		spiderAuthorizator
				.checkGetUsersMatchedRecordPartPermissionsForActionOnRecordTypeAndCollectedData(
						inactiveUser, READ, BOOK, collectedData, false);
	}

	@Test
	public void checkUserSatisfiesActionForCollectedData() {
		setupForUserWithRoleGuest2();
		spiderAuthorizator
				.checkGetUsersMatchedRecordPartPermissionsForActionOnRecordTypeAndCollectedData(
						user, READ, BOOK, collectedData, false);

		assertOtherSupportClassesUsedCorrectly();
	}

	@Test
	public void checkUserSatisfiesActionForCollectedDataWithPermissionTermForUser() {
		setupForUserWithOnePermissionTerm();

		spiderAuthorizator
				.checkGetUsersMatchedRecordPartPermissionsForActionOnRecordTypeAndCollectedData(
						user, READ, BOOK, collectedData, false);

		assertCheckUserSatisfiesActionForCollectedDataWithPermissionTermForUser();
	}

	private void assertCheckUserSatisfiesActionForCollectedDataWithPermissionTermForUser() {
		ruleCalculator.MCR.assertParameters("calculateRulesForActionAndRecordTypeAndCollectedData",
				0, READ, BOOK, collectedData);

		List<Rule> providedRules = getProvidedRulesForFirstCallToProvidedRulesSatisfiesRequiredRules();
		Iterator<String> roleIterator = user.roles.iterator();
		assertEquals(rulesProvider.roleIds.get(0), roleIterator.next());
		assertUserRulesMatchWithProvidedRules(providedRules);

		beefeaterAuthorizator.MCR.assertParameter("providedRulesSatisfiesRequiredRules", 0,
				"requiredRules", ruleCalculator.MCR
						.getReturnValue("calculateRulesForActionAndRecordTypeAndCollectedData", 0));
	}

	private void setupForUserWithOnePermissionTerm() {
		user = new User("userWithPermissionTerm");
		user.roles.add("guest");
		setUpDependencyProvider();
	}

	@Test
	public void testRolePermissionTermDoesNotOverwriteRulePermissionTerm() {
		setupForUserWithOnePermissionTerm();

		spiderAuthorizator
				.checkGetUsersMatchedRecordPartPermissionsForActionOnRecordTypeAndCollectedData(
						user, READ, BOOK, collectedData, false);
		List<Rule> providedRules = (List<Rule>) beefeaterAuthorizator.MCR
				.getValueForMethodNameAndCallNumberAndParameterName(
						"providedRulesSatisfiesRequiredRules", 0, "providedRules");
		Rule firstFakeRuleFromRulesProviderSpy = providedRules.get(0);
		RulePartValues rulePartValuesForKey = firstFakeRuleFromRulesProviderSpy
				.getRulePartValuesForKey("OWNING_ORGANISATION");
		assertEquals(rulePartValuesForKey.size(), 1);
		assertTrue(rulePartValuesForKey.contains("system.uu"));
	}

	@Test
	public void checkUserSatisfiesActionForCollectedDataWithTwoRolesAndPermissionTermsForUser() {
		setupUserWithTwoRolesPermissionTerm();

		spiderAuthorizator
				.checkGetUsersMatchedRecordPartPermissionsForActionOnRecordTypeAndCollectedData(
						user, READ, BOOK, collectedData, false);

		ruleCalculator.MCR.assertParameters("calculateRulesForActionAndRecordTypeAndCollectedData",
				0, READ, BOOK, collectedData);

		List<Rule> providedRules = getProvidedRulesForFirstCallToProvidedRulesSatisfiesRequiredRules();

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

		beefeaterAuthorizator.MCR.assertParameter("providedRulesSatisfiesRequiredRules", 0,
				"requiredRules", ruleCalculator.MCR
						.getReturnValue("calculateRulesForActionAndRecordTypeAndCollectedData", 0));
	}

	private List<Rule> getProvidedRulesForFirstCallToProvidedRulesSatisfiesRequiredRules() {
		return (List<Rule>) beefeaterAuthorizator.MCR
				.getValueForMethodNameAndCallNumberAndParameterName(
						"providedRulesSatisfiesRequiredRules", 0, "providedRules");
	}

	private void assertRuleContainsAllKeys(Rule rule, String... expectedKeys) {
		List<String> expectedKeysList = Arrays.asList(expectedKeys);
		Set<String> ruleKeySet = rule.keySet();
		assertTrue(ruleKeySet.containsAll(expectedKeysList));
	}

	@Test(expectedExceptions = AuthorizationException.class, expectedExceptionsMessageRegExp = ""
			+ "user with id someUserId is not authorized to read a record of type: book")
	public void checkUserDoesNotSatisfiesActionForCollectedData() {
		setupForprovidedRulesDoesNotSatisfiyRequiredRules();

		user.roles.add("guest2");

		spiderAuthorizator
				.checkGetUsersMatchedRecordPartPermissionsForActionOnRecordTypeAndCollectedData(
						user, READ, BOOK, collectedData, false);
	}

	@Test
	public void userSatisfiesActionForCollectedData() {
		setupForUserWithRoleGuest2();

		boolean authorized = spiderAuthorizator
				.userIsAuthorizedForActionOnRecordTypeAndCollectedData(user, READ, BOOK,
						collectedData);
		assertTrue(authorized);

		assertOtherSupportClassesUsedCorrectly();
	}

	private void setupForUserWithRoleGuest2() {
		user.roles.add("guest2");
		setUpDependencyProvider();
	}

	@Test
	public void testCheckUserSatisfiesActionForCollectedData() {
		setupForUserWithRoleGuest2();

		spiderAuthorizator.checkUserIsAuthorizedForActionOnRecordTypeAndCollectedData(user, READ,
				BOOK, collectedData);

		assertOtherSupportClassesUsedCorrectly();
	}

	private void assertOtherSupportClassesUsedCorrectly() {
		ruleCalculator.MCR.assertParameters("calculateRulesForActionAndRecordTypeAndCollectedData",
				0, READ, BOOK, collectedData);

		List<Rule> providedRules = getProvidedRulesForFirstCallToProvidedRulesSatisfiesRequiredRules();

		Iterator<String> iterator = user.roles.iterator();
		assertEquals(rulesProvider.roleIds.get(0), iterator.next());
		assertEquals(providedRules.get(0), rulesProvider.returnedRules.get(0).get(0));
		assertEquals(providedRules.get(1), rulesProvider.returnedRules.get(0).get(1));

		assertEquals(rulesProvider.roleIds.get(1), iterator.next());
		assertEquals(providedRules.get(2), rulesProvider.returnedRules.get(1).get(0));
		assertEquals(providedRules.get(3), rulesProvider.returnedRules.get(1).get(1));

		beefeaterAuthorizator.MCR.assertParameter("providedRulesSatisfiesRequiredRules", 0,
				"requiredRules", ruleCalculator.MCR
						.getReturnValue("calculateRulesForActionAndRecordTypeAndCollectedData", 0));
	}

	@Test
	public void userSatisfiesActionForCollectedDataWithPermissionTermForUser() {
		setupForUserWithOnePermissionTerm();

		boolean authorized = spiderAuthorizator
				.userIsAuthorizedForActionOnRecordTypeAndCollectedData(user, READ, BOOK,
						collectedData);
		assertTrue(authorized);

		assertUserSatisfiesActionForCollectedDataWithPermissionTermForUser();
	}

	private void assertUserSatisfiesActionForCollectedDataWithPermissionTermForUser() {
		ruleCalculator.MCR.assertParameters("calculateRulesForActionAndRecordTypeAndCollectedData",
				0, READ, BOOK, collectedData);

		List<Rule> providedRules = getProvidedRulesForFirstCallToProvidedRulesSatisfiesRequiredRules();

		assertUserRulesMatchWithProvidedRules(providedRules);

		beefeaterAuthorizator.MCR.assertParameter("providedRulesSatisfiesRequiredRules", 0,
				"requiredRules", ruleCalculator.MCR
						.getReturnValue("calculateRulesForActionAndRecordTypeAndCollectedData", 0));
	}

	private void assertUserRulesMatchWithProvidedRules(List<Rule> providedRules) {
		List<Rule> rulesFromFirstRole = rulesProvider.returnedRules.get(0);
		Rule firstRule = rulesFromFirstRole.get(0);
		assertEquals(firstRule.getNumberOfRuleParts(), 2);
		assertNotNull(firstRule.getRulePartValuesForKey("action"));
		assertNotNull(firstRule.getRulePartValuesForKey("OWNING_ORGANISATION"));
		assertSame(providedRules.get(0), firstRule);

		Rule secondRule = rulesFromFirstRole.get(1);
		assertEquals(secondRule.getNumberOfRuleParts(), 2);
		assertNotNull(secondRule.getRulePartValuesForKey("action"));
		assertNotNull(secondRule.getRulePartValuesForKey("OWNING_ORGANISATION"));
		assertSame(providedRules.get(1), secondRule);
	}

	@Test
	public void testCheckUserSatisfiesActionForCollectedDataWithPermissionTermForUser() {
		setupForUserWithOnePermissionTerm();

		spiderAuthorizator.checkUserIsAuthorizedForActionOnRecordTypeAndCollectedData(user, READ,
				BOOK, collectedData);

		assertUserSatisfiesActionForCollectedDataWithPermissionTermForUser();
	}

	@Test
	public void userSatisfiesActionForCollectedDataWithPermissionTermWithTwoRolesAndPermissionTermsForUser() {
		setupUserWithTwoRolesPermissionTerm();

		boolean authorized = spiderAuthorizator
				.userIsAuthorizedForActionOnRecordTypeAndCollectedData(user, READ, BOOK,
						collectedData);
		assertTrue(authorized);
		assertUserSatisfiesActionForCollectedDataWithPermissionTermWithTwoRolesAndPermissionTermsForUser();
	}

	private void assertUserSatisfiesActionForCollectedDataWithPermissionTermWithTwoRolesAndPermissionTermsForUser() {

		ruleCalculator.MCR.assertParameters("calculateRulesForActionAndRecordTypeAndCollectedData",
				0, READ, BOOK, collectedData);

		List<Rule> providedRules = getProvidedRulesForFirstCallToProvidedRulesSatisfiesRequiredRules();

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

		beefeaterAuthorizator.MCR.assertParameter("providedRulesSatisfiesRequiredRules", 0,
				"requiredRules", ruleCalculator.MCR
						.getReturnValue("calculateRulesForActionAndRecordTypeAndCollectedData", 0));
	}

	@Test
	public void testCheckUserSatisfiesActionForCollectedDataWithPermissionTermWithTwoRolesAndPermissionTermsForUser() {
		setupUserWithTwoRolesPermissionTerm();

		spiderAuthorizator.checkUserIsAuthorizedForActionOnRecordTypeAndCollectedData(user, READ,
				BOOK, collectedData);

		assertUserSatisfiesActionForCollectedDataWithPermissionTermWithTwoRolesAndPermissionTermsForUser();
	}

	private void setupUserWithTwoRolesPermissionTerm() {
		user = new User("userWithTwoRolesPermissionTerm");
		user.roles.add("admin");
		user.roles.add("guest");
		setUpDependencyProvider();
	}

	@Test
	public void userDoesNotSatisfiesActionForCollectedData() {
		setupForprovidedRulesDoesNotSatisfiyRequiredRules();

		boolean authorized = spiderAuthorizator
				.userIsAuthorizedForActionOnRecordTypeAndCollectedData(user, READ, BOOK,
						collectedData);
		assertFalse(authorized);
	}

	@Test(expectedExceptions = AuthorizationException.class)
	public void testCheckUserSatisfiesActionForCollectedDataNotAuthorized() {
		setupForprovidedRulesDoesNotSatisfiyRequiredRules();

		spiderAuthorizator.checkUserIsAuthorizedForActionOnRecordTypeAndCollectedData(user, READ,
				BOOK, collectedData);
	}

	@Test(expectedExceptions = AuthorizationException.class, expectedExceptionsMessageRegExp = ""
			+ "user with id nonExistingUserId does not exist")
	public void checkUserDoesNotSatisfiesActionForRecordUserDoesNotExistCollectingRules() {

		User nonExistingUser = setupForNonExistingUser();

		spiderAuthorizator
				.checkGetUsersMatchedRecordPartPermissionsForActionOnRecordTypeAndCollectedData(
						nonExistingUser, READ, BOOK, collectedData, true);
	}

	@Test
	public void checkUserDoesNotSatisfiesActionForRecordUserDoesNotExistInitialExceptionIsSentAlongCollectingRules() {

		User nonExistingUser = setupForNonExistingUser();

		try {
			spiderAuthorizator
					.checkGetUsersMatchedRecordPartPermissionsForActionOnRecordTypeAndCollectedData(
							nonExistingUser, READ, BOOK, collectedData, true);
		} catch (Exception e) {
			assertTrue(e.getCause() instanceof RecordNotFoundException);
		}
	}

	@Test(expectedExceptions = AuthorizationException.class, expectedExceptionsMessageRegExp = ""
			+ "user with id inactiveUserId is inactive")
	public void userInactiveForActionOnRecordTypeAndCollectedDataCollectingRules() {

		User inactiveUser = setupForInactiveAndNotSatisfyActionForRecordType();

		spiderAuthorizator
				.checkGetUsersMatchedRecordPartPermissionsForActionOnRecordTypeAndCollectedData(
						inactiveUser, READ, BOOK, collectedData, true);
	}

	@Test
	public void userSatisfiesActionForCollectedDataCollectingRules() {
		setupForUserWithRoleGuest2();
		Set<String> usersReadRecordPartPermissions = spiderAuthorizator
				.checkGetUsersMatchedRecordPartPermissionsForActionOnRecordTypeAndCollectedData(
						user, READ, BOOK, collectedData, true);

		ruleCalculator.MCR.assertParameters("calculateRulesForActionAndRecordTypeAndCollectedData",
				0, READ, BOOK, collectedData);

		List<Rule> providedRules = getProvidedRulesForFirstCallToProvidedRulesMatchRequiredRules();

		Iterator<String> iterator = user.roles.iterator();
		assertEquals(rulesProvider.roleIds.get(0), iterator.next());
		assertEquals(providedRules.get(0), rulesProvider.returnedRules.get(0).get(0));
		assertEquals(providedRules.get(1), rulesProvider.returnedRules.get(0).get(1));

		assertEquals(rulesProvider.roleIds.get(1), iterator.next());
		assertEquals(providedRules.get(2), rulesProvider.returnedRules.get(1).get(0));
		assertEquals(providedRules.get(3), rulesProvider.returnedRules.get(1).get(1));

		beefeaterAuthorizator.MCR.assertParameter("providedRulesMatchRequiredRules", 0,
				"requiredRules", ruleCalculator.MCR
						.getReturnValue("calculateRulesForActionAndRecordTypeAndCollectedData", 0));

		assertEquals(spiderAuthorizator.getMatchedRules(),
				beefeaterAuthorizator.MCR.getReturnValue("providedRulesMatchRequiredRules", 0));

		assertTrue(usersReadRecordPartPermissions.isEmpty());
	}

	private List<Rule> getProvidedRulesForFirstCallToProvidedRulesMatchRequiredRules() {
		return (List<Rule>) beefeaterAuthorizator.MCR
				.getValueForMethodNameAndCallNumberAndParameterName(
						"providedRulesMatchRequiredRules", 0, "providedRules");
	}

	@Test
	public void userSatisfiesActionForCollectedDataWithPermissionTermForUserCollectingRules() {
		setupForUserWithOnePermissionTerm();
		rulesProvider.returnReadRecordPartPermissions = true;

		Set<String> usersReadRecordPartPermissions = spiderAuthorizator
				.checkGetUsersMatchedRecordPartPermissionsForActionOnRecordTypeAndCollectedData(
						user, READ, BOOK, collectedData, true);

		ruleCalculator.MCR.assertParameters("calculateRulesForActionAndRecordTypeAndCollectedData",
				0, READ, BOOK, collectedData);

		List<Rule> providedRules = getProvidedRulesForFirstCallToProvidedRulesMatchRequiredRules();

		assertUserRulesMatchWithProvidedRules(providedRules);

		assertEquals(usersReadRecordPartPermissions.size(), 2);
		assertTrue(usersReadRecordPartPermissions.contains("price"));
		assertTrue(usersReadRecordPartPermissions.contains("placement"));

		beefeaterAuthorizator.MCR.assertParameter("providedRulesMatchRequiredRules", 0,
				"requiredRules", ruleCalculator.MCR
						.getReturnValue("calculateRulesForActionAndRecordTypeAndCollectedData", 0));
	}

	@Test
	public void userWritePermissionsForCollectedRulesNoReturnedPermissions() {
		setupForUserWithOnePermissionTerm();
		rulesProvider.returnReadRecordPartPermissions = false;
		rulesProvider.returnWriteRecordPartPermissions = false;

		Set<String> usersReadRecordPartPermissions = spiderAuthorizator
				.checkGetUsersMatchedRecordPartPermissionsForActionOnRecordTypeAndCollectedData(
						user, "update", BOOK, collectedData, true);

		assertEquals(usersReadRecordPartPermissions.size(), 0);
	}

	@Test
	public void userWritePermissionsForCollectedRules() {
		setupForUserWithOnePermissionTerm();
		rulesProvider.returnReadRecordPartPermissions = false;
		rulesProvider.returnWriteRecordPartPermissions = true;

		Set<String> usersReadRecordPartPermissions = spiderAuthorizator
				.checkGetUsersMatchedRecordPartPermissionsForActionOnRecordTypeAndCollectedData(
						user, "update", BOOK, collectedData, true);

		assertEquals(usersReadRecordPartPermissions.size(), 2);
		assertTrue(usersReadRecordPartPermissions.contains("priceWrite"));
		assertTrue(usersReadRecordPartPermissions.contains("placementWrite"));
	}

	@Test
	public void userSatisfiesActionForCollectedDataWithPermissionTermWithTwoRolesAndPermissionTermsForUserCollectingRules() {
		setupUserWithTwoRolesPermissionTerm();

		spiderAuthorizator
				.checkGetUsersMatchedRecordPartPermissionsForActionOnRecordTypeAndCollectedData(
						user, READ, BOOK, collectedData, true);

		ruleCalculator.MCR.assertParameters("calculateRulesForActionAndRecordTypeAndCollectedData",
				0, READ, BOOK, collectedData);

		List<Rule> providedRules = getProvidedRulesForFirstCallToProvidedRulesMatchRequiredRules();

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

		beefeaterAuthorizator.MCR.assertParameter("providedRulesMatchRequiredRules", 0,
				"requiredRules", ruleCalculator.MCR
						.getReturnValue("calculateRulesForActionAndRecordTypeAndCollectedData", 0));
	}

	@Test(expectedExceptions = AuthorizationException.class, expectedExceptionsMessageRegExp = ""
			+ "user with id userWithTwoRolesPermissionTerm is not authorized to read a record of type: book")
	public void testEmptyMatchedRulesGivesNoAuthorzation() throws Exception {
		setupUserWithTwoRolesPermissionTerm();
		BeefeaterAuthorizatorSpy authorizatorSpy = beefeaterAuthorizator;
		authorizatorSpy.returnNoMatchedRules = true;

		spiderAuthorizator
				.checkGetUsersMatchedRecordPartPermissionsForActionOnRecordTypeAndCollectedData(
						user, READ, BOOK, collectedData, true);
	}
}
