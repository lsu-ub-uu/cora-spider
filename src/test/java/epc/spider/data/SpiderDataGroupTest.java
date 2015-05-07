package epc.spider.data;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertNotNull;
import static org.testng.Assert.assertTrue;

import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.testng.Assert;
import org.testng.annotations.Test;

import epc.metadataformat.data.DataAtomic;
import epc.metadataformat.data.DataElement;
import epc.metadataformat.data.DataGroup;

public class SpiderDataGroupTest {
	@Test
	public void testInit() {
		SpiderDataGroup spiderDataGroup = SpiderDataGroup.withDataId("dataId");
		assertEquals(spiderDataGroup.getDataId(), "dataId");
		assertNotNull(spiderDataGroup.getAttributes());
		assertNotNull(spiderDataGroup.getChildren());
	}

	@Test
	public void testAddAttribute() {
		SpiderDataGroup spiderDataGroup = SpiderDataGroup.withDataId("dataId");
		spiderDataGroup.addAttributeByIdWithValue("dataId", "value");
		Map<String, String> attributes = spiderDataGroup.getAttributes();
		String key = attributes.keySet().iterator().next();
		String value = attributes.get(key);
		assertEquals(key, "dataId");
		assertEquals(value, "value");
	}

	@Test
	public void testAddChild() {
		SpiderDataGroup spiderDataGroup = SpiderDataGroup.withDataId("dataId");
		SpiderDataElement dataElement = SpiderDataAtomic.withDataIdAndValue("childDataId",
				"childValue");
		spiderDataGroup.addChild(dataElement);
		List<SpiderDataElement> children = spiderDataGroup.getChildren();
		SpiderDataElement childElementOut = children.get(0);
		assertEquals(childElementOut.getDataId(), "childDataId");
	}

	@Test
	public void testContainsChildWithId() {
		SpiderDataGroup dataGroup = SpiderDataGroup.withDataId("dataId");
		dataGroup.addChild(SpiderDataAtomic.withDataIdAndValue("otherChildId", "otherChildValue"));
		SpiderDataElement child = SpiderDataAtomic.withDataIdAndValue("childId", "child value");
		dataGroup.addChild(child);
		assertTrue(dataGroup.containsChildWithDataId("childId"));
	}

	@Test
	public void testContainsChildWithIdNotFound() {
		DataGroup dataGroup = DataGroup.withDataId("dataId");
		DataElement child = DataAtomic.withDataIdAndValue("childId", "child value");
		dataGroup.addChild(child);
		assertFalse(dataGroup.containsChildWithDataId("childId_NOT_FOUND"));
	}

	@Test
	public void testFromDataGroup() {
		DataGroup dataGroup = DataGroup.withDataId("dataId");
		SpiderDataGroup spiderDataGroup = SpiderDataGroup.fromDataGroup(dataGroup);
		assertEquals(spiderDataGroup.getDataId(), "dataId");
	}

	@Test
	public void testFromDataGroupWithAttribute() {
		DataGroup dataGroup = DataGroup.withDataId("groupDataId");
		dataGroup.addAttributeByIdWithValue("dataId", "value");
		SpiderDataGroup spiderDataGroup = SpiderDataGroup.fromDataGroup(dataGroup);
		Map<String, String> attributes = spiderDataGroup.getAttributes();
		String key = attributes.keySet().iterator().next();
		String value = attributes.get(key);
		assertEquals(key, "dataId");
		assertEquals(value, "value");
	}

	@Test
	public void testFromDataGroupWithChild() {
		DataGroup dataGroup = DataGroup.withDataId("groupDataId");
		dataGroup.addChild(DataAtomic.withDataIdAndValue("childDataId", "atomicValue"));
		SpiderDataGroup spiderDataGroup = SpiderDataGroup.fromDataGroup(dataGroup);
		Assert.assertEquals(spiderDataGroup.getChildren().stream().findAny().get().getDataId(),
				"childDataId");
	}

	@Test
	public void testFromDataGroupLevelsOfChildren() {
		DataGroup dataGroup = DataGroup.withDataId("dataId");
		dataGroup.addChild(DataAtomic.withDataIdAndValue("atomicDataId", "atomicValue"));
		DataGroup dataGroup2 = DataGroup.withDataId("childDataId");
		dataGroup2.addChild(DataGroup.withDataId("grandChildDataId"));
		dataGroup.addChild(dataGroup2);
		SpiderDataGroup spiderDataGroup = SpiderDataGroup.fromDataGroup(dataGroup);
		Iterator<SpiderDataElement> iterator = spiderDataGroup.getChildren().iterator();
		Assert.assertTrue(iterator.hasNext(), "spiderDataGroup should have at least one child");

		SpiderDataAtomic dataAtomicChild = (SpiderDataAtomic) iterator.next();
		Assert.assertEquals(dataAtomicChild.getDataId(), "atomicDataId",
				"DataId should be the one set in the DataAtomic, first child of dataGroup");

		SpiderDataGroup dataGroupChild = (SpiderDataGroup) iterator.next();
		Assert.assertEquals(dataGroupChild.getDataId(), "childDataId",
				"DataId should be the one set in dataGroup2");

		Assert.assertEquals(dataGroupChild.getChildren().stream().findAny().get().getDataId(),
				"grandChildDataId", "DataId should be the one set in the child of dataGroup2");
	}

