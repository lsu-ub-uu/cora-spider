package epc.spider.record;

import static org.testng.Assert.assertEquals;

import java.util.Collection;

import org.testng.annotations.Test;

import epc.metadataformat.data.DataGroup;
import epc.spider.record.storage.RecordNotFoundException;
import epc.spider.record.storage.RecordStorage;
import epc.spider.testdata.TestDataRecordInMemoryStorage;

public class RecordStorageInMemoryTest {

	@Test(expectedExceptions = RecordNotFoundException.class)
	public void testReadRecordListNotFound() {
		RecordStorage recordStorage = TestDataRecordInMemoryStorage
				.createRecordStorageInMemoryWithTestData();
		String recordType = "place_NOT_FOUND";
		recordStorage.readList(recordType);
	}

	@Test
	public void testReadRecordList() {
		RecordStorage recordStorage = TestDataRecordInMemoryStorage
				.createRecordStorageInMemoryWithTestData();
		String recordType = "place";
		Collection<DataGroup> recordList = recordStorage.readList(recordType);
		assertEquals(recordList.iterator().next().getDataId(), "authority");
	}
}
