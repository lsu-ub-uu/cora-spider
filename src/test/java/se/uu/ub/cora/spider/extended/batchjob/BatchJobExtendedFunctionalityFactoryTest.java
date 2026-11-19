/*
 * Copyright 2026 Uppsala University Library
 *
 * This file is part of Cora.
 *
 *     Cora is free software: you can redistribute it and/or modify
 *     it under the terms of the GNU General Public License as published by
 *     the Free Software Foundation, either version 3 of the License, or
 *     (at your option) any later version.
 *
 *     Cora is distributed in the hope that it will be useful,
 *     but WITHOUT ANY WARRANTY; without even the implied warranty of
 *     MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *     GNU General Public License for more details.
 *
 *     You should have received a copy of the GNU General Public License
 *     along with Cora.  If not, see <http://www.gnu.org/licenses/>.
 */
package se.uu.ub.cora.spider.extended.batchjob;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertSame;
import static org.testng.Assert.assertTrue;
import static se.uu.ub.cora.spider.extendedfunctionality.ExtendedFunctionalityPosition.CREATE_AFTER_METADATA_VALIDATION;
import static se.uu.ub.cora.spider.extendedfunctionality.ExtendedFunctionalityPosition.CREATE_BEFORE_ENHANCE;
import static se.uu.ub.cora.spider.extendedfunctionality.ExtendedFunctionalityPosition.CREATE_BEFORE_STORE;

import java.util.List;
import java.util.Optional;

import org.testng.Assert;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import se.uu.ub.cora.spider.dependency.SpiderDependencyProvider;
import se.uu.ub.cora.spider.extendedfunctionality.ExtendedFunctionality;
import se.uu.ub.cora.spider.extendedfunctionality.ExtendedFunctionalityContext;
import se.uu.ub.cora.spider.extendedfunctionality.ExtendedFunctionalityFactory;
import se.uu.ub.cora.spider.extendedfunctionality.ExtendedFunctionalityPosition;
import se.uu.ub.cora.spider.index.internal.BatchRunnerFactoryImp;
import se.uu.ub.cora.spider.index.internal.IndexBatchHandlerImp;
import se.uu.ub.cora.spider.spy.SpiderDependencyProviderSpy;

public class BatchJobExtendedFunctionalityFactoryTest {

	private ExtendedFunctionalityFactory factory;
	private SpiderDependencyProvider dependencyProvider;

	@BeforeMethod
	public void beforeMethod() {
		factory = new BatchJobExtendedFunctionalityFactory();
		dependencyProvider = new SpiderDependencyProviderSpy();
		factory.initializeUsingDependencyProvider(dependencyProvider);
	}

	@Test
	public void testInit() {
		assertTrue(factory instanceof BatchJobExtendedFunctionalityFactory);
	}

	@Test
	public void testGetExtendedFunctionalityContextsForCreate() {
		ExtendedFunctionalityContext extFunc = assertContextExistsAndReturnIt(
				CREATE_AFTER_METADATA_VALIDATION);
		assertEquals(extFunc.recordType, "indexBatchJob");
		assertEquals(extFunc.position, CREATE_AFTER_METADATA_VALIDATION);
		assertEquals(extFunc.runAsNumber, 0);

		ExtendedFunctionalityContext extFunc2 = assertContextExistsAndReturnIt(CREATE_BEFORE_STORE);
		assertEquals(extFunc2.recordType, "indexBatchJob");
		assertEquals(extFunc2.position, CREATE_BEFORE_STORE);
		assertEquals(extFunc2.runAsNumber, 0);

		ExtendedFunctionalityContext extFunc3 = assertContextExistsAndReturnIt(
				CREATE_BEFORE_ENHANCE);
		assertEquals(extFunc3.recordType, "indexBatchJob");
		assertEquals(extFunc3.position, CREATE_BEFORE_ENHANCE);
		assertEquals(extFunc3.runAsNumber, 0);
	}

	@Test
	public void testFactorCreateAuthorization() {
		List<ExtendedFunctionality> functionalities = factory
				.factor(CREATE_AFTER_METADATA_VALIDATION, "indexBatchJob");
		assertTrue(functionalities.get(0) instanceof BatchJobCheckAuthorizedExtendedFunctionality);
	}

	@Test
	public void testFactorDependenciesAuthorization() {
		List<ExtendedFunctionality> functionalities = factory
				.factor(CREATE_AFTER_METADATA_VALIDATION, "indexBatchJob");
		BatchJobCheckAuthorizedExtendedFunctionality extFunctionality = (BatchJobCheckAuthorizedExtendedFunctionality) functionalities
				.get(0);

		SpiderDependencyProvider dependencyProvider2 = extFunctionality
				.onlyForTestGetDependencyProvider();
		assertSame(dependencyProvider2, dependencyProvider);
	}

	@Test
	public void testFactorCreateCalculateNumberToIndex() {
		List<ExtendedFunctionality> functionalities = factory.factor(CREATE_BEFORE_STORE,
				"indexBatchJob");
		assertTrue(functionalities.get(0) instanceof CalculateNumberToIndexExtendedFunctionality);
	}

	@Test
	public void testFactorDependenciesCalculateNumberToIndex() {
		List<ExtendedFunctionality> functionalities = factory.factor(CREATE_BEFORE_STORE,
				"indexBatchJob");
		CalculateNumberToIndexExtendedFunctionality extFunctionality = (CalculateNumberToIndexExtendedFunctionality) functionalities
				.get(0);

		SpiderDependencyProvider dependencyProvider2 = extFunctionality
				.onlyForTestGetDependencyProvider();
		assertSame(dependencyProvider2, dependencyProvider);
	}

	@Test
	public void testFactorCreateRunner() {
		List<ExtendedFunctionality> functionalities = factory.factor(CREATE_BEFORE_ENHANCE,
				"indexBatchJob");
		assertTrue(functionalities.get(0) instanceof RunBatchJobExtendedFunctionality);
	}

	@Test
	public void testFactorDependenciesRunner() {
		List<ExtendedFunctionality> functionalities = factory.factor(CREATE_BEFORE_ENHANCE,
				"indexBatchJob");
		RunBatchJobExtendedFunctionality extFunctionality = (RunBatchJobExtendedFunctionality) functionalities
				.get(0);
		IndexBatchHandlerImp indexBatchHandler = (IndexBatchHandlerImp) extFunctionality
				.onlyForTestGetIndexBatchHandler();

		assertSame(extFunctionality.onlyForTestGetDependencyProvider(), dependencyProvider);

		BatchRunnerFactoryImp batchRunnerFactory = (BatchRunnerFactoryImp) indexBatchHandler
				.getBatchRunnerFactory();
		SpiderDependencyProvider dependencyProvider2 = batchRunnerFactory.getDependencyProvider();
		assertSame(dependencyProvider2, dependencyProvider);
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
