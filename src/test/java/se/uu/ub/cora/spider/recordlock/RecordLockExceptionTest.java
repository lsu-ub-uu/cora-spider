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

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertNull;
import static org.testng.Assert.assertSame;
import static org.testng.Assert.assertTrue;

import org.testng.annotations.Test;

public class RecordLockExceptionTest {
	private static final String MESSAGE = "message";

	@Test
	public void testIsRuntimeException() {
		RecordLockException exception = RecordLockException.withMessage(MESSAGE);

		assertTrue(exception instanceof RuntimeException);
	}

	@Test
	public void testWithMessage() {
		RecordLockException exception = RecordLockException.withMessage(MESSAGE);

		assertEquals(exception.getMessage(), MESSAGE);
		assertNull(exception.getCause());
	}

	@Test
	public void testWithMessageAndException() {
		Exception cause = new RuntimeException();

		RecordLockException exception = RecordLockException.withMessageAndException(MESSAGE, cause);

		assertEquals(exception.getMessage(), MESSAGE);
		assertSame(exception.getCause(), cause);
	}
}
