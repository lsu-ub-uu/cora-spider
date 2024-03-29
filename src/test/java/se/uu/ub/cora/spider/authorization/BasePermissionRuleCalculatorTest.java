/*
 * Copyright 2016, 2018 Uppsala University Library
 * Copyright 2018 Olov McKie
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

import static org.testng.Assert.assertEquals;

import java.util.ArrayList;
import java.util.List;

import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import se.uu.ub.cora.beefeater.authorization.Rule;
import se.uu.ub.cora.beefeater.authorization.RulePartValues;
import se.uu.ub.cora.data.collected.PermissionTerm;

public class BasePermissionRuleCalculatorTest {

	private PermissionRuleCalculator ruleCalculator;
	private String action = "create";
	private String recordType = "book";
	private List<PermissionTerm> permissionTerms;

	@BeforeMethod
	public void setUp() {
		ruleCalculator = new BasePermissionRuleCalculator();
		permissionTerms = new ArrayList<>();
	}

	@Test
	public void testWithoutData() {
		List<Rule> requiredRules = ruleCalculator.calculateRulesForActionAndRecordType(action,
				recordType);

		assertEquals(requiredRules.size(), 1);

		Rule requiredRule = requiredRules.get(0);
		assertEquals(requiredRule.keySet().size(), 2);
		assertCorrectActionAndRecordType(requiredRule);
	}

	@Test
	public void testWithCollectedDataNoPermissions() {
		List<Rule> requiredRules = ruleCalculator
				.calculateRulesForActionAndRecordTypeAndCollectedData(action, recordType,
						permissionTerms);

		assertEquals(requiredRules.size(), 1);

		Rule requiredRule = requiredRules.get(0);
		assertEquals(requiredRule.keySet().size(), 2);
		assertCorrectActionAndRecordType(requiredRule);
	}

	@Test
	public void testWithCollectedDataOnePermissions() {
		createPermissionTerm("someTermId", "someTermValue", "SOME_PERMISSION_KEY");

		List<Rule> requiredRules = ruleCalculator
				.calculateRulesForActionAndRecordTypeAndCollectedData(action, recordType,
						permissionTerms);

		assertEquals(requiredRules.size(), 1);
		Rule requiredRule = requiredRules.get(0);
		assertEquals(requiredRule.keySet().size(), 3);
		assertCorrectActionAndRecordType(requiredRule);
		assertCorrectRulePartForRuleAndKeyAndValue(requiredRule, "SOME_PERMISSION_KEY",
				"someTermValue");
	}

	@Test
	public void testWithCollectedDataTwoPermissions() {
		createPermissionTerm("someTermId", "someTermValue", "SOME_PERMISSION_KEY");
		createPermissionTerm("otherTermId", "otherTermValue", "OTHER_PERMISSION_KEY");

		List<Rule> requiredRules = ruleCalculator
				.calculateRulesForActionAndRecordTypeAndCollectedData(action, recordType,
						permissionTerms);

		assertEquals(requiredRules.size(), 1);

		Rule requiredRule = requiredRules.get(0);
		assertEquals(requiredRule.keySet().size(), 4);
		assertCorrectActionAndRecordType(requiredRule);
		assertCorrectRulePartForRuleAndKeyAndValue(requiredRule, "SOME_PERMISSION_KEY",
				"someTermValue");
		assertCorrectRulePartForRuleAndKeyAndValue(requiredRule, "OTHER_PERMISSION_KEY",
				"otherTermValue");
	}

	@Test
	public void testWithCollectedDataOnePermissionsTwice() {
		createPermissionTerm("someTermId", "someTermValue", "SOME_PERMISSION_KEY");
		createPermissionTerm("someTermId", "someTermValue2", "SOME_PERMISSION_KEY");

		List<Rule> requiredRules = ruleCalculator
				.calculateRulesForActionAndRecordTypeAndCollectedData(action, recordType,
						permissionTerms);

		assertEquals(requiredRules.size(), 2);

		Rule requiredRule = requiredRules.get(0);
		assertEquals(requiredRule.keySet().size(), 3);
		assertCorrectActionAndRecordType(requiredRule);
		assertCorrectRulePartForRuleAndKeyAndValue(requiredRule, "SOME_PERMISSION_KEY",
				"someTermValue");

		Rule requiredRule2 = requiredRules.get(1);
		assertEquals(requiredRule2.keySet().size(), 3);
		assertCorrectActionAndRecordType(requiredRule2);
		assertCorrectRulePartForRuleAndKeyAndValue(requiredRule2, "SOME_PERMISSION_KEY",
				"someTermValue2");
	}

	@Test
	public void testWithCollectedDataTwoPermissionsRepeated() {
		createPermissionTerm("someTermId", "someTermValue", "SOME_PERMISSION_KEY");
		createPermissionTerm("someTermId", "someTermValue2", "SOME_PERMISSION_KEY");
		createPermissionTerm("otherTermId", "otherTermValue", "OTHER_PERMISSION_KEY");
		createPermissionTerm("otherTermId", "otherTermValue2", "OTHER_PERMISSION_KEY");

		List<Rule> requiredRules = ruleCalculator
				.calculateRulesForActionAndRecordTypeAndCollectedData(action, recordType,
						permissionTerms);

		assertEquals(requiredRules.size(), 4);

		Rule requiredRule = requiredRules.get(0);
		assertEquals(requiredRule.keySet().size(), 4);
		assertCorrectActionAndRecordType(requiredRule);
		assertCorrectRulePartForRuleAndKeyAndValue(requiredRule, "SOME_PERMISSION_KEY",
				"someTermValue");
		assertCorrectRulePartForRuleAndKeyAndValue(requiredRule, "OTHER_PERMISSION_KEY",
				"otherTermValue");

		Rule requiredRule2 = requiredRules.get(1);
		assertEquals(requiredRule2.keySet().size(), 4);
		assertCorrectActionAndRecordType(requiredRule2);
		assertCorrectRulePartForRuleAndKeyAndValue(requiredRule2, "SOME_PERMISSION_KEY",
				"someTermValue2");
		assertCorrectRulePartForRuleAndKeyAndValue(requiredRule2, "OTHER_PERMISSION_KEY",
				"otherTermValue");

		Rule requiredRule3 = requiredRules.get(2);
		assertEquals(requiredRule3.keySet().size(), 4);
		assertCorrectActionAndRecordType(requiredRule3);
		assertCorrectRulePartForRuleAndKeyAndValue(requiredRule3, "SOME_PERMISSION_KEY",
				"someTermValue");
		assertCorrectRulePartForRuleAndKeyAndValue(requiredRule3, "OTHER_PERMISSION_KEY",
				"otherTermValue2");

		Rule requiredRule4 = requiredRules.get(3);
		assertEquals(requiredRule4.keySet().size(), 4);
		assertCorrectActionAndRecordType(requiredRule4);
		assertCorrectRulePartForRuleAndKeyAndValue(requiredRule4, "SOME_PERMISSION_KEY",
				"someTermValue2");
		assertCorrectRulePartForRuleAndKeyAndValue(requiredRule4, "OTHER_PERMISSION_KEY",
				"otherTermValue2");
	}

	private void createPermissionTerm(String id, String value, String permissionKey) {
		PermissionTerm permissionTerm = new PermissionTerm(id, value, permissionKey);
		permissionTerms.add(permissionTerm);
	}

	private void assertCorrectRulePartForRuleAndKeyAndValue(Rule requiredRule, String key,
			String value) {
		RulePartValues rulePartValues = requiredRule.getRulePartValuesForKey(key);
		assertEquals(rulePartValues.size(), 1);
		assertEquals(rulePartValues.iterator().next(), "system." + value);
	}

	private void assertCorrectActionAndRecordType(Rule requiredRule) {
		RulePartValues actionValues = requiredRule.getRulePartValuesForKey("action");
		assertEquals(actionValues.size(), 1);
		assertEquals(actionValues.iterator().next(), "system." + action);

		RulePartValues recordTypeValues = requiredRule.getRulePartValuesForKey("recordType");
		assertEquals(recordTypeValues.size(), 1);
		assertEquals(recordTypeValues.iterator().next(), "system." + recordType);
	}
}
