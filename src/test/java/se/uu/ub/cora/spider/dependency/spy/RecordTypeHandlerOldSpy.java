/*
 * Copyright 2020 Uppsala University Library
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

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import se.uu.ub.cora.bookkeeper.metadata.Constraint;
import se.uu.ub.cora.bookkeeper.recordtype.RecordTypeHandler;
import se.uu.ub.cora.bookkeeper.recordtype.Unique;
import se.uu.ub.cora.testutils.mcr.MethodCallRecorder;
import se.uu.ub.cora.testutils.mrv.MethodReturnValues;

public class RecordTypeHandlerOldSpy implements RecordTypeHandler {

	public MethodCallRecorder MCR = new MethodCallRecorder();
	public MethodReturnValues MRV = new MethodReturnValues();

	/**
	 * isPublicForRead is default false, if set to true, the recordType is considered totaly public
	 * and no security checks are supposed to be done for reading
	 */
	public boolean isPublicForRead = false;

	/**
	 * recordPartConstraints is default empty, can be set to "readWrite" or "write" to change
	 * behavior of {@link #hasRecordPartReadConstraint()} and
	 * {@link #hasRecordPartWriteConstraint()}
	 */
	public String recordPartConstraint = "";
	public Set<Constraint> writeStringConstraints = new HashSet<Constraint>();
	public Set<Constraint> writeConstraints = new HashSet<Constraint>();

	public String id = "id";
	public String parentId = "someParentId";

	public boolean isChildOfBinary = false;
	public boolean representsTheRecordTypeDefiningSearches = false;
	public boolean representsTheRecordTypeDefiningRecordTypes = false;

	public boolean hasLinkedSearch = false;

	public String returnedSearchId = "someSearchId";

	// /**
	// * shouldAutoGenerateId is default false, if set to true will method shouldAutoGenerateId()
	// * return true instead of false.
	// */
	// public boolean shouldAutoGenerateId = false;

	public List<String> listOfimplementingTypesIds = new ArrayList<>();

	public RecordTypeHandlerOldSpy() {
		MCR.useMRV(MRV);
		MRV.setDefaultReturnValuesSupplier("shouldAutoGenerateId", () -> false);
		MRV.setDefaultReturnValuesSupplier("getCreateDefinitionId",
				() -> "fakeCreateMetadataIdFromRecordTypeHandlerSpy");
		MRV.setDefaultReturnValuesSupplier("getDefinitionId",
				() -> "fakeDefMetadataIdFromRecordTypeHandlerSpy");
		MRV.setDefaultReturnValuesSupplier("getUpdateDefinitionId",
				() -> "fakeUpdateMetadataIdFromRecordTypeHandlerSpy");
		MRV.setDefaultReturnValuesSupplier("storeInArchive", () -> false);
		MRV.setDefaultReturnValuesSupplier("getRecordTypeId",
				() -> "fakeRecordTypeIdFromRecordTypeHandlerSpy");
		MRV.setDefaultReturnValuesSupplier("getCombinedIdForIndex",
				() -> List.of("someCombinedIdFromSpy"));
		MRV.setDefaultReturnValuesSupplier("getUniqueDefinitions", () -> Collections.emptyList());
		MRV.setDefaultReturnValuesSupplier("isPublicForRead", () -> isPublicForRead);
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
		MCR.addCall();
		boolean answer = false;
		if ("readWrite".equals(recordPartConstraint)) {
			answer = true;
		} else

		if ("write".equals(recordPartConstraint)) {
			answer = false;
		}
		MCR.addReturned(answer);
		return answer;
	}

	@Override
	public Set<Constraint> getReadRecordPartConstraints() {
		MCR.addCall();
		Set<Constraint> constraints = new HashSet<Constraint>();
		constraints.add(new Constraint("someKey"));
		MCR.addReturned(constraints);
		return constraints;
	}

	@Override
	public boolean hasRecordPartWriteConstraint() {
		MCR.addCall();
		boolean answer = false;
		if ("readWrite".equals(recordPartConstraint)) {
			answer = true;
		} else if ("write".equals(recordPartConstraint)) {
			answer = true;
		}
		MCR.addReturned(answer);
		return answer;
	}

	@Override
	public Set<Constraint> getUpdateWriteRecordPartConstraints() {
		MCR.addCall();
		MCR.addReturned(writeConstraints);
		return writeConstraints;
	}

	@Override
	public boolean representsTheRecordTypeDefiningSearches() {
		MCR.addCall();
		MCR.addReturned(representsTheRecordTypeDefiningSearches);
		return representsTheRecordTypeDefiningSearches;
	}

	@Override
	public boolean representsTheRecordTypeDefiningRecordTypes() {
		MCR.addCall();
		MCR.addReturned(representsTheRecordTypeDefiningRecordTypes);
		return representsTheRecordTypeDefiningRecordTypes;
	}

	@Override
	public boolean hasLinkedSearch() {
		MCR.addCall();
		MCR.addReturned(hasLinkedSearch);
		return hasLinkedSearch;
	}

	@Override
	public String getSearchId() {
		MCR.addCall();
		MCR.addReturned(returnedSearchId);
		return returnedSearchId;
	}

	@Override
	public boolean hasRecordPartCreateConstraint() {
		MCR.addCall();
		boolean answer = false;
		if ("readWrite".equals(recordPartConstraint)) {
			answer = true;
		} else if ("write".equals(recordPartConstraint)) {
			answer = true;
		}
		MCR.addReturned(answer);
		return answer;
	}

	@Override
	public Set<Constraint> getCreateWriteRecordPartConstraints() {
		MCR.addCall();
		MCR.addReturned(writeStringConstraints);
		return writeStringConstraints;
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
