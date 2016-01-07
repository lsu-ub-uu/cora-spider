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
import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertNotNull;
import static org.testng.Assert.assertTrue;

import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.testng.Assert;
import org.testng.annotations.Test;

import se.uu.ub.cora.bookkeeper.data.DataAtomic;
import se.uu.ub.cora.bookkeeper.data.DataElement;
import se.uu.ub.cora.bookkeeper.data.DataGroup;
//import se.uu.ub.cora.bookkeeper.data.DataRecordLink;

public class SpiderDataGroupTest {
	@Test
	public void testInit() {
		SpiderDataGroup spiderDataGroup = SpiderDataGroup.withNameInData("nameInData");
		assertEquals(spiderDataGroup.getNameInData(), "nameInData");
		assertNotNull(spiderDataGroup.getAttributes());
		assertNotNull(spiderDataGroup.getChildren());
	}

	@Test
	public void testGroupIsSpiderData() {
		SpiderDataGroup spiderDataGroup = SpiderDataGroup.withNameInData("nameInData");
		assertTrue(spiderDataGroup instanceof SpiderData);
	}

	@Test
	public void testInitWithRepeatId() {
		SpiderDataGroup spiderDataGroup = SpiderDataGroup.withNameInData("nameInData");
		spiderDataGroup.setRepeatId("hrumph");
		assertEquals(spiderDataGroup.getNameInData(), "nameInData");
		assertNotNull(spiderDataGroup.getAttributes());
		assertNotNull(spiderDataGroup.getChildren());
		assertEquals(spiderDataGroup.getRepeatId(), "hrumph");
	}

	@Test
	public void testAddAttribute() {
		SpiderDataGroup spiderDataGroup = SpiderDataGroup.withNameInData("nameInData");
		spiderDataGroup.addAttributeByIdWithValue("nameInData", "value");
		Map<String, String> attributes = spiderDataGroup.getAttributes();
		Entry<String, String> entry = attributes.entrySet().iterator().next();
		assertEquals(entry.getKey(), "nameInData");
		assertEquals(entry.getValue(), "value");
	}

	@Test
	public void testAddChild() {
		SpiderDataGroup spiderDataGroup = SpiderDataGroup.withNameInData("nameInData");
		SpiderDataElement dataElement = SpiderDataAtomic.withNameInDataAndValue("childNameInData",
				"childValue");
		spiderDataGroup.addChild(dataElement);
		List<SpiderDataElement> children = spiderDataGroup.getChildren();
		SpiderDataElement childElementOut = children.get(0);
		assertEquals(childElementOut.getNameInData(), "childNameInData");
	}

	@Test
	public void testContainsChildWithId() {
		SpiderDataGroup dataGroup = SpiderDataGroup.withNameInData("nameInData");
		dataGroup.addChild(
				SpiderDataAtomic.withNameInDataAndValue("otherChildId", "otherChildValue"));
		SpiderDataElement child = SpiderDataAtomic.withNameInDataAndValue("childId", "child value");
		dataGroup.addChild(child);
		assertTrue(dataGroup.containsChildWithNameInData("childId"));
	}

	@Test
	public void testContainsChildWithIdNotFound() {
		SpiderDataGroup dataGroup = SpiderDataGroup.withNameInData("nameInData");
		SpiderDataElement child = SpiderDataAtomic.withNameInDataAndValue("childId", "child value");
		dataGroup.addChild(child);
		assertFalse(dataGroup.containsChildWithNameInData("childId_NOT_FOUND"));
	}

	@Test
	public void testFromDataGroup() {
		DataGroup dataGroup = DataGroup.withNameInData("nameInData");
		SpiderDataGroup spiderDataGroup = SpiderDataGroup.fromDataGroup(dataGroup);
		assertEquals(spiderDataGroup.getNameInData(), "nameInData");
	}

	@Test
	public void testFromDataGroupWithRepeatId() {
		DataGroup dataGroup = DataGroup.withNameInData("nameInData");
		dataGroup.setRepeatId("puh");
		SpiderDataGroup spiderDataGroup = SpiderDataGroup.fromDataGroup(dataGroup);
		assertEquals(spiderDataGroup.getNameInData(), "nameInData");
		assertEquals(spiderDataGroup.getRepeatId(), "puh");
	}

