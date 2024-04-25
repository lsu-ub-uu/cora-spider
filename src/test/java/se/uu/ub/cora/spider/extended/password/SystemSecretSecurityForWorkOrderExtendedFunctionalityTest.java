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
package se.uu.ub.cora.spider.extended.password;

import static org.testng.Assert.assertTrue;

import java.util.ArrayList;
import java.util.List;

import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import se.uu.ub.cora.data.DataRecordLink;
import se.uu.ub.cora.data.spies.DataGroupSpy;
import se.uu.ub.cora.data.spies.DataRecordLinkSpy;
import se.uu.ub.cora.spider.authorization.AuthorizationException;
import se.uu.ub.cora.spider.extendedfunctionality.ExtendedFunctionality;
import se.uu.ub.cora.spider.extendedfunctionality.ExtendedFunctionalityData;

public class SystemSecretSecurityForWorkOrderExtendedFunctionalityTest {

	private ExtendedFunctionalityData extData;
	private ExtendedFunctionality extFunc;

	@BeforeMethod
	private void beforeMethod() {
		extFunc = new SystemSecretSecurityForWorkOrderExtendedFunctionality();
		extData = new ExtendedFunctionalityData();

	}

	private void setUpExDataToContainLinksWithRecordTypesToSearch(String recordType) {
		DataGroupSpy workOrder = new DataGroupSpy();
		List<DataRecordLinkSpy> recordTypeLinks = createListOfRecordTypeLinksForTypes(recordType);
		workOrder.MRV.setSpecificReturnValuesSupplier("getChildrenOfTypeAndName",
				() -> recordTypeLinks, DataRecordLink.class, "recordType");

		extData.dataGroup = workOrder;
	}

	private List<DataRecordLinkSpy> createListOfRecordTypeLinksForTypes(String... recordTypes) {
		List<DataRecordLinkSpy> recordTypeLinks = new ArrayList<>();
		for (String recordType : recordTypes) {
			DataRecordLinkSpy recordLink = createRecordLink(recordType);
			recordTypeLinks.add(recordLink);
		}
		return recordTypeLinks;
	}

	private DataRecordLinkSpy createRecordLink(String recordType) {
		DataRecordLinkSpy linkSpy = new DataRecordLinkSpy();
		linkSpy.MRV.setDefaultReturnValuesSupplier("getLinkedRecordId", () -> recordType);
		return linkSpy;
	}

	@Test
	public void testDoNothingWhenWorkOrderIsNotLinkingToSystemSecret() throws Exception {
		setUpExDataToContainLinksWithRecordTypesToSearch("someRecordType");

		extFunc.useExtendedFunctionality(extData);

		assertTrue(true, "No error thrown if search does not handle systemSecret");
	}

	@Test(expectedExceptions = AuthorizationException.class, expectedExceptionsMessageRegExp = "Access denied")
	public void testExtendedFunctionalityThrowsErrorForWorkOrderLinkingToSystemSecret()
			throws Exception {
		setUpExDataToContainLinksWithRecordTypesToSearch("systemSecret");

		extFunc.useExtendedFunctionality(extData);
	}
}
