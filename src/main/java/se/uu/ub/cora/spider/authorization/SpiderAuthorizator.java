/*
 * Copyright 2016, 2018, 2020 Uppsala University Library
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

package se.uu.ub.cora.spider.authorization;

import java.util.Set;

import se.uu.ub.cora.beefeater.authentication.User;
import se.uu.ub.cora.data.DataGroup;

public interface SpiderAuthorizator {

	/**
	 * userIsAuthorizedForActionOnRecordType is used to check if a user is allowed to perform the
	 * action on the specified recordType based on the rules the user is associated with.<br>
	 * <br>
	 * Implementations MUST ensure that the user is active in storage, and that the user through its
	 * stored active rules has access to the action on the recordType.
	 * 
	 * @param user
	 *            the logged in user (or guest)
	 * @param action
	 *            the action the user wants to perform, such as read, update, etc.
	 * @param recordType
	 *            the recordType the user wants to perform the action on
	 * @return a boolean, true if the user is allowed to perform the action on the specified
	 *         recordType else false
	 */
	boolean userIsAuthorizedForActionOnRecordType(User user, String action, String recordType);

	/**
	 * checkUserIsAuthorizedForActionOnRecordType MUST implement the same requirements as
	 * {@link #userIsAuthorizedForActionOnRecordType(User, String, String)} and if not authorized
	 * throw an {@link AuthorizationException}
	 * 
	 * @param user
	 * @param action
	 * @param recordType
	 */
	void checkUserIsAuthorizedForActionOnRecordType(User user, String action, String recordType);

	/**
	 * userIsAuthorizedForActionOnRecordTypeAndCollectedData MUST implement the same requirements as
	 * {@link #userIsAuthorizedForActionOnRecordType(User, String, String)} with the addition of a
	 * check that the users also has access to the collectedData through its associated rules.
	 * 
	 * @param user
	 * @param action
	 * @param recordType
	 * @param collectedData
	 *            the collectedData to use extend the access check with
	 * @return
	 */
	boolean userIsAuthorizedForActionOnRecordTypeAndCollectedData(User user, String action,
			String recordType, DataGroup collectedData);

	/**
	 * checkAndGetUserAuthorizationsForActionOnRecordTypeAndCollectedData MUST implement the same
	 * requirements as
	 * {@link #userIsAuthorizedForActionOnRecordTypeAndCollectedData(User, String, String)} and if
	 * not authorized throw an {@link AuthorizationException}
	 * <p>
	 * If the user is authorized and calculateRecordPartPermissions is specified to true, should a
	 * list of recordPart permissions collected from all the active rules the user has access to,
	 * that matches the collected data SHALL be returned. The returned list of recordPart
	 * permissions should be filtered so that it only contains those recordPart permissions that are
	 * for the specified action and the specified recordType.
	 * 
	 * @param user
	 * @param action
	 * @param recordType
	 * @param collectedData
	 * @param calculateRecordPartPermissions,
	 *            a boolean, if recordPartPermissions should be calculated
	 * @return A list of recordPart permissions
	 */
	Set<String> checkAndGetUserAuthorizationsForActionOnRecordTypeAndCollectedData(User user,
			String action, String recordType, DataGroup collectedData,
			boolean calculateRecordPartPermissions);

}
