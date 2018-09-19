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

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import se.uu.ub.cora.beefeater.Authorizator;
import se.uu.ub.cora.beefeater.authentication.User;
import se.uu.ub.cora.beefeater.authorization.Rule;
import se.uu.ub.cora.beefeater.authorization.RulePartValues;
import se.uu.ub.cora.bookkeeper.data.DataAtomic;
import se.uu.ub.cora.bookkeeper.data.DataGroup;
import se.uu.ub.cora.spider.dependency.SpiderDependencyProvider;
import se.uu.ub.cora.spider.record.storage.RecordStorage;
import se.uu.ub.cora.spider.role.RulesProvider;

public final class SpiderAuthorizatorImp implements SpiderAuthorizator {

	private static final String FILTER = "filter";
	private static final String USER_STRING = "user with id ";
	private Authorizator authorizator;
	private PermissionRuleCalculator ruleCalculator;
	private RulesProvider rulesProvider;
	private RecordStorage recordStorage;

	private SpiderAuthorizatorImp(SpiderDependencyProvider dependencyProvider,
			Authorizator authorizator, RulesProvider rulesProvider) {
		this.authorizator = authorizator;
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

		return authorizator.providedRulesSatisfiesRequiredRules(providedRules, requiredRules);
	}

	private List<Rule> getActiveRulesForUser(User user) {
		List<Rule> providedRules = getProvidedRulesForUser(user);

		// // THIS IS A SMALL HACK UNTIL WE HAVE RECORDRELATIONS AND CAN READ FROM
		// USER, will be needed for userId, organisation, etc

		providedRules.forEach(rule -> {
			RulePartValues userIdValues = new RulePartValues();
			userIdValues.add("system.*");
			rule.put("createdBy", userIdValues);
		});
		return providedRules;
	}

	private List<Rule> getProvidedRulesForUser(User user) {
		List<Rule> providedRules = new ArrayList<>();
		DataGroup userAsDataGroup = getUserAsDataGroup(user);
		user.roles.forEach(roleId -> addRulesForRole(providedRules, roleId, userAsDataGroup));
		return providedRules;
	}

	private DataGroup getUserAsDataGroup(User user) {
		DataGroup emptyFilter = DataGroup.withNameInData(FILTER);
		Collection<DataGroup> users = recordStorage.readAbstractList("user", emptyFilter);
		return findUserInListOfUsers(user, users);
	}

	private boolean addRulesForRole(List<Rule> providedRules, String roleId,
			DataGroup userAsDataGroup) {
		List<Rule> activeRulesFromRole = rulesProvider.getActiveRules(roleId);
		List<DataGroup> userRolesFromUserDataGroup = userAsDataGroup
				.getAllGroupsWithNameInData("userRole");
		possiblyAddPermisionTermValuesToAllRules(roleId, activeRulesFromRole,
				userRolesFromUserDataGroup);
		return providedRules.addAll(activeRulesFromRole);
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
		List<DataGroup> permissionTermRuleParts = userRole.getAllGroupsWithNameInData("permissionTermRulePart");
		for(DataGroup permissionRulePart : permissionTermRuleParts){
			createRulePartUsingInfoFromRulePartInUser(rule, permissionRulePart);
		}

	}

	private void createRulePartUsingInfoFromRulePartInUser(Rule rule, DataGroup rulePartInUser) {
		RulePartValues rulePartValues = new RulePartValues();
		addAllValuesFromRulePartToRulePartValues(rulePartInUser, rulePartValues);
		String permissionKey = getPermissionKeyUsingRulePart(rulePartInUser);
		rule.put(permissionKey, rulePartValues);
	}

	private void addAllValuesFromRulePartToRulePartValues(DataGroup rulePart,
			RulePartValues rulePartValues) {
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
		checkUserIsActive(user);
		List<Rule> requiredRulesForActionAndRecordType = ruleCalculator
				.calculateRulesForActionAndRecordType(action, recordType);

		return userSatisfiesRequiredRules(user, requiredRulesForActionAndRecordType);
	}

	private void checkUserIsActive(User user) {
		DataGroup emptyFilter = DataGroup.withNameInData(FILTER);
		Collection<DataGroup> users = recordStorage.readAbstractList("user", emptyFilter);
		DataGroup foundUser = findUserInListOfUsers(user, users);

		if (userIsInactive(foundUser)) {
			throw new AuthorizationException(USER_STRING + user.id + " is inactive");
		}
	}

	private boolean userIsInactive(DataGroup foundUser) {
		return "inactive".equals(foundUser.getFirstAtomicValueWithNameInData("activeStatus"));
	}

	private DataGroup findUserInListOfUsers(User user, Collection<DataGroup> users) {
		for (DataGroup readUser : users) {
			String id = getIdFromUser(readUser);
			if (id.equals(user.id)) {
				return readUser;
			}

		}
		throw new AuthorizationException(USER_STRING + user.id + " does not exist");
	}

	private String getIdFromUser(DataGroup readUser) {
		DataGroup recordInfo = readUser.getFirstGroupWithNameInData("recordInfo");
		return recordInfo.getFirstAtomicValueWithNameInData("id");
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
		checkUserIsActive(user);
		List<Rule> requiredRules = ruleCalculator
				.calculateRulesForActionAndRecordTypeAndCollectedData(action, recordType,
						collectedData);

		List<Rule> providedRules = getProvidedRulesForUser(user);
		return authorizator.providedRulesSatisfiesRequiredRules(providedRules, requiredRules);
	}

}
