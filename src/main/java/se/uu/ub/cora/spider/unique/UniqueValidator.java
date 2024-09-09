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

import se.uu.ub.cora.bookkeeper.recordtype.Unique;
import se.uu.ub.cora.bookkeeper.validator.ValidationAnswer;
import se.uu.ub.cora.data.collected.StorageTerm;

public interface UniqueValidator {
	/**
	 * validateUniqueForExistingRecord verify the unique constrains set in the recordType.
	 * </p>
	 * This method checks for duplicate records based on the unique constraints set up in metadata,
	 * unique in recordType with matching storageTerms in metadata. Matching values found in storage
	 * are reported as errors in the answer unless the matched values are for the same recordId as
	 * specified.
	 * 
	 * @param recordType
	 *            is the recordType related to the unique definition to validate.
	 * @param recordId
	 *            is the recordId of the specific record to validate.
	 * @param uniques
	 *            A List of Uniques, each {@link Unique} object defines a unique rule for a record.
	 * @param storageTerms
	 *            A set of {@link StorageTerm}s to be checked for unique
	 * 
	 * @return A ValidationAnswer with the result of the unique validation.
	 */
	ValidationAnswer validateUniqueForExistingRecord(String recordType, String recordId,
			List<Unique> uniques, Set<StorageTerm> storageTerms);

	/**
	 * validateUniqueForNewRecord verify the unique constrains set in the recordType.
	 * </p>
	 * This method checks for duplicate records based on the unique constraints set up in metadata,
	 * unique in recordType with matching storageTerms in metadata. Matching values found in storage
	 * are reported as errors in the answer.
	 * 
	 * @param recordType
	 *            is the recordType related to the unique definition to validate.
	 * @param uniques
	 *            A List of Uniques, each {@link Unique} object defines a unique rule for a record.
	 * @param storageTerms
	 *            A set of {@link StorageTerm}s to be checked for unique
	 * 
	 * @return A ValidationAnswer with the result of the unique validation.
	 */
	ValidationAnswer validateUniqueForNewRecord(String recordType, List<Unique> uniques,
			Set<StorageTerm> storageTerms);
}
