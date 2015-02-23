package epc.spider.getmetadata;

import epc.metadataformat.CoherentMetadata;
import epc.metadataformat.getmetadata.MetadataStorageGetterInputBoundary;

/**
 * MetadataGetter makes it possible to get metadata from inner parts of the
 * system.
 * 
 * @author <a href="mailto:olov.mckie@ub.uu.se">Olov McKie</a>
 *
 * @since 0.1
 *
 */
public class MetadataGetter implements MetadataGetterInputBoundary {

	private final MetadataStorageGetterInputBoundary getMetadataInputBoundary;

	/**
	 * MetadataGetter initializes this MetadataGetter so that it uses the
	 * provided MetadataStorageGetterInputBoundary to get metadata.
	 * 
	 * @param metadataStorageGetterInputBoundary
	 *            An implementation of the
	 *            {@link MetadataStorageGetterInputBoundary} interface to provide
	 *            implementation for the getter.
	 */
	public MetadataGetter(
			MetadataStorageGetterInputBoundary metadataStorageGetterInputBoundary) {
		if (null == metadataStorageGetterInputBoundary) {
			throw new IllegalArgumentException(
					"getMetadataInputBoundary must not be null");
		}
		this.getMetadataInputBoundary = metadataStorageGetterInputBoundary;
	}

	/**
	 * {@inheritDoc}
	 */
	public CoherentMetadata getAllMetadata() {
		return getMetadataInputBoundary.getAllMetadata();
	}

}
