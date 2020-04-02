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

import se.uu.ub.cora.data.DataGroup;
import se.uu.ub.cora.spider.data.DataAtomicSpy;
import se.uu.ub.cora.spider.data.DataGroupSpy;
import se.uu.ub.cora.spider.record.RecordTypeHandler;
import se.uu.ub.cora.spider.spy.MethodCallRecorder;

public class RecordTypeHandlerSpy implements RecordTypeHandler {
	public MethodCallRecorder MCR = new MethodCallRecorder();

	public boolean isPublicForRead = false;
	public boolean isAbstract = false;
	public boolean recordTypeHasReadPartConstraints = false;
	public String recordPartConstraint = "";
	public boolean hasRecordPartReadContraintHasBeenCalled = false;
	public Set<String> writeConstraints = new HashSet<String>();

	public RecordTypeHandlerSpy() {
		writeConstraints.add("someKey");
	}

	@Override
	public boolean isAbstract() {
		MCR.addCall("isAbstract");
		return isAbstract;
	}

	@Override
	public boolean shouldAutoGenerateId() {
		MCR.addCall("shouldAutoGenerateId");
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public String getNewMetadataId() {
		MCR.addCall("getNewMetadataId");
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public String getMetadataId() {
		MCR.addCall("getMetadataId");
		// TODO Auto-generated method stub
		return "fakeMetadataIdFromRecordTypeHandlerSpy";
	}

	@Override
	public List<String> createListOfPossibleIdsToThisRecord(String recordId) {
		MCR.addCall("createListOfPossibleIdsToThisRecord", "recordId", recordId);
		List<String> fakeList = new ArrayList<>();
		fakeList.add("fakeIdFromRecordTypeHandlerSpy");
		return fakeList;
	}

	@Override
	public boolean isPublicForRead() {
		MCR.addCall("isPublicForRead");
		return isPublicForRead;
	}

	@Override
	public boolean hasRecordPartReadConstraint() {
		MCR.addCall("hasRecordPartReadConstraint");
		hasRecordPartReadContraintHasBeenCalled = true;
		if ("readWrite".equals(recordPartConstraint)) {
			return true;
		}

		if ("write".equals(recordPartConstraint)) {
			return false;
		}
		// ""
		return false;
	}

	@Override
	public DataGroup getMetadataGroup() {
		MCR.addCall("getMetadataGroup");
		DataGroup metadataDataGroup = new DataGroupSpy("organisationGroup");
		metadataDataGroup.addChild(new DataAtomicSpy("nameInData", "organisation"));
		return metadataDataGroup;
	}

	@Override
	public Set<String> getRecordPartReadConstraints() {
		MCR.addCall("getRecordPartReadConstraints");
		Set<String> constraints = new HashSet<String>();
		constraints.add("someKey");
		return constraints;
	}

	@Override
	public boolean hasRecordPartWriteConstraint() {
		MCR.addCall("hasRecordPartWriteConstraint");
		hasRecordPartReadContraintHasBeenCalled = true;
		if ("readWrite".equals(recordPartConstraint)) {
			return true;
		}

		if ("write".equals(recordPartConstraint)) {
			return true;
		}
		return false;
	}

	@Override
	public Set<String> getRecordPartWriteConstraints() {
		MCR.addCall("getRecordPartWriteConstraints");
		return writeConstraints;
	}

}
