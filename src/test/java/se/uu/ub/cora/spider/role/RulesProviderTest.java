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

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertSame;
import static org.testng.Assert.assertTrue;

import java.util.Arrays;
import java.util.List;

import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import se.uu.ub.cora.beefeater.authorization.Rule;
import se.uu.ub.cora.beefeater.authorization.RuleImp;
import se.uu.ub.cora.beefeater.authorization.RulePartValues;
import se.uu.ub.cora.data.DataAtomic;
import se.uu.ub.cora.data.DataGroup;
import se.uu.ub.cora.data.DataRecordLink;
import se.uu.ub.cora.data.spies.DataAtomicSpy;
import se.uu.ub.cora.data.spies.DataAttributeSpy;
import se.uu.ub.cora.data.spies.DataGroupSpy;
import se.uu.ub.cora.data.spies.DataRecordGroupSpy;
import se.uu.ub.cora.data.spies.DataRecordLinkSpy;
import se.uu.ub.cora.spider.spy.RulesRecordPartRecordStorageSpy;
import se.uu.ub.cora.storage.RecordNotFoundException;
import se.uu.ub.cora.storage.spies.RecordStorageSpy;

public class RulesProviderTest {

	private static final String SOME_ROLE = "someRole";
	private RecordStorageSpy recordStorage;
	private RulesProviderImp rulesProvider;

	@BeforeMethod
	private void beforeMethod() {
		recordStorage = new RecordStorageSpy();
		rulesProvider = new RulesProviderImp(recordStorage);
	}

	@Test
	public void testGetRecordStorage() {
		assertSame(rulesProvider.getRecordStorage(), recordStorage);
	}

	@Test
	public void testRoleNotFoundInStorage() {
		recordStorage.MRV.setThrowException("read",
				RecordNotFoundException.withMessage("someMessage"), "permissionRole", SOME_ROLE);

		List<Rule> rules = rulesProvider.getActiveRules(SOME_ROLE);
		assertEquals(rules.size(), 0);
	}

	@Test
	public void inactiveRoleGivesEmptyRuleList() {
		DataRecordGroupSpy inactiveRole = createInactiveRole();
		recordStorage.MRV.setSpecificReturnValuesSupplier("read", () -> inactiveRole,
				"permissionRole", SOME_ROLE);

		List<Rule> rules = rulesProvider.getActiveRules(SOME_ROLE);

		recordStorage.MCR.assertParameters("read", 0, "permissionRole", SOME_ROLE);
		assertEquals(rules.size(), 0);
	}

	private DataRecordGroupSpy createInactiveRole() {
		DataRecordGroupSpy role = new DataRecordGroupSpy();
		role.MRV.setDefaultReturnValuesSupplier("getFirstAtomicValueWithNameInData",
				() -> "inactive");
		recordStorage.MRV.setSpecificReturnValuesSupplier("read", () -> role, "permissionRole",
				SOME_ROLE);
		return role;
	}

	private DataRecordGroupSpy createActiveRole() {
		DataRecordGroupSpy role = new DataRecordGroupSpy();
		role.MRV.setDefaultReturnValuesSupplier("getFirstAtomicValueWithNameInData",
				() -> "active");
		recordStorage.MRV.setSpecificReturnValuesSupplier("read", () -> role, "permissionRole",
				SOME_ROLE);
		return role;
	}

	private DataRecordGroupSpy createActiveRule(String id) {
		DataRecordGroupSpy rule = new DataRecordGroupSpy();
		rule.MRV.setDefaultReturnValuesSupplier("getFirstAtomicValueWithNameInData",
				() -> "active");
		recordStorage.MRV.setSpecificReturnValuesSupplier("read", () -> rule, "permissionRule", id);
		return rule;
	}

	private void addRulePartToRule(DataRecordGroupSpy rule, DataGroupSpy... rulePart) {
		rule.MRV.setSpecificReturnValuesSupplier("getAllGroupsWithNameInData",
				() -> Arrays.asList(rulePart), "permissionRulePart");
	}

