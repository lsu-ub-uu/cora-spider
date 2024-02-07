package se.uu.ub.cora.spider.extended.visibility;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertTrue;

import java.util.List;

import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import se.uu.ub.cora.spider.dependency.SpiderDependencyProvider;
import se.uu.ub.cora.spider.dependency.spy.SpiderDependencyProviderOldSpy;
import se.uu.ub.cora.spider.extendedfunctionality.ExtendedFunctionality;
import se.uu.ub.cora.spider.extendedfunctionality.ExtendedFunctionalityContext;
import se.uu.ub.cora.spider.extendedfunctionality.ExtendedFunctionalityFactory;
import se.uu.ub.cora.spider.extendedfunctionality.ExtendedFunctionalityPosition;

public class VisibilityExtendedFunctionalityFactoryTest {

	private ExtendedFunctionalityFactory factory;
	private SpiderDependencyProvider dependencyProvider;

	@BeforeMethod
	public void beforeMethod() throws Exception {
		factory = new VisibilityExtendedFunctionalityFactory();
		dependencyProvider = new SpiderDependencyProviderOldSpy();
		factory.initializeUsingDependencyProvider(dependencyProvider);
	}

	@Test
	public void testInit() throws Exception {
		assertTrue(factory instanceof VisibilityExtendedFunctionalityFactory);
	}

	@Test
	public void testGetExtendedFunctionalityContexts() {
		assertEquals(factory.getExtendedFunctionalityContexts().size(), 1);
		assertContext();
	}

	@Test
	public void testFactor() throws Exception {
		List<ExtendedFunctionality> functionalities = factory
				.factor(ExtendedFunctionalityPosition.UPDATE_BEFORE_STORE, "binary");
		assertTrue(functionalities.get(0) instanceof VisibilityExtendedFunctionality);
	}

	private void assertContext() {
		ExtendedFunctionalityContext visibilityExtFunc = factory.getExtendedFunctionalityContexts()
				.get(0);
		assertEquals(visibilityExtFunc.recordType, "binary");
		assertEquals(visibilityExtFunc.position, ExtendedFunctionalityPosition.UPDATE_BEFORE_STORE);
		assertEquals(visibilityExtFunc.runAsNumber, 0);
	}
}
