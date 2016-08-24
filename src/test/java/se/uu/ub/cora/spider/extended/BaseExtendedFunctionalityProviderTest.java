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

import java.util.Collections;
import java.util.List;

import org.testng.annotations.Test;

public class BaseExtendedFunctionalityProviderTest {
	@Test
	public void testGetFunctionalityForCreateAfterMetadataValidationNullAsType() {
		ExtendedFunctionalityProvider baseExtendedFunctionalityProvider = new BaseExtendedFunctionalityProvider();
		String recordType = null;
		List<ExtendedFunctionality> functionalityForCreateAfterMetadataValidation = baseExtendedFunctionalityProvider
				.getFunctionalityForCreateAfterMetadataValidation(recordType);
		assertEquals(Collections.emptyList(), functionalityForCreateAfterMetadataValidation);
	}

	@Test
	public void testGetFunctionalityForCreateAfterMetadataValidationEmptyAsType() {
		ExtendedFunctionalityProvider baseExtendedFunctionalityProvider = new BaseExtendedFunctionalityProvider();
		String recordType = "";
		List<ExtendedFunctionality> functionalityForCreateAfterMetadataValidation = baseExtendedFunctionalityProvider
				.getFunctionalityForCreateAfterMetadataValidation(recordType);
		assertEquals(Collections.emptyList(), functionalityForCreateAfterMetadataValidation);
	}

	@Test
	public void testGetFunctionalityForCreateAfterMetadataValidationUnknownAsType() {
		ExtendedFunctionalityProvider baseExtendedFunctionalityProvider = new BaseExtendedFunctionalityProvider();
		String recordType = "UnknownType";
		List<ExtendedFunctionality> functionalityForCreateAfterMetadataValidation = baseExtendedFunctionalityProvider
				.getFunctionalityForCreateAfterMetadataValidation(recordType);
		assertEquals(Collections.emptyList(), functionalityForCreateAfterMetadataValidation);
	}
	
	@Test
	public void testGetFunctionalityForCreateAfterMetadataValidationMetadataGroupAsType() {
		ExtendedFunctionalityProvider baseExtendedFunctionalityProvider = new BaseExtendedFunctionalityProvider();
		String recordType = "metadataGroup";
		List<ExtendedFunctionality> functionalityForCreateAfterMetadataValidation = baseExtendedFunctionalityProvider
				.getFunctionalityForCreateAfterMetadataValidation(recordType);
		assertEquals(1, functionalityForCreateAfterMetadataValidation.size());
	}
	
}
