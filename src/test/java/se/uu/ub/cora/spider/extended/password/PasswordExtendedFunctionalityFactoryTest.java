/*
 * Copyright 2022 Uppsala University Library
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

import java.util.List;

import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import se.uu.ub.cora.data.DataProvider;
import se.uu.ub.cora.data.spies.DataFactorySpy;
import se.uu.ub.cora.password.texthasher.TextHasher;
import se.uu.ub.cora.password.texthasher.TextHasherFactory;
import se.uu.ub.cora.password.texthasher.TextHasherFactoryImp;
import se.uu.ub.cora.spider.dependency.spy.RecordTypeHandlerSpy;
import se.uu.ub.cora.spider.dependency.spy.SpiderDependencyProviderOldSpy;
import se.uu.ub.cora.spider.extendedfunctionality.ExtendedFunctionality;
import se.uu.ub.cora.spider.extendedfunctionality.ExtendedFunctionalityContext;
import se.uu.ub.cora.spider.extendedfunctionality.ExtendedFunctionalityPosition;

public class PasswordExtendedFunctionalityFactoryTest {
	PasswordExtendedFunctionalityFactory factory;
	SpiderDependencyProviderOldSpy dependencyProvider;
	private TextHasherFactorySpy textHasherFactorySpy;
	private DataFactorySpy dataFactorySpy;

	@BeforeMethod
	public void beforeMethod() {
		dataFactorySpy = new DataFactorySpy();
		DataProvider.onlyForTestSetDataFactory(dataFactorySpy);
		factory = new PasswordExtendedFunctionalityFactory();
		dependencyProvider = new SpiderDependencyProviderOldSpy(null);
		RecordTypeHandlerSpy recordTypeHandlerSpy = new RecordTypeHandlerSpy();
		dependencyProvider.mapOfRecordTypeHandlerSpies.put("user", recordTypeHandlerSpy);
		recordTypeHandlerSpy.listOfimplementingTypesIds.add("coraUser");
		recordTypeHandlerSpy.listOfimplementingTypesIds.add("otherUser");
		factory.initializeUsingDependencyProvider(dependencyProvider);
		textHasherFactorySpy = new TextHasherFactorySpy();
	}

	@Test
	public void testInitCreatesContextsForAllKnownImplementationsOfUser() throws Exception {
		dependencyProvider.MCR.assertParameters("getRecordTypeHandler", 0, "user");

		List<ExtendedFunctionalityContext> extendedFunctionalityContexts = factory
				.getExtendedFunctionalityContexts();

		ExtendedFunctionalityContext firstContext = extendedFunctionalityContexts.get(0);
		assertEquals(firstContext.position, ExtendedFunctionalityPosition.UPDATE_BEFORE_STORE);
		assertEquals(firstContext.recordType, "coraUser");

		ExtendedFunctionalityContext secondContext = extendedFunctionalityContexts.get(1);
		assertEquals(secondContext.position, ExtendedFunctionalityPosition.UPDATE_BEFORE_STORE);
		assertEquals(secondContext.recordType, "otherUser");
	}

	@Test
	public void testFactoryHasADefaultTextHasherFactory() throws Exception {
		PasswordExtendedFunctionalityFactoryOnlyForTest testFactor = new PasswordExtendedFunctionalityFactoryOnlyForTest();
		assertTrue(testFactor.onlyForTestGetTextHasherFactory() instanceof TextHasherFactoryImp);
	}

	@Test
	public void testFactorSetDependencyProviderAndCreatedTextHasher() throws Exception {
		PasswordExtendedFunctionalityFactoryOnlyForTest factory = new PasswordExtendedFunctionalityFactoryOnlyForTest();
		factory.initializeUsingDependencyProvider(dependencyProvider);
		factory.onlyForTestSetTextHasherFactory(textHasherFactorySpy);

		List<ExtendedFunctionality> extendedFunctionalities = factory.factor(null, null);

		assertEquals(extendedFunctionalities.size(), 1);
		PasswordExtendedFunctionality extendedFunctionality = (PasswordExtendedFunctionality) extendedFunctionalities
				.get(0);
		assertEquals(extendedFunctionality.onlyForTestGetDependencyProvider(), dependencyProvider);
		TextHasher hasherSpy1 = extendedFunctionality.onlyForTestGetTextHasher();
		textHasherFactorySpy.MCR.assertReturn("factor", 0, hasherSpy1);
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

}
