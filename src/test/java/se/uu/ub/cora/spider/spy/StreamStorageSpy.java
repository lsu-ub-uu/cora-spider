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
package se.uu.ub.cora.spider.spy;

import java.io.InputStream;

import se.uu.ub.cora.storage.StreamStorage;
import se.uu.ub.cora.storage.spies.archive.InputStreamSpy;
import se.uu.ub.cora.testutils.mcr.MethodCallRecorder;
import se.uu.ub.cora.testutils.mrv.MethodReturnValues;

public class StreamStorageSpy implements StreamStorage {
	public MethodCallRecorder MCR = new MethodCallRecorder();
	public MethodReturnValues MRV = new MethodReturnValues();

	public StreamStorageSpy() {
		MCR.useMRV(MRV);
		MRV.setDefaultReturnValuesSupplier("store", () -> 100);
		MRV.setDefaultReturnValuesSupplier("retrieve", InputStreamSpy::new);
	}

	@Override
	public long store(String streamId, String dataDivider, InputStream stream) {
		return (long) MCR.addCallAndReturnFromMRV("streamId", streamId, "dataDivider", dataDivider,
				"stream", stream);
	}

	@Override
	public InputStream retrieve(String streamId, String dataDivider) {
		return (InputStream) MCR.addCallAndReturnFromMRV("streamId", streamId, "dataDivider",
				dataDivider);
	}

}
