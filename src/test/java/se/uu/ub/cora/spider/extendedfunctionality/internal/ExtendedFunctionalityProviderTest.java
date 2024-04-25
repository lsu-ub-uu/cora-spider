/*
 * Copyright 2020, 2021, 2024 Uppsala University Library
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

import static org.testng.Assert.assertSame;
import static se.uu.ub.cora.spider.extendedfunctionality.ExtendedFunctionalityPosition.CREATE_AFTER_AUTHORIZATION;

import java.util.List;

import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import se.uu.ub.cora.spider.extendedfunctionality.ExtendedFunctionality;
import se.uu.ub.cora.spider.extendedfunctionality.ExtendedFunctionalityPosition;
import se.uu.ub.cora.spider.extendedfunctionality.ExtendedFunctionalityProvider;

public class ExtendedFunctionalityProviderTest {

	private ExtendedFunctionalityProviderImp provider;
	private FactorySorterSpy factorySorterSpy;

	@BeforeMethod
	public void setUp() {
		factorySorterSpy = new FactorySorterSpy();
		provider = new ExtendedFunctionalityProviderImp(factorySorterSpy);
	}

	@Test
	public void testExtendedFactories() {
		assertSame(provider.getFactorySorterNeededForTest(), factorySorterSpy);
	}

	@Test
	public void testGetExtendedFunctionalityForPositionAndRecordType2() throws Exception {
		List<ExtendedFunctionality> functionality = ((ExtendedFunctionalityProvider) provider)
				.getFunctionalityForPositionAndRecordType(CREATE_AFTER_AUTHORIZATION,
						"someRecordType");

		assertCorrectCallAndAnswerFor(CREATE_AFTER_AUTHORIZATION, functionality, "someRecordType");
	}

	@Test
	public void testGetExtendedFunctionalityForPositionAndRecordType() throws Exception {
		List<ExtendedFunctionality> functionality = provider
				.getFunctionalityForPositionAndRecordType(CREATE_AFTER_AUTHORIZATION,
						"someRecordType");

		assertCorrectCallAndAnswerFor(CREATE_AFTER_AUTHORIZATION, functionality, "someRecordType");
	}

	@Test
	public void testGetFunctionalityForDeleteBefore() throws Exception {
		List<ExtendedFunctionality> functionality = provider
				.getFunctionalityBeforeDelete("someRecordType");

		assertCorrectCallAndAnswerFor(ExtendedFunctionalityPosition.DELETE_BEFORE, functionality,
				"someRecordType");
	}

	@Test
	public void testGetFunctionalityForDeleteAfter() throws Exception {
		List<ExtendedFunctionality> functionality = provider
				.getFunctionalityAfterDelete("someRecordType");

		assertCorrectCallAndAnswerFor(ExtendedFunctionalityPosition.DELETE_AFTER, functionality,
				"someRecordType");
	}

	private void assertCorrectCallAndAnswerFor(ExtendedFunctionalityPosition position,
			List<ExtendedFunctionality> functionality, String recordType) {
		assertSpyCalledWithCorrectPosition(position, recordType);
		assertFunctionalityIsTheOneReturnedFromSpy(functionality);
	}

	private void assertSpyCalledWithCorrectPosition(ExtendedFunctionalityPosition position,
			String recordType) {
		factorySorterSpy.MCR.assertParameters("getFunctionalityForPositionAndRecordType", 0,
				position, recordType);
	}

	private void assertFunctionalityIsTheOneReturnedFromSpy(
			List<ExtendedFunctionality> functionality) {
		factorySorterSpy.MCR.assertReturn("getFunctionalityForPositionAndRecordType", 0,
				functionality);
	}
}
