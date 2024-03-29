/*
 * Copyright 2020, 2021 Uppsala University Library
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
import static se.uu.ub.cora.spider.extendedfunctionality.ExtendedFunctionalityPosition.CREATE_BEFORE_ENHANCE;
import static se.uu.ub.cora.spider.extendedfunctionality.ExtendedFunctionalityPosition.CREATE_BEFORE_METADATA_VALIDATION;
import static se.uu.ub.cora.spider.extendedfunctionality.ExtendedFunctionalityPosition.READ_BEFORE_RETURN;
import static se.uu.ub.cora.spider.extendedfunctionality.ExtendedFunctionalityPosition.UPDATE_BEFORE_METADATA_VALIDATION;
import static se.uu.ub.cora.spider.extendedfunctionality.ExtendedFunctionalityPosition.UPDATE_BEFORE_RETURN;

import java.util.List;

import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import se.uu.ub.cora.spider.extendedfunctionality.ExtendedFunctionality;
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

		assertCorrectCallAndAnswerFor(CREATE_BEFORE_METADATA_VALIDATION, functionality,
				"someRecordType");
	}

	@Test
	public void testGetFunctionalityForCreateAfterMetadataValidation() throws Exception {
		List<ExtendedFunctionality> functionality = provider
				.getFunctionalityForCreateAfterMetadataValidation("someRecordType");

		assertCorrectCallAndAnswerFor(CREATE_AFTER_METADATA_VALIDATION, functionality,
				"someRecordType");
	}

	@Test
	public void testGetFunctionalityForCreateBeforeEnhance() throws Exception {
		List<ExtendedFunctionality> functionality = provider
				.getFunctionalityForCreateBeforeEnhance("someRecordType");

		assertCorrectCallAndAnswerFor(CREATE_BEFORE_ENHANCE, functionality, "someRecordType");
	}

	@Test
	public void testGetFunctionalityForReadBeforeReturn() throws Exception {
		List<ExtendedFunctionality> functionality = provider
				.getFunctionalityForReadBeforeReturn("someRecordType");

		assertCorrectCallAndAnswerFor(READ_BEFORE_RETURN, functionality, "someRecordType");
	}

	@Test
	public void testGetFunctionalityForUpdateBeforeMetadataValidation() throws Exception {
		List<ExtendedFunctionality> functionality = provider
				.getFunctionalityForUpdateBeforeMetadataValidation("someRecordType");

		assertCorrectCallAndAnswerFor(UPDATE_BEFORE_METADATA_VALIDATION, functionality,
				"someRecordType");
	}

	@Test
	public void testGetFunctionalityForUpdateAfterMetadataValidation() throws Exception {
		List<ExtendedFunctionality> functionality = provider
				.getFunctionalityForUpdateAfterMetadataValidation("someRecordType");

		assertCorrectCallAndAnswerFor(
				ExtendedFunctionalityPosition.UPDATE_AFTER_METADATA_VALIDATION, functionality,
				"someRecordType");
	}

	@Test
	public void testGetFunctionalityForUpdateBeforeReturn() throws Exception {
		List<ExtendedFunctionality> functionality = provider
				.getFunctionalityForUpdateBeforeReturn("someRecordType");

		assertCorrectCallAndAnswerFor(UPDATE_BEFORE_RETURN, functionality, "someRecordType");
	}

	@Test
	public void testGetFunctionalityForDeleteBefore() throws Exception {
		List<ExtendedFunctionality> functionality = provider
				.getFunctionalityBeforeDelete("someRecordType");

		assertCorrectCallAndAnswerFor(ExtendedFunctionalityPosition.DELETE_BEFORE, functionality,
				"someRecordType");
	}

	@Test
	public void testGetFunctionalityForDeleteAfter() throws Exception {
		List<ExtendedFunctionality> functionality = provider
				.getFunctionalityAfterDelete("someRecordType");

		assertCorrectCallAndAnswerFor(ExtendedFunctionalityPosition.DELETE_AFTER, functionality,
				"someRecordType");
	}

	@Test
	public void testGetFunctionalityForUpdateBeforeStore() throws Exception {
		List<ExtendedFunctionality> functionality = provider
				.getFunctionalityForUpdateBeforeStore("someRecordType");

		assertCorrectCallAndAnswerFor(ExtendedFunctionalityPosition.UPDATE_BEFORE_STORE,
				functionality, "someRecordType");
	}

	private void assertCorrectCallAndAnswerFor(ExtendedFunctionalityPosition position,
			List<ExtendedFunctionality> functionality, String recordType) {
		assertSpyCalledWithCorrectPosition(position, recordType);
		assertFunctionalityIsTheOneReturnedFromSpy(functionality);
	}

	private void assertSpyCalledWithCorrectPosition(ExtendedFunctionalityPosition position,
			String recordType) {
		factorySorterSpy.MCR.assertParameters("getFunctionalityForPositionAndRecordType", 0,
				position, recordType);
	}

	private void assertFunctionalityIsTheOneReturnedFromSpy(
			List<ExtendedFunctionality> functionality) {
		factorySorterSpy.MCR.assertReturn("getFunctionalityForPositionAndRecordType", 0,
				functionality);
	}

	@Test
	public void testGetFunctionalityForUpdateAfterStore() {
		String recordType = "someOtherRecordType";
		List<ExtendedFunctionality> functionality = provider
				.getFunctionalityForUpdateAfterStore(recordType);
		assertCorrectCallAndAnswerFor(ExtendedFunctionalityPosition.UPDATE_AFTER_STORE,
				functionality, recordType);

	}

}
