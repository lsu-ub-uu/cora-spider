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

		assertEqualNumberOfContextsThanHooksThatEndsWithAfterAuthorization(
				extendedFunctionalityContexts);

		ExtendedFunctionalityContext firstContext = extendedFunctionalityContexts.get(0);
		assertFirstHook(firstContext);
	}

	private void assertFirstHook(ExtendedFunctionalityContext firstContext) {
		assertEquals(firstContext.position,
				ExtendedFunctionalityPosition.CREATE_AFTER_AUTHORIZATION);
		assertEquals(firstContext.recordType, "systemSecret");
	}

	private void assertEqualNumberOfContextsThanHooksThatEndsWithAfterAuthorization(
			List<ExtendedFunctionalityContext> extendedFunctionalityContexts) {

		assertEquals(extendedFunctionalityContexts.size(),
				getNumberOfHooksEndingWith(
						"_AFTER_AUTHORIZATION"));
	}

	private int getNumberOfHooksEndingWith(String string) {
		int number = 0;
		for (ExtendedFunctionalityPosition hook : ExtendedFunctionalityPosition.values()) {
			if (endsWithAfterAuthorization(hook)) {
				number++;
			}
		}
		return number;
	}

	private boolean endsWithAfterAuthorization(ExtendedFunctionalityPosition hook) {
		return hook.toString().endsWith("_AFTER_AUTHORIZATION");
	}

}
