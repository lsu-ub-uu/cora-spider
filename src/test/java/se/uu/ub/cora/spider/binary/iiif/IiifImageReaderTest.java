/*
 * Copyright 2024 Uppsala University Library
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
package se.uu.ub.cora.spider.binary.iiif;

import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import se.uu.ub.cora.binary.BinaryProvider;
import se.uu.ub.cora.spider.binary.iiif.internal.IiifImageReaderImp;

public class IiifImageReaderTest {
	private IiifImageInstanceProviderSpy iiifImageAdapterInstanceProvider;

	private IiifImageReader reader;

	@BeforeMethod
	private void beforeMethod() {
		reader = new IiifImageReaderImp();
		iiifImageAdapterInstanceProvider = new IiifImageInstanceProviderSpy();
		BinaryProvider
				.onlyForTestSetIiifImageAdapterInstanceProvider(iiifImageAdapterInstanceProvider);
	}

	@Test
	public void testReadImage() throws Exception {

		reader.readImage("", null, null, null, null, null);

		iiifImageAdapterInstanceProvider.MCR.assertParameters("getIiifImageAdapter", 0);

	}

}
