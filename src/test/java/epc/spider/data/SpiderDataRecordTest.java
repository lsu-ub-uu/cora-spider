package epc.spider.data;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertTrue;

import java.util.Set;

import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import epc.metadataformat.data.DataGroup;
import epc.metadataformat.data.DataRecord;

public class SpiderDataRecordTest {
	private SpiderDataRecord spiderDataRecord;

	@BeforeMethod
	public void beforeMethod() {
		SpiderDataGroup spiderDataGroup = SpiderDataGroup.withDataId("dataId");
		spiderDataRecord = SpiderDataRecord.withSpiderDataGroup(spiderDataGroup);
	}

	@Test
	public void testKeys() {
		spiderDataRecord.addKey("KEY");
		assertTrue(spiderDataRecord.containsKey("KEY"));
	}

	@Test
	public void testGetKeys() {
		spiderDataRecord.addKey("KEY1");
		spiderDataRecord.addKey("KEY2");
		Set<String> keys = spiderDataRecord.getKeys();
		assertTrue(keys.contains("KEY1"));
		assertTrue(keys.contains("KEY2"));
	}

	@Test
	public void testAddAction() {
		spiderDataRecord.addAction(Action.READ);

		assertTrue(spiderDataRecord.getActions().contains(Action.READ));
		assertFalse(spiderDataRecord.getActions().contains(Action.DELETE));
		// small hack to get 100% coverage on enum
		Action.valueOf(Action.READ.toString());
	}

	@Test
	public void testGetSpiderDataGroup() {
		String dataId = spiderDataRecord.getSpiderDataGroup().getDataId();
		assertEquals(dataId, "dataId");
	}

	@Test
	public void testSpiderDataGroup() {
		SpiderDataGroup spiderDataGroup = SpiderDataGroup.withDataId("dataId");
		spiderDataRecord.setSpiderDataGroup(spiderDataGroup);
		assertEquals(spiderDataRecord.getSpiderDataGroup(), spiderDataGroup);
	}

	@Test
	public void testFromDataRecord() {
		DataRecord dataRecord = new DataRecord();
		dataRecord.setDataGroup(DataGroup.withDataId("dataId"));
		dataRecord.addKey("KEY1");
		dataRecord.addKey("KEY2");

		spiderDataRecord = SpiderDataRecord.fromDataRecord(dataRecord);
		Set<String> keys = spiderDataRecord.getKeys();
		assertTrue(keys.contains("KEY1"));
		assertTrue(keys.contains("KEY2"));

		String dataId = spiderDataRecord.getSpiderDataGroup().getDataId();
		assertEquals(dataId, "dataId");
	}

	@Test
	public void testToDataRecord() {
		spiderDataRecord.addKey("KEY1");
		spiderDataRecord.addKey("KEY2");
		DataRecord dataRecord = spiderDataRecord.toDataRecord();
		assertEquals(dataRecord.getDataGroup().getDataId(), "dataId");
		Set<String> keys = dataRecord.getKeys();
		assertTrue(keys.contains("KEY1"));
		assertTrue(keys.contains("KEY2"));
	}
}
