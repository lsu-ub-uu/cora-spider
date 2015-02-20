package epc.spider.getmetadata;

import epc.metadataformat.CoherentMetadata;
import epc.metadataformat.getmetadata.MetadataStorageGetterInputBoundry;

/**
 * MetadataGetter makes it possible to get metadata from inner parts of the
 * system.
 * 
 * @author <a href="mailto:olov.mckie@ub.uu.se">Olov McKie</a>
 *
 * @since 0.1
 *
 */
public class MetadataGetter implements MetadataGetterInputBoundry {

	private final MetadataStorageGetterInputBoundry getMetadataInputBoundry;

	/**
	 * MetadataGetter initializes this MetadataGetter so that it uses the
	 * provided MetadataStorageGetterInputBoundry to get metadata.
	 * 
	 * @param metadataStorageGetterInputBoundry
	 *            An implementation of the
	 *            {@link MetadataStorageGetterInputBoundry} interface to provide
	 *            implementation for the getter.
	 */
	public MetadataGetter(
			MetadataStorageGetterInputBoundry metadataStorageGetterInputBoundry) {
		if (null == metadataStorageGetterInputBoundry) {
			throw new IllegalArgumentException(
					"getMetadataInputBoundry must not be null");
		}
		this.getMetadataInputBoundry = metadataStorageGetterInputBoundry;
	}

	/**
	 * {@inheritDoc}
	 */
	public CoherentMetadata getAllMetadata() {
		return getMetadataInputBoundry.getAllMetadata();
	}

}
