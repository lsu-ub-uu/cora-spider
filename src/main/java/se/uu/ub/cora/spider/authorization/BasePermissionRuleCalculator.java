/*
 * Copyright 2016, 2018 Uppsala University Library
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
import java.util.Set;

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
		List<Rule> requiredRules = new ArrayList<>();
		// Rule requiredRule = createRequiredRuleWithActionAndRecordType(action,
		// recordType);
		// requiredRules.add(requiredRule);

		// should multiple of same data result in more values, or duplicated rules with
		// one value each (will not scale)
		List<DataGroup> collectedDataTerms = permission
				.getAllGroupsWithNameInData("collectedDataTerm");
		Map<String, List<RulePartValues>> sortedRulePartValues = sortCollectedDataTermsByPermissionKey(
				collectedDataTerms);
		// for (DataGroup collectedDataTerm : collectedDataTerms) {
		// createRuleForCollectedDataTerm(requiredRule, collectedDataTerm);
		// }
		Set<String> permissionKeys = sortedRulePartValues.keySet();
		for (String permissionKey : permissionKeys) {
			List<RulePartValues> list = sortedRulePartValues.get(permissionKey);
			for (RulePartValues rulePartValues : list) {
				// create rule
				Rule requiredRule = createRequiredRuleWithActionAndRecordType(action, recordType);
				requiredRules.add(requiredRule);
				requiredRule.put(permissionKey, rulePartValues);

				// go over all other lists than this permissionKeys list
				Set<String> permissionKeysInner = sortedRulePartValues.keySet();
				for (String permissionKeyInner : permissionKeysInner) {
					if (!permissionKeyInner.equals(permissionKey)) {
						List<RulePartValues> listInner = sortedRulePartValues.get(permissionKey);
						for (RulePartValues rulePartValuesInner : listInner) {
							// add all ruleparts to rule.
							requiredRule.put(permissionKeyInner, rulePartValuesInner);
						}
					}
				}

			}

		}
		return requiredRules;
	}

	private Map<String, List<RulePartValues>> sortCollectedDataTermsByPermissionKey(
			List<DataGroup> collectedDataTerms) {
		// TODO Auto-generated method stub
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

	private void createRuleForCollectedDataTerm(Rule requiredRule, DataGroup collectedDataTerm) {
		DataGroup extraData = collectedDataTerm.getFirstGroupWithNameInData("extraData");
		String permissionKey = extraData.getFirstAtomicValueWithNameInData("permissionKey");
		String value = collectedDataTerm.getFirstAtomicValueWithNameInData("collectTermValue");
		createRulePart(requiredRule, permissionKey, value);
	}

}
