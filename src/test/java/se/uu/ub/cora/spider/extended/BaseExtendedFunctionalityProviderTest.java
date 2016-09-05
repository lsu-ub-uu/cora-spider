/*
 * Copyright 2016 Uppsala University Library
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

package se.uu.ub.cora.spider.extended;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertTrue;

import java.util.Collections;
import java.util.List;

import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import se.uu.ub.cora.spider.metadata.MetadataConsistencyValidator;

public class BaseExtendedFunctionalityProviderTest {
	private ExtendedFunctionalityProvider baseExtendedFunctionalityProvider;

	@BeforeMethod
	public void setUp() {
		baseExtendedFunctionalityProvider = new BaseExtendedFunctionalityProvider();
	}

	@Test
	public void testGetFunctionalityForCreateBeforeMetadataValidationNullAsType() {
		String recordType = null;
		List<ExtendedFunctionality> eFL = baseExtendedFunctionalityProvider
				.getFunctionalityForCreateBeforeMetadataValidation(recordType);
		assertEquals(Collections.emptyList(), eFL);
	}

	@Test
	public void testGetFunctionalityForCreateBeforeMetadataValidationEmptyAsType() {
		String recordType = "";
		List<ExtendedFunctionality> eFL = baseExtendedFunctionalityProvider
				.getFunctionalityForCreateBeforeMetadataValidation(recordType);
		assertEquals(Collections.emptyList(), eFL);
	}

	@Test
	public void testGetFunctionalityForCreateBeforeMetadataValidationUnknownAsType() {
		String recordType = "UnknownType";
		List<ExtendedFunctionality> eFL = baseExtendedFunctionalityProvider
				.getFunctionalityForCreateBeforeMetadataValidation(recordType);
		assertEquals(Collections.emptyList(), eFL);
	}

	@Test
	public void testGetFunctionalityForCreateAfterMetadataValidationNullAsType() {
		String recordType = null;
		List<ExtendedFunctionality> eFL = baseExtendedFunctionalityProvider
				.getFunctionalityForCreateAfterMetadataValidation(recordType);
		assertEquals(Collections.emptyList(), eFL);
	}

	@Test
	public void testGetFunctionalityForCreateAfterMetadataValidationEmptyAsType() {
		String recordType = "";
		List<ExtendedFunctionality> eFL = baseExtendedFunctionalityProvider
				.getFunctionalityForCreateAfterMetadataValidation(recordType);
		assertEquals(Collections.emptyList(), eFL);
	}

	@Test
	public void testGetFunctionalityForCreateAfterMetadataValidationUnknownAsType() {
		String recordType = "UnknownType";
		List<ExtendedFunctionality> eFL = baseExtendedFunctionalityProvider
				.getFunctionalityForCreateAfterMetadataValidation(recordType);
		assertEquals(Collections.emptyList(), eFL);
	}

	@Test
	public void testGetFunctionalityForCreateAfterMetadataValidationMetadataGroupAsType() {
		String recordType = "metadataGroup";
		List<ExtendedFunctionality> eFL = baseExtendedFunctionalityProvider
				.getFunctionalityForCreateAfterMetadataValidation(recordType);
		assertEquals(1, eFL.size());

		assertTrue(eFL.get(0) instanceof MetadataConsistencyValidator);
	}

}
