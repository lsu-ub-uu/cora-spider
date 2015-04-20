package epc.spider.record;

import epc.metadataformat.data.DataGroup;
import epc.spider.record.storage.RecordStorage;

public class RecordStorageDeleteSpy implements RecordStorage {

	public boolean deleteWasCalled = false;

	@Override
	public DataGroup read(String type, String id) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void create(String type, String id, DataGroup record) {
		// TODO Auto-generated method stub

	}

	@Override
	public void deleteByTypeAndId(String type, String id) {
		deleteWasCalled = true;

	}

	@Override
	public void update(String type, String id, DataGroup record) {
		// TODO Auto-generated method stub

	}

}
