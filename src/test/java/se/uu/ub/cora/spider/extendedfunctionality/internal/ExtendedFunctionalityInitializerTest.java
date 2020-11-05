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

import static org.testng.Assert.assertTrue;

import java.util.ServiceLoader;

import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import se.uu.ub.cora.spider.extendedfunctionality.ExtendedFunctionalityFactory;

public class ExtendedFunctionalityInitializerTest {

	private ExtendedFunctionalityInitializer initializer;

	@BeforeMethod
	public void beforeMethod() {
		initializer = new ExtendedFunctionalityInitializer();
	}

	@Test
	public void testFunctionalityForCreateBeforeMetadataValidationFactory() {
		ExtendedFunctionalityProviderImp provider = (ExtendedFunctionalityProviderImp) initializer
				.getExtendedFunctionalityProvider();
		FactorySorterImp factorySorter = (FactorySorterImp) provider
				.getFactorySorterNeededForTest();
		Iterable<ExtendedFunctionalityFactory> extendedFunctionalityFactories = factorySorter
				.getExtendedFunctionalityFactoriesNeededForTest();
		assertTrue(extendedFunctionalityFactories instanceof ServiceLoader);
	}

}
