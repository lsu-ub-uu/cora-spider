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
package se.uu.ub.cora.spider.extendedfunctionality;

import java.util.HashMap;
import java.util.Map;

import se.uu.ub.cora.beefeater.authentication.User;
import se.uu.ub.cora.data.DataRecord;
import se.uu.ub.cora.data.DataRecordGroup;

/**
 * ExtendedFunctionalityData contains data, useful when implementing an extended functionality. The
 * data is, recordId, recordType, authToken, user, dataGroup, previouslyStoredDataGroup.
 */
public class ExtendedFunctionalityData {
	/**
	 * A String with the authToken representing the currently logged in user
	 */
	public String authToken;

	/**
	 * A DataRecordGroup containing all data for the record currently being handled
	 */
	public DataRecordGroup dataRecordGroup;

	/**
	 * A String with the recordType of the record currently being handled
	 */
	public String recordType;

	/**
	 * A String with the recordId of the record currently being handled
	 * <p>
	 * <b>Note! the recordId is not set for calls for
	 * getFunctionalityForCreateBeforeMetadataValidation and
	 * getFunctionalityForCreateAfterMetadataValidation as it is not yet determined what it shall
	 * be.</b>
	 */
	public String recordId;

	/**
	 * A User with the currently active user
	 */
	public User user;

	/**
	 * The DataRecordGroup for the record currently being handled as it was read from storage at the
	 * begining of the current operation.
	 * <p>
	 * <b>Note! this dataRecordGroup is only set for calls made to extended functionality when
	 * updating a record.</b>
	 */
	public DataRecordGroup previouslyStoredDataRecordGroup;

	/**
	 * The representation of the record as DataRecord
	 */
	public DataRecord dataRecord;

	/**
	 * dataSharer is a Map intended to share data between different extendedFunctionalites, that is
	 * not represented in the other fields in this class.
	 * <p>
	 * To avoid conflicting keys between different extendedFunctionallites are the
	 * extendedFunctionality that sets a value in this map expected to use a prefix for the key with
	 * the className of the extendedFunctionality.
	 */
	public Map<String, Object> dataSharer = new HashMap<>();

}