	private DataGroupSpy createRulePartWithTwoActions() {
		DataAttributeSpy actionAttribute = new DataAttributeSpy();
		actionAttribute.MRV.setDefaultReturnValuesSupplier("getValue", () -> "action");
		DataAtomicSpy rulePartValue0 = new DataAtomicSpy();
		rulePartValue0.MRV.setDefaultReturnValuesSupplier("getValue", () -> "system.create");
		DataAtomicSpy rulePartValue1 = new DataAtomicSpy();
		rulePartValue1.MRV.setDefaultReturnValuesSupplier("getValue", () -> "system.read");

		DataGroupSpy rulePart = new DataGroupSpy();
		rulePart.MRV.setDefaultReturnValuesSupplier("getChildren",
				() -> List.of(rulePartValue0, rulePartValue1));
		rulePart.MRV.setSpecificReturnValuesSupplier("getAttribute", () -> actionAttribute, "type");
		return rulePart;
	}

	private DataGroupSpy createRulePartWithTwoRecordTypes() {
		DataAttributeSpy actionAttribute = new DataAttributeSpy();
		actionAttribute.MRV.setDefaultReturnValuesSupplier("getValue", () -> "recordType");
		DataAtomicSpy rulePartValue0 = new DataAtomicSpy();
		rulePartValue0.MRV.setDefaultReturnValuesSupplier("getValue", () -> "system.person");
		DataAtomicSpy rulePartValue1 = new DataAtomicSpy();
		rulePartValue1.MRV.setDefaultReturnValuesSupplier("getValue", () -> "system.place");

		DataGroupSpy rulePart = new DataGroupSpy();
		rulePart.MRV.setDefaultReturnValuesSupplier("getChildren",
				() -> List.of(rulePartValue0, rulePartValue1));
		rulePart.MRV.setSpecificReturnValuesSupplier("getAttribute", () -> actionAttribute, "type");
		return rulePart;
	}

	private DataRecordGroupSpy createInactiveRule(String id) {
		DataRecordGroupSpy rule = new DataRecordGroupSpy();
		rule.MRV.setDefaultReturnValuesSupplier("getFirstAtomicValueWithNameInData",
				() -> "inactive");
		recordStorage.MRV.setSpecificReturnValuesSupplier("read", () -> rule, "permissionRule", id);
		return rule;
	}

	@Test
	public void activeRoleGivesWithTwoInactiveRules() {
		DataRecordGroupSpy activeRole = createActiveRole();
		DataRecordLinkSpy ruleLink0 = createLink("someRuleId0");
		DataRecordLinkSpy ruleLink1 = createLink("someRuleId1");
		addRulesToRole(activeRole, ruleLink0, ruleLink1);
		createInactiveRule("someRuleId0");
		createInactiveRule("someRuleId1");

		List<Rule> rules = rulesProvider.getActiveRules(SOME_ROLE);

		recordStorage.MCR.assertNumberOfCallsToMethod("read", 3);
		recordStorage.MCR.assertParameters("read", 0, "permissionRole", SOME_ROLE);
		recordStorage.MCR.assertParameters("read", 1, "permissionRule", "someRuleId0");
		recordStorage.MCR.assertParameters("read", 2, "permissionRule", "someRuleId1");

		assertEquals(rules.size(), 0);
	}

	@Test
	public void inactiveRoleGivesWithTwoActiveRules() {
		DataRecordGroupSpy activeRole = createInactiveRole();
		DataRecordLinkSpy ruleLink0 = createLink("someRuleId0");
		DataRecordLinkSpy ruleLink1 = createLink("someRuleId1");
		addRulesToRole(activeRole, ruleLink0, ruleLink1);
		createActiveRule("someRuleId0");
		createActiveRule("someRuleId1");

		List<Rule> rules = rulesProvider.getActiveRules(SOME_ROLE);

		recordStorage.MCR.assertNumberOfCallsToMethod("read", 1);
		recordStorage.MCR.assertParameters("read", 0, "permissionRole", SOME_ROLE);
		assertEquals(rules.size(), 0);
	}

	@Test
	public void activeRoleGivesWithTwoActiveRules() {
		DataRecordGroupSpy activeRole = createActiveRole();
		DataRecordLinkSpy ruleLink0 = createLink("someRuleId0");
		DataRecordLinkSpy ruleLink1 = createLink("someRuleId1");
		addRulesToRole(activeRole, ruleLink0, ruleLink1);
		DataRecordGroupSpy activeRule0 = createActiveRule("someRuleId0");
		addRulePartToRule(activeRule0, createRulePartWithTwoActions());
		DataRecordGroupSpy activeRule1 = createActiveRule("someRuleId1");
		addRulePartToRule(activeRule1, createRulePartWithTwoActions());

		List<Rule> rules = rulesProvider.getActiveRules(SOME_ROLE);

		recordStorage.MCR.assertNumberOfCallsToMethod("read", 3);
		recordStorage.MCR.assertParameters("read", 0, "permissionRole", SOME_ROLE);
		recordStorage.MCR.assertParameters("read", 1, "permissionRule", "someRuleId0");
		recordStorage.MCR.assertParameters("read", 2, "permissionRule", "someRuleId1");

		assertEquals(rules.size(), 2);

		assertRules(rules.get(0));
		assertRules(rules.get(1));
	}

