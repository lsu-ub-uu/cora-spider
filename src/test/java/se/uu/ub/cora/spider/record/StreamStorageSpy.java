/*
 * Copyright 2016 Uppsala University Library
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

import se.uu.ub.cora.spider.stream.storage.StreamStorage;

public class StreamStorageSpy implements StreamStorage {

	public String streamId;
	public String dataDivider;
	public InputStream stream;

	@Override
	public void store(String streamId, String dataDivider, InputStream stream) {
		this.streamId = streamId;
		this.dataDivider = dataDivider;
		this.stream = stream;

	}

}
