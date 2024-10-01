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

import java.util.List;
import java.util.Optional;

import se.uu.ub.cora.data.DataChild;
import se.uu.ub.cora.data.DataGroup;
import se.uu.ub.cora.data.DataRecordLink;
import se.uu.ub.cora.data.spies.DataGroupSpy;
import se.uu.ub.cora.data.spies.DataRecordGroupSpy;
import se.uu.ub.cora.data.spies.DataRecordLinkSpy;

public final class DataCreator2 {

	// public static DataGroup createRecordInfoWithRecordType(String recordType) {
	// DataGroup recordInfo = new DataGroupOldSpy("recordInfo");
	// recordInfo.addChild(createLinkWithLinkedId("type", "recordType", recordType));
	// return recordInfo;
	// }

	public static DataGroup createRecordInfoWithRecordTypeAndRecordIdAndDataDivider(
			String recordType, String recordId, String dataDivider) {
		// DataGroup recordInfo = createRecordInfoWithRecordType(recordType);
		// recordInfo.addChild(new DataAtomicOldSpy("id", recordId));
		// recordInfo.addChild(createDataDividerWithLinkedRecordId(dataDivider));
		// return recordInfo;
		return createRecordInfoWithIdAndTypeAndLinkedRecordId(recordId, recordType, dataDivider);
	}

	// public static DataGroup createRecordInfoWithIdAndLinkedRecordId(String id,
	// String linkedRecordId) {
	// DataGroup createRecordInfo = new DataGroupOldSpy("recordInfo");
	// DataRecordLink dataDivider = createDataDividerWithLinkedRecordId(linkedRecordId);
	// createRecordInfo.addChild(dataDivider);
	// createRecordInfo.addChild(new DataAtomicOldSpy("id", id));
	// return createRecordInfo;
	// }

	public static DataRecordGroupSpy createRecordWithNameInDataAndIdAndTypeAndLinkedRecordIdAndCreatedBy(
			String nameInData, String id, String recordType, String dataDivider, String createdBy) {
		DataRecordGroupSpy record = new DataRecordGroupSpy();
		record.MRV.setDefaultReturnValuesSupplier("getNameInData", () -> nameInData);
		record.MRV.setDefaultReturnValuesSupplier("getType", () -> recordType);
		record.MRV.setDefaultReturnValuesSupplier("getId", () -> id);
		record.MRV.setDefaultReturnValuesSupplier("getDataDivider", () -> dataDivider);
		record.MRV.setDefaultReturnValuesSupplier("getCreatedBy", () -> createdBy);
		return record;
	}

	public static DataRecordGroupSpy createRecordWithNameInDataAndIdAndTypeAndLinkedRecordId(
			String nameInData, String id, String recordType, String dataDivider) {
		return createRecordWithNameInDataAndIdAndTypeAndLinkedRecordIdAndCreatedBy(nameInData, id,
				recordType, dataDivider, "createdByUserId");
	}

	public static DataRecordGroupSpy createRecordWithNameInDataAndIdAndDataDivider(
			String nameInData, String id, String dataDivider) {
		return createRecordWithNameInDataAndIdAndTypeAndLinkedRecordId(nameInData, id, "someType",
				dataDivider);
	}

	public static DataRecordGroupSpy createRecordWithNameInDataAndLinkedDataDividerId(
			String nameInData, String dataDivider) {
		return createRecordWithNameInDataAndIdAndTypeAndLinkedRecordId(nameInData, "someId",
				"someType", dataDivider);
	}

	public static DataRecordGroupSpy createSearchWithIdAndRecordTypeToSearchIn(String id,
			String idRecordTypeToSearchIn) {
		DataRecordGroupSpy record = createRecordWithNameInDataAndLinkedDataDividerId("search", id);
		DataRecordLink recordTypeToSearchIn = createLinkWithLinkedId("recordTypeToSearchIn",
				"recordType", idRecordTypeToSearchIn);
		record.MRV.setSpecificReturnValuesSupplier("getFirstChildOfTypeAndName",
				() -> recordTypeToSearchIn, DataRecordLink.class, "recordTypeToSearchIn");
		return record;
	}

