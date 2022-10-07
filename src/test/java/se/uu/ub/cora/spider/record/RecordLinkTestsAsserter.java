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

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertTrue;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import se.uu.ub.cora.data.Action;
import se.uu.ub.cora.data.DataChild;
import se.uu.ub.cora.data.DataGroup;
import se.uu.ub.cora.data.DataLink;
import se.uu.ub.cora.data.DataRecord;

public class RecordLinkTestsAsserter {
	public static void assertTopLevelLinkContainsReadActionOnly(DataRecord record) {
		DataRecordLinkSpy link = getLinkFromRecord(record);
		link.MCR.assertParameters("addAction", 0, Action.READ);
	}

	public static void assertTopLevelLinkContainsReadActionOnly(DataGroup dataGroup) {
		DataLink link = (DataLink) dataGroup.getFirstChildWithNameInData("link");
		assertTrue(link.hasReadAction());
	}

	private static DataRecordLinkSpy getLinkFromRecord(DataRecord record) {
		DataGroup dataGroup = record.getDataGroup();
		DataRecordLinkSpy link = (DataRecordLinkSpy) dataGroup.getFirstChildWithNameInData("link");
		return link;
	}

	public static void assertTopLevelTwoLinksContainReadActionOnly(DataRecord record) {
		List<DataRecordLinkSpy> links = getLinksFromRecord(record);
		for (DataRecordLinkSpy link : links) {
			link.MCR.assertParameters("addAction", 0, Action.READ);
		}
		assertEquals(links.size(), 2);
	}

	public static void assertTopLevelTwoLinksDoesNotContainReadAction(DataRecord record) {
		List<DataRecordLinkSpy> links = getLinksFromRecord(record);
		for (DataRecordLinkSpy link : links) {
			link.MCR.assertMethodNotCalled("addAction");
		}
		assertEquals(links.size(), 2);
	}

	private static List<DataRecordLinkSpy> getLinksFromRecord(DataRecord record) {
		DataGroup dataGroup = record.getDataGroup();
		List<DataChild> links = dataGroup.getAllChildrenWithNameInData("link");
		List<DataRecordLinkSpy> links2 = new ArrayList<DataRecordLinkSpy>();
		for (DataChild DataGroup2 : links) {
			links2.add((DataRecordLinkSpy) DataGroup2);
		}
		return links2;
	}

	public static void assertOneLevelDownLinkContainsReadActionOnly(DataRecord record) {
		DataGroup dataGroup = record.getDataGroup();
		DataGroup dataGroupOneLevelDown = (DataGroup) dataGroup
				.getFirstChildWithNameInData("oneLevelDown");
		DataRecordLinkSpy link = (DataRecordLinkSpy) dataGroupOneLevelDown
				.getFirstChildWithNameInData("link");
		link.MCR.assertParameters("addAction", 0, Action.READ);
	}

	public static void assertTopLevelResourceLinkContainsReadActionOnly(DataRecord record) {
		DataLink link = getResourceLinkFromRecord(record);
		assertTrue(link.hasReadAction());
	}

	private static DataLink getResourceLinkFromRecord(DataRecord record) {
		DataGroup dataGroup = record.getDataGroup();
		DataLink link = (DataLink) dataGroup.getFirstChildWithNameInData("link");
		return link;
	}

	public static void assertOneLevelDownResourceLinkContainsReadActionOnly(DataRecord record) {
		DataGroup dataGroup = record.getDataGroup();
		DataGroup dataGroupOneLevelDown = (DataGroup) dataGroup
				.getFirstChildWithNameInData("oneLevelDown");
		DataLink link = (DataLink) dataGroupOneLevelDown.getFirstChildWithNameInData("link");
		assertTrue(link.hasReadAction());
	}

	public static void assertTopLevelLinkDoesNotContainReadAction(DataRecord record) {
		DataLink link = getLinkFromRecord(record);
		assertFalse(link.hasReadAction());
	}

	public static void assertRecordStorageWasNOTCalledForReadKey(
			RecordEnhancerTestsRecordStorage recordStorage, String readKey) {
		Map<String, Integer> readNumberMap = recordStorage.readNumberMap;
		assertFalse(readNumberMap.containsKey(readKey));
	}

}
