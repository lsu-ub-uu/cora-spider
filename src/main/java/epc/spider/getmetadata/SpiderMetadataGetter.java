package epc.spider.getmetadata;

import epc.metadataformat.CoherentMetadata;

/**
 * SpiderMetadataGetter provides an interface for the SpiderMetadataGetterImp
 * 
 * @author <a href="mailto:olov.mckie@ub.uu.se">Olov McKie</a>
 *
 * @since 0.1
 *
 */
public interface SpiderMetadataGetter {
	/**
	 * getAllMetadata returns all metadata for the whole system, as a
	 * CoherentMetadata populated with metadataFormat, Presentation, Collections
	 * and Texts
	 * 
	 * @return A CoherentMetadata with all metadata for the entire system
	 */
	CoherentMetadata getAllMetadata();
}
