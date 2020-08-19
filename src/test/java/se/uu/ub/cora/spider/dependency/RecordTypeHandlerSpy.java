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
package se.uu.ub.cora.spider.dependency;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import se.uu.ub.cora.bookkeeper.metadata.Constraint;
import se.uu.ub.cora.data.DataGroup;
import se.uu.ub.cora.spider.data.DataAtomicSpy;
import se.uu.ub.cora.spider.data.DataGroupSpy;
import se.uu.ub.cora.spider.record.RecordTypeHandler;
import se.uu.ub.cora.spider.spy.MethodCallRecorder;

public class RecordTypeHandlerSpy implements RecordTypeHandler {
	public MethodCallRecorder MCR = new MethodCallRecorder();

	/**
	 * isPublicForRead is default false, if set to true, the recordType is considered totaly public
	 * and no security checks are supposed to be done for reading
	 */
	public boolean isPublicForRead = false;
	/**
	 * isAbstract is default false, if set to true, the recordType is considered abstract
	 */
	public boolean isAbstract = false;
	/**
	 * recordTypeHasReadPartConstraints is default false, if set to true, the recordType has read
	 * record parts constraints.
	 */
	// public boolean recordTypeHasReadPartConstraints = false;

	/**
	 * recordPartConstraints is default empty, can be set to "readWrite" or "write" to change
	 * behavior of {@link #hasRecordPartReadConstraint()} and
	 * {@link #hasRecordPartWriteConstraint()}
	 */
	public String recordPartConstraint = "";
	// public boolean hasRecordPartReadContraintHasBeenCalled = false;
	public Set<Constraint> writeStringConstraints = new HashSet<Constraint>();
	public Set<Constraint> writeConstraints = new HashSet<Constraint>();

	public boolean hasParent = false;
	public String parentId = "someParentId";

	public boolean isChildOfBinary = false;
	public boolean representsTheRecordTypeDefiningSearches = false;
	public boolean representsTheRecordTypeDefiningRecordTypes = false;

	public boolean hasLinkedSearch = false;

	public String returnedSearchId = "someSearchId";

	/**
	 * shouldAutoGenerateId is default false, if set to true will method shouldAutoGenerateId()
	 * return true instead of false.
	 */
	public boolean shouldAutoGenerateId = false;

	public RecordTypeHandlerSpy() {
		writeStringConstraints.add(new Constraint("someKey"));
	}

	@Override
	public boolean isAbstract() {
		MCR.addCall();
		MCR.addReturned(isAbstract);
		return isAbstract;
	}

	@Override
	public boolean shouldAutoGenerateId() {
		MCR.addCall();
		MCR.addReturned(shouldAutoGenerateId);
		return shouldAutoGenerateId;
	}

	@Override
	public String getNewMetadataId() {
		MCR.addCall();
		String returnValue = "fakeMetadataIdFromRecordTypeHandlerSpy";
		MCR.addReturned(returnValue);
		return returnValue;
	}

	@Override
	public String getMetadataId() {
		MCR.addCall();
		String returnValue = "fakeMetadataIdFromRecordTypeHandlerSpy";
		MCR.addReturned(returnValue);
		return returnValue;
	}

	@Override
	public List<String> createListOfPossibleIdsToThisRecord(String recordId) {
		MCR.addCall("recordId", recordId);
		List<String> fakeList = new ArrayList<>();
		fakeList.add("fakeIdFromRecordTypeHandlerSpy");
		MCR.addReturned(fakeList);
		return fakeList;
	}

	@Override
	public boolean isPublicForRead() {
		MCR.addCall();
		MCR.addReturned(isPublicForRead);
		return isPublicForRead;
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
	public DataGroup getMetadataGroup() {
		MCR.addCall();
		DataGroup metadataDataGroup = new DataGroupSpy("organisationGroup");
		metadataDataGroup.addChild(new DataAtomicSpy("nameInData", "organisation"));
		MCR.addReturned(metadataDataGroup);
		return metadataDataGroup;
	}

	@Override
	public Set<Constraint> getRecordPartReadConstraints() {
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
	public Set<Constraint> getRecordPartWriteConstraints() {
		MCR.addCall();
		MCR.addReturned(writeConstraints);
		return writeConstraints;
	}

	@Override
	public boolean hasParent() {
		MCR.addCall();
		MCR.addReturned(hasParent);
		return hasParent;
	}

	@Override
	public String getParentId() {
		MCR.addCall();
		MCR.addReturned(parentId);
		return parentId;
	}

	@Override
	public boolean isChildOfBinary() {
		MCR.addCall();
		MCR.addReturned(isChildOfBinary);
		return isChildOfBinary;
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
	public Set<Constraint> getRecordPartCreateWriteConstraints() {
		MCR.addCall();
		MCR.addReturned(writeStringConstraints);
		return writeStringConstraints;
	}

}
