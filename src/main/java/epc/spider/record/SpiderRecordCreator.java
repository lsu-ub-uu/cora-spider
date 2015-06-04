package epc.spider.record;

import epc.spider.data.SpiderDataGroup;
import epc.spider.data.SpiderDataRecord;

public interface SpiderRecordCreator {
	SpiderDataRecord createAndStoreRecord(String userId, String type, SpiderDataGroup record);

}
