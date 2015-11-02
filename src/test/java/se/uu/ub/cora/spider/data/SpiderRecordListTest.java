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

package se.uu.ub.cora.spider.data;

import java.util.List;

import org.testng.annotations.Test;

import static org.testng.Assert.assertEquals;

public class SpiderRecordListTest {
	@Test
	public void testInit() {
		String containRecordsOfType = "metadata";
		SpiderRecordList spiderRecordList = SpiderRecordList
				.withContainRecordsOfType(containRecordsOfType);
		assertEquals(spiderRecordList.getContainRecordsOfType(), "metadata");
	}

	@Test
	public void testAddRecord() {
		SpiderRecordList spiderRecordList = SpiderRecordList.withContainRecordsOfType("metadata");
		SpiderDataGroup spiderDataGroup = SpiderDataGroup.withNameInData("spiderDataGroupId");
		SpiderDataRecord record = SpiderDataRecord.withSpiderDataGroup(spiderDataGroup);
		spiderRecordList.addRecord(record);
		List<SpiderDataRecord> records = spiderRecordList.getRecords();
		assertEquals(records.get(0), record);
	}

	@Test
	public void testTotalNo() {
		SpiderRecordList spiderRecordList = SpiderRecordList.withContainRecordsOfType("metadata");
		spiderRecordList.setTotalNo("2");
		assertEquals(spiderRecordList.getTotalNumberOfTypeInStorage(), "2");
	}

	@Test
	public void testFromNo() {
		SpiderRecordList spiderRecordList = SpiderRecordList.withContainRecordsOfType("metadata");
		spiderRecordList.setFromNo("0");
		assertEquals(spiderRecordList.getFromNo(), "0");
	}

	@Test
	public void testToNo() {
		SpiderRecordList spiderRecordList = SpiderRecordList.withContainRecordsOfType("metadata");
		spiderRecordList.setToNo("2");
		assertEquals(spiderRecordList.getToNo(), "2");
	}
}
