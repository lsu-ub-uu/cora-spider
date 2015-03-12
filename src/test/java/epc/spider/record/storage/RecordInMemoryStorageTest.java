package epc.spider.record.storage;

import static org.testng.Assert.assertEquals;

import java.util.HashMap;
import java.util.Map;

import org.testng.annotations.Test;

import epc.spider.data.SpiderDataAtomic;
import epc.spider.data.SpiderDataGroup;
import epc.spider.testdata.TestDataRecordInMemoryStorage;

public class RecordInMemoryStorageTest {
	@Test
	public void testInitWithData() {
		Map<String, Map<String, SpiderDataGroup>> records = new HashMap<>();
		records.put("place", new HashMap<String, SpiderDataGroup>());

		SpiderDataGroup recordInfo = SpiderDataGroup.withDataId("recordInfo");
		recordInfo.addChild(SpiderDataAtomic.withDataIdAndValue("type", "place"));
		recordInfo.addChild(SpiderDataAtomic.withDataIdAndValue("id", "place:0001"));

		/**
		 * <pre>
		 * 		recordInfo
		 * 			type
		 * 			id
		 * 			organisation
		 * 			user
		 * 			tsCreated (recordCreatedDate)
		 * 			list tsUpdated (recordUpdatedDate)
		 * 			catalog Language
		 * </pre>
		 */

		SpiderDataGroup dataGroup = SpiderDataGroup.withDataId("dataId");
		dataGroup.addChild(recordInfo);

		records.get("place").put("place:0001", dataGroup);

		RecordStorageInMemory recordsInMemory = new RecordStorageInMemory(records);
		assertEquals(recordsInMemory.read("place", "place:0001"), dataGroup,
				"dataGroup should be the one added on startup");

	}

	@Test(expectedExceptions = IllegalArgumentException.class)
	public void testInitWithNull() {
		new RecordStorageInMemory(null);
	}

	@Test(expectedExceptions = RecordNotFoundException.class)
	public void testReadMissingrecordType() {
		RecordStorageInMemory recordsInMemory = new RecordStorageInMemory();
		recordsInMemory.read("", "");
	}

	@Test(expectedExceptions = RecordNotFoundException.class)
	public void testReadMissingrecordId() {
		RecordStorageInMemory recordsInMemory = TestDataRecordInMemoryStorage
				.createRecordStorageInMemoryWithTestData();
		recordsInMemory.read("place", "");
	}

	@Test
	public void testCreateRead() {

		SpiderDataGroup recordInfo = SpiderDataGroup.withDataId("recordInfo");
		recordInfo.addChild(SpiderDataAtomic.withDataIdAndValue("type", "place"));
		recordInfo.addChild(SpiderDataAtomic.withDataIdAndValue("id", "place:0001"));

		SpiderDataGroup dataGroup = SpiderDataGroup.withDataId("dataId");
		dataGroup.addChild(recordInfo);

		RecordStorageInMemory recordsInMemory = new RecordStorageInMemory();
		recordsInMemory.create("type", "place:0001", dataGroup);
		SpiderDataGroup dataGroupOut = recordsInMemory.read("type", "place:0001");
		assertEquals(dataGroupOut, dataGroup, "dataGroupOut should be the same as dataGroup");
	}

	@Test
	public void testCreateTworecordsRead() {

		SpiderDataGroup recordInfo = SpiderDataGroup.withDataId("recordInfo");
		recordInfo.addChild(SpiderDataAtomic.withDataIdAndValue("type", "place"));
		recordInfo.addChild(SpiderDataAtomic.withDataIdAndValue("id", "place:0001"));

		SpiderDataGroup dataGroup = SpiderDataGroup.withDataId("dataId");
		dataGroup.addChild(recordInfo);

		RecordStorageInMemory recordsInMemory = new RecordStorageInMemory();
		recordsInMemory.create("type", "place:0001", dataGroup);
		recordsInMemory.create("type", "place:0002", dataGroup);

		SpiderDataGroup dataGroupOut = recordsInMemory.read("type", "place:0001");
		assertEquals(dataGroupOut, dataGroup, "dataGroupOut should be the same as dataGroup");

		SpiderDataGroup dataGroupOut2 = recordsInMemory.read("type", "place:0002");
		assertEquals(dataGroupOut2, dataGroup, "dataGroupOut2 should be the same as dataGroup");

	}
}
