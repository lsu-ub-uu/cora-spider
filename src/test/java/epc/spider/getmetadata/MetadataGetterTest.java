package epc.spider.getmetadata;

import static org.testng.Assert.assertNotNull;

import org.testng.Assert;
import org.testng.annotations.Test;

import epc.metadataformat.CoherentMetadata;
import epc.metadataformat.MetadataHolder;
import epc.metadataformat.TextHolder;
import epc.metadataformat.TextVariable;
import epc.metadataformat.getmetadata.MetadataStorageGetter;
import epc.metadataformat.getmetadata.MetadataStorageGetterInputBoundary;
import epc.metadataformat.storage.MetadataInMemoryStorage;
import epc.metadataformat.storage.MetadataStorageGateway;
import epc.spider.getmetadata.testdata.TestDataAuthority;

public class MetadataGetterTest {
	@Test
	public void testInit() {
		MetadataStorageGateway metadataInMemoryStorage = new MetadataInMemoryStorage();
		MetadataStorageGetterInputBoundary metadataStorageGetter = new MetadataStorageGetter(
				metadataInMemoryStorage);
		MetadataGetterInputBoundary metadataGetter = new MetadataGetter(
				metadataStorageGetter);

		CoherentMetadata coherentMetadata = metadataGetter.getAllMetadata();
		assertNotNull(coherentMetadata);
	}

	@Test(expectedExceptions = IllegalArgumentException.class)
	public void testInitWithNull() {
		new MetadataGetter(null);
	}

	@Test
	private void testGetAllMetadata() {

		TextHolder textHolder = new TextHolder();
		MetadataHolder metadataHolder = TestDataAuthority
				.createTestAuthorityMetadataHolder();
		CoherentMetadata coherentMetadata = new CoherentMetadata(textHolder,
				metadataHolder);
		MetadataStorageGateway metadataInMemoryStorage = new MetadataInMemoryStorage(
				coherentMetadata);
		MetadataStorageGetterInputBoundary metadataStorageGetter = new MetadataStorageGetter(
				metadataInMemoryStorage);
		
		//spider
		MetadataGetterInputBoundary metadataGetter = new MetadataGetter(
				metadataStorageGetter);

		CoherentMetadata coherentMetadataOut = metadataGetter.getAllMetadata();

		TextVariable otherVariable = (TextVariable) coherentMetadataOut
				.getMetadataElements().getMetadataElement("otherId");

		Assert.assertEquals(otherVariable.getDataId(), "otherDataId",
				"otherDataId should be otherDataId as defined in the testData");
	}
}
