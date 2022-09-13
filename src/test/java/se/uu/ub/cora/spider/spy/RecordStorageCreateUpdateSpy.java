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

package se.uu.ub.cora.spider.spy;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.function.Supplier;

import se.uu.ub.cora.data.DataGroup;
import se.uu.ub.cora.data.DataRecordLink;
import se.uu.ub.cora.data.collected.RecordToRecordLink;
import se.uu.ub.cora.data.collected.StorageTerm;
import se.uu.ub.cora.spider.data.DataAtomicSpy;
import se.uu.ub.cora.spider.data.DataGroupOldSpy;
import se.uu.ub.cora.spider.testdata.DataCreator2;
import se.uu.ub.cora.storage.RecordNotFoundException;
import se.uu.ub.cora.storage.RecordStorage;
import se.uu.ub.cora.storage.StorageReadResult;
import se.uu.ub.cora.testspies.data.DataRecordLinkSpy;
import se.uu.ub.cora.testutils.mcr.MethodCallRecorder;

public class RecordStorageCreateUpdateSpy implements RecordStorage {
	public MethodCallRecorder MCR = new MethodCallRecorder();
	public DataGroup createRecord;
	public DataGroup updateRecord;
	public String dataDivider;
	public boolean createWasCalled = false;

	public boolean modifiableLinksExistsForRecord = false;
	public DataGroup group;
	public String type;
	public String id;
	public List<StorageTerm> storageTerms;

