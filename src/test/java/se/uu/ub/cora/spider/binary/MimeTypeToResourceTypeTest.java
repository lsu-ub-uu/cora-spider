/*
 * Copyright 2023 Uppsala University Library
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

import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import se.uu.ub.cora.spider.binary.internal.MimeTypeToBinaryType;
import se.uu.ub.cora.spider.binary.internal.MimeTypeToBinaryTypeImp;

public class MimeTypeToResourceTypeTest {

	private MimeTypeToBinaryType converter;

	@BeforeMethod
	private void beforeMethod() {
		converter = new MimeTypeToBinaryTypeImp();
	}

	@Test
	public void testInit() throws Exception {
		assertTrue(converter instanceof MimeTypeToBinaryType);
	}

	@Test
	public void testGeneric() throws Exception {
		assertMimeTypeConvertedToBinaryType("whatever", "generic");
	}

	@Test
	public void testImage() throws Exception {
		assertMimeTypeConvertedToBinaryType("image/whatever", "image");
	}

	private void assertMimeTypeConvertedToBinaryType(String mimeType, String expected) {
		String binaryType = converter.toBinaryType(mimeType);
		assertEquals(binaryType, expected);
	}

	@Test
	public void testVideo() throws Exception {
		assertMimeTypeConvertedToBinaryType("video/whatever", "video");
	}

	@Test
	public void testSound() throws Exception {
		assertMimeTypeConvertedToBinaryType("audio/whatever", "sound");
	}

	@Test
	public void testDocument() throws Exception {
		assertMimeTypeConvertedToBinaryType("application/pdf", "document");
		assertMimeTypeConvertedToBinaryType("application/vnd.oasis.opendocument.text", "document");
	}

	@Test
	public void testText() throws Exception {
		assertMimeTypeConvertedToBinaryType("text/plain", "text");
	}

	@Test
	public void testCompressed() throws Exception {
		assertMimeTypeConvertedToBinaryType("application/x-tar", "compressed");
		assertMimeTypeConvertedToBinaryType("application/zip", "compressed");
		assertMimeTypeConvertedToBinaryType("application/x-7z-compressed", "compressed");
	}
}