	public static DataRecordLink createLinkWithLinkedId(String nameInData, String linkedRecordType,
			String id) {
		DataRecordLinkSpy linkSpy = new DataRecordLinkSpy();
		linkSpy.MRV.setDefaultReturnValuesSupplier("getNameInData", () -> nameInData);
		linkSpy.MRV.setDefaultReturnValuesSupplier("getLinkedRecordType", () -> linkedRecordType);
		linkSpy.MRV.setDefaultReturnValuesSupplier("getLinkedRecordId", () -> id);
		return linkSpy;
	}

	// public static DataRecordGroupSpy createRecordInfoWithLinkedDataDividerId(
	// String linkedRecordId) {
	// DataRecordGroupSpy createRecordInfo = new DataGroupOldSpy("recordInfo");
	// DataRecordLink dataDivider = createDataDividerWithLinkedRecordId(linkedRecordId);
	// createRecordInfo.addChild(dataDivider);
	// return createRecordInfo;
	// }

	public static DataRecordLink createDataDividerWithLinkedRecordId(String linkedRecordId) {
		return createLinkWithLinkedId("dataDivider", "system", linkedRecordId);
	}

	public static DataGroupSpy createRecordInfoWithIdAndTypeAndLinkedRecordId(String id,
			String recordType, String linkedRecordId) {
		// DataGroupSpy recordInfo = new DataGroupOldSpy("recordInfo");
		// recordInfo.addChild(new DataAtomicOldSpy("id", id));
		// recordInfo.addChild(createLinkWithLinkedId("type", "recordType", recordType));
		//
		// DataRecordLink dataDivider = createDataDividerWithLinkedRecordId(linkedRecordId);
		// recordInfo.addChild(dataDivider);
		// return recordInfo;
		DataGroupSpy recordInfo = createGroupWithNameInData("recordInfo");
		setAtomicNameInDataUsingValueInGroup("id", id, recordInfo);

		DataRecordLink type = createLinkWithLinkedId("type", "recordType", recordType);
		attachLinkToParent(type, recordInfo);

		return recordInfo;
	}

	public static DataGroupSpy createGroupWithNameInData(String nameInData) {
		DataGroupSpy recordInfo = new DataGroupSpy();
		recordInfo.MRV.setDefaultReturnValuesSupplier("getNameInData", () -> nameInData);
		return recordInfo;
	}

	public static void attachLinkToRecord(DataRecordLink link, DataRecordGroupSpy parent) {
		parent.MRV.setSpecificReturnValuesSupplier("getFirstChildOfTypeAndName", () -> link,
				DataRecordLink.class, link.getNameInData());
		parent.MRV.setSpecificReturnValuesSupplier("containsChildWithNameInData", () -> true,
				link.getNameInData());
	}

	public static void attachLinkToParent(DataRecordLink link, DataGroupSpy parent) {
		parent.MRV.setSpecificReturnValuesSupplier("getFirstChildOfTypeAndName", () -> link,
				DataRecordLink.class, link.getNameInData());
		parent.MRV.setSpecificReturnValuesSupplier("containsChildWithNameInData", () -> true,
				link.getNameInData());
	}