	@Test
	public void testFromDataGroupWithAttribute() {
		DataGroup dataGroup = DataGroup.withNameInData("groupNameInData");
		dataGroup.addAttributeByIdWithValue("nameInData", "value");
		SpiderDataGroup spiderDataGroup = SpiderDataGroup.fromDataGroup(dataGroup);
		Map<String, String> attributes = spiderDataGroup.getAttributes();
		Entry<String, String> entry = attributes.entrySet().iterator().next();
		assertEquals(entry.getKey(), "nameInData");
		assertEquals(entry.getValue(), "value");
	}

	@Test
	public void testFromDataGroupWithChild() {
		DataGroup dataGroup = DataGroup.withNameInData("groupNameInData");
		dataGroup.addChild(DataAtomic.withNameInDataAndValue("childNameInData", "atomicValue"));
		SpiderDataGroup spiderDataGroup = SpiderDataGroup.fromDataGroup(dataGroup);
		Assert.assertEquals(spiderDataGroup.getChildren().stream().findAny().get().getNameInData(),
				"childNameInData");
	}

	@Test
	public void testFromDataGroupWithDataRecordLinkChild() {
		DataGroup dataGroup = DataGroup.withNameInData("groupNameInData");
		dataGroup.addChild(createRecordLink());
		SpiderDataGroup spiderDataGroup = SpiderDataGroup.fromDataGroup(dataGroup);
		SpiderDataElement spiderDataElement = spiderDataGroup.getChildren().get(0);
		assertEquals(spiderDataElement.getNameInData(), "childNameInData");

		SpiderDataRecordLink spiderDataRecordLink = (SpiderDataRecordLink) spiderDataElement;
		SpiderDataAtomic linkedRecordType = (SpiderDataAtomic) spiderDataRecordLink.getFirstChildWithNameInData("linkedRecordType");
		assertEquals(linkedRecordType.getValue(), "aRecordType");
		SpiderDataAtomic linkedRecordId = (SpiderDataAtomic)spiderDataRecordLink.getFirstChildWithNameInData("linkedRecordId");
		assertEquals(linkedRecordId.getValue(), "aRecordId");

	}

	@Test
	public void testFromDataGroupWithNonCompleteDataRecordLinkChild(){
		DataGroup dataGroup = DataGroup.withNameInData("groupNameInData");

		DataGroup dataRecordLinkWithNoLinkedRecordId = DataGroup.withNameInData("childNameInData");
		DataAtomic linkedRecordType = DataAtomic.withNameInDataAndValue("linkedRecordType", "aRecordType");
		dataRecordLinkWithNoLinkedRecordId.addChild(linkedRecordType);

		SpiderDataGroup spiderDataGroup = SpiderDataGroup.fromDataGroup(dataGroup);
		assertFalse(spiderDataGroup instanceof SpiderDataRecordLink);
	}

	private DataGroup createRecordLink() {
		DataGroup dataRecordLink = DataGroup.withNameInData("childNameInData");

		DataAtomic linkedRecordType = DataAtomic.withNameInDataAndValue("linkedRecordType", "aRecordType");
		dataRecordLink.addChild(linkedRecordType);

		DataAtomic linkedRecordId = DataAtomic.withNameInDataAndValue("linkedRecordId", "aRecordId");
		dataRecordLink.addChild(linkedRecordId);

		return  dataRecordLink;
	}

	@Test
	public void testFromDataGroupLevelsOfChildren() {
		DataGroup dataGroup = DataGroup.withNameInData("nameInData");
		dataGroup.addChild(DataAtomic.withNameInDataAndValue("atomicNameInData", "atomicValue"));
		DataGroup dataGroup2 = DataGroup.withNameInData("childNameInData");
		dataGroup2.addChild(DataGroup.withNameInData("grandChildNameInData"));
		dataGroup.addChild(dataGroup2);
		SpiderDataGroup spiderDataGroup = SpiderDataGroup.fromDataGroup(dataGroup);
		Iterator<SpiderDataElement> iterator = spiderDataGroup.getChildren().iterator();
		Assert.assertTrue(iterator.hasNext(), "spiderDataGroup should have at least one child");

		SpiderDataAtomic dataAtomicChild = (SpiderDataAtomic) iterator.next();
		Assert.assertEquals(dataAtomicChild.getNameInData(), "atomicNameInData",
				"NameInData should be the one set in the DataAtomic, first child of dataGroup");

		SpiderDataGroup dataGroupChild = (SpiderDataGroup) iterator.next();
		Assert.assertEquals(dataGroupChild.getNameInData(), "childNameInData",
				"NameInData should be the one set in dataGroup2");

		Assert.assertEquals(dataGroupChild.getChildren().stream().findAny().get().getNameInData(),
				"grandChildNameInData",
				"NameInData should be the one set in the child of dataGroup2");
	}

