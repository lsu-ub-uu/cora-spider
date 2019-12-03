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

package se.uu.ub.cora.spider.testdata;

import se.uu.ub.cora.data.DataAtomic;
import se.uu.ub.cora.data.DataGroup;
import se.uu.ub.cora.spider.data.DataAtomicSpy;
import se.uu.ub.cora.spider.data.DataGroupSpy;

public final class DataCreator2 {

	public static DataGroup createRecordInfoWithRecordType(String recordType) {
		DataGroup recordInfo = new DataGroupSpy("recordInfo");
		DataGroup typeGroup = new DataGroupSpy("type");
		typeGroup.addChild(new DataAtomicSpy("linkedRecordType", "recordType"));
		typeGroup.addChild(new DataAtomicSpy("linkedRecordId", recordType));
		recordInfo.addChild(typeGroup);
		return recordInfo;
	}

	public static DataGroup createRecordInfoWithRecordTypeAndRecordIdAndDataDivider(
			String recordType, String recordId, String dataDivider) {
		DataGroup recordInfo = createRecordInfoWithRecordType(recordType);
		recordInfo.addChild(new DataAtomicSpy("id", recordId));
		recordInfo.addChild(createDataDividerWithLinkedRecordId(dataDivider));
		return recordInfo;
	}

	public static DataGroup createRecordInfoWithIdAndLinkedRecordId(String id,
			String linkedRecordId) {
		DataGroup createRecordInfo = new DataGroupSpy("recordInfo");
		DataGroup dataDivider = createDataDividerWithLinkedRecordId(linkedRecordId);
		createRecordInfo.addChild(dataDivider);
		createRecordInfo.addChild(new DataAtomicSpy("id", id));
		return createRecordInfo;
	}

	public static DataGroup createRecordWithNameInDataAndIdAndLinkedRecordId(String nameInData,
			String id, String linkedRecordId) {
		DataGroup record = new DataGroupSpy(nameInData);
		DataGroup createRecordInfo = createRecordInfoWithIdAndLinkedRecordId(id, linkedRecordId);
		record.addChild(createRecordInfo);
		return record;
	}

	// TODO:samordna recordInfos!
	public static DataGroup createRecordWithNameInDataAndLinkedRecordId(String nameInData,
			String linkedRecordId) {
		DataGroup record = new DataGroupSpy(nameInData);
		DataGroup createRecordInfo = createRecordInfoWithLinkedRecordId(linkedRecordId);
		record.addChild(createRecordInfo);
		return record;
	}

	public static DataGroup createRecordInfoWithLinkedRecordId(String linkedRecordId) {
		DataGroup createRecordInfo = new DataGroupSpy("recordInfo");
		DataGroup dataDivider = createDataDividerWithLinkedRecordId(linkedRecordId);
		createRecordInfo.addChild(dataDivider);
		return createRecordInfo;
	}

	public static DataGroup createDataDividerWithLinkedRecordId(String linkedRecordId) {
		DataRecordLinkSpy dataDivider = new DataRecordLinkSpy("dataDivider");
		dataDivider.addChild(new DataAtomicSpy("linkedRecordType", "system"));
		dataDivider.addChild(new DataAtomicSpy("linkedRecordId", linkedRecordId));
		return dataDivider;
	}

	public static DataGroup createSearchWithIdAndRecordTypeToSearchIn(String id,
			String idRecordTypeToSearchIn) {
		DataGroup search = new DataGroupSpy("search");
		DataGroup recordInfo = DataCreator2.createRecordInfoWithRecordType("search");
		recordInfo.addChild(new DataAtomicSpy("id", id));
		search.addChild(recordInfo);

		DataGroup recordTypeToSearchIn = new DataGroupSpy("recordTypeToSearchIn");
		recordTypeToSearchIn.addChild(new DataAtomicSpy("linkedRecordType", "recordType"));
		recordTypeToSearchIn.addChild(new DataAtomicSpy("linkedRecordId", idRecordTypeToSearchIn));
		search.addChild(recordTypeToSearchIn);
		return search;
	}

	public static DataGroup createRecordWithNameInDataAndIdAndTypeAndLinkedRecordId(
			String nameInData, String id, String recordType, String linkedRecordId) {
		DataGroup record = new DataGroupSpy(nameInData);
		DataGroup createRecordInfo = createRecordInfoWithIdAndTypeAndLinkedRecordId(id, recordType,
				linkedRecordId);
		record.addChild(createRecordInfo);
		return record;
	}

	public static DataGroup createRecordInfoWithIdAndTypeAndLinkedRecordId(String id,
			String recordType, String linkedRecordId) {
		DataGroup createRecordInfo = new DataGroupSpy("recordInfo");
		createRecordInfo.addChild(new DataAtomicSpy("id", id));
		DataRecordLinkSpy typeGroup = new DataRecordLinkSpy("type");
		typeGroup.addChild(new DataAtomicSpy("linkedRecordType", "recordType"));
		typeGroup.addChild(new DataAtomicSpy("linkedRecordId", recordType));
		createRecordInfo.addChild(typeGroup);

		DataGroup dataDivider = createDataDividerWithLinkedRecordId(linkedRecordId);
		createRecordInfo.addChild(dataDivider);
		return createRecordInfo;
	}

	public static DataGroup createWorkOrderWithIdAndRecordTypeAndRecordIdToIndex(String id,
			String recordType, String recordId) {
		DataGroup workOrder = new DataGroupSpy("workOrder");
		DataGroup recordInfo = new DataGroupSpy("recordInfo");
		recordInfo.addChild(new DataAtomicSpy("id", id));
		workOrder.addChild(recordInfo);

		DataGroup recordTypeLink = new DataGroupSpy("recordType");
		recordTypeLink.addChild(new DataAtomicSpy("linkedRecordType", "recordType"));
		recordTypeLink.addChild(new DataAtomicSpy("linkedRecordId", recordType));
		workOrder.addChild(recordTypeLink);

		workOrder.addChild(new DataAtomicSpy("recordId", recordId));
		workOrder.addChild(new DataAtomicSpy("type", "index"));
		return workOrder;
	}

