package se.uu.ub.cora.spider.extended.binary;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertTrue;

import java.util.List;

import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import se.uu.ub.cora.spider.dependency.SpiderDependencyProvider;
import se.uu.ub.cora.spider.extendedfunctionality.ExtendedFunctionality;
import se.uu.ub.cora.spider.extendedfunctionality.ExtendedFunctionalityContext;
import se.uu.ub.cora.spider.extendedfunctionality.ExtendedFunctionalityFactory;
import se.uu.ub.cora.spider.extendedfunctionality.ExtendedFunctionalityPosition;
import se.uu.ub.cora.spider.spy.SpiderDependencyProviderSpy;

public class BinaryProtocolsExtendedFunctionalityFactoryTest {

	private ExtendedFunctionalityFactory factory;
	private SpiderDependencyProvider dependencyProvider;

	@BeforeMethod
	public void beforeMethod() {
		factory = new BinaryProtocolsExtendedFunctionalityFactory();
		dependencyProvider = new SpiderDependencyProviderSpy();

	}

	@Test
	public void testInitCreatesContexts() throws Exception {

		factory.initializeUsingDependencyProvider(dependencyProvider);

		List<ExtendedFunctionalityContext> extendedFunctionalityContexts = factory
				.getExtendedFunctionalityContexts();

		assertEquals(extendedFunctionalityContexts.size(), 1);

		ExtendedFunctionalityContext firstContext = extendedFunctionalityContexts.get(0);
		assertEquals(firstContext.position, ExtendedFunctionalityPosition.CREATE_BEFORE_ENHANCE);
		assertEquals(firstContext.recordType, "binary");

	}

	@Test
	public void testFactor() throws Exception {
		List<ExtendedFunctionality> extendedFunctionalities = factory.factor(null, null);

		assertEquals(extendedFunctionalities.size(), 1);
		assertTrue(extendedFunctionalities.get(0) instanceof BinaryProtocolsExtendedFunctionality);

	}
}