	private void assertRules(Rule rule) {
		RulePartValues actionRulePart = rule.getRulePartValuesForKey("action");
		assertEquals(actionRulePart.size(), 2);
		assertTrue(actionRulePart.contains("system.create"));
		assertTrue(actionRulePart.contains("system.read"));
	}

	private void addRulesToRole(DataRecordGroupSpy activeRole,
			DataRecordLinkSpy... permissionRuleLink) {
		activeRole.MRV.setSpecificReturnValuesSupplier("getChildrenOfTypeAndName",
				() -> Arrays.asList(permissionRuleLink), DataRecordLink.class,
				"permissionRuleLink");
	}

	private DataRecordLinkSpy createLink(String ruleId) {
		DataRecordLinkSpy permissionRuleLink = new DataRecordLinkSpy();
		permissionRuleLink.MRV.setDefaultReturnValuesSupplier("getLinkedRecordId", () -> ruleId);
		return permissionRuleLink;
	}

	@Test
	public void testWithPermissionTerms() {
		DataRecordGroupSpy activeRole = createActiveRole();
		DataRecordLinkSpy ruleLink0 = createLink("someRuleId0");
		DataRecordLinkSpy ruleLink1 = createLink("someRuleId1");
		addRulesToRole(activeRole, ruleLink0, ruleLink1);
		DataRecordGroupSpy someRuleId0 = createActiveRule("someRuleId0");
		addRulePartToRule(someRuleId0, createRulePartWithTwoActions(),
				createRulePartWithTwoRecordTypes());
		DataRecordGroupSpy someRuleId1 = createActiveRule("someRuleId1");
		addRulePartToRule(someRuleId1, createRulePartWithTwoActions());
		addPermissionTermRulePart(someRuleId1, createPermissionTermRulePart("PUBLISHED_STATUS",
				createPermissionValue("system.published")));

		List<Rule> rules = rulesProvider.getActiveRules(SOME_ROLE);

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

	private void addPermissionTermRulePart(DataRecordGroupSpy rule, DataGroupSpy... dataGroupSpy) {
		rule.MRV.setSpecificReturnValuesSupplier("getAllGroupsWithNameInData",
				() -> Arrays.asList(dataGroupSpy), "permissionTermRulePart");
	}

	private DataGroupSpy createPermissionTermRulePart(String permissionKey,
			DataAtomicSpy... values) {
		String id = createUniqueId();
		DataGroupSpy permissionTermRulePart = createPermissionRuleParts(id, values);
		createPermissionTermInStorage(id, permissionKey);
		return permissionTermRulePart;
	}

	private String createUniqueId() {
		return "someCollectTerm" + System.nanoTime();
	}

	private DataGroupSpy createPermissionRuleParts(String id, DataAtomicSpy... values) {
		DataGroupSpy permissionTermRulePart = new DataGroupSpy();
		permissionTermRulePart.MRV.setSpecificReturnValuesSupplier(
				"getAllDataAtomicsWithNameInData", () -> Arrays.asList(values), "value");

		DataRecordLinkSpy permissionRuleLink = createLink(id);
		permissionTermRulePart.MRV.setSpecificReturnValuesSupplier("getFirstChildOfTypeAndName",
				() -> permissionRuleLink, DataRecordLink.class, "rule");
		return permissionTermRulePart;
	}

	private DataAtomicSpy createPermissionValue(String value) {
		DataAtomicSpy value0 = new DataAtomicSpy();
		value0.MRV.setDefaultReturnValuesSupplier("getValue", () -> value);
		return value0;
	}

	private void createPermissionTermInStorage(String id, String permissionKey) {
		DataRecordGroupSpy permissionTerm = new DataRecordGroupSpy();
		DataGroupSpy extraData = new DataGroupSpy();
		permissionTerm.MRV.setSpecificReturnValuesSupplier("getFirstGroupWithNameInData",
				() -> extraData, "extraData");
		extraData.MRV.setSpecificReturnValuesSupplier("getFirstAtomicValueWithNameInData",
				() -> permissionKey, "permissionKey");

		recordStorage.MRV.setSpecificReturnValuesSupplier("read", () -> permissionTerm,
				"collectTerm", id);
	}

	@Test
	public void testWithMultiplePermissionTerms() {
		DataRecordGroupSpy activeRole = createActiveRole();
		DataRecordLinkSpy ruleLink0 = createLink("someRuleId0");
		DataRecordLinkSpy ruleLink1 = createLink("someRuleId1");
		DataRecordLinkSpy ruleLink2 = createLink("someRuleId2");
		addRulesToRole(activeRole, ruleLink0, ruleLink1, ruleLink2);

		DataRecordGroupSpy someRuleId0 = createActiveRule("someRuleId0");
		addRulePartToRule(someRuleId0, createRulePartWithTwoActions(),
				createRulePartWithTwoRecordTypes());

		DataRecordGroupSpy someRuleId1 = createActiveRule("someRuleId1");
		addRulePartToRule(someRuleId1, createRulePartWithTwoActions());
		addPermissionTermRulePart(someRuleId1, createPermissionTermRulePart("PUBLISHED_STATUS",
				createPermissionValue("system.published")));

		DataRecordGroupSpy someRuleId2 = createActiveRule("someRuleId2");
		addRulePartToRule(someRuleId2, createRulePartWithTwoActions());
		addPermissionTermRulePart(someRuleId2,
				createPermissionTermRulePart("PUBLISHED_STATUS",
						createPermissionValue("system.published"),
						createPermissionValue("system.notPublished")),
				createPermissionTermRulePart("DELETED_STATUS",
						createPermissionValue("system.deleted"),
						createPermissionValue("system.notDeleted")));

		List<Rule> rules = rulesProvider.getActiveRules(SOME_ROLE);
		assertEquals(rules.size(), 3);
		Rule rule0 = rules.get(0);
		RulePartValues actionRulePart0 = rule0.getRulePartValuesForKey("action");
		assertEquals(actionRulePart0.size(), 2);
		assertTrue(actionRulePart0.contains("system.create"));
		assertTrue(actionRulePart0.contains("system.read"));

		RulePartValues recordTypeRulePart0 = rule0.getRulePartValuesForKey("recordType");
		assertEquals(recordTypeRulePart0.size(), 2);
		assertTrue(recordTypeRulePart0.contains("system.person"));
		assertTrue(recordTypeRulePart0.contains("system.place"));

		Rule rule1 = rules.get(1);
		RulePartValues actionRulePart1 = rule1.getRulePartValuesForKey("action");
		assertEquals(actionRulePart1.size(), 2);
		assertTrue(actionRulePart1.contains("system.create"));
		assertTrue(actionRulePart1.contains("system.read"));

		RulePartValues permissionPublishedRulePart1 = rule1
				.getRulePartValuesForKey("PUBLISHED_STATUS");
		assertEquals(permissionPublishedRulePart1.size(), 1);
		assertTrue(permissionPublishedRulePart1.contains("system.published"));

		Rule rule2 = rules.get(2);
		RulePartValues actionRulePart2 = rule2.getRulePartValuesForKey("action");
		assertEquals(actionRulePart2.size(), 2);
		assertTrue(actionRulePart2.contains("system.create"));
		assertTrue(actionRulePart2.contains("system.read"));

		RulePartValues permissionPublishedRulePart2_a = rule2
				.getRulePartValuesForKey("DELETED_STATUS");
		assertEquals(permissionPublishedRulePart2_a.size(), 2);
		assertTrue(permissionPublishedRulePart2_a.contains("system.deleted"));
		assertTrue(permissionPublishedRulePart2_a.contains("system.notDeleted"));

		RulePartValues permissionPublishedRulePart2_b = rule2
				.getRulePartValuesForKey("PUBLISHED_STATUS");
		assertEquals(permissionPublishedRulePart2_b.size(), 2);
		assertTrue(permissionPublishedRulePart2_b.contains("system.published"));
		assertTrue(permissionPublishedRulePart2_b.contains("system.notPublished"));
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
