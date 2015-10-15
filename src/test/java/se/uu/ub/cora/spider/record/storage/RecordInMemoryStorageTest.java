package se.uu.ub.cora.spider.record.storage;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertFalse;

import java.util.HashMap;
import java.util.Map;

import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import se.uu.ub.cora.metadataformat.data.DataAtomic;
import se.uu.ub.cora.metadataformat.data.DataGroup;
import se.uu.ub.cora.metadataformat.data.DataRecordLink;
import se.uu.ub.cora.spider.testdata.TestDataRecordInMemoryStorage;

public class RecordInMemoryStorageTest {
	private RecordStorage recordsInMemory;
	private DataGroup emptyLinkList = DataGroup.withNameInData("collectedDataLinks");

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

	@Test
	public void testCreateWithLink() {
		DataGroup dataGroup = createDataGroupWithRecordInfo();

		DataGroup linkList = DataGroup.withNameInData("collectedDataLinks");
		DataGroup recordToRecordLink = DataGroup.withNameInData("recordToRecordLink");
		linkList.addChild(recordToRecordLink);

		DataRecordLink from = DataRecordLink.withNameInDataAndRecordTypeAndRecordId("from",
				"fromRecordType", "fromRecordId");
		recordToRecordLink.addChild(from);

		DataRecordLink to = DataRecordLink.withNameInDataAndRecordTypeAndRecordId("to",
				"toRecordType", "toRecordId");
		recordToRecordLink.addChild(to);

		DataGroup recordToRecordLink2 = DataGroup.withNameInData("recordToRecordLink");
		linkList.addChild(recordToRecordLink2);

		DataRecordLink from2 = DataRecordLink.withNameInDataAndRecordTypeAndRecordId("from",
				"fromRecordType", "fromRecordId");
		recordToRecordLink2.addChild(from2);

		DataRecordLink to2 = DataRecordLink.withNameInDataAndRecordTypeAndRecordId("to",
				"toRecordType", "toRecordId");
		recordToRecordLink2.addChild(to2);

		recordsInMemory.create("fromRecordType", "fromRecordId", dataGroup, linkList);

		DataGroup readLinkList = recordsInMemory.readLinkList("fromRecordType", "fromRecordId");
		assertEquals(readLinkList.getChildren().size(), 2);

		DataGroup generatedLinksPointToRecord = recordsInMemory
				.generateLinkCollectionPointingToRecord("toRecordType", "toRecordId");
		assertEquals(generatedLinksPointToRecord.getChildren().size(), 1);

		DataGroup recordToRecordLinkOut = (DataGroup) generatedLinksPointToRecord.getChildren()
				.get(0);
		assertEquals(recordToRecordLinkOut.getNameInData(), "recordToRecordLink");
		DataRecordLink fromOut = (DataRecordLink) recordToRecordLinkOut
				.getFirstChildWithNameInData("from");
		assertEquals(fromOut.getRecordType(), "fromRecordType");
		assertEquals(fromOut.getRecordId(), "fromRecordId");

		DataRecordLink toOut = (DataRecordLink) recordToRecordLinkOut
				.getFirstChildWithNameInData("to");
		assertEquals(toOut.getRecordType(), "toRecordType");
		assertEquals(toOut.getRecordId(), "toRecordId");

		DataGroup generatedLinksPointToRecord1 = recordsInMemory
				.generateLinkCollectionPointingToRecord("toRecordType", "toRecordId");
		assertEquals(generatedLinksPointToRecord1.getChildren().size(), 1);
		DataGroup generatedLinksPointToRecord2 = recordsInMemory
				.generateLinkCollectionPointingToRecord("toRecordType", "toRecordId2");
		assertEquals(generatedLinksPointToRecord2.getChildren().size(), 0);
		DataGroup generatedLinksPointToRecord3 = recordsInMemory
				.generateLinkCollectionPointingToRecord("toRecordType2", "toRecordId2");
		assertEquals(generatedLinksPointToRecord3.getChildren().size(), 0);
	}

