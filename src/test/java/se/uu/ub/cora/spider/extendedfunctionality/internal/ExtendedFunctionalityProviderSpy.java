/*
 * Copyright 2022, 2023, 2024 Uppsala University Library
 * Copyright 2016 Olov McKie
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

import static org.testng.Assert.assertEquals;

import java.util.ArrayList;
import java.util.List;

import se.uu.ub.cora.spider.extendedfunctionality.ExtendedFunctionality;
import se.uu.ub.cora.spider.extendedfunctionality.ExtendedFunctionalityData;
import se.uu.ub.cora.spider.extendedfunctionality.ExtendedFunctionalityPosition;
import se.uu.ub.cora.spider.extendedfunctionality.ExtendedFunctionalityProvider;
import se.uu.ub.cora.spider.extendedfunctionality.ExtendedFunctionalitySpy;
import se.uu.ub.cora.testutils.mcr.MethodCallRecorder;
import se.uu.ub.cora.testutils.mrv.MethodReturnValues;

public class ExtendedFunctionalityProviderSpy implements ExtendedFunctionalityProvider {
	public MethodCallRecorder MCR = new MethodCallRecorder();
	public MethodReturnValues MRV = new MethodReturnValues();

	public ExtendedFunctionalityProviderSpy() {
		MCR.useMRV(MRV);
		MRV.setDefaultReturnValuesSupplier("getFunctionalityBeforeDelete",
				() -> createListWithTwoExtendedFunctionalitySpies());
		MRV.setDefaultReturnValuesSupplier("getFunctionalityAfterDelete",
				() -> createListWithTwoExtendedFunctionalitySpies());
		MRV.setDefaultReturnValuesSupplier("getFunctionalityForPositionAndRecordType",
				() -> createListWithTwoExtendedFunctionalitySpies());
	}

	private List<ExtendedFunctionality> createListWithTwoExtendedFunctionalitySpies() {
		ArrayList<ExtendedFunctionality> listOfExtendedFunctionality = new ArrayList<>();
		ExtendedFunctionalitySpy extendedFunctionalitySpy = new ExtendedFunctionalitySpy();
		listOfExtendedFunctionality.add(extendedFunctionalitySpy);
		ExtendedFunctionalitySpy extendedFunctionalitySpy2 = new ExtendedFunctionalitySpy();
		listOfExtendedFunctionality.add(extendedFunctionalitySpy2);
		return listOfExtendedFunctionality;
	}

	@Override
	public List<ExtendedFunctionality> getFunctionalityBeforeDelete(String recordType) {
		return (List<ExtendedFunctionality>) MCR.addCallAndReturnFromMRV("recordType", recordType);
	}

	@Override
	public List<ExtendedFunctionality> getFunctionalityAfterDelete(String recordType) {
		return (List<ExtendedFunctionality>) MCR.addCallAndReturnFromMRV("recordType", recordType);
	}

	@Override
	public List<ExtendedFunctionality> getFunctionalityForPositionAndRecordType(
			ExtendedFunctionalityPosition position, String recordType) {
		return (List<ExtendedFunctionality>) MCR.addCallAndReturnFromMRV("position", position,
				"recordType", recordType);
	}

	public void assertCallToPositionAndFunctionalityCalledWithData(
			ExtendedFunctionalityPosition position, ExtendedFunctionalityData expectedData,
			int callNumber) {
		MCR.assertParameter("getFunctionalityForPositionAndRecordType", callNumber, "position",
				position);
		MCR.assertParameter("getFunctionalityForPositionAndRecordType", callNumber, "recordType",
				expectedData.recordType);
		List<ExtendedFunctionalitySpy> exSpyList = (List<ExtendedFunctionalitySpy>) MCR
				.getReturnValue("getFunctionalityForPositionAndRecordType", callNumber);

		assertExtendedFunctionalityIsCalledWithExpectedData(exSpyList.get(0), expectedData);
		assertExtendedFunctionalityIsCalledWithExpectedData(exSpyList.get(1), expectedData);
	}

	public void assertCallToMethodAndFunctionalityCalledWithData(String methodName,
			ExtendedFunctionalityData expectedData) {
		MCR.assertParameter(methodName, 0, "recordType", expectedData.recordType);
		MCR.assertNumberOfCallsToMethod(methodName, 1);
		List<ExtendedFunctionalitySpy> exSpyList = (List<ExtendedFunctionalitySpy>) MCR
				.getReturnValue(methodName, 0);

		assertExtendedFunctionalityIsCalledWithExpectedData(exSpyList.get(0), expectedData);
		assertExtendedFunctionalityIsCalledWithExpectedData(exSpyList.get(1), expectedData);
	}

	private void assertExtendedFunctionalityIsCalledWithExpectedData(
			ExtendedFunctionalitySpy extendedFunctionality,
			ExtendedFunctionalityData expectedData) {
		String methodName2 = "useExtendedFunctionality";
		extendedFunctionality.MCR.assertMethodWasCalled(methodName2);
		extendedFunctionality.MCR.assertNumberOfCallsToMethod(methodName2, 1);
		ExtendedFunctionalityData data = (ExtendedFunctionalityData) extendedFunctionality.MCR
				.getValueForMethodNameAndCallNumberAndParameterName(methodName2, 0, "data");
		assertEquals(data.recordType, expectedData.recordType);
		assertEquals(data.recordId, expectedData.recordId);
		assertEquals(data.authToken, expectedData.authToken);
		assertEquals(data.user, expectedData.user);
		assertEquals(data.previouslyStoredTopDataGroup, expectedData.previouslyStoredTopDataGroup);
		assertEquals(data.dataGroup, expectedData.dataGroup);
	}

}
