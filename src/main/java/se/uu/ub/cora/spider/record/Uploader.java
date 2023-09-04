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

package se.uu.ub.cora.spider.record;

import java.io.InputStream;

import se.uu.ub.cora.data.DataRecord;

public interface Uploader {

	/**
	 * upload is a method inteneded to upload resources into Cora. The method can <b>ONLY</b> be
	 * used with type equal to <b>binary</b>. When this method is called, it should update the
	 * metadata in under the given resourceType in binary record. It should be updated with data
	 * related to the uploaded resource.
	 * 
	 * </p>
	 * If the authToken does not authenticate a {@link AuthenticationException} be thrown.
	 * </p>
	 * If the type is different than binary record type a {@link MisuseException} be thrown,
	 * indicating that the resource can not be uploaded with that type.
	 * </p>
	 * If resourceType does not exists for the validationType then a {@link DataException} willbe
	 * thrown.
	 * 
	 * AuthenticationException
	 * 
	 * @param authToken
	 *            A String with the authToken of the user that uploads a resource
	 * @param type
	 *            A String with the name of the record type of the resource to upload
	 * @param id
	 *            A String with the record id of the resource to upload
	 * @param inputStream
	 *            An InputStrema with the resource to upload
	 * @param resourceType
	 *            A String with the name of the resourceType which is intended to upload the
	 *            resource to.
	 * @return
	 */
	DataRecord upload(String authToken, String type, String id, InputStream inputStream,
			String resourceType);

}
