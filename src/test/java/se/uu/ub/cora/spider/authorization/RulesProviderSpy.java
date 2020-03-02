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

package se.uu.ub.cora.spider.authorization;

import java.util.ArrayList;
import java.util.List;

import se.uu.ub.cora.beefeater.authorization.Rule;
import se.uu.ub.cora.beefeater.authorization.RuleImp;
import se.uu.ub.cora.beefeater.authorization.RulePartValuesImp;
import se.uu.ub.cora.spider.role.RulesProvider;

public class RulesProviderSpy implements RulesProvider {

	public String roleId;
	public List<String> roleIds = new ArrayList<>();
	public List<List<Rule>> returnedRules = new ArrayList<>();

	@Override
	public List<Rule> getActiveRules(String roleId) {
		this.roleId = roleId;
		roleIds.add(roleId);

		ArrayList<Rule> rules = new ArrayList<>();

		Rule rule = new RuleImp();
		rules.add(rule);
		RulePartValuesImp rulePart = new RulePartValuesImp();
		rule.addRulePart("action", rulePart);
		rulePart.add("system.read");

		Rule rule2 = new RuleImp();
		rules.add(rule2);
		RulePartValuesImp rulePart2 = new RulePartValuesImp();
		rule2.addRulePart("action", rulePart2);
		rulePart2.add("system.update");

		returnedRules.add(rules);

		return rules;
	}

}
