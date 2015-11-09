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

package se.uu.ub.cora.spider.record.storage;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertFalse;

import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import se.uu.ub.cora.bookkeeper.data.DataAtomic;
import se.uu.ub.cora.bookkeeper.data.DataElement;
import se.uu.ub.cora.bookkeeper.data.DataGroup;
import se.uu.ub.cora.bookkeeper.data.DataRecordLink;
import se.uu.ub.cora.spider.testdata.DataCreator;
import se.uu.ub.cora.spider.testdata.TestDataRecordInMemoryStorage;

public class RecordStorageInMemoryTest {
	private RecordStorage recordStorage;
	private DataGroup emptyLinkList;

	@BeforeMethod
	public void beforeMethod() {
		recordStorage = new RecordStorageInMemory();
		emptyLinkList = DataCreator.createLinkList();
	}

	@Test
	public void testInitWithData() {
		Map<String, Map<String, DataGroup>> records = new HashMap<>();
		records.put("place", new HashMap<String, DataGroup>());

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
	public void testCreateAndReadLinkList() {
		DataGroup dataGroup = createDataGroupWithRecordInfo();
		DataGroup linkList = createLinkListWithTwoLinks("fromRecordId");
		recordStorage.create("fromRecordType", "fromRecordId", dataGroup, linkList);

		DataGroup readLinkList = recordStorage.readLinkList("fromRecordType", "fromRecordId");

		assertEquals(readLinkList.getChildren().size(), 2);
	}

	@Test
	public void testCreateGenerateLinksPointToRecord() {
		DataGroup dataGroup = createDataGroupWithRecordInfo();
		DataGroup linkList = createLinkListWithTwoLinks("fromRecordId");
		recordStorage.create("fromRecordType", "fromRecordId", dataGroup, linkList);

		DataGroup linkList2 = createLinkListWithTwoLinks("fromRecordId2");
		recordStorage.create("fromRecordType", "fromRecordId2", dataGroup, linkList2);

		DataGroup generatedLinksPointToRecord = recordStorage
				.generateLinkCollectionPointingToRecord("toRecordType", "toRecordId");

		List<DataElement> generatedLinks = generatedLinksPointToRecord.getChildren();
		assertEquals(generatedLinks.size(), 2);

		assertRecordLinkIsCorrect((DataGroup) generatedLinks.get(0), "fromRecordType",
				"fromRecordId2", "toRecordType", "toRecordId");
		assertRecordLinkIsCorrect((DataGroup) generatedLinks.get(1), "fromRecordType",
				"fromRecordId", "toRecordType", "toRecordId");

		assertNoGeneratedLinksForRecordTypeAndRecordId("toRecordType", "NOT_toRecordId");
		assertNoGeneratedLinksForRecordTypeAndRecordId("NOT_toRecordType", "toRecordId");
	}

	private void assertRecordLinkIsCorrect(DataGroup recordToRecordLink, String fromRecordType,
			String fromRecordId, String toRecordType, String toRecordId) {
		assertEquals(recordToRecordLink.getNameInData(), "recordToRecordLink");
		DataRecordLink fromOut = (DataRecordLink) recordToRecordLink
				.getFirstChildWithNameInData("from");
		assertEquals(fromOut.getLinkedRecordType(), fromRecordType);
		assertEquals(fromOut.getLinkedRecordId(), fromRecordId);

		DataRecordLink toOut = (DataRecordLink) recordToRecordLink
				.getFirstChildWithNameInData("to");
		assertEquals(toOut.getLinkedRecordType(), toRecordType);
		assertEquals(toOut.getLinkedRecordId(), toRecordId);
	}

	private void assertNoGeneratedLinksForRecordTypeAndRecordId(String toRecordType,
			String toRecordId) {
		DataGroup generatedLinksPointToOtherRecordId = recordStorage
				.generateLinkCollectionPointingToRecord(toRecordType, toRecordId);
		assertEquals(generatedLinksPointToOtherRecordId.getChildren().size(), 0);
	}

	private DataGroup createLinkListWithTwoLinks(String fromRecordId) {
		DataGroup linkList = DataCreator.createLinkList();

		linkList.addChild(DataCreator.createRecordToRecordLink("fromRecordType", fromRecordId,
				"toRecordType", "toRecordId"));

		linkList.addChild(DataCreator.createRecordToRecordLink("fromRecordType", fromRecordId,
				"toRecordType", "toRecordId2"));
		return linkList;
	}

	@Test
	public void testCreateWithoutLink() {
		DataGroup dataGroup = createDataGroupWithRecordInfo();

		DataGroup linkList = emptyLinkList;
		recordStorage.create("fromRecordType", "fromRecordId", dataGroup, linkList);

		DataGroup readLinkList = recordStorage.readLinkList("fromRecordType", "fromRecordId");
		assertEquals(readLinkList.getChildren().size(), 0);
	}

	@Test
	public void testCreateWithoutLink2() {
		DataGroup dataGroup = createDataGroupWithRecordInfo();

		DataGroup linkList = emptyLinkList;
		recordStorage.create("fromRecordType", "fromRecordId", dataGroup, linkList);

		DataGroup readLinkList = recordStorage.readLinkList("fromRecordType", "fromRecordId");
		assertEquals(readLinkList.getChildren().size(), 0);

		recordStorage.deleteByTypeAndId("fromRecordType", "fromRecordId");
		recordStorage.create("fromRecordType", "fromRecordId", dataGroup, linkList);
	}

	@Test(expectedExceptions = IllegalArgumentException.class)
	public void testInitWithNull() {
		new RecordStorageInMemory(null);
	}

	@Test(expectedExceptions = RecordNotFoundException.class)
	public void testReadRecordListNotFound() {
		String recordType = "place_NOT_FOUND";
		recordStorage.readList(recordType);
	}

	@Test
	public void testReadRecordList() {
		recordStorage = TestDataRecordInMemoryStorage.createRecordStorageInMemoryWithTestData();
		String recordType = "place";
		Collection<DataGroup> recordList = recordStorage.readList(recordType);
		assertEquals(recordList.iterator().next().getNameInData(), "authority");
	}

	@Test(expectedExceptions = RecordNotFoundException.class)
	public void testReadMissingrecordType() {
		recordStorage.read("", "");
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

		recordStorage.create("type", "place:0001", dataGroup, emptyLinkList);
		DataGroup dataGroupOut = recordStorage.read("type", "place:0001");
		assertEquals(dataGroupOut.getNameInData(), dataGroup.getNameInData());
	}

	@Test
	public void testCreateTworecordsRead() {

		DataGroup dataGroup = createDataGroupWithRecordInfo();

		recordStorage.create("type", "place:0001", dataGroup, emptyLinkList);
		recordStorage.create("type", "place:0002", dataGroup, emptyLinkList);

		DataGroup dataGroupOut = recordStorage.read("type", "place:0001");
		assertEquals(dataGroupOut.getNameInData(), dataGroup.getNameInData());

		DataGroup dataGroupOut2 = recordStorage.read("type", "place:0002");
		assertEquals(dataGroupOut2.getNameInData(), dataGroup.getNameInData());
	}

	@Test
	public void testCreateDataInStorageShouldBeIndependent() {
		DataGroup dataGroup = createDataGroupWithRecordInfo();
		dataGroup.addChild(DataAtomic.withNameInDataAndValue("childId", "childValue"));
		recordStorage.create("type", "place:0001", dataGroup, emptyLinkList);

		dataGroup.getChildren().clear();

		DataGroup dataGroupOut = recordStorage.read("type", "place:0001");
		DataAtomic child = (DataAtomic) dataGroupOut.getChildren().get(1);

		assertEquals(child.getValue(), "childValue");
	}

	@Test(expectedExceptions = RecordConflictException.class)
	public void testCreateConflict() {
		DataGroup dataGroup = createDataGroupWithRecordInfo();
		recordStorage.create("type", "place1", dataGroup, emptyLinkList);
		recordStorage.create("type", "place1", dataGroup, emptyLinkList);
	}

	@Test
	public void testDelete() {
		DataGroup dataGroup = createDataGroupWithRecordInfo();

		recordStorage.create("type", "place:0001", dataGroup, emptyLinkList);
		DataGroup dataGroupOut = recordStorage.read("type", "place:0001");
		assertEquals(dataGroupOut.getNameInData(), dataGroup.getNameInData());

		recordStorage.deleteByTypeAndId("type", "place:0001");

		boolean recordFound = true;
		try {
			recordStorage.read("type", "place:0001");
			recordFound = true;

		} catch (RecordNotFoundException e) {
			recordFound = false;
		}
		assertFalse(recordFound);
	}

	@Test(expectedExceptions = RecordNotFoundException.class)
	public void testLinkListIsRemovedOnDelete() {
		DataGroup dataGroup = createDataGroupWithRecordInfo();
		DataGroup linkList = createLinkListWithTwoLinks("fromRecordId");

		recordStorage.create("fromRecordType", "fromRecordId", dataGroup, linkList);

		recordStorage.deleteByTypeAndId("fromRecordType", "fromRecordId");
		recordStorage.readLinkList("fromRecordType", "fromRecordId");

	}

	@Test(expectedExceptions = RecordNotFoundException.class)
	public void testLinkListIsRemovedOnDeleteRecordTypeStillExistsInLinkListStorage() {
		DataGroup dataGroup = createDataGroupWithRecordInfo();
		DataGroup linkList = createLinkListWithTwoLinks("fromRecordId");

		recordStorage.create("fromRecordType", "fromRecordId", dataGroup,
				createLinkListWithLinksForTestingRemoveOfLinks());
		recordStorage.create("fromRecordType", "fromRecordId2", dataGroup, linkList);

		recordStorage.deleteByTypeAndId("fromRecordType", "fromRecordId");
		recordStorage.readLinkList("fromRecordType", "fromRecordId");

	}

	private DataGroup createLinkListWithLinksForTestingRemoveOfLinks() {
		DataGroup linkList = DataCreator.createLinkList();

		linkList.addChild(DataCreator.createRecordToRecordLink("fromRecordType", "fromRecordId",
				"toRecordType", "toRecordId"));
		linkList.addChild(DataCreator.createRecordToRecordLink("fromRecordType", "fromRecordId2",
				"toRecordType", "toRecordId"));
		return linkList;
	}

	@Test
	public void testGenerateLinksPointToRecordAreRemovedOnDelete() {
		DataGroup dataGroup = createDataGroupWithRecordInfo();
		DataGroup linkList = createLinkListWithTwoLinks("fromRecordId");

		recordStorage.create("fromRecordType", "fromRecordId", dataGroup, linkList);
		assertNoOfLinksPointingToRecord("toRecordType", "toRecordId", 1);
		// delete
		recordStorage.deleteByTypeAndId("fromRecordType", "fromRecordId");
		assertNoOfLinksPointingToRecord("toRecordType", "toRecordId", 0);

		assertFalse(recordStorage.linksExistForRecord("toRecordType", "toRecordId"));
	}

	@Test(expectedExceptions = RecordNotFoundException.class)
	public void testDeleteNotFound() {
		DataGroup dataGroup = createDataGroupWithRecordInfo();

		recordStorage.create("type", "place:0001", dataGroup, emptyLinkList);
		DataGroup dataGroupOut = recordStorage.read("type", "place:0001");
		assertEquals(dataGroupOut.getNameInData(), dataGroup.getNameInData());

		recordStorage.deleteByTypeAndId("type", "place:0001_NOT_FOUND");
	}

	@Test
	public void testUpdate() {
		DataGroup dataGroup = createDataGroupWithRecordInfo();
		dataGroup.addChild(DataAtomic.withNameInDataAndValue("childId", "childValue"));
		recordStorage.create("type", "place:0001", dataGroup, emptyLinkList);

		DataGroup dataGroupOut = recordStorage.read("type", "place:0001");
		DataAtomic child = (DataAtomic) dataGroupOut.getChildren().get(1);

		DataGroup dataGroup2 = createDataGroupWithRecordInfo();
		dataGroup2.addChild(DataAtomic.withNameInDataAndValue("childId2", "childValue2"));
		recordStorage.update("type", "place:0001", dataGroup2, emptyLinkList);

		DataGroup dataGroupOut2 = recordStorage.read("type", "place:0001");
		DataAtomic child2 = (DataAtomic) dataGroupOut2.getChildren().get(1);

		assertEquals(child.getValue(), "childValue");
		assertEquals(child2.getValue(), "childValue2");
	}

	@Test
	public void testUpdateWithoutLink() {
		DataGroup dataGroup = createDataGroupWithRecordInfo();
		dataGroup.addChild(DataAtomic.withNameInDataAndValue("childId", "childValue"));
		recordStorage.create("place", "place:0001", dataGroup, emptyLinkList);

		DataGroup dataGroup2 = createDataGroupWithRecordInfo();

		recordStorage.update("place", "place:0001", dataGroup2, emptyLinkList);

		DataGroup readLinkList = recordStorage.readLinkList("place", "place:0001");
		assertEquals(readLinkList.getChildren().size(), 0);
	}

	@Test
	public void testUpdateAndReadLinkList() {
		DataGroup dataGroup = createDataGroupWithRecordInfo();
		DataGroup linkList = createLinkListWithTwoLinks("fromRecordId");
		recordStorage.create("fromRecordType", "fromRecordId", dataGroup, linkList);

		DataGroup readLinkList = recordStorage.readLinkList("fromRecordType", "fromRecordId");

		assertEquals(readLinkList.getChildren().size(), 2);

		// update
		DataGroup linkListOne = createLinkListWithOneLink("fromRecordId");
		recordStorage.update("fromRecordType", "fromRecordId", dataGroup, linkListOne);

		DataGroup readLinkListUpdated = recordStorage.readLinkList("fromRecordType",
				"fromRecordId");

		assertEquals(readLinkListUpdated.getChildren().size(), 1);
	}

	private DataGroup createLinkListWithOneLink(String fromRecordId) {
		DataGroup linkList = DataCreator.createLinkList();

		linkList.addChild(DataCreator.createRecordToRecordLink("fromRecordType", fromRecordId,
				"toRecordType", "toRecordId"));

		return linkList;
	}

	@Test
	public void testUpdateGenerateLinksPointToRecordAreRemovedAndAdded() {
		DataGroup dataGroup = createDataGroupWithRecordInfo();
		DataGroup linkList = createLinkListWithTwoLinks("fromRecordId");

		recordStorage.create("fromRecordType", "fromRecordId", dataGroup, linkList);
		assertNoOfLinksPointingToRecord("toRecordType", "toRecordId", 1);
		// update
		recordStorage.update("fromRecordType", "fromRecordId", dataGroup, emptyLinkList);
		assertNoOfLinksPointingToRecord("toRecordType", "toRecordId", 0);

		// update
		recordStorage.update("fromRecordType", "fromRecordId", dataGroup, linkList);
		assertNoOfLinksPointingToRecord("toRecordType", "toRecordId", 1);
	}

	private void assertNoOfLinksPointingToRecord(String toRecordType, String toRecordId,
			int expectedNoOfLinksPointingToRecord) {
		DataGroup generatedLinksPointToRecord = recordStorage
				.generateLinkCollectionPointingToRecord(toRecordType, toRecordId);
		List<DataElement> generatedLinks = generatedLinksPointToRecord.getChildren();
		assertEquals(generatedLinks.size(), expectedNoOfLinksPointingToRecord);
	}

	@Test(expectedExceptions = RecordNotFoundException.class)
	public void testUpdateNotFoundType() {
		DataGroup dataGroup = createDataGroupWithRecordInfo();
		dataGroup.addChild(DataAtomic.withNameInDataAndValue("childId", "childValue"));
		recordStorage.update("type", "place:0001", dataGroup, emptyLinkList);
	}

	@Test(expectedExceptions = RecordNotFoundException.class)
	public void testUpdateNotFoundId() {
		DataGroup dataGroup = createDataGroupWithRecordInfo();
		dataGroup.addChild(DataAtomic.withNameInDataAndValue("childId", "childValue"));
		recordStorage.create("type", "place:0001", dataGroup, emptyLinkList);
		recordStorage.update("type", "place:0002", dataGroup, emptyLinkList);
	}

	@Test
	public void testUpdateDataInStorageShouldBeIndependent() {
		DataGroup dataGroup = createDataGroupWithRecordInfo();
		dataGroup.addChild(DataAtomic.withNameInDataAndValue("childId", "childValue"));
		recordStorage.create("type", "place:0001", dataGroup, emptyLinkList);
		recordStorage.update("type", "place:0001", dataGroup, emptyLinkList);

		dataGroup.getChildren().clear();

		DataGroup dataGroupOut = recordStorage.read("type", "place:0001");
		DataAtomic child = (DataAtomic) dataGroupOut.getChildren().get(1);

		assertEquals(child.getValue(), "childValue");
	}

}
