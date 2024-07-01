/*
 * Copyright 2020, 2024 Uppsala University Library
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
package se.uu.ub.cora.spider.extended.apptoken;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertSame;
import static org.testng.Assert.assertTrue;
import static se.uu.ub.cora.spider.extendedfunctionality.ExtendedFunctionalityPosition.UPDATE_AFTER_METADATA_VALIDATION;
import static se.uu.ub.cora.spider.extendedfunctionality.ExtendedFunctionalityPosition.UPDATE_AFTER_STORE;

import java.util.List;

import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import se.uu.ub.cora.password.texthasher.TextHasherFactory;
import se.uu.ub.cora.password.texthasher.TextHasherFactoryImp;
import se.uu.ub.cora.spider.dependency.SpiderDependencyProvider;
import se.uu.ub.cora.spider.extended.password.TextHasherFactorySpy;
import se.uu.ub.cora.spider.extendedfunctionality.ExtendedFunctionality;
import se.uu.ub.cora.spider.extendedfunctionality.ExtendedFunctionalityContext;
import se.uu.ub.cora.spider.extendedfunctionality.ExtendedFunctionalityFactory;
import se.uu.ub.cora.spider.extendedfunctionality.ExtendedFunctionalityPosition;
import se.uu.ub.cora.spider.spy.SpiderDependencyProviderSpy;
import se.uu.ub.cora.spider.systemsecret.SystemSecretOperationsImp;

public class ApptokenExtendedFunctionalityFactoryTest {
	private static final String RECORD_TYPE_USER = "user";
	private ExtendedFunctionalityFactory factory;
	private SpiderDependencyProvider dependencyProviderSpy;
	private ApptokenExtendedFunctionalityFactoryOnlyForTest onlyForTestFactory;
	private TextHasherFactorySpy textHasherFactorySpy;

	@BeforeMethod
	public void beforeMethod() {
		factory = new ApptokenExtendedFunctionalityFactory();
		dependencyProviderSpy = new SpiderDependencyProviderSpy();
		factory.initializeUsingDependencyProvider(dependencyProviderSpy);

		onlyForTestFactory = new ApptokenExtendedFunctionalityFactoryOnlyForTest();
		onlyForTestFactory.initializeUsingDependencyProvider(dependencyProviderSpy);
		textHasherFactorySpy = new TextHasherFactorySpy();
	}

	class ApptokenExtendedFunctionalityFactoryOnlyForTest
			extends ApptokenExtendedFunctionalityFactory {
		TextHasherFactory onlyForTestGetTextHasherFactory() {
			return textHasherFactory;
		}

		void onlyForTestSetTextHasherFactory(TextHasherFactory textHasherFactory) {
			this.textHasherFactory = textHasherFactory;
		}
	}

	@Test
	public void testFactoryHasADefaultTextHasherFactory() throws Exception {
		assertTrue(onlyForTestFactory
				.onlyForTestGetTextHasherFactory() instanceof TextHasherFactoryImp);
	}

	@Test
	public void testGetExtendedFunctionalityContexts() {
		assertEquals(factory.getExtendedFunctionalityContexts().size(), 2);
		assertCorrectContextUsingIndexNumberAndPosition(0, 0, RECORD_TYPE_USER,
				UPDATE_AFTER_METADATA_VALIDATION);
		assertCorrectContextUsingIndexNumberAndPosition(1, 0, RECORD_TYPE_USER,
				ExtendedFunctionalityPosition.UPDATE_AFTER_STORE);
	}

	private void assertCorrectContextUsingIndexNumberAndPosition(int index, int runAsNumber,
			String recordType, ExtendedFunctionalityPosition position) {
		ExtendedFunctionalityContext workOrderEnhancer = factory.getExtendedFunctionalityContexts()
				.get(index);
		assertEquals(workOrderEnhancer.position, position);
		assertEquals(workOrderEnhancer.recordType, recordType);
		assertEquals(workOrderEnhancer.runAsNumber, runAsNumber);
	}

	@Test
	public void testUpdateAfterMetadataValidationForUser() {
		List<ExtendedFunctionality> functionalities = factory
				.factor(UPDATE_AFTER_METADATA_VALIDATION, RECORD_TYPE_USER);

		assertTrue(functionalities.get(0) instanceof AppTokenHandlerExtendedFunctionality);
	}

	@Test
	public void testDependenciesForAppTokenHandlerExtendedFunctionality() throws Exception {
		onlyForTestFactory.onlyForTestSetTextHasherFactory(textHasherFactorySpy);

		List<ExtendedFunctionality> functionalities = onlyForTestFactory
				.factor(UPDATE_AFTER_METADATA_VALIDATION, RECORD_TYPE_USER);
		AppTokenHandlerExtendedFunctionality extendedFunctionality = (AppTokenHandlerExtendedFunctionality) functionalities
				.get(0);
		assertTrue(extendedFunctionality
				.onlyForTestGetAppTokenGenerator() instanceof AppTokenGeneratorImp);

		SystemSecretOperationsImp systemSecretOperationsFromHandler = (SystemSecretOperationsImp) extendedFunctionality
				.onlyForTestGetSystemSecretOperations();
		assertSame(systemSecretOperationsFromHandler.onlyForTestGetDependencyProvider(),
				dependencyProviderSpy);
		textHasherFactorySpy.MCR.assertReturn("factor", 0,
				systemSecretOperationsFromHandler.onlyForTestGetTextHasher());
	}

	@Test
	public void testUpdataAfterStoreForUser() {
		List<ExtendedFunctionality> functionalities = factory.factor(UPDATE_AFTER_STORE,
				RECORD_TYPE_USER);

		assertTrue(functionalities.get(0) instanceof AppTokenClearTextExtendedFuncionality);
	}

}
