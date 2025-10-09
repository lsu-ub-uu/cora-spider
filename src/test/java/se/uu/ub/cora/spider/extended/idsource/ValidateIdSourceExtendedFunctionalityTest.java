/*
 * Copyright 2025 Uppsala University Library
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
package se.uu.ub.cora.spider.extended.idsource;

import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import se.uu.ub.cora.bookkeeper.metadata.DataMissingException;
import se.uu.ub.cora.data.DataRecordLink;
import se.uu.ub.cora.data.spies.DataRecordGroupSpy;
import se.uu.ub.cora.spider.extendedfunctionality.ExtendedFunctionality;
import se.uu.ub.cora.spider.extendedfunctionality.ExtendedFunctionalityData;

public class ValidateIdSourceExtendedFunctionalityTest {

	private ExtendedFunctionality extFunc;
	private DataRecordGroupSpy dataRecordGroup;
	private ExtendedFunctionalityData data;

	@BeforeMethod
	private void beforeMethod() {
		extFunc = new ValidateIdSourceExtendedFunctionality();

		setData();
	}

	private void setData() {
		dataRecordGroup = new DataRecordGroupSpy();
		data = new ExtendedFunctionalityData();
		data.dataRecordGroup = dataRecordGroup;
	}

	@Test
	public void testDoNothingIfSequenceNotChoosen() {
		setDataRecordGroup("notSequence", false);

		extFunc.useExtendedFunctionality(data);

		dataRecordGroup.MCR.assertParameters("getFirstAtomicValueWithNameInData", 0, "idSource");
		dataRecordGroup.MCR.assertMethodNotCalled("containsChildOfTypeAndName");
	}

	@Test
	public void testWhenSequenceIsChoosen_SequenceLinked_DoNothing() {
		setDataRecordGroup("sequence", true);

		extFunc.useExtendedFunctionality(data);

		dataRecordGroup.MCR.assertParameters("getFirstAtomicValueWithNameInData", 0, "idSource");
		dataRecordGroup.MCR.assertParameters("containsChildOfTypeAndName", 0, DataRecordLink.class,
				"sequence");
	}

	@Test(expectedExceptions = DataMissingException.class, expectedExceptionsMessageRegExp = ""
			+ "The record type someId must link to a sequence when a sequence is chosen as the idSource.")
	public void testWhenSequenceIsChoosen_NoSequenceLinked_ThrowException() {
		setDataRecordGroup("sequence", false);

		extFunc.useExtendedFunctionality(data);

		dataRecordGroup.MCR.assertParameters("getFirstAtomicValueWithNameInData", 0, "idSource");
		dataRecordGroup.MCR.assertParameters("containsChildOfTypeAndName", 0, DataRecordLink.class,
				"sequence");
	}

	private void setDataRecordGroup(String idSource, boolean contains) {
		dataRecordGroup.MRV.setDefaultReturnValuesSupplier("getId", () -> "someId");
		dataRecordGroup.MRV.setSpecificReturnValuesSupplier("getFirstAtomicValueWithNameInData",
				() -> idSource, "idSource");
		dataRecordGroup.MRV.setSpecificReturnValuesSupplier("containsChildOfTypeAndName",
				() -> contains, DataRecordLink.class, "sequence");
	}

}
