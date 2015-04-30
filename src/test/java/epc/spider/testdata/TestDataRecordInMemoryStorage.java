package epc.spider.testdata;

import java.util.HashMap;
import java.util.Map;

import epc.metadataformat.data.DataAtomic;
import epc.metadataformat.data.DataGroup;
import epc.spider.record.storage.RecordStorageInMemory;

public class TestDataRecordInMemoryStorage {
	private static Map<String, Map<String, DataGroup>> records = new HashMap<>();

	public static RecordStorageInMemory createRecordStorageInMemoryWithTestData() {
		addPlace();
		addMetadata();
		addPresentation();
		addText();
		addRecordType();

		RecordStorageInMemory recordsInMemory = new RecordStorageInMemory(records);
		return recordsInMemory;
	}

	private static void addPlace() {
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
	}

	private static void addMetadata() {
		String metadata = "metadata";
		records.put(metadata, new HashMap<String, DataGroup>());
		DataGroup dataGroup = DataGroup.withDataId(metadata);

		DataGroup recordInfo = DataGroup.withDataId("recordInfo");
		recordInfo.addChild(DataAtomic.withDataIdAndValue("id", "metadata:place"));
		recordInfo.addChild(DataAtomic.withDataIdAndValue("type", metadata));
		dataGroup.addChild(recordInfo);

		records.get(metadata).put("metadata:place", dataGroup);
	}

	private static void addPresentation() {
		String presentation = "presentation";
		records.put(presentation, new HashMap<String, DataGroup>());
		DataGroup dataGroup = DataGroup.withDataId(presentation);

		DataGroup recordInfo = DataGroup.withDataId("recordInfo");
		recordInfo.addChild(DataAtomic.withDataIdAndValue("id", presentation + ":placeView"));
		recordInfo.addChild(DataAtomic.withDataIdAndValue("type", presentation));
		dataGroup.addChild(recordInfo);

		records.get(presentation).put(presentation + ":placeView", dataGroup);
	}

	private static void addText() {
		String text = "text";
		records.put(text, new HashMap<String, DataGroup>());
		DataGroup dataGroup = DataGroup.withDataId(text);

		DataGroup recordInfo = DataGroup.withDataId("recordInfo");
		recordInfo.addChild(DataAtomic.withDataIdAndValue("id", text + ":placeText"));
		recordInfo.addChild(DataAtomic.withDataIdAndValue("type", text));
		dataGroup.addChild(recordInfo);

		records.get(text).put(text + ":placeText", dataGroup);
	}

	private static void addRecordType() {
		String recordType = "recordType";
		records.put(recordType, new HashMap<String, DataGroup>());
		DataGroup dataGroup = DataGroup.withDataId(recordType);

		DataGroup recordInfo = DataGroup.withDataId("recordInfo");
		recordInfo.addChild(DataAtomic.withDataIdAndValue("id", recordType + ":metadata"));
		recordInfo.addChild(DataAtomic.withDataIdAndValue("type", recordType));
		dataGroup.addChild(recordInfo);

		records.get(recordType).put(recordType + ":metadata", dataGroup);
	}
}
