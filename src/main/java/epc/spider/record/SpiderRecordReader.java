package epc.spider.record;

import epc.spider.data.SpiderDataRecord;
import epc.spider.data.SpiderRecordList;

public interface SpiderRecordReader {

	SpiderDataRecord readRecord(String userId, String type, String id);

	SpiderRecordList readRecordList(String userId, String type);
}
