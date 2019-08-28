package se.uu.ub.cora.spider.record;

import java.util.Collection;
import java.util.List;

import se.uu.ub.cora.data.DataAtomic;
import se.uu.ub.cora.data.DataGroup;
import se.uu.ub.cora.spider.testdata.DataCreator;
import se.uu.ub.cora.storage.RecordStorage;
import se.uu.ub.cora.storage.SpiderReadResult;

public class RecordStorageResultListCreatorSpy implements RecordStorage {

	public long start;
	public long totalNumberOfMatches;
	public List<DataGroup> listOfDataGroups;
	public String abstractString = "false";

	@Override
	public DataGroup read(String type, String id) {
		DataGroup dataGroup = DataGroup.withNameInData("recordType");

		DataGroup recordInfo = DataCreator.createRecordInfoWithRecordTypeAndRecordId("recordType",
				"metadata");
		dataGroup.addChild(recordInfo);

		dataGroup.addChild(DataAtomic.withNameInDataAndValue("abstract", abstractString));
		return dataGroup;
	}

	@Override
	public void create(String type, String id, DataGroup record, DataGroup collectedTerms,
			DataGroup linkList, String dataDivider) {
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
	public void update(String type, String id, DataGroup record, DataGroup collectedTerms,
			DataGroup linkList, String dataDivider) {
		// TODO Auto-generated method stub

	}

	@Override
	public SpiderReadResult readList(String type, DataGroup filter) {
		return createSpiderReadResult();
	}

	private SpiderReadResult createSpiderReadResult() {
		SpiderReadResult srr = new SpiderReadResult();
		srr.start = start;
		srr.totalNumberOfMatches = totalNumberOfMatches;
		srr.listOfDataGroups = listOfDataGroups;
		return srr;
	}

	@Override
	public SpiderReadResult readAbstractList(String type, DataGroup filter) {
		return createSpiderReadResult();
	}

	@Override
	public DataGroup readLinkList(String type, String id) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Collection<DataGroup> generateLinkCollectionPointingToRecord(String type, String id) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public boolean recordsExistForRecordType(String type) {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public boolean recordExistsForAbstractOrImplementingRecordTypeAndRecordId(String type,
			String id) {
		// TODO Auto-generated method stub
		return false;
	}

}
