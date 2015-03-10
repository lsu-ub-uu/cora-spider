package epc.spider.getmetadata;

import epc.metadataformat.CoherentMetadata;
import epc.metadataformat.getmetadata.MetadataGetter;

/**
 * SpiderMetadataGetterImp makes it possible to get metadata from inner parts of the system.
 * 
 * @author <a href="mailto:olov.mckie@ub.uu.se">Olov McKie</a>
 *
 * @since 0.1
 *
 */
public final class SpiderMetadataGetterImp implements SpiderMetadataGetter {

	private final MetadataGetter metadataGetter;

	public static SpiderMetadataGetterImp usingMetadataGetter(MetadataGetter metadataGetter) {
		return new SpiderMetadataGetterImp(metadataGetter);
	}

	private SpiderMetadataGetterImp(MetadataGetter metadataGetter) {
		throwErrorIfConstrucotrArgumentIsNull(metadataGetter);
		this.metadataGetter = metadataGetter;
	}

	private void throwErrorIfConstrucotrArgumentIsNull(MetadataGetter metadataGetter) {
		if (null == metadataGetter) {
			throw new IllegalArgumentException("metadataGetter must not be null");
		}
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public CoherentMetadata getAllMetadata() {
		return metadataGetter.getAllMetadata();
	}

}
