package epc.spider.getmetadata;

import org.testng.Assert;
import org.testng.annotations.Test;

import epc.metadataformat.CoherentMetadata;
import epc.metadataformat.getmetadata.MetadataStorageGetter;
import epc.metadataformat.getmetadata.MetadataStorageGetterInputBoundry;
import epc.metadataformat.storage.MetadataInMemoryStorage;
import epc.metadataformat.storage.MetadataStorageGateway;

public class MetadataGetterTest {
	@Test
	public void testInit() {
		MetadataStorageGateway metadataInMemoryStorage = new MetadataInMemoryStorage();
		MetadataStorageGetterInputBoundry metadataStorageGetter = new MetadataStorageGetter(
				metadataInMemoryStorage);
		MetadataGetterInputBoundry metadataGetter = new MetadataGetter(metadataStorageGetter);

		CoherentMetadata coherentMetadata = metadataGetter.getAllMetadata();
		Assert.assertNotNull(coherentMetadata);
	}

	@Test(expectedExceptions = IllegalArgumentException.class)
	public void testInitWithNull() {
		new MetadataGetter(null);
	}
	
}
