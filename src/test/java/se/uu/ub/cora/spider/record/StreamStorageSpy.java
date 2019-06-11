/*
 * Copyright 2016 Uppsala University Library
 * Copyright 2016 Olov McKie
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

import java.io.IOException;
import java.io.InputStream;

import se.uu.ub.cora.storage.StreamStorage;

public class StreamStorageSpy implements StreamStorage {

	public String streamId;
	public String dataDivider;
	public InputStream stream;
	public long size;

	@Override
	public long store(String streamId, String dataDivider, InputStream stream) {
		this.streamId = streamId;
		this.dataDivider = dataDivider;
		this.stream = stream;
		byte[] data = new byte[1024];
		int bytesRead;
		size = 0;
		try {
			bytesRead = stream.read(data);
			while (bytesRead != -1) {
				size += bytesRead;
				bytesRead = stream.read(data);
			}
			stream.close();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return size;
	}

	@Override
	public InputStream retrieve(String streamId, String dataDivider) {
		this.streamId = streamId;
		this.dataDivider = dataDivider;
		return stream;
	}

}
