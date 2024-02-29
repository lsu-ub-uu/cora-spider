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

import java.util.Map;

import se.uu.ub.cora.spider.authorization.AuthorizationException;
import se.uu.ub.cora.spider.record.RecordNotFoundException;

public interface IiifReader {

	/**
	 * readIiif proxies the call to an image server using IIIF protocol. All parameters are
	 * specified in the IIIF standard. @see <a href="https://iiif.io/api/image/">IIIF Image Api</a>
	 * Further desciption of the parameters can be found in the linked webpage.
	 * 
	 * @param identifier
	 *            The id of the record to read the image from.
	 * @param requestedUri
	 *            An uri with the requested iiif parameters
	 * @param method
	 *            The method requested.
	 * @param headersMap
	 *            Map with all requested headers
	 * @return An {@link IiifResponse} containing the response of the iiif server.
	 * 
	 * @throws {@link
	 *             RecordNotFoundException} if the record does not exists.
	 * @throws {@link
	 *             AuthorizationException} if the caller is not authorized to see the contents of
	 *             the record.
	 */
	IiifResponse readIiif(String identifier, String requestedUri, String method,
			Map<String, String> headersMap);

}
