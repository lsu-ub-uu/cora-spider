package se.uu.ub.cora.spider.spy;

import se.uu.ub.cora.bookkeeper.data.DataGroup;
import se.uu.ub.cora.spider.search.RecordIndexer;

public class RecordIndexerSpy implements RecordIndexer {

	public DataGroup recordIndexData;
	public DataGroup record;

	public String id;
	public String type;

	public boolean indexDataHasBeenCalled = false;

	@Override
	public void indexData(DataGroup recordIndexData, DataGroup record) {
		this.recordIndexData = recordIndexData;
		this.record = record;
		indexDataHasBeenCalled = true;

	}

	@Override
	public void deleteFromIndex(String type, String id) {
		this.type = type;
		this.id = id;
	}

}
