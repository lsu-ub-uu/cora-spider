package epc.spider.record;

import static org.testng.Assert.assertEquals;

import java.util.Collection;

import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import epc.metadataformat.data.DataAtomic;
import epc.metadataformat.data.DataGroup;
import epc.metadataformat.storage.MetadataStorage;
import epc.spider.record.storage.RecordNotFoundException;
import epc.spider.record.storage.RecordStorage;
import epc.spider.record.storage.RecordStorageInMemory;
import epc.spider.testdata.TestDataRecordInMemoryStorage;

public class RecordStorageInMemoryTest {

	private RecordStorage recordStorage;
	private MetadataStorage metadataStorage;

	@BeforeMethod
	public void BeforeMethod() {
		RecordStorageInMemory recordStorageInMemory = TestDataRecordInMemoryStorage
				.createRecordStorageInMemoryWithTestData();
		recordStorage = recordStorageInMemory;
		metadataStorage = recordStorageInMemory;
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
		assertEquals(recordList.iterator().next().getDataId(), "authority");
	}

	@Test
	public void testGetMetadataElements() {
		Collection<DataGroup> metadataElements = metadataStorage.getMetadataElements();
		DataGroup metadataElement = metadataElements.iterator().next();
		assertEquals(metadataElement.getDataId(), "metadata");
		DataGroup recordInfo = (DataGroup) metadataElement.getChildren().get(0);
		DataAtomic id = (DataAtomic) recordInfo.getChildren().get(0);
		assertEquals(id.getValue(), "metadata:place");
	}

	@Test
	public void testGetPresentationElements() {
		Collection<DataGroup> presentationElements = metadataStorage.getPresentationElements();
		DataGroup presentationElement = presentationElements.iterator().next();
		assertEquals(presentationElement.getDataId(), "presentation");
		DataGroup recordInfo = (DataGroup) presentationElement.getChildren().get(0);
		DataAtomic id = (DataAtomic) recordInfo.getChildren().get(0);
		assertEquals(id.getValue(), "presentation:placeView");
	}

	@Test
	public void testGetTexts() {
		Collection<DataGroup> texts = metadataStorage.getTexts();
		DataGroup text = texts.iterator().next();
		assertEquals(text.getDataId(), "text");
		DataGroup recordInfo = (DataGroup) text.getChildren().get(0);
		DataAtomic id = (DataAtomic) recordInfo.getChildren().get(0);
		assertEquals(id.getValue(), "text:placeText");
	}

	@Test
	public void testGetRecordTypes() {
		Collection<DataGroup> recordTypes = metadataStorage.getRecordTypes();
		DataGroup recordType = recordTypes.iterator().next();
		assertEquals(recordType.getDataId(), "recordType");
		DataGroup recordInfo = (DataGroup) recordType.getChildren().get(0);
		DataAtomic id = (DataAtomic) recordInfo.getChildren().get(0);
		assertEquals(id.getValue(), "recordType:metadata");
	}

}
