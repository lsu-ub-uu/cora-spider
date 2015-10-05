package se.uu.ub.cora.spider.record.storage;

import java.util.Collection;

import se.uu.ub.cora.metadataformat.data.DataGroup;

public interface RecordStorage {

	DataGroup read(String type, String id);

	void create(String type, String id, DataGroup record);

	void deleteByTypeAndId(String type, String id);

	void update(String type, String id, DataGroup record);

	Collection<DataGroup> readList(String type);

}
