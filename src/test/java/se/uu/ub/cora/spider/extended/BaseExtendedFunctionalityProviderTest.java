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
import java.util.HashMap;
import java.util.List;

import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import se.uu.ub.cora.spider.dependency.SpiderDependencyProvider;
import se.uu.ub.cora.spider.dependency.SpiderDependencyProviderSpy;

public class BaseExtendedFunctionalityProviderTest {
	private ExtendedFunctionalityProvider baseExtendedFunctionalityProvider;

	@BeforeMethod
	public void setUp() {
		SpiderDependencyProvider dependencyProvider = new SpiderDependencyProviderSpy(
				new HashMap<>());
		baseExtendedFunctionalityProvider = new BaseExtendedFunctionalityProvider(
				dependencyProvider);
	}

	@Test
	public void testGetFunctionalityForCreateBeforeMetadataValidation() {
		fetchAndAssertCreateBeforeMetadataValidation(null);
		fetchAndAssertCreateBeforeMetadataValidation("");
		fetchAndAssertCreateBeforeMetadataValidation("UnknownType");
		fetchAndAssertCreateBeforeMetadataValidationForAppToken();
	}

	private void fetchAndAssertCreateBeforeMetadataValidation(String recordType) {
		List<ExtendedFunctionality> bEFP = baseExtendedFunctionalityProvider
				.getFunctionalityForCreateBeforeMetadataValidation(recordType);
		assertEquals(Collections.emptyList(), bEFP);
	}

	private void fetchAndAssertCreateBeforeMetadataValidationForAppToken() {
		List<ExtendedFunctionality> bEFP = baseExtendedFunctionalityProvider
				.getFunctionalityForCreateBeforeMetadataValidation("appToken");
		assertEquals(bEFP.size(), 1);
		ExtendedFunctionality extendedFunctionality = bEFP.get(0);
		assertTrue(extendedFunctionality instanceof AppTokenEnhancerAsExtendedFunctionality);
	}

	@Test
	public void testGetFunctionalityForCreateAfterMetadataValidation() {
		fetchAndAssertCreateAfterMetadataValidation(null);
		fetchAndAssertCreateAfterMetadataValidation("");
		fetchAndAssertCreateAfterMetadataValidation("UnkownType");
	}

	@Test
	public void testGetAddedFunctionalityForCreateAfterMetadataValidationForMetadataGroup() {
		List<ExtendedFunctionality> bEFP = baseExtendedFunctionalityProvider
				.getFunctionalityForCreateAfterMetadataValidation("metadataGroup");
		assertEquals(bEFP.size(), 1);
		ExtendedFunctionality extendedFunctionality = bEFP.get(0);
		assertTrue(
				extendedFunctionality instanceof MetadataConsistencyValidatorAsExtendedFunctionality);
	}

	@Test
	public void testGetAddedFunctionalityForCreateAfterMetadataValidationForCollectionVariable() {
		List<ExtendedFunctionality> bEFP = baseExtendedFunctionalityProvider
				.getFunctionalityForCreateAfterMetadataValidation("metadataCollectionVariable");
		assertEquals(bEFP.size(), 1);
		ExtendedFunctionality extendedFunctionality = bEFP.get(0);
		assertTrue(
				extendedFunctionality instanceof MetadataConsistencyValidatorAsExtendedFunctionality);
	}

	private void fetchAndAssertCreateAfterMetadataValidation(String recordType) {
		List<ExtendedFunctionality> bEFP = baseExtendedFunctionalityProvider
				.getFunctionalityForCreateAfterMetadataValidation(recordType);
		assertEquals(Collections.emptyList(), bEFP);
	}

	@Test
	public void testGetFunctionalityForCreateBeforeReturn() {
		fetchAndAssertCreateBeforeReturn(null);
		fetchAndAssertCreateBeforeReturn("");
		fetchAndAssertCreateBeforeReturn("UnkownType");
		fetchAndAssertCreateBeforeReturnForAppToken();
	}

	private void fetchAndAssertCreateBeforeReturn(String recordType) {
		List<ExtendedFunctionality> bEFP = baseExtendedFunctionalityProvider
				.getFunctionalityForCreateBeforeReturn(recordType);
		assertEquals(Collections.emptyList(), bEFP);
	}

	private void fetchAndAssertCreateBeforeReturnForAppToken() {
		List<ExtendedFunctionality> bEFP = baseExtendedFunctionalityProvider
				.getFunctionalityForCreateBeforeReturn("appToken");
		assertEquals(bEFP.size(), 1);
		ExtendedFunctionality extendedFunctionality = bEFP.get(0);
		assertTrue(extendedFunctionality instanceof UserUpdaterForAppTokenAsExtendedFunctionality);
	}

	@Test
	public void testGetFunctionalityForUpdateBeforeMetadataValidationNullAsType() {
		fetchAndAssertUpdateBeforeMetadataValidation(null);
		fetchAndAssertUpdateBeforeMetadataValidation("");
		fetchAndAssertUpdateBeforeMetadataValidation("UnknownType");
	}

	private void fetchAndAssertUpdateBeforeMetadataValidation(String recordType) {
		List<ExtendedFunctionality> bEFP = baseExtendedFunctionalityProvider
				.getFunctionalityForUpdateBeforeMetadataValidation(recordType);
		assertEquals(Collections.emptyList(), bEFP);
	}

	@Test
	public void testGetFunctionalityForUpdateAfterMetadataValidationNullAsType() {
		fetchAndAssertUpdateAfterMetadataValidation(null);
		fetchAndAssertUpdateAfterMetadataValidation("");
		fetchAndAssertUpdateAfterMetadataValidation("UnknownType");
	}

	private void fetchAndAssertUpdateAfterMetadataValidation(String recordType) {
		List<ExtendedFunctionality> bEFP = baseExtendedFunctionalityProvider
				.getFunctionalityForUpdateAfterMetadataValidation(recordType);
		assertEquals(Collections.emptyList(), bEFP);
	}

	@Test
	public void testGetFunctionalityForUpdateAfterMetadataValidationMetadataGroupAsType() {
		String recordType = "metadataGroup";
		fetchAndAssertUpdateAfterMetadataValidation(recordType);
	}

}
