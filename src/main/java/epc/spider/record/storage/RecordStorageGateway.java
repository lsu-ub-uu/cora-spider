package epc.spider.record.storage;

import epc.metadataformat.data.DataGroup;

public interface RecordStorageGateway {

	DataGroup read(String recordType, String recordId);
	
	
}
