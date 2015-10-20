/*
 * Copyright 2015 Uppsala University Library
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
import se.uu.ub.cora.metadataformat.data.DataGroup;
import se.uu.ub.cora.spider.data.Action;
import se.uu.ub.cora.spider.data.SpiderDataGroup;
import se.uu.ub.cora.spider.data.SpiderDataRecord;
import se.uu.ub.cora.spider.data.SpiderRecordList;
import se.uu.ub.cora.spider.record.storage.RecordStorage;

public final class SpiderRecordReaderImp implements SpiderRecordReader {
	private static final String RECORD_TYPE = "recordType";
	private Authorizator authorization;
	private RecordStorage recordStorage;
	private PermissionKeyCalculator keyCalculator;
	private SpiderRecordList readRecordList;
	private String userId;
	private String recordType;
	private String recordId;

	public static SpiderRecordReaderImp usingAuthorizationAndRecordStorageAndKeyCalculator(
			Authorizator authorization, RecordStorage recordStorage,
			PermissionKeyCalculator keyCalculator) {
		return new SpiderRecordReaderImp(authorization, recordStorage, keyCalculator);
	}

	private SpiderRecordReaderImp(Authorizator authorization, RecordStorage recordStorage,
			PermissionKeyCalculator keyCalculator) {
		this.authorization = authorization;
		this.recordStorage = recordStorage;
		this.keyCalculator = keyCalculator;
	}

	@Override
	public SpiderDataRecord readRecord(String userId, String recordType, String recordId) {
		this.userId = userId;
		this.recordType = recordType;
		this.recordId = recordId;
		checkRecordsRecordTypeNotAbstract();
		DataGroup recordRead = recordStorage.read(recordType, recordId);

		checkUserIsAuthorisedToReadData(recordRead);

		// filter data
		// TODO: filter hidden data if user does not have right to see it

		return convertToSpiderDataRecord(recordRead);
	}

	private void checkRecordsRecordTypeNotAbstract() {
		DataGroup recordTypeDataGroup = getRecordType(recordType);
		if (recordTypeIsAbstract(recordTypeDataGroup)) {
			throw new MisuseException("Reading for record: " + recordId
					+ " on the abstract recordType:" + recordType + " is not allowed");
		}
	}

	private DataGroup getRecordType(String recordType) {
		return recordStorage.read(RECORD_TYPE, recordType);
	}

	private boolean recordTypeIsAbstract(DataGroup recordTypeDataGroup) {
		return "true".equals(recordTypeDataGroup.getFirstAtomicValueWithNameInData("abstract"));
	}

	private void checkUserIsAuthorisedToReadData(DataGroup recordRead) {
		if (isNotAuthorizedToRead(recordRead)) {
			throw new AuthorizationException(
					"User:" + userId + " is not authorized to read for record:" + recordId
							+ " of type:" + recordType);
		}
	}

	private boolean isNotAuthorizedToRead(DataGroup recordRead) {
		String accessType = "READ";
		Set<String> recordCalculateKeys = keyCalculator.calculateKeys(accessType, recordType,
				recordRead);
		return !authorization.isAuthorized(userId, recordCalculateKeys);
	}

	private SpiderDataRecord convertToSpiderDataRecord(DataGroup dataGroup) {
		SpiderDataGroup spiderDataGroup = SpiderDataGroup.fromDataGroup(dataGroup);
		SpiderDataRecord spiderDataRecord = SpiderDataRecord.withSpiderDataGroup(spiderDataGroup);
		addLinks(spiderDataRecord);
		return spiderDataRecord;
	}

	private void addLinks(SpiderDataRecord spiderDataRecord) {
		spiderDataRecord.addAction(Action.READ);
		spiderDataRecord.addAction(Action.UPDATE);
		spiderDataRecord.addAction(Action.DELETE);
		spiderDataRecord.addAction(Action.READ_INCOMING_LINKS);
	}

	@Override
	public SpiderDataGroup readIncomingLinks(String userId, String recordType, String recordId) {
		this.userId = userId;
		this.recordType = recordType;
		this.recordId = recordId;
		checkRecordsRecordTypeNotAbstract();
		DataGroup recordRead = recordStorage.read(recordType, recordId);

		checkUserIsAuthorisedToReadData(recordRead);

		DataGroup linksPointingToRecord = recordStorage
				.generateLinkCollectionPointingToRecord(recordType, recordId);

		return SpiderDataGroup.fromDataGroup(linksPointingToRecord);
	}

	// TODO: move readRecordList to a class of its own
	@Override
	public SpiderRecordList readRecordList(String userId, String recordType) {

		checkUserIsAuthorizedToReadListRecordType(userId, recordType);
		readRecordList = SpiderRecordList.withContainRecordsOfType(recordType);

		readRecordsOfType(recordType);
		setFromToInReadRecordList();

		return readRecordList;
	}

	private void checkUserIsAuthorizedToReadListRecordType(String userId, String recordType) {
		// calculate permissionKey
		String accessType = "READ";
		Set<String> recordCalculateKeys = keyCalculator.calculateKeysForList(accessType,
				recordType);
		if (!authorization.isAuthorized(userId, recordCalculateKeys)) {
			throw new AuthorizationException("User:" + userId + " is not authorized to read records"
					+ "of type:" + recordType);
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
		// find child recordTypes
		Collection<DataGroup> recordTypes = recordStorage.readList(RECORD_TYPE);

		for (DataGroup recordTypePossibleChild : recordTypes) {
			if (isChildOfAbstractRecordType(abstractRecordType, recordTypePossibleChild)) {
				addChildToReadRecordList(recordTypePossibleChild);
			}
		}
	}

	private boolean isChildOfAbstractRecordType(String abstractRecordType,
			DataGroup recordTypePossibleChild) {
		String parentId = "parentId";
		if (recordTypePossibleChild.containsChildWithNameInData(parentId)) {
			String parentIdValue = recordTypePossibleChild
					.getFirstAtomicValueWithNameInData(parentId);
			if (parentIdValue.equals(abstractRecordType)) {
				return true;
			}
		}
		return false;
	}

	private void addChildToReadRecordList(DataGroup recordTypePossibleChild) {
		// get this recordTypes data from storage
		String childRecordType = recordTypePossibleChild.getFirstGroupWithNameInData("recordInfo")
				.getFirstAtomicValueWithNameInData("id");
		readRecordsOfSpecifiedRecordTypeAndAddToReadRecordList(childRecordType);
	}

	private void readRecordsOfSpecifiedRecordTypeAndAddToReadRecordList(String recordType) {
		Collection<DataGroup> dataGroupList = recordStorage.readList(recordType);
		for (DataGroup dataGroup : dataGroupList) {
			SpiderDataRecord spiderDataRecord = convertToSpiderDataRecord(dataGroup);
			readRecordList.addRecord(spiderDataRecord);
		}
	}

	private void setFromToInReadRecordList() {
		readRecordList.setTotalNo(String.valueOf(readRecordList.getRecords().size()));
		readRecordList.setFromNo("0");
		readRecordList.setToNo(String.valueOf(readRecordList.getRecords().size() - 1));
	}
}
