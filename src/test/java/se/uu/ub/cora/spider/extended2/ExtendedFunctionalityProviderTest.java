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
package se.uu.ub.cora.spider.extended2;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertSame;
import static org.testng.Assert.assertTrue;

import java.util.ArrayList;
import java.util.List;

import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import se.uu.ub.cora.spider.extended.ExtendedFunctionality;

public class ExtendedFunctionalityProviderTest {

	private FunctionalityFactories functionalityFactories;
	private ExtendedFunctionalityProviderImp provider;
	private List<FunctionalityForCreateBeforeMetadataValidationFactory> list;

	@BeforeMethod
	public void setUp() {
		functionalityFactories = new FunctionalityFactories();
		list = new ArrayList<>();
		functionalityFactories.createBeforeMetadataValidation = list;
		provider = new ExtendedFunctionalityProviderImp(functionalityFactories);

	}

	@Test
	public void testExtendedFactories() {
		assertSame(provider.getExtendedFactories(), functionalityFactories);
	}

	@Test
	public void testCreateBeforeMetadataValidationNoFunctionality() {
		List<ExtendedFunctionality> functionality = provider
				.getFunctionalityForCreateBeforeMetadataValidation("");
		assertTrue(functionality.isEmpty());

	}

	@Test
	public void testCreateBeforeMetadataValidationWithOneFunctionality() {
		FunctionalityFactorySpy factorySpy = new FunctionalityFactorySpy();
		list.add(factorySpy);
		List<ExtendedFunctionality> functionality = provider
				.getFunctionalityForCreateBeforeMetadataValidation("");
		assertTrue(factorySpy.factorWasCalled);
		assertEquals(functionality.get(0), factorySpy.returnedFunctionality);

	}
}
