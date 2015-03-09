package epc.spider.record;

import epc.metadataformat.data.DataGroup;

public interface SpiderRecordHandler {

	DataGroup readRecord(String userId, String type, String id);
	
	DataGroup createAndStoreRecord(String userId, String type, DataGroup record);
}
