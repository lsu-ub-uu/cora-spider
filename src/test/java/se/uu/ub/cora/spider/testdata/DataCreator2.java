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

import java.util.function.Supplier;

import se.uu.ub.cora.data.DataAtomic;
import se.uu.ub.cora.data.DataGroup;
import se.uu.ub.cora.data.DataRecordLink;
import se.uu.ub.cora.data.spies.DataRecordLinkSpy;
import se.uu.ub.cora.spider.data.DataAtomicSpy;
import se.uu.ub.cora.spider.data.DataGroupOldSpy;

public final class DataCreator2 {

	public static DataGroup createRecordInfoWithRecordType(String recordType) {
		DataGroup recordInfo = new DataGroupOldSpy("recordInfo");
		recordInfo.addChild(createLinkWithLinkedId("type", "recordType", recordType));

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
		DataGroup createRecordInfo = new DataGroupOldSpy("recordInfo");
		DataRecordLink dataDivider = createDataDividerWithLinkedRecordId(linkedRecordId);
		createRecordInfo.addChild(dataDivider);
		createRecordInfo.addChild(new DataAtomicSpy("id", id));
		return createRecordInfo;
	}

	public static DataRecordLink createLinkWithLinkedId(String nameInData, String linkedRecordType,
			String id) {
		DataRecordLinkSpy linkSpy = new DataRecordLinkSpy();
		linkSpy.MRV.setDefaultReturnValuesSupplier("getNameInData",
				(Supplier<String>) () -> nameInData);
		linkSpy.MRV.setDefaultReturnValuesSupplier("getLinkedRecordType",
				(Supplier<String>) () -> linkedRecordType);
		linkSpy.MRV.setDefaultReturnValuesSupplier("getLinkedRecordId",
				(Supplier<String>) () -> id);
		return linkSpy;
	}

	public static DataGroup createRecordWithNameInDataAndIdAndLinkedRecordId(String nameInData,
			String id, String linkedRecordId) {
		DataGroup record = new DataGroupOldSpy(nameInData);
		DataGroup createRecordInfo = createRecordInfoWithIdAndLinkedRecordId(id, linkedRecordId);
		record.addChild(createRecordInfo);
		return record;
	}

	public static DataGroup createRecordWithNameInDataAndLinkedRecordId(String nameInData,
			String linkedRecordId) {
		DataGroup record = new DataGroupOldSpy(nameInData);
		DataGroup createRecordInfo = createRecordInfoWithLinkedRecordId(linkedRecordId);
		record.addChild(createRecordInfo);
		return record;
	}

	public static DataGroup createRecordInfoWithLinkedRecordId(String linkedRecordId) {
		DataGroup createRecordInfo = new DataGroupOldSpy("recordInfo");
		DataRecordLink dataDivider = createDataDividerWithLinkedRecordId(linkedRecordId);
		createRecordInfo.addChild(dataDivider);
		return createRecordInfo;
	}

	public static DataRecordLink createDataDividerWithLinkedRecordId(String linkedRecordId) {
		DataRecordLinkSpy linkSpy = new DataRecordLinkSpy();
		linkSpy.MRV.setDefaultReturnValuesSupplier("getNameInData",
				(Supplier<String>) () -> "dataDivider");
		linkSpy.MRV.setDefaultReturnValuesSupplier("getLinkedRecordType",
				(Supplier<String>) () -> "system");
		linkSpy.MRV.setDefaultReturnValuesSupplier("getLinkedRecordId",
				(Supplier<String>) () -> linkedRecordId);
		return linkSpy;
	}

	public static DataGroup createSearchWithIdAndRecordTypeToSearchIn(String id,
			String idRecordTypeToSearchIn) {
		DataGroup search = new DataGroupOldSpy("search");
		DataGroup recordInfo = DataCreator2.createRecordInfoWithRecordType("search");
		recordInfo.addChild(new DataAtomicSpy("id", id));
		search.addChild(recordInfo);

		DataGroup recordTypeToSearchIn = new DataGroupOldSpy("recordTypeToSearchIn");
		recordTypeToSearchIn.addChild(new DataAtomicSpy("linkedRecordType", "recordType"));
		recordTypeToSearchIn.addChild(new DataAtomicSpy("linkedRecordId", idRecordTypeToSearchIn));
		search.addChild(recordTypeToSearchIn);
		return search;
	}

	public static DataGroup createRecordWithNameInDataAndIdAndTypeAndLinkedRecordId(
			String nameInData, String id, String recordType, String linkedRecordId) {
		DataGroup record = new DataGroupOldSpy(nameInData);
		DataGroup createRecordInfo = createRecordInfoWithIdAndTypeAndLinkedRecordId(id, recordType,
				linkedRecordId);
		record.addChild(createRecordInfo);
		return record;
	}

	public static DataGroup createRecordInfoWithIdAndTypeAndLinkedRecordId(String id,
			String recordType, String linkedRecordId) {
		DataGroup createRecordInfo = new DataGroupOldSpy("recordInfo");
		createRecordInfo.addChild(new DataAtomicSpy("id", id));
		createRecordInfo.addChild(createLinkWithLinkedId("type", "recordType", recordType));

		DataRecordLink dataDivider = createDataDividerWithLinkedRecordId(linkedRecordId);
		createRecordInfo.addChild(dataDivider);
		return createRecordInfo;
	}

	public static DataGroup createWorkOrderWithIdAndRecordTypeAndRecordIdToIndex(String id,
			String recordType, String recordId) {
		String workOrderType = "index";
		return createWorkOrderWithIdRecordTypeRecordIdAndWorkOrderType(id, recordType, recordId,
				workOrderType);
	}

