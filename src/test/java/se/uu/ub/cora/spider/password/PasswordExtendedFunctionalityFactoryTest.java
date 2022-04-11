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
package se.uu.ub.cora.spider.password;

import org.testng.annotations.Test;

import se.uu.ub.cora.spider.dependency.SpiderDependencyProviderSpy;

public class PasswordExtendedFunctionalityFactoryTest {
	@Test
	public void testInit() throws Exception {
		PasswordExtendedFunctionalityFactory factory = new PasswordExtendedFunctionalityFactory();
		// useExtendedFunctionalityBeforeStore
		SpiderDependencyProviderSpy dependencyProvider = new SpiderDependencyProviderSpy(null);
		factory.initializeUsingDependencyProvider(dependencyProvider);

		dependencyProvider.MCR.assertParameters("getRecordTypeHandler", 0, "user");

		// List<ExtendedFunctionalityContext> extendedFunctionalityContexts = factory
		// .getExtendedFunctionalityContexts();
		// ExtendedFunctionalityContext firstContext = extendedFunctionalityContexts.get(0);
		// assertEquals(firstContext.recordType, "user");

	}
}
