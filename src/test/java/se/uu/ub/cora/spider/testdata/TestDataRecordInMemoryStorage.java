package se.uu.ub.cora.spider.testdata;

import se.uu.ub.cora.metadataformat.data.DataAtomic;
import se.uu.ub.cora.metadataformat.data.DataGroup;
import se.uu.ub.cora.metadataformat.data.DataRecordLink;
import se.uu.ub.cora.spider.record.storage.RecordStorage;
import se.uu.ub.cora.spider.record.storage.RecordStorageInMemory;

public class TestDataRecordInMemoryStorage {

	public static RecordStorageInMemory createRecordStorageInMemoryWithTestData() {
		RecordStorageInMemory recordsInMemory = new RecordStorageInMemory();
		addPlace(recordsInMemory);
		addSecondPlace(recordsInMemory);
		addMetadata(recordsInMemory);
		addPresentation(recordsInMemory);
		addText(recordsInMemory);
		addRecordType(recordsInMemory);
		addRecordTypeRecordType(recordsInMemory);
		addRecordTypeRecordTypeAutoGeneratedId(recordsInMemory);
		addRecordTypePlace(recordsInMemory);

		DataGroup dummy = DataGroup.withNameInData("dummy");
		recordsInMemory.create("metadataCollectionVariable", "dummy1", dummy,
				DataGroup.withNameInData("collectedLinksList"));
		recordsInMemory.create("metadataItemCollection", "dummy1", dummy,
				DataGroup.withNameInData("collectedLinksList"));
		recordsInMemory.create("metadataCollectionItem", "dummy1", dummy,
				DataGroup.withNameInData("collectedLinksList"));
		recordsInMemory.create("metadataTextVariable", "dummy1", dummy,
				DataGroup.withNameInData("collectedLinksList"));
		recordsInMemory.create("metadataDataToDataLink", "dummy1", dummy,
				DataGroup.withNameInData("collectedLinksList"));
		return recordsInMemory;
	}

	private static void addPlace(RecordStorageInMemory recordsInMemory) {
		DataGroup recordInfo = DataGroup.withNameInData("recordInfo");
		recordInfo.addChild(DataAtomic.withNameInDataAndValue("type", "place"));
		recordInfo.addChild(DataAtomic.withNameInDataAndValue("id", "place:0001"));

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

		DataGroup dataGroup = DataGroup.withNameInData("authority");
		dataGroup.addChild(recordInfo);
		recordsInMemory.create("place", "place:0001", dataGroup,
				DataGroup.withNameInData("collectedLinksList"));
	}

	private static void addSecondPlace(RecordStorage recordsInMemory) {
		DataGroup recordInfo = DataGroup.withNameInData("recordInfo");
		recordInfo.addChild(DataAtomic.withNameInDataAndValue("type", "place"));
		recordInfo.addChild(DataAtomic.withNameInDataAndValue("id", "place:0002"));

		DataGroup dataGroup = DataGroup.withNameInData("authority");
		dataGroup.addChild(recordInfo);

		// create link to place:0001
		DataGroup collectedLinksList = DataGroup.withNameInData("collectedLinksList");
		DataGroup recordToRecordLink = DataGroup.withNameInData("recordToRecordLink");
		DataRecordLink from = DataRecordLink.withNameInDataAndRecordTypeAndRecordId("from", "place",
				"place:0002");
		recordToRecordLink.addChild(from);
		DataRecordLink to = DataRecordLink.withNameInDataAndRecordTypeAndRecordId("to", "place",
				"place:0001");
		recordToRecordLink.addChild(to);

		collectedLinksList.addChild(recordToRecordLink);
		recordsInMemory.create("place", "place:0002", dataGroup, collectedLinksList);
	}

	private static void addMetadata(RecordStorageInMemory recordsInMemory) {
		String metadata = "metadataGroup";
		DataGroup dataGroup = DataGroup.withNameInData("metadata");

		DataGroup recordInfo = DataGroup.withNameInData("recordInfo");
		recordInfo.addChild(DataAtomic.withNameInDataAndValue("id", "place"));
		recordInfo.addChild(DataAtomic.withNameInDataAndValue("type", metadata));
		dataGroup.addChild(recordInfo);
		recordsInMemory.create(metadata, "place", dataGroup,
				DataGroup.withNameInData("collectedLinksList"));
	}

