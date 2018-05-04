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

package se.uu.ub.cora.spider.authorization;

import static org.testng.Assert.assertEquals;

import java.util.List;

import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import se.uu.ub.cora.beefeater.authorization.Rule;
import se.uu.ub.cora.beefeater.authorization.RulePartValues;
import se.uu.ub.cora.bookkeeper.data.DataAtomic;
import se.uu.ub.cora.bookkeeper.data.DataGroup;

public class BasePermissionRuleCalculatorTest {

	private PermissionRuleCalculator ruleCalculator;
	private String action = "create";
	private String recordType = "book";

	@BeforeMethod
	public void setUp() {
		ruleCalculator = new BasePermissionRuleCalculator();
	}

	@Test
	public void testWithoutData() {
		List<Rule> requiredRules = ruleCalculator.calculateRulesForActionAndRecordType(action,
				recordType);
		assertEquals(requiredRules.size(), 1);

		Rule requiredRule = requiredRules.get(0);
		assertEquals(requiredRule.keySet().size(), 2);
		RulePartValues actionValues = requiredRule.get("action");
		assertEquals(actionValues.size(), 1);
		assertEquals(actionValues.iterator().next(), "system.create");

		RulePartValues recordTypeValues = requiredRule.get("recordType");
		assertEquals(recordTypeValues.size(), 1);
		assertEquals(recordTypeValues.iterator().next(), "system.book");
	}

	@Test
	public void testWithCollectedDataNoPermissions() {
		DataGroup collectedData = DataGroup.withNameInData("collectedData");

		List<Rule> requiredRules = ruleCalculator
				.calculateRulesForActionAndRecordTypeAndCollectedData(action, recordType,
						collectedData);
		assertEquals(requiredRules.size(), 1);

		Rule requiredRule = requiredRules.get(0);
		assertEquals(requiredRule.keySet().size(), 2);
		RulePartValues actionValues = requiredRule.get("action");
		assertEquals(actionValues.size(), 1);
		assertEquals(actionValues.iterator().next(), "system.create");

		RulePartValues recordTypeValues = requiredRule.get("recordType");
		assertEquals(recordTypeValues.size(), 1);
		assertEquals(recordTypeValues.iterator().next(), "system.book");
	}

	@Test
	public void testWithCollectedDataOnePermissions() {
		DataGroup collectedData = DataGroup.withNameInData("collectedData");
		DataGroup permission = DataGroup.withNameInData("permission");
		collectedData.addChild(permission);

		DataGroup collectedDataTerm = createCollectedDataTermUsingRepeatIdAndTermIdAndTermValueAndPermissionKey(
				"0", "someTermId", "someTermValue", "SOME_PERMISSION_KEY");

		permission.addChild(collectedDataTerm);

		List<Rule> requiredRules = ruleCalculator
				.calculateRulesForActionAndRecordTypeAndCollectedData(action, recordType,
						collectedData);
		assertEquals(requiredRules.size(), 1);

		Rule requiredRule = requiredRules.get(0);
		assertEquals(requiredRule.keySet().size(), 3);
		RulePartValues actionValues = requiredRule.get("action");
		assertEquals(actionValues.size(), 1);
		assertEquals(actionValues.iterator().next(), "system.create");

		RulePartValues recordTypeValues = requiredRule.get("recordType");
		assertEquals(recordTypeValues.size(), 1);
		assertEquals(recordTypeValues.iterator().next(), "system.book");

		RulePartValues createdByValues = requiredRule.get("SOME_PERMISSION_KEY");
		assertEquals(createdByValues.size(), 1);
		assertEquals(createdByValues.iterator().next(), "system.someTermValue");
	}

	private DataGroup createCollectedDataTermUsingRepeatIdAndTermIdAndTermValueAndPermissionKey(
			String repeatId, String termId, String termValue, String permissionKey) {
		DataGroup collectedDataTerm = DataGroup.withNameInData("collectedDataTerm");
		collectedDataTerm.setRepeatId(repeatId);
		collectedDataTerm.addChild(DataAtomic.withNameInDataAndValue("collectTermId", termId));
		collectedDataTerm
				.addChild(DataAtomic.withNameInDataAndValue("collectTermValue", termValue));

		DataGroup extraData = DataGroup.withNameInData("extraData");
		collectedDataTerm.addChild(extraData);
		extraData.addChild(DataAtomic.withNameInDataAndValue("permissionKey", permissionKey));
		return collectedDataTerm;
	}

