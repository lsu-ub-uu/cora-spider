package se.uu.ub.cora.spider.record.internal;

import se.uu.ub.cora.data.DataGroup;
import se.uu.ub.cora.data.DataRecord;

public interface RecordListIndexer {

	DataRecord indexRecordList(String authToken, String type, DataGroup filter);

}
