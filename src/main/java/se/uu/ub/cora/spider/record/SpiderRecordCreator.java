package se.uu.ub.cora.spider.record;

import se.uu.ub.cora.spider.data.SpiderDataGroup;
import se.uu.ub.cora.spider.data.SpiderDataRecord;

public interface SpiderRecordCreator {
	SpiderDataRecord createAndStoreRecord(String userId, String type, SpiderDataGroup record);

}
