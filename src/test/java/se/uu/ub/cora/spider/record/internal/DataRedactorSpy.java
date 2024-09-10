/*
 * Copyright 2024 Olov McKie
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
package se.uu.ub.cora.spider.record.internal;

import java.util.Set;

import se.uu.ub.cora.bookkeeper.metadata.Constraint;
import se.uu.ub.cora.bookkeeper.recordpart.DataRedactor;
import se.uu.ub.cora.data.DataGroup;
import se.uu.ub.cora.data.spies.DataGroupSpy;
import se.uu.ub.cora.testutils.mcr.MethodCallRecorder;
import se.uu.ub.cora.testutils.mrv.MethodReturnValues;

public class DataRedactorSpy implements DataRedactor {
	public MethodCallRecorder MCR = new MethodCallRecorder();
	public MethodReturnValues MRV = new MethodReturnValues();

	public DataRedactorSpy() {
		MCR.useMRV(MRV);
		MRV.setDefaultReturnValuesSupplier("removeChildrenForConstraintsWithoutPermissions",
				DataGroupSpy::new);
		MRV.setDefaultReturnValuesSupplier("replaceChildrenForConstraintsWithoutPermissions",
				DataGroupSpy::new);
	}

	@Override
	public DataGroup removeChildrenForConstraintsWithoutPermissions(String metadataId,
			DataGroup dataGroup, Set<Constraint> recordPartConstraints,
			Set<String> recordPartPermissions) {
		return (DataGroup) MCR.addCallAndReturnFromMRV("metadataId", metadataId, "dataGroup",
				dataGroup, "recordPartConstraints", recordPartConstraints, "recordPartPermissions",
				recordPartPermissions);
	}

	@Override
	public DataGroup replaceChildrenForConstraintsWithoutPermissions(String metadataId,
			DataGroup originalDataGroup, DataGroup changedDataGroup,
			Set<Constraint> recordPartConstraints, Set<String> recordPartPermissions) {
		return (DataGroup) MCR.addCallAndReturnFromMRV("metadataId", metadataId,
				"originalDataGroup", originalDataGroup, "changedDataGroup", changedDataGroup,
				"recordPartConstraints", recordPartConstraints, "recordPartPermissions",
				recordPartPermissions);
	}
}