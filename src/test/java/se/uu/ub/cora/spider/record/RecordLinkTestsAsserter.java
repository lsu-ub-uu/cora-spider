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

import se.uu.ub.cora.data.DataGroup;
import se.uu.ub.cora.data.DataLink;
import se.uu.ub.cora.data.DataRecord;

public class RecordLinkTestsAsserter {
	public static void assertTopLevelLinkContainsReadActionOnly(DataRecord record) {
		DataLink link = getLinkFromRecord(record);
		assertTrue(link.hasReadAction());
	}

	public static void assertTopLevelLinkContainsReadActionOnly(DataGroup dataGroup) {
		DataLink link = (DataLink) dataGroup.getFirstChildWithNameInData("link");
		assertTrue(link.hasReadAction());
	}

	private static DataLink getLinkFromRecord(DataRecord record) {
		DataGroup dataGroup = record.getDataGroup();
		DataLink link = (DataLink) dataGroup.getFirstChildWithNameInData("link");
		return link;
	}

	public static void assertTopLevelTwoLinksContainReadActionOnly(DataRecord record) {
		List<DataLink> links = getLinksFromRecord(record);
		for (DataLink link : links) {
			assertTrue(link.hasReadAction());
		}
		assertEquals(links.size(), 2);
	}

	public static void assertTopLevelTwoLinksDoesNotContainReadAction(DataRecord record) {
		List<DataLink> links = getLinksFromRecord(record);
		for (DataLink link : links) {
			assertFalse(link.hasReadAction());
		}
		assertEquals(links.size(), 2);
	}

	private static List<DataLink> getLinksFromRecord(DataRecord record) {
		DataGroup dataGroup = record.getDataGroup();
		List<DataGroup> links = dataGroup.getAllGroupsWithNameInData("link");
		List<DataLink> links2 = new ArrayList<DataLink>();
		for (DataGroup DataGroup2 : links) {
			links2.add((DataLink) DataGroup2);
		}
		return links2;
	}

	public static void assertOneLevelDownLinkContainsReadActionOnly(DataRecord record) {
		DataGroup dataGroup = record.getDataGroup();
		DataGroup dataGroupOneLevelDown = (DataGroup) dataGroup
				.getFirstChildWithNameInData("oneLevelDown");
		DataLink link = (DataLink) dataGroupOneLevelDown.getFirstChildWithNameInData("link");
		assertTrue(link.hasReadAction());
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

	public static void assertRecordStorageWasCalledOnlyOnceForReadKey(
			RecordEnhancerTestsRecordStorage recordStorage, String readKey) {
		Map<String, Integer> readNumberMap = recordStorage.readNumberMap;
		Integer actual = readNumberMap.get(readKey);
		assertEquals(actual.intValue(), 1);
	}
}
