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
import java.util.List;

import se.uu.ub.cora.beefeater.authorization.Rule;
import se.uu.ub.cora.beefeater.authorization.RulePartValues;
import se.uu.ub.cora.bookkeeper.data.DataGroup;

public class BasePermissionRuleCalculator implements PermissionRuleCalculator {

	private static final String SYSTEM = "system.";

	@Override
	public List<Rule> calculateRulesForActionAndRecordType(String action, String recordType) {
		List<Rule> requiredRules = new ArrayList<>();
		Rule requiredRule = createRequiredRule();
		requiredRules.add(requiredRule);
		createRulePart(requiredRule, "action", action);
		createRulePart(requiredRule, "recordType", recordType);
		return requiredRules;
	}

	@Override
	public List<Rule> calculateRulesForActionAndRecordTypeAndCollectedData(String action,
			String recordType, DataGroup collectedData) {
		List<Rule> requiredRules = calculateRulesForActionAndRecordType(action, recordType);
		possiblyCreateRulesForCollectedPermissionTerms(collectedData, requiredRules);
		return requiredRules;
	}

	private Rule createRequiredRule() {
		return new Rule();
	}

	private void createRulePart(Rule requiredRule, String key, String... values) {
		RulePartValues set = new RulePartValues();
		for (String value : values) {
			set.add(SYSTEM + value);
		}
		requiredRule.put(key, set);
	}

	private void possiblyCreateRulesForCollectedPermissionTerms(DataGroup collectedData,
			List<Rule> requiredRules) {
		if (thereAreCollectedPermissionValuesFromData(collectedData)) {
			DataGroup permission = collectedData.getFirstGroupWithNameInData("permission");
			createRulesFromCollectedPermissionTerms(permission, requiredRules);
		}
	}

	private boolean thereAreCollectedPermissionValuesFromData(DataGroup collectedData) {
		return collectedData.containsChildWithNameInData("permission");
	}

	private void createRulesFromCollectedPermissionTerms(DataGroup permission,
			List<Rule> requiredRules) {
		Rule requiredRule = requiredRules.get(0);

		List<DataGroup> collectedDataTerms = permission
				.getAllGroupsWithNameInData("collectedDataTerm");
		for (DataGroup collectedDataTerm : collectedDataTerms) {
			createRuleForCollectedDataTerm(requiredRule, collectedDataTerm);
		}
	}

	private void createRuleForCollectedDataTerm(Rule requiredRule, DataGroup collectedDataTerm) {
		DataGroup extraData = collectedDataTerm.getFirstGroupWithNameInData("extraData");
		String permissionKey = extraData.getFirstAtomicValueWithNameInData("permissionKey");
		String value = collectedDataTerm.getFirstAtomicValueWithNameInData("collectTermValue");
		createRulePart(requiredRule, permissionKey, value);
	}

}
