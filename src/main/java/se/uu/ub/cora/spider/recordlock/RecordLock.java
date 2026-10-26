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
package se.uu.ub.cora.spider.recordlock;

/**
 * RecordLock acquires and releases locks on records, identified by type and id.
 * <p>
 * A lock is held from a successful call to {@link #acquire(String, String)} until
 * {@link #release(String, String)} is called for the same type and id. Callers SHOULD release the
 * lock in a finally block to make sure it is released even if an error occurs.
 * <p>
 * <em>RecordLock uses the AutoClosable structure to close used resources. Clients MUST use a
 * try-with-resources block or manually call the {@link #close()} method.</em>
 * <p>
 * Implementations of RecordLock are generally not threadsafe.
 */
public interface RecordLock extends AutoCloseable {

	/**
	 * acquire acquires a lock for the record with the specified type and id. If the lock is held by
	 * someone else, acquire MUST wait and retry until the lock is acquired or the maximum waiting
	 * time is reached.
	 *
	 * @param type
	 *            A String with the type of the record to lock
	 * @param id
	 *            A String with the id of the record to lock
	 * @throws RecordLockException
	 *             if the lock could not be acquired within the maximum waiting time, or if the
	 *             thread is interrupted while waiting to retry
	 */
	void acquire(String type, String id);

	/**
	 * release releases the lock for the record with the specified type and id. Releasing a lock
	 * that is not held MUST NOT throw an exception.
	 *
	 * @param type
	 *            A String with the type of the locked record
	 * @param id
	 *            A String with the id of the locked record
	 */
	void release(String type, String id);

	/**
	 * close closes the underlying resources.
	 * <p>
	 * If an exception occurs while interacting with the underlying resources MUST an
	 * {@link RecordLockException} be thrown.
	 */
	@Override
	void close();
}
