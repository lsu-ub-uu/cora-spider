/*
 * Copyright 2015, 2022 Uppsala University Library
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

import se.uu.ub.cora.data.DataGroup;
import se.uu.ub.cora.data.DataRecord;

public interface RecordCreator {
	/**
	 * createAndStoreRecord is a method intended to validate, create and store a record in storage
	 * and possibly in archive.
	 * 
	 * </p>
	 * If the type is an abstract type MUST a {@link MisuseException} be thrown, indicating that the
	 * requested record can not be created for an abstract type.
	 * </p>
	 * If the type and id already is already stored MUST a {@link ConflictException} be thrown,
	 * indicating that the requested record can not be created. The same error should be thrown if
	 * another record exists with the same id and a common abstract parent type.
	 * </p>
	 * 
	 * @param authToken
	 *            A string with the authToken of the user who performs the action.
	 * @param type
	 *            A string with the record type of the record.
	 * @param recordDataGroup
	 *            A {@link DataGroup} that contains the data to store.
	 * @return The {@link DataRecord} with the data that the user is allow to read from the record.
	 */
	DataRecord createAndStoreRecord(String authToken, String type, DataGroup record);

}
