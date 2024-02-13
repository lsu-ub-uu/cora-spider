package se.uu.ub.cora.spider.extended.visibility;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertTrue;

import java.util.List;
import java.util.Optional;

import org.testng.Assert;
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
	public void testGetExtendedFunctionalityContextsForUpdate() {
		assertEquals(factory.getExtendedFunctionalityContexts().size(), 2);
		assertContext(ExtendedFunctionalityPosition.UPDATE_BEFORE_STORE);
	}

	@Test
	public void testGetExtendedFunctionalityContextsForCreate() {
		assertContext(ExtendedFunctionalityPosition.CREATE_AFTER_METADATA_VALIDATION);
	}

	@Test
	public void testFactorUpdate() throws Exception {
		List<ExtendedFunctionality> functionalities = factory
				.factor(ExtendedFunctionalityPosition.UPDATE_BEFORE_STORE, "binary");
		assertTrue(functionalities.get(0) instanceof VisibilityExtendedFunctionality);
	}

	@Test
	public void testFactorCreate() throws Exception {
		List<ExtendedFunctionality> functionalities = factory
				.factor(ExtendedFunctionalityPosition.CREATE_AFTER_METADATA_VALIDATION, "binary");
		assertTrue(functionalities.get(0) instanceof VisibilityExtendedFunctionality);
	}

	private void assertContext(ExtendedFunctionalityPosition position) {
		ExtendedFunctionalityContext visibilityExtFunc = assertContextExistsAndReturnIt(position);
		assertEquals(visibilityExtFunc.recordType, "binary");
		assertEquals(visibilityExtFunc.position, position);
		assertEquals(visibilityExtFunc.runAsNumber, 0);
	}

	private ExtendedFunctionalityContext assertContextExistsAndReturnIt(
			ExtendedFunctionalityPosition position) {
		Optional<ExtendedFunctionalityContext> visibilityExtFuncOpt = factory
				.getExtendedFunctionalityContexts().stream()
				.filter(contexts -> contexts.position.equals(position)).findFirst();

		if (!visibilityExtFuncOpt.isPresent()) {
			Assert.fail("Failed find a matching context");
		}

		return visibilityExtFuncOpt.get();
	}
}
