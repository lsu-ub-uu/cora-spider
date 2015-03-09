package epc.spider.getmetadata;

import static org.testng.Assert.assertNotNull;

import org.testng.Assert;
import org.testng.annotations.Test;

import epc.metadataformat.CoherentMetadata;
import epc.metadataformat.MetadataHolder;
import epc.metadataformat.TextHolder;
import epc.metadataformat.TextVariable;
import epc.metadataformat.getmetadata.MetadataGetter;
import epc.metadataformat.getmetadata.MetadataGetterImp;
import epc.metadataformat.storage.MetadataStorageInMemory;
import epc.metadataformat.storage.MetadataStorage;
import epc.spider.getmetadata.testdata.TestDataAuthority;

public class SpiderMetadataGetterTest {
	@Test
	public void testInit() {
		MetadataStorage metadataInMemoryStorage = new MetadataStorageInMemory();
		MetadataGetter metadataStorageGetter = MetadataGetterImp.usingMetadataStorage(metadataInMemoryStorage);
		SpiderMetadataGetter metadataGetter = SpiderMetadataGetterImp.usingMetadataGetter(metadataStorageGetter);

		CoherentMetadata coherentMetadata = metadataGetter.getAllMetadata();
		assertNotNull(coherentMetadata);
	}

	@Test(expectedExceptions = IllegalArgumentException.class)
	public void testInitWithNull() {
		SpiderMetadataGetterImp.usingMetadataGetter(null);
	}

	@Test
	private void testGetAllMetadata() {

		TextHolder textHolder = new TextHolder();
		MetadataHolder metadataHolder = TestDataAuthority.createTestAuthorityMetadataHolder();
		CoherentMetadata coherentMetadata = CoherentMetadata.usingTextHolderAndMetadataHolder(textHolder, metadataHolder);
		MetadataStorage metadataInMemoryStorage = MetadataStorageInMemory.usingCoherentMetadata(coherentMetadata);
		MetadataGetter metadataStorageGetter = MetadataGetterImp.usingMetadataStorage(metadataInMemoryStorage);

		// spider
		SpiderMetadataGetter metadataGetter = SpiderMetadataGetterImp.usingMetadataGetter(metadataStorageGetter);

		CoherentMetadata coherentMetadataOut = metadataGetter.getAllMetadata();

		TextVariable otherVariable = (TextVariable) coherentMetadataOut.getMetadataElements()
				.getMetadataElement("otherId");

		Assert.assertEquals(otherVariable.getDataId(), "otherDataId",
				"otherDataId should be otherDataId as defined in the testData");
	}
}
