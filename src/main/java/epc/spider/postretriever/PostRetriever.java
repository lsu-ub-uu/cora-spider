package epc.spider.postretriever;

import epc.beefeater.AuthorizationInputBoundary;
import epc.metadataformat.data.DataGroup;
import epc.spider.postretriever.storage.PostRetrieverStorageGateway;

public class PostRetriever implements PostRetrieverInputBoundary {

	private PostRetrieverStorageGateway postRetrieverStorageGateway;
	private AuthorizationInputBoundary authorization;

	public PostRetriever(AuthorizationInputBoundary authorization,
			PostRetrieverStorageGateway postRetrieverStorageGateway) {
		this.authorization = authorization;
		this.postRetrieverStorageGateway = postRetrieverStorageGateway;
	}

	@Override
	public DataGroup retrievePost(String userId, String postType, String postId) {
//		if(authorization.isAuthorized(userId, permissionKey)){
//			
//		}
		return postRetrieverStorageGateway.read(postType, postId);
	}

}
