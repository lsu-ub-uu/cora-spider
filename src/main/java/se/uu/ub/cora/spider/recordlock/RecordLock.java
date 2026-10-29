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
 * RecordLock represents an acquired lock for a single record.
 * <p>
 * The lock is acquired before an instance is returned by a
 * {@link RecordLockInstanceProvider}. Closing the lock releases it and cleans up its resources.
 * <p>
 * <em>RecordLock uses the AutoCloseable structure to close used resources. Clients MUST use a
 * try-with-resources block or manually call the {@link #close()} method.</em>
 * <p>
 * Implementations of RecordLock are generally not threadsafe.
 */
public interface RecordLock extends AutoCloseable {

	/**
	 * close releases the acquired record lock and closes its underlying resources.
	 * <p>
	 * If an exception occurs while interacting with the underlying resources MUST an
	 * {@link RecordLockException} be thrown.
	 */
	@Override
	void close();
}
