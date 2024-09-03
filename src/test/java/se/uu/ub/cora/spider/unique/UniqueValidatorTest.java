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
import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertSame;
import static org.testng.Assert.assertTrue;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.testng.annotations.BeforeMethod;
import org.testng.annotations.BeforeTest;
import org.testng.annotations.Test;

import se.uu.ub.cora.bookkeeper.recordtype.Unique;
import se.uu.ub.cora.bookkeeper.validator.ValidationAnswer;
import se.uu.ub.cora.data.collected.StorageTerm;
import se.uu.ub.cora.storage.Condition;
import se.uu.ub.cora.storage.Filter;
import se.uu.ub.cora.storage.Part;
import se.uu.ub.cora.storage.RecordStorage;
import se.uu.ub.cora.storage.RelationalOperator;
import se.uu.ub.cora.storage.StorageReadResult;
import se.uu.ub.cora.storage.spies.RecordStorageSpy;

public class UniqueValidatorTest {

	private static final String SOME_RECORD_TYPE = "someRecordType";
	private UniqueValidator uniqueValidator;
	private RecordStorageSpy recordStorage;
	private StorageReadResult duplicateReadResult = new StorageReadResult();
	private Set<StorageTerm> storageTerms;
	private Set<Unique> uniqueDefinitions;
	private Map<Integer, List<ConditionPair>> expectedConditionsMap;

	@BeforeTest
	public void BeforeTest() {
		duplicateReadResult.totalNumberOfMatches = 1;
	}

	@BeforeMethod
	private void beforeMethod() {
		recordStorage = new RecordStorageSpy();
		uniqueValidator = UniqueValidatorImp.usingRecordStorage(recordStorage);
		uniqueDefinitions = new LinkedHashSet<>();
		storageTerms = new LinkedHashSet<>();
		expectedConditionsMap = new HashMap<>();
	}

