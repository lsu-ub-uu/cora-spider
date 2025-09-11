/*
 * Copyright 2025 Uppsala University Library
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

package se.uu.ub.cora.spider.record;

import se.uu.ub.cora.data.DataRecord;

public interface RecordDecorator {

	/**
	 * decorateRecord returns a decorated record as a DataRecord. A decorated record means that the
	 * definition (text) of the record fields are included as attributes in all the languages
	 * defined in the metadata.
	 * <p>
	 * If user not authorized then it throws an {@link AuthenticationException}
	 * 
	 * @param recordToDecorate
	 *            (In/Out parameter) A {@link DataRecord} to be decorated.
	 * @param authToken
	 *            A String with the authToken of the user reading the record
	 */

	void decorateRecord(DataRecord recordToDecorate, String authToken);
}
