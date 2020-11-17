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
package se.uu.ub.cora.spider.extendedfunctionality;

import java.util.List;

import se.uu.ub.cora.spider.dependency.SpiderDependencyProvider;

/**
 * ExtendedFunctionalityFactory is used to factor {@link ExtendedFunctionality}, to be used in
 * various places in Spider for different recordTypes.<br>
 */
public interface ExtendedFunctionalityFactory {

	/**
	 * initializeUsingDependencyProvider is called once when the system starts up, and should be
	 * used to initialize the factory if needed. The systems {@link SpiderDependencyProvider} is
	 * sent to the method to make it possible to start the factory.
	 * 
	 * @param dependencyProvider
	 *            The systems populated SpiderDependencyProvider
	 */
	void initializeUsingDependencyProvider(SpiderDependencyProvider dependencyProvider);

	/**
	 * getExtendedFunctionalityContexts should be implemented so that it returns a List with
	 * {@link ExtendedFunctionalityContext} for wich circumstances this factory produces
	 * ExtendedFunctionality instances.
	 * 
	 * @return A List of ExtendedFunctionalityContexts to determin under what cirumstances, the
	 *         extendedFunctionalities that this factory produces, are intended to be called
	 */
	List<ExtendedFunctionalityContext> getExtendedFunctionalityContexts();

	/**
	 * Factor is used by spider to get an instans of ExtendedFunctionality to use in predetermined
	 * places in Spider. Factor should be implemented so that it creates a list of
	 * {@link ExtendedFunctionality} for the specified position and recordType.<br>
	 * <br>
	 * Factories implementing this interface will only be called for the positions and recordTypes,
	 * specified by the
	 * {@link ExtendedFunctionalityFactory#factor(ExtendedFunctionalityPosition, String)}method.
	 * 
	 * @param position
	 *            The current {@link ExtendedFunctionalityPosition}
	 * @param recordType
	 *            The current recordType
	 * 
	 * @return A List of instances of factored extended funtionalities
	 */
	List<ExtendedFunctionality> factor(ExtendedFunctionalityPosition position, String recordType);

}