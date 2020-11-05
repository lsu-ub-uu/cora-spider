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
import static se.uu.ub.cora.spider.extendedfunctionality.ExtendedFunctionalityPosition.CREATE_AFTER_METADATA_VALIDATION;
import static se.uu.ub.cora.spider.extendedfunctionality.ExtendedFunctionalityPosition.CREATE_BEFORE_METADATA_VALIDATION;
import static se.uu.ub.cora.spider.extendedfunctionality.ExtendedFunctionalityPosition.CREATE_BEFORE_RETURN;
import static se.uu.ub.cora.spider.extendedfunctionality.ExtendedFunctionalityPosition.UPDATE_BEFORE_METADATA_VALIDATION;

import java.util.List;

import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import se.uu.ub.cora.spider.extended.ExtendedFunctionality;
import se.uu.ub.cora.spider.extendedfunctionality.ExtendedFunctionalityPosition;

public class ExtendedFunctionalityProviderTest {

	private ExtendedFunctionalityProviderImp provider;
	private FactorySorterSpy factorySorterSpy;

	@BeforeMethod
	public void setUp() {
		factorySorterSpy = new FactorySorterSpy();
		provider = new ExtendedFunctionalityProviderImp(factorySorterSpy);

	}

	@Test
	public void testExtendedFactories() {
		assertSame(provider.getFactorySorterNeededForTest(), factorySorterSpy);
	}

	@Test
	public void testGetFunctionalityForCreateBeforeMeatadataValidation() throws Exception {
		List<ExtendedFunctionality> functionality = provider
				.getFunctionalityForCreateBeforeMetadataValidation("someRecordType");

		assertCorrectCallAndAnswerFor(CREATE_BEFORE_METADATA_VALIDATION, functionality);
	}

	@Test
	public void testGetFunctionalityForCreateAfterMetadataValidation() throws Exception {
		List<ExtendedFunctionality> functionality = provider
				.getFunctionalityForCreateAfterMetadataValidation("someRecordType");

		assertCorrectCallAndAnswerFor(CREATE_AFTER_METADATA_VALIDATION, functionality);
	}

	@Test
	public void testGetFunctionalityForCreateBeforeReturn() throws Exception {
		List<ExtendedFunctionality> functionality = provider
				.getFunctionalityForCreateBeforeReturn("someRecordType");

		assertCorrectCallAndAnswerFor(CREATE_BEFORE_RETURN, functionality);
	}

	@Test
	public void testGetFunctionalityForUpdateBeforeMetadataValidation() throws Exception {
		List<ExtendedFunctionality> functionality = provider
				.getFunctionalityForUpdateBeforeMetadataValidation("someRecordType");

		assertCorrectCallAndAnswerFor(UPDATE_BEFORE_METADATA_VALIDATION, functionality);
	}

	@Test
	public void testGetFunctionalityForUpdateAfterMetadataValidation() throws Exception {
		List<ExtendedFunctionality> functionality = provider
				.getFunctionalityForUpdateAfterMetadataValidation("someRecordType");

		assertCorrectCallAndAnswerFor(
				ExtendedFunctionalityPosition.UPDATE_AFTER_METADATA_VALIDATION, functionality);
	}

	@Test
	public void testGetFunctionalityForDeleteBefore() throws Exception {
		List<ExtendedFunctionality> functionality = provider
				.getFunctionalityBeforeDelete("someRecordType");

		assertCorrectCallAndAnswerFor(ExtendedFunctionalityPosition.DELETE_BEFORE, functionality);
	}

	@Test
	public void testGetFunctionalityForDeleteAfter() throws Exception {
		List<ExtendedFunctionality> functionality = provider
				.getFunctionalityAfterDelete("someRecordType");

		assertCorrectCallAndAnswerFor(ExtendedFunctionalityPosition.DELETE_AFTER, functionality);
	}

	private void assertCorrectCallAndAnswerFor(ExtendedFunctionalityPosition position,
			List<ExtendedFunctionality> functionality) {
		assertSpyCalledWithCorrectPosition(position);
		assertFunctionalityIsTheOneReturnedFromSpy(functionality);
	}

	private void assertSpyCalledWithCorrectPosition(ExtendedFunctionalityPosition position) {
		factorySorterSpy.MCR.assertParameters("getFunctionalityForPositionAndRecordType", 0,
				position, "someRecordType");
	}

	private void assertFunctionalityIsTheOneReturnedFromSpy(
			List<ExtendedFunctionality> functionality) {
		factorySorterSpy.MCR.assertReturn("getFunctionalityForPositionAndRecordType", 0,
				functionality);
	}

}
