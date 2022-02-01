/*
 * Copyright 2016, 2022 Uppsala University Library
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

/**
 * ExtendedFunctionality is functionality that can be plugged into Spider to extend its capabilities
 * when handling records. It is a very open system, and almost any type of functionality can be
 * added.
 * 
 */
public interface ExtendedFunctionality {

	/**
	 * useExtendedFunctionality is called from different places in spider to use the
	 * extendedFunctionality
	 * 
	 * @param data
	 *            An ExtendedFunctionalityData populated with authToken, dataGroup, etc.
	 */
	void useExtendedFunctionality(ExtendedFunctionalityData data);
}