	@Test
	public void testWithCollectedDataTwoPermissions() {
		DataGroup collectedData = DataGroup.withNameInData("collectedData");
		DataGroup permission = DataGroup.withNameInData("permission");
		collectedData.addChild(permission);

		DataGroup collectedDataTerm = createCollectedDataTermUsingRepeatIdAndTermIdAndTermValueAndPermissionKey(
				"0", "someTermId", "someTermValue", "SOME_PERMISSION_KEY");
		permission.addChild(collectedDataTerm);

		DataGroup collectedDataTerm2 = createCollectedDataTermUsingRepeatIdAndTermIdAndTermValueAndPermissionKey(
				"1", "otherTermId", "otherTermValue", "OTHER_PERMISSION_KEY");
		permission.addChild(collectedDataTerm2);

		List<Rule> requiredRules = ruleCalculator
				.calculateRulesForActionAndRecordTypeAndCollectedData(action, recordType,
						collectedData);
		assertEquals(requiredRules.size(), 1);

		Rule requiredRule = requiredRules.get(0);
		assertEquals(requiredRule.keySet().size(), 4);
		RulePartValues actionValues = requiredRule.get("action");
		assertEquals(actionValues.size(), 1);
		assertEquals(actionValues.iterator().next(), "system.create");

		RulePartValues recordTypeValues = requiredRule.get("recordType");
		assertEquals(recordTypeValues.size(), 1);
		assertEquals(recordTypeValues.iterator().next(), "system.book");

		RulePartValues createdByValues = requiredRule.get("SOME_PERMISSION_KEY");
		assertEquals(createdByValues.size(), 1);
		assertEquals(createdByValues.iterator().next(), "system.someTermValue");

		RulePartValues createdByValues2 = requiredRule.get("OTHER_PERMISSION_KEY");
		assertEquals(createdByValues2.size(), 1);
		assertEquals(createdByValues2.iterator().next(), "system.otherTermValue");
	}

	@Test
	public void testWithCollectedDataTwoPermissionsRepeated() {
		DataGroup collectedData = DataGroup.withNameInData("collectedData");
		DataGroup permission = DataGroup.withNameInData("permission");
		collectedData.addChild(permission);

		DataGroup collectedDataTerm = createCollectedDataTermUsingRepeatIdAndTermIdAndTermValueAndPermissionKey(
				"0", "someTermId", "someTermValue", "SOME_PERMISSION_KEY");
		permission.addChild(collectedDataTerm);
		DataGroup collectedDataTerm1 = createCollectedDataTermUsingRepeatIdAndTermIdAndTermValueAndPermissionKey(
				"1", "someTermId", "someTermValue1", "SOME_PERMISSION_KEY");
		permission.addChild(collectedDataTerm1);

		DataGroup collectedDataTerm2 = createCollectedDataTermUsingRepeatIdAndTermIdAndTermValueAndPermissionKey(
				"1", "otherTermId", "otherTermValue", "OTHER_PERMISSION_KEY");
		permission.addChild(collectedDataTerm2);

		DataGroup collectedDataTerm3 = createCollectedDataTermUsingRepeatIdAndTermIdAndTermValueAndPermissionKey(
				"2", "otherTermId", "otherTermValue2", "OTHER_PERMISSION_KEY");
		permission.addChild(collectedDataTerm3);

		List<Rule> requiredRules = ruleCalculator
				.calculateRulesForActionAndRecordTypeAndCollectedData(action, recordType,
						collectedData);
		assertEquals(requiredRules.size(), 4);

		// for rules 0-3
		Rule requiredRule = requiredRules.get(0);
		assertEquals(requiredRule.keySet().size(), 4);
		RulePartValues actionValues = requiredRule.get("action");
		assertEquals(actionValues.size(), 1);
		assertEquals(actionValues.iterator().next(), "system.create");

		RulePartValues recordTypeValues = requiredRule.get("recordType");
		assertEquals(recordTypeValues.size(), 1);
		assertEquals(recordTypeValues.iterator().next(), "system.book");

		RulePartValues createdByValues = requiredRule.get("SOME_PERMISSION_KEY");
		assertEquals(createdByValues.size(), 1);
		assertEquals(createdByValues.iterator().next(), "system.someTermValue");
		// assertEquals(createdByValues.iterator().next(), "system.someTermValue1");

		RulePartValues createdByValues2 = requiredRule.get("OTHER_PERMISSION_KEY");
		assertEquals(createdByValues2.size(), 1);
		assertEquals(createdByValues2.iterator().next(), "system.otherTermValue");
		// assertEquals(createdByValues2.iterator().next(), "system.otherTermValue2");
	}

}
