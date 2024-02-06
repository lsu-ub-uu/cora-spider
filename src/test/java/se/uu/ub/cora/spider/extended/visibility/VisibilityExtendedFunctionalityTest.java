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
package se.uu.ub.cora.spider.extended.visibility;

import static org.testng.Assert.assertTrue;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import se.uu.ub.cora.data.DataProvider;
import se.uu.ub.cora.data.spies.DataFactorySpy;
import se.uu.ub.cora.data.spies.DataGroupSpy;
import se.uu.ub.cora.spider.extendedfunctionality.ExtendedFunctionality;
import se.uu.ub.cora.spider.extendedfunctionality.ExtendedFunctionalityData;

public class VisibilityExtendedFunctionalityTest {

	private static final String TS_VISIBILITY = "tsVisibility";
	private VisibilityExtendedFunctionality visibilityExtFunc;
	private DataGroupSpy updatedDataGroup;
	private DataGroupSpy storedDataGroup;
	private DataGroupSpy updatedAdminInfo;
	private DataGroupSpy storedAdminInfo;
	private ExtendedFunctionalityData extendedFunctionalityData;
	private DataFactorySpy dataFactory;

	@BeforeMethod
	private void beforeTest() {
		updatedDataGroup = new DataGroupSpy();
		storedDataGroup = new DataGroupSpy();
		updatedAdminInfo = new DataGroupSpy();
		storedAdminInfo = new DataGroupSpy();

		updatedDataGroup.MRV.setDefaultReturnValuesSupplier("getFirstGroupWithNameInData",
				() -> updatedAdminInfo);
		updatedAdminInfo.MRV.setDefaultReturnValuesSupplier("getFirstAtomicValueWithNameInData",
				() -> "unpublished");

		storedDataGroup.MRV.setDefaultReturnValuesSupplier("getFirstGroupWithNameInData",
				() -> storedAdminInfo);
		storedAdminInfo.MRV.setDefaultReturnValuesSupplier("getFirstAtomicValueWithNameInData",
				() -> "unpublished");

		extendedFunctionalityData = new ExtendedFunctionalityData();
		extendedFunctionalityData.dataGroup = updatedDataGroup;
		extendedFunctionalityData.previouslyStoredTopDataGroup = storedDataGroup;

		dataFactory = new DataFactorySpy();
		DataProvider.onlyForTestSetDataFactory(dataFactory);

		visibilityExtFunc = new VisibilityExtendedFunctionality();
	}

	@Test
	public void testInit() throws Exception {
		assertTrue(visibilityExtFunc instanceof ExtendedFunctionality);
	}

	@Test
	public void testVisibilityAdminInfoDoesNotExist() throws Exception {
		visibilityExtFunc.useExtendedFunctionality(extendedFunctionalityData);

		updatedDataGroup.MCR.assertParameters("containsChildWithNameInData", 0, "adminInfo");
		updatedDataGroup.MCR.assertMethodNotCalled("getFirstGroupWithNameInData");
	}

	@Test
	public void testVisiblityNoChanges() throws Exception {
		updatedDataGroup.MRV.setDefaultReturnValuesSupplier("containsChildWithNameInData",
				() -> true);
		storedDataGroup.MRV.setDefaultReturnValuesSupplier("containsChildWithNameInData",
				() -> true);

		visibilityExtFunc.useExtendedFunctionality(extendedFunctionalityData);

		updatedDataGroup.MCR.assertParameters("containsChildWithNameInData", 0, "adminInfo");
		updatedDataGroup.MCR.assertParameters("getFirstGroupWithNameInData", 0, "adminInfo");
		updatedAdminInfo.MCR.assertParameters("getFirstAtomicValueWithNameInData", 0, "visibility");

		storedDataGroup.MCR.assertParameters("containsChildWithNameInData", 0, "adminInfo");
		storedDataGroup.MCR.assertParameters("getFirstGroupWithNameInData", 0, "adminInfo");
		storedAdminInfo.MCR.assertParameters("getFirstAtomicValueWithNameInData", 0, "visibility");

		dataFactory.MCR.assertMethodNotCalled("factorAtomicUsingNameInDataAndValue");
		updatedAdminInfo.MCR.assertMethodNotCalled("removeFirstChildWithNameInData");
		updatedAdminInfo.MCR.assertMethodNotCalled("addChild");
	}

