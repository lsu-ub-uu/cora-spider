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

	private ExtendedFunctionalityProviderImp provider;
	private List<ExtendedFunctionalityFactory> fakeImplementations;

	@BeforeMethod
	public void setUp() {
		fakeImplementations = new ArrayList<>();
		provider = new ExtendedFunctionalityProviderImp(fakeImplementations);

	}

	@Test
	public void testExtendedFactories() {
		assertSame(provider.getFunctionalityFactoryImplementations(), fakeImplementations);
	}

	@Test
	public void testCreateBeforeMetadataValidationNoFunctionality() {
		ExtendedFunctionalityProviderImp noImplementationsProvider = new ExtendedFunctionalityProviderImp(
				new ArrayList<>());
		List<ExtendedFunctionality> functionality = noImplementationsProvider
				.getFunctionalityForCreateBeforeMetadataValidation("someRecordType");
		assertTrue(functionality.isEmpty());

	}

	@Test
	public void testCreateBeforeMetadataValidationWithOneFunctionalityEmptyPositionAndEmptyType() {
		ExtendedFunctionalityFactorySpy factorySpy = createFactorySpyInList(null, "", 0);
		provider = new ExtendedFunctionalityProviderImp(fakeImplementations);

		List<ExtendedFunctionality> functionality = provider
				.getFunctionalityForCreateBeforeMetadataValidation("someRecordType");

		assertNoFunctionalityWasReturned(factorySpy, functionality);
	}

	private ExtendedFunctionalityFactorySpy createFactorySpyInList(
			ExtendedFunctionalityPosition extendedFunctionalityPosition, String recordType,
			int runAsNumber) {
		ExtendedFunctionalityContext efc = new ExtendedFunctionalityContext(
				extendedFunctionalityPosition, recordType, runAsNumber);
		List<ExtendedFunctionalityContext> extendedFunctionalityContexts = new ArrayList<>();
		extendedFunctionalityContexts.add(efc);

		ExtendedFunctionalityFactorySpy factorySpy = new ExtendedFunctionalityFactorySpy(
				extendedFunctionalityContexts);
		fakeImplementations.add(factorySpy);
		return factorySpy;
	}

	private void assertNoFunctionalityWasReturned(ExtendedFunctionalityFactorySpy factorySpy,
			List<ExtendedFunctionality> functionality) {
		factorySpy.MCR.assertMethodNotCalled("factor");
		assertTrue(functionality.isEmpty());
	}

	@Test
	public void testCreateBeforeMetadataValidationWithOneFunctionalityWrongPositionAndEmptyType() {
		ExtendedFunctionalityFactorySpy factorySpy = createFactorySpyInList(
				ExtendedFunctionalityPosition.CREATE_BEFORE_METADATA_VALIDATION, "", 0);
		provider = new ExtendedFunctionalityProviderImp(fakeImplementations);

		List<ExtendedFunctionality> functionality = provider
				.getFunctionalityForCreateBeforeMetadataValidation("someRecordType");

		assertNoFunctionalityWasReturned(factorySpy, functionality);
	}

	@Test
	public void testCreateBeforeMetadataValidationWithOneFunctionalityEmptyPositionWrongType() {
		ExtendedFunctionalityFactorySpy factorySpy = createFactorySpyInList(null,
				"notTheRecordTypeWeAreLookingFor", 0);
		provider = new ExtendedFunctionalityProviderImp(fakeImplementations);

		List<ExtendedFunctionality> functionality = provider
				.getFunctionalityForCreateBeforeMetadataValidation("someRecordType");

		assertNoFunctionalityWasReturned(factorySpy, functionality);
	}

	@Test
	public void testCreateBeforeMetadataValidationWithOneFunctionalityWrongPositionAndWrongType() {
		ExtendedFunctionalityPosition extendedFunctionalityPosition = ExtendedFunctionalityPosition.CREATE_AFTER_METADATA_VALIDATION;
		String recordType = "notTheRecordTypeWeAreLookingFor";
		int runAsNumber = 0;
		ExtendedFunctionalityFactorySpy factorySpy = createFactorySpyInList(
				extendedFunctionalityPosition, recordType, runAsNumber);
		provider = new ExtendedFunctionalityProviderImp(fakeImplementations);

		List<ExtendedFunctionality> functionality = provider
				.getFunctionalityForCreateBeforeMetadataValidation("someRecordType");

		assertNoFunctionalityWasReturned(factorySpy, functionality);
	}

	@Test
	public void testCreateBeforeMetadataValidationWithOneFunctionalityRightPositionAndRightType() {
		ExtendedFunctionalityFactorySpy factorySpy = createFactorySpyInList(
				ExtendedFunctionalityPosition.CREATE_BEFORE_METADATA_VALIDATION, "someRecordType",
				0);

		provider = new ExtendedFunctionalityProviderImp(fakeImplementations);
		List<ExtendedFunctionality> functionality = provider
				.getFunctionalityForCreateBeforeMetadataValidation("someRecordType");

		assertEquals(functionality.get(0), factorySpy.MCR.getReturnValue("factor", 0));
		factorySpy.MCR.assertParameters("factor", 0,
				ExtendedFunctionalityPosition.CREATE_BEFORE_METADATA_VALIDATION, "someRecordType");
	}

	// @Test
	// public void testTwoFunctionalityFactoriesForSameTypeAndRecordTypeListInRightOrder() {
	// ExtendedFunctionalityFactorySpy factorySpy = createFactorySpyInList(
	// ExtendedFunctionalityPosition.CREATE_BEFORE_METADATA_VALIDATION, "someRecordType",
	// 1);
	// ExtendedFunctionalityFactorySpy factorySpy2 = createFactorySpyInList(
	// ExtendedFunctionalityPosition.CREATE_BEFORE_METADATA_VALIDATION, "someRecordType",
	// 0);
	//
	// List<ExtendedFunctionality> functionality = provider
	// .getFunctionalityForCreateBeforeMetadataValidation("someRecordType");
	//
	// factorySpy.MCR.assertMethodWasCalled("factor");
	// assertEquals(functionality.get(0), factorySpy2.MCR.getReturnValue("factor", 0));
	//
	// factorySpy.MCR.assertParameters("factor", 0,
	// ExtendedFunctionalityPosition.CREATE_BEFORE_METADATA_VALIDATION, "someRecordType");
	// }

}
