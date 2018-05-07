/*
 * Copyright 2016, 2018 Uppsala University Library
 * Copyright 2018 Olov McKie
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
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import se.uu.ub.cora.beefeater.authorization.Rule;
import se.uu.ub.cora.beefeater.authorization.RulePartValues;
import se.uu.ub.cora.bookkeeper.data.DataGroup;

public class BasePermissionRuleCalculator implements PermissionRuleCalculator {

	private static final String SYSTEM = "system.";

	@Override
	public List<Rule> calculateRulesForActionAndRecordType(String action, String recordType) {
		List<Rule> requiredRules = new ArrayList<>();
		Rule requiredRule = createRequiredRuleWithActionAndRecordType(action, recordType);
		requiredRules.add(requiredRule);
		return requiredRules;
	}

	private Rule createRequiredRuleWithActionAndRecordType(String action, String recordType) {
		Rule requiredRule = new Rule();
		createRulePart(requiredRule, "action", action);
		createRulePart(requiredRule, "recordType", recordType);
		return requiredRule;
	}

	@Override
	public List<Rule> calculateRulesForActionAndRecordTypeAndCollectedData(String action,
			String recordType, DataGroup collectedData) {
		if (thereAreCollectedPermissionValuesFromData(collectedData)) {
			return createRulesForActonAndRecordTypeAndCollectedData(action, recordType,
					collectedData);
		}
		return calculateRulesForActionAndRecordType(action, recordType);
	}

	private boolean thereAreCollectedPermissionValuesFromData(DataGroup collectedData) {
		return collectedData.containsChildWithNameInData("permission");
	}

	private void createRulePart(Rule requiredRule, String key, String... values) {
		RulePartValues rulePartValues = new RulePartValues();
		for (String value : values) {
			rulePartValues.add(SYSTEM + value);
		}
		requiredRule.put(key, rulePartValues);
	}

	private List<Rule> createRulesForActonAndRecordTypeAndCollectedData(String action,
			String recordType, DataGroup collectedData) {
		DataGroup permission = collectedData.getFirstGroupWithNameInData("permission");
		return createRulesFromActionAndRecordTypeAndCollectedPermissionTerms(action, recordType,
				permission);
	}

	private List<Rule> createRulesFromActionAndRecordTypeAndCollectedPermissionTerms(String action,
			String recordType, DataGroup permission) {
		List<DataGroup> collectedDataTerms = permission
				.getAllGroupsWithNameInData("collectedDataTerm");

		Map<String, List<RulePartValues>> sortedRulePartValues = sortCollectedDataTermsByPermissionKey(
				collectedDataTerms);
		List<String> permissionKeys = new ArrayList<>();
		permissionKeys.addAll(sortedRulePartValues.keySet());

		List<RulePart> builtRulePartsIn = new ArrayList<>();
		RulePart actionRulePart = RulePart.withKeyAndValue("action", SYSTEM + action);
		builtRulePartsIn.add(actionRulePart);

		RulePart recordTypeRulePart = RulePart.withKeyAndValue("recordType", SYSTEM + recordType);
		builtRulePartsIn.add(recordTypeRulePart);

		return createRulesRecurse(sortedRulePartValues, permissionKeys, builtRulePartsIn);

	}

	private List<Rule> createRulesRecurse(Map<String, List<RulePartValues>> sortedRulePartValues,
			List<String> permissionKeysIn, List<RulePart> builtRulePartsIn) {
		List<Rule> requiredRules = new ArrayList<>();

		List<String> permissionKeys = new ArrayList<>();
		permissionKeys.addAll(permissionKeysIn);
		String permissionKey = permissionKeys.remove(0);

		List<RulePartValues> list = sortedRulePartValues.get(permissionKey);
		for (RulePartValues rulePartValues : list) {
			List<RulePart> builtRuleParts = new ArrayList<>();
			builtRuleParts.addAll(builtRulePartsIn);
			RulePart rulePart = RulePart.withKeyAndRulePartValues(permissionKey, rulePartValues);
			builtRuleParts.add(rulePart);

			if (permissionKeys.isEmpty()) {
				// crate rule
				// Rule requiredRule = createRequiredRuleWithActionAndRecordType(action,
				// recordType);
				Rule requiredRule = new Rule();
				requiredRules.add(requiredRule);
				for (RulePart rulePart2 : builtRuleParts) {
					requiredRule.put(rulePart2.key, rulePart2.rulePartValues);
				}
			} else {
				requiredRules.addAll(
						createRulesRecurse(sortedRulePartValues, permissionKeys, builtRuleParts));
			}

		}
		return requiredRules;
	}

	private Map<String, List<RulePartValues>> sortCollectedDataTermsByPermissionKey(
			List<DataGroup> collectedDataTerms) {
		Map<String, List<RulePartValues>> out = new HashMap<>();

		for (DataGroup collectedDataTerm : collectedDataTerms) {
			DataGroup extraData = collectedDataTerm.getFirstGroupWithNameInData("extraData");
			String permissionKey = extraData.getFirstAtomicValueWithNameInData("permissionKey");
			String value = collectedDataTerm.getFirstAtomicValueWithNameInData("collectTermValue");

			RulePartValues rulePartValues = new RulePartValues();
			rulePartValues.add(SYSTEM + value);
			if (!out.containsKey(permissionKey)) {
				out.put(permissionKey, new ArrayList<>());
			}
			out.get(permissionKey).add(rulePartValues);
		}
		return out;
	}

}
