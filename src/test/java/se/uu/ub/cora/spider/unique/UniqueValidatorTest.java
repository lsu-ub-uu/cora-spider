/*
 * Copyright 2024, 2025 Uppsala University Library
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
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.function.Supplier;

import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import se.uu.ub.cora.bookkeeper.recordtype.Unique;
import se.uu.ub.cora.bookkeeper.validator.ValidationAnswer;
import se.uu.ub.cora.data.DataProvider;
import se.uu.ub.cora.data.collected.StorageTerm;
import se.uu.ub.cora.data.spies.DataFactorySpy;
import se.uu.ub.cora.data.spies.DataRecordGroupSpy;
import se.uu.ub.cora.storage.Condition;
import se.uu.ub.cora.storage.Filter;
import se.uu.ub.cora.storage.Part;
import se.uu.ub.cora.storage.RecordStorage;
import se.uu.ub.cora.storage.RelationalOperator;
import se.uu.ub.cora.storage.StorageReadResult;
import se.uu.ub.cora.storage.spies.RecordStorageSpy;
import se.uu.ub.cora.testutils.mcr.MethodCallRecorder;
import se.uu.ub.cora.testutils.mrv.MethodReturnValues;

public class UniqueValidatorTest {

	private static final String RECORD_TYPE = "someRecordType";
	private static final String RECORD_ID = "someRecordId";
	private UniqueValidator uniqueValidator;
	private RecordStorageSpy recordStorage;
	private Set<StorageTerm> storageTerms;
	private List<Unique> uniqueDefinitions;
	private Map<Integer, List<ConditionPair>> expectedConditionsMap;
	private DataFactorySpy dataFactorySpy;
	private StorageReadResult duplicateReadResult;
	private StorageReadResult okStorageReadResult;

	@BeforeMethod
	private void beforeMethod() {
		setUpFactoriesAndProviders();
		resetStorageReadResultForDuplicate();
		okStorageReadResult = new StorageReadResult();
		recordStorage = new RecordStorageSpy();
		uniqueValidator = UniqueValidatorImp.usingRecordStorage(recordStorage);
		uniqueDefinitions = new LinkedList<>();
		storageTerms = new LinkedHashSet<>();
		expectedConditionsMap = new HashMap<>();
	}

	private void setUpFactoriesAndProviders() {
		dataFactorySpy = new DataFactorySpy();
		DataProvider.onlyForTestSetDataFactory(dataFactorySpy);
	}

	private void resetStorageReadResultForDuplicate() {
		duplicateReadResult = new StorageReadResult();
		DataRecordGroupSpy recordGroup = new DataRecordGroupSpy();
		duplicateReadResult.listOfDataRecordGroups = List.of(recordGroup);
	}

	@org.testng.annotations.DataProvider(name = "new")
	public Iterator<Callable<ValidationAnswer>> testCasesForNew() {
		return Arrays.asList(newRecord()).iterator();
	}

	@org.testng.annotations.DataProvider(name = "Existing")
	public Iterator<Callable<ValidationAnswer>> testCasesForExisting() {
		return Arrays.asList(existingRecord()).iterator();
	}

	@org.testng.annotations.DataProvider(name = "newAndExisting")
	public Iterator<Callable<ValidationAnswer>> testCasesForNewAndExisting() {
		return Arrays.asList(newRecord(), existingRecord()).iterator();
	}

	@Test(dataProvider = "newAndExisting")
	private void whenNoUniqueIsSetInRecordType(Callable<ValidationAnswer> callable)
			throws Exception {
		ValidationAnswer answer = callable.call();

		assertDataIsValid(answer);
		assertReadListInStorageNotCalled();
	}

	private Callable<ValidationAnswer> existingRecord() {
		return () -> uniqueValidator.validateUniqueForExistingRecord(RECORD_TYPE, RECORD_ID,
				uniqueDefinitions, storageTerms);
	}

	private Callable<ValidationAnswer> newRecord() {
		return () -> uniqueValidator.validateUniqueForNewRecord(RECORD_TYPE, uniqueDefinitions,
				storageTerms);
	}

	private void assertDataIsValid(ValidationAnswer answer) {
		assertTrue(answer.dataIsValid());
	}

	private void assertReadListInStorageNotCalled() {
		recordStorage.MCR.assertMethodNotCalled("readList");
	}

	@Test(dataProvider = "newAndExisting")
	private void oneUniqueRule_NoCollectedData_IsValid(Callable<ValidationAnswer> callable)
			throws Exception {
		addUniqueRule("keyA");

		ValidationAnswer answer = callable.call();

		assertDataIsValid(answer);
		assertReadListInStorageNotCalled();
	}

	@Test(dataProvider = "newAndExisting")
	private void noUniqueRule_HasCollectedData_IsValid(Callable<ValidationAnswer> callable)
			throws Exception {
		addStorageTerm("keyA", "valueA");

		ValidationAnswer answer = callable.call();

		assertDataIsValid(answer);
		assertReadListInStorageNotCalled();
	}

	@Test(dataProvider = "newAndExisting")
	private void oneUniqueRule_HasCollectedData_IsValid(Callable<ValidationAnswer> callable)
			throws Exception {
		addUniqueRule("keyA");
		addStorageTerm("keyA", "valueA");

		ValidationAnswer answer = callable.call();

		assertDataIsValid(answer);
		assertNumberOfCallsToReadList(1);
		assertCallToReadListWithRecordType(RECORD_TYPE);

		expectedConditionPairForRule(0, "keyA", "valueA");
		assertFilterForReadList(0);
	}

	@Test(dataProvider = "newAndExisting")
	public void oneUniqueRule_HasCollectedData_IsNotValid(Callable<ValidationAnswer> callable)
			throws Exception {
		recordStorage.MRV.setDefaultReturnValuesSupplier("readList", () -> duplicateReadResult);
		addUniqueRule("keyA");
		addStorageTerm("keyA", "valueA");

		ValidationAnswer answer = callable.call();

		assertDataIsInvalid(answer, List.of(
				"A record matching the unique rule with [key: keyA, value: valueA] already exists "
						+ "in the system"));
		assertNumberOfCallsToReadList(1);
		assertCallToReadListWithRecordType(RECORD_TYPE);

		expectedConditionPairForRule(0, "keyA", "valueA");
		assertFilterForReadList(0);
	}

	@Test(dataProvider = "newAndExisting")
	public void oneUniqueRule_HasCollectedData_IsNotValid_MoreThanOnePriorWithSameValues(
			Callable<ValidationAnswer> callable) throws Exception {
		DataRecordGroupSpy recordGroup1 = new DataRecordGroupSpy();
		recordGroup1.MRV.setDefaultReturnValuesSupplier("isInTrashBin", () -> Optional.of(false));
		DataRecordGroupSpy recordGroup2 = new DataRecordGroupSpy();
		recordGroup2.MRV.setDefaultReturnValuesSupplier("isInTrashBin", () -> Optional.of(false));

		DataRecordGroupSpy currentGroup = new DataRecordGroupSpy();
		currentGroup.MRV.setDefaultReturnValuesSupplier("getId", () -> RECORD_ID);
		duplicateReadResult.listOfDataRecordGroups = List.of(recordGroup1, recordGroup2,
				currentGroup);

		recordStorage.MRV.setDefaultReturnValuesSupplier("readList", () -> duplicateReadResult);
		addUniqueRule("keyA");
		addStorageTerm("keyA", "valueA");

		ValidationAnswer answer = callable.call();

		assertDataIsInvalid(answer, List.of(
				"A record matching the unique rule with [key: keyA, value: valueA] already exists "
						+ "in the system"));
		assertNumberOfCallsToReadList(1);
		assertCallToReadListWithRecordType(RECORD_TYPE);

		expectedConditionPairForRule(0, "keyA", "valueA");
		assertFilterForReadList(0);
	}

	@Test(dataProvider = "new")
	public void new_oneUniqueRule_HasCollectedData_IsInValid_InTrashOrCurrent(
			Callable<ValidationAnswer> callable) throws Exception {
		DataRecordGroupSpy recordGroup1 = new DataRecordGroupSpy();
		recordGroup1.MRV.setDefaultReturnValuesSupplier("isInTrashBin", () -> Optional.of(true));

		DataRecordGroupSpy recordGroup2 = new DataRecordGroupSpy();
		recordGroup2.MRV.setDefaultReturnValuesSupplier("isInTrashBin", () -> Optional.of(true));

		DataRecordGroupSpy recordGroup3 = new DataRecordGroupSpy();
		recordGroup3.MRV.setDefaultReturnValuesSupplier("isInTrashBin", () -> Optional.of(true));
		recordGroup3.MRV.setDefaultReturnValuesSupplier("getId", () -> RECORD_ID);

		DataRecordGroupSpy currentGroup = new DataRecordGroupSpy();
		recordGroup3.MRV.setDefaultReturnValuesSupplier("isInTrashBin", () -> Optional.of(false));
		currentGroup.MRV.setDefaultReturnValuesSupplier("getId", () -> RECORD_ID);

		duplicateReadResult.listOfDataRecordGroups = List.of(recordGroup1, recordGroup2,
				recordGroup3, currentGroup);

		recordStorage.MRV.setDefaultReturnValuesSupplier("readList", () -> duplicateReadResult);
		addUniqueRule("keyA");
		addStorageTerm("keyA", "valueA");

		ValidationAnswer answer = callable.call();

		assertDataIsInvalid(answer, List.of(
				"A record matching the unique rule with [key: keyA, value: valueA] already exists "
						+ "in the system"));
		assertNumberOfCallsToReadList(1);
		assertCallToReadListWithRecordType(RECORD_TYPE);

		expectedConditionPairForRule(0, "keyA", "valueA");
		assertFilterForReadList(0);
	}

	@Test(dataProvider = "Existing")
	public void existing_oneUniqueRule_HasCollectedData_IsValid_InTrashOrCurrent(
			Callable<ValidationAnswer> callable) throws Exception {
		DataRecordGroupSpy recordGroup1 = new DataRecordGroupSpy();
		recordGroup1.MRV.setDefaultReturnValuesSupplier("isInTrashBin", () -> Optional.of(true));

		DataRecordGroupSpy recordGroup2 = new DataRecordGroupSpy();
		recordGroup2.MRV.setDefaultReturnValuesSupplier("isInTrashBin", () -> Optional.of(true));

		DataRecordGroupSpy recordGroup3 = new DataRecordGroupSpy();
		recordGroup3.MRV.setDefaultReturnValuesSupplier("isInTrashBin", () -> Optional.of(true));
		recordGroup3.MRV.setDefaultReturnValuesSupplier("getId", () -> RECORD_ID);

		DataRecordGroupSpy currentGroup = new DataRecordGroupSpy();
		recordGroup3.MRV.setDefaultReturnValuesSupplier("isInTrashBin", () -> Optional.of(false));
		currentGroup.MRV.setDefaultReturnValuesSupplier("getId", () -> RECORD_ID);

		duplicateReadResult.listOfDataRecordGroups = List.of(recordGroup1, recordGroup2,
				recordGroup3, currentGroup);

		recordStorage.MRV.setDefaultReturnValuesSupplier("readList", () -> duplicateReadResult);
		addUniqueRule("keyA");
		addStorageTerm("keyA", "valueA");

		ValidationAnswer answer = callable.call();
		assertDataIsValid(answer);
		assertNumberOfCallsToReadList(1);
		assertCallToReadListWithRecordType(RECORD_TYPE);

		expectedConditionPairForRule(0, "keyA", "valueA");
		assertFilterForReadList(0);
	}

	@Test(dataProvider = "new")
	public void new_twoUniqueRule_HasCollectedData_IsValid_SecondIsDuplicateSameRecord_FirstIsDuplicate(
			Callable<ValidationAnswer> callable) throws Exception {
		StorageReadResult duplicateSameReadResult = new StorageReadResult();
		DataRecordGroupSpy recordGroup = new DataRecordGroupSpy();
		duplicateSameReadResult.listOfDataRecordGroups = List.of(recordGroup);
		recordGroup.MRV.setDefaultReturnValuesSupplier("getId", () -> RECORD_ID);

		List<StorageReadResult> returnStorageResults = List.of(duplicateReadResult,
				duplicateSameReadResult);

		recordStorage.MRV.setDefaultReturnValuesSupplier("readList",
				createSupplierForList(returnStorageResults));

		addUniqueRule("keyA");
		addUniqueRule("keyB");
		addStorageTerm("keyA", "valueA");
		addStorageTerm("keyB", "valueB");

		ValidationAnswer answer = callable.call();

		assertDataIsInvalid(answer, List.of(
				"A record matching the unique rule with [key: keyA, value: valueA] already exists in the system",
				"A record matching the unique rule with [key: keyB, value: valueB] already exists "
						+ "in the system"));
		assertNumberOfCallsToReadList(2);
		assertCallToReadListWithRecordType(RECORD_TYPE);

		expectedConditionPairForRule(0, "keyA", "valueA");
		assertFilterForReadList(0);
		expectedConditionPairForRule(1, "keyB", "valueB");
		assertFilterForReadList(1);
	}

	@Test(dataProvider = "Existing")
	public void twoUniqueRule_HasCollectedData_IsValid_SecondIsDuplicateSameRecord_FirstIsDuplicate(
			Callable<ValidationAnswer> callable) throws Exception {
		StorageReadResult duplicateSameReadResult = new StorageReadResult();
		DataRecordGroupSpy recordGroup = new DataRecordGroupSpy();
		duplicateSameReadResult.listOfDataRecordGroups = List.of(recordGroup);
		recordGroup.MRV.setDefaultReturnValuesSupplier("getId", () -> RECORD_ID);

		List<StorageReadResult> returnStorageResults = List.of(duplicateReadResult,
				duplicateSameReadResult);

		recordStorage.MRV.setDefaultReturnValuesSupplier("readList",
				createSupplierForList(returnStorageResults));

		addUniqueRule("keyA");
		addUniqueRule("keyB");
		addStorageTerm("keyA", "valueA");
		addStorageTerm("keyB", "valueB");

		ValidationAnswer answer = callable.call();

		assertDataIsInvalid(answer, List.of(
				"A record matching the unique rule with [key: keyA, value: valueA] already exists "
						+ "in the system"));
		assertNumberOfCallsToReadList(2);
		assertCallToReadListWithRecordType(RECORD_TYPE);

		expectedConditionPairForRule(0, "keyA", "valueA");
		assertFilterForReadList(0);
		expectedConditionPairForRule(1, "keyB", "valueB");
		assertFilterForReadList(1);
	}

	@Test(dataProvider = "new")
	public void oneUniqueRule_HasCollectedData_IsInvalidValid_IsDuplicateCanNotBeSameRecord(
			Callable<ValidationAnswer> callable) throws Exception {
		DataRecordGroupSpy recordGroup = new DataRecordGroupSpy();
		duplicateReadResult.listOfDataRecordGroups = List.of(recordGroup);
		recordGroup.MRV.setDefaultReturnValuesSupplier("getId", () -> RECORD_ID);

		recordStorage.MRV.setDefaultReturnValuesSupplier("readList", () -> duplicateReadResult);
		addUniqueRule("keyA");
		addStorageTerm("keyA", "valueA");

		ValidationAnswer answer = callable.call();

		assertDataIsInvalid(answer, List.of(
				"A record matching the unique rule with [key: keyA, value: valueA] already exists "
						+ "in the system"));
		assertNumberOfCallsToReadList(1);
		assertCallToReadListWithRecordType(RECORD_TYPE);

		expectedConditionPairForRule(0, "keyA", "valueA");
		assertFilterForReadList(0);
	}

	@Test(dataProvider = "Existing")
	public void oneUniqueRule_HasCollectedData_IsValid_IsDuplicateSameRecord(
			Callable<ValidationAnswer> callable) throws Exception {
		DataRecordGroupSpy recordGroup = new DataRecordGroupSpy();
		duplicateReadResult.listOfDataRecordGroups = List.of(recordGroup);
		recordGroup.MRV.setDefaultReturnValuesSupplier("getId", () -> RECORD_ID);

		recordStorage.MRV.setDefaultReturnValuesSupplier("readList", () -> duplicateReadResult);
		addUniqueRule("keyA");
		addStorageTerm("keyA", "valueA");

		ValidationAnswer answer = callable.call();

		assertDataIsValid(answer);
		assertNumberOfCallsToReadList(1);
		assertCallToReadListWithRecordType(RECORD_TYPE);

		expectedConditionPairForRule(0, "keyA", "valueA");
		assertFilterForReadList(0);
	}

	@Test(dataProvider = "newAndExisting")
	public void oneUniqueRuleWithCombine_HasCollectedData_IsNotValid(
			Callable<ValidationAnswer> callable) throws Exception {
		recordStorage.MRV.setDefaultReturnValuesSupplier("readList", () -> duplicateReadResult);
		addUniqueRule("keyA", "keyB", "keyC");
		addStorageTerm("keyA", "valueA");
		addStorageTerm("keyB", "valueB");
		addStorageTerm("keyC", "valueC");

		ValidationAnswer answer = callable.call();

		assertDataIsInvalid(answer,
				List.of("A record matching the unique rule with [key: keyA, value: valueA], "
						+ "[key: keyB, value: valueB], [key: keyC, value: valueC] "
						+ "already exists in the system"));
		assertNumberOfCallsToReadList(1);
		assertCallToReadListWithRecordType(RECORD_TYPE);

		expectedConditionPairForRule(0, "keyA", "valueA");
		expectedConditionPairForRule(0, "keyB", "valueB");
		expectedConditionPairForRule(0, "keyC", "valueC");
		assertFilterForReadList(0);
	}

	@Test(dataProvider = "newAndExisting")
	public void oneUniqueRule_HasNoMatchingCollectedData_IsValid(
			Callable<ValidationAnswer> callable) throws Exception {
		addUniqueRule("keyA");
		addStorageTerm("keyB", "valueB");

		ValidationAnswer answer = callable.call();

		assertDataIsValid(answer);
		assertNumberOfCallsToReadList(0);
	}

	@Test(dataProvider = "newAndExisting")
	public void oneUniqueRule_HasOneMatchingCollectedData_IsValid(
			Callable<ValidationAnswer> callable) throws Exception {
		addUniqueRule("keyA");

		addStorageTerm("keyB", "valueB");
		addStorageTerm("keyA", "valueA");

		ValidationAnswer answer = callable.call();

		assertDataIsValid(answer);
		assertNumberOfCallsToReadList(1);
		assertCallToReadListWithRecordType(RECORD_TYPE);

		expectedConditionPairForRule(0, "keyA", "valueA");
		assertFilterForReadList(0);
	}

	@Test(dataProvider = "newAndExisting")
	public void oneUniqueRuleWithOneCombine_AllMatchingCollectedData_IsValid(
			Callable<ValidationAnswer> callable) throws Exception {
		addUniqueRule("keyA", "keyB");

		addStorageTerm("keyB", "valueB");
		addStorageTerm("keyA", "valueA");

		ValidationAnswer answer = callable.call();

		assertDataIsValid(answer);
		assertNumberOfCallsToReadList(1);
		assertCallToReadListWithRecordType(RECORD_TYPE);

		expectedConditionPairForRule(0, "keyA", "valueA");
		expectedConditionPairForRule(0, "keyB", "valueB");
		assertFilterForReadList(0);
	}

	@Test(dataProvider = "newAndExisting")
	public void oneUniqueRuleWithOneCombine_OnlyUniqueTermMatchingCollectedData_IsValid(
			Callable<ValidationAnswer> callable) throws Exception {
		addUniqueRule("keyA", "keyB");

		addStorageTerm("keyA", "valueA");
		addStorageTerm("keyC", "valueC");

		ValidationAnswer answer = callable.call();

		assertDataIsValid(answer);
		assertNumberOfCallsToReadList(1);
		assertCallToReadListWithRecordType(RECORD_TYPE);

		expectedConditionPairForRule(0, "keyA", "valueA");
		assertFilterForReadList(0);
	}

	@Test(dataProvider = "newAndExisting")
	public void oneUniqueRuleWithTwoCombine_UniqueTermAndOneCombineTermMatchingCollectedData_IsValid(
			Callable<ValidationAnswer> callable) throws Exception {
		addUniqueRule("keyA", "keyB", "keyC");

		addStorageTerm("keyA", "valueA");
		addStorageTerm("keyC", "valueC");

		ValidationAnswer answer = callable.call();

		assertDataIsValid(answer);
		assertNumberOfCallsToReadList(1);
		assertCallToReadListWithRecordType(RECORD_TYPE);

		expectedConditionPairForRule(0, "keyA", "valueA");
		expectedConditionPairForRule(0, "keyC", "valueC");
		assertFilterForReadList(0);
	}

	@Test(dataProvider = "newAndExisting")
	public void twoUniqueRuleWithTwoCombine_AllMatchingCollectedData_IsValid(
			Callable<ValidationAnswer> callable) throws Exception {
		addUniqueRule("keyA", "keyB");
		addUniqueRule("keyC");

		addStorageTerm("keyA", "valueA");
		addStorageTerm("keyB", "valueB");
		addStorageTerm("keyC", "valueC");

		ValidationAnswer answer = callable.call();

		assertDataIsValid(answer);
		assertNumberOfCallsToReadList(2);
		assertCallToReadListWithRecordType(RECORD_TYPE);

		expectedConditionPairForRule(0, "keyA", "valueA");
		expectedConditionPairForRule(0, "keyB", "valueB");
		assertFilterForReadList(0);

		expectedConditionPairForRule(1, "keyC", "valueC");
		assertFilterForReadList(1);
	}

	@Test(dataProvider = "newAndExisting")
	public void twoUniqueRuleWithTwoCombine_AllMatchingCollectedData_IsInValid(
			Callable<ValidationAnswer> callable) throws Exception {
		addUniqueRule("keyA", "keyB");
		addUniqueRule("keyC");

		addStorageTerm("keyA", "valueA");
		addStorageTerm("keyB", "valueB");
		addStorageTerm("keyC", "valueC");

		List<StorageReadResult> returnStorageResults = List.of(duplicateReadResult,
				okStorageReadResult);

		recordStorage.MRV.setDefaultReturnValuesSupplier("readList",
				createSupplierForList(returnStorageResults));

		ValidationAnswer answer = callable.call();

		String error1 = "A record matching the unique rule with [key: keyA, value: valueA], "
				+ "[key: keyB, value: valueB] already exists in the system";

		assertDataIsInvalid(answer, List.of(error1));
		assertNumberOfCallsToReadList(2);
		assertCallToReadListWithRecordType(RECORD_TYPE);

		expectedConditionPairForRule(0, "keyA", "valueA");
		expectedConditionPairForRule(0, "keyB", "valueB");
		assertFilterForReadList(0);

		expectedConditionPairForRule(1, "keyC", "valueC");
		assertFilterForReadList(1);
	}

	private Supplier<?> createSupplierForList(List<StorageReadResult> returnStorageResults) {
		return new Supplier<StorageReadResult>() {
			int counter = -1;

			@Override
			public StorageReadResult get() {
				counter++;
				return returnStorageResults.get(counter);
			}
		};
	}

	@Test(dataProvider = "newAndExisting")
	public void manyUniqueRuleWithTwoCombine_ManyMatchingCollectedData_IsInvalid(
			Callable<ValidationAnswer> callable) throws Exception {
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

		ValidationAnswer answer = callable.call();

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
		assertCallToReadListWithRecordType(RECORD_TYPE);

		expectedConditionPairForRule(0, "keyA", "valueA");
		expectedConditionPairForRule(0, "keyB", "valueB");
		assertFilterForReadList(0);

		expectedConditionPairForRule(1, "keyD", "valueD");
		expectedConditionPairForRule(1, "keyE", "valueE");
		expectedConditionPairForRule(1, "keyF", "valueF");
		assertFilterForReadList(1);

		expectedConditionPairForRule(2, "keyB", "valueB");
		expectedConditionPairForRule(2, "keyF", "valueF");
		assertFilterForReadList(2);

		expectedConditionPairForRule(3, "keyA", "valueA");
		expectedConditionPairForRule(3, "keyF", "valueF");
		expectedConditionPairForRule(3, "keyE", "valueE");
		assertFilterForReadList(3);
	}

	@Test(dataProvider = "newAndExisting")
	public void manyUniqueRuleWithTwoCombine_ManyMatchingCollectedData_IsValid_2(
			Callable<ValidationAnswer> callable) throws Exception {
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

		ValidationAnswer answer = callable.call();

		assertDataIsValid(answer);
		assertNumberOfCallsToReadList(4);
		assertCallToReadListWithRecordType(RECORD_TYPE);

		expectedConditionPairForRule(0, "keyA", "valueA");
		expectedConditionPairForRule(0, "keyB", "valueB");
		assertFilterForReadList(0);

		expectedConditionPairForRule(1, "keyD", "valueD");
		expectedConditionPairForRule(1, "keyE", "valueE");
		expectedConditionPairForRule(1, "keyF", "valueF");
		assertFilterForReadList(1);

		expectedConditionPairForRule(2, "keyB", "valueB");
		expectedConditionPairForRule(2, "keyF", "valueF");
		assertFilterForReadList(2);

		expectedConditionPairForRule(3, "keyA", "valueA");
		expectedConditionPairForRule(3, "keyF", "valueF");
		expectedConditionPairForRule(3, "keyE", "valueE");
		assertFilterForReadList(3);
	}

	@Test
	public void testOnlyForTestGetRecordStorage() {
		RecordStorage onlyForTestrecordStorage = ((UniqueValidatorImp) uniqueValidator)
				.onlyForTestGetRecordStorage();
		assertSame(onlyForTestrecordStorage, recordStorage);
	}

	private void expectedConditionPairForRule(int uniqueRuleNumber, String key, String value) {
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
		recordStorage.MCR.assertParameterAsEqual("readList", 0, "type", recordType);
	}

	private void assertFilterForReadList(int readListNumber) {
		Filter usedFilter = (Filter) recordStorage.MCR
				.getParameterForMethodAndCallNumberAndParameter("readList", readListNumber,
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

	class OnlyForTestUniqueValidator extends UniqueValidatorImp {
		public MethodCallRecorder MCR = new MethodCallRecorder();
		public MethodReturnValues MRV = new MethodReturnValues();

		OnlyForTestUniqueValidator(RecordStorage recordStorage) {
			super(recordStorage);
			MCR.useMRV(MRV);
			MRV.setDefaultReturnValuesSupplier("validateUnique", ValidationAnswer::new);
		}

		@Override
		ValidationAnswer validateUnique(String recordType, Optional<String> recordId,
				List<Unique> uniqueRules, Set<StorageTerm> storageTerms) {
			return (ValidationAnswer) MCR.addCallAndReturnFromMRV("recordType", recordType,
					"recordId", recordId, "uniqueRules", uniqueRules, "storageTerms", storageTerms);
		}
	}

}