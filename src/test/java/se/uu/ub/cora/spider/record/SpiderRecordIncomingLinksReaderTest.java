package se.uu.ub.cora.spider.record;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertTrue;

import java.util.HashMap;

import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import se.uu.ub.cora.spider.authentication.AuthenticationException;
import se.uu.ub.cora.spider.authentication.Authenticator;
import se.uu.ub.cora.spider.authentication.AuthenticatorSpy;
import se.uu.ub.cora.spider.authorization.AuthorizationException;
import se.uu.ub.cora.spider.authorization.NeverAuthorisedStub;
import se.uu.ub.cora.spider.authorization.PermissionRuleCalculator;
import se.uu.ub.cora.spider.authorization.SpiderAuthorizator;
import se.uu.ub.cora.spider.data.Action;
import se.uu.ub.cora.spider.data.SpiderDataAtomic;
import se.uu.ub.cora.spider.data.SpiderDataGroup;
import se.uu.ub.cora.spider.data.SpiderDataList;
import se.uu.ub.cora.spider.data.SpiderDataRecordLink;
import se.uu.ub.cora.spider.dependency.SpiderDependencyProviderSpy;
import se.uu.ub.cora.spider.record.storage.RecordStorage;
import se.uu.ub.cora.spider.spy.AuthorizatorAlwaysAuthorizedSpy;
import se.uu.ub.cora.spider.spy.NoRulesCalculatorStub;
import se.uu.ub.cora.spider.spy.RecordStorageSpy;
import se.uu.ub.cora.spider.testdata.TestDataRecordInMemoryStorage;

public class SpiderRecordIncomingLinksReaderTest {

	private SpiderRecordIncomingLinksReader incomingLinksReader;

	private RecordStorage recordStorage;
	private Authenticator authenticator;
	private SpiderAuthorizator authorizator;
	private PermissionRuleCalculator keyCalculator;
	private SpiderDependencyProviderSpy dependencyProvider;

	@BeforeMethod
	public void beforeMethod() {
		authenticator = new AuthenticatorSpy();
		authorizator = new AuthorizatorAlwaysAuthorizedSpy();
		recordStorage = TestDataRecordInMemoryStorage.createRecordStorageInMemoryWithTestData();
		keyCalculator = new NoRulesCalculatorStub();
		setUpDependencyProvider();
	}

	private void setUpDependencyProvider() {
		dependencyProvider = new SpiderDependencyProviderSpy(new HashMap<>());
		dependencyProvider.authenticator = authenticator;
		dependencyProvider.spiderAuthorizator = authorizator;
		dependencyProvider.recordStorage = recordStorage;
		dependencyProvider.keyCalculator = keyCalculator;

		incomingLinksReader = SpiderRecordIncomingLinksReaderImp
				.usingDependencyProvider(dependencyProvider);
	}

	@Test
	public void testReadIncomingLinks() {
		SpiderDataList linksPointingToRecord = incomingLinksReader
				.readIncomingLinks("someToken78678567", "place", "place:0001");
		assertEquals(linksPointingToRecord.getTotalNumberOfTypeInStorage(), "1");
		assertEquals(linksPointingToRecord.getFromNo(), "1");
		assertEquals(linksPointingToRecord.getToNo(), "1");

		SpiderDataGroup link = (SpiderDataGroup) linksPointingToRecord.getDataList().iterator()
				.next();
		assertEquals(link.getNameInData(), "recordToRecordLink");

		SpiderDataRecordLink from = (SpiderDataRecordLink) link.getFirstChildWithNameInData("from");
		SpiderDataAtomic linkedRecordType = (SpiderDataAtomic) from
				.getFirstChildWithNameInData("linkedRecordType");
		SpiderDataAtomic linkedRecordId = (SpiderDataAtomic) from
				.getFirstChildWithNameInData("linkedRecordId");

		assertEquals(linkedRecordType.getValue(), "place");
		assertEquals(linkedRecordId.getValue(), "place:0002");
		assertEquals(from.getActions().size(), 1);
		assertTrue(from.getActions().contains(Action.READ));

		SpiderDataRecordLink to = (SpiderDataRecordLink) link.getFirstChildWithNameInData("to");
		SpiderDataAtomic toLinkedRecordType = (SpiderDataAtomic) to
				.getFirstChildWithNameInData("linkedRecordType");
		SpiderDataAtomic toLinkedRecordId = (SpiderDataAtomic) to
				.getFirstChildWithNameInData("linkedRecordId");

		assertEquals(toLinkedRecordType.getValue(), "place");
		assertEquals(toLinkedRecordId.getValue(), "place:0001");

	}

