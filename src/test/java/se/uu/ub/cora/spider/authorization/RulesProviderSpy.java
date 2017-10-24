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
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import se.uu.ub.cora.spider.role.RulesProvider;

public class RulesProviderSpy implements RulesProvider {

	public String roleId;

	@Override
	public List<Map<String, Set<String>>> getActiveRules(String roleId) {
		this.roleId = roleId;
		ArrayList<Map<String, Set<String>>> rules = new ArrayList<>();

		HashMap<String, Set<String>> rule = new HashMap<>();
		rules.add(rule);
		HashSet<String> rulePart = new HashSet<>();
		rule.put("action", rulePart);
		rulePart.add("system.read");

		HashMap<String, Set<String>> rule2 = new HashMap<>();
		rules.add(rule2);
		HashSet<String> rulePart2 = new HashSet<>();
		rule2.put("action", rulePart2);
		rulePart2.add("system.update");

		return rules;
	}

}