	@Test
	public void testValidateUnique_whenNoUniqueIsSetInRecordType() throws Exception {
		ValidationAnswer answer = uniqueValidator.validateUnique(SOME_RECORD_TYPE,
				uniqueDefinitions, storageTerms);

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
	public void oneUniqueRule_NoCollectedData_IsValid() throws Exception {
		addUniqueRule("keyA");

		ValidationAnswer answer = uniqueValidator.validateUnique(SOME_RECORD_TYPE,
				uniqueDefinitions, storageTerms);

		assertDataIsValid(answer);
		assertReadListInStorageNotCalled();
	}

	@Test
	public void NoUniqueRule_HasCollectedData_IsValid() throws Exception {
		addStorageTerm("keyA", "valueA");

		ValidationAnswer answer = uniqueValidator.validateUnique(SOME_RECORD_TYPE,
				uniqueDefinitions, storageTerms);

		assertDataIsValid(answer);
		assertReadListInStorageNotCalled();
	}

	@Test
	public void oneUniqueRule_HasCollectedData_IsValid() throws Exception {
		addUniqueRule("keyA");
		addStorageTerm("keyA", "valueA");

		ValidationAnswer answer = uniqueValidator.validateUnique(SOME_RECORD_TYPE,
				uniqueDefinitions, storageTerms);

		assertDataIsValid(answer);
		assertNumberOfCallsToReadList(1);
		assertCallToReadListWithRecordType(SOME_RECORD_TYPE);

		addConditionPairForRule(0, "keyA", "valueA");
		assertFilterForReadList(0);
	}

	@Test
	public void oneUniqueRule_HasCollectedData_IsNotValid() throws Exception {
		recordStorage.MRV.setDefaultReturnValuesSupplier("readList", () -> duplicateReadResult);
		addUniqueRule("keyA");
		addStorageTerm("keyA", "valueA");

		ValidationAnswer answer = uniqueValidator.validateUnique(SOME_RECORD_TYPE,
				uniqueDefinitions, storageTerms);
		assertDataIsInvalid(answer, List.of(
				"A record matching the unique rule with [key: keyA, value: valueA] already exists "
						+ "in the system"));
		assertNumberOfCallsToReadList(1);
		assertCallToReadListWithRecordType(SOME_RECORD_TYPE);

		addConditionPairForRule(0, "keyA", "valueA");
		assertFilterForReadList(0);
	}

	@Test
	public void oneUniqueRuleWithCombine_HasCollectedData_IsNotValid() throws Exception {
		recordStorage.MRV.setDefaultReturnValuesSupplier("readList", () -> duplicateReadResult);
		addUniqueRule("keyA", "keyB", "keyC");
		addStorageTerm("keyA", "valueA");
		addStorageTerm("keyB", "valueB");
		addStorageTerm("keyC", "valueC");

		ValidationAnswer answer = uniqueValidator.validateUnique(SOME_RECORD_TYPE,
				uniqueDefinitions, storageTerms);
		assertDataIsInvalid(answer,
				List.of("A record matching the unique rule with [key: keyA, value: valueA], "
						+ "[key: keyB, value: valueB], [key: keyC, value: valueC] "
						+ "already exists in the system"));
		assertNumberOfCallsToReadList(1);
		assertCallToReadListWithRecordType(SOME_RECORD_TYPE);

		addConditionPairForRule(0, "keyA", "valueA");
		addConditionPairForRule(0, "keyB", "valueB");
		addConditionPairForRule(0, "keyC", "valueC");
		assertFilterForReadList(0);
	}

	@Test
	public void oneUniqueRule_HasNoMatchingCollectedData_IsValid() throws Exception {
		addUniqueRule("keyA");
		addStorageTerm("keyB", "valueB");

		ValidationAnswer answer = uniqueValidator.validateUnique(SOME_RECORD_TYPE,
				uniqueDefinitions, storageTerms);

		assertDataIsValid(answer);
		assertNumberOfCallsToReadList(0);
	}

	@Test
	public void oneUniqueRule_HasOneMatchingCollectedData_IsValid() throws Exception {
		addUniqueRule("keyA");

		addStorageTerm("keyB", "valueB");
		addStorageTerm("keyA", "valueA");

		ValidationAnswer answer = uniqueValidator.validateUnique(SOME_RECORD_TYPE,
				uniqueDefinitions, storageTerms);

		assertDataIsValid(answer);
		assertNumberOfCallsToReadList(1);
		assertCallToReadListWithRecordType(SOME_RECORD_TYPE);

		addConditionPairForRule(0, "keyA", "valueA");
		assertFilterForReadList(0);
	}

	@Test
	public void oneUniqueRuleWithOneCombine_AllMatchingCollectedData_IsValid() throws Exception {
		addUniqueRule("keyA", "keyB");

		addStorageTerm("keyB", "valueB");
		addStorageTerm("keyA", "valueA");

		ValidationAnswer answer = uniqueValidator.validateUnique(SOME_RECORD_TYPE,
				uniqueDefinitions, storageTerms);

		assertDataIsValid(answer);
		assertNumberOfCallsToReadList(1);
		assertCallToReadListWithRecordType(SOME_RECORD_TYPE);

		addConditionPairForRule(0, "keyA", "valueA");
		addConditionPairForRule(0, "keyB", "valueB");
		assertFilterForReadList(0);
	}

	@Test
	public void oneUniqueRuleWithOneCombine_OnlyUniqueTermMatchingCollectedData_IsValid()
			throws Exception {
		addUniqueRule("keyA", "keyB");

		addStorageTerm("keyA", "valueA");
		addStorageTerm("keyC", "valueC");

		ValidationAnswer answer = uniqueValidator.validateUnique(SOME_RECORD_TYPE,
				uniqueDefinitions, storageTerms);

		assertDataIsValid(answer);
		assertNumberOfCallsToReadList(1);
		assertCallToReadListWithRecordType(SOME_RECORD_TYPE);

		addConditionPairForRule(0, "keyA", "valueA");
		assertFilterForReadList(0);
	}

	@Test
	public void oneUniqueRuleWithTwoCombine_UniqueTermAndOneCombineTermMatchingCollectedData_IsValid()
			throws Exception {
		addUniqueRule("keyA", "keyB", "keyC");

		addStorageTerm("keyA", "valueA");
		addStorageTerm("keyC", "valueC");

		ValidationAnswer answer = uniqueValidator.validateUnique(SOME_RECORD_TYPE,
				uniqueDefinitions, storageTerms);

		assertDataIsValid(answer);
		assertNumberOfCallsToReadList(1);
		assertCallToReadListWithRecordType(SOME_RECORD_TYPE);

		addConditionPairForRule(0, "keyA", "valueA");
		addConditionPairForRule(0, "keyC", "valueC");
		assertFilterForReadList(0);
	}

	@Test
	public void twoUniqueRuleWithTwoCombine_AllMatchingCollectedData_IsValid() throws Exception {
		addUniqueRule("keyA", "keyB");
		addUniqueRule("keyC");

		addStorageTerm("keyA", "valueA");
		addStorageTerm("keyB", "valueB");
		addStorageTerm("keyC", "valueC");

		ValidationAnswer answer = uniqueValidator.validateUnique(SOME_RECORD_TYPE,
				uniqueDefinitions, storageTerms);

		assertDataIsValid(answer);
		assertNumberOfCallsToReadList(2);
		assertCallToReadListWithRecordType(SOME_RECORD_TYPE);

		addConditionPairForRule(0, "keyA", "valueA");
		addConditionPairForRule(0, "keyB", "valueB");
		assertFilterForReadList(0);

		addConditionPairForRule(1, "keyC", "valueC");
		assertFilterForReadList(1);
	}

	@Test
	public void manyUniqueRuleWithTwoCombine_ManyMatchingCollectedData_IsValid() throws Exception {
		addUniqueRule("keyA", "keyB");
		addUniqueRule("keyD", "keyE", "keyF");
		addUniqueRule("keyC");
		addUniqueRule("keyB", "keyZ", "keyF");
		addUniqueRule("keyA", "keyF", "keyE");

		addStorageTerm("keyA", "valueA");
		addStorageTerm("keyB", "valueB");
		addStorageTerm("keyD", "valueD");
		addStorageTerm("keyE", "valueE");
		addStorageTerm("keyF", "valueF");
		addStorageTerm("keyK", "valueK");

		recordStorage.MRV.setDefaultReturnValuesSupplier("readList", () -> duplicateReadResult);

		ValidationAnswer answer = uniqueValidator.validateUnique(SOME_RECORD_TYPE,
				uniqueDefinitions, storageTerms);

		String error1 = "A record matching the unique rule with [key: keyA, value: valueA], "
				+ "[key: keyB, value: valueB] already exists in the system";
		String error2 = "A record matching the unique rule with [key: keyD, value: valueD], "
				+ "[key: keyE, value: valueE], [key: keyF, value: valueF] already exists in the system";
		String error3 = "A record matching the unique rule with [key: keyB, value: valueB], "
				+ "[key: keyF, value: valueF] already exists in the system";
		String error4 = "A record matching the unique rule with [key: keyA, value: valueA], "
				+ "[key: keyF, value: valueF], [key: keyE, value: valueE] already exists in the system";
		assertDataIsInvalid(answer, List.of(error1, error2, error3, error4));
		assertNumberOfCallsToReadList(4);
		assertCallToReadListWithRecordType(SOME_RECORD_TYPE);

		addConditionPairForRule(0, "keyA", "valueA");
		addConditionPairForRule(0, "keyB", "valueB");
		assertFilterForReadList(0);

		addConditionPairForRule(1, "keyD", "valueD");
		addConditionPairForRule(1, "keyE", "valueE");
		addConditionPairForRule(1, "keyF", "valueF");
		assertFilterForReadList(1);

		addConditionPairForRule(2, "keyB", "valueB");
		addConditionPairForRule(2, "keyF", "valueF");
		assertFilterForReadList(2);

		addConditionPairForRule(3, "keyA", "valueA");
		addConditionPairForRule(3, "keyF", "valueF");
		addConditionPairForRule(3, "keyE", "valueE");
		assertFilterForReadList(3);
	}

	@Test
	public void manyUniqueRuleWithTwoCombine_ManyMatchingCollectedData_IsValid_2()
			throws Exception {
		addUniqueRule("keyA", "keyB");
		addUniqueRule("keyD", "keyE", "keyF");
		addUniqueRule("keyC");
		addUniqueRule("keyB", "keyZ", "keyF");
		addUniqueRule("keyA", "keyF", "keyE");

		addStorageTerm("keyA", "valueA");
		addStorageTerm("keyB", "valueB");
		addStorageTerm("keyD", "valueD");
		addStorageTerm("keyE", "valueE");
		addStorageTerm("keyF", "valueF");
		addStorageTerm("keyK", "valueK");

		ValidationAnswer answer = uniqueValidator.validateUnique(SOME_RECORD_TYPE,
				uniqueDefinitions, storageTerms);

		assertDataIsValid(answer);
		assertNumberOfCallsToReadList(4);
		assertCallToReadListWithRecordType(SOME_RECORD_TYPE);

		addConditionPairForRule(0, "keyA", "valueA");
		addConditionPairForRule(0, "keyB", "valueB");
		assertFilterForReadList(0);

		addConditionPairForRule(1, "keyD", "valueD");
		addConditionPairForRule(1, "keyE", "valueE");
		addConditionPairForRule(1, "keyF", "valueF");
		assertFilterForReadList(1);

		addConditionPairForRule(2, "keyB", "valueB");
		addConditionPairForRule(2, "keyF", "valueF");
		assertFilterForReadList(2);

		addConditionPairForRule(3, "keyA", "valueA");
		addConditionPairForRule(3, "keyF", "valueF");
		addConditionPairForRule(3, "keyE", "valueE");
		assertFilterForReadList(3);
	}

	private void addConditionPairForRule(int uniqueRuleNumber, String key, String value) {
		expectedConditionsMap.computeIfAbsent(uniqueRuleNumber, ArrayList::new);
		expectedConditionsMap.get(uniqueRuleNumber).add(new ConditionPair(key, value));
	}

	private void addUniqueRule(String uniqueKey, String... combineKeys) {
		Set<String> combineKeySet = new LinkedHashSet<>();
		for (String combineKey : combineKeys) {
			combineKeySet.add(combineKey);
		}
		uniqueDefinitions.add(new Unique(uniqueKey, combineKeySet));
	}

	private void addStorageTerm(String key, String value) {
		storageTerms.add(new StorageTerm("notImportantTermId", key, value));
	}

	private void assertDataIsInvalid(ValidationAnswer answer, Collection<String> errorMessage) {
		assertFalse(answer.dataIsValid());
		assertEquals(answer.getErrorMessages(), errorMessage);
	}

	private void assertNumberOfCallsToReadList(int calledNumberOfTimes) {
		recordStorage.MCR.assertNumberOfCallsToMethod("readList", calledNumberOfTimes);
	}

	private void assertCallToReadListWithRecordType(String recordType) {
		recordStorage.MCR.assertParameterAsEqual("readList", 0, "types", List.of(recordType));
	}

	private void assertFilterForReadList(int readListNumber, ConditionPair... expectedConditions) {
		Filter usedFilter = (Filter) recordStorage.MCR
				.getValueForMethodNameAndCallNumberAndParameterName("readList", readListNumber,
						"filter");
		assertOnlyOneIncludePart(usedFilter);
		assertNoExcludePart(usedFilter);
		assertFilterConditions(usedFilter, expectedConditionsMap.get(readListNumber));
	}

	private void assertOnlyOneIncludePart(Filter usedFilter) {
		assertEquals(usedFilter.include.size(), 1);
	}

	private void assertNoExcludePart(Filter usedFilter) {
		assertEquals(usedFilter.exclude.size(), 0);
	}

	private void assertFilterConditions(Filter usedFilter, List<ConditionPair> expectedConditions) {
		Part includePart = usedFilter.include.get(0);
		assertEquals(includePart.conditions.size(), expectedConditions.size());

		assertConditionsInPart(includePart, expectedConditions);
	}

	private void assertConditionsInPart(Part includePart, List<ConditionPair> expectedConditions) {
		int conditionNumber = 0;
		for (Condition condition : includePart.conditions) {
			ConditionPair expectedCondition = expectedConditions.get(conditionNumber);
			assertCondition(expectedCondition, condition);
			conditionNumber++;
		}
	}

	private void assertCondition(ConditionPair expectedCondition, Condition condition) {
		assertEquals(condition.key(), expectedCondition.conditionKey());
		assertEquals(condition.operator(), RelationalOperator.EQUAL_TO);
		assertEquals(condition.value(), expectedCondition.conditionValue());
	}

	private record ConditionPair(String conditionKey, String conditionValue) {
	}

	@Test
	public void testOnlyForTestGetRecordStorage() throws Exception {
		RecordStorage onlyForTestrecordStorage = ((UniqueValidatorImp) uniqueValidator)
				.onlyForTestGetRecordStorage();
		assertSame(onlyForTestrecordStorage, recordStorage);
	}
}