	@Override
	public DataGroup read(String type, String id) {
		MCR.addCall("type", type, "id", id);
		if (type.equals("recordType") && id.equals("typeWithAutoGeneratedId")) {
			DataGroup group = new DataGroupOldSpy("recordType");
			addMetadataIdAndMetadataIdNew(group, "place", "placeNew");
			group.addChild(new DataAtomicSpy("userSuppliedId", "false"));
			group.addChild(new DataAtomicSpy("abstract", "false"));
			MCR.addReturned(group);
			return group;
		}
		if (type.equals("recordType") && id.equals("typeWithUserGeneratedId")) {
			DataGroup group = new DataGroupOldSpy("recordType");
			addMetadataIdAndMetadataIdNew(group, "place", "placeNew");
			group.addChild(new DataAtomicSpy("userSuppliedId", "true"));
			group.addChild(new DataAtomicSpy("abstract", "false"));
			MCR.addReturned(group);
			return group;
		}
		if (type.equals("typeWithAutoGeneratedId") && id.equals("somePlace")) {
			if (null == group) {
				group = new DataGroupOldSpy("typeWithAutoGeneratedId");
				createAndAddRecordInfo(group);

				addMetadataIdAndMetadataIdNew(group, "place", "placeNew");
				group.addChild(new DataAtomicSpy("userSuppliedId", "true"));
				group.addChild(new DataAtomicSpy("abstract", "false"));
				group.addChild(new DataAtomicSpy("unit", "Uppsala"));
				group.addChild(new DataGroupOldSpy("recordInfo"));
			}
			MCR.addReturned(group);
			return group;
		}
		if (type.equals("typeWithUserGeneratedId") && id.equals("uppsalaRecord1")) {
			DataGroup group = new DataGroupOldSpy("typeWithUserGeneratedId");
			addMetadataIdAndMetadataIdNew(group, "place", "placeNew");
			group.addChild(new DataAtomicSpy("userSuppliedId", "true"));
			group.addChild(new DataAtomicSpy("abstract", "false"));
			group.addChild(new DataAtomicSpy("unit", "Uppsala"));
			group.addChild(new DataGroupOldSpy("recordInfo"));
			MCR.addReturned(group);
			return group;
		}
		if (type.equals("typeWithUserGeneratedId") && id.equals("gothenburgRecord1")) {
			DataGroup group = new DataGroupOldSpy("typeWithUserGeneratedId");
			addMetadataIdAndMetadataIdNew(group, "place", "placeNew");
			group.addChild(new DataAtomicSpy("userSuppliedId", "true"));
			group.addChild(new DataAtomicSpy("abstract", "false"));
			group.addChild(new DataAtomicSpy("unit", "gothenburg"));
			group.addChild(new DataGroupOldSpy("recordInfo"));
			MCR.addReturned(group);
			return group;
		}
		if (type.equals("recordType") && id.equals("recordType")) {
			DataGroup group = new DataGroupOldSpy("recordType");
			addMetadataIdAndMetadataIdNew(group, "recordType", "recordTypeNew");
			group.addChild(new DataAtomicSpy("recordInfo", "recordInfo"));
			group.addChild(new DataAtomicSpy("userSuppliedId", "true"));
			group.addChild(new DataAtomicSpy("abstract", "false"));
			group.addChild(new DataGroupOldSpy("recordInfo"));
			MCR.addReturned(group);
			return group;
		}
		if (type.equals("recordType") && id.equals("image")) {
			DataGroup group = new DataGroupOldSpy("recordType");
			addMetadataIdAndMetadataIdNew(group, "image", "imageNew");
			group.addChild(new DataAtomicSpy("userSuppliedId", "true"));
			group.addChild(new DataAtomicSpy("abstract", "false"));
			group.addChild(new DataGroupOldSpy("recordInfo"));

			// DataGroup parentIdGroup = new DataGroupOldSpy("parentId");
			// parentIdGroup.addChild(new DataAtomicSpy("linkedRecordType", "recordType"));
			// parentIdGroup.addChild(new DataAtomicSpy("linkedRecordId", "binary"));
			// group.addChild(parentIdGroup);
			DataRecordLinkSpy linkSpy = new DataRecordLinkSpy();
			linkSpy.MRV.setDefaultReturnValuesSupplier("getNameInData",
					(Supplier<String>) () -> "parentId");
			linkSpy.MRV.setDefaultReturnValuesSupplier("getLinkedRecordId",
					(Supplier<String>) () -> "binary");
			group.addChild(linkSpy);
			MCR.addReturned(group);
			return group;
		}
		if (type.equals("recordType") && id.equals("binary")) {
			DataGroup group = new DataGroupOldSpy("recordType");
			addMetadataIdAndMetadataIdNew(group, "binary", "binaryNew");
			group.addChild(new DataAtomicSpy("userSuppliedId", "true"));
			group.addChild(new DataAtomicSpy("abstract", "true"));
			group.addChild(new DataGroupOldSpy("recordInfo"));
			MCR.addReturned(group);
			return group;
		}
		if (type.equals("recordType") && id.equals("metadataGroup")) {
			DataGroup group = new DataGroupOldSpy("metadata");
			addMetadataIdAndMetadataIdNew(group, "metadataGroupGroup", "metadataGroupNewGroup");
			group.addChild(new DataAtomicSpy("userSuppliedId", "true"));
			group.addChild(new DataAtomicSpy("abstract", "false"));
			group.addChild(new DataGroupOldSpy("recordInfo"));
			MCR.addReturned(group);
			return group;
		}
		if (type.equals("recordType") && id.equals("metadataCollectionVariable")) {
			DataGroup group = new DataGroupOldSpy("metadata");
			addMetadataIdAndMetadataIdNew(group, "metadataCollectionVariableGroup",
					"metadataCollectionVariableNewGroup");
			group.addChild(new DataAtomicSpy("userSuppliedId", "true"));
			group.addChild(new DataAtomicSpy("abstract", "false"));
			group.addChild(new DataGroupOldSpy("recordInfo"));
			MCR.addReturned(group);
			return group;
		}
		if (type.equals("recordType") && id.equals("metadataRecordLink")) {
			DataGroup group = new DataGroupOldSpy("metadata");
			addMetadataIdAndMetadataIdNew(group, "metadataRecordLinkGroup",
					"metadataRecordLinkNewGroup");
			group.addChild(new DataAtomicSpy("userSuppliedId", "true"));
			group.addChild(new DataAtomicSpy("abstract", "false"));
			group.addChild(new DataGroupOldSpy("recordInfo"));
			MCR.addReturned(group);
			return group;
		}
		if (type.equals("recordType") && id.equals("typeWithAutoGeneratedIdWrongRecordInfo")) {
			DataGroup group = new DataGroupOldSpy("recordType");
			addMetadataIdAndMetadataIdNew(group, "typeWithAutoGeneratedIdWrongRecordInfo",
					"typeWithAutoGeneratedIdWrongRecordInfoGroup");
			group.addChild(new DataAtomicSpy("userSuppliedId", "false"));
			group.addChild(new DataAtomicSpy("abstract", "false"));
			MCR.addReturned(group);

			return group;
		}
		if (type.equals("metadataGroup") && id.equals("testGroup")) {
			DataGroup group = new DataGroupOldSpy("testGroup");

			DataGroup childReferences = new DataGroupOldSpy("childReferences");
			childReferences.addChild(createChildReference("childOne", "1", "1"));
			group.addChild(childReferences);
			group.addChild(new DataGroupOldSpy("recordInfo"));
			MCR.addReturned(group);
			return group;
		}
		if (type.equals("metadataGroup") && id.equals("childOne")) {
			DataGroup group = new DataGroupOldSpy("metadata");
			group.addChild(new DataAtomicSpy("nameInData", "childOne"));
			MCR.addReturned(group);
			return group;
		}
		if (type.equals("metadataGroup") && id.equals("childTwo")) {
			DataGroup group = new DataGroupOldSpy("metadata");
			group.addChild(new DataAtomicSpy("nameInData", "childTwo"));
			group.addChild(new DataGroupOldSpy("recordInfo"));
			MCR.addReturned(group);
			return group;
		}
		if (type.equals("metadataGroup") && id.equals("testGroupWithOneChild")) {
			DataGroup group = new DataGroupOldSpy("testGroupWithOneChild");
			group.addChild(new DataGroupOldSpy("recordInfo"));

			DataGroup childReferences = new DataGroupOldSpy("childReferences");
			childReferences.addChild(createChildReference("childOne", "1", "1"));
			group.addChild(childReferences);
			MCR.addReturned(group);
			return group;
		}
		if (type.equals("metadataGroup") && id.equals("testGroupWithTwoChildren")) {
			DataGroup group = new DataGroupOldSpy("testGroupWithTwoChildren");
			group.addChild(new DataGroupOldSpy("recordInfo"));

			DataGroup childReferences = new DataGroupOldSpy("childReferences");
			childReferences.addChild(createChildReference("childOne", "1", "1"));
			childReferences
					.addChild(createChildReference("childWithSameNameInDataAsChildTwo", "0", "1"));
			group.addChild(childReferences);
			MCR.addReturned(group);
			return group;
		}
		if (type.equals("metadataGroup") && id.equals("testGroupWithThreeChildren")) {
			DataGroup group = new DataGroupOldSpy("testGroupWithTwoChildren");
			group.addChild(new DataGroupOldSpy("recordInfo"));

			DataGroup childReferences = new DataGroupOldSpy("childReferences");
			childReferences.addChild(createChildReference("childOne", "1", "1"));
			childReferences.addChild(createChildReference("childTwo", "0", "1"));
			childReferences.addChild(createChildReference("childThree", "1", "1"));
			group.addChild(childReferences);
			MCR.addReturned(group);
			return group;
		}
		if (type.equals("metadataGroup") && id.equals("childWithSameNameInDataAsChildTwo")) {
			// name in data is not same as id to test same scenario as
			// recordInfoGroup/recordInfoNewGroup
			// different id, same name in data
			DataGroup group = new DataGroupOldSpy("metadata");
			group.addChild(new DataGroupOldSpy("recordInfo"));
			group.addChild(new DataAtomicSpy("nameInData", "childTwo"));
			MCR.addReturned(group);
			return group;
		}
		if (type.equals("metadataGroup") && id.equals("childThree")) {
			throw new RecordNotFoundException("No record exists with recordId: childThree");
		}
		if (id.equals("testItemCollection")) {
			DataGroup group = new DataGroupOldSpy("metadata");

			DataGroup itemReferences = new DataGroupOldSpy("collectionItemReferences");

			createAndAddItemReference(itemReferences, "thisItem", "one");
			createAndAddItemReference(itemReferences, "thatItem", "two");

			group.addChild(itemReferences);
			MCR.addReturned(group);
			return group;
		}
		if (id.equals("testParentMissingItemCollectionVar")) {
			DataGroup group = new DataGroupOldSpy("metadata");
			group.addChild(new DataGroupOldSpy("recordInfo"));

			DataGroup refCollection = new DataGroupOldSpy("refCollection");
			refCollection.addChild(new DataAtomicSpy("linkedRecordType", "metadataItemCollection"));
			refCollection.addChild(
					new DataAtomicSpy("linkedRecordId", "testParentMissingItemCollection"));
			group.addChild(refCollection);
			MCR.addReturned(group);

			return group;
		}
		if (id.equals("testParentMissingItemCollection")) {
			DataGroup group = new DataGroupOldSpy("metadata");
			group.addChild(new DataGroupOldSpy("recordInfo"));

			DataGroup itemReferences = new DataGroupOldSpy("collectionItemReferences");

			createAndAddItemReference(itemReferences, "thisItem", "one");
			createAndAddItemReference(itemReferences, "thoseItem", "two");
			group.addChild(itemReferences);
			MCR.addReturned(group);
			return group;
		}
		if (id.equals("testParentCollectionVar")) {
			DataGroup group = new DataGroupOldSpy("metadata");
			group.addChild(new DataGroupOldSpy("recordInfo"));
			DataGroup refCollection = new DataGroupOldSpy("refCollection");
			refCollection.addChild(new DataAtomicSpy("linkedRecordType", "metadataItemCollection"));
			refCollection.addChild(new DataAtomicSpy("linkedRecordId", "testParentItemCollection"));
			group.addChild(refCollection);
			MCR.addReturned(group);
			return group;
		}
		if (id.equals("testParentItemCollection")) {
			DataGroup group = new DataGroupOldSpy("metadata");
			group.addChild(new DataGroupOldSpy("recordInfo"));

			DataGroup itemReferences = new DataGroupOldSpy("collectionItemReferences");

			createAndAddItemReference(itemReferences, "thisItem", "one");
			createAndAddItemReference(itemReferences, "thatItem", "two");
			createAndAddItemReference(itemReferences, "thoseItem", "three");

			group.addChild(itemReferences);
			MCR.addReturned(group);
			return group;
		}
		if (type.equals("metadataCollectionItem") && id.equals("thisItem")) {
			DataGroup group = new DataGroupOldSpy("metadata");
			group.addChild(new DataGroupOldSpy("recordInfo"));
			group.addChild(new DataAtomicSpy("nameInData", "this"));
			MCR.addReturned(group);
			return group;
		}
		if (type.equals("metadataCollectionItem") && id.equals("thatItem")) {
			DataGroup group = new DataGroupOldSpy("metadata");
			group.addChild(new DataGroupOldSpy("recordInfo"));
			group.addChild(new DataAtomicSpy("nameInData", "that"));
			MCR.addReturned(group);
			return group;
		}
		if ("image".equals(type) && "image:123456789".equals(id)) {
			DataGroup group = DataCreator2.createRecordWithNameInDataAndIdAndLinkedRecordId("image",
					"image:123456789", "cora");
			MCR.addReturned(group);
			return group;
		}
		DataGroup dataGroupToReturn = new DataGroupOldSpy("someNameInData");
		createAndAddRecordInfo(dataGroupToReturn);
		MCR.addReturned(dataGroupToReturn);
		return dataGroupToReturn;
	}

