package se.uu.ub.cora.spider.record;

import java.util.ArrayList;
import java.util.Collection;

import se.uu.ub.cora.bookkeeper.data.DataAtomic;
import se.uu.ub.cora.bookkeeper.data.DataGroup;
import se.uu.ub.cora.spider.record.storage.RecordStorage;

public class RecordStorageSpy implements RecordStorage {

	public Collection<String> readLists = new ArrayList<>();
	public boolean deleteWasCalled = false;
	public boolean createWasCalled = false;

	@Override
	public DataGroup read(String type, String id) {

		if ("abstract".equals(id)) {
			String recordType = "recordType";
			DataGroup dataGroup = DataGroup.withNameInData(recordType);

			DataGroup recordInfo = DataGroup.withNameInData("recordInfo");
			recordInfo.addChild(DataAtomic.withNameInDataAndValue("id", "abstract"));
			recordInfo.addChild(DataAtomic.withNameInDataAndValue("type", recordType));
			dataGroup.addChild(recordInfo);

			dataGroup.addChild(DataAtomic.withNameInDataAndValue("metadataId", "abstract"));
			dataGroup.addChild(DataAtomic.withNameInDataAndValue("presentationViewId",
					"presentation:pgAbstractView"));
			dataGroup.addChild(DataAtomic.withNameInDataAndValue("presentationFormId",
					"presentation:pgAbstractForm"));
			dataGroup.addChild(
					DataAtomic.withNameInDataAndValue("newMetadataId", "metadata:abstractNew"));
			dataGroup.addChild(DataAtomic.withNameInDataAndValue("newPresentationFormId",
					"presentation:pgAbstractFormNew"));
			dataGroup.addChild(DataAtomic.withNameInDataAndValue("listPresentationViewId",
					"presentation:pgAbstractViewList"));
			dataGroup.addChild(DataAtomic.withNameInDataAndValue("searchMetadataId",
					"metadata:AbstractSearch"));
			dataGroup.addChild(DataAtomic.withNameInDataAndValue("searchPresentationFormId",
					"presentation:pgAbstractSearchForm"));
			dataGroup.addChild(DataAtomic.withNameInDataAndValue("userSuppliedId", "false"));
			dataGroup.addChild(
					DataAtomic.withNameInDataAndValue("permissionKey", "RECORDTYPE_ABSTRACT"));
			dataGroup.addChild(DataAtomic.withNameInDataAndValue("selfPresentationViewId",
					"presentation:pgAbstractRecordType"));
			dataGroup.addChild(DataAtomic.withNameInDataAndValue("abstract", "true"));
			return dataGroup;
		}
		if ("child1".equals(id)) {
			String recordType = "recordType";
			DataGroup dataGroup = DataGroup.withNameInData(recordType);

			DataGroup recordInfo = DataGroup.withNameInData("recordInfo");
			recordInfo.addChild(DataAtomic.withNameInDataAndValue("id", "child1"));
			recordInfo.addChild(DataAtomic.withNameInDataAndValue("type", recordType));
			dataGroup.addChild(recordInfo);

			dataGroup.addChild(DataAtomic.withNameInDataAndValue("metadataId", "child1"));
			dataGroup.addChild(DataAtomic.withNameInDataAndValue("presentationViewId",
					"presentation:pgChild1View"));
			dataGroup.addChild(DataAtomic.withNameInDataAndValue("presentationFormId",
					"presentation:pgChild1Form"));
			dataGroup.addChild(
					DataAtomic.withNameInDataAndValue("newMetadataId", "metadata:child1New"));
			dataGroup.addChild(DataAtomic.withNameInDataAndValue("newPresentationFormId",
					"presentation:pgChild1FormNew"));
			dataGroup.addChild(DataAtomic.withNameInDataAndValue("listPresentationViewId",
					"presentation:pgChild1ViewList"));
			dataGroup.addChild(
					DataAtomic.withNameInDataAndValue("searchMetadataId", "metadata:Child1Search"));
			dataGroup.addChild(DataAtomic.withNameInDataAndValue("searchPresentationFormId",
					"presentation:pgChild1SearchForm"));
			dataGroup.addChild(DataAtomic.withNameInDataAndValue("userSuppliedId", "true"));
			dataGroup.addChild(
					DataAtomic.withNameInDataAndValue("permissionKey", "RECORDTYPE_CHILD1"));
			dataGroup.addChild(DataAtomic.withNameInDataAndValue("selfPresentationViewId",
					"presentation:pgChild1RecordType"));

			dataGroup.addChild(DataAtomic.withNameInDataAndValue("abstract", "false"));
			dataGroup.addChild(DataAtomic.withNameInDataAndValue("parentId", "abstract"));
			return dataGroup;
		}
		if ("child2".equals(id)) {
			String recordType = "recordType";
			DataGroup dataGroup = DataGroup.withNameInData(recordType);

			DataGroup recordInfo = DataGroup.withNameInData("recordInfo");
			recordInfo.addChild(DataAtomic.withNameInDataAndValue("id", "child2"));
			recordInfo.addChild(DataAtomic.withNameInDataAndValue("type", recordType));
			dataGroup.addChild(recordInfo);

			dataGroup.addChild(DataAtomic.withNameInDataAndValue("metadataId", "child2"));
			dataGroup.addChild(DataAtomic.withNameInDataAndValue("presentationViewId",
					"presentation:pgChild2View"));
			dataGroup.addChild(DataAtomic.withNameInDataAndValue("presentationFormId",
					"presentation:pgChild2Form"));
			dataGroup.addChild(
					DataAtomic.withNameInDataAndValue("newMetadataId", "metadata:child2New"));
			dataGroup.addChild(DataAtomic.withNameInDataAndValue("newPresentationFormId",
					"presentation:pgChild2FormNew"));
			dataGroup.addChild(DataAtomic.withNameInDataAndValue("listPresentationViewId",
					"presentation:pgChild2ViewList"));
			dataGroup.addChild(
					DataAtomic.withNameInDataAndValue("searchMetadataId", "metadata:Child2Search"));
			dataGroup.addChild(DataAtomic.withNameInDataAndValue("searchPresentationFormId",
					"presentation:pgChild2SearchForm"));
			dataGroup.addChild(DataAtomic.withNameInDataAndValue("userSuppliedId", "true"));
			dataGroup.addChild(
					DataAtomic.withNameInDataAndValue("permissionKey", "RECORDTYPE_CHILD2"));
			dataGroup.addChild(DataAtomic.withNameInDataAndValue("selfPresentationViewId",
					"presentation:pgChild2RecordType"));

			dataGroup.addChild(DataAtomic.withNameInDataAndValue("abstract", "false"));
			dataGroup.addChild(DataAtomic.withNameInDataAndValue("parentId", "abstract"));
			return dataGroup;
		}
		if ("otherType".equals(id)) {
			String recordType = "recordType";
			DataGroup dataGroup = DataGroup.withNameInData(recordType);

			DataGroup recordInfo = DataGroup.withNameInData("recordInfo");
			recordInfo.addChild(DataAtomic.withNameInDataAndValue("id", "otherType"));
			recordInfo.addChild(DataAtomic.withNameInDataAndValue("type", recordType));
			dataGroup.addChild(recordInfo);

			dataGroup.addChild(DataAtomic.withNameInDataAndValue("metadataId", "otherType"));
			dataGroup.addChild(DataAtomic.withNameInDataAndValue("presentationViewId",
					"presentation:pgotherTypeView"));
			dataGroup.addChild(DataAtomic.withNameInDataAndValue("presentationFormId",
					"presentation:pgotherTypeForm"));
			dataGroup.addChild(
					DataAtomic.withNameInDataAndValue("newMetadataId", "metadata:otherTypeNew"));
			dataGroup.addChild(DataAtomic.withNameInDataAndValue("newPresentationFormId",
					"presentation:pgotherTypeFormNew"));
			dataGroup.addChild(DataAtomic.withNameInDataAndValue("listPresentationViewId",
					"presentation:pgotherTypeViewList"));
			dataGroup.addChild(DataAtomic.withNameInDataAndValue("searchMetadataId",
					"metadata:otherTypeSearch"));
			dataGroup.addChild(DataAtomic.withNameInDataAndValue("searchPresentationFormId",
					"presentation:pgotherTypeSearchForm"));
			dataGroup.addChild(DataAtomic.withNameInDataAndValue("userSuppliedId", "true"));
			dataGroup.addChild(
					DataAtomic.withNameInDataAndValue("permissionKey", "RECORDTYPE_otherType"));
			dataGroup.addChild(DataAtomic.withNameInDataAndValue("selfPresentationViewId",
					"presentation:pgotherTypeRecordType"));

			dataGroup.addChild(DataAtomic.withNameInDataAndValue("abstract", "false"));
			dataGroup.addChild(DataAtomic.withNameInDataAndValue("parentId", "NOT ABSTRACT"));
			return dataGroup;
		}
		if ("spyType".equals(id)) {
			String recordType = "recordType";
			DataGroup dataGroup = DataGroup.withNameInData(recordType);

			DataGroup recordInfo = DataGroup.withNameInData("recordInfo");
			recordInfo.addChild(DataAtomic.withNameInDataAndValue("id", "spyType"));
			recordInfo.addChild(DataAtomic.withNameInDataAndValue("type", recordType));
			dataGroup.addChild(recordInfo);

			dataGroup.addChild(DataAtomic.withNameInDataAndValue("metadataId", "spyType"));
			dataGroup.addChild(DataAtomic.withNameInDataAndValue("presentationViewId",
					"presentation:pgotherTypeView"));
			dataGroup.addChild(DataAtomic.withNameInDataAndValue("presentationFormId",
					"presentation:pgotherTypeForm"));
			dataGroup.addChild(
					DataAtomic.withNameInDataAndValue("newMetadataId", "metadata:otherTypeNew"));
			dataGroup.addChild(DataAtomic.withNameInDataAndValue("newPresentationFormId",
					"presentation:pgotherTypeFormNew"));
			dataGroup.addChild(DataAtomic.withNameInDataAndValue("listPresentationViewId",
					"presentation:pgotherTypeViewList"));
			dataGroup.addChild(DataAtomic.withNameInDataAndValue("searchMetadataId",
					"metadata:otherTypeSearch"));
			dataGroup.addChild(DataAtomic.withNameInDataAndValue("searchPresentationFormId",
					"presentation:pgotherTypeSearchForm"));
			dataGroup.addChild(DataAtomic.withNameInDataAndValue("userSuppliedId", "false"));
			dataGroup.addChild(
					DataAtomic.withNameInDataAndValue("permissionKey", "RECORDTYPE_otherType"));
			dataGroup.addChild(DataAtomic.withNameInDataAndValue("selfPresentationViewId",
					"presentation:pgotherTypeRecordType"));

			dataGroup.addChild(DataAtomic.withNameInDataAndValue("abstract", "false"));
			dataGroup.addChild(DataAtomic.withNameInDataAndValue("parentId", "NOT ABSTRACT"));
			return dataGroup;
		}
		return null;
	}

	@Override
	public void create(String type, String id, DataGroup record, DataGroup linkList) {
		createWasCalled = true;

	}

	@Override
	public void deleteByTypeAndId(String type, String id) {
		deleteWasCalled = true;
	}

	@Override
	public void update(String type, String id, DataGroup record) {
		// TODO Auto-generated method stub

	}

	@Override
	public Collection<DataGroup> readList(String type) {
		readLists.add(type);
		if ("recordType".equals(type)) {
			ArrayList<DataGroup> recordTypes = new ArrayList<>();
			recordTypes.add(read("recordType", "abstract"));
			recordTypes.add(read("recordType", "child1"));
			recordTypes.add(read("recordType", "child2"));
			recordTypes.add(read("recordType", "otherType"));
			return recordTypes;
		}
		return new ArrayList<DataGroup>();
	}

	@Override
	public DataGroup readLinkList(String type, String id) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public DataGroup generateLinkCollectionPointingToRecord(String type, String id) {
		// TODO Auto-generated method stub
		return null;
	}

}
