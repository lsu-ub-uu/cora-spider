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
package se.uu.ub.cora.spider.binary.iiif.internal;

import se.uu.ub.cora.binary.BinaryProvider;
import se.uu.ub.cora.binary.iiif.IiifImageAdapter;
import se.uu.ub.cora.spider.binary.iiif.IiifReader;
import se.uu.ub.cora.spider.data.ResourceInputStream;

public class IiifImageReaderImp implements IiifReader {

	@Override
	public ResourceInputStream readImage(String identifier, String region, String size,
			String rotation, String quality, String format) {

		IiifImageAdapter iiifImageAdapter = BinaryProvider.getIiifImageAdapter();
		return null;

	}

}
