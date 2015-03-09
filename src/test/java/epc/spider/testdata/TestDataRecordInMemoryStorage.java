package epc.spider.testdata;

import java.util.HashMap;
import java.util.Map;

import epc.metadataformat.data.DataAtomic;
import epc.metadataformat.data.DataGroup;
import epc.spider.record.storage.RecordStorageInMemory;

public class TestDataRecordInMemoryStorage {
	public static RecordStorageInMemory createRecordStorageInMemoryWithTestData() {
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

		DataGroup dataGroup = DataGroup.withDataId("authority");
		dataGroup.addChild(recordInfo);

		records.get("place").put("place:0001", dataGroup);

		RecordStorageInMemory recordsInMemory = new RecordStorageInMemory(records);
		return recordsInMemory;
	}
}