	private void createAndAddRecordInfo(DataGroup group) {
		DataGroup recordInfo = new DataGroupOldSpy("recordInfo");
		DataGroup createdBy = new DataGroupOldSpy("createdBy");
		createdBy.addChild(new DataAtomicSpy("linkedRecordType", "user"));
		createdBy.addChild(new DataAtomicSpy("linkedRecordId", "6789"));
		recordInfo.addChild(createdBy);
		recordInfo.addChild(new DataAtomicSpy("tsCreated", "2016-10-01T00:00:00.000000Z"));
		group.addChild(recordInfo);
	}

	private void addMetadataIdAndMetadataIdNew(DataGroup group, String metadataId,
			String metadataIdNew) {
		group.addChild(createLinkWithLinkedId("newMetadataId", "metadataGroup", metadataId));
		group.addChild(createLinkWithLinkedId("metadataId", "metadataGroup", metadataIdNew));
	}

	private DataGroup createChildReference(String refId, String repeatMin, String repeatMax) {
		DataGroup childReference = new DataGroupOldSpy("childReference");

		DataGroup ref = new DataGroupOldSpy("ref");
		ref.addChild(new DataAtomicSpy("linkedRecordType", "metadataGroup"));
		ref.addChild(new DataAtomicSpy("linkedRecordId", refId));
		ref.addAttributeByIdWithValue("type", "group");
		childReference.addChild(ref);
		childReference.addChild(new DataAtomicSpy("repeatMin", repeatMin));
		childReference.addChild(new DataAtomicSpy("repeatMax", repeatMax));
		return childReference;
	}

