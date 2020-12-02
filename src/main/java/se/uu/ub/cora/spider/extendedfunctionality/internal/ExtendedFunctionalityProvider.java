/*
 * Copyright 2016 Uppsala University Library
 * Copyright 2016 Olov McKie
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

package se.uu.ub.cora.spider.extendedfunctionality.internal;

import java.util.List;

import se.uu.ub.cora.spider.extendedfunctionality.ExtendedFunctionality;

public interface ExtendedFunctionalityProvider {
	List<ExtendedFunctionality> getFunctionalityForCreateBeforeMetadataValidation(
			String recordType);

	List<ExtendedFunctionality> getFunctionalityForCreateAfterMetadataValidation(String recordType);

	List<ExtendedFunctionality> getFunctionalityForCreateBeforeReturn(String recordType);

	List<ExtendedFunctionality> getFunctionalityForUpdateBeforeMetadataValidation(
			String recordType);

	List<ExtendedFunctionality> getFunctionalityForUpdateAfterMetadataValidation(String recordType);

	List<ExtendedFunctionality> getFunctionalityForUpdateBeforeStore(String recordType);

	List<ExtendedFunctionality> getFunctionalityBeforeDelete(String recordType);

	List<ExtendedFunctionality> getFunctionalityAfterDelete(String recordType);
}
