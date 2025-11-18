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

import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.StringJoiner;

import se.uu.ub.cora.bookkeeper.recordtype.Unique;
import se.uu.ub.cora.bookkeeper.validator.ValidationAnswer;
import se.uu.ub.cora.data.DataRecordGroup;
import se.uu.ub.cora.data.collected.StorageTerm;
import se.uu.ub.cora.storage.Condition;
import se.uu.ub.cora.storage.Filter;
import se.uu.ub.cora.storage.Part;
import se.uu.ub.cora.storage.RecordStorage;
import se.uu.ub.cora.storage.RelationalOperator;
import se.uu.ub.cora.storage.StorageReadResult;

public class UniqueValidatorImp implements UniqueValidator {
	private RecordStorage recordStorage;

	public static UniqueValidatorImp usingRecordStorage(RecordStorage recordStorage) {
		return new UniqueValidatorImp(recordStorage);
	}

	UniqueValidatorImp(RecordStorage recordStorage) {
		this.recordStorage = recordStorage;
	}

	@Override
	public ValidationAnswer validateUniqueForExistingRecord(String recordType, String recordId,
			List<Unique> uniqueRules, Set<StorageTerm> storageTerms) {
		return validateUnique(recordType, Optional.of(recordId), uniqueRules, storageTerms);
	}

	ValidationAnswer validateUnique(String recordType, Optional<String> recordId,
			List<Unique> uniqueRules, Set<StorageTerm> storageTerms) {
		if (noNeedToRunValidation(uniqueRules, storageTerms)) {
			return noDuplicatesFound();
		}
		return checkUniqueRulesAndBuildAnswer(recordType, recordId, uniqueRules, storageTerms);
	}

	private boolean noNeedToRunValidation(List<Unique> uniques, Set<StorageTerm> storageTerms) {
		return uniques.isEmpty() || storageTerms.isEmpty();
	}

	private ValidationAnswer noDuplicatesFound() {
		return new ValidationAnswer();
	}

	private ValidationAnswer checkUniqueRulesAndBuildAnswer(String recordType,
			Optional<String> recordId, List<Unique> uniqueRules, Set<StorageTerm> storageTerms) {
		List<String> errorMessages = checkuniqueRules(recordType, recordId, uniqueRules,
				storageTerms);
		return createValidationAnswer(errorMessages);
	}

	private ValidationAnswer createValidationAnswer(List<String> errorMessages) {
		ValidationAnswer answer = new ValidationAnswer();
		answer.addErrorMessages(errorMessages);
		return answer;
	}

	private List<String> checkuniqueRules(String recordType, Optional<String> recordId,
			List<Unique> uniqueRules, Set<StorageTerm> storageTerms) {
		List<String> errorMessages = new ArrayList<>();
		for (Unique uniqueRule : uniqueRules) {
			Optional<String> errorMessage = checkUniqueRule(recordType, recordId, storageTerms,
					uniqueRule);
			if (errorMessage.isPresent()) {
				errorMessages.add(errorMessage.get());
			}
		}
		return errorMessages;
	}

	private Optional<String> checkUniqueRule(String recordType, Optional<String> recordId,
			Set<StorageTerm> storageTerms, Unique unique) {
		Optional<Filter> filter = possiblyCreateFilter(unique, storageTerms);
		if (filter.isPresent()) {
			return checkUniqueInStorageUsingFilter(recordType, recordId, filter.get());
		}
		return Optional.empty();
	}

	private Optional<String> checkUniqueInStorageUsingFilter(String recordType,
			Optional<String> recordId, Filter filter) {
		StorageReadResult readResult = recordStorage.readList(recordType, filter);

		if (noDuplicatesFoundInStorage(recordId, readResult)) {
			return Optional.empty();
		}

		List<Condition> conditions = getConditionsFromFilter(filter);
		return Optional.of(createAndAddErrorAnswer(conditions));
	}

