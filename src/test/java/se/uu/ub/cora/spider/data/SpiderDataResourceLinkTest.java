/*
 * Copyright 2016 Uppsala University Library
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
import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertNotNull;
import static org.testng.Assert.assertTrue;

import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import se.uu.ub.cora.data.DataAtomic;
import se.uu.ub.cora.data.DataAtomicProvider;
import se.uu.ub.cora.data.DataGroup;
import se.uu.ub.cora.data.DataGroupFactory;
import se.uu.ub.cora.data.DataGroupProvider;

public class SpiderDataResourceLinkTest {

	SpiderDataResourceLink spiderResourceLink;
	private DataGroupFactory dataGroupFactory;
	private DataAtomicFactorySpy dataAtomicFactory;

	@BeforeMethod
	public void setUp() {
		dataGroupFactory = new DataGroupFactorySpy();
		DataGroupProvider.setDataGroupFactory(dataGroupFactory);
		dataAtomicFactory = new DataAtomicFactorySpy();
		DataAtomicProvider.setDataAtomicFactory(dataAtomicFactory);
		spiderResourceLink = SpiderDataResourceLink.withNameInData("nameInData");

		SpiderDataAtomic streamId = SpiderDataAtomic.withNameInDataAndValue("streamId",
				"myStreamId");
		spiderResourceLink.addChild(streamId);

	}

	@Test
	public void testInit() {
		assertEquals(spiderResourceLink.getNameInData(), "nameInData");
		assertNotNull(spiderResourceLink.getAttributes());
		assertNotNull(spiderResourceLink.getChildren());
		assertEquals(spiderResourceLink.extractAtomicValue("streamId"), "myStreamId");
		assertNotNull(spiderResourceLink.getActions());
	}

	@Test
	public void testInitWithRepeatId() {
		spiderResourceLink.setRepeatId("hugh");
		assertEquals(spiderResourceLink.getRepeatId(), "hugh");
	}

	@Test
	public void testAddAction() {
		spiderResourceLink.addAction(Action.READ);

		assertTrue(spiderResourceLink.getActions().contains(Action.READ));
		assertFalse(spiderResourceLink.getActions().contains(Action.DELETE));
		// small hack to get 100% coverage on enum
		Action.valueOf(Action.READ.toString());
	}

	@Test
	public void testToDataRecordLink() {
		DataGroup dataRecordLink = spiderResourceLink.toDataGroup();

		assertEquals(dataRecordLink.getNameInData(), "nameInData");
		assertEquals(dataRecordLink.getFirstAtomicValueWithNameInData("streamId"), "myStreamId");
	}

	@Test
	public void testToDataRecordLinkWithRepeatId() {
		spiderResourceLink.setRepeatId("essan");
		DataGroup dataRecordLink = spiderResourceLink.toDataGroup();
		assertEquals(dataRecordLink.getNameInData(), "nameInData");
		assertEquals(dataRecordLink.getFirstAtomicValueWithNameInData("streamId"), "myStreamId");
		assertEquals(dataRecordLink.getRepeatId(), "essan");
	}

	@Test
	public void testFromDataRecordLink() {
		DataGroup dataRecordLink = createResourceLinkAsDataGroup();

		SpiderDataRecordLink spiderDataRecordLink = SpiderDataRecordLink
				.fromDataRecordLink(dataRecordLink);

		assertCorrectFromDataResourceLink(spiderDataRecordLink);
	}

	private DataGroup createResourceLinkAsDataGroup() {
		DataGroup dataRecordLink = new DataGroupSpy("nameInData");

		DataAtomic streamId = new DataAtomicSpy("streamId", "myStreamId");
		dataRecordLink.addChild(streamId);
		return dataRecordLink;
	}

	private void assertCorrectFromDataResourceLink(SpiderDataRecordLink spiderDataRecordLink) {
		assertEquals(spiderDataRecordLink.getNameInData(), "nameInData");

		SpiderDataAtomic convertedRecordId = (SpiderDataAtomic) spiderDataRecordLink
				.getFirstChildWithNameInData("streamId");
		assertEquals(convertedRecordId.getValue(), "myStreamId");
	}

	@Test
	public void testFromDataRecordLinkWithRepeatId() {
		DataGroup dataRecordLink = createResourceLinkAsDataGroup();
		dataRecordLink.setRepeatId("roi");

		SpiderDataRecordLink spiderDataRecordLink = SpiderDataRecordLink
				.fromDataRecordLink(dataRecordLink);
		assertCorrectFromDataResourceLinkWithRepeatId(spiderDataRecordLink);
	}

	private void assertCorrectFromDataResourceLinkWithRepeatId(
			SpiderDataRecordLink spiderDataRecordLink) {
		assertCorrectFromDataResourceLink(spiderDataRecordLink);
		assertEquals(spiderDataRecordLink.getRepeatId(), "roi");
	}
}
