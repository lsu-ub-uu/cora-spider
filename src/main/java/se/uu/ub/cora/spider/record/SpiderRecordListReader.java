package se.uu.ub.cora.spider.record;

import se.uu.ub.cora.spider.data.SpiderRecordList;

public interface SpiderRecordListReader {
    SpiderRecordList readRecordList(String userId, String type);
}
