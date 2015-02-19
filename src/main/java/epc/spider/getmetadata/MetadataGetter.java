package epc.spider.getmetadata;

import epc.metadataformat.CoherentMetadata;
import epc.metadataformat.getmetadata.MetadataStorageGetterInputBoundry;

public class MetadataGetter implements MetadataGetterInputBoundry {

	private MetadataStorageGetterInputBoundry getMetadataInputBoundry;

	public MetadataGetter(MetadataStorageGetterInputBoundry getMetadataInputBoundry) {
		if (null == getMetadataInputBoundry) {
			throw new IllegalArgumentException(
					"getMetadataInputBoundry must not be null");
		}
		this.getMetadataInputBoundry = getMetadataInputBoundry;
	}

	public CoherentMetadata getAllMetadata() {

		return getMetadataInputBoundry.getAllMetadata();
	}

}
