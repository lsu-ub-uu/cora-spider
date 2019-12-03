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

package se.uu.ub.cora.spider.testdata;

import se.uu.ub.cora.data.DataGroup;
import se.uu.ub.cora.spider.data.DataAtomicSpy;
import se.uu.ub.cora.spider.data.DataGroupSpy;

public final class DataCreator2 {

	public static DataGroup createRecordInfoWithRecordType(String recordType) {
		DataGroup recordInfo = new DataGroupSpy("recordInfo");
		DataGroup typeGroup = new DataGroupSpy("type");
		typeGroup.addChild(new DataAtomicSpy("linkedRecordType", "recordType"));
		typeGroup.addChild(new DataAtomicSpy("linkedRecordId", recordType));
		recordInfo.addChild(typeGroup);
		return recordInfo;
	}

	public static DataGroup createRecordInfoWithRecordTypeAndRecordIdAndDataDivider(
			String recordType, String recordId, String dataDivider) {
		DataGroup recordInfo = createRecordInfoWithRecordType(recordType);
		recordInfo.addChild(new DataAtomicSpy("id", recordId));
		recordInfo.addChild(createDataDividerWithLinkedRecordId(dataDivider));
		return recordInfo;
	}

	public static DataGroup createRecordInfoWithIdAndLinkedRecordId(String id,
			String linkedRecordId) {
		DataGroup createRecordInfo = new DataGroupSpy("recordInfo");
		DataGroup dataDivider = createDataDividerWithLinkedRecordId(linkedRecordId);
		createRecordInfo.addChild(dataDivider);
		createRecordInfo.addChild(new DataAtomicSpy("id", id));
		return createRecordInfo;
	}

	public static DataGroup createRecordWithNameInDataAndIdAndLinkedRecordId(String nameInData,
			String id, String linkedRecordId) {
		DataGroup record = new DataGroupSpy(nameInData);
		DataGroup createRecordInfo = createRecordInfoWithIdAndLinkedRecordId(id, linkedRecordId);
		record.addChild(createRecordInfo);
		return record;
	}

	// TODO:samordna recordInfos!
	public static DataGroup createRecordWithNameInDataAndLinkedRecordId(String nameInData,
			String linkedRecordId) {
		DataGroup record = new DataGroupSpy(nameInData);
		DataGroup createRecordInfo = createRecordInfoWithLinkedRecordId(linkedRecordId);
		record.addChild(createRecordInfo);
		return record;
	}

	public static DataGroup createRecordInfoWithLinkedRecordId(String linkedRecordId) {
		DataGroup createRecordInfo = new DataGroupSpy("recordInfo");
		DataGroup dataDivider = createDataDividerWithLinkedRecordId(linkedRecordId);
		createRecordInfo.addChild(dataDivider);
		return createRecordInfo;
	}

	public static DataGroup createDataDividerWithLinkedRecordId(String linkedRecordId) {
		DataRecordLinkSpy dataDivider = new DataRecordLinkSpy("dataDivider");
		dataDivider.addChild(new DataAtomicSpy("linkedRecordType", "system"));
		dataDivider.addChild(new DataAtomicSpy("linkedRecordId", linkedRecordId));
		return dataDivider;
	}

	public static DataGroup createSearchWithIdAndRecordTypeToSearchIn(String id,
			String idRecordTypeToSearchIn) {
		DataGroup search = new DataGroupSpy("search");
		DataGroup recordInfo = DataCreator2.createRecordInfoWithRecordType("search");
		recordInfo.addChild(new DataAtomicSpy("id", id));
		search.addChild(recordInfo);

		DataGroup recordTypeToSearchIn = new DataGroupSpy("recordTypeToSearchIn");
		recordTypeToSearchIn.addChild(new DataAtomicSpy("linkedRecordType", "recordType"));
		recordTypeToSearchIn.addChild(new DataAtomicSpy("linkedRecordId", idRecordTypeToSearchIn));
		search.addChild(recordTypeToSearchIn);
		return search;
	}

	public static DataGroup createRecordWithNameInDataAndIdAndTypeAndLinkedRecordId(
			String nameInData, String id, String recordType, String linkedRecordId) {
		DataGroup record = new DataGroupSpy(nameInData);
		DataGroup createRecordInfo = createRecordInfoWithIdAndTypeAndLinkedRecordId(id, recordType,
				linkedRecordId);
		record.addChild(createRecordInfo);
		return record;
	}

	public static DataGroup createRecordInfoWithIdAndTypeAndLinkedRecordId(String id,
			String recordType, String linkedRecordId) {
		DataGroup createRecordInfo = new DataGroupSpy("recordInfo");
		createRecordInfo.addChild(new DataAtomicSpy("id", id));
		DataRecordLinkSpy typeGroup = new DataRecordLinkSpy("type");
		typeGroup.addChild(new DataAtomicSpy("linkedRecordType", "recordType"));
		typeGroup.addChild(new DataAtomicSpy("linkedRecordId", recordType));
		createRecordInfo.addChild(typeGroup);

		DataGroup dataDivider = createDataDividerWithLinkedRecordId(linkedRecordId);
		createRecordInfo.addChild(dataDivider);
		return createRecordInfo;
	}

	public static DataGroup createWorkOrderWithIdAndRecordTypeAndRecordIdToIndex(String id,
			String recordType, String recordId) {
		DataGroup workOrder = new DataGroupSpy("workOrder");
		DataGroup recordInfo = new DataGroupSpy("recordInfo");
		recordInfo.addChild(new DataAtomicSpy("id", id));
		workOrder.addChild(recordInfo);

		DataGroup recordTypeLink = new DataGroupSpy("recordType");
		recordTypeLink.addChild(new DataAtomicSpy("linkedRecordType", "recordType"));
		recordTypeLink.addChild(new DataAtomicSpy("linkedRecordId", recordType));
		workOrder.addChild(recordTypeLink);

		workOrder.addChild(new DataAtomicSpy("recordId", recordId));
		workOrder.addChild(new DataAtomicSpy("type", "index"));
		return workOrder;
	}
}
