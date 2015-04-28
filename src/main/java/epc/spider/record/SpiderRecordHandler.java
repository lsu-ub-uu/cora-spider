package epc.spider.record;

import epc.spider.data.SpiderDataGroup;
import epc.spider.data.SpiderDataRecord;
import epc.spider.data.SpiderRecordList;

public interface SpiderRecordHandler {

	SpiderDataRecord readRecord(String userId, String type, String id);

	SpiderDataRecord createAndStoreRecord(String userId, String type, SpiderDataGroup record);

	void deleteRecord(String userId, String type, String id);

	SpiderDataRecord updateRecord(String userId, String type, String id, SpiderDataGroup record);

	SpiderRecordList readRecordList(String userId, String type);
}
