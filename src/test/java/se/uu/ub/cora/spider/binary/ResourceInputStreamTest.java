/*
 * Copyright 2016, 2023 Uppsala University Library
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

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

import org.testng.annotations.Test;

import se.uu.ub.cora.spider.binary.ResourceInputStream;

public class ResourceInputStreamTest {
	@Test
	public void testContent() {
		InputStream stream = new ByteArrayInputStream("a string".getBytes(StandardCharsets.UTF_8));
		ResourceInputStream spiderBinaryStream = ResourceInputStream.withNameSizeInputStream(
				"testName", 1234567890, "application/octet-stream", stream);

		assertEquals(spiderBinaryStream.name, "testName");
		assertEquals(spiderBinaryStream.size, 1234567890);
		assertEquals(spiderBinaryStream.stream, stream);
		assertEquals(spiderBinaryStream.mimeType, "application/octet-stream");
	}
}
