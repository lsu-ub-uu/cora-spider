package epc.spider.testdata;

import java.util.HashMap;
import java.util.Map;

import epc.metadataformat.data.DataAtomic;
import epc.metadataformat.data.DataGroup;
import epc.spider.record.storage.RecordInMemoryStorage;

public class TestDataRecordInMemoryStorage {
	public static RecordInMemoryStorage createRecordInMemoryStorageWithTestData() {
		Map<String, Map<String, DataGroup>> records = new HashMap<>();
		records.put("place", new HashMap<String, DataGroup>());

		DataGroup recordInfo = new DataGroup("recordInfo");
		recordInfo.addChild(new DataAtomic("type", "place"));
		recordInfo.addChild(new DataAtomic("id", "place:0001"));

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

		DataGroup dataGroup = new DataGroup("authority");
		dataGroup.addChild(recordInfo);

		records.get("place").put("place:0001", dataGroup);

		RecordInMemoryStorage recordsInMemory = new RecordInMemoryStorage(
				records);
		return recordsInMemory;
	}
}
