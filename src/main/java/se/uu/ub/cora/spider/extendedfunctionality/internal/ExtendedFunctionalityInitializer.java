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
package se.uu.ub.cora.spider.extendedfunctionality.internal;

import java.util.ServiceLoader;

import se.uu.ub.cora.spider.dependency.SpiderDependencyProvider;
import se.uu.ub.cora.spider.extendedfunctionality.ExtendedFunctionalityFactory;
import se.uu.ub.cora.spider.extendedfunctionality.ExtendedFunctionalityProvider;

/**
 * ExtendedFunctionalityInitializer uses ServiceLoader to locate all implementations of
 * {@linkplain ExtendedFunctionalityFactory}. The found implementations are passed along to
 * {@linkplain ExtendedFunctionalityProvider} before returning an instance of the provider in the
 * getExtendedFunctionalityProvider method.
 */
public class ExtendedFunctionalityInitializer {
	private SpiderDependencyProvider dependencyProvider;

	public ExtendedFunctionalityInitializer(SpiderDependencyProvider dependencyProvider) {
		this.dependencyProvider = dependencyProvider;
	}

	/**
	 * getExtendedFunctionalityProvider is used to get an instance of ExtendedFunctionalityProvider.
	 * 
	 * @return A ExtendedFunctionalityProvider with the found implementations of
	 *         FunctionalityFactory
	 */
	public ExtendedFunctionalityProvider getExtendedFunctionalityProvider() {
		Iterable<ExtendedFunctionalityFactory> extendedFunctionalityFactories = ServiceLoader
				.load(ExtendedFunctionalityFactory.class);
		FactorySorter factorySorter = new FactorySorterImp(dependencyProvider,
				extendedFunctionalityFactories);
		return new ExtendedFunctionalityProviderImp(factorySorter);
	}

}
