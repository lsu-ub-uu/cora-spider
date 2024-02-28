/*
 * Copyright 2024 Uppsala University Library
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
package se.uu.ub.cora.spider.testspies;

import java.util.List;
import java.util.Map;

import se.uu.ub.cora.spider.binary.iiif.IiifImageReader;
import se.uu.ub.cora.spider.binary.iiif.IiifImageResponse;
import se.uu.ub.cora.testutils.mcr.MethodCallRecorder;
import se.uu.ub.cora.testutils.mrv.MethodReturnValues;

public class IiifReaderSpy implements IiifImageReader {
	public MethodCallRecorder MCR = new MethodCallRecorder();
	public MethodReturnValues MRV = new MethodReturnValues();

	public IiifReaderSpy() {
		MCR.useMRV(MRV);
		// MRV.setDefaultReturnValuesSupplier("readImage", DataRecordSpy::new);
	}

	@Override
	public IiifImageResponse readIiif(String identifier, String requestedUri, String method,
			Map<String, List<Object>> headers) {
		// TODO Auto-generated method stub
		return null;
	}

}