	private static void addPresentation(RecordStorageInMemory recordsInMemory) {
		String presentation = "presentation";
		DataGroup dataGroup = DataGroup.withNameInData(presentation);

		DataGroup recordInfo = DataGroup.withNameInData("recordInfo");
		recordInfo.addChild(DataAtomic.withNameInDataAndValue("id", "placeView"));
		recordInfo.addChild(DataAtomic.withNameInDataAndValue("type", presentation));
		dataGroup.addChild(recordInfo);

		recordsInMemory.create(presentation, "placeView", dataGroup,
				DataGroup.withNameInData("collectedLinksList"));
	}

	private static void addText(RecordStorageInMemory recordsInMemory) {
		String text = "text";
		DataGroup dataGroup = DataGroup.withNameInData("text");

		DataGroup recordInfo = DataGroup.withNameInData("recordInfo");
		recordInfo.addChild(DataAtomic.withNameInDataAndValue("id", "placeText"));
		recordInfo.addChild(DataAtomic.withNameInDataAndValue("type", text));
		dataGroup.addChild(recordInfo);
		recordsInMemory.create(text, "placeText", dataGroup,
				DataGroup.withNameInData("collectedLinksList"));
	}

	private static void addRecordType(RecordStorageInMemory recordsInMemory) {
		String recordType = "recordType";
		DataGroup dataGroup = DataGroup.withNameInData(recordType);

		DataGroup recordInfo = DataGroup.withNameInData("recordInfo");
		recordInfo.addChild(DataAtomic.withNameInDataAndValue("id", "metadata"));
		recordInfo.addChild(DataAtomic.withNameInDataAndValue("type", recordType));
		dataGroup.addChild(recordInfo);

		dataGroup.addChild(DataAtomic.withNameInDataAndValue("abstract", "false"));
		recordsInMemory.create(recordType, "metadata", dataGroup,
				DataGroup.withNameInData("collectedLinksList"));
	}

	private static void addRecordTypeRecordType(RecordStorageInMemory recordsInMemory) {
		String recordType = "recordType";
		DataGroup dataGroup = DataGroup.withNameInData(recordType);

		DataGroup recordInfo = DataGroup.withNameInData("recordInfo");
		recordInfo.addChild(DataAtomic.withNameInDataAndValue("id", "recordType"));
		recordInfo.addChild(DataAtomic.withNameInDataAndValue("type", recordType));
		dataGroup.addChild(recordInfo);

		dataGroup.addChild(DataAtomic.withNameInDataAndValue("id", "recordType"));
		dataGroup.addChild(DataAtomic.withNameInDataAndValue("metadataId", "recordType"));
		dataGroup.addChild(DataAtomic.withNameInDataAndValue("presentationViewId",
				"presentation:pgRecordTypeView"));
		dataGroup.addChild(DataAtomic.withNameInDataAndValue("presentationFormId",
				"presentation:pgRecordTypeForm"));
		dataGroup.addChild(DataAtomic.withNameInDataAndValue("newMetadataId", "recordTypeNew"));
		dataGroup.addChild(DataAtomic.withNameInDataAndValue("newPresentationFormId",
				"presentation:pgRecordTypeFormNew"));
		dataGroup.addChild(DataAtomic.withNameInDataAndValue("listPresentationViewId",
				"presentation:pgRecordTypeViewList"));
		dataGroup.addChild(
				DataAtomic.withNameInDataAndValue("searchMetadataId", "metadata:recordTypeSearch"));
		dataGroup.addChild(DataAtomic.withNameInDataAndValue("searchPresentationFormId",
				"presentation:pgRecordTypeSearchForm"));
		dataGroup.addChild(DataAtomic.withNameInDataAndValue("userSuppliedId", "true"));
		dataGroup.addChild(
				DataAtomic.withNameInDataAndValue("permissionKey", "RECORDTYPE_RECORDTYPE"));
		dataGroup.addChild(DataAtomic.withNameInDataAndValue("selfPresentationViewId",
				"presentation:pgrecordTypeRecordType"));
		dataGroup.addChild(DataAtomic.withNameInDataAndValue("abstract", "false"));
		recordsInMemory.create(recordType, "recordType", dataGroup,
				DataGroup.withNameInData("collectedLinksList"));
	}

