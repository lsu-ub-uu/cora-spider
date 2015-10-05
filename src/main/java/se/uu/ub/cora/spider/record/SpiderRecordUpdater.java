package se.uu.ub.cora.spider.record;

import se.uu.ub.cora.spider.data.SpiderDataGroup;
import se.uu.ub.cora.spider.data.SpiderDataRecord;

public interface SpiderRecordUpdater {

	SpiderDataRecord updateRecord(String userId, String type, String id, SpiderDataGroup record);

}
