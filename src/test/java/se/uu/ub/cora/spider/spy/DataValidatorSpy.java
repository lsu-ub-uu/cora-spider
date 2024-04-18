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

import se.uu.ub.cora.bookkeeper.validator.DataValidator;
import se.uu.ub.cora.bookkeeper.validator.ValidationAnswer;
import se.uu.ub.cora.data.DataGroup;
import se.uu.ub.cora.testutils.mcr.MethodCallRecorder;
import se.uu.ub.cora.testutils.mrv.MethodReturnValues;

public class DataValidatorSpy implements DataValidator {
	public MethodCallRecorder MCR = new MethodCallRecorder();
	public MethodReturnValues MRV = new MethodReturnValues();

	public DataValidatorSpy() {
		MCR.useMRV(MRV);
		MRV.setDefaultReturnValuesSupplier("validateData", ValidationAnswer::new);
		MRV.setDefaultReturnValuesSupplier("validateListFilter", ValidationAnswer::new);
		MRV.setDefaultReturnValuesSupplier("validateIndexSettings", ValidationAnswer::new);
	}

	@Override
	public ValidationAnswer validateData(String metadataGroupId, DataGroup dataGroup) {
		return (ValidationAnswer) MCR.addCallAndReturnFromMRV("metadataGroupId", metadataGroupId,
				"dataGroup", dataGroup);
	}

	@Override
	public ValidationAnswer validateListFilter(String recordType, DataGroup filterDataGroup) {
		return (ValidationAnswer) MCR.addCallAndReturnFromMRV("recordType", recordType,
				"filterDataGroup", filterDataGroup);
	}

	@Override
	public ValidationAnswer validateIndexSettings(String recordType, DataGroup indexSettings) {
		return (ValidationAnswer) MCR.addCallAndReturnFromMRV("recordType", recordType,
				"indexSettings", indexSettings);
	}

}
