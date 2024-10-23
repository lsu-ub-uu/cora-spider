/*
 *	 Copyright 2024 Uppsala University Library
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
package se.uu.ub.cora.spider.extended.regex;

import java.util.Optional;
import java.util.regex.Pattern;

import se.uu.ub.cora.data.DataRecordGroup;
import se.uu.ub.cora.spider.extendedfunctionality.ExtendedFunctionality;
import se.uu.ub.cora.spider.extendedfunctionality.ExtendedFunctionalityData;
import se.uu.ub.cora.spider.record.DataException;

public class RegexExtendedFunctionality implements ExtendedFunctionality {

	private static final String TYPE = "type";
	private static final String REGEX = "regEx";
	private static final String TEXT_VARIABLE = "textVariable";

	@Override
	public void useExtendedFunctionality(ExtendedFunctionalityData data) {
		DataRecordGroup dataRecordGroup = data.dataRecordGroup;
		if (isTextVariable(dataRecordGroup)) {
			isValidRegexOrThrowException(dataRecordGroup);
		}
	}

	private boolean isTextVariable(DataRecordGroup dataRecordGroup) {
		if (dataRecordGroup.hasAttributes()) {
			Optional<String> type = dataRecordGroup.getAttributeValue(TYPE);
			return type.isPresent() && TEXT_VARIABLE.equals(type.get());
		}
		return false;
	}

	private void isValidRegexOrThrowException(DataRecordGroup dataRecordGroup) {
		String regExValue = dataRecordGroup.getFirstAtomicValueWithNameInData(REGEX);
		try {
			Pattern.compile(regExValue);
		} catch (Exception e) {
			throw new DataException("The supplied regEx is invalid, " + e.getMessage());
		}
	}
}