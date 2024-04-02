/*
 * Copyright 2022 Uppsala University Library
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
package se.uu.ub.cora.spider.binary;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertTrue;

import org.testng.annotations.Test;

public class ArchiveDataIntegrityExceptionTest {
	@Test
	public void testInit() {
		String message = "message";
		ArchiveDataIntergrityException exception = ArchiveDataIntergrityException
				.withMessage(message);
		assertEquals(exception.getMessage(), "message");
		assertTrue(exception instanceof RuntimeException);
	}

	@Test
	public void testMessageAndError() throws Exception {
		String message = "message";
		Exception exception = new RuntimeException();
		ArchiveDataIntergrityException storageException = ArchiveDataIntergrityException
				.withMessageAndException(message, exception);
		assertEquals(storageException.getMessage(), "message");
		assertEquals(storageException.getCause(), exception);
	}
}