	private static void addRecordTypeRecordTypeAutoGeneratedId(
			RecordStorageInMemory recordsInMemory) {
		String recordType = "recordType";
		DataGroup dataGroup = DataGroup.withNameInData(recordType);

		DataGroup recordInfo = DataGroup.withNameInData("recordInfo");
		recordInfo.addChild(DataAtomic.withNameInDataAndValue("id", "recordType"));
		recordInfo.addChild(DataAtomic.withNameInDataAndValue("type", recordType));
		dataGroup.addChild(recordInfo);
		dataGroup.addChild(DataAtomic.withNameInDataAndValue("id", "recordType"));
		dataGroup.addChild(DataAtomic.withNameInDataAndValue("metadataId", "recordType"));
		dataGroup.addChild(DataAtomic.withNameInDataAndValue("presentationViewId",
				"presentation:pgRecordTypeView"));
		dataGroup.addChild(DataAtomic.withNameInDataAndValue("presentationFormId",
				"presentation:pgRecordTypeForm"));
		dataGroup.addChild(DataAtomic.withNameInDataAndValue("newMetadataId", "recordTypeNew"));
		dataGroup.addChild(DataAtomic.withNameInDataAndValue("newPresentationFormId",
				"presentation:pgRecordTypeFormNew"));
		dataGroup.addChild(DataAtomic.withNameInDataAndValue("listPresentationViewId",
				"presentation:pgRecordTypeViewList"));
		dataGroup.addChild(
				DataAtomic.withNameInDataAndValue("searchMetadataId", "metadata:recordTypeSearch"));
		dataGroup.addChild(DataAtomic.withNameInDataAndValue("searchPresentationFormId",
				"presentation:pgRecordTypeSearchForm"));
		dataGroup.addChild(DataAtomic.withNameInDataAndValue("userSuppliedId", "false"));
		dataGroup.addChild(
				DataAtomic.withNameInDataAndValue("permissionKey", "RECORDTYPE_RECORDTYPE"));
		dataGroup.addChild(DataAtomic.withNameInDataAndValue("selfPresentationViewId",
				"presentation:pgrecordTypeRecordType"));
		dataGroup.addChild(DataAtomic.withNameInDataAndValue("abstract", "false"));
		recordsInMemory.create(recordType, "recordTypeAutoGeneratedId", dataGroup,
				DataGroup.withNameInData("collectedLinksList"));
	}

	private static void addRecordTypePlace(RecordStorageInMemory recordsInMemory) {
		String recordType = "recordType";
		DataGroup dataGroup = DataGroup.withNameInData(recordType);

		DataGroup recordInfo = DataGroup.withNameInData("recordInfo");
		recordInfo.addChild(DataAtomic.withNameInDataAndValue("id", "place"));
		recordInfo.addChild(DataAtomic.withNameInDataAndValue("type", recordType));
		dataGroup.addChild(recordInfo);

		dataGroup.addChild(DataAtomic.withNameInDataAndValue("id", "place"));
		dataGroup.addChild(DataAtomic.withNameInDataAndValue("metadataId", "metadata:place"));
		dataGroup.addChild(DataAtomic.withNameInDataAndValue("presentationViewId",
				"presentation:pgPlaceView"));
		dataGroup.addChild(DataAtomic.withNameInDataAndValue("presentationFormId",
				"presentation:pgPlaceForm"));
		dataGroup.addChild(DataAtomic.withNameInDataAndValue("newMetadataId", "metadata:placeNew"));
		dataGroup.addChild(DataAtomic.withNameInDataAndValue("newPresentationFormId",
				"presentation:pgPlaceFormNew"));
		dataGroup.addChild(DataAtomic.withNameInDataAndValue("listPresentationViewId",
				"presentation:pgPlaceViewList"));
		dataGroup.addChild(
				DataAtomic.withNameInDataAndValue("searchMetadataId", "metadata:placeSearch"));
		dataGroup.addChild(DataAtomic.withNameInDataAndValue("searchPresentationFormId",
				"presentation:pgPlaceSearchForm"));
		dataGroup.addChild(DataAtomic.withNameInDataAndValue("userSuppliedId", "false"));
		dataGroup.addChild(DataAtomic.withNameInDataAndValue("permissionKey", "RECORDTYPE_PLACE"));
		dataGroup.addChild(DataAtomic.withNameInDataAndValue("selfPresentationViewId",
				"presentation:pgPlaceRecordType"));
		dataGroup.addChild(DataAtomic.withNameInDataAndValue("abstract", "false"));
		recordsInMemory.create(recordType, "place", dataGroup,
				DataGroup.withNameInData("collectedLinksList"));
	}
}
