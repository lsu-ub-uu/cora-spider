/*
 * Copyright 2018 Uppsala University Library
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
 */package se.uu.ub.cora.spider.authorization;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import se.uu.ub.cora.beefeater.authorization.Rule;
import se.uu.ub.cora.beefeater.authorization.RulePartValues;

public final class RulesCreator {

	private Map<String, List<RulePartValues>> sortedRulePartValues;
	private List<String> remainingPermissionKeys;
	private List<RulePart> collectedRuleParts;
	private List<Rule> requiredRules = new ArrayList<>();
	private String currentPermissionKey;

	private RulesCreator(Map<String, List<RulePartValues>> sortedRulePartValues,
			List<RulePart> builtRuleParts) {
		this.sortedRulePartValues = sortedRulePartValues;
		this.collectedRuleParts = copyRuleParts(builtRuleParts);
	}

	public static List<Rule> recursivelyCreateRules(
			Map<String, List<RulePartValues>> sortedRulePartValues, List<String> permissionKeys,
			List<RulePart> builtRuleParts) {
		RulesCreator instance = new RulesCreator(sortedRulePartValues, builtRuleParts);
		return instance.recursivelyCreateRulesForPermissionKeysWithListOfRuleParts(permissionKeys);
	}

	private List<Rule> recursivelyCreateRulesForPermissionKeysWithListOfRuleParts(
			List<String> permissionKeys) {
		currentPermissionKey = getFirstKey(permissionKeys);
		remainingPermissionKeys = copyListExcludingFirstKey(permissionKeys);

		return recursivelyCreateRulesPartsForCurrentPermissionKey();
	}

	private String getFirstKey(List<String> permissionKeys) {
		return permissionKeys.get(0);
	}

	private List<String> copyListExcludingFirstKey(List<String> permissionKeys) {
		List<String> permissionKeysCopy = new ArrayList<>();
		permissionKeysCopy.addAll(permissionKeys);
		permissionKeysCopy.remove(0);
		return permissionKeysCopy;
	}

	private Rule createRuleFromListOfRuleParts(List<RulePart> builtRuleParts) {
		Rule requiredRule = new Rule();
		for (RulePart rulePart : builtRuleParts) {
			requiredRule.put(rulePart.key, rulePart.rulePartValues);
		}
		return requiredRule;
	}

	private List<Rule> recursivelyCreateRulesPartsForCurrentPermissionKey() {

		List<RulePartValues> list = sortedRulePartValues.get(currentPermissionKey);
		for (RulePartValues rulePartValues : list) {
			recursivelyCreateRulesForRulePartValues(rulePartValues);
		}
		return copyRule(requiredRules);
	}

	private List<Rule> copyRule(List<Rule> rules) {
		List<Rule> builtRuleParts = new ArrayList<>();
		builtRuleParts.addAll(rules);
		return builtRuleParts;
	}

	private void recursivelyCreateRulesForRulePartValues(RulePartValues rulePartValues) {
		List<RulePart> currentRuleParts = copyRuleParts(collectedRuleParts);
		RulePart rulePart = RulePart.withKeyAndRulePartValues(currentPermissionKey, rulePartValues);
		currentRuleParts.add(rulePart);

		createRuleOrRecursivelyCreateRulesForRemainingPermissionKeys(currentRuleParts);
	}

	private void createRuleOrRecursivelyCreateRulesForRemainingPermissionKeys(
			List<RulePart> currentRuleParts) {
		if (handlingLastListOfRulePartValues()) {
			createRuleForRuleParts(currentRuleParts);
		} else {
			recursivelyCreateRulesForRemainingPermissionKeys(currentRuleParts);
		}
	}

	private void createRuleForRuleParts(List<RulePart> currentRuleParts) {
		Rule requiredRule = createRuleFromListOfRuleParts(currentRuleParts);
		requiredRules.add(requiredRule);
	}

	private void recursivelyCreateRulesForRemainingPermissionKeys(List<RulePart> currentRuleParts) {
		requiredRules.addAll(RulesCreator.recursivelyCreateRules(sortedRulePartValues,
				remainingPermissionKeys, currentRuleParts));
	}

	private boolean handlingLastListOfRulePartValues() {
		return remainingPermissionKeys.isEmpty();
	}

	private List<RulePart> copyRuleParts(List<RulePart> builtRulePartsIn) {
		List<RulePart> builtRuleParts = new ArrayList<>();
		builtRuleParts.addAll(builtRulePartsIn);
		return builtRuleParts;
	}

}
