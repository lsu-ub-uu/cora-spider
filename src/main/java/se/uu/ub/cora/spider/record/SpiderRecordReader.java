package se.uu.ub.cora.spider.record;

import se.uu.ub.cora.spider.data.SpiderDataGroup;
import se.uu.ub.cora.spider.data.SpiderDataRecord;
import se.uu.ub.cora.spider.data.SpiderRecordList;

public interface SpiderRecordReader {

	SpiderDataRecord readRecord(String userId, String type, String id);

	SpiderRecordList readRecordList(String userId, String type);

	SpiderDataGroup readIncomingLinks(String userId, String type, String id);
}
