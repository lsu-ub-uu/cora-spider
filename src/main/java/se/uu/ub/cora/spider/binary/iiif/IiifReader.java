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

import se.uu.ub.cora.spider.data.ResourceInputStream;

public interface IiifReader {

	/**
	 * readImage read an image using a IIIF protocol. All parameters are specified in the IIIF
	 * standard. @see <a href="https://iiif.io/api/image/">IIIF Image Api</a> Further desciption of
	 * the parameters can be found in the linked webpage.
	 * 
	 * @param identifier
	 *            The id of the record to read the image from.
	 * @param region
	 *            The rectangular portion of the underlying image
	 * @param size
	 *            The size parameter specifies the dimensions to which the extracted region is to be
	 *            scaled.
	 * @param rotation
	 *            The rotation parameter specifies mirroring and rotation.
	 * @param quality
	 *            The quality parameter determines whether the image is delivered in color,
	 *            grayscale or black and white.
	 * @param format
	 *            The format of the returned image is expressed as a suffix, mirroring common
	 *            filename extensions, at the end of the URI.
	 * @return A {@link ResourceInputStream} containing information about the requested image.
	 * @throws NF
	 */
	ResourceInputStream readImage(String identifier, String region, String size, String rotation,
			String quality, String format);

}
