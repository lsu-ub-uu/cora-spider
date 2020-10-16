/*
 * Copyright 2020 Uppsala University Library
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
package se.uu.ub.cora.spider.extended2;

import java.util.ArrayList;
import java.util.List;

import se.uu.ub.cora.spider.extended.ExtendedFunctionality;
import se.uu.ub.cora.spider.extended.ExtendedFunctionalityProvider;

public class ExtendedFunctionalityProviderImp implements ExtendedFunctionalityProvider {

	private FunctionalityFactories extendedFactories;

	public ExtendedFunctionalityProviderImp(FunctionalityFactories extendedFactories) {
		this.extendedFactories = extendedFactories;
	}

	@Override
	public List<ExtendedFunctionality> getFunctionalityForCreateBeforeMetadataValidation(
			String recordType) {
		List<ExtendedFunctionality> functionalities = new ArrayList<>();
		for (FunctionalityFactory extendedFactory : extendedFactories.createBeforeMetadataValidation) {
			functionalities.add(extendedFactory.factor());
		}
		return functionalities;
	}

	@Override
	public List<ExtendedFunctionality> getFunctionalityForCreateAfterMetadataValidation(
			String recordType) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public List<ExtendedFunctionality> getFunctionalityForCreateBeforeReturn(String recordType) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public List<ExtendedFunctionality> getFunctionalityForUpdateBeforeMetadataValidation(
			String recordType) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public List<ExtendedFunctionality> getFunctionalityForUpdateAfterMetadataValidation(
			String recordType) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public List<ExtendedFunctionality> getFunctionalityBeforeDelete(String recordType) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public List<ExtendedFunctionality> getFunctionalityAfterDelete(String recordType) {
		// TODO Auto-generated method stub
		return null;
	}

	public FunctionalityFactories getExtendedFactories() {
		return extendedFactories;
	}

}
