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
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import se.uu.ub.cora.data.DataAtomic;
import se.uu.ub.cora.data.DataAtomicProvider;
import se.uu.ub.cora.data.DataElement;
import se.uu.ub.cora.data.DataGroup;
import se.uu.ub.cora.data.DataGroupFactory;
import se.uu.ub.cora.data.DataGroupProvider;

public class SpiderDataGroupTest {
	private DataGroupFactory dataGroupFactory;
	private DataAtomicFactorySpy dataAtomicFactory;

	@BeforeMethod
	public void setUp() {
		dataGroupFactory = new DataGroupFactorySpy();
		DataGroupProvider.setDataGroupFactory(dataGroupFactory);
		dataAtomicFactory = new DataAtomicFactorySpy();
		DataAtomicProvider.setDataAtomicFactory(dataAtomicFactory);
	}

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
	public void testRemoveChildWithId() {
		SpiderDataGroup dataGroup = SpiderDataGroup.withNameInData("nameInData");
		SpiderDataElement child = SpiderDataAtomic.withNameInDataAndValue("childId", "child value");
		dataGroup.addChild(child);
		dataGroup.removeChild("childId");
		assertFalse(dataGroup.containsChildWithNameInData("childId"));
	}

	@Test(expectedExceptions = DataMissingException.class)
	public void testRemoveChildWithIdNotFound() {
		SpiderDataGroup dataGroup = SpiderDataGroup.withNameInData("nameInData");
		dataGroup.removeChild("childId");
		assertFalse(dataGroup.containsChildWithNameInData("childId"));
	}

	@Test
	public void testFromDataGroup() {
		DataGroup dataGroup = new DataGroupSpy("nameInData");
		SpiderDataGroup spiderDataGroup = SpiderDataGroup.fromDataGroup(dataGroup);
		assertEquals(spiderDataGroup.getNameInData(), "nameInData");
	}

	@Test
	public void testFromDataGroupWithRepeatId() {
		DataGroup dataGroup = new DataGroupSpy("nameInData");
		dataGroup.setRepeatId("puh");
		SpiderDataGroup spiderDataGroup = SpiderDataGroup.fromDataGroup(dataGroup);
		assertEquals(spiderDataGroup.getNameInData(), "nameInData");
		assertEquals(spiderDataGroup.getRepeatId(), "puh");
	}

	@Test
	public void testFromDataGroupWithAttribute() {
		DataGroup dataGroup = new DataGroupSpy("groupNameInData");
		dataGroup.addAttributeByIdWithValue("nameInData", "value");
		SpiderDataGroup spiderDataGroup = SpiderDataGroup.fromDataGroup(dataGroup);
		Map<String, String> attributes = spiderDataGroup.getAttributes();
		Entry<String, String> entry = attributes.entrySet().iterator().next();
		assertEquals(entry.getKey(), "nameInData");
		assertEquals(entry.getValue(), "value");
	}

	@Test
	public void testFromDataGroupWithChild() {
		DataGroup dataGroup = new DataGroupSpy("groupNameInData");
		dataGroup.addChild(new DataAtomicSpy("childNameInData", "atomicValue"));
		SpiderDataGroup spiderDataGroup = SpiderDataGroup.fromDataGroup(dataGroup);
		Assert.assertEquals(spiderDataGroup.getChildren().stream().findAny().get().getNameInData(),
				"childNameInData");
	}

	@Test
	public void testFromDataGroupWithDataRecordLinkChild() {
		DataGroup dataGroup = new DataGroupSpy("groupNameInData");
		dataGroup.addChild(createRecordLink());
		SpiderDataGroup spiderDataGroup = SpiderDataGroup.fromDataGroup(dataGroup);
		SpiderDataElement spiderDataElement = spiderDataGroup.getChildren().get(0);
		assertEquals(spiderDataElement.getNameInData(), "childNameInData");

		SpiderDataRecordLink spiderDataRecordLink = (SpiderDataRecordLink) spiderDataElement;
		SpiderDataAtomic linkedRecordType = (SpiderDataAtomic) spiderDataRecordLink
				.getFirstChildWithNameInData("linkedRecordType");
		assertEquals(linkedRecordType.getValue(), "aRecordType");
		SpiderDataAtomic linkedRecordId = (SpiderDataAtomic) spiderDataRecordLink
				.getFirstChildWithNameInData("linkedRecordId");
		assertEquals(linkedRecordId.getValue(), "aRecordId");

	}

	private DataGroup createRecordLink() {
		DataGroup dataRecordLink = new DataGroupSpy("childNameInData");

		DataAtomic linkedRecordType = new DataAtomicSpy("linkedRecordType", "aRecordType");
		dataRecordLink.addChild(linkedRecordType);

		DataAtomic linkedRecordId = new DataAtomicSpy("linkedRecordId", "aRecordId");
		dataRecordLink.addChild(linkedRecordId);

		return dataRecordLink;
	}

