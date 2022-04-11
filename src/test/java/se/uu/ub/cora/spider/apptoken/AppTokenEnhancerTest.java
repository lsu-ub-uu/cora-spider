/*
 * Copyright 2017, 2022 Uppsala University Library
 * Copyright 2022 Olov McKie
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
package se.uu.ub.cora.spider.apptoken;

import static org.testng.Assert.assertNotEquals;
import static org.testng.Assert.assertNotNull;
import static org.testng.Assert.assertTrue;

import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import se.uu.ub.cora.data.DataGroup;
import se.uu.ub.cora.data.DataProvider;
import se.uu.ub.cora.spider.extendedfunctionality.ExtendedFunctionalityData;
import se.uu.ub.cora.testspies.data.DataFactorySpy;
import se.uu.ub.cora.testspies.data.DataGroupSpy;

public class AppTokenEnhancerTest {

	private AppTokenEnhancer extendedFunctionality;
	private DataFactorySpy dataFactory;

	@BeforeMethod
	public void setUp() {
		dataFactory = new DataFactorySpy();
		DataProvider.onlyForTestSetDataFactory(dataFactory);
		extendedFunctionality = new AppTokenEnhancer();
	}

	@Test
	public void useExtendedFunctionality() {
		assertNotNull(extendedFunctionality);
	}

	@Test
	public void generateAndAddAppToken() {
		DataGroupSpy minimalGroup = new DataGroupSpy();

		callExtendedFunctionalityWithGroup(minimalGroup);

		dataFactory.MCR.assertParameters("factorAtomicUsingNameInDataAndValue", 0, "token");
		String tokenValue = getCreatedAtomicValueFromFactorySpyForCallNumber(0);
		var createdAtomic = dataFactory.MCR.getReturnValue("factorAtomicUsingNameInDataAndValue",
				0);
		assertTrue(tokenValue.length() > 30);
		minimalGroup.MCR.assertParameters("addChild", 0, createdAtomic);
	}

	private void callExtendedFunctionalityWithGroup(DataGroup minimalGroup) {
		ExtendedFunctionalityData data = new ExtendedFunctionalityData();
		data.authToken = "someToken";
		data.dataGroup = minimalGroup;
		extendedFunctionality.useExtendedFunctionality(data);
	}

	private String getCreatedAtomicValueFromFactorySpyForCallNumber(int callNumber) {
		return (String) dataFactory.MCR.getValueForMethodNameAndCallNumberAndParameterName(
				"factorAtomicUsingNameInDataAndValue", callNumber, "value");
	}

	@Test
	public void generateAndAddAppTokenDifferentTokens() {
		DataGroup minimalGroup = new DataGroupSpy();
		DataGroup minimalGroup2 = new DataGroupSpy();

		callExtendedFunctionalityWithGroup(minimalGroup);
		callExtendedFunctionalityWithGroup(minimalGroup2);

		String tokenValue = getCreatedAtomicValueFromFactorySpyForCallNumber(0);
		String tokenValue2 = getCreatedAtomicValueFromFactorySpyForCallNumber(1);

		assertNotEquals(tokenValue, tokenValue2);
	}
}
