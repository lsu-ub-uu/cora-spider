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

import java.util.List;
import java.util.Set;

import se.uu.ub.cora.bookkeeper.validator.ValidationAnswer;
import se.uu.ub.cora.data.collected.StorageTerm;
import se.uu.ub.cora.storage.RecordStorage;

public class UniqueValidatorImp implements UniqueValidator {

	private RecordStorage recordStorage;

	public static UniqueValidatorImp usingRecordStorage(RecordStorage recordStorage) {
		return new UniqueValidatorImp(recordStorage);
	}

	private UniqueValidatorImp(RecordStorage recordStorage) {
		this.recordStorage = recordStorage;
	}

	@Override
	public ValidationAnswer validateUnique(String recordType, Set<Object> uniques,
			Set<StorageTerm> storageTerms) {
		if (!uniques.isEmpty() && !storageTerms.isEmpty()) {
			recordStorage.readList(List.of(recordType), null);
		}
		return new ValidationAnswer();
	}

}
