package epc.spider.testdata;

import java.util.HashMap;
import java.util.Map;

import epc.metadataformat.data.DataAtomic;
import epc.metadataformat.data.DataGroup;
import epc.spider.postretriever.storage.PostRetrieverInMemoryStorage;

public class TestDataPostRetrieverInMemoryStorage {
	public static PostRetrieverInMemoryStorage createPostRetrieverInMemoryStorageWithTestData() {
		Map<String, Map<String, DataGroup>> posts = new HashMap<>();
		posts.put("place", new HashMap<String, DataGroup>());

		DataGroup recordInfo = new DataGroup("recordInfo");
		recordInfo.addChild(new DataAtomic("type", "place"));
		recordInfo.addChild(new DataAtomic("id", "place:0001"));

		/**
		 * <pre>
		 * 		recordInfo
		 * 			type
		 * 			id
		 * 			organisation
		 * 			user
		 * 			tsCreated (recordCreatedDate)
		 * 			list tsUpdated (recordUpdatedDate)
		 * 			catalog Language
		 * </pre>
		 */

		DataGroup dataGroup = new DataGroup("authority");
		dataGroup.addChild(recordInfo);

		posts.get("place").put("place:0001", dataGroup);

		PostRetrieverInMemoryStorage postsInMemory = new PostRetrieverInMemoryStorage(
				posts);
		return postsInMemory;
	}
}
