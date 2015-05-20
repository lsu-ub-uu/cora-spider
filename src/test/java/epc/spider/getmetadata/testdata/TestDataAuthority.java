package epc.spider.getmetadata.testdata;

import epc.metadataformat.metadata.MetadataChildReference;
import epc.metadataformat.metadata.MetadataGroup;
import epc.metadataformat.metadata.MetadataHolder;
import epc.metadataformat.metadata.TextVariable;

public class TestDataAuthority {
	public static MetadataHolder createTestAuthorityMetadataHolder() {

		MetadataHolder metadataHolder = new MetadataHolder();
		
		//otherVariable
		String regularExpression = "((^(([0-1][0-9])|([2][0-3])):[0-5][0-9]$|^$){1}";
		TextVariable otherVariable = TextVariable.withIdAndDataIdAndTextIdAndDeffTextIdAndRegularExpression("otherId", "otherDataId", "otherTextId", "otherDeffTextId",
				regularExpression);
		metadataHolder.addMetadataElement(otherVariable);

		//authorityGroup
		MetadataGroup authorityGroup = MetadataGroup.withIdAndDataIdAndTextIdAndDefTextId("authority", "authorityDataId", "authorityTextId", "authorityDeffTextId");
		metadataHolder.addMetadataElement(authorityGroup);
		MetadataChildReference otherReference = MetadataChildReference.withReferenceIdAndRepeatMinAndRepeatMax("otherTextId", 1, MetadataChildReference.UNLIMITED);
		authorityGroup.addChildReference(otherReference);
		
		
		return metadataHolder;
	}

}
