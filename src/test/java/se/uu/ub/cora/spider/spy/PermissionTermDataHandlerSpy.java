/*
 * Copyright 2015, 2016, 2018, 2020, 2021, 2022, 2023, 2024, 2025 Uppsala University Library
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

import java.util.Collections;
import java.util.List;

import se.uu.ub.cora.bookkeeper.termcollector.PermissionTermDataHandler;
import se.uu.ub.cora.data.collected.PermissionTerm;
import se.uu.ub.cora.testutils.mcr.MethodCallRecorder;
import se.uu.ub.cora.testutils.mrv.MethodReturnValues;

public class PermissionTermDataHandlerSpy implements PermissionTermDataHandler {
	public MethodCallRecorder MCR = new MethodCallRecorder();
	public MethodReturnValues MRV = new MethodReturnValues();

	public PermissionTermDataHandlerSpy() {
		MCR.useMRV(MRV);
		MRV.setDefaultReturnValuesSupplier("getMixedPermissionTermValuesConsideringModeState",
				Collections::emptyList);
	}

	@Override
	public List<PermissionTerm> getMixedPermissionTermValuesConsideringModeState(
			List<PermissionTerm> previousPermissions, List<PermissionTerm> currentPermissions) {
		return (List<PermissionTerm>) MCR.addCallAndReturnFromMRV("previousPermissions",
				previousPermissions, "currentPermissions", currentPermissions);
	}
}