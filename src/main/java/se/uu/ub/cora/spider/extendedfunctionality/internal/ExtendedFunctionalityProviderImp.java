/*
 * Copyright 2020, 2021 Uppsala University Library
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

import static se.uu.ub.cora.spider.extendedfunctionality.ExtendedFunctionalityPosition.CREATE_AFTER_METADATA_VALIDATION;
import static se.uu.ub.cora.spider.extendedfunctionality.ExtendedFunctionalityPosition.CREATE_BEFORE_ENHANCE;
import static se.uu.ub.cora.spider.extendedfunctionality.ExtendedFunctionalityPosition.CREATE_BEFORE_METADATA_VALIDATION;
import static se.uu.ub.cora.spider.extendedfunctionality.ExtendedFunctionalityPosition.DELETE_AFTER;
import static se.uu.ub.cora.spider.extendedfunctionality.ExtendedFunctionalityPosition.DELETE_BEFORE;
import static se.uu.ub.cora.spider.extendedfunctionality.ExtendedFunctionalityPosition.READ_BEFORE_RETURN;
import static se.uu.ub.cora.spider.extendedfunctionality.ExtendedFunctionalityPosition.UPDATE_AFTER_METADATA_VALIDATION;
import static se.uu.ub.cora.spider.extendedfunctionality.ExtendedFunctionalityPosition.UPDATE_AFTER_STORE;
import static se.uu.ub.cora.spider.extendedfunctionality.ExtendedFunctionalityPosition.UPDATE_BEFORE_METADATA_VALIDATION;
import static se.uu.ub.cora.spider.extendedfunctionality.ExtendedFunctionalityPosition.UPDATE_BEFORE_RETURN;
import static se.uu.ub.cora.spider.extendedfunctionality.ExtendedFunctionalityPosition.UPDATE_BEFORE_STORE;

import java.util.List;

import se.uu.ub.cora.spider.extendedfunctionality.ExtendedFunctionality;
import se.uu.ub.cora.spider.extendedfunctionality.ExtendedFunctionalityPosition;
import se.uu.ub.cora.spider.extendedfunctionality.ExtendedFunctionalityProvider;

public class ExtendedFunctionalityProviderImp implements ExtendedFunctionalityProvider {
	private FactorySorter factorySorter;

	public ExtendedFunctionalityProviderImp(FactorySorter factorySorter) {
		this.factorySorter = factorySorter;
	}

	@Override
	public List<ExtendedFunctionality> getFunctionalityForCreateBeforeMetadataValidation(
			String recordType) {
		return getFunctionalityForPositionAndRecordType(CREATE_BEFORE_METADATA_VALIDATION,
				recordType);
	}

	private List<ExtendedFunctionality> getFunctionalityForPositionAndRecordType(
			ExtendedFunctionalityPosition position, String recordType) {
		return factorySorter.getFunctionalityForPositionAndRecordType(position, recordType);
	}

	@Override
	public List<ExtendedFunctionality> getFunctionalityForCreateAfterMetadataValidation(
			String recordType) {
		return factorySorter.getFunctionalityForPositionAndRecordType(
				CREATE_AFTER_METADATA_VALIDATION, recordType);
	}

	@Override
	public List<ExtendedFunctionality> getFunctionalityForCreateBeforeEnhance(String recordType) {
		return factorySorter.getFunctionalityForPositionAndRecordType(CREATE_BEFORE_ENHANCE,
				recordType);
	}

	@Override
	public List<ExtendedFunctionality> getFunctionalityForReadBeforeReturn(String recordType) {
		return factorySorter.getFunctionalityForPositionAndRecordType(READ_BEFORE_RETURN,
				recordType);
	}

	@Override
	public List<ExtendedFunctionality> getFunctionalityForUpdateBeforeMetadataValidation(
			String recordType) {
		return factorySorter.getFunctionalityForPositionAndRecordType(
				UPDATE_BEFORE_METADATA_VALIDATION, recordType);
	}

	@Override
	public List<ExtendedFunctionality> getFunctionalityForUpdateAfterMetadataValidation(
			String recordType) {
		return factorySorter.getFunctionalityForPositionAndRecordType(
				UPDATE_AFTER_METADATA_VALIDATION, recordType);
	}

	@Override
	public List<ExtendedFunctionality> getFunctionalityForUpdateBeforeReturn(String recordType) {
		return factorySorter.getFunctionalityForPositionAndRecordType(UPDATE_BEFORE_RETURN,
				recordType);
	}

	@Override
	public List<ExtendedFunctionality> getFunctionalityBeforeDelete(String recordType) {
		return factorySorter.getFunctionalityForPositionAndRecordType(DELETE_BEFORE, recordType);
	}

	@Override
	public List<ExtendedFunctionality> getFunctionalityAfterDelete(String recordType) {
		return factorySorter.getFunctionalityForPositionAndRecordType(DELETE_AFTER, recordType);
	}

	public FactorySorter getFactorySorterNeededForTest() {
		return factorySorter;
	}

	@Override
	public List<ExtendedFunctionality> getFunctionalityForUpdateBeforeStore(String recordType) {
		return factorySorter.getFunctionalityForPositionAndRecordType(UPDATE_BEFORE_STORE,
				recordType);
	}

	@Override
	public List<ExtendedFunctionality> getFunctionalityForUpdateAfterStore(String recordType) {
		return factorySorter.getFunctionalityForPositionAndRecordType(UPDATE_AFTER_STORE,
				recordType);
	}

}
