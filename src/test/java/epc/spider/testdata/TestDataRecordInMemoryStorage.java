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
		records.put("recordType", new HashMap<String, DataGroup>());
		addRecordType();
		addRecordTypeRecordType();
		addRecordTypePlace();
		
		addRecordTypeAbstract();
		addRecordTypeChild1();
		addRecordTypeChild2();

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
		String metadata = "metadataGroup";
		records.put(metadata, new HashMap<String, DataGroup>());
		DataGroup dataGroup = DataGroup.withDataId("metadata");

		DataGroup recordInfo = DataGroup.withDataId("recordInfo");
		recordInfo.addChild(DataAtomic.withDataIdAndValue("id", "place"));
		recordInfo.addChild(DataAtomic.withDataIdAndValue("type", metadata));
		dataGroup.addChild(recordInfo);

		records.get(metadata).put("place", dataGroup);
	}

	private static void addPresentation() {
		String presentation = "presentation";
		records.put(presentation, new HashMap<String, DataGroup>());
		DataGroup dataGroup = DataGroup.withDataId(presentation);

		DataGroup recordInfo = DataGroup.withDataId("recordInfo");
		recordInfo.addChild(DataAtomic.withDataIdAndValue("id", "placeView"));
		recordInfo.addChild(DataAtomic.withDataIdAndValue("type", presentation));
		dataGroup.addChild(recordInfo);

		records.get(presentation).put("placeView", dataGroup);
	}

	private static void addText() {
		String text = "text";
		records.put(text, new HashMap<String, DataGroup>());
		DataGroup dataGroup = DataGroup.withDataId(text);

		DataGroup recordInfo = DataGroup.withDataId("recordInfo");
		recordInfo.addChild(DataAtomic.withDataIdAndValue("id", "placeText"));
		recordInfo.addChild(DataAtomic.withDataIdAndValue("type", text));
		dataGroup.addChild(recordInfo);

		records.get(text).put("placeText", dataGroup);
	}

	private static void addRecordType() {
		String recordType = "recordType";
		DataGroup dataGroup = DataGroup.withDataId(recordType);

		DataGroup recordInfo = DataGroup.withDataId("recordInfo");
		recordInfo.addChild(DataAtomic.withDataIdAndValue("id", "metadata"));
		recordInfo.addChild(DataAtomic.withDataIdAndValue("type", recordType));
		dataGroup.addChild(recordInfo);

		dataGroup.addChild(DataAtomic.withDataIdAndValue("abstract", "false"));
		records.get(recordType).put("metadata", dataGroup);
	}

	private static void addRecordTypeRecordType() {
		String recordType = "recordType";
		DataGroup dataGroup = DataGroup.withDataId(recordType);

		DataGroup recordInfo = DataGroup.withDataId("recordInfo");
		recordInfo.addChild(DataAtomic.withDataIdAndValue("id", "recordType"));
		recordInfo.addChild(DataAtomic.withDataIdAndValue("type", recordType));
		dataGroup.addChild(recordInfo);

		dataGroup.addChild(DataAtomic.withDataIdAndValue("id", "recordType"));
		dataGroup.addChild(DataAtomic.withDataIdAndValue("metadataId", "recordType"));
		dataGroup.addChild(DataAtomic.withDataIdAndValue("presentationViewId",
				"presentation:pgRecordTypeView"));
		dataGroup.addChild(DataAtomic.withDataIdAndValue("presentationFormId",
				"presentation:pgRecordTypeForm"));
		dataGroup.addChild(DataAtomic.withDataIdAndValue("newMetadataId", "recordTypeNew"));
		dataGroup.addChild(DataAtomic.withDataIdAndValue("newPresentationFormId",
				"presentation:pgRecordTypeFormNew"));
		dataGroup.addChild(DataAtomic.withDataIdAndValue("listPresentationViewId",
				"presentation:pgRecordTypeViewList"));
		dataGroup.addChild(DataAtomic.withDataIdAndValue("searchMetadataId",
				"metadata:recordTypeSearch"));
		dataGroup.addChild(DataAtomic.withDataIdAndValue("searchPresentationFormId",
				"presentation:pgRecordTypeSearchForm"));
		dataGroup.addChild(DataAtomic.withDataIdAndValue("userSuppliedId", "true"));
		dataGroup.addChild(DataAtomic.withDataIdAndValue("permissionKey", "RECORDTYPE_RECORDTYPE"));
		dataGroup.addChild(DataAtomic.withDataIdAndValue("selfPresentationViewId",
				"presentation:pgrecordTypeRecordType"));
		dataGroup.addChild(DataAtomic.withDataIdAndValue("abstract", "false"));
		records.get(recordType).put("recordType", dataGroup);

	}

	private static void addRecordTypePlace() {
		String recordType = "recordType";
		DataGroup dataGroup = DataGroup.withDataId(recordType);

		DataGroup recordInfo = DataGroup.withDataId("recordInfo");
		recordInfo.addChild(DataAtomic.withDataIdAndValue("id", "place"));
		recordInfo.addChild(DataAtomic.withDataIdAndValue("type", recordType));
		dataGroup.addChild(recordInfo);

		dataGroup.addChild(DataAtomic.withDataIdAndValue("id", "place"));
		dataGroup.addChild(DataAtomic.withDataIdAndValue("metadataId", "metadata:place"));
		dataGroup.addChild(DataAtomic.withDataIdAndValue("presentationViewId",
				"presentation:pgPlaceView"));
		dataGroup.addChild(DataAtomic.withDataIdAndValue("presentationFormId",
				"presentation:pgPlaceForm"));
		dataGroup.addChild(DataAtomic.withDataIdAndValue("newMetadataId", "metadata:placeNew"));
		dataGroup.addChild(DataAtomic.withDataIdAndValue("newPresentationFormId",
				"presentation:pgPlaceFormNew"));
		dataGroup.addChild(DataAtomic.withDataIdAndValue("listPresentationViewId",
				"presentation:pgPlaceViewList"));
		dataGroup.addChild(DataAtomic
				.withDataIdAndValue("searchMetadataId", "metadata:placeSearch"));
		dataGroup.addChild(DataAtomic.withDataIdAndValue("searchPresentationFormId",
				"presentation:pgPlaceSearchForm"));
		dataGroup.addChild(DataAtomic.withDataIdAndValue("userSuppliedId", "false"));
		dataGroup.addChild(DataAtomic.withDataIdAndValue("permissionKey", "RECORDTYPE_PLACE"));
		dataGroup.addChild(DataAtomic.withDataIdAndValue("selfPresentationViewId",
				"presentation:pgPlaceRecordType"));
		dataGroup.addChild(DataAtomic.withDataIdAndValue("abstract", "false"));
		records.get(recordType).put("place", dataGroup);

	}
	private static void addRecordTypeAbstract() {
		String recordType = "recordType";
		DataGroup dataGroup = DataGroup.withDataId(recordType);

		DataGroup recordInfo = DataGroup.withDataId("recordInfo");
		recordInfo.addChild(DataAtomic.withDataIdAndValue("id", "abstract"));
		recordInfo.addChild(DataAtomic.withDataIdAndValue("type", recordType));
		dataGroup.addChild(recordInfo);

		dataGroup.addChild(DataAtomic.withDataIdAndValue("metadataId", "abstract"));
		dataGroup.addChild(DataAtomic.withDataIdAndValue("presentationViewId",
				"presentation:pgAbstractView"));
		dataGroup.addChild(DataAtomic.withDataIdAndValue("presentationFormId",
				"presentation:pgAbstractForm"));
		dataGroup.addChild(DataAtomic.withDataIdAndValue("newMetadataId", "metadata:abstractNew"));
		dataGroup.addChild(DataAtomic.withDataIdAndValue("newPresentationFormId",
				"presentation:pgAbstractFormNew"));
		dataGroup.addChild(DataAtomic.withDataIdAndValue("listPresentationViewId",
				"presentation:pgAbstractViewList"));
		dataGroup.addChild(DataAtomic
				.withDataIdAndValue("searchMetadataId", "metadata:AbstractSearch"));
		dataGroup.addChild(DataAtomic.withDataIdAndValue("searchPresentationFormId",
				"presentation:pgAbstractSearchForm"));
		dataGroup.addChild(DataAtomic.withDataIdAndValue("userSuppliedId", "false"));
		dataGroup.addChild(DataAtomic.withDataIdAndValue("permissionKey", "RECORDTYPE_ABSTRACT"));
		dataGroup.addChild(DataAtomic.withDataIdAndValue("selfPresentationViewId",
				"presentation:pgAbstractRecordType"));
		
		dataGroup.addChild(DataAtomic.withDataIdAndValue("abstract", "true"));
		
		
		records.get(recordType).put("abstract", dataGroup);
	}
	private static void addRecordTypeChild1() {
		String recordType = "recordType";
		DataGroup dataGroup = DataGroup.withDataId(recordType);

		DataGroup recordInfo = DataGroup.withDataId("recordInfo");
		recordInfo.addChild(DataAtomic.withDataIdAndValue("id", "child1"));
		recordInfo.addChild(DataAtomic.withDataIdAndValue("type", recordType));
		dataGroup.addChild(recordInfo);

		dataGroup.addChild(DataAtomic.withDataIdAndValue("metadataId", "child1"));
		dataGroup.addChild(DataAtomic.withDataIdAndValue("presentationViewId",
				"presentation:pgChild1View"));
		dataGroup.addChild(DataAtomic.withDataIdAndValue("presentationFormId",
				"presentation:pgChild1Form"));
		dataGroup.addChild(DataAtomic.withDataIdAndValue("newMetadataId", "metadata:child1New"));
		dataGroup.addChild(DataAtomic.withDataIdAndValue("newPresentationFormId",
				"presentation:pgChild1FormNew"));
		dataGroup.addChild(DataAtomic.withDataIdAndValue("listPresentationViewId",
				"presentation:pgChild1ViewList"));
		dataGroup.addChild(DataAtomic
				.withDataIdAndValue("searchMetadataId", "metadata:Child1Search"));
		dataGroup.addChild(DataAtomic.withDataIdAndValue("searchPresentationFormId",
				"presentation:pgChild1SearchForm"));
		dataGroup.addChild(DataAtomic.withDataIdAndValue("userSuppliedId", "true"));
		dataGroup.addChild(DataAtomic.withDataIdAndValue("permissionKey", "RECORDTYPE_CHILD1"));
		dataGroup.addChild(DataAtomic.withDataIdAndValue("selfPresentationViewId",
				"presentation:pgChild1RecordType"));
		
		dataGroup.addChild(DataAtomic.withDataIdAndValue("abstract", "false"));
		dataGroup.addChild(DataAtomic.withDataIdAndValue("parentId", "abstract"));
		
		
		records.get(recordType).put("child1", dataGroup);
	}
	private static void addRecordTypeChild2() {
		String recordType = "recordType";
		DataGroup dataGroup = DataGroup.withDataId(recordType);
		
		DataGroup recordInfo = DataGroup.withDataId("recordInfo");
		recordInfo.addChild(DataAtomic.withDataIdAndValue("id", "child2"));
		recordInfo.addChild(DataAtomic.withDataIdAndValue("type", recordType));
		dataGroup.addChild(recordInfo);
		
		dataGroup.addChild(DataAtomic.withDataIdAndValue("metadataId", "child2"));
		dataGroup.addChild(DataAtomic.withDataIdAndValue("presentationViewId",
				"presentation:pgChild2View"));
		dataGroup.addChild(DataAtomic.withDataIdAndValue("presentationFormId",
				"presentation:pgChild2Form"));
		dataGroup.addChild(DataAtomic.withDataIdAndValue("newMetadataId", "metadata:child2New"));
		dataGroup.addChild(DataAtomic.withDataIdAndValue("newPresentationFormId",
				"presentation:pgChild2FormNew"));
		dataGroup.addChild(DataAtomic.withDataIdAndValue("listPresentationViewId",
				"presentation:pgChild2ViewList"));
		dataGroup.addChild(DataAtomic
				.withDataIdAndValue("searchMetadataId", "metadata:Child2Search"));
		dataGroup.addChild(DataAtomic.withDataIdAndValue("searchPresentationFormId",
				"presentation:pgChild2SearchForm"));
		dataGroup.addChild(DataAtomic.withDataIdAndValue("userSuppliedId", "true"));
		dataGroup.addChild(DataAtomic.withDataIdAndValue("permissionKey", "RECORDTYPE_CHILD2"));
		dataGroup.addChild(DataAtomic.withDataIdAndValue("selfPresentationViewId",
				"presentation:pgChild2RecordType"));
		
		dataGroup.addChild(DataAtomic.withDataIdAndValue("abstract", "false"));
		dataGroup.addChild(DataAtomic.withDataIdAndValue("parentId", "abstract"));
		
		
		records.get(recordType).put("child2", dataGroup);
	}
}
