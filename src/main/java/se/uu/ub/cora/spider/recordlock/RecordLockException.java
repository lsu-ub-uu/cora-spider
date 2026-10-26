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
 * RecordLockException is thrown when a record lock can not be acquired, for instance when the
 * maximum waiting time is reached or the waiting thread is interrupted.
 */
public class RecordLockException extends RuntimeException {

	private static final long serialVersionUID = 1L;

	private RecordLockException(String message) {
		super(message);
	}

	private RecordLockException(String message, Exception exception) {
		super(message, exception);
	}

	public static RecordLockException withMessage(String message) {
		return new RecordLockException(message);
	}

	public static RecordLockException withMessageAndException(String message, Exception exception) {
		return new RecordLockException(message, exception);
	}
}
