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

package se.uu.ub.cora.spider.role;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertTrue;

import java.util.List;
import java.util.Set;

import org.testng.annotations.Test;

import se.uu.ub.cora.beefeater.authorization.Rule;
import se.uu.ub.cora.beefeater.authorization.RulePartValues;
import se.uu.ub.cora.spider.spy.RecordStorageSpy;
import se.uu.ub.cora.storage.RecordStorage;

public class RulesProviderTest {
	@Test
	public void test() {
		RecordStorage recordStorage = new RecordStorageSpy();
		RulesProvider rulesProvider = new RulesProviderImp(recordStorage);
		String roleId = "guest";
		List<Rule> rules = rulesProvider.getActiveRules(roleId);
		assertEquals(rules.size(), 1);
		Rule rule = rules.get(0);
		Set<String> actionRulePart = rule.get("action");
		assertEquals(actionRulePart.size(), 2);
		assertTrue(actionRulePart.contains("system.create"));
		assertTrue(actionRulePart.contains("system.read"));
	}

	@Test
	public void testWithPermissionTerms() {
		RecordStorage recordStorage = new RecordStorageSpy();
		RulesProvider rulesProvider = new RulesProviderImp(recordStorage);
		String roleId = "guestWithPermissionTerms";
		List<Rule> rules = rulesProvider.getActiveRules(roleId);
		assertEquals(rules.size(), 2);
		Rule rule = rules.get(0);
		RulePartValues actionRulePart = rule.get("action");
		assertEquals(actionRulePart.size(), 2);
		assertTrue(actionRulePart.contains("system.create"));
		assertTrue(actionRulePart.contains("system.read"));

		RulePartValues recordTypeRulePart = rule.get("recordType");
		assertEquals(recordTypeRulePart.size(), 2);
		assertTrue(recordTypeRulePart.contains("system.person"));
		assertTrue(recordTypeRulePart.contains("system.place"));

		Rule rule2 = rules.get(1);
		RulePartValues actionRulePart2 = rule2.get("action");
		assertEquals(actionRulePart2.size(), 2);
		assertTrue(actionRulePart2.contains("system.create"));
		assertTrue(actionRulePart2.contains("system.read"));

		RulePartValues permissionPublishedRulePart = rule2.get("PUBLISHED_STATUS");
		assertEquals(permissionPublishedRulePart.size(), 1);
		assertTrue(permissionPublishedRulePart.contains("system.published"));
	}

	@Test
	public void testWithMultiplePermissionTerms() {
		RecordStorage recordStorage = new RecordStorageSpy();
		RulesProvider rulesProvider = new RulesProviderImp(recordStorage);
		String roleId = "guestWithMultiplePermissionTerms";
		List<Rule> rules = rulesProvider.getActiveRules(roleId);
		assertEquals(rules.size(), 3);
		Rule rule = rules.get(0);
		RulePartValues actionRulePart = rule.get("action");
		assertEquals(actionRulePart.size(), 2);
		assertTrue(actionRulePart.contains("system.create"));
		assertTrue(actionRulePart.contains("system.read"));

		RulePartValues recordTypeRulePart = rule.get("recordType");
		assertEquals(recordTypeRulePart.size(), 2);
		assertTrue(recordTypeRulePart.contains("system.person"));
		assertTrue(recordTypeRulePart.contains("system.place"));

		Rule rule2 = rules.get(1);
		RulePartValues actionRulePart2 = rule2.get("action");
		assertEquals(actionRulePart2.size(), 2);
		assertTrue(actionRulePart2.contains("system.create"));
		assertTrue(actionRulePart2.contains("system.read"));

		RulePartValues permissionPublishedRulePart = rule2.get("PUBLISHED_STATUS");
		assertEquals(permissionPublishedRulePart.size(), 1);
		assertTrue(permissionPublishedRulePart.contains("system.published"));

		Rule rule3 = rules.get(2);
		RulePartValues actionRulePart3 = rule3.get("action");
		assertEquals(actionRulePart3.size(), 2);
		assertTrue(actionRulePart3.contains("system.create"));
		assertTrue(actionRulePart3.contains("system.read"));

		RulePartValues permissionPublishedRulePart2 = rule3.get("DELETED_STATUS");
		assertEquals(permissionPublishedRulePart2.size(), 2);
		assertTrue(permissionPublishedRulePart2.contains("system.deleted"));
		assertTrue(permissionPublishedRulePart2.contains("system.notDeleted"));

		RulePartValues permissionPublishedRulePart3 = rule3.get("PUBLISHED_STATUS");
		assertEquals(permissionPublishedRulePart3.size(), 2);
		assertTrue(permissionPublishedRulePart3.contains("system.published"));
		assertTrue(permissionPublishedRulePart3.contains("system.notPublished"));

	}

	@Test
	public void testInactiveRole() {
		RecordStorage recordStorage = new RecordStorageSpy();
		RulesProvider rulesProvider = new RulesProviderImp(recordStorage);
		String roleId = "inactive";
		List<Rule> rules = rulesProvider.getActiveRules(roleId);
		assertEquals(rules.size(), 0);
	}

	@Test
	public void testNotFoundRole() {
		RecordStorage recordStorage = new RecordStorageSpy();
		RulesProvider rulesProvider = new RulesProviderImp(recordStorage);
		String roleId = "roleNotFoundInStorage";
		List<Rule> rules = rulesProvider.getActiveRules(roleId);
		assertEquals(rules.size(), 0);
	}
}
