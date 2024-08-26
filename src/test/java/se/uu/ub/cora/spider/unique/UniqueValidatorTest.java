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
import se.uu.ub.cora.storage.spies.RecordStorageSpy;

public class UniqueValidatorTest {

	private static final String SOME_RECORD_TYPE = "someRecordType";
	private UniqueValidator uniqueValidator;
	Set<StorageTerm> storageTerms = new LinkedHashSet<>();
	Set<Unique> uniqueDefinitions = new LinkedHashSet<>();
	private RecordStorageSpy recordStorage;
	private Unique uniqueWithoutCombineTerms;

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

		assertTrue(answer.dataIsValid());
		assertReadListInStorageNotCalled();
	}

	private void assertReadListInStorageNotCalled() {
		recordStorage.MCR.assertMethodNotCalled("readList");
	}

	@Test
	public void testOneUniqueRuleExist_HasDefinitionButNoIncomingData_IsUnique() throws Exception {
		ValidationAnswer answer = uniqueValidator.validateUnique(SOME_RECORD_TYPE,
				Set.of(uniqueWithoutCombineTerms), Collections.emptySet());

		assertTrue(answer.dataIsValid());
		assertReadListInStorageNotCalled();
	}

	@Test
	public void testOneUniqueRuleExist_HasNoDefinitionButIncomingData_IsUnique() throws Exception {
		StorageTerm storageTerm = new StorageTerm("someStorageTermId", "someUniqueTermStorageKey",
				"someTermValue");

		ValidationAnswer answer = uniqueValidator.validateUnique(SOME_RECORD_TYPE,
				Collections.emptySet(), Set.of(storageTerm));

		assertTrue(answer.dataIsValid());
		assertReadListInStorageNotCalled();
	}

	@Test
	public void testOneUniqueRuleExist_HasDefinitionAndIncomingData_IsUnique() throws Exception {
		StorageTerm storageTerm = new StorageTerm("someStorageTermId", "someUniqueTermStorageKey",
				"someTermValue");

		ValidationAnswer answer = uniqueValidator.validateUnique(SOME_RECORD_TYPE,
				Set.of(uniqueWithoutCombineTerms), Set.of(storageTerm));

		assertTrue(answer.dataIsValid());

		recordStorage.MCR.assertNumberOfCallsToMethod("readList", 1);

		assertCallToReadListWithFilter(SOME_RECORD_TYPE);
	}

	private void assertCallToReadListWithFilter(String recordType) {
		assertCallReadListParameterRecordType(recordType);
		List<String> types = (List<String>) recordStorage.MCR
				.getValueForMethodNameAndCallNumberAndParameterName("readList", 0, "types");
	}

	private void assertCallReadListParameterRecordType(String recordType) {
		List<String> types = (List<String>) recordStorage.MCR
				.getValueForMethodNameAndCallNumberAndParameterName("readList", 0, "types");
		assertEquals(types.size(), 1);
		assertEquals(types.get(0), recordType);
	}

}