	@Test
	public void testFromDataGroupWithNonCompleteDataRecordLinkChild() {
		DataGroup dataGroup = new DataGroupSpy("groupNameInData");

		DataGroup dataRecordLinkWithNoLinkedRecordId = new DataGroupSpy("childNameInData");
		DataAtomic linkedRecordType = new DataAtomicSpy("linkedRecordType", "aRecordType");
		dataRecordLinkWithNoLinkedRecordId.addChild(linkedRecordType);
		dataGroup.addChild(dataRecordLinkWithNoLinkedRecordId);

		SpiderDataGroup spiderDataGroup = SpiderDataGroup.fromDataGroup(dataGroup);
		assertFalse(spiderDataGroup instanceof SpiderDataRecordLink);
	}

	@Test
	public void testFromDataGroupWithDataResourceLinkChild() {
		DataGroup dataGroup = new DataGroupSpy("groupNameInData");
		dataGroup.addChild(createResourceLink());
		SpiderDataGroup spiderDataGroup = SpiderDataGroup.fromDataGroup(dataGroup);
		SpiderDataElement spiderDataElement = spiderDataGroup.getChildren().get(0);
		assertEquals(spiderDataElement.getNameInData(), "childNameInData");

		SpiderDataResourceLink spiderDataResourceLink = (SpiderDataResourceLink) spiderDataElement;
		SpiderDataAtomic streamId = (SpiderDataAtomic) spiderDataResourceLink
				.getFirstChildWithNameInData("streamId");
		assertEquals(streamId.getValue(), "aStreamId");
		assertTrue(spiderDataResourceLink instanceof SpiderDataResourceLink);

	}

	private DataGroup createResourceLink() {
		DataGroup dataResourceLink = new DataGroupSpy("childNameInData");

		dataResourceLink.addChild(new DataAtomicSpy("streamId", "aStreamId"));
		dataResourceLink.addChild(new DataAtomicSpy("filename", "aFileName"));
		dataResourceLink.addChild(new DataAtomicSpy("filesize", "12345"));
		dataResourceLink.addChild(new DataAtomicSpy("mimeType", "application/pdf"));

		return dataResourceLink;
	}

	@Test
	public void testFromDataGroupWithDataIncompleteResourceLinkChildMissingStreamId() {
		DataGroup resourceLink = createResourceLinkNoStreamId();
		createAndAssertNotResourceLink(resourceLink);
	}

	private void createAndAssertNotResourceLink(DataGroup resourceLink) {
		DataGroup dataGroup = new DataGroupSpy("groupNameInData");
		dataGroup.addChild(resourceLink);
		SpiderDataGroup spiderDataGroup = SpiderDataGroup.fromDataGroup(dataGroup);
		SpiderDataElement spiderDataElement = spiderDataGroup
				.getFirstChildWithNameInData("childNameInData");
		assertFalse(spiderDataElement instanceof SpiderDataResourceLink);
	}

	private DataGroup createResourceLinkNoStreamId() {
		DataGroup dataResourceLink = new DataGroupSpy("childNameInData");

		// dataResourceLink.addChild(new DataAtomicSpy("streamId",
		// "aStreamId"));
		dataResourceLink.addChild(new DataAtomicSpy("filename", "aFileName"));
		dataResourceLink.addChild(new DataAtomicSpy("filesize", "12345"));
		dataResourceLink.addChild(new DataAtomicSpy("mimeType", "application/pdf"));

		return dataResourceLink;
	}

	@Test
	public void testFromDataGroupWithDataIncompleteResourceLinkChildMissingFilename() {
		DataGroup resourceLink = createResourceLinkNoFilename();
		createAndAssertNotResourceLink(resourceLink);
	}

	private DataGroup createResourceLinkNoFilename() {
		DataGroup dataResourceLink = new DataGroupSpy("childNameInData");

		dataResourceLink.addChild(new DataAtomicSpy("streamId", "aStreamId"));
		// dataResourceLink.addChild(new DataAtomicSpy("filename",
		// "aFileName"));
		dataResourceLink.addChild(new DataAtomicSpy("filesize", "12345"));
		dataResourceLink.addChild(new DataAtomicSpy("mimeType", "application/pdf"));

		return dataResourceLink;
	}

	@Test
	public void testFromDataGroupWithDataIncompleteResourceLinkChildMissingFilesize() {
		DataGroup resourceLink = createResourceLinkNoFilesize();
		createAndAssertNotResourceLink(resourceLink);
	}

