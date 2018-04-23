/*
 * Copyright 2016 Olov McKie
 * Copyright 2017, 2018 Uppsala University Library
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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import se.uu.ub.cora.beefeater.authentication.User;
import se.uu.ub.cora.bookkeeper.data.DataGroup;

public class AlwaysAuthorisedExceptStub implements SpiderAuthorizator {
	public Map<String, Set<String>> notAuthorizedForRecordTypeAndActions = new HashMap<>();
	public List<String> notAuthorizedForIds = new ArrayList<>();

	@Override
	public void checkUserIsAuthorizedForActionOnRecordType(User user, String action,
			String recordType) {
		if (notAuthorizedForRecordTypeAndAction(recordType, action)) {
			throw new AuthorizationException("not authorized");
		}
	}

	@Override
	public void checkUserIsAuthorizedForActionOnRecordTypeAndRecord(User user, String action,
			String recordType, DataGroup record) {
		if (notAuthorizedForRecordTypeAndAction(recordType, action)) {
			throw new AuthorizationException("not authorized");
		}

	}

	private boolean notAuthorizedForRecordTypeAndAction(String recordType, String action) {
		return notAuthorizedForRecordTypeAndActions.containsKey(recordType)
				&& notAuthorizedForRecordTypeAndActions.get(recordType).contains(action);
	}

	@Override
	public boolean userIsAuthorizedForActionOnRecordTypeAndRecord(User user, String action,
			String recordType, DataGroup record) {
		if (notAuthorizedForRecordTypeAndAction(recordType, action)
				|| notAuthorizedForRecordId(record)) {
			return false;
		}
		return true;
	}

	private boolean notAuthorizedForRecordId(DataGroup record) {
		String recordId = record.getFirstGroupWithNameInData("recordInfo")
				.getFirstAtomicValueWithNameInData("id");
		return notAuthorizedForIds.contains(recordId);
	}

	@Override
	public boolean userIsAuthorizedForActionOnRecordType(User user, String action,
			String recordType) {
		if (notAuthorizedForRecordTypeAndAction(recordType, action)) {
			return false;
		}
		return true;
	}

	@Override
	public void checkUserIsAuthorizedForActionOnRecordTypeAndCollectedData(User user, String action,
			String string, DataGroup collectedData) {
		// TODO Auto-generated method stub

	}

	@Override
	public boolean userIsAuthorizedForActionOnRecordTypeAndCollectedData(User user, String action,
			String string, DataGroup collectedData) {
		// TODO Auto-generated method stub
		return false;
	}

}
