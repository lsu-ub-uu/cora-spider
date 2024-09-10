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
import se.uu.ub.cora.spider.data.DataAtomicOldSpy;
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
		recordInfo.addChild(new DataAtomicOldSpy("id", recordId));
		recordInfo.addChild(createDataDividerWithLinkedRecordId(dataDivider));
		return recordInfo;
	}

	public static DataGroup createRecordInfoWithIdAndLinkedRecordId(String id,
			String linkedRecordId) {
		DataGroup createRecordInfo = new DataGroupOldSpy("recordInfo");
		DataRecordLink dataDivider = createDataDividerWithLinkedRecordId(linkedRecordId);
		createRecordInfo.addChild(dataDivider);
		createRecordInfo.addChild(new DataAtomicOldSpy("id", id));
		return createRecordInfo;
	}

	public static DataRecordLink createLinkWithLinkedId(String nameInData, String linkedRecordType,
			String id) {
		DataRecordLinkSpy linkSpy = new DataRecordLinkSpy();
		linkSpy.MRV.setDefaultReturnValuesSupplier("getNameInData", () -> nameInData);
		linkSpy.MRV.setDefaultReturnValuesSupplier("getLinkedRecordType", () -> linkedRecordType);
		linkSpy.MRV.setDefaultReturnValuesSupplier("getLinkedRecordId", () -> id);
		return linkSpy;
	}

	public static DataGroup createRecordWithNameInDataAndIdAndLinkedRecordId(String nameInData,
			String id, String linkedRecordId) {
		DataGroup record = new DataGroupOldSpy(nameInData);
		DataGroup createRecordInfo = createRecordInfoWithIdAndLinkedRecordId(id, linkedRecordId);
		record.addChild(createRecordInfo);
		return record;
	}

	public static DataGroup createRecordWithNameInDataAndLinkedDataDividerId(String nameInData,
			String linkedRecordId) {
		DataGroup record = new DataGroupOldSpy(nameInData);
		DataGroup createRecordInfo = createRecordInfoWithLinkedDataDividerId(linkedRecordId);
		record.addChild(createRecordInfo);
		return record;
	}

	public static DataGroup createRecordInfoWithLinkedDataDividerId(String linkedRecordId) {
		DataGroup createRecordInfo = new DataGroupOldSpy("recordInfo");
		DataRecordLink dataDivider = createDataDividerWithLinkedRecordId(linkedRecordId);
		createRecordInfo.addChild(dataDivider);
		return createRecordInfo;
	}

	public static DataRecordLink createDataDividerWithLinkedRecordId(String linkedRecordId) {
		DataRecordLinkSpy linkSpy = new DataRecordLinkSpy();
		linkSpy.MRV.setDefaultReturnValuesSupplier("getNameInData", () -> "dataDivider");
		linkSpy.MRV.setDefaultReturnValuesSupplier("getLinkedRecordType", () -> "system");
		linkSpy.MRV.setDefaultReturnValuesSupplier("getLinkedRecordId", () -> linkedRecordId);
		return linkSpy;
	}

	public static DataGroup createSearchWithIdAndRecordTypeToSearchIn(String id,
			String idRecordTypeToSearchIn) {
		DataGroup search = new DataGroupOldSpy("search");
		DataGroup recordInfo = DataCreator2.createRecordInfoWithRecordType("search");
		recordInfo.addChild(new DataAtomicOldSpy("id", id));
		search.addChild(recordInfo);

		DataGroup recordTypeToSearchIn = new DataGroupOldSpy("recordTypeToSearchIn");
		recordTypeToSearchIn.addChild(new DataAtomicOldSpy("linkedRecordType", "recordType"));
		recordTypeToSearchIn
				.addChild(new DataAtomicOldSpy("linkedRecordId", idRecordTypeToSearchIn));
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
		createRecordInfo.addChild(new DataAtomicOldSpy("id", id));
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
		recordInfo.addChild(new DataAtomicOldSpy("id", id));
		workOrder.addChild(recordInfo);

		DataRecordLinkSpy recordTypeLink = new DataRecordLinkSpy();
		recordTypeLink.MRV.setDefaultReturnValuesSupplier("getNameInData",
				(Supplier<String>) () -> "recordType");
		recordTypeLink.MRV.setDefaultReturnValuesSupplier("getLinkedRecordId",
				(Supplier<String>) () -> recordType);
		workOrder.addChild(recordTypeLink);

		workOrder.addChild(new DataAtomicOldSpy("recordId", recordId));
		workOrder.addChild(new DataAtomicOldSpy("type", workOrderType));
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
		createdByGroup.addChild(new DataAtomicOldSpy("linkedRecordType", "user"));
		createdByGroup.addChild(new DataAtomicOldSpy("linkedRecordId", createdBy));
		createRecordInfo.addChild(createdByGroup);
		return record;
	}

	public static DataGroup createMetadataGroupWithTwoChildren() {
		DataGroup dataGroup = new DataGroupOldSpy("metadata");
		dataGroup.addAttributeByIdWithValue("type", "group");
		DataGroup recordInfo = new DataGroupOldSpy("recordInfo");
		recordInfo.addChild(new DataAtomicOldSpy("id", "testNewGroup"));
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
		childReference.addChild(createLinkWithLinkedId("ref", "metadata", ref));
		DataAtomic repeatMinAtomic = new DataAtomicOldSpy("repeatMin", repeatMin);
		childReference.addChild(repeatMinAtomic);
		DataAtomic repeatMaxAtomic = new DataAtomicOldSpy("repeatMax", repeatMax);
		childReference.addChild(repeatMaxAtomic);
		return childReference;
	}

	public static DataGroup createMetadataGroupWithThreeChildren() {
		DataGroup dataGroup = createMetadataGroupWithTwoChildren();
		DataGroup childReferences = dataGroup.getFirstGroupWithNameInData("childReferences");
		childReferences.addChild(createChildReference("childThree", "1", "1"));
		return dataGroup;
	}

	public static DataGroup createDataGroupDescribingACollectionVariable() {
		DataGroup dataGroup = new DataGroupOldSpy("metadata");
		dataGroup.addAttributeByIdWithValue("type", "collectionVariable");
		DataGroup recordInfo = new DataGroupOldSpy("recordInfo");
		recordInfo.addChild(new DataAtomicOldSpy("id", "testCollectionVar"));
		recordInfo.addChild(createDataDividerWithLinkedRecordId("test"));
		dataGroup.addChild(recordInfo);
		dataGroup.addChild(
				createLinkWithLinkedId("refCollection", "metadata", "testItemCollection"));
		return dataGroup;
	}

	public static DataGroup createMetadataGroupWithOneChild() {
		DataGroup dataGroup = new DataGroupOldSpy("metadata");
		dataGroup.addAttributeByIdWithValue("type", "group");
		DataGroup recordInfo = new DataGroupOldSpy("recordInfo");
		recordInfo.addChild(new DataAtomicOldSpy("id", "testNewGroup"));
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
		dataGroup.addAttributeByIdWithValue("type", "recordLink");
		DataGroup recordInfo = new DataGroupOldSpy("recordInfo");
		recordInfo.addChild(new DataAtomicOldSpy("id", "testRecordLink"));
		recordInfo.addChild(new DataAtomicOldSpy("type", "recordLink"));
		recordInfo.addChild(createDataDividerWithLinkedRecordId("test"));
		dataGroup.addChild(recordInfo);
		return dataGroup;
	}
}
