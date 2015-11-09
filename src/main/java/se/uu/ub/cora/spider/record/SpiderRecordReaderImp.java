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

import java.util.Set;

import se.uu.ub.cora.beefeater.Authorizator;
import se.uu.ub.cora.bookkeeper.data.DataGroup;
import se.uu.ub.cora.spider.data.Action;
import se.uu.ub.cora.spider.data.SpiderDataElement;
import se.uu.ub.cora.spider.data.SpiderDataGroup;
import se.uu.ub.cora.spider.data.SpiderDataRecord;
import se.uu.ub.cora.spider.data.SpiderDataRecordLink;
import se.uu.ub.cora.spider.record.storage.RecordStorage;

public final class SpiderRecordReaderImp extends SpiderRecordHandler implements SpiderRecordReader {
	private Authorizator authorization;
	private PermissionKeyCalculator keyCalculator;
	private String userId;

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

		SpiderDataGroup spiderDataGroup = SpiderDataGroup.fromDataGroup(recordRead);
		return createDataRecordContainingDataGroup(spiderDataGroup);
	}

	private void checkRecordsRecordTypeNotAbstract() {
		DataGroup recordTypeDefinition = getRecordTypeDefinition();
		if (recordTypeIsAbstract(recordTypeDefinition)) {
			throw new MisuseException("Reading for record: " + recordId
					+ " on the abstract recordType:" + recordType + " is not allowed");
		}
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

		SpiderDataGroup links = SpiderDataGroup.fromDataGroup(linksPointingToRecord);
		addReadActionToIncomingLinks(links);
		return links;
	}

	private void addReadActionToIncomingLinks(SpiderDataGroup links) {
		for (SpiderDataElement spiderDataElement : links.getChildren()) {
			SpiderDataGroup spiderDataGroup = (SpiderDataGroup) spiderDataElement;
			SpiderDataRecordLink spiderRecordLink = (SpiderDataRecordLink) spiderDataGroup
					.getFirstChildWithNameInData("from");
			spiderRecordLink.addAction(Action.READ);
		}
	}
}
