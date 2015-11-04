package se.uu.ub.cora.spider.testdata;

import se.uu.ub.cora.bookkeeper.data.DataAtomic;
import se.uu.ub.cora.bookkeeper.data.DataGroup;

public final class DataCreator {
	private static final String SELF_PRESENTATION_VIEW_ID = "selfPresentationViewId";
	private static final String PERMISSION_KEY = "permissionKey";
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

		dataGroup.addChild(DataAtomic.withNameInDataAndValue(METADATA_ID, id));
		dataGroup.addChild(DataAtomic.withNameInDataAndValue(PRESENTATION_VIEW_ID,
				"pg" + idWithCapitalFirst + "View"));
		dataGroup.addChild(DataAtomic.withNameInDataAndValue(PRESENTATION_FORM_ID,
				"pg" + idWithCapitalFirst + "Form"));
		dataGroup.addChild(DataAtomic.withNameInDataAndValue(NEW_METADATA_ID, id + "New"));
		dataGroup.addChild(DataAtomic.withNameInDataAndValue(NEW_PRESENTATION_FORM_ID,
				"pg" + idWithCapitalFirst + "FormNew"));
		dataGroup.addChild(DataAtomic.withNameInDataAndValue(LIST_PRESENTATION_VIEW_ID,
				"pg" + idWithCapitalFirst + "List"));
		dataGroup.addChild(DataAtomic.withNameInDataAndValue(SEARCH_METADATA_ID, id + "Search"));
		dataGroup.addChild(DataAtomic.withNameInDataAndValue(SEARCH_PRESENTATION_FORM_ID,
				"pg" + idWithCapitalFirst + "SearchForm"));

		dataGroup.addChild(DataAtomic.withNameInDataAndValue(USER_SUPPLIED_ID, userSuppliedId));
		dataGroup.addChild(DataAtomic.withNameInDataAndValue(PERMISSION_KEY,
				"RECORDTYPE_" + id.toUpperCase()));
		dataGroup.addChild(DataAtomic.withNameInDataAndValue(SELF_PRESENTATION_VIEW_ID,
				"pg" + idWithCapitalFirst + "Self"));
		dataGroup.addChild(DataAtomic.withNameInDataAndValue("abstract", abstractValue));
		if (null != parentId) {
			dataGroup.addChild(DataAtomic.withNameInDataAndValue("parentId", parentId));
		}
		return dataGroup;
	}

	public static DataGroup createRecordTypeWithIdAndUserSuppliedIdAndParentId(String id,
			String userSuppliedId, String parentId) {
		return createRecordTypeWithIdAndUserSuppliedIdAndAbstractAndParentId(id, userSuppliedId,
				"false", parentId);
	}

	public static DataGroup createRecordInfoWithRecordTypeAndRecordId(String recordType,
			String recordId) {
		DataGroup recordInfo = DataGroup.withNameInData("recordInfo");
		recordInfo.addChild(DataAtomic.withNameInDataAndValue("type", recordType));
		recordInfo.addChild(DataAtomic.withNameInDataAndValue("id", recordId));
		return recordInfo;
	}
}
