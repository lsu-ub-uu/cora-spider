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

package se.uu.ub.cora.spider.role;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Stream;

import se.uu.ub.cora.bookkeeper.data.DataAtomic;
import se.uu.ub.cora.bookkeeper.data.DataElement;
import se.uu.ub.cora.bookkeeper.data.DataGroup;
import se.uu.ub.cora.spider.record.storage.RecordStorage;

public class RulesProviderImp implements RulesProvider {

	private RecordStorage recordStorage;

	public RulesProviderImp(RecordStorage recordStorage) {
		this.recordStorage = recordStorage;
	}

	@Override
	public List<Map<String, Set<String>>> getActiveRules(String roleId) {
		DataGroup readRole = recordStorage.read("permissionRole", roleId);
		if (roleNotFoundInStorage(readRole)) {
			return new ArrayList<>();
		}
		if (roleIsInactive(readRole)) {
			return new ArrayList<>();
		}

		return getActiveRulesForRole(readRole);
	}

	private boolean roleNotFoundInStorage(DataGroup readRole) {
		return null == readRole;
	}

	private boolean roleIsInactive(DataGroup readRole) {
		return !"active".equals(readRole.getFirstAtomicValueWithNameInData("activeStatus"));
	}

	private List<Map<String, Set<String>>> getActiveRulesForRole(DataGroup readRole) {
		List<Map<String, Set<String>>> listOfRules = new ArrayList<>();
		List<DataElement> children = readRole.getChildren();
		Stream<DataElement> permissionRuleLinks = children.stream()
				.filter(child -> "permissionRuleLink".equals(child.getNameInData()));

		permissionRuleLinks.forEach(rule -> addRuleToListOfRules(rule, listOfRules));
		return listOfRules;
	}

	private void addRuleToListOfRules(DataElement dataElementRule,
			List<Map<String, Set<String>>> listOfRules) {
		String linkedRecordId = ((DataGroup) dataElementRule).getFirstAtomicValueWithNameInData("linkedRecordId");
		DataGroup readRule = recordStorage.read("permissionRule", linkedRecordId);
		if ("active".equals(readRule.getFirstAtomicValueWithNameInData("activeStatus"))) {

			List<DataElement> children = readRule.getChildren();
			Stream<DataElement> permissionRuleParts = children.stream()
					.filter(child -> "permissionRulePart".equals(child.getNameInData()));

			Map<String, Set<String>> rule = new HashMap<>();
			listOfRules.add(rule);
			permissionRuleParts.forEach(rulePart -> addRulePartToRule(rulePart, rule));
		}
	}

	private void addRulePartToRule(DataElement rulePart, Map<String, Set<String>> rule) {
		Set<String> ruleValues = new HashSet<>();
		List<DataElement> children = ((DataGroup) rulePart).getChildren();
		children.forEach(ruleValue -> ruleValues.add(((DataAtomic) ruleValue).getValue()));
		rule.put(rulePart.getAttributes().get("type"), ruleValues);
	}

}