	@Test
	public void testCreateWithOutLink() {
		DataGroup dataGroup = createDataGroupWithRecordInfo();

		DataGroup linkList = DataGroup.withNameInData("collectedDataLinks");
		recordsInMemory.create("fromRecordType", "fromRecordId", dataGroup, linkList);

		DataGroup readLinkList = recordsInMemory.readLinkList("fromRecordType", "fromRecordId");
		assertEquals(readLinkList.getChildren().size(), 0);
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

		recordsInMemory.create("type", "place:0001", dataGroup, emptyLinkList);
		DataGroup dataGroupOut = recordsInMemory.read("type", "place:0001");
		assertEquals(dataGroupOut.getNameInData(), dataGroup.getNameInData());
	}

	@Test
	public void testCreateTworecordsRead() {

		DataGroup dataGroup = createDataGroupWithRecordInfo();

		recordsInMemory.create("type", "place:0001", dataGroup, emptyLinkList);
		recordsInMemory.create("type", "place:0002", dataGroup, emptyLinkList);

		DataGroup dataGroupOut = recordsInMemory.read("type", "place:0001");
		assertEquals(dataGroupOut.getNameInData(), dataGroup.getNameInData());

		DataGroup dataGroupOut2 = recordsInMemory.read("type", "place:0002");
		assertEquals(dataGroupOut2.getNameInData(), dataGroup.getNameInData());
	}

	@Test
	public void testCreateDataInStorageShouldBeIndependent() {
		DataGroup dataGroup = createDataGroupWithRecordInfo();
		dataGroup.addChild(DataAtomic.withNameInDataAndValue("childId", "childValue"));
		recordsInMemory.create("type", "place:0001", dataGroup, emptyLinkList);

		dataGroup.getChildren().clear();

		DataGroup dataGroupOut = recordsInMemory.read("type", "place:0001");
		DataAtomic child = (DataAtomic) dataGroupOut.getChildren().get(1);

		assertEquals(child.getValue(), "childValue");
	}

	@Test(expectedExceptions = RecordConflictException.class)
	public void testCreateConflict() {
		DataGroup dataGroup = createDataGroupWithRecordInfo();
		recordsInMemory.create("type", "place1", dataGroup, emptyLinkList);
		recordsInMemory.create("type", "place1", dataGroup, emptyLinkList);
	}

	@Test
	public void testDelete() {
		DataGroup dataGroup = createDataGroupWithRecordInfo();

		recordsInMemory.create("type", "place:0001", dataGroup, emptyLinkList);
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

		recordsInMemory.create("type", "place:0001", dataGroup, emptyLinkList);
		DataGroup dataGroupOut = recordsInMemory.read("type", "place:0001");
		assertEquals(dataGroupOut.getNameInData(), dataGroup.getNameInData());

		recordsInMemory.deleteByTypeAndId("type", "place:0001_NOT_FOUND");
	}

	@Test
	public void testUpdate() {
		DataGroup dataGroup = createDataGroupWithRecordInfo();
		dataGroup.addChild(DataAtomic.withNameInDataAndValue("childId", "childValue"));
		recordsInMemory.create("type", "place:0001", dataGroup, emptyLinkList);

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
		recordsInMemory.create("type", "place:0001", dataGroup, emptyLinkList);
		recordsInMemory.update("type", "place:0002", dataGroup);
	}

	@Test
	public void testUpdateDataInStorageShouldBeIndependent() {
		DataGroup dataGroup = createDataGroupWithRecordInfo();
		dataGroup.addChild(DataAtomic.withNameInDataAndValue("childId", "childValue"));
		recordsInMemory.create("type", "place:0001", dataGroup, emptyLinkList);
		recordsInMemory.update("type", "place:0001", dataGroup);

		dataGroup.getChildren().clear();

		DataGroup dataGroupOut = recordsInMemory.read("type", "place:0001");
		DataAtomic child = (DataAtomic) dataGroupOut.getChildren().get(1);

		assertEquals(child.getValue(), "childValue");
	}
}