	// public static DataRecordGroupSpy createWorkOrderWithIdAndRecordTypeAndRecordIdToIndex(String
	// id,
	// String recordType, String recordId) {
	// String workOrderType = "index";
	// return createWorkOrderWithIdRecordTypeRecordIdAndWorkOrderType(id, recordType, recordId,
	// workOrderType);
	// }
	//
	// public static DataRecordGroupSpy createWorkOrderWithIdRecordTypeRecordIdAndWorkOrderType(
	// String id, String recordType, String recordId, String workOrderType) {
	// DataRecordGroupSpy workOrder = new DataGroupOldSpy("workOrder");
	// DataRecordGroupSpy recordInfo = new DataGroupOldSpy("recordInfo");
	// recordInfo.addChild(new DataAtomicOldSpy("id", id));
	// workOrder.addChild(recordInfo);
	//
	// DataRecordLinkSpy recordTypeLink = new DataRecordLinkSpy();
	// recordTypeLink.MRV.setDefaultReturnValuesSupplier("getNameInData",
	// (Supplier<String>) () -> "recordType");
	// recordTypeLink.MRV.setDefaultReturnValuesSupplier("getLinkedRecordId",
	// (Supplier<String>) () -> recordType);
	// workOrder.addChild(recordTypeLink);
	//
	// workOrder.addChild(new DataAtomicOldSpy("recordId", recordId));
	// workOrder.addChild(new DataAtomicOldSpy("type", workOrderType));
	// return workOrder;
	// }

	public static DataRecordGroupSpy createMetadataGroupWithTwoChildren() {
		DataRecordGroupSpy recordGroup = createRecordWithNameInDataAndIdAndDataDivider("metadata",
				"testNewGroup", "test");
		recordGroup.MRV.setSpecificReturnValuesSupplier("getAttributeValue",
				() -> Optional.of("group"), "type");

		List<DataChild> refs = List.of(createChildReference("childOne", "1", "1"),
				createChildReference("childTwo", "0", "2"));
		DataGroupSpy childReferences = createChildReferences(refs);

		recordGroup.MRV.setSpecificReturnValuesSupplier("getFirstChildWithNameInData",
				() -> childReferences, "childReferences");

		return recordGroup;
	}

	public static DataGroupSpy createChildReferences(List<DataChild> children) {
		DataGroupSpy childReferences = createGroupWithNameInData("childReferences");
		childReferences.MRV.setDefaultReturnValuesSupplier("getChildren", () -> children);
		return childReferences;
	}

	public static DataGroupSpy createChildReference(String ref, String repeatMin,
			String repeatMax) {
		DataGroupSpy childReference = createGroupWithNameInData("childReference");

		DataRecordLink refLink = createLinkWithLinkedId("ref", "metadata", ref);
		attachLinkToParent(refLink, childReference);

		setAtomicNameInDataUsingValueInGroup("repeatMin", repeatMin, childReference);
		setAtomicNameInDataUsingValueInGroup("repeatMax", repeatMax, childReference);
		return childReference;
	}

	public static void setAtomicNameInDataUsingValueInGroup(String nameInData, String value,
			DataGroupSpy group) {
		group.MRV.setSpecificReturnValuesSupplier("getFirstAtomicValueWithNameInData", () -> value,
				nameInData);
	}

	public static void setAtomicNameInDataUsingValueInRecord(String nameInData, String value,
			DataRecordGroupSpy record) {
		record.MRV.setSpecificReturnValuesSupplier("getFirstAtomicValueWithNameInData", () -> value,
				nameInData);
		record.MRV.setSpecificReturnValuesSupplier("containsChildWithNameInData", () -> true,
				nameInData);
	}

	public static DataRecordGroupSpy createMetadataGroupWithThreeChildren() {
		// DataRecordGroupSpy dataGroup = createMetadataGroupWithTwoChildren();
		// DataRecordGroupSpy childReferences = dataGroup
		// .getFirstGroupWithNameInData("childReferences");
		// childReferences.addChild(createChildReference("childThree", "1", "1"));
		// return dataGroup;

		DataRecordGroupSpy recordGroup = createRecordWithNameInDataAndIdAndDataDivider("metadata",
				"testNewGroup", "test");
		recordGroup.MRV.setSpecificReturnValuesSupplier("getAttributeValue",
				() -> Optional.of("group"), "type");

		List<DataChild> refs = List.of(createChildReference("childOne", "1", "1"),
				createChildReference("childTwo", "0", "2"),
				createChildReference("childThree", "1", "1"));
		DataGroupSpy childReferences = createChildReferences(refs);

		recordGroup.MRV.setSpecificReturnValuesSupplier("getFirstChildWithNameInData",
				() -> childReferences, "childReferences");

		return recordGroup;
	}

