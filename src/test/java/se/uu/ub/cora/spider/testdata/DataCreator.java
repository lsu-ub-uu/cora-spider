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

import se.uu.ub.cora.bookkeeper.data.DataAtomic;
import se.uu.ub.cora.bookkeeper.data.DataGroup;
import se.uu.ub.cora.spider.data.SpiderDataAtomic;
import se.uu.ub.cora.spider.data.SpiderDataGroup;
import se.uu.ub.cora.spider.spy.DataRecordLinkCollectorSpy;

public final class DataCreator {
	private static final String SELF_PRESENTATION_VIEW_ID = "selfPresentationViewId";
	private static final String USER_SUPPLIED_ID = "userSuppliedId";
	private static final String SEARCH_PRESENTATION_FORM_ID = "searchPresentationFormId";
	private static final String SEARCH_METADATA_ID = "searchMetadataId";
	private static final String LIST_PRESENTATION_VIEW_ID = "listPresentationViewId";
	private static final String NEW_PRESENTATION_FORM_ID = "newPresentationFormId";
	private static final String PRESENTATION_FORM_ID = "presentationFormId";
	private static final String PRESENTATION_VIEW_ID = "presentationViewId";
	private static final String METADATA_ID = "metadataId";
	private static final String NEW_METADATA_ID = "newMetadataId";
	private static final String RECORD_TYPE = "recordType";

	public static DataGroup createRecordTypeWithIdAndUserSuppliedIdAndAbstract(String id,
			String userSuppliedId, String abstractValue) {
		return createRecordTypeWithIdAndUserSuppliedIdAndAbstractAndParentId(id, userSuppliedId,
				abstractValue, null);
	}

	private static DataGroup createRecordTypeWithIdAndUserSuppliedIdAndAbstractAndParentId(
			String id, String userSuppliedId, String abstractValue, String parentId) {
		String idWithCapitalFirst = id.substring(0, 1).toUpperCase() + id.substring(1);

		DataGroup dataGroup = DataGroup.withNameInData(RECORD_TYPE);
		dataGroup.addChild(createRecordInfoWithRecordTypeAndRecordId(RECORD_TYPE, id));

		dataGroup.addChild(
				createChildWithNamInDataLinkedTypeLinkedId(METADATA_ID, "metadataGroup", id));

		dataGroup.addChild(createChildWithNamInDataLinkedTypeLinkedId(PRESENTATION_VIEW_ID,
				"presentationGroup", "pg" + idWithCapitalFirst + "View"));

		dataGroup.addChild(createChildWithNamInDataLinkedTypeLinkedId(PRESENTATION_FORM_ID,
				"presentationGroup", "pg" + idWithCapitalFirst + "Form"));
		dataGroup.addChild(createChildWithNamInDataLinkedTypeLinkedId(NEW_METADATA_ID,
				"metadataGroup", id + "New"));

		dataGroup.addChild(createChildWithNamInDataLinkedTypeLinkedId(NEW_PRESENTATION_FORM_ID,
				"presentationGroup", "pg" + idWithCapitalFirst + "FormNew"));
		dataGroup.addChild(createChildWithNamInDataLinkedTypeLinkedId(LIST_PRESENTATION_VIEW_ID,
				"presentationGroup", "pg" + idWithCapitalFirst + "List"));
		dataGroup.addChild(DataAtomic.withNameInDataAndValue(SEARCH_METADATA_ID, id + "Search"));
		dataGroup.addChild(DataAtomic.withNameInDataAndValue(SEARCH_PRESENTATION_FORM_ID,
				"pg" + idWithCapitalFirst + "SearchForm"));

		dataGroup.addChild(DataAtomic.withNameInDataAndValue(USER_SUPPLIED_ID, userSuppliedId));
		dataGroup.addChild(DataAtomic.withNameInDataAndValue(SELF_PRESENTATION_VIEW_ID,
				"pg" + idWithCapitalFirst + "Self"));
		dataGroup.addChild(DataAtomic.withNameInDataAndValue("abstract", abstractValue));
		if (null != parentId) {
			dataGroup.addChild(
					createChildWithNamInDataLinkedTypeLinkedId("parentId", "recordType", parentId));
		}
		return dataGroup;
	}

	public static DataGroup createChildWithNamInDataLinkedTypeLinkedId(String nameInData,
			String linkedRecordType, String id) {
		DataGroup metadataId = DataGroup.withNameInData(nameInData);
		metadataId
				.addChild(DataAtomic.withNameInDataAndValue("linkedRecordType", linkedRecordType));
		metadataId.addChild(DataAtomic.withNameInDataAndValue("linkedRecordId", id));
		return metadataId;
	}

