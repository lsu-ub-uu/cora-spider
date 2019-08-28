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
import java.util.List;
import java.util.Map;

import se.uu.ub.cora.beefeater.authorization.Rule;
import se.uu.ub.cora.beefeater.authorization.RulePartValues;
import se.uu.ub.cora.data.DataGroup;

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

	private void createRulePart(Rule requiredRule, String key, String... values) {
		RulePartValues rulePartValues = new RulePartValues();
		for (String value : values) {
			rulePartValues.add(SYSTEM + value);
		}
		requiredRule.put(key, rulePartValues);
	}

	@Override
	public List<Rule> calculateRulesForActionAndRecordTypeAndCollectedData(String action,
			String recordType, DataGroup collectedData) {
		if (thereAreCollectedPermissionValuesFromData(collectedData)) {
			return createRulesForActionAndRecordTypeAndCollectedData(action, recordType,
					collectedData);
		}
		return calculateRulesForActionAndRecordType(action, recordType);
	}

	private boolean thereAreCollectedPermissionValuesFromData(DataGroup collectedData) {
		return collectedData.containsChildWithNameInData("permission");
	}

	private List<Rule> createRulesForActionAndRecordTypeAndCollectedData(String action,
			String recordType, DataGroup collectedData) {
		Map<String, List<RulePartValues>> sortedRulePartValues = CollectedDataPermissionRulePartExtractor
				.extractRulePartsSortedByPermissionKeyFromCollectedData(collectedData);
		return createRulesFromActionAndRecordTypeAndSortedRulePartValues(action, recordType,
				sortedRulePartValues);
	}

	private List<Rule> createRulesFromActionAndRecordTypeAndSortedRulePartValues(String action,
			String recordType, Map<String, List<RulePartValues>> sortedRulePartValues) {
		List<String> permissionKeys = createListOfPermissionKeysFromSortedRulePartValues(
				sortedRulePartValues);

		List<RulePart> rulePartList = createRulePartListWithActionAndRecordType(action, recordType);

		return RulesCreator.recursivelyCreateRules(sortedRulePartValues, permissionKeys,
				rulePartList);
	}

	private List<String> createListOfPermissionKeysFromSortedRulePartValues(
			Map<String, List<RulePartValues>> sortedRulePartValues) {
		List<String> permissionKeys = new ArrayList<>();
		permissionKeys.addAll(sortedRulePartValues.keySet());
		return permissionKeys;
	}

	private List<RulePart> createRulePartListWithActionAndRecordType(String action,
			String recordType) {
		List<RulePart> rulePartList = new ArrayList<>();
		addActionToRulePartList(action, rulePartList);

		addRecordTypeToRulePartList(recordType, rulePartList);
		return rulePartList;
	}

	private void addActionToRulePartList(String action, List<RulePart> rulePartList) {
		RulePart actionRulePart = RulePart.withKeyAndValue("action", SYSTEM + action);
		rulePartList.add(actionRulePart);
	}

	private void addRecordTypeToRulePartList(String recordType, List<RulePart> rulePartList) {
		RulePart recordTypeRulePart = RulePart.withKeyAndValue("recordType", SYSTEM + recordType);
		rulePartList.add(recordTypeRulePart);
	}

}
