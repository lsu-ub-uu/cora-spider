/*
 * Copyright 2022 Uppsala University Library
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
package se.uu.ub.cora.spider.data;

import se.uu.ub.cora.data.DataGroup;
import se.uu.ub.cora.storage.Filter;

/**
 * DataGroupToFilter is intended to be used to convert a filter as a {@link DataGroup} to a
 * {@link Filter}
 * </p>
 * Implementations are not expected to be threadsafe, but must be able to handle multiple sequential
 * calls to {@link #convert(DataGroup)}.
 */
public interface DataGroupToFilter {
	/**
	 * convert method converts a DataGroup to a Filter. The dataGroup must have the correct filter
	 * structure.
	 * 
	 * @param dataGroup
	 *            a filter as DataGroup.
	 * @return the representation of the sent filter as a Filter.
	 */
	Filter convert(DataGroup dataGroup);

}