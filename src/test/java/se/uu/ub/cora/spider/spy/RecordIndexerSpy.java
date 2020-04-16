package se.uu.ub.cora.spider.spy;

import java.util.ArrayList;
import java.util.List;

import se.uu.ub.cora.data.DataGroup;
import se.uu.ub.cora.search.RecordIndexer;

public class RecordIndexerSpy implements RecordIndexer {

	public MethodCallRecorder MCR = new MethodCallRecorder();
	public DataGroup recordIndexData;
	public DataGroup record;

	public String id;
	public String type;

	public boolean indexDataHasBeenCalled = false;
	public List<String> ids = new ArrayList<>();

	@Override
	public void indexData(List<String> ids, DataGroup recordIndexData, DataGroup record) {
		MCR.addCall("ids", ids, "recordIndexData", recordIndexData, "record", record);
		this.ids = ids;
		this.recordIndexData = recordIndexData;
		this.record = record;
		indexDataHasBeenCalled = true;

	}

	@Override
	public void deleteFromIndex(String type, String id) {
		MCR.addCall("type", type, "id", id);
		this.type = type;
		this.id = id;
	}

}