	@Test
	public void testToDataGroup() {
		SpiderDataGroup spiderDataGroup = SpiderDataGroup.withNameInData("nameInData");
		DataGroup dataGroup = spiderDataGroup.toDataGroup();
		assertEquals(dataGroup.getNameInData(), "nameInData");
		assertNotNull(dataGroup.getAttributes());
		assertNotNull(dataGroup.getChildren());
	}

	@Test
	public void testToDataGroupWithRepeatId() {
		SpiderDataGroup spiderDataGroup = SpiderDataGroup.withNameInData("nameInData");
		spiderDataGroup.setRepeatId("nalle");
		DataGroup dataGroup = spiderDataGroup.toDataGroup();
		assertEquals(dataGroup.getNameInData(), "nameInData");
		assertNotNull(dataGroup.getAttributes());
		assertNotNull(dataGroup.getChildren());
		assertEquals(dataGroup.getRepeatId(), "nalle");
	}

	@Test
	public void testToDataGroupWithAttribute() {
		SpiderDataGroup spiderDataGroup = SpiderDataGroup.withNameInData("nameInData");
		spiderDataGroup.addAttributeByIdWithValue("nameInData", "value");

		DataGroup dataGroup = spiderDataGroup.toDataGroup();

		Map<String, String> attributes = dataGroup.getAttributes();
		Entry<String, String> entry = attributes.entrySet().iterator().next();
		assertEquals(entry.getKey(), "nameInData");
		assertEquals(entry.getValue(), "value");
	}

	@Test
	public void testToDataGroupWithChild() {
		SpiderDataGroup spiderDataGroup = SpiderDataGroup.withNameInData("nameInData");
		SpiderDataElement dataElement = SpiderDataAtomic.withNameInDataAndValue("childNameInData",
				"childValue");
		spiderDataGroup.addChild(dataElement);

		DataGroup dataGroup = spiderDataGroup.toDataGroup();

		List<DataElement> children = dataGroup.getChildren();
		DataElement childElementOut = children.get(0);
		assertEquals(childElementOut.getNameInData(), "childNameInData");
	}

	@Test
	public void testToDataGroupWithDataRecordLinkChild() {
		SpiderDataGroup spiderDataGroup = SpiderDataGroup.withNameInData("nameInData");

		SpiderDataRecordLink spiderRecordLink = SpiderDataRecordLink.withNameInData("childNameInData");
		SpiderDataAtomic linkedRecordType = SpiderDataAtomic.withNameInDataAndValue("linkedRecordType", "aRecordType");
		spiderRecordLink.addChild(linkedRecordType);

		SpiderDataAtomic linkedRecordId = SpiderDataAtomic.withNameInDataAndValue("linkedRecordId", "aRecordId");
		spiderRecordLink.addChild(linkedRecordId);

		spiderDataGroup.addChild(spiderRecordLink);

		DataGroup dataGroup = spiderDataGroup.toDataGroup();

		List<DataElement> children = dataGroup.getChildren();
		DataElement childElementOut = children.get(0);
		assertEquals(childElementOut.getNameInData(), "childNameInData");
		DataGroup dataRecordLink = (DataGroup) childElementOut;
		assertEquals(dataRecordLink.getFirstAtomicValueWithNameInData("linkedRecordType"), "aRecordType");
		assertEquals(dataRecordLink.getFirstAtomicValueWithNameInData("linkedRecordId"), "aRecordId");

	}

