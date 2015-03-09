package epc.spider.record.storage;

import static org.testng.Assert.assertEquals;

import java.util.HashMap;
import java.util.Map;

import org.testng.annotations.Test;

import epc.metadataformat.data.DataAtomic;
import epc.metadataformat.data.DataGroup;
import epc.spider.record.storage.RecordStorageInMemory;
import epc.spider.record.storage.RecordNotFoundException;
import epc.spider.testdata.TestDataRecordInMemoryStorage;

public class RecordInMemoryStorageTest {
	@Test
	public void testInitWithData() {
		Map<String, Map<String, DataGroup>> records = new HashMap<>();
		records.put("place", new HashMap<String, DataGroup>());

		DataGroup recordInfo = DataGroup.withDataId("recordInfo");
		recordInfo.addChild(DataAtomic.withDataIdAndValue("type", "place"));
		recordInfo.addChild(DataAtomic.withDataIdAndValue("id", "place:0001"));

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

		DataGroup dataGroup = DataGroup.withDataId("dataId");
		dataGroup.addChild(recordInfo);

		records.get("place").put("place:0001", dataGroup);

		RecordStorageInMemory recordsInMemory = new RecordStorageInMemory(
				records);
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

		DataGroup recordInfo = DataGroup.withDataId("recordInfo");
		recordInfo.addChild(DataAtomic.withDataIdAndValue("type", "place"));
		recordInfo.addChild(DataAtomic.withDataIdAndValue("id", "place:0001"));

		DataGroup dataGroup = DataGroup.withDataId("dataId");
		dataGroup.addChild(recordInfo);

		RecordStorageInMemory recordsInMemory = new RecordStorageInMemory();
		recordsInMemory.create("type", "place:0001", dataGroup);
		DataGroup dataGroupOut = recordsInMemory.read("type", "place:0001");
		assertEquals(dataGroupOut, dataGroup,
				"dataGroupOut should be the same as dataGroup");
	}

	@Test
	public void testCreateTworecordsRead() {

		DataGroup recordInfo = DataGroup.withDataId("recordInfo");
		recordInfo.addChild(DataAtomic.withDataIdAndValue("type", "place"));
		recordInfo.addChild(DataAtomic.withDataIdAndValue("id", "place:0001"));

		DataGroup dataGroup = DataGroup.withDataId("dataId");
		dataGroup.addChild(recordInfo);

		RecordStorageInMemory recordsInMemory = new RecordStorageInMemory();
		recordsInMemory.create("type", "place:0001", dataGroup);
		recordsInMemory.create("type", "place:0002", dataGroup);

		DataGroup dataGroupOut = recordsInMemory.read("type", "place:0001");
		assertEquals(dataGroupOut, dataGroup,
				"dataGroupOut should be the same as dataGroup");

		DataGroup dataGroupOut2 = recordsInMemory.read("type", "place:0002");
		assertEquals(dataGroupOut2, dataGroup,
				"dataGroupOut2 should be the same as dataGroup");

	}
}
