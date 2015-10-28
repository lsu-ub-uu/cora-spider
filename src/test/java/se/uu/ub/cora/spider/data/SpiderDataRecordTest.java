package se.uu.ub.cora.spider.data;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertTrue;

import java.util.Set;

import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import se.uu.ub.cora.bookkeeper.data.DataGroup;
import se.uu.ub.cora.bookkeeper.data.DataRecord;
import se.uu.ub.cora.spider.data.Action;
import se.uu.ub.cora.spider.data.SpiderDataGroup;
import se.uu.ub.cora.spider.data.SpiderDataRecord;

public class SpiderDataRecordTest {
	private SpiderDataRecord spiderDataRecord;

	@BeforeMethod
	public void beforeMethod() {
		SpiderDataGroup spiderDataGroup = SpiderDataGroup.withNameInData("nameInData");
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
		String nameInData = spiderDataRecord.getSpiderDataGroup().getNameInData();
		assertEquals(nameInData, "nameInData");
	}

	@Test
	public void testSpiderDataGroup() {
		SpiderDataGroup spiderDataGroup = SpiderDataGroup.withNameInData("nameInData");
		spiderDataRecord.setSpiderDataGroup(spiderDataGroup);
		assertEquals(spiderDataRecord.getSpiderDataGroup(), spiderDataGroup);
	}

	@Test
	public void testFromDataRecord() {
		DataRecord dataRecord = new DataRecord();
		dataRecord.setDataGroup(DataGroup.withNameInData("nameInData"));
		dataRecord.addKey("KEY1");
		dataRecord.addKey("KEY2");

		spiderDataRecord = SpiderDataRecord.fromDataRecord(dataRecord);
		Set<String> keys = spiderDataRecord.getKeys();
		assertTrue(keys.contains("KEY1"));
		assertTrue(keys.contains("KEY2"));

		String nameInData = spiderDataRecord.getSpiderDataGroup().getNameInData();
		assertEquals(nameInData, "nameInData");
	}

	@Test
	public void testToDataRecord() {
		spiderDataRecord.addKey("KEY1");
		spiderDataRecord.addKey("KEY2");
		DataRecord dataRecord = spiderDataRecord.toDataRecord();
		assertEquals(dataRecord.getDataGroup().getNameInData(), "nameInData");
		Set<String> keys = dataRecord.getKeys();
		assertTrue(keys.contains("KEY1"));
		assertTrue(keys.contains("KEY2"));
	}
}
