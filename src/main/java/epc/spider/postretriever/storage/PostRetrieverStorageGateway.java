package epc.spider.postretriever.storage;

import epc.metadataformat.data.DataGroup;

public interface PostRetrieverStorageGateway {

	DataGroup read(String postType, String postId);
	
	
}
