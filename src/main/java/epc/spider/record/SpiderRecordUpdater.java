package epc.spider.record;

import epc.spider.data.SpiderDataGroup;
import epc.spider.data.SpiderDataRecord;

public interface SpiderRecordUpdater {

	SpiderDataRecord updateRecord(String userId, String type, String id, SpiderDataGroup record);

}