	public static DataGroup createRecordTypeWithIdAndUserSuppliedIdAndParentId(String id,
			String userSuppliedId, String parentId) {
		return createRecordTypeWithIdAndUserSuppliedIdAndAbstractAndParentId(id, userSuppliedId,
				"false", parentId);
	}

	public static DataGroup createRecordInfoWithRecordTypeAndRecordId(String recordType,
			String recordId) {
		DataGroup recordInfo = DataGroup.withNameInData("recordInfo");
		DataGroup typeGroup = DataGroup.withNameInData("type");
		typeGroup.addChild(DataAtomic.withNameInDataAndValue("linkedRecordType", "recordType"));
		typeGroup.addChild(DataAtomic.withNameInDataAndValue("linkedRecordId", recordType));
		recordInfo.addChild(typeGroup);
		recordInfo.addChild(DataAtomic.withNameInDataAndValue("id", recordId));
		return recordInfo;
	}

	public static SpiderDataGroup createRecordWithNameInDataAndIdAndLinkedRecordId(
			String nameInData, String id, String linkedRecordId) {
		SpiderDataGroup record = SpiderDataGroup.withNameInData(nameInData);
		SpiderDataGroup createRecordInfo = createRecordInfoWithIdAndLinkedRecordId(id,
				linkedRecordId);
		record.addChild(createRecordInfo);
		return record;
	}

	public static SpiderDataGroup createRecordWithNameInDataAndIdAndTypeAndLinkedRecordIdAndCreatedBy(
			String nameInData, String id, String recordType, String linkedRecordId,
			String createdBy) {
		SpiderDataGroup record = SpiderDataGroup.withNameInData(nameInData);
		SpiderDataGroup createRecordInfo = createRecordInfoWithIdAndTypeAndLinkedRecordId(id,
				recordType, linkedRecordId);
		record.addChild(createRecordInfo);
		SpiderDataGroup createdByGroup = SpiderDataGroup.withNameInData("createdBy");
		createdByGroup
				.addChild(SpiderDataAtomic.withNameInDataAndValue("linkedRecordType", "user"));
		createdByGroup
				.addChild(SpiderDataAtomic.withNameInDataAndValue("linkedRecordId", createdBy));
		createRecordInfo.addChild(createdByGroup);
		return record;
	}

	public static SpiderDataGroup createRecordWithNameInDataAndIdAndTypeAndLinkedRecordId(
			String nameInData, String id, String recordType, String linkedRecordId) {
		SpiderDataGroup record = SpiderDataGroup.withNameInData(nameInData);
		SpiderDataGroup createRecordInfo = createRecordInfoWithIdAndTypeAndLinkedRecordId(id,
				recordType, linkedRecordId);
		record.addChild(createRecordInfo);
		return record;
	}

	public static SpiderDataGroup createRecordInfoWithLinkedRecordId(String linkedRecordId) {
		SpiderDataGroup createRecordInfo = SpiderDataGroup.withNameInData("recordInfo");
		SpiderDataGroup dataDivider = createDataDividerWithLinkedRecordId(linkedRecordId);
		createRecordInfo.addChild(dataDivider);
		return createRecordInfo;
	}

	public static SpiderDataGroup createDataDividerWithLinkedRecordId(String linkedRecordId) {
		SpiderDataGroup dataDivider = SpiderDataGroup.withNameInData("dataDivider");
		dataDivider.addChild(SpiderDataAtomic.withNameInDataAndValue("linkedRecordType", "system"));
		dataDivider.addChild(
				SpiderDataAtomic.withNameInDataAndValue("linkedRecordId", linkedRecordId));
		return dataDivider;
	}

	public static SpiderDataGroup createRecordWithNameInDataAndLinkedRecordId(String nameInData,
			String linkedRecordId) {
		SpiderDataGroup record = SpiderDataGroup.withNameInData(nameInData);
		SpiderDataGroup createRecordInfo = createRecordInfoWithLinkedRecordId(linkedRecordId);
		record.addChild(createRecordInfo);
		return record;
	}

	public static SpiderDataGroup createRecordInfoWithIdAndLinkedRecordId(String id,
			String linkedRecordId) {
		SpiderDataGroup createRecordInfo = SpiderDataGroup.withNameInData("recordInfo");
		createRecordInfo.addChild(SpiderDataAtomic.withNameInDataAndValue("id", id));
		SpiderDataGroup dataDivider = createDataDividerWithLinkedRecordId(linkedRecordId);
		createRecordInfo.addChild(dataDivider);
		return createRecordInfo;
	}