	@Test
	public void testReadIncomingLinksWhenLinkPointsToParentRecordType() {
		SpiderDataList linksPointingToRecord = incomingLinksReader
				.readIncomingLinks("someToken78678567", "place", "place:0003");
		assertEquals(linksPointingToRecord.getTotalNumberOfTypeInStorage(), "1");
		assertEquals(linksPointingToRecord.getFromNo(), "1");
		assertEquals(linksPointingToRecord.getToNo(), "1");

		SpiderDataGroup link = (SpiderDataGroup) linksPointingToRecord.getDataList().iterator()
				.next();
		assertEquals(link.getNameInData(), "recordToRecordLink");

		SpiderDataRecordLink from = (SpiderDataRecordLink) link.getFirstChildWithNameInData("from");
		SpiderDataAtomic linkedRecordType = (SpiderDataAtomic) from
				.getFirstChildWithNameInData("linkedRecordType");
		SpiderDataAtomic linkedRecordId = (SpiderDataAtomic) from
				.getFirstChildWithNameInData("linkedRecordId");

		assertEquals(linkedRecordType.getValue(), "place");
		assertEquals(linkedRecordId.getValue(), "place:0004");
		assertEquals(from.getActions().size(), 1);
		assertTrue(from.getActions().contains(Action.READ));

		SpiderDataRecordLink to = (SpiderDataRecordLink) link.getFirstChildWithNameInData("to");
		SpiderDataAtomic toLinkedRecordType = (SpiderDataAtomic) to
				.getFirstChildWithNameInData("linkedRecordType");
		SpiderDataAtomic toLinkedRecordId = (SpiderDataAtomic) to
				.getFirstChildWithNameInData("linkedRecordId");

		assertEquals(toLinkedRecordType.getValue(), "authority");
		assertEquals(toLinkedRecordId.getValue(), "place:0003");

	}

	@Test
	public void testReadIncomingLinksNoParentRecordTypeNoLinks() {
		SpiderDataList linksPointingToRecord = incomingLinksReader
				.readIncomingLinks("someToken78678567", "search", "aSearchId");
		assertEquals(linksPointingToRecord.getTotalNumberOfTypeInStorage(), "0");
		assertEquals(linksPointingToRecord.getFromNo(), "1");
		assertEquals(linksPointingToRecord.getToNo(), "0");

	}

	@Test(expectedExceptions = AuthenticationException.class)
	public void testReadIncomingLinksAuthenticationNotAuthenticated() {
		recordStorage = new RecordStorageSpy();
		setUpDependencyProvider();
		incomingLinksReader.readIncomingLinks("dummyNonAuthenticatedToken", "place", "place:0001");
	}

	@Test(expectedExceptions = AuthorizationException.class)
	public void testReadIncomingLinksUnauthorized() {
		authorizator = new NeverAuthorisedStub();
		setUpDependencyProvider();
		incomingLinksReader.readIncomingLinks("unauthorizedUserId", "place", "place:0001");
	}

	@Test(expectedExceptions = MisuseException.class)
	public void testReadIncomingLinksAbstractType() {
		recordStorage = new RecordStorageSpy();
		setUpDependencyProvider();

		incomingLinksReader.readIncomingLinks("someToken78678567", "abstract", "place:0001");
	}
}
