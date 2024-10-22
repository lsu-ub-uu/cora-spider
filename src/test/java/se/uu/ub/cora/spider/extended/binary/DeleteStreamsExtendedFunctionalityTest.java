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

import se.uu.ub.cora.data.spies.DataRecordGroupSpy;
import se.uu.ub.cora.spider.extendedfunctionality.ExtendedFunctionalityData;
import se.uu.ub.cora.spider.spy.SpiderDependencyProviderSpy;
import se.uu.ub.cora.spider.spy.StreamStorageSpy;

public class DeleteStreamsExtendedFunctionalityTest {

	private ExtendedFunctionalityData data;
	private DeleteStreamsExtendedFunctionality extFunc;
	private SpiderDependencyProviderSpy dependencyProvider;

	@BeforeMethod
	private void beforeMethod() {

		dependencyProvider = new SpiderDependencyProviderSpy();
		// TODO: Flytta StreamStorageSpy till storage spies och använd den istället
		streamStorage = new StreamStorageSpy();
		dependencyProvider.MRV.setDefaultReturnValuesSupplier("getRecordStorage",
				() -> streamStorage);

		data = new ExtendedFunctionalityData();
		data.dataRecordGroup = new DataRecordGroupSpy();

		extFunc = new DeleteStreamsExtendedFunctionality();
	}

	@Test
	public void testBinaryWithoutStreams() throws Exception {
		extFunc.useExtendedFunctionality(data);

	}
}