	public static SpiderDataGroup createMetadataGroupWithOneChild() {
		SpiderDataGroup spiderDataGroup = SpiderDataGroup.withNameInData("metadata");
		SpiderDataGroup recordInfo = SpiderDataGroup.withNameInData("recordInfo");
		recordInfo.addChild(SpiderDataAtomic.withNameInDataAndValue("id", "testNewGroup"));
		recordInfo.addChild(SpiderDataAtomic.withNameInDataAndValue("type", "metadataGroup"));
		recordInfo.addChild(createDataDividerWithLinkedRecordId("test"));

		spiderDataGroup.addChild(recordInfo);

		spiderDataGroup.addChild(createChildReference());

		return spiderDataGroup;
	}

	public static SpiderDataGroup createSomeDataGroupWithOneChild() {
		SpiderDataGroup spiderDataGroup = SpiderDataGroup.withNameInData("someData");
		SpiderDataGroup recordInfo = SpiderDataGroup.withNameInData("recordInfo");
		recordInfo.addChild(SpiderDataAtomic.withNameInDataAndValue("id", "testNewGroup"));
		recordInfo.addChild(SpiderDataAtomic.withNameInDataAndValue("type", "someData"));
		recordInfo.addChild(createDataDividerWithLinkedRecordId("test"));

		spiderDataGroup.addChild(recordInfo);

		spiderDataGroup.addChild(createChildReference());

		return spiderDataGroup;
	}

	private static SpiderDataGroup createChildReference() {
		SpiderDataGroup childReferences = SpiderDataGroup.withNameInData("childReferences");

		childReferences.addChild(createChildReference("childOne", "1", "1"));

		return childReferences;
	}

	public static SpiderDataGroup createRecordInfoWithIdAndTypeAndLinkedRecordId(String id,
			String recordType, String linkedRecordId) {
		SpiderDataGroup createRecordInfo = SpiderDataGroup.withNameInData("recordInfo");
		createRecordInfo.addChild(SpiderDataAtomic.withNameInDataAndValue("id", id));
		SpiderDataGroup typeGroup = SpiderDataGroup.withNameInData("type");
		typeGroup.addChild(
				SpiderDataAtomic.withNameInDataAndValue("linkedRecordType", "recordType"));
		typeGroup.addChild(SpiderDataAtomic.withNameInDataAndValue("linkedRecordId", recordType));
		createRecordInfo.addChild(typeGroup);
		// createRecordInfo.addChild(SpiderDataAtomic.withNameInDataAndValue("type",
		// recordType));

		SpiderDataGroup dataDivider = createDataDividerWithLinkedRecordId(linkedRecordId);
		createRecordInfo.addChild(dataDivider);
		return createRecordInfo;
	}

	public static SpiderDataGroup createMetadataGroupWithTwoChildren() {
		SpiderDataGroup spiderDataGroup = SpiderDataGroup.withNameInData("metadata");
		SpiderDataGroup recordInfo = SpiderDataGroup.withNameInData("recordInfo");
		recordInfo.addChild(SpiderDataAtomic.withNameInDataAndValue("id", "testNewGroup"));
		recordInfo.addChild(SpiderDataAtomic.withNameInDataAndValue("type", "metadataGroup"));
		recordInfo.addChild(createDataDividerWithLinkedRecordId("test"));

		spiderDataGroup.addChild(recordInfo);

		spiderDataGroup.addChild(createChildReferences());

		return spiderDataGroup;
	}

	private static SpiderDataGroup createChildReferences() {
		SpiderDataGroup childReferences = SpiderDataGroup.withNameInData("childReferences");

		childReferences.addChild(createChildReference("childOne", "1", "1"));
		childReferences.addChild(createChildReference("childTwo", "0", "2"));

		return childReferences;
	}

	private static SpiderDataGroup createChildReference(String ref, String repeatMin,
			String repeatMax) {
		SpiderDataGroup childReference = SpiderDataGroup.withNameInData("childReference");

		SpiderDataGroup refGroup = SpiderDataGroup.withNameInData("ref");
		SpiderDataAtomic linkedRecordType = SpiderDataAtomic
				.withNameInDataAndValue("linkedRecordType", "metadataGroup");
		refGroup.addChild(linkedRecordType);
		SpiderDataAtomic linkedRecordId = SpiderDataAtomic.withNameInDataAndValue("linkedRecordId",
				ref);
		refGroup.addChild(linkedRecordId);

		refGroup.addAttributeByIdWithValue("type", "group");
		childReference.addChild(refGroup);

		SpiderDataAtomic repeatMinAtomic = SpiderDataAtomic.withNameInDataAndValue("repeatMin",
				repeatMin);
		childReference.addChild(repeatMinAtomic);

		SpiderDataAtomic repeatMaxAtomic = SpiderDataAtomic.withNameInDataAndValue("repeatMax",
				repeatMax);
		childReference.addChild(repeatMaxAtomic);

		return childReference;
	}

