/*
 * Copyright 2026 Uppsala University Library
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
package se.uu.ub.cora.spider.recordlock.internal;

import static org.testng.Assert.assertNotNull;
import static org.testng.Assert.assertTrue;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;

import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import se.uu.ub.cora.initialize.ModuleInitializer;
import se.uu.ub.cora.initialize.ModuleInitializerImp;
import se.uu.ub.cora.initialize.spies.ModuleInitializerSpy;
import se.uu.ub.cora.logger.LoggerFactory;
import se.uu.ub.cora.logger.LoggerProvider;
import se.uu.ub.cora.logger.spies.LoggerFactorySpy;
import se.uu.ub.cora.spider.recordlock.RecordLock;
import se.uu.ub.cora.spider.recordlock.RecordLockInstanceProvider;
import se.uu.ub.cora.spider.recordlock.RecordLockInstanceProviderSpy;

public class RecordLockProviderTest {
	private LoggerFactory loggerFactory = new LoggerFactorySpy();
	private ModuleInitializerSpy moduleInitializerSpy;
	private RecordLockInstanceProviderSpy instanceProviderSpy;

	@BeforeMethod
	public void beforeMethod() {
		LoggerProvider.setLoggerFactory(loggerFactory);
	}

	private void setupModuleInstanceProviderToReturnRecordLockFactorySpy() {
		moduleInitializerSpy = new ModuleInitializerSpy();
		instanceProviderSpy = new RecordLockInstanceProviderSpy();
		moduleInitializerSpy.MRV.setDefaultReturnValuesSupplier(
				"loadOneImplementationBySelectOrder", () -> instanceProviderSpy);
		RecordLockProvider.onlyForTestSetModuleInitializer(moduleInitializerSpy);
	}

	@AfterMethod
	private void afterMethod() {
		RecordLockProvider.onlyForTestSetRecordLockInstanceProvider(null);
	}

	// Force test to run first
	@Test(priority = -1)
	public void testDefaultInitializerIsModuleInitalizer() {
		ModuleInitializer initializer = RecordLockProvider.onlyForTestGetModuleInitializer();
		assertNotNull(initializer);
		assertTrue(initializer instanceof ModuleInitializerImp);
	}

	@Test
	public void testPrivateConstructor() throws Exception {
		Constructor<RecordLockProvider> constructor = RecordLockProvider.class
				.getDeclaredConstructor();
		assertTrue(Modifier.isPrivate(constructor.getModifiers()));
	}

	@Test(expectedExceptions = InvocationTargetException.class)
	public void testPrivateConstructorInvoke() throws Exception {
		Constructor<RecordLockProvider> constructor = RecordLockProvider.class
				.getDeclaredConstructor();
		constructor.setAccessible(true);
		constructor.newInstance();
	}

	@Test
	public void testGetRecordLockIsSynchronized_toPreventProblemsWithFindingImplementations()
			throws Exception {
		Method getRecordLock = RecordLockProvider.class.getMethod("getRecordLock");
		assertTrue(Modifier.isSynchronized(getRecordLock.getModifiers()));
	}

	@Test
	public void testGetRecordLockUsesModuleInitializerToGetFactory() {
		setupModuleInstanceProviderToReturnRecordLockFactorySpy();

		RecordLock recordLock = RecordLockProvider.getRecordLock();

		moduleInitializerSpy.MCR.assertParameters("loadOneImplementationBySelectOrder", 0,
				RecordLockInstanceProvider.class);
		instanceProviderSpy.MCR.assertReturn("getRecordLock", 0, recordLock);
	}

	@Test
	public void testOnlyForTestSetRecordLockInstanceProvider() {
		RecordLockInstanceProviderSpy instanceProviderSpy2 = new RecordLockInstanceProviderSpy();
		RecordLockProvider.onlyForTestSetRecordLockInstanceProvider(instanceProviderSpy2);

		RecordLock recordLock = RecordLockProvider.getRecordLock();

		instanceProviderSpy2.MCR.assertReturn("getRecordLock", 0, recordLock);
	}

	@Test
	public void testMultipleCallsToGetRecordLockOnlyLoadsImplementationsOnce() {
		setupModuleInstanceProviderToReturnRecordLockFactorySpy();

		RecordLockProvider.getRecordLock();
		RecordLockProvider.getRecordLock();
		RecordLockProvider.getRecordLock();
		RecordLockProvider.getRecordLock();

		moduleInitializerSpy.MCR.assertNumberOfCallsToMethod("loadOneImplementationBySelectOrder",
				1);
	}

}
