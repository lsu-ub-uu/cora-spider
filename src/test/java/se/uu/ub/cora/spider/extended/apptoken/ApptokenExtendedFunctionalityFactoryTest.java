/*
 * Copyright 2020 Uppsala University Library
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
import static se.uu.ub.cora.spider.extendedfunctionality.ExtendedFunctionalityPosition.CREATE_AFTER_AUTHORIZATION;

import java.util.List;

import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import se.uu.ub.cora.spider.dependency.SpiderDependencyProvider;
import se.uu.ub.cora.spider.dependency.spy.SpiderDependencyProviderOldSpy;
import se.uu.ub.cora.spider.extendedfunctionality.ExtendedFunctionality;
import se.uu.ub.cora.spider.extendedfunctionality.ExtendedFunctionalityContext;
import se.uu.ub.cora.spider.extendedfunctionality.ExtendedFunctionalityFactory;
import se.uu.ub.cora.spider.extendedfunctionality.ExtendedFunctionalityPosition;

public class ApptokenExtendedFunctionalityFactoryTest {

	private ExtendedFunctionalityFactory factory;
	private SpiderDependencyProvider dependencyProviderSpy;

	@BeforeMethod
	public void setUp() {
		factory = new ApptokenExtendedFunctionalityFactory();
		dependencyProviderSpy = new SpiderDependencyProviderOldSpy();
		factory.initializeUsingDependencyProvider(dependencyProviderSpy);
	}

	@Test
	public void testGetExtendedFunctionalityContexts() {
		assertEquals(factory.getExtendedFunctionalityContexts().size(), 2);
		assertCorrectContextUsingIndexNumberAndPosition(0, 0, "appToken",
				CREATE_AFTER_AUTHORIZATION);
		assertCorrectContextUsingIndexNumberAndPosition(1, 0, "appToken",
				ExtendedFunctionalityPosition.CREATE_BEFORE_ENHANCE);
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
	public void testCreateBeforeValidationForApptoken() {
		List<ExtendedFunctionality> functionalities = factory.factor(CREATE_AFTER_AUTHORIZATION,
				"appToken");
		assertTrue(functionalities.get(0) instanceof AppTokenEnhancer);
	}

	@Test
	public void testCreateBeforeReturnForApptoken() {
		List<ExtendedFunctionality> functionalities = factory
				.factor(ExtendedFunctionalityPosition.CREATE_BEFORE_ENHANCE, "appToken");
		UserUpdaterForAppToken extendedFunctionality = (UserUpdaterForAppToken) functionalities
				.get(0);

		assertSame(extendedFunctionality.onlyForTestGetDependencyProvider(), dependencyProviderSpy);
		assertTrue(extendedFunctionality instanceof UserUpdaterForAppToken);
	}

}
