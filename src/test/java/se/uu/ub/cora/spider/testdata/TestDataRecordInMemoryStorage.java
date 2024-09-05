/*
 * Copyright 2015, 2019 Uppsala University Library
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

package se.uu.ub.cora.spider.testdata;

import java.util.Collections;
import java.util.Set;

import se.uu.ub.cora.data.DataAtomic;
import se.uu.ub.cora.data.DataGroup;
import se.uu.ub.cora.data.collected.Link;
import se.uu.ub.cora.spider.data.DataAtomicOldSpy;
import se.uu.ub.cora.spider.data.DataGroupOldSpy;
import se.uu.ub.cora.spider.record.storage.RecordStorageInMemoryStub;
import se.uu.ub.cora.spider.testspies.DataRecordLinkSpy;
import se.uu.ub.cora.storage.RecordStorage;

public class TestDataRecordInMemoryStorage {

	private static String dataDivider = "cora";
	private static Set<Link> emptyLinkList = Collections.emptySet();

	public static RecordStorageInMemoryStub createRecordStorageInMemoryWithTestData() {
		RecordStorageInMemoryStub recordsInMemory = new RecordStorageInMemoryStub();
		addPlace(recordsInMemory);
		addSecondPlace(recordsInMemory);
		addThirdPlace(recordsInMemory);
		addFourthPlace(recordsInMemory);
		addMetadata(recordsInMemory);
		addMetadataForBinary(recordsInMemory);
		addPresentation(recordsInMemory);
		addText(recordsInMemory);
		addRecordType(recordsInMemory);
		addImageOne(recordsInMemory);
		addRecordTypeRecordType(recordsInMemory);
		addRecordTypeBinary(recordsInMemory);
		addRecordTypeUser(recordsInMemory);
		addRecordTypeImage(recordsInMemory);
		addRecordTypeRecordTypeAutoGeneratedId(recordsInMemory);
		addRecordTypePlace(recordsInMemory);
		addRecordTypeSearch(recordsInMemory);
		addRecordTypeSystem(recordsInMemory);
		addSearch(recordsInMemory);
		addSystem(recordsInMemory);
		addSearchWithTwoRecordTypeToSearchIn(recordsInMemory);
		addRecordTypeAbstractAuthority(recordsInMemory);
		addImage(recordsInMemory);

		DataGroup dummy = new DataGroupOldSpy("dummy");
		recordsInMemory.create("metadataCollectionVariable", "dummy1", dummy, null,
				Collections.emptySet(), dataDivider);
		recordsInMemory.create("metadataCollectionVariableChild", "dummy1", dummy, null,
				Collections.emptySet(), dataDivider);
		recordsInMemory.create("metadataItemCollection", "dummy1", dummy, null,
				Collections.emptySet(), dataDivider);
		recordsInMemory.create("metadataCollectionItem", "dummy1", dummy, null,
				Collections.emptySet(), dataDivider);
		recordsInMemory.create("metadataTextVariable", "dummy1", dummy, null,
				Collections.emptySet(), dataDivider);
		recordsInMemory.create("metadataRecordLink", "dummy1", dummy, null, Collections.emptySet(),
				dataDivider);
		recordsInMemory.create("metadataRecordRelation", "dummyRecordRelation", dummy, null,
				Collections.emptySet(), dataDivider);
		recordsInMemory.create("permissionRole", "dummyPermissionRole", dummy, null,
				Collections.emptySet(), dataDivider);
		return recordsInMemory;
	}

	private static void addPlace(RecordStorageInMemoryStub recordsInMemory) {
		DataGroup recordInfo = DataCreator.createRecordInfoWithRecordTypeAndRecordId("place",
				"place:0001");
		DataGroup dataGroup = new DataGroupOldSpy("authority");
		dataGroup.addChild(recordInfo);
		recordsInMemory.create("place", "place:0001", dataGroup, null, emptyLinkList, dataDivider);
	}

	private static void addSecondPlace(RecordStorage recordsInMemory) {
		DataGroup dataGroup = DataCreator2.createRecordWithNameInDataAndIdAndTypeAndLinkedRecordId(
				"authority", "place:0002", "place", "cora");

		DataRecordLinkSpy dataRecordLink = new DataRecordLinkSpy("link");
		dataGroup.addChild(dataRecordLink);
		addLinkedRecordTypeAndLinkedRecordIdToRecordLink("place", "place:0001", dataRecordLink);
		Link link = new Link("place", "place:0001");

		recordsInMemory.create("place", "place:0002", dataGroup, null, Set.of(link), "cora");
	}

	private static void addThirdPlace(RecordStorage recordsInMemory) {
		DataGroup dataGroup = DataCreator2.createRecordWithNameInDataAndIdAndTypeAndLinkedRecordId(
				"authority", "place:0003", "place", "cora");

		// DataGroup collectedLinksList = emptyLinkList;
		recordsInMemory.create("place", "place:0003", dataGroup, null, emptyLinkList, "cora");
	}

	private static void addFourthPlace(RecordStorage recordsInMemory) {
		DataGroup dataGroup = DataCreator2.createRecordWithNameInDataAndIdAndTypeAndLinkedRecordId(
				"authority", "place:0004", "place", "cora");

		DataGroup dataRecordLink = new DataGroupOldSpy("link");
		dataGroup.addChild(dataRecordLink);
		addLinkedRecordTypeAndLinkedRecordIdToRecordLink("authority", "place:0003", dataRecordLink);

		// DataGroup collectedLinksList = emptyLinkList;
		// DataGroup recordToRecordLink = new DataGroupOldSpy("recordToRecordLink");
		//
		// DataRecordLinkSpy from = new DataRecordLinkSpy("from");
		// recordToRecordLink.addChild(from);
		// addLinkedRecordTypeAndLinkedRecordIdToRecordLink("place", "place:0004", from);
		// DataRecordLinkSpy to = new DataRecordLinkSpy("to");
		// recordToRecordLink.addChild(to);
		// addLinkedRecordTypeAndLinkedRecordIdToRecordLink("authority", "place:0003", to);
		//
		// collectedLinksList.addChild(recordToRecordLink);

		Link link = new Link("authority", "place:0003");

		recordsInMemory.create("place", "place:0004", dataGroup, null, Set.of(link), "cora");
	}

	private static void addLinkedRecordTypeAndLinkedRecordIdToRecordLink(
			String linkedRecordTypeString, String linkedRecordIdString, DataGroup dataRecordLink) {
		DataAtomic linkedRecordType = new DataAtomicOldSpy("linkedRecordType", linkedRecordTypeString);
		dataRecordLink.addChild(linkedRecordType);

		DataAtomic linkedRecordId = new DataAtomicOldSpy("linkedRecordId", linkedRecordIdString);
		dataRecordLink.addChild(linkedRecordId);
	}

	private static void addMetadata(RecordStorageInMemoryStub recordsInMemory) {
		String metadata = "metadataGroup";
		DataGroup dataGroup = new DataGroupOldSpy("metadata");

		DataGroup recordInfo = DataCreator.createRecordInfoWithRecordTypeAndRecordId(metadata,
				"place");
		dataGroup.addChild(recordInfo);
		recordsInMemory.create(metadata, "place", dataGroup, null, emptyLinkList, dataDivider);
	}

	private static void addMetadataForBinary(RecordStorageInMemoryStub recordsInMemory) {
		String metadata = "metadataGroup";
		DataGroup dataGroup = new DataGroupOldSpy("metadata");

		DataGroup recordInfo = DataCreator.createRecordInfoWithRecordTypeAndRecordId(metadata,
				"binary");
		dataGroup.addChild(recordInfo);
		recordsInMemory.create(metadata, "binary", dataGroup, null, emptyLinkList, dataDivider);
	}

	private static void addPresentation(RecordStorageInMemoryStub recordsInMemory) {
		String presentation = "presentation";
		DataGroup dataGroup = new DataGroupOldSpy(presentation);

		DataGroup recordInfo = DataCreator.createRecordInfoWithRecordTypeAndRecordId(presentation,
				"placeView");
		dataGroup.addChild(recordInfo);

		recordsInMemory.create(presentation, "placeView", dataGroup, null, emptyLinkList,
				dataDivider);
	}

	private static void addText(RecordStorageInMemoryStub recordsInMemory) {
		String text = "text";
		DataGroup dataGroup = new DataGroupOldSpy("text");

		DataGroup recordInfo = DataCreator.createRecordInfoWithRecordTypeAndRecordId(text,
				"placeText");
		dataGroup.addChild(recordInfo);
		recordsInMemory.create(text, "placeText", dataGroup, null, emptyLinkList, dataDivider);
	}

	private static void addRecordType(RecordStorageInMemoryStub recordsInMemory) {
		String recordType = "recordType";
		DataGroup dataGroup = new DataGroupOldSpy(recordType);

		DataGroup recordInfo = DataCreator.createRecordInfoWithRecordTypeAndRecordId(recordType,
				"metadata");
		dataGroup.addChild(recordInfo);

		dataGroup.addChild(new DataAtomicOldSpy("abstract", "false"));
		recordsInMemory.create(recordType, "metadata", dataGroup, null, emptyLinkList, dataDivider);
	}

	private static void addRecordTypeRecordType(RecordStorageInMemoryStub recordsInMemory) {
		String recordType = "recordType";
		DataGroup dataGroup = DataCreator
				.createRecordTypeWithIdAndUserSuppliedIdAndAbstractAndPublicRead("recordType",
						"true", "false", "false");
		recordsInMemory.create(recordType, "recordType", dataGroup, null, emptyLinkList,
				dataDivider);
	}

	private static void addRecordTypeRecordTypeAutoGeneratedId(
			RecordStorageInMemoryStub recordsInMemory) {
		String recordType = "recordType";
		DataGroup dataGroup = DataCreator
				.createRecordTypeWithIdAndUserSuppliedIdAndAbstractAndPublicRead(
						"recordTypeAutoGeneratedId", "false", "false", "false");
		recordsInMemory.create(recordType, "recordTypeAutoGeneratedId", dataGroup, null,
				emptyLinkList, dataDivider);

	}

	private static void addRecordTypeBinary(RecordStorageInMemoryStub recordsInMemory) {
		String recordType = "recordType";
		DataGroup dataGroup = DataCreator
				.createRecordTypeWithIdAndUserSuppliedIdAndAbstractAndPublicRead("binary", "true",
						"true", "false");
		recordsInMemory.create(recordType, "binary", dataGroup, null, emptyLinkList, dataDivider);
	}

	private static void addRecordTypeUser(RecordStorageInMemoryStub recordsInMemory) {
		String recordType = "recordType";
		DataGroup dataGroup = DataCreator
				.createRecordTypeWithIdAndUserSuppliedIdAndAbstractAndPublicRead("user", "true",
						"false", "false");
		recordsInMemory.create(recordType, "user", dataGroup, null, emptyLinkList, dataDivider);
	}

	private static void addRecordTypeImage(RecordStorageInMemoryStub recordsInMemory) {
		String recordType = "recordType";
		DataGroup dataGroup = DataCreator
				.createRecordTypeWithIdAndUserSuppliedIdAndParentId("image", "true", "binary");
		recordsInMemory.create(recordType, "image", dataGroup, null, emptyLinkList, dataDivider);
	}

	private static void addImageOne(RecordStorageInMemoryStub recordsInMemory) {
		DataGroup dataGroup = DataCreator2.createRecordWithNameInDataAndIdAndTypeAndLinkedRecordId(
				"binary", "image:123456789", "binary", "cora");
		DataGroup resourceInfo = new DataGroupOldSpy("resourceInfo");
		dataGroup.addChild(resourceInfo);
		DataGroup master = new DataGroupOldSpy("master");
		resourceInfo.addChild(master);
		DataAtomic streamId = new DataAtomicOldSpy("streamId", "678912345");
		master.addChild(streamId);
		DataAtomic uploadedFileName = new DataAtomicOldSpy("filename", "adele.png");
		master.addChild(uploadedFileName);
		DataAtomic size = new DataAtomicOldSpy("filesize", "123");
		master.addChild(size);
		DataAtomic mimeType = new DataAtomicOldSpy("mimeType", "application/octet-stream");
		master.addChild(mimeType);
		recordsInMemory.create("binary", "image:123456789", dataGroup, null, emptyLinkList,
				dataDivider);
	}

	private static void addRecordTypePlace(RecordStorageInMemoryStub recordsInMemory) {
		String recordType = "recordType";
		DataGroup dataGroup = DataCreator
				.createRecordTypeWithIdAndUserSuppliedIdAndParentId("place", "false", "authority");

		DataGroup filter = new DataGroupOldSpy("filter");
		filter.addChild(new DataAtomicOldSpy("linkedRecordType", "metadataGroup"));
		filter.addChild(new DataAtomicOldSpy("linkedRecordId", "placeFilterGroup"));
		dataGroup.addChild(filter);

		recordsInMemory.create(recordType, "place", dataGroup, null, emptyLinkList, dataDivider);
	}

	private static void addRecordTypeSearch(RecordStorageInMemoryStub recordsInMemory) {
		String recordType = "recordType";
		DataGroup dataGroup = DataCreator
				.createRecordTypeWithIdAndUserSuppliedIdAndAbstractAndPublicRead("search", "true",
						"false", "false");

		recordsInMemory.create(recordType, "search", dataGroup, null, emptyLinkList, dataDivider);
	}

	private static void addSearch(RecordStorageInMemoryStub recordsInMemory) {
		DataGroup recordInfo = DataCreator.createRecordInfoWithRecordTypeAndRecordId("search",
				"aSearchId");
		DataGroup dataGroup = new DataGroupOldSpy("search");
		dataGroup.addChild(recordInfo);

		DataGroup metadataId = new DataGroupOldSpy("metadataId");
		metadataId.addChild(new DataAtomicOldSpy("linkedRecordType", "metadataGroup"));
		metadataId.addChild(new DataAtomicOldSpy("linkedRecordId", "searchResourcesGroup"));
		dataGroup.addChild(metadataId);

		DataGroup recordTypeToSearchInGroup = new DataGroupOldSpy("recordTypeToSearchIn");
		dataGroup.addChild(recordTypeToSearchInGroup);
		recordTypeToSearchInGroup.addChild(new DataAtomicOldSpy("linkedRecordType", "recordType"));
		recordTypeToSearchInGroup.addChild(new DataAtomicOldSpy("linkedRecordId", "place"));
		recordsInMemory.create("search", "aSearchId", dataGroup, null, emptyLinkList, dataDivider);
	}

	private static void addSearchWithTwoRecordTypeToSearchIn(
			RecordStorageInMemoryStub recordsInMemory) {
		DataGroup recordInfo = DataCreator.createRecordInfoWithRecordTypeAndRecordId("search",
				"anotherSearchId");

		DataGroup dataGroup = new DataGroupOldSpy("search");
		dataGroup.addChild(recordInfo);
		DataGroup metadataId = new DataGroupOldSpy("metadataId");
		metadataId.addChild(new DataAtomicOldSpy("linkedRecordType", "metadataGroup"));
		metadataId.addChild(new DataAtomicOldSpy("linkedRecordId", "searchResourcesGroup"));
		dataGroup.addChild(metadataId);

		DataGroup recordTypeToSearchInGroup = new DataGroupOldSpy("recordTypeToSearchIn");
		dataGroup.addChild(recordTypeToSearchInGroup);
		recordTypeToSearchInGroup.addChild(new DataAtomicOldSpy("linkedRecordType", "recordType"));
		recordTypeToSearchInGroup.addChild(new DataAtomicOldSpy("linkedRecordId", "place"));

		DataGroup recordTypeToSearchInGroup2 = new DataGroupOldSpy("recordTypeToSearchIn");
		dataGroup.addChild(recordTypeToSearchInGroup2);
		recordTypeToSearchInGroup2.addChild(new DataAtomicOldSpy("linkedRecordType", "recordType"));
		recordTypeToSearchInGroup2.addChild(new DataAtomicOldSpy("linkedRecordId", "image"));

		recordsInMemory.create("search", "anotherSearchId", dataGroup, null, emptyLinkList,
				dataDivider);
	}

	private static void addRecordTypeAbstractAuthority(RecordStorageInMemoryStub recordsInMemory) {
		String recordType = "recordType";

		DataGroup dataGroup = DataCreator
				.createRecordTypeWithIdAndUserSuppliedIdAndAbstractAndPublicRead(
						"abstractAuthority", "false", "true", "false");

		recordsInMemory.create(recordType, "abstractAuthority", dataGroup, null, emptyLinkList,
				dataDivider);
	}

	private static void addImage(RecordStorageInMemoryStub recordsInMemory) {
		DataGroup recordInfo = DataCreator.createRecordInfoWithRecordTypeAndRecordId("image",
				"image:0001");
		DataGroup dataGroup = new DataGroupOldSpy("binary");
		dataGroup.addChild(recordInfo);
		recordsInMemory.create("image", "image:0001", dataGroup, null, emptyLinkList, dataDivider);
	}

	private static void addRecordTypeSystem(RecordStorageInMemoryStub recordsInMemory) {
		String recordType = "recordType";
		DataGroup dataGroup = DataCreator
				.createRecordTypeWithIdAndUserSuppliedIdAndAbstractAndPublicRead("system", "true",
						"false", "false");

		recordsInMemory.create(recordType, "system", dataGroup, null, emptyLinkList, dataDivider);
	}

	private static void addSystem(RecordStorageInMemoryStub recordsInMemory) {
		DataGroup dataGroup = new DataGroupOldSpy("system");
		DataGroup recordInfo = DataCreator.createRecordInfoWithRecordTypeAndRecordId("system",
				"cora");
		dataGroup.addChild(recordInfo);
		dataGroup.addChild(new DataAtomicOldSpy("systemName", "cora"));

		recordsInMemory.create("system", "cora", dataGroup, null, emptyLinkList, dataDivider);
	}
}
