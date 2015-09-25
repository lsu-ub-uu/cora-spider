package epc.spider.record.storage;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertFalse;

import java.util.HashMap;
import java.util.Map;

import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import epc.metadataformat.data.DataAtomic;
import epc.metadataformat.data.DataGroup;
import epc.spider.testdata.TestDataRecordInMemoryStorage;

public class RecordInMemoryStorageTest {
	private RecordStorage recordsInMemory;

	@BeforeMethod
	public void beforeMethod() {
		recordsInMemory = new RecordStorageInMemory();
	}

	@Test
	public void testInitWithData() {
		Map<String, Map<String, DataGroup>> records = new HashMap<>();
		records.put("place", new HashMap<String, DataGroup>());

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

		DataGroup dataGroup = createDataGroupWithRecordInfo();

		records.get("place").put("place:0001", dataGroup);

		RecordStorageInMemory recordsInMemoryWithData = new RecordStorageInMemory(records);
		assertEquals(recordsInMemoryWithData.read("place", "place:0001"), dataGroup,
				"dataGroup should be the one added on startup");

	}

	private DataGroup createDataGroupWithRecordInfo() {

		DataGroup recordInfo = DataGroup.withNameInData("recordInfo");
		recordInfo.addChild(DataAtomic.withNameInDataAndValue("type", "place"));
		recordInfo.addChild(DataAtomic.withNameInDataAndValue("id", "place:0001"));

		DataGroup dataGroup = DataGroup.withNameInData("nameInData");
		dataGroup.addChild(recordInfo);
		return dataGroup;
	}

	@Test(expectedExceptions = IllegalArgumentException.class)
	public void testInitWithNull() {
		new RecordStorageInMemory(null);
	}

	@Test(expectedExceptions = RecordNotFoundException.class)
	public void testReadMissingrecordType() {
		recordsInMemory.read("", "");
	}

	@Test(expectedExceptions = RecordNotFoundException.class)
	public void testReadMissingrecordId() {
		RecordStorageInMemory recordsInMemoryWithTestData = TestDataRecordInMemoryStorage
				.createRecordStorageInMemoryWithTestData();
		recordsInMemoryWithTestData.read("place", "");
	}

	@Test
	public void testCreateRead() {

		DataGroup dataGroup = createDataGroupWithRecordInfo();

		recordsInMemory.create("type", "place:0001", dataGroup);
		DataGroup dataGroupOut = recordsInMemory.read("type", "place:0001");
		assertEquals(dataGroupOut.getNameInData(), dataGroup.getNameInData());
	}

	@Test
	public void testCreateTworecordsRead() {

		DataGroup dataGroup = createDataGroupWithRecordInfo();

		recordsInMemory.create("type", "place:0001", dataGroup);
		recordsInMemory.create("type", "place:0002", dataGroup);

		DataGroup dataGroupOut = recordsInMemory.read("type", "place:0001");
		assertEquals(dataGroupOut.getNameInData(), dataGroup.getNameInData());

		DataGroup dataGroupOut2 = recordsInMemory.read("type", "place:0002");
		assertEquals(dataGroupOut2.getNameInData(), dataGroup.getNameInData());
	}

	@Test
	public void testCreateDataInStorageShouldBeIndependent() {
		DataGroup dataGroup = createDataGroupWithRecordInfo();
		dataGroup.addChild(DataAtomic.withNameInDataAndValue("childId", "childValue"));
		recordsInMemory.create("type", "place:0001", dataGroup);

		dataGroup.getChildren().clear();

		DataGroup dataGroupOut = recordsInMemory.read("type", "place:0001");
		DataAtomic child = (DataAtomic) dataGroupOut.getChildren().get(1);

		assertEquals(child.getValue(), "childValue");
	}

	@Test
	public void testDelete() {
		DataGroup dataGroup = createDataGroupWithRecordInfo();

		recordsInMemory.create("type", "place:0001", dataGroup);
		DataGroup dataGroupOut = recordsInMemory.read("type", "place:0001");
		assertEquals(dataGroupOut.getNameInData(), dataGroup.getNameInData());

		recordsInMemory.deleteByTypeAndId("type", "place:0001");

		boolean recordFound = true;
		try {
			recordsInMemory.read("type", "place:0001");
			recordFound = true;

		} catch (RecordNotFoundException e) {
			recordFound = false;
		}
		assertFalse(recordFound);
	}

	@Test(expectedExceptions = RecordNotFoundException.class)
	public void testDeleteNotFound() {
		DataGroup dataGroup = createDataGroupWithRecordInfo();

		recordsInMemory.create("type", "place:0001", dataGroup);
		DataGroup dataGroupOut = recordsInMemory.read("type", "place:0001");
		assertEquals(dataGroupOut.getNameInData(), dataGroup.getNameInData());

		recordsInMemory.deleteByTypeAndId("type", "place:0001_NOT_FOUND");
	}

	@Test
	public void testUpdate() {
		DataGroup dataGroup = createDataGroupWithRecordInfo();
		dataGroup.addChild(DataAtomic.withNameInDataAndValue("childId", "childValue"));
		recordsInMemory.create("type", "place:0001", dataGroup);

		DataGroup dataGroupOut = recordsInMemory.read("type", "place:0001");
		DataAtomic child = (DataAtomic) dataGroupOut.getChildren().get(1);

		DataGroup dataGroup2 = createDataGroupWithRecordInfo();
		dataGroup2.addChild(DataAtomic.withNameInDataAndValue("childId2", "childValue2"));
		recordsInMemory.update("type", "place:0001", dataGroup2);

		DataGroup dataGroupOut2 = recordsInMemory.read("type", "place:0001");
		DataAtomic child2 = (DataAtomic) dataGroupOut2.getChildren().get(1);

		assertEquals(child.getValue(), "childValue");
		assertEquals(child2.getValue(), "childValue2");
	}

	@Test(expectedExceptions = RecordNotFoundException.class)
	public void testUpdateNotFoundType() {
		DataGroup dataGroup = createDataGroupWithRecordInfo();
		dataGroup.addChild(DataAtomic.withNameInDataAndValue("childId", "childValue"));
		recordsInMemory.update("type", "place:0001", dataGroup);
	}

	@Test(expectedExceptions = RecordNotFoundException.class)
	public void testUpdateNotFoundId() {
		DataGroup dataGroup = createDataGroupWithRecordInfo();
		dataGroup.addChild(DataAtomic.withNameInDataAndValue("childId", "childValue"));
		recordsInMemory.create("type", "place:0001", dataGroup);
		recordsInMemory.update("type", "place:0002", dataGroup);
	}

	@Test
	public void testUpdateDataInStorageShouldBeIndependent() {
		DataGroup dataGroup = createDataGroupWithRecordInfo();
		dataGroup.addChild(DataAtomic.withNameInDataAndValue("childId", "childValue"));
		recordsInMemory.create("type", "place:0001", dataGroup);
		recordsInMemory.update("type", "place:0001", dataGroup);

		dataGroup.getChildren().clear();

		DataGroup dataGroupOut = recordsInMemory.read("type", "place:0001");
		DataAtomic child = (DataAtomic) dataGroupOut.getChildren().get(1);

		assertEquals(child.getValue(), "childValue");
	}
}
