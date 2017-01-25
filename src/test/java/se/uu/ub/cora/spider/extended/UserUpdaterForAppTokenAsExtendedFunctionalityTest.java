package se.uu.ub.cora.spider.extended;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertNotNull;

import java.util.HashMap;

import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import se.uu.ub.cora.bookkeeper.linkcollector.DataRecordLinkCollector;
import se.uu.ub.cora.bookkeeper.validator.DataValidator;
import se.uu.ub.cora.spider.authentication.Authenticator;
import se.uu.ub.cora.spider.authentication.AuthenticatorSpy;
import se.uu.ub.cora.spider.authorization.PermissionRuleCalculator;
import se.uu.ub.cora.spider.authorization.SpiderAuthorizator;
import se.uu.ub.cora.spider.data.SpiderDataAtomic;
import se.uu.ub.cora.spider.data.SpiderDataGroup;
import se.uu.ub.cora.spider.dependency.SpiderDependencyProviderSpy;
import se.uu.ub.cora.spider.dependency.SpiderInstanceFactorySpy2;
import se.uu.ub.cora.spider.dependency.SpiderInstanceProvider;
import se.uu.ub.cora.spider.record.SpiderRecordUpdaterSpy;
import se.uu.ub.cora.spider.record.storage.RecordIdGenerator;
import se.uu.ub.cora.spider.record.storage.RecordStorage;
import se.uu.ub.cora.spider.testdata.SpiderDataCreator;
import se.uu.ub.cora.spider.testdata.TestDataAppTokenStorage;

public class UserUpdaterForAppTokenAsExtendedFunctionalityTest {

	private UserUpdaterForAppTokenAsExtendedFunctionality extendedFunctionality;

	private RecordStorage recordStorage;
	private Authenticator authenticator;
	private SpiderAuthorizator spiderAuthorizator;
	private RecordIdGenerator idGenerator;
	private PermissionRuleCalculator ruleCalculator;
	private DataValidator dataValidator;
	private DataRecordLinkCollector linkCollector;
	private SpiderDependencyProviderSpy dependencyProvider;
	private ExtendedFunctionalityProviderSpy extendedFunctionalityProvider;

	private SpiderInstanceFactorySpy2 spiderInstanceFactory;

	@BeforeMethod
	public void setUp() {
		spiderInstanceFactory = new SpiderInstanceFactorySpy2();
		SpiderInstanceProvider.setSpiderInstanceFactory(spiderInstanceFactory);

		dependencyProvider = new SpiderDependencyProviderSpy(null);
		authenticator = new AuthenticatorSpy();
		// spiderAuthorizator = new AuthorizatorAlwaysAuthorizedSpy();
		// dataValidator = new DataValidatorAlwaysValidSpy();
		recordStorage = TestDataAppTokenStorage.createRecordStorageInMemoryWithTestData();
		// idGenerator = new TimeStampIdGenerator();
		// ruleCalculator = new NoRulesCalculatorStub();
		// linkCollector = new DataRecordLinkCollectorSpy();
		// extendedFunctionalityProvider = new
		// ExtendedFunctionalityProviderSpy();
		setUpDependencyProvider();
		extendedFunctionality = UserUpdaterForAppTokenAsExtendedFunctionality
				.usingSpiderDependencyProvider(dependencyProvider);
	}

	private void setUpDependencyProvider() {
		dependencyProvider = new SpiderDependencyProviderSpy(new HashMap<>());
		dependencyProvider.authenticator = authenticator;
		dependencyProvider.spiderAuthorizator = spiderAuthorizator;
		dependencyProvider.dataValidator = dataValidator;
		dependencyProvider.recordStorage = recordStorage;
		dependencyProvider.keyCalculator = ruleCalculator;
		dependencyProvider.linkCollector = linkCollector;
		dependencyProvider.idGenerator = idGenerator;
		dependencyProvider.extendedFunctionalityProvider = extendedFunctionalityProvider;
	}

	@Test
	public void init() {
		assertNotNull(extendedFunctionality);
	}

	@Test
	public void useExtendedFunctionality() {
		SpiderDataGroup minimalAppTokenGroup = SpiderDataGroup.withNameInData("appToken");
		minimalAppTokenGroup
				.addChild(SpiderDataCreator.createRecordInfoWithRecordTypeAndRecordIdAndDataDivider(
						"appToken", "someAppTokenId", "cora"));
		minimalAppTokenGroup
				.addChild(SpiderDataAtomic.withNameInDataAndValue("note", "my device!"));

		extendedFunctionality.useExtendedFunctionality("dummy1Token", minimalAppTokenGroup);
		SpiderRecordUpdaterSpy spiderRecordUpdaterSpy = spiderInstanceFactory.createdUpdaters
				.get(0);
		SpiderDataGroup updatedUserDataGroup = spiderRecordUpdaterSpy.record;
		SpiderDataGroup userAppTokenGroup = (SpiderDataGroup) updatedUserDataGroup
				.getFirstChildWithNameInData("userAppTokenGroup");
		assertEquals(userAppTokenGroup.extractAtomicValue("note"), "my device!");
		SpiderDataGroup apptokenLink = userAppTokenGroup.extractGroup("appTokenLink");
		assertEquals(apptokenLink.extractAtomicValue("linkedRecordType"), "appToken");
		assertEquals(apptokenLink.extractAtomicValue("linkedRecordId"), "someAppTokenId");
		assertNotNull(userAppTokenGroup.getRepeatId());
	}

}
