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

import se.uu.ub.cora.initialize.SelectOrder;

/**
 * RecordLockInstanceProvider is used to provide lock for Records
 */
public interface RecordLockInstanceProvider extends SelectOrder {

	/**
	 * getRecordLock returns an acquired lock for the record with the specified type and id. If the
	 * lock cannot be acquired according to the implementation's timeout and conflict policy, this
	 * method MUST throw a {@link RecordLockException} and MUST NOT return a lock.
	 * 
	 * @param recordType
	 *            the type of the record to lock
	 * @param recordId
	 *            the id of the record to lock
	 * @return an acquired lock for the specified record
	 * @throws RecordLockException
	 *             if the lock cannot be acquired
	 */
	RecordLock getRecordLock(String recordType, String recordId);
}
