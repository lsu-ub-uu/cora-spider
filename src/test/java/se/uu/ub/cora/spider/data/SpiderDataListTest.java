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

import static org.testng.Assert.assertEquals;

import java.util.List;

import org.testng.annotations.Test;

public class SpiderDataListTest {
	@Test
	public void testInit() {
		String containDataOfType = "metadata";
		SpiderDataList spiderDataList = SpiderDataList.withContainDataOfType(containDataOfType);
		assertEquals(spiderDataList.getContainDataOfType(), "metadata");
	}

	@Test
	public void testAddRecord() {
		SpiderDataList spiderDataList = SpiderDataList.withContainDataOfType("metadata");
		SpiderDataGroup spiderDataGroup = SpiderDataGroup.withNameInData("spiderDataGroupId");
		SpiderDataRecord record = SpiderDataRecord.withSpiderDataGroup(spiderDataGroup);
		spiderDataList.addData(record);
		List<SpiderData> records = spiderDataList.getDataList();
		assertEquals(records.get(0), record);
	}

	@Test
	public void testAddGroup() {
		SpiderDataList spiderDataList = SpiderDataList.withContainDataOfType("metadata");
		SpiderDataGroup spiderDataGroup = SpiderDataGroup.withNameInData("spiderDataGroupId");
		spiderDataList.addData(spiderDataGroup);
		List<SpiderData> groups = spiderDataList.getDataList();
		assertEquals(groups.get(0), spiderDataGroup);
	}

	@Test
	public void testTotalNo() {
		SpiderDataList spiderDataList = SpiderDataList.withContainDataOfType("metadata");
		spiderDataList.setTotalNo("2");
		assertEquals(spiderDataList.getTotalNumberOfTypeInStorage(), "2");
	}

	@Test
	public void testFromNo() {
		SpiderDataList spiderDataList = SpiderDataList.withContainDataOfType("metadata");
		spiderDataList.setFromNo("0");
		assertEquals(spiderDataList.getFromNo(), "0");
	}

	@Test
	public void testToNo() {
		SpiderDataList spiderDataList = SpiderDataList.withContainDataOfType("metadata");
		spiderDataList.setToNo("2");
		assertEquals(spiderDataList.getToNo(), "2");
	}
}