	private boolean noDuplicatesFoundInStorage(Optional<String> recordId,
			StorageReadResult readResult) {
		return readResult.totalNumberOfMatches == 0
				|| onlyMatchForDuplicatesIsOurCurrentRecord(recordId, readResult);
	}

	private boolean onlyMatchForDuplicatesIsOurCurrentRecord(Optional<String> recordId,
			StorageReadResult readResult) {
		return readResult.totalNumberOfMatches == 1 && recordId.isPresent()
				&& (matchIsOurCurrentRecord(recordId.get(), readResult));
	}

	private boolean matchIsOurCurrentRecord(String recordId, StorageReadResult readResult) {
		String id = getFirstIdFromResult(readResult);
		return id.equals(recordId);
	}

	private String getFirstIdFromResult(StorageReadResult readResult) {
		DataRecordGroup firstRecordGroup = readResult.listOfDataRecordGroups.get(0);
		return firstRecordGroup.getId();
	}

	private List<Condition> getConditionsFromFilter(Filter filter) {
		return filter.include.get(0).conditions;
	}

	private String createAndAddErrorAnswer(List<Condition> conditions) {
		String conditionsAsString = conditionsToString(conditions);
		return "A record matching the unique rule with " + conditionsAsString
				+ " already exists in the system";
	}

	private String conditionsToString(List<Condition> conditions) {
		StringJoiner stringJoiner = new StringJoiner(", ");
		for (Condition condition : conditions) {
			String conditionAsString = MessageFormat.format("[key: {0}, value: {1}]",
					condition.key(), condition.value());
			stringJoiner.add(conditionAsString);
		}
		return stringJoiner.toString();
	}

	private Optional<Filter> possiblyCreateFilter(Unique unique, Set<StorageTerm> storageTerms) {
		Optional<Condition> uniqueCondition = possiblyCreateCondition(storageTerms,
				unique.uniqueTermStorageKey());
		if (uniqueCondition.isEmpty()) {
			return Optional.empty();
		}
		List<Condition> combineConditions = possiblyCreateCombineConditions(
				unique.combineTermStorageKeys(), storageTerms);
		return createFilterForConditions(uniqueCondition.get(), combineConditions);
	}

	private Optional<Filter> createFilterForConditions(Condition uniqueCondition,
			List<Condition> combineConditions) {
		Filter filter = new Filter();
		Part part = new Part();
		filter.include.add(part);
		part.conditions.add(uniqueCondition);
		part.conditions.addAll(combineConditions);
		return Optional.of(filter);
	}

	private List<Condition> possiblyCreateCombineConditions(Set<String> combineTermStorageKeys,
			Set<StorageTerm> storageTerms) {
		List<Condition> conditions = new ArrayList<>();
		for (String combineTermStorageKey : combineTermStorageKeys) {
			Optional<Condition> combineCondition = possiblyCreateCondition(storageTerms,
					combineTermStorageKey);
			if (combineCondition.isPresent()) {
				conditions.add(combineCondition.get());
			}
		}
		return conditions;
	}

	private Optional<Condition> possiblyCreateCondition(Set<StorageTerm> storageTerms,
			String termKeyFromUnique) {
		for (StorageTerm storageTerm : storageTerms) {
			if (termKeyFromUnique.equals(storageTerm.storageKey())) {
				return createCondition(termKeyFromUnique, storageTerm);
			}
		}
		return Optional.empty();
	}

	private Optional<Condition> createCondition(String termKeyFromUnique, StorageTerm storageTerm) {
		String storageTermValue = storageTerm.value();
		Condition condition = new Condition(termKeyFromUnique, RelationalOperator.EQUAL_TO,
				storageTermValue);
		return Optional.of(condition);
	}

	public RecordStorage onlyForTestGetRecordStorage() {
		return recordStorage;
	}

	@Override
	public ValidationAnswer validateUniqueForNewRecord(String recordType, List<Unique> uniques,
			Set<StorageTerm> storageTerms) {
		return validateUnique(recordType, Optional.empty(), uniques, storageTerms);
	}
}
