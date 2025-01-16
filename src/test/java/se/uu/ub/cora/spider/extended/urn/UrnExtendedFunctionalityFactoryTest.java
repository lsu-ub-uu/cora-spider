package se.uu.ub.cora.spider.extended.urn;

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

public class UrnExtendedFunctionalityFactoryTest {

	private ExtendedFunctionalityFactory factory;
	private SpiderDependencyProvider dependencyProvider;

	@BeforeMethod
	public void beforeMethod() throws Exception {
		factory = new UrnExtendedFunctionalityFactory();
		dependencyProvider = new SpiderDependencyProviderOldSpy();
		factory.initializeUsingDependencyProvider(dependencyProvider);
	}

	@Test
	public void testInit() throws Exception {
		assertTrue(factory instanceof UrnExtendedFunctionalityFactory);
	}

	@Test
	public void testGetExtendedFunctionalityContextsForCreate() {
		assertContext(ExtendedFunctionalityPosition.CREATE_AFTER_METADATA_VALIDATION);
	}

	@Test
	public void testFactorUpdate() throws Exception {
		List<ExtendedFunctionality> functionalities = factory
				.factor(ExtendedFunctionalityPosition.UPDATE_BEFORE_STORE, "alvin-record");
		assertTrue(functionalities.get(0) instanceof UrnExtendedFunctionality);
	}

	@Test
	public void testFactorCreate() throws Exception {
		List<ExtendedFunctionality> functionalities = factory.factor(
				ExtendedFunctionalityPosition.CREATE_AFTER_METADATA_VALIDATION, "alvin-record");
		assertTrue(functionalities.get(0) instanceof UrnExtendedFunctionality);
	}

	private void assertContext(ExtendedFunctionalityPosition position) {
		ExtendedFunctionalityContext visibilityExtFunc = assertContextExistsAndReturnIt(position);
		assertEquals(visibilityExtFunc.recordType, "alvin-record");
		assertEquals(visibilityExtFunc.position, position);
		assertEquals(visibilityExtFunc.runAsNumber, 0);
	}

	private ExtendedFunctionalityContext assertContextExistsAndReturnIt(
			ExtendedFunctionalityPosition position) {
		var optionalExtFuncContext = tryToFindMatchingContext(position);

		if (!optionalExtFuncContext.isPresent()) {
			Assert.fail("Failed find a matching context");
		}

		return optionalExtFuncContext.get();
	}

	private Optional<ExtendedFunctionalityContext> tryToFindMatchingContext(
			ExtendedFunctionalityPosition position) {
		return factory.getExtendedFunctionalityContexts().stream()
				.filter(contexts -> contexts.position.equals(position)).findFirst();
	}
}
