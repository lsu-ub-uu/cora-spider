package se.uu.ub.cora.spider.spy;

import java.util.ArrayList;
import java.util.List;

import se.uu.ub.cora.data.DataGroup;
import se.uu.ub.cora.search.RecordIndexer;
import se.uu.ub.cora.testutils.mcr.MethodCallRecorder;

public class RecordIndexerSpy implements RecordIndexer {

	public MethodCallRecorder MCR = new MethodCallRecorder();
	public DataGroup collectedData;
	public DataGroup record;

	public String id;
	public String type;

	public boolean indexDataHasBeenCalled = false;
	public List<String> ids = new ArrayList<>();
	public boolean throwErrorOnEvenCalls = false;

	@Override
	public void indexData(List<String> ids, DataGroup collectedData, DataGroup record) {
		MCR.addCall("ids", ids, "collectedData", collectedData, "record", record);
		this.ids = ids;
		this.collectedData = collectedData;
		this.record = record;
		indexDataHasBeenCalled = true;
		if (throwErrorOnEvenCalls) {
			if (MCR.getNumberOfCallsToMethod("indexData") % 2 == 0) {
				throw new RuntimeException("Some error from spy");
			}
		}
	}

	@Override
	public void deleteFromIndex(String type, String id) {
		MCR.addCall("type", type, "id", id);
		this.type = type;
		this.id = id;
	}

	@Override
	public void indexDataWithoutExplicitCommit(List<String> ids, DataGroup collectedData,
			DataGroup record) {
		MCR.addCall("ids", ids, "collectedData", collectedData, "record", record);
		this.ids = ids;
		this.collectedData = collectedData;
		this.record = record;
		if (throwErrorOnEvenCalls) {
			if (MCR.getNumberOfCallsToMethod("indexDataWithoutExplicitCommit") % 2 == 0) {
				throw new RuntimeException("Some error from spy");
			}
		}
	}

}
