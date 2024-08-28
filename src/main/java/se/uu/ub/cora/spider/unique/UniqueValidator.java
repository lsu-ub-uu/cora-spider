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

import java.util.Set;

import se.uu.ub.cora.bookkeeper.recordtype.Unique;
import se.uu.ub.cora.bookkeeper.validator.ValidationAnswer;
import se.uu.ub.cora.data.collected.StorageTerm;

public interface UniqueValidator {

	/**
	 * ValidateUnique verify the unique constrains set in the recordType.
	 * 
	 * @param recordType
	 *            is the recordType related to the unique definition to validate.
	 * @param uniques
	 *            is a Set of Unique object. Each {@link Unique} object defines a unique rule for a
	 *            record.
	 * @param storageTerms
	 *            Incomming data as dataGroup. The unique constraints will be match to the variables
	 *            that matches the storageTerms.
	 * 
	 * @return A ValidationAnswer with the result of the validation.
	 */
	ValidationAnswer validateUnique(String recordType, Set<Unique> uniques,
			Set<StorageTerm> storageTerms);
}
