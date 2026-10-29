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

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertFalse;
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
import se.uu.ub.cora.spider.recordlock.RecordLockException;
import se.uu.ub.cora.spider.recordlock.RecordLockInstanceProvider;
import se.uu.ub.cora.spider.recordlock.RecordLockInstanceProviderSpy;

public class RecordLockProviderTest {
	private static final String RECORD_TYPE = "someRecordType";
	private static final String RECORD_ID = "someRecordId";

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
	public void testGetRecordLockDoesNotSynchronizeLockAcquisition()
			throws Exception {
		Method getRecordLock = RecordLockProvider.class.getMethod("getRecordLock", String.class,
				String.class);
		assertFalse(Modifier.isSynchronized(getRecordLock.getModifiers()));
	}

	@Test
	public void testProviderSelectionIsSynchronized() throws Exception {
		Method locateAndChooseProvider = RecordLockProvider.class
				.getDeclaredMethod("locateAndChooseRecordLockInstanceProvider");
		assertTrue(Modifier.isSynchronized(locateAndChooseProvider.getModifiers()));
	}

	@Test
	public void testGetRecordLockAcceptsRecordIdentity() throws Exception {
		Method getRecordLock = RecordLockProvider.class.getMethod("getRecordLock", String.class,
				String.class);

		assertEquals(getRecordLock.getParameterCount(), 2);
	}

	@Test
	public void testGetRecordLockPassesRecordIdentityToInstanceProvider() {
		setupModuleInstanceProviderToReturnRecordLockFactorySpy();

		RecordLockProvider.getRecordLock(RECORD_TYPE, RECORD_ID);

		instanceProviderSpy.MCR.assertParameters("getRecordLock", 0, RECORD_TYPE, RECORD_ID);
	}

	@Test(expectedExceptions = RecordLockException.class)
	public void testGetRecordLockPropagatesAcquisitionFailure() {
		setupModuleInstanceProviderToReturnRecordLockFactorySpy();
		instanceProviderSpy.MRV.setAlwaysThrowException("getRecordLock",
				RecordLockException.withMessage("could not acquire lock"));

		RecordLockProvider.getRecordLock(RECORD_TYPE, RECORD_ID);
	}

	@Test
	public void testRecordLockOnlyExposesClose() {
		Method[] recordLockMethods = RecordLock.class.getDeclaredMethods();

		assertEquals(recordLockMethods.length, 1);
		assertEquals(recordLockMethods[0].getName(), "close");
	}

	@Test
	public void testGetRecordLockUsesModuleInitializerToGetFactory() {
		setupModuleInstanceProviderToReturnRecordLockFactorySpy();

		RecordLock recordLock = RecordLockProvider.getRecordLock(RECORD_TYPE, RECORD_ID);

		moduleInitializerSpy.MCR.assertParameters("loadOneImplementationBySelectOrder", 0,
				RecordLockInstanceProvider.class);
		instanceProviderSpy.MCR.assertParameters("getRecordLock", 0, RECORD_TYPE, RECORD_ID);
		instanceProviderSpy.MCR.assertReturn("getRecordLock", 0, recordLock);
	}

	@Test
	public void testOnlyForTestSetRecordLockInstanceProvider() {
		RecordLockInstanceProviderSpy instanceProviderSpy2 = new RecordLockInstanceProviderSpy();
		RecordLockProvider.onlyForTestSetRecordLockInstanceProvider(instanceProviderSpy2);

		RecordLock recordLock = RecordLockProvider.getRecordLock(RECORD_TYPE, RECORD_ID);

		instanceProviderSpy2.MCR.assertParameters("getRecordLock", 0, RECORD_TYPE, RECORD_ID);
		instanceProviderSpy2.MCR.assertReturn("getRecordLock", 0, recordLock);
	}

	@Test
	public void testMultipleCallsToGetRecordLockOnlyLoadsImplementationsOnce() {
		setupModuleInstanceProviderToReturnRecordLockFactorySpy();

		RecordLockProvider.getRecordLock(RECORD_TYPE, RECORD_ID);
		RecordLockProvider.getRecordLock(RECORD_TYPE, RECORD_ID);
		RecordLockProvider.getRecordLock(RECORD_TYPE, RECORD_ID);
		RecordLockProvider.getRecordLock(RECORD_TYPE, RECORD_ID);

		moduleInitializerSpy.MCR.assertNumberOfCallsToMethod("loadOneImplementationBySelectOrder",
				1);
	}

}
