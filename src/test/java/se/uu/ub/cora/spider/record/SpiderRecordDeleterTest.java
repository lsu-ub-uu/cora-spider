package se.uu.ub.cora.spider.record;

import static org.testng.Assert.assertTrue;

import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import se.uu.ub.cora.beefeater.Authorizator;
import se.uu.ub.cora.beefeater.AuthorizatorImp;
import se.uu.ub.cora.spider.record.storage.RecordNotFoundException;
import se.uu.ub.cora.spider.record.storage.RecordStorage;
import se.uu.ub.cora.spider.testdata.TestDataRecordInMemoryStorage;

public class SpiderRecordDeleterTest {
	private RecordStorage recordStorage;
	private Authorizator authorization;
	private PermissionKeyCalculator keyCalculator;
	private SpiderRecordDeleter recordDeleter;

	@BeforeMethod
	public void beforeMethod() {
		authorization = new AuthorizatorImp();
		recordStorage = TestDataRecordInMemoryStorage.createRecordStorageInMemoryWithTestData();
		keyCalculator = new RecordPermissionKeyCalculator();
		recordDeleter = SpiderRecordDeleterImp.usingAuthorizationAndRecordStorageAndKeyCalculator(
				authorization, recordStorage, keyCalculator);

	}

	@Test
	public void testDeleteAuthorized() {
		RecordStorageSpy recordStorage = new RecordStorageSpy();
		recordDeleter = SpiderRecordDeleterImp.usingAuthorizationAndRecordStorageAndKeyCalculator(
				authorization, recordStorage, keyCalculator);
		recordDeleter.deleteRecord("userId", "child1", "place:0001");
		assertTrue(recordStorage.deleteWasCalled);
	}

	@Test(expectedExceptions = AuthorizationException.class)
	public void testDeleteUnauthorized() {
		recordDeleter.deleteRecord("unauthorizedUserId", "place", "place:0001");
	}

	@Test(expectedExceptions = RecordNotFoundException.class)
	public void testDeleteNotFound() {
		recordDeleter.deleteRecord("userId", "place", "place:0001_NOT_FOUND");
	}

	@Test(expectedExceptions = MisuseException.class)
	public void testDeleteRecordAbstractRecordType() {
		SpiderRecordDeleter recordDeleter = SpiderRecordDeleterImp
				.usingAuthorizationAndRecordStorageAndKeyCalculator(authorization,
						new RecordStorageSpy(), keyCalculator);
		recordDeleter.deleteRecord("userId", "abstract", "xxx");
	}

	@Test(expectedExceptions = RecordNotFoundException.class)
	public void testReadingDataForANonExistingRecordType() {
		recordDeleter.deleteRecord("userId", "nonExistingRecordType", "anId");
	}
}
