package epc.spider.record;

import java.util.ArrayList;
import java.util.Collection;

import epc.metadataformat.data.DataAtomic;
import epc.metadataformat.data.DataGroup;
import epc.spider.record.storage.RecordStorage;

public class RecordStorageListReaderSpy implements RecordStorage {

	public Collection<String> readLists = new ArrayList<>();

	@Override
	public DataGroup read(String type, String id) {
		if("abstract".equals(id)){
			String recordType = "recordType";
			DataGroup dataGroup = DataGroup.withDataId(recordType);

			DataGroup recordInfo = DataGroup.withDataId("recordInfo");
			recordInfo.addChild(DataAtomic.withDataIdAndValue("id", "abstract"));
			recordInfo.addChild(DataAtomic.withDataIdAndValue("type", recordType));
			dataGroup.addChild(recordInfo);

			dataGroup.addChild(DataAtomic.withDataIdAndValue("metadataId", "abstract"));
			dataGroup.addChild(DataAtomic.withDataIdAndValue("presentationViewId",
					"presentation:pgAbstractView"));
			dataGroup.addChild(DataAtomic.withDataIdAndValue("presentationFormId",
					"presentation:pgAbstractForm"));
			dataGroup.addChild(DataAtomic.withDataIdAndValue("newMetadataId", "metadata:abstractNew"));
			dataGroup.addChild(DataAtomic.withDataIdAndValue("newPresentationFormId",
					"presentation:pgAbstractFormNew"));
			dataGroup.addChild(DataAtomic.withDataIdAndValue("listPresentationViewId",
					"presentation:pgAbstractViewList"));
			dataGroup.addChild(DataAtomic
					.withDataIdAndValue("searchMetadataId", "metadata:AbstractSearch"));
			dataGroup.addChild(DataAtomic.withDataIdAndValue("searchPresentationFormId",
					"presentation:pgAbstractSearchForm"));
			dataGroup.addChild(DataAtomic.withDataIdAndValue("userSuppliedId", "false"));
			dataGroup.addChild(DataAtomic.withDataIdAndValue("permissionKey", "RECORDTYPE_ABSTRACT"));
			dataGroup.addChild(DataAtomic.withDataIdAndValue("selfPresentationViewId",
					"presentation:pgAbstractRecordType"));
			dataGroup.addChild(DataAtomic.withDataIdAndValue("abstract", "true"));
			return dataGroup;
		}
		if("child1".equals(id)){
				String recordType = "recordType";
				DataGroup dataGroup = DataGroup.withDataId(recordType);

				DataGroup recordInfo = DataGroup.withDataId("recordInfo");
				recordInfo.addChild(DataAtomic.withDataIdAndValue("id", "child1"));
				recordInfo.addChild(DataAtomic.withDataIdAndValue("type", recordType));
				dataGroup.addChild(recordInfo);

				dataGroup.addChild(DataAtomic.withDataIdAndValue("metadataId", "child1"));
				dataGroup.addChild(DataAtomic.withDataIdAndValue("presentationViewId",
						"presentation:pgChild1View"));
				dataGroup.addChild(DataAtomic.withDataIdAndValue("presentationFormId",
						"presentation:pgChild1Form"));
				dataGroup.addChild(DataAtomic.withDataIdAndValue("newMetadataId", "metadata:child1New"));
				dataGroup.addChild(DataAtomic.withDataIdAndValue("newPresentationFormId",
						"presentation:pgChild1FormNew"));
				dataGroup.addChild(DataAtomic.withDataIdAndValue("listPresentationViewId",
						"presentation:pgChild1ViewList"));
				dataGroup.addChild(DataAtomic
						.withDataIdAndValue("searchMetadataId", "metadata:Child1Search"));
				dataGroup.addChild(DataAtomic.withDataIdAndValue("searchPresentationFormId",
						"presentation:pgChild1SearchForm"));
				dataGroup.addChild(DataAtomic.withDataIdAndValue("userSuppliedId", "true"));
				dataGroup.addChild(DataAtomic.withDataIdAndValue("permissionKey", "RECORDTYPE_CHILD1"));
				dataGroup.addChild(DataAtomic.withDataIdAndValue("selfPresentationViewId",
						"presentation:pgChild1RecordType"));
				
				dataGroup.addChild(DataAtomic.withDataIdAndValue("abstract", "false"));
				dataGroup.addChild(DataAtomic.withDataIdAndValue("parentId", "abstract"));
				return dataGroup;
		}
		if("child2".equals(id)){
			String recordType = "recordType";
			DataGroup dataGroup = DataGroup.withDataId(recordType);
			
			DataGroup recordInfo = DataGroup.withDataId("recordInfo");
			recordInfo.addChild(DataAtomic.withDataIdAndValue("id", "child2"));
			recordInfo.addChild(DataAtomic.withDataIdAndValue("type", recordType));
			dataGroup.addChild(recordInfo);
			
			dataGroup.addChild(DataAtomic.withDataIdAndValue("metadataId", "child2"));
			dataGroup.addChild(DataAtomic.withDataIdAndValue("presentationViewId",
					"presentation:pgChild2View"));
			dataGroup.addChild(DataAtomic.withDataIdAndValue("presentationFormId",
					"presentation:pgChild2Form"));
			dataGroup.addChild(DataAtomic.withDataIdAndValue("newMetadataId", "metadata:child2New"));
			dataGroup.addChild(DataAtomic.withDataIdAndValue("newPresentationFormId",
					"presentation:pgChild2FormNew"));
			dataGroup.addChild(DataAtomic.withDataIdAndValue("listPresentationViewId",
					"presentation:pgChild2ViewList"));
			dataGroup.addChild(DataAtomic
					.withDataIdAndValue("searchMetadataId", "metadata:Child2Search"));
			dataGroup.addChild(DataAtomic.withDataIdAndValue("searchPresentationFormId",
					"presentation:pgChild2SearchForm"));
			dataGroup.addChild(DataAtomic.withDataIdAndValue("userSuppliedId", "true"));
			dataGroup.addChild(DataAtomic.withDataIdAndValue("permissionKey", "RECORDTYPE_CHILD2"));
			dataGroup.addChild(DataAtomic.withDataIdAndValue("selfPresentationViewId",
					"presentation:pgChild2RecordType"));
			
			dataGroup.addChild(DataAtomic.withDataIdAndValue("abstract", "false"));
			dataGroup.addChild(DataAtomic.withDataIdAndValue("parentId", "abstract"));
			return dataGroup;
		}
		return null;
	}

	@Override
	public void create(String type, String id, DataGroup record) {
		// TODO Auto-generated method stub

	}

	@Override
	public void deleteByTypeAndId(String type, String id) {
		// TODO Auto-generated method stub

	}

	@Override
	public void update(String type, String id, DataGroup record) {
		// TODO Auto-generated method stub

	}

	@Override
	public Collection<DataGroup> readList(String type) {
		readLists.add(type);
		if("recordType".equals(type)){
			ArrayList<DataGroup> recordTypes = new ArrayList<>();
			recordTypes.add(read("recordType", "abstract"));
			recordTypes.add(read("recordType", "child1"));
			recordTypes.add(read("recordType", "child2"));
			return recordTypes;
		}
		return new ArrayList<DataGroup>();
	}

}
