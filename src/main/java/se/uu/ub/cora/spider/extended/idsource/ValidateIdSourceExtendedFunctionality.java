/*
 * Copyright 2025 Uppsala University Library
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
package se.uu.ub.cora.spider.extended.idsource;

import java.text.MessageFormat;

import se.uu.ub.cora.data.DataRecordGroup;
import se.uu.ub.cora.data.DataRecordLink;
import se.uu.ub.cora.spider.data.DataMissingException;
import se.uu.ub.cora.spider.extendedfunctionality.ExtendedFunctionality;
import se.uu.ub.cora.spider.extendedfunctionality.ExtendedFunctionalityData;

public class ValidateIdSourceExtendedFunctionality implements ExtendedFunctionality {

	@Override
	public void useExtendedFunctionality(ExtendedFunctionalityData data) {
		DataRecordGroup dataRecordGroup = data.dataRecordGroup;
		validateIdSource(dataRecordGroup);
	}

	private void validateIdSource(DataRecordGroup dataRecordGroup) {
		String idSource = dataRecordGroup.getFirstAtomicValueWithNameInData("idSource");
		if ("sequence".equals(idSource)) {
			ensureSequenceLinkExists(dataRecordGroup);
		}
	}

	private void ensureSequenceLinkExists(DataRecordGroup dataRecordGroup) {
		boolean sequenceExists = dataRecordGroup.containsChildOfTypeAndName(DataRecordLink.class,
				"sequence");
		if (!sequenceExists) {
			sequenceLinkDoesNotExistsThrowError(dataRecordGroup);
		}
	}

	private void sequenceLinkDoesNotExistsThrowError(DataRecordGroup dataRecordGroup) {
		String message = "The record type {0} must link to a sequence when a sequence is chosen as the idSource.";
		throw new DataMissingException(MessageFormat.format(message, dataRecordGroup.getId()));
	}

}
