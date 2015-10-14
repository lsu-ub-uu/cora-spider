package se.uu.ub.cora.spider.data;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertNotNull;
import static org.testng.Assert.assertTrue;

import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.testng.Assert;
import org.testng.annotations.Test;

import se.uu.ub.cora.metadataformat.data.DataAtomic;
import se.uu.ub.cora.metadataformat.data.DataElement;
import se.uu.ub.cora.metadataformat.data.DataGroup;
import se.uu.ub.cora.metadataformat.data.DataRecordLink;

public class SpiderDataGroupTest {
	@Test
	public void testInit() {
		SpiderDataGroup spiderDataGroup = SpiderDataGroup.withNameInData("nameInData");
		assertEquals(spiderDataGroup.getNameInData(), "nameInData");
		assertNotNull(spiderDataGroup.getAttributes());
		assertNotNull(spiderDataGroup.getChildren());
	}

	@Test
	public void testAddAttribute() {
		SpiderDataGroup spiderDataGroup = SpiderDataGroup.withNameInData("nameInData");
		spiderDataGroup.addAttributeByIdWithValue("nameInData", "value");
		Map<String, String> attributes = spiderDataGroup.getAttributes();
		String key = attributes.keySet().iterator().next();
		String value = attributes.get(key);
		assertEquals(key, "nameInData");
		assertEquals(value, "value");
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
	public void testFromDataGroupWithAttribute() {
		DataGroup dataGroup = DataGroup.withNameInData("groupNameInData");
		dataGroup.addAttributeByIdWithValue("nameInData", "value");
		SpiderDataGroup spiderDataGroup = SpiderDataGroup.fromDataGroup(dataGroup);
		Map<String, String> attributes = spiderDataGroup.getAttributes();
		String key = attributes.keySet().iterator().next();
		String value = attributes.get(key);
		assertEquals(key, "nameInData");
		assertEquals(value, "value");
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
		dataGroup.addChild(DataRecordLink.withNameInDataAndRecordTypeAndRecordId("childNameInData",
				"aRecordType", "aRecordId"));
		SpiderDataGroup spiderDataGroup = SpiderDataGroup.fromDataGroup(dataGroup);
		SpiderDataElement spiderDataElement = spiderDataGroup.getChildren().get(0);
		assertEquals(spiderDataElement.getNameInData(), "childNameInData");

		SpiderDataRecordLink spiderDataRecordLink = (SpiderDataRecordLink) spiderDataElement;
		assertEquals(spiderDataRecordLink.getRecordType(), "aRecordType");
		assertEquals(spiderDataRecordLink.getRecordId(), "aRecordId");
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
	public void testToDataGroupWithAttribute() {
		SpiderDataGroup spiderDataGroup = SpiderDataGroup.withNameInData("nameInData");
		spiderDataGroup.addAttributeByIdWithValue("nameInData", "value");

		DataGroup dataGroup = spiderDataGroup.toDataGroup();

		Map<String, String> attributes = dataGroup.getAttributes();
		String key = attributes.keySet().iterator().next();
		String value = attributes.get(key);
		assertEquals(key, "nameInData");
		assertEquals(value, "value");
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
		SpiderDataElement dataElement = SpiderDataRecordLink.withNameInDataAndRecordTypeAndRecordId(
				"childNameInData", "aRecordType", "aRecordId");
		spiderDataGroup.addChild(dataElement);

		DataGroup dataGroup = spiderDataGroup.toDataGroup();

		List<DataElement> children = dataGroup.getChildren();
		DataElement childElementOut = children.get(0);
		assertEquals(childElementOut.getNameInData(), "childNameInData");
		DataRecordLink dataRecordLink = (DataRecordLink) childElementOut;
		assertEquals(dataRecordLink.getRecordType(), "aRecordType");
		assertEquals(dataRecordLink.getRecordId(), "aRecordId");

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
