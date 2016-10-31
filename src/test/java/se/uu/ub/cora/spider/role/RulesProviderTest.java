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

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertTrue;

import java.util.List;
import java.util.Map;
import java.util.Set;

import org.testng.annotations.Test;

import se.uu.ub.cora.spider.record.storage.RecordStorage;
import se.uu.ub.cora.spider.spy.RecordStorageSpy;

public class RulesProviderTest {
	@Test
	public void test() {
		RecordStorage recordStorage = new RecordStorageSpy();
		RulesProvider rulesProvider = new RulesProvider(recordStorage);
		String roleId = "guest";
		List<Map<String, Set<String>>> rules = rulesProvider.getActiveRules(roleId);
		assertEquals(rules.size(), 1);
		Map<String, Set<String>> rule = rules.get(0);
		Set<String> actionRulePart = rule.get("action");
		assertEquals(actionRulePart.size(), 2);
		assertTrue(actionRulePart.contains("system.create"));
		assertTrue(actionRulePart.contains("system.read"));
	}

	@Test
	public void testInactiveRole() {
		RecordStorage recordStorage = new RecordStorageSpy();
		RulesProvider rulesProvider = new RulesProvider(recordStorage);
		String roleId = "inactive";
		List<Map<String, Set<String>>> rules = rulesProvider.getActiveRules(roleId);
		assertEquals(rules.size(), 0);
	}

	@Test
	public void testNotFoundRole() {
		RecordStorage recordStorage = new RecordStorageSpy();
		RulesProvider rulesProvider = new RulesProvider(recordStorage);
		String roleId = "roleNotFoundInStorage";
		List<Map<String, Set<String>>> rules = rulesProvider.getActiveRules(roleId);
		assertEquals(rules.size(), 0);
	}
}
