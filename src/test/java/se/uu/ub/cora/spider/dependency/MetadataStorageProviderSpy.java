package se.uu.ub.cora.spider.dependency;

import java.util.Map;

import se.uu.ub.cora.bookkeeper.storage.MetadataStorage;
import se.uu.ub.cora.storage.MetadataStorageProvider;

public class MetadataStorageProviderSpy implements MetadataStorageProvider {
	private MetadataStorageSpy metadataStorage = new MetadataStorageSpy();

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
	public MetadataStorage getMetadataStorage() {
		return metadataStorage;
	}

}
