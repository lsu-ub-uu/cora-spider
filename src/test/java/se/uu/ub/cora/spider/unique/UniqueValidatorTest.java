/*
 * Copyright 2024 Uppsala University Library
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
package se.uu.ub.cora.spider.unique;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertTrue;

import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import se.uu.ub.cora.bookkeeper.validator.ValidationAnswer;
import se.uu.ub.cora.data.collected.StorageTerm;
import se.uu.ub.cora.storage.Condition;
import se.uu.ub.cora.storage.Filter;
import se.uu.ub.cora.storage.Part;
import se.uu.ub.cora.storage.RelationalOperator;
import se.uu.ub.cora.storage.spies.RecordStorageSpy;

public class UniqueValidatorTest {

	private static final String SOME_RECORD_TYPE = "someRecordType";
	private UniqueValidator uniqueValidator;
	Set<StorageTerm> storageTerms = new LinkedHashSet<>();
	Set<Unique> uniqueDefinitions = new LinkedHashSet<>();
	private RecordStorageSpy recordStorage;
	private Unique uniqueWithoutCombineTerms;
	private StorageTerm someStorageTerm = new StorageTerm("someStorageTermId",
			"someUniqueTermStorageKey", "someTermValue");

	@BeforeMethod
	private void beforeMethod() {
		recordStorage = new RecordStorageSpy();
		uniqueValidator = UniqueValidatorImp.usingRecordStorage(recordStorage);

		uniqueWithoutCombineTerms = new Unique("someUniqueTermStorageKey", Collections.emptySet());
	}

	@Test
	public void testValidateUnique_whenNoUniqueIsSetInRecordType() throws Exception {
		ValidationAnswer answer = uniqueValidator.validateUnique(SOME_RECORD_TYPE,
				Collections.emptySet(), Collections.emptySet());

		assertDataIsValid(answer);
		assertReadListInStorageNotCalled();
	}

	private void assertDataIsValid(ValidationAnswer answer) {
		assertTrue(answer.dataIsValid());
	}

	private void assertReadListInStorageNotCalled() {
		recordStorage.MCR.assertMethodNotCalled("readList");
	}

	@Test
	public void testOneUniqueRuleExist_HasDefinitionButNoIncomingData_IsUnique() throws Exception {
		ValidationAnswer answer = uniqueValidator.validateUnique(SOME_RECORD_TYPE,
				Set.of(uniqueWithoutCombineTerms), Collections.emptySet());

		assertDataIsValid(answer);
		assertReadListInStorageNotCalled();
	}

	@Test
	public void testOneUniqueRuleExist_HasNoDefinitionButIncomingData_IsUnique() throws Exception {
		ValidationAnswer answer = uniqueValidator.validateUnique(SOME_RECORD_TYPE,
				Collections.emptySet(), Set.of(someStorageTerm));

		assertDataIsValid(answer);
		assertReadListInStorageNotCalled();
	}

	@Test
	public void testOneUniqueRuleExist_HasDefinitionAndIncomingData_IsUnique() throws Exception {
		ValidationAnswer answer = uniqueValidator.validateUnique(SOME_RECORD_TYPE,
				Set.of(uniqueWithoutCombineTerms), Set.of(someStorageTerm));

		assertDataIsValid(answer);
		assertNumberOfCallsToReadList(1);
		assertCallToReadListWithRecordType(SOME_RECORD_TYPE);

		int numberOfConditions = 1;
		// String conditionKey = "someUniqueTermStorageKey";
		String conditionKey = uniqueWithoutCombineTerms.uniqueTermStorageKey();
		String conditionValue = someStorageTerm.value();
		int readListNumber = 0;

		// assertFilterForCall(0,{conditionKey, conditionValue}...)
		AssertCondition assertCondition = new AssertCondition(conditionKey, conditionValue);

		assertFilter(readListNumber, numberOfConditions, List.of(assertCondition));
	}

	private void assertFilter(int readListNumber, int numberOfConditions,
			List<AssertCondition> assertConditions) {
		Filter usedFilter = (Filter) recordStorage.MCR
				.getValueForMethodNameAndCallNumberAndParameterName("readList", readListNumber,
						"filter");
		assertEquals(usedFilter.include.size(), 1);
		assertEquals(usedFilter.exclude.size(), readListNumber);

		assertEquals(usedFilter.include.size(), 1);
		Part includePart = usedFilter.include.get(0);
		assertEquals(includePart.conditions.size(), assertConditions.size());

		int conditionNumber = 0;
		for (Condition condition : includePart.conditions) {
			assertCondition(assertConditions.get(conditionNumber).conditionKey(),
					assertConditions.get(conditionNumber).conditionValue(), condition,
					conditionNumber);
		}
	}

	private void assertCondition(String conditionKey, String conditionValue, Condition condition,
			int conditionNumber) {
		assertEquals(condition.key(), conditionKey);
		assertEquals(condition.operator(), RelationalOperator.EQUAL_TO);
		assertEquals(condition.value(), conditionValue);
	}

	private void assertCallToReadListWithRecordType(String recordType) {
		recordStorage.MCR.assertParameterAsEqual("readList", 0, "types", List.of(recordType));
	}

	private void assertNumberOfCallsToReadList(int calledNumberOfTimes) {
		recordStorage.MCR.assertNumberOfCallsToMethod("readList", calledNumberOfTimes);
	}

	record AssertCondition(String conditionKey, String conditionValue) {
	}

}