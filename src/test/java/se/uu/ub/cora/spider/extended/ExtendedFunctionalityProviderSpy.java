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

package se.uu.ub.cora.spider.extended;

import java.util.ArrayList;
import java.util.List;

public class ExtendedFunctionalityProviderSpy implements ExtendedFunctionalityProvider {
	public List<ExtendedFunctionalitySpy> fetchedFunctionalityForCreateBeforeMetadataValidation = new ArrayList<>();
	public List<ExtendedFunctionalitySpy> fetchedFunctionalityForCreateAfterMetadataValidation = new ArrayList<>();

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

}
