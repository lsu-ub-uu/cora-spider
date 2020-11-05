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
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import se.uu.ub.cora.spider.extended.ExtendedFunctionality;
import se.uu.ub.cora.spider.extended.ExtendedFunctionalityProvider;

public class ExtendedFunctionalityProviderImp implements ExtendedFunctionalityProvider {

	private Iterable<ExtendedFunctionalityFactory> extendedFunctionalityFactories;
	private Map<String, List<ExtendedFunctionalityFactory>> createBeforeMetadataValidation = new HashMap<>();
	private Map<ExtendedFunctionalityPosition, Map<String, List<ExtendedFunctionalityFactory>>> factories = new HashMap<>();

	public ExtendedFunctionalityProviderImp(
			Iterable<ExtendedFunctionalityFactory> extendedFunctionalityFactories) {
		this.extendedFunctionalityFactories = extendedFunctionalityFactories;

		sortFactories();
	}

	private void sortFactories() {

		for (ExtendedFunctionalityFactory extendedFactory : extendedFunctionalityFactories) {
			List<ExtendedFunctionalityContext> efcs = extendedFactory
					.getExtendedFunctionalityContexts();
			for (ExtendedFunctionalityContext efc : efcs) {
				ExtendedFunctionalityPosition position = efc.extendedFunctionalityPosition;
				if (!factories.containsKey(position)) {
					factories.put(position, new HashMap<>());
				}
				Map<String, List<ExtendedFunctionalityFactory>> positionMap = factories
						.get(position);
				if (!positionMap.containsKey(efc.recordType)) {
					positionMap.put(efc.recordType, new ArrayList<>());
				}
				positionMap.get(efc.recordType).add(extendedFactory);
			}
		}
	}

	@Override
	public List<ExtendedFunctionality> getFunctionalityForCreateBeforeMetadataValidation(
			String recordType) {
		List<ExtendedFunctionality> functionalities = new ArrayList<>();
		if (factories.containsKey(ExtendedFunctionalityPosition.CREATE_BEFORE_METADATA_VALIDATION)
				&& factories.get(ExtendedFunctionalityPosition.CREATE_BEFORE_METADATA_VALIDATION)
						.containsKey(recordType)) {

			List<ExtendedFunctionalityFactory> currentFactories = factories
					.get(ExtendedFunctionalityPosition.CREATE_BEFORE_METADATA_VALIDATION)
					.get(recordType);
			for (ExtendedFunctionalityFactory extendedFactory : currentFactories) {
				functionalities.add(extendedFactory.factor(
						ExtendedFunctionalityPosition.CREATE_BEFORE_METADATA_VALIDATION,
						recordType));
			}
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

	public Iterable<ExtendedFunctionalityFactory> getFunctionalityFactoryImplementations() {
		return extendedFunctionalityFactories;
	}

}
