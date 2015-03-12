package epc.spider.testdata;

import java.util.HashMap;
import java.util.Map;

import epc.spider.data.SpiderDataAtomic;
import epc.spider.data.SpiderDataGroup;
import epc.spider.record.storage.RecordStorageInMemory;

public class TestDataRecordInMemoryStorage {
	public static RecordStorageInMemory createRecordStorageInMemoryWithTestData() {
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

		SpiderDataGroup dataGroup = SpiderDataGroup.withDataId("authority");
		dataGroup.addChild(recordInfo);

		records.get("place").put("place:0001", dataGroup);

		RecordStorageInMemory recordsInMemory = new RecordStorageInMemory(records);
		return recordsInMemory;
	}
}
