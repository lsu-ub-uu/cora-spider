package epc.spider.postretriever;

import org.testng.Assert;
import org.testng.annotations.Test;

import epc.beefeater.AuthorizationInputBoundary;
import epc.beefeater.Authorizator;
import epc.metadataformat.data.DataGroup;
import epc.spider.postretriever.storage.PostRetrieverStorageGateway;
import epc.spider.testdata.TestDataPostRetrieverInMemoryStorage;

public class PostRetrieverTest {
	@Test
	public void testInit() {

		// PostRetrieverStorageGateway postStorage = new
		// PostRetrieverInMemoryStorage();
		PostRetrieverStorageGateway postStorage = TestDataPostRetrieverInMemoryStorage
				.createPostRetrieverInMemoryStorageWithTestData();
		
		AuthorizationInputBoundary authorization = new Authorizator();
		
		PostRetrieverInputBoundary postRetriever = new PostRetriever(authorization,
				postStorage);

		DataGroup post = postRetriever.retrievePost("userId","place", "place:0001");
		// recordInfo
		// id
		// post.getDataId()

		Assert.assertEquals(post.getDataId(), "authority",
				"postOut.getDataId should be authority");
	}
}
