package se.uu.ub.cora.spider.extended;

import static org.testng.Assert.assertNotEquals;
import static org.testng.Assert.assertNotNull;
import static org.testng.Assert.assertTrue;

import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import se.uu.ub.cora.spider.data.SpiderDataAtomic;
import se.uu.ub.cora.spider.data.SpiderDataGroup;

public class AppTokenEnhancerAsExtendedFunctionalityTest {

	private AppTokenEnhancerAsExtendedFunctionality extendedFunctionality;

	@BeforeMethod
	public void setUp() {
		extendedFunctionality = new AppTokenEnhancerAsExtendedFunctionality();
	}

	@Test
	public void useExtendedFunctionality() {
		assertNotNull(extendedFunctionality);
	}

	@Test
	private void generateAndAddAppToken() {
		SpiderDataGroup minimalGroup = SpiderDataGroup.withNameInData("appToken");
		extendedFunctionality.useExtendedFunctionality("someToken", minimalGroup);
		SpiderDataAtomic token = (SpiderDataAtomic) minimalGroup
				.getFirstChildWithNameInData("token");
		assertTrue(token.getValue().length() > 30);
	}

	@Test
	private void generateAndAddAppTokenDifferentTokens() {
		SpiderDataGroup minimalGroup = SpiderDataGroup.withNameInData("appToken");
		extendedFunctionality.useExtendedFunctionality("someToken", minimalGroup);
		SpiderDataAtomic token = (SpiderDataAtomic) minimalGroup
				.getFirstChildWithNameInData("token");

		SpiderDataGroup minimalGroup2 = SpiderDataGroup.withNameInData("appToken");
		extendedFunctionality.useExtendedFunctionality("someToken", minimalGroup2);
		SpiderDataAtomic token2 = (SpiderDataAtomic) minimalGroup2
				.getFirstChildWithNameInData("token");

		assertNotEquals(token.getValue(), token2.getValue());
	}
}
