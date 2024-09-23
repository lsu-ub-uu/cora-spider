/*
 * Copyright 2016, 2018, 2019, 2024 Uppsala University Library
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
import java.util.Collections;
import java.util.List;

import se.uu.ub.cora.beefeater.authorization.Rule;
import se.uu.ub.cora.beefeater.authorization.RuleImp;
import se.uu.ub.cora.beefeater.authorization.RulePartValuesImp;
import se.uu.ub.cora.data.DataAtomic;
import se.uu.ub.cora.data.DataChild;
import se.uu.ub.cora.data.DataGroup;
import se.uu.ub.cora.data.DataRecordGroup;
import se.uu.ub.cora.data.DataRecordLink;
import se.uu.ub.cora.storage.RecordNotFoundException;
import se.uu.ub.cora.storage.RecordStorage;

public class RulesProviderImp implements RulesProvider {

	private RecordStorage recordStorage;

	public RulesProviderImp(RecordStorage recordStorage) {
		this.recordStorage = recordStorage;
	}

	@Override
	public List<Rule> getActiveRules(String roleId) {
		try {
			DataRecordGroup readRole = recordStorage.read("permissionRole", roleId);
			if (isInactiveRole(readRole)) {
				return Collections.emptyList();
			}
			return getActiveRulesForRole(readRole);
		} catch (RecordNotFoundException e) {
			return Collections.emptyList();
		}
	}

	private boolean isInactiveRole(DataRecordGroup readRole) {
		return !dataContainsActiveStatusAsActive(readRole);
	}

	private List<Rule> getActiveRulesForRole(DataRecordGroup readRole) {
		List<Rule> listOfRules = new ArrayList<>();
		List<DataRecordLink> ruleLinks = readRole.getChildrenOfTypeAndName(DataRecordLink.class,
				"permissionRuleLink");
		for (DataRecordLink ruleLink : ruleLinks) {
			possiblyAddRuleToListOfRules(ruleLink, listOfRules);
		}
		return listOfRules;
	}

	private void possiblyAddRuleToListOfRules(DataRecordLink dataElementRule,
			List<Rule> listOfRules) {
		DataRecordGroup readRule = getLinkedRuleFromStorage(dataElementRule);
		if (dataContainsActiveStatusAsActive(readRule)) {
			addRuleToListOfRules(listOfRules, readRule);
		}
	}

	private DataRecordGroup getLinkedRuleFromStorage(DataRecordLink ruleLink) {
		return recordStorage.read("permissionRule", ruleLink.getLinkedRecordId());
	}

	private boolean dataContainsActiveStatusAsActive(DataRecordGroup readRule) {
		return "active".equals(readRule.getFirstAtomicValueWithNameInData("activeStatus"));
	}

	private void addRuleToListOfRules(List<Rule> listOfRules, DataRecordGroup readRule) {
		Rule rule = new RuleImp();
		listOfRules.add(rule);

		addRulePartsToRule(rule, readRule);
		addTermRulePartToRule(rule, readRule);
		possiblyAddReadPermissions(rule, readRule);
		possiblyAddWritePermissions(rule, readRule);
		addAllWritePermissionsToReadPermissionsAsReadIsImplied(rule);
	}

	private void addRulePartsToRule(Rule rule, DataRecordGroup readRule) {
		List<DataGroup> permissionRuleParts = readRule
				.getAllGroupsWithNameInData("permissionRulePart");
		permissionRuleParts.forEach(rulePart -> addRulePartToRule(rulePart, rule));
	}

	private void addRulePartToRule(DataGroup rulePart, Rule rule) {
		RulePartValuesImp ruleValues = createRulePartValuesForRulePart(rulePart);
		rule.addRulePart(rulePart.getAttribute("type").getValue(), ruleValues);
	}

	private RulePartValuesImp createRulePartValuesForRulePart(DataGroup rulePart) {
		RulePartValuesImp ruleValues = new RulePartValuesImp();
		List<DataChild> rulePartsValues = rulePart.getChildren();
		rulePartsValues.forEach(ruleValue -> ruleValues.add(((DataAtomic) ruleValue).getValue()));
		return ruleValues;
	}

	private void addTermRulePartToRule(Rule rule, DataRecordGroup readRule) {
		List<DataGroup> permissionTermRuleParts = readRule
				.getAllGroupsWithNameInData("permissionTermRulePart");
		permissionTermRuleParts.forEach(rulePart -> addTermRulePartToRule(rulePart, rule));
	}

	private void addTermRulePartToRule(DataGroup ruleTermPart, Rule rule) {
		RulePartValuesImp ruleValues = new RulePartValuesImp();

		List<DataAtomic> valueChildren = ruleTermPart.getAllDataAtomicsWithNameInData("value");
		valueChildren.forEach(ruleValue -> ruleValues.add(ruleValue.getValue()));

		String permissionKey = getPermissionKeyForRuleTermPart(ruleTermPart);
		rule.addRulePart(permissionKey, ruleValues);
	}

	private String getPermissionKeyForRuleTermPart(DataGroup ruleTermPart) {
		DataRecordLink permissionTermRuleLink = ruleTermPart
				.getFirstChildOfTypeAndName(DataRecordLink.class, "rule");
		DataRecordGroup permissionTerm = recordStorage.read("collectTerm",
				permissionTermRuleLink.getLinkedRecordId());
		DataGroup extraData = permissionTerm.getFirstGroupWithNameInData("extraData");
		return extraData.getFirstAtomicValueWithNameInData("permissionKey");
	}

	private void possiblyAddReadPermissions(Rule rule, DataRecordGroup readRule) {
		if (readRule.containsChildWithNameInData("readPermissions")) {
			addReadPermissions(rule, readRule);
		}
	}

	private void addReadPermissions(Rule rule, DataRecordGroup readRule) {
		DataGroup readPermissions = readRule.getFirstGroupWithNameInData("readPermissions");
		for (DataAtomic readPermission : readPermissions
				.getAllDataAtomicsWithNameInData("readPermission")) {
			rule.addReadRecordPartPermission(readPermission.getValue());
		}
	}

	private void possiblyAddWritePermissions(Rule rule, DataRecordGroup readRule) {
		if (readRule.containsChildWithNameInData("writePermissions")) {
			addWritePermissions(rule, readRule);
		}
	}

	private void addWritePermissions(Rule rule, DataRecordGroup readRule) {
		DataGroup writePermissions = readRule.getFirstGroupWithNameInData("writePermissions");
		for (DataAtomic writePermission : writePermissions
				.getAllDataAtomicsWithNameInData("writePermission")) {
			rule.addWriteRecordPartPermission(writePermission.getValue());
		}
	}

	private void addAllWritePermissionsToReadPermissionsAsReadIsImplied(Rule rule) {
		for (String writePermission : rule.getWriteRecordPartPermissions()) {
			rule.addReadRecordPartPermission(writePermission);
		}
	}

	public RecordStorage onlyForTestGetRecordStorage() {
		return recordStorage;
	}
}
