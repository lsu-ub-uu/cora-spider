package se.uu.ub.cora.spider.dependency;

import java.util.Map;

import se.uu.ub.cora.spider.record.StreamStorageSpy;
import se.uu.ub.cora.storage.StreamStorage;
import se.uu.ub.cora.storage.StreamStorageProvider;

public class StreamStorageProviderSpy implements StreamStorageProvider {
	public StreamStorage streamStorage = new StreamStorageSpy();

	@Override
	public int getOrderToSelectImplementionsBy() {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public void startUsingInitInfo(Map<String, String> initInfo) {
		// TODO Auto-generated method stub

	}

	@Override
	public StreamStorage getStreamStorage() {
		return streamStorage;
	}

}
