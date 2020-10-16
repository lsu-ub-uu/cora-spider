/*
 * Copyright 2020 Uppsala University Library
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
package se.uu.ub.cora.spider.extended2;

import static org.testng.Assert.assertSame;
import static org.testng.Assert.assertTrue;

import java.util.ServiceLoader;

import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import se.uu.ub.cora.spider.extended.ExtendedFunctionalityProvider;

public class ExtendedFunctionalityInitializerTest {

	private ExtendedFunctionalityInitializerImp initializer;
	private ExtendedFunctionalityStarterSpy starterSpy;

	@BeforeMethod
	public void beforeMethod() {
		initializer = new ExtendedFunctionalityInitializerImp();
		starterSpy = new ExtendedFunctionalityStarterSpy();
		initializer.setStarterSpy(starterSpy);
	}

	@Test
	public void testStandardExtendedFunctionalityStarterCreatedOnStartup() throws Exception {
		initializer = new ExtendedFunctionalityInitializerImp();
		ExtendedFunctionalityStarter exImp = initializer.getStarter();
		assertTrue(exImp instanceof ExtendedFunctionalityStarterImp);
	}

	@Test
	public void testSetAndGetSpyExtendedFunctionalityStarter() throws Exception {
		assertSame(initializer.getStarter(), starterSpy);
	}

	@Test
	public void testExtendedFunctionalityIsSameAsStarterReturns() throws Exception {
		ExtendedFunctionalityProvider exImp = initializer.getExtendedFunctionalityProvider();
		assertSame(exImp, starterSpy.getExtendedFunctionalityProvider());
	}

	@Test
	public void testImplementation1AddedToStarter() throws Exception {
		ExtendedFunctionalityProvider exImp = initializer.getExtendedFunctionalityProvider();

		Iterable<ExtendedFunctionalityForCreateBeforeMetadataValidation> iterable = starterSpy
				.getCreateBeforeMetadataValidation();
		assertTrue(iterable instanceof ServiceLoader);
	}

	// @Test
	// public void testRecordStorageProviderImplementationsArePassedOnToStarter() {
	// TheRestModuleStarterSpy starter = startTheRestModuleInitializerWithStarterSpy();
	//
	// Iterable<RecordStorageProvider> iterable = starter.recordStorageProviderImplementations;
	// assertTrue(iterable instanceof ServiceLoader);
	// }

}
