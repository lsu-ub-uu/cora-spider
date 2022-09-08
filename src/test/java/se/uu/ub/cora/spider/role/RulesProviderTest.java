/*
 * Copyright 2016, 2018, 2019 Uppsala University Library
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
import static org.testng.Assert.assertSame;
import static org.testng.Assert.assertTrue;

import java.util.List;

import org.testng.annotations.Test;

import se.uu.ub.cora.beefeater.authorization.Rule;
import se.uu.ub.cora.beefeater.authorization.RuleImp;
import se.uu.ub.cora.beefeater.authorization.RulePartValues;
import se.uu.ub.cora.data.DataAtomic;
import se.uu.ub.cora.data.DataGroup;
import se.uu.ub.cora.spider.spy.OldRecordStorageSpy;
import se.uu.ub.cora.spider.spy.RulesRecordPartRecordStorageSpy;
import se.uu.ub.cora.storage.RecordStorage;

public class RulesProviderTest {
	@Test
	public void test() {
		RecordStorage recordStorage = new OldRecordStorageSpy();
		RulesProvider rulesProvider = new RulesProviderImp(recordStorage);
		String roleId = "guest";
		List<Rule> rules = rulesProvider.getActiveRules(roleId);
		assertEquals(rules.size(), 1);
		Rule rule = rules.get(0);
		RulePartValues actionRulePart = rule.getRulePartValuesForKey("action");
		assertEquals(actionRulePart.size(), 2);
		assertTrue(actionRulePart.contains("system.create"));
		assertTrue(actionRulePart.contains("system.read"));
	}

	@Test
	public void testGetRecordStorage() {
		RecordStorage recordStorage = new OldRecordStorageSpy();
		RulesProviderImp rulesProvider = new RulesProviderImp(recordStorage);
		assertSame(rulesProvider.getRecordStorage(), recordStorage);
	}

	@Test
	public void testWithPermissionTerms() {
		RecordStorage recordStorage = new OldRecordStorageSpy();
		RulesProvider rulesProvider = new RulesProviderImp(recordStorage);
		String roleId = "guestWithPermissionTerms";
		List<Rule> rules = rulesProvider.getActiveRules(roleId);
		assertEquals(rules.size(), 2);
		Rule rule = rules.get(0);
		RulePartValues actionRulePart = rule.getRulePartValuesForKey("action");
		assertEquals(actionRulePart.size(), 2);
		assertTrue(actionRulePart.contains("system.create"));
		assertTrue(actionRulePart.contains("system.read"));

		RulePartValues recordTypeRulePart = rule.getRulePartValuesForKey("recordType");
		assertEquals(recordTypeRulePart.size(), 2);
		assertTrue(recordTypeRulePart.contains("system.person"));
		assertTrue(recordTypeRulePart.contains("system.place"));

		Rule rule2 = rules.get(1);
		RulePartValues actionRulePart2 = rule2.getRulePartValuesForKey("action");
		assertEquals(actionRulePart2.size(), 2);
		assertTrue(actionRulePart2.contains("system.create"));
		assertTrue(actionRulePart2.contains("system.read"));

		RulePartValues permissionPublishedRulePart = rule2
				.getRulePartValuesForKey("PUBLISHED_STATUS");
		assertEquals(permissionPublishedRulePart.size(), 1);
		assertTrue(permissionPublishedRulePart.contains("system.published"));
	}

	@Test
	public void testWithMultiplePermissionTerms() {
		RecordStorage recordStorage = new OldRecordStorageSpy();
		RulesProvider rulesProvider = new RulesProviderImp(recordStorage);
		String roleId = "guestWithMultiplePermissionTerms";
		List<Rule> rules = rulesProvider.getActiveRules(roleId);
		assertEquals(rules.size(), 3);
		Rule rule = rules.get(0);
		RulePartValues actionRulePart = rule.getRulePartValuesForKey("action");
		assertEquals(actionRulePart.size(), 2);
		assertTrue(actionRulePart.contains("system.create"));
		assertTrue(actionRulePart.contains("system.read"));

		RulePartValues recordTypeRulePart = rule.getRulePartValuesForKey("recordType");
		assertEquals(recordTypeRulePart.size(), 2);
		assertTrue(recordTypeRulePart.contains("system.person"));
		assertTrue(recordTypeRulePart.contains("system.place"));

		Rule rule2 = rules.get(1);
		RulePartValues actionRulePart2 = rule2.getRulePartValuesForKey("action");
		assertEquals(actionRulePart2.size(), 2);
		assertTrue(actionRulePart2.contains("system.create"));
		assertTrue(actionRulePart2.contains("system.read"));

		RulePartValues permissionPublishedRulePart = rule2
				.getRulePartValuesForKey("PUBLISHED_STATUS");
		assertEquals(permissionPublishedRulePart.size(), 1);
		assertTrue(permissionPublishedRulePart.contains("system.published"));

		Rule rule3 = rules.get(2);
		RulePartValues actionRulePart3 = rule3.getRulePartValuesForKey("action");
		assertEquals(actionRulePart3.size(), 2);
		assertTrue(actionRulePart3.contains("system.create"));
		assertTrue(actionRulePart3.contains("system.read"));

		RulePartValues permissionPublishedRulePart2 = rule3
				.getRulePartValuesForKey("DELETED_STATUS");
		assertEquals(permissionPublishedRulePart2.size(), 2);
		assertTrue(permissionPublishedRulePart2.contains("system.deleted"));
		assertTrue(permissionPublishedRulePart2.contains("system.notDeleted"));

		RulePartValues permissionPublishedRulePart3 = rule3
				.getRulePartValuesForKey("PUBLISHED_STATUS");
		assertEquals(permissionPublishedRulePart3.size(), 2);
		assertTrue(permissionPublishedRulePart3.contains("system.published"));
		assertTrue(permissionPublishedRulePart3.contains("system.notPublished"));

	}

	@Test
	public void testInactiveRole() {
		RecordStorage recordStorage = new OldRecordStorageSpy();
		RulesProvider rulesProvider = new RulesProviderImp(recordStorage);
		String roleId = "inactive";
		List<Rule> rules = rulesProvider.getActiveRules(roleId);
		assertEquals(rules.size(), 0);
	}

	@Test
	public void testNotFoundRole() {
		RecordStorage recordStorage = new OldRecordStorageSpy();
		RulesProvider rulesProvider = new RulesProviderImp(recordStorage);
		String roleId = "roleNotFoundInStorage";
		List<Rule> rules = rulesProvider.getActiveRules(roleId);
		assertEquals(rules.size(), 0);
	}

	@Test
	public void testWithReadRecordPartPermissions() {
		RulesRecordPartRecordStorageSpy recordStorage = new RulesRecordPartRecordStorageSpy();
		RulesProvider rulesProvider = new RulesProviderImp(recordStorage);

		List<Rule> rules = rulesProvider.getActiveRules("roleWithReadRecordPartPermissions");
		assertEquals(rules.size(), 2);

		DataGroup ruleInStorage = recordStorage.returnedReadDataGroups.get(1);
		assertCorrectRuleWithOneReadPermission(rules, ruleInStorage);

		DataGroup secondRuleInStorage = recordStorage.returnedReadDataGroups.get(2);
		assertCorrectRuleWithTwoReadPermissions(rules, secondRuleInStorage);

	}

	private void assertCorrectRuleWithOneReadPermission(List<Rule> rules, DataGroup ruleInStorage) {
		RuleImp rule = (RuleImp) rules.get(0);
		assertEquals(rule.getReadRecordPartPermissions().size(), 1);

		DataGroup readPermissionsFromRuleInStorage = ruleInStorage
				.getFirstGroupWithNameInData("readPermissions");

		DataAtomic firstReadPermissionFromStorage = readPermissionsFromRuleInStorage
				.getAllDataAtomicsWithNameInData("readPermission").get(0);

		assertEquals(rule.getReadRecordPartPermissions().get(0),
				firstReadPermissionFromStorage.getValue());
	}

	private void assertCorrectRuleWithTwoReadPermissions(List<Rule> rules,
			DataGroup ruleInStorage) {
		RuleImp ruleWithTwoReadPermissions = (RuleImp) rules.get(1);

		DataGroup readPermissionsFromRuleInStorage = ruleInStorage
				.getFirstGroupWithNameInData("readPermissions");

		DataAtomic firstReadPermissionFromStorage = readPermissionsFromRuleInStorage
				.getAllDataAtomicsWithNameInData("readPermission").get(0);
		assertEquals(ruleWithTwoReadPermissions.getReadRecordPartPermissions().get(0),
				firstReadPermissionFromStorage.getValue());

		DataAtomic secondReadPermissionFromStorage = readPermissionsFromRuleInStorage
				.getAllDataAtomicsWithNameInData("readPermission").get(1);
		assertEquals(ruleWithTwoReadPermissions.getReadRecordPartPermissions().get(1),
				secondReadPermissionFromStorage.getValue());
	}

	@Test
	public void testWithWriteRecordPartPermissions() {
		RulesRecordPartRecordStorageSpy recordStorage = new RulesRecordPartRecordStorageSpy();
		RulesProvider rulesProvider = new RulesProviderImp(recordStorage);

		List<Rule> rules = rulesProvider.getActiveRules("roleWithWriteRecordPartPermissions");
		assertEquals(rules.size(), 2);

		DataGroup ruleInStorage = recordStorage.returnedReadDataGroups.get(1);
		assertCorrectRuleWithOneWritePermission(rules, ruleInStorage);

		DataGroup secondRuleInStorage = recordStorage.returnedReadDataGroups.get(2);
		assertCorrectRuleWithTwoWritePermissions(rules, secondRuleInStorage);
	}

	private void assertCorrectRuleWithOneWritePermission(List<Rule> rules,
			DataGroup ruleInStorage) {
		RuleImp rule = (RuleImp) rules.get(0);
		assertEquals(rule.getWriteRecordPartPermissions().size(), 1);

		DataGroup writePermissionsFromRuleInStorage = ruleInStorage
				.getFirstGroupWithNameInData("writePermissions");

		DataAtomic firstWritePermissionFromStorage = writePermissionsFromRuleInStorage
				.getAllDataAtomicsWithNameInData("writePermission").get(0);

		assertEquals(rule.getWriteRecordPartPermissions().get(0),
				firstWritePermissionFromStorage.getValue());
	}

	private void assertCorrectRuleWithTwoWritePermissions(List<Rule> rules,
			DataGroup ruleInStorage) {
		RuleImp ruleWithTwoWritePermissions = (RuleImp) rules.get(1);

		DataGroup writePermissionsFromRuleInStorage = ruleInStorage
				.getFirstGroupWithNameInData("writePermissions");

		DataAtomic firstReadPermissionFromStorage = writePermissionsFromRuleInStorage
				.getAllDataAtomicsWithNameInData("writePermission").get(0);
		assertEquals(ruleWithTwoWritePermissions.getWriteRecordPartPermissions().get(0),
				firstReadPermissionFromStorage.getValue());

		DataAtomic secondWritePermissionFromStorage = writePermissionsFromRuleInStorage
				.getAllDataAtomicsWithNameInData("writePermission").get(1);
		assertEquals(ruleWithTwoWritePermissions.getWriteRecordPartPermissions().get(1),
				secondWritePermissionFromStorage.getValue());
	}

	@Test
	public void testReadAndWritePermissions() {
		RulesRecordPartRecordStorageSpy recordStorage = new RulesRecordPartRecordStorageSpy();
		RulesProvider rulesProvider = new RulesProviderImp(recordStorage);

		List<Rule> rules = rulesProvider
				.getActiveRules("roleWithReadAndWriteRecordPartPermissions");
		assertEquals(rules.size(), 1);

		RuleImp rule = (RuleImp) rules.get(0);
		assertEquals(rule.getWriteRecordPartPermissions().size(), 2);
		List<String> readPermissionsInRule = rule.getReadRecordPartPermissions();
		assertEquals(readPermissionsInRule.size(), 3);

		List<DataAtomic> writePermissionsFromMetadata = getWritePermissionsFromRuleInStorage(
				recordStorage);

		assertWriteFromMetadataIsAddedToReadInPopulatedRule(readPermissionsInRule,
				writePermissionsFromMetadata);

	}

	private void assertWriteFromMetadataIsAddedToReadInPopulatedRule(
			List<String> readPermissionsInRule, List<DataAtomic> writePermissionsFromMetadata) {
		assertEquals(readPermissionsInRule.get(1), writePermissionsFromMetadata.get(0).getValue());
		assertEquals(readPermissionsInRule.get(2), writePermissionsFromMetadata.get(1).getValue());
	}

	private List<DataAtomic> getWritePermissionsFromRuleInStorage(
			RulesRecordPartRecordStorageSpy recordStorage) {
		DataGroup ruleFromStorage = recordStorage.returnedReadDataGroups.get(1);
		DataGroup writePermissionsFromRuleInStorage = ruleFromStorage
				.getFirstGroupWithNameInData("writePermissions");

		List<DataAtomic> writePermissions = writePermissionsFromRuleInStorage
				.getAllDataAtomicsWithNameInData("writePermission");
		return writePermissions;
	}

}
