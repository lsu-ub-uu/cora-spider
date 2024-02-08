/*
 * Copyright 2016, 2021 Uppsala University Library
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

package se.uu.ub.cora.spider.extendedfunctionality;

import java.util.List;

public interface ExtendedFunctionalityProvider {
	/**
	 * Returns a list with all ExtendedFunctionalities hooked on CreateBeforeMetadataValidation (see
	 * {@link ExtendedFunctionalityPosition})and a recordType.
	 * 
	 * @param recordType
	 *            A string with the recordType name. Only the ExtendedFunctionality related to the
	 *            recordType should be returned.
	 * @return
	 */
	List<ExtendedFunctionality> getFunctionalityForCreateBeforeMetadataValidation(
			String recordType);

	/**
	 * Returns a list with all ExtendedFunctionalities hooked on CreateAfterMetadataValidation (see
	 * {@link ExtendedFunctionalityPosition})and a recordType.
	 * 
	 * @param recordType
	 *            A string with the recordType name. Only the ExtendedFunctionality related to the
	 *            recordType should be returned.
	 * @return
	 */
	List<ExtendedFunctionality> getFunctionalityForCreateAfterMetadataValidation(String recordType);

	/**
	 * Returns a list with all ExtendedFunctionalities hooked on CreateBeforeEnhance (see
	 * {@link ExtendedFunctionalityPosition})and a recordType.
	 * 
	 * @param recordType
	 *            A string with the recordType name. Only the ExtendedFunctionality related to the
	 *            recordType should be returned.
	 * @return
	 */
	List<ExtendedFunctionality> getFunctionalityForCreateBeforeEnhance(String recordType);

	/**
	 * Returns a list with all ExtendedFunctionalities hooked on ReadBeforeReturn (see
	 * {@link ExtendedFunctionalityPosition})and a recordType.
	 * 
	 * @param recordType
	 *            A string with the recordType name. Only the ExtendedFunctionality related to the
	 *            recordType should be returned.
	 * @return
	 */
	List<ExtendedFunctionality> getFunctionalityForReadBeforeReturn(String recordType);

	/**
	 * Returns a list with all ExtendedFunctionalities hooked on UpdateBeforeMetadataValidation (see
	 * {@link ExtendedFunctionalityPosition})and a recordType.
	 * 
	 * @param recordType
	 *            A string with the recordType name. Only the ExtendedFunctionality related to the
	 *            recordType should be returned.
	 * @return
	 */
	List<ExtendedFunctionality> getFunctionalityForUpdateBeforeMetadataValidation(
			String recordType);

	/**
	 * Returns a list with all ExtendedFunctionalities hooked on UpdateAfterMetadataValidation (see
	 * {@link ExtendedFunctionalityPosition})and a recordType.
	 * 
	 * @param recordType
	 *            A string with the recordType name. Only the ExtendedFunctionality related to the
	 *            recordType should be returned.
	 * @return
	 */
	List<ExtendedFunctionality> getFunctionalityForUpdateAfterMetadataValidation(String recordType);

	/**
	 * Returns a list with all ExtendedFunctionalities hooked on UpdateBeforeStore (see
	 * {@link ExtendedFunctionalityPosition})and a recordType.
	 * 
	 * @param recordType
	 *            A string with the recordType name. Only the ExtendedFunctionality related to the
	 *            recordType should be returned.
	 * @return
	 */
	List<ExtendedFunctionality> getFunctionalityForUpdateBeforeStore(String recordType);

	/**
	 * Returns a list with all ExtendedFunctionalities hooked on UpdateBeforeReturn (see
	 * {@link ExtendedFunctionalityPosition})and a recordType.
	 * 
	 * @param recordType
	 *            A string with the recordType name. Only the ExtendedFunctionality related to the
	 *            recordType should be returned.
	 * @return
	 */
	List<ExtendedFunctionality> getFunctionalityForUpdateBeforeReturn(String recordType);

	/**
	 * Returns a list with all ExtendedFunctionalities hooked on BeforeDelete (see
	 * {@link ExtendedFunctionalityPosition})and a recordType.
	 * 
	 * @param recordType
	 *            A string with the recordType name. Only the ExtendedFunctionality related to the
	 *            recordType should be returned.
	 * @return
	 */
	List<ExtendedFunctionality> getFunctionalityBeforeDelete(String recordType);

	/**
	 * Returns a list with all ExtendedFunctionalities hooked on AfterDelete (see
	 * {@link ExtendedFunctionalityPosition})and a recordType.
	 * 
	 * @param recordType
	 *            A string with the recordType name. Only the ExtendedFunctionality related to the
	 *            recordType should be returned.
	 * @return
	 */
	List<ExtendedFunctionality> getFunctionalityAfterDelete(String recordType);

	/**
	 * Returns a list with all ExtendedFunctionalities hooked on UpdateAfterStore (see
	 * {@link ExtendedFunctionalityPosition})and a recordType.
	 * 
	 * @param recordType
	 *            A string with the recordType name. Only the ExtendedFunctionality related to the
	 *            recordType should be returned.
	 * @return
	 */
	List<ExtendedFunctionality> getFunctionalityForUpdateAfterStore(String recordType);

}