	@Test
	public void testVisibilityChangedTsVisibilityNotExist() throws Exception {
		updatedDataGroup.MRV.setDefaultReturnValuesSupplier("containsChildWithNameInData",
				() -> true);

		updatedAdminInfo.MRV.setDefaultReturnValuesSupplier("getFirstAtomicValueWithNameInData",
				() -> "published");

		visibilityExtFunc.useExtendedFunctionality(extendedFunctionalityData);

		dataFactory.MCR.assertParameters("factorAtomicUsingNameInDataAndValue", 0, TS_VISIBILITY);
		var tsVisibilityAtomic = dataFactory.MCR
				.getReturnValue("factorAtomicUsingNameInDataAndValue", 0);

		updatedAdminInfo.MCR.assertParameters("addChild", 0, tsVisibilityAtomic);
	}

	@Test
	public void testTsVisibilityTimeStamp() throws Exception {
		updatedDataGroup.MRV.setDefaultReturnValuesSupplier("containsChildWithNameInData",
				() -> true);
		updatedAdminInfo.MRV.setDefaultReturnValuesSupplier("getFirstAtomicValueWithNameInData",
				() -> "published");

		LocalDateTime before = LocalDateTime.now();
		visibilityExtFunc.useExtendedFunctionality(extendedFunctionalityData);
		LocalDateTime after = LocalDateTime.now();

		String tsVisibility = (String) dataFactory.MCR
				.getValueForMethodNameAndCallNumberAndParameterName(
						"factorAtomicUsingNameInDataAndValue", 0, "value");

		LocalDateTime tsVisibilityLocalDate = parseToLocalDate(tsVisibility);

		assertTrue(tsVisibilityLocalDate.isAfter(before));
		assertTrue(tsVisibilityLocalDate.isBefore(after));
	}

	@Test
	public void testTsVisibilityExistAndDifferentValue() throws Exception {
		updatedDataGroup.MRV.setDefaultReturnValuesSupplier("containsChildWithNameInData",
				() -> true);
		updatedAdminInfo.MRV.setDefaultReturnValuesSupplier("getFirstAtomicValueWithNameInData",
				() -> "published");
		updatedAdminInfo.MRV.setSpecificReturnValuesSupplier("containsChildWithNameInData",
				() -> true, TS_VISIBILITY);
		storedDataGroup.MRV.setDefaultReturnValuesSupplier("containsChildWithNameInData",
				() -> true);

		visibilityExtFunc.useExtendedFunctionality(extendedFunctionalityData);
		updatedAdminInfo.MCR.assertMethodWasCalled("removeFirstChildWithNameInData");
		updatedAdminInfo.MCR.assertParameters("containsChildWithNameInData", 0, TS_VISIBILITY);
		dataFactory.MCR.assertMethodWasCalled("factorAtomicUsingNameInDataAndValue");
		updatedAdminInfo.MCR.assertMethodWasCalled("addChild");
	}

	@Test
	public void testTsVisibilityDontExistAndDifferentValue() throws Exception {
		updatedDataGroup.MRV.setDefaultReturnValuesSupplier("containsChildWithNameInData",
				() -> true);
		updatedAdminInfo.MRV.setDefaultReturnValuesSupplier("getFirstAtomicValueWithNameInData",
				() -> "published");
		updatedAdminInfo.MRV.setSpecificReturnValuesSupplier("containsChildWithNameInData",
				() -> false, TS_VISIBILITY);
		storedDataGroup.MRV.setDefaultReturnValuesSupplier("containsChildWithNameInData",
				() -> true);

		visibilityExtFunc.useExtendedFunctionality(extendedFunctionalityData);
		updatedAdminInfo.MCR.assertMethodNotCalled("removeFirstChildWithNameInData");

		dataFactory.MCR.assertMethodWasCalled("factorAtomicUsingNameInDataAndValue");
		updatedAdminInfo.MCR.assertMethodWasCalled("addChild");
	}

	private LocalDateTime parseToLocalDate(String toParse) {
		String format = "yyyy-MM-dd'T'HH:mm:ss.SSSSSS'Z'";
		DateTimeFormatter formatter = DateTimeFormatter.ofPattern(format);

		return LocalDateTime.parse(toParse, formatter);
	}
}
