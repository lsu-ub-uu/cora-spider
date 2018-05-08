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
 */
package se.uu.ub.cora.spider.authorization;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import se.uu.ub.cora.beefeater.authorization.RulePartValues;
import se.uu.ub.cora.bookkeeper.data.DataGroup;

public final class CollectedDataPermissionRulePartExtractor {
	private static final String SYSTEM = "system.";
	private DataGroup collectedData;
	private Map<String, List<RulePartValues>> sortedRulePartValues = new HashMap<>();

	private CollectedDataPermissionRulePartExtractor(DataGroup collectedData) {
		this.collectedData = collectedData;
	}

	static Map<String, List<RulePartValues>> extractRulePartsSortedByPermissionKeyFromCollectedData(
			DataGroup collectedData) {
		CollectedDataPermissionRulePartExtractor instance = new CollectedDataPermissionRulePartExtractor(
				collectedData);
		return instance.sortPermissionsByPermissionKey();
	}

	private Map<String, List<RulePartValues>> sortPermissionsByPermissionKey() {
		DataGroup permission = collectedData.getFirstGroupWithNameInData("permission");
		List<DataGroup> collectedDataTerms = permission
				.getAllGroupsWithNameInData("collectedDataTerm");
		sortCollectedDataTermsByPermissionKey(collectedDataTerms);
		return sortedRulePartValues;
	}

	private void sortCollectedDataTermsByPermissionKey(List<DataGroup> collectedDataTerms) {
		for (DataGroup collectedDataTerm : collectedDataTerms) {
			sortCollectedDataTermIntoRulePartValuesBasedOnPermissionKey(collectedDataTerm);
		}
	}

	private void sortCollectedDataTermIntoRulePartValuesBasedOnPermissionKey(
			DataGroup collectedDataTerm) {
		String permissionKey = extractPermissionKeyFromCollectedDataTerm(collectedDataTerm);
		RulePartValues rulePartValues = createRulePartValuesForCollectedDataTerm(collectedDataTerm);

		ensureSortedRulePartValuesHasListForPermissionKey(permissionKey);
		addRulePartValuesToSortedRulePartValuesUnderKey(permissionKey, rulePartValues);
	}

	private String extractPermissionKeyFromCollectedDataTerm(DataGroup collectedDataTerm) {
		DataGroup extraData = collectedDataTerm.getFirstGroupWithNameInData("extraData");
		return extraData.getFirstAtomicValueWithNameInData("permissionKey");
	}

	private RulePartValues createRulePartValuesForCollectedDataTerm(DataGroup collectedDataTerm) {
		String value = collectedDataTerm.getFirstAtomicValueWithNameInData("collectTermValue");
		RulePartValues rulePartValues = new RulePartValues();
		rulePartValues.add(SYSTEM + value);
		return rulePartValues;
	}

	private void ensureSortedRulePartValuesHasListForPermissionKey(String permissionKey) {
		if (!sortedRulePartValues.containsKey(permissionKey)) {
			sortedRulePartValues.put(permissionKey, new ArrayList<>());
		}
	}

	private void addRulePartValuesToSortedRulePartValuesUnderKey(String permissionKey,
			RulePartValues rulePartValues) {
		sortedRulePartValues.get(permissionKey).add(rulePartValues);
	}
}
