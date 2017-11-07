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

import se.uu.ub.cora.spider.data.SpiderDataAtomic;
import se.uu.ub.cora.spider.data.SpiderDataGroup;

public final class SpiderDataCreator {

	public static SpiderDataGroup createRecordInfoWithRecordType(String recordType) {
		SpiderDataGroup recordInfo = SpiderDataGroup.withNameInData("recordInfo");
		SpiderDataGroup typeGroup = SpiderDataGroup.withNameInData("type");
		typeGroup.addChild(
				SpiderDataAtomic.withNameInDataAndValue("linkedRecordType", "recordType"));
		typeGroup.addChild(SpiderDataAtomic.withNameInDataAndValue("linkedRecordId", recordType));
		recordInfo.addChild(typeGroup);
		// recordInfo.addChild(SpiderDataAtomic.withNameInDataAndValue("type",
		// recordType));
		return recordInfo;
	}

	public static SpiderDataGroup createRecordInfoWithRecordTypeAndRecordIdAndDataDivider(
			String recordType, String recordId, String dataDivider) {
		SpiderDataGroup recordInfo = createRecordInfoWithRecordType(recordType);
		recordInfo.addChild(SpiderDataAtomic.withNameInDataAndValue("id", recordId));
		recordInfo.addChild(createDataDividerWithLinkedRecordId(dataDivider));
		return recordInfo;
	}

	public static SpiderDataGroup createDataDividerWithLinkedRecordId(String linkedRecordId) {
		SpiderDataGroup dataDivider = SpiderDataGroup.withNameInData("dataDivider");
		dataDivider.addChild(SpiderDataAtomic.withNameInDataAndValue("linkedRecordType", "system"));
		dataDivider.addChild(
				SpiderDataAtomic.withNameInDataAndValue("linkedRecordId", linkedRecordId));
		return dataDivider;
	}

	public static SpiderDataGroup createSearchWithIdAndRecordTypeToSearchIn(String id,
			String idRecordTypeToSearchIn) {
		SpiderDataGroup search = SpiderDataGroup.withNameInData("search");
		SpiderDataGroup recordInfo = SpiderDataCreator.createRecordInfoWithRecordType("search");
		recordInfo.addChild(SpiderDataAtomic.withNameInDataAndValue("id", id));
		search.addChild(recordInfo);

		SpiderDataGroup recordTypeToSearchIn = SpiderDataGroup
				.withNameInData("recordTypeToSearchIn");
		recordTypeToSearchIn.addChild(
				SpiderDataAtomic.withNameInDataAndValue("linkedRecordType", "recordType"));
		recordTypeToSearchIn.addChild(
				SpiderDataAtomic.withNameInDataAndValue("linkedRecordId", idRecordTypeToSearchIn));
		search.addChild(recordTypeToSearchIn);
		return search;
	}
}
