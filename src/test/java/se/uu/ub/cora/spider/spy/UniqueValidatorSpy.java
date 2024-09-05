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
package se.uu.ub.cora.spider.spy;

import java.util.List;
import java.util.Set;

import se.uu.ub.cora.bookkeeper.recordtype.Unique;
import se.uu.ub.cora.bookkeeper.validator.ValidationAnswer;
import se.uu.ub.cora.data.collected.StorageTerm;
import se.uu.ub.cora.spider.unique.UniqueValidator;
import se.uu.ub.cora.testutils.mcr.MethodCallRecorder;
import se.uu.ub.cora.testutils.mrv.MethodReturnValues;

public class UniqueValidatorSpy implements UniqueValidator {
	public MethodCallRecorder MCR = new MethodCallRecorder();
	public MethodReturnValues MRV = new MethodReturnValues();

	public UniqueValidatorSpy() {
		MCR.useMRV(MRV);
		MRV.setDefaultReturnValuesSupplier("validateUnique", ValidationAnswerSpy::new);
	}

	@Override
	public ValidationAnswer validateUnique(String recordType, String recordId,
			List<Unique> uniques, Set<StorageTerm> storageTerms) {
		return (ValidationAnswer) MCR.addCallAndReturnFromMRV("recordType", recordType, "uniques",
				uniques, "storageTerms", storageTerms);
	}
}