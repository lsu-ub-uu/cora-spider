/*
 * Copyright 2022, 2024 Uppsala University Library
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
package se.uu.ub.cora.spider.extended.password;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertTrue;
import static se.uu.ub.cora.spider.extendedfunctionality.ExtendedFunctionalityPosition.UPDATE_AFTER_STORE;
import static se.uu.ub.cora.spider.extendedfunctionality.ExtendedFunctionalityPosition.UPDATE_BEFORE_STORE;

import java.util.List;

import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import se.uu.ub.cora.password.texthasher.TextHasher;
import se.uu.ub.cora.password.texthasher.TextHasherFactory;
import se.uu.ub.cora.password.texthasher.TextHasherFactoryImp;
import se.uu.ub.cora.spider.extendedfunctionality.ExtendedFunctionality;
import se.uu.ub.cora.spider.extendedfunctionality.ExtendedFunctionalityContext;
import se.uu.ub.cora.spider.spy.SpiderDependencyProviderSpy;

public class PasswordExtendedFunctionalityFactoryTest {
	private PasswordExtendedFunctionalityFactory factory;
	private SpiderDependencyProviderSpy dependencyProvider;
	private TextHasherFactorySpy textHasherFactorySpy;
	private PasswordExtendedFunctionalityFactoryOnlyForTest onlyForTestFactory;

	private static final String USER_RECORD_TYPE = "user";

	@BeforeMethod
	public void beforeMethod() {
		factory = new PasswordExtendedFunctionalityFactory();
		onlyForTestFactory = new PasswordExtendedFunctionalityFactoryOnlyForTest();

		dependencyProvider = new SpiderDependencyProviderSpy();
		textHasherFactorySpy = new TextHasherFactorySpy();
	}

	class PasswordExtendedFunctionalityFactoryOnlyForTest
			extends PasswordExtendedFunctionalityFactory {
		TextHasherFactory onlyForTestGetTextHasherFactory() {
			return textHasherFactory;
		}

		void onlyForTestSetTextHasherFactory(TextHasherFactory textHasherFactory) {
			this.textHasherFactory = textHasherFactory;
		}
	}

	@Test
	public void testInitCreatesContextsForUserRecordType() throws Exception {
		factory.initializeUsingDependencyProvider(dependencyProvider);
		List<ExtendedFunctionalityContext> extendedFunctionalityContexts = factory
				.getExtendedFunctionalityContexts();

		assertEquals(extendedFunctionalityContexts.size(), 2);
		ExtendedFunctionalityContext firstContext = extendedFunctionalityContexts.get(0);
		assertEquals(firstContext.position, UPDATE_BEFORE_STORE);
		assertEquals(firstContext.recordType, USER_RECORD_TYPE);
		ExtendedFunctionalityContext secondContext = extendedFunctionalityContexts.get(1);
		assertEquals(secondContext.position, UPDATE_AFTER_STORE);
		assertEquals(secondContext.recordType, USER_RECORD_TYPE);
	}

	@Test
	public void testFactoryHasADefaultTextHasherFactory() throws Exception {
		assertTrue(onlyForTestFactory
				.onlyForTestGetTextHasherFactory() instanceof TextHasherFactoryImp);
	}

	@Test
	public void testFactorSetDependencyProviderAndCreatedTextHasher() throws Exception {
		onlyForTestFactory.initializeUsingDependencyProvider(dependencyProvider);
		onlyForTestFactory.onlyForTestSetTextHasherFactory(textHasherFactorySpy);

		List<ExtendedFunctionality> extendedFunctionalities = onlyForTestFactory
				.factor(UPDATE_BEFORE_STORE, USER_RECORD_TYPE);

		assertEquals(extendedFunctionalities.size(), 1);
		var extendedFunctionality = (PasswordExtendedFunctionality) extendedFunctionalities.get(0);

		assertEquals(extendedFunctionality.onlyForTestGetDependencyProvider(), dependencyProvider);
		assertTextHasherInstancePassedToExtendedFunctionality(extendedFunctionality);
	}

	private void assertTextHasherInstancePassedToExtendedFunctionality(
			PasswordExtendedFunctionality extendedFunctionality) {
		TextHasher textHasher = extendedFunctionality.onlyForTestGetTextHasher();
		textHasherFactorySpy.MCR.assertReturn("factor", 0, textHasher);
	}

	@Test
	public void testFactorForUpdateAfterStore() throws Exception {
		factory.initializeUsingDependencyProvider(dependencyProvider);
		List<ExtendedFunctionality> extendedFunctionalities = factory.factor(UPDATE_AFTER_STORE,
				USER_RECORD_TYPE);

		assertEquals(extendedFunctionalities.size(), 1);
		var extendedFunctionality = (PasswordSystemSecretRemoverExtendedFunctionality) extendedFunctionalities
				.get(0);

		assertEquals(extendedFunctionality.onlyForTestGetDependencyProvider(), dependencyProvider);
	}
}