	public static SpiderDataGroup createMetadataGroupWithThreeChildren() {
		SpiderDataGroup spiderDataGroup = createMetadataGroupWithTwoChildren();
		SpiderDataGroup childReferences = (SpiderDataGroup) spiderDataGroup
				.getFirstChildWithNameInData("childReferences");
		childReferences.addChild(createChildReference("childThree", "1", "1"));

		return spiderDataGroup;
	}

	public static DataRecordLinkCollectorSpy getDataRecordLinkCollectorSpyWithCollectedLinkAdded() {
		DataGroup recordToRecordLink = createDataForRecordToRecordLink();

		DataRecordLinkCollectorSpy linkCollector = new DataRecordLinkCollectorSpy();
		linkCollector.collectedDataLinks.addChild(recordToRecordLink);
		return linkCollector;
	}

	public static DataGroup createDataForRecordToRecordLink() {
		DataGroup recordToRecordLink = DataGroup.withNameInData("recordToRecordLink");

		DataGroup from = DataGroup.withNameInData("from");
		from.addChild(DataAtomic.withNameInDataAndValue("linkedRecordType", "dataWithLinks"));
		from.addChild(DataAtomic.withNameInDataAndValue("linkedRecordId", "someId"));

		recordToRecordLink.addChild(from);

		DataGroup to = DataGroup.withNameInData("to");
		to.addChild(DataAtomic.withNameInDataAndValue("linkedRecordType", "toRecordType"));
		to.addChild(DataAtomic.withNameInDataAndValue("linkedRecordId", "toRecordId"));
		to.addChild(to);

		recordToRecordLink.addChild(to);
		return recordToRecordLink;
	}

	public static SpiderDataGroup createMetadataGroupWithCollectionVariableAsChild() {
		SpiderDataGroup spiderDataGroup = SpiderDataGroup.withNameInData("metadata");
		SpiderDataGroup recordInfo = SpiderDataGroup.withNameInData("recordInfo");
		recordInfo.addChild(SpiderDataAtomic.withNameInDataAndValue("id", "testCollectionVar"));
		recordInfo.addChild(SpiderDataAtomic.withNameInDataAndValue("type", "collectionVariable"));
		recordInfo.addChild(createDataDividerWithLinkedRecordId("test"));
		spiderDataGroup.addChild(recordInfo);
		spiderDataGroup.addChild(createRefCollectionIdWithLinkedRecordid("testItemCollection"));

		return spiderDataGroup;
	}

	public static SpiderDataGroup createRefCollectionIdWithLinkedRecordid(String linkedRecordId) {
		SpiderDataGroup refCollection = SpiderDataGroup.withNameInData("refCollection");
		refCollection.addChild(SpiderDataAtomic.withNameInDataAndValue("linkedRecordType",
				"metadataItemCollection"));
		refCollection.addChild(
				SpiderDataAtomic.withNameInDataAndValue("linkedRecordId", linkedRecordId));
		return refCollection;
	}

	public static SpiderDataGroup createMetadataGroupWithRecordLinkAsChild() {
		SpiderDataGroup spiderDataGroup = SpiderDataGroup.withNameInData("metadata");
		SpiderDataGroup recordInfo = SpiderDataGroup.withNameInData("recordInfo");
		recordInfo.addChild(SpiderDataAtomic.withNameInDataAndValue("id", "testRecordLink"));
		recordInfo.addChild(SpiderDataAtomic.withNameInDataAndValue("type", "recordLink"));
		recordInfo.addChild(createDataDividerWithLinkedRecordId("test"));
		spiderDataGroup.addChild(recordInfo);

		return spiderDataGroup;
	}

	public static DataGroup createDataGroupWithNameInDataTypeAndId(String nameInData,
			String recordType, String recordId) {
		DataGroup dataGroup = DataGroup.withNameInData(nameInData);
		DataGroup recordInfo = DataCreator.createRecordInfoWithRecordTypeAndRecordId(recordType,
				recordId);
		dataGroup.addChild(recordInfo);
		return dataGroup;
	}
}
