package epc.spider.record;

import epc.metadataformat.data.DataGroup;

public interface RecordInputBoundary {

	DataGroup readRecord(String userId, String type, String id);

}
