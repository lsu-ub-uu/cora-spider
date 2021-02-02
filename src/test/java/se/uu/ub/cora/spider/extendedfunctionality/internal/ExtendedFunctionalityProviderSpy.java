/*
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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import se.uu.ub.cora.spider.extendedfunctionality.ExtendedFunctionality;
import se.uu.ub.cora.spider.extendedfunctionality.ExtendedFunctionalitySpy;

public class ExtendedFunctionalityProviderSpy implements ExtendedFunctionalityProvider {
	public List<ExtendedFunctionalitySpy> fetchedFunctionalityForCreateBeforeMetadataValidation = new ArrayList<>();
	public List<ExtendedFunctionalitySpy> fetchedFunctionalityForCreateAfterMetadataValidation = new ArrayList<>();
	public List<ExtendedFunctionalitySpy> fetchedFunctionalityForUpdateBeforeMetadataValidation = new ArrayList<>();
	public List<ExtendedFunctionalitySpy> fetchedFunctionalityForUpdateAfterMetadataValidation = new ArrayList<>();
	public List<ExtendedFunctionalitySpy> fetchedFunctionalityForCreateBeforeReturn = new ArrayList<>();
	public List<ExtendedFunctionalitySpy> fetchedFunctionalityBeforeDelete = new ArrayList<>();
	public List<ExtendedFunctionalitySpy> fetchedFunctionalityForUpdateBeforeStore = new ArrayList<>();
	public List<ExtendedFunctionalitySpy> fetchedFunctionalityForUpdateAfterStore = new ArrayList<>();
	public Map<String, String> recordTypes = new HashMap<>();

	@Override
	public List<ExtendedFunctionality> getFunctionalityForCreateBeforeMetadataValidation(
			String recordType) {
		return createListWithTwoExtendedFunctionalitySpies(
				fetchedFunctionalityForCreateBeforeMetadataValidation);
	}

	@Override
	public List<ExtendedFunctionality> getFunctionalityForCreateAfterMetadataValidation(
			String recordType) {
		return createListWithTwoExtendedFunctionalitySpies(
				fetchedFunctionalityForCreateAfterMetadataValidation);
	}

	@Override
	public List<ExtendedFunctionality> getFunctionalityForCreateBeforeReturn(String recordType) {
		return createListWithTwoExtendedFunctionalitySpies(
				fetchedFunctionalityForCreateBeforeReturn);
	}

	private List<ExtendedFunctionality> createListWithTwoExtendedFunctionalitySpies(
			List<ExtendedFunctionalitySpy> fetchedFunctionalityList) {
		ArrayList<ExtendedFunctionality> listOfExtendedFunctionality = new ArrayList<>();
		ExtendedFunctionalitySpy extendedFunctionalitySpy = new ExtendedFunctionalitySpy();
		listOfExtendedFunctionality.add(extendedFunctionalitySpy);
		fetchedFunctionalityList.add(extendedFunctionalitySpy);
		ExtendedFunctionalitySpy extendedFunctionalitySpy2 = new ExtendedFunctionalitySpy();
		listOfExtendedFunctionality.add(extendedFunctionalitySpy2);
		fetchedFunctionalityList.add(extendedFunctionalitySpy2);
		return listOfExtendedFunctionality;
	}

	@Override
	public List<ExtendedFunctionality> getFunctionalityForUpdateBeforeMetadataValidation(
			String recordType) {
		return createListWithTwoExtendedFunctionalitySpies(
				fetchedFunctionalityForUpdateBeforeMetadataValidation);
	}

	@Override
	public List<ExtendedFunctionality> getFunctionalityForUpdateAfterMetadataValidation(
			String recordType) {
		return createListWithTwoExtendedFunctionalitySpies(
				fetchedFunctionalityForUpdateAfterMetadataValidation);
	}

	@Override
	public List<ExtendedFunctionality> getFunctionalityBeforeDelete(String recordType) {
		return createListWithTwoExtendedFunctionalitySpies(fetchedFunctionalityBeforeDelete);
	}

	@Override
	public List<ExtendedFunctionality> getFunctionalityAfterDelete(String recordType) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public List<ExtendedFunctionality> getFunctionalityForUpdateBeforeStore(String recordType) {
		recordTypes.put("updateBeforeStore", recordType);
		return createListWithTwoExtendedFunctionalitySpies(
				fetchedFunctionalityForUpdateBeforeStore);
	}

	@Override
	public List<ExtendedFunctionality> getFunctionalityForUpdateAfterStore(String recordType) {
		recordTypes.put("updateAfterStore", recordType);
		return createListWithTwoExtendedFunctionalitySpies(fetchedFunctionalityForUpdateAfterStore);
	}

}
