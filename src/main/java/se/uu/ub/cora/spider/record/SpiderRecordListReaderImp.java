/*
 * Copyright 2015, 2016 Uppsala University Library
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

import java.util.Collection;
import java.util.Set;

import se.uu.ub.cora.beefeater.Authorizator;
import se.uu.ub.cora.beefeater.authentication.User;
import se.uu.ub.cora.bookkeeper.data.DataGroup;
import se.uu.ub.cora.spider.authentication.Authenticator;
import se.uu.ub.cora.spider.data.SpiderDataGroup;
import se.uu.ub.cora.spider.data.SpiderDataList;
import se.uu.ub.cora.spider.data.SpiderDataRecord;
import se.uu.ub.cora.spider.dependency.SpiderDependencyProvider;

public class SpiderRecordListReaderImp extends SpiderRecordHandler
		implements SpiderRecordListReader {
	private Authenticator authenticator;
	private Authorizator authorization;
	private PermissionKeyCalculator keyCalculator;
	private SpiderDataList readRecordList;
	private String authToken;
	private User user;

	public SpiderRecordListReaderImp(SpiderDependencyProvider dependencyProvider) {
		this.authenticator = dependencyProvider.getAuthenticator();
		this.authorization = dependencyProvider.getAuthorizator();
		this.recordStorage = dependencyProvider.getRecordStorage();
		this.keyCalculator = dependencyProvider.getPermissionKeyCalculator();
	}

	public static SpiderRecordListReaderImp usingDependencyProvider(
			SpiderDependencyProvider dependencyProvider) {
		return new SpiderRecordListReaderImp(dependencyProvider);
	}

	@Override
	public SpiderDataList readRecordList(String authToken, String recordType) {

		this.authToken = authToken;
		tryToGetActiveUser();
		checkUserIsAuthorizedToReadListRecordType(recordType);
		readRecordList = SpiderDataList.withContainDataOfType(recordType);

		readRecordsOfType(recordType);
		setFromToInReadRecordList();

		return readRecordList;
	}

	private void tryToGetActiveUser() {
		user = authenticator.tryToGetActiveUser(authToken);
	}

	private void checkUserIsAuthorizedToReadListRecordType(String recordType) {
		String accessType = "READ";
		Set<String> recordCalculateKeys = keyCalculator.calculateKeysForList(accessType,
				recordType);
		if (!authorization.isAuthorized(user, recordCalculateKeys)) {
			throw new AuthorizationException("User:" + user.id
					+ " is not authorized to read records" + "of type:" + recordType);
		}
	}

	private void readRecordsOfType(String recordType) {
		if (recordTypeIsAbstract(recordType)) {
			addChildrenOfAbstractTypeToReadRecordList(recordType);
		} else {
			readRecordsOfSpecifiedRecordTypeAndAddToReadRecordList(recordType);
		}
	}

	private boolean recordTypeIsAbstract(String recordType) {
		DataGroup recordTypeDataGroup = recordStorage.read(RECORD_TYPE, recordType);
		String abstractString = recordTypeDataGroup.getFirstAtomicValueWithNameInData("abstract");
		return "true".equals(abstractString);
	}

	private void addChildrenOfAbstractTypeToReadRecordList(String abstractRecordType) {
		Collection<DataGroup> recordTypes = recordStorage.readList(RECORD_TYPE);

		for (DataGroup recordTypePossibleChild : recordTypes) {
			if (isChildOfAbstractRecordType(abstractRecordType, recordTypePossibleChild)) {
				addChildToReadRecordList(recordTypePossibleChild);
			}
		}
	}

	private void addChildToReadRecordList(DataGroup recordTypePossibleChild) {
		String childRecordType = recordTypePossibleChild.getFirstGroupWithNameInData("recordInfo")
				.getFirstAtomicValueWithNameInData("id");
		if (recordStorage.recordsExistForRecordType(childRecordType)) {
			readRecordsOfSpecifiedRecordTypeAndAddToReadRecordList(childRecordType);
		}
	}

	private void readRecordsOfSpecifiedRecordTypeAndAddToReadRecordList(String recordType) {
		Collection<DataGroup> dataGroupList = recordStorage.readList(recordType);
		this.recordType = recordType;
		for (DataGroup dataGroup : dataGroupList) {
			SpiderDataGroup spiderDataGroup = SpiderDataGroup.fromDataGroup(dataGroup);
			SpiderDataRecord spiderDataRecord = createDataRecordContainingDataGroup(
					spiderDataGroup);
			readRecordList.addData(spiderDataRecord);
		}
	}

	private void setFromToInReadRecordList() {
		readRecordList.setTotalNo(String.valueOf(readRecordList.getDataList().size()));
		readRecordList.setFromNo("1");
		readRecordList.setToNo(String.valueOf(readRecordList.getDataList().size()));
	}
}