	public static DataGroup createWorkOrderWithIdRecordTypeRecordIdAndWorkOrderType(String id,
			String recordType, String recordId, String workOrderType) {
		DataGroup workOrder = new DataGroupOldSpy("workOrder");
		DataGroup recordInfo = new DataGroupOldSpy("recordInfo");
		recordInfo.addChild(new DataAtomicSpy("id", id));
		workOrder.addChild(recordInfo);

		DataRecordLinkSpy recordTypeLink = new DataRecordLinkSpy();
		recordTypeLink.MRV.setDefaultReturnValuesSupplier("getNameInData",
				(Supplier<String>) () -> "recordType");
		recordTypeLink.MRV.setDefaultReturnValuesSupplier("getLinkedRecordId",
				(Supplier<String>) () -> recordType);
		workOrder.addChild(recordTypeLink);

		workOrder.addChild(new DataAtomicSpy("recordId", recordId));
		workOrder.addChild(new DataAtomicSpy("type", workOrderType));
		return workOrder;
	}

	public static DataGroup createRecordWithNameInDataAndIdAndTypeAndLinkedRecordIdAndCreatedBy(
			String nameInData, String id, String recordType, String linkedRecordId,
			String createdBy) {
		DataGroup record = new DataGroupOldSpy(nameInData);
		DataGroup createRecordInfo = createRecordInfoWithIdAndTypeAndLinkedRecordId(id, recordType,
				linkedRecordId);
		record.addChild(createRecordInfo);
		DataGroup createdByGroup = new DataGroupOldSpy("createdBy");
		createdByGroup.addChild(new DataAtomicSpy("linkedRecordType", "user"));
		createdByGroup.addChild(new DataAtomicSpy("linkedRecordId", createdBy));
		createRecordInfo.addChild(createdByGroup);
		return record;
	}

	public static DataGroup createMetadataGroupWithTwoChildren() {
		DataGroup dataGroup = new DataGroupOldSpy("metadata");
		DataGroup recordInfo = new DataGroupOldSpy("recordInfo");
		recordInfo.addChild(new DataAtomicSpy("id", "testNewGroup"));
		recordInfo.addChild(new DataAtomicSpy("type", "metadataGroup"));
		recordInfo.addChild(createDataDividerWithLinkedRecordId("test"));

		dataGroup.addChild(recordInfo);

		dataGroup.addChild(createChildReferences());

		return dataGroup;
	}

	private static DataGroup createChildReferences() {
		DataGroup childReferences = new DataGroupOldSpy("childReferences");

		childReferences.addChild(createChildReference("childOne", "1", "1"));
		childReferences.addChild(createChildReference("childTwo", "0", "2"));

		return childReferences;
	}

	private static DataGroup createChildReference(String ref, String repeatMin, String repeatMax) {
		DataGroup childReference = new DataGroupOldSpy("childReference");

		DataGroup refGroup = new DataGroupOldSpy("ref");
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
		DataGroup dataGroup = createMetadataGroupWithTwoChildren();
		DataGroup childReferences = dataGroup.getFirstGroupWithNameInData("childReferences");
		childReferences.addChild(createChildReference("childThree", "1", "1"));

		return dataGroup;
	}

	public static DataGroup createMetadataGroupWithCollectionVariableAsChild() {
		DataGroup dataGroup = new DataGroupOldSpy("metadata");
		DataGroup recordInfo = new DataGroupOldSpy("recordInfo");
		recordInfo.addChild(new DataAtomicSpy("id", "testCollectionVar"));
		recordInfo.addChild(new DataAtomicSpy("type", "collectionVariable"));
		recordInfo.addChild(createDataDividerWithLinkedRecordId("test"));
		dataGroup.addChild(recordInfo);
		dataGroup.addChild(createRefCollectionIdWithLinkedRecordid("testItemCollection"));

		return dataGroup;
	}

	public static DataGroup createRefCollectionIdWithLinkedRecordid(String linkedRecordId) {
		DataGroup refCollection = new DataGroupOldSpy("refCollection");
		refCollection.addChild(new DataAtomicSpy("linkedRecordType", "metadataItemCollection"));
		refCollection.addChild(new DataAtomicSpy("linkedRecordId", linkedRecordId));
		return refCollection;
	}

	public static DataGroup createMetadataGroupWithOneChild() {
		DataGroup dataGroup = new DataGroupOldSpy("metadata");
		DataGroup recordInfo = new DataGroupOldSpy("recordInfo");
		recordInfo.addChild(new DataAtomicSpy("id", "testNewGroup"));
		recordInfo.addChild(new DataAtomicSpy("type", "metadataGroup"));
		recordInfo.addChild(createDataDividerWithLinkedRecordId("test"));

		dataGroup.addChild(recordInfo);

		dataGroup.addChild(createChildReference());

		return dataGroup;
	}

	private static DataGroup createChildReference() {
		DataGroup childReferences = new DataGroupOldSpy("childReferences");

		childReferences.addChild(createChildReference("childOne", "1", "1"));

		return childReferences;
	}

	public static DataGroup createMetadataGroupWithRecordLinkAsChild() {
		DataGroup dataGroup = new DataGroupOldSpy("metadata");
		DataGroup recordInfo = new DataGroupOldSpy("recordInfo");
		recordInfo.addChild(new DataAtomicSpy("id", "testRecordLink"));
		recordInfo.addChild(new DataAtomicSpy("type", "recordLink"));
		recordInfo.addChild(createDataDividerWithLinkedRecordId("test"));
		dataGroup.addChild(recordInfo);

		return dataGroup;
	}
}
