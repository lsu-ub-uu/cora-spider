package epc.spider.getmetadata.testdata;

import epc.metadataformat.MetadataChildReference;
import epc.metadataformat.MetadataGroup;
import epc.metadataformat.MetadataHolder;
import epc.metadataformat.TextVariable;

public class TestDataAuthority {
	public static MetadataHolder createTestAuthorityMetadataHolder() {

		MetadataHolder metadataHolder = new MetadataHolder();
		
		//otherVariable
		String regularExpression = "((^(([0-1][0-9])|([2][0-3])):[0-5][0-9]$|^$){1}";
		TextVariable otherVariable = new TextVariable("otherId", "otherDataId",
				"otherTextId", "otherDeffTextId", regularExpression);
		metadataHolder.addMetadataElement(otherVariable);

		//authorityGroup
		MetadataGroup authorityGroup = new MetadataGroup("authority",
				"authorityDataId", "authorityTextId", "authorityDeffTextId");
		metadataHolder.addMetadataElement(authorityGroup);
		MetadataChildReference otherReference = new MetadataChildReference(
				"otherTextId", 1, MetadataChildReference.UNLIMITED);
		authorityGroup.addChildReference(otherReference);
		
		
		return metadataHolder;
	}

}