	@Test
	public void testToDataGroup() {
		SpiderDataGroup spiderDataGroup = SpiderDataGroup.withDataId("dataId");
		DataGroup dataGroup = spiderDataGroup.toDataGroup();
		assertEquals(dataGroup.getDataId(), "dataId");
		assertNotNull(dataGroup.getAttributes());
		assertNotNull(dataGroup.getChildren());
	}

	@Test
	public void testToDataGroupWithAttribute() {
		SpiderDataGroup spiderDataGroup = SpiderDataGroup.withDataId("dataId");
		spiderDataGroup.addAttributeByIdWithValue("dataId", "value");

		DataGroup dataGroup = spiderDataGroup.toDataGroup();

		Map<String, String> attributes = dataGroup.getAttributes();
		String key = attributes.keySet().iterator().next();
		String value = attributes.get(key);
		assertEquals(key, "dataId");
		assertEquals(value, "value");
	}

	@Test
	public void testToDataGroupWithChild() {
		SpiderDataGroup spiderDataGroup = SpiderDataGroup.withDataId("dataId");
		SpiderDataElement dataElement = SpiderDataAtomic.withDataIdAndValue("childDataId",
				"childValue");
		spiderDataGroup.addChild(dataElement);

		DataGroup dataGroup = spiderDataGroup.toDataGroup();

		List<DataElement> children = dataGroup.getChildren();
		DataElement childElementOut = children.get(0);
		assertEquals(childElementOut.getDataId(), "childDataId");
	}

	@Test
	public void testToDataGroupWithLevelsOfChildren() {
		SpiderDataGroup spiderDataGroup = SpiderDataGroup.withDataId("dataId");
		spiderDataGroup
				.addChild(SpiderDataAtomic.withDataIdAndValue("atomicDataId", "atomicValue"));
		SpiderDataGroup dataGroup2 = SpiderDataGroup.withDataId("childDataId");
		dataGroup2.addChild(SpiderDataGroup.withDataId("grandChildDataId"));
		spiderDataGroup.addChild(dataGroup2);

		DataGroup dataGroup = spiderDataGroup.toDataGroup();
		Iterator<DataElement> iterator = dataGroup.getChildren().iterator();
		Assert.assertTrue(iterator.hasNext(), "dataGroup should have at least one child");

		DataAtomic dataAtomicChild = (DataAtomic) iterator.next();
		Assert.assertEquals(dataAtomicChild.getDataId(), "atomicDataId",
				"DataId should be the one set in the DataAtomic, first child of dataGroup");

		DataGroup dataGroupChild = (DataGroup) iterator.next();
		Assert.assertEquals(dataGroupChild.getDataId(), "childDataId",
				"DataId should be the one set in dataGroup2");

		Assert.assertEquals(dataGroupChild.getChildren().stream().findAny().get().getDataId(),
				"grandChildDataId", "DataId should be the one set in the child of dataGroup2");
	}

	@Test
	public void testExtractGroup() {
		SpiderDataGroup spiderDataGroup = SpiderDataGroup.withDataId("dataId");
		spiderDataGroup
				.addChild(SpiderDataAtomic.withDataIdAndValue("atomicDataId", "atomicValue"));
		SpiderDataGroup dataGroup2 = SpiderDataGroup.withDataId("childDataId");
		dataGroup2.addChild(SpiderDataGroup.withDataId("grandChildDataId"));
		spiderDataGroup.addChild(dataGroup2);
		assertEquals(spiderDataGroup.extractGroup("childDataId"), dataGroup2);
	}

	@Test(expectedExceptions = DataMissingException.class)
	public void testExtractGroupNotFound() {
		SpiderDataGroup spiderDataGroup = SpiderDataGroup.withDataId("dataId");
		spiderDataGroup
				.addChild(SpiderDataAtomic.withDataIdAndValue("atomicDataId", "atomicValue"));
		SpiderDataGroup dataGroup2 = SpiderDataGroup.withDataId("childDataId");
		dataGroup2.addChild(SpiderDataGroup.withDataId("grandChildDataId"));
		spiderDataGroup.addChild(dataGroup2);
		spiderDataGroup.extractGroup("childDataId_NOT_FOUND");
	}

	@Test
	public void testExtractAtomicValue() {
		SpiderDataGroup spiderDataGroup = SpiderDataGroup.withDataId("dataId");
		spiderDataGroup
				.addChild(SpiderDataAtomic.withDataIdAndValue("atomicDataId", "atomicValue"));
		assertEquals(spiderDataGroup.extractAtomicValue("atomicDataId"), "atomicValue");
	}

	@Test(expectedExceptions = DataMissingException.class)
	public void testExtractAtomicValueNotFound() {
		SpiderDataGroup spiderDataGroup = SpiderDataGroup.withDataId("dataId");
		spiderDataGroup
				.addChild(SpiderDataAtomic.withDataIdAndValue("atomicDataId", "atomicValue"));
		spiderDataGroup.extractAtomicValue("atomicDataId_NOT_FOUND");
	}
}
