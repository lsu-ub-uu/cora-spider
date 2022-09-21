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

import se.uu.ub.cora.beefeater.authorization.RulePartValuesImp;
import se.uu.ub.cora.data.collected.PermissionTerm;

public final class CollectedDataPermissionRulePartExtractor {
	private static final String SYSTEM = "system.";
	private List<PermissionTerm> permissionTerms;
	private Map<String, List<RulePartValuesImp>> sortedRulePartValues = new HashMap<>();

	private CollectedDataPermissionRulePartExtractor(List<PermissionTerm> permissionTerms) {
		this.permissionTerms = permissionTerms;
	}

	static Map<String, List<RulePartValuesImp>> extractRulePartsSortedByPermissionKeyFromCollectedData(
			List<PermissionTerm> permissionTerms) {
		CollectedDataPermissionRulePartExtractor instance = new CollectedDataPermissionRulePartExtractor(
				permissionTerms);
		return instance.sortPermissionsByPermissionKey();
	}

	private Map<String, List<RulePartValuesImp>> sortPermissionsByPermissionKey() {
		sortCollectedDataTermsByPermissionKey(permissionTerms);
		return sortedRulePartValues;
	}

	private void sortCollectedDataTermsByPermissionKey(List<PermissionTerm> permissionTerms) {
		for (PermissionTerm permissionTerm : permissionTerms) {
			sortCollectedDataTermIntoRulePartValuesBasedOnPermissionKey(permissionTerm);
		}
	}

	private void sortCollectedDataTermIntoRulePartValuesBasedOnPermissionKey(
			PermissionTerm permissionTerm) {
		RulePartValuesImp rulePartValues = createRulePartValuesForCollectedDataTerm(permissionTerm);

		ensureSortedRulePartValuesHasListForPermissionKey(permissionTerm.permissionKey());
		addRulePartValuesToSortedRulePartValuesUnderKey(permissionTerm.permissionKey(),
				rulePartValues);
	}

	private RulePartValuesImp createRulePartValuesForCollectedDataTerm(
			PermissionTerm permissionTerm) {
		RulePartValuesImp rulePartValues = new RulePartValuesImp();
		rulePartValues.add(SYSTEM + permissionTerm.value());
		return rulePartValues;
	}

	private void ensureSortedRulePartValuesHasListForPermissionKey(String permissionKey) {
		if (!sortedRulePartValues.containsKey(permissionKey)) {
			sortedRulePartValues.put(permissionKey, new ArrayList<>());
		}
	}

	private void addRulePartValuesToSortedRulePartValuesUnderKey(String permissionKey,
			RulePartValuesImp rulePartValues) {
		sortedRulePartValues.get(permissionKey).add(rulePartValues);
	}
}