	private static DataRecordLink createLinkWithLinkedId(String nameInData, String linkedRecordType,
			String id) {
		DataRecordLinkSpy linkSpy = new DataRecordLinkSpy();
		linkSpy.MRV.setDefaultReturnValuesSupplier("getNameInData",
				(Supplier<String>) () -> nameInData);
		linkSpy.MRV.setDefaultReturnValuesSupplier("getLinkedRecordId",
				(Supplier<String>) () -> id);
		return linkSpy;
	}

	private void createAndAddItemReference(DataGroup collectionItemReferences,
			String linkedRecordId, String repeatId) {
		DataGroup ref1 = new DataGroupOldSpy("ref");
		ref1.setRepeatId(repeatId);
		ref1.addChild(new DataAtomicSpy("linkedRecordType", "metadataCollectionItem"));
		ref1.addChild(new DataAtomicSpy("linkedRecordId", linkedRecordId));
		collectionItemReferences.addChild(ref1);
	}

	@Override
	public void create(String type, String id, DataGroup record, List<StorageTerm> storageTerms,
			List<RecordToRecordLink> links, String dataDivider) {
		this.type = type;
		this.id = id;
		createRecord = record;
		this.storageTerms = storageTerms;
		this.dataDivider = dataDivider;
		createWasCalled = true;
	}

