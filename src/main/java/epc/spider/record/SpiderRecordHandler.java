package epc.spider.record;

import epc.spider.data.SpiderDataGroup;

public interface SpiderRecordHandler {

	SpiderDataGroup readRecord(String userId, String type, String id);

	SpiderDataGroup createAndStoreRecord(String userId, String type, SpiderDataGroup record);

	void deleteRecord(String userId, String type, String id);
}
