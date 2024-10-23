/*
 * Copyright 2024 Uppsala University Library
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
package se.uu.ub.cora.spider.extended.binary;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertSame;
import static org.testng.Assert.assertTrue;
import static se.uu.ub.cora.spider.extendedfunctionality.ExtendedFunctionalityPosition.DELETE_AFTER;
import static se.uu.ub.cora.spider.extendedfunctionality.ExtendedFunctionalityPosition.READ_BEFORE_RETURN;
import static se.uu.ub.cora.spider.extendedfunctionality.ExtendedFunctionalityPosition.UPDATE_BEFORE_RETURN;

import java.util.List;

import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import se.uu.ub.cora.spider.dependency.SpiderDependencyProvider;
import se.uu.ub.cora.spider.extendedfunctionality.ExtendedFunctionality;
import se.uu.ub.cora.spider.extendedfunctionality.ExtendedFunctionalityContext;
import se.uu.ub.cora.spider.extendedfunctionality.ExtendedFunctionalityFactory;
import se.uu.ub.cora.spider.spy.SpiderDependencyProviderSpy;

public class BinaryExtendedFunctionalityFactoryTest {

	private ExtendedFunctionalityFactory factory;
	private SpiderDependencyProvider dependencyProvider;

	@BeforeMethod
	public void beforeMethod() {
		factory = new BinaryExtendedFunctionalityFactory();
		dependencyProvider = new SpiderDependencyProviderSpy();

	}

	@Test
	public void testInitCreatesContexts() throws Exception {

		factory.initializeUsingDependencyProvider(dependencyProvider);

		List<ExtendedFunctionalityContext> extendedFunctionalityContexts = factory
				.getExtendedFunctionalityContexts();

		assertEquals(extendedFunctionalityContexts.size(), 3);

		ExtendedFunctionalityContext firstContext = extendedFunctionalityContexts.get(0);
		assertEquals(firstContext.position, READ_BEFORE_RETURN);
		assertEquals(firstContext.recordType, "binary");

		ExtendedFunctionalityContext secondContext = extendedFunctionalityContexts.get(1);
		assertEquals(secondContext.position, UPDATE_BEFORE_RETURN);
		assertEquals(secondContext.recordType, "binary");

		ExtendedFunctionalityContext thirdContext = extendedFunctionalityContexts.get(2);
		assertEquals(thirdContext.position, DELETE_AFTER);
		assertEquals(thirdContext.recordType, "binary");

	}

	@Test
	public void testFactorBinaryExtendedFunctionality_READ_BEFORE_RETURN() throws Exception {
		List<ExtendedFunctionality> extendedFunctionalities = factory.factor(READ_BEFORE_RETURN,
				"someRecordType");

		assertEquals(extendedFunctionalities.size(), 1);
		assertTrue(extendedFunctionalities.get(0) instanceof BinaryProtocolsExtendedFunctionality);
	}

	@Test
	public void testFactorBinaryExtendedFunctionality_UPDATE_BEFORE_RETURN() throws Exception {
		List<ExtendedFunctionality> extendedFunctionalities = factory.factor(UPDATE_BEFORE_RETURN,
				"someRecordType");

		assertEquals(extendedFunctionalities.size(), 1);
		assertTrue(extendedFunctionalities.get(0) instanceof BinaryProtocolsExtendedFunctionality);
	}

	@Test
	public void testFactorBinaryExtendedFunctionality_DELETE_AFTER() throws Exception {
		factory.initializeUsingDependencyProvider(dependencyProvider);

		List<ExtendedFunctionality> extendedFunctionalities = factory.factor(DELETE_AFTER,
				"someRecordType");

		assertEquals(extendedFunctionalities.size(), 1);
		ExtendedFunctionality extendedFunctionality = extendedFunctionalities.get(0);
		assertTrue(extendedFunctionality instanceof DeleteStreamsExtendedFunctionality);
		assertSame(((DeleteStreamsExtendedFunctionality) extendedFunctionality)
				.onlyForTestGetDependencyProvider(), dependencyProvider);
	}
}
