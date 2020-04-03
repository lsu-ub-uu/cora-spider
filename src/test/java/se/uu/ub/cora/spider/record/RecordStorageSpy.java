package se.uu.ub.cora.spider.record;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import se.uu.ub.cora.data.DataGroup;
import se.uu.ub.cora.spider.data.DataAtomicSpy;
import se.uu.ub.cora.spider.data.DataGroupSpy;
import se.uu.ub.cora.spider.spy.MethodCallRecorder;
import se.uu.ub.cora.spider.testdata.DataCreator;
import se.uu.ub.cora.storage.RecordStorage;
import se.uu.ub.cora.storage.StorageReadResult;

public class RecordStorageSpy implements RecordStorage {
	public MethodCallRecorder MCR = new MethodCallRecorder();

	public long start = 0;
	public long totalNumberOfMatches = 0;
	public List<DataGroup> listOfDataGroups = Collections.emptyList();
	public String abstractString = "false";

	public int numberOfRecordsToReturn = 0;

	@Override
	public DataGroup read(String type, String id) {
		MCR.addCall("type", type, "id", id);
		DataGroup dataGroup = new DataGroupSpy("recordType");

		DataGroup recordInfo = DataCreator.createRecordInfoWithRecordTypeAndRecordId("recordType",
				"metadata");
		dataGroup.addChild(recordInfo);
		dataGroup.addChild(new DataAtomicSpy("abstract", abstractString));
		MCR.addReturned(dataGroup);
		return dataGroup;
	}

	@Override
	public void create(String type, String id, DataGroup record, DataGroup collectedTerms,
			DataGroup linkList, String dataDivider) {
		MCR.addCall("type", type, "id", id, "record", record, "collectedTerms", collectedTerms,
				"linkList", linkList, "dataDivider", dataDivider);
	}

	@Override
	public void deleteByTypeAndId(String type, String id) {
		MCR.addCall("type", type, "id", id);
	}

	@Override
	public boolean linksExistForRecord(String type, String id) {
		MCR.addCall("type", type, "id", id);
		MCR.addReturned(false);
		return false;
	}

	@Override
	public void update(String type, String id, DataGroup record, DataGroup collectedTerms,
			DataGroup linkList, String dataDivider) {
		MCR.addCall("type", type, "id", id, "record", record, "collectedTerms", collectedTerms,
				"linkList", linkList, "dataDivider", dataDivider);
	}

	@Override
	public StorageReadResult readList(String type, DataGroup filter) {
		MCR.addCall("type", type, "filter", filter);
		StorageReadResult createSpiderReadResult = createSpiderReadResult();
		MCR.addReturned(createSpiderReadResult);
		return createSpiderReadResult;
	}

	private StorageReadResult createSpiderReadResult() {
		StorageReadResult readResult = new StorageReadResult();
		readResult.start = start;
		readResult.totalNumberOfMatches = totalNumberOfMatches;
		if (numberOfRecordsToReturn > 0) {
			listOfDataGroups = new ArrayList<>();
			addRecordsToList();
		}
		readResult.listOfDataGroups = listOfDataGroups;
		return readResult;
	}

	private void addRecordsToList() {
		int i = (int) start;
		while (i < numberOfRecordsToReturn) {
			DataGroupSpy topDataGroup = new DataGroupSpy("dummy");
			DataGroupSpy recordInfo = new DataGroupSpy("recordInfo");
			topDataGroup.addChild(recordInfo);
			DataGroupSpy type = new DataGroupSpy("type");
			recordInfo.addChild(type);
			type.addChild(new DataAtomicSpy("linkedRecordId", "dummyRecordType"));
			listOfDataGroups.add(topDataGroup);
			i++;
		}
	}

	@Override
	public StorageReadResult readAbstractList(String type, DataGroup filter) {
		MCR.addCall("type", type, "filter", filter);
		StorageReadResult createSpiderReadResult = createSpiderReadResult();
		MCR.addReturned(createSpiderReadResult);
		return createSpiderReadResult;
	}

	@Override
	public DataGroup readLinkList(String type, String id) {
		MCR.addCall("type", type, "id", id);
		MCR.addReturned(null);
		return null;
	}

	@Override
	public Collection<DataGroup> generateLinkCollectionPointingToRecord(String type, String id) {
		MCR.addCall("type", type, "id", id);
		MCR.addReturned(null);
		return null;
	}

	@Override
	public boolean recordsExistForRecordType(String type) {
		MCR.addCall("type", type);
		MCR.addReturned(null);
		return false;
	}

	@Override
	public boolean recordExistsForAbstractOrImplementingRecordTypeAndRecordId(String type,
			String id) {
		MCR.addCall("type", type, "id", id);
		MCR.addReturned(false);
		return false;
	}

}
