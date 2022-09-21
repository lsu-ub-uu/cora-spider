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

import java.util.List;
import java.util.Set;

import se.uu.ub.cora.beefeater.authentication.User;
import se.uu.ub.cora.data.DataGroup;
import se.uu.ub.cora.data.collected.PermissionTerm;

public interface SpiderAuthorizator {

	/**
	 * userIsAuthorizedForActionOnRecordType is used to check if a user is allowed to perform the
	 * action on the specified recordType based on the rules the user is associated with.
	 * <p>
	 * Implementations MUST ensure that the user is active in storage, and that the user through its
	 * stored active rules has access to the action on the recordType.
	 * <p>
	 * This method is very similar to
	 * {@link #checkUserIsAuthorizedForActionOnRecordType(User, String, String)} with the main
	 * difference beeing that this one returns a boolean instead of throwing an excepiton.
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
	 * checkUserIsAuthorizedForActionOnRecordType is used to check if a user is allowed to perform
	 * the action on the specified recordType based on the rules the user is associated with.
	 * <p>
	 * Implementations MUST ensure that the user is active in storage, and that the user through its
	 * stored active rules has access to the action on the recordType.
	 * <p>
	 * If the user is not authorized MUST implementations throw an {@link AuthorizationException}
	 * <p>
	 * This method is very similar to
	 * {@link #userIsAuthorizedForActionOnRecordType(User, String, String)} with the main difference
	 * beeing that this one throws an excepiton instead of returning a boolean.
	 * 
	 * @param user
	 *            the logged in user (or guest)
	 * @param action
	 *            the action the user wants to perform, such as read, update, etc.
	 * @param recordType
	 *            the recordType the user wants to perform the action on
	 */
	void checkUserIsAuthorizedForActionOnRecordType(User user, String action, String recordType);

	/**
	 * userIsAuthorizedForActionOnRecordTypeAndCollectedData is used to check if a user is allowed
	 * to perform the action on the specified recordType based on the rules the user is associated
	 * with. In addition should implementations also check that the users also has access to the
	 * collectedData through its associated rules.
	 * <p>
	 * Implementations MUST ensure that the user is active in storage, and that the user through its
	 * stored active rules has access to the action and collected data on the recordType.
	 * <p>
	 * This method is very similar to
	 * {@link #checkUserIsAuthorizedForActionOnRecordTypeAndCollectedData(User, String, String, List)}
	 * with the main difference beeing that this one returns a boolean instead of throwing an
	 * excepiton.
	 * 
	 * @param user
	 *            the logged in user (or guest)
	 * @param action
	 *            the action the user wants to perform, such as read, update, etc.
	 * @param recordType
	 *            the recordType the user wants to perform the action on
	 * @param permissionTerms
	 *            the collectedData to use extend the access check with
	 * @return a boolean, true if the user is allowed to perform the action on the specified
	 *         recordType else false
	 */
	boolean userIsAuthorizedForActionOnRecordTypeAndCollectedData(User user, String action,
			String recordType, List<PermissionTerm> permissionTerms);

	/**
	 * checkUserIsAuthorizedForActionOnRecordTypeAndCollectedData is used to check if a user is
	 * allowed to perform the action on the specified recordType based on the rules the user is
	 * associated with. In addition should implementations also check that the users also has access
	 * to the collectedData through its associated rules.
	 * <p>
	 * Implementations MUST ensure that the user is active in storage, and that the user through its
	 * stored active rules has access to the action and collected data on the recordType.
	 * <p>
	 * If the user is not authorized MUST implementations throw an {@link AuthorizationException}
	 * <p>
	 * This method is very similar to
	 * {@link #userIsAuthorizedForActionOnRecordTypeAndCollectedData(User, String, String, List)}
	 * with the main difference beeing that this one throws an excepiton instead of returning a
	 * boolean.
	 * 
	 * @param user
	 *            the logged in user (or guest)
	 * @param action
	 *            the action the user wants to perform, such as read, update, etc.
	 * @param recordType
	 *            the recordType the user wants to perform the action on
	 * @param permissionTerms
	 *            the collectedData to use extend the access check with
	 */
	void checkUserIsAuthorizedForActionOnRecordTypeAndCollectedData(User user, String action,
			String recordType, List<PermissionTerm> permissionTerms);

	/**
	 * checkGetUsersMatchedRecordPartPermissionsForActionOnRecordTypeAndCollectedData is used to get
	 * all recordPartPermissions that come from matched rules. RecordPart permissions are collected
	 * for all rules that allow the user to perform the action on the specified recordType based on
	 * the rules and the collectedData, the user is associated with.
	 * <p>
	 * Implementations MUST ensure that the user is active in storage, and collect only the
	 * recordPart permissions that the user through its stored active rules has access to based on
	 * the action and collected data for the recordType.
	 * <p>
	 * The returned set of recordPart permissions should be filtered so that it only contains those
	 * recordPart permissions that are for the specified action and the specified recordType. If no
	 * recordPart permissons are to be returned should and emtpy set be retured instead.
	 * <p>
	 * For a read action SHALL the matched rules read recordPart permissions be returned, and for
	 * all other actions shall the rules write recordPart permissions be returned.
	 * <p>
	 * If the user is not authorized MUST implementations throw an {@link AuthorizationException}
	 * <p>
	 * This method is very similar to
	 * {@link #getUsersMatchedRecordPartPermissionsForActionOnRecordTypeAndCollectedData(User, String, String, DataGroup)}
	 * with the main difference beeing that this one throws an excepiton instead of returning an
	 * empty set if the User is not authorized as specified above.
	 * 
	 * @param user
	 *            the logged in user (or guest)
	 * 
	 * @param action
	 *            the action the user wants to perform, such as read, update, etc.
	 * @param recordType
	 *            the recordType the user wants to perform the action on
	 * @param permissionTerms
	 *            the collectedData to use extend the access check with
	 * @param calculateRecordPartPermissions,
	 *            a boolean, if recordPartPermissions should be calculated
	 * @return A set of recordPart permissions
	 */
	Set<String> checkGetUsersMatchedRecordPartPermissionsForActionOnRecordTypeAndCollectedData(
			User user, String action, String recordType, List<PermissionTerm> permissionTerms,
			boolean calculateRecordPartPermissions);

}