	@Test
	public void testToDataGroupWithLevelsOfChildren() {
		SpiderDataGroup spiderDataGroup = SpiderDataGroup.withNameInData("nameInData");
		spiderDataGroup.addChild(
				SpiderDataAtomic.withNameInDataAndValue("atomicNameInData", "atomicValue"));
		SpiderDataGroup dataGroup2 = SpiderDataGroup.withNameInData("childNameInData");
		dataGroup2.addChild(SpiderDataGroup.withNameInData("grandChildNameInData"));
		spiderDataGroup.addChild(dataGroup2);

		DataGroup dataGroup = spiderDataGroup.toDataGroup();
		Iterator<DataElement> iterator = dataGroup.getChildren().iterator();
		assertTrue(iterator.hasNext(), "dataGroup should have at least one child");

		DataAtomic dataAtomicChild = (DataAtomic) iterator.next();
		assertEquals(dataAtomicChild.getNameInData(), "atomicNameInData",
				"NameInData should be the one set in the DataAtomic, first child of dataGroup");

		DataGroup dataGroupChild = (DataGroup) iterator.next();
		assertEquals(dataGroupChild.getNameInData(), "childNameInData",
				"NameInData should be the one set in dataGroup2");

		assertEquals(dataGroupChild.getChildren().stream().findAny().get().getNameInData(),
				"grandChildNameInData",
				"NameInData should be the one set in the child of dataGroup2");
	}

	@Test
	public void testExtractGroup() {
		SpiderDataGroup spiderDataGroup = SpiderDataGroup.withNameInData("nameInData");
		spiderDataGroup.addChild(
				SpiderDataAtomic.withNameInDataAndValue("atomicNameInData", "atomicValue"));
		SpiderDataGroup dataGroup2 = SpiderDataGroup.withNameInData("childNameInData");
		dataGroup2.addChild(SpiderDataGroup.withNameInData("grandChildNameInData"));
		spiderDataGroup.addChild(dataGroup2);
		assertEquals(spiderDataGroup.extractGroup("childNameInData"), dataGroup2);
	}

	@Test(expectedExceptions = DataMissingException.class)
	public void testExtractGroupNotFound() {
		SpiderDataGroup spiderDataGroup = SpiderDataGroup.withNameInData("nameInData");
		spiderDataGroup.addChild(
				SpiderDataAtomic.withNameInDataAndValue("atomicNameInData", "atomicValue"));
		SpiderDataGroup dataGroup2 = SpiderDataGroup.withNameInData("childNameInData");
		dataGroup2.addChild(SpiderDataGroup.withNameInData("grandChildNameInData"));
		spiderDataGroup.addChild(dataGroup2);
		spiderDataGroup.extractGroup("childNameInData_NOT_FOUND");
	}

	@Test
	public void testGetFirstChildWithNameInData() {
		SpiderDataGroup spiderDataGroup = SpiderDataGroup.withNameInData("nameInData");
		spiderDataGroup.addChild(
				SpiderDataAtomic.withNameInDataAndValue("atomicNameInData", "atomicValue"));
		SpiderDataGroup dataGroup2 = SpiderDataGroup.withNameInData("childNameInData");
		dataGroup2.addChild(SpiderDataGroup.withNameInData("grandChildNameInData"));
		spiderDataGroup.addChild(dataGroup2);
		assertEquals(spiderDataGroup.getFirstChildWithNameInData("childNameInData"), dataGroup2);
	}

	@Test(expectedExceptions = DataMissingException.class)
	public void testGetFirstChildWithNameInDataNotFound() {
		SpiderDataGroup spiderDataGroup = SpiderDataGroup.withNameInData("nameInData");
		spiderDataGroup.addChild(
				SpiderDataAtomic.withNameInDataAndValue("atomicNameInData", "atomicValue"));
		SpiderDataGroup dataGroup2 = SpiderDataGroup.withNameInData("childNameInData");
		dataGroup2.addChild(SpiderDataGroup.withNameInData("grandChildNameInData"));
		spiderDataGroup.addChild(dataGroup2);
		spiderDataGroup.getFirstChildWithNameInData("childNameInData_NOT_FOUND");
	}

	@Test
	public void testExtractAtomicValue() {
		SpiderDataGroup spiderDataGroup = SpiderDataGroup.withNameInData("nameInData");
		spiderDataGroup.addChild(
				SpiderDataAtomic.withNameInDataAndValue("atomicNameInData", "atomicValue"));
		assertEquals(spiderDataGroup.extractAtomicValue("atomicNameInData"), "atomicValue");
	}

	@Test(expectedExceptions = DataMissingException.class)
	public void testExtractAtomicValueNotFound() {
		SpiderDataGroup spiderDataGroup = SpiderDataGroup.withNameInData("nameInData");
		spiderDataGroup.addChild(
				SpiderDataAtomic.withNameInDataAndValue("atomicNameInData", "atomicValue"));
		spiderDataGroup.extractAtomicValue("atomicNameInData_NOT_FOUND");
	}
}
