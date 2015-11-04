package se.uu.ub.cora.spider.record;

import java.util.Collection;

import se.uu.ub.cora.bookkeeper.data.DataGroup;
import se.uu.ub.cora.bookkeeper.data.DataRecordLink;
import se.uu.ub.cora.spider.record.storage.RecordStorage;
import se.uu.ub.cora.spider.testdata.DataCreator;

public class RecordStorageProvidingDataForDataRecordLinkTests implements RecordStorage {

	@Override
	public DataGroup read(String type, String id) {
		if (type.equals("recordType")) {
			return DataCreator.createRecordTypeWithIdAndUserSuppliedIdAndAbstract("dataWithLinks",
					"false", "false");
		}
		if (type.equals("dataWithLinks")) {
			DataGroup recordInfo = DataCreator.createRecordInfoWithRecordTypeAndRecordId(type, id);
			DataGroup dataGroup = DataGroup.withNameInData("dataWithLinks");
			dataGroup.addChild(recordInfo);
			if (id.equals("oneLinkTopLevel")) {
				dataGroup.addChild(createLinkToNothing());
				return dataGroup;
			}
			if (id.equals("oneLinkOneLevelDown")) {
				DataGroup oneLevelDown = DataGroup.withNameInData("oneLevelDown");
				dataGroup.addChild(oneLevelDown);
				oneLevelDown.addChild(createLinkToNothing());
				return dataGroup;
			}
		}
		return null;
	}

	private DataRecordLink createLinkToNothing() {
		return DataRecordLink.withNameInDataAndLinkedRecordTypeAndLinkedRecordId("link", "someType",
				"someId");
	}

	@Override
	public void create(String type, String id, DataGroup record, DataGroup linkList) {
		// TODO Auto-generated method stub

	}

	@Override
	public void deleteByTypeAndId(String type, String id) {
		// TODO Auto-generated method stub

	}

	@Override
	public boolean linksExistForRecord(String type, String id) {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public void update(String type, String id, DataGroup record) {
		// TODO Auto-generated method stub

	}

	@Override
	public Collection<DataGroup> readList(String type) {
		// TODO Auto-generated method stub
		return null;
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
