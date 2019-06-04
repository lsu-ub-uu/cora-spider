/*
 * Copyright 2015, 2019 Uppsala University Library
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

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import se.uu.ub.cora.bookkeeper.data.DataGroup;
import se.uu.ub.cora.spider.testdata.DataCreator;
import se.uu.ub.cora.spider.testdata.RecordLinkTestsDataCreator;
import se.uu.ub.cora.storage.RecordStorage;
import se.uu.ub.cora.storage.SpiderReadResult;

public class RecordLinkTestsRecordStorage implements RecordStorage {

	public boolean recordIdExistsForRecordType = true;
	public boolean createWasRead = false;
	public String type;
	public String id;

	@Override
	public DataGroup read(String type, String id) {
		if (type.equals("recordType")) {
			if ("validationOrder".equals(id)) {
				return DataCreator.createRecordTypeWithIdAndUserSuppliedIdAndAbstractAndPublicRead(
						id, "false", "false", "false");
			}
			return DataCreator.createRecordTypeWithIdAndUserSuppliedIdAndAbstractAndPublicRead(
					"dataWithLinks", "false", "false", "false");
		}
		if (type.equals("dataWithLinks")) {
			if (id.equals("oneLinkTopLevel")) {
				return RecordLinkTestsDataCreator.createSpiderDataGroupWithRecordInfoAndLink()
						.toDataGroup();
			}
			if (id.equals("oneLinkOneLevelDown")) {
				return RecordLinkTestsDataCreator.createDataGroupWithRecordInfoAndLinkOneLevelDown()
						.toDataGroup();
			}
		}
		if (type.equals("toRecordType")) {
			if (id.equals("recordLinkNotAuthorized")) {
				return RecordLinkTestsDataCreator.createLinkChildAsRecordDataGroup().toDataGroup();
			}
		}
		return null;
	}

	@Override
	public void create(String type, String id, DataGroup record, DataGroup collectedTerms,
			DataGroup linkList, String dataDivider) {
		createWasRead = true;

	}

	@Override
	public void deleteByTypeAndId(String type, String id) {
		// TODO Auto-generated method stub

	}

	@Override
	public boolean linksExistForRecord(String type, String id) {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public void update(String type, String id, DataGroup record, DataGroup collectedTerms,
			DataGroup linkList, String dataDivider) {
		// TODO Auto-generated method stub

	}

	@Override
	public SpiderReadResult readList(String type, DataGroup filter) {
		List<DataGroup> list = new ArrayList<>();
		list.add(read(type, "oneLinkTopLevel"));
		list.add(read(type, "oneLinkOneLevelDown"));
		SpiderReadResult spiderReadResult = new SpiderReadResult();
		spiderReadResult.listOfDataGroups = list;
		return spiderReadResult;
	}

	@Override
	public SpiderReadResult readAbstractList(String type, DataGroup filter) {
		return null;
	}

	@Override
	public DataGroup readLinkList(String type, String id) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Collection<DataGroup> generateLinkCollectionPointingToRecord(String type, String id) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public boolean recordsExistForRecordType(String type) {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public boolean recordExistsForAbstractOrImplementingRecordTypeAndRecordId(String type,
			String id) {
		this.type = type;
		this.id = id;
		return recordIdExistsForRecordType;
	}

}
