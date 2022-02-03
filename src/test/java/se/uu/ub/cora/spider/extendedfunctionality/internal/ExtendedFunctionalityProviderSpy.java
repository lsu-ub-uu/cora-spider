/*
 * Copyright 2016 Olov McKie
 * Copyright 2022 Uppsala University Library
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
import se.uu.ub.cora.spider.extendedfunctionality.ExtendedFunctionalityProvider;
import se.uu.ub.cora.spider.extendedfunctionality.ExtendedFunctionalitySpy;
import se.uu.ub.cora.testutils.mcr.MethodCallRecorder;

public class ExtendedFunctionalityProviderSpy implements ExtendedFunctionalityProvider {
	public MethodCallRecorder MCR = new MethodCallRecorder();

	@Override
	public List<ExtendedFunctionality> getFunctionalityForCreateBeforeMetadataValidation(
			String recordType) {
		MCR.addCall("recordType", recordType);
		List<ExtendedFunctionality> out = createListWithTwoExtendedFunctionalitySpies();
		MCR.addReturned(out);
		return out;
	}

	@Override
	public List<ExtendedFunctionality> getFunctionalityForCreateAfterMetadataValidation(
			String recordType) {
		MCR.addCall("recordType", recordType);
		List<ExtendedFunctionality> out = createListWithTwoExtendedFunctionalitySpies();
		MCR.addReturned(out);
		return out;
	}

	@Override
	public List<ExtendedFunctionality> getFunctionalityForCreateBeforeReturn(String recordType) {
		MCR.addCall("recordType", recordType);
		List<ExtendedFunctionality> out = createListWithTwoExtendedFunctionalitySpies();
		MCR.addReturned(out);
		return out;
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
	public List<ExtendedFunctionality> getFunctionalityForUpdateBeforeMetadataValidation(
			String recordType) {
		MCR.addCall("recordType", recordType);
		List<ExtendedFunctionality> out = createListWithTwoExtendedFunctionalitySpies();
		MCR.addReturned(out);
		return out;
	}

	@Override
	public List<ExtendedFunctionality> getFunctionalityForUpdateAfterMetadataValidation(
			String recordType) {
		MCR.addCall("recordType", recordType);
		List<ExtendedFunctionality> out = createListWithTwoExtendedFunctionalitySpies();
		MCR.addReturned(out);
		return out;
	}

	@Override
	public List<ExtendedFunctionality> getFunctionalityForUpdateBeforeStore(String recordType) {
		MCR.addCall("recordType", recordType);
		List<ExtendedFunctionality> out = createListWithTwoExtendedFunctionalitySpies();
		MCR.addReturned(out);
		return out;
	}

	@Override
	public List<ExtendedFunctionality> getFunctionalityForUpdateAfterStore(String recordType) {
		MCR.addCall("recordType", recordType);
		List<ExtendedFunctionality> out = createListWithTwoExtendedFunctionalitySpies();
		MCR.addReturned(out);
		return out;
	}

	@Override
	public List<ExtendedFunctionality> getFunctionalityBeforeDelete(String recordType) {
		MCR.addCall("recordType", recordType);
		List<ExtendedFunctionality> out = createListWithTwoExtendedFunctionalitySpies();
		MCR.addReturned(out);
		return out;
	}

	@Override
	public List<ExtendedFunctionality> getFunctionalityAfterDelete(String recordType) {
		MCR.addCall("recordType", recordType);
		List<ExtendedFunctionality> out = createListWithTwoExtendedFunctionalitySpies();
		MCR.addReturned(out);
		return out;
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

	private void assertExtendedFunctionalityIsCalledWithExpectedData(ExtendedFunctionalitySpy exSpy,
			ExtendedFunctionalityData expectedData) {
		String methodName2 = "useExtendedFunctionality";
		exSpy.MCR.assertMethodWasCalled(methodName2);
		exSpy.MCR.assertNumberOfCallsToMethod(methodName2, 1);
		ExtendedFunctionalityData data = (ExtendedFunctionalityData) exSpy.MCR
				.getValueForMethodNameAndCallNumberAndParameterName(methodName2, 0, "data");
		assertEquals(data.recordType, expectedData.recordType);
		assertEquals(data.recordId, expectedData.recordId);
		assertEquals(data.authToken, expectedData.authToken);
		assertEquals(data.user, expectedData.user);
		assertEquals(data.previouslyStoredTopDataGroup, expectedData.previouslyStoredTopDataGroup);
		assertEquals(data.dataGroup, expectedData.dataGroup);
	}
}
