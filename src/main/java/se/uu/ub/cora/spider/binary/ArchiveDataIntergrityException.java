/*
 * Copyright 2015, 2019 Uppsala University Library
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

public class ArchiveDataIntergrityException extends RuntimeException {

	private static final long serialVersionUID = -2867876073765359936L;

	public ArchiveDataIntergrityException(String message) {
		super(message);
	}

	public ArchiveDataIntergrityException(String message, Exception exception) {
		super(message, exception);
	}

	public static ArchiveDataIntergrityException withMessage(String message) {
		return new ArchiveDataIntergrityException(message);
	}

	public static ArchiveDataIntergrityException withMessageAndException(String message,
			Exception e) {
		return new ArchiveDataIntergrityException(message, e);
	}
}
