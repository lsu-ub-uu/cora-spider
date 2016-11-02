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

import static org.testng.Assert.assertEquals;

import java.util.List;
import java.util.Map;
import java.util.Set;

import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import se.uu.ub.cora.bookkeeper.data.DataGroup;
import se.uu.ub.cora.spider.testdata.DataCreator;

public class BasePermissionRuleCalculatorTest {

	private PermissionRuleCalculator ruleCalculator;

	@BeforeMethod
	public void setUp() {
		ruleCalculator = new BasePermissionRuleCalculator();
	}

	@Test
	public void testWithData() {
		String action = "create";
		String recordType = "book";
		DataGroup record = DataCreator
				.createRecordWithNameInDataAndIdAndTypeAndLinkedRecordIdAndCreatedBy("book",
						"myBook", "book", "systemOne", "12345")
				.toDataGroup();
		List<Map<String, Set<String>>> requiredRules = ruleCalculator
				.calculateRulesForActionAndRecordTypeAndData(action, recordType, record);
		assertEquals(requiredRules.size(), 1);

		Map<String, Set<String>> requiredRule = requiredRules.get(0);
		Set<String> actionValues = requiredRule.get("action");
		assertEquals(actionValues.size(), 1);
		assertEquals(actionValues.iterator().next(), "system.create");

		Set<String> recordTypeValues = requiredRule.get("recordType");
		assertEquals(recordTypeValues.size(), 1);
		assertEquals(recordTypeValues.iterator().next(), "system.book");

		Set<String> createdByValues = requiredRule.get("createdBy");
		assertEquals(createdByValues.size(), 1);
		assertEquals(createdByValues.iterator().next(), "system.12345");
	}

	@Test
	public void testWithoutData() {
		String action = "create";
		String recordType = "book";
		List<Map<String, Set<String>>> requiredRules = ruleCalculator
				.calculateRulesForActionAndRecordType(action, recordType);
		assertEquals(requiredRules.size(), 1);

		Map<String, Set<String>> requiredRule = requiredRules.get(0);
		Set<String> actionValues = requiredRule.get("action");
		assertEquals(actionValues.size(), 1);
		assertEquals(actionValues.iterator().next(), "system.create");

		Set<String> recordTypeValues = requiredRule.get("recordType");
		assertEquals(recordTypeValues.size(), 1);
		assertEquals(recordTypeValues.iterator().next(), "system.book");
	}
}
