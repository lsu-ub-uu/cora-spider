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

package se.uu.ub.cora.spider.spy;

import java.util.Collection;

import se.uu.ub.cora.bookkeeper.data.DataAtomic;
import se.uu.ub.cora.bookkeeper.data.DataGroup;
import se.uu.ub.cora.spider.record.storage.RecordStorage;

public class RecordStorageCreateUpdateSpy implements RecordStorage {

	public DataGroup createRecord;
	public DataGroup updateRecord;
	public String dataDivider;

	public boolean modifiableLinksExistsForRecord = false;

	@Override
	public DataGroup read(String type, String id) {
		if (type.equals("recordType") && id.equals("typeWithAutoGeneratedId")) {
			DataGroup group = DataGroup.withNameInData("recordType");
			group.addChild(DataAtomic.withNameInDataAndValue("newMetadataId", "placeNew"));
			group.addChild(DataAtomic.withNameInDataAndValue("metadataId", "place"));
			group.addChild(DataAtomic.withNameInDataAndValue("userSuppliedId", "false"));
			group.addChild(DataAtomic.withNameInDataAndValue("abstract", "false"));

			return group;
		}
		if (type.equals("recordType") && id.equals("typeWithUserGeneratedId")) {
			DataGroup group = DataGroup.withNameInData("recordType");
			group.addChild(DataAtomic.withNameInDataAndValue("newMetadataId", "placeNew"));
			group.addChild(DataAtomic.withNameInDataAndValue("metadataId", "place"));
			group.addChild(DataAtomic.withNameInDataAndValue("userSuppliedId", "true"));
			group.addChild(DataAtomic.withNameInDataAndValue("abstract", "false"));
			return group;
		}
		if (type.equals("typeWithUserGeneratedId") && id.equals("uppsalaRecord1")) {
			DataGroup group = DataGroup.withNameInData("typeWithUserGeneratedId");
			group.addChild(DataAtomic.withNameInDataAndValue("newMetadataId", "placeNew"));
			group.addChild(DataAtomic.withNameInDataAndValue("metadataId", "place"));
			group.addChild(DataAtomic.withNameInDataAndValue("userSuppliedId", "true"));
			group.addChild(DataAtomic.withNameInDataAndValue("abstract", "false"));
			group.addChild(DataAtomic.withNameInDataAndValue("unit", "Uppsala"));
			return group;
		}
		if (type.equals("typeWithUserGeneratedId") && id.equals("gothenburgRecord1")) {
			DataGroup group = DataGroup.withNameInData("typeWithUserGeneratedId");
			group.addChild(DataAtomic.withNameInDataAndValue("newMetadataId", "placeNew"));
			group.addChild(DataAtomic.withNameInDataAndValue("metadataId", "place"));
			group.addChild(DataAtomic.withNameInDataAndValue("userSuppliedId", "true"));
			group.addChild(DataAtomic.withNameInDataAndValue("abstract", "false"));
			group.addChild(DataAtomic.withNameInDataAndValue("unit", "gothenburg"));
			return group;
		}
		if (type.equals("recordType") && id.equals("recordType")) {
			DataGroup group = DataGroup.withNameInData("recordType");
			group.addChild(DataAtomic.withNameInDataAndValue("newMetadataId", "recordTypeNew"));
			group.addChild(DataAtomic.withNameInDataAndValue("recordInfo", "recordInfo"));
			group.addChild(DataAtomic.withNameInDataAndValue("metadataId", "recordType"));
			group.addChild(DataAtomic.withNameInDataAndValue("userSuppliedId", "true"));
			group.addChild(DataAtomic.withNameInDataAndValue("abstract", "false"));
			return group;
		}
		if (type.equals("recordType") && id.equals("image")) {
			DataGroup group = DataGroup.withNameInData("recordType");
			group.addChild(DataAtomic.withNameInDataAndValue("newMetadataId", "imageNew"));
			group.addChild(DataAtomic.withNameInDataAndValue("metadataId", "image"));
			group.addChild(DataAtomic.withNameInDataAndValue("userSuppliedId", "true"));
			group.addChild(DataAtomic.withNameInDataAndValue("abstract", "false"));
			group.addChild(DataAtomic.withNameInDataAndValue("parentId", "binary"));
			return group;
		}
		if (type.equals("recordType") && id.equals("binary")) {
			DataGroup group = DataGroup.withNameInData("recordType");
			group.addChild(DataAtomic.withNameInDataAndValue("newMetadataId", "binaryNew"));
			group.addChild(DataAtomic.withNameInDataAndValue("metadataId", "binary"));
			group.addChild(DataAtomic.withNameInDataAndValue("userSuppliedId", "true"));
			group.addChild(DataAtomic.withNameInDataAndValue("abstract", "true"));
			return group;
		}
		return null;
	}

	@Override
	public void create(String type, String id, DataGroup record, DataGroup linkList,
			String dataDivider) {
		createRecord = record;
		this.dataDivider = dataDivider;
	}

	@Override
	public void deleteByTypeAndId(String type, String id) {
		// TODO Auto-generated method stub

	}

	@Override
	public boolean linksExistForRecord(String type, String id) {
		return modifiableLinksExistsForRecord;
	}

	@Override
	public void update(String type, String id, DataGroup record, DataGroup linkList,
			String dataDivider) {
		updateRecord = record;
		this.dataDivider = dataDivider;
	}

	@Override
	public Collection<DataGroup> readList(String type) {
		// TODO Auto-generated method stub
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
	public boolean recordExistsForRecordTypeAndRecordId(String type, String id) {
		return false;
	}

}
