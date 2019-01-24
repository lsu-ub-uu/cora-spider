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

import se.uu.ub.cora.spider.data.Action;
import se.uu.ub.cora.spider.data.SpiderDataGroup;
import se.uu.ub.cora.spider.data.SpiderDataRecord;
import se.uu.ub.cora.spider.data.SpiderDataRecordLink;
import se.uu.ub.cora.spider.data.SpiderDataResourceLink;

public class RecordLinkTestsAsserter {
	public static void assertTopLevelLinkContainsReadActionOnly(SpiderDataRecord record) {
		SpiderDataRecordLink link = getLinkFromRecord(record);
		assertTrue(link.getActions().contains(Action.READ));
		assertEquals(link.getActions().size(), 1);
	}

	private static SpiderDataRecordLink getLinkFromRecord(SpiderDataRecord record) {
		SpiderDataGroup spiderDataGroup = record.getSpiderDataGroup();
		SpiderDataRecordLink link = (SpiderDataRecordLink) spiderDataGroup
				.getFirstChildWithNameInData("link");
		return link;
	}

	public static void assertTopLevelTwoLinksContainReadActionOnly(SpiderDataRecord record) {
		List<SpiderDataRecordLink> links = getLinksFromRecord(record);
		for (SpiderDataRecordLink link : links) {
			assertTrue(link.getActions().contains(Action.READ));
			assertEquals(link.getActions().size(), 1);
		}
		assertEquals(links.size(), 2);
	}

	private static List<SpiderDataRecordLink> getLinksFromRecord(SpiderDataRecord record) {
		SpiderDataGroup spiderDataGroup = record.getSpiderDataGroup();
		List<SpiderDataGroup> links = spiderDataGroup.getAllGroupsWithNameInData("link");
		List<SpiderDataRecordLink> links2 = new ArrayList<SpiderDataRecordLink>();
		for (SpiderDataGroup spiderDataGroup2 : links) {
			links2.add((SpiderDataRecordLink) spiderDataGroup2);
		}
		return links2;
	}

	public static void assertOneLevelDownLinkContainsReadActionOnly(SpiderDataRecord record) {
		SpiderDataGroup spiderDataGroup = record.getSpiderDataGroup();
		SpiderDataGroup spiderDataGroupOneLevelDown = (SpiderDataGroup) spiderDataGroup
				.getFirstChildWithNameInData("oneLevelDown");
		SpiderDataRecordLink link = (SpiderDataRecordLink) spiderDataGroupOneLevelDown
				.getFirstChildWithNameInData("link");
		assertTrue(link.getActions().contains(Action.READ));
		assertEquals(link.getActions().size(), 1);
	}

	public static void assertTopLevelResourceLinkContainsReadActionOnly(SpiderDataRecord record) {
		SpiderDataResourceLink link = getResourceLinkFromRecord(record);
		assertTrue(link.getActions().contains(Action.READ));
		assertEquals(link.getActions().size(), 1);
	}

	private static SpiderDataResourceLink getResourceLinkFromRecord(SpiderDataRecord record) {
		SpiderDataGroup spiderDataGroup = record.getSpiderDataGroup();
		SpiderDataResourceLink link = (SpiderDataResourceLink) spiderDataGroup
				.getFirstChildWithNameInData("link");
		return link;
	}

	public static void assertOneLevelDownResourceLinkContainsReadActionOnly(
			SpiderDataRecord record) {
		SpiderDataGroup spiderDataGroup = record.getSpiderDataGroup();
		SpiderDataGroup spiderDataGroupOneLevelDown = (SpiderDataGroup) spiderDataGroup
				.getFirstChildWithNameInData("oneLevelDown");
		SpiderDataResourceLink link = (SpiderDataResourceLink) spiderDataGroupOneLevelDown
				.getFirstChildWithNameInData("link");
		assertTrue(link.getActions().contains(Action.READ));
		assertEquals(link.getActions().size(), 1);
	}

	public static void assertTopLevelLinkDoesNotContainReadAction(SpiderDataRecord record) {
		SpiderDataRecordLink link = getLinkFromRecord(record);
		assertFalse(link.getActions().contains(Action.READ));
		assertEquals(link.getActions().size(), 0);
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
