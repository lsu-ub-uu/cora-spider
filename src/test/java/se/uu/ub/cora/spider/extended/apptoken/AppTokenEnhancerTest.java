/*
 * Copyright 2017, 2022 Uppsala University Library
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
package se.uu.ub.cora.spider.extended.apptoken;

import static org.testng.Assert.assertNotEquals;
import static org.testng.Assert.assertNotNull;
import static org.testng.Assert.assertTrue;

import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import se.uu.ub.cora.data.DataGroup;
import se.uu.ub.cora.data.DataProvider;
import se.uu.ub.cora.spider.extendedfunctionality.ExtendedFunctionalityData;

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

		var generatedAtomicToken = minimalGroup.MCR
				.getValueForMethodNameAndCallNumberAndParameterName("addChild", 0, "dataChild");
		dataFactory.MCR.assertReturn("factorAtomicUsingNameInDataAndValue", 0,
				generatedAtomicToken);
		dataFactory.MCR.assertParameters("factorAtomicUsingNameInDataAndValue", 0, "token");
		String tokenValue = (String) dataFactory.MCR
				.getValueForMethodNameAndCallNumberAndParameterName(
						"factorAtomicUsingNameInDataAndValue", 0, "value");
		assertTrue(tokenValue.length() > 30);

	}

	private void callExtendedFunctionalityWithGroup(DataGroup minimalGroup) {
		ExtendedFunctionalityData data = new ExtendedFunctionalityData();
		data.authToken = "someToken";
		data.dataGroup = minimalGroup;
		extendedFunctionality.useExtendedFunctionality(data);
	}

	@Test
	public void generateAndAddAppTokenDifferentTokens() {
		DataGroupSpy minimalGroup1 = new DataGroupSpy();
		callExtendedFunctionalityWithGroup(minimalGroup1);

		DataGroupSpy minimalGroup2 = new DataGroupSpy();
		callExtendedFunctionalityWithGroup(minimalGroup2);

		String tokenValue1 = (String) dataFactory.MCR
				.getValueForMethodNameAndCallNumberAndParameterName(
						"factorAtomicUsingNameInDataAndValue", 0, "value");
		String tokenValue2 = (String) dataFactory.MCR
				.getValueForMethodNameAndCallNumberAndParameterName(
						"factorAtomicUsingNameInDataAndValue", 1, "value");

		assertNotEquals(tokenValue1, tokenValue2);
	}
}
