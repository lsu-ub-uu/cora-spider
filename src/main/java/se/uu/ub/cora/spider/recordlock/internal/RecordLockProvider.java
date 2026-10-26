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

import se.uu.ub.cora.initialize.ModuleInitializer;
import se.uu.ub.cora.initialize.ModuleInitializerImp;
import se.uu.ub.cora.initialize.SelectOrder;
import se.uu.ub.cora.spider.recordlock.RecordLock;
import se.uu.ub.cora.spider.recordlock.RecordLockException;
import se.uu.ub.cora.spider.recordlock.RecordLockInstanceProvider;

/**
 * RecordLockProvider is used to provide access to lock for Records.
 * </p>
 * Implementing {@link RecordLockInstanceProvider}s are found using javas module system, and the one
 * with the higest {@link SelectOrder} is used to provide access to record lock.
 */
public class RecordLockProvider {

	private static ModuleInitializer moduleInitializer = new ModuleInitializerImp();
	private static RecordLockInstanceProvider recordLockInstanceProvider;

	private RecordLockProvider() {
		// prevent call to constructor
		throw new UnsupportedOperationException();
	}

	/**
	 * getRecordLock returns an acquired lock for the record with the specified type and id.
	 * </p>
	 * If the lock cannot be acquired according to the provider's timeout and conflict policy, an
	 * exception is thrown and no lock is returned. Code using the returned {@link RecordLock}
	 * instance MUST consider the returned instance as NOT thread safe and close it to release the
	 * lock.
	 * 
	 * @param recordType
	 *            the type of the record to lock
	 * @param recordId
	 *            the id of the record to lock
	 * @return an acquired lock for the specified record
	 * @throws RecordLockException
	 *             if the lock cannot be acquired
	 */
	public static RecordLock getRecordLock(String recordType, String recordId) {
		locateAndChooseRecordLockInstanceProvider();
		return recordLockInstanceProvider.getRecordLock(recordType, recordId);
	}

	private static synchronized void locateAndChooseRecordLockInstanceProvider() {
		if (recordLockInstanceProvider == null) {
			recordLockInstanceProvider = moduleInitializer
					.loadOneImplementationBySelectOrder(RecordLockInstanceProvider.class);
		}
	}

	/**
	 * onlyForTestSetRecordLockInstanceProvider sets a RecordLockInstanceProvider that will be used
	 * to return instances for the {@link #getRecordLock(String, String)} method. This possibility
	 * to set a RecordLockInstanceProvider is provided to enable testing of getting a record lock in
	 * other classes and is not intented to be used in production.
	 * <p>
	 * The RecordLockInstanceProvider to use in production should be provided through an
	 * implementation of {@link RecordLockInstanceProvider} in a seperate java module.
	 * 
	 * @param recordLockInstanceProvider
	 *            A recordLockInstanceProvider to use to return recordLock instances for testing
	 */
	public static void onlyForTestSetRecordLockInstanceProvider(
			RecordLockInstanceProvider recordLockInstanceProvider) {
		RecordLockProvider.recordLockInstanceProvider = recordLockInstanceProvider;
	}

	static ModuleInitializer onlyForTestGetModuleInitializer() {
		return moduleInitializer;
	}

	static void onlyForTestSetModuleInitializer(ModuleInitializer moduleInitializer) {
		RecordLockProvider.moduleInitializer = moduleInitializer;
	}

}
