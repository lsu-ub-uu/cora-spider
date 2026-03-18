/*
 * Copyright 2026 Uppsala University Library
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

package se.uu.ub.cora.spider.record.internal;

import se.uu.ub.cora.beefeater.authentication.User;
import se.uu.ub.cora.data.DataRecord;
import se.uu.ub.cora.data.DataRecordLink;

/**
 * LinkAuthorizator checks if a {@link User} has READ access to a linked {@link DataRecord}.
 * 
 */
public interface LinkAuthorizator {
	/**
	 * isAuthorizedToReadLink checks if the user has READ access to the record linked through the
	 * dataLink.
	 * </p>
	 * for READ on a recordLink ex. binary, alvin-record, diva-output</br>
	 * </br>
	 * recordTypeIsPublic --> possible yes</br>
	 * recordIsPublished --> possible yes</br>
	 * recordTypeUsesHostRecord --> *1</br>
	 * permissionUnit --> possible no (if no match permissionUnit)</br>
	 * roles rules (recordtype+colletterms+action)</br>
	 * </br>
	 * *1</br>
	 * recordTypeIsPublic (ignore)</br>
	 * recordIsPublished (ignore)</br>
	 * recordTypeUsesHostRecord (ignore for now)</br>
	 * permissionUnit --> possible no (if no match permissionUnit)</br>
	 * roles rules (READ, system.alvin-record.binary, collectTerms)</br>
	 * 
	 * @param user
	 *            a {@link User} to check if it has READ access
	 * @param dataRecordLink
	 *            A {@link DataRecordLink} to the record to check access for
	 * @return A boolean true if the User has READ access else false
	 */
	boolean isAuthorizedToReadRecordLink(User user, DataRecordLink dataRecordLink);
}