	public static DataRecordGroupSpy createDataGroupDescribingACollectionVariable() {
		// DataRecordGroupSpy dataGroup = new DataGroupOldSpy("metadata");
		// dataGroup.addAttributeByIdWithValue("type", "collectionVariable");
		// DataRecordGroupSpy recordInfo = new DataGroupOldSpy("recordInfo");
		// recordInfo.addChild(new DataAtomicOldSpy("id", "testCollectionVar"));
		// recordInfo.addChild(createDataDividerWithLinkedRecordId("test"));
		// dataGroup.addChild(recordInfo);
		// dataGroup.addChild(
		// createLinkWithLinkedId("refCollection", "metadata", "testItemCollection"));
		// return dataGroup;
		DataRecordGroupSpy record = createRecordWithNameInDataAndIdAndDataDivider("metadata",
				"testCollectionVar", "test");
		record.MRV.setSpecificReturnValuesSupplier("getAttributeValue",
				() -> Optional.of("collectionVariable"), "type");
		DataRecordLink refColl = createLinkWithLinkedId("refCollection", "metadata",
				"testItemCollection");
		attachLinkToRecord(refColl, record);
		return record;
	}

	public static DataRecordGroupSpy createMetadataGroupWithOneChild() {
		// DataRecordGroupSpy dataGroup = new DataGroupOldSpy("metadata");
		// dataGroup.addAttributeByIdWithValue("type", "group");
		// DataRecordGroupSpy recordInfo = new DataGroupOldSpy("recordInfo");
		// recordInfo.addChild(new DataAtomicOldSpy("id", "testNewGroup"));
		// recordInfo.addChild(createDataDividerWithLinkedRecordId("test"));
		// dataGroup.addChild(recordInfo);
		// dataGroup.addChild(createChildReference());
		// return dataGroup;
		DataRecordGroupSpy record = createRecordWithNameInDataAndIdAndDataDivider("metadata",
				"testNewGroup", "test");
		record.MRV.setSpecificReturnValuesSupplier("getAttributeValue", () -> Optional.of("group"),
				"type");

		List<DataChild> refs = List.of(createChildReference("childOne", "1", "1"));
		DataGroupSpy childReferences = createChildReferences(refs);
		record.MRV.setSpecificReturnValuesSupplier("getFirstChildWithNameInData",
				() -> childReferences, "childReferences");
		return record;
	}

	// private static DataRecordGroupSpy createChildReference() {
	// DataRecordGroupSpy childReferences = new DataGroupOldSpy("childReferences");
	// childReferences.addChild(createChildReference("childOne", "1", "1"));
	// return childReferences;
	// }

	public static DataRecordGroupSpy createMetadataGroupWithRecordLinkAsChild() {
		// DataRecordGroupSpy dataGroup = new DataGroupOldSpy("metadata");
		// dataGroup.addAttributeByIdWithValue("type", "recordLink");
		// DataRecordGroupSpy recordInfo = new DataGroupOldSpy("recordInfo");
		// recordInfo.addChild(new DataAtomicOldSpy("id", "testRecordLink"));
		// recordInfo.addChild(new DataAtomicOldSpy("type", "recordLink"));
		// recordInfo.addChild(createDataDividerWithLinkedRecordId("test"));
		// dataGroup.addChild(recordInfo);
		// return dataGroup;

		DataRecordGroupSpy record = createRecordWithNameInDataAndIdAndDataDivider("metadata",
				"testRecordLink", "test");
		record.MRV.setSpecificReturnValuesSupplier("getAttributeValue",
				() -> Optional.of("recordLink"), "type");

		List<DataChild> refs = List.of(createChildReference("childOne", "1", "1"));
		DataGroupSpy childReferences = createChildReferences(refs);
		record.MRV.setSpecificReturnValuesSupplier("getFirstChildWithNameInData",
				() -> childReferences, "childReferences");
		return record;
	}
}
