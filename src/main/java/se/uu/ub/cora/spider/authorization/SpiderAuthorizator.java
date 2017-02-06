/*
 * Copyright 2016 Uppsala University Library
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
import java.util.Map;
import java.util.Set;

import se.uu.ub.cora.beefeater.authentication.User;
import se.uu.ub.cora.bookkeeper.data.DataGroup;

public interface SpiderAuthorizator {
	boolean userSatisfiesRequiredRules(User user, List<Map<String, Set<String>>> requiredRules);

	void checkUserIsAuthorizedForActionOnRecordType(User user, String action, String recordType);

	void checkUserIsAuthorizedForActionOnRecordTypeAndRecord(User user, String action,
			String recordType, DataGroup record);

	boolean userIsAuthorizedForActionOnRecordTypeAndRecord(User user, String action,
			String recordType, DataGroup record);

	boolean userIsAuthorizedForActionOnRecordType(User user, String action, String recordType);

}
