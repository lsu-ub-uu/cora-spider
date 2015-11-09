package se.uu.ub.cora.spider.record;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import se.uu.ub.cora.bookkeeper.data.DataGroup;
import se.uu.ub.cora.spider.record.storage.RecordStorage;
import se.uu.ub.cora.spider.testdata.DataCreator;

public class RecordLinkTestsRecordStorage implements RecordStorage {

	@Override
	public DataGroup read(String type, String id) {
		if (type.equals("recordType")) {
			return DataCreator.createRecordTypeWithIdAndUserSuppliedIdAndAbstract("dataWithLinks",
					"false", "false");
		}
		if (type.equals("dataWithLinks")) {
			if (id.equals("oneLinkTopLevel")) {
				return RecordLinkTestsDataCreator.createSpiderDataGroupWithRecordInfoAndLink()
						.toDataGroup();
			}
			if (id.equals("oneLinkOneLevelDown")) {
				return RecordLinkTestsDataCreator.createDataGroupWithRecordInfoAndLinkOneLevelDown()
						.toDataGroup();
			}
		}
		return null;
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
	public void update(String type, String id, DataGroup record, DataGroup linkList) {
		// TODO Auto-generated method stub

	}

	@Override
	public Collection<DataGroup> readList(String type) {
		List<DataGroup> list = new ArrayList<>();
		list.add(read(type, "oneLinkTopLevel"));
		list.add(read(type, "oneLinkOneLevelDown"));
		return list;
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
