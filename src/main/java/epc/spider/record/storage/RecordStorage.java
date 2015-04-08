package epc.spider.record.storage;

import epc.metadataformat.data.DataGroup;

public interface RecordStorage {

	DataGroup read(String recordType, String recordId);

	void create(String type, String id, DataGroup record);

	void deleteByTypeAndId(String recordType, String recordId);

}
