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
import se.uu.ub.cora.data.spies.DataAtomicSpy;
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
	private DataAtomicSpy regEx;

	@BeforeMethod
	private void beforeTest() {
		someRecord = new DataRecordGroupSpy();
		someRecord.MRV.setSpecificReturnValuesSupplier("hasAttributes", () -> true);
		someRecord.MRV.setSpecificReturnValuesSupplier("getAttributeValue",
				() -> Optional.of("textVariable"), "type");

		regEx = new DataAtomicSpy();
		regEx.MRV.setDefaultReturnValuesSupplier("getValue", () -> ".+");

		someRecord.MRV.setSpecificReturnValuesSupplier("getFirstDataAtomicWithNameInData",
				() -> regEx, "regEx");

		extendedFunctionalityData = new ExtendedFunctionalityData();
		extendedFunctionalityData.dataRecordGroup = someRecord;

		dataFactory = new DataFactorySpy();
		DataProvider.onlyForTestSetDataFactory(dataFactory);

		regexExtFunc = new RegexExtendedFunctionality();
	}

	@Test
	public void testInit() throws Exception {
		assertTrue(regexExtFunc instanceof ExtendedFunctionality);
	}

	@Test
	public void testRegexValid() throws Exception {
		try {
			regexExtFunc.useExtendedFunctionality(extendedFunctionalityData);
		} catch (Exception e) {
			fail("no exception should have been thrown");
		}

		someRecord.MCR.assertParameters("getFirstDataAtomicWithNameInData", 0, "regEx");
		someRecord.MCR.assertMethodWasCalled("hasAttributes");
	}

	@Test
	public void testRegexInvalid() throws Exception {
		regEx.MRV.setDefaultReturnValuesSupplier("getValue", () -> "(b0rked");
		try {
			regexExtFunc.useExtendedFunctionality(extendedFunctionalityData);
		} catch (Exception e) {
			assertEquals(e.getClass(), DataException.class);
			assertEquals(e.getMessage(), "Failed to compile the supplied regEx: (b0rked");
		}

		someRecord.MCR.assertParameters("getFirstDataAtomicWithNameInData", 0, "regEx");
		someRecord.MCR.assertMethodWasCalled("hasAttributes");
	}

}
