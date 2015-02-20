package epc.spider.getmetadata;

import epc.metadataformat.CoherentMetadata;

/**
 * MetadataGetterInputBoundry provides an interface for the MetadataGetter
 * 
 * @author <a href="mailto:olov.mckie@ub.uu.se">Olov McKie</a>
 *
 * @since 0.1
 *
 */
public interface MetadataGetterInputBoundry {
	/**
	 * getAllMetadata returns all metadata for the whole system, as a
	 * CoherentMetadata populated with metadataFormat, Presentation, Collections
	 * and Texts
	 * 
	 * @return A CoherentMetadata with all metadata for the entire system
	 */
	public CoherentMetadata getAllMetadata();
}
