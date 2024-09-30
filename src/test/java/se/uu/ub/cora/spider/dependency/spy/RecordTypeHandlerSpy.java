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
package se.uu.ub.cora.spider.dependency.spy;

import java.util.Collections;
import java.util.List;
import java.util.Set;

import se.uu.ub.cora.bookkeeper.metadata.Constraint;
import se.uu.ub.cora.bookkeeper.recordtype.RecordTypeHandler;
import se.uu.ub.cora.bookkeeper.recordtype.Unique;
import se.uu.ub.cora.testutils.mcr.MethodCallRecorder;
import se.uu.ub.cora.testutils.mrv.MethodReturnValues;

public class RecordTypeHandlerSpy implements RecordTypeHandler {

	public MethodCallRecorder MCR = new MethodCallRecorder();
	public MethodReturnValues MRV = new MethodReturnValues();

	public RecordTypeHandlerSpy() {
		MCR.useMRV(MRV);
		MRV.setDefaultReturnValuesSupplier("shouldAutoGenerateId", () -> false);
		MRV.setDefaultReturnValuesSupplier("getCreateDefinitionId",
				() -> "fakeCreateMetadataIdFromRecordTypeHandlerSpy");
		MRV.setDefaultReturnValuesSupplier("getUpdateDefinitionId",
				() -> "fakeUpdateMetadataIdFromRecordTypeHandlerSpy");
		MRV.setDefaultReturnValuesSupplier("getDefinitionId",
				() -> "fakeDefMetadataIdFromRecordTypeHandlerSpy");
		MRV.setDefaultReturnValuesSupplier("isPublicForRead", () -> false);
		MRV.setDefaultReturnValuesSupplier("hasRecordPartReadConstraint", () -> false);
		MRV.setDefaultReturnValuesSupplier("getReadRecordPartConstraints",
				() -> Collections.emptySet());
		MRV.setDefaultReturnValuesSupplier("hasRecordPartWriteConstraint", () -> false);
		MRV.setDefaultReturnValuesSupplier("getWriteRecordPartConstraints",
				() -> Collections.emptySet());
		MRV.setDefaultReturnValuesSupplier("getUpdateWriteRecordPartConstraints",
				() -> Collections.emptySet());
		MRV.setDefaultReturnValuesSupplier("representsTheRecordTypeDefiningSearches", () -> false);
		MRV.setDefaultReturnValuesSupplier("representsTheRecordTypeDefiningRecordTypes",
				() -> false);
		MRV.setDefaultReturnValuesSupplier("hasLinkedSearch", () -> false);
		MRV.setDefaultReturnValuesSupplier("getSearchId",
				() -> "fakeUpdateSearchIdFromRecordTypeHandlerSpy");
		MRV.setDefaultReturnValuesSupplier("hasRecordPartCreateConstraint", () -> false);
		MRV.setDefaultReturnValuesSupplier("getCreateWriteRecordPartConstraints",
				() -> Collections.emptySet());
		MRV.setDefaultReturnValuesSupplier("getRecordTypeId",
				() -> "fakeRecordTypeIdFromRecordTypeHandlerSpy");
		MRV.setDefaultReturnValuesSupplier("storeInArchive", () -> false);
		MRV.setDefaultReturnValuesSupplier("getCombinedIdForIndex",
				() -> List.of("someCombinedIdFromSpy"));
		MRV.setDefaultReturnValuesSupplier("getUniqueDefinitions", () -> Collections.emptyList());
	}

	@Override
	public boolean shouldAutoGenerateId() {
		return (boolean) MCR.addCallAndReturnFromMRV();
	}

	@Override
	public String getCreateDefinitionId() {
		return (String) MCR.addCallAndReturnFromMRV();
	}

	@Override
	public String getUpdateDefinitionId() {
		return (String) MCR.addCallAndReturnFromMRV();
	}

	@Override
	public String getDefinitionId() {
		return (String) MCR.addCallAndReturnFromMRV();
	}

	@Override
	public boolean isPublicForRead() {
		return (boolean) MCR.addCallAndReturnFromMRV();
	}

	@Override
	public boolean hasRecordPartReadConstraint() {
		return (boolean) MCR.addCallAndReturnFromMRV();
	}

	@Override
	public Set<Constraint> getReadRecordPartConstraints() {
		return (Set<Constraint>) MCR.addCallAndReturnFromMRV();
	}

	@Override
	public boolean hasRecordPartWriteConstraint() {
		return (boolean) MCR.addCallAndReturnFromMRV();
	}

	@Override
	public Set<Constraint> getUpdateWriteRecordPartConstraints() {
		return (Set<Constraint>) MCR.addCallAndReturnFromMRV();
	}

	@Override
	public boolean representsTheRecordTypeDefiningSearches() {
		return (boolean) MCR.addCallAndReturnFromMRV();
	}

	@Override
	public boolean representsTheRecordTypeDefiningRecordTypes() {
		return (boolean) MCR.addCallAndReturnFromMRV();
	}

	@Override
	public boolean hasLinkedSearch() {
		return (boolean) MCR.addCallAndReturnFromMRV();
	}

	@Override
	public String getSearchId() {
		return (String) MCR.addCallAndReturnFromMRV();
	}

	@Override
	public boolean hasRecordPartCreateConstraint() {
		return (boolean) MCR.addCallAndReturnFromMRV();
	}

	@Override
	public Set<Constraint> getCreateWriteRecordPartConstraints() {
		return (Set<Constraint>) MCR.addCallAndReturnFromMRV();
	}

	@Override
	public String getRecordTypeId() {
		return (String) MCR.addCallAndReturnFromMRV();
	}

	@Override
	public boolean storeInArchive() {
		return (boolean) MCR.addCallAndReturnFromMRV();
	}

	@Override
	public List<Unique> getUniqueDefinitions() {
		return (List<Unique>) MCR.addCallAndReturnFromMRV();
	}
}
