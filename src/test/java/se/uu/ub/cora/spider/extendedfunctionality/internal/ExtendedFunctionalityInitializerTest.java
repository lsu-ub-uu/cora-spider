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
package se.uu.ub.cora.spider.extendedfunctionality.internal;

import static org.testng.Assert.assertSame;
import static org.testng.Assert.assertTrue;

import java.util.Collections;
import java.util.ServiceLoader;

import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import se.uu.ub.cora.data.DataProvider;
import se.uu.ub.cora.data.spies.DataFactorySpy;
import se.uu.ub.cora.logger.LoggerFactory;
import se.uu.ub.cora.logger.LoggerProvider;
import se.uu.ub.cora.spider.dependency.SpiderDependencyProvider;
import se.uu.ub.cora.spider.dependency.spy.SpiderDependencyProviderOldSpy;
import se.uu.ub.cora.spider.extendedfunctionality.ExtendedFunctionalityFactory;
import se.uu.ub.cora.spider.log.LoggerFactorySpy;

public class ExtendedFunctionalityInitializerTest {
	private DataFactorySpy dataFactorySpy;
	private ExtendedFunctionalityInitializer initializer;
	private SpiderDependencyProvider dependencyProvider;

	@BeforeMethod
	public void beforeMethod() {
		LoggerFactory loggerFactory = new LoggerFactorySpy();
		LoggerProvider.setLoggerFactory(loggerFactory);
		dataFactorySpy = new DataFactorySpy();
		DataProvider.onlyForTestSetDataFactory(dataFactorySpy);
		dependencyProvider = new SpiderDependencyProviderOldSpy(Collections.emptyMap());
		initializer = new ExtendedFunctionalityInitializer(dependencyProvider);
	}

	@Test
	public void testFunctionalityForCreateBeforeMetadataValidationFactory() {
		ExtendedFunctionalityProviderImp provider = (ExtendedFunctionalityProviderImp) initializer
				.getExtendedFunctionalityProvider();
		FactorySorterImp factorySorter = (FactorySorterImp) provider
				.getFactorySorterNeededForTest();
		assertSame(factorySorter.getDependencyProvider(), dependencyProvider);

		Iterable<ExtendedFunctionalityFactory> extendedFunctionalityFactories = factorySorter
				.getExtendedFunctionalityFactoriesNeededForTest();
		assertTrue(extendedFunctionalityFactories instanceof ServiceLoader);
	}

}
