/*
 * Copyright 2016, 2017 Uppsala University Library
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

import se.uu.ub.cora.beefeater.authentication.User;
import se.uu.ub.cora.bookkeeper.data.DataGroup;
import se.uu.ub.cora.spider.data.Action;
import se.uu.ub.cora.spider.data.SpiderDataElement;
import se.uu.ub.cora.spider.data.SpiderDataGroup;
import se.uu.ub.cora.spider.data.SpiderDataLink;
import se.uu.ub.cora.spider.data.SpiderDataRecord;
import se.uu.ub.cora.spider.data.SpiderDataRecordLink;
import se.uu.ub.cora.spider.data.SpiderDataResourceLink;
import se.uu.ub.cora.spider.dependency.SpiderDependencyProvider;

public class DataGroupToRecordEnhancerImp implements DataGroupToRecordEnhancer {

	private static final String RECORD_TYPE = "recordType";
	private static final String PARENT_ID = "parentId";
	private DataGroup dataGroup;
	private SpiderDataRecord record;
	private SpiderDependencyProvider dependencyProvider;
	private User user;
	private String recordType;
	private String handledRecordId;

	public DataGroupToRecordEnhancerImp(SpiderDependencyProvider dependencyProvider) {
		this.dependencyProvider = dependencyProvider;
	}

	@Override
	public SpiderDataRecord enhance(User user, String recordType, DataGroup dataGroup) {
		this.user = user;
		this.recordType = recordType;
		this.dataGroup = dataGroup;
		SpiderDataGroup spiderDataGroup = SpiderDataGroup.fromDataGroup(dataGroup);
		record = SpiderDataRecord.withSpiderDataGroup(spiderDataGroup);
		handledRecordId = getRecordIdFromDataRecord(record);
		addActions();
		addReadActionToDataRecordLinks(spiderDataGroup);
		return record;
	}

	private String getRecordIdFromDataRecord(SpiderDataRecord spiderDataRecord) {
		SpiderDataGroup topLevelDataGroup = spiderDataRecord.getSpiderDataGroup();
		SpiderDataGroup recordInfo = topLevelDataGroup.extractGroup("recordInfo");
		return recordInfo.extractAtomicValue("id");
	}

	protected void addActions() {
		if (userIsAuthorizedForAction("read")) {
			record.addAction(Action.READ);
		}
		if (userIsAuthorizedForAction("update")) {
			record.addAction(Action.UPDATE);
		}
		possiblyAddDeleteAction(record);
		possiblyAddIncomingLinksAction(record);
		possiblyAddUploadAction(record);
		possiblyAddSearchAction(record);
		addActionsForRecordType(record);
	}

	private void possiblyAddDeleteAction(SpiderDataRecord spiderDataRecord) {
		if (!incomingLinksExistsForRecord(spiderDataRecord)
				&& userIsAuthorizedForAction("delete")) {
			spiderDataRecord.addAction(Action.DELETE);
		}
	}

	private boolean incomingLinksExistsForRecord(SpiderDataRecord spiderDataRecord) {
		SpiderDataGroup topLevelDataGroup = spiderDataRecord.getSpiderDataGroup();
		SpiderDataGroup recordInfo = topLevelDataGroup.extractGroup("recordInfo");
		String recordTypeForThisRecord = recordInfo.extractAtomicValue("type");
		return dependencyProvider.getRecordStorage().linksExistForRecord(recordTypeForThisRecord,
				handledRecordId);
	}

	private boolean userIsAuthorizedForAction(String action) {
		return dependencyProvider.getSpiderAuthorizator()
				.userIsAuthorizedForActionOnRecordTypeAndRecord(user, action, recordType,
						dataGroup);
	}

	private void possiblyAddIncomingLinksAction(SpiderDataRecord spiderDataRecord) {
		if (incomingLinksExistsForRecord(spiderDataRecord)) {
			spiderDataRecord.addAction(Action.READ_INCOMING_LINKS);
		}
	}

	private void possiblyAddUploadAction(SpiderDataRecord spiderDataRecord) {
		if (isHandledRecordIdChildOfBinary(recordType) && userIsAuthorizedForAction("upload")) {
			spiderDataRecord.addAction(Action.UPLOAD);
		}
	}

	private boolean isHandledRecordIdChildOfBinary(String dataRecordRecordId) {
		String refParentId = extractParentId(dataRecordRecordId);
		return "binary".equals(refParentId);
	}

	private String extractParentId(String dataRecordRecordId) {
		DataGroup handledRecordTypeDataGroup = dependencyProvider.getRecordStorage()
				.read(RECORD_TYPE, dataRecordRecordId);
		if (handledRecordHasParent(handledRecordTypeDataGroup)) {
			DataGroup parentGroup = handledRecordTypeDataGroup
					.getFirstGroupWithNameInData(PARENT_ID);
			return parentGroup.getFirstAtomicValueWithNameInData("linkedRecordId");
		}
		return "";
	}

	private boolean handledRecordHasParent(DataGroup handledRecordTypeDataGroup) {
		return handledRecordTypeDataGroup.containsChildWithNameInData(PARENT_ID);
	}

	private void addActionsForRecordType(SpiderDataRecord spiderDataRecord) {
		if (isRecordType()) {
			possiblyAddCreateAction(spiderDataRecord);

			possiblyAddListAction(spiderDataRecord);
		}
	}

	private boolean isRecordType() {
		return recordType.equals(RECORD_TYPE);
	}

	private void possiblyAddCreateAction(SpiderDataRecord spiderDataRecord) {
		if (!isHandledRecordIdOfTypeAbstract(handledRecordId)
				&& userIsAuthorizedForActionOnRecordType("create", handledRecordId)) {
			spiderDataRecord.addAction(Action.CREATE);
		}
	}

	private boolean userIsAuthorizedForActionOnRecordType(String action, String recordType) {
		return dependencyProvider.getSpiderAuthorizator()
				.userIsAuthorizedForActionOnRecordTypeAndRecord(user, action, recordType,
						dataGroup);
	}

	private void possiblyAddListAction(SpiderDataRecord spiderDataRecord) {
		if (userIsAuthorizedForActionOnRecordType("list", handledRecordId)) {

			spiderDataRecord.addAction(Action.LIST);
		}
	}

	private void possiblyAddSearchAction(SpiderDataRecord spiderDataRecord) {
		if (isRecordTypeSearch()
				&& userIsAuthorizedForActionOnRecordType("search", handledRecordId)) {
			spiderDataRecord.addAction(Action.SEARCH);
		}
	}

	private boolean isRecordTypeSearch() {
		return "search".equals(recordType);
	}

	private boolean isHandledRecordIdOfTypeAbstract(String recordId) {
		String abstractInRecordTypeDefinition = getAbstractFromHandledRecord(recordId);
		return "true".equals(abstractInRecordTypeDefinition);
	}

	private String getAbstractFromHandledRecord(String recordId) {
		DataGroup handleRecordTypeDataGroup = dependencyProvider.getRecordStorage()
				.read(RECORD_TYPE, recordId);
		return handleRecordTypeDataGroup.getFirstAtomicValueWithNameInData("abstract");
	}

	private void addReadActionToDataRecordLinks(SpiderDataGroup spiderDataGroup) {
		for (SpiderDataElement spiderDataChild : spiderDataGroup.getChildren()) {
			addReadActionToDataRecordLink(spiderDataChild);
		}
	}

	private void addReadActionToDataRecordLink(SpiderDataElement spiderDataChild) {
		if (isLink(spiderDataChild)) {
			((SpiderDataLink) spiderDataChild).addAction(Action.READ);
		}
		if (isGroup(spiderDataChild)) {
			addReadActionToDataRecordLinks((SpiderDataGroup) spiderDataChild);
		}
	}

	private boolean isLink(SpiderDataElement spiderDataChild) {
		return spiderDataChild instanceof SpiderDataRecordLink
				|| spiderDataChild instanceof SpiderDataResourceLink;
	}

	private boolean isGroup(SpiderDataElement spiderDataChild) {
		return spiderDataChild instanceof SpiderDataGroup;
	}

}
