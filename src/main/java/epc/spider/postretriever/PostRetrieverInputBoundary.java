package epc.spider.postretriever;

import epc.metadataformat.data.DataGroup;

public interface PostRetrieverInputBoundary {

	DataGroup retrievePost(String userId, String type, String id);

}
