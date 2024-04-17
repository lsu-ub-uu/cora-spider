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
package se.uu.ub.cora.spider.extended.password;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertTrue;

import java.util.List;

import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import se.uu.ub.cora.spider.extendedfunctionality.ExtendedFunctionality;
import se.uu.ub.cora.spider.extendedfunctionality.ExtendedFunctionalityContext;
import se.uu.ub.cora.spider.extendedfunctionality.ExtendedFunctionalityFactory;
import se.uu.ub.cora.spider.extendedfunctionality.ExtendedFunctionalityPosition;

public class SystemSecretSecurityExtendedFunctionalityFactoryTest {

	private SystemSecretSecurityExtendedFunctionalityFactory factory;

	@BeforeMethod
	private void beforeMethod() {
		factory = new SystemSecretSecurityExtendedFunctionalityFactory();
	}

	@Test
	public void testExtensExtendedFunctionalityFactory() throws Exception {
		assertTrue(factory instanceof ExtendedFunctionalityFactory);
	}

	@Test
	public void testInitCreatesContextsForUserRecordType() throws Exception {
		factory.initializeUsingDependencyProvider(null);
		List<ExtendedFunctionalityContext> extendedFunctionalityContexts = factory
				.getExtendedFunctionalityContexts();

		assertEqualNumberOfContextsThanPositionsEndingWithAfterAuthorization(
				extendedFunctionalityContexts);
		assertContextUsingConextListAndListIndex(extendedFunctionalityContexts, 0);
	}

	private void assertContextUsingConextListAndListIndex(
			List<ExtendedFunctionalityContext> extendedFunctionalityContexts, int listIndex) {
		ExtendedFunctionalityContext firstContext = extendedFunctionalityContexts.get(listIndex);
		assertEquals(firstContext.position,
				ExtendedFunctionalityPosition.CREATE_AFTER_AUTHORIZATION);
		assertEquals(firstContext.recordType, "systemSecret");
	}

	private void assertEqualNumberOfContextsThanPositionsEndingWithAfterAuthorization(
			List<ExtendedFunctionalityContext> extendedFunctionalityContexts) {

		int numberOfPositionsEndingWithAfterAuthorization = getNumberOfPositionsEndingWithAfterAuthorization();
		assertEquals(extendedFunctionalityContexts.size(),
				numberOfPositionsEndingWithAfterAuthorization);
	}

	private int getNumberOfPositionsEndingWithAfterAuthorization() {
		int counter = 0;
		for (ExtendedFunctionalityPosition position : ExtendedFunctionalityPosition.values()) {
			counter = incrementCounterIfPositionEndsWithAfterAuthorization(counter, position);
		}
		return counter;
	}

	private int incrementCounterIfPositionEndsWithAfterAuthorization(int counter,
			ExtendedFunctionalityPosition position) {
		if (endsWithAfterAuthorization(position)) {
			counter++;
		}
		return counter;
	}

	private boolean endsWithAfterAuthorization(ExtendedFunctionalityPosition position) {
		return position.toString().endsWith("_AFTER_AUTHORIZATION");
	}

	@Test
	public void testFactor() throws Exception {
		List<ExtendedFunctionality> extendedFunctionalityList = factory.factor(null, null);

		assertEquals(extendedFunctionalityList.size(), 1);
		assertTrue(extendedFunctionalityList
				.get(0) instanceof SystemSecretSecurityExtendedFunctionality);
	}
}