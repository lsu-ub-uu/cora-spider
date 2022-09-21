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

import java.util.List;

import se.uu.ub.cora.beefeater.authentication.User;
import se.uu.ub.cora.bookkeeper.recordpart.DataRedactor;
import se.uu.ub.cora.data.Action;
import se.uu.ub.cora.data.DataGroup;
import se.uu.ub.cora.data.DataRecord;
import se.uu.ub.cora.spider.authorization.AuthorizationException;
import se.uu.ub.cora.spider.authorization.SpiderAuthorizator;

/**
 * DataGroupToRecordEnhancer converts a {@link DataGroup} into a {@link DataRecord}. This includes
 * adding actions to the record and read actions to the links in the DataGroup. Use the
 * {@link #enhance(User, String, DataGroup, DataRedactor)} method to convert a DataGroup when read
 * access is required and {@link #enhanceIgnoringReadAccess(User, String, DataGroup, DataRedactor)}
 * when the user might not have read access, create etc.
 */
public interface DataGroupToRecordEnhancer {
	/**
	 * Enhance converts a DataGroup into a DataRecord. It is very similar to
	 * {@link #enhanceIgnoringReadAccess(User, String, DataGroup, DataRedactor)} except that it will
	 * not complete if the User does not have read access to the enhanced record and it will instead
	 * throw an exception. This method is intended to be used when enhancing the DataGroup as one of
	 * the last steps before returning the data to the user, during actions that require read access
	 * such as read, list, search etc. where the user must have permission to read the affected
	 * data.
	 * <p>
	 * The conversion has a few major parts.
	 * <ol>
	 * <li>Create a new DataRecord, and add the DataGroup to it.</li>
	 * <li>Find out what actions the User is allowed to do for the record and add those actions to
	 * the DataRecord. This is a multistep process in itself.
	 * <ol>
	 * <li>Add standard actions for all recordTypes (possibly add read, update, delete, index,
	 * incomingLinks actions)</li>
	 * <li>If dataGroup beeing enhanced is a binary type, possibly add upload action</li>
	 * <li>If dataGroup beeing enhanced is a search type, possibly add add search action</li>
	 * <li>If dataGroup beeing enhanced is a recordType type, add recordType specific actions
	 * (create, list, validate, search)</li>
	 * </ol>
	 * </li>
	 * <li>Add the users read and write recordPartPermissions to the record</li>
	 * <li>Redact information the user is not allowed to read from the DataGroup, based on settings
	 * in metadata and the users currently active roles.</li>
	 * <li>Add read links to all linked records in the DataGroup if the User has read access to the
	 * linked record.</li>
	 * </ol>
	 * 
	 * The implementations MUST throw an {@link AuthorizationException} if the user does NOT have
	 * authorization to read the record including security check against collected data, using
	 * {@link SpiderAuthorizator#checkGetUsersMatchedRecordPartPermissionsForActionOnRecordTypeAndCollectedData(User, String, String, List, boolean)}
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
	 * @param dataRedactor
	 *            A DataRedactor to use when enhancing
	 * @return A newly created DataRecord constructed as discussed above
	 */
	DataRecord enhance(User user, String recordType, DataGroup dataGroup,
			DataRedactor dataRedactor);

	/**
	 * enhanceIgnoringReadAccess converts a DataGroup into a DataRecord. It is very similar to
	 * {@link #enhance(User, String, DataGroup, DataRedactor)} except that it will complete even if
	 * the User does not have read access to the enhanced record and not throw an exception. This
	 * method is intended to be used when enhancing the DataGroup as one of the last steps before
	 * returning the data to the user, during actions such as create where the user may only have
	 * permission to create but no permissions to read the affected data.
	 * <p>
	 * The conversion has a few major parts.
	 * <ol>
	 * <li>Create a new DataRecord, and add the DataGroup to it.</li>
	 * <li>Find out what actions the User is allowed to do for the record and add those actions to
	 * the DataRecord. This is a multistep process in itself.
	 * <ol>
	 * <li>Add standard actions for all recordTypes (possibly add read, update, delete, index,
	 * incomingLinks actions)</li>
	 * <li>If dataGroup beeing enhanced is a binary type, possibly add upload action</li>
	 * <li>If dataGroup beeing enhanced is a search type, possibly add add search action</li>
	 * <li>If dataGroup beeing enhanced is a recordType type, add recordType specific actions
	 * (create, list, validate, search)</li>
	 * </ol>
	 * </li>
	 * <li>Add the users read and write recordPartPermissions to the record</li>
	 * <li>Redact information the user is not allowed to read from the DataGroup, based on settings
	 * in metadata and the users currently active roles. If the user has no read access SHOULD all
	 * read protected data be redacted.</li>
	 * <li>Add read links to all linked records in the DataGroup if the User has read access to the
	 * linked record.</li>
	 * </ol>
	 * <p>
	 * Note that this method does not guarante that the User has read action to the data, and is
	 * therefor inteded to be used before returning data to the user during action such as create,
	 * where the user is providing data and not for actions where data is read from storage.
	 * 
	 * @param user
	 *            The User that will get the DataRecord
	 * @param recordType
	 *            A String with the records recordType, it must be the implementing recordType (not
	 *            the abstract parent type if the recordType has a parent)
	 * @param dataGroup
	 *            A DataGroup with data to turn into a DataRecord
	 * @param dataRedactor
	 *            A DataRedactor to use when enhancing
	 * @return A newly created DataRecord constructed as discussed above
	 */
	DataRecord enhanceIgnoringReadAccess(User user, String recordType, DataGroup dataGroup,
			DataRedactor dataRedactor);

}