	public static DataGroup createRecordWithNameInDataAndIdAndTypeAndLinkedRecordIdAndCreatedBy(
			String nameInData, String id, String recordType, String linkedRecordId,
			String createdBy) {
		DataGroup record = new DataGroupSpy(nameInData);
		DataGroup createRecordInfo = createRecordInfoWithIdAndTypeAndLinkedRecordId(id, recordType,
				linkedRecordId);
		record.addChild(createRecordInfo);
		DataGroup createdByGroup = new DataGroupSpy("createdBy");
		createdByGroup.addChild(new DataAtomicSpy("linkedRecordType", "user"));
		createdByGroup.addChild(new DataAtomicSpy("linkedRecordId", createdBy));
		createRecordInfo.addChild(createdByGroup);
		return record;
	}

	public static DataGroup createMetadataGroupWithTwoChildren() {
		DataGroup spiderDataGroup = new DataGroupSpy("metadata");
		DataGroup recordInfo = new DataGroupSpy("recordInfo");
		recordInfo.addChild(new DataAtomicSpy("id", "testNewGroup"));
		recordInfo.addChild(new DataAtomicSpy("type", "metadataGroup"));
		recordInfo.addChild(createDataDividerWithLinkedRecordId("test"));

		spiderDataGroup.addChild(recordInfo);

		spiderDataGroup.addChild(createChildReferences());

		return spiderDataGroup;
	}

	private static DataGroup createChildReferences() {
		DataGroup childReferences = new DataGroupSpy("childReferences");

		childReferences.addChild(createChildReference("childOne", "1", "1"));
		childReferences.addChild(createChildReference("childTwo", "0", "2"));

		return childReferences;
	}

	private static DataGroup createChildReference(String ref, String repeatMin, String repeatMax) {
		DataGroup childReference = new DataGroupSpy("childReference");

		DataGroup refGroup = new DataGroupSpy("ref");
		DataAtomic linkedRecordType = new DataAtomicSpy("linkedRecordType", "metadataGroup");
		refGroup.addChild(linkedRecordType);
		DataAtomic linkedRecordId = new DataAtomicSpy("linkedRecordId", ref);
		refGroup.addChild(linkedRecordId);

		refGroup.addAttributeByIdWithValue("type", "group");
		childReference.addChild(refGroup);

		DataAtomic repeatMinAtomic = new DataAtomicSpy("repeatMin", repeatMin);
		childReference.addChild(repeatMinAtomic);

		DataAtomic repeatMaxAtomic = new DataAtomicSpy("repeatMax", repeatMax);
		childReference.addChild(repeatMaxAtomic);

		return childReference;
	}

	public static DataGroup createMetadataGroupWithThreeChildren() {
		DataGroup spiderDataGroup = createMetadataGroupWithTwoChildren();
		DataGroup childReferences = spiderDataGroup.getFirstGroupWithNameInData("childReferences");
		childReferences.addChild(createChildReference("childThree", "1", "1"));

		return spiderDataGroup;
	}

	public static DataGroup createMetadataGroupWithCollectionVariableAsChild() {
		DataGroup spiderDataGroup = new DataGroupSpy("metadata");
		DataGroup recordInfo = new DataGroupSpy("recordInfo");
		recordInfo.addChild(new DataAtomicSpy("id", "testCollectionVar"));
		recordInfo.addChild(new DataAtomicSpy("type", "collectionVariable"));
		recordInfo.addChild(createDataDividerWithLinkedRecordId("test"));
		spiderDataGroup.addChild(recordInfo);
		spiderDataGroup.addChild(createRefCollectionIdWithLinkedRecordid("testItemCollection"));

		return spiderDataGroup;
	}

	public static DataGroup createRefCollectionIdWithLinkedRecordid(String linkedRecordId) {
		DataGroup refCollection = new DataGroupSpy("refCollection");
		refCollection.addChild(new DataAtomicSpy("linkedRecordType", "metadataItemCollection"));
		refCollection.addChild(new DataAtomicSpy("linkedRecordId", linkedRecordId));
		return refCollection;
	}

	public static DataGroup createMetadataGroupWithOneChild() {
		DataGroup spiderDataGroup = new DataGroupSpy("metadata");
		DataGroup recordInfo = new DataGroupSpy("recordInfo");
		recordInfo.addChild(new DataAtomicSpy("id", "testNewGroup"));
		recordInfo.addChild(new DataAtomicSpy("type", "metadataGroup"));
		recordInfo.addChild(createDataDividerWithLinkedRecordId("test"));

		spiderDataGroup.addChild(recordInfo);

		spiderDataGroup.addChild(createChildReference());

		return spiderDataGroup;
	}

	private static DataGroup createChildReference() {
		DataGroup childReferences = new DataGroupSpy("childReferences");

		childReferences.addChild(createChildReference("childOne", "1", "1"));

		return childReferences;
	}

	public static DataGroup createMetadataGroupWithRecordLinkAsChild() {
		DataGroup spiderDataGroup = new DataGroupSpy("metadata");
		DataGroup recordInfo = new DataGroupSpy("recordInfo");
		recordInfo.addChild(new DataAtomicSpy("id", "testRecordLink"));
		recordInfo.addChild(new DataAtomicSpy("type", "recordLink"));
		recordInfo.addChild(createDataDividerWithLinkedRecordId("test"));
		spiderDataGroup.addChild(recordInfo);

		return spiderDataGroup;
	}
}
