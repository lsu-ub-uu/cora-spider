package epc.spider.postretriever.storage;

import static org.testng.Assert.assertEquals;

import java.util.HashMap;
import java.util.Map;

import org.testng.annotations.Test;

import epc.metadataformat.data.DataAtomic;
import epc.metadataformat.data.DataGroup;

public class PostRetrieverInMemoryStorageTest {
	@Test
	public void testInitWithData() {
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

		DataGroup dataGroup = new DataGroup("dataId");
		dataGroup.addChild(recordInfo);

		posts.get("place").put("place:0001", dataGroup);

		PostRetrieverInMemoryStorage postsInMemory = new PostRetrieverInMemoryStorage(
				posts);
		assertEquals(postsInMemory.read("place", "place:0001"), dataGroup,
				"dataGroup should be the one added on startup");

	}

	@Test(expectedExceptions = IllegalArgumentException.class)
	public void testInitWithNull() {
		new PostRetrieverInMemoryStorage(null);

	}

	@Test
	public void testCreateRead() {

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

		DataGroup dataGroup = new DataGroup("dataId");
		dataGroup.addChild(recordInfo);

		PostRetrieverInMemoryStorage postsInMemory = new PostRetrieverInMemoryStorage();
		postsInMemory.create("type", "place:0001", dataGroup);
		DataGroup dataGroupOut = postsInMemory.read("type", "place:0001");
		assertEquals(dataGroupOut, dataGroup,
				"dataGroupOut should be the same as dataGroup");
	}
}