	@Override
	public void deleteByTypeAndId(String type, String id) {
		// TODO Auto-generated method stub

	}

	@Override
	public boolean linksExistForRecord(String type, String id) {
		return modifiableLinksExistsForRecord;
	}

	@Override
	public void update(String type, String id, DataGroup record, List<StorageTerm> storageTerms,
			List<RecordToRecordLink> links, String dataDivider) {
		updateRecord = record;
		this.storageTerms = storageTerms;
		this.dataDivider = dataDivider;
	}

	@Override
	public StorageReadResult readList(String type, DataGroup filter) {
		ArrayList<DataGroup> recordTypeList = new ArrayList<>();

		DataGroup metadataGroup = new DataGroupOldSpy("recordType");
		DataGroup recordInfo = new DataGroupOldSpy("recordInfo");
		recordInfo.addChild(new DataAtomicSpy("id", "metadata"));
		metadataGroup.addChild(recordInfo);
		recordTypeList.add(metadataGroup);

		DataGroup metadataGroupGroup = new DataGroupOldSpy("recordType");
		DataGroup recordInfoMetadataGroup = new DataGroupOldSpy("recordInfo");
		recordInfoMetadataGroup.addChild(new DataAtomicSpy("id", "metadataGroup"));
		metadataGroupGroup.addChild(recordInfoMetadataGroup);

		// DataGroup par entIdGroup = new DataGroupOldSpy("parentId");
		// parentIdGroup.addChild(new DataAtomicSpy("linkedRecordType", "recordType"));
		// parentIdGroup.addChild(new DataAtomicSpy("linkedRecordId", "binary"));
		// metadataGroupGroup.addChild(parentIdGroup);
		metadataGroupGroup.addChild(createLinkWithLinkedId("parentId", "recordType", "binary"));

		recordTypeList.add(metadataGroupGroup);

		DataGroup metadataTextVariable = new DataGroupOldSpy("recordType");
		DataGroup recordInfoTextVariable = new DataGroupOldSpy("recordInfo");
		recordInfoTextVariable.addChild(new DataAtomicSpy("id", "metadataTextVariable"));
		metadataTextVariable.addChild(recordInfoTextVariable);

		// DataGroup parentIdGroupTextVar = new DataGroupOldSpy("parentId");
		// parentIdGroupTextVar.addChild(new DataAtomicSpy("linkedRecordType", "recordType"));
		// parentIdGroupTextVar.addChild(new DataAtomicSpy("linkedRecordId", "binary"));
		// metadataTextVariable.addChild(parentIdGroupTextVar);
		metadataTextVariable.addChild(createLinkWithLinkedId("parentId", "recordType", "binary"));

		// metadataTextVariable.addChild(new DataAtomicSpy("parentId",
		// "metadata"));
		recordTypeList.add(metadataTextVariable);

		DataGroup presentationVar = new DataGroupOldSpy("recordType");
		DataGroup recordInfoPresentationVar = new DataGroupOldSpy("recordInfo");
		recordInfoPresentationVar.addChild(new DataAtomicSpy("id", "presentationVar"));

		// DataGroup parentIdGroupPresentationVar = new DataGroupOldSpy("parentId");
		// parentIdGroupPresentationVar.addChild(new DataAtomicSpy("linkedRecordType",
		// "recordType"));
		// parentIdGroupPresentationVar.addChild(new DataAtomicSpy("linkedRecordId", "binary"));
		// presentationVar.addChild(parentIdGroupPresentationVar);
		presentationVar.addChild(createLinkWithLinkedId("parentId", "recordType", "binary"));
		presentationVar.addChild(recordInfoTextVariable);

		// presentationVar.addChild(new DataAtomicSpy("parentId",
		// "presentation"));
		recordTypeList.add(presentationVar);
		StorageReadResult spiderReadResult = new StorageReadResult();
		spiderReadResult.listOfDataGroups = recordTypeList;
		return spiderReadResult;

	}

	@Override
	public StorageReadResult readAbstractList(String type, DataGroup filter) {
		return null;
	}

	@Override
	public DataGroup readLinkList(String type, String id) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Collection<DataGroup> generateLinkCollectionPointingToRecord(String type, String id) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public boolean recordExistsForAbstractOrImplementingRecordTypeAndRecordId(String type,
			String id) {
		return false;
	}

	@Override
	public long getTotalNumberOfRecordsForType(String type, DataGroup filter) {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public long getTotalNumberOfRecordsForAbstractType(String abstractType,
			List<String> implementingTypes, DataGroup filter) {
		// TODO Auto-generated method stub
		return 0;
	}

}
