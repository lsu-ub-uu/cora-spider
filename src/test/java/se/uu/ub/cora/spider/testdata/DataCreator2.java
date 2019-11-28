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
		// recordInfo.addChild(new DataAtomicSpy("type",
		// recordType));
		return recordInfo;
	}

	public static DataGroup createRecordInfoWithRecordTypeAndRecordIdAndDataDivider(
			String recordType, String recordId, String dataDivider) {
		DataGroup recordInfo = createRecordInfoWithRecordType(recordType);
		recordInfo.addChild(new DataAtomicSpy("id", recordId));
		recordInfo.addChild(createDataDividerWithLinkedRecordId(dataDivider));
		return recordInfo;
	}

	public static DataGroup createDataDividerWithLinkedRecordId(String linkedRecordId) {
		DataGroup dataDivider = new DataGroupSpy("dataDivider");
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
}
