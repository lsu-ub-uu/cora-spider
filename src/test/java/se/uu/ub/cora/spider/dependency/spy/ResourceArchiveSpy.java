/*
 * Copyright 2022 Uppsala University Library
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
package se.uu.ub.cora.spider.dependency.spy;

import java.io.InputStream;

import se.uu.ub.cora.storage.archive.ResourceArchive;

public class ResourceArchiveSpy implements ResourceArchive {

	@Override
	public void create(String dataDivider, String type, String id, InputStream resource, String mimeType) {
		// TODO Auto-generated method stub

	}

	@Override
	public InputStream read(String dataDivider, String type, String id) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void update(String dataDivider, String type, String id, InputStream resource, String mimeType) {
		// TODO Auto-generated method stub

	}

	@Override
	public void delete(String dataDivider, String type, String id) {
		// TODO Auto-generated method stub

	}

}
