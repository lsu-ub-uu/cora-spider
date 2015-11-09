package se.uu.ub.cora.spider.testdata;

import se.uu.ub.cora.spider.data.SpiderDataAtomic;
import se.uu.ub.cora.spider.data.SpiderDataGroup;

public final class SpiderDataCreator {
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

	public static SpiderDataGroup createDataGroupWithNameInDataAndRecordInfoWithRecordType(
			String nameInData, String recordType) {
		SpiderDataGroup dataGroup = SpiderDataGroup.withNameInData(nameInData);
		dataGroup.addChild(createRecordInfoWithRecordType(recordType));
		return dataGroup;
	}

	private static SpiderDataGroup createRecordInfoWithRecordType(String recordType) {
		SpiderDataGroup recordInfo = SpiderDataGroup.withNameInData("recordInfo");
		recordInfo.addChild(SpiderDataAtomic.withNameInDataAndValue("type", recordType));
		return recordInfo;
	}

	public static SpiderDataGroup createRecordTypeWithIdAndUserSuppliedIdAndAbstract(String id,
			String userSuppliedId, String abstractValue) {
		return createRecordTypeWithIdAndUserSuppliedIdAndAbstractAndParentId(id, userSuppliedId,
				abstractValue, null);
	}

	private static SpiderDataGroup createRecordTypeWithIdAndUserSuppliedIdAndAbstractAndParentId(
			String id, String userSuppliedId, String abstractValue, String parentId) {
		String idWithCapitalFirst = id.substring(0, 1).toUpperCase() + id.substring(1);

		SpiderDataGroup dataGroup = SpiderDataGroup.withNameInData(RECORD_TYPE);
		dataGroup.addChild(createRecordInfoWithRecordTypeAndRecordId(RECORD_TYPE, id));

		dataGroup.addChild(SpiderDataAtomic.withNameInDataAndValue(METADATA_ID, id));
		dataGroup.addChild(SpiderDataAtomic.withNameInDataAndValue(PRESENTATION_VIEW_ID,
				"pg" + idWithCapitalFirst + "View"));
		dataGroup.addChild(SpiderDataAtomic.withNameInDataAndValue(PRESENTATION_FORM_ID,
				"pg" + idWithCapitalFirst + "Form"));
		dataGroup.addChild(SpiderDataAtomic.withNameInDataAndValue(NEW_METADATA_ID, id + "New"));
		dataGroup.addChild(SpiderDataAtomic.withNameInDataAndValue(NEW_PRESENTATION_FORM_ID,
				"pg" + idWithCapitalFirst + "FormNew"));
		dataGroup.addChild(SpiderDataAtomic.withNameInDataAndValue(LIST_PRESENTATION_VIEW_ID,
				"pg" + idWithCapitalFirst + "List"));
		dataGroup.addChild(
				SpiderDataAtomic.withNameInDataAndValue(SEARCH_METADATA_ID, id + "Search"));
		dataGroup.addChild(SpiderDataAtomic.withNameInDataAndValue(SEARCH_PRESENTATION_FORM_ID,
				"pg" + idWithCapitalFirst + "SearchForm"));

		dataGroup.addChild(
				SpiderDataAtomic.withNameInDataAndValue(USER_SUPPLIED_ID, userSuppliedId));
		dataGroup.addChild(SpiderDataAtomic.withNameInDataAndValue(PERMISSION_KEY,
				"RECORDTYPE_" + id.toUpperCase()));
		dataGroup.addChild(SpiderDataAtomic.withNameInDataAndValue(SELF_PRESENTATION_VIEW_ID,
				"pg" + idWithCapitalFirst + "Self"));
		dataGroup.addChild(SpiderDataAtomic.withNameInDataAndValue("abstract", abstractValue));
		if (null != parentId) {
			dataGroup.addChild(SpiderDataAtomic.withNameInDataAndValue("parentId", parentId));
		}
		return dataGroup;
	}

	public static SpiderDataGroup createRecordTypeWithIdAndUserSuppliedIdAndParentId(String id,
			String userSuppliedId, String parentId) {
		return createRecordTypeWithIdAndUserSuppliedIdAndAbstractAndParentId(id, userSuppliedId,
				"false", parentId);
	}

	public static SpiderDataGroup createRecordInfoWithRecordTypeAndRecordId(String recordType,
			String recordId) {
		SpiderDataGroup recordInfo = createRecordInfoWithRecordType(recordType);
		recordInfo.addChild(SpiderDataAtomic.withNameInDataAndValue("id", recordId));
		return recordInfo;
	}

}
