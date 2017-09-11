package se.uu.ub.cora.spider.record;

import se.uu.ub.cora.spider.data.SpiderDataList;

public interface SpiderRecordIncomingLinksReader {
	SpiderDataList readIncomingLinks(String authToken, String type, String id);
}
