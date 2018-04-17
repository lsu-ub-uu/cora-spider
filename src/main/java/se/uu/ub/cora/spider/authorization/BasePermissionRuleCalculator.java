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
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

import se.uu.ub.cora.bookkeeper.data.DataGroup;

public class BasePermissionRuleCalculator implements PermissionRuleCalculator {

	private static final String SYSTEM = "system.";
	private DataGroup record;

	@Override
	public List<Map<String, Set<String>>> calculateRulesForActionAndRecordType(String action,
			String recordType) {
		List<Map<String, Set<String>>> requiredRules = new ArrayList<>();
		Map<String, Set<String>> requiredRule = createRequiredRule();
		requiredRules.add(requiredRule);
		createRulePart(requiredRule, "action", action);
		createRulePart(requiredRule, "recordType", recordType);
		return requiredRules;
	}

	@Override
	public List<Map<String, Set<String>>> calculateRulesForActionAndRecordTypeAndCollectedData(
			String action, String recordType, DataGroup collectedData) {
		List<Map<String, Set<String>>> requiredRules = calculateRulesForActionAndRecordType(action,
				recordType);
		possiblyCreateRulesForCollectedPermissionTerms(collectedData, requiredRules);
		return requiredRules;
	}

	@Override
	public List<Map<String, Set<String>>> calculateRulesForActionAndRecordTypeAndData(String action,
			String recordType, DataGroup record) {
		this.record = record;
		List<Map<String, Set<String>>> requiredRules = calculateRulesForActionAndRecordType(action,
				recordType);
		Map<String, Set<String>> requiredRule = requiredRules.get(0);
		createRulePart(requiredRule, "createdBy", extractUserId());
		return requiredRules;
	}

	private Map<String, Set<String>> createRequiredRule() {
		return new TreeMap<>();
	}

	private void createRulePart(Map<String, Set<String>> requiredRule, String key,
			String... values) {
		Set<String> set = new HashSet<>();
		for (String value : values) {
			set.add(SYSTEM + value);
		}
		requiredRule.put(key, set);
	}

	private String extractUserId() {
		DataGroup recordInfo = record.getFirstGroupWithNameInData("recordInfo");
		DataGroup createdByGroup = recordInfo.getFirstGroupWithNameInData("createdBy");
		return createdByGroup.getFirstAtomicValueWithNameInData("linkedRecordId");
	}

	private void possiblyCreateRulesForCollectedPermissionTerms(DataGroup collectedData,
			List<Map<String, Set<String>>> requiredRules) {
		if (thereAreCollectedPermissionValuesFromData(collectedData)) {
			DataGroup permission = collectedData.getFirstGroupWithNameInData("permission");
			createRulesFromCollectedPermissionTerms(permission, requiredRules);
		}
	}

	private void createRulesFromCollectedPermissionTerms(DataGroup permission,
			List<Map<String, Set<String>>> requiredRules) {
		Map<String, Set<String>> requiredRule = requiredRules.get(0);

		List<DataGroup> collectedDataTerms = permission
				.getAllGroupsWithNameInData("collectedDataTerm");
		for (DataGroup collectedDataTerm : collectedDataTerms) {
			createRuleForCollectedDataTerm(requiredRule, collectedDataTerm);
		}
	}

	private void createRuleForCollectedDataTerm(Map<String, Set<String>> requiredRule,
			DataGroup collectedDataTerm) {
		DataGroup extraData = collectedDataTerm.getFirstGroupWithNameInData("extraData");
		String permissionKey = extraData.getFirstAtomicValueWithNameInData("permissionKey");
		String value = collectedDataTerm.getFirstAtomicValueWithNameInData("collectTermValue");
		createRulePart(requiredRule, permissionKey, value);
	}

	private boolean thereAreCollectedPermissionValuesFromData(DataGroup collectedData) {
		return collectedData.containsChildWithNameInData("permission");
	}
}
