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

import se.uu.ub.cora.bookkeeper.data.DataGroup;
import se.uu.ub.cora.spider.data.Action;
import se.uu.ub.cora.spider.data.SpiderDataElement;
import se.uu.ub.cora.spider.data.SpiderDataGroup;
import se.uu.ub.cora.spider.data.SpiderDataRecord;
import se.uu.ub.cora.spider.data.SpiderDataRecordLink;
import se.uu.ub.cora.spider.record.storage.RecordStorage;

public class SpiderRecordHandler {
	private static final String RECORD_TYPE = "recordType";
	protected RecordStorage recordStorage;
	protected String recordType;
	protected String recordId;

	protected DataGroup getRecordTypeDefinition() {
		return recordStorage.read(RECORD_TYPE, recordType);
	}

	protected boolean isRecordTypeAbstract() {
		DataGroup recordTypeDefinition = getRecordTypeDefinition();
		String abstractInRecordTypeDefinition = recordTypeDefinition.
				getFirstAtomicValueWithNameInData("abstract");
		return "true".equals(abstractInRecordTypeDefinition);
	}


	protected void addReadActionToDataRecordLinks(SpiderDataGroup spiderDataGroup) {
		for (SpiderDataElement spiderDataChild : spiderDataGroup.getChildren()) {
			addReadActionToDataRecordLink(spiderDataChild);
		}
	}

	private void addReadActionToDataRecordLink(SpiderDataElement spiderDataChild) {
		if (isLink(spiderDataChild)) {
			((SpiderDataRecordLink) spiderDataChild).addAction(Action.READ);
		}
		if (isGroup(spiderDataChild)) {
			addReadActionToDataRecordLinks((SpiderDataGroup) spiderDataChild);
		}
	}

	private boolean isLink(SpiderDataElement spiderDataChild) {
		return spiderDataChild instanceof SpiderDataRecordLink;
	}

	private boolean isGroup(SpiderDataElement spiderDataChild) {
		return spiderDataChild instanceof SpiderDataGroup;
	}

	protected SpiderDataRecord createDataRecordContainingDataGroup(
			SpiderDataGroup spiderDataGroup) {
		addReadActionToDataRecordLinks(spiderDataGroup);
		SpiderDataRecord spiderDataRecord = SpiderDataRecord.withSpiderDataGroup(spiderDataGroup);
		addActions(spiderDataRecord);
		return spiderDataRecord;
	}

	protected void addActions(SpiderDataRecord spiderDataRecord) {
		spiderDataRecord.addAction(Action.READ);
		spiderDataRecord.addAction(Action.UPDATE);
		spiderDataRecord.addAction(Action.DELETE);
		if (incomingLinksExistsForRecord(spiderDataRecord)) {
			spiderDataRecord.addAction(Action.READ_INCOMING_LINKS);
		}
		addActionsForRecordType(spiderDataRecord);
	}

	private void addActionsForRecordType(SpiderDataRecord spiderDataRecord) {
		if (isRecordType()) {
			possiblyAddCreateAction(spiderDataRecord);

			spiderDataRecord.addAction(Action.LIST);
			spiderDataRecord.addAction(Action.SEARCH);
		}
	}

	private void possiblyAddCreateAction(SpiderDataRecord spiderDataRecord) {
		String recordId = getRecordIdFromDataRecord(spiderDataRecord);
		if(!isHandledRecordIdOfTypeAbstract(recordId)) {
            spiderDataRecord.addAction(Action.CREATE);
        }
	}

	protected boolean isRecordType() {
		return recordType.equals(RECORD_TYPE);
	}

	private String getRecordIdFromDataRecord(SpiderDataRecord spiderDataRecord) {
		SpiderDataGroup spiderDataGroup = spiderDataRecord.getSpiderDataGroup();
		SpiderDataGroup recordInfo = (SpiderDataGroup)spiderDataGroup.getFirstChildWithNameInData("recordInfo");
		return recordInfo.extractAtomicValue("id");
	}

	private boolean isHandledRecordIdOfTypeAbstract(String recordId){
		DataGroup handleRecordTypeDataGroup = recordStorage.read(RECORD_TYPE, recordId);
		String abstractInRecordTypeDefinition = handleRecordTypeDataGroup.
				getFirstAtomicValueWithNameInData("abstract");
		return "true".equals(abstractInRecordTypeDefinition);
	}


	protected boolean incomingLinksExistsForRecord(SpiderDataRecord spiderDataRecord) {
		SpiderDataGroup spiderDataGroup = spiderDataRecord.getSpiderDataGroup();
		SpiderDataGroup recordInfo = spiderDataGroup.extractGroup("recordInfo");
		String recordTypeForThisRecord = recordInfo.extractAtomicValue("type");
		String recordIdForThisRecord = recordInfo.extractAtomicValue("id");
		return recordStorage.linksExistForRecord(recordTypeForThisRecord, recordIdForThisRecord);
	}
}
