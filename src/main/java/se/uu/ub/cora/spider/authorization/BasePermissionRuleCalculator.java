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
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

import se.uu.ub.cora.bookkeeper.data.DataGroup;

public class BasePermissionRuleCalculator implements PermissionRuleCalculator {

	private static final String SYSTEM = "system.";
	private List<Map<String, Set<String>>> requiredRules;
	private DataGroup record;

	@Override
	public Set<String> calculateKeys(String accessType, String recordType, DataGroup record) {
		// TODO: remove this, user calculateRules instead
		Set<String> keys = new HashSet<>();
		String key = String.join(":", accessType, recordType.toUpperCase(), "SYSTEM", "*");
		keys.add(key);
		return keys;
	}

	@Override
	public Set<String> calculateKeysForList(String accessType, String recordType) {
		// TODO: remove this, user calculateRules instead
		Set<String> keys = new HashSet<>();
		String key = String.join(":", accessType, recordType.toUpperCase(), "SYSTEM", "*");
		keys.add(key);
		return keys;
	}

	@Override
	public List<Map<String, Set<String>>> calculateRules(String action, String recordType,
			DataGroup record) {
		this.record = record;
		requiredRules = new ArrayList<Map<String, Set<String>>>();
		Map<String, Set<String>> requiredRule = createRequiredRule();
		createRulePart(requiredRule, "action", SYSTEM + action);
		createRulePart(requiredRule, "recordType", SYSTEM + recordType);
		// createRulePart(requiredRule, "createdBy", SYSTEM + extractUserId());
		return requiredRules;
	}

	private Map<String, Set<String>> createRequiredRule() {
		Map<String, Set<String>> requiredRule = new TreeMap<>();
		requiredRules.add(requiredRule);
		return requiredRule;
	}

	private void createRulePart(Map<String, Set<String>> requiredRule, String key,
			String... values) {
		Set<String> set = new HashSet<String>();
		for (String value : values) {
			set.add(value);
		}
		requiredRule.put(key, set);
	}

	private String extractUserId() {
		return record.extractGroup("recordInfo").extractAtomicValue("createdBy");
	}
}
