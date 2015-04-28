package epc.spider.data;

import static org.testng.Assert.assertEquals;

import java.util.List;

import org.testng.annotations.Test;

public class SpiderRecordListTest {
	@Test
	public void testInit() {
		String containRecordsOfType = "metadata";
		SpiderRecordList spiderRecordList = SpiderRecordList
				.withContainRecordsOfType(containRecordsOfType);
		assertEquals(spiderRecordList.getContainRecordsOfType(), "metadata");
	}

	@Test
	public void testAddRecord() {
		SpiderRecordList spiderRecordList = SpiderRecordList.withContainRecordsOfType("metadata");
		SpiderDataGroup spiderDataGroup = SpiderDataGroup.withDataId("spiderDataGroupId");
		SpiderDataRecord record = SpiderDataRecord.withSpiderDataGroup(spiderDataGroup);
		spiderRecordList.addRecord(record);
		List<SpiderDataRecord> records = spiderRecordList.getRecords();
		assertEquals(records.get(0), record);
	}

	@Test
	public void testTotalNo() {
		SpiderRecordList spiderRecordList = SpiderRecordList.withContainRecordsOfType("metadata");
		spiderRecordList.setTotalNo("2");
		assertEquals(spiderRecordList.getTotalNumberOfTypeInStorage(), "2");
	}

	@Test
	public void testFromNo() {
		SpiderRecordList spiderRecordList = SpiderRecordList.withContainRecordsOfType("metadata");
		spiderRecordList.setFromNo("0");
		assertEquals(spiderRecordList.getFromNo(), "0");
	}

	@Test
	public void testToNo() {
		SpiderRecordList spiderRecordList = SpiderRecordList.withContainRecordsOfType("metadata");
		spiderRecordList.setToNo("2");
		assertEquals(spiderRecordList.getToNo(), "2");
	}
}
