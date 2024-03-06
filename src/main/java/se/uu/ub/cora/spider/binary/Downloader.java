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

import se.uu.ub.cora.spider.record.MisuseException;

public interface Downloader {

	/**
	 * download is a method intended to download resources from storage. The only accepted type is
	 * <b>binary</b>. At this moment the method can <b>ONLY</b> download resources of type master.
	 * All resources of type master must be downloaded from archive.
	 * 
	 * The method will return an SpiderInputStream of the requested resource.
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
	 * If the binary record related to the requested resource does not exist at NotFound must be
	 * thrown.
	 * 
	 * At this moment if resourceType is different than master a {@link MisuseException} will be
	 * thrown
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
	 * @return
	 * 
	 */
	ResourceInputStream download(String authToken, String type, String id, String representation);

}
