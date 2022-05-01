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

import se.uu.ub.cora.data.DataAtomic;
import se.uu.ub.cora.data.DataAtomicProvider;
import se.uu.ub.cora.data.DataGroup;
import se.uu.ub.cora.spider.data.DataAtomicFactorySpy;
import se.uu.ub.cora.spider.data.DataGroupSpy;
import se.uu.ub.cora.spider.extended.apptoken.AppTokenEnhancer;
import se.uu.ub.cora.spider.extendedfunctionality.ExtendedFunctionalityData;

public class AppTokenEnhancerTest {

	private AppTokenEnhancer extendedFunctionality;
	private DataAtomicFactorySpy dataAtomicFactory;

	@BeforeMethod
	public void setUp() {
		dataAtomicFactory = new DataAtomicFactorySpy();
		DataAtomicProvider.setDataAtomicFactory(dataAtomicFactory);
		extendedFunctionality = new AppTokenEnhancer();
	}

	@Test
	public void useExtendedFunctionality() {
		assertNotNull(extendedFunctionality);
	}

	@Test
	public void generateAndAddAppToken() {
		DataGroup minimalGroup = new DataGroupSpy("appToken");
		callExtendedFunctionalityWithGroup(minimalGroup);
		DataAtomic token = (DataAtomic) minimalGroup.getFirstChildWithNameInData("token");
		assertTrue(token.getValue().length() > 30);
	}

	private void callExtendedFunctionalityWithGroup(DataGroup minimalGroup) {
		ExtendedFunctionalityData data = new ExtendedFunctionalityData();
		data.authToken = "someToken";
		data.dataGroup = minimalGroup;
		extendedFunctionality.useExtendedFunctionality(data);
	}

	@Test
	public void generateAndAddAppTokenDifferentTokens() {
		DataGroup minimalGroup = new DataGroupSpy("appToken");
		callExtendedFunctionalityWithGroup(minimalGroup);
		DataAtomic token = (DataAtomic) minimalGroup.getFirstChildWithNameInData("token");

		DataGroup minimalGroup2 = new DataGroupSpy("appToken");
		callExtendedFunctionalityWithGroup(minimalGroup2);
		DataAtomic token2 = (DataAtomic) minimalGroup2.getFirstChildWithNameInData("token");

		assertNotEquals(token.getValue(), token2.getValue());
	}
}
