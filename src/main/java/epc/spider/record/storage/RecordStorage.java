package epc.spider.record.storage;

import epc.spider.data.SpiderDataGroup;

public interface RecordStorage {

	SpiderDataGroup read(String recordType, String recordId);

	void create(String type, String id, SpiderDataGroup record);

}