	private DataGroup createResourceLinkNoFilesize() {
		DataGroup dataResourceLink = new DataGroupSpy("childNameInData");

		dataResourceLink.addChild(new DataAtomicSpy("streamId", "aStreamId"));
		dataResourceLink.addChild(new DataAtomicSpy("filename", "aFileName"));
		// dataResourceLink.addChild(new DataAtomicSpy("filesize",
		// "12345"));
		dataResourceLink.addChild(new DataAtomicSpy("mimeType", "application/pdf"));

		return dataResourceLink;
	}

	@Test
	public void testFromDataGroupWithDataIncompleteResourceLinkChildMissingMimeType() {
		DataGroup resourceLink = createResourceLinkNoMimeType();
		createAndAssertNotResourceLink(resourceLink);
	}

	private DataGroup createResourceLinkNoMimeType() {
		DataGroup dataResourceLink = new DataGroupSpy("childNameInData");

		dataResourceLink.addChild(new DataAtomicSpy("streamId", "aStreamId"));
		dataResourceLink.addChild(new DataAtomicSpy("filename", "aFileName"));
		dataResourceLink.addChild(new DataAtomicSpy("filesize", "12345"));
		// dataResourceLink.addChild(new DataAtomicSpy("mimeType",
		// "application/pdf"));

		return dataResourceLink;
	}

	@Test
	public void testFromDataGroupLevelsOfChildren() {
		DataGroup dataGroup = new DataGroupSpy("nameInData");
		dataGroup.addChild(new DataAtomicSpy("atomicNameInData", "atomicValue"));
		DataGroup dataGroup2 = new DataGroupSpy("childNameInData");
		dataGroup2.addChild(new DataGroupSpy("grandChildNameInData"));
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

		SpiderDataRecordLink spiderRecordLink = SpiderDataRecordLink
				.withNameInData("childNameInData");
		SpiderDataAtomic linkedRecordType = SpiderDataAtomic
				.withNameInDataAndValue("linkedRecordType", "aRecordType");
		spiderRecordLink.addChild(linkedRecordType);

		SpiderDataAtomic linkedRecordId = SpiderDataAtomic.withNameInDataAndValue("linkedRecordId",
				"aRecordId");
		spiderRecordLink.addChild(linkedRecordId);

		spiderDataGroup.addChild(spiderRecordLink);

		DataGroup dataGroup = spiderDataGroup.toDataGroup();

		List<DataElement> children = dataGroup.getChildren();
		DataElement childElementOut = children.get(0);
		assertEquals(childElementOut.getNameInData(), "childNameInData");
		DataGroup dataRecordLink = (DataGroup) childElementOut;
		assertEquals(dataRecordLink.getFirstAtomicValueWithNameInData("linkedRecordType"),
				"aRecordType");
		assertEquals(dataRecordLink.getFirstAtomicValueWithNameInData("linkedRecordId"),
				"aRecordId");

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

	@Test
	public void testGetAllGroupsWithNameInData() {
		SpiderDataGroup spiderDataGroup = SpiderDataGroup.withNameInData("nameInData");
		spiderDataGroup.addChild(
				SpiderDataAtomic.withNameInDataAndValue("atomicNameInData", "atomicValue"));
		addTwoGroupChildrenWithSameNameInData(spiderDataGroup);

		List<SpiderDataGroup> groupsFound = spiderDataGroup
				.getAllGroupsWithNameInData("childNameInData");
		assertEquals(groupsFound.size(), 2);
	}

	private void addTwoGroupChildrenWithSameNameInData(SpiderDataGroup spiderDataGroup) {
		SpiderDataGroup dataGroup = SpiderDataGroup.withNameInData("childNameInData");
		dataGroup.addChild(SpiderDataAtomic.withNameInDataAndValue("firstName", "someName"));
		dataGroup.setRepeatId("0");
		spiderDataGroup.addChild(dataGroup);
		SpiderDataGroup dataGroup2 = SpiderDataGroup.withNameInData("childNameInData");
		dataGroup2.addChild(SpiderDataAtomic.withNameInDataAndValue("firstName", "someOtherName"));
		dataGroup2.setRepeatId("1");
		spiderDataGroup.addChild(dataGroup2);
	}

	@Test
	public void testGetAllGroupsWithNameInDataNoMatches() {
		SpiderDataGroup spiderDataGroup = SpiderDataGroup.withNameInData("nameInData");
		spiderDataGroup.addChild(
				SpiderDataAtomic.withNameInDataAndValue("atomicNameInData", "atomicValue"));

		List<SpiderDataGroup> groupsFound = spiderDataGroup
				.getAllGroupsWithNameInData("childNameInData");
		assertEquals(groupsFound.size(), 0);
	}

}
