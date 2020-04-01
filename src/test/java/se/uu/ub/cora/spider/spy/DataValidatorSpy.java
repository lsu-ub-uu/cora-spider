/*
 * Copyright 2015, 2020 Uppsala University Library
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

import java.util.HashSet;
import java.util.Set;

import se.uu.ub.cora.bookkeeper.validator.DataValidator;
import se.uu.ub.cora.bookkeeper.validator.ValidationAnswer;
import se.uu.ub.cora.data.DataGroup;

public class DataValidatorSpy implements DataValidator {
	public MethodCallRecorder MCR = new MethodCallRecorder();
	public boolean validValidation = true;
	private Set<String> notValidForMetadataGroupId = new HashSet<>();

	@Override
	public ValidationAnswer validateData(String metadataGroupId, DataGroup dataGroup) {
		MCR.addCall("validateData", "metadataGroupId", metadataGroupId, "dataGroup", dataGroup);

		ValidationAnswer validationAnswer = new ValidationAnswer();
		if (!validValidation) {
			validationAnswer.addErrorMessage("Data always invalid");
		}
		if (notValidForMetadataGroupId.contains(metadataGroupId)) {
			validationAnswer.addErrorMessage("Data invalid for metadataId " + metadataGroupId);
		}
		return validationAnswer;
	}

	public void setNotValidForMetadataGroupId(String metadataGroupId) {
		notValidForMetadataGroupId.add(metadataGroupId);
	}

}
