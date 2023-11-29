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
package se.uu.ub.cora.spider.binary.internal;

import java.util.List;

public class MimeTypeToBinaryTypeImp implements MimeTypeToBinaryType {

	private List<String> documentList = List.of("application/pdf",
			"application/vnd.oasis.opendocument.text");

	private List<String> compressedList = List.of("application/zip", "application/x-tar",
			"application/x-7z-compressed");

	@Override
	public String toBinaryType(String mimeType) {
		if (mimeType.startsWith("image/")) {
			return "image";
		}
		if (mimeType.startsWith("audio/")) {
			return "sound";
		}
		if (mimeType.startsWith("video/")) {
			return "video";
		}
		if (documentList.contains(mimeType)) {
			return "document";
		}
		if (mimeType.equals("text/plain")) {
			return "text";
		}
		if (compressedList.contains(mimeType)) {
			return "compressed";
		}
		return "generic";
	}

}
