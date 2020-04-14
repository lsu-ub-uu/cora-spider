/*
 * Copyright 2016, 2020 Uppsala University Library
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

import se.uu.ub.cora.beefeater.authentication.User;
import se.uu.ub.cora.data.Action;
import se.uu.ub.cora.data.DataGroup;
import se.uu.ub.cora.data.DataRecord;
import se.uu.ub.cora.spider.authorization.AuthorizationException;
import se.uu.ub.cora.spider.authorization.SpiderAuthorizator;

/**
 * DataGroupToRecordEnhancer converts a {@link DataGroup} into a {@link DataRecord}. This includes
 * adding actions to the record and read actions to the links in the DataGroup. Use the
 * {@link #enhance(User, String, DataGroup)} method to convert a DataGroup.
 */
public interface DataGroupToRecordEnhancer {
	/**
	 * Enhance converts a DataGroup into a DataRecord. The conversion has a few major parts.
	 * <ol>
	 * <li>Create a new DataRecord, and add the DataGroup to it.</li>
	 * <li>Find out what actions the User is allowed to do for the record and add those actions to
	 * the DataRecord. This is a multistep process in itself.
	 * <ol>
	 * <li>Add standard actions for all recordTypes (read, update, delete, index,
	 * incomingLinks)</li>
	 * <li>If dataGroup beeing enhanced is a binary type, add upload</li>
	 * <li>If dataGroup beeing enhanced is a search type, add search</li>
	 * <li>If dataGroup beeing enhanced is a recordType type, add recordType specific actions
	 * (create, list, validate, search)</li>
	 * </ol>
	 * </li>
	 * <li>Redact information the user is not allowed to read from the DataGroup, based on settings
	 * in metadata and the users currently active roles.</li>
	 * <li>Add read links to all linked records in the DataGroup if the User has read access to the
	 * linked record.</li>
	 * </ol>
	 * 
	 * The implementations MUST throw an {@link AuthorizationException} if the user does NOT have
	 * authorization to read the record including security check against collected data, using
	 * {@link SpiderAuthorizator#checkAndGetUserAuthorizationsForActionOnRecordTypeAndCollectedData(User, String, String, DataGroup, boolean)}
	 * or similar method.
	 * <p>
	 * If no exception is thrown and the returned DataRecord contains {@link Action#READ} it is
	 * considered ok to send the record to the user.
	 * 
	 * @param user
	 *            The User that will get the DataRecord
	 * @param recordType
	 *            A String with the records recordType, it must be the implementing recordType (not
	 *            the abstract parent type if the recordType has a parent)
	 * @param dataGroup
	 *            A DataGroup with data to turn into a DataRecord
	 * @return A newly created DataRecord containing the DataGroup with added actions
	 */
	DataRecord enhance(User user, String recordType, DataGroup dataGroup);

}