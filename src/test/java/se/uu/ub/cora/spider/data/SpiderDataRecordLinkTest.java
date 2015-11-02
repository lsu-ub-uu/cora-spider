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

import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;
import se.uu.ub.cora.bookkeeper.data.DataGroup;
import se.uu.ub.cora.bookkeeper.data.DataRecordLink;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertTrue;

public class SpiderDataRecordLinkTest {
	private SpiderDataRecordLink spiderRecordLink;

	@BeforeMethod
	public void setUp() {
		spiderRecordLink = SpiderDataRecordLink.withNameInDataAndLinkedRecordTypeAndLinkedRecordId("nameInData",
				"linkedRecordType", "linkedRecordId");

	}

	@Test
	public void testInit() {
		assertEquals(spiderRecordLink.getNameInData(), "nameInData");
		assertEquals(spiderRecordLink.getLinkedRecordType(), "linkedRecordType");
		assertEquals(spiderRecordLink.getLinkedRecordId(), "linkedRecordId");
	}

	@Test
	public void testInitWithRepeatId() {
		spiderRecordLink.setRepeatId("hugh");
		assertEquals(spiderRecordLink.getNameInData(), "nameInData");
		assertEquals(spiderRecordLink.getLinkedRecordType(), "linkedRecordType");
		assertEquals(spiderRecordLink.getLinkedRecordId(), "linkedRecordId");
		assertEquals(spiderRecordLink.getRepeatId(), "hugh");
	}

	@Test
	public void testInitWithLinkedRepeatId(){
		spiderRecordLink.setLinkedRepeatId("1");
		assertEquals(spiderRecordLink.getNameInData(), "nameInData");
		assertEquals(spiderRecordLink.getLinkedRepeatId(), "1");
	}

	@Test
	public void testInitWithLinkedPath(){
		SpiderDataGroup spiderDataGroup = SpiderDataGroup.withNameInData("linkedPathDataGroup");
		spiderRecordLink.setLinkedPath(spiderDataGroup);
		assertEquals(spiderRecordLink.getLinkedPath().getNameInData(), "linkedPathDataGroup");
	}

	@Test
	public void testAddAction() {
		spiderRecordLink.addAction(Action.READ);

		assertTrue(spiderRecordLink.getActions().contains(Action.READ));
		assertFalse(spiderRecordLink.getActions().contains(Action.DELETE));
		// small hack to get 100% coverage on enum
		Action.valueOf(Action.READ.toString());
	}

	@Test
	public void testToDataRecordLink() {
		DataRecordLink dataRecordLink = spiderRecordLink.toDataRecordLink();

		assertEquals(dataRecordLink.getNameInData(), "nameInData");
		assertEquals(dataRecordLink.getLinkedRecordType(), "linkedRecordType");
		assertEquals(dataRecordLink.getLinkedRecordId(), "linkedRecordId");
	}

	@Test
	public void testToDataRecordLinkWithRepeatId() {
		spiderRecordLink.setRepeatId("essan");
		DataRecordLink dataRecordLink = spiderRecordLink.toDataRecordLink();
		assertEquals(dataRecordLink.getNameInData(), "nameInData");
		assertEquals(dataRecordLink.getLinkedRecordType(), "linkedRecordType");
		assertEquals(dataRecordLink.getLinkedRecordId(), "linkedRecordId");
		assertEquals(dataRecordLink.getRepeatId(), "essan");
	}

	@Test
	public void testToDataRecordWithLinkedRepeatId(){
		spiderRecordLink.setLinkedRepeatId("one");
		DataRecordLink dataRecordLink = spiderRecordLink.toDataRecordLink();
		assertEquals(dataRecordLink.getLinkedRepeatId(), "one");
	}

	@Test
	public void testToDataRecordWithLinkedPath(){
		SpiderDataGroup spiderDataGroup = SpiderDataGroup.withNameInData("linkedPathDataGroup");
		spiderRecordLink.setLinkedPath(spiderDataGroup);
		DataRecordLink dataRecordLink = spiderRecordLink.toDataRecordLink();
		assertEquals(dataRecordLink.getLinkedPath().getNameInData(), "linkedPathDataGroup");
	}


	@Test
	public void testFromDataRecordLink() {
		DataRecordLink dataRecordLink = DataRecordLink
				.withNameInDataAndLinkedRecordTypeAndLinkedRecordId("nameInData", "linkedRecordType", "linkedRecordId");
		SpiderDataRecordLink spiderDataRecordLink = SpiderDataRecordLink
				.fromDataRecordLink(dataRecordLink);
		assertEquals(spiderDataRecordLink.getNameInData(), "nameInData");
		assertEquals(spiderDataRecordLink.getLinkedRecordType(), "linkedRecordType");
		assertEquals(spiderDataRecordLink.getLinkedRecordId(), "linkedRecordId");
	}

	@Test
	public void testFromDataRecordLinkWithRepeatId() {
		DataRecordLink dataRecordLink = DataRecordLink
				.withNameInDataAndLinkedRecordTypeAndLinkedRecordId("nameInData", "linkedRecordType", "linkedRecordId");
		dataRecordLink.setRepeatId("roi");
		SpiderDataRecordLink spiderDataRecordLink = SpiderDataRecordLink
				.fromDataRecordLink(dataRecordLink);
		assertEquals(spiderDataRecordLink.getNameInData(), "nameInData");
		assertEquals(spiderDataRecordLink.getLinkedRecordType(), "linkedRecordType");
		assertEquals(spiderDataRecordLink.getLinkedRecordId(), "linkedRecordId");
		assertEquals(spiderDataRecordLink.getRepeatId(), "roi");
	}

	@Test
	public void testFromDataRecordLinkWithLinkedRepeatId(){
		DataRecordLink dataRecordLink = DataRecordLink.withNameInDataAndLinkedRecordTypeAndLinkedRecordId("nameInData", "linkedRecordType", "linkedRecordId");
		dataRecordLink.setLinkedRepeatId("linkedOne");
		SpiderDataRecordLink spiderDataRecordLink = SpiderDataRecordLink.fromDataRecordLink(dataRecordLink);
		assertEquals(spiderDataRecordLink.getLinkedRepeatId(), "linkedOne");
	}

	@Test
	public void testFromDataRecordLinkWithLinkedLinkedPath(){
		DataRecordLink dataRecordLink = DataRecordLink.withNameInDataAndLinkedRecordTypeAndLinkedRecordId("nameInData", "linkedRecordType", "linkedRecordId");
		dataRecordLink.setLinkedPath(DataGroup.withNameInData("linkedPath"));
		SpiderDataRecordLink spiderDataRecordLink = SpiderDataRecordLink.fromDataRecordLink(dataRecordLink);
		assertEquals(spiderDataRecordLink.getLinkedPath().getNameInData(), "linkedPath");
	}
}
