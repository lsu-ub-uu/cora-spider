package epc.spider.postretriever.storage;

import java.util.HashMap;
import java.util.Map;

import epc.metadataformat.data.DataGroup;

public class PostRetrieverInMemoryStorage implements
		PostRetrieverStorageGateway {
	private Map<String, Map<String, DataGroup>> posts = new HashMap<>();

	public PostRetrieverInMemoryStorage(
			Map<String, Map<String, DataGroup>> posts) {
		if(null==posts){
			throw new IllegalArgumentException("posts must not be null");
		}
		this.posts = posts;
	}

	public PostRetrieverInMemoryStorage() {
		// Make it possible to use default empty post storage
	}

	public void create(String postType, String postId, DataGroup dataGroup) {
		ensurePostTypeStorageExists(postType);
		posts.get(postType).put(postId, dataGroup);
	}

	public DataGroup read(String postType, String postId) {
		return posts.get(postType).get(postId);
	}

	private void ensurePostTypeStorageExists(String postType) {
		if (null == posts.get(postType)) {
		posts.put(postType, new HashMap<String, DataGroup>());
		}
	}

}
