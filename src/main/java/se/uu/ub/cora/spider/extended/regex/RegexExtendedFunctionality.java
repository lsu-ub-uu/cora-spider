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

import se.uu.ub.cora.data.DataAtomic;
import se.uu.ub.cora.spider.extendedfunctionality.ExtendedFunctionality;
import se.uu.ub.cora.spider.extendedfunctionality.ExtendedFunctionalityData;
import se.uu.ub.cora.spider.record.DataException;

public class RegexExtendedFunctionality implements ExtendedFunctionality {

	private static final String TEXT_VARIABLE = "textVariable";
	private static final String TYPE = "type";

	@Override
	public void useExtendedFunctionality(ExtendedFunctionalityData data) {
		if (isTextVariable(data)) {
			isValidRegexOrThrowException(data);
		}
	}

	private void isValidRegexOrThrowException(ExtendedFunctionalityData data) {
		DataAtomic regEx = data.dataRecordGroup.getFirstDataAtomicWithNameInData("regEx");
		String regExValue = regEx.getValue();
		try {
			Pattern.compile(regExValue);
		} catch (Exception e) {
			throw new DataException("Failed to compile the supplied regEx: " + regExValue);
		}
	}

	private boolean isTextVariable(ExtendedFunctionalityData data) {
		if (data.dataRecordGroup.hasAttributes()) {
			Optional<String> type = data.dataRecordGroup.getAttributeValue(TYPE);
			return type.isPresent() && TEXT_VARIABLE.equals(type.get());
		}
		return false;
	}

}