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
package se.uu.ub.cora.spider.resourceconvert;

public interface ResourceConvert {

	/**
	 * sendMessageForAnalyzingAndConvertingImages method configures a message to be sent to an
	 * specific mq rabbit exchange to handle analyze images and convertion to thumbnails and jp2
	 * representation.
	 * 
	 * @param dataDivider
	 *            A string with the data divider of the binary to convert.
	 * @param type
	 *            A string with the type of the binary to convert.
	 * @param id
	 *            A string with the id of the binary to convert.
	 * @param mimeType
	 */
	void sendMessageForAnalyzingAndConvertingImages(String dataDivider, String type, String id,
			String mimeType);

	/**
	 * 
	 * sendMessageForAnalyzeAndConvertToThumbnails method configures a message to be sent to an
	 * specific mq rabbit exchange to handle converting pdf to thumbnails.
	 * 
	 * @param dataDivider
	 *            A string with the data divider of the binary to convert.
	 * @param type
	 *            A string with the type of the binary to convert.
	 * @param id
	 *            A string with the id of the binary to convert.
	 */
	void sendMessageToConvertPdfToThumbnails(String dataDivider, String type, String id);

}
