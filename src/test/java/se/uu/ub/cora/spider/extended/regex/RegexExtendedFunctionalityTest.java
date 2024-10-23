/*
 * Copyright 2024 Uppsala University Library
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
package se.uu.ub.cora.spider.extended.regex;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertTrue;
import static org.testng.Assert.fail;

import java.util.Optional;

import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import se.uu.ub.cora.data.DataProvider;
import se.uu.ub.cora.data.spies.DataFactorySpy;
import se.uu.ub.cora.data.spies.DataRecordGroupSpy;
import se.uu.ub.cora.spider.extendedfunctionality.ExtendedFunctionality;
import se.uu.ub.cora.spider.extendedfunctionality.ExtendedFunctionalityData;
import se.uu.ub.cora.spider.record.DataException;

public class RegexExtendedFunctionalityTest {

	private RegexExtendedFunctionality regexExtFunc;
	private DataRecordGroupSpy someRecord;
	private ExtendedFunctionalityData extendedFunctionalityData;
	private DataFactorySpy dataFactory;

	@BeforeMethod
	private void beforeTest() {
		setupTextVariable();

		dataFactory = new DataFactorySpy();
		DataProvider.onlyForTestSetDataFactory(dataFactory);

		regexExtFunc = new RegexExtendedFunctionality();
	}

	@Test
	public void testInit() throws Exception {
		assertTrue(regexExtFunc instanceof ExtendedFunctionality);
	}

	@Test
	public void testNoAttributes() throws Exception {
		someRecord.MRV.setSpecificReturnValuesSupplier("hasAttributes", () -> false);

		regexExtFunc.useExtendedFunctionality(extendedFunctionalityData);

		someRecord.MCR.assertMethodWasCalled("hasAttributes");
		someRecord.MCR.assertMethodNotCalled("getAttributeValue");
	}

	@Test
	public void testNotTextVariable() throws Exception {
		someRecord.MRV.setSpecificReturnValuesSupplier("getAttributeValue",
				() -> Optional.of("somethingElse"), "type");

		regexExtFunc.useExtendedFunctionality(extendedFunctionalityData);

		someRecord.MCR.assertMethodNotCalled("getFirstAtomicValueWithNameInData");
	}

	@Test
	public void testRegexValid() throws Exception {
		someRecord.MRV.setDefaultReturnValuesSupplier("getFirstAtomicValueWithNameInData",
				() -> "[a-z]");
		try {
			regexExtFunc.useExtendedFunctionality(extendedFunctionalityData);
		} catch (Exception e) {
			fail("No exception should have been thrown");
		}

		someRecord.MCR.assertMethodWasCalled("hasAttributes");
		someRecord.MCR.assertParameters("getFirstAtomicValueWithNameInData", 0, "regEx");
	}

	@Test
	public void testRegexInvalid() throws Exception {
		someRecord.MRV.setDefaultReturnValuesSupplier("getFirstAtomicValueWithNameInData",
				() -> "a{1");
		try {
			regexExtFunc.useExtendedFunctionality(extendedFunctionalityData);
			fail("Exception should have been thrown");
		} catch (Exception e) {
			assertTrue(e instanceof DataException);
			assertEquals(e.getMessage(),
					"The supplied regEx is invalid, Unclosed counted closure near index 3\n"
							+ "a{1");
		}

		someRecord.MCR.assertMethodWasCalled("hasAttributes");
		someRecord.MCR.assertParameters("getFirstAtomicValueWithNameInData", 0, "regEx");
	}

	private void setupTextVariable() {
		someRecord = new DataRecordGroupSpy();
		someRecord.MRV.setSpecificReturnValuesSupplier("hasAttributes", () -> true);
		someRecord.MRV.setSpecificReturnValuesSupplier("getAttributeValue",
				() -> Optional.of("textVariable"), "type");

		extendedFunctionalityData = new ExtendedFunctionalityData();
		extendedFunctionalityData.dataRecordGroup = someRecord;
	}
}
