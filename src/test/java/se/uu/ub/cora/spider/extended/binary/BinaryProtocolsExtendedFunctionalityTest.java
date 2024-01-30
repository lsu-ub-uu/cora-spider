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
package se.uu.ub.cora.spider.extended.binary;

import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import se.uu.ub.cora.data.spies.DataGroupSpy;
import se.uu.ub.cora.data.spies.DataRecordSpy;
import se.uu.ub.cora.spider.extendedfunctionality.ExtendedFunctionality;
import se.uu.ub.cora.spider.extendedfunctionality.ExtendedFunctionalityData;

public class BinaryProtocolsExtendedFunctionalityTest {

	private ExtendedFunctionality extFunctionality;
	private ExtendedFunctionalityData data;
	private DataRecordSpy dataRecordSpy;
	private DataGroupSpy binaryGroup;

	@BeforeMethod
	public void beforeMethod() {
		extFunctionality = new BinaryProtocolsExtendedFunctionality();
		dataRecordSpy = new DataRecordSpy();
		binaryGroup = new DataGroupSpy();
		dataRecordSpy.MRV.setDefaultReturnValuesSupplier("getType", () -> "binary");
		dataRecordSpy.MRV.setDefaultReturnValuesSupplier("getDataGroup", () -> binaryGroup);

		data = new ExtendedFunctionalityData();

		data.dataRecord = dataRecordSpy;
	}

	@Test
	public void testSetUpProtocolIfBinaryPublished() throws Exception {
		binaryGroup.MRV.setSpecificReturnValuesSupplier("containsChildWithNameInData", () -> true,
				"jp2");

		extFunctionality.useExtendedFunctionality(data);

		dataRecordSpy.MCR.assertParameters("addProtocol", 0, "iiif");
	}

	@Test
	public void testSetUpProtocolIfBinaryNotPublished() throws Exception {

		extFunctionality.useExtendedFunctionality(data);

		dataRecordSpy.MCR.assertMethodNotCalled("addProtocol");
	}

}
