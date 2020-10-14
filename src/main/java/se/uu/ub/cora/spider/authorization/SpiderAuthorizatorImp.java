/*
 * Copyright 2016, 2019 Uppsala University Library
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

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import se.uu.ub.cora.beefeater.Authorizator;
import se.uu.ub.cora.beefeater.authentication.User;
import se.uu.ub.cora.beefeater.authorization.Rule;
import se.uu.ub.cora.beefeater.authorization.RulePartValuesImp;
import se.uu.ub.cora.data.DataAtomic;
import se.uu.ub.cora.data.DataGroup;
import se.uu.ub.cora.spider.dependency.SpiderDependencyProvider;
import se.uu.ub.cora.spider.role.RulesProvider;
import se.uu.ub.cora.storage.RecordNotFoundException;
import se.uu.ub.cora.storage.RecordStorage;

public final class SpiderAuthorizatorImp implements SpiderAuthorizator {

	private static final String USER_STRING = "user with id ";
	private Authorizator beefeaterAuthorizator;
	private PermissionRuleCalculator ruleCalculator;
	private RulesProvider rulesProvider;
	private RecordStorage recordStorage;
	private SpiderDependencyProvider dependencyProvider;
	Set<String> cachedActiveUsers = new HashSet<>();
	Map<String, List<Rule>> cachedProvidedRulesForUser = new HashMap<>();
	private List<Rule> matchedRules;

	private SpiderAuthorizatorImp(SpiderDependencyProvider dependencyProvider,
			Authorizator authorizator, RulesProvider rulesProvider) {
		this.dependencyProvider = dependencyProvider;
		this.beefeaterAuthorizator = authorizator;
		this.rulesProvider = rulesProvider;
		ruleCalculator = dependencyProvider.getPermissionRuleCalculator();
		recordStorage = dependencyProvider.getRecordStorage();
	}

	public static SpiderAuthorizatorImp usingSpiderDependencyProviderAndAuthorizatorAndRulesProvider(
			SpiderDependencyProvider dependencyProvider, Authorizator authorizator,
			RulesProvider rulesProvider) {
		return new SpiderAuthorizatorImp(dependencyProvider, authorizator, rulesProvider);
	}

	private boolean userSatisfiesRequiredRules(User user, List<Rule> requiredRules) {
		List<Rule> providedRules = getActiveRulesForUser(user);

		return beefeaterAuthorizator.providedRulesSatisfiesRequiredRules(providedRules,
				requiredRules);
	}

	private List<Rule> getActiveRulesForUser(User user) {
		List<Rule> cachedProvidedRules = cachedProvidedRulesForUser.get(user.id);
		if (cachedProvidedRules != null) {
			return cachedProvidedRules;
		}
		return readProvidedRulesForUser(user);
	}

	private List<Rule> readProvidedRulesForUser(User user) {
		List<Rule> providedRules = new ArrayList<>();
		DataGroup userAsDataGroup = getUserAsDataGroup(user);
		user.roles.forEach(roleId -> addRulesForRole(providedRules, roleId, userAsDataGroup));
		cachedProvidedRulesForUser.put(user.id, providedRules);
		return providedRules;
	}

	private DataGroup getUserAsDataGroup(User user) {
		try {
			return recordStorage.read("user", user.id);
		} catch (RecordNotFoundException e) {
			throw new AuthorizationException(USER_STRING + user.id + " does not exist", e);
		}
	}

	private void addRulesForRole(List<Rule> providedRules, String roleId,
			DataGroup userAsDataGroup) {
		List<Rule> activeRulesFromRole = rulesProvider.getActiveRules(roleId);
		List<DataGroup> userRolesFromUserDataGroup = userAsDataGroup
				.getAllGroupsWithNameInData("userRole");
		possiblyAddPermisionTermValuesToAllRules(roleId, activeRulesFromRole,
				userRolesFromUserDataGroup);
		providedRules.addAll(activeRulesFromRole);
	}

	private void possiblyAddPermisionTermValuesToAllRules(String roleId,
			List<Rule> activeRulesFromRole, List<DataGroup> userRolesFromUserDataGroup) {
		for (Rule rule : activeRulesFromRole) {
			possiblyAddPermissionTermValuesToRule(roleId, userRolesFromUserDataGroup, rule);
		}
	}

	private void possiblyAddPermissionTermValuesToRule(String roleId,
			List<DataGroup> userRolesFromUserDataGroup, Rule rule) {
		for (DataGroup userRole : userRolesFromUserDataGroup) {
			possiblyAddPermissionTermsAsRulePartValues(roleId, rule, userRole);
		}
	}

	private void possiblyAddPermissionTermsAsRulePartValues(String roleId, Rule rule,
			DataGroup userRole) {
		if (userRole.containsChildWithNameInData("permissionTermRulePart")) {
			possiblyAddPermissionTermAsRulePartValue(roleId, rule, userRole);
		}
	}

	private void possiblyAddPermissionTermAsRulePartValue(String roleId, Rule rule,
			DataGroup userRole) {
		if (currentRoleMatchesRoleId(userRole, roleId)) {
			addPermissionTermsAsRulePartValues(rule, userRole);
		}
	}

	private boolean currentRoleMatchesRoleId(DataGroup userRole, String roleId) {
		String idOfCurrentRole = extractIdOfCurrentRole(userRole);
		return idOfCurrentRole.equals(roleId);
	}

	private String extractIdOfCurrentRole(DataGroup userRole) {
		DataGroup innerUserRole = userRole.getFirstGroupWithNameInData("userRole");
		return innerUserRole.getFirstAtomicValueWithNameInData("linkedRecordId");
	}

	private void addPermissionTermsAsRulePartValues(Rule rule, DataGroup userRole) {
		List<DataGroup> permissionTermRuleParts = userRole
				.getAllGroupsWithNameInData("permissionTermRulePart");
		for (DataGroup permissionRulePart : permissionTermRuleParts) {
			createRulePartUsingInfoFromRulePartInUser(rule, permissionRulePart);
		}

	}

	private void createRulePartUsingInfoFromRulePartInUser(Rule rule, DataGroup rulePartInUser) {
		RulePartValuesImp rulePartValues = new RulePartValuesImp();
		addAllValuesFromRulePartToRulePartValues(rulePartInUser, rulePartValues);
		String permissionKey = getPermissionKeyUsingRulePart(rulePartInUser);
		rule.addRulePartIfKeyIsAbsent(permissionKey, rulePartValues);
	}

	private void addAllValuesFromRulePartToRulePartValues(DataGroup rulePart,
			RulePartValuesImp rulePartValues) {
		for (DataAtomic rulePartValue : rulePart.getAllDataAtomicsWithNameInData("value")) {
			rulePartValues.add(rulePartValue.getValue());
		}
	}

	private String getPermissionKeyUsingRulePart(DataGroup rulePart) {
		String permissionTermId = extractPermissionTermId(rulePart);
		return extractPermissionKey(permissionTermId);
	}

	private String extractPermissionTermId(DataGroup rulePart) {
		DataGroup ruleGroup = rulePart.getFirstGroupWithNameInData("rule");
		return ruleGroup.getFirstAtomicValueWithNameInData("linkedRecordId");
	}

	private String extractPermissionKey(String permissionTermId) {
		DataGroup collectPermissionTerm = recordStorage.read("collectPermissionTerm",
				permissionTermId);
		DataGroup extraData = collectPermissionTerm.getFirstGroupWithNameInData("extraData");
		return extraData.getFirstAtomicValueWithNameInData("permissionKey");
	}

	@Override
	public boolean userIsAuthorizedForActionOnRecordType(User user, String action,
			String recordType) {
		checkUserIsActiveInStorage(user);
		List<Rule> requiredRulesForActionAndRecordType = ruleCalculator
				.calculateRulesForActionAndRecordType(action, recordType);

		return userSatisfiesRequiredRules(user, requiredRulesForActionAndRecordType);
	}

	private void checkUserIsActiveInStorage(User user) {
		if (!cachedActiveUsers.contains(user.id)) {
			DataGroup dataGroupUser = getUserAsDataGroup(user);
			if (userIsInactive(dataGroupUser)) {
				throw new AuthorizationException(USER_STRING + user.id + " is inactive");
			} else {
				cachedActiveUsers.add(user.id);
			}
		}

	}

	private boolean userIsInactive(DataGroup foundUser) {
		return "inactive".equals(foundUser.getFirstAtomicValueWithNameInData("activeStatus"));
	}

	@Override
	public void checkUserIsAuthorizedForActionOnRecordType(User user, String action,
			String recordType) {
		if (!userIsAuthorizedForActionOnRecordType(user, action, recordType)) {
			throw new AuthorizationException(USER_STRING + user.id
					+ " is not authorized to create a record  of type:" + recordType);
		}
	}

	@Override
	public void checkUserIsAuthorizedForActionOnRecordTypeAndCollectedData(User user, String action,
			String recordType, DataGroup collectedData) {
		if (!userIsAuthorizedForActionOnRecordTypeAndCollectedData(user, action, recordType,
				collectedData)) {
			throw new AuthorizationException(USER_STRING + user.id + " is not authorized to "
					+ action + " a record of type: " + recordType);
		}
	}

	@Override
	public boolean userIsAuthorizedForActionOnRecordTypeAndCollectedData(User user, String action,
			String recordType, DataGroup collectedData) {
		checkUserIsActiveInStorage(user);
		List<Rule> requiredRules = ruleCalculator
				.calculateRulesForActionAndRecordTypeAndCollectedData(action, recordType,
						collectedData);

		List<Rule> providedRules = getActiveRulesForUser(user);
		return beefeaterAuthorizator.providedRulesSatisfiesRequiredRules(providedRules,
				requiredRules);
	}

	public SpiderDependencyProvider getDependencyProvider() {
		// needed for test
		return dependencyProvider;
	}

	public Authorizator getAuthorizator() {
		// needed for test
		return beefeaterAuthorizator;
	}

	public RulesProvider getRulesProvider() {
		// needed for test
		return rulesProvider;
	}

	@Override
	public Set<String> checkGetUsersMatchedRecordPartPermissionsForActionOnRecordTypeAndCollectedData(
			User user, String action, String recordType, DataGroup collectedData,
			boolean calculateRecordPartPermissions) {
		if (calculateRecordPartPermissions) {
			checkUserIsActiveInStorage(user);
			tryToGetMatchedRules(user, action, recordType, collectedData);
			if ("read".equals(action)) {
				return collectReadRecordPartPermissions(recordType);
			}
			return collectWriteRecordPartPermissions(recordType);
		}
		checkUserIsAuthorizedForActionOnRecordTypeAndCollectedData(user, action, recordType,
				collectedData);
		return Collections.emptySet();
	}

	private void tryToGetMatchedRules(User user, String action, String recordType,
			DataGroup collectedData) {
		List<Rule> requiredRules = ruleCalculator
				.calculateRulesForActionAndRecordTypeAndCollectedData(action, recordType,
						collectedData);
		List<Rule> providedRules = getActiveRulesForUser(user);
		matchRules(requiredRules, providedRules);
		possiblyThrowAuthorizationExceptionWhenEmptyMatchedRules(user, action, recordType);
	}

	private void matchRules(List<Rule> requiredRules, List<Rule> providedRules) {
		matchedRules = beefeaterAuthorizator.providedRulesMatchRequiredRules(providedRules,
				requiredRules);
	}

	private void possiblyThrowAuthorizationExceptionWhenEmptyMatchedRules(User user, String action,
			String recordType) {
		if (matchedRules.isEmpty()) {
			throw new AuthorizationException(USER_STRING + user.id + " is not authorized to "
					+ action + " a record of type: " + recordType);
		}
	}

	private Set<String> collectReadRecordPartPermissions(String recordType) {
		Set<String> usersReadRecordPartPermissions = new HashSet<>();

		for (Rule rule : matchedRules) {
			List<String> writeRecordPartPermissions = rule.getReadRecordPartPermissions();
			addRecordPartsPermissions(recordType, usersReadRecordPartPermissions,
					writeRecordPartPermissions);
		}
		return usersReadRecordPartPermissions;
	}

	private void addRecordPartsPermissions(String recordType,
			Set<String> usersRecordPartPermissions, List<String> recordPartPermissions) {
		if (!recordPartPermissions.isEmpty()) {
			addExistingRecordPartPermissions(recordType, usersRecordPartPermissions,
					recordPartPermissions);
		}
	}

	private void addExistingRecordPartPermissions(String recordType,
			Set<String> usersRecordPartPermissions, List<String> recordPartPermissions) {
		for (String readRecordPart : recordPartPermissions) {
			possiblyAddRecordPartsOnlyForRecordType(recordType, usersRecordPartPermissions,
					readRecordPart);
		}
	}

	private void possiblyAddRecordPartsOnlyForRecordType(String recordType,
			Set<String> usersReadRecordPartPermissions, String readRecordPart) {
		if (readRecordPart.startsWith(recordType)) {
			String permissionWithoutRecordType = readRecordPart.replace(recordType + ".", "");
			usersReadRecordPartPermissions.add(permissionWithoutRecordType);
		}
	}

	private Set<String> collectWriteRecordPartPermissions(String recordType) {
		Set<String> usersWriteRecordPartPermissions = new HashSet<>();

		for (Rule rule : matchedRules) {
			List<String> writeRecordPartPermissions = rule.getWriteRecordPartPermissions();
			addRecordPartsPermissions(recordType, usersWriteRecordPartPermissions,
					writeRecordPartPermissions);
		}
		return usersWriteRecordPartPermissions;
	}

	List<Rule> getMatchedRules() {
		// needed for test
		return matchedRules;
	}

}
