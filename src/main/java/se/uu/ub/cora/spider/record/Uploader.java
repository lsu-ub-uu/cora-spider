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
	 * upload is a method inteneded to upload resources into storage. The method must <b>ONLY</b>
	 * accept record type <b>binary</b>. At this moment the method can <b>ONLY</b> upload resources
	 * of type master. All resources of type master must be uploaded in the archive.
	 * 
	 * IT MIGHT happen like be below, please modify if necessary! The method returns the binary
	 * record, related to the uploaded resource, updated with the information of the uploaded
	 * resource.
	 * 
	 * WHAT happens if no binary record is found with given Type and Id?? MisuseException??
	 * 
	 * </p>
	 * If the authToken does not authenticate a {@link AuthenticationException} be thrown.
	 * </p>
	 * If the type is different than binary record type a {@link MisuseException} must be thrown,
	 * indicating that the resource can not be uploaded with that type.
	 * </p>
	 * If the inputStream does exits {@link DataMissingException} must be thrown, indicating no
	 * resource can be uploaded.
	 * </p>
	 * At this moment if resourceType is different than master a {@link DataMissingException} will
	 * be thrown
	 * 
	 * @param authToken
	 *            A String with the authToken of the user that uploads a resource
	 * @param type
	 *            A String with the name of the record type of the resource to upload
	 * @param id
	 *            A String with the record id of the resource to upload
	 * @param inputStream
	 *            An InputStrema with the resource to upload
	 * @param representation
	 *            A String with the name of the representation which is intended to upload the
	 *            resource to.
	 * @return A DataRecord with the binary record of the related uploded resource with updated data
	 *         of that resource.
	 */
	DataRecord upload(String authToken, String type, String id, InputStream inputStream,
			String representation);

}
