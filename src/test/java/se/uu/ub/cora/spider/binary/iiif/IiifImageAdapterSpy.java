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

package se.uu.ub.cora.spider.binary.iiif;

import se.uu.ub.cora.binary.iiif.IiifImageAdapter;
import se.uu.ub.cora.binary.iiif.IiifImageParameters;
import se.uu.ub.cora.binary.iiif.IiifImageResponse;
import se.uu.ub.cora.testutils.mcr.MethodCallRecorder;
import se.uu.ub.cora.testutils.mrv.MethodReturnValues;

public class IiifImageAdapterSpy implements IiifImageAdapter {

	public MethodCallRecorder MCR = new MethodCallRecorder();
	public MethodReturnValues MRV = new MethodReturnValues();

	public IiifImageAdapterSpy() {
		MCR.useMRV(MRV);
		MRV.setDefaultReturnValuesSupplier("requestImage", () -> 0);
		MRV.setDefaultReturnValuesSupplier("getIiifImageAdapter", IiifImageAdapterSpy::new);
	}

	@Override
	public IiifImageResponse requestImage(IiifImageParameters iiifImageParameters) {
		return (IiifImageResponse) MCR.addCallAndReturnFromMRV("iiifImageParameters",
				iiifImageParameters);
	}

	@Override
	public IiifImageResponse requestInformation(String dataDivider, String identifier) {
		// TODO Auto-generated method stub
		return null;
	}
